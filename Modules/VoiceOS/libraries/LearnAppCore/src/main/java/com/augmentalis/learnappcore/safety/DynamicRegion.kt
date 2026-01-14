/**
 * DynamicRegion.kt - Data model for dynamic content regions
 *
 * Part of LearnApp Safety System.
 * Represents regions of the screen that contain changing content,
 * helping the exploration engine avoid infinite loops.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5.2
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.safety

import android.graphics.Rect

/**
 * Type of dynamic content change detected.
 *
 * Used to categorize how content changes in a region.
 * IPC Format: DYN:screen_hash:region_id:change_type
 */
enum class DynamicChangeType(val ipcCode: String, val description: String) {
    /**
     * Feed-like content that keeps loading (endless scroll).
     * Examples: Instagram feed, Twitter timeline, News apps
     */
    INFINITE_SCROLL("INF", "Infinite scroll feed"),

    /**
     * Same structure but different content (pull to refresh).
     * Examples: Email inbox, notification list
     */
    CONTENT_REFRESH("REF", "Same structure, refreshing content"),

    /**
     * Layout structure changes (ads, promoted content injected).
     * Examples: App with ad placements, promoted posts
     */
    LAYOUT_CHANGE("LAY", "Structure changes dynamically"),

    /**
     * Time-based updates (clocks, timers, live data).
     * Examples: Stock prices, sports scores, live counters
     */
    TIME_BASED("TIM", "Time-based updates"),

    /**
     * Animation or carousel content.
     * Examples: Image carousels, promotional banners
     */
    ANIMATED("ANI", "Animated or carousel content"),

    /**
     * User presence indicators (typing, online status).
     * Examples: Chat apps showing typing indicators
     */
    PRESENCE("PRS", "Presence or status indicators");

    companion object {
        /**
         * Parse from IPC code string.
         */
        fun fromIpcCode(code: String): DynamicChangeType? {
            return entries.find { it.ipcCode.equals(code, ignoreCase = true) }
        }
    }
}

/**
 * Represents a region of the screen containing dynamic content.
 *
 * Tracked to avoid re-scanning content that changes frequently.
 * Once marked as dynamic, the exploration engine will:
 * 1. Capture elements once
 * 2. Skip re-scanning on subsequent visits
 * 3. Log to AVU for command generation awareness
 *
 * @property screenHash Hash of the screen containing this region
 * @property regionId Unique identifier for the region (resourceId or generated)
 * @property bounds Screen bounds of the dynamic region
 * @property changeType How the content changes
 * @property changeCount Number of times content changed (threshold = 3)
 * @property firstSeenTimestamp When region was first detected
 * @property lastChangeTimestamp When content last changed
 * @property parentClassName Android class name of the container
 * @property sampleElementCount Number of elements seen in this region
 */
data class DynamicRegion(
    val screenHash: String,
    val regionId: String,
    val bounds: Rect,
    val changeType: DynamicChangeType,
    val changeCount: Int = 0,
    val firstSeenTimestamp: Long = System.currentTimeMillis(),
    val lastChangeTimestamp: Long = System.currentTimeMillis(),
    val parentClassName: String = "",
    val sampleElementCount: Int = 0
) {
    /**
     * Whether region has been confirmed as dynamic.
     * Requires 3+ content changes to confirm.
     */
    val isConfirmedDynamic: Boolean
        get() = changeCount >= CHANGE_THRESHOLD

    /**
     * Generate DYN IPC line for AVU export.
     *
     * Format: DYN:screen_hash:region_id:change_type
     */
    fun toDynLine(): String {
        return "DYN:$screenHash:$regionId:${changeType.name}"
    }

    /**
     * Create updated region with incremented change count.
     */
    fun withChange(): DynamicRegion {
        return copy(
            changeCount = changeCount + 1,
            lastChangeTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Check if region overlaps with given bounds.
     */
    fun overlaps(other: Rect): Boolean {
        return Rect.intersects(bounds, other)
    }

    companion object {
        /**
         * Minimum changes required to confirm dynamic content.
         */
        const val CHANGE_THRESHOLD = 3

        /**
         * Create region from container node info.
         */
        fun fromContainer(
            screenHash: String,
            resourceId: String,
            bounds: Rect,
            className: String,
            suggestedType: DynamicChangeType = DynamicChangeType.CONTENT_REFRESH
        ): DynamicRegion {
            val regionId = if (resourceId.isNotEmpty()) {
                resourceId.substringAfterLast("/").take(30)
            } else {
                "region_${bounds.centerX()}_${bounds.centerY()}"
            }

            return DynamicRegion(
                screenHash = screenHash,
                regionId = regionId,
                bounds = bounds,
                changeType = suggestedType,
                parentClassName = className
            )
        }

        /**
         * Parse from AVU DYN line.
         *
         * Input: "DYN:abc123:feed_region:INFINITE_SCROLL"
         */
        fun fromAvuLine(line: String): DynamicRegion? {
            if (!line.startsWith("DYN:")) return null
            val parts = line.split(":")
            if (parts.size < 4) return null

            val changeType = try {
                DynamicChangeType.valueOf(parts[3])
            } catch (e: IllegalArgumentException) {
                DynamicChangeType.CONTENT_REFRESH
            }

            return DynamicRegion(
                screenHash = parts[1],
                regionId = parts[2],
                bounds = Rect(), // Bounds not stored in AVU format
                changeType = changeType,
                changeCount = CHANGE_THRESHOLD // Already confirmed if in AVU
            )
        }
    }
}
