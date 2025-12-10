// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/parser/DocumentParserFactory.android.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.parser

import android.content.Context
import com.augmentalis.ava.features.rag.domain.DocumentType

/**
 * Android implementation of DocumentParserFactory
 */
actual object DocumentParserFactory {
    private var context: Context? = null
    private val parsers = mutableMapOf<DocumentType, DocumentParser>()

    /**
     * Initialize the factory with Android context
     *
     * Must be called before using any parsers
     */
    fun initialize(context: Context) {
        this.context = context
        // Register parsers
        parsers[DocumentType.PDF] = PdfParser(context)
        parsers[DocumentType.TXT] = TxtParser(context)
        parsers[DocumentType.HTML] = HtmlParser(context)
        parsers[DocumentType.RTF] = RtfParser(context)
        parsers[DocumentType.MD] = MarkdownParser(context)
        parsers[DocumentType.DOCX] = DocxParser(context)
        // TODO: Add EPUB parser
    }

    actual fun getParser(documentType: DocumentType): DocumentParser? {
        check(context != null) { "DocumentParserFactory not initialized. Call initialize(context) first." }
        return parsers[documentType]
    }

    actual fun getAllParsers(): List<DocumentParser> {
        check(context != null) { "DocumentParserFactory not initialized. Call initialize(context) first." }
        return parsers.values.toList()
    }

    actual fun isSupported(documentType: DocumentType): Boolean {
        if (context == null) return false
        return parsers.containsKey(documentType)
    }
}
