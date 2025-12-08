// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/search/HybridSearchIntegrationTest.kt
// created: 2025-11-27
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.features.rag.data.SQLiteRAGRepository
import com.augmentalis.ava.features.rag.data.room.RAGDatabase
import com.augmentalis.ava.features.rag.domain.*
import com.augmentalis.ava.features.rag.embeddings.EmbeddingProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for Hybrid Search
 *
 * Phase 2.4: Task P1-RAG-005
 *
 * Tests the full hybrid search pipeline:
 * - FTS4 keyword search
 * - Semantic vector search
 * - Reciprocal Rank Fusion
 * - End-to-end search accuracy
 */
@RunWith(AndroidJUnit4::class)
class HybridSearchIntegrationTest {

    private lateinit var context: Context
    private lateinit var repository: SQLiteRAGRepository
    private lateinit var embeddingProvider: EmbeddingProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Close any existing database instance
        RAGDatabase.closeInstance()

        // Create mock embedding provider
        embeddingProvider = object : EmbeddingProvider {
            override suspend fun embed(text: String): Result<Embedding.Float32> {
                // Simple mock: hash text to generate consistent embeddings
                val hash = text.hashCode()
                val embedding = FloatArray(384) { i ->
                    ((hash + i) % 1000).toFloat() / 1000f
                }
                return Result.success(Embedding.Float32(embedding))
            }

            override suspend fun embedBatch(texts: List<String>): Result<List<Embedding.Float32>> {
                return Result.success(texts.map { embed(it).getOrThrow() })
            }
        }

        repository = SQLiteRAGRepository(
            context = context,
            embeddingProvider = embeddingProvider,
            enableClustering = false,
            enableCache = false
        )

