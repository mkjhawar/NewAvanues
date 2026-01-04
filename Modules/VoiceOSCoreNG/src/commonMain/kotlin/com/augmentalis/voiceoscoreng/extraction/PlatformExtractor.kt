package com.augmentalis.voiceoscoreng.extraction

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * PlatformExtractor - Expect declarations for platform-specific element extraction.
 *
 * Each platform implements this differently:
 * - Android: AccessibilityNodeInfo + WebView.evaluateJavascript
 * - iOS: WKWebView.evaluateJavaScript (web only)
 * - Desktop: Chrome DevTools Protocol (CDP)
 *
 * The extraction pipeline uses a fallback chain:
 * 1. Native accessibility extraction (if available)
 * 2. WebView JavaScript injection
 * 3. ElementParser HTML parsing (fallback)
 */

/**
 * Extract accessibility elements from the current screen.
 * Platform-specific implementation.
 *
 * @return List of extracted ElementInfo, or empty if not available
 */
expect fun extractAccessibilityElements(): List<ElementInfo>

/**
 * Execute JavaScript in a web context and return the result.
 * Platform-specific implementation.
 *
 * @param script JavaScript code to execute
 * @return JSON result string, or null if execution failed
 */
expect fun executeWebScript(script: String): String?

/**
 * Check if accessibility extraction is available on this platform.
 *
 * @return true if native accessibility extraction is supported
 */
expect fun isAccessibilityAvailable(): Boolean

/**
 * Check if web script execution is available.
 *
 * @return true if JavaScript injection is supported
 */
expect fun isWebExtractionAvailable(): Boolean

/**
 * Get the platform name for logging/debugging.
 *
 * @return Platform identifier (e.g., "android", "ios", "desktop")
 */
expect fun getPlatformName(): String

/**
 * ExtractionResult - Result of element extraction operation.
 *
 * @property elements List of extracted elements
 * @property source Where the elements came from
 * @property errorMessage Error message if extraction failed
 */
data class ExtractionResult(
    val elements: List<ElementInfo>,
    val source: ExtractionSource,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = errorMessage == null
    val elementCount: Int get() = elements.size

    companion object {
        fun empty(source: ExtractionSource) = ExtractionResult(emptyList(), source)
        fun error(message: String, source: ExtractionSource) =
            ExtractionResult(emptyList(), source, message)
    }
}

/**
 * ExtractionSource - Where elements were extracted from.
 */
enum class ExtractionSource {
    /** Native accessibility service (Android AccessibilityService, iOS Accessibility) */
    ACCESSIBILITY,

    /** WebView JavaScript injection */
    WEBVIEW_JS,

    /** Browser CDP (Chrome DevTools Protocol) */
    CDP,

    /** HTML parsing fallback */
    HTML_PARSER,

    /** No extraction source available */
    NONE
}

/**
 * UnifiedExtractor - Orchestrates extraction across platforms with fallback chain.
 */
object UnifiedExtractor {

    /**
     * Extract elements using the best available method.
     *
     * Fallback chain:
     * 1. Native accessibility (if available)
     * 2. WebView JS injection (if available)
     * 3. Return empty result
     *
     * @return ExtractionResult with elements and source info
     */
    fun extract(): ExtractionResult {
        // Try native accessibility first
        if (isAccessibilityAvailable()) {
            try {
                val elements = extractAccessibilityElements()
                if (elements.isNotEmpty()) {
                    return ExtractionResult(elements, ExtractionSource.ACCESSIBILITY)
                }
            } catch (e: Exception) {
                // Fall through to next method
            }
        }

        // Try WebView JS injection
        if (isWebExtractionAvailable()) {
            try {
                val json = executeWebScript(ExtractionBundle.ELEMENT_EXTRACTOR_JS)
                if (!json.isNullOrBlank()) {
                    val elements = ElementParser.parseAccessibilityJson(json)
                    if (elements.isNotEmpty()) {
                        return ExtractionResult(elements, ExtractionSource.WEBVIEW_JS)
                    }
                }
            } catch (e: Exception) {
                // Fall through to empty result
            }
        }

        return ExtractionResult.empty(ExtractionSource.NONE)
    }

    /**
     * Extract elements from HTML content using the parser.
     *
     * @param html HTML content to parse
     * @return ExtractionResult with parsed elements
     */
    fun extractFromHtml(html: String): ExtractionResult {
        return try {
            val elements = ElementParser.parseHtml(html)
            ExtractionResult(elements, ExtractionSource.HTML_PARSER)
        } catch (e: Exception) {
            ExtractionResult.error("HTML parsing failed: ${e.message}", ExtractionSource.HTML_PARSER)
        }
    }

    /**
     * Extract elements from accessibility JSON.
     *
     * @param json JSON from accessibility service
     * @return ExtractionResult with parsed elements
     */
    fun extractFromAccessibilityJson(json: String): ExtractionResult {
        return try {
            val elements = ElementParser.parseAccessibilityJson(json)
            ExtractionResult(elements, ExtractionSource.ACCESSIBILITY)
        } catch (e: Exception) {
            ExtractionResult.error("JSON parsing failed: ${e.message}", ExtractionSource.ACCESSIBILITY)
        }
    }

    /**
     * Get information about available extraction sources.
     *
     * @return Map of source to availability
     */
    fun getAvailableSources(): Map<ExtractionSource, Boolean> {
        return mapOf(
            ExtractionSource.ACCESSIBILITY to isAccessibilityAvailable(),
            ExtractionSource.WEBVIEW_JS to isWebExtractionAvailable(),
            ExtractionSource.HTML_PARSER to true // Always available
        )
    }
}
