package com.augmentalis.magiccode.plugins.core

import com.augmentalis.avucodec.core.AvuEscape

/**
 * AVU Manifest Parser - Parses Avanues Universal Format plugin manifests.
 *
 * Converts AVU format plugin.avu files to [PluginManifest] instances.
 * This parser supports the AVU Plugin Format v1.0 specification.
 *
 * ## AVU Format Structure
 * ```
 * # Avanues Universal Plugin Format v1.0
 * PLG:id:version:entrypoint:name
 * DSC:description text
 * AUT:name:email:url
 * PCP:cap1|cap2|cap3
 * MOD:module1|module2
 * DEP:pluginId:versionConstraint
 * PRM:permission:rationale
 * PLT:platform:minVersion
 * AST:type:path
 * CFG:start
 * KEY:name:type:default:description
 * CFG:end
 * HKS:event:handler
 * ```
 *
 * ## Usage
 * ```kotlin
 * val avuContent = File("plugin.avu").readText()
 * val manifest = AvuManifestParser.parse(avuContent)
 * if (manifest != null) {
 *     println("Loaded plugin: ${manifest.id}")
 * }
 * ```
 *
 * @since 2.0.0
 * @see PluginManifest
 */
object AvuManifestParser {

    // AVU Protocol codes for plugin manifests
    private const val CODE_PLUGIN = "PLG"
    private const val CODE_DESCRIPTION = "DSC"
    private const val CODE_AUTHOR = "AUT"
    private const val CODE_PLUGIN_CAP = "PCP"
    private const val CODE_MODULE = "MOD"
    private const val CODE_DEPENDENCY = "DEP"
    private const val CODE_PERMISSION = "PRM"
    private const val CODE_PLATFORM = "PLT"
    private const val CODE_ASSET = "AST"
    private const val CODE_CONFIG = "CFG"
    private const val CODE_CONFIG_KEY = "KEY"
    private const val CODE_HOOK = "HKS"

