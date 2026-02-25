package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.slf4j.LoggerFactory
import com.augmentalis.avaui.lsp.stubs.*

/**
 * Provides validation and diagnostics for AvanueUI DSL documents.
 * Single responsibility: validate documents and publish diagnostics to the client.
 */
class DiagnosticsProvider(
    private val vosParser: VosParser,
    private val jsonDslParser: JsonDSLParser,
    private val compactSyntaxParser: CompactSyntaxParser
) {

    private val logger = LoggerFactory.getLogger(DiagnosticsProvider::class.java)

    fun publishDiagnostics(uri: String, content: String, client: LanguageClient?) {
        val diagnostics = validate(uri, content)
        val params = PublishDiagnosticsParams().apply {
            this.uri = uri
            this.diagnostics = diagnostics
        }
        client?.publishDiagnostics(params)
    }

    fun validate(uri: String, content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        if (content.trim().isEmpty()) return diagnostics

        val fileType = FileTypeDetector.detect(uri, content)
        val validationResult = when (fileType) {
            FileType.JSON -> jsonDslParser.validate(content)
            FileType.YAML -> vosParser.validate(content)
            FileType.COMPACT -> compactSyntaxParser.validate(content)
            FileType.UNKNOWN -> when {
                content.trim().startsWith("{") -> jsonDslParser.validate(content)
                content.trim().startsWith("Ava") -> compactSyntaxParser.validate(content)
                else -> vosParser.validate(content)
            }
        }

        validationResult.errors.forEach { error ->
            diagnostics.add(Diagnostic().apply {
                range = Range(Position(error.line, 0), Position(error.line, 100))
                severity = DiagnosticSeverity.Error
                message = error.message
                source = "avanueui-${fileType.name.lowercase()}"
            })
        }

        validationResult.warnings.forEach { warning ->
            diagnostics.add(Diagnostic().apply {
                range = Range(Position(warning.line, 0), Position(warning.line, 100))
                severity = DiagnosticSeverity.Warning
                message = warning.message
                source = "avanueui-${fileType.name.lowercase()}"
            })
        }

        logger.debug("Validated: ${diagnostics.size} diagnostics (${validationResult.errors.size} errors, ${validationResult.warnings.size} warnings)")

        addSemanticValidation(diagnostics, content, uri)
        return diagnostics
    }

    private fun addSemanticValidation(diagnostics: MutableList<Diagnostic>, content: String, uri: String) {
        try {
            val fileType = FileTypeDetector.detect(uri, content)
            val parseResult: Any? = when (fileType) {
                FileType.JSON -> jsonDslParser.parseComponent(content).let { if (it.isSuccess) it.getOrNull() else null }
                FileType.YAML -> vosParser.parseComponent(content).let { if (it.isSuccess) it.getOrNull() else null }
                FileType.COMPACT -> compactSyntaxParser.parseComponent(content).let { if (it.isSuccess) it.getOrNull() else null }
                FileType.UNKNOWN -> return
            }
            if (parseResult == null) return

            val rootComponent: ComponentNode? = when (parseResult) {
                is ScreenNode -> parseResult.root
                is ComponentNode -> parseResult
                else -> null
            }
            if (rootComponent == null) return
            validateComponentTree(rootComponent, diagnostics, 0)
        } catch (e: Exception) {
            logger.debug("Semantic validation skipped: ${e.message}")
        }
    }

    private fun validateComponentTree(component: ComponentNode, diagnostics: MutableList<Diagnostic>, depth: Int) {
        validateComponentRules(component, diagnostics)
        validatePropertyValues(component, diagnostics)
        validateRequiredFields(component, diagnostics)

        if (depth > 10) {
            diagnostics.add(createWarning(0, "Component nesting depth exceeds 10 levels, consider refactoring"))
        }

        component.children.forEach { child ->
            if (child is ComponentNode) {
                validateComponentTree(child, diagnostics, depth + 1)
                validateParentChildRelationship(component, child, diagnostics)
            }
        }
    }

    private fun validateComponentRules(component: ComponentNode, diagnostics: MutableList<Diagnostic>) {
        when (component.type) {
            ComponentType.BUTTON -> {
                if (!component.properties.containsKey("text") && !component.properties.containsKey("icon")) {
                    diagnostics.add(createWarning(0, "Button should have 'text' or 'icon' property"))
                }
            }
            ComponentType.TEXT_FIELD -> {
                if (!component.properties.containsKey("avid") && !component.properties.containsKey("id")) {
                    diagnostics.add(createWarning(0, "TextField should have 'avid' for data binding"))
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
            else -> {}
        }
    }

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
                    val valid = setOf("start", "center", "end", "top", "bottom", "left", "right")
                    if (value.toString().lowercase() !in valid) {
                        diagnostics.add(createWarning(0, "Invalid alignment value: $value"))
                    }
                }
            }
        }
    }

    private fun validateRequiredFields(component: ComponentNode, diagnostics: MutableList<Diagnostic>) {
        if (!component.properties.containsKey("avid") && !component.properties.containsKey("id")) {
            diagnostics.add(createInfo(0, "Consider adding 'avid' property for voice navigation"))
        }
        val interactiveTypes = setOf(
            ComponentType.BUTTON, ComponentType.CHECKBOX, ComponentType.SWITCH,
            ComponentType.TEXT_FIELD, ComponentType.SLIDER, ComponentType.RADIO
        )
        if (component.type in interactiveTypes && component.eventHandlers.isEmpty()) {
            diagnostics.add(createInfo(0, "Interactive component should have event handler (onClick, onChange, etc.)"))
        }
    }

    private fun validateParentChildRelationship(parent: ComponentNode, child: ComponentNode, diagnostics: MutableList<Diagnostic>) {
        if (parent.type == ComponentType.SCROLL_VIEW && child.type == ComponentType.SCROLL_VIEW) {
            diagnostics.add(createWarning(0, "ScrollView should not contain another ScrollView"))
        }
        if (parent.type == ComponentType.CARD && child.type == ComponentType.CARD) {
            diagnostics.add(createInfo(0, "Consider avoiding direct Card nesting for better UX"))
        }
    }

    companion object {
        fun isValidColor(value: String): Boolean {
            if (value.matches(Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))) return true
            val named = setOf("red", "blue", "green", "black", "white", "gray", "yellow", "orange", "purple")
            return value.lowercase() in named
        }

        fun isValidSize(value: String): Boolean {
            return value.matches(Regex("^\\d+(\\.\\d+)?(dp|sp|px|%)$"))
        }

        fun createError(line: Int, message: String) = Diagnostic().apply {
            range = Range(Position(line, 0), Position(line, 100))
            severity = DiagnosticSeverity.Error
            this.message = message
            source = "avanueui-semantic"
        }

        fun createWarning(line: Int, message: String) = Diagnostic().apply {
            range = Range(Position(line, 0), Position(line, 100))
            severity = DiagnosticSeverity.Warning
            this.message = message
            source = "avanueui-semantic"
        }

        fun createInfo(line: Int, message: String) = Diagnostic().apply {
            range = Range(Position(line, 0), Position(line, 100))
            severity = DiagnosticSeverity.Information
            this.message = message
            source = "avanueui-semantic"
        }
    }
}
