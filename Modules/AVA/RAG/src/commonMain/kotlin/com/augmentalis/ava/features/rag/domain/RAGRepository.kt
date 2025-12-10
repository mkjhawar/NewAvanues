// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/domain/RAGRepository.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.domain

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for RAG operations
 *
 * This interface defines the contract for document indexing and retrieval.
 * Implementations handle platform-specific storage and embedding.
 */
interface RAGRepository {
    /**
     * Add a document to the RAG system
     *
     * @param request Document to add
     * @return Result of the operation
     */
    suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult>

    /**
     * Get a document by ID
     */
    suspend fun getDocument(documentId: String): Result<Document?>

    /**
     * List all documents
     *
     * @param status Filter by status (null = all)
     * @return Flow of documents
     */
    fun listDocuments(status: DocumentStatus? = null): Flow<Document>

    /**
     * Delete a document and all its chunks
     */
    suspend fun deleteDocument(documentId: String): Result<Unit>

    /**
     * Process pending documents (extract chunks, generate embeddings)
     *
     * @param documentId Process specific document (null = all pending)
     * @return Number of documents processed
     */
    suspend fun processDocuments(documentId: String? = null): Result<Int>

    /**
     * Search for relevant chunks
     *
     * @param query Search query
     * @return Search results
     */
    suspend fun search(query: SearchQuery): Result<SearchResponse>

    /**
     * Get chunks for a document
     */
    suspend fun getChunks(documentId: String): Result<List<Chunk>>

    /**
     * Get system statistics
     */
    suspend fun getStatistics(): Result<RAGStatistics>

    /**
     * Clear all data (for testing/reset)
     */
    suspend fun clearAll(): Result<Unit>
}

/**
 * Statistics about the RAG system
 */
data class RAGStatistics(
    val totalDocuments: Int,
    val indexedDocuments: Int,
    val pendingDocuments: Int,
    val failedDocuments: Int,
    val totalChunks: Int,
    val storageUsedBytes: Long,
    val lastIndexedAt: String? = null
)
