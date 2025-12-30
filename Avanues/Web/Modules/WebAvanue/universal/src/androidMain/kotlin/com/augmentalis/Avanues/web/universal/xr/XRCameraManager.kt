package com.augmentalis.Avanues.web.universal.xr

import android.webkit.WebView

/**
 * Manages camera lifecycle for WebXR AR sessions.
 * Ensures proper camera release when session pauses.
 *
 * Requirements:
 * - REQ-XR-002: Camera Permission Management
 * - REQ-XR-005: WebXR Session Lifecycle Management
 *
 * Responsibilities:
 * - Track camera usage during AR sessions
 * - Release camera when app backgrounds
 * - Notify web content of camera state changes
 *
 * @see <a href="/.ideacode-v2/features/012-add-webxr-support-to-webavanue-browser-to-enable-immersive-ar-vr-web-experiences/spec.md">WebXR Specification</a>
 */
class XRCameraManager(private val webView: WebView? = null) {

    private var isCameraActive = false

    companion object {
        /**
         * JavaScript to notify web content that camera should be released.
         */
        private const val JS_RELEASE_CAMERA = """
            (function() {
                if (navigator.xr && window.xrSession) {
                    // End AR session (releases camera)
                    window.xrSession.end();
                }

                // Also notify getUserMedia streams to stop
                if (window.localStream) {
                    window.localStream.getTracks().forEach(track => track.stop());
                }
            })();
        """

        /**
         * JavaScript to check if camera is in use by XR session.
         */
        private const val JS_CHECK_CAMERA = """
            (function() {
                if (navigator.xr && window.xrSession && window.xrSession.mode === 'immersive-ar') {
                    return 'active';
                }
                return 'inactive';
            })();
        """
    }

    /**
     * Notify that AR session has started (camera will be used).
     */
    fun onARSessionStarted() {
        isCameraActive = true
    }

    /**
     * Notify that AR session has ended (camera released).
     */
    fun onARSessionEnded() {
        isCameraActive = false
    }

    /**
     * Check if camera is currently in use.
     *
     * @return true if camera is active
     */
    fun isCameraInUse(): Boolean = isCameraActive

    /**
     * Release camera by ending AR session.
     * Should be called when activity is paused.
     */
    fun releaseCamera() {
        if (isCameraActive) {
            webView?.evaluateJavascript(JS_RELEASE_CAMERA) { result ->
                isCameraActive = false
            }
        }
    }

    /**
     * Query web content to check camera state.
     * Asynchronously checks if camera is being used by XR.
     *
     * @param callback Callback with camera state ("active" or "inactive")
     */
    fun queryCameraState(callback: (String) -> Unit) {
        webView?.evaluateJavascript(JS_CHECK_CAMERA) { result ->
            val state = result?.trim('"') ?: "inactive"
            callback(state)
        }
    }

    /**
     * Force release camera immediately.
     * Used when app is destroyed or emergency stop needed.
     */
    fun forceReleaseCamera() {
        releaseCamera()
        isCameraActive = false
    }
}
