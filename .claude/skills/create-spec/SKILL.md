---
name: create-spec
description: Specification creation workflow. Use when user needs requirements, specifications, or detailed feature definitions before implementation.
---

# Specification Creation

## Trigger Words

| Intent | Examples |
|--------|----------|
| Specify | "spec", "specification", "requirements" |
| Define | "define", "document requirements" |
| Plan | "what should it do", "feature details" |

## Specification Structure

```
specs/{feature-name}.md
├── Overview
├── Requirements (Functional)
├── Non-Functional Requirements
├── User Scenarios
├── Technical Constraints
├── Success Criteria
└── Out of Scope
```

## Workflow

```
1. Gather    → User intent, context
2. Research  → Existing code, patterns
3. Draft     → Initial spec
4. Clarify   → Ask questions
5. Finalize  → Complete spec
```

## Spec Template

```markdown
# {Feature Name}

## Overview
[Brief description]

## Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|

## Non-Functional Requirements
| ID | Requirement | Target |
|----|-------------|--------|

## User Scenarios
### Scenario 1: [Name]
- Given: [precondition]
- When: [action]
- Then: [outcome]

## Technical Constraints
- [Constraint 1]

## Success Criteria
- [ ] [Measurable criterion]

## Out of Scope
- [What this does NOT include]
```

## Quality Checks

| Check | Requirement |
|-------|-------------|
| Complete | All sections filled |
| Testable | Criteria are measurable |
| Scoped | Clear boundaries |
| Prioritized | Must/Should/Could |
