# AVID / AVU / AVACode — Quality Analysis Review
**Date:** 260222 | **Reviewer:** Code-Reviewer Agent | **Branch:** VoiceOS-1M-SpeechEngine

---

## Summary

Three modules form the DSL/codec/fingerprint layer of NewAvanues. AVU is the largest (58 kt) and has the highest defect density, including a confirmed protocol code collision (`ACD`) documented previously and a newly identified lexer off-by-one in the header separator scanner. AVID has a fundamental split between two ID generation systems that do not interoperate (one deterministic, one random), and a platform-tagging footgun. AVACode is the cleanest of the three but has JVM-specific API usage in commonMain and a fragile `commit()` implementation.

---

## Module 1: AVID

**Files:** 9 kt across commonMain / androidMain / iosMain / desktopMain

### SCORE: 72 / 100 | HEALTH: YELLOW

---

### P0 Issues (Critical)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `AvidGenerator.kt:423` | `generateFromContent()` calls `content.hashCode()` — Kotlin's `String.hashCode()` is NOT specified to be deterministic across JVM vs Kotlin/Native. On K/N it uses MurmurHash3, on JVM it uses a polynomial hash. Two platforms hashing the same content string will produce different IDs, breaking cross-platform content addressing. | Replace with `Fingerprint.deterministicHash(content, 16)` from the same module. |

### P1 Issues (High)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `AvidGenerator.kt:248` + `Fingerprint.kt:47` | Two separate ID generation systems that **do not interoperate**: `Fingerprint.forElement()` is deterministic (polynomial hash), `AvidGenerator.generateCompact()` calls `generateHash8()` (random UUID segment). The same UI element scanned twice gets two different compact AVIDs. Determinism is only available if callers manually pass `elementHash` from `Fingerprint`. This is not enforced and easily missed. | `generateCompact()` should compute `Fingerprint.forElement(type, packageName, resourceId, name, contentDesc)` internally when no `elementHash` is passed, OR add a deprecation error if `elementHash` is null. |
| High | `AvidGlobalID.kt:42` + `AvidLocalID.kt:50` | Both objects default `currentPlatform = Platform.ANDROID` with no static initializer that calls `setPlatform()`. If a caller forgets initialization (easy in a new module), all IDs across iOS/desktop are silently tagged as Android platform codes. AVID-A vs AVID-I collisions across devices will go undetected. | Make `currentPlatform` nullable and throw `IllegalStateException("setPlatform() has not been called")` on first `generate()` if null. OR use the KMP `expect/actual` pattern that `currentTimeMillis()` already uses in `AvidGenerator.kt`. |

### P2 Issues (Medium)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Medium | `Fingerprint.kt:86-96` | `deterministicHash` uses polynomial rolling hash with multiplier 31 (Java String hash), truncating to 8 hex chars (32 effective bits). Avalanche is poor for strings with long shared prefixes (e.g., `com.example.app.ButtonA` and `com.example.app.ButtonB` only differ at position 22). For 8-char hashes the birthday collision probability is 1 in ~4 billion per element pair — acceptable individually but with millions of elements across devices this becomes a statistical concern. | Consider FNV-1a or DJB2 which have better avalanche at low bit counts. The hash input structure (packageName|type|resourceId|name|contentDesc) partially mitigates this since most bits of differentiation appear early. Document the known collision probability. |
| Medium | `AvidGenerator.kt:51` | `AvidGenerator` (in `core/`) and `AvidGlobalID` / `AvidLocalID` (in root package) are three separate objects all generating IDs. The compact format (AvidGenerator) and the AVID-A-000001 sequence format (AvidGlobalID) are completely different ID systems with no cross-reference. This creates confusion about which system to use and when. | Add a single entry-point facade or a KDoc "ID System Guide" in the module README clarifying: use `Fingerprint.forElement()` + `AvidGenerator.generateCompact()` for UI elements; use `AvidGlobalID` for entities needing server sync. |
| Medium | `AvidGenerator.kt:6` | `Code-Reviewed-By: CCA` — CCA is not a person name. This header field is ambiguous and does not follow the project's Rule 7 naming convention. | Remove or replace with the reviewer's name. |

---

## Module 2: AVU

**Files:** 58 kt across commonMain / androidMain / iosMain / desktopMain / commonTest

### SCORE: 74 / 100 | HEALTH: YELLOW

---

