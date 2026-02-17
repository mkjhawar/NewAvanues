package com.augmentalis.noteavanue.repository

import com.augmentalis.noteavanue.model.Note
import com.augmentalis.noteavanue.model.NoteAttachment
import com.augmentalis.noteavanue.model.NoteFolder
import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic repository interface for NoteAvanue persistence.
 *
 * All note, folder, and attachment CRUD goes through this interface.
 * Implementation uses SQLDelight queries generated from the Database module.
 * All list methods return [Flow] for reactive Compose UI updates.
 */
interface INoteRepository {

    // ═══════════════════════════════════════════════════════════════════
    // Notes
    // ═══════════════════════════════════════════════════════════════════

    /** Get all notes (pinned first, then by updated_at DESC) */
    fun getAllNotes(): Flow<List<Note>>

    /** Get a single note by ID */
    suspend fun getNoteById(id: String): Note?

    /** Get notes in a specific folder */
    fun getNotesByFolder(folderId: String): Flow<List<Note>>

    /** Full-text search across title and content */
    fun searchNotes(query: String): Flow<List<Note>>

    /** Get recent notes (limited) */
    fun getRecentNotes(limit: Int = 20): Flow<List<Note>>

    /** Save a note (insert or update) */
    suspend fun saveNote(note: Note)

    /** Update just the content (fast auto-save path) */
    suspend fun updateContent(id: String, markdownContent: String, wordCount: Int)

    /** Toggle pin status */
    suspend fun togglePin(id: String)

    /** Move note to a different folder */
    suspend fun moveToFolder(noteId: String, folderId: String?)

    /** Delete a note */
    suspend fun deleteNote(id: String)

    // ═══════════════════════════════════════════════════════════════════
    // Folders
    // ═══════════════════════════════════════════════════════════════════

    /** Get all folders */
    fun getAllFolders(): Flow<List<NoteFolder>>

    /** Get root-level folders (no parent) */
    fun getRootFolders(): Flow<List<NoteFolder>>

    /** Save a folder */
    suspend fun saveFolder(folder: NoteFolder)

    /** Delete a folder (notes in it become parentless) */
    suspend fun deleteFolder(id: String)

    // ═══════════════════════════════════════════════════════════════════
    // Attachments
    // ═══════════════════════════════════════════════════════════════════

    /** Get all attachments for a note */
    fun getAttachments(noteId: String): Flow<List<NoteAttachment>>

    /** Add an attachment to a note */
    suspend fun addAttachment(noteId: String, attachment: NoteAttachment)

    /** Remove an attachment */
    suspend fun removeAttachment(id: String)
}
