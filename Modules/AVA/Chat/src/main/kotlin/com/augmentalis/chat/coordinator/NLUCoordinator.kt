package com.augmentalis.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import com.augmentalis.chat.data.BuiltInIntents
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NLU Coordinator - Single Responsibility: NLU state and classification
 *
 * Extracted from ChatViewModel as part of SOLID refactoring (P0).
 * Handles all NLU-related operations:
 * - Model initialization and readiness state
 * - Intent classification with caching
 * - Candidate intent loading and management
 *
 * Thread-safe: Uses synchronized cache and atomic state updates.
 *
 * @param intentClassifier ONNX-based intent classifier
 * @param modelManager NLU model management
 * @param trainExampleRepository User-taught intent examples
 * @param chatPreferences User preferences for cache configuration
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 */
@Singleton
class NLUCoordinator @Inject constructor(
    private val nluDispatcher: NLUDispatcher,
    private val intentClassifier: IntentClassifier, // Kept for initialization access if needed
    private val modelManager: ModelManager,
    private val trainExampleRepository: TrainExampleRepository,
    private val chatPreferences: ChatPreferences
) : INLUCoordinator {
    companion object {
        private const val TAG = "NLUCoordinator"
    }

    // ==================== State ====================

    private val _isNLUReady = MutableStateFlow(false)
    override val isNLUReady: StateFlow<Boolean> = _isNLUReady.asStateFlow()

    private val _isNLULoaded = MutableStateFlow(false)
    override val isNLULoaded: StateFlow<Boolean> = _isNLULoaded.asStateFlow()

    private val _candidateIntents = MutableStateFlow<List<String>>(emptyList())
    override val candidateIntents: StateFlow<List<String>> = _candidateIntents.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== Caching ====================

    // Issue 3.2 Fix: @Volatile for thread-safe visibility across coroutines
    @Volatile
    private var candidateIntentsCacheTimestamp = 0L

    private val intentsCacheTTL: Long
        get() = chatPreferences.getIntentsCacheTTL()

    /**
     * LRU cache for NLU classifications.
     * Caches normalized utterance -> IntentClassification to avoid redundant ONNX inference.
     */
    private val classificationCache: MutableMap<String, IntentClassification> by lazy {
        val maxSize = chatPreferences.getNLUCacheMaxSize()
        Collections.synchronizedMap(
            object : LinkedHashMap<String, IntentClassification>(maxSize, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, IntentClassification>): Boolean {
                    return size > maxSize
                }
            }
        )
    }

    // ==================== Initialization ====================

    /**
     * Initialize NLU classifier and load ONNX model.
     *
     * @return Result indicating success or failure with error message
     */
    override suspend fun initialize(): Result<Unit> {
        return try {
            Log.d(TAG, "Initializing NLU classifier...")
            val startTime = System.currentTimeMillis()

            if (!modelManager.isModelAvailable()) {
                val error = "NLU model not found. Please download the model first."
                _errorMessage.value = error
                Log.w(TAG, "NLU model not available")
                return Result.Error(IllegalStateException(error), error)
            }

            val modelPath = modelManager.getModelPath()
            when (val result = intentClassifier.initialize(modelPath)) {
                is Result.Success -> {
                    val initTime = System.currentTimeMillis() - startTime
                    Log.d(TAG, "NLU classifier initialized successfully in ${initTime}ms")
                    _isNLUReady.value = true
                    _isNLULoaded.value = true
                    Log.i(TAG, "*** NLU MODEL LOADED AND READY ***")

                    loadCandidateIntents()

                    // Initialize Fast Path (Optimization)
                    // Uses centralized keyword config from BuiltInIntents
                    nluDispatcher.initialize(BuiltInIntents.FAST_KEYWORDS)
                    
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    _errorMessage.value = result.message
                    _isNLULoaded.value = false
                    Log.e(TAG, "Failed to initialize NLU: ${result.message}", result.exception)
                    Result.Error(result.exception, result.message)
                }
            }
        } catch (e: Exception) {
            val error = "NLU initialization failed: ${e.message}"
            _errorMessage.value = error
            Log.e(TAG, "NLU initialization exception", e)
            Result.Error(e, error)
        }
    }

    // ==================== Classification ====================

    /**
     * Classify an utterance with caching.
     *
     * @param utterance User input text
     * @return Classification result with intent and confidence, or null if NLU not ready
     */
    override suspend fun classify(utterance: String): IntentClassification? {
        if (!_isNLUReady.value || _candidateIntents.value.isEmpty()) {
            Log.d(TAG, "NLU not ready, skipping classification")
            return null
        }

        val normalizedUtterance = utterance.trim().lowercase()

        // Check cache first
        classificationCache[normalizedUtterance]?.let { cached ->
            Log.d(TAG, "NLU cache HIT for: \"$normalizedUtterance\"")
            Log.d(TAG, "  Intent: ${cached.intent} (cached)")
            Log.d(TAG, "  Confidence: ${cached.confidence} (cached)")
            return cached
        }

        // Cache miss - perform classification
        Log.d(TAG, "NLU cache MISS for: \"$normalizedUtterance\"")
        return when (val result = nluDispatcher.dispatch(
            utterance = utterance.trim(),
            candidateIntents = _candidateIntents.value
        )) {
            is Result.Success -> {
                val classification = result.data
                classificationCache[normalizedUtterance] = classification

                Log.d(TAG, "NLU Classification Results:")
                Log.d(TAG, "  Intent: ${classification.intent}")
                Log.d(TAG, "  Confidence: ${classification.confidence}")
                Log.d(TAG, "  Inference time: ${classification.inferenceTimeMs}ms")
                Log.i(TAG, "NLU cache stats: ${classificationCache.size} entries (added)")

                classification
            }
            is Result.Error -> {
                Log.e(TAG, "NLU classification failed: ${result.message}", result.exception)
                IntentClassification(
                    intent = BuiltInIntents.UNKNOWN,
                    confidence = 0.0f,
                    inferenceTimeMs = 0
                )
            }
        }
    }

    /**
     * Get cached classification for an utterance without re-classifying.
     *
     * @param utterance User input text
     * @return Cached classification or null if not in cache
     */
    override fun getCachedClassification(utterance: String): IntentClassification? {
        return classificationCache[utterance.trim().lowercase()]
    }

    // ==================== Intent Management ====================

    /**
     * Load candidate intents for NLU classification.
     * Combines built-in, user-taught, and .ava file intents.
     */
    override suspend fun loadCandidateIntents() {
        try {
            // Check cache freshness
            val now = System.currentTimeMillis()
            if (_candidateIntents.value.isNotEmpty() &&
                (now - candidateIntentsCacheTimestamp) < intentsCacheTTL) {
                Log.d(TAG, "Using cached candidate intents (age: ${now - candidateIntentsCacheTimestamp}ms)")
                return
            }

            Log.d(TAG, "Loading candidate intents from repository...")
            val startTime = System.currentTimeMillis()

            // 1. Built-in intents
            val builtInIntents = BuiltInIntents.ALL_INTENTS
            Log.d(TAG, "Loaded ${builtInIntents.size} built-in intents")

            // 2. User-taught intents
            val userTaughtIntents = mutableSetOf<String>()
            try {
                val examples = withTimeoutOrNull(3000L) {
                    trainExampleRepository.getAllExamples().first()
                }
                examples?.let { userTaughtIntents.addAll(it.map { ex -> ex.intent }) }
                Log.d(TAG, "Loaded ${userTaughtIntents.size} user-taught intents")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load user-taught intents: ${e.message}")
            }

            // 3. Intents from .ava files
            val avaFileIntents = try {
                intentClassifier.getLoadedIntents().also {
                    Log.d(TAG, "Loaded ${it.size} intents from .ava files")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load intents from classifier: ${e.message}")
                emptyList()
            }

            // 4. Combine and deduplicate
            val allIntents = (builtInIntents + userTaughtIntents + avaFileIntents).toSet().toList()

            if (allIntents.isEmpty()) {
                Log.e(TAG, "Combined intents list is empty! Falling back to built-in intents")
                _candidateIntents.value = BuiltInIntents.ALL_INTENTS
            } else {
                _candidateIntents.value = allIntents
            }

            candidateIntentsCacheTimestamp = now

            val loadTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Loaded ${allIntents.size} total candidate intents in ${loadTime}ms")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load candidate intents: ${e.message}", e)
            _candidateIntents.value = BuiltInIntents.ALL_INTENTS
        }
    }

    /**
     * Invalidate intents cache and reload.
     * Call after user teaches a new intent.
     */
    override suspend fun invalidateAndReloadIntents() {
        candidateIntentsCacheTimestamp = 0L
        loadCandidateIntents()
    }

    /**
     * Clear NLU classification cache.
     * Call when user teaches new intents to force re-classification.
     */
    override fun clearClassificationCache() {
        classificationCache.clear()
        Log.d(TAG, "NLU classification cache cleared")
    }

    /**
     * Clear error message.
     */
    override fun clearError() {
        _errorMessage.value = null
    }
}
