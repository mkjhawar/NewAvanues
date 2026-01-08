---
description: Implementation plan .yolo .cot .tot .tasks .implement | /i.plan spec.md
---

# /i.plan - Create Implementation Plan

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.plan`
Auto-start: API server starts automatically if not running

---


## Usage
`/plan <spec_file> [flags]`

## Arguments
| Arg | Required | Description |
|-----|----------|-------------|
| spec_file | Yes | Path to specification file |

## Flags
| Flag | Effect |
|------|--------|
| `.yolo` | Auto-chain to tasks→implement→test→commit |
| `.tasks` | Generate detailed task list from plan |
| `.implement` | Auto-chain to `/iimplement` after plan creation |
| `.stop` | Disable chaining |
| `.cot` | Show reasoning (phase ordering, KMP logic) |
| `.tot` | Tree of Thought (explore approaches) |

## Process
1. Load spec from `docs/ideacode/specs/###-feature/spec.md`
2. Extract platforms (Android, iOS, Web, Backend)
3. Order phases: Backend → Shared/KMP → Android → iOS → Web
4. Calculate KMP benefits (if Android+iOS)
5. Assess swarm activation (3+ platforms OR 15+ tasks)
6. Generate time estimates (sequential vs parallel)
7. Write plan to `plan.md`
8. Offer chaining: `/itasks` or `/iimplement`

## Platform Phase Ordering
| Priority | Phase | Rationale |
|----------|-------|-----------|
| 1 | Backend | Foundation (API, DB) |
| 2 | Shared/KMP | Code reuse (if Android+iOS) |
| 3 | Android | Most users |
| 4 | iOS | Depends on shared |
| 5 | Web | UI layer |

## KMP Decision Logic
| Condition | Action |
|-----------|--------|
| Android only | No KMP |
| iOS only | No KMP |
| Android + iOS | KMP setup (Phase 1) |
| Benefit > 40% code reuse | Recommend KMP |
| Benefit < 40% | Optional KMP |

## Swarm Assessment
Recommends swarm if:
- 3+ platforms, OR
- Estimated 15+ tasks, OR
- Complex integration points

## Task Tracking (TodoWrite Integration)
When `.tasks` or `.implement` is used:

| Event | TodoWrite Action |
|-------|------------------|
| Plan created | Create all tasks as `pending` |
| Phase started | Mark first task as `in_progress` |
| Task completed | Mark as `completed`, start next |

**Benefits:**
- Modular progress tracking
- Resume from any point
- Visual progress in status line
- Task modification during execution

## Workflow Questions (Interactive Mode)

After plan generation, ask sequentially:

| # | Question | Yes Action | No Action |
|---|----------|------------|-----------|
| 1 | "Create tasks for tracking?" | Generate TodoWrite tasks | Continue without tasks |
| 2 | "Proceed to implement?" | Run `/iimplement` | Stop after plan |

### Question Flow
```
Plan created successfully.

→ Create tasks for tracking? (Y/n): Y
  Tasks created in TodoWrite.

→ Proceed to implement? (Y/n): Y
  Starting implementation...
```

### YOLO Mode Behavior
When `.yolo` is set:
- Skip all questions
- Auto-answer YES to all
- Flow: Plan → Tasks → Implement → Test → Commit

## Examples
| Command | Result |
|---------|--------|
| `/plan spec.md` | Plan → ask workflow questions |
| `/plan spec.md .tasks` | Plan → auto-create tasks → ask implement? |
| `/plan spec.md .implement` | Plan → tasks → auto-implement |
| `/plan spec.md .yolo` | Plan → tasks → implement → test → commit |
| `/plan spec.md .cot` | Show phase ordering reasoning |

## Output Format
```markdown
# Implementation Plan: {Feature}

## Overview
Platforms: {Android, iOS, Web, Backend}
Swarm Recommended: {Yes/No}
Estimated: {N} tasks, {M} hours

## Phases
### Phase 1: {Backend}
- Task 1: ...
- Task 2: ...

### Phase 2: {KMP Setup} (if applicable)
- Task 1: ...

## Time Estimates
Sequential: {X} hours
Parallel (Swarm): {Y} hours
Savings: {X-Y} hours ({percentage}%)
```

## MCP Integration
Uses: `ideacode_plan`

## Related Commands
| Command | Purpose |
|---------|---------|
| `/ispecify` | Create spec first |
| `/itasks` | Generate tasks from plan |
| `/iimplement` | Execute implementation |
| `/idevelop` | Full workflow (includes plan) |

## Documentation
- Spec: `docs/ideacode/specs/006-intelligent-tech-stack-workflow/spec.md`

## Metadata
- **Command:** `/i.plan`
- **Version:** 9.0
- **Platform-Aware:** YES
