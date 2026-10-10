# LLD Mock — Distributed Job Scheduler (Attempt 01)

**Date:** 2026-10-10  
**Role:** Schrödinger Senior Software Developer (Backend)  
**Language:** Java 21 / Spring Boot examples  
**Format:** Guided question-by-question mock; not a timed, unaided implementation  
**Result:** **7.8/10** (equal-weight mean of eight assessed sections; cancellation was supplied as a reference answer, not independently scored)  
**Status:** Learning — next step: independently code the critical flow and retry the mock.

> This file separates what was demonstrated in the mock from the interviewer-provided reference design. The score measures performance during guided practice, not predicted interview success.

## 1. Scorecard

| Section | Score /10 | Evidence / feedback |
|---|---:|---|
| Requirements | 8 | One-time, cron, priority, retry, cancellation, status, multi-worker, misfires, extensibility; needed overlap/cancellation/durability clarifications |
| Class design | 7.5 | Job vs JobInstance, Command, Scheduler, WorkerPool, Redis lock, RetryPolicy; missing repositories/strategy separation initially |
| Java interfaces / patterns | 6.5 | Correct Command/Strategy/Registry purposes, but requested code was not supplied independently |
| Concurrency | 8 | PriorityBlockingQueue and atomic DB UPDATE conditional on PENDING; needed delayed scheduling/cancel-race details |
| Retry implementation | 8 | Durable nextRetryAt, nonblocking worker, three attempts, exponential backoff; needed attempt ownership and claim lease |
| Graceful shutdown | 8 | Stop poll, drain, shutdown/await; needed cooperative interruption, no unsafe reset of running work |
| SOLID principles | 7 | Correct definitions; needed examples tied to JobScheduler classes |
| Bulkhead / resource isolation | 9 | Separate bounded fast/slow/critical pools, custom rejection and durable deferral; differentiate dispatch deferral from business retry |
| Cancellation / reschedule race | Not scored | User requested reference answer rather than providing an implementation |

**Overall assessed mean:** (8 + 7.5 + 6.5 + 8 + 8 + 8 + 7 + 9) / 8 = **7.75 → 7.8/10**.

## 2. Problem statement and scope

Design an extensible, reliable low-level Job Scheduler:

- One-time and cron jobs; later add fixed-delay.
- Distinct job types: email, cleanup, reporting.
- HIGH / MEDIUM / LOW priority when multiple jobs are due.
- Max 3 total attempts, exponential backoff.
- Cancel a scheduled job; best-effort cancellation once RUNNING.
- Query status; survive restart.
- Distributed worker machines and atomic ownership.
- Misfires: catch up or skip.
- 5–10K executions/minute; avoid duplicate concurrent ownership.
- Show SOLID, command/strategy/registry patterns, class responsibilities, Java concurrency and lifecycle.

This is **LLD**: assume storage and worker infrastructure; do not spend the round drawing Kafka brokers or shards unless asked.

## 3. Main classes and responsibilities

| Type | Responsibility |
|---|---|
| `Job` | Durable definition: job ID, type, priority, payload reference, schedule, retry policy, status |
| `JobInstance` | One scheduled occurrence: run ID, job ID, scheduledAt, attemptCount, execution state, lease/owner, timestamps |
| `JobCommand` | Extensible execution behavior, `execute(JobContext)` |
| `Schedule` | Compute upcoming run time; implementations OneTime, Cron, FixedDelay |
| `RetryPolicy` | Compute next delay or terminate retries; ExponentialBackoff, FixedDelay, NoRetry |
| `JobCommandRegistry` | Resolve registered commands by job type |
| `JobScheduler` | Find due occurrences, coordinate scheduling and dispatch; not execute business logic |
| `JobDispatcher` | Route due instances to appropriate bounded worker pools |
| `JobRepository` | Job definition persistence |
| `JobInstanceRepository` | Atomic claims, completion, retry persistence, cancellation, lease management |
| `WorkerPool` | Run commands using bounded executors |
| `CancellationToken` | Cooperative cancellation for running operations |

### Class relationships

