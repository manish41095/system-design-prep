# Concept Knowledge Tracker

Update status, confidence, revision dates and evidence after each session. `—` means unassessed, not a judgment of existing experience. Use the [concept template](../templates/README.md) for detailed notes.

| Topic | Priority | Status | Confidence /5 | Last revised | Next action |
|---|---|---|---|---|---|
| Back-of-envelope estimation, QPS, storage, bandwidth | High | Can explain | 3/5 | 2026-09-24 | Add Redis memory, replicas and headroom to [Mock 02](../practice/mocks/2026-09-24-rate-limiter-attempt-01.md) |
| Latency, throughput, availability and SLOs | High | Learning | 3/5 | 2026-10-01 | Replace broad five-nines promise with separate browse, order and tracking SLOs |
| Horizontal vs vertical scaling | High | Learning | 3/5 | 2026-09-25 | Connect channel worker autoscaling to queue lag and provider quotas |
| Load balancing, L4/L7, health checks | High | Learning | 2/5 | 2026-09-15 | Explain gateway/LB roles and unhealthy-instance removal |
| DNS, HTTP/HTTPS, TLS, TCP, HTTP/2 and HTTP/3 | Medium | Not started | — | — | Trace request end-to-end |
| REST, gRPC, WebSocket and API versioning | High | Can explain | 3/5 | 2026-10-01 | Defend SSE for tracking and add reconnect/event-resume behavior |
| API gateway, service discovery and configuration | High | Learning | 2/5 | 2026-09-24 | Complete versioned rule propagation and reconciliation |
| Database indexes, composite indexes and query plans | High | Learning | 3/5 | 2026-10-01 | Explain composite order indexes and verify them with EXPLAIN |
| Transactions, isolation levels and MVCC | High | Not started | — | — | Explain concurrent updates |
| Optimistic/pessimistic locking | High | Can explain | 3/5 | 2026-10-01 | Demonstrate atomic driver claim and cancel-versus-accept race |
| Replication, read replicas and failover | High | Not started | — | — | Explain lag and consistency |
| Partitioning vs sharding | High | Not started | — | — | Design a growing table |
| Consistent hashing and virtual nodes | High | Learning | 2/5 | 2026-09-24 | Explain Redis hash-slot movement and hot-key limits |
| Unique ID and short-code generation | High | Can explain | 4/5 | 2026-09-15 | Defend block allocation and keyed obfuscation on re-attempt |
| SQL vs NoSQL and data-model trade-offs | High | Can explain | 4/5 | 2026-10-01 | Simplify OrderItem storage and defend location-history partitioning |
| CAP, consistency models and quorum | High | Not started | — | — | Explain a network partition |
| Caching patterns and TTL | High | Learning | 3/5 | 2026-09-24 | Set token-bucket idle TTL from refill-to-full time plus buffer |
| Cache invalidation, stampede and hot keys | High | Learning | 1/5 | 2026-09-15 | Explain expiry, single-flight, jitter and hot-key mitigation |
| Redis data structures and distributed cache | High | Can explain | 3/5 | 2026-09-24 | Explain Redis failure, fallback and protected backend behavior |
| Kafka topics, partitions, consumer groups | High | Learning | 3/5 | 2026-09-25 | Explain topic layout, partition key, consumer groups and lag |
| Delivery semantics, retries and DLQ | High | Can explain | 3/5 | 2026-10-01 | Replace exactly-once claim with effectively-once business handling |
| Idempotency and transactional outbox | High | Can explain | 3/5 | 2026-10-01 | Add durable order idempotency and consumer inbox to the outbox design |
| Saga and distributed transactions | High | Can explain | 4/5 | 2026-10-01 | Explain durable orchestrator recovery, timers and idempotent compensation |
| Rate limiting and backpressure | High | Can explain | 3/5 | 2026-09-24 | Re-attempt reliability and multi-region follow-ups on 2026-10-03 |
| Timeouts, retries, circuit breakers, bulkheads | High | Learning | 2/5 | 2026-09-25 | Apply timeout, Retry-After, jitter and circuit breaker to providers |
| Distributed locks, leases and fencing tokens | High | Learning | 2/5 | 2026-10-01 | Compare Redis lock with database constraint and assignment lease |
| Leader election and coordination | Medium | Can explain | 3/5 | 2026-09-15 | Explain allocator failover and block durability |
| Object storage, CDN and presigned URLs | High | Not started | — | — | Design media upload/download |
| Search indexing and eventual consistency | Medium | Can explain | 3/5 | 2026-10-01 | Explain Menu DB to Elasticsearch outbox synchronization |
| OAuth2, OIDC, JWT and service authentication | High | Not started | — | — | Trace authenticated request |
| Multi-tenancy and tenant isolation | Very High | Not started | — | — | Complete SaaS design exercise |
| Tenant-aware caching, jobs and observability | Very High | Not started | — | — | Prove cross-tenant isolation |
| Logs, metrics, traces and correlation IDs | High | Not started | — | — | Debug a slow request |
| SLI/SLO, alerting and error budgets | Medium | Not started | — | — | Define service objectives |
| Capacity planning, load testing and bottlenecks | High | Can explain | 4/5 | 2026-10-01 | Add 6,667 location events per second and replication overhead |
| Deployment, rolling/canary release and rollback | Medium | Not started | — | — | Plan zero-downtime deployment |
| Backup, restore, RPO/RTO and disaster recovery | High | Not started | — | — | Design recovery plan |
| Java concurrency, executors and CompletableFuture | High | Not started | — | — | Explain pool sizing and blocking I/O |
| Spring Boot transaction boundaries and connection pools | High | Not started | — | — | Explain transaction + async pitfalls |
| JVM memory, GC and performance diagnosis | High | Not started | — | — | Investigate a latency spike |

## How to study one concept

Explain what it is, why it is needed, how it works internally, a short example, limitations/trade-offs, real-project usage and follow-up questions. Then apply it to a design and explain it aloud without notes.

## Revision

Use the last successful recall date to plan revision after approximately 1, 3, 7 and 14 days, adapting based on difficulty. Failed recall means revise and reassess; do not artificially increase confidence. Keep a short error log of concepts that repeatedly cause difficulty.