    /**
     * Parse AVU format content to PluginManifest.
     *
     * @param content AVU format string
     * @return PluginManifest or null if parsing fails
     */
    fun parse(content: String): PluginManifest? {
        if (content.isBlank()) return null

        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        var id = ""
        var version = ""
        var entrypoint = ""
        var name = ""
        var description: String? = null
        var author = ""
        var authorEmail = ""
        var authorUrl = ""
        val capabilities = mutableListOf<String>()
        val targetModules = mutableListOf<String>()
        val dependencies = mutableListOf<PluginDependency>()
        val permissions = mutableListOf<String>()
        val permissionRationales = mutableMapOf<String, String>()
        val platforms = mutableMapOf<String, String>()
        val assetImages = mutableListOf<String>()
        val assetFonts = mutableListOf<String>()
        val assetIcons = mutableListOf<String>()
        val assetThemes = mutableListOf<String>()
        val assetCustom = mutableListOf<String>()
        val configSchema = mutableListOf<ConfigKey>()
        val hooks = mutableMapOf<String, String>()

        var inConfigBlock = false

        for (line in lines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex < 0) continue

            val code = line.substring(0, colonIndex).uppercase()
            val value = line.substring(colonIndex + 1)

            when {
                code == CODE_CONFIG && value == "start" -> {
                    inConfigBlock = true
                }
                code == CODE_CONFIG && value == "end" -> {
                    inConfigBlock = false
                }
                inConfigBlock && code == CODE_CONFIG_KEY -> {
                    parseConfigKey(value)?.let { configSchema.add(it) }
                }
                else -> {
                    when (code) {
                        CODE_PLUGIN -> {
                            val parts = splitUnescaped(value)
                            id = unescape(parts.getOrNull(0) ?: "")
                            version = unescape(parts.getOrNull(1) ?: "")
                            entrypoint = unescape(parts.getOrNull(2) ?: "")
                            name = unescape(parts.getOrNull(3) ?: "")
                        }
                        CODE_DESCRIPTION -> {
                            description = unescape(value)
                        }
                        CODE_AUTHOR -> {
                            val parts = splitUnescaped(value)
                            author = unescape(parts.getOrNull(0) ?: "")
                            authorEmail = unescape(parts.getOrNull(1) ?: "")
                            authorUrl = unescape(parts.getOrNull(2) ?: "")
                        }
                        CODE_PLUGIN_CAP -> {
                            val caps = value.split("|").map { unescape(it) }
                            capabilities.addAll(caps)
                        }
                        CODE_MODULE -> {
                            val mods = value.split("|").map { unescape(it) }
                            targetModules.addAll(mods)
                        }
                        CODE_DEPENDENCY -> {
                            val parts = splitUnescaped(value)
                            val depId = unescape(parts.getOrNull(0) ?: "")
                            val depVersion = unescape(parts.getOrElse(1) { "*" })
                            if (depId.isNotBlank()) {
                                dependencies.add(PluginDependency(depId, depVersion))
                            }
                        }
                        CODE_PERMISSION -> {
                            val parts = splitUnescaped(value)
                            val perm = unescape(parts.getOrNull(0) ?: "")
                            val rationale = unescape(parts.getOrElse(1) { "" })
                            if (perm.isNotBlank()) {
                                permissions.add(perm)
                                if (rationale.isNotBlank()) {
                                    permissionRationales[perm] = rationale
                                }
                            }
                        }
                        CODE_PLATFORM -> {
                            val parts = splitUnescaped(value)
                            val platform = unescape(parts.getOrNull(0) ?: "")
                            val minVersion = unescape(parts.getOrElse(1) { "any" })
                            if (platform.isNotBlank()) {
                                platforms[platform] = minVersion
                            }
                        }
                        CODE_ASSET -> {
                            val parts = splitUnescaped(value)
                            val type = unescape(parts.getOrNull(0) ?: "")
                            val path = unescape(parts.getOrElse(1) { "" })
                            if (path.isNotBlank()) {
                                when (type.lowercase()) {
                                    "image", "images" -> assetImages.add(path)
                                    "font", "fonts" -> assetFonts.add(path)
                                    "icon", "icons" -> assetIcons.add(path)
                                    "theme", "themes" -> assetThemes.add(path)
                                    "model", "config", "locale" -> assetCustom.add(path)
                                    else -> assetCustom.add(path)
                                }
                            }
                        }
                        CODE_HOOK -> {
                            val parts = splitUnescaped(value)
                            val event = unescape(parts.getOrNull(0) ?: "")
                            val handler = unescape(parts.getOrElse(1) { "" })
                            if (event.isNotBlank() && handler.isNotBlank()) {
                                hooks[event] = handler
                            }
                        }
                    }
                }
            }
        }

        // Validate required fields
        if (id.isBlank() || version.isBlank() || entrypoint.isBlank()) {
            return null
        }

        // Determine source based on ID
        val source = when {
            id.startsWith("com.augmentalis.") -> "PRE_BUNDLED"
            id.startsWith("com.appavenue.") -> "APPAVENUE_STORE"
            else -> "THIRD_PARTY"
        }

        // Build assets if any are present
        val assets = if (assetImages.isNotEmpty() || assetFonts.isNotEmpty() ||
            assetIcons.isNotEmpty() || assetThemes.isNotEmpty() || assetCustom.isNotEmpty()) {
            PluginAssets(
                images = assetImages,
                fonts = assetFonts,
                icons = assetIcons,
                themes = assetThemes,
                custom = assetCustom
            )
        } else {
            null
        }

