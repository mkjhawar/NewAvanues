package com.augmentalis.magiccode.plugins.platform

import com.avanues.avu.dsl.plugin.LoadedPlugin
import com.avanues.avu.dsl.plugin.PluginLoadResult
import com.avanues.avu.dsl.plugin.PluginLoader
import com.augmentalis.magiccode.plugins.core.PluginLog
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.NSFileManager

/**
 * iOS implementation of PluginClassLoader for .avp text plugins.
 *
 * Reads .avp plugin files bundled with the iOS app (or in the app's Documents directory
 * for user-installed plugins) and interprets them via the AVU DSL pipeline.
 *
 * Unlike the previous static registry approach, this implementation works with any
 * .avp file path — whether bundled in the app bundle or placed in the Documents/
 * directory at runtime. All plugin behaviour is defined in the .avp text; no compiled
 * Swift or Kotlin code is required.
 *
 * ## Loading pipeline
 * 1. Read .avp file text from [pluginPath] via NSFileManager
 * 2. Run through [PluginLoader.load] (AvuDslLexer → AvuDslParser → manifest extraction → validation)
 * 3. Store the resulting [LoadedPlugin] keyed by [entrypoint]
 */
actual class PluginClassLoader {

    private val loadedPlugins = mutableMapOf<String, LoadedPlugin>()

    companion object {
        private const val TAG = "IosAvpPluginClassLoader"
    }

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
        PluginLog.d(TAG, "Loading .avp plugin: $entrypoint from $pluginPath")

        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(pluginPath)) {
            throw ClassNotFoundException(
                "AVP plugin file not found: $pluginPath (plugin: $entrypoint)"
            )
        }

        val avpContent: String = try {
            val data = NSData.dataWithContentsOfFile(pluginPath)
                ?: throw ClassNotFoundException(
                    "Failed to read data from .avp file: $pluginPath"
                )
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
                ?: throw ClassNotFoundException(
                    "Failed to decode .avp file as UTF-8: $pluginPath"
                )
        } catch (e: ClassNotFoundException) {
            throw e
        } catch (e: Exception) {
            PluginLog.e(TAG, "IO error reading .avp file: $pluginPath", e)
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
                PluginLog.i(TAG, "Successfully parsed .avp plugin: ${plugin.pluginId} v${plugin.manifest.version}")
                plugin
            }
            is PluginLoadResult.ParseError -> {
                val errors = loadResult.errors.joinToString("; ")
                PluginLog.e(TAG, "AVP parse errors for $entrypoint: $errors")
                throw IllegalArgumentException(
                    "AVP parse errors in plugin '$entrypoint': $errors"
                )
            }
            is PluginLoadResult.ValidationError -> {
                val errors = loadResult.errors.joinToString("; ")
                PluginLog.e(TAG, "AVP validation errors for $entrypoint: $errors")
                throw IllegalArgumentException(
                    "AVP validation errors in plugin '$entrypoint': $errors"
                )
            }
            is PluginLoadResult.PermissionError -> {
                val errors = loadResult.errors.joinToString("; ")
                PluginLog.e(TAG, "AVP permission errors for $entrypoint: $errors")
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
        PluginLog.d(TAG, "iOS AVP plugin loader cleared")
    }
}
