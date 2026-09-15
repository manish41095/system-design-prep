# Mock 01 — URL Shortener — Attempt 01

| Field | Value |
|---|---|
| Date | 2026-09-15 |
| Type | HLD |
| Evaluator | ChatGPT interviewer |
| Status | Stopped during cache deep dive |
| Score | 34/50 — 6.8/10 |
| Verdict | Borderline / Lean Hire for an incomplete attempt |
| Re-attempt due | 2026-09-24 |

> The score is based only on answers given during the mock. The corrected design later in this document did not increase the score.

## Original prompt

Design a URL-shortening platform similar to TinyURL. It should create short links, redirect users, support expiration and click analytics, and handle billions of links with high availability and low redirect latency.

## My actual interview answers

### Q1 — Requirements

**Interviewer:** What is in scope, and what can we explicitly defer?

**My answer:**

Functional requirements:

1. Shorten a long URL to a seven-character code.
2. Redirect a short URL to the original URL.
3. Track click analytics per URL.

Custom short codes for premium users are out of scope.

Non-functional requirements:

- The system should be highly available.
- Redirect latency should be low.
- The system should scale to billions of URLs.

**Follow-up:** Should shortened URLs remain permanent, or should the system support expiration and deletion?

**My response:**

The system should support a configurable expiration time ranging from months to years, depending on the business requirement.

### Q2 — Traffic and storage estimation

**Interviewer:** What traffic and storage assumptions would you use?

**My answer:**

- 100 million URLs are created per month.
- 10 billion redirects happen per month.
- The system is read-heavy, with a 100:1 read/write ratio.
- Peak traffic can be ten times the average.
- I assumed 200 KB per record: original URL 70 KB, short URL 30 KB and metadata 100 KB.
- Monthly and yearly storage can be calculated from these assumptions.

**Follow-up:** Calculate the actual QPS and verify the 200 KB assumption.

**My response:**

I asked to skip the calculation and move to the next section.

### Q3 — APIs

**Interviewer:** Define the create and redirect API contracts.

**My answer:**

Create:

- POST **/api/v1/shortUrl**
- Request contains **originalUrl** and **userId**.
- Response contains **originalUrl**, **userId**, **shortUrl** and **createdAt**.

Redirect:

- GET **/api/v1/redirectUrl** with the short URL.
- Return a 302 redirect to the original URL.

**Follow-up:** Why 302 instead of 301, and what happens for an invalid or expired code?

**My response:**

301 means moved permanently and 302 means moved temporarily. I might use the old URL again in the future, so I chose 302. Return 404 when the short code is invalid or expired.

### Q4 — Data store

**Interviewer:** What schema and database would you use?

**My answer:**

Use a NoSQL database such as DynamoDB or Cassandra because the main access pattern is a single-key lookup from short URL to long URL. The system does not require joins or multi-row transactions. Partitioning by the short URL supports horizontal growth.

Schema fields:

- id
- long_url
- short_url as partition key
- created_by
- created_at
- expire_at

### Q5 — Short-code generation

**Interviewer:** How would you generate a unique seven-character code?

**My answer:**

I compared three options:

1. Random string with collision check — rejected because each write needs a read-check-write cycle.
2. Truncated MD5/SHA — rejected because truncation can collide and still needs collision handling.
3. Base62 encoding of an auto-incrementing ID — selected because it avoids collisions.

Seven Base62 characters provide 62^7 combinations, which is in the trillions. Sequential IDs are predictable, so the ID should be obfuscated before Base62 encoding using an offset, XOR or shuffle.

### Q6 — Distributed ID allocation

**Interviewer:** How will multiple servers and regions generate IDs without duplicates or a central bottleneck?

**My answer:**

ZooKeeper or a database table allocates large ID blocks, for example one million IDs, to each application server. A server increments locally within its assigned block, so normal writes require no coordination. A server crash can waste the unused IDs in its block, which is acceptable because the Base62 space is very large.

### Q7 — High-level architecture and flows

**Interviewer:** Describe the architecture and the create and redirect flows.

**My answer — write path:**

