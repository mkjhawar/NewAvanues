/**
 * FileUploadHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Last Modified: 2026-01-27 00:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for file upload and file picker operations
 * Features: File picker triggering, upload management, file selection cancellation
 * Location: CommandManager module (handlers package)
 *
 * Changelog:
 * - v1.0.0 (2026-01-27): Initial implementation with file upload voice commands
 */

package com.augmentalis.avamagic.voice.handlers.input

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

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
 * - Singleton pattern for global access
 * - Implements CommandHandler for CommandRegistry integration
 * - Thread-safe state management using StateFlow and AtomicBoolean
 * - Delegates file picker operations to registered listeners
 *
 * Thread Safety:
 * - All public methods are thread-safe
 * - State changes use atomic operations
 * - Coroutine scope managed with SupervisorJob
 *
 * @since 1.0.0
 */
class FileUploadHandler private constructor(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "FileUploadHandler"
        private const val MODULE_ID = "file_upload"

        @Volatile
        private var instance: FileUploadHandler? = null

        /**
         * Get singleton instance
         *
         * Thread-safe: Uses double-checked locking pattern.
         *
         * @param context Application context (will be converted to applicationContext)
         * @return Singleton FileUploadHandler instance
         */
        fun getInstance(context: Context): FileUploadHandler {
            return instance ?: synchronized(this) {
                instance ?: FileUploadHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

        /**
         * Reset singleton instance (for testing only)
         */
        internal fun resetInstance() {
            synchronized(this) {
                instance?.dispose()
                instance = null
            }
        }

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

    // CommandHandler interface implementation
    override val moduleId: String = "fileupload"

    override val supportedCommands: List<String> = listOf(
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

    // Coroutine scope for async operations
    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State management
    private val _uploadState = MutableStateFlow(FileUploadState())
    val uploadState: StateFlow<FileUploadState> = _uploadState.asStateFlow()

    // Upload operation state
    private val isUploading = AtomicBoolean(false)
    private val isPickerOpen = AtomicBoolean(false)

    // Integration state
    private var isInitialized = false
    private var isRegistered = false

    // Listener for file operations
    private var fileOperationListener: FileOperationListener? = null

    // Activity result launcher reference (set by hosting activity)
    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null

    init {
        initialize()
        // Register with CommandRegistry automatically
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize the handler
     *
     * @return true if initialization successful, false otherwise
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            isInitialized = true
            Log.d(TAG, "FileUploadHandler initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * Register commands with VOS4 system
     *
     * @return true if registration successful, false otherwise
     */
    fun registerCommands(): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            return false
        }

        if (isRegistered) {
            Log.w(TAG, "Commands already registered")
            return true
        }

        return try {
            isRegistered = true
            Log.d(TAG, "Registered ${supportedCommands.size} voice commands for file upload")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register commands", e)
            false
        }
    }

    /**
     * Unregister commands from system
     */
    fun unregisterCommands() {
        if (!isRegistered) return

        try {
            isRegistered = false
            Log.d(TAG, "Commands unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering commands", e)
        }
    }

    /**
     * Set file operation listener
     *
     * @param listener Listener to receive file operation callbacks
     */
    fun setFileOperationListener(listener: FileOperationListener?) {
        this.fileOperationListener = listener
    }

    /**
     * Set file picker launcher for activity result handling
     *
     * @param launcher ActivityResultLauncher for file picker intent
     */
    fun setFilePickerLauncher(launcher: ActivityResultLauncher<Intent>?) {
        this.filePickerLauncher = launcher
    }

    /**
     * Handle file selected from picker
     *
     * Called by hosting activity when file picker returns a result.
     *
     * @param uri URI of the selected file
     */
    fun onFileSelected(uri: Uri?) {
        isPickerOpen.set(false)

        if (uri == null) {
            Log.d(TAG, "File selection cancelled by user")
            _uploadState.value = _uploadState.value.copy(
                selectedFile = null,
                status = UploadStatus.IDLE,
                errorMessage = null
            )
            fileOperationListener?.onFileSelectionCancelled()
            return
        }

        Log.d(TAG, "File selected: $uri")

        // Get file info
        val fileInfo = getFileInfo(uri)

        _uploadState.value = _uploadState.value.copy(
            selectedFile = fileInfo,
            status = UploadStatus.FILE_SELECTED,
            errorMessage = null
        )

        fileOperationListener?.onFileSelected(fileInfo)
    }

    // ========== CommandHandler Interface Implementation ==========

    /**
     * Check if this handler can process the command
     * (command is already normalized by CommandRegistry)
     *
     * @param command Normalized command text (lowercase, trimmed)
     * @return true if this handler can process the command
     */
    override fun canHandle(command: String): Boolean {
        return when {
            FILE_PICKER_COMMANDS.contains(command) -> true
            CANCEL_UPLOAD_COMMANDS.contains(command) -> true
            REMOVE_FILE_COMMANDS.contains(command) -> true
            CONFIRM_UPLOAD_COMMANDS.contains(command) -> true
            else -> false
        }
    }

    /**
     * Execute the command
     * (command is already normalized by CommandRegistry)
     *
     * @param command Normalized command text (lowercase, trimmed)
     * @return true if command was successfully executed
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized for command processing")
            return false
        }

        Log.d(TAG, "Processing voice command: '$command'")

        return try {
            when {
                FILE_PICKER_COMMANDS.contains(command) -> openFilePicker()
                CANCEL_UPLOAD_COMMANDS.contains(command) -> cancelUpload()
                REMOVE_FILE_COMMANDS.contains(command) -> removeFile()
                CONFIRM_UPLOAD_COMMANDS.contains(command) -> confirmUpload()
                else -> {
                    Log.w(TAG, "Unknown command: $command")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $command", e)
            false
        }
    }

    // ========== File Operations ==========

    /**
     * Open file picker
     *
     * Launches system file picker for file selection.
     *
     * @return true if picker was successfully opened
     */
    private suspend fun openFilePicker(): Boolean {
        if (isPickerOpen.get()) {
            Log.w(TAG, "File picker already open")
            return false
        }

        return withContext(Dispatchers.Main) {
            try {
                val intent = createFilePickerIntent()

                // Try using launcher first
                val launcher = filePickerLauncher
                if (launcher != null) {
                    launcher.launch(intent)
                    isPickerOpen.set(true)
                    _uploadState.value = _uploadState.value.copy(
                        status = UploadStatus.PICKER_OPEN
                    )
                    Log.d(TAG, "File picker opened via launcher")
                    true
                } else {
                    // Fallback: Notify listener to handle intent
                    val handled = fileOperationListener?.onOpenFilePicker(intent) ?: false
                    if (handled) {
                        isPickerOpen.set(true)
                        _uploadState.value = _uploadState.value.copy(
                            status = UploadStatus.PICKER_OPEN
                        )
                        Log.d(TAG, "File picker opened via listener")
                    } else {
                        Log.e(TAG, "No launcher or listener available to open file picker")
                    }
                    handled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open file picker", e)
                _uploadState.value = _uploadState.value.copy(
                    status = UploadStatus.ERROR,
                    errorMessage = "Failed to open file picker: ${e.message}"
                )
                false
            }
        }
    }

    /**
     * Create intent for file picker
     *
     * @return Intent configured for file selection
     */
    private fun createFilePickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"

            // Allow multiple MIME types
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "image/*",
                "video/*",
                "audio/*",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/*",
                "*/*"
            ))

            // For Android 11+, use Downloads directory as initial location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"))
            }
        }
    }

    /**
     * Cancel current upload operation
     *
     * @return true if cancellation was successful
     */
    private suspend fun cancelUpload(): Boolean {
        return withContext(Dispatchers.Main) {
            val currentStatus = _uploadState.value.status

            when (currentStatus) {
                UploadStatus.UPLOADING -> {
                    // Cancel active upload
                    isUploading.set(false)
                    _uploadState.value = _uploadState.value.copy(
                        status = UploadStatus.CANCELLED,
                        progress = 0f,
                        errorMessage = null
                    )
                    fileOperationListener?.onUploadCancelled()
                    Log.d(TAG, "Upload cancelled")
                    true
                }

                UploadStatus.PICKER_OPEN -> {
                    // Close picker
                    isPickerOpen.set(false)
                    _uploadState.value = _uploadState.value.copy(
                        status = UploadStatus.IDLE
                    )
                    fileOperationListener?.onFileSelectionCancelled()
                    Log.d(TAG, "File picker cancelled")
                    true
                }

                UploadStatus.FILE_SELECTED -> {
                    // Clear selection
                    _uploadState.value = FileUploadState()
                    fileOperationListener?.onFileRemoved()
                    Log.d(TAG, "File selection cleared")
                    true
                }

                else -> {
                    Log.w(TAG, "Nothing to cancel (current status: $currentStatus)")
                    false
                }
            }
        }
    }

    /**
     * Remove currently selected file
     *
     * @return true if file was removed
     */
    private suspend fun removeFile(): Boolean {
        return withContext(Dispatchers.Main) {
            val currentFile = _uploadState.value.selectedFile

            if (currentFile == null) {
                Log.w(TAG, "No file selected to remove")
                return@withContext false
            }

            _uploadState.value = _uploadState.value.copy(
                selectedFile = null,
                status = UploadStatus.IDLE,
                progress = 0f,
                errorMessage = null
            )

            fileOperationListener?.onFileRemoved()
            Log.d(TAG, "File removed: ${currentFile.name}")
            true
        }
    }

    /**
     * Confirm and start upload
     *
     * @return true if upload was started
     */
    private suspend fun confirmUpload(): Boolean {
        return withContext(Dispatchers.Main) {
            val currentState = _uploadState.value

            if (currentState.selectedFile == null) {
                Log.w(TAG, "No file selected for upload")
                _uploadState.value = currentState.copy(
                    status = UploadStatus.ERROR,
                    errorMessage = "No file selected. Say 'upload file' to select a file first."
                )
                return@withContext false
            }

            if (isUploading.get()) {
                Log.w(TAG, "Upload already in progress")
                return@withContext false
            }

            // Start upload
            isUploading.set(true)
            _uploadState.value = currentState.copy(
                status = UploadStatus.UPLOADING,
                progress = 0f,
                errorMessage = null
            )

            val result = fileOperationListener?.onUploadConfirmed(currentState.selectedFile) ?: false

            if (result) {
                Log.d(TAG, "Upload started for: ${currentState.selectedFile.name}")
            } else {
                isUploading.set(false)
                _uploadState.value = _uploadState.value.copy(
                    status = UploadStatus.ERROR,
                    errorMessage = "Failed to start upload"
                )
                Log.e(TAG, "Failed to start upload")
            }

            result
        }
    }

    // ========== Upload Progress Updates ==========

    /**
     * Update upload progress
     *
     * Called by upload implementation to update progress.
     *
     * @param progress Progress value (0.0 to 1.0)
     */
    fun updateProgress(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        _uploadState.value = _uploadState.value.copy(
            progress = clampedProgress
        )
    }

    /**
     * Mark upload as completed
     *
     * @param uploadedUri URI of the uploaded file (if applicable)
     */
    fun onUploadComplete(uploadedUri: Uri? = null) {
        isUploading.set(false)
        _uploadState.value = _uploadState.value.copy(
            status = UploadStatus.COMPLETED,
            progress = 1f,
            uploadedUri = uploadedUri,
            errorMessage = null
        )
        fileOperationListener?.onUploadCompleted(uploadedUri)
        Log.d(TAG, "Upload completed: $uploadedUri")
    }

    /**
     * Mark upload as failed
     *
     * @param error Error message
     */
    fun onUploadFailed(error: String) {
        isUploading.set(false)
        _uploadState.value = _uploadState.value.copy(
            status = UploadStatus.ERROR,
            errorMessage = error
        )
        fileOperationListener?.onUploadFailed(error)
        Log.e(TAG, "Upload failed: $error")
    }

    // ========== Utility Methods ==========

    /**
     * Get file info from URI
     *
     * @param uri File URI
     * @return FileInfo with file details
     */
    private fun getFileInfo(uri: Uri): FileInfo {
        var name = "Unknown"
        var size = 0L
        var mimeType = "application/octet-stream"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // Get display name
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: "Unknown"
                }

                // Get size
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        // Get MIME type
        context.contentResolver.getType(uri)?.let {
            mimeType = it
        }

        return FileInfo(
            uri = uri,
            name = name,
            size = size,
            mimeType = mimeType
        )
    }

    /**
     * Check if ready for operation
     *
     * @return true if handler is initialized and registered
     */
    fun isReady(): Boolean = isInitialized && isRegistered

    /**
     * Get current upload state
     *
     * @return Current FileUploadState
     */
    fun getState(): FileUploadState = _uploadState.value

    /**
     * Get handler status
     *
     * @return FileUploadHandlerStatus with current state information
     */
    fun getStatus(): FileUploadHandlerStatus {
        return FileUploadHandlerStatus(
            isInitialized = isInitialized,
            isRegistered = isRegistered,
            isUploading = isUploading.get(),
            isPickerOpen = isPickerOpen.get(),
            hasSelectedFile = _uploadState.value.selectedFile != null,
            currentStatus = _uploadState.value.status,
            commandsSupported = supportedCommands.size
        )
    }

    /**
     * Reset upload state
     *
     * Clears all upload state to initial values.
     */
    fun resetState() {
        isUploading.set(false)
        isPickerOpen.set(false)
        _uploadState.value = FileUploadState()
        Log.d(TAG, "Upload state reset")
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        unregisterCommands()
        CommandRegistry.unregisterHandler(moduleId)
        handlerScope.cancel()
        fileOperationListener = null
        filePickerLauncher = null
        instance = null
        Log.d(TAG, "FileUploadHandler disposed")
    }
}

