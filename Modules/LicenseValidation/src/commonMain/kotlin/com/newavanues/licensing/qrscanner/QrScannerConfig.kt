package com.newavanues.licensing.qrscanner

/**
 * Configuration options for the QR scanner.
 */
data class QrScannerConfig(
    /**
     * Which camera to use (front or back).
     */
    val cameraFacing: CameraFacing = CameraFacing.BACK,

    /**
     * Scanning mode - single scan or continuous.
     */
    val scanMode: ScanMode = ScanMode.SINGLE,

    /**
     * Whether to trigger haptic feedback on successful scan.
     */
    val enableHapticFeedback: Boolean = true,

    /**
     * Whether to play a sound on successful scan.
     */
    val enableSoundFeedback: Boolean = false,

    /**
     * Set of barcode formats to accept.
     * Empty set means accept all supported formats.
     */
    val acceptedFormats: Set<QrFormat> = setOf(QrFormat.QR_CODE),

    /**
     * Whether to show the scanning overlay/viewfinder.
     */
    val showOverlay: Boolean = true,

    /**
     * Delay in milliseconds between scans in continuous mode.
     * Helps prevent duplicate scans of the same code.
     */
    val scanDelayMs: Long = 1000L,

    /**
     * Whether to automatically stop after a successful scan (in SINGLE mode).
     */
    val autoStopOnSuccess: Boolean = true,

    /**
     * Resolution preference for camera preview.
     */
    val resolution: ScannerResolution = ScannerResolution.HD,

    /**
     * Whether to enable torch/flashlight control.
     */
    val enableTorch: Boolean = true
) {
    companion object {
        /**
         * Default configuration for license QR scanning.
         */
        val DEFAULT = QrScannerConfig()

        /**
         * Configuration optimized for continuous scanning.
         */
        val CONTINUOUS = QrScannerConfig(
            scanMode = ScanMode.CONTINUOUS,
            autoStopOnSuccess = false,
            scanDelayMs = 500L
        )

        /**
         * Configuration for scanning from images/files (desktop/web).
         */
        val FILE_IMPORT = QrScannerConfig(
            scanMode = ScanMode.SINGLE,
            enableHapticFeedback = false,
            showOverlay = false
        )
    }

    /**
     * Check if a given format is accepted by this configuration.
     */
    fun acceptsFormat(format: QrFormat): Boolean {
        return acceptedFormats.isEmpty() || format in acceptedFormats
    }
}

/**
 * Camera facing direction.
 */
enum class CameraFacing {
    /** Back-facing camera (default for scanning) */
    BACK,

    /** Front-facing camera */
    FRONT;

    /**
     * Toggle to the opposite facing.
     */
    fun toggle(): CameraFacing = when (this) {
        BACK -> FRONT
        FRONT -> BACK
    }
}

/**
 * Scanning mode.
 */
enum class ScanMode {
    /** Stop after first successful scan */
    SINGLE,

    /** Keep scanning until manually stopped */
    CONTINUOUS
}

/**
 * Camera resolution preference.
 */
enum class ScannerResolution {
    /** 480p - Lower quality, faster processing */
    SD,

    /** 720p - Good balance of quality and speed */
    HD,

    /** 1080p - Higher quality, may be slower */
    FULL_HD;

    /**
     * Get approximate width for this resolution.
     */
    val width: Int
        get() = when (this) {
            SD -> 640
            HD -> 1280
            FULL_HD -> 1920
        }

    /**
     * Get approximate height for this resolution.
     */
    val height: Int
        get() = when (this) {
            SD -> 480
            HD -> 720
            FULL_HD -> 1080
        }
}
