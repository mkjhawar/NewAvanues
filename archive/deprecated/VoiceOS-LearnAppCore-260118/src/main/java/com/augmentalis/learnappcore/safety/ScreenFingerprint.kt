/**
 * ScreenFingerprint.kt - Screen state tracking for change detection
 *
 * Part of LearnApp Safety System.
 * Creates fingerprints of screen content to:
 * 1. Detect when screen content changes
 * 2. Track exploration coverage
 * 3. Identify visited vs new screens
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.safety

import com.augmentalis.learnappcore.models.ElementInfo

/**
 * Screen fingerprint data class.
 *
 * @property screenHash Unique hash of the screen
 * @property activityName Activity class name
 * @property packageName Package name
 * @property elementCount Number of elements on screen
 * @property interactiveCount Number of interactive elements
 * @property structuralHash Hash of structural layout
 * @property contentHash Hash of text/content
 * @property timestamp When fingerprint was created
 */
data class ScreenFingerprint(
    val screenHash: String,
    val activityName: String,
    val packageName: String,
    val elementCount: Int,
    val interactiveCount: Int,
    val structuralHash: String,
    val contentHash: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if this fingerprint matches another (same screen).
     *
     * Two screens match if they have the same structural hash.
     * Content hash may differ (dynamic content).
     */
    fun matchesStructure(other: ScreenFingerprint): Boolean {
        return structuralHash == other.structuralHash
    }

    /**
     * Check if content has changed from another fingerprint.
     */
    fun hasContentChanged(other: ScreenFingerprint): Boolean {
        return contentHash != other.contentHash
    }

    /**
     * Calculate similarity percentage with another fingerprint.
     *
     * @return 0.0 to 1.0 similarity score
     */
    fun similarity(other: ScreenFingerprint): Float {
        var score = 0f

        // Same activity = 30%
        if (activityName == other.activityName) score += 0.3f

        // Similar element count = 20%
        val countDiff = kotlin.math.abs(elementCount - other.elementCount)
        val maxCount = maxOf(elementCount, other.elementCount)
        if (maxCount > 0) {
            score += 0.2f * (1f - countDiff.toFloat() / maxCount)
        }

        // Same structural hash = 40%
        if (structuralHash == other.structuralHash) score += 0.4f

        // Same content hash = 10%
        if (contentHash == other.contentHash) score += 0.1f

        return score
    }

    /**
     * Generate SCR IPC line for AVU export.
     *
     * Format: SCR:hash:activity:timestamp:element_count
     */
    fun toScrLine(): String {
        return "SCR:$screenHash:$activityName:$timestamp:$elementCount"
    }

    companion object {
        /**
         * Create fingerprint from screen elements.
         *
         * @param activityName Current activity
         * @param packageName Current package
         * @param elements All elements on screen
         * @return ScreenFingerprint
         */
        fun create(
            activityName: String,
            packageName: String,
            elements: List<ElementInfo>
        ): ScreenFingerprint {
            val interactiveElements = elements.filter { it.isClickable || it.isLongClickable || it.isEditable }

            // Create structural hash (based on element types and hierarchy)
            val structuralHash = createStructuralHash(elements)

            // Create content hash (based on text content)
            val contentHash = createContentHash(elements)

            // Create screen hash (combination)
            val screenHash = createScreenHash(activityName, structuralHash)

            return ScreenFingerprint(
                screenHash = screenHash,
                activityName = activityName,
                packageName = packageName,
                elementCount = elements.size,
                interactiveCount = interactiveElements.size,
                structuralHash = structuralHash,
                contentHash = contentHash
            )
        }

        /**
         * Create structural hash from element types and positions.
         *
         * Structural hash captures the layout without content.
         */
        private fun createStructuralHash(elements: List<ElementInfo>): String {
            // Use class names and approximate positions
            val structureSignature = elements
                .sortedBy { it.bounds.top * 10000 + it.bounds.left }
                .take(50) // Top 50 elements
                .joinToString("|") { element ->
                    val shortClass = element.className.substringAfterLast(".")
                    val quadrant = getQuadrant(element)
                    "$shortClass:$quadrant"
                }

            return structureSignature.hashCode().toString(16).padStart(8, '0')
        }

        /**
         * Create content hash from element text.
         *
         * Content hash changes when text changes.
         */
        private fun createContentHash(elements: List<ElementInfo>): String {
            val contentSignature = elements
                .filter { it.text.isNotBlank() || it.contentDescription.isNotBlank() }
                .sortedBy { it.bounds.top }
                .take(30) // Top 30 text elements
                .joinToString("|") { element ->
                    "${element.getDisplayName().take(20)}"
                }

            return contentSignature.hashCode().toString(16).padStart(8, '0')
        }

        /**
         * Create screen hash from activity and structure.
         */
        private fun createScreenHash(activityName: String, structuralHash: String): String {
            val combined = "$activityName:$structuralHash"
            return combined.hashCode().toString(16).padStart(8, '0')
        }

        /**
         * Get quadrant (1-4) for element based on position.
         */
        private fun getQuadrant(element: ElementInfo): Int {
            val centerX = element.bounds.centerX()
            val centerY = element.bounds.centerY()

            // Assuming 1080x1920 or similar
            // Divide into 4 quadrants
            val isRight = centerX > 540
            val isBottom = centerY > 960

            return when {
                !isRight && !isBottom -> 1 // Top-left
                isRight && !isBottom -> 2 // Top-right
                !isRight && isBottom -> 3 // Bottom-left
                else -> 4 // Bottom-right
            }
        }

        /**
         * Parse from AVU SCR line.
         *
         * Input: "SCR:abc123:CallsActivity:1733931600:15"
         */
        fun fromAvuLine(line: String): ScreenFingerprint? {
            if (!line.startsWith("SCR:")) return null
            val parts = line.split(":")
            if (parts.size < 5) return null

            return ScreenFingerprint(
                screenHash = parts[1],
                activityName = parts[2],
                packageName = "", // Not stored in SCR line
                elementCount = parts[4].toIntOrNull() ?: 0,
                interactiveCount = 0, // Not stored in SCR line
                structuralHash = parts[1], // Use screen hash as fallback
                contentHash = "", // Not stored in SCR line
                timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Screen state tracker for exploration.
 *
 * Maintains history of visited screens and their fingerprints.
 */
object ScreenTracker {

    // Screen hash -> list of fingerprints (for change tracking)
    private val screenHistory = mutableMapOf<String, MutableList<ScreenFingerprint>>()

    // All unique screens visited
    private val visitedScreens = mutableSetOf<String>()

    /**
     * Record a screen visit.
     *
     * @param fingerprint Screen fingerprint
     * @return true if this is a new screen, false if visited before
     */
    fun recordVisit(fingerprint: ScreenFingerprint): Boolean {
        val isNew = fingerprint.screenHash !in visitedScreens
        visitedScreens.add(fingerprint.screenHash)

        // Track history
        val history = screenHistory.getOrPut(fingerprint.screenHash) { mutableListOf() }
        history.add(fingerprint)

        // Keep only last 5 fingerprints per screen
        if (history.size > 5) {
            history.removeAt(0)
        }

        return isNew
    }

    /**
     * Check if screen has been visited.
     */
    fun hasVisited(screenHash: String): Boolean {
        return screenHash in visitedScreens
    }

    /**
     * Get visit count for a screen.
     */
    fun getVisitCount(screenHash: String): Int {
        return screenHistory[screenHash]?.size ?: 0
    }

    /**
     * Check if screen content has changed since last visit.
     */
    fun hasContentChanged(fingerprint: ScreenFingerprint): Boolean {
        val history = screenHistory[fingerprint.screenHash] ?: return true
        if (history.isEmpty()) return true

        val lastVisit = history.last()
        return fingerprint.hasContentChanged(lastVisit)
    }

    /**
     * Find similar screen by fingerprint.
     *
     * @param fingerprint Fingerprint to match
     * @param threshold Similarity threshold (0.0 to 1.0)
     * @return Most similar screen hash or null
     */
    fun findSimilarScreen(fingerprint: ScreenFingerprint, threshold: Float = 0.7f): String? {
        var bestMatch: String? = null
        var bestScore = 0f

        for ((screenHash, history) in screenHistory) {
            if (history.isEmpty()) continue
            val lastFingerprint = history.last()
            val similarity = fingerprint.similarity(lastFingerprint)

            if (similarity > threshold && similarity > bestScore) {
                bestScore = similarity
                bestMatch = screenHash
            }
        }

        return bestMatch
    }

    /**
     * Get all visited screen hashes.
     */
    fun getVisitedScreens(): Set<String> {
        return visitedScreens.toSet()
    }

    /**
     * Get exploration coverage percentage.
     *
     * Based on unique screens visited.
     *
     * @param estimatedTotal Estimated total screens in app
     * @return Coverage percentage (0.0 to 100.0)
     */
    fun getCoverage(estimatedTotal: Int): Float {
        if (estimatedTotal <= 0) return 0f
        return (visitedScreens.size.toFloat() / estimatedTotal) * 100f
    }

    /**
     * Export all screen fingerprints as SCR lines.
     */
    fun exportScrLines(): List<String> {
        return screenHistory.values.flatten()
            .distinctBy { it.screenHash }
            .map { it.toScrLine() }
    }

    /**
     * Reset all tracking data.
     */
    fun reset() {
        screenHistory.clear()
        visitedScreens.clear()
    }

    /**
     * Clear history for specific screen.
     */
    fun clearScreen(screenHash: String) {
        screenHistory.remove(screenHash)
        visitedScreens.remove(screenHash)
    }
}
