package com.augmentalis.voiceoscoreng.extraction

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Tests for ExtractionBundle JavaScript loading and validation.
 */
class ExtractionBundleTest {

    @Test
    fun `ELEMENT_EXTRACTOR_JS is not empty`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.isNotBlank())
    }

    @Test
    fun `script contains extractElements function`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.contains("extractElements"))
    }

    @Test
    fun `script contains JSON stringify`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.contains("JSON.stringify"))
    }

    @Test
    fun `script contains interactive selectors`() {
        val script = ExtractionBundle.ELEMENT_EXTRACTOR_JS
        assertTrue(script.contains("button"))
        assertTrue(script.contains("input"))
        assertTrue(script.contains("role"))
    }

    @Test
    fun `script is wrapped in IIFE`() {
        val script = ExtractionBundle.ELEMENT_EXTRACTOR_JS
        assertTrue(script.trimStart().startsWith("(function()"))
        assertTrue(script.trimEnd().endsWith("();"))
    }

    @Test
    fun `isScriptValid returns true for valid script`() {
        assertTrue(ExtractionBundle.isScriptValid())
    }

    @Test
    fun `getScriptSize returns positive value`() {
        assertTrue(ExtractionBundle.getScriptSize() > 0)
    }

    @Test
    fun `script size is reasonable for minified JS`() {
        // Should be less than 10KB for a simple extraction script
        assertTrue(ExtractionBundle.getScriptSize() < 10_000)
    }

    @Test
    fun `script handles visibility check`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.contains("isVisible"))
    }

    @Test
    fun `script handles bounds formatting`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.contains("formatBounds"))
    }

    @Test
    fun `script handles deduplication`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.contains("isDuplicate"))
    }

    @Test
    fun `script returns metadata with URL and title`() {
        val script = ExtractionBundle.ELEMENT_EXTRACTOR_JS
        assertTrue(script.contains("window.location.href"))
        assertTrue(script.contains("document.title"))
    }

    @Test
    fun `script extracts aria-label for accessibility`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.contains("aria-label"))
    }

    @Test
    fun `script handles disabled elements`() {
        assertTrue(ExtractionBundle.ELEMENT_EXTRACTOR_JS.contains("disabled"))
    }
}
