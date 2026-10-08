# Distributed Job Scheduler — HLD Revision Pack

Status: Learning  
Target round: Senior Backend / System Design  
Recommended interview time: 45–60 minutes

## 1. Problem statement

Design a distributed job scheduler that supports immediate, future and recurring execution. The system should execute millions of jobs per day, scale horizontally, tolerate node failures, support retries and cancellation, and provide near-real-time job status.

A strong interview answer should focus on correctness around claiming, dispatching and retrying jobs, while accepting **at-least-once execution** and requiring idempotent workers.

---

## 2. Requirements

### Functional

1. Schedule a job for immediate or future execution.
2. Support cron / recurring schedules.
3. Track states such as PENDING, CLAIMED, QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED and SKIPPED.
4. Update or cancel a job before execution.
5. Configurable retry count and retry/backoff policy.
6. Dead-letter handling for permanently failed jobs.
7. Optional overlap policy for recurring jobs: ALLOW, SKIP or QUEUE.
8. Optional misfire policy for occurrences missed during downtime.

### Non-functional

- About 20M jobs/day.
- Peak around 5K scheduled executions/sec under normal load.
- Hundreds of worker/executor instances.
- Target execution accuracy around ±2 seconds under supported load.
- Horizontal scalability.
- Highly available control plane and workers.
- At-least-once execution.
- Eventual consistency is acceptable for read-only status views, but state transitions such as claim/cancel/reschedule need stronger consistency.
- No job should be silently lost.

### Important interview clarification

Do not simply say “availability > consistency” for the whole system. Say:

> We favor availability for execution, but we still need strong consistency around job claiming, cancellation and state transitions.

---

## 3. Capacity estimation

20M jobs/day:

```
20,000,000 / 86,400 ≈ 231 jobs/sec average
```

Peak is assumed to be much higher, around 5K/sec.

The architecture should be sized for peak traffic and bursts, not average traffic.

If average metadata is ~1 KB:

```
20M × 1 KB ≈ 20 GB/day before indexes/replication
```

This immediately motivates retention, partitioning and archival.

---

## 4. High-level architecture

```text
Clients
   |
Load Balancer / API Gateway
   |  auth, rate limit, routing
   v
Job Service
   |
   v
PostgreSQL  <-----------------------------+
   |                                      |
   | due jobs                             | status/result
   v                                      |
Scheduler / Dispatcher                    |
   |                                      |
   | claim + outbox                       |
   v                                      |
Outbox Publisher                          |
   |                                      |
   v                                      |
Kafka                                     |
   |                                      |
   v                                      |
Job Consumer / Worker Pool ---------------+
   |
   v
Business logic / downstream systems

Redis: optional coordination, short-lived scheduling structures,
rate limits, cached status, heartbeats — not the authoritative source of truth.
```

### Responsibilities

**API Gateway**
- Authentication / authorization
- Rate limiting
- Request routing

**Job Service**
- Create/update/cancel jobs
- Validate cron expressions and retry configuration
- Persist job definitions
- Expose status APIs

**PostgreSQL**
- Source of truth for job definitions and job runs
- Durable job state
- Transactional claiming
- Outbox table

**Scheduler / Dispatcher**
- Finds jobs approaching their execution time
- Claims them safely
- Creates execution records / outbox events
- Does not execute business logic

**Kafka**
- Durable buffer between scheduling and execution
- Decouples scheduler throughput from worker throughput
- Enables horizontal worker scaling

**Workers**
- Consume execution events
- Apply idempotency
- Execute job logic
- Update execution status
- Retry or DLQ on failure

---

## 5. Data model

### job_definition

```text
job_id
owner_id
job_type
payload_ref / payload
schedule_type        // ONCE, CRON, FIXED_DELAY
cron_expression
scheduled_at
timezone
overlap_policy       // ALLOW, SKIP, QUEUE
misfire_policy       // RUN_ALL_MISSED, RUN_LATEST_ONLY, SKIP_MISSED
max_retries
retry_policy
priority
status
created_at
updated_at
```

