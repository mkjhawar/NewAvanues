package com.augmentalis.webavanue.feature.xr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CommonCameraManager - Platform-agnostic camera state machine for XR
 *
 * Tracks camera lifecycle during AR sessions.
 * Platform implementations handle actual camera operations.
 */
abstract class CommonCameraManager {

    /**
     * Camera state enum
     */
    enum class CameraState {
        INACTIVE,
        STARTING,
        ACTIVE,
        PAUSED,
        RELEASING,
        ERROR
    }

    // State tracking
    protected val _cameraState = MutableStateFlow(CameraState.INACTIVE)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    protected val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    /**
     * Notify that AR session has started (camera will be used)
     */
    fun onARSessionStarted() {
        _cameraState.value = CameraState.ACTIVE
        _isCameraActive.value = true
    }

    /**
     * Notify that AR session has ended (camera released)
     */
    fun onARSessionEnded() {
        _cameraState.value = CameraState.INACTIVE
        _isCameraActive.value = false
    }

    /**
     * Notify camera is paused (app backgrounded)
     */
    fun onCameraPaused() {
        if (_isCameraActive.value) {
            _cameraState.value = CameraState.PAUSED
        }
    }

    /**
     * Notify camera resumed
     */
    fun onCameraResumed() {
        if (_cameraState.value == CameraState.PAUSED) {
            _cameraState.value = CameraState.ACTIVE
        }
    }

    /**
     * Check if camera is currently in use
     */
    fun isCameraInUse(): Boolean = _isCameraActive.value

    /**
     * Release camera by ending AR session
     * Should be called when activity is paused
     */
    fun releaseCamera() {
        if (_isCameraActive.value) {
            _cameraState.value = CameraState.RELEASING
            executeReleaseCameraScript { success ->
                _cameraState.value = CameraState.INACTIVE
                _isCameraActive.value = false
            }
        }
    }

    /**
     * Force release camera immediately
     * Used when app is destroyed or emergency stop needed
     */
    fun forceReleaseCamera() {
        executeReleaseCameraScript(null)
        _cameraState.value = CameraState.INACTIVE
        _isCameraActive.value = false
    }

    /**
     * Query camera state from web content
     *
     * @param callback Callback with camera active state
     */
    fun queryCameraState(callback: (Boolean) -> Unit) {
        executeCheckCameraScript { result ->
            val isActive = result == "active"
            if (isActive != _isCameraActive.value) {
                _isCameraActive.value = isActive
                _cameraState.value = if (isActive) CameraState.ACTIVE else CameraState.INACTIVE
            }
            callback(isActive)
        }
    }

    // ========== Abstract Methods (Platform-Specific) ==========

    /**
     * Execute JavaScript to release camera in WebView
     *
     * @param callback Optional callback when complete
     */
    protected abstract fun executeReleaseCameraScript(callback: ((Boolean) -> Unit)?)

    /**
     * Execute JavaScript to check camera state
     *
     * @param callback Callback with result ("active" or "inactive")
     */
    protected abstract fun executeCheckCameraScript(callback: (String) -> Unit)

    companion object {
        /**
         * JavaScript to notify web content that camera should be released
         */
        const val JS_RELEASE_CAMERA = """
            (function() {
                if (navigator.xr && window.xrSession) {
                    window.xrSession.end();
                }
                if (window.localStream) {
                    window.localStream.getTracks().forEach(track => track.stop());
                }
                return 'released';
            })();
        """

        /**
         * JavaScript to check if camera is in use by XR session
         */
        const val JS_CHECK_CAMERA = """
            (function() {
                if (navigator.xr && window.xrSession && window.xrSession.mode === 'immersive-ar') {
                    return 'active';
                }
                return 'inactive';
            })();
        """
    }
}