```mermaid
classDiagram
    class Job {
      +UUID id
      +String type
      +JobPriority priority
      +JobStatus status
    }
    class JobInstance {
      +UUID id
      +UUID jobId
      +Instant scheduledAt
      +int attemptCount
      +InstanceStatus status
      +Instant leaseUntil
    }
    class Schedule {
      <<interface>>
      +nextExecutionAfter(Instant) Optional~Instant~
    }
    class RetryPolicy {
      <<interface>>
      +nextDelay(int) Optional~Duration~
    }
    class JobCommand {
      <<interface>>
      +execute(JobContext) void
    }
    class JobCommandRegistry {
      +getCommand(String) JobCommand
    }
    class JobScheduler
    class JobDispatcher
    class JobRepository {
      <<interface>>
    }
    class JobInstanceRepository {
      <<interface>>
    }
    Job --> Schedule
    Job --> RetryPolicy
    Job "1" --> "*" JobInstance
    JobScheduler --> JobRepository
    JobScheduler --> JobInstanceRepository
    JobScheduler --> JobDispatcher
    JobDispatcher --> JobCommandRegistry
    JobCommandRegistry --> JobCommand
    JobDispatcher --> JobInstanceRepository
```

## 4. Core Java contracts (revision reference, not code written independently during mock)

```java
public interface JobCommand {
    void execute(JobContext context) throws Exception;
}

public interface Schedule {
    Optional<Instant> nextExecutionAfter(Instant time);
}

public interface RetryPolicy {
    // failedAttempt is 1-based (attempt 1 already failed).
    Optional<Duration> nextDelay(int failedAttempt);
}

public interface JobInstanceRepository {
    List<JobInstance> claimDueJobs(Instant now, int batchSize);
    boolean markRunning(UUID instanceId, UUID workerId, long token);
    boolean markCompleted(UUID instanceId, UUID workerId, long token);
    boolean scheduleRetry(UUID instanceId, UUID workerId, long token,
                          Instant nextRetryAt);
    boolean cancelIfNotRunning(UUID instanceId);
}
```

### Command + Registry

```java
public final class JobCommandRegistry {
    private final Map<String, JobCommand> commands;

    public JobCommandRegistry(Map<String, JobCommand> commands) {
        this.commands = Map.copyOf(commands);
    }

    public JobCommand getCommand(String type) {
        JobCommand command = commands.get(type);
        if (command == null) throw new IllegalArgumentException("Unknown job type: " + type);
        return command;
    }
}
```

For Spring, register `@Component("EMAIL")`, `@Component("CLEANUP")`, etc., and inject the bean map. No arbitrary user-provided class names should be loaded.

### Patterns and SOLID

- **Command:** `JobCommand` hides per-job business behavior.
- **Strategy:** `Schedule` and `RetryPolicy` let new algorithms be added.
- **Registry/Factory:** job type maps to command.
- **Repository:** persistence abstracted from scheduling.
- **Bulkhead:** separate worker pools for fast, slow, critical jobs.
- **SRP:** scheduler schedules; command executes; repository persists; policy calculates retry.
- **OCP:** add `FixedDelaySchedule` or `ExportJob` without rewriting scheduler/worker.
- **DIP:** scheduler depends on `JobRepository`/`JobDispatcher`, not concrete PostgreSQL/executor types.
- **ISP/LSP:** narrow interfaces; every schedule implementation honors the same next-execution contract.

**Nuance:** fixed-delay recurrence computes the next run from previous completion, while cron uses calendar time. The scheduling contract needs adequate context rather than assuming all strategies calculate from identical starting points.

## 5. Execution state machine

```text
PENDING -> CLAIMED -> RUNNING -> COMPLETED
                        |
                        +-> RETRY_SCHEDULED -> CLAIMED -> RUNNING
                        |
                        +-> FAILED

PENDING / unstarted CLAIMED -> CANCELLED
RUNNING -> best-effort cooperative cancellation (policy-defined outcome)
```

Parent `Job` controls recurring definition status: ACTIVE / PAUSED / CANCELLED. Each `JobInstance` has its independent state. Use unique `(job_id, scheduled_at)` or an occurrence key to avoid duplicate recurring-instance generation.

### Scheduler queue and distributed claim

- `PriorityBlockingQueue` is thread-safe **but not time-aware**: `take()` may return tomorrow’s job immediately.
- Consider `DelayQueue<DelayedJob>` for due-time gating, plus deterministic priority tie-breaking when times match; durable DB is authoritative.
- Concurrent rescheduling requires remove/reinsert or versioned stale-entry detection; mutating an object inside a heap does not reorder it.
- Atomic DB claims prevent different machines from both winning the same state transition:

