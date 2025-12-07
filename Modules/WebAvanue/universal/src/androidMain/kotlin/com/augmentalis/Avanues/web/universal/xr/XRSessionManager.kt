package com.augmentalis.Avanues.web.universal.xr

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages WebXR session lifecycle in coordination with Android activity lifecycle.
 *
 * Requirements:
 * - REQ-XR-005: WebXR Session Lifecycle Management
 * - REQ-XR-007: Performance Optimization for XR
 *
 * Responsibilities:
 * - Track XR session state (inactive, requesting, active, paused)
 * - Pause/resume XR sessions during activity lifecycle events
 * - Monitor session duration for auto-pause
 * - Coordinate with WebView lifecycle
 *
 * @see <a href="/.ideacode-v2/features/012-add-webxr-support-to-webavanue-browser-to-enable-immersive-ar-vr-web-experiences/spec.md">WebXR Specification</a>
 */
class XRSessionManager {

    /**
     * WebXR session state.
     */
    enum class SessionState {
        /** No active XR session */
        INACTIVE,

        /** XR session requested, waiting for permission/initialization */
        REQUESTING,

        /** XR session active and running */
        ACTIVE,

        /** XR session paused (app backgrounded) */
        PAUSED,

        /** XR session ended (user exited or error occurred) */
        ENDED
    }

    /**
     * Type of XR session.
     */
    enum class SessionMode {
        /** Augmented Reality - camera-based world tracking */
        IMMERSIVE_AR,

        /** Virtual Reality - 360° immersive content */
        IMMERSIVE_VR,

        /** Inline XR - non-immersive XR content in webpage */
        INLINE,

        /** Unknown or not yet determined */
        UNKNOWN
    }

    /**
     * Session information.
     */
    data class SessionInfo(
        val mode: SessionMode = SessionMode.UNKNOWN,
        val startTime: Long = 0L,
        val durationMillis: Long = 0L,
        val frameRate: Float = 0f
    )

