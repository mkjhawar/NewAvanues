package com.augmentalis.magiccode.plugins.platform

/**
 * Platform-specific plugin class loader.
 *
 * Loads plugin code from JAR/APK files with proper isolation.
 * Implementations vary by platform (Android DexClassLoader, iOS dynamic frameworks, JVM URLClassLoader).
 */
expect class PluginClassLoader {
    /**
     * Load a class from the plugin.
     *
     * @param className Fully qualified class name
     * @param pluginPath Path to plugin library (JAR/APK/Framework)
     * @return Loaded class instance
     * @throws ClassNotFoundException if class cannot be found
     */
    fun loadClass(className: String, pluginPath: String): Any

    /**
     * Unload plugin classes and release resources.
     */
    fun unload()
}
