/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * DynamicContentDetector.kt - Detects dynamic content to avoid infinite loops
 *
 * Part of VoiceOSCoreNG Safety System.
 * Identifies regions of the screen that contain changing content,
 * such as infinite scrolling feeds, timelines, and notification lists.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Migrated to KMP: 2026-01-16
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5.2
 *
 * ## Detection Strategy:
 * 1. Track content fingerprints per region
 * 2. If content changes 3+ times, mark as dynamic
 * 3. Once marked, skip re-scanning that region
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo

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
 * @property parentClassName Class name of the container
 * @property sampleElementCount Number of elements seen in this region
 */
data class DynamicRegion(
    val screenHash: String,
    val regionId: String,
    val bounds: Bounds,
    val changeType: DynamicChangeType,
    val changeCount: Int = 0,
    val firstSeenTimestamp: Long = currentTimeMillis(),
    val lastChangeTimestamp: Long = currentTimeMillis(),
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
            lastChangeTimestamp = currentTimeMillis()
        )
    }

    /**
     * Check if region overlaps with given bounds.
     */
    fun overlaps(other: Bounds): Boolean {
        return boundsIntersect(bounds, other)
    }

    companion object {
        /**
         * Minimum changes required to confirm dynamic content.
         */
        const val CHANGE_THRESHOLD = 3

        /**
         * Create region from container element info.
         */
        fun fromContainer(
            screenHash: String,
            resourceId: String,
            bounds: Bounds,
            className: String,
            suggestedType: DynamicChangeType = DynamicChangeType.CONTENT_REFRESH
        ): DynamicRegion {
            val regionId = if (resourceId.isNotEmpty()) {
                resourceId.substringAfterLast("/").take(30)
            } else {
                "region_${bounds.centerX}_${bounds.centerY}"
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
                bounds = Bounds.EMPTY, // Bounds not stored in AVU format
                changeType = changeType,
                changeCount = CHANGE_THRESHOLD // Already confirmed if in AVU
            )
        }
    }
}

/**
 * Dynamic Content Detector - Identifies changing content regions
 *
 * Stateful object that tracks content fingerprints across screen visits.
 * Uses a combination of:
 * - Class name indicators (RecyclerView, ViewPager)
 * - Resource ID patterns (feed, timeline, list)
 * - Content fingerprint comparison (element hash changes)
 */
object DynamicContentDetector {

    // ============================================================
    // Class name indicators of potentially dynamic content
    // ============================================================
    val DYNAMIC_CLASS_INDICATORS = listOf(
        // Scrolling lists (most common source of dynamic content)
        "RecyclerView",
        "ListView",
        "NestedScrollView",

        // Paging content
        "ViewPager",
        "ViewPager2",
        "HorizontalScrollView",

        // Tab content
        "TabLayout",
        "BottomNavigationView",

        // Card containers
        "CardView",
        "ConstraintLayout",  // Often used for feed items

        // Compose equivalents
        "LazyColumn",
        "LazyRow",
        "Pager"
    )

    // ============================================================
    // Resource ID patterns indicating dynamic content
    // ============================================================
    val DYNAMIC_RESOURCE_PATTERNS = listOf(
        // Feed-related
        Regex(".*feed.*", RegexOption.IGNORE_CASE),
        Regex(".*timeline.*", RegexOption.IGNORE_CASE),
        Regex(".*stream.*", RegexOption.IGNORE_CASE),

        // List-related
        Regex(".*list.*", RegexOption.IGNORE_CASE),
        Regex(".*recycler.*", RegexOption.IGNORE_CASE),
        Regex(".*scroll.*", RegexOption.IGNORE_CASE),

        // Notification/inbox
        Regex(".*notification.*", RegexOption.IGNORE_CASE),
        Regex(".*inbox.*", RegexOption.IGNORE_CASE),
        Regex(".*message.*list.*", RegexOption.IGNORE_CASE),

        // Story/carousel
        Regex(".*stories.*", RegexOption.IGNORE_CASE),
        Regex(".*carousel.*", RegexOption.IGNORE_CASE),
        Regex(".*banner.*", RegexOption.IGNORE_CASE)
    )

