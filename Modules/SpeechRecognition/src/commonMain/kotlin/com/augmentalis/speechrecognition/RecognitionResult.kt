/**
 * RecognitionResult.kt - Speech recognition result data model
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP commonMain
 */
package com.augmentalis.speechrecognition

/**
 * Word-level timestamp information (primarily for Whisper)
 */
data class WordTimestamp(
    val word: String,
    val startTime: Float,
    val endTime: Float,
    val confidence: Float
)

/**
 * Represents a speech recognition result from any engine.
 * Enhanced to support advanced features like language detection,
 * translation, and word-level timestamps.
 */
data class RecognitionResult(
    val text: String,
    val originalText: String = text,
    val confidence: Float,
    val timestamp: Long = currentTimeMillis(),
    val isPartial: Boolean = false,
    val isFinal: Boolean = true,
    val alternatives: List<String> = emptyList(),
    val engine: String = "",
    val mode: String = "",
    val metadata: Map<String, Any> = emptyMap(),

    // Advanced features (primarily for Whisper)
    val language: String? = null,           // Detected language
    val translation: String? = null,        // Translation if enabled
    val wordTimestamps: List<WordTimestamp>? = null  // Word-level timing
) {
    /**
     * Check if result meets confidence threshold
     */
    fun meetsThreshold(threshold: Float): Boolean {
        return confidence >= threshold
    }

    /**
     * Get best alternative or original text
     */
    fun getBestText(): String {
        return text.ifBlank {
            alternatives.firstOrNull() ?: originalText
        }
    }

    /**
     * Check if result is empty
     */
    fun isEmpty(): Boolean {
        return text.isBlank()
    }

    /**
     * Get the best alternative (first one) or null
     */
    fun getBestAlternative(): String? {
        return alternatives.firstOrNull()
    }

    /**
     * Check if this result has advanced features (language, translation, timestamps)
     */
    fun hasAdvancedFeatures(): Boolean {
        return language != null || translation != null || !wordTimestamps.isNullOrEmpty()
    }

    /**
     * Get the total duration from word timestamps
     */
    fun getTotalDuration(): Float {
        return wordTimestamps?.let { timestamps ->
            if (timestamps.isEmpty()) 0f
            else timestamps.last().endTime - timestamps.first().startTime
        } ?: 0f
    }

    /**
     * Get words that meet a specific confidence threshold
     */
    fun getHighConfidenceWords(threshold: Float): List<WordTimestamp> {
        return wordTimestamps?.filter { it.confidence >= threshold } ?: emptyList()
    }

    /**
     * Check if translation is available
     */
    fun hasTranslation(): Boolean = !translation.isNullOrBlank()

    /**
     * Check if language detection was performed
     */
    fun hasDetectedLanguage(): Boolean = !language.isNullOrBlank()

    /**
     * Convert to log-friendly string
     */
    override fun toString(): String {
        val advanced = mutableListOf<String>()
        if (language != null) advanced.add("lang=$language")
        if (translation != null) advanced.add("translation=${translation.take(20)}...")
        if (!wordTimestamps.isNullOrEmpty()) advanced.add("words=${wordTimestamps.size}")

        val advancedStr = if (advanced.isNotEmpty()) ", ${advanced.joinToString()}" else ""

        return "RecognitionResult(" +
            "text='$text', " +
            "confidence=$confidence, " +
            "partial=$isPartial, " +
            "engine=$engine" +
            advancedStr +
            ")"
    }

    companion object {
        /**
         * Create an empty result
         */
        fun empty(engine: String = "unknown", mode: String = ""): RecognitionResult {
            return RecognitionResult(
                text = "",
                confidence = 0f,
                engine = engine,
                mode = mode
            )
        }

        /**
         * Create an error result
         */
        fun error(message: String, engine: String = "unknown", mode: String = ""): RecognitionResult {
            return RecognitionResult(
                text = "",
                confidence = 0f,
                engine = engine,
                mode = mode,
                metadata = mapOf("error" to message)
            )
        }
    }
}
