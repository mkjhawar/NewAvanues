package com.augmentalis.noteavanue.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Source of note content — tracks how the note was created.
 */
@Serializable
enum class NoteSource {
    /** Typed manually by the user */
    MANUAL,
    /** Created via voice dictation */
    DICTATED,
    /** Imported from external source (file, share intent, etc.) */
    IMPORTED
}

/**
 * Core note model — stores Markdown content with metadata.
 *
 * The `markdownContent` field holds the full rich-text note as Markdown.
 * The compose-rich-editor library converts between RichTextState ↔ Markdown
 * so we always persist the Markdown string (portable, searchable).
 */
@Serializable
data class Note(
    val id: String,
    val title: String = "",
    val markdownContent: String = "",
    val folderId: String? = null,
    val attachments: List<NoteAttachment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val source: NoteSource = NoteSource.MANUAL,
    /** Percentage of content originating from voice dictation (0.0–1.0) */
    val voiceOriginPct: Float = 0f,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString()
) {
    val wordCount: Int get() = markdownContent.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val charCount: Int get() = markdownContent.length
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
    val voiceMode: NoteVoiceMode = NoteVoiceMode.COMMANDING,
    val error: String? = null
)
