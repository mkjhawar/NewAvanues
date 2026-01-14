---
description: Dot modifier reference | /imodifiers
---

# /imodifiers - Modifier Reference

## Syntax
`.modifier` (dot prefix, stackable, position-free)

**Position-free:** Modifiers work anywhere in command:
```
/ifix .yolo "bug"     ✓
/ifix "bug" .yolo     ✓
.yolo /ifix "bug"     ✓
```

## Behavior Modifiers
| Modifier | Effect |
|----------|--------|
| `.yolo` | Full automation (safeguards enforced) |
| `.swarm` | Multi-agent parallel execution |
| `.stop` | Disable workflow chaining |
| `.tcr` | Test && Commit \|\| Revert |
| `.tdd` | Force TDD mode (write tests first) |
| `.skip-tdd` | Skip TDD (requires reason if score >= 90) |
| `.tutor` | AI explanations (learning mode) |
| `.verbose` | Detailed output |
| `.profile <name>` | Use specific profile (default, strict, prototype, production) |

## Profile Modifiers
| Modifier | TDD | OOD | Auto-Verify | Visual |
|----------|-----|-----|-------------|--------|
| `.profile default` | 50 | 40 | Yes | No |
| `.profile strict` | 30 | 30 | Yes | Yes |
| `.profile prototype` | 90 | 80 | No | No |
| `.profile production` | 40 | 40 | Yes | Yes |

## TDD Modifiers (Intelligent Test-Driven Development)
| Modifier | Effect |
|----------|--------|
| `.tdd` | Force TDD regardless of score |
| `.skip-tdd` | Skip TDD (reason required if score >= 90) |
| `.tdd-strict` | TDD + mutation testing + 100% coverage |

### TDD Auto-Detection
TDD is automatically recommended based on task scoring:

| Score | Level | Action |
|-------|-------|--------|
| < 50 | None | Proceed without TDD |
| 50-69 | Recommended | Ask user |
| 70-89 | Strongly Recommended | Ask user |
| >= 90 | Required | Enforce TDD |

**Positive Signals:** Business logic (+30), Bug fix (+25), Refactoring (+20), Critical path (+15), Multiple files (+10), Complex logic (+10), Data integrity (+10), Concurrency (+10)

**Negative Signals:** Trivial change (-30), UI only (-20), Config files (-15), Documentation (-15), Prototype mode (-10)

See: `LD-IDEACODE-TDD-Scoring-V1.md` for full algorithm

## OOD Modifiers (Intelligent Object-Oriented Development)
| Modifier | Effect |
|----------|--------|
| `.ood` | Force OOD pattern analysis |
| `.skip-ood` | Skip OOD recommendations |
| `.ddd` | Full DDD analysis (Entity, Aggregate, Repository, Service) |
| `.solid` | Enforce SOLID principles check |

### OOD Auto-Detection
OOD is automatically recommended based on context scoring:

| Score | Level | Action |
|-------|-------|--------|
| < 40 | None | Proceed without OOD patterns |
| 40-59 | Suggested | "OOD patterns may improve this code" |
| 60-79 | Recommended | Ask: "Apply OOD patterns?" |
| >= 80 | Strongly Recommended | Ask with pattern suggestions |

**Positive Signals:** Domain logic (+30), Data modeling (+25), State management (+20), Relationships (+20), Validation rules (+15), Identity (+10), Persistence (+10)

**Negative Signals:** Simple CRUD (-25), Utility code (-20), Config files (-15), UI layer (-15), Scripts (-10)

### OOD Pattern Recommendations
| Context | Pattern |
|---------|---------|
| Data with identity | Entity |
| Immutable values | Value Object |
| Complex creation | Factory / Builder |
| Cross-entity ops | Domain Service |
| Data access | Repository |
| Object clusters | Aggregate |
| Business rules | Specification |

See: `LD-IDEACODE-OOD-Scoring-V1.md` for full algorithm

## Reasoning Modifiers
| Modifier | Effect | Visible |
|----------|--------|---------|
| `.cot` | Chain of Thought | No |
| `.scot` | Show Chain of Thought | Yes |
| `.tot` | Tree of Thought | No |
| `.stot` | Show Tree of Thought | Yes |
| `.rot` | Reflection on Thinking | No |
| `.srot` | Show Reflection | Yes |

### Claude Code Thinking Levels (v2.0.74+)
| Modifier | Claude Code Equivalent | Budget Level |
|----------|------------------------|--------------|
| `.think` | "think" | Low |
| `.think-hard` | "think hard" / "think harder" | Medium-High |
| `.ultrathink` | "ultrathink" | Maximum |

**Usage:** Add to any command for scaled reasoning:
```
/ifix .ultrathink "complex race condition"
/iplan .think-hard "new architecture"
```

## Persistence Modifiers
| Modifier | Effect |
|----------|--------|
| `.persist` | Save context to continuity files |
| `.resume` | Load previous session |
| `.checkpoint` | Create restore point |

## Scope Modifiers
| Modifier | Focus |
|----------|-------|
| `.code` | Code analysis |
| `.ui` | UI analysis |
| `.docs` | Documentation |
| `.api` | API documentation |
| `.dev` | Developer docs |
| `.user` | User docs |
| `.app` | Full app |
| `.from-mockup` | Generate from design |

