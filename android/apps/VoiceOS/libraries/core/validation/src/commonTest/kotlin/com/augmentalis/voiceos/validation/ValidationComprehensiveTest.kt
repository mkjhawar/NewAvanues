/**
 * ValidationComprehensiveTest.kt - Comprehensive tests for validation utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.validation

import kotlin.test.*

/**
 * Comprehensive tests for SqlEscapeUtils
 */
class SqlEscapeUtilsTest {

    @Test
    fun testEscapeLikePattern_BasicInput() {
        val result = SqlEscapeUtils.escapeLikePattern("test")
        assertEquals("test", result)
    }

    @Test
    fun testEscapeLikePattern_WithPercent() {
        val result = SqlEscapeUtils.escapeLikePattern("100%")
        assertEquals("100\\%", result)
    }

    @Test
    fun testEscapeLikePattern_WithUnderscore() {
        val result = SqlEscapeUtils.escapeLikePattern("file_name")
        assertEquals("file\\_name", result)
    }

    @Test
    fun testEscapeLikePattern_WithBackslash() {
        val result = SqlEscapeUtils.escapeLikePattern("path\\file")
        assertEquals("path\\\\file", result)
    }

    @Test
    fun testEscapeLikePattern_AllSpecialChars() {
        val result = SqlEscapeUtils.escapeLikePattern("%_\\")
        assertEquals("\\%\\_\\\\", result)
    }

    @Test
    fun testEscapeLikePattern_EmptyString() {
        val result = SqlEscapeUtils.escapeLikePattern("")
        assertEquals("", result)
    }

    @Test
    fun testEscapeLikePattern_OnlySpecialChars() {
        val result = SqlEscapeUtils.escapeLikePattern("%%%")
        assertEquals("\\%\\%\\%", result)
    }

    @Test
    fun testEscapeLikePattern_MixedContent() {
        val result = SqlEscapeUtils.escapeLikePattern("user_100%_active")
        assertEquals("user\\_100\\%\\_active", result)
    }

    @Test
    fun testEscapeLikePattern_MultipleBackslashes() {
        val result = SqlEscapeUtils.escapeLikePattern("\\\\path\\\\file")
        assertEquals("\\\\\\\\path\\\\\\\\file", result)
    }

    @Test
    fun testEscapeLikePattern_UnicodeCharacters() {
        val result = SqlEscapeUtils.escapeLikePattern("用户_100%")
        assertEquals("用户\\_100\\%", result)
    }

    @Test
    fun testEscapeLikePattern_Spaces() {
        val result = SqlEscapeUtils.escapeLikePattern("hello world")
        assertEquals("hello world", result)
    }

    @Test
    fun testEscapeLikePattern_Numbers() {
        val result = SqlEscapeUtils.escapeLikePattern("12345")
        assertEquals("12345", result)
    }

    @Test
    fun testEscapeLikePattern_SpecialSqlChars() {
        // These chars are NOT LIKE wildcards and should not be escaped
        val result = SqlEscapeUtils.escapeLikePattern("'quoted'")
        assertEquals("'quoted'", result)
    }

    @Test
    fun testEscapeLikePattern_RealWorldExamples() {
        // File patterns
        assertEquals("\\%.txt", SqlEscapeUtils.escapeLikePattern("%.txt"))

        // User input with wildcards
        assertEquals("search\\_term", SqlEscapeUtils.escapeLikePattern("search_term"))

        // Paths
        assertEquals("C:\\\\Users\\\\file", SqlEscapeUtils.escapeLikePattern("C:\\Users\\file"))
    }
}

/**
 * Tests for SQL LIKE pattern usage
 */
class SqlLikePatternTest {

    @Test
    fun testBuildLikeStartsWith() {
        val input = "test%value"
        val escaped = SqlEscapeUtils.escapeLikePattern(input)
        val pattern = "$escaped%"

        assertEquals("test\\%value%", pattern)
    }

    @Test
    fun testBuildLikeEndsWith() {
        val input = "_test"
        val escaped = SqlEscapeUtils.escapeLikePattern(input)
        val pattern = "%$escaped"

        assertEquals("%\\_test", pattern)
    }

    @Test
    fun testBuildLikeContains() {
        val input = "100%"
        val escaped = SqlEscapeUtils.escapeLikePattern(input)
        val pattern = "%$escaped%"

        assertEquals("%100\\%%", pattern)
    }

