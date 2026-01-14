/**
 * ExtractionStubs.kt - Stub types for VoiceOSCoreNG extraction integration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-03
 *
 * These stubs provide compatibility while VoiceOSCoreNG is being developed.
 * Once VoiceOSCoreNG is ready, these will be replaced with the real types.
 */
package com.augmentalis.voiceoscore.integration.extraction

/**
 * Bounds - Represents element bounds
 */
data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * ElementInfo - Cross-platform element representation
 */
data class ElementInfo(
    val className: String,
    val resourceId: String,
    val text: String?,
    val contentDescription: String,
    val bounds: Bounds,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEnabled: Boolean,
    val packageName: String
)

/**
 * Extraction registry for accessibility extractors
 */
private var accessibilityExtractor: (() -> List<ElementInfo>)? = null
private var webScriptExecutor: ((String) -> String?)? = null

/**
 * Register an accessibility extractor
 */
fun registerAccessibilityExtractor(extractor: () -> List<ElementInfo>) {
    accessibilityExtractor = extractor
}

/**
 * Register a web script executor
 */
fun registerWebScriptExecutor(executor: (String) -> String?) {
    webScriptExecutor = executor
}

/**
 * Clear all registered extractors
 */
fun clearExtractors() {
    accessibilityExtractor = null
    webScriptExecutor = null
}

/**
 * Extraction bundle for JavaScript extraction
 */
object ExtractionBundle {
    /**
     * JavaScript for extracting elements from web pages
     */
    const val ELEMENT_EXTRACTOR_JS = """
        (function() {
            var elements = [];
            var allElements = document.querySelectorAll('*');
            for (var i = 0; i < allElements.length; i++) {
                var el = allElements[i];
                var rect = el.getBoundingClientRect();
                if (rect.width > 0 && rect.height > 0) {
                    elements.push({
                        tagName: el.tagName,
                        id: el.id || '',
                        className: el.className || '',
                        text: el.textContent ? el.textContent.substring(0, 100) : '',
                        bounds: {
                            left: Math.round(rect.left),
                            top: Math.round(rect.top),
                            right: Math.round(rect.right),
                            bottom: Math.round(rect.bottom)
                        },
                        isClickable: el.onclick !== null || el.tagName === 'A' || el.tagName === 'BUTTON',
                        isScrollable: el.scrollHeight > el.clientHeight
                    });
                }
            }
            return JSON.stringify(elements);
        })()
    """
}
