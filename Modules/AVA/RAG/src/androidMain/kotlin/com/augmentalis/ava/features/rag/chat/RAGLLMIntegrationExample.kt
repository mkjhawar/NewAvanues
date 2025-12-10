// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/chat/RAGLLMIntegrationExample.kt
// created: 2025-11-15
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.chat

import android.content.Context
import com.augmentalis.ava.features.llm.provider.LocalLLMProvider as LLMLocalProvider
import com.augmentalis.ava.features.llm.domain.LLMConfig
import com.augmentalis.ava.features.rag.data.SQLiteRAGRepository
import com.augmentalis.ava.features.rag.embeddings.ONNXEmbeddingProvider
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.io.File

/**
 * Example integration of RAG + LLM for context-aware chat
 *
 * This demonstrates how to wire up:
 * - RAGRepository (document retrieval with clustering)
 * - LocalLLMProvider (MLC-LLM on-device inference)
 * - RAGChatEngine (orchestration)
 *
 * Usage:
 * ```kotlin
 * val integration = RAGLLMIntegration(context)
 * integration.initialize().onSuccess {
 *     integration.ask("How do I reset the device?") { response ->
 *         when (response) {
 *             is ChatResponse.Streaming -> updateUI(response.text)
 *             is ChatResponse.Complete -> showSources(response.sources)
 *         }
 *     }
 * }
 * ```
 *
 * Created: 2025-11-15
 * Part of: RAG Phase 4 - LLM Integration
 */
