package com.augmentalis.photoavanue

import com.augmentalis.photoavanue.model.CameraState
import com.augmentalis.photoavanue.model.CaptureMode
import com.augmentalis.photoavanue.model.FlashMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic camera controller interface.
 *
 * Implementations provide CameraX (Android), AVCaptureSession (iOS),
 * or stub (Desktop) backends. UI composables observe [state] reactively
 * and call methods to drive camera operations.
 */
interface ICameraController {
    /** Observable camera state — UI collects this for reactive updates. */
    val state: StateFlow<CameraState>

    // ── Capture ──────────────────────────────────────────────────────
    fun capturePhoto()
    fun startRecording()
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()

    // ── Controls ─────────────────────────────────────────────────────
    fun switchLens()
    fun setCaptureMode(mode: CaptureMode)
    fun setFlashMode(mode: FlashMode)
    fun zoomIn()
    fun zoomOut()
    /** Set zoom to a discrete level (1..5). */
    fun setZoomLevel(level: Int)
    fun increaseExposure()
    fun decreaseExposure()
    /** Set exposure to a discrete level (1..5). */
    fun setExposureLevel(level: Int)

    // ── Lifecycle ────────────────────────────────────────────────────
    fun release()
}
