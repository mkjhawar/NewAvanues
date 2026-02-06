# Developer Manual - Chapter 85: AVU Runtime Interpreter

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active
**Implementation**: Phase 2 Complete

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
[AvuDslLexer]          -->  List<Token>     (Layer 2: lexer/)
    |
    v
[AvuDslParser]         -->  AvuDslFile      (Layer 2: parser/)
    |
    v
[AvuInterpreter]       -->  Execution       (Layer 3: interpreter/)
  |-- ExpressionEvaluator  (expressions + type coercion)
  |-- ExecutionContext      (scope stack + sandbox enforcement)
    |
    v
[IAvuDispatcher]       -->  System actions  (Platform-specific)
```

---

## File Layout

```
dsl/interpreter/
├── SandboxConfig.kt         (50 lines)  - Execution limit configuration
├── RuntimeError.kt           (85 lines)  - Sealed error hierarchy
├── IAvuDispatcher.kt         (75 lines)  - Dispatch interface + CompositeDispatcher
├── ExecutionContext.kt       (170 lines) - Scope stack, functions, events, sandbox
├── ExpressionEvaluator.kt   (230 lines) - Expression evaluation + type coercion
└── AvuInterpreter.kt        (415 lines) - Statement execution + public API
```

Total: ~1,025 lines across 6 files.

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

## IAvuDispatcher Interface

The dispatcher abstracts platform-specific code execution:

```kotlin
interface IAvuDispatcher {
    suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult
    fun canDispatch(code: String): Boolean
}

sealed class DispatchResult {
    data class Success(val data: Any? = null) : DispatchResult()
    data class Error(val message: String, val cause: Throwable? = null) : DispatchResult()
    data class Timeout(val timeoutMs: Long) : DispatchResult()
}
```

**Design Rationale:**
- **Testability:** Mock dispatchers for unit tests without device
- **Platform abstraction:** Single interpreter across Android, iOS, Web, Desktop
- **Async support:** `suspend` functions for long-running operations
- **Capability detection:** `canDispatch()` enables dispatcher chaining

### CompositeDispatcher

Chain-of-responsibility pattern for multi-platform or multi-module dispatch:

```kotlin
class CompositeDispatcher(private val dispatchers: List<IAvuDispatcher>) : IAvuDispatcher {
    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        for (dispatcher in dispatchers) {
            if (dispatcher.canDispatch(code)) return dispatcher.dispatch(code, arguments)
        }
        return DispatchResult.Error("No dispatcher found for code: $code")
    }
    override fun canDispatch(code: String): Boolean = dispatchers.any { it.canDispatch(code) }
}
```

---

## AvuInterpreter

**Public API:**

```kotlin
class AvuInterpreter(
    private val dispatcher: IAvuDispatcher,
    private val sandbox: SandboxConfig = SandboxConfig.DEFAULT
) {
    suspend fun execute(file: AvuDslFile): ExecutionResult
    suspend fun executeWorkflow(file: AvuDslFile, name: String): ExecutionResult
    suspend fun handleTrigger(file: AvuDslFile, pattern: String, captures: Map<String, String>): ExecutionResult
}
```

**Result type:**

```kotlin
sealed class ExecutionResult {
    data class Success(val returnValue: Any? = null, val executionTimeMs: Long = 0) : ExecutionResult()
    data class Failure(val error: RuntimeError, val executionTimeMs: Long = 0) : ExecutionResult()
    data object NoHandler : ExecutionResult()
}
```

### SOLID Architecture

The interpreter follows Single Responsibility Principle with two collaborating classes:

| Class | Responsibility |
|-------|---------------|
| `AvuInterpreter` | Statement execution, control flow, public API |
| `ExpressionEvaluator` | Expression evaluation, type coercion, built-in dispatch |

Both share `ExecutionContext` (scope stack + sandbox enforcement).

---

## ExpressionEvaluator

Evaluates all 11 expression types with JavaScript-like type coercion:

```kotlin
internal class ExpressionEvaluator(private val dispatcher: IAvuDispatcher) {
    suspend fun evaluate(expr: Expression, context: ExecutionContext): Any?
    fun toBooleanValue(value: Any?): Boolean
    fun toNumber(value: Any?, location: SourceLocation): Number
}
```

### Type Coercion Rules

| Operation | Behavior |
|-----------|----------|
| `+` (String on either side) | String concatenation |
| `+` (both numeric) | Addition (Int if both Int, else Double) |
| `-`, `*` | Numeric only (Int if both Int, else Double) |
| `/` | Numeric only, always Double |
| `==`, `!=` | Structural equality |
| `<`, `>`, `<=`, `>=` | Numeric only |
| `and`, `or` | Boolean coercion, non-short-circuit |

### Boolean Coercion

| Value | Boolean |
|-------|---------|
| `null`, `false`, `0`, `0.0`, `""` | `false` |
| Everything else | `true` |

### Built-In Method Dispatch

`screen.contains("text")` flows through:

1. `Identifier("screen")` evaluates to String `"screen"`
2. `MemberAccess` creates `BuiltInCallable("screen", "contains")`
3. `CallExpression` dispatches `QRY` code:
   ```kotlin
   dispatcher.dispatch("QRY", mapOf(
       "query" to "screen_contains",
       "target" to "screen",
       "method" to "contains",
       "args" to listOf("text")
   ))
   ```

---

## ExecutionContext

Manages runtime state for a single execution:

```kotlin
class ExecutionContext(private val sandbox: SandboxConfig) {
    // Variable scope stack (innermost-to-outermost lookup)
    fun pushScope() / fun popScope()
    fun getVariable(name: String): Any?
    fun setVariable(name: String, value: Any?, location: SourceLocation?)

