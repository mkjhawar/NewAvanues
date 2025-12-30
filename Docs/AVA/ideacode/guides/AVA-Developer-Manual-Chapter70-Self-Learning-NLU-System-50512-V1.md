# Developer Manual: Chapter 70 - Self-Learning NLU System

**Version**: 1.0
**Date**: 2025-12-04
**Author**: Manoj Jhawar
**Related ADR**: ADR-013-Self-Learning-NLU-LLM-Teacher

---

## Overview

The Self-Learning NLU System enables AVA to automatically improve its intent recognition by using the LLM as a "teacher" for unrecognized queries. This creates a seamless user experience where users never see the distinction between NLU and LLM processing.

---

## Architecture Components

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AVA SELF-LEARNING SYSTEM                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────┐ │
│  │ IntentRouter│───►│NLUClassifier│───►│ InferenceManager            │ │
│  │             │    │             │    │ (Battery/Thermal Aware)     │ │
│  └─────────────┘    └─────────────┘    └─────────────────────────────┘ │
│         │                  │                        │                   │
│         │                  │                        ▼                   │
│         │                  │           ┌─────────────────────────────┐ │
│         │                  │           │    LOCAL LLM    │  CLOUD LLM │ │
│         │                  │           │    (Gemma 2B)   │  (OpenAI)  │ │
│         │                  │           └─────────────────────────────┘ │
│         │                  │                        │                   │
│         │                  │                        ▼                   │
│         │                  │           ┌─────────────────────────────┐ │
│         │                  │           │   LLMResponseParser         │ │
│         │                  │           │   (Extract intent/vars)     │ │
│         │                  │           └─────────────────────────────┘ │
│         │                  │                        │                   │
│         │                  ▼                        ▼                   │
│         │      ┌─────────────────────────────────────────────────────┐ │
│         │      │              NLU SELF LEARNER                       │ │
│         │      │  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │ │
│         │      │  │TrainExample  │  │ WorkManager  │  │ Embedding │ │ │
│         │      │  │  Repository  │  │   (Deferred) │  │  Compute  │ │ │
│         │      │  └──────────────┘  └──────────────┘  └───────────┘ │ │
│         │      └─────────────────────────────────────────────────────┘ │
│         │                                                               │
└─────────┴───────────────────────────────────────────────────────────────┘
```

### Key Files

| File | Purpose | Module |
|------|---------|--------|
| `IntentRouter.kt` | Route based on confidence | `common/Actions` |
| `IntentClassifier.kt` | NLU classification + embedding | `common/NLU` |
| `InferenceManager.kt` | Battery/thermal-aware switching | `common/LLM` |
| `LLMTeacherPrompt.kt` | System prompt for teaching | `common/LLM` |
| `LLMResponseParser.kt` | Parse LLM output | `common/LLM` |
| `NLUSelfLearner.kt` | Orchestrate learning flow | `common/NLU` |
| `EmbeddingComputeWorker.kt` | Background embedding job | `common/NLU` |
| `CloudLLMProvider.kt` | Cloud LLM interface | `common/LLM` |
| `TrainExample.sq` | Database schema | `common/Data` |

---

## Implementation Guide

### Step 1: Create LLM Teacher Prompt

**File**: `common/LLM/src/main/java/com/augmentalis/ava/features/llm/LLMTeacherPrompt.kt`

```kotlin
package com.augmentalis.ava.features.llm

/**
 * System prompt for LLM-as-Teacher mode.
 * Instructs the LLM to respond AND classify intent simultaneously.
 */
object LLMTeacherPrompt {

    /**
     * Complete list of intent categories the LLM can classify into.
     * Keep in sync with BuiltInIntents and .ava files.
     */
    private val INTENT_CATEGORIES = """
        |INTENT CATEGORIES:
        |Smart Home: control_lights, control_temperature, control_device, control_tv
        |Media: play_music, pause_music, skip_track, volume_control, play_video
        |Productivity: set_alarm, set_reminder, set_timer, add_calendar_event, check_schedule
        |Communication: make_call, send_message, read_messages, check_voicemail
        |Information: check_weather, show_time, search_web, get_directions, find_nearby
        |Math: perform_calculation
        |Notes: create_note, add_to_list, read_notes
        |Apps: open_app, close_app, app_settings
        |System: show_history, new_conversation, device_settings
        |General: general_question (for anything not fitting above)
    """.trimMargin()

