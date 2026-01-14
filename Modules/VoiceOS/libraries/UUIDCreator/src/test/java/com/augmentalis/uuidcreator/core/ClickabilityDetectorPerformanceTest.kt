package com.augmentalis.uuidcreator.core

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.junit.Assert.*
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction

/**
 * Performance benchmark tests for ClickabilityDetector
 *
 * Verifies that scoring algorithm meets performance requirements:
 * - Single element: <10ms per element
 * - Batch of 100 elements: <1 second total
 * - Fast path (isClickable=true): <1ms per element
 *
 * ## Methodology
 * - Use System.nanoTime() for precise measurement
 * - Test multiple scenarios (explicit, multi-signal, no signals)
 * - Measure p50, p95, p99 percentiles
 * - Verify no performance regressions
 *
 * ## Acceptance Criteria (from spec)
 * - VUID creation overhead: <50ms per element
 * - Clickability scoring: <10ms per element
 * - Total exploration time increase: <10%
 *
 * @since 2025-12-08 (Phase 2: Smart Detection)
 */
@RunWith(AndroidJUnit4::class)
class ClickabilityDetectorPerformanceTest {

    private lateinit var context: Context
    private lateinit var detector: ClickabilityDetector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        detector = ClickabilityDetector(context)
    }

    // ===== Single Element Performance Tests =====

    @Test
    fun `single element explicit clickable under 1ms`() {
        val element = mockNode(isClickable = true)

        // Warmup (JIT compilation)
        repeat(10) {
            detector.calculateScore(element)
        }

        // Measure
        val measurements = mutableListOf<Long>()
        repeat(100) {
            val startTime = System.nanoTime()
            detector.calculateScore(element)
            val duration = System.nanoTime() - startTime
            measurements.add(duration)
        }

        val avgNs = measurements.average()
        val avgMs = avgNs / 1_000_000.0

        assertTrue(
            "Average fast path time ${avgMs}ms exceeds 1ms (measured: $avgMs ms)",
            avgMs < 1.0
        )

        // Also check p99 (99th percentile)
        val p99Ns = measurements.sorted()[99]
        val p99Ms = p99Ns / 1_000_000.0

        assertTrue(
            "P99 fast path time ${p99Ms}ms exceeds 2ms",
            p99Ms < 2.0
        )
    }

    @Test
    fun `single element multi-signal under 10ms`() {
        val element = mockNode(
            className = "androidx.cardview.widget.CardView",
            isClickable = false,
            isFocusable = true,
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")),
            resourceId = "com.example:id/card_item"
        )

        // Warmup
        repeat(10) {
            detector.calculateScore(element)
        }

        // Measure
        val measurements = mutableListOf<Long>()
        repeat(100) {
            val startTime = System.nanoTime()
            detector.calculateScore(element)
            val duration = System.nanoTime() - startTime
            measurements.add(duration)
        }

        val avgNs = measurements.average()
        val avgMs = avgNs / 1_000_000.0

        assertTrue(
            "Average multi-signal time ${avgMs}ms exceeds 10ms",
            avgMs < 10.0
        )

        // Check p99
        val p99Ns = measurements.sorted()[99]
        val p99Ms = p99Ns / 1_000_000.0

        assertTrue(
            "P99 multi-signal time ${p99Ms}ms exceeds 15ms",
            p99Ms < 15.0
        )
    }

    @Test
    fun `single element no signals under 5ms`() {
        val element = mockNode(
            className = "android.widget.View",
            isClickable = false,
            isFocusable = false,
            actions = emptyList(),
            resourceId = null
        )

        // Warmup
        repeat(10) {
            detector.calculateScore(element)
        }

        // Measure
        val measurements = mutableListOf<Long>()
        repeat(100) {
            val startTime = System.nanoTime()
            detector.calculateScore(element)
            val duration = System.nanoTime() - startTime
            measurements.add(duration)
        }

        val avgNs = measurements.average()
        val avgMs = avgNs / 1_000_000.0

        assertTrue(
            "Average no-signal time ${avgMs}ms exceeds 5ms",
            avgMs < 5.0
        )
    }

    // ===== Batch Performance Tests =====

    @Test
    fun `batch of 100 elements under 1 second`() {
        // Create diverse element types
        val elements = mutableListOf<AccessibilityNodeInfo>()

        // 20% explicit clickable (fast path)
        repeat(20) {
            elements.add(mockNode(isClickable = true))
        }

        // 30% multi-signal (containers with hints)
        repeat(30) {
            elements.add(mockNode(
                className = "android.widget.LinearLayout",
                isClickable = false,
                isFocusable = true,
                actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")),
                resourceId = "com.example:id/item_${elements.size}"
            ))
        }

        // 50% no signals (decorative)
        repeat(50) {
            elements.add(mockNode(
                className = "android.widget.ImageView",
                isClickable = false,
                isFocusable = false,
                actions = emptyList(),
                resourceId = null
            ))
        }

        // Warmup
        elements.take(10).forEach {
            detector.calculateScore(it)
        }

        // Measure batch processing
        val startTime = System.currentTimeMillis()
        elements.forEach { element ->
            detector.calculateScore(element)
        }
        val totalTime = System.currentTimeMillis() - startTime

        assertTrue(
            "Batch of 100 elements took ${totalTime}ms (exceeds 1000ms)",
            totalTime < 1000
        )

        // Calculate average per element
        val avgPerElement = totalTime / 100.0
        assertTrue(
            "Average per element ${avgPerElement}ms exceeds 10ms",
            avgPerElement < 10.0
        )
    }

    @Test
    fun `batch of 500 elements under 5 seconds`() {
        // Simulate large app (500 elements)
        val elements = (1..500).map { i ->
            when {
                i % 5 == 0 -> mockNode(isClickable = true)  // 20% explicit
                i % 3 == 0 -> mockNode(  // 33% multi-signal
                    className = "androidx.cardview.widget.CardView",
                    isClickable = false,
                    isFocusable = true,
                    actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click"))
                )
                else -> mockNode(  // 47% decorative
                    className = "android.widget.ImageView",
                    isClickable = false
                )
            }
        }

        // Warmup
        elements.take(20).forEach {
            detector.calculateScore(it)
        }

        // Measure
        val startTime = System.currentTimeMillis()
        elements.forEach { element ->
            detector.calculateScore(element)
        }
        val totalTime = System.currentTimeMillis() - startTime

        assertTrue(
            "Batch of 500 elements took ${totalTime}ms (exceeds 5000ms)",
            totalTime < 5000
        )

        val avgPerElement = totalTime / 500.0
        assertTrue(
            "Average per element ${avgPerElement}ms exceeds 10ms",
            avgPerElement < 10.0
        )
    }

    // ===== Percentile Analysis Tests =====

    @Test
    fun `percentile analysis for 1000 elements`() {
        // Create 1000 diverse elements
        val elements = (1..1000).map { i ->
            when (i % 10) {
                0, 1 -> mockNode(isClickable = true)  // 20% fast path
                2, 3, 4 -> mockNode(  // 30% multi-signal
                    className = "android.widget.LinearLayout",
                    isClickable = false,
                    isFocusable = true,
                    actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")),
                    resourceId = "item_$i"
                )
                else -> mockNode(isClickable = false)  // 50% simple
            }
        }

        // Warmup
        elements.take(50).forEach {
            detector.calculateScore(it)
        }

        // Measure each element
        val measurements = elements.map { element ->
            val startTime = System.nanoTime()
            detector.calculateScore(element)
            val duration = System.nanoTime() - startTime
            duration
        }.sorted()

        // Calculate percentiles
        val p50 = measurements[500] / 1_000_000.0  // Median
        val p95 = measurements[950] / 1_000_000.0  // 95th percentile
        val p99 = measurements[990] / 1_000_000.0  // 99th percentile
        val max = measurements.last() / 1_000_000.0

        // Log results
        println("Performance Percentiles (1000 elements):")
        println("  P50 (median): ${p50}ms")
        println("  P95: ${p95}ms")
        println("  P99: ${p99}ms")
        println("  Max: ${max}ms")

        // Verify requirements
        assertTrue("P50 ${p50}ms exceeds 5ms", p50 < 5.0)
        assertTrue("P95 ${p95}ms exceeds 10ms", p95 < 10.0)
        assertTrue("P99 ${p99}ms exceeds 15ms", p99 < 15.0)
        assertTrue("Max ${max}ms exceeds 20ms", max < 20.0)
    }

    // ===== Memory Performance Tests =====

    @Test
    fun `no memory leaks in batch processing`() {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        // Process 10,000 elements
        repeat(10_000) { i ->
            val element = mockNode(
                isClickable = i % 2 == 0,
                isFocusable = i % 3 == 0
            )
            detector.calculateScore(element)
        }

        // Force GC
        System.gc()
        Thread.sleep(100)

        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Memory increase should be minimal (<5MB)
        assertTrue(
            "Memory increased by ${memoryIncrease / 1024 / 1024}MB (exceeds 5MB)",
            memoryIncrease < 5 * 1024 * 1024
        )
    }

    // ===== Real-World Scenario Tests =====

    @Test
    fun `deviceInfo app simulation (117 elements)`() {
        // Simulate DeviceInfo app (117 clickable elements)
        // - 78 LinearLayout tabs
        // - 22 CardViews
        // - 5 Buttons
        // - 1 ImageButton
        // - 11 decorative elements

        val elements = mutableListOf<AccessibilityNodeInfo>()

        // 78 LinearLayout tabs (multi-signal)
        repeat(78) {
            elements.add(mockNode(
                className = "android.widget.LinearLayout",
                isClickable = false,
                isFocusable = true,
                resourceId = "tab_item_$it"
            ))
        }

        // 22 CardViews (multi-signal)
        repeat(22) {
            elements.add(mockNode(
                className = "androidx.cardview.widget.CardView",
                isClickable = false,
                isFocusable = true,
                actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click"))
            ))
        }

        // 5 Buttons (explicit)
        repeat(5) {
            elements.add(mockNode(
                className = "android.widget.Button",
                isClickable = true
            ))
        }

        // 1 ImageButton (explicit)
        elements.add(mockNode(
            className = "android.widget.ImageButton",
            isClickable = true
        ))

        // 11 decorative elements
        repeat(11) {
            elements.add(mockNode(
                className = "android.widget.ImageView",
                isClickable = false
            ))
        }

        // Warmup
        elements.take(10).forEach {
            detector.calculateScore(it)
        }

        // Measure
        val startTime = System.currentTimeMillis()
        val scores = elements.map { element ->
            detector.calculateScore(element)
        }
        val totalTime = System.currentTimeMillis() - startTime

        // Verify performance
        assertTrue(
            "DeviceInfo simulation (117 elements) took ${totalTime}ms (exceeds 1000ms)",
            totalTime < 1000
        )

        val avgPerElement = totalTime / 117.0
        assertTrue(
            "Average per element ${avgPerElement}ms exceeds 10ms",
            avgPerElement < 10.0
        )

        // Verify VUID creation rate
        val shouldCreateCount = scores.count { it.shouldCreateVUID() }
        val creationRate = shouldCreateCount.toDouble() / scores.size

        // Should create VUIDs for most elements (95%+)
        assertTrue(
            "Creation rate ${creationRate * 100}% below 90%",
            creationRate >= 0.90
        )
    }

    // ===== Helper Methods =====

    private fun mockNode(
        className: String? = "android.widget.View",
        isClickable: Boolean = false,
        isFocusable: Boolean = false,
        actions: List<AccessibilityAction>? = null,
        resourceId: String? = null,
        text: String? = null,
        contentDescription: String? = null
    ): AccessibilityNodeInfo {
        val node = mock(AccessibilityNodeInfo::class.java)

        `when`(node.className).thenReturn(className)
        `when`(node.isClickable).thenReturn(isClickable)
        `when`(node.isFocusable).thenReturn(isFocusable)
        `when`(node.actionList).thenReturn(actions)
        `when`(node.viewIdResourceName).thenReturn(resourceId)
        `when`(node.text).thenReturn(text?.let { android.text.SpannableString(it) })
        `when`(node.contentDescription).thenReturn(contentDescription)
        `when`(node.childCount).thenReturn(0)

        return node
    }
}
