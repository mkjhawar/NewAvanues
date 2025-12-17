/**
 * DialogStateDetector.kt - Detects modal dialog presence
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:35:00 PDT
 *
 * Detects modal dialogs using multiple signals:
 * - Framework dialog classes (strongest signal)
 * - Generic dialog classes
 * - Dialog-related resource IDs
 * - Standard dialog buttons (OK, Cancel, Yes, No)
 * - Multiple button combinations (2+ dialog buttons)
 */
package com.augmentalis.learnapp.state.detectors

import com.augmentalis.learnapp.state.AppState
import com.augmentalis.learnapp.state.StateDetectionContext
import com.augmentalis.learnapp.state.StateDetectionPatterns
import com.augmentalis.learnapp.state.StateDetector

/**
 * Detector for modal dialogs
 *
 * Identifies dialogs through framework classes, IDs, and button patterns.
 */
class DialogStateDetector : BaseStateDetector(AppState.DIALOG) {

    override fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float {
        var currentScore = score

        // 1. Framework dialog classes (STRONGEST signal)
        val frameworkDialogCount = context.classNames.count { className ->
            StateDetectionPatterns.DIALOG_FRAMEWORK_CLASSES.any { it in className }
        }
        if (frameworkDialogCount > 0) {
            val frameworkScore = StateDetector.WEIGHT_FRAMEWORK_CLASS + 0.1f
            currentScore += frameworkScore
            indicators.add("$frameworkDialogCount framework dialog classes (score: +${String.format("%.2f", frameworkScore)})")
        }

        // 2. Generic dialog class (don't double-count)
        val genericDialogCount = context.classNames.count { it.contains("Dialog", ignoreCase = true) }
        if (genericDialogCount > 0 && frameworkDialogCount == 0) {
            val dialogScore = StateDetector.WEIGHT_CLASS_NAME + 0.2f
            currentScore += dialogScore
            indicators.add("$genericDialogCount generic Dialog classes (score: +${String.format("%.2f", dialogScore)})")
        }

        // 3. Resource IDs - dialog-related IDs
        val idResult = idMatcher.match(context, StateDetectionPatterns.DIALOG_VIEW_ID_PATTERNS)
        if (idResult.matchCount > 0) {
            val idScore = StateDetector.WEIGHT_RESOURCE_ID * idResult.score
            currentScore += idScore
            indicators.add("${idResult.matchCount} dialog view IDs (score: +${String.format("%.2f", idScore)})")
        }

        // 4. Standard dialog buttons (OK, Cancel, Yes, No)
        val dialogButtonCount = context.textContent.count { text ->
            StateDetectionPatterns.DIALOG_KEYWORDS.any { keyword ->
                text.equals(keyword, ignoreCase = true)
            }
        }
        if (dialogButtonCount >= 2) {
            val buttonScore = StateDetector.WEIGHT_CONTEXTUAL + 0.15f
            currentScore += buttonScore
            indicators.add("$dialogButtonCount dialog buttons (score: +${String.format("%.2f", buttonScore)})")
        } else if (dialogButtonCount == 1) {
            val singleButtonScore = StateDetector.WEIGHT_CONTEXTUAL
            currentScore += singleButtonScore
            indicators.add("$dialogButtonCount dialog button (score: +${String.format("%.2f", singleButtonScore)})")
        }

        // 5. Standard Android dialog button IDs (button1, button2, button3)
        val hasStandardButtonIds = context.viewIds.any { id ->
            id.contains("button1") || id.contains("button2") || id.contains("button3")
        }
        if (hasStandardButtonIds) {
            val stdButtonScore = StateDetector.WEIGHT_RESOURCE_ID * 0.7f
            currentScore += stdButtonScore
            indicators.add("Standard Android dialog button IDs (score: +${String.format("%.2f", stdButtonScore)})")
        }

        // 6. Modal dialog layout (limited content)
        val hasDialogClass = genericDialogCount > 0 || frameworkDialogCount > 0
        if (hasDialogClass && context.textContent.size < 10) {
            val modalScore = StateDetector.WEIGHT_CONTEXTUAL * 0.5f
            currentScore += modalScore
            indicators.add("Modal dialog layout - limited content (score: +${String.format("%.2f", modalScore)})")
        }

        // 7. Boost for framework dialog with IDs
        if (frameworkDialogCount > 0 && idResult.matchCount > 0) {
            currentScore += 0.1f
            indicators.add("Framework dialog with resource IDs - very high confidence")
        }

        // 8. Additional boost for framework dialog with buttons
        if (frameworkDialogCount > 0 && dialogButtonCount >= 2) {
            currentScore += 0.05f
            indicators.add("Framework dialog with action buttons - confirmed dialog")
        }

        return currentScore
    }
}
