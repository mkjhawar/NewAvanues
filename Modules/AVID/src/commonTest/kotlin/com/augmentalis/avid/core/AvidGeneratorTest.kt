/**
 * AvidGeneratorTest.kt - Unit tests for AvidGenerator (compact format)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvidGeneratorTest {

    // -------------------------------------------------------------------------
    // generateCompact — format validation
    // -------------------------------------------------------------------------

    @Test
    fun generateCompact_producesValidCompactAppFormat() {
        val avid = AvidGenerator.generateCompact(
            packageName = "com.instagram.android",
            version = "12.0.0",
            typeName = "button"
        )
        assertTrue(AvidGenerator.isCompactApp(avid), "Expected compact app format, got: $avid")
    }

    @Test
    fun generateCompact_reversePackageEmbedded() {
        val avid = AvidGenerator.generateCompact("com.example.app", "1.0.0", "button")
        // Reversed: "app.example.com" is prefix
        assertTrue(avid.startsWith("app.example.com:"), "Expected reversed package prefix in: $avid")
    }

    // -------------------------------------------------------------------------
    // generateCompactSimple — format validation
    // -------------------------------------------------------------------------

    @Test
    fun generateCompactSimple_producesValidSimpleFormat() {
        val avid = AvidGenerator.generateCompactSimple(AvidGenerator.Module.AVA, "message")
        assertTrue(AvidGenerator.isCompactSimple(avid), "Expected simple format, got: $avid")
    }

    @Test
    fun generateCompactSimple_embedsCorrectTypeAbbrev() {
        val avid = AvidGenerator.generateCompactSimple(AvidGenerator.Module.AVA, "message")
        val parts = avid.split(":")
        assertEquals("ava", parts[0])
        assertEquals("msg", parts[1])
        assertEquals(8, parts[2].length, "Hash part must be 8 chars")
    }

    // -------------------------------------------------------------------------
    // parse / roundtrip
    // -------------------------------------------------------------------------

    @Test
    fun parse_compactApp_roundtrip() {
        val original = AvidGenerator.generateCompact("com.instagram.android", "12.0.0", "button", "a1b2c3d4")
        val parsed = AvidGenerator.parse(original)

        assertNotNull(parsed)
        assertEquals("com.instagram.android", parsed.packageName)
        assertEquals("12.0.0", parsed.version)
        assertEquals("btn", parsed.typeAbbrev)
        assertEquals("a1b2c3d4", parsed.hash)
        assertEquals(AvidGenerator.AvidFormat.COMPACT_APP, parsed.format)

        // Reconstruct and verify it matches
        assertEquals(original, parsed.toCompactAvid())
    }

    @Test
    fun parse_compactSimple_roundtrip() {
        val avid = AvidGenerator.generateCompactSimple("ava", "message")
        val parsed = AvidGenerator.parse(avid)
        assertNotNull(parsed)
        assertEquals(AvidGenerator.AvidFormat.COMPACT_SIMPLE, parsed.format)
        assertEquals("ava", parsed.module)
        assertEquals("msg", parsed.typeAbbrev)
    }

    // -------------------------------------------------------------------------
    // reversePackage / unreversePackage
    // -------------------------------------------------------------------------

    @Test
    fun reversePackage_andBack_isIdentity() {
        val pkg = "com.augmentalis.voiceos"
        assertEquals(pkg, AvidGenerator.unreversePackage(AvidGenerator.reversePackage(pkg)))
    }

    @Test
    fun reversePackage_correctOutput() {
        assertEquals("android.instagram.com", AvidGenerator.reversePackage("com.instagram.android"))
    }

    // -------------------------------------------------------------------------
    // isValid
    // -------------------------------------------------------------------------

    @Test
    fun isValid_rejectsRandomStrings() {
        assertTrue(!AvidGenerator.isValid(""), "Empty string must not be valid")
        assertTrue(!AvidGenerator.isValid("not-an-avid"), "Random string must not be valid")
    }

    @Test
    fun parse_returnsNullForInvalidAvid() {
        assertNull(AvidGenerator.parse("garbage::string"))
    }
}
