/**
 * LLMPromptFormat.kt - LLM prompt format options
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Defines different prompt formats for LLM context generation.
 * Each format balances token efficiency vs context richness.
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

/**
 * LLM Prompt Format
 *
 * Specifies the format for generating LLM-ready prompts from quantized context.
 * Choose based on your token budget and required detail level.
 */
enum class LLMPromptFormat {
    /**
     * Compact format (~50-100 tokens)
     *
     * Minimal context for quick queries.
     * Includes: app name, current screen, top actionable elements.
     * Use for: Simple navigation, basic commands.
     */
    COMPACT,

    /**
     * HTML-like format (~200 tokens)
     *
     * Research-backed format with semantic structure.
     * Includes: Screen hierarchy, element roles, navigation hints.
     * Use for: Complex interactions, multi-step tasks.
     */
    HTML,

    /**
     * Full format (~500+ tokens)
     *
     * Complete context with all available information.
     * Includes: Full element tree, all attributes, relationships.
     * Use for: Detailed analysis, comprehensive understanding.
     */
    FULL;

    /**
     * Get approximate token budget for this format
     */
    fun getApproxTokens(): IntRange = when (this) {
        COMPACT -> 50..100
        HTML -> 150..250
        FULL -> 400..600
    }

    /**
     * Get format description
     */
    fun getDescription(): String = when (this) {
        COMPACT -> "Minimal context for quick queries"
        HTML -> "Semantic structure with navigation hints"
        FULL -> "Complete context with all details"
    }
}
