package com.avanues.avu.codec

import com.avanues.avu.codec.core.AvuEscape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for AvuEscape — percent-encoding utility for AVU wire format.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class AvuEscapeTest {

    // ── escape ────────────────────────────────────────────────────────────────

    @Test
    fun escape_colon_is_encoded() {
        val result = AvuEscape.escape("hello:world")
        assertEquals("hello%3Aworld", result)
    }

    @Test
    fun escape_percent_is_encoded_first_preventing_double_encoding() {
        // A raw '%' must become '%25', not '%253A' if a colon follows later
        val result = AvuEscape.escape("100%:done")
        // % → %25 first, then : → %3A
        assertEquals("100%25%3Adone", result)
    }

    @Test
    fun escape_newline_and_carriage_return() {
        val result = AvuEscape.escape("line1\nline2\rend")
        assertEquals("line1%0Aline2%0Dend", result)
    }

    @Test
    fun escape_clean_string_is_unchanged() {
        val input = "hello world"
        assertEquals(input, AvuEscape.escape(input))
    }

    @Test
    fun escape_url_with_colon_and_slashes() {
        val url = "https://example.com:8080/path"
        val escaped = AvuEscape.escape(url)
        assertEquals("https%3A//example.com%3A8080/path", escaped)
    }

    // ── unescape ──────────────────────────────────────────────────────────────

    @Test
    fun unescape_restores_colon() {
        val result = AvuEscape.unescape("hello%3Aworld")
        assertEquals("hello:world", result)
    }

    @Test
    fun unescape_restores_percent_last_preventing_double_decode() {
        // '%25' must decode to '%', and must not re-interpret '%3A' inside it
        val result = AvuEscape.unescape("100%25%3Adone")
        assertEquals("100%:done", result)
    }

    @Test
    fun unescape_restores_newline_and_carriage_return() {
        val result = AvuEscape.unescape("line1%0Aline2%0Dend")
        assertEquals("line1\nline2\rend", result)
    }

    // ── roundtrip ────────────────────────────────────────────────────────────

    @Test
    fun roundtrip_all_special_chars() {
        val original = "https://api.example.com:443/query?a=1%2B2\nresult\r"
        val encoded = AvuEscape.escape(original)
        val decoded = AvuEscape.unescape(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun roundtrip_empty_string() {
        val original = ""
        assertEquals(original, AvuEscape.unescape(AvuEscape.escape(original)))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @Test
    fun needsEscaping_detects_colon() {
        assertTrue(AvuEscape.needsEscaping("a:b"))
    }

    @Test
    fun needsEscaping_detects_percent() {
        assertTrue(AvuEscape.needsEscaping("50% done"))
    }

    @Test
    fun needsEscaping_false_for_plain_text() {
        assertFalse(AvuEscape.needsEscaping("hello world"))
    }

    @Test
    fun escapeIfNeeded_skips_allocation_for_plain_text() {
        val input = "plain text"
        val result = AvuEscape.escapeIfNeeded(input)
        // Returns same content (no escaping needed)
        assertEquals(input, result)
    }

    @Test
    fun escapeIfNeeded_encodes_when_necessary() {
        val result = AvuEscape.escapeIfNeeded("key:value")
        assertEquals("key%3Avalue", result)
    }
}
