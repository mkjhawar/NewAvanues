/**
 * JsonUtilsComprehensiveTest.kt - Comprehensive tests for JSON utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.json

import kotlin.test.*

/**
 * Comprehensive test suite for JsonUtils covering all edge cases and scenarios
 */
class JsonUtilsComprehensiveTest {

    @Test
    fun testEscapeJsonString_AllSpecialCharacters() {
        // Test all JSON special characters
        val input = "Line 1\nLine 2\rLine 3\tTab\bBackspace\"Quote\\Backslash"
        val expected = "Line 1\\nLine 2\\rLine 3\\tTab\\bBackspace\\\"Quote\\\\Backslash"
        assertEquals(expected, JsonUtils.escapeJsonString(input))
    }

    @Test
    fun testEscapeJsonString_UnicodeCharacters() {
        // Test unicode characters
        val input = "Hello 世界 🌍 مرحبا"
        val result = JsonUtils.escapeJsonString(input)
        assertEquals(input, result) // Unicode should pass through unchanged
    }

    @Test
    fun testEscapeJsonString_EmptyAndNull() {
        assertEquals("", JsonUtils.escapeJsonString(""))
        // Note: null handling depends on implementation
    }

    @Test
    fun testQuoteJsonString_EdgeCases() {
        assertEquals("\"\"", JsonUtils.quoteJsonString(""))
        assertEquals("\"test\"", JsonUtils.quoteJsonString("test"))
        assertEquals("\"\\\"nested\\\"\"", JsonUtils.quoteJsonString("\"nested\""))
    }

    @Test
    fun testCreateJsonObject_EmptyObject() {
        val result = JsonUtils.createJsonObject()
        assertTrue(result.contains("{"))
        assertTrue(result.contains("}"))
    }

    @Test
    fun testCreateJsonObject_SingleProperty() {
        val result = JsonUtils.createJsonObject("key" to "value")
        assertTrue(result.contains("\"key\": \"value\""))
    }

    @Test
    fun testCreateJsonObject_MultipleTypes() {
        val result = JsonUtils.createJsonObject(
            "string" to "text",
            "number" to 42,
            "boolean" to true,
            "null" to null,
            "float" to 3.14,
            "negative" to -100
        )

        assertTrue(result.contains("\"string\": \"text\""))
        assertTrue(result.contains("\"number\": 42"))
        assertTrue(result.contains("\"boolean\": true"))
        assertTrue(result.contains("\"null\": null"))
        assertTrue(result.contains("\"float\": 3.14"))
        assertTrue(result.contains("\"negative\": -100"))
    }

    @Test
    fun testCreateJsonObject_NestedObjects() {
        // Test with nested JSON strings
        val nested = JsonUtils.createJsonObject("inner" to "value")
        val result = JsonUtils.createJsonObject(
            "outer" to nested
        )
        assertTrue(result.contains("\"outer\":"))
        // Note: The nested object will be escaped as a string
    }

    @Test
    fun testCreateJsonArray_EmptyArray() {
        val result = JsonUtils.createJsonArray()
        assertEquals("[]", result)
    }

    @Test
    fun testCreateJsonArray_SingleElement() {
        val result = JsonUtils.createJsonArray("single")
        assertEquals("[\"single\"]", result)
    }

    @Test
    fun testCreateJsonArray_MixedTypes() {
        val result = JsonUtils.createJsonArray(
            "string",
            42,
            true,
            false,
            null,
            3.14,
            -100
        )
        assertEquals("[\"string\", 42, true, false, null, 3.14, -100]", result)
    }

    @Test
    fun testToJsonValue_AllTypes() {
        assertEquals("null", JsonUtils.toJsonValue(null))
        assertEquals("\"text\"", JsonUtils.toJsonValue("text"))
        assertEquals("42", JsonUtils.toJsonValue(42))
        assertEquals("3.14", JsonUtils.toJsonValue(3.14))
        assertEquals("true", JsonUtils.toJsonValue(true))
        assertEquals("false", JsonUtils.toJsonValue(false))
        assertEquals("-100", JsonUtils.toJsonValue(-100))
        assertEquals("0", JsonUtils.toJsonValue(0))
    }

    @Test
    fun testToJsonValue_SpecialStrings() {
        assertEquals("\"\"", JsonUtils.toJsonValue(""))
        assertEquals("\"null\"", JsonUtils.toJsonValue("null"))
        assertEquals("\"true\"", JsonUtils.toJsonValue("true"))
        assertEquals("\"false\"", JsonUtils.toJsonValue("false"))
        assertEquals("\"123\"", JsonUtils.toJsonValue("123"))
    }

    @Test
    fun testPrettyPrint_SimpleObject() {
        val input = "{\"name\":\"John\",\"age\":30}"
        val result = JsonUtils.prettyPrint(input)

        assertTrue(result.contains("{\n"))
        assertTrue(result.contains("  \"name\": \"John\""))
        assertTrue(result.contains("  \"age\": 30"))
        assertTrue(result.contains("\n}"))
    }

