---
name: ood-development
description: Intelligent Object-Oriented Development with auto-scoring. Use when implementing domain logic, data models, or complex business rules. Auto-invokes based on OOD score calculation.
---

# OOD Development

## Purpose
Apply Object-Oriented Development patterns based on intelligent context scoring.

## Auto-Invoke Triggers

| Trigger | Score Impact |
|---------|--------------|
| Domain logic | +30 |
| Data modeling | +25 |
| State management | +20 |
| Relationships | +20 |
| Validation rules | +15 |
| Simple CRUD | -25 |
| Utility code | -20 |

## Score Thresholds

| Score | Action |
|-------|--------|
| < 40 | No OOD patterns |
| 40-59 | Suggest patterns |
| 60-79 | Recommend patterns |
| >= 80 | Strongly recommend |

## Pattern Recommendations

| Context | Pattern |
|---------|---------|
| Data with identity | Entity |
| Immutable values | Value Object |
| Object clusters | Aggregate |
| Data access | Repository |
| Cross-entity ops | Domain Service |
| Complex creation | Factory |
| Business rules | Specification |

## Standards Reference
See: `.ideacode/living-docs/LD-IDEACODE-OOD-Scoring-V1.md`

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.ood` | Force OOD analysis |
| `.skip-ood` | Skip recommendations |
| `.ddd` | Full DDD patterns |
| `.solid` | SOLID compliance check |
