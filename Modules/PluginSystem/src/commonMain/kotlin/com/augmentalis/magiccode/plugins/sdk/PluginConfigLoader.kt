/**
 * PluginConfigLoader.kt - AVU config file loading utility
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides utilities for loading plugin configuration from various sources
 * including AVU format files, JSON, and maps.
 */
package com.augmentalis.magiccode.plugins.sdk

import com.augmentalis.magiccode.plugins.universal.PluginConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Utility for loading plugin configuration from various sources.
 *
 * Supports loading configuration from:
 * - AVU format (key|value lines)
 * - JSON format
 * - Map<String, Any> structures
 *
 * ## AVU Format
 * The AVU (Augmentalis Value Unit) format uses pipe-delimited lines:
 * ```
 * setting|model=gpt-4
 * setting|maxTokens=1000
 * secret|apiKey=sk-xxxxx
 * feature|streaming
 * feature|function-calling
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * val loader = PluginConfigLoader()
 *
 * // Load from AVU string
 * val config = loader.loadFromAvu("""
 *     setting|model=gpt-4
 *     setting|timeout=30000
 *     secret|apiKey=sk-xxxxx
 *     feature|streaming
 * """.trimIndent())
 *
 * // Load from JSON
 * val jsonConfig = loader.loadFromJson("""
 *     {
 *         "settings": { "model": "gpt-4" },
 *         "secrets": { "apiKey": "sk-xxxxx" },
 *         "features": ["streaming"]
 *     }
 * """)
 * ```
 *
 * @since 1.0.0
 * @see PluginConfig
 */
class PluginConfigLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // =========================================================================
    // AVU Loading
    // =========================================================================

    /**
     * Load configuration from AVU format string.
     *
     * AVU format uses pipe-delimited lines with type prefixes:
     * - `setting|key=value` - Configuration setting
     * - `secret|key=value` - Secret value (API keys, tokens)
     * - `feature|name` - Feature flag (enabled)
     * - Lines starting with `#` or empty lines are ignored
     *
     * @param content AVU format configuration string
     * @return Parsed PluginConfig
     * @throws IllegalArgumentException for malformed lines
     */
    fun loadFromAvu(content: String): PluginConfig {
        val settings = mutableMapOf<String, String>()
        val secrets = mutableMapOf<String, String>()
        val features = mutableSetOf<String>()

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                val parsed = parseAvuLine(line)
                if (parsed != null) {
                    val (type, value) = parsed
                    when (type.lowercase()) {
                        "setting", "s" -> {
                            val keyValue = parseKeyValue(value)
                            if (keyValue != null) {
                                settings[keyValue.first] = keyValue.second
                            }
                        }
                        "secret", "sec" -> {
                            val keyValue = parseKeyValue(value)
                            if (keyValue != null) {
                                secrets[keyValue.first] = keyValue.second
                            }
                        }
                        "feature", "f" -> {
                            features.add(value)
                        }
                    }
                }
            }

        return PluginConfig(
            settings = settings,
            secrets = secrets,
            features = features
        )
    }

    /**
     * Load configuration from AVU lines list.
     *
     * @param lines List of AVU format lines
     * @return Parsed PluginConfig
     */
    fun loadFromAvuLines(lines: List<String>): PluginConfig {
        return loadFromAvu(lines.joinToString("\n"))
    }

    // =========================================================================
    // Map Loading
    // =========================================================================

    /**
     * Load configuration from a map structure.
     *
     * Expected map format:
     * ```
     * {
     *     "settings": { "key": "value", ... },
     *     "secrets": { "key": "value", ... },
     *     "features": ["feature1", "feature2", ...]
     * }
     * ```
     *
     * Also supports flat map with prefixed keys:
     * ```
     * {
     *     "setting.model": "gpt-4",
     *     "secret.apiKey": "sk-xxx",
     *     "feature.streaming": "true"
     * }
     * ```
     *
     * @param map Configuration map
     * @return Parsed PluginConfig
     */
    fun loadFromMap(map: Map<String, Any>): PluginConfig {
        // Check for structured format first
        val structuredSettings = map["settings"]
        val structuredSecrets = map["secrets"]
        val structuredFeatures = map["features"]

        if (structuredSettings != null || structuredSecrets != null || structuredFeatures != null) {
            return loadFromStructuredMap(map)
        }

        // Fall back to flat format with prefixes
        return loadFromFlatMap(map)
    }

    private fun loadFromStructuredMap(map: Map<String, Any>): PluginConfig {
        val settings = mutableMapOf<String, String>()
        val secrets = mutableMapOf<String, String>()
        val features = mutableSetOf<String>()

        // Extract settings
        @Suppress("UNCHECKED_CAST")
        (map["settings"] as? Map<String, Any>)?.forEach { (key, value) ->
            settings[key] = value.toString()
        }

        // Extract secrets
        @Suppress("UNCHECKED_CAST")
        (map["secrets"] as? Map<String, Any>)?.forEach { (key, value) ->
            secrets[key] = value.toString()
        }

        // Extract features (can be list or set)
        when (val featuresVal = map["features"]) {
            is Collection<*> -> featuresVal.filterNotNull().forEach { features.add(it.toString()) }
            is Array<*> -> featuresVal.filterNotNull().forEach { features.add(it.toString()) }
        }

        return PluginConfig(
            settings = settings,
            secrets = secrets,
            features = features
        )
    }

    private fun loadFromFlatMap(map: Map<String, Any>): PluginConfig {
        val settings = mutableMapOf<String, String>()
        val secrets = mutableMapOf<String, String>()
        val features = mutableSetOf<String>()

        map.forEach { (key, value) ->
            when {
                key.startsWith("setting.") -> {
                    settings[key.removePrefix("setting.")] = value.toString()
                }
                key.startsWith("secret.") -> {
                    secrets[key.removePrefix("secret.")] = value.toString()
                }
                key.startsWith("feature.") -> {
                    val featureName = key.removePrefix("feature.")
                    if (value.toString().toBooleanStrictOrNull() != false) {
                        features.add(featureName)
                    }
                }
                else -> {
                    // Default to setting
                    settings[key] = value.toString()
                }
            }
        }

        return PluginConfig(
            settings = settings,
            secrets = secrets,
            features = features
        )
    }

    // =========================================================================
    // JSON Loading
    // =========================================================================

    /**
     * Load configuration from JSON string.
     *
     * Supports two JSON formats:
     *
     * Structured format:
     * ```json
     * {
     *     "settings": { "model": "gpt-4", "timeout": "30000" },
     *     "secrets": { "apiKey": "sk-xxxxx" },
     *     "features": ["streaming", "function-calling"]
     * }
     * ```
     *
     * Flat format (all values as settings):
     * ```json
     * {
     *     "model": "gpt-4",
     *     "timeout": "30000"
     * }
     * ```
     *
     * @param jsonString JSON configuration string
     * @return Parsed PluginConfig
     * @throws kotlinx.serialization.SerializationException for invalid JSON
     */
    fun loadFromJson(jsonString: String): PluginConfig {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        return loadFromJsonObject(jsonObject)
    }

    private fun loadFromJsonObject(jsonObject: JsonObject): PluginConfig {
        val settings = mutableMapOf<String, String>()
        val secrets = mutableMapOf<String, String>()
        val features = mutableSetOf<String>()

        // Check for structured format
        jsonObject["settings"]?.jsonObject?.forEach { (key, value) ->
            settings[key] = value.jsonPrimitive.content
        }

        jsonObject["secrets"]?.jsonObject?.forEach { (key, value) ->
            secrets[key] = value.jsonPrimitive.content
        }

        jsonObject["features"]?.jsonArray?.forEach { element ->
            features.add(element.jsonPrimitive.content)
        }

        // If no structured keys found, treat all as settings
        if (settings.isEmpty() && secrets.isEmpty() && features.isEmpty()) {
            jsonObject.forEach { (key, value) ->
                if (key !in setOf("settings", "secrets", "features")) {
                    settings[key] = value.jsonPrimitive.content
                }
            }
        }

        return PluginConfig(
            settings = settings,
            secrets = secrets,
            features = features
        )
    }

    // =========================================================================
    // Merging
    // =========================================================================

    /**
     * Merge multiple configurations.
     *
     * Later configurations override earlier ones for settings and secrets.
     * Features are combined (union).
     *
     * @param configs Configurations to merge in order
     * @return Merged configuration
     */
    fun merge(vararg configs: PluginConfig): PluginConfig {
        if (configs.isEmpty()) return PluginConfig.EMPTY
        if (configs.size == 1) return configs[0]

        val settings = mutableMapOf<String, String>()
        val secrets = mutableMapOf<String, String>()
        val features = mutableSetOf<String>()

        configs.forEach { config ->
            settings.putAll(config.settings)
            secrets.putAll(config.secrets)
            features.addAll(config.features)
        }

        return PluginConfig(
            settings = settings,
            secrets = secrets,
            features = features
        )
    }

    // =========================================================================
    // Companion Object
    // =========================================================================

    companion object {
        /**
         * Shared loader instance for convenience.
         */
        val default = PluginConfigLoader()

        /**
         * Parse a single AVU format line.
         *
         * AVU lines use pipe (|) as delimiter:
         * - `type|value` where type is "setting", "secret", or "feature"
         *
         * @param line The line to parse
         * @return Pair of (type, value) or null if not parseable
         */
        fun parseAvuLine(line: String): Pair<String, String>? {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                return null
            }

            val pipeIndex = trimmed.indexOf('|')
            if (pipeIndex <= 0) {
                return null
            }

            val type = trimmed.substring(0, pipeIndex).trim()
            val value = trimmed.substring(pipeIndex + 1).trim()

            if (type.isEmpty() || value.isEmpty()) {
                return null
            }

            return type to value
        }

        /**
         * Parse a key=value string.
         *
         * @param keyValue String in format "key=value"
         * @return Pair of (key, value) or null if not parseable
         */
        fun parseKeyValue(keyValue: String): Pair<String, String>? {
            val equalsIndex = keyValue.indexOf('=')
            if (equalsIndex <= 0) {
                return null
            }

            val key = keyValue.substring(0, equalsIndex).trim()
            val value = keyValue.substring(equalsIndex + 1).trim()

            if (key.isEmpty()) {
                return null
            }

            return key to value
        }

        /**
         * Create a simple configuration from settings map.
         *
         * Convenience method for creating config with only settings.
         *
         * @param settings Settings map
         * @return PluginConfig with only settings
         */
        fun fromSettings(settings: Map<String, String>): PluginConfig {
            return PluginConfig.fromSettings(settings)
        }

        /**
         * Create an empty configuration.
         *
         * @return Empty PluginConfig
         */
        fun empty(): PluginConfig = PluginConfig.EMPTY
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Convert PluginConfig to AVU format string.
 *
 * @return AVU format representation of this config
 */
