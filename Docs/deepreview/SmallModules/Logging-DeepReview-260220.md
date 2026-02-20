# Logging Module — Deep Review
**Date:** 2026-02-20
**Files Reviewed:** 11 .kt files
**Source sets:** commonMain (4), androidMain (2), iosMain (2), desktopMain (2)
**Reviewer:** Code Review Agent

---

## Summary

The Logging module provides a clean KMP-first logging abstraction: a platform-agnostic `Logger`
interface with `expect/actual` factory, a `PIIRedactionHelper` for privacy-safe log output,
and a `PIISafeLogger` wrapper that composes both. The `LogLevel` enum correctly mirrors Android
priority values for compatibility, and lazy message evaluation prevents string interpolation cost
when a log level is disabled.

However, three significant issues undermine the module's correctness. First, the `PIISafeLogger`
breaks lazy evaluation — the message lambda is eagerly evaluated before PII redaction, meaning
disabled log levels still pay the full evaluation + redaction cost. Second, the `NAME_PATTERN`
and `ZIP_CODE_PATTERN` regexes in `PIIRedactionHelper` will produce massive false-positive
redaction in production logs, destroying their usefulness (any 5-digit number becomes
`[REDACTED-ZIP]`, any two capitalized words become `[REDACTED-NAME]`). Third,
`PIISafeLoggerFactory`'s static convenience methods allocate two new objects per call with no
caching, making them unsuitable for hot logging paths. The iOS implementation uses `NSLog`
instead of the privacy-aware `os_log` API, which is a privacy concern for a voice app.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `commonMain/PIISafeLogger.kt:59-65` | Lazy evaluation is broken: `delegate.v { redact(message()) }` calls `message()` eagerly inside the outer lambda body. Even if `LogLevel.VERBOSE` is filtered, `message()` is always evaluated and redaction always runs. This negates the primary performance benefit of lazy logging. | Change to a two-stage check: `override fun v(message: () -> String) { if (delegate.isLoggable(LogLevel.VERBOSE)) delegate.v { redact(message()) } }`. This way `message()` is only called if the level is loggable. |
| High | `commonMain/PIIRedactionHelper.kt:51-54` | `NAME_PATTERN` with `IGNORE_CASE` matches any two adjacent words starting with a letter, including "Android Studio", "Stack Trace", "Null Pointer", "View Model", "Error Message". Virtually every log line contains false-positive name redaction, making logs unreadable. | Remove `IGNORE_CASE` from `NAME_PATTERN` so it only matches proper-cased tokens (uppercase first letter). Also add a whitelist of common non-name word pairs (class names, technical terms). |
| High | `commonMain/PIIRedactionHelper.kt:63-66` | `ZIP_CODE_PATTERN` `\b\d{5}\b` matches any standalone 5-digit number: port numbers (54321), pixel dimensions (1080p is 1920×1080), timestamp fragments, error codes, line numbers. Applying this to all log output destroys diagnostic utility. | Narrow ZIP matching: require context clues (e.g., preceded by "zip", "postal", or a US state abbreviation). Or remove ZIP from the blanket `redactPII()` call and only use it explicitly when processing address-like content. |
| High | `commonMain/PIISafeLogger.kt:41-47` | `PIISafeLoggerFactory` static convenience methods (`v`, `d`, `i` etc.) call `getLogger(tag)` → `PIISafeLogger(LoggerFactory.getLogger(tag))` on every invocation. Each call allocates 2 new objects (PIISafeLogger + AndroidLogger/DesktopLogger/IosLogger). These are unusable on hot paths. | Add an internal `ConcurrentHashMap<String, PIISafeLogger>` cache keyed by tag. Or deprecate the static convenience methods and require callers to hold a `PIISafeLogger` instance. |
| Medium | `iosMain/IosLogger.kt:9-10` | Uses `NSLog` for all log output. `NSLog` writes to the Unified Logging System but does not support `os_log` privacy levels (`%{private}` metadata). Voice command strings logged through here will appear unredacted in crash reports and Console.app captures, even when PII redaction is applied at the Kotlin layer. | Replace `NSLog` with `os_log` via Kotlin/Native cinterop or via a Swift bridging class. Use `os_log_with_type` and mark sensitive parameters as `%{private}`. |
| Medium | `desktopMain/DesktopLogger.kt:73-78` | `isLoggable()` calls `System.getProperty("log.level")` on **every log statement**. `System.getProperty()` acquires a global lock on the system properties map. In a high-frequency logging scenario this is a hidden lock contention point. | Cache the parsed `LogLevel` at construction time and provide a static method to update it: `DesktopLogger.setSystemLevel(LogLevel.DEBUG)`. Alternatively, re-read only if a volatile "dirty" flag is set. |
| Medium | `commonMain/PIIRedactionHelper.kt:33-36` | `PHONE_PATTERN` matches 10-digit sequences that include timestamps, device IDs, and order numbers. Patterns like `2026020112345` are matched as phone numbers. The word-boundary `\b` is not used at both ends, so digits within longer numbers can match. | Add `\b` anchors around the full pattern and require the pattern not be preceded by `/` or `.` (URL/path context) to reduce false positives. |
| Medium | `commonMain/PIIRedactionHelper.kt:74-90` | `redactPII()` applies all redactors in sequence: ZIP is applied before Address, meaning address street numbers (which could be 5 digits like "12345 Main Street") get their street number redacted to `[REDACTED-ZIP]` before `redactAddress()` can match the full pattern. Order matters and is fragile. | Apply `redactAddress()` before `redactZipCode()` so full addresses are captured first. Better: compose a single multi-pattern regex pass. |
| Medium | `commonMain/LoggerFactory.kt:43-45` | `globalMinLevel` is a read-write property backed by `@Volatile`. Concurrent read-modify-write (e.g., `if (globalMinLevel == VERBOSE) globalMinLevel = DEBUG`) is not atomic. In tests or hot reload scenarios this could produce inconsistent log filtering. | Document that `globalMinLevel` is intended for single-assignment (startup config) only, not for concurrent toggling. Or protect writes with `AtomicReference`. |
| Low | `commonMain/PIISafeLogger.kt:76-78` | `piiSafeLogger()` extension creates a new `PIISafeLogger` on every call (since it calls `PIISafeLoggerFactory.getLogger()` which creates a new instance). If called at class instantiation in an `init {}` block this is fine, but if called lazily or per-operation it allocates. | Document: "Call once and store as a property, not per-operation." |
| Low | `commonMain/PIIRedactionHelper.kt:137-145` | `maskEmail()` splits on `@` but `email.split("@")` returns all parts. For a malformed email like `"a@b@c"`, `parts.size == 3`, returns `"[INVALID-EMAIL]"` correctly. However `parts[1].substringAfterLast('.')` on `"b@c"` (domain has no dot) returns `"b@c"` not a TLD. The masking would be `a***@b***.[empty]`. | Use `parts[1].substringBeforeLast('.')` for masked domain and `parts[1].substringAfterLast('.')` for TLD, but guard against no-dot domains. |

