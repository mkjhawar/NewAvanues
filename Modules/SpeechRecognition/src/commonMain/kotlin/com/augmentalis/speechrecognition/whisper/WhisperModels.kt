/**
 * WhisperModels.kt - Shared Whisper types for all platforms
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-20
 *
 * Platform-agnostic types used by Whisper engines on Android, iOS, and Desktop.
 * No platform dependencies — pure Kotlin models.
 */
package com.augmentalis.speechrecognition.whisper

/**
 * Engine states for the Whisper lifecycle.
 * Shared across all platform implementations.
 */
enum class WhisperEngineState {
    UNINITIALIZED,
    LOADING_MODEL,
    READY,
    LISTENING,
    PROCESSING,
    PAUSED,
    ERROR,
    DESTROYED
}

/**
 * Whisper model sizes with approximate resource requirements.
 *
 * Model files follow ggml format: ggml-{size}.bin
 * English-only models are smaller and faster for English-only use cases.
 */
enum class WhisperModelSize(
    val displayName: String,
    val ggmlFileName: String,
    val approxSizeMB: Int,
    val minRAMMB: Int,
    val isEnglishOnly: Boolean,
    /** Relative speed vs TINY baseline (higher = slower but more accurate) */
    val relativeSpeed: Float = 1.0f
) {
    TINY("Tiny", "ggml-tiny.bin", 75, 256, false, 1.0f),
    TINY_EN("Tiny (English)", "ggml-tiny.en.bin", 75, 256, true, 1.0f),
    BASE("Base", "ggml-base.bin", 142, 512, false, 2.0f),
    BASE_EN("Base (English)", "ggml-base.en.bin", 142, 512, true, 2.0f),
    SMALL("Small", "ggml-small.bin", 466, 1024, false, 6.0f),
    SMALL_EN("Small (English)", "ggml-small.en.bin", 466, 1024, true, 6.0f),
    MEDIUM("Medium", "ggml-medium.bin", 1500, 2048, false, 20.0f),
    MEDIUM_EN("Medium (English)", "ggml-medium.en.bin", 1500, 2048, true, 20.0f);

    companion object {
        /**
         * Select best model for available RAM (platform-agnostic).
         * Conservative: picks model needing ~50% of available RAM or less.
         */
        fun forAvailableRAM(availableMB: Int, englishOnly: Boolean = false): WhisperModelSize {
            val candidates = entries
                .filter { it.isEnglishOnly == englishOnly }
                .filter { it.minRAMMB <= availableMB / 2 }
                .sortedByDescending { it.approxSizeMB }
            return candidates.firstOrNull()
                ?: if (englishOnly) TINY_EN else TINY
        }
    }
}

/** Result of a transcription operation. */
data class TranscriptionResult(
    val text: String,
    val segments: List<TranscriptionSegment>,
    val processingTimeMs: Long,
    /** Average confidence across all segments [0.0-1.0]. 0 if not available. */
    val confidence: Float = 0f,
    /** Detected language code (e.g., "en", "es"). Null if not available. */
    val detectedLanguage: String? = null
)

/** A single timed segment from transcription. */
data class TranscriptionSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    /** Average token probability for this segment [0.0-1.0]. 0 if not available. */
    val confidence: Float = 0f
)

/**
 * VAD state machine states.
 */
enum class VADState {
    /** No speech detected, waiting for speech onset */
    SILENCE,
    /** Speech detected, accumulating audio */
    SPEECH,
    /** Speech ended, in hangover period (may resume) */
    HANGOVER
}

/**
 * Callback when a complete speech chunk is ready for transcription.
 */
fun interface OnSpeechChunkReady {
    fun onChunkReady(audioData: FloatArray, durationMs: Long)
}
