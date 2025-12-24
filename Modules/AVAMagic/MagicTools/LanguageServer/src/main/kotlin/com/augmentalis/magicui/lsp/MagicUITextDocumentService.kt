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
import com.augmentalis.magiccode.generator.ast.ComponentNode
import com.augmentalis.magiccode.generator.ast.ScreenNode
import com.augmentalis.magiccode.generator.ast.ComponentType
import com.augmentalis.magiccode.generator.ast.AvaUINode

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

        // Add semantic validation on top of parser validation
        addSemanticValidation(diagnostics, content, uri)

        logger.debug("Total diagnostics after semantic validation: ${diagnostics.size}")

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
     * Add semantic validation rules on top of parser validation
     */
    private fun addSemanticValidation(diagnostics: MutableList<Diagnostic>, content: String, uri: String) {
        try {
            val fileType = determineFileType(uri, content)

            // Parse content to AST for semantic analysis
            val parseResult: Any? = when (fileType) {
                FileType.JSON -> {
                    val result = jsonDslParser.parseComponent(content)
                    if (result.isSuccess) result.getOrNull() else null
                }
                FileType.YAML -> {
                    val result = vosParser.parseComponent(content)
                    if (result.isSuccess) result.getOrNull() else null
                }
                FileType.COMPACT -> {
                    val result = compactSyntaxParser.parseComponent(content)
                    if (result.isSuccess) result.getOrNull() else null
                }
                FileType.UNKNOWN -> return
            }

            if (parseResult == null) return

            // Extract root component for validation
            val rootComponent: ComponentNode? = when (parseResult) {
                is ScreenNode -> parseResult.root
                is ComponentNode -> parseResult
                else -> null
            }

            if (rootComponent == null) return

            // Perform semantic validation
            validateComponentTree(rootComponent, diagnostics, 0)

        } catch (e: Exception) {
            logger.debug("Semantic validation skipped: ${e.message}")
        }
    }

    /**
     * Recursively validate component tree
     */
    private fun validateComponentTree(component: ComponentNode, diagnostics: MutableList<Diagnostic>, depth: Int) {
        // Component-specific validation
        validateComponentRules(component, diagnostics)

        // Property value validation
        validatePropertyValues(component, diagnostics)

        // Required field validation
        validateRequiredFields(component, diagnostics)

        // Hierarchy depth validation
        if (depth > 10) {
            diagnostics.add(createWarning(0, "Component nesting depth exceeds 10 levels, consider refactoring"))
        }

        // Validate children recursively
        component.children.forEach { child ->
            if (child is ComponentNode) {
                validateComponentTree(child, diagnostics, depth + 1)
                validateParentChildRelationship(component, child, diagnostics)
            }
        }
    }

    /**
     * Validate component-specific rules
     */
    private fun validateComponentRules(component: ComponentNode, diagnostics: MutableList<Diagnostic>) {
        when (component.type) {
            ComponentType.BUTTON -> {
                if (!component.properties.containsKey("text") && !component.properties.containsKey("icon")) {
                    diagnostics.add(createWarning(0, "Button should have 'text' or 'icon' property"))
                }
            }
            ComponentType.TEXT_FIELD -> {
                if (!component.properties.containsKey("vuid") && !component.properties.containsKey("id")) {
                    diagnostics.add(createWarning(0, "TextField should have 'vuid' for data binding"))
                }
            }
            ComponentType.IMAGE -> {
                if (!component.properties.containsKey("src") && !component.properties.containsKey("icon")) {
                    diagnostics.add(createError(0, "Image must have 'src' or 'icon' property"))
                }
            }
            ComponentType.CONTAINER, ComponentType.ROW, ComponentType.COLUMN -> {
                if (component.children.isEmpty()) {
                    diagnostics.add(createWarning(0, "${component.type} should contain child components"))
                }
            }
            else -> { /* No specific rules for this component type */ }
        }
    }

    /**
     * Validate property values
     */
    private fun validatePropertyValues(component: ComponentNode, diagnostics: MutableList<Diagnostic>) {
        component.properties.forEach { (key, value) ->
            when (key) {
                "color", "backgroundColor", "borderColor" -> {
                    if (!isValidColor(value.toString())) {
                        diagnostics.add(createWarning(0, "Invalid color value: $value"))
                    }
                }
                "width", "height", "padding", "margin" -> {
                    if (!isValidSize(value.toString())) {
                        diagnostics.add(createWarning(0, "Invalid size value: $value (use dp, sp, px, or %)"))
                    }
                }
                "alignment", "gravity" -> {
                    val validAlignments = setOf("start", "center", "end", "top", "bottom", "left", "right")
                    if (value.toString().lowercase() !in validAlignments) {
                        diagnostics.add(createWarning(0, "Invalid alignment value: $value"))
                    }
                }
            }
        }
    }

    /**
     * Validate required fields
     */
    private fun validateRequiredFields(component: ComponentNode, diagnostics: MutableList<Diagnostic>) {
        // All components should have vuid for voice navigation
        if (!component.properties.containsKey("vuid") && !component.properties.containsKey("id")) {
            diagnostics.add(createInfo(0, "Consider adding 'vuid' property for voice navigation"))
        }

        // Interactive components should have event handlers
        val interactiveTypes = setOf(
            ComponentType.BUTTON, ComponentType.CHECKBOX, ComponentType.SWITCH,
            ComponentType.TEXT_FIELD, ComponentType.SLIDER, ComponentType.RADIO
        )
        if (component.type in interactiveTypes && component.eventHandlers.isEmpty()) {
            diagnostics.add(createInfo(0, "Interactive component should have event handler (onClick, onChange, etc.)"))
        }
    }

    /**
     * Validate parent-child relationships
     */
    private fun validateParentChildRelationship(parent: ComponentNode, child: ComponentNode, diagnostics: MutableList<Diagnostic>) {
        // ScrollView should not contain another ScrollView
        if (parent.type == ComponentType.SCROLL_VIEW && child.type == ComponentType.SCROLL_VIEW) {
            diagnostics.add(createWarning(0, "ScrollView should not contain another ScrollView"))
        }

        // Card nesting depth check
        if (parent.type == ComponentType.CARD && child.type == ComponentType.CARD) {
            diagnostics.add(createInfo(0, "Consider avoiding direct Card nesting for better UX"))
        }
    }

    /**
     * Helper: Validate color value
     */
    private fun isValidColor(value: String): Boolean {
        // Hex colors: #RGB, #RRGGBB, #AARRGGBB
        if (value.matches(Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))) return true

        // Named colors
        val namedColors = setOf("red", "blue", "green", "black", "white", "gray", "yellow", "orange", "purple")
        if (value.lowercase() in namedColors) return true

        return false
    }

    /**
     * Helper: Validate size value
     */
    private fun isValidSize(value: String): Boolean {
        // Numeric values with units: 16dp, 12sp, 100px, 50%
        return value.matches(Regex("^\\d+(\\.\\d+)?(dp|sp|px|%)$"))
    }

    /**
     * Helper: Create error diagnostic
     */
    private fun createError(line: Int, message: String): Diagnostic {
        return Diagnostic().apply {
            range = Range(Position(line, 0), Position(line, 100))
            severity = DiagnosticSeverity.Error
            this.message = message
            source = "magicui-semantic"
        }
    }

    /**
     * Helper: Create warning diagnostic
     */
    private fun createWarning(line: Int, message: String): Diagnostic {
        return Diagnostic().apply {
            range = Range(Position(line, 0), Position(line, 100))
            severity = DiagnosticSeverity.Warning
            this.message = message
            source = "magicui-semantic"
        }
    }

    /**
     * Helper: Create info diagnostic
     */
    private fun createInfo(line: Int, message: String): Diagnostic {
        return Diagnostic().apply {
            range = Range(Position(line, 0), Position(line, 100))
            severity = DiagnosticSeverity.Information
            this.message = message
            source = "magicui-semantic"
        }
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
