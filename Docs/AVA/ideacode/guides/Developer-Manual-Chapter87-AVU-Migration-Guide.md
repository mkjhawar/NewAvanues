# Developer Manual - Chapter 87: AVU Migration Guide

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active
**Implementation**: Phase 5 Complete

---

## Overview

This chapter guides migration from the existing `MacroDSL.kt` (Kotlin builder-pattern DSL) to the new AVU DSL (`.vos`/`.avp` text format). The old system uses compiled Kotlin lambdas for conditions and actions; the new system uses declarative text files interpreted at runtime. This enables cross-platform execution, dynamic loading, and App Store compliance.

---

## Why Migrate?

| Aspect | MacroDSL.kt (Old) | AVU DSL (New) |
|--------|-------------------|---------------|
| Format | Compiled Kotlin code | Text files (.vos/.avp) |
| Platform | Android only (uses `java.util.UUID`) | KMP (all platforms) |
| Conditions | Kotlin lambdas `{ ctx -> ctx.screenContains("x") }` | Expression: `screen.contains("x")` |
| Distribution | Must be compiled into app | Text file, loadable at runtime |
| App Store | Potential DexClassLoader issues | Fully compliant (text = data) |
| Extensibility | Requires app rebuild | Load/unload without rebuild |
| Testing | Unit tests only | Parse test + mock dispatcher |
| Type Safety | Compile-time (Kotlin) | Runtime (validation + sandbox) |

---

## Concept Mapping

| MacroDSL.kt | AVU DSL | Notes |
|-------------|---------|-------|
| `macro("Name") { }` | `@workflow "Name"` | Top-level container |
| `description = "..."` | `metadata: name: ...` | In header section |
| `trigger = "phrase"` | `triggers: phrase` | In header section |
| `tags("a", "b")` | Header metadata | Use metadata section |
| `step { tap("btn") }` | `AAC(action: "CLICK", target: "btn")` | Wire protocol code |
| `step { typeText("field", "text") }` | `AAC(action: "SET_TEXT", target: "field", text: "text")` | Wire protocol code |
| `step { scroll("down") }` | `VCM(id: "s1", action: "SCROLL_DOWN")` | Wire protocol code |
| `step { openApp("pkg") }` | `VCM(id: "o1", action: "launch", target: "pkg")` | Wire protocol code |
| `step { custom(id, phrase, type, params) }` | `VCM(id: id, action: type, ...)` | Wire protocol code |
| `delay(1000)` | `@wait 1000` | Milliseconds |
| `conditional { condition {...} then {...} otherwise {...} }` | `@if ... @else ...` | Expression-based condition |
| `loop(5) { }` | `@repeat 5` | Fixed count |
| `loopWhile { condition = ...; maxIterations = 100 }` | `@while condition` | Sandbox enforces limits |
| `waitFor { condition = ...; timeout(5000) }` | `@wait condition timeout 5000` | Inline syntax |
| `variable("name", value)` | `@set name = value` | Scoped to declaration |
| `MacroContext.screenContains("text")` | `screen.contains("text")` | Built-in runtime object |
| `MacroContext.findNodeByText("text")` | `screen.findNode("text")` | Built-in runtime object |
| `MacroContext.isVisible("text")` | `screen.findNode("text") != null` | Null check |

---

## Migration: Step by Step

### Step 1: Create the Header

**Old (MacroDSL.kt):**
```kotlin
val loginMacro = macro("Login Workflow") {
    description = "Automated login process"
    trigger = "login"
    author = "VOS4 Team"
    tags("automation", "login")
```

**New (login.vos):**
```
---
schema: avu-2.2
version: 1.0.0
type: workflow
metadata:
  name: Login Workflow
  description: Automated login process
  author: VOS4 Team
codes:
  VCM: Voice Command (id:action:params)
  AAC: Accessibility Action (id:actionType:targetAvid:params)
triggers:
  login
---
```

---

### Step 2: Convert Steps to Code Invocations

**Old:**
```kotlin
step { tap("login_button") }
step { typeText("username_field", "john_doe") }
step { scroll("down") }
step { openApp("com.example.app") }
```

**New:**
```
AAC(action: "CLICK", target: "login_button")
AAC(action: "SET_TEXT", target: "username_field", text: "john_doe")
VCM(id: "s1", action: "SCROLL_DOWN")
VCM(id: "o1", action: "launch", target: "com.example.app")
```

**Quick Reference:**
- `tap(id)` -> `AAC(action: "CLICK", target: id)`
- `typeText(id, text)` -> `AAC(action: "SET_TEXT", target: id, text: text)`
- `scroll(dir)` -> `VCM(action: "SCROLL_" + dir.uppercase())`
- `openApp(pkg)` -> `VCM(action: "launch", target: pkg)`

---

### Step 3: Convert Conditionals

**Old:**
```kotlin
conditional {
    condition { screenContains("username") }
    then {
        step { typeText("username_field", "john_doe") }
        step { typeText("password_field", "secret123") }
    }
    otherwise {
        step { tap("back") }
    }
}
```

