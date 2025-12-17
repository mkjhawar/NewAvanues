/**
 * ComparisonMetrics.kt - Metrics collection and analysis for comparisons
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 *
 * Note: DivergenceSeverity, DivergenceCategory, DivergenceDetail, and ComparisonResult
 * are defined in DivergenceReport.kt
 */
package com.augmentalis.voiceoscore.testing

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Real-time metrics for comparison framework
 */
data class ComparisonMetricsSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val totalComparisons: Long = 0,
    val totalDivergences: Long = 0,
    val divergenceRate: Float = 0f,

    // Timing metrics
    val avgComparisonOverheadMs: Double = 0.0,
    val maxComparisonOverheadMs: Long = 0,
    val minComparisonOverheadMs: Long = Long.MAX_VALUE,

    // Breakdown by severity
    val criticalCount: Long = 0,
    val highCount: Long = 0,
    val mediumCount: Long = 0,
    val lowCount: Long = 0,

    // Breakdown by category
    val returnValueDivergences: Long = 0,
    val stateDivergences: Long = 0,
    val sideEffectDivergences: Long = 0,
    val timingDivergences: Long = 0,
    val exceptionDivergences: Long = 0,
    val asyncDivergences: Long = 0,

    // Method-level metrics
    val methodMetrics: Map<String, MethodMetrics> = emptyMap(),

    // Performance impact
    val comparisonImpactPercent: Float = 0f  // % overhead compared to execution time
) {
    val divergencePercentage: Float
        get() = if (totalComparisons > 0) {
            (totalDivergences.toFloat() / totalComparisons.toFloat()) * 100f
        } else 0f
}

/**
 * Per-method comparison metrics
 */
data class MethodMetrics(
    val methodName: String,
    val invocationCount: Long = 0,
    val divergenceCount: Long = 0,
    val avgLegacyTimeMs: Double = 0.0,
    val avgRefactoredTimeMs: Double = 0.0,
    val avgComparisonTimeMs: Double = 0.0,
    val lastDivergence: Long? = null,
    val divergenceRate: Float = 0f
) {
    val divergencePercentage: Float
        get() = if (invocationCount > 0) {
            (divergenceCount.toFloat() / invocationCount.toFloat()) * 100f
        } else 0f

    val performanceDelta: Float
        get() = if (avgLegacyTimeMs > 0) {
            ((avgRefactoredTimeMs - avgLegacyTimeMs) / avgLegacyTimeMs * 100).toFloat()
        } else 0f
}

/**
 * Collects and analyzes comparison metrics
 *
 * Features:
 * - Real-time metric updates
 * - Low-overhead tracking (<1ms per comparison)
 * - Method-level breakdown
 * - Performance impact monitoring
 */
class ComparisonMetricsCollector {
    private val totalComparisons = AtomicLong(0)
    private val totalDivergences = AtomicLong(0)

    private val severityCounts = ConcurrentHashMap<DivergenceSeverity, AtomicLong>().apply {
        DivergenceSeverity.values().forEach { put(it, AtomicLong(0)) }
    }

    private val categoryCounts = ConcurrentHashMap<DivergenceCategory, AtomicLong>().apply {
        DivergenceCategory.values().forEach { put(it, AtomicLong(0)) }
    }

    private val methodInvocations = ConcurrentHashMap<String, AtomicLong>()
    private val methodDivergences = ConcurrentHashMap<String, AtomicLong>()
    private val methodLegacyTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val methodRefactoredTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val methodComparisonTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val methodLastDivergence = ConcurrentHashMap<String, Long>()

    private val comparisonOverheads = mutableListOf<Long>()
    private val comparisonOverheadsLock = Any()

    private val _metricsFlow = MutableStateFlow(ComparisonMetricsSnapshot())
    val metricsFlow: StateFlow<ComparisonMetricsSnapshot> = _metricsFlow.asStateFlow()