    // Function registry (@define)
    fun registerFunction(funcDef: FunctionDef)
    fun getFunction(name: String): FunctionDef?

    // Event listeners (@emit)
    fun addEventListener(event: String, listener: (Any?) -> Unit)
    fun emitEvent(event: String, data: Any?)

    // Sandbox enforcement
    fun incrementStep(location: SourceLocation?)     // checks step + time limits
    fun enterNesting(location: SourceLocation?)       // checks depth limit
    fun exitNesting()
    fun checkLoopLimit(iterations: Int, location: SourceLocation?)
}
```

### Variable Scoping

Variables use a scope stack with update-in-place semantics:
- `setVariable` searches all scopes for existing variable; if found, updates in place
- If not found, creates in current (innermost) scope
- This enables functions to modify parent-scope variables

```
@workflow "example"
  @set global = "outer"
  @define inner_func()
    @set local = "inner"
    @log $local       # "inner"
    @log $global      # "outer" (found in parent scope)
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
) {
    companion object {
        val DEFAULT = SandboxConfig()
        val STRICT = SandboxConfig(5_000, 500, 50, 5, 50)
        val SYSTEM = SandboxConfig(60_000, 10_000, 1_000, 20, 500)
        val TESTING = SandboxConfig(60_000, 10_000, 1_000, 50, 500)
    }
}
```

| Profile | Use Case | Time | Steps | Loops |
|---------|----------|------|-------|-------|
| `DEFAULT` | General use | 10s | 1,000 | 100 |
| `STRICT` | Untrusted plugins | 5s | 500 | 50 |
| `SYSTEM` | System workflows | 60s | 10,000 | 1,000 |
| `TESTING` | Unit tests | 60s | 10,000 | 1,000 |

---

## Runtime Errors

Sealed class hierarchy, all carrying `SourceLocation`:

```kotlin
sealed class RuntimeError(message: String, val location: SourceLocation?) : Exception(message) {
    class SandboxViolation(violation: String, limit: Long, current: Long, loc: SourceLocation?)
    class DispatchError(code: String, reason: String, loc: SourceLocation?)
    class TypeError(expected: String, actual: String, operation: String, loc: SourceLocation?)
    class UndefinedVariable(name: String, loc: SourceLocation?)
    class UndefinedFunction(name: String, loc: SourceLocation?)
    class TimeoutError(timeoutMs: Long, description: String, loc: SourceLocation?)
    class General(reason: String, loc: SourceLocation?, cause: Throwable? = null)
}
```

### Error Format

```
TypeError: Cannot apply '+' to String and Boolean
  at line 23, column 15
```

---

## Statement Execution

| Statement | Behavior |
|-----------|----------|
| `CodeInvocation` | Evaluate args, `dispatcher.dispatch(code, argsMap)` |
| `FunctionCall` | Lookup @define, push scope, bind params (named then positional), execute body |
| `WaitDelay` | `kotlinx.coroutines.delay(ms)` |
| `WaitCondition` | Poll condition every 100ms, throw `TimeoutError` on timeout |
| `IfElse` | Evaluate condition to Boolean, execute `thenBody` or `elseBody` |
| `Repeat` | Evaluate count, check loop limit, execute body N times |
| `While` | Evaluate condition each iteration, check loop limit |
| `Sequence` | Execute body statements sequentially |
| `Assignment` | Evaluate expression, set variable in scope |
| `Log` | Evaluate expression, `println("[AVU DSL] $msg")` |
| `Return` | Throw `ReturnException(value)`, caught at function/workflow boundary |
| `Emit` | Evaluate data, call `context.emitEvent()` |

### Function Parameter Binding

Named arguments bind first, then positional fill remaining slots:

```
@define login(app, username, password)
  ...

