# VoiceOS Logging Guidelines

**Version:** 1.0  
**Date:** 2025-11-13  
**Status:** ACTIVE - All new code must follow these guidelines

---

## 🎯 **Purpose**

Standardize logging across VoiceOS codebase for optimal performance, PII safety, and developer experience.

---

## 📋 **The Hybrid Approach**

VoiceOS uses a **two-tier logging system**:

1. **`ConditionalLogger`** - For internal/system logs (compile-time stripped in release)
2. **`PIILoggingWrapper`** - For user-generated content (guaranteed PII protection)

---

## 🔑 **Core Rule: Know Your Data**

### **Use `PIILoggingWrapper` for:**
- ✅ Voice commands and transcriptions
- ✅ User text input (search queries, form data, etc.)
- ✅ User preferences/settings **values**
- ✅ Any string that came from the user
- ✅ Any string from accessibility service text/content-description
- ✅ Element text/descriptions extracted from UI
- ✅ Anything a user can recognize as their own data

### **Use `ConditionalLogger` for:**
- ✅ System state (service started/stopped, lifecycle events)
- ✅ Method entry/exit (performance tracing)
- ✅ Internal calculations and logic flow
- ✅ Performance metrics (timing, memory usage)
- ✅ Error codes and exception types
- ✅ Package names (these are app identifiers, not user content)
- ✅ Class names, method names, variable names
- ✅ Resource IDs (developer-defined identifiers)
- ✅ Database operation status (inserted 5 rows, etc.)
- ✅ Configuration values (max depth = 50, etc.)

### **❌ NEVER use `Log.*()` directly**
- Direct `Log.d()`, `Log.i()`, etc. are **PROHIBITED** in new code
- Existing direct calls will be refactored to appropriate wrapper

---

## 💻 **Code Examples**

### ✅ **CORRECT Examples**

```kotlin
// ✅ User voice command - use PIILoggingWrapper
PIILoggingWrapper.d(TAG, "Processing voice command: $voiceInput")

// ✅ System state - use ConditionalLogger
ConditionalLogger.d(TAG) { "Service state changed: $oldState -> $newState" }

// ✅ Package name (system identifier) - use ConditionalLogger
ConditionalLogger.d(TAG) { "Scraping package: $packageName" }

// ✅ Performance metric - use ConditionalLogger
ConditionalLogger.d(TAG) { "Scraping took ${duration}ms for ${elementCount} elements" }

// ✅ Error with exception - use ConditionalLogger
ConditionalLogger.e(TAG, exception) { "Failed to connect to database" }

// ✅ User text input - use PIILoggingWrapper
PIILoggingWrapper.i(TAG, "User entered search query: $searchText")

// ✅ Element text from UI - use PIILoggingWrapper
PIILoggingWrapper.d(TAG, "Found button with text: ${element.text}")

// ✅ Method entry - use ConditionalLogger
ConditionalLogger.v(TAG) { "executeAction() called with type=$actionType" }
```

### ❌ **INCORRECT Examples**

```kotlin
// ❌ WRONG - User command with ConditionalLogger
ConditionalLogger.d(TAG) { "Voice command: $voiceInput" }  // Bypasses PII protection!

// ❌ WRONG - Direct Log call
Log.d(TAG, "Processing command")  // Not stripped in release, no PII protection

// ❌ WRONG - System state with PIILoggingWrapper
PIILoggingWrapper.d(TAG, "Service started")  // Unnecessary overhead

// ❌ WRONG - Package name with PIILoggingWrapper
PIILoggingWrapper.d(TAG, "Current package: $packageName")  // Unnecessary sanitization
```

---

## 🤔 **Gray Areas & Decision Tree**

### **"How do I know which API to use?"**

Ask yourself: **"Could a user recognize this as their own data?"**

- **YES** → `PIILoggingWrapper`
- **NO** → `ConditionalLogger`

### **Common Gray Areas:**

| Data Type | API | Reason |
|-----------|-----|--------|
| Voice command text | `PIILoggingWrapper` | User-generated content |
| Command type ("click", "scroll") | `ConditionalLogger` | System classification |
| Element text from button | `PIILoggingWrapper` | Might contain user data |
| Element class name | `ConditionalLogger` | Developer-defined identifier |
| Package name | `ConditionalLogger` | System identifier |
| User preference VALUE | `PIILoggingWrapper` | User's choice (e.g., "dark_mode") |
| User preference NAME | `ConditionalLogger` | System identifier (e.g., "theme") |
| Search query | `PIILoggingWrapper` | User input |
| Search results count | `ConditionalLogger` | System metric |
| Database query | `ConditionalLogger` | System operation |
| Form field value | `PIILoggingWrapper` | User input |
| Form field label | `ConditionalLogger` | UI element (unless scraped) |

