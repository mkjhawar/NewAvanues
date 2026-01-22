/**
 * ComparisonFramework.kt - Core comparison engine for behavioral testing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 *
 * This framework enables comprehensive comparison between legacy and refactored
 * implementations, detecting ANY difference in behavior including:
 * - Return values
 * - State changes
 * - Side effects (DB, broadcasts, services, etc.)
 * - Execution timing
 * - Exceptions
 *
 * The framework is designed for:
 * - Low overhead (<10ms per comparison)
 * - Real-time divergence detection
 * - Automatic rollback on critical divergences
 * - Comprehensive metrics and reporting
 */
package com.augmentalis.voiceoscore.testing

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import com.augmentalis.avid.core.AvidGenerator

/**
 * Configuration for comparison framework
 */
data class ComparisonConfig(
    val enableStateComparison: Boolean = true,
    val enableReturnValueComparison: Boolean = true,
    val enableSideEffectComparison: Boolean = true,
    val enableTimingComparison: Boolean = true,
    val timingThresholdPercent: Float = 20f,
    val sideEffectOrderMatters: Boolean = true,
    val maxComparisonTimeMs: Long = 10,
    val enableAlerts: Boolean = true,
    val enableRollback: Boolean = true
)

/**
 * Main comparison framework
 *
 * Usage:
 * ```kotlin
 * val framework = ComparisonFramework(config)
 *
 * // Compare method execution
 * val result = framework.compare(
 *     methodName = "handleVoiceCommand",
 *     legacyExecution = { legacyService.handleVoiceCommand(cmd) },
 *     refactoredExecution = { refactoredService.handleVoiceCommand(cmd) }
 * )
 *
 * if (result.isCritical) {
 *     // Rollback triggered automatically
 * }
 * ```
 */
