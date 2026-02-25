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
import java.lang.management.ManagementFactory

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

    /** VAD sensitivity: higher = more sensitive to speech onset (0.0-1.0) */
    val vadSensitivity: Float = 0.6f,

    /** Silence duration (ms) before a speech chunk is finalized */
    val silenceThresholdMs: Long = 700,

    /** Minimum speech duration (ms) to consider as valid utterance */
    val minSpeechDurationMs: Long = 300,

    /** Maximum audio chunk duration (ms) before forced transcription */
    val maxChunkDurationMs: Long = 30_000,

    /** Optional VAD profile preset. When set, overrides individual VAD parameters
     *  (vadSensitivity, silenceThresholdMs, minSpeechDurationMs) with profile values. */
    val vadProfile: VADProfile? = null,

    /**
     * Initial prompt for decoder biasing. When set, Whisper's decoder is primed with
     * these tokens, making it more likely to transcribe words from the prompt.
     *
     * Use [InitialPromptBuilder.build] to construct from active commands.
     * Set to null to disable biasing (default for DICTATION mode).
     */
    val initialPrompt: String? = null
) {
    /** Effective VAD sensitivity: profile value if set, otherwise explicit config value */
    val effectiveVadSensitivity: Float get() = vadProfile?.vadSensitivity ?: vadSensitivity

    /** Effective silence threshold: profile value if set, otherwise explicit config value */
    val effectiveSilenceThresholdMs: Long get() = vadProfile?.silenceTimeoutMs ?: silenceThresholdMs

    /** Effective minimum speech duration: profile value if set, otherwise explicit config value */
    val effectiveMinSpeechDurationMs: Long get() = vadProfile?.minSpeechDurationMs ?: minSpeechDurationMs

    /** Effective hangover frames: profile value if set, otherwise default 5 */
    val effectiveHangoverFrames: Int get() = vadProfile?.hangoverFrames ?: 5

    /** Effective threshold alpha: profile value if set, otherwise default */
    val effectiveThresholdAlpha: Float get() = vadProfile?.thresholdAlpha ?: WhisperVAD.DEFAULT_THRESHOLD_ALPHA

    /** Effective min threshold: profile value if set, otherwise default */
    val effectiveMinThreshold: Float get() = vadProfile?.minThreshold ?: WhisperVAD.DEFAULT_MIN_THRESHOLD

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
         * Uses physical RAM (via OperatingSystemMXBean) for model selection,
         * falling back to JVM max heap if MXBean is unavailable.
         */
        fun autoTuned(language: String = "en"): DesktopWhisperConfig {
            val physicalMemMB = getPhysicalMemoryMB()
            val isEnglish = language.startsWith("en")
            val modelSize = WhisperModelSize.forAvailableRAM(physicalMemMB, isEnglish)
            return DesktopWhisperConfig(modelSize = modelSize, language = language)
        }

        /**
         * Create a config optimized for command recognition on Desktop.
         * Prefers Distil-Whisper models for English, includes initial_prompt biasing.
         */
        fun forCommandMode(
            language: String = "en",
            staticCommands: List<String> = emptyList(),
            dynamicCommands: List<String> = emptyList()
        ): DesktopWhisperConfig {
            val physicalMemMB = getPhysicalMemoryMB()
            val modelSize = WhisperModelSize.forCommandMode(physicalMemMB, language)
            val prompt = InitialPromptBuilder.build(staticCommands, dynamicCommands)

            return DesktopWhisperConfig(
                modelSize = modelSize,
                language = language,
                initialPrompt = prompt,
                silenceThresholdMs = 500,
                minSpeechDurationMs = 200,
                maxChunkDurationMs = 10_000
            )
        }

        private fun getPhysicalMemoryMB(): Int = try {
            val osBean = ManagementFactory.getOperatingSystemMXBean()
                as com.sun.management.OperatingSystemMXBean
            (osBean.totalMemorySize / (1024 * 1024)).toInt()
        } catch (_: Exception) {
            (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
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
                "English-only model but language='$language'. Use non-EN model or set language to 'en'."
            ))
        }
        return Result.success(Unit)
    }
}
