/**
 * PoorQualityElementInfo.kt - Info about poor quality elements
 *
 * Data classes for tracking and managing UI elements with insufficient metadata.
 * Used to queue elements for user notification and track metadata quality issues
 * across screen captures.
 */
package com.augmentalis.learnapp.validation

import android.graphics.Rect

/**
 * Information about an element with poor metadata quality
 *
 * Captures essential details about elements that don't meet the minimum
 * metadata requirements for reliable voice command generation.
 *
 * @property className The element's class name (e.g., "android.widget.Button")
 * @property bounds The element's screen boundaries
 * @property suggestions Prioritized list of improvement suggestions
 * @property elementHash Unique identifier for this element
 * @property depth Depth in the view hierarchy (0 = root)
 * @property timestamp When this element was captured (milliseconds since epoch)
 */
data class PoorQualityElementInfo(
    val className: String,
    val bounds: Rect,
    val suggestions: List<String>,
    val elementHash: String,
    val depth: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get the most critical suggestion for this element
     * @return The highest priority suggestion
     */
    fun getPrioritySuggestion(): String {
        return suggestions.firstOrNull() ?: "No suggestions available"
    }

    /**
     * Check if this element is at the same position as another
     * @param other Another element to compare with
     * @return true if bounds overlap
     */
    fun overlaps(other: PoorQualityElementInfo): Boolean {
        return Rect.intersects(bounds, other.bounds)
    }
}

/**
 * Queue item for metadata notifications
 *
 * Combines poor quality element information with its detailed quality score
 * and screen context for notification purposes.
 *
 * @property element Information about the poor quality element
 * @property qualityScore The detailed quality assessment
 * @property screenHash Hash identifying the screen this element appeared on
 */
data class MetadataNotificationItem(
    val element: PoorQualityElementInfo,
    val qualityScore: MetadataQualityScore,
    val screenHash: String
) {
    /**
     * Check if this notification is still relevant based on age
     * @param maxAgeMs Maximum age in milliseconds (default: 30 seconds)
     * @return true if the notification is still fresh
     */
    fun isFresh(maxAgeMs: Long = 30_000): Boolean {
        return System.currentTimeMillis() - element.timestamp < maxAgeMs
    }

    /**
     * Get a concise summary for notification display
     * @return A brief summary string
     */
    fun getSummary(): String {
        return "${element.className} at (${element.bounds.centerX()},${element.bounds.centerY()}): ${element.getPrioritySuggestion()}"
    }
}
