/**
 * CommandMatchingService - Unified command matching for all consumers
 *
 * Single entry point for speech recognition, VoiceOSCore, and other modules
 * that need to match user input against known commands.
 *
 * Uses cascading multi-strategy approach:
 * 1. Learned mappings (user corrections)
 * 2. Exact match (fast path)
 * 3. Synonym expansion + exact
 * 4. Fuzzy matching (Levenshtein + Jaccard)
 * 5. Semantic matching (embeddings)
 * 6. Ensemble voting (if multiple candidates)
 *
 * Created: 2026-01-17
 */

package com.augmentalis.nlu.matching

import com.augmentalis.nlu.matcher.EmbeddingProvider
import com.augmentalis.nlu.matcher.FuzzyMatcher
import com.augmentalis.nlu.matcher.PatternMatcher
import com.augmentalis.nlu.matcher.SemanticMatcher
import kotlin.math.max

/**
 * Unified command matching service.
 *
 * Example usage:
 * ```kotlin
 * val service = CommandMatchingService()
 * service.registerCommands(listOf("open calculator", "open camera", "go back"))
 * service.setSynonyms(mapOf("tap" to "click", "press" to "click"))
 *
 * val result = service.match("opn calculater")
 * // Returns: MatchResult.Fuzzy("open calculator", 0.85, MatchStrategy.LEVENSHTEIN)
 * ```
 */
