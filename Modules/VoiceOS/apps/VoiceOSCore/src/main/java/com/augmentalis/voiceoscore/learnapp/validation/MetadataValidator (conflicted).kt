/**
 * MetadataValidator.kt - Validates element metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Provides validation services for UI element metadata, checking if elements
 * have sufficient information for voice command generation. Wraps MetadataQuality
 * with convenient validation methods and report generation.
 *
 * Usage:
 * ```
 * val validator = MetadataValidator()
 * if (!validator.hasSufficientMetadata(node)) {
 *     Log.w(TAG, validator.generateReport(node))
 * }
 * ```
 */
package com.augmentalis.voiceoscore.learnapp.validation

import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

/**
 * Validator for UI element metadata quality
 *
 * Provides convenient methods for validating element metadata and
 * generating human-readable quality reports.
 */
class MetadataValidator {

    companion object {
        private const val TAG = "MetadataValidator"
    }

    /**
     * Validate an element's metadata quality
     *
     * @param node The accessibility node to validate
     * @return A detailed quality score with suggestions
     */
    fun validateElement(node: AccessibilityNodeInfo): MetadataQualityScore {
        return MetadataQuality.assess(node)
    }

    /**
     * Check if an element has sufficient metadata for voice commands
     *
     * @param node The accessibility node to check
     * @return true if metadata is acceptable or better (not POOR)
     */
    fun hasSufficientMetadata(node: AccessibilityNodeInfo): Boolean {
        val score = validateElement(node)
        return score.isSufficient()
    }

    /**
     * Generate a human-readable report of element metadata quality
     *
     * Creates a formatted report showing:
     * - Element class name
     * - Overall quality level and score
     * - Checklist of present metadata
     * - Prioritized suggestions for improvement
     *
     * @param node The accessibility node to report on
     * @return A formatted string report
     */
    fun generateReport(node: AccessibilityNodeInfo): String {
        val score = validateElement(node)
        return buildString {
            appendLine("Metadata Quality Report")
            appendLine("=".repeat(50))
            appendLine("Class: ${score.className}")
            appendLine("Quality: ${score.level} (score: ${String.format("%.2f", score.score)})")
            appendLine()

            // Show what metadata is present
            if (score.hasText) appendLine("  ✓ Has text")
            if (score.hasContentDescription) appendLine("  ✓ Has contentDescription")
            if (score.hasViewId) appendLine("  ✓ Has resource ID")
            if (score.isActionable) appendLine("  ✓ Is actionable")
            appendLine()

            // Show improvement suggestions if any
            if (score.suggestions.isNotEmpty()) {
                appendLine("Suggestions:")
                score.suggestions.forEach { appendLine("  • $it") }
            }
        }
    }

    /**
     * Log a validation report for debugging
     *
     * @param node The accessibility node to report on
     * @param level The log level to use (default: WARN)
     */
    fun logReport(node: AccessibilityNodeInfo, level: Int = Log.WARN) {
        val report = generateReport(node)
        when (level) {
            Log.ERROR -> Log.e(TAG, report)
            Log.WARN -> Log.w(TAG, report)
            Log.INFO -> Log.i(TAG, report)
            Log.DEBUG -> Log.d(TAG, report)
            else -> Log.v(TAG, report)
        }
    }
}