1. Client sends POST **/api/v1/urlshorten** to the API Gateway.
2. The load balancer routes the request to an available application server.
3. The application server uses the next ID from its local block.
4. The ID is Base62-encoded into a short code.
5. The mapping and timestamps are written to the database.
6. After a successful database write, the cache is updated.
7. The short URL is returned with 201 Created.
8. If the database write fails, return an error and do not update the cache.

**My answer — read path:**

1. Client requests GET **/api/v1/{shortUrl}**.
2. The load balancer routes the request to an application server.
3. The application server checks the distributed cache.
4. On a cache hit, return a 302 redirect.
5. On a cache miss, query the database.
6. If found, populate the cache and return a 302 redirect.
7. If missing or expired, return 404.

### Q8 — Diagram clarification

**Interviewer:** Does every application server access ZooKeeper, Redis and the database?

**My response:**

Yes. The diagram connected each dependency to one application server only to keep the picture clear, but every application server communicates with ZooKeeper, the distributed cache and the database.

## Architecture I produced

This Mermaid diagram reconstructs the architecture drawn during the mock. Every application server uses the shared dependencies.

~~~mermaid
flowchart TB
    Client["Client"] --> LB["Load Balancer"]
    LB --> Gateway["API Gateway"]
    Gateway --> Apps["URL Service Instances"]
    Apps --> Allocator["ZooKeeper ID Block Allocator"]
    Apps --> Cache["Redis Distributed Cache"]
    Apps --> DB["NoSQL Database and Replicas"]
~~~

### Q9 — Cache eviction

**Interviewer:** How would the cache handle expiry, stampede, hot keys and Redis failure?

**My answer:**

I selected LRU instead of LFU. URL traffic is often recency-driven: a link receives a burst for a few hours or days and then loses popularity. LRU keeps recently accessed links, while an old link with a historically high count might remain too long under a simple LFU policy.

Use Redis or Memcached native eviction instead of scheduled bulk cleanup. A scheduled cache wipe would create a cold cache and suddenly increase database traffic.

**Follow-up:** Address expiration, stampede, hot keys and Redis-node failure specifically.

**My response:**

The interview was stopped before these cases were answered.

## Scorecard

| Axis | Score /10 | Evidence from this attempt |
|---|---:|---|
| Requirements | 7.5 | Core use cases, expiry and major NFRs were identified. Numeric latency/availability targets, deletion behaviour and analytics scope were not clarified. |
| Architecture | 7.0 | The create and redirect flows were coherent and cache-aside was used correctly. The initial diagram made shared connections unclear and did not include analytics, failure boundaries or multi-region behaviour. |
| Problem-solving | 7.0 | Base62, alternative comparison and block allocation showed good reasoning. API details, estimation and cache-failure cases remained incomplete. |
| Scale and trade-offs | 5.5 | The read/write ratio and peak assumption were useful, but no QPS/storage calculation was completed. Partition throughput, hot keys, stampede, failover and consistency were not defended. |
| Communication | 7.0 | The answers were structured and choices were usually explained. Some API fields were inconsistent, the diagram required clarification, and the interview ended before the main deep dives. |
| **Total** | **34/50** | **6.8/10 — Borderline / Lean Hire for an incomplete attempt** |

## What went well

1. The requirements were separated into functional and non-functional requirements.
2. The NoSQL choice was tied to the single-key access pattern rather than selected by name only.
3. Three key-generation approaches were compared before selecting Base62.
4. ID-block allocation removed coordination from the normal write path.
5. The create and redirect flows were clear.
6. The database-first cache update avoided caching a mapping when persistence failed.
7. LRU was defended using expected traffic behaviour instead of being named without reasoning.

## Corrections to my answers

