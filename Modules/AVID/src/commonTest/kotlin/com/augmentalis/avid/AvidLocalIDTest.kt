/**
 * AvidLocalIDTest.kt - Unit tests for AvidLocalID
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avid

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvidLocalIDTest {

    @BeforeTest
    fun resetSequence() {
        AvidLocalID.reset()
        AvidLocalID.setPlatform(Platform.ANDROID)
    }

    // -------------------------------------------------------------------------
    // Format
    // -------------------------------------------------------------------------

    @Test
    fun generate_hasAvidlPrefix() {
        val id = AvidLocalID.generate(Platform.ANDROID)
        assertTrue(id.startsWith("AVIDL-"), "Local ID must start with AVIDL-, got: $id")
    }

    @Test
    fun generate_isDistinctFromGlobalFormat() {
        val local = AvidLocalID.generate(Platform.ANDROID)
        assertFalse(AvidGlobalID.isValid(local), "Local ID must not match global AVID pattern")
        assertTrue(AvidLocalID.isValid(local))
    }

    // -------------------------------------------------------------------------
    // Sequence
    // -------------------------------------------------------------------------

    @Test
    fun generate_firstSequenceIsOne() {
        val id = AvidLocalID.generate(Platform.ANDROID)
        assertEquals("000001", id.split("-").last())
    }

    @Test
    fun generate_monotonicallyIncreasing() {
        val a = AvidLocalID.generate(Platform.ANDROID)
        val b = AvidLocalID.generate(Platform.ANDROID)
        val seqA = a.split("-").last().toLong()
        val seqB = b.split("-").last().toLong()
        assertTrue(seqB > seqA)
    }

    // -------------------------------------------------------------------------
    // Promotion local → global
    // -------------------------------------------------------------------------

    @Test
    fun promoteToGlobal_producesValidGlobalAvid() {
        val local = AvidLocalID.generate(Platform.ANDROID)
        val global = AvidLocalID.promoteToGlobal(local, globalSequence = 1001L)
        assertNotNull(global)
        assertTrue(AvidGlobalID.isValid(global), "Promoted ID must be a valid global AVID, got: $global")
        assertTrue(global.startsWith("AVID-A-"), "Promoted Android ID must start with AVID-A-")
        assertTrue(global.endsWith("001001"), "Sequence 1001 must be padded to 001001")
    }

    @Test
    fun promoteToGlobal_invalidLocalId_returnsNull() {
        assertNull(AvidLocalID.promoteToGlobal("INVALID-ID", 100L))
    }

    // -------------------------------------------------------------------------
    // Parse roundtrip
    // -------------------------------------------------------------------------

    @Test
    fun parse_roundtrip() {
        val id = AvidLocalID.generate(Platform.IOS, seq = 77L)
        val parsed = AvidLocalID.parse(id)
        assertNotNull(parsed)
        assertEquals(Platform.IOS, parsed.platform)
        assertEquals(77L, parsed.sequence)
        assertEquals(id, parsed.toString())
    }

    @Test
    fun parse_invalidId_returnsNull() {
        assertNull(AvidLocalID.parse("AVID-A-000001")) // global format, not local
    }
}
