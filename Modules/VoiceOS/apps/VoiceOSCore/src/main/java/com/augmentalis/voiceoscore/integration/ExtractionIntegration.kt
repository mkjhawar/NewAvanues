/**
 * ExtractionIntegration.kt - Connects VoiceOSCore extraction to VoiceOSCoreNG shared code
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-31
 *
 * This adapter bridges VoiceOSCore's UIScrapingEngine with VoiceOSCoreNG's
 * shared extraction framework, enabling:
 * - Cross-platform element representation
 * - Unified extraction pipeline
 * - Fallback chain support
 */
package com.augmentalis.voiceoscore.integration

import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.extraction.registerAccessibilityExtractor
import com.augmentalis.voiceoscoreng.extraction.registerWebScriptExecutor
import com.augmentalis.voiceoscoreng.extraction.clearExtractors

/**
 * ExtractionIntegration - Bridges UIScrapingEngine with VoiceOSCoreNG extraction.
 *
 * Usage:
 * ```
 * val integration = ExtractionIntegration(scrapingEngine)
 * integration.register()
 *
 * // Now VoiceOSCoreNG's UnifiedExtractor can use VoiceOSCore extraction
 * val result = UnifiedExtractor.extract()
 * ```
 */
class ExtractionIntegration(
    private val scrapingEngine: UIScrapingEngine
) {
    private var isRegistered = false

    /**
     * Register UIScrapingEngine with VoiceOSCoreNG.
     * Call this when AccessibilityService is connected.
     */
    fun register() {
        if (isRegistered) return

        registerAccessibilityExtractor {
            extractElements()
        }

        isRegistered = true
    }

    /**
     * Unregister from VoiceOSCoreNG.
     * Call this when AccessibilityService is disconnected.
     */
    fun unregister() {
        if (!isRegistered) return

        clearExtractors()
        isRegistered = false
    }

    /**
     * Extract elements using UIScrapingEngine and convert to ElementInfo.
     */
    private fun extractElements(): List<ElementInfo> {
        return try {
            scrapingEngine.extractUIElements()
                .map { it.toElementInfo() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        /**
         * Convert UIElement to ElementInfo for cross-platform use.
         */
        fun UIScrapingEngine.UIElement.toElementInfo(): ElementInfo {
            return ElementInfo(
                className = className ?: "unknown",
                resourceId = resourceId ?: "",
                text = text,
                contentDescription = contentDescription ?: "",
                bounds = Bounds(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom
                ),
                isClickable = isClickable,
                isScrollable = false, // UIElement doesn't track scrollability
                isEnabled = true, // Assume enabled by default
                packageName = "" // Not available in UIElement
            )
        }

        /**
         * Convert ElementInfo back to UIElement attributes for compatibility.
         */
        fun ElementInfo.toUIElementAttributes(): Map<String, Any> {
            return mapOf(
                "className" to className,
                "resourceId" to resourceId,
                "text" to text,
                "contentDescription" to contentDescription,
                "bounds" to "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}",
                "isClickable" to isClickable,
                "isScrollable" to isScrollable,
                "isEnabled" to isEnabled
            )
        }
    }
}

/**
 * WebViewExtractionIntegration - Bridges WebView JavaScript execution.
 *
 * Usage:
 * ```
 * val integration = WebViewExtractionIntegration { script ->
 *     webView.evaluateJavascript(script) { result -> ... }
 * }
 * integration.register()
 * ```
 */
class WebViewExtractionIntegration(
    private val scriptExecutor: (String) -> String?
) {
    private var isRegistered = false

    /**
     * Register WebView script executor with VoiceOSCoreNG.
     */
    fun register() {
        if (isRegistered) return

        registerWebScriptExecutor(scriptExecutor)
        isRegistered = true
    }

    /**
     * Unregister from VoiceOSCoreNG.
     */
    fun unregister() {
        if (!isRegistered) return

        clearExtractors()
        isRegistered = false
    }

    companion object {
        /**
         * Get the shared extraction JavaScript bundle.
         * Use this to inject into WebView for element extraction.
         */
        fun getExtractionScript(): String {
            return com.augmentalis.voiceoscoreng.extraction.ExtractionBundle.ELEMENT_EXTRACTOR_JS
        }
    }
}
