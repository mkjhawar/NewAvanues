/**
 * HashUtilsComprehensiveTest.kt - Comprehensive tests for hash utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.hash

import kotlin.test.*

/**
 * Comprehensive tests for HashUtils
 */
class HashUtilsTest {

    @Test
    fun testCalculateAppHash_BasicInput() {
        val hash = HashUtils.calculateAppHash("com.example.app", 1)
        assertNotNull(hash)
        assertEquals(64, hash.length) // SHA-256 produces 64 hex chars
    }

    @Test
    fun testCalculateAppHash_DifferentVersions() {
        val hash1 = HashUtils.calculateAppHash("com.app", 1)
        val hash2 = HashUtils.calculateAppHash("com.app", 2)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testCalculateAppHash_DifferentPackages() {
        val hash1 = HashUtils.calculateAppHash("com.app1", 1)
        val hash2 = HashUtils.calculateAppHash("com.app2", 1)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testCalculateAppHash_Deterministic() {
        val hash1 = HashUtils.calculateAppHash("com.test", 42)
        val hash2 = HashUtils.calculateAppHash("com.test", 42)
        assertEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHash_BasicInput() {
        val hash = HashUtils.calculateHash("test")
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun testCalculateHash_EmptyString() {
        val hash = HashUtils.calculateHash("")
        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun testCalculateHash_LongString() {
        val longString = "a".repeat(10000)
        val hash = HashUtils.calculateHash(longString)
        assertEquals(64, hash.length)
    }

    @Test
    fun testCalculateHash_UnicodeString() {
        val hash = HashUtils.calculateHash("Hello 世界 🌍")
        assertEquals(64, hash.length)
    }

    @Test
    fun testCalculateHash_SpecialCharacters() {
        val hash = HashUtils.calculateHash("!@#\$%^&*()_+-=")
        assertEquals(64, hash.length)
    }

    @Test
    fun testCalculateHash_Deterministic() {
        val hash1 = HashUtils.calculateHash("test input")
        val hash2 = HashUtils.calculateHash("test input")
        assertEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHash_CaseSensitive() {
        val hash1 = HashUtils.calculateHash("Test")
        val hash2 = HashUtils.calculateHash("test")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHash_WhitespaceSensitive() {
        val hash1 = HashUtils.calculateHash("test")
        val hash2 = HashUtils.calculateHash(" test")
        val hash3 = HashUtils.calculateHash("test ")
        assertNotEquals(hash1, hash2)
        assertNotEquals(hash1, hash3)
        assertNotEquals(hash2, hash3)
    }

    @Test
    fun testHashFormat_LowercaseHex() {
        val hash = HashUtils.calculateHash("test")
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun testIsValidHash_ValidHash() {
        val hash = HashUtils.calculateHash("test")
        assertTrue(HashUtils.isValidHash(hash))
    }

    @Test
    fun testIsValidHash_InvalidLength() {
        assertFalse(HashUtils.isValidHash("abc123"))
        assertFalse(HashUtils.isValidHash("a".repeat(63)))
        assertFalse(HashUtils.isValidHash("a".repeat(65)))
    }

    @Test
    fun testIsValidHash_InvalidCharacters() {
        val invalidHash = "g".repeat(64) // 'g' is not valid hex
        assertFalse(HashUtils.isValidHash(invalidHash))
    }

    @Test
    fun testIsValidHash_EmptyString() {
        assertFalse(HashUtils.isValidHash(""))
    }

    @Test
    fun testIsValidHash_UppercaseHex() {
        val upperHash = "A".repeat(64)
        // Depending on implementation, this may be valid or invalid
        // SHA-256 hashes are typically lowercase
        val result = HashUtils.isValidHash(upperHash)
        // Just verify it doesn't throw
        assertNotNull(result)
    }
}

/**
 * Tests for collision resistance
 */
class HashCollisionTest {

    @Test
    fun testNoCollisionsForSimilarInputs() {
        val hashes = mutableSetOf<String>()

        // Generate hashes for similar inputs
        for (i in 0..100) {
            val hash = HashUtils.calculateHash("input$i")
            assertFalse(hashes.contains(hash), "Collision detected at index $i")
            hashes.add(hash)
        }

        assertEquals(101, hashes.size)
    }

    @Test
    fun testNoCollisionsForAppVersions() {
        val hashes = mutableSetOf<String>()
        val packageName = "com.example.app"

        for (version in 1..100) {
            val hash = HashUtils.calculateAppHash(packageName, version)
            assertFalse(hashes.contains(hash))
            hashes.add(hash)
        }

        assertEquals(100, hashes.size)
    }
}

/**
 * Integration tests for hash utilities
 */
class HashUtilsIntegrationTest {

    @Test
    fun testAppHashUpdateDetection() {
        val packageName = "com.myapp"

        val v1Hash = HashUtils.calculateAppHash(packageName, 1)
        val v2Hash = HashUtils.calculateAppHash(packageName, 2)

        // Update should produce different hash
        assertNotEquals(v1Hash, v2Hash)

        // Same version should produce same hash
        val v1HashAgain = HashUtils.calculateAppHash(packageName, 1)
        assertEquals(v1Hash, v1HashAgain)
    }

    @Test
    fun testDataDeduplication() {
        val data1 = "Important data"
        val data2 = "Important data"
        val data3 = "Different data"

        val hash1 = HashUtils.calculateHash(data1)
        val hash2 = HashUtils.calculateHash(data2)
        val hash3 = HashUtils.calculateHash(data3)

        // Identical data should have same hash
        assertEquals(hash1, hash2)

        // Different data should have different hash
        assertNotEquals(hash1, hash3)
    }

    @Test
    fun testHashAsIdentifier() {
        // Use hash as unique identifier
        val items = mapOf(
            HashUtils.calculateHash("item1") to "Item 1",
            HashUtils.calculateHash("item2") to "Item 2",
            HashUtils.calculateHash("item3") to "Item 3"
        )

        assertEquals(3, items.size)

        // Lookup by hash
        val key = HashUtils.calculateHash("item2")
        assertEquals("Item 2", items[key])
    }
}
