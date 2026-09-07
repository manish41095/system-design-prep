# Mock Interview Bank

These are candidate-first prompts. The candidate sees only the prompt and initial scale. The interviewer uses the follow-ups gradually and does not teach during the attempt.

## 01 — URL Shortener (HLD)

**Prompt:** Design a service that creates short links and redirects users with high availability and low latency. Support expiry and click analytics.

**Probe:** key generation and collisions; 301 vs 302; read-after-write; cache hot keys; abuse; analytics off the critical path.

## 02 — Vending Machine (LLD, Java)

**Prompt:** Design and implement a vending machine that accepts money, selects products, dispenses an item and returns change.

**Probe:** state transitions; insufficient stock/change; cancellation; money precision; concurrency; interfaces; unit tests.

## 03 — Notification Platform (HLD)

**Prompt:** Design email, SMS and push notification delivery for transactional and bulk traffic with user preferences.

**Probe:** API/idempotency; topic/partition key; priority; retry and DLQ; provider failover; rate limits; delivery status.

## 04 — Movie Ticket Booking (LLD, Java)

**Prompt:** Design seat search, temporary holds, booking and cancellation for a multiplex.

**Probe:** double booking; hold expiry; optimistic vs pessimistic locking; payment boundary; state machine; concurrent tests.

## 05 — Distributed Rate Limiter (HLD)

**Prompt:** Design per-user and per-API rate limiting across many gateway instances.

**Probe:** token bucket vs sliding window; Redis atomicity; clock/skew; hot tenants; fail-open vs fail-closed; headers.

## 06 — Food Delivery (HLD)

**Prompt:** Design restaurant discovery, ordering, driver assignment and live order tracking for a city-scale service.

**Probe:** location indexing; order state; dispatch; event ordering; surge; retries; consistency; degraded mode.

## 07 — Media Storage and Retrieval (HLD)

**Prompt:** Design large media upload, metadata storage, thumbnail processing, retrieval and retention cleanup.

**Probe:** direct/object-store upload; multipart resume; async processing; metadata partitions; CDN/cache; orphan cleanup; observability.

## 08 — Ride Sharing (LLD, Java)

**Prompt:** Design trip request, driver matching, trip lifecycle and fare calculation.

**Probe:** strategy interfaces; state invariants; competing accepts; location updates; pricing extension; thread safety; tests.

## 09 — Payment Processing (HLD)

**Prompt:** Design payment initiation, provider integration, status tracking, refunds and reconciliation.

**Probe:** idempotency; immutable ledger; webhook duplicates/out-of-order events; outbox; saga; audit; PCI boundary; recovery.

## 10 — Weakest Design Re-solve (HLD or LLD)

**Prompt:** Repeat the lowest-scoring previous problem without notes and defend the exact follow-ups previously missed.

**Probe:** compare old/new decision; demonstrate fixed gaps; find the next bottleneck; explain what remains uncertain.

## Difficulty progression

- First attempt: normal interviewer prompts and one deep dive.
- Second attempt: 10× traffic plus one component failure.
- Third attempt: interviewer interruptions, requirement change and explicit trade-off defense.

Use [Mock 01](mock-01-url-shortener.md) to start. Store completed attempts under [mocks](mocks/README.md).
