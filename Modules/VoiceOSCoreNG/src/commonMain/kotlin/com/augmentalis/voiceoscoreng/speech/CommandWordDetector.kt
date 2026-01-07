/**
 * CommandWordDetector.kt - Convert continuous speech to command detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Bridges the gap between continuous speech recognition engines (Vosk, Google, Azure)
 * and command-word based operation (like Vivoka).
 *
 * Continuous engines return free-form text: "I want to go back please"
 * Command-word engines detect specific phrases: "go back"
 *
 * This detector extracts command phrases from continuous text with confidence scoring.
 *
 * References:
 * - Keyword Spotting: https://picovoice.ai/blog/keyword-spotting-voice-recognition/
 * - Microsoft Keyword Recognition: https://learn.microsoft.com/en-us/azure/ai-services/speech-service/keyword-recognition-overview
 */
package com.augmentalis.voiceoscoreng.speech

import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.common.StaticCommandRegistry
import kotlin.math.max
import kotlin.math.min

/**
 * Detects command words in continuous speech text.
 *
 * ## Architecture
 *
 * ```
 * Continuous Speech     CommandWordDetector         Output
 * ─────────────────    ───────────────────────    ────────────────
 * "I want to go       → Phrase Extraction        → CommandMatch
 *  back please"       → Fuzzy Matching            ("go back", 0.95)
 *                     → Confidence Scoring
 * ```
 *
 * ## Matching Strategy
 *
 * 1. **Exact Match**: "go back" in text → 1.0 confidence
 * 2. **Fuzzy Match**: "going back" matches "go back" → 0.85 confidence
 * 3. **Partial Match**: "back" matches "go back" → 0.6 confidence
 * 4. **No Match**: Below threshold → filtered out
 *
 * ## Usage
 *
 * ```kotlin
 * val detector = CommandWordDetector()
 * detector.updateCommands(listOf("go back", "scroll down", "tap settings"))
 *
 * val matches = detector.detectCommands("please go back now")
 * // Returns: [CommandMatch("go back", confidence=0.95, startIndex=7)]
 * ```
 */