    /**
     * Record a comparison result
     */
    fun recordComparison(
        result: ComparisonResult,
        comparisonOverheadMs: Long
    ) {
        val start = System.currentTimeMillis()

        totalComparisons.incrementAndGet()

        // Track comparison overhead
        synchronized(comparisonOverheadsLock) {
            comparisonOverheads.add(comparisonOverheadMs)
            if (comparisonOverheads.size > MAX_OVERHEAD_SAMPLES) {
                comparisonOverheads.removeAt(0)
            }
        }

        // Track method-level metrics
        methodInvocations.getOrPut(result.methodName) { AtomicLong(0) }.incrementAndGet()
        methodLegacyTimes.getOrPut(result.methodName) { mutableListOf() }
            .add(result.legacyExecutionTimeMs)
        methodRefactoredTimes.getOrPut(result.methodName) { mutableListOf() }
            .add(result.refactoredExecutionTimeMs)
        methodComparisonTimes.getOrPut(result.methodName) { mutableListOf() }
            .add(comparisonOverheadMs)

        // Track divergences
        if (result.hasDivergence) {
            totalDivergences.incrementAndGet()
            methodDivergences.getOrPut(result.methodName) { AtomicLong(0) }.incrementAndGet()
            methodLastDivergence[result.methodName] = result.timestamp

            // Update severity and category counts
            result.divergences.forEach { divergence ->
                severityCounts[divergence.severity]?.incrementAndGet()
                categoryCounts[divergence.category]?.incrementAndGet()
            }
        }

        // Update metrics snapshot periodically
        if (totalComparisons.get() % UPDATE_INTERVAL == 0L) {
            updateMetricsSnapshot()
        }

        val recordTime = System.currentTimeMillis() - start
        if (recordTime > 5) {
            Log.w(TAG, "Metric recording took ${recordTime}ms (should be <1ms)")
        }
    }

    /**
     * Update the metrics snapshot
     */
    private fun updateMetricsSnapshot() {
        val snapshot = computeSnapshot()
        _metricsFlow.value = snapshot
    }

    /**
     * Compute current metrics snapshot
     */
    fun computeSnapshot(): ComparisonMetricsSnapshot {
        val totalComps = totalComparisons.get()
        val totalDivs = totalDivergences.get()

        val overheadStats = synchronized(comparisonOverheadsLock) {
            if (comparisonOverheads.isEmpty()) {
                Triple(0.0, 0L, Long.MAX_VALUE)
            } else {
                Triple(
                    comparisonOverheads.average(),
                    comparisonOverheads.maxOrNull() ?: 0L,
                    comparisonOverheads.minOrNull() ?: Long.MAX_VALUE
                )
            }
        }

        // Compute method-level metrics
        val methodMetrics = methodInvocations.keys.associateWith { methodName ->
            val invocations = methodInvocations[methodName]?.get() ?: 0L
            val divergences = methodDivergences[methodName]?.get() ?: 0L
            val legacyTimes = methodLegacyTimes[methodName] ?: emptyList()
            val refactoredTimes = methodRefactoredTimes[methodName] ?: emptyList()
            val comparisonTimes = methodComparisonTimes[methodName] ?: emptyList()

            MethodMetrics(
                methodName = methodName,
                invocationCount = invocations,
                divergenceCount = divergences,
                avgLegacyTimeMs = if (legacyTimes.isNotEmpty()) legacyTimes.average() else 0.0,
                avgRefactoredTimeMs = if (refactoredTimes.isNotEmpty()) refactoredTimes.average() else 0.0,
                avgComparisonTimeMs = if (comparisonTimes.isNotEmpty()) comparisonTimes.average() else 0.0,
                lastDivergence = methodLastDivergence[methodName]
            )
        }

        // Calculate comparison impact
        val totalExecutionTime = methodMetrics.values.sumOf {
            it.avgLegacyTimeMs * it.invocationCount
        }
        val totalComparisonTime = methodMetrics.values.sumOf {
            it.avgComparisonTimeMs * it.invocationCount
        }
        val comparisonImpact = if (totalExecutionTime > 0) {
            (totalComparisonTime / totalExecutionTime * 100).toFloat()
        } else 0f

        return ComparisonMetricsSnapshot(
            timestamp = System.currentTimeMillis(),
            totalComparisons = totalComps,
            totalDivergences = totalDivs,
            avgComparisonOverheadMs = overheadStats.first,
            maxComparisonOverheadMs = overheadStats.second,
            minComparisonOverheadMs = if (overheadStats.third == Long.MAX_VALUE) 0L else overheadStats.third,
            criticalCount = severityCounts[DivergenceSeverity.CRITICAL]?.get() ?: 0,
            highCount = severityCounts[DivergenceSeverity.HIGH]?.get() ?: 0,
            mediumCount = severityCounts[DivergenceSeverity.MEDIUM]?.get() ?: 0,
            lowCount = severityCounts[DivergenceSeverity.LOW]?.get() ?: 0,
            returnValueDivergences = categoryCounts[DivergenceCategory.RETURN_VALUE]?.get() ?: 0,
            stateDivergences = categoryCounts[DivergenceCategory.STATE]?.get() ?: 0,
            sideEffectDivergences = categoryCounts[DivergenceCategory.SIDE_EFFECT]?.get() ?: 0,
            timingDivergences = categoryCounts[DivergenceCategory.TIMING]?.get() ?: 0,
            exceptionDivergences = categoryCounts[DivergenceCategory.EXCEPTION]?.get() ?: 0,
            asyncDivergences = categoryCounts[DivergenceCategory.ASYNC_RESULT]?.get() ?: 0,
            methodMetrics = methodMetrics,
            comparisonImpactPercent = comparisonImpact
        )
    }

