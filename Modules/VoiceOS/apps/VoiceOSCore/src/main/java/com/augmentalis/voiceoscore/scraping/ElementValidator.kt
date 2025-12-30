/**
 * ElementValidator.kt - Element validation for safe action execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-23
 *
 * PROBLEM SOLVED:
 * Prevents crashes from attempting actions on invalid elements (empty bounds,
 * invisible elements, elements too small for interaction).
 *
 * CRITICAL FIX:
 * Analysis identified that VoiceCommandProcessor was executing actions without
 * validating element bounds, causing crashes when users tried to interact with
 * invisible or invalid UI elements.
 *
 * VALIDATION LAYERS:
 * 1. Visibility check (isVisibleToUser)
 * 2. Bounds validation (non-empty rectangle)
 * 3. Minimum size check (48dp touch target - Android guideline)
 * 4. Enabled state check
 *
 * See: VoiceOS-Analysis-CommandGeneration-EdgeCases-251223-V1.md
 * See: VoiceOS-Plan-CommandGeneration-Fixes-251223-V1.md (Cluster 2.1)
 */
package com.augmentalis.voiceoscore.scraping

import android.content.res.Resources
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Validates elements before action execution to prevent crashes.
 *
 * **Usage:**
 * ```kotlin
 * val validator = ElementValidator(resources)
 * if (validator.isValidForInteraction(node)) {
 *     node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
 * }
 * ```
 */
class ElementValidator(private val resources: Resources) {

    companion object {
        private const val TAG = "ElementValidator"

        /**
         * Minimum touch target size in DP (Android accessibility guideline).
         * https://developer.android.com/guide/topics/ui/accessibility/principles#touch-target-size
         */
        private const val MIN_TOUCH_TARGET_DP = 48

        /**
         * Metrics for validation failures.
         */
        data class ValidationMetrics(
            var invisibleRejections: Int = 0,
            var emptyBoundsRejections: Int = 0,
            var tooSmallWarnings: Int = 0,
            var disabledRejections: Int = 0,
            var totalValidations: Int = 0
        )

        private val metrics = ValidationMetrics()

        /**
         * Get current validation metrics (for monitoring/debugging).
         */
        fun getMetrics(): ValidationMetrics = metrics.copy()

        /**
         * Reset metrics (for testing).
         */
        fun resetMetrics() {
            metrics.invisibleRejections = 0
            metrics.emptyBoundsRejections = 0
            metrics.tooSmallWarnings = 0
            metrics.disabledRejections = 0
            metrics.totalValidations = 0
        }
    }

    /**
     * Validate element is safe for interaction.
     *
     * Checks:
     * 1. Element is visible to user
     * 2. Element has non-empty bounds
     * 3. Element is enabled
     * 4. Element meets minimum size (warning only, not blocking)
     *
     * @param node AccessibilityNodeInfo to validate
     * @return true if element is safe for interaction, false otherwise
     */
    fun isValidForInteraction(node: AccessibilityNodeInfo): Boolean {
        metrics.totalValidations++

        // Layer 1: Visibility check
        if (!node.isVisibleToUser) {
            metrics.invisibleRejections++
            Log.w(TAG, "Element is not visible to user: ${node.className}")
            return false
        }

        // Layer 2: Bounds validation (CRITICAL)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) {
            metrics.emptyBoundsRejections++
            Log.w(TAG, "Element has empty bounds (CRITICAL): ${node.className}, viewId=${node.viewIdResourceName}")
            return false
        }

        // Layer 3: Enabled state check
        if (!node.isEnabled) {
            metrics.disabledRejections++
            Log.d(TAG, "Element is disabled: ${node.className}")
            return false
        }

        // Layer 4: Minimum size check (warning only, not blocking)
        val minTouchTargetPx = (MIN_TOUCH_TARGET_DP * resources.displayMetrics.density).toInt()
        if (bounds.width() < minTouchTargetPx && bounds.height() < minTouchTargetPx) {
            metrics.tooSmallWarnings++
            Log.w(
                TAG,
                "Element below recommended touch target size (${bounds.width()}x${bounds.height()}px < ${minTouchTargetPx}px): " +
                        "${node.className}. Interaction may be unreliable."
            )
            // WARNING ONLY - still allow interaction in degraded mode
        }

        return true
    }

    /**
     * Validate element has non-empty bounds (standalone check).
     *
     * @param node AccessibilityNodeInfo to validate
     * @return true if bounds are non-empty, false otherwise
     */
    fun hasValidBounds(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return !bounds.isEmpty
    }

    /**
     * Validate element meets minimum touch target size.
     *
     * @param node AccessibilityNodeInfo to validate
     * @return true if element meets minimum size, false otherwise
     */
    fun meetsMinimumSize(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val minTouchTargetPx = (MIN_TOUCH_TARGET_DP * resources.displayMetrics.density).toInt()
        return bounds.width() >= minTouchTargetPx || bounds.height() >= minTouchTargetPx
    }

    /**
     * Get element bounds as string for logging.
     *
     * @param node AccessibilityNodeInfo
     * @return Bounds string in format "[left,top][right,bottom]"
     */
    fun getBoundsString(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return if (bounds.isEmpty) {
            "[EMPTY]"
        } else {
            "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]"
        }
    }

    /**
     * Log validation metrics (for debugging).
     */
    fun logMetrics() {
        Log.i(
            TAG,
            "Validation Metrics: Total=${ metrics.totalValidations}, " +
                    "Invisible=${metrics.invisibleRejections}, " +
                    "EmptyBounds=${metrics.emptyBoundsRejections}, " +
                    "TooSmall=${metrics.tooSmallWarnings}, " +
                    "Disabled=${metrics.disabledRejections}"
        )
    }
}
