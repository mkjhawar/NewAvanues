package com.augmentalis.webavanue

import platform.Foundation.*

/**
 * iOS DownloadPathValidator implementation
 */
actual class DownloadPathValidator {
    actual fun isValidPath(path: String): Boolean {
        val fileManager = NSFileManager.defaultManager

        // Check if parent directory exists
        val parentPath = (path as NSString).stringByDeletingLastPathComponent
        return fileManager.fileExistsAtPath(parentPath)
    }

    actual fun isWritable(path: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        val parentPath = (path as NSString).stringByDeletingLastPathComponent
        return fileManager.isWritableFileAtPath(parentPath)
    }

    actual fun sanitizeFilename(filename: String): String {
        // Remove invalid characters for iOS filesystem
        val invalidChars = setOf('/', ':', '\\', '|', '?', '*', '<', '>', '"')
        return filename.filter { it !in invalidChars }
    }

    actual fun getDefaultDownloadPath(filename: String): String {
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String

        val sanitized = sanitizeFilename(filename)
        return documentsPath?.let { "$it/$sanitized" } ?: sanitized
    }
}
