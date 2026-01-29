# VoiceOSCore Refactor Plan: Confidence Normalization & Critical Fixes

**Created:** 2026-01-29
**Status:** Planning
**Author:** Development Team

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Issue 1: Vivoka Confidence Scale Mismatch](#issue-1-vivoka-confidence-scale-mismatch)
3. [Issue 2: Numeric Command "1" Not Working](#issue-2-numeric-command-1-not-working)
4. [Issue 3: Overlay Numbers Not Clearing](#issue-3-overlay-numbers-not-clearing)
5. [Issue 4: Command Execution Stops After 3-4 Apps](#issue-4-command-execution-stops-after-3-4-apps)
6. [Issue 5: Duplicate SpeechConfig Classes](#issue-5-duplicate-speechconfig-classes)
7. [Implementation Plan](#implementation-plan)
8. [Files to Modify](#files-to-modify)
9. [Testing Strategy](#testing-strategy)

---

## Executive Summary

This document outlines critical fixes needed for VoiceOSCore, including a major refactor to standardize confidence values across all speech engines to use a 0-10000 scale externally while maintaining native internal representations.

### Issues Identified

| Issue | Severity | Impact |
|-------|----------|--------|
| Vivoka confidence mismatch | **Critical** | Random commands execute, wrong commands rejected |
| Numeric "1" not working | High | Calculator and list navigation broken |
| Overlay not clearing | High | Wrong items clicked, UI confusion |
| Commands stop after 3-4 apps | **Critical** | Voice control becomes unusable |

---

## Issue 1: Vivoka Confidence Scale Mismatch

### Problem Description

Vivoka SDK returns confidence values in the **0-10000** range, but the system is configured to expect **0.0-1.0** range. This causes:

1. **All commands pass threshold check** - A Vivoka confidence of 7000 compared to threshold 0.7 always passes (7000 > 0.7)
2. **Random low-confidence commands execute** - Even garbage recognition with confidence 100 passes (100 > 0.7)
3. **ConfidenceScorer incorrectly normalizes** - Uses `VIVOKA_MAX_SCORE = 100f` instead of `10000f`

### Root Cause Analysis

**File:** `VivokaRecognizer.kt` (lines 113-117)
```kotlin
val confidence = first.confidence  // Int from Vivoka: 0-10000
Log.d(TAG, "Hypothesis: command='$command', confidence=$confidence, threshold=${config.confidenceThreshold}")

if (confidence < config.confidenceThreshold) {  // BROKEN: comparing 7000 < 0.7
```

**File:** `ConfidenceScorer.kt` (line 90)
```kotlin
private const val VIVOKA_MAX_SCORE = 100f  // WRONG: Should be 10000f
```

**File:** `SpeechConfiguration.kt` (lines 63-79) - Has correct scale but not used consistently
```kotlin
fun normalizedToVivokaConfidence(normalized: Float): Float {
    return (normalized * 9000f + 1000f).coerceIn(1000f, 10000f)  // Implies 1000-10000 range
}
```

### Proposed Solution: Unified 0-10000 External Scale

**Design Decision:** Standardize all external/user-facing confidence values to 0-10000 scale.

**Benefits:**
- Granular control (7500 vs 7600 instead of 0.75 vs 0.76)
- Consistent UX regardless of engine
- No loss of Vivoka precision
- Easier debugging with comparable numbers

**Conversion Rules:**

| Engine | Native Scale | External (API) | Conversion |
|--------|-------------|----------------|------------|
| Vivoka | 0-10000 | 0-10000 | None (native) |
| Google | 0.0-1.0 | 0-10000 | `raw * 10000` |
| VOSK | log-likelihood | 0-10000 | `sigmoid(raw) * 10000` |
| Android STT | 0.0-1.0 | 0-10000 | `raw * 10000` |
| Whisper | 0.0-1.0 | 0-10000 | `raw * 10000` |

### Files Requiring Changes

#### Core Configuration (Change threshold type/validation)
- [ ] `SpeechRecognition/src/commonMain/kotlin/com/augmentalis/speechrecognition/SpeechConfig.kt`
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/speech/SpeechConfig.kt`

#### Confidence Scoring (Update normalization)
- [ ] `SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/confidence/ConfidenceScorer.kt`
- [ ] `SpeechRecognition/src/androidMain/kotlin/com/augmentalis/speechrecognition/ConfidenceScorer.kt`

#### Vivoka Engine (Fix comparison)
- [ ] `SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaRecognizer.kt`
- [ ] `SpeechRecognition/src/androidMain/kotlin/com/augmentalis/speechrecognition/vivoka/VivokaRecognizer.kt`

#### Other Engines (Add conversion)
- [ ] `SpeechRecognition/src/androidMain/kotlin/com/augmentalis/speechrecognition/AndroidSTTEngine.kt`
- [ ] Other engine implementations

#### Consumers (Update threshold values)
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/CommandWordDetector.kt`
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/speech/SpeechEngineManager.kt`
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/VoiceOSCore.kt`
- [ ] All files using `confidenceThreshold` (52 files total)

### Code Changes

#### 1. SpeechConfig - Change threshold to Int

```kotlin
// BEFORE
val confidenceThreshold: Float = 0.7f

// AFTER
/**
 * Minimum confidence threshold (0-10000 scale)
 * 7000 = 70% confidence
 * Results below this are filtered out
 */
val confidenceThreshold: Int = 7000

// Validation change
confidenceThreshold !in 0..10000 ->
    Result.failure(IllegalArgumentException("Confidence threshold must be 0-10000"))
```

#### 2. ConfidenceScorer - Normalize TO 10000

```kotlin
// BEFORE
private const val VIVOKA_MAX_SCORE = 100f

fun normalizeConfidence(rawScore: Float, engine: RecognitionEngine): Float {
    return when (engine) {
        RecognitionEngine.VIVOKA -> (rawScore / VIVOKA_MAX_SCORE).coerceIn(0f, 1f)
        // ...
    }
}

// AFTER
private const val NORMALIZED_MAX = 10000f

/**
 * Normalize confidence score from engine-specific scale to 0-10000
 */
fun normalizeConfidence(rawScore: Float, engine: RecognitionEngine): Int {
    return when (engine) {
        RecognitionEngine.VIVOKA -> {
            // Vivoka already uses 0-10000, pass through
            rawScore.toInt().coerceIn(0, 10000)
        }
        RecognitionEngine.VOSK -> {
            // VOSK returns acoustic log-likelihood, use sigmoid then scale
            val sigmoid = 1f / (1f + exp(-rawScore))
            (sigmoid * NORMALIZED_MAX).toInt().coerceIn(0, 10000)
        }
        RecognitionEngine.GOOGLE,
        RecognitionEngine.ANDROID,
        RecognitionEngine.WHISPER -> {
            // Already 0-1 scale, multiply by 10000
            (rawScore * NORMALIZED_MAX).toInt().coerceIn(0, 10000)
        }
    }
}
```

#### 3. VivokaRecognizer - Fix comparison

```kotlin
// BEFORE
val confidence = first.confidence
if (confidence < config.confidenceThreshold) {  // Int vs Float!

// AFTER
val confidence = first.confidence  // Int: 0-10000
if (confidence < config.confidenceThreshold) {  // Int vs Int (both 0-10000)
```

---

## Issue 2: Numeric Command "1" Not Working

### Problem Description

In Google Calculator app, saying "1" does not work while other numbers (2-9) work. The voice engine recognizes "one" (word) but the command is registered as "1" (digit).

### Root Cause Analysis

**File:** `CommandGenerator.kt` (lines 300-358)
```kotlin
fun generateNumericCommands(...): List<QuantizedCommand> {
    // ...
    QuantizedCommand(
        phrase = number.toString(),  // "1", "2", "3" - digit strings only
        // ...
    )
}
```

**File:** `NumberHandler.kt` (lines 73-76)
```kotlin
private val wordNumbers = mapOf(
    "one" to 1, "two" to 2, "three" to 3, ...
)
```

The `NumberHandler` supports word numbers but `CommandGenerator` only registers digit strings.

### Proposed Solution

Create a comprehensive `NumberToWords` utility that converts ANY number to its word equivalent and register BOTH forms:

#### NumberToWords Utility

```kotlin
/**
 * NumberToWords.kt - Converts numbers to spoken word equivalents
 *
 * Supports numbers from 0 to 999,999,999 (and beyond if needed)
 *
 * Examples:
 *   1 -> "one"
 *   21 -> "twenty one"
 *   100 -> "one hundred"
 *   101 -> "one hundred one"
 *   1234 -> "one thousand two hundred thirty four"
 */
object NumberToWords {

    private val ones = arrayOf(
        "", "one", "two", "three", "four", "five",
        "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen",
        "sixteen", "seventeen", "eighteen", "nineteen"
    )

    private val tens = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty",
        "sixty", "seventy", "eighty", "ninety"
    )

    /**
     * Convert a number to its word representation
     * @param number The number to convert (0 to Int.MAX_VALUE)
     * @return Word representation (e.g., 101 -> "one hundred one")
     */
    fun convert(number: Int): String {
        if (number == 0) return "zero"
        if (number < 0) return "negative ${convert(-number)}"

        return when {
            number < 20 -> ones[number]
            number < 100 -> {
                val ten = tens[number / 10]
                val one = ones[number % 10]
                if (one.isEmpty()) ten else "$ten $one"
            }
            number < 1000 -> {
                val hundreds = "${ones[number / 100]} hundred"
                val remainder = number % 100
                if (remainder == 0) hundreds else "$hundreds ${convert(remainder)}"
            }
            number < 1_000_000 -> {
                val thousands = "${convert(number / 1000)} thousand"
                val remainder = number % 1000
                if (remainder == 0) thousands else "$thousands ${convert(remainder)}"
            }
            number < 1_000_000_000 -> {
                val millions = "${convert(number / 1_000_000)} million"
                val remainder = number % 1_000_000
                if (remainder == 0) millions else "$millions ${convert(remainder)}"
            }
            else -> {
                val billions = "${convert(number / 1_000_000_000)} billion"
                val remainder = number % 1_000_000_000
                if (remainder == 0) billions else "$billions ${convert(remainder)}"
            }
        }.trim()
    }

    /**
     * Parse word number back to integer
     * @param words Word representation (e.g., "one hundred one")
     * @return Integer value or null if not parseable
     */
    fun parse(words: String): Int? {
        // Implementation for reverse conversion
        // ... (handle "one hundred", "twenty one", etc.)
    }
}
```

#### Updated CommandGenerator

```kotlin
fun generateNumericCommands(...): List<QuantizedCommand> {
    return bestElementPerIndex.flatMap { element ->
        val number = visualIndex + 1
        val digitPhrase = number.toString()           // "1", "100", "101"
        val wordPhrase = NumberToWords.convert(number) // "one", "one hundred", "one hundred one"

        // Register both digit and word forms
        listOf(digitPhrase, wordPhrase).map { phrase ->
            QuantizedCommand(
                phrase = phrase,
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.9f,
                metadata = mapOf(
                    "isNumericCommand" to "true",
                    "numericIndex" to number.toString(),
                    "digitForm" to digitPhrase,
                    "wordForm" to wordPhrase,
                    // ... other metadata
                )
            )
        }
    }
}
```

#### Examples

| Number | Digit Form | Word Form |
|--------|-----------|-----------|
| 1 | "1" | "one" |
| 10 | "10" | "ten" |
| 21 | "21" | "twenty one" |
| 100 | "100" | "one hundred" |
| 101 | "101" | "one hundred one" |
| 123 | "123" | "one hundred twenty three" |
| 1000 | "1000" | "one thousand" |

### Files to Modify

- [ ] Create: `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/util/NumberToWords.kt`
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/CommandGenerator.kt`
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/number/NumberHandler.kt` (use NumberToWords)

---

## Issue 3: Overlay Numbers Not Clearing

### Problem Description

1. Overlay numbers don't clear on screen/app change
2. Overlay numbers sometimes click wrong items (say "3", clicks item 5)
3. Duplicate overlay numbers appear on scroll

### Root Cause Analysis

**Screen Change Not Clearing:**
- `VoiceOSAccessibilityService.handleScreenChange()` updates commands but doesn't explicitly clear overlay
- No call to overlay clear before generating new commands

**Wrong Click Positions:**
- `BoundsResolver` uses cached bounds that become stale after scroll
- Scroll offset tracking may not be updating properly
- `scrollOffsets` map can get out of sync with actual positions

**Duplicate Numbers on Scroll:**
- `generateNumericCommands()` uses `visualIndex` which resets on each generation
- Elements that scroll off and back on get new numbers
- No persistent AVID-to-number mapping being used

### Proposed Solution

#### 1. Clear overlay on screen change

```kotlin
// VoiceOSAccessibilityService.kt
private fun handleScreenChange(event: AccessibilityEvent) {
    serviceScope.launch(Dispatchers.Default) {
        // CLEAR overlay before generating new commands
        clearOverlayNumbers()

        // ... existing code ...
    }
}
```

#### 2. Improve scroll offset tracking

```kotlin
// BoundsResolver.kt - Add sync validation
fun validateScrollOffsets(currentRoot: AccessibilityNodeInfo) {
    // Walk tree and update any stale scroll offsets
}
```

#### 3. Add persistent number assignments

```kotlin
// NumberAssignmentManager.kt (new file)
class NumberAssignmentManager {
    private val avidToNumber = mutableMapOf<String, Int>()
    private var nextNumber = 1

    fun getOrAssignNumber(avid: String): Int {
        return avidToNumber.getOrPut(avid) { nextNumber++ }
    }

    fun clearForNewScreen() {
        avidToNumber.clear()
        nextNumber = 1
    }
}
```

### Files to Modify

- [ ] `VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/VoiceOSAccessibilityService.kt`
- [ ] `VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/BoundsResolver.kt`
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/CommandGenerator.kt`
- [ ] Create: `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/number/NumberAssignmentManager.kt`

---

## Issue 4: Command Execution Stops After 3-4 Apps

### Problem Description

After switching between 3-4 apps, voice commands are recognized but not executed. The voice engine responds but no actions occur.

### Root Cause Analysis

**1. CommandRegistry Spinlock Starvation**

**File:** `CommandRegistry.kt` (lines 75-86, 205-236)
```kotlin
fun updateSync(newCommands: List<QuantizedCommand>) {
    // Spin until we acquire the mutex
    while (!mutex.tryLock()) {
        Thread.yield()  // Can starve under contention
    }
    // ...
}
```

After 3-4 app switches with 100+ commands each:
- Multiple threads spinning on mutex
- `Thread.yield()` ineffective under high contention
- Lock holders block for 10-50ms building maps
- Commands timeout or silently fail

**2. Commands Not Cleared on App Switch**

**File:** `VoiceOSAccessibilityService.kt` (lines 184-190)
```kotlin
if (packageName != null && packageName != currentPackageName) {
    currentPackageName = packageName
    getBoundsResolver()?.onPackageChanged(packageName)
    handleScreenChange(event)
    // NO CALL to coordinator.clearDynamicCommands()!
}
```

Commands accumulate across app switches, causing:
- Registry bloat (200+ stale commands)
- Stale VUID pointers to destroyed elements
- Longer search times in `findByPhrase()`

**3. Volatile Snapshot Race Condition**

**File:** `CommandRegistry.kt` (lines 40-48)
```kotlin
@Volatile
private var commandsSnapshot: Map<String, QuantizedCommand> = emptyMap()

@Volatile
private var labelCache: Map<String, String> = emptyMap()

// Two separate volatile reads can return inconsistent state!
fun findByPhrase(phrase: String): QuantizedCommand? {
    val snapshot = commandsSnapshot  // Read volatile once
    val labels = labelCache          // Read volatile once - may not match!
```

### Proposed Solution

#### 1. Replace spinlock with proper coroutine synchronization

```kotlin
// CommandRegistry.kt
class CommandRegistry {
    private val mutex = Mutex()

    // Remove sync version, use only suspend
    suspend fun update(newCommands: List<QuantizedCommand>) {
        mutex.withLock {
            updateInternal(newCommands)
        }
    }

    // For non-coroutine contexts, use runBlocking with timeout
    fun updateSync(newCommands: List<QuantizedCommand>) {
        runBlocking {
            withTimeout(5000L) {
                update(newCommands)
            }
        }
    }
}
```

#### 2. Clear commands on app switch

```kotlin
// VoiceOSAccessibilityService.kt
if (packageName != null && packageName != currentPackageName) {
    currentPackageName = packageName
    getBoundsResolver()?.onPackageChanged(packageName)

    // CLEAR stale commands before loading new ones
    getActionCoordinator().clearDynamicCommands()

    handleScreenChange(event)
}
```

#### 3. Use atomic snapshot pair

```kotlin
// CommandRegistry.kt
data class CommandSnapshot(
    val commands: Map<String, QuantizedCommand>,
    val labelCache: Map<String, String>
)

@Volatile
private var snapshot: CommandSnapshot = CommandSnapshot(emptyMap(), emptyMap())

fun findByPhrase(phrase: String): QuantizedCommand? {
    val snap = snapshot  // Single atomic read
    // ... use snap.commands and snap.labelCache
}
```

### Files to Modify

- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/CommandRegistry.kt`
- [ ] `VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/VoiceOSAccessibilityService.kt`
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/actions/ActionCoordinator.kt`

---

## Issue 5: Duplicate SpeechConfig Classes

### Problem Description

There are TWO separate `SpeechConfig` data classes in different modules:

1. **SpeechRecognition module:** `com.augmentalis.speechrecognition.SpeechConfig`
2. **VoiceOSCore module:** `com.augmentalis.voiceoscore.SpeechConfig`

This causes:
- Confusion about which to use
- Inconsistent configuration across modules
- Duplicated maintenance effort
- Potential for drift between implementations
- Import ambiguity requiring full package qualification

### Comparison of the Two Classes

| Feature | SpeechRecognition | VoiceOSCore |
|---------|------------------|-------------|
| **Package** | `com.augmentalis.speechrecognition` | `com.augmentalis.voiceoscore` |
| **Engine field** | `engine: SpeechEngine = VOSK` | None (determined elsewhere) |
| **Language codes** | Has `LanguageCodes` object | Uses `DEFAULT_LANGUAGE` constant |
| **TTS config** | Full TTS settings | None |
| **Fuzzy matching** | `enableFuzzyMatching`, `fuzzyMatchThreshold` | None |
| **Cloud settings** | `cloudApiKey`, `azureRegion` | `apiKey`, `apiRegion` |
| **Model config** | `modelPath` only | `modelPath`, `modelSize` enum |
| **Timing** | `timeoutDuration`, `dictationTimeout` | `silenceTimeout`, `endOfSpeechTimeout` |
| **Factory methods** | `vosk()`, `googleCloud()`, `azure()`, `whisper()` | `forVoiceCommands()`, `forDictation()`, `forOffline()` |

### Root Cause

The VoiceOSCore module was created as a KMP migration and defined its own `SpeechConfig` instead of depending on SpeechRecognition's version. Both evolved independently.

### Proposed Solution: Consolidate to Single Source

**Option A: Keep in SpeechRecognition (Recommended)**
- SpeechRecognition is the lower-level module
- VoiceOSCore depends on SpeechRecognition
- Move all config to `com.augmentalis.speechrecognition.SpeechConfig`
- VoiceOSCore imports and extends if needed

**Option B: Keep in VoiceOSCore**
- VoiceOSCore is the main consumer
- Move all config to `com.augmentalis.voiceoscore.SpeechConfig`
- SpeechRecognition becomes config-agnostic (takes raw parameters)

**Option C: New Shared Module**
- Create `speech-config` module
- Both modules depend on it
- Most flexible but adds complexity

### Recommended Approach: Option A

1. **Merge fields** into SpeechRecognition's `SpeechConfig`:
   - Add `modelSize: ModelSize` enum
   - Add `silenceTimeout` (rename from `timeoutDuration`)
   - Add `endOfSpeechTimeout` (rename from `dictationTimeout`)
   - Keep TTS settings
   - Keep fuzzy matching settings

2. **Deprecate** VoiceOSCore's `SpeechConfig`:
   ```kotlin
   @Deprecated(
       "Use com.augmentalis.speechrecognition.SpeechConfig",
       ReplaceWith("SpeechConfig", "com.augmentalis.speechrecognition.SpeechConfig")
   )
   typealias SpeechConfig = com.augmentalis.speechrecognition.SpeechConfig
   ```

3. **Migrate** all VoiceOSCore usages to use SpeechRecognition's version

4. **Delete** VoiceOSCore's `SpeechConfig.kt` after migration complete

### Unified SpeechConfig Structure

```kotlin
package com.augmentalis.speechrecognition

data class SpeechConfig(
    // ═══════════════════════════════════════════════════════════════════
    // Core Settings
    // ═══════════════════════════════════════════════════════════════════
    val language: String = LanguageCodes.ENGLISH_US,
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val engine: SpeechEngine = SpeechEngine.VOSK,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Int = 7000,  // 0-10000 scale (after refactor)

    // ═══════════════════════════════════════════════════════════════════
    // Timing Settings
    // ═══════════════════════════════════════════════════════════════════
    val maxRecordingDuration: Long = 30_000L,
    val silenceTimeout: Long = 5_000L,
    val endOfSpeechTimeout: Long = 2_000L,
    val voiceTimeoutMinutes: Long = 5L,

    // ═══════════════════════════════════════════════════════════════════
    // Feature Flags
    // ═══════════════════════════════════════════════════════════════════
    val enableProfanityFilter: Boolean = false,
    val enableInterimResults: Boolean = true,
    val enableWordTimestamps: Boolean = false,
    val preferOffline: Boolean = false,
    val enableFuzzyMatching: Boolean = true,
    val fuzzyMatchThreshold: Float = 0.7f,
    val enableSemanticMatching: Boolean = true,

    // ═══════════════════════════════════════════════════════════════════
    // Engine-Specific Settings
    // ═══════════════════════════════════════════════════════════════════
    val apiKey: String? = null,
    val apiRegion: String? = null,
    val modelPath: String? = null,
    val modelSize: ModelSize = ModelSize.MEDIUM,
    val staticCommandsPath: String = "static_commands/",

    // ═══════════════════════════════════════════════════════════════════
    // VoiceOS Commands
    // ═══════════════════════════════════════════════════════════════════
    val muteCommand: String = "mute voice",
    val wakeCommand: String = "wake up voice",
    val startDictationCommand: String = "start dictation",
    val stopDictationCommand: String = "stop dictation",

    // ═══════════════════════════════════════════════════════════════════
    // TTS Settings
    // ═══════════════════════════════════════════════════════════════════
    val enableTTS: Boolean = false,
    val ttsRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVolume: Float = 1.0f,
    val ttsLanguage: String = language,
    val ttsFeedbackLevel: TTSFeedbackLevel = TTSFeedbackLevel.NORMAL,
    val ttsVoice: String? = null
)
```

### Files to Modify

- [ ] `SpeechRecognition/src/commonMain/kotlin/com/augmentalis/speechrecognition/SpeechConfig.kt` - Merge all fields
- [ ] `VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/speech/SpeechConfig.kt` - Deprecate & delete
- [ ] All files importing `com.augmentalis.voiceoscore.SpeechConfig` - Update imports
- [ ] `VoiceOSCore/src/androidMain/kotlin/.../VivokaAndroidEngine.kt` - Uses both, consolidate

### Migration Steps

1. Add missing fields to SpeechRecognition's SpeechConfig
2. Add `@Deprecated` typealias in VoiceOSCore
3. Fix all compilation errors from deprecation warnings
4. Run tests to verify behavior unchanged
5. Remove VoiceOSCore's SpeechConfig.kt
6. Update documentation

---

## Implementation Plan

### Phase 1: Critical Bug Fixes (Immediate) - YOLO MODE

1. **Fix Vivoka threshold comparison** - Normalize before comparing
2. **Fix ConfidenceScorer MAX value** - Change 100 to 10000
3. **Clear commands on app switch** - Add clearDynamicCommands() call
4. **Fix CommandRegistry spinlock** - Replace with atomic snapshot pattern

### Phase 2: Confidence Scale Refactor (0-10000)

1. Update `SpeechConfig.confidenceThreshold` to Int 0-10000
2. Update all `ConfidenceScorer` implementations
3. Update validation in both SpeechConfig classes
4. Update all consumers (52 files)
5. Update documentation

### Phase 3: Numeric Commands Enhancement

1. Create `NumberToWords.kt` utility (supports 0 to billions)
2. Update `CommandGenerator.generateNumericCommands()` to register both forms
3. Update `NumberHandler` to use `NumberToWords.parse()`

### Phase 4: Overlay Fixes

1. Add overlay clearing on screen/app change
2. Create `NumberAssignmentManager` for persistent AVID-to-number mapping
3. Improve scroll offset tracking in `BoundsResolver`

### Phase 5: SpeechConfig Consolidation

1. Merge all fields into SpeechRecognition's SpeechConfig
2. Add deprecation typealias in VoiceOSCore
3. Migrate all usages
4. Delete VoiceOSCore's SpeechConfig.kt

### Phase 6: Testing & Validation

1. Unit tests for confidence normalization
2. Unit tests for NumberToWords
3. Integration tests for app switching
4. Manual testing on Device Info, Calculator, Gmail apps

---

## Testing Strategy

### Test Cases

#### Vivoka Confidence
- [ ] Threshold 7000, confidence 7500 → Command executes
- [ ] Threshold 7000, confidence 6500 → Command rejected
- [ ] Threshold 7000, confidence 100 → Command rejected (was passing before!)

#### Numeric Commands
- [ ] Say "1" in Calculator → Button 1 clicked
- [ ] Say "one" in Calculator → Button 1 clicked
- [ ] Say "2" through "9" → Correct buttons clicked

#### Overlay Numbers
- [ ] Switch apps → Old overlay clears
- [ ] Scroll list → Numbers update correctly
- [ ] Say "3" → Item 3 clicked (not item 5)

#### App Switching
- [ ] Switch 5+ apps → Commands still work
- [ ] Rapid app switching → No lockup
- [ ] Return to first app → Commands work

---

## Appendix: Full File List

Files containing `confidenceThreshold` that need review for Phase 2:

```
SpeechRecognition/src/commonMain/kotlin/com/augmentalis/speechrecognition/SpeechConfig.kt
SpeechRecognition/src/androidMain/kotlin/com/augmentalis/speechrecognition/ConfidenceScorer.kt
SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/confidence/ConfidenceScorer.kt
SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaRecognizer.kt
SpeechRecognition/src/androidMain/kotlin/com/augmentalis/speechrecognition/vivoka/VivokaRecognizer.kt
VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/speech/SpeechConfig.kt
VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/CommandWordDetector.kt
VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/speech/SpeechEngineManager.kt
... (52 files total - see grep results)
```

---

## Notes

- The Device Info app issue may be related to rapid screen updates causing event cascade
- Consider adding a "developer mode" confidence display showing raw and normalized values
- Future: Consider per-engine confidence calibration based on usage data