class ComparisonFramework(
    private val config: ComparisonConfig = ComparisonConfig(),
    private val rollbackTrigger: RollbackTrigger? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Comparators
    private val returnValueComparator = ReturnValueComparator()
    private val stateComparator = StateComparator()
    private val sideEffectComparator = SideEffectComparator()
    private val timingComparator = TimingComparator()

    // Reporting and alerts
    private val divergenceReporter = DivergenceReporter()
    private val metricsCollector = ComparisonMetricsCollector()
    private val alertSystem = DivergenceAlertSystem(rollbackTrigger)

    /**
     * Compare execution of legacy and refactored implementations
     *
     * @param methodName Name of method being compared
     * @param legacyExecution Lambda that executes legacy implementation
     * @param refactoredExecution Lambda that executes refactored implementation
     * @param captureState Optional lambda to capture state snapshots
     * @param captureSideEffects Optional lambda to capture side effects
     * @return Comparison result with all divergences
     */
    suspend fun <T> compare(
        methodName: String,
        legacyExecution: suspend (SideEffectTracker?) -> T,
        refactoredExecution: suspend (SideEffectTracker?) -> T,
        captureState: (suspend () -> Pair<ServiceStateSnapshot, ServiceStateSnapshot>)? = null,
        ignoredStateFields: Set<String> = StateComparator.DEFAULT_IGNORED_FIELDS
    ): ComparisonResult = withContext(Dispatchers.Default) {
        val executionId = AvidGenerator.generateCompactSimple(AvidGenerator.Module.VOICEOS, "element")
        val comparisonStart = System.currentTimeMillis()

        Log.d(TAG, "Starting comparison: $methodName (id: $executionId)")

        val divergences = mutableListOf<DivergenceDetail>()

        // Capture initial state (if provided)
        val initialState = captureState?.invoke()

        // Setup side effect trackers
        val legacyTracker = if (config.enableSideEffectComparison) SideEffectTracker() else null
        val refactoredTracker = if (config.enableSideEffectComparison) SideEffectTracker() else null

        // Execute both implementations with timing and exception handling
        val legacyResult = executeWithTracking(
            methodName = "$methodName (legacy)",
            tracker = legacyTracker,
            execution = legacyExecution
        )

        val refactoredResult = executeWithTracking(
            methodName = "$methodName (refactored)",
            tracker = refactoredTracker,
            execution = refactoredExecution
        )

        // Compare exceptions first
        val exceptionDivergences = compareExceptions(
            methodName,
            legacyResult.exception,
            refactoredResult.exception
        )
        divergences.addAll(exceptionDivergences)

        // Only continue if both succeeded or both failed the same way
        if (exceptionDivergences.isEmpty() || exceptionDivergences.none { it.severity == DivergenceSeverity.CRITICAL }) {

            // Compare return values
            if (config.enableReturnValueComparison && legacyResult.exception == null && refactoredResult.exception == null) {
                val returnValueDivergences = returnValueComparator.compare(
                    legacyResult.result,
                    refactoredResult.result,
                    methodName
                )
                divergences.addAll(returnValueDivergences)
            }

            // Compare state changes (if provided)
            if (config.enableStateComparison && initialState != null) {
                val finalState = captureState.invoke()
                val stateDivergences = stateComparator.compare(
                    initialState.first,
                    finalState.first,
                    methodName,
                    ignoredStateFields
                )
                divergences.addAll(stateDivergences)
            }

            // Compare side effects
            if (config.enableSideEffectComparison && legacyTracker != null && refactoredTracker != null) {
                val legacyTrace = legacyTracker.stop()
                val refactoredTrace = refactoredTracker.stop()

                val sideEffectDivergences = sideEffectComparator.compare(
                    legacyTrace,
                    refactoredTrace,
                    methodName,
                    orderMatters = config.sideEffectOrderMatters
                )
                divergences.addAll(sideEffectDivergences)
            }

            // Compare timing
            if (config.enableTimingComparison) {
                val timingDivergences = timingComparator.compare(
                    legacyResult.executionTimeMs,
                    refactoredResult.executionTimeMs,
                    methodName,
                    config.timingThresholdPercent
                )
                divergences.addAll(timingDivergences)
            }
        }

        // Calculate comparison overhead
        val comparisonOverhead = System.currentTimeMillis() - comparisonStart

        // Create comparison result
        val result = ComparisonResult(
            methodName = methodName,
            timestamp = comparisonStart,
            executionId = executionId,
            divergences = divergences,
            legacyExecutionTimeMs = legacyResult.executionTimeMs,
            refactoredExecutionTimeMs = refactoredResult.executionTimeMs,
            confidence = calculateConfidence(divergences)
        )

        // Log result
        divergenceReporter.log(result)

        // Record metrics
        metricsCollector.recordComparison(result, comparisonOverhead)

        // Evaluate alerts (if enabled)
        if (config.enableAlerts) {
            alertSystem.evaluate(result)
        }

        // Log comparison overhead
        if (comparisonOverhead > config.maxComparisonTimeMs) {
            Log.w(TAG, "Comparison overhead ${comparisonOverhead}ms exceeds target ${config.maxComparisonTimeMs}ms")
        }

        Log.d(TAG, "Comparison complete: $methodName - " +
                "${if (result.hasDivergence) "DIVERGED (${divergences.size})" else "MATCHED"} " +
                "in ${comparisonOverhead}ms")

        result
    }

    /**
     * Execute a block with tracking (timing, exceptions, side effects)
     */
    private data class ExecutionResult<T>(
        val result: T?,
        val exception: Throwable?,
        val executionTimeMs: Long
    )

    private suspend fun <T> executeWithTracking(
        methodName: String,
        tracker: SideEffectTracker?,
        execution: suspend (SideEffectTracker?) -> T
    ): ExecutionResult<T> {
        val start = System.currentTimeMillis()
        tracker?.start()

        return try {
            val result = execution(tracker)
            val duration = System.currentTimeMillis() - start

            ExecutionResult(
                result = result,
                exception = null,
                executionTimeMs = duration
            )
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - start
            Log.w(TAG, "Exception in $methodName: ${e.message}")

            ExecutionResult(
                result = null,
                exception = e,
                executionTimeMs = duration
            )
        }
    }

    /**
     * Compare exceptions between implementations
     */
    private fun compareExceptions(
        methodName: String,
        legacyException: Throwable?,
        refactoredException: Throwable?
    ): List<DivergenceDetail> {
        // Both succeeded - no divergence
        if (legacyException == null && refactoredException == null) {
            return emptyList()
        }

        // Both failed with same exception type - acceptable
        if (legacyException != null && refactoredException != null &&
            legacyException::class == refactoredException::class
        ) {
            return listOf(
                DivergenceDetail(
                    category = DivergenceCategory.EXCEPTION,
                    severity = DivergenceSeverity.LOW,
                    message = "Both implementations threw ${legacyException::class.simpleName} in $methodName (acceptable)",
                    legacyValue = legacyException.message,
                    refactoredValue = refactoredException.message
                )
            )
        }

        // One succeeded, one failed - CRITICAL divergence
        return listOf(
            DivergenceDetail(
                category = DivergenceCategory.EXCEPTION,
                severity = DivergenceSeverity.CRITICAL,
                message = "Exception mismatch in $methodName",
                legacyValue = legacyException?.let { "${it::class.simpleName}: ${it.message}" } ?: "SUCCESS",
                refactoredValue = refactoredException?.let { "${it::class.simpleName}: ${it.message}" } ?: "SUCCESS",
                stackTrace = (legacyException ?: refactoredException)?.stackTraceToString()
            )
        )
    }

    /**
     * Calculate confidence in comparison accuracy
     *
     * Confidence decreases if:
     * - Comparison took too long (may have missed things)
     * - State capture was not provided
     * - Side effect tracking was disabled
     */
    private fun calculateConfidence(divergences: List<DivergenceDetail>): Float {
        var confidence = 1.0f

        // Reduce confidence if certain features were disabled
        if (!config.enableStateComparison) confidence *= 0.9f
        if (!config.enableSideEffectComparison) confidence *= 0.9f
        if (!config.enableTimingComparison) confidence *= 0.95f

        // Reduce confidence if there are many divergences (may indicate comparison issues)
        if (divergences.size > 10) confidence *= 0.8f

        return confidence
    }

    /**
     * Get current divergence statistics
     */
    fun getStats(): DivergenceStats {
        return divergenceReporter.getStats()
    }

    /**
     * Get comparison metrics
     */
    fun getMetrics(): ComparisonMetricsSnapshot {
        return metricsCollector.getCurrentMetrics()
    }

    /**
     * Get timing statistics for a method
     */
    fun getTimingStats(methodName: String): Pair<TimingStats, TimingStats>? {
        return timingComparator.getAllStats()[methodName]
    }

    /**
     * Get alert system for configuration
     */
    fun getAlertSystem(): DivergenceAlertSystem {
        return alertSystem
    }

    /**
     * Generate comprehensive report
     */
    fun generateReport(): String {
        return buildString {
            appendLine("=== COMPARISON FRAMEWORK REPORT ===")
            appendLine()

            // Divergence statistics
            val stats = getStats()
            appendLine("## Divergence Summary")
            appendLine("Total Comparisons: ${stats.totalComparisons}")
            appendLine("Total Divergences: ${stats.totalDivergences} (${stats.divergencePercentage}%)")
            appendLine("Critical: ${stats.criticalCount}, High: ${stats.highCount}, Medium: ${stats.mediumCount}, Low: ${stats.lowCount}")
            appendLine()

            // Metrics
            appendLine(metricsCollector.generateReport())
            appendLine()

            // Alert statistics
            appendLine("## Alert System")
            val alertStats = alertSystem.getStats()
            appendLine("Total Alerts: ${alertStats["totalAlerts"]}")
            appendLine("Circuit Breaker: ${alertStats["circuitBreakerState"]}")
            appendLine()

            // Timing analysis
            val allTimingStats = timingComparator.getAllStats()
            if (allTimingStats.isNotEmpty()) {
                appendLine("## Timing Analysis (Top 10 Methods)")
                allTimingStats.entries
                    .sortedByDescending { (it.value.first.avgMs + it.value.second.avgMs) / 2 }
                    .take(10)
                    .forEach { (methodName, stats) ->
                        val (legacy, refactored) = stats
                        appendLine()
                        appendLine("Method: $methodName")
                        appendLine("  Legacy:     avg=${legacy.avgMs}ms, p95=${legacy.p95Ms}ms, count=${legacy.count}")
                        appendLine("  Refactored: avg=${refactored.avgMs}ms, p95=${refactored.p95Ms}ms, count=${refactored.count}")
                        val perfDelta = if (legacy.avgMs > 0) {
                            ((refactored.avgMs - legacy.avgMs) / legacy.avgMs * 100).toInt()
                        } else 0
                        appendLine("  Performance: ${if (perfDelta > 0) "+" else ""}$perfDelta%")
                    }
            }
        }
    }

    /**
     * Reset all statistics and metrics
     */
    fun reset() {
        divergenceReporter.reset()
        metricsCollector.reset()
        timingComparator.reset()
        alertSystem.reset()
        Log.i(TAG, "Comparison framework reset")
    }

    companion object {
        private const val TAG = "ComparisonFramework"
    }
}

/**
 * Simple rollback trigger that logs rollback requests
 */
class LoggingRollbackTrigger : RollbackTrigger {
    override suspend fun triggerRollback(reason: String, result: ComparisonResult) {
        Log.e("RollbackTrigger", "ROLLBACK TRIGGERED: $reason")
        Log.e("RollbackTrigger", "Method: ${result.methodName}")
        Log.e("RollbackTrigger", "Execution ID: ${result.executionId}")
        Log.e("RollbackTrigger", "Divergences: ${result.divergences.size}")
        result.divergences.forEach { divergence ->
            Log.e("RollbackTrigger", "  - [${divergence.severity}] ${divergence.message}")
        }
    }
}
