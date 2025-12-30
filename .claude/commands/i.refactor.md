---
description: Guided refactoring .tdd .ood .ddd .solid .tcr | /i.refactor .ood "UserService"
---

<help>

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3847/i.refactor`
Auto-start: API server starts automatically if not running

---


  /refactor - Guided Code Refactoring Workflow       


 PURPOSE
Systematic code refactoring with test-first approach, incremental changes,
and verification at each step. No behavior changes, only structure improvements.

 USAGE
  /refactor [description] [flags]

 ARGUMENTS
  description    What to refactor (optional, will ask if not provided)
                Example: "Simplify UserService class"

 FLAGS & MODIFIERS
  .yolo       Full automation (refactor→test→commit)
  .tdd        Force TDD (default for refactor - tests before AND after)
  .skip-tdd   Skip TDD requirement (NOT recommended for refactoring)
  .ood        Force OOD pattern analysis and application
  .skip-ood   Skip OOD recommendations
  .ddd        Full DDD refactoring (Entity, Aggregate, Repository)
  .solid      Strict SOLID compliance enforcement
  .cot        Show Chain of Thought reasoning
  .tot        Tree of Thoughts (explore multiple refactoring approaches)

 EXAMPLES
  Basic usage:
    /refactor
    → Asks: What to refactor? Why? Project?
    → Guided step-by-step refactoring

  With description:
    /i.refactor "Extract UserService methods into smaller classes"
    → Starts directly with refactoring

  With reasoning:
    /i.refactor "Simplify auth logic" .tot
    → Explores 3-5 refactoring approaches
    → Recommends optimal approach

  With OOD patterns:
    /i.refactor .ood "OrderService"
    → Analyzes for Entity, Value Object, Aggregate patterns
    → Suggests domain model improvements

  Full DDD refactoring:
    /i.refactor .ddd "user management module"
    → Full domain model analysis
    → Entity/Aggregate/Repository extraction
    → SOLID compliance check

 WORKFLOW SEQUENCE
  1. Scope Definition - What to refactor, what NOT to touch
  2. OOD Analysis - Calculate OOD score, identify pattern opportunities
  3. Test First - 100% coverage BEFORE refactoring (TDD MANDATORY)
  4. Incremental Refactor - Small steps, tests after each
  5. SOLID Validation - Check compliance after each step
  6. Update Spec - MODIFIED internal only
  7. Archive - Record refactoring pattern

 TDD ENFORCEMENT (MANDATORY FOR REFACTORING)
  Refactoring ALWAYS uses TDD because:
  - Must prove behavior preservation
  - Tests document expected behavior
  - Catches regressions immediately

  TDD Refactor Flow:
  1. Run existing tests (must pass)
  2. Add tests for any uncovered code
  3. Make ONE small refactoring change
  4. Run tests (must still pass)
  5. Commit if green
  6. Repeat steps 3-5

  If tests fail after refactor → REVERT immediately

 OOD ANALYSIS (AUTO-DETECTION)
  OOD score calculated at start of refactoring:

  | Score | Level | Action |
  |-------|-------|--------|
  | < 40 | None | Standard refactoring |
  | 40-59 | Suggested | "OOD patterns may improve structure" |
  | 60-79 | Recommended | "Apply OOD? (Entity/Value Object/Service)" |
  | >= 80 | Strongly Rec | "OOD Strongly Recommended. Which patterns?" |

  OOD Pattern Detection:
  | Code Smell | OOD Pattern |
  |------------|-------------|
  | God class | Entity + Service split |
  | Data clump | Value Object |
  | Feature envy | Move to appropriate class |
  | Primitive obsession | Value Object |
  | Shotgun surgery | Aggregate |

 SOLID VALIDATION
  After each refactoring step, check:
  - Single Responsibility: Class has one reason to change
  - Open/Closed: Extended not modified
  - Liskov Substitution: Subtypes substitutable
  - Interface Segregation: No fat interfaces
  - Dependency Inversion: Depend on abstractions

 RELATED COMMANDS
  /fix        Bug fixes
  /develop    New features
  /analyze    Code analysis

 FULL DOCUMENTATION
  Protocol-Code-Review-v1.0.md (incremental verification)

 TIPS
  • Always verify tests pass BEFORE refactoring
  • Make smallest possible changes
  • Commit after each successful refactoring step
  • Revert immediately if tests fail
  • Use .ood to identify structural improvements
  • Apply OOD patterns incrementally (one at a time)

  COMMON PITFALLS
  • Don't combine refactoring with feature changes
  • Don't skip tests between steps
  • Don't make large sweeping changes at once
  • Don't skip OOD analysis for domain code
  • Don't ignore SOLID violations
</help>

<!-- Help Detection .>
{IF user_input contains "?" OR user_input contains "help" OR user_input contains "--help"}
  {DISPLAY content from <help> section above}
  {EXIT}
{END IF}

---

Start guided development workflow using the **REFACTOR template**.

Use the `ideacode_guided_develop` MCP tool with mode="refactor".

This template is optimized for:
- Code improvement without behavior change
- Test-first refactoring (100% coverage before)
- Incremental changes
- Verification after each step

Ask the user:
1. What code needs refactoring?
2. Why (what improvement)?
3. Project name
4. Enable AI tutor? (default: yes)

Then call: `ideacode_guided_develop` with:
- mode: "refactor"
- feature_description: (refactor description from user)
- project: (from user)
- tutor_enabled: true

The workflow includes:
1. Scope Definition (what to refactor, what NOT to touch)
2. Test First (100% coverage BEFORE refactoring)
3. Incremental Refactor (small steps, tests after each)
4. Update Spec (MODIFIED internal only)
5. Archive

CRITICAL: Tests must pass after EVERY small change. If tests fail, revert immediately.

Show each step's instructions and checkpoint results to the user.
