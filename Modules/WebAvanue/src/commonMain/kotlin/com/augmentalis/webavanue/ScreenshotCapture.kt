package com.augmentalis.webavanue

import kotlinx.coroutines.flow.Flow

/**
 * Screenshot type enumeration
 */
enum class ScreenshotType {
    /**
     * Capture only the currently visible viewport
     */
    VISIBLE_AREA,

    /**
     * Capture the entire page by scrolling and stitching
     */
    FULL_PAGE
}

/**
 * Platform-agnostic screenshot data wrapper
 * Encapsulates platform-specific bitmap/image data
 */
expect class ScreenshotData {
    /**
     * Get the width of the screenshot in pixels
     */
    val width: Int

    /**
     * Get the height of the screenshot in pixels
     */
    val height: Int

    /**
     * Recycle/release the underlying image data
     * Call this when done with the screenshot to free memory
     */
    fun recycle()
}

/**
 * Screenshot capture result
 */
sealed class ScreenshotResult {
    /**
     * Screenshot captured successfully
     * @param data The captured screenshot data
     * @param filepath Path where screenshot was saved (if saved)
     */
    data class Success(
        val data: ScreenshotData,
        val filepath: String? = null
    ) : ScreenshotResult()

    /**
     * Screenshot capture failed
     * @param error Error message
     * @param cause Exception that caused the failure
     */
    data class Error(
        val error: String,
        val cause: Throwable? = null
    ) : ScreenshotResult()

    /**
     * Screenshot capture in progress
     * @param progress Progress percentage (0.0 to 1.0)
     * @param message Status message
     */
    data class Progress(
        val progress: Float,
        val message: String
    ) : ScreenshotResult()
}

/**
 * Screenshot capture request
 */
data class ScreenshotRequest(
    val type: ScreenshotType,
    val quality: Int = 80,
    val saveToGallery: Boolean = true,
    val filename: String? = null
)

/**
 * Screenshot capture interface
 *
 * Platform-agnostic interface for capturing webpage screenshots.
 * Implementations will be platform-specific (Android WebView, iOS WKWebView, etc.)
 */
interface ScreenshotCapture {

    /**
     * Capture a screenshot of the current webpage
     *
     * @param request Screenshot capture request
     * @return Flow of ScreenshotResult (Progress updates followed by Success or Error)
     */
    fun capture(request: ScreenshotRequest): Flow<ScreenshotResult>

    /**
     * Cancel an ongoing screenshot capture
     */
    suspend fun cancel()

    /**
     * Save screenshot data to the device gallery
     *
     * @param data Screenshot data to save
     * @param filename Filename (will be generated if null)
     * @param quality JPEG quality (0-100)
     * @return File path or null on failure
     */
    suspend fun saveScreenshot(
        data: ScreenshotData,
        filename: String? = null,
        quality: Int = 80
    ): String?

    /**
     * Share a screenshot
     *
     * @param filepath Path to the screenshot file
     * @param title Share dialog title
     */
    suspend fun share(filepath: String, title: String = "Share Screenshot")
}

/**
 * Screenshot filename generator
 */
object ScreenshotFilenameUtils {

    /**
     * Generate a screenshot filename with timestamp
     * Format: Screenshot_YYYYMMDD_HHMMSS.png
     */
    fun generateFilename(): String {
        val timestamp = currentFormattedTime()
        return "Screenshot_$timestamp.png"
    }

    /**
     * Get the default screenshot directory path
     * Platform-specific implementation will provide actual path
     */
    fun getScreenshotDirectory(): String {
        return getScreenshotDirectoryPath()
    }
}

/**
 * Get the default screenshot directory path
 * Platform-specific implementation
 */
expect fun getScreenshotDirectoryPath(): String

/**
 * Get current time formatted for filename
 * Format: YYYYMMDD_HHMMSS
 */
internal expect fun currentFormattedTime(): String

/**
 * Create an instance of ScreenshotCapture
 * Platform-specific factory function
 */
expect fun createScreenshotCapture(webView: Any): ScreenshotCapture
