package com.augmentalis.voiceoscoreng.common
import com.augmentalis.voiceoscoreng.common.Bounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ElementInfoTest {

    // ==================== Bounds Tests ====================

    @Test
    fun `Bounds calculates width correctly`() {
        val bounds = Bounds(10, 20, 110, 70)
        assertEquals(100, bounds.width)
    }

    @Test
    fun `Bounds calculates height correctly`() {
        val bounds = Bounds(10, 20, 110, 70)
        assertEquals(50, bounds.height)
    }

    @Test
    fun `Bounds calculates centerX correctly`() {
        val bounds = Bounds(10, 20, 110, 70)
        assertEquals(60, bounds.centerX)
    }

    @Test
    fun `Bounds calculates centerY correctly`() {
        val bounds = Bounds(10, 20, 110, 70)
        assertEquals(45, bounds.centerY)
    }

    @Test
    fun `Bounds EMPTY has zero values`() {
        assertEquals(0, Bounds.EMPTY.left)
        assertEquals(0, Bounds.EMPTY.top)
        assertEquals(0, Bounds.EMPTY.right)
        assertEquals(0, Bounds.EMPTY.bottom)
    }

    @Test
    fun `Bounds fromString parses valid string`() {
        val bounds = Bounds.fromString("10,20,110,70")
        assertNotNull(bounds)
        assertEquals(10, bounds.left)
        assertEquals(20, bounds.top)
        assertEquals(110, bounds.right)
        assertEquals(70, bounds.bottom)
    }

    @Test
    fun `Bounds fromString returns null for invalid string`() {
        assertNull(Bounds.fromString("invalid"))
        assertNull(Bounds.fromString("10,20,30"))
        assertNull(Bounds.fromString(""))
    }

    @Test
    fun `Bounds toString formats correctly`() {
        val bounds = Bounds(10, 20, 110, 70)
        assertEquals("10,20,110,70", bounds.toString())
    }

    // ==================== ElementInfo Creation Tests ====================

    @Test
    fun `ElementInfo can be created with all fields`() {
        val bounds = Bounds(0, 0, 100, 50)
        val element = ElementInfo(
            className = "Button",
            resourceId = "com.app:id/submit",
            text = "Submit",
            contentDescription = "Submit form",
            bounds = bounds,
            isClickable = true,
            isScrollable = false,
            isEnabled = true,
            packageName = "com.example.app"
        )

        assertEquals("Button", element.className)
        assertEquals("com.app:id/submit", element.resourceId)
        assertEquals("Submit", element.text)
        assertEquals("Submit form", element.contentDescription)
        assertEquals(bounds, element.bounds)
        assertTrue(element.isClickable)
        assertFalse(element.isScrollable)
        assertTrue(element.isEnabled)
        assertEquals("com.example.app", element.packageName)
    }

    @Test
    fun `ElementInfo has correct default values`() {
        val element = ElementInfo(className = "View")

        assertEquals("View", element.className)
        assertEquals("", element.resourceId)
        assertEquals("", element.text)
        assertEquals("", element.contentDescription)
        assertEquals(Bounds.EMPTY, element.bounds)
        assertFalse(element.isClickable)
        assertFalse(element.isScrollable)
        assertTrue(element.isEnabled)
        assertEquals("", element.packageName)
    }

    @Test
    fun `ElementInfo EMPTY has correct defaults`() {
        val empty = ElementInfo.EMPTY
        assertEquals("", empty.className)
        assertFalse(empty.isClickable)
    }

    // ==================== Voice Label Tests ====================

    @Test
    fun `voiceLabel returns text when available`() {
        val element = ElementInfo(
            className = "Button",
            text = "Click Me",
            contentDescription = "A button",
            resourceId = "com.app:id/my_button"
        )
        assertEquals("Click Me", element.voiceLabel)
    }

    @Test
    fun `voiceLabel returns contentDescription when text is empty`() {
        val element = ElementInfo(
            className = "ImageButton",
            text = "",
            contentDescription = "Settings"
        )
        assertEquals("Settings", element.voiceLabel)
    }

    @Test
    fun `voiceLabel returns formatted resourceId when text and contentDescription empty`() {
        val element = ElementInfo(
            className = "Button",
            text = "",
            contentDescription = "",
            resourceId = "com.app:id/submit_button"
        )
        assertEquals("submit button", element.voiceLabel)
    }

    @Test
    fun `voiceLabel returns className when all else empty`() {
        val element = ElementInfo(className = "android.widget.Button")
        assertEquals("Button", element.voiceLabel)
    }

    // ==================== Voice Content Tests ====================

    @Test
    fun `hasVoiceContent returns true when text present`() {
        val element = ElementInfo(className = "Button", text = "OK")
        assertTrue(element.hasVoiceContent)
    }

    @Test
    fun `hasVoiceContent returns true when contentDescription present`() {
        val element = ElementInfo(className = "ImageView", contentDescription = "Profile picture")
        assertTrue(element.hasVoiceContent)
    }

    @Test
    fun `hasVoiceContent returns true when resourceId present`() {
        val element = ElementInfo(className = "View", resourceId = "com.app:id/header")
        assertTrue(element.hasVoiceContent)
    }

    @Test
    fun `hasVoiceContent returns false when all empty`() {
        val element = ElementInfo(className = "View")
        assertFalse(element.hasVoiceContent)
    }

    // ==================== Actionable Tests ====================

    @Test
    fun `isActionable returns true for clickable element`() {
        val element = ElementInfo(className = "Button", isClickable = true)
        assertTrue(element.isActionable)
    }

    @Test
    fun `isActionable returns true for scrollable element`() {
        val element = ElementInfo(className = "RecyclerView", isScrollable = true)
        assertTrue(element.isActionable)
    }

    @Test
    fun `isActionable returns false for non-interactive element`() {
        val element = ElementInfo(className = "TextView", isClickable = false, isScrollable = false)
        assertFalse(element.isActionable)
    }

    // ==================== Factory Methods Tests ====================

    @Test
    fun `button factory creates correct element`() {
        val button = ElementInfo.button(
            text = "Submit",
            resourceId = "com.app:id/submit",
            packageName = "com.example"
        )

        assertEquals("Button", button.className)
        assertEquals("Submit", button.text)
        assertTrue(button.isClickable)
        assertTrue(button.isEnabled)
    }

    @Test
    fun `input factory creates correct element`() {
        val input = ElementInfo.input(
            hint = "Enter email",
            resourceId = "com.app:id/email"
        )

        assertEquals("EditText", input.className)
        assertEquals("Enter email", input.contentDescription)
        assertTrue(input.isClickable)
    }

    // ==================== Equality and Copy Tests ====================

    @Test
    fun `ElementInfo equality works correctly`() {
        val element1 = ElementInfo(className = "Button", text = "OK")
        val element2 = ElementInfo(className = "Button", text = "OK")
        assertEquals(element1, element2)
    }

    @Test
    fun `ElementInfo copy preserves unchanged fields`() {
        val original = ElementInfo(
            className = "Button",
            text = "Submit",
            isClickable = true
        )
        val copied = original.copy(text = "Cancel")

        assertEquals("Button", copied.className)
        assertEquals("Cancel", copied.text)
        assertTrue(copied.isClickable)
    }
}
