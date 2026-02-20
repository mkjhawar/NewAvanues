/**
 * WhisperConfig.kt - Configuration for Whisper speech recognition engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Manages model selection, path resolution, audio parameters, and performance tuning.
 */
package com.augmentalis.speechrecognition.whisper

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.io.File

/**
 * Complete configuration for the Whisper engine.
 */
data class WhisperConfig(
    /** Selected model size */
    val modelSize: WhisperModelSize = WhisperModelSize.BASE,

    /** Language code (BCP-47, e.g. "en", "es", "fr"). "auto" for auto-detect. */
    val language: String = "en",

    /** Enable translation to English (whisper built-in feature) */
    val translateToEnglish: Boolean = false,

    /** Number of CPU threads for inference. 0 = auto (uses available cores / 2) */
    val numThreads: Int = 0,

    /** Custom model file path. If set, overrides modelSize-based path resolution. */
    val customModelPath: String? = null,

    /** VAD sensitivity: higher = more sensitive to speech onset (0.0-1.0) */
    val vadSensitivity: Float = 0.6f,

    /** Silence duration (ms) before a speech chunk is finalized */
    val silenceThresholdMs: Long = 700,

    /** Minimum speech duration (ms) to consider as valid utterance */
    val minSpeechDurationMs: Long = 300,

    /** Maximum audio chunk duration (ms) before forced transcription */
    val maxChunkDurationMs: Long = 30_000,

    /** Whether to use 16-bit quantized models when available (faster, slightly less accurate) */
    val useQuantized: Boolean = false
) {
    /**
     * Resolve the effective number of threads.
     */
    fun effectiveThreadCount(): Int {
        if (numThreads > 0) return numThreads
        val cores = Runtime.getRuntime().availableProcessors()
        // Use half the cores, minimum 2, maximum 4 (diminishing returns beyond 4)
        return cores.coerceIn(2, 4)
    }

    /**
     * Resolve the model file path, checking multiple locations.
     * Priority: customModelPath > internal storage > external files > assets
     */
    fun resolveModelPath(context: Context): String? {
        // 1. Custom path takes priority
        customModelPath?.let { path ->
            if (File(path).exists()) return path
            Log.w(TAG, "Custom model path not found: $path")
        }

        val fileName = modelSize.ggmlFileName

        // 2. Internal storage: /data/data/<pkg>/files/whisper/models/
        val internalModel = File(context.filesDir, "whisper/models/$fileName")
        if (internalModel.exists()) return internalModel.absolutePath

        // 3. External files: /sdcard/Android/data/<pkg>/files/whisper/models/
        context.getExternalFilesDir(null)?.let { extDir ->
            val externalModel = File(extDir, "whisper/models/$fileName")
            if (externalModel.exists()) return externalModel.absolutePath
        }

        Log.w(TAG, "Model not found: $fileName. Download required.")
        return null
    }

    /**
     * Get the directory where models should be stored.
     */
    fun getModelDirectory(context: Context): File {
        val dir = File(context.filesDir, "whisper/models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Validate this configuration.
     */
    fun validate(): Result<Unit> {
        if (vadSensitivity !in 0f..1f) {
            return Result.failure(IllegalArgumentException("vadSensitivity must be 0.0-1.0"))
        }
        if (silenceThresholdMs < 100 || silenceThresholdMs > 5000) {
            return Result.failure(IllegalArgumentException("silenceThresholdMs must be 100-5000"))
        }
        if (minSpeechDurationMs < 50 || minSpeechDurationMs > 5000) {
            return Result.failure(IllegalArgumentException("minSpeechDurationMs must be 50-5000"))
        }
        if (modelSize.isEnglishOnly && language != "en" && language != "auto") {
            return Result.failure(IllegalArgumentException(
                "English-only model selected but language is '$language'. Use non-EN model or set language to 'en'."
            ))
        }
        return Result.success(Unit)
    }

    companion object {
        private const val TAG = "WhisperConfig"

        /**
         * Create a config auto-tuned for the current device.
         * Uses ActivityManager to determine available RAM and selects the best model.
         */
        fun autoTuned(context: Context, language: String = "en"): WhisperConfig {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                    as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val totalRamMB = (memInfo.totalMem / (1024 * 1024)).toInt()

            val isEnglish = language.startsWith("en")
            val modelSize = WhisperModelSize.forAvailableRAM(totalRamMB, isEnglish)

            return WhisperConfig(
                modelSize = modelSize,
                language = language
            )
        }
    }
}
