package com.augmentalis.commandmanager

/**
 * Platform-agnostic interface for file operations during import/export.
 *
 * This interface abstracts platform-specific file access mechanisms:
 * - **Android**: Uses Storage Access Framework (SAF) with content URIs
 * - **iOS**: Uses document picker with file URLs
 * - **Desktop**: Uses native file dialogs with file paths
 *
 * By abstracting file I/O, the core export/import logic in [ICommandExporter]
 * and [ICommandImporter] remains platform-independent and testable.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Export flow
 * val exporter: ICommandExporter = ...
 * val fileProvider: IExportFileProvider = ...
 *
 * val package = exporter.exportAll()
 * val json = Json.encodeToString(package)
 *
 * val result = fileProvider.writeExport(json, "my_commands_backup.json")
 * result.onSuccess { uri ->
 *     showToast("Exported to: $uri")
 * }.onFailure { error ->
 *     showError("Export failed: ${error.message}")
 * }
 *
 * // Import flow
 * val importResult = fileProvider.readImport(selectedFileUri)
 * importResult.onSuccess { json ->
 *     val package = Json.decodeFromString<ExportPackage>(json)
 *     val preview = importer.preview(package)
 *     // Show preview UI...
 * }
 * ```
 *
 * ## Platform Implementation Notes
 *
 * ### Android
 * - Use `Intent.ACTION_CREATE_DOCUMENT` for writing exports
 * - Use `Intent.ACTION_OPEN_DOCUMENT` for reading imports
 * - Content URIs (content://) should be used, not file paths
 * - Request persistable URI permissions for long-term access
 *
 * ### iOS
 * - Use `UIDocumentPickerViewController` for both operations
 * - URLs will be file:// scheme
 * - Handle security-scoped resource access
 *
 * ### Desktop (JVM)
 * - Use `java.awt.FileDialog` or JavaFX `FileChooser`
 * - Direct file paths can be used
 *
 * ## File Format
 *
 * Export files are JSON with `.json` extension:
 * - MIME type: `application/json`
 * - Encoding: UTF-8
 * - Suggested filename pattern: `voiceos_commands_export_YYYYMMDD.json`
 *
 * @see ICommandExporter
 * @see ICommandImporter
 * @see ExportPackage
 */
interface IExportFileProvider {

    /**
     * Write export data to a file.
     *
     * Opens a platform-native file save dialog and writes the provided
     * JSON string to the selected location.
     *
     * On Android, this typically triggers the Storage Access Framework
     * document creation flow. The user can choose the save location.
     *
     * @param data JSON string to write (should be a serialized [ExportPackage])
     * @param suggestedName Suggested filename shown in save dialog.
     *                      Defaults to "voiceos_commands_export.json"
     * @return [Result] containing the file path/URI on success, or the
     *         exception on failure
     *
     * Possible failures:
     * - User cancelled the save dialog
     * - No write permission to selected location
     * - Insufficient storage space
     * - I/O error during write
     */
    suspend fun writeExport(
        data: String,
        suggestedName: String = "voiceos_commands_export.json"
    ): Result<String>

    /**
     * Read import data from a file.
     *
     * Opens a platform-native file picker dialog (if fileUri is empty)
     * or reads from the specified URI/path.
     *
     * On Android, content URIs from the Storage Access Framework are
     * expected. On desktop, file paths are used directly.
     *
     * @param fileUri URI or path to the file to read. On Android, this
     *                should be a content:// URI from document picker
     * @return [Result] containing the file contents as String on success,
     *         or the exception on failure
     *
     * Possible failures:
     * - User cancelled the file picker
     * - File not found
     * - No read permission
     * - File is not valid UTF-8 text
     * - I/O error during read
     */
    suspend fun readImport(fileUri: String): Result<String>

    /**
     * Get default export directory.
     *
     * Returns the platform-appropriate default directory for exports.
     * This is used as a hint for file dialogs but may be overridden
     * by the user.
     *
     * Platform defaults:
     * - **Android**: `Downloads` directory or app-specific documents
     * - **iOS**: App's document directory
     * - **Desktop**: User's home directory or Documents folder
     *
     * @return Path string to the default export directory
     */
    fun getDefaultExportDirectory(): String

    companion object {
        /**
         * Default filename for exports.
         */
        const val DEFAULT_EXPORT_FILENAME = "voiceos_commands_export.json"

        /**
         * MIME type for export files.
         */
        const val EXPORT_MIME_TYPE = "application/json"

        /**
         * File extension for exports.
         */
        const val EXPORT_FILE_EXTENSION = ".json"

        /**
         * Generate a timestamped export filename.
         *
         * @param timestamp Unix timestamp in milliseconds
         * @return Filename like "voiceos_commands_export_20260122.json"
         */
        fun generateExportFilename(timestamp: Long): String {
            // Simple date formatting without external dependencies
            // Implementations can use platform-specific formatting
            return "voiceos_commands_export_$timestamp$EXPORT_FILE_EXTENSION"
        }
    }
}
