package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ISynonymProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Command Matcher - Fuzzy matching for voice input to commands.
 *
 * Matches voice input against commands in a registry, supporting:
 * - Synonym expansion (e.g., "tap" → "click")
 * - Exact phrase matching
 * - Fuzzy word-based matching
 * - Action type filtering
 *
 * ## Matching Flow:
 * 1. Expand synonyms in input (if provider available)
 * 2. Try exact match with expanded input
 * 3. Try exact match with original input
 * 4. Calculate fuzzy similarity scores
 * 5. Return best match or ambiguous result
 */
object CommandMatcher {

    /**
     * Synonym provider for expanding voice input.
     * Set this to enable synonym-aware matching.
     */
    var synonymProvider: ISynonymProvider? = null

    /**
     * Default language for synonym expansion.
     */
    var defaultLanguage: String = "en"

    /** High-confidence threshold — if any candidate scores this high, skip full sort */
    private const val EARLY_EXIT_THRESHOLD = 0.95f

    /** Max entries in the LRU cache */
    private const val MAX_CACHE_SIZE = 100

    /** LRU cache for fuzzy match results. Key = "input|threshold|actionFilter" */
    private val matchCache = linkedMapOf<String, MatchResult>()

    /** Guards all cache reads/writes and generation checks for thread safety */
    private val cacheMutex = Mutex()

    /** Last known registry generation when cache was valid */
    private var lastRegistryGeneration: Long = -1

    /** Get from LRU cache, promoting entry to most-recently-used position */
    private fun cacheGet(key: String): MatchResult? {
        val value = matchCache.remove(key) ?: return null
        matchCache[key] = value // re-insert at end = most recently used
        return value
    }

    /** Put into LRU cache, evicting oldest entry when full. Single eviction
     *  suffices because we add at most one entry per call. */
    private fun cachePut(key: String, value: MatchResult) {
        matchCache.remove(key)
        matchCache[key] = value
        if (matchCache.size > MAX_CACHE_SIZE) {
            matchCache.remove(matchCache.keys.first())
        }
    }

    /**
     * Match voice input against commands in registry.
     *
     * Performance optimizations (3-layer):
     * 1. LRU cache — repeated inputs return instantly
     * 2. Word pre-filter — reduces candidate set before scoring
     * 3. Early exit — skips full sort when high-confidence match found
     *
     * @param voiceInput Raw voice input string
     * @param registry Command registry to search
     * @param threshold Minimum similarity score (0.0 - 1.0), default 0.7
     * @param actionFilter Optional filter for specific action type
     * @param language Language for synonym expansion (uses defaultLanguage if null)
     * @return MatchResult indicating match type and matched command(s)
     */
    suspend fun match(
        voiceInput: String,
        registry: CommandRegistry,
        threshold: Float = 0.7f,
        actionFilter: CommandActionType? = null,
        language: String? = null,
        provider: ISynonymProvider? = synonymProvider
    ): MatchResult {
        val normalized = voiceInput.lowercase().trim()

        if (normalized.isBlank()) {
            return MatchResult.NoMatch
        }

        val lang = language ?: defaultLanguage
        val expanded = provider?.expand(normalized, lang) ?: normalized

        // Get commands, optionally filtered by action type
        val commands = if (actionFilter != null) {
            registry.all().filter { it.actionType == actionFilter }
        } else {
            registry.all()
        }

        if (commands.isEmpty()) {
            return MatchResult.NoMatch
        }

        // --- Exact match paths (no caching needed, already O(n)) ---

        if (expanded != normalized) {
            commands.firstOrNull { cmd ->
                cmd.phrase.lowercase() == expanded
            }?.let {
                return MatchResult.Exact(it, synonymExpanded = true)
            }
        }

        commands.firstOrNull { cmd ->
            cmd.phrase.lowercase() == normalized
        }?.let {
            return MatchResult.Exact(it)
        }

        val symbolNormalized = SymbolNormalizer.normalize(normalized, lang)
        if (symbolNormalized != normalized) {
            commands.firstOrNull { cmd ->
                SymbolNormalizer.normalize(cmd.phrase.lowercase(), lang) == symbolNormalized
            }?.let {
                return MatchResult.Exact(it, synonymExpanded = true)
            }
        }

        // --- Fuzzy match path (optimized with cache + pre-filter + early exit) ---

        // Layer 1: LRU cache check (mutex protects generation check + cache access atomically)
        val cacheKey = "$normalized|$threshold|${actionFilter?.name ?: ""}"
        val cached = cacheMutex.withLock {
            val registryGen = registry.generation()
            if (registryGen != lastRegistryGeneration) {
                matchCache.clear()
                lastRegistryGeneration = registryGen
            }
            cacheGet(cacheKey)
        }
        if (cached != null) return cached

        // Layer 2: Word pre-filter — only score commands sharing at least one word with input
        val inputWords = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val expandedWords = if (expanded != normalized) {
            expanded.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        } else inputWords
        val allInputWords = inputWords + expandedWords

        val preFiltered = commands.filter { cmd ->
            val phraseWords = cmd.phrase.lowercase().split(Regex("\\s+"))
            phraseWords.any { pw -> allInputWords.any { iw -> pw.contains(iw) || iw.contains(pw) } }
        }

        // Fall back to full scan if pre-filter is too aggressive (no candidates)
        val scoreCandidates = preFiltered.ifEmpty { commands }

        // Layer 3: Score with early exit on high confidence
        var bestCmd: QuantizedCommand? = null
        var bestScore = 0f
        var secondBestScore = 0f
        val aboveThreshold = mutableListOf<Pair<QuantizedCommand, Float>>()

        for (cmd in scoreCandidates) {
            val phraseLC = cmd.phrase.lowercase()
            val scoreOriginal = similarity(normalized, phraseLC)
            val scoreExpanded = if (expanded != normalized) {
                similarity(expanded, phraseLC)
            } else scoreOriginal
            val score = maxOf(scoreOriginal, scoreExpanded)

            if (score >= threshold) {
                aboveThreshold.add(cmd to score)
                if (score > bestScore) {
                    secondBestScore = bestScore
                    bestScore = score
                    bestCmd = cmd
                } else if (score > secondBestScore) {
                    secondBestScore = score
                }

                // Early exit: high-confidence match with clear margin
                if (bestScore >= EARLY_EXIT_THRESHOLD && bestScore - secondBestScore > 0.1f) {
                    val result = MatchResult.Fuzzy(bestCmd!!, bestScore, expanded != normalized)
                    cacheMutex.withLock { cachePut(cacheKey, result) }
                    return result
                }
            }
        }

        val result = when {
            aboveThreshold.isEmpty() -> MatchResult.NoMatch

            aboveThreshold.size == 1 -> MatchResult.Fuzzy(
                aboveThreshold[0].first,
                aboveThreshold[0].second,
                synonymExpanded = expanded != normalized
            )

            isAmbiguous(bestScore, secondBestScore) -> {
                MatchResult.Ambiguous(
                    aboveThreshold.sortedByDescending { it.second }.map { it.first }
                )
            }

            else -> MatchResult.Fuzzy(
                bestCmd!!,
                bestScore,
                synonymExpanded = expanded != normalized
            )
        }

        cacheMutex.withLock { cachePut(cacheKey, result) }
        return result
    }

