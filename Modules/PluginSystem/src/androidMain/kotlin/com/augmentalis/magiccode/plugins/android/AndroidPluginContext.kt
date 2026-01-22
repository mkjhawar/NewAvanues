/**
 * AndroidPluginContext.kt - Android-specific plugin context implementation
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides Android-specific context and utilities for plugins, including
 * access to Android Context, SharedPreferences, and platform directories.
 */
package com.augmentalis.magiccode.plugins.android

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.augmentalis.magiccode.plugins.core.PluginLog
import com.augmentalis.magiccode.plugins.core.PluginLogger
import com.augmentalis.magiccode.plugins.universal.PluginEventBus
import com.augmentalis.magiccode.plugins.universal.PlatformInfo
import java.io.File

/**
 * Android-specific plugin context implementation.
 *
 * Provides plugins with access to Android platform resources, file system paths,
 * and services. Each plugin receives its own context instance with isolated
 * storage directories.
 *
 * ## Directory Structure
 * ```
 * /data/data/<app>/
 * ├── files/
 * │   └── plugins/
 * │       └── <plugin-id>/      # pluginDataDir
 * │           ├── data/         # Persistent plugin data
 * │           └── config/       # Plugin configuration
 * ├── cache/
 * │   └── plugins/
 * │       └── <plugin-id>/      # cacheDir - Temporary plugin data
 * └── shared/
 *     └── plugins/              # sharedDir - Inter-plugin data sharing
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * class MyPlugin : UniversalPlugin {
 *     private lateinit var context: AndroidPluginContext
 *
 *     override suspend fun initialize(config: PluginConfig, ctx: PluginContext): InitResult {
 *         context = ctx as AndroidPluginContext
 *
 *         // Access Android context
 *         val androidCtx = context.getAndroidContext()
 *
 *         // Use SharedPreferences
 *         val prefs = context.getSharedPreferences("my_settings")
 *         prefs.edit().putString("key", "value").apply()
 *
 *         // Access plugin directories
 *         val dataFile = File(context.pluginDataDir, "data.json")
 *
 *         return InitResult.Success()
 *     }
 * }
 * ```
 *
 * @property androidContext Android Context (typically Application context)
 * @property pluginDataDir Path to plugin's persistent data directory
 * @property cacheDir Path to plugin's cache directory (may be cleared by system)
 * @property sharedDir Path to shared directory for inter-plugin data exchange
 * @property logger Plugin logger instance
 * @property serviceRegistry Platform service registry
 * @property eventBus Plugin event bus for inter-plugin communication
 *
 * @since 1.0.0
 * @see com.augmentalis.magiccode.plugins.universal.PluginContext
 */
