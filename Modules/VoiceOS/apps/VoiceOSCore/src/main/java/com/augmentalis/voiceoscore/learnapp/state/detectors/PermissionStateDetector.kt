/**
 * PermissionStateDetector.kt - Detects permission request dialogs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:35:00 PDT
 *
 * Detects permission request screens using multiple signals:
 * - Permission keywords in text
 * - Permission-related resource IDs
 * - Allow/Deny button combinations
 * - Multiple permission indicators
 */
package com.augmentalis.voiceoscore.learnapp.state.detectors

import com.augmentalis.voiceoscore.learnapp.state.AppState
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionContext
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionPatterns
import com.augmentalis.voiceoscore.learnapp.state.StateDetector

/**
 * Detector for permission request dialogs
 *
 * Identifies permission requests through keywords, IDs, and button patterns.
 */
class PermissionStateDetector : BaseStateDetector(AppState.PERMISSION) {

    override fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float {
        var currentScore = score

        // 1. Text keywords - permission-related text
        val textResult = textMatcher.match(context, StateDetectionPatterns.PERMISSION_KEYWORDS)
        if (textResult.matchCount > 0) {
            val textScore = StateDetector.WEIGHT_TEXT_KEYWORD * textResult.score
            currentScore += textScore
            indicators.add("${textResult.matchCount} permission keywords (score: +${String.format("%.2f", textScore)})")
        }

        // 2. Resource IDs - permission-related IDs
        val idResult = idMatcher.match(context, StateDetectionPatterns.PERMISSION_VIEW_ID_PATTERNS)
        if (idResult.matchCount > 0) {
            val idScore = StateDetector.WEIGHT_RESOURCE_ID * idResult.score
            currentScore += idScore
            indicators.add("${idResult.matchCount} permission view IDs (score: +${String.format("%.2f", idScore)})")
        }

        // 3. Allow/Deny button combination (strong indicator)
        val hasAllowButton = context.textContent.any { it.contains("allow", ignoreCase = true) }
        val hasDenyButton = context.textContent.any {
            it.contains("deny", ignoreCase = true) ||
            it.contains("don't allow", ignoreCase = true) ||
            it.contains("never", ignoreCase = true)
        }

        if (hasAllowButton && hasDenyButton) {
            val buttonScore = StateDetector.WEIGHT_CONTEXTUAL + 0.15f
            currentScore += buttonScore
            indicators.add("Allow/Deny button combination (score: +${String.format("%.2f", buttonScore)})")
        } else if (hasAllowButton || hasDenyButton) {
            val singleButtonScore = StateDetector.WEIGHT_CONTEXTUAL * 0.5f
            currentScore += singleButtonScore
            indicators.add("Permission action button (score: +${String.format("%.2f", singleButtonScore)})")
        }

        // 4. Boost for multiple permission signals
        if (textResult.matchCount > 0 && idResult.matchCount > 0) {
            currentScore += 0.1f
            indicators.add("Multiple permission indicators - high confidence")
        }

        // 5. Additional boost for all three signals
        if (textResult.matchCount > 0 && idResult.matchCount > 0 && (hasAllowButton || hasDenyButton)) {
            currentScore += 0.15f
            indicators.add("Permission keywords, IDs, and action buttons - very high confidence")
        }

        return currentScore
    }
}
