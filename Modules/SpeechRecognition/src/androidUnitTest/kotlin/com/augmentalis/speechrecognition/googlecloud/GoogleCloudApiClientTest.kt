/**
 * GoogleCloudApiClientTest.kt - Tests for Google Cloud STT v2 API client internals
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests deterministic helper methods via reflection: PCM conversion,
 * error mapping, backoff calculation, and response parsing.
 */
package com.augmentalis.speechrecognition.googlecloud

import com.augmentalis.speechrecognition.SpeechError
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleCloudApiClientTest {

    private lateinit var client: GoogleCloudApiClient
    private val config = GoogleCloudConfig(
        projectId = "test-project",
        authMode = GoogleCloudAuthMode.API_KEY,
        apiKey = "test-key"
    )

    @Before
    fun setUp() {
        client = GoogleCloudApiClient(config)
    }

    @After
    fun tearDown() {
        client.destroy()
    }

    // ── floatArrayToPcmBytes ─────────────────────────────────────

    @Test
    fun `floatArrayToPcmBytes converts silence to zero bytes`() {
        val silence = FloatArray(10) { 0.0f }
        val pcm = invokeFloatArrayToPcmBytes(silence)

        assertEquals(20, pcm.size) // 10 samples * 2 bytes each
        // All bytes should be 0 for silence
        assertTrue(pcm.all { it.toInt() == 0 })
    }

    @Test
    fun `floatArrayToPcmBytes output has correct size`() {
        val audio = FloatArray(100) { it.toFloat() / 100f }
        val pcm = invokeFloatArrayToPcmBytes(audio)
        assertEquals(200, pcm.size) // 100 samples * 2 bytes
    }

    @Test
    fun `floatArrayToPcmBytes clamps values above 1`() {
        val audio = floatArrayOf(2.0f)
        val pcm = invokeFloatArrayToPcmBytes(audio)

        // 1.0f * 32767 = 32767 = 0x7FFF → LE: [0xFF, 0x7F]
        assertEquals(0xFF.toByte(), pcm[0])
        assertEquals(0x7F.toByte(), pcm[1])
    }

    @Test
    fun `floatArrayToPcmBytes clamps values below negative 1`() {
        val audio = floatArrayOf(-2.0f)
        val pcm = invokeFloatArrayToPcmBytes(audio)

        // -1.0f * 32767 = -32767 → as Short = 0x8001 → LE: [0x01, 0x80]
        assertEquals(0x01.toByte(), pcm[0])
        assertEquals(0x80.toByte(), pcm[1])
    }

    @Test
    fun `floatArrayToPcmBytes handles max positive`() {
        val audio = floatArrayOf(1.0f)
        val pcm = invokeFloatArrayToPcmBytes(audio)

        // 1.0f * 32767 = 32767 = 0x7FFF → LE: [0xFF, 0x7F]
        val sample = (pcm[1].toInt() and 0xFF shl 8) or (pcm[0].toInt() and 0xFF)
        assertEquals(Short.MAX_VALUE.toInt(), sample)
    }

    @Test
    fun `floatArrayToPcmBytes handles empty array`() {
        val pcm = invokeFloatArrayToPcmBytes(FloatArray(0))
        assertEquals(0, pcm.size)
    }

    @Test
    fun `floatArrayToPcmBytes preserves zero crossing`() {
        // Small positive and negative values should produce distinct outputs
        val audio = floatArrayOf(0.001f, -0.001f)
        val pcm = invokeFloatArrayToPcmBytes(audio)

        // Values are small but non-zero → should produce different bytes
        val sample1 = readInt16LE(pcm, 0)
        val sample2 = readInt16LE(pcm, 2)
        assertTrue(sample1 >= 0, "Positive float should produce non-negative Int16")
        assertTrue(sample2 <= 0, "Negative float should produce non-positive Int16")
    }

    // ── mapHttpError ─────────────────────────────────────────────

    @Test
    fun `mapHttpError 400 returns CHECK_CONFIGURATION`() {
        val error = invokeMapHttpError(400, """{"error":{"message":"Bad request"}}""")
        assertEquals(SpeechError.ERROR_CLIENT, error.code)
        assertEquals(SpeechError.Action.CHECK_CONFIGURATION, error.suggestedAction)
        assertNotNull(error.message)
    }

    @Test
    fun `mapHttpError 401 returns REINITIALIZE`() {
        val error = invokeMapHttpError(401, """{"error":{"message":"Unauthorized"}}""")
        assertEquals(SpeechError.ERROR_CLIENT, error.code)
        assertEquals(SpeechError.Action.REINITIALIZE, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 403 returns REINITIALIZE`() {
        val error = invokeMapHttpError(403, """{"error":{"message":"Forbidden"}}""")
        assertEquals(SpeechError.Action.REINITIALIZE, error.suggestedAction)
    }

    @Test
    fun `mapHttpError 429 returns RETRY_WITH_BACKOFF`() {
        val error = invokeMapHttpError(429, """{"error":{"message":"Rate limited"}}""")
        assertEquals(SpeechError.ERROR_SERVER, error.code)
        assertEquals(SpeechError.Action.RETRY_WITH_BACKOFF, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 500 returns RETRY_WITH_BACKOFF`() {
        val error = invokeMapHttpError(500, """{"error":{"message":"Internal error"}}""")
        assertEquals(SpeechError.ERROR_SERVER, error.code)
        assertEquals(SpeechError.Action.RETRY_WITH_BACKOFF, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 503 returns RETRY_WITH_BACKOFF`() {
        val error = invokeMapHttpError(503, """{"error":{"message":"Service unavailable"}}""")
        assertEquals(SpeechError.ERROR_SERVER, error.code)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError unknown code returns LOG_AND_REPORT`() {
        val error = invokeMapHttpError(418, "I'm a teapot")
        assertEquals(SpeechError.ERROR_UNKNOWN, error.code)
        assertEquals(SpeechError.Action.LOG_AND_REPORT, error.suggestedAction)
        assertNotNull(error.message)
    }

    @Test
    fun `mapHttpError handles malformed JSON body`() {
        val error = invokeMapHttpError(500, "not json at all")
        // Should not throw, should fall back to truncated body
        assertNotNull(error.message)
        assertEquals(SpeechError.ERROR_SERVER, error.code)
    }

    @Test
    fun `mapHttpError 400 is not recoverable`() {
        val error = invokeMapHttpError(400, "{}")
        assertEquals(false, error.isRecoverable)
    }

    // ── calculateBackoff ─────────────────────────────────────────

    @Test
    fun `calculateBackoff attempt 0 is around 1000ms`() {
        val backoff = invokeCalculateBackoff(0)
        // Base = 1000ms, jitter up to 20% = 1000-1200ms
        assertTrue(backoff >= 1000, "Backoff attempt 0 should be >= 1000ms, was $backoff")
        assertTrue(backoff <= 1200, "Backoff attempt 0 should be <= 1200ms, was $backoff")
    }

    @Test
    fun `calculateBackoff attempt 1 is around 2000ms`() {
        val backoff = invokeCalculateBackoff(1)
        // 1000 * 2^1 = 2000, jitter up to 20% = 2000-2400ms
        assertTrue(backoff >= 2000, "Backoff attempt 1 should be >= 2000ms, was $backoff")
        assertTrue(backoff <= 2400, "Backoff attempt 1 should be <= 2400ms, was $backoff")
    }

    @Test
    fun `calculateBackoff attempt 2 is around 4000ms`() {
        val backoff = invokeCalculateBackoff(2)
        // 1000 * 2^2 = 4000, jitter up to 20% = 4000-4800ms
        assertTrue(backoff >= 4000, "Backoff attempt 2 should be >= 4000ms, was $backoff")
        assertTrue(backoff <= 4800, "Backoff attempt 2 should be <= 4800ms, was $backoff")
    }

    @Test
    fun `calculateBackoff caps at 10000ms`() {
        val backoff = invokeCalculateBackoff(10)
        // Should cap at 10000ms + up to 20% jitter = max 12000ms
        assertTrue(backoff <= 12000, "Backoff should cap near 10000ms, was $backoff")
    }

    @Test
    fun `calculateBackoff grows exponentially`() {
        val b0 = invokeCalculateBackoff(0)
        val b1 = invokeCalculateBackoff(1)
        val b2 = invokeCalculateBackoff(2)

        // Each should be roughly 2x the previous (within jitter tolerance)
        assertTrue(b1 > b0, "Backoff should grow: b1=$b1 > b0=$b0")
        assertTrue(b2 > b1, "Backoff should grow: b2=$b2 > b1=$b1")
    }

    // ── RecognizeResponse sealed class ───────────────────────────

    @Test
    fun `RecognizeResponse Success wraps result`() {
        val result = com.augmentalis.speechrecognition.RecognitionResult(
            text = "hello",
            confidence = 0.95f,
            engine = "GoogleCloud"
        )
        val response = RecognizeResponse.Success(result)
        assertEquals("hello", response.result.text)
        assertEquals(0.95f, response.result.confidence)
    }

    @Test
    fun `RecognizeResponse Error wraps SpeechError`() {
        val error = SpeechError.networkError("timeout")
        val response = RecognizeResponse.Error(error)
        assertEquals(SpeechError.ERROR_NETWORK, response.error.code)
    }

    // ── Reflection Helpers ───────────────────────────────────────

    private fun invokeFloatArrayToPcmBytes(audioData: FloatArray): ByteArray {
        val method = GoogleCloudApiClient::class.java.getDeclaredMethod(
            "floatArrayToPcmBytes", FloatArray::class.java
        )
        method.isAccessible = true
        return method.invoke(client, audioData) as ByteArray
    }

    private fun invokeMapHttpError(statusCode: Int, responseBody: String): SpeechError {
        val method = GoogleCloudApiClient::class.java.getDeclaredMethod(
            "mapHttpError", Int::class.java, String::class.java
        )
        method.isAccessible = true
        return method.invoke(client, statusCode, responseBody) as SpeechError
    }

    private fun invokeCalculateBackoff(attempt: Int): Long {
        val method = GoogleCloudApiClient::class.java.getDeclaredMethod(
            "calculateBackoff", Int::class.java
        )
        method.isAccessible = true
        return method.invoke(client, attempt) as Long
    }

    private fun readInt16LE(bytes: ByteArray, offset: Int): Int {
        val low = bytes[offset].toInt() and 0xFF
        val high = bytes[offset + 1].toInt() and 0xFF
        val value = (high shl 8) or low
        return if (value >= 0x8000) value - 0x10000 else value
    }
}
