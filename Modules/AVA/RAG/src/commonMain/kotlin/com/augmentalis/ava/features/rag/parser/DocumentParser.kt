// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/parser/DocumentParser.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.parser

import com.augmentalis.ava.features.rag.domain.DocumentType

/**
 * Interface for parsing documents into text
 *
 * Each document type (PDF, DOCX, etc.) has its own parser implementation.
 * Parsers extract text while preserving structure (headings, sections, pages).
 */
interface DocumentParser {
    /**
     * Document types this parser supports
     */
    val supportedTypes: Set<DocumentType>

    /**
     * Parse a document file into structured text
     *
     * @param filePath Path to the document
     * @param documentType Type of document
     * @return Parsed document with structure
     */
    suspend fun parse(filePath: String, documentType: DocumentType): Result<ParsedDocument>
}

/**
 * Result of parsing a document
 *
 * Contains the extracted text organized by structure (pages, sections, etc.)
 */
data class ParsedDocument(
    val text: String,
    val pages: List<Page> = emptyList(),
    val sections: List<Section> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val totalPages: Int = pages.size,
    val wordCount: Int = text.split("\\s+".toRegex()).size
)

/**
 * A page in the document
 */
data class Page(
    val number: Int,
    val text: String,
    val startOffset: Int,
    val endOffset: Int
)

/**
 * A section in the document (chapter, heading, etc.)
 */
data class Section(
    val title: String,
    val level: Int,  // 1 = top level, 2 = subsection, etc.
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val pageNumber: Int? = null
)

/**
 * Factory for creating document parsers
 */
expect object DocumentParserFactory {
    /**
     * Get a parser for the specified document type
     *
     * @param documentType Type of document to parse
     * @return Parser instance or null if not supported
     */
    fun getParser(documentType: DocumentType): DocumentParser?

    /**
     * Get all available parsers
     */
    fun getAllParsers(): List<DocumentParser>

    /**
     * Check if a document type is supported
     */
    fun isSupported(documentType: DocumentType): Boolean
}
