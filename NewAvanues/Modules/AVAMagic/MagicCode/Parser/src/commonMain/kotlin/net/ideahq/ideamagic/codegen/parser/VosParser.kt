package com.augmentalis.magiccode.generator.parser

import com.augmentalis.magiccode.generator.ast.*

/**
 * VosParser - Parses AvaUI DSL to AST
 *
 * Supports JSON-based DSL for App Store compliance
 * (interpreted as data, not executed as code)
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class VosParser {

    /**
     * Parse JSON DSL to Screen AST
     */
    fun parseScreen(json: String): Result<ScreenNode> {
        return try {
            val data = parseJson(json)
            val screen = buildScreenNode(data)
            Result.success(screen)
        } catch (e: Exception) {
            Result.failure(ParseException("Failed to parse screen: ${e.message}", e))
        }
    }

    /**
     * Parse component definition
     */
    fun parseComponent(json: String): Result<ComponentNode> {
        return try {
            val data = parseJson(json)
            val component = buildComponentNode(data)
            Result.success(component)
        } catch (e: Exception) {
            Result.failure(ParseException("Failed to parse component: ${e.message}", e))
        }
    }

    /**
     * Parse theme definition
     */
    fun parseTheme(json: String): Result<ThemeNode> {
        return try {
            val data = parseJson(json)
            val theme = buildThemeNode(data)
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(ParseException("Failed to parse theme: ${e.message}", e))
        }
    }

    /**
     * Validate DSL structure
     */
    fun validate(json: String): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        try {
            val data = parseJson(json)

            // Validate required fields
            if (!data.containsKey("type")) {
                errors.add(ValidationError("Missing required field: type", 0))
            }

            // Validate component type
            val type = data["type"] as? String
            if (type != null && !isValidComponentType(type)) {
                errors.add(ValidationError("Invalid component type: $type", 0))
            }

            // Validate properties
            val properties = data["properties"] as? Map<*, *>
            if (properties != null) {
                validateProperties(type ?: "", properties, errors, warnings)
            }

            // Validate children
            val children = data["children"] as? List<*>
            if (children != null) {
                validateChildren(children, errors, warnings)
            }

        } catch (e: Exception) {
            errors.add(ValidationError("Parse error: ${e.message}", 0))
        }

        return ValidationResult(errors, warnings)
    }

    // Private helper methods

    private fun parseJson(json: String): Map<String, Any> {
        // Simplified JSON parsing - production would use kotlinx.serialization
        // This is a placeholder for the actual implementation
        return emptyMap()
    }

    private fun buildScreenNode(data: Map<String, Any>): ScreenNode {
        val name = data["name"] as? String ?: "Screen"
        val rootData = data["root"] as? Map<String, Any> ?: throw ParseException("Missing root component")
        val root = buildComponentNode(rootData)

        val stateVars = (data["state"] as? List<*>)?.mapNotNull { stateData ->
            buildStateVariable(stateData as? Map<String, Any> ?: return@mapNotNull null)
        } ?: emptyList()

        val imports = (data["imports"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        return ScreenNode(name, root, stateVars, imports)
    }

    private fun buildComponentNode(data: Map<String, Any>): ComponentNode {
        val id = data["id"] as? String ?: generateId()
        val typeName = data["type"] as? String ?: throw ParseException("Missing component type")
        val type = parseComponentType(typeName)

        val properties = (data["properties"] as? Map<String, Any>)?.mapValues { (_, value) ->
            parsePropertyValue(value)
        } ?: emptyMap()

        val children = (data["children"] as? List<*>)?.mapNotNull { childData ->
            buildComponentNode(childData as? Map<String, Any> ?: return@mapNotNull null)
        } ?: emptyList()

        val eventHandlers = (data["events"] as? Map<*, *>)?.mapNotNull { (key, value) ->
            (key as? String)?.let { it to (value as? String ?: "") }
        }?.toMap() ?: emptyMap()

        return ComponentNode(id, type, properties, children, eventHandlers)
    }

    private fun buildStateVariable(data: Map<String, Any>): StateVariable {
        val name = data["name"] as? String ?: throw ParseException("Missing state variable name")
        val type = data["type"] as? String ?: "String"
        val initialValue = data["initialValue"]?.let { parsePropertyValue(it) }
        val mutable = data["mutable"] as? Boolean ?: true

        return StateVariable(name, type, initialValue, mutable)
    }

    private fun buildThemeNode(data: Map<String, Any>): ThemeNode {
        val name = data["name"] as? String ?: "Theme"
        val colors = (data["colors"] as? Map<*, *>)?.mapNotNull { (key, value) ->
            (key as? String)?.let { it to (value as? String ?: "") }
        }?.toMap() ?: emptyMap()

        val typography = emptyMap<String, TypographyStyle>() // Simplified
        val spacing = emptyMap<String, Int>() // Simplified
        val shapes = emptyMap<String, ShapeStyle>() // Simplified

        return ThemeNode(name, colors, typography, spacing, shapes)
    }

    private fun parseComponentType(typeName: String): ComponentType {
        return when (typeName.uppercase()) {
            "BUTTON" -> ComponentType.BUTTON
            "CARD" -> ComponentType.CARD
            "CHECKBOX" -> ComponentType.CHECKBOX
            "CHIP" -> ComponentType.CHIP
            "DIVIDER" -> ComponentType.DIVIDER
            "IMAGE" -> ComponentType.IMAGE
            "LIST_ITEM", "LISTITEM" -> ComponentType.LIST_ITEM
            "TEXT" -> ComponentType.TEXT
            "TEXT_FIELD", "TEXTFIELD" -> ComponentType.TEXT_FIELD
            "COLOR_PICKER", "COLORPICKER" -> ComponentType.COLOR_PICKER
            "ICON_PICKER", "ICONPICKER" -> ComponentType.ICON_PICKER
            "ICON" -> ComponentType.ICON
            "LABEL" -> ComponentType.LABEL
            "CONTAINER" -> ComponentType.CONTAINER
            "ROW" -> ComponentType.ROW
            "COLUMN" -> ComponentType.COLUMN
            "SPACER" -> ComponentType.SPACER
            "SWITCH" -> ComponentType.SWITCH
            "SLIDER" -> ComponentType.SLIDER
            "PROGRESS_BAR", "PROGRESSBAR" -> ComponentType.PROGRESS_BAR
            "SPINNER" -> ComponentType.SPINNER
            else -> ComponentType.CUSTOM
        }
    }

    private fun parsePropertyValue(value: Any): Any {
        return when (value) {
            is String -> value
            is Number -> value
            is Boolean -> value
            is List<*> -> value
            is Map<*, *> -> value
            else -> value.toString()
        }
    }

    private fun isValidComponentType(type: String): Boolean {
        return try {
            parseComponentType(type)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun validateProperties(
        type: String,
        properties: Map<*, *>,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // Component-specific property validation
        // This would be expanded based on component requirements
    }

    private fun validateChildren(
        children: List<*>,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        children.forEachIndexed { index, child ->
            if (child !is Map<*, *>) {
                errors.add(ValidationError("Invalid child at index $index", index))
            }
        }
    }

    private var idCounter = 0
    private fun generateId(): String = "component_${idCounter++}"
}

/**
 * Parse Exception
 */
class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Validation Result
 */
data class ValidationResult(
    val errors: List<ValidationError>,
    val warnings: List<ValidationWarning>
) {
    val isValid: Boolean get() = errors.isEmpty()
}

data class ValidationError(val message: String, val line: Int)
data class ValidationWarning(val message: String, val line: Int)
