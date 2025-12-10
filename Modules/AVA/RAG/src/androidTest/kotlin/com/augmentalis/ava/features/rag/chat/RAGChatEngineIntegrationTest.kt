// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/chat/RAGChatEngineIntegrationTest.kt
// created: 2025-11-15
// author: AVA AI Team

package com.augmentalis.ava.features.rag.chat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.features.rag.data.InMemoryRAGRepository
import com.augmentalis.ava.features.rag.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlinx.datetime.Clock

/**
 * Integration tests for RAGChatEngine
 *
 * Tests the orchestration of:
 * - Document retrieval (RAG)
 * - Context assembly
 * - LLM prompt construction
 * - Streaming response generation
 * - Source citations
 *
 * Created: 2025-11-15
 * Part of: RAG Phase 4 - LLM Integration Tests
 */
@RunWith(AndroidJUnit4::class)
class RAGChatEngineIntegrationTest {

    private lateinit var context: Context
    private lateinit var ragRepository: InMemoryRAGRepository
    private lateinit var mockLLMProvider: MockLLMProvider
    private lateinit var mockEmbeddingProvider: MockEmbeddingProvider
    private lateinit var chatEngine: RAGChatEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create mock embedding provider
        mockEmbeddingProvider = MockEmbeddingProvider()

        // Create in-memory RAG repository with test data
        ragRepository = InMemoryRAGRepository(embeddingProvider = mockEmbeddingProvider)

        // Create mock LLM provider
        mockLLMProvider = MockLLMProvider()

