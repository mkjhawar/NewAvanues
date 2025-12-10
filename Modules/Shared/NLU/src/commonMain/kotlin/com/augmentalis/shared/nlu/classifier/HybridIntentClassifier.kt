/**
 * HybridIntentClassifier - Three-stage intent classification
 *
 * Combines pattern matching, fuzzy matching, and semantic similarity
 * for robust intent recognition that handles:
 * - Exact commands (pattern)
 * - Typos and speech errors (fuzzy)
 * - Novel phrasings (semantic)
 *
 * Created: 2025-12-07
 */

package com.augmentalis.shared.nlu.classifier

import com.augmentalis.shared.nlu.matcher.EmbeddingProvider
import com.augmentalis.shared.nlu.matcher.FuzzyMatcher
import com.augmentalis.shared.nlu.matcher.PatternMatcher
import com.augmentalis.shared.nlu.matcher.SemanticMatcher
import com.augmentalis.shared.nlu.model.IntentMatch
import com.augmentalis.shared.nlu.model.MatchMethod
import com.augmentalis.shared.nlu.model.UnifiedIntent

/**
 * Hybrid classifier combining three matching strategies.
 *
 * Pipeline:
 * 1. Pattern match (exact) - fastest, highest confidence
 * 2. Fuzzy match (Levenshtein) - handles typos
 * 3. Semantic match (embeddings) - handles novel phrasings
 *
 * @property config Classifier configuration
 */