| My answer | Correction |
|---|---|
| A URL record may need 200 KB | This is far too large for a normal mapping. A reasonable interview assumption is about 200 bytes to 1 KB, depending on URL length and metadata. State the assumption instead of presenting it as a fixed fact. |
| Storage was divided by days and hours | Storage is cumulative: records per month × bytes per record. Division by seconds is used for QPS, not total storage. |
| 302 because the old URL may be used later | The stronger reason is that 302 is not treated as a permanent redirect. It gives the service more control over expiry, destination changes and click analytics. A cached 301 may bypass the service on later clicks. |
| Invalid and expired both return 404 | 404 is acceptable when hiding whether a code ever existed. 410 Gone is more expressive for a known expired link. Choose based on API and security requirements. |
| GET /api/v1/redirectUrl with a short URL object | A cleaner public API is **GET /{shortCode}**. The response has a **Location** header and a 302 status. |
| userId supplied in the request body | For authenticated users, obtain user identity from the access token/security context. Do not trust a caller-supplied userId. Anonymous creation can have no owner. |
| XOR or a simple offset hides sequential IDs | A simple reversible transformation can still expose patterns. Use a keyed permutation/Feistel-style mapping, a secret-backed obfuscator, or accept the enumeration risk with rate limiting. |
| Each server talks to ZooKeeper during the write | Each server contacts the allocator only when acquiring a new block. Normal writes use the local block. |
| Redis native LRU fully solves caching | Eviction is only one concern. URL expiry, stampede, hot keys, node failure and database protection need separate mechanisms. |
| Load balancer before API Gateway | This can exist internally, but for a clear interview diagram use DNS/WAF/API Gateway or edge load balancer as one entry layer, then route to stateless service instances. Avoid drawing two routing layers without explaining their roles. |

## Correct capacity calculation

Use 30 days ≈ 2,592,000 seconds.

### Writes

~~~text
100,000,000 creates / 2,592,000 seconds
≈ 38.6 writes/second average
≈ 386 writes/second at 10× peak
~~~

### Redirect reads

~~~text
10,000,000,000 redirects / 2,592,000 seconds
≈ 3,858 reads/second average
≈ 38,580 reads/second at 10× peak
~~~

The 100:1 read/write ratio is consistent with these assumptions.

### Storage

Assume one mapping requires approximately 200 bytes before database overhead:

~~~text
100,000,000 × 200 bytes
≈ 20 GB/month raw

≈ 240 GB/year raw
≈ 720 GB/year with replication factor 3
~~~

Allow extra space for database indexes, item overhead, analytics and operational headroom. The architectural conclusion is that storage is manageable, while the latency-sensitive read path and hot-link traffic deserve more attention.

## Corrected API design

### Create a short URL

~~~http
POST /api/v1/urls
Authorization: Bearer <token>
Content-Type: application/json

{
  "originalUrl": "https://example.com/article",
  "expiresAt": "2027-09-15T00:00:00Z"
}
~~~

~~~http
HTTP/1.1 201 Created
Location: /api/v1/urls/aZ91Kx2

{
  "shortCode": "aZ91Kx2",
  "shortUrl": "https://sho.rt/aZ91Kx2",
  "originalUrl": "https://example.com/article",
  "createdAt": "2026-09-15T10:00:00Z",
  "expiresAt": "2027-09-15T00:00:00Z"
}
~~~

Important errors:

- 400 for malformed/unsupported URLs or invalid expiration.
- 401 when an authenticated feature requires login.
- 429 when creation rate limits are exceeded.
- 503 when an ID block cannot be obtained and no local IDs remain.

### Redirect

~~~http
GET /aZ91Kx2

HTTP/1.1 302 Found
Location: https://example.com/article
Cache-Control: no-store
~~~

Use 404 for unknown codes. Use either 404 or 410 for expired codes, depending on whether the API should reveal that the link previously existed.

## Corrected data model

### URL mapping

| Field | Purpose |
|---|---|
| short_code | Partition/primary key; seven-character Base62 value |
| numeric_id | Allocated unique ID before obfuscation/encoding |
| long_url | Validated destination URL |
| created_by | Nullable owner ID |
| created_at | Creation timestamp |
| expires_at | Business expiration timestamp |
| status | ACTIVE, DISABLED or EXPIRED |
| url_hash | Optional deduplication support |
| version | Optional optimistic update/version field |

The main lookup is by **short_code**, so no secondary index is required for redirects. If users must list their links, add an index such as **created_by + created_at**. Database TTL can clean old records asynchronously, but the application must still check **expires_at** because TTL deletion is not always immediate.

