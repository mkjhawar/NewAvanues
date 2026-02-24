/**
 * AvxNative.kt - Thread-safe JNI wrapper for Sherpa-ONNX on Android
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Provides thread-safe access to the Sherpa-ONNX native library (sherpa-onnx-jni).
 * All native calls are serialized through a single synchronized lock to prevent
 * concurrent access to ONNX Runtime sessions.
 *
 * Native library: libsherpa-onnx-jni.so (bundled via sherpa-onnx AAR dependency)
 * JNI package: com.augmentalis.speechrecognition.avx.AvxNative
 */
package com.augmentalis.speechrecognition.avx

import android.util.Log

/**
 * Transcription result from AVX/Sherpa-ONNX.
 */
data class AvxTranscriptionResult(
    /** Best hypothesis text */
    val text: String,
    /** Confidence score [0.0-1.0] */
    val confidence: Float,
    /** Alternative hypotheses (N-best minus the best) */
    val alternatives: List<String> = emptyList()
)

/**
 * Thread-safe JNI wrapper for Sherpa-ONNX native calls.
 *
 * Why this exists (same rationale as WhisperNative):
 * 1. Thread safety — ONNX Runtime sessions are not thread-safe
 * 2. Null-pointer guards — calling native with ptr=0 causes SIGSEGV
 * 3. Library loading — idempotent System.loadLibrary management
 */
object AvxNative {

    private const val TAG = "AvxNative"

    @Volatile
    private var isLibraryLoaded = false

    /**
     * Load the Sherpa-ONNX native library. Safe to call multiple times.
     */
    fun ensureLoaded(): Boolean {
        if (isLibraryLoaded) return true
        return synchronized(this) {
            if (isLibraryLoaded) return@synchronized true
            try {
                System.loadLibrary("sherpa-onnx-jni")
                isLibraryLoaded = true
                Log.i(TAG, "sherpa-onnx-jni loaded")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load sherpa-onnx-jni", e)
                false
            }
        }
    }

    /**
     * Create an ONNX Runtime session from a model file.
     *
     * @param modelPath Path to the decrypted ONNX model file
     * @param numThreads Number of inference threads
     * @param sampleRate Audio sample rate (must be 16000)
     * @return Session pointer (0 if failed)
     */
    fun createSession(modelPath: String, numThreads: Int, sampleRate: Int): Long {
        if (!ensureLoaded()) return 0L
        return synchronized(this) {
            try {
                nativeCreateSession(modelPath, numThreads, sampleRate)
            } catch (e: Exception) {
                Log.e(TAG, "createSession failed: $modelPath", e)
                0L
            }
        }
    }

    /**
     * Free an ONNX Runtime session and release resources.
     */
    fun freeSession(sessionPtr: Long) {
        if (sessionPtr == 0L) return
        synchronized(this) {
            try {
                nativeFreeSession(sessionPtr)
            } catch (e: Exception) {
                Log.e(TAG, "freeSession failed", e)
            }
        }
    }

    /**
     * Set hot words for decoder biasing.
     *
     * @param sessionPtr Active session pointer
     * @param phrases Array of hot word phrases
     * @param boosts Array of boost scores (parallel with phrases)
     */
    fun setHotWords(sessionPtr: Long, phrases: Array<String>, boosts: FloatArray) {
        if (sessionPtr == 0L) return
        synchronized(this) {
            try {
                nativeSetHotWords(sessionPtr, phrases, boosts)
            } catch (e: Exception) {
                Log.e(TAG, "setHotWords failed", e)
            }
        }
    }

    /**
     * Run transcription on audio data.
     *
     * @param sessionPtr Active session pointer
     * @param audioData 16kHz mono float audio samples
     * @param nBest Number of N-best hypotheses to return
     * @return Transcription result with best text and alternatives
     */
    fun transcribe(sessionPtr: Long, audioData: FloatArray, nBest: Int = 5): AvxTranscriptionResult {
        if (sessionPtr == 0L) return AvxTranscriptionResult("", 0f)

        return synchronized(this) {
            try {
                val rawResult = nativeTranscribe(sessionPtr, audioData, nBest)
                parseTranscriptionResult(rawResult)
            } catch (e: Exception) {
                Log.e(TAG, "transcribe failed", e)
                AvxTranscriptionResult("", 0f)
            }
        }
    }

    /**
     * Get system info from ONNX Runtime.
     */
    fun getSystemInfo(): String {
        if (!ensureLoaded()) return "library not loaded"
        return synchronized(this) {
            try {
                nativeGetSystemInfo()
            } catch (e: Exception) {
                "error: ${e.message}"
            }
        }
    }

    // --- JNI External Declarations ---

    @JvmStatic
    private external fun nativeCreateSession(modelPath: String, numThreads: Int, sampleRate: Int): Long

    @JvmStatic
    private external fun nativeFreeSession(sessionPtr: Long)

    @JvmStatic
    private external fun nativeSetHotWords(sessionPtr: Long, phrases: Array<String>, boosts: FloatArray)

    /**
     * Returns a pipe-delimited string: "text|confidence|alt1|alt2|..."
     * Parsed by [parseTranscriptionResult].
     */
    @JvmStatic
    private external fun nativeTranscribe(sessionPtr: Long, audioData: FloatArray, nBest: Int): String

    @JvmStatic
    private external fun nativeGetSystemInfo(): String

    // --- Result Parsing ---

    /**
     * Parse the pipe-delimited native result into a structured object.
     * Format: "text|confidence|alt1|alt2|..."
     */
    private fun parseTranscriptionResult(raw: String): AvxTranscriptionResult {
        if (raw.isBlank()) return AvxTranscriptionResult("", 0f)

        val parts = raw.split("|")
        val text = parts.getOrElse(0) { "" }.trim()
        val confidence = parts.getOrElse(1) { "0" }.toFloatOrNull() ?: 0f
        val alternatives = if (parts.size > 2) {
            parts.subList(2, parts.size).map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        return AvxTranscriptionResult(text, confidence, alternatives)
    }
}
