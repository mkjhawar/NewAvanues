package com.newavanues.licensing.qrscanner

/**
 * Represents the result of a QR code scan operation.
 */
sealed class QrScanResult {
    /**
     * Successful scan with decoded content.
     */
    data class Success(
        val content: String,
        val format: QrFormat = QrFormat.QR_CODE,
        val timestamp: Long = currentTimeMillis()
    ) : QrScanResult()

    /**
     * Scan failed with an error.
     */
    data class Error(
        val error: QrScanError,
        val message: String? = null
    ) : QrScanResult()

    /**
     * Camera permission was denied by the user.
     */
    object PermissionDenied : QrScanResult()

    /**
     * No camera available on the device.
     */
    object NoCameraAvailable : QrScanResult()

    /**
     * Scanning was cancelled by the user.
     */
    object Cancelled : QrScanResult()

    /**
     * Check if this result represents a successful scan.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Get the content if successful, null otherwise.
     */
    fun contentOrNull(): String? = (this as? Success)?.content

    /**
     * Get the error if failed, null otherwise.
     */
    fun errorOrNull(): QrScanError? = (this as? Error)?.error
}

/**
 * Supported barcode/QR code formats.
 */
enum class QrFormat {
    QR_CODE,
    DATA_MATRIX,
    AZTEC,
    PDF417,
    CODE_128,
    CODE_39,
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    UNKNOWN
}

/**
 * Errors that can occur during QR scanning.
 */
enum class QrScanError {
    /** Camera initialization failed */
    CAMERA_INIT_FAILED,

    /** Camera access error during scanning */
    CAMERA_ERROR,

    /** No QR code found in the image/frame */
    NO_CODE_FOUND,

    /** QR code found but could not be decoded */
    DECODE_FAILED,

    /** The scanned format is not supported */
    UNSUPPORTED_FORMAT,

    /** Image processing error */
    PROCESSING_ERROR,

    /** Scanner is not initialized */
    NOT_INITIALIZED,

    /** Scanner is already running */
    ALREADY_SCANNING,

    /** File not found (for image import) */
    FILE_NOT_FOUND,

    /** Invalid image format */
    INVALID_IMAGE,

    /** Generic/unknown error */
    UNKNOWN;

    /**
     * Get a user-friendly error message.
     */
    fun toUserMessage(): String = when (this) {
        CAMERA_INIT_FAILED -> "Failed to initialize camera. Please try again."
        CAMERA_ERROR -> "Camera error occurred. Please restart the scanner."
        NO_CODE_FOUND -> "No QR code found. Please position the code within the frame."
        DECODE_FAILED -> "Could not read the QR code. Please try again."
        UNSUPPORTED_FORMAT -> "This barcode format is not supported."
        PROCESSING_ERROR -> "Error processing the image. Please try again."
        NOT_INITIALIZED -> "Scanner not ready. Please wait."
        ALREADY_SCANNING -> "Scanner is already active."
        FILE_NOT_FOUND -> "Image file not found."
        INVALID_IMAGE -> "Invalid image format."
        UNKNOWN -> "An unexpected error occurred."
    }
}

/**
 * Platform-independent way to get current time in milliseconds.
 */
internal expect fun currentTimeMillis(): Long
