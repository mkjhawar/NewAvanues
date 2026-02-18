package com.augmentalis.photoavanue

import com.augmentalis.photoavanue.model.ExtensionMode
import com.augmentalis.photoavanue.model.StabilizationMode
import com.augmentalis.photoavanue.model.WhiteBalanceMode

/**
 * Pro camera controller with manual controls and CameraX Extensions.
 *
 * Tier 2 controller extending [ICameraController] with:
 * - CameraX Extensions (Bokeh, HDR, Night, FaceRetouch)
 * - Manual ISO, shutter speed, focus distance
 * - White balance presets
 * - RAW capture toggle
 * - Stabilization modes
 *
 * Platform implementations:
 * - Android: Camera2 interop via CameraX + ExtensionsManager
 * - iOS: AVCaptureDevice manual controls (future)
 * - Desktop: Stub/unsupported (future)
 */
interface IProCameraController : ICameraController {

    // ── Extensions ────────────────────────────────────────────────────
    /** Set active camera extension mode. Triggers camera rebind. */
    fun setExtensionMode(mode: ExtensionMode)

    // ── Pro Mode ──────────────────────────────────────────────────────
    /** Enable/disable pro (manual) mode. When disabled, auto mode resumes. */
    fun setProMode(enabled: Boolean)

    // ── ISO ───────────────────────────────────────────────────────────
    /** Set ISO value directly. Only effective in pro mode. */
    fun setIso(value: Int)
    /** Lock/unlock ISO at current value. */
    fun lockIso(locked: Boolean)

    // ── Shutter Speed ─────────────────────────────────────────────────
    /** Set shutter speed in nanoseconds. Only effective in pro mode. */
    fun setShutterSpeed(nanos: Long)
    /** Lock/unlock shutter speed at current value. */
    fun lockShutter(locked: Boolean)

    // ── Focus ─────────────────────────────────────────────────────────
    /** Set focus distance in diopters (0.0 = infinity). Only effective in pro mode. */
    fun setFocusDistance(diopters: Float)
    /** Lock/unlock focus at current position. */
    fun lockFocus(locked: Boolean)

    // ── White Balance ─────────────────────────────────────────────────
    fun setWhiteBalance(mode: WhiteBalanceMode)
    fun lockWhiteBalance(locked: Boolean)

    // ── RAW ───────────────────────────────────────────────────────────
    /** Enable/disable RAW (DNG) capture alongside JPEG. */
    fun setRawCapture(enabled: Boolean)

    // ── Stabilization ─────────────────────────────────────────────────
    fun setStabilization(mode: StabilizationMode)
}
