# Low-Level Design Tracker

Practice for 45–60 minute Java interviews. Start with requirements, entities, class diagram, interfaces and relationships. Implement the critical behavior, write tests and explain trade-offs. Prefer simple, working code over unnecessary patterns.

| # | Problem | Priority | Status | Confidence /5 | Last attempt | Next action |
|---|---|---|---|---|---|---|
| 1 | Vending machine | High | Learning | 2/5 | [Attempt 01 — 2026-10-02](../practice/mocks/2026-10-02-vending-machine-attempt-01.md) | Rebuild checkout, exact change and rollback; re-attempt 2026-10-12 |
| 2 | Parking lot | High | Not started | — | — | Allocation, pricing, concurrency |
| 3 | Elevator | High | Not started | — | — | Scheduling, state, safety |
| 4 | Library management | Medium | Not started | — | — | Copy vs book, lending rules |
| 5 | Movie ticket booking | High | Not started | — | — | Seat locking, expiry, payment |
| 6 | Food delivery | High | Not started | — | — | Order lifecycle, strategy, events |
| 7 | Ride sharing | High | Not started | — | — | Matching, trip state, pricing |
| 8 | Stock exchange / matching engine | High | Not started | — | — | Price-time priority, order book |
| 9 | Splitwise | High | Not started | — | — | Expense splitting, balances |
| 10 | ATM | Medium | Not started | — | — | State, cash dispensing, failure |
| 11 | In-memory cache | High | Not started | — | — | LRU, TTL, concurrency |
| 12 | Rate limiter | High | Not started | — | — | Clock, token bucket, tests |
| 13 | Logger framework | High | Not started | — | — | Levels, appenders, async queue |
| 14 | Notification system | High | Not started | — | — | Strategy, factory, retry |
| 15 | [Task scheduler](../practice/mocks/2026-10-10-job-scheduler-lld-attempt-01.md) | High | Learning | 3.9/5 (guided) | [Attempt 01 — 2026-10-10](../practice/mocks/2026-10-10-job-scheduler-lld-attempt-01.md) | Unaided Java implementation; cancellation race and lease recovery |
| 16 | File storage / media retrieval | High | Not started | — | — | Pluggable storage and fallback |

## Design-pattern tracker

| Pattern | Status | Confidence /5 | Practice application |
|---|---|---|---|
| Strategy | Learning | 3.5/5 | Job scheduling and retry-policy strategies |
| Factory / Abstract Factory | Learning | 3/5 | Job command registry runtime selection (registry, not necessarily Abstract Factory) |
| State | Learning | 3/5 | [Vending machine states](vending-machine/VendingMachine.java) and order lifecycle |
| Observer | Not started | — | Domain events / notifications |
| Decorator | Not started | — | Logging, compression, validation |
| Adapter | Not started | — | External payment / storage integration |
| Command | Learning | 3.5/5 | JobCommand implementations for email, cleanup, report |
| Chain of Responsibility | Not started | — | Validation / request processing |
| Builder | Not started | — | Complex immutable objects |
| Singleton | Learning | 2/5 | Explain why per-machine lifecycle does not require a global Singleton |
| Template Method | Not started | — | Shared processing with extension points |
| Repository | Learning | 3.5/5 | JobInstanceRepository with atomic claims and retry persistence |

## Java implementation checklist

Define interfaces and domain invariants. Use appropriate collections and encapsulation. Handle invalid transitions, errors and boundary cases. Explain thread safety when shared mutable state exists. Include unit tests for normal, failure and concurrency cases where relevant. Do not add synchronized blocks, patterns or databases without a requirement. Record what is implemented versus what is only proposed.

Use the [LLD template](../templates/README.md) for each attempt. Keep interview notes and production code clearly separated.
