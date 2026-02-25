/**
 * Http2FrameCodecTest.kt — Unit tests for Http2FrameCodec and Http2FrameType
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests frame type constants, Http2Flags bit values,
 * Http2FrameType.from() lookup, Http2Frame.hasFlag(),
 * and Http2FrameCodec size constants.
 */
package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.http2.Http2Flags
import com.augmentalis.httpavanue.http2.Http2Frame
import com.augmentalis.httpavanue.http2.Http2FrameCodec
import com.augmentalis.httpavanue.http2.Http2FrameType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Http2FrameCodecTest {

    // ── Http2FrameType Constants (RFC 7540 Section 6) ────────────

    @Test
    fun `frame type values match RFC 7540`() {
        assertEquals(0x0, Http2FrameType.DATA.value)
        assertEquals(0x1, Http2FrameType.HEADERS.value)
        assertEquals(0x2, Http2FrameType.PRIORITY.value)
        assertEquals(0x3, Http2FrameType.RST_STREAM.value)
        assertEquals(0x4, Http2FrameType.SETTINGS.value)
        assertEquals(0x5, Http2FrameType.PUSH_PROMISE.value)
        assertEquals(0x6, Http2FrameType.PING.value)
        assertEquals(0x7, Http2FrameType.GOAWAY.value)
        assertEquals(0x8, Http2FrameType.WINDOW_UPDATE.value)
        assertEquals(0x9, Http2FrameType.CONTINUATION.value)
    }

    @Test
    fun `Http2FrameType has exactly 10 members`() {
        assertEquals(10, Http2FrameType.entries.size)
    }

    // ── Http2FrameType.from() ─────────────────────────────────────

    @Test
    fun `from returns correct type for known values`() {
        assertEquals(Http2FrameType.DATA, Http2FrameType.from(0x0))
        assertEquals(Http2FrameType.SETTINGS, Http2FrameType.from(0x4))
        assertEquals(Http2FrameType.GOAWAY, Http2FrameType.from(0x7))
        assertEquals(Http2FrameType.CONTINUATION, Http2FrameType.from(0x9))
    }

    @Test
    fun `from returns null for unknown type value`() {
        assertNull(Http2FrameType.from(0xFF))
        assertNull(Http2FrameType.from(0x0A))
    }

    // ── Http2Flags Constants ──────────────────────────────────────

    @Test
    fun `flag constants are correct bit values`() {
        assertEquals(0x1, Http2Flags.END_STREAM)
        assertEquals(0x4, Http2Flags.END_HEADERS)
        assertEquals(0x8, Http2Flags.PADDED)
        assertEquals(0x20, Http2Flags.PRIORITY)
        assertEquals(0x1, Http2Flags.ACK) // Same bit as END_STREAM, used on SETTINGS/PING
    }

    // ── Http2Frame.hasFlag ────────────────────────────────────────

    @Test
    fun `hasFlag returns true when the flag bit is set`() {
        val frame = Http2Frame(
            type = Http2FrameType.HEADERS,
            typeValue = Http2FrameType.HEADERS.value,
            flags = Http2Flags.END_STREAM or Http2Flags.END_HEADERS,
            streamId = 1,
            payload = byteArrayOf()
        )
        assertTrue(frame.hasFlag(Http2Flags.END_STREAM))
        assertTrue(frame.hasFlag(Http2Flags.END_HEADERS))
    }

    @Test
    fun `hasFlag returns false when flag bit is not set`() {
        val frame = Http2Frame(
            type = Http2FrameType.DATA,
            typeValue = Http2FrameType.DATA.value,
            flags = 0,
            streamId = 3,
            payload = byteArrayOf(1, 2, 3)
        )
        assertFalse(frame.hasFlag(Http2Flags.END_STREAM))
        assertFalse(frame.hasFlag(Http2Flags.PADDED))
    }

    // ── Http2FrameCodec Size Constants ────────────────────────────

    @Test
    fun `FRAME_HEADER_SIZE is 9`() {
        assertEquals(9, Http2FrameCodec.FRAME_HEADER_SIZE)
    }

    @Test
    fun `DEFAULT_MAX_FRAME_SIZE is 16384`() {
        assertEquals(16384, Http2FrameCodec.DEFAULT_MAX_FRAME_SIZE)
    }

    @Test
    fun `MAX_MAX_FRAME_SIZE is 2 to the power of 24 minus 1`() {
        assertEquals(16777215, Http2FrameCodec.MAX_MAX_FRAME_SIZE)
    }

    // ── Connection Preface ────────────────────────────────────────

    @Test
    fun `CONNECTION_PREFACE decodes to correct magic string`() {
        val preface = Http2FrameCodec.CONNECTION_PREFACE.decodeToString()
        assertEquals("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n", preface)
    }

    @Test
    fun `CONNECTION_PREFACE has length of 24 bytes`() {
        assertEquals(24, Http2FrameCodec.CONNECTION_PREFACE.size)
    }

    // ── Http2Frame.length derived property ───────────────────────

    @Test
    fun `frame length property equals payload size`() {
        val payload = ByteArray(42) { it.toByte() }
        val frame = Http2Frame(
            type = Http2FrameType.DATA,
            typeValue = Http2FrameType.DATA.value,
            flags = 0,
            streamId = 5,
            payload = payload
        )
        assertEquals(42, frame.length)
    }
}
