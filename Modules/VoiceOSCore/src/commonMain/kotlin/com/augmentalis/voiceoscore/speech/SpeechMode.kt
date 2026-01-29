/**
 * SpeechMode.kt - Recognition modes for speech processing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * KMP migration of VoiceOSCore SpeechMode enum.
 * Determines how speech is processed and matched.
 */
package com.augmentalis.voiceoscore

/**
 * Recognition modes that determine how speech is processed.
 *
 * The mode affects:
 * - Grammar constraints (restricted vs open vocabulary)
 * - Confidence thresholds
 * - Command matching behavior
 * - Recognition timing (single utterance vs continuous)
 */
enum class SpeechMode {
    /**
     * Static command mode - matches against predefined commands only
     * - Highest accuracy for known commands
     * - Limited vocabulary (navigation, media, system commands)
     * - Fast processing
     * - Best for: System-wide voice control
     */
    STATIC_COMMAND,

    /**
     * Dynamic command mode - matches against UI-scraped and contextual commands
     * - Adapts to current screen content
     * - Medium vocabulary size
     * - Good balance of accuracy and flexibility
     * - Best for: App-specific voice control
     */
    DYNAMIC_COMMAND,

    /**
     * Combined mode - matches against both static AND dynamic commands
     * - Full command coverage
     * - Larger vocabulary than single mode
     * - Slightly lower accuracy due to larger search space
     * - Best for: General voice control with context awareness
     */
    COMBINED_COMMAND,

    /**
     * Dictation mode - continuous speech recognition
     * - Large vocabulary
     * - Sentence-level processing
     * - Punctuation support (engine dependent)
     * - Best for: Text input fields
     */
    DICTATION,

    /**
     * Free speech mode - unrestricted speech recognition
     * - Largest vocabulary
     * - No command matching
     * - Lowest constraints
     * - Best for: Search queries, natural language input
     */
    FREE_SPEECH,

    /**
     * Hybrid mode - Vivoka-specific mode
     * - Switches between online/offline automatically
     * - Best quality based on connectivity
     * - Engine-specific behavior
     */
    HYBRID;

    /**
     * Check if mode uses command matching
     */
    fun usesCommandMatching(): Boolean =
        this in listOf(STATIC_COMMAND, DYNAMIC_COMMAND, COMBINED_COMMAND)

    /**
     * Check if mode supports continuous recognition
     */
    fun supportsContinuous(): Boolean =
        this in listOf(DICTATION, FREE_SPEECH)

    /**
     * Check if mode requires dynamic command loading
     */
    fun requiresDynamicCommands(): Boolean =
        this in listOf(DYNAMIC_COMMAND, COMBINED_COMMAND)

    /**
     * Check if mode requires static command loading
     */
    fun requiresStaticCommands(): Boolean =
        this in listOf(STATIC_COMMAND, COMBINED_COMMAND)

    /**
     * Get recommended confidence threshold for this mode
     *
     * Higher threshold = more strict matching (fewer false positives)
     * Lower threshold = more lenient matching (fewer false negatives)
     */
    fun getRecommendedConfidenceThreshold(): Float = when (this) {
        STATIC_COMMAND -> 0.8f     // High confidence for predefined commands
        DYNAMIC_COMMAND -> 0.7f    // Medium confidence for dynamic
        COMBINED_COMMAND -> 0.75f  // Balanced for combined
        DICTATION -> 0.6f          // Lower for continuous speech
        FREE_SPEECH -> 0.5f        // Lowest for unrestricted
        HYBRID -> 0.7f             // Medium confidence for hybrid
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String = when (this) {
        STATIC_COMMAND -> "Predefined commands only"
        DYNAMIC_COMMAND -> "Screen-based commands"
        COMBINED_COMMAND -> "Static + dynamic commands"
        DICTATION -> "Continuous speech input"
        FREE_SPEECH -> "Unrestricted speech"
        HYBRID -> "Auto online/offline switching"
    }

    /**
     * Get grammar type hint for engine configuration
     */
    fun getGrammarType(): GrammarType = when (this) {
        STATIC_COMMAND -> GrammarType.RESTRICTED
        DYNAMIC_COMMAND -> GrammarType.RESTRICTED
        COMBINED_COMMAND -> GrammarType.RESTRICTED
        DICTATION -> GrammarType.OPEN
        FREE_SPEECH -> GrammarType.OPEN
        HYBRID -> GrammarType.ADAPTIVE
    }

    companion object {
        /**
         * Default mode for voice control applications
         */
        val DEFAULT = COMBINED_COMMAND

        /**
         * Mode for text entry/dictation
         */
        val TEXT_ENTRY = DICTATION
    }
}

/**
 * Grammar constraint type for speech recognition
 */
enum class GrammarType {
    /**
     * Restricted grammar - only recognize specific phrases
     */
    RESTRICTED,

    /**
     * Open grammar - recognize any speech
     */
    OPEN,

    /**
     * Adaptive grammar - switches based on context
     */
    ADAPTIVE
}
