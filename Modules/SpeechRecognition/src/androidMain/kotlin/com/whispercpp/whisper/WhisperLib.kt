/**
 * WhisperLib.kt - JNI external function declarations for whisper.cpp
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * CRITICAL: This class MUST be in package com.whispercpp.whisper because the native
 * jni.c registers methods under Java_com_whispercpp_whisper_WhisperLib_00024Companion_*.
 * Changing this package will break JNI linking.
 *
 * Do NOT call these methods directly — use WhisperNative for thread safety
 * and null-pointer protection. These are the raw JNI declarations only.
 */
package com.whispercpp.whisper

import android.content.res.AssetManager
import java.io.InputStream

/**
 * Raw JNI declarations matching the native method registrations in jni.c.
 * The Companion external functions map to JNI methods registered as:
 * Java_com_whispercpp_whisper_WhisperLib_00024Companion_<methodName>
 */
class WhisperLib {
    companion object {
        @JvmStatic
        external fun initContext(modelPath: String): Long

        @JvmStatic
        external fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long

        @JvmStatic
        external fun freeContext(contextPtr: Long)

        @JvmStatic
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)

        @JvmStatic
        external fun getTextSegmentCount(contextPtr: Long): Int

        @JvmStatic
        external fun getTextSegment(contextPtr: Long, index: Int): String

        @JvmStatic
        external fun getTextSegmentT0(contextPtr: Long, index: Int): Long

        @JvmStatic
        external fun getTextSegmentT1(contextPtr: Long, index: Int): Long

        @JvmStatic
        external fun getSystemInfo(): String

        /** Get token count for a segment (for confidence scoring). */
        @JvmStatic
        external fun getTextSegmentTokenCount(contextPtr: Long, segmentIndex: Int): Int

        /** Get token probability [0,1] for confidence scoring. */
        @JvmStatic
        external fun getTextSegmentTokenProb(contextPtr: Long, segmentIndex: Int, tokenIndex: Int): Float

        /** Get detected language code after transcription (e.g., "en", "es"). */
        @JvmStatic
        external fun getDetectedLanguage(contextPtr: Long): String

        @JvmStatic
        external fun benchMemcpy(numThreads: Int): String

        @JvmStatic
        external fun benchGgmlMulMat(numThreads: Int): String
    }
}

/**
 * JNI declarations for the demo package variant (initContextFromInputStream).
 * Maps to: Java_com_whispercppdemo_whisper_WhisperLib_00024Companion_*
 */
// This is handled by the com.whispercppdemo package in jni.c — not needed here
// as we use initContext(filePath) or initContextFromAsset instead.
