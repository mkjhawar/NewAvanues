package com.augmentalis.magiccode.plugins.platform

import com.avanues.avu.dsl.plugin.LoadedPlugin

/**
 * Platform-specific plugin class loader for .avp text plugins.
 *
 * Reads .avp plugin files as text, parses them through the AVU DSL pipeline
 * (AvuDslLexer → AvuDslParser → PluginLoader validation), and stores the
 * resulting LoadedPlugin AST for later execution by AvuInterpreter.
 *
 * No compiled DEX bytecode or class files are required. The .avp file IS the
 * entire plugin — interpreted at runtime.
 *
 * ## Supported file location
 * `pluginPath` passed to [loadClass] must be the absolute path to the `.avp` file.
 * The `entrypoint` parameter is used as the registry key (the plugin's declared
 * `plugin_id` from the .avp header must match it for ID verification to pass).
 */
expect class PluginClassLoader() {
    /**
     * Load and parse an .avp plugin file.
     *
     * Reads the .avp text from [pluginPath], runs it through the AVU DSL
     * lexer → parser → PluginLoader validation pipeline, and stores the
     * resulting [LoadedPlugin] keyed by [entrypoint] (the plugin ID).
     *
     * @param entrypoint Plugin ID declared in the .avp header (`plugin_id` field)
     * @param pluginPath Absolute path to the .avp plugin text file
     * @return The [LoadedPlugin] containing parsed AST and sandbox config (as Any for KMP compat)
     * @throws ClassNotFoundException if the file cannot be found or parsed
     * @throws IllegalArgumentException if the file fails AVU validation
     */
    fun loadClass(entrypoint: String, pluginPath: String): Any

    /**
     * Retrieve the parsed LoadedPlugin for a previously loaded plugin.
     *
     * @param pluginId The plugin ID (entrypoint passed to [loadClass])
     * @return The [LoadedPlugin] or null if not loaded
     */
    fun getLoadedPlugin(pluginId: String): LoadedPlugin?

    /**
     * Unload plugin and release associated resources.
     */
    fun unload()
}