class CommandMatchingService(
    private val config: MatchingConfig = MatchingConfig()
) {
    // Matchers
    private val patternMatcher = PatternMatcher()
    private val fuzzyMatcher = FuzzyMatcher(
        minSimilarity = config.fuzzyThreshold,
        maxCandidates = config.maxCandidates
    )
    private val semanticMatcher = SemanticMatcher(
        minSimilarity = config.semanticThreshold,
        maxCandidates = config.maxCandidates
    )

    // Multilingual support
    private val normalizer = MultilingualNormalizer(config.normalizationConfig)
    private val synonymProvider = LocalizedSynonymProvider()
    private val languageDetector = LanguageDetector()

    // Default locale (can be overridden per-match)
    var defaultLocale: SupportedLocale = SupportedLocale.ENGLISH

    // Command registry
    private val commands = mutableListOf<RegisteredCommand>()
    private val commandIndex = mutableMapOf<String, RegisteredCommand>()

    // Synonym mappings (word -> canonical word) - legacy, use synonymProvider instead
    private val synonyms = mutableMapOf<String, String>()

    // Learned mappings (misrecognition -> correct command)
    private val learnedMappings = mutableMapOf<String, LearnedMapping>()

    // Statistics
    @kotlin.concurrent.Volatile
    private var totalMatches = 0L
    @kotlin.concurrent.Volatile
    private var exactMatches = 0L
    @kotlin.concurrent.Volatile
    private var fuzzyMatches = 0L
    @kotlin.concurrent.Volatile
    private var semanticMatches = 0L
    @kotlin.concurrent.Volatile
    private var noMatches = 0L

    // ==========================================================================
    // Registration API
    // ==========================================================================

    /**
     * Register commands for matching.
     *
     * @param commands List of command phrases
     * @param priority Optional priority (higher = preferred)
     */
    @Synchronized
    fun registerCommands(commands: List<String>, priority: Int = 0) {
        for (phrase in commands) {
            registerCommand(phrase, priority)
        }
        rebuildIndexes()
    }

    /**
     * Register a single command with metadata.
     */
    @Synchronized
    fun registerCommand(
        phrase: String,
        priority: Int = 0,
        category: String? = null,
        actionId: String? = null,
        alternativePhrases: List<String> = emptyList()
    ) {
        val normalized = normalize(phrase)
        val command = RegisteredCommand(
            canonicalPhrase = normalized,
            originalPhrase = phrase,
            priority = priority,
            category = category,
            actionId = actionId,
            alternativePhrases = alternativePhrases.map { normalize(it) }
        )
        commands.add(command)
        commandIndex[normalized] = command
        alternativePhrases.forEach { alt ->
            commandIndex[normalize(alt)] = command
        }
    }

    /**
     * Register synonyms for word expansion.
     *
     * @param synonymMap Map of synonym -> canonical word
     */
    @Synchronized
    fun setSynonyms(synonymMap: Map<String, String>) {
        synonyms.clear()
        synonymMap.forEach { (synonym, canonical) ->
            synonyms[synonym.lowercase()] = canonical.lowercase()
        }
    }

    /**
     * Add a single synonym mapping.
     */
    @Synchronized
    fun addSynonym(synonym: String, canonical: String) {
        synonyms[synonym.lowercase()] = canonical.lowercase()
    }

    /**
     * Set embedding provider for semantic matching.
     */
    fun setEmbeddingProvider(provider: EmbeddingProvider) {
        semanticMatcher.setEmbeddingProvider(provider)
    }

    /**
     * Rebuild internal indexes after registration changes.
     */
    @Synchronized
    fun rebuildIndexes() {
        // Build pattern matcher index
        val unifiedIntents = commands.map { cmd ->
            com.augmentalis.nlu.model.UnifiedIntent(
                id = cmd.actionId ?: cmd.canonicalPhrase,
                canonicalPhrase = cmd.canonicalPhrase,
                patterns = listOf(cmd.canonicalPhrase) + cmd.alternativePhrases,
                synonyms = emptyList(),
                embedding = null,
                category = cmd.category ?: "general",
                actionId = cmd.actionId ?: cmd.canonicalPhrase,
                priority = cmd.priority,
                locale = "en",
                source = "commands"
            )
        }
        patternMatcher.index(unifiedIntents)
        fuzzyMatcher.index(unifiedIntents)
        semanticMatcher.index(unifiedIntents)
    }

    // ==========================================================================
    // Matching API
    // ==========================================================================

    /**
     * Match input against registered commands.
     *
     * Uses cascading strategy:
     * 1. Learned mappings (from user corrections)
     * 2. Exact match
     * 3. Synonym expansion + exact
     * 4. Fuzzy (Levenshtein + Jaccard)
     * 5. Semantic (embeddings)
     * 6. Ensemble voting
     *
     * @param input User input text
     * @param strategies Which strategies to use (default: all enabled)
     * @param locale Target locale for normalization (null = auto-detect or default)
     * @return MatchResult with matched command and metadata
     */
    @Synchronized
    fun match(
        input: String,
        strategies: Set<MatchStrategy> = config.enabledStrategies,
        locale: SupportedLocale? = null
    ): MatchResult {
        totalMatches++
        val effectiveLocale = locale ?: languageDetector.detect(input) ?: defaultLocale
        val normalized = normalizer.normalize(input, effectiveLocale)

        if (normalized.isBlank()) {
            noMatches++
            return MatchResult.NoMatch
        }

        // Stage 1: Learned mappings (fastest)
        if (MatchStrategy.LEARNED in strategies) {
            learnedMappings[normalized]?.let { learned ->
                exactMatches++
                return MatchResult.Exact(
                    command = learned.correctCommand,
                    strategy = MatchStrategy.LEARNED,
                    metadata = mapOf("original_misrecognition" to learned.originalInput)
                )
            }
        }

        // Stage 2: Exact match
        if (MatchStrategy.EXACT in strategies) {
            commandIndex[normalized]?.let { cmd ->
                exactMatches++
                return MatchResult.Exact(
                    command = cmd.canonicalPhrase,
                    strategy = MatchStrategy.EXACT
                )
            }
        }

        // Stage 3: Synonym expansion + exact
        if (MatchStrategy.SYNONYM in strategies && synonyms.isNotEmpty()) {
            val expanded = expandSynonyms(normalized)
            if (expanded != normalized) {
                commandIndex[expanded]?.let { cmd ->
                    exactMatches++
                    return MatchResult.Exact(
                        command = cmd.canonicalPhrase,
                        strategy = MatchStrategy.SYNONYM,
                        metadata = mapOf("expanded_from" to normalized)
                    )
                }
            }
        }

        // Stage 4: Fuzzy matching
        val fuzzyCandidates = mutableListOf<ScoredCandidate>()

        if (MatchStrategy.LEVENSHTEIN in strategies) {
            val levenshteinResults = matchLevenshtein(normalized)
            fuzzyCandidates.addAll(levenshteinResults)
        }

        if (MatchStrategy.JACCARD in strategies) {
            val jaccardResults = matchJaccard(normalized)
            fuzzyCandidates.addAll(jaccardResults)
        }

        // Stage 5: Semantic matching
        val semanticCandidates = mutableListOf<ScoredCandidate>()

        if (MatchStrategy.SEMANTIC in strategies && semanticMatcher.isAvailable()) {
            val semanticResults = matchSemantic(normalized)
            semanticCandidates.addAll(semanticResults)
        }

        // Stage 6: Ensemble voting / selection
        val allCandidates = (fuzzyCandidates + semanticCandidates)
            .groupBy { it.command }
            .map { (command, scores) ->
                // Combine scores from different strategies
                val combinedScore = combineScores(scores)
                val strategies = scores.map { it.strategy }.toSet()
                ScoredCandidate(command, combinedScore, strategies.first(), strategies)
            }
            .filter { it.score >= config.minimumConfidence }
            .sortedByDescending { it.score }

        return when {
            allCandidates.isEmpty() -> {
                noMatches++
                MatchResult.NoMatch
            }

            allCandidates.size == 1 -> {
                recordMatch(allCandidates[0])
                MatchResult.Fuzzy(
                    command = allCandidates[0].command,
                    confidence = allCandidates[0].score,
                    strategy = allCandidates[0].strategy,
                    metadata = mapOf("all_strategies" to allCandidates[0].allStrategies.map { it.name })
                )
            }

            isAmbiguous(allCandidates[0], allCandidates[1]) -> {
                MatchResult.Ambiguous(
                    candidates = allCandidates.take(config.maxCandidates).map {
                        AmbiguousCandidate(it.command, it.score, it.strategy)
                    }
                )
            }

            else -> {
                recordMatch(allCandidates[0])
                MatchResult.Fuzzy(
                    command = allCandidates[0].command,
                    confidence = allCandidates[0].score,
                    strategy = allCandidates[0].strategy,
                    metadata = mapOf(
                        "all_strategies" to allCandidates[0].allStrategies.map { it.name },
                        "runner_up" to allCandidates.getOrNull(1)?.command
                    )
                )
            }
        }
    }

    /**
     * Quick exact-only match (for hot paths).
     */
    @Synchronized
    fun matchExact(input: String): String? {
        val normalized = normalize(input)
        return commandIndex[normalized]?.canonicalPhrase
    }

    /**
     * Match with specific strategy only.
     */
    @Synchronized
    fun matchWith(input: String, strategy: MatchStrategy): MatchResult {
        return match(input, setOf(strategy))
    }

    // ==========================================================================
    // Learning API
    // ==========================================================================

    /**
     * Learn from user correction.
     *
     * When user corrects a misrecognition, store the mapping for future fast lookup.
     *
     * @param misrecognized What the system heard
     * @param correct What the user actually meant
     */
    @Synchronized
    fun learn(misrecognized: String, correct: String) {
        val normalizedMis = normalize(misrecognized)
        val normalizedCorrect = normalize(correct)

        // Only learn if correct is a valid command
        if (commandIndex.containsKey(normalizedCorrect)) {
            learnedMappings[normalizedMis] = LearnedMapping(
                originalInput = misrecognized,
                correctCommand = normalizedCorrect,
                learnedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Remove a learned mapping.
     */
    @Synchronized
    fun unlearn(misrecognized: String) {
        learnedMappings.remove(normalize(misrecognized))
    }

    /**
     * Get all learned mappings (for persistence).
     */
    @Synchronized
    fun getLearnedMappings(): Map<String, LearnedMapping> = learnedMappings.toMap()

    /**
     * Restore learned mappings (from persistence).
     */
    @Synchronized
    fun restoreLearnedMappings(mappings: Map<String, LearnedMapping>) {
        learnedMappings.clear()
        learnedMappings.putAll(mappings)
    }

    // ==========================================================================
    // Internal Matching Methods
    // ==========================================================================

    private fun matchLevenshtein(normalized: String): List<ScoredCandidate> {
        return commands
            .map { cmd ->
                val score = levenshteinSimilarity(normalized, cmd.canonicalPhrase)
                ScoredCandidate(cmd.canonicalPhrase, score, MatchStrategy.LEVENSHTEIN)
            }
            .filter { it.score >= config.fuzzyThreshold }
            .sortedByDescending { it.score }
            .take(config.maxCandidates)
    }

    private fun matchJaccard(normalized: String): List<ScoredCandidate> {
        return commands
            .map { cmd ->
                val score = jaccardSimilarity(normalized, cmd.canonicalPhrase)
                ScoredCandidate(cmd.canonicalPhrase, score, MatchStrategy.JACCARD)
            }
            .filter { it.score >= config.fuzzyThreshold }
            .sortedByDescending { it.score }
            .take(config.maxCandidates)
    }

    private fun matchSemantic(normalized: String): List<ScoredCandidate> {
        val results = semanticMatcher.match(normalized)
        return results.map { match ->
            ScoredCandidate(
                match.intent.canonicalPhrase,
                match.score,
                MatchStrategy.SEMANTIC
            )
        }
    }

    // ==========================================================================
    // Similarity Algorithms
    // ==========================================================================

    /**
     * Levenshtein distance-based similarity (optimized single-row).
     */
    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val len1 = s1.length
        val len2 = s2.length

        // Single-row optimization
        var previousRow = IntArray(len2 + 1) { it }
        var currentRow = IntArray(len2 + 1)

        for (i in 1..len1) {
            currentRow[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currentRow[j] = minOf(
                    currentRow[j - 1] + 1,
                    previousRow[j] + 1,
                    previousRow[j - 1] + cost
                )
            }
            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }

        val distance = previousRow[len2]
        val maxLength = max(len1, len2)
        return 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Jaccard similarity on words (handles word reordering).
     */
    private fun jaccardSimilarity(s1: String, s2: String): Float {
        val words1 = s1.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val words2 = s2.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

        if (words1.isEmpty() || words2.isEmpty()) return 0f

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        // Base Jaccard
        var score = intersection.toFloat() / union

        // Bonus for partial word matches
        var partialBonus = 0f
        for (w1 in words1) {
            for (w2 in words2) {
                if (w1 !in words2 && w2 !in words1) {
                    if (w1.contains(w2) || w2.contains(w1)) {
                        partialBonus += 0.1f
                    }
                }
            }
        }

        return (score + partialBonus).coerceIn(0f, 1f)
    }

    /**
     * Expand synonyms in input text.
     */
    private fun expandSynonyms(text: String): String {
        val words = text.split(Regex("\\s+"))
        val expanded = words.map { word ->
            synonyms[word] ?: word
        }
        return expanded.joinToString(" ")
    }

    /**
     * Combine scores from multiple strategies.
     */
    private fun combineScores(scores: List<ScoredCandidate>): Float {
        if (scores.isEmpty()) return 0f
        if (scores.size == 1) return scores[0].score

        // Weighted combination with agreement bonus
        val weightedSum = scores.sumOf { candidate ->
            val weight = config.strategyWeights[candidate.strategy] ?: 1.0f
            (candidate.score * weight).toDouble()
        }
        val totalWeight = scores.sumOf { candidate ->
            (config.strategyWeights[candidate.strategy] ?: 1.0f).toDouble()
        }

        val baseScore = (weightedSum / totalWeight).toFloat()

        // Agreement bonus: multiple strategies agreeing boosts confidence
        val agreementBonus = (scores.size - 1) * config.agreementBonus

        return (baseScore + agreementBonus).coerceIn(0f, 1f)
    }

    /**
     * Check if two candidates are too close (ambiguous).
     */
    private fun isAmbiguous(c1: ScoredCandidate, c2: ScoredCandidate): Boolean {
        return (c1.score - c2.score) < config.ambiguityThreshold
    }

    /**
     * Normalize text for matching.
     */
    private fun normalize(text: String): String {
        return text.lowercase().trim().replace(Regex("\\s+"), " ")
    }

    private fun recordMatch(candidate: ScoredCandidate) {
        when (candidate.strategy) {
            MatchStrategy.LEVENSHTEIN, MatchStrategy.JACCARD -> fuzzyMatches++
            MatchStrategy.SEMANTIC -> semanticMatches++
            else -> exactMatches++
        }
    }

    // ==========================================================================
    // Statistics & Utilities
    // ==========================================================================

    /**
     * Get matching statistics.
     */
    @Synchronized
    fun getStatistics(): MatchingStatistics {
        return MatchingStatistics(
            totalMatches = totalMatches,
            exactMatches = exactMatches,
            fuzzyMatches = fuzzyMatches,
            semanticMatches = semanticMatches,
            noMatches = noMatches,
            learnedMappingsCount = learnedMappings.size,
            commandsCount = commands.size,
            synonymsCount = synonyms.size
        )
    }

    /**
     * Reset statistics.
     */
    @Synchronized
    fun resetStatistics() {
        totalMatches = 0
        exactMatches = 0
        fuzzyMatches = 0
        semanticMatches = 0
        noMatches = 0
    }

    /**
     * Clear all data.
     */
    @Synchronized
    fun clear() {
        commands.clear()
        commandIndex.clear()
        synonyms.clear()
        learnedMappings.clear()
        patternMatcher.clear()
        fuzzyMatcher.clear()
        semanticMatcher.clear()
        resetStatistics()
    }

    /**
     * Get registered command count.
     */
    @Synchronized
    fun commandCount(): Int = commands.size

    /**
     * Check if a command is registered.
     */
    @Synchronized
    fun hasCommand(phrase: String): Boolean = commandIndex.containsKey(normalize(phrase))
}

// =============================================================================
// Data Classes
// =============================================================================

/**
 * Configuration for matching service.
 */
data class MatchingConfig(
    val fuzzyThreshold: Float = 0.7f,
    val semanticThreshold: Float = 0.6f,
    val minimumConfidence: Float = 0.5f,
    val ambiguityThreshold: Float = 0.1f,
    val maxCandidates: Int = 5,
    val agreementBonus: Float = 0.05f,
    val normalizationConfig: NormalizationConfig = NormalizationConfig(),
    val enabledStrategies: Set<MatchStrategy> = setOf(
        MatchStrategy.LEARNED,
        MatchStrategy.EXACT,
        MatchStrategy.SYNONYM,
        MatchStrategy.LEVENSHTEIN,
        MatchStrategy.JACCARD,
        MatchStrategy.SEMANTIC
    ),
    val strategyWeights: Map<MatchStrategy, Float> = mapOf(
        MatchStrategy.LEARNED to 1.0f,
        MatchStrategy.EXACT to 1.0f,
        MatchStrategy.SYNONYM to 0.95f,
        MatchStrategy.LEVENSHTEIN to 0.85f,
        MatchStrategy.JACCARD to 0.8f,
        MatchStrategy.SEMANTIC to 0.9f
    )
)

/**
 * Matching strategies available.
 */
enum class MatchStrategy {
    LEARNED,      // From user corrections
    EXACT,        // Direct string match
    SYNONYM,      // Synonym expansion + exact
    LEVENSHTEIN,  // Edit distance
    JACCARD,      // Word overlap
    SEMANTIC,     // Embedding similarity
    PHONEME       // Phonetic similarity (future)
}

/**
 * Result of a match operation.
 */
sealed class MatchResult {
    /**
     * Exact match found.
     */
    data class Exact(
        val command: String,
        val strategy: MatchStrategy,
        val metadata: Map<String, Any?> = emptyMap()
    ) : MatchResult()

    /**
     * Fuzzy match found with confidence score.
     */
    data class Fuzzy(
        val command: String,
        val confidence: Float,
        val strategy: MatchStrategy,
        val metadata: Map<String, Any?> = emptyMap()
    ) : MatchResult()

    /**
     * Multiple candidates with similar scores.
     */
    data class Ambiguous(
        val candidates: List<AmbiguousCandidate>
    ) : MatchResult()

    /**
     * No match found.
     */
    data object NoMatch : MatchResult()

    /**
     * Get matched command or null.
     */
    fun commandOrNull(): String? = when (this) {
        is Exact -> command
        is Fuzzy -> command
        is Ambiguous -> candidates.firstOrNull()?.command
        is NoMatch -> null
    }

    /**
     * Check if a match was found.
     */
    fun isMatch(): Boolean = this !is NoMatch
}

/**
 * Candidate in ambiguous result.
 */
data class AmbiguousCandidate(
    val command: String,
    val confidence: Float,
    val strategy: MatchStrategy
)

/**
 * Internal: Scored candidate during matching.
 */
internal data class ScoredCandidate(
    val command: String,
    val score: Float,
    val strategy: MatchStrategy,
    val allStrategies: Set<MatchStrategy> = setOf(strategy)
)

/**
 * Registered command with metadata.
 */
data class RegisteredCommand(
    val canonicalPhrase: String,
    val originalPhrase: String,
    val priority: Int = 0,
    val category: String? = null,
    val actionId: String? = null,
    val alternativePhrases: List<String> = emptyList()
)

/**
 * Learned mapping from user correction.
 */
data class LearnedMapping(
    val originalInput: String,
    val correctCommand: String,
    val learnedAt: Long
)

/**
 * Matching statistics.
 */
data class MatchingStatistics(
    val totalMatches: Long,
    val exactMatches: Long,
    val fuzzyMatches: Long,
    val semanticMatches: Long,
    val noMatches: Long,
    val learnedMappingsCount: Int,
    val commandsCount: Int,
    val synonymsCount: Int
) {
    val exactRate: Float get() = if (totalMatches > 0) exactMatches.toFloat() / totalMatches else 0f
    val fuzzyRate: Float get() = if (totalMatches > 0) fuzzyMatches.toFloat() / totalMatches else 0f
    val semanticRate: Float get() = if (totalMatches > 0) semanticMatches.toFloat() / totalMatches else 0f
    val missRate: Float get() = if (totalMatches > 0) noMatches.toFloat() / totalMatches else 0f
}

// Platform-specific time function
internal expect fun currentTimeMillis(): Long
