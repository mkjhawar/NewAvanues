/**
 * EmptyStateDetector.kt - Detects empty state screens
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:35:00 PDT
 *
 * Detects empty state screens (no content, no data) using multiple signals:
 * - Empty state keywords in text
 * - Empty state resource IDs
 * - Action buttons to add content
 */
package com.augmentalis.learnapp.state.detectors

import com.augmentalis.learnapp.state.AppState
import com.augmentalis.learnapp.state.StateDetectionContext
import com.augmentalis.learnapp.state.StateDetectionPatterns
import com.augmentalis.learnapp.state.StateDetector

/**
 * Detector for empty state screens
 *
 * Identifies empty states through keywords, IDs, and action buttons.
 */
class EmptyStateDetector : BaseStateDetector(AppState.EMPTY_STATE) {

    override fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float {
        var currentScore = score

        // 1. Text keywords - empty state text (HIGHER weight)
        val textResult = textMatcher.match(context, StateDetectionPatterns.EMPTY_STATE_KEYWORDS)
        if (textResult.matchCount > 0) {
            val textScore = (StateDetector.WEIGHT_TEXT_KEYWORD + 0.35f) * textResult.score
            currentScore += textScore
            indicators.add("${textResult.matchCount} empty state keywords (score: +${String.format("%.2f", textScore)})")
        }

        // 2. Resource IDs - empty state IDs
        val idResult = idMatcher.match(context, StateDetectionPatterns.EMPTY_STATE_VIEW_ID_PATTERNS)
        if (idResult.matchCount > 0) {
            val idScore = (StateDetector.WEIGHT_RESOURCE_ID + 0.05f) * idResult.score
            currentScore += idScore
            indicators.add("${idResult.matchCount} empty state view IDs (score: +${String.format("%.2f", idScore)})")
        }

        // 3. Action buttons (Get Started, Add, Create)
        val hasActionButton = context.textContent.any { text ->
            (text.contains("get started", ignoreCase = true) ||
             text.contains("add", ignoreCase = true) ||
             text.contains("create", ignoreCase = true))
        } && context.classNames.any { it.contains("Button") }

        if (hasActionButton) {
            val actionScore = StateDetector.WEIGHT_CONTEXTUAL + 0.05f
            currentScore += actionScore
            indicators.add("Action button detected (score: +${String.format("%.2f", actionScore)})")
        }

        // 4. Boost for multiple empty state signals
        if (textResult.matchCount > 0 && idResult.matchCount > 0) {
            currentScore += 0.15f
            indicators.add("Multiple empty state indicators - high confidence")
        }

        return currentScore
    }
}
