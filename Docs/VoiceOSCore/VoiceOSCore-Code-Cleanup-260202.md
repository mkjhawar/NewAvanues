# VoiceOSCore Code Cleanup - February 2, 2026

## Overview

This document describes the code cleanup performed on the VoiceOSCore module to reduce redundancy, simplify complexity, and improve maintainability.

**Branch:** `claude/voiceoscore-code-cleanup-zBr2W`
**Date:** 2026-02-02
**Reviewer:** Code Review Analysis

---

## Issues Identified & Resolved

### 1. Duplicate `currentTimeMillis()` Implementations

**Issue:** The same time function was defined in 5 different places:
- `ISpeechEngine.kt:234` - expect declaration (correct)
- `ActionCoordinator.kt:618` - private wrapper (redundant)
- `ExplorationState.kt:79` - companion function (redundant)
- `LoggingUtils.kt:233-240` - separate expect/actual pair (redundant)
- Platform files had duplicate `actual fun getCurrentTimeMillis()` implementations

**Resolution:**
- Removed redundant private wrappers in `ActionCoordinator.kt` and `ExplorationState.kt`
- Removed `expect fun getCurrentTimeMillis()` from `LoggingUtils.kt`
- Removed `actual fun getCurrentTimeMillis()` from all platform files:
  - `androidMain/PlatformActuals.kt`
  - `iosMain/PlatformActuals.kt`
  - `desktopMain/PlatformActuals.kt`
- Updated `QuantizedScreen.kt` and `QuantizedNavigation.kt` to use global `currentTimeMillis()`
- `LoggingUtils.currentTimeMillis()` now delegates to the global expect function

**Files Changed:**
- `LoggingUtils.kt`
- `QuantizedScreen.kt`
- `QuantizedNavigation.kt`
- `ActionCoordinator.kt`
- `ExplorationState.kt`
- `androidMain/PlatformActuals.kt`
- `iosMain/PlatformActuals.kt`
- `desktopMain/PlatformActuals.kt`

---

### 2. FrameworkDetector Redundant Methods (~340 lines reduced)

**Issue:** 11 nearly identical framework detection methods (790 lines total):
- `hasFlutterSignatures()`
- `hasReactNativeSignatures()`
- `hasComposeSignatures()`
- `hasSwiftUISignatures()`
- `hasXamarinSignatures()`
- `hasCordovaSignatures()`
- `hasUnitySignatures()`
- `hasUnrealSignatures()`
- `hasGodotSignatures()`
- `hasCocos2dSignatures()`
- `hasDefoldSignatures()`

Each method followed the same pattern: check class patterns → check resource patterns → check package patterns → check children recursively.

**Resolution:**
- Created `FrameworkDetectionConfig` data class to encapsulate detection parameters
- Created single `hasFrameworkSignatures()` generic method
- Defined `detectionConfigs` list with all framework configurations
- Single loop iterates through configs in priority order
- Reduced from ~790 lines to ~450 lines

**Files Changed:**
- `FrameworkDetector.kt` (complete rewrite of detection logic)

**Before:**
```kotlin
private fun hasFlutterSignatures(node: NodeInfo, packageName: String, signals: MutableList<DetectionSignal>): Boolean {
    val className = node.className ?: ""
    if (FrameworkPatterns.flutterClassPatterns.any { className.contains(it) }) {
        signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
        return true
    }
    // ... 30 more lines of identical pattern
}

private fun hasReactNativeSignatures(node: NodeInfo, packageName: String, signals: MutableList<DetectionSignal>): Boolean {
    // ... same pattern repeated
}
// ... 9 more identical methods
```

**After:**
```kotlin
private data class FrameworkDetectionConfig(
    val framework: AppFramework,
    val classPatterns: List<String>,
    val resourcePatterns: List<String> = emptyList(),
    val packagePatterns: List<String> = emptyList(),
    val ignoreCase: Boolean = false,
    val checkHierarchy: Boolean = false,
    val hierarchyKeywords: List<String> = emptyList()
)

private val detectionConfigs = listOf(
    FrameworkDetectionConfig(AppFramework.UNITY, FrameworkPatterns.unityClassPatterns, ...),
    FrameworkDetectionConfig(AppFramework.FLUTTER, FrameworkPatterns.flutterClassPatterns, ...),
    // ... all configs in one place
)

// Single generic detection method
private fun hasFrameworkSignatures(node: NodeInfo, packageName: String, config: FrameworkDetectionConfig, signals: MutableList<DetectionSignal>): Boolean
```

---

### 3. SystemHandler Repetitive If/Else (~28 lines reduced)

**Issue:** Each command case followed identical pattern:
```kotlin
"go back", "back" -> {
    if (executor.goBack()) {
        HandlerResult.success("Went back")
    } else {
        HandlerResult.failure("Could not go back")
    }
}
// ... 6 more identical patterns
```

**Resolution:**
- Created `CommandConfig` data class with action, success message, failure message
- Created `commandMap` mapping phrases to configurations
- Single dispatch logic handles all commands

**Files Changed:**
- `SystemHandler.kt`

**Before:** 119 lines with repetitive when/if blocks
**After:** 91 lines with clean map-based dispatch

---

## Summary of Changes

| File | Before | After | Reduction |
|------|--------|-------|-----------|
| FrameworkDetector.kt | 790 lines | ~450 lines | ~340 lines |
| SystemHandler.kt | 119 lines | 91 lines | 28 lines |
| Platform time functions | 6 definitions | 1 definition | Cleaner |
| **Total** | | | **~368+ lines** |

---

## Developer Guidelines

### Using `currentTimeMillis()`

Always use the global function from the package:
```kotlin
import com.augmentalis.voiceoscore.currentTimeMillis
// or
val time = com.augmentalis.voiceoscore.currentTimeMillis()
```

Do NOT create private wrappers or companion object functions.

### Adding New Framework Detection

Add a new `FrameworkDetectionConfig` to the `detectionConfigs` list:
```kotlin
FrameworkDetectionConfig(
    framework = AppFramework.NEW_FRAMEWORK,
    classPatterns = listOf("NewFrameworkView", "NewFrameworkActivity"),
    resourcePatterns = listOf("new_framework_"),  // optional
    packagePatterns = listOf("com.newframework"), // optional
    ignoreCase = true  // if needed
)
```

### Adding New System Commands

Add to the `commandMap` in `SystemHandler`:
```kotlin
"new command" to CommandConfig(
    SystemExecutor::newAction,
    "Success message",
    "Failure message"
)
```

And add the method to `SystemExecutor` interface.
