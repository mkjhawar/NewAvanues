/**
 * AvxConfig.kt - Configuration for the AVX (AvaVox) command engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Platform-agnostic configuration for the ONNX-based command recognition engine.
 * Supports hot words boosting, beam size tuning, and language selection.
 */
package com.augmentalis.speechrecognition.avx

/**
 * Configuration for the AVX engine.
 *
 * Unlike Whisper (which loads a single multilingual model), AVX loads a
 * language-specific model and uses hot words to bias toward known commands.
 */
data class AvxConfig(
    /** Target language for recognition */
    val language: AvxLanguage = AvxLanguage.ENGLISH,

    /**
     * Number of CPU threads for ONNX Runtime inference.
     * 0 = auto (platform-specific default: Android 2, Desktop 4)
     */
    val numThreads: Int = 0,

    /**
     * Hot words for decoder biasing.
     * These are the active voice commands that AVX should prioritize.
     * Updated dynamically via updateCommands() as screen context changes.
     */
    val hotWords: List<HotWord> = emptyList(),

    /**
     * Default boost score for hot words added via updateCommands().
     * Higher = stronger bias. Range: 1.0-30.0, default 10.0
     */
    val defaultHotWordBoost: Float = 10.0f,

    /**
     * Maximum hot words to pass to the decoder.
     * Sherpa-ONNX practical limit is ~500 before performance degrades.
     */
    val maxHotWords: Int = 500,

    /**
     * Minimum confidence threshold for accepting a result.
     * Results below this are considered low-confidence and may trigger
     * fallback to Vivoka (when pre-filter is active).
     */
    val confidenceThreshold: Float = 0.85f,

    /**
     * Number of N-best hypotheses to request from the decoder.
     * More candidates = better chance of command match, but slightly slower.
     */
    val nBestCount: Int = 5,

    /** Custom model file path. If set, overrides language-based path resolution. */
    val customModelPath: String? = null,

    /** Enable streaming mode (partial results as speech progresses) */
    val enableStreaming: Boolean = false,

    /**
     * Sample rate for audio input.
     * Sherpa-ONNX models expect 16kHz mono audio.
     */
    val sampleRate: Int = 16000
) {
    /**
     * Resolve the effective thread count for the current platform.
     */
    fun effectiveThreadCount(isAndroid: Boolean = true): Int {
        if (numThreads > 0) return numThreads
        val cores = Runtime.getRuntime().availableProcessors()
        return if (isAndroid) {
            (cores / 2).coerceIn(2, 4) // Mobile: conservative
        } else {
            (cores / 2).coerceIn(2, 8) // Desktop: more headroom
        }
    }

    /**
     * Build hot words list from command strings.
     * Deduplicates, trims, and caps at maxHotWords.
     */
    fun withCommands(commands: List<String>): AvxConfig {
        val newHotWords = commands
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(maxHotWords)
            .map { HotWord(it, defaultHotWordBoost) }
        return copy(hotWords = newHotWords)
    }

    /**
     * Validate configuration parameters.
     */
    fun validate(): Result<Unit> {
        if (confidenceThreshold !in 0f..1f) {
            return Result.failure(IllegalArgumentException("confidenceThreshold must be 0.0-1.0"))
        }
        if (nBestCount < 1 || nBestCount > 20) {
            return Result.failure(IllegalArgumentException("nBestCount must be 1-20"))
        }
        if (defaultHotWordBoost < 0f || defaultHotWordBoost > 30f) {
            return Result.failure(IllegalArgumentException("defaultHotWordBoost must be 0.0-30.0"))
        }
        if (sampleRate != 16000) {
            return Result.failure(IllegalArgumentException("sampleRate must be 16000 (Sherpa-ONNX requirement)"))
        }
        return Result.success(Unit)
    }

    companion object {
        /**
         * Create a config for command recognition with the given language.
         */
        fun forLanguage(langCode: String): AvxConfig {
            val language = AvxLanguage.forCode(langCode) ?: AvxLanguage.ENGLISH
            return AvxConfig(language = language)
        }
    }
}
