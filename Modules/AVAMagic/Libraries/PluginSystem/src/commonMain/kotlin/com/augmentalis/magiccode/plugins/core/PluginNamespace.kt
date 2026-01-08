package com.augmentalis.magiccode.plugins.core

/**
 * Plugin namespace for resource isolation.
 *
 * Each plugin gets an isolated namespace with dedicated directories for cache,
 * temporary files, and preferences to prevent cross-plugin interference.
 */
data class PluginNamespace(
    /**
     * Plugin identifier.
     */
    val pluginId: String,

    /**
     * Base directory for this plugin's resources.
     */
    val baseDir: String,

    /**
     * Cache directory for this plugin.
     */
    val cacheDir: String,

    /**
     * Temporary files directory for this plugin.
     */
    val tempDir: String,

    /**
     * Preferences file path for this plugin.
     */
    val preferencesFile: String,

    /**
     * Current disk usage in bytes.
     */
    var diskUsageBytes: Long = 0L
) {
    companion object {
        /**
         * Create namespace for a plugin.
         *
         * @param pluginId Plugin identifier
         * @param appDataDir Application data directory
         * @return PluginNamespace instance
         */
        fun create(pluginId: String, appDataDir: String): PluginNamespace {
            // Sanitize plugin ID for filesystem (replace dots with underscores)
            val safeName = pluginId.replace(".", "_")

            val baseDir = "$appDataDir/plugins/$safeName"
            val cacheDir = "$baseDir/cache"
            val tempDir = "$baseDir/temp"
            val preferencesFile = "$baseDir/preferences.json"

            return PluginNamespace(
                pluginId = pluginId,
                baseDir = baseDir,
                cacheDir = cacheDir,
                tempDir = tempDir,
                preferencesFile = preferencesFile
            )
        }
    }

    /**
     * Check if a file path belongs to this namespace.
     *
     * @param filePath File path to check
     * @return true if path is within this namespace
     */
    fun isWithinNamespace(filePath: String): Boolean {
        return filePath.startsWith(baseDir)
    }

    /**
     * Validate that a resource access is authorized for this namespace.
     *
     * @param resourcePath Resource path being accessed
     * @throws SecurityException if access is not authorized
     */
    fun validateAccess(resourcePath: String) {
        if (!isWithinNamespace(resourcePath)) {
            throw SecurityException(
                "Plugin $pluginId attempted to access resource outside its namespace: $resourcePath"
            )
        }
    }

    /**
     * Clean up namespace (delete all files/directories).
     * FR-017: Namespace cleanup on plugin uninstall.
     *
     * @param fileIO FileIO instance for deletion
     * @return true if cleanup successful
     */
    fun cleanup(fileIO: com.augmentalis.magiccode.plugins.platform.FileIO): Boolean {
        return try {
            fileIO.delete(baseDir)
        } catch (e: Exception) {
            false
        }
    }
}
