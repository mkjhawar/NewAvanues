package com.augmentalis.webavanue

import android.webkit.WebView

/**
 * Android implementation of XR camera management.
 *
 * Extends CommonCameraManager with Android WebView JavaScript execution.
 */
class XRCameraManager(private val webView: WebView? = null) : CommonCameraManager() {

    override fun executeReleaseCameraScript(callback: ((Boolean) -> Unit)?) {
        webView?.evaluateJavascript(JS_RELEASE_CAMERA) { result ->
            callback?.invoke(result?.contains("released") == true)
        }
    }

    override fun executeCheckCameraScript(callback: (String) -> Unit) {
        webView?.evaluateJavascript(JS_CHECK_CAMERA) { result ->
            callback(result?.trim('"') ?: "inactive")
        }
    }
}
