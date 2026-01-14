/**
 * UnifiedExtractor.kt - Unified element extraction facade
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.extraction

/**
 * Unified facade for extracting elements from various sources.
 */
object UnifiedExtractor {

    /**
     * Extracts elements from HTML string.
     */
    fun extractFromHtml(html: String): ExtractionResult {
        val elements = ElementParser.parseHtml(html)
        return ExtractionResult(
            elements = elements,
            source = ExtractionSource.HTML_PARSER,
            isSuccess = true
        )
    }

    /**
     * Extracts elements from accessibility JSON.
     */
    fun extractFromAccessibilityJson(json: String): ExtractionResult {
        val elements = ElementParser.parseAccessibilityJson(json)
        return ExtractionResult(
            elements = elements,
            source = ExtractionSource.ACCESSIBILITY,
            isSuccess = true
        )
    }

    /**
     * Returns a map of extraction sources and their availability.
     */
    fun getAvailableSources(): Map<ExtractionSource, Boolean> {
        return mapOf(
            ExtractionSource.HTML_PARSER to true,  // Always available
            ExtractionSource.ACCESSIBILITY to false,  // Platform-dependent
            ExtractionSource.WEBVIEW_JS to false,     // Platform-dependent
            ExtractionSource.CDP to false,            // Requires Chrome DevTools
            ExtractionSource.NONE to false
        )
    }
}

/**
 * Returns the current platform name.
 */
expect fun getPlatformName(): String