// ========== Data Classes ==========

/**
 * Information about a selected file
 *
 * @property uri Content URI of the file
 * @property name Display name of the file
 * @property size File size in bytes
 * @property mimeType MIME type of the file
 */
data class FileInfo(
    val uri: Uri,
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
 * @property uploadedUri URI of uploaded file (after completion)
 * @property errorMessage Error message if status is ERROR
 */
data class FileUploadState(
    val selectedFile: FileInfo? = null,
    val status: UploadStatus = UploadStatus.IDLE,
    val progress: Float = 0f,
    val uploadedUri: Uri? = null,
    val errorMessage: String? = null
)

/**
 * Status of the FileUploadHandler
 *
 * @property isInitialized Whether handler is initialized
 * @property isRegistered Whether commands are registered
 * @property isUploading Whether upload is in progress
 * @property isPickerOpen Whether file picker is open
 * @property hasSelectedFile Whether a file is selected
 * @property currentStatus Current upload status
 * @property commandsSupported Number of supported commands
 */
data class FileUploadHandlerStatus(
    val isInitialized: Boolean,
    val isRegistered: Boolean,
    val isUploading: Boolean,
    val isPickerOpen: Boolean,
    val hasSelectedFile: Boolean,
    val currentStatus: UploadStatus,
    val commandsSupported: Int
)

// ========== Listener Interface ==========

/**
 * Listener for file operation events
 *
 * Implement this interface to receive callbacks for file upload operations.
 */
interface FileOperationListener {
    /**
     * Called when file picker should be opened
     *
     * @param intent Intent for file picker
     * @return true if picker was opened, false otherwise
     */
    fun onOpenFilePicker(intent: Intent): Boolean

    /**
     * Called when a file has been selected
     *
     * @param fileInfo Information about the selected file
     */
    fun onFileSelected(fileInfo: FileInfo)

    /**
     * Called when file selection was cancelled
     */
    fun onFileSelectionCancelled()

    /**
     * Called when selected file was removed
     */
    fun onFileRemoved()

    /**
     * Called when upload is confirmed and should start
     *
     * @param fileInfo File to upload
     * @return true if upload was started, false otherwise
     */
    fun onUploadConfirmed(fileInfo: FileInfo): Boolean

    /**
     * Called when upload was cancelled
     */
    fun onUploadCancelled()

    /**
     * Called when upload completed successfully
     *
     * @param uploadedUri URI of the uploaded file (if applicable)
     */
    fun onUploadCompleted(uploadedUri: Uri?)

    /**
     * Called when upload failed
     *
     * @param error Error message
     */
    fun onUploadFailed(error: String)
}

/**
 * Default implementation of FileOperationListener
 *
 * Provides empty implementations for all methods.
 * Extend this class to override only the methods you need.
 */
open class DefaultFileOperationListener : FileOperationListener {
    override fun onOpenFilePicker(intent: Intent): Boolean = false
    override fun onFileSelected(fileInfo: FileInfo) {}
    override fun onFileSelectionCancelled() {}
    override fun onFileRemoved() {}
    override fun onUploadConfirmed(fileInfo: FileInfo): Boolean = false
    override fun onUploadCancelled() {}
    override fun onUploadCompleted(uploadedUri: Uri?) {}
    override fun onUploadFailed(error: String) {}
}
