/**
 * LoadingStateDetector.kt - Detects loading/processing screens
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:35:00 PDT
 *
 * Detects loading and processing states using multiple signals:
 * - Framework progress indicators (ProgressBar classes - strongest signal)
 * - Loading-related keywords in text
 * - Loading-related resource IDs
 * - Minimal content with progress (splash screen pattern)
 */
package com.augmentalis.learnapp.state.detectors

import com.augmentalis.learnapp.state.AppState
import com.augmentalis.learnapp.state.StateDetectionContext
import com.augmentalis.learnapp.state.StateDetectionPatterns
import com.augmentalis.learnapp.state.StateDetector

/**
 * Detector for loading/processing screens
 *
 * Identifies loading states through progress indicators, keywords,
 * and content analysis.
 */
class LoadingStateDetector : BaseStateDetector(AppState.LOADING) {

    override fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float {
        var currentScore = score

        // 1. Framework progress indicator classes (STRONGEST signal)
        val frameworkProgressCount = context.classNames.count { className ->
            StateDetectionPatterns.PROGRESS_FRAMEWORK_CLASSES.any { it in className }
        }
        if (frameworkProgressCount > 0) {
            val frameworkScore = StateDetector.WEIGHT_FRAMEWORK_CLASS
            currentScore += frameworkScore
            indicators.add("$frameworkProgressCount framework progress indicators (score: +${String.format("%.2f", frameworkScore)})")
        }

        // 2. Generic ProgressBar class
        val progressBarCount = context.classNames.count { it.contains("ProgressBar") }
        if (progressBarCount > 0 && frameworkProgressCount == 0) {
            val progressScore = StateDetector.WEIGHT_CLASS_NAME + 0.2f
            currentScore += progressScore
            indicators.add("$progressBarCount ProgressBar widgets (score: +${String.format("%.2f", progressScore)})")
        }

        // 3. Text keywords - loading-related text
        val textResult = textMatcher.match(context, StateDetectionPatterns.LOADING_KEYWORDS)
        if (textResult.matchCount > 0) {
            val textScore = StateDetector.WEIGHT_TEXT_KEYWORD * textResult.score
            currentScore += textScore
            indicators.add("${textResult.matchCount} loading keywords (score: +${String.format("%.2f", textScore)})")
        }

        // 4. Resource IDs - loading-related IDs
        val idResult = idMatcher.match(context, StateDetectionPatterns.LOADING_VIEW_ID_PATTERNS)
        if (idResult.matchCount > 0) {
            val idScore = StateDetector.WEIGHT_RESOURCE_ID * idResult.score
            currentScore += idScore
            indicators.add("${idResult.matchCount} loading view IDs (score: +${String.format("%.2f", idScore)})")
        }

        // 5. Minimal content with progress (splash screen pattern)
        val hasProgress = progressBarCount > 0 || frameworkProgressCount > 0
        if (context.textContent.size < 5 && hasProgress) {
            val splashScore = StateDetector.WEIGHT_CONTEXTUAL
            currentScore += splashScore
            indicators.add("Minimal content with progress indicator (score: +${String.format("%.2f", splashScore)})")
        }

        // 6. Boost for multiple strong signals
        if (frameworkProgressCount > 0 && idResult.matchCount > 0) {
            currentScore += 0.1f
            indicators.add("Framework progress with resource IDs - very high confidence")
        }

        return currentScore
    }
}
