# Mock 01 — URL Shortener

**Type:** HLD  
**Duration:** 45–60 minutes  
**Rules:** No notes or solution browsing. Think aloud. The interviewer asks one question at a time.

## Candidate prompt

Design a URL-shortening platform similar to TinyURL.

The system must:

- Create a short URL from a long URL.
- Redirect a short URL to the original URL.
- Handle expiry and custom aliases.
- Record click analytics without slowing redirects.
- Support billions of stored links with high availability and low redirect latency.

Clarify anything else before designing. Do not assume every feature is required.

## Candidate deliverables

During the interview, produce:

1. Scope, NFRs and explicit exclusions.
2. Average and peak QPS, storage and bandwidth assumptions.
3. APIs and redirect semantics.
4. Data model, indexes and access patterns.
5. Architecture diagram and create/redirect flows.
6. Key-generation approach with collision and predictability analysis.
7. Caching, partitioning/replication and read-after-write behavior.
8. Analytics pipeline, failure handling, security/abuse controls and observability.
9. Major trade-offs and 10× evolution plan.

## Interviewer-only question sequence

Reveal one question at a time. Do not give hints until the candidate has reasoned.

1. What is in scope, and what can we explicitly defer?
2. Which scale assumptions will change the design?
3. What are the create and redirect API contracts?
4. Would redirect return 301, 302, 307 or 308? Why?
5. How is the key generated? What happens on collision or enumeration?
6. Walk through a cache hit, cache miss and expired link.
7. How do you avoid an immediate redirect failing after link creation?
8. How do cache nodes scale and what happens when one is added?
9. How are click events recorded without increasing redirect latency?
10. What if analytics events are duplicated or arrive out of order?
11. What becomes the bottleneck at 10× traffic?
12. What happens when cache, database writer or one region fails?
13. How do you stop malicious URLs, scraping and hot-key abuse?
14. Summarize the design and its largest trade-off in two minutes.

## Save the attempt before feedback

Create **practice/mocks/YYYY-MM-DD-url-shortener-attempt-01.md** from [the attempt template](mocks/mock-template.md). Add your diagram source or image in the same folder if needed. Preserve incomplete reasoning and questions you could not answer.

## Evaluation

Use five scores: Requirements, Architecture, Problem-solving, Scale/Trade-offs and Communication, each /10. Feedback must cite evidence from the actual attempt. Choose three exact corrective exercises and schedule a re-attempt after 7–14 days.

Do not add a model solution to this file before completing the baseline; it would weaken the mock.
