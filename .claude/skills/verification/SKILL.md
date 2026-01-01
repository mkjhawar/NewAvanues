---
name: verification
description: Automatic verification at each workflow phase. Ensures specs are complete, plans cover requirements, and implementations match plans. Auto-invokes after each phase.
---

# Verification

## Purpose
Automatically verify quality at each development phase.

## Auto-Verify Phases

| Phase | Verification |
|-------|--------------|
| Spec | Requirements complete, user stories defined |
| Plan | Tasks cover requirements, dependencies identified |
| Implement | Code matches plan, tests pass, quality gates met |

## Verification Checklist

### Spec Verification
- [ ] All requirements documented
- [ ] User stories follow format
- [ ] Acceptance criteria defined
- [ ] Out of scope clearly stated
- [ ] No ambiguous requirements

### Plan Verification
- [ ] Tasks cover all requirements
- [ ] Dependencies identified
- [ ] No circular dependencies
- [ ] Tasks are atomic
- [ ] Platform considerations addressed

### Implementation Verification
- [ ] Code implements all tasks
- [ ] Tests written and passing
- [ ] Quality gates met (90% coverage)
- [ ] No lint errors
- [ ] Build succeeds

## Visual Verification (UI)

When UI components involved:
- Capture screenshot after creation
- Store in `.ideacode/verification/screenshots/`
- Compare against mockups if provided

## Configuration
See: `.ideacode/config.yml` → `verification` section

## Profiles

| Profile | Auto-Verify |
|---------|-------------|
| default | Yes |
| strict | Yes + visual |
| prototype | No |
| production | Yes + visual + security |