class CommandWordDetector(
    /**
     * Minimum confidence threshold for matches (0.0 - 1.0).
     * Lower values catch more matches but increase false positives.
     */
    var confidenceThreshold: Float = 0.7f,

    /**
     * Maximum number of matches to return per detection.
     */
    var maxMatches: Int = 5,

    /**
     * Whether to use fuzzy matching (Levenshtein distance).
     */
    var enableFuzzyMatching: Boolean = true,

    /**
     * Fuzzy matching tolerance (0.0 = exact, 1.0 = very loose).
     */
    var fuzzyTolerance: Float = 0.2f
) {
    // ═══════════════════════════════════════════════════════════════════════
    // Command Registry
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Registered commands for detection
     */
    private val commands = mutableSetOf<String>()

    /**
     * Normalized commands (lowercase, trimmed) for matching
     */
    private val normalizedCommands = mutableMapOf<String, String>() // normalized -> original

    /**
     * Pre-computed word lists for partial matching
     */
    private val commandWords = mutableMapOf<String, List<String>>() // normalized -> words

    // ═══════════════════════════════════════════════════════════════════════
    // Command Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the command list for detection.
     *
     * @param newCommands List of command phrases to detect
     */
    fun updateCommands(newCommands: List<String>) {
        commands.clear()
        normalizedCommands.clear()
        commandWords.clear()

        newCommands.forEach { cmd ->
            val normalized = normalize(cmd)
            if (normalized.isNotEmpty()) {
                commands.add(cmd)
                normalizedCommands[normalized] = cmd
                commandWords[normalized] = normalized.split(" ").filter { it.isNotEmpty() }
            }
        }
    }

    /**
     * Add static commands from registry.
     */
    fun addStaticCommands() {
        updateCommands(commands.toList() + StaticCommandRegistry.allPhrases())
    }

    /**
     * Add dynamic commands from command registry.
     */
    fun addDynamicCommands(registry: CommandRegistry) {
        val dynamicPhrases = registry.getAllCommands().map { it.phrase }
        updateCommands(commands.toList() + dynamicPhrases)
    }

    /**
     * Get current command count.
     */
    val commandCount: Int get() = commands.size

    // ═══════════════════════════════════════════════════════════════════════
    // Command Detection
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Detect command words in continuous speech text.
     *
     * @param text Raw text from continuous speech engine
     * @return List of command matches sorted by confidence (highest first)
     */
    fun detectCommands(text: String): List<CommandMatch> {
        if (text.isBlank() || commands.isEmpty()) {
            return emptyList()
        }

        val normalizedText = normalize(text)
        val textWords = normalizedText.split(" ").filter { it.isNotEmpty() }

        val matches = mutableListOf<CommandMatch>()

        for ((normalizedCmd, originalCmd) in normalizedCommands) {
            val match = matchCommand(normalizedText, textWords, normalizedCmd, originalCmd)
            if (match != null && match.confidence >= confidenceThreshold) {
                matches.add(match)
            }
        }

        // Sort by confidence descending, take top N
        return matches
            .sortedByDescending { it.confidence }
            .take(maxMatches)
    }

    /**
     * Detect the best single command match.
     *
     * @param text Raw text from speech
     * @return Best matching command or null if none found
     */
    fun detectBestCommand(text: String): CommandMatch? {
        return detectCommands(text).firstOrNull()
    }

    /**
     * Check if text contains any registered command.
     */
    fun containsCommand(text: String): Boolean {
        return detectBestCommand(text) != null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Matching Algorithm
    // ═══════════════════════════════════════════════════════════════════════

    private fun matchCommand(
        normalizedText: String,
        textWords: List<String>,
        normalizedCmd: String,
        originalCmd: String
    ): CommandMatch? {
        val cmdWords = commandWords[normalizedCmd] ?: return null

        // Strategy 1: Exact substring match (highest confidence)
        val exactIndex = normalizedText.indexOf(normalizedCmd)
        if (exactIndex >= 0) {
            return CommandMatch(
                command = originalCmd,
                matchedText = extractMatchedText(normalizedText, exactIndex, normalizedCmd.length),
                confidence = 1.0f,
                matchType = MatchType.EXACT,
                startIndex = exactIndex,
                endIndex = exactIndex + normalizedCmd.length
            )
        }

        // Strategy 2: Word sequence match (all command words in order)
        val sequenceMatch = findWordSequence(textWords, cmdWords)
        if (sequenceMatch != null) {
            return CommandMatch(
                command = originalCmd,
                matchedText = cmdWords.joinToString(" "),
                confidence = 0.9f,
                matchType = MatchType.WORD_SEQUENCE,
                startIndex = sequenceMatch.first,
                endIndex = sequenceMatch.second
            )
        }

        // Strategy 3: Fuzzy matching (if enabled)
        if (enableFuzzyMatching) {
            val fuzzyMatch = findFuzzyMatch(textWords, cmdWords, normalizedCmd)
            if (fuzzyMatch != null) {
                return CommandMatch(
                    command = originalCmd,
                    matchedText = fuzzyMatch.matchedText,
                    confidence = fuzzyMatch.confidence,
                    matchType = MatchType.FUZZY,
                    startIndex = -1,
                    endIndex = -1
                )
            }
        }

        // Strategy 4: Partial word match (at least 60% of command words present)
        val partialMatch = findPartialMatch(textWords, cmdWords)
        if (partialMatch != null) {
            return CommandMatch(
                command = originalCmd,
                matchedText = partialMatch.matchedWords.joinToString(" "),
                confidence = partialMatch.confidence,
                matchType = MatchType.PARTIAL,
                startIndex = -1,
                endIndex = -1
            )
        }

        return null
    }

    /**
     * Find word sequence in text (all command words appear in order).
     */
    private fun findWordSequence(
        textWords: List<String>,
        cmdWords: List<String>
    ): Pair<Int, Int>? {
        if (cmdWords.isEmpty() || textWords.size < cmdWords.size) return null

        var cmdIndex = 0
        var startTextIndex = -1
        var endTextIndex = -1

        for (i in textWords.indices) {
            if (textWords[i] == cmdWords[cmdIndex]) {
                if (cmdIndex == 0) startTextIndex = i
                cmdIndex++
                if (cmdIndex == cmdWords.size) {
                    endTextIndex = i
                    return Pair(startTextIndex, endTextIndex)
                }
            }
        }

        return null
    }

    /**
     * Find fuzzy match using Levenshtein distance.
     */
    private fun findFuzzyMatch(
        textWords: List<String>,
        cmdWords: List<String>,
        normalizedCmd: String
    ): FuzzyResult? {
        // Try matching consecutive word windows
        val windowSize = cmdWords.size
        if (textWords.size < windowSize) return null

        var bestMatch: FuzzyResult? = null
        var bestScore = 0f

        for (i in 0..(textWords.size - windowSize)) {
            val windowText = textWords.subList(i, i + windowSize).joinToString(" ")
            val distance = levenshteinDistance(windowText, normalizedCmd)
            val maxLen = max(windowText.length, normalizedCmd.length)
            val similarity = 1f - (distance.toFloat() / maxLen)

            if (similarity > bestScore && similarity >= (1f - fuzzyTolerance)) {
                bestScore = similarity
                bestMatch = FuzzyResult(
                    matchedText = windowText,
                    confidence = similarity * 0.85f // Slight penalty for fuzzy
                )
            }
        }

        return bestMatch
    }

    /**
     * Find partial word match (majority of command words present).
     */
    private fun findPartialMatch(
        textWords: List<String>,
        cmdWords: List<String>
    ): PartialResult? {
        if (cmdWords.isEmpty()) return null

        val matchedWords = cmdWords.filter { it in textWords }
        val matchRatio = matchedWords.size.toFloat() / cmdWords.size

        if (matchRatio >= 0.6f) { // At least 60% of words match
            return PartialResult(
                matchedWords = matchedWords,
                confidence = matchRatio * 0.7f // Penalty for partial
            )
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Utility Functions
    // ═══════════════════════════════════════════════════════════════════════

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .trim()
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove punctuation
            .replace(Regex("\\s+"), " ") // Collapse whitespace
    }

    private fun extractMatchedText(text: String, start: Int, length: Int): String {
        return text.substring(start, min(start + length, text.length))
    }

    /**
     * Levenshtein distance for fuzzy matching.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Data Classes
    // ═══════════════════════════════════════════════════════════════════════

    private data class FuzzyResult(
        val matchedText: String,
        val confidence: Float
    )

    private data class PartialResult(
        val matchedWords: List<String>,
        val confidence: Float
    )
}

/**
 * Result of command detection.
 */
data class CommandMatch(
    /**
     * The original registered command that was matched.
     */
    val command: String,

    /**
     * The actual text from input that matched.
     */
    val matchedText: String,

    /**
     * Confidence score (0.0 - 1.0).
     */
    val confidence: Float,

    /**
     * Type of match achieved.
     */
    val matchType: MatchType,

    /**
     * Start index in normalized input (-1 if not applicable).
     */
    val startIndex: Int,

    /**
     * End index in normalized input (-1 if not applicable).
     */
    val endIndex: Int
) {
    /**
     * Whether this is a high-confidence match.
     */
    val isHighConfidence: Boolean get() = confidence >= 0.85f

    /**
     * Whether this is exact match.
     */
    val isExactMatch: Boolean get() = matchType == MatchType.EXACT
}

/**
 * Type of match achieved.
 */
enum class MatchType {
    /**
     * Exact substring match (highest confidence).
     */
    EXACT,

    /**
     * All command words found in sequence.
     */
    WORD_SEQUENCE,

    /**
     * Fuzzy match using Levenshtein distance.
     */
    FUZZY,

    /**
     * Partial word match (majority of words present).
     */
    PARTIAL
}
