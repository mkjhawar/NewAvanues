package com.augmentalis.noteavanue.attachment

/**
 * Constants and utilities for the `att://` URI scheme.
 *
 * NoteAvanue uses a custom URI scheme to reference attachments:
 * ```
 * att://notes/{noteId}/attachments/{attachmentId}
 * att://notes/{noteId}/thumbnails/{attachmentId}
 * ```
 *
 * These URIs are embedded in Markdown content as image/link references:
 * - Photos: `![caption](att://notes/abc123/attachments/def456)`
 * - Audio: `[Recording 1](att://notes/abc123/attachments/ghi789)`
 * - Documents: `[Report.pdf](att://notes/abc123/attachments/jkl012)`
 *
 * The platform-specific [NoteAttachmentResolver] maps these to file paths.
 */
object AttachmentConstants {
    const val SCHEME = "att"
    const val AUTHORITY = "notes"

    /** Regex pattern matching att:// URIs in Markdown content */
    val ATT_URI_PATTERN = Regex("""att://notes/([^/]+)/(?:attachments|thumbnails)/([^/\s)]+)""")

    /**
     * Build an attachment URI.
     *
     * @param noteId The note ID
     * @param attachmentId The attachment ID
     * @return URI string in `att://notes/{noteId}/attachments/{attachmentId}` format
     */
    fun buildUri(noteId: String, attachmentId: String): String =
        "$SCHEME://$AUTHORITY/$noteId/attachments/$attachmentId"

    /**
     * Build a thumbnail URI.
     */
    fun buildThumbnailUri(noteId: String, attachmentId: String): String =
        "$SCHEME://$AUTHORITY/$noteId/thumbnails/$attachmentId"

    /**
     * Check if a URI is an att:// scheme URI.
     */
    fun isAttUri(uri: String): Boolean = uri.startsWith("$SCHEME://")

    /**
     * Extract noteId from an att:// URI.
     *
     * @return The note ID, or null if the URI doesn't match
     */
    fun extractNoteId(uri: String): String? {
        val match = ATT_URI_PATTERN.find(uri) ?: return null
        return match.groupValues[1]
    }

    /**
     * Extract attachmentId from an att:// URI.
     *
     * @return The attachment ID, or null if the URI doesn't match
     */
    fun extractAttachmentId(uri: String): String? {
        val match = ATT_URI_PATTERN.find(uri) ?: return null
        return match.groupValues[2]
    }

    /**
     * Extract all att:// URIs from Markdown content.
     *
     * @param markdownContent The full Markdown text
     * @return List of att:// URIs found in the content
     */
    fun extractAllUris(markdownContent: String): List<String> {
        return ATT_URI_PATTERN.findAll(markdownContent).map { it.value }.toList()
    }

    /**
     * Build a Markdown image reference for a photo attachment.
     */
    fun markdownImage(noteId: String, attachmentId: String, caption: String = ""): String {
        val uri = buildUri(noteId, attachmentId)
        return "![$caption]($uri)"
    }

    /**
     * Build a Markdown link reference for a non-image attachment.
     */
    fun markdownLink(noteId: String, attachmentId: String, displayName: String): String {
        val uri = buildUri(noteId, attachmentId)
        return "[$displayName]($uri)"
    }
}
