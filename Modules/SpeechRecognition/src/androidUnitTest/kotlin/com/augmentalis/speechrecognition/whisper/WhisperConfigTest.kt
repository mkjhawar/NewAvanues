/**
 * WhisperConfigTest.kt - Tests for Whisper engine configuration
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests validation, thread count calculation, and model path resolution.
 * Uses JUnit4 (no Robolectric needed for non-Context methods).
 */
package com.augmentalis.speechrecognition.whisper

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhisperConfigTest {

    // ── Default Values ───────────────────────────────────────────

    @Test
    fun `defaults have sensible values`() {
        val config = WhisperConfig()

        assertEquals(WhisperModelSize.BASE, config.modelSize)
        assertEquals("en", config.language)
        assertFalse(config.translateToEnglish)
        assertEquals(0, config.numThreads)
        assertEquals(null, config.customModelPath)
        assertEquals(0.6f, config.vadSensitivity)
        assertEquals(700L, config.silenceThresholdMs)
        assertEquals(300L, config.minSpeechDurationMs)
        assertEquals(30_000L, config.maxChunkDurationMs)
        assertFalse(config.useQuantized)
    }

    // ── effectiveThreadCount ─────────────────────────────────────

    @Test
    fun `effectiveThreadCount returns explicit value when set`() {
        val config = WhisperConfig(numThreads = 3)
        assertEquals(3, config.effectiveThreadCount())
    }

    @Test
    fun `effectiveThreadCount auto-detects when zero`() {
        val config = WhisperConfig(numThreads = 0)
        val threads = config.effectiveThreadCount()

        // Auto-detection: cores / 2, clamped to [2, 4]
        assertTrue(threads >= 2, "Auto threads should be >= 2, was $threads")
        assertTrue(threads <= 4, "Auto threads should be <= 4, was $threads")
    }

    @Test
    fun `effectiveThreadCount returns 1 when explicitly set to 1`() {
        val config = WhisperConfig(numThreads = 1)
        assertEquals(1, config.effectiveThreadCount())
    }

    @Test
    fun `effectiveThreadCount returns large value when explicitly set`() {
        val config = WhisperConfig(numThreads = 8)
        assertEquals(8, config.effectiveThreadCount())
    }

    // ── Validation ───────────────────────────────────────────────

    @Test
    fun `validate passes for default config`() {
        assertTrue(WhisperConfig().validate().isSuccess)
    }

    @Test
    fun `validate fails for vadSensitivity below 0`() {
        val config = WhisperConfig(vadSensitivity = -0.1f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for vadSensitivity above 1`() {
        val config = WhisperConfig(vadSensitivity = 1.1f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate passes for vadSensitivity at boundaries`() {
        assertTrue(WhisperConfig(vadSensitivity = 0.0f).validate().isSuccess)
        assertTrue(WhisperConfig(vadSensitivity = 1.0f).validate().isSuccess)
    }

    @Test
    fun `validate fails for silenceThresholdMs below 100`() {
        val config = WhisperConfig(silenceThresholdMs = 50)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for silenceThresholdMs above 5000`() {
        val config = WhisperConfig(silenceThresholdMs = 6000)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate passes for silenceThresholdMs at boundaries`() {
        assertTrue(WhisperConfig(silenceThresholdMs = 100).validate().isSuccess)
        assertTrue(WhisperConfig(silenceThresholdMs = 5000).validate().isSuccess)
    }

    @Test
    fun `validate fails for minSpeechDurationMs below 50`() {
        val config = WhisperConfig(minSpeechDurationMs = 30)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for minSpeechDurationMs above 5000`() {
        val config = WhisperConfig(minSpeechDurationMs = 6000)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate passes for minSpeechDurationMs at boundaries`() {
        assertTrue(WhisperConfig(minSpeechDurationMs = 50).validate().isSuccess)
        assertTrue(WhisperConfig(minSpeechDurationMs = 5000).validate().isSuccess)
    }

    @Test
    fun `validate fails for English-only model with non-English language`() {
        val config = WhisperConfig(
            modelSize = WhisperModelSize.BASE_EN,
            language = "es"
        )
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("English-only") == true)
    }

    @Test
    fun `validate passes for English-only model with English language`() {
        val config = WhisperConfig(
            modelSize = WhisperModelSize.BASE_EN,
            language = "en"
        )
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes for English-only model with auto language`() {
        val config = WhisperConfig(
            modelSize = WhisperModelSize.TINY_EN,
            language = "auto"
        )
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes for multilingual model with any language`() {
        val config = WhisperConfig(
            modelSize = WhisperModelSize.BASE,
            language = "fr"
        )
        assertTrue(config.validate().isSuccess)
    }

    // ── Model Properties ─────────────────────────────────────────

    @Test
    fun `modelSize ggmlFileName is accessible`() {
        val config = WhisperConfig(modelSize = WhisperModelSize.TINY)
        assertEquals("ggml-tiny.bin", config.modelSize.ggmlFileName)
    }

    @Test
    fun `modelSize displayName is human-readable`() {
        val config = WhisperConfig(modelSize = WhisperModelSize.SMALL_EN)
        assertEquals("Small (English)", config.modelSize.displayName)
    }

    // ── Data Class Equality ──────────────────────────────────────

    @Test
    fun `configs with same values are equal`() {
        val a = WhisperConfig(modelSize = WhisperModelSize.SMALL, language = "fr")
        val b = WhisperConfig(modelSize = WhisperModelSize.SMALL, language = "fr")
        assertEquals(a, b)
    }

    @Test
    fun `copy preserves all fields`() {
        val config = WhisperConfig(
            modelSize = WhisperModelSize.MEDIUM,
            language = "de",
            translateToEnglish = true,
            numThreads = 4,
            vadSensitivity = 0.8f
        )
        assertEquals(config, config.copy())
    }

    // ── Custom Model Path ────────────────────────────────────────

    @Test
    fun `custom model path overrides default`() {
        val config = WhisperConfig(customModelPath = "/sdcard/my-model.bin")
        assertEquals("/sdcard/my-model.bin", config.customModelPath)
    }

    @Test
    fun `custom model path is null by default`() {
        val config = WhisperConfig()
        assertEquals(null, config.customModelPath)
    }
}
