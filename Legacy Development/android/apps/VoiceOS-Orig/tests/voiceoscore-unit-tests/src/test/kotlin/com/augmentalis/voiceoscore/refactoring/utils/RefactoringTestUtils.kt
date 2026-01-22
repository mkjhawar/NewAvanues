/**
 * RefactoringTestUtils.kt - Test utility functions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-15 03:37:00 PDT
 * Part of: VoiceOSService SOLID Refactoring - Day 3 Afternoon
 */
package com.augmentalis.voiceoscore.refactoring.utils

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.models.CommandContext
import com.augmentalis.voiceoscore.refactoring.interfaces.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.mockito.Mockito
import kotlin.system.measureTimeMillis

/**
 * Test Utility Functions for VoiceOSService Refactoring
 *
 * Provides common test helpers:
 * - Timing measurements
 * - Flow collection
 * - Event creation
 * - Assertion helpers
 * - Performance validation
 */
object RefactoringTestUtils {

    // ========================================
    // Timing & Performance
    // ========================================

    /**
     * Measure execution time of a suspend function
     * @return Pair<Result, ExecutionTimeMs>
     */
    suspend fun <T> measureExecutionTime(block: suspend () -> T): Pair<T, Long> {
        var result: T? = null
        val timeMs = measureTimeMillis {
            result = block()
        }
        return Pair(result!!, timeMs)
    }

    /**
     * Assert execution time is under threshold
     */
    suspend fun <T> assertExecutionTime(
        thresholdMs: Long,
        message: String = "Execution time exceeded threshold",
        block: suspend () -> T
    ): T {
        val (result, timeMs) = measureExecutionTime(block)
        assert(timeMs <= thresholdMs) {
            "$message: Expected <= ${thresholdMs}ms, got ${timeMs}ms"
        }
        return result
    }

    /**
     * Measure DI overhead (component creation time)
     */
    suspend fun measureDIOverhead(
        iterations: Int = 100,
        block: suspend () -> Any
    ): DIOverheadMetrics {
        val times = mutableListOf<Long>()

        repeat(iterations) {
            val (_, timeMs) = measureExecutionTime(block)
            times.add(timeMs)
        }

        return DIOverheadMetrics(
            iterations = iterations,
            averageMs = times.average(),
            minMs = times.minOrNull() ?: 0,
            maxMs = times.maxOrNull() ?: 0,
            medianMs = times.sorted()[times.size / 2].toDouble(),
            p95Ms = times.sorted()[(times.size * 0.95).toInt()].toDouble()
        )
    }

    // ========================================
    // Flow Collection Helpers
    // ========================================

    /**
     * Collect flow events for a duration
     */
    suspend fun <T> Flow<T>.collectForDuration(durationMs: Long): List<T> {
        val events = mutableListOf<T>()
        val job = Job()

        withTimeout(durationMs) {
            try {
                collect { event ->
                    events.add(event)
                }
            } catch (e: Exception) {
                // Timeout expected
            }
        }

        return events
    }

    /**
     * Collect N events from a flow with timeout
     */
    suspend fun <T> Flow<T>.collectN(count: Int, timeoutMs: Long = 5000): List<T> {
        return withTimeout(timeoutMs) {
            val events = mutableListOf<T>()
            takeWhile { events.size < count }
                .collect { events.add(it) }
            events
        }
    }

    /**
     * Wait for specific flow event with timeout
     */
    suspend fun <T> Flow<T>.waitForEvent(
        predicate: (T) -> Boolean,
        timeoutMs: Long = 5000
    ): T {
        return withTimeout(timeoutMs) {
            first(predicate)
        }
    }

    // ========================================
    // Mock Creation Helpers
    // ========================================

    /**
     * Create mock AccessibilityEvent
     */
    fun createMockAccessibilityEvent(
        eventType: Int = AccessibilityEvent.TYPE_VIEW_CLICKED,
        packageName: String = "com.test.app",
        className: String = "android.widget.Button",
        text: String = "Test Button"
    ): AccessibilityEvent {
        val event = AccessibilityEvent.obtain(eventType)
        event.packageName = packageName
        event.className = className
        event.text.add(text)
        return event
    }

    /**
     * Create mock AccessibilityNodeInfo
     */
    fun createMockAccessibilityNodeInfo(
        text: String = "Test",
        contentDescription: String? = null,
        resourceId: String? = null,
        className: String = "android.widget.Button",
        isClickable: Boolean = true,
        isEnabled: Boolean = true
    ): AccessibilityNodeInfo {
        val node = Mockito.mock(AccessibilityNodeInfo::class.java)
        Mockito.`when`(node.text).thenReturn(text)
        Mockito.`when`(node.contentDescription).thenReturn(contentDescription)
        Mockito.`when`(node.viewIdResourceName).thenReturn(resourceId)
        Mockito.`when`(node.className).thenReturn(className)
        Mockito.`when`(node.isClickable).thenReturn(isClickable)
        Mockito.`when`(node.isEnabled).thenReturn(isEnabled)
        return node
    }

