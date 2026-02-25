/**
 * LayoutModeTest.kt — Unit tests for LayoutMode enum
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests fromString lookup, DEFAULT constant, SPATIAL_CAPABLE set,
 * GALLERY_CONTENT_TYPES, and enum completeness.
 */
package com.augmentalis.cockpit

import com.augmentalis.cockpit.model.LayoutMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutModeTest {

    // ── DEFAULT ───────────────────────────────────────────────────

    @Test
    fun `DEFAULT is COCKPIT`() {
        assertEquals(LayoutMode.COCKPIT, LayoutMode.DEFAULT)
    }

    // ── fromString ────────────────────────────────────────────────

    @Test
    fun `fromString resolves exact uppercase name`() {
        assertEquals(LayoutMode.GRID, LayoutMode.fromString("GRID"))
        assertEquals(LayoutMode.FULLSCREEN, LayoutMode.fromString("FULLSCREEN"))
        assertEquals(LayoutMode.WORKFLOW, LayoutMode.fromString("WORKFLOW"))
    }

    @Test
    fun `fromString is case-insensitive`() {
        assertEquals(LayoutMode.SPLIT_LEFT, LayoutMode.fromString("split_left"))
        assertEquals(LayoutMode.CAROUSEL, LayoutMode.fromString("Carousel"))
        assertEquals(LayoutMode.SPATIAL_DICE, LayoutMode.fromString("spatial_dice"))
    }

    @Test
    fun `fromString returns DEFAULT for unknown value`() {
        assertEquals(LayoutMode.DEFAULT, LayoutMode.fromString("UNKNOWN_MODE"))
        assertEquals(LayoutMode.DEFAULT, LayoutMode.fromString(""))
        assertEquals(LayoutMode.DEFAULT, LayoutMode.fromString("not_a_layout"))
    }

    @Test
    fun `fromString round-trips all enum names`() {
        LayoutMode.entries.forEach { mode ->
            assertEquals(mode, LayoutMode.fromString(mode.name))
        }
    }

    // ── SPATIAL_CAPABLE ───────────────────────────────────────────

    @Test
    fun `SPATIAL_CAPABLE contains expected modes`() {
        val expected = setOf(
            LayoutMode.FREEFORM, LayoutMode.COCKPIT, LayoutMode.MOSAIC,
            LayoutMode.T_PANEL, LayoutMode.SPATIAL_DICE
        )
        assertEquals(expected, LayoutMode.SPATIAL_CAPABLE)
    }

    @Test
    fun `GRID and FULLSCREEN are NOT in SPATIAL_CAPABLE`() {
        assertFalse(LayoutMode.SPATIAL_CAPABLE.contains(LayoutMode.GRID))
        assertFalse(LayoutMode.SPATIAL_CAPABLE.contains(LayoutMode.FULLSCREEN))
    }

    // ── GALLERY_CONTENT_TYPES ─────────────────────────────────────

    @Test
    fun `GALLERY_CONTENT_TYPES contains image video camera and screen_cast`() {
        assertTrue(LayoutMode.GALLERY_CONTENT_TYPES.contains("image"))
        assertTrue(LayoutMode.GALLERY_CONTENT_TYPES.contains("video"))
        assertTrue(LayoutMode.GALLERY_CONTENT_TYPES.contains("camera"))
        assertTrue(LayoutMode.GALLERY_CONTENT_TYPES.contains("screen_cast"))
    }

    @Test
    fun `GALLERY_CONTENT_TYPES does not contain note or web`() {
        assertFalse(LayoutMode.GALLERY_CONTENT_TYPES.contains("note"))
        assertFalse(LayoutMode.GALLERY_CONTENT_TYPES.contains("web"))
    }

    // ── Completeness ──────────────────────────────────────────────

    @Test
    fun `LayoutMode has exactly 14 members`() {
        assertEquals(14, LayoutMode.entries.size)
    }
}
