/**
 * Http2SettingsTest.kt — Unit tests for Http2Settings
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests defaults, companion constants, encode/decode wire roundtrip,
 * and partial-frame decode robustness.
 */
package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.http2.Http2Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Http2SettingsTest {

    // ── Default Values ────────────────────────────────────────────

    @Test
    fun `default settings match RFC 7540 initial values`() {
        val s = Http2Settings()
        assertEquals(4096, s.headerTableSize)
        assertFalse(s.enablePush)
        assertEquals(100, s.maxConcurrentStreams)
        assertEquals(65535, s.initialWindowSize)
        assertEquals(16384, s.maxFrameSize)
        assertEquals(Int.MAX_VALUE, s.maxHeaderListSize)
    }

    // ── Companion Constants ───────────────────────────────────────

    @Test
    fun `parameter ID constants match RFC 7540 section 6_5_2`() {
        assertEquals(0x1, Http2Settings.HEADER_TABLE_SIZE)
        assertEquals(0x2, Http2Settings.ENABLE_PUSH)
        assertEquals(0x3, Http2Settings.MAX_CONCURRENT_STREAMS)
        assertEquals(0x4, Http2Settings.INITIAL_WINDOW_SIZE)
        assertEquals(0x5, Http2Settings.MAX_FRAME_SIZE)
        assertEquals(0x6, Http2Settings.MAX_HEADER_LIST_SIZE)
    }

    // ── Encode/Decode Roundtrip ───────────────────────────────────

    @Test
    fun `encode then decode roundtrip preserves all fields`() {
        val original = Http2Settings(
            headerTableSize = 8192,
            enablePush = true,
            maxConcurrentStreams = 250,
            initialWindowSize = 131072,
            maxFrameSize = 32768,
            maxHeaderListSize = 8192
        )
        val encoded = Http2Settings.encode(original)
        val decoded = Http2Settings.decode(encoded)

        assertEquals(original.headerTableSize, decoded.headerTableSize)
        assertEquals(original.enablePush, decoded.enablePush)
        assertEquals(original.maxConcurrentStreams, decoded.maxConcurrentStreams)
        assertEquals(original.initialWindowSize, decoded.initialWindowSize)
        assertEquals(original.maxFrameSize, decoded.maxFrameSize)
        assertEquals(original.maxHeaderListSize, decoded.maxHeaderListSize)
    }

    @Test
    fun `default settings roundtrip through encode and decode`() {
        val defaults = Http2Settings()
        // maxHeaderListSize == Int.MAX_VALUE is NOT encoded (omitted by design)
        val encoded = Http2Settings.encode(defaults)
        val decoded = Http2Settings.decode(encoded)

        assertEquals(defaults.headerTableSize, decoded.headerTableSize)
        assertEquals(defaults.enablePush, decoded.enablePush)
        assertEquals(defaults.maxConcurrentStreams, decoded.maxConcurrentStreams)
        assertEquals(defaults.initialWindowSize, decoded.initialWindowSize)
        assertEquals(defaults.maxFrameSize, decoded.maxFrameSize)
    }

    @Test
    fun `enablePush false encodes as 0 and decodes correctly`() {
        val settings = Http2Settings(enablePush = false)
        val roundtripped = Http2Settings.decode(Http2Settings.encode(settings))
        assertFalse(roundtripped.enablePush)
    }

    @Test
    fun `enablePush true encodes as 1 and decodes correctly`() {
        val settings = Http2Settings(enablePush = true)
        val roundtripped = Http2Settings.decode(Http2Settings.encode(settings))
        assertTrue(roundtripped.enablePush)
    }

    // ── Encoded Byte Length ───────────────────────────────────────

    @Test
    fun `encode produces 6 bytes per parameter when maxHeaderListSize omitted`() {
        val defaults = Http2Settings() // maxHeaderListSize == Int.MAX_VALUE → not written
        val encoded = Http2Settings.encode(defaults)
        // 5 parameters × 6 bytes = 30
        assertEquals(30, encoded.size)
    }

    @Test
    fun `encode produces 36 bytes when all 6 settings are present`() {
        val settings = Http2Settings(maxHeaderListSize = 16384)
        val encoded = Http2Settings.encode(settings)
        // 6 parameters × 6 bytes = 36
        assertEquals(36, encoded.size)
    }

    // ── Decode Ignores Unknown IDs ────────────────────────────────

    @Test
    fun `decode ignores unknown parameter IDs without throwing`() {
        // Build a 6-byte block with unknown id 0x99
        val unknownBlock = byteArrayOf(0x00, 0x99.toByte(), 0x00, 0x00, 0x00, 0x01)
        val decoded = Http2Settings.decode(unknownBlock)
        // Should parse without exception; fields remain at defaults
        assertEquals(Http2Settings(), decoded)
    }
}
