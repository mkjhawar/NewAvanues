/**
 * LearningSystem.kt - STUB VERSION (VoiceDataManager dependency removed)
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Modified: 2025-11-24 - Disabled learning system (VoiceDataManager dependency)
 *
 * This is a stub implementation with all learning functionality disabled.
 * Original file backed up - restore when VoiceDataManager is re-enabled.
 */
package com.augmentalis.voiceos.speech.engines.common

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Stub learning system - all functionality disabled.
 *
 * TODO: Restore full implementation when VoiceDataManager is re-enabled
 * Original file: ~565 lines with full learning/vocabulary caching
 */
class LearningSystem(
    private val engineType: String,
    private val context: Context
) {

    companion object {
        private const val TAG = "LearningSystem [STUB]"
    }

    // Stub caches (empty, no persistence)
    private val learnedCommands = ConcurrentHashMap<String, String>()
    private val vocabularyCache = ConcurrentHashMap<String, Boolean>()

    /**
     * Match source enum (stub)
     */
    enum class MatchSource {
        LEARNED_COMMAND,
        VOCABULARY_CACHE,
        SIMILARITY_MATCH,
        EXACT_MATCH,
        NO_MATCH
    }

    /**
     * Match result data class (stub)
     */
    data class MatchResult(
        val matched: String,
        val original: String,
        val confidence: Float,
        val source: MatchSource
    )

    /**
     * Stub initialize - does nothing
     */
    suspend fun initialize() {
        Log.w(TAG, "Learning system DISABLED - VoiceDataManager dependency removed")
    }

    /**
     * Stub loadCommands - returns empty map
     */
    suspend fun loadCommands(): Map<String, String> {
        return emptyMap()
    }

    /**
     * Stub loadVocabulary - returns empty map
     */
    suspend fun loadVocabulary(): Map<String, Boolean> {
        return emptyMap()
    }

    /**
     * Stub learnCommand - does nothing
     * Accepts 2 or 3 parameters (confidence optional)
     * Non-suspend for compatibility with non-coroutine callers
     */
    fun learnCommand(original: String, corrected: String, confidence: Float = 1.0f) {
        // No-op
    }

    /**
     * Stub cacheVocabulary - does nothing
     */
    suspend fun cacheVocabulary(word: String, exists: Boolean) {
        // No-op
    }

    /**
     * Stub findSimilarCommand - returns null (no matching)
     */
    fun findSimilarCommand(text: String, threshold: Double = 0.8): String? {
        return null
    }

    /**
     * Stub isInVocabulary - always returns true (accept all)
     */
    fun isInVocabulary(word: String): Boolean {
        return true
    }

    /**
     * Stub getStatistics - returns empty stats
     */
    fun getStatistics(): Map<String, Int> {
        return mapOf(
            "learnedCommands" to 0,
            "vocabularyEntries" to 0
        )
    }

    /**
     * Stub cleanup - does nothing
     */
    fun cleanup() {
        learnedCommands.clear()
        vocabularyCache.clear()
    }

    /**
     * Stub processWithLearning - returns NO_MATCH result with original text
     */
    fun processWithLearning(
        recognized: String,
        commands: List<String>,
        confidence: Float
    ): MatchResult {
        // Stub: no learning, just return original
        return MatchResult(
            matched = recognized,
            original = recognized,
            confidence = confidence,
            source = MatchSource.NO_MATCH
        )
    }

    /**
     * Stub destroy - does nothing
     */
    fun destroy() {
        // No-op
    }

    /**
     * Stub clearAllData - does nothing
     */
    suspend fun clearAllData() {
        // No-op
    }
}
