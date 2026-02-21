/**
 * SpeechConfigTest.kt - Comprehensive unit tests for SpeechConfig
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 * Updated: 2026-02-22 - Extended coverage: boundary cases, TTS defaults, blank whitespace
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
        assertNull(config.azureRegion)
        assertNull(config.gcpProjectId)
        assertNull(config.gcpRecognizerMode)
    }

    @Test
    fun `default factory TTS fields start at expected defaults`() {
        val config = SpeechConfig.default()

        assertFalse(config.enableTTS)
        assertEquals(1.0f, config.ttsRate)
        assertEquals(1.0f, config.ttsPitch)
        assertEquals(1.0f, config.ttsVolume)
        assertEquals(SpeechConfig.TTSFeedbackLevel.NORMAL, config.ttsFeedbackLevel)
        assertNull(config.ttsVoice)
        assertEquals(LanguageCodes.ENGLISH_US, config.ttsLanguage)
    }

    @Test
    fun `vosk factory creates config with VOSK engine and default language`() {
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
    fun `googleCloud factory sets engine and project ID with batch mode by default`() {
        val config = SpeechConfig.googleCloud(projectId = "test-project-123")
        assertEquals(SpeechEngine.GOOGLE_CLOUD, config.engine)
        assertEquals("test-project-123", config.gcpProjectId)
        assertNull(config.cloudApiKey)
        assertEquals("batch", config.gcpRecognizerMode)
    }

    @Test
    fun `googleCloud factory with streaming true sets streaming recognizer mode`() {
        val config = SpeechConfig.googleCloud(projectId = "test-project", streaming = true)
        assertEquals("streaming", config.gcpRecognizerMode)
    }

    @Test
    fun `googleCloud factory with streaming false sets batch recognizer mode`() {
        val config = SpeechConfig.googleCloud(projectId = "test-project", streaming = false)
        assertEquals("batch", config.gcpRecognizerMode)
    }

    @Test
    fun `googleCloud factory with API key stores key alongside project ID`() {
        val config = SpeechConfig.googleCloud(apiKey = "my-api-key", projectId = "project-1")
        assertEquals("my-api-key", config.cloudApiKey)
        assertEquals("project-1", config.gcpProjectId)
    }

    @Test
    fun `googleCloud factory with custom language stores it correctly`() {
        val config = SpeechConfig.googleCloud(
            projectId = "proj",
            language = LanguageCodes.JAPANESE
        )
        assertEquals(LanguageCodes.JAPANESE, config.language)
    }

    @Test
    fun `azure factory sets engine API key and region`() {
        val config = SpeechConfig.azure(apiKey = "azure-key", region = "eastus")
        assertEquals(SpeechEngine.AZURE, config.engine)
        assertEquals("azure-key", config.cloudApiKey)
        assertEquals("eastus", config.azureRegion)
        assertEquals(LanguageCodes.ENGLISH_US, config.language)
    }

    @Test
    fun `azure factory accepts custom language`() {
        val config = SpeechConfig.azure("key", "westus2", LanguageCodes.FRENCH)
        assertEquals(LanguageCodes.FRENCH, config.language)
        assertEquals(SpeechEngine.AZURE, config.engine)
    }

    @Test
    fun `whisper factory sets engine with null modelPath by default`() {
        val config = SpeechConfig.whisper()
        assertEquals(SpeechEngine.WHISPER, config.engine)
        assertNull(config.modelPath)
        assertEquals(LanguageCodes.ENGLISH_US, config.language)
    }

    @Test
    fun `whisper factory with explicit modelPath stores path`() {
        val config = SpeechConfig.whisper(modelPath = "/data/model.bin")
        assertEquals(SpeechEngine.WHISPER, config.engine)
        assertEquals("/data/model.bin", config.modelPath)
    }

    @Test
    fun `whisper factory with custom language`() {
        val config = SpeechConfig.whisper(language = LanguageCodes.GERMAN)
        assertEquals(LanguageCodes.GERMAN, config.language)
    }

    // ── Fluent API — Immutability ────────────────────────────────

    @Test
    fun `withLanguage creates new config leaving original unchanged`() {
        val original = SpeechConfig.default()
        val updated = original.withLanguage(LanguageCodes.GERMAN)

        assertEquals(LanguageCodes.GERMAN, updated.language)
        assertEquals(LanguageCodes.ENGLISH_US, original.language)
        assertEquals(original.engine, updated.engine)
        assertEquals(original.mode, updated.mode)
    }

    @Test
    fun `withEngine creates new config with updated engine`() {
        val original = SpeechConfig.default()
        val updated = original.withEngine(SpeechEngine.WHISPER)
        assertEquals(SpeechEngine.WHISPER, updated.engine)
        assertEquals(SpeechEngine.VOSK, original.engine)
    }

    @Test
    fun `withMode creates new config with updated mode`() {
        val updated = SpeechConfig.default().withMode(SpeechMode.DICTATION)
        assertEquals(SpeechMode.DICTATION, updated.mode)
    }

    @Test
    fun `withMode accepts all SpeechMode values`() {
        val base = SpeechConfig.default()
        assertEquals(SpeechMode.STATIC_COMMAND, base.withMode(SpeechMode.STATIC_COMMAND).mode)
        assertEquals(SpeechMode.DYNAMIC_COMMAND, base.withMode(SpeechMode.DYNAMIC_COMMAND).mode)
        assertEquals(SpeechMode.DICTATION, base.withMode(SpeechMode.DICTATION).mode)
        assertEquals(SpeechMode.FREE_SPEECH, base.withMode(SpeechMode.FREE_SPEECH).mode)
        assertEquals(SpeechMode.HYBRID, base.withMode(SpeechMode.HYBRID).mode)
    }

    @Test
    fun `withVAD enables and disables VAD`() {
        val config = SpeechConfig.default()
        assertTrue(config.enableVAD)
        assertFalse(config.withVAD(false).enableVAD)
        assertTrue(config.withVAD(false).withVAD(true).enableVAD)
    }

    @Test
    fun `withConfidenceThreshold updates threshold without clamping`() {
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
    fun `withApiKey sets cloud API key and does not alter other fields`() {
        val config = SpeechConfig.default().withApiKey("test-key")
        assertEquals("test-key", config.cloudApiKey)
        assertEquals(SpeechEngine.VOSK, config.engine)
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
    fun `withStreamingMode true sets streaming recognizer mode`() {
        val config = SpeechConfig.default().withStreamingMode(true)
        assertEquals("streaming", config.gcpRecognizerMode)
    }

    @Test
    fun `withStreamingMode false sets batch recognizer mode`() {
        val config = SpeechConfig.default().withStreamingMode(false)
        assertEquals("batch", config.gcpRecognizerMode)
    }

    @Test
    fun `withStreamingMode default argument resolves to streaming`() {
        val config = SpeechConfig.default().withStreamingMode()
        assertEquals("streaming", config.gcpRecognizerMode)
    }

    @Test
    fun `withFuzzyMatching toggles fuzzy matching flag`() {
        val config = SpeechConfig.default().withFuzzyMatching(false)
        assertFalse(config.enableFuzzyMatching)
        assertTrue(config.withFuzzyMatching(true).enableFuzzyMatching)
    }

    @Test
    fun `withSemanticMatching toggles semantic matching flag`() {
        val config = SpeechConfig.default().withSemanticMatching(false)
        assertFalse(config.enableSemanticMatching)
        assertTrue(config.withSemanticMatching(true).enableSemanticMatching)
    }

    @Test
    fun `fluent API chains multiple calls correctly`() {
        val config = SpeechConfig.default()
            .withEngine(SpeechEngine.GOOGLE_CLOUD)
            .withLanguage(LanguageCodes.JAPANESE)
            .withProjectId("chain-project")
            .withStreamingMode(true)
            .withConfidenceThreshold(0.6f)
            .withVAD(false)

        assertEquals(SpeechEngine.GOOGLE_CLOUD, config.engine)
        assertEquals(LanguageCodes.JAPANESE, config.language)
        assertEquals("chain-project", config.gcpProjectId)
        assertEquals("streaming", config.gcpRecognizerMode)
        assertEquals(0.6f, config.confidenceThreshold)
        assertFalse(config.enableVAD)
    }

    // ── TTS Fluent API ───────────────────────────────────────────

    @Test
    fun `withTTS enables TTS`() {
        val config = SpeechConfig.default().withTTS(true)
        assertTrue(config.enableTTS)
    }

    @Test
    fun `withTTS disables TTS`() {
        val config = SpeechConfig.default().withTTS(true).withTTS(false)
        assertFalse(config.enableTTS)
    }

    @Test
    fun `withTTSRate clamps value below minimum to 0_1f`() {
        val config = SpeechConfig.default().withTTSRate(0.05f)
        assertEquals(0.1f, config.ttsRate)
    }

    @Test
    fun `withTTSRate clamps value above maximum to 3_0f`() {
        val config = SpeechConfig.default().withTTSRate(5.0f)
        assertEquals(3.0f, config.ttsRate)
    }

    @Test
    fun `withTTSRate preserves value within valid range`() {
        assertEquals(1.5f, SpeechConfig.default().withTTSRate(1.5f).ttsRate)
        assertEquals(2.0f, SpeechConfig.default().withTTSRate(2.0f).ttsRate)
    }

    @Test
    fun `withTTSRate does not clamp exact boundary values`() {
        assertEquals(0.1f, SpeechConfig.default().withTTSRate(0.1f).ttsRate)
        assertEquals(3.0f, SpeechConfig.default().withTTSRate(3.0f).ttsRate)
    }

    @Test
    fun `withTTSPitch clamps value below minimum to 0_1f`() {
        val config = SpeechConfig.default().withTTSPitch(0.0f)
        assertEquals(0.1f, config.ttsPitch)
    }

    @Test
    fun `withTTSPitch clamps value above maximum to 2_0f`() {
        val config = SpeechConfig.default().withTTSPitch(3.0f)
        assertEquals(2.0f, config.ttsPitch)
    }

    @Test
    fun `withTTSPitch preserves value within valid range`() {
        assertEquals(1.2f, SpeechConfig.default().withTTSPitch(1.2f).ttsPitch)
    }

    @Test
    fun `withTTSPitch does not clamp exact boundary values`() {
        assertEquals(0.1f, SpeechConfig.default().withTTSPitch(0.1f).ttsPitch)
        assertEquals(2.0f, SpeechConfig.default().withTTSPitch(2.0f).ttsPitch)
    }

    @Test
    fun `withTTSVolume clamps negative value to 0_0f`() {
        val config = SpeechConfig.default().withTTSVolume(-0.5f)
        assertEquals(0.0f, config.ttsVolume)
    }

    @Test
    fun `withTTSVolume clamps value above maximum to 1_0f`() {
        val config = SpeechConfig.default().withTTSVolume(1.5f)
        assertEquals(1.0f, config.ttsVolume)
    }

    @Test
    fun `withTTSVolume preserves value within valid range`() {
        assertEquals(0.75f, SpeechConfig.default().withTTSVolume(0.75f).ttsVolume)
    }

    @Test
    fun `withTTSVolume does not clamp exact boundary values`() {
        assertEquals(0.0f, SpeechConfig.default().withTTSVolume(0.0f).ttsVolume)
        assertEquals(1.0f, SpeechConfig.default().withTTSVolume(1.0f).ttsVolume)
    }

    @Test
    fun `withTTSLanguage sets TTS language independently of recognition language`() {
        val config = SpeechConfig.default()
            .withLanguage(LanguageCodes.ENGLISH_US)
            .withTTSLanguage(LanguageCodes.SPANISH)
        assertEquals(LanguageCodes.ENGLISH_US, config.language)
        assertEquals(LanguageCodes.SPANISH, config.ttsLanguage)
    }

    @Test
    fun `withTTSFeedbackLevel changes to SILENT`() {
        val config = SpeechConfig.default()
            .withTTSFeedbackLevel(SpeechConfig.TTSFeedbackLevel.SILENT)
        assertEquals(SpeechConfig.TTSFeedbackLevel.SILENT, config.ttsFeedbackLevel)
    }

    @Test
    fun `withTTSFeedbackLevel changes to MINIMAL`() {
        val config = SpeechConfig.default()
            .withTTSFeedbackLevel(SpeechConfig.TTSFeedbackLevel.MINIMAL)
        assertEquals(SpeechConfig.TTSFeedbackLevel.MINIMAL, config.ttsFeedbackLevel)
    }

    @Test
    fun `withTTSFeedbackLevel changes to VERBOSE`() {
        val config = SpeechConfig.default()
            .withTTSFeedbackLevel(SpeechConfig.TTSFeedbackLevel.VERBOSE)
        assertEquals(SpeechConfig.TTSFeedbackLevel.VERBOSE, config.ttsFeedbackLevel)
    }

    @Test
    fun `withTTSFeedbackLevel default is NORMAL`() {
        assertEquals(SpeechConfig.TTSFeedbackLevel.NORMAL, SpeechConfig.default().ttsFeedbackLevel)
    }

    @Test
    fun `withTTSVoice sets voice name`() {
        val config = SpeechConfig.default().withTTSVoice("en-US-Wavenet-D")
        assertEquals("en-US-Wavenet-D", config.ttsVoice)
    }

    // ── Validation — Success Cases ───────────────────────────────

    @Test
    fun `validate passes for default config`() {
        assertTrue(SpeechConfig.default().validate().isSuccess)
    }

    @Test
    fun `validate passes for vosk factory config`() {
        assertTrue(SpeechConfig.vosk().validate().isSuccess)
    }

    @Test
    fun `validate passes for whisper with model path`() {
        assertTrue(SpeechConfig.whisper(modelPath = "/path/model.bin").validate().isSuccess)
    }

    @Test
    fun `validate passes for Google Cloud with valid projectId`() {
        val config = SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD, gcpProjectId = "valid-project")
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes for Azure factory with key and region`() {
        assertTrue(SpeechConfig.azure("key", "eastus").validate().isSuccess)
    }

    @Test
    fun `validate passes for confidence threshold at lower boundary 0_0f`() {
        val config = SpeechConfig.default().withConfidenceThreshold(0.0f)
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes for confidence threshold at upper boundary 1_0f`() {
        val config = SpeechConfig.default().withConfidenceThreshold(1.0f)
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes for timeout at exact minimum boundary 1000ms`() {
        val config = SpeechConfig(timeoutDuration = 1000, maxRecordingDuration = 1000)
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes when maxRecordingDuration equals timeoutDuration`() {
        val config = SpeechConfig(timeoutDuration = 5000, maxRecordingDuration = 5000)
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes for TTS values at exact valid boundaries`() {
        val config = SpeechConfig(ttsRate = 0.1f, ttsPitch = 0.1f, ttsVolume = 0.0f)
        assertTrue(config.validate().isSuccess)

        val config2 = SpeechConfig(ttsRate = 3.0f, ttsPitch = 2.0f, ttsVolume = 1.0f)
        assertTrue(config2.validate().isSuccess)
    }

    // ── Validation — Failure Cases ───────────────────────────────

    @Test
    fun `validate fails for empty string language`() {
        val config = SpeechConfig.default().withLanguage("")
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Language") == true)
    }

    @Test
    fun `validate fails for whitespace-only language`() {
        val config = SpeechConfig.default().withLanguage("   ")
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Language") == true)
    }

    @Test
    fun `validate fails for confidence threshold below zero`() {
        val result = SpeechConfig.default().withConfidenceThreshold(-0.1f).validate()
        assertTrue(result.isFailure)
    }

    @Test
    fun `validate fails for confidence threshold above one`() {
        val result = SpeechConfig.default().withConfidenceThreshold(1.5f).validate()
        assertTrue(result.isFailure)
    }

    @Test
    fun `validate fails for Google Cloud with null projectId`() {
        val config = SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD, gcpProjectId = null)
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("project ID") == true)
    }

    @Test
    fun `validate fails for Google Cloud with blank projectId`() {
        val config = SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD, gcpProjectId = "")
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for Google Cloud with whitespace-only projectId`() {
        val config = SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD, gcpProjectId = "   ")
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for Azure with null API key`() {
        val config = SpeechConfig(engine = SpeechEngine.AZURE, azureRegion = "eastus")
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Azure requires API key") == true)
    }

    @Test
    fun `validate fails for Azure with blank API key`() {
        val config = SpeechConfig(engine = SpeechEngine.AZURE, cloudApiKey = "", azureRegion = "eastus")
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for Azure with null region`() {
        val config = SpeechConfig(engine = SpeechEngine.AZURE, cloudApiKey = "my-key")
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Azure requires region") == true)
    }

    @Test
    fun `validate fails for Azure with blank region`() {
        val config = SpeechConfig(engine = SpeechEngine.AZURE, cloudApiKey = "my-key", azureRegion = "")
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for timeout below 1000ms`() {
        val config = SpeechConfig(timeoutDuration = 999, maxRecordingDuration = 30000)
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Timeout") == true)
    }

    @Test
    fun `validate fails when maxRecordingDuration is less than timeoutDuration`() {
        val config = SpeechConfig(timeoutDuration = 5000, maxRecordingDuration = 3000)
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Max recording") == true)
    }

    @Test
    fun `validate fails for TTS rate below minimum via direct construction`() {
        val config = SpeechConfig(ttsRate = 0.05f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS rate above maximum via direct construction`() {
        val config = SpeechConfig(ttsRate = 5.0f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS pitch below minimum via direct construction`() {
        val config = SpeechConfig(ttsPitch = 0.0f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS pitch above maximum via direct construction`() {
        val config = SpeechConfig(ttsPitch = 3.0f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS volume below minimum via direct construction`() {
        val config = SpeechConfig(ttsVolume = -0.1f)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for TTS volume above maximum via direct construction`() {
        val config = SpeechConfig(ttsVolume = 1.5f)
        assertTrue(config.validate().isFailure)
    }

    // ── requiresNetwork ──────────────────────────────────────────

    @Test
    fun `requiresNetwork returns true for ANDROID_STT`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.ANDROID_STT).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns true for GOOGLE_CLOUD`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns true for AZURE`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.AZURE).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns true for WEB_SPEECH`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.WEB_SPEECH).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns false for VOSK`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.VOSK).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns false for WHISPER`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.WHISPER).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns false for VIVOKA`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.VIVOKA).requiresNetwork())
    }

    @Test
    fun `requiresNetwork returns false for APPLE_SPEECH`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.APPLE_SPEECH).requiresNetwork())
    }

    // ── requiresModelDownload ────────────────────────────────────

    @Test
    fun `requiresModelDownload returns true for VOSK without modelPath`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.VOSK, modelPath = null).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns true for WHISPER without modelPath`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.WHISPER, modelPath = null).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns true for VIVOKA without modelPath`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.VIVOKA, modelPath = null).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns true for VOSK with blank modelPath`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.VOSK, modelPath = "").requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns true for WHISPER with whitespace-only modelPath`() {
        assertTrue(SpeechConfig(engine = SpeechEngine.WHISPER, modelPath = "   ").requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns false for VOSK with non-blank modelPath`() {
        assertFalse(
            SpeechConfig(engine = SpeechEngine.VOSK, modelPath = "/path/model.bin")
                .requiresModelDownload()
        )
    }

    @Test
    fun `requiresModelDownload returns false for WHISPER with non-blank modelPath`() {
        assertFalse(
            SpeechConfig(engine = SpeechEngine.WHISPER, modelPath = "/models/whisper-base.bin")
                .requiresModelDownload()
        )
    }

    @Test
    fun `requiresModelDownload returns false for GOOGLE_CLOUD`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.GOOGLE_CLOUD).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns false for AZURE`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.AZURE).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns false for ANDROID_STT`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.ANDROID_STT).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns false for WEB_SPEECH`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.WEB_SPEECH).requiresModelDownload())
    }

    @Test
    fun `requiresModelDownload returns false for APPLE_SPEECH`() {
        assertFalse(SpeechConfig(engine = SpeechEngine.APPLE_SPEECH).requiresModelDownload())
    }

    // ── toString ─────────────────────────────────────────────────

    @Test
    fun `toString contains engine name`() {
        val str = SpeechConfig.default().toString()
        assertTrue(str.contains("VOSK"))
    }

    @Test
    fun `toString contains language`() {
        val str = SpeechConfig.default().toString()
        assertTrue(str.contains("en-US"))
    }

    @Test
    fun `toString contains mode`() {
        val str = SpeechConfig.default().toString()
        assertTrue(str.contains("DYNAMIC_COMMAND"))
    }

    // ── LanguageCodes ────────────────────────────────────────────

    @Test
    fun `LanguageCodes ALL contains exactly 20 entries`() {
        assertEquals(20, LanguageCodes.ALL.size)
    }

    @Test
    fun `LanguageCodes ALL contains all expected language codes`() {
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.ENGLISH_US))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.ENGLISH_UK))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.SPANISH))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.SPANISH_MEXICO))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.FRENCH))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.GERMAN))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.HINDI))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.JAPANESE))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.MALAY))
        assertTrue(LanguageCodes.ALL.contains(LanguageCodes.VIETNAMESE))
    }

    @Test
    fun `LanguageCodes constants use BCP-47 format`() {
        assertTrue(LanguageCodes.ENGLISH_US.contains("-"))
        assertTrue(LanguageCodes.ENGLISH_US == "en-US")
        assertTrue(LanguageCodes.FRENCH == "fr-FR")
        assertTrue(LanguageCodes.HINDI == "hi-IN")
    }

    // ── Data Class Equality and Copy ─────────────────────────────

    @Test
    fun `configs created from same factory with same args are equal`() {
        val a = SpeechConfig.vosk(LanguageCodes.FRENCH)
        val b = SpeechConfig.vosk(LanguageCodes.FRENCH)
        assertEquals(a, b)
    }

    @Test
    fun `copy with no changes produces equal config`() {
        val original = SpeechConfig.googleCloud(
            apiKey = "key",
            projectId = "proj",
            streaming = true
        )
        val copy = original.copy()
        assertEquals(original, copy)
    }

    @Test
    fun `copy with one changed field differs only in that field`() {
        val original = SpeechConfig.vosk(LanguageCodes.ENGLISH_US)
        val modified = original.copy(language = LanguageCodes.KOREAN)

        assertEquals(LanguageCodes.KOREAN, modified.language)
        assertEquals(original.engine, modified.engine)
        assertEquals(original.mode, modified.mode)
        assertEquals(original.confidenceThreshold, modified.confidenceThreshold)
    }

    @Test
    fun `withLanguage and copy produce equivalent results`() {
        val viaFluent = SpeechConfig.default().withLanguage(LanguageCodes.RUSSIAN)
        val viaCopy = SpeechConfig.default().copy(language = LanguageCodes.RUSSIAN)
        assertEquals(viaFluent, viaCopy)
    }
}
