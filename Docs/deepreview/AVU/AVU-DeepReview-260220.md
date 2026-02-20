# AVU Module — Deep Code Review
**Date:** 2026-02-20
**Reviewer:** Code Reviewer Agent
**Scope:** `Modules/AVU/src/` — all 58 .kt files
**Branch:** HTTPAvanue

---

## Summary

The AVU (Avanues Universal DSL) module is a well-architected, KMP-clean Kotlin Multiplatform library implementing a full DSL pipeline: wire protocol codec, lexer, parser, AST, tree-walking interpreter, plugin sandbox, tooling (formatter, validator, highlighter, recorder), and migration utilities. The overall structure is sound, the sealed class hierarchy is expressive, and the codec escape scheme is correctly implemented. Test coverage is good for the interpreter and migration layers.

However, several issues require immediate attention. The most severe is a **3-letter code collision** in `AVUEncoder`: two distinct protocol features (`CODE_ACCEPT_DATA` and `CODE_APP_CATEGORY_DB`) share the identical code `"ACD"`, making their messages indistinguishable on the wire. The lexer has a **critical dash-detection bug** where `--` (two dashes) is misidentified as a `---` header separator, causing any `--` comment-like text to silently corrupt file parsing. The `RuntimeError.SandboxViolation` factory methods for `stepLimit`, `nestingLimit`, and `variableLimit` **always report `current == limit`** rather than the actual step count, making sandbox diagnostics misleading. Additionally, two test files reference a non-existent package (`com.augmentalis.voiceoscore.dsl.*`) and will not compile.