### job_run

```text
run_id / execution_id
job_id
scheduled_time
attempt_no
status
executor_id
lease_until
started_at
completed_at
error_code
error_message
```

### outbox_events

```text
event_id
aggregate_id / job_id
event_type
payload
status              // NEW, PUBLISHED, FAILED
retry_count
created_at
published_at
```

Useful indexes:

```sql
CREATE INDEX idx_job_due
ON job_definition(status, scheduled_at);

CREATE INDEX idx_outbox_new
ON outbox_events(created_at)
WHERE status = 'NEW';
```

---

## 6. Core scheduling flow

1. Client creates a job through Job Service.
2. Job Service validates and persists it in PostgreSQL.
3. Far-future jobs remain only in PostgreSQL.
4. Scheduler loads only a near-term sliding window, for example the next 5 minutes.
5. Multiple scheduler nodes safely divide due jobs using DB row claiming.
6. Scheduler changes the job/run state and inserts an outbox event in the same DB transaction.
7. Outbox publisher sends JOB_READY to Kafka.
8. Workers consume the message.
9. Worker executes the job idempotently.
10. Worker persists SUCCESS/FAILED and then commits Kafka offset.

---

## 7. Multiple schedulers picking the same job

### Weak solution

A Redis `SETNX` per job can coordinate schedulers, but Redis lock + DB update + Kafka publish creates multiple failure windows.

### Preferred solution

Use PostgreSQL:

```sql
SELECT id
FROM jobs
WHERE status = 'PENDING'
  AND scheduled_at <= now()
ORDER BY scheduled_at
FOR UPDATE SKIP LOCKED
LIMIT 500;
```

Each scheduler transaction locks a different batch.

```text
Scheduler 1 -> rows 1–500
Scheduler 2 -> skips locked rows -> 501–1000
Scheduler 3 -> next available batch
```

Then update:

```
PENDING -> CLAIMED
```

This gives horizontal work distribution without one Redis lock per job.

---

## 8. DB + Kafka dual-write problem

Failure:

```
DB: PENDING -> CLAIMED commits
scheduler crashes / Kafka unavailable
Kafka event never published
```

A heartbeat can detect stuck jobs, but it is not the strongest correctness mechanism.

### Transactional outbox

Inside one PostgreSQL transaction:

```
BEGIN

update job/run state
insert JOB_READY into outbox_events

COMMIT
```

A separate publisher sends the outbox event to Kafka.

Benefits:

- DB succeeds, Kafka down -> event remains durable.
- Scheduler crashes -> another outbox publisher can continue.
- No silent loss between DB state and message creation.

Remaining edge case:

```
Kafka publish succeeds
publisher crashes before marking PUBLISHED
```

The event may be published again.

Therefore:

```
at-least-once publication
+
idempotent consumers
```

---

## 9. Idempotency and duplicate execution

Failure:

```
worker executes business operation
worker crashes before Kafka offset commit
Kafka redelivers
```

Use a unique execution id / idempotency key.

Worker flow:

```
consume execution_id
BEGIN TX
  check/insert idempotency record
  apply business state change
  mark execution SUCCESS
COMMIT
commit Kafka offset
```

Important ordering:

```
business transaction commit
BEFORE
Kafka offset commit
```

If offset is committed first and worker crashes before business commit, the job can be lost.

The final defense should be an atomic DB constraint where possible:

```
UNIQUE(execution_id)
```

or an idempotency key on the target business operation.

Do not rely only on:

```java
if (!exists(id)) {
    execute();
}
```

because two workers can race between the read and write.

---

## 10. Worker crash / orphaned running jobs

For long-running work, use a **lease** on the job run.

```text
job_run
run_id
executor_id
status = RUNNING
lease_until
attempt
```