    @Test
    fun testBuildExactMatch() {
        val input = "exact_match%"
        val escaped = SqlEscapeUtils.escapeLikePattern(input)

        assertEquals("exact\\_match\\%", escaped)
    }
}

/**
 * Edge case tests for validation
 */
class ValidationEdgeCaseTest {

    @Test
    fun testVeryLongInput() {
        val longInput = "a%_\\".repeat(1000)
        val result = SqlEscapeUtils.escapeLikePattern(longInput)

        // Should handle without error
        assertNotNull(result)
        assertTrue(result.length > longInput.length)
    }

    @Test
    fun testOnlyBackslashes() {
        val result = SqlEscapeUtils.escapeLikePattern("\\\\\\")
        assertEquals("\\\\\\\\\\\\", result)
    }

    @Test
    fun testAlternatingSpecialChars() {
        val result = SqlEscapeUtils.escapeLikePattern("%_%_%")
        assertEquals("\\%\\_\\%\\_\\%", result)
    }

    @Test
    fun testNewlinesAndTabs() {
        val result = SqlEscapeUtils.escapeLikePattern("line1\nline2\ttab")
        assertEquals("line1\nline2\ttab", result)
    }

    @Test
    fun testNullCharacter() {
        val result = SqlEscapeUtils.escapeLikePattern("null\u0000char")
        assertEquals("null\u0000char", result)
    }
}

/**
 * Security tests for SQL injection prevention
 */
class SqlInjectionPreventionTest {

    @Test
    fun testPreventWildcardInjection() {
        // User tries to match all rows
        val maliciousInput = "%"
        val escaped = SqlEscapeUtils.escapeLikePattern(maliciousInput)

        assertEquals("\\%", escaped)
        assertFalse(escaped == "%") // Should not match all
    }

    @Test
    fun testPreventUnderscoreInjection() {
        // User tries to match single character wildcard
        val maliciousInput = "_"
        val escaped = SqlEscapeUtils.escapeLikePattern(maliciousInput)

        assertEquals("\\_", escaped)
    }

    @Test
    fun testPreventComplexInjection() {
        // Complex pattern injection attempt
        val maliciousInput = "admin' OR '1'='1' --%"
        val escaped = SqlEscapeUtils.escapeLikePattern(maliciousInput)

        // Quotes are not LIKE wildcards, so they're not escaped
        // But % is escaped
        assertTrue(escaped.contains("\\%"))
        assertFalse(escaped.endsWith("%"))
    }

    @Test
    fun testSafeSearchPattern() {
        // User searches for literal "100%"
        val userInput = "100%"
        val escaped = SqlEscapeUtils.escapeLikePattern(userInput)
        val searchPattern = "%$escaped%"

        // Pattern should be: %100\%% (not %100%%)
        assertEquals("%100\\%%", searchPattern)
    }
}

/**
 * Integration tests for validation utilities
 */
class ValidationIntegrationTest {

    @Test
    fun testSearchFunctionality() {
        // Simulate search with user input
        fun buildSearchQuery(userInput: String): String {
            val escaped = SqlEscapeUtils.escapeLikePattern(userInput)
            return "SELECT * FROM items WHERE name LIKE '%$escaped%' ESCAPE '\\'"
        }

        val query = buildSearchQuery("100%")
        assertTrue(query.contains("100\\%"))
        assertTrue(query.contains("ESCAPE"))
    }

    @Test
    fun testFilePatternMatching() {
        // Match file names with literal wildcards
        fun buildFileQuery(pattern: String): String {
            val escaped = SqlEscapeUtils.escapeLikePattern(pattern)
            return "SELECT * FROM files WHERE name LIKE '$escaped' ESCAPE '\\'"
        }

        val query = buildFileQuery("*.txt")
        // * is not a SQL wildcard, so it stays as is
        assertEquals("SELECT * FROM files WHERE name LIKE '*.txt' ESCAPE '\\'", query)
    }

    @Test
    fun testMultipleEscapes() {
        val inputs = listOf("100%", "file_name", "path\\dir", "normal")

        inputs.forEach { input ->
            val escaped = SqlEscapeUtils.escapeLikePattern(input)
            assertNotNull(escaped)
        }
    }

    @Test
    fun testDeterministicEscaping() {
        val input = "test%_\\"
        val result1 = SqlEscapeUtils.escapeLikePattern(input)
        val result2 = SqlEscapeUtils.escapeLikePattern(input)

        assertEquals(result1, result2)
    }
}
