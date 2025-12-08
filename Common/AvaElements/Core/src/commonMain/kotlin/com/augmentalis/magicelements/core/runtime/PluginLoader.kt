package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.*
import com.augmentalis.magicelements.core.mel.*

/**
 * Plugin Loader
 *
 * Responsible for loading plugins from various sources and parsing them.
 * Supports YAML, JSON, Kotlin DSL, and MEL (MagicUI Expression Language) formats.
 */
class PluginLoader {

    /**
     * Load a plugin from a source
     *
     * @param source Plugin source
     * @return Loaded plugin instance
     * @throws PluginException.LoadException if loading fails
     */
    suspend fun load(source: PluginSource): MagicElementPlugin {
        return when (source) {
            is PluginSource.File -> loadFromFile(source.path)
            is PluginSource.Data -> loadFromData(source.content, source.format)
            is PluginSource.Remote -> loadFromRemote(source.url)
        }
    }

    /**
     * Load a MEL-based plugin and return a PluginRuntime.
     *
     * This is the new entry point for dual-tier plugins using MagicUI Expression Language.
     *
     * @param source Plugin source (YAML or JSON)
     * @param platform Target platform (auto-detected if not specified)
     * @return Initialized PluginRuntime
     * @throws PluginException.LoadException if loading or initialization fails
     */
    suspend fun loadMELPlugin(source: PluginSource, platform: Platform? = null): PluginRuntime {
        return try {
            when (source) {
                is PluginSource.File -> {
                    val content = expect_loadFileContent(source.path)
                    val format = detectFormat(source.path, content)
                    parseMELPlugin(content, format, platform)
                }
                is PluginSource.Data -> {
                    val format = when (source.format) {
                        PluginSource.Data.Format.YAML -> PluginSourceFormat.YAML
                        PluginSource.Data.Format.JSON -> PluginSourceFormat.JSON
                        PluginSource.Data.Format.KOTLIN -> throw PluginException.LoadException(
                            "Kotlin DSL is not supported for MEL plugins"
                        )
                    }
                    parseMELPlugin(source.content, format, platform)
                }
                is PluginSource.Remote -> {
                    val content = expect_loadRemoteContent(source.url)
                    val format = detectFormat(source.url, content)
                    parseMELPlugin(content, format, platform)
                }
            }
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.LoadException("Failed to load MEL plugin: ${e.message}", e)
        }
    }

    /**
     * Parse MEL plugin definition and create runtime.
     */
    private fun parseMELPlugin(
        content: String,
        format: PluginSourceFormat,
        platform: Platform?
    ): PluginRuntime {
        val parser = PluginDefinitionParser()

        val definition = when (format) {
            PluginSourceFormat.JSON -> parser.parseJson(content).getOrElse { error ->
                throw PluginException.LoadException("Failed to parse JSON plugin: ${error.message}", error)
            }
            PluginSourceFormat.YAML -> {
                // Convert YAML to JSON first using YamlPluginParser
                val yamlParser = YamlPluginParser()
                val jsonContent = yamlParser.toJson(content)
                parser.parseJson(jsonContent).getOrElse { error ->
                    throw PluginException.LoadException("Failed to parse YAML plugin: ${error.message}", error)
                }
            }
        }

        return PluginRuntime.create(definition, platform)
    }

    /**
     * Detect plugin format from file extension or content.
     */
    private fun detectFormat(path: String, content: String): PluginSourceFormat {
        return when {
            path.endsWith(".yaml") || path.endsWith(".yml") -> PluginSourceFormat.YAML
            path.endsWith(".json") -> PluginSourceFormat.JSON
            content.trimStart().startsWith("{") -> PluginSourceFormat.JSON
            else -> PluginSourceFormat.YAML // Default to YAML
        }
    }

    /**
     * Load plugin from file path
     */
    private suspend fun loadFromFile(path: String): MagicElementPlugin {
        // Platform-specific file loading
        return expect_loadPluginFromFile(path)
    }

    /**
     * Load plugin from data string
     */
    private suspend fun loadFromData(content: String, format: PluginSource.Data.Format): MagicElementPlugin {
        return when (format) {
            PluginSource.Data.Format.YAML -> parseYamlPlugin(content)
            PluginSource.Data.Format.JSON -> parseJsonPlugin(content)
            PluginSource.Data.Format.KOTLIN -> parseKotlinPlugin(content)
        }
    }

    /**
     * Load plugin from remote URL
     */
    private suspend fun loadFromRemote(url: String): MagicElementPlugin {
        // Platform-specific remote loading
        return expect_loadPluginFromRemote(url)
    }

    /**
     * Parse YAML plugin definition
     */
    private fun parseYamlPlugin(yaml: String): MagicElementPlugin {
        val parser = YamlPluginParser()
        return parser.parse(yaml).getOrThrow()
    }

    /**
     * Parse JSON plugin definition
     */
    private fun parseJsonPlugin(data: String): MagicElementPlugin {
        val parser = JsonPluginParser()
        return parser.parse(data).getOrThrow()
    }

    /**
     * Parse Kotlin DSL plugin
     */
    private fun parseKotlinPlugin(kotlin: String): MagicElementPlugin {
        // TODO: Implement Kotlin script evaluation (sandboxed)
        throw NotImplementedError("Kotlin plugin parsing not yet implemented")
    }
}

/**
 * Platform-specific plugin loading from file
 *
 * Each platform must provide implementation via expect/actual
 */
expect suspend fun expect_loadPluginFromFile(path: String): MagicElementPlugin

/**
 * Platform-specific plugin loading from remote URL
 *
 * Each platform must provide implementation via expect/actual
 */
expect suspend fun expect_loadPluginFromRemote(url: String): MagicElementPlugin

/**
 * Platform-specific file content loading
 *
 * Each platform must provide implementation via expect/actual
 */
expect suspend fun expect_loadFileContent(path: String): String

/**
 * Platform-specific remote content loading
 *
 * Each platform must provide implementation via expect/actual
 */
expect suspend fun expect_loadRemoteContent(url: String): String
