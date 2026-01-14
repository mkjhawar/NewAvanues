/**
 * VUIDGeneratorTest.kt - Tests for KMP VUIDGenerator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.vuid

import com.augmentalis.vuid.core.VUIDGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class VUIDGeneratorTest {

    @Test
    fun testGenerateCompactApp() {
        val vuid = VUIDGenerator.generateCompact(
            packageName = "com.instagram.android",
            version = "12.0.0",
            typeName = "button"
        )

        // Format: android.instagram.com:12.0.0:btn:xxxxxxxx
        assertTrue(vuid.startsWith("android.instagram.com:12.0.0:btn:"))
        assertEquals(8, vuid.substringAfterLast(":").length)
    }

    @Test
    fun testGenerateCompactModule() {
        val vuid = VUIDGenerator.generateCompactModule(
            module = VUIDGenerator.Module.AVA,
            version = "1.0.0",
            typeName = "message"
        )

        // Format: ava:1.0.0:msg:xxxxxxxx
        assertTrue(vuid.startsWith("ava:1.0.0:msg:"))
        assertEquals(8, vuid.substringAfterLast(":").length)
    }

    @Test
    fun testGenerateCompactSimple() {
        val vuid = VUIDGenerator.generateCompactSimple(
            module = VUIDGenerator.Module.AVA,
            typeName = "message"
        )

        // Format: ava:msg:xxxxxxxx
        assertTrue(vuid.startsWith("ava:msg:"))
        assertEquals(8, vuid.substringAfterLast(":").length)
    }

    @Test
    fun testGenerateMessageVuid() {
        val vuid = VUIDGenerator.generateMessageVuid()
        assertTrue(VUIDGenerator.isCompactSimple(vuid))
        assertTrue(vuid.startsWith("ava:msg:"))
    }

    @Test
    fun testReversePackage() {
        assertEquals(
            "android.instagram.com",
            VUIDGenerator.reversePackage("com.instagram.android")
        )
    }

    @Test
    fun testUnreversePackage() {
        assertEquals(
            "com.instagram.android",
            VUIDGenerator.unreversePackage("android.instagram.com")
        )
    }

    @Test
    fun testIsCompactApp() {
        assertTrue(VUIDGenerator.isCompactApp("android.instagram.com:12.0.0:btn:a7f3e2c1"))
        assertFalse(VUIDGenerator.isCompactApp("ava:msg:a7f3e2c1"))
    }

    @Test
    fun testIsCompactSimple() {
        assertTrue(VUIDGenerator.isCompactSimple("ava:msg:a7f3e2c1"))
        assertFalse(VUIDGenerator.isCompactSimple("android.instagram.com:12.0.0:btn:a7f3e2c1"))
    }

    @Test
    fun testParseCompactApp() {
        val parsed = VUIDGenerator.parse("android.instagram.com:12.0.0:btn:a7f3e2c1")

        assertNotNull(parsed)
        assertEquals("com.instagram.android", parsed.packageName)
        assertEquals("12.0.0", parsed.version)
        assertEquals("btn", parsed.typeAbbrev)
        assertEquals("button", parsed.typeName)
        assertEquals("a7f3e2c1", parsed.hash)
        assertEquals(VUIDGenerator.VuidFormat.COMPACT_APP, parsed.format)
    }

    @Test
    fun testParseCompactSimple() {
        val parsed = VUIDGenerator.parse("ava:msg:a7f3e2c1")

        assertNotNull(parsed)
        assertEquals("ava", parsed.module)
        assertEquals("msg", parsed.typeAbbrev)
        assertEquals("message", parsed.typeName)
        assertEquals("a7f3e2c1", parsed.hash)
        assertEquals(VUIDGenerator.VuidFormat.COMPACT_SIMPLE, parsed.format)
    }

    @Test
    fun testToVerbose() {
        val parsed = VUIDGenerator.parse("android.instagram.com:12.0.0:btn:a7f3e2c1")

        assertNotNull(parsed)
        assertEquals(
            "com.instagram.android v12.0.0 button [a7f3e2c1]",
            parsed.toVerbose()
        )
    }

    @Test
    fun testTypeAbbrevFromTypeName() {
        assertEquals("btn", VUIDGenerator.TypeAbbrev.fromTypeName("button"))
        assertEquals("btn", VUIDGenerator.TypeAbbrev.fromTypeName("ImageButton"))
        assertEquals("inp", VUIDGenerator.TypeAbbrev.fromTypeName("EditText"))
        assertEquals("msg", VUIDGenerator.TypeAbbrev.fromTypeName("message"))
        assertEquals("cnv", VUIDGenerator.TypeAbbrev.fromTypeName("conversation"))
        assertEquals("elm", VUIDGenerator.TypeAbbrev.fromTypeName("unknown"))
    }

    @Test
    fun testTypeAbbrevToTypeName() {
        assertEquals("button", VUIDGenerator.TypeAbbrev.toTypeName("btn"))
        assertEquals("input", VUIDGenerator.TypeAbbrev.toTypeName("inp"))
        assertEquals("message", VUIDGenerator.TypeAbbrev.toTypeName("msg"))
        assertEquals("conversation", VUIDGenerator.TypeAbbrev.toTypeName("cnv"))
        assertEquals("element", VUIDGenerator.TypeAbbrev.toTypeName("xyz"))
    }

    @Test
    fun testGenerateHash8() {
        val hash = VUIDGenerator.generateHash8()
        assertEquals(8, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun testIsValid() {
        // Compact formats
        assertTrue(VUIDGenerator.isValid("android.instagram.com:12.0.0:btn:a7f3e2c1"))
        assertTrue(VUIDGenerator.isValid("ava:1.0.0:msg:a7f3e2c1"))
        assertTrue(VUIDGenerator.isValid("ava:msg:a7f3e2c1"))

        // Legacy UUID
        assertTrue(VUIDGenerator.isValid("550e8400-e29b-41d4-a716-446655440000"))

        // Invalid
        assertFalse(VUIDGenerator.isValid("invalid-vuid"))
    }
}
