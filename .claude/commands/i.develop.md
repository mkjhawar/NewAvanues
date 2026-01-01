---
description: Full workflow .yolo .swarm .tdd .ood .wiz .idea | /i.develop .yolo "dark mode"
---

# v14.0 - Autonomous Loop Support

**New Modifier:** `.autonomous` - Run until completion with safety limits

```
/i.develop .autonomous "feature"     # Run until done (100 calls/hr, 30 min max)
/i.develop .autonomous .yolo "auth"  # Autonomous + auto-approve
```

API: `POST /v1/autonomous/start` → `POST /v1/autonomous/check` → auto-stop on completion

---

# /i.develop - Development Workflow

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.develop`
Auto-start: API server starts automatically if not running

---


## Usage
`/develop [description] [flags]`

## Arguments
| Arg | Required | Description |
|-----|----------|-------------|
| description | No | Feature to build (will prompt if omitted) |

## Flags
| Flag | Effect |
|------|--------|
| `.autonomous` | Run until completion (100 calls/hr, 30 min max, circuit breaker) |
| `.yolo` | Auto-progress, no approvals |
| `.tdd` | Force TDD mode (tests first) |
| `.skip-tdd` | Skip TDD recommendation |
| `.ood` | Force OOD analysis and patterns |
| `.skip-ood` | Skip OOD recommendation |
| `.ddd` | Full DDD analysis (Entity, Aggregate, Repository) |
| `.solid` | Enforce SOLID principles check |
| `.stop` | Disable chaining, manual control |
| `.cot` | Show reasoning |
| `.tot` | Tree of Thought exploration |
| `.swarm` | Force swarm mode |
| `.no-swarm` | Disable swarm |
| `.tutor` | Educational mode with explanations |
| `.prototype` | Defer testing, lightweight docs |
| `.wiz` | Full wizard mode (tutor + checkpoints + interactive guidance) |
| `.idea` | Business planning pre-phase (market, financials, pitch) |
| `.checkpoint-freq` | Checkpoint frequency: low\|medium\|high (default: medium) |

## Workflow Phases
| # | Phase | Action | Auto-Next |
|---|-------|--------|-----------|
| -1 | Intelligence | Complexity analysis → redirect if needed | ✓ |
| 0 | Q&A | 5 questions (what/why/who/project/constraints) | YOLO only |
| 1 | Specify | Delta spec, platform detection | YOLO only |
| 2 | Plan | Platform phases, KMP strategy | YOLO only |
| 3 | Tasks | Breakdown, dependencies, batches | YOLO only |
| 4 | Implement | Execute (IDE Loop: Implement→Defend→Evaluate→Commit) | YOLO only |
| 5 | Test | 90%+ coverage, quality gates | YOLO only |
| 6 | Finalize | Commit, archive, pattern record | Manual |

## Intelligence Layer (Pre-Flight)
Analyzes description before starting workflow:

| Detection | Score | Action |
|-----------|-------|--------|
| Trivial (typo, alignment) | 0-10 | Redirect to `/ifix` |
| Too simple | 11-29 | Suggest `/ifix` (50% time savings) |
| Standard feature | 30-79 | Proceed with `/i.develop` |
| Architectural | 80-100 | Require design phase first |
| Existing spec | - | Skip to `/iplan` |

**Algorithm:** See `.claude/lib/algorithms/complexity-analysis.md`

## Swarm Auto-Activation
Triggers when:
- 3+ platforms detected, OR
- 15+ tasks identified

Time savings: 30-40% vs sequential
Override: `.swarm` or `.no-swarm`

## Quality Gates
| Metric | Requirement |
|--------|-------------|
| Test coverage | 90%+ |
| Blockers | 0 |
| Warnings | 0 |
| Function equivalence | 100% (refactoring) |

## TDD Auto-Detection

### TDD Score Calculation
Calculated during Phase 1 (Specify):

| Signal | Points |
|--------|--------|
| Business logic keywords | +30 |
| Critical path (auth, payment, data) | +15 |
| Multiple files (>3) | +10 |
| Complex logic | +10 |
| UI only | -20 |
| Config only | -15 |
| Prototype mode | -10 |

### TDD Thresholds
| Score | Action |
|-------|--------|
| < 50 | No TDD |
| 50-69 | Ask: "TDD Recommended?" |
| 70-89 | Ask: "TDD Strongly Recommended?" |
| >= 90 | Enforce TDD |

### TDD Development Flow
When TDD activated:
```
1. Write test for first requirement
2. Verify test fails (red)
3. Implement minimal code to pass
4. Verify test passes (green)
5. Refactor if needed
6. Repeat for next requirement
```

### TDD + YOLO Mode
When `.yolo` and TDD score >= 50:
- Auto-enable TDD
- Generate tests alongside implementation
- Run tests after each task
- Commit only if tests pass

## OOD Auto-Detection (Object-Oriented Development)

### OOD Score Calculation
Calculated during Phase 1 (Specify):

| Signal | Points |
|--------|--------|
| Domain logic keywords (`domain/`, `model/`, `entity/`) | +30 |
| Data modeling (User, Order, Product, etc.) | +25 |
| State management (lifecycle, workflow) | +20 |
| Relationships (aggregate, composition) | +20 |
| Validation rules (invariant, constraint) | +15 |
| Identity (id, uuid) | +10 |
| Simple CRUD (list, get, set) | -25 |
| Utility code (`utils/`, `helpers/`) | -20 |
| UI layer (view, screen, component) | -15 |

### OOD Thresholds
| Score | Action |
|-------|--------|
| < 40 | No OOD patterns |
| 40-59 | Suggest: "OOD patterns may improve this code" |
| 60-79 | Ask: "Apply OOD patterns? (Entity/Value Object/Service)" |
| >= 80 | Ask: "OOD Strongly Recommended. Which patterns?" |

### OOD Pattern Recommendations
| Context | Patterns |
|---------|----------|
| Data with identity | Entity |
| Immutable data values | Value Object |
| Complex object creation | Factory / Builder |
| Cross-entity operations | Domain Service |
| Data access abstraction | Repository |
| Object clusters | Aggregate |
| Business rules | Specification / Policy |

### OOD + TDD Combined
When both scores are high:
```
1. Define domain model (Entity, Value Object)
2. Write tests for domain behavior (TDD)
3. Implement domain logic
4. Add repository tests (TDD)
5. Implement persistence
6. Validate SOLID compliance
```

### OOD + YOLO Mode
When `.yolo` and OOD score >= 60:
- Auto-suggest patterns based on context
- Apply Entity/Value Object for data modeling
- Create Repository for persistence
- Validate SOLID after implementation

## Examples
| Command | Behavior |
|---------|----------|
| `/i.develop` | Interactive Q&A → workflow → manual approvals |
| `/i.develop "dark mode" .yolo` | Auto: analyze → Q&A → spec → plan → tasks → implement → test → commit |
| `/i.develop "OAuth" .cot` | Show reasoning at each step |
| `/i.develop "chat" .swarm` | Force swarm (even if <15 tasks) |
| `/i.develop "POC" .prototype` | Skip testing, minimal docs |
| `/i.develop "order management" .ood` | Force OOD patterns (Entity, Aggregate, Repository) |
| `/i.develop "user system" .ddd` | Full DDD analysis with domain modeling |
| `/i.develop "payment" .tdd .ood` | Both TDD and OOD enforced |

## Modes Comparison
| Mode | Approvals | Chaining | Testing | Use Case |
|------|-----------|----------|---------|----------|
| Interactive (default) | Yes | Optional | Full | Standard features |
| YOLO (`.yolo`) | No | Auto | Full | Simple features (<30 min) |
| Manual (`.stop`) | Yes | None | Full | Complex review needed |
| Prototype (`.prototype`) | Optional | Optional | Deferred | Experiments, POCs |

## MCP Integration
Uses: `ideacode_specify`, `ideacode_plan`, `ideacode_implement`, `ideacode_test`, `ideacode_commit`, `ideacode_archive`

## Q&A Questions (Phase 0)
| # | Question | Purpose |
|---|----------|---------|
| 1 | What are you building? | Feature description |
| 2 | Why (user problem)? | Requirements validation |
| 3 | Who (target users)? | User story context |
| 4 | Which project? | Platform detection |
| 5 | Any constraints? | Technical limitations |

## Related Commands
| Command | Purpose | When to Use |
|---------|---------|-------------|
| `/ispecify` | Spec only | Need spec without implementation |
| `/iplan` | Plan only | Have spec, need plan |
| `/iimplement` | Execute only | Have plan, ready to code |
| `/ifix` | Bug fixes | Small changes, bug fixes |
| `/iwiz` | Interactive + tutor | Learning, complex features |

## Error Handling
On error at any stage:
1. Stop workflow
2. Report error with stack trace
3. Offer recovery: Retry / Skip / Abort / Debug with `/ifix`
4. Use `ideacode_checkpoint` for analysis

## Documentation
- Full protocol: `Protocol-Swarm-Coordination-v1.0.md`
- Code review: `Protocol-Code-Review-v1.0.md`
- Tech stack workflow: `docs/ideacode/specs/006-intelligent-tech-stack-workflow/spec.md`

## Metadata
- **Command:** `/i.develop`
- **Version:** 10.2
- **Intelligence:** YES (complexity, spec reuse, urgency, TDD, OOD)
- **Swarm:** Auto-activation
- **TDD:** Auto-detection
- **OOD:** Auto-detection
