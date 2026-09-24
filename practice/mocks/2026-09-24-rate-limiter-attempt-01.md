# Mock 02 — Distributed Rate Limiter — Attempt 01

| Field | Value |
|---|---|
| Date | 2026-09-24 |
| Type | HLD |
| Evaluator | ChatGPT interviewer |
| Status | Stopped before Redis-failure and reliability deep dive |
| Score | 36/50 — 7.2/10 |
| Verdict | Lean Hire / lower-end Pass for an incomplete attempt |
| Re-attempt due | 2026-10-03 |

> The score is based only on answers given during the mock. The corrected design and possible interview questions later in this document did not increase the score.

## Original prompt

Design a distributed rate limiter for an API Gateway. It must work across multiple gateway instances, support configurable limits and add very little latency.

## My actual interview answers

### Q1 — Functional requirements

**Interviewer:** What functional requirements would you include, and what would you keep out of scope?

**My answer:**

1. The system should rate-limit requests by user ID, IP address or device ID.
2. The system should inform the user when the limit is exceeded.
3. Rules should be dynamically configurable.
4. Different limits for premium customers are out of scope for the first version.

### Q2 — Non-functional requirements

**Interviewer:** What non-functional requirements matter most?

**My answer:**

1. The system should be highly available, targeting 99.999% uptime.
2. It should add very low latency because it is on every request path.
3. It should scale to one billion monthly active users and 100 million daily active users.

### Q3 — Traffic estimation

**Interviewer:** Estimate average and peak requests per second.

**My answer:**

Assume each of 100 million daily active users makes 100 API requests per day.

~~~text
100 million × 100 = 10 billion requests/day
10 billion / 86,400 ≈ 1.1 lakh requests/second average
Peak traffic ≈ 5 × average ≈ 5.5 lakh requests/second
~~~

### Q4 — Algorithm comparison

**Interviewer:** Which rate-limiting algorithm would you choose and why?

**My answer:**

Token bucket or sliding-window counter can handle this workload.

- Token bucket uses little memory and supports controlled bursts.
- Sliding-window counter keeps two small counters and uses a weighted average to reduce boundary spikes.
- Fixed window allows a large burst at the window boundary.
- Sliding-window log may use too much memory.
- Leaky bucket can add queueing latency.

**Follow-up:** Choose one algorithm.

**My response:**

I choose token bucket because it uses less memory and supports controlled traffic bursts. Redis will hold each rate-limit key and allow fast atomic operations.

### Q5 — Token-bucket state and flow

**Interviewer:** What exact state will Redis store, and how is a request evaluated?

**My answer:**

Redis stores:

1. Number of tokens currently available.
2. The timestamp when the bucket was last refilled or modified.

The key receives a TTL so inactive users are automatically removed.

Request flow:

1. Fetch the current state. If the key is absent, initialize it with full capacity and the current timestamp.
2. Calculate elapsed time and how many tokens should be refilled.
3. Update the token balance.
4. If enough tokens exist, consume a token and allow the request; otherwise reject it.

### Q6 — Atomicity

**Interviewer:** How do you prevent concurrent requests from exceeding the limit?

**My first answer:**

Use Redis Cluster because one Redis instance could become a bottleneck at roughly 1.1 lakh average requests per second. Redis shards keys using hash slots. Since the key contains user and endpoint, concurrent requests for the same key reach the same shard, which keeps the state together.

**Follow-up:** The same shard does not by itself make the read-modify-write sequence atomic. What Redis mechanism is needed?

**My corrected response:**

Redis processes commands serially on a shard. Run the complete calculation in a Lua script or Redis Function so the read, refill, decrement and write execute without interruption.

### Q7 — Architecture and request flow

**Interviewer:** Describe the architecture and request flow.

**My answer:**

Components:

1. Client sends the request.
2. The rate limiter runs at the API Gateway.
3. An admin portal stores or manages rate-limit rules.
4. A rule worker asynchronously fetches rules from the database and synchronizes them to API Gateway instances.
5. Redis tracks request state and timestamps.

Request flow:

1. Client sends a request.
2. API Gateway intercepts it.
3. The rate limiter reads a cached rule and finds the matching policy.
4. It evaluates the rule and atomically updates Redis state.
5. It either forwards the request to the backend service or rejects it.

**Follow-up:** The diagram omitted Redis and did not explain rule consistency across gateways.

**My response:**

I asked to move to the next question.

## Architecture I produced

This diagram reconstructs the submitted interview diagram. Redis was mentioned verbally but was missing from the drawing.