```sql
UPDATE job_instances
SET status = 'CLAIMED',
    worker_id = :workerId,
    lease_until = :leaseUntil,
    execution_token = execution_token + 1
WHERE id = :instanceId AND status = 'PENDING';
```

Only the caller observing one updated row owns the claim. Due-job fetch across scheduler nodes may use `SELECT ... FOR UPDATE SKIP LOCKED` with short transactions.

**Key caveat:** a claimed job can outlive its lease after a GC pause. Use token/version checks for state changes; use idempotency or downstream fencing for side effects.

## 6. Retry design — user answer and corrections

The user correctly designed a `RetryPolicy` with exponential delay, `JobWorker` that persists `RETRY_SCHEDULED`, and `JobScheduler` that polls the DB for due retries every ~500 ms. No `Thread.sleep()`; retries survive restart.

Establish unambiguous attempt semantics: `attemptCount` = number of executions **started**, incremented atomically when a worker starts; never also increment in the catch block.

| Started attempt | Failure action |
|---|---|
| 1 | schedule retry in 1 second |
| 2 | schedule retry in 2 seconds |
| 3 | mark FAILED |

```java
public final class ExponentialBackoffPolicy implements RetryPolicy {
    private final int maxAttempts;
    private final Duration initial;

    public ExponentialBackoffPolicy(int maxAttempts, Duration initial) {
        this.maxAttempts = maxAttempts;
        this.initial = initial;
    }

    @Override
    public Optional<Duration> nextDelay(int failedAttempt) {
        if (failedAttempt >= maxAttempts) return Optional.empty();
        return Optional.of(initial.multipliedBy(1L << (failedAttempt - 1)));
    }
}
```

Use `Instant` for persistence, cap backoff, add jitter and classify retryable versus permanent errors. Persist retry state conditionally on worker ownership/token. A scheduler crash after DB claim but before in-memory enqueue requires leases and reclamation.

## 7. Cancellation and rescheduling race (reference answer supplied during mock, unscored)

Cancellation must atomically win over the `CLAIMED -> RUNNING` transition.

```sql
UPDATE job_instances
SET status = 'CANCELLED', version = version + 1
WHERE id = :instanceId AND status IN ('PENDING', 'CLAIMED');
```

Worker immediately before business logic:

```sql
UPDATE job_instances
SET status = 'RUNNING', version = version + 1
WHERE id = :instanceId AND status = 'CLAIMED'
  AND worker_id = :workerId;
```

If cancellation wins, worker sees zero rows and must not start. If RUNNING wins, cancellation is best-effort (cooperative token/checkpoint). A status transition alone cannot atomically prevent an already-started **external** side effect.

Reschedule:

```sql
UPDATE job_instances
SET scheduled_at = :newTime, version = version + 1
WHERE id = :instanceId AND status = 'PENDING'
  AND version = :expectedVersion;
```

Queued snapshots carry the old version and are rejected if stale. Repeated cancel is idempotent.

## 8. Graceful shutdown — user answer and corrections

Original answer: stop DB polling, drain `PriorityBlockingQueue`, return unstarted jobs to `PENDING`, call `ExecutorService.shutdown()`, await ~30s, then `shutdownNow()`, recover zombies via heartbeat.

Corrections:

1. Mark instance **DRAINING** and stop BOTH polling and dispatch before draining.
2. Release claims only for tasks provably not started. Do **not** blindly reset RUNNING to PENDING: side effects may already have occurred.
3. `shutdownNow()` is a cooperative interrupt request, not forced thread termination.
4. Finish/persist completed work within grace period; recover others via persisted leases and idempotency.
5. Shutdown should be idempotent; K8s readiness/termination grace should coordinate draining.

```java
if (!shuttingDown.compareAndSet(false, true)) return;
schedulerPool.shutdown(); // also stop queue dispatcher
workerPool.shutdown();
try {
    if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
        workerPool.shutdownNow();
    }
} catch (InterruptedException e) {
    workerPool.shutdownNow();
    Thread.currentThread().interrupt();
}
// Recovery coordinator reclaims expired persisted leases later.
```

## 9. Bulkhead / bounded executors — strongest answer (9/10)

