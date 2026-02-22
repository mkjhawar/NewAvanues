/**
 * HpackRoundtripTest.kt — Encode/decode roundtrip tests for HPACK
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests HpackEncoder + HpackDecoder together using in-memory byte arrays.
 * Validates static-table indexing, dynamic-table growth, literal headers,
 * and integer encoding with multi-byte continuations.
 * No socket I/O — pure Kotlin data structure tests.
 */
package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.hpack.HpackDecoder
import com.augmentalis.httpavanue.hpack.HpackEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HpackRoundtripTest {

    // ── Static Table Headers ──────────────────────────────────────

    @Test
    fun `static table GET method roundtrips correctly`() {
        // :method GET is static index 2 — encoder should use indexed representation
        val headers = listOf(":method" to "GET")
        val encoded = HpackEncoder().encode(headers)
        val decoded = HpackDecoder().decode(encoded)

        assertEquals(1, decoded.size)
        assertEquals(":method", decoded[0].first)
        assertEquals("GET", decoded[0].second)
    }

    @Test
    fun `static table POST method roundtrips correctly`() {
        val headers = listOf(":method" to "POST")
        assertRoundtrip(headers)
    }

    @Test
    fun `static table root path roundtrips correctly`() {
        val headers = listOf(":path" to "/")
        assertRoundtrip(headers)
    }

    @Test
    fun `static table https scheme roundtrips correctly`() {
        val headers = listOf(":scheme" to "https")
        assertRoundtrip(headers)
    }

    @Test
    fun `status 200 roundtrips correctly via static table`() {
        val headers = listOf(":status" to "200")
        assertRoundtrip(headers)
    }

    // ── Custom Headers (Literal + Dynamic Table) ──────────────────

    @Test
    fun `custom header with unknown name roundtrips correctly`() {
        val headers = listOf("x-custom-header" to "my-value")
        assertRoundtrip(headers)
    }

    @Test
    fun `multiple headers roundtrip preserving order`() {
        val headers = listOf(
            ":method" to "GET",
            ":path" to "/api/data",
            ":scheme" to "https",
            "content-type" to "application/json",
            "authorization" to "Bearer token123"
        )
        val decoded = roundtrip(headers)

        assertEquals(headers.size, decoded.size)
        headers.forEachIndexed { i, (name, value) ->
            assertEquals(name, decoded[i].first, "name mismatch at index $i")
            assertEquals(value, decoded[i].second, "value mismatch at index $i")
        }
    }

    @Test
    fun `empty header list encodes to empty byte array and decodes to empty list`() {
        val encoded = HpackEncoder().encode(emptyList())
        assertEquals(0, encoded.size)
        val decoded = HpackDecoder().decode(encoded)
        assertTrue(decoded.isEmpty())
    }

    // ── Dynamic Table Reuse ───────────────────────────────────────

    @Test
    fun `same header encoded twice with the same encoder produces same or smaller second encoding`() {
        val encoder = HpackEncoder()
        val decoder = HpackDecoder()
        val headers = listOf("x-session-id" to "abc123")

        val firstEncoded = encoder.encode(headers)
        val firstDecoded = decoder.decode(firstEncoded)
        assertEquals("abc123", firstDecoded.first { it.first == "x-session-id" }.second)

        // Second encode should use dynamic table — same or smaller
        val secondEncoded = encoder.encode(headers)
        val secondDecoded = decoder.decode(secondEncoded)
        assertEquals("abc123", secondDecoded.first { it.first == "x-session-id" }.second)
        assertTrue(secondEncoded.size <= firstEncoded.size,
            "Second encoding (${secondEncoded.size}B) should not be larger than first (${firstEncoded.size}B)"
        )
    }

    // ── Integer Encoding Edge Cases ───────────────────────────────

    @Test
    fun `header with long value triggers multi-byte integer encoding and decodes correctly`() {
        // A value long enough to exceed the 7-bit integer prefix boundary
        val longValue = "x".repeat(200)
        val headers = listOf("x-long" to longValue)
        val decoded = roundtrip(headers)

        assertEquals(longValue, decoded[0].second)
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun roundtrip(headers: List<Pair<String, String>>): List<Pair<String, String>> {
        val encoded = HpackEncoder().encode(headers)
        return HpackDecoder().decode(encoded)
    }

    private fun assertRoundtrip(headers: List<Pair<String, String>>) {
        val decoded = roundtrip(headers)
        assertEquals(headers.size, decoded.size)
        headers.forEachIndexed { i, (name, value) ->
            assertEquals(name, decoded[i].first)
            assertEquals(value, decoded[i].second)
        }
    }
}
