# Developer Manual - Chapter 78: Handler Utilities

**Module:** `Modules/VoiceOSCore`
**Package:** `com.augmentalis.voiceoscore`
**File:** `HandlerUtilities.kt`

---

## Overview

HandlerUtilities provides common extensions and a DSL for reducing boilerplate in VoiceOS command handlers. These utilities extract patterns that were duplicated across 18+ handlers.

## Key Components

### 1. String Extensions

```kotlin
// Normalize command text for matching
fun String.normalizeCommand(): String = this.lowercase().trim()

// Extract target from command with prefixes
fun String.extractTarget(vararg prefixes: String): String

// Check if string matches any pattern
fun String.matchesAny(vararg patterns: String): Boolean
```

### 2. Result Extensions

```kotlin
// Convert Boolean to HandlerResult
fun Boolean.toHandlerResult(
    successMessage: String,
    failureMessage: String,
    data: Map<String, Any?> = emptyMap()
): HandlerResult
```

### 3. Safe Execution

```kotlin
// Wrap handler operations with error handling
inline fun runHandlerCatching(
    operationName: String,
    block: () -> HandlerResult
): HandlerResult
```

### 4. Command Router DSL

The command router provides a clean DSL for matching commands:

```kotlin
fun commandRouter(command: String, block: CommandRouter.() -> Unit): HandlerResult

class CommandRouter(private val command: String) {
    fun on(vararg patterns: String, handler: () -> HandlerResult)
    fun onPrefix(prefix: String, handler: (target: String) -> HandlerResult)
    fun onRegex(pattern: Regex, handler: (MatchResult) -> HandlerResult)
    fun otherwise(handler: () -> HandlerResult)
}
```

---

## Usage Examples

### Before (Traditional Pattern)

```kotlin
override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
    val normalizedAction = command.phrase.lowercase().trim()

    return when {
        normalizedAction == "scroll up" || normalizedAction == "page up" -> {
            try {
                val result = executor.scrollUp()
                if (result) {
                    HandlerResult.Success(message = "Scrolled up")
                } else {
                    HandlerResult.failure("Could not scroll up")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scrolling up", e)
                HandlerResult.failure("Error: ${e.message}")
            }
        }
        normalizedAction == "scroll down" || normalizedAction == "page down" -> {
            // Similar boilerplate...
        }
        else -> HandlerResult.notHandled()
    }
}
```

### After (Using Handler Utilities)

```kotlin
override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult =
    commandRouter(command.phrase) {
        on("scroll up", "page up") {
            executor.scrollUp().toHandlerResult(
                "Scrolled up",
                "Could not scroll up - no scrollable content"
            )
        }

        on("scroll down", "page down") {
            executor.scrollDown().toHandlerResult(
                "Scrolled down",
                "Could not scroll down - already at bottom"
            )
        }

        onPrefix("scroll to") { target ->
            runHandlerCatching("scroll to element") {
                executor.scrollToElement(target).toHandlerResult(
                    "Scrolled to $target",
                    "Could not find element: $target"
                )
            }
        }

        otherwise { HandlerResult.notHandled() }
    }
```

---

## Patterns Extracted

| Pattern | Usage Count | Utility |
|---------|-------------|---------|
| Command normalization | 18+ handlers | `normalizeCommand()` |
| Boolean to Result | 15+ handlers | `toHandlerResult()` |
| Error wrapping | 18+ handlers | `runHandlerCatching()` |
| Multi-pattern match | 12+ handlers | `matchesAny()` |
| Target extraction | 8+ handlers | `extractTarget()` |
| Command routing | 18+ handlers | `commandRouter` DSL |

---

## Benefits

### Code Reduction
- NavigationHandler: 127 → 82 lines (~35% reduction)
- Consistent error handling across all handlers
- Reduced copy-paste errors

### Readability
- Declarative command matching
- Clear intent in handler code
- Self-documenting DSL syntax

### Maintainability
- Single source of truth for common patterns
- Easy to add new utilities
- Centralized logging and error handling

---

## Migration Guide

### Step 1: Import Utilities

```kotlin
import com.augmentalis.voiceoscore.normalizeCommand
import com.augmentalis.voiceoscore.toHandlerResult
import com.augmentalis.voiceoscore.commandRouter
import com.augmentalis.voiceoscore.runHandlerCatching
```

### Step 2: Replace `when` Blocks

Replace nested `when` blocks with `commandRouter`:

```kotlin
// Before
return when {
    command == "foo" -> handleFoo()
    command == "bar" -> handleBar()
    else -> HandlerResult.notHandled()
}

// After
return commandRouter(command) {
    on("foo") { handleFoo() }
    on("bar") { handleBar() }
    otherwise { HandlerResult.notHandled() }
}
```

### Step 3: Use Result Extensions

Replace boolean-to-result conversions:

```kotlin
// Before
if (result) {
    HandlerResult.Success(message = "Done")
} else {
    HandlerResult.failure("Failed")
}

// After
result.toHandlerResult("Done", "Failed")
```

---

## File Location

```
Modules/VoiceOSCore/
└── src/commonMain/kotlin/com/augmentalis/voiceoscore/
    └── HandlerUtilities.kt
```

---

## Related Documentation

- [Chapter 77: Logging Module Architecture](Developer-Manual-Chapter77-Logging-Module-Architecture.md)
- [Chapter 79: WebAvanue Repository Architecture](Developer-Manual-Chapter79-WebAvanue-Repository-Architecture.md)
