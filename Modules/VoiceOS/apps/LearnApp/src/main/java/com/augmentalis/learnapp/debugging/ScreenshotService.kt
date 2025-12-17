package com.augmentalis.learnapp.debugging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screenshot Service
 *
 * Captures screenshots during exploration for debugging purposes.
 * Screenshots are saved to app's external files directory.
 *
 * Location: /sdcard/Android/data/com.augmentalis.voiceos/files/learnapp_screenshots/
 *
 * Note: Full screen capture requires MediaProjection API (Android 11+) with user permission.
 * This simplified version can capture individual views.
 */
class ScreenshotService(private val context: Context) {

    private val screenshotDir: File by lazy {
        File(context.getExternalFilesDir(null), "learnapp_screenshots").apply {
            mkdirs()
        }
    }

    /**
     * Capture screenshot of a view
     *
     * @param view View to capture
     * @param screenHash Screen hash for filename
     * @return Screenshot file or null if failed
     */
    fun captureView(view: View, screenHash: String): File? {
        return try {
            val timestamp = System.currentTimeMillis()
            val filename = "screen_${screenHash.take(8)}_$timestamp.png"
            val file = File(screenshotDir, filename)

            val bitmap = Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            bitmap.recycle()

            android.util.Log.d("ScreenshotService", "Screenshot saved: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotService", "Failed to capture screenshot", e)
            null
        }
    }

    /**
     * Save screen state as text log
     *
     * Alternative to screenshots - saves detailed text representation.
     *
     * @param screenHash Screen hash
     * @param content Screen content description
     * @return Log file or null if failed
     */
    fun saveScreenLog(screenHash: String, content: String): File? {
        return try {
            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(timestamp))
            val filename = "screen_log_${screenHash.take(8)}_$dateStr.txt"
            val file = File(screenshotDir, filename)

            file.writeText(content)

            android.util.Log.d("ScreenshotService", "Screen log saved: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotService", "Failed to save screen log", e)
            null
        }
    }

    /**
     * Get all screenshot files
     */
    fun getScreenshots(): List<File> {
        return screenshotDir.listFiles()?.filter {
            it.extension == "png"
        }?.sortedByDescending {
            it.lastModified()
        } ?: emptyList()
    }

    /**
     * Get all screen log files
     */
    fun getScreenLogs(): List<File> {
        return screenshotDir.listFiles()?.filter {
            it.extension == "txt"
        }?.sortedByDescending {
            it.lastModified()
        } ?: emptyList()
    }

    /**
     * Clear old screenshots (keep last N)
     */
    fun clearOldScreenshots(keepLast: Int = 50) {
        val screenshots = getScreenshots()
        screenshots.drop(keepLast).forEach { file ->
            file.delete()
            android.util.Log.d("ScreenshotService", "Deleted old screenshot: ${file.name}")
        }
    }

    /**
     * Get screenshot directory path
     */
    fun getScreenshotDirectory(): String {
        return screenshotDir.absolutePath
    }

    companion object {
        private var instance: ScreenshotService? = null

        fun getInstance(context: Context): ScreenshotService {
            return instance ?: synchronized(this) {
                instance ?: ScreenshotService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
