package com.augmentalis.noteavanue.attachment

import android.content.Context
import android.net.Uri
import android.util.Log
import com.augmentalis.noteavanue.model.AttachmentType
import com.augmentalis.noteavanue.model.NoteAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

private const val TAG = "NoteAttachmentResolver"

/**
 * Resolves and manages note attachment storage on Android.
 *
 * Attachments are stored in the app's internal storage under:
 * `files/note_attachments/{noteId}/{attachmentId}.{ext}`
 *
 * The `att://` URI scheme maps to this local path:
 * `att://notes/{noteId}/attachments/{attachmentId}` → file path
 *
 * This class handles:
 * - Copying picked/captured files into the managed directory
 * - Resolving `att://` URIs to `File` objects
 * - Cleaning up attachments when notes are deleted
 * - Thumbnail generation for image attachments
 */
class NoteAttachmentResolver(
    private val context: Context
) {
    private val attachmentsRoot: File
        get() = File(context.filesDir, "note_attachments").also { it.mkdirs() }

    /**
     * Import an attachment from a content URI (gallery, file picker, etc.).
     *
     * Copies the content to the managed directory and returns a [NoteAttachment].
     *
     * @param noteId The note this attachment belongs to
     * @param contentUri Android content:// or file:// URI
     * @param type The attachment type
     * @param displayName Human-readable name for the attachment
     * @param mimeType MIME type of the content
     * @return The created NoteAttachment with a local URI
     */
    suspend fun importAttachment(
        noteId: String,
        contentUri: Uri,
        type: AttachmentType,
        displayName: String,
        mimeType: String
    ): NoteAttachment = withContext(Dispatchers.IO) {
        val attachmentId = UUID.randomUUID().toString()
        val extension = extensionFromMime(mimeType)
        val noteDir = File(attachmentsRoot, noteId).also { it.mkdirs() }
        val targetFile = File(noteDir, "$attachmentId.$extension")

        // Copy content from URI to managed storage
        try {
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Cannot open input stream for $contentUri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy attachment: ${e.message}", e)
            throw e
        }

        val fileSize = targetFile.length()
        val localUri = "att://notes/$noteId/attachments/$attachmentId"

        // Generate thumbnail for images
        val thumbnailUri = if (type == AttachmentType.PHOTO) {
            generateThumbnail(noteId, attachmentId, targetFile)
        } else {
            null
        }

        Log.i(TAG, "Imported attachment: $displayName ($fileSize bytes) → $localUri")

        NoteAttachment(
            id = attachmentId,
            uri = localUri,
            type = type,
            name = displayName,
            mimeType = mimeType,
            fileSizeBytes = fileSize,
            thumbnailUri = thumbnailUri
        )
    }

    /**
     * Import from a raw input stream (e.g., camera capture output).
     */
    suspend fun importFromStream(
        noteId: String,
        inputStream: InputStream,
        type: AttachmentType,
        displayName: String,
        mimeType: String
    ): NoteAttachment = withContext(Dispatchers.IO) {
        val attachmentId = UUID.randomUUID().toString()
        val extension = extensionFromMime(mimeType)
        val noteDir = File(attachmentsRoot, noteId).also { it.mkdirs() }
        val targetFile = File(noteDir, "$attachmentId.$extension")

        try {
            targetFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write stream: ${e.message}", e)
            throw e
        }

        val fileSize = targetFile.length()
        val localUri = "att://notes/$noteId/attachments/$attachmentId"

        val thumbnailUri = if (type == AttachmentType.PHOTO) {
            generateThumbnail(noteId, attachmentId, targetFile)
        } else {
            null
        }

        NoteAttachment(
            id = attachmentId,
            uri = localUri,
            type = type,
            name = displayName,
            mimeType = mimeType,
            fileSizeBytes = fileSize,
            thumbnailUri = thumbnailUri
        )
    }

    /**
     * Resolve an `att://` URI to a file system path.
     *
     * @param attUri URI in format `att://notes/{noteId}/attachments/{attachmentId}`
     * @return The File on disk, or null if not found
     */
    fun resolve(attUri: String): File? {
        val parsed = parseAttUri(attUri) ?: return null
        val noteDir = File(attachmentsRoot, parsed.noteId)
        if (!noteDir.exists()) return null

        // Find the file matching the attachment ID (any extension)
        return noteDir.listFiles()?.firstOrNull { file ->
            file.nameWithoutExtension == parsed.attachmentId
        }
    }

    /**
     * Delete a single attachment from disk.
     */
    suspend fun deleteAttachment(attUri: String) = withContext(Dispatchers.IO) {
        val file = resolve(attUri)
        if (file != null && file.exists()) {
            file.delete()
            Log.d(TAG, "Deleted attachment: $attUri")
        }

        // Also delete thumbnail if exists
        val parsed = parseAttUri(attUri)
        if (parsed != null) {
            val thumbFile = File(
                File(attachmentsRoot, "${parsed.noteId}/thumbnails"),
                "${parsed.attachmentId}_thumb.jpg"
            )
            if (thumbFile.exists()) thumbFile.delete()
        }
    }

    /**
     * Delete all attachments for a note (called when note is deleted).
     */
    suspend fun deleteAllForNote(noteId: String) = withContext(Dispatchers.IO) {
        val noteDir = File(attachmentsRoot, noteId)
        if (noteDir.exists()) {
            noteDir.deleteRecursively()
            Log.i(TAG, "Deleted all attachments for note $noteId")
        }
    }

    /**
     * Get total storage used by attachments for a note.
     */
    fun getStorageUsed(noteId: String): Long {
        val noteDir = File(attachmentsRoot, noteId)
        if (!noteDir.exists()) return 0
        return noteDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════

    private fun generateThumbnail(
        noteId: String,
        attachmentId: String,
        sourceFile: File
    ): String? {
        try {
            val thumbDir = File(attachmentsRoot, "$noteId/thumbnails").also { it.mkdirs() }
            val thumbFile = File(thumbDir, "${attachmentId}_thumb.jpg")

            // Use Android BitmapFactory for thumbnail generation
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            // Calculate sample size for 200px max dimension
            val maxDim = maxOf(options.outWidth, options.outHeight)
            options.inSampleSize = (maxDim / 200).coerceAtLeast(1)
            options.inJustDecodeBounds = false

            val bitmap = android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath, options)
                ?: return null

            thumbFile.outputStream().use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, output)
            }
            bitmap.recycle()

            return "att://notes/$noteId/thumbnails/$attachmentId"
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail generation failed: ${e.message}")
            return null
        }
    }

    private data class ParsedAttUri(val noteId: String, val attachmentId: String)

    private fun parseAttUri(uri: String): ParsedAttUri? {
        // att://notes/{noteId}/attachments/{attachmentId}
        // att://notes/{noteId}/thumbnails/{attachmentId}
        val regex = Regex("""att://notes/([^/]+)/(?:attachments|thumbnails)/([^/]+)""")
        val match = regex.matchEntire(uri) ?: return null
        return ParsedAttUri(
            noteId = match.groupValues[1],
            attachmentId = match.groupValues[2]
        )
    }

    private fun extensionFromMime(mimeType: String): String = when {
        mimeType.startsWith("image/jpeg") -> "jpg"
        mimeType.startsWith("image/png") -> "png"
        mimeType.startsWith("image/webp") -> "webp"
        mimeType.startsWith("image/gif") -> "gif"
        mimeType.startsWith("audio/mpeg") || mimeType.startsWith("audio/mp3") -> "mp3"
        mimeType.startsWith("audio/wav") -> "wav"
        mimeType.startsWith("audio/ogg") -> "ogg"
        mimeType.startsWith("audio/aac") || mimeType.startsWith("audio/mp4") -> "m4a"
        mimeType.startsWith("video/mp4") -> "mp4"
        mimeType.startsWith("video/webm") -> "webm"
        mimeType.startsWith("application/pdf") -> "pdf"
        mimeType.contains("document") -> "docx"
        mimeType.startsWith("text/plain") -> "txt"
        mimeType.startsWith("text/markdown") -> "md"
        else -> "bin"
    }
}
