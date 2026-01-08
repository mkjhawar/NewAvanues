---
description: Execute plan with task tracking .yolo .swarm .tdd .resume | /i.implement plan.md
---

# v14.0 - Autonomous Loop Support

**New Modifier:** `.autonomous` - Execute plan until completion with safety limits

```
/i.implement .autonomous plan.md      # Run until done (100 calls/hr, 30 min max)
/i.implement .autonomous .yolo plan.md  # Autonomous + auto-approve
```

API: `POST /v1/autonomous/start` → `POST /v1/autonomous/check` → auto-stop on completion

---

# /i.implement - Execute Implementation Plan

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.implement`
Auto-start: API server starts automatically if not running

---


## Usage
`/implement <plan_file> [flags]`

## Arguments
| Arg | Required | Description |
|-----|----------|-------------|
| plan_file | Yes | Path to plan file (or spec number to auto-find plan) |

## Flags
| Flag | Effect |
|------|--------|
| `.autonomous` | Run until plan complete (100 calls/hr, 30 min max, circuit breaker) |
| `.yolo` | Auto-progress, no approvals between tasks |
| `.tdd` | Force TDD for all tasks |
| `.skip-tdd` | Skip TDD recommendations |
| `.swarm` | Force parallel execution with agents |
| `.cot` | Show reasoning at each step |
| `.resume` | Resume from last incomplete task |
| `.phase` | Execute specific phase only (e.g., `.phase 2`) |
| `.dry-run` | Preview tasks without executing |

## Process
1. **Load Plan** - Parse plan file, extract phases and tasks
2. **Create Todo List** - Generate TodoWrite tasks for tracking
3. **Phase Execution** - Execute each phase in order
4. **Task Tracking** - Update todo status as tasks complete
5. **Quality Gates** - Run tests after each phase
6. **Finalize** - Commit, update progress

## Task Tracking (TodoWrite Integration)
| Event | TodoWrite Action |
|-------|------------------|
| Plan loaded | Create all tasks as `pending` |
| Task started | Mark task as `in_progress` |
| Task completed | Mark task as `completed` |
| Task blocked | Keep `in_progress`, add blocker task |
| Phase complete | All phase tasks `completed` |

## Plan File Detection
| Input | Resolution |
|-------|------------|
| `plan.md` | Use directly |
| `007` | Find `docs/ideacode/specs/007-*/plan.md` |
| `auth` | Find `docs/ideacode/specs/*auth*/plan.md` |
| (none) | Find most recent plan in current spec folder |

## Phase Execution Order
| Priority | Phase | Dependency |
|----------|-------|------------|
| 1 | Backend | None |
| 2 | Shared/KMP | Backend complete |
| 3 | Android | Shared complete |
| 4 | iOS | Shared complete |
| 5 | Web | Backend complete |

## IDE Loop (Per Task)
| Step | Action |
|------|--------|
| 1. TDD Check | Calculate TDD score for task |
| 2. Test First | If TDD, write failing test |
| 3. Implement | Write code for task |
| 4. Defend | Run linter, type-check |
| 5. Evaluate | Run relevant tests |
| 6. Commit | Atomic commit if passing |

## TDD Per-Task Scoring

Each task is scored individually:

| Task Type | TDD Score | Action |
|-----------|-----------|--------|
| Business logic | 70+ | TDD Recommended |
| Data/API | 60+ | TDD Recommended |
| UI component | 20-40 | No TDD |
| Config change | 0-20 | No TDD |

### TDD Task Flow
```
For each task:
1. Calculate TDD score
2. If score >= 50:
   → ASK: "TDD for this task? (Y/n)"
3. If TDD:
   a. Write test defining expected behavior
   b. Verify test fails
   c. Implement task
   d. Verify test passes
4. Run full test suite
5. Commit if green
```

### Per-Task TDD Display
```
Task 3/12: Implement payment validation

TDD Score: 75 (Business logic + Critical path)
→ TDD Recommended. Write test first? (Y/n): Y

Writing test: test_payment_validation_rejects_expired_card()
```

## Swarm Mode
When `.swarm` or auto-activated (3+ platforms, 15+ tasks):

| Agent | Responsibility |
|-------|---------------|
| Backend Agent | API, database, auth |
| Android Agent | Compose UI, Android-specific |
| iOS Agent | SwiftUI, iOS-specific |
| Web Agent | React/Next.js, web-specific |
| Test Agent | Coverage, quality gates |
| Architect | Cross-cutting concerns |

## Resume Mode
When `.resume`:
1. Load TodoWrite state
2. Find first `in_progress` or `pending` task
3. Continue from that point
4. Skip `completed` tasks

## Examples
| Command | Result |
|---------|--------|
| `/implement plan.md` | Execute plan with task tracking |
| `/implement 007` | Find and execute spec 007's plan |
| `/implement plan.md .yolo` | Auto-execute all tasks |
| `/implement plan.md .phase 2` | Execute phase 2 only |
| `/implement plan.md .resume` | Resume from last task |
| `/implement plan.md .dry-run` | Preview tasks only |
| `/implement plan.md .swarm` | Parallel execution |

## Output Format
```
## Implementation Progress

### Phase 1: Backend
- [x] Task 1.1: Create API endpoints
- [x] Task 1.2: Add database schema
- [ ] Task 1.3: Implement auth (in_progress)

### Phase 2: Android
- [ ] Task 2.1: Create UI components (pending)
- [ ] Task 2.2: Add ViewModel (pending)

Progress: 2/5 tasks (40%)
```

## Quality Gates
| Gate | Requirement |
|------|-------------|
| Tests | 90%+ coverage |
| Lint | 0 errors |
| Types | 0 errors |
| Build | Success |

## Workflow Questions (Interactive Mode)

After implementation completes, ask sequentially:

| # | Question | Yes Action | No Action |
|---|----------|------------|-----------|
| 1 | "Run tests?" | Execute test suite | Skip tests |
| 2 | "Commit changes?" | Create commit | Leave uncommitted |

### Question Flow
```
Implementation complete.

→ Run tests? (Y/n): Y
  All tests passed (92% coverage).

→ Commit changes? (Y/n): Y
  Committed: feat: implement user authentication
```

### YOLO Mode Behavior
When `.yolo` is set:
- Skip all questions
- Auto-run tests
- Auto-commit if tests pass

## Error Handling
| Scenario | Action |
|----------|--------|
| Task fails | Stop, report error, offer: Retry / Skip / Debug |
| Test fails | Keep task `in_progress`, show failure |
| Build fails | Rollback last change, offer fix |

## MCP Integration
Uses: `ideacode_implement`, `ideacode_test`, `ideacode_commit`

## Related Commands
| Command | Purpose |
|---------|---------|
| `/ispecify` | Create spec first |
| `/iplan` | Create plan from spec |
| `/idevelop` | Full workflow (includes implement) |
| `/ifix` | Debug implementation issues |

## Metadata
- **Command:** `/i.implement`
- **Alias:** `/i.implement`
- **Version:** 10.2
- **TodoWrite:** YES (task tracking)
- **Swarm:** Auto-activation
