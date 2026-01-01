---
description: Bug fixing .yolo .swarm .tdd .ood .investigate .issue .tcr | /i.fix .yolo "auth bug"
---

# v14.0 - Autonomous Loop Support

**New Modifier:** `.autonomous` - Run until bug is fixed with safety limits

```
/i.fix .autonomous "memory leak"      # Run until fixed (100 calls/hr, 30 min max)
/i.fix .autonomous .yolo "auth race"  # Autonomous + auto-approve
```

API: `POST /v1/autonomous/start` → `POST /v1/autonomous/check` → auto-stop on completion

---

# /i.fix - Intelligent Bug Fix Workflow

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.fix`
Auto-start: API server starts automatically if not running

---


## Usage
`/fix [flags] <bug_description>`

## Arguments
| Arg | Required | Description |
|-----|----------|-------------|
| description | Yes | Bug to fix (short phrase) |

## Flags
| Flag | Effect |
|------|--------|
| `.autonomous` | Run until fixed (100 calls/hr, 30 min max, circuit breaker) |
| `.yolo` | Auto-progress through all stages |
| `.investigate` | Investigate root cause first (see Investigation Mode) |
| `.issue` | Deep issue analysis with ToT/CoT (alias for .investigate .tot) |
| `.module` | Specify affected module: `.module "UserService"` |
| `.fix` | After investigation, proceed to fix (use with `.investigate`) |
| `.report` | Investigation only, output report, never fix |
| `.tdd` | Force TDD mode (write failing test first) |
| `.skip-tdd` | Skip TDD (requires reason if score >= 90) |
| `.ood` | Analyze for OOD pattern opportunities |
| `.skip-ood` | Skip OOD analysis |
| `.solid` | Enforce SOLID principles in fix |
| `.stop` | Disable workflow chaining |
| `.tcr` | Test && Commit \|\| Revert protocol |
| `.cot` | Show Chain of Thought reasoning |
| `.tot` | Tree of Thought multi-solution exploration |
| `.advanced` | Force tree-based analysis |
| `.swarm` | Force multi-agent mode |
| `.security` | Apply security standards |
| `.test` | Generate test cases |
| `.docs` | Update documentation |

## Workflow Phases
| # | Phase | Action | Auto-Next |
|---|-------|--------|-----------|
| -1 | Intelligence | Intent detection → redirect if needed | ✓ |
| 0 | Specialist | Spawn domain expert (OS/PhD/Senior level) | ✓ |
| 1 | Analyze | CoT/ToT deep code analysis (MANDATORY) | ✓ |
| 2 | Gather | Specialist collects info, locates issue | YOLO only |
| 3 | Fix | Execute with domain expertise | YOLO only |
| 4 | Test | Verify quality gates | YOLO only |
| 5 | Commit | Document and commit | YOLO only |
| 6 | Chain | Detect debt, related bugs | Manual |

---

## Investigation Mode

Use when you see symptoms but don't know the root cause.

### Investigation Modifiers
| Modifier | Behavior |
|----------|----------|
| `.investigate` | Investigate → output report → **ASK**: "Proceed to fix? (Y/n)" |
| `.investigate .fix` | Investigate → ask confirmation → fix |
| `.investigate .report` | Investigate → output report only (never fix) |
| `.investigate .yolo` | Investigate → auto-fix without confirmation |

### Default Behavior (No Modifiers)
When `/i.fix "description"` is called without modifiers:
1. Assess if cause is known or unknown
2. If unknown → auto-trigger investigation phase
3. After investigation → **ASK**: "Root cause identified. Proceed to fix? (Y/n)"
4. User confirms → proceed to fix phase

### Investigation Output
```
## Investigation Report: {description}

### Symptoms
- {observed behavior}

### Analysis Method
CoT | ToT (auto-selected based on complexity)

### Files Examined
| File | Relevance |
|------|-----------|
| {path} | {finding} |

### Root Cause
**Confidence:** {%}
**Cause:** {explanation}
**Location:** {file:line}

### Recommended Fix
{approach}

