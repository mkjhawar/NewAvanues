/**
 * ComparisonFrameworkIntegrationTest.kt - Integration test for comparison framework
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Testing Framework
 * Created: 2025-10-15 02:48:36 PDT
 */
package com.augmentalis.voiceoscore.testing

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration test demonstrating comparison framework usage
 *
 * Tests:
 * 1. Return value comparison
 * 2. State comparison
 * 3. Side effect comparison
 * 4. Timing comparison
 * 5. Exception comparison
 * 6. Alert system
 * 7. Metrics collection
 */
class ComparisonFrameworkIntegrationTest {

    private lateinit var framework: ComparisonFramework
    private lateinit var rollbackTrigger: TestRollbackTrigger

    @Before
    fun setup() {
        rollbackTrigger = TestRollbackTrigger()
        framework = ComparisonFramework(
            config = ComparisonConfig(
                timingThresholdPercent = 20f,
                maxComparisonTimeMs = 50
            ),
            rollbackTrigger = rollbackTrigger
        )
    }

    @After
    fun teardown() {
        // Print final report
        println(framework.generateReport())
    }

    /**
     * Test: Identical implementations should have no divergence
     */
    @Test
    fun testIdenticalImplementations() = runBlocking {
        val result = framework.compare(
            methodName = "identicalMethod",
            legacyExecution = { calculateSum(10, 20) },
            refactoredExecution = { calculateSum(10, 20) }
        )

        assertFalse("Should have no divergence", result.hasDivergence)
        assertEquals(30, result.divergences.size)
    }

    /**
     * Test: Different return values should be detected
     */
    @Test
    fun testReturnValueDivergence() = runBlocking {
        val result = framework.compare(
            methodName = "returnValueDifference",
            legacyExecution = { calculateSum(10, 20) }, // 30
            refactoredExecution = { calculateSum(10, 21) } // 31
        )

        assertTrue("Should detect divergence", result.hasDivergence)
        assertEquals(DivergenceSeverity.CRITICAL, result.maxSeverity)
        assertTrue(result.divergences.any { it.category == DivergenceCategory.RETURN_VALUE })
    }

    /**
     * Test: Side effect differences should be detected
     */
    @Test
    fun testSideEffectDivergence() = runBlocking {
        val result = framework.compare(
            methodName = "sideEffectDifference",
            legacyExecution = { tracker ->
                tracker?.trackDatabaseInsert("users", mapOf("id" to 1, "name" to "Alice"))
                42
            },
            refactoredExecution = { tracker ->
                tracker?.trackDatabaseInsert("users", mapOf("id" to 2, "name" to "Bob"))
                42
            }
        )

        assertTrue("Should detect side effect divergence", result.hasDivergence)
        assertTrue(result.divergences.any { it.category == DivergenceCategory.SIDE_EFFECT })
    }

    /**
     * Test: Timing differences should be detected
     */
    @Test
    fun testTimingDivergence() = runBlocking {
        val result = framework.compare(
            methodName = "timingDifference",
            legacyExecution = {
                delay(100)
                "fast"
            },
            refactoredExecution = {
                delay(300) // 3x slower
                "slow"
            }
        )

        assertTrue("Should detect timing divergence", result.hasDivergence)
        assertTrue(result.divergences.any { it.category == DivergenceCategory.TIMING })
        assertTrue(result.divergences.any { it.severity == DivergenceSeverity.MEDIUM })
    }

    /**
     * Test: Exception differences should be detected
     */
    @Test
    fun testExceptionDivergence() = runBlocking {
        val result = framework.compare(
            methodName = "exceptionDifference",
            legacyExecution = {
                throw IllegalArgumentException("Legacy error")
            },
            refactoredExecution = {
                "success"
            }
        )

        assertTrue("Should detect exception divergence", result.hasDivergence)
        assertEquals(DivergenceSeverity.CRITICAL, result.maxSeverity)
        assertTrue(result.divergences.any { it.category == DivergenceCategory.EXCEPTION })
    }

