package com.newavanues.licensing.qrscanner

import org.khronos.webgl.Uint8ClampedArray
import kotlin.js.Promise

/**
 * External declarations for Web APIs used by the QR scanner.
 *
 * These declarations enable access to:
 * - MediaDevices API for camera access
 * - Canvas API for frame processing
 * - jsQR library for QR code decoding
 */

// ============================================================================
// Navigator and MediaDevices API
// ============================================================================

/**
 * Browser window object.
 */
external val window: Window

/**
 * Browser navigator object.
 */
external val navigator: Navigator

/**
 * Browser document object.
 */
external val document: Document

/**
 * Window interface with animation frame support.
 */
external interface Window {
    /**
     * Request an animation frame callback.
     * Used for continuous frame processing loop.
     */
    fun requestAnimationFrame(callback: (Double) -> Unit): Int

    /**
     * Cancel a pending animation frame request.
     */
    fun cancelAnimationFrame(handle: Int)
}

/**
 * Navigator interface with media devices access.
 */
external interface Navigator {
    /**
     * MediaDevices interface for camera access.
     */
    val mediaDevices: MediaDevices?
}

/**
 * MediaDevices interface for accessing media input devices.
 */
external interface MediaDevices {
    /**
     * Request access to media devices (camera, microphone).
     *
     * @param constraints Media stream constraints specifying what to request
     * @return Promise resolving to a MediaStream
     */
    fun getUserMedia(constraints: dynamic): Promise<MediaStream>

    /**
     * Enumerate available media devices.
     *
     * @return Promise resolving to array of MediaDeviceInfo
     */
    fun enumerateDevices(): Promise<Array<MediaDeviceInfo>>
}

/**
 * Information about a media device.
 */
external interface MediaDeviceInfo {
    val deviceId: String
    val groupId: String
    val kind: String // "audioinput" | "audiooutput" | "videoinput"
    val label: String
}

/**
 * MediaStream representing a stream of media content.
 */
external interface MediaStream {
    /**
     * Get all video tracks in this stream.
     */
    fun getVideoTracks(): Array<MediaStreamTrack>

    /**
     * Get all audio tracks in this stream.
     */
    fun getAudioTracks(): Array<MediaStreamTrack>

    /**
     * Get all tracks in this stream.
     */
    fun getTracks(): Array<MediaStreamTrack>
}

/**
 * MediaStreamTrack representing a single media track.
 */
external interface MediaStreamTrack {
    val id: String
    val kind: String // "audio" | "video"
    val label: String
    val enabled: Boolean
    val readyState: String // "live" | "ended"

    /**
     * Stop the track and release resources.
     */
    fun stop()

    /**
     * Get the current settings of the track.
     */
    fun getSettings(): MediaTrackSettings

    /**
     * Get the capabilities of the track.
     */
    fun getCapabilities(): MediaTrackCapabilities

    /**
     * Apply constraints to the track.
     */
    fun applyConstraints(constraints: dynamic): Promise<Unit>
}

/**
 * Settings of a media track.
 */
external interface MediaTrackSettings {
    val width: Int?
    val height: Int?
    val frameRate: Double?
    val facingMode: String?
    val deviceId: String?
}

/**
 * Capabilities of a media track.
 */
external interface MediaTrackCapabilities {
    val width: dynamic // { min: Int, max: Int }
    val height: dynamic // { min: Int, max: Int }
    val frameRate: dynamic // { min: Double, max: Double }
    val facingMode: Array<String>?
    val torch: Boolean?
}

// ============================================================================
// Document and DOM Elements
// ============================================================================

/**
 * Document interface for DOM manipulation.
 */
external interface Document {
    /**
     * Create an HTML element.
     */
    fun createElement(tagName: String): HTMLElement
}

/**
 * Base HTML element interface.
 */
external interface HTMLElement {
    var id: String
    var className: String
    var innerHTML: String
    val style: CSSStyleDeclaration
}

/**
 * CSS style declaration for element styling.
 */
external interface CSSStyleDeclaration {
    var display: String
    var position: String
    var width: String
    var height: String
}

/**
 * HTML video element for displaying video streams.
 */
external interface HTMLVideoElement : HTMLElement {
    var srcObject: MediaStream?
    val videoWidth: Int
    val videoHeight: Int
    var autoplay: Boolean
    var playsInline: Boolean
    var muted: Boolean
    val readyState: Int

    /**
     * Start playing the video.
     */
    fun play(): Promise<Unit>

    /**
     * Pause the video.
     */
    fun pause()
}

/**
 * HTML canvas element for 2D graphics.
 */
