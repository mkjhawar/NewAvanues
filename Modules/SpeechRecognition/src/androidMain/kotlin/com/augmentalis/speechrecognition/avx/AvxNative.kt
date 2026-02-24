/**
 * AvxNative.kt - Thread-safe wrapper around Sherpa-ONNX OnlineRecognizer for Android
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Provides thread-safe access to the Sherpa-ONNX streaming transducer recognizer.
 * Wraps the official com.k2fsa.sherpa.onnx Kotlin API rather than raw JNI.
 *
 * Responsibilities:
 * 1. Thread safety — all recognizer calls serialized through lock
 * 2. Hot words file management — writes hot words to temp file for Sherpa config
 * 3. Model lifecycle — create/release OnlineRecognizer and OnlineStream
 * 4. Audio streaming — acceptWaveform + decode + getResult pipeline
 *
 * Native library: libsherpa-onnx-jni.so (bundled in sherpa-onnx AAR)
 */
package com.augmentalis.speechrecognition.avx

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File

// AvxTranscriptionResult and AvxModelPaths are defined in commonMain/AvxModels.kt

/**
 * Thread-safe wrapper around Sherpa-ONNX OnlineRecognizer.
 *
 * Unlike the previous raw JNI approach, this uses the official Sherpa-ONNX
 * Kotlin API (OnlineRecognizer, OnlineStream) which handles native library
 * loading and session management internally.
 */
class AvxNative(private val context: Context) {

    companion object {
        private const val TAG = "AvxNative"
    }

    private val lock = Any()

    @Volatile
    private var recognizer: OnlineRecognizer? = null

    @Volatile
    private var stream: OnlineStream? = null

    @Volatile
    private var hotWordsFile: File? = null

