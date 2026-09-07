# Mock 01 — URL Shortener

**Type:** HLD  
**Duration:** 45–60 minutes  
**Rules:** No notes or solution browsing. Think aloud. The interviewer asks one question at a time.

## How to use this mock

If this is your first attempt, follow [Start Here](start-here.md).

Choose one mode:

1. **Interactive mock — recommended:** give the candidate prompt below to a person or ChatGPT. The interviewer uses the question sequence but asks only one question at a time.
2. **Solo written mock:** copy the [question-by-question answer sheet](mocks/url-shortener-answer-template.md), start a timer, and answer every question aloud before writing the answer.

Your completed file belongs under **practice/mocks/YYYY-MM-DD-url-shortener-attempt-01.md**. Do not write answers in this prompt file.

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
2. Which non-functional requirements should be prioritized?
3. Which scale assumptions will change the design?
4. What are the create and redirect API contracts?
5. Would redirect return 301, 302, 307 or 308? Why?
6. What is the data model, access pattern and required index?
7. How is the key generated? What happens on collision or enumeration?
8. Draw the components and walk through create and redirect.
9. Walk through a cache hit, cache miss, expired link, hot key and stampede.
10. How do storage/cache nodes scale, and how is read-after-write handled?
11. How are click events recorded without increasing redirect latency?
12. What if analytics events are duplicated or arrive out of order?
13. What happens when cache, database writer, broker or one region fails?
14. How do you stop malicious URLs, scraping and hot-key abuse?
15. What becomes the bottleneck at 10× traffic?
16. Summarize the design and its largest trade-off in two minutes.

The answer sheet combines closely related questions into fourteen answer sections.

## Save the attempt before feedback

Use the [URL-shortener answer sheet](mocks/url-shortener-answer-template.md). Add your diagram source or image in the same folder if needed. Preserve incomplete reasoning and questions you could not answer.

## Evaluation

Say **END MOCK** before requesting feedback. Use five scores: Requirements, Architecture, Problem-solving, Scale/Trade-offs and Communication, each /10. Feedback must cite evidence from the actual attempt. Choose three exact corrective exercises and schedule a re-attempt after 7–14 days.

Do not add a model solution to this file before completing the baseline; it would weaken the mock.
