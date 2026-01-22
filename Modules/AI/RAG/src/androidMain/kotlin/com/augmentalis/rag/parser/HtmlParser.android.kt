// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/parser/HtmlParser.android.kt
// created: 2025-11-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.parser

import android.content.Context
import com.augmentalis.rag.domain.DocumentType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import java.io.File
import java.net.URL

/**
 * Parser for HTML files and web documents
 *
 * Features:
 * - Parse local HTML files
 * - Fetch and parse web pages (URLs)
 * - Extract clean text while preserving structure
 * - Identify sections based on headings (h1-h6)
 * - Handle both static HTML and dynamic web content
 * - Clean unwanted elements (scripts, styles, ads)
 */
class HtmlParser(private val context: Context) : DocumentParser {

    override val supportedTypes: Set<DocumentType> = setOf(DocumentType.HTML)

    /**
     * Parse HTML document from file or URL
     *
     * @param filePath Can be either:
     *   - Local file path: "/sdcard/document.html"
     *   - Web URL: "https://example.com/article.html"
     */
    override suspend fun parse(
        filePath: String,
        documentType: DocumentType
    ): Result<ParsedDocument> {
        return try {
            require(documentType == DocumentType.HTML) {
                "HtmlParser only supports HTML documents, got $documentType"
            }

            // Determine if it's a URL or local file
            val doc = if (isUrl(filePath)) {
                parseWebDocument(filePath)
            } else {
                parseLocalFile(filePath)
            }

            // Extract clean text
            val text = extractCleanText(doc)

            // Extract sections based on headings
            val sections = extractSections(doc)

            // Create pseudo-pages (chunk by character count for navigation)
            val pages = createPages(text, charsPerPage = 2500)

            // Extract metadata
            val metadata = extractMetadata(doc, filePath)

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
            Result.failure(Exception("Failed to parse HTML: ${e.message}", e))
        }
    }

    private fun isUrl(path: String): Boolean {
        return path.startsWith("http://", ignoreCase = true) ||
                path.startsWith("https://", ignoreCase = true)
    }

    private fun parseWebDocument(url: String): Document {
        return Jsoup.connect(url)
            .userAgent("AVA/1.0 (Android; RAG Document Parser)")
            .timeout(15000) // 15 second timeout
            .followRedirects(true)
            .get()
    }

    private fun parseLocalFile(filePath: String): Document {
        val file = File(filePath)
        require(file.exists()) { "File not found: $filePath" }

        return Jsoup.parse(file, "UTF-8")
    }

    /**
     * Extract clean text from HTML while preserving structure
     *
     * Removes:
     * - Scripts and styles
     * - Navigation menus
     * - Advertisements
     * - Social media widgets
     * - Comments
     */
    private fun extractCleanText(doc: Document): String {
        // Clone document to avoid modifying original
        val cleanDoc = doc.clone()

        // Remove unwanted elements
        cleanDoc.select("script, style, nav, aside, .advertisement, .social-share, .comments").remove()
        cleanDoc.select("[class*='ad-'], [class*='banner-'], [class*='popup']").remove()
        cleanDoc.select("iframe, embed, object").remove()

        // Try to find main content
        val mainContent = findMainContent(cleanDoc)

        // Extract text with preserved structure
        return mainContent.text()
    }

    /**
     * Find the main content area of the page
     *
     * Looks for common main content selectors:
     * - <main> tag
     * - <article> tag
     * - Elements with class/id containing "content", "article", "post"
     * - Falls back to <body> if nothing found
     */
    private fun findMainContent(doc: Document): Element {
        // Try semantic HTML5 tags first
        doc.selectFirst("main")?.let { return it }
        doc.selectFirst("article")?.let { return it }

        // Try common class/id patterns
        val contentSelectors = listOf(
            "#content", ".content", "#main-content", ".main-content",
            "#article", ".article", "#post", ".post",
            "[role=main]", ".article-body", ".post-content"
        )

        for (selector in contentSelectors) {
            doc.selectFirst(selector)?.let { return it }
        }

        // Fall back to body
        return doc.body()
    }

