/**
 * NegativeIndicatorAnalyzer.kt - Detects contradictory UI patterns
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Identifies contradictory patterns in UI analysis that should reduce
 * confidence scores. For example, a RecyclerView present on what appears
 * to be a login screen is a negative indicator.
 */
package com.augmentalis.voiceoscore.learnapp.state.advanced

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.learnapp.state.AppState

/**
 * Negative indicator detection result
 */
data class NegativeIndicator(
    val type: NegativeIndicatorType,
    val penaltyWeight: Float,
    val reason: String,
    val affectedState: AppState
)

/**
 * Types of negative indicators
 */
enum class NegativeIndicatorType {
    COMPLEX_UI_ON_SIMPLE_STATE,     // RecyclerView on login screen
    INCONSISTENT_HIERARCHY,          // Too many nested dialogs
    CONFLICTING_INDICATORS,          // Loading + Error simultaneously
    ABNORMAL_ELEMENT_COUNT,          // Too many/few elements for state
    MISMATCHED_INTERACTION,          // Non-clickable buttons
    TEMPORAL_INCONSISTENCY           // State duration doesn't match pattern
}

/**
 * Analyzes UI patterns to detect contradictory indicators
 *
 * Applies penalty weights to confidence scores when contradictory patterns
 * are detected. Uses state-specific rules to identify anomalies.
 */
class NegativeIndicatorAnalyzer(context: Context) {

    companion object {
        private const val TAG = "NegativeIndicatorAnalyzer"

        // Complex UI component classes
        private val COMPLEX_UI_CLASSES = setOf(
            "RecyclerView",
            "ListView",
            "GridView",
            "ViewPager",
            "ViewPager2"
        )

        // Expected element count ranges for different states
        private val STATE_ELEMENT_RANGES = mapOf(
            AppState.LOGIN to 3..15,
            AppState.LOADING to 1..8,
            AppState.ERROR to 2..12,
            AppState.PERMISSION to 3..10,
            AppState.TUTORIAL to 5..25,
            AppState.EMPTY_STATE to 2..10,
            AppState.DIALOG to 2..15
        )
    }

    // Developer settings (lazy initialized)
    private val developerSettings: LearnAppDeveloperSettings by lazy {
        LearnAppDeveloperSettings(context)
    }

    /**
     * Analyze UI tree for negative indicators
     *
     * @param rootNode Root of accessibility tree
     * @param detectedState Currently detected state
     * @param textContent Collected text content
     * @param classNames Collected class names
     * @return List of negative indicators found
     */
    fun analyzeNegativeIndicators(
        rootNode: AccessibilityNodeInfo?,
        detectedState: AppState,
        textContent: List<String>,
        classNames: List<String>
    ): List<NegativeIndicator> {
        if (rootNode == null) return emptyList()

        val indicators = mutableListOf<NegativeIndicator>()

        // Check for complex UI on simple states
        indicators.addAll(checkComplexUIOnSimpleState(detectedState, classNames))

        // Check for inconsistent hierarchy
        indicators.addAll(checkInconsistentHierarchy(rootNode, detectedState))

        // Check for conflicting indicators
        indicators.addAll(checkConflictingIndicators(detectedState, textContent))

        // Check for abnormal element counts
        indicators.addAll(checkAbnormalElementCount(detectedState, textContent, classNames))

        // Check for mismatched interactions
        indicators.addAll(checkMismatchedInteractions(rootNode, detectedState))

        return indicators
    }

    /**
     * Calculate total penalty from negative indicators
     *
     * @param indicators List of negative indicators
     * @return Total penalty to subtract from confidence (0.0 to 1.0)
     */
    fun calculateTotalPenalty(indicators: List<NegativeIndicator>): Float {
        return indicators.sumOf { it.penaltyWeight.toDouble() }
            .toFloat()
            .coerceAtMost(0.8f) // Maximum 80% penalty
    }

    /**
     * Apply penalties to confidence score
     *
     * @param baseConfidence Original confidence score
     * @param indicators List of negative indicators
     * @return Adjusted confidence score
     */
    fun applyPenalties(baseConfidence: Float, indicators: List<NegativeIndicator>): Float {
        val totalPenalty = calculateTotalPenalty(indicators)
        return (baseConfidence - totalPenalty).coerceAtLeast(0.0f)
    }

    /**
     * Check for complex UI components on simple states
     */
    private fun checkComplexUIOnSimpleState(
        state: AppState,
        classNames: List<String>
    ): List<NegativeIndicator> {
        val indicators = mutableListOf<NegativeIndicator>()

        // States that should NOT have complex UI
        val simpleStates = setOf(
            AppState.LOGIN,
            AppState.LOADING,
            AppState.ERROR,
            AppState.PERMISSION,
            AppState.EMPTY_STATE
        )

        if (state in simpleStates) {
            val complexComponents = classNames.filter { className ->
                COMPLEX_UI_CLASSES.any { className.contains(it) }
            }

            if (complexComponents.isNotEmpty()) {
                indicators.add(
                    NegativeIndicator(
                        type = NegativeIndicatorType.COMPLEX_UI_ON_SIMPLE_STATE,
                        penaltyWeight = developerSettings.getPenaltyMajorContradiction(),
                        reason = "Complex UI found on $state: ${complexComponents.joinToString()}",
                        affectedState = state
                    )
                )
            }
        }

        return indicators
    }

