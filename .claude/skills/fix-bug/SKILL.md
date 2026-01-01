---
name: fix-bug
description: Bug fixing workflow. Use when user reports a bug, error, issue, or something not working. Investigates, diagnoses, fixes, tests, and commits.
---

# Bug Fix

## Trigger Words

| Intent | Examples |
|--------|----------|
| Fix | "fix", "repair", "resolve" |
| Bug | "bug", "error", "issue", "broken" |
| Problem | "not working", "fails", "crash" |

## Workflow

```
1. Reproduce → Understand the issue
2. Diagnose  → Find root cause
3. Fix       → Implement solution
4. Test      → Verify fix + no regression
5. Commit    → Document the fix
```

## Steps

| Step | Action | Tools |
|------|--------|-------|
| 1 | Gather info | Read, Grep |
| 2 | Trace execution | Read, Bash |
| 3 | Identify root cause | Analysis |
| 4 | Write fix | Edit |
| 5 | Add test | Write |
| 6 | Run tests | ideacode_test |
| 7 | Commit | ideacode_commit |

## Root Cause Categories

| Category | Check |
|----------|-------|
| Logic | Incorrect conditions, off-by-one |
| State | Race condition, stale data |
| Input | Missing validation, edge cases |
| Dependency | API change, version mismatch |
| Config | Environment, settings |

## Commit Format

```
fix(module): brief description

- Root cause: [what was wrong]
- Fix: [what was changed]
- Test: [how verified]
```

## Modifiers

| Modifier | Effect |
|----------|--------|
| .yolo | Skip approvals |
| .tcr | Test-Commit-Revert |
