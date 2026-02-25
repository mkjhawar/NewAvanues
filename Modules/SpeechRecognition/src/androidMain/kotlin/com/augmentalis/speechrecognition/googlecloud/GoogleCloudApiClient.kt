/**
 * GoogleCloudApiClient.kt - REST client for Google Cloud STT v2 synchronous recognize
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Handles synchronous (batch) recognition requests to the Google Cloud
 * Speech-to-Text v2 API. Used in VAD_BATCH mode where WhisperVAD detects
 * speech chunks and each chunk is sent as a single recognize request.
 *
 * Audio format: LINEAR16, 16kHz mono — matches WhisperAudio output exactly.
 * Auth: Firebase ID token (Bearer) or API key (query param).
 * Retry: Exponential backoff with jitter for transient failures.
 */
package com.augmentalis.speechrecognition.googlecloud

import android.util.Base64
import android.util.Log
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechError
import com.augmentalis.speechrecognition.WordTimestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random

/**
 * REST client for Google Cloud STT v2 synchronous recognition.
 *
 * Converts Float32 audio (from WhisperAudio) to base64-encoded LINEAR16 PCM,
 * sends to the v2 recognize endpoint, and parses the response into
 * RecognitionResult objects.
 */
class GoogleCloudApiClient(
    private val config: GoogleCloudConfig
) {
    companion object {
        private const val TAG = "GCApiClient"
        private const val ENGINE_NAME = "GoogleCloud"
        private const val SAMPLE_RATE = 16_000
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        // Retry constants
        private const val BACKOFF_BASE_MS = 1000L
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val BACKOFF_MAX_MS = 10_000L
        private const val BACKOFF_JITTER = 0.2
    }

    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    /**
     * Send a speech chunk for synchronous recognition.
     *
     * @param audioData Float32 PCM samples at 16kHz mono (from WhisperAudio)
     * @param speechMode Current speech mode name for result metadata
     * @return RecognitionResult on success, or SpeechError on failure
     */
    suspend fun recognize(
        audioData: FloatArray,
        speechMode: String = "",
        phraseHints: List<String> = emptyList()
    ): RecognizeResponse = withContext(Dispatchers.IO) {
        val pcmBytes = floatArrayToPcmBytes(audioData)
        val audioBase64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val audioDurationMs = (audioData.size * 1000L) / SAMPLE_RATE

        val requestJson = buildRequestJson(audioBase64, phraseHints)

        var lastError: SpeechError? = null

        for (attempt in 0 until config.maxRetries) {
            try {
                val token = getAuthToken()
                val url = buildUrl()

                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                    .apply {
                        if (config.authMode == GoogleCloudAuthMode.FIREBASE_AUTH && token != null) {
                            addHeader("Authorization", "Bearer $token")
                        }
                    }
                    .build()

                val startTimeMs = System.currentTimeMillis()
                val response = httpClient.newCall(request).execute()
                val processingTimeMs = System.currentTimeMillis() - startTimeMs

                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val result = parseResponse(responseBody, audioDurationMs, processingTimeMs, speechMode)
                    return@withContext RecognizeResponse.Success(result)
                }

                val error = mapHttpError(response.code, responseBody)

                // On 401, try refreshing the token once
                if (response.code == 401 && attempt == 0 && config.authMode == GoogleCloudAuthMode.FIREBASE_AUTH) {
                    Log.w(TAG, "Auth token expired, refreshing...")
                    refreshAuthToken()
                    continue
                }

                // Don't retry client errors (except 401 handled above and 429)
                if (response.code in 400..499 && response.code != 429) {
                    return@withContext RecognizeResponse.Error(error)
                }

                lastError = error

            } catch (e: IOException) {
                Log.w(TAG, "Network error on attempt ${attempt + 1}: ${e.message}")
                lastError = SpeechError.networkError("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error on attempt ${attempt + 1}", e)
                lastError = SpeechError(
                    code = SpeechError.ERROR_UNKNOWN,
                    message = "Recognition failed: ${e.message}",
                    isRecoverable = false,
                    suggestedAction = SpeechError.Action.LOG_AND_REPORT
                )
                return@withContext RecognizeResponse.Error(lastError)
            }

            // Exponential backoff with jitter before retry
            if (attempt < config.maxRetries - 1) {
                val backoffMs = calculateBackoff(attempt)
                Log.d(TAG, "Retrying in ${backoffMs}ms (attempt ${attempt + 1}/${config.maxRetries})")
                delay(backoffMs)
            }
        }

        RecognizeResponse.Error(lastError ?: SpeechError.networkError("All retry attempts exhausted"))
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    // --- Private implementation ---

    /**
     * Convert Float32 audio samples [-1.0, 1.0] to Int16 LE PCM bytes.
     * This is the reverse of WhisperAudio's Int16 → Float32 conversion.
     */
    private fun floatArrayToPcmBytes(audioData: FloatArray): ByteArray {
        val pcm = ByteArray(audioData.size * 2) // 2 bytes per Int16 sample
        for (i in audioData.indices) {
            val clamped = audioData[i].coerceIn(-1.0f, 1.0f)
            val intSample = (clamped * Short.MAX_VALUE).toInt().toShort()
            pcm[i * 2] = (intSample.toInt() and 0xFF).toByte()           // low byte
            pcm[i * 2 + 1] = (intSample.toInt() shr 8 and 0xFF).toByte() // high byte
        }
        return pcm
    }

    /**
     * Build the JSON request body for the v2 recognize endpoint.
     */
    private fun buildRequestJson(audioBase64: String, phraseHints: List<String> = emptyList()): String {
        val request = JsonObject().apply {
            add("config", JsonObject().apply {
                add("autoDecodingConfig", JsonObject())
                val languageCodes = com.google.gson.JsonArray()
                languageCodes.add(config.language)
                add("languageCodes", languageCodes)
                addProperty("model", config.model)
                add("features", JsonObject().apply {
                    addProperty("enableAutomaticPunctuation", config.enableAutoPunctuation)
                    addProperty("enableWordTimeOffsets", config.enableWordTimeOffsets)
                    addProperty("enableWordConfidence", config.enableWordConfidence)
                    addProperty("maxAlternatives", config.maxAlternatives)
                    addProperty("profanityFilter", config.profanityFilter)
                })

                // Phrase hints via adaptation — biases recognition toward expected commands
                if (phraseHints.isNotEmpty()) {
                    add("adaptation", JsonObject().apply {
                        val phraseSets = com.google.gson.JsonArray()
                        phraseSets.add(JsonObject().apply {
                            val phrases = com.google.gson.JsonArray()
                            phraseHints.take(500).forEach { hint ->
                                phrases.add(JsonObject().apply {
                                    addProperty("value", hint)
                                    addProperty("boost", 10.0)
                                })
                            }
                            add("phrases", phrases)
                        })
                        add("phraseSets", phraseSets)
                    })
                }
            })
            addProperty("content", audioBase64)
        }

        return gson.toJson(request)
    }

    /**
     * Build the full URL, appending API key if using API_KEY auth mode.
     */
    private fun buildUrl(): String {
        val baseUrl = config.buildRecognizeUrl()
        return if (config.authMode == GoogleCloudAuthMode.API_KEY && !config.apiKey.isNullOrBlank()) {
            "$baseUrl?key=${config.apiKey}"
        } else {
            baseUrl
        }
    }

    /**
     * Get the auth token based on configured auth mode.
     */
    private suspend fun getAuthToken(): String? {
        return when (config.authMode) {
            GoogleCloudAuthMode.FIREBASE_AUTH -> {
                try {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        Log.w(TAG, "No Firebase user signed in")
                        return null
                    }
                    user.getIdToken(false).await().token
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get Firebase ID token", e)
                    null
                }
            }
            GoogleCloudAuthMode.API_KEY -> null // API key is passed as query param
        }
    }

    /**
     * Force-refresh the Firebase auth token.
     */
    private suspend fun refreshAuthToken(): String? {
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return null
            user.getIdToken(true).await().token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh Firebase ID token", e)
            null
        }
    }

    /**
     * Parse the v2 recognize response into a RecognitionResult.
     */
    private fun parseResponse(
        responseBody: String,
        audioDurationMs: Long,
        processingTimeMs: Long,
        speechMode: String
    ): RecognitionResult {
        val json = JsonParser.parseString(responseBody).asJsonObject
        val results = json.getAsJsonArray("results") ?: return RecognitionResult.empty(ENGINE_NAME, speechMode)

        if (results.size() == 0) {
            return RecognitionResult.empty(ENGINE_NAME, speechMode)
        }

        val firstResult = results[0].asJsonObject
        val alternatives = firstResult.getAsJsonArray("alternatives")
            ?: return RecognitionResult.empty(ENGINE_NAME, speechMode)

        if (alternatives.size() == 0) {
            return RecognitionResult.empty(ENGINE_NAME, speechMode)
        }

        val bestAlternative = alternatives[0].asJsonObject
        val transcript = bestAlternative.get("transcript")?.asString ?: ""
        val confidence = bestAlternative.get("confidence")?.asFloat ?: 0f

        // Parse word-level timestamps
        val wordTimestamps = mutableListOf<WordTimestamp>()
        bestAlternative.getAsJsonArray("words")?.forEach { wordElement ->
            val wordObj = wordElement.asJsonObject
            val word = wordObj.get("word")?.asString ?: ""
            val startOffset = parseDuration(wordObj.get("startOffset")?.asString)
            val endOffset = parseDuration(wordObj.get("endOffset")?.asString)
            val wordConfidence = wordObj.get("confidence")?.asFloat ?: confidence
            wordTimestamps.add(WordTimestamp(word, startOffset, endOffset, wordConfidence))
        }

        // Collect alternative transcripts
        val altTexts = mutableListOf<String>()
        for (i in 1 until alternatives.size()) {
            alternatives[i].asJsonObject.get("transcript")?.asString?.let { altTexts.add(it) }
        }

        return RecognitionResult(
            text = transcript,
            originalText = transcript,
            confidence = confidence,
            isPartial = false,
            isFinal = true,
            engine = ENGINE_NAME,
            mode = speechMode,
            alternatives = altTexts,
            wordTimestamps = wordTimestamps.ifEmpty { null },
            metadata = mapOf(
                "processingTimeMs" to processingTimeMs,
                "audioDurationMs" to audioDurationMs,
                "model" to config.model,
                "language" to config.language
            )
        )
    }

    /**
     * Parse a protobuf Duration string (e.g., "1.500s") to seconds as Float.
     */
    private fun parseDuration(durationStr: String?): Float {
        if (durationStr == null) return 0f
        val cleaned = durationStr.removeSuffix("s")
        return cleaned.toFloatOrNull() ?: 0f
    }

    /**
     * Map HTTP status codes to SpeechError.
     */
    private fun mapHttpError(statusCode: Int, responseBody: String): SpeechError {
        val detail = try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.getAsJsonObject("error")?.get("message")?.asString ?: responseBody.take(200)
        } catch (e: Exception) {
            responseBody.take(200)
        }

        return when (statusCode) {
            400 -> SpeechError(
                code = SpeechError.ERROR_CLIENT,
                message = "Bad request: $detail",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.CHECK_CONFIGURATION
            )
            401, 403 -> SpeechError(
                code = SpeechError.ERROR_CLIENT,
                message = "Authentication failed: $detail",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.REINITIALIZE
            )
            429 -> SpeechError(
                code = SpeechError.ERROR_SERVER,
                message = "Rate limited: $detail",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY_WITH_BACKOFF
            )
            in 500..599 -> SpeechError(
                code = SpeechError.ERROR_SERVER,
                message = "Server error ($statusCode): $detail",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY_WITH_BACKOFF
            )
            else -> SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = "HTTP $statusCode: $detail",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.LOG_AND_REPORT
            )
        }
    }

    /**
     * Calculate exponential backoff delay with jitter.
     */
    private fun calculateBackoff(attempt: Int): Long {
        val exponentialMs = BACKOFF_BASE_MS * Math.pow(BACKOFF_MULTIPLIER, attempt.toDouble()).toLong()
        val cappedMs = min(exponentialMs, BACKOFF_MAX_MS)
        val jitter = (cappedMs * BACKOFF_JITTER * Random.nextDouble()).toLong()
        return cappedMs + jitter
    }
}

/**
 * Result type for recognize operations.
 */
sealed class RecognizeResponse {
    data class Success(val result: RecognitionResult) : RecognizeResponse()
    data class Error(val error: SpeechError) : RecognizeResponse()
}