### P0 Issues (Critical)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `AVUEncoder.kt:33` + `AVUEncoder.kt:68` | `CODE_ACCEPT_DATA = "ACD"` and `CODE_APP_CATEGORY_DB = "ACD"` — **both emit the same 3-letter code on the wire**. Any decoder receiving `ACD:...` cannot determine whether it is an accept-with-data response or an app category database header. This is a protocol wire-level ambiguity with no disambiguation mechanism. Previously documented in MEMORY.md (260220). | Rename one. Suggested: `CODE_APP_CATEGORY_DB = "ACB"`. Update all downstream: `AVUDecoder.parseAppCategoryDatabase()`, `AVUDecoder.isAppCategoryDatabase()`, and any `.acd` file generators. |
| High | `AVUDecoder.kt:34` | **Double-unescape bug**: `ParsedMessage.param(index)` calls `AVUEncoder.unescape(params[index])` at L34. However `params` is already populated at L59 via `parts.subList(2, parts.size).map { AVUEncoder.unescape(it) }`. The values in `params` are already unescaped strings. Calling `unescape()` again means a field originally containing a literal `%` (encoded as `%25`) will decode correctly to `%` on first pass, but a field containing `%25` literally (encoded as `%2525`) would decode to `%25` on first pass and then further decode to `%` on second pass — silently corrupting the value. | Remove the `unescape()` call from `ParsedMessage.param()` — the list is already unescaped. OR rename `paramOrNull()` to return raw and `param()` to return unescaped (current intention), but then do NOT pre-unescape in `parse()`. Pick one decode site and stick to it. |

### P1 Issues (High)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `AvuDslLexer.kt:105` | Off-by-one in header separator detection: `c == '-' && canMatch('-') && canMatchAt(current, '-')`. `canMatch('-')` checks `source[current] == '-'` (position after `c`). `canMatchAt(current, '-')` **also** checks `source[current] == '-'` — the same position. A 2-dash input `--` would match since both checks look at the same character. The third dash is never verified at `current + 1`. Previously documented in MEMORY.md (260220 AVU review). | Fix: `c == '-' && canMatch('-') && canMatchAt(current + 1, '-')`. Also verify bounds: `canMatchAt` should guard `index + 1 < source.length`. |
| High | `ExpressionEvaluator.kt:27` + `L101-102` | Comment at L27 states `and`/`or` are "non-short-circuit (both sides always evaluated)". But implementation at L101-102 uses Kotlin's `&&` and `||` which ARE short-circuit operators. The behavior contradicts the documented contract. Users writing `$x != null and $x.length > 0` would expect both sides to evaluate (per docs) but `&&` short-circuits on false left, which for this example is actually the safe behavior. However, for side-effect expressions, the mismatch is a bug if callers relied on non-short-circuit semantics as documented. | Either: (a) Fix the comment to say "short-circuit evaluation" (simplest), or (b) Implement actual non-short-circuit: evaluate both sides before combining. Choose consistently and document clearly. |
| High | `AvuCodeRegistry.kt:41` | `@Synchronized` is a JVM-only annotation. On Kotlin/Native (iOS, desktop) this is a **no-op**, making `AvuCodeRegistry` not thread-safe on non-JVM platforms. `register()`, `get()`, `getAll()` can race in multi-threaded contexts on iOS/desktop. | Replace `@Synchronized` with a `kotlinx.coroutines.sync.Mutex` (suspend-friendly) or use `atomicfu` if sync access is needed without coroutines. Since this is `object` singleton in commonMain, use `@ThreadLocal` + init patterns, or a platform-agnostic locking mechanism. |
| High | `MacroDslMigrator.kt:162` | `generateStep()` emits `"$indent@if ${step.condition}"` where `step.condition` is a raw `String` passed in by the caller. If this string contains a newline, the generated DSL will be syntactically invalid (the parser expects condition on same line as `@if`). If it contains unescaped quotes, the output is malformed. | Validate that `step.condition` contains no newlines before embedding. Alternatively, accept condition as an `AvuAstNode.Expression` rather than a raw string so it can be serialized safely. |

