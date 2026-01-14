/**
 * UnifiedNluService - Main entry point for Shared NLU
 *
 * Provides a unified API for intent classification that can be used
 * by both VoiceOS and AVA applications.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.shared.nlu.service

import com.augmentalis.shared.nlu.classifier.ClassificationResult
import com.augmentalis.shared.nlu.classifier.ClassifierConfig
import com.augmentalis.shared.nlu.classifier.HybridIntentClassifier
import com.augmentalis.shared.nlu.matcher.EmbeddingProvider
import com.augmentalis.shared.nlu.model.IntentMatch
import com.augmentalis.shared.nlu.model.UnifiedIntent
import com.augmentalis.shared.nlu.parser.AvuIntentParser
import com.augmentalis.shared.nlu.parser.ParseResult
import com.augmentalis.shared.nlu.repository.IntentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Unified NLU service for intent classification.
 *
 * Usage:
 * ```kotlin
 * val service = UnifiedNluService(repository)
 * service.loadFromAvu(avuContent)
 * val result = service.classify("go back")
 * ```
 *
 * @property repository Intent data repository
 * @property config Classifier configuration
 */
class UnifiedNluService(
    private val repository: IntentRepository,
    private val config: ClassifierConfig = ClassifierConfig()
) {
    private val parser = AvuIntentParser()
    private val classifier = HybridIntentClassifier(config)

    private var isInitialized = false

    /**
     * Initialize the service by loading intents from repository
     */
    suspend fun initialize(): InitResult {
        return try {
            val intents = repository.getAll()
            classifier.index(intents)
            isInitialized = true

            InitResult(
                success = true,
                intentCount = intents.size,
                stats = classifier.getStats()
            )
        } catch (e: Exception) {
            InitResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Set embedding provider for semantic matching
     */
    fun setEmbeddingProvider(provider: EmbeddingProvider) {
        classifier.setEmbeddingProvider(provider)
    }

    /**
     * Load intents from AVU file content
     *
     * @param avuContent Raw AVU .aai file content
     * @param persist Save to repository if true
     * @return Parse result with loaded intents
     */
    suspend fun loadFromAvu(avuContent: String, persist: Boolean = true): ParseResult {
        val result = parser.parse(avuContent)

        if (result.isSuccess && persist) {
            repository.saveAll(result.intents)
        }

        // Re-index classifier
        val allIntents = repository.getAll()
        classifier.index(allIntents)
        isInitialized = true

        return result
    }

    /**
     * Load intents from multiple AVU files
     */
    suspend fun loadFromAvuFiles(avuContents: List<String>, persist: Boolean = true): List<ParseResult> {
        val results = avuContents.map { parser.parse(it) }

        if (persist) {
            val allIntents = results.flatMap { it.intents }
            repository.saveAll(allIntents)
        }

        // Re-index classifier
        val allIntents = repository.getAll()
        classifier.index(allIntents)
        isInitialized = true

        return results
    }

    /**
     * Classify user input
     *
     * @param input User input text
     * @param embedding Pre-computed embedding (optional)
     * @return Classification result with matches
     */
    fun classify(input: String, embedding: FloatArray? = null): ClassificationResult {
        if (!isInitialized) {
            return ClassificationResult(
                matches = emptyList(),
                method = com.augmentalis.shared.nlu.model.MatchMethod.UNKNOWN,
                confidence = 0f,
                processingTimeMs = 0
            )
        }

        return classifier.classify(input, embedding)
    }

    /**
     * Fast classification using pattern matching only
     */
    fun classifyFast(input: String): IntentMatch? {
        return classifier.classifyFast(input)
    }

    /**
     * Check if input has an exact match
     */
    fun hasExactMatch(input: String): Boolean {
        return classifier.hasExactMatch(input)
    }

    /**
     * Get intent by ID
     */
    suspend fun getIntent(id: String): UnifiedIntent? {
        return repository.getById(id)
    }

    /**
     * Get all intents
     */
    suspend fun getAllIntents(): List<UnifiedIntent> {
        return repository.getAll()
    }

    /**
     * Get intents as reactive Flow
     */
    fun getIntentsAsFlow(): Flow<List<UnifiedIntent>> {
        return repository.getAllAsFlow()
    }

    /**
     * Get intents by category
     */
    suspend fun getIntentsByCategory(category: String): List<UnifiedIntent> {
        return repository.getByCategory(category)
    }

    /**
     * Get intents by source
     */
    suspend fun getIntentsBySource(source: String): List<UnifiedIntent> {
        return repository.getBySource(source)
    }

    /**
     * Search intents by phrase
     */
    suspend fun searchIntents(query: String): List<UnifiedIntent> {
        return repository.searchByPhrase(query)
    }

    /**
     * Add or update intent
     */
    suspend fun saveIntent(intent: UnifiedIntent) {
        repository.save(intent)

        // Re-index classifier
        val allIntents = repository.getAll()
        classifier.index(allIntents)
    }

    /**
     * Delete intent
     */
    suspend fun deleteIntent(id: String) {
        repository.delete(id)

        // Re-index classifier
        val allIntents = repository.getAll()
        classifier.index(allIntents)
    }

    /**
     * Export intents to AVU format
     */
    suspend fun exportToAvu(
        locale: String = "en-US",
        project: String = "shared"
    ): String {
        val intents = repository.getAll()
        return parser.generate(intents, locale, project)
    }

    /**
     * Export intents from specific source
     */
    suspend fun exportSourceToAvu(
        source: String,
        locale: String = "en-US"
    ): String {
        val intents = repository.getBySource(source)
        return parser.generate(intents, locale, source)
    }

    /**
     * Get service statistics
     */
    suspend fun getStatistics(): NluStatistics {
        val count = repository.count()
        val byCategory = repository.countByCategory()
        val categories = repository.getCategories()
        val classifierStats = classifier.getStats()

        return NluStatistics(
            totalIntents = count,
            byCategory = byCategory,
            categories = categories,
            patternCount = classifierStats.patternCount,
            embeddedCount = classifierStats.embeddedCount,
            semanticAvailable = classifierStats.semanticAvailable
        )
    }

    /**
     * Clear all data
     */
    suspend fun clear() {
        repository.clear()
        classifier.clear()
        isInitialized = false
    }

    /**
     * Refresh classifier index from repository
     */
    suspend fun refresh() {
        val intents = repository.getAll()
        classifier.index(intents)
        isInitialized = intents.isNotEmpty()
    }
}

/**
 * Initialization result
 */
data class InitResult(
    val success: Boolean,
    val intentCount: Int = 0,
    val stats: com.augmentalis.shared.nlu.classifier.ClassifierStats? = null,
    val error: String? = null
)

/**
 * NLU statistics
 */
data class NluStatistics(
    val totalIntents: Long,
    val byCategory: Map<String, Long>,
    val categories: List<String>,
    val patternCount: Int,
    val embeddedCount: Int,
    val semanticAvailable: Boolean
)
