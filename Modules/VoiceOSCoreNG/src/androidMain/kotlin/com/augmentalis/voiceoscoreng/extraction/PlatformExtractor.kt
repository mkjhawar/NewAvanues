package com.augmentalis.voiceoscoreng.extraction

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Android implementation of PlatformExtractor.
 *
 * On Android, element extraction can use:
 * - AccessibilityService for native accessibility tree
 * - WebView.evaluateJavascript for web content
 *
 * This is a stub implementation. Real extraction will be connected
 * to VoiceOSCore's UIScrapingEngine in Phase 2.
 */

// Holder for accessibility extraction callback
private var accessibilityExtractor: (() -> List<ElementInfo>)? = null
private var webScriptExecutor: ((String) -> String?)? = null

/**
 * Register the accessibility extractor callback.
 * Called by VoiceOSCore to connect its UIScrapingEngine.
 */
fun registerAccessibilityExtractor(extractor: () -> List<ElementInfo>) {
    accessibilityExtractor = extractor
}

/**
 * Register the web script executor callback.
 * Called by VoiceOSCore to connect its WebViewScrapingEngine.
 */
fun registerWebScriptExecutor(executor: (String) -> String?) {
    webScriptExecutor = executor
}

/**
 * Clear registered extractors (for testing).
 */
fun clearExtractors() {
    accessibilityExtractor = null
    webScriptExecutor = null
}

actual fun extractAccessibilityElements(): List<ElementInfo> {
    return accessibilityExtractor?.invoke() ?: emptyList()
}

actual fun executeWebScript(script: String): String? {
    return webScriptExecutor?.invoke(script)
}

actual fun isAccessibilityAvailable(): Boolean {
    return accessibilityExtractor != null
}

actual fun isWebExtractionAvailable(): Boolean {
    return webScriptExecutor != null
}

actual fun getPlatformName(): String = "android"
