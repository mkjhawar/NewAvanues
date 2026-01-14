/**
 * BaseStateDetector.kt - Abstract base class for state detectors
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-23 06:34:25 PDT
 *
 * Provides common infrastructure for state detection implementations.
 * Eliminates boilerplate through template method pattern while allowing
 * specialized detection logic in subclasses.
 *
 * Benefits:
 * - Reduces duplicate code across 7 detectors (~250 lines saved)
 * - Enforces consistent result construction
 * - Centralizes score coercion logic
 * - Simplifies detector implementations
 */
package com.augmentalis.voiceoscore.learnapp.state.detectors

import com.augmentalis.voiceoscore.learnapp.state.AppState
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionContext
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionResult
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionStrategy
import com.augmentalis.voiceoscore.learnapp.state.matchers.ClassNamePatternMatcher
import com.augmentalis.voiceoscore.learnapp.state.matchers.ResourceIdPatternMatcher
import com.augmentalis.voiceoscore.learnapp.state.matchers.TextPatternMatcher

/**
 * Abstract base class for state detectors using Template Method pattern
 *
 * Provides:
 * - Pre-configured pattern matchers (text, ID, class name)
 * - Consistent result construction
 * - Automatic score coercion
 * - Indicator list management
 *
 * Subclasses implement [detectSpecific] with state-specific logic.
 */
abstract class BaseStateDetector(
    final override val targetState: AppState
) : StateDetectionStrategy {

    /** Text pattern matcher for keyword detection */
    protected val textMatcher = TextPatternMatcher()

    /** Resource ID pattern matcher for view ID detection */
    protected val idMatcher = ResourceIdPatternMatcher()

    /** Class name pattern matcher for widget type detection */
    protected val classMatcher = ClassNamePatternMatcher()

    /**
     * Detect state presence (Template Method)
     *
     * Calls [detectSpecific] for state-specific logic, then constructs
     * result with automatic score coercion.
     *
     * @param context Detection context with UI data
     * @return Detection result with confidence and indicators
     */
    final override fun detect(context: StateDetectionContext): StateDetectionResult {
        val indicators = mutableListOf<String>()
        var score = 0f

        // Call template method for state-specific detection
        score = detectSpecific(context, indicators, score)

        // Coerce score to valid range [0, 1]
        score = score.coerceIn(0f, 1f)

        return StateDetectionResult(
            state = targetState,
            confidence = score,
            indicators = indicators
        )
    }

    /**
     * Perform state-specific detection logic
     *
     * Subclasses implement this to:
     * 1. Analyze context data (text, IDs, classes)
     * 2. Add detection indicators to the list
     * 3. Return accumulated confidence score
     *
     * @param context Detection context with UI data
     * @param indicators List to add detection indicators to
     * @param score Starting score (usually 0f)
     * @return Final confidence score (will be coerced to [0, 1])
     */
    protected abstract fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float
}
