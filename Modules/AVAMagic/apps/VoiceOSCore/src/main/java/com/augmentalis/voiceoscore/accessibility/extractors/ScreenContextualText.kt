/**
 * ScreenContextualText.kt - NLU contextual text extraction models
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-30
 *
 * Data classes for non-actionable contextual text extraction from UI screens.
 * Used to provide richer context to LLMs by including screen titles, headers,
 * breadcrumbs, and descriptive labels that help understand screen purpose.
 *
 * Part of NLU Context Extraction feature for VoiceOS.
 */
package com.augmentalis.voiceoscore.accessibility.extractors

/**
 * Screen Contextual Text
 *
 * Contains non-actionable text extracted from a screen that provides
 * semantic context for LLM prompts. This includes titles, headers,
 * breadcrumbs, and descriptive labels.
 *
 * ## Token Budget:
 * - Screen Title: ~10 tokens
 * - Section Headers: ~50 tokens
 * - Breadcrumbs: ~20 tokens
 * - Visible Labels: ~100 tokens
 * - **Total: ~180 tokens per screen**
 *
 * ## Usage:
 * ```kotlin
 * val contextualText = uiScrapingEngine.extractContextualText(rootNode, packageName)
 * val promptSection = contextualText.toLLMPromptSection()
 * ```
 *
 * @property screenHash Unique identifier for the screen
 * @property screenTitle Main title of the screen (e.g., "Settings", "Profile")
 * @property sectionHeaders List of section/category headers (e.g., "Network", "Display")
 * @property breadcrumbs Navigation path to current screen (e.g., ["Settings", "Network", "Wi-Fi"])
 * @property visibleLabels Non-clickable descriptive text labels
 * @property timestamp When this extraction was performed
 */
data class ScreenContextualText(
    val screenHash: String,
    val screenTitle: String?,
    val sectionHeaders: List<String>,
    val breadcrumbs: List<String>,
    val visibleLabels: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Maximum tokens allocated for contextual text in prompts
         */
        const val MAX_TOKENS = 200

        /**
         * Maximum number of section headers to include
         */
        const val MAX_SECTION_HEADERS = 10

        /**
         * Maximum number of visible labels to include
         */
        const val MAX_VISIBLE_LABELS = 15

        /**
         * Maximum breadcrumb depth
         */
        const val MAX_BREADCRUMB_DEPTH = 5
    }

    /**
     * Check if contextual text has any content
     */
    fun hasContent(): Boolean =
        !screenTitle.isNullOrBlank() ||
                sectionHeaders.isNotEmpty() ||
                breadcrumbs.isNotEmpty() ||
                visibleLabels.isNotEmpty()

    /**
     * Format contextual text for LLM prompt
     *
     * Generates a compact, token-efficient representation for LLM consumption.
     *
     * @return Formatted string for LLM prompt
     */
    fun toLLMPromptSection(): String = buildString {
        screenTitle?.let {
            if (it.isNotBlank()) appendLine("Screen: $it")
        }

        if (breadcrumbs.isNotEmpty()) {
            appendLine("Path: ${breadcrumbs.joinToString(" > ")}")
        }

        if (sectionHeaders.isNotEmpty()) {
            appendLine("Sections: ${sectionHeaders.joinToString(", ")}")
        }

        if (visibleLabels.isNotEmpty()) {
            appendLine("Context: ${visibleLabels.take(5).joinToString("; ")}")
        }
    }

    /**
     * Format as compact single line (for token-constrained scenarios)
     *
     * @return Single-line compact representation
     */
    fun toCompactString(): String = buildString {
        screenTitle?.let { append("[$it] ") }
        if (breadcrumbs.isNotEmpty()) {
            append(breadcrumbs.joinToString(">"))
            append(" ")
        }
        if (sectionHeaders.isNotEmpty()) {
            append("{${sectionHeaders.take(3).joinToString(",")}}")
        }
    }

    /**
     * Estimate token count for this contextual text
     *
     * Uses rough approximation: ~0.75 tokens per word
     *
     * @return Estimated token count
     */
    fun estimateTokenCount(): Int {
        var wordCount = 0
        screenTitle?.let { wordCount += it.split(" ").size }
        wordCount += sectionHeaders.sumOf { it.split(" ").size }
        wordCount += breadcrumbs.sumOf { it.split(" ").size }
        wordCount += visibleLabels.sumOf { it.split(" ").size }

        // Rough approximation: 0.75 tokens per word
        return (wordCount * 0.75).toInt()
    }
}

/**
 * Text Category Classification
 *
 * Classifies text elements by their contextual role.
 */
enum class ContextualTextCategory {
    /** Screen title - main heading/toolbar title */
    SCREEN_TITLE,

    /** Section header - category/group heading */
    SECTION_HEADER,

    /** Breadcrumb - navigation path component */
    BREADCRUMB,

    /** Descriptive label - non-actionable info text */
    LABEL,

    /** Unknown/unclassified text */
    UNKNOWN
}

/**
 * Extracted Text Item
 *
 * Represents a single extracted text element with its classification.
 *
 * @property text The text content
 * @property category Classification of the text role
 * @property confidence Confidence score (0.0-1.0)
 * @property depth Node depth in UI tree
 */
data class ExtractedTextItem(
    val text: String,
    val category: ContextualTextCategory,
    val confidence: Float,
    val depth: Int = 0
)
