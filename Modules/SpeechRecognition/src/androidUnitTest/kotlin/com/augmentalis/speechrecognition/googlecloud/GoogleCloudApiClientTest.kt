/**
 * GoogleCloudApiClientTest.kt - Tests for Google Cloud STT v2 API client internals
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Tests deterministic helper methods via reflection: PCM conversion,
 * error mapping, backoff calculation, and response parsing.
 *
 * Design note — mapHttpError error mapping (actual implementation):
 *   400            → CHECK_CONFIGURATION, isRecoverable = false
 *   401, 403       → REINITIALIZE,        isRecoverable = true
 *   429            → RETRY_WITH_BACKOFF,  isRecoverable = true
 *   500-599        → RETRY_WITH_BACKOFF,  isRecoverable = true
 *   anything else  → LOG_AND_REPORT,      isRecoverable = false
 *
 * calculateBackoff formula:
 *   base = 1000L * 2^attempt, capped at 10_000L
 *   result = capped + (capped * 0.2 * Random.nextDouble())
 *   Therefore result is in [capped, capped * 1.2]
 */
package com.augmentalis.speechrecognition.googlecloud

import com.augmentalis.speechrecognition.SpeechError
import org.junit.After
import org.junit.Before
import org.junit.Test
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

        // 1.0f clamped * Short.MAX_VALUE (32767) = 32767 = 0x7FFF
        // Little-endian: low byte = 0xFF, high byte = 0x7F
        assertEquals(0xFF.toByte(), pcm[0])
        assertEquals(0x7F.toByte(), pcm[1])
    }

    @Test
    fun `floatArrayToPcmBytes clamps values below negative 1`() {
        val audio = floatArrayOf(-2.0f)
        val pcm = invokeFloatArrayToPcmBytes(audio)

        // -1.0f clamped * 32767 = -32767 → as Short = 0x8001
        // Little-endian: low byte = 0x01, high byte = 0x80
        assertEquals(0x01.toByte(), pcm[0])
        assertEquals(0x80.toByte(), pcm[1])
    }

    @Test
    fun `floatArrayToPcmBytes handles max positive`() {
        val audio = floatArrayOf(1.0f)
        val pcm = invokeFloatArrayToPcmBytes(audio)

        // Reconstruct Int16 from little-endian bytes
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
        // Small positive and negative values should produce distinct Int16 signs
        val audio = floatArrayOf(0.001f, -0.001f)
        val pcm = invokeFloatArrayToPcmBytes(audio)

        val sample1 = readInt16LE(pcm, 0)
        val sample2 = readInt16LE(pcm, 2)
        assertTrue(sample1 >= 0, "Positive float should produce non-negative Int16, was $sample1")
        assertTrue(sample2 <= 0, "Negative float should produce non-positive Int16, was $sample2")
    }

    // ── mapHttpError ─────────────────────────────────────────────

    @Test
    fun `mapHttpError 400 returns CHECK_CONFIGURATION and is not recoverable`() {
        val error = invokeMapHttpError(400, """{"error":{"message":"Bad request"}}""")
        assertEquals(SpeechError.ERROR_CLIENT, error.code)
        assertEquals(SpeechError.Action.CHECK_CONFIGURATION, error.suggestedAction)
        assertEquals(false, error.isRecoverable)
        assertNotNull(error.message)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `mapHttpError 401 returns REINITIALIZE and is recoverable`() {
        val error = invokeMapHttpError(401, """{"error":{"message":"Unauthorized"}}""")
        assertEquals(SpeechError.ERROR_CLIENT, error.code)
        assertEquals(SpeechError.Action.REINITIALIZE, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 403 returns REINITIALIZE and is recoverable`() {
        // 401 and 403 share the same branch in the implementation
        val error = invokeMapHttpError(403, """{"error":{"message":"Forbidden"}}""")
        assertEquals(SpeechError.ERROR_CLIENT, error.code)
        assertEquals(SpeechError.Action.REINITIALIZE, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 429 returns RETRY_WITH_BACKOFF and is recoverable`() {
        val error = invokeMapHttpError(429, """{"error":{"message":"Rate limited"}}""")
        assertEquals(SpeechError.ERROR_SERVER, error.code)
        assertEquals(SpeechError.Action.RETRY_WITH_BACKOFF, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 500 returns RETRY_WITH_BACKOFF and is recoverable`() {
        val error = invokeMapHttpError(500, """{"error":{"message":"Internal error"}}""")
        assertEquals(SpeechError.ERROR_SERVER, error.code)
        assertEquals(SpeechError.Action.RETRY_WITH_BACKOFF, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 503 returns RETRY_WITH_BACKOFF and is recoverable`() {
        // 503 falls in the 500-599 range, which maps to RETRY_WITH_BACKOFF
        val error = invokeMapHttpError(503, """{"error":{"message":"Service unavailable"}}""")
        assertEquals(SpeechError.ERROR_SERVER, error.code)
        assertEquals(SpeechError.Action.RETRY_WITH_BACKOFF, error.suggestedAction)
        assertTrue(error.isRecoverable)
    }

    @Test
    fun `mapHttpError 502 returns RETRY_WITH_BACKOFF`() {
        val error = invokeMapHttpError(502, """{"error":{"message":"Bad gateway"}}""")
        assertEquals(SpeechError.ERROR_SERVER, error.code)
        assertEquals(SpeechError.Action.RETRY_WITH_BACKOFF, error.suggestedAction)
    }

    @Test
    fun `mapHttpError unknown code returns LOG_AND_REPORT and is not recoverable`() {
        val error = invokeMapHttpError(418, "I'm a teapot")
        assertEquals(SpeechError.ERROR_UNKNOWN, error.code)
        assertEquals(SpeechError.Action.LOG_AND_REPORT, error.suggestedAction)
        assertEquals(false, error.isRecoverable)
        assertNotNull(error.message)
    }

    @Test
    fun `mapHttpError handles malformed JSON body gracefully`() {
        // Must not throw; falls back to truncated raw body in the error message
        val error = invokeMapHttpError(500, "not json at all")
        assertNotNull(error.message)
        assertTrue(error.message.isNotBlank())
        assertEquals(SpeechError.ERROR_SERVER, error.code)
    }

    @Test
    fun `mapHttpError handles empty body gracefully`() {
        val error = invokeMapHttpError(500, "")
        assertNotNull(error.message)
        assertEquals(SpeechError.ERROR_SERVER, error.code)
    }

    // ── calculateBackoff ─────────────────────────────────────────
    //
    // Formula: result in [base * 2^attempt capped at 10_000,
    //                      base * 2^attempt capped at 10_000 * 1.2]
    // Jitter is additive and strictly non-negative (Random.nextDouble() in [0, 1)).

    @Test
    fun `calculateBackoff attempt 0 is in range 1000-1200ms`() {
        // base = 1000 * 2^0 = 1000, jitter adds 0-20% → [1000, 1200)
        val backoff = invokeCalculateBackoff(0)
        assertTrue(backoff >= 1000L, "Attempt 0 backoff should be >= 1000ms, was $backoff")
        assertTrue(backoff < 1201L, "Attempt 0 backoff should be < 1201ms, was $backoff")
    }

    @Test
    fun `calculateBackoff attempt 1 is in range 2000-2400ms`() {
        // base = 1000 * 2^1 = 2000, jitter adds 0-20% → [2000, 2400)
        val backoff = invokeCalculateBackoff(1)
        assertTrue(backoff >= 2000L, "Attempt 1 backoff should be >= 2000ms, was $backoff")
        assertTrue(backoff < 2401L, "Attempt 1 backoff should be < 2401ms, was $backoff")
    }

    @Test
    fun `calculateBackoff attempt 2 is in range 4000-4800ms`() {
        // base = 1000 * 2^2 = 4000, jitter adds 0-20% → [4000, 4800)
        val backoff = invokeCalculateBackoff(2)
        assertTrue(backoff >= 4000L, "Attempt 2 backoff should be >= 4000ms, was $backoff")
        assertTrue(backoff < 4801L, "Attempt 2 backoff should be < 4801ms, was $backoff")
    }

    @Test
    fun `calculateBackoff caps at 10000ms plus jitter`() {
        // 2^10 * 1000 = 1_024_000 but capped at 10_000, jitter adds 0-20% → [10000, 12000)
        val backoff = invokeCalculateBackoff(10)
        assertTrue(backoff >= 10_000L, "Backoff should be >= 10_000ms (the cap), was $backoff")
        assertTrue(backoff < 12_001L, "Backoff should be < 12_001ms (cap + max jitter), was $backoff")
    }

    @Test
    fun `calculateBackoff grows monotonically across early attempts`() {
        val b0 = invokeCalculateBackoff(0)
        val b1 = invokeCalculateBackoff(1)
        val b2 = invokeCalculateBackoff(2)

        // Base doubles each step; jitter is at most 20% so growth is guaranteed
        // b1 base (2000) > b0 max (1200); b2 base (4000) > b1 max (2400)
        assertTrue(b1 > b0, "Backoff should grow: b1=$b1 must be > b0=$b0")
        assertTrue(b2 > b1, "Backoff should grow: b2=$b2 must be > b1=$b1")
    }

    // ── RecognizeResponse sealed class ───────────────────────────

    @Test
    fun `RecognizeResponse Success wraps result correctly`() {
        val result = com.augmentalis.speechrecognition.RecognitionResult(
            text = "hello world",
            confidence = 0.95f,
            engine = "GoogleCloud"
        )
        val response = RecognizeResponse.Success(result)

        assertEquals("hello world", response.result.text)
        assertEquals(0.95f, response.result.confidence)
        assertEquals("GoogleCloud", response.result.engine)
    }

    @Test
    fun `RecognizeResponse Error wraps SpeechError correctly`() {
        val error = SpeechError.networkError("connection timeout")
        val response = RecognizeResponse.Error(error)

        assertEquals(SpeechError.ERROR_NETWORK, response.error.code)
        assertTrue(response.error.isRecoverable)
        assertEquals(SpeechError.Action.CHECK_CONNECTION, response.error.suggestedAction)
    }

    @Test
    fun `RecognizeResponse Success and Error are distinct types`() {
        val success = RecognizeResponse.Success(
            com.augmentalis.speechrecognition.RecognitionResult(
                text = "ok",
                confidence = 1.0f
            )
        )
        val error = RecognizeResponse.Error(SpeechError.networkError())

        assertTrue(success is RecognizeResponse.Success)
        assertTrue(error is RecognizeResponse.Error)
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
