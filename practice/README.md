# Mock Interview Tracker

## New to mocks?

Use **[Start Here — Run Your First Mock and Save It to Git](start-here.md)**. It shows:

- Who asks the questions.
- The exact URL-shortener questions.
- Where to write every answer and follow-up.
- How to score the attempt.
- Which tracker rows to update.
- Exact clone, branch, add, commit and push commands.

Use the [question-by-question answer sheet](mocks/url-shortener-answer-template.md) for your first attempt.

## Practice workflow

1. Run the prompt without notes and preserve the original attempt.
2. Save the transcript, diagram or Java code under [mocks](mocks/README.md).
3. Score only observable evidence.
4. Add the top three gaps to the error log.
5. Complete small corrective exercises.
6. Re-attempt the same problem after 7–14 days and compare decisions.

## How to run a mock

Use a 45–60 minute timer and no notes. Ask another person or ChatGPT to act as interviewer. Share only the problem statement, not a reference answer. Think aloud, ask clarifying questions, draw a diagram and defend trade-offs. Save your actual answer before reviewing feedback. Do not invent scores or completed work.

For HLD, spend approximately 5 minutes on requirements, 5 on estimates, 5 on APIs/data, 15 on architecture, 15 on deep dives and 5–10 on failures/trade-offs. For LLD, spend approximately 5 on requirements, 10 on entities/classes, 5 on patterns, 25 on code/tests and 5–10 on trade-offs. Adjust based on the interviewer. The [answer framework](answer-framework.md) gives the complete speaking structure.

## Scoring rubric

Score each category from 0 to 10. A score of 5 means partial understanding; 7 means a reasonable solution with gaps; 9–10 requires a clear, defensible answer with strong follow-ups.

| Category | What good looks like |
|---|---|
| Requirements /10 | Clarifies scope, scale, constraints and priorities |
| Architecture /10 | Coherent components, APIs, data model and request flows; for LLD, class design and interfaces |
| Problem-solving /10 | Solves critical challenges with correct reasoning and working code where required |
| Scale and trade-offs /10 | Justifies choices, handles failures, concurrency and bottlenecks |
| Communication /10 | Structured explanation, time management, clear follow-ups |

**Total:** /50. Record HLD/LLD type, actual duration and who evaluated it. A self-score is not an external interview score. Use trends and specific feedback, not just totals.

## Planned mock queue

The full question prompts and interviewer probes are in the [mock bank](mock-bank.md).

| # | Type | Problem | Focus | Date | Score /50 | Top gap | Re-attempt |
|---|---|---|---|---|---|---|---|
| 01 | HLD | [URL shortener](mock-01-url-shortener.md) | Requirements, estimation, DB, cache | — | — | — | — |
| 02 | LLD | Vending machine | State pattern, interfaces, Java tests | — | — | — | — |
| 03 | HLD | Notification system | Kafka, retries, DLQ, idempotency | — | — | — | — |
| 04 | LLD | Movie ticket booking | Concurrency, locking, state transitions | — | — | — | — |
| 05 | HLD | Rate limiter | Algorithms, Redis, distributed correctness | — | — | — | — |
| 06 | HLD | Food delivery | Order lifecycle, location, dispatch | — | — | — | — |
| 07 | HLD | Media storage | Uploads, metadata, retrieval, cleanup | — | — | — | — |
| 08 | LLD | Ride sharing | Strategy, matching, state and extensibility | — | — | — | — |
| 09 | HLD | Payment processing | Idempotency, ledger, consistency, recovery | — | — | — | — |
| 10 | HLD/LLD | Weakest previous problem | Re-solve and defend missed follow-ups | — | — | — | — |

The queue is a plan, not a deadline. Replace a later problem if mock feedback reveals a more important gap.

## Error log

| Date | Problem | Exact weakness | Root cause | Corrective exercise | Due date | Verified? |
|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — |

Useful feedback is specific: “Could not explain duplicate payment prevention” or “Spent 25 minutes on requirements and did not reach scaling.” Avoid “Need to improve HLD.”

## After every mock

Save the transcript/diagram/code in **practice/mocks/YYYY-MM-DD-problem-attempt-01.md** using the [generic mock template](mocks/mock-template.md). For Mock 01, use the clearer [URL-shortener answer sheet](mocks/url-shortener-answer-template.md). Record the five scores, three exact gaps, corrective exercises and a re-attempt date.

Revise the gaps, then re-attempt after approximately 7–14 days. Compare answers and mark a gap verified only when the missed follow-up can be answered without notes. Update the relevant HLD/LLD and concept rows only when evidence supports the new status.