    /**
     * Match with explicit synonym provider (for testing or one-off use).
     * Thread-safe: passes provider/language as parameters instead of mutating singleton state.
     */
    suspend fun matchWithSynonyms(
        voiceInput: String,
        registry: CommandRegistry,
        provider: ISynonymProvider,
        language: String = "en",
        threshold: Float = 0.7f,
        actionFilter: CommandActionType? = null
    ): MatchResult {
        return match(voiceInput, registry, threshold, actionFilter, language, provider)
    }

    /**
     * Check if two scores are close enough to be considered ambiguous.
     */
    private fun isAmbiguous(score1: Float, score2: Float): Boolean {
        return (score1 - score2) < 0.1f // Within 10% is ambiguous
    }

    /**
     * Match voice input against a phrase with symbol alias support.
     * Handles bidirectional matching:
     * - "sound and vibration" matches "Sound & vibration"
     * - "display size ampersand text" also matches
     *
     * @param voiceInput User's voice input
     * @param phrase Command phrase to match
     * @param locale Locale for symbol normalization
     * @return True if inputs are equivalent after normalization
     */
    fun matchWithSymbolAliases(
        voiceInput: String,
        phrase: String,
        locale: String = "en"
    ): Boolean {
        return SymbolNormalizer.matchWithAliases(voiceInput, phrase, locale)
    }

    /**
     * Calculate similarity between two strings using Jaccard index on words.
     * Also considers partial word matches for better voice input handling.
     * Normalizes symbols before comparison for better matching.
     */
    private fun similarity(input: String, phrase: String): Float {
        val inputWords = input.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val phraseWords = phrase.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

        if (inputWords.isEmpty() || phraseWords.isEmpty()) {
            return 0f
        }

        // Exact word intersection
        val exactIntersection = inputWords.intersect(phraseWords).size

        // Partial matches (input word is substring of phrase word or vice versa)
        var partialMatches = 0f
        for (inputWord in inputWords) {
            for (phraseWord in phraseWords) {
                if (inputWord !in phraseWords && phraseWord !in inputWords) {
                    if (phraseWord.contains(inputWord) || inputWord.contains(phraseWord)) {
                        partialMatches += 0.5f
                        break
                    }
                }
            }
        }

        val union = inputWords.union(phraseWords).size
        val totalMatches = exactIntersection + partialMatches

        return if (union == 0) 0f else (totalMatches / union).coerceIn(0f, 1f)
    }

    /**
     * Result of a command match operation.
     */
    sealed class MatchResult {
        /**
         * Exact phrase match found.
         *
         * @property command The matched command
         * @property synonymExpanded True if match was found via synonym expansion
         */
        data class Exact(
            val command: QuantizedCommand,
            val synonymExpanded: Boolean = false
        ) : MatchResult()

        /**
         * Fuzzy match found with confidence score.
         *
         * @property command The matched command
         * @property confidence Similarity score (0.0 - 1.0)
         * @property synonymExpanded True if match was improved via synonym expansion
         */
        data class Fuzzy(
            val command: QuantizedCommand,
            val confidence: Float,
            val synonymExpanded: Boolean = false
        ) : MatchResult()

        /**
         * Multiple commands matched with similar scores.
         *
         * @property candidates List of ambiguous matches
         */
        data class Ambiguous(val candidates: List<QuantizedCommand>) : MatchResult()

        /**
         * No matching command found.
         */
        data object NoMatch : MatchResult()

        /**
         * Check if this result found a match.
         */
        fun isMatch(): Boolean = this !is NoMatch

        /**
         * Get the matched command, or null if no match.
         */
        fun matchedCommand(): QuantizedCommand? = when (this) {
            is Exact -> command
            is Fuzzy -> command
            is Ambiguous -> candidates.firstOrNull()
            is NoMatch -> null
        }
    }
}
