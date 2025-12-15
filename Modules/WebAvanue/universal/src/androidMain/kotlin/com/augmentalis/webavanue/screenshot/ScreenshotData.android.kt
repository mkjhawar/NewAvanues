package com.augmentalis.webavanue.feature.screenshot

import android.graphics.Bitmap

/**
 * Android implementation of ScreenshotData
 * Wraps an Android Bitmap
 */
actual class ScreenshotData(
    val bitmap: Bitmap
) {
    /**
     * Get the width of the screenshot in pixels
     */
    actual val width: Int
        get() = bitmap.width

    /**
     * Get the height of the screenshot in pixels
     */
    actual val height: Int
        get() = bitmap.height

    /**
     * Recycle the underlying bitmap to free memory
     */
    actual fun recycle() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