Worker periodically renews the lease.

Recovery query:

```
status = RUNNING
AND lease_until < now()
```

The run is considered orphaned and can be retried.

Executor heartbeats in Redis are useful for operational health, but job-level leases are a more precise correctness mechanism.

Important: the crashed worker may have partially completed its work. Retried code must therefore be idempotent or resume safely from checkpoints.

---

## 11. Recurring-job overlap policies

Store a per-job setting:

```
overlap_policy = ALLOW | SKIP | QUEUE
```

### ALLOW

Every scheduled occurrence creates its own JobRun.

Use when executions are independent and concurrency is safe.

### SKIP

If a previous execution is RUNNING, do not start the new occurrence.

Optionally create a JobRun with status SKIPPED for auditability.

### QUEUE

Create every occurrence, but allow only one active execution per job.

Example:

```
10:00 RUNNING
10:01 QUEUED
10:02 QUEUED
10:03 QUEUED
```

After 10:00 finishes, 10:01 becomes RUNNING.

Concurrency must be enforced with an atomic DB transition / lock / constraint.

---

## 12. Misfire policy

Overlap and misfire are different.

If the system is unavailable for 20 minutes and a cron job is due every minute, choose a policy such as:

```
RUN_ALL_MISSED
RUN_LATEST_ONLY
SKIP_MISSED
```

This policy should be explicit per job or job type.

---

## 13. Sliding scheduling window

Do not load jobs scheduled months into the future into active scheduler memory.

Use:

```
Far future -> PostgreSQL only
Next 5 minutes -> active scheduling window
Due within seconds -> dispatch path
```

The scheduler can periodically load jobs whose `scheduled_at` falls inside the next few minutes.

A near-term priority queue or Redis sorted set can be used for precise dispatch.

Critical sentence:

> The sliding window is only an optimization. PostgreSQL remains the source of truth, so losing scheduler memory does not lose scheduled jobs.

Polling frequency must match the SLA. Polling every 10–20 seconds is incompatible with a strict ±2 second target unless another near-term mechanism provides precise dispatch.

---

## 14. Thundering herd: 1M jobs scheduled for one second

Suppose normal sustainable capacity is 5K executions/sec.

```
1,000,000 / 5,000 = 200 seconds
```

It is impossible to honestly promise ±2 seconds for every job at that capacity.

Use:

1. DB batching and `SKIP LOCKED`.
2. Kafka as a durable burst buffer.
3. Enough partitions for consumer parallelism.
4. Worker autoscaling based on consumer lag and execution latency.
5. Backpressure and concurrency limits to protect downstream systems.
6. Priority classes such as HIGH / NORMAL / LOW.
7. Admission control or quota for extreme workloads.
8. A documented degraded/burst SLA.

Important senior-level point:

> Autoscaling cannot create infinite capacity because downstream databases and APIs also have limits.

---

## 15. Retry design

Each job can define:

```text
max_attempts
retry_strategy
base_delay
max_delay
jitter
retryable_error_types
```

Example backoff:

```
1s -> 5s -> 30s -> 2m -> 10m
```

Prefer exponential backoff with jitter so thousands of jobs do not retry simultaneously after a shared dependency recovers.

### Retry flow

```
RUNNING
  |
failure
  v
classify error
  |
  +-- non-retryable -> FAILED / DLQ
  |
  +-- retryable and attempts left
          |
          v
      RETRY_SCHEDULED
          |
      next_retry_at
          |
      scheduler/retry dispatcher
          |
          v
        Kafka
```

Do not immediately republish into a tight loop.

The retry schedule should be durable in DB or in a durable delayed-delivery mechanism.

### Retry storm protection

If a downstream dependency is unhealthy:

- circuit breaker
- global/per-target concurrency limit
- exponential backoff
- jitter
- rate-limited retries
- pause/resume a job class if needed

---

## 16. Dead-letter queue