external interface HTMLCanvasElement : HTMLElement {
    var width: Int
    var height: Int

    /**
     * Get a rendering context for the canvas.
     *
     * @param contextId The context type ("2d", "webgl", etc.)
     * @return The rendering context
     */
    fun getContext(contextId: String): dynamic
}

/**
 * Canvas 2D rendering context.
 */
external interface CanvasRenderingContext2D {
    /**
     * Draw an image (or video frame) onto the canvas.
     */
    fun drawImage(image: dynamic, dx: Int, dy: Int)
    fun drawImage(image: dynamic, dx: Int, dy: Int, dWidth: Int, dHeight: Int)

    /**
     * Get the image data from a region of the canvas.
     */
    fun getImageData(sx: Int, sy: Int, sw: Int, sh: Int): ImageData

    /**
     * Clear a rectangular area.
     */
    fun clearRect(x: Int, y: Int, width: Int, height: Int)
}

/**
 * ImageData containing pixel data from a canvas.
 */
external interface ImageData {
    /**
     * One-dimensional array of pixel data in RGBA format.
     */
    val data: Uint8ClampedArray

    /**
     * Width of the image data in pixels.
     */
    val width: Int

    /**
     * Height of the image data in pixels.
     */
    val height: Int
}

// ============================================================================
// jsQR Library External Declaration
// ============================================================================

/**
 * External declaration for the jsQR library.
 *
 * jsQR is a pure JavaScript QR code reading library that works by analyzing
 * image data directly without needing any native dependencies.
 *
 * npm: https://www.npmjs.com/package/jsqr
 *
 * Usage:
 * ```kotlin
 * val code = jsQR(imageData.data, imageData.width, imageData.height, options)
 * if (code != null) {
 *     println("Found QR code: ${code.data}")
 * }
 * ```
 */
@JsModule("jsqr")
@JsNonModule
external fun jsQR(
    data: Uint8ClampedArray,
    width: Int,
    height: Int,
    options: JsQROptions? = definedExternally
): JsQRCode?

/**
 * Result from jsQR containing decoded QR code data.
 */
external interface JsQRCode {
    /**
     * The decoded string content of the QR code.
     */
    val data: String

    /**
     * Binary data if the QR code contains binary content.
     */
    val binaryData: Array<Int>

    /**
     * Location of the QR code in the image.
     */
    val location: JsQRLocation
}

/**
 * Location information of a QR code in an image.
 */
external interface JsQRLocation {
    val topLeftCorner: JsQRPoint
    val topRightCorner: JsQRPoint
    val bottomLeftCorner: JsQRPoint
    val bottomRightCorner: JsQRPoint
    val topLeftFinderPattern: JsQRPoint
    val topRightFinderPattern: JsQRPoint
    val bottomLeftFinderPattern: JsQRPoint
}

/**
 * A point with x and y coordinates.
 */
external interface JsQRPoint {
    val x: Double
    val y: Double
}

/**
 * Options for jsQR scanning.
 */
external interface JsQROptions {
    /**
     * Inversion mode for the QR code.
     * "dontInvert" | "onlyInvert" | "attemptBoth"
     */
    val inversionAttempts: String?
}

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Create jsQR options.
 */
fun createJsQROptions(inversionAttempts: String = "dontInvert"): JsQROptions {
    val options = js("{}")
    options.inversionAttempts = inversionAttempts
    return options.unsafeCast<JsQROptions>()
}

/**
 * Create video element configured for camera preview.
 */
fun createVideoElement(): HTMLVideoElement {
    val video = document.createElement("video").unsafeCast<HTMLVideoElement>()
    video.autoplay = true
    video.playsInline = true
    video.muted = true
    video.style.display = "none"
    return video
}

/**
 * Create canvas element for frame processing.
 */
fun createCanvasElement(width: Int, height: Int): HTMLCanvasElement {
    val canvas = document.createElement("canvas").unsafeCast<HTMLCanvasElement>()
    canvas.width = width
    canvas.height = height
    canvas.style.display = "none"
    return canvas
}

/**
 * Create media constraints for getUserMedia.
 *
 * @param facingMode "user" for front camera, "environment" for back camera
 * @param width Preferred video width
 * @param height Preferred video height
 */
fun createMediaConstraints(
    facingMode: String = "environment",
    width: Int = 1280,
    height: Int = 720
): dynamic {
    val constraints = js("{}")
    constraints.audio = false
    val videoConstraints = js("{}")
    videoConstraints.facingMode = facingMode
    val idealDimensions = js("{}")
    idealDimensions.ideal = width
    videoConstraints.width = idealDimensions
    val idealHeight = js("{}")
    idealHeight.ideal = height
    videoConstraints.height = idealHeight
    constraints.video = videoConstraints
    return constraints
}