fun PluginConfig.toAvuString(): String {
    val lines = mutableListOf<String>()

    settings.forEach { (key, value) ->
        lines.add("setting|$key=$value")
    }

    secrets.forEach { (key, value) ->
        lines.add("secret|$key=$value")
    }

    features.forEach { feature ->
        lines.add("feature|$feature")
    }

    return lines.joinToString("\n")
}

/**
 * Convert PluginConfig to JSON string.
 *
 * @return JSON representation of this config
 */
fun PluginConfig.toJsonString(): String {
    val settingsJson = settings.entries.joinToString(", ") { (k, v) ->
        "\"$k\": \"$v\""
    }
    val secretsJson = secrets.entries.joinToString(", ") { (k, v) ->
        "\"$k\": \"$v\""
    }
    val featuresJson = features.joinToString(", ") { "\"$it\"" }

    return """
        {
            "settings": { $settingsJson },
            "secrets": { $secretsJson },
            "features": [ $featuresJson ]
        }
    """.trimIndent()
}

/**
 * Merge this configuration with another.
 *
 * The other configuration's values override this one's for settings and secrets.
 * Features are combined.
 *
 * @param other Configuration to merge with
 * @return Merged configuration
 */
fun PluginConfig.mergeWith(other: PluginConfig): PluginConfig {
    return PluginConfigLoader.default.merge(this, other)
}