**New:**
```
@if screen.contains("username")
  AAC(action: "SET_TEXT", target: "username_field", text: "john_doe")
  AAC(action: "SET_TEXT", target: "password_field", text: "secret123")
@else
  AAC(action: "CLICK", target: "back")
```

---

### Step 4: Convert Loops

**Fixed Count:**

Old: `loop(5) { step { tap("next") }; delay(500) }`

New:
```
@repeat 5
  AAC(action: "CLICK", target: "next")
  @wait 500
```

**Conditional:**

Old:
```kotlin
loopWhile {
    condition = { !screenContains("Done") }
    maxIterations = 10
    body { step { scroll("down") }; delay(300) }
}
```

New:
```
@while not screen.contains("Done")
  VCM(action: "SCROLL_DOWN")
  @wait 300
```

Note: Sandbox enforces iteration limits automatically (default 100).

---

### Step 5: Convert WaitFor

**Old:**
```kotlin
waitFor {
    condition = { screenContains("Loading complete") }
    timeout(5000)
    checkInterval(200)
}
```

**New:**
```
@wait screen.contains("Loading complete") timeout 5000
```

Check interval is handled by the runtime (not exposed in DSL).

---

### Step 6: Convert Variables

**Old:**
```kotlin
variable("username", "john_doe")
variable("loginAttempts", 0)
step { typeText("field", getVariable("username") as String) }
setVariable("loginAttempts", (getVariable("loginAttempts") as Int) + 1)
```

**New:**
```
@set username = "john_doe"
@set loginAttempts = 0
AAC(action: "SET_TEXT", target: "field", text: $username)
@set loginAttempts = $loginAttempts + 1
```

---

## Complete Before/After Example

### Before (MacroDSL.kt)

```kotlin
val loginMacro = macro("Login Workflow") {
    description = "Automated login process"
    trigger = "login"
    author = "VOS4 Team"
    tags("automation", "login", "user-flow")

    step { openApp("com.example.app") }
    delay(1000)
    step { tap("login button") }
    delay(500)

    conditional {
        condition { screenContains("username") }
        then {
            step { typeText("username field", "john_doe") }
            step { typeText("password field", "password123") }
            step { tap("submit") }
        }
        otherwise {
            step { tap("back") }
        }
    }

    waitFor {
        condition { screenContains("Welcome") }
        timeout(5000)
    }
}
```

### After (login.vos)

```
---
schema: avu-2.2
version: 1.0.0
type: workflow
metadata:
  name: Login Workflow
  description: Automated login process
  author: VOS4 Team
codes:
  VCM: Voice Command (id:action:params)
  AAC: Accessibility Action (id:actionType:targetAvid:params)
triggers:
  login
---

@workflow "Login Workflow"
  VCM(id: "open", action: "launch", target: "com.example.app")
  @wait 1000
  AAC(action: "CLICK", target: "login_button")
  @wait 500

  @if screen.contains("username")
    AAC(action: "SET_TEXT", target: "username_field", text: "john_doe")
    AAC(action: "SET_TEXT", target: "password_field", text: "password123")
    AAC(action: "CLICK", target: "submit")
  @else
    AAC(action: "CLICK", target: "back")

  @wait screen.contains("Welcome") timeout 5000
```

---

## Key Differences

### 1. Conditions Are Expressions, Not Lambdas

- Old: `condition { ctx -> ctx.screenContains("x") && ctx.currentApp() == "pkg" }`
- New: `screen.contains("x") and context.currentApp == "pkg"`
- No arbitrary Kotlin code; limited to runtime-provided objects

### 2. No UUID Generation

- Old: `UUID.randomUUID().toString()` in MacroBuilder.build()
- New: String literal IDs in code invocations; runtime handles execution IDs

### 3. Indentation Replaces @DslMarker

- Old: Kotlin scoping via `@MacroDsl` marker and lambdas
- New: Indentation-based block structure (Python-style)

### 4. Wire Protocol Codes Replace CommandBuilder

- Old: `step { tap("btn") }` via `CommandBuilder`
- New: `AAC(action: "CLICK", target: "btn")` via 3-letter codes
- Direct mapping to VoiceOS wire protocol

### 5. $name Syntax for Variables

- Old: `variable("x", val)` / `getVariable("x")`
- New: `@set x = val` / `$x`

### 6. Error Handling via @if, Not try/catch

- Old: Kotlin try/catch around steps
- New: `@if` conditional checks before risky operations

---

## MacroDslMigrator (Implemented - Phase 5)

**Package:** `com.augmentalis.voiceoscore.dsl.migration`

An automated migration utility that converts macro definitions into AVU DSL `.vos` text. Rather than parsing Kotlin PSI (which would be platform-specific), the migrator works with a platform-agnostic `MigrationStep` intermediate representation. Android code maps `MacroStep` → `MigrationStep`, then `MacroDslMigrator` generates `.vos` text in `commonMain`.