~~~mermaid
flowchart LR
    Client["Client"] --> Gateway["API Gateway with Rate Limiter"]
    Rules["Admin Portal and Policy DB"] --> Worker["Async Rule Worker"]
    Worker --> Gateway
    Gateway -->|Allow| Backend["Backend Service"]
    Gateway -->|Reject| Rejected["429 Response"]
~~~

### Q8 — Rejection response

**Interviewer:** What should be returned when the limit is exceeded?

**My answer:**

Return HTTP **429 Too Many Requests**.

~~~json
{
  "error": {
    "code": "too_many_requests",
    "message": "Rate limit exceeded. You have reached the maximum allocation of 100 requests per minute for your tier.",
    "retry_after_seconds": 30,
    "documentation_url": "https://request.com"
  }
}
~~~

**Follow-up:** Which response headers should be included?

**My response:**

~~~http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 30
RateLimit-Limit: 100
RateLimit-Remaining: 0
RateLimit-Reset: 30
~~~

### Q9 — Redis failure

**Interviewer:** What happens when Redis becomes slow or unavailable? Should the gateway fail open or fail closed, and how is the backend protected?

**My response:**

The mock ended before this question was answered.

## Scorecard

| Axis | Score /10 | Evidence from this attempt |
|---|---:|---|
| Requirements | 7.5 | User/IP/device limits, rejection feedback and dynamic rules were identified. Rule precedence, weighted requests, latency budget and consistency expectations were not clarified. |
| Architecture | 6.5 | Gateway placement, policy storage, async rule sync and Redis were identified. Redis was absent from the diagram, rule propagation was skipped, and failure/multi-region paths were missing. |
| Problem-solving | 8.0 | Algorithm comparison, token-bucket state and Lua atomicity were strong. Atomicity needed a follow-up because sharding alone was initially treated as sufficient. |
| Scale and trade-offs | 6.5 | Average and peak RPS were calculated correctly. Memory sizing, hot keys, Redis failure, regional quotas and accuracy trade-offs were not covered. |
| Communication | 7.5 | Answers were structured and concise, and the 429 contract was clear. One architecture clarification was skipped and the mock ended before the reliability close. |
| **Total** | **36/50** | **7.2/10 — Lean Hire / lower-end Pass for an incomplete attempt** |

## Progress compared with Mock 01

| Area | Mock 01 | Mock 02 |
|---|---|---|
| Total score | 34/50 | 36/50 |
| Estimation | Calculation was skipped | Average and peak RPS calculated correctly |
| Core crux | Strong Base62 ID design | Strong token-bucket and Lua design |
| Concurrency | Not deeply tested | Atomic Redis script reached after follow-up |
| Reliability close | Missing | Still missing |
| Diagram | Shared dependencies were unclear | Redis and policy-distribution paths were incomplete |

The improvement is real, but the same habit remains: the core algorithm is explained well, while failure handling and the final senior-level close are left unfinished.

## What went well

1. Average and peak RPS calculations were directionally correct.
2. Token bucket, sliding-window counter, fixed window, sliding-window log and leaky bucket were compared.
3. Token bucket was selected for a clear reason: controlled bursts and small state.
4. Redis state was correctly modeled using token count and last-refill time.
5. TTL was used to remove inactive buckets.
6. Redis Cluster was introduced for horizontal scaling.
7. Lua script or Redis Function was correctly used for atomic read-refill-decrement-write.
8. HTTP 429, Retry-After and rate-limit headers were provided correctly.
9. The rate limiter was placed at the API Gateway before backend execution.

## Corrections and improvements

| My answer | Corrected senior-level explanation |
|---|---|
| Premium-user limits are out of scope | That is a reasonable first-version exclusion, but the policy model should still support tiers later without redesign. |
| 99.999% availability | Five nines permits only a few minutes of downtime per year. This requires multi-AZ gateways, replicated Redis, no synchronous policy-DB lookup and a defined fallback mode. |
| TTL slightly longer than the rate-limit window | Token bucket has no fixed window. Set idle TTL to at least the time needed to refill from empty to full, plus a safety buffer. |
| Redis Cluster ensures atomicity | Cluster keeps one key on one shard, but atomicity comes from a Lua script or Redis Function. Every key used by the script must be on the same hash slot. |
| Redis is single-threaded | The important interview point is that one Lua script/Function executes atomically on the owning primary shard. Do not depend on a general statement about Redis threads. |
| Admin portal is persistent storage | The admin portal is the UI. A Policy Service validates writes and stores versioned policies in a durable Policy DB. |
| Rule worker syncs rules | Explain delivery: publish a versioned event, update local gateway caches, retry failed delivery, and periodically reconcile with the source of truth. |
| Redis stores request count and timestamp | For token bucket, store remaining tokens and last-refill timestamp. A count belongs more naturally to window-based algorithms. |
| Diagram omits Redis | Redis is on the request path and must appear in the diagram, along with the local rule cache and monitoring path. |
| RateLimit-Reset: 30 | Clearly define whether the value means seconds until reset or an absolute timestamp, and keep the API consistent. Retry-After may be seconds or an HTTP date. |

