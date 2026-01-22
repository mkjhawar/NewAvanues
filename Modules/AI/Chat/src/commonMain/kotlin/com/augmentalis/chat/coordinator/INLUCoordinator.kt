package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import com.augmentalis.nlu.IntentClassification
import kotlinx.coroutines.flow.StateFlow

/**
 * NLU Coordinator Interface - Cross-platform NLU coordination
 *
 * Abstracts NLU operations for cross-platform use in KMP.
 * Provides:
 * - Model initialization and readiness state
 * - Intent classification with caching
 * - Candidate intent loading and management
 *
 * Thread-safe: Implementations must use synchronized cache and atomic state updates.
 *
 * @see NLUCoordinator for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
interface INLUCoordinator {
    // ==================== State ====================

    /**
     * Indicates whether the NLU model is ready for classification.
     */
    val isNLUReady: StateFlow<Boolean>

    /**
     * Indicates whether the NLU model has been loaded.
     */
    val isNLULoaded: StateFlow<Boolean>

    /**
     * List of candidate intents available for classification.
     * Combines built-in, user-taught, and .ava file intents.
     */
    val candidateIntents: StateFlow<List<String>>

    /**
     * Current error message, if any.
     */
    val errorMessage: StateFlow<String?>

    // ==================== Initialization ====================

    /**
     * Initialize NLU classifier and load model.
     *
     * @return Result indicating success or failure with error message
     */
    suspend fun initialize(): Result<Unit>

    // ==================== Classification ====================

    /**
     * Classify an utterance with caching.
     *
     * @param utterance User input text
     * @return Classification result with intent and confidence, or null if NLU not ready
     */
    suspend fun classify(utterance: String): IntentClassification?

    /**
     * Get cached classification for an utterance without re-classifying.
     *
     * @param utterance User input text
     * @return Cached classification or null if not in cache
     */
    fun getCachedClassification(utterance: String): IntentClassification?

    // ==================== Intent Management ====================

    /**
     * Load candidate intents for NLU classification.
     * Combines built-in, user-taught, and .ava file intents.
     */
    suspend fun loadCandidateIntents()

    /**
     * Invalidate intents cache and reload.
     * Call after user teaches a new intent.
     */
    suspend fun invalidateAndReloadIntents()

    /**
     * Clear NLU classification cache.
     * Call when user teaches new intents to force re-classification.
     */
    fun clearClassificationCache()

    /**
     * Clear error message.
     */
    fun clearError()
}