    /**
     * Extract sections based on HTML headings (h1-h6)
     */
    private fun extractSections(doc: Document): List<Section> {
        val sections = mutableListOf<Section>()
        val mainContent = findMainContent(doc)

        // Find all headings
        val headings = mainContent.select("h1, h2, h3, h4, h5, h6")

        headings.forEachIndexed { index, heading ->
            val level = heading.tagName().substring(1).toInt() // "h1" -> 1
            val title = heading.text()

            // Find content until next heading
            val content = extractSectionContent(heading, headings.getOrNull(index + 1))

            if (content.isNotBlank()) {
                sections.add(
                    Section(
                        title = title,
                        level = level,
                        text = content,
                        startOffset = 0, // Will be calculated later
                        endOffset = 0,
                        pageNumber = null
                    )
                )
            }
        }

        // Calculate offsets
        var currentOffset = 0
        sections.forEachIndexed { index, section ->
            val textLength = section.text.length
            sections[index] = section.copy(
                startOffset = currentOffset,
                endOffset = currentOffset + textLength
            )
            currentOffset += textLength + 1
        }

        return sections
    }

    /**
     * Extract text content between two headings
     */
    private fun extractSectionContent(startHeading: Element, endHeading: Element?): String {
        val content = StringBuilder()
        var currentElement = startHeading.nextElementSibling()

        while (currentElement != null && currentElement != endHeading) {
            // Only include content elements, skip navigation/ads
            if (isContentElement(currentElement)) {
                val text = currentElement.text()
                if (text.isNotBlank()) {
                    content.append(text).append("\n\n")
                }
            }
            currentElement = currentElement.nextElementSibling()
        }

        return content.toString().trim()
    }

    /**
     * Check if element is likely to be content (not navigation/ads)
     */
    private fun isContentElement(element: Element): Boolean {
        val tagName = element.tagName().lowercase()
        val className = element.className().lowercase()
        val id = element.id().lowercase()

        // Skip navigation/aside elements
        if (tagName in setOf("nav", "aside", "header", "footer")) return false

        // Skip elements with ad-related classes
        val skipPatterns = listOf("ad", "advertisement", "banner", "social", "share", "comment", "popup")
        if (skipPatterns.any { className.contains(it) || id.contains(it) }) return false

        // Include common content tags
        if (tagName in setOf("p", "div", "section", "article", "blockquote", "pre", "code", "ul", "ol", "li", "table")) return true

        return false
    }

    /**
     * Create pseudo-pages by chunking text
     */
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
     * Extract metadata from HTML document
     */
    private fun extractMetadata(doc: Document, sourcePath: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        // Basic info
        metadata["source"] = if (isUrl(sourcePath)) "web" else "local_file"
        metadata["source_path"] = sourcePath

        // Document title
        doc.title()?.let { metadata["title"] = it }

        // Meta tags
        doc.selectFirst("meta[name=description]")?.attr("content")?.let {
            metadata["description"] = it
        }
        doc.selectFirst("meta[name=author]")?.attr("content")?.let {
            metadata["author"] = it
        }
        doc.selectFirst("meta[name=keywords]")?.attr("content")?.let {
            metadata["keywords"] = it
        }
        doc.selectFirst("meta[property='og:title']")?.attr("content")?.let {
            metadata["og_title"] = it
        }
        doc.selectFirst("meta[property='og:description']")?.attr("content")?.let {
            metadata["og_description"] = it
        }

        // Published date (various formats)
        val dateSelectors = listOf(
            "meta[property='article:published_time']",
            "meta[name='publication_date']",
            "meta[name='date']",
            "time[datetime]"
        )
        for (selector in dateSelectors) {
            val dateElement = doc.selectFirst(selector)
            if (dateElement != null) {
                val date = dateElement.attr("content").ifEmpty { dateElement.attr("datetime") }
                if (date.isNotBlank()) {
                    metadata["published_date"] = date
                    break
                }
            }
        }

        // Canonical URL
        doc.selectFirst("link[rel=canonical]")?.attr("href")?.let {
            metadata["canonical_url"] = it
        }

        // Language
        doc.selectFirst("html")?.attr("lang")?.let {
            metadata["language"] = it
        }

        return metadata
    }
}
