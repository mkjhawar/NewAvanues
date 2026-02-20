/**
 * WhisperNative.kt - Thread-safe JNI caller for whisper.cpp
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Provides thread-safe, null-guarded access to the raw JNI methods in
 * com.whispercpp.whisper.WhisperLib. whisper.cpp contexts are NOT thread-safe,
 * so all native calls are serialized through a single synchronized lock.
 *
 * Overhead: ~50ns per uncontended lock + ~5us JNI crossing. This is <0.001%
 * of whisper inference time (200-2000ms). The cost prevents SIGSEGV crashes
 * from concurrent native access.
 */
package com.augmentalis.speechrecognition.whisper

import android.content.res.AssetManager
import android.util.Log
import com.whispercpp.whisper.WhisperLib

/**
 * Thread-safe wrapper for whisper.cpp JNI calls.
 *
 * Why this exists:
 * 1. Thread safety — whisper.cpp contexts corrupt on concurrent access
 * 2. Null-pointer guards — calling native with ptr=0 causes SIGSEGV
 * 3. Library loading — idempotent System.loadLibrary management
 */
object WhisperNative {

    private const val TAG = "WhisperNative"

    @Volatile
    private var isLibraryLoaded = false

    /**
     * Load the native whisper-jni library. Safe to call multiple times.
     */
    fun ensureLoaded(): Boolean {
        if (isLibraryLoaded) return true
        return synchronized(this) {
            if (isLibraryLoaded) return@synchronized true
            try {
                System.loadLibrary("whisper-jni")
                isLibraryLoaded = true
                Log.i(TAG, "whisper-jni loaded")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load whisper-jni", e)
                false
            }
        }
    }

    fun initContext(modelPath: String): Long {
        if (!ensureLoaded()) return 0L
        return synchronized(this) {
            try {
                WhisperLib.initContext(modelPath)
            } catch (e: Exception) {
                Log.e(TAG, "initContext failed: $modelPath", e)
                0L
            }
        }
    }

    fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long {
        if (!ensureLoaded()) return 0L
        return synchronized(this) {
            try {
                WhisperLib.initContextFromAsset(assetManager, assetPath)
            } catch (e: Exception) {
                Log.e(TAG, "initContextFromAsset failed: $assetPath", e)
                0L
            }
        }
    }

    fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray) {
        if (contextPtr == 0L) return
        synchronized(this) {
            WhisperLib.fullTranscribe(contextPtr, numThreads, audioData)
        }
    }

    fun getTextSegmentCount(contextPtr: Long): Int {
        if (contextPtr == 0L) return 0
        return synchronized(this) { WhisperLib.getTextSegmentCount(contextPtr) }
    }

    fun getTextSegment(contextPtr: Long, index: Int): String {
        if (contextPtr == 0L) return ""
        return synchronized(this) { WhisperLib.getTextSegment(contextPtr, index) }
    }

    fun getSegmentStartTime(contextPtr: Long, index: Int): Long {
        if (contextPtr == 0L) return 0L
        return synchronized(this) { WhisperLib.getTextSegmentT0(contextPtr, index) }
    }

    fun getSegmentEndTime(contextPtr: Long, index: Int): Long {
        if (contextPtr == 0L) return 0L
        return synchronized(this) { WhisperLib.getTextSegmentT1(contextPtr, index) }
    }

    fun freeContext(contextPtr: Long) {
        if (contextPtr == 0L) return
        synchronized(this) {
            try {
                WhisperLib.freeContext(contextPtr)
            } catch (e: Exception) {
                Log.e(TAG, "freeContext failed", e)
            }
        }
    }

    fun getSystemInfo(): String {
        if (!ensureLoaded()) return "library not loaded"
        return synchronized(this) { WhisperLib.getSystemInfo() }
    }

    /**
     * Run transcription and collect all segments into a single result.
     * This is the primary entry point — one synchronized block for the
     * entire transcribe+read cycle to prevent interleaving.
     */
    fun transcribeToText(contextPtr: Long, numThreads: Int, audioData: FloatArray): TranscriptionResult {
        if (contextPtr == 0L) return TranscriptionResult("", emptyList(), 0L)

        val startNs = System.nanoTime()

        return synchronized(this) {
            WhisperLib.fullTranscribe(contextPtr, numThreads, audioData)

            val segCount = WhisperLib.getTextSegmentCount(contextPtr)
            val segments = ArrayList<TranscriptionSegment>(segCount)
            val text = StringBuilder()

            for (i in 0 until segCount) {
                val segText = WhisperLib.getTextSegment(contextPtr, i)
                val t0 = WhisperLib.getTextSegmentT0(contextPtr, i)
                val t1 = WhisperLib.getTextSegmentT1(contextPtr, i)
                segments.add(TranscriptionSegment(segText.trim(), t0 * 10, t1 * 10))
                text.append(segText)
            }

            TranscriptionResult(text.toString().trim(), segments, (System.nanoTime() - startNs) / 1_000_000)
        }
    }
}
