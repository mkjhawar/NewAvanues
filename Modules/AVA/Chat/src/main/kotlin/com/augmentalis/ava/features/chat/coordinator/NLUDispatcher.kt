package com.augmentalis.ava.features.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.features.nlu.IntentClassification
import com.augmentalis.ava.features.nlu.IntentClassifier
import com.augmentalis.ava.features.nlu.KeywordSpotter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NLU Dispatcher
 *
 * Routes utterances to the appropriate NLU engine based on complexity and latency requirements.
 * Implements "Adaptive NLU" architecture:
 * 1. Fast Path: KeywordSpotter (<1ms) - Immediate command execution
 * 2. Deep Path: IntentClassifier (~50ms) - Semantic understanding
 *
 * @author AVA Team
 */
@Singleton
class NLUDispatcher @Inject constructor(
    private val intentClassifier: IntentClassifier
) {
    private val keywordSpotter = KeywordSpotter()
    
    companion object {
        private const val TAG = "NLUDispatcher"
    }

    /**
     * initialize dispatcher and keyword spotter
     */
    fun initialize(keywords: Map<String, String>) {
        keywordSpotter.clear()
        keywords.forEach { (phrase, intentId) ->
            keywordSpotter.addKeyword(phrase, intentId)
        }
        Log.d(TAG, "Initialized fast-path with ${keywords.size} keywords")
    }

    /**
     * Dispatch utterance to best NLU engine
     */
    suspend fun dispatch(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> {
        val trimmed = utterance.trim()
        val paramStart = System.currentTimeMillis()

        // 1. Fast Path: Keyword Spotting
        // Check exact match for instant execution
        val keywordMatch = keywordSpotter.matchExact(trimmed)
        if (keywordMatch != null) {
            val latency = System.currentTimeMillis() - paramStart
            Log.i(TAG, "⚡ Fast Path Match: '$trimmed' -> $keywordMatch (${latency}ms)")
            
            return Result.Success(
                IntentClassification(
                    intent = keywordMatch,
                    confidence = 1.0f, // Keywords are 100% confident
                    inferenceTimeMs = latency,
                    allScores = mapOf(keywordMatch to 1.0f)
                )
            )
        }

        // 2. Deep Path: Semantic Intent Classification
        // Fallback for natural language
        Log.d(TAG, "Deep Path: Delegating to IntentClassifier")
        return intentClassifier.classifyIntent(trimmed, candidateIntents)
    }
    
    /**
     * Update/Teach new keywords dynamically
     */
    fun startTeaching() {
        // Prepare for new keywords
    }
    
    fun addFastPathKeyword(phrase: String, intentId: String) {
        keywordSpotter.addKeyword(phrase, intentId)
    }
}
