/**
 * AvidCreationMetricsCollector.kt - Collects Avid creation metrics
 *
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.metrics

import com.augmentalis.voiceoscore.learnapp.models.ElementInfo

/**
 * Avid Creation Metrics Collector
 *
 * Collects metrics during Avid creation process
 */
class AvidCreationMetricsCollector {

    private var vuidsCreated: Int = 0
    private var elementsDetected: Int = 0
    private var filteredCount: Int = 0
    private var errorCount: Int = 0
    private var explorationTimestamp: Long = System.currentTimeMillis()

    /**
     * Record metrics for element processing
     */
    fun recordElementProcessed() {
        elementsDetected++
    }

    /**
     * Record Avid creation time
     */
    fun recordCreationTime(timeMs: Long) {
        // Stub implementation
    }

    /**
     * Get collected metrics
     */
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "vuidsCreated" to vuidsCreated,
            "elementsDetected" to elementsDetected,
            "filteredCount" to filteredCount,
            "errorCount" to errorCount
        )
    }

    /**
     * Build AvidCreationMetrics object
     *
     * @param packageName Package name of the app being explored
     * @return AvidCreationMetrics object
     */
    fun buildMetrics(packageName: String): AvidCreationMetrics {
        return AvidCreationMetrics(
            vuidsCreated = vuidsCreated,
            elementsDetected = elementsDetected,
            filteredCount = filteredCount,
            errorCount = errorCount,
            explorationTimestamp = explorationTimestamp
        )
    }

    /**
     * Reset metrics
     */
    fun reset() {
        vuidsCreated = 0
        elementsDetected = 0
        filteredCount = 0
        errorCount = 0
        explorationTimestamp = System.currentTimeMillis()
    }

    /**
     * Callback when element is detected
     */
    fun onElementDetected() {
        elementsDetected++
    }

    /**
     * Callback when Avid is created
     */
    fun onAvidCreated() {
        vuidsCreated++
    }

    /**
     * Callback when element is filtered
     */
    fun onElementFiltered(element: ElementInfo, reason: String) {
        filteredCount++
    }
}