### Impact Assessment
- Files affected: {N}
- Risk level: Low | Medium | High
```

### Investigation Examples
| Command | Behavior |
|---------|----------|
| `/i.fix "app running slow"` | Auto-investigate → ask to fix |
| `/i.fix .investigate "memory spike"` | Investigate → ask to fix |
| `/i.fix .investigate .report "random crash"` | Investigate only → report |
| `/i.fix .investigate .yolo "timeout error"` | Investigate → auto-fix |
| `/i.fix .investigate .fix "auth failing"` | Investigate → confirm → fix |

---

## TDD Auto-Detection (Intelligent Test-Driven Development)

### TDD Score Calculation
Automatically calculated before fix execution:

**Positive Signals (Add Points):**
| Signal | Detection | Points |
|--------|-----------|--------|
| Business Logic | `auth`, `payment`, `validate`, `calculate`, `process`, `encrypt`, `token` | +30 |
| Bug Fix | `/i.fix` command or `fix`, `broken`, `bug`, `error` | +25 |
| Critical Path | Path contains: `auth/`, `payment/`, `security/`, `data/`, `core/` | +15 |
| Multiple Files | >3 files affected | +10 |
| Complex Logic | >3 branches, nested conditions, concurrency | +10 |
| Data Integrity | `database`, `migration`, `transaction` | +10 |

**Negative Signals (Subtract Points):**
| Signal | Detection | Points |
|--------|-----------|--------|
| Trivial | `typo`, `copy`, `text`, `comment`, `rename` | -30 |
| UI Only | `*.css`, `@Composable`, `SwiftUI`, `View`, `Screen` | -20 |
| Config | `*.json`, `*.yml`, `*.gradle` | -15 |

### TDD Thresholds
| Score | Action |
|-------|--------|
| < 50 | Proceed without TDD |
| 50-69 | Ask: "TDD Recommended. Use TDD? (Y/n)" |
| 70-89 | Ask: "TDD Strongly Recommended. Use TDD? (Y/n)" |
| >= 90 | Enforce TDD (must use `.skip-tdd "reason"` to override) |

### TDD Workflow (When Activated)
```
1. RED: Write failing test that reproduces the bug
2. Verify test fails (confirms bug exists)
3. GREEN: Implement minimal fix
4. Verify test passes (confirms bug fixed)
5. Run full test suite
6. Commit with test
```

### TDD Display Format
```
TDD Analysis: "payment validation failing"

Signals:
  + 25  Bug fix (/i.fix)
  + 30  Business logic (payment, validation)
  + 15  Critical path (payment/)
  ─────────────────────────────
  = 70  TDD Strongly Recommended

