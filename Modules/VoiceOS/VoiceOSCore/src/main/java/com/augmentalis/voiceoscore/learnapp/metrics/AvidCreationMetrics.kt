/**
 * AvidCreationMetrics.kt - Metrics data class for Avid creation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Data class for tracking Avid creation statistics during exploration.
 */
package com.augmentalis.voiceoscore.learnapp.metrics

/**
 * Avid Creation Metrics
 *
 * Tracks statistics from Avid creation during app exploration.
 *
 * @property vuidsCreated Number of Avids successfully created
 * @property elementsDetected Total number of elements detected
 * @property filteredCount Number of elements filtered out
 * @property errorCount Number of errors during Avid creation
 * @property explorationTimestamp Timestamp of exploration session
 *
 * @since 1.0.0
 */
data class AvidCreationMetrics(
    val vuidsCreated: Int = 0,
    val elementsDetected: Int = 0,
    val filteredCount: Int = 0,
    val errorCount: Int = 0,
    val explorationTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get creation success rate
     *
     * @return Percentage of detected elements that resulted in Avid creation
     */
    fun getCreationRate(): Float {
        if (elementsDetected == 0) return 0f
        return (vuidsCreated.toFloat() / elementsDetected) * 100f
    }

    /**
     * Get filter rate
     *
     * @return Percentage of detected elements that were filtered out
     */
    fun getFilterRate(): Float {
        if (elementsDetected == 0) return 0f
        return (filteredCount.toFloat() / elementsDetected) * 100f
    }

    /**
     * Get error rate
     *
     * @return Percentage of detected elements that resulted in errors
     */
    fun getErrorRate(): Float {
        if (elementsDetected == 0) return 0f
        return (errorCount.toFloat() / elementsDetected) * 100f
    }

    /**
     * Check if metrics indicate healthy Avid creation
     *
     * Healthy if:
     * - Creation rate > 50%
     * - Error rate < 10%
     *
     * @return true if metrics are healthy
     */
    fun isHealthy(): Boolean {
        return getCreationRate() > 50f && getErrorRate() < 10f
    }

    /**
     * Get summary string for logging
     *
     * @return Human-readable summary
     */
    fun toSummaryString(): String {
        return buildString {
            append("Avids: $vuidsCreated/$elementsDetected")
            append(" (${String.format("%.1f", getCreationRate())}%)")
            append(", Filtered: $filteredCount")
            append(", Errors: $errorCount")
        }
    }

    /**
     * Get detailed report string for logging
     *
     * @return Detailed report with all metrics
     */
    fun toReportString(): String {
        return buildString {
            appendLine("Avid Creation Report:")
            appendLine("  Avids Created: $vuidsCreated")
            appendLine("  Elements Detected: $elementsDetected")
            appendLine("  Elements Filtered: $filteredCount")
            appendLine("  Errors: $errorCount")
            appendLine("  Creation Rate: ${String.format("%.2f", getCreationRate())}%")
            appendLine("  Filter Rate: ${String.format("%.2f", getFilterRate())}%")
            appendLine("  Error Rate: ${String.format("%.2f", getErrorRate())}%")
            appendLine("  Healthy: ${if (isHealthy()) "Yes" else "No"}")
        }
    }

    companion object {
        /**
         * Create empty metrics
         */
        fun empty(): AvidCreationMetrics {
            return AvidCreationMetrics()
        }

        /**
         * Combine multiple metrics
         *
         * @param metricsList List of metrics to combine
         * @return Combined metrics
         */
        fun combine(metricsList: List<AvidCreationMetrics>): AvidCreationMetrics {
            if (metricsList.isEmpty()) return empty()

            return AvidCreationMetrics(
                vuidsCreated = metricsList.sumOf { it.vuidsCreated },
                elementsDetected = metricsList.sumOf { it.elementsDetected },
                filteredCount = metricsList.sumOf { it.filteredCount },
                errorCount = metricsList.sumOf { it.errorCount },
                explorationTimestamp = metricsList.maxOf { it.explorationTimestamp }
            )
        }
    }
}

/**
 * Builder for AvidCreationMetrics
 */
class AvidCreationMetricsBuilder {
    private var vuidsCreated: Int = 0
    private var elementsDetected: Int = 0
    private var filteredCount: Int = 0
    private var errorCount: Int = 0
    private var explorationTimestamp: Long = System.currentTimeMillis()

    fun setVuidsCreated(count: Int) = apply { this.vuidsCreated = count }
    fun setElementsDetected(count: Int) = apply { this.elementsDetected = count }
    fun setFilteredCount(count: Int) = apply { this.filteredCount = count }
    fun setErrorCount(count: Int) = apply { this.errorCount = count }
    fun setExplorationTimestamp(timestamp: Long) = apply { this.explorationTimestamp = timestamp }

    fun incrementVuidsCreated(delta: Int = 1) = apply { this.vuidsCreated += delta }
    fun incrementElementsDetected(delta: Int = 1) = apply { this.elementsDetected += delta }
    fun incrementFilteredCount(delta: Int = 1) = apply { this.filteredCount += delta }
    fun incrementErrorCount(delta: Int = 1) = apply { this.errorCount += delta }

    fun build(): AvidCreationMetrics {
        return AvidCreationMetrics(
            vuidsCreated = vuidsCreated,
            elementsDetected = elementsDetected,
            filteredCount = filteredCount,
            errorCount = errorCount,
            explorationTimestamp = explorationTimestamp
        )
    }
}
