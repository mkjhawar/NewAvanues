package com.augmentalis.avaelements.components.form

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * FileUpload Component
 *
 * A file upload component with drag-and-drop support.
 *
 * Features:
 * - Click to browse files
 * - Drag-and-drop support
 * - Multiple file selection
 * - File type filtering
 * - File size limits
 * - Upload progress tracking
 * - File preview support
 *
 * Platform mappings:
 * - Android: Intent for file selection
 * - iOS: UIDocumentPickerViewController
 * - Web: Input type="file" with drag-drop
 *
 * Usage:
 * ```kotlin
 * FileUpload(
 *     accept = listOf("image/wildcard", ".pdf", ".doc"),
 *     multiple = true,
 *     maxSize = 5 * 1024 * 1024, // 5MB
 *     placeholder = "Drop files here or click to browse",
 *     onFilesSelected = { files ->
 *         files.forEach { println("Selected: dollar{it.name}") }
 *     }
 * )
 * ```
 */
data class FileUploadComponent(
    override val type: String = "FileUpload",
    val accept: List<String> = emptyList(),
    val multiple: Boolean = false,
    val maxSize: Long? = null,
    val placeholder: String = "Choose file(s)",
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onFilesSelected: ((List<FileData>) -> Unit)? = null
) : Component {
    init {
        if (maxSize != null) {
            require(maxSize > 0) { "Max file size must be positive" }
        }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * File data representation
 */
data class FileData(
    val name: String,
    val size: Long,
    val type: String,
    val data: ByteArray
) {
    init {
        require(name.isNotBlank()) { "File name cannot be blank" }
        require(size >= 0) { "File size must be non-negative" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FileData

        if (name != other.name) return false
        if (size != other.size) return false
        if (type != other.type) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}