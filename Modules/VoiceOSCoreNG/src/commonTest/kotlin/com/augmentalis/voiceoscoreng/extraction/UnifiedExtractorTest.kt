package com.augmentalis.voiceoscoreng.extraction

import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Tests for UnifiedExtractor and ExtractionResult.
 */
class UnifiedExtractorTest {

    // ==================== ExtractionResult Tests ====================

    @Test
    fun `ExtractionResult empty creates empty result`() {
        val result = ExtractionResult.empty(ExtractionSource.ACCESSIBILITY)
        assertTrue(result.elements.isEmpty())
        assertEquals(ExtractionSource.ACCESSIBILITY, result.source)
        assertTrue(result.isSuccess)
        assertEquals(0, result.elementCount)
    }

    @Test
    fun `ExtractionResult error creates error result`() {
        val result = ExtractionResult.error("Test error", ExtractionSource.WEBVIEW_JS)
        assertTrue(result.elements.isEmpty())
        assertEquals(ExtractionSource.WEBVIEW_JS, result.source)
        assertFalse(result.isSuccess)
        assertEquals("Test error", result.errorMessage)
    }

    @Test
    fun `ExtractionResult with elements is successful`() {
        val elements = listOf(
            ElementInfo.button(text = "Submit"),
            ElementInfo.input(resourceId = "email")
        )
        val result = ExtractionResult(elements, ExtractionSource.HTML_PARSER)

        assertTrue(result.isSuccess)
        assertEquals(2, result.elementCount)
        assertEquals(ExtractionSource.HTML_PARSER, result.source)
    }

    // ==================== ExtractionSource Tests ====================

    @Test
    fun `ExtractionSource enum has all expected values`() {
        val sources = ExtractionSource.entries
        assertTrue(sources.contains(ExtractionSource.ACCESSIBILITY))
        assertTrue(sources.contains(ExtractionSource.WEBVIEW_JS))
        assertTrue(sources.contains(ExtractionSource.CDP))
        assertTrue(sources.contains(ExtractionSource.HTML_PARSER))
        assertTrue(sources.contains(ExtractionSource.NONE))
    }

    // ==================== UnifiedExtractor.extractFromHtml Tests ====================

    @Test
    fun `extractFromHtml parses valid HTML`() {
        val html = """<button id="test">Click me</button>"""
        val result = UnifiedExtractor.extractFromHtml(html)

        assertTrue(result.isSuccess)
        assertEquals(ExtractionSource.HTML_PARSER, result.source)
        assertEquals(1, result.elementCount)
    }

    @Test
    fun `extractFromHtml handles empty HTML`() {
        val result = UnifiedExtractor.extractFromHtml("")
        assertTrue(result.isSuccess)
        assertEquals(0, result.elementCount)
    }

    @Test
    fun `extractFromHtml handles malformed HTML`() {
        val result = UnifiedExtractor.extractFromHtml("<button>Unclosed")
        assertTrue(result.isSuccess) // Should not crash
        assertNotNull(result.elements)
    }

    // ==================== UnifiedExtractor.extractFromAccessibilityJson Tests ====================

    @Test
    fun `extractFromAccessibilityJson parses valid JSON`() {
        val json = """
            {
                "elements": [
                    {
                        "className": "android.widget.Button",
                        "text": "Submit",
                        "resourceId": "submit_btn",
                        "bounds": "0,0,100,50",
                        "clickable": true,
                        "enabled": true
                    }
                ]
            }
        """.trimIndent()

        val result = UnifiedExtractor.extractFromAccessibilityJson(json)

        assertTrue(result.isSuccess)
        assertEquals(ExtractionSource.ACCESSIBILITY, result.source)
        assertEquals(1, result.elementCount)
    }

    @Test
    fun `extractFromAccessibilityJson handles empty array`() {
        val json = """{"elements": []}"""
        val result = UnifiedExtractor.extractFromAccessibilityJson(json)

        assertTrue(result.isSuccess)
        assertEquals(0, result.elementCount)
    }

    @Test
    fun `extractFromAccessibilityJson handles invalid JSON`() {
        val result = UnifiedExtractor.extractFromAccessibilityJson("not valid json")
        assertTrue(result.isSuccess) // ElementParser returns empty on error
        assertEquals(0, result.elementCount)
    }

    // ==================== UnifiedExtractor.getAvailableSources Tests ====================

    @Test
    fun `getAvailableSources includes HTML_PARSER as always available`() {
        val sources = UnifiedExtractor.getAvailableSources()
        assertTrue(sources[ExtractionSource.HTML_PARSER] == true)
    }

    @Test
    fun `getAvailableSources returns map with expected keys`() {
        val sources = UnifiedExtractor.getAvailableSources()
        assertTrue(sources.containsKey(ExtractionSource.ACCESSIBILITY))
        assertTrue(sources.containsKey(ExtractionSource.WEBVIEW_JS))
        assertTrue(sources.containsKey(ExtractionSource.HTML_PARSER))
    }

    // ==================== Platform Function Tests ====================

    @Test
    fun `getPlatformName returns non-empty string`() {
        val platform = getPlatformName()
        assertTrue(platform.isNotBlank())
    }

    @Test
    fun `getPlatformName returns expected values`() {
        val platform = getPlatformName()
        assertTrue(platform in listOf("android", "ios", "desktop"))
    }
}
