/**
 * AvxModels.kt - AVX (AvaVox) command engine model definitions
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Defines supported AVX languages, model metadata, and download URLs.
 * AVX uses Sherpa-ONNX under the hood but is branded externally as AvaVox.
 * No Sherpa references in UI, filenames, or public-facing code.
 *
 * Model storage: AON-encrypted ONNX files (AES-256-GCM + HMAC-SHA256)
 * Naming: Ava-AvxS-{LangCode}.aon
 */
package com.augmentalis.speechrecognition.avx

/**
 * Engine lifecycle states for AVX (mirrors WhisperEngineState pattern).
 */
enum class AvxEngineState {
    UNINITIALIZED,
    LOADING_MODEL,
    READY,
    LISTENING,
    PROCESSING,
    ERROR,
    DESTROYED
}

/**
 * Supported AVX languages with model metadata.
 *
 * Each language has a tuned ONNX model (~60-75MB) optimized for that language's
 * phonetics. This per-language tuning is what gives AVX better command recognition
 * than generic multilingual models like Whisper.
 */
enum class AvxLanguage(
    val displayName: String,
    /** BCP-47 language code */
    val langCode: String,
    /** On-device encrypted filename: Ava-AvxS-{CODE}.aon */
    val aonFileName: String,
    /** Approximate model size in MB */
    val approxSizeMB: Int,
    /** Whether this is a priority language (shipped with initial model set) */
    val isPriority: Boolean = false
) {
    ENGLISH("English", "en", "Ava-AvxS-EN.aon", 70, true),
    FRENCH("French", "fr", "Ava-AvxS-FR.aon", 65, true),
    GERMAN("German", "de", "Ava-AvxS-DE.aon", 65, true),
    SPANISH("Spanish", "es", "Ava-AvxS-ES.aon", 65, true),
    ITALIAN("Italian", "it", "Ava-AvxS-IT.aon", 65, true),
    PORTUGUESE("Portuguese", "pt", "Ava-AvxS-PT.aon", 65, true),
    DUTCH("Dutch", "nl", "Ava-AvxS-NL.aon", 60, true),
    RUSSIAN("Russian", "ru", "Ava-AvxS-RU.aon", 70, true),
    CHINESE("Chinese", "zh", "Ava-AvxS-ZH.aon", 75, true),
    JAPANESE("Japanese", "ja", "Ava-AvxS-JA.aon", 70, true),
    KOREAN("Korean", "ko", "Ava-AvxS-KO.aon", 65, true),
    ARABIC("Arabic", "ar", "Ava-AvxS-AR.aon", 65, true),
    HINDI("Hindi", "hi", "Ava-AvxS-HI.aon", 65, true),
    TURKISH("Turkish", "tr", "Ava-AvxS-TR.aon", 60, true),
    POLISH("Polish", "pl", "Ava-AvxS-PL.aon", 60, true);

    companion object {
        /** Find language by BCP-47 code (case-insensitive, prefix match) */
        fun forCode(langCode: String): AvxLanguage? {
            val normalized = langCode.lowercase().take(2)
            return entries.find { it.langCode == normalized }
        }

        /** Get all priority languages */
        fun priorityLanguages(): List<AvxLanguage> = entries.filter { it.isPriority }

        /** Total download size for all priority languages in MB */
        fun totalPrioritySizeMB(): Int = priorityLanguages().sumOf { it.approxSizeMB }
    }
}

/**
 * Hot word entry for decoder biasing.
 * AVX/Sherpa-ONNX supports hot words that get a score boost during decoding,
 * making the engine more likely to recognize known commands.
 */
data class HotWord(
    /** The word or phrase to boost (e.g., "scroll down", "go back") */
    val phrase: String,
    /**
     * Boost score multiplier. Higher = more bias toward this phrase.
     * Recommended range: 5.0-20.0 for voice commands.
     * Default: 10.0 (strong bias without overwhelming the acoustic model)
     */
    val boost: Float = 10.0f
)

/**
 * AVX model metadata stored in local inventory after download.
 */
data class AvxModelInfo(
    val language: AvxLanguage,
    /** Absolute path to the .aon file on device */
    val filePath: String,
    /** File size in bytes */
    val fileSizeBytes: Long,
    /** When the model was downloaded (epoch millis) */
    val downloadedAtMs: Long,
    /** Model version string (from CDN metadata) */
    val version: String = "1.0.0"
)