        return PluginManifest(
            id = id,
            name = name.ifBlank { id.substringAfterLast('.') },
            version = version,
            author = author,
            description = description,
            entrypoint = entrypoint,
            capabilities = capabilities,
            dependencies = dependencies,
            permissions = permissions.distinct(),
            permissionRationales = permissionRationales,
            source = source,
            verificationLevel = "UNVERIFIED",
            assets = assets,
            manifestVersion = "2.0",
            homepage = authorUrl.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Check if content is an AVU plugin manifest.
     */
    fun isAvuManifest(content: String): Boolean {
        return content.lines().any { line ->
            val trimmed = line.trim()
            trimmed.startsWith("$CODE_PLUGIN:") ||
                trimmed.contains("Avanues Universal Plugin Format")
        }
    }

    /**
     * Convert PluginManifest to AVU format string.
     *
     * @param manifest Plugin manifest to convert
     * @return AVU format string
     */
    fun toAvu(manifest: PluginManifest): String = buildString {
        // Header comment
        appendLine("# Avanues Universal Plugin Format v1.0")
        appendLine("# Type: Plugin Manifest")
        appendLine("# Extension: .avu")
        appendLine()

        // Plugin header
        appendLine("$CODE_PLUGIN:${escape(manifest.id)}:${escape(manifest.version)}:${escape(manifest.entrypoint)}:${escape(manifest.name)}")

        // Description
        manifest.description?.let {
            if (it.isNotBlank()) {
                appendLine("$CODE_DESCRIPTION:${escape(it)}")
            }
        }

        // Author
        if (manifest.author.isNotBlank()) {
            val authorLine = buildString {
                append("$CODE_AUTHOR:${escape(manifest.author)}")
                append(":") // email (empty if not available)
                append(":${escape(manifest.homepage ?: "")}") // url
            }
            appendLine(authorLine)
        }

        // Capabilities
        if (manifest.capabilities.isNotEmpty()) {
            appendLine("$CODE_PLUGIN_CAP:${manifest.capabilities.joinToString("|") { escape(it) }}")
        }

        // Dependencies
        manifest.dependencies.forEach { dep ->
            appendLine("$CODE_DEPENDENCY:${escape(dep.pluginId)}:${escape(dep.version)}")
        }

        // Permissions
        manifest.permissions.forEach { perm ->
            val rationale = manifest.permissionRationales[perm] ?: ""
            appendLine("$CODE_PERMISSION:${escape(perm)}:${escape(rationale)}")
        }

        // Assets
        manifest.assets?.let { assets ->
            assets.images.forEach { appendLine("$CODE_ASSET:image:${escape(it)}") }
            assets.fonts.forEach { appendLine("$CODE_ASSET:font:${escape(it)}") }
            assets.icons.forEach { appendLine("$CODE_ASSET:icon:${escape(it)}") }
            assets.themes.forEach { appendLine("$CODE_ASSET:theme:${escape(it)}") }
            assets.custom.forEach { appendLine("$CODE_ASSET:custom:${escape(it)}") }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private fun parseConfigKey(value: String): ConfigKey? {
        val parts = splitUnescaped(value)
        if (parts.isEmpty()) return null
        return ConfigKey(
            name = unescape(parts.getOrNull(0) ?: ""),
            type = unescape(parts.getOrElse(1) { "string" }),
            default = unescape(parts.getOrElse(2) { "" }),
            description = unescape(parts.getOrElse(3) { "" })
        )
    }

    /**
     * Split a value by colons, respecting escaped colons.
     */
    private fun splitUnescaped(value: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < value.length) {
            when {
                i + 2 < value.length && value.substring(i, i + 3) == "%3A" -> {
                    current.append("%3A")
                    i += 3
                }
                value[i] == ':' -> {
                    result.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(value[i])
                    i++
                }
            }
        }
        result.add(current.toString())
        return result
    }

    /**
     * Escape special characters per AVU protocol specification.
     *
     * Delegates to [AvuEscape.escape] - the canonical implementation.
     */
    private fun escape(text: String): String = AvuEscape.escape(text)

    /**
     * Unescape special characters.
     *
     * Delegates to [AvuEscape.unescape] - the canonical implementation.
     */
    private fun unescape(text: String): String = AvuEscape.unescape(text)

    // ════════════════════════════════════════════════════════════════════════
    // CONFIG KEY DATA CLASS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Plugin configuration key definition.
     */
    data class ConfigKey(
        val name: String,
        val type: String,
        val default: String = "",
        val description: String = ""
    )
}
