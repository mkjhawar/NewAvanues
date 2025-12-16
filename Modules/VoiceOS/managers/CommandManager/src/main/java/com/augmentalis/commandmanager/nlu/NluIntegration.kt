/**
 * NluIntegration - Bridges UnifiedNluService with CommandManager
 *
 * Provides a clean integration layer between the shared NLU module
 * and VoiceOS CommandManager. Handles:
 * - Service initialization and lifecycle
 * - Intent-to-Command conversion
 * - Classification result mapping
 *
 * Created: 2025-12-07
 */

package com.augmentalis.commandmanager.nlu

import android.content.Context
import android.util.Log
import com.augmentalis.shared.nlu.classifier.ClassificationResult
import com.augmentalis.shared.nlu.model.IntentMatch
import com.augmentalis.shared.nlu.model.IntentSource
import com.augmentalis.shared.nlu.model.MatchMethod
import com.augmentalis.shared.nlu.model.UnifiedIntent
import com.augmentalis.shared.nlu.repository.IntentRepositoryFactory
import com.augmentalis.shared.nlu.service.InitResult
import com.augmentalis.shared.nlu.service.UnifiedNluService
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandSource

/**
 * NLU Integration for VoiceOS CommandManager.
 *
 * Usage:
 * ```kotlin
 * val nlu = NluIntegration(context)
 * nlu.initialize()
 * val result = nlu.classify("go back")
 * if (result.hasMatch) {
 *     val command = result.toCommand()
 *     commandManager.executeCommand(command)
 * }
 * ```
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

    // Unified NLU service
    private val nluService: UnifiedNluService by lazy {
        val repository = IntentRepositoryFactory.create(context)
        UnifiedNluService(repository)
    }

    private var isInitialized = false

    /**
     * Initialize NLU service.
     *
     * Loads intents from database and indexes them for classification.
     * Should be called during CommandManager initialization.
     *
     * @return InitResult with success status and intent count
     */
    suspend fun initialize(): InitResult {
        Log.i(TAG, "Initializing NLU integration...")

        val result = nluService.initialize()

        if (result.success) {
            isInitialized = true
            Log.i(TAG, "✅ NLU initialized: ${result.intentCount} intents indexed")
            result.stats?.let { stats ->
                Log.d(TAG, "   Patterns: ${stats.patternCount}")
                Log.d(TAG, "   Embedded: ${stats.embeddedCount}")
                Log.d(TAG, "   Semantic: ${stats.semanticAvailable}")
            }
        } else {
            Log.e(TAG, "❌ NLU initialization failed: ${result.error}")
        }

        return result
    }

    /**
     * Check if NLU is initialized
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Classify user input and return NLU result.
     *
     * Uses hybrid classification: pattern → fuzzy → semantic.
     *
     * @param input User input text (e.g., "go back", "scroll down")
     * @return NluClassificationResult with matches and confidence
     */
    fun classify(input: String): NluClassificationResult {
        if (!isInitialized) {
            Log.w(TAG, "NLU not initialized, falling back to basic matching")
            return NluClassificationResult(
                hasMatch = false,
                matches = emptyList(),
                method = MatchMethod.UNKNOWN,
                confidence = 0f,
                processingTimeMs = 0
            )
        }

        val result = nluService.classify(input)

        Log.d(TAG, "Classification result for '$input': ${result.topMatch?.intent?.id ?: "none"} " +
                "(${result.method}, confidence=${result.confidence})")

        return NluClassificationResult(
            hasMatch = result.hasMatch,
            matches = result.matches.map { NluMatch.from(it) },
            method = result.method,
            confidence = result.confidence,
            processingTimeMs = result.processingTimeMs
        )
    }

    /**
     * Fast classification using pattern matching only.
     *
     * Use for high-frequency commands where speed is critical.
     *
     * @param input User input text
     * @return IntentMatch or null if no match
     */
    fun classifyFast(input: String): NluMatch? {
        if (!isInitialized) return null

        return nluService.classifyFast(input)?.let { NluMatch.from(it) }
    }

    /**
     * Check if exact match exists.
     *
     * @param input User input text
     * @return true if exact pattern match found
     */
    fun hasExactMatch(input: String): Boolean {
        if (!isInitialized) return false
        return nluService.hasExactMatch(input)
    }

    /**
     * Load intents from AVU content.
     *
     * Use to load additional intent definitions at runtime.
     *
     * @param avuContent AVU file content
     * @param persist Save to database if true
     */
    suspend fun loadFromAvu(avuContent: String, persist: Boolean = true) {
        Log.i(TAG, "Loading intents from AVU content...")
        val result = nluService.loadFromAvu(avuContent, persist)

        if (result.isSuccess) {
            Log.i(TAG, "✅ Loaded ${result.intents.size} intents from AVU")
            isInitialized = true
        } else {
            Log.e(TAG, "❌ Failed to load AVU: ${result.errors.joinToString()}")
        }
    }

    /**
     * Load VoiceOS-specific intents from command definitions.
     *
     * Converts existing VoiceOS command definitions to UnifiedIntent format
     * and loads them into the NLU service.
     *
     * @param commands List of command definitions from CommandLoader
     */
    suspend fun loadFromVoiceOSCommands(commands: List<VoiceOSCommandDef>) {
        Log.i(TAG, "Loading ${commands.size} VoiceOS commands into NLU...")

        val intents = commands.map { cmd ->
            UnifiedIntent(
                id = cmd.id,
                canonicalPhrase = cmd.patterns.firstOrNull() ?: cmd.id,
                patterns = cmd.patterns,
                synonyms = emptyList(),
                embedding = null,
                category = cmd.category,
                actionId = cmd.id,
                priority = 50, // Default priority
                locale = cmd.locale,
                source = IntentSource.VOICEOS
            )
        }

        nluService.saveIntents(intents)
        nluService.refresh()
        isInitialized = true

        Log.i(TAG, "✅ Loaded ${intents.size} VoiceOS intents")
    }

    /**
     * Save a list of intents to the NLU service
     */
    private suspend fun UnifiedNluService.saveIntents(intents: List<UnifiedIntent>) {
        intents.forEach { saveIntent(it) }
    }

    /**
     * Clear all NLU data
     */
    suspend fun clear() {
        nluService.clear()
        isInitialized = false
        Log.i(TAG, "NLU data cleared")
    }

    /**
     * Refresh NLU index from repository
     */
    suspend fun refresh() {
        nluService.refresh()
        isInitialized = true
        Log.i(TAG, "NLU index refreshed")
    }
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
    /**
     * Get top match
     */
    val topMatch: NluMatch? get() = matches.firstOrNull()

    /**
     * Convert top match to Command for CommandManager
     */
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

    /**
     * Get command ID of top match
     */
    val commandId: String? get() = topMatch?.intentId

    /**
     * Check if result is high confidence
     */
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
    companion object {
        fun from(match: IntentMatch): NluMatch {
            return NluMatch(
                intentId = match.intent.id,
                canonicalPhrase = match.intent.canonicalPhrase,
                category = match.intent.category,
                actionId = match.intent.actionId,
                score = match.score,
                matchedPhrase = match.matchedPhrase,
                method = match.method
            )
        }
    }

    /**
     * Convert to Command
     */
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
