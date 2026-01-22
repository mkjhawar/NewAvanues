/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of NLU Coordinator using ONNX Runtime.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import com.augmentalis.chat.data.BuiltInIntents
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.ModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop (JVM) implementation of INLUCoordinator.
 *
 * Uses the NLU module's ONNX-based IntentClassifier for desktop.
 * Features:
 * - ONNX Runtime for MobileBERT inference
 * - LRU classification cache with configurable size
 * - Thread-safe operations
 * - Built-in intent loading from BuiltInIntents
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class NLUCoordinatorDesktop(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : INLUCoordinator {

    // ==================== State ====================

    private val _isNLUReady = MutableStateFlow(false)
    override val isNLUReady: StateFlow<Boolean> = _isNLUReady.asStateFlow()

    private val _isNLULoaded = MutableStateFlow(false)
    override val isNLULoaded: StateFlow<Boolean> = _isNLULoaded.asStateFlow()

    private val _candidateIntents = MutableStateFlow<List<String>>(emptyList())
    override val candidateIntents: StateFlow<List<String>> = _candidateIntents.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== Internal State ====================

    // Intent classifier (from NLU module)
    private var classifier: IntentClassifier? = null
    private val modelManager = ModelManager()

    // Classification cache (thread-safe)
    private val classificationCache = ConcurrentHashMap<String, IntentClassification>()
    private val maxCacheSize = 100

    // Initialization mutex
    private val initMutex = Mutex()

    // ==================== Initialization ====================

    override suspend fun initialize(): Result<Unit> = initMutex.withLock {
        try {
            if (_isNLULoaded.value) {
                println("[NLUCoordinatorDesktop] Already initialized, skipping")
                return Result.Success(Unit)
            }

            println("[NLUCoordinatorDesktop] Starting initialization...")

            // Check if model is available
            if (!modelManager.isModelAvailable()) {
                println("[NLUCoordinatorDesktop] Model not found, downloading...")
                val downloadResult = modelManager.downloadModelsIfNeeded { progress ->
                    println("[NLUCoordinatorDesktop] Download progress: ${(progress * 100).toInt()}%")
                }
                when (downloadResult) {
                    is Result.Error -> {
                        _errorMessage.value = downloadResult.message
                        return downloadResult
                    }
                    else -> {}
                }
            }

            // Get classifier instance
            classifier = IntentClassifier.getInstance(Unit)

            // Initialize classifier with model path
            val modelPath = modelManager.getModelPath()
            println("[NLUCoordinatorDesktop] Initializing classifier with model: $modelPath")

            val initResult = classifier!!.initialize(modelPath)
            when (initResult) {
                is Result.Error -> {
                    _errorMessage.value = initResult.message
                    return initResult
                }
                else -> {}
            }

            _isNLULoaded.value = true

            // Load candidate intents
            loadCandidateIntents()

            // Pre-compute embeddings for built-in intents
            precomputeBuiltInEmbeddings()

            _isNLUReady.value = true
            println("[NLUCoordinatorDesktop] Initialization complete. Ready for classification.")

            Result.Success(Unit)
        } catch (e: Exception) {
            val errorMsg = "NLU initialization failed: ${e.message}"
            println("[NLUCoordinatorDesktop] $errorMsg")
            _errorMessage.value = errorMsg
            Result.Error(
                exception = e,
                message = errorMsg
            )
        }
    }

    /**
     * Pre-compute embeddings for built-in intents to improve classification speed.
     */
    private suspend fun precomputeBuiltInEmbeddings() {
        val intentTexts = mutableMapOf<String, String>()

        // Map intents to representative text
        for (intent in BuiltInIntents.ALL_INTENTS) {
            val examples = BuiltInIntents.getExampleUtterances(intent)
            if (examples.isNotEmpty()) {
                // Use first example as representative text
                intentTexts[intent] = examples.first()
            } else {
                // Use intent name as fallback
                intentTexts[intent] = intent.replace("_", " ")
            }
        }

        val count = classifier?.precomputeEmbeddings(intentTexts) ?: 0
        println("[NLUCoordinatorDesktop] Pre-computed $count intent embeddings")
    }

    // ==================== Classification ====================

    override suspend fun classify(utterance: String): IntentClassification? {
        if (!_isNLUReady.value) {
            println("[NLUCoordinatorDesktop] NLU not ready, returning null")
            return null
        }

        val normalizedUtterance = utterance.trim().lowercase()

        // Check cache first
        classificationCache[normalizedUtterance]?.let { cached ->
            println("[NLUCoordinatorDesktop] Cache hit for: $normalizedUtterance")
            return cached
        }

        // Check fast-path keywords
        val fastIntent = BuiltInIntents.FAST_KEYWORDS[normalizedUtterance]
        if (fastIntent != null) {
            val result = IntentClassification(
                intent = fastIntent,
                confidence = 1.0f,
                inferenceTimeMs = 0,
                allScores = mapOf(fastIntent to 1.0f)
            )
            cacheClassification(normalizedUtterance, result)
            println("[NLUCoordinatorDesktop] Fast-path match: $fastIntent")
            return result
        }

        // Run NLU classification
        val candidates = _candidateIntents.value.ifEmpty { BuiltInIntents.ALL_INTENTS }

        return when (val result = classifier?.classifyIntent(utterance, candidates)) {
            is Result.Success -> {
                cacheClassification(normalizedUtterance, result.data)
                result.data
            }
            is Result.Error -> {
                println("[NLUCoordinatorDesktop] Classification error: ${result.message}")
                null
            }
            else -> null
        }
    }

    override fun getCachedClassification(utterance: String): IntentClassification? {
        return classificationCache[utterance.trim().lowercase()]
    }

    private fun cacheClassification(utterance: String, classification: IntentClassification) {
        // Evict oldest entries if cache is full (simple LRU approximation)
        if (classificationCache.size >= maxCacheSize) {
            val keysToRemove = classificationCache.keys.take(maxCacheSize / 4)
            keysToRemove.forEach { classificationCache.remove(it) }
        }
        classificationCache[utterance] = classification
    }

    // ==================== Intent Management ====================

    override suspend fun loadCandidateIntents() {
        // Start with built-in intents
        val intents = BuiltInIntents.ALL_INTENTS.toMutableList()

        // In a full implementation, we would also load:
        // 1. User-taught intents from database
        // 2. .ava file intents from config

        _candidateIntents.value = intents
        println("[NLUCoordinatorDesktop] Loaded ${intents.size} candidate intents")
    }

    override suspend fun invalidateAndReloadIntents() {
        clearClassificationCache()
        loadCandidateIntents()

        // Re-compute embeddings if classifier is ready
        if (_isNLUReady.value) {
            precomputeBuiltInEmbeddings()
        }
    }

    override fun clearClassificationCache() {
        classificationCache.clear()
        println("[NLUCoordinatorDesktop] Classification cache cleared")
    }

    override fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Add a user-taught intent.
     *
     * @param intent Intent identifier
     * @param exampleText Example utterance for this intent
     */
    suspend fun addUserIntent(intent: String, exampleText: String) {
        // Add to candidate intents if not present
        if (intent !in _candidateIntents.value) {
            _candidateIntents.value = _candidateIntents.value + intent
        }

        // Compute and add embedding
        classifier?.precomputeEmbeddings(mapOf(intent to exampleText))

        // Clear cache to ensure re-classification
        clearClassificationCache()

        println("[NLUCoordinatorDesktop] Added user intent: $intent")
    }

    /**
     * Release resources.
     * Call during app shutdown.
     */
    fun close() {
        classifier?.close()
        classifier = null
        _isNLUReady.value = false
        _isNLULoaded.value = false
        classificationCache.clear()
        println("[NLUCoordinatorDesktop] Resources released")
    }

    companion object {
        @Volatile
        private var INSTANCE: NLUCoordinatorDesktop? = null

        /**
         * Get singleton instance of NLUCoordinatorDesktop.
         *
         * @param scope Coroutine scope for async operations
         * @return Singleton instance
         */
        fun getInstance(
            scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
        ): NLUCoordinatorDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NLUCoordinatorDesktop(scope).also {
                    INSTANCE = it
                }
            }
        }
    }
}
