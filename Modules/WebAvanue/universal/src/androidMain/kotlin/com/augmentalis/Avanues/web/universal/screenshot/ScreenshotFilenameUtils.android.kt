package com.augmentalis.Avanues.web.universal.screenshot

import android.os.Environment

/**
 * Get the default screenshot directory path on Android
 */
actual fun ScreenshotFilenameUtils.getScreenshotDirectory(): String {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        .resolve("WebAvanue")
        .absolutePath
}
