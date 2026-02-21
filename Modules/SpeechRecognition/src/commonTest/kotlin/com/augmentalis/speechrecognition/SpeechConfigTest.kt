/**
 * SpeechConfigTest.kt - Tests for unified speech recognition configuration
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests factory methods, fluent API, validation, and utility methods
 * of the cross-platform SpeechConfig data class.
 */
package com.augmentalis.speechrecognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeechConfigTest {

    // ── Factory Methods ──────────────────────────────────────────

    @Test
    fun `default factory creates config with standard defaults`() {
        val config = SpeechConfig.default()

        assertEquals(LanguageCodes.ENGLISH_US, config.language)
        assertEquals(SpeechMode.DYNAMIC_COMMAND, config.mode)
        assertEquals(SpeechEngine.VOSK, config.engine)
        assertTrue(config.enableVAD)
        assertEquals(0.45f, config.confidenceThreshold)
        assertEquals(30000L, config.maxRecordingDuration)
        assertEquals(5000L, config.timeoutDuration)
        assertEquals(2000L, config.dictationTimeout)
        assertTrue(config.voiceEnabled)
        assertTrue(config.enableFuzzyMatching)
        assertTrue(config.enableSemanticMatching)
        assertNull(config.cloudApiKey)
        assertNull(config.modelPath)
    }

    @Test
    fun `vosk factory creates config with VOSK engine`() {
        val config = SpeechConfig.vosk()
        assertEquals(SpeechEngine.VOSK, config.engine)
        assertEquals(LanguageCodes.ENGLISH_US, config.language)
    }

    @Test
    fun `vosk factory accepts custom language`() {
        val config = SpeechConfig.vosk(LanguageCodes.SPANISH)
        assertEquals(SpeechEngine.VOSK, config.engine)
        assertEquals(LanguageCodes.SPANISH, config.language)
    }

    @Test
    fun `googleCloud factory requires projectId and sets engine`() {
        val config = SpeechConfig.googleCloud(
            projectId = "test-project-123"
        )
        assertEquals(SpeechEngine.GOOGLE_CLOUD, config.engine)
        assertEquals("test-project-123", config.gcpProjectId)
        assertNull(config.cloudApiKey)
        assertEquals("batch", config.gcpRecognizerMode)
    }

    @Test
    fun `googleCloud factory with streaming mode`() {
        val config = SpeechConfig.googleCloud(
            projectId = "test-project",
            streaming = true
        )
        assertEquals("streaming", config.gcpRecognizerMode)
    }

    @Test
    fun `googleCloud factory with API key`() {
        val config = SpeechConfig.googleCloud(
            apiKey = "my-api-key",
            projectId = "project-1"
        )
        assertEquals("my-api-key", config.cloudApiKey)
        assertEquals("project-1", config.gcpProjectId)
    }

    @Test
    fun `azure factory sets engine key and region`() {
        val config = SpeechConfig.azure(
            apiKey = "azure-key",
            region = "eastus"
        )
        assertEquals(SpeechEngine.AZURE, config.engine)
        assertEquals("azure-key", config.cloudApiKey)
        assertEquals("eastus", config.azureRegion)
        assertEquals(LanguageCodes.ENGLISH_US, config.language)
    }

    @Test
    fun `azure factory accepts custom language`() {
        val config = SpeechConfig.azure("key", "westus2", LanguageCodes.FRENCH)
        assertEquals(LanguageCodes.FRENCH, config.language)
    }

    @Test
    fun `whisper factory sets engine and optional modelPath`() {
        val config = SpeechConfig.whisper()
        assertEquals(SpeechEngine.WHISPER, config.engine)
        assertNull(config.modelPath)

        val withPath = SpeechConfig.whisper(modelPath = "/data/model.bin")
        assertEquals("/data/model.bin", withPath.modelPath)
    }

    // ── Fluent API ───────────────────────────────────────────────

    @Test
    fun `withLanguage creates new config with updated language`() {
        val original = SpeechConfig.default()
        val updated = original.withLanguage(LanguageCodes.GERMAN)

        assertEquals(LanguageCodes.GERMAN, updated.language)
        assertEquals(LanguageCodes.ENGLISH_US, original.language) // immutable
        assertEquals(original.engine, updated.engine) // unchanged
    }

    @Test
    fun `withEngine creates new config with updated engine`() {
        val updated = SpeechConfig.default().withEngine(SpeechEngine.WHISPER)
        assertEquals(SpeechEngine.WHISPER, updated.engine)
    }

    @Test
    fun `withMode creates new config with updated mode`() {
        val updated = SpeechConfig.default().withMode(SpeechMode.DICTATION)
        assertEquals(SpeechMode.DICTATION, updated.mode)
    }

    @Test
    fun `withVAD toggles VAD setting`() {
        val config = SpeechConfig.default()
        assertTrue(config.enableVAD)
        assertFalse(config.withVAD(false).enableVAD)
    }

    @Test
    fun `withConfidenceThreshold updates threshold`() {
        val config = SpeechConfig.default().withConfidenceThreshold(0.8f)
        assertEquals(0.8f, config.confidenceThreshold)
    }

    @Test
    fun `withTimeout updates timeout duration`() {
        val config = SpeechConfig.default().withTimeout(10000L)
        assertEquals(10000L, config.timeoutDuration)
    }

    @Test
    fun `withMaxRecording updates max recording duration`() {
        val config = SpeechConfig.default().withMaxRecording(60000L)
        assertEquals(60000L, config.maxRecordingDuration)
    }

    @Test
    fun `withApiKey sets cloud API key`() {
        val config = SpeechConfig.default().withApiKey("test-key")
        assertEquals("test-key", config.cloudApiKey)
    }

    @Test
    fun `withModelPath sets model path`() {
        val config = SpeechConfig.default().withModelPath("/models/whisper.bin")
        assertEquals("/models/whisper.bin", config.modelPath)
    }

    @Test
    fun `withProjectId sets GCP project ID`() {
        val config = SpeechConfig.default().withProjectId("my-project")
        assertEquals("my-project", config.gcpProjectId)
    }

    @Test
    fun `withStreamingMode sets GCP recognizer mode`() {
        val streaming = SpeechConfig.default().withStreamingMode(true)
        assertEquals("streaming", streaming.gcpRecognizerMode)

        val batch = SpeechConfig.default().withStreamingMode(false)
        assertEquals("batch", batch.gcpRecognizerMode)
    }

    @Test
    fun `withFuzzyMatching toggles fuzzy matching`() {
        val config = SpeechConfig.default().withFuzzyMatching(false)
        assertFalse(config.enableFuzzyMatching)
    }

    @Test
    fun `withSemanticMatching toggles semantic matching`() {
        val config = SpeechConfig.default().withSemanticMatching(false)
        assertFalse(config.enableSemanticMatching)
    }

    @Test
    fun `fluent API chains correctly`() {
        val config = SpeechConfig.default()
            .withEngine(SpeechEngine.GOOGLE_CLOUD)
            .withLanguage(LanguageCodes.JAPANESE)
            .withProjectId("chain-project")
            .withStreamingMode(true)
            .withConfidenceThreshold(0.6f)

        assertEquals(SpeechEngine.GOOGLE_CLOUD, config.engine)
        assertEquals(LanguageCodes.JAPANESE, config.language)
        assertEquals("chain-project", config.gcpProjectId)
        assertEquals("streaming", config.gcpRecognizerMode)
        assertEquals(0.6f, config.confidenceThreshold)
    }

    // ── TTS Fluent API ───────────────────────────────────────────

    @Test
    fun `withTTS enables TTS`() {
        val config = SpeechConfig.default().withTTS(true)
        assertTrue(config.enableTTS)
    }

    @Test
    fun `withTTSRate clamps to valid range`() {
        val low = SpeechConfig.default().withTTSRate(0.05f)
        assertEquals(0.1f, low.ttsRate)

        val high = SpeechConfig.default().withTTSRate(5.0f)
        assertEquals(3.0f, high.ttsRate)

        val normal = SpeechConfig.default().withTTSRate(1.5f)
        assertEquals(1.5f, normal.ttsRate)
    }

    @Test
    fun `withTTSPitch clamps to valid range`() {
        val low = SpeechConfig.default().withTTSPitch(0.0f)
        assertEquals(0.1f, low.ttsPitch)

        val high = SpeechConfig.default().withTTSPitch(3.0f)
        assertEquals(2.0f, high.ttsPitch)
    }

    @Test
    fun `withTTSVolume clamps to valid range`() {
        val low = SpeechConfig.default().withTTSVolume(-0.5f)
        assertEquals(0.0f, low.ttsVolume)

        val high = SpeechConfig.default().withTTSVolume(1.5f)
        assertEquals(1.0f, high.ttsVolume)
    }

    @Test
    fun `withTTSLanguage sets TTS language`() {
        val config = SpeechConfig.default().withTTSLanguage(LanguageCodes.SPANISH)
        assertEquals(LanguageCodes.SPANISH, config.ttsLanguage)
    }

    @Test
    fun `withTTSFeedbackLevel changes level`() {
        val config = SpeechConfig.default()
            .withTTSFeedbackLevel(SpeechConfig.TTSFeedbackLevel.VERBOSE)
        assertEquals(SpeechConfig.TTSFeedbackLevel.VERBOSE, config.ttsFeedbackLevel)
    }

    @Test
    fun `withTTSVoice sets voice name`() {
        val config = SpeechConfig.default().withTTSVoice("en-US-Wavenet-D")
        assertEquals("en-US-Wavenet-D", config.ttsVoice)
    }

    // ── Validation ───────────────────────────────────────────────

    @Test
    fun `validate passes for default config`() {
        assertTrue(SpeechConfig.default().validate().isSuccess)
    }

    @Test
    fun `validate fails for blank language`() {
        val config = SpeechConfig.default().withLanguage("")
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Language") == true)
    }

    @Test
    fun `validate fails for confidence below zero`() {
        val config = SpeechConfig.default().withConfidenceThreshold(-0.1f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for confidence above one`() {
        val config = SpeechConfig.default().withConfidenceThreshold(1.5f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for Google Cloud without projectId`() {
        val config = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            gcpProjectId = null
        )
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("project ID") == true)
    }

    @Test
    fun `validate fails for Google Cloud with blank projectId`() {
        val config = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            gcpProjectId = ""
        )
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate passes for Google Cloud with valid projectId`() {
        val config = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            gcpProjectId = "valid-project"
        )
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate fails for Azure without API key`() {
        val config = SpeechConfig(
            engine = SpeechEngine.AZURE,
            azureRegion = "eastus"
        )
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for Azure without region`() {
        val config = SpeechConfig(
            engine = SpeechEngine.AZURE,
            cloudApiKey = "my-key"
        )
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate passes for Azure with key and region`() {
        val config = SpeechConfig.azure("key", "eastus")
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate fails for timeout below 1000ms`() {
        val config = SpeechConfig(timeoutDuration = 500)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails when maxRecording less than timeout`() {
        val config = SpeechConfig(
            timeoutDuration = 5000,
            maxRecordingDuration = 3000
        )
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS rate out of range`() {
        // Note: withTTSRate clamps, but direct construction doesn't
        val config = SpeechConfig(ttsRate = 5.0f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS pitch out of range`() {
        val config = SpeechConfig(ttsPitch = 3.0f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS volume out of range`() {
        val config = SpeechConfig(ttsVolume = 1.5f)
        assertTrue(config.validate().isFailure)
    }

    // ── Utility Methods ──────────────────────────────────────────

    @Test
    fun `requiresNetwork returns true for online engines`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.ANDROID_STT).requiresNetwork())
        assertTrue(SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD).requiresNetwork())
        assertTrue(SpeechConfig(engine = SpeechEngine.AZURE).requiresNetwork())
        assertTrue(SpeechConfig(engine = SpeechEngine.WEB_SPEECH).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns false for offline engines`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.VOSK).requiresNetwork())
        assertFalse(SpeechConfig(engine = SpeechEngine.WHISPER).requiresNetwork())
        assertFalse(SpeechConfig(engine = SpeechEngine.VIVOKA).requiresNetwork())
        assertFalse(SpeechConfig(engine = SpeechEngine.APPLE_SPEECH).requiresNetwork())
    }

    @Test
    fun `requiresModelDownload for offline engines without modelPath`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.VOSK).requiresModelDownload())
        assertTrue(SpeechConfig(engine = SpeechEngine.WHISPER).requiresModelDownload())
        assertTrue(SpeechConfig(engine = SpeechEngine.VIVOKA).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns false when modelPath set`() {
        assertFalse(
            SpeechConfig(engine = SpeechEngine.VOSK, modelPath = "/path/model.bin")
                .requiresModelDownload()
        )
    }

    @Test
    fun `requiresModelDownload returns false for online-only engines`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD).requiresModelDownload())
        assertFalse(SpeechConfig(engine = SpeechEngine.AZURE).requiresModelDownload())
        assertFalse(SpeechConfig(engine = SpeechEngine.ANDROID_STT).requiresModelDownload())
    }

    @Test
    fun `toString contains key fields`() {
        val config = SpeechConfig.default()
        val str = config.toString()
        assertTrue(str.contains("VOSK"))
        assertTrue(str.contains("en-US"))
        assertTrue(str.contains("DYNAMIC_COMMAND"))
    }

    // ── Language Codes ───────────────────────────────────────────

    @Test
    fun `LanguageCodes ALL contains expected count`() {
        assertEquals(20, LanguageCodes.ALL.size)
    }

    @Test
    fun `LanguageCodes ALL contains all defined constants`() {
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.ENGLISH_US))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.HINDI))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.JAPANESE))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.MALAY))
    }

    // ── Data Class Equality ──────────────────────────────────────

    @Test
    fun `configs with same values are equal`() {
        val a = SpeechConfig.vosk(LanguageCodes.FRENCH)
        val b = SpeechConfig.vosk(LanguageCodes.FRENCH)
        assertEquals(a, b)
    }

    @Test
    fun `copy preserves all fields`() {
        val original = SpeechConfig.googleCloud(
            apiKey = "key",
            projectId = "proj",
            streaming = true
        )
        val copy = original.copy()
        assertEquals(original, copy)
    }
}
