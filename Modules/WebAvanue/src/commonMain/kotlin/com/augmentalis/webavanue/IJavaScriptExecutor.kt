package com.augmentalis.webavanue

/**
 * Platform-abstract JavaScript evaluation interface.
 *
 * Android: wraps WebView.evaluateJavascript()
 * iOS: wraps WKWebView.evaluateJavaScript() (future)
 *
 * All methods must be called from the main/UI thread on Android.
 */
interface IJavaScriptExecutor {
    /**
     * Evaluate JavaScript code in the web page context.
     *
     * @param script JavaScript code to evaluate (should return JSON string)
     * @return JSON result string, or null if execution failed/timed out
     */
    suspend fun evaluateJavaScript(script: String): String?
}
