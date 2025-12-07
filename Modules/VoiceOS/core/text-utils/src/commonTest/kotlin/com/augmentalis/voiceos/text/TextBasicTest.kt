/**
 * TextBasicTest.kt - Basic tests for text utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.text

import kotlin.test.*

/**
 * Basic tests for TextSanitizers covering all actual methods
 */
class TextSanitizersBasicTest {

    @Test
    fun testSanitizeXPath_RemovesScriptTags() {
        val input = "//div[@id='test']<script>alert('XSS')</script>"
        val result = TextSanitizers.sanitizeXPath(input)
        assertFalse(result.contains("<script", ignoreCase = true))
        assertTrue(result.contains("//div[@id='test']"))
    }

    @Test
    fun testSanitizeXPath_RemovesEventHandlers() {
        val input = "//button[@onclick='alert(1)']"
        val result = TextSanitizers.sanitizeXPath(input)
        assertFalse(result.contains("onclick", ignoreCase = true))
    }

    @Test
    fun testSanitizeXPath_PreservesValidXPath() {
        val validPaths = listOf(
            "//div[@class='container']",
            "//*[@id='element-id']",
            "//button[contains(text(), 'Submit')]",
            "//input[@type='text' and @name='username']",
            "/html/body/div[1]/p[2]"
        )

        for (xpath in validPaths) {
            val result = TextSanitizers.sanitizeXPath(xpath)
            // Should preserve the basic structure
            assertTrue(result.contains("//") || result.contains("/"))
        }
    }

    @Test
    fun testEscapeForJavaScript_BasicEscaping() {
        assertEquals("\\\"", TextSanitizers.escapeForJavaScript("\""))
        assertEquals("\\'", TextSanitizers.escapeForJavaScript("'"))
        assertEquals("\\\\", TextSanitizers.escapeForJavaScript("\\"))
        assertEquals("\\n", TextSanitizers.escapeForJavaScript("\n"))
        assertEquals("\\r", TextSanitizers.escapeForJavaScript("\r"))
        assertEquals("\\t", TextSanitizers.escapeForJavaScript("\t"))
    }

    @Test
    fun testEscapeForJavaScript_ComplexString() {
        val input = "Hello \"World\"\nLine 2\t'End'"
        val expected = "Hello \\\"World\\\"\\nLine 2\\t\\'End\\'"
        assertEquals(expected, TextSanitizers.escapeForJavaScript(input))
    }

    @Test
    fun testIsJavaScriptSafe_SafeStrings() {
        assertTrue(TextSanitizers.isJavaScriptSafe("Hello World"))
        assertTrue(TextSanitizers.isJavaScriptSafe("test123"))
        assertTrue(TextSanitizers.isJavaScriptSafe("user@example.com"))
        assertTrue(TextSanitizers.isJavaScriptSafe("dash-underscore_dot."))
    }

    @Test
    fun testIsJavaScriptSafe_UnsafeStrings() {
        assertFalse(TextSanitizers.isJavaScriptSafe("<script>"))
        assertFalse(TextSanitizers.isJavaScriptSafe("';alert(1);//"))
        assertFalse(TextSanitizers.isJavaScriptSafe("\" onload=\""))
        assertFalse(TextSanitizers.isJavaScriptSafe("javascript:"))
        assertFalse(TextSanitizers.isJavaScriptSafe("eval("))
        assertFalse(TextSanitizers.isJavaScriptSafe("Function("))
    }

    @Test
    fun testEscapeHtml_BasicCharacters() {
        assertEquals("&lt;", TextSanitizers.escapeHtml("<"))
        assertEquals("&gt;", TextSanitizers.escapeHtml(">"))
        assertEquals("&amp;", TextSanitizers.escapeHtml("&"))
        assertEquals("&quot;", TextSanitizers.escapeHtml("\""))
        assertEquals("&#39;", TextSanitizers.escapeHtml("'"))
    }

    @Test
    fun testEscapeHtml_ComplexString() {
        val input = "<div class=\"container\">Hello & 'Goodbye'</div>"
        val expected = "&lt;div class=&quot;container&quot;&gt;Hello &amp; &#39;Goodbye&#39;&lt;/div&gt;"
        assertEquals(expected, TextSanitizers.escapeHtml(input))
    }

