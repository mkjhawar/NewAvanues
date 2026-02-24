/**
 * AvxConfig.kt - Configuration for the AVX (AvaVox) command engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Platform-agnostic configuration for the ONNX-based command recognition engine.
 * Maps to Sherpa-ONNX OnlineRecognizerConfig parameters internally.
 *
 * Key Sherpa-ONNX constraints:
 * - Hot words require decodingMethod = "modified_beam_search"
 * - Hot words require transducer model type (NOT paraformer or CTC)
 * - Sample rate must be 16000 Hz
 */
package com.augmentalis.speechrecognition.avx

/**
 * Configuration for the AVX engine.
 *
 * Unlike Whisper (which loads a single multilingual model), AVX loads a
 * language-specific transducer model and uses hot words to bias toward known commands.
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
     * Maps to maxActivePaths in modified_beam_search.
     * More candidates = better chance of command match, but slightly slower.
     */
    val nBestCount: Int = 4,

    /**
     * Decoding method for the transducer model.
     * MUST be "modified_beam_search" for hot words support.
     * "greedy_search" is faster but disables hot words entirely.
     */
    val decodingMethod: String = "modified_beam_search",

    /**
     * Maximum active paths for beam search.
     * Higher = more exploration = better accuracy, but slower.
     * Must match nBestCount or be >= nBestCount.
     */
    val maxActivePaths: Int = 4,

    /**
     * Enable endpoint detection (silence-based utterance segmentation).
     * When true, Sherpa-ONNX automatically detects end of speech.
     */
    val enableEndpoint: Boolean = true,

    /**
     * Blank penalty for transducer decoding.
     * Helps reduce insertions. 0.0 = no penalty, higher = fewer blanks.
     */
    val blankPenalty: Float = 0.0f,

    /** Custom model directory path. If set, overrides language-based path resolution. */
    val customModelPath: String? = null,

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
        if (decodingMethod != "modified_beam_search" && decodingMethod != "greedy_search") {
            return Result.failure(IllegalArgumentException(
                "decodingMethod must be 'modified_beam_search' or 'greedy_search'"
            ))
        }
        if (decodingMethod != "modified_beam_search" && hotWords.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(
                "Hot words require decodingMethod='modified_beam_search'"
            ))
        }
        if (!language.hasTransducerModel && language.tier != AvxModelTier.PLANNED) {
            return Result.failure(IllegalArgumentException(
                "Language '${language.displayName}' does not have a transducer model"
            ))
        }
        return Result.success(Unit)
    }

    companion object {
        /**
         * Create a config for command recognition with the given language.
         * Returns null if the language doesn't have a transducer model.
         */
        fun forLanguage(langCode: String): AvxConfig? {
            val language = AvxLanguage.forCode(langCode) ?: return null
            if (!language.hasTransducerModel) return null
            return AvxConfig(language = language)
        }

        /**
         * Create a config for command recognition, falling back to English
         * if the requested language doesn't have a transducer model.
         */
        fun forLanguageOrFallback(langCode: String): AvxConfig {
            val language = AvxLanguage.forCode(langCode)
            return if (language != null && language.hasTransducerModel) {
                AvxConfig(language = language)
            } else {
                AvxConfig(language = AvxLanguage.ENGLISH)
            }
        }
    }
}