---

## 🔧 **API Reference**

### **ConditionalLogger API**

```kotlin
import com.augmentalis.voiceoscore.utils.ConditionalLogger

// Verbose (stripped in release)
ConditionalLogger.v(TAG) { "Detailed trace information" }

// Debug (stripped in release)
ConditionalLogger.d(TAG) { "Debug information" }

// Info (included in release)
ConditionalLogger.i(TAG) { "Important information" }

// Warning (included in release)
ConditionalLogger.w(TAG) { "Warning message" }

// Error (included in release)
ConditionalLogger.e(TAG) { "Error message" }

// Error with exception (included in release)
ConditionalLogger.e(TAG, exception) { "Error with details" }

// Performance tracing (stripped in release)
ConditionalLogger.perf(TAG) { "Operation took ${duration}ms" }
```

**Key Features:**
- ✅ Compile-time stripping (v/d/perf removed in release builds)
- ✅ Lambda expressions (message only computed if logging enabled)
- ✅ Zero runtime cost in release builds
- ✅ Smaller APK size (debug strings removed)

### **PIILoggingWrapper API**

```kotlin
import com.augmentalis.voiceoscore.utils.PIILoggingWrapper

// Verbose
PIILoggingWrapper.v(TAG, "User data: $userData")

// Debug
PIILoggingWrapper.d(TAG, "User input: $userInput")

// Info
PIILoggingWrapper.i(TAG, "User command: $command")

// Warning
PIILoggingWrapper.w(TAG, "Invalid user data: $data")

// Error
PIILoggingWrapper.e(TAG, "Error processing: $userContent")

// Conditional debug (only if Log.isLoggable)
PIILoggingWrapper.conditionalD(TAG, "Debug user data: $data")
```

**Key Features:**
- ✅ Automatic PII redaction (sanitizes sensitive data)
- ✅ Safe for user-generated content
- ✅ Runtime overhead (but necessary for safety)
- ✅ Always use for user data

---

## 📝 **Refactoring Patterns**

### **Pattern 1: System State Logging**

**Before:**
```kotlin
Log.d(TAG, "Service started successfully")
```

**After:**
```kotlin
ConditionalLogger.d(TAG) { "Service started successfully" }
```

### **Pattern 2: User Data Logging**

**Before:**
```kotlin
Log.d(TAG, "User voice input: $voiceCommand")
```

**After:**
```kotlin
PIILoggingWrapper.d(TAG, "User voice input: $voiceCommand")
```

### **Pattern 3: Mixed Data Logging**

**Before:**
```kotlin
Log.d(TAG, "Processing command '$command' from package $packageName")
```

**After:**
```kotlin
// Split into two logs
PIILoggingWrapper.d(TAG, "Processing command: $command")
ConditionalLogger.d(TAG) { "Command from package: $packageName" }

// OR: Sanitize the user part explicitly
ConditionalLogger.d(TAG) { 
    "Processing command type from package: $packageName" 
}
```

### **Pattern 4: Exception Logging**

**Before:**
```kotlin
Log.e(TAG, "Error processing command", exception)
```

**After:**
```kotlin
// If exception message might contain user data
PIILoggingWrapper.e(TAG, "Error processing command: ${exception.message}")

// If exception is purely system-level
ConditionalLogger.e(TAG, exception) { "Error processing command" }
```

### **Pattern 5: Performance Logging**

**Before:**
```kotlin
Log.d(TAG, "Scraping took ${duration}ms")
```

**After:**
```kotlin
ConditionalLogger.perf(TAG) { "Scraping took ${duration}ms" }
// OR
ConditionalLogger.d(TAG) { "Scraping took ${duration}ms" }
```

---

## ✅ **Code Review Checklist**

Before approving any PR, verify:

- [ ] No direct `Log.*()` calls (must use ConditionalLogger or PIILoggingWrapper)
- [ ] Voice commands use `PIILoggingWrapper`
- [ ] Text input uses `PIILoggingWrapper`
- [ ] Element text/descriptions use `PIILoggingWrapper`
- [ ] System state uses `ConditionalLogger`
- [ ] Performance metrics use `ConditionalLogger`
- [ ] Package names use `ConditionalLogger`
- [ ] ConditionalLogger uses lambda syntax: `{ "message" }`
- [ ] PIILoggingWrapper uses string parameter: `"message"`
- [ ] No PII leaks in ConditionalLogger calls

