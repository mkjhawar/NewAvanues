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
 * Comprehensive unit tests for ClickabilityDetector
 *
 * Coverage targets:
 * - All 5 signal detectors (isClickable, isFocusable, ACTION_CLICK, resourceId, container)
 * - Scoring algorithm accuracy
 * - Confidence level classification
 * - Edge cases (null values, exceptions, empty data)
 * - Performance requirements (<10ms per element)
 * - Container detection heuristics
 *
 * Test strategy:
 * - Mock AccessibilityNodeInfo for controlled testing
 * - Test each signal independently
 * - Test signal combinations
 * - Validate scoring thresholds
 * - Measure performance
 */
@RunWith(AndroidJUnit4::class)
class ClickabilityDetectorTest {

    private lateinit var context: Context
    private lateinit var detector: ClickabilityDetector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        detector = ClickabilityDetector(context)
    }

    // ===== SIGNAL 1: isClickable Flag Tests =====

    @Test
    fun `explicit isClickable true returns score 1_0`() {
        val element = mockNode(isClickable = true)

        val score = detector.calculateScore(element)

        assertEquals(1.0, score.score, 0.001)
        assertEquals(ClickabilityConfidence.EXPLICIT, score.confidence)
        assertTrue(score.reasons.contains("isClickable=true"))
        assertTrue(score.shouldCreateVUID())
    }

    @Test
    fun `explicit isClickable true for any element type`() {
        val types = listOf(
            "android.widget.LinearLayout",
            "android.widget.Button",
            "androidx.cardview.widget.CardView",
            "android.widget.TextView",
            "android.widget.ImageView"
        )

        types.forEach { className ->
            val element = mockNode(
                className = className,
                isClickable = true
            )

            val score = detector.calculateScore(element)

            assertEquals("Failed for $className", 1.0, score.score, 0.001)
            assertEquals("Failed for $className", ClickabilityConfidence.EXPLICIT, score.confidence)
        }
    }

    // ===== SIGNAL 2: isFocusable Flag Tests =====

    @Test
    fun `isFocusable true adds 0_3 to score`() {
        val element = mockNode(
            isClickable = false,
            isFocusable = true
        )

        val score = detector.calculateScore(element)

        assertEquals(0.3, score.score, 0.001)
        assertTrue(score.reasons.any { it.contains("isFocusable") })
    }

    @Test
    fun `isFocusable false contributes nothing to score`() {
        val element = mockNode(
            isClickable = false,
            isFocusable = false
        )

        val score = detector.calculateScore(element)

        assertFalse(score.reasons.any { it.contains("isFocusable") })
    }

    // ===== SIGNAL 3: ACTION_CLICK Tests =====

    @Test
    fun `ACTION_CLICK present adds 0_4 to score`() {
        val element = mockNode(
            isClickable = false,
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click"))
        )

        val score = detector.calculateScore(element)

        assertEquals(0.4, score.score, 0.001)
        assertTrue(score.reasons.any { it.contains("hasClickAction") })
    }

    @Test
    fun `multiple actions including ACTION_CLICK detected`() {
        val element = mockNode(
            isClickable = false,
            actions = listOf(
                AccessibilityAction(AccessibilityNodeInfo.ACTION_FOCUS, "Focus"),
                AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click"),
                AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, "Long Click")
            )
        )

        val score = detector.calculateScore(element)

        assertTrue(score.score >= 0.4)
        assertTrue(score.reasons.any { it.contains("hasClickAction") })
    }

    @Test
    fun `no ACTION_CLICK contributes nothing`() {
        val element = mockNode(
            isClickable = false,
            actions = listOf(
                AccessibilityAction(AccessibilityNodeInfo.ACTION_FOCUS, "Focus")
            )
        )

        val score = detector.calculateScore(element)

        assertFalse(score.reasons.any { it.contains("hasClickAction") })
    }

    @Test
    fun `null action list handled gracefully`() {
        val element = mockNode(
            isClickable = false,
            actions = null
        )

        val score = detector.calculateScore(element)

        // Should not crash, score should be 0
        assertEquals(0.0, score.score, 0.001)
    }

    // ===== SIGNAL 4: Resource ID Pattern Tests =====

    @Test
    fun `clickable resource id adds 0_2 to score`() {
        val clickableIds = listOf(
            "com.example:id/button_submit",
            "com.example:id/btn_ok",
            "com.example:id/tab_home",
            "com.example:id/card_item",
            "com.example:id/action_bar",
            "com.example:id/clickable_view"
        )

        clickableIds.forEach { resourceId ->
            val element = mockNode(
                isClickable = false,
                resourceId = resourceId
            )

            val score = detector.calculateScore(element)

            assertTrue(
                "Failed for $resourceId: score=${score.score}",
                score.score >= 0.2
            )
            assertTrue(
                "Failed for $resourceId",
                score.reasons.any { it.contains("clickableResourceId") }
            )
        }
    }

    @Test
    fun `non-clickable resource id contributes nothing`() {
        val nonClickableIds = listOf(
            "com.example:id/text_label",
            "com.example:id/image_icon",
            "com.example:id/divider_line",
            "com.example:id/container_layout"
        )

        nonClickableIds.forEach { resourceId ->
            val element = mockNode(
                isClickable = false,
                resourceId = resourceId
            )

            val score = detector.calculateScore(element)

            assertFalse(
                "Incorrectly matched: $resourceId",
                score.reasons.any { it.contains("clickableResourceId") }
            )
        }
    }

    @Test
    fun `null or empty resource id handled gracefully`() {
        val element = mockNode(
            isClickable = false,
            resourceId = null
        )

        val score = detector.calculateScore(element)

        assertFalse(score.reasons.any { it.contains("clickableResourceId") })
    }

    // ===== SIGNAL 5: Clickable Container Tests =====

    @Test
    fun `LinearLayout with focusable detected as clickable container`() {
        val element = mockNode(
            className = "android.widget.LinearLayout",
            isClickable = false,
            isFocusable = true
        )

        val score = detector.calculateScore(element)

        // Should get: isFocusable (0.3) + clickableContainer (0.3) = 0.6
        assertTrue(score.score >= 0.5)
        assertTrue(score.reasons.any { it.contains("clickableContainer") })
    }

    @Test
    fun `CardView with click action detected as clickable container`() {
        val element = mockNode(
            className = "androidx.cardview.widget.CardView",
            isClickable = false,
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click"))
        )

        val score = detector.calculateScore(element)

        // Should get: hasClickAction (0.4) + clickableContainer (0.3) = 0.7
        assertTrue(score.score >= 0.5)
        assertTrue(score.reasons.any { it.contains("clickableContainer") })
    }

    @Test
    fun `FrameLayout with single clickable child detected as clickable container`() {
        val childElement = mockNode(isClickable = true)

        val element = mockNode(
            className = "android.widget.FrameLayout",
            isClickable = false,
            childCount = 1,
            children = listOf(childElement)
        )

        val score = detector.calculateScore(element)

        // Should get clickableContainer (0.3)
        assertTrue(score.score >= 0.3)
        assertTrue(score.reasons.any { it.contains("clickableContainer") })
    }

    @Test
    fun `non-container type not detected as clickable container`() {
        val element = mockNode(
            className = "android.widget.TextView",
            isClickable = false,
            isFocusable = true
        )

        val score = detector.calculateScore(element)

        // Should only get isFocusable (0.3), NOT clickableContainer
        assertEquals(0.3, score.score, 0.001)
        assertFalse(score.reasons.any { it.contains("clickableContainer") })
    }

    // ===== Combined Signals Tests =====

    @Test
    fun `combined signals exceed threshold`() {
        val element = mockNode(
            isClickable = false,
            isFocusable = true,  // +0.3
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")),  // +0.4
            resourceId = "com.example:id/card_item"  // +0.2
        )

        val score = detector.calculateScore(element)

        // Total: 0.3 + 0.4 + 0.2 = 0.9
        assertEquals(0.9, score.score, 0.001)
        assertEquals(ClickabilityConfidence.HIGH, score.confidence)
        assertTrue(score.shouldCreateVUID())
    }

    @Test
    fun `all signals combined achieve maximum score`() {
        val childElement = mockNode(isClickable = true)

        val element = mockNode(
            className = "android.widget.LinearLayout",
            isClickable = false,
            isFocusable = true,  // +0.3
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")),  // +0.4
            resourceId = "com.example:id/tab_home",  // +0.2
            childCount = 1,
            children = listOf(childElement)  // Enables clickableContainer (+0.3)
        )

        val score = detector.calculateScore(element)

        // Total: 0.3 + 0.4 + 0.2 + 0.3 = 1.2
        assertTrue(score.score >= 1.0)
        assertEquals(ClickabilityConfidence.HIGH, score.confidence)
    }

    // ===== Confidence Level Tests =====

    @Test
    fun `confidence EXPLICIT for isClickable true`() {
        val element = mockNode(isClickable = true)
        val score = detector.calculateScore(element)
        assertEquals(ClickabilityConfidence.EXPLICIT, score.confidence)
    }

    @Test
    fun `confidence HIGH for score 0_9`() {
        val element = mockNode(
            isClickable = false,
            isFocusable = true,  // +0.3
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")),  // +0.4
            resourceId = "com.example:id/button_ok"  // +0.2
        )

        val score = detector.calculateScore(element)

        assertEquals(0.9, score.score, 0.001)
        assertEquals(ClickabilityConfidence.HIGH, score.confidence)
    }

    @Test
    fun `confidence MEDIUM for score 0_7`() {
        val element = mockNode(
            className = "androidx.cardview.widget.CardView",
            isClickable = false,
            isFocusable = true,  // +0.3
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click"))  // +0.4
            // Container bonus: +0.3
        )

        val score = detector.calculateScore(element)

        assertTrue(score.score >= 0.7)
        assertEquals(ClickabilityConfidence.MEDIUM, score.confidence)
    }

    @Test
    fun `confidence LOW for score 0_5`() {
        val childElement = mockNode(isClickable = true)

        val element = mockNode(
            className = "android.widget.LinearLayout",
            isClickable = false,
            resourceId = "com.example:id/tab_item",  // +0.2
            childCount = 1,
            children = listOf(childElement)  // Enables clickableContainer (+0.3)
        )

        val score = detector.calculateScore(element)

        assertEquals(0.5, score.score, 0.001)
        assertEquals(ClickabilityConfidence.LOW, score.confidence)
    }

    @Test
    fun `confidence NONE for score below 0_5`() {
        val element = mockNode(
            isClickable = false,
            isFocusable = true  // +0.3 only
        )

        val score = detector.calculateScore(element)

        assertEquals(0.3, score.score, 0.001)
        assertEquals(ClickabilityConfidence.NONE, score.confidence)
        assertFalse(score.shouldCreateVUID())
    }

    // ===== Threshold Tests =====

    @Test
    fun `threshold 0_5 correctly filters elements`() {
        assertEquals(0.5, ClickabilityDetector.CLICKABILITY_THRESHOLD, 0.001)

        // Below threshold
        val belowThreshold = mockNode(
            isClickable = false,
            isFocusable = true  // 0.3 < 0.5
        )
        assertFalse(detector.calculateScore(belowThreshold).shouldCreateVUID())

        // At threshold
        val atThreshold = mockNode(
            isClickable = false,
            isFocusable = true,  // +0.3
            resourceId = "com.example:id/tab_item"  // +0.2 = 0.5
        )
        assertTrue(detector.calculateScore(atThreshold).shouldCreateVUID())

        // Above threshold
        val aboveThreshold = mockNode(
            isClickable = false,
            isFocusable = true,  // +0.3
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click"))  // +0.4 = 0.7
        )
        assertTrue(detector.calculateScore(aboveThreshold).shouldCreateVUID())
    }

    // ===== Edge Cases =====

    @Test
    fun `null className handled gracefully`() {
        val element = mockNode(
            className = null,
            isClickable = false
        )

        val score = detector.calculateScore(element)

        // Should not crash
        assertNotNull(score)
        assertEquals(ClickabilityConfidence.NONE, score.confidence)
    }

    @Test
    fun `empty element with no signals scores zero`() {
        val element = mockNode(
            className = "android.widget.View",
            isClickable = false,
            isFocusable = false,
            actions = emptyList(),
            resourceId = null
        )

        val score = detector.calculateScore(element)

        assertEquals(0.0, score.score, 0.001)
        assertEquals(ClickabilityConfidence.NONE, score.confidence)
        assertFalse(score.shouldCreateVUID())
    }

    // ===== Performance Tests =====

    @Test
    fun `score calculation completes in under 10ms`() {
        val element = mockNode(
            className = "androidx.cardview.widget.CardView",
            isClickable = false,
            isFocusable = true,
            actions = listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")),
            resourceId = "com.example:id/card_item"
        )

        val score = detector.calculateScore(element)

        assertTrue(
            "Computation time ${score.computationTimeMs}ms exceeds 10ms limit",
            score.computationTimeMs < 10.0
        )
    }

    @Test
    fun `explicit isClickable fast path under 1ms`() {
        val element = mockNode(isClickable = true)

        val score = detector.calculateScore(element)

        // Fast path should be VERY fast (<1ms)
        assertTrue(
            "Fast path time ${score.computationTimeMs}ms exceeds 1ms",
            score.computationTimeMs < 1.0
        )
    }

    @Test
    fun `performance benchmark 100 elements under 1 second`() {
        val elements = (1..100).map { i ->
            mockNode(
                className = "android.widget.LinearLayout",
                isClickable = false,
                isFocusable = i % 2 == 0,
                actions = if (i % 3 == 0) listOf(AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "Click")) else null,
                resourceId = if (i % 4 == 0) "com.example:id/button_$i" else null
            )
        }

        val startTime = System.currentTimeMillis()
        elements.forEach { element ->
            detector.calculateScore(element)
        }
        val totalTime = System.currentTimeMillis() - startTime

        assertTrue(
            "100 elements took ${totalTime}ms (>1000ms)",
            totalTime < 1000
        )

        val avgTimePerElement = totalTime / 100.0
        assertTrue(
            "Average time per element ${avgTimePerElement}ms exceeds 10ms",
            avgTimePerElement < 10.0
        )
    }

    // ===== Helper Methods =====

    /**
     * Create mock AccessibilityNodeInfo with specified properties
     */
    private fun mockNode(
        className: String? = "android.widget.View",
        isClickable: Boolean = false,
        isFocusable: Boolean = false,
        actions: List<AccessibilityAction>? = null,
        resourceId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        childCount: Int = 0,
        children: List<AccessibilityNodeInfo> = emptyList()
    ): AccessibilityNodeInfo {
        val node = mock(AccessibilityNodeInfo::class.java)

        `when`(node.className).thenReturn(className)
        `when`(node.isClickable).thenReturn(isClickable)
        `when`(node.isFocusable).thenReturn(isFocusable)
        `when`(node.actionList).thenReturn(actions)
        `when`(node.viewIdResourceName).thenReturn(resourceId)
        `when`(node.text).thenReturn(text?.let { android.text.SpannableString(it) })
        `when`(node.contentDescription).thenReturn(contentDescription)
        `when`(node.childCount).thenReturn(childCount)

        children.forEachIndexed { index, child ->
            `when`(node.getChild(index)).thenReturn(child)
        }

        return node
    }
}
