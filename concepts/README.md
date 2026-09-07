# Concept Knowledge Tracker

Update status, confidence, revision dates and evidence after each session. `—` means unassessed, not a judgment of existing experience. Use the [concept template](../templates/README.md) for detailed notes.

| Topic | Priority | Status | Confidence /5 | Last revised | Next action |
|---|---|---|---|---|---|
| Back-of-envelope estimation, QPS, storage, bandwidth | High | Not started | — | — | Estimate a URL shortener |
| Latency, throughput, availability and SLOs | High | Not started | — | — | Explain trade-offs with examples |
| Horizontal vs vertical scaling | High | Not started | — | — | Design stateless scaling |
| Load balancing, L4/L7, health checks | High | Not started | — | — | Explain failure handling |
| DNS, HTTP/HTTPS, TLS, TCP, HTTP/2 and HTTP/3 | Medium | Not started | — | — | Trace request end-to-end |
| REST, gRPC, WebSocket and API versioning | High | Not started | — | — | Select API style for a use case |
| API gateway, service discovery and configuration | High | Not started | — | — | Design service communication |
| Database indexes, composite indexes and query plans | High | Not started | — | — | Explain EXPLAIN and index order |
| Transactions, isolation levels and MVCC | High | Not started | — | — | Explain concurrent updates |
| Optimistic/pessimistic locking | High | Not started | — | — | Prevent lost updates |
| Replication, read replicas and failover | High | Not started | — | — | Explain lag and consistency |
| Partitioning vs sharding | High | Not started | — | — | Design a growing table |
| Consistent hashing and virtual nodes | High | Not started | — | — | Explain node addition/removal |
| SQL vs NoSQL and data-model trade-offs | High | Not started | — | — | Choose storage for three workloads |
| CAP, consistency models and quorum | High | Not started | — | — | Explain a network partition |
| Caching patterns and TTL | High | Not started | — | — | Design cache-aside |
| Cache invalidation, stampede and hot keys | High | Not started | — | — | Handle stale/hot data |
| Redis data structures and distributed cache | High | Not started | — | — | Choose keys and expiry |
| Kafka topics, partitions, consumer groups | High | Not started | — | — | Explain ordering and scaling |
| Delivery semantics, retries and DLQ | High | Not started | — | — | Handle duplicate events |
| Idempotency and transactional outbox | High | Not started | — | — | Design reliable order events |
| Saga and distributed transactions | High | Not started | — | — | Design compensating actions |
| Rate limiting and backpressure | High | Not started | — | — | Implement token bucket |
| Timeouts, retries, circuit breakers, bulkheads | High | Not started | — | — | Prevent cascading failures |
| Distributed locks, leases and fencing tokens | High | Not started | — | — | Explain stale lock owner |
| Leader election and coordination | Medium | Not started | — | — | Design single-active worker |
| Object storage, CDN and presigned URLs | High | Not started | — | — | Design media upload/download |
| Search indexing and eventual consistency | Medium | Not started | — | — | Design search synchronization |
| OAuth2, OIDC, JWT and service authentication | High | Not started | — | — | Trace authenticated request |
| Multi-tenancy and tenant isolation | Very High | Not started | — | — | Complete SaaS design exercise |
| Tenant-aware caching, jobs and observability | Very High | Not started | — | — | Prove cross-tenant isolation |
| Logs, metrics, traces and correlation IDs | High | Not started | — | — | Debug a slow request |
| SLI/SLO, alerting and error budgets | Medium | Not started | — | — | Define service objectives |
| Capacity planning, load testing and bottlenecks | High | Not started | — | — | Analyze a bounded thread pool |
| Deployment, rolling/canary release and rollback | Medium | Not started | — | — | Plan zero-downtime deployment |
| Backup, restore, RPO/RTO and disaster recovery | High | Not started | — | — | Design recovery plan |
| Java concurrency, executors and CompletableFuture | High | Not started | — | — | Explain pool sizing and blocking I/O |
| Spring Boot transaction boundaries and connection pools | High | Not started | — | — | Explain transaction + async pitfalls |
| JVM memory, GC and performance diagnosis | High | Not started | — | — | Investigate a latency spike |

## How to study one concept

Explain what it is, why it is needed, how it works internally, a short example, limitations/trade-offs, real-project usage and follow-up questions. Then apply it to a design and explain it aloud without notes.

## Revision

Use the last successful recall date to plan revision after approximately 1, 3, 7 and 14 days, adapting based on difficulty. Failed recall means revise and reassess; do not artificially increase confidence. Keep a short error log of concepts that repeatedly cause difficulty.
