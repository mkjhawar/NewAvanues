/**
 * HashUtilsTest.kt - Unit tests for HashUtils
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HashUtilsTest {

    @Test
    fun calculateHash_returnsSixtyFourCharLowercaseHex() {
        val hash = HashUtils.calculateHash("hello world")
        assertEquals(64, hash.length, "SHA-256 hash must be 64 characters")
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' }, "Hash must be lowercase hex")
    }

    @Test
    fun calculateHash_sameInputProducesSameOutput() {
        val input = "com.example.app:VoiceOS"
        assertEquals(HashUtils.calculateHash(input), HashUtils.calculateHash(input))
    }

    @Test
    fun calculateHash_differentInputsProduceDifferentOutputs() {
        assertNotEquals(
            HashUtils.calculateHash("com.example.app"),
            HashUtils.calculateHash("com.different.app")
        )
    }

    @Test
    fun calculateAppHash_encodesPackageAndVersion() {
        val hash = HashUtils.calculateAppHash("com.augmentalis.voiceos", 42)
        assertTrue(HashUtils.isValidHash(hash), "calculateAppHash must return a valid SHA-256 hash")
    }

    @Test
    fun isValidHash_rejectsIncorrectLengthAndUppercase() {
        assertTrue(HashUtils.isValidHash("a".repeat(64)))
        assertTrue(!HashUtils.isValidHash("a".repeat(63)), "63-char string must be invalid")
        assertTrue(!HashUtils.isValidHash("A".repeat(64)), "Uppercase hex must be invalid")
        assertTrue(!HashUtils.isValidHash(""), "Empty string must be invalid")
    }
}
