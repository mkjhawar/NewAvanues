/**
 * IosWhisperConfig.kt - iOS-specific Whisper engine configuration
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * iOS-specific configuration for the Whisper speech engine.
 * Selects model size based on device memory, configures threads,
 * and resolves model file paths in the iOS file system.
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.logInfo
import com.augmentalis.speechrecognition.whisper.vsm.VSMFormat
import com.augmentalis.speechrecognition.whisper.vsm.vsmFileName
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Configuration for the iOS Whisper engine.
 *
 * @param modelSize Which model to use (TINY_EN recommended for iOS)
 * @param language BCP-47 language code (e.g., "en", "es")
 * @param translateToEnglish Whether to use Whisper's translation feature
 * @param numThreads Number of inference threads (0 = auto)
 * @param vadSensitivity VAD sensitivity: higher = more sensitive to speech onset (0.0-1.0)
 * @param silenceThresholdMs Silence duration before VAD finalizes a chunk
 * @param minSpeechDurationMs Minimum utterance length to transcribe
 * @param maxChunkDurationMs Maximum chunk duration before forced transcription
 */
@OptIn(ExperimentalForeignApi::class)
data class IosWhisperConfig(
    val modelSize: WhisperModelSize = WhisperModelSize.TINY_EN,
    val language: String = "en",
    val translateToEnglish: Boolean = false,
    val numThreads: Int = 0,
    val vadSensitivity: Float = 0.6f,
    val silenceThresholdMs: Int = 700,
    val minSpeechDurationMs: Int = 300,
    val maxChunkDurationMs: Int = 30_000,

    /** Optional VAD profile preset. When set, overrides individual VAD parameters
     *  (vadSensitivity, silenceThresholdMs, minSpeechDurationMs) with profile values. */
    val vadProfile: VADProfile? = null
) {
    /** Effective VAD sensitivity: profile value if set, otherwise explicit config value */
    val effectiveVadSensitivity: Float get() = vadProfile?.vadSensitivity ?: vadSensitivity

    /** Effective silence threshold: profile value if set, otherwise explicit config value */
    val effectiveSilenceThresholdMs: Int get() = vadProfile?.silenceTimeoutMs?.toInt() ?: silenceThresholdMs

    /** Effective minimum speech duration: profile value if set, otherwise explicit config value */
    val effectiveMinSpeechDurationMs: Int get() = vadProfile?.minSpeechDurationMs?.toInt() ?: minSpeechDurationMs

    /** Effective hangover frames: profile value if set, otherwise default 5 */
    val effectiveHangoverFrames: Int get() = vadProfile?.hangoverFrames ?: 5

    /** Effective threshold alpha: profile value if set, otherwise default */
    val effectiveThresholdAlpha: Float get() = vadProfile?.thresholdAlpha ?: WhisperVAD.DEFAULT_THRESHOLD_ALPHA

    /** Effective min threshold: profile value if set, otherwise default */
    val effectiveMinThreshold: Float get() = vadProfile?.minThreshold ?: WhisperVAD.DEFAULT_MIN_THRESHOLD

    companion object {
        private const val TAG = "IosWhisperConfig"

        /** Models directory inside the app's Documents */
        private const val MODELS_SUBDIR = "whisper/models"

        /**
         * Create an auto-tuned configuration based on device capabilities.
         * Selects the best model that fits in available memory.
         */
        fun autoTuned(language: String = "en"): IosWhisperConfig {
            val physicalMemMB = (NSProcessInfo.processInfo.physicalMemory / (1024uL * 1024uL)).toInt()
            val isEnglish = language.startsWith("en")
            val modelSize = WhisperModelSize.forAvailableRAM(physicalMemMB, isEnglish)

            logInfo(TAG, "Auto-tuned: device=${physicalMemMB}MB, model=${modelSize.displayName}")

            return IosWhisperConfig(
                modelSize = modelSize,
                language = language,
                numThreads = effectiveThreadCount()
            )
        }

        /**
         * Calculate effective thread count for iOS.
         * Uses half of active processors, clamped to [2, 6].
         */
        fun effectiveThreadCount(): Int {
            val cores = NSProcessInfo.processInfo.activeProcessorCount.toInt()
            return (cores / 2).coerceIn(2, 6)
        }

        /**
         * Get the models directory path on iOS (inside Documents).
         * Creates the directory if it doesn't exist.
         */
        fun getModelsDirectory(): String {
            val fileManager = NSFileManager.defaultManager
            val documentsDir = getDocumentsDirectory()
            val modelsPath = "$documentsDir/$MODELS_SUBDIR"

            // Create directory if needed
            if (!fileManager.fileExistsAtPath(modelsPath)) {
                fileManager.createDirectoryAtPath(
                    modelsPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }

            return modelsPath
        }

        /**
         * Get the shared VLM storage directory: {Documents}/ava-ai-models/vlm/
         * Creates the directory if it doesn't exist.
         */
        fun getSharedVsmDirectory(): String {
            val fileManager = NSFileManager.defaultManager
            val documentsDir = getDocumentsDirectory()
            val vsmPath = "$documentsDir/${VSMFormat.SHARED_STORAGE_DIR}"

            if (!fileManager.fileExistsAtPath(vsmPath)) {
                fileManager.createDirectoryAtPath(
                    vsmPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }

            return vsmPath
        }

        /**
         * Get the iOS Documents directory path.
         */
        private fun getDocumentsDirectory(): String {
            val fileManager = NSFileManager.defaultManager
            return (fileManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            ).firstOrNull() as? NSURL)?.path ?: ""
        }
    }

    /**
     * Resolve the model file path on this iOS device.
     * Check order: 1) Shared VLM storage (.vlm) 2) Legacy storage (.bin)
     * @return Full path to the model file, or null if not downloaded
     */
    fun resolveModelPath(): String? {
        val fileManager = NSFileManager.defaultManager

        // 1. Shared VLM storage: {Documents}/ava-ai-models/vlm/ (encrypted .vlm)
        val sharedVsm = "${getSharedVsmDirectory()}/${vsmFileName(modelSize.ggmlFileName)}"
        if (fileManager.fileExistsAtPath(sharedVsm)) return sharedVsm

        // 2. Legacy storage: {Documents}/whisper/models/ (unencrypted .bin)
        val legacyBin = "${getModelsDirectory()}/${modelSize.ggmlFileName}"
        if (fileManager.fileExistsAtPath(legacyBin)) return legacyBin

        return null
    }

    /**
     * Get effective thread count for this configuration.
     */
    fun effectiveThreadCount(): Int {
        if (numThreads > 0) return numThreads
        return Companion.effectiveThreadCount()
    }

    /**
     * Validate the configuration.
     */
    fun validate(): Result<Unit> {
        if (vadSensitivity !in 0f..1f) {
            return Result.failure(IllegalArgumentException("vadSensitivity must be 0.0-1.0"))
        }
        if (language.isBlank()) {
            return Result.failure(IllegalArgumentException("Language cannot be blank"))
        }
        if (silenceThresholdMs < 100) {
            return Result.failure(IllegalArgumentException("Silence threshold too short: $silenceThresholdMs"))
        }
        if (maxChunkDurationMs < 1000) {
            return Result.failure(IllegalArgumentException("Max chunk duration too short: $maxChunkDurationMs"))
        }
        return Result.success(Unit)
    }
}