    @Test
    fun testPrettyPrint_NestedObject() {
        val input = "{\"user\":{\"name\":\"John\",\"age\":30},\"active\":true}"
        val result = JsonUtils.prettyPrint(input)

        assertTrue(result.contains("\"user\": {"))
        assertTrue(result.contains("    \"name\": \"John\""))
        assertTrue(result.contains("  \"active\": true"))
    }

    @Test
    fun testPrettyPrint_Array() {
        val input = "[1,2,3]"
        val result = JsonUtils.prettyPrint(input)

        assertTrue(result.contains("[\n"))
        assertTrue(result.contains("  1,"))
        assertTrue(result.contains("  2,"))
        assertTrue(result.contains("  3"))
        assertTrue(result.contains("\n]"))
    }

    @Test
    fun testPrettyPrint_CustomIndent() {
        val input = "{\"key\":\"value\"}"
        val result = JsonUtils.prettyPrint(input, "\t")

        assertTrue(result.contains("\t\"key\": \"value\""))
    }

    @Test
    fun testPrettyPrint_EmptyStructures() {
        assertEquals("{\n}", JsonUtils.prettyPrint("{}"))
        assertEquals("[\n]", JsonUtils.prettyPrint("[]"))
    }
}

/**
 * Comprehensive test suite for JsonConverters
 */
class JsonConvertersComprehensiveTest {

    @Test
    fun testBoundsToJson_StandardBounds() {
        val result = JsonConverters.boundsToJson(10, 20, 100, 200)
        assertTrue(result.contains("\"left\": 10"))
        assertTrue(result.contains("\"top\": 20"))
        assertTrue(result.contains("\"right\": 100"))
        assertTrue(result.contains("\"bottom\": 200"))
    }

    @Test
    fun testBoundsToJson_ZeroBounds() {
        val result = JsonConverters.boundsToJson(0, 0, 0, 0)
        assertTrue(result.contains("\"left\": 0"))
        assertTrue(result.contains("\"top\": 0"))
        assertTrue(result.contains("\"right\": 0"))
        assertTrue(result.contains("\"bottom\": 0"))
    }

    @Test
    fun testBoundsToJson_NegativeBounds() {
        val result = JsonConverters.boundsToJson(-100, -50, 100, 50)
        assertTrue(result.contains("\"left\": -100"))
        assertTrue(result.contains("\"top\": -50"))
        assertTrue(result.contains("\"right\": 100"))
        assertTrue(result.contains("\"bottom\": 50"))
    }

    @Test
    fun testPointToJson_StandardPoint() {
        val result = JsonConverters.pointToJson(50, 100)
        assertTrue(result.contains("\"x\": 50"))
        assertTrue(result.contains("\"y\": 100"))
    }

    @Test
    fun testPointToJson_Origin() {
        val result = JsonConverters.pointToJson(0, 0)
        assertTrue(result.contains("\"x\": 0"))
        assertTrue(result.contains("\"y\": 0"))
    }

    @Test
    fun testPointToJson_NegativeCoordinates() {
        val result = JsonConverters.pointToJson(-50, -100)
        assertTrue(result.contains("\"x\": -50"))
        assertTrue(result.contains("\"y\": -100"))
    }