## Correct traffic calculation

~~~text
100,000,000 DAU × 100 requests/day
= 10,000,000,000 requests/day

10,000,000,000 / 86,400
≈ 115,741 requests/second average

115,741 × 5
≈ 578,705 requests/second peak
~~~

Architectural consequence:

- The rate limiter cannot call a relational policy database on every request.
- Rules must be cached locally at gateways.
- Token state must be distributed across a Redis Cluster.
- The Lua script must be small and constant-time.
- Capacity must be validated with load testing at peak traffic plus headroom.

## Memory estimate

Assume one active bucket needs approximately 200 bytes after key, values and Redis overhead.

~~~text
100 million active buckets × 200 bytes
≈ 20 GB raw memory

With one replica:
≈ 40 GB before operational headroom
~~~

The exact size must be measured because Redis object overhead and key length matter. TTL removes inactive keys. If one user has separate buckets for many endpoints, multiply the estimate by the average number of active user-endpoint combinations.

## Correct key and token state

Example key:

~~~text
rl:{user:12345}:GET:/orders
~~~

The hash tag keeps related Lua-script keys in one Redis hash slot.

Stored state:

| Field | Meaning |
|---|---|
| tokens | Current token balance |
| last_refill_ms | Last refill timestamp |
| policy_version | Optional version used for debugging/migration |

Policy state is separate:

| Field | Meaning |
|---|---|
| capacity | Maximum burst size |
| refill_rate | Tokens added per second |
| request_cost | Tokens consumed by this request |
| scope | User, IP, device, tenant or API |
| version | Monotonic policy version |

## Correct atomic token-bucket operation

For every request:

1. Read current tokens and last-refill time inside one Lua script.
2. Use Redis server time to reduce clock differences between gateways.
3. Calculate elapsed time.
4. Add tokens: **min(capacity, oldTokens + elapsed × refillRate)**.
5. If tokens are at least the request cost, subtract cost and allow.
6. Otherwise reject and calculate retry-after time.
7. Save new state and refresh idle TTL.
8. Return allow/reject, remaining tokens and retry-after in one result.

## Corrected architecture

~~~mermaid
flowchart TB
    Client["Client"] --> Edge["WAF and Load Balancer"]
    Edge --> Gateways["API Gateway Cluster"]
    Gateways --> Filter["Rate Limiter Filter"]
    Filter --> Rules["Local Rule Cache"]
    Filter --> Redis["Redis Cluster with Replicas"]
    Filter -->|Allow| Backend["Backend Services"]
    Filter -->|Reject| Response["429 Response"]

    Admin["Admin Portal"] --> Policy["Policy Service"]
    Policy --> DB["Versioned Policy DB"]
    Policy --> Events["Policy Update Stream"]
    Events --> Rules

    Filter --> Metrics["Metrics and Alerts"]
~~~

### Responsibility of each component

| Component | Responsibility |
|---|---|
| API Gateway filter | Build key, load rule, call atomic Redis script and allow/reject |
| Local rule cache | Avoid policy-database lookup on every request |
| Redis Cluster | Store distributed token state |
| Policy Service | Validate and version rule changes |
| Policy DB | Durable source of truth for rules |
| Policy event stream | Push updates to gateways |
| Reconciliation worker | Repair missed/stale rule updates |
| Monitoring | Track latency, rejection, Redis errors and fallback behaviour |

## Correct request flow

1. WAF performs basic network-level protection.
2. API Gateway authenticates the caller when a user-based key is needed.
3. Rate-limiter filter builds keys for applicable scopes, such as IP, user and endpoint.
4. It loads the latest rule from its local in-memory cache.
5. It calls the Redis Lua script.
6. Redis atomically refills and consumes tokens.
7. If allowed, the gateway forwards the request to the backend.
8. If rejected, it returns 429 with Retry-After and quota headers.
9. Metrics are emitted asynchronously.

## Missing answer — Redis slow or unavailable

