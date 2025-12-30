package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class JsonPluginParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(jsonString: String): Result<MagicElementPlugin> {
        return try {
            val format = json.decodeFromString<PluginJsonFormat>(jsonString)
            val plugin = createPluginFromFormat(format)
            Result.success(plugin)
        } catch (e: Exception) {
            Result.failure(PluginException.LoadException("Failed to parse JSON plugin: ${e.message}", e))
        }
    }

    private fun createPluginFromFormat(format: PluginJsonFormat): MagicElementPlugin {
        val metadata = PluginMetadata(
            id = format.metadata.id,
            name = format.metadata.name,
            version = format.metadata.version,
            author = format.metadata.author ?: "Unknown",
            description = format.metadata.description ?: "",
            minSdkVersion = "${format.metadata.minSdkVersion}.0.0",
            permissions = format.metadata.permissions.mapNotNull {
                try { Permission.valueOf(it) } catch (e: Exception) { null }
            }.toSet(),
            dependencies = format.metadata.dependencies
        )

        val components = format.components.map { def ->
            ComponentDefinition(
                type = def.type,
                factory = { config ->
                    createComponentFromConfig(config, def)
                },
                validator = { config ->
                    validateComponent(config, def.schema)
                },
                schema = def.schema?.let { schema ->
                    ComponentSchema(
                        properties = schema.properties.mapValues { (_, prop) ->
                            PropertySchema(
                                type = parsePropertyType(prop.type),
                                description = prop.description ?: "",
                                defaultValue = prop.defaultValue?.toString(),
                                allowedValues = prop.allowedValues?.map { it.toString() }
                            )
                        },
                        requiredProperties = schema.requiredProperties.toSet()
                    )
                }
            )
        }

        return JsonPlugin(metadata, components)
    }

    private fun createComponentFromConfig(
        config: ComponentConfig,
        def: ComponentJsonDef
    ): Component {
        return GenericPluginComponent(
            type = def.type,
            id = config.id,
            properties = config.properties,
            style = config.style,
            modifiers = config.modifiers
        )
    }

    private fun validateComponent(
        config: ComponentConfig,
        schema: ComponentSchemaJson?
    ): ValidationResult {
        if (schema == null) return ValidationResult(true, emptyList())

        val errors = mutableListOf<ValidationError>()

        schema.requiredProperties.forEach { required ->
            if (!config.properties.containsKey(required)) {
                errors.add(ValidationError(
                    field = required,
                    message = "Required property '$required' is missing",
                    code = "MISSING_REQUIRED"
                ))
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun parsePropertyType(type: String): PropertyType {
        return try {
            PropertyType.valueOf(type.uppercase())
        } catch (e: Exception) {
            PropertyType.STRING // Default to STRING if unknown
        }
    }
}

private class JsonPlugin(
    override val metadata: PluginMetadata,
    private val componentDefs: List<ComponentDefinition>
) : MagicElementPlugin {

    override val id: String = metadata.id

    override fun getComponents(): List<ComponentDefinition> = componentDefs

    override fun onLoad() {}

    override fun onUnload() {}
}

data class GenericPluginComponent(
    override val type: String,
    override val id: String?,
    val properties: Map<String, Any?>,
    override val style: ComponentStyle?,
    override val modifiers: List<Modifier>
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