    /**
     * System prompt that instructs LLM to:
     * 1. Respond naturally to the user
     * 2. Classify the intent
     * 3. Generate variations
     * 4. Rate confidence
     */
    val SYSTEM_PROMPT = """
        |You are AVA, a helpful AI assistant on Android. When responding to the user, you MUST:
        |
        |1. RESPOND naturally and helpfully to the user's request
        |2. CLASSIFY the intent from the categories below
        |3. GENERATE 3-5 natural variations of the user's phrase
        |4. RATE your confidence in the classification (0.0-1.0)
        |
        |$INTENT_CATEGORIES
        |
        |FORMAT YOUR RESPONSE EXACTLY AS (including brackets):
        |[RESPONSE]
        |Your natural response here...
        |
        |[INTENT]
        |intent_name
        |
        |[VARIATIONS]
        |- variation 1
        |- variation 2
        |- variation 3
        |
        |[CONFIDENCE]
        |0.XX
        |
        |IMPORTANT RULES:
        |- Keep [RESPONSE] conversational and helpful
        |- [INTENT] must be exactly one intent from the list above
        |- [VARIATIONS] should be natural ways to say the same thing
        |- [CONFIDENCE] should be 0.95+ if certain, 0.7-0.94 if likely, <0.7 if unsure
        |- If truly ambiguous, use general_question with lower confidence
    """.trimMargin()

    /**
     * Simplified prompt for low-resource devices.
     * Omits variations to reduce output length.
     */
    val SIMPLE_PROMPT = """
        |You are AVA. Respond naturally, then classify:
        |[RESPONSE] your response [INTENT] intent_name [CONFIDENCE] 0.XX
        |Intents: control_lights, control_temperature, play_music, pause_music,
        |set_alarm, set_reminder, check_weather, show_time, perform_calculation,
        |make_call, send_message, open_app, general_question
    """.trimMargin()
}
```

### Step 2: Create LLM Response Parser

**File**: `common/LLM/src/main/java/com/augmentalis/ava/features/llm/LLMResponseParser.kt`

```kotlin
package com.augmentalis.ava.features.llm

import timber.log.Timber

/**
 * Result from parsing LLM teacher response.
 */
data class LLMTeacherResult(
    val response: String,
    val intent: String,
    val variations: List<String>,
    val confidence: Float
) {
    /**
     * Check if this result is valid for teaching.
     * Requires response, valid intent, and reasonable confidence.
     */
    fun isValidForTeaching(): Boolean {
        return response.isNotBlank() &&
               intent.isNotBlank() &&
               intent != "unknown" &&
               confidence >= 0.5f
    }
}

/**
 * Parser for LLM teacher response format.
 *
 * Expected format:
 * ```
 * [RESPONSE]
 * The response text...
 *
 * [INTENT]
 * intent_name
 *
 * [VARIATIONS]
 * - variation 1
 * - variation 2
 *
 * [CONFIDENCE]
 * 0.95
 * ```
 */
object LLMResponseParser {

