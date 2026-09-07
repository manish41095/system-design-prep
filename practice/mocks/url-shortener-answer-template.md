# URL Shortener — Answer Sheet — Attempt 01

Date: YYYY-MM-DD  
Started at:  
Finished at:  
Duration:  
Evaluator: Person / ChatGPT / Self-review  
Diagram: link or Mermaid section  
Re-attempt due:

## Instructions

Answer aloud first, then preserve what you actually said. The interviewer may change the order or add follow-ups. Do not fill corrections into the original answers after feedback.

## Q1 — Requirements

**Interviewer:** What is in scope, and what can we explicitly defer?

**My answer:**

**Follow-up asked:**

**My response:**

## Q2 — Non-functional requirements

**Interviewer:** Which qualities matter most: availability, consistency, latency, durability or something else? Give targets or priorities.

**My answer:**

**Follow-up asked:**

**My response:**

## Q3 — Scale estimation

**Interviewer:** Which scale assumptions change your design? Estimate average/peak reads and writes, storage and read/write ratio.

**My answer and calculations:**

**Follow-up asked:**

**My response:**

## Q4 — APIs

**Interviewer:** Define the create-link and redirect API contracts, including important errors.

**My answer:**

**Follow-up asked:**

**My response:**

## Q5 — Redirect behavior

**Interviewer:** Would the redirect return 301, 302, 307 or 308? Why does the choice matter?

**My answer:**

**Follow-up asked:**

**My response:**

## Q6 — Data model

**Interviewer:** What data do you store, what are the access patterns, and which indexes or constraints are required?

**My answer:**

**Follow-up asked:**

**My response:**

## Q7 — Key generation

**Interviewer:** How is the short key generated? Discuss uniqueness, collisions, predictability and multiple service instances.

**My answer:**

**Alternatives considered:**

**Follow-up asked:**

**My response:**

## Q8 — High-level architecture

**Interviewer:** Draw the main components. Walk through create and redirect end to end.

**My answer:**

~~~mermaid
flowchart LR
    Client --> Gateway
    Gateway --> Service
~~~

Replace the placeholder with your actual diagram.

**Follow-up asked:**

**My response:**

## Q9 — Caching

**Interviewer:** Walk through a cache hit, miss and expired link. How do you handle invalidation, hot keys and stampede?

**My answer:**

**Follow-up asked:**

**My response:**

## Q10 — Consistency and partitioning

**Interviewer:** How do you scale storage and cache nodes? How do you provide read-after-write behavior?

**My answer:**

**Follow-up asked:**

**My response:**

## Q11 — Analytics

**Interviewer:** How do you capture click analytics without slowing redirects? What about duplicate or out-of-order events?

**My answer:**

**Follow-up asked:**

**My response:**

## Q12 — Failures

**Interviewer:** What happens when the cache, database writer, message broker or one region fails?

**My answer:**

**Follow-up asked:**

**My response:**

## Q13 — Security and abuse

**Interviewer:** How do you handle malicious URLs, enumeration, scraping, rate limits and privacy?

**My answer:**

**Follow-up asked:**

**My response:**

## Q14 — Trade-offs and summary

**Interviewer:** What becomes the bottleneck at 10× traffic? Summarize the design and its largest trade-off in two minutes.

**My answer:**

**Follow-up asked:**

**My response:**

## Questions I could not answer

- 

## Interviewer feedback

Paste feedback only after saying **END MOCK**.

## Scorecard

| Axis | Score /10 | Evidence from my actual attempt |
|---|---:|---|
| Requirements | — | — |
| Architecture | — | — |
| Problem-solving | — | — |
| Scale and trade-offs | — | — |
| Communication | — | — |
| **Total** | **—/50** | |

## Top three gaps and corrections

| Gap | Root cause | Corrective exercise | Due | Verified? |
|---|---|---|---|---|
| 1 | | | | No |
| 2 | | | | No |
| 3 | | | | No |

## What I would change

Write the improved reasoning here. Do not replace the original answers above.

## Re-attempt comparison

Complete after the next attempt. Link attempt 02 and describe what measurably improved.
