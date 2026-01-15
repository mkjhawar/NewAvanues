/**
 * AvidMetricsRepository.kt - Repository for Avid metrics tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Stores and retrieves Avid creation and usage metrics.
 */
package com.augmentalis.voiceoscore.learnapp.metrics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Avid Metrics Repository
 *
 * Repository for tracking Avid creation and usage metrics.
 */
class AvidMetricsRepository {

    private val _metricsFlow = MutableStateFlow(AvidMetrics())
    val metricsFlow: Flow<AvidMetrics> = _metricsFlow.asStateFlow()

    private val currentMetrics: AvidMetrics
        get() = _metricsFlow.value

    /**
     * Record Avid creation
     */
    fun recordCreation(packageName: String, elementType: String) {
        _metricsFlow.value = currentMetrics.copy(
            totalCreated = currentMetrics.totalCreated + 1,
            lastCreatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Record Avid lookup
     */
    fun recordLookup(packageName: String, found: Boolean) {
        val metrics = currentMetrics
        _metricsFlow.value = metrics.copy(
            totalLookups = metrics.totalLookups + 1,
            successfulLookups = metrics.successfulLookups + if (found) 1 else 0
        )
    }

    /**
     * Record elements detected
     */
    fun recordElementsDetected(count: Int) {
        _metricsFlow.value = currentMetrics.copy(
            totalElementsDetected = currentMetrics.totalElementsDetected + count
        )
    }

    /**
     * Record filtered elements
     */
    fun recordFilteredElements(count: Int, reason: String) {
        _metricsFlow.value = currentMetrics.copy(
            totalFiltered = currentMetrics.totalFiltered + count
        )
    }

    /**
     * Get current metrics snapshot
     *
     * Note: Use getMetricsSnapshot() to avoid JVM clash with currentMetrics property
     */
    fun getMetricsSnapshot(): AvidMetrics {
        return currentMetrics
    }

    /**
     * Reset metrics
     */
    fun reset() {
        _metricsFlow.value = AvidMetrics()
    }

    /**
     * Calculate creation rate
     */
    fun getCreationRate(): Float {
        val metrics = currentMetrics
        if (metrics.totalElementsDetected == 0L) return 0f
        return (metrics.totalCreated.toFloat() / metrics.totalElementsDetected) * 100f
    }

    /**
     * Calculate lookup success rate
     */
    fun getLookupSuccessRate(): Float {
        val metrics = currentMetrics
        if (metrics.totalLookups == 0L) return 0f
        return (metrics.successfulLookups.toFloat() / metrics.totalLookups) * 100f
    }

    /**
     * Save metrics from exploration session
     *
     * @param metrics AvidCreationMetrics from exploration
     */
    fun saveMetrics(metrics: AvidCreationMetrics) {
        _metricsFlow.value = AvidMetrics(
            totalCreated = metrics.vuidsCreated.toLong(),
            totalElementsDetected = metrics.elementsDetected.toLong(),
            totalFiltered = metrics.filteredCount.toLong(),
            lastCreatedAt = metrics.explorationTimestamp
        )
    }
}

/**
 * Avid Metrics
 */
data class AvidMetrics(
    val totalCreated: Long = 0,
    val totalLookups: Long = 0,
    val successfulLookups: Long = 0,
    val totalElementsDetected: Long = 0,
    val totalFiltered: Long = 0,
    val lastCreatedAt: Long = 0
)
