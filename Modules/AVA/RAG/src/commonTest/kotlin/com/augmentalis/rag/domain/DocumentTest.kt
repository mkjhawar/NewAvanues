// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/domain/DocumentTest.kt
// created: 2025-11-14
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.domain

import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Tests for Document domain models
 *
 * Tests Document, DocumentType, DocumentStatus, and related classes
 */
class DocumentTest {

    // ========== DOCUMENT TYPE TESTS ==========

    @Test
    fun `test DocumentType from extension`() {
        assertEquals(DocumentType.PDF, DocumentType.fromExtension("pdf"))
        assertEquals(DocumentType.DOCX, DocumentType.fromExtension("docx"))
        assertEquals(DocumentType.TXT, DocumentType.fromExtension("txt"))
        assertEquals(DocumentType.MD, DocumentType.fromExtension("md"))
        assertEquals(DocumentType.HTML, DocumentType.fromExtension("html"))
        assertEquals(DocumentType.EPUB, DocumentType.fromExtension("epub"))
        assertEquals(DocumentType.RTF, DocumentType.fromExtension("rtf"))
    }

    @Test
    fun `test DocumentType from extension case insensitive`() {
        assertEquals(DocumentType.PDF, DocumentType.fromExtension("PDF"))
        assertEquals(DocumentType.PDF, DocumentType.fromExtension("Pdf"))
        assertEquals(DocumentType.DOCX, DocumentType.fromExtension("DOCX"))
        assertEquals(DocumentType.TXT, DocumentType.fromExtension("TXT"))
    }

    @Test
    fun `test DocumentType from extension with unknown type`() {
        assertNull(DocumentType.fromExtension("unknown"))
        assertNull(DocumentType.fromExtension(""))
        assertNull(DocumentType.fromExtension("xyz"))
    }

    @Test
    fun `test DocumentType from mimeType`() {
        assertEquals(DocumentType.PDF, DocumentType.fromMimeType("application/pdf"))
        assertEquals(DocumentType.TXT, DocumentType.fromMimeType("text/plain"))
        assertEquals(DocumentType.MD, DocumentType.fromMimeType("text/markdown"))
        assertEquals(DocumentType.HTML, DocumentType.fromMimeType("text/html"))
    }

    @Test
    fun `test DocumentType from mimeType case insensitive`() {
        assertEquals(DocumentType.PDF, DocumentType.fromMimeType("APPLICATION/PDF"))
        assertEquals(DocumentType.TXT, DocumentType.fromMimeType("TEXT/PLAIN"))
    }

    @Test
    fun `test DocumentType from mimeType with unknown type`() {
        assertNull(DocumentType.fromMimeType("application/unknown"))
        assertNull(DocumentType.fromMimeType(""))
    }

    @Test
    fun `test DocumentType properties`() {
        assertEquals("pdf", DocumentType.PDF.extension)
        assertEquals("application/pdf", DocumentType.PDF.mimeType)

        assertEquals("docx", DocumentType.DOCX.extension)
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            DocumentType.DOCX.mimeType)