class AndroidPluginContext(
    private val androidContext: Context,
    val pluginDataDir: String,
    val cacheDir: String,
    val sharedDir: String,
    val logger: PluginLogger,
    val serviceRegistry: ServiceRegistry,
    val eventBus: PluginEventBus,
    val platformInfo: PlatformInfo = createAndroidPlatformInfo()
) {
    /**
     * Plugin ID this context belongs to.
     */
    val pluginId: String = extractPluginIdFromPath(pluginDataDir)

    /**
     * Get the underlying Android Context.
     *
     * Returns the Application context to prevent memory leaks.
     * For Activity-specific features, use getActivityContext() when available.
     *
     * @return Android Application Context
     */
    fun getAndroidContext(): Context = androidContext.applicationContext

    /**
     * Get SharedPreferences for this plugin.
     *
     * Creates a namespaced SharedPreferences file to avoid conflicts with
     * other plugins. The preferences file is stored in the app's default
     * SharedPreferences directory with a plugin-specific prefix.
     *
     * @param name Preferences file name (will be prefixed with plugin ID)
     * @param mode Operating mode (default: MODE_PRIVATE)
     * @return SharedPreferences instance for this plugin
     */
    fun getSharedPreferences(
        name: String,
        mode: Int = Context.MODE_PRIVATE
    ): SharedPreferences {
        val prefName = "plugin_${pluginId.replace('.', '_')}_$name"
        return androidContext.getSharedPreferences(prefName, mode)
    }

    /**
     * Get the default SharedPreferences for this plugin.
     *
     * Convenience method that returns preferences with the default name.
     *
     * @return Default SharedPreferences for this plugin
     */
    fun getDefaultPreferences(): SharedPreferences {
        return getSharedPreferences("default")
    }

    /**
     * Get a File reference to a file in the plugin data directory.
     *
     * Creates parent directories if they don't exist.
     *
     * @param path Relative path within the plugin data directory
     * @return File reference (may not exist yet)
     */
    fun getDataFile(path: String): File {
        val file = File(pluginDataDir, path)
        file.parentFile?.mkdirs()
        return file
    }

    /**
     * Get a File reference to a file in the plugin cache directory.
     *
     * Cache files may be deleted by the system when storage is low.
     * Creates parent directories if they don't exist.
     *
     * @param path Relative path within the cache directory
     * @return File reference (may not exist yet)
     */
    fun getCacheFile(path: String): File {
        val file = File(cacheDir, path)
        file.parentFile?.mkdirs()
        return file
    }

    /**
     * Get a File reference to a file in the shared directory.
     *
     * The shared directory allows inter-plugin data exchange.
     * Creates parent directories if they don't exist.
     *
     * @param path Relative path within the shared directory
     * @return File reference (may not exist yet)
     */
    fun getSharedFile(path: String): File {
        val file = File(sharedDir, path)
        file.parentFile?.mkdirs()
        return file
    }

    /**
     * Check if the plugin data directory exists and is accessible.
     *
     * @return true if the data directory exists and is writable
     */
    fun isDataDirectoryAccessible(): Boolean {
        val dir = File(pluginDataDir)
        return dir.exists() && dir.isDirectory && dir.canWrite()
    }

    /**
     * Get the total size of the plugin data directory in bytes.
     *
     * @return Total size in bytes, or -1 if directory doesn't exist
     */
    fun getDataDirectorySize(): Long {
        return calculateDirectorySize(File(pluginDataDir))
    }

    /**
     * Get the total size of the plugin cache directory in bytes.
     *
     * @return Total size in bytes, or -1 if directory doesn't exist
     */
    fun getCacheDirectorySize(): Long {
        return calculateDirectorySize(File(cacheDir))
    }

    /**
     * Clear the plugin cache directory.
     *
     * Deletes all files and subdirectories in the cache directory.
     * The cache directory itself is recreated after clearing.
     *
     * @return true if cache was successfully cleared
     */
    fun clearCache(): Boolean {
        return try {
            val cacheDir = File(cacheDir)
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            cacheDir.mkdirs()
            PluginLog.d(TAG, "Cleared cache for plugin: $pluginId")
            true
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to clear cache for plugin: $pluginId", e)
            false
        }
    }

    /**
     * Get a service from the service registry.
     *
     * Convenience method for accessing services from the context.
     *
     * @param T The expected service type
     * @param serviceId The service identifier
     * @return The service instance or null if not found
     */
    suspend inline fun <reified T : Any> getService(serviceId: String): T? {
        return serviceRegistry.get(serviceId)
    }

    /**
     * Check if a service is available.
     *
     * @param serviceId The service identifier
     * @return true if the service is registered
     */
    suspend fun hasService(serviceId: String): Boolean {
        return serviceRegistry.hasService(serviceId)
    }

    /**
     * Log a debug message using the plugin logger.
     *
     * @param message Message to log
     * @param throwable Optional exception
     */
    fun logDebug(message: String, throwable: Throwable? = null) {
        logger.debug(pluginId, message, throwable)
    }

    /**
     * Log an info message using the plugin logger.
     *
     * @param message Message to log
     * @param throwable Optional exception
     */
    fun logInfo(message: String, throwable: Throwable? = null) {
        logger.info(pluginId, message, throwable)
    }

    /**
     * Log a warning message using the plugin logger.
     *
     * @param message Message to log
     * @param throwable Optional exception
     */
    fun logWarn(message: String, throwable: Throwable? = null) {
        logger.warn(pluginId, message, throwable)
    }

    /**
     * Log an error message using the plugin logger.
     *
     * @param message Message to log
     * @param throwable Optional exception
     */
    fun logError(message: String, throwable: Throwable? = null) {
        logger.error(pluginId, message, throwable)
    }

    /**
     * Calculate total size of a directory recursively.
     */
    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return -1L
        return try {
            directory.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to calculate directory size: ${directory.path}", e)
            -1L
        }
    }

    /**
     * Extract plugin ID from the data directory path.
     */
    private fun extractPluginIdFromPath(path: String): String {
        // Path format: .../plugins/<plugin-id>/
        val segments = path.split(File.separator)
        val pluginsIndex = segments.indexOf("plugins")
        return if (pluginsIndex >= 0 && pluginsIndex < segments.lastIndex) {
            segments[pluginsIndex + 1]
        } else {
            "unknown"
        }
    }

    companion object {
        private const val TAG = "AndroidPluginContext"

        /**
         * Base directory name for plugin data within app files.
         */
        private const val PLUGINS_DIR = "plugins"

        /**
         * Base directory name for shared inter-plugin data.
         */
        private const val SHARED_DIR = "shared"

        /**
         * Create an AndroidPluginContext for a specific plugin.
         *
         * Sets up directory structure and creates the context with all
         * required dependencies.
         *
         * @param context Android Context (preferably Application context)
         * @param pluginId Unique plugin identifier
         * @param logger Plugin logger instance
         * @param serviceRegistry Platform service registry
         * @param eventBus Plugin event bus
         * @return Configured AndroidPluginContext
         */
        fun create(
            context: Context,
            pluginId: String,
            logger: PluginLogger,
            serviceRegistry: ServiceRegistry,
            eventBus: PluginEventBus
        ): AndroidPluginContext {
            val appContext = context.applicationContext

            // Create plugin-specific directories
            val pluginDataDir = File(appContext.filesDir, "$PLUGINS_DIR/$pluginId").apply {
                mkdirs()
            }

            val pluginCacheDir = File(appContext.cacheDir, "$PLUGINS_DIR/$pluginId").apply {
                mkdirs()
            }

            val sharedDir = File(appContext.filesDir, "$SHARED_DIR/$PLUGINS_DIR").apply {
                mkdirs()
            }

            PluginLog.d(TAG, "Created context for plugin: $pluginId")
            PluginLog.d(TAG, "  Data dir: ${pluginDataDir.absolutePath}")
            PluginLog.d(TAG, "  Cache dir: ${pluginCacheDir.absolutePath}")
            PluginLog.d(TAG, "  Shared dir: ${sharedDir.absolutePath}")

            return AndroidPluginContext(
                androidContext = appContext,
                pluginDataDir = pluginDataDir.absolutePath,
                cacheDir = pluginCacheDir.absolutePath,
                sharedDir = sharedDir.absolutePath,
                logger = logger,
                serviceRegistry = serviceRegistry,
                eventBus = eventBus
            )
        }

        /**
         * Create platform info for Android.
         *
         * Gathers device and OS information for the platform info.
         *
         * @return PlatformInfo populated with Android device details
         */
        fun createAndroidPlatformInfo(): PlatformInfo {
            return PlatformInfo(
                platform = "android",
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                deviceType = determineDeviceType(),
                extras = mapOf(
                    "manufacturer" to Build.MANUFACTURER,
                    "model" to Build.MODEL,
                    "device" to Build.DEVICE,
                    "brand" to Build.BRAND,
                    "sdk_int" to Build.VERSION.SDK_INT.toString(),
                    "build_id" to Build.ID
                )
            )
        }

        /**
         * Determine the device type based on screen characteristics.
         *
         * @return Device type string (phone, tablet, tv, etc.)
         */
        private fun determineDeviceType(): String {
            // This is a simplified determination - in production,
            // you would use screen size and density calculations
            return when {
                Build.DEVICE.contains("tv", ignoreCase = true) -> "tv"
                Build.DEVICE.contains("watch", ignoreCase = true) -> "watch"
                Build.DEVICE.contains("auto", ignoreCase = true) -> "auto"
                // Default to phone - tablet detection requires Context
                else -> "phone"
            }
        }

        /**
         * Clean up data for a specific plugin.
         *
         * Removes all plugin directories and preferences.
         *
         * @param context Android Context
         * @param pluginId Plugin identifier to clean up
         * @return true if cleanup was successful
         */
        fun cleanupPluginData(context: Context, pluginId: String): Boolean {
            val appContext = context.applicationContext
            return try {
                // Remove data directory
                File(appContext.filesDir, "$PLUGINS_DIR/$pluginId").deleteRecursively()

                // Remove cache directory
                File(appContext.cacheDir, "$PLUGINS_DIR/$pluginId").deleteRecursively()

                // Remove SharedPreferences (they use a prefix pattern)
                val prefsDir = File(appContext.dataDir, "shared_prefs")
                if (prefsDir.exists()) {
                    prefsDir.listFiles()
                        ?.filter { it.name.startsWith("plugin_${pluginId.replace('.', '_')}") }
                        ?.forEach { it.delete() }
                }

                PluginLog.i(TAG, "Cleaned up data for plugin: $pluginId")
                true
            } catch (e: Exception) {
                PluginLog.e(TAG, "Failed to clean up plugin data: $pluginId", e)
                false
            }
        }
    }
}

