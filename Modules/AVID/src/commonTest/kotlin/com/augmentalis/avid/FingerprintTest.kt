/**
 * FingerprintTest.kt - Unit tests for Fingerprint
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FingerprintTest {

    // -------------------------------------------------------------------------
    // forElement — determinism and format
    // -------------------------------------------------------------------------

    @Test
    fun forElement_sameInputsProduceSameHash() {
        val h1 = Fingerprint.forElement("BTN", "com.example.app", "btn_save", "Save", "Save button")
        val h2 = Fingerprint.forElement("BTN", "com.example.app", "btn_save", "Save", "Save button")
        assertEquals(h1, h2)
    }

    @Test
    fun forElement_differentPackageProducesDifferentHash() {
        val h1 = Fingerprint.forElement("BTN", "com.app.a", "btn_save", "Save", "")
        val h2 = Fingerprint.forElement("BTN", "com.app.b", "btn_save", "Save", "")
        assertNotEquals(h1, h2, "Same element in different packages must have different hashes")
    }

    @Test
    fun forElement_hashIsEightLowercaseHexChars() {
        val hash = Fingerprint.forElement("INP", "com.example", "edit_search", "Search", null)
        assertEquals(8, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun forElement_differentTypeProducesDifferentHash() {
        val btnHash = Fingerprint.forElement("BTN", "com.example", "id", "OK", null)
        val inpHash = Fingerprint.forElement("INP", "com.example", "id", "OK", null)
        assertNotEquals(btnHash, inpHash)
    }

    // -------------------------------------------------------------------------
    // forApp
    // -------------------------------------------------------------------------

    @Test
    fun forApp_hashIsTwelveLowercaseHexChars() {
        val hash = Fingerprint.forApp("com.instagram.android", "Instagram")
        assertEquals(12, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun forApp_sameInputsProduceSameHash() {
        assertEquals(
            Fingerprint.forApp("com.example", "Example"),
            Fingerprint.forApp("com.example", "Example")
        )
    }

    // -------------------------------------------------------------------------
    // deterministicHash
    // -------------------------------------------------------------------------

    @Test
    fun deterministicHash_emptyInputIsStable() {
        val h1 = Fingerprint.deterministicHash("", 8)
        val h2 = Fingerprint.deterministicHash("", 8)
        assertEquals(h1, h2)
    }

    @Test
    fun deterministicHash_respectsRequestedLength() {
        for (len in listOf(4, 8, 12, 16)) {
            assertEquals(len, Fingerprint.deterministicHash("test", len).length)
        }
    }
}
