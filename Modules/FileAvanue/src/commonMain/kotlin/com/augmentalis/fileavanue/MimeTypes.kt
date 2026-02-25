package com.augmentalis.fileavanue

/**
 * Consolidated MIME type registry for file type detection.
 *
 * Single source of truth for extension-to-MIME mappings across the Avanues ecosystem.
 * Consolidates scattered mappings from WebAvanue (DownloadQueue/FilenameUtils)
 * and HTTPAvanue (StaticFileMiddleware).
 */
object MimeTypes {

    private val EXTENSION_TO_MIME: Map<String, String> = mapOf(
        // Images
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "bmp" to "image/bmp",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "tiff" to "image/tiff",
        "tif" to "image/tiff",
        "heic" to "image/heic",
        "heif" to "image/heif",
        "avif" to "image/avif",
        "raw" to "image/x-raw",

        // Video
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "avi" to "video/x-msvideo",
        "mov" to "video/quicktime",
        "wmv" to "video/x-ms-wmv",
        "flv" to "video/x-flv",
        "webm" to "video/webm",
        "m4v" to "video/x-m4v",
        "3gp" to "video/3gpp",
        "ts" to "video/mp2t",

        // Audio
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "flac" to "audio/flac",
        "aac" to "audio/aac",
        "ogg" to "audio/ogg",
        "wma" to "audio/x-ms-wma",
        "m4a" to "audio/mp4",
        "opus" to "audio/opus",
        "mid" to "audio/midi",
        "midi" to "audio/midi",
        "aiff" to "audio/aiff",

        // Documents
        "pdf" to "application/pdf",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "odt" to "application/vnd.oasis.opendocument.text",
        "ods" to "application/vnd.oasis.opendocument.spreadsheet",
        "odp" to "application/vnd.oasis.opendocument.presentation",
        "rtf" to "application/rtf",
        "csv" to "text/csv",

        // Text / Code
        "txt" to "text/plain",
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "text/javascript",
        "json" to "application/json",
        "xml" to "application/xml",
        "yaml" to "application/x-yaml",
        "yml" to "application/x-yaml",
        "md" to "text/markdown",
        "kt" to "text/x-kotlin",
        "java" to "text/x-java-source",
        "py" to "text/x-python",
        "swift" to "text/x-swift",
        "c" to "text/x-csrc",
        "cpp" to "text/x-c++src",
        "h" to "text/x-chdr",
        "sh" to "application/x-sh",
        "log" to "text/plain",
        "ini" to "text/plain",
        "cfg" to "text/plain",
        "conf" to "text/plain",
        "properties" to "text/plain",

        // Archives
        "zip" to "application/zip",
        "rar" to "application/vnd.rar",
        "7z" to "application/x-7z-compressed",
        "tar" to "application/x-tar",
        "gz" to "application/gzip",
        "bz2" to "application/x-bzip2",
        "xz" to "application/x-xz",

        // Fonts
        "ttf" to "font/ttf",
        "otf" to "font/otf",
        "woff" to "font/woff",
        "woff2" to "font/woff2",

        // Misc
        "apk" to "application/vnd.android.package-archive",
        "ipa" to "application/octet-stream",
        "dmg" to "application/x-apple-diskimage",
        "exe" to "application/x-msdownload",
        "deb" to "application/x-deb",
        "rpm" to "application/x-rpm",
        "wasm" to "application/wasm",
        "onnx" to "application/octet-stream",
        "aon" to "application/octet-stream",
        "sqlite" to "application/x-sqlite3",
        "db" to "application/x-sqlite3",
    )

    /** Lookup MIME type from file extension (case-insensitive). */
    fun fromExtension(extension: String): String =
        EXTENSION_TO_MIME[extension.lowercase().trimStart('.')] ?: "application/octet-stream"

    /** Lookup MIME type from filename. */
    fun fromFilename(filename: String): String {
        val ext = filename.substringAfterLast('.', "")
        return if (ext.isEmpty()) "application/octet-stream" else fromExtension(ext)
    }

    /** Check if MIME type is an image. */
    fun isImage(mimeType: String): Boolean = mimeType.startsWith("image/")

    /** Check if MIME type is a video. */
    fun isVideo(mimeType: String): Boolean = mimeType.startsWith("video/")

    /** Check if MIME type is audio. */
    fun isAudio(mimeType: String): Boolean = mimeType.startsWith("audio/")

    /** Check if MIME type is a document (PDF, Office, ODF, text). */
    fun isDocument(mimeType: String): Boolean = mimeType.startsWith("text/") ||
        mimeType == "application/pdf" ||
        mimeType.contains("word") ||
        mimeType.contains("excel") || mimeType.contains("spreadsheet") ||
        mimeType.contains("powerpoint") || mimeType.contains("presentation") ||
        mimeType.contains("opendocument") ||
        mimeType == "application/rtf" ||
        mimeType == "text/csv"

    /** Check if MIME type is an archive. */
    fun isArchive(mimeType: String): Boolean =
        mimeType == "application/zip" ||
        mimeType.contains("rar") ||
        mimeType.contains("7z") ||
        mimeType.contains("tar") ||
        mimeType.contains("gzip") ||
        mimeType.contains("bzip")
}
