/**
 * NluIntegration - Bridges UnifiedNluService with CommandManager
 *
 * STUBBED: NLU module not yet integrated into VoiceOS monorepo.
 * This stub provides a no-op implementation that allows CommandManager
 * to compile and run without NLU functionality.
 *
 * TODO: Enable when NLU module is set up in monorepo
 *
 * Created: 2025-12-07
 * Stubbed: 2025-12-16
 */

package com.augmentalis.commandmanager.nlu

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandSource

/**
 * NLU Integration for VoiceOS CommandManager.
 *
 * STUBBED: Returns no matches. Enable NLU module for full functionality.
 */
class NluIntegration(
    private val context: Context
) {
    companion object {
        private const val TAG = "NluIntegration"

        @Volatile
        private var instance: NluIntegration? = null

        fun getInstance(context: Context): NluIntegration {
            return instance ?: synchronized(this) {
                instance ?: NluIntegration(context.applicationContext).also { instance = it }
            }
        }
    }

    private var isInitialized = false

    /**
     * Initialize NLU service (stubbed - always succeeds).
     */
    suspend fun initialize(): InitResult {
        Log.w(TAG, "NLU integration is STUBBED - no NLU processing available")
        isInitialized = true
        return InitResult(success = true, intentCount = 0, error = null, stats = null)
    }

    /**
     * Check if NLU is initialized
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Classify user input (stubbed - returns no match).
     */
    fun classify(input: String): NluClassificationResult {
        Log.d(TAG, "NLU classify (stubbed): '$input' -> no match")
        return NluClassificationResult(
            hasMatch = false,
            matches = emptyList(),
            method = MatchMethod.UNKNOWN,
            confidence = 0f,
            processingTimeMs = 0
        )
    }

    /**
     * Fast classification (stubbed - returns null).
     */
    fun classifyFast(input: String): NluMatch? {
        return null
    }

    /**
     * Check if exact match exists (stubbed - always false).
     */
    fun hasExactMatch(input: String): Boolean = false

    /**
     * Load intents from AVU content (stubbed - no-op).
     */
    suspend fun loadFromAvu(avuContent: String, persist: Boolean = true) {
        Log.w(TAG, "loadFromAvu (stubbed): NLU module not available")
    }

    /**
     * Load VoiceOS commands into NLU (stubbed - no-op).
     */
    suspend fun loadFromVoiceOSCommands(commands: List<VoiceOSCommandDef>) {
        Log.w(TAG, "loadFromVoiceOSCommands (stubbed): NLU module not available")
    }

    /**
     * Clear all NLU data (stubbed - no-op).
     */
    suspend fun clear() {
        isInitialized = false
        Log.d(TAG, "NLU data cleared (stubbed)")
    }

    /**
     * Refresh NLU index (stubbed - no-op).
     */
    suspend fun refresh() {
        isInitialized = true
        Log.d(TAG, "NLU index refreshed (stubbed)")
    }
}

// Stub types for NLU module

/**
 * NLU initialization result
 */
data class InitResult(
    val success: Boolean,
    val intentCount: Int,
    val error: String?,
    val stats: InitStats?
)

data class InitStats(
    val patternCount: Int = 0,
    val embeddedCount: Int = 0,
    val semanticAvailable: Boolean = false
)

/**
 * Match method enumeration
 */
enum class MatchMethod {
    EXACT,
    PATTERN,
    FUZZY,
    SEMANTIC,
    UNKNOWN
}

/**
 * NLU classification result
 */
data class NluClassificationResult(
    val hasMatch: Boolean,
    val matches: List<NluMatch>,
    val method: MatchMethod,
    val confidence: Float,
    val processingTimeMs: Long
) {
    val topMatch: NluMatch? get() = matches.firstOrNull()

    fun toCommand(originalText: String, originalConfidence: Float = 0.9f): Command? {
        val match = topMatch ?: return null

        return Command(
            id = match.intentId,
            text = originalText,
            source = CommandSource.VOICE,
            timestamp = System.currentTimeMillis(),
            confidence = confidence.coerceIn(0f, 1f)
        )
    }

    val commandId: String? get() = topMatch?.intentId
    val isHighConfidence: Boolean get() = confidence >= 0.85f
}

/**
 * NLU match result
 */
data class NluMatch(
    val intentId: String,
    val canonicalPhrase: String,
    val category: String,
    val actionId: String,
    val score: Float,
    val matchedPhrase: String?,
    val method: MatchMethod
) {
    fun toCommand(originalText: String): Command {
        return Command(
            id = intentId,
            text = originalText,
            source = CommandSource.VOICE,
            timestamp = System.currentTimeMillis(),
            confidence = score
        )
    }
}

/**
 * VoiceOS command definition for loading into NLU
 */
data class VoiceOSCommandDef(
    val id: String,
    val patterns: List<String>,
    val category: String,
    val locale: String = "en-US"
)
