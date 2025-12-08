package net.ideahq.avamagic.codegen.parser

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import net.ideahq.avamagic.codegen.ast.*

/**
 * JsonDSLParser - Production JSON parser using kotlinx.serialization
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

@Serializable
data class ScreenDefinition(
    val name: String,
    val imports: List<String> = emptyList(),
    val state: List<StateVariableDefinition> = emptyList(),
    val root: ComponentDefinition
)

@Serializable
data class ComponentDefinition(
    val id: String? = null,
    val type: String,
    val properties: Map<String, JsonElement> = emptyMap(),
    val children: List<ComponentDefinition> = emptyList(),
    val events: Map<String, String> = emptyMap()
)

@Serializable
data class StateVariableDefinition(
    val name: String,
    val type: String,
    val initialValue: JsonElement? = null,
    val mutable: Boolean = true
)

@Serializable
data class ThemeDefinition(
    val name: String,
    val description: String? = null,
    val colors: Map<String, String> = emptyMap(),
    val typography: Map<String, TypographyDefinition> = emptyMap(),
    val spacing: Map<String, Int> = emptyMap(),
    val shapes: Map<String, ShapeDefinition> = emptyMap(),
    val elevation: Map<String, Int> = emptyMap(),
    val animation: Map<String, Int> = emptyMap()
)

@Serializable
data class TypographyDefinition(
    val fontFamily: String,
    val fontSize: Int,
    val fontWeight: String,
    val lineHeight: Double
)

@Serializable
data class ShapeDefinition(
    val cornerRadius: Int,
    val borderWidth: Int? = null,
    val borderColor: String? = null
)

/**
 * Production JSON DSL Parser
 */
