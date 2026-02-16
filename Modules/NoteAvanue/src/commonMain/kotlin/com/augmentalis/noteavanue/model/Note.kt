package com.augmentalis.noteavanue.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String = "",
    val content: String = "",
    val attachments: List<NoteAttachment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString()
) {
    val wordCount: Int get() = content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val charCount: Int get() = content.length
    val hasAttachments: Boolean get() = attachments.isNotEmpty()
}

@Serializable
data class NoteAttachment(
    val id: String,
    val uri: String,
    val type: AttachmentType,
    val name: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0,
    val insertPosition: Int = -1,
    val caption: String = "",
    val thumbnailUri: String? = null
)

enum class AttachmentType { PHOTO, DOCUMENT, AUDIO, SKETCH, VIDEO }

@Serializable
data class NoteEditorState(
    val note: Note? = null,
    val isEditing: Boolean = false,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val cursorPosition: Int = 0,
    val error: String? = null
)
