/**
 * FileUploadHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Last Modified: 2026-01-28 00:00 PST
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for file upload and file picker operations
 * Features: File picker triggering, upload management, file selection cancellation
 * Location: MagicVoiceHandlers module
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler pattern with executor
 * - v1.0.0 (2026-01-27): Initial implementation with file upload voice commands
 */

package com.augmentalis.avanueui.voice.handlers.input

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for file upload operations.
 *
 * Handles voice commands for:
 * - Opening file picker ("upload file", "choose file", "open file picker")
 * - Cancelling upload ("cancel upload")
 * - Removing selected file ("remove file", "clear file")
 * - Confirming upload ("upload")
 *
 * Design:
 * - Implements BaseHandler for VoiceOS integration
 * - Delegates file operations to FileUploadExecutor
 * - Thread-safe via coroutine execution
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for file upload operations
 * @since 2.0.0
 */
class FileUploadHandler(
    private val executor: FileUploadExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "FileUploadHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Command patterns for matching
        private val FILE_PICKER_COMMANDS = setOf(
            "upload file",
            "choose file",
            "select file",
            "open file picker",
            "pick file",
            "browse files",
            "attach file"
        )

        private val CANCEL_UPLOAD_COMMANDS = setOf(
            "cancel upload",
            "cancel file upload",
            "stop upload",
            "abort upload"
        )

        private val REMOVE_FILE_COMMANDS = setOf(
            "remove file",
            "clear file",
            "delete file",
            "remove selected file",
            "clear selection",
            "deselect file"
        )

        private val CONFIRM_UPLOAD_COMMANDS = setOf(
            "upload",
            "confirm upload",
            "start upload",
            "send file",
            "submit file"
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // File picker commands
        "upload file",
        "choose file",
        "select file",
        "open file picker",
        "pick file",
        "browse files",
        "attach file",

        // Cancel commands
        "cancel upload",
        "cancel file upload",
        "stop upload",
        "abort upload",

        // Remove/clear commands
        "remove file",
        "clear file",
        "delete file",
        "remove selected file",
        "clear selection",
        "deselect file",

        // Confirm commands
        "upload",
        "confirm upload",
        "start upload",
        "send file",
        "submit file"
    )

    /**
     * Callback for voice feedback when file operations complete
     */
    var onFileOperationCompleted: ((operation: String, success: Boolean) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d { "Processing file upload command: $normalizedAction" }

        return try {
            when {
                FILE_PICKER_COMMANDS.contains(normalizedAction) -> handleOpenFilePicker()
                CANCEL_UPLOAD_COMMANDS.contains(normalizedAction) -> handleCancelUpload()
                REMOVE_FILE_COMMANDS.contains(normalizedAction) -> handleRemoveFile()
                CONFIRM_UPLOAD_COMMANDS.contains(normalizedAction) -> handleConfirmUpload()
                else -> {
                    Log.w { "Unknown command: $normalizedAction" }
                    HandlerResult.notHandled()
                }
            }
        } catch (e: Exception) {
            Log.e({ "Error processing command: $normalizedAction" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle open file picker command
     */
    private suspend fun handleOpenFilePicker(): HandlerResult {
        val result = executor.openFilePicker()

        return when (result) {
            is FileUploadResult.PickerOpened -> {
                onFileOperationCompleted?.invoke("open_picker", true)
                HandlerResult.Success(
                    message = "File picker opened",
                    data = mapOf("operation" to "open_picker")
                )
            }
            is FileUploadResult.PickerAlreadyOpen -> {
                HandlerResult.Success(
                    message = "File picker is already open",
                    data = mapOf("operation" to "open_picker", "alreadyOpen" to true)
                )
            }
            is FileUploadResult.Error -> {
                onFileOperationCompleted?.invoke("open_picker", false)
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            is FileUploadResult.NoLauncherAvailable -> {
                HandlerResult.Failure(
                    reason = "No file picker launcher available",
                    recoverable = false,
                    suggestedAction = "Register a file picker launcher first"
                )
            }
            else -> HandlerResult.failure(
                reason = "Unexpected result from file picker",
                recoverable = true
            )
        }
    }

    /**
     * Handle cancel upload command
     */
    private suspend fun handleCancelUpload(): HandlerResult {
        val result = executor.cancelUpload()

        return when (result) {
            is FileUploadResult.UploadCancelled -> {
                onFileOperationCompleted?.invoke("cancel_upload", true)
                HandlerResult.Success(
                    message = "Upload cancelled",
                    data = mapOf("operation" to "cancel_upload")
                )
            }
            is FileUploadResult.PickerClosed -> {
                onFileOperationCompleted?.invoke("close_picker", true)
                HandlerResult.Success(
                    message = "File picker closed",
                    data = mapOf("operation" to "close_picker")
                )
            }
            is FileUploadResult.SelectionCleared -> {
                onFileOperationCompleted?.invoke("clear_selection", true)
                HandlerResult.Success(
                    message = "File selection cleared",
                    data = mapOf("operation" to "clear_selection")
                )
            }
            is FileUploadResult.NothingToCancel -> {
                HandlerResult.Success(
                    message = "Nothing to cancel",
                    data = mapOf("operation" to "cancel", "nothingToCancel" to true)
                )
            }
            is FileUploadResult.Error -> {
                onFileOperationCompleted?.invoke("cancel_upload", false)
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            else -> HandlerResult.failure(
                reason = "Unexpected result from cancel operation",
                recoverable = true
            )
        }
    }

    /**
     * Handle remove file command
     */
    private suspend fun handleRemoveFile(): HandlerResult {
        val result = executor.removeFile()

        return when (result) {
            is FileUploadResult.FileRemoved -> {
                onFileOperationCompleted?.invoke("remove_file", true)
                HandlerResult.Success(
                    message = "File '${result.fileName}' removed",
                    data = mapOf(
                        "operation" to "remove_file",
                        "fileName" to result.fileName
                    )
                )
            }
            is FileUploadResult.NoFileSelected -> {
                HandlerResult.Failure(
                    reason = "No file selected to remove",
                    recoverable = true,
                    suggestedAction = "Select a file first by saying 'upload file'"
                )
            }
            is FileUploadResult.Error -> {
                onFileOperationCompleted?.invoke("remove_file", false)
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            else -> HandlerResult.failure(
                reason = "Unexpected result from remove operation",
                recoverable = true
            )
        }
    }

    /**
     * Handle confirm upload command
     */
    private suspend fun handleConfirmUpload(): HandlerResult {
        val result = executor.confirmUpload()

        return when (result) {
            is FileUploadResult.UploadStarted -> {
                onFileOperationCompleted?.invoke("start_upload", true)
                HandlerResult.Success(
                    message = "Upload started for '${result.fileName}'",
                    data = mapOf(
                        "operation" to "start_upload",
                        "fileName" to result.fileName
                    )
                )
            }
            is FileUploadResult.NoFileSelected -> {
                HandlerResult.Failure(
                    reason = "No file selected for upload",
                    recoverable = true,
                    suggestedAction = "Say 'upload file' to select a file first"
                )
            }
            is FileUploadResult.UploadAlreadyInProgress -> {
                HandlerResult.Success(
                    message = "Upload already in progress",
                    data = mapOf("operation" to "start_upload", "alreadyInProgress" to true)
                )
            }
            is FileUploadResult.Error -> {
                onFileOperationCompleted?.invoke("start_upload", false)
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            else -> HandlerResult.failure(
                reason = "Unexpected result from upload operation",
                recoverable = true
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about a selected file
 *
 * @property uri Content URI of the file (as a String for KMP compatibility)
 * @property name Display name of the file
 * @property size File size in bytes
 * @property mimeType MIME type of the file
 */
data class FileInfo(
    val uri: String,
    val name: String,
    val size: Long,
    val mimeType: String
) {
    /**
     * Get human-readable file size
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Check if file is an image
     */
    fun isImage(): Boolean = mimeType.startsWith("image/")

    /**
     * Check if file is a video
     */
    fun isVideo(): Boolean = mimeType.startsWith("video/")

    /**
     * Check if file is audio
     */
    fun isAudio(): Boolean = mimeType.startsWith("audio/")

    /**
     * Check if file is a document (PDF, DOC, etc.)
     */
    fun isDocument(): Boolean = mimeType.startsWith("application/") || mimeType.startsWith("text/")
}

/**
 * Upload operation status
 */
enum class UploadStatus {
    /** No upload in progress */
    IDLE,

    /** File picker is open */
    PICKER_OPEN,

    /** File has been selected */
    FILE_SELECTED,

    /** Upload is in progress */
    UPLOADING,

    /** Upload completed successfully */
    COMPLETED,

    /** Upload was cancelled */
    CANCELLED,

    /** Upload failed with error */
    ERROR
}

/**
 * State of file upload operation
 *
 * @property selectedFile Currently selected file info
 * @property status Current upload status
 * @property progress Upload progress (0.0 to 1.0)
 * @property uploadedUri URI of uploaded file as a String (after completion)
 * @property errorMessage Error message if status is ERROR
 */
data class FileUploadState(
    val selectedFile: FileInfo? = null,
    val status: UploadStatus = UploadStatus.IDLE,
    val progress: Float = 0f,
    val uploadedUri: String? = null,
    val errorMessage: String? = null
)

/**
 * Status of the FileUploadHandler
 *
 * @property isUploading Whether upload is in progress
 * @property isPickerOpen Whether file picker is open
 * @property hasSelectedFile Whether a file is selected
 * @property currentStatus Current upload status
 */
data class FileUploadHandlerStatus(
    val isUploading: Boolean,
    val isPickerOpen: Boolean,
    val hasSelectedFile: Boolean,
    val currentStatus: UploadStatus
)

// ═══════════════════════════════════════════════════════════════════════════════
// Result Sealed Class
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Result type for file upload operations
 */
sealed class FileUploadResult {
    /** File picker was opened successfully */
    object PickerOpened : FileUploadResult()

    /** File picker is already open */
    object PickerAlreadyOpen : FileUploadResult()

    /** File picker was closed */
    object PickerClosed : FileUploadResult()

    /** No file picker launcher is available */
    object NoLauncherAvailable : FileUploadResult()

    /** File was selected */
    data class FileSelected(val fileInfo: FileInfo) : FileUploadResult()

    /** No file is currently selected */
    object NoFileSelected : FileUploadResult()

    /** File was removed from selection */
    data class FileRemoved(val fileName: String) : FileUploadResult()

    /** File selection was cleared */
    object SelectionCleared : FileUploadResult()

    /** Upload was started */
    data class UploadStarted(val fileName: String) : FileUploadResult()

    /** Upload is already in progress */
    object UploadAlreadyInProgress : FileUploadResult()

    /** Upload was cancelled */
    object UploadCancelled : FileUploadResult()

    /** Nothing to cancel */
    object NothingToCancel : FileUploadResult()

    /** Upload completed successfully */
    data class UploadCompleted(val uploadedUri: String?) : FileUploadResult()

    /** Upload failed */
    data class UploadFailed(val message: String) : FileUploadResult()

    /** Generic error */
    data class Error(val message: String) : FileUploadResult()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for file upload operations.
 *
 * Implementations should:
 * 1. Manage file picker launcher (ActivityResultLauncher)
 * 2. Handle file selection via content resolver
 * 3. Manage upload state and progress
 * 4. Perform actual file upload operations
 *
 * Activity Integration:
 * The executor implementation should be created in the Activity/Fragment
 * that hosts the file picker launcher, and should manage:
 * - ActivityResultLauncher registration
 * - Content resolver access
 * - Upload service/API integration
 */
interface FileUploadExecutor {

    /**
     * Open the file picker.
     *
     * @return Result indicating if picker was opened
     */
    suspend fun openFilePicker(): FileUploadResult

    /**
     * Cancel the current upload or close picker.
     *
     * @return Result indicating what was cancelled
     */
    suspend fun cancelUpload(): FileUploadResult

    /**
     * Remove the currently selected file.
     *
     * @return Result indicating if file was removed
     */
    suspend fun removeFile(): FileUploadResult

    /**
     * Confirm and start the upload.
     *
     * @return Result indicating if upload was started
     */
    suspend fun confirmUpload(): FileUploadResult

    /**
     * Get current upload state.
     *
     * @return Current file upload state
     */
    suspend fun getState(): FileUploadState

    /**
     * Get handler status.
     *
     * @return Current handler status
     */
    suspend fun getStatus(): FileUploadHandlerStatus

    /**
     * Handle file selected callback from picker.
     *
     * Called by the platform when file picker returns a result.
     *
     * @param uri URI of the selected file as a String, or null if cancelled
     */
    suspend fun onFileSelected(uri: String?)

    /**
     * Update upload progress.
     *
     * @param progress Progress value (0.0 to 1.0)
     */
    suspend fun updateProgress(progress: Float)

    /**
     * Mark upload as completed.
     *
     * @param uploadedUri URI of the uploaded file as a String (if applicable)
     */
    suspend fun onUploadComplete(uploadedUri: String?)

    /**
     * Mark upload as failed.
     *
     * @param error Error message
     */
    suspend fun onUploadFailed(error: String)

    /**
     * Reset state to initial values.
     */
    suspend fun resetState()
}
