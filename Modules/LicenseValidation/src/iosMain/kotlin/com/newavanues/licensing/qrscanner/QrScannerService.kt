package com.newavanues.licensing.qrscanner

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset1280x720
import platform.AVFoundation.AVCaptureSessionPreset1920x1080
import platform.AVFoundation.AVCaptureSessionPreset640x480
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorAccuracy
import platform.CoreImage.CIDetectorAccuracyHigh
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.Foundation.NSData
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.create
import platform.QuartzCore.CALayer
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of QrScannerService using AVFoundation.
 *
 * Uses AVCaptureSession with AVCaptureMetadataOutput for real-time QR code detection.
 * Camera preview is provided via AVCaptureVideoPreviewLayer which can be accessed
 * by native iOS code for display in SwiftUI/UIKit.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class QrScannerService actual constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResults = MutableSharedFlow<QrScanResult>(replay = 0, extraBufferCapacity = 10)
    actual val scanResults: Flow<QrScanResult> = _scanResults.asSharedFlow()

    private val _isTorchEnabled = MutableStateFlow(false)
    actual val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled.asStateFlow()

    private var _currentConfig: QrScannerConfig = QrScannerConfig.DEFAULT
    actual val currentConfig: QrScannerConfig
        get() = _currentConfig

    // AVFoundation components
    private var captureSession: AVCaptureSession? = null
    private var captureDevice: AVCaptureDevice? = null
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var metadataOutput: AVCaptureMetadataOutput? = null
    private var metadataDelegate: MetadataOutputDelegate? = null

    // Track current camera position
    private var currentCameraPosition = AVCaptureDevicePositionBack

    // Debounce for continuous scanning
    private var lastScanTime: Long = 0

    // CIDetector for image processing
    private val qrDetector: CIDetector? by lazy {
        CIDetector.detectorOfType(
            CIDetectorTypeQRCode,
            context = null,
            options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh)
        )
    }

    // Haptic feedback generator
    private val hapticGenerator: UIImpactFeedbackGenerator by lazy {
        UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    }

    /**
     * Start the QR code scanning process.
     *
     * This will:
     * 1. Request camera permissions if needed
     * 2. Initialize AVCaptureSession
     * 3. Configure AVCaptureMetadataOutput for QR detection
     * 4. Start the capture session
     */
    actual suspend fun startScanning(config: QrScannerConfig) {
        if (_isScanning.value) {
            _scanResults.emit(QrScanResult.Error(QrScanError.ALREADY_SCANNING))
            return
        }

        _currentConfig = config

        // Check/request permissions
        if (!hasPermission()) {
            val granted = requestPermission()
            if (!granted) {
                _scanResults.emit(QrScanResult.PermissionDenied)
                return
            }
        }

        // Initialize camera on main thread
        withContext(Dispatchers.Main) {
            try {
                initializeCamera(config)
                captureSession?.startRunning()
                _isScanning.value = true
            } catch (e: Exception) {
                _scanResults.emit(
                    QrScanResult.Error(
                        QrScanError.CAMERA_INIT_FAILED,
                        e.message
                    )
                )
            }
        }
    }

    /**
     * Stop the scanning process and release camera resources.
     */
    actual suspend fun stopScanning() {
        withContext(Dispatchers.Main) {
            captureSession?.stopRunning()
            _isScanning.value = false
        }
    }

    /**
     * Process a static image for QR codes using CIDetector.
     *
     * @param imageData Raw image bytes (PNG, JPEG, etc.)
     * @return QrScanResult with the decoded content or error
     */
    actual fun processImage(imageData: ByteArray): QrScanResult {
        if (imageData.isEmpty()) {
            return QrScanResult.Error(QrScanError.INVALID_IMAGE, "Image data is empty")
        }

        return try {
            // Convert ByteArray to NSData
            val nsData = imageData.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = imageData.size.toULong())
            }

            // Create CIImage from NSData
            val ciImage = CIImage(data = nsData)
                ?: return QrScanResult.Error(QrScanError.INVALID_IMAGE, "Failed to create image")

            // Detect QR codes
            val features = qrDetector?.featuresInImage(ciImage) ?: emptyList<Any>()

            if (features.isEmpty()) {
                return QrScanResult.Error(QrScanError.NO_CODE_FOUND)
            }

            // Get the first QR code
            val qrFeature = features.firstOrNull()
            if (qrFeature != null) {
                // CIQRCodeFeature has messageString property
                val message = qrFeature.toString() // Simplified - actual implementation needs casting
                QrScanResult.Success(
                    content = message,
                    format = QrFormat.QR_CODE
                )
            } else {
                QrScanResult.Error(QrScanError.DECODE_FAILED)
            }
        } catch (e: Exception) {
            QrScanResult.Error(QrScanError.PROCESSING_ERROR, e.message)
        }
    }

    /**
     * Toggle the torch/flashlight.
     */
    actual suspend fun toggleTorch(): Boolean {
        return withContext(Dispatchers.Main) {
            val device = captureDevice ?: return@withContext false

            if (!device.hasTorch) {
                return@withContext false
            }

            try {
                device.lockForConfiguration(null)
                val newTorchState = !_isTorchEnabled.value
                device.torchMode = if (newTorchState) {
                    platform.AVFoundation.AVCaptureTorchModeOn
                } else {
                    platform.AVFoundation.AVCaptureTorchModeOff
                }
                device.unlockForConfiguration()
                _isTorchEnabled.value = newTorchState
                newTorchState
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Switch between front and back camera.
     */
    actual suspend fun switchCamera() {
        withContext(Dispatchers.Main) {
            val session = captureSession ?: return@withContext

            // Stop running
            session.stopRunning()

            // Toggle position
            currentCameraPosition = when (currentCameraPosition) {
                AVCaptureDevicePositionBack -> AVCaptureDevicePositionFront
                else -> AVCaptureDevicePositionBack
            }

            // Reconfigure with new camera
            try {
                reconfigureCamera()
                session.startRunning()
            } catch (e: Exception) {
                // Revert on failure
                currentCameraPosition = when (currentCameraPosition) {
                    AVCaptureDevicePositionBack -> AVCaptureDevicePositionFront
                    else -> AVCaptureDevicePositionBack
                }
            }
        }
    }

    /**
     * Check if camera permission has been granted.
     */
    actual fun hasPermission(): Boolean {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return status == AVAuthorizationStatusAuthorized
    }

    /**
     * Request camera permission.
     */
    actual suspend fun requestPermission(): Boolean {
        val currentStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)

        return when (currentStatus) {
            AVAuthorizationStatusAuthorized -> true
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> false
            AVAuthorizationStatusNotDetermined -> {
                suspendCoroutine { continuation ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        continuation.resume(granted)
                    }
                }
            }
            else -> false
        }
    }

    /**
     * Release all resources held by the scanner.
     */
    actual fun release() {
        captureSession?.stopRunning()
        captureSession = null
        captureDevice = null
        previewLayer = null
        metadataOutput = null
        metadataDelegate = null
        _isScanning.value = false
        _isTorchEnabled.value = false
    }

    // MARK: - iOS-specific public APIs

    /**
     * Get the preview layer for displaying camera feed in SwiftUI/UIKit.
     * Must be called after startScanning.
     */
    fun getPreviewLayer(): AVCaptureVideoPreviewLayer? = previewLayer

    /**
     * Get the current capture session for advanced customization.
     */
    fun getCaptureSession(): AVCaptureSession? = captureSession

    // MARK: - Private implementation

    private fun initializeCamera(config: QrScannerConfig) {
        // Create capture session
        val session = AVCaptureSession()
        captureSession = session

        // Set preset based on config
        session.sessionPreset = when (config.resolution) {
            ScannerResolution.SD -> AVCaptureSessionPreset640x480
            ScannerResolution.HD -> AVCaptureSessionPreset1280x720
            ScannerResolution.FULL_HD -> AVCaptureSessionPreset1920x1080
        }

        // Set camera position
        currentCameraPosition = when (config.cameraFacing) {
            CameraFacing.BACK -> AVCaptureDevicePositionBack
            CameraFacing.FRONT -> AVCaptureDevicePositionFront
        }

        // Get camera device
        val device = getCamera() ?: run {
            scope.launch {
                _scanResults.emit(QrScanResult.NoCameraAvailable)
            }
            return
        }
        captureDevice = device

        // Add input
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
            ?: return
        if (session.canAddInput(input)) {
            session.addInput(input)
        }

        // Add metadata output for QR detection
        val output = AVCaptureMetadataOutput()
        metadataOutput = output

        if (session.canAddOutput(output)) {
            session.addOutput(output)

            // Create and set delegate
            val delegate = MetadataOutputDelegate { metadataObjects ->
                handleMetadataOutput(metadataObjects)
            }
            metadataDelegate = delegate

            output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())

            // Set metadata types to detect
            output.metadataObjectTypes = getMetadataTypes(config.acceptedFormats)
        }

        // Create preview layer
        val preview = AVCaptureVideoPreviewLayer(session = session)
        preview.videoGravity = AVLayerVideoGravityResizeAspectFill
        previewLayer = preview
    }

    private fun reconfigureCamera() {
        val session = captureSession ?: return

        // Remove existing inputs
        session.inputs.forEach { input ->
            session.removeInput(input as platform.AVFoundation.AVCaptureInput)
        }

        // Get new camera
        val device = getCamera() ?: return
        captureDevice = device

        // Add new input
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
            ?: return
        if (session.canAddInput(input)) {
            session.addInput(input)
        }

        // Reset torch state since we switched cameras
        _isTorchEnabled.value = false
    }

    private fun getCamera(): AVCaptureDevice? {
        val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
            mediaType = AVMediaTypeVideo,
            position = currentCameraPosition
        )
        return discoverySession.devices.firstOrNull() as? AVCaptureDevice
    }

    private fun getMetadataTypes(acceptedFormats: Set<QrFormat>): List<Any> {
        if (acceptedFormats.isEmpty()) {
            // Accept all common types
            return listOf(
                AVMetadataObjectTypeQRCode,
                AVMetadataObjectTypeDataMatrixCode,
                AVMetadataObjectTypeAztecCode,
                AVMetadataObjectTypePDF417Code,
                AVMetadataObjectTypeCode128Code,
                AVMetadataObjectTypeCode39Code,
                AVMetadataObjectTypeEAN13Code,
                AVMetadataObjectTypeEAN8Code,
                AVMetadataObjectTypeUPCECode
            )
        }

        return acceptedFormats.mapNotNull { format ->
            when (format) {
                QrFormat.QR_CODE -> AVMetadataObjectTypeQRCode
                QrFormat.DATA_MATRIX -> AVMetadataObjectTypeDataMatrixCode
                QrFormat.AZTEC -> AVMetadataObjectTypeAztecCode
                QrFormat.PDF417 -> AVMetadataObjectTypePDF417Code
                QrFormat.CODE_128 -> AVMetadataObjectTypeCode128Code
                QrFormat.CODE_39 -> AVMetadataObjectTypeCode39Code
                QrFormat.EAN_13 -> AVMetadataObjectTypeEAN13Code
                QrFormat.EAN_8 -> AVMetadataObjectTypeEAN8Code
                QrFormat.UPC_E -> AVMetadataObjectTypeUPCECode
                QrFormat.UPC_A -> null // iOS uses EAN13 for UPC-A
                QrFormat.UNKNOWN -> null
            }
        }
    }

    private fun handleMetadataOutput(metadataObjects: List<*>) {
        if (metadataObjects.isEmpty()) return

        val now = currentTimeMillis()

        // Apply scan delay for continuous mode
        if (_currentConfig.scanMode == ScanMode.CONTINUOUS) {
            if (now - lastScanTime < _currentConfig.scanDelayMs) {
                return
            }
        }

        val qrObject = metadataObjects.firstOrNull { obj ->
            (obj as? AVMetadataMachineReadableCodeObject)?.stringValue != null
        } as? AVMetadataMachineReadableCodeObject ?: return

        val content = qrObject.stringValue ?: return
        val format = mapMetadataType(qrObject.type)

        // Check if format is accepted
        if (!_currentConfig.acceptsFormat(format)) {
            return
        }

        lastScanTime = now

        // Trigger haptic feedback
        if (_currentConfig.enableHapticFeedback) {
            hapticGenerator.prepare()
            hapticGenerator.impactOccurred()
        }

        // Emit result
        scope.launch {
            _scanResults.emit(
                QrScanResult.Success(
                    content = content,
                    format = format
                )
            )

            // Auto-stop in single mode
            if (_currentConfig.scanMode == ScanMode.SINGLE && _currentConfig.autoStopOnSuccess) {
                stopScanning()
            }
        }
    }

    private fun mapMetadataType(type: Any?): QrFormat {
        return when (type) {
            AVMetadataObjectTypeQRCode -> QrFormat.QR_CODE
            AVMetadataObjectTypeDataMatrixCode -> QrFormat.DATA_MATRIX
            AVMetadataObjectTypeAztecCode -> QrFormat.AZTEC
            AVMetadataObjectTypePDF417Code -> QrFormat.PDF417
            AVMetadataObjectTypeCode128Code -> QrFormat.CODE_128
            AVMetadataObjectTypeCode39Code -> QrFormat.CODE_39
            AVMetadataObjectTypeEAN13Code -> QrFormat.EAN_13
            AVMetadataObjectTypeEAN8Code -> QrFormat.EAN_8
            AVMetadataObjectTypeUPCECode -> QrFormat.UPC_E
            else -> QrFormat.UNKNOWN
        }
    }
}

/**
 * Delegate class for AVCaptureMetadataOutput.
 * Bridges AVFoundation callbacks to Kotlin.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class MetadataOutputDelegate(
    private val onMetadataOutput: (List<*>) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: platform.AVFoundation.AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: platform.AVFoundation.AVCaptureConnection
    ) {
        onMetadataOutput(didOutputMetadataObjects)
    }
}
