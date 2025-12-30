package com.augmentalis.magiccode.plugins.platform

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlin.reflect.KClass

/**
 * iOS implementation of PluginClassLoader.
 *
 * ## iOS Plugin Architecture
 *
 * iOS enforces strict security restrictions that prevent runtime code loading.
 * This implementation provides three strategies for iOS plugin support:
 *
 * ### Strategy 1: Pre-Bundled Plugins (Production Recommended)
 * Plugins are compiled into the main app bundle as frameworks during build time.
 * Pros: Fully supported, performant, App Store compliant
 * Cons: Cannot load plugins after app release
 *
 * ### Strategy 2: Plugin Registry Pattern (Current Implementation)
 * Plugins register themselves at app startup via static initialization.
 * The PluginClassLoader acts as a lookup registry rather than a dynamic loader.
 * Pros: Simple, no reflection needed, type-safe
 * Cons: All plugins must be known at compile time
 *
 * ### Strategy 3: Interpreted/Scripting Plugins (Future)
 * Use embedded scripting engines (e.g., JavaScript via JavaScriptCore).
 * Pros: True runtime extensibility
 * Cons: Limited to script-based plugins, performance overhead
 *
 * ## Usage
 *
 * ### For Plugin Developers (iOS):
 * ```kotlin
 * // In plugin module, register during static initialization
 * @OptIn(ExperimentalStdlibApi::class)
 * @EagerInitialization
 * private val pluginRegistration = IosPluginClassLoader.register(
 *     className = "com.example.MyPlugin",
 *     factory = { MyPlugin() }
 * )
 * ```
 *
 * ### For App Developers:
 * ```kotlin
 * // At app startup, ensure all plugin modules are linked
 * val loader = IosPluginClassLoader()
 * val plugin = loader.loadClass("com.example.MyPlugin", "")
 * ```
 *
 * @see IosPluginRegistry
 * @since 1.0.0
 */
actual class PluginClassLoader {
    companion object {
        private const val TAG = "IosPluginClassLoader"

        /**
         * Global plugin registry for iOS pre-bundled plugins.
         *
         * Plugins register themselves here during static initialization.
         * This is the iOS-specific approach to "loading" plugins without
         * dynamic code loading capabilities.
         */
        private val pluginRegistry = mutableMapOf<String, () -> Any>()

        /**
         * Register a pre-bundled iOS plugin.
         *
         * Call this from your plugin module's static initialization block
         * to register the plugin factory.
         *
         * ## Example
         * ```kotlin
         * @OptIn(ExperimentalStdlibApi::class)
         * @EagerInitialization
         * private val registration = IosPluginClassLoader.register(
         *     className = "com.example.ImageFilterPlugin",
         *     factory = { ImageFilterPlugin() }
         * )
         * ```
         *
         * @param className Fully qualified class name (must match manifest entrypoint)
         * @param factory Lambda that creates new plugin instance
         */
        fun register(className: String, factory: () -> Any) {
            PluginLog.d(TAG, "Registering iOS plugin: $className")
            pluginRegistry[className] = factory
        }

        /**
         * Check if a plugin class is registered.
         *
         * @param className Fully qualified class name
         * @return true if plugin is registered and can be loaded
         */
        fun isRegistered(className: String): Boolean {
            return pluginRegistry.containsKey(className)
        }

        /**
         * Get all registered plugin class names.
         *
         * Useful for debugging and discovering available plugins at runtime.
         *
         * @return Set of registered fully qualified class names
         */
        fun getRegisteredPlugins(): Set<String> {
            return pluginRegistry.keys.toSet()
        }

        /**
         * Clear all plugin registrations.
         *
         * Warning: This will prevent plugins from being loaded.
         * Only use for testing or cleanup scenarios.
         */
        fun clearRegistry() {
            PluginLog.w(TAG, "Clearing iOS plugin registry")
            pluginRegistry.clear()
        }
    }

    /**
     * Load a class from the iOS plugin registry.
     *
     * Unlike Android/JVM, this doesn't load code from disk. Instead,
     * it looks up a pre-registered factory function and invokes it
     * to create a new plugin instance.
     *
     * ## Important
     * The `pluginPath` parameter is **ignored on iOS** since all plugins
     * must be pre-bundled. It's kept for interface compatibility with
     * Android/JVM implementations.
     *
     * @param className Fully qualified class name (must be pre-registered)
     * @param pluginPath Ignored on iOS (kept for cross-platform compatibility)
     * @return New instance of the plugin class
     * @throws ClassNotFoundException if plugin not registered
     * @throws IllegalStateException if plugin factory fails
     */
    actual fun loadClass(className: String, pluginPath: String): Any {
        PluginLog.d(TAG, "Loading iOS plugin: $className")

        // Note: pluginPath is intentionally ignored on iOS
        // All plugins must be pre-bundled and registered

        val factory = pluginRegistry[className]
            ?: throw ClassNotFoundException(
                "Plugin not registered: $className. " +
                "iOS plugins must call IosPluginClassLoader.register() during static initialization. " +
                "Registered plugins: ${pluginRegistry.keys.joinToString()}"
            )

        return try {
            factory()
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to instantiate iOS plugin: $className", e)
            throw IllegalStateException("Plugin factory failed for $className", e)
        }
    }

    /**
     * Unload plugin classes.
     *
     * On iOS, this is a no-op since plugins are loaded as part of the
     * main app bundle and cannot be unloaded at runtime. The iOS
     * memory management system will handle cleanup when plugin
     * instances are no longer referenced.
     */
    actual fun unload() {
        // No-op for iOS - plugins are part of main app bundle
        // and cannot be unloaded. Memory will be reclaimed by ARC
        // when plugin instances are no longer referenced.
        PluginLog.d(TAG, "Unload called (no-op on iOS)")
    }
}

/**
 * Helper annotation for iOS plugin registration.
 *
 * Apply this to a top-level property in your iOS plugin module
 * to ensure the registration code runs at app startup.
 *
 * ## Example
 * ```kotlin
 * @IosPluginRegistration
 * @OptIn(ExperimentalStdlibApi::class)
 * @EagerInitialization
 * private val register = IosPluginClassLoader.register(
 *     className = "com.example.MyPlugin",
 *     factory = { MyPlugin() }
 * )
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class IosPluginRegistration