The user proposed separate dedicated pools for slow, fast and high-priority jobs, each with bounded queue capacity and custom rejection; rejected unstarted jobs should be durably deferred rather than silently lost.

```text
                 Scheduler
                     |
                 Dispatcher
            /--------|---------\
         FAST       SLOW      CRITICAL
         pool       pool        pool
       bounded    bounded     bounded
```

**Important improvements:**
- Do not use `CallerRunsPolicy` on a scheduler dispatch thread if slow jobs could block scheduling.
- On rejected submission, atomically release the claim or mark deferred using token checks, without incrementing the business execution attempt.
- Prioritize based on due time, job criticality and fairness; guard against starvation.
- Pool sizes are workload-dependent; hard-coded counts are merely illustrative.
- Monitor queue depth, active threads, rejection counts, execution latency and oldest waiting job.

## 10. Critical edge-case matrix

| Scenario | Mitigation |
|---|---|
| Same run claimed twice | Atomic conditional claim / `SKIP LOCKED` |
| Claimed, process crashes before enqueue | Expiring lease and recovery |
| Worker crashes after external effect | Business idempotency key, durable dedup |
| Old worker resumes after lease expiry | Fencing/ownership token; downstream idempotency |
| Duplicate cron instance created | Unique occurrence key |
| Job scheduled far in future | DB authoritative; only near-term queue in memory |
| Same due instant, different priority | Priority tie-break after due-time eligibility |
| Rescheduled entry remains in queue | Version check, drop stale snapshot |
| Cancel races with RUNNING | Atomic status competition, best-effort after start |
| Retry occurs on restart | Persist `nextRetryAt` |
| Three failures | Final FAILED; optional DLQ/audit record |
| Slow task starves fast jobs | Bulkhead / bounded pools |
| All pools saturated | Admission control, durable deferral, alert |
| Grace period expires | Interrupt cooperative work, recover via lease |
| Lost scheduler or Redis state | DB source of truth; reload |
| Recurrence overlaps | ALLOW, SKIP, QUEUE configurable |
| Downtime misses occurrences | Misfire: run all, latest, or skip |
| DST/clock shift | Store UTC instants + IANA timezone for cron |
| Priority starvation | Aging / reserved capacity / weighted fairness |
| DB write fails after side effect | Idempotent retried business operation |
| Very large queue | Bounded admission and durable backlog |

## 11. Interview pacing — 45–60 min

- **0–5:** clarify one-time/cron, priority, retries, cancellation, durability, overlap and scale.
- **5–15:** Job vs JobInstance, lifecycle, responsibilities.
- **15–25:** Schedule/RetryPolicy/JobCommand/Registry, repository/dispatcher relations.
- **25–40:** implement atomic claim, due dispatch and non-blocking durable retry.
- **40–50:** cancellation/reschedule race, lease recovery, graceful shutdown.
- **50–60:** bulkheads, bounded queues, failure scenarios and trade-offs.

### Two-minute interview pitch

> I separate durable job definitions from individual execution instances. Job defines schedule, job type, priority and retry policy, and JobInstance tracks each occurrence and its attempt/state. Command handles executable job logic; Strategy handles cron/one-time schedules and retry policies; a registry resolves job types without changing the scheduler.
>
> The scheduler queries durable storage for due work and uses atomic state transitions to claim executions. Workers execute through bounded pools; leases and execution tokens handle crashes and stale owners, while idempotent business operations protect against repeated side effects. Retries persist nextRetryAt rather than sleeping in a worker thread. Cancellation races are resolved with conditional status updates and version checks.
>
> For production readiness I drain scheduling and workers gracefully, recover expired leases, and isolate slow from critical jobs with the Bulkhead Pattern. This follows SRP, OCP and DIP and remains extensible.

## 12. Next actions

1. **Unaided coding:** Implement `JobScheduler`, `JobDispatcher`, `RetryPolicy`, `JobInstanceRepository` interface and job claiming/retry flow in Java without seeing reference code.
2. **Race tests:** cancellation vs RUNNING, reschedule vs dispatch, lease expiry/stale worker, simultaneous claims, attempt-3 terminal failure.
3. **Timed LLD re-attempt:** one 45–60 min dry run without hints; update score based on actual code and trade-offs.
4. **Alternate LLD:** Parking Lot or Notification Framework with concrete Java class relationships and implementation.