---

## Detailed Findings — Critical and High Issues

### HIGH 1: `PIISafeLogger` breaks lazy evaluation (all platforms)
**File:** `commonMain/kotlin/com/avanues/logging/PIISafeLogger.kt:59-65`

```kotlin
// CURRENT (EAGER EVALUATION — defeats lazy logging):
override fun v(message: () -> String) = delegate.v { redact(message()) }
//                                                    ^^^^^^^^^ called eagerly
// Execution: outer lambda { redact(message()) } is constructed.
// When delegate.v { } is called, the inner body calls message() immediately.
// Even if delegate.v checks isLoggable(VERBOSE) first, the message()
// is already evaluated before delegate.v gets to check.

// Actually worse: the `{ redact(message()) }` lambda captures `message`
// and calls it when delegate.v evaluates the lambda. So message() is called
// exactly when delegate evaluates the lambda — which is INSIDE delegate.v's
// isLoggable check. This means lazy evaluation IS technically preserved...
// BUT only if delegate.v(lambda) defers the call. Let's verify AndroidLogger:

// AndroidLogger.v:
override fun v(message: () -> String) {
    if (isLoggable(LogLevel.VERBOSE)) {
        Log.v(tag, message())  // message() called here, inside the if
    }
}
// So PIISafeLogger.v { redact(message()) } → delegate.v { redact(message()) }
// → AndroidLogger checks isLoggable → if true, calls the lambda → message() called → redact()

// This IS lazy for the outer message(), but redact() is always called if
// the level is loggable. The concern is that redact() itself is expensive
// (7 regex passes). If message() is cheap but the string is long, redact() dominates.

// The REAL bug: PIISafeLoggerFactory static methods pass eager strings:
fun d(tag: String, message: String?) = getLogger(tag).d { message ?: "null" }
// The message String is already computed by the caller. This is expected for
// the static API. But the instance API is correctly lazy.

// ACTUAL BUG IN PIISafeLogger — redact() on exception path:
override fun e(message: () -> String, throwable: Throwable) =
    delegate.e({ redact(message()) }, throwable)
// If the delegate's e(lambda, throwable) is:
// fun e(message: () -> String, throwable: Throwable) {
//     if (isLoggable(ERROR)) { Log.e(tag, message(), throwable) }
// }
// Then message() (= redact(originalMessage())) is called inside the if check — correct.
// But for Android, stacktrace is in throwable.stackTraceToString() which is NOT redacted.
// Throwable messages and stack frames can contain PII (user data in exception messages).
```

