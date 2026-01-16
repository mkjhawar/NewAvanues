package com.augmentalis.voiceoscoreng.functions

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Desktop (JVM) implementation of PlatformExtractor.
 *
 * On Desktop, element extraction can use:
 * - Chrome DevTools Protocol (CDP) for browser automation
 * - Direct DOM access via browser integration
 *
 * This is a stub implementation. Real extraction will be
 * implemented in Phase 4 using CDP Runtime.evaluate.
 */

// Holder for CDP script execution callback
private var cdpScriptExecutor: ((String) -> String?)? = null

/**
 * Register the CDP script executor callback.
 * Called by desktop app to connect Chrome DevTools Protocol.
 */
fun registerCdpScriptExecutor(executor: (String) -> String?) {
    cdpScriptExecutor = executor
}

/**
 * Clear registered extractors (for testing).
 */
fun clearExtractors() {
    cdpScriptExecutor = null
}

actual fun extractAccessibilityElements(): List<ElementInfo> {
    // Desktop doesn't have Android-style accessibility extraction
    // Uses CDP/browser integration instead
    return emptyList()
}

actual fun executeWebScript(script: String): String? {
    return cdpScriptExecutor?.invoke(script)
}

actual fun isAccessibilityAvailable(): Boolean {
    // Desktop doesn't support native accessibility extraction
    return false
}

actual fun isWebExtractionAvailable(): Boolean {
    return cdpScriptExecutor != null
}

actual fun getPlatformName(): String = "desktop"
