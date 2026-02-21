/**
 * WhisperModelsTest.kt - Tests for Whisper model size selection and properties
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests model enumeration, RAM-based selection, and model metadata.
 */
package com.augmentalis.speechrecognition

import com.augmentalis.speechrecognition.whisper.WhisperEngineState
import com.augmentalis.speechrecognition.whisper.WhisperModelSize
import com.augmentalis.speechrecognition.whisper.VADState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WhisperModelsTest {

    // ── Model Enumeration ────────────────────────────────────────

    @Test
    fun `all 8 model sizes are defined`() {
        assertEquals(8, WhisperModelSize.entries.size)
    }

    @Test
    fun `every model has non-blank display name`() {
        WhisperModelSize.entries.forEach { model ->
            assertTrue(model.displayName.isNotBlank(), "${model.name} has blank displayName")
        }
    }

    @Test
    fun `every model has non-blank ggmlFileName`() {
        WhisperModelSize.entries.forEach { model ->
            assertTrue(model.ggmlFileName.isNotBlank(), "${model.name} has blank ggmlFileName")
            assertTrue(model.ggmlFileName.startsWith("ggml-"), "${model.name} ggml filename doesn't start with ggml-")
            assertTrue(model.ggmlFileName.endsWith(".bin"), "${model.name} ggml filename doesn't end with .bin")
        }
    }

    @Test
    fun `every model has vlm-format vsmName`() {
        WhisperModelSize.entries.forEach { model ->
            assertTrue(model.vsmName.startsWith("VoiceOS-"), "${model.name} vsmName doesn't start with VoiceOS-")
            assertTrue(model.vsmName.endsWith(".vlm"), "${model.name} vsmName doesn't end with .vlm")
        }
    }

    @Test
    fun `model sizes increase from tiny to medium`() {
        assertTrue(WhisperModelSize.TINY.approxSizeMB < WhisperModelSize.BASE.approxSizeMB)
        assertTrue(WhisperModelSize.BASE.approxSizeMB < WhisperModelSize.SMALL.approxSizeMB)
        assertTrue(WhisperModelSize.SMALL.approxSizeMB < WhisperModelSize.MEDIUM.approxSizeMB)
    }

    @Test
    fun `english and multilingual variants have same size`() {
        assertEquals(WhisperModelSize.TINY.approxSizeMB, WhisperModelSize.TINY_EN.approxSizeMB)
        assertEquals(WhisperModelSize.BASE.approxSizeMB, WhisperModelSize.BASE_EN.approxSizeMB)
        assertEquals(WhisperModelSize.SMALL.approxSizeMB, WhisperModelSize.SMALL_EN.approxSizeMB)
        assertEquals(WhisperModelSize.MEDIUM.approxSizeMB, WhisperModelSize.MEDIUM_EN.approxSizeMB)
    }

    @Test
    fun `english models are flagged correctly`() {
        assertTrue(WhisperModelSize.TINY_EN.isEnglishOnly)
        assertTrue(WhisperModelSize.BASE_EN.isEnglishOnly)
        assertTrue(WhisperModelSize.SMALL_EN.isEnglishOnly)
        assertTrue(WhisperModelSize.MEDIUM_EN.isEnglishOnly)
    }

    @Test
    fun `multilingual models are not english-only`() {
        assertFalse(WhisperModelSize.TINY.isEnglishOnly)
        assertFalse(WhisperModelSize.BASE.isEnglishOnly)
        assertFalse(WhisperModelSize.SMALL.isEnglishOnly)
        assertFalse(WhisperModelSize.MEDIUM.isEnglishOnly)
    }

    @Test
    fun `relative speed increases with model size`() {
        assertTrue(WhisperModelSize.TINY.relativeSpeed < WhisperModelSize.BASE.relativeSpeed)
        assertTrue(WhisperModelSize.BASE.relativeSpeed < WhisperModelSize.SMALL.relativeSpeed)
        assertTrue(WhisperModelSize.SMALL.relativeSpeed < WhisperModelSize.MEDIUM.relativeSpeed)
    }

    // ── forAvailableRAM ──────────────────────────────────────────

    @Test
    fun `forAvailableRAM selects TINY for very low RAM`() {
        val model = WhisperModelSize.forAvailableRAM(256, englishOnly = false)
        assertEquals(WhisperModelSize.TINY, model)
    }

    @Test
    fun `forAvailableRAM selects TINY_EN for very low RAM english`() {
        val model = WhisperModelSize.forAvailableRAM(256, englishOnly = true)
        assertEquals(WhisperModelSize.TINY_EN, model)
    }

    @Test
    fun `forAvailableRAM selects BASE for 1GB RAM`() {
        val model = WhisperModelSize.forAvailableRAM(1024, englishOnly = false)
        assertEquals(WhisperModelSize.BASE, model)
    }

    @Test
    fun `forAvailableRAM selects SMALL for 2GB RAM`() {
        val model = WhisperModelSize.forAvailableRAM(2048, englishOnly = false)
        assertEquals(WhisperModelSize.SMALL, model)
    }

    @Test
    fun `forAvailableRAM selects MEDIUM for 4GB+ RAM`() {
        val model = WhisperModelSize.forAvailableRAM(4096, englishOnly = false)
        assertEquals(WhisperModelSize.MEDIUM, model)
    }

    @Test
    fun `forAvailableRAM selects MEDIUM_EN for 4GB+ RAM english`() {
        val model = WhisperModelSize.forAvailableRAM(4096, englishOnly = true)
        assertEquals(WhisperModelSize.MEDIUM_EN, model)
    }

    @Test
    fun `forAvailableRAM falls back to TINY for insufficient RAM`() {
        val model = WhisperModelSize.forAvailableRAM(100, englishOnly = false)
        assertEquals(WhisperModelSize.TINY, model)
    }

    @Test
    fun `forAvailableRAM english and multilingual pick differently`() {
        val multi = WhisperModelSize.forAvailableRAM(2048, englishOnly = false)
        val english = WhisperModelSize.forAvailableRAM(2048, englishOnly = true)
        assertNotEquals(multi, english)
        assertFalse(multi.isEnglishOnly)
        assertTrue(english.isEnglishOnly)
    }

    // ── WhisperEngineState ───────────────────────────────────────

    @Test
    fun `all 8 engine states are defined`() {
        assertEquals(8, WhisperEngineState.entries.size)
    }

    @Test
    fun `engine states include full lifecycle`() {
        val states = WhisperEngineState.entries.map { it.name }
        assertTrue("UNINITIALIZED" in states)
        assertTrue("LOADING_MODEL" in states)
        assertTrue("READY" in states)
        assertTrue("LISTENING" in states)
        assertTrue("PROCESSING" in states)
        assertTrue("PAUSED" in states)
        assertTrue("ERROR" in states)
        assertTrue("DESTROYED" in states)
    }

    // ── VADState ─────────────────────────────────────────────────

    @Test
    fun `all 3 VAD states are defined`() {
        assertEquals(3, VADState.entries.size)
    }

    @Test
    fun `VAD states are SILENCE SPEECH HANGOVER`() {
        val states = VADState.entries.map { it.name }
        assertTrue("SILENCE" in states)
        assertTrue("SPEECH" in states)
        assertTrue("HANGOVER" in states)
    }
}
