package com.avanues.avu.codec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AVUDecoder — AVU wire protocol message parser.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class AVUDecoderTest {

    // ── parse ─────────────────────────────────────────────────────────────────

    @Test
    fun parse_basic_message_returns_correct_components() {
        val parsed = AVUDecoder.parse("VCM:cmd123:SCROLL_TOP")
        assertNotNull(parsed)
        assertEquals("VCM", parsed.code)
        assertEquals("cmd123", parsed.id)
        assertEquals(1, parsed.params.size)
        assertEquals("SCROLL_TOP", parsed.params[0])
    }

    @Test
    fun parse_message_with_multiple_params() {
        val parsed = AVUDecoder.parse("ERR:req1:404:Not found")
        assertNotNull(parsed)
        assertEquals("ERR", parsed.code)
        assertEquals("req1", parsed.id)
        assertEquals(2, parsed.params.size)
        assertEquals("404", parsed.params[0])
        assertEquals("Not found", parsed.params[1])
    }

    @Test
    fun parse_returns_null_for_too_short_message() {
        assertNull(AVUDecoder.parse("VCM"))
    }

    @Test
    fun parse_returns_null_for_lowercase_code() {
        assertNull(AVUDecoder.parse("vcm:id:action"))
    }

    @Test
    fun parse_unescapes_colon_in_field_values() {
        val encoded = AVUEncoder.encodeVoiceCommand("cmd1", "NAVIGATE", mapOf("url" to "http://site.com:80/"))
        val parsed = AVUDecoder.parse(encoded)
        assertNotNull(parsed)
        // url param contains colon — unescape should restore it
        val urlParam = parsed.params.firstOrNull { it.contains("url=") }
        assertNotNull(urlParam)
        assertTrue(urlParam.contains(":"))
    }

    // ── parseVoiceCommand ─────────────────────────────────────────────────────

    @Test
    fun parseVoiceCommand_extracts_triple_correctly() {
        val message = AVUEncoder.encodeVoiceCommand("cmd42", "ZOOM_IN")
        val result = AVUDecoder.parseVoiceCommand(message)
        assertNotNull(result)
        assertEquals("cmd42", result.first)   // commandId
        assertEquals("ZOOM_IN", result.second) // action
        assertTrue(result.third.isEmpty())     // no params
    }

    @Test
    fun parseVoiceCommand_returns_null_for_non_VCM_message() {
        assertNull(AVUDecoder.parseVoiceCommand("ACC:req1"))
    }

    // ── parseError ────────────────────────────────────────────────────────────

    @Test
    fun parseError_extracts_code_and_message() {
        val message = AVUEncoder.encodeError("req5", 500, "Internal error")
        val result = AVUDecoder.parseError(message)
        assertNotNull(result)
        assertEquals("req5", result.first)
        assertEquals(500, result.second)
        assertEquals("Internal error", result.third)
    }

    @Test
    fun parseError_returns_null_for_non_ERR_message() {
        assertNull(AVUDecoder.parseError("VCM:cmd1:CLICK"))
    }

    // ── parseHandshake ────────────────────────────────────────────────────────

    @Test
    fun parseHandshake_extracts_session_app_version() {
        val message = AVUEncoder.encodeHandshake("sess1", "com.myapp", "2.3.0")
        val result = AVUDecoder.parseHandshake(message)
        assertNotNull(result)
        assertEquals("sess1", result.first)
        assertEquals("com.myapp", result.second)
        assertEquals("2.3.0", result.third)
    }

    // ── parseSpeechToText ─────────────────────────────────────────────────────

    @Test
    fun parseSpeechToText_extracts_all_fields() {
        val message = AVUEncoder.encodeSpeechToText("sess1", "hello world", 0.95f, true)
        val result = AVUDecoder.parseSpeechToText(message)
        assertNotNull(result)
        assertEquals("sess1", result.sessionId)
        assertEquals("hello world", result.transcript)
        assertEquals(true, result.isFinal)
    }

    // ── roundtrip encode→decode ───────────────────────────────────────────────

    @Test
    fun roundtrip_voice_command_preserves_all_data() {
        val original = AVUEncoder.encodeVoiceCommand("cmd99", "SCROLL_DOWN")
        val parsed = AVUDecoder.parse(original)
        assertNotNull(parsed)
        val reEncoded = AVUEncoder.encodeGeneric(parsed.code, parsed.id, *parsed.params.toTypedArray())
        assertEquals(original, reEncoded)
    }

    // ── isCode helper ─────────────────────────────────────────────────────────

    @Test
    fun parsedMessage_isCode_returns_true_for_matching_code() {
        val parsed = AVUDecoder.parse("VCM:cmd1:CLICK")
        assertNotNull(parsed)
        assertTrue(parsed.isCode(AVUEncoder.CODE_VOICE_COMMAND))
    }
}
