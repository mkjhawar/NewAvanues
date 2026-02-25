/**
 * IosWhisperNative.kt - Thread-safe wrapper around whisper_bridge cinterop bindings
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Provides the same API contract as WhisperNative (Android) and DesktopWhisperNative
 * but backed by Kotlin/Native cinterop with whisper_bridge.h instead of JNI.
 *
 * Thread safety: All calls are serialized via atomicfu SynchronizedObject
 * since whisper.cpp contexts are NOT thread-safe.
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import kotlin.concurrent.Volatile
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaque
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong
import kotlinx.cinterop.toKString
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * Thread-safe Kotlin/Native wrapper for whisper.cpp via cinterop bridge.
 *
 * Why this exists:
 * 1. Thread safety — whisper.cpp contexts corrupt on concurrent access
 * 2. Null-pointer guards — calling native with NULL causes crashes
 * 3. API parity — same interface as Android WhisperNative / Desktop DesktopWhisperNative
 */
@OptIn(ExperimentalForeignApi::class)
object IosWhisperNative {

    private const val TAG = "IosWhisperNative"
    /** Sentinel value indicating confidence is unavailable (native methods not linked) */
    const val CONFIDENCE_UNAVAILABLE = -1f

    private val lock = SynchronizedObject()

    @Volatile
    private var isLibraryAvailable = false

    /**
     * Check if the native whisper bridge is available.
     * On iOS, this verifies the cinterop bindings are linked.
     */
    fun ensureAvailable(): Boolean {
        if (isLibraryAvailable) return true
        return synchronized(lock) {
            if (isLibraryAvailable) return@synchronized true
            try {
                // Test that the cinterop bindings are linked by calling system_info
                val info = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_system_info()
                isLibraryAvailable = info != null
                if (isLibraryAvailable) {
                    logInfo(TAG, "whisper_bridge available on iOS")
                }
                isLibraryAvailable
            } catch (e: Exception) {
                logError(TAG, "whisper_bridge not available: ${e.message}")
                false
            }
        }
    }

    /**
     * Initialize a whisper context from a model file path.
     * @return Context pointer as Long (cast from void*), or 0 on failure
     */
    fun initContext(modelPath: String): Long {
        if (!ensureAvailable()) return 0L
        return synchronized(lock) {
            try {
                val ctx = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_init(modelPath)
                if (ctx == null) {
                    logError(TAG, "initContext failed: $modelPath")
                    0L
                } else {
                    // Store the opaque pointer as Long for cross-platform consistency
                    ctx.toLong()
                }
            } catch (e: Exception) {
                logError(TAG, "initContext exception: ${e.message}")
                0L
            }
        }
    }

    /**
     * Free a whisper context. NULL-safe.
     */
    fun freeContext(contextPtr: Long) {
        if (contextPtr == 0L) return
        synchronized(lock) {
            try {
                com.augmentalis.speechrecognition.native.whisper.whisper_bridge_free(
                    contextPtr.toNativePtr()
                )
            } catch (e: Exception) {
                logError(TAG, "freeContext failed: ${e.message}")
            }
        }
    }

    /**
     * Get system info string (CPU features, SIMD support).
     */
    fun getSystemInfo(): String {
        if (!ensureAvailable()) return "library not available"
        return synchronized(lock) {
            com.augmentalis.speechrecognition.native.whisper.whisper_bridge_system_info()
                ?.toKString() ?: "unknown"
        }
    }

    /**
     * Run transcription and collect all segments into a single result.
     * One synchronized block for the entire transcribe+read cycle.
     *
     * Includes per-segment confidence from token probabilities and
     * language detection when available.
     */
    fun transcribeToText(
        contextPtr: Long,
        numThreads: Int,
        audioData: FloatArray,
        language: String = "en",
        translate: Boolean = false
    ): TranscriptionResult {
        if (contextPtr == 0L) return TranscriptionResult("", emptyList(), 0L)

        val startMs = (NSDate().timeIntervalSince1970 * 1000).toLong()

        return synchronized(lock) {
            memScoped {
                val nativePtr = contextPtr.toNativePtr()

                // Pin the float array and pass to native
                val samplesPtr = allocArrayOf(*audioData)

                val result = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_transcribe(
                    ctx = nativePtr,
                    n_threads = numThreads,
                    samples = samplesPtr,
                    n_samples = audioData.size,
                    language = language,
                    translate = translate
                )

                if (result != 0) {
                    logError(TAG, "Transcription failed with code: $result")
                    return@memScoped TranscriptionResult("", emptyList(), 0L)
                }

                val segCount = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_segment_count(nativePtr)
                val segments = ArrayList<TranscriptionSegment>(segCount)
                val text = StringBuilder()
                var totalConfidence = 0f
                var hasRealConfidence = false

                for (i in 0 until segCount) {
                    val segText = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_segment_text(nativePtr, i)
                        ?.toKString()?.trim() ?: ""
                    val t0 = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_segment_t0(nativePtr, i)
                    val t1 = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_segment_t1(nativePtr, i)

                    val segConfidence = getSegmentConfidenceUnsafe(nativePtr, i)
                    val effectiveConfidence = if (segConfidence == CONFIDENCE_UNAVAILABLE) 0f else segConfidence
                    if (segConfidence != CONFIDENCE_UNAVAILABLE) hasRealConfidence = true

                    segments.add(TranscriptionSegment(segText, t0 * 10, t1 * 10, effectiveConfidence))
                    if (segText.isNotEmpty()) {
                        text.append(segText)
                        text.append(" ")
                    }
                    totalConfidence += effectiveConfidence
                }

                // Report 0 confidence when token probabilities aren't available,
                // letting the ConfidenceScorer classify this as REJECT/unknown
                val avgConfidence = if (!hasRealConfidence) 0f
                    else if (segCount > 0) totalConfidence / segCount
                    else 0f
                val detectedLang = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_detected_language(nativePtr)
                    ?.toKString()

                val endMs = (NSDate().timeIntervalSince1970 * 1000).toLong()

                TranscriptionResult(
                    text = text.toString().trim(),
                    segments = segments,
                    processingTimeMs = endMs - startMs,
                    confidence = avgConfidence,
                    detectedLanguage = detectedLang
                )
            }
        }
    }

    /**
     * Get average token probability for a segment as confidence [0,1].
     * Called from within synchronized block — no lock needed.
     */
    private fun getSegmentConfidenceUnsafe(contextPtr: COpaquePointer?, segmentIndex: Int): Float {
        return try {
            val tokenCount = com.augmentalis.speechrecognition.native.whisper.whisper_bridge_segment_token_count(contextPtr, segmentIndex)
            if (tokenCount <= 0) return CONFIDENCE_UNAVAILABLE
            var probSum = 0f
            for (t in 0 until tokenCount) {
                probSum += com.augmentalis.speechrecognition.native.whisper.whisper_bridge_segment_token_prob(contextPtr, segmentIndex, t)
            }
            probSum / tokenCount
        } catch (e: Exception) {
            CONFIDENCE_UNAVAILABLE
        }
    }

    /**
     * Convert Long back to opaque C pointer for cinterop calls.
     * Uses kotlinx.cinterop.toCPointer extension (Long → CPointer<COpaque>?).
     */
    private fun Long.toNativePtr(): COpaquePointer? {
        if (this == 0L) return null
        return this.toCPointer<COpaque>()
    }
}
