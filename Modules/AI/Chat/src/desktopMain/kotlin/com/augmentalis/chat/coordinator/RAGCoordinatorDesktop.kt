/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of RAG Coordinator.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.chat.domain.RAGContextBuilder
import com.augmentalis.chat.domain.SourceCitation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop (JVM) implementation of IRAGCoordinator.
 *
 * Provides RAG (Retrieval Augmented Generation) context for LLM responses.
 * On desktop, this can integrate with:
 * - Local document stores
 * - SQLite-based vector storage
 * - REST API-based RAG services
 *
 * This implementation provides the interface with stub functionality
 * that can be extended with actual RAG implementation.
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class RAGCoordinatorDesktop : IRAGCoordinator {

    // ==================== State ====================

    private val _recentSourceCitations = MutableStateFlow<List<SourceCitation>>(emptyList())
    override val recentSourceCitations: StateFlow<List<SourceCitation>> = _recentSourceCitations.asStateFlow()

    private val _ragEnabled = MutableStateFlow(false)
    override val ragEnabled: StateFlow<Boolean> = _ragEnabled.asStateFlow()

    private val _selectedDocumentIds = MutableStateFlow<List<String>>(emptyList())
    override val selectedDocumentIds: StateFlow<List<String>> = _selectedDocumentIds.asStateFlow()

    private val _ragThreshold = MutableStateFlow(0.7f)
    override val ragThreshold: StateFlow<Float> = _ragThreshold.asStateFlow()

    // ==================== Internal ====================

    private val contextBuilder = RAGContextBuilder()

    // Simulated document store (for demo purposes)
    // In production, this would connect to actual RAG storage
    private val documentStore = mutableMapOf<String, DocumentInfo>()

    init {
        println("[RAGCoordinatorDesktop] Initialized (RAG disabled by default)")
    }

    // ==================== Retrieval ====================

    override suspend fun retrieveContext(query: String): IRAGCoordinator.RAGResult {
        val startTime = System.currentTimeMillis()

        if (!_ragEnabled.value || _selectedDocumentIds.value.isEmpty()) {
            return IRAGCoordinator.RAGResult(
                context = null,
                citations = emptyList(),
                retrievalTimeMs = System.currentTimeMillis() - startTime
            )
        }

        println("[RAGCoordinatorDesktop] RAG retrieval for: $query")

        // Keyword-based retrieval: tokenize query → score documents by term frequency → top-3
        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty()) {
            return IRAGCoordinator.RAGResult(
                context = null,
                citations = emptyList(),
                retrievalTimeMs = System.currentTimeMillis() - startTime
            )
        }

        val selectedDocs = _selectedDocumentIds.value.toSet()
        val scoredChunks = documentStore
            .filter { it.key in selectedDocs }
            .flatMap { (_, doc) -> chunkDocument(doc).map { chunk -> chunk to doc } }
            .map { (chunk, doc) ->
                val chunkTerms = tokenize(chunk)
                val score = queryTerms.sumOf { term ->
                    chunkTerms.count { it == term }
                }.toFloat() / chunkTerms.size.coerceAtLeast(1)
                Triple(chunk, doc, score)
            }
            .filter { it.third > 0f }
            .sortedByDescending { it.third }
            .take(3)

        if (scoredChunks.isEmpty()) {
            return IRAGCoordinator.RAGResult(
                context = null,
                citations = emptyList(),
                retrievalTimeMs = System.currentTimeMillis() - startTime
            )
        }

        val context = scoredChunks.joinToString("\n\n---\n\n") { (chunk, doc, _) ->
            "[Source: ${doc.title}]\n$chunk"
        }

        val citations = scoredChunks.map { (_, doc, score) ->
            SourceCitation(
                documentId = doc.id,
                title = doc.title,
                relevanceScore = score,
                snippet = doc.content.take(200)
            )
        }

        _recentSourceCitations.value = citations

        return IRAGCoordinator.RAGResult(
            context = context,
            citations = citations,
            retrievalTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Tokenize text into lowercase terms, filtering stopwords and short tokens.
     */
    private fun tokenize(text: String): List<String> {
        val stopwords = setOf("the", "a", "an", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "shall", "to", "of", "in", "for", "on", "with",
            "at", "by", "from", "as", "into", "about", "like", "through", "after", "over",
            "between", "out", "up", "down", "it", "its", "this", "that", "and", "or", "but",
            "not", "no", "if", "then", "so", "than", "too", "very", "just", "i", "me", "my",
            "we", "our", "you", "your", "he", "she", "they", "them", "what", "which", "who")
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopwords }
    }

    /**
     * Split a document into overlapping chunks of ~500 characters for retrieval.
     */
    private fun chunkDocument(doc: DocumentInfo): List<String> {
        val content = doc.content
        if (content.length <= 600) return listOf(content)

        val chunks = mutableListOf<String>()
        var start = 0
        val chunkSize = 500
        val overlap = 100
        while (start < content.length) {
            val end = (start + chunkSize).coerceAtMost(content.length)
            chunks.add(content.substring(start, end))
            start += chunkSize - overlap
        }
        return chunks
    }

    override fun buildPromptWithContext(userMessage: String, ragContext: String): String {
        return contextBuilder.buildPromptWithContext(userMessage, ragContext)
    }

    // ==================== Settings Management ====================

    override fun setRAGEnabled(enabled: Boolean) {
        _ragEnabled.value = enabled
        println("[RAGCoordinatorDesktop] RAG enabled: $enabled")
    }

    override fun setSelectedDocuments(documentIds: List<String>) {
        _selectedDocumentIds.value = documentIds
        println("[RAGCoordinatorDesktop] Selected documents: $documentIds")
    }

    override fun setRAGThreshold(threshold: Float) {
        _ragThreshold.value = threshold.coerceIn(0.0f, 1.0f)
        println("[RAGCoordinatorDesktop] RAG threshold: ${_ragThreshold.value}")
    }

    override fun isRAGActive(): Boolean {
        return _ragEnabled.value && _selectedDocumentIds.value.isNotEmpty()
    }

    override fun clearCitations() {
        _recentSourceCitations.value = emptyList()
    }

    // ==================== Document Management (Desktop-specific) ====================

    /**
     * Add a document to the RAG store.
     *
     * @param id Unique document identifier
     * @param title Document title
     * @param content Document content
     * @param metadata Optional metadata
     */
    fun addDocument(
        id: String,
        title: String,
        content: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        documentStore[id] = DocumentInfo(
            id = id,
            title = title,
            content = content,
            metadata = metadata
        )
        println("[RAGCoordinatorDesktop] Added document: $id ($title)")
    }

    /**
     * Remove a document from the RAG store.
     *
     * @param id Document identifier
     */
    fun removeDocument(id: String) {
        documentStore.remove(id)
        _selectedDocumentIds.value = _selectedDocumentIds.value - id
        println("[RAGCoordinatorDesktop] Removed document: $id")
    }

    /**
     * Get all available documents.
     *
     * @return List of document info
     */
    fun getAvailableDocuments(): List<DocumentInfo> {
        return documentStore.values.toList()
    }

    /**
     * Document info data class.
     */
    data class DocumentInfo(
        val id: String,
        val title: String,
        val content: String,
        val metadata: Map<String, String>
    )

    companion object {
        @Volatile
        private var INSTANCE: RAGCoordinatorDesktop? = null

        /**
         * Get singleton instance of RAGCoordinatorDesktop.
         *
         * @return Singleton instance
         */
        fun getInstance(): RAGCoordinatorDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RAGCoordinatorDesktop().also {
                    INSTANCE = it
                }
            }
        }
    }
}
