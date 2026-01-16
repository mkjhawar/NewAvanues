package com.augmentalis.voiceoscoreng.functions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HashUtilsTest {

    // ==================== Package Name Hashing ====================

    @Test
    fun `hashPackageName returns 6-char hash for valid package`() {
        val hash = HashUtils.hashPackageName("com.instagram.android")
        assertEquals(6, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `hashPackageName returns consistent hash for same input`() {
        val hash1 = HashUtils.hashPackageName("com.example.app")
        val hash2 = HashUtils.hashPackageName("com.example.app")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hashPackageName returns different hashes for different inputs`() {
        val hash1 = HashUtils.hashPackageName("com.example.app1")
        val hash2 = HashUtils.hashPackageName("com.example.app2")
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `hashPackageName returns zeros for blank input`() {
        assertEquals("000000", HashUtils.hashPackageName(""))
        assertEquals("000000", HashUtils.hashPackageName("   "))
    }

    // ==================== Element Property Hashing ====================

    @Test
    fun `hashElementProperties returns 8-char hash`() {
        val hash = HashUtils.hashElementProperties(
            resourceId = "com.app:id/button",
            text = "Submit",
            bounds = "10,20,100,50"
        )
        assertEquals(8, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `hashElementProperties returns consistent hash for same input`() {
        val hash1 = HashUtils.hashElementProperties("id", "text", "0,0,100,100")
        val hash2 = HashUtils.hashElementProperties("id", "text", "0,0,100,100")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hashElementProperties returns different hash for different bounds`() {
        val hash1 = HashUtils.hashElementProperties("id", "text", "0,0,100,100")
        val hash2 = HashUtils.hashElementProperties("id", "text", "0,0,200,200")
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `hashElementProperties handles empty values`() {
        val hash = HashUtils.hashElementProperties("", "", "")
        assertEquals(8, hash.length)
    }

    // ==================== General Hash Generation ====================

    @Test
    fun `generateHash respects length parameter`() {
        for (length in 1..16) {
            val hash = HashUtils.generateHash("test input", length)
            assertEquals(length, hash.length, "Hash length should be $length")
        }
    }

    @Test
    fun `generateHash returns zeros for empty input`() {
        assertEquals("0000", HashUtils.generateHash("", 4))
        assertEquals("00000000", HashUtils.generateHash("", 8))
    }

    @Test
    fun `generateHash produces only hex characters`() {
        val hash = HashUtils.generateHash("test with special chars !@#\$%", 16)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `generateHash is case sensitive`() {
        val hash1 = HashUtils.generateHash("Test", 8)
        val hash2 = HashUtils.generateHash("test", 8)
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `generateHash throws for invalid length`() {
        var exceptionThrown = false
        try {
            HashUtils.generateHash("test", 0)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)

        exceptionThrown = false
        try {
            HashUtils.generateHash("test", 17)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }

    // ==================== Seeded Hash Generation ====================

    @Test
    fun `generateSeededHash returns consistent hash for same seed`() {
        val hash1 = HashUtils.generateSeededHash(12345L, 8)
        val hash2 = HashUtils.generateSeededHash(12345L, 8)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `generateSeededHash returns different hash for different seeds`() {
        val hash1 = HashUtils.generateSeededHash(12345L, 8)
        val hash2 = HashUtils.generateSeededHash(54321L, 8)
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `generateSeededHash respects length parameter`() {
        for (length in 1..16) {
            val hash = HashUtils.generateSeededHash(42L, length)
            assertEquals(length, hash.length)
        }
    }

    @Test
    fun `generateSeededHash produces only hex characters`() {
        val hash = HashUtils.generateSeededHash(getCurrentTimeMillis(), 16)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    // ==================== Hash Validation ====================

    @Test
    fun `isValidHash returns true for valid hex strings`() {
        assertTrue(HashUtils.isValidHash("abcdef"))
        assertTrue(HashUtils.isValidHash("123456"))
        assertTrue(HashUtils.isValidHash("a1b2c3"))
        assertTrue(HashUtils.isValidHash("0"))
    }

    @Test
    fun `isValidHash returns false for invalid hex strings`() {
        assertFalse(HashUtils.isValidHash(""))
        assertFalse(HashUtils.isValidHash("ghijkl"))
        assertFalse(HashUtils.isValidHash("ABCDEF")) // uppercase not allowed
        assertFalse(HashUtils.isValidHash("abc123!"))
    }

    @Test
    fun `isValidHash checks expected length`() {
        assertTrue(HashUtils.isValidHash("abcdef", 6))
        assertFalse(HashUtils.isValidHash("abcdef", 8))
        assertFalse(HashUtils.isValidHash("abcd", 6))
    }

    @Test
    fun `isValidHash with expectedLength 0 accepts any length`() {
        assertTrue(HashUtils.isValidHash("a", 0))
        assertTrue(HashUtils.isValidHash("abcdef", 0))
        assertTrue(HashUtils.isValidHash("0123456789abcdef", 0))
    }

    // ==================== Hash Normalization ====================

    @Test
    fun `normalizeHash converts to lowercase`() {
        assertEquals("abcdef", HashUtils.normalizeHash("ABCDEF"))
        assertEquals("abcdef", HashUtils.normalizeHash("AbCdEf"))
        assertEquals("123abc", HashUtils.normalizeHash("123ABC"))
    }

    @Test
    fun `normalizeHash preserves already lowercase`() {
        assertEquals("abcdef", HashUtils.normalizeHash("abcdef"))
    }
}