    /**
     * Create mock CommandContext
     */
    fun createMockCommandContext(
        packageName: String = "com.test.app",
        activityName: String = "MainActivity",
        extras: Map<String, Any> = emptyMap()
    ): CommandContext {
        // CommandContext expects a packageName String, not a map
        return CommandContext(packageName)
    }

    // ========================================
    // Assertion Helpers
    // ========================================

    /**
     * Assert state transition is valid
     */
    fun <T : Enum<T>> assertValidStateTransition(
        from: T,
        to: T,
        validTransitions: Map<T, Set<T>>,
        message: String = "Invalid state transition"
    ) {
        val allowedNextStates = validTransitions[from] ?: emptySet()
        assert(to in allowedNextStates) {
            "$message: Cannot transition from $from to $to. Allowed: $allowedNextStates"
        }
    }

    /**
     * Assert metrics are within acceptable ranges
     */
    fun assertMetricsValid(
        metrics: Map<String, Number>,
        ranges: Map<String, ClosedRange<Double>>,
        message: String = "Metrics out of range"
    ) {
        metrics.forEach { (key, value) ->
            val range = ranges[key] ?: return@forEach
            val doubleValue = value.toDouble()
            assert(doubleValue in range) {
                "$message: $key = $doubleValue, expected $range"
            }
        }
    }

    /**
     * Assert performance requirements met
     */
    fun assertPerformanceRequirements(
        cpuPercent: Float,
        memoryMb: Long,
        responseTimeMs: Long,
        maxCpu: Float = 5f,
        maxMemoryMb: Long = 20,
        maxResponseMs: Long = 100
    ) {
        assert(cpuPercent <= maxCpu) {
            "CPU usage too high: $cpuPercent% (max: $maxCpu%)"
        }
        assert(memoryMb <= maxMemoryMb) {
            "Memory usage too high: ${memoryMb}MB (max: ${maxMemoryMb}MB)"
        }
        assert(responseTimeMs <= maxResponseMs) {
            "Response time too slow: ${responseTimeMs}ms (max: ${maxResponseMs}ms)"
        }
    }

    // ========================================
    // Concurrency Helpers
    // ========================================

    /**
     * Run multiple operations concurrently
     */
    suspend fun runConcurrently(
        count: Int,
        block: suspend (index: Int) -> Unit
    ) {
        val jobs = (0 until count).map { index ->
            CoroutineScope(Job()).launch {
                block(index)
            }
        }
        jobs.forEach { it.join() }
    }

    /**
     * Stress test with concurrent operations
     */
    suspend fun stressTest(
        operations: Int,
        concurrency: Int,
        operation: suspend () -> Unit
    ): StressTestResult {
        val startTime = System.currentTimeMillis()
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<Exception>()

        val batches = operations / concurrency
        repeat(batches) {
            runConcurrently(concurrency) {
                try {
                    operation()
                    successCount++
                } catch (e: Exception) {
                    failureCount++
                    errors.add(e)
                }
            }
        }

        val durationMs = System.currentTimeMillis() - startTime

        return StressTestResult(
            totalOperations = operations,
            successCount = successCount,
            failureCount = failureCount,
            durationMs = durationMs,
            operationsPerSecond = (operations.toDouble() / durationMs) * 1000,
            errors = errors
        )
    }

    // ========================================
    // Retry Helpers
    // ========================================

    /**
     * Retry operation with exponential backoff
     */
    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 100,
        factor: Double = 2.0,
        block: suspend (attempt: Int) -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block(attempt)
            } catch (e: Exception) {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        return block(maxAttempts - 1) // Last attempt, let exception propagate
    }

    // ========================================
    // Data Classes
    // ========================================

    data class DIOverheadMetrics(
        val iterations: Int,
        val averageMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val medianMs: Double,
        val p95Ms: Double
    ) {
        fun isAcceptable(maxAverageMs: Double = 5.0): Boolean {
            return averageMs <= maxAverageMs
        }

        override fun toString(): String {
            return """
                DI Overhead Metrics ($iterations iterations):
                  Average: ${"%.2f".format(averageMs)}ms
                  Min: ${minMs}ms
                  Max: ${maxMs}ms
                  Median: ${"%.2f".format(medianMs)}ms
                  P95: ${"%.2f".format(p95Ms)}ms
            """.trimIndent()
        }
    }

    data class StressTestResult(
        val totalOperations: Int,
        val successCount: Int,
        val failureCount: Int,
        val durationMs: Long,
        val operationsPerSecond: Double,
        val errors: List<Exception>
    ) {
        val successRate: Double = successCount.toDouble() / totalOperations

        override fun toString(): String {
            return """
                Stress Test Result:
                  Total Operations: $totalOperations
                  Successes: $successCount
                  Failures: $failureCount
                  Success Rate: ${"%.2f".format(successRate * 100)}%
                  Duration: ${durationMs}ms
                  Ops/sec: ${"%.2f".format(operationsPerSecond)}
                  Errors: ${errors.size}
            """.trimIndent()
        }
    }
}