## Corrected short-code generation

Keep the selected range-allocation design:

1. A strongly consistent allocator reserves a non-overlapping ID block.
2. Each application server generates IDs locally from its current block.
3. Apply a keyed, deterministic permutation if raw sequence exposure is unacceptable.
4. Base62-encode the result.
5. Write the mapping using **short_code** as the primary key.

62^7 = 3,521,614,606,208 possible codes. At 100 million new links per month, the seven-character space is large enough for the assumed workload.

If the allocator becomes unavailable, servers can continue using their existing blocks. Only new-link creation fails after a server exhausts its block; existing redirects continue working.

## Corrected architecture

~~~mermaid
flowchart TB
    Client["Client"] --> Edge["DNS, WAF and API Gateway"]
    Edge --> Service["Stateless URL Service"]
    Service --> L1["Small Local Cache"]
    Service --> Redis["Redis Cluster"]
    Service --> DB["NoSQL Mapping Store"]
    Service --> Allocator["ID Block Allocator"]
    Service --> Events["Click Event Stream"]
    Events --> Analytics["Analytics Consumers and Store"]
~~~

All URL-service instances access Redis, the database and the allocator. The allocator is outside the redirect path.

## Completed areas that were missed

### 1. Expiration

- Store **expires_at** in the database.
- Cache with TTL no longer than **expires_at - current time**.
- Validate expiration after cache/database lookup.
- Return 404 or 410 for expired links.
- Use database TTL/background cleanup for physical deletion.
- Negative-cache invalid or expired codes briefly to reduce repeated database misses.

### 2. Cache stampede

When a popular entry expires, many requests can miss together and overload the database.

Use:

- Request coalescing/single-flight so only one request reloads a key.
- A short distributed lock only when coordination across instances is necessary.
- Random TTL jitter so many keys do not expire together.
- Stale-while-revalidate only when serving a slightly stale mapping is allowed.
- Database rate limiting and bounded fallbacks.

### 3. Hot URLs

A viral short code can overload one Redis shard.

Mitigations:

- Keep a small L1 cache such as Caffeine on each application instance.
- Replicate exceptionally hot mappings across cache nodes or use hot-key read replicas.
- Detect hot keys using per-key traffic metrics.
- Apply request coalescing.
- Be careful with browser/CDN caching because it can bypass click analytics.

### 4. Redis node failure

- Use Redis Cluster with replicas and automatic failover.
- Configure short client timeouts.
- Fall back to the database on cache failure.
- Protect the database with circuit breakers, rate limits and request coalescing.
- Never retry Redis indefinitely on the redirect path.
- Repopulate the cache gradually after recovery to avoid a cache avalanche.

### 5. Read-after-write consistency

Writing the database first and then caching the result normally allows an immediately created link to work from cache. If the cache update fails:

- Route the creator's immediate read to a strongly consistent database read, or
- Return only after the mapping is visible in the chosen consistency model.
- In a multi-region system, use a home region or synchronously replicate the small mapping when immediate global use is required.

### 6. Analytics pipeline

Keep analytics outside the redirect critical path:

1. Return the redirect without waiting for analytics storage.
2. Publish a click event containing **event_id, short_code, timestamp, coarse location, referrer and user-agent category**.
3. Partition events by **short_code** when per-link ordering is useful.
4. Consumers aggregate counts into hourly/daily buckets.
5. Make consumers idempotent using **event_id** or accept approximate counters if the product allows it.
6. Use retries and a DLQ for poison events.
7. Apply retention and privacy rules; avoid unnecessary raw personal data.

If the event stream is briefly unavailable, use a bounded local buffer or accept small analytics loss based on the business SLA. Redirect availability remains the priority.

### 7. Database partitioning and replication

- Partition by **short_code**, which spreads normal traffic well because codes appear random after permutation.
- Use replication across availability zones.
- Choose strong consistency for link creation/uniqueness where needed.
- Eventual consistency can be acceptable for analytics.
- Monitor hot partitions because one viral key can still create concentrated reads even with a good partition key.
- Add multiple regions only when latency/availability requirements justify the operational complexity.

