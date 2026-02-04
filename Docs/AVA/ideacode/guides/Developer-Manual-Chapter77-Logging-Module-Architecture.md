# Chapter 77: Logging Module Architecture

**Date:** 2026-02-03
**Author:** VOS4 Development Team
**Status:** Active

---

## Overview

This chapter documents the consolidated KMP Logging module (`Modules/Logging`) which provides cross-platform logging infrastructure with PII-safe logging capabilities.

## Module Summary

| Aspect | Details |
|--------|---------|
| **Module Path** | `Modules/Logging` |
| **Package** | `com.avanues.logging` |
| **Platforms** | Android, iOS, Desktop (JVM) |
| **Lines of Code** | ~836 |

---

## Architecture

### Module Structure

```
Modules/Logging/
├── build.gradle.kts                    # KMP build configuration
├── src/
│   ├── commonMain/kotlin/com/avanues/logging/
│   │   ├── Logger.kt                   # Cross-platform logger interface
│   │   ├── LoggerFactory.kt            # Platform factory (expect/actual)
│   │   ├── LogLevel.kt                 # Log level enumeration
│   │   ├── PIIRedactionHelper.kt       # PII detection and redaction
│   │   └── PIISafeLogger.kt            # Auto-redacting logger wrapper
│   ├── androidMain/kotlin/com/avanues/logging/
│   │   ├── AndroidLogger.kt            # android.util.Log implementation
│   │   └── LoggerFactory.kt            # Android actual
│   ├── iosMain/kotlin/com/avanues/logging/
│   │   ├── IosLogger.kt                # NSLog implementation
│   │   └── LoggerFactory.kt            # iOS actual
│   └── desktopMain/kotlin/com/avanues/logging/
│       ├── DesktopLogger.kt            # System.out/err with ANSI colors
│       └── LoggerFactory.kt            # Desktop actual
```

---

## Core Components

### 1. Logger Interface

Cross-platform logging interface with lazy evaluation for performance:

```kotlin
interface Logger {
    fun v(message: () -> String)     // Verbose
    fun d(message: () -> String)     // Debug
    fun i(message: () -> String)     // Info
    fun w(message: () -> String)     // Warning
    fun e(message: () -> String)     // Error
    fun e(message: () -> String, throwable: Throwable)
    fun wtf(message: () -> String)   // Assert (critical)
    fun isLoggable(level: LogLevel): Boolean
}
```

**Key Features:**
- Lazy evaluation: Message lambda only called if logging enabled
- Exception support with stack traces
- Level filtering via `isLoggable()`

### 2. LogLevel Enumeration

```kotlin
enum class LogLevel(val priority: Int) {
    VERBOSE(2),  // Most detailed
    DEBUG(3),    // Development diagnostics
    INFO(4),     // General information
    WARN(5),     // Potential issues
    ERROR(6),    // Recoverable errors
    ASSERT(7)    // Critical failures
}
```

Priority values match Android's `Log` levels for compatibility.

### 3. LoggerFactory (Expect/Actual Pattern)

```kotlin
// commonMain - expect declaration
expect object LoggerFactory {
    fun getLogger(tag: String): Logger
}

// Extension for reified type
inline fun <reified T> LoggerFactory.getLogger(): Logger =
    getLogger(T::class.simpleName ?: "Unknown")
```

**Platform Implementations:**

| Platform | Implementation | Backend |
|----------|----------------|---------|
| Android | `AndroidLogger` | `android.util.Log` |
| iOS | `IosLogger` | `NSLog` |
| Desktop | `DesktopLogger` | `System.out/err` + ANSI colors |

---

## PII-Safe Logging

### PIIRedactionHelper

Automatically detects and redacts Personally Identifiable Information:

| PII Type | Pattern | Replacement |
|----------|---------|-------------|
| Email | `user@domain.com` | `[REDACTED-EMAIL]` |
| Phone | `(555) 123-4567` | `[REDACTED-PHONE]` |
| Credit Card | `4111-1111-1111-1111` | `[REDACTED-CC]` |
| SSN | `123-45-6789` | `[REDACTED-SSN]` |
| ZIP Code | `12345-6789` | `[REDACTED-ZIP]` |
| Address | `123 Main St` | `[REDACTED-ADDRESS]` |
| Name | `John Smith` | `[REDACTED-NAME]` |

**Usage:**

