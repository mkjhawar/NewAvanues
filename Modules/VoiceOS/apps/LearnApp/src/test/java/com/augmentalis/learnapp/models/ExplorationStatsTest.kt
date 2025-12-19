/**
 * ExplorationStatsTest.kt - Unit tests for ExplorationStats
 *
 * Tests statistic calculations and formatting.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-18
 */

package com.augmentalis.learnapp.models

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ExplorationStats
 *
 * Tests:
 * - Duration formatting
 * - Average calculations
 * - Edge case handling (zeros)
 * - String output
 */
class ExplorationStatsTest {

    // ============================================================
    // Duration Formatting Tests
    // ============================================================

    @Test
    fun `formatDuration formats seconds correctly`() {
        val stats = createStats(durationMs = 45000) // 45 seconds

        assertEquals("00:45", stats.formatDuration())
    }

    @Test
    fun `formatDuration formats minutes and seconds`() {
        val stats = createStats(durationMs = 185000) // 3:05

        assertEquals("03:05", stats.formatDuration())
    }

    @Test
    fun `formatDuration handles zero duration`() {
        val stats = createStats(durationMs = 0)

        assertEquals("00:00", stats.formatDuration())
    }

    @Test
    fun `formatDuration handles long durations`() {
        val stats = createStats(durationMs = 3661000) // 61:01

        assertEquals("61:01", stats.formatDuration())
    }

    // ============================================================
    // Average Calculations Tests
    // ============================================================

    @Test
    fun `averageElementsPerScreen calculates correctly`() {
        val stats = createStats(totalScreens = 10, totalElements = 250)

        assertEquals(25.0f, stats.averageElementsPerScreen(), 0.01f)
    }

    @Test
    fun `averageElementsPerScreen returns zero for no screens`() {
        val stats = createStats(totalScreens = 0, totalElements = 0)

        assertEquals(0f, stats.averageElementsPerScreen(), 0.01f)
    }

    @Test
    fun `averageEdgesPerScreen calculates correctly`() {
        val stats = createStats(totalScreens = 5, totalEdges = 15)

        assertEquals(3.0f, stats.averageEdgesPerScreen(), 0.01f)
    }

    @Test
    fun `averageEdgesPerScreen returns zero for no screens`() {
        val stats = createStats(totalScreens = 0, totalEdges = 0)

        assertEquals(0f, stats.averageEdgesPerScreen(), 0.01f)
    }

    @Test
    fun `averageEdgesPerScreen handles fractional results`() {
        val stats = createStats(totalScreens = 3, totalEdges = 7)

        assertEquals(2.33f, stats.averageEdgesPerScreen(), 0.01f)
    }

    // ============================================================
    // Default Values Tests
    // ============================================================

    @Test
    fun `default safety values are zero`() {
        val stats = ExplorationStats(
            packageName = "com.test",
            appName = "Test",
            totalScreens = 10,
            totalElements = 100,
            totalEdges = 20,
            durationMs = 60000,
            maxDepth = 5
        )

        assertEquals(0, stats.dangerousElementsSkipped)
        assertEquals(0, stats.loginScreensDetected)
        assertEquals(0, stats.scrollableContainersFound)
    }

    // ============================================================
    // toString Tests
    // ============================================================

    @Test
    fun `toString contains app name`() {
        val stats = createStats(appName = "Instagram")

        val output = stats.toString()

        assertTrue(output.contains("Instagram"))
    }

    @Test
    fun `toString contains all key metrics`() {
        val stats = ExplorationStats(
            packageName = "com.test",
            appName = "TestApp",
            totalScreens = 15,
            totalElements = 200,
            totalEdges = 45,
            durationMs = 120000,
            maxDepth = 8,
            dangerousElementsSkipped = 3,
            loginScreensDetected = 1,
            scrollableContainersFound = 10
        )

        val output = stats.toString()

        assertTrue(output.contains("15")) // screens
        assertTrue(output.contains("200")) // elements
        assertTrue(output.contains("45")) // edges
        assertTrue(output.contains("8")) // maxDepth
        assertTrue(output.contains("02:00")) // duration
        assertTrue(output.contains("3")) // dangerous
        assertTrue(output.contains("1")) // login
        assertTrue(output.contains("10")) // scrollable
    }

    // ============================================================
    // Data Class Tests
    // ============================================================

    @Test
    fun `equals works correctly`() {
        val stats1 = createStats(totalScreens = 10)
        val stats2 = createStats(totalScreens = 10)
        val stats3 = createStats(totalScreens = 20)

        assertEquals(stats1, stats2)
        assertNotEquals(stats1, stats3)
    }

    @Test
    fun `copy creates independent instance`() {
        val original = createStats(totalScreens = 10)
        val copy = original.copy(totalScreens = 20)

        assertEquals(10, original.totalScreens)
        assertEquals(20, copy.totalScreens)
    }

    // ============================================================
    // Helper
    // ============================================================

    private fun createStats(
        packageName: String = "com.test.app",
        appName: String = "TestApp",
        totalScreens: Int = 10,
        totalElements: Int = 100,
        totalEdges: Int = 25,
        durationMs: Long = 60000,
        maxDepth: Int = 5,
        dangerousElementsSkipped: Int = 0,
        loginScreensDetected: Int = 0,
        scrollableContainersFound: Int = 5
    ): ExplorationStats {
        return ExplorationStats(
            packageName = packageName,
            appName = appName,
            totalScreens = totalScreens,
            totalElements = totalElements,
            totalEdges = totalEdges,
            durationMs = durationMs,
            maxDepth = maxDepth,
            dangerousElementsSkipped = dangerousElementsSkipped,
            loginScreensDetected = loginScreensDetected,
            scrollableContainersFound = scrollableContainersFound
        )
    }
}
