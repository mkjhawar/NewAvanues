---
name: tdd-development
description: Intelligent Test-Driven Development with auto-scoring. Use when implementing business logic, bug fixes, or critical path code. Auto-invokes based on TDD score calculation.
---

# TDD Development

## Purpose
Apply Test-Driven Development based on intelligent task scoring.

## Auto-Invoke Triggers

| Trigger | Score Impact |
|---------|--------------|
| Business logic | +30 |
| Bug fix | +25 |
| Critical path (auth, payment) | +15 |
| Complex logic | +10 |
| UI only | -20 |
| Config only | -15 |

## Score Thresholds

| Score | Action |
|-------|--------|
| < 50 | No TDD |
| 50-69 | Recommend TDD |
| 70-89 | Strongly recommend |
| >= 90 | Enforce TDD |

## TDD Workflow

```
1. RED   - Write failing test
2. GREEN - Minimal code to pass
3. REFACTOR - Improve while green
4. REPEAT
```

## Standards Reference
See: `.ideacode/living-docs/LD-IDEACODE-TDD-Scoring-V1.md`

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.tdd` | Force TDD |
| `.skip-tdd` | Skip (reason required if score >= 90) |
| `.tdd-strict` | TDD + mutation testing |
