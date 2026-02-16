package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * An attachment embedded in a rich Note.
 *
 * Notes support inline photos, document references, and sketches.
 * Attachments are stored as JSON in Note.attachmentsJson and rendered
 * inline at the specified position within the note text.
 */
@Serializable
data class NoteAttachment(
    /** Unique identifier */
    val id: String,
    /** File URI (content://, file://, or asset path) */
    val uri: String,
    /** Attachment type */
    val type: AttachmentType,
    /** Display name */
    val name: String = "",
    /** Character offset in note text where this attachment is inserted */
    val textPosition: Int = 0,
    /** Optional caption text below the attachment */
    val caption: String = "",
    /** Thumbnail URI for preview (generated on insert) */
    val thumbnailUri: String = "",
)

@Serializable
enum class AttachmentType {
    /** Photo from camera or gallery */
    PHOTO,
    /** PDF or other document reference */
    DOCUMENT,
    /** Hand-drawn sketch from whiteboard */
    SKETCH,
    /** Audio clip */
    AUDIO,
}
