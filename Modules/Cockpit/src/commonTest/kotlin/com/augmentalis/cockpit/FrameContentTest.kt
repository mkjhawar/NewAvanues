/**
 * FrameContentTest.kt — Unit tests for FrameContent sealed class
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests typeId values, companion constants, ALL_TYPES completeness,
 * defaults for each subtype, and structural equality.
 */
package com.augmentalis.cockpit

import com.augmentalis.cockpit.model.CameraLens
import com.augmentalis.cockpit.model.CastQuality
import com.augmentalis.cockpit.model.FlashMode
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.SummaryType
import com.augmentalis.cockpit.model.WidgetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrameContentTest {

    // ── typeId Constants ──────────────────────────────────────────

    @Test
    fun `typeId for each subtype matches the companion constant`() {
        assertEquals(FrameContent.TYPE_WEB, FrameContent.Web().typeId)
        assertEquals(FrameContent.TYPE_PDF, FrameContent.Pdf().typeId)
        assertEquals(FrameContent.TYPE_IMAGE, FrameContent.Image().typeId)
        assertEquals(FrameContent.TYPE_VIDEO, FrameContent.Video().typeId)
        assertEquals(FrameContent.TYPE_NOTE, FrameContent.Note().typeId)
        assertEquals(FrameContent.TYPE_CAMERA, FrameContent.Camera().typeId)
        assertEquals(FrameContent.TYPE_VOICE_NOTE, FrameContent.VoiceNote().typeId)
        assertEquals(FrameContent.TYPE_FORM, FrameContent.Form().typeId)
        assertEquals(FrameContent.TYPE_SIGNATURE, FrameContent.Signature().typeId)
        assertEquals(FrameContent.TYPE_VOICE, FrameContent.Voice().typeId)
        assertEquals(FrameContent.TYPE_MAP, FrameContent.Map().typeId)
        assertEquals(FrameContent.TYPE_WHITEBOARD, FrameContent.Whiteboard().typeId)
        assertEquals(FrameContent.TYPE_TERMINAL, FrameContent.Terminal().typeId)
        assertEquals(FrameContent.TYPE_AI_SUMMARY, FrameContent.AiSummary().typeId)
        assertEquals(FrameContent.TYPE_SCREEN_CAST, FrameContent.ScreenCast().typeId)
        assertEquals(FrameContent.TYPE_WIDGET, FrameContent.Widget().typeId)
        assertEquals(FrameContent.TYPE_FILE, FrameContent.File().typeId)
        assertEquals(FrameContent.TYPE_EXTERNAL_APP, FrameContent.ExternalApp().typeId)
    }

    // ── ALL_TYPES Completeness ────────────────────────────────────

    @Test
    fun `ALL_TYPES contains exactly 18 entries`() {
        assertEquals(18, FrameContent.ALL_TYPES.size)
    }

    @Test
    fun `ALL_TYPES contains all known type IDs`() {
        val expected = listOf(
            "web", "pdf", "image", "video", "note", "camera",
            "voice_note", "form", "signature", "voice",
            "map", "whiteboard", "terminal",
            "ai_summary", "screen_cast", "widget",
            "file", "external_app"
        )
        expected.forEach { typeId ->
            assertTrue(
                FrameContent.ALL_TYPES.contains(typeId),
                "ALL_TYPES missing: $typeId"
            )
        }
        FrameContent.ALL_TYPES.forEach { typeId ->
            assertTrue(
                expected.contains(typeId),
                "Unexpected type in ALL_TYPES: $typeId"
            )
        }
    }

    @Test
    fun `ALL_TYPES has no duplicates`() {
        assertEquals(FrameContent.ALL_TYPES.size, FrameContent.ALL_TYPES.distinct().size)
    }

    // ── Default Values ────────────────────────────────────────────

    @Test
    fun `Web defaults to google homepage with desktop mode enabled`() {
        val web = FrameContent.Web()
        assertEquals("https://www.google.com", web.url)
        assertTrue(web.desktopMode)
        assertEquals(0, web.scrollX)
        assertEquals(0, web.scrollY)
        assertEquals(1.0f, web.zoomLevel)
    }

    @Test
    fun `Camera defaults to BACK lens and flash OFF`() {
        val camera = FrameContent.Camera()
        assertEquals(CameraLens.BACK, camera.lensFacing)
        assertEquals(FlashMode.OFF, camera.flashMode)
        assertEquals(1.0f, camera.zoom)
    }

    @Test
    fun `Video defaults to not playing and not muted`() {
        val video = FrameContent.Video()
        assertFalse(video.isPlaying)
        assertFalse(video.isMuted)
        assertEquals(0L, video.playbackPositionMs)
        assertEquals(1.0f, video.playbackSpeed)
    }

    @Test
    fun `AiSummary defaults to BRIEF summary type with autoRefresh enabled`() {
        val ai = FrameContent.AiSummary()
        assertEquals(SummaryType.BRIEF, ai.summaryType)
        assertTrue(ai.autoRefresh)
        assertTrue(ai.sourceFrameIds.isEmpty())
    }

    @Test
    fun `Widget defaults to CLOCK type`() {
        val widget = FrameContent.Widget()
        assertEquals(WidgetType.CLOCK, widget.widgetType)
        assertEquals("{}", widget.configJson)
    }

    @Test
    fun `ScreenCast defaults to disconnected MEDIUM quality`() {
        val cast = FrameContent.ScreenCast()
        assertFalse(cast.isConnected)
        assertEquals(CastQuality.MEDIUM, cast.quality)
        assertEquals("", cast.sourceDeviceId)
    }

    // ── Structural Equality (data class) ─────────────────────────

    @Test
    fun `two Web instances with same url are equal`() {
        val a = FrameContent.Web(url = "https://example.com")
        val b = FrameContent.Web(url = "https://example.com")
        assertEquals(a, b)
    }

    @Test
    fun `copy preserves other fields when only one field changes`() {
        val original = FrameContent.Note(markdownContent = "Hello", fontSize = 16f)
        val modified = original.copy(fontSize = 20f)

        assertEquals("Hello", modified.markdownContent)
        assertEquals(20f, modified.fontSize)
        assertEquals(FrameContent.TYPE_NOTE, modified.typeId)
    }
}
