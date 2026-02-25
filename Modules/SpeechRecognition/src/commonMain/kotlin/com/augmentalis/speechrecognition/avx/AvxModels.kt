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
 * Model storage: AON-encrypted archives containing ONNX transducer files
 * (encoder + decoder + joiner + tokens.txt)
 * Naming: Ava-AvxS-{LangCode}.aon
 *
 * CRITICAL: Hot words boosting requires transducer models with
 * modified_beam_search decoding. CTC/paraformer models do NOT support hot words.
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
 * Model capability tier indicating hot words support level.
 *
 * FULL: Dedicated transducer model with hot words — best command recognition
 * BILINGUAL: Shared transducer model covering 2 languages — good command recognition
 * PLANNED: No transducer model yet — falls back to Whisper + 6-stage matching
 */
enum class AvxModelTier {
    /** Dedicated per-language transducer model with full hot words support */
    FULL,
    /** Bilingual transducer model (shared with another language) */
    BILINGUAL,
    /** No transducer model available yet — not usable for AVX, use Whisper */
    PLANNED
}

/**
 * Transducer model file set: 3 ONNX files + vocabulary.
 *
 * Sherpa-ONNX streaming transducer models consist of:
 * - encoder: main acoustic model (~40-120MB int8)
 * - decoder: prediction network (~0.5MB int8)
 * - joiner: joint network (~0.3MB int8)
 * - tokens: BPE/char vocabulary (~5KB)
 *
 * All 4 files are archived into a single AON-encrypted file for storage.
 */
data class AvxModelFiles(
    val encoderFilename: String,
    val decoderFilename: String,
    val joinerFilename: String,
    val tokensFilename: String = "tokens.txt"
)

/**
 * Supported AVX languages with model metadata.
 *
 * Each language has a tuned ONNX transducer model optimized for that language's
 * phonetics. This per-language tuning is what gives AVX better command recognition
 * than generic multilingual models like Whisper.
 *
 * Languages are organized by tier:
 * - FULL: EN, ZH, KO, FR — have dedicated streaming transducer models
 * - BILINGUAL: ZH_EN — shared bilingual model
 * - PLANNED: DE, ES, IT, PT, NL, RU, JA, AR, HI, TR, PL — awaiting transducer models
 */
