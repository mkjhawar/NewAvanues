package com.augmentalis.voiceoscoreng.extraction

import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * TDD Tests for ElementParser
 *
 * Tests HTML and accessibility tree parsing into ElementInfo objects.
 */
class ElementParserTest {

    // ==================== HTML Parsing Tests ====================

    @Test
    fun `parseHtml extracts button elements`() {
        val html = """
            <button id="submit-btn" class="primary">Submit</button>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)

        assertEquals(1, elements.size)
        val btn = elements.first()
        assertEquals("button", btn.className.lowercase())
        assertEquals("Submit", btn.text)
        assertEquals("submit-btn", btn.resourceId)
        assertTrue(btn.isClickable)
    }

    @Test
    fun `parseHtml extracts input elements with aria-label`() {
        val html = """
            <input type="text" id="email" aria-label="Email address" placeholder="Enter email">
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)

        assertEquals(1, elements.size)
        val input = elements.first()
        assertEquals("input", input.className.lowercase())
        assertEquals("Email address", input.contentDescription)
        assertTrue(input.isClickable)
    }

    @Test
    fun `parseHtml extracts links with href`() {
        val html = """
            <a href="/settings" id="settings-link">Settings</a>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)

        assertEquals(1, elements.size)
        val link = elements.first()
        assertEquals("a", link.className.lowercase())
        assertEquals("Settings", link.text)
        assertTrue(link.isClickable)
    }

    @Test
    fun `parseHtml extracts multiple elements`() {
        val html = """
            <div>
                <button id="btn1">First</button>
                <button id="btn2">Second</button>
                <input id="input1" type="text">
            </div>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)

        assertEquals(3, elements.size)
    }

    @Test
    fun `parseHtml ignores non-interactive elements`() {
        val html = """
            <div>
                <span>Just text</span>
                <p>Paragraph</p>
                <button>Click me</button>
            </div>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)

        // Only button should be extracted
        assertEquals(1, elements.size)
        assertEquals("Click me", elements.first().text)
    }

    @Test
    fun `parseHtml extracts select elements`() {
        val html = """
            <select id="country" aria-label="Select country">
                <option value="us">United States</option>
                <option value="uk">United Kingdom</option>
            </select>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)

        assertEquals(1, elements.size)
        assertEquals("select", elements.first().className.lowercase())
    }

    @Test
    fun `parseHtml handles empty input`() {
        val elements = ElementParser.parseHtml("")
        assertTrue(elements.isEmpty())
    }

    @Test
    fun `parseHtml handles malformed HTML gracefully`() {
        val html = "<button>Unclosed"
        val elements = ElementParser.parseHtml(html)
        // Should not crash, may return partial results
        assertNotNull(elements)
    }

    // ==================== Accessibility Tree Parsing Tests ====================

    @Test
    fun `parseAccessibilityJson extracts elements from JSON`() {
        val json = """
            {
                "elements": [
                    {
                        "className": "android.widget.Button",
                        "text": "Submit",
                        "resourceId": "com.app:id/submit",
                        "bounds": "0,0,100,50",
                        "clickable": true,
                        "enabled": true
                    }
                ]
            }
        """.trimIndent()

        val elements = ElementParser.parseAccessibilityJson(json)

        assertEquals(1, elements.size)
        assertEquals("android.widget.Button", elements.first().className)
        assertEquals("Submit", elements.first().text)
    }

    @Test
    fun `parseAccessibilityJson handles empty array`() {
        val json = """{"elements": []}"""
        val elements = ElementParser.parseAccessibilityJson(json)
        assertTrue(elements.isEmpty())
    }

    // ==================== XPath Generation Tests ====================

    @Test
    fun `generateXPath creates valid path for button with id`() {
        val element = ElementInfo.button(text = "Submit", resourceId = "submit-btn")
        val xpath = ElementParser.generateXPath(element)

        assertTrue(xpath.contains("button") || xpath.contains("@id"))
    }

    @Test
    fun `generateXPath creates path using text when no id`() {
        val element = ElementInfo.button(text = "Submit")
        val xpath = ElementParser.generateXPath(element)

        assertTrue(xpath.contains("Submit") || xpath.contains("text()"))
    }

    // ==================== Element Filtering Tests ====================

    @Test
    fun `filterActionable returns only clickable or scrollable elements`() {
        val elements = listOf(
            ElementInfo(className = "Button", isClickable = true),
            ElementInfo(className = "TextView", isClickable = false),
            ElementInfo(className = "ScrollView", isScrollable = true),
            ElementInfo(className = "ImageView", isClickable = false)
        )

        val actionable = ElementParser.filterActionable(elements)

        assertEquals(2, actionable.size)
        assertTrue(actionable.all { it.isClickable || it.isScrollable })
    }

    @Test
    fun `filterWithContent returns elements with voice labels`() {
        val elements = listOf(
            ElementInfo(className = "Button", text = "Submit"),
            ElementInfo(className = "Button", text = ""),
            ElementInfo(className = "Button", contentDescription = "Close"),
            ElementInfo(className = "Button")
        )

        val withContent = ElementParser.filterWithContent(elements)

        assertEquals(2, withContent.size)
    }

    // ==================== Deduplication Tests ====================

    @Test
    fun `deduplicate removes elements with same resourceId`() {
        val elements = listOf(
            ElementInfo(className = "Button", resourceId = "btn1", text = "First"),
            ElementInfo(className = "Button", resourceId = "btn1", text = "First Copy"),
            ElementInfo(className = "Button", resourceId = "btn2", text = "Second")
        )

        val unique = ElementParser.deduplicate(elements)

        assertEquals(2, unique.size)
    }

    @Test
    fun `deduplicate keeps elements with same text but different ids`() {
        val elements = listOf(
            ElementInfo(className = "Button", resourceId = "btn1", text = "Submit"),
            ElementInfo(className = "Button", resourceId = "btn2", text = "Submit")
        )

        val unique = ElementParser.deduplicate(elements)

        assertEquals(2, unique.size)
    }
}