### P2 Issues (Medium)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Medium | `AVUDecoder.kt:368-371` | `parseVersion()` in `parseAppCategoryHeader` at line 367-370: the `version:` key in the YAML metadata section can be overridden by the `ACD:version:...` body line later. If both are present with different values, the body line wins (last assignment wins). Document this precedence explicitly, or use the header version consistently. | Prefer the body `ACD:` header line version (which is what happens now). Add a comment clarifying the resolution order. |
| Medium | `AvuDslLexer.kt:246` | `scanNumber()` assigns `text.toIntOrNull() ?: 0` for integers and `text.toDoubleOrNull() ?: 0.0` for floats. A malformed number (e.g., `123abc` — impossible from the scanner since it stops at non-digit, but boundary case) silently becomes 0. This masks scan errors as 0-values. | Use `text.toIntOrNull()` and emit `TokenType.ERROR` if null, rather than defaulting to 0. |
| Medium | `AvuHeader.kt:170-173` | `parse()` computes `headerEndIndex` as `lines.take(index + 1).sumOf { it.length + 1 }`. This assumes each line was separated by exactly one `\n` character. If the input used `\r\n` line endings, the computed byte offset will be off by one per line, causing `content.substring(bodyStart)` to mis-align. | Use `content.indexOf('\n', ...)` to find actual line end positions rather than summing reconstructed lengths. |
| Medium | `AvuV1Compat.kt:31` | `parseV1Message` splits on `:` naively without accounting for escaped colons. A v1 message field containing `:` (which is valid in URLs, for example) will be split incorrectly. The v1 format did not have an escape mechanism, but callers of this migration utility may pass current messages. | Document that v1 messages must not contain literal colons in field values, or apply the escape mechanism before splitting. |
| Medium | `AVUDecoder.kt:600-604` | `parseHandover()` resolves the `module` field with double-lookup: `headerData.metadata["module"] ?: headerData.sections["module"]?.firstOrNull() ?: ""`. This is a fallback chain across two different storage mechanisms for the same field. Callers who put `module:` as a top-level key (not under `metadata:`) will only be found by the second branch. The resolution is fragile and undocumented. | Normalize `module` into `HeaderData.metadata` during `AvuHeader.parse()`, similar to how `schema`, `version`, and `project` are parsed as top-level keys. |

### P3 Issues (Low)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Low | `AVUEncoder.kt:1` | `@author Augmentalis Engineering` — while not "Claude AI", the project convention per MEMORY.md and Rule 7 is to use "Manoj Jhawar" or omit. This applies to `AVUDecoder.kt` and `AvuEscape.kt` and `AvuHeader.kt` and `AvuCodeInfo.kt` and `AvuCodeRegistry.kt` and `AvuHandoverCodes.kt`. | Replace `@author Augmentalis Engineering` with `Manoj Jhawar` or remove the author field. |
| Low | `AvuDslParser.kt:4` | Stale import comment: `from [com.augmentalis.voiceoscore.dsl.lexer.AvuDslLexer]` — wrong package path. The actual lexer package is `com.avanues.avu.dsl.lexer`. | Fix the KDoc reference to the correct package. |
| Low | `SandboxConfig.kt` | `maxExecutionTimeMs = 10_000` for DEFAULT. The sandbox clock check in `ExecutionContext.incrementStep()` calls `currentTimeMillis()` on every single step. For 1,000 steps this is 1,000 platform time calls. On iOS `currentTimeMillis()` calls `NSDate().timeIntervalSince1970` which allocates an NSDate object per call. | Cache `currentTimeMillis()` result and only re-check every N steps (e.g., every 50 steps). |

---

## Module 3: AVACode

**Files:** 10 kt in commonMain / jvmMain

### SCORE: 80 / 100 | HEALTH: GREEN (with caution)

---

