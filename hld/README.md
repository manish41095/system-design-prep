# High-Level Design Tracker

Use 45–60 minutes for a timed attempt. Each problem should have a requirements document, architecture diagram, API/data model, critical-flow deep dive, failure analysis and mock feedback. Begin with the [HLD template](../templates/README.md).

| # | Problem | Priority | Status | Confidence /5 | Last attempt | Next action |
|---|---|---|---|---|---|---|
| 1 | Multi-tenant supply-chain SaaS | Very High | Not started | — | — | [Start exercise](multi-tenant-saas.md) |
| 2 | URL shortener | High | Learning | 3/5 | [Attempt 01 — 2026-09-15](../practice/mocks/2026-09-15-url-shortener-attempt-01.md) | Fix estimation/cache gaps; re-attempt 2026-09-24 |
| 3 | Rate limiter | High | Learning | 3/5 | [Attempt 01 — 2026-09-24](../practice/mocks/2026-09-24-rate-limiter-attempt-01.md) | Fix Redis failure and rule propagation; re-attempt 2026-10-03 |
| 4 | Notification system | High | Learning | 3/5 | [Attempt 01 — 2026-09-25](../practice/mocks/2026-09-25-notification-system-attempt-01.md) | Fix outbox, delivery deduplication and provider backpressure; re-attempt 2026-10-05 |
| 5 | File storage / media server | High | Not started | — | — | Upload, metadata, retrieval, cleanup |
| 6 | Food delivery | High | Not started | — | — | Ordering, dispatch, consistency |
| 7 | Ride sharing | High | Not started | — | — | Location, matching, trip state |
| 8 | Stock exchange / order matching | High | Not started | — | — | Ordering, matching, durability |
| 9 | Movie ticket booking | High | Not started | — | — | Seat contention and reservation TTL |
| 10 | E-commerce order and inventory | High | Not started | — | — | Saga, outbox, inventory reservation |
| 11 | Payment processing | High | Not started | — | — | Idempotency, reconciliation, ledger |
| 12 | Chat / messaging | Medium | Not started | — | — | WebSocket, ordering, delivery |
| 13 | News feed | Medium | Not started | — | — | Fan-out and hot users |
| 14 | Search / autocomplete | Medium | Not started | — | — | Indexing, ranking, freshness |
| 15 | Distributed job scheduler | High | Not started | — | — | Leases, retries, duplicate execution |
| 16 | Logging and metrics platform | Medium | Not started | — | — | Ingestion, retention, query |
| 17 | API gateway / service platform | High | Not started | — | — | Routing, auth, limits, discovery |
| 18 | Video streaming platform | Medium | Not started | — | — | Encoding, CDN, adaptive streaming |

## Required evidence for every design

- Functional and non-functional requirements with explicit assumptions.
- Scale estimates using units and stated assumptions.
- APIs, data model and a readable architecture diagram.
- One or two critical flows explained step by step.
- Storage, consistency, caching, messaging and security decisions with alternatives.
- Failure modes, recovery, bottlenecks, observability and capacity implications.
- A 2-minute summary, follow-up questions and a recorded mock score.

## Completion standard

A design is not complete when the diagram is drawn. Mark it interview ready only after a timed explanation, defensible trade-offs and a re-attempt that addresses previous weaknesses. Keep all scores unassessed until an actual attempt.
