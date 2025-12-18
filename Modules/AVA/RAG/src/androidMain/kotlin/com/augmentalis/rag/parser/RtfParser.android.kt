// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/parser/RtfParser.android.kt
// created: 2025-11-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.parser

import android.content.Context
import com.augmentalis.rag.domain.DocumentType
import java.io.File
import java.io.FileInputStream
import javax.swing.text.rtf.RTFEditorKit

/**
 * Parser for RTF (Rich Text Format) files
 *
 * Uses RTFEditorKit to convert RTF to plain text while preserving structure.
 * RTF is a common format for word processor documents (older than DOCX).
 */
class RtfParser(private val context: Context) : DocumentParser {

    override val supportedTypes: Set<DocumentType> = setOf(DocumentType.RTF)

    override suspend fun parse(
        filePath: String,
        documentType: DocumentType
    ): Result<ParsedDocument> {
        return try {
            require(documentType == DocumentType.RTF) {
                "RtfParser only supports RTF documents, got $documentType"
            }

            val file = File(filePath)
            require(file.exists()) { "File not found: $filePath" }

            // Use RTFEditorKit to parse RTF
            val rtfKit = RTFEditorKit()
            val doc = rtfKit.createDefaultDocument()

            FileInputStream(file).use { stream ->
                rtfKit.read(stream, doc, 0)
            }

            // Extract text from the document
            val text = doc.getText(0, doc.length)

            // Create pseudo-pages (2500 chars per page)
            val pages = createPages(text, charsPerPage = 2500)

            // Extract sections (simple heuristic based on text patterns)
            val sections = extractSections(text)

            // Metadata
            val metadata = mapOf(
                "file_name" to file.name,
                "file_size" to file.length().toString(),
                "format" to "RTF",
                "character_count" to text.length.toString()
            )

            Result.success(
                ParsedDocument(
                    text = text,
                    pages = pages,
                    sections = sections,
                    metadata = metadata,
                    totalPages = pages.size,
                    wordCount = text.split("\\s+".toRegex()).size
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse RTF file: ${e.message}", e))
        }
    }

    private fun createPages(text: String, charsPerPage: Int): List<Page> {
        val pages = mutableListOf<Page>()
        val chunks = text.chunked(charsPerPage)

        var currentOffset = 0
        chunks.forEachIndexed { index, chunk ->
            val endOffset = currentOffset + chunk.length
            pages.add(
                Page(
                    number = index + 1,
                    text = chunk,
                    startOffset = currentOffset,
                    endOffset = endOffset
                )
            )
            currentOffset = endOffset
        }

        return pages
    }

    /**
     * Extract sections based on common RTF heading patterns
     *
     * Looks for:
     * - Lines in all caps
     * - Numbered sections
     * - Lines followed by blank lines (paragraph breaks)
     */
    private fun extractSections(text: String): List<Section> {
        val sections = mutableListOf<Section>()
        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }

        var currentOffset = 0

        paragraphs.forEachIndexed { index, paragraph ->
            val trimmed = paragraph.trim()
            val firstLine = trimmed.lines().firstOrNull() ?: ""

            // Check if this paragraph looks like a heading
            val isHeading = when {
                // All caps (at least 10 chars, max 100 chars)
                firstLine.matches(Regex("^[A-Z][A-Z ]{9,99}$")) -> true
                // Numbered section (1. Title, 1.1 Title, etc.)
                firstLine.matches(Regex("^\\d+(\\.\\d+)*\\.?\\s+[A-Z].+")) -> true
                // Chapter/Section prefix
                firstLine.matches(Regex("^(Chapter|Section|Part)\\s+\\d+.*", RegexOption.IGNORE_CASE)) -> true
                else -> false
            }

            if (isHeading) {
                // Determine heading level
                val level = when {
                    firstLine.matches(Regex("^\\d+\\.\\s+.+")) -> 1  // "1. Title"
                    firstLine.matches(Regex("^\\d+\\.\\d+\\.\\s+.+")) -> 2  // "1.1. Title"
                    firstLine.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\s+.+")) -> 3  // "1.1.1. Title"
                    else -> 1
                }

                // Find section content (this paragraph + following non-heading paragraphs)
                val sectionParagraphs = mutableListOf(paragraph)
                var nextIndex = index + 1

                while (nextIndex < paragraphs.size) {
                    val nextPara = paragraphs[nextIndex]
                    val nextFirstLine = nextPara.trim().lines().firstOrNull() ?: ""

                    val nextIsHeading = nextFirstLine.matches(Regex("^[A-Z][A-Z ]{9,99}$")) ||
                            nextFirstLine.matches(Regex("^\\d+(\\.\\d+)*\\.?\\s+[A-Z].+")) ||
                            nextFirstLine.matches(Regex("^(Chapter|Section|Part)\\s+\\d+.*", RegexOption.IGNORE_CASE))

                    if (nextIsHeading) break

                    sectionParagraphs.add(nextPara)
                    nextIndex++
                }

                val sectionText = sectionParagraphs.joinToString("\n\n")
                val endOffset = currentOffset + sectionText.length

                sections.add(
                    Section(
                        title = firstLine,
                        level = level,
                        text = sectionText,
                        startOffset = currentOffset,
                        endOffset = endOffset,
                        pageNumber = (currentOffset / 2500) + 1
                    )
                )
            }

            currentOffset += paragraph.length + 2 // +2 for \n\n
        }

        return sections
    }
}
