package com.augmentalis.noteavanue.repository

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.noteavanue.model.AttachmentType
import com.augmentalis.noteavanue.model.Note
import com.augmentalis.noteavanue.model.NoteAttachment
import com.augmentalis.noteavanue.model.NoteFolder
import com.augmentalis.noteavanue.model.NoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers

/**
 * SQLDelight-backed implementation of [INoteRepository].
 *
 * Maps between domain models ([Note], [NoteFolder], [NoteAttachment])
 * and SQLDelight-generated row types. Uses coroutine Flow for reactive updates.
 *
 * @param database The unified VoiceOSDatabase instance
 */
class NoteRepositoryImpl(
    private val database: VoiceOSDatabase
) : INoteRepository {

    private val noteQueries get() = database.noteEntityQueries
    private val folderQueries get() = database.noteFolderQueries
    private val attachmentQueries get() = database.noteAttachmentQueries

    // ═══════════════════════════════════════════════════════════════════
    // Notes
    // ═══════════════════════════════════════════════════════════════════

    override fun getAllNotes(): Flow<List<Note>> =
        noteQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toNote() } }

    override suspend fun getNoteById(id: String): Note? =
        noteQueries.selectById(id).executeAsOneOrNull()?.toNote()

    override fun getNotesByFolder(folderId: String): Flow<List<Note>> =
        noteQueries.selectByFolder(folderId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toNote() } }

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteQueries.searchByText(query, query)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toNote() } }

    override fun getRecentNotes(limit: Int): Flow<List<Note>> =
        noteQueries.selectRecent(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toNote() } }

    override suspend fun saveNote(note: Note) {
        val now = Clock.System.now().toString()
        noteQueries.insertOrReplace(
            id = note.id,
            title = note.title,
            markdown_content = note.markdownContent,
            folder_id = note.folderId,
            is_pinned = if (note.isPinned) 1L else 0L,
            is_locked = if (note.isLocked) 1L else 0L,
            source = note.source.name,
            voice_origin_pct = note.voiceOriginPct.toDouble(),
            word_count = note.wordCount.toLong(),
            tags_json = Json.encodeToString(note.tags),
            created_at = note.createdAt.ifBlank { now },
            updated_at = now
        )
    }

    override suspend fun updateContent(id: String, markdownContent: String, wordCount: Int) {
        val now = Clock.System.now().toString()
        noteQueries.updateContent(markdownContent, wordCount.toLong(), now, id)
    }

    override suspend fun togglePin(id: String) {
        val now = Clock.System.now().toString()
        noteQueries.togglePin(now, id)
    }

    override suspend fun moveToFolder(noteId: String, folderId: String?) {
        val now = Clock.System.now().toString()
        noteQueries.moveToFolder(folderId, now, noteId)
    }

    override suspend fun deleteNote(id: String) {
        attachmentQueries.deleteByNote(id)
        noteQueries.deleteById(id)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Folders
    // ═══════════════════════════════════════════════════════════════════

    override fun getAllFolders(): Flow<List<NoteFolder>> =
        folderQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toFolder() } }

    override fun getRootFolders(): Flow<List<NoteFolder>> =
        folderQueries.selectRootFolders()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toFolder() } }

    override suspend fun saveFolder(folder: NoteFolder) {
        val now = Clock.System.now().toString()
        folderQueries.insertOrReplace(
            id = folder.id,
            name = folder.name,
            parent_id = folder.parentId,
            icon = folder.icon,
            sort_order = folder.sortOrder.toLong(),
            is_smart_folder = if (folder.isSmartFolder) 1L else 0L,
            smart_filter = folder.smartFilter,
            created_at = folder.createdAt.ifBlank { now },
            updated_at = now
        )
    }

    override suspend fun deleteFolder(id: String) {
        database.transaction {
            // Move notes in this folder to root (null folder)
            val now = Clock.System.now().toString()
            noteQueries.selectByFolder(id).executeAsList().forEach { note ->
                noteQueries.moveToFolder(null, now, note.id)
            }
            folderQueries.deleteById(id)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Attachments
    // ═══════════════════════════════════════════════════════════════════

    override fun getAttachments(noteId: String): Flow<List<NoteAttachment>> =
        attachmentQueries.selectByNote(noteId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toAttachment() } }

    override suspend fun addAttachment(noteId: String, attachment: NoteAttachment) {
        val now = Clock.System.now().toString()
        attachmentQueries.insert(
            id = attachment.id,
            note_id = noteId,
            type = attachment.type.name,
            uri = attachment.uri,
            name = attachment.name,
            mime_type = attachment.mimeType,
            file_size_bytes = attachment.fileSizeBytes,
            block_offset = attachment.insertPosition.toLong(),
            caption = attachment.caption,
            thumbnail_uri = attachment.thumbnailUri,
            created_at = now
        )
    }

    override suspend fun removeAttachment(id: String) {
        attachmentQueries.deleteById(id)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Mapping Extensions
    // ═══════════════════════════════════════════════════════════════════

    private fun com.augmentalis.database.Note_entity.toNote(): Note = Note(
        id = id,
        title = title,
        markdownContent = markdown_content,
        folderId = folder_id,
        isPinned = is_pinned == 1L,
        isLocked = is_locked == 1L,
        source = try { NoteSource.valueOf(source) } catch (_: Exception) { NoteSource.MANUAL },
        voiceOriginPct = voice_origin_pct.toFloat(),
        tags = parseTags(tags_json),
        createdAt = created_at,
        updatedAt = updated_at
    )

    private fun com.augmentalis.database.Note_folder.toFolder(): NoteFolder = NoteFolder(
        id = id,
        name = name,
        parentId = parent_id,
        icon = icon,
        sortOrder = sort_order.toInt(),
        isSmartFolder = is_smart_folder == 1L,
        smartFilter = smart_filter,
        createdAt = created_at,
        updatedAt = updated_at
    )

    private fun com.augmentalis.database.Note_attachment.toAttachment(): NoteAttachment = NoteAttachment(
        id = id,
        uri = uri,
        type = try { AttachmentType.valueOf(type) } catch (_: Exception) { AttachmentType.DOCUMENT },
        name = name,
        mimeType = mime_type,
        fileSizeBytes = file_size_bytes,
        insertPosition = block_offset.toInt(),
        caption = caption,
        thumbnailUri = thumbnail_uri
    )

    private fun parseTags(json: String): List<String> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            Json.decodeFromString<List<String>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