    private val RESPONSE_REGEX = Regex(
        """\[RESPONSE\](.*?)(?:\[INTENT\]|\z)""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val INTENT_REGEX = Regex(
        """\[INTENT\]\s*(\w+)""",
        RegexOption.IGNORE_CASE
    )

    private val VARIATIONS_REGEX = Regex(
        """\[VARIATIONS\](.*?)(?:\[CONFIDENCE\]|\z)""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val CONFIDENCE_REGEX = Regex(
        """\[CONFIDENCE\]\s*([\d.]+)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parse LLM output into structured result.
     *
     * @param llmOutput Raw LLM output string
     * @return Parsed result or null if parsing fails
     */
    fun parse(llmOutput: String): LLMTeacherResult? {
        if (llmOutput.isBlank()) {
            Timber.w("Empty LLM output")
            return null
        }

        // Extract response
        val response = RESPONSE_REGEX.find(llmOutput)
            ?.groupValues?.get(1)
            ?.trim()

        if (response.isNullOrBlank()) {
            // Fallback: if no [RESPONSE] tag, use entire output as response
            Timber.d("No [RESPONSE] tag found, using fallback")
            return LLMTeacherResult(
                response = llmOutput.trim(),
                intent = "general_question",
                variations = emptyList(),
                confidence = 0.5f
            )
        }

        // Extract intent
        val intent = INTENT_REGEX.find(llmOutput)
            ?.groupValues?.get(1)
            ?.trim()
            ?.lowercase()
            ?: "general_question"

        // Extract variations
        val variationsBlock = VARIATIONS_REGEX.find(llmOutput)
            ?.groupValues?.get(1)
            ?: ""

        val variations = variationsBlock
            .lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .filter { it.isNotBlank() && it.length > 3 }
            .take(5) // Max 5 variations

        // Extract confidence
        val confidence = CONFIDENCE_REGEX.find(llmOutput)
            ?.groupValues?.get(1)
            ?.toFloatOrNull()
            ?.coerceIn(0f, 1f)
            ?: 0.8f // Default to 0.8 if not specified

        val result = LLMTeacherResult(
            response = response,
            intent = intent,
            variations = variations,
            confidence = confidence
        )

        Timber.d("Parsed LLM response: intent=$intent, confidence=$confidence, variations=${variations.size}")
        return result
    }

    /**
     * Extract just the response portion for display.
     * Strips all metadata tags.
     */
    fun extractResponseOnly(llmOutput: String): String {
        val parsed = parse(llmOutput)
        return parsed?.response ?: llmOutput
            .replace(Regex("""\[RESPONSE\]|\[INTENT\].*|\[VARIATIONS\].*|\[CONFIDENCE\].*""", RegexOption.DOT_MATCHES_ALL), "")
            .trim()
    }
}
```

### Step 3: Create Inference Manager

**File**: `common/LLM/src/main/java/com/augmentalis/ava/features/llm/InferenceManager.kt`

```kotlin
package com.augmentalis.ava.features.llm

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages inference backend selection based on device state.
 *
 * Monitors:
 * - Battery level and charging status
 * - Thermal status (Android 10+)
 * - Network connectivity
 * - Cloud LLM configuration
 *
 * Decision logic prioritizes:
 * 1. User experience (fast responses)
 * 2. Battery life (prefer cloud when low)
 * 3. Device health (prevent overheating)
 */
@Singleton
class InferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Available inference backends.
     */
    enum class InferenceBackend {
        LOCAL_LLM,   // On-device Gemma 2B
        CLOUD_LLM,   // OpenAI/Groq/etc
        QUEUED,      // Defer until conditions improve
        NLU_ONLY     // Use NLU result even if low confidence
    }

    /**
     * Current device state snapshot.
     */
    data class DeviceState(
        val batteryPercent: Int,
        val isCharging: Boolean,
        val thermalStatus: Int,
        val hasNetwork: Boolean,
        val hasCloudConfig: Boolean,
        val isLowPowerMode: Boolean
    ) {
        val isBatteryLow: Boolean get() = batteryPercent < 30 && !isCharging
        val isBatteryCritical: Boolean get() = batteryPercent < 15 && !isCharging
        val isThermalThrottling: Boolean get() = thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        val isThermalCritical: Boolean get() = thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE
    }

    /**
     * Select optimal inference backend based on current device state.
     *
     * Priority:
     * 1. Thermal critical → QUEUED (prevent damage)
     * 2. Thermal throttling + has cloud → CLOUD_LLM
     * 3. Battery critical + has cloud → CLOUD_LLM
     * 4. Battery low + has cloud → CLOUD_LLM
     * 5. Normal conditions → LOCAL_LLM
     * 6. No cloud + struggling → LOCAL_LLM (best effort)
     */
    fun selectBackend(): InferenceBackend {
        val state = getDeviceState()
        Timber.d("Device state: battery=${state.batteryPercent}%, charging=${state.isCharging}, thermal=${state.thermalStatus}")

        return when {
            // Critical thermal - cannot run local inference
            state.isThermalCritical -> {
                Timber.w("Thermal critical - queuing request")
                if (state.hasNetwork && state.hasCloudConfig) {
                    InferenceBackend.CLOUD_LLM
                } else {
                    InferenceBackend.QUEUED
                }
            }

            // Thermal throttling - prefer cloud
            state.isThermalThrottling && state.hasNetwork && state.hasCloudConfig -> {
                Timber.i("Thermal throttling - using cloud LLM")
                InferenceBackend.CLOUD_LLM
            }

            // Battery critical - must use cloud or queue
            state.isBatteryCritical -> {
                if (state.hasNetwork && state.hasCloudConfig) {
                    Timber.i("Battery critical - using cloud LLM")
                    InferenceBackend.CLOUD_LLM
                } else {
                    Timber.w("Battery critical, no cloud - using NLU only")
                    InferenceBackend.NLU_ONLY
                }
            }

            // Battery low - prefer cloud if available
            state.isBatteryLow && state.hasNetwork && state.hasCloudConfig -> {
                Timber.i("Battery low - using cloud LLM")
                InferenceBackend.CLOUD_LLM
            }

            // Low power mode enabled - prefer cloud
            state.isLowPowerMode && state.hasNetwork && state.hasCloudConfig -> {
                Timber.i("Low power mode - using cloud LLM")
                InferenceBackend.CLOUD_LLM
            }

            // Normal conditions - use local
            else -> {
                Timber.d("Normal conditions - using local LLM")
                InferenceBackend.LOCAL_LLM
            }
        }
    }

    /**
     * Get current device state snapshot.
     */
    fun getDeviceState(): DeviceState {
        // Battery info
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, 50) ?: 50
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = (level * 100) / scale

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        // Thermal status (Android 10+)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

        // Network connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val hasNetwork = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // Cloud config (check SharedPreferences)
        val prefs = context.getSharedPreferences("ava_settings", Context.MODE_PRIVATE)
        val hasCloudConfig = prefs.getString("cloud_api_key", "")?.isNotBlank() == true

        // Low power mode
        val isLowPowerMode = powerManager.isPowerSaveMode

        return DeviceState(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            thermalStatus = thermalStatus,
            hasNetwork = hasNetwork,
            hasCloudConfig = hasCloudConfig,
            isLowPowerMode = isLowPowerMode
        )
    }

    /**
     * Check if cloud LLM should be suggested to user.
     * Returns true if device is frequently falling back or struggling.
     */
    fun shouldSuggestCloudSetup(): Boolean {
        val state = getDeviceState()
        return !state.hasCloudConfig &&
               (state.isBatteryLow || state.isThermalThrottling)
    }

    /**
     * Get user-friendly message about current inference mode.
     */
    fun getInferenceStatusMessage(): String? {
        val state = getDeviceState()
        return when {
            state.isThermalCritical -> "Device is too warm. Cooling down..."
            state.isThermalThrottling && !state.hasCloudConfig ->
                "Device is warm. Consider adding Cloud AI for better performance."
            state.isBatteryCritical && !state.hasCloudConfig ->
                "Battery very low. Responses may be slower."
            state.isBatteryLow && !state.hasCloudConfig ->
                "Battery low. Consider adding Cloud AI backup."
            else -> null
        }
    }
}
```

### Step 4: Create NLU Self-Learner

**File**: `common/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/NLUSelfLearner.kt`

**Note**: NLUSelfLearner uses its own `LLMTeachingInput` data class to avoid coupling with the LLM module. ChatViewModel converts LLMTeacherResult to simple parameters.

```kotlin
package com.augmentalis.ava.features.nlu

import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates NLU self-learning from LLM classifications.
 *
 * When LLM processes a query:
 * 1. Computes embedding using IntentClassifier
 * 2. Saves utterance + intent + embedding to TrainExample table
 * 3. Optionally saves variations (if high confidence)
 * 4. Schedules deferred embedding computation for low-battery scenarios
 */
@Singleton
class NLUSelfLearner @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val workManager: WorkManager
) {
    companion object {
        const val VARIATION_CONFIDENCE_THRESHOLD = 0.85f
        const val MIN_CONFIDENCE_THRESHOLD = 0.60f
        const val MAX_UTTERANCE_LENGTH = 500
        const val WORK_TAG_EMBEDDING = "embedding_compute"

        val EXCLUDED_INTENTS = setOf(
            "unknown", "teach_ava", "clarify_request", "general_question"
        )
    }

    /**
     * Input data for learning from LLM classification.
     * Avoids coupling NLU module to LLM module.
     */
    data class LLMTeachingInput(
        val intent: String,
        val confidence: Float,
        val variations: List<String> = emptyList()
    ) {
        fun isValidForTeaching(): Boolean {
            return intent.isNotBlank() &&
                   intent != "unknown" &&
                   intent != "clarify_request" &&
                   confidence >= MIN_CONFIDENCE_THRESHOLD
        }
    }

    /**
     * Learn from LLM classification (simple parameters).
     * Preferred API - avoids module dependency.
     */
    suspend fun learnFromLLM(
        utterance: String,
        intent: String,
        confidence: Float,
        variations: List<String> = emptyList()
    ): Boolean = learnFromLLM(utterance, LLMTeachingInput(intent, confidence, variations))

    /**
     * Learn from LLM classification (structured input).
     */
    suspend fun learnFromLLM(
        utterance: String,
        input: LLMTeachingInput
    ): Boolean = withContext(Dispatchers.IO) {
        if (!input.isValidForTeaching() ||
            input.intent in EXCLUDED_INTENTS ||
            utterance.length > MAX_UTTERANCE_LENGTH) {
            return@withContext false
        }

        try {
            // Check if already exists
            if (intentClassifier.findEmbeddingByUtterance(utterance) != null) {
                return@withContext false
            }

            // Try immediate embedding computation
            val embedding = intentClassifier.computeEmbedding(utterance)
            if (embedding != null) {
                intentClassifier.saveTrainedEmbedding(
                    utterance = utterance,
                    intent = input.intent,
                    embedding = embedding,
                    source = "llm_auto",
                    confidence = input.confidence
                )
            } else {
                // Schedule for later if model not ready
                scheduleEmbeddingComputation(utterance, input.intent, input.confidence)
            }

            // Save variations if high confidence
            if (input.confidence >= VARIATION_CONFIDENCE_THRESHOLD) {
                input.variations.forEach { variation ->
                    if (intentClassifier.findEmbeddingByUtterance(variation) == null) {
                        scheduleEmbeddingComputation(variation, input.intent, input.confidence * 0.9f)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to learn from LLM")
            false
        }
    }

    private fun scheduleEmbeddingComputation(utterance: String, intent: String, confidence: Float) {
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()

        val inputData = workDataOf(
            EmbeddingComputeWorker.KEY_UTTERANCE to utterance,
            EmbeddingComputeWorker.KEY_INTENT to intent,
            EmbeddingComputeWorker.KEY_CONFIDENCE to confidence
        )

        val workRequest = OneTimeWorkRequestBuilder<EmbeddingComputeWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG_EMBEDDING)
            .build()

        workManager.enqueueUniqueWork("embedding_${utterance.hashCode()}", ExistingWorkPolicy.KEEP, workRequest)
    }

    suspend fun confirmIntent(utterance: String) = withContext(Dispatchers.IO) {
        intentClassifier.confirmTrainedEmbedding(utterance)
    }

    suspend fun correctIntent(utterance: String, correctIntent: String) = withContext(Dispatchers.IO) {
        intentClassifier.deleteTrainedEmbedding(utterance)
        scheduleEmbeddingComputation(utterance, correctIntent, 1.0f)
    }

    suspend fun getStats(): LearningStats = withContext(Dispatchers.IO) {
        val stats = intentClassifier.getLearningStats()
        LearningStats(stats.total, stats.llmAuto, stats.llmVariation, stats.user, stats.confirmed)
    }

    data class LearningStats(
        val totalExamples: Int,
        val llmAutoTaught: Int,
        val llmVariations: Int,
        val userTaught: Int,
        val userConfirmed: Int
    )
}
```

**Usage from ChatViewModel**:
```kotlin
// ChatViewModel calls with simple parameters (no LLM module import needed)
val llmTeacherResult = LLMResponseParser.parse(rawResponseContent)
if (llmTeacherResult != null) {
    nluSelfLearner.learnFromLLM(
        utterance = userInput,
        intent = llmTeacherResult.intent,
        confidence = llmTeacherResult.confidence,
        variations = llmTeacherResult.variations
    )
}
```

### Step 5: Create Embedding Compute Worker

**File**: `common/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/EmbeddingComputeWorker.kt`

```kotlin
package com.augmentalis.ava.features.nlu

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker for computing embeddings of new training examples.
 *
 * Runs with WorkManager constraints:
 * - Battery not low
 * - Device idle (optional)
 * - Exponential backoff on failure
 *
 * Computes BERT embedding using IntentClassifier's ONNX model,
 * then saves to TrainExample table for future NLU matching.
 */
@HiltWorker
class EmbeddingComputeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val intentClassifier: IntentClassifier
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_UTTERANCE = "utterance"
        const val KEY_INTENT = "intent"
        const val MAX_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        val utterance = inputData.getString(KEY_UTTERANCE)
        val intent = inputData.getString(KEY_INTENT)

        if (utterance.isNullOrBlank() || intent.isNullOrBlank()) {
            Timber.w("Missing utterance or intent in work input")
            return Result.failure()
        }

        return try {
            Timber.d("Computing embedding for: '$utterance' → $intent")

            // Compute embedding using BERT model
            val embedding = intentClassifier.computeEmbedding(utterance)

            if (embedding == null) {
                Timber.w("Failed to compute embedding - model not loaded")
                return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }

            // Save embedding to database
            val saved = intentClassifier.saveTrainedEmbedding(
                utterance = utterance,
                intent = intent,
                embedding = embedding
            )

            if (saved) {
                Timber.i("Saved embedding for '$utterance' → $intent")
                Result.success()
            } else {
                Timber.w("Failed to save embedding to database")
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error computing embedding")
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}
```

---

## Database Schema Updates

### TrainExample Table

**File**: `common/core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/TrainExample.sq`

```sql
-- Training examples for NLU self-learning
-- Sources:
--   user: Manual user teaching
--   llm_auto: Automatic LLM classification
--   llm_variation: LLM-generated variations
--   llm_confirmed: LLM + user confirmation

CREATE TABLE TrainExample (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    utterance TEXT NOT NULL,
    intent TEXT NOT NULL,
    source TEXT NOT NULL DEFAULT 'user',
    confidence REAL NOT NULL DEFAULT 1.0,
    embedding BLOB,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    user_confirmed INTEGER NOT NULL DEFAULT 0,
    times_matched INTEGER NOT NULL DEFAULT 0
);

-- Unique constraint on utterance (no duplicates)
CREATE UNIQUE INDEX idx_train_utterance ON TrainExample(utterance);

-- Fast lookups
CREATE INDEX idx_train_intent ON TrainExample(intent);
CREATE INDEX idx_train_source ON TrainExample(source);
CREATE INDEX idx_train_confidence ON TrainExample(confidence);

-- Queries
selectAll:
SELECT * FROM TrainExample ORDER BY created_at DESC;

selectByIntent:
SELECT * FROM TrainExample WHERE intent = ? ORDER BY confidence DESC;

selectBySource:
SELECT * FROM TrainExample WHERE source = ?;

findByUtterance:
SELECT * FROM TrainExample WHERE utterance = ? LIMIT 1;

selectWithEmbeddings:
SELECT * FROM TrainExample WHERE embedding IS NOT NULL;

insert:
INSERT OR IGNORE INTO TrainExample (utterance, intent, source, confidence)
VALUES (?, ?, ?, ?);

updateEmbedding:
UPDATE TrainExample SET embedding = ? WHERE utterance = ?;

updateConfidence:
UPDATE TrainExample SET confidence = ? WHERE id = ?;

updateIntent:
UPDATE TrainExample SET intent = ?, source = ? WHERE utterance = ?;

confirmUtterance:
UPDATE TrainExample SET user_confirmed = 1, source = 'llm_confirmed' WHERE utterance = ?;

incrementMatched:
UPDATE TrainExample SET times_matched = times_matched + 1 WHERE id = ?;

countAll:
SELECT COUNT(*) FROM TrainExample;

countBySource:
SELECT COUNT(*) FROM TrainExample WHERE source = ?;

countConfirmed:
SELECT COUNT(*) FROM TrainExample WHERE user_confirmed = 1;

deleteOldLowConfidence:
DELETE FROM TrainExample
WHERE source = 'llm_auto'
AND confidence < 0.7
AND user_confirmed = 0
AND created_at < strftime('%s', 'now') - 2592000; -- 30 days
```

---

## Configuration

### Cloud LLM Settings

**SharedPreferences keys** (`ava_settings`):

| Key | Type | Description |
|-----|------|-------------|
| `cloud_provider` | String | "none", "openai", "groq", "anthropic", "custom" |
| `cloud_api_key` | String | Encrypted API key |
| `cloud_api_url` | String | Custom API URL (for "custom" provider) |
| `cloud_model` | String | Model ID (e.g., "gpt-4o-mini") |
| `cloud_enabled` | Boolean | Enable/disable cloud fallback |

### Thresholds

| Setting | Default | Description |
|---------|---------|-------------|
| `nlu_confidence_threshold` | 0.75 | Above this = NLU direct |
| `llm_teaching_threshold` | 0.60 | Above this = save to DB |
| `variation_threshold` | 0.85 | Above this = save variations |
| `battery_low_threshold` | 30 | % below = prefer cloud |
| `battery_critical_threshold` | 15 | % below = must use cloud |

---

## Testing

### Unit Tests

```kotlin
@Test
fun `LLMResponseParser extracts all fields`() {
    val input = """
        [RESPONSE]
        Playing smooth jazz now.
        [INTENT]
        play_music
        [VARIATIONS]
        - play jazz
        - jazz music please
        [CONFIDENCE]
        0.92
    """.trimIndent()

    val result = LLMResponseParser.parse(input)

    assertNotNull(result)
    assertEquals("Playing smooth jazz now.", result.response)
    assertEquals("play_music", result.intent)
    assertEquals(2, result.variations.size)
    assertEquals(0.92f, result.confidence, 0.01f)
}

@Test
fun `InferenceManager selects cloud when battery low`() {
    // Mock device state with 20% battery
    val manager = InferenceManager(mockContext)

    val backend = manager.selectBackend()

    assertEquals(InferenceManager.InferenceBackend.CLOUD_LLM, backend)
}

@Test
fun `NLUSelfLearner excludes general_question intent`() = runTest {
    val learner = NLUSelfLearner(mockRepo, mockWorkManager)
    val result = LLMTeacherResult(
        response = "test",
        intent = "general_question",
        variations = emptyList(),
        confidence = 0.95f
    )

    val saved = learner.learnFromLLM("test query", result)

    assertFalse(saved)
}
```

### Integration Tests

```kotlin
@Test
fun `End-to-end learning flow`() = runTest {
    // 1. Send unknown query
    viewModel.processUserInput("play some smooth jazz")

    // 2. Wait for LLM response
    advanceUntilIdle()

    // 3. Verify TrainExample saved
    val examples = repository.selectByIntent("play_music")
    assertTrue(examples.any { it.utterance == "play some smooth jazz" })

    // 4. Send same query again
    viewModel.processUserInput("play some smooth jazz")

    // 5. Verify NLU now recognizes it
    val state = viewModel.uiState.value
    assertTrue(state.lastNluConfidence > 0.8f)
}
```

---

## Monitoring & Debugging

### Logcat Tags

| Tag | Purpose |
|-----|---------|
| `NLUSelfLearner` | Learning flow |
| `LLMResponseParser` | Parsing results |
| `InferenceManager` | Backend selection |
| `EmbeddingComputeWorker` | Background jobs |
| `IntentClassifier` | NLU classification |

### Debug Commands

```bash
# View learning logs
adb logcat -s NLUSelfLearner:V LLMResponseParser:V

# Check pending embedding jobs
adb shell dumpsys jobscheduler | grep -A 5 "embedding_compute"

# Database inspection
adb shell run-as com.augmentalis.ava sqlite3 databases/ava.db \
    "SELECT utterance, intent, source, confidence FROM TrainExample ORDER BY created_at DESC LIMIT 10;"
```

---

## Performance Considerations

### Battery Impact

| Operation | Impact | Mitigation |
|-----------|--------|------------|
| NLU classification | ~1mAh per 100 queries | Use NPU/NNAPI |
| Local LLM | ~50mAh per query | Battery-aware switching |
| Cloud LLM | Minimal | Network only |
| Embedding compute | ~5mAh per embedding | Deferred via WorkManager |

### Memory Usage

| Component | Memory | Notes |
|-----------|--------|-------|
| NLU model (ONNX) | ~50MB | Loaded once |
| LLM model (Gemma 2B) | ~1.5GB | Lazy load |
| Embedding cache | ~10MB | 10K embeddings |

---

## References

- ADR-013: Self-Learning NLU with LLM-as-Teacher
- ADR-008: Hardware-Aware Inference Backend
- ADR-014: Flow Gaps Fix (Confidence, InferenceManager, Accessibility)
- [Android WorkManager](https://developer.android.com/develop/background-work/background-tasks/optimize-battery)
- [ONNX Runtime Android](https://onnxruntime.ai/docs/execution-providers/NNAPI-ExecutionProvider.html)

---

## ADR-014: Flow Gaps Fix

### Overview

ADR-014 addresses integration gaps in the Self-Learning NLU system, ensuring proper wiring between components.

### Phase 1: Unified Confidence Threshold System

**Problem:** Hardcoded confidence thresholds scattered across multiple files.

**Solution:** Centralized threshold via `ChatPreferences.getConfidenceThreshold()`.

**Files Modified:**

| File | Change |
|------|--------|
| `HybridResponseGenerator.kt` | Uses `chatPreferences.getConfidenceThreshold()` |
| `ChatViewModel.kt` | Uses `chatPreferences.getConfidenceThreshold()` |

**Code Example:**
```kotlin
// Before (hardcoded)
private const val CONFIDENCE_THRESHOLD = 0.75f

// After (centralized)
val currentThreshold = chatPreferences.getConfidenceThreshold()
if (confidenceScore > currentThreshold) {
    // High confidence - use NLU result
}
```

### Phase 2: Wire InferenceManager

**Problem:** InferenceManager exists but was not wired into HybridResponseGenerator.

**Solution:** Inject InferenceManager via Hilt and use for backend selection.

**Files Modified:**

| File | Change |
|------|--------|
| `HybridResponseGenerator.kt` | Added `inferenceManager` parameter |
| `LLMModule.kt` | Updated provider to pass `InferenceManager` |

**Code Example:**
```kotlin
// HybridResponseGenerator now uses InferenceManager
@Singleton
class HybridResponseGenerator @Inject constructor(
    private val chatPreferences: ChatPreferences,
    private val inferenceManager: InferenceManager,  // NEW
    private val llmService: LLMService
) {
    suspend fun generateResponse(input: String): Response {
        when (inferenceManager.selectBackend()) {
            InferenceBackend.LOCAL_LLM -> useLocalLLM()
            InferenceBackend.CLOUD_LLM -> useCloudLLM()
            InferenceBackend.QUEUED -> deferResponse()
            InferenceBackend.NLU_ONLY -> useNluOnly()
        }
    }
}
```

### Dependency Injection Updates

**AppModule.kt Additions:**

```kotlin
@Provides
@Singleton
fun provideInferenceManager(
    @ApplicationContext context: Context
): InferenceManager {
    return InferenceManager(context)
}

@Provides
@Singleton
fun provideNLUSelfLearner(
    intentClassifier: IntentClassifier,
    workManager: WorkManager
): NLUSelfLearner {
    return NLUSelfLearner(intentClassifier, workManager)
}
```

### Verification

Run these commands to verify the integration:

```bash
# Check InferenceManager is wired
adb logcat -s InferenceManager:V | grep "selectBackend"

# Check confidence threshold
adb logcat -s ChatViewModel:V | grep "threshold"
```

---

**Created by Manoj Jhawar, manoj@ideahq.net**
