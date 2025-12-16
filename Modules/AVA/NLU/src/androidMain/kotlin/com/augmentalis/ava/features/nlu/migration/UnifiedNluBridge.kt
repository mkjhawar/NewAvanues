/**
 * UnifiedNluBridge - Bridges AVA's IntentClassifier with Shared NLU
 *
 * Provides integration between AVA's BERT-based IntentClassifier and the
 * shared UnifiedNluService. This enables:
 * - Fast pattern matching before expensive BERT inference
 * - Shared intent definitions with VoiceOS
 * - Unified command vocabulary across the platform
 *
 * Usage Flow:
 * 1. Try UnifiedNluService pattern matching (fast, ~1ms)
 * 2. If no match or low confidence, fall back to BERT inference (~50ms)
 * 3. Results can be synced back to shared database
 *
 * Created: 2025-12-07
 */

package com.augmentalis.ava.features.nlu.migration

import android.content.Context
import android.util.Log
import com.augmentalis.ava.features.nlu.IntentClassification
import com.augmentalis.ava.features.nlu.IntentClassifier
import com.augmentalis.shared.nlu.model.IntentMatch
import com.augmentalis.shared.nlu.model.IntentSource
import com.augmentalis.shared.nlu.model.MatchMethod
import com.augmentalis.shared.nlu.model.UnifiedIntent
import com.augmentalis.shared.nlu.repository.IntentRepositoryFactory
import com.augmentalis.shared.nlu.service.UnifiedNluService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridges AVA IntentClassifier with UnifiedNluService.
 *
 * Provides hybrid classification that uses pattern matching for fast
 * common commands and BERT inference for complex/unknown queries.
 */
