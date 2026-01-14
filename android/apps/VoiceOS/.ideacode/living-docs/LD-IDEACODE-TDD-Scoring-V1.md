# LD-IDEACODE-TDD-Scoring-V1

## Purpose
Intelligent Test-Driven Development scoring system for automatic TDD recommendation and enforcement.

---

## TDD Score Calculation

### Formula
```
TDD Score = 0 + (positive signals) - (negative signals)
```

### Positive Signals (Add Points)

| Signal | Keywords/Patterns | Points |
|--------|-------------------|--------|
| Business Logic | `auth`, `login`, `payment`, `checkout`, `validate`, `verify`, `calculate`, `process`, `encrypt`, `decrypt`, `hash`, `token`, `permission`, `role`, `pricing`, `discount`, `tax`, `invoice`, `transaction` | +30 |
| Bug Fix | Command is `/ifix` OR contains: `fix`, `broken`, `bug`, `error`, `crash`, `fail`, `issue` | +25 |
| Refactor | Command is `/irefactor` OR contains: `refactor`, `cleanup`, `reorganize`, `restructure`, `simplify` | +20 |
| Critical Path | File path contains: `auth/`, `payment/`, `security/`, `data/`, `core/`, `api/`, `service/` | +15 |
| Multiple Files | Analysis shows >3 files modified | +10 |
| Complex Logic | Code has: >3 branches, nested conditions, recursion, state machines | +10 |
| Data Integrity | Keywords: `database`, `migration`, `schema`, `constraint`, `transaction`, `rollback` | +10 |
| Concurrency | Keywords: `async`, `thread`, `mutex`, `lock`, `concurrent`, `parallel`, `race` | +10 |

### Negative Signals (Subtract Points)

| Signal | Keywords/Patterns | Points |
|--------|-------------------|--------|
| Trivial Change | `typo`, `copy`, `text`, `comment`, `rename`, `spelling`, `wording`, `label`, `string` | -30 |
| UI Only | Files: `*.xml`, `*.css`, `*.scss`, `*.styl` OR keywords: `@Composable`, `SwiftUI`, `View`, `Screen`, `Layout`, `Style`, `Color`, `Theme`, `Animation` | -20 |
| Config Files | Files: `*.json`, `*.yml`, `*.yaml`, `*.gradle`, `*.toml`, `*.properties`, `*.env` | -15 |
| Documentation | Files: `*.md`, `*.txt`, `*.rst` OR keywords: `README`, `docs`, `comment` | -15 |
| Prototype Mode | `.prototype` modifier used | -10 |
| Simple CRUD | Keywords: `list`, `show`, `display` (without business logic) | -10 |

---

## Thresholds

| Score | Level | Action |
|-------|-------|--------|
| < 50 | None | Proceed without TDD |
| 50-69 | Recommended | Ask: "TDD Recommended. Use TDD? (Y/n)" |
| 70-89 | Strongly Recommended | Ask: "TDD Strongly Recommended. Use TDD? (Y/n)" |
| >= 90 | Required | Enforce TDD. Must use `.skip-tdd "reason"` to override |

---

## TDD Workflow

### Standard TDD Flow (Red-Green-Refactor)
```
1. RED: Write failing test that defines expected behavior
2. GREEN: Write minimal code to make test pass
3. REFACTOR: Improve code while keeping tests green
4. REPEAT: Next requirement
```

### Integration with Commands

| Command | TDD Behavior |
|---------|--------------|
| `/ifix` | Calculate score → If >= 50, recommend TDD → Write test that fails → Fix → Test passes |
| `/idevelop` | Calculate score → If >= 50, ask TDD → Generate tests alongside code |
| `/irefactor` | Always TDD (tests must pass before AND after) |
| `/iimplement` | Calculate per-task score → TDD for high-score tasks |

---

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.tdd` | Force TDD mode regardless of score |
| `.skip-tdd` | Skip TDD (requires reason if score >= 90) |
| `.tdd-strict` | TDD + mutation testing + 100% coverage |

---

## Score Display Format

```
TDD Analysis: "payment validation failing"

Signals Detected:
  + 25  Bug fix (/ifix command)
  + 30  Business logic (payment, validation)
  + 15  Critical path (payment/)
  ─────────────────────────────
  = 70  TDD Strongly Recommended

→ Use TDD? (Y/n): _
```

---

## Skip Override (Score >= 90)

When score >= 90 and user wants to skip:
```
TDD Score: 95 (Required)

To skip TDD, provide a reason:
/ifix .skip-tdd "hotfix for production, will add tests in follow-up"

→ Reason recorded. Proceeding without TDD.
→ TODO created: "Add tests for [fix description]"
```

---

## Examples

### Example 1: High Score (TDD Required)
```
/ifix "authentication token refresh race condition"

TDD Score: 95
  + 25  Bug fix
  + 30  Business logic (authentication, token)
  + 15  Critical path (auth)
  + 10  Concurrency (race condition)
  + 15  Critical path (implied)

→ TDD Required. Write failing test first.
```

### Example 2: Medium Score (TDD Recommended)
```
/idevelop "add user profile validation"

TDD Score: 55
  + 30  Business logic (validation)
  + 15  Critical path (user)
  + 10  Multiple files

→ TDD Recommended. Use TDD? (Y/n): _
```

### Example 3: Low Score (No TDD)
```
/ifix "fix button alignment on settings screen"

TDD Score: -15
  + 25  Bug fix
  - 20  UI only (button, screen)
  - 20  UI only (settings screen)

→ Proceeding without TDD.
```

### Example 4: Forced TDD
```
/idevelop .tdd "add dark mode toggle"

TDD Score: -10 (UI only)
.tdd modifier: FORCED

→ TDD Mode enabled (forced by modifier).
```

---

## Configuration (Optional)

Projects can customize in `.ideacode/config.yml`:

```yaml
tdd:
  enabled: true
  threshold_recommend: 50
  threshold_strong: 70
  threshold_required: 90
  custom_signals:
    positive:
      - pattern: "inventory"
        points: 20
    negative:
      - pattern: "prototype"
        points: -25
```

---

## Metadata
- **Document:** LD-IDEACODE-TDD-Scoring-V1
- **Version:** 1.0
- **Created:** 2025-12-05
- **Author:** IDEACODE
