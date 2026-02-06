# Developer Manual - Chapter 85: AVU Runtime Interpreter

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active

---

## Overview

Layer 3 of the AVU three-layer architecture. The AVU Runtime is a tree-walking interpreter that takes an AST (from `AvuDslParser`) and executes it inside a configurable sandbox. It dispatches wire protocol code invocations through the `IAvuDispatcher` interface, connecting DSL scripts to actual system actions.

**Package:** `com.augmentalis.voiceoscore.dsl.interpreter`

---

## Pipeline

```
Source (.vos/.avp)
    |
    v
[AvuDslLexer]     -->  List<Token>     (Layer 2: lexer/)
    |
    v
[AvuDslParser]    -->  AvuDslFile      (Layer 2: parser/)
    |
    v
[AvuInterpreter]  -->  Execution       (Layer 3: interpreter/)
    |
    v
[IAvuDispatcher]  -->  System actions  (Platform-specific)
```

---

## Lexer Summary (AvuDslLexer)

**Package:** `com.augmentalis.voiceoscore.dsl.lexer`

- Two-phase scanning: header emits `HEADER_LINE` tokens, body fully tokenized
- Python-style `INDENT`/`DEDENT` via indent stack (tabs = 4 spaces)
- 47 token types across 8 categories (structure, literals, identifiers, directives, delimiters, operators, keywords, special)
- `CODE_NAME` detection: 3-letter all-uppercase tokens classified at lex time

```kotlin
val tokens = AvuDslLexer(source).tokenize()
```

---

## Parser Summary (AvuDslParser)

**Package:** `com.augmentalis.voiceoscore.dsl.parser`

- Recursive descent with 8-level expression precedence climbing
- Header parsing delegates to `AvuHeader.parse()` from AVUCodec
- Block parsing via `INDENT`/`DEDENT` tokens
- Collect-all error recovery, synchronizes at `@workflow`/`@define`/`@on`

```kotlin
val result = AvuDslParser(tokens).parse()
if (result.hasErrors) { /* report */ }
val ast = result.file
```

---

## AST Node Hierarchy

| Category | Types | Count |
|----------|-------|-------|
| Declarations | `Workflow`, `FunctionDef`, `TriggerHandler` | 3 |
| Statements | `CodeInvocation`, `FunctionCall`, `WaitDelay`, `WaitCondition`, `IfElse`, `Repeat`, `While`, `Sequence`, `Assignment`, `Log`, `Return`, `Emit` | 12 |
| Expressions | `StringLiteral`, `IntLiteral`, `FloatLiteral`, `BooleanLiteral`, `VariableRef`, `Identifier`, `BinaryOp`, `UnaryOp`, `MemberAccess`, `CallExpression`, `Grouped` | 11 |

All nodes carry `SourceLocation(line, column, length)` for error reporting.

---

## IAvuDispatcher Interface (Future Phase 2)

The dispatcher abstracts platform-specific code execution:

```kotlin
interface IAvuDispatcher {
    suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult
    fun canDispatch(code: String): Boolean
}

sealed class DispatchResult {
    data class Success(val data: Any? = null) : DispatchResult()
    data class Error(val message: String, val code: String? = null) : DispatchResult()
    data class Timeout(val code: String) : DispatchResult()
}
```

**Design Rationale:**
- **Testability:** Mock dispatchers for unit tests without device
- **Platform abstraction:** Single interpreter across Android, iOS, Web, Desktop
- **Async support:** `suspend` functions for long-running operations
- **Capability detection:** `canDispatch()` enables dispatcher chaining

---

## AvuInterpreter (Future Phase 2)

```kotlin
class AvuInterpreter(
    private val dispatcher: IAvuDispatcher,
    private val sandbox: SandboxConfig = SandboxConfig()
) {
    suspend fun execute(file: AvuDslFile): ExecutionResult
    suspend fun executeWorkflow(workflow: AvuAstNode.Declaration.Workflow): ExecutionResult
    suspend fun handleTrigger(pattern: String, captures: Map<String, String>): ExecutionResult
}

sealed class ExecutionResult {
    data class Success(val value: Any?) : ExecutionResult()
    data class Error(val message: String, val location: SourceLocation?) : ExecutionResult()
    object NoHandler : ExecutionResult()
}
```

---

## Sandbox Configuration

```kotlin
data class SandboxConfig(
    val maxExecutionTimeMs: Long = 10_000,
    val maxSteps: Int = 1_000,
    val maxLoopIterations: Int = 100,
    val maxNestingDepth: Int = 10,
    val maxVariables: Int = 100
)
```

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxExecutionTimeMs` | 10,000 | Wall-clock timeout |
| `maxSteps` | 1,000 | Max interpreter steps (statement executions) |
| `maxLoopIterations` | 100 | Max iterations per `@repeat`/`@while` |
| `maxNestingDepth` | 10 | Max call stack depth (`@define` nesting) |
| `maxVariables` | 100 | Max variables in scope |

### Profiles

```kotlin
// Strict: untrusted user plugins
val strictSandbox = SandboxConfig(
    maxExecutionTimeMs = 5_000, maxSteps = 500,
    maxLoopIterations = 50, maxNestingDepth = 5, maxVariables = 50
)