    /**
     * Create and configure the OnlineRecognizer with a transducer model.
     *
     * @param modelPaths Paths to the decrypted model files (encoder, decoder, joiner, tokens)
     * @param config AVX engine configuration
     * @return true if recognizer was created successfully
     */
    fun createRecognizer(modelPaths: AvxModelPaths, config: AvxConfig): Boolean {
        return synchronized(lock) {
            try {
                // Release any existing recognizer
                releaseInternal()

                // Write hot words file if commands are set
                val hwFile = writeHotWordsFile(config.hotWords)

                val transducerConfig = OnlineTransducerModelConfig(
                    encoder = modelPaths.encoderPath,
                    decoder = modelPaths.decoderPath,
                    joiner = modelPaths.joinerPath
                )

                val modelConfig = OnlineModelConfig(
                    transducer = transducerConfig,
                    tokens = modelPaths.tokensPath,
                    numThreads = config.effectiveThreadCount(),
                    debug = false,
                    provider = "cpu"
                )

                // Endpoint detection: tuned for short voice commands
                val endpointConfig = EndpointConfig(
                    rule1 = EndpointRule(
                        mustContainNonSilence = false,
                        minTrailingSilence = 2.4f,   // 2.4s pure silence = end
                        minUtteranceLength = 0f
                    ),
                    rule2 = EndpointRule(
                        mustContainNonSilence = true,
                        minTrailingSilence = 0.8f,    // 0.8s silence after speech = end
                        minUtteranceLength = 0f
                    ),
                    rule3 = EndpointRule(
                        mustContainNonSilence = false,
                        minTrailingSilence = 0f,
                        minUtteranceLength = 15f      // Max 15s utterance
                    )
                )

                val recognizerConfig = OnlineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = config.sampleRate, featureDim = 80),
                    modelConfig = modelConfig,
                    endpointConfig = endpointConfig,
                    enableEndpoint = config.enableEndpoint,
                    decodingMethod = config.decodingMethod,
                    maxActivePaths = config.maxActivePaths,
                    hotwordsFile = hwFile?.absolutePath ?: "",
                    hotwordsScore = config.defaultHotWordBoost,
                    blankPenalty = config.blankPenalty
                )

                recognizer = OnlineRecognizer(config = recognizerConfig)
                stream = recognizer?.createStream()
                hotWordsFile = hwFile

                Log.i(TAG, "OnlineRecognizer created: lang=${config.language.langCode}, " +
                        "threads=${config.effectiveThreadCount()}, " +
                        "hotWords=${config.hotWords.size}, " +
                        "decoding=${config.decodingMethod}")
                true

            } catch (e: Exception) {
                Log.e(TAG, "Failed to create OnlineRecognizer", e)
                releaseInternal()
                false
            }
        }
    }

    /**
     * Feed audio samples to the recognizer stream.
     *
     * @param samples 16kHz mono float audio data
     * @param sampleRate Audio sample rate (must be 16000)
     */
    fun acceptWaveform(samples: FloatArray, sampleRate: Int = 16000) {
        synchronized(lock) {
            val s = stream ?: return
            try {
                s.acceptWaveform(samples, sampleRate)
            } catch (e: Exception) {
                Log.e(TAG, "acceptWaveform failed", e)
            }
        }
    }

    /**
     * Run decoding on accumulated audio. Call after acceptWaveform.
     * May need to be called multiple times until isReady returns false.
     */
    fun decode() {
        synchronized(lock) {
            val r = recognizer ?: return
            val s = stream ?: return
            try {
                while (r.isReady(s)) {
                    r.decode(s)
                }
            } catch (e: Exception) {
                Log.e(TAG, "decode failed", e)
            }
        }
    }

    /**
     * Get the current recognition result.
     *
     * @return Transcription result with text, confidence, and alternatives
     */
    fun getResult(): AvxTranscriptionResult {
        return synchronized(lock) {
            val r = recognizer ?: return AvxTranscriptionResult("", 0f)
            val s = stream ?: return AvxTranscriptionResult("", 0f)
            try {
                val result = r.getResult(s)
                val text = result.text.trim()

                // Sherpa-ONNX doesn't provide a direct confidence score.
                // Estimate from token count and text length as a heuristic.
                // The real confidence filtering happens in ConfidenceScorer.
                val confidence = if (text.isNotEmpty()) {
                    // Non-empty result gets base confidence of 0.7,
                    // boosted by token count (more tokens = more confident the decoder was)
                    val tokenBonus = (result.tokens.size.coerceAtMost(10) * 0.03f)
                    (0.7f + tokenBonus).coerceAtMost(1.0f)
                } else {
                    0f
                }

                AvxTranscriptionResult(
                    text = text,
                    confidence = confidence,
                    alternatives = emptyList(), // OnlineRecognizer returns single best
                    tokens = result.tokens.toList(),
                    timestamps = result.timestamps.toList()
                )
            } catch (e: Exception) {
                Log.e(TAG, "getResult failed", e)
                AvxTranscriptionResult("", 0f)
            }
        }
    }

    /**
     * Check if the recognizer has detected an endpoint (end of utterance).
     */
    fun isEndpoint(): Boolean {
        return synchronized(lock) {
            val r = recognizer ?: return false
            val s = stream ?: return false
            try {
                r.isEndpoint(s)
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Reset the stream for a new utterance.
     * Call after processing an endpoint to start fresh.
     */
    fun reset() {
        synchronized(lock) {
            val r = recognizer ?: return
            val s = stream ?: return
            try {
                r.reset(s)
            } catch (e: Exception) {
                Log.e(TAG, "reset failed", e)
            }
        }
    }

    /**
     * Update hot words by rewriting the hot words file and recreating the stream.
     *
     * Sherpa-ONNX supports per-stream hot words via createStream(hotwords).
     * We recreate the stream with the new hot words string.
     */
    fun updateHotWords(hotWords: List<HotWord>) {
        synchronized(lock) {
            val r = recognizer ?: return
            try {
                // Build hot words string: "phrase1 :boost1\nphrase2 :boost2\n..."
                val hotWordsStr = hotWords.joinToString("\n") { "${it.phrase} :${it.boost}" }

                // Recreate stream with new hot words
                stream?.let { /* old stream will be GC'd */ }
                stream = r.createStream(hotWordsStr)

                Log.d(TAG, "Hot words updated: ${hotWords.size} phrases")
            } catch (e: Exception) {
                Log.e(TAG, "updateHotWords failed", e)
            }
        }
    }

    /**
     * Check if the recognizer is initialized and ready.
     */
    fun isReady(): Boolean = recognizer != null && stream != null

    /**
     * Release all resources.
     */
    fun release() {
        synchronized(lock) {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        try {
            stream = null
            recognizer?.release()
            recognizer = null
            hotWordsFile?.delete()
            hotWordsFile = null
            Log.d(TAG, "OnlineRecognizer released")
        } catch (e: Exception) {
            Log.e(TAG, "release failed", e)
        }
    }

    /**
     * Write hot words to a temporary text file for Sherpa-ONNX config.
     * Format: one entry per line, "phrase :boost"
     * Example: "scroll down :10.0"
     *
     * @return File object, or null if no hot words
     */
    private fun writeHotWordsFile(hotWords: List<HotWord>): File? {
        if (hotWords.isEmpty()) return null

        val cacheDir = File(context.cacheDir, "avx_hw")
        cacheDir.mkdirs()
        val hwFile = File(cacheDir, "hotwords.txt")

        val content = hotWords.joinToString("\n") { "${it.phrase} :${it.boost}" }
        hwFile.writeText(content)

        Log.d(TAG, "Hot words file written: ${hwFile.absolutePath} (${hotWords.size} entries)")
        return hwFile
    }
}