→ Use TDD? (Y/n): _
```

### TDD Examples
| Command | Score | Behavior |
|---------|-------|----------|
| `/i.fix "login token expired"` | 70 | TDD Strongly Recommended |
| `/i.fix "fix typo in error"` | -5 | No TDD |
| `/i.fix .tdd "button color"` | -20 | TDD Forced (modifier) |
| `/i.fix .skip-tdd "hotfix" "auth race"` | 90 | TDD Skipped (reason logged) |

---

## Phase 0: Domain Specialist (MANDATORY - ALL FIXES)

**Always spawn specialist based on fix domain.**

| Fix Domain | Specialist | Level |
|------------|------------|-------|
| Memory, crash, ANR, drivers | Systems | OS-Level |
| Threading, concurrency, race | Concurrency | OS-Level |
| Platform (Android/iOS internals) | Platform | OS-Level |
| Crypto, auth, tokens, permissions | Security | PhD-Level |
| Algorithms, complexity, optimization | Algorithm | PhD-Level |
| Distributed systems, consensus | Distributed | PhD-Level |
| Network, API, latency | Network | Senior |
| Database, queries, migrations | Data | Senior |
| UI, animations, layout | UI | Senior |

---

## Phase 1: Code Analysis (MANDATORY - ALL FIXES)

**Deep analysis of affected code before any fix attempt.**

| Analysis Type | When | Method |
|---------------|------|--------|
| **CoT (Chain of Thought)** | Clear cause, linear path | Step-by-step reasoning through code flow |
| **ToT (Tree of Thought)** | Unclear cause, multiple possibilities | Branch exploration, hypothesis testing |

### Analysis Steps
```
1. Read affected file(s) completely
2. Trace call stack / data flow
3. Identify all touch points (what else uses this code?)
4. Document assumptions
5. Form hypothesis with CoT or ToT
6. Validate hypothesis before implementing fix
```

### Output Required
```
ANALYSIS:
- Files read: [list]
- Call flow: A → B → C
- Impact scope: [components affected]
- Hypothesis: [root cause theory]
- Confidence: [%]
- Method: CoT | ToT
```

## Intelligence Layer (Pre-Flight)
Auto-detects intent and routes accordingly:

| Detection | Confidence | Action |
|-----------|-----------|--------|
| Feature request ("add", "implement", "create") | 85-95% | Redirect to `/idevelop` |
| Multiple issues | 90% | Route to `/plan → /implement` |
| Needs investigation ("random", "inconsistent") | 80% | Route to `/debug → /analyze` |
| Standard bug fix | 100% | Proceed with fix |

## Complexity Analysis
Auto-determines optimal strategy:

| Complexity | Indicators | Strategy | Files | Tasks |
|-----------|-----------|----------|-------|-------|
| SIMPLE | Single file, clear cause, isolated | Linear | 1-2 | 1 |
| MODERATE | 2-3 components, reproducible | Linear + investigate | 2-5 | 2-3 |
| COMPLEX | Multiple layers, unclear root cause | Tree-based (L0-L4) | 5-10 | 4-8 |
| MULTI_DOMAIN | Security+Performance, UX+Data, etc. | Swarm (specialist agents) | 10+ | 8+ |

**Indicators by Complexity:**

SIMPLE: "button not working", "text not showing", "crash on X action"

MODERATE: "sometimes fails", "only on Android", "specific user type"

COMPLEX: "randomly fails", "inconsistent behavior", "production-only issue"

MULTI_DOMAIN: "auth slow" (security+performance), "crash + memory leak" (ui+data)

## Strategy A: Linear Fix (SIMPLE/MODERATE)

| Step | Action |
|------|--------|
| 1.1 | Load standards: `ideacode_standards .context "bug fix: {description}"` |
| 1.2 | Search codebase for related code patterns |
| 1.3 | Read affected files and identify root cause |
| 1.4 | Implement minimal fix (no scope creep) |
| 1.5 | Create test (must fail before fix, pass after) |
| 1.6 | Verify quality gates (tests pass, build succeeds, no lint errors) |

**Quality Gates (MANDATORY):**
- [ ] Bug test fails before fix
- [ ] Bug test passes after fix
- [ ] All existing tests pass
- [ ] No lint errors
- [ ] Build succeeds

## Strategy B: Tree-Based Analysis (COMPLEX)

| Level | Action |
|-------|--------|
| L0 | Create symptom tree (3+ hypotheses) |
| L1 | Layer isolation via binary search (UI / Business / Data) |
| L2 | Component-level analysis within identified layer |
| L3 | Function/method-level root cause |
| L4 | Root cause confirmed |

**Layer Isolation Protocol:**
1. Test Business Logic layer (middle) first
2. If passes → issue in UI or Data layer
3. If fails → issue in Business Logic
4. Create component tree within identified layer
5. Document findings: `LAYER: {X}, COMPONENT: {Y}, ROOT CAUSE: {Z}`

**Modifiers:** `.cot` shows reasoning at each level; `.tot` explores multiple solution branches

**Output:** Fix document at `docs/fixes/advanced/{MODULE}-{ISSUE}-{DATE}.md` with complete L0-L4 analysis

## Strategy C: Swarm Mode (MULTI_DOMAIN)

| Step | Action |
|------|--------|
| C.1 | Identify domains (Security, Performance, UX, Data, Platform) |
| C.2 | Spawn specialist agents (1 per domain + Scrum Master) |
| C.3 | Load: `Protocol-Swarm-Coordination-v1.0.md` |
| C.4 | Each agent analyzes from domain perspective |
| C.5 | Scrum Master detects conflicts and synthesizes unified solution |
| C.6 | All agents validate final solution |

**Output:** Swarm document at `docs/fixes/swarm/{SESSION-ID}-{DATE}.md` with all agent contributions

## Commit Guidelines

| Complexity | Format |
|-----------|--------|
| SIMPLE | `fix: {short description}` → Issue + Root Cause + Solution + Test |
| COMPLEX | Advanced analysis tree (L0→L4) + Documentation path |
| MULTI_DOMAIN | Domains involved + Agent contributions + Documentation path |

**Example (COMPLEX):**
```
fix: fix random user validation failures

Advanced Analysis:
- L0 (System): User validation randomly fails during login
- L1 (Layer): Business Logic Layer
- L2 (Component): UserValidationService
- L4 (Root Cause): Race condition in token refresh

Solution: Add mutex lock to token refresh
Documentation: docs/fixes/advanced/AUTH-RACE-CONDITION-20251129.md

