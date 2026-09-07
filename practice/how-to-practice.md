# How to Practice System Design

## The learning loop

Do not read complete solutions before attempting a problem. Use **Learn → Recall → Apply → Mock → Fix → Re-solve**. Separate study practice from timed interview attempts.

### 1. Learn one concept (20–30 minutes)

Choose one topic from the concept tracker. Write your own short notes using What → Why → How → Internals → Example → Trade-offs → Real-project usage → Follow-up questions. Explain it aloud without notes. If you cannot explain it, return to the missing part rather than marking it complete.

### 2. Apply it (30–40 minutes)

Choose a small exercise. For example, after caching, design a cache-aside read flow with TTL, invalidation and a cache-miss failure scenario. For Java LLD, implement one feature and write tests. Record why you chose the design and what alternatives you rejected.

### 3. Run a timed mock (45–60 minutes)

Use the planned queue in [Mock tracker](README.md). Have an interviewer ask questions, or use ChatGPT in interviewer-only mode. Do not look at notes or model answers. Speak your reasoning aloud and draw your own architecture/class diagram. Save your answer before feedback.

### 4. Evaluate and fix

Score the five categories out of 10, total /50. Record the top three exact weaknesses. For each, write a corrective exercise and due date. Re-attempt the same problem after 7–14 days and compare the two attempts. Do not mark a gap fixed until you answer the missed follow-up without notes.

## Daily 90-minute example

| Time | Activity | Output |
|---|---|---|
| 0–25 min | Learn/revise one concept | Short notes + spoken explanation |
| 25–65 min | Apply to HLD/LLD | Diagram, pseudocode or Java code/tests |
| 65–80 min | Answer 3–5 follow-ups without notes | Recorded gaps |
| 80–90 min | Update tracker | Status, evidence, next action, revision date |

On mock days, replace this session with the full mock and feedback. Do not add an extra workload just to maintain a streak.

## How to practice with ChatGPT

Start a new chat and paste this prompt:

> Act as a senior backend system-design interviewer for an experienced Java/Spring Boot engineer. Conduct a 45–60 minute [HLD/LLD] interview on [problem]. Ask one question at a time. Do not reveal the solution or give hints unless I explicitly ask. Start by asking me to clarify requirements. Challenge my assumptions about scale, APIs, data models, concurrency, failures and trade-offs. For LLD, require Java interfaces, implementation and tests. Keep track of missed points. When I say END MOCK, evaluate only my actual answers using Requirements, Architecture, Problem-solving, Scale/Trade-offs and Communication, each /10. Give the total /50, evidence for each score, the top three gaps, corrective exercises and a re-attempt prompt. Do not invent answers or scores for me.

Use voice or speak aloud if convenient. If practicing alone, record your explanation and review it against the rubric. ChatGPT feedback is useful practice, not a guarantee of a real interview outcome.

## Updating GitHub after a session

1. Open the relevant concept/HLD/LLD tracker and select a topic.
2. Change its status and confidence only when your actual work supports it.
3. Add a link to your notes, diagram, Java implementation or mock transcript.
4. Save the dated mock in `practice/mocks/` and fill in the scorecard.
5. Add the top three weaknesses to the error log and set a re-attempt date.
6. Commit with a descriptive message such as `practice: complete URL shortener mock 01` or `revision: fix cache stampede explanation`.

You can edit Markdown through GitHub's pencil icon and commit directly to `main`. For code, clone the repo, create a feature branch if useful, run tests, commit and push. Never commit credentials, company source code, customer data or proprietary architecture.

## What counts as complete?

A concept is interview ready when you can explain it without notes, apply it to a concrete design and defend the important trade-offs. An HLD is complete when the requirements, estimates, APIs, data model, diagram, critical flows, scaling/failures and trade-offs are documented and defended in a mock. An LLD additionally needs compilable Java code, meaningful tests and a discussion of concurrency and extensibility. Reading or copying a solution is only the learning stage.