    /**
     * Test: Rollback should be triggered on critical divergence
     */
    @Test
    fun testRollbackTrigger() = runBlocking {
        val result = framework.compare(
            methodName = "criticalDivergence",
            legacyExecution = { "expected" },
            refactoredExecution = { "wrong" }
        )

        assertTrue("Should have critical divergence", result.isCritical)

        // Give alert system time to process
        delay(500)

        assertTrue("Rollback should be triggered", rollbackTrigger.wasTriggered)
        assertEquals("criticalDivergence", rollbackTrigger.lastResult?.methodName)
    }

    /**
     * Test: Multiple comparisons should accumulate metrics
     */
    @Test
    fun testMetricsCollection() = runBlocking {
        // Run multiple comparisons
        repeat(10) { i ->
            framework.compare(
                methodName = "metricTest",
                legacyExecution = { i },
                refactoredExecution = { i }
            )
        }

        // Run some with divergences
        repeat(3) { i ->
            framework.compare(
                methodName = "metricTest",
                legacyExecution = { i },
                refactoredExecution = { i + 1 } // Different
            )
        }

        val metrics = framework.getMetrics()
        assertEquals(13, metrics.totalComparisons)
        assertEquals(3, metrics.totalDivergences)
        assertTrue(metrics.avgComparisonOverheadMs < 50) // Should be fast
    }

    /**
     * Test: Alert system should throttle alerts
     */
    @Test
    fun testAlertThrottling() = runBlocking {
        var alertCount = 0
        val listener = object : AlertListener {
            override suspend fun onAlert(alert: DivergenceAlert) {
                alertCount++
            }
        }

        framework.getAlertSystem().addListener(listener)

        // Trigger multiple alerts rapidly
        repeat(10) {
            framework.compare(
                methodName = "alertTest",
                legacyExecution = { "a" },
                refactoredExecution = { "b" }
            )
            delay(100) // Small delay
        }

        // Give alert system time to process
        delay(1000)

        // Should have fewer alerts than comparisons due to throttling
        assertTrue("Alerts should be throttled", alertCount < 10)
        Log.d(TAG, "Alert count: $alertCount (out of 10 comparisons)")
    }

    /**
     * Test: Circuit breaker should open after failures
     */
    @Test
    fun testCircuitBreaker() = runBlocking {
        val circuitBreaker = framework.getAlertSystem().circuitBreaker

        // Initially closed
        assertFalse(circuitBreaker.isOpen())

        // Trigger failures
        repeat(10) {
            framework.compare(
                methodName = "circuitBreakerTest",
                legacyExecution = { throw RuntimeException("fail") },
                refactoredExecution = { "success" }
            )
        }

        // Give alert system time to process
        delay(1000)

        // Circuit should be open now
        assertTrue("Circuit breaker should be open after failures", circuitBreaker.isOpen())
    }

    /**
     * Test: Collection comparison
     */
    @Test
    fun testCollectionComparison() = runBlocking {
        val result = framework.compare(
            methodName = "collectionTest",
            legacyExecution = { listOf(1, 2, 3, 4, 5) },
            refactoredExecution = { listOf(1, 2, 3, 4, 6) } // Different last element
        )

        assertTrue("Should detect collection divergence", result.hasDivergence)
        assertTrue(result.divergences.any { it.category == DivergenceCategory.RETURN_VALUE })
    }

    // Helper functions for testing

    private suspend fun calculateSum(a: Int, b: Int): Int {
        delay(10) // Simulate some work
        return a + b
    }

    /**
     * Test rollback trigger implementation
     */
    private class TestRollbackTrigger : RollbackTrigger {
        var wasTriggered = false
        var lastResult: ComparisonResult? = null

        override suspend fun triggerRollback(reason: String, result: ComparisonResult) {
            wasTriggered = true
            lastResult = result
            Log.w(TAG, "TEST ROLLBACK TRIGGERED: $reason")
        }
    }

    companion object {
        private const val TAG = "ComparisonFrameworkTest"
    }
}