    // State flows
    private val _sessionState = MutableStateFlow(SessionState.INACTIVE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _sessionInfo = MutableStateFlow(SessionInfo())
    val sessionInfo: StateFlow<SessionInfo> = _sessionInfo.asStateFlow()

    private var webView: WebView? = null
    private var sessionStartTime: Long = 0L

    companion object {
        /**
         * Auto-pause timeout (30 minutes) for battery/thermal protection.
         * REQ-XR-007: Performance Optimization
         */
        private const val AUTO_PAUSE_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes

        /**
         * JavaScript to end XR session.
         */
        private const val JS_END_SESSION = """
            (function() {
                if (navigator.xr && window.xrSession) {
                    window.xrSession.end();
                    return 'Session ended';
                }
                return 'No active session';
            })();
        """

        /**
         * JavaScript to check XR session state.
         */
        private const val JS_CHECK_SESSION = """
            (function() {
                if (navigator.xr && window.xrSession) {
                    return JSON.stringify({
                        mode: window.xrSession.mode || 'unknown',
                        active: true
                    });
                }
                return JSON.stringify({ active: false });
            })();
        """
    }

    /**
     * Set WebView instance for session management.
     * Should be called when WebView is created.
     *
     * @param webView WebView instance
     */
    fun setWebView(webView: WebView) {
        this.webView = webView
    }

    /**
     * Notify that XR session is being requested.
     * Called when web page requests XR session via navigator.xr.requestSession().
     *
     * @param mode Session mode (immersive-ar, immersive-vr, inline)
     */
    fun onSessionRequested(mode: SessionMode) {
        _sessionState.value = SessionState.REQUESTING
        _sessionInfo.value = SessionInfo(mode = mode)
    }

    /**
     * Notify that XR session has started.
     * Called when XR session successfully initialized.
     *
     * @param mode Session mode
     */
    fun onSessionStarted(mode: SessionMode) {
        sessionStartTime = System.currentTimeMillis()
        _sessionState.value = SessionState.ACTIVE
        _sessionInfo.value = SessionInfo(
            mode = mode,
            startTime = sessionStartTime,
            durationMillis = 0L
        )
    }

    /**
     * Notify that XR session has ended.
     * Called when user exits XR or session is terminated.
     */
    fun onSessionEnded() {
        val duration = if (sessionStartTime > 0) {
            System.currentTimeMillis() - sessionStartTime
        } else {
            0L
        }

        _sessionInfo.value = _sessionInfo.value.copy(durationMillis = duration)
        _sessionState.value = SessionState.ENDED
        sessionStartTime = 0L

        // Transition to INACTIVE after a brief delay
        // (allows UI to show "session ended" state)
        // In production, use coroutine delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_sessionState.value == SessionState.ENDED) {
                _sessionState.value = SessionState.INACTIVE
            }
        }, 1000)
    }

    /**
     * Pause active XR session.
     * Called when activity is paused (Home button, app switcher, etc.).
     *
     * REQ-XR-005: Session must pause when activity pauses.
     */
    fun pauseSession() {
        if (_sessionState.value == SessionState.ACTIVE) {
            _sessionState.value = SessionState.PAUSED

            // End WebXR session via JavaScript
            webView?.evaluateJavascript(JS_END_SESSION) { result ->
                // Session ended in web content
                // State will transition back to INACTIVE when web reports session end
            }
        }
    }

    /**
     * Resume paused XR session.
     * Called when activity is resumed from background.
     *
     * Note: WebXR sessions cannot be automatically resumed - user must re-request.
     * This method transitions state to allow new session request.
     */
    fun resumeSession() {
        if (_sessionState.value == SessionState.PAUSED) {
            // Transition to INACTIVE - user must manually restart XR session
            // WebXR spec requires user gesture to start immersive session
            _sessionState.value = SessionState.INACTIVE
        }
    }

    /**
     * Check if XR session is currently active.
     *
     * @return true if session is active (not paused or ended)
     */
    fun isSessionActive(): Boolean {
        return _sessionState.value == SessionState.ACTIVE
    }

    /**
     * Check session duration and auto-pause if timeout exceeded.
     * Should be called periodically during active session.
     *
     * REQ-XR-007: Auto-pause after 30 minutes to prevent battery drain.
     *
     * @return true if session was auto-paused
     */
    fun checkAutoPause(): Boolean {
        if (_sessionState.value == SessionState.ACTIVE) {
            val duration = System.currentTimeMillis() - sessionStartTime
            if (duration > AUTO_PAUSE_TIMEOUT_MS) {
                pauseSession()
                return true
            }
        }
        return false
    }

    /**
     * Update session frame rate for performance monitoring.
     * Called periodically by performance monitor.
     *
     * @param fps Current frame rate
     */
    fun updateFrameRate(fps: Float) {
        if (_sessionState.value == SessionState.ACTIVE) {
            _sessionInfo.value = _sessionInfo.value.copy(frameRate = fps)
        }
    }

    /**
     * Query current XR session state from WebView.
     * Asynchronously checks if web content has active XR session.
     *
     * @param callback Callback with session active state and mode
     */
    fun querySessionState(callback: (active: Boolean, mode: String?) -> Unit) {
        webView?.evaluateJavascript(JS_CHECK_SESSION) { result ->
            // Parse JSON result
            // Example: {"active":true,"mode":"immersive-ar"}
            try {
                val active = result?.contains("\"active\":true") == true
                val mode = if (active) {
                    // Extract mode from JSON
                    val modeMatch = Regex("\"mode\":\"([^\"]+)\"").find(result)
                    modeMatch?.groupValues?.get(1)
                } else {
                    null
                }
                callback(active, mode)
            } catch (e: Exception) {
                callback(false, null)
            }
        }
    }

    /**
     * Force end current XR session.
     * Used when app is destroyed or XR must be terminated immediately.
     */
    fun forceEndSession() {
        if (_sessionState.value in listOf(SessionState.ACTIVE, SessionState.PAUSED, SessionState.REQUESTING)) {
            webView?.evaluateJavascript(JS_END_SESSION, null)
            onSessionEnded()
        }
    }

    /**
     * Clean up resources.
     * Called when WebView is destroyed.
     */
    fun cleanup() {
        forceEndSession()
        webView = null
    }

    /**
     * Get user-friendly session state description.
     *
     * @return Human-readable session state
     */
    fun getSessionStateDescription(): String {
        return when (_sessionState.value) {
            SessionState.INACTIVE -> "No XR session active"
            SessionState.REQUESTING -> "Starting XR session..."
            SessionState.ACTIVE -> {
                val mode = _sessionInfo.value.mode
                val modeStr = when (mode) {
                    SessionMode.IMMERSIVE_AR -> "AR"
                    SessionMode.IMMERSIVE_VR -> "VR"
                    SessionMode.INLINE -> "Inline XR"
                    SessionMode.UNKNOWN -> "XR"
                }
                "$modeStr session active"
            }
            SessionState.PAUSED -> "XR session paused"
            SessionState.ENDED -> "XR session ended"
        }
    }
}