        // Create chat engine
        chatEngine = RAGChatEngine(
            ragRepository = ragRepository,
            llmProvider = mockLLMProvider,
            config = ChatConfig(
                maxContextChunks = 3,
                minSimilarity = 0.6f,
                maxContextLength = 1000
            )
        )
    }

    @Test
    fun testChatEngineCreation() {
        assertNotNull("Chat engine should be created", chatEngine)
    }

    @Test
    fun testAskWithNoDocuments() = runBlocking {
        // Ask question with empty repository
        val responses = chatEngine.ask("How do I reset the device?").toList()

        // Should return NoContext response
        assertTrue("Should have at least one response", responses.isNotEmpty())
        val firstResponse = responses.first()
        assertTrue("First response should be NoContext", firstResponse is ChatResponse.NoContext)

        if (firstResponse is ChatResponse.NoContext) {
            assertTrue(
                "Message should mention no information",
                firstResponse.message.contains("don't have information", ignoreCase = true)
            )
        }
    }

    @Test
    fun testAskWithRelevantDocuments() = runBlocking {
        // Add test document to repository
        addTestDocument(
            title = "Device Manual",
            content = "To reset the device, press and hold the power button for 10 seconds. The device will reboot automatically."
        )

        // Ask question
        val responses = chatEngine.ask("How do I reset the device?").toList()

        // Should have streaming responses
        assertTrue("Should have responses", responses.isNotEmpty())

        // Check for streaming chunks
        val streamingResponses = responses.filterIsInstance<ChatResponse.Streaming>()
        assertTrue("Should have streaming responses", streamingResponses.isNotEmpty())

        // Check for complete response with sources
        val completeResponses = responses.filterIsInstance<ChatResponse.Complete>()
        assertEquals("Should have exactly one complete response", 1, completeResponses.size)

        val complete = completeResponses.first()
        assertTrue("Should have full text", complete.fullText.isNotEmpty())
        assertTrue("Should have sources", complete.sources.isNotEmpty())
        assertEquals("Should have 1 source", 1, complete.sources.size)

        val source = complete.sources.first()
        assertEquals("Source title should match", "Device Manual", source.title)
        assertTrue("Source similarity should be high", source.similarity > 0.6f)
    }

    @Test
    fun testAskWithMultipleRelevantChunks() = runBlocking {
        // Add multiple related documents
        addTestDocument(
            title = "Quick Start Guide",
            content = "Press the power button to turn on. Hold for 3 seconds to enter power menu."
        )

        addTestDocument(
            title = "Troubleshooting",
            content = "If device freezes, force reset by holding power button for 10 seconds."
        )

        addTestDocument(
            title = "Advanced Settings",
            content = "Factory reset erases all data. Navigate to Settings > System > Reset."
        )

        // Ask question
        val responses = chatEngine.ask("How do I reset the device?").toList()

        // Check complete response
        val completeResponses = responses.filterIsInstance<ChatResponse.Complete>()
        assertEquals("Should have one complete response", 1, completeResponses.size)

        val complete = completeResponses.first()
        assertTrue("Should have multiple sources", complete.sources.size > 1)
        assertTrue("Should have at most 3 sources (config limit)", complete.sources.size <= 3)

        // Sources should be sorted by similarity
        val similarities = complete.sources.map { it.similarity }
        assertEquals("Sources should be sorted desc", similarities, similarities.sortedDescending())
    }

    @Test
    fun testContextAssembly() = runBlocking {
        // Add document with known content
        val testContent = "The answer is 42. This is a test document for context assembly."
        addTestDocument(
            title = "Test Doc",
            content = testContent
        )

        // Ask question and capture prompt sent to LLM
        chatEngine.ask("What is the answer?").toList()

        // Check that mock LLM received prompt with context
        val lastPrompt = mockLLMProvider.lastPrompt
        assertNotNull("LLM should have received prompt", lastPrompt)
        assertTrue("Prompt should contain context", lastPrompt!!.contains("Context from documents"))
        assertTrue("Prompt should contain source attribution", lastPrompt.contains("[Source:"))
        assertTrue("Prompt should contain test content", lastPrompt.contains("42"))
    }

    @Test
    fun testConversationHistory() = runBlocking {
        // Add test document
        addTestDocument(
            title = "Manual",
            content = "The device has a battery life of 24 hours."
        )

        // Create conversation history
        val history = listOf(
            Message(
                role = MessageRole.USER,
                content = "What is the battery life?"
            ),
            Message(
                role = MessageRole.ASSISTANT,
                content = "The device has a battery life of 24 hours."
            )
        )

        // Ask follow-up question with history
        chatEngine.ask(
            question = "How long does it last?",
            conversationHistory = history
        ).toList()

        // Check that prompt included conversation history
        val lastPrompt = mockLLMProvider.lastPrompt
        assertNotNull("LLM should have received prompt", lastPrompt)
        assertTrue("Prompt should include previous conversation", lastPrompt!!.contains("Previous conversation"))
        assertTrue("Prompt should include user's previous question", lastPrompt.contains("battery life"))
    }

    @Test
    fun testMinSimilarityThreshold() = runBlocking {
        // Add irrelevant document
        addTestDocument(
            title = "Cooking Recipe",
            content = "Mix flour and water to make dough. Bake at 350 degrees for 20 minutes."
        )

        // Ask unrelated question
        val responses = chatEngine.ask("How do I reset the device?").toList()

        // Should return NoContext (similarity too low)
        val noContextResponses = responses.filterIsInstance<ChatResponse.NoContext>()
        assertTrue("Should have NoContext response", noContextResponses.isNotEmpty())
    }

    @Test
    fun testMaxContextLength() = runBlocking {
        // Add document with long content
        val longContent = "A".repeat(5000)  // 5000 chars
        addTestDocument(
            title = "Long Doc",
            content = longContent
        )

        // Ask question
        chatEngine.ask("Tell me about this").toList()

        // Check that prompt respects maxContextLength (1000 chars in config)
        val lastPrompt = mockLLMProvider.lastPrompt
        assertNotNull("LLM should have received prompt", lastPrompt)

        // Extract context section from prompt
        val contextStart = lastPrompt!!.indexOf("Context from documents:")
        val contextEnd = lastPrompt.indexOf("User question:")
        if (contextStart >= 0 && contextEnd >= 0) {
            val contextSection = lastPrompt.substring(contextStart, contextEnd)
            assertTrue(
                "Context should be truncated to maxContextLength",
                contextSection.length <= 1200  // Allow some overhead for formatting
            )
        }
    }

    // Helper: Add test document to repository
    private suspend fun addTestDocument(title: String, content: String) {
        // Create a temp file for the test
        val testFile = java.io.File(context.cacheDir, "$title.txt")
        testFile.writeText(content)

        // Add document using repository's addDocument method
        val request = AddDocumentRequest(
            filePath = testFile.absolutePath,
            title = title,
            processImmediately = true  // Process immediately for testing
        )

        ragRepository.addDocument(request)

        // Clean up
        testFile.delete()
    }

    /**
     * Mock LLM provider for testing
     */
    private class MockLLMProvider : LLMProvider {
        var lastPrompt: String? = null

        override fun generateStream(prompt: String): Flow<String> {
            lastPrompt = prompt
            return flow {
                // Emit mock streaming response
                val response = "Based on the provided context, "
                response.forEach { char ->
                    emit(char.toString())
                }
            }
        }

        override suspend fun generate(prompt: String): String {
            lastPrompt = prompt
            return "Based on the provided context, this is a test response."
        }
    }

    /**
     * Mock embedding provider for testing
     */
    private class MockEmbeddingProvider : com.augmentalis.ava.features.rag.embeddings.EmbeddingProvider {
        override val name: String = "MockEmbedding"
        override val dimension: Int = 384

        override suspend fun isAvailable(): Boolean = true

        override suspend fun embed(text: String): Result<com.augmentalis.ava.features.rag.domain.Embedding.Float32> {
            val hash = text.hashCode()
            val values = FloatArray(dimension) { i ->
                ((hash + i) % 1000) / 1000f
            }
            return Result.success(com.augmentalis.ava.features.rag.domain.Embedding.Float32(values))
        }

        override suspend fun embedBatch(texts: List<String>): Result<List<com.augmentalis.ava.features.rag.domain.Embedding.Float32>> {
            return Result.success(texts.map { embed(it).getOrThrow() })
        }

        override fun estimateTimeMs(count: Int): Long = count * 10L
    }
}
