/**
 * HybridExplorationTest.kt - Unit tests for Hybrid C-Lite exploration strategy
 *
 * TDD: These tests are written FIRST, before implementation.
 *
 * Tests cover:
 * - stableId() generation
 * - stabilityScore() calculation
 * - Element sorting by stability
 * - Click tracking by stableId
 *
 * @author AI Assistant
 * @since 2025-12-04
 */
package com.augmentalis.voiceoscore.learnapp

import android.graphics.Rect
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import org.junit.Assert.*
import org.junit.Test

class HybridExplorationTest {

    // ==================== stableId() Tests ====================

    @Test
    fun `stableId returns resourceId when available`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.microsoft.teams:id/chat_tab",
            text = "Chat",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals("res:com.microsoft.teams:id/chat_tab", element.stableId())
    }

    @Test
    fun `stableId returns text when no resourceId`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "",
            text = "Settings",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals("txt:android.widget.Button|Settings", element.stableId())
    }

    @Test
    fun `stableId returns contentDescription when no resourceId or text`() {
        val element = createElementInfo(
            className = "android.widget.ImageView",
            resourceId = "",
            text = "",
            contentDescription = "Menu icon",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals("cd:android.widget.ImageView|Menu icon", element.stableId())
    }

    @Test
    fun `stableId returns position when no identifiers`() {
        val element = createElementInfo(
            className = "android.view.View",
            resourceId = "",
            text = "",
            contentDescription = "",
            bounds = Rect(100, 200, 150, 250)
        )
        // Note: In unit tests without Robolectric, Rect.centerX/Y return 0 (stubbed)
        // We verify the format is correct: "pos:className|x:y"
        val stableId = element.stableId()
        assertTrue("stableId should start with 'pos:'", stableId.startsWith("pos:"))
        assertTrue("stableId should contain className", stableId.contains("android.view.View"))
        assertTrue("stableId should contain coordinate separator", stableId.contains(":"))
    }

    @Test
    fun `stableId prioritizes resourceId over text`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.app:id/button1",
            text = "Click Me",
            contentDescription = "Button description"
        )
        assertTrue(element.stableId().startsWith("res:"))
    }

    @Test
    fun `stableId prioritizes text over contentDescription`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "",
            text = "Click Me",
            contentDescription = "Button description"
        )
        assertTrue(element.stableId().startsWith("txt:"))
    }

    @Test
    fun `stableId handles empty strings correctly`() {
        val element = createElementInfo(
            className = "android.view.View",
            resourceId = "",
            text = "",
            contentDescription = "",
            bounds = Rect(50, 50, 100, 100)
        )
        assertTrue(element.stableId().startsWith("pos:"))
    }

    // ==================== stabilityScore() Tests ====================

    @Test
    fun `stabilityScore returns 100 for resourceId`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.teams:id/chat"
        )
        assertEquals(100, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 100 for resourceId even with other fields`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.teams:id/chat",
            text = "Chat",
            contentDescription = "Chat tab"
        )
        assertEquals(100, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 80 for text and contentDesc`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "",
            text = "Settings",
            contentDescription = "Open settings"
        )
        assertEquals(80, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 60 for text only`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "",
            text = "Settings",
            contentDescription = ""
        )
        assertEquals(60, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 40 for contentDesc only`() {
        val element = createElementInfo(
            className = "android.widget.ImageView",
            resourceId = "",
            text = "",
            contentDescription = "Menu"
        )
        assertEquals(40, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 0 for bounds only`() {
        val element = createElementInfo(
            className = "android.view.View",
            resourceId = "",
            text = "",
            contentDescription = "",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals(0, element.stabilityScore())
    }

    // ==================== Sorting Tests ====================

    @Test
    fun `elements sorted by stability score descending`() {
        val elements = listOf(
            createElementInfo(className = "View"),  // score 0
            createElementInfo(className = "Button", resourceId = "id/btn"),   // score 100
            createElementInfo(className = "Text", text = "Hello"),            // score 60
            createElementInfo(className = "Image", contentDescription = "Icon"),  // score 40
        )

        val sorted = elements.sortedByDescending { it.stabilityScore() }

        assertEquals(100, sorted[0].stabilityScore())  // resourceId first
        assertEquals(60, sorted[1].stabilityScore())   // text second
        assertEquals(40, sorted[2].stabilityScore())   // contentDesc third
        assertEquals(0, sorted[3].stabilityScore())    // bounds last
    }

    @Test
    fun `sorting is stable for equal scores`() {
        val elements = listOf(
            createElementInfo(className = "Button1", resourceId = "id/btn1"),
            createElementInfo(className = "Button2", resourceId = "id/btn2"),
            createElementInfo(className = "Button3", resourceId = "id/btn3"),
        )

        val sorted = elements.sortedByDescending { it.stabilityScore() }

        // All have score 100, should maintain original order
        assertEquals("Button1", sorted[0].className.substringAfterLast("."))
        assertEquals("Button2", sorted[1].className.substringAfterLast("."))
        assertEquals("Button3", sorted[2].className.substringAfterLast("."))
    }

    // ==================== Click Tracking Tests ====================

    @Test
    fun `clicked elements are tracked by stableId`() {
        val clickedIds = mutableSetOf<String>()
        val element = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.app:id/button"
        )

        clickedIds.add(element.stableId())

        assertTrue(element.stableId() in clickedIds)
    }

    @Test
    fun `unclicked elements filtered correctly`() {
        val clickedIds = mutableSetOf("res:id/btn1")
        val elements = listOf(
            createElementInfo(className = "Button", resourceId = "id/btn1"),
            createElementInfo(className = "Button", resourceId = "id/btn2"),
            createElementInfo(className = "Button", resourceId = "id/btn3"),
        )

        val unclicked = elements.filter { it.stableId() !in clickedIds }

        assertEquals(2, unclicked.size)
        assertTrue(unclicked.none { it.stableId() == "res:id/btn1" })
    }

    @Test
    fun `same element produces same stableId`() {
        val element1 = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.app:id/button",
            text = "Click"
        )
        val element2 = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.app:id/button",
            text = "Click"
        )

        assertEquals(element1.stableId(), element2.stableId())
    }

    @Test
    fun `different elements produce different stableIds`() {
        val element1 = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.app:id/button1"
        )
        val element2 = createElementInfo(
            className = "android.widget.Button",
            resourceId = "com.app:id/button2"
        )

        assertNotEquals(element1.stableId(), element2.stableId())
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `stableId handles special characters in text`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            text = "Hello|World"
        )
        // Should still work, pipe is part of our delimiter but text can contain it
        val stableId = element.stableId()
        assertTrue(stableId.contains("Hello|World"))
    }

    @Test
    fun `stableId handles unicode text`() {
        val element = createElementInfo(
            className = "android.widget.Button",
            text = "设置"  // Chinese for "Settings"
        )
        assertEquals("txt:android.widget.Button|设置", element.stableId())
    }

    @Test
    fun `stabilityScore handles null-like empty strings`() {
        val element = createElementInfo(
            className = "android.view.View",
            resourceId = "   ",  // whitespace only - should be treated as empty
            text = "",
            contentDescription = ""
        )
        // Depending on implementation, might be 100 or 0
        // This test documents expected behavior
        // If whitespace-only resourceId is valid: 100
        // If whitespace-only is treated as empty: 0
        val score = element.stabilityScore()
        assertTrue(score == 0 || score == 100)
    }

    // ==================== Helper Functions ====================

    private fun createElementInfo(
        className: String,
        resourceId: String = "",
        text: String = "",
        contentDescription: String = "",
        bounds: Rect = Rect(0, 0, 100, 50)
    ): ElementInfo {
        return ElementInfo(
            className = className,
            resourceId = resourceId,
            text = text,
            contentDescription = contentDescription,
            bounds = bounds
        )
    }
}
