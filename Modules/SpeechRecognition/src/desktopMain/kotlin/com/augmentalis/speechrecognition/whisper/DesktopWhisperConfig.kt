/**
 * DesktopWhisperConfig.kt - Desktop-specific Whisper configuration
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Desktop equivalent of Android's WhisperConfig. Uses file system paths
 * instead of Android Context for model resolution.
 *
 * Default model directory: ~/.avanues/whisper/models/
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logWarn
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import com.augmentalis.speechrecognition.whisper.vsm.vsmFileName
import java.io.File

/**
 * Configuration for the Desktop Whisper engine.
 */
data class DesktopWhisperConfig(
    /** Selected model size */
    val modelSize: WhisperModelSize = WhisperModelSize.BASE,

    /** Language code (BCP-47, e.g. "en", "es", "fr"). "auto" for auto-detect. */
    val language: String = "en",

    /** Enable translation to English */
    val translateToEnglish: Boolean = false,

    /** Number of CPU threads for inference. 0 = auto */
    val numThreads: Int = 0,

    /** Custom model file path. If set, overrides modelSize-based path resolution. */
    val customModelPath: String? = null,

    /** Silence duration (ms) before a speech chunk is finalized */
    val silenceThresholdMs: Long = 700,

    /** Minimum speech duration (ms) to consider as valid utterance */
    val minSpeechDurationMs: Long = 300,

    /** Maximum audio chunk duration (ms) before forced transcription */
    val maxChunkDurationMs: Long = 30_000
) {
    companion object {
        private const val TAG = "DesktopWhisperConfig"

        /** Default model directory under user home (legacy .bin storage) */
        private val DEFAULT_MODEL_DIR = File(
            System.getProperty("user.home", "."),
            ".avanues/whisper/models"
        )

        /** Shared VLM storage: ~/.augmentalis/models/vlm/ (matches AI/ALC pattern) */
        val SHARED_VSM_DIR: File = File(
            System.getProperty("user.home", "."),
            ".augmentalis/models/vlm"
        )

        /**
         * Auto-tuned config for the current desktop machine.
         * Uses Runtime.maxMemory() as a proxy for available resources.
         */
        fun autoTuned(language: String = "en"): DesktopWhisperConfig {
            val maxMemMB = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
            val isEnglish = language.startsWith("en")
            val modelSize = WhisperModelSize.forAvailableRAM(maxMemMB, isEnglish)
            return DesktopWhisperConfig(modelSize = modelSize, language = language)
        }
    }

    /**
     * Resolve the effective number of threads.
     */
    fun effectiveThreadCount(): Int {
        if (numThreads > 0) return numThreads
        val cores = Runtime.getRuntime().availableProcessors()
        // Desktop can use more cores than mobile — up to 8
        return (cores / 2).coerceIn(2, 8)
    }

    /**
     * Resolve the model file path, checking multiple locations.
     * Priority: customModelPath > shared VSM storage > default model dir > working directory
     */
    fun resolveModelPath(): String? {
        // 1. Custom path takes priority
        customModelPath?.let { path ->
            if (File(path).exists()) return path
            logWarn(TAG, "Custom model path not found: $path")
        }

        val fileName = modelSize.ggmlFileName

        // 2. Shared VLM storage: ~/.augmentalis/models/vlm/ (encrypted .vlm)
        val sharedVsm = File(SHARED_VSM_DIR, vsmFileName(fileName))
        if (sharedVsm.exists() && sharedVsm.length() > 0) return sharedVsm.absolutePath

        // 3. Default model directory: ~/.avanues/whisper/models/ (legacy .bin)
        val defaultModel = File(DEFAULT_MODEL_DIR, fileName)
        if (defaultModel.exists()) return defaultModel.absolutePath

        // 4. Working directory / models/
        val cwdModel = File("models/$fileName")
        if (cwdModel.exists()) return cwdModel.absolutePath

        // 5. Working directory direct
        val cwdDirect = File(fileName)
        if (cwdDirect.exists()) return cwdDirect.absolutePath

        logWarn(TAG, "Model not found: $fileName. Download to: ${SHARED_VSM_DIR.absolutePath}")
        return null
    }

    /**
     * Get the directory where models should be stored.
     */
    fun getModelDirectory(): File {
        if (!DEFAULT_MODEL_DIR.exists()) DEFAULT_MODEL_DIR.mkdirs()
        return DEFAULT_MODEL_DIR
    }

    /**
     * Validate this configuration.
     */
    fun validate(): Result<Unit> {
        if (silenceThresholdMs < 100 || silenceThresholdMs > 5000) {
            return Result.failure(IllegalArgumentException("silenceThresholdMs must be 100-5000"))
        }
        if (minSpeechDurationMs < 50 || minSpeechDurationMs > 5000) {
            return Result.failure(IllegalArgumentException("minSpeechDurationMs must be 50-5000"))
        }
        if (modelSize.isEnglishOnly && language != "en" && language != "auto") {
            return Result.failure(IllegalArgumentException(
                "English-only model but language='$language'. Use non-EN model or set language to 'en'."
            ))
        }
        return Result.success(Unit)
    }
}
