# Implementation Plan: VoiceOSCoreNG Compile Error Fix

**Date:** 2026-01-19 | **Version:** V1 | **Author:** Claude
**Branch:** legacy-consolidation
**Status:** Ready for Implementation

---

## Summary

Fix 193 compile errors in `android/apps/voiceoscoreng/` caused by incorrect import paths. The app imports from `com.augmentalis.voiceoscoreng.*` but the VoiceOSCore KMP module exports `com.augmentalis.voiceoscore.*`.

---

## Root Cause Analysis

### Problem
The Android app `voiceoscoreng` references packages that don't exist:
- `com.augmentalis.voiceoscoreng.handlers.*`
- `com.augmentalis.voiceoscoreng.features.*`
- `com.augmentalis.voiceoscoreng.common.*`
- `com.augmentalis.voiceoscoreng.functions.*`
- `com.augmentalis.voiceoscoreng.persistence.*`

### Actual Package Structure
VoiceOSCore uses a **flat package structure** at `com.augmentalis.voiceoscore.*`:

| Missing Import | Actual Location |
|----------------|-----------------|
| `VoiceOSCoreNG` | `com.augmentalis.voiceoscore.VoiceOSCoreNG` |
| `ElementInfo`, `Bounds` | `com.augmentalis.voiceoscore.ElementInfo` |
| `ElementFingerprint` | `com.augmentalis.voiceoscore.ElementFingerprint` |
| `QuantizedCommand` | `com.augmentalis.voiceoscore.QuantizedCommand` |
| `CommandRegistry` | `com.augmentalis.voiceoscore.CommandRegistry` |
| `StaticCommandRegistry` | `com.augmentalis.voiceoscore.StaticCommandRegistry` |
| `CommandGenerator` | `com.augmentalis.voiceoscore.CommandGenerator` |
| `LearnAppConfig` | `com.augmentalis.voiceoscore.LearnAppConfig` |
| `LearnAppDevToggle` | `com.augmentalis.voiceoscore.LearnAppDevToggle` |
| `ServiceConfiguration` | `com.augmentalis.voiceoscore.ServiceConfiguration` |
| `HashUtils` | `com.augmentalis.voiceoscore.HashUtils` |
| `ICommandPersistence` | `com.augmentalis.voiceoscore.ICommandPersistence` |
| `ScreenHashRepository`, `ScreenInfo` | `com.augmentalis.voiceoscore.ScreenHashRepository` |

### Missing Implementations (Need Creation)
Some classes referenced by the app do NOT exist in VoiceOSCore and need to be created:

| Missing Class | Purpose | Action |
|---------------|---------|--------|
| `DeveloperSettingsScreen` | Compose UI for dev settings | Create in app |
| `ScanningCallbacks` | Callbacks for UI scanning | Create in app |
| `AndroidCommandPersistence` | Android impl of ICommandPersistence | Create in androidMain |
| `ScreenHashRepositoryImpl` | SQLDelight impl of ScreenHashRepository | Create in androidMain |
| `createForAndroid` | Factory function for VoiceOSCoreNG | Create in androidMain |

---

## Implementation Tasks

### Phase 1: Fix Imports (10 files)

#### Task 1.1: MainActivity.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/MainActivity.kt`

**Changes:**
```kotlin
// REMOVE these imports:
import com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.features.LearnAppConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import com.augmentalis.voiceoscoreng.features.DeveloperSettingsScreen
import com.augmentalis.voiceoscoreng.features.ScanningCallbacks

// ADD these imports:
import com.augmentalis.voiceoscore.VoiceOSCoreNG
import com.augmentalis.voiceoscore.LearnAppConfig
import com.augmentalis.voiceoscore.LearnAppDevToggle
// DeveloperSettingsScreen - create locally or import from app
// ScanningCallbacks - create locally or import from app
```

#### Task 1.2: VoiceOSCoreNGApplication.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/VoiceOSCoreNGApplication.kt`

**Changes:**
```kotlin
// REMOVE:
import com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG as VoiceOSCoreNGConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import com.augmentalis.voiceoscoreng.persistence.AndroidCommandPersistence
import com.augmentalis.voiceoscoreng.persistence.ICommandPersistence

// ADD:
import com.augmentalis.voiceoscore.VoiceOSCoreNG as VoiceOSCoreNGConfig
import com.augmentalis.voiceoscore.LearnAppDevToggle
import com.augmentalis.voiceoscore.ICommandPersistence
// AndroidCommandPersistence - need to create or use database module
```

#### Task 1.3: VoiceOSAccessibilityService.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`

**Changes:**
```kotlin
// REMOVE:
import com.augmentalis.voiceoscoreng.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.createForAndroid
import com.augmentalis.voiceoscoreng.common.*
import com.augmentalis.voiceoscoreng.handlers.ServiceConfiguration
import com.augmentalis.voiceoscoreng.persistence.*

// ADD:
import com.augmentalis.voiceoscore.VoiceOSCoreNG
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ServiceConfiguration
import com.augmentalis.voiceoscore.ICommandPersistence
import com.augmentalis.voiceoscore.ScreenHashRepository
import com.augmentalis.voiceoscore.ScreenInfo
```

#### Task 1.4: DynamicCommandGenerator.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/DynamicCommandGenerator.kt`

**Changes:**
```kotlin
// REMOVE:
import com.augmentalis.voiceoscoreng.common.*
import com.augmentalis.voiceoscoreng.functions.HashUtils
import com.augmentalis.voiceoscoreng.persistence.ICommandPersistence

// ADD:
import com.augmentalis.voiceoscore.CommandGenerator
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.StaticCommandRegistry
import com.augmentalis.voiceoscore.HashUtils
import com.augmentalis.voiceoscore.ICommandPersistence
```

