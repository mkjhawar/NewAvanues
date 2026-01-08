/**
 * LLMPromptFormat.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.ai

/**
 * LLM Prompt Format
 *
 * Formats for generating prompts for language models
 */
enum class LLMPromptFormat {
    /**
     * Compact format - minimal context, essential info only
     */
    COMPACT,

    /**
     * HTML format - structured HTML for web display
     */
    HTML,

    /**
     * Full format - complete context with all details
     */
    FULL
}
