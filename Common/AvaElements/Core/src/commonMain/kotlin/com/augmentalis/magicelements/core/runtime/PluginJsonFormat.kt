package com.augmentalis.avaelements.core.runtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PluginJsonFormat(
    val metadata: PluginMetadataJson,
    val components: List<ComponentJsonDef> = emptyList(),
    val themes: List<ThemeJsonDef>? = null
)

@Serializable
data class PluginMetadataJson(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val description: String? = null,
    val minSdkVersion: Int = 1,
    val permissions: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

@Serializable
data class ComponentJsonDef(
    val type: String,
    val schema: ComponentSchemaJson? = null,
    val defaultProps: Map<String, JsonElement>? = null,
    val template: JsonElement? = null
)

@Serializable
data class ComponentSchemaJson(
    val properties: Map<String, PropertySchemaJson> = emptyMap(),
    val requiredProperties: List<String> = emptyList()
)

@Serializable
data class PropertySchemaJson(
    val type: String,
    val description: String? = null,
    val defaultValue: JsonElement? = null,
    val allowedValues: List<JsonElement>? = null
)

@Serializable
data class ThemeJsonDef(
    val name: String,
    val colors: Map<String, String>? = null,
    val typography: Map<String, String>? = null,
    val spacing: Map<String, Float>? = null
)
