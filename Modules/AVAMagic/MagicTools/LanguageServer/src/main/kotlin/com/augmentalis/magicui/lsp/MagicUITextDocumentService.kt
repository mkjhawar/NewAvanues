package com.augmentalis.magicui.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.TextDocumentService
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import com.augmentalis.magiccode.generator.parser.VosParser
import com.augmentalis.magiccode.generator.parser.JsonDSLParser
import com.augmentalis.magiccode.generator.parser.CompactSyntaxParser

/**
 * Text Document Service for MagicUI Language Server
 *
 * Handles all document-related LSP features:
 * - Completion (autocomplete)
 * - Hover (documentation)
 * - Definition (go-to-definition)
 * - Diagnostics (error checking)
 * - Formatting
 */
class MagicUITextDocumentService : TextDocumentService {

    private val logger = LoggerFactory.getLogger(MagicUITextDocumentService::class.java)

    private var client: LanguageClient? = null

    // Document cache: URI -> content
    private val documents = ConcurrentHashMap<String, String>()

    // Parsers
    private val vosParser = VosParser()
    private val jsonDslParser = JsonDSLParser()
    private val compactSyntaxParser = CompactSyntaxParser()

    fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val uri = params.textDocument.uri
        val content = params.textDocument.text

        logger.info("Document opened: $uri")
        documents[uri] = content

        // Run diagnostics
        publishDiagnostics(uri, content)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val uri = params.textDocument.uri
        val changes = params.contentChanges

        // Apply incremental changes
        val currentContent = documents[uri] ?: ""
        val newContent = applyChanges(currentContent, changes)

        documents[uri] = newContent
        logger.debug("Document changed: $uri")

