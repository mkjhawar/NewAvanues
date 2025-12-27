/**
 * MetadataQuality.kt - Quality assessment for element metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Evaluates the quality of UI element metadata to determine if it's sufficient
 * for reliable voice command generation. Uses a weighted scoring system based on
 * presence of text, contentDescription, viewId, and actionable properties.
 *
 * Scoring Weights:
 * - Text label: 30%
 * - Content description: 25%
 * - View ID: 30%
 * - Actionable: 15%
 *
 * Quality Levels:
 * - EXCELLENT (>=0.8): Perfect for voice commands
 * - GOOD (>=0.6): Good for voice commands
 * - ACCEPTABLE (>=0.4): Usable but could be better
 * - POOR (<0.4): Insufficient for reliable voice commands
 */
package com.augmentalis.voiceoscore.learnapp.validation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Quality levels for element metadata
 */
enum class MetadataQualityLevel {
    EXCELLENT,   // >= 0.8 - Perfect for voice commands
    GOOD,        // >= 0.6 - Good for voice commands
    ACCEPTABLE,  // >= 0.4 - Usable but could be better
    POOR         // < 0.4 - Insufficient for reliable voice commands
}

/**
 * Quality score result with detailed breakdown
 *
 * @property level The overall quality level
 * @property score Numeric score from 0.0 to 1.0
 * @property hasText Whether element has text label
 * @property hasContentDescription Whether element has contentDescription
 * @property hasViewId Whether element has resource ID
 * @property isActionable Whether element supports user interaction
 * @property className The element's class name
 * @property suggestions List of improvement suggestions, prioritized by importance
 */
data class MetadataQualityScore(
    val level: MetadataQualityLevel,
    val score: Float,  // 0.0-1.0
    val hasText: Boolean,
    val hasContentDescription: Boolean,
    val hasViewId: Boolean,
    val isActionable: Boolean,
    val className: String,
    val suggestions: List<String>
) {
    /**
     * Check if metadata is sufficient for voice commands
     * @return true if quality level is acceptable or better
     */
    fun isSufficient(): Boolean = level != MetadataQualityLevel.POOR

    /**
     * Get the most important suggestion for improvement
     * @return The highest priority suggestion, or null if none
     */
    fun getPrioritySuggestion(): String? = suggestions.firstOrNull()
}

/**
 * Assess metadata quality for UI elements
 *
 * Evaluates accessibility metadata and provides actionable suggestions
 * for improving element discoverability and voice command generation.
 */
object MetadataQuality {

    // Scoring weights for different metadata aspects
    private const val WEIGHT_TEXT = 0.3f
    private const val WEIGHT_CONTENT_DESC = 0.25f
    private const val WEIGHT_VIEW_ID = 0.3f
    private const val WEIGHT_ACTIONABLE = 0.15f

    /**
     * Assess the metadata quality of an accessibility node
     *
     * @param node The accessibility node to evaluate
     * @return A detailed quality score with suggestions
     */
    fun assess(node: AccessibilityNodeInfo): MetadataQualityScore {
        var score = 0f
        val suggestions = mutableListOf<String>()

        // Evaluate text label (30% weight)
        val hasText = !node.text.isNullOrBlank()
        if (hasText) {
            score += WEIGHT_TEXT
        } else {
            suggestions.add("Add text label to element")
        }

        // Evaluate content description (25% weight)
        val hasContentDesc = !node.contentDescription.isNullOrBlank()
        if (hasContentDesc) {
            score += WEIGHT_CONTENT_DESC
        } else {
            suggestions.add("Add android:contentDescription for accessibility")
        }

        // Evaluate resource ID (30% weight)
        val hasViewId = !node.viewIdResourceName.isNullOrBlank()
        if (hasViewId) {
            score += WEIGHT_VIEW_ID
        } else {
            suggestions.add("Add android:id with meaningful name")
        }

        // Evaluate actionability (15% weight)
        val isActionable = node.isClickable || node.isLongClickable ||
                          node.isEditable || node.isCheckable
        if (isActionable) {
            score += WEIGHT_ACTIONABLE
        }

        // Determine quality level based on score
        val level = when {
            score >= 0.8f -> MetadataQualityLevel.EXCELLENT
            score >= 0.6f -> MetadataQualityLevel.GOOD
            score >= 0.4f -> MetadataQualityLevel.ACCEPTABLE
            else -> MetadataQualityLevel.POOR
        }

        // Add critical warning for elements with no identifying information
        if (level == MetadataQualityLevel.POOR && !hasText && !hasContentDesc) {
            suggestions.add(0, "CRITICAL: Element has no text or description")
        }

        return MetadataQualityScore(
            level = level,
            score = score,
            hasText = hasText,
            hasContentDescription = hasContentDesc,
            hasViewId = hasViewId,
            isActionable = isActionable,
            className = node.className?.toString() ?: "unknown",
            suggestions = suggestions
        )
    }

    /**
     * Determine if a poor quality score requires user notification
     *
     * @param score The quality score to evaluate
     * @return true if the user should be notified about poor metadata
     */
    fun requiresNotification(score: MetadataQualityScore): Boolean {
        return score.level == MetadataQualityLevel.POOR
    }
}
