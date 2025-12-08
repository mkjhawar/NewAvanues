package com.augmentalis.avanues.avamagic.components.argscanner

import android.os.Environment
import java.io.File

/**
 * Android platform implementation for ARG scanner file operations
 */

actual fun getPlatformPrimaryPath(): String {
    // App's internal files directory
    return "/data/data/com.augmentalis.voiceos/files/arg/"
}

actual fun getPlatformSecondaryPath(): String {
    // External storage (requires permission)
    val externalDir = Environment.getExternalStorageDirectory()
    return "$externalDir/Avanue/registry/"
}

actual fun getPlatformUserPath(): String {
    // User-accessible location
    val externalDir = Environment.getExternalStorageDirectory()
    return "$externalDir/Documents/Avanue/registry/"
}

actual fun platformReadFile(path: String): String {
    return File(path).readText()
}

actual fun platformIsDirectory(path: String): Boolean {
    return File(path).isDirectory
}

actual fun platformListFiles(directory: String): List<String> {
    val dir = File(directory)
    if (!dir.exists() || !dir.isDirectory) {
        return emptyList()
    }

    return dir.listFiles()?.map { it.absolutePath } ?: emptyList()
}
