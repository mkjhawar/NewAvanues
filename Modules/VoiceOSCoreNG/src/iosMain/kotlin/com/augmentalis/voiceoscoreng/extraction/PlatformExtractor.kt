package com.augmentalis.voiceoscoreng.extraction

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * iOS implementation of PlatformExtractor.
 *
 * On iOS, element extraction can use:
 * - WKWebView.evaluateJavaScript for web content
 * - Native accessibility APIs (limited compared to Android)
 *
 * This is a stub implementation. Real extraction will be
 * implemented in Phase 4 using WKWebView JavaScript evaluation.
 */

// Holder for web script execution callback
private var webScriptExecutor: ((String) -> String?)? = null

/**
 * Register the web script executor callback.
 * Called by iOS native code to connect WKWebView.
 */
fun registerWebScriptExecutor(executor: (String) -> String?) {
    webScriptExecutor = executor
}

/**
 * Clear registered extractors (for testing).
 */
fun clearExtractors() {
    webScriptExecutor = null
}

actual fun extractAccessibilityElements(): List<ElementInfo> {
    // iOS doesn't have Android-style accessibility extraction
    // Web content uses JavaScript injection instead
    return emptyList()
}

actual fun executeWebScript(script: String): String? {
    return webScriptExecutor?.invoke(script)
}

actual fun isAccessibilityAvailable(): Boolean {
    // iOS doesn't support native accessibility extraction like Android
    return false
}

actual fun isWebExtractionAvailable(): Boolean {
    return webScriptExecutor != null
}

actual fun getPlatformName(): String = "ios"
