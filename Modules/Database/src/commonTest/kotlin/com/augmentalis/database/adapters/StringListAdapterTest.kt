package com.augmentalis.database.adapters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for StringListAdapter and booleanAdapter — SQLDelight column adapters.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class StringListAdapterTest {

    // ── stringListAdapter — encode ────────────────────────────────────────────

    @Test
    fun encode_empty_list_produces_empty_json_array() {
        val encoded = stringListAdapter.encode(emptyList())
        assertEquals("[]", encoded)
    }

    @Test
    fun encode_single_element_list() {
        val encoded = stringListAdapter.encode(listOf("hello"))
        assertTrue(encoded.contains("hello"))
        assertTrue(encoded.startsWith("["))
        assertTrue(encoded.endsWith("]"))
    }

    @Test
    fun encode_multiple_elements() {
        val encoded = stringListAdapter.encode(listOf("a", "b", "c"))
        assertTrue(encoded.contains("\"a\""))
        assertTrue(encoded.contains("\"b\""))
        assertTrue(encoded.contains("\"c\""))
    }

    // ── stringListAdapter — decode ────────────────────────────────────────────

    @Test
    fun decode_empty_json_array_returns_empty_list() {
        val result = stringListAdapter.decode("[]")
        assertEquals(emptyList(), result)
    }

    @Test
    fun decode_json_array_with_strings() {
        val result = stringListAdapter.decode("""["go back","navigate back"]""")
        assertEquals(listOf("go back", "navigate back"), result)
    }

    @Test
    fun decode_malformed_json_returns_empty_list() {
        // decode must never throw — returns empty list on bad input
        val result = stringListAdapter.decode("{not valid json}")
        assertEquals(emptyList(), result)
    }

    @Test
    fun decode_empty_string_returns_empty_list() {
        val result = stringListAdapter.decode("")
        assertEquals(emptyList(), result)
    }

    // ── stringListAdapter — roundtrip ─────────────────────────────────────────

    @Test
    fun roundtrip_preserves_list_contents_and_order() {
        val original = listOf("play music", "pause", "stop", "next track")
        val encoded = stringListAdapter.encode(original)
        val decoded = stringListAdapter.decode(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun roundtrip_with_special_characters_in_strings() {
        val original = listOf("say \"hello\"", "open app: settings", "100%")
        val encoded = stringListAdapter.encode(original)
        val decoded = stringListAdapter.decode(encoded)
        assertEquals(original, decoded)
    }

    // ── booleanAdapter ────────────────────────────────────────────────────────

    @Test
    fun booleanAdapter_encodes_true_as_1() {
        assertEquals(1L, booleanAdapter.encode(true))
    }

    @Test
    fun booleanAdapter_encodes_false_as_0() {
        assertEquals(0L, booleanAdapter.encode(false))
    }

    @Test
    fun booleanAdapter_decodes_1_as_true() {
        assertEquals(true, booleanAdapter.decode(1L))
    }

    @Test
    fun booleanAdapter_decodes_0_as_false() {
        assertEquals(false, booleanAdapter.decode(0L))
    }

    @Test
    fun booleanAdapter_decodes_any_nonzero_as_true() {
        // SQLite stores non-zero as truthy
        assertEquals(true, booleanAdapter.decode(42L))
        assertEquals(true, booleanAdapter.decode(-1L))
    }

    @Test
    fun booleanAdapter_roundtrip_true() {
        assertEquals(true, booleanAdapter.decode(booleanAdapter.encode(true)))
    }

    @Test
    fun booleanAdapter_roundtrip_false() {
        assertEquals(false, booleanAdapter.decode(booleanAdapter.encode(false)))
    }
}
