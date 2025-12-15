/**
 * JITHashMetricsTest.kt - Unit tests for JIT hash-based rescan metrics
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * Tests JITHashMetrics data class behavior for Phase 2 Task 1.1
 * (Hash-based rescan optimization)
 */

package com.augmentalis.voiceoscore.learnapp.jit

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for JustInTimeLearner.JITHashMetrics.
 *
 * Tests:
 * - Skip rate calculation accuracy
 * - Optimization effectiveness threshold (70%)
 * - Summary string formatting
 * - Edge cases (0%, 50%, 100% skip rates)
 */
class JITHashMetricsTest {

    // ========================================================================
    // Test: Skip Rate Calculation
    // ========================================================================

    @Test
    fun `skip rate 80 of 100 screens - calculates 80 percent correctly`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 80,
            rescanned = 20,
            skipPercentage = 80.0f
        )

        assertEquals(100, metrics.totalScreens)
        assertEquals(80, metrics.skipped)
        assertEquals(20, metrics.rescanned)
        assertEquals(80.0f, metrics.skipPercentage, 0.01f)
    }

    @Test
    fun `skip rate 50 of 100 screens - calculates 50 percent correctly`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 50,
            rescanned = 50,
            skipPercentage = 50.0f
        )

        assertEquals(50.0f, metrics.skipPercentage, 0.01f)
    }

    @Test
    fun `skip rate 0 of 100 screens - calculates 0 percent correctly`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 0,
            rescanned = 100,
            skipPercentage = 0.0f
        )

        assertEquals(0.0f, metrics.skipPercentage, 0.01f)
    }

    @Test
    fun `skip rate 100 of 100 screens - calculates 100 percent correctly`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 100,
            rescanned = 0,
            skipPercentage = 100.0f
        )

        assertEquals(100.0f, metrics.skipPercentage, 0.01f)
    }

    // ========================================================================
    // Test: Optimization Effectiveness (70% threshold)
    // ========================================================================

    @Test
    fun `isOptimizationEffective - 80 percent skip rate - returns true`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 80,
            rescanned = 20,
            skipPercentage = 80.0f
        )

        assertTrue("80% skip rate should be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `isOptimizationEffective - 70 percent skip rate - returns true`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 70,
            rescanned = 30,
            skipPercentage = 70.0f
        )

        assertTrue("70% skip rate (threshold) should be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `isOptimizationEffective - 69 percent skip rate - returns false`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 69,
            rescanned = 31,
            skipPercentage = 69.0f
        )

        assertFalse("69% skip rate (below threshold) should not be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `isOptimizationEffective - 50 percent skip rate - returns false`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 50,
            rescanned = 50,
            skipPercentage = 50.0f
        )

        assertFalse("50% skip rate should not be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `isOptimizationEffective - 0 percent skip rate - returns false`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 0,
            rescanned = 100,
            skipPercentage = 0.0f
        )

        assertFalse("0% skip rate should not be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `isOptimizationEffective - 100 percent skip rate - returns true`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 100,
            rescanned = 0,
            skipPercentage = 100.0f
        )

        assertTrue("100% skip rate should be effective", metrics.isOptimizationEffective())
    }

    // ========================================================================
    // Test: Summary String Formatting
    // ========================================================================

    @Test
    fun `getSummary - formats correctly with 80 percent skip rate`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 80,
            rescanned = 20,
            skipPercentage = 80.0f
        )

        val summary = metrics.getSummary()

        assertTrue("Summary should contain 'Hash Metrics'", summary.contains("Hash Metrics"))
        assertTrue("Summary should contain skipped count '80'", summary.contains("80"))
        assertTrue("Summary should contain total count '100'", summary.contains("100"))
        assertTrue("Summary should contain percentage '80.0%'", summary.contains("80.0%"))
        assertTrue("Summary should contain 'skip rate'", summary.contains("skip rate"))
    }

    @Test
    fun `getSummary - formats correctly with 0 percent skip rate`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 0,
            rescanned = 100,
            skipPercentage = 0.0f
        )

        val summary = metrics.getSummary()

        assertTrue("Summary should contain '0'", summary.contains("0"))
        assertTrue("Summary should contain '0.0%'", summary.contains("0.0%"))
    }

    @Test
    fun `getSummary - formats correctly with 100 percent skip rate`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 100,
            rescanned = 0,
            skipPercentage = 100.0f
        )

        val summary = metrics.getSummary()

        assertTrue("Summary should contain '100'", summary.contains("100"))
        assertTrue("Summary should contain '100.0%'", summary.contains("100.0%"))
    }

    @Test
    fun `getSummary - formats decimal percentages correctly`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 150,
            skipped = 120,
            rescanned = 30,
            skipPercentage = 80.5f
        )

        val summary = metrics.getSummary()

        assertTrue("Summary should contain '120'", summary.contains("120"))
        assertTrue("Summary should contain '150'", summary.contains("150"))
        assertTrue("Summary should contain '80.5%'", summary.contains("80.5%"))
    }

    // ========================================================================
    // Test: Edge Cases
    // ========================================================================

    @Test
    fun `edge case - zero total screens - handles gracefully`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 0,
            skipped = 0,
            rescanned = 0,
            skipPercentage = 0.0f
        )

        assertEquals(0, metrics.totalScreens)
        assertEquals(0, metrics.skipped)
        assertEquals(0, metrics.rescanned)
        assertFalse("Zero screens should not be effective", metrics.isOptimizationEffective())
        assertNotNull("Summary should not be null", metrics.getSummary())
    }

    @Test
    fun `edge case - single screen skipped - calculates 100 percent`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 1,
            skipped = 1,
            rescanned = 0,
            skipPercentage = 100.0f
        )

        assertEquals(100.0f, metrics.skipPercentage, 0.01f)
        assertTrue("Single screen skipped should be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `edge case - single screen rescanned - calculates 0 percent`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 1,
            skipped = 0,
            rescanned = 1,
            skipPercentage = 0.0f
        )

        assertEquals(0.0f, metrics.skipPercentage, 0.01f)
        assertFalse("Single screen rescanned should not be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `edge case - large numbers - calculates correctly`() {
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 10000,
            skipped = 9500,
            rescanned = 500,
            skipPercentage = 95.0f
        )

        assertEquals(95.0f, metrics.skipPercentage, 0.01f)
        assertTrue("95% skip rate should be effective", metrics.isOptimizationEffective())
    }

    // ========================================================================
    // Test: Real-World Scenarios
    // ========================================================================

    @Test
    fun `real world - typical app with 80 percent unchanged screens`() {
        // Scenario: App updated from v1.0 to v1.1
        // 80 screens unchanged (skipped), 20 screens changed (rescanned)
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 80,
            rescanned = 20,
            skipPercentage = 80.0f
        )

        assertTrue("Real-world 80% skip rate should be effective", metrics.isOptimizationEffective())
        assertTrue("Summary should be informative", metrics.getSummary().length > 50)
    }

    @Test
    fun `real world - major app redesign with 30 percent unchanged screens`() {
        // Scenario: App had major UI redesign
        // Only 30 screens unchanged (skipped), 70 screens changed (rescanned)
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 100,
            skipped = 30,
            rescanned = 70,
            skipPercentage = 30.0f
        )

        assertFalse("Major redesign 30% skip rate should not be effective", metrics.isOptimizationEffective())
    }

    @Test
    fun `real world - first app scan - 0 percent skipped`() {
        // Scenario: First time scanning app (no cached hashes)
        // 0 screens skipped, all rescanned
        val metrics = JustInTimeLearner.JITHashMetrics(
            totalScreens = 50,
            skipped = 0,
            rescanned = 50,
            skipPercentage = 0.0f
        )

        assertFalse("First scan 0% skip rate should not be effective", metrics.isOptimizationEffective())
        assertEquals(0.0f, metrics.skipPercentage, 0.01f)
    }
}