```kotlin
// Full redaction
val safe = PIIRedactionHelper.redactPII("Contact: john@email.com at 555-123-4567")
// Result: "Contact: [REDACTED-EMAIL] at [REDACTED-PHONE]"

// Partial masking (for debugging)
val masked = PIIRedactionHelper.maskEmail("john.doe@example.com")
// Result: "j***@e***.com"

// Detection
if (PIIRedactionHelper.containsPII(userInput)) {
    // Handle sensitive data
}
```

### PIISafeLogger

Wrapper that automatically redacts PII from all log messages:

```kotlin
// Get PII-safe logger
val log = PIISafeLoggerFactory.getLogger("MyClass")

// All messages automatically redacted
log.d { "User input: $userEmail" }  // Email automatically redacted
log.e { "Error processing: $userData" }

// Extension functions
class MyClass {
    private val log = piiSafeLogger()  // Auto-detects class name
}
```

---

## Usage Examples

### Basic Logging

```kotlin
class UserRepository {
    private val logger = LoggerFactory.getLogger("UserRepository")
    // Or: private val logger = LoggerFactory.getLogger<UserRepository>()

    fun fetchUser(id: String) {
        logger.d { "Fetching user: $id" }

        try {
            // ... fetch logic
            logger.i { "User fetched successfully" }
        } catch (e: Exception) {
            logger.e({ "Failed to fetch user: $id" }, e)
        }
    }
}
```

### PII-Safe Logging

```kotlin
class AuthManager {
    private val log = PIISafeLoggerFactory.getLogger("AuthManager")

    fun authenticate(email: String, password: String) {
        // Email automatically redacted in logs
        log.d { "Authenticating user: $email" }
        // Output: "Authenticating user: [REDACTED-EMAIL]"
    }
}
```

### Configuring Log Level

```kotlin
// Set global minimum log level
globalMinLevel = LogLevel.INFO  // Only INFO and above will be logged

// Desktop: Can also use system property
// -Dlog.level=DEBUG
```

---

## Platform-Specific Behavior

### Android

- Uses `android.util.Log` for native integration with Logcat
- Respects both `globalMinLevel` and `Log.isLoggable()` settings
- Tag length truncated to 23 characters (Android limitation)

### iOS

- Uses `NSLog` for native integration with Console.app
- All messages prefixed with `[$tag] LEVEL: message`
- Can be filtered in Console.app by tag or level

### Desktop

- Uses `System.out` for INFO and below
- Uses `System.err` for WARN and above
- ANSI color coding:
  - VERBOSE: Gray
  - DEBUG: Blue
  - INFO: Green
  - WARN: Yellow
  - ERROR: Red
  - WTF: Magenta

---

## Migration Guide

### From voiceos-logging

If migrating from the deprecated `com.augmentalis.voiceos.logging`:

```kotlin
// Old
import com.augmentalis.voiceos.logging.Logger
import com.augmentalis.voiceos.logging.LoggerFactory

// New
import com.avanues.logging.Logger
import com.avanues.logging.LoggerFactory
```

### From Custom Logging

Replace direct `android.util.Log` calls:

```kotlin
// Old (Android-only)
Log.d(TAG, "Message: $data")

// New (Cross-platform)
private val logger = LoggerFactory.getLogger("MyClass")
logger.d { "Message: $data" }  // Lazy evaluation
```

---

## Archived Modules

The following logging implementations have been archived (replaced by `Modules/Logging`):

| Archived Path | Archive Location |
|---------------|------------------|
| `Modules/AVAMagic/Core/voiceos-logging` | `archive/voiceos-logging-260202/` |
| `Modules/VoiceOS/core/voiceos-logging` | `archive/VoiceOS-CoreLibs-270127/` |

---

## Best Practices

1. **Use lazy evaluation**: Always use lambda syntax `{ }` for message strings
2. **Use PII-safe logging**: For user-facing data, use `PIISafeLoggerFactory`
3. **Keep tags short**: Android limits tags to 23 characters
4. **Log context**: Include relevant IDs/context in error logs
5. **Don't log sensitive data**: Even with redaction, avoid logging passwords/tokens

---

## Dependencies

The Logging module has **no external dependencies** - it's self-contained using only Kotlin stdlib and platform APIs.

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":Modules:Logging"))
}
```

---

## Related Chapters

- [Chapter 76: RPC Module Architecture](Developer-Manual-Chapter76-RPC-Module-Architecture.md)
- [Chapter 75: StateFlow Utilities](Developer-Manual-Chapter75-StateFlow-Utilities.md)
