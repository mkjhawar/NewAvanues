package com.avanues.avu.codec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AVUEncoder — AVU wire protocol message encoder.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class AVUEncoderTest {

    // ── Voice Commands ────────────────────────────────────────────────────────

    @Test
    fun encodeVoiceCommand_produces_correct_pipe_delimited_format() {
        val encoded = AVUEncoder.encodeVoiceCommand("cmd123", "SCROLL_TOP")
        assertEquals("VCM:cmd123:SCROLL_TOP", encoded)
    }

    @Test
    fun encodeVoiceCommand_escapes_colon_in_commandId() {
        val encoded = AVUEncoder.encodeVoiceCommand("cmd:x", "CLICK")
        assertTrue(encoded.startsWith("VCM:cmd%3Ax:"))
    }

    @Test
    fun encodeVoiceCommand_includes_params_as_key_value() {
        val encoded = AVUEncoder.encodeVoiceCommand("cmd1", "NAVIGATE", mapOf("url" to "https://google.com"))
        assertTrue(encoded.startsWith("VCM:cmd1:NAVIGATE:"))
        assertTrue(encoded.contains("url="))
    }

    @Test
    fun encodeVoiceCommand_throws_on_blank_commandId() {
        assertFailsWith<IllegalArgumentException> {
            AVUEncoder.encodeVoiceCommand("", "CLICK")
        }
    }

    @Test
    fun encodeVoiceCommand_throws_on_blank_action() {
        assertFailsWith<IllegalArgumentException> {
            AVUEncoder.encodeVoiceCommand("cmd1", "")
        }
    }

    // ── Accept / Decline / Busy ───────────────────────────────────────────────

    @Test
    fun encodeAccept_without_data() {
        val encoded = AVUEncoder.encodeAccept("req1")
        assertEquals("ACC:req1", encoded)
    }

    @Test
    fun encodeAccept_with_data() {
        val encoded = AVUEncoder.encodeAccept("req1", "result text")
        assertEquals("ACD:req1:result text", encoded)
    }

    @Test
    fun encodeDecline_without_reason() {
        val encoded = AVUEncoder.encodeDecline("req2")
        assertEquals("DEC:req2", encoded)
    }

    @Test
    fun encodeDecline_with_reason() {
        val encoded = AVUEncoder.encodeDecline("req2", "busy")
        assertEquals("DCR:req2:busy", encoded)
    }

    @Test
    fun encodeBusy_without_callback() {
        val encoded = AVUEncoder.encodeBusy("req3")
        assertEquals("BSY:req3", encoded)
    }

    // ── Error ────────────────────────────────────────────────────────────────

    @Test
    fun encodeError_with_code_and_message() {
        val encoded = AVUEncoder.encodeError("req4", 404, "Not found")
        assertEquals("ERR:req4:404:Not found", encoded)
    }

    // ── Handshake / Ping ─────────────────────────────────────────────────────

    @Test
    fun encodeHandshake_produces_correct_format() {
        val encoded = AVUEncoder.encodeHandshake("session1", "com.app", "1.0.0")
        assertEquals("HND:session1:com.app:1.0.0", encoded)
    }

    @Test
    fun encodePing_produces_correct_format() {
        val encoded = AVUEncoder.encodePing("sess1", 1706300000000L)
        assertEquals("PNG:sess1:1706300000000", encoded)
    }

    // ── Generic ──────────────────────────────────────────────────────────────

    @Test
    fun encodeGeneric_three_uppercase_code_produces_correct_format() {
        val encoded = AVUEncoder.encodeGeneric("VCM", "id1", "param1", "param2")
        assertEquals("VCM:id1:param1:param2", encoded)
    }

    @Test
    fun encodeGeneric_throws_on_invalid_code() {
        assertFailsWith<IllegalArgumentException> {
            AVUEncoder.encodeGeneric("vcm", "id1") // lowercase
        }
    }

    @Test
    fun encodeGeneric_throws_on_code_not_three_chars() {
        assertFailsWith<IllegalArgumentException> {
            AVUEncoder.encodeGeneric("VCMD", "id1") // 4 chars
        }
    }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    fun isValidMessage_accepts_well_formed_message() {
        assertTrue(AVUEncoder.isValidMessage("VCM:cmd1:SCROLL"))
    }

    @Test
    fun isValidMessage_rejects_too_short_message() {
        assertTrue(!AVUEncoder.isValidMessage("VCM"))
    }

    @Test
    fun extractCode_returns_correct_three_letter_code() {
        val code = AVUEncoder.extractCode("VCM:cmd1:ACTION")
        assertEquals("VCM", code)
    }

    @Test
    fun extractCode_returns_null_for_lowercase_code() {
        val code = AVUEncoder.extractCode("vcm:cmd1")
        assertEquals(null, code)
    }

    // ── Plugin Manifest ───────────────────────────────────────────────────────

    @Test
    fun encodePluginHeader_produces_PLG_format() {
        val encoded = AVUEncoder.encodePluginHeader("com.example.plugin", "1.0.0", "com.example.Main", "My Plugin")
        assertEquals("PLG:com.example.plugin:1.0.0:com.example.Main:My Plugin", encoded)
    }

    @Test
    fun encodePluginCapabilities_pipe_separates_caps() {
        val encoded = AVUEncoder.encodePluginCapabilities(listOf("voice", "ai.nlu"))
        assertEquals("PCP:voice|ai.nlu", encoded)
    }
}