### MigrationStep (IR)

Sealed class representing all migratable constructs:

```kotlin
sealed class MigrationStep {
    data class Action(val code: String, val arguments: Map<String, String>)
    data class Delay(val millis: Long)
    data class Conditional(val condition: String, val thenSteps: List<MigrationStep>, val elseSteps: List<MigrationStep>)
    data class Loop(val count: Int, val steps: List<MigrationStep>)
    data class LoopWhile(val condition: String, val steps: List<MigrationStep>, val maxIterations: Int)
    data class WaitFor(val condition: String, val timeoutMs: Long)
    data class Variable(val name: String, val value: String)
}

data class MigrationMacro(
    val name: String, val description: String, val trigger: String,
    val author: String, val tags: List<String>, val steps: List<MigrationStep>
)
```

### MacroDslMigrator

```kotlin
object MacroDslMigrator {
    fun migrate(macro: MigrationMacro): MigrationResult
    fun migrateMultiple(macros: List<MigrationMacro>): MigrationResult
}

sealed class MigrationResult {
    data class Success(val content: String, val codesUsed: Set<String>, val warnings: List<String>)
    data class Error(val message: String)
    fun contentOrNull(): String?
}
```

**Features:**
- Generates complete `.vos` files with proper header (schema, version, codes, triggers, metadata)
- Auto-detects codes used across all steps (including nested conditionals and loops)
- Creates `@define` functions and `@on` trigger handlers for voice-activated macros
- Batch migration of multiple macros into a single `.vos` file
- Emits warnings for macros without triggers

### Usage Example

```kotlin
val macro = MigrationMacro(
    name = "Login Flow",
    trigger = "login",
    description = "Automated login",
    steps = listOf(
        MigrationStep.Action("VCM", mapOf("action" to "launch", "target" to "com.example")),
        MigrationStep.Delay(1000),
        MigrationStep.Action("AAC", mapOf("action" to "CLICK", "target" to "login_btn")),
        MigrationStep.Conditional(
            condition = "screen.contains(\"username\")",
            thenSteps = listOf(
                MigrationStep.Action("AAC", mapOf("action" to "SET_TEXT", "target" to "user", "text" to "john"))
            )
        )
    )
)

val result = MacroDslMigrator.migrate(macro)
if (result is MigrationResult.Success) {
    File("login.vos").writeText(result.content)
}
```

### AvuV1Compat (Wire Protocol Backward Compatibility)

Parses raw v1 `CODE:field1:field2` wire protocol messages and converts them to structured dispatch arguments or AVU DSL text:

```kotlin
object AvuV1Compat {
    fun parseV1Message(message: String): V1ParseResult
    fun toDispatchArguments(message: V1Message): Map<String, Any?>
    fun toAvuDslText(message: V1Message): String
    fun isV1Format(text: String): Boolean
}
```

**Known field mappings** for VCM, AAC, SCR, CHT, TTS — unknown codes use positional names (`field_0`, `field_1`, ...).

### File Layout

```
dsl/migration/
├── MigrationStep.kt     (57 lines)  - IR sealed class + MigrationMacro
├── MacroDslMigrator.kt  (234 lines) - .vos text generation
└── AvuV1Compat.kt       (150 lines) - V1 wire protocol backward compat
```

Total: ~441 lines across 3 files.

---

## Superseded Documents

- **Ch67: Avanues Plugin Development** -> Superseded by Ch86: AVU Plugin System
- **Ch68: Workflow Engine Architecture** -> Superseded by Ch85: AVU Runtime Interpreter

---

## Best Practices

1. **Migrate workflows before plugins** - Simpler structure, fewer required header fields
2. **Test each converted workflow independently** - Parse, mock-dispatch, then real device
3. **Verify trigger patterns match** - Ensure `triggers:` section matches original `trigger = "..."` strings
4. **Use @log liberally during migration** - Trace execution flow to verify correctness
5. **Keep old code until verified** - Run both systems in parallel, toggle via feature flag

---

## Related Documents

- [Ch67: Avanues Plugin Development](Developer-Manual-Chapter67-Avanues-Plugin-Development.md) (SUPERSEDED)
- [Ch68: Workflow Engine Architecture](Developer-Manual-Chapter68-Workflow-Engine-Architecture.md) (SUPERSEDED)
- [Ch81: AVU Protocol Overview](Developer-Manual-Chapter81-AVU-Protocol-Overview.md)
- [Ch83: AVU DSL Syntax](Developer-Manual-Chapter83-AVU-DSL-Syntax.md)
- [Ch85: AVU Runtime Interpreter](Developer-Manual-Chapter85-AVU-Runtime-Interpreter.md)
- [Ch86: AVU Plugin System](Developer-Manual-Chapter86-AVU-Plugin-System.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-06 | Initial chapter |
| 2.0 | 2026-02-06 | Updated: MacroDslMigrator, MigrationStep, AvuV1Compat now implemented (Phase 5) |
