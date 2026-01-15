/**
 * TextPatternMatcher.kt - Matches patterns in text content
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Matches keywords and phrases in UI text content.
 * Case-insensitive matching with frequency-based scoring.
 */
package com.augmentalis.voiceoscore.learnapp.state.matchers

import com.augmentalis.voiceoscore.learnapp.state.PatternMatcher
import com.augmentalis.voiceoscore.learnapp.state.PatternMatchResult
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionContext
import kotlin.math.min

/**
 * Matches text patterns in UI content
 *
 * Analyzes textContent field of StateDetectionContext to find keyword matches.
 * Provides frequency-based scoring where more matches = higher confidence.
 */
class TextPatternMatcher : PatternMatcher {

    companion object {
        /**
         * Maximum matches to consider for scoring
         * Beyond this, additional matches don't increase score
         */
        private const val MAX_RELEVANT_MATCHES = 5
    }

    /**
     * Match text patterns against UI text content
     *
     * Performs case-insensitive substring matching.
     * Score calculated as: min(matchCount / MAX_RELEVANT_MATCHES, 1.0)
     *
     * @param context UI data context
     * @param patterns Set of keywords/phrases to match
     * @return Match result with count and score
     */
    override fun match(
        context: StateDetectionContext,
        patterns: Set<String>
    ): PatternMatchResult {
        if (patterns.isEmpty() || context.textContent.isEmpty()) {
            return PatternMatchResult.noMatch()
        }

        val matchedPatterns = mutableListOf<String>()
        var matchCount = 0

        // Count how many text elements contain any pattern
        for (text in context.textContent) {
            for (pattern in patterns) {
                if (text.contains(pattern, ignoreCase = true)) {
                    matchCount++
                    if (pattern !in matchedPatterns) {
                        matchedPatterns.add(pattern)
                    }
                    break // Count each text element only once
                }
            }
        }

        // Calculate score: more matches = higher confidence, capped at 1.0
        val score = min(matchCount.toFloat() / MAX_RELEVANT_MATCHES, 1.0f)

        return PatternMatchResult(
            matchCount = matchCount,
            matchedPatterns = matchedPatterns,
            score = score
        )
    }
}
