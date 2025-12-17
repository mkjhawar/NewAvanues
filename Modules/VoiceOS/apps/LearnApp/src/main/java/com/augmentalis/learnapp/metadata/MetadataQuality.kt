/**
 * MetadataQuality.kt - Metadata quality assessment
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/metadata/MetadataQuality.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Represents quality levels for UI element metadata
 */

package com.augmentalis.learnapp.metadata

import com.augmentalis.learnapp.models.ElementInfo

/**
 * Metadata Quality
 *
 * Represents quality assessment of UI element metadata.
 *
 * ## Quality Levels
 *
 * - **EXCELLENT**: Full text + contentDescription + resourceId
 * - **GOOD**: Text + contentDescription OR resourceId
 * - **ACCEPTABLE**: At least one identifier (text OR contentDescription OR resourceId)
 * - **POOR**: Only className (insufficient for voice commands)
 *
 * @since 1.0.0
 */
enum class MetadataQuality {
    /**
     * Excellent metadata - all identifiers present
     */
    EXCELLENT,

    /**
     * Good metadata - multiple identifiers present
     */
    GOOD,

    /**
     * Acceptable metadata - minimum identifier present
     */
    ACCEPTABLE,

    /**
     * Poor metadata - insufficient identifiers
     */
    POOR;

    companion object {
        /**
         * Assess metadata quality for an element
         *
         * @param element Element to assess
         * @return Quality level
         */
        fun assess(element: ElementInfo): MetadataQuality {
            val hasText = element.text.isNotBlank()
            val hasContentDesc = element.contentDescription.isNotBlank()
            val hasResourceId = element.resourceId.isNotBlank()

            val identifierCount = listOf(hasText, hasContentDesc, hasResourceId).count { it }

            return when {
                identifierCount >= 3 -> EXCELLENT
                identifierCount == 2 -> GOOD
                identifierCount == 1 -> ACCEPTABLE
                else -> POOR
            }
        }

        /**
         * Check if quality requires user notification
         *
         * @param quality Quality level
         * @return true if notification recommended
         */
        fun requiresNotification(quality: MetadataQuality): Boolean {
            return quality == POOR
        }

        /**
         * Get quality score (0-100)
         *
         * @param quality Quality level
         * @return Score from 0 (poor) to 100 (excellent)
         */
        fun getScore(quality: MetadataQuality): Int {
            return when (quality) {
                EXCELLENT -> 100
                GOOD -> 75
                ACCEPTABLE -> 50
                POOR -> 25
            }
        }
    }
}

/**
 * Metadata Notification Item
 *
 * Represents a queued notification for insufficient metadata.
 *
 * @property element Element with poor metadata
 * @property quality Assessed quality level
 * @property timestamp When detected (milliseconds)
 * @property suggestions Generated metadata suggestions
 * @property screenHash Current screen hash (for context)
 */
data class MetadataNotificationItem(
    val element: ElementInfo,
    val quality: MetadataQuality,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestions: List<MetadataSuggestion> = emptyList(),
    val screenHash: String = ""
) {
    /**
     * Get priority score for queue ordering
     * Higher = more urgent
     *
     * @return Priority score
     */
    fun getPriority(): Int {
        var priority = when (quality) {
            MetadataQuality.POOR -> 100
            MetadataQuality.ACCEPTABLE -> 50
            MetadataQuality.GOOD -> 25
            MetadataQuality.EXCELLENT -> 0
        }

        // Boost priority for interactive elements
        if (element.isClickable || element.isButton()) {
            priority += 20
        }

        return priority
    }
}
