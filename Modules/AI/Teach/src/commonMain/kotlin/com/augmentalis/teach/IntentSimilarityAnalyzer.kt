package com.augmentalis.teach
import com.augmentalis.ava.core.domain.model.TrainExample
import kotlin.math.sqrt

/**
 * Phase 1.1: Intent Similarity Analysis
 *
 * Uses TF-IDF (Term Frequency-Inverse Document Frequency) to analyze
 * similarity between intents and detect potentially confusable or duplicate intents.
 *
 * Helps users:
 * - Identify similar intents that could be merged
 * - Detect potential confusion points in training data
 * - Improve intent taxonomy
 */
class IntentSimilarityAnalyzer {

    /**
     * Analyze intent similarities and return recommendations
     */
    fun analyzeSimilarities(
        examples: List<TrainExample>,
        similarityThreshold: Double = 0.6
    ): IntentSimilarityReport {
        if (examples.isEmpty()) {
            return IntentSimilarityReport(emptyList(), emptyList())
        }

        // Group examples by intent
        val intentGroups = examples.groupBy { it.intent }

        // Build TF-IDF model
        val tfidfModel = buildTfidfModel(intentGroups)

        // Calculate pairwise similarities
        val similarities = calculatePairwiseSimilarities(tfidfModel)

        // Find similar intent pairs
        val similarPairs = similarities
            .filter { it.similarity >= similarityThreshold }
            .sortedByDescending { it.similarity }

        // Generate consolidation suggestions
        val suggestions = generateConsolidationSuggestions(similarPairs, intentGroups)

        return IntentSimilarityReport(
            similarIntents = similarPairs,
            consolidationSuggestions = suggestions
        )
    }

    /**
     * Build TF-IDF model for all intents
     */
    private fun buildTfidfModel(
        intentGroups: Map<String, List<TrainExample>>
    ): Map<String, Map<String, Double>> {
        val tfidfVectors = mutableMapOf<String, Map<String, Double>>()

        // Calculate document frequency (DF) for each term
        val documentFrequency = mutableMapOf<String, Int>()
        val totalDocuments = intentGroups.size

        intentGroups.forEach { (_, examples) ->
            val uniqueTerms = examples
                .flatMap { tokenize(it.utterance) }
                .toSet()

            uniqueTerms.forEach { term ->
                documentFrequency[term] = documentFrequency.getOrDefault(term, 0) + 1
            }
        }

        // Calculate TF-IDF for each intent
        intentGroups.forEach { (intent, examples) ->
            val allTokens = examples.flatMap { tokenize(it.utterance) }
            val termFrequency = allTokens.groupingBy { it }.eachCount()
            val maxFreq = termFrequency.values.maxOrNull() ?: 1

            val tfidf = termFrequency.mapValues { (term, freq) ->
                val tf = freq.toDouble() / maxFreq
                val df = documentFrequency[term] ?: 1
                val idf = kotlin.math.ln(totalDocuments.toDouble() / df)
                tf * idf
            }

            tfidfVectors[intent] = tfidf
        }

        return tfidfVectors
    }

    /**
     * Calculate pairwise cosine similarities between all intents
     */
    private fun calculatePairwiseSimilarities(
        tfidfModel: Map<String, Map<String, Double>>
    ): List<IntentSimilarity> {
        val similarities = mutableListOf<IntentSimilarity>()
        val intents = tfidfModel.keys.toList()

        for (i in intents.indices) {
            for (j in i + 1 until intents.size) {
                val intent1 = intents[i]
                val intent2 = intents[j]
                val vector1 = tfidfModel[intent1] ?: emptyMap()
                val vector2 = tfidfModel[intent2] ?: emptyMap()

                val similarity = cosineSimilarity(vector1, vector2)

                if (similarity > 0.0) {
                    similarities.add(
                        IntentSimilarity(
                            intent1 = intent1,
                            intent2 = intent2,
                            similarity = similarity,
                            sharedTerms = findSharedTerms(vector1, vector2)
                        )
                    )
                }
            }
        }

        return similarities
    }

    /**
     * Calculate cosine similarity between two TF-IDF vectors
     */
    private fun cosineSimilarity(
        vector1: Map<String, Double>,
        vector2: Map<String, Double>
    ): Double {
        val allTerms = (vector1.keys + vector2.keys).toSet()

        var dotProduct = 0.0
        var magnitude1 = 0.0
        var magnitude2 = 0.0

        allTerms.forEach { term ->
            val v1 = vector1[term] ?: 0.0
            val v2 = vector2[term] ?: 0.0

            dotProduct += v1 * v2
            magnitude1 += v1 * v1
            magnitude2 += v2 * v2
        }

        val magnitude = sqrt(magnitude1) * sqrt(magnitude2)

        return if (magnitude > 0.0) {
            dotProduct / magnitude
        } else {
            0.0
        }
    }

