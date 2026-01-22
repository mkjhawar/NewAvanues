package com.newavanues.licensing.qrscanner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * JavaScript/Web implementation of QrScannerService.
 *
 * Uses the MediaDevices API for camera access and jsQR for QR code decoding.
 * Frame processing is done using requestAnimationFrame for smooth performance.
 *
 * Browser compatibility:
 * - Chrome 53+
 * - Firefox 36+
 * - Safari 11+
 * - Edge 12+
 *
 * Note: Camera access requires HTTPS or localhost due to browser security policies.
 */
actual class QrScannerService actual constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State flows
    private val _isScanning = MutableStateFlow(false)
    private val _isTorchEnabled = MutableStateFlow(false)
    private val _scanResults = MutableSharedFlow<QrScanResult>(replay = 0, extraBufferCapacity = 10)

    // Configuration
    private var _currentConfig: QrScannerConfig = QrScannerConfig.DEFAULT

    // Media resources
    private var mediaStream: MediaStream? = null
    private var videoElement: HTMLVideoElement? = null
    private var canvasElement: HTMLCanvasElement? = null
    private var canvasContext: CanvasRenderingContext2D? = null
    private var animationFrameId: Int? = null

    // Scanning state
    private var lastScanTime: Long = 0
    private var lastScannedContent: String? = null
    private var permissionGranted: Boolean? = null
    private var currentFacingMode: CameraFacing = CameraFacing.BACK

    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    actual val scanResults: Flow<QrScanResult> = _scanResults.asSharedFlow()

    actual val currentConfig: QrScannerConfig
        get() = _currentConfig

    actual val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled.asStateFlow()

    /**
     * Start the QR code scanning process.
     *
     * This will:
     * 1. Request camera permission via browser prompt
     * 2. Initialize video stream
     * 3. Start frame processing loop
     */
    actual suspend fun startScanning(config: QrScannerConfig) {
        if (_isScanning.value) {
            emitResult(QrScanResult.Error(QrScanError.ALREADY_SCANNING))
            return
        }

        _currentConfig = config
        currentFacingMode = config.cameraFacing

        // Check if MediaDevices API is available
        if (navigator.mediaDevices == null) {
            emitResult(QrScanResult.NoCameraAvailable)
            return
        }

        try {
            // Request camera access
            val constraints = createMediaConstraints(
                facingMode = if (currentFacingMode == CameraFacing.FRONT) "user" else "environment",
                width = config.resolution.width,
                height = config.resolution.height
            )

            mediaStream = awaitPromise(navigator.mediaDevices!!.getUserMedia(constraints))
            permissionGranted = true

            // Check if we got any video tracks
            val videoTracks = mediaStream!!.getVideoTracks()
            if (videoTracks.isEmpty()) {
                releaseMediaStream()
                emitResult(QrScanResult.NoCameraAvailable)
                return
            }

            // Create video element and start playback
            videoElement = createVideoElement()
            videoElement!!.srcObject = mediaStream

            awaitPromise(videoElement!!.play())

            // Wait for video to be ready
            waitForVideoReady()

            // Create canvas for frame processing
            val video = videoElement!!
            canvasElement = createCanvasElement(video.videoWidth, video.videoHeight)
            canvasContext = canvasElement!!.getContext("2d").unsafeCast<CanvasRenderingContext2D>()

            // Start scanning
            _isScanning.value = true
            lastScanTime = 0
            lastScannedContent = null

            // Start the frame processing loop
            startFrameProcessing()

        } catch (e: Exception) {
            releaseResources()
            val errorMessage = e.message ?: "Unknown error"

            // Check for permission denied
            if (errorMessage.contains("NotAllowedError") ||
                errorMessage.contains("Permission denied") ||
                errorMessage.contains("not allowed")) {
                permissionGranted = false
                emitResult(QrScanResult.PermissionDenied)
            } else if (errorMessage.contains("NotFoundError") ||
                       errorMessage.contains("Requested device not found")) {
                emitResult(QrScanResult.NoCameraAvailable)
            } else {
                emitResult(QrScanResult.Error(
                    QrScanError.CAMERA_INIT_FAILED,
                    errorMessage
                ))
            }
        }
    }

    /**
     * Stop the scanning process and release camera resources.
     */
    actual suspend fun stopScanning() {
        if (!_isScanning.value) return

        _isScanning.value = false
        stopFrameProcessing()
        releaseResources()
    }

    /**
     * Process a static image for QR codes.
     *
     * Useful for image upload functionality in web apps.
     *
     * @param imageData Raw image bytes (PNG, JPEG)
     * @return QrScanResult with decoded content or error
     */
    actual fun processImage(imageData: ByteArray): QrScanResult {
        if (imageData.isEmpty()) {
            return QrScanResult.Error(QrScanError.INVALID_IMAGE, "Empty image data")
        }

        return try {
            // For static image processing, we would need to:
            // 1. Create an Image element
            // 2. Load the image data
            // 3. Draw to canvas
            // 4. Get ImageData and run jsQR
            //
            // This is a simplified implementation that assumes the image
            // is already decoded. In production, you'd want to handle
            // various image formats and sizes.

            // Convert ByteArray to Uint8ClampedArray
            // Note: This assumes the imageData is raw RGBA pixel data
            // For actual image files (PNG/JPEG), you'd need to decode first
            val uint8Array = imageData.toUint8ClampedArray()

            // Attempt to decode (assuming square image for simplicity)
            val size = kotlin.math.sqrt(imageData.size / 4.0).toInt()
            if (size <= 0) {
                return QrScanResult.Error(QrScanError.INVALID_IMAGE, "Invalid image dimensions")
            }

            val code = jsQR(uint8Array, size, size)

            if (code != null) {
                if (_currentConfig.acceptsFormat(QrFormat.QR_CODE)) {
                    QrScanResult.Success(
                        content = code.data,
                        format = QrFormat.QR_CODE
                    )
                } else {
                    QrScanResult.Error(QrScanError.UNSUPPORTED_FORMAT)
                }
            } else {
                QrScanResult.Error(QrScanError.NO_CODE_FOUND)
            }
        } catch (e: Exception) {
            QrScanResult.Error(QrScanError.PROCESSING_ERROR, e.message)
        }
    }

    /**
     * Toggle the torch/flashlight.
     *
     * Note: Torch support in browsers is limited and may not work on all devices.
     * It requires the ImageCapture API which has limited browser support.
     */
    actual suspend fun toggleTorch(): Boolean {
        if (!_isScanning.value || mediaStream == null) {
            return false
        }

        return try {
            val videoTrack = mediaStream!!.getVideoTracks().firstOrNull() ?: return false
            val capabilities = videoTrack.getCapabilities()

            // Check if torch is supported
            if (capabilities.torch != true) {
                return false
            }

            val newTorchState = !_isTorchEnabled.value
            val constraints = js("{}")
            val advancedArray = js("[]")
            val torchConstraint = js("{}")
            torchConstraint.torch = newTorchState
            advancedArray.push(torchConstraint)
            constraints.advanced = advancedArray

            awaitPromise(videoTrack.applyConstraints(constraints))
            _isTorchEnabled.value = newTorchState
            newTorchState
        } catch (e: Exception) {
            console.log("Torch toggle failed: ${e.message}")
            false
        }
    }

    /**
     * Switch between front and back camera.
     */
    actual suspend fun switchCamera() {
        if (!_isScanning.value) return

        // Stop current stream
        val wasScanning = _isScanning.value
        val currentConfig = _currentConfig
        stopScanning()

        // Toggle facing mode
        currentFacingMode = currentFacingMode.toggle()

        // Restart with new facing mode
        if (wasScanning) {
            startScanning(currentConfig.copy(cameraFacing = currentFacingMode))
        }
    }

    /**
     * Check if camera permission has been granted.
     */
    actual fun hasPermission(): Boolean {
        return permissionGranted == true
    }

    /**
     * Request camera permission.
     *
     * In browsers, this is handled by getUserMedia which shows a permission prompt.
     * The actual permission state is determined when starting the scanner.
     */
    actual suspend fun requestPermission(): Boolean {
        if (permissionGranted == true) return true

        // Check if MediaDevices API is available
        if (navigator.mediaDevices == null) {
            permissionGranted = false
            return false
        }

        return try {
            // Request permission by calling getUserMedia briefly
            val constraints = createMediaConstraints()
            val stream = awaitPromise(navigator.mediaDevices!!.getUserMedia(constraints))

            // Permission granted, stop the stream immediately
            stream.getTracks().forEach { it.stop() }
            permissionGranted = true
            true
        } catch (e: Exception) {
            permissionGranted = false
            false
        }
    }

    /**
     * Release all resources held by the scanner.
     */
    actual fun release() {
        scope.launch {
            stopScanning()
        }
        scope.cancel()
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * Start the frame processing loop using requestAnimationFrame.
     */
    private fun startFrameProcessing() {
        processFrame()
    }

    /**
     * Stop the frame processing loop.
     */
    private fun stopFrameProcessing() {
        animationFrameId?.let { window.cancelAnimationFrame(it) }
        animationFrameId = null
    }

    /**
     * Process a single video frame for QR codes.
     */
    private fun processFrame() {
        if (!_isScanning.value) return

        val video = videoElement ?: return
        val canvas = canvasElement ?: return
        val ctx = canvasContext ?: return

        // Check if video has data
        if (video.readyState >= 2) { // HAVE_CURRENT_DATA or better
            // Update canvas size if needed
            if (canvas.width != video.videoWidth || canvas.height != video.videoHeight) {
                canvas.width = video.videoWidth
                canvas.height = video.videoHeight
            }

            // Draw video frame to canvas
            ctx.drawImage(video.asDynamic(), 0, 0, canvas.width, canvas.height)

            // Get image data
            val imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)

            // Attempt to decode QR code
            val code = jsQR(imageData.data, imageData.width, imageData.height)

            if (code != null) {
                handleDetectedCode(code)
            }
        }

        // Schedule next frame
        animationFrameId = window.requestAnimationFrame { processFrame() }
    }

    /**
     * Handle a detected QR code.
     */
    private fun handleDetectedCode(code: JsQRCode) {
        val content = code.data
        val currentTime = currentTimeMillis()

        // Check scan delay to prevent duplicate scans
        if (_currentConfig.scanMode == ScanMode.CONTINUOUS) {
            if (content == lastScannedContent &&
                currentTime - lastScanTime < _currentConfig.scanDelayMs) {
                return
            }
        }

        lastScanTime = currentTime
        lastScannedContent = content

        // Check if format is accepted
        if (!_currentConfig.acceptsFormat(QrFormat.QR_CODE)) {
            emitResult(QrScanResult.Error(QrScanError.UNSUPPORTED_FORMAT))
            return
        }

        // Emit success result
        val result = QrScanResult.Success(
            content = content,
            format = QrFormat.QR_CODE
        )
        emitResult(result)

        // Auto-stop on success in single scan mode
        if (_currentConfig.scanMode == ScanMode.SINGLE && _currentConfig.autoStopOnSuccess) {
            scope.launch {
                stopScanning()
            }
        }
    }

    /**
     * Emit a scan result to the shared flow.
     */
    private fun emitResult(result: QrScanResult) {
        scope.launch {
            _scanResults.emit(result)
        }
    }

    /**
     * Wait for video element to have data ready.
     */
    private suspend fun waitForVideoReady() {
        val video = videoElement ?: return

        // Poll until video is ready
        var attempts = 0
        while (video.videoWidth == 0 || video.videoHeight == 0) {
            if (attempts++ > 100) {
                throw Exception("Video stream failed to initialize")
            }
            delay(50)
        }
    }

    /**
     * Simple coroutine delay implementation.
     */
    private suspend fun delay(ms: Int) {
        suspendCoroutine<Unit> { continuation ->
            js("setTimeout")(
                { continuation.resume(Unit) },
                ms
            )
        }
    }

    /**
     * Release media stream resources.
     */
    private fun releaseMediaStream() {
        mediaStream?.getTracks()?.forEach { track ->
            track.stop()
        }
        mediaStream = null
    }

    /**
     * Release all media resources.
     */
    private fun releaseResources() {
        stopFrameProcessing()
        releaseMediaStream()

        videoElement?.srcObject = null
        videoElement = null
        canvasElement = null
        canvasContext = null

        _isTorchEnabled.value = false
    }

    /**
     * Await a JavaScript Promise.
     */
    private suspend fun <T> awaitPromise(promise: kotlin.js.Promise<T>): T {
        return suspendCoroutine { continuation ->
            promise.then(
                onFulfilled = { value ->
                    continuation.resume(value)
                    Unit.asDynamic()
                },
                onRejected = { error ->
                    val errorMessage = error.asDynamic().message as? String
                        ?: error.toString()
                    continuation.resumeWithException(Exception(errorMessage))
                    Unit.asDynamic()
                }
            )
        }
    }

    /**
     * Convert ByteArray to Uint8ClampedArray.
     */
    private fun ByteArray.toUint8ClampedArray(): Uint8ClampedArray {
        val uint8Array = Uint8Array(size)
        for (i in indices) {
            uint8Array.asDynamic()[i] = this[i]
        }
        // Create Uint8ClampedArray from the Uint8Array buffer
        return js("new Uint8ClampedArray")(uint8Array.buffer).unsafeCast<Uint8ClampedArray>()
    }
}

/**
 * External console for logging.
 */
private external object console {
    fun log(message: String)
    fun error(message: String)
    fun warn(message: String)
}