### P1 Issues (High)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `WorkflowInstance.kt:94-95` + `WorkflowInstance.kt:155-158` | `WorkflowInstance` is a `data class` containing `MutableMap<String, StepState>` (`stepStates`) and `MutableList<WorkflowTransition>` (`history`). `next()` directly mutates these shared collections **before** returning a `copy()` of the instance. The copy shares the same mutable references. Any code holding a reference to the original instance will silently observe the mutations. `data class` semantics imply value semantics — mutable shared state breaks this contract. | Either: (a) Make `WorkflowInstance` a regular `class` with explicit mutation, or (b) Make `copy()` deep-copy the mutable collections, or (c) Change `stepStates` and `history` to immutable types and rebuild on each transition (the pure-functional approach). Option (c) is recommended for correctness. |
| High | `WorkflowInstance.kt:94` + `WorkflowPersistence.kt:30+93` | `System.currentTimeMillis()` is called in `WorkflowInstance.next()`, `back()`, `skip()`, `jumpTo()` and `WorkflowPersistence.serialize()` / `checkpoint()`. `System` is a JVM API unavailable on Kotlin/Native. This code is in `commonMain` but will fail to compile for iOS/desktop targets if they ever depend on `AVACode`. | Replace with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()` which is KMP-compatible, or use the same `expect/actual currentTimeMillis()` pattern already established in `AvidGenerator.kt`. |

### P2 Issues (Medium)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Medium | `FormBinding.kt:136-138` | `commit()` casts `initialData` to `MutableMap` and clears it: `(initialData as? MutableMap)?.clear()`. `initialData` is declared as `val` of type `Map<String, Any?>`. The cast succeeds at runtime only because `toMap()` internally returns a `LinkedHashMap`. This is an implementation detail of the stdlib. If Kotlin ever returns a truly immutable map from `toMap()`, this silently becomes a no-op and `hasChanges()` breaks permanently. | Change `private val initialData: Map<String, Any?> = data.toMap()` to `private val initialData: MutableMap<String, Any?> = data.toMutableMap()`. Then `commit()` becomes `initialData.clear(); initialData.putAll(data)`. |
| Medium | `FormBinding.kt:29-46` | `FormBinding.set()` throws `ValidationException` on invalid input. This means callers must wrap every field set in a try-catch, and partial batch updates via `setData()` leave the binding in an inconsistent state (some fields set, some not). | Return a `Result<Unit, ValidationException>` or a list of errors from `set()`, and make `setData()` atomic (validate all first, then apply all). |
| Medium | `DatabaseSchema.kt` | `ColumnDefinition` has a `unique: Boolean` field that is stored but never used in `toSQL()`. The unique constraint is expressed at the table level via the `constraints` list only. The field in `ColumnDefinition` is dead code that could mislead maintainers. | Either emit `UNIQUE` as an inline column constraint in `ColumnDefinition.toSQL()` and remove from the `constraints` list, or remove the `unique` field from `ColumnDefinition` entirely. |
| Medium | `ValidationRule.kt:167-191` | `MinDate` / `MaxDate` compare dates using string comparison (`value >= minDate`). This is correct only for ISO 8601 dates in `YYYY-MM-DD` format with zero-padded components. Non-standard date strings (e.g., `2026-2-5`) would compare incorrectly. There is no format validation before comparison. | Add ISO 8601 format validation (regex check) before the comparison, and document the expected format in KDoc. |

### P3 Issues (Low)

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Low | `FieldDefinition.kt:26` | The `id` regex `[a-z][a-z0-9_]*` requires lowercase start and allows underscores but not hyphens. This is a design constraint, but the error message ("lowercase alphanumeric with underscores") should clarify the start-with-letter requirement. | Improve error: "Field ID must start with a lowercase letter and contain only lowercase letters, digits, and underscores". |
| Low | `FileIO.jvm.kt:12` | `actual object FileIO` — the `expect` declaration is missing from the file listing (no `commonMain` counterpart was found in the glob). If `commonMain` has an `expect object FileIO`, this is fine. If `jvmMain` is the only target, `actual` is unnecessary and misleading. | Verify `expect object FileIO` exists in commonMain. If this is jvmMain-only functionality, remove the `actual` keyword and use a regular object. |

---

## Cross-Cutting Recommendations

1. **ACD collision** (P0 — AVU): Fix `CODE_APP_CATEGORY_DB = "ACB"` immediately. This is a wire protocol bug that silently misroutes messages. Tracked in MEMORY.md since 260220 — must be resolved before any multi-format decoder deployment.
2. **Lexer `--` false trigger** (P1 — AVU): Fix `canMatchAt(current + 1, '-')` in `AvuDslLexer.kt:105`. A two-dash comment or argument inadvertently enters header separator mode.
3. **Double-unescape in AVUDecoder** (P0 — AVU): `ParsedMessage.param()` double-unescapes already-decoded values. Fields containing `%` literals will be corrupted.
4. **AVID non-determinism** (P1 — AVID): `generateCompact()` uses a random hash when no `elementHash` is passed, making the same UI element produce a new AVID on every scan. This destroys the purpose of deterministic fingerprinting for persistent voice command storage.
5. **JVM API in commonMain** (P1 — AVACode): `System.currentTimeMillis()` blocks KMP expansion beyond JVM targets. Replace now before iOS/desktop consumers are added.
6. **@Synchronized on K/N** (P1 — AVU): `AvuCodeRegistry` is not thread-safe on iOS/desktop targets.