### 8. Failure handling

| Failure | Behaviour |
|---|---|
| URL-service instance fails | Load balancer routes requests to healthy stateless instances |
| Redis fails | Short timeout, database fallback, circuit breaker and database protection |
| Database primary/partition fails | Multi-AZ replica/failover; redirects may degrade but should not corrupt mappings |
| ID allocator fails | Existing ID blocks continue; new creation eventually pauses; redirects are unaffected |
| Analytics broker fails | Redirect continues; buffer/drop events according to analytics SLA |
| One region fails | DNS/global routing sends traffic to another region with replicated mappings |
| Dependency is slow | Timeout, bounded retry with jitter, bulkhead and circuit breaker |

### 9. Security and abuse prevention

- Accept only supported schemes such as HTTP and HTTPS.
- Normalize and validate destination URLs.
- Reject internal/private network destinations when server-side fetching exists.
- Use malware/phishing deny-lists or scanning where required.
- Rate-limit creation and redirect scraping.
- Authenticate link-management operations.
- Authorize only the owner/admin to disable or inspect private analytics.
- Encrypt data in transit and at rest.
- Use random-looking codes or keyed permutation to reduce enumeration.
- Avoid storing unnecessary IP addresses or other personal data.
- Support disabling malicious links quickly.

### 10. Observability

Monitor:

- Redirect p50, p95 and p99 latency.
- Create and redirect success/error rates.
- Cache hit ratio and cache latency.
- Database latency, throttling and hot partitions.
- 404/410 rate.
- Remaining IDs in local blocks and allocator errors.
- Analytics queue lag, retry and DLQ count.
- Traffic for unusually hot or abusive codes.

### 11. Bottlenecks at 10× traffic

The likely first risks are:

1. Viral hot keys concentrating traffic.
2. Redis/database overload during cache failure or mass expiration.
3. Database partition throttling.
4. Analytics queue lag.
5. Cross-region replication delay.

Scale stateless application instances horizontally, use L1 plus distributed caching, protect the database, spread TTLs, isolate analytics from redirects and add regional reads only when measurements require them.

## Two-minute corrected interview summary

> The system has two main paths. For creation, a stateless URL service takes the next ID from a locally allocated non-overlapping block, optionally permutes it to hide sequencing, Base62-encodes it and stores the mapping in a NoSQL database keyed by short code. The database is written before Redis so a failed persistence operation is never cached.
>
> For redirects, the service checks a small local cache and then Redis. On a miss, it reads the mapping by short code, verifies status and expiration, caches it with a bounded TTL and returns a 302 with the destination in the Location header. Click analytics are sent asynchronously so they do not increase redirect latency.
>
> The design scales using stateless services, partitioning by short code, cache replicas and multi-AZ database replication. The main risks are hot URLs and a cache failure causing a database spike, so I use hot-key detection, request coalescing, TTL jitter, short timeouts, circuit breakers and protected database fallback. The main trade-off is choosing redirect availability and latency over perfectly accurate real-time analytics.

## Top three gaps and corrective exercises

| Gap | Root cause | Corrective exercise | Due | Verified? |
|---|---|---|---|---|
| Estimation did not reach numbers or decisions | Calculation was skipped and bytes were confused with KB | Recalculate URL-shortener QPS/storage from memory and state one decision caused by each number | 2026-09-17 | No |
| Cache reliability was incomplete | Eviction was explained, but expiry/stampede/hot-key/failover were not | Draw and explain all four cache failure paths in five minutes | 2026-09-18 | No |
| Senior reliability close was missing | Interview stopped before analytics, security, failures and observability | Give a five-minute closing covering failure, security, monitoring and largest trade-off | 2026-09-20 | No |

## Re-attempt plan

Re-attempt this problem on **2026-09-24** without reading the corrected answer during the mock.

Attempt 02 passes the gap check only if:

- Average and peak QPS plus storage are calculated correctly.
- At least one estimate changes an architectural decision.
- Cache expiry, stampede, hot key and Redis failure are answered without prompting.
- Analytics reliability, security and failure behaviour are covered.
- The final two-minute summary is completed.