# Named arguments:
login(app: "slack", username: "john", password: "secret")

# Mixed (positional fills unbound):
login("slack", password: "secret", username: "john")
```

---

## Full Usage Example

```kotlin
import com.augmentalis.voiceoscore.dsl.lexer.AvuDslLexer
import com.augmentalis.voiceoscore.dsl.parser.AvuDslParser
import com.augmentalis.voiceoscore.dsl.interpreter.*

// 1. Parse
val source = File("workflow.vos").readText()
val tokens = AvuDslLexer(source).tokenize()
val parseResult = AvuDslParser(tokens).parse()
if (parseResult.hasErrors) { /* report errors */ }

// 2. Execute
val dispatcher = CompositeDispatcher(listOf(
    AccessibilityDispatcher(service),
    VoiceCommandDispatcher(),
    QueryDispatcher(screenReader)
))
val interpreter = AvuInterpreter(dispatcher, SandboxConfig.DEFAULT)

when (val result = interpreter.execute(parseResult.file)) {
    is ExecutionResult.Success -> println("Done in ${result.executionTimeMs}ms")
    is ExecutionResult.Failure -> println("Error: ${result.error.message}")
    is ExecutionResult.NoHandler -> println("No handler found")
}
```

---

## Platform Dispatchers (Implemented - Phase 6)

**Package:** `com.augmentalis.voiceoscore.dsl.interpreter`

| Platform | Class | Source Set | Features |
|----------|-------|-----------|----------|
| Android | `AndroidAvuDispatcher` | `androidMain` | Bridges to HandlerRegistry, IAvuEnvironment for QRY |
| iOS | `IosAvuDispatcher` | `iosMain` | QRY/LOG/CHT/TTS support, stubs for gesture codes |
| Desktop | `DesktopAvuDispatcher` | `desktopMain` | Clipboard (CLP), system props (SYS), QRY, LOG |
| All | `LoggingDispatcher` | `commonMain` | Debug wrapper recording all dispatch calls |

### IAvuEnvironment

Platform abstraction for screen state queries used by all dispatchers:

```kotlin
interface IAvuEnvironment {
    suspend fun screenContains(text: String): Boolean
    suspend fun getScreenText(): List<String>
    suspend fun isElementVisible(elementId: String): Boolean
    suspend fun getForegroundApp(): String?
    suspend fun getProperty(key: String): Any?
}
```

### AndroidAvuDispatcher

The primary production dispatcher. Routes AVU codes to HandlerRegistry:

```kotlin
class AndroidAvuDispatcher(
    handlerRegistry: HandlerRegistry,
    environment: IAvuEnvironment = StubEnvironment()
) : IAvuDispatcher
```

- **QRY** codes → IAvuEnvironment (screen queries)
- **VCM/AAC/CHT/etc.** → HandlerRegistry.findHandler() → IHandler.execute()
- Maps 50+ AVU codes to handler actions

### LoggingDispatcher

Debug wrapper for recording and inspecting dispatch calls:

```kotlin
val logging = LoggingDispatcher(innerDispatcher)
// ... execute DSL ...
val log: List<DispatchLogEntry> = logging.getLog()
```

## Tooling (Implemented - Phase 7)

**Package:** `com.augmentalis.voiceoscore.dsl.tooling`

| Tool | Purpose |
|------|---------|
| `AvuDslHighlighter` | Syntax highlighting (16 categories: directive, code, string, etc.) |
| `AvuDslFormatter` | AST-based code formatter / pretty-printer |
| `AvuDslValidator` | Static analysis: E001-E003 errors, W001-W004 warnings, I001-I002 info |
| `WorkflowRecorder` | Records dispatch calls and generates .vos files |

### AvuDslValidator Diagnostics

| Code | Severity | Description |
|------|----------|-------------|
| E001 | Error | Undeclared code used in body |
| E002 | Error | Undefined function called |
| E003 | Error | Empty workflow/function body |
| W001 | Warning | Unused declared code |
| W002 | Warning | Duplicate trigger pattern |
| W003 | Warning | Variable set but never read |
| W004 | Warning | Unreachable code after @return |
| I001 | Info | No triggers defined |
| I002 | Info | Function defined but never called |

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
| 1.0 | 2026-02-06 | Initial chapter (design spec) |
| 2.0 | 2026-02-06 | Updated with actual implementation (Phase 2 complete) |
| 3.0 | 2026-02-06 | Updated: platform dispatchers (Phase 6) + tooling (Phase 7) implemented |
