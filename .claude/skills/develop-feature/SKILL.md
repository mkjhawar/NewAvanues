---
name: develop-feature
description: Full feature development workflow. Use when user wants to build, create, implement, or add a new feature. Handles specification, planning, implementation, testing, and commit.
---

# Feature Development

## Trigger Words

| Intent | Examples |
|--------|----------|
| Create | "build", "create", "implement", "add feature" |
| New | "new capability", "I want to add" |
| Feature | "feature", "functionality", "module" |

## Workflow

```
1. Specify  → ideacode_specify → specs/{feature}.md
2. Plan     → ideacode_plan    → specs/{feature}.plan.md
3. Implement → ideacode_implement
4. Test     → ideacode_test
5. Commit   → ideacode_commit
```

## Steps

| Step | Action | Output |
|------|--------|--------|
| 1 | Gather requirements | Spec file |
| 2 | Break into tasks | Plan file |
| 3 | Write code | Source files |
| 4 | Run tests | Test results |
| 5 | Commit changes | Git commit |

## Quality Gates

| Gate | Requirement |
|------|-------------|
| Spec review | User approves spec |
| Plan review | User approves plan |
| Tests pass | All tests green |
| Coverage | 90%+ on new code |

## Modifiers

| Modifier | Effect |
|----------|--------|
| .yolo | Skip approvals |
| .swarm | Multi-agent parallel |
| .tutor | Explain as you go |
