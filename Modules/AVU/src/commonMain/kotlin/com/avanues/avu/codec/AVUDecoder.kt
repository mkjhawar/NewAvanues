package com.avanues.avu.codec

/**
 * AVU Decoder - Parses Avanues Universal IPC Protocol messages.
 *
 * Companion to [AVUEncoder] for decoding incoming AVU IPC Protocol messages.
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 */
object AVUDecoder {

    private const val DELIMITER = ':'

    /**
     * Parsed IPC message result.
     */
    data class ParsedMessage(
        val code: String,
        val id: String,
        val params: List<String>
    ) {
        /**
         * Get parameter at index, or null if not present.
         */
        fun paramOrNull(index: Int): String? = params.getOrNull(index)

        /**
         * Get parameter at index, unescaped.
         */
        fun param(index: Int): String = AVUEncoder.unescape(params[index])

        /**
         * Check if this is a specific message type.
         */
        fun isCode(expected: String): Boolean = code == expected
    }

    /**
     * Parse an IPC message string.
     *
     * @param message Raw IPC message (e.g., "VCM:cmd123:SCROLL_TOP")
     * @return Parsed message or null if invalid
     */
    fun parse(message: String): ParsedMessage? {
        if (message.length < 4) return null

        val parts = message.split(DELIMITER)
        if (parts.size < 2) return null

        val code = parts[0]
        if (code.length != 3 || !code.all { it.isUpperCase() }) return null

        val id = AVUEncoder.unescape(parts[1])
        val params = if (parts.size > 2) {
            parts.subList(2, parts.size).map { AVUEncoder.unescape(it) }
        } else {
            emptyList()
        }

        return ParsedMessage(code, id, params)
    }

