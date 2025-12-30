// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/parser/TextChunker.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.parser

import com.augmentalis.rag.domain.*
import kotlinx.datetime.Clock

/**
 * Semantic text chunker for RAG system
 *
 * Splits document text into chunks suitable for embedding and retrieval.
 * Uses hybrid strategy: respects document structure while maintaining
 * consistent chunk sizes.
 */
class TextChunker(
    private val config: ChunkingConfig = ChunkingConfig()
) {
    /**
     * Chunk a parsed document into smaller pieces
     *
     * @param document Document to chunk
     * @param parsedDocument Parsed document with text and structure
     * @return List of chunks with metadata
     */
    fun chunk(document: Document, parsedDocument: ParsedDocument): List<Chunk> {
        return when (config.strategy) {
            ChunkingStrategy.FIXED_SIZE -> chunkFixedSize(document, parsedDocument)
            ChunkingStrategy.SEMANTIC -> chunkSemantic(document, parsedDocument)
            ChunkingStrategy.HYBRID -> chunkHybrid(document, parsedDocument)
        }
    }

    /**
     * Fixed-size chunking with overlap
     *
     * Simple strategy: split text into fixed token-sized chunks
     * with configurable overlap.
     */
    private fun chunkFixedSize(
        document: Document,
        parsedDocument: ParsedDocument
    ): List<Chunk> {
        val text = parsedDocument.text
        val chunks = mutableListOf<Chunk>()
        var currentOffset = 0
        var chunkIndex = 0

        while (currentOffset < text.length) {
            // Find end offset for this chunk
            val endOffset = TokenCounter.findOffsetForTokenCount(
                text,
                currentOffset,
                config.maxTokens
            )

            if (endOffset <= currentOffset) break

            // Extract chunk text
            val chunkText = text.substring(currentOffset, endOffset).trim()

            if (chunkText.isNotEmpty()) {
                // Determine page number for this chunk
                val pageNumber = findPageNumber(parsedDocument.pages, currentOffset)

                // Create chunk
                val chunk = Chunk(
                    id = "${document.id}_chunk_$chunkIndex",
                    documentId = document.id,
                    content = chunkText,
                    chunkIndex = chunkIndex,
                    startOffset = currentOffset,
                    endOffset = endOffset,
                    metadata = ChunkMetadata(
                        pageNumber = pageNumber,
                        tokens = TokenCounter.countTokens(chunkText),
                        semanticType = SemanticType.PARAGRAPH
                    ),
                    createdAt = Clock.System.now()
                )

                chunks.add(chunk)
                chunkIndex++
            }

            // Move to next chunk with overlap
            val overlapOffset = TokenCounter.findOffsetForTokenCount(
                text,
                currentOffset,
                config.maxTokens - config.overlapTokens
            )

            currentOffset = if (overlapOffset > currentOffset) {
                overlapOffset
            } else {
                endOffset  // No overlap if we can't move forward
            }
        }

        return chunks
    }

    /**
     * Semantic chunking based on document structure
     *
     * Respects section boundaries, paragraphs, and natural breaks.
     */
    private fun chunkSemantic(
        document: Document,
        parsedDocument: ParsedDocument
    ): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var chunkIndex = 0

        // If document has sections, chunk by section
        if (parsedDocument.sections.isNotEmpty() && config.respectSectionBoundaries) {
            for (section in parsedDocument.sections) {
                val sectionChunks = chunkSection(
                    document,
                    section,
                    chunkIndex,
                    findPageNumber(parsedDocument.pages, section.startOffset)
                )
                chunks.addAll(sectionChunks)
                chunkIndex += sectionChunks.size
            }
        } else {
            // Fall back to paragraph-based chunking
            val paragraphs = extractParagraphs(parsedDocument.text)
            chunks.addAll(
                chunkParagraphs(document, paragraphs, parsedDocument.pages)
            )
        }

        return chunks
    }

    /**
     * Hybrid chunking: semantic + size limits
     *
     * Best of both worlds: respect structure but enforce size limits.
     */
    private fun chunkHybrid(
        document: Document,
        parsedDocument: ParsedDocument
    ): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var chunkIndex = 0

        // Process sections if available
        val sections = if (parsedDocument.sections.isNotEmpty() && config.respectSectionBoundaries) {
            parsedDocument.sections
        } else {
            // Create artificial sections from text
            listOf(
                Section(
                    title = document.title,
                    level = 1,
                    text = parsedDocument.text,
                    startOffset = 0,
                    endOffset = parsedDocument.text.length
                )
            )
        }

        for (section in sections) {
            val sectionTokens = TokenCounter.countTokens(section.text)
            val pageNumber = findPageNumber(parsedDocument.pages, section.startOffset)

            if (sectionTokens <= config.maxTokens) {
                // Section fits in one chunk
                if (sectionTokens >= config.minChunkTokens) {
                    chunks.add(
                        createChunk(
                            document = document,
                            text = section.text,
                            chunkIndex = chunkIndex++,
                            startOffset = section.startOffset,
                            endOffset = section.endOffset,
                            section = section.title,
                            pageNumber = pageNumber,
                            semanticType = if (section.level == 1) SemanticType.HEADING else SemanticType.PARAGRAPH
                        )
                    )
                }
            } else {
                // Section too large, split it with overlap
                var currentOffset = section.startOffset
                val sectionEndOffset = section.endOffset

                while (currentOffset < sectionEndOffset) {
                    val chunkEndOffset = TokenCounter.findOffsetForTokenCount(
                        parsedDocument.text,
                        currentOffset,
                        config.maxTokens
                    ).coerceAtMost(sectionEndOffset)

                    if (chunkEndOffset <= currentOffset) break

                    val chunkText = parsedDocument.text.substring(
                        currentOffset,
                        chunkEndOffset
                    ).trim()

                    if (chunkText.isNotEmpty() && TokenCounter.countTokens(chunkText) >= config.minChunkTokens) {
                        chunks.add(
                            createChunk(
                                document = document,
                                text = chunkText,
                                chunkIndex = chunkIndex++,
                                startOffset = currentOffset,
                                endOffset = chunkEndOffset,
                                section = section.title,
                                pageNumber = findPageNumber(parsedDocument.pages, currentOffset),
                                semanticType = SemanticType.PARAGRAPH
                            )
                        )
                    }

                    // Move forward with overlap
                    val overlapOffset = TokenCounter.findOffsetForTokenCount(
                        parsedDocument.text,
                        currentOffset,
                        config.maxTokens - config.overlapTokens
                    )

                    currentOffset = if (overlapOffset > currentOffset) {
                        overlapOffset
                    } else {
                        chunkEndOffset
                    }
                }
            }
        }

        return chunks
    }

    /**
     * Chunk a single section
     */
    private fun chunkSection(
        document: Document,
        section: Section,
        startChunkIndex: Int,
        pageNumber: Int?
    ): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        val sectionTokens = TokenCounter.countTokens(section.text)

        if (sectionTokens <= config.maxTokens) {
            // Section fits in one chunk
            chunks.add(
                createChunk(
                    document = document,
                    text = section.text,
                    chunkIndex = startChunkIndex,
                    startOffset = section.startOffset,
                    endOffset = section.endOffset,
                    section = section.title,
                    pageNumber = pageNumber,
                    semanticType = SemanticType.PARAGRAPH
                )
            )
        } else {
            // Split section into multiple chunks
            var currentOffset = section.startOffset
            var chunkIndex = startChunkIndex

            while (currentOffset < section.endOffset) {
                val endOffset = TokenCounter.findOffsetForTokenCount(
                    section.text,
                    currentOffset - section.startOffset,
                    config.maxTokens
                ) + section.startOffset

                val chunkText = section.text.substring(
                    currentOffset - section.startOffset,
                    (endOffset - section.startOffset).coerceAtMost(section.text.length)
                ).trim()

                if (chunkText.isNotEmpty()) {
                    chunks.add(
                        createChunk(
                            document = document,
                            text = chunkText,
                            chunkIndex = chunkIndex++,
                            startOffset = currentOffset,
                            endOffset = endOffset,
                            section = section.title,
                            pageNumber = pageNumber,
                            semanticType = SemanticType.PARAGRAPH
                        )
                    )
                }

                currentOffset = endOffset
            }
        }

        return chunks
    }

    /**
     * Extract paragraphs from text
     */
    private fun extractParagraphs(text: String): List<String> {
        return text.split(Regex("\n\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Chunk paragraphs, combining small ones
     */
    private fun chunkParagraphs(
        document: Document,
        paragraphs: List<String>,
        pages: List<Page>
    ): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var chunkIndex = 0
        var currentChunkText = ""
        var currentChunkStart = 0
        var currentOffset = 0

        for (paragraph in paragraphs) {
            val paragraphTokens = TokenCounter.countTokens(paragraph)
            val currentTokens = TokenCounter.countTokens(currentChunkText)

            if (currentTokens + paragraphTokens > config.maxTokens && currentChunkText.isNotEmpty()) {
                // Flush current chunk
                chunks.add(
                    createChunk(
                        document = document,
                        text = currentChunkText,
                        chunkIndex = chunkIndex++,
                        startOffset = currentChunkStart,
                        endOffset = currentOffset,
                        pageNumber = findPageNumber(pages, currentChunkStart),
                        semanticType = SemanticType.PARAGRAPH
                    )
                )

                currentChunkText = paragraph
                currentChunkStart = currentOffset
            } else {
                if (currentChunkText.isNotEmpty()) {
                    currentChunkText += "\n\n$paragraph"
                } else {
                    currentChunkText = paragraph
                    currentChunkStart = currentOffset
                }
            }

            currentOffset += paragraph.length + 2  // +2 for \n\n
        }

        // Flush last chunk
        if (currentChunkText.isNotEmpty()) {
            chunks.add(
                createChunk(
                    document = document,
                    text = currentChunkText,
                    chunkIndex = chunkIndex,
                    startOffset = currentChunkStart,
                    endOffset = currentOffset,
                    pageNumber = findPageNumber(pages, currentChunkStart),
                    semanticType = SemanticType.PARAGRAPH
                )
            )
        }

        return chunks
    }

    /**
     * Create a chunk with metadata
     */
    private fun createChunk(
        document: Document,
        text: String,
        chunkIndex: Int,
        startOffset: Int,
        endOffset: Int,
        section: String? = null,
        heading: String? = null,
        pageNumber: Int? = null,
        semanticType: SemanticType = SemanticType.PARAGRAPH
    ): Chunk {
        return Chunk(
            id = "${document.id}_chunk_$chunkIndex",
            documentId = document.id,
            content = text,
            chunkIndex = chunkIndex,
            startOffset = startOffset,
            endOffset = endOffset,
            metadata = ChunkMetadata(
                section = section,
                heading = heading,
                pageNumber = pageNumber,
                tokens = TokenCounter.countTokens(text),
                semanticType = semanticType
            ),
            createdAt = Clock.System.now()
        )
    }

    /**
     * Find page number for a character offset
     */
    private fun findPageNumber(pages: List<Page>, offset: Int): Int? {
        return pages.firstOrNull { offset >= it.startOffset && offset < it.endOffset }?.number
    }
}