Fixes #1234
```

## OOD Analysis (Object-Oriented Development)

When fixing bugs, OOD analysis can identify structural issues:

### OOD Detection During Fix
| Detection | OOD Suggestion |
|-----------|----------------|
| God class (>500 lines, >10 methods) | Split into Entity + Service |
| Data clump (same fields repeated) | Extract Value Object |
| Feature envy (method uses other class data) | Move to appropriate class |
| Shotgun surgery (change requires many files) | Create Aggregate |
| Primitive obsession | Create Value Object |

### OOD Score in Fix Context
| Score | Action |
|-------|--------|
| < 40 | Fix only, no OOD suggestions |
| 40-59 | Suggest: "Consider OOD refactoring after fix" |
| >= 60 | Ask: "Apply OOD patterns as part of fix?" |

### OOD + Fix Workflow
When OOD is recommended during fix:
```
1. Analyze root cause
2. Identify OOD violations
3. Fix immediate bug
4. Offer OOD refactoring: "Prevent similar bugs with OOD? (Y/n)"
5. If yes: Apply patterns (Entity/Value Object/Service)
```

## Post-Fix Intelligence Chaining

If `.stop` modifier NOT present, auto-detect:

| Detection | Trigger | Recommendation |
|-----------|---------|-----------------|
| Technical debt | Debt score > 50 | `/refactor "{components}"` |
| Related bugs | Similar patterns found | `/fix "{pattern} in other locations"` |
| Doc changes | API/behavior modified | `/document .api` or `.user` |
| Coverage gap | <90% coverage | Generate additional tests |
| OOD opportunity | OOD score >= 60 | `/refactor .ood "{class}"` |

**Example Chain (YOLO mode):**
```
 Bug fix complete
  Debt score: 62/100
 Related bugs: 3 locations found
 API documentation needs update

Auto-executing chains:
  → /refactor (debt 62)
  → /fix (3 related bugs)
  → /document .api (API changed)
```

## Quality Gates (ALL STRATEGIES)
| Metric | Target | Enforced |
|--------|--------|----------|
| Test coverage | 90%+ | YES |
| Bug test passes | 100% | YES |
| Existing tests | 100% pass | YES |
| Build success | 100% | YES |
| No blockers | 0 | YES |

## Examples
| Command | Behavior |
|---------|----------|
| `/i.fix "login crash"` | Interactive → complexity analysis → linear fix → manual approval |
| `/i.fix .yolo "auth slow" .security` | Auto: intent check → complexity → linear fix → test → commit + security standards |
| `/i.fix .advanced .cot "random freeze"` | Force tree-based → show reasoning at each level → interactive approval |
| `/i.fix .tcr "memory leak" .test` | Test && Commit \|\| Revert + generate test standards |
| `/i.fix .swarm "auth slow + UI freeze"` | Multi-domain → spawn agents → swarm coordination → unified fix |
| `/i.fix .ood "UserService bug"` | Analyze OOD patterns → suggest Entity/Service split |
| `/i.fix .tdd .ood "order validation"` | TDD + OOD combined (test first + proper modeling) |

## Modes Comparison
| Mode | Approvals | Chaining | Testing | Use Case |
|------|-----------|----------|---------|----------|
| Interactive (default) | Yes | Optional | Full | Standard bugs |
| YOLO (`.yolo`) | No | Auto | Full | Simple bugs (<30 min) |
| Manual (`.stop`) | Yes | None | Full | Complex review needed |
| Advanced (`.advanced`) | Yes | Optional | Full | Complex bugs |
| Swarm (`.swarm`) | Yes | Auto | Full | Multi-domain bugs |

## MCP Integration
Uses: `ideacode_standards`, `ideacode_commit`, `ideacode_test`, `ideacode_research`, `ideacode_checkpoint`

## Related Commands
| Command | Purpose | When to Use |
|---------|---------|-------------|
| `/idevelop` | Feature development | Adding functionality |
| `/iplan` | Create fix plan | Multiple related issues |
| `/debug` | Root cause investigation | Unknown cause |
| `/analyze` | Code analysis | Component impact assessment |
| `/refactor` | Technical debt cleanup | After fix (if debt detected) |
| `/document` | Update docs | API/behavior changes |

## Error Handling
On error at any stage:
1. Stop workflow
2. Report error with location
3. Offer recovery: Retry / Skip / Debug
4. Use `ideacode_checkpoint` to analyze

## Documentation
- Swarm protocol: `Protocol-Swarm-Coordination-v1.0.md`
- Code review: `Protocol-Code-Review-v1.0.md`
- YOLO safeguards: `CLAUDE.md` (No shortcuts, 100% function equivalence)

## Metadata
- **Command:** `/i.fix`
- **Version:** 10.2
- **Intelligence:** YES (intent detection, complexity analysis, chaining, TDD, OOD)
- **Strategies:** Linear (simple), Tree-Based (complex), Swarm (multi-domain)
- **TDD:** Auto-detection
- **OOD:** Auto-detection (structural issues)
- **Enforcement:** Quality gates MANDATORY, YOLO safeguards enforced
