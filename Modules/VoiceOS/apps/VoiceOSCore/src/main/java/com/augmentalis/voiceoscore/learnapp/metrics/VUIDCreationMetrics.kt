/**
 * VUIDCreationMetrics.kt - VUID creation monitoring and metrics
 * Path: VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/metrics/VUIDCreationMetrics.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-08
 * Feature: LearnApp VUID Creation Fix - Phase 3 (Observability)
 *
 * Purpose:
 * - Track VUID creation success rate per exploration
 * - Monitor element filtering with detailed reasons
 * - Generate diagnostic reports for debugging
 * - Store metrics in database for historical analysis
 *
 * Part of: LearnApp-VUID-Creation-Fix-Spec-5081218-V1.md (Phase 3)
 */

package com.augmentalis.voiceoscore.learnapp.metrics

import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CopyOnWriteArrayList

/**
 * VUID creation metrics for a single exploration session
 *
 * Tracks element detection, VUID creation, and filtering statistics
 * to diagnose and monitor VUID creation success rate.
 *
 * @property packageName Target app package name
 * @property explorationTimestamp When exploration started (Unix milliseconds)
 * @property elementsDetected Total number of elements found during scraping
 * @property vuidsCreated Number of VUIDs successfully created
 * @property creationRate Success rate (vuidsCreated / elementsDetected)
 * @property filteredCount Number of elements filtered out (not assigned VUID)
 * @property filteredByType Element counts grouped by className
 * @property filterReasons Filter counts grouped by reason
 */
data class VUIDCreationMetrics(
    val packageName: String,
    val explorationTimestamp: Long,
    val elementsDetected: Int,
    val vuidsCreated: Int,
    val creationRate: Double,
    val filteredCount: Int,
    val filteredByType: Map<String, Int>,
    val filterReasons: Map<String, Int>
) {
    /**
     * Generate human-readable report string
     *
     * Used for logging and file export.
     */
    fun toReportString(): String = buildString {
        appendLine("VUID Creation Report - $packageName")
        appendLine("=".repeat(50))
        appendLine("Exploration Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(explorationTimestamp))}")
        appendLine("Elements detected: $elementsDetected")
        appendLine("VUIDs created: $vuidsCreated")
        appendLine("Creation rate: ${formatPercentage(creationRate)} ${getStatusIcon(creationRate)}")
        appendLine("Filtered: $filteredCount")
        appendLine()

        if (filteredByType.isNotEmpty()) {
            appendLine("Filtered By Type:")
            filteredByType.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                val percentage = if (elementsDetected > 0) count.toDouble() / elementsDetected else 0.0
                appendLine("  ${shortenClassName(type)}: $count (${formatPercentage(percentage)})")
            }
            appendLine()
        }

        if (filterReasons.isNotEmpty()) {
            appendLine("Filter Reasons:")
            filterReasons.entries.sortedByDescending { it.value }.forEach { (reason, count) ->
                appendLine("  $reason: $count")
            }
        }
    }

    /**
     * Format percentage for display
     */
    private fun formatPercentage(rate: Double): String {
        return "${(rate * 100).toInt()}%"
    }

    /**
     * Get status icon based on creation rate
     */
    private fun getStatusIcon(rate: Double): String = when {
        rate >= 0.95 -> "✅" // Excellent (95%+)
        rate >= 0.80 -> "⚠️"  // Warning (80-95%)
        else -> "❌"         // Error (<80%)
    }

    /**
     * Shorten class names for readability
     */
    private fun shortenClassName(className: String): String {
        return className.substringAfterLast('.').removeSuffix("Layout").removeSuffix("View")
    }
}

/**
 * Severity level for filtered elements
 *
 * Helps identify which filtered elements are problematic vs. intentional.
 */
enum class FilterSeverity {
    /**
     * Expected filtering (e.g., decorative elements)
     * No action needed - working as intended
     */
    INTENDED,

    /**
     * Suspicious filtering (e.g., container with click hints but filtered)
     * May indicate edge case that needs investigation
     */
    WARNING,

