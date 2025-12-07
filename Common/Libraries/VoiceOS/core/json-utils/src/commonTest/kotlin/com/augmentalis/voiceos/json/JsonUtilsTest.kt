/**
 * JsonUtilsTest.kt - Tests for JSON utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.json

import kotlin.test.*

class JsonUtilsTest {

    @Test
    fun testEscapeJsonString() {
        val input = "Line 1\nLine 2\t\"quoted\""
        val result = JsonUtils.escapeJsonString(input)
        assertEquals("Line 1\\nLine 2\\t\\\"quoted\\\"", result)
    }

    @Test
    fun testQuoteJsonString() {
        val result = JsonUtils.quoteJsonString("test")
        assertEquals("\"test\"", result)
    }

    @Test
    fun testCreateJsonObject() {
        val result = JsonUtils.createJsonObject(
            "name" to "John",
            "age" to 30,
            "active" to true
        )
        assertTrue(result.contains("\"name\": \"John\""))
        assertTrue(result.contains("\"age\": 30"))
        assertTrue(result.contains("\"active\": true"))
    }

    @Test
    fun testCreateJsonArray() {
        val result = JsonUtils.createJsonArray("one", 2, true, null)
        assertEquals("[\"one\", 2, true, null]", result)
    }

    @Test
    fun testToJsonValue() {
        assertEquals("null", JsonUtils.toJsonValue(null))
        assertEquals("\"text\"", JsonUtils.toJsonValue("text"))
        assertEquals("42", JsonUtils.toJsonValue(42))
        assertEquals("true", JsonUtils.toJsonValue(true))
    }

    @Test
    fun testPrettyPrint() {
        val input = "{\"name\":\"John\",\"age\":30}"
        val result = JsonUtils.prettyPrint(input)
        assertTrue(result.contains("{\n"))
        assertTrue(result.contains("  \"name\": \"John\""))
        assertTrue(result.contains("  \"age\": 30"))
    }
}

class JsonConvertersTest {

    @Test
    fun testBoundsToJson() {
        val result = JsonConverters.boundsToJson(10, 20, 100, 200)
        assertTrue(result.contains("\"left\": 10"))
        assertTrue(result.contains("\"top\": 20"))
        assertTrue(result.contains("\"right\": 100"))
        assertTrue(result.contains("\"bottom\": 200"))
    }

    @Test
    fun testPointToJson() {
        val result = JsonConverters.pointToJson(50, 100)
        assertTrue(result.contains("\"x\": 50"))
        assertTrue(result.contains("\"y\": 100"))
    }

    @Test
    fun testParseSynonyms() {
        val input = """["word1", "word2", "word3"]"""
        val result = JsonConverters.parseSynonyms(input)
        assertEquals(listOf("word1", "word2", "word3"), result)
    }

    @Test
    fun testParseSynonymsEmpty() {
        val result = JsonConverters.parseSynonyms("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testCreateActionJson() {
        val result = JsonConverters.createActionJson(
            action = "click",
            target = "button1",
            params = mapOf("force" to true)
        )
        assertTrue(result.contains("\"action\": \"click\""))
        assertTrue(result.contains("\"target\": \"button1\""))
        assertTrue(result.contains("\"force\": true"))
    }
}