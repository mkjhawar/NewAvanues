# Workflow Chaining Engine
# Shared workflow continuation logic for all IDEACODE workflow commands
# Version: 8.5

## Workflow Chain Definition

```
specify → plan → tasks → implement → test → commit → archive
```

## Mode Detection

```
{IF arguments contain "--yolo"}
  SET mode = "yolo"
{ELSE IF arguments contain "--stop" OR "--manual"}
  SET mode = "manual"
{ELSE}
  SET mode = "interactive"  // Safe default
{END IF}
```

## Workflow Continuation Templates

### After /specify

```
{IF mode === "yolo"}
  ✅ Specification complete!
  ⚡ YOLO mode: Auto-proceeding to /plan...

  {automatically invoke: /plan {spec_file} --yolo}

{ELSE IF mode === "interactive"}
  ✅ Specification complete!

  ┌─────────────────────────────────────────────┐
  │ 🎯 Next Step: Create Implementation Plan   │
  │                                             │
  │ Would you like to proceed to:              │
  │   1. /plan (recommended)          │
  │   2. /implement (skip planning)   │
  │   3. Stop here (manual control)            │
  │                                             │
  │ 💡 Recommendation: Use plan for complex    │
  │    features (>5 subtasks, multi-domain)    │
  └─────────────────────────────────────────────┘

  {use AskUserQuestion tool with 3 options above}

  {IF user chooses option 1}
    Now proceeding to /plan...
    {invoke: /plan {spec_file}}
  {ELSE IF user chooses option 2}
    Skipping planning, proceeding to /implement...
    {invoke: /implement {spec_file}}
  {ELSE}
    ✅ Specification saved!

    When ready to continue, run:
      /plan {spec_file}
  {END IF}

{ELSE} // manual mode
  ✅ Specification complete!
  📄 File: {spec_file}

  Next steps (run manually when ready):
    • /plan {spec_file}
    • /implement {spec_file}
    • /clarify {spec_file} (optional: identify unclear areas)
{END IF}
```

### After /plan

```
{IF mode === "yolo"}
  ✅ Plan complete!
  ⚡ YOLO mode: Auto-proceeding to /tasks...

  {automatically invoke: /tasks {plan_file} --yolo}

{ELSE IF mode === "interactive"}
  ✅ Plan complete!

  ┌─────────────────────────────────────────────┐
  │ 🎯 Next Step: Generate Task Breakdown      │
  │                                             │
  │ Would you like to proceed to:              │
  │   1. /tasks (generate tasks)      │
  │   2. /implement (start coding)    │
  │   3. Stop here                             │
  └─────────────────────────────────────────────┘

  {use AskUserQuestion tool}

  {IF user chooses option 1}
    {invoke: /tasks {plan_file}}
  {ELSE IF user chooses option 2}
    {invoke: /implement {plan_file}}
  {ELSE}
    ✅ Plan saved!
    When ready: /tasks {plan_file}
  {END IF}

{ELSE} // manual mode
  ✅ Plan complete!
  📄 File: {plan_file}

  Next: /tasks {plan_file}
{END IF}
```

### After /tasks

```
{IF mode === "yolo"}
  ✅ Tasks generated!
  ⚡ YOLO mode: Auto-proceeding to /implement...

  {automatically invoke: /implement {plan_file} --yolo}

{ELSE IF mode === "interactive"}
  ✅ Tasks generated!

  ┌─────────────────────────────────────────────┐
  │ 🎯 Next Step: Start Implementation         │
  │                                             │
  │ Would you like to:                         │
  │   1. /implement (start coding)    │
  │   2. Review tasks manually first           │
  └─────────────────────────────────────────────┘

  {use AskUserQuestion tool}

  {IF user chooses option 1}
    {invoke: /implement {plan_file}}
  {ELSE}
    ✅ Tasks saved!
    When ready: /implement {plan_file}
  {END IF}

{ELSE} // manual mode
  ✅ Tasks generated!
  📄 File: {tasks_file}

  Next: /implement {plan_file}
{END IF}
```

### After /implement

```
{IF mode === "yolo"}
  ✅ Implementation complete!
  ⚡ YOLO mode: Auto-running tests...

  {run test command from config or detect test framework}

  {IF tests pass}
    ✅ All tests passed!
    ⚡ YOLO mode: Creating commit...
    {create git commit with proper message}
    ✅ Committed successfully!

    🎉 Full workflow complete in YOLO mode!
       Duration: {duration}
       Next: /archive when feature is production-ready
  {ELSE}
    ❌ Tests failed: {failure_count} failures
    ⚠️  YOLO mode paused due to test failures

    {display test failures}

    Would you like to:
      1. Auto-fix failures (recommended)
      2. Review failures manually
      3. Skip tests and commit anyway (not recommended)
  {END IF}

{ELSE IF mode === "interactive"}
  ✅ Implementation complete!

  ┌─────────────────────────────────────────────┐
  │ 🎯 Next Step: Quality Assurance             │
  │                                             │
  │ Would you like to:                         │
  │   1. Run tests                             │
  │   2. Code review (IDE Loop v8.4)           │
  │   3. Create commit without testing         │
  │   4. Stop here                             │
  │                                             │
  │ ⚠️  Recommendation: Always test before     │
  │    committing                              │
  └─────────────────────────────────────────────┘

  {use AskUserQuestion tool}

{ELSE} // manual mode
  ✅ Implementation complete!

  Next steps (manual):
    • Test your implementation
    • Create git commit
    • Run /archive when production-ready
{END IF}
```

## Context Preservation

When chaining to next command, preserve these values:
- `spec_file`: Path to specification file
- `plan_file`: Path to plan file
- `tasks_file`: Path to tasks file
- `feature_name`: Feature name (for commits, messages)
- `mode`: Current mode (yolo, interactive, manual)
- `profile`: Project profile (android-app, backend-api, etc.)

## Error Handling

### Test Failures in YOLO Mode
```
{IF tests fail AND mode === "yolo"}
  ⚠️  YOLO mode paused

  Options:
    1. Auto-fix (run /fix with test output)
    2. Manual review
    3. Abort workflow

  {DO NOT auto-commit if tests fail}
{END IF}
```

### Code Review Blockers in YOLO Mode
```
{IF blockers found AND mode === "yolo"}
  ⚠️  YOLO mode paused

  {display blockers}

  Options:
    1. Auto-fix blockers
    2. Manual review
    3. Abort workflow
{END IF}
```

## Usage in Commands

To use this workflow engine in a command:

```markdown
<!-- At end of command, after main logic completes -->

<!-- Include workflow continuation -->
{INCLUDE _workflow-engine.md logic for this command}
```

Example for /specify:
```markdown
<!-- After spec.md is created successfully -->

{SET spec_file = ".ideacode/specs/004-feature-name/spec.md"}
{DETECT mode from arguments}
{EXECUTE "After /specify" template from _workflow-engine.md}
```

---

**Version:** 8.5
**Last Updated:** 2025-11-23
