// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/parser/TxtParser.android.kt
// created: 2025-11-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.parser

import android.content.Context
import com.augmentalis.rag.domain.DocumentType
import java.io.File

/**
 * Parser for plain text files (.txt)
 *
 * Simple parser that reads text files directly without any special processing.
 * Preserves line breaks and basic structure.
 */
class TxtParser(private val context: Context) : DocumentParser {

    override val supportedTypes: Set<DocumentType> = setOf(DocumentType.TXT)

    override suspend fun parse(
        filePath: String,
        documentType: DocumentType
    ): Result<ParsedDocument> {
        return try {
            require(documentType == DocumentType.TXT) {
                "TxtParser only supports TXT documents, got $documentType"
            }

            val file = File(filePath)
            require(file.exists()) { "File not found: $filePath" }

            // Read entire file
            val text = file.readText(Charsets.UTF_8)

            // Split into pseudo-pages (every 50 lines for navigation purposes)
            val lines = text.lines()
            val linesPerPage = 50
            val pages = mutableListOf<Page>()

            var currentOffset = 0
            lines.chunked(linesPerPage).forEachIndexed { index, pageLines ->
                val pageText = pageLines.joinToString("\n")
                val endOffset = currentOffset + pageText.length

                pages.add(
                    Page(
                        number = index + 1,
                        text = pageText,
                        startOffset = currentOffset,
                        endOffset = endOffset
                    )
                )

                currentOffset = endOffset + 1 // +1 for newline
            }

            // Extract sections (lines starting with common markers)
            val sections = extractSections(text)

            // Metadata
            val metadata = mapOf(
                "file_name" to file.name,
                "file_size" to file.length().toString(),
                "encoding" to "UTF-8",
                "line_count" to lines.size.toString()
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
            Result.failure(Exception("Failed to parse TXT file: ${e.message}", e))
        }
    }

    /**
     * Extract sections based on common heading patterns
     *
     * Looks for lines that start with:
     * - All caps (SECTION HEADING)
     * - Number followed by period (1. Section)
     * - Hash symbols (# Markdown-style)
     * - Underlined text (text followed by ===== or -----)
     */
    private fun extractSections(text: String): List<Section> {
        val sections = mutableListOf<Section>()
        val lines = text.lines()
        var currentOffset = 0

        for (i in lines.indices) {
            val line = lines[i].trim()
            val nextLine = if (i + 1 < lines.size) lines[i + 1].trim() else ""

            // Check for section heading patterns
            val sectionInfo = when {
                // Pattern 1: All caps (minimum 3 words)
                line.matches(Regex("^[A-Z][A-Z ]{10,}$")) -> {
                    SectionInfo(title = line, level = 1)
                }
                // Pattern 2: Numbered section (1. Title, 1.1. Subtitle, etc.)
                line.matches(Regex("^\\d+(\\.\\d+)*\\.\\s+.+")) -> {
                    val dotCount = line.takeWhile { it == '.' || it.isDigit() }.count { it == '.' }
                    val title = line.substringAfter(". ")
                    SectionInfo(title = title, level = dotCount)
                }
                // Pattern 3: Markdown-style heading (# Title)
                line.matches(Regex("^#{1,6}\\s+.+")) -> {
                    val level = line.takeWhile { it == '#' }.length
                    val title = line.trimStart('#').trim()
                    SectionInfo(title = title, level = level)
                }
                // Pattern 4: Underlined heading (next line is ===== or -----)
                nextLine.matches(Regex("^[=]{3,}$")) -> {
                    SectionInfo(title = line, level = 1)
                }
                nextLine.matches(Regex("^[-]{3,}$")) -> {
                    SectionInfo(title = line, level = 2)
                }
                else -> null
            }

            if (sectionInfo != null) {
                // Find section end (next heading or end of document)
                val endIndex = findSectionEnd(lines, i + 1)
                val sectionText = lines.subList(i, endIndex).joinToString("\n")
                val endOffset = currentOffset + sectionText.length

                sections.add(
                    Section(
                        title = sectionInfo.title,
                        level = sectionInfo.level,
                        text = sectionText,
                        startOffset = currentOffset,
                        endOffset = endOffset,
                        pageNumber = (currentOffset / 1000) + 1 // Rough page estimate
                    )
                )
            }

            currentOffset += lines[i].length + 1 // +1 for newline
        }

        return sections
    }

    private fun findSectionEnd(lines: List<String>, startIndex: Int): Int {
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            val nextLine = if (i + 1 < lines.size) lines[i + 1].trim() else ""

            // Check if this is a new heading
            val isHeading = line.matches(Regex("^[A-Z][A-Z ]{10,}$")) ||
                    line.matches(Regex("^\\d+(\\.\\d+)*\\.\\s+.+")) ||
                    line.matches(Regex("^#{1,6}\\s+.+")) ||
                    nextLine.matches(Regex("^[=]{3,}$")) ||
                    nextLine.matches(Regex("^[-]{3,}$"))

            if (isHeading) {
                return i
            }
        }
        return lines.size
    }

    private data class SectionInfo(val title: String, val level: Int)
}
