package com.newavanues.licensing.qrscanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * Desktop/JVM implementation of QrScannerService using ZXing.
 *
 * On desktop platforms, QR scanning is primarily done via image import rather than live camera.
 * The main entry point is [processImage] which decodes QR codes from image files.
 *
 * Features:
 * - QR code decoding from PNG, JPEG, GIF, BMP images
 * - Support for multiple barcode formats via ZXing
 * - No camera permissions required (file-based import)
 * - Webcam support is optional and not implemented in this version
 *
 * Usage:
 * ```kotlin
 * val scanner = QrScannerService()
 * val imageBytes = File("qrcode.png").readBytes()
 * val result = scanner.processImage(imageBytes)
 * when (result) {
 *     is QrScanResult.Success -> println("Decoded: ${result.content}")
 *     is QrScanResult.Error -> println("Error: ${result.error}")
 * }
 * ```
 */
actual class QrScannerService actual constructor() {

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResults = MutableSharedFlow<QrScanResult>(replay = 0, extraBufferCapacity = 10)
    actual val scanResults: Flow<QrScanResult> = _scanResults.asSharedFlow()

    private val _isTorchEnabled = MutableStateFlow(false)
    actual val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled.asStateFlow()

    private var _currentConfig: QrScannerConfig = QrScannerConfig.FILE_IMPORT
    actual val currentConfig: QrScannerConfig
        get() = _currentConfig

    private val multiFormatReader = MultiFormatReader()

    /**
     * Start scanning mode.
     *
     * On desktop, this puts the scanner in "ready" state for image processing.
     * Unlike mobile platforms, this does not start camera capture.
     * Use [processImage] to decode QR codes from image files.
     *
     * @param config Scanner configuration (primarily uses FILE_IMPORT defaults)
     */
    actual suspend fun startScanning(config: QrScannerConfig) {
        _currentConfig = config
        _isScanning.value = true
    }

    /**
     * Stop scanning mode.
     */
    actual suspend fun stopScanning() {
        _isScanning.value = false
    }

    /**
     * Process an image to extract QR code content.
     *
     * This is the main entry point for desktop QR scanning.
     * Supports common image formats: PNG, JPEG, GIF, BMP.
     *
     * @param imageData Raw image bytes
     * @return QrScanResult with decoded content or error details
     */
    actual fun processImage(imageData: ByteArray): QrScanResult {
        if (imageData.isEmpty()) {
            return QrScanResult.Error(
                error = QrScanError.INVALID_IMAGE,
                message = "Image data is empty"
            )
        }

        return try {
            val image = try {
                ImageIO.read(ByteArrayInputStream(imageData))
            } catch (e: Exception) {
                return QrScanResult.Error(
                    error = QrScanError.INVALID_IMAGE,
                    message = "Failed to read image: ${e.message}"
                )
            }

            if (image == null) {
                return QrScanResult.Error(
                    error = QrScanError.INVALID_IMAGE,
                    message = "Unsupported image format or corrupted image"
                )
            }

            val source = BufferedImageLuminanceSource(image)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            // Configure hints for better detection
            val hints = buildDecodeHints()

            val result: Result = try {
                multiFormatReader.decode(bitmap, hints)
            } catch (e: NotFoundException) {
                return QrScanResult.Error(
                    error = QrScanError.NO_CODE_FOUND,
                    message = "No QR code found in the image"
                )
            } finally {
                multiFormatReader.reset()
            }

            val format = mapZxingFormat(result.barcodeFormat)

            // Check if format is accepted
            if (!_currentConfig.acceptsFormat(format)) {
                return QrScanResult.Error(
                    error = QrScanError.UNSUPPORTED_FORMAT,
                    message = "Barcode format ${result.barcodeFormat} is not accepted"
                )
            }

            val scanResult = QrScanResult.Success(
                content = result.text,
                format = format,
                timestamp = System.currentTimeMillis()
            )

            // Emit to shared flow for listeners
            _scanResults.tryEmit(scanResult)

            // Auto-stop if configured
            if (_currentConfig.autoStopOnSuccess && _currentConfig.scanMode == ScanMode.SINGLE) {
                _isScanning.value = false
            }

            scanResult

        } catch (e: Exception) {
            val errorResult = QrScanResult.Error(
                error = QrScanError.PROCESSING_ERROR,
                message = "Failed to process image: ${e.message}"
            )
            _scanResults.tryEmit(errorResult)
            errorResult
        }
    }

    /**
     * Toggle torch/flashlight.
     *
     * Not applicable on desktop - always returns false.
     */
    actual suspend fun toggleTorch(): Boolean {
        // Torch not available on desktop
        return false
    }

    /**
     * Switch between front and back camera.
     *
     * Not applicable on desktop - no-op.
     */
    actual suspend fun switchCamera() {
        // Camera switching not available on desktop
    }

    /**
     * Check if camera permission is granted.
     *
     * On desktop with file import, no permission is needed.
     * Always returns true.
     */
    actual fun hasPermission(): Boolean = true

    /**
     * Request camera permission.
     *
     * On desktop with file import, no permission is needed.
     * Always returns true immediately.
     */
    actual suspend fun requestPermission(): Boolean = true

    /**
     * Release resources held by the scanner.
     */
    actual fun release() {
        _isScanning.value = false
        multiFormatReader.reset()
    }

    /**
     * Build decode hints for ZXing based on current configuration.
     */
    private fun buildDecodeHints(): Map<DecodeHintType, Any> {
        val hints = mutableMapOf<DecodeHintType, Any>()

        // Try harder to find barcodes
        hints[DecodeHintType.TRY_HARDER] = true

        // Enable pure barcode mode for cleaner images
        hints[DecodeHintType.PURE_BARCODE] = false

        // Set possible formats based on config
        if (_currentConfig.acceptedFormats.isNotEmpty()) {
            val zxingFormats = _currentConfig.acceptedFormats.mapNotNull { format ->
                mapToZxingFormat(format)
            }
            if (zxingFormats.isNotEmpty()) {
                hints[DecodeHintType.POSSIBLE_FORMATS] = zxingFormats
            }
        }

        return hints
    }

    /**
     * Map ZXing barcode format to our QrFormat enum.
     */
    private fun mapZxingFormat(zxingFormat: com.google.zxing.BarcodeFormat): QrFormat {
        return when (zxingFormat) {
            com.google.zxing.BarcodeFormat.QR_CODE -> QrFormat.QR_CODE
            com.google.zxing.BarcodeFormat.DATA_MATRIX -> QrFormat.DATA_MATRIX
            com.google.zxing.BarcodeFormat.AZTEC -> QrFormat.AZTEC
            com.google.zxing.BarcodeFormat.PDF_417 -> QrFormat.PDF417
            com.google.zxing.BarcodeFormat.CODE_128 -> QrFormat.CODE_128
            com.google.zxing.BarcodeFormat.CODE_39 -> QrFormat.CODE_39
            com.google.zxing.BarcodeFormat.EAN_13 -> QrFormat.EAN_13
            com.google.zxing.BarcodeFormat.EAN_8 -> QrFormat.EAN_8
            com.google.zxing.BarcodeFormat.UPC_A -> QrFormat.UPC_A
            com.google.zxing.BarcodeFormat.UPC_E -> QrFormat.UPC_E
            else -> QrFormat.UNKNOWN
        }
    }

    /**
     * Map our QrFormat enum to ZXing barcode format.
     */
    private fun mapToZxingFormat(format: QrFormat): com.google.zxing.BarcodeFormat? {
        return when (format) {
            QrFormat.QR_CODE -> com.google.zxing.BarcodeFormat.QR_CODE
            QrFormat.DATA_MATRIX -> com.google.zxing.BarcodeFormat.DATA_MATRIX
            QrFormat.AZTEC -> com.google.zxing.BarcodeFormat.AZTEC
            QrFormat.PDF417 -> com.google.zxing.BarcodeFormat.PDF_417
            QrFormat.CODE_128 -> com.google.zxing.BarcodeFormat.CODE_128
            QrFormat.CODE_39 -> com.google.zxing.BarcodeFormat.CODE_39
            QrFormat.EAN_13 -> com.google.zxing.BarcodeFormat.EAN_13
            QrFormat.EAN_8 -> com.google.zxing.BarcodeFormat.EAN_8
            QrFormat.UPC_A -> com.google.zxing.BarcodeFormat.UPC_A
            QrFormat.UPC_E -> com.google.zxing.BarcodeFormat.UPC_E
            QrFormat.UNKNOWN -> null
        }
    }
}
