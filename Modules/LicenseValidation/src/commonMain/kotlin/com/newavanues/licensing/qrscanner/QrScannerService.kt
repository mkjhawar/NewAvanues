package com.newavanues.licensing.qrscanner

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic interface for QR code scanning.
 *
 * This service provides a common API for QR code scanning across all platforms:
 * - Android: CameraX + ML Kit
 * - iOS: AVFoundation + Vision
 * - Desktop: ZXing (file import / optional webcam)
 * - Web/JS: MediaDevices API + jsQR
 *
 * Usage:
 * ```kotlin
 * val scanner = QrScannerService()
 *
 * // Start scanning
 * scanner.startScanning(QrScannerConfig.DEFAULT)
 *
 * // Collect results
 * scanner.scanResults.collect { result ->
 *     when (result) {
 *         is QrScanResult.Success -> handleScannedCode(result.content)
 *         is QrScanResult.Error -> showError(result.error)
 *         QrScanResult.PermissionDenied -> requestPermission()
 *         QrScanResult.NoCameraAvailable -> showManualEntry()
 *         QrScanResult.Cancelled -> dismiss()
 *     }
 * }
 *
 * // Stop scanning when done
 * scanner.stopScanning()
 * ```
 */
expect class QrScannerService() {
    /**
     * Flow indicating whether the scanner is currently active.
     */
    val isScanning: StateFlow<Boolean>

    /**
     * Flow of scan results. Emits [QrScanResult] for each scan attempt.
     * In SINGLE mode, stops after first successful scan.
     * In CONTINUOUS mode, continues emitting until [stopScanning] is called.
     */
    val scanResults: Flow<QrScanResult>

    /**
     * Current scanner configuration.
     */
    val currentConfig: QrScannerConfig

    /**
     * Start the QR code scanning process.
     *
     * On mobile platforms (Android/iOS), this will:
     * 1. Request camera permissions if needed
     * 2. Initialize the camera
     * 3. Begin processing frames for QR codes
     *
     * On desktop, this may either:
     * - Start webcam capture (if available and configured)
     * - Wait for image input via [processImage]
     *
     * On web, this will:
     * - Request camera permission via browser
     * - Start video stream processing
     *
     * @param config Scanner configuration options
     */
    suspend fun startScanning(config: QrScannerConfig = QrScannerConfig.DEFAULT)

    /**
     * Stop the scanning process and release camera resources.
     */
    suspend fun stopScanning()

    /**
     * Process a static image for QR codes.
     *
     * Useful for:
     * - Desktop file import
     * - Web image upload
     * - Processing screenshots or saved images
     *
     * @param imageData Raw image bytes (PNG, JPEG, etc.)
     * @return QrScanResult with the decoded content or error
     */
    fun processImage(imageData: ByteArray): QrScanResult

    /**
     * Toggle the torch/flashlight on supported devices.
     *
     * @return true if torch is now on, false if off or not supported
     */
    suspend fun toggleTorch(): Boolean

    /**
     * Check if the torch/flashlight is currently enabled.
     */
    val isTorchEnabled: StateFlow<Boolean>

    /**
     * Switch between front and back camera.
     * Only applicable on mobile platforms.
     */
    suspend fun switchCamera()

    /**
     * Check if camera permission has been granted.
     * Returns true on platforms that don't require permission (desktop file import).
     */
    fun hasPermission(): Boolean

    /**
     * Request camera permission.
     * Returns true if permission was granted.
     */
    suspend fun requestPermission(): Boolean

    /**
     * Release all resources held by the scanner.
     * Call this when the scanner is no longer needed.
     */
    fun release()
}

/**
 * Callback interface for scan results.
 * Alternative to collecting from [QrScannerService.scanResults] flow.
 */
interface QrScannerCallback {
    /**
     * Called when a QR code is successfully scanned.
     */
    fun onScanSuccess(result: QrScanResult.Success)

    /**
     * Called when an error occurs during scanning.
     */
    fun onScanError(result: QrScanResult.Error)

    /**
     * Called when camera permission is denied.
     */
    fun onPermissionDenied()

    /**
     * Called when no camera is available.
     */
    fun onNoCameraAvailable()

    /**
     * Called when scanning is cancelled by user.
     */
    fun onCancelled()
}

/**
 * Extension function to convert QrScanResult to callback invocations.
 */
fun QrScanResult.dispatch(callback: QrScannerCallback) {
    when (this) {
        is QrScanResult.Success -> callback.onScanSuccess(this)
        is QrScanResult.Error -> callback.onScanError(this)
        QrScanResult.PermissionDenied -> callback.onPermissionDenied()
        QrScanResult.NoCameraAvailable -> callback.onNoCameraAvailable()
        QrScanResult.Cancelled -> callback.onCancelled()
    }
}
