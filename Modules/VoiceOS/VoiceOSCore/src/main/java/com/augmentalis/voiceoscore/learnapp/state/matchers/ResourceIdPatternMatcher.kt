/**
 * ResourceIdPatternMatcher.kt - Matches patterns in resource IDs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Matches patterns in Android resource IDs (viewIdResourceName).
 * Extracts ID name from full package path and performs pattern matching.
 */
package com.augmentalis.voiceoscore.learnapp.state.matchers

import com.augmentalis.voiceoscore.learnapp.state.PatternMatcher
import com.augmentalis.voiceoscore.learnapp.state.PatternMatchResult
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionContext
import kotlin.math.min

/**
 * Matches resource ID patterns in UI elements
 *
 * Analyzes viewIds field of StateDetectionContext.
 * Extracts ID name from full resource identifier (e.g., "com.app:id/btn_login" → "btn_login")
 * then matches against patterns.
 */
class ResourceIdPatternMatcher : PatternMatcher {

    companion object {
        /**
         * Maximum matches to consider for scoring
         */
        private const val MAX_RELEVANT_MATCHES = 3
    }

    /**
     * Match resource ID patterns
     *
     * @param context UI data context
     * @param patterns Set of ID patterns to match (e.g., "btn_login", "et_password")
     * @return Match result with count and score
     */
    override fun match(
        context: StateDetectionContext,
        patterns: Set<String>
    ): PatternMatchResult {
        if (patterns.isEmpty() || context.viewIds.isEmpty()) {
            return PatternMatchResult.noMatch()
        }

        val matchedPatterns = mutableListOf<String>()
        var matchCount = 0

        for (fullResourceId in context.viewIds) {
            val idName = extractResourceIdName(fullResourceId)

            for (pattern in patterns) {
                if (idName.contains(pattern, ignoreCase = true)) {
                    matchCount++
                    if (pattern !in matchedPatterns) {
                        matchedPatterns.add(pattern)
                    }
                    break // Count each resource ID only once
                }
            }
        }

        // Resource IDs are strong signals, so we cap at lower threshold
        val score = min(matchCount.toFloat() / MAX_RELEVANT_MATCHES, 1.0f)

        return PatternMatchResult(
            matchCount = matchCount,
            matchedPatterns = matchedPatterns,
            score = score
        )
    }

    /**
     * Extract resource ID name from full identifier
     *
     * Converts "com.example.app:id/btn_login" to "btn_login"
     *
     * @param fullResourceId Full resource identifier
     * @return Extracted ID name, or original string if no ":id/" found
     */
    private fun extractResourceIdName(fullResourceId: String): String {
        return fullResourceId.substringAfterLast(":id/", fullResourceId)
    }
}