    // ============================================================
    // Tracking state: screenHash -> regionId -> fingerprints
    // ============================================================
    private val regionFingerprints = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
    private val confirmedDynamicRegions = mutableMapOf<String, MutableList<DynamicRegion>>()

    /**
     * Check if an element is inside a known dynamic region.
     *
     * @param screenHash Current screen hash
     * @param elementBounds Element's screen bounds
     * @return DynamicRegion if element is inside one, null otherwise
     */
    fun isInDynamicRegion(screenHash: String, elementBounds: Bounds): DynamicRegion? {
        return confirmedDynamicRegions[screenHash]?.find { region ->
            region.overlaps(elementBounds)
        }
    }

    /**
     * Detect if an element is a potential dynamic content container.
     *
     * Uses class name and resource ID pattern matching.
     *
     * @param element Element to check
     * @return Suggested DynamicChangeType if dynamic, null if not
     */
    fun detectPotentialDynamicContainer(element: ElementInfo): DynamicChangeType? {
        val className = element.className
        val resourceId = element.resourceId

        // Check class name indicators
        val isScrollableContainer = DYNAMIC_CLASS_INDICATORS.any { indicator ->
            className.contains(indicator, ignoreCase = true)
        }

        // Check resource ID patterns
        val matchesResourcePattern = DYNAMIC_RESOURCE_PATTERNS.any { pattern ->
            pattern.matches(resourceId)
        }

        // Determine change type based on indicators
        return when {
            // Infinite scroll indicators
            className.contains("RecyclerView", ignoreCase = true) ||
            className.contains("ListView", ignoreCase = true) ||
            className.contains("LazyColumn", ignoreCase = true) -> {
                if (resourceId.contains("feed", ignoreCase = true) ||
                    resourceId.contains("timeline", ignoreCase = true)) {
                    DynamicChangeType.INFINITE_SCROLL
                } else {
                    DynamicChangeType.CONTENT_REFRESH
                }
            }

            // Carousel/pager indicators
            className.contains("ViewPager", ignoreCase = true) ||
            className.contains("Carousel", ignoreCase = true) -> {
                DynamicChangeType.ANIMATED
            }

            // Story indicators
            resourceId.contains("stories", ignoreCase = true) ||
            resourceId.contains("banner", ignoreCase = true) -> {
                DynamicChangeType.ANIMATED
            }

            // Notification/inbox
            resourceId.contains("notification", ignoreCase = true) ||
            resourceId.contains("inbox", ignoreCase = true) -> {
                DynamicChangeType.CONTENT_REFRESH
            }

            // Generic scrollable with matching resource
            isScrollableContainer && matchesResourcePattern -> {
                DynamicChangeType.CONTENT_REFRESH
            }

            // Element itself is scrollable
            element.isScrollable -> {
                DynamicChangeType.CONTENT_REFRESH
            }

            else -> null
        }
    }