Thread safety is a systemic concern: five global singletons (`AvuCodeRegistry`, `CodePermissionMap`, `PluginSandbox`, `PluginRegistry`, `LoggingDispatcher`) hold mutable state in unsynchronized `mutableMapOf`/`mutableListOf` structures. The module appears designed for single-threaded or test-time use, but the `PluginRegistry.handleTrigger()` is a `suspend` function that can be called from multiple coroutines, and the `LoggingDispatcher` is explicitly a decorator meant for production wrapping — making these races real and not hypothetical.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `AVUEncoder.kt:33,68` | `CODE_ACCEPT_DATA = "ACD"` and `CODE_APP_CATEGORY_DB = "ACD"` are duplicate 3-letter codes. `encodeAccept()` and App Category DB headers produce identical wire prefixes that are indistinguishable on decode. | Rename one code. Suggested: rename `CODE_APP_CATEGORY_DB` to `"ACB"` (App Category Base) or `"ADB"`. Update all callers and tests. |
| Critical | `AvuDslLexer.kt:105` | `c == '-' && canMatch('-') && canMatchAt(current, '-')` — after consuming the first `-` (now at `current`), both `canMatch('-')` and `canMatchAt(current, '-')` check `source[current]` (the same character). The condition only requires `--` (two dashes) to trigger `scanHeaderSeparator()`, not `---`. Any `--` sequence (e.g., `-- a comment`) is misidentified as a header separator. | Fix to `c == '-' && canMatch('-') && canMatchAt(current + 1, '-')` so the third dash position is checked correctly. |
| Critical | `RuntimeError.kt:31,37,40` | `stepLimit()`, `nestingLimit()`, `variableLimit()` all pass `limit.toLong()` as both `limit` and `current` in the `SandboxViolation` constructor. Error messages always read `current=N` where `N == limit`, making it impossible to distinguish "hit limit exactly" from "exceeded limit by 50". | Pass the actual current count as a parameter: `fun stepLimit(limit: Int, current: Int, location: SourceLocation?)`. Thread the current count through from `ExecutionContext.incrementStep()`. |
| Critical | `ExecutionContextTest.kt:71` | Imports `com.augmentalis.voiceoscore.dsl.ast.AvuAstNode` — package does not exist. Correct package is `com.avanues.avu.dsl.ast.AvuAstNode`. Test will not compile. | Fix import to `import com.avanues.avu.dsl.ast.AvuAstNode`. |
| Critical | `AvuDslHighlighterTest.kt:140–169` | All `Token` and `TokenType` references use `com.augmentalis.voiceoscore.dsl.lexer.*` — wrong package. Correct package is `com.avanues.avu.dsl.lexer.*`. Tests will not compile. | Fix all fully-qualified references to `com.avanues.avu.dsl.lexer.TokenType` and `com.avanues.avu.dsl.lexer.Token`. |
| High | `AVUEncoder.kt:170` | `encodeChat()`: `"$CODE_CHAT:$messageId:$senderId:${escape(text)}"` — `messageId` and `senderId` are not escaped. Any `:` or `%` in these fields corrupts the wire message. | Replace with `"$CODE_CHAT:${escape(messageId)}:${escape(senderId)}:${escape(text)}"`. |
| High | `AVUEncoder.kt:699` | `encodeHandover()`: `"${entry.code}:${entry.key}:${escape(entry.value)}"` — `entry.key` is not escaped. Keys containing `:` (e.g., `"url:path"`) corrupt the handover file format. | Replace with `"${entry.code}:${escape(entry.key)}:${escape(entry.value)}"`. |
| High | `AVUDecoder.kt:34` | `ParsedMessage.param(index: Int)` accesses `params[index]` with no bounds check. Throws `IndexOutOfBoundsException` for any caller that doesn't pre-validate `params.size`. Safe alternative `paramOrNull()` exists but is inconsistently used. | Add bounds check: `fun param(index: Int): String = AVUEncoder.unescape(params.getOrNull(index) ?: throw IndexOutOfBoundsException("No param at index $index (size=${params.size})"))`. |
| High | `AvuInterpreter.kt:333` | `executeWaitCondition()` calls `context.incrementStep(stmt.location)` inside the polling loop (every 100ms). A 5-second `@wait` with default sandbox `maxSteps=1000` fires 50 step increments during polling, spending 5% of the step budget on wait overhead. A 100-second wait would trigger `SandboxViolation("step limit exceeded")` instead of `TimeoutError`. | Move `incrementStep()` out of the wait loop. The timeout mechanism already enforces wall-clock limits; polling should not burn steps. |
| High | `AvuCodeRegistry.kt:31–32` | Global `object` singleton with `mutableMapOf` for `codes` and `codesByCategory`. `register()` and `get()` are called from multiple coroutines/threads without synchronization (plugin loading, module init). Data race on all map operations. | Use `ConcurrentHashMap` or wrap with `@Synchronized`/`Mutex`. For KMP compatibility, use `kotlinx.atomicfu` or a `Mutex`-guarded wrapper. |
| High | `LoggingDispatcher.kt:23` | `private val _log = mutableListOf<DispatchLogEntry>()` — plain mutable list. `dispatch()` is `suspend` and may be called from multiple coroutines. Concurrent `_log.add()` operations race. | Replace with `CopyOnWriteArrayList` (JVM) or use a `Mutex`-guarded list in `commonMain`. |
| High | `PluginRegistry.kt:22–23` | `plugins` and `triggerIndex` are plain `mutableMapOf()`. `handleTrigger()` is `suspend` — concurrent trigger invocations from multiple coroutines race on both maps. Registration mutations also race with active dispatch. | Wrap all map access in a `kotlinx.coroutines.sync.Mutex` (consistent with the rest of the interpreter's coroutine approach). |
| High | `PluginSandbox.kt:56,64` | `verifiedAuthors` is a `mutableSetOf` on a global `object`. `addVerifiedAuthor()` and `isVerifiedAuthor()` (called from `determineTrustLevel()`) race if called concurrently. | Use `CopyOnWriteArraySet` or guard with `@Synchronized`. |
| High | `CodePermissionMap.kt` | `customMappings` is a `mutableMapOf()` on a global `object`. `registerCustomMapping()` races with `getRequiredPermissions()` under concurrent plugin loading. | Synchronize access or use `ConcurrentHashMap`. |
| High | `MacroDslMigrator.kt:162,174` | `generateStep()` for `Conditional` and `LoopWhile` outputs `step.condition` raw into the DSL body: `"$indent@if ${step.condition}"`. If condition strings contain `"`, `\n`, or special DSL characters, the generated file is syntactically invalid. | Escape condition strings with the same `escapeString()` helper used for `Variable` values (L181), or wrap in quotes and escape. |
| High | `AvuV1Compat.kt:31` | `message.split(":")` splits on ALL colons including unescaped colons in field values. `VCM:id:https://example.com` parses as 4 fields instead of 3, corrupting URL-containing V1 messages. V1 format has no escaping mechanism. | Document the known limitation clearly. For URL fields, apply `URI.decode` or restrict to known-safe field positions. At minimum, add a test that documents the broken behavior. |
| High | `DesktopAvuDispatcher.kt:108,118` | `handleClipboard()` calls `java.awt.Toolkit.getDefaultToolkit()` inside a `try { } catch (e: Exception)` block. On headless JVMs, `getDefaultToolkit()` throws `java.awt.AWTError` which is an `Error`, not an `Exception`. The catch is bypassed and the `AWTError` propagates uncaught through the `suspend` dispatch call. | Catch `Throwable` (or specifically `AWTError`) at the outer level, or check `GraphicsEnvironment.isHeadless()` before attempting clipboard access. |
| High | `DesktopAvuDispatcher.kt:61` | QRY `foreground_app` check: `arguments["query"] == "foreground_app"`, but `ExpressionEvaluator` builds the QRY dispatch using `target + "_" + method` → `"context_foreground_app"`. The check never matches; `context.foreground_app()` in DSL always falls through to `getProperty()` instead of `getForegroundApp()`. | Change check to `arguments["query"] == "context_foreground_app"` to match how `ExpressionEvaluator` builds the QRY arguments. |
| Medium | `AVUDecoder.kt:451` | `parseAppPatternGroup()`: `if (patterns.isEmpty()) return null` is a dead guard — `split("|")` on any string always returns at least one element (`[""]` for empty string). An empty pattern group passes through as `listOf("")`. | Remove the dead guard and instead check `if (patterns.all { it.isBlank() }) return null`. |
| Medium | `AVUDecoder.kt` (plugin manifest parsing) | Config block detection: `value == "start"` is a case-sensitive comparison. `CFG:Start` or `CFG:START` silently fails to open the config block. | Use `value.lowercase() == "start"` and `"end"` for the comparison. |
| Medium | `AvuCodeRegistry.kt:186` | `val expectedFields = info.format.split(":").size` is computed in `validate()` but the variable is never used — only `info.fields.count { it.required }` is used for the actual check. Dead variable silently misleads future readers. | Remove the unused `expectedFields` variable, or use it in an actual format validation check. |
| Medium | `ExpressionEvaluator.kt:101–102` | `AND`/`OR` operators evaluate both sides regardless of the left operand result (documented as non-short-circuit). In `@if screen.contains("x") and screen.contains("y")`, both QRY dispatches fire even when the first is false. QRY dispatches may trigger side effects (screen scanning, state reads). | Consider implementing short-circuit evaluation for `AND`/`OR` as the default behavior. Provide a `strict_eval` pragma or a `band`/`bor` variant if non-short-circuit is needed for specific use cases. |
| Medium | `ExpressionEvaluator.kt:125` | `result == result.toLong().toDouble()` — floating-point equality used to detect integer results. For values like `9007199254740993.0` (exceeds `Double` integer precision), the comparison gives wrong results and `Int` truncation occurs incorrectly. | Use `left is Int && right is Int` check before the arithmetic instead of checking the result afterward. |
| Medium | `AvuDslHighlighter.kt:147–154` | `categorizeHeaderLine()` classifies any header line containing `:` as `HEADER_KEY`. Lines with URL values like `  description: https://example.com` get `HEADER_KEY` for the entire line (key + colon + value). The value portion is never separately classified as `HEADER_VALUE`. | Split on first `:` and emit two spans: one for the key part (`HEADER_KEY`) and one for the value part (`HEADER_VALUE`). This requires changing `highlight()` to handle `HEADER_LINE` tokens specially rather than delegating entirely to `categorize()`. |
| Medium | `AvuDslHighlighter.kt:160–168` | `findTokenOffset()` is O(source.length) per call (full linear scan from offset 0 for every token). `highlight()` calls it for every token, making the overall complexity O(tokens × source.length). For a 1,000-line, 500-token file: ~500,000 character scans. | Pre-compute a line-start offset array once before the `mapNotNull` loop: `val lineOffsets = computeLineOffsets(source)`. Then `findTokenOffset` becomes O(1): `return lineOffsets[token.line - 1] + token.column - 1`. |
| Medium | `WorkflowRecorder.kt:179` | `formatArgValue()` for `String` wraps in quotes but does not escape backslashes or embedded quotes: `is String -> "\"$value\""`. Input `say "hello"` produces `"say "hello""` — invalid DSL. | Escape the string: `is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""`. |
| Medium | `WorkflowRecorder.kt:137` | `val safeName = workflowName.lowercase()...` computed but never used in `generate()` output — `@workflow "$workflowName"` uses the original name. Dead variable. | Remove `safeName` or use it as the workflow identifier in the generated `@workflow` declaration for DSL compatibility (identifiers cannot contain spaces). |
| Medium | `WorkflowRecorder.kt:143` | When `steps` is empty, `generate()` produces a valid header but an empty `@workflow` body — the validator would report E003 (empty workflow). | Either skip `generate()` when empty (returning an empty string), or emit a `# (no steps recorded)` comment inside the workflow body. |
| Medium | `IAvuEnvironment.kt` (`StubEnvironment`) | `StubEnvironment` silently returns `false`/`null`/empty for all methods. No logging indicates when stub results are being served. Developers on iOS/Desktop debugging why `screen.contains()` always returns false get no indication the stub is active. | Add `println("[AVU] StubEnvironment: $methodName called — no real implementation on this platform")` in each stub method, or accept a `logger: (String) -> Unit` parameter. |
| Medium | `PluginManifest.kt` | `pluginId` validation regex allows single-character IDs like `"a"`. Reverse-domain convention (`com.example.plugin`) requires at least two dot-separated components. A short ID like `"x"` passes validation and could shadow important codes. | Tighten regex to require at least one dot: `"^[a-zA-Z][a-zA-Z0-9]*([._-][a-zA-Z0-9]+)+$"`. |
| Medium | `PluginSandbox.kt:49` | `determineTrustLevel()` grants `SYSTEM` trust to any plugin that merely claims a `com.augmentalis.` ID prefix. There is no cryptographic signature verification. A malicious or compromised plugin can self-elevate to `SYSTEM` trust. | Document this as a known limitation pending a code-signing scheme. At minimum, log a warning when a non-built-in plugin receives `SYSTEM` trust. Consider downgrading to `VERIFIED` for third-party plugins regardless of claimed ID. |
| Medium | `AvuDslValidator.kt` | E003 (empty body) only checked for `@workflow` declarations, not for `@on` trigger handler declarations. An empty `@on voice_command("test")` passes validation but is a runtime no-op. | Add E003 check for `TriggerHandler` nodes with empty `body` lists. |
| Medium | `AvuDslValidator.kt` | W003 (set but never read) uses global `assignedVars`/`readVars` sets across all declarations. A var set in `@define funcA()` and never read within `funcA` does not trigger W003 if any other declaration reads a variable of the same name. False negatives for unused variables in named functions. | Track variable usage per-declaration scope, not globally. |
| Medium | `AvuDslValidator.kt` | W004 (unreachable code) only checks the top-level body list. Unreachable statements inside `@if`, `@repeat`, `@while`, or `@sequence` bodies are never reported. | Recurse into nested statement bodies in `checkUnreachableCode()`. |
| Medium | `PluginRegistry.kt:116` | `handleTrigger()` constructs a new `AvuInterpreter` on every trigger invocation. If voice triggers are frequent (e.g., "show clock" fires repeatedly), this allocates a new interpreter + evaluator per invocation. | Cache a per-plugin interpreter instance and reset its `ExecutionContext` between invocations instead of reconstructing. |
| Medium | `PluginRegistry.kt:128` | When a plugin trigger execution fails, the plugin transitions to `ERROR` state permanently. There is no recovery path — subsequent same-trigger calls return `Error("Plugin is not active")` indefinitely until explicit re-registration. | Introduce a `DEGRADED` state with configurable retry limit, or expose a `resetPlugin(pluginId)` API. |
| Medium | `AvuDslFormatter.kt` | `formatExpression()` for `BinaryOp` emits `left op right` without parentheses. `(a + b) * c` parses correctly (AST respects precedence) but formats as `a + b * c`, which re-parses with different precedence. Round-trip is semantically incorrect for expressions with mixed precedence. | Wrap sub-expressions in parentheses when the child operator has lower precedence than the parent. |
| Low | `AvuHandoverCodes.kt:161` | `registerAll()` silently swallows `IllegalArgumentException` with `catch (_: IllegalArgumentException)`. The comment says this is expected for shared codes, but there is no logging at all — a genuine registration bug (wrong format string) would be invisible. | Log a `println("[AVU] Handover code ${info.code} already registered with different definition — using existing")` in the catch block so intentional silencing is still auditable. |
| Low | `AvuDslFormatter.kt` | `formatHeader()` omits the `# Avanues Universal Format v2.2` comment lines that `encodeHandover()` and `AvuHeader` generate. A `parse → format → parse` round-trip produces valid output but without the version comment, making format identification harder for non-standard tooling. | Include the `# Avanues Universal Format v2.2` comment as the first line(s) of `formatHeader()` output. |
| Low | `AvuDslParser.kt` | `expectStringLiteral()` fallback `token.lexeme.removeSurrounding("\"")` only strips double quotes. The lexer supports both `"` and `'` string literals; single-quoted strings would not be stripped by the fallback path. | Use `removeSurrounding("\"").removeSurrounding("'")` or check the actual delimiter character before stripping. |
| Low | `MacroDslMigrator.kt` | `migrateMultiple()` uses the metadata (name, description, author) from `macros.first()` for the combined output header, labeled as a "Collection". Remaining macros' metadata is silently discarded. | Accept an explicit `CollectionMetadata(name, description)` parameter for the combined header rather than stealing from the first entry. |
| Low | `IosAvuDispatcher.kt:57` | `method == "foreground_app" || arguments["query"] == "foreground_app"` — the `method == "foreground_app"` branch is unreachable because `ExpressionEvaluator` does not pass a `method` argument in QRY dispatches (it builds `query = "target_method"`). Dead OR branch. Note: the Desktop counterpart (L61) does not have the method check and is also broken (see HIGH item above). | Align iOS check with the actual QRY dispatch format: `arguments["query"] == "context_foreground_app"`. |
| Low | `LoggingDispatcherTest.kt` | Defines a local `private class MockDispatcher` that shadows the shared `MockDispatcher` from the test package. This defeats test reuse and can mask behavioral differences between the two implementations. | Remove the local `MockDispatcher` and import the shared one from `com.avanues.avu.dsl.interpreter.MockDispatcher`. |
| Low | `AvuCodeRegistry.kt` | `register()` allows the same `AvuCodeInfo` instance to be registered multiple times (identical info is a no-op). But `codesByCategory` appends the info to the list on every call, even for duplicates. Multiple calls to `registerAll()` at startup (e.g., during hot reload) result in duplicate entries in `getByCategory()` output. | Add a guard: `if (existing == info) return` before the `codes[info.code] = info` and `codesByCategory` append. |
| Low | `AvuDslHighlighter.kt:160` | `findTokenOffset()` does not handle `\r\n` line endings. It only increments `line` on `\n`, so a `\r\n`-encoded file with token at line 3 would have its column offset off by the number of `\r` characters preceding it. | In the line-scan loop, also skip `\r` characters: `if (source[offset] == '\r') { offset++; continue }`. |

---

## Detailed Findings

### CRITICAL-1: 3-Letter Code Collision — `AVUEncoder.kt:33` and `AVUEncoder.kt:68`

```kotlin
// Line 33 — IPC response code
const val CODE_ACCEPT_DATA = "ACD"

// Line 68 — App Category DB header code
const val CODE_APP_CATEGORY_DB = "ACD"  // ACD:version:timestamp:author (header)
```

Both constants resolve to `"ACD"`. Any decoder receiving an `ACD:...` message cannot determine whether it is an accept-with-data response or an App Category DB header. `encodeAccept(requestId, data)` and `parseAppCategoryDb()` both emit/expect the same prefix. This is a **protocol-level collision** — the wire is ambiguous.

**Fix:** Rename `CODE_APP_CATEGORY_DB` to `"ACB"` (App Category Base) or `"ADB"` (App DB). Update `encodeAppCategoryDb()`, `parseAppCategoryDb()`, and all call sites. Add a `AvuCodeInfo` init-time uniqueness check so this class of error is caught at registration.

---

### CRITICAL-2: Lexer Dash Detection Bug — `AvuDslLexer.kt:105`

```kotlin
c == '-' && canMatch('-') && canMatchAt(current, '-') -> scanHeaderSeparator()
```

After `advance()` consumes the first `-`, `current` points to position of the **second** `-`. At this point:
- `canMatch('-')` checks `source[current]` (position of second dash) — correct
- `canMatchAt(current, '-')` also checks `source[current]` (same position as second dash) — **duplicate check**

Neither call checks position `current + 1` (the third dash). The condition is satisfied by any `--` sequence, not just `---`. Input like `-- This is a comment` or `--deprecated` triggers `scanHeaderSeparator()`, increments `headerSeparatorCount`, and enters header-parsing mode unexpectedly.

**Fix:**
```kotlin
// Before (broken):
c == '-' && canMatch('-') && canMatchAt(current, '-') -> scanHeaderSeparator()

// After (correct):
c == '-' && canMatch('-') && canMatchAt(current + 1, '-') -> scanHeaderSeparator()
```

---

### CRITICAL-3: SandboxViolation Wrong `current` Value — `RuntimeError.kt:31,37,40`

```kotlin
companion object {
    fun stepLimit(limit: Int, location: SourceLocation?) =
        SandboxViolation("step limit exceeded", limit.toLong(), limit.toLong(), location)
        //                                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //                                      both limit and current = same value

    fun nestingLimit(limit: Int, location: SourceLocation?) =
        SandboxViolation("nesting depth exceeded", limit.toLong(), limit.toLong(), location)

    fun variableLimit(limit: Int, location: SourceLocation?) =
        SandboxViolation("variable limit exceeded", limit.toLong(), limit.toLong(), location)
}
```

The error message produced is: `"Sandbox violation: step limit exceeded (limit=1000, current=1000)"`. The `current` field is always identical to `limit`, providing no diagnostic value. Only `loopLimit()` passes the actual iteration count as `current`.

**Fix:** Add `current: Int` parameter to each factory method and thread the actual count through from `ExecutionContext`:
```kotlin
fun stepLimit(limit: Int, current: Int, location: SourceLocation?) =
    SandboxViolation("step limit exceeded", limit.toLong(), current.toLong(), location)
```

---

### CRITICAL-4 & 5: Wrong Package in Test Files

**`ExecutionContextTest.kt:71`:**
```kotlin
val funcDef = com.augmentalis.voiceoscore.dsl.ast.AvuAstNode.Declaration.FunctionDef(
```
Package `com.augmentalis.voiceoscore.dsl.ast` does not exist. Correct: `com.avanues.avu.dsl.ast`.

**`AvuDslHighlighterTest.kt:140–169`:**
```kotlin
com.augmentalis.voiceoscore.dsl.lexer.TokenType.AT_WORKFLOW,
com.augmentalis.voiceoscore.dsl.lexer.Token(type, "@test", 1, 1)
```
Package `com.augmentalis.voiceoscore.dsl.lexer` does not exist. Correct: `com.avanues.avu.dsl.lexer`.

Both test files will fail to compile. Fix all fully-qualified references to use the `com.avanues.avu.*` package tree.

---

### HIGH-1: `encodeChat()` Unescaped Fields — `AVUEncoder.kt:170`

```kotlin
fun encodeChat(messageId: String = "", senderId: String = "", text: String): String {
    require(text.isNotBlank()) { "text cannot be blank" }
    return "$CODE_CHAT:$messageId:$senderId:${escape(text)}"
    //              ^^^^^^^^^^^  ^^^^^^^^^^
    //              NOT escaped  NOT escaped
}
```

If `messageId = "msg:001"` or `senderId = "user:42"`, the resulting wire message `CHT:msg:001:user:42:Hello` has 6 colon-delimited fields instead of 4, corrupting the `id` and `params` on the decode side.

**Fix:**
```kotlin
return "$CODE_CHAT:${escape(messageId)}:${escape(senderId)}:${escape(text)}"
```

---

### HIGH-2: `encodeHandover()` Unescaped Key — `AVUEncoder.kt:699`

```kotlin
handover.entries.forEach { entry ->
    appendLine("${entry.code}:${entry.key}:${escape(entry.value)}")
    //                        ^^^^^^^^^^^^^
    //                        key NOT escaped
}
```

Handover keys such as `"url:original"`, `"api:endpoint"`, or any key containing `:` will produce a 4-field line instead of 3, breaking `parseHandoverEntry()`.

**Fix:**
```kotlin
appendLine("${entry.code}:${escape(entry.key)}:${escape(entry.value)}")
```

---

### HIGH-3: `executeWaitCondition()` Burns Steps — `AvuInterpreter.kt:333`

```kotlin
while (true) {
    val condValue = evaluator.evaluate(stmt.condition, context)
    if (evaluator.toBooleanValue(condValue)) return

    val elapsed = currentTimeMillis() - startTime
    if (elapsed >= timeoutMs) {
        throw RuntimeError.TimeoutError(timeoutMs, "Wait condition not met", stmt.location)
    }
    delay(pollInterval)
    context.incrementStep(stmt.location)  // <-- inside polling loop
}
```

With default `maxSteps = 1000` and `pollInterval = 100ms`, a 5-second wait consumes 50 steps (5% of budget). A 100-second wait with `SandboxConfig.DEFAULT` (no wall-clock limit override) would fire `SandboxViolation("step limit exceeded")` at ~100 seconds instead of `TimeoutError`, producing a misleading error type.

**Fix:** Remove `context.incrementStep()` from the wait loop. The wait loop should only increment for condition evaluations that involve real expression work, not for the polling delay overhead. Move the `incrementStep` call to just before `evaluator.evaluate(stmt.condition, context)` and count only one step per condition evaluation, not per delay cycle.

---

### HIGH-4: Thread Safety — Five Unsynchronized Global Singletons

The following global `object` singletons hold mutable state in plain `mutableMapOf`/`mutableSetOf`/`mutableListOf` without any synchronization:

| Singleton | Mutable Field | Risk |
|-----------|--------------|------|
| `AvuCodeRegistry` | `codes`, `codesByCategory` | Module init races with plugin `register()` |
| `CodePermissionMap` | `customMappings` | Plugin load races with permission check |
| `PluginSandbox` | `verifiedAuthors` | `addVerifiedAuthor()` races with `determineTrustLevel()` |
| `PluginRegistry` | `plugins`, `triggerIndex` | `handleTrigger()` (suspend) races with `register()`/`unregister()` |
| `LoggingDispatcher` | `_log` | `dispatch()` (suspend) races: concurrent `_log.add()` |

**Fix:** For KMP compatibility use `kotlinx.coroutines.sync.Mutex` to guard mutable map/list access in all five locations. For `AvuCodeRegistry` and `CodePermissionMap` (read-heavy), a `ReentrantReadWriteLock` (JVM only) or a `kotlinx.atomicfu`-based snapshot map provides better read throughput. Alternatively, if all registrations are guaranteed to complete before any concurrent reads (startup ordering), document this contract explicitly.

---

### HIGH-5: Desktop Clipboard `AWTError` Uncaught — `DesktopAvuDispatcher.kt:108,118`

```kotlin
"get" -> {
    try {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        // ...
    } catch (e: Exception) {   // <-- only catches Exception
        DispatchResult.Error("Clipboard read failed: ${e.message}")
    }
}
```

On headless JVMs (`-Djava.awt.headless=true`), `getDefaultToolkit()` throws `java.awt.AWTError` which extends `Error`, not `Exception`. The catch block is bypassed and the error propagates as an unhandled `AWTError` through the `suspend` dispatch chain.

**Fix:**
```kotlin
"get" -> {
    if (java.awt.GraphicsEnvironment.isHeadless()) {
        return DispatchResult.Error("Clipboard not available in headless environment")
    }
    try {
        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        // ...
    } catch (e: Exception) {
        DispatchResult.Error("Clipboard read failed: ${e.message}")
    }
}
```

---

### HIGH-6: QRY `foreground_app` Mismatch — Desktop and iOS Dispatchers

`ExpressionEvaluator` dispatches QRY calls using:
```kotlin
// Built from: target.method (e.g., context.foreground_app → "context_foreground_app")
dispatcher.dispatch("QRY", mapOf("query" to "${callable.target}_${callable.method}", ...))
```

But `DesktopAvuDispatcher.handleQuery()` checks:
```kotlin
arguments["query"] == "foreground_app"   // never "context_foreground_app"
```

This means `context.foreground_app()` in DSL always falls through to `environment.getProperty("")` (returning `null`) instead of calling `environment.getForegroundApp()`.

`IosAvuDispatcher` has the same bug (L57): the `method == "foreground_app"` branch is unreachable (no `method` key in QRY dispatch map), and `arguments["query"] == "foreground_app"` also never matches.

**Fix:** Change both dispatchers to check `arguments["query"] == "context_foreground_app"` to match the evaluator's dispatch format.

---

### HIGH-7: `MacroDslMigrator` — Unescaped Condition Strings — L162,174

```kotlin
is MigrationStep.Conditional -> {
    sb.appendLine("$indent@if ${step.condition}")   // condition is raw, unescaped
    // ...
}
is MigrationStep.LoopWhile -> {
    sb.appendLine("$indent@while ${step.condition}") // same issue
    // ...
}
```

If a macro condition string contains `"`, `\n`, or `@`, the generated DSL is syntactically invalid. The generated output cannot be round-tripped through the parser.

**Fix:** The condition should either be treated as a raw DSL expression (in which case the migrator must parse and re-emit it via `AvuDslFormatter`) or be quoted as a string and the comparator generated from context. At minimum, escape unsafe characters using `escapeString()` or wrap in a comment if automatic escaping is not feasible.

---

### HIGH-8: `AvuV1Compat.parseV1Message()` — Unescaped Colon Split — L31

```kotlin
val parts = message.split(":")
```

V1 wire format has no escaping. `VCM:id:https://example.com` splits into `["VCM", "id", "https", "//example.com"]` (4 parts) rather than `["VCM", "id", "https://example.com"]` (3 parts). The `fields` list will have 3 entries where field `[1]` is `"https"` and field `[2]` is `"//example.com"`.

This is a known limitation of V1 (no escaping), but it is not documented and the test suite does not include a URL-field test case.

**Fix:** Document the known limitation clearly in KDoc. Add a test that shows URL fields break: `parseV1Message("VCM:cmd1:https://example.com")` should have a comment explaining the expected broken behavior. Consider `splitFirst(n)` alternatives for known codes where the last field may contain colons.

---

## Missing Test Coverage

The following critical areas have no tests:

| Area | Missing Tests |
|------|--------------|
| `AVUEncoder` / `AVUDecoder` | No codec round-trip tests. No tests for escaped special characters in `encodeChat`, `encodeVoiceCommand`, `encodeHandover`. No test for `ACD` code collision. |
| `AvuHeader` | No tests for `parse()`, `generate()`, or `bodyStart` offset computation. No `\r\n` line ending test. |
| `AvuEscape` | No test for `%` inside values, double-escape behavior, or `escapeIfNeeded` optimization. |
| `AvuCodeRegistry` | No tests for `register()`, duplicate detection, or `generateLegend()` output. |
| `DesktopAvuDispatcher` | No tests for any dispatch path, especially clipboard or QRY. |
| `IosAvuDispatcher` | No tests at all. |
| `AvuV1Compat` | No test for URL-containing V1 fields (colon split corruption). |
| Lexer `---` bug | No test that `--text` does not trigger header mode. |
| `SandboxViolation.current` | No test that `current != limit` in the violation error. |

---

## Rule 7 Violations

No Rule 7 violations found. All `@author` fields use `"Augmentalis Engineering"` throughout. No files contain `"Claude"`, `"AI Assistant"`, `"VOS4 Development Team"`, or similar prohibited attributions.

---

## Health Signal

| Severity | Count |
|----------|-------|
| Critical | 5 |
| High | 13 |
| Medium | 15 |
| Low | 9 |
| **Total** | **42** |
