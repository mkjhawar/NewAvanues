/**
 * SpeechMode.kt - Recognition modes enumeration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-08-28
 * Updated: 2026-01-18 - Migrated to KMP commonMain
 */
package com.augmentalis.speechrecognition

/**
 * Recognition modes that determine how speech is processed.
 */
enum class SpeechMode {
    /**
     * Static command mode - matches against predefined commands only
     * - Highest accuracy for known commands
     * - Limited vocabulary
     * - Fast processing
     */
    STATIC_COMMAND,

    /**
     * Dynamic command mode - matches against UI-scraped and contextual commands
     * - Adapts to current screen content
     * - Medium vocabulary size
     * - Good balance of accuracy and flexibility
     */
    DYNAMIC_COMMAND,

    /**
     * Dictation mode - continuous speech recognition
     * - Large vocabulary
     * - Sentence-level processing
     * - Punctuation support (engine dependent)
     */
    DICTATION,

    /**
     * Free speech mode - unrestricted speech recognition
     * - Largest vocabulary
     * - No command matching
     * - Lowest constraints
     */
    FREE_SPEECH,

    /**
     * Hybrid mode - engine-specific mode
     * - Switches between online/offline automatically
     * - Best quality based on connectivity
     */
    HYBRID;

    /**
     * Check if mode uses command matching
     */
    fun usesCommandMatching(): Boolean {
        return this in listOf(STATIC_COMMAND, DYNAMIC_COMMAND)
    }

    /**
     * Check if mode supports continuous recognition
     */
    fun supportsContinuous(): Boolean {
        return this in listOf(DICTATION, FREE_SPEECH)
    }

    /**
     * Get recommended confidence threshold for this mode
     */
    fun getRecommendedConfidenceThreshold(): Float {
        return when (this) {
            STATIC_COMMAND -> 0.8f    // High confidence for commands
            DYNAMIC_COMMAND -> 0.7f   // Medium confidence for dynamic
            DICTATION -> 0.6f         // Lower for continuous speech
            FREE_SPEECH -> 0.5f       // Lowest for unrestricted
            HYBRID -> 0.7f            // Medium confidence for hybrid
        }
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        return when (this) {
            STATIC_COMMAND -> "Predefined commands only"
            DYNAMIC_COMMAND -> "Screen-based commands"
            DICTATION -> "Continuous speech input"
            FREE_SPEECH -> "Unrestricted speech"
            HYBRID -> "Auto online/offline switching"
        }
    }
}
