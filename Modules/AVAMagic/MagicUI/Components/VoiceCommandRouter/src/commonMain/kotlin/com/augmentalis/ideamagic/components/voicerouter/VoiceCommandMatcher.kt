package com.augmentalis.magicui.components.voicerouter

import com.augmentalis.magicui.components.argscanner.ARGFile
import com.augmentalis.magicui.components.argscanner.ARGRegistry
import com.augmentalis.magicui.components.argscanner.Capability

/**
 * Voice Command Matcher
 *
 * Matches natural language voice input against registered capability patterns
 * and extracts parameters.
 *
 * ## Pattern Syntax
 * - `{param}` - Required parameter placeholder
 * - `[optional]` - Optional word/phrase (TODO)
 * - Literal words must match exactly
 *
 * ## Examples
 * ```kotlin
 * val matcher = VoiceCommandMatcher(registry)
 * val results = matcher.match("open google.com")
 *
 * results.forEach { match ->
 *     println("${match.app.name}: ${match.capability.name}")
 *     println("Params: ${match.parameters}")
 *     println("Score: ${match.score}")
 * }
 * ```
 *
 * @since 1.0.0
 */
class VoiceCommandMatcher(
    private val registry: ARGRegistry
) {

    /**
     * Match voice input against all registered capabilities
     *
     * @param input Voice input text (e.g., "open google.com")
     * @return List of matches sorted by score (best first)
     */
    fun match(input: String): List<VoiceCommandMatch> {
        val inputLower = input.lowercase().trim()
        val matches = mutableListOf<VoiceCommandMatch>()

        registry.getAll().forEach { argFile ->
            argFile.capabilities.forEach { capability ->
                capability.voiceCommands.forEach { pattern ->
                    val matchResult = matchPattern(inputLower, pattern.lowercase(), capability)
                    if (matchResult != null) {
                        matches.add(VoiceCommandMatch(
                            app = argFile.app,
                            capability = capability,
                            pattern = pattern,
                            parameters = matchResult.parameters,
                            score = matchResult.score
                        ))
                    }
                }
            }
        }

        return matches.sortedByDescending { it.score }
    }

    /**
     * Match a specific capability pattern
     *
     * @param input User input (lowercase)
     * @param pattern Command pattern (lowercase)
     * @param capability Capability definition for parameter types
     * @return Pattern match result or null if no match
     */
    private fun matchPattern(
        input: String,
        pattern: String,
        capability: Capability
    ): PatternMatchResult? {
        val patternTokens = tokenizePattern(pattern)
        val inputTokens = input.split(Regex("\\s+"))

        // Extract static and dynamic parts
        val staticParts = mutableListOf<String>()
        val dynamicParts = mutableListOf<ParameterPlaceholder>()

        patternTokens.forEach { token ->
            when {
                token.startsWith("{") && token.endsWith("}") -> {
                    val paramName = token.substring(1, token.length - 1)
                    val paramDef = capability.params.find { it.name == paramName }
                    dynamicParts.add(ParameterPlaceholder(
                        name = paramName,
                        position = staticParts.size + dynamicParts.size,
                        type = paramDef?.type,
                        required = paramDef?.required ?: true
                    ))
                }
                else -> staticParts.add(token)
            }
        }

        // Try to match pattern
        val parameters = mutableMapOf<String, String>()
        var score = 1.0f
        var inputIndex = 0
        var patternIndex = 0

        while (patternIndex < patternTokens.size && inputIndex < inputTokens.size) {
            val patternToken = patternTokens[patternIndex]

            if (patternToken.startsWith("{") && patternToken.endsWith("}")) {
                // Dynamic parameter - capture until next static token
                val paramName = patternToken.substring(1, patternToken.length - 1)
                val nextStaticToken = findNextStaticToken(patternTokens, patternIndex + 1)

                val capturedValue = if (nextStaticToken != null) {
                    captureUntil(inputTokens, inputIndex, nextStaticToken)
                } else {
                    // Last parameter - capture everything remaining
                    captureRemaining(inputTokens, inputIndex)
                }

                if (capturedValue.value.isEmpty()) {
                    // Required parameter missing
                    return null
                }

                parameters[paramName] = capturedValue.value
                inputIndex = capturedValue.endIndex
                patternIndex++
            } else {
                // Static token - must match exactly
                if (inputTokens[inputIndex] == patternToken) {
                    score += 0.1f  // Exact match bonus
                    inputIndex++
                    patternIndex++
                } else {
                    // Try fuzzy match
                    if (fuzzyMatch(inputTokens[inputIndex], patternToken)) {
                        score -= 0.1f  // Fuzzy match penalty
                        inputIndex++
                        patternIndex++
                    } else {
                        // No match
                        return null
                    }
                }
            }
        }

        // Check if we consumed all required tokens
        if (patternIndex < patternTokens.size) {
            // Pattern has remaining required tokens
            return null
        }

        // Bonus for exact input length match
        if (inputIndex == inputTokens.size) {
            score += 0.2f
        } else {
            // Penalty for unused input tokens
            score -= (inputTokens.size - inputIndex) * 0.05f
        }

        return PatternMatchResult(parameters, score)
    }

    /**
     * Tokenize pattern into words and placeholders
     */
    private fun tokenizePattern(pattern: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inPlaceholder = false

        pattern.forEach { char ->
            when (char) {
                '{' -> {
                    if (current.isNotEmpty()) {
                        tokens.addAll(current.toString().trim().split(Regex("\\s+")))
                        current.clear()
                    }
                    inPlaceholder = true
                    current.append(char)
                }
                '}' -> {
                    current.append(char)
                    tokens.add(current.toString())
                    current.clear()
                    inPlaceholder = false
                }
                ' ' -> {
                    if (inPlaceholder) {
                        current.append(char)
                    } else if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
        }

        if (current.isNotEmpty()) {
            if (inPlaceholder) {
                tokens.add(current.toString())
            } else {
                tokens.addAll(current.toString().trim().split(Regex("\\s+")))
            }
        }

        return tokens.filter { it.isNotBlank() }
    }

    /**
     * Find next static (non-placeholder) token
     */
    private fun findNextStaticToken(tokens: List<String>, startIndex: Int): String? {
        for (i in startIndex until tokens.size) {
            val token = tokens[i]
            if (!token.startsWith("{") || !token.endsWith("}")) {
                return token
            }
        }
        return null
    }

    /**
     * Capture tokens until we hit the target static token
     */
    private fun captureUntil(
        tokens: List<String>,
        startIndex: Int,
        targetToken: String
    ): CaptureResult {
        val captured = mutableListOf<String>()
        var index = startIndex

        while (index < tokens.size && tokens[index] != targetToken) {
            captured.add(tokens[index])
            index++
        }

        return CaptureResult(
            value = captured.joinToString(" "),
            endIndex = index
        )
    }

    /**
     * Capture all remaining tokens
     */
    private fun captureRemaining(tokens: List<String>, startIndex: Int): CaptureResult {
        val captured = tokens.subList(startIndex, tokens.size)
        return CaptureResult(
            value = captured.joinToString(" "),
            endIndex = tokens.size
        )
    }

    /**
     * Fuzzy string matching (basic Levenshtein-based)
     */
    private fun fuzzyMatch(a: String, b: String): Boolean {
        if (a.length < 3 || b.length < 3) return false

        // Allow 1 character difference for short words
        val distance = levenshteinDistance(a, b)
        return distance <= 1
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[a.length][b.length]
    }
}

/**
 * Voice command match result
 */
data class VoiceCommandMatch(
    val app: com.augmentalis.avanues.avamagic.components.argscanner.AppInfo,
    val capability: Capability,
    val pattern: String,
    val parameters: Map<String, String>,
    val score: Float
)

/**
 * Internal pattern match result
 */
private data class PatternMatchResult(
    val parameters: Map<String, String>,
    val score: Float
)

/**
 * Parameter placeholder in pattern
 */
private data class ParameterPlaceholder(
    val name: String,
    val position: Int,
    val type: com.augmentalis.avanues.avamagic.components.argscanner.ParamType?,
    val required: Boolean
)

/**
 * Capture result
 */
private data class CaptureResult(
    val value: String,
    val endIndex: Int
)
