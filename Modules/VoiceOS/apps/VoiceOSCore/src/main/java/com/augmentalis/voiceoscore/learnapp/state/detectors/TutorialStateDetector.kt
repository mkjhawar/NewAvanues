/**
 * TutorialStateDetector.kt - Detects tutorial/onboarding screens
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:35:00 PDT
 *
 * Detects tutorial and onboarding screens using multiple signals:
 * - Tutorial/onboarding keywords in text
 * - Tutorial-related resource IDs
 * - Skip/Next navigation buttons
 * - Page indicators
 */
package com.augmentalis.voiceoscore.learnapp.state.detectors

import com.augmentalis.voiceoscore.learnapp.state.AppState
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionContext
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionPatterns
import com.augmentalis.voiceoscore.learnapp.state.StateDetector

/**
 * Detector for tutorial/onboarding screens
 *
 * Identifies tutorials through keywords, navigation buttons, and page indicators.
 */
class TutorialStateDetector : BaseStateDetector(AppState.TUTORIAL) {

    override fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float {
        var currentScore = score

        // 1. Text keywords - tutorial/onboarding text
        val textResult = textMatcher.match(context, StateDetectionPatterns.TUTORIAL_KEYWORDS)
        if (textResult.matchCount > 0) {
            val textScore = StateDetector.WEIGHT_TEXT_KEYWORD * textResult.score
            currentScore += textScore
            indicators.add("${textResult.matchCount} tutorial keywords (score: +${String.format("%.2f", textScore)})")
        }

        // 2. Resource IDs - tutorial-related IDs
        val idResult = idMatcher.match(context, StateDetectionPatterns.TUTORIAL_VIEW_ID_PATTERNS)
        if (idResult.matchCount > 0) {
            val idScore = StateDetector.WEIGHT_RESOURCE_ID * idResult.score
            currentScore += idScore
            indicators.add("${idResult.matchCount} tutorial view IDs (score: +${String.format("%.2f", idScore)})")
        }

        // 3. Skip button (common in tutorials)
        val hasSkipButton = context.textContent.any { it.contains("skip", ignoreCase = true) } &&
                           context.classNames.any { it.contains("Button") }
        if (hasSkipButton) {
            val skipScore = StateDetector.WEIGHT_CONTEXTUAL + 0.1f
            currentScore += skipScore
            indicators.add("Skip button detected (score: +${String.format("%.2f", skipScore)})")
        }

        // 4. Next button (tutorial progression)
        val hasNextButton = context.textContent.any { it.contains("next", ignoreCase = true) } &&
                           context.classNames.any { it.contains("Button") }
        if (hasNextButton) {
            val nextScore = StateDetector.WEIGHT_CONTEXTUAL
            currentScore += nextScore
            indicators.add("Next button detected (score: +${String.format("%.2f", nextScore)})")
        }

        // 5. Page indicator patterns (ViewPager/Carousel)
        val hasPageIndicator = context.viewIds.any {
            it.contains("indicator", ignoreCase = true) ||
            it.contains("pager", ignoreCase = true) ||
            it.contains("dot", ignoreCase = true)
        }
        if (hasPageIndicator) {
            val indicatorScore = StateDetector.WEIGHT_CONTEXTUAL
            currentScore += indicatorScore
            indicators.add("Page indicator detected (score: +${String.format("%.2f", indicatorScore)})")
        }

        // 6. Boost for multiple tutorial signals
        if (textResult.matchCount > 0 && idResult.matchCount > 0) {
            currentScore += 0.1f
            indicators.add("Multiple tutorial indicators - high confidence")
        }

        // 7. Additional boost for skip+next combination
        if (hasSkipButton && hasNextButton) {
            currentScore += 0.15f
            indicators.add("Skip/Next combination - typical onboarding flow")
        }

        return currentScore
    }
}
