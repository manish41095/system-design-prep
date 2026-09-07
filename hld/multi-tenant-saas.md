# HLD 01 — Multi-tenant Supply-chain SaaS

**Priority:** Very High | **Status:** Not started | **Confidence:** Unassessed | **Last attempt:** —

This is a learning exercise, not a claim of production multi-tenancy experience. Use it to prepare for a Tech Lead discussion about building a SaaS platform from scratch.

## 1. Clarify requirements

Assume a platform serving separate companies. Each tenant manages its own users, products, warehouses, inventory, orders and reports. Ask whether tenants can have multiple organizations, whether cross-tenant collaboration is permitted, what isolation/compliance requirements exist, expected tenant count, data volume, peak traffic, regional residency, availability and recovery targets. Clarify tenant onboarding, subscription limits and tenant-specific configuration.

Do not invent scale figures in the interview. State assumptions and calculate capacity from them.

## 2. Tenancy models

| Model | Advantages | Costs / risks | Suitable when |
|---|---|---|---|
| Shared DB, shared schema with tenant_id | Low cost, simple infrastructure, efficient pooling | Every access path must enforce isolation; noisy neighbors | Many smaller tenants with comparable requirements |
| Shared DB, separate schema | Better logical separation, easier schema-scoped operations | Migrations, connection/search-path handling and tenant count complexity | Moderate number of tenants needing stronger separation |
| Database per tenant | Stronger operational separation, tenant-specific backup/restore and placement | Higher cost, connection management, provisioning and migration overhead | Large or regulated tenants with stronger isolation needs |

Choose based on requirements, not a universal rule. A hybrid model can move larger tenants to dedicated databases. Shared-schema isolation is logical, not the same as physical isolation. Row-level security can provide defense in depth but requires correct policies and database identity/context management.

## 3. Architecture

```text
Client / Tenant domain
        |
      HTTPS
        |
API Gateway / Load Balancer
        |
Authentication + tenant membership validation
        |
Tenant-aware application services
   |         |           |
Catalog   Orders     Inventory
   |         |           |
   +---- Tenant-aware data access ----+
   |                |                 |
Tenant config    Redis/cache     Message broker
                    |                 |
              Background workers       |
                    |                 |
             Tenant-scoped data stores
                    |
          Logs / metrics / tracing
```

Start with a modular monolith if scale and team boundaries do not justify microservices. Extract services when independent scaling, ownership or deployment needs justify the operational cost.

## 4. Request and authorization flow

1. Authenticate the user using a trusted identity provider or authentication service.
2. Resolve the requested tenant from a trusted domain, route or token claim.
3. Verify the authenticated principal is an active member of that tenant and has permission for the requested action.
4. Establish a validated tenant context for the request; never treat an arbitrary frontend tenant ID as authorization.
5. Apply tenant scope in the service and persistence layer; validate resource ownership before mutations.
6. Clear request context reliably. Do not assume ThreadLocal automatically propagates to async work, CompletableFuture or reused executor threads.
7. Include tenant identity in safe audit metadata and enforce isolation for every downstream call.

For a user who belongs to multiple tenants, switching tenant must trigger fresh membership/authorization checks. Prefer explicit tenant arguments at service boundaries where it improves correctness and testability. Authentication answers who the user is; authorization answers what that user can do in a particular tenant.

## 5. Data model and APIs

Illustrative shared-schema tables:

```sql
CREATE TABLE tenants (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  status TEXT NOT NULL
);
CREATE TABLE tenant_memberships (
  tenant_id UUID NOT NULL REFERENCES tenants(id),
  user_id UUID NOT NULL,
  role TEXT NOT NULL,
  PRIMARY KEY (tenant_id, user_id)
);
CREATE TABLE products (
  tenant_id UUID NOT NULL REFERENCES tenants(id),
  id UUID NOT NULL,
  sku TEXT NOT NULL,
  name TEXT NOT NULL,
  PRIMARY KEY (tenant_id, id),
  UNIQUE (tenant_id, sku)
);
```

Other tenant-owned tables should follow a consistent ownership model. Composite foreign keys can enforce same-tenant relationships. Index common queries with tenant_id leading where appropriate, based on actual query plans and selectivity. Never assume an index is useful without checking the workload.

Example APIs: `POST /tenants`, `POST /tenants/{tenantId}/members`, `GET /products`, `POST /orders`, `POST /inventory/reservations`. The tenant in a path is a requested scope, not proof of access. Check permissions on every operation, including exports and administrative endpoints.

## 6. Caching and asynchronous work

Cache keys must include tenant identity and relevant authorization/configuration scope, for example `tenant:{id}:product:{id}`. Separate tenant-wide from user-specific caches. Apply quotas, TTLs and invalidation to limit noisy-neighbor effects. Avoid exposing sensitive tenant identifiers or data unnecessarily in telemetry.

Events should carry a validated tenant identifier, event ID, schema version and correlation ID. Consumers must validate the event source and tenant scope before processing. Use an outbox when reliable publication must follow a database transaction; use idempotent consumers and deduplication keys to handle retries. Tenant context must be explicitly restored for jobs and messages, not inherited accidentally from a worker thread. Dead-letter queues, retries and replay tools must also preserve isolation.

## 7. Isolation, scaling and operations

Consider per-tenant rate limits, resource quotas, connection pools, job fairness, large-tenant throttling and tenant-aware monitoring. Avoid a single large tenant exhausting shared workers or database connections. Plan tenant onboarding, migrations, backup/restore, data export/deletion, regional placement and tenant migration between tenancy models. Test tenant-specific restore rather than assuming a shared database backup provides it easily.

Measure p95/p99 latency, errors, queue lag, database saturation and per-tenant resource usage. Define availability and recovery objectives from business requirements. Include security testing, audit logging, encryption, secret management and least-privilege access.

## 8. Critical interview follow-ups

- How do you guarantee every query is tenant-scoped, including native SQL and admin jobs?
- What happens when a user changes tenants during a session?
- How does tenant context propagate through CompletableFuture and Kafka consumers?
- How do you prevent cache-key collisions and cross-tenant data leaks?
- How would you migrate one large tenant to a dedicated database with minimal downtime?
- How do you handle per-tenant migrations and restore one tenant's data?
- How do you prevent noisy neighbors and enforce subscription quotas?
- How would you test isolation with two tenants using identical product IDs/SKUs?
- What changes if tenants require different regions or encryption keys?
- Why choose a modular monolith instead of microservices initially?

## 9. Practice deliverables

Create a diagram, explicit assumptions and estimates, API/data model, tenant-resolution and order flows, tenancy-model comparison, security threat analysis and failure/recovery plan. Implement a small Spring Boot proof of concept with two tenants, membership checks, tenant-scoped persistence and negative isolation tests. Keep it separate from production code.

**Mock score:** — /50 | **Top gaps:** — | **Next revision:** —