    /**
     * Get current metrics
     */
    fun getCurrentMetrics(): ComparisonMetricsSnapshot {
        return _metricsFlow.value
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        totalComparisons.set(0)
        totalDivergences.set(0)
        severityCounts.values.forEach { it.set(0) }
        categoryCounts.values.forEach { it.set(0) }
        methodInvocations.clear()
        methodDivergences.clear()
        methodLegacyTimes.clear()
        methodRefactoredTimes.clear()
        methodComparisonTimes.clear()
        methodLastDivergence.clear()
        synchronized(comparisonOverheadsLock) {
            comparisonOverheads.clear()
        }
        updateMetricsSnapshot()
    }

    /**
     * Generate a detailed metrics report
     */
    fun generateReport(): String {
        val snapshot = computeSnapshot()

        return buildString {
            appendLine("=== COMPARISON FRAMEWORK METRICS ===")
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(snapshot.timestamp)}")
            appendLine()

            appendLine("## Overall Statistics")
            appendLine("Total Comparisons: ${snapshot.totalComparisons}")
            appendLine("Total Divergences: ${snapshot.totalDivergences} (${snapshot.divergencePercentage}%)")
            appendLine()

            appendLine("## Performance")
            appendLine("Avg Comparison Overhead: ${snapshot.avgComparisonOverheadMs}ms")
            appendLine("Min Overhead: ${snapshot.minComparisonOverheadMs}ms")
            appendLine("Max Overhead: ${snapshot.maxComparisonOverheadMs}ms")
            appendLine("Comparison Impact: ${snapshot.comparisonImpactPercent}%")
            appendLine()

            appendLine("## Divergences by Severity")
            appendLine("CRITICAL: ${snapshot.criticalCount}")
            appendLine("HIGH:     ${snapshot.highCount}")
            appendLine("MEDIUM:   ${snapshot.mediumCount}")
            appendLine("LOW:      ${snapshot.lowCount}")
            appendLine()

            appendLine("## Divergences by Category")
            appendLine("Return Value: ${snapshot.returnValueDivergences}")
            appendLine("State:        ${snapshot.stateDivergences}")
            appendLine("Side Effect:  ${snapshot.sideEffectDivergences}")
            appendLine("Timing:       ${snapshot.timingDivergences}")
            appendLine("Exception:    ${snapshot.exceptionDivergences}")
            appendLine("Async:        ${snapshot.asyncDivergences}")
            appendLine()

            if (snapshot.methodMetrics.isNotEmpty()) {
                appendLine("## Method-Level Metrics")
                snapshot.methodMetrics.values
                    .sortedByDescending { it.divergenceCount }
                    .take(10)
                    .forEach { method ->
                        appendLine()
                        appendLine("Method: ${method.methodName}")
                        appendLine("  Invocations: ${method.invocationCount}")
                        appendLine("  Divergences: ${method.divergenceCount} (${method.divergencePercentage}%)")
                        appendLine("  Avg Legacy Time: ${method.avgLegacyTimeMs}ms")
                        appendLine("  Avg Refactored Time: ${method.avgRefactoredTimeMs}ms")
                        appendLine("  Performance Delta: ${method.performanceDelta}%")
                        appendLine("  Avg Comparison Time: ${method.avgComparisonTimeMs}ms")
                    }
            }
        }
    }

    companion object {
        private const val TAG = "ComparisonMetrics"
        private const val UPDATE_INTERVAL = 10L  // Update snapshot every 10 comparisons
        private const val MAX_OVERHEAD_SAMPLES = 1000  // Keep last 1000 samples
    }
}
