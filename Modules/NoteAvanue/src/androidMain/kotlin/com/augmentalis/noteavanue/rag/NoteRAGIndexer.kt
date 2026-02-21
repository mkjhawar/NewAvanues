package com.augmentalis.noteavanue.rag

import android.util.Log
import com.augmentalis.noteavanue.model.Note
import com.augmentalis.rag.domain.AddDocumentRequest
import com.augmentalis.rag.domain.DocumentType
import com.augmentalis.rag.domain.RAGRepository
import com.augmentalis.rag.domain.SearchQuery
import com.augmentalis.rag.domain.SearchResult
import com.augmentalis.rag.domain.SearchFilters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.File

private const val TAG = "NoteRAGIndexer"

/**
 * Bridges NoteAvanue → AI/RAG module for on-device semantic search.
 *
 * When a note is saved, this indexer:
 * 1. Writes the Markdown content to a temp file (RAG expects file paths)
 * 2. Submits it as a Markdown document via [RAGRepository.addDocument]
 * 3. Triggers processing (chunk → embed → store)
 *
 * Notes are indexed as `DocumentType.MD` with metadata:
 * - `source = "noteavanue"`
 * - `noteId = <note.id>`
 * - `voiceOriginPct = <pct>`
 * - `folderId = <folderId>`
 * - `tags = <comma-separated>`
 *
 * Search is exposed via [searchNotes] which wraps [RAGRepository.search]
 * with NoteAvanue-scoped filters.
 */
class NoteRAGIndexer(
    private val ragRepository: RAGRepository,
    private val cacheDir: File
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val indexMutex = Mutex()

    /**
     * Index or re-index a note in the RAG system.
     *
     * Writes markdown to a temp file, then submits via RAG pipeline.
     * Idempotent — re-indexing the same noteId replaces previous chunks.
     *
     * @param note The note to index
     */
    suspend fun indexNote(note: Note) {
        if (note.markdownContent.isBlank() && note.title.isBlank()) {
            Log.d(TAG, "Skipping empty note ${note.id}")
            return
        }

        indexMutex.withLock {
            try {
                // Build full content with title as H1 for better RAG context
                val fullContent = buildString {
                    if (note.title.isNotBlank()) {
                        appendLine("# ${note.title}")
                        appendLine()
                    }
                    append(note.markdownContent)
                }

                // Write to temp file (RAG AddDocumentRequest takes a file path)
                val tempFile = File(cacheDir, "rag_note_${note.id}.md")
                tempFile.parentFile?.mkdirs()
                tempFile.writeText(fullContent)

                // Delete existing document to replace
                val docId = noteDocumentId(note.id)
                ragRepository.deleteDocument(docId)

                // Build metadata for NoteAvanue-scoped filtering
                val metadata = buildMap {
                    put("source", "noteavanue")
                    put("noteId", note.id)
                    put("voiceOriginPct", note.voiceOriginPct.toString())
                    note.folderId?.let { put("folderId", it) }
                    if (note.tags.isNotEmpty()) {
                        put("tags", note.tags.joinToString(","))
                    }
                    if (note.isPinned) put("pinned", "true")
                }

                // Submit document to RAG
                val request = AddDocumentRequest(
                    filePath = tempFile.absolutePath,
                    title = note.title.ifBlank { "Untitled Note" },
                    metadata = metadata,
                    processImmediately = true
                )

                val result = ragRepository.addDocument(request)
                result.onSuccess { addResult ->
                    Log.i(TAG, "Indexed note ${note.id}: docId=${addResult.documentId}, status=${addResult.status}")

                    // Trigger processing (chunk + embed)
                    ragRepository.processDocuments(addResult.documentId)
                        .onSuccess { count ->
                            Log.i(TAG, "Processed $count documents for note ${note.id}")
                        }
                        .onFailure { e ->
                            Log.w(TAG, "Failed to process note ${note.id}: ${e.message}")
                        }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to index note ${note.id}: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error indexing note ${note.id}", e)
            }
        }
    }

    /**
     * Index a note asynchronously (fire-and-forget from save path).
     */
    fun indexNoteAsync(note: Note) {
        scope.launch {
            indexNote(note)
        }
    }

    /**
     * Remove a note from the RAG index.
     *
     * @param noteId The note ID to remove
     */
    suspend fun removeNote(noteId: String) {
        try {
            val docId = noteDocumentId(noteId)
            ragRepository.deleteDocument(docId).onSuccess {
                Log.i(TAG, "Removed note $noteId from RAG index")
                // Clean up temp file
                File(cacheDir, "rag_note_$noteId.md").delete()
            }.onFailure { e ->
                Log.w(TAG, "Failed to remove note $noteId from RAG: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing note $noteId", e)
        }
    }

    /**
     * Search notes using semantic search.
     *
     * Scoped to NoteAvanue documents only (metadata.source == "noteavanue").
     *
     * @param query Natural language search query
     * @param maxResults Maximum number of results
     * @param folderId Optional folder scope
     * @return List of matching search results with note context
     */
    suspend fun searchNotes(
        query: String,
        maxResults: Int = 10,
        folderId: String? = null
    ): List<NoteSearchResult> {
        try {
            val filters = SearchFilters(
                documentTypes = listOf(DocumentType.MD),
                metadata = buildMap {
                    put("source", "noteavanue")
                    folderId?.let { put("folderId", it) }
                }
            )

            val searchQuery = SearchQuery(
                query = query,
                maxResults = maxResults,
                minSimilarity = 0.4f,
                filters = filters,
                includeContent = true
            )

            val response = ragRepository.search(searchQuery)
            return response.getOrNull()?.results?.map { result ->
                NoteSearchResult(
                    noteId = result.document?.metadata?.get("noteId") ?: "",
                    noteTitle = result.document?.title ?: "",
                    matchedChunkContent = result.chunk.content,
                    similarity = result.similarity,
                    section = result.chunk.metadata.section,
                    heading = result.chunk.metadata.heading
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Deterministic RAG document ID for a note.
     * Using a fixed prefix ensures we can delete/replace by note ID.
     */
    private fun noteDocumentId(noteId: String): String = "noteavanue_$noteId"

    /**
     * Cancel the internal coroutine scope, stopping any in-flight indexing work.
     * Call when the owning component (ViewModel, DI scope) is destroyed.
     */
    override fun close() {
        scope.cancel()
    }
}

/**
 * Search result scoped to NoteAvanue, with note-specific context.
 */
data class NoteSearchResult(
    /** ID of the matching note */
    val noteId: String,
    /** Title of the matching note */
    val noteTitle: String,
    /** The chunk content that matched the query */
    val matchedChunkContent: String,
    /** Semantic similarity score (0.0–1.0) */
    val similarity: Float,
    /** Section heading where the match occurred */
    val section: String? = null,
    /** Sub-heading where the match occurred */
    val heading: String? = null
)