// Relaxed: system workflows
val relaxedSandbox = SandboxConfig(
    maxExecutionTimeMs = 60_000, maxSteps = 10_000,
    maxLoopIterations = 1_000, maxNestingDepth = 20, maxVariables = 500
)
```

---

## Execution Model

### Tree-Walking Evaluation

The interpreter walks the AST using a visitor pattern:

1. **Declarations** define workflows, functions, and trigger handlers
2. **Statements** execute side effects (code invocations, assignments, control flow)
3. **Expressions** evaluate to values (literals, variables, operations)

### Variable Scope

Lexical scoping with scope stack:

- **Global scope:** Top-level variables
- **Workflow scope:** Created by `@workflow`
- **Function scope:** Created by `@define`
- Variable lookup searches innermost to outermost scope

```
@workflow "example"
  @set global = "outer"
  @define inner_func()
    @set local = "inner"
    @log $local       # "inner"
    @log $global      # "outer" (found in parent scope)
```

### Code Invocation Execution

1. Evaluate each `NamedArgument` expression to a value
2. Build `Map<String, Any?>` from name-value pairs
3. Call `dispatcher.dispatch(code, arguments)`
4. Check `DispatchResult` and handle errors

```
VCM(id: "cmd1", action: "SCROLL_DOWN", target: "chrome")
```

Translates to:

```kotlin
dispatcher.dispatch("VCM", mapOf(
    "id" to "cmd1",
    "action" to "SCROLL_DOWN",
    "target" to "chrome"
))
```

### Control Flow

| Construct | Behavior |
|-----------|----------|
| `@if`/`@else` | Evaluate condition to Boolean, execute branch |
| `@repeat N` | Execute body N times (capped by `maxLoopIterations`) |
| `@while cond` | Execute body while condition is true (capped) |
| `@wait N` | Suspend for N milliseconds |
| `@wait cond timeout N` | Poll condition every 100ms, throw on timeout |
| `@return [expr]` | Early exit via exception-based control flow |
| `@emit "event" [data]` | Fire event to registered listeners |
| `@sequence` | Execute statements sequentially (explicit grouping) |

---

## Error Handling

All runtime errors carry `SourceLocation` for precise reporting.

### Error Types

| Error | Cause |
|-------|-------|
| `SandboxViolation` | Step limit, timeout, nesting depth, variable count exceeded |
| `DispatchError` | Code invocation failed (unknown code, invalid args, platform error) |
| `TypeError` | Wrong operand types (non-Boolean condition, non-integer loop count) |
| `UndefinedVariable` | Reference to `$variable` that was never assigned |
| `TimeoutError` | `@wait condition` exceeded its timeout |

### Error Format

```
TypeError: Cannot apply '+' to String and Boolean
  at line 23, column 15 in workflow "calculate"
```

---

## Platform Dispatchers (Future Phase 2)

| Platform | Dispatcher | Mechanism |
|----------|-----------|-----------|
| Android | `AndroidAvuDispatcher` | Accessibility Service, Intent dispatch |
| iOS | `iOSAvuDispatcher` | UIAccessibility, URL scheme dispatch |
| Desktop | `DesktopAvuDispatcher` | Robot API, window management |
| Web | `WebAvuDispatcher` | DOM manipulation, browser extension APIs |

### Android Example

```kotlin
class AndroidAvuDispatcher(
    private val accessibilityService: AccessibilityService
) : IAvuDispatcher {
    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        return when (code) {
            "AAC" -> handleAccessibilityAction(arguments)
            "VCM" -> handleVoiceCommand(arguments)
            "SCR" -> handleScreenRead(arguments)
            else -> DispatchResult.Error("Unsupported code: $code")
        }
    }

    override fun canDispatch(code: String): Boolean =
        code in setOf("AAC", "VCM", "SCR", "APP", "SYS")
}
```

---

## Best Practices

1. **Validate AST before execution** - Always check `ParseResult.hasErrors` before passing to interpreter
2. **Configure sandbox per use case** - Strict for untrusted plugins, relaxed for system workflows
3. **Use IAvuDispatcher for all system interactions** - Testable via mock, platform-agnostic
4. **Log execution steps** - Essential for debugging plugin issues
5. **Handle DispatchResult.Error gracefully** - Log, provide fallback, or surface to user

---

## Related Documents

- [Ch81: AVU Protocol Overview](Developer-Manual-Chapter81-AVU-Protocol-Overview.md)
- [Ch82: AVU Wire Protocol](Developer-Manual-Chapter82-AVU-Wire-Protocol.md)
- [Ch83: AVU DSL Syntax](Developer-Manual-Chapter83-AVU-DSL-Syntax.md)
- [Ch84: AVU Code Registry](Developer-Manual-Chapter84-AVU-Code-Registry.md)
- [Ch86: AVU Plugin System](Developer-Manual-Chapter86-AVU-Plugin-System.md)
- [Ch68: Workflow Engine Architecture](Developer-Manual-Chapter68-Workflow-Engine-Architecture.md) (SUPERSEDED)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-06 | Initial chapter |
