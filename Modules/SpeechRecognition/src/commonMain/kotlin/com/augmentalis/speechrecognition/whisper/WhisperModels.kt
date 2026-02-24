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
 * Model files follow ggml format: ggml-{size}.bin (download source name).
 * On-device storage uses clean .vlm filenames with no whisper/ggml traces.
 * English-only models are smaller and faster for English-only use cases.
 */
enum class WhisperModelSize(
    val displayName: String,
    /** Original HuggingFace filename (used ONLY for downloads, never stored) */
    val ggmlFileName: String,
    /** Clean on-device filename: VoiceOS-{Size}-{Lang}.vlm — no whisper/ggml traces */
    val vsmName: String,
    val approxSizeMB: Int,
    val minRAMMB: Int,
    val isEnglishOnly: Boolean,
    /** Relative speed vs TINY baseline (higher = slower but more accurate) */
    val relativeSpeed: Float = 1.0f
) {
    TINY("Tiny", "ggml-tiny.bin", "VoiceOS-Tin-MUL.vlm", 75, 256, false, 1.0f),
    TINY_EN("Tiny (English)", "ggml-tiny.en.bin", "VoiceOS-Tin-EN.vlm", 75, 256, true, 1.0f),
    BASE("Base", "ggml-base.bin", "VoiceOS-Bas-MUL.vlm", 142, 512, false, 2.0f),
    BASE_EN("Base (English)", "ggml-base.en.bin", "VoiceOS-Bas-EN.vlm", 142, 512, true, 2.0f),
    SMALL("Small", "ggml-small.bin", "VoiceOS-Sml-MUL.vlm", 466, 1024, false, 6.0f),
    SMALL_EN("Small (English)", "ggml-small.en.bin", "VoiceOS-Sml-EN.vlm", 466, 1024, true, 6.0f),
    MEDIUM("Medium", "ggml-medium.bin", "VoiceOS-Med-MUL.vlm", 1500, 2048, false, 20.0f),
    MEDIUM_EN("Medium (English)", "ggml-medium.en.bin", "VoiceOS-Med-EN.vlm", 1500, 2048, true, 20.0f),

    /**
     * Distil-Whisper Small (English) — 5-6x faster than standard Small with <1% WER degradation.
     * Same whisper.cpp JNI, just a different GGML model file. English-only.
     * Ideal for COMMAND mode where speed matters more than multilingual support.
     */
    DISTIL_SMALL_EN("Distil-Small (English)", "ggml-distil-small.en.bin", "VoiceOS-DSm-EN.vlm", 350, 512, true, 1.2f),

    /**
     * Distil-Whisper Medium (English) — ~3x faster than standard Medium with <1% WER degradation.
     * Best accuracy-to-speed ratio for English command recognition.
     */
    DISTIL_MEDIUM_EN("Distil-Medium (English)", "ggml-distil-medium.en.bin", "VoiceOS-DMd-EN.vlm", 700, 1024, true, 4.0f);

    /** Whether this is a Distil-Whisper model (faster, English-only, lower resource usage) */
    val isDistilled: Boolean get() = name.startsWith("DISTIL_")

    companion object {
        /**
         * Select best model for available RAM (platform-agnostic).
         * Conservative: picks model needing ~50% of available RAM or less.
         */
        fun forAvailableRAM(availableMB: Int, englishOnly: Boolean = false): WhisperModelSize {
            val candidates = entries
                .filter { it.isEnglishOnly == englishOnly }
                .filter { !it.isDistilled } // Standard models only — use forCommandMode for distilled
                .filter { it.minRAMMB <= availableMB / 2 }
                .sortedByDescending { it.approxSizeMB }
            return candidates.firstOrNull()
                ?: if (englishOnly) TINY_EN else TINY
        }

        /**
         * Select optimal model for command recognition.
         * Prefers Distil-Whisper models (faster, lower resource) when language is English.
         * Falls back to standard models for non-English or if RAM is insufficient.
         */
        fun forCommandMode(availableMB: Int, language: String): WhisperModelSize {
            val isEnglish = language.startsWith("en")
            if (isEnglish) {
                // Prefer Distil models — much faster for command recognition
                val distilCandidates = entries
                    .filter { it.isDistilled }
                    .filter { it.minRAMMB <= availableMB / 2 }
                    .sortedByDescending { it.approxSizeMB }
                distilCandidates.firstOrNull()?.let { return it }
            }
            // Fall back to standard selection
            return forAvailableRAM(availableMB, isEnglish)
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

/**
 * Builds an initial_prompt string from active commands for Whisper decoder biasing.
 *
 * Whisper's initial_prompt acts as a soft bias: the decoder has seen these words before
 * and is more likely to transcribe them correctly. This is NOT a grammar constraint —
 * Whisper can still transcribe words outside the prompt.
 *
 * The prompt is capped at [MAX_PROMPT_TOKENS] approximate tokens (~4 chars per token)
 * to stay within whisper.cpp's internal prompt buffer limit.
 */
object InitialPromptBuilder {
    /** Max approximate token count for the prompt (whisper.cpp limit is ~224 tokens) */
    private const val MAX_PROMPT_TOKENS = 200
    private const val AVG_CHARS_PER_TOKEN = 4

    /**
     * Build a prompt string from command lists.
     * Commands are deduplicated, sorted by length (shorter = more common), and
     * joined with commas. Truncated to fit within token budget.
     *
     * @param staticCommands Pre-defined system commands (e.g., "scroll down", "go back")
     * @param dynamicCommands UI-scraped commands from current screen context
     * @return Prompt string, or null if no commands available
     */
    fun build(staticCommands: List<String>, dynamicCommands: List<String> = emptyList()): String? {
        val allCommands = (staticCommands + dynamicCommands)
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.length } // Short commands first (more likely, more useful for biasing)

        if (allCommands.isEmpty()) return null

        val maxChars = MAX_PROMPT_TOKENS * AVG_CHARS_PER_TOKEN
        val builder = StringBuilder()

        for (command in allCommands) {
            val addition = if (builder.isEmpty()) command else ", $command"
            if (builder.length + addition.length > maxChars) break
            builder.append(addition)
        }

        return builder.toString().ifBlank { null }
    }
}
