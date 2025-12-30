// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/parser/MarkdownParser.android.kt
// created: 2025-11-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.parser

import android.content.Context
import com.augmentalis.rag.domain.DocumentType
import java.io.File

/**
 * Parser for Markdown (.md) files
 *
 * Parses Markdown syntax including:
 * - Headings (# H1, ## H2, etc.)
 * - Code blocks (``` or indented)
 * - Lists (ordered and unordered)
 * - Links and images
 * - Emphasis (bold, italic)
 *
 * Preserves structure while converting to plain text for RAG processing.
 */
class MarkdownParser(private val context: Context) : DocumentParser {

    override val supportedTypes: Set<DocumentType> = setOf(DocumentType.MD)

    override suspend fun parse(
        filePath: String,
        documentType: DocumentType
    ): Result<ParsedDocument> {
        return try {
            require(documentType == DocumentType.MD) {
                "MarkdownParser only supports MD documents, got $documentType"
            }

            val file = File(filePath)
            require(file.exists()) { "File not found: $filePath" }

            // Read raw markdown
            val markdownText = file.readText(Charsets.UTF_8)

            // Convert markdown to plain text (preserving structure)
            val plainText = convertMarkdownToPlainText(markdownText)

            // Extract sections based on headings
            val sections = extractSectionsFromMarkdown(markdownText)

            // Create pages
            val pages = createPages(plainText, charsPerPage = 2500)

            // Extract metadata from YAML front matter if present
            val metadata = extractFrontMatter(markdownText).toMutableMap()
            metadata["file_name"] = file.name
            metadata["file_size"] = file.length().toString()
            metadata["format"] = "Markdown"

            Result.success(
                ParsedDocument(
                    text = plainText,
                    pages = pages,
                    sections = sections,
                    metadata = metadata,
                    totalPages = pages.size,
                    wordCount = plainText.split("\\s+".toRegex()).size
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse Markdown file: ${e.message}", e))
        }
    }

    /**
     * Convert Markdown to plain text while preserving readability
     */
    private fun convertMarkdownToPlainText(markdown: String): String {
        var text = markdown

        // Remove YAML front matter
        text = text.replace(Regex("^---\\n.*?\\n---\\n", RegexOption.DOT_MATCHES_ALL), "")

        // Convert headings (keep the text, remove # symbols)
        text = text.replace(Regex("^#{1,6}\\s+(.+)$", RegexOption.MULTILINE)) { match ->
            match.groupValues[1]
        }

        // Remove HTML tags
        text = text.replace(Regex("<[^>]+>"), "")

        // Convert links: [text](url) -> text (url)
        text = text.replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")) { match ->
            "${match.groupValues[1]} (${match.groupValues[2]})"
        }

        // Convert images: ![alt](url) -> [Image: alt]
        text = text.replace(Regex("!\\[([^\\]]+)\\]\\([^)]+\\)")) { match ->
            "[Image: ${match.groupValues[1]}]"
        }

        // Convert bold: **text** or __text__ -> text
        text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        text = text.replace(Regex("__([^_]+)__"), "$1")

        // Convert italic: *text* or _text_ -> text
        text = text.replace(Regex("\\*([^*]+)\\*"), "$1")
        text = text.replace(Regex("_([^_]+)_"), "$1")

        // Convert code blocks: ```code``` -> [Code: code]
        text = text.replace(Regex("```[a-z]*\\n([^`]+)```", RegexOption.DOT_MATCHES_ALL)) { match ->
            "[Code Block]\n${match.groupValues[1].trim()}\n[/Code Block]"
        }

        // Convert inline code: `code` -> code
        text = text.replace(Regex("`([^`]+)`"), "$1")

        // Convert horizontal rules
        text = text.replace(Regex("^(---|\\*\\*\\*|___)\\s*$", RegexOption.MULTILINE), "\n---\n")

        // Convert unordered lists: - item or * item -> • item
        text = text.replace(Regex("^[*-]\\s+(.+)$", RegexOption.MULTILINE), "• $1")

        // Convert ordered lists: 1. item -> 1. item (keep as is)
        // Already in good format

        // Clean up multiple blank lines
        text = text.replace(Regex("\n{3,}"), "\n\n")

        return text.trim()
    }

    /**
     * Extract sections based on Markdown headings
     */
    private fun extractSectionsFromMarkdown(markdown: String): List<Section> {
        val sections = mutableListOf<Section>()
        val lines = markdown.lines()

        // Find all heading lines
        val headingIndices = mutableListOf<Int>()
        lines.forEachIndexed { index, line ->
            if (line.matches(Regex("^#{1,6}\\s+.+"))) {
                headingIndices.add(index)
            }
        }

        // Extract sections between headings
        headingIndices.forEachIndexed { idx, startIndex ->
            val line = lines[startIndex]
            val level = line.takeWhile { it == '#' }.length
            val title = line.trimStart('#').trim()

            // Find end index (next heading or end of file)
            val endIndex = headingIndices.getOrNull(idx + 1) ?: lines.size

            // Extract section content
            val sectionLines = lines.subList(startIndex, endIndex)
            val sectionMarkdown = sectionLines.joinToString("\n")
            val sectionText = convertMarkdownToPlainText(sectionMarkdown)

            // Calculate offsets (approximate)
            var offset = 0
            for (i in 0 until startIndex) {
                offset += lines[i].length + 1 // +1 for newline
            }

            sections.add(
                Section(
                    title = title,
                    level = level,
                    text = sectionText,
                    startOffset = offset,
                    endOffset = offset + sectionMarkdown.length,
                    pageNumber = (offset / 2500) + 1
                )
            )
        }

        return sections
    }

    /**
     * Extract YAML front matter metadata
     *
     * Example:
     * ---
     * title: Document Title
     * author: John Doe
     * date: 2025-11-05
     * ---
     */
    private fun extractFrontMatter(markdown: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        val frontMatterRegex = Regex("^---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL)
        val match = frontMatterRegex.find(markdown)

        if (match != null) {
            val yamlContent = match.groupValues[1]
            val lines = yamlContent.lines()

            lines.forEach { line ->
                if (line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().trim('"', '\'')
                        metadata[key] = value
                    }
                }
            }
        }

        return metadata
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
}
