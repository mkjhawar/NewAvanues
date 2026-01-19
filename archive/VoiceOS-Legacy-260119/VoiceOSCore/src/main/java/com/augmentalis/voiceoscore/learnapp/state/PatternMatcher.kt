/**
 * PatternMatcher.kt - Interface for pattern matching strategies
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Strategy pattern interface for matching UI patterns.
 * Separates pattern matching logic from state detection logic.
 *
 * SOLID Principles:
 * - Single Responsibility: Each matcher handles one type of pattern
 * - Interface Segregation: Small, focused interface
 */
package com.augmentalis.voiceoscore.learnapp.state

/**
 * Strategy interface for matching patterns in UI data
 *
 * Implementations analyze specific aspects of the UI (text, resource IDs, class names)
 * to find patterns that indicate app states.
 *
 * @see TextPatternMatcher
 * @see ResourceIdPatternMatcher
 * @see ClassNamePatternMatcher
 */
interface PatternMatcher {

    /**
     * Match patterns in the given context
     *
     * @param context Detection context containing UI data
     * @param patterns Set of patterns to match against
     * @return Match result with count and score
     */
    fun match(context: StateDetectionContext, patterns: Set<String>): PatternMatchResult
}

/**
 * Result of pattern matching operation
 *
 * @property matchCount Number of patterns that matched
 * @property matchedPatterns List of patterns that matched (for debugging)
 * @property score Calculated match score (0.0-1.0)
 */
data class PatternMatchResult(
    val matchCount: Int,
    val matchedPatterns: List<String> = emptyList(),
    val score: Float = 0f
) {
    companion object {
        /**
         * Create a no-match result
         */
        fun noMatch(): PatternMatchResult = PatternMatchResult(0, emptyList(), 0f)
    }
}
