/**
 * ClassNamePatternMatcher.kt - Matches patterns in class names
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Matches patterns in Android view class names.
 * Handles both simple class names (e.g., "Button") and
 * full qualified names (e.g., "android.widget.Button").
 */
package com.augmentalis.learnapp.state.matchers

import com.augmentalis.learnapp.state.PatternMatcher
import com.augmentalis.learnapp.state.PatternMatchResult
import com.augmentalis.learnapp.state.StateDetectionContext
import kotlin.math.min

/**
 * Matches class name patterns in UI elements
 *
 * Analyzes classNames field of StateDetectionContext to find matches.
 * Supports matching against both simple names and fully qualified class names.
 */
class ClassNamePatternMatcher : PatternMatcher {

    companion object {
        /**
         * Maximum matches to consider for scoring
         */
        private const val MAX_RELEVANT_MATCHES = 4
    }

    /**
     * Match class name patterns
     *
     * @param context UI data context
     * @param patterns Set of class patterns to match (e.g., "Button", "EditText", "ProgressBar")
     * @return Match result with count and score
     */
    override fun match(
        context: StateDetectionContext,
        patterns: Set<String>
    ): PatternMatchResult {
        if (patterns.isEmpty() || context.classNames.isEmpty()) {
            return PatternMatchResult.noMatch()
        }

        val matchedPatterns = mutableListOf<String>()
        var matchCount = 0

        for (className in context.classNames) {
            for (pattern in patterns) {
                if (matchesClassName(className, pattern)) {
                    matchCount++
                    if (pattern !in matchedPatterns) {
                        matchedPatterns.add(pattern)
                    }
                    break // Count each class only once
                }
            }
        }

        val score = min(matchCount.toFloat() / MAX_RELEVANT_MATCHES, 1.0f)

        return PatternMatchResult(
            matchCount = matchCount,
            matchedPatterns = matchedPatterns,
            score = score
        )
    }

    /**
     * Check if className matches pattern
     *
     * Handles both simple names and fully qualified names.
     * For example, pattern "Button" matches:
     * - "Button"
     * - "android.widget.Button"
     * - "androidx.appcompat.widget.AppCompatButton"
     *
     * @param className Full class name from UI element
     * @param pattern Pattern to match (can be simple or qualified)
     * @return True if pattern matches
     */
    private fun matchesClassName(className: String, pattern: String): Boolean {
        // Direct match (case-insensitive)
        if (className.equals(pattern, ignoreCase = true)) {
            return true
        }

        // Check if className ends with pattern (handles qualified names)
        if (className.endsWith(pattern, ignoreCase = true)) {
            return true
        }

        // Check if className contains pattern (fuzzy match)
        if (className.contains(pattern, ignoreCase = true)) {
            return true
        }

        return false
    }
}
