package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * Provides context-aware autocompletion for AvanueUI DSL documents.
 * Single responsibility: analyze completion context and return suggestions.
 */
class CompletionProvider {

    fun getCompletionItems(content: String, position: Position): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val context = analyzeContext(content, position)

        when (context.type) {
            CompletionContext.COMPONENT_NAME -> items.addAll(getComponentCompletions())
            CompletionContext.PROPERTY_NAME -> items.addAll(getPropertyCompletions(context.componentType))
            CompletionContext.PROPERTY_VALUE -> items.addAll(getValueCompletions(context.propertyName))
            CompletionContext.EVENT_HANDLER -> items.addAll(getEventHandlerCompletions())
            else -> {
                items.addAll(getComponentCompletions())
                items.addAll(getPropertyCompletions(null))
            }
        }
        return items
    }

    private fun analyzeContext(content: String, position: Position): CompletionContextInfo {
        val lines = content.lines()
        if (position.line >= lines.size) return CompletionContextInfo(CompletionContext.UNKNOWN)

        val currentLine = lines[position.line]
        val textBeforeCursor = currentLine.substring(0, minOf(position.character, currentLine.length))

        if (textBeforeCursor.contains(":")) {
            val propertyName = textBeforeCursor.substringBeforeLast(":").trim().split(" ").last()
            return CompletionContextInfo(type = CompletionContext.PROPERTY_VALUE, propertyName = propertyName)
        }
        if (textBeforeCursor.trim().startsWith("on")) {
            return CompletionContextInfo(CompletionContext.EVENT_HANDLER)
        }
        if (textBeforeCursor.startsWith("  ") || textBeforeCursor.startsWith("\t")) {
            return CompletionContextInfo(CompletionContext.PROPERTY_NAME)
        }
        return CompletionContextInfo(CompletionContext.COMPONENT_NAME)
    }

    private fun getComponentCompletions(): List<CompletionItem> {
        val components = mapOf(
            "Button" to "avid: \${1:button-id}\n  text: \${2:Click me}\n  onClick: \${3:handleClick}",
            "TextField" to "avid: \${1:field-id}\n  placeholder: \${2:Enter text}\n  onChange: \${3:handleChange}",
            "Card" to "avid: \${1:card-id}\n  children:\n    - \${2}",
            "Text" to "avid: \${1:text-id}\n  text: \${2:Hello World}",
            "Image" to "avid: \${1:image-id}\n  src: \${2:image.png}",
            "Column" to "avid: \${1:column-id}\n  children:\n    - \${2}",
            "Row" to "avid: \${1:row-id}\n  children:\n    - \${2}",
            "Container" to "avid: \${1:container-id}\n  children:\n    - \${2}",
            "Checkbox" to "avid: \${1:checkbox-id}\n  checked: \${2:false}\n  onChange: \${3:handleCheck}",
            "Switch" to "avid: \${1:switch-id}\n  enabled: \${2:true}\n  onChange: \${3:handleSwitch}"
        )
        return components.map { (name, snippet) ->
            CompletionItem(name).apply {
                kind = CompletionItemKind.Class
                detail = "AvanueUI $name Component"
                documentation = Either.forLeft(ComponentDocs.getDocumentation(name))
                insertText = "$name:\n  $snippet"
                insertTextFormat = InsertTextFormat.Snippet
                sortText = "0$name"
            }
        }
    }

    private fun getPropertyCompletions(componentType: String?): List<CompletionItem> {
        val commonProps = listOf(
            "avid" to "AvanueUI Voice IDentifier for navigation",
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
                sortText = "1$name"
            }
        }
    }

    private fun getValueCompletions(propertyName: String?): List<CompletionItem> {
        return when (propertyName?.lowercase()) {
            "color", "backgroundcolor", "bordercolor" -> getColorCompletions()
            "alignment", "gravity" -> getAlignmentCompletions()
            "visible", "enabled", "checked" -> getBooleanCompletions()
            else -> emptyList()
        }
    }

    private fun getColorCompletions(): List<CompletionItem> {
        val colors = mapOf(
            "red" to "#FF0000", "blue" to "#0000FF", "green" to "#00FF00",
            "black" to "#000000", "white" to "#FFFFFF", "gray" to "#808080",
            "yellow" to "#FFFF00", "orange" to "#FFA500", "purple" to "#800080"
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

    private fun getAlignmentCompletions(): List<CompletionItem> {
        return listOf("start", "center", "end", "top", "bottom", "left", "right").map {
            CompletionItem(it).apply { kind = CompletionItemKind.Enum; detail = "Alignment value"; insertText = it }
        }
    }

    private fun getBooleanCompletions(): List<CompletionItem> {
        return listOf("true", "false").map {
            CompletionItem(it).apply { kind = CompletionItemKind.Value; detail = "Boolean value"; insertText = it }
        }
    }

    private fun getEventHandlerCompletions(): List<CompletionItem> {
        val handlers = mapOf(
            "onClick" to "Click event handler", "onChange" to "Change event handler",
            "onSubmit" to "Submit event handler", "onFocus" to "Focus event handler",
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

    private enum class CompletionContext {
        COMPONENT_NAME, PROPERTY_NAME, PROPERTY_VALUE, EVENT_HANDLER, UNKNOWN
    }

    private data class CompletionContextInfo(
        val type: CompletionContext,
        val componentType: String? = null,
        val propertyName: String? = null
    )
}