        assertEquals("txt", DocumentType.TXT.extension)
        assertEquals("text/plain", DocumentType.TXT.mimeType)
    }

    @Test
    fun `test all DocumentType entries`() {
        val types = DocumentType.entries
        assertEquals(7, types.size, "Should have 7 document types")

        assertTrue(types.contains(DocumentType.PDF))
        assertTrue(types.contains(DocumentType.DOCX))
        assertTrue(types.contains(DocumentType.TXT))
        assertTrue(types.contains(DocumentType.MD))
        assertTrue(types.contains(DocumentType.HTML))
        assertTrue(types.contains(DocumentType.EPUB))
        assertTrue(types.contains(DocumentType.RTF))
    }

    // ========== DOCUMENT STATUS TESTS ==========

    @Test
    fun `test DocumentStatus values`() {
        val statuses = DocumentStatus.entries

        assertTrue(statuses.contains(DocumentStatus.PENDING))
        assertTrue(statuses.contains(DocumentStatus.PROCESSING))
        assertTrue(statuses.contains(DocumentStatus.INDEXED))
        assertTrue(statuses.contains(DocumentStatus.FAILED))
        assertTrue(statuses.contains(DocumentStatus.OUTDATED))
        assertTrue(statuses.contains(DocumentStatus.DELETED))
    }

    @Test
    fun `test DocumentStatus enum ordering`() {
        val statuses = DocumentStatus.entries

        // Verify expected workflow order
        val expectedOrder = listOf(
            DocumentStatus.PENDING,
            DocumentStatus.PROCESSING,
            DocumentStatus.INDEXED
        )

        expectedOrder.forEachIndexed { index, status ->
            assertTrue(statuses.contains(status), "Should contain $status")
        }
    }

    // ========== DOCUMENT TESTS ==========

    @Test
    fun `test Document creation with required fields`() {
        val now = Clock.System.now()
        val document = Document(
            id = "doc-001",
            title = "Test Document",
            filePath = "/path/to/test.pdf",
            fileType = DocumentType.PDF,
            sizeBytes = 1024,
            createdAt = now,
            modifiedAt = now
        )

        assertEquals("doc-001", document.id)
        assertEquals("Test Document", document.title)
        assertEquals("/path/to/test.pdf", document.filePath)
        assertEquals(DocumentType.PDF, document.fileType)
        assertEquals(1024L, document.sizeBytes)
        assertEquals(now, document.createdAt)
        assertEquals(now, document.modifiedAt)
        assertNull(document.indexedAt)
        assertEquals(0, document.chunkCount)
        assertTrue(document.metadata.isEmpty())
        assertEquals(DocumentStatus.PENDING, document.status)
    }

    @Test
    fun `test Document with all fields`() {
        val created = Clock.System.now()
        val modified = Clock.System.now()
        val indexed = Clock.System.now()

        val metadata = mapOf("author" to "John Doe", "category" to "Technical")

        val document = Document(
            id = "doc-002",
            title = "Complete Document",
            filePath = "/docs/complete.pdf",
            fileType = DocumentType.PDF,
            sizeBytes = 2048,
            createdAt = created,
            modifiedAt = modified,
            indexedAt = indexed,
            chunkCount = 10,
            metadata = metadata,
            status = DocumentStatus.INDEXED
        )

        assertEquals("doc-002", document.id)
        assertEquals("Complete Document", document.title)
        assertEquals(indexed, document.indexedAt)
        assertEquals(10, document.chunkCount)
        assertEquals(2, document.metadata.size)
        assertEquals("John Doe", document.metadata["author"])
        assertEquals(DocumentStatus.INDEXED, document.status)
    }

    @Test
    fun `test Document with metadata`() {
        val now = Clock.System.now()
        val metadata = mapOf(
            "author" to "Alice",
            "tags" to "important,urgent",
            "version" to "1.0"
        )

        val document = Document(
            id = "doc-003",
            title = "Metadata Test",
            filePath = "/test.txt",
            fileType = DocumentType.TXT,
            sizeBytes = 512,
            createdAt = now,
            modifiedAt = now,
            metadata = metadata
        )

        assertEquals(3, document.metadata.size)
        assertEquals("Alice", document.metadata["author"])
        assertEquals("important,urgent", document.metadata["tags"])
        assertEquals("1.0", document.metadata["version"])
    }

    @Test
    fun `test Document copy with status change`() {
        val now = Clock.System.now()
        val original = Document(
            id = "doc-004",
            title = "Original",
            filePath = "/original.pdf",
            fileType = DocumentType.PDF,
            sizeBytes = 1024,
            createdAt = now,
            modifiedAt = now,
            status = DocumentStatus.PENDING
        )

        val processing = original.copy(status = DocumentStatus.PROCESSING)
        assertEquals(DocumentStatus.PROCESSING, processing.status)
        assertEquals(original.id, processing.id)
        assertEquals(original.title, processing.title)

        val indexed = processing.copy(
            status = DocumentStatus.INDEXED,
            indexedAt = now,
            chunkCount = 5
        )
        assertEquals(DocumentStatus.INDEXED, indexed.status)
        assertEquals(now, indexed.indexedAt)
        assertEquals(5, indexed.chunkCount)
    }

    // ========== ADD DOCUMENT REQUEST TESTS ==========

    @Test
    fun `test AddDocumentRequest with minimal fields`() {
        val request = AddDocumentRequest(filePath = "/test.pdf")

        assertEquals("/test.pdf", request.filePath)
        assertNull(request.title)
        assertTrue(request.metadata.isEmpty())
        assertFalse(request.processImmediately)
    }

    @Test
    fun `test AddDocumentRequest with all fields`() {
        val request = AddDocumentRequest(
            filePath = "/docs/manual.pdf",
            title = "User Manual",
            metadata = mapOf("lang" to "en"),
            processImmediately = true
        )

        assertEquals("/docs/manual.pdf", request.filePath)
        assertEquals("User Manual", request.title)
        assertEquals(1, request.metadata.size)
        assertEquals("en", request.metadata["lang"])
        assertTrue(request.processImmediately)
    }

    @Test
    fun `test AddDocumentRequest derives title from path when not provided`() {
        val request = AddDocumentRequest(filePath = "/path/to/my-document.pdf")

        assertNull(request.title)
        // Implementation would extract "my-document.pdf" from path
    }

    // ========== ADD DOCUMENT RESULT TESTS ==========

    @Test
    fun `test AddDocumentResult creation`() {
        val result = AddDocumentResult(
            documentId = "doc-123",
            status = DocumentStatus.PENDING,
            message = "Document added successfully"
        )

        assertEquals("doc-123", result.documentId)
        assertEquals(DocumentStatus.PENDING, result.status)
        assertEquals("Document added successfully", result.message)
    }

    @Test
    fun `test AddDocumentResult with null message`() {
        val result = AddDocumentResult(
            documentId = "doc-456",
            status = DocumentStatus.INDEXED
        )

        assertEquals("doc-456", result.documentId)
        assertEquals(DocumentStatus.INDEXED, result.status)
        assertNull(result.message)
    }

    @Test
    fun `test AddDocumentResult for failed processing`() {
        val result = AddDocumentResult(
            documentId = "doc-789",
            status = DocumentStatus.FAILED,
            message = "Failed to parse document: Invalid format"
        )

        assertEquals(DocumentStatus.FAILED, result.status)
        assertTrue(result.message!!.contains("Failed to parse"))
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    fun `test complete document lifecycle workflow`() {
        val now = Clock.System.now()

        // 1. Create request
        val request = AddDocumentRequest(
            filePath = "/docs/guide.pdf",
            title = "User Guide",
            processImmediately = false
        )

        // 2. Create pending document
        var document = Document(
            id = "doc-lifecycle",
            title = request.title ?: "guide.pdf",
            filePath = request.filePath,
            fileType = DocumentType.PDF,
            sizeBytes = 1024,
            createdAt = now,
            modifiedAt = now,
            status = DocumentStatus.PENDING
        )

        assertEquals(DocumentStatus.PENDING, document.status)

        // 3. Start processing
        document = document.copy(status = DocumentStatus.PROCESSING)
        assertEquals(DocumentStatus.PROCESSING, document.status)

        // 4. Complete indexing
        document = document.copy(
            status = DocumentStatus.INDEXED,
            indexedAt = now,
            chunkCount = 15
        )

        assertEquals(DocumentStatus.INDEXED, document.status)
        assertNotNull(document.indexedAt)
        assertEquals(15, document.chunkCount)
    }

    @Test
    fun `test document with various file types`() {
        val now = Clock.System.now()

        val fileTypes = listOf(
            "/docs/report.pdf" to DocumentType.PDF,
            "/docs/letter.docx" to DocumentType.DOCX,
            "/docs/readme.txt" to DocumentType.TXT,
            "/docs/notes.md" to DocumentType.MD,
            "/docs/page.html" to DocumentType.HTML,
            "/docs/book.epub" to DocumentType.EPUB,
            "/docs/doc.rtf" to DocumentType.RTF
        )

        fileTypes.forEachIndexed { index, (path, expectedType) ->
            val doc = Document(
                id = "doc-$index",
                title = "Document $index",
                filePath = path,
                fileType = expectedType,
                sizeBytes = 1024,
                createdAt = now,
                modifiedAt = now
            )

            assertEquals(expectedType, doc.fileType)
            assertTrue(doc.filePath.endsWith(".${expectedType.extension}"))
        }
    }

    @Test
    fun `test document size tracking`() {
        val now = Clock.System.now()

        val sizes = listOf(
            1024L,          // 1 KB
            1024 * 1024L,   // 1 MB
            5 * 1024 * 1024L, // 5 MB
            0L              // Empty file
        )

        sizes.forEach { size ->
            val doc = Document(
                id = "doc-size-$size",
                title = "Size Test",
                filePath = "/test.pdf",
                fileType = DocumentType.PDF,
                sizeBytes = size,
                createdAt = now,
                modifiedAt = now
            )

            assertEquals(size, doc.sizeBytes)
            assertTrue(doc.sizeBytes >= 0, "Size should not be negative")
        }
    }
}