After max attempts or on a non-retryable failure:

```
FAILED_PERMANENTLY
   |
   v
DLQ
```

Store enough context to diagnose and safely replay:

```
job_id
run_id
attempt_count
last_error
payload reference
first_failure_at
last_failure_at
```

DLQ replay must preserve idempotency. Replaying should create a controlled new attempt, not blindly bypass state checks.

Operational controls should support:

- inspect
- retry selected jobs
- bulk replay with rate limits
- discard only with explicit policy
- alert on DLQ growth

---

## 17. Cancellation and rescheduling edge cases

### Cancel before claim

Simple:

```
PENDING -> CANCELLED
```

### Cancel after Kafka publish

The message may already be in Kafka.

Worker must re-check authoritative job/run state before executing important side effects.

```
consume
-> state is CANCELLED?
-> stop
```

For expensive jobs, optionally publish a cancellation signal, but DB state remains authoritative.

### Cancel while RUNNING

Define semantics:

- best-effort cooperative cancellation
- cannot guarantee rollback of external side effects
- job implementation should periodically check cancellation token for long-running work

### Reschedule race

Use versioning / optimistic locking so an old scheduler snapshot cannot execute a superseded schedule.

Example:

```
job_version
```

Execution event carries the version. Worker rejects stale versions.

---

## 18. Time, timezone and clock-skew edge cases

Store execution instants in UTC.

Keep the user's timezone with the cron definition for computing future occurrences.

Handle:

- DST jump forward: a local time may not exist.
- DST fallback: the same local time may occur twice.
- timezone rule changes.
- clock skew across scheduler nodes.

Use NTP-synchronized clocks and avoid relying on arbitrary local server timezones.

Define cron semantics explicitly.

---

## 19. Job dependencies

Optional advanced feature:

```
Job B depends on Job A
```

Do not continuously scan all dependencies.

Maintain dependency state / count.

Example:

```
remaining_dependencies
```

When A succeeds, decrement B atomically. When it reaches zero, B becomes schedulable.

Define what happens when a dependency fails: fail dependent, skip it, or wait for manual intervention.

---

## 20. Long-running jobs

For jobs lasting minutes/hours:

- renewable lease
- execution timeout
- cooperative cancellation
- heartbeat/progress
- checkpointing where useful
- separate worker pools for very long work so they do not starve short jobs

Do not hold a database transaction open for the whole business execution.

---

## 21. Outbox scalability

The outbox is an operational delivery table, not permanent history.

Use:

- partition by `created_at`
- partial/indexed query for NEW events
- batch reads
- `FOR UPDATE SKIP LOCKED` across multiple publishers
- short retention for PUBLISHED rows
- partition dropping or batched deletion
- archive elsewhere only if business/audit requirements demand it

Typical query:

```sql
SELECT *
FROM outbox_events
WHERE status = 'NEW'
ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT 500;
```

Do not optimize primarily for `job_id` if the publisher query is by status/time.

---

## 22. Partitioning strategy

At large scale, consider time partitioning for job runs/history.

Good candidates:

- job_run by scheduled_time / created_at
- outbox by created_at

Keep hot/current partitions small.

For a very large multi-tenant system, tenant/hash sharding may be needed later, but do not jump to sharding before showing why a single PostgreSQL cluster plus partitioning is insufficient.

---

## 23. Kafka design considerations

Possible topics:

```
job-run
job-retry
job-dlq
```

Separate topics are optional; topic design depends on operational and retention needs.

Partition key options:

- `job_id` when ordering for one logical job matters.
- tenant/customer key if per-tenant ordering matters.
- high-cardinality execution id for even distribution when ordering is unnecessary.

Remember:

> Kafka guarantees ordering only within a partition, not globally.

Consumer count beyond partition count does not increase parallelism within a consumer group.

---

## 24. Priority jobs

Two options:

### Separate topics/worker pools

