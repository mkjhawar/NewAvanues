// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/data/MetadataFilteringTest.kt
// created: 2025-11-28
// author: AVA AI Team - Phase 3.1: Metadata Filtering
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.rag.domain.*
import com.augmentalis.rag.embeddings.EmbeddingProvider
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Comprehensive tests for metadata filtering in RAG search
 *
 * Tests all filter types:
 * - Document type filtering (PDF, DOCX, TXT, etc.)
 * - Date range filtering (creation/modification dates)
 * - File size filtering (min/max bytes)
 * - Combined filters with AND logic
 * - NULL filter handling (no filtering)
 *
 * Part of: RAG Phase 3.1 - Metadata Filtering (+1% RAG grade)
 * Target: 100% RAG grade on Android
 */
@RunWith(AndroidJUnit4::class)
class MetadataFilteringTest {

    private lateinit var context: Context
    private lateinit var repository: SQLiteRAGRepository
    private lateinit var mockEmbeddingProvider: MockEmbeddingProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockEmbeddingProvider = MockEmbeddingProvider()

        repository = SQLiteRAGRepository(
            context = context,
            embeddingProvider = mockEmbeddingProvider,
            chunkingConfig = ChunkingConfig(
                strategy = ChunkingStrategy.FIXED_SIZE,
                maxTokens = 100,
                overlapTokens = 10
            ),
            enableClustering = false  // Disable for simpler testing
        )
    }

    @After
    fun tearDown() = runBlocking {
        repository.clearAll()

        // Clear database
        val dbFile = context.getDatabasePath("rag_database")
        dbFile.delete()
    }

    // ========== FILE TYPE FILTERING TESTS ==========

    @Test
    fun testFilterByDocumentType_SingleType() = runBlocking {
        // Add documents of different types
        val pdfDoc = addTestDocument(fileType = DocumentType.PDF, content = "PDF content")
        val docxDoc = addTestDocument(fileType = DocumentType.DOCX, content = "DOCX content")
        val txtDoc = addTestDocument(fileType = DocumentType.TXT, content = "TXT content")

        // Search with PDF filter only
        val filters = SearchFilters(documentTypes = listOf(DocumentType.PDF))
        val query = SearchQuery("content", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return PDF chunks
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            assertEquals("All results should be from PDF documents",
                DocumentType.PDF, searchResult.document?.fileType)
        }
    }

    @Test
    fun testFilterByDocumentType_MultipleTypes() = runBlocking {
        // Add documents of different types
        val pdfDoc = addTestDocument(fileType = DocumentType.PDF, content = "Test content")
        val docxDoc = addTestDocument(fileType = DocumentType.DOCX, content = "Test content")
        val txtDoc = addTestDocument(fileType = DocumentType.TXT, content = "Test content")

        // Search with PDF and DOCX filter
        val filters = SearchFilters(documentTypes = listOf(DocumentType.PDF, DocumentType.DOCX))
        val query = SearchQuery("content", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should return PDF and DOCX, but not TXT
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            val fileType = searchResult.document?.fileType
            assertTrue("Should only return PDF or DOCX",
                fileType == DocumentType.PDF || fileType == DocumentType.DOCX)
            assertNotEquals("Should not return TXT", DocumentType.TXT, fileType)
        }
    }

    // ========== DATE RANGE FILTERING TESTS ==========

    @Test
    fun testFilterByDateRange_RecentOnly() = runBlocking {
        val now = Clock.System.now()
        val yesterday = now - 1.days
        val weekAgo = now - 7.days

        // Add documents with different timestamps
        val oldDoc = addTestDocument(
            content = "Old document",
            createdAt = weekAgo
        )
        val recentDoc = addTestDocument(
            content = "Recent document",
            createdAt = now
        )

        // Filter for documents from last 2 days
        val filters = SearchFilters(
            dateRange = DateRange(
                start = (now - 2.days).toString(),
                end = now.toString()
            )
        )
        val query = SearchQuery("document", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return recent document
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            val docId = searchResult.document?.id
            assertNotEquals("Should not return old document", oldDoc.id, docId)
        }
    }

    @Test
    fun testFilterByDateRange_StartDateOnly() = runBlocking {
        val now = Clock.System.now()
        val yesterday = now - 1.days
        val weekAgo = now - 7.days

        // Add documents with different timestamps
        val oldDoc = addTestDocument(content = "Old doc", createdAt = weekAgo)
        val midDoc = addTestDocument(content = "Mid doc", createdAt = yesterday)
        val newDoc = addTestDocument(content = "New doc", createdAt = now)

        // Filter for documents after yesterday
        val filters = SearchFilters(
            dateRange = DateRange(start = yesterday.toString(), end = null)
        )
        val query = SearchQuery("doc", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should return mid and new, but not old
        assertTrue("Should find results", result.results.isNotEmpty())
        val resultDocIds = result.results.map { it.document?.id }
        assertFalse("Should not include old document", resultDocIds.contains(oldDoc.id))
    }

    @Test
    fun testFilterByDateRange_EndDateOnly() = runBlocking {
        val now = Clock.System.now()
        val yesterday = now - 1.days
        val weekAgo = now - 7.days

        // Add documents with different timestamps
        val oldDoc = addTestDocument(content = "Old doc", createdAt = weekAgo)
        val midDoc = addTestDocument(content = "Mid doc", createdAt = yesterday)
        val newDoc = addTestDocument(content = "New doc", createdAt = now)

        // Filter for documents before yesterday
        val filters = SearchFilters(
            dateRange = DateRange(start = null, end = yesterday.toString())
        )
        val query = SearchQuery("doc", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should return old and mid, but not new
        assertTrue("Should find results", result.results.isNotEmpty())
        val resultDocIds = result.results.map { it.document?.id }
        assertFalse("Should not include new document", resultDocIds.contains(newDoc.id))
    }

    // ========== FILE SIZE FILTERING TESTS ==========

    @Test
    fun testFilterByFileSize_SmallFilesOnly() = runBlocking {
        // Add documents of different sizes
        val smallDoc = addTestDocument(
            content = "Small",  // ~5 bytes
            fileSize = 5L
        )
        val mediumDoc = addTestDocument(
            content = "Medium size document content here",  // ~32 bytes
            fileSize = 32L
        )
        val largeDoc = addTestDocument(
            content = "Large document with lots of content " * 100,
            fileSize = 10_000_000L  // 10 MB
        )

        // Filter for files under 1KB
        val filters = SearchFilters(
            minFileSize = null,
            maxFileSize = 1000L
        )
        val query = SearchQuery("document", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return small and medium documents
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            val fileSize = searchResult.document?.sizeBytes ?: 0L
            assertTrue("File size should be under 1KB", fileSize < 1000L)
        }
    }

    @Test
    fun testFilterByFileSize_LargeFilesOnly() = runBlocking {
        // Add documents of different sizes
        val smallDoc = addTestDocument(content = "Small", fileSize = 100L)
        val mediumDoc = addTestDocument(content = "Medium", fileSize = 50_000L)
        val largeDoc = addTestDocument(content = "Large", fileSize = 10_000_000L)

        // Filter for files over 1MB
        val filters = SearchFilters(
            minFileSize = 1_000_000L,
            maxFileSize = null
        )
        val query = SearchQuery("content", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return large document
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            val fileSize = searchResult.document?.sizeBytes ?: 0L
            assertTrue("File size should be over 1MB", fileSize >= 1_000_000L)
        }
    }

    @Test
    fun testFilterByFileSize_Range() = runBlocking {
        // Add documents of different sizes
        val tinyDoc = addTestDocument(content = "Tiny", fileSize = 10L)
        val smallDoc = addTestDocument(content = "Small", fileSize = 1000L)
        val mediumDoc = addTestDocument(content = "Medium", fileSize = 50_000L)
        val largeDoc = addTestDocument(content = "Large", fileSize = 10_000_000L)

        // Filter for files between 100 bytes and 100KB
        val filters = SearchFilters(
            minFileSize = 100L,
            maxFileSize = 100_000L
        )
        val query = SearchQuery("content", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return small and medium documents
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            val fileSize = searchResult.document?.sizeBytes ?: 0L
            assertTrue("File size should be in range", fileSize >= 100L && fileSize <= 100_000L)
        }
    }

    // ========== COMBINED FILTERS TESTS ==========

    @Test
    fun testCombinedFilters_TypeAndSize() = runBlocking {
        // Add various documents
        val smallPdf = addTestDocument(
            fileType = DocumentType.PDF,
            content = "Small PDF",
            fileSize = 1000L
        )
        val largePdf = addTestDocument(
            fileType = DocumentType.PDF,
            content = "Large PDF",
            fileSize = 10_000_000L
        )
        val smallDocx = addTestDocument(
            fileType = DocumentType.DOCX,
            content = "Small DOCX",
            fileSize = 1000L
        )

        // Filter for small PDFs only
        val filters = SearchFilters(
            documentTypes = listOf(DocumentType.PDF),
            minFileSize = null,
            maxFileSize = 10_000L
        )
        val query = SearchQuery("PDF", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return small PDF
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            assertEquals("Should be PDF", DocumentType.PDF, searchResult.document?.fileType)
            val fileSize = searchResult.document?.sizeBytes ?: 0L
            assertTrue("Should be small file", fileSize <= 10_000L)
        }
    }

    @Test
    fun testCombinedFilters_TypeAndDate() = runBlocking {
        val now = Clock.System.now()
        val yesterday = now - 1.days
        val weekAgo = now - 7.days

        // Add various documents
        val oldPdf = addTestDocument(
            fileType = DocumentType.PDF,
            content = "Old PDF",
            createdAt = weekAgo
        )
        val recentPdf = addTestDocument(
            fileType = DocumentType.PDF,
            content = "Recent PDF",
            createdAt = now
        )
        val recentDocx = addTestDocument(
            fileType = DocumentType.DOCX,
            content = "Recent DOCX",
            createdAt = now
        )

        // Filter for recent PDFs only
        val filters = SearchFilters(
            documentTypes = listOf(DocumentType.PDF),
            dateRange = DateRange(
                start = (now - 2.days).toString(),
                end = now.toString()
            )
        )
        val query = SearchQuery("PDF", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return recent PDF
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            assertEquals("Should be PDF", DocumentType.PDF, searchResult.document?.fileType)
            val docId = searchResult.document?.id
            assertNotEquals("Should not be old document", oldPdf.id, docId)
        }
    }

    @Test
    fun testCombinedFilters_AllThree() = runBlocking {
        val now = Clock.System.now()
        val yesterday = now - 1.days

        // Add various documents
        val targetDoc = addTestDocument(
            fileType = DocumentType.PDF,
            content = "Target document",
            fileSize = 5000L,
            createdAt = now
        )
        val wrongType = addTestDocument(
            fileType = DocumentType.DOCX,
            content = "Wrong type",
            fileSize = 5000L,
            createdAt = now
        )
        val wrongSize = addTestDocument(
            fileType = DocumentType.PDF,
            content = "Wrong size",
            fileSize = 10_000_000L,
            createdAt = now
        )
        val wrongDate = addTestDocument(
            fileType = DocumentType.PDF,
            content = "Wrong date",
            fileSize = 5000L,
            createdAt = yesterday - 10.days
        )

        // Filter for recent, small PDFs
        val filters = SearchFilters(
            documentTypes = listOf(DocumentType.PDF),
            dateRange = DateRange(
                start = (now - 2.days).toString(),
                end = now.toString()
            ),
            minFileSize = 1000L,
            maxFileSize = 10_000L
        )
        val query = SearchQuery("document", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should only return target document
        assertTrue("Should find results", result.results.isNotEmpty())
        result.results.forEach { searchResult ->
            assertEquals("Should match all filters", targetDoc.id, searchResult.document?.id)
        }
    }

    // ========== NULL FILTER TESTS (NO FILTERING) ==========

    @Test
    fun testNullFilters_NoFiltering() = runBlocking {
        // Add various documents
        val doc1 = addTestDocument(fileType = DocumentType.PDF, content = "Document 1")
        val doc2 = addTestDocument(fileType = DocumentType.DOCX, content = "Document 2")
        val doc3 = addTestDocument(fileType = DocumentType.TXT, content = "Document 3")

        // Search with empty filters
        val filters = SearchFilters()
        val query = SearchQuery("Document", filters = filters)
        val result = repository.searchHybrid(query).getOrThrow()

        // Should return all documents
        assertTrue("Should find all documents", result.results.size >= 3)
    }

    // ========== HELPER METHODS ==========

    private suspend fun addTestDocument(
        fileType: DocumentType = DocumentType.PDF,
        content: String,
        fileSize: Long? = null,
        createdAt: Instant = Clock.System.now()
    ): Document {
        val docId = UUID.randomUUID().toString()

        // Create test file
        val extension = fileType.extension
        val testFile = File(context.cacheDir, "test-$docId.$extension")
        testFile.writeText(content)

        // Create document object
        val document = Document(
            id = docId,
            title = "Test Document $docId",
            filePath = testFile.absolutePath,
            fileType = fileType,
            sizeBytes = fileSize ?: testFile.length(),
            createdAt = createdAt,
            modifiedAt = createdAt,
            status = DocumentStatus.INDEXED
        )

        // Create chunks manually
        val chunk = Chunk(
            id = UUID.randomUUID().toString(),
            documentId = docId,
            content = content,
            chunkIndex = 0,
            startOffset = 0,
            endOffset = content.length,
            metadata = ChunkMetadata(
                section = null,
                heading = null,
                pageNumber = 1,
                tokens = content.split(" ").size,
                semanticType = SemanticType.PARAGRAPH,
                importance = 0.5f
            ),
            createdAt = createdAt
        )

        // Add to repository using batch method
        repository.addDocumentBatch(document, listOf(chunk)).getOrThrow()

        return document
    }
}

/**
 * Mock embedding provider for testing
 * Returns fixed-size random embeddings
 */
private class MockEmbeddingProvider : EmbeddingProvider {
    override suspend fun embed(text: String): Result<Embedding.Float32> {
        val vector = FloatArray(384) { Math.random().toFloat() }
        return Result.success(Embedding.Float32(vector))
    }

    override suspend fun embedBatch(texts: List<String>): Result<List<Embedding.Float32>> {
        val embeddings = texts.map {
            val vector = FloatArray(384) { Math.random().toFloat() }
            Embedding.Float32(vector)
        }
        return Result.success(embeddings)
    }
}