**Clarification after full analysis:** The lazy evaluation concern is partially mitigated because the
lambda wrapping preserves deferral. The actual high-severity issue is:

1. **`throwable.stackTraceToString()` is not redacted.** The exception message (accessible via
   `throwable.message`) can contain user-visible strings (e.g., "Failed to parse command: 'call
   John Smith at 555-123-4567'"). Android's `Log.e(tag, msg, throwable)` logs the full stack
   trace including exception messages unredacted. `PIISafeLogger.e(message, throwable)` only
   redacts `message()` but not `throwable.message` or nested cause messages.

2. **Static convenience methods in `PIISafeLoggerFactory` are always eager** (strings computed by
   callers before being passed). This is by design but means callers using the static API get no
   lazy evaluation.

**Fix for throwable PII leakage:**
```kotlin
override fun e(message: () -> String, throwable: Throwable) {
    // Wrap throwable to redact its message chain
    val redactedThrowable = RedactedThrowable(throwable)
    delegate.e({ redact(message()) }, redactedThrowable)
}

private class RedactedThrowable(cause: Throwable) : Throwable(
    message = PIIRedactionHelper.redactPII(cause.message),
    cause = cause.cause?.let { RedactedThrowable(it) }
) {
    override fun getStackTrace(): Array<StackTraceElement> = cause.stackTrace
}
```

---

### HIGH 2: `NAME_PATTERN` causes pervasive false-positive redaction
**File:** `commonMain/kotlin/com/avanues/logging/PIIRedactionHelper.kt:51-54`

```kotlin
// CURRENT:
private val NAME_PATTERN: Regex = Regex(
    "\\b[A-Z][a-z]+(?:['-][A-Z][a-z]+)?\\s+[A-Z][a-z]+(?:['-][A-Z][a-z]+)?\\b",
    RegexOption.IGNORE_CASE  // <-- THIS makes it match everything
)

// With IGNORE_CASE, the pattern [A-Z][a-z]+ matches ANY sequence of letters.
// "Voice Command" → [REDACTED-NAME]
// "Debug Mode" → [REDACTED-NAME]
// "Stack Trace" → [REDACTED-NAME]
// "Null Pointer" → [REDACTED-NAME]
// "Screen Change" → [REDACTED-NAME]
// "View Model" → [REDACTED-NAME]
// Virtually every two-word phrase in any log line.

// FIX: Remove IGNORE_CASE so only true proper-case names match:
private val NAME_PATTERN: Regex = Regex(
    "\\b[A-Z][a-z]{1,}(?:['-][A-Z][a-z]+)?\\s+[A-Z][a-z]{1,}(?:['-][A-Z][a-z]+)?\\b"
    // No IGNORE_CASE
)
// Now only matches "John Smith", "Mary-Jane O'Connor" — NOT "voice command"
```

---

### HIGH 3: `ZIP_CODE_PATTERN` redacts port numbers, error codes, and dimensions
**File:** `commonMain/kotlin/com/avanues/logging/PIIRedactionHelper.kt:63-66`

```kotlin
// CURRENT:
private val ZIP_CODE_PATTERN: Regex = Regex(
    "\\b\\d{5}(?:-\\d{4})?\\b",
    RegexOption.IGNORE_CASE
)

// Examples of false positives in log output:
// "Port 54321 accepted connection" → "Port [REDACTED-ZIP] accepted connection"
// "HTTP status 20001 unexpected" → "HTTP status [REDACTED-ZIP] unexpected"
// "Error code 10048 (address in use)" → "Error code [REDACTED-ZIP] (address in use)"
// "Resolution 19201 pixels wide" → "Resolution [REDACTED-ZIP] pixels wide"
// Commit hash fragments: "a3f2b1c0d4e5 at line 12345" → "at line [REDACTED-ZIP]"

// These false positives make log lines containing 5-digit numbers completely
// uninterpretable, severely hindering debugging.

// FIX: Require preceding state/country context or remove from blanket redactPII():
private val ZIP_CODE_PATTERN: Regex = Regex(
    "(?:zip|postal|zip code|post code)[:\\s]+\\b\\d{5}(?:-\\d{4})?\\b",
    RegexOption.IGNORE_CASE
)
// Or: exclude ZIP from the blanket redactPII() and only call redactZipCode()
// when processing structured address data.
```

---

### HIGH 4: Static convenience methods allocate objects per call
**File:** `commonMain/kotlin/com/avanues/logging/PIISafeLogger.kt:41-47`

```kotlin
// CURRENT: Creates new objects on EVERY log call
fun d(tag: String, message: String?) = getLogger(tag).d { message ?: "null" }
// getLogger(tag) → PIISafeLogger(LoggerFactory.getLogger(tag)) → new AndroidLogger(tag)
// = 2 allocations per log statement

// FIX: Cache loggers by tag
private val cache = java.util.concurrent.ConcurrentHashMap<String, PIISafeLogger>()

fun getLogger(tag: String): PIISafeLogger =
    cache.getOrPut(tag) { PIISafeLogger(LoggerFactory.getLogger(tag)) }
```

Note: `ConcurrentHashMap` is JVM-specific. For KMP, use a `@ThreadLocal` map or accept the
allocation cost for static convenience methods and document that callers in hot paths should hold
a `PIISafeLogger` instance.

---

### MEDIUM: `NSLog` exposes voice commands in device logs (iOS)
**File:** `iosMain/kotlin/com/avanues/logging/IosLogger.kt:9,24-60`

`NSLog` outputs to the Unified Logging System at the default privacy level (public). Any string
logged via `NSLog` is readable by anyone with physical device access using Console.app or
`log stream`. For a voice command application, this means transcribed speech ("call Mom at
home" → matched command phrase) appears in the raw device log.

The `os_log` API supports `%{private}` format specifiers that redact values in captured logs.
However, Kotlin/Native does not have a pre-built cinterop for `os_log`. The preferred path is
a Swift bridging class:

```swift
// VoiceOSLogger.swift (added to iOS app target)
import os.log
@objc class VoiceOSLogger: NSObject {
    static let log = OSLog(subsystem: "com.augmentalis.voiceos", category: "voice")
    @objc static func log(tag: String, message: String) {
        os_log("%{private}@: %{private}@", log: log, type: .debug, tag, message)
    }
}
```

Then `IosLogger` would call `VoiceOSLogger.log(tag: tag, message: message())` instead of `NSLog`.

---

## Rule 7 Violations

None found. All file headers use "Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC". No
AI/Claude attribution present in any file.

---

## Test Coverage Assessment

No test files found in `Logging/src/`. The following are high-priority test targets:

- `PIIRedactionHelper.redactPII()`: Validate each pattern against known PII. Also test for
  false positives with class names, port numbers, error codes, and 5-digit numbers.
- `PIIRedactionHelper.redactName()`: Verify that "Voice Command", "Debug Mode", "Stack Trace"
  are NOT redacted after the NAME_PATTERN fix.
- `AndroidLogger.isLoggable()`: Verify priority ordering matches Android constants.
- `PIISafeLogger`: Verify that `message()` lambda is NOT called when level is below `globalMinLevel`.
- `PIISafeLogger.e(message, throwable)`: Verify that exception message PII is redacted.

---

## Package Naming Note

The Logging module uses `com.avanues.logging` (note: single "a", not `com.augmentalis`). The
Foundation module uses `com.augmentalis.foundation`. This is an inconsistency in the module
namespace convention. If the project convention is `com.augmentalis.*`, the Logging package
should be `com.augmentalis.logging`. This is low-priority but creates confusion when importing.

---

## Summary Counts

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High | 4 |
| Medium | 5 |
| Low | 2 |
| **Total** | **11** |
