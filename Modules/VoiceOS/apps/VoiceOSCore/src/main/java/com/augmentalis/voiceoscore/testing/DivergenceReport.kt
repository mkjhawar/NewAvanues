/**
 * DivergenceReport.kt - Divergence detection and reporting
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 */
package com.augmentalis.voiceoscore.testing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Severity levels for divergences
 */
enum class DivergenceSeverity(val priority: Int) {
    CRITICAL(4),    // Different return value, crash, data loss
    HIGH(3),        // Different state, missing side effect
    MEDIUM(2),      // Timing difference >50%, extra side effect
    LOW(1)          // Timing difference 20-50%, logging differences
}

/**
 * Category of divergence
 */
enum class DivergenceCategory {
    RETURN_VALUE,       // Return value mismatch
    STATE,              // State variable mismatch
    SIDE_EFFECT,        // Side effect mismatch (DB, broadcasts, etc.)
    TIMING,             // Execution timing difference
    EXCEPTION,          // Exception mismatch (one threw, other didn't)
    ASYNC_RESULT        // Async result mismatch
}

/**
 * Details about a specific divergence
 */
data class DivergenceDetail(
    val category: DivergenceCategory,
    val severity: DivergenceSeverity,
    val message: String,
    val legacyValue: Any?,
    val refactoredValue: Any?,
    val stackTrace: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Comparison result for a single method invocation
 */
data class ComparisonResult(
    val methodName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val executionId: String,
    val divergences: List<DivergenceDetail> = emptyList(),
    val legacyExecutionTimeMs: Long = 0,
    val refactoredExecutionTimeMs: Long = 0,
    val confidence: Float = 1.0f // Confidence in comparison accuracy
) {
    val hasDivergence: Boolean
        get() = divergences.isNotEmpty()

    val maxSeverity: DivergenceSeverity?
        get() = divergences.maxByOrNull { it.severity.priority }?.severity

    val isCritical: Boolean
        get() = maxSeverity?.priority ?: 0 >= DivergenceSeverity.CRITICAL.priority
}

/**
 * Aggregated divergence statistics
 */
data class DivergenceStats(
    val totalComparisons: Long = 0,
    val totalDivergences: Long = 0,
    val criticalCount: Long = 0,
    val highCount: Long = 0,
    val mediumCount: Long = 0,
    val lowCount: Long = 0,
    val divergenceRate: Float = 0f,
    val avgComparisonTimeMs: Double = 0.0,
    val categoryCounts: Map<DivergenceCategory, Long> = emptyMap()
) {
    val divergencePercentage: Float
        get() = if (totalComparisons > 0) {
            (totalDivergences.toFloat() / totalComparisons.toFloat()) * 100f
        } else 0f
}

/**
 * Divergence reporter - logs and tracks divergences
 *
 * Features:
 * - Thread-safe divergence tracking
 * - Real-time divergence streaming
 * - Aggregated statistics
 * - Automatic severity-based logging
 */
class DivergenceReporter {
    private val divergenceFlow = MutableSharedFlow<ComparisonResult>(
        replay = 0,
        extraBufferCapacity = 1000
    )

    private val totalComparisons = AtomicLong(0)
    private val totalDivergences = AtomicLong(0)
    private val severityCounts = ConcurrentHashMap<DivergenceSeverity, AtomicLong>().apply {
        DivergenceSeverity.values().forEach { put(it, AtomicLong(0)) }
    }
    private val categoryCounts = ConcurrentHashMap<DivergenceCategory, AtomicLong>().apply {
        DivergenceCategory.values().forEach { put(it, AtomicLong(0)) }
    }

    private val comparisonTimes = mutableListOf<Long>()
    private val comparisonTimesLock = Any()

    /**
     * Observable flow of comparison results
     */
    val divergences: SharedFlow<ComparisonResult> = divergenceFlow.asSharedFlow()

    /**
     * Log a comparison result
     */
    suspend fun log(result: ComparisonResult) {
        totalComparisons.incrementAndGet()

        // Track comparison time
        synchronized(comparisonTimesLock) {
            comparisonTimes.add(System.currentTimeMillis() - result.timestamp)
            // Keep only last 1000 comparison times
            if (comparisonTimes.size > 1000) {
                comparisonTimes.removeAt(0)
            }
        }

        if (result.hasDivergence) {
            totalDivergences.incrementAndGet()

            // Update severity counts
            result.divergences.forEach { divergence ->
                severityCounts[divergence.severity]?.incrementAndGet()
                categoryCounts[divergence.category]?.incrementAndGet()
            }

            // Log based on severity
            logDivergence(result)

            // Emit to flow
            divergenceFlow.emit(result)
        }
    }

    /**
     * Log divergence based on severity
     */
    private fun logDivergence(result: ComparisonResult) {
        val severity = result.maxSeverity ?: return
        val header = "[$severity] Divergence in ${result.methodName}"

        val message = buildString {
            appendLine(header)
            appendLine("Execution ID: ${result.executionId}")
            appendLine("Timestamp: ${result.timestamp}")
            appendLine("Legacy execution: ${result.legacyExecutionTimeMs}ms")
            appendLine("Refactored execution: ${result.refactoredExecutionTimeMs}ms")
            appendLine("Divergences (${result.divergences.size}):")

            result.divergences.forEachIndexed { index, divergence ->
                appendLine("  ${index + 1}. [${divergence.category}] ${divergence.message}")
                appendLine("     Legacy: ${divergence.legacyValue}")
                appendLine("     Refactored: ${divergence.refactoredValue}")
                if (divergence.stackTrace != null) {
                    appendLine("     Stack: ${divergence.stackTrace}")
                }
                if (divergence.metadata.isNotEmpty()) {
                    appendLine("     Metadata: ${divergence.metadata}")
                }
            }
        }

        when (severity) {
            DivergenceSeverity.CRITICAL -> android.util.Log.e(TAG, message)
            DivergenceSeverity.HIGH -> android.util.Log.w(TAG, message)
            DivergenceSeverity.MEDIUM -> android.util.Log.i(TAG, message)
            DivergenceSeverity.LOW -> android.util.Log.d(TAG, message)
        }
    }

    /**
     * Get current statistics
     */
    fun getStats(): DivergenceStats {
        val avgTime = synchronized(comparisonTimesLock) {
            if (comparisonTimes.isEmpty()) 0.0
            else comparisonTimes.average()
        }

        return DivergenceStats(
            totalComparisons = totalComparisons.get(),
            totalDivergences = totalDivergences.get(),
            criticalCount = severityCounts[DivergenceSeverity.CRITICAL]?.get() ?: 0,
            highCount = severityCounts[DivergenceSeverity.HIGH]?.get() ?: 0,
            mediumCount = severityCounts[DivergenceSeverity.MEDIUM]?.get() ?: 0,
            lowCount = severityCounts[DivergenceSeverity.LOW]?.get() ?: 0,
            avgComparisonTimeMs = avgTime,
            categoryCounts = categoryCounts.mapValues { it.value.get() }
        )
    }

    /**
     * Reset all statistics
     */
    fun reset() {
        totalComparisons.set(0)
        totalDivergences.set(0)
        severityCounts.values.forEach { it.set(0) }
        categoryCounts.values.forEach { it.set(0) }
        synchronized(comparisonTimesLock) {
            comparisonTimes.clear()
        }
    }

    companion object {
        private const val TAG = "DivergenceReporter"
    }
}
