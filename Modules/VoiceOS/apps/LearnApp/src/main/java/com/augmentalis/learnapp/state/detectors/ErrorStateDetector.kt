/**
 * ErrorStateDetector.kt - Detects error and failure screens
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:35:00 PDT
 *
 * Detects error and failure states using multiple signals:
 * - Error keywords in text (higher weight - critical indicator)
 * - Error-related resource IDs
 * - Retry buttons
 * - Multiple error indicators
 */
package com.augmentalis.learnapp.state.detectors

import com.augmentalis.learnapp.state.AppState
import com.augmentalis.learnapp.state.StateDetectionContext
import com.augmentalis.learnapp.state.StateDetectionPatterns
import com.augmentalis.learnapp.state.StateDetector

/**
 * Detector for error and failure screens
 *
 * Identifies error states through keywords, IDs, and retry actions.
 */
class ErrorStateDetector : BaseStateDetector(AppState.ERROR) {

    override fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float {
        var currentScore = score

        // 1. Text keywords - error-related text (HIGHER weight)
        val textResult = textMatcher.match(context, StateDetectionPatterns.ERROR_KEYWORDS)
        if (textResult.matchCount > 0) {
            val textScore = (StateDetector.WEIGHT_TEXT_KEYWORD + 0.25f) * textResult.score
            currentScore += textScore
            indicators.add("${textResult.matchCount} error keywords (score: +${String.format("%.2f", textScore)})")
        }

        // 2. Resource IDs - error-related IDs
        val idResult = idMatcher.match(context, StateDetectionPatterns.ERROR_VIEW_ID_PATTERNS)
        if (idResult.matchCount > 0) {
            val idScore = StateDetector.WEIGHT_RESOURCE_ID * idResult.score
            currentScore += idScore
            indicators.add("${idResult.matchCount} error view IDs (score: +${String.format("%.2f", idScore)})")
        }

        // 3. Retry button - strong indicator of error state
        val hasRetryButton = context.textContent.any { text ->
            (text.contains("retry", ignoreCase = true) ||
             text.contains("try again", ignoreCase = true))
        } && context.classNames.any { it.contains("Button") }

        if (hasRetryButton) {
            val retryScore = StateDetector.WEIGHT_CONTEXTUAL + 0.1f
            currentScore += retryScore
            indicators.add("Retry button detected (score: +${String.format("%.2f", retryScore)})")
        }

        // 4. Boost for multiple error signals
        if (textResult.matchCount > 0 && idResult.matchCount > 0) {
            currentScore += 0.15f
            indicators.add("Multiple error indicators - high confidence")
        }

        // 5. Additional boost for retry button with error indicators
        if (hasRetryButton && (textResult.matchCount > 0 || idResult.matchCount > 0)) {
            currentScore += 0.1f
            indicators.add("Retry button with error indicators - very high confidence")
        }

        return currentScore
    }
}