#### Task 1.5: ElementExtractor.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/ElementExtractor.kt`

**Changes:**
```kotlin
// REMOVE:
import com.augmentalis.voiceoscoreng.common.*
import com.augmentalis.voiceoscoreng.functions.HashUtils

// ADD:
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.CommandGenerator
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HashUtils
```

#### Task 1.6: AVUFormatter.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/AVUFormatter.kt`

**Changes:**
```kotlin
// REMOVE:
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.common.ElementInfo

// ADD:
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ElementInfo
```

#### Task 1.7: ScreenCacheManager.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/ScreenCacheManager.kt`

**Changes:**
```kotlin
// REMOVE:
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.functions.HashUtils
import com.augmentalis.voiceoscoreng.persistence.*

// ADD:
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.HashUtils
import com.augmentalis.voiceoscore.ScreenHashRepository
import com.augmentalis.voiceoscore.ScreenInfo
```

#### Task 1.8: ExplorationModels.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/ExplorationModels.kt`

**Changes:**
```kotlin
// REMOVE:
import com.augmentalis.voiceoscoreng.common.ElementInfo

// ADD:
import com.augmentalis.voiceoscore.ElementInfo
```

#### Task 1.9: OverlayService.kt
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/OverlayService.kt`

**Changes:**
```kotlin
// Check for any persistence imports and update
// REMOVE:
import com.augmentalis.voiceoscoreng.persistence.*

// ADD appropriate voiceoscore imports
```

---

### Phase 2: Create Missing Implementations

#### Task 2.1: Create AndroidCommandPersistence
**File:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/AndroidCommandPersistence.kt`

```kotlin
package com.augmentalis.voiceoscore

import android.content.Context
import com.augmentalis.database.VoiceOSDatabase

/**
 * Android implementation of ICommandPersistence using SQLDelight.
 */
class AndroidCommandPersistence(
    private val database: VoiceOSDatabase
) : ICommandPersistence {
    // Implement all interface methods using database queries
}
```

#### Task 2.2: Create ScreenHashRepositoryImpl
**File:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/ScreenHashRepositoryImpl.kt`

```kotlin
package com.augmentalis.voiceoscore

import com.augmentalis.database.VoiceOSDatabase

/**
 * Android implementation of ScreenHashRepository using SQLDelight.
 */
class ScreenHashRepositoryImpl(
    private val database: VoiceOSDatabase
) : ScreenHashRepository {
    // Implement all interface methods
}
```

#### Task 2.3: Create createForAndroid factory
**File:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/VoiceOSCoreAndroidFactory.kt`

```kotlin
package com.augmentalis.voiceoscore

import android.content.Context
import android.accessibilityservice.AccessibilityService

/**
 * Factory function to create VoiceOSCoreNG for Android.
 */
fun VoiceOSCoreNG.Companion.createForAndroid(
    context: Context,
    accessibilityService: AccessibilityService? = null
): VoiceOSCoreNG {
    return VoiceOSCoreNG.Builder()
        .setPlatformProvider(VoiceOSCoreAndroid.init(context))
        .build()
}
```

#### Task 2.4: Create DeveloperSettingsScreen (App-local)
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/ui/DeveloperSettingsScreen.kt`

Create a Compose UI screen for developer settings (toggle dev mode, view logs, etc.)

#### Task 2.5: Create ScanningCallbacks (App-local)
**File:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/ui/ScanningCallbacks.kt`

Create callback interface for UI scanning operations.

---

### Phase 3: Fix Compose-related Errors

Several errors relate to Compose context issues:
- `@Composable invocations can only happen from the context of a @Composable function`
- `Property delegate must have a 'getValue' method`

These require reviewing the code structure to ensure Compose functions are properly annotated.

---

## File Change Summary

| File | Action | Lines Changed (Est.) |
|------|--------|---------------------|
| MainActivity.kt | Fix imports | ~15 |
| VoiceOSCoreNGApplication.kt | Fix imports | ~10 |
| VoiceOSAccessibilityService.kt | Fix imports | ~25 |
| DynamicCommandGenerator.kt | Fix imports | ~15 |
| ElementExtractor.kt | Fix imports | ~10 |
| AVUFormatter.kt | Fix imports | ~5 |
| ScreenCacheManager.kt | Fix imports | ~10 |
| ExplorationModels.kt | Fix imports | ~3 |
| OverlayService.kt | Fix imports + Compose fixes | ~20 |
| **NEW:** AndroidCommandPersistence.kt | Create | ~100 |
| **NEW:** ScreenHashRepositoryImpl.kt | Create | ~80 |
| **NEW:** VoiceOSCoreAndroidFactory.kt | Create | ~30 |
| **NEW:** DeveloperSettingsScreen.kt | Create | ~150 |
| **NEW:** ScanningCallbacks.kt | Create | ~30 |

---

## Verification

After implementation:
1. Run `./gradlew :android:apps:voiceoscoreng:compileDebugKotlin`
2. Verify 0 compile errors
3. Run unit tests: `./gradlew :android:apps:voiceoscoreng:testDebugUnitTest`
4. Run Android app on emulator/device

---

## Dependencies

- VoiceOSCore module must be built first
- VoiceOS database module must be included

---

## Risks

| Risk | Mitigation |
|------|------------|
| Breaking changes in API | Use type aliases for backwards compatibility if needed |
| Missing functionality | Create stubs, implement incrementally |
| Compose version mismatch | Ensure BOM versions match |

---

## Notes

- The VoiceOSCore module follows KMP flat package structure per CLAUDE.md rules
- All classes are at `com.augmentalis.voiceoscore.*` (no subpackages)
- Android-specific implementations go in `androidMain` source set