    /**
     * Track content fingerprint for a region.
     *
     * Call this each time you scrape elements from a potential dynamic region.
     * If fingerprint changes 3+ times, region is marked as dynamic.
     *
     * @param screenHash Current screen hash
     * @param regionId Region identifier (resourceId or generated)
     * @param contentFingerprint Hash of current content (e.g., child element hashes)
     * @param bounds Region bounds
     * @param className Container class name
     * @param suggestedType Suggested change type from detection
     * @return Updated DynamicRegion if confirmed, null if not yet confirmed
     */
    fun trackContentChange(
        screenHash: String,
        regionId: String,
        contentFingerprint: String,
        bounds: Bounds,
        className: String,
        suggestedType: DynamicChangeType
    ): DynamicRegion? {
        // Get or create fingerprint history for this screen
        val screenRegions = regionFingerprints.getOrPut(screenHash) { mutableMapOf() }
        val fingerprints = screenRegions.getOrPut(regionId) { mutableListOf() }

        // Check if content changed
        val lastFingerprint = fingerprints.lastOrNull()
        if (lastFingerprint != null && lastFingerprint != contentFingerprint) {
            // Content changed - add new fingerprint
            fingerprints.add(contentFingerprint)

            // Check if threshold reached
            if (fingerprints.size >= DynamicRegion.CHANGE_THRESHOLD) {
                // Create and store confirmed dynamic region
                val region = DynamicRegion.fromContainer(
                    screenHash = screenHash,
                    resourceId = regionId,
                    bounds = bounds,
                    className = className,
                    suggestedType = suggestedType
                ).copy(changeCount = fingerprints.size)

                val screenDynamicRegions = confirmedDynamicRegions.getOrPut(screenHash) { mutableListOf() }
                // Update existing or add new
                val existingIndex = screenDynamicRegions.indexOfFirst { it.regionId == regionId }
                if (existingIndex >= 0) {
                    screenDynamicRegions[existingIndex] = region
                } else {
                    screenDynamicRegions.add(region)
                }

                return region
            }
        } else if (lastFingerprint == null) {
            // First observation
            fingerprints.add(contentFingerprint)
        }

        return null
    }

    /**
     * Generate content fingerprint from child elements.
     *
     * Creates a hash based on element stable IDs to detect content changes.
     *
     * @param children Child elements of the container
     * @return Fingerprint string
     */
    fun generateContentFingerprint(children: List<ElementInfo>): String {
        if (children.isEmpty()) return "empty"

        // Use stable IDs of first N elements to create fingerprint
        val sampleSize = minOf(children.size, 10)
        val sample = children.take(sampleSize)

        return sample.joinToString("|") { it.stableId() }.hashCode().toString(16)
    }

    /**
     * Get all confirmed dynamic regions for a screen.
     *
     * @param screenHash Screen hash
     * @return List of confirmed dynamic regions
     */
    fun getDynamicRegions(screenHash: String): List<DynamicRegion> {
        return confirmedDynamicRegions[screenHash] ?: emptyList()
    }

    /**
     * Clear tracking data for a screen.
     *
     * Call when navigating away from a screen or resetting exploration.
     *
     * @param screenHash Screen hash to clear
     */
    fun clearScreen(screenHash: String) {
        regionFingerprints.remove(screenHash)
        // Keep confirmed regions in case we return to the screen
    }

    /**
     * Clear all tracking data.
     *
     * Call when starting a new exploration session.
     */
    fun reset() {
        regionFingerprints.clear()
        confirmedDynamicRegions.clear()
    }

    /**
     * Export all confirmed dynamic regions as AVU DYN lines.
     *
     * @return List of DYN IPC lines
     */
    fun exportDynLines(): List<String> {
        return confirmedDynamicRegions.values.flatten().map { it.toDynLine() }
    }

    /**
     * Import dynamic regions from AVU DYN lines.
     *
     * @param lines List of DYN IPC lines
     */
    fun importFromAvuLines(lines: List<String>) {
        for (line in lines) {
            val region = DynamicRegion.fromAvuLine(line) ?: continue
            val screenRegions = confirmedDynamicRegions.getOrPut(region.screenHash) { mutableListOf() }
            if (screenRegions.none { it.regionId == region.regionId }) {
                screenRegions.add(region)
            }
        }
    }
}

// ============================================================
// KMP-compatible utility functions
// ============================================================

// currentTimeMillis expect fun is defined in ISpeechEngine.kt

/**
 * Check if two bounds intersect.
 */
internal fun boundsIntersect(a: Bounds, b: Bounds): Boolean {
    return a.left < b.right && b.left < a.right &&
           a.top < b.bottom && b.top < a.bottom
}
