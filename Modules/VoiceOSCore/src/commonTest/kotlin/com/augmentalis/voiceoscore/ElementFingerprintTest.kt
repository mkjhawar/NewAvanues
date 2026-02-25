package com.augmentalis.voiceoscore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ElementFingerprint — deterministic element identifier generation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class ElementFingerprintTest {

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    fun generate_same_inputs_produce_same_fingerprint() {
        val fp1 = ElementFingerprint.generate(
            className = "android.widget.Button",
            packageName = "com.example.app",
            resourceId = "com.example.app:id/submit",
            text = "Submit"
        )
        val fp2 = ElementFingerprint.generate(
            className = "android.widget.Button",
            packageName = "com.example.app",
            resourceId = "com.example.app:id/submit",
            text = "Submit"
        )
        assertEquals(fp1, fp2)
    }

    @Test
    fun generate_different_text_produces_different_fingerprint() {
        val fp1 = ElementFingerprint.generate(
            className = "android.widget.Button",
            packageName = "com.example.app",
            text = "Submit"
        )
        val fp2 = ElementFingerprint.generate(
            className = "android.widget.Button",
            packageName = "com.example.app",
            text = "Cancel"
        )
        assertNotEquals(fp1, fp2)
    }

    @Test
    fun generate_different_package_produces_different_fingerprint() {
        val fp1 = ElementFingerprint.generate(
            className = "android.widget.Button",
            packageName = "com.app.one",
            text = "OK"
        )
        val fp2 = ElementFingerprint.generate(
            className = "android.widget.Button",
            packageName = "com.app.two",
            text = "OK"
        )
        assertNotEquals(fp1, fp2)
    }

    @Test
    fun generate_different_class_produces_different_fingerprint() {
        val fp1 = ElementFingerprint.generate("android.widget.Button", "com.app", text = "Save")
        val fp2 = ElementFingerprint.generate("android.widget.TextView", "com.app", text = "Save")
        assertNotEquals(fp1, fp2)
    }

    // ── Format validation ─────────────────────────────────────────────────────

    @Test
    fun generate_produces_typeCode_colon_hash8_format() {
        val fp = ElementFingerprint.generate(
            className = "android.widget.Button",
            packageName = "com.example.app",
            text = "Login"
        )
        val parts = fp.split(":")
        assertEquals(2, parts.size)
        // Type code: 3 uppercase letters
        assertEquals(3, parts[0].length)
        assertTrue(parts[0].all { it.isUpperCase() })
        // Hash: 8 hex characters
        assertEquals(8, parts[1].length)
        assertTrue(parts[1].all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun isValid_returns_true_for_correct_format() {
        val fp = ElementFingerprint.generate("android.widget.Button", "com.app", text = "Click")
        assertTrue(ElementFingerprint.isValid(fp))
    }

    @Test
    fun isValid_returns_false_for_malformed_string() {
        assertFalse(ElementFingerprint.isValid("not-a-fingerprint"))
        assertFalse(ElementFingerprint.isValid("BTN:shortHash"))
        assertFalse(ElementFingerprint.isValid("btn:a3f2e1c9")) // lowercase code
        assertFalse(ElementFingerprint.isValid("BTN:a3f2e1c9X")) // 9-char hash
    }

    // ── parse ────────────────────────────────────────────────────────────────

    @Test
    fun parse_valid_fingerprint_returns_pair() {
        val fp = ElementFingerprint.generate("android.widget.Button", "com.app", text = "OK")
        val parsed = ElementFingerprint.parse(fp)
        assertNotNull(parsed)
        assertEquals(3, parsed.first.length)
        assertEquals(8, parsed.second.length)
    }

    @Test
    fun parse_invalid_fingerprint_returns_null() {
        assertNull(ElementFingerprint.parse("invalid"))
        assertNull(ElementFingerprint.parse("BTN:hash:extra"))
    }

    // ── getTypeCode ───────────────────────────────────────────────────────────

    @Test
    fun getTypeCode_returns_three_uppercase_letters() {
        val code = ElementFingerprint.getTypeCode("android.widget.Button")
        assertEquals(3, code.length)
        assertTrue(code.all { it.isUpperCase() })
    }

    // ── deterministicHash ─────────────────────────────────────────────────────

    @Test
    fun deterministicHash_same_input_same_output() {
        val h1 = ElementFingerprint.deterministicHash("test-input", 8)
        val h2 = ElementFingerprint.deterministicHash("test-input", 8)
        assertEquals(h1, h2)
    }

    @Test
    fun deterministicHash_different_input_different_output() {
        val h1 = ElementFingerprint.deterministicHash("input-a", 8)
        val h2 = ElementFingerprint.deterministicHash("input-b", 8)
        assertNotEquals(h1, h2)
    }
}
