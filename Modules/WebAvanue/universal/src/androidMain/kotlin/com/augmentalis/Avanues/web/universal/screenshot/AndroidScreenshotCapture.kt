package com.augmentalis.webavanue.feature.screenshot

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import androidx.core.content.FileProvider
import com.augmentalis.webavanue.ui.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

/**
 * Android implementation of ScreenshotCapture using WebView
 *
 * Features:
 * - Visible area capture using View.drawToBitmap()
 * - Full page capture by scrolling and stitching
 * - Memory efficient (uses tile-based stitching for large pages)
 * - Progress updates during full page capture
 * - Saves to Pictures/WebAvanue/ directory
 * - Share functionality via Android intent
 */
class AndroidScreenshotCapture(
    private val context: Context,
    private val webView: WebView
) : ScreenshotCapture {

    private var captureJob: Job? = null

    override fun capture(request: ScreenshotRequest): Flow<ScreenshotResult> = flow {
        try {
            when (request.type) {
                ScreenshotType.VISIBLE_AREA -> {
                    emit(ScreenshotResult.Progress(0.5f, "Capturing visible area..."))
                    val bitmap = captureVisibleArea()

                    val filepath = if (request.saveToGallery) {
                        emit(ScreenshotResult.Progress(0.8f, "Saving screenshot..."))
                        val filename = request.filename ?: ScreenshotFilenameUtils.generateFilename()
                        saveToGallery(bitmap, filename, request.quality)
                    } else null

                    emit(ScreenshotResult.Success(ScreenshotData(bitmap), filepath))
                }

                ScreenshotType.FULL_PAGE -> {
                    emit(ScreenshotResult.Progress(0.1f, "Preparing full page capture..."))

                    // Check if we should continue
                    if (!coroutineContext.isActive) {
                        emit(ScreenshotResult.Error("Screenshot capture cancelled"))
                        return@flow
                    }

                    val bitmap = captureFullPage { progress, message ->
                        emit(ScreenshotResult.Progress(progress, message))
                    }

                    if (bitmap == null) {
                        emit(ScreenshotResult.Error("Failed to capture full page"))
                        return@flow
                    }

                    val filepath = if (request.saveToGallery) {
                        emit(ScreenshotResult.Progress(0.9f, "Saving screenshot..."))
                        val filename = request.filename ?: ScreenshotFilenameUtils.generateFilename()
                        saveToGallery(bitmap, filename, request.quality)
                    } else null

                    emit(ScreenshotResult.Success(ScreenshotData(bitmap), filepath))
                }
            }
        } catch (e: Exception) {
            Logger.error("AndroidScreenshotCapture", "Screenshot capture failed: ${e.message}", e)
            emit(ScreenshotResult.Error("Failed to capture screenshot: ${e.message}", e))
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun cancel() {
        captureJob?.cancel()
        captureJob = null
    }

    override suspend fun saveScreenshot(
        data: ScreenshotData,
        filename: String?,
        quality: Int
    ): String? = withContext(Dispatchers.IO) {
        if (data !is ScreenshotData) return@withContext null

        val finalFilename = filename ?: ScreenshotFilenameUtils.generateFilename()
        saveToGallery(data.bitmap, finalFilename, quality)
    }

    override suspend fun share(filepath: String, title: String) = withContext(Dispatchers.Main) {
        try {
            val file = File(filepath)
            if (!file.exists()) {
                Logger.error("AndroidScreenshotCapture", "Screenshot file not found: $filepath")
                return@withContext
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            Logger.error("AndroidScreenshotCapture", "Failed to share screenshot: ${e.message}", e)
        }
    }

    /**
     * Capture only the visible viewport
     */
    private suspend fun captureVisibleArea(): Bitmap = withContext(Dispatchers.Main) {
        val width = webView.width
        val height = webView.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)

        bitmap
    }

    /**
     * Capture the entire page by scrolling and stitching
     *
     * This implementation:
     * 1. Gets the full content height from WebView
     * 2. Scrolls through the page in viewport-sized chunks
     * 3. Captures each chunk and stitches them together
     * 4. Restores original scroll position
     *
     * Memory management:
     * - Uses tile-based approach to avoid OOM on huge pages
     * - Limits maximum height to 15,000 pixels
     * - Recycles intermediate bitmaps
     */
    private suspend fun captureFullPage(
        onProgress: suspend (Float, String) -> Unit
    ): Bitmap? = withContext(Dispatchers.Main) {
        // Save original scroll position
        val originalScrollX = webView.scrollX
        val originalScrollY = webView.scrollY

        try {
            // Get page dimensions
            val viewportWidth = webView.width
            val viewportHeight = webView.height
            val contentHeight = (webView.contentHeight * webView.scale).toInt()

            // Safety check: limit maximum height to prevent OOM
            val maxHeight = 15000
            val actualHeight = contentHeight.coerceAtMost(maxHeight)

            if (actualHeight > maxHeight) {
                Logger.warn(
                    "AndroidScreenshotCapture",
                    "Page height ($contentHeight) exceeds maximum ($maxHeight). Truncating."
                )
            }

            onProgress(0.2f, "Capturing page (${actualHeight}px tall)...")

            // Calculate number of tiles needed
            val numTiles = (actualHeight + viewportHeight - 1) / viewportHeight
            val tiles = mutableListOf<Bitmap>()

            try {
                // Capture each tile
                for (i in 0 until numTiles) {
                    // Check for cancellation
                    if (!coroutineContext.isActive) {
                        Logger.info("AndroidScreenshotCapture", "Full page capture cancelled")
                        tiles.forEach { it.recycle() }
                        return@withContext null
                    }

                    val scrollY = i * viewportHeight
                    webView.scrollTo(0, scrollY)

                    // Wait for scroll to settle
                    delay(100)

                    // Calculate tile height (last tile might be shorter)
                    val tileHeight = if (i == numTiles - 1) {
                        actualHeight - (i * viewportHeight)
                    } else {
                        viewportHeight
                    }

                    // Capture tile
                    val tile = Bitmap.createBitmap(viewportWidth, viewportHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(tile)
                    webView.draw(canvas)

                    // Crop if needed (last tile)
                    val croppedTile = if (tileHeight < viewportHeight) {
                        Bitmap.createBitmap(tile, 0, 0, viewportWidth, tileHeight)
                            .also { tile.recycle() }
                    } else {
                        tile
                    }

                    tiles.add(croppedTile)

                    val progress = 0.2f + (0.7f * (i + 1) / numTiles)
                    onProgress(progress, "Captured ${i + 1} of $numTiles sections...")
                }

                onProgress(0.9f, "Stitching sections together...")

                // Stitch tiles together
                val finalBitmap = Bitmap.createBitmap(viewportWidth, actualHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(finalBitmap)

                var currentY = 0f
                for (tile in tiles) {
                    canvas.drawBitmap(tile, 0f, currentY, null)
                    currentY += tile.height
                }

                // Clean up tiles
                tiles.forEach { it.recycle() }

                finalBitmap
            } catch (e: Exception) {
                // Clean up on error
                tiles.forEach { it.recycle() }
                throw e
            }
        } finally {
            // Restore original scroll position
            webView.scrollTo(originalScrollX, originalScrollY)
        }
    }

    /**
     * Save bitmap to device gallery
     *
     * Uses MediaStore API for Android Q+ (scoped storage)
     * Uses legacy file API for older versions
     */
    private suspend fun saveToGallery(
        bitmap: Bitmap,
        filename: String,
        quality: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android Q+
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/WebAvanue")
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let { imageUri ->
                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
                    }

                    // Get actual file path
                    val projection = arrayOf(MediaStore.Images.Media.DATA)
                    context.contentResolver.query(imageUri, projection, null, null, null)?.use { cursor ->
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        if (cursor.moveToFirst()) {
                            cursor.getString(columnIndex)
                        } else null
                    }
                }
            } else {
                // Legacy file API for older versions
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val webAvanueDir = File(picturesDir, "WebAvanue")

                if (!webAvanueDir.exists()) {
                    webAvanueDir.mkdirs()
                }

                val file = File(webAvanueDir, filename)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
                }

                // Notify media scanner
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(file)
                context.sendBroadcast(intent)

                file.absolutePath
            }
        } catch (e: Exception) {
            Logger.error("AndroidScreenshotCapture", "Failed to save screenshot: ${e.message}", e)
            null
        }
    }
}

/**
 * Create an instance of ScreenshotCapture for Android
 */
actual fun createScreenshotCapture(webView: Any): ScreenshotCapture {
    require(webView is WebView) { "webView must be an Android WebView" }

    val context = webView.context
    return AndroidScreenshotCapture(context, webView)
}
