/**
 * NluService - High accuracy NLU service
 *
 * Extends classification with:
 * - Hybrid classification (ensemble voting)
 * - Self-learning calibration
 * - Context-aware classification
 * - BERT verification for medium-confidence results
 *
 * Created: 2025-12-07
 * Renamed: 2026-01-16 (from EnhancedNluService)
 */

package com.augmentalis.nlu.service

import com.augmentalis.nlu.NluThresholds
import com.augmentalis.nlu.classifier.CalibrationData
import com.augmentalis.nlu.classifier.ClassificationContext
import com.augmentalis.nlu.classifier.ClassificationResult
import com.augmentalis.nlu.classifier.ClassifierConfig
import com.augmentalis.nlu.classifier.EnhancedClassificationResult
import com.augmentalis.nlu.classifier.EnhancedConfig
import com.augmentalis.nlu.classifier.HybridClassifier
import com.augmentalis.nlu.classifier.HybridIntentClassifier
import com.augmentalis.nlu.classifier.LearningExport
import com.augmentalis.nlu.matcher.EmbeddingProvider
import com.augmentalis.nlu.model.IntentMatch
import com.augmentalis.nlu.model.MatchMethod
import com.augmentalis.nlu.model.UnifiedIntent
import com.augmentalis.nlu.parser.AvuIntentParser
import com.augmentalis.nlu.parser.ParseResult
import com.augmentalis.nlu.repository.IntentRepository
import kotlinx.coroutines.flow.Flow

/**
 * NLU service with high accuracy target.
 *
 * Key Features:
 * 1. Ensemble voting across pattern, fuzzy, and semantic matchers
 * 2. Per-intent confidence calibration (learns from feedback)
 * 3. Context-aware classification (previous intent, current app)
 * 4. BERT verification interface for medium-confidence results
 * 5. Self-learning from user corrections
 *
 * Usage:
 * ```kotlin
 * val service = NluService(repository)
 * service.initialize()
 *
 * // Basic classification
 * val result = service.classify("go back")
 *
 * // With context
 * val result = service.classify(
 *     input = "scroll down",
 *     context = ClassificationContext(currentApp = "com.example.browser")
 * )
 *
 * // Record feedback for learning
 * service.recordCorrect("go back", "navigate_back", MatchMethod.EXACT)
 * service.recordIncorrect("play music", "media_play", "app_open_spotify")
 * ```
 */