class UnifiedNluBridge private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "UnifiedNluBridge"

        // Confidence threshold for trusting pattern match (skip BERT)
        private const val PATTERN_CONFIDENCE_THRESHOLD = 0.9f

        // Confidence threshold for hybrid result (combine both)
        private const val HYBRID_CONFIDENCE_THRESHOLD = 0.7f

        @Volatile
        private var instance: UnifiedNluBridge? = null

        fun getInstance(context: Context): UnifiedNluBridge {
            return instance ?: synchronized(this) {
                instance ?: UnifiedNluBridge(context.applicationContext).also { instance = it }
            }
        }
    }

    // Unified NLU service (shared with VoiceOS)
    private val unifiedNluService: UnifiedNluService by lazy {
        val repository = IntentRepositoryFactory.create(context)
        UnifiedNluService(repository)
    }

    private var isInitialized = false

    /**
     * Initialize the bridge.
     *
     * Loads intents from shared database and indexes them for pattern matching.
     * Should be called during AVA's NLU initialization.
     */
    suspend fun initialize(): BridgeInitResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing UnifiedNluBridge...")

        try {
            val result = unifiedNluService.initialize()

            if (result.success) {
                isInitialized = true
                Log.i(TAG, "✅ Bridge initialized: ${result.intentCount} shared intents")
                BridgeInitResult(
                    success = true,
                    sharedIntentCount = result.intentCount,
                    patternCount = result.stats?.patternCount ?: 0
                )
            } else {
                Log.w(TAG, "⚠️ Bridge initialization failed: ${result.error}")
                BridgeInitResult(
                    success = false,
                    error = result.error
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bridge initialization error", e)
            BridgeInitResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Check if bridge is ready
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Classify using hybrid approach: pattern matching + BERT
     *
     * Fast path: Pattern match with high confidence → return immediately
     * Slow path: Low/no pattern match → use BERT classifier
     *
     * @param utterance User input text
     * @param intentClassifier AVA's BERT-based classifier (for fallback)
     * @param candidateIntents List of possible intents
     * @return HybridClassificationResult with method used and confidence
     */
    suspend fun classifyHybrid(
        utterance: String,
        intentClassifier: IntentClassifier,
        candidateIntents: List<String>
    ): HybridClassificationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // Step 1: Try fast pattern matching
        if (isInitialized) {
            val patternResult = unifiedNluService.classify(utterance)

            if (patternResult.hasMatch && patternResult.confidence >= PATTERN_CONFIDENCE_THRESHOLD) {
                // High-confidence pattern match - skip BERT
                val topMatch = patternResult.topMatch!!
                val totalTime = System.currentTimeMillis() - startTime

                Log.i(TAG, "✓ Fast path: '${topMatch.intent.id}' (${patternResult.method}, " +
                        "confidence=${patternResult.confidence}, ${totalTime}ms)")

                return@withContext HybridClassificationResult(
                    intent = topMatch.intent.id,
                    confidence = patternResult.confidence,
                    method = HybridMethod.PATTERN_ONLY,
                    patternMatch = topMatch,
                    processingTimeMs = totalTime
                )
            }

            // Step 2: Pattern match exists but low confidence - combine with BERT
            if (patternResult.hasMatch && patternResult.confidence >= HYBRID_CONFIDENCE_THRESHOLD) {
                val bertResult = intentClassifier.classifyIntent(utterance, candidateIntents)
                val totalTime = System.currentTimeMillis() - startTime

                when (bertResult) {
                    is com.augmentalis.ava.core.common.Result.Success -> {
                        // Combine pattern and BERT scores
                        val patternIntent = patternResult.topMatch!!.intent.id
                        val bertIntent = bertResult.data.intent
                        val bertConfidence = bertResult.data.confidence

                        // If both agree, boost confidence
                        val finalIntent: String
                        val finalConfidence: Float
                        if (patternIntent == bertIntent) {
                            finalIntent = patternIntent
                            finalConfidence = (patternResult.confidence + bertConfidence) / 2 * 1.1f
                            Log.i(TAG, "✓ Hybrid (agree): '$finalIntent' (pattern+BERT, " +
                                    "confidence=${finalConfidence.coerceIn(0f, 1f)}, ${totalTime}ms)")
                        } else {
                            // Disagreement - trust BERT for semantic understanding
                            finalIntent = bertIntent
                            finalConfidence = bertConfidence * 0.9f
                            Log.i(TAG, "✓ Hybrid (disagree): '$finalIntent' (BERT preferred, " +
                                    "confidence=$finalConfidence, ${totalTime}ms)")
                        }

                        return@withContext HybridClassificationResult(
                            intent = finalIntent,
                            confidence = finalConfidence.coerceIn(0f, 1f),
                            method = HybridMethod.HYBRID,
                            patternMatch = patternResult.topMatch,
                            bertResult = bertResult.data,
                            processingTimeMs = totalTime
                        )
                    }
                    is com.augmentalis.ava.core.common.Result.Error -> {
                        // BERT failed, use pattern result
                        val topMatch = patternResult.topMatch!!
                        Log.w(TAG, "BERT failed, using pattern match: ${topMatch.intent.id}")
                        return@withContext HybridClassificationResult(
                            intent = topMatch.intent.id,
                            confidence = patternResult.confidence * 0.9f,
                            method = HybridMethod.PATTERN_ONLY,
                            patternMatch = topMatch,
                            processingTimeMs = System.currentTimeMillis() - startTime
                        )
                    }
                }
            }
        }

        // Step 3: No pattern match or not initialized - use BERT only
        val bertResult = intentClassifier.classifyIntent(utterance, candidateIntents)
        val totalTime = System.currentTimeMillis() - startTime

        when (bertResult) {
            is com.augmentalis.ava.core.common.Result.Success -> {
                Log.i(TAG, "✓ BERT only: '${bertResult.data.intent}' " +
                        "(confidence=${bertResult.data.confidence}, ${totalTime}ms)")
                HybridClassificationResult(
                    intent = bertResult.data.intent,
                    confidence = bertResult.data.confidence,
                    method = HybridMethod.BERT_ONLY,
                    bertResult = bertResult.data,
                    processingTimeMs = totalTime
                )
            }
            is com.augmentalis.ava.core.common.Result.Error -> {
                Log.e(TAG, "✗ Classification failed: ${bertResult.message}")
                HybridClassificationResult(
                    intent = "unknown",
                    confidence = 0f,
                    method = HybridMethod.FAILED,
                    error = bertResult.message,
                    processingTimeMs = totalTime
                )
            }
        }
    }

    /**
     * Fast classification using pattern matching only.
     *
     * Use for high-frequency commands where speed is critical (e.g., navigation).
     * Falls back to null if no confident match, allowing caller to use BERT.
     *
     * @param utterance User input text
     * @return IntentMatch or null if no confident match
     */
    fun classifyFast(utterance: String): IntentMatch? {
        if (!isInitialized) return null

        val result = unifiedNluService.classifyFast(utterance)

        if (result != null && result.score >= PATTERN_CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "Fast match: '${result.intent.id}' (score=${result.score})")
            return result
        }

        return null
    }

    /**
     * Sync AVA intents to shared database.
     *
     * Exports AVA's intent definitions to the shared NLU database
     * for use by VoiceOS and other platform components.
     *
     * @param intents List of AVA intents to export
     */
    suspend fun syncIntentsToShared(intents: List<AvaIntentDef>) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Syncing ${intents.size} AVA intents to shared database...")

        val unifiedIntents = intents.map { ava ->
            UnifiedIntent(
                id = ava.id,
                canonicalPhrase = ava.primaryPhrase,
                patterns = ava.patterns,
                synonyms = ava.synonyms,
                embedding = null, // AVA uses BERT, not pre-computed embeddings
                category = ava.category,
                actionId = ava.id,
                priority = 50,
                locale = ava.locale,
                source = IntentSource.AVA
            )
        }

        for (intent in unifiedIntents) {
            unifiedNluService.saveIntent(intent)
        }

        unifiedNluService.refresh()

        Log.i(TAG, "✅ Synced ${intents.size} intents to shared database")
    }

    /**
     * Load intents from shared database for AVA use.
     *
     * @return List of UnifiedIntent from shared database
     */
    suspend fun loadSharedIntents(): List<UnifiedIntent> = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.w(TAG, "Bridge not initialized, returning empty list")
            return@withContext emptyList()
        }

        val intents = unifiedNluService.getAllIntents()
        Log.d(TAG, "Loaded ${intents.size} shared intents")
        intents
    }

    /**
     * Clear bridge resources
     */
    suspend fun clear() {
        unifiedNluService.clear()
        isInitialized = false
        Log.i(TAG, "Bridge cleared")
    }
}

