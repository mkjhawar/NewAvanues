package com.augmentalis.webavanue

import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "AndroidJSExecutor"
private const val JS_TIMEOUT_MS = 3000L

/**
 * Android implementation of [IJavaScriptExecutor].
 *
 * Wraps WebView.evaluateJavascript() with timeout and main-thread dispatch.
 * Uses the same pattern as [WebAvanueVoiceOSBridge.executeAction].
 */
class AndroidJavaScriptExecutor(
    private val webView: WebView
) : IJavaScriptExecutor {

    override suspend fun evaluateJavaScript(script: String): String? = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<String?>()

        try {
            webView.evaluateJavascript(script) { result ->
                deferred.complete(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "evaluateJavascript failed", e)
            deferred.complete(null)
        }

        val resultStr = withTimeoutOrNull(JS_TIMEOUT_MS) { deferred.await() }

        if (resultStr.isNullOrEmpty() || resultStr == "null") {
            Log.w(TAG, "Empty or null JS result")
            return@withContext null
        }

        // Unescape the JSON string that Android WebView wraps in quotes
        resultStr
            .removeSurrounding("\"")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