    @Test
    fun testStripHtmlTags_RemovesTags() {
        assertEquals("Hello World", TextSanitizers.stripHtmlTags("<p>Hello World</p>"))
        assertEquals("Click here", TextSanitizers.stripHtmlTags("<a href='#'>Click here</a>"))
        assertEquals("Bold Text", TextSanitizers.stripHtmlTags("<strong>Bold</strong> <em>Text</em>"))
    }

    @Test
    fun testStripHtmlTags_PreservesText() {
        val input = "<div><h1>Title</h1><p>Paragraph</p></div>"
        val result = TextSanitizers.stripHtmlTags(input)
        assertEquals("TitleParagraph", result)
        assertFalse(result.contains("<"))
        assertFalse(result.contains(">"))
    }

    @Test
    fun testStripHtmlTags_HandlesEmptyTags() {
        assertEquals("", TextSanitizers.stripHtmlTags("<div></div>"))
        assertEquals("Text", TextSanitizers.stripHtmlTags("<p>Text</p>"))
    }
}

/**
 * Basic tests for TextUtils covering all actual methods
 */
class TextUtilsBasicTest {

    @Test
    fun testTruncate_BasicTruncation() {
        val text = "This is a long text that needs to be truncated"
        val result = TextUtils.truncate(text, 20)
        assertTrue(result.length <= 23) // 20 + "..."
        assertTrue(result.endsWith("...") || result.length <= 20)
    }

    @Test
    fun testTruncate_ShortText() {
        val text = "Short"
        val result = TextUtils.truncate(text, 10)
        assertEquals("Short", result)
    }

    @Test
    fun testTruncate_ExactLength() {
        val text = "Exactly10!"
        val result = TextUtils.truncate(text, 10)
        assertEquals("Exactly10!", result)
    }

    @Test
    fun testTruncate_CustomSuffix() {
        val text = "This is a long text"
        val result = TextUtils.truncate(text, 10, "…")
        assertTrue(result.endsWith("…"))
        assertTrue(result.length <= 11)
    }

    @Test
    fun testCapitalizeWords_BasicCases() {
        assertEquals("Hello World", TextUtils.capitalizeWords("hello world"))
        assertEquals("The Quick Brown Fox", TextUtils.capitalizeWords("the quick brown fox"))
        assertEquals("Already Capitalized", TextUtils.capitalizeWords("Already Capitalized"))
    }

    @Test
    fun testCapitalizeWords_SingleWord() {
        assertEquals("Hello", TextUtils.capitalizeWords("hello"))
        assertEquals("World", TextUtils.capitalizeWords("WORLD"))
    }

    @Test
    fun testNormalizeWhitespace_ExtraSpaces() {
        assertEquals("Hello World", TextUtils.normalizeWhitespace("Hello    World"))
        assertEquals("One Two Three", TextUtils.normalizeWhitespace("One  Two   Three"))
    }

    @Test
    fun testNormalizeWhitespace_LeadingTrailing() {
        assertEquals("Trimmed", TextUtils.normalizeWhitespace("  Trimmed  "))
        assertEquals("Test String", TextUtils.normalizeWhitespace("   Test   String   "))
    }

    @Test
    fun testNormalizeWhitespace_NewlinesTabs() {
        assertEquals("Hello World", TextUtils.normalizeWhitespace("Hello\n\t\r\nWorld"))
    }

    @Test
    fun testIsAlphanumeric_Valid() {
        assertTrue(TextUtils.isAlphanumeric("Hello123"))
        assertTrue(TextUtils.isAlphanumeric("Test456"))
        assertTrue(TextUtils.isAlphanumeric("OnlyLetters"))
        assertTrue(TextUtils.isAlphanumeric("12345"))
    }

    @Test
    fun testIsAlphanumeric_Invalid() {
        assertFalse(TextUtils.isAlphanumeric("Hello World"))
        assertFalse(TextUtils.isAlphanumeric("test@example"))
        assertFalse(TextUtils.isAlphanumeric("under_score"))
        assertFalse(TextUtils.isAlphanumeric("dash-es"))
    }