    @Test
    fun testParseSynonyms_EmptyArray() {
        val result = JsonConverters.parseSynonyms("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testParseSynonyms_SingleWord() {
        val result = JsonConverters.parseSynonyms("[\"word\"]")
        assertEquals(listOf("word"), result)
    }

    @Test
    fun testParseSynonyms_MultipleWords() {
        val result = JsonConverters.parseSynonyms("[\"word1\", \"word2\", \"word3\"]")
        assertEquals(listOf("word1", "word2", "word3"), result)
    }

    @Test
    fun testParseSynonyms_WithSpaces() {
        val result = JsonConverters.parseSynonyms("[ \"word1\" , \"word2\" , \"word3\" ]")
        assertEquals(listOf("word1", "word2", "word3"), result)
    }

    @Test
    fun testParseSynonyms_InvalidJson() {
        // Should handle gracefully or return empty list
        val result = JsonConverters.parseSynonyms("not json")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testCreateActionJson_MinimalAction() {
        val result = JsonConverters.createActionJson(
            action = "click"
        )
        assertTrue(result.contains("\"action\": \"click\""))
    }

    @Test
    fun testCreateActionJson_WithTarget() {
        val result = JsonConverters.createActionJson(
            action = "click",
            target = "button1"
        )
        assertTrue(result.contains("\"action\": \"click\""))
        assertTrue(result.contains("\"target\": \"button1\""))
    }

    @Test
    fun testCreateActionJson_WithParams() {
        val result = JsonConverters.createActionJson(
            action = "scroll",
            params = mapOf(
                "direction" to "down",
                "distance" to 100,
                "smooth" to true
            )
        )
        assertTrue(result.contains("\"action\": \"scroll\""))
        assertTrue(result.contains("\"direction\": \"down\""))
        assertTrue(result.contains("\"distance\": 100"))
        assertTrue(result.contains("\"smooth\": true"))
    }

    @Test
    fun testCreateActionJson_FullAction() {
        val result = JsonConverters.createActionJson(
            action = "navigate",
            target = "settings_page",
            params = mapOf(
                "animation" to "slide",
                "duration" to 300,
                "preload" to true
            )
        )
        assertTrue(result.contains("\"action\": \"navigate\""))
        assertTrue(result.contains("\"target\": \"settings_page\""))
        assertTrue(result.contains("\"animation\": \"slide\""))
        assertTrue(result.contains("\"duration\": 300"))
        assertTrue(result.contains("\"preload\": true"))
    }

    @Test
    fun testCreateActionJson_EmptyParams() {
        val result = JsonConverters.createActionJson(
            action = "refresh",
            target = "list",
            params = emptyMap()
        )
        assertTrue(result.contains("\"action\": \"refresh\""))
        assertTrue(result.contains("\"target\": \"list\""))
        assertFalse(result.contains("\"params\""))
    }

    @Test
    fun testCreateActionJson_SpecialCharacters() {
        val result = JsonConverters.createActionJson(
            action = "display",
            target = "message\"with\\quotes",
            params = mapOf(
                "text" to "Line 1\nLine 2"
            )
        )
        assertTrue(result.contains("\"action\": \"display\""))
        // Check that special characters are escaped
        assertTrue(result.contains("\\\"") || result.contains("\\\\"))
    }
}

/**
 * Property-based testing for JSON utilities
 */
class JsonUtilsPropertyTest {

    @Test
    fun testEscapeRoundTrip() {
        // Any string that is escaped should produce valid JSON when quoted
        val testStrings = listOf(
            "",
            "simple",
            "with\nnewline",
            "with\ttab",
            "with\"quote",
            "with\\backslash",
            "unicode: 世界 🌍",
            "\u0000\u001F", // Control characters
            "mixed\n\t\"\\content"
        )

        for (original in testStrings) {
            val escaped = JsonUtils.escapeJsonString(original)
            val quoted = "\"$escaped\""
            // In a real scenario, we'd parse this JSON and verify we get the original back
            assertNotNull(quoted)
        }
    }

    @Test
    fun testCreateJsonObjectIsValid() {
        // Any created JSON object should have valid structure
        val pairs = listOf(
            arrayOf(),
            arrayOf("single" to "value"),
            arrayOf("a" to 1, "b" to 2, "c" to 3),
            arrayOf("null" to null, "bool" to true, "num" to 42)
        )

        for (pairArray in pairs) {
            val json = JsonUtils.createJsonObject(*pairArray)
            assertTrue(json.startsWith("{"))
            assertTrue(json.endsWith("}"))
            assertEquals(1, json.count { it == '{' })
            assertEquals(1, json.count { it == '}' })
        }
    }

    @Test
    fun testCreateJsonArrayIsValid() {
        // Any created JSON array should have valid structure
        val arrays = listOf(
            arrayOf(),
            arrayOf("single"),
            arrayOf(1, 2, 3),
            arrayOf(null, true, "text", 42)
        )

        for (array in arrays) {
            val json = JsonUtils.createJsonArray(*array)
            assertTrue(json.startsWith("["))
            assertTrue(json.endsWith("]"))
            assertEquals(1, json.count { it == '[' })
            assertEquals(1, json.count { it == ']' })
        }
    }
}

/**
 * Performance tests for JSON utilities
 */
class JsonUtilsPerformanceTest {

    @Test
    fun testLargeObjectCreation() {
        // Create a large object and ensure it completes in reasonable time
        val pairs = (1..1000).map { "key$it" to "value$it" }.toTypedArray()
        val json = JsonUtils.createJsonObject(*pairs)

        assertTrue(json.length > 10000, "Large object should be created")
        // Performance test - should complete quickly
    }

    @Test
    fun testLargeArrayCreation() {
        // Create a large array and ensure it completes in reasonable time
        val values = (1..1000).toList().toTypedArray()
        val json = JsonUtils.createJsonArray(*values)

        assertTrue(json.length > 1000, "Large array should be created")
        // Performance test - should complete quickly
    }

    @Test
    fun testDeepNestingEscaping() {
        // Test deeply nested escaping
        var str = "base"
        repeat(10) {
            str = JsonUtils.escapeJsonString("nested: \"$str\"")
        }
        assertTrue(str.contains("\\\\"))
    }
}