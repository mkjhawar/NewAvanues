package com.augmentalis.webavanue

import android.os.Environment

/**
 * Get the default screenshot directory path on Android
 */
actual fun getScreenshotDirectoryPath(): String {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        .resolve("WebAvanue")
        .absolutePath
}
