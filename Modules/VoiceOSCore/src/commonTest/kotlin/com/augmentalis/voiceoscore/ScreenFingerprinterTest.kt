package com.augmentalis.voiceoscore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ScreenFingerprinter and FingerprintUtils — screen identity hashing.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class ScreenFingerprinterTest {

    private val fingerprinter = ScreenFingerprinter()

    private fun makeElement(
        className: String,
        text: String = "",
        contentDesc: String = "",
        resourceId: String = "",
        isClickable: Boolean = false,
        left: Int = 0,
        top: Int = 0
    ) = ElementInfo(
        className = className,
        text = text,
        contentDescription = contentDesc,
        resourceId = resourceId,
        isClickable = isClickable,
        bounds = Bounds(left, top, left + 100, top + 50)
    )

    // ── calculateFingerprint ──────────────────────────────────────────────────

    @Test
    fun calculateFingerprint_null_root_returns_empty_hash() {
        val hash = fingerprinter.calculateFingerprint(null)
        assertEquals(FingerprintUtils.EMPTY_HASH, hash)
    }

    @Test
    fun calculateFingerprint_empty_list_returns_empty_hash() {
        val hash = fingerprinter.calculateFingerprint(emptyList<ElementInfo>())
        assertEquals(FingerprintUtils.EMPTY_HASH, hash)
    }

    @Test
    fun calculateFingerprint_same_elements_produce_same_hash() {
        val elements = listOf(
            makeElement("Button", text = "Submit", left = 10, top = 20),
            makeElement("TextView", text = "Hello", left = 10, top = 80)
        )
        val h1 = fingerprinter.calculateFingerprint(elements)
        val h2 = fingerprinter.calculateFingerprint(elements)
        assertEquals(h1, h2)
    }

    @Test
    fun calculateFingerprint_different_text_produces_different_hash() {
        val elements1 = listOf(makeElement("Button", text = "Submit"))
        val elements2 = listOf(makeElement("Button", text = "Cancel"))
        assertNotEquals(
            fingerprinter.calculateFingerprint(elements1),
            fingerprinter.calculateFingerprint(elements2)
        )
    }

    // ── calculateStructuralFingerprint ────────────────────────────────────────

    @Test
    fun calculateStructuralFingerprint_same_structure_same_hash_regardless_of_text() {
        val elements1 = listOf(makeElement("Button", text = "Day 1", resourceId = "btn_submit"))
        val elements2 = listOf(makeElement("Button", text = "Day 2", resourceId = "btn_submit"))
        // Structure uses className+resourceId+isClickable — text differences don't change structural hash
        assertEquals(
            fingerprinter.calculateStructuralFingerprint(elements1),
            fingerprinter.calculateStructuralFingerprint(elements2)
        )
    }

    @Test
    fun calculateStructuralFingerprint_different_class_produces_different_hash() {
        val elements1 = listOf(makeElement("Button"))
        val elements2 = listOf(makeElement("TextView"))
        assertNotEquals(
            fingerprinter.calculateStructuralFingerprint(elements1),
            fingerprinter.calculateStructuralFingerprint(elements2)
        )
    }

    // ── isDynamicContentScreen ────────────────────────────────────────────────

    @Test
    fun isDynamicContentScreen_false_for_static_elements() {
        val elements = listOf(
            makeElement("TextView", text = "Settings"),
            makeElement("Button", text = "Save")
        )
        assertFalse(fingerprinter.isDynamicContentScreen(elements))
    }

    @Test
    fun isDynamicContentScreen_true_when_many_elements_have_timestamps() {
        // More than 20% of elements have time-like patterns
        val elements = (1..5).map { i ->
            makeElement("TextView", text = "$i:30 PM")
        }
        assertTrue(fingerprinter.isDynamicContentScreen(elements))
    }

    // ── detectPopup ───────────────────────────────────────────────────────────

    @Test
    fun detectPopup_returns_false_for_regular_elements() {
        val elements = listOf(
            makeElement("Button", text = "Submit"),
            makeElement("TextView", text = "Welcome")
        )
        val info = fingerprinter.detectPopup(elements)
        assertFalse(info.isPopup)
    }

    @Test
    fun detectPopup_detects_AlertDialog_class() {
        val elements = listOf(
            makeElement("android.app.AlertDialog", text = "Warning"),
            makeElement("Button", text = "OK"),
            makeElement("Button", text = "Cancel")
        )
        val info = fingerprinter.detectPopup(elements)
        assertTrue(info.isPopup)
        assertEquals(PopupType.ALERT, info.popupType)
        assertTrue(info.hasPositiveAction)
        assertTrue(info.hasNegativeAction)
    }

    // ── FingerprintUtils.normalizeText ────────────────────────────────────────

    @Test
    fun normalizeText_replaces_timestamp() {
        val result = FingerprintUtils.normalizeText("Meeting at 3:45 PM")
        assertTrue(result.contains("[TIME]"))
        assertFalse(result.contains("3:45"))
    }

    @Test
    fun normalizeText_replaces_badge_count() {
        val result = FingerprintUtils.normalizeText("Notifications (5)")
        assertTrue(result.contains("[COUNT]"))
        assertFalse(result.contains("(5)"))
    }

    @Test
    fun normalizeText_returns_empty_for_null() {
        assertEquals("", FingerprintUtils.normalizeText(null))
    }

    // ── isSameScreen ─────────────────────────────────────────────────────────

    @Test
    fun screenState_isSameScreen_true_for_identical_hash() {
        val s1 = ScreenState(hash = "abc123", isPopup = false)
        val s2 = ScreenState(hash = "abc123", isPopup = false)
        assertTrue(s1.isSameScreen(s2))
    }

    @Test
    fun screenState_isSameScreen_false_for_different_hash() {
        val s1 = ScreenState(hash = "abc123")
        val s2 = ScreenState(hash = "xyz789")
        assertFalse(s1.isSameScreen(s2))
    }

    @Test
    fun screenState_isSameScreen_false_popup_vs_regular() {
        val popup = ScreenState(hash = "abc123", isPopup = true)
        val regular = ScreenState(hash = "abc123", isPopup = false)
        assertFalse(popup.isSameScreen(regular))
    }
}
