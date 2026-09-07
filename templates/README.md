# Interview Preparation Templates

Copy the relevant template into a new Markdown file for each topic or design. Write your own answers before consulting reference material.

## Concept template

```markdown
# Topic
Status: Not started | Confidence: —/5 | Last revised: — | Next revision: —

## What is it?
## Why is it needed?
## How does it work internally?
## Short example or Java/Spring Boot code
## Trade-offs and limitations
## Real-project usage (only verified experience)
## Interview explanation (60–90 seconds)
## Follow-up questions and answers
## Mistakes / knowledge gaps
## Next action
```

## HLD template

```markdown
# System name
Date: — | Duration: — | Status: Not started | Confidence: —/5

## 1. Functional requirements and exclusions
## 2. Non-functional requirements and assumptions
## 3. Scale estimation (QPS, storage, bandwidth, peak traffic)
## 4. APIs and contracts
## 5. Data model and access patterns
## 6. High-level architecture diagram
## 7. Critical flow 1
## 8. Critical flow 2
## 9. Scaling, consistency and caching
## 10. Failures, recovery, security and observability
## 11. Alternatives and trade-offs
## 12. Two-minute final explanation
## 13. Interviewer follow-ups
## 14. Mock score /50, top three gaps, next revision
```

## LLD template

```markdown
# Problem name
Date: — | Duration: — | Status: Not started | Confidence: —/5

## Requirements and assumptions
## Use cases and invariants
## Entities and responsibilities
## Class diagram and relationships
## Interfaces and design patterns (why each is needed)
## Critical Java implementation
## Unit tests and edge cases
## Concurrency and failure handling
## Trade-offs and possible extensions
## Interview explanation and follow-ups
## Mock score /50 and next action
```

## Mock interview template

```markdown
# Mock — Problem
Date: — | Type: HLD/LLD | Duration: — | Interviewer/self-review: —

## Requirements /10
Score: —
Evidence:
## Architecture /10
Score: —
Evidence:
## Problem-solving /10
Score: —
Evidence:
## Scale and trade-offs /10
Score: —
Evidence:
## Communication /10
Score: —
Evidence:
## Total /50
—
## Questions I could not answer
## Top three gaps and exact corrective exercises
## What I would change in the design
## Re-attempt date and outcome
```

## Daily session template

```markdown
# Date
Today's priority:
Concept practiced:
Design/coding exercise:
What I explained without notes:
Questions I missed:
Evidence / commit or notes link:
Next revision:
Tomorrow's highest-priority action:
```

## Diagram guidance

Use Mermaid diagrams directly in Markdown for simple flowcharts, sequence diagrams and class diagrams. For complex diagrams, store an exported image alongside editable source. Keep diagrams readable and explain components, data flow, failure boundaries and ownership. Do not publish confidential project diagrams.
