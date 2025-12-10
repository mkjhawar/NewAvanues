// filename: Universal/AVA/Features/RAG/src/iosMain/kotlin/com/augmentalis/ava/features/rag/parser/DocumentParserFactory.ios.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.parser

import com.augmentalis.ava.features.rag.domain.DocumentType

/**
 * iOS implementation of DocumentParserFactory
 *
 * TODO Phase 2: Implement actual parsers
 */
actual object DocumentParserFactory {
    actual fun getParser(documentType: DocumentType): DocumentParser? {
        // TODO: Implement parsers in Phase 2
        return null
    }

    actual fun getAllParsers(): List<DocumentParser> {
        // TODO: Implement parsers in Phase 2
        return emptyList()
    }

    actual fun isSupported(documentType: DocumentType): Boolean {
        // TODO: Implement parsers in Phase 2
        return false
    }
}
