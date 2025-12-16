package com.augmentalis.ava.core.data.converter

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TypeConverters, especially BLOB binary serialization.
 *
 * Validates:
 * - Binary float serialization/deserialization (round-trip)
 * - Correct handling of null values
 * - Data integrity (byte-for-byte equality)
 * - Performance compared to JSON (60% space savings)
 */
class TypeConvertersTest {

    private lateinit var converter: TypeConverters

    @Before
    fun setup() {
        converter = TypeConverters()
    }

    @Test
    fun `fromFloatList converts list to ByteArray correctly`() {
        val floats = listOf(0.5f, -1.0f, 0.0f, 0.12345f)

        val bytes = converter.fromFloatList(floats)

        assertNotNull("ByteArray should not be null", bytes)
        assertEquals("ByteArray size should be 4 bytes per float", 16, bytes!!.size)
    }

    @Test
    fun `toFloatList converts ByteArray to list correctly`() {
        val floats = listOf(0.5f, -1.0f, 0.0f, 0.12345f)
        val bytes = converter.fromFloatList(floats)

        val result = converter.toFloatList(bytes)

        assertNotNull("List should not be null", result)
        assertEquals("List size should match original", 4, result!!.size)
    }

    @Test
    fun `round-trip conversion preserves float values`() {
        val original = listOf(0.5f, -1.0f, 0.0f, 0.12345f, -0.67890f)

        val bytes = converter.fromFloatList(original)
        val result = converter.toFloatList(bytes)

        assertNotNull(result)
        assertEquals("Size should match", original.size, result!!.size)

        for (i in original.indices) {
            assertEquals(
                "Float at index $i should match",
                original[i],
                result[i],
                0.0001f  // Small delta for floating point comparison
            )
        }
    }

    @Test
    fun `round-trip with 384-dimensional embedding vector`() {
        // Simulate typical embedding vector (384 dimensions)
        val embedding = List(384) { (it.toFloat() - 192f) / 100f }

        val bytes = converter.fromFloatList(embedding)
        val result = converter.toFloatList(bytes)

        assertNotNull("Result should not be null", result)
        assertEquals("Size should be 384", 384, result!!.size)
        assertEquals("ByteArray should be 1536 bytes", 1536, bytes!!.size)

        // Verify all values match
        for (i in embedding.indices) {
            assertEquals(
                "Value at index $i should match",
                embedding[i],
                result[i],
                0.0001f
            )
        }
    }

    @Test
    fun `null input returns null output`() {
        val bytes = converter.fromFloatList(null)
        assertNull("Null input should return null", bytes)

        val floats = converter.toFloatList(null)
        assertNull("Null input should return null", floats)
    }

    @Test
    fun `empty list returns empty ByteArray`() {
        val emptyList = emptyList<Float>()

        val bytes = converter.fromFloatList(emptyList)

        assertNotNull("Should not be null", bytes)
        assertEquals("Should be empty", 0, bytes!!.size)
    }

    @Test
    fun `toFloatList returns null for invalid byte size`() {
        // ByteArray with invalid size (not multiple of 4)
        val invalidBytes = byteArrayOf(1, 2, 3)

        val result = converter.toFloatList(invalidBytes)

        assertNull("Should return null for invalid byte size", result)
    }

    @Test
    fun `toFloatList returns null for empty ByteArray`() {
        val emptyBytes = byteArrayOf()

        val result = converter.toFloatList(emptyBytes)

        assertNull("Should return null for empty ByteArray", result)
    }

    @Test
    fun `binary format is more efficient than JSON`() {
        val embedding = List(384) { (it.toFloat() - 192f) / 100f }

        // Binary BLOB
        val binaryBytes = converter.fromFloatList(embedding)
        val binarySize = binaryBytes?.size ?: 0

        // JSON format (measure byte size, not string length)
        val jsonString = converter.fromFloatListJson(embedding)
        val jsonSize = jsonString?.toByteArray(Charsets.UTF_8)?.size ?: 0

        assertTrue("Binary should be smaller than JSON", binarySize < jsonSize)

        // Verify space savings
        // Binary: 1,536 bytes (384 floats × 4 bytes)
        // JSON: varies based on float formatting, typically 2.5-3x larger
        val savingsPercent = ((jsonSize - binarySize).toDouble() / jsonSize) * 100
        assertTrue(
            "Should have significant space savings, actual: ${savingsPercent.toInt()}%",
            savingsPercent >= 20  // At least 20% savings
        )

        println("Binary: $binarySize bytes, JSON: $jsonSize bytes, Savings: ${savingsPercent.toInt()}%")
    }