        // Run diagnostics
        publishDiagnostics(uri, newContent)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val uri = params.textDocument.uri
        documents.remove(uri)
        logger.info("Document closed: $uri")
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        val uri = params.textDocument.uri
        logger.info("Document saved: $uri")
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>> {
        val uri = params.textDocument.uri
        val position = params.position
        val content = documents[uri] ?: ""

        logger.debug("Completion requested at $uri:${position.line}:${position.character}")

        return CompletableFuture.supplyAsync {
            val completions = getCompletionItems(content, position)
            Either.forRight(CompletionList(false, completions))
        }
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover> {
        val uri = params.textDocument.uri
        val position = params.position
        val content = documents[uri] ?: ""

        logger.debug("Hover requested at $uri:${position.line}:${position.character}")

        return CompletableFuture.supplyAsync {
            getHoverInfo(content, position)
        }
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        val uri = params.textDocument.uri
        val position = params.position
        val content = documents[uri] ?: ""

        logger.debug("Definition requested at $uri:${position.line}:${position.character}")

        return CompletableFuture.supplyAsync {
            val locations = getDefinitionLocations(content, position, uri)
            Either.forLeft(locations)
        }
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<TextEdit>> {
        val uri = params.textDocument.uri
        val content = documents[uri] ?: ""

        logger.debug("Formatting requested for $uri")

        return CompletableFuture.supplyAsync {
            formatDocument(content)
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Apply incremental text changes to document
     */
    private fun applyChanges(content: String, changes: List<TextDocumentContentChangeEvent>): String {
        var result = content

        for (change in changes) {
            result = if (change.range == null) {
                // Full document change
                change.text
            } else {
                // Incremental change
                val range = change.range
                val lines = result.lines().toMutableList()

                // Calculate offset
                val startOffset = lines.take(range.start.line).sumOf { it.length + 1 } + range.start.character
                val endOffset = lines.take(range.end.line).sumOf { it.length + 1 } + range.end.character

                result.substring(0, startOffset) + change.text + result.substring(endOffset)
            }
        }

        return result
    }

    /**
     * Publish diagnostics (errors/warnings) to client
     */
    private fun publishDiagnostics(uri: String, content: String) {
        val diagnostics = validateDocument(uri, content)

        val params = PublishDiagnosticsParams().apply {
            this.uri = uri
            this.diagnostics = diagnostics
        }

        client?.publishDiagnostics(params)
    }

    /**
     * Validate document and return diagnostics using appropriate parser
     */
    private fun validateDocument(uri: String, content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()

        if (content.trim().isEmpty()) {
            return diagnostics
        }

        // Determine file type from URI
        val fileType = determineFileType(uri, content)

        // Use appropriate parser for validation
        val validationResult = when (fileType) {
            FileType.JSON -> jsonDslParser.validate(content)
            FileType.YAML -> vosParser.validate(content)
            FileType.COMPACT -> compactSyntaxParser.validate(content)
            FileType.UNKNOWN -> {
                // Try to infer from content
                when {
                    content.trim().startsWith("{") -> jsonDslParser.validate(content)
                    content.trim().startsWith("Magic") -> compactSyntaxParser.validate(content)
                    else -> vosParser.validate(content)
                }
            }
        }

        // Convert validation errors to LSP diagnostics
        validationResult.errors.forEach { error ->
            diagnostics.add(
                Diagnostic().apply {
                    range = Range(
                        Position(error.line, 0),
                        Position(error.line, 100) // End of line approximation
                    )
                    severity = DiagnosticSeverity.Error
                    message = error.message
                    source = "magicui-${fileType.name.lowercase()}"
                }
            )
        }

        // Convert validation warnings to LSP diagnostics
        validationResult.warnings.forEach { warning ->
            diagnostics.add(
                Diagnostic().apply {
                    range = Range(
                        Position(warning.line, 0),
                        Position(warning.line, 100)
                    )
                    severity = DiagnosticSeverity.Warning
                    message = warning.message
                    source = "magicui-${fileType.name.lowercase()}"
                }
            )
        }

        logger.debug("Validated document: ${diagnostics.size} diagnostics (${validationResult.errors.size} errors, ${validationResult.warnings.size} warnings)")

        return diagnostics
    }

    /**
     * Determine file type from URI and content
     */
    private fun determineFileType(uri: String, content: String): FileType {
        return when {
            uri.endsWith(".magic.json") || uri.endsWith(".json") -> FileType.JSON
            uri.endsWith(".magic.yaml") || uri.endsWith(".yaml") || uri.endsWith(".yml") -> FileType.YAML
            uri.endsWith(".magicui") || uri.endsWith(".ucd") -> FileType.COMPACT
            content.trim().startsWith("{") -> FileType.JSON
            content.trim().startsWith("Magic") || content.trim().startsWith("Ava") -> FileType.COMPACT
            else -> FileType.YAML // Default to YAML
        }
    }

    /**
     * File type enum
     */
    private enum class FileType {
        JSON, YAML, COMPACT, UNKNOWN
    }

    /**
     * Get completion items for current position
     */
    private fun getCompletionItems(content: String, position: Position): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()

        // TODO: Implement smart completion based on context
        // For now, provide basic component suggestions

        // Component suggestions
        val components = listOf(
            "Button", "TextField", "Card", "Text", "Image",
            "Column", "Row", "Container", "Divider", "Checkbox"
        )

        components.forEach { component ->
            items.add(
                CompletionItem(component).apply {
                    kind = CompletionItemKind.Class
                    detail = "MagicUI Component"
                    documentation = Either.forLeft("A $component component")
                    insertText = "$component:\n  "
                    insertTextFormat = InsertTextFormat.Snippet
                }
            )
        }

        // Property suggestions (when inside a component)
        val properties = listOf(
            "text", "onClick", "enabled", "visible", "vuid",
            "modifiers", "style", "theme"
        )

        properties.forEach { property ->
            items.add(
                CompletionItem(property).apply {
                    kind = CompletionItemKind.Property
                    detail = "Component Property"
                    insertText = "$property: "
                }
            )
        }

        return items
    }

    /**
     * Get hover information for symbol at position
     */
    private fun getHoverInfo(content: String, position: Position): Hover {
        // TODO: Implement hover documentation lookup
        // For now, return basic info

        val markedString = MarkupContent().apply {
            kind = "markdown"
            value = "**MagicUI Component**\n\nHover documentation coming soon..."
        }

        return Hover(markedString)
    }

    /**
     * Get definition locations for symbol at position
     */
    private fun getDefinitionLocations(content: String, position: Position, uri: String): List<Location> {
        // TODO: Implement go-to-definition (e.g., navigate to VUID definition)
        // For now, return empty list

        return emptyList()
    }

    /**
     * Format document
     */
    private fun formatDocument(content: String): List<TextEdit> {
        // TODO: Implement formatting (YAML/JSON formatting)
        // For now, return empty list (no changes)

        return emptyList()
    }
}
