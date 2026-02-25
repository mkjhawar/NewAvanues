package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages document lifecycle and caching for the LSP server.
 * Single responsibility: track open documents and apply incremental changes.
 */
class DocumentManager {

    private val logger = LoggerFactory.getLogger(DocumentManager::class.java)

    private val documents = ConcurrentHashMap<String, String>()

    fun getContent(uri: String): String? = documents[uri]

    fun open(uri: String, content: String) {
        documents[uri] = content
        logger.info("Document opened: $uri")
    }

    fun change(uri: String, changes: List<TextDocumentContentChangeEvent>): String? {
        val current = documents[uri] ?: return null
        val updated = applyChanges(current, changes)
        documents[uri] = updated
        logger.debug("Document changed: $uri")
        return updated
    }

    fun close(uri: String) {
        documents.remove(uri)
        logger.info("Document closed: $uri")
    }

    private fun applyChanges(content: String, changes: List<TextDocumentContentChangeEvent>): String {
        var result = content
        for (change in changes) {
            result = if (change.range == null) {
                change.text
            } else {
                val range = change.range
                val lines = result.lines().toMutableList()
                val startOffset = lines.take(range.start.line).sumOf { it.length + 1 } + range.start.character
                val endOffset = lines.take(range.end.line).sumOf { it.length + 1 } + range.end.character
                result.substring(0, startOffset) + change.text + result.substring(endOffset)
            }
        }
        return result
    }
}
