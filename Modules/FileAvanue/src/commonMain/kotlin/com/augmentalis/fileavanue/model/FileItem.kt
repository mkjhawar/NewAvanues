package com.augmentalis.fileavanue.model

import kotlinx.serialization.Serializable

/**
 * Platform-agnostic representation of a file or directory.
 *
 * URIs are opaque strings whose format depends on the storage provider:
 * - Local Android: `content://` or absolute path
 * - Desktop: absolute filesystem path
 * - Cloud: provider-specific resource ID
 * - Web: IndexedDB key or File System Access handle ID
 */
@Serializable
data class FileItem(
    val uri: String,
    val name: String,
    val mimeType: String = "*/*",
    val fileSizeBytes: Long = 0,
    val isDirectory: Boolean = false,
    val dateCreated: Long = 0L,
    val dateModified: Long = 0L,
    val parentUri: String = "",
    val thumbnailUri: String = "",
    val childCount: Int = -1,
    val isHidden: Boolean = false,
    val providerId: String = "local",
) {
    val extension: String get() = name.substringAfterLast('.', "")
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isAudio: Boolean get() = mimeType.startsWith("audio/")
    val isPdf: Boolean get() = mimeType == "application/pdf"
    val isArchive: Boolean get() = extension.lowercase() in ARCHIVE_EXTENSIONS
    val isDocument: Boolean get() = extension.lowercase() in DOCUMENT_EXTENSIONS

    val formattedSize: String get() = formatBytes(fileSizeBytes)

    companion object {
        private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
        private val DOCUMENT_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "odt", "ods", "odp", "txt", "rtf", "csv"
        )
    }
}

/**
 * Formats byte count into human-readable string (KB, MB, GB).
 * Uses binary units (1024-based) for consistency with OS file managers.
 */
fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "Unknown"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

/**
 * Expect declaration for platform-specific String.format since it's not in Kotlin common stdlib.
 * Each platform provides the actual implementation.
 */
internal expect fun String.Companion.format(format: String, vararg args: Any?): String
