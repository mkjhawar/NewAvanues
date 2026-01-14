/*
 * Copyright (c) 2025 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * All rights reserved.
 *
 * VoiceOS - Voice-First Accessibility Platform
 * ScreenContextualText - Holds non-actionable contextual text from screen
 */

package com.augmentalis.voiceoscore.accessibility.extractors

/**
 * Holds extracted contextual text from a screen for NLU enhancement.
 *
 * Contains non-actionable text elements that provide semantic context:
 * - Screen titles and headers
 * - Navigation breadcrumbs
 * - Section headers
 * - Static labels that describe the UI state
 *
 * Used by AIContextSerializer to enhance LLM prompts with screen context.
 */
data class ScreenContextualText(
    /** Screen or window title */
    val screenTitle: String? = null,

    /** Navigation breadcrumbs (e.g., "Settings > Display > Theme") */
    val breadcrumbs: List<String> = emptyList(),

    /** Section headers visible on screen */
    val sectionHeaders: List<String> = emptyList(),

    /** Non-actionable text labels that provide context */
    val visibleLabels: List<String> = emptyList(),

    /** Primary content description (for content-heavy screens) */
    val contentSummary: String? = null
) {
    /**
     * Returns true if any contextual text content is available.
     */
    fun hasContent(): Boolean {
        return !screenTitle.isNullOrBlank() ||
               breadcrumbs.isNotEmpty() ||
               sectionHeaders.isNotEmpty() ||
               visibleLabels.isNotEmpty() ||
               !contentSummary.isNullOrBlank()
    }

    /**
     * Estimates the approximate token count for this contextual text.
     * Uses a simple heuristic of ~4 characters per token.
     */
    fun estimateTokenCount(): Int {
        val totalChars = (screenTitle?.length ?: 0) +
            breadcrumbs.sumOf { it.length } +
            sectionHeaders.sumOf { it.length } +
            visibleLabels.sumOf { it.length } +
            (contentSummary?.length ?: 0)
        return (totalChars / 4).coerceAtLeast(1)
    }

    /**
     * Returns a compact string representation for token-efficient prompts.
     */
    fun toCompactString(): String {
        val parts = mutableListOf<String>()

        screenTitle?.takeIf { it.isNotBlank() }?.let {
            parts.add("Title: $it")
        }

        if (breadcrumbs.isNotEmpty()) {
            parts.add("Path: ${breadcrumbs.joinToString(" > ")}")
        }

        if (sectionHeaders.isNotEmpty()) {
            parts.add("Sections: ${sectionHeaders.take(3).joinToString(", ")}")
        }

        if (visibleLabels.isNotEmpty()) {
            parts.add("Labels: ${visibleLabels.take(3).joinToString(", ")}")
        }

        return parts.joinToString("; ")
    }

    companion object {
        /**
         * Creates an empty ScreenContextualText instance.
         */
        fun empty(): ScreenContextualText = ScreenContextualText()
    }
}