## Workflow Modifiers
| Modifier | Effect |
|----------|--------|
| `.clarify` | Add clarification questions |
| `.checklist` | Generate feature checklist |
| `.tasks` | Generate task list |
| `.implement` | Chain to implementation |
| `.progress` | Show progress tracking |
| `.debug` | Enable debug logging |
| `.phase N` | Execute specific phase |
| `.investigate` | Investigation mode (bug analysis) |
| `.fix` | Proceed to fix after investigation |
| `.report` | Investigation report only (never fix) |
| `.dry-run` | Preview tasks without executing |
| `.no-verify` | Skip auto-verification |
| `.visual` | Enable visual verification (screenshots) |

## Capture Modifiers
| Modifier | Destination |
|----------|-------------|
| `.global` | `design-standards/` |
| `.project` | `.ideacode/design-standards/` |
| `.test` | `tests/` |
| `.security` | `design-standards/security/` |
| `.backlog` | `backlog/` |

## Examples by Use Case

### Bug Fixing
```
/ifix "bug"                         # Interactive (auto-investigate if unclear)
/ifix .yolo "simple bug"            # Auto-fix
/ifix .swarm "complex bug"          # Multi-agent
/ifix .tdd "auth bug"               # Force TDD (write test first)
/ifix .investigate "random crash"   # Investigate → ask to fix
/ifix .investigate .report "leak"   # Investigate only → report
/ifix .investigate .yolo "timeout"  # Investigate → auto-fix
```

### TDD Examples
```
/ifix "auth token race"        # Score 95 → TDD Required
/ifix "button color"           # Score -15 → No TDD
/ifix .tdd "UI alignment"      # Force TDD despite low score
/ifix .skip-tdd "hotfix" "auth race"  # Skip TDD with reason
```

### Development
```
/idevelop "feature"            # Interactive
/idevelop .yolo "feature"      # Full auto
/idevelop .tdd "feature"       # Test-driven
/idevelop .ood "domain logic"  # OOD patterns
/idevelop .ddd "order system"  # Full DDD
/idevelop .swarm "multi-plat"  # Parallel agents
```

### Refactoring with OOD
```
/irefactor "UserService"       # Standard refactor (TDD default)
/irefactor .ood "OrderService" # OOD pattern analysis
/irefactor .ddd "user module"  # Full DDD refactoring
/irefactor .solid "legacy"     # SOLID compliance focus
```

### Documentation
```
/idocument .all                # All docs
/idocument .api                # API only
/idocument .manuals            # User + dev manuals
```

### Analysis
```
/ianalyze .code                # Code focus
/ianalyze .ui                  # UI focus
/ianalyze .swarm .cot          # Multi-agent with reasoning
```

## YOLO Safeguards (Always Enforced)
| Rule | Requirement |
|------|-------------|
| No shortcuts | Proper implementation only |
| No disable without re-enable | Must restore |
| 100% function equivalence | Refactoring preserves behavior |
| All tests pass | No skips |
| No TODOs | Complete implementation |

## Modifier Compatibility Matrix

| Command | `.yolo` | `.swarm` | `.tdd` | `.ood` | `.ddd` | `.solid` | `.investigate` |
|---------|---------|----------|--------|--------|--------|----------|----------------|
| `/ispecify` | Yes | No | No | No | No | No | No |
| `/iplan` | Yes | No | No | No | No | No | No |
| `/iimplement` | Yes | Yes | Yes | Yes | No | Yes | No |
| `/idevelop` | Yes | Yes | Yes | Yes | Yes | Yes | No |
| `/ifix` | Yes | Yes | Yes | Yes | No | Yes | Yes |
| `/irefactor` | Yes | No | Default | Yes | Yes | Yes | No |
| `/ianalyze` | No | Yes | No | Yes | No | Yes | No |
| `/ireview` | No | Yes | No | Yes | No | Yes | No |
| `/iscan` | No | No | No | No | No | No | No |
| `/idocument` | Yes | Yes | No | No | No | No | No |

**Notes:**
- `/irefactor` has TDD enabled by default
- `.ddd` includes Entity, Aggregate, Repository, Service patterns
- `.solid` checks Single Responsibility, Open/Closed, Liskov, Interface Segregation, Dependency Inversion

## Validation Rules

When invalid modifier used, respond with:
```
Modifier .{name} not supported by /{command}

Supported: .yolo .cot .tasks
Alternative: Use /{alternative} instead
```

| Invalid Combination | Alternative |
|---------------------|-------------|
| `/ianalyze .yolo` | Use `/ianalyze` (no auto needed) |
| `/ispecify .swarm` | Use `/idevelop .swarm` |
| `/iscan .yolo` | Use `/iscan` (read-only) |
| `/ireview .tdd` | Use `/ifix .tdd` for fixes |
| `/iplan .tcr` | Use `/irefactor .tcr` |
| `/irefactor .skip-tdd` without reason | Provide reason: `.skip-tdd "reason"` |
| `/ifix .skip-tdd` (score >= 90) | Provide reason or use `.tdd` |
| `/ispecify .ddd` | Use `/idevelop .ddd` (DDD needs implementation) |
| `/iscan .ood` | Use `/ianalyze .ood` for OOD analysis |
| `/ifix .ddd` | Use `/irefactor .ddd` (DDD is refactoring) |

## Metadata
- **Command:** `/imodifiers`
- **Version:** 10.2
