package com.augmentalis.chat.coordinator

import com.augmentalis.chat.domain.SourceCitation
import kotlinx.coroutines.flow.StateFlow

/**
 * RAG Coordinator Interface - Cross-platform RAG integration
 *
 * Abstracts RAG context retrieval for cross-platform use in KMP.
 * Provides:
 * - Document context retrieval
 * - Source citation extraction
 * - RAG settings management
 *
 * Graceful degradation: If RAG repository is null, returns empty context.
 *
 * @see RAGCoordinator for Android implementation
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
interface IRAGCoordinator {
    // ==================== State ====================

    /**
     * Recent source citations from RAG retrieval
     */
    val recentSourceCitations: StateFlow<List<SourceCitation>>

    // ==================== Settings ====================

    /**
     * Indicates whether RAG-enhanced responses are enabled
     */
    val ragEnabled: StateFlow<Boolean>

    /**
     * List of selected document IDs for RAG retrieval
     */
    val selectedDocumentIds: StateFlow<List<String>>

    /**
     * Similarity threshold for RAG retrieval (0.0-1.0)
     */
    val ragThreshold: StateFlow<Float>

    // ==================== Retrieval ====================

    /**
     * Result of RAG context retrieval.
     */
    data class RAGResult(
        val context: String?,
        val citations: List<SourceCitation>,
        val retrievalTimeMs: Long
    )

    /**
     * Retrieve RAG context for a user query.
     *
     * @param query User query text
     * @return RAGResult with context, citations, and timing
     */
    suspend fun retrieveContext(query: String): RAGResult

    /**
     * Build a prompt with RAG context included.
     */
    fun buildPromptWithContext(userMessage: String, ragContext: String): String

    // ==================== Settings Management ====================

    /**
     * Enable or disable RAG-enhanced responses.
     */
    fun setRAGEnabled(enabled: Boolean)

    /**
     * Set the list of document IDs to use for RAG retrieval.
     */
    fun setSelectedDocuments(documentIds: List<String>)

    /**
     * Set the similarity threshold for RAG retrieval.
     */
    fun setRAGThreshold(threshold: Float)

    /**
     * Check if RAG is currently active (enabled and documents selected).
     */
    fun isRAGActive(): Boolean

    /**
     * Clear source citations.
     */
    fun clearCitations()
}