class HybridIntentClassifier(
    private val config: ClassifierConfig = ClassifierConfig()
) {
    private val patternMatcher = PatternMatcher()
    private val fuzzyMatcher = FuzzyMatcher(
        minSimilarity = config.fuzzyMinSimilarity,
        maxCandidates = config.maxCandidates
    )
    private val semanticMatcher = SemanticMatcher(
        minSimilarity = config.semanticMinSimilarity,
        maxCandidates = config.maxCandidates
    )

    private var isIndexed = false

    /**
     * Index intents for classification
     */
    fun index(intents: List<UnifiedIntent>) {
        patternMatcher.index(intents)
        fuzzyMatcher.index(intents)
        semanticMatcher.index(intents)
        isIndexed = true
    }

    /**
     * Set embedding provider for semantic matching
     */
    fun setEmbeddingProvider(provider: EmbeddingProvider) {
        semanticMatcher.setEmbeddingProvider(provider)
    }

    /**
     * Classify input text to find matching intents
     *
     * @param input User input text
     * @param inputEmbedding Pre-computed embedding (optional)
     * @return Classification result with matches and metadata
     */
    fun classify(input: String, inputEmbedding: FloatArray? = null): ClassificationResult {
        if (!isIndexed) {
            return ClassificationResult(
                matches = emptyList(),
                method = MatchMethod.UNKNOWN,
                confidence = 0f,
                processingTimeMs = 0
            )
        }

        val startTime = currentTimeMillis()

        // Stage 1: Exact pattern match
        val patternMatches = patternMatcher.match(input)
        if (patternMatches.isNotEmpty() && patternMatches.first().score >= config.exactMatchThreshold) {
            return ClassificationResult(
                matches = patternMatches.take(config.maxCandidates),
                method = MatchMethod.EXACT,
                confidence = patternMatches.first().score,
                processingTimeMs = currentTimeMillis() - startTime
            )
        }

        // Stage 2: Fuzzy match
        val fuzzyMatches = fuzzyMatcher.match(input)
        if (fuzzyMatches.isNotEmpty() && fuzzyMatches.first().score >= config.fuzzyAcceptThreshold) {
            return ClassificationResult(
                matches = fuzzyMatches.take(config.maxCandidates),
                method = MatchMethod.FUZZY,
                confidence = fuzzyMatches.first().score,
                processingTimeMs = currentTimeMillis() - startTime
            )
        }

        // Stage 3: Semantic match (if available)
        if (semanticMatcher.isAvailable() || inputEmbedding != null) {
            val semanticMatches = semanticMatcher.match(input, inputEmbedding)
            if (semanticMatches.isNotEmpty()) {
                return ClassificationResult(
                    matches = semanticMatches.take(config.maxCandidates),
                    method = MatchMethod.SEMANTIC,
                    confidence = semanticMatches.first().score,
                    processingTimeMs = currentTimeMillis() - startTime
                )
            }
        }

        // Stage 4: Hybrid fallback - combine fuzzy and pattern with lower thresholds
        val hybridMatches = combineResults(patternMatches, fuzzyMatches)
        if (hybridMatches.isNotEmpty()) {
            return ClassificationResult(
                matches = hybridMatches.take(config.maxCandidates),
                method = MatchMethod.HYBRID,
                confidence = hybridMatches.first().score,
                processingTimeMs = currentTimeMillis() - startTime
            )
        }

        // No match found
        return ClassificationResult(
            matches = emptyList(),
            method = MatchMethod.UNKNOWN,
            confidence = 0f,
            processingTimeMs = currentTimeMillis() - startTime
        )
    }

    /**
     * Quick classification using pattern matching only
     *
     * Use for high-frequency commands where speed is critical.
     */
    fun classifyFast(input: String): IntentMatch? {
        return patternMatcher.match(input).firstOrNull()
    }

    /**
     * Check if an exact match exists
     */
    fun hasExactMatch(input: String): Boolean {
        return patternMatcher.hasExactMatch(input)
    }

    /**
     * Combine results from multiple matchers
     */
    private fun combineResults(
        patternMatches: List<IntentMatch>,
        fuzzyMatches: List<IntentMatch>
    ): List<IntentMatch> {
        val combined = mutableMapOf<String, IntentMatch>()

        // Add pattern matches with boosted score
        for (match in patternMatches) {
            combined[match.intent.id] = match.copy(
                score = match.score * config.patternWeight,
                method = MatchMethod.HYBRID
            )
        }

        // Merge fuzzy matches
        for (match in fuzzyMatches) {
            val existing = combined[match.intent.id]
            if (existing != null) {
                // Combine scores
                combined[match.intent.id] = existing.copy(
                    score = (existing.score + match.score * config.fuzzyWeight) / 2
                )
            } else {
                combined[match.intent.id] = match.copy(
                    score = match.score * config.fuzzyWeight,
                    method = MatchMethod.HYBRID
                )
            }
        }

        return combined.values
            .filter { it.score >= config.hybridMinScore }
            .sortedByDescending { it.score }
    }

    /**
     * Get statistics about indexed intents
     */
    fun getStats(): ClassifierStats {
        return ClassifierStats(
            patternCount = patternMatcher.patternCount(),
            intentCount = fuzzyMatcher.intentCount(),
            embeddedCount = semanticMatcher.embeddedIntentCount(),
            semanticAvailable = semanticMatcher.isAvailable()
        )
    }

    /**
     * Clear all indices
     */
    fun clear() {
        patternMatcher.clear()
        fuzzyMatcher.clear()
        semanticMatcher.clear()
        isIndexed = false
    }

    /**
     * Platform-agnostic time function
     */
    private fun currentTimeMillis(): Long {
        // This will be implemented platform-specifically
        return 0L // Placeholder - actual implementation in platform modules
    }
}

/**
 * Classifier configuration
 */
data class ClassifierConfig(
    val exactMatchThreshold: Float = 0.95f,
    val fuzzyMinSimilarity: Float = 0.7f,
    val fuzzyAcceptThreshold: Float = 0.85f,
    val semanticMinSimilarity: Float = 0.6f,
    val hybridMinScore: Float = 0.5f,
    val patternWeight: Float = 1.0f,
    val fuzzyWeight: Float = 0.9f,
    val semanticWeight: Float = 0.85f,
    val maxCandidates: Int = 5
)

/**
 * Classification result
 */
data class ClassificationResult(
    val matches: List<IntentMatch>,
    val method: MatchMethod,
    val confidence: Float,
    val processingTimeMs: Long
) {
    val topMatch: IntentMatch? get() = matches.firstOrNull()
    val hasMatch: Boolean get() = matches.isNotEmpty()
    val isHighConfidence: Boolean get() = confidence >= 0.85f
}

/**
 * Classifier statistics
 */
data class ClassifierStats(
    val patternCount: Int,
    val intentCount: Int,
    val embeddedCount: Int,
    val semanticAvailable: Boolean
)
