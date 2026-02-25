/**
 * AvidGlobalIDTest.kt - Unit tests for AvidGlobalID
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avid

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvidGlobalIDTest {

    @BeforeTest
    fun resetSequence() {
        AvidGlobalID.reset()
        AvidGlobalID.setPlatform(Platform.ANDROID)
    }

    // -------------------------------------------------------------------------
    // Format
    // -------------------------------------------------------------------------

    @Test
    fun generate_producesValidFormat() {
        val id = AvidGlobalID.generate(Platform.ANDROID)
        assertTrue(AvidGlobalID.isValid(id), "Expected valid format, got: $id")
    }

    @Test
    fun generate_platformCodeIsEmbedded() {
        val id = AvidGlobalID.generate(Platform.IOS)
        assertTrue(id.startsWith("AVID-I-"), "iOS ID must start with AVID-I-, got: $id")
    }

    @Test
    fun generate_sequencePaddedToSixDigits() {
        // After reset, first call produces sequence = 1 → "000001"
        val id = AvidGlobalID.generate(Platform.ANDROID)
        val seqPart = id.split("-").last()
        assertEquals(6, seqPart.length, "Sequence must be zero-padded to 6 digits")
        assertEquals("000001", seqPart)
    }

    // -------------------------------------------------------------------------
    // Monotonic increment
    // -------------------------------------------------------------------------

    @Test
    fun generate_sequenceIncreasesMonotonically() {
        val id1 = AvidGlobalID.generate(Platform.ANDROID)
        val id2 = AvidGlobalID.generate(Platform.ANDROID)
        val seq1 = id1.split("-").last().toLong()
        val seq2 = id2.split("-").last().toLong()
        assertTrue(seq2 > seq1, "Second ID sequence must be greater than first")
    }

    // -------------------------------------------------------------------------
    // Parse roundtrip
    // -------------------------------------------------------------------------

    @Test
    fun parse_roundtrip() {
        val id = AvidGlobalID.generate(Platform.WEB, seq = 42L)
        val parsed = AvidGlobalID.parse(id)
        assertNotNull(parsed)
        assertEquals(Platform.WEB, parsed.platform)
        assertEquals(42L, parsed.sequence)
        assertEquals(id, parsed.toString())
    }

    @Test
    fun parse_invalidId_returnsNull() {
        assertNull(AvidGlobalID.parse("NOT-VALID"))
        assertNull(AvidGlobalID.parse(""))
    }

    // -------------------------------------------------------------------------
    // extractPlatform
    // -------------------------------------------------------------------------

    @Test
    fun extractPlatform_correctForEachCode() {
        Platform.entries.forEach { platform ->
            val id = AvidGlobalID.generate(platform)
            assertEquals(platform, AvidGlobalID.extractPlatform(id))
        }
    }
}
