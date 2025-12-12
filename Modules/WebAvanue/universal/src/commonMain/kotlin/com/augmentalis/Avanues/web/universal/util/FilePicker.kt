package com.augmentalis.Avanues.web.universal.util

/**
 * FilePicker - Cross-platform file picker interface.
 *
 * Provides platform-specific implementations for picking files and saving files.
 * Use expect/actual pattern for KMP compatibility.
 *
 * Platform implementations:
 * - Android: Uses Intent.ACTION_OPEN_DOCUMENT and Intent.ACTION_CREATE_DOCUMENT
 * - iOS: Uses UIDocumentPickerViewController
 * - Desktop: Uses JFileChooser or native file dialogs
 *
 * @since 1.0.0
 */
interface FilePicker {

    /**
     * Opens a file picker to select a file for import.
     *
     * @param mimeTypes List of MIME types to filter (e.g., ["text/html"])
     * @param callback Callback invoked with the selected file's content or null if cancelled
     */
    suspend fun pickFile(
        mimeTypes: List<String> = listOf("*/*"),
        callback: (FilePickerResult?) -> Unit
    )

    /**
     * Opens a file picker to save a file.
     *
     * @param filename Default filename
     * @param content Content to save
     * @param mimeType MIME type of the file
     * @param callback Callback invoked with success status and file path
     */
    suspend fun saveFile(
        filename: String,
        content: String,
        mimeType: String = "text/html",
        callback: (SaveFileResult) -> Unit
    )

    /**
     * Shares a file using the platform's share sheet (Android/iOS).
     *
     * @param filename Filename
     * @param content File content
     * @param mimeType MIME type
     */
    suspend fun shareFile(
        filename: String,
        content: String,
        mimeType: String = "text/html"
    )
}

/**
 * Result of file picking operation.
 *
 * @property filename Name of the picked file
 * @property content Content of the file as string
 * @property uri Platform-specific URI/path to the file
 */
data class FilePickerResult(
    val filename: String,
    val content: String,
    val uri: String?
)

/**
 * Result of file save operation.
 *
 * @property success Whether the save was successful
 * @property filePath Path where the file was saved (null if failed)
 * @property error Error message if failed
 */
data class SaveFileResult(
    val success: Boolean,
    val filePath: String?,
    val error: String? = null
)

/**
 * Expected FilePicker factory function.
 * Platform-specific implementations will provide actual implementations.
 */
expect fun createFilePicker(): FilePicker

/**
 * Helper function to get the Downloads directory path for the current platform.
 *
 * @return Platform-specific downloads directory path
 */
expect fun getDownloadsDirectory(): String
