# SpeechRecognition KMP Migration - February 2, 2026

## Overview

This document describes the migration of SpeechRecognition module from dual-package legacy Java code to a unified KMP (Kotlin Multiplatform) structure.

**Branch:** `claude/speechrecognition-cleanup-zBr2W`
**Date:** 2026-02-02
**Status:** Completed

---

## Problem Statement

The SpeechRecognition module had two parallel codebases:

| Package | Location | Files | Lines |
|---------|----------|-------|-------|
| Legacy | `src/main/java/com/augmentalis/voiceos/speech/*` | 71 | ~25,000 |
| KMP | `src/androidMain/kotlin/com/augmentalis/speechrecognition/*` | 33 | ~10,000 |

This caused:
- 26 duplicate files with near-identical functionality
- Confusing import decisions for developers
- Inconsistent API (e.g., `OnSpeechErrorListener` signature differed)
- Maintenance overhead - bugs needed fixing in two places

---

## Solution

### 1. Chose KMP as Canonical

The KMP package (`com.augmentalis.speechrecognition`) was chosen as canonical because:
- It's the future-proof multiplatform architecture
- It already had the most up-to-date implementations
- It follows modern Kotlin conventions

### 2. Updated Consumer Imports

**VoiceOSCore Module:**
- `VivokaAndroidEngine.kt` - Updated imports and API usage

```kotlin
// Before (legacy)
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine
import com.augmentalis.voiceos.speech.engines.vivoka.VivokaPathResolver

// After (KMP)
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.vivoka.VivokaEngine
import com.augmentalis.speechrecognition.vivoka.VivokaPathResolver
```

**API Breaking Change - Error Listener:**

```kotlin
// Legacy API
typealias OnSpeechErrorListener = (error: String, code: Int) -> Unit

// KMP API
typealias OnSpeechErrorListener = (error: SpeechError) -> Unit

// Updated usage in VivokaAndroidEngine.kt
vivokaEngine?.setErrorListener { error ->
    _errors.emit(SpeechError(
        code = SpeechError.ErrorCode.RECOGNITION_FAILED,
        message = error.message,
        recoverable = error.isRecoverable
    ))
}
```

**VoiceOS CommandManager Module:**
- `CommandManager.kt` - Updated ConfidenceScorer import
- `CommandManagerIntegrationTest.kt` - Updated ConfidenceLevel import

```kotlin
// Before
import com.augmentalis.voiceos.speech.confidence.ConfidenceScorer
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel

// After
import com.augmentalis.speechrecognition.ConfidenceScorer
import com.augmentalis.speechrecognition.ConfidenceLevel
```

### 3. Archived Legacy Code

Legacy source code moved to archive:
```
archive/deprecated/SpeechRecognition-legacy-260202/
├── java/                        # ~25,000 lines of legacy code
│   └── com/augmentalis/voiceos/speech/
│       ├── api/
│       ├── commands/
│       ├── confidence/
│       ├── engines/
│       │   ├── android/
│       │   ├── common/
│       │   ├── google/
│       │   ├── tts/
│       │   ├── vivoka/
│       │   ├── vosk/
│       │   └── whisper/
│       ├── help/
│       └── utils/
└── test.old-android-only/       # Legacy tests
```

### 4. Retained in src/main/

These files remain as they're needed for Android build:
- `AndroidManifest.xml` - Library manifest
- `cpp/` - Native C++ code for Whisper integration
- `res/` - Android resources

---

## Files Changed

| File | Change |
|------|--------|
| `VoiceOSCore/src/androidMain/.../VivokaAndroidEngine.kt` | Updated imports and error listener API |
| `VoiceOS/managers/CommandManager/.../CommandManager.kt` | Updated ConfidenceScorer import |
| `VoiceOS/managers/CommandManager/.../CommandManagerIntegrationTest.kt` | Updated ConfidenceLevel import |

---

## Migration Impact

### Lines Saved
- Legacy code archived: ~25,000 lines
- Duplicate maintenance eliminated

### API Stability
- KMP API is now the single source of truth
- All consumers use consistent imports

### Developer Experience
- Clear package structure: `com.augmentalis.speechrecognition.*`
- No more confusion about which package to import

---

## Verification

After migration, verify:

1. **Build succeeds:**
   ```bash
   ./gradlew :Modules:SpeechRecognition:assembleDebug
   ./gradlew :Modules:VoiceOSCore:assembleDebug
   ./gradlew :Modules:VoiceOS:managers:CommandManager:assembleDebug
   ```

2. **Tests pass:**
   ```bash
   ./gradlew :Modules:SpeechRecognition:test
   ./gradlew :Modules:VoiceOSCore:test
   ```

3. **Runtime verification:**
   - Speech recognition initializes correctly
   - Commands are recognized
   - Error handling works

---

## Rollback

If issues are discovered, the legacy code can be restored from:
```
archive/deprecated/SpeechRecognition-legacy-260202/java/
```

Move it back to:
```
Modules/SpeechRecognition/src/main/java/
```

And revert import changes in VoiceOSCore and VoiceOS modules.

---

## Future Work

1. **Move SimilarityMatcher to KMP commonMain** - Currently only in androidMain
2. **Create KMP tests** - Migrate useful tests from legacy test suite
3. **Consider removing native C++ dependency** - Or migrate to KMP native targets
