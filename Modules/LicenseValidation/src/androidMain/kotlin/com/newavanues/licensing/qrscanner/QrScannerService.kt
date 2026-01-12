package com.newavanues.licensing.qrscanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android implementation of QrScannerService using CameraX and ML Kit.
 *
 * This implementation provides:
 * - Real-time QR code scanning via CameraX camera preview
 * - ML Kit barcode detection for accurate QR code reading
 * - Torch/flashlight control
 * - Front/back camera switching
 * - Static image processing for QR codes
 *
 * Usage in Android:
 * ```kotlin
 * val scanner = QrScannerService()
 *
 * // Initialize with context (required before scanning)
 * scanner.initialize(context, lifecycleOwner, previewView)
 *
 * // Start scanning
 * scanner.startScanning()
 *
 * // Collect results
 * scanner.scanResults.collect { result ->
 *     when (result) {
 *         is QrScanResult.Success -> handleCode(result.content)
 *         is QrScanResult.Error -> showError(result.error)
 *         // ...
 *     }
 * }
 * ```
 */
actual class QrScannerService actual constructor() {

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResults = MutableSharedFlow<QrScanResult>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    actual val scanResults: Flow<QrScanResult> = _scanResults.asSharedFlow()

    private var _currentConfig = QrScannerConfig.DEFAULT
    actual val currentConfig: QrScannerConfig get() = _currentConfig

    private val _isTorchEnabled = MutableStateFlow(false)
    actual val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled.asStateFlow()

    // Android-specific properties
    private var context: Context? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentLensFacing = CameraSelector.LENS_FACING_BACK

    private val barcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_PDF417
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mutex = Mutex()
    private var lastScanTime = 0L
    private var isInitialized = false

    /**
     * Initialize the scanner with Android-specific dependencies.
     *
     * This must be called before [startScanning] on Android.
     *
     * @param context Android context
     * @param lifecycleOwner Lifecycle owner for camera binding
     * @param previewView Optional preview view for camera preview
     */
    fun initialize(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null
    ) {
        this.context = context.applicationContext
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
        isInitialized = true
    }

    /**
     * Set the preview view for camera preview.
     *
     * Can be called after initialization to update the preview surface.
     *
     * @param previewView The PreviewView to display camera preview
     */
    fun setPreviewView(previewView: PreviewView?) {
        this.previewView = previewView
        if (isInitialized && _isScanning.value) {
            // Re-bind camera with new preview if already scanning
            scope.launch {
                rebindCamera()
            }
        }
    }

    actual suspend fun startScanning(config: QrScannerConfig) {
        mutex.withLock {
            if (_isScanning.value) {
                _scanResults.emit(QrScanResult.Error(QrScanError.ALREADY_SCANNING))
                return
            }

            val ctx = context
            val lifecycle = lifecycleOwner

            if (ctx == null || lifecycle == null) {
                _scanResults.emit(
                    QrScanResult.Error(
                        QrScanError.NOT_INITIALIZED,
                        "Scanner not initialized. Call initialize() first."
                    )
                )
                return
            }

            if (!hasPermission()) {
                _scanResults.emit(QrScanResult.PermissionDenied)
                return
            }

            _currentConfig = config
            cameraSelector = if (config.cameraFacing == CameraFacing.FRONT) {
                currentLensFacing = CameraSelector.LENS_FACING_FRONT
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                currentLensFacing = CameraSelector.LENS_FACING_BACK
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                setupCamera(ctx, lifecycle)
                _isScanning.value = true
            } catch (e: Exception) {
                _scanResults.emit(
                    QrScanResult.Error(
                        QrScanError.CAMERA_INIT_FAILED,
                        e.message ?: "Failed to initialize camera"
                    )
                )
            }
        }
    }

    actual suspend fun stopScanning() {
        mutex.withLock {
            if (!_isScanning.value) return

            _isScanning.value = false
            _isTorchEnabled.value = false

            cameraProvider?.unbindAll()
            camera = null
            preview = null
            imageAnalysis = null
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    actual fun processImage(imageData: ByteArray): QrScanResult {
        val ctx = context ?: return QrScanResult.Error(
            QrScanError.NOT_INITIALIZED,
            "Scanner not initialized"
        )

        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return QrScanResult.Error(QrScanError.INVALID_IMAGE, "Failed to decode image")

            val image = InputImage.fromBitmap(bitmap, 0)

            // Process synchronously for this method
            var result: QrScanResult = QrScanResult.Error(QrScanError.NO_CODE_FOUND)

            val task = barcodeScanner.process(image)
            // Block and wait for result (used for static image processing)
            while (!task.isComplete) {
                Thread.sleep(10)
            }

            if (task.isSuccessful) {
                val barcodes = task.result
                barcodes?.firstOrNull { isAcceptedFormat(it) }?.let { barcode ->
                    barcode.rawValue?.let { value ->
                        result = QrScanResult.Success(
                            content = value,
                            format = mapBarcodeFormat(barcode.format),
                            timestamp = currentTimeMillis()
                        )
                    }
                }
            } else {
                task.exception?.let { e ->
                    result = QrScanResult.Error(
                        QrScanError.PROCESSING_ERROR,
                        e.message
                    )
                }
            }

            result
        } catch (e: Exception) {
            QrScanResult.Error(
                QrScanError.PROCESSING_ERROR,
                e.message ?: "Failed to process image"
            )
        }
    }

    actual suspend fun toggleTorch(): Boolean {
        val cam = camera ?: return false

        if (!cam.cameraInfo.hasFlashUnit()) {
            return false
        }

        val newState = !_isTorchEnabled.value
        cam.cameraControl.enableTorch(newState).await()
        _isTorchEnabled.value = newState

        return newState
    }

    actual suspend fun switchCamera() {
        mutex.withLock {
            if (!_isScanning.value) return

            val ctx = context ?: return
            val lifecycle = lifecycleOwner ?: return

            // Toggle camera facing
            currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(currentLensFacing)
                .build()

            // Disable torch when switching cameras
            _isTorchEnabled.value = false

            try {
                rebindCamera()
            } catch (e: Exception) {
                _scanResults.emit(
                    QrScanResult.Error(
                        QrScanError.CAMERA_ERROR,
                        "Failed to switch camera: ${e.message}"
                    )
                )
            }
        }
    }

    actual fun hasPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun requestPermission(): Boolean {
        // Permission requesting must be done from Activity level
        // This method returns the current permission state
        // The UI layer should use Accompanist permissions to handle the request
        return hasPermission()
    }

    actual fun release() {
        scope.launch {
            stopScanning()
        }
        cameraExecutor.shutdown()
        barcodeScanner.close()
        scope.cancel()

        context = null
        lifecycleOwner = null
        previewView = null
        cameraProvider = null
        isInitialized = false
    }

    // ==================== Private Methods ====================

    private suspend fun setupCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        cameraProvider = ProcessCameraProvider.getInstance(context).await()

        rebindCamera()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private suspend fun rebindCamera() {
        val provider = cameraProvider ?: return
        val lifecycle = lifecycleOwner ?: return

        withContext(Dispatchers.Main) {
            provider.unbindAll()

            // Setup preview
            preview = Preview.Builder()
                .setTargetResolution(
                    android.util.Size(
                        _currentConfig.resolution.width,
                        _currentConfig.resolution.height
                    )
                )
                .build()
                .also { preview ->
                    previewView?.let { view ->
                        preview.setSurfaceProvider(view.surfaceProvider)
                    }
                }

            // Setup image analysis for barcode scanning
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(
                    android.util.Size(
                        _currentConfig.resolution.width,
                        _currentConfig.resolution.height
                    )
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processFrame(imageProxy)
                    }
                }

            try {
                camera = provider.bindToLifecycle(
                    lifecycle,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                scope.launch {
                    _scanResults.emit(
                        QrScanResult.Error(
                            QrScanError.CAMERA_INIT_FAILED,
                            e.message ?: "Failed to bind camera"
                        )
                    )
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processFrame(imageProxy: ImageProxy) {
        if (!_isScanning.value) {
            imageProxy.close()
            return
        }

        // Apply scan delay in continuous mode
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < _currentConfig.scanDelayMs) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { isAcceptedFormat(it) }?.let { barcode ->
                    barcode.rawValue?.let { value ->
                        lastScanTime = currentTime

                        scope.launch {
                            val result = QrScanResult.Success(
                                content = value,
                                format = mapBarcodeFormat(barcode.format),
                                timestamp = currentTimeMillis()
                            )
                            _scanResults.emit(result)

                            // Auto-stop in SINGLE mode
                            if (_currentConfig.scanMode == ScanMode.SINGLE &&
                                _currentConfig.autoStopOnSuccess
                            ) {
                                stopScanning()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                scope.launch {
                    _scanResults.emit(
                        QrScanResult.Error(
                            QrScanError.PROCESSING_ERROR,
                            e.message
                        )
                    )
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun isAcceptedFormat(barcode: Barcode): Boolean {
        val format = mapBarcodeFormat(barcode.format)
        return _currentConfig.acceptsFormat(format)
    }

    private fun mapBarcodeFormat(mlKitFormat: Int): QrFormat {
        return when (mlKitFormat) {
            Barcode.FORMAT_QR_CODE -> QrFormat.QR_CODE
            Barcode.FORMAT_DATA_MATRIX -> QrFormat.DATA_MATRIX
            Barcode.FORMAT_AZTEC -> QrFormat.AZTEC
            Barcode.FORMAT_PDF417 -> QrFormat.PDF417
            Barcode.FORMAT_CODE_128 -> QrFormat.CODE_128
            Barcode.FORMAT_CODE_39 -> QrFormat.CODE_39
            Barcode.FORMAT_EAN_13 -> QrFormat.EAN_13
            Barcode.FORMAT_EAN_8 -> QrFormat.EAN_8
            Barcode.FORMAT_UPC_A -> QrFormat.UPC_A
            Barcode.FORMAT_UPC_E -> QrFormat.UPC_E
            else -> QrFormat.UNKNOWN
        }
    }
}