/**
 * Bridge initialization result
 */
data class BridgeInitResult(
    val success: Boolean,
    val sharedIntentCount: Int = 0,
    val patternCount: Int = 0,
    val error: String? = null
)

/**
 * Hybrid classification method used
 */
enum class HybridMethod {
    PATTERN_ONLY,  // Fast path - pattern match only
    BERT_ONLY,     // BERT inference only
    HYBRID,        // Combined pattern + BERT
    FAILED         // Classification failed
}

/**
 * Hybrid classification result
 */
data class HybridClassificationResult(
    val intent: String,
    val confidence: Float,
    val method: HybridMethod,
    val patternMatch: IntentMatch? = null,
    val bertResult: IntentClassification? = null,
    val error: String? = null,
    val processingTimeMs: Long = 0
) {
    val isSuccess: Boolean get() = method != HybridMethod.FAILED
    val isHighConfidence: Boolean get() = confidence >= 0.85f
    val usedFastPath: Boolean get() = method == HybridMethod.PATTERN_ONLY
}

/**
 * AVA intent definition for syncing to shared database
 */
data class AvaIntentDef(
    val id: String,
    val primaryPhrase: String,
    val patterns: List<String>,
    val synonyms: List<String>,
    val category: String,
    val locale: String = "en-US"
)
