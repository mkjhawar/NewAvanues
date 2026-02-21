/**
 * GoogleCloudConfigTest.kt - Tests for Google Cloud STT v2 configuration
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests configuration validation, factory mapping from SpeechConfig,
 * URL building, and auth mode inference.
 */
package com.augmentalis.speechrecognition.googlecloud

import com.augmentalis.speechrecognition.LanguageCodes
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechEngine
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleCloudConfigTest {

    // ── Default Values ───────────────────────────────────────────

    @Test
    fun `defaults have sensible values`() {
        val config = GoogleCloudConfig(projectId = "test-proj")

        assertEquals("test-proj", config.projectId)
        assertEquals("_", config.recognizerName)
        assertEquals("global", config.location)
        assertEquals(GoogleCloudMode.VAD_BATCH, config.mode)
        assertEquals("en-US", config.language)
        assertEquals("latest_short", config.model)
        assertTrue(config.enableAutoPunctuation)
        assertTrue(config.enableWordTimeOffsets)
        assertTrue(config.enableWordConfidence)
        assertEquals(3, config.maxAlternatives)
        assertEquals(GoogleCloudAuthMode.API_KEY, config.authMode)
        assertNull(config.apiKey)
        assertEquals(700L, config.silenceThresholdMs)
        assertEquals(300L, config.minSpeechDurationMs)
        assertEquals(30_000L, config.maxChunkDurationMs)
        assertEquals(30_000L, config.requestTimeoutMs)
        assertEquals(10_000L, config.connectTimeoutMs)
        assertEquals(3, config.maxRetries)
        assertFalse(config.profanityFilter)
    }

    // ── fromSpeechConfig Factory ─────────────────────────────────

    @Test
    fun `fromSpeechConfig maps batch mode correctly`() {
        val speechConfig = SpeechConfig.googleCloud(
            projectId = "my-project",
            streaming = false
        )
        val gcpConfig = GoogleCloudConfig.fromSpeechConfig(speechConfig)

        assertEquals("my-project", gcpConfig.projectId)
        assertEquals(GoogleCloudMode.VAD_BATCH, gcpConfig.mode)
        assertEquals(LanguageCodes.ENGLISH_US, gcpConfig.language)
    }

    @Test
    fun `fromSpeechConfig maps streaming mode correctly`() {
        val speechConfig = SpeechConfig.googleCloud(
            projectId = "stream-project",
            streaming = true
        )
        val gcpConfig = GoogleCloudConfig.fromSpeechConfig(speechConfig)

        assertEquals(GoogleCloudMode.STREAMING, gcpConfig.mode)
    }

    @Test
    fun `fromSpeechConfig infers FIREBASE_AUTH when no API key`() {
        val speechConfig = SpeechConfig.googleCloud(
            projectId = "proj",
            apiKey = null
        )
        val gcpConfig = GoogleCloudConfig.fromSpeechConfig(speechConfig)

        assertEquals(GoogleCloudAuthMode.FIREBASE_AUTH, gcpConfig.authMode)
    }

    @Test
    fun `fromSpeechConfig uses API_KEY when API key provided`() {
        val speechConfig = SpeechConfig.googleCloud(
            apiKey = "my-api-key",
            projectId = "proj"
        )
        val gcpConfig = GoogleCloudConfig.fromSpeechConfig(speechConfig)

        assertEquals(GoogleCloudAuthMode.API_KEY, gcpConfig.authMode)
        assertEquals("my-api-key", gcpConfig.apiKey)
    }

    @Test
    fun `fromSpeechConfig maps language`() {
        val speechConfig = SpeechConfig.googleCloud(
            projectId = "proj",
            language = LanguageCodes.JAPANESE
        )
        val gcpConfig = GoogleCloudConfig.fromSpeechConfig(speechConfig)

        assertEquals(LanguageCodes.JAPANESE, gcpConfig.language)
    }

    @Test
    fun `fromSpeechConfig maps profanity filter`() {
        val speechConfig = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            gcpProjectId = "proj",
            enableProfanityFilter = true
        )
        val gcpConfig = GoogleCloudConfig.fromSpeechConfig(speechConfig)

        assertTrue(gcpConfig.profanityFilter)
    }

    @Test
    fun `fromSpeechConfig clamps silence threshold`() {
        // SpeechConfig.timeoutDuration maps to silenceThresholdMs, clamped to 300-5000
        val lowTimeout = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            gcpProjectId = "proj",
            timeoutDuration = 100 // below 300 minimum
        )
        val gcpLow = GoogleCloudConfig.fromSpeechConfig(lowTimeout)
        assertEquals(300L, gcpLow.silenceThresholdMs)

        val highTimeout = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            gcpProjectId = "proj",
            timeoutDuration = 10000 // above 5000 maximum
        )
        val gcpHigh = GoogleCloudConfig.fromSpeechConfig(highTimeout)
        assertEquals(5000L, gcpHigh.silenceThresholdMs)
    }

    @Test
    fun `fromSpeechConfig handles empty projectId`() {
        val speechConfig = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            gcpProjectId = null
        )
        val gcpConfig = GoogleCloudConfig.fromSpeechConfig(speechConfig)
        assertEquals("", gcpConfig.projectId)
    }

    // ── URL Builders ─────────────────────────────────────────────

    @Test
    fun `buildRecognizeUrl uses correct v2 format`() {
        val config = GoogleCloudConfig(projectId = "my-project")
        val url = config.buildRecognizeUrl()

        assertEquals(
            "https://speech.googleapis.com/v2/projects/my-project/locations/global/recognizers/_:recognize",
            url
        )
    }

    @Test
    fun `buildRecognizeUrl uses custom location and recognizer`() {
        val config = GoogleCloudConfig(
            projectId = "proj-1",
            location = "us-central1",
            recognizerName = "my-recognizer"
        )
        val url = config.buildRecognizeUrl()

        assertTrue(url.contains("/locations/us-central1/"))
        assertTrue(url.contains("/recognizers/my-recognizer:recognize"))
    }

    @Test
    fun `buildStreamingUrl uses correct v2 format`() {
        val config = GoogleCloudConfig(projectId = "stream-proj")
        val url = config.buildStreamingUrl()

        assertEquals(
            "https://speech.googleapis.com/v2/projects/stream-proj/locations/global/recognizers/_:streamingRecognize",
            url
        )
    }

    @Test
    fun `buildStreamingUrl uses custom location`() {
        val config = GoogleCloudConfig(
            projectId = "proj-2",
            location = "europe-west1"
        )
        val url = config.buildStreamingUrl()
        assertTrue(url.contains("/locations/europe-west1/"))
    }

    // ── Validation ───────────────────────────────────────────────

    @Test
    fun `validate passes for minimal valid config`() {
        val config = GoogleCloudConfig(
            projectId = "valid-project",
            authMode = GoogleCloudAuthMode.FIREBASE_AUTH
        )
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate passes for API_KEY with key provided`() {
        val config = GoogleCloudConfig(
            projectId = "valid-project",
            authMode = GoogleCloudAuthMode.API_KEY,
            apiKey = "my-key"
        )
        assertTrue(config.validate().isSuccess)
    }

    @Test
    fun `validate fails for blank projectId`() {
        val config = GoogleCloudConfig(projectId = "")
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("project ID") == true)
    }

    @Test
    fun `validate fails for blank language`() {
        val config = GoogleCloudConfig(projectId = "proj", language = "")
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for API_KEY without key`() {
        val config = GoogleCloudConfig(
            projectId = "proj",
            authMode = GoogleCloudAuthMode.API_KEY,
            apiKey = null
        )
        val result = config.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key") == true)
    }

    @Test
    fun `validate fails for API_KEY with blank key`() {
        val config = GoogleCloudConfig(
            projectId = "proj",
            authMode = GoogleCloudAuthMode.API_KEY,
            apiKey = ""
        )
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for request timeout below 1000ms`() {
        val config = GoogleCloudConfig(projectId = "proj", requestTimeoutMs = 500)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for connect timeout below 1000ms`() {
        val config = GoogleCloudConfig(projectId = "proj", connectTimeoutMs = 999)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for negative max retries`() {
        val config = GoogleCloudConfig(projectId = "proj", maxRetries = -1)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for max retries above 10`() {
        val config = GoogleCloudConfig(projectId = "proj", maxRetries = 11)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate passes for max retries at boundaries`() {
        assertTrue(GoogleCloudConfig(projectId = "proj", maxRetries = 0, authMode = GoogleCloudAuthMode.FIREBASE_AUTH).validate().isSuccess)
        assertTrue(GoogleCloudConfig(projectId = "proj", maxRetries = 10, authMode = GoogleCloudAuthMode.FIREBASE_AUTH).validate().isSuccess)
    }

    @Test
    fun `validate fails for silence threshold below 100`() {
        val config = GoogleCloudConfig(projectId = "proj", silenceThresholdMs = 50)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate fails for silence threshold above 5000`() {
        val config = GoogleCloudConfig(projectId = "proj", silenceThresholdMs = 6000)
        assertTrue(config.validate().isFailure)
    }

    @Test
    fun `validate passes for silence threshold at boundaries`() {
        assertTrue(GoogleCloudConfig(projectId = "proj", silenceThresholdMs = 100, authMode = GoogleCloudAuthMode.FIREBASE_AUTH).validate().isSuccess)
        assertTrue(GoogleCloudConfig(projectId = "proj", silenceThresholdMs = 5000, authMode = GoogleCloudAuthMode.FIREBASE_AUTH).validate().isSuccess)
    }

    // ── Enum Coverage ────────────────────────────────────────────

    @Test
    fun `GoogleCloudMode has 2 values`() {
        assertEquals(2, GoogleCloudMode.entries.size)
        assertTrue(GoogleCloudMode.entries.any { it.name == "VAD_BATCH" })
        assertTrue(GoogleCloudMode.entries.any { it.name == "STREAMING" })
    }

    @Test
    fun `GoogleCloudAuthMode has 2 values`() {
        assertEquals(2, GoogleCloudAuthMode.entries.size)
        assertTrue(GoogleCloudAuthMode.entries.any { it.name == "FIREBASE_AUTH" })
        assertTrue(GoogleCloudAuthMode.entries.any { it.name == "API_KEY" })
    }

    // ── Data Class Equality ──────────────────────────────────────

    @Test
    fun `configs with same values are equal`() {
        val a = GoogleCloudConfig(projectId = "p1", language = "en-US")
        val b = GoogleCloudConfig(projectId = "p1", language = "en-US")
        assertEquals(a, b)
    }

    @Test
    fun `copy preserves all fields`() {
        val config = GoogleCloudConfig(
            projectId = "proj",
            mode = GoogleCloudMode.STREAMING,
            authMode = GoogleCloudAuthMode.FIREBASE_AUTH,
            maxRetries = 5
        )
        assertEquals(config, config.copy())
    }
}
