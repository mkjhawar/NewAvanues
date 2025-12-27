# Protocol: YOLO Mode Safeguards v9.0

## Purpose

Define MANDATORY safeguards for `.yolo` mode to prevent shortcuts, incomplete implementations, and function-breaking refactoring.

---

## Three Non-Negotiable Rules

### Rule 1: No Shortcuts ❌

**Principle:** Speed NEVER justifies poor quality.

**Forbidden Practices:**
- Using quick hacks or temporary solutions
- Skipping validation, error handling, or security checks
- Using deprecated APIs because they're "easier"
- Leaving TODO/FIXME comments
- Commenting out failing tests instead of fixing them
- Hardcoding values instead of using configuration
- Copy-pasting code instead of DRY principles
- Using `any` type instead of proper types
- Disabling linters or warnings

**Required Practices:**
- Full, proper implementation every time
- Complete validation and error handling
- Current best practices and non-deprecated APIs
- Zero TODOs or FIXMEs in committed code
- All tests passing (100%)
- Configuration-based, not hardcoded
- DRY - abstractions and imports
- Proper typing
- Clean linter reports

**Detection:**
```bash
# Pre-commit checks
grep -r "TODO\|FIXME" src/
grep -r "quick hack\|temporary" src/
grep -r "deprecated" src/
npm run lint -- --max-warnings 0
```

---

### Rule 2: No Disable Without Re-enable 🔄

**Principle:** If you turn something off, you MUST turn it back on.

**Forbidden Practices:**
- Disabling feature flags and leaving them off
- Commenting out validation permanently
- Skipping security checks without restoration
- Disabling linter rules permanently
- Setting DEBUG=true and leaving it
- Removing error boundaries without replacement
- Bypassing authentication for testing, never restoring

**Required Practices:**
- If disabled → re-enable in same commit
- If feature-flagged → remove flag when stable
- If check skipped → restore with comment explaining why
- Debug flags → only in debug builds, never production
- Document all temporary disablements with TODO + ticket

**Acceptable Pattern:**
```kotlin
// ✅ GOOD - conditional, not permanent
if (BuildConfig.DEBUG) {
    // Skip rate limiting in debug
} else {
    rateLimiter.check(request)
}
```

**Unacceptable Pattern:**
```kotlin
// ❌ BAD - permanently disabled
// rateLimiter.check(request)  // Disabled for testing
```

**Detection:**
```bash
# Check for commented-out code
git diff --cached | grep "^+.*//.*(" 

# Check for permanent disables
grep -r "disabled for\|skipped for\|bypassed for" src/
```

---

### Rule 3: 100% Function Equivalence in Refactoring ⚖️

**Principle:** Refactoring changes HOW, never WHAT.

**Forbidden Practices:**
- "Simplifying" code by removing edge case handling
- Changing return types or signatures
- Removing null/undefined checks
- Dropping error cases
- Changing execution order or timing
- Removing side effects without replacement
- "Optimizing" in ways that change behavior

**Required Practices:**
- Input X → Output Y (before) === Input X → Output Y (after)
- ALL edge cases preserved
- ALL error paths preserved
- Exact same contract (types, signatures, exceptions)
- Same timing, ordering, and side effects
- Tests prove equivalence (before/after passing)

**Verification Process:**

```bash
# 1. Run tests before refactoring
npm test
# Record: 47 tests, 0 failures

# 2. Perform refactoring
# ... make changes ...

# 3. Run tests after refactoring
npm test
# MUST: 47 tests, 0 failures
# MUST: No new tests required (same functionality)
# MUST: No skipped tests
```

**Equivalence Checklist:**
```
□ Same inputs produce same outputs
□ Same exceptions thrown for invalid inputs
□ Same side effects (file writes, API calls, etc.)
□ Same performance characteristics (no O(n) → O(n²))
□ Same thread safety guarantees
□ Same memory usage patterns
□ All existing tests pass unchanged
```

**Example - GOOD:**
```typescript
// Before
function getUserName(user: User): string {
    if (!user) throw new Error("User required");
    if (!user.name) throw new Error("Name required");
    return user.name.trim();
}

// After - refactored but EQUIVALENT
function getUserName(user: User): string {
    validateUser(user);
    validateUserName(user.name);
    return normalizeString(user.name);
}

function validateUser(user: User): void {
    if (!user) throw new Error("User required");
}

function validateUserName(name: string): void {
    if (!name) throw new Error("Name required");
}

function normalizeString(str: string): string {
    return str.trim();
}
```

**Example - BAD:**
```typescript
// Before
function getUserName(user: User): string {
    if (!user) throw new Error("User required");
    if (!user.name) throw new Error("Name required");
    return user.name.trim();
}

// After - "SIMPLIFIED" but BREAKS BEHAVIOR ❌
function getUserName(user: User): string {
    return user?.name?.trim() ?? "";  // ❌ Returns "" instead of throwing!
}
```

---

## Enforcement Mechanisms

### Pre-Commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "Running YOLO safeguard checks..."

# Check 1: No shortcuts
if git diff --cached | grep -iE "TODO|FIXME|hack|temporary"; then
    echo "❌ BLOCKED: TODOs/hacks found in commit"
    exit 1
fi

# Check 2: No disabled code
if git diff --cached | grep "^+.*//.*validate\|^+.*//.*check\|^+.*//.*security"; then
    echo "❌ BLOCKED: Commented validation/checks found"
    exit 1
fi

# Check 3: Tests must pass
npm test || {
    echo "❌ BLOCKED: Tests failing"
    exit 1
}

echo "✅ YOLO safeguards passed"
```

### Post-Implementation Review

After `.yolo` execution, automatically run `/review .swarm` to verify:
- No shortcuts taken
- Nothing disabled without re-enabling
- Function equivalence maintained

### Automated Checklist

```markdown
## YOLO Pre-Commit Verification

### Rule 1: No Shortcuts
- [ ] No TODO/FIXME comments
- [ ] No hardcoded values
- [ ] No copy-pasted code
- [ ] All tests passing
- [ ] Linter clean (0 warnings)
- [ ] No deprecated APIs used

### Rule 2: No Disable Without Re-enable
- [ ] No commented-out code
- [ ] All feature flags removed or documented
- [ ] All validations active
- [ ] No debug flags in production code
- [ ] Security checks all active

### Rule 3: 100% Function Equivalence
- [ ] Same number of tests passing
- [ ] No behavior changes
- [ ] All edge cases preserved
- [ ] Same contract (types, exceptions)
- [ ] Performance characteristics preserved
```

---

## Consequences of Violations

| Violation | Action |
|-----------|--------|
| Shortcuts found | Commit blocked, must fix |
| Disabled code without re-enable | Commit blocked, must restore |
| Function equivalence broken | Commit blocked, must revert |
| Tests failing | Commit blocked, must fix |

**Zero Tolerance:** YOLO mode violations result in automatic commit rejection.

---

## Version History

- **v9.0** (2025-11-26): Initial safeguards protocol
  - Rule 1: No shortcuts
  - Rule 2: No disable without re-enable
  - Rule 3: 100% function equivalence

---

**Status:** Active
**Enforcement:** Mandatory
**Scope:** All `.yolo` operations