class RAGLLMIntegration(
    private val context: Context
) {
    private lateinit var ragRepository: SQLiteRAGRepository
    private lateinit var llmProvider: LLMLocalProvider
    private lateinit var llmAdapter: LocalLLMProviderAdapter
    private lateinit var chatEngine: RAGChatEngine

    private var isInitialized = false

    /**
     * Initialize all components
     *
     * Steps:
     * 1. Create ONNX embedding provider
     * 2. Create SQLite RAG repository with clustering
     * 3. Initialize LocalLLMProvider with Gemma-2B-IT model
     * 4. Create adapter to bridge interfaces
     * 5. Create RAGChatEngine orchestrator
     *
     * @return Result.Success if all components initialized, Result.Error otherwise
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            Timber.i("=== RAG + LLM Integration Initialization ===")

            // 1. Create ONNX embedding provider
            Timber.d("Step 1/5: Creating ONNX embedding provider...")
            val embeddingProvider = ONNXEmbeddingProvider(context)
            // Embedding provider initializes on first use (lazy loading)

            // 2. Create SQLite RAG repository
            Timber.d("Step 2/5: Creating RAG repository with clustering...")
            ragRepository = SQLiteRAGRepository(
                context = context,
                embeddingProvider = embeddingProvider,
                enableClustering = true,           // Enable k-means clustering for 40x speedup
                clusterCount = 256,                 // 256 clusters for 200k chunks
                topClusters = 3                     // Search top-3 nearest clusters
            )

            // 3. Initialize LocalLLMProvider
            Timber.d("Step 3/5: Initializing LocalLLMProvider...")
            llmProvider = LLMLocalProvider(
                context = context,
                autoModelSelection = true           // Enable auto language-based model switching
            )

            // Find Gemma model in external files directory
            val modelsDir = File(context.getExternalFilesDir(null), "models")
            val gemmaModelPath = File(modelsDir, "gemma-2b-it-q4f16_1")

            if (!gemmaModelPath.exists()) {
                val error = "Gemma model not found: ${gemmaModelPath.absolutePath}"
                Timber.e(error)
                return Result.Error(
                    exception = java.io.FileNotFoundException(error),
                    message = "LLM model not found. Please run model setup first."
                )
            }

            val config = LLMConfig(
                modelPath = gemmaModelPath.absolutePath,
                modelLib = "gemma-2b-it-q4f16_1",
                device = "opencl",                  // Use GPU acceleration
                maxMemoryMB = 2048                  // 2GB memory budget
            )

            val initResult = llmProvider.initialize(config)
            if (initResult is Result.Error) {
                Timber.e("Failed to initialize LocalLLMProvider: ${initResult.message}")
                return initResult
            }

            // 4. Create adapter
            Timber.d("Step 4/5: Creating LLM adapter...")
            llmAdapter = LocalLLMProviderAdapter(llmProvider)

            // 5. Create RAGChatEngine
            Timber.d("Step 5/5: Creating RAG chat engine...")
            chatEngine = RAGChatEngine(
                ragRepository = ragRepository,
                llmProvider = llmAdapter,
                config = ChatConfig(
                    maxContextChunks = 5,           // Top 5 most relevant chunks
                    minSimilarity = 0.7f,            // 70% minimum relevance
                    maxContextLength = 2000,         // ~500 tokens of context
                    maxHistoryMessages = 10,         // Last 10 messages for context
                    systemPrompt = """
                        You are AVA, an intelligent assistant with access to technical documentation and manuals.
                        Your role is to help users find information from their documents accurately and efficiently.

                        Guidelines:
                        - Answer based ONLY on the provided context from documents
                        - Always cite specific sources (document name, page number)
                        - If the context doesn't contain the answer, clearly state "I don't have that information in the documents"
                        - Be conversational but precise
                        - Keep responses concise unless detail is requested
                        - When providing instructions, use numbered steps
                    """.trimIndent()
                )
            )

            isInitialized = true
            Timber.i("✅ RAG + LLM Integration initialized successfully")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize RAG + LLM integration")
            Result.Error(
                exception = e,
                message = "Initialization failed: ${e.message}"
            )
        }
    }

    /**
     * Ask a question with RAG context and LLM generation
     *
     * @param question User's question
     * @param onResponse Callback invoked for each response chunk
     */
    suspend fun ask(
        question: String,
        onResponse: (ChatResponse) -> Unit
    ) {
        if (!isInitialized) {
            Timber.e("RAGLLMIntegration not initialized - call initialize() first")
            onResponse(ChatResponse.Error(
                message = "Integration not initialized",
                cause = IllegalStateException("Call initialize() first")
            ))
            return
        }

        Timber.i("User question: $question")

        try {
            chatEngine.ask(
                question = question,
                conversationHistory = emptyList()  // TODO: Add conversation history tracking
            ).collect { response ->
                // Forward response to caller
                onResponse(response)

                // Log responses for debugging
                when (response) {
                    is ChatResponse.Streaming -> {
                        // Streaming chunk - don't log (too verbose)
                    }
                    is ChatResponse.Complete -> {
                        Timber.i("Response complete: ${response.fullText.length} chars, ${response.sources.size} sources")
                        response.sources.forEach { source ->
                            Timber.d("Source: ${source.title} (p.${source.page}, ${(source.similarity * 100).toInt()}%)")
                        }
                    }
                    is ChatResponse.NoContext -> {
                        Timber.w("No relevant context found for question: $question")
                    }
                    is ChatResponse.Error -> {
                        Timber.e(response.cause, "Error during chat: ${response.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during ask()")
            onResponse(ChatResponse.Error(
                message = "Unexpected error: ${e.message}",
                cause = e
            ))
        }
    }

    /**
     * Clean up resources
     *
     * Call this when the integration is no longer needed (e.g., app shutdown)
     */
    suspend fun cleanup() {
        if (isInitialized) {
            Timber.i("Cleaning up RAG + LLM integration...")
            llmProvider.cleanup()
            // RAG repository cleanup happens automatically via Room
            isInitialized = false
        }
    }
}

/**
 * Example usage in a Jetpack Compose ViewModel
 */
/*
@HiltViewModel
class RAGChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val ragLLMIntegration = RAGLLMIntegration(context)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        // Initialize on ViewModel creation
        viewModelScope.launch {
            ragLLMIntegration.initialize().onFailure { error ->
                Timber.e("Failed to initialize RAG+LLM: ${error.message}")
            }
        }
    }

    fun askQuestion(question: String) {
        viewModelScope.launch {
            _isGenerating.value = true

            // Add user message
            _messages.value += ChatMessage(
                role = MessageRole.USER,
                content = question,
                timestamp = System.currentTimeMillis()
            )

            var assistantResponse = ""
            val sources = mutableListOf<Source>()

            ragLLMIntegration.ask(question) { response ->
                when (response) {
                    is ChatResponse.Streaming -> {
                        assistantResponse += response.text
                        updateAssistantMessage(assistantResponse)
                    }

                    is ChatResponse.Complete -> {
                        sources.addAll(response.sources)
                        updateAssistantMessage(assistantResponse, sources)
                        _isGenerating.value = false
                    }

                    is ChatResponse.NoContext -> {
                        updateAssistantMessage(response.message)
                        _isGenerating.value = false
                    }

                    is ChatResponse.Error -> {
                        updateAssistantMessage("Error: ${response.message}")
                        _isGenerating.value = false
                    }
                }
            }
        }
    }

    private fun updateAssistantMessage(content: String, sources: List<Source> = emptyList()) {
        val updatedMessages = _messages.value.toMutableList()
        val lastIndex = updatedMessages.indexOfLast { it.role == MessageRole.ASSISTANT }

        if (lastIndex >= 0) {
            updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                content = content,
                sources = sources
            )
        } else {
            updatedMessages.add(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = content,
                    sources = sources,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        _messages.value = updatedMessages
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            ragLLMIntegration.cleanup()
        }
    }
}

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val sources: List<Source> = emptyList(),
    val timestamp: Long
)
*/
