/**
 * VUIDCreationMetricsCollectorTest.kt - Unit tests for VUIDCreationMetricsCollector
 * Path: VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/metrics/VUIDCreationMetricsCollectorTest.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-08
 * Feature: LearnApp VUID Creation Fix - Phase 3 (Observability)
 *
 * Comprehensive unit tests for VUIDCreationMetricsCollector including:
 * - Counter increments
 * - Severity classification
 * - Report generation
 * - Thread safety
 *
 * Part of: LearnApp-VUID-Metrics-Phase3-Implementation-Report-5081218-V1.md
 */

package com.augmentalis.voiceoscore.learnapp.metrics

import android.view.accessibility.AccessibilityNodeInfo
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Test suite for VUIDCreationMetricsCollector
 *
 * Validates:
 * 1. Counter increments (onElementDetected, onVUIDCreated, onElementFiltered)
 * 2. Severity classification (INTENDED, WARNING, ERROR)
 * 3. Metrics building and report generation
 * 4. Thread safety and concurrent operations
 * 5. Filter report generation
 * 6. Reset functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VUIDCreationMetricsCollectorTest {

    private lateinit var collector: VUIDCreationMetricsCollector
    private lateinit var mockNodeInfo: AccessibilityNodeInfo

    @Before
    fun setup() {
        collector = VUIDCreationMetricsCollector()
        mockNodeInfo = AccessibilityNodeInfo.obtain()
    }

    @After
    fun teardown() {
        mockNodeInfo.recycle()
    }

    // ========== Counter Increment Tests ==========

    /**
     * Test 1: Element detection counter increments correctly
     *
     * Validates:
     * - onElementDetected() increments counter
     * - Multiple calls accumulate correctly
     */
    @Test
    fun testElementDetectionCounterIncrement() {
        // Initial state
        val (detected0, created0, rate0) = collector.getCurrentStats()
        assertEquals("Initial detected should be 0", 0, detected0)

        // Increment once
        collector.onElementDetected()
        val (detected1, _, _) = collector.getCurrentStats()
        assertEquals("Detected should be 1", 1, detected1)

        // Increment multiple times
        repeat(10) { collector.onElementDetected() }
        val (detected11, _, _) = collector.getCurrentStats()
        assertEquals("Detected should be 11", 11, detected11)
    }

    /**
     * Test 2: VUID creation counter increments correctly
     *
     * Validates:
     * - onVUIDCreated() increments counter
     * - Multiple calls accumulate correctly
     */
    @Test
    fun testVUIDCreationCounterIncrement() {
        // Initial state
        val (_, created0, _) = collector.getCurrentStats()
        assertEquals("Initial created should be 0", 0, created0)

        // Increment once
        collector.onVUIDCreated()
        val (_, created1, _) = collector.getCurrentStats()
        assertEquals("Created should be 1", 1, created1)

        // Increment multiple times
        repeat(10) { collector.onVUIDCreated() }
        val (_, created11, _) = collector.getCurrentStats()
        assertEquals("Created should be 11", 11, created11)
    }

    /**
     * Test 3: Creation rate calculation is correct
     *
     * Validates:
     * - Rate = vuidsCreated / elementsDetected
     * - Rate is 0.0 when no elements detected
     * - Rate handles partial creation correctly
     */
    @Test
    fun testCreationRateCalculation() {
        // No elements - rate should be 0.0
        val (_, _, rate0) = collector.getCurrentStats()
        assertEquals("Rate should be 0.0 when no elements", 0.0, rate0, 0.001)

        // 100% rate
        collector.onElementDetected()
        collector.onVUIDCreated()
        val (_, _, rate100) = collector.getCurrentStats()
        assertEquals("Rate should be 1.0 (100%)", 1.0, rate100, 0.001)

        // 50% rate
        collector.onElementDetected() // 2 detected, 1 created
        val (_, _, rate50) = collector.getCurrentStats()
        assertEquals("Rate should be 0.5 (50%)", 0.5, rate50, 0.001)

        // 67% rate
        collector.onVUIDCreated() // 2 detected, 2 created
        collector.onElementDetected() // 3 detected, 2 created
        val (_, _, rate67) = collector.getCurrentStats()
        assertEquals("Rate should be 0.67 (67%)", 0.666, rate67, 0.01)
    }

    /**
     * Test 4: Filtered element tracking
     *
     * Validates:
     * - onElementFiltered() adds to filtered list
     * - Type counts are tracked correctly
     * - Reason counts are tracked correctly
     */
    @Test
    fun testFilteredElementTracking() {
        // Setup mock node
        mockNodeInfo.className = "android.widget.Button"
        mockNodeInfo.text = "Submit"
        mockNodeInfo.isClickable = false

        // Filter element
        collector.onElementDetected()
        collector.onElementFiltered(mockNodeInfo, "Below threshold")

        // Build metrics
        val metrics = collector.buildMetrics("com.example.app")

        // Verify
        assertEquals("Filtered count should be 1", 1, metrics.filteredCount)
        assertEquals("Button type should have count 1", 1, metrics.filteredByType["android.widget.Button"])
        assertEquals("Below threshold reason should have count 1", 1, metrics.filterReasons["Below threshold"])
    }

    /**
     * Test 5: Multiple filtered element types
     *
     * Validates:
     * - Different types are tracked separately
     * - Counts accumulate for same type
     */
    @Test
    fun testMultipleFilteredElementTypes() {
        // Create different element types
        val button = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.Button"
            isClickable = false
        }
        val imageView = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.ImageView"
            isClickable = false
        }
        val linearLayout = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.LinearLayout"
            isClickable = false
        }

        try {
            // Filter different types
            collector.onElementDetected()
            collector.onElementFiltered(button, "Below threshold")

            collector.onElementDetected()
            collector.onElementFiltered(imageView, "Decorative")

            collector.onElementDetected()
            collector.onElementFiltered(button, "Below threshold")

            collector.onElementDetected()
            collector.onElementFiltered(linearLayout, "Container blacklist")

            // Build metrics
            val metrics = collector.buildMetrics("com.example.app")

            // Verify
            assertEquals("Total filtered should be 3", 3, metrics.filteredCount)
            assertEquals("Button count should be 2", 2, metrics.filteredByType["android.widget.Button"])
            assertEquals("ImageView count should be 1", 1, metrics.filteredByType["android.widget.ImageView"])
            assertEquals("LinearLayout count should be 1", 1, metrics.filteredByType["android.widget.LinearLayout"])
            assertEquals("Below threshold reason should be 2", 2, metrics.filterReasons["Below threshold"])
            assertEquals("Decorative reason should be 1", 1, metrics.filterReasons["Decorative"])
            assertEquals("Container blacklist reason should be 1", 1, metrics.filterReasons["Container blacklist"])
        } finally {
            button.recycle()
            imageView.recycle()
            linearLayout.recycle()
        }
    }

    // ========== Severity Classification Tests ==========

    /**
     * Test 6: ERROR severity for isClickable=true elements
     *
     * Validates:
     * - isClickable=true always generates ERROR
     * - This should NEVER happen after Phase 1 fix
     */
    @Test
    fun testErrorSeverityForClickableElement() {
        // Setup clickable element
        mockNodeInfo.className = "android.widget.Button"
        mockNodeInfo.isClickable = true

        // Filter it (should be ERROR)
        collector.onElementDetected()
        collector.onElementFiltered(mockNodeInfo, "Test filter")

        // Generate report
        val report = collector.generateFilterReport()

        // Verify
        assertEquals("Should have 1 ERROR", 1, report.errorCount)
        assertEquals("Should have 0 WARNING", 0, report.warningCount)
        assertEquals("Should have 0 INTENDED", 0, report.intendedCount)
        assertEquals("First element should be ERROR severity", FilterSeverity.ERROR, report.elements[0].severity)
    }

    /**
     * Test 7: WARNING severity for containers with click hints
     *
     * Validates:
     * - Container + isFocusable = WARNING
     * - Container + ACTION_CLICK = WARNING
     */
    @Test
    fun testWarningSeverityForContainerWithHints() {
        // Test 1: Container with isFocusable
        val container1 = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.LinearLayout"
            isClickable = false
            isFocusable = true
        }

        collector.onElementDetected()
        collector.onElementFiltered(container1, "Below threshold")

        val report1 = collector.generateFilterReport()
        assertEquals("Should have 1 WARNING for focusable container", 1, report1.warningCount)
        container1.recycle()

        // Reset and test 2: Container with ACTION_CLICK
        collector.reset()
        val container2 = AccessibilityNodeInfo.obtain().apply {
            className = "androidx.cardview.widget.CardView"
            isClickable = false
            addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
        }

        collector.onElementDetected()
        collector.onElementFiltered(container2, "Below threshold")

        val report2 = collector.generateFilterReport()
        assertEquals("Should have 1 WARNING for container with click action", 1, report2.warningCount)
        container2.recycle()
    }

    /**
     * Test 8: INTENDED severity for normal filtering
     *
     * Validates:
     * - Non-clickable, non-container elements = INTENDED
     * - Decorative elements = INTENDED
     */
    @Test
    fun testIntendedSeverityForNormalFiltering() {
        // Setup decorative element
        mockNodeInfo.className = "android.widget.ImageView"
        mockNodeInfo.isClickable = false
        mockNodeInfo.isFocusable = false
        mockNodeInfo.contentDescription = null

        // Filter it
        collector.onElementDetected()
        collector.onElementFiltered(mockNodeInfo, "Decorative")

        // Generate report
        val report = collector.generateFilterReport()

        // Verify
        assertEquals("Should have 0 ERROR", 0, report.errorCount)
        assertEquals("Should have 0 WARNING", 0, report.warningCount)
        assertEquals("Should have 1 INTENDED", 1, report.intendedCount)
        assertEquals("First element should be INTENDED severity", FilterSeverity.INTENDED, report.elements[0].severity)
    }

    /**
     * Test 9: Mixed severity classifications
     *
     * Validates:
     * - Multiple severities tracked separately
     * - Counts are accurate
     */
    @Test
    fun testMixedSeverityClassifications() {
        // Create test elements
        val clickable = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.Button"
            isClickable = true
        }
        val containerWithHint = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.LinearLayout"
            isClickable = false
            isFocusable = true
        }
        val decorative = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.ImageView"
            isClickable = false
        }

        try {
            // Filter all three
            collector.onElementDetected()
            collector.onElementFiltered(clickable, "Filtered clickable") // ERROR

            collector.onElementDetected()
            collector.onElementFiltered(containerWithHint, "Filtered container") // WARNING

            collector.onElementDetected()
            collector.onElementFiltered(decorative, "Decorative") // INTENDED

            // Generate report
            val report = collector.generateFilterReport()

            // Verify
            assertEquals("Should have 1 ERROR", 1, report.errorCount)
            assertEquals("Should have 1 WARNING", 1, report.warningCount)
            assertEquals("Should have 1 INTENDED", 1, report.intendedCount)
            assertEquals("Total should be 3", 3, report.totalFiltered)
        } finally {
            clickable.recycle()
            containerWithHint.recycle()
            decorative.recycle()
        }
    }

    // ========== Metrics Building Tests ==========

    /**
     * Test 10: Build complete metrics
     *
     * Validates:
     * - All fields are populated correctly
     * - Package name is stored
     * - Timestamp is set
     */
    @Test
    fun testBuildCompleteMetrics() {
        // Setup scenario: 10 detected, 8 created, 2 filtered
        repeat(10) { collector.onElementDetected() }
        repeat(8) { collector.onVUIDCreated() }

        mockNodeInfo.className = "android.widget.Button"
        mockNodeInfo.isClickable = false
        collector.onElementFiltered(mockNodeInfo, "Below threshold")
        collector.onElementFiltered(mockNodeInfo, "Below threshold")

        // Build metrics
        val metrics = collector.buildMetrics("com.example.testapp")

        // Verify
        assertEquals("Package name should match", "com.example.testapp", metrics.packageName)
        assertEquals("Elements detected should be 10", 10, metrics.elementsDetected)
        assertEquals("VUIDs created should be 8", 8, metrics.vuidsCreated)
        assertEquals("Creation rate should be 0.8", 0.8, metrics.creationRate, 0.001)
        assertEquals("Filtered count should be 2", 2, metrics.filteredCount)
        assertTrue("Timestamp should be recent", metrics.explorationTimestamp > System.currentTimeMillis() - 1000)
    }

    /**
     * Test 11: Perfect 100% metrics
     *
     * Validates:
     * - 100% creation rate when all elements get VUIDs
     * - No filtered elements
     */
    @Test
    fun testPerfect100PercentMetrics() {
        // All elements get VUIDs
        repeat(117) {
            collector.onElementDetected()
            collector.onVUIDCreated()
        }

        // Build metrics
        val metrics = collector.buildMetrics("com.ytheekshana.deviceinfo")

        // Verify
        assertEquals("Elements detected should be 117", 117, metrics.elementsDetected)
        assertEquals("VUIDs created should be 117", 117, metrics.vuidsCreated)
        assertEquals("Creation rate should be 1.0 (100%)", 1.0, metrics.creationRate, 0.001)
        assertEquals("Filtered count should be 0", 0, metrics.filteredCount)
        assertTrue("filteredByType should be empty", metrics.filteredByType.isEmpty())
        assertTrue("filterReasons should be empty", metrics.filterReasons.isEmpty())
    }

    /**
     * Test 12: Report string generation
     *
     * Validates:
     * - toReportString() generates readable output
     * - Contains key metrics
     */
    @Test
    fun testReportStringGeneration() {
        // Setup metrics
        repeat(10) { collector.onElementDetected() }
        repeat(9) { collector.onVUIDCreated() }

        mockNodeInfo.className = "android.widget.ImageView"
        mockNodeInfo.isClickable = false
        collector.onElementFiltered(mockNodeInfo, "Decorative")

        val metrics = collector.buildMetrics("com.example.app")

        // Generate report
        val report = metrics.toReportString()

        // Verify content
        assertTrue("Report should contain package name", report.contains("com.example.app"))
        assertTrue("Report should contain detected count", report.contains("10"))
        assertTrue("Report should contain created count", report.contains("9"))
        assertTrue("Report should contain rate", report.contains("90%"))
        assertTrue("Report should contain filtered count", report.contains("Filtered: 1"))
    }

    // ========== Thread Safety Tests ==========

    /**
     * Test 13: Concurrent element detection
     *
     * Validates:
     * - onElementDetected() is thread-safe
     * - No race conditions when called concurrently
     */
    @Test
    fun testConcurrentElementDetection() {
        val threadCount = 10
        val incrementsPerThread = 100
        val latch = CountDownLatch(threadCount)

        // Launch concurrent threads
        repeat(threadCount) {
            thread {
                repeat(incrementsPerThread) {
                    collector.onElementDetected()
                }
                latch.countDown()
            }
        }

        // Wait for completion
        assertTrue("Threads should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))

        // Verify
        val (detected, _, _) = collector.getCurrentStats()
        assertEquals("All increments should be counted", threadCount * incrementsPerThread, detected)
    }

    /**
     * Test 14: Concurrent VUID creation
     *
     * Validates:
     * - onVUIDCreated() is thread-safe
     * - No race conditions when called concurrently
     */
    @Test
    fun testConcurrentVUIDCreation() {
        val threadCount = 10
        val incrementsPerThread = 100
        val latch = CountDownLatch(threadCount)

        // Launch concurrent threads
        repeat(threadCount) {
            thread {
                repeat(incrementsPerThread) {
                    collector.onVUIDCreated()
                }
                latch.countDown()
            }
        }

        // Wait for completion
        assertTrue("Threads should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))

        // Verify
        val (_, created, _) = collector.getCurrentStats()
        assertEquals("All increments should be counted", threadCount * incrementsPerThread, created)
    }

    /**
     * Test 15: Concurrent mixed operations
     *
     * Validates:
     * - Mixed onElementDetected() and onVUIDCreated() calls are thread-safe
     * - Rate calculation remains consistent
     */
    @Test
    fun testConcurrentMixedOperations() {
        val threadCount = 20
        val operationsPerThread = 50
        val latch = CountDownLatch(threadCount)

        // Launch concurrent threads - half detect, half create
        repeat(threadCount / 2) {
            thread {
                repeat(operationsPerThread) {
                    collector.onElementDetected()
                }
                latch.countDown()
            }
            thread {
                repeat(operationsPerThread) {
                    collector.onVUIDCreated()
                }
                latch.countDown()
            }
        }

        // Wait for completion
        assertTrue("Threads should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))

        // Verify - should have equal detected and created
        val (detected, created, rate) = collector.getCurrentStats()
        assertEquals("Detected should match operations", threadCount / 2 * operationsPerThread, detected)
        assertEquals("Created should match operations", threadCount / 2 * operationsPerThread, created)
        assertEquals("Rate should be 1.0 (100%)", 1.0, rate, 0.001)
    }

    /**
     * Test 16: Concurrent element filtering
     *
     * Validates:
     * - onElementFiltered() with CopyOnWriteArrayList is thread-safe
     * - No ConcurrentModificationException
     */
    @Test
    fun testConcurrentElementFiltering() {
        val threadCount = 10
        val filtersPerThread = 50
        val latch = CountDownLatch(threadCount)

        // Launch concurrent threads
        repeat(threadCount) {
            thread {
                val node = AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    isClickable = false
                }
                try {
                    repeat(filtersPerThread) {
                        collector.onElementDetected()
                        collector.onElementFiltered(node, "Concurrent filter")
                    }
                } finally {
                    node.recycle()
                    latch.countDown()
                }
            }
        }

        // Wait for completion
        assertTrue("Threads should complete within 5 seconds", latch.await(5, TimeUnit.SECONDS))

        // Verify
        val metrics = collector.buildMetrics("com.example.app")
        assertEquals("All filters should be counted", threadCount * filtersPerThread, metrics.filteredCount)

        val report = collector.generateFilterReport()
        assertEquals("Report should have all filtered elements", threadCount * filtersPerThread, report.totalFiltered)
    }

    // ========== Filter Report Tests ==========

    /**
     * Test 17: Generate filter report
     *
     * Validates:
     * - Filter report contains all filtered elements
     * - Severity counts are correct
     */
    @Test
    fun testGenerateFilterReport() {
        // Create mixed severity scenario
        val clickable = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.Button"
            isClickable = true
        }
        val container = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.LinearLayout"
            isClickable = false
            isFocusable = true
        }
        val decorative = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.ImageView"
            isClickable = false
        }

        try {
            collector.onElementDetected()
            collector.onElementFiltered(clickable, "Test ERROR")

            collector.onElementDetected()
            collector.onElementFiltered(container, "Test WARNING")

            collector.onElementDetected()
            collector.onElementFiltered(decorative, "Test INTENDED")

            // Generate report
            val report = collector.generateFilterReport()

            // Verify
            assertEquals("Total filtered should be 3", 3, report.totalFiltered)
            assertEquals("Error count should be 1", 1, report.errorCount)
            assertEquals("Warning count should be 1", 1, report.warningCount)
            assertEquals("Intended count should be 1", 1, report.intendedCount)
            assertEquals("Elements list should have 3 items", 3, report.elements.size)

            // Verify element details
            val errorElement = report.elements.find { it.severity == FilterSeverity.ERROR }
            assertNotNull("Should have ERROR element", errorElement)
            assertEquals("ERROR element should be Button", "android.widget.Button", errorElement?.className)
        } finally {
            clickable.recycle()
            container.recycle()
            decorative.recycle()
        }
    }

    /**
     * Test 18: Filter report with no filters
     *
     * Validates:
     * - Empty report when no elements filtered
     */
    @Test
    fun testFilterReportWithNoFilters() {
        // No filtering, just detection and creation
        repeat(10) {
            collector.onElementDetected()
            collector.onVUIDCreated()
        }

        // Generate report
        val report = collector.generateFilterReport()

        // Verify
        assertEquals("Total filtered should be 0", 0, report.totalFiltered)
        assertEquals("Error count should be 0", 0, report.errorCount)
        assertEquals("Warning count should be 0", 0, report.warningCount)
        assertEquals("Intended count should be 0", 0, report.intendedCount)
        assertTrue("Elements list should be empty", report.elements.isEmpty())
    }

    // ========== Reset Functionality Tests ==========

    /**
     * Test 19: Reset clears all metrics
     *
     * Validates:
     * - reset() clears all counters
     * - reset() clears filtered elements
     * - Collector can be reused after reset
     */
    @Test
    fun testResetClearsAllMetrics() {
        // Setup initial state
        repeat(10) { collector.onElementDetected() }
        repeat(8) { collector.onVUIDCreated() }
        mockNodeInfo.className = "android.widget.Button"
        mockNodeInfo.isClickable = false
        collector.onElementFiltered(mockNodeInfo, "Test")

        // Verify non-zero state
        val (detected1, created1, _) = collector.getCurrentStats()
        assertTrue("Detected should be > 0 before reset", detected1 > 0)
        assertTrue("Created should be > 0 before reset", created1 > 0)

        // Reset
        collector.reset()

        // Verify cleared state
        val (detected2, created2, rate2) = collector.getCurrentStats()
        assertEquals("Detected should be 0 after reset", 0, detected2)
        assertEquals("Created should be 0 after reset", 0, created2)
        assertEquals("Rate should be 0.0 after reset", 0.0, rate2, 0.001)

        val metrics = collector.buildMetrics("com.example.app")
        assertEquals("Filtered count should be 0 after reset", 0, metrics.filteredCount)
        assertTrue("filteredByType should be empty after reset", metrics.filteredByType.isEmpty())
        assertTrue("filterReasons should be empty after reset", metrics.filterReasons.isEmpty())

        val report = collector.generateFilterReport()
        assertEquals("Filter report should be empty after reset", 0, report.totalFiltered)
    }

    /**
     * Test 20: Reuse collector after reset
     *
     * Validates:
     * - Collector can be reused for new exploration
     * - No state leakage from previous exploration
     */
    @Test
    fun testReuseCollectorAfterReset() {
        // First exploration
        repeat(5) {
            collector.onElementDetected()
            collector.onVUIDCreated()
        }
        val metrics1 = collector.buildMetrics("com.example.app1")
        assertEquals("First exploration should have 5 detected", 5, metrics1.elementsDetected)

        // Reset
        collector.reset()

        // Second exploration
        repeat(10) {
            collector.onElementDetected()
            collector.onVUIDCreated()
        }
        val metrics2 = collector.buildMetrics("com.example.app2")
        assertEquals("Second exploration should have 10 detected", 10, metrics2.elementsDetected)
        assertEquals("Second exploration should have 10 created", 10, metrics2.vuidsCreated)
        assertEquals("Second exploration should have correct package", "com.example.app2", metrics2.packageName)
    }

    // ========== Edge Cases Tests ==========

    /**
     * Test 21: Zero elements scenario
     *
     * Validates:
     * - Handles case when no elements detected
     * - No division by zero errors
     */
    @Test
    fun testZeroElementsScenario() {
        // Build metrics without any operations
        val metrics = collector.buildMetrics("com.example.empty")

        // Verify
        assertEquals("Elements detected should be 0", 0, metrics.elementsDetected)
        assertEquals("VUIDs created should be 0", 0, metrics.vuidsCreated)
        assertEquals("Creation rate should be 0.0", 0.0, metrics.creationRate, 0.001)
        assertEquals("Filtered count should be 0", 0, metrics.filteredCount)
    }

    /**
     * Test 22: More VUIDs than elements (should not happen in practice)
     *
     * Validates:
     * - Rate calculation handles edge case
     * - No overflow or unexpected behavior
     */
    @Test
    fun testMoreVUIDsThanElements() {
        // Simulate unusual scenario
        collector.onElementDetected()
        collector.onVUIDCreated()
        collector.onVUIDCreated() // Extra VUID (should not happen)

        val (detected, created, rate) = collector.getCurrentStats()
        assertEquals("Detected should be 1", 1, detected)
        assertEquals("Created should be 2", 2, created)
        assertEquals("Rate should be 2.0 (200%)", 2.0, rate, 0.001)
    }

    /**
     * Test 23: Null className handling
     *
     * Validates:
     * - Handles null className gracefully
     * - Uses "Unknown" placeholder
     */
    @Test
    fun testNullClassNameHandling() {
        val nullClassNode = AccessibilityNodeInfo.obtain().apply {
            className = null
            isClickable = false
        }

        try {
            collector.onElementDetected()
            collector.onElementFiltered(nullClassNode, "No class name")

            val metrics = collector.buildMetrics("com.example.app")

            // Verify "Unknown" is used
            assertEquals("Should have 'Unknown' type", 1, metrics.filteredByType["Unknown"])
        } finally {
            nullClassNode.recycle()
        }
    }

    /**
     * Test 24: Empty filter reason
     *
     * Validates:
     * - Handles empty reason string
     * - Still tracks the filter
     */
    @Test
    fun testEmptyFilterReason() {
        mockNodeInfo.className = "android.widget.Button"
        mockNodeInfo.isClickable = false

        collector.onElementDetected()
        collector.onElementFiltered(mockNodeInfo, "")

        val metrics = collector.buildMetrics("com.example.app")

        // Verify
        assertEquals("Should have 1 filtered", 1, metrics.filteredCount)
        assertEquals("Should track empty reason", 1, metrics.filterReasons[""])
    }

    /**
     * Test 25: Large volume stress test
     *
     * Validates:
     * - Handles large number of elements
     * - Performance remains acceptable
     */
    @Test
    fun testLargeVolumeStressTest() {
        val largeCount = 10000

        // Simulate large exploration
        repeat(largeCount) {
            collector.onElementDetected()
            if (it % 10 != 0) { // 90% creation rate
                collector.onVUIDCreated()
            } else {
                mockNodeInfo.className = "android.widget.Button"
                mockNodeInfo.isClickable = false
                collector.onElementFiltered(mockNodeInfo, "Filtered")
            }
        }

        // Build metrics
        val metrics = collector.buildMetrics("com.example.large")

        // Verify
        assertEquals("Should handle $largeCount elements", largeCount, metrics.elementsDetected)
        assertEquals("Should have correct creation count", 9000, metrics.vuidsCreated)
        assertEquals("Should have correct filter count", 1000, metrics.filteredCount)
        assertEquals("Should have correct rate", 0.9, metrics.creationRate, 0.001)
    }

    companion object {
        private const val TAG = "VUIDCreationMetricsCollectorTest"
    }
}
