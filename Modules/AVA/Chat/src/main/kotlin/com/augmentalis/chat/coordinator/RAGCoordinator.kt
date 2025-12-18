package com.augmentalis.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.chat.domain.RAGContextBuilder
import com.augmentalis.chat.domain.SourceCitation
import com.augmentalis.rag.domain.RAGRepository
import com.augmentalis.rag.domain.SearchFilters
import com.augmentalis.rag.domain.SearchQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG Coordinator - Single Responsibility: RAG context retrieval
 *
 * Extracted from ChatViewModel as part of SOLID refactoring (P0).
 * Handles all RAG-related operations:
 * - Document context retrieval
 * - Source citation extraction
 * - RAG settings management
 *
 * Graceful degradation: If RAG repository is null, returns empty context.
 *
 * @param ragRepository Optional RAG repository for document retrieval
 * @param chatPreferences User preferences for RAG settings
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 */
@Singleton
class RAGCoordinator @Inject constructor(
    private val ragRepository: RAGRepository?,
    private val chatPreferences: ChatPreferences
) : IRAGCoordinator {
    companion object {
        private const val TAG = "RAGCoordinator"
    }

    // ==================== State ====================

    private val _recentSourceCitations = MutableStateFlow<List<SourceCitation>>(emptyList())
    override val recentSourceCitations: StateFlow<List<SourceCitation>> = _recentSourceCitations.asStateFlow()

    // ==================== Settings ====================

    override val ragEnabled: StateFlow<Boolean> = chatPreferences.ragEnabled
    override val selectedDocumentIds: StateFlow<List<String>> = chatPreferences.selectedDocumentIds
    override val ragThreshold: StateFlow<Float> = chatPreferences.ragThreshold

    // ==================== Context Builder ====================

    private val ragContextBuilder = RAGContextBuilder()

    // ==================== Retrieval ====================

    /**
     * Retrieve RAG context for a user query.
     *
     * @param query User query text
     * @return RAGResult with context, citations, and timing
     */
    override suspend fun retrieveContext(query: String): IRAGCoordinator.RAGResult {
        val startTime = System.currentTimeMillis()

        // Check if RAG is enabled and configured
        if (!ragEnabled.value) {
            Log.d(TAG, "RAG disabled in settings")
            return IRAGCoordinator.RAGResult(null, emptyList(), 0)
        }

        if (selectedDocumentIds.value.isEmpty()) {
            Log.d(TAG, "No documents selected for RAG")
            return IRAGCoordinator.RAGResult(null, emptyList(), 0)
        }

        if (ragRepository == null) {
            Log.d(TAG, "RAG repository not available")
            return IRAGCoordinator.RAGResult(null, emptyList(), 0)
        }

        Log.d(TAG, "RAG retrieval starting...")
        Log.d(TAG, "  Enabled: ${ragEnabled.value}")
        Log.d(TAG, "  Selected documents: ${selectedDocumentIds.value.size}")
        Log.d(TAG, "  Threshold: ${ragThreshold.value}")

        val searchQuery = SearchQuery(
            query = query.trim(),
            maxResults = 5,
            minSimilarity = ragThreshold.value,
            filters = SearchFilters(documentIds = selectedDocumentIds.value)
        )

        val searchResult = ragRepository.search(searchQuery)

        return if (searchResult.isSuccess) {
            val searchResponse = searchResult.getOrNull()
            if (searchResponse == null) {
                Log.w(TAG, "RAG search succeeded but returned null response, continuing without RAG context")
                _recentSourceCitations.value = emptyList()
                IRAGCoordinator.RAGResult(null, emptyList(), System.currentTimeMillis() - startTime)
            } else {
                Log.d(TAG, "RAG search successful:")
                Log.d(TAG, "  Results: ${searchResponse.results.size}")
                Log.d(TAG, "  Search time: ${searchResponse.searchTimeMs}ms")
                Log.d(TAG, "  Cache hit: ${searchResponse.cacheHit}")

                if (searchResponse.results.isNotEmpty()) {
                    val ragContext = ragContextBuilder.assembleContext(searchResponse.results)
                    val citations = ragContextBuilder.extractSourceCitations(searchResponse.results)
                    _recentSourceCitations.value = citations

                    Log.d(TAG, "RAG context assembled (${ragContext.length} chars)")
                    Log.d(TAG, "Source citations: ${citations.size}")

                    IRAGCoordinator.RAGResult(ragContext, citations, searchResponse.searchTimeMs)
                } else {
                    Log.d(TAG, "No RAG results above threshold (${ragThreshold.value})")
                    _recentSourceCitations.value = emptyList()
                    IRAGCoordinator.RAGResult(null, emptyList(), searchResponse.searchTimeMs)
                }
            }
        } else {
            val exception = searchResult.exceptionOrNull()
            Log.e(TAG, "RAG search failed", exception)
            _recentSourceCitations.value = emptyList()
            IRAGCoordinator.RAGResult(null, emptyList(), System.currentTimeMillis() - startTime)
        }
    }

    /**
     * Build a prompt with RAG context included.
     */
    override fun buildPromptWithContext(userMessage: String, ragContext: String): String {
        return ragContextBuilder.buildPromptWithContext(userMessage, ragContext)
    }

    // ==================== Settings Management ====================

    /**
     * Enable or disable RAG-enhanced responses.
     */
    override fun setRAGEnabled(enabled: Boolean) {
        chatPreferences.setRagEnabled(enabled)
        Log.d(TAG, "RAG ${if (enabled) "enabled" else "disabled"}")

        if (!enabled) {
            _recentSourceCitations.value = emptyList()
        }
    }

    /**
     * Set the list of document IDs to use for RAG retrieval.
     */
    override fun setSelectedDocuments(documentIds: List<String>) {
        chatPreferences.setSelectedDocumentIds(documentIds)
        chatPreferences.setRagEnabled(documentIds.isNotEmpty())
        Log.d(TAG, "Selected ${documentIds.size} documents for RAG")

        if (documentIds.isEmpty()) {
            _recentSourceCitations.value = emptyList()
        }
    }

    /**
     * Set the similarity threshold for RAG retrieval.
     */
    override fun setRAGThreshold(threshold: Float) {
        require(threshold in 0f..1f) { "Threshold must be between 0.0 and 1.0" }
        chatPreferences.setRagThreshold(threshold)
        Log.d(TAG, "RAG similarity threshold set to: $threshold")
    }

    /**
     * Check if RAG is currently active (enabled and documents selected).
     */
    override fun isRAGActive(): Boolean {
        return ragEnabled.value && selectedDocumentIds.value.isNotEmpty() && ragRepository != null
    }

    /**
     * Clear source citations.
     */
    override fun clearCitations() {
        _recentSourceCitations.value = emptyList()
    }
}