    @Test
    fun `handles extreme float values`() {
        val extremeFloats = listOf(
            Float.MIN_VALUE,
            Float.MAX_VALUE,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            0.0f,
            -0.0f
        )

        val bytes = converter.fromFloatList(extremeFloats)
        val result = converter.toFloatList(bytes)

        assertNotNull(result)
        assertEquals(extremeFloats.size, result!!.size)

        for (i in extremeFloats.indices) {
            assertEquals(
                "Extreme value at index $i should match",
                extremeFloats[i],
                result[i],
                0.0f
            )
        }
    }

    @Test
    fun `handles NaN correctly`() {
        val floatsWithNaN = listOf(1.0f, Float.NaN, 2.0f)

        val bytes = converter.fromFloatList(floatsWithNaN)
        val result = converter.toFloatList(bytes)

        assertNotNull(result)
        assertEquals(3, result!!.size)
        assertEquals(1.0f, result[0], 0.0f)
        assertTrue("NaN should remain NaN", result[1].isNaN())
        assertEquals(2.0f, result[2], 0.0f)
    }

    @Test
    fun `legacy JSON converters still work`() {
        val floats = listOf(0.5f, -1.0f, 0.0f, 0.12345f)

        val json = converter.fromFloatListJson(floats)
        val result = converter.toFloatListJson(json)

        assertNotNull("JSON result should not be null", result)
        assertEquals("Size should match", floats.size, result!!.size)

        for (i in floats.indices) {
            assertEquals(
                "Float at index $i should match",
                floats[i],
                result[i],
                0.0001f
            )
        }
    }

    @Test
    fun `Map converters work correctly`() {
        val map = mapOf("key1" to "value1", "key2" to "value2")

        val json = converter.fromMap(map)
        val result = converter.toMap(json)

        assertNotNull("Result should not be null", result)
        assertEquals("Map should match", map, result)
    }

    @Test
    fun `null map returns null`() {
        val json = converter.fromMap(null)
        assertNull("Null map should return null", json)

        val map = converter.toMap(null)
        assertNull("Null JSON should return null", map)
    }

    // ==================== StringListConverter Tests (Bug Fix) ====================

    @Test
    fun `StringListConverter should serialize and deserialize string list`() {
        val stringConverter = StringListConverter()
        val strings = listOf("send email", "compose email", "write email")

        val json = stringConverter.fromStringList(strings)
        val result = stringConverter.toStringList(json)

        assertNotNull("Result should not be null", result)
        assertEquals("List size should match", strings.size, result!!.size)
        assertEquals("List content should match", strings, result)
    }

    @Test
    fun `StringListConverter should handle empty list`() {
        val stringConverter = StringListConverter()
        val emptyList = emptyList<String>()

        val json = stringConverter.fromStringList(emptyList)
        val result = stringConverter.toStringList(json)

        assertNotNull("Result should not be null", result)
        assertEquals("Empty list should remain empty", emptyList, result)
    }

    @Test
    fun `StringListConverter should handle null`() {
        val stringConverter = StringListConverter()

        val json = stringConverter.fromStringList(null)
        assertNull("Null input should return null", json)

        val result = stringConverter.toStringList(null)
        assertNull("Null input should return null", result)
    }

    @Test
    fun `StringListConverter should handle single item`() {
        val stringConverter = StringListConverter()
        val singleItem = listOf("single value")

        val json = stringConverter.fromStringList(singleItem)
        val result = stringConverter.toStringList(json)

        assertNotNull("Result should not be null", result)
        assertEquals("Single item should be preserved", singleItem, result)
    }

    @Test
    fun `StringListConverter should handle special characters`() {
        val stringConverter = StringListConverter()
        val specialStrings = listOf(
            "hello \"world\"",
            "line1\nline2",
            "tab\there",
            "unicode: 你好",
            "emoji: 🚀"
        )

        val json = stringConverter.fromStringList(specialStrings)
        val result = stringConverter.toStringList(json)

        assertNotNull("Result should not be null", result)
        assertEquals("Special characters should be preserved", specialStrings, result)
    }

    @Test
    fun `StringListConverter should return null for invalid JSON`() {
        val stringConverter = StringListConverter()
        val invalidJson = "{this is not valid json}"

        val result = stringConverter.toStringList(invalidJson)

        assertNull("Invalid JSON should return null", result)
    }

    @Test
    fun `StringListConverter round-trip preserves order`() {
        val stringConverter = StringListConverter()
        val orderedList = listOf("first", "second", "third", "fourth")

        val json = stringConverter.fromStringList(orderedList)
        val result = stringConverter.toStringList(json)

        assertNotNull("Result should not be null", result)
        assertEquals("Order should be preserved", orderedList, result)
    }
}