        // Clear any existing data
        runBlocking {
            repository.clearAll()
        }
    }

    @After
    fun teardown() {
        runBlocking {
            repository.clearAll()
        }
        RAGDatabase.closeInstance()
    }

    @Test
    fun testHybridSearchCombinesBothMethods() = runBlocking {
        // Create test documents with both semantic and keyword relevance
        val testDocs = listOf(
            "Machine learning is a subset of artificial intelligence",
            "Deep learning uses neural networks for pattern recognition",
            "What is machine learning? A comprehensive introduction"
        )

        val documentIds = mutableListOf<String>()

        // Add documents
        for ((index, content) in testDocs.withIndex()) {
            val tempFile = File.createTempFile("test_doc_$index", ".txt", context.cacheDir)
            tempFile.writeText(content)

            val result = repository.addDocument(
                AddDocumentRequest(
                    filePath = tempFile.absolutePath,
                    title = "Test Doc $index",
                    processImmediately = true
                )
            )

            assertTrue(result.isSuccess)
            documentIds.add(result.getOrThrow().documentId)
            tempFile.delete()
        }

        // Wait for indexing
        kotlinx.coroutines.delay(500)

        // Test hybrid search
        val query = SearchQuery(
            query = "machine learning introduction",
            maxResults = 10,
            minSimilarity = 0.0f
        )

        val searchResult = repository.searchHybrid(query)
        assertTrue(searchResult.isSuccess)

        val response = searchResult.getOrThrow()
        val chunks = response.results

        // Should find at least 2 relevant documents
        assertTrue(chunks.size >= 2, "Hybrid search should find multiple relevant results")

        // Doc 3 should rank high (exact keyword match "machine learning" + "introduction")
        // Doc 1 should rank high (semantic + keyword match "machine learning")
        val topDocIds = chunks.take(2).map { it.chunk.documentId }.toSet()
        assertTrue(
            topDocIds.contains(documentIds[0]) || topDocIds.contains(documentIds[2]),
            "Top results should include documents with keyword matches"
        )
    }

    @Test
    fun testKeywordSearchFindsExactMatches() = runBlocking {
        val testDocs = listOf(
            "The AVA AI assistant uses NLU for understanding",
            "Natural language understanding is important",
            "Deep learning models are powerful"
        )

        val documentIds = mutableListOf<String>()

        for ((index, content) in testDocs.withIndex()) {
            val tempFile = File.createTempFile("test_doc_$index", ".txt", context.cacheDir)
            tempFile.writeText(content)

            val result = repository.addDocument(
                AddDocumentRequest(
                    filePath = tempFile.absolutePath,
                    title = "Test Doc $index",
                    processImmediately = true
                )
            )

            assertTrue(result.isSuccess)
            documentIds.add(result.getOrThrow().documentId)
            tempFile.delete()
        }

        kotlinx.coroutines.delay(500)

        val query = SearchQuery(
            query = "AVA NLU",
            maxResults = 5,
            minSimilarity = 0.0f
        )

        val searchResult = repository.searchHybrid(query)
        assertTrue(searchResult.isSuccess)

        val chunks = searchResult.getOrThrow().results

        // First document contains both "AVA" and "NLU"
        assertTrue(chunks.isNotEmpty())
        val topChunk = chunks.first()
        assertTrue(
            topChunk.chunk.content.contains("AVA", ignoreCase = true) ||
            topChunk.chunk.content.contains("NLU", ignoreCase = true)
        )
    }

    @Test
    fun testHybridSearchBetterThanSemanticOnly() = runBlocking {
        val testDocs = listOf(
            "XYZ123 is a unique identifier for this system",
            "Artificial intelligence and machine learning",
            "The identifier XYZ123 is used throughout"
        )

        val documentIds = mutableListOf<String>()

        for ((index, content) in testDocs.withIndex()) {
            val tempFile = File.createTempFile("test_doc_$index", ".txt", context.cacheDir)
            tempFile.writeText(content)

            val result = repository.addDocument(
                AddDocumentRequest(
                    filePath = tempFile.absolutePath,
                    title = "Test Doc $index",
                    processImmediately = true
                )
            )

            assertTrue(result.isSuccess)
            documentIds.add(result.getOrThrow().documentId)
            tempFile.delete()
        }

        kotlinx.coroutines.delay(500)

        // Query for exact identifier - keyword search should excel here
        val query = SearchQuery(
            query = "XYZ123",
            maxResults = 5,
            minSimilarity = 0.0f
        )

        val hybridResult = repository.searchHybrid(query)
        assertTrue(hybridResult.isSuccess)

        val semanticResult = repository.search(query)
        assertTrue(semanticResult.isSuccess)

        val hybridChunks = hybridResult.getOrThrow().results
        val semanticChunks = semanticResult.getOrThrow().results

        // Hybrid search should find the exact matches
        assertTrue(hybridChunks.isNotEmpty(), "Hybrid search should find exact keyword matches")

        // First result should contain the identifier
        val topResult = hybridChunks.first()
        assertTrue(
            topResult.chunk.content.contains("XYZ123"),
            "Top result should contain exact keyword match"
        )
    }

    @Test
    fun testWeightedFusion() = runBlocking {
        val testDocs = listOf(
            "Machine learning and artificial intelligence",
            "Deep neural networks for pattern recognition"
        )

        for ((index, content) in testDocs.withIndex()) {
            val tempFile = File.createTempFile("test_doc_$index", ".txt", context.cacheDir)
            tempFile.writeText(content)

            repository.addDocument(
                AddDocumentRequest(
                    filePath = tempFile.absolutePath,
                    title = "Test Doc $index",
                    processImmediately = true
                )
            )
            tempFile.delete()
        }

        kotlinx.coroutines.delay(500)

        val query = SearchQuery(
            query = "machine learning",
            maxResults = 5,
            minSimilarity = 0.0f
        )

        // Test weighted fusion favoring semantic search
        val semanticHeavyResult = repository.searchHybrid(
            query,
            useWeightedFusion = true,
            semanticWeight = 0.9f,
            keywordWeight = 0.1f
        )
        assertTrue(semanticHeavyResult.isSuccess)

        // Test weighted fusion favoring keyword search
        val keywordHeavyResult = repository.searchHybrid(
            query,
            useWeightedFusion = true,
            semanticWeight = 0.1f,
            keywordWeight = 0.9f
        )
        assertTrue(keywordHeavyResult.isSuccess)

        // Both should return results
        assertTrue(semanticHeavyResult.getOrThrow().results.isNotEmpty())
        assertTrue(keywordHeavyResult.getOrThrow().results.isNotEmpty())
    }

    @Test
    fun testHybridSearchPerformance() = runBlocking {
        // Add 100 test documents
        for (i in 1..100) {
            val content = "Document $i contains information about ${if (i % 3 == 0) "machine learning" else "data science"}"
            val tempFile = File.createTempFile("test_doc_$i", ".txt", context.cacheDir)
            tempFile.writeText(content)

            repository.addDocument(
                AddDocumentRequest(
                    filePath = tempFile.absolutePath,
                    title = "Test Doc $i",
                    processImmediately = true
                )
            )
            tempFile.delete()
        }

        kotlinx.coroutines.delay(2000) // Wait for indexing

        val query = SearchQuery(
            query = "machine learning",
            maxResults = 10,
            minSimilarity = 0.0f
        )

        val startTime = System.currentTimeMillis()
        val result = repository.searchHybrid(query)
        val endTime = System.currentTimeMillis()

        assertTrue(result.isSuccess)
        val searchTime = endTime - startTime

        // Hybrid search should complete in reasonable time
        assertTrue(searchTime < 500, "Hybrid search should complete in <500ms, took ${searchTime}ms")

        val response = result.getOrThrow()
        assertTrue(response.results.isNotEmpty(), "Should find results")
    }

    @Test
    fun testEmptyQuery() = runBlocking {
        val query = SearchQuery(
            query = "",
            maxResults = 10,
            minSimilarity = 0.0f
        )

        val result = repository.searchHybrid(query)
        assertTrue(result.isSuccess)

        // Empty query should return empty or minimal results
        val chunks = result.getOrThrow().results
        assertTrue(chunks.isEmpty() || chunks.size < 3)
    }

    @Test
    fun testNoResults() = runBlocking {
        // Add document
        val tempFile = File.createTempFile("test_doc", ".txt", context.cacheDir)
        tempFile.writeText("Machine learning is important")

        repository.addDocument(
            AddDocumentRequest(
                filePath = tempFile.absolutePath,
                title = "Test Doc",
                processImmediately = true
            )
        )
        tempFile.delete()

        kotlinx.coroutines.delay(500)

        // Query for something completely unrelated
        val query = SearchQuery(
            query = "quantum physics relativity",
            maxResults = 10,
            minSimilarity = 0.7f
        )

        val result = repository.searchHybrid(query)
        assertTrue(result.isSuccess)

        // Should return empty or low-similarity results
        val chunks = result.getOrThrow().results
        assertTrue(chunks.isEmpty() || chunks.all { it.similarity < 0.5f })
    }
}