```
job-high
job-normal
job-low
```

Pros: strong isolation and predictable capacity.

### Priority-aware dispatcher

Pros: fewer topics; more complex fairness.

Always avoid starvation. Reserve some capacity for lower priorities or use weighted scheduling.

---

## 25. Backpressure

Protect the whole dependency chain.

Signals:

- Kafka consumer lag
- worker concurrency
- DB connection pool saturation
- downstream latency/error rate
- queue depth
- execution age

Actions:

- stop increasing worker concurrency
- slow admission
- reduce dispatcher batch size
- circuit-break unhealthy downstream dependencies
- keep durable backlog in Kafka/DB instead of in process memory

---

## 26. Availability and failure scenarios

### Scheduler crashes before claim commit

DB transaction rolls back. Another scheduler can claim the job.

### Scheduler crashes after outbox commit

Event remains in outbox and another publisher sends it.

### Kafka unavailable

Outbox accumulates safely; alert and retry.

### Worker crashes before business commit

Kafka redelivers; work retries.

### Worker crashes after business commit but before offset commit

Kafka redelivers; idempotency prevents duplicate side effects.

### DB unavailable

New scheduling/state transitions cannot safely proceed. API may reject writes or degrade to read-only. Do not acknowledge a durable schedule if it was never persisted.

### Redis unavailable

Core correctness should continue if Redis is only optimization/coordination. Redis loss should not lose jobs.

### Entire scheduler fleet down

Jobs remain in PostgreSQL. On recovery, misfire policy controls overdue occurrences.

---

## 27. Observability

Metrics:

- schedule-to-start latency
- execution latency
- success/failure rate
- retry count/rate
- DLQ depth
- oldest pending job age
- claimed-but-not-dispatched age
- RUNNING jobs with expired leases
- outbox backlog and oldest NEW event
- Kafka consumer lag
- per-job-type / per-tenant error rate
- cancellation lag
- worker utilization
- DB connection pool and slow-query metrics

Tracing:

```
job_id
run_id
execution_id
trace_id / correlation_id
```

Logs should be structured and searchable.

Alert on symptoms tied to SLOs, not only CPU.

---

## 28. Security

- Authenticate scheduling APIs.
- Authorize job ownership / tenant access.
- Do not put secrets directly in job payloads.
- Store secret references and resolve them securely at execution time.
- Encrypt sensitive data at rest/in transit.
- Validate job type and payload.
- Rate limit schedule creation.
- Protect admin/DLQ replay operations with elevated permissions.
- Audit create/update/cancel/replay operations.

---

## 29. Interview edge-case checklist

Before ending the design, quickly scan these:

- duplicate scheduler claim
- DB commit succeeds but Kafka publish fails
- duplicate Kafka delivery
- worker dies before completion
- worker dies after side effect
- retry storm
- poison job / DLQ
- recurring-job overlap
- missed cron occurrences
- cancellation already in Kafka
- reschedule race
- stale schedule version
- 1M jobs at same timestamp
- downstream service outage
- Kafka unavailable
- DB unavailable
- Redis unavailable
- scheduler fleet restart
- long-running jobs
- execution timeout
- clock skew
- timezone/DST
- backlog fairness / priority starvation
- outbox growth
- history cleanup
- idempotency-record retention
- deployment / graceful shutdown

You do not need to explain all of these upfront. Mention the most important ones and go deeper where the interviewer probes.

---

## 30. Common mistakes to avoid

1. Redis locks as the only correctness mechanism.
2. Updating DB then publishing Kafka without addressing dual writes.
3. Claiming “exactly once” for the whole business operation.
4. Committing Kafka offset before durable business state.
5. Polling every 20 seconds while promising ±2 second execution.
6. Keeping all future jobs in scheduler memory.
7. Saying autoscaling solves unlimited burst load.
8. Retrying immediately with no backoff/jitter.
9. Treating logs as a recovery mechanism.
10. Using one global overlap policy for every recurring job.
11. Holding DB transactions open during long job execution.
12. Forgetting cancellation/reschedule races.
13. Ignoring retention of job history/outbox/idempotency data.
14. Adding Redis/Kafka without explaining why each is needed.

