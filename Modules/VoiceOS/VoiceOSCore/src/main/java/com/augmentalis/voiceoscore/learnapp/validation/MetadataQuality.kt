/**
 * MetadataQuality.kt - Quality assessment for element metadata
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

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings

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
class MetadataQuality(context: Context) {

    // Developer settings (lazy initialized)
    private val developerSettings: LearnAppDeveloperSettings by lazy {
        LearnAppDeveloperSettings(context)
    }

    /**
     * Assess the metadata quality of an accessibility node
     *
     * @param node The accessibility node to evaluate
     * @return A detailed quality score with suggestions
     */
    fun assess(node: AccessibilityNodeInfo): MetadataQualityScore {
        var score = 0f
        val suggestions = mutableListOf<String>()

        // Get scoring weights from settings
        val weightText = developerSettings.getQualityWeightText()
        val weightContentDesc = developerSettings.getQualityWeightContentDesc()
        val weightViewId = developerSettings.getQualityWeightResourceId()
        val weightActionable = developerSettings.getQualityWeightActionable()

        // Evaluate text label
        val hasText = !node.text.isNullOrBlank()
        if (hasText) {
            score += weightText
        } else {
            suggestions.add("Add text label to element")
        }

        // Evaluate content description
        val hasContentDesc = !node.contentDescription.isNullOrBlank()
        if (hasContentDesc) {
            score += weightContentDesc
        } else {
            suggestions.add("Add android:contentDescription for accessibility")
        }

        // Evaluate resource ID
        val hasViewId = !node.viewIdResourceName.isNullOrBlank()
        if (hasViewId) {
            score += weightViewId
        } else {
            suggestions.add("Add android:id with meaningful name")
        }

        // Evaluate actionability
        val isActionable = node.isClickable || node.isLongClickable ||
                          node.isEditable || node.isCheckable
        if (isActionable) {
            score += weightActionable
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