class JsonDSLParser {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parse screen from JSON string
     */
    fun parseScreen(jsonString: String): Result<ScreenNode> {
        return try {
            val definition = json.decodeFromString<ScreenDefinition>(jsonString)
            val screen = buildScreenNode(definition)
            Result.success(screen)
        } catch (e: SerializationException) {
            Result.failure(ParseException("JSON parsing error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(ParseException("Failed to parse screen: ${e.message}", e))
        }
    }

    /**
     * Parse component from JSON string
     */
    fun parseComponent(jsonString: String): Result<ComponentNode> {
        return try {
            val definition = json.decodeFromString<ComponentDefinition>(jsonString)
            val component = buildComponentNode(definition)
            Result.success(component)
        } catch (e: SerializationException) {
            Result.failure(ParseException("JSON parsing error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(ParseException("Failed to parse component: ${e.message}", e))
        }
    }

    /**
     * Parse theme from JSON string
     */
    fun parseTheme(jsonString: String): Result<ThemeNode> {
        return try {
            val definition = json.decodeFromString<ThemeDefinition>(jsonString)
            val theme = buildThemeNode(definition)
            Result.success(theme)
        } catch (e: SerializationException) {
            Result.failure(ParseException("JSON parsing error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(ParseException("Failed to parse theme: ${e.message}", e))
        }
    }

    /**
     * Validate JSON structure without full parsing
     */
    fun validate(jsonString: String): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        try {
            // Parse as generic JSON first
            val jsonElement = json.parseToJsonElement(jsonString)

            if (jsonElement !is JsonObject) {
                errors.add(ValidationError("Root must be a JSON object", 0))
                return ValidationResult(errors, warnings)
            }

            // Check for required fields
            if (!jsonElement.containsKey("type")) {
                errors.add(ValidationError("Missing required field: type", 0))
            }

            // Validate type field
            val type = jsonElement["type"]
            if (type is JsonPrimitive && type.isString) {
                val typeValue = type.content
                if (!isValidComponentType(typeValue)) {
                    errors.add(ValidationError("Invalid component type: $typeValue", 0))
                }
            }

            // Validate properties
            if (jsonElement.containsKey("properties")) {
                val props = jsonElement["properties"]
                if (props !is JsonObject) {
                    errors.add(ValidationError("Properties must be an object", 0))
                }
            }

            // Validate children
            if (jsonElement.containsKey("children")) {
                val children = jsonElement["children"]
                if (children !is JsonArray) {
                    errors.add(ValidationError("Children must be an array", 0))
                } else {
                    children.forEachIndexed { index, child ->
                        if (child !is JsonObject) {
                            errors.add(ValidationError("Child at index $index must be an object", index))
                        }
                    }
                }
            }

            // Check for common issues
            if (jsonElement.containsKey("onClick") && jsonElement.containsKey("events")) {
                warnings.add(ValidationWarning("Both onClick and events defined, events will take precedence", 0))
            }

        } catch (e: SerializationException) {
            errors.add(ValidationError("Invalid JSON: ${e.message}", 0))
        } catch (e: Exception) {
            errors.add(ValidationError("Validation error: ${e.message}", 0))
        }

        return ValidationResult(errors, warnings)
    }

    // Builder methods

    private fun buildScreenNode(definition: ScreenDefinition): ScreenNode {
        val root = buildComponentNode(definition.root)
        val stateVars = definition.state.map { buildStateVariable(it) }
        return ScreenNode(definition.name, root, stateVars, definition.imports)
    }

    private fun buildComponentNode(definition: ComponentDefinition): ComponentNode {
        val id = definition.id ?: generateId()
        val type = parseComponentType(definition.type)
        val properties = definition.properties.mapValues { (_, value) ->
            parseJsonElement(value)
        }
        val children = definition.children.map { buildComponentNode(it) }
        val eventHandlers = definition.events

        return ComponentNode(id, type, properties, children, eventHandlers)
    }

    private fun buildStateVariable(definition: StateVariableDefinition): StateVariable {
        val initialValue = definition.initialValue?.let { parseJsonElementToPropertyValue(it) }
        return StateVariable(definition.name, definition.type, initialValue, definition.mutable)
    }

    private fun buildThemeNode(definition: ThemeDefinition): ThemeNode {
        val typography = definition.typography.mapValues { (_, typoDef) ->
            TypographyStyle(
                fontFamily = typoDef.fontFamily,
                fontSize = typoDef.fontSize,
                fontWeight = typoDef.fontWeight,
                lineHeight = typoDef.lineHeight
            )
        }

        val shapes = definition.shapes.mapValues { (_, shapeDef) ->
            ShapeStyle(
                cornerRadius = shapeDef.cornerRadius,
                borderWidth = shapeDef.borderWidth,
                borderColor = shapeDef.borderColor
            )
        }

        return ThemeNode(definition.name, definition.colors, typography, definition.spacing, shapes)
    }

    // Parsing utilities

    private fun parseJsonElement(element: JsonElement): Any {
        return when (element) {
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.content == "true" || element.content == "false" -> element.content.toBoolean()
                element.content.toIntOrNull() != null -> element.content.toInt()
                element.content.toDoubleOrNull() != null -> element.content.toDouble()
                else -> element.content
            }
            is JsonArray -> element.map { parseJsonElement(it) }
            is JsonObject -> element.mapValues { (_, value) -> parseJsonElement(value) }
            else -> element.toString()
        }
    }

    private fun parseJsonElementToPropertyValue(element: JsonElement): PropertyValue {
        return when (element) {
            is JsonPrimitive -> when {
                element.isString -> PropertyValue.StringValue(element.content)
                element.content == "true" || element.content == "false" ->
                    PropertyValue.BoolValue(element.content.toBoolean())
                element.content.toIntOrNull() != null ->
                    PropertyValue.IntValue(element.content.toInt())
                element.content.toDoubleOrNull() != null ->
                    PropertyValue.DoubleValue(element.content.toDouble())
                else -> PropertyValue.StringValue(element.content)
            }
            is JsonArray -> PropertyValue.ListValue(element.map { parseJsonElementToPropertyValue(it) })
            is JsonObject -> PropertyValue.MapValue(element.mapValues { (_, value) ->
                parseJsonElementToPropertyValue(value)
            })
            else -> PropertyValue.StringValue(element.toString())
        }
    }

    private fun parseComponentType(typeName: String): ComponentType {
        return try {
            ComponentType.valueOf(typeName.uppercase().replace("-", "_"))
        } catch (e: IllegalArgumentException) {
            ComponentType.CUSTOM
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

    private var idCounter = 0
    private fun generateId(): String = "component_${idCounter++}"
}

/**
 * JSON serialization extensions
 */
fun ScreenNode.toJson(prettyPrint: Boolean = true): String {
    val json = Json { this.prettyPrint = prettyPrint }
    val definition = ScreenDefinition(
        name = name,
        imports = imports,
        state = stateVariables.map { it.toDefinition() },
        root = root.toDefinition()
    )
    return json.encodeToString(definition)
}

fun ComponentNode.toJson(prettyPrint: Boolean = true): String {
    val json = Json { this.prettyPrint = prettyPrint }
    val definition = toDefinition()
    return json.encodeToString(definition)
}

fun ThemeNode.toJson(prettyPrint: Boolean = true): String {
    val json = Json { this.prettyPrint = prettyPrint }
    val definition = ThemeDefinition(
        name = name,
        colors = colors,
        typography = typography.mapValues { (_, style) ->
            TypographyDefinition(
                fontFamily = style.fontFamily,
                fontSize = style.fontSize,
                fontWeight = style.fontWeight,
                lineHeight = style.lineHeight
            )
        },
        spacing = spacing,
        shapes = shapes.mapValues { (_, shape) ->
            ShapeDefinition(
                cornerRadius = shape.cornerRadius,
                borderWidth = shape.borderWidth,
                borderColor = shape.borderColor
            )
        }
    )
    return json.encodeToString(definition)
}

private fun StateVariable.toDefinition(): StateVariableDefinition {
    return StateVariableDefinition(
        name = name,
        type = type,
        initialValue = initialValue?.toJsonElement(),
        mutable = mutable
    )
}

private fun ComponentNode.toDefinition(): ComponentDefinition {
    return ComponentDefinition(
        id = id,
        type = type.name,
        properties = properties.mapValues { (_, value) -> value.toJsonElement() },
        children = children.map { it.toDefinition() },
        events = eventHandlers
    )
}

private fun Any.toJsonElement(): JsonElement {
    return when (this) {
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is List<*> -> JsonArray(this.map { it?.toJsonElement() ?: JsonNull })
        is Map<*, *> -> JsonObject(this.mapKeys { it.key.toString() }.mapValues {
            it.value?.toJsonElement() ?: JsonNull
        })
        else -> JsonPrimitive(toString())
    }
}

private fun PropertyValue.toJsonElement(): JsonElement {
    return when (this) {
        is PropertyValue.StringValue -> JsonPrimitive(value)
        is PropertyValue.IntValue -> JsonPrimitive(value)
        is PropertyValue.DoubleValue -> JsonPrimitive(value)
        is PropertyValue.BoolValue -> JsonPrimitive(value)
        is PropertyValue.EnumValue -> JsonPrimitive(value)
        is PropertyValue.ListValue -> JsonArray(items.map { it.toJsonElement() })
        is PropertyValue.MapValue -> JsonObject(items.mapValues { (_, v) -> v.toJsonElement() })
        is PropertyValue.ReferenceValue -> JsonPrimitive(ref)
    }
}