/**
 * Extension function to convert AndroidPluginContext to PluginContext.
 *
 * Creates a PluginContext compatible with the common module interface.
 *
 * @return PluginContext with the same configuration
 */
fun AndroidPluginContext.toPluginContext(): com.augmentalis.magiccode.plugins.universal.PluginContext {
    return com.augmentalis.magiccode.plugins.universal.PluginContext(
        appDataDir = pluginDataDir,
        cacheDir = cacheDir,
        serviceRegistry = serviceRegistry,
        eventBus = eventBus,
        platformInfo = platformInfo
    )
}

/**
 * Extension function to get AndroidPluginContext from a PluginContext.
 *
 * If the provided context is already an AndroidPluginContext, returns it directly.
 * Otherwise, throws an exception.
 *
 * @return AndroidPluginContext
 * @throws IllegalArgumentException if context is not an AndroidPluginContext
 */
fun com.augmentalis.magiccode.plugins.universal.PluginContext.asAndroidContext(): AndroidPluginContext {
    // Note: This would require storing the AndroidPluginContext reference
    // In practice, you would use a different mechanism like a registry
    throw UnsupportedOperationException(
        "Cannot convert generic PluginContext to AndroidPluginContext. " +
        "Use AndroidPluginContext directly when Android-specific features are needed."
    )
}