There is no single fail-open/fail-closed answer for every API.

### Fail closed

Reject or strongly restrict requests when protecting:

- Login and password-reset endpoints
- Payment or money-transfer APIs
- Expensive writes
- Security-sensitive operations

This protects the system and prevents abuse, but legitimate traffic may be rejected during a Redis outage.

### Fail open with a local emergency limit

Allow limited traffic when protecting:

- Low-risk reads
- Public content
- Endpoints where availability matters more than strict quota accuracy

Do not allow unlimited traffic. Each gateway should use a small local token bucket with conservative limits.

### Protect the backend

- Use Redis replicas and automatic failover.
- Set a very short Redis timeout.
- Use a circuit breaker to stop repeated slow calls.
- Apply a local emergency rate limit.
- Add per-service concurrency limits and bulkheads.
- Load-shed low-priority requests.
- Monitor fallback rate and Redis latency.
- Recover gradually to avoid a traffic surge.

A good interview answer is:

> For security-sensitive and expensive operations, I fail closed. For low-risk reads, I fail open only through a conservative local limiter. In both cases, short Redis timeouts, a circuit breaker, backend concurrency limits and load shedding prevent an outage from becoming a backend overload.

## Missing answer — rule propagation and consistency

1. Admin changes a rule through the Policy Service.
2. Policy Service validates it and saves a new version in the Policy DB.
3. It publishes a **PolicyChanged(version)** event.
4. Gateways update their local Caffeine-style rule caches.
5. A background poll compares versions periodically and repairs missed events.
6. Gateways use the last-known-good rule if the DB or event stream is unavailable.
7. Emergency deny rules can use a separate fast control channel if required.

This gives eventual consistency for normal policy changes without adding a database call to every request.

## Missing answer — hot keys

Most user-specific keys distribute well. Hot keys appear with:

- A shared public IP or NAT gateway
- One very large tenant
- A global endpoint-wide limit
- One shared API key

Mitigations:

- Use hierarchical limits: local gateway quota plus global Redis quota.
- Pre-allocate portions of a global quota to gateways.
- Separate large tenants into dedicated partitions.
- Detect hot keys and alert.
- Avoid one global counter for the whole platform.
- Use local aggregation when small bounded inaccuracy is acceptable.

## Missing answer — multi-region design

A strictly global token bucket requires cross-region coordination on every request, which adds latency and creates a global dependency.

A practical design:

1. Store the global policy centrally.
2. Allocate part of the quota to each region.
3. Enforce each regional quota using regional Redis.
4. Periodically rebalance quota based on traffic.
5. Accept small temporary overuse during failover as a documented trade-off.

Use a strict globally coordinated limit only for rare high-risk operations where correctness matters more than latency and availability.

## Missing answer — combining multiple limits

A request may need to satisfy all applicable policies:

- IP limit before authentication
- User limit after authentication
- Device limit
- Tenant limit
- Endpoint limit
- Global emergency limit

The request is allowed only when every mandatory bucket has tokens. If a Lua script touches several Redis keys, place related keys in one hash slot or evaluate independent scopes carefully. Avoid partially consuming tokens when another required limit rejects the request.

## Missing answer — observability

Track:

- Allowed and rejected request counts by rule
- Rate-limiter p50, p95 and p99 latency
- Redis command and Lua-script latency
- Redis timeout/failure count
- Local-fallback and fail-open/fail-closed count
- Policy version and stale-rule age
- Hot keys and shard imbalance
- Backend overload after limiter failures
- 429 rate by tenant, API and region
- Memory, key count and eviction rate

Alerts should detect stale policies, high fallback usage, Redis saturation and unusual rejection spikes.

## Possible interview questions and short answers

### 1. Why token bucket instead of fixed window?

Token bucket supports controlled bursts and avoids fixed-window boundary spikes. It stores only token balance and last-refill time.

### 2. What is the refill formula?

~~~text
newTokens = min(capacity, oldTokens + elapsedTime × refillRate)
~~~

Allow when **newTokens >= requestCost**, then subtract the cost.

### 3. Why use Lua instead of GET followed by SET?

GET plus SET is a race under concurrent requests. Lua executes the whole calculation atomically on the owning Redis shard.

### 4. Does Redis Cluster automatically guarantee token-bucket correctness?

No. It distributes keys and keeps one key on one shard. The Lua script provides atomicity. Multi-key scripts must use keys in the same hash slot.

### 5. How do you avoid clock skew?

Use Redis server time inside the script or pass a trusted monotonic server timestamp. Do not trust client timestamps.

