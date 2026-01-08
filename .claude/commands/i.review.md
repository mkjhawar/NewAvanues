---
description: Post-write analysis with quality gates | /i.review | /i.review .auto | /i.review .swarm
---

# /i.review - Mandatory Post-Write Analysis

---

## Overview

Comprehensive quality review triggered after code changes. Uses TodoWrite for checklist tracking with real-time progress display.

---

## Usage

`/i.review [target] [modifiers]`

## Arguments

| Arg | Required | Description |
|-----|----------|-------------|
| target | No | File, folder, or module to review (defaults to changed files) |

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.auto` | Auto-trigger mode (called by other commands) |
| `.swarm` | Parallel expert analysis with agents |
| `.yolo` | Auto-fix issues without prompting |
| `.phase` | Post-phase review (lighter, focused) |
| `.sprint` | Post-sprint review (comprehensive) |
| `.quick` | Fast mode - critical checks only |
| `.full` | Full mode - all 6 categories |
| `.docs` | Include developer/user manual checks |

---

## Trigger Points

### Automatic Invocation

| Event | Trigger | Mode |
|-------|---------|------|
| `/i.implement` phase complete | Auto | `.phase` |
| `/i.implement` all phases complete | Auto | `.sprint` |
| `/i.develop` complete | Auto | `.full` |
| `/i.fix` complete | Auto | `.quick` |
| `/i.refactor` complete | Auto | `.full` |

### Manual Invocation

```
/i.review                    # Review changed files
/i.review src/Auth.kt        # Review specific file
/i.review .swarm             # Multi-agent expert review
/i.review .docs              # Include documentation checks
```

---

## Review Categories (TodoWrite Checklist)

### 1. COMPLETENESS CHECK

| Check | Criteria | Priority |
|-------|----------|----------|
| No TODO/FIXME left unaddressed | Search for markers | P0 |
| All imports resolved | No unresolved imports | P0 |
| No missing implementations | No abstract methods unfilled | P0 |
| All interface methods implemented | Complete contracts | P0 |

### 2. TECHNIQUE VALIDATION

| Check | Criteria | Priority |
|-------|----------|----------|
| Using current best practices | No deprecated APIs | P1 |
| Following language idioms | Kotlin/Swift/TS conventions | P1 |
| Proper error handling patterns | No swallowed exceptions | P0 |
| Consistent code style | Match project standards | P2 |

### 3. SECURITY SCAN

| Check | Criteria | Priority |
|-------|----------|----------|
| No hardcoded secrets | Scan for API keys, passwords | P0 |
| Input validation present | User inputs sanitized | P0 |
| SQL injection prevention | Parameterized queries | P0 |
| XSS prevention | Output encoding | P0 |
| OWASP Top 10 compliance | Full scan | P1 |

### 4. QUALITY GATES

| Check | Criteria | Priority |
|-------|----------|----------|
| Functions < 30 lines | Line count check | P1 |
| Cyclomatic complexity < 10 | Complexity analysis | P1 |
| No code duplication | DRY principle | P1 |
| Test coverage >= 80% | Coverage report | P1 |

### 5. DEVELOPER MANUAL (Mandatory)

| Check | Criteria | Priority |
|-------|----------|----------|
| API documentation for public methods | KDoc/JSDoc present | P0 |
| Architecture docs for new components | Living doc updated | P1 |
| README updated with new features | Module README current | P1 |
| CHANGELOG entry added | Version tracking | P1 |

### 6. USER MANUAL (if UI changes)

| Check | Criteria | Priority |
|-------|----------|----------|
| User guide for new features | End-user docs | P1 |
| Screenshots/mockups captured | Visual documentation | P2 |
| Error messages user-friendly | UX check | P1 |
| Help text updated | In-app help | P2 |

---

## TodoWrite Integration

### Checklist Format

```
## Post-Write Review

### 1. Completeness Check
- [ ] No TODO/FIXME left unaddressed
- [ ] All imports resolved
- [ ] No missing class/function implementations
- [ ] All interface methods implemented

### 2. Technique Validation
- [ ] Using current best practices (not deprecated APIs)
- [ ] Following language idioms
- [ ] Proper error handling patterns

### 3. Security Scan
- [ ] No hardcoded secrets
- [ ] Input validation present
- [ ] SQL injection prevention

### 4. Quality Gates
- [ ] Functions < 30 lines
- [ ] Cyclomatic complexity < 10
- [ ] No code duplication

### 5. Developer Manual
- [ ] API documentation for public methods
- [ ] Architecture docs for new components
- [ ] README updated with new features
- [ ] CHANGELOG entry added

### 6. User Manual (UI changes only)
- [ ] User guide for new features
- [ ] Screenshots/mockups captured
- [ ] Error messages user-friendly
```

### Progress Display

```
╔══════════════════════════════════════════════════════════════╗
║  🔍 POST-WRITE REVIEW IN PROGRESS                            ║
╚══════════════════════════════════════════════════════════════╝

Phase 1/6: Completeness Check
  [✓] No TODO/FIXME left unaddressed
  [✓] All imports resolved
  [▶] Checking implementations...
  [ ] All interface methods implemented

Progress: 2/4 checks (50%)
```

### Real-Time Updates

| Event | TodoWrite Action |
|-------|------------------|
| Check started | Mark `in_progress` |
| Check passed | Mark `completed` with ✓ |
| Check failed | Keep `in_progress`, show issue |
| Category done | Show summary, next category |
| All done | Final report with score |

---

## Output Format

### Console Display (Real-Time)

```
╔══════════════════════════════════════════════════════════════╗
║  🔍 POST-WRITE REVIEW: src/Auth.kt                           ║
╚══════════════════════════════════════════════════════════════╝