---

## 31. 45–60 minute interview plan

### 0–5 min: requirements

Clarify:

- one-time + recurring?
- precision/SLA?
- execution duration?
- retry guarantees?
- overlap semantics?
- scale?
- at-least-once acceptable?

### 5–10 min: scale + APIs

Do rough estimates and APIs.

### 10–20 min: high-level design

Draw:

```
Client -> Job Service -> DB -> Scheduler -> Outbox -> Kafka -> Workers
```

### 20–35 min: deep dive

Prefer these:

1. duplicate scheduler claim
2. DB + Kafka consistency
3. worker idempotency
4. retry/DLQ

### 35–45 min: scale

Discuss:

- sliding window
- partitioning
- Kafka partitions
- worker scaling
- thundering herd
- backpressure

### 45–55 min: failures/edge cases

Choose the most relevant:

- crash recovery
- overlap/misfire
- cancellation
- clock/DST
- DB/Kafka outage

### Final 2–3 min: summary

State guarantees and trade-offs.

---

## 32. Two-minute final answer

> I would keep PostgreSQL as the source of truth for job definitions and execution state. Jobs scheduled far in the future remain only in the database, while scheduler instances load a small sliding window of upcoming work. Multiple schedulers safely claim jobs using database transactions and FOR UPDATE SKIP LOCKED.
>
> To avoid the DB/Kafka dual-write problem, claiming a job and writing a JOB_READY outbox event happen in the same transaction. Outbox publishers deliver those events to Kafka. Kafka buffers bursts and worker consumers execute jobs horizontally.
>
> The system provides at-least-once execution, so every run has a unique execution ID and workers are idempotent. Business state is committed before Kafka offsets. Long-running jobs use leases so orphaned executions can be recovered.
>
> Retries use configurable exponential backoff with jitter and eventually move poison jobs to a DLQ. Recurring jobs support ALLOW, SKIP and QUEUE overlap policies plus explicit misfire behavior. PostgreSQL remains authoritative, so loss of Redis or scheduler memory does not lose jobs.
>
> At extreme bursts, such as one million jobs scheduled at the same second, Kafka provides buffering, workers autoscale within downstream capacity, and the system applies backpressure, priorities and realistic burst SLAs rather than pretending every job can meet the normal ±2 second target.

---

## 33. Likely interviewer follow-ups

1. Why PostgreSQL instead of Redis as the scheduler source of truth?
2. Why Kafka at all?
3. How do 10 schedulers avoid duplicates?
4. What if Kafka is down after the DB update?
5. Can you guarantee exactly once?
6. What happens if a worker crashes after charging a payment?
7. How do recurring jobs avoid overlapping?
8. What happens after 20 minutes of scheduler downtime?
9. How do you support a million jobs at exactly 9:00?
10. How do you scale the outbox table?
11. How do you cancel a job already published to Kafka?
12. How do you prevent retry storms?
13. How do you handle DST?
14. What metrics tell you the scheduler is unhealthy?
15. How do you deploy workers without losing running jobs?

---

## 34. Re-attempt goals

On the next mock, answer these without hints:

- Explain the complete architecture in under 4 minutes.
- Explain `FOR UPDATE SKIP LOCKED` clearly.
- Explain transactional outbox without confusing it with heartbeat recovery.
- Explain at-least-once + idempotency.
- Explain business commit vs Kafka offset commit ordering.
- Explain sliding window scheduling while preserving PostgreSQL as source of truth.
- Handle recurring overlap and misfire policies.
- Handle thundering herd honestly.
- Design retries with exponential backoff, jitter and DLQ.