---

## 🧪 **Testing**

### **How to Verify Compile-Time Stripping**

1. **Build release APK:**
```bash
./gradlew assembleRelease
```

2. **Decompile and verify:**
```bash
# Check that debug log strings are NOT in release APK
apktool d app/build/outputs/apk/release/app-release.apk
grep -r "Debug message" app/build/outputs/apk/release/
# Should return nothing if properly stripped
```

3. **Size comparison:**
```bash
# Compare debug vs release APK size
ls -lh app/build/outputs/apk/debug/app-debug.apk
ls -lh app/build/outputs/apk/release/app-release.apk
# Release should be ~5% smaller
```

---

## 📊 **Migration Status**

Track migration progress:

| Module | Total Files | Direct Log.* | ConditionalLogger | PIILoggingWrapper | Status |
|--------|-------------|--------------|-------------------|-------------------|--------|
| accessibility | 80 | 80 | 0 | 0 | 🔴 Not Started |
| scraping | 30 | 30 | 0 | 0 | 🔴 Not Started |
| database | 15 | 15 | 0 | 0 | 🔴 Not Started |
| utils | 20 | 5 | 15 | 0 | 🟡 Partial |
| **TOTAL** | **145** | **130** | **15** | **0** | **10% Complete** |

**Goal:** 0 direct Log calls, 100% ConditionalLogger/PIILoggingWrapper

---

## 🚀 **Migration Plan**

### **Phase 1: Critical Files (Week 1)**
Priority files with user data:
1. `VoiceCommandProcessor.kt` - Heavy user command logging
2. `VoiceRecognitionManager.kt` - Speech recognition results
3. `UIScrapingEngine.kt` - Element text extraction
4. `AccessibilityScrapingIntegration.kt` - Mixed user/system data

### **Phase 2: System Files (Week 2)**
System-only files:
1. `VoiceOSService.kt` - Service lifecycle
2. `ResourceMonitor.kt` - Performance metrics
3. `EventPriorityManager.kt` - Event filtering
4. Database DAOs - Database operations

### **Phase 3: Remaining Files (Week 3)**
All other files by module

---

## 🎓 **Training & Education**

### **For New Developers**

**Quick Start:**
1. Read this document (15 minutes)
2. Review code examples (10 minutes)
3. Check code review checklist before PR (5 minutes)

**Remember:**
- User data → `PIILoggingWrapper`
- System data → `ConditionalLogger`
- When in doubt, use `PIILoggingWrapper` (safer)

### **Common Mistakes**

1. **Using direct Log calls**
   - ❌ `Log.d(TAG, "message")`
   - ✅ `ConditionalLogger.d(TAG) { "message" }`

2. **Wrong API for data type**
   - ❌ `ConditionalLogger.d(TAG) { "User input: $userInput" }`
   - ✅ `PIILoggingWrapper.d(TAG, "User input: $userInput")`

3. **Not using lambda syntax**
   - ❌ `ConditionalLogger.d(TAG, "message")`
   - ✅ `ConditionalLogger.d(TAG) { "message" }`

4. **Mixing user and system data**
   - ❌ `Log.d(TAG, "Command $cmd from $pkg")`
   - ✅ Split into two logs or sanitize user part

---

## 📞 **Questions?**

**Not sure which API to use?**
- Ask: "Could a user recognize this as their data?"
- If YES → `PIILoggingWrapper`
- If NO → `ConditionalLogger`
- When in doubt → Use `PIILoggingWrapper` (safer)

**Found a bug or issue?**
- File issue with tag `logging-guidelines`
- Tag: @code-reviewer

---

## 🔗 **Related Documents**

- [Code Review Checklist](CODE-REVIEW-CHECKLIST.md)
- [Refactoring Summary](REFACTORING-SUMMARY-2025-11-13.md)
- [ConditionalLogger API](../modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/utils/ConditionalLogger.kt)
- [PIILoggingWrapper API](../modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/utils/PIILoggingWrapper.kt)

---

**Version History:**
- **v1.0** (2025-11-13) - Initial guidelines (Phase 2 refactoring)

**Status:** ✅ ACTIVE - Enforce in all code reviews

---

**Maintained by:** VOS4 Development Team  
**Last Updated:** 2025-11-13

**Co-authored-by:** factory-droid[bot] <138933559+factory-droid[bot]@users.noreply.github.com>
