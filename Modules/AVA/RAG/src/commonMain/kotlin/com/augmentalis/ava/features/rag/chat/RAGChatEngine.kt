// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/chat/RAGChatEngine.kt
// created: 2025-11-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.chat

import com.augmentalis.ava.features.rag.domain.RAGRepository
import com.augmentalis.ava.features.rag.domain.SearchQuery
import com.augmentalis.ava.features.rag.domain.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * RAG-enhanced chat engine
 *
 * Combines document retrieval with LLM generation for accurate,
 * grounded responses with source citations.
 *
 * Flow:
 * 1. User asks question
 * 2. Search relevant document chunks (RAG)
 * 3. Assemble context from top results
 * 4. Generate LLM response using context
 * 5. Stream response with source citations
 */
class RAGChatEngine(
    private val ragRepository: RAGRepository,
    private val llmProvider: LLMProvider,
    private val config: ChatConfig = ChatConfig()
) {

    /**
     * Ask a question with RAG context
     *
     * @param question User's question
     * @param conversationHistory Previous messages for context
     * @return Flow of response text chunks + final sources
     */
    suspend fun ask(
        question: String,
        conversationHistory: List<Message> = emptyList()
    ): Flow<ChatResponse> = flow {
        // 1. Search documents for relevant context
        val searchResults = ragRepository.search(
            SearchQuery(
                query = question,
                maxResults = config.maxContextChunks,
                minSimilarity = config.minSimilarity
            )
        ).getOrNull()

        if (searchResults == null || searchResults.results.isEmpty()) {
            // No relevant documents found
            emit(ChatResponse.NoContext(
                message = "I don't have information about that in my documents."
            ))
            return@flow
        }

        // 2. Assemble context from top results
        val context = assembleContext(searchResults.results, config.maxContextLength)

        // 3. Build prompt with context
        val prompt = buildPrompt(
            question = question,
            context = context,
            conversationHistory = conversationHistory,
            config = config
        )

        // 4. Generate LLM response (streaming)
        val textBuilder = StringBuilder()

        llmProvider.generateStream(prompt).collect { chunk ->
            textBuilder.append(chunk)
            emit(ChatResponse.Streaming(chunk))
        }

        // 5. Emit final response with sources
        emit(
            ChatResponse.Complete(
                fullText = textBuilder.toString(),
                sources = searchResults.results.take(3).map { result ->
                    Source(
                        title = result.document?.title ?: "Unknown Document",
                        snippet = result.chunk.content.take(200),
                        page = result.chunk.metadata.pageNumber,
                        similarity = result.similarity
                    )
                }
            )
        )
    }

    /**
     * Assemble context from search results
     *
     * Concatenates relevant chunks with source attribution
     */
    private fun assembleContext(
        results: List<SearchResult>,
        maxLength: Int
    ): String {
        val contextBuilder = StringBuilder()
        var currentLength = 0

        results.forEach { result ->
            val docTitle = result.document?.title ?: "Document"
            val pageNum = result.chunk.metadata.pageNumber ?: "?"
            val similarity = (result.similarity * 100).toInt()

            val entry = """
                [Source: $docTitle, Page $pageNum, Relevance: $similarity%]
                ${result.chunk.content}

            """.trimIndent()

            if (currentLength + entry.length <= maxLength) {
                contextBuilder.append(entry).append("\n\n")
                currentLength += entry.length
            }
        }

        return contextBuilder.toString().trim()
    }

    /**
     * Build LLM prompt with RAG context
     */
    private fun buildPrompt(
        question: String,
        context: String,
        conversationHistory: List<Message>,
        config: ChatConfig
    ): String {
        val systemPrompt = config.systemPrompt

        val conversationContext = if (conversationHistory.isNotEmpty()) {
            val recent = conversationHistory.takeLast(config.maxHistoryMessages)
            "Previous conversation:\n" + recent.joinToString("\n") {
                "${it.role}: ${it.content}"
            } + "\n\n"
        } else {
            ""
        }

        return """
$systemPrompt

${conversationContext}Context from documents:
$context

User question: $question

Instructions:
- Answer based ONLY on the provided context
- Cite specific sources (document name, page number)
- If the context doesn't contain the answer, say "I don't have that information in the documents"
- Be conversational but accurate
- Keep responses concise unless detail is requested

Answer:
        """.trimIndent()
    }
}

/**
 * Chat engine configuration
 */
data class ChatConfig(
    val maxContextChunks: Int = 5,           // Top 5 most relevant chunks
    val minSimilarity: Float = 0.7f,         // 70% minimum relevance
    val maxContextLength: Int = 2000,        // ~500 tokens of context
    val maxHistoryMessages: Int = 10,        // Last 10 messages for context
    val systemPrompt: String = """
You are AVA, an intelligent assistant with access to technical documentation and manuals.
Your role is to help users find information from their documents accurately and efficiently.
    """.trimIndent()
)

/**
 * Conversation message
 */
data class Message(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Chat response types
 */
sealed class ChatResponse {
    /**
     * Streaming text chunk (partial response)
     */
    data class Streaming(val text: String) : ChatResponse()

    /**
     * Complete response with sources
     */
    data class Complete(
        val fullText: String,
        val sources: List<Source>
    ) : ChatResponse()

    /**
     * No relevant context found
     */
    data class NoContext(val message: String) : ChatResponse()

    /**
     * Error occurred
     */
    data class Error(val message: String, val cause: Throwable?) : ChatResponse()
}

/**
 * Source citation
 */
data class Source(
    val title: String,
    val snippet: String,
    val page: Int?,
    val similarity: Float
)

/**
 * LLM provider interface
 *
 * Implement this with your LLM backend (MLC-LLM, etc.)
 */
interface LLMProvider {
    /**
     * Generate response stream from prompt
     *
     * @param prompt Full prompt with context
     * @return Flow of text chunks
     */
    fun generateStream(prompt: String): Flow<String>

    /**
     * Generate complete response (non-streaming)
     *
     * @param prompt Full prompt with context
     * @return Complete generated text
     */
    suspend fun generate(prompt: String): String
}
