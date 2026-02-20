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
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathDomainMask
import platform.Foundation.NSProcessInfo

/**
 * Configuration for the iOS Whisper engine.
 *
 * @param modelSize Which model to use (TINY_EN recommended for iOS)
 * @param language BCP-47 language code (e.g., "en", "es")
 * @param translateToEnglish Whether to use Whisper's translation feature
 * @param numThreads Number of inference threads (0 = auto)
 * @param silenceThresholdMs Silence duration before VAD finalizes a chunk
 * @param minSpeechDurationMs Minimum utterance length to transcribe
 * @param maxChunkDurationMs Maximum chunk duration before forced transcription
 */
data class IosWhisperConfig(
    val modelSize: WhisperModelSize = WhisperModelSize.TINY_EN,
    val language: String = "en",
    val translateToEnglish: Boolean = false,
    val numThreads: Int = 0,
    val silenceThresholdMs: Int = 700,
    val minSpeechDurationMs: Int = 300,
    val maxChunkDurationMs: Int = 30_000
) {
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
            val documentsDir = fileManager.URLsForDirectory(
                NSSearchPathDirectory.NSDocumentDirectory,
                NSSearchPathDomainMask.NSUserDomainMask
            ).firstOrNull()?.path ?: ""

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
    }

    /**
     * Resolve the model file path on this iOS device.
     * @return Full path to the model file, or null if not downloaded
     */
    fun resolveModelPath(): String? {
        val modelsDir = getModelsDirectory()
        val modelFile = "$modelsDir/${modelSize.ggmlFileName}"
        val fileManager = NSFileManager.defaultManager
        return if (fileManager.fileExistsAtPath(modelFile)) modelFile else null
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
