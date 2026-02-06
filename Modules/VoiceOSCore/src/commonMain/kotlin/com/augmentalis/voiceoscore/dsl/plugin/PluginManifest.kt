package com.augmentalis.voiceoscore.dsl.plugin

import com.augmentalis.voiceoscore.dsl.ast.AvuDslHeader

/**
 * Plugin manifest extracted from .avp file header.
 *
 * Contains all metadata needed to validate, register, and manage a plugin
 * throughout its lifecycle. Extracted from [AvuDslHeader] during plugin loading.
 */
data class PluginManifest(
    val pluginId: String,
    val name: String,
    val version: String,
    val minVosVersion: Int?,
    val author: String?,
    val description: String?,
    val codes: Map<String, String>,
    val permissions: Set<PluginPermission>,
    val triggers: List<String>
) {
    /**
     * Validate that the manifest has all required fields.
     */
    fun validate(): ManifestValidation {
        val errors = mutableListOf<String>()

        if (pluginId.isBlank()) errors.add("plugin_id is required")
        else if (!pluginId.matches(Regex("^[a-zA-Z][a-zA-Z0-9._-]*$")))
            errors.add("plugin_id must be a valid identifier: $pluginId")
        if (name.isBlank()) errors.add("name is required")
        if (version.isBlank()) errors.add("version is required")
        if (codes.isEmpty()) errors.add("At least one code must be declared")
        if (triggers.isEmpty()) errors.add("At least one trigger must be declared")

        return ManifestValidation(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    companion object {
        /**
         * Extract a plugin manifest from a parsed DSL header.
         */
        fun fromHeader(header: AvuDslHeader): PluginManifest {
            val metadata = header.metadata
            return PluginManifest(
                pluginId = metadata["plugin_id"] ?: "",
                name = metadata["name"] ?: "",
                version = header.version,
                minVosVersion = metadata["min_vos_version"]?.toIntOrNull(),
                author = metadata["author"],
                description = metadata["description"],
                codes = header.codes,
                permissions = PluginPermission.parseList(header.permissions),
                triggers = header.triggers
            )
        }
    }
}

data class ManifestValidation(
    val isValid: Boolean,
    val errors: List<String>
)