    /**
     * Check for inconsistent hierarchy patterns
     */
    private fun checkInconsistentHierarchy(
        rootNode: AccessibilityNodeInfo,
        state: AppState
    ): List<NegativeIndicator> {
        val indicators = mutableListOf<NegativeIndicator>()

        val maxDepth = calculateMaxDepth(rootNode)

        // Check for excessive nesting
        if (maxDepth > 20) {
            indicators.add(
                NegativeIndicator(
                    type = NegativeIndicatorType.INCONSISTENT_HIERARCHY,
                    penaltyWeight = developerSettings.getPenaltyModerateContradiction(),
                    reason = "Excessive hierarchy depth: $maxDepth levels",
                    affectedState = state
                )
            )
        }

        // Check for shallow hierarchy on complex states
        if (state == AppState.READY && maxDepth < 5) {
            indicators.add(
                NegativeIndicator(
                    type = NegativeIndicatorType.INCONSISTENT_HIERARCHY,
                    penaltyWeight = developerSettings.getPenaltyMinorContradiction(),
                    reason = "Suspiciously shallow hierarchy for READY state: $maxDepth levels",
                    affectedState = state
                )
            )
        }

        return indicators
    }

    /**
     * Check for conflicting indicators in text content
     */
    private fun checkConflictingIndicators(
        state: AppState,
        textContent: List<String>
    ): List<NegativeIndicator> {
        val indicators = mutableListOf<NegativeIndicator>()

        val textLower = textContent.joinToString(" ").lowercase()

        // Check for loading + error
        if (state == AppState.LOADING && textLower.contains("error")) {
            indicators.add(
                NegativeIndicator(
                    type = NegativeIndicatorType.CONFLICTING_INDICATORS,
                    penaltyWeight = developerSettings.getPenaltyModerateContradiction(),
                    reason = "Error text found in LOADING state",
                    affectedState = state
                )
            )
        }

        // Check for login + content lists
        if (state == AppState.LOGIN && (textLower.contains("feed") || textLower.contains("posts"))) {
            indicators.add(
                NegativeIndicator(
                    type = NegativeIndicatorType.CONFLICTING_INDICATORS,
                    penaltyWeight = developerSettings.getPenaltyModerateContradiction(),
                    reason = "Content indicators found in LOGIN state",
                    affectedState = state
                )
            )
        }

        return indicators
    }

    /**
     * Check for abnormal element counts
     */
    private fun checkAbnormalElementCount(
        state: AppState,
        textContent: List<String>,
        classNames: List<String>
    ): List<NegativeIndicator> {
        val indicators = mutableListOf<NegativeIndicator>()

        val expectedRange = STATE_ELEMENT_RANGES[state] ?: return indicators
        val totalElements = textContent.size + classNames.size

        if (totalElements !in expectedRange) {
            val penalty = if (totalElements < expectedRange.first || totalElements > expectedRange.last * 2) {
                developerSettings.getPenaltyModerateContradiction()
            } else {
                developerSettings.getPenaltyMinorContradiction()
            }

            indicators.add(
                NegativeIndicator(
                    type = NegativeIndicatorType.ABNORMAL_ELEMENT_COUNT,
                    penaltyWeight = penalty,
                    reason = "$state expects ${expectedRange.first}-${expectedRange.last} elements, found $totalElements",
                    affectedState = state
                )
            )
        }

        return indicators
    }

    /**
     * Check for mismatched interactions (e.g., non-clickable buttons)
     */
    private fun checkMismatchedInteractions(
        rootNode: AccessibilityNodeInfo,
        state: AppState
    ): List<NegativeIndicator> {
        val indicators = mutableListOf<NegativeIndicator>()
        val mismatchedButtons = mutableListOf<String>()

        traverseForMismatchedButtons(rootNode, mismatchedButtons)

        if (mismatchedButtons.isNotEmpty()) {
            indicators.add(
                NegativeIndicator(
                    type = NegativeIndicatorType.MISMATCHED_INTERACTION,
                    penaltyWeight = developerSettings.getPenaltyMinorContradiction(),
                    reason = "Found ${mismatchedButtons.size} non-interactive buttons",
                    affectedState = state
                )
            )
        }

        return indicators
    }

    /**
     * Calculate maximum hierarchy depth
     */
    private fun calculateMaxDepth(node: AccessibilityNodeInfo, currentDepth: Int = 0): Int {
        if (node.childCount == 0) return currentDepth

        var maxChildDepth = currentDepth
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val childDepth = calculateMaxDepth(child, currentDepth + 1)
                maxChildDepth = maxOf(maxChildDepth, childDepth)
            }
        }
        return maxChildDepth
    }

    /**
     * Traverse tree to find mismatched button interactions
     */
    private fun traverseForMismatchedButtons(
        node: AccessibilityNodeInfo,
        mismatchedButtons: MutableList<String>
    ) {
        val className = node.className?.toString() ?: ""

        // Check if it's a button that's not clickable
        if (className.contains("Button", ignoreCase = true) &&
            !node.isClickable &&
            node.isEnabled) {
            node.text?.toString()?.let { mismatchedButtons.add(it) }
        }

        // Traverse children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverseForMismatchedButtons(child, mismatchedButtons)
            }
        }
    }
}