### 1. Completeness Check
  [✓] No TODO/FIXME markers found
  [✓] All imports resolved (12 imports)
  [✓] No missing implementations
  [✓] All interface methods implemented
  → PASS (4/4)

### 2. Technique Validation
  [✓] Using current best practices
  [✓] Following Kotlin idioms
  [⚠] Error handling: 2 catch blocks swallow exceptions
  → WARN (2/3)

### 3. Security Scan
  [✓] No hardcoded secrets
  [✓] Input validation present
  [✓] SQL injection prevention (N/A)
  → PASS (3/3)

### 4. Quality Gates
  [✓] Functions < 30 lines (max: 24)
  [✓] Cyclomatic complexity < 10 (max: 6)
  [⚠] Code duplication: 1 block (8 lines)
  → WARN (2/3)

### 5. Developer Manual
  [✓] API documentation present
  [✗] Architecture docs missing for AuthManager
  [✓] README is current
  [✓] CHANGELOG updated
  → FAIL (3/4)

══════════════════════════════════════════════════════════════

SUMMARY: 15/17 checks passed (88%)
  ✓ PASS: 15
  ⚠ WARN: 2
  ✗ FAIL: 1

ACTIONS REQUIRED:
  1. Add architecture doc for AuthManager
  2. Fix exception handling in lines 45, 89
  3. Deduplicate validation logic

Run: /i.fix .yolo to auto-fix issues
Run: /i.review .swarm for expert analysis
```

---

## .swarm Mode - Expert Analysis

When `.swarm` modifier is used:

| Agent | Focus |
|-------|-------|
| Security Expert | OWASP, secrets, injection |
| Architecture Expert | SOLID, patterns, coupling |
| Performance Expert | Complexity, bottlenecks |
| Documentation Expert | API docs, manuals |
| Test Expert | Coverage, edge cases |

### Swarm Output

```
╔══════════════════════════════════════════════════════════════╗
║  🐝 SWARM REVIEW: 5 Experts Analyzing                        ║
╚══════════════════════════════════════════════════════════════╝

[Security Expert] ✓ No critical issues (2 minor warnings)
[Architecture Expert] ✓ SOLID compliance: 8/10
[Performance Expert] ✓ O(n) complexity, no bottlenecks
[Documentation Expert] ⚠ Missing: AuthManager architecture doc
[Test Expert] ✓ Coverage: 87%, edge cases covered

CONSENSUS: APPROVED with minor fixes
```

---

## File Output

Creates review report in module docs folder:

| Input Type | Output Location |
|------------|-----------------|
| `Modules/VoiceOS/...` | `Docs/VoiceOS/Reviews/Review-{component}-YYMMDD-V1.md` |
| `Modules/AVA/...` | `Docs/AVA/Reviews/Review-{component}-YYMMDD-V1.md` |
| `src/...` | `Docs/Project/Reviews/Review-{component}-YYMMDD-V1.md` |

### Report Template

```markdown
# Code Review: {component}

**Date:** {date}
**Reviewer:** Claude (AI)
**Target:** {file_path}
**Score:** {score}/100

## Summary
{overall_assessment}

## Findings

### Critical (P0)
{critical_issues}

### High (P1)
{high_issues}

### Medium (P2)
{medium_issues}

## Recommendations
{action_items}

## Metrics
- Lines changed: {lines}
- Complexity: {complexity}
- Coverage: {coverage}%
```

---

## Quick Mode (.quick)

For fast reviews, only checks:

| Category | Checks |
|----------|--------|
| Completeness | TODO/FIXME only |
| Security | Hardcoded secrets only |
| Quality | Complexity only |

---

## Integration with Other Commands

### /i.implement Integration

```kotlin
// After each phase completes:
fun onPhaseComplete(phase: Phase) {
    runReview(mode = ".phase", target = phase.changedFiles)
}

// After all phases complete:
fun onImplementComplete() {
    runReview(mode = ".sprint", target = allChangedFiles)
}
```

### /i.develop Integration

```kotlin
// After development workflow completes:
fun onDevelopComplete() {
    runReview(mode = ".full .docs", target = featureFiles)
}
```

---

## Exit Criteria

| Condition | Action |
|-----------|--------|
| All P0 checks pass | Proceed to next step |
| Any P0 check fails | BLOCK - must fix before proceeding |
| P1 checks >= 80% pass | Proceed with warnings |
| P1 checks < 80% pass | Recommend fixes |

---

## Examples

| Command | Behavior |
|---------|----------|
| `/i.review` | Review all changed files |
| `/i.review src/Auth.kt` | Review specific file |
| `/i.review .quick` | Fast critical-only review |
| `/i.review .full .docs` | Complete review with docs |
| `/i.review .swarm` | Multi-agent expert review |
| `/i.review .yolo` | Auto-fix all issues |
| `/i.review .phase` | Post-phase review (lighter) |

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/i.analyze` | Deep code analysis |
| `/i.fix` | Fix identified issues |
| `/i.document` | Generate missing docs |
| `/i.refactor` | Improve code quality |

---

## Metadata

- **Command:** `/i.review`
- **Version:** 1.0
- **TodoWrite:** YES (checklist tracking)
- **Auto-trigger:** YES (after phases/sprints)
- **Swarm:** YES (expert analysis)
