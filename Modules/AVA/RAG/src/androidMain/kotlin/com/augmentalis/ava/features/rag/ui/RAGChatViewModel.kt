// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/ui/RAGChatViewModel.kt
// created: 2025-11-06
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.features.rag.chat.*
import com.augmentalis.ava.features.rag.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for RAG Chat Interface
 *
 * Integrates RAGChatEngine for context-aware LLM responses with document citations.
 *
 * Features:
 * - Streaming chat responses
 * - Source citations
 * - Conversation history
 * - Error handling
 * - No-context detection
 *
 * Usage:
 * ```kotlin
 * val chatViewModel = RAGChatViewModel(
 *     ragRepository = repository,
 *     llmProvider = MLCLLMProvider(context)
 * )
 *
 * RAGChatScreen(viewModel = chatViewModel)
 * ```
 */
class RAGChatViewModel(
    private val ragRepository: RAGRepository,
    private val llmProvider: LLMProvider,
    chatConfig: ChatConfig = ChatConfig()
) : ViewModel() {

    private val chatEngine = RAGChatEngine(
        ragRepository = ragRepository,
        llmProvider = llmProvider,
        config = chatConfig
    )

    // UI State
    private val _messages = MutableStateFlow<List<ChatMessageUI>>(emptyList())
    val messages: StateFlow<List<ChatMessageUI>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Search results (for search-only mode)
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    /**
     * Send question to RAG chat
     *
     * Retrieves relevant documents and generates context-aware response.
     *
     * @param question User question
     */
    fun askQuestion(question: String) {
        if (question.isBlank()) return

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null

            // Add user message
            val userMessage = ChatMessageUI(
                id = generateMessageId(),
                role = MessageRole.USER,
                content = question,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + userMessage

            // Create assistant message placeholder
            val assistantMessageId = generateMessageId()
            val assistantMessage = ChatMessageUI(
                id = assistantMessageId,
                role = MessageRole.ASSISTANT,
                content = "",
                timestamp = System.currentTimeMillis(),
                isStreaming = true
            )
            _messages.value = _messages.value + assistantMessage

            try {
                var fullResponse = ""
                val sources = mutableListOf<Source>()

                chatEngine.ask(
                    question = question,
                    conversationHistory = convertToMessageHistory()
                ).collect { response ->
                    when (response) {
                        is ChatResponse.Streaming -> {
                            // Append text chunk
                            fullResponse += response.text
                            updateAssistantMessage(assistantMessageId, fullResponse, sources, isStreaming = true)
                        }

                        is ChatResponse.Complete -> {
                            // Final update with sources
                            sources.addAll(response.sources)
                            updateAssistantMessage(assistantMessageId, fullResponse, sources, isStreaming = false)
                            _isGenerating.value = false
                        }

                        is ChatResponse.NoContext -> {
                            // No relevant documents
                            updateAssistantMessage(
                                assistantMessageId,
                                response.message,
                                emptyList(),
                                isStreaming = false
                            )
                            _isGenerating.value = false
                        }

                        is ChatResponse.Error -> {
                            // Error occurred
                            _error.value = response.message
                            removeMessage(assistantMessageId)
                            _isGenerating.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Chat error")
                _error.value = "Error: ${e.message}"
                removeMessage(assistantMessageId)
                _isGenerating.value = false
            }
        }
    }

    /**
     * Search documents without LLM generation
     *
     * Useful for exploring what documents contain without generating a response.
     *
     * @param query Search query
     */
    fun searchDocuments(query: String, maxResults: Int = 10) {
        if (query.isBlank()) return

        viewModelScope.launch {
            try {
                val result = ragRepository.search(
                    SearchQuery(
                        query = query,
                        maxResults = maxResults,
                        minSimilarity = 0.7f
                    )
                )

                result.fold(
                    onSuccess = { searchResponse ->
                        _searchResults.value = searchResponse.results
                    },
                    onFailure = { error ->
                        Timber.e(error, "Search failed")
                        _error.value = "Search failed: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Search error")
                _error.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Clear conversation history
     */
    fun clearChat() {
        _messages.value = emptyList()
        _error.value = null
    }

    /**
     * Stop current generation
     */
    fun stopGeneration() {
        viewModelScope.launch {
            // Stop LLM provider if it's generating
            if (_isGenerating.value) {
                // Note: LLMProvider interface doesn't expose stop() yet
                // This would require extending the interface
                _isGenerating.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Update assistant message during streaming
     */
    private fun updateAssistantMessage(
        messageId: String,
        content: String,
        sources: List<Source>,
        isStreaming: Boolean
    ) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                message.copy(
                    content = content,
                    sources = sources,
                    isStreaming = isStreaming
                )
            } else {
                message
            }
        }
    }

    /**
     * Remove message by ID
     */
    private fun removeMessage(messageId: String) {
        _messages.value = _messages.value.filter { it.id != messageId }
    }

    /**
     * Convert UI messages to chat engine format
     */
    private fun convertToMessageHistory(): List<com.augmentalis.ava.features.rag.chat.Message> {
        return _messages.value
            .filter { !it.isStreaming } // Exclude streaming messages
            .map { uiMessage ->
                com.augmentalis.ava.features.rag.chat.Message(
                    role = when (uiMessage.role) {
                        MessageRole.USER -> com.augmentalis.ava.features.rag.chat.MessageRole.USER
                        MessageRole.ASSISTANT -> com.augmentalis.ava.features.rag.chat.MessageRole.ASSISTANT
                        MessageRole.SYSTEM -> com.augmentalis.ava.features.rag.chat.MessageRole.SYSTEM
                    },
                    content = uiMessage.content
                )
            }
    }

    /**
     * Generate unique message ID
     */
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

/**
 * UI model for chat messages
 */
data class ChatMessageUI(
    val id: String,
    val role: MessageRole,
    val content: String,
    val sources: List<Source> = emptyList(),
    val timestamp: Long,
    val isStreaming: Boolean = false
)

/**
 * Message role
 */
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

/**
 * Message for chat engine history
 */
data class Message(
    val role: MessageRole,
    val content: String
)
