/**
 * SpeechEngineTest.kt - Tests for speech engine enumeration
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Validates engine properties, capabilities, and metadata methods.
 */
package com.augmentalis.speechrecognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechEngineTest {

    @Test
    fun `all 8 engines are defined`() {
        assertEquals(8, SpeechEngine.entries.size)
    }

    // ── Offline Capability ───────────────────────────────────────

    @Test
    fun `offline capable engines include VOSK VIVOKA WHISPER APPLE_SPEECH`() {
        assertTrue(SpeechEngine.VOSK.isOfflineCapable())
        assertTrue(SpeechEngine.VIVOKA.isOfflineCapable())
        assertTrue(SpeechEngine.WHISPER.isOfflineCapable())
        assertTrue(SpeechEngine.APPLE_SPEECH.isOfflineCapable())
    }

    @Test
    fun `online-only engines are not offline capable`() {
        assertFalse(SpeechEngine.ANDROID_STT.isOfflineCapable())
        assertFalse(SpeechEngine.GOOGLE_CLOUD.isOfflineCapable())
        assertFalse(SpeechEngine.AZURE.isOfflineCapable())
        assertFalse(SpeechEngine.WEB_SPEECH.isOfflineCapable())
    }

    // ── API Key Requirement ──────────────────────────────────────

    @Test
    fun `only GOOGLE_CLOUD and AZURE require API key`() {
        assertTrue(SpeechEngine.GOOGLE_CLOUD.requiresApiKey())
        assertTrue(SpeechEngine.AZURE.requiresApiKey())
    }

    @Test
    fun `other engines do not require API key`() {
        assertFalse(SpeechEngine.VOSK.requiresApiKey())
        assertFalse(SpeechEngine.VIVOKA.requiresApiKey())
        assertFalse(SpeechEngine.ANDROID_STT.requiresApiKey())
        assertFalse(SpeechEngine.WHISPER.requiresApiKey())
        assertFalse(SpeechEngine.APPLE_SPEECH.requiresApiKey())
        assertFalse(SpeechEngine.WEB_SPEECH.requiresApiKey())
    }

    // ── Display Names ────────────────────────────────────────────

    @Test
    fun `every engine has a non-blank display name`() {
        SpeechEngine.entries.forEach { engine ->
            assertTrue(engine.getDisplayName().isNotBlank(), "Engine $engine has blank display name")
        }
    }

    @Test
    fun `display names match expected values`() {
        assertEquals("VOSK Offline", SpeechEngine.VOSK.getDisplayName())
        assertEquals("OpenAI Whisper", SpeechEngine.WHISPER.getDisplayName())
        assertEquals("Google Cloud Speech", SpeechEngine.GOOGLE_CLOUD.getDisplayName())
        assertEquals("Azure Cognitive Services", SpeechEngine.AZURE.getDisplayName())
    }

    // ── Memory Usage ─────────────────────────────────────────────

    @Test
    fun `every engine has positive memory estimate`() {
        SpeechEngine.entries.forEach { engine ->
            assertTrue(engine.getEstimatedMemoryUsage() > 0, "Engine $engine has zero/negative memory")
        }
    }

    @Test
    fun `whisper has highest memory usage`() {
        val maxMem = SpeechEngine.entries.maxOf { it.getEstimatedMemoryUsage() }
        assertEquals(SpeechEngine.WHISPER.getEstimatedMemoryUsage(), maxMem)
    }

    @Test
    fun `web speech has lowest memory usage`() {
        val minMem = SpeechEngine.entries.minOf { it.getEstimatedMemoryUsage() }
        assertEquals(SpeechEngine.WEB_SPEECH.getEstimatedMemoryUsage(), minMem)
    }
}