    /**
     * Parse voice command message.
     *
     * @return Triple of (commandId, action, params) or null if not a voice command
     */
    fun parseVoiceCommand(message: String): Triple<String, String, Map<String, String>>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_VOICE_COMMAND)) return null

        val action = parsed.paramOrNull(0) ?: return null
        val params = mutableMapOf<String, String>()

        // Parse key=value params
        for (i in 1 until parsed.params.size) {
            val param = parsed.params[i]
            val eqIndex = param.indexOf('=')
            if (eqIndex > 0) {
                val key = param.substring(0, eqIndex)
                val value = param.substring(eqIndex + 1)
                params[key] = value
            }
        }

        return Triple(parsed.id, action, params)
    }

    /**
     * Parse error message.
     *
     * @return Triple of (requestId, errorCode, errorMessage) or null if not an error
     */
    fun parseError(message: String): Triple<String, Int, String>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_ERROR)) return null

        val errorCode = parsed.paramOrNull(0)?.toIntOrNull() ?: 0
        val errorMessage = parsed.paramOrNull(1) ?: "Unknown error"

        return Triple(parsed.id, errorCode, errorMessage)
    }

    /**
     * Parse handshake message.
     *
     * @return Triple of (sessionId, appId, version) or null if not a handshake
     */
    fun parseHandshake(message: String): Triple<String, String, String>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_HANDSHAKE)) return null

        val appId = parsed.paramOrNull(0) ?: return null
        val version = parsed.paramOrNull(1) ?: "1.0.0"

        return Triple(parsed.id, appId, version)
    }

    /**
     * Parse AI query message.
     *
     * @return Triple of (queryId, query, context) or null if not an AI query
     */
    fun parseAIQuery(message: String): Triple<String, String, String?>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_AI_QUERY)) return null

        val query = parsed.paramOrNull(0) ?: return null
        val context = parsed.paramOrNull(1)

        return Triple(parsed.id, query, context)
    }

    /**
     * Parse speech-to-text message.
     *
     * @return Quadruple of (sessionId, transcript, confidence, isFinal) or null
     */
    fun parseSpeechToText(message: String): SpeechToTextResult? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_SPEECH_TO_TEXT)) return null

        val transcript = parsed.paramOrNull(0) ?: return null
        val confidence = parsed.paramOrNull(1)?.toFloatOrNull() ?: 0f
        val isFinal = parsed.paramOrNull(2)?.toBooleanStrictOrNull() ?: true

        return SpeechToTextResult(parsed.id, transcript, confidence, isFinal)
    }

    /**
     * Speech-to-text parse result.
     */
    data class SpeechToTextResult(
        val sessionId: String,
        val transcript: String,
        val confidence: Float,
        val isFinal: Boolean
    )

    // ════════════════════════════════════════════════════════════════════════
    // PLUGIN MANIFEST PARSING (AVU Plugin Format v1.0)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Parse a complete plugin manifest from AVU format string.
     *
     * @param content AVU format manifest content
     * @return Parsed PluginManifest or null if invalid
     */
    fun parsePluginManifest(content: String): PluginManifest? {
        if (content.isBlank()) return null

        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        var manifest = PluginManifest()
        var inConfigBlock = false
        val configKeys = mutableListOf<ConfigKey>()

        for (line in lines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex < 0) continue

            val code = line.substring(0, colonIndex).uppercase()
            val value = line.substring(colonIndex + 1)

            when {
                code == AVUEncoder.CODE_CONFIG && value == "start" -> {
                    inConfigBlock = true
                }
                code == AVUEncoder.CODE_CONFIG && value == "end" -> {
                    inConfigBlock = false
                    manifest = manifest.copy(configSchema = configKeys.toList())
                }
                inConfigBlock && code == AVUEncoder.CODE_CONFIG_KEY -> {
                    parseConfigKey(value)?.let { configKeys.add(it) }
                }
                else -> {
                    manifest = parseManifestLine(manifest, code, value)
                }
            }
        }

        // Validate required fields
        if (manifest.id.isBlank() || manifest.version.isBlank()) {
            return null
        }

        return manifest
    }

    private fun parseManifestLine(manifest: PluginManifest, code: String, value: String): PluginManifest {
        return when (code) {
            AVUEncoder.CODE_PLUGIN -> {
                val parts = splitUnescaped(value)
                manifest.copy(
                    id = AVUEncoder.unescape(parts.getOrNull(0) ?: ""),
                    version = AVUEncoder.unescape(parts.getOrNull(1) ?: ""),
                    entrypoint = AVUEncoder.unescape(parts.getOrNull(2) ?: ""),
                    name = AVUEncoder.unescape(parts.getOrNull(3) ?: "")
                )
            }
            AVUEncoder.CODE_DESCRIPTION -> {
                manifest.copy(description = AVUEncoder.unescape(value))
            }
            AVUEncoder.CODE_AUTHOR -> {
                val parts = splitUnescaped(value)
                manifest.copy(
                    author = Author(
                        name = AVUEncoder.unescape(parts.getOrNull(0) ?: ""),
                        email = AVUEncoder.unescape(parts.getOrNull(1) ?: ""),
                        url = AVUEncoder.unescape(parts.getOrNull(2) ?: "")
                    )
                )
            }
            AVUEncoder.CODE_PLUGIN_CAP -> {
                val capabilities = value.split("|").map { AVUEncoder.unescape(it) }
                manifest.copy(capabilities = capabilities.toSet())
            }
            AVUEncoder.CODE_MODULE -> {
                val modules = value.split("|").map { AVUEncoder.unescape(it) }
                manifest.copy(targetModules = modules.toSet())
            }
            AVUEncoder.CODE_DEPENDENCY -> {
                val parts = splitUnescaped(value)
                val dep = Dependency(
                    pluginId = AVUEncoder.unescape(parts.getOrNull(0) ?: ""),
                    versionConstraint = AVUEncoder.unescape(parts.getOrElse(1) { "*" })
                )
                manifest.copy(dependencies = manifest.dependencies + dep)
            }
            AVUEncoder.CODE_PERMISSION -> {
                val parts = splitUnescaped(value)
                val perm = Permission(
                    name = AVUEncoder.unescape(parts.getOrNull(0) ?: ""),
                    rationale = AVUEncoder.unescape(parts.getOrElse(1) { "" })
                )
                manifest.copy(permissions = manifest.permissions + perm)
            }
            AVUEncoder.CODE_PLATFORM -> {
                val parts = splitUnescaped(value)
                val platform = parts.getOrNull(0) ?: ""
                val minVersion = parts.getOrElse(1) { "any" }
                manifest.copy(platforms = manifest.platforms + (platform to minVersion))
            }
            AVUEncoder.CODE_ASSET -> {
                val parts = splitUnescaped(value)
                val asset = Asset(
                    type = AVUEncoder.unescape(parts.getOrNull(0) ?: ""),
                    path = AVUEncoder.unescape(parts.getOrElse(1) { "" })
                )
                manifest.copy(assets = manifest.assets + asset)
            }
            AVUEncoder.CODE_HOOK -> {
                val parts = splitUnescaped(value)
                val event = AVUEncoder.unescape(parts.getOrNull(0) ?: "")
                val handler = AVUEncoder.unescape(parts.getOrElse(1) { "" })
                manifest.copy(hooks = manifest.hooks + (event to handler))
            }
            else -> manifest
        }
    }

    private fun parseConfigKey(value: String): ConfigKey? {
        val parts = splitUnescaped(value)
        if (parts.isEmpty()) return null
        return ConfigKey(
            name = AVUEncoder.unescape(parts.getOrNull(0) ?: ""),
            type = AVUEncoder.unescape(parts.getOrElse(1) { "string" }),
            default = AVUEncoder.unescape(parts.getOrElse(2) { "" }),
            description = AVUEncoder.unescape(parts.getOrElse(3) { "" })
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
     * Check if content is a valid plugin manifest.
     */
    fun isPluginManifest(content: String): Boolean {
        return content.lines().any { line ->
            val trimmed = line.trim()
            trimmed.startsWith("${AVUEncoder.CODE_PLUGIN}:") ||
                trimmed.contains("Avanues Universal Plugin Format")
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // APP CATEGORY DATABASE PARSING (ACD Format v1.0)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Parse a complete app category database from ACD format string.
     *
     * @param content ACD format content
     * @return Parsed AppCategoryDatabase or null if invalid
     */
    fun parseAppCategoryDatabase(content: String): AppCategoryDatabase? {
        if (content.isBlank()) return null

        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("---") }

        var version = "1.0.0"
        var timestamp = 0L
        var author = ""
        val entries = mutableListOf<AppCategoryEntry>()
        val patternGroups = mutableListOf<AppPatternGroup>()

        // Parse YAML-like metadata section
        var inMetadata = false
        for (line in lines) {
            when {
                line.startsWith("schema:") -> continue
                line.startsWith("version:") && !inMetadata -> {
                    version = line.substringAfter("version:").trim()
                }
                line.startsWith("metadata:") -> inMetadata = true
                line.startsWith("project:") || line.startsWith("  ") -> continue
                else -> {
                    inMetadata = false
                    val colonIndex = line.indexOf(':')
                    if (colonIndex < 0) continue

                    val code = line.substring(0, colonIndex).uppercase()
                    val value = line.substring(colonIndex + 1)

                    when (code) {
                        AVUEncoder.CODE_APP_CATEGORY_DB -> {
                            parseAppCategoryHeader(value)?.let { header ->
                                version = header.version
                                timestamp = header.timestamp
                                author = header.author
                            }
                        }
                        AVUEncoder.CODE_APP_PKG_CATEGORY -> {
                            parseAppPackageCategory(value)?.let { entries.add(it) }
                        }
                        AVUEncoder.CODE_APP_PATTERN_GROUP -> {
                            parseAppPatternGroup(value)?.let { patternGroups.add(it) }
                        }
                    }
                }
            }
        }

        // Validate we have some content
        if (entries.isEmpty() && patternGroups.isEmpty()) {
            return null
        }

        return AppCategoryDatabase(
            version = version,
            timestamp = timestamp,
            author = author,
            entries = entries,
            patternGroups = patternGroups
        )
    }

    /**
     * Parse ACD header line.
     * Format: version:timestamp:author
     */
    private fun parseAppCategoryHeader(value: String): AppCategoryDatabaseHeader? {
        val parts = splitUnescaped(value)
        if (parts.isEmpty()) return null
        return AppCategoryDatabaseHeader(
            version = AVUEncoder.unescape(parts.getOrNull(0) ?: "1.0.0"),
            timestamp = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
            author = AVUEncoder.unescape(parts.getOrElse(2) { "" })
        )
    }

    /**
     * Parse APC (App Package Category) line.
     * Format: packageName:category:source:confidence
     */
    private fun parseAppPackageCategory(value: String): AppCategoryEntry? {
        val parts = splitUnescaped(value)
        if (parts.size < 2) return null
        return AppCategoryEntry(
            packageName = AVUEncoder.unescape(parts[0]),
            category = AVUEncoder.unescape(parts[1]),
            source = AVUEncoder.unescape(parts.getOrElse(2) { "system" }),
            confidence = parts.getOrNull(3)?.toFloatOrNull() ?: 0.90f
        )
    }

    /**
     * Parse APG (App Pattern Group) line.
     * Format: category:pattern1|pattern2|pattern3
     */
    private fun parseAppPatternGroup(value: String): AppPatternGroup? {
        val parts = splitUnescaped(value)
        if (parts.size < 2) return null
        val category = AVUEncoder.unescape(parts[0])
        val patterns = parts[1].split("|").map { AVUEncoder.unescape(it) }
        if (patterns.isEmpty()) return null
        return AppPatternGroup(
            category = category,
            patterns = patterns
        )
    }

    /**
     * Check if content is a valid app category database.
     */
    fun isAppCategoryDatabase(content: String): Boolean {
        return content.lines().any { line ->
            val trimmed = line.trim()
            trimmed.startsWith("${AVUEncoder.CODE_APP_CATEGORY_DB}:") ||
                trimmed.contains("AppCategoryDatabase") ||
                trimmed.startsWith("${AVUEncoder.CODE_APP_PKG_CATEGORY}:")
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // APP CATEGORY DATABASE DATA CLASSES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Parsed app category database.
     */
    data class AppCategoryDatabase(
        val version: String = "1.0.0",
        val timestamp: Long = 0L,
        val author: String = "",
        val entries: List<AppCategoryEntry> = emptyList(),
        val patternGroups: List<AppPatternGroup> = emptyList()
    ) {
        /**
         * Get category for a package name (exact match only).
         */
        fun getCategoryForPackage(packageName: String): AppCategoryEntry? {
            return entries.find { it.packageName == packageName }
        }

        /**
         * Get category using pattern matching fallback.
         */
        fun getCategoryByPattern(packageName: String): String? {
            val lowerPackage = packageName.lowercase()
            for (group in patternGroups) {
                if (group.patterns.any { lowerPackage.contains(it.lowercase()) }) {
                    return group.category
                }
            }
            return null
        }

        /**
         * Get all entries for a specific category.
         */
        fun getEntriesForCategory(category: String): List<AppCategoryEntry> {
            return entries.filter { it.category.equals(category, ignoreCase = true) }
        }
    }

    /**
     * ACD header data.
     */
    data class AppCategoryDatabaseHeader(
        val version: String,
        val timestamp: Long,
        val author: String
    )

    /**
     * Single app category entry from APC line.
     */
    data class AppCategoryEntry(
        val packageName: String,
        val category: String,
        val source: String = "system",
        val confidence: Float = 0.90f
    )

    /**
     * Pattern group from APG line.
     */
    data class AppPatternGroup(
        val category: String,
        val patterns: List<String>
    )

    // ════════════════════════════════════════════════════════════════════════
    // PLUGIN MANIFEST DATA CLASSES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Parsed plugin manifest.
     */
    data class PluginManifest(
        val id: String = "",
        val version: String = "",
        val entrypoint: String = "",
        val name: String = "",
        val description: String = "",
        val author: Author = Author(),
        val capabilities: Set<String> = emptySet(),
        val targetModules: Set<String> = emptySet(),
        val dependencies: List<Dependency> = emptyList(),
        val permissions: List<Permission> = emptyList(),
        val platforms: Map<String, String> = emptyMap(),
        val assets: List<Asset> = emptyList(),
        val configSchema: List<ConfigKey> = emptyList(),
        val hooks: Map<String, String> = emptyMap()
    )

    /**
     * Plugin author info.
     */
    data class Author(
        val name: String = "",
        val email: String = "",
        val url: String = ""
    )

    /**
     * Plugin dependency.
     */
    data class Dependency(
        val pluginId: String,
        val versionConstraint: String = "*"
    )

    /**
     * Plugin permission.
     */
    data class Permission(
        val name: String,
        val rationale: String = ""
    )

    /**
     * Plugin asset.
     */
    data class Asset(
        val type: String,
        val path: String
    )

    /**
     * Plugin configuration key.
     */
    data class ConfigKey(
        val name: String,
        val type: String,
        val default: String = "",
        val description: String = ""
    )
}
