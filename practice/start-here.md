# Start Here — Run Your First Mock and Save It to Git

This page explains the complete loop using **Mock 01: URL Shortener**.

## What you are supposed to do

You are the candidate. ChatGPT or another person is the interviewer.

The interviewer asks **one question**. You speak your answer aloud. After answering, write the same answer in your attempt file. The interviewer then asks a follow-up. Continue for 45–60 minutes. Feedback and scoring happen only after the mock ends.

Do not write a perfect answer after reading a solution. The repository should preserve what you actually said, because the mistakes decide what you study next.

## Step 1 — Prepare the answer file

Copy [the URL-shortener answer sheet](mocks/url-shortener-answer-template.md) to:

**practice/mocks/YYYY-MM-DD-url-shortener-attempt-01.md**

For example:

**practice/mocks/2026-09-07-url-shortener-attempt-01.md**

Fill only the date, duration and evaluator before starting. Leave all answers empty.

## Step 2 — Start the interviewer

Open a new ChatGPT conversation and paste:

> Act as a senior system-design interviewer. Run Mock 01 from my system-design-prep repository: URL Shortener. Ask only one question at a time, starting with requirements. Do not reveal an answer, checklist, hint or score during the interview. Challenge my assumptions about estimates, APIs, database, key generation, caching, consistency, failures, analytics, security and trade-offs. Wait for my answer before the next question. When I say END MOCK, stop asking questions and evaluate only what I actually answered using Requirements, Architecture, Problem-solving, Scale/Trade-offs and Communication, each out of 10. Cite evidence, list my top three gaps and give one small corrective exercise for each.

Start a 45–60 minute timer. Speak naturally; do not read prepared notes.

## Step 3 — Answer and record each question

The exact questions are in the [answer sheet](mocks/url-shortener-answer-template.md). Use this pattern:

### Interviewer question

What is in scope, and what can we defer?

### My actual answer

Write what you actually said, for example:

> Core scope: create a short URL, redirect it, support expiry and collect click events asynchronously. I will defer user accounts, link editing and a detailed analytics dashboard. I assume redirects need high availability and low latency.

### Follow-up asked

Should custom aliases be strongly consistent?

### My response

Write your real response, even if uncertain:

> I am not sure yet. I would first clarify whether duplicate aliases are allowed across tenants.

Do not correct this section after feedback. Put corrections under **What I would change**.

## Step 4 — End and score

Say **END MOCK**. Copy the interviewer’s feedback into the scorecard.

A score must have evidence:

| Weak score | Useful score |
|---|---|
| Architecture: 6/10 | Architecture: 6/10 — create and redirect flows were present, but cache failure and database failover were missing |
| Need better caching | Could not explain hot-key handling or cache stampede protection |

Select exactly three gaps. Each gap must produce an exercise:

| Gap | Corrective exercise |
|---|---|
| Confused 301 and 302 | Explain redirect caching and analytics impact in 90 seconds without notes |
| Weak key generation | Compare Base62 counter, random ID and hash in a decision table |
| Missed cache hot keys | Draw hot-key replication and request-coalescing flow |

Set the re-attempt for 7–14 days later.

## Step 5 — Update the trackers

After the attempt:

1. Update the Mock 01 row in [the mock tracker](README.md): date, score, top gap and re-attempt date.
2. Update only concept rows you actually demonstrated in [the concept tracker](../concepts/README.md).
3. Do not mark URL shortener or a concept Interview ready after one attempt.
4. Link the attempt file as evidence.

Example Mock 01 row:

| # | Type | Problem | Date | Score | Top gap | Re-attempt |
|---|---|---|---|---|---|---|
| 01 | HLD | URL shortener | 2026-09-07 | 31/50 | Key generation | 2026-09-16 |

Use your real date and score; the row above is only a formatting example.

## Step 6 — Save through Git

### First time only

Run these commands in Git Bash or a terminal:

~~~bash
git clone https://github.com/manish41095/system-design-prep.git
cd system-design-prep
~~~

### For every mock

~~~bash
git checkout main
git pull
git checkout -b practice/url-shortener-attempt-01
cp practice/mocks/url-shortener-answer-template.md practice/mocks/2026-09-07-url-shortener-attempt-01.md
~~~

Replace the date with your actual attempt date. Edit the new attempt file, **practice/README.md**, and any evidence-backed concept/HLD tracker.

Then run:

~~~bash
git status
git add practice/mocks/2026-09-07-url-shortener-attempt-01.md practice/README.md concepts/README.md hld/README.md
git diff --cached
git commit -m "practice: complete URL shortener mock 01"
git push -u origin practice/url-shortener-attempt-01
~~~

If you did not modify one of the tracker files, remove it from the **git add** command. Open the GitHub link printed after push and create a pull request. Review the diff, then merge it.

## GitHub browser method

If you do not want to use commands:

1. Open [the answer-sheet template](mocks/url-shortener-answer-template.md).
2. Copy its raw content.
3. In GitHub, choose **Add file → Create new file**.
4. Name it **practice/mocks/YYYY-MM-DD-url-shortener-attempt-01.md**.
5. Paste and fill your real attempt.
6. Edit **practice/README.md** using the pencil icon.
7. Commit both changes to a new branch and open a pull request.

## What Git history should show

A useful history looks like:

- **practice: complete URL shortener mock 01**
- **study: fix redirect and key-generation gaps**
- **practice: reattempt URL shortener mock 01**

The number of commits is not the goal. Each commit should show evidence of learning, a diagnosed gap or a verified improvement.
