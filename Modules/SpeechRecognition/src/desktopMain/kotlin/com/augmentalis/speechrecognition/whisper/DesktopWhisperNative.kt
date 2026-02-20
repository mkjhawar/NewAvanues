/**
 * DesktopWhisperNative.kt - Thread-safe JNI wrapper for whisper.cpp on Desktop
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Desktop JVM equivalent of Android's WhisperNative. Uses the same JNI
 * external declarations (whisper.cpp is platform-agnostic C++) but loads
 * the native library from the system library path instead of Android jniLibs.
 *
 * Native library naming:
 * - macOS:   libwhisper-jni.dylib
 * - Linux:   libwhisper-jni.so
 * - Windows: whisper-jni.dll
 *
 * Place the compiled library in one of:
 * 1. java.library.path (System.getProperty("java.library.path"))
 * 2. App resource directory
 * 3. Working directory
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.whisper.TranscriptionResult
import com.augmentalis.speechrecognition.whisper.TranscriptionSegment

/**
 * Thread-safe wrapper for whisper.cpp JNI calls on Desktop JVM.
 *
 * Why this exists:
 * 1. Thread safety — whisper.cpp contexts corrupt on concurrent access
 * 2. Null-pointer guards — calling native with ptr=0 causes SIGSEGV
 * 3. Library loading — idempotent System.loadLibrary management
 */
object DesktopWhisperNative {

    private const val TAG = "DesktopWhisperNative"

    @Volatile
    private var isLibraryLoaded = false

    /**
     * Load the native whisper-jni library. Safe to call multiple times.
     * On Desktop, this requires the native library to be in java.library.path.
     */
    fun ensureLoaded(): Boolean {
        if (isLibraryLoaded) return true
        return synchronized(this) {
            if (isLibraryLoaded) return@synchronized true
            try {
                System.loadLibrary("whisper-jni")
                isLibraryLoaded = true
                logInfo(TAG, "whisper-jni loaded on desktop")
                true
            } catch (e: UnsatisfiedLinkError) {
                // Try loading from explicit path as fallback
                tryLoadFromPath()
            }
        }
    }

    /**
     * Attempt to load from well-known paths when java.library.path doesn't include it.
     */
    private fun tryLoadFromPath(): Boolean {
        val osName = System.getProperty("os.name", "").lowercase()
        val libName = when {
            osName.contains("mac") || osName.contains("darwin") -> "libwhisper-jni.dylib"
            osName.contains("win") -> "whisper-jni.dll"
            else -> "libwhisper-jni.so"
        }

        // Check common locations relative to the app
        val searchPaths = listOf(
            System.getProperty("user.dir"),
            System.getProperty("user.dir") + "/lib",
            System.getProperty("user.dir") + "/natives",
            System.getProperty("user.home") + "/.avanues/lib"
        )

        for (basePath in searchPaths) {
            val fullPath = "$basePath/$libName"
            val file = java.io.File(fullPath)
            if (file.exists()) {
                try {
                    System.load(file.absolutePath)
                    isLibraryLoaded = true
                    logInfo(TAG, "whisper-jni loaded from: $fullPath")
                    return true
                } catch (e: UnsatisfiedLinkError) {
                    logError(TAG, "Failed to load from $fullPath: ${e.message}")
                }
            }
        }

        logError(TAG, "whisper-jni native library not found. Searched: $searchPaths")
        return false
    }

    // --- JNI External Declarations ---
    // These match the native method registrations in whisper_jni.cpp
    // Package: com.augmentalis.speechrecognition.whisper.DesktopWhisperNative

    @JvmStatic
    private external fun nativeInitContext(modelPath: String): Long

    @JvmStatic
    private external fun nativeFreeContext(contextPtr: Long)

    @JvmStatic
    private external fun nativeFullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)

    @JvmStatic
    private external fun nativeGetTextSegmentCount(contextPtr: Long): Int

    @JvmStatic
    private external fun nativeGetTextSegment(contextPtr: Long, index: Int): String

    @JvmStatic
    private external fun nativeGetTextSegmentT0(contextPtr: Long, index: Int): Long

    @JvmStatic
    private external fun nativeGetTextSegmentT1(contextPtr: Long, index: Int): Long

    @JvmStatic
    private external fun nativeGetSystemInfo(): String

    // --- Public Thread-Safe API ---

    fun initContext(modelPath: String): Long {
        if (!ensureLoaded()) return 0L
        return synchronized(this) {
            try {
                nativeInitContext(modelPath)
            } catch (e: Exception) {
                logError(TAG, "initContext failed: $modelPath", e)
                0L
            }
        }
    }

    fun freeContext(contextPtr: Long) {
        if (contextPtr == 0L) return
        synchronized(this) {
            try {
                nativeFreeContext(contextPtr)
            } catch (e: Exception) {
                logError(TAG, "freeContext failed", e)
            }
        }
    }

    fun getSystemInfo(): String {
        if (!ensureLoaded()) return "library not loaded"
        return synchronized(this) { nativeGetSystemInfo() }
    }

    /**
     * Run transcription and collect all segments into a single result.
     * One synchronized block for the entire transcribe+read cycle.
     */
    fun transcribeToText(contextPtr: Long, numThreads: Int, audioData: FloatArray): TranscriptionResult {
        if (contextPtr == 0L) return TranscriptionResult("", emptyList(), 0L)

        val startNs = System.nanoTime()

        return synchronized(this) {
            nativeFullTranscribe(contextPtr, numThreads, audioData)

            val segCount = nativeGetTextSegmentCount(contextPtr)
            val segments = ArrayList<TranscriptionSegment>(segCount)
            val text = StringBuilder()

            for (i in 0 until segCount) {
                val segText = nativeGetTextSegment(contextPtr, i)
                val t0 = nativeGetTextSegmentT0(contextPtr, i)
                val t1 = nativeGetTextSegmentT1(contextPtr, i)
                segments.add(TranscriptionSegment(segText.trim(), t0 * 10, t1 * 10))
                text.append(segText)
            }

            TranscriptionResult(text.toString().trim(), segments, (System.nanoTime() - startNs) / 1_000_000)
        }
    }
}
