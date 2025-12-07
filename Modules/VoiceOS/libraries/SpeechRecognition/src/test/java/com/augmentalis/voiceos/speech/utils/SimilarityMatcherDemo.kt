/**
 * SimilarityMatcherDemo.kt - Demonstration of fuzzy matching capabilities
 *
 * Shows real-world examples of how SimilarityMatcher handles common
 * speech recognition errors and typos.
 *
 * Created: 2025-10-09 02:55:24 PDT
 */

package com.augmentalis.voiceos.speech.utils

/**
 * Demo utility to showcase SimilarityMatcher capabilities
 * Run this to see example similarity scores for common use cases
 */
object SimilarityMatcherDemo {

    /**
     * Common voice commands that might be recognized incorrectly
     */
    private val commonCommands = listOf(
        "open calculator",
        "open camera",
        "open calendar",
        "go back",
        "go home",
        "volume up",
        "volume down",
        "turn on wifi",
        "turn off wifi",
        "turn on bluetooth",
        "turn off bluetooth",
        "call mom",
        "send message",
        "take screenshot",
        "open settings"
    )

    /**
     * Demo: Common speech recognition errors
     */
    fun demoTypicalErrors(): String {
        val results = StringBuilder()
        results.appendLine("=== TYPICAL SPEECH RECOGNITION ERRORS ===\n")

        val testCases = listOf(
            "opn calculator" to "open calculator",
            "opn calcluator" to "open calculator",
            "go bak" to "go back",
            "volum up" to "volume up",
            "turn on wiif" to "turn on wifi",
            "tak screenshot" to "take screenshot",
            "send mesage" to "send message",
            "cal mom" to "call mom"
        )

        for ((misheard, expected) in testCases) {
            val result = SimilarityMatcher.findMostSimilarWithConfidence(
                input = misheard,
                commands = commonCommands,
                threshold = 0.70f
            )

            val similarity = if (result != null) {
                val percentage = (result.second * 100).toInt()
                "$percentage%"
            } else {
                "No match"
            }

            val matched = result?.first ?: "NONE"
            val status = if (matched == expected) "✓" else "✗"

            results.appendLine("Input: '$misheard'")
            results.appendLine("  Expected: $expected")
            results.appendLine("  Matched:  $matched ($similarity) $status")
            results.appendLine()
        }

        return results.toString()
    }

    /**
     * Demo: Similarity scores for various distances
     */
    fun demoSimilarityScores(): String {
        val results = StringBuilder()
        results.appendLine("=== SIMILARITY SCORE EXAMPLES ===\n")

        val testPairs = listOf(
            "hello" to "hello",           // Exact match
            "hello" to "helo",            // 1 char missing
            "hello" to "hallo",           // 1 char different
            "calculator" to "calcluator", // 1 char typo
            "open" to "opn",              // 1 char missing
            "wifi" to "wiif",             // 1 char swapped
            "go back" to "go bak",        // 1 char missing
            "volume" to "volum",          // 1 char missing
            "hello" to "world",           // Completely different
            "abc" to "xyz"                // Completely different
        )

        for ((s1, s2) in testPairs) {
            val similarity = SimilarityMatcher.calculateSimilarity(s1, s2)
            val distance = SimilarityMatcher.levenshteinDistance(s1, s2)
            val percentage = (similarity * 100).toInt()

            results.appendLine("'$s1' vs '$s2'")
            results.appendLine("  Similarity: $percentage% (distance: $distance)")
            results.appendLine()
        }

        return results.toString()
    }

    /**
     * Demo: Finding multiple similar commands
     */
    fun demoMultipleMatches(): String {
        val results = StringBuilder()
        results.appendLine("=== MULTIPLE MATCHES (ALTERNATIVES) ===\n")

        val testInputs = listOf(
            "open" to 0.40f,
            "turn" to 0.30f,
            "volum" to 0.60f
        )

        for ((input, threshold) in testInputs) {
            val matches = SimilarityMatcher.findAllSimilar(
                input = input,
                commands = commonCommands,
                threshold = threshold,
                maxResults = 5
            )

            results.appendLine("Input: '$input' (threshold: ${(threshold * 100).toInt()}%)")
            if (matches.isEmpty()) {
                results.appendLine("  No matches found")
            } else {
                matches.forEachIndexed { index, (command, score) ->
                    val percentage = (score * 100).toInt()
                    results.appendLine("  ${index + 1}. $command ($percentage%)")
                }
            }
            results.appendLine()
        }

        return results.toString()
    }

    /**
     * Demo: Confidence level recommendations
     */
    fun demoConfidenceLevels(): String {
        val results = StringBuilder()
        results.appendLine("=== CONFIDENCE LEVEL RECOMMENDATIONS ===\n")

        val testCases = listOf(
            "open calculator" to "open calculator",  // Exact match
            "opn calculator" to "open calculator",   // High similarity
            "open calc" to "open calculator",        // Medium similarity
            "calculator" to "open calculator",       // Lower similarity
            "xyz" to "open calculator"               // No match
        )

        for ((input, command) in testCases) {
            val similarity = SimilarityMatcher.calculateSimilarity(input, command)
            val percentage = (similarity * 100).toInt()

            val recommendation = when {
                similarity >= 0.90f -> "HIGH - Execute immediately (green)"
                similarity >= 0.70f -> "MEDIUM - Ask confirmation (yellow)"
                similarity >= 0.60f -> "LOW - Show alternatives (orange)"
                else -> "REJECT - Command not recognized (red)"
            }

            results.appendLine("Input: '$input' vs '$command'")
            results.appendLine("  Similarity: $percentage%")
            results.appendLine("  Recommendation: $recommendation")
            results.appendLine()
        }

        return results.toString()
    }

    /**
     * Run all demos
     */
    fun runAllDemos(): String {
        val results = StringBuilder()
        results.appendLine("╔═══════════════════════════════════════════════════════════════╗")
        results.appendLine("║        SIMILARITY MATCHER DEMONSTRATION                       ║")
        results.appendLine("╚═══════════════════════════════════════════════════════════════╝")
        results.appendLine()

        results.append(demoTypicalErrors())
        results.appendLine()
        results.append(demoSimilarityScores())
        results.appendLine()
        results.append(demoMultipleMatches())
        results.appendLine()
        results.append(demoConfidenceLevels())

        return results.toString()
    }
}

/**
 * Main function to run the demo (for testing purposes)
 */
fun main() {
    println(SimilarityMatcherDemo.runAllDemos())
}
