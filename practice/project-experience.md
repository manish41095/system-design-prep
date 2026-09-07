# Real-project Experience and Interview Stories

Use genuine experience to connect system-design concepts to production engineering. The examples below are preparation prompts based on previously discussed work, not verified claims about ownership, scale or business outcomes. Confirm each fact before using it in an interview. Do not publish proprietary implementation details, internal endpoints, customer data or confidential metrics.

## Story template

**Context:** What business or technical problem existed?

**My responsibility:** What did I personally design, implement, review or operate?

**Constraints:** What were the actual scale, latency, reliability or compatibility requirements?

**Decision:** What alternatives did I consider, and why did I choose this approach?

**Implementation:** Explain the relevant architecture and critical flow at a safe, non-confidential level.

**Result:** Record measured results only. If metrics are unavailable, describe the verified functional outcome without inventing percentages.

**Trade-offs and learning:** What would I change, and what follow-up questions should I be ready for?

## Candidate stories to validate

| Topic | Experience to reconstruct | Concepts to connect | Evidence / verified outcome |
|---|---|---|---|
| Media ingestion under load | FTP worker-pool saturation and blocking I/O investigation | Bounded queues, backpressure, pool sizing, capacity testing | — |
| Metadata and storage retrieval | Cache, filesystem and database lookup behavior | Cache-aside, consistency, fallback, latency | — |
| Database partitioning | Partition preparation, retention and recovery scenarios | Partitioning, operational reliability, migrations | — |
| Asynchronous processing | IPCam control/image pairing and cleanup coordination | State machines, concurrency, idempotency, failure recovery | — |
| Caching | Typed caches, expiry and lookup behavior | Cache keys, TTL, invalidation, memory bounds | — |
| Security and service design | Relevant Java/Spring Boot authentication or authorization work | JWT, authorization, trust boundaries | — |
| Multi-tenancy | No direct production ownership claimed | Learning exercise, design alternatives, isolation | — |

## Tech Lead interview positioning

“I have not had direct production ownership of multi-tenancy yet. My backend experience gives me a foundation in persistence, security, concurrency and system reliability. I have studied the common tenancy models and would begin by clarifying isolation, scale and compliance requirements before selecting a design. I can explain the trade-offs and have built a separate practice design to deepen that area.”

Do not claim that a learning exercise is production experience. For leadership questions, distinguish personal implementation, team collaboration, architectural recommendations and decisions you actually owned. Prepare examples of technical trade-offs, debugging, mentoring/reviewing and handling incidents only where supported by real experience.
