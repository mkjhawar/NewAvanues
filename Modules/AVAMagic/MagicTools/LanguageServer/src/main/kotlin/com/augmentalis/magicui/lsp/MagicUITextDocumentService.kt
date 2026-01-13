package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.TextDocumentService
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
// TODO: Replace with actual imports when modules are available
import com.augmentalis.avaui.lsp.stubs.*

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

        // Analyze context to provide smart suggestions
        val context = analyzeCompletionContext(content, position)

        when (context.type) {
            CompletionContext.COMPONENT_NAME -> {
                items.addAll(getComponentCompletions())
            }
            CompletionContext.PROPERTY_NAME -> {
                items.addAll(getPropertyCompletions(context.componentType))
            }
            CompletionContext.PROPERTY_VALUE -> {
                items.addAll(getValueCompletions(context.propertyName, context.componentType))
            }
            CompletionContext.EVENT_HANDLER -> {
                items.addAll(getEventHandlerCompletions())
            }
            else -> {
                // Provide general suggestions
                items.addAll(getComponentCompletions())
                items.addAll(getPropertyCompletions(null))
            }
        }

        return items
    }

    /**
     * Analyze context to determine what kind of completion to provide
     */
    private fun analyzeCompletionContext(content: String, position: Position): CompletionContextInfo {
        val lines = content.lines()
        if (position.line >= lines.size) {
            return CompletionContextInfo(CompletionContext.UNKNOWN)
        }

        val currentLine = lines[position.line]
        val textBeforeCursor = currentLine.substring(0, minOf(position.character, currentLine.length))

        // Check if we're in a property value context
        if (textBeforeCursor.contains(":")) {
            val propertyName = textBeforeCursor.substringBeforeLast(":").trim().split(" ").last()
            return CompletionContextInfo(
                type = CompletionContext.PROPERTY_VALUE,
                propertyName = propertyName
            )
        }

        // Check if we're in an event handler context
        if (textBeforeCursor.trim().startsWith("on")) {
            return CompletionContextInfo(CompletionContext.EVENT_HANDLER)
        }

        // Check if we're indented (likely a property)
        if (textBeforeCursor.startsWith("  ") || textBeforeCursor.startsWith("\t")) {
            return CompletionContextInfo(CompletionContext.PROPERTY_NAME)
        }

        // Default to component name
        return CompletionContextInfo(CompletionContext.COMPONENT_NAME)
    }

    /**
     * Get component completions with snippets
     */
    private fun getComponentCompletions(): List<CompletionItem> {
        val components = mapOf(
            "Button" to "vuid: \${1:button-id}\n  text: \${2:Click me}\n  onClick: \${3:handleClick}",
            "TextField" to "vuid: \${1:field-id}\n  placeholder: \${2:Enter text}\n  onChange: \${3:handleChange}",
            "Card" to "vuid: \${1:card-id}\n  children:\n    - \${2}",
            "Text" to "vuid: \${1:text-id}\n  text: \${2:Hello World}",
            "Image" to "vuid: \${1:image-id}\n  src: \${2:image.png}",
            "Column" to "vuid: \${1:column-id}\n  children:\n    - \${2}",
            "Row" to "vuid: \${1:row-id}\n  children:\n    - \${2}",
            "Container" to "vuid: \${1:container-id}\n  children:\n    - \${2}",
            "Checkbox" to "vuid: \${1:checkbox-id}\n  checked: \${2:false}\n  onChange: \${3:handleCheck}",
            "Switch" to "vuid: \${1:switch-id}\n  enabled: \${2:true}\n  onChange: \${3:handleSwitch}"
        )

        return components.map { (name, snippet) ->
            CompletionItem(name).apply {
                kind = CompletionItemKind.Class
                detail = "MagicUI $name Component"
                documentation = Either.forLeft(getComponentDocumentation(name))
                insertText = "$name:\n  $snippet"
                insertTextFormat = InsertTextFormat.Snippet
                sortText = "0$name" // Sort components first
            }
        }
    }

    /**
     * Get property completions based on component type
     */
    private fun getPropertyCompletions(componentType: String?): List<CompletionItem> {
        val commonProps = listOf(
            "vuid" to "Unique voice identifier for navigation",
            "visible" to "Component visibility (true/false)",
            "enabled" to "Component enabled state (true/false)",
            "style" to "Custom styling object",
            "modifiers" to "Layout modifiers"
        )

        val typeSpecificProps = when (componentType?.lowercase()) {
            "button", "textfield", "checkbox", "switch" -> listOf(
                "onClick" to "Click event handler",
                "onChange" to "Change event handler"
            )
            "text" -> listOf(
                "text" to "Text content",
                "fontSize" to "Font size (dp, sp)",
                "color" to "Text color (hex or named)"
            )
            "image" -> listOf(
                "src" to "Image source URL",
                "alt" to "Alternative text",
                "width" to "Image width",
                "height" to "Image height"
            )
            else -> emptyList()
        }

        return (commonProps + typeSpecificProps).map { (name, description) ->
            CompletionItem(name).apply {
                kind = CompletionItemKind.Property
                detail = "Component Property"
                documentation = Either.forLeft(description)
                insertText = "$name: "
                sortText = "1$name" // Sort properties after components
            }
        }
    }

    /**
     * Get value completions for specific properties
     */
    private fun getValueCompletions(propertyName: String?, componentType: String?): List<CompletionItem> {
        return when (propertyName?.lowercase()) {
            "color", "backgroundcolor", "bordercolor" -> getColorCompletions()
            "alignment", "gravity" -> getAlignmentCompletions()
            "visible", "enabled", "checked" -> getBooleanCompletions()
            else -> emptyList()
        }
    }

    /**
     * Get color value completions
     */
    private fun getColorCompletions(): List<CompletionItem> {
        val colors = mapOf(
            "red" to "#FF0000",
            "blue" to "#0000FF",
            "green" to "#00FF00",
            "black" to "#000000",
            "white" to "#FFFFFF",
            "gray" to "#808080",
            "yellow" to "#FFFF00",
            "orange" to "#FFA500",
            "purple" to "#800080"
        )

        return colors.map { (name, hex) ->
            CompletionItem(name).apply {
                kind = CompletionItemKind.Color
                detail = hex
                documentation = Either.forLeft("Color: $name ($hex)")
                insertText = name
            }
        }
    }

    /**
     * Get alignment value completions
     */
    private fun getAlignmentCompletions(): List<CompletionItem> {
        val alignments = listOf("start", "center", "end", "top", "bottom", "left", "right")

        return alignments.map { alignment ->
            CompletionItem(alignment).apply {
                kind = CompletionItemKind.Enum
                detail = "Alignment value"
                insertText = alignment
            }
        }
    }

    /**
     * Get boolean value completions
     */
    private fun getBooleanCompletions(): List<CompletionItem> {
        return listOf("true", "false").map { value ->
            CompletionItem(value).apply {
                kind = CompletionItemKind.Value
                detail = "Boolean value"
                insertText = value
            }
        }
    }

    /**
     * Get event handler completions
     */
    private fun getEventHandlerCompletions(): List<CompletionItem> {
        val handlers = mapOf(
            "onClick" to "Click event handler",
            "onChange" to "Change event handler",
            "onSubmit" to "Submit event handler",
            "onFocus" to "Focus event handler",
            "onBlur" to "Blur event handler"
        )

        return handlers.map { (name, description) ->
            CompletionItem(name).apply {
                kind = CompletionItemKind.Event
                detail = description
                insertText = "$name: \${1:handleEvent}"
                insertTextFormat = InsertTextFormat.Snippet
            }
        }
    }

    /**
     * Get component documentation
     */
    private fun getComponentDocumentation(componentName: String): String {
        return when (componentName) {
            "Button" -> "Interactive button component with click handling"
            "TextField" -> "Text input field with validation and change handling"
            "Card" -> "Container with elevation and rounded corners"
            "Text" -> "Display text with customizable styling"
            "Image" -> "Display images from URLs or assets"
            "Column" -> "Vertical layout container"
            "Row" -> "Horizontal layout container"
            "Container" -> "Generic container for grouping components"
            "Checkbox" -> "Checkbox input for boolean values"
            "Switch" -> "Toggle switch for boolean values"
            else -> "MagicUI component"
        }
    }

    /**
     * Completion context types
     */
    private enum class CompletionContext {
        COMPONENT_NAME, PROPERTY_NAME, PROPERTY_VALUE, EVENT_HANDLER, UNKNOWN
    }

    /**
     * Completion context information
     */
    private data class CompletionContextInfo(
        val type: CompletionContext,
        val componentType: String? = null,
        val propertyName: String? = null
    }

    /**
     * Get hover information for symbol at position
     */
    private fun getHoverInfo(content: String, position: Position): Hover {
        val lines = content.lines()
        if (position.line >= lines.size) {
            return createEmptyHover()
        }

        val currentLine = lines[position.line]

        // Try to identify what we're hovering over
        val hoverContext = analyzeHoverContext(currentLine, position.character)

        val documentation = when {
            hoverContext.componentName != null -> {
                getComponentHoverDocumentation(hoverContext.componentName)
            }
            hoverContext.propertyName != null -> {
                getPropertyHoverDocumentation(hoverContext.propertyName, hoverContext.propertyValue)
            }
            hoverContext.vuid != null -> {
                getVuidHoverDocumentation(hoverContext.vuid)
            }
            else -> null
        }

        return if (documentation != null) {
            val markedString = MarkupContent().apply {
                kind = "markdown"
                value = documentation
            }
            Hover(markedString)
        } else {
            createEmptyHover()
        }
    }

    /**
     * Analyze hover context to determine what user is hovering over
     */
    private fun analyzeHoverContext(line: String, character: Int): HoverContextInfo {
        // Extract word at cursor position
        val beforeCursor = line.substring(0, minOf(character, line.length))
        val afterCursor = line.substring(minOf(character, line.length))

        val wordBefore = beforeCursor.split(Regex("[\\s:,]")).lastOrNull() ?: ""
        val wordAfter = afterCursor.split(Regex("[\\s:,]")).firstOrNull() ?: ""
        val word = wordBefore + wordAfter

        // Check if it's a component name (starts with uppercase)
        if (word.isNotEmpty() && word[0].isUpperCase()) {
            return HoverContextInfo(componentName = word)
        }

        // Check if it's a property (contains colon)
        if (line.contains(":")) {
            val parts = line.split(":")
            val propName = parts[0].trim()
            val propValue = if (parts.size > 1) parts[1].trim() else null
            return HoverContextInfo(propertyName = propName, propertyValue = propValue)
        }

        // Check if it's a VUID
        if (word.startsWith("VUID-") || word.contains("vuid")) {
            return HoverContextInfo(vuid = word)
        }

        return HoverContextInfo()
    }

    /**
     * Get hover documentation for component
     */
    private fun getComponentHoverDocumentation(componentName: String): String {
        val baseDoc = when (componentName) {
            "Button" -> """
                ### Button Component
                Interactive button for user actions

                **Properties:**
                - `text`: Button label text
                - `icon`: Optional icon
                - `onClick`: Click event handler
                - `enabled`: Enable/disable state
                - `vuid`: Voice unique identifier

                **Example:**
                ```yaml
                Button:
                  vuid: submit-btn
                  text: Submit Form
                  onClick: handleSubmit
                ```
            """.trimIndent()

            "TextField" -> """
                ### TextField Component
                Text input field with validation

                **Properties:**
                - `placeholder`: Placeholder text
                - `value`: Current value
                - `onChange`: Change event handler
                - `vuid`: Voice unique identifier
                - `validation`: Validation rules

                **Example:**
                ```yaml
                TextField:
                  vuid: email-input
                  placeholder: Enter email
                  onChange: handleEmailChange
                ```
            """.trimIndent()

            "Card" -> """
                ### Card Component
                Container with elevation and rounded corners

                **Properties:**
                - `elevation`: Shadow depth
                - `children`: Child components
                - `backgroundColor`: Card background color
                - `padding`: Inner padding

                **Example:**
                ```yaml
                Card:
                  vuid: profile-card
                  elevation: 4
                  children:
                    - Text: ...
                ```
            """.trimIndent()

            else -> """
                ### $componentName Component
                MagicUI component for building user interfaces

                **Common Properties:**
                - `vuid`: Voice unique identifier
                - `visible`: Visibility state
                - `enabled`: Enabled state
            """.trimIndent()
        }

        return baseDoc
    }

    /**
     * Get hover documentation for property
     */
    private fun getPropertyHoverDocumentation(propertyName: String, propertyValue: String?): String {
        return when (propertyName) {
            "vuid" -> """
                ### VUID (Voice Unique Identifier)
                Unique identifier for voice navigation and component access

                **Format:** `component-type-descriptor`
                **Example:** `login-submit-button`

                Used by VoiceOS to navigate and interact with UI elements.
            """.trimIndent()

            "onClick", "onChange", "onSubmit" -> """
                ### Event Handler: $propertyName
                Callback function triggered on ${propertyName.substring(2)} event

                **Expected Format:** Function reference or inline handler
                **Example:** `$propertyName: handleUserAction`
            """.trimIndent()

            "color", "backgroundColor", "borderColor" -> """
                ### Color Property: $propertyName
                ${if (propertyValue != null) "**Current:** `$propertyValue`\n\n" else ""}
                **Accepted Formats:**
                - Hex: `#RGB`, `#RRGGBB`, `#AARRGGBB`
                - Named: red, blue, green, black, white, gray, yellow, orange, purple

                **Examples:** `#FF0000`, `red`, `#80FF0000`
            """.trimIndent()

            "width", "height", "padding", "margin" -> """
                ### Size Property: $propertyName
                ${if (propertyValue != null) "**Current:** `$propertyValue`\n\n" else ""}
                **Accepted Units:**
                - `dp` - Density-independent pixels (recommended)
                - `sp` - Scale-independent pixels (for text)
                - `px` - Physical pixels
                - `%` - Percentage of parent

                **Examples:** `16dp`, `14sp`, `100px`, `50%`
            """.trimIndent()

            else -> """
                ### Property: $propertyName
                ${if (propertyValue != null) "**Value:** `$propertyValue`" else "Component property"}
            """.trimIndent()
        }
    }

    /**
     * Get hover documentation for VUID
     */
    private fun getVuidHoverDocumentation(vuid: String): String {
        return """
            ### VUID: `$vuid`
            Voice Unique Identifier for component navigation

            **Usage:**
            - Voice commands: "Click $vuid", "Focus $vuid"
            - Programmatic access: `findByVuid("$vuid")`
            - Analytics tracking

            **Navigation:** Use Go-to-Definition (F12) to navigate to component
        """.trimIndent()
    }

    /**
     * Create empty hover when no documentation available
     */
    private fun createEmptyHover(): Hover {
        return Hover(MarkupContent().apply {
            kind = "markdown"
            value = ""
        })
    }

    /**
     * Hover context information
     */
    private data class HoverContextInfo(
        val componentName: String? = null,
        val propertyName: String? = null,
        val propertyValue: String? = null,
        val vuid: String? = null
    )

    /**
     * Get definition locations for symbol at position
     */
    private fun getDefinitionLocations(content: String, position: Position, uri: String): List<Location> {
        val lines = content.lines()
        if (position.line >= lines.size) {
            return emptyList()
        }

        val currentLine = lines[position.line]

        // Try to extract VUID at cursor position
        val vuid = extractVuidAtPosition(currentLine, position.character)

        if (vuid != null && isValidVuidFormat(vuid)) {
            // Search for VUID definition in document
            val definitionLocation = findVuidDefinition(content, vuid, uri)
            if (definitionLocation != null) {
                return listOf(definitionLocation)
            }
        }

        return emptyList()
    }

    /**
     * Extract VUID at cursor position
     */
    private fun extractVuidAtPosition(line: String, character: Int): String? {
        // Extract word at cursor
        val beforeCursor = line.substring(0, minOf(character, line.length))
        val afterCursor = line.substring(minOf(character, line.length))

        val wordBefore = beforeCursor.split(Regex("[\\s:,\"']")).lastOrNull() ?: ""
        val wordAfter = afterCursor.split(Regex("[\\s:,\"']")).firstOrNull() ?: ""
        val word = wordBefore + wordAfter

        // Check if it looks like a VUID
        return if (word.matches(Regex("^[a-z0-9-]+$")) && word.contains("-")) {
            word
        } else {
            null
        }
    }

    /**
     * Validate VUID format
     */
    private fun isValidVuidFormat(vuid: String): Boolean {
        // VUID format: lowercase letters, numbers, hyphens
        // Example: button-submit, login-email-field
        return vuid.matches(Regex("^[a-z][a-z0-9-]*[a-z0-9]$")) &&
               vuid.length >= 3 &&
               vuid.length <= 64
    }

    /**
     * Find VUID definition in document
     */
    private fun findVuidDefinition(content: String, vuid: String, uri: String): Location? {
        val lines = content.lines()

        // Search for vuid: <target> pattern
        lines.forEachIndexed { index, line ->
            if (line.contains("vuid:") && line.contains(vuid)) {
                // Found definition
                val range = Range(
                    Position(index, 0),
                    Position(index, line.length)
                )
                return Location(uri, range)
            }
        }

        return null
    }

    /**
     * Generate VUID suggestion based on component type and context
     */
    private fun generateVuidSuggestion(componentType: String?, context: String?): String {
        val prefix = componentType?.lowercase()?.replace(Regex("[^a-z0-9]"), "-") ?: "component"
        val suffix = context?.lowercase()?.replace(Regex("[^a-z0-9]"), "-") ?: "item"
        val random = (1000..9999).random()

        return "$prefix-$suffix-$random"
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
