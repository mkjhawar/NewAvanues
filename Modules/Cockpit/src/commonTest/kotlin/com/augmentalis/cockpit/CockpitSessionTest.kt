/**
 * CockpitSessionTest.kt — Unit tests for CockpitSession
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Covers construction defaults, copy semantics, and flag combinations.
 */
package com.augmentalis.cockpit

import com.augmentalis.cockpit.model.CockpitSession
import com.augmentalis.cockpit.model.LayoutMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CockpitSessionTest {

    // ── Construction & Defaults ───────────────────────────────────

    @Test
    fun `session created with required fields has correct defaults`() {
        val session = CockpitSession(id = "s1", name = "Research")

        assertEquals("s1", session.id)
        assertEquals("Research", session.name)
        assertEquals(LayoutMode.DEFAULT, session.layoutMode)
        assertTrue(session.workflowSteps.isEmpty())
        assertNull(session.selectedFrameId)
        assertFalse(session.isDefault)
        assertNull(session.backgroundUri)
        assertEquals("", session.createdAt)
        assertEquals("", session.updatedAt)
    }

    @Test
    fun `LayoutMode DEFAULT resolves to COCKPIT`() {
        assertEquals(LayoutMode.COCKPIT, LayoutMode.DEFAULT)
        val session = CockpitSession(id = "s2", name = "Test")
        assertEquals(LayoutMode.COCKPIT, session.layoutMode)
    }

    @Test
    fun `session can be created as the default session`() {
        val session = CockpitSession(id = "default-1", name = "Home", isDefault = true)
        assertTrue(session.isDefault)
    }

    // ── Copy Semantics ────────────────────────────────────────────

    @Test
    fun `copy with changed layoutMode preserves other fields`() {
        val original = CockpitSession(
            id = "s3",
            name = "Work",
            isDefault = false,
            selectedFrameId = "f1"
        )
        val updated = original.copy(layoutMode = LayoutMode.GRID)

        assertEquals(LayoutMode.GRID, updated.layoutMode)
        assertEquals("s3", updated.id)
        assertEquals("Work", updated.name)
        assertEquals("f1", updated.selectedFrameId)
        assertFalse(updated.isDefault)
    }

    @Test
    fun `copy with new selectedFrameId updates selection only`() {
        val session = CockpitSession(id = "s4", name = "Meeting", selectedFrameId = null)
        val withSelection = session.copy(selectedFrameId = "frame-42")

        assertEquals("frame-42", withSelection.selectedFrameId)
        assertEquals(session.id, withSelection.id)
        assertEquals(session.name, withSelection.name)
    }

    @Test
    fun `two sessions with same fields are equal`() {
        val a = CockpitSession(id = "x", name = "Alpha", layoutMode = LayoutMode.SPLIT_LEFT)
        val b = CockpitSession(id = "x", name = "Alpha", layoutMode = LayoutMode.SPLIT_LEFT)
        assertEquals(a, b)
    }

    // ── Layout Mode Variants ──────────────────────────────────────

    @Test
    fun `session accepts all LayoutMode values`() {
        LayoutMode.entries.forEach { mode ->
            val session = CockpitSession(id = "id", name = "n", layoutMode = mode)
            assertEquals(mode, session.layoutMode)
        }
    }

    @Test
    fun `backgroundUri can be set to any string`() {
        val session = CockpitSession(
            id = "bg",
            name = "Themed",
            backgroundUri = "content://backgrounds/night_sky.jpg"
        )
        assertEquals("content://backgrounds/night_sky.jpg", session.backgroundUri)
    }
}
