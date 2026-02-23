/**
 * GoogleCloudStreamingClient.kt - HTTP/2 streaming client for Google Cloud STT v2
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Real-time streaming speech recognition via the v2 streamingRecognize endpoint.
 * Uses OkHttp with HTTP/2 and chunked transfer encoding to send audio continuously
 * and receive partial + final recognition results.
 *
 * Lifecycle: startStreaming() -> sendAudioChunk() (repeated) -> stopStreaming()
 * Results emitted via SharedFlow for the engine to collect.
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * HTTP/2 streaming client for Google Cloud STT v2.
 *
 * Sends a streaming config message first, then continuously streams
 * audio chunks and reads back partial/final recognition results.
 * Uses a Channel as an audio queue between the producer (mic capture)
 * and consumer (HTTP stream).
 */
class GoogleCloudStreamingClient(
    private val config: GoogleCloudConfig,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "GCStreamClient"
        private const val ENGINE_NAME = "GoogleCloud"
        private const val SAMPLE_RATE = 16_000
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

        // Reconnection constants
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_BASE_DELAY_MS = 1000L
        private const val RECONNECT_MAX_DELAY_MS = 15_000L

        // Stream duration limit (Google imposes ~5 min per stream)
        private const val STREAM_MAX_DURATION_MS = 290_000L // ~4:50 — leave margin
    }

    private val gson = Gson()
    private val isStreaming = AtomicBoolean(false)

    // Audio queue: mic thread produces, HTTP stream thread consumes.
    // Declared as var because each streaming session rebuilds a fresh channel —
    // a closed channel cannot be reused after stopStreaming() or session rotation.
    private var audioQueue = Channel<ByteArray>(Channel.UNLIMITED)

    // Result flow
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    private var streamJob: Job? = null
    private var reconnectAttempts = 0

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Streaming — no read timeout
        .writeTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .protocols(listOf(okhttp3.Protocol.H2_PRIOR_KNOWLEDGE, okhttp3.Protocol.HTTP_1_1))
        .build()

    /**
     * Start the streaming recognition session.
     * Launches a background coroutine that manages the HTTP/2 stream lifecycle.
     */
    private var currentPhraseHints: List<String> = emptyList()

    fun startStreaming(speechMode: String = "", phraseHints: List<String> = emptyList()) {
        if (isStreaming.getAndSet(true)) {
            Log.w(TAG, "Already streaming")
            return
        }

        currentPhraseHints = phraseHints
        reconnectAttempts = 0

        streamJob = scope.launch(Dispatchers.IO) {
            streamLoop(speechMode)
        }

        Log.i(TAG, "Streaming started (phraseHints=${phraseHints.size})")
    }

    /**
     * Send an audio chunk to the streaming session.
     * Converts Float32 samples to Int16 LE PCM bytes and enqueues.
     *
     * @param audioData Float32 PCM samples at 16kHz mono
     */
    fun sendAudioChunk(audioData: FloatArray) {
        if (!isStreaming.get()) return
        val pcmBytes = floatArrayToPcmBytes(audioData)
        audioQueue.trySend(pcmBytes)
    }

    /**
     * Stop the streaming session gracefully.
     */
    fun stopStreaming() {
        if (!isStreaming.getAndSet(false)) return

        audioQueue.close()
        streamJob?.cancel()
        streamJob = null

        Log.i(TAG, "Streaming stopped")
    }

    /**
     * Check if currently streaming.
     */
    fun isStreaming(): Boolean = isStreaming.get()

    /**
     * Clean up resources.
     */
    fun destroy() {
        stopStreaming()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    // --- Private implementation ---

    /**
     * Main streaming loop with auto-reconnection.
     * Each iteration opens a new HTTP/2 stream, sends the config message,
     * then continuously sends audio chunks until the stream ends or errors.
     */
    private suspend fun streamLoop(speechMode: String) {
        while (isStreaming.get() && scope.isActive) {
            try {
                performStreamSession(speechMode)
                // If we exit normally (stream duration exceeded), reconnect
                if (isStreaming.get()) {
                    reconnectAttempts = 0 // Reset on clean exit
                    Log.i(TAG, "Stream session ended normally, reconnecting...")
                    continue
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "Stream connection error: ${e.message}")
                if (!handleReconnect()) break
            } catch (e: Exception) {
                Log.e(TAG, "Stream error", e)
                scope.launch {
                    _errorFlow.emit(SpeechError(
                        code = SpeechError.ERROR_UNKNOWN,
                        message = "Streaming error: ${e.message}",
                        isRecoverable = true,
                        suggestedAction = SpeechError.Action.RETRY
                    ))
                }
                if (!handleReconnect()) break
            }
        }
    }

    /**
     * Perform a single streaming session.
     * Opens the HTTP/2 connection, sends config, streams audio, reads results.
     */
    private suspend fun performStreamSession(speechMode: String) {
        // Rebuild the audio queue for this session. The previous session's channel
        // is closed by stopStreaming() or by the 4:50-min rotation in writeTo().
        // A closed Channel cannot receive new items, so we must replace it before
        // any sendAudioChunk() calls arrive for this new session.
        audioQueue = Channel(Channel.UNLIMITED)

        val token = getAuthToken()
        val url = buildStreamingUrl()

        // Build the initial config message
        val configJson = buildStreamingConfigJson()

        // Create a streaming request body that writes config + audio chunks
        val requestBody = object : RequestBody() {
            override fun contentType() = JSON_MEDIA_TYPE.toMediaType()
            override fun isOneShot() = true

            override fun writeTo(sink: BufferedSink) {
                // Write the config as the first message (newline-delimited JSON)
                sink.writeUtf8(configJson)
                sink.writeUtf8("\n")
                sink.flush()

                val streamStartMs = System.currentTimeMillis()

                // Stream audio chunks from the queue
                while (isStreaming.get()) {
                    val result = audioQueue.tryReceive()
                    if (result.isSuccess) {
                        val pcmBytes = result.getOrNull() ?: continue
                        val audioBase64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)

                        val audioMsg = JsonObject().apply {
                            addProperty("audio", audioBase64)
                        }
                        sink.writeUtf8(gson.toJson(audioMsg))
                        sink.writeUtf8("\n")
                        sink.flush()
                    } else if (result.isClosed) {
                        break
                    } else {
                        // No data available — brief yield
                        Thread.sleep(10)
                    }

                    // Check stream duration limit
                    if (System.currentTimeMillis() - streamStartMs > STREAM_MAX_DURATION_MS) {
                        Log.i(TAG, "Stream duration limit reached, will reconnect")
                        break
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .apply {
                if (config.authMode == GoogleCloudAuthMode.FIREBASE_AUTH && token != null) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        withContext(Dispatchers.IO) {
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Stream request failed: ${response.code} — $errorBody")
                scope.launch {
                    _errorFlow.emit(SpeechError(
                        code = if (response.code in 400..499) SpeechError.ERROR_CLIENT else SpeechError.ERROR_SERVER,
                        message = "Stream failed (${response.code}): ${errorBody.take(200)}",
                        isRecoverable = response.code != 400,
                        suggestedAction = if (response.code == 401) SpeechError.Action.REINITIALIZE
                        else SpeechError.Action.RETRY_WITH_BACKOFF
                    ))
                }
                return@withContext
            }

            // Read streaming response lines (newline-delimited JSON)
            response.body?.source()?.let { source ->
                while (!source.exhausted() && isStreaming.get()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue

                    try {
                        parseStreamingResponse(line, speechMode)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse streaming response: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Parse a single streaming response line and emit results.
     */
    private suspend fun parseStreamingResponse(jsonLine: String, speechMode: String) {
        val json = JsonParser.parseString(jsonLine).asJsonObject
        val results = json.getAsJsonArray("results") ?: return

        for (resultElement in results) {
            val resultObj = resultElement.asJsonObject
            val isFinal = resultObj.get("isFinal")?.asBoolean ?: false
            val alternatives = resultObj.getAsJsonArray("alternatives") ?: continue

            if (alternatives.size() == 0) continue

            val bestAlt = alternatives[0].asJsonObject
            val transcript = bestAlt.get("transcript")?.asString ?: ""
            val confidence = bestAlt.get("confidence")?.asFloat ?: 0f

            if (transcript.isBlank()) continue

            // Parse word timestamps for final results
            val wordTimestamps = if (isFinal) {
                val words = mutableListOf<WordTimestamp>()
                bestAlt.getAsJsonArray("words")?.forEach { wordElement ->
                    val wordObj = wordElement.asJsonObject
                    words.add(WordTimestamp(
                        word = wordObj.get("word")?.asString ?: "",
                        startTime = parseDuration(wordObj.get("startOffset")?.asString),
                        endTime = parseDuration(wordObj.get("endOffset")?.asString),
                        confidence = wordObj.get("confidence")?.asFloat ?: confidence
                    ))
                }
                words.ifEmpty { null }
            } else null

            // Collect alternative transcripts
            val altTexts = mutableListOf<String>()
            for (i in 1 until alternatives.size()) {
                alternatives[i].asJsonObject.get("transcript")?.asString?.let { altTexts.add(it) }
            }

            _resultFlow.emit(RecognitionResult(
                text = transcript,
                originalText = transcript,
                confidence = if (isFinal) confidence else 0f,
                isPartial = !isFinal,
                isFinal = isFinal,
                engine = ENGINE_NAME,
                mode = speechMode,
                alternatives = altTexts,
                wordTimestamps = wordTimestamps,
                metadata = mapOf(
                    "streaming" to true,
                    "model" to config.model,
                    "language" to config.language
                )
            ))
        }
    }

    /**
     * Build the initial streaming configuration JSON message.
     */
    private fun buildStreamingConfigJson(): String {
        val config = JsonObject().apply {
            add("config", JsonObject().apply {
                add("autoDecodingConfig", JsonObject())
                val languageCodes = com.google.gson.JsonArray()
                languageCodes.add(this@GoogleCloudStreamingClient.config.language)
                add("languageCodes", languageCodes)
                addProperty("model", this@GoogleCloudStreamingClient.config.model)
                add("features", JsonObject().apply {
                    addProperty("enableAutomaticPunctuation", this@GoogleCloudStreamingClient.config.enableAutoPunctuation)
                    addProperty("enableWordTimeOffsets", this@GoogleCloudStreamingClient.config.enableWordTimeOffsets)
                    addProperty("enableWordConfidence", this@GoogleCloudStreamingClient.config.enableWordConfidence)
                    addProperty("maxAlternatives", this@GoogleCloudStreamingClient.config.maxAlternatives)
                    addProperty("profanityFilter", this@GoogleCloudStreamingClient.config.profanityFilter)
                })

                // Phrase hints via adaptation — biases streaming recognition toward expected commands
                if (currentPhraseHints.isNotEmpty()) {
                    add("adaptation", JsonObject().apply {
                        val phraseSets = com.google.gson.JsonArray()
                        phraseSets.add(JsonObject().apply {
                            val phrases = com.google.gson.JsonArray()
                            currentPhraseHints.take(500).forEach { hint ->
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
            add("streamingConfig", JsonObject().apply {
                add("streamingFeatures", JsonObject().apply {
                    addProperty("enableVoiceActivityEvents", true)
                    addProperty("interimResults", true)
                })
            })
        }
        return gson.toJson(config)
    }

    /**
     * Build streaming URL with optional API key.
     */
    private fun buildStreamingUrl(): String {
        val baseUrl = config.buildStreamingUrl()
        return if (config.authMode == GoogleCloudAuthMode.API_KEY && !config.apiKey.isNullOrBlank()) {
            "$baseUrl?key=${config.apiKey}"
        } else {
            baseUrl
        }
    }

    /**
     * Handle reconnection with exponential backoff.
     * @return true if should retry, false if max attempts exceeded
     */
    private suspend fun handleReconnect(): Boolean {
        reconnectAttempts++
        if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached ($MAX_RECONNECT_ATTEMPTS)")
            isStreaming.set(false)
            scope.launch {
                _errorFlow.emit(SpeechError.networkError("Streaming connection lost after $MAX_RECONNECT_ATTEMPTS attempts"))
            }
            return false
        }

        val delayMs = (RECONNECT_BASE_DELAY_MS * Math.pow(2.0, (reconnectAttempts - 1).toDouble()).toLong())
            .coerceAtMost(RECONNECT_MAX_DELAY_MS)
        Log.i(TAG, "Reconnecting in ${delayMs}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
        delay(delayMs)
        return true
    }

    private suspend fun getAuthToken(): String? {
        return when (config.authMode) {
            GoogleCloudAuthMode.FIREBASE_AUTH -> {
                try {
                    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get Firebase ID token", e)
                    null
                }
            }
            GoogleCloudAuthMode.API_KEY -> null
        }
    }

    private fun parseDuration(durationStr: String?): Float {
        if (durationStr == null) return 0f
        return durationStr.removeSuffix("s").toFloatOrNull() ?: 0f
    }

    /**
     * Convert Float32 audio samples [-1.0, 1.0] to Int16 LE PCM bytes.
     */
    private fun floatArrayToPcmBytes(audioData: FloatArray): ByteArray {
        val pcm = ByteArray(audioData.size * 2)
        for (i in audioData.indices) {
            val clamped = audioData[i].coerceIn(-1.0f, 1.0f)
            val intSample = (clamped * Short.MAX_VALUE).toInt().toShort()
            pcm[i * 2] = (intSample.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = (intSample.toInt() shr 8 and 0xFF).toByte()
        }
        return pcm
    }
}
