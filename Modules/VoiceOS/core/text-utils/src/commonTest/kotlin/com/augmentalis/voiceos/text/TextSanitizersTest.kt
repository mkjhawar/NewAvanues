/**
 * TextSanitizersTest.kt - Tests for text sanitization utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.text

import kotlin.test.*

class TextSanitizersTest {

    @Test
    fun testSanitizeXPathRemovesScriptTags() {
        val input = "//div[@id='test']<script>alert('xss')</script>"
        val result = TextSanitizers.sanitizeXPath(input)
        assertEquals("//div[@id='test']", result)
    }

    @Test
    fun testSanitizeXPathRemovesEventHandlers() {
        val input = "//div[@onclick='evil()']"
        val result = TextSanitizers.sanitizeXPath(input)
        assertEquals("//div[@'evil()']", result)
    }

    @Test
    fun testIsJavaScriptSafeDetectsDangerousPatterns() {
        assertFalse(TextSanitizers.isJavaScriptSafe("<script>"))
        assertFalse(TextSanitizers.isJavaScriptSafe("javascript:alert()"))
        assertFalse(TextSanitizers.isJavaScriptSafe("onerror=alert"))
        assertFalse(TextSanitizers.isJavaScriptSafe("eval(code)"))
        assertTrue(TextSanitizers.isJavaScriptSafe("normal text"))
    }

    @Test
    fun testEscapeForJavaScript() {
        val input = "Line 1\nLine 2\t'quoted' \"double\""
        val result = TextSanitizers.escapeForJavaScript(input)
        assertEquals("Line 1\\nLine 2\\t\\'quoted\\' \\\"double\\\"", result)
    }

    @Test
    fun testEscapeHtml() {
        val input = "<div class=\"test\">Content & 'more'</div>"
        val result = TextSanitizers.escapeHtml(input)
        assertEquals("&lt;div class=&quot;test&quot;&gt;Content &amp; &#39;more&#39;&lt;/div&gt;", result)
    }

    @Test
    fun testStripHtmlTags() {
        val input = "<p>Hello <b>World</b>!</p>"
        val result = TextSanitizers.stripHtmlTags(input)
        assertEquals("Hello World!", result)
    }
}