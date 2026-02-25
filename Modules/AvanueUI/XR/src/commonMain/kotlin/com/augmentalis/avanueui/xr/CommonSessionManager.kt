/**
 * CommonSessionManager.kt - Platform-agnostic XR session state machine
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Migrated to AvaUI/XR)
 *
 * Originally from: Avanues/Web/common/webavanue/universal
 *
 * Manages XR session lifecycle state transitions:
 * INACTIVE → REQUESTING → ACTIVE → PAUSED/ENDED → INACTIVE
 *
 * Platform implementations provide:
 * - JavaScript execution in WebView
 * - Delayed task scheduling
 */
package com.augmentalis.avanueui.xr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CommonSessionManager - Platform-agnostic XR session state machine
 *
 * Manages XR session lifecycle state transitions:
 * INACTIVE → REQUESTING → ACTIVE → PAUSED/ENDED → INACTIVE
 *
 * Platform implementations provide:
 * - JavaScript execution in WebView
 * - Delayed task scheduling
 */
abstract class CommonSessionManager {

    // State flows
    protected val _sessionState = MutableStateFlow(SessionState.INACTIVE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    protected val _sessionInfo = MutableStateFlow(SessionInfo())
    val sessionInfo: StateFlow<SessionInfo> = _sessionInfo.asStateFlow()

    protected var sessionStartTime: Long = 0L

    /**
     * Notify that XR session is being requested
     *
     * @param mode Session mode (immersive-ar, immersive-vr, inline)
     */
    fun onSessionRequested(mode: SessionMode) {
        _sessionState.value = SessionState.REQUESTING
        _sessionInfo.value = SessionInfo(mode = mode)
    }

    /**
     * Notify that XR session has started
     *
     * @param mode Session mode
     */
    fun onSessionStarted(mode: SessionMode) {
        sessionStartTime = currentTimeMillis()
        _sessionState.value = SessionState.ACTIVE
        _sessionInfo.value = SessionInfo(
            mode = mode,
            startTime = sessionStartTime,
            durationMillis = 0L
        )
    }

    /**
     * Notify that XR session has ended
     */
    fun onSessionEnded() {
        val duration = if (sessionStartTime > 0) {
            currentTimeMillis() - sessionStartTime
        } else {
            0L
        }

        _sessionInfo.value = _sessionInfo.value.copy(durationMillis = duration)
        _sessionState.value = SessionState.ENDED
        sessionStartTime = 0L

        // Transition to INACTIVE after delay
        scheduleDelayedTask(1000L) {
            if (_sessionState.value == SessionState.ENDED) {
                _sessionState.value = SessionState.INACTIVE
            }
        }
    }

    /**
     * Pause active XR session
     */
    fun pauseSession() {
        if (_sessionState.value == SessionState.ACTIVE) {
            _sessionState.value = SessionState.PAUSED
            executeEndSessionScript()
        }
    }

    /**
     * Resume paused XR session
     * Note: WebXR requires user gesture to restart - transitions to INACTIVE
     */
    fun resumeSession() {
        if (_sessionState.value == SessionState.PAUSED) {
            _sessionState.value = SessionState.INACTIVE
        }
    }

    /**
     * Check if XR session is currently active
     */
    fun isSessionActive(): Boolean {
        return _sessionState.value == SessionState.ACTIVE
    }

    /**
     * Check session duration and auto-pause if timeout exceeded
     *
     * @return true if session was auto-paused
     */
    fun checkAutoPause(): Boolean {
        if (_sessionState.value == SessionState.ACTIVE) {
            val duration = currentTimeMillis() - sessionStartTime
            if (duration > AUTO_PAUSE_TIMEOUT_MS) {
                pauseSession()
                return true
            }
        }
        return false
    }

    /**
     * Update session frame rate for performance monitoring
     *
     * @param fps Current frame rate
     */
    fun updateFrameRate(fps: Float) {
        if (_sessionState.value == SessionState.ACTIVE) {
            _sessionInfo.value = _sessionInfo.value.copy(frameRate = fps)
        }
    }

    /**
     * Force end current XR session
     */
    fun forceEndSession() {
        if (_sessionState.value in listOf(SessionState.ACTIVE, SessionState.PAUSED, SessionState.REQUESTING)) {
            executeEndSessionScript()
            onSessionEnded()
        }
    }

    /**
     * Get user-friendly session state description
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

    // ========== Abstract Methods (Platform-Specific) ==========

    /**
     * Execute JavaScript to end XR session in WebView
     */
    protected abstract fun executeEndSessionScript()

    /**
     * Schedule a delayed task
     *
     * @param delayMs Delay in milliseconds
     * @param task Task to execute
     */
    protected abstract fun scheduleDelayedTask(delayMs: Long, task: () -> Unit)

    /**
     * Get current time in milliseconds
     */
    protected abstract fun currentTimeMillis(): Long

    companion object {
        /**
         * Auto-pause timeout (30 minutes) for battery/thermal protection
         */
        const val AUTO_PAUSE_TIMEOUT_MS = 30 * 60 * 1000L

        /**
         * JavaScript to end XR session
         */
        const val JS_END_SESSION = """
            (function() {
                if (navigator.xr && window.xrSession) {
                    window.xrSession.end();
                    return 'Session ended';
                }
                return 'No active session';
            })();
        """

        /**
         * JavaScript to check XR session state
         */
        const val JS_CHECK_SESSION = """
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
}