    /**
     * Wrong filtering (e.g., isClickable=true but filtered)
     * Indicates bug in filter logic - should NOT happen
     */
    ERROR
}

/**
 * Record of a single filtered element
 *
 * Captured during exploration to diagnose why elements don't get VUIDs.
 *
 * @property elementHash Unique hash of element (for deduplication)
 * @property name Element text or content description
 * @property className Android view class name
 * @property isClickable Android's isClickable flag value
 * @property filterReason Why element was filtered (e.g., "Below threshold", "Decorative")
 * @property severity Classification of filter intent (INTENDED, WARNING, ERROR)
 * @property timestamp When element was filtered (Unix milliseconds)
 */
data class FilteredElement(
    val elementHash: String,
    val name: String?,
    val className: String,
    val isClickable: Boolean,
    val filterReason: String,
    val severity: FilterSeverity,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Filter report containing all filtered elements
 *
 * Summarizes filtering activity for diagnostics.
 *
 * @property totalFiltered Total number of filtered elements
 * @property errorCount Number of ERROR severity filters (should be 0)
 * @property warningCount Number of WARNING severity filters
 * @property intendedCount Number of INTENDED severity filters
 * @property elements Full list of filtered elements
 */
data class FilterReport(
    val totalFiltered: Int,
    val errorCount: Int,
    val warningCount: Int,
    val intendedCount: Int,
    val elements: List<FilteredElement>
)

/**
 * Real-time metrics collector during exploration
 *
 * Thread-safe collector that tracks VUID creation metrics in real-time.
 * Integrates with ExplorationEngine to monitor element detection and filtering.
 *
 * ## Usage
 * ```kotlin
 * val collector = VUIDCreationMetricsCollector()
 *
 * // During exploration
 * elements.forEach { element ->
 *     collector.onElementDetected()
 *
 *     if (shouldCreateVUID(element)) {
 *         createVUID(element)
 *         collector.onVUIDCreated()
 *     } else {
 *         collector.onElementFiltered(element, "Below threshold")
 *     }
 * }
 *
 * // After exploration
 * val metrics = collector.buildMetrics("com.example.app")
 * Log.i(TAG, metrics.toReportString())
 * ```
 *
 * @since 2025-12-08 (Phase 3: Observability)
 */
class VUIDCreationMetricsCollector {

    /**
     * Thread-safe counters and collections
     */
    private var elementsDetected = 0
    private var vuidsCreated = 0
    private val filteredByType = mutableMapOf<String, Int>()
    private val filterReasons = mutableMapOf<String, Int>()
    private val filteredElements = CopyOnWriteArrayList<FilteredElement>()

    /**
     * Container types that may trigger warnings if filtered
     */
    private val containerTypes = setOf(
        "android.widget.LinearLayout",
        "android.widget.FrameLayout",
        "android.widget.RelativeLayout",
        "androidx.cardview.widget.CardView",
        "com.google.android.material.card.MaterialCardView"
    )

    /**
     * Called when an element is detected during scraping
     */
    @Synchronized
    fun onElementDetected() {
        elementsDetected++
    }

    /**
     * Called when a VUID is successfully created
     */
    @Synchronized
    fun onVUIDCreated() {
        vuidsCreated++
    }

    /**
     * Called when an element is filtered out (no VUID created)
     *
     * @param element Accessibility node that was filtered
     * @param reason Why element was filtered
     */
    @Synchronized
    fun onElementFiltered(element: AccessibilityNodeInfo, reason: String) {
        val className = element.className?.toString() ?: "Unknown"

        // Update type counts
        filteredByType[className] = filteredByType.getOrDefault(className, 0) + 1

        // Update reason counts
        filterReasons[reason] = filterReasons.getOrDefault(reason, 0) + 1

        // Determine severity
        val severity = determineSeverity(element, reason)

        // Record filtered element
        val filtered = FilteredElement(
            elementHash = element.hashCode().toString(),
            name = element.text?.toString() ?: element.contentDescription?.toString(),
            className = className,
            isClickable = element.isClickable,
            filterReason = reason,
            severity = severity
        )

        filteredElements.add(filtered)
    }

    /**
     * Called when an ElementInfo is filtered out (no VUID created)
     *
     * Overload for ElementInfo data class used in ExplorationEngine.
     *
     * @param element ElementInfo that was filtered
     * @param reason Why element was filtered
     */
    @Synchronized
    fun onElementFiltered(element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo, reason: String) {
        val className = element.className

        // Update type counts
        filteredByType[className] = filteredByType.getOrDefault(className, 0) + 1

        // Update reason counts
        filterReasons[reason] = filterReasons.getOrDefault(reason, 0) + 1

        // Determine severity for ElementInfo
        val severity = if (element.isClickable) {
            FilterSeverity.ERROR // Should never filter clickable elements
        } else if (element.className in containerTypes) {
            FilterSeverity.WARNING // Suspicious: container filtered
        } else {
            FilterSeverity.INTENDED // Expected filtering
        }

        // Record filtered element
        val filtered = FilteredElement(
            elementHash = element.hashCode().toString(),
            name = element.text.ifEmpty { element.contentDescription }.ifEmpty { null },
            className = className,
            isClickable = element.isClickable,
            filterReason = reason,
            severity = severity
        )

        filteredElements.add(filtered)
    }

    /**
     * Determine severity of filtering decision
     *
     * @param element Filtered element
     * @param reason Filter reason
     * @return Severity classification
     */
    private fun determineSeverity(
        element: AccessibilityNodeInfo,
        reason: String
    ): FilterSeverity {
        // ERROR: isClickable=true but filtered (should NEVER happen after Phase 1)
        if (element.isClickable) {
            return FilterSeverity.ERROR
        }

        // WARNING: Container with clickability hints
        val className = element.className?.toString() ?: ""
        val isContainer = className in containerTypes
        if (isContainer && (element.isFocusable || hasClickAction(element))) {
            return FilterSeverity.WARNING
        }

        // INTENDED: Expected filtering (decorative elements, low scores, etc.)
        return FilterSeverity.INTENDED
    }

    /**
     * Check if element has click action in action list
     */
    private fun hasClickAction(element: AccessibilityNodeInfo): Boolean {
        return element.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_CLICK
        }
    }

    /**
     * Build final metrics object
     *
     * @param packageName Target app package
     * @return Immutable metrics snapshot
     */
    @Synchronized
    fun buildMetrics(packageName: String): VUIDCreationMetrics {
        val creationRate = if (elementsDetected > 0) {
            vuidsCreated.toDouble() / elementsDetected
        } else {
            0.0
        }

        return VUIDCreationMetrics(
            packageName = packageName,
            explorationTimestamp = System.currentTimeMillis(),
            elementsDetected = elementsDetected,
            vuidsCreated = vuidsCreated,
            creationRate = creationRate,
            filteredCount = filteredByType.values.sum(),
            filteredByType = filteredByType.toMap(),
            filterReasons = filterReasons.toMap()
        )
    }

    /**
     * Generate filter report with element details
     *
     * @return Report containing all filtered elements
     */
    @Synchronized
    fun generateFilterReport(): FilterReport {
        val grouped = filteredElements.groupBy { it.severity }

        return FilterReport(
            totalFiltered = filteredElements.size,
            errorCount = grouped[FilterSeverity.ERROR]?.size ?: 0,
            warningCount = grouped[FilterSeverity.WARNING]?.size ?: 0,
            intendedCount = grouped[FilterSeverity.INTENDED]?.size ?: 0,
            elements = filteredElements.toList()
        )
    }

    /**
     * Reset all metrics (for new exploration)
     */
    @Synchronized
    fun reset() {
        elementsDetected = 0
        vuidsCreated = 0
        filteredByType.clear()
        filterReasons.clear()
        filteredElements.clear()
    }

    /**
     * Get current stats (for real-time display)
     */
    @Synchronized
    fun getCurrentStats(): Triple<Int, Int, Double> {
        val rate = if (elementsDetected > 0) {
            vuidsCreated.toDouble() / elementsDetected
        } else {
            0.0
        }
        return Triple(elementsDetected, vuidsCreated, rate)
    }
}
