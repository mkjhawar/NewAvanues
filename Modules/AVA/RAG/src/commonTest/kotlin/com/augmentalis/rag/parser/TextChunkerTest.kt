// filename: Universal/AVA/Features/RAG/src/commonTest/kotlin/com/augmentalis/ava/features/rag/parser/TextChunkerTest.kt
// created: 2025-11-14
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.parser

import com.augmentalis.rag.domain.*
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Comprehensive tests for TextChunker
 *
 * Tests all chunking strategies: FIXED_SIZE, SEMANTIC, HYBRID
 * Covers edge cases, overlap logic, and metadata generation
 */
class TextChunkerTest {

    private lateinit var chunker: TextChunker
    private lateinit var document: Document

    @BeforeTest
    fun setup() {
        val now = Clock.System.now()
        document = Document(
            id = "test-doc-001",
            title = "Test Document",
            filePath = "/test.txt",
            fileType = DocumentType.TXT,
            sizeBytes = 1000,
            createdAt = now,
            modifiedAt = now
        )
    }

    // ========== FIXED SIZE CHUNKING TESTS ==========

    @Test
    fun `test fixed size chunking with no overlap`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.FIXED_SIZE,
                maxTokens = 100,
                overlapTokens = 0,
                minChunkTokens = 10
            )
        )

        val text = "This is a test. ".repeat(50)  // ~200 tokens
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        // Should create 2 chunks (~100 tokens each)
        assertTrue(chunks.size >= 2, "Expected at least 2 chunks")
        chunks.forEach { chunk ->
            assertTrue(chunk.metadata.tokens <= 100, "Chunk exceeds max tokens")
            assertTrue(chunk.content.isNotEmpty(), "Chunk is empty")
            assertEquals(document.id, chunk.documentId, "Document ID mismatch")
        }
    }

    @Test
    fun `test fixed size chunking with overlap`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.FIXED_SIZE,
                maxTokens = 50,
                overlapTokens = 10,
                minChunkTokens = 5
            )
        )

        val text = "Word ".repeat(100)  // ~100 tokens
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        // With overlap, we should have more chunks
        assertTrue(chunks.size >= 2, "Expected overlapping chunks")

        // Verify chunk IDs are sequential
        chunks.forEachIndexed { index, chunk ->
            assertEquals("${document.id}_chunk_$index", chunk.id)
            assertEquals(index, chunk.chunkIndex)
        }
    }

    @Test
    fun `test fixed size chunking with empty text`() {
        chunker = TextChunker()

        val parsedDoc = ParsedDocument(
            text = "",
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        assertTrue(chunks.isEmpty(), "Should create no chunks for empty text")
    }

    @Test
    fun `test fixed size chunking with very short text`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.FIXED_SIZE,
                maxTokens = 100,
                minChunkTokens = 5
            )
        )

        val text = "Short text"
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        assertEquals(1, chunks.size, "Should create single chunk for short text")
        assertEquals(text.trim(), chunks[0].content)
    }

    // ========== SEMANTIC CHUNKING TESTS ==========

    @Test
    fun `test semantic chunking with sections`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.SEMANTIC,
                maxTokens = 100,
                respectSectionBoundaries = true
            )
        )

        val text = """
            Introduction
            This is the introduction section with some text.

            Chapter 1
            This is chapter 1 with different content.

            Chapter 2
            This is chapter 2 with more content.
        """.trimIndent()

        val sections = listOf(
            Section(
                title = "Introduction",
                level = 1,
                text = "This is the introduction section with some text.",
                startOffset = 0,
                endOffset = 48
            ),
            Section(
                title = "Chapter 1",
                level = 1,
                text = "This is chapter 1 with different content.",
                startOffset = 50,
                endOffset = 92
            ),
            Section(
                title = "Chapter 2",
                level = 1,
                text = "This is chapter 2 with more content.",
                startOffset = 94,
                endOffset = 130
            )
        )

        val parsedDoc = ParsedDocument(
            text = text,
            sections = sections,
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        // Should create chunks respecting section boundaries
        assertTrue(chunks.size >= 3, "Expected chunks for each section")
    }

    @Test
    fun `test semantic chunking without sections falls back to paragraphs`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.SEMANTIC,
                maxTokens = 100
            )
        )

        val text = """
            First paragraph with some text.

            Second paragraph with different content.

            Third paragraph with more information.
        """.trimIndent()

        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        assertTrue(chunks.isNotEmpty(), "Should create chunks from paragraphs")
    }

    // ========== HYBRID CHUNKING TESTS ==========

    @Test
    fun `test hybrid chunking respects section boundaries and size limits`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.HYBRID,
                maxTokens = 50,
                overlapTokens = 10,
                respectSectionBoundaries = true,
                minChunkTokens = 5
            )
        )

        val largeSection = "Word ".repeat(100)  // ~100 tokens (exceeds maxTokens)
        val sections = listOf(
            Section(
                title = "Large Section",
                level = 1,
                text = largeSection,
                startOffset = 0,
                endOffset = largeSection.length
            )
        )

        val parsedDoc = ParsedDocument(
            text = largeSection,
            sections = sections,
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        // Large section should be split into multiple chunks
        assertTrue(chunks.size >= 2, "Large section should be split")
        chunks.forEach { chunk ->
            assertTrue(chunk.metadata.tokens <= 50, "Chunk exceeds max tokens")
        }
    }

    @Test
    fun `test hybrid chunking keeps small sections together`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.HYBRID,
                maxTokens = 100,
                minChunkTokens = 1,  // Allow small chunks for this test
                respectSectionBoundaries = true
            )
        )

        val smallSection = "Small section text"
        val sections = listOf(
            Section(
                title = "Small Section",
                level = 1,
                text = smallSection,
                startOffset = 0,
                endOffset = smallSection.length
            )
        )

        val parsedDoc = ParsedDocument(
            text = smallSection,
            sections = sections,
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        // Small section should remain as single chunk
        assertEquals(1, chunks.size, "Small section should be single chunk")
        assertEquals(smallSection, chunks[0].content.trim())
    }

    @Test
    fun `test hybrid chunking with multiple sections of varying sizes`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.HYBRID,
                maxTokens = 50,
                overlapTokens = 5,
                minChunkTokens = 10
            )
        )

        val text = "Short section\n\n" + "Long section text. ".repeat(50)
        val sections = listOf(
            Section(
                title = "Short",
                level = 1,
                text = "Short section",
                startOffset = 0,
                endOffset = 13
            ),
            Section(
                title = "Long",
                level = 1,
                text = "Long section text. ".repeat(50),
                startOffset = 15,
                endOffset = text.length
            )
        )

        val parsedDoc = ParsedDocument(
            text = text,
            sections = sections,
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        // Short section = 1 chunk, Long section = multiple chunks
        assertTrue(chunks.size >= 2, "Expected multiple chunks")
    }

    // ========== METADATA TESTS ==========

    @Test
    fun `test chunks have correct metadata`() {
        chunker = TextChunker()

        val text = "Test text for metadata validation"
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = listOf(
                Page(number = 1, text = text, startOffset = 0, endOffset = text.length)
            )
        )

        val chunks = chunker.chunk(document, parsedDoc)

        chunks.forEach { chunk ->
            assertNotNull(chunk.metadata, "Metadata should exist")
            assertTrue(chunk.metadata.tokens > 0, "Token count should be positive")
            assertEquals(SemanticType.PARAGRAPH, chunk.metadata.semanticType)
            assertNotNull(chunk.createdAt, "Created timestamp should exist")
        }
    }

    @Test
    fun `test chunks include page numbers when available`() {
        chunker = TextChunker(
            config = ChunkingConfig(maxTokens = 50)
        )

        val text = "Page 1 content. ".repeat(20) + "Page 2 content. ".repeat(20)
        val page1Text = text.substring(0, 200)
        val page2Text = text.substring(200)

        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = listOf(
                Page(number = 1, text = page1Text, startOffset = 0, endOffset = 200),
                Page(number = 2, text = page2Text, startOffset = 200, endOffset = text.length)
            )
        )

        val chunks = chunker.chunk(document, parsedDoc)

        // Verify page numbers are set
        chunks.forEach { chunk ->
            // Page number should be set based on chunk offset
            val expectedPage = if (chunk.startOffset < 200) 1 else 2
            assertEquals(expectedPage, chunk.metadata.pageNumber,
                "Chunk should have correct page number")
        }
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `test chunking with special characters`() {
        chunker = TextChunker(
            config = ChunkingConfig(minChunkTokens = 1)
        )

        val text = "Special chars: @#$%^&*(){}[]|\\:;<>?,./~`"
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        assertTrue(chunks.isNotEmpty(), "Should handle special characters")
        assertEquals(text.trim(), chunks[0].content)
    }

    @Test
    fun `test chunking with unicode characters`() {
        chunker = TextChunker(
            config = ChunkingConfig(minChunkTokens = 1)
        )

        val text = "Unicode: 你好世界 مرحبا العالم Здравствуй мир 🌍🌎🌏"
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        assertTrue(chunks.isNotEmpty(), "Should handle unicode")
        assertTrue(chunks[0].content.contains("你好世界"), "Should preserve unicode")
    }

    @Test
    fun `test chunking with only whitespace`() {
        chunker = TextChunker()

        val text = "    \n\n\t\t    \n    "
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        assertTrue(chunks.isEmpty(), "Should create no chunks for whitespace-only text")
    }

    @Test
    fun `test chunk offsets are correct`() {
        chunker = TextChunker(
            config = ChunkingConfig(
                strategy = ChunkingStrategy.FIXED_SIZE,
                maxTokens = 20,
                overlapTokens = 0
            )
        )

        val text = "Word ".repeat(50)
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        chunks.forEach { chunk ->
            // Verify offsets are within text bounds
            assertTrue(chunk.startOffset >= 0, "Start offset negative")
            assertTrue(chunk.endOffset <= text.length, "End offset exceeds text length")
            assertTrue(chunk.startOffset < chunk.endOffset, "Start >= end")

            // Verify content matches offsets
            val expectedContent = text.substring(chunk.startOffset, chunk.endOffset).trim()
            assertEquals(expectedContent, chunk.content, "Content doesn't match offsets")
        }
    }

    // ========== CONFIGURATION TESTS ==========

    @Test
    fun `test custom chunking configuration`() {
        val config = ChunkingConfig(
            strategy = ChunkingStrategy.FIXED_SIZE,
            maxTokens = 75,
            overlapTokens = 15,
            minChunkTokens = 20,
            respectSectionBoundaries = false
        )

        chunker = TextChunker(config)

        val text = "Test ".repeat(100)
        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        chunks.forEach { chunk ->
            assertTrue(chunk.metadata.tokens <= 75, "Respects maxTokens")
        }
    }

    @Test
    fun `test default configuration creates reasonable chunks`() {
        chunker = TextChunker(
            config = ChunkingConfig(minChunkTokens = 1)  // Allow small chunks for test
        )

        val text = """
            This is a test document with multiple paragraphs.
            Each paragraph should be chunked appropriately.

            Second paragraph has different content.
            It should be handled correctly.

            Third paragraph provides more test data.
        """.trimIndent()

        val parsedDoc = ParsedDocument(
            text = text,
            sections = emptyList(),
            pages = emptyList()
        )

        val chunks = chunker.chunk(document, parsedDoc)

        assertTrue(chunks.isNotEmpty(), "Default config should produce chunks")
        chunks.forEach { chunk ->
            assertTrue(chunk.content.isNotBlank(), "Chunks should have content")
            assertTrue(chunk.metadata.tokens > 0, "Chunks should have token count")
        }
    }
}
