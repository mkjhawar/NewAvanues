package com.augmentalis.magiccode.plugins.platform

import com.avanues.avu.dsl.plugin.LoadedPlugin
import com.avanues.avu.dsl.plugin.PluginLoadResult
import com.avanues.avu.dsl.plugin.PluginLoader
import java.io.File

/**
 * JVM/Desktop implementation of PluginClassLoader for .avp text plugins.
 *
 * Replaces the former URLClassLoader (bytecode-based) approach with AVU DSL
 * text interpretation. .avp files are plain text parsed at runtime by the
 * AVU DSL pipeline — no compiled JAR or class files required.
 *
 * ## Loading pipeline
 * 1. Read .avp file text from [pluginPath]
 * 2. Run through [PluginLoader.load] (AvuDslLexer → AvuDslParser → manifest extraction → validation)
 * 3. Store the resulting [LoadedPlugin] keyed by [entrypoint]
 */
actual class PluginClassLoader {

    private val loadedPlugins = mutableMapOf<String, LoadedPlugin>()

    /**
     * Parse an .avp plugin file and store the resulting [LoadedPlugin].
     *
     * @param entrypoint Plugin ID expected in the .avp header `plugin_id` field
     * @param pluginPath Absolute path to the .avp text file
     * @return The [LoadedPlugin] containing the parsed AST and sandbox config
     * @throws ClassNotFoundException if the file is not found or cannot be read
     * @throws IllegalArgumentException if the .avp content fails AVU validation
     */
    actual fun loadClass(entrypoint: String, pluginPath: String): Any {
        val avpFile = File(pluginPath)
        if (!avpFile.exists()) {
            throw ClassNotFoundException(
                "AVP plugin file not found: $pluginPath (plugin: $entrypoint)"
            )
        }

        val avpContent = try {
            avpFile.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            throw ClassNotFoundException(
                "Failed to read .avp file for plugin $entrypoint: ${e.message}", e
            )
        }

        val loadResult = PluginLoader.load(avpContent)

        val loadedPlugin = when (loadResult) {
            is PluginLoadResult.Success -> {
                val plugin = loadResult.plugin
                if (plugin.manifest.pluginId != entrypoint) {
                    throw IllegalArgumentException(
                        "Plugin ID mismatch in .avp file: expected '$entrypoint', " +
                            "got '${plugin.manifest.pluginId}' in $pluginPath"
                    )
                }
                plugin
            }
            is PluginLoadResult.ParseError -> {
                val errors = loadResult.errors.joinToString("; ")
                throw IllegalArgumentException(
                    "AVP parse errors in plugin '$entrypoint': $errors"
                )
            }
            is PluginLoadResult.ValidationError -> {
                val errors = loadResult.errors.joinToString("; ")
                throw IllegalArgumentException(
                    "AVP validation errors in plugin '$entrypoint': $errors"
                )
            }
            is PluginLoadResult.PermissionError -> {
                val errors = loadResult.errors.joinToString("; ")
                throw IllegalArgumentException(
                    "AVP permission errors in plugin '$entrypoint': $errors"
                )
            }
        }

        loadedPlugins[entrypoint] = loadedPlugin
        return loadedPlugin
    }

    /**
     * Retrieve the parsed [LoadedPlugin] for a previously loaded plugin.
     *
     * @param pluginId The plugin ID (entrypoint passed to [loadClass])
     * @return The [LoadedPlugin] or null if not yet loaded
     */
    actual fun getLoadedPlugin(pluginId: String): LoadedPlugin? {
        return loadedPlugins[pluginId]
    }

    /**
     * Unload all parsed plugins and release memory.
     */
    actual fun unload() {
        loadedPlugins.clear()
    }
}
