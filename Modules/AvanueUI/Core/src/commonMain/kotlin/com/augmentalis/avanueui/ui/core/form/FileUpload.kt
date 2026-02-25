package com.augmentalis.avanueui.ui.core.form

import com.augmentalis.avanueui.core.*

/**
 * File upload component for selecting and uploading files.
 *
 * FileUpload provides a user interface for selecting files with support
 * for drag-and-drop, file type filtering, size limits, and multiple files.
 *
 * @property label Upload button/area label
 * @property accept List of accepted MIME types
 * @property multiple Allow multiple file selection (default false)
 * @property maxFileSize Maximum file size in bytes (default null = unlimited)
 * @property maxFiles Maximum number of files when multiple is true (default null = unlimited)
 * @property dragDrop Enable drag-and-drop (default true)
 * @property showPreview Show file preview for images (default false)
 * @property files Currently selected files
 * @property size Component size (default MD)
 * @property enabled Whether user can interact (default true)
 * @since 1.0.0
 */
data class FileUploadComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val label: String = "Choose File",
    val accept: List<String> = emptyList(),
    val multiple: Boolean = false,
    val maxFileSize: Long? = null,
    val maxFiles: Int? = null,
    val dragDrop: Boolean = true,
    val showPreview: Boolean = false,
    val files: List<UploadedFile> = emptyList(),
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true
) : Component {
    init {
        require(label.isNotBlank()) { "Label cannot be blank" }
        if (maxFileSize != null) {
            require(maxFileSize > 0) { "maxFileSize must be positive (got $maxFileSize)" }
        }
        if (maxFiles != null) {
            require(maxFiles > 0) { "maxFiles must be positive (got $maxFiles)" }
            require(multiple) { "maxFiles requires multiple to be true" }
        }
        if (!multiple) {
            require(files.size <= 1) { "Single file mode allows only one file" }
        }
        if (maxFiles != null) {
            require(files.size <= maxFiles) { "Files exceed maxFiles limit of $maxFiles" }
        }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Adds a file to the upload list.
     */
    fun addFile(file: UploadedFile): FileUploadComponent {
        if (!multiple && files.isNotEmpty()) {
            return copy(files = listOf(file))
        }
        if (maxFiles != null && files.size >= maxFiles) {
            return this
        }
        if (maxFileSize != null && file.size > maxFileSize) {
            return this
        }
        if (accept.isNotEmpty()) {
            val matchesAny = accept.any { pattern ->
                file.mimeType.matches(Regex(pattern.replace("*", ".*")))
            }
            if (!matchesAny) return this
        }
        return copy(files = files + file)
    }

    /**
     * Removes a file from the upload list.
     */
    fun removeFile(fileId: String): FileUploadComponent {
        return copy(files = files.filter { it.id != fileId })
    }

    /**
     * Clears all files.
     */
    fun clearFiles(): FileUploadComponent {
        return copy(files = emptyList())
    }

    /**
     * Total size of all files in bytes.
     */
    val totalSize: Long
        get() = files.sumOf { it.size }

    /**
     * Whether the maximum file limit has been reached.
     */
    val isMaxFilesReached: Boolean
        get() = maxFiles != null && files.size >= maxFiles

    /**
     * Whether any files are selected.
     */
    val hasFiles: Boolean
        get() = files.isNotEmpty()

    companion object {
        /**
         * Creates a file upload for images only.
         */
        fun images(label: String = "Upload Image", multiple: Boolean = false) =
            FileUploadComponent(
                label = label,
                accept = listOf("image/jpeg", "image/png", "image/gif", "image/webp"),
                multiple = multiple,
                showPreview = true
            )

        /**
         * Creates a file upload for documents.
         */
        fun documents(label: String = "Upload Document", multiple: Boolean = false) =
            FileUploadComponent(
                label = label,
                accept = listOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ),
                multiple = multiple
            )

        /**
         * Creates a file upload for videos.
         */
        fun videos(label: String = "Upload Video", multiple: Boolean = false) =
            FileUploadComponent(
                label = label,
                accept = listOf("video/mp4", "video/webm", "video/ogg"),
                multiple = multiple
            )

        /**
         * Creates a drag-and-drop upload area.
         */
        fun dropzone(label: String = "Drop files here", multiple: Boolean = true) =
            FileUploadComponent(
                label = label,
                dragDrop = true,
                multiple = multiple
            )
    }
}

/**
 * Represents an uploaded file.
 *
 * @property id Unique file identifier
 * @property name File name
 * @property size File size in bytes
 * @property mimeType MIME type
 * @property uploadProgress Upload progress (0.0 to 1.0, null if not started)
 * @property url URL to uploaded file (null if not yet uploaded)
 * @property error Error message if upload failed
 */
data class UploadedFile(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val uploadProgress: Float? = null,
    val url: String? = null,
    val error: String? = null
) {
    init {
        require(id.isNotBlank()) { "File ID cannot be blank" }
        require(name.isNotBlank()) { "File name cannot be blank" }
        require(size >= 0) { "File size must be non-negative (got $size)" }
        require(mimeType.isNotBlank()) { "MIME type cannot be blank" }
        if (uploadProgress != null) {
            require(uploadProgress in 0f..1f) {
                "Upload progress must be between 0.0 and 1.0 (got $uploadProgress)"
            }
        }
    }

    /**
     * Whether the file is currently uploading.
     */
    val isUploading: Boolean
        get() = uploadProgress != null && uploadProgress < 1f

    /**
     * Whether the upload is complete.
     */
    val isUploaded: Boolean
        get() = uploadProgress == 1f && url != null

    /**
     * Whether the upload failed.
     */
    val hasFailed: Boolean
        get() = error != null

    /**
     * File size formatted as human-readable string.
     */
    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }

    /**
     * Whether the file is an image.
     */
    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    /**
     * Whether the file is a video.
     */
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    /**
     * Whether the file is audio.
     */
    val isAudio: Boolean
        get() = mimeType.startsWith("audio/")

    /**
     * File extension from name.
     */
    val extension: String
        get() = name.substringAfterLast(".", "")

    companion object {
        /**
         * Creates a pending file (not yet uploaded).
         */
        fun pending(id: String, name: String, size: Long, mimeType: String) =
            UploadedFile(id, name, size, mimeType, uploadProgress = null)

        /**
         * Creates a file with upload progress.
         */
        fun uploading(id: String, name: String, size: Long, mimeType: String, progress: Float) =
            UploadedFile(id, name, size, mimeType, uploadProgress = progress)

        /**
         * Creates a completed upload.
         */
        fun completed(id: String, name: String, size: Long, mimeType: String, url: String) =
            UploadedFile(id, name, size, mimeType, uploadProgress = 1f, url = url)

        /**
         * Creates a failed upload.
         */
        fun failed(id: String, name: String, size: Long, mimeType: String, error: String) =
            UploadedFile(id, name, size, mimeType, error = error)
    }
}
