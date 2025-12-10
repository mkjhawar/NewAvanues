// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/parser/PdfParser.android.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.parser

import android.content.Context
import com.augmentalis.ava.features.rag.domain.DocumentType
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.io.StringWriter

/**
 * Android PDF parser using PdfBox-Android
 *
 * Uses TomRoush/PdfBox-Android for full text extraction from PDF documents.
 * Supports:
 * - Full text extraction with formatting
 * - Page-by-page extraction
 * - Metadata extraction
 * - Structure preservation
 */
class PdfParser(private val context: Context) : DocumentParser {
    override val supportedTypes = setOf(DocumentType.PDF)

    init {
        // Initialize PdfBox resource loader (required for Android)
        PDFBoxResourceLoader.init(context)
    }

    override suspend fun parse(filePath: String, documentType: DocumentType): Result<ParsedDocument> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("File not found: $filePath"))
            }

            // Load PDF document
            val document = PDDocument.load(file)

            try {
                val pageCount = document.numberOfPages
                val pages = mutableListOf<Page>()
                val fullTextBuilder = StringBuilder()
                var currentOffset = 0

                // Extract text from each page
                val textStripper = PDFTextStripper()

                for (pageIndex in 0 until pageCount) {
                    // Set page range for current page
                    textStripper.startPage = pageIndex + 1
                    textStripper.endPage = pageIndex + 1

                    // Extract text for this page
                    val pageText = textStripper.getText(document)

                    pages.add(
                        Page(
                            number = pageIndex + 1,
                            text = pageText,
                            startOffset = currentOffset,
                            endOffset = currentOffset + pageText.length
                        )
                    )

                    fullTextBuilder.append(pageText)
                    currentOffset += pageText.length
                }

                // Extract metadata
                val docInfo = document.documentInformation
                val metadata = buildMap {
                    put("pages", pageCount.toString())
                    put("parser", "PdfBox-Android")

                    docInfo?.let {
                        it.title?.let { title -> put("title", title) }
                        it.author?.let { author -> put("author", author) }
                        it.subject?.let { subject -> put("subject", subject) }
                        it.creator?.let { creator -> put("creator", creator) }
                        it.producer?.let { producer -> put("producer", producer) }
                        it.creationDate?.let { date -> put("creationDate", date.toString()) }
                        it.modificationDate?.let { date -> put("modificationDate", date.toString()) }
                    }
                }

                // Extract sections using font analysis
                val fullText = fullTextBuilder.toString()
                val sections = extractSections(document, fullText, pages)

                Result.success(
                    ParsedDocument(
                        text = fullText,
                        pages = pages,
                        sections = sections,
                        metadata = metadata,
                        totalPages = pageCount
                    )
                )
            } finally {
                document.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extract sections by analyzing font sizes and text patterns
     *
     * Strategy:
     * 1. Use custom TextStripper to capture font information for each line
     * 2. Detect headings based on:
     *    - Font size (larger than body text)
     *    - Font weight (Bold in name)
     *    - Text patterns (all caps, numbered sections)
     * 3. Build section hierarchy based on font size levels
     */
    private fun extractSections(document: PDDocument, fullText: String, pages: List<Page>): List<Section> {
        val headingDetector = HeadingDetector()

        try {
            headingDetector.getText(document)
        } catch (e: Exception) {
            // If section detection fails, return empty list (graceful degradation)
            return emptyList()
        }

        val headings = headingDetector.getDetectedHeadings()
        val sections = mutableListOf<Section>()

        // Build sections from headings
        for (i in headings.indices) {
            val heading = headings[i]
            val nextHeading = headings.getOrNull(i + 1)

            // Calculate text range for this section
            val startOffset = fullText.indexOf(heading.text, heading.approximateOffset)
            if (startOffset == -1) continue // Heading not found in text

            val endOffset = if (nextHeading != null) {
                val nextStart = fullText.indexOf(nextHeading.text, nextHeading.approximateOffset)
                if (nextStart > startOffset) nextStart else fullText.length
            } else {
                fullText.length
            }

            val sectionText = fullText.substring(startOffset, endOffset)

            // Find which page this section starts on
            val pageNumber = pages.firstOrNull {
                startOffset >= it.startOffset && startOffset < it.endOffset
            }?.number

            sections.add(
                Section(
                    title = heading.text.trim(),
                    level = heading.level,
                    text = sectionText,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    pageNumber = pageNumber
                )
            )
        }

        return sections
    }

    /**
     * Detected heading info returned from HeadingDetector
     */
    data class DetectedHeading(
        val text: String,
        val level: Int,
        val approximateOffset: Int
    )

    /**
     * Custom PDFTextStripper that detects headings based on font analysis
     */
    private class HeadingDetector : PDFTextStripper() {
        private data class HeadingCandidate(
            val text: String,
            val fontSize: Float,
            val fontName: String,
            val approximateOffset: Int,
            var level: Int = 1
        )

        private val headingCandidates = mutableListOf<HeadingCandidate>()
        private val fontSizes = mutableListOf<Float>()
        private var currentOffset = 0

        init {
            sortByPosition = true
        }

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            if (text.isBlank()) {
                currentOffset += text.length
                return
            }

            // Get font info from first character (representative of line)
            val firstPos = textPositions.firstOrNull() ?: run {
                currentOffset += text.length
                return
            }

            val fontSize = firstPos.fontSizeInPt
            val fontName = firstPos.font?.name ?: ""

            // Track all font sizes for baseline detection
            fontSizes.add(fontSize)

            val trimmedText = text.trim()

            // Detect potential headings
            val isHeading = when {
                // Pattern 1: Large font size (will refine level after collecting all)
                fontSize > 12f && trimmedText.length in 5..200 -> true

                // Pattern 2: Bold font
                fontName.contains("Bold", ignoreCase = true) && trimmedText.length in 5..200 -> true

                // Pattern 3: All caps (at least 10 chars)
                trimmedText.matches(Regex("^[A-Z][A-Z ]{9,}$")) -> true

                // Pattern 4: Numbered section (1., 1.1., etc.)
                trimmedText.matches(Regex("^\\d+(\\.\\d+)*\\.\\s+.+")) -> true

                // Pattern 5: Short line with larger font (potential heading)
                trimmedText.length < 100 && fontSize > 10f -> true

                else -> false
            }

            if (isHeading) {
                headingCandidates.add(
                    HeadingCandidate(
                        text = trimmedText,
                        fontSize = fontSize,
                        fontName = fontName,
                        approximateOffset = currentOffset
                    )
                )
            }

            currentOffset += text.length
        }

        fun getDetectedHeadings(): List<DetectedHeading> {
            if (headingCandidates.isEmpty()) return emptyList()

            // Calculate body text baseline (median font size)
            val sortedSizes = fontSizes.sorted()
            val medianFontSize = if (sortedSizes.isNotEmpty()) sortedSizes[sortedSizes.size / 2] else 12f

            // Group headings by font size to determine levels
            val uniqueSizes = headingCandidates.map { it.fontSize }.distinct().sortedDescending()
            val sizeToLevel = uniqueSizes.mapIndexed { index, size ->
                size to (index + 1).coerceAtMost(3) // Max 3 levels
            }.toMap()

            // Assign levels based on font size hierarchy
            headingCandidates.forEach { candidate ->
                candidate.level = when {
                    // All caps or very large = Level 1
                    candidate.text.matches(Regex("^[A-Z][A-Z ]{9,}$")) -> 1
                    candidate.fontSize >= medianFontSize + 4 -> 1

                    // Numbered sections: count dots for level
                    candidate.text.matches(Regex("^\\d+(\\.\\d+)*\\.\\s+.+")) -> {
                        val dotCount = candidate.text.takeWhile { it == '.' || it.isDigit() || it.isWhitespace() }
                            .count { it == '.' }
                        dotCount.coerceIn(1, 3)
                    }

                    // Use font size mapping
                    else -> sizeToLevel[candidate.fontSize] ?: 2
                }
            }

            // Filter out false positives (too many headings = likely not headings)
            val totalTextLength = currentOffset
            val headingRatio = if (totalTextLength > 0) {
                headingCandidates.sumOf { it.text.length }.toFloat() / totalTextLength
            } else 0f

            val filtered = if (headingRatio > 0.3f) {
                // Too many headings - use only the largest fonts
                headingCandidates.filter { it.fontSize >= medianFontSize + 3 }
            } else {
                headingCandidates
            }

            // Convert to public DetectedHeading type
            return filtered.map { DetectedHeading(it.text, it.level, it.approximateOffset) }
        }
    }
}