    @Test
    fun testExtractNumbers_BasicCases() {
        assertEquals(listOf(123, 456), TextUtils.extractNumbers("abc123def456ghi"))
        assertEquals(listOf(42), TextUtils.extractNumbers("The answer is 42"))
        assertEquals(listOf(2025, 11, 17), TextUtils.extractNumbers("Date: 2025-11-17"))
    }

    @Test
    fun testExtractNumbers_NoNumbers() {
        assertEquals(emptyList(), TextUtils.extractNumbers("No numbers here"))
        assertEquals(emptyList(), TextUtils.extractNumbers(""))
    }

    @Test
    fun testWordCount_BasicCases() {
        assertEquals(3, TextUtils.wordCount("Hello World Test"))
        assertEquals(1, TextUtils.wordCount("Hello"))
        assertEquals(0, TextUtils.wordCount(""))
        assertEquals(0, TextUtils.wordCount("   "))
    }

    @Test
    fun testWordCount_ExtraSpaces() {
        assertEquals(3, TextUtils.wordCount("Hello    World    Test"))
        assertEquals(2, TextUtils.wordCount("  Leading  Trailing  "))
    }

    @Test
    fun testReplaceAll_BasicReplacement() {
        val text = "Hello World, Hello Universe"
        val replacements = mapOf("Hello" to "Hi", "World" to "Earth")
        val result = text.replaceAll(replacements)
        assertEquals("Hi Earth, Hi Universe", result)
    }

    @Test
    fun testIsValidEmail_ValidEmails() {
        assertTrue("test@example.com".isValidEmail())
        assertTrue("user.name@company.org".isValidEmail())
        assertTrue("admin+tag@sub.domain.co.uk".isValidEmail())
    }

    @Test
    fun testIsValidEmail_InvalidEmails() {
        assertFalse("notanemail".isValidEmail())
        assertFalse("@example.com".isValidEmail())
        assertFalse("user@".isValidEmail())
        assertFalse("user @example.com".isValidEmail())
    }

    @Test
    fun testIsValidPhone_ValidPhones() {
        assertTrue("555-123-4567".isValidPhone())
        assertTrue("(555) 123-4567".isValidPhone())
        assertTrue("+1-555-123-4567".isValidPhone())
        assertTrue("5551234567".isValidPhone())
    }

    @Test
    fun testIsValidPhone_InvalidPhones() {
        assertFalse("123".isValidPhone())
        assertFalse("not a phone".isValidPhone())
        assertFalse("555-CALL-NOW".isValidPhone())
    }
}

/**
 * Integration tests for text utilities
 */
class TextUtilsIntegrationBasicTest {

    @Test
    fun testEscapeAndStripHtml() {
        val maliciousInput = "<script>alert('XSS')</script>Hello \"World\""

        // Escape HTML
        val escaped = TextSanitizers.escapeHtml(maliciousInput)
        assertTrue(escaped.contains("&lt;script&gt;"))

        // Strip HTML tags
        val stripped = TextSanitizers.stripHtmlTags(maliciousInput)
        assertFalse(stripped.contains("<"))
        assertFalse(stripped.contains(">"))
    }

    @Test
    fun testTruncateNormalizedText() {
        val messyText = "This   is  a   very    messy    text   "

        // Normalize then truncate
        val normalized = TextUtils.normalizeWhitespace(messyText)
        assertEquals("This is a very messy text", normalized)

        val truncated = TextUtils.truncate(normalized, 15)
        assertTrue(truncated.length <= 18)
    }

    @Test
    fun testComplexSanitization() {
        val complexInput = """
            <div onclick='steal()'>
                <script>alert('XSS')</script>
                <p>Normal content</p>
                <iframe src='evil.com'></iframe>
            </div>
        """.trimIndent()

        // Strip all HTML
        val result = TextSanitizers.stripHtmlTags(complexInput)

        // Should remove all tags but preserve text content
        assertFalse(result.contains("<"))
        assertTrue(result.contains("Normal content"))
    }
}