### 6. How is an inactive bucket removed?

Give the key an idle TTL at least as long as the time required for an empty bucket to become full, plus a safety buffer.

### 7. How do dynamic rules reach all gateways?

Save a versioned policy, publish an update event, refresh local gateway caches and periodically reconcile versions.

### 8. What happens if a gateway misses an update?

It continues with the last-known-good rule. A periodic version check detects and repairs stale entries.

### 9. What happens if Redis fails?

Use a short timeout, circuit breaker and local emergency limiter. Fail closed for high-risk APIs and fail open with conservative limits for low-risk reads.

### 10. How do you handle a global limit across many gateways?

For exact enforcement, use one atomic Redis key, which may become hot. At high scale, allocate local quota portions to gateways or regions and accept bounded inaccuracy.

### 11. What is a hot-key example?

A global endpoint limit or a shared NAT IP can send huge traffic to one Redis key and one shard.

### 12. How do you support weighted requests?

Assign each operation a token cost. A normal read may cost one token while an expensive report may cost ten.

### 13. How do you apply IP and user limits together?

Evaluate both policies and allow only if both pass. Ensure rejection does not leave partially consumed tokens, or define an accepted rollback/ordering strategy.

### 14. Should authentication run before rate limiting?

Apply a coarse IP limit before authentication to stop anonymous abuse. Apply the user/tenant limit after identity is known.

### 15. What if one Redis shard becomes overloaded?

Redis Cluster can move hash slots, but one hot key cannot be split automatically. Use local quota allocation, dedicated partitions or bounded approximate counting.

### 16. How would you test the limiter?

Test refill math, empty/full buckets, exact boundaries, concurrent requests, TTL, Lua atomicity, clock behaviour, rule changes, Redis timeout, failover and load at peak RPS.

### 17. How do you prevent policy mistakes?

Validate capacity/refill values, use versioned changes, audit logs, staged rollout, dry-run mode and fast rollback.

### 18. How do you support multiple service tiers later?

Add tier and tenant attributes to policy matching. Keep the execution algorithm unchanged; only policy selection changes.

### 19. How accurate is the system during regional failover?

A region may temporarily consume more than its global share. State this bounded-overuse trade-off and reconcile/rebalance quota after failover.

### 20. Why cache rules locally but keep counters in Redis?

Rules change infrequently and are safe to cache. Token state changes on every request and must be shared across gateway instances.

## Two-minute corrected interview summary

> I would implement the rate limiter as a filter in every API Gateway instance. Policies are stored in a versioned Policy DB and distributed asynchronously to local gateway caches, so the request path never calls the database.
>
> For enforcement, I choose token bucket because it uses small state and supports controlled bursts. The key is based on scope, caller and endpoint. Redis stores remaining tokens and the last-refill timestamp. One Lua script uses Redis time, refills tokens, consumes the request cost and returns allow or reject atomically.
>
> At about 116,000 average and 579,000 peak requests per second, I would use a replicated Redis Cluster and stateless gateway instances. For a Redis failure, sensitive writes fail closed, while low-risk reads use a conservative local fallback limiter. Short timeouts, a circuit breaker, backend concurrency limits and load shedding protect downstream services.
>
> Multi-region enforcement uses regional Redis and allocated regional quotas because strict global coordination would add too much latency. I would monitor limiter latency, rejection rate, Redis errors, stale policies, fallback usage, memory and hot keys. The main trade-off is strict global accuracy versus latency and availability.

## Top three gaps and corrective exercises

| Gap | Root cause | Corrective exercise | Due | Verified? |
|---|---|---|---|---|
| Redis failure behaviour was unanswered | The mock stopped before the reliability deep dive | Explain fail-open/fail-closed plus backend protection in two minutes | 2026-09-26 | No |
| Rule propagation was skipped | Architecture named a worker but did not explain consistency or recovery | Draw versioned event update plus reconciliation flow | 2026-09-27 | No |
| Hot keys and multi-region limits were missing | Scaling focused on Redis Cluster but not cross-region/global coordination | Compare strict global bucket with regional quota allocation | 2026-09-29 | No |

## Re-attempt plan

Re-attempt the rate limiter on **2026-10-03** without reading the corrected answer during the mock.

Attempt 02 passes only if:

- The architecture diagram includes Redis, local rule cache, Policy Service, DB and update stream.
- Redis failure behaviour is answered without prompting.
- Rule propagation, stale rules and recovery are explained.
- Hot-key and multi-region trade-offs are covered.
- A two-minute final summary is completed.
