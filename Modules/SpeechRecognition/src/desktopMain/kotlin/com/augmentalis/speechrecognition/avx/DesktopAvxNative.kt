/**
 * DesktopAvxNative.kt - Thread-safe JNI wrapper for Sherpa-ONNX on Desktop JVM
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Desktop JVM equivalent of Android's AvxNative. Loads the Sherpa-ONNX native
 * library from java.library.path or well-known Desktop locations.
 *
 * Native library naming:
 * - macOS:   libsherpa-onnx-jni.dylib
 * - Linux:   libsherpa-onnx-jni.so
 * - Windows: sherpa-onnx-jni.dll
 */
package com.augmentalis.speechrecognition.avx

import com.augmentalis.speechrecognition.logError
import com.augmentalis.speechrecognition.logInfo

/**
 * Thread-safe JNI wrapper for Sherpa-ONNX on Desktop JVM.
 */
object DesktopAvxNative {

    private const val TAG = "DesktopAvxNative"

    @Volatile
    private var isLibraryLoaded = false

    fun ensureLoaded(): Boolean {
        if (isLibraryLoaded) return true
        return synchronized(this) {
            if (isLibraryLoaded) return@synchronized true
            try {
                System.loadLibrary("sherpa-onnx-jni")
                isLibraryLoaded = true
                logInfo(TAG, "sherpa-onnx-jni loaded on desktop")
                true
            } catch (e: UnsatisfiedLinkError) {
                tryLoadFromPath()
            }
        }
    }

    private fun tryLoadFromPath(): Boolean {
        val osName = System.getProperty("os.name", "").lowercase()
        val libName = when {
            osName.contains("mac") || osName.contains("darwin") -> "libsherpa-onnx-jni.dylib"
            osName.contains("win") -> "sherpa-onnx-jni.dll"
            else -> "libsherpa-onnx-jni.so"
        }

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
                    logInfo(TAG, "sherpa-onnx-jni loaded from: $fullPath")
                    return true
                } catch (e: UnsatisfiedLinkError) {
                    logError(TAG, "Failed to load from $fullPath: ${e.message}")
                }
            }
        }

        logError(TAG, "sherpa-onnx-jni native library not found. Searched: $searchPaths")
        return false
    }

    // --- JNI External Declarations ---

    @JvmStatic
    private external fun nativeCreateSession(modelPath: String, numThreads: Int, sampleRate: Int): Long

    @JvmStatic
    private external fun nativeFreeSession(sessionPtr: Long)

    @JvmStatic
    private external fun nativeSetHotWords(sessionPtr: Long, phrases: Array<String>, boosts: FloatArray)

    @JvmStatic
    private external fun nativeTranscribe(sessionPtr: Long, audioData: FloatArray, nBest: Int): String

    @JvmStatic
    private external fun nativeGetSystemInfo(): String

    // --- Public Thread-Safe API ---

    fun createSession(modelPath: String, numThreads: Int, sampleRate: Int): Long {
        if (!ensureLoaded()) return 0L
        return synchronized(this) {
            try {
                nativeCreateSession(modelPath, numThreads, sampleRate)
            } catch (e: Exception) {
                logError(TAG, "createSession failed: $modelPath", e)
                0L
            }
        }
    }

    fun freeSession(sessionPtr: Long) {
        if (sessionPtr == 0L) return
        synchronized(this) {
            try {
                nativeFreeSession(sessionPtr)
            } catch (e: Exception) {
                logError(TAG, "freeSession failed", e)
            }
        }
    }

    fun setHotWords(sessionPtr: Long, phrases: Array<String>, boosts: FloatArray) {
        if (sessionPtr == 0L) return
        synchronized(this) {
            try {
                nativeSetHotWords(sessionPtr, phrases, boosts)
            } catch (e: Exception) {
                logError(TAG, "setHotWords failed", e)
            }
        }
    }

    fun transcribe(sessionPtr: Long, audioData: FloatArray, nBest: Int = 5): AvxTranscriptionResult {
        if (sessionPtr == 0L) return AvxTranscriptionResult("", 0f)
        return synchronized(this) {
            try {
                val raw = nativeTranscribe(sessionPtr, audioData, nBest)
                parseResult(raw)
            } catch (e: Exception) {
                logError(TAG, "transcribe failed", e)
                AvxTranscriptionResult("", 0f)
            }
        }
    }

    private fun parseResult(raw: String): AvxTranscriptionResult {
        if (raw.isBlank()) return AvxTranscriptionResult("", 0f)
        val parts = raw.split("|")
        val text = parts.getOrElse(0) { "" }.trim()
        val confidence = parts.getOrElse(1) { "0" }.toFloatOrNull() ?: 0f
        val alternatives = if (parts.size > 2) {
            parts.subList(2, parts.size).map { it.trim() }.filter { it.isNotBlank() }
        } else emptyList()
        return AvxTranscriptionResult(text, confidence, alternatives)
    }
}
