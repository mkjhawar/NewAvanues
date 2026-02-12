package com.augmentalis.webavanue

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.*
import platform.UIKit.*
import platform.WebKit.*
import kotlinx.cinterop.*

/**
 * iOS ScreenshotData implementation using UIImage
 */
actual class ScreenshotData(val image: UIImage) {
    actual val width: Int
        get() = image.size.useContents { width.toInt() }

    actual val height: Int
        get() = image.size.useContents { height.toInt() }

    actual fun recycle() {
        // UIImage is automatically managed by ARC
        // No manual cleanup needed
    }
}

/**
 * iOS screenshot filename utils
 */
internal actual fun currentFormattedTime(): String {
    val dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyyMMdd_HHmmss"
    return dateFormatter.stringFromDate(NSDate())
}

/**
 * Get screenshot directory path for iOS
 */
actual fun getScreenshotDirectoryPath(): String {
    val documentsPath = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).firstOrNull() as? String

    return documentsPath?.let { "$it/Screenshots" } ?: NSTemporaryDirectory()
}

/**
 * iOS ScreenshotCapture implementation using WKWebView snapshot
 */
class IOSScreenshotCapture(private val webView: WKWebView) : ScreenshotCapture {

    private var isCancelled = false

    override fun capture(request: ScreenshotRequest): Flow<ScreenshotResult> = flow {
        isCancelled = false
        emit(ScreenshotResult.Progress(0.0f, "Preparing screenshot..."))

        when (request.type) {
            ScreenshotType.VISIBLE_AREA -> {
                emit(ScreenshotResult.Progress(0.5f, "Capturing visible area..."))
                val result = captureVisibleArea(request)
                emit(result)
            }
            ScreenshotType.FULL_PAGE -> {
                emit(ScreenshotResult.Progress(0.5f, "Capturing full page..."))
                // Full page screenshots require scrolling and stitching
                // For now, just capture visible area
                val result = captureVisibleArea(request)
                emit(result)
            }
        }
    }

    override suspend fun cancel() {
        isCancelled = true
    }

    override suspend fun saveScreenshot(
        data: ScreenshotData,
        filename: String?,
        quality: Int
    ): String? {
        val finalFilename = filename ?: ScreenshotFilenameUtils.generateFilename()
        val directory = getScreenshotDirectoryPath()

        // Create directory if needed
        val fileManager = NSFileManager.defaultManager
        fileManager.createDirectoryAtPath(
            directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        val filePath = "$directory/$finalFilename"

        // Convert UIImage to PNG data
        val pngData = UIImagePNGRepresentation(data.image)
        if (pngData != null) {
            val success = pngData.writeToFile(filePath, atomically = true)
            return if (success) filePath else null
        }

        return null
    }

    override suspend fun share(filepath: String, title: String) {
        // Sharing requires UIViewController integration
        println("Share screenshot not implemented for iOS (requires UIViewController)")
    }

    private suspend fun captureVisibleArea(request: ScreenshotRequest): ScreenshotResult {
        if (isCancelled) {
            return ScreenshotResult.Error("Screenshot cancelled")
        }

        var capturedImage: UIImage? = null
        var captureError: NSError? = null

        val configuration = WKSnapshotConfiguration()
        configuration.rect = platform.CoreGraphics.CGRectZero.readValue()

        // Take snapshot
        webView.takeSnapshotWithConfiguration(configuration) { image, error ->
            capturedImage = image
            captureError = error
        }

        // Wait for snapshot to complete (synchronous for now)
        // In production, this should be properly async
        Thread.sleep(500)

        return if (captureError == null && capturedImage != null) {
            val data = ScreenshotData(capturedImage!!)

            if (request.saveToGallery) {
                val filepath = saveScreenshot(
                    data,
                    request.filename,
                    request.quality
                )
                ScreenshotResult.Success(data, filepath)
            } else {
                ScreenshotResult.Success(data, null)
            }
        } else {
            ScreenshotResult.Error(
                captureError?.localizedDescription ?: "Unknown error",
                null
            )
        }
    }
}

/**
 * Create screenshot capture for iOS
 */
actual fun createScreenshotCapture(webView: Any): ScreenshotCapture {
    require(webView is WKWebView) { "webView must be WKWebView for iOS" }
    return IOSScreenshotCapture(webView)
}