    /**
     * Find shared terms between two intents
     */
    private fun findSharedTerms(
        vector1: Map<String, Double>,
        vector2: Map<String, Double>
    ): List<String> {
        return (vector1.keys intersect vector2.keys)
            .sortedByDescending { (vector1[it] ?: 0.0) + (vector2[it] ?: 0.0) }
            .take(5) // Top 5 shared terms
    }

    /**
     * Generate consolidation suggestions
     */
    private fun generateConsolidationSuggestions(
        similarPairs: List<IntentSimilarity>,
        intentGroups: Map<String, List<TrainExample>>
    ): List<ConsolidationSuggestion> {
        val suggestions = mutableListOf<ConsolidationSuggestion>()

        // Group highly similar intents (>= 0.8 similarity)
        val highSimilarityPairs = similarPairs.filter { it.similarity >= 0.8 }

        highSimilarityPairs.forEach { pair ->
            val count1 = intentGroups[pair.intent1]?.size ?: 0
            val count2 = intentGroups[pair.intent2]?.size ?: 0

            // Suggest keeping the intent with more examples
            val (keepIntent, mergeIntent) = if (count1 >= count2) {
                pair.intent1 to pair.intent2
            } else {
                pair.intent2 to pair.intent1
            }

            suggestions.add(
                ConsolidationSuggestion(
                    keepIntent = keepIntent,
                    mergeIntent = mergeIntent,
                    similarity = pair.similarity,
                    reason = "High similarity (${(pair.similarity * 100).toInt()}%) - likely duplicates",
                    keepCount = intentGroups[keepIntent]?.size ?: 0,
                    mergeCount = intentGroups[mergeIntent]?.size ?: 0,
                    sharedTerms = pair.sharedTerms
                )
            )
        }

        return suggestions.sortedByDescending { it.similarity }
    }

    /**
     * Tokenize utterance into terms
     * Simple tokenization: lowercase, split on non-alphanumeric, remove stopwords
     */
    private fun tokenize(text: String): List<String> {
        val stopwords = setOf(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "should",
            "could", "may", "might", "must", "can", "i", "you", "he", "she", "it",
            "we", "they", "me", "him", "her", "us", "them", "my", "your", "his",
            "her", "its", "our", "their", "this", "that", "these", "those", "am",
            "in", "on", "at", "to", "from", "with", "by", "for", "of", "as"
        )

        return text
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() && it !in stopwords && it.length > 1 }
    }

    /**
     * Find intents similar to a specific intent
     */
    fun findSimilarIntents(
        targetIntent: String,
        examples: List<TrainExample>,
        limit: Int = 5
    ): List<IntentSimilarity> {
        val report = analyzeSimilarities(examples, similarityThreshold = 0.0)

        return report.similarIntents
            .filter { it.intent1 == targetIntent || it.intent2 == targetIntent }
            .sortedByDescending { it.similarity }
            .take(limit)
    }
}

/**
 * Similarity between two intents
 */
data class IntentSimilarity(
    val intent1: String,
    val intent2: String,
    val similarity: Double,
    val sharedTerms: List<String>
) {
    val similarityPercentage: Int
        get() = (similarity * 100).toInt()

    val similarityLevel: SimilarityLevel
        get() = when {
            similarity >= 0.8 -> SimilarityLevel.VERY_HIGH
            similarity >= 0.6 -> SimilarityLevel.HIGH
            similarity >= 0.4 -> SimilarityLevel.MODERATE
            else -> SimilarityLevel.LOW
        }
}

/**
 * Suggestion to consolidate similar intents
 */
data class ConsolidationSuggestion(
    val keepIntent: String,
    val mergeIntent: String,
    val similarity: Double,
    val reason: String,
    val keepCount: Int,
    val mergeCount: Int,
    val sharedTerms: List<String>
) {
    val totalExamplesAfterMerge: Int
        get() = keepCount + mergeCount
}

/**
 * Complete similarity analysis report
 */
data class IntentSimilarityReport(
    val similarIntents: List<IntentSimilarity>,
    val consolidationSuggestions: List<ConsolidationSuggestion>
) {
    val totalSimilarPairs: Int
        get() = similarIntents.size

    val highSimilarityCount: Int
        get() = similarIntents.count { it.similarity >= 0.8 }

    val moderateSimilarityCount: Int
        get() = similarIntents.count { it.similarity in 0.6..0.8 }
}

/**
 * Similarity level enum
 */
enum class SimilarityLevel {
    VERY_HIGH,  // >= 80%
    HIGH,       // >= 60%
    MODERATE,   // >= 40%
    LOW         // < 40%
}