class NluService(
    private val repository: IntentRepository,
    private val enhancedConfig: EnhancedConfig = EnhancedConfig(),
    private val basicConfig: ClassifierConfig = ClassifierConfig()
) {
    private val parser = AvuIntentParser()

    // Primary classifier with ensemble voting
    private val hybridClassifier = HybridClassifier(enhancedConfig)

    // Basic classifier (fallback and comparison)
    private val basicClassifier = HybridIntentClassifier(basicConfig)

    // BERT verifier callback (set by AVA when available)
    private var bertVerifier: BertVerifier? = null

    private var isInitialized = false

    /**
     * Initialize the service
     */
    suspend fun initialize(): InitResult {
        return try {
            val intents = repository.getAll()

            hybridClassifier.index(intents)
            basicClassifier.index(intents)

            isInitialized = true

            InitResult(
                success = true,
                intentCount = intents.size,
                classifierReady = true,
                learningEnabled = true
            )
        } catch (e: Exception) {
            InitResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Set embedding provider
     */
    fun setEmbeddingProvider(provider: EmbeddingProvider) {
        hybridClassifier.setEmbeddingProvider(provider)
        basicClassifier.setEmbeddingProvider(provider)
    }

    /**
     * Set BERT verifier for medium-confidence verification
     *
     * Call this from AVA to enable BERT verification:
     * ```kotlin
     * service.setBertVerifier { input, candidates ->
     *     intentClassifier.classifyIntent(input, candidates)
     * }
     * ```
     */
    fun setBertVerifier(verifier: BertVerifier) {
        bertVerifier = verifier
    }

    // ============================================================================
    // Classification API
    // ============================================================================

    /**
     * Primary classification with ensemble voting
     *
     * This is the main classification method for high accuracy.
     */
    fun classify(
        input: String,
        embedding: FloatArray? = null,
        context: ClassificationContext? = null
    ): EnhancedClassificationResult {
        if (!isInitialized) {
            return EnhancedClassificationResult.empty()
        }

        return hybridClassifier.classify(input, embedding, context)
    }

    /**
     * Classification with automatic BERT verification
     *
     * Uses hybrid classifier, then verifies with BERT if:
     * - Confidence is in medium range (0.6-0.85)
     * - BERT verifier is available
     *
     * This achieves the highest accuracy by combining fast pattern matching
     * with BERT's semantic understanding for ambiguous cases.
     */
    suspend fun classifyWithVerification(
        input: String,
        embedding: FloatArray? = null,
        context: ClassificationContext? = null
    ): VerifiedClassificationResult {
        if (!isInitialized) {
            return VerifiedClassificationResult(
                result = EnhancedClassificationResult.empty(),
                wasVerified = false
            )
        }

        val classificationResult = hybridClassifier.classify(input, embedding, context)

        // Check if verification is needed and available
        if (classificationResult.needsVerification && bertVerifier != null) {
            val candidates = classificationResult.matches.map { it.intent.id }

            if (candidates.isNotEmpty()) {
                val bertResult = bertVerifier!!.verify(input, candidates)

                if (bertResult != null) {
                    // BERT verification succeeded - check if it agrees
                    val bertIntent = bertResult.intentId
                    val bertConfidence = bertResult.confidence

                    val classifiedIntent = classificationResult.topMatch?.intent?.id

                    return if (bertIntent == classifiedIntent) {
                        // Agreement - boost confidence
                        val boostedConfidence = (classificationResult.confidence + bertConfidence) / 2 * NluThresholds.BERT_AGREEMENT_BOOST_MULTIPLIER
                        VerifiedClassificationResult(
                            result = classificationResult.copy(
                                confidence = boostedConfidence.coerceIn(0f, 1f)
                            ),
                            wasVerified = true,
                            bertAgreed = true,
                            bertConfidence = bertConfidence
                        )
                    } else {
                        // Disagreement - trust BERT for semantic understanding
                        // But create a new result with BERT's answer
                        val bertMatch = classificationResult.matches.find { it.intent.id == bertIntent }
                        val finalMatches = if (bertMatch != null) {
                            listOf(bertMatch.copy(score = bertConfidence)) +
                                    classificationResult.matches.filter { it.intent.id != bertIntent }
                        } else {
                            classificationResult.matches
                        }

                        VerifiedClassificationResult(
                            result = classificationResult.copy(
                                matches = finalMatches,
                                confidence = bertConfidence * NluThresholds.BERT_OVERRIDE_CONFIDENCE_FACTOR,
                                method = MatchMethod.SEMANTIC
                            ),
                            wasVerified = true,
                            bertAgreed = false,
                            bertConfidence = bertConfidence,
                            bertOverrode = true
                        )
                    }
                }
            }
        }

        // No verification needed or available
        return VerifiedClassificationResult(
            result = classificationResult,
            wasVerified = false
        )
    }

    /**
     * Basic classification (uses original HybridIntentClassifier)
     *
     * Use for comparison or when enhanced features not needed.
     */
    fun classifyBasic(input: String, embedding: FloatArray? = null): ClassificationResult {
        if (!isInitialized) {
            return ClassificationResult(
                matches = emptyList(),
                method = MatchMethod.UNKNOWN,
                confidence = 0f,
                processingTimeMs = 0
            )
        }

        return basicClassifier.classify(input, embedding)
    }

    /**
     * Fast classification (pattern matching only)
     */
    fun classifyFast(input: String): IntentMatch? {
        return basicClassifier.classifyFast(input)
    }

    /**
     * Check for exact match
     */
    fun hasExactMatch(input: String): Boolean {
        return basicClassifier.hasExactMatch(input)
    }

    // ============================================================================
    // Self-Learning API
    // ============================================================================

    /**
     * Record correct classification (positive feedback)
     */
    fun recordCorrect(input: String, intentId: String, method: MatchMethod) {
        hybridClassifier.recordCorrect(input, intentId, method)
    }

    /**
     * Record incorrect classification (negative feedback)
     */
    fun recordIncorrect(input: String, wrongIntentId: String, correctIntentId: String?) {
        hybridClassifier.recordIncorrect(input, wrongIntentId, correctIntentId)
    }

    /**
     * Add negative sample (something that should NOT be classified)
     */
    fun addNegativeSample(input: String) {
        hybridClassifier.addNegativeSample(input)
    }

    /**
     * Get current accuracy (based on recorded feedback)
     */
    fun getAccuracy(): Float {
        return hybridClassifier.getAccuracy()
    }

    /**
     * Get calibration data for specific intent
     */
    fun getCalibration(intentId: String): CalibrationData? {
        return hybridClassifier.getCalibrationData()[intentId]
    }

    /**
     * Export learning data (for persistence)
     */
    fun exportLearningData(): LearningExport {
        return hybridClassifier.exportLearningData()
    }

    /**
     * Import learning data (from persistence)
     */
    fun importLearningData(data: LearningExport) {
        hybridClassifier.importLearningData(data)
    }

    // ============================================================================
    // Intent Management
    // ============================================================================

    suspend fun loadFromAvu(avuContent: String, persist: Boolean = true): ParseResult {
        val result = parser.parse(avuContent)

        if (result.isSuccess && persist) {
            repository.saveAll(result.intents)
        }

        val allIntents = repository.getAll()
        hybridClassifier.index(allIntents)
        basicClassifier.index(allIntents)
        isInitialized = true

        return result
    }

    suspend fun getIntent(id: String): UnifiedIntent? = repository.getById(id)

    suspend fun getAllIntents(): List<UnifiedIntent> = repository.getAll()

    fun getIntentsAsFlow(): Flow<List<UnifiedIntent>> = repository.getAllAsFlow()

    suspend fun getIntentsByCategory(category: String): List<UnifiedIntent> =
        repository.getByCategory(category)

    suspend fun searchIntents(query: String): List<UnifiedIntent> =
        repository.searchByPhrase(query)

    suspend fun saveIntent(intent: UnifiedIntent) {
        repository.save(intent)

        val allIntents = repository.getAll()
        hybridClassifier.index(allIntents)
        basicClassifier.index(allIntents)
    }

    suspend fun deleteIntent(id: String) {
        repository.delete(id)

        val allIntents = repository.getAll()
        hybridClassifier.index(allIntents)
        basicClassifier.index(allIntents)
    }

    suspend fun exportToAvu(locale: String = "en-US", project: String = "shared"): String {
        val intents = repository.getAll()
        return parser.generate(intents, locale, project)
    }

    suspend fun clear() {
        repository.clear()
        hybridClassifier.clear()
        basicClassifier.clear()
        isInitialized = false
    }

    suspend fun refresh() {
        val intents = repository.getAll()
        hybridClassifier.index(intents)
        basicClassifier.index(intents)
        isInitialized = intents.isNotEmpty()
    }

    /**
     * Get NLU statistics
     */
    suspend fun getStatistics(): NluStatistics {
        val count = repository.count()
        val byCategory = repository.countByCategory()
        val categories = repository.getCategories()
        val accuracy = hybridClassifier.getAccuracy()
        val calibrationCount = hybridClassifier.getCalibrationData().size

        return NluStatistics(
            totalIntents = count,
            byCategory = byCategory,
            categories = categories,
            currentAccuracy = accuracy,
            calibratedIntents = calibrationCount,
            bertVerifierAvailable = bertVerifier != null
        )
    }
}

// ============================================================================
// Data Classes
// ============================================================================

/**
 * Initialization result
 */
data class InitResult(
    val success: Boolean,
    val intentCount: Int = 0,
    val classifierReady: Boolean = false,
    val learningEnabled: Boolean = false,
    val error: String? = null
)

/**
 * Verified classification result
 */
data class VerifiedClassificationResult(
    val result: EnhancedClassificationResult,
    val wasVerified: Boolean,
    val bertAgreed: Boolean = false,
    val bertConfidence: Float = 0f,
    val bertOverrode: Boolean = false
) {
    val intent: String? get() = result.topMatch?.intent?.id
    val confidence: Float get() = result.confidence
    val isHighConfidence: Boolean get() = result.isHighConfidence
}

/**
 * NLU statistics
 */
data class NluStatistics(
    val totalIntents: Long,
    val byCategory: Map<String, Long>,
    val categories: List<String>,
    val currentAccuracy: Float,
    val calibratedIntents: Int,
    val bertVerifierAvailable: Boolean
)

/**
 * BERT verifier interface
 *
 * Implement this to enable BERT verification for medium-confidence results.
 */
fun interface BertVerifier {
    /**
     * Verify classification using BERT
     *
     * @param input User input text
     * @param candidates List of candidate intent IDs to choose from
     * @return Verification result or null if verification failed
     */
    suspend fun verify(input: String, candidates: List<String>): BertVerificationResult?
}

/**
 * BERT verification result
 */
data class BertVerificationResult(
    val intentId: String,
    val confidence: Float,
    val inferenceTimeMs: Long = 0
)