enum class AvxLanguage(
    val displayName: String,
    /** BCP-47 language code */
    val langCode: String,
    /** On-device encrypted filename: Ava-AvxS-{CODE}.aon */
    val aonFileName: String,
    /** Approximate total model size in MB (encoder + decoder + joiner + tokens, int8) */
    val approxSizeMB: Int,
    /** Hot words / command recognition capability tier */
    val tier: AvxModelTier,
    /** HuggingFace model repository ID for download */
    val modelRepoId: String,
    /** File names within the model repository */
    val modelFiles: AvxModelFiles,
    /** Whether this is a priority language (downloaded first) */
    val isPriority: Boolean = false
) {
    // --- Tier FULL: Dedicated streaming transducer models with hot words ---

    ENGLISH(
        "English", "en", "Ava-AvxS-EN.aon", 44,
        AvxModelTier.FULL,
        "csukuangfj/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
        AvxModelFiles(
            encoderFilename = "encoder-epoch-99-avg-1.int8.onnx",
            decoderFilename = "decoder-epoch-99-avg-1.int8.onnx",
            joinerFilename = "joiner-epoch-99-avg-1.int8.onnx"
        ),
        isPriority = true
    ),

    CHINESE(
        "Chinese", "zh", "Ava-AvxS-ZH.aon", 67,
        AvxModelTier.FULL,
        "csukuangfj/sherpa-onnx-streaming-zipformer-multi-zh-hans-2023-12-12",
        AvxModelFiles(
            encoderFilename = "encoder-epoch-20-avg-1-chunk-16-left-128.int8.onnx",
            decoderFilename = "decoder-epoch-20-avg-1-chunk-16-left-128.int8.onnx",
            joinerFilename = "joiner-epoch-20-avg-1-chunk-16-left-128.int8.onnx"
        ),
        isPriority = true
    ),

    KOREAN(
        "Korean", "ko", "Ava-AvxS-KO.aon", 121,
        AvxModelTier.FULL,
        "k2-fsa/sherpa-onnx-streaming-zipformer-korean-2024-06-16",
        AvxModelFiles(
            encoderFilename = "encoder-epoch-99-avg-1.int8.onnx",
            decoderFilename = "decoder-epoch-99-avg-1.int8.onnx",
            joinerFilename = "joiner-epoch-99-avg-1.int8.onnx"
        ),
        isPriority = true
    ),

    FRENCH(
        "French", "fr", "Ava-AvxS-FR.aon", 70,
        AvxModelTier.FULL,
        "shaojieli/sherpa-onnx-streaming-zipformer-fr-2023-04-14",
        AvxModelFiles(
            encoderFilename = "encoder-epoch-29-avg-9-with-averaged-model.int8.onnx",
            decoderFilename = "decoder-epoch-29-avg-9-with-averaged-model.int8.onnx",
            joinerFilename = "joiner-epoch-29-avg-9-with-averaged-model.int8.onnx"
        ),
        isPriority = true
    ),

    // --- Tier BILINGUAL: Shared transducer model ---

    CHINESE_ENGLISH(
        "Chinese + English", "zh-en", "Ava-AvxS-ZHEN.aon", 315,
        AvxModelTier.BILINGUAL,
        "csukuangfj/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20",
        AvxModelFiles(
            encoderFilename = "encoder-epoch-99-avg-1.int8.onnx",
            decoderFilename = "decoder-epoch-99-avg-1.int8.onnx",
            joinerFilename = "joiner-epoch-99-avg-1.int8.onnx"
        ),
        isPriority = false
    ),

    // --- Tier PLANNED: No transducer model yet, uses Whisper fallback ---

    GERMAN("German", "de", "Ava-AvxS-DE.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    SPANISH("Spanish", "es", "Ava-AvxS-ES.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    ITALIAN("Italian", "it", "Ava-AvxS-IT.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    PORTUGUESE("Portuguese", "pt", "Ava-AvxS-PT.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    DUTCH("Dutch", "nl", "Ava-AvxS-NL.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    RUSSIAN("Russian", "ru", "Ava-AvxS-RU.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    JAPANESE("Japanese", "ja", "Ava-AvxS-JA.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    ARABIC("Arabic", "ar", "Ava-AvxS-AR.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    HINDI("Hindi", "hi", "Ava-AvxS-HI.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    TURKISH("Turkish", "tr", "Ava-AvxS-TR.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", "")),
    POLISH("Polish", "pl", "Ava-AvxS-PL.aon", 0, AvxModelTier.PLANNED,
        "", AvxModelFiles("", "", ""));

    /** Whether this language has a working transducer model (FULL or BILINGUAL) */
    val hasTransducerModel: Boolean get() = tier != AvxModelTier.PLANNED

    /** Whether hot words boosting is available for this language */
    val supportsHotWords: Boolean get() = hasTransducerModel

    companion object {
        /** Find language by BCP-47 code (case-insensitive, prefix match) */
        fun forCode(langCode: String): AvxLanguage? {
            val normalized = langCode.lowercase().take(2)
            return entries.find { it.langCode == normalized }
        }

        /** Get all priority languages */
        fun priorityLanguages(): List<AvxLanguage> = entries.filter { it.isPriority }

        /** Get languages with working transducer models */
        fun availableLanguages(): List<AvxLanguage> = entries.filter { it.hasTransducerModel }

        /** Get languages awaiting transducer models */
        fun plannedLanguages(): List<AvxLanguage> = entries.filter { it.tier == AvxModelTier.PLANNED }

        /** Total download size for all priority languages in MB */
        fun totalPrioritySizeMB(): Int = priorityLanguages().sumOf { it.approxSizeMB }

        /** Total download size for all available languages in MB */
        fun totalAvailableSizeMB(): Int = availableLanguages().sumOf { it.approxSizeMB }
    }
}

/**
 * Hot word entry for decoder biasing.
 * AVX/Sherpa-ONNX supports hot words that get a score boost during decoding,
 * making the engine more likely to recognize known commands.
 *
 * Hot words are written to a temporary text file in the format:
 *   phrase :boost
 * and passed to OnlineRecognizerConfig.hotwordsFile
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
 * Transcription result from AVX/Sherpa-ONNX.
 * Shared across platforms (Android, Desktop).
 */
data class AvxTranscriptionResult(
    /** Best hypothesis text */
    val text: String,
    /** Confidence score [0.0-1.0] — estimated from decoder output */
    val confidence: Float,
    /** Alternative hypotheses (N-best minus the best) */
    val alternatives: List<String> = emptyList(),
    /** Individual token strings from the best hypothesis */
    val tokens: List<String> = emptyList(),
    /** Per-token timestamps in seconds */
    val timestamps: List<Float> = emptyList()
)

/**
 * Paths to the extracted (decrypted) transducer model files.
 * Used by both Android (cache dir) and Desktop (temp dir).
 */
data class AvxModelPaths(
    val encoderPath: String,
    val decoderPath: String,
    val joinerPath: String,
    val tokensPath: String
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
