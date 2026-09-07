# 45–60 Minute System Design Answer Framework

Use this framework from memory. It is a speaking guide, not a script to recite.

## Opening (first 30 seconds)

> I will first clarify the scope and success criteria, estimate the important scale, define APIs and data access patterns, draw a simple end-to-end design, then deep-dive into the highest-risk areas and finish with failures and trade-offs. Is that direction okay?

## HLD timebox

| Time | Stage | Evidence to produce |
|---|---|---|
| 0–5 min | Clarify requirements | 3–5 functional requirements, NFRs, exclusions |
| 5–10 min | Estimate scale | Average/peak QPS, storage, bandwidth, read/write ratio |
| 10–15 min | APIs and data | Critical endpoints/events, entities, access patterns |
| 15–28 min | Architecture | Readable diagram and two end-to-end flows |
| 28–43 min | Deep dive | Two risks: data, cache, partitioning, messaging, concurrency |
| 43–52 min | Reliability | Failures, retries, idempotency, observability, security |
| 52–60 min | Trade-offs and close | Alternatives, bottleneck, evolution path, summary |

Do not estimate numbers that will not influence a decision. State assumptions and ask the interviewer to confirm them.

## LLD timebox

| Time | Stage | Evidence to produce |
|---|---|---|
| 0–5 min | Scope | Use cases, exclusions, invariants |
| 5–12 min | Model | Entities, responsibilities, relationships |
| 12–18 min | Design | Interfaces and patterns justified by change points |
| 18–43 min | Java implementation | Critical happy path and business rules |
| 43–52 min | Tests | Happy path, edge cases, invalid state, concurrency |
| 52–60 min | Trade-offs | Extensibility, thread safety, persistence boundaries |

Prefer a small working design over many empty classes. Mention a pattern only when it solves a real variation or state transition.

## The decision sentence

For every important choice, say:

> Because **[requirement/constraint]**, I choose **[option]**. It gives **[benefit]**, but costs **[trade-off]**. If **[condition changes]**, I would use **[alternative]**.

Example: “Because redirects are read-heavy and latency-sensitive, I choose cache-aside with TTL. It reduces database reads, but permits bounded staleness and needs stampede protection.”

## Deep-dive checklist

Select only the items relevant to the problem:

- Data ownership, indexes, partition key, replication and consistency.
- Cache key, TTL, invalidation, hot keys and stampede protection.
- Message key, ordering scope, retry, DLQ and idempotent consumer.
- Race conditions, locking/versioning and invariant enforcement.
- Timeouts, retry budget, circuit breaker and backpressure.
- Authentication, authorization, abuse prevention and sensitive data.
- Logs, metrics, traces, SLI/SLO and recovery procedure.

## When stuck

1. Restate the requirement and invariant.
2. Compare two options.
3. Choose a reasonable default and state the trade-off.
4. Continue the end-to-end flow.
5. Record the uncertainty for the scorecard.

Do not wait silently for a perfect answer.

## Closing (last two minutes)

Summarize the critical flows, the main scaling mechanism, the consistency choice, the largest remaining risk and how the design would evolve at 10× traffic.

After the mock, use the [score rubric](README.md#scoring-rubric) and save the attempt before reading any reference solution.
