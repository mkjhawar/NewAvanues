package com.augmentalis.avacode.plugins.security

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * JVM implementation of PermissionStorage.
 *
 * Stores permission states as JSON files in user data directory.
 * Each plugin gets its own JSON file.
 *
 * TODO: For production, consider:
 * - SQLite database for better querying
 * - File locking for concurrent access
 * - Backup and migration support
 * - Encrypted storage for sensitive permissions
 */
class JvmPermissionStorage(
    private val storageDirectory: File = getDefaultStorageDirectory()
) : PermissionStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        // Ensure storage directory exists
        if (!storageDirectory.exists()) {
            storageDirectory.mkdirs()
        }
    }

    companion object {
        private const val STORAGE_DIR_NAME = "plugin_permissions"
        private const val FILE_EXTENSION = ".json"

        /**
         * Get default storage directory.
         *
         * Uses platform-appropriate location:
         * - Windows: %APPDATA%/MagicCode/plugin_permissions
         * - macOS: ~/Library/Application Support/MagicCode/plugin_permissions
         * - Linux: ~/.config/MagicCode/plugin_permissions
         */
        private fun getDefaultStorageDirectory(): File {
            val userHome = System.getProperty("user.home")
            val osName = System.getProperty("os.name").lowercase()

            val baseDir = when {
                osName.contains("win") -> {
                    // Windows
                    File(System.getenv("APPDATA") ?: "$userHome/AppData/Roaming", "MagicCode")
                }
                osName.contains("mac") -> {
                    // macOS
                    File(userHome, "Library/Application Support/MagicCode")
                }
                else -> {
                    // Linux and others
                    File(userHome, ".config/MagicCode")
                }
            }

            return File(baseDir, STORAGE_DIR_NAME)
        }
    }

    /**
     * Get file for a plugin's permission state.
     */
    private fun getPluginFile(pluginId: String): File {
        // Sanitize plugin ID for filename
        val safeFilename = pluginId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(storageDirectory, "$safeFilename$FILE_EXTENSION")
    }

    /**
     * Save permission state for a plugin.
     */
    override suspend fun save(state: PluginPermissionState) {
        try {
            val file = getPluginFile(state.pluginId)
            val jsonString = json.encodeToString(state)

            Files.write(
                file.toPath(),
                jsonString.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        } catch (e: Exception) {
            // TODO: Log error
            System.err.println("Failed to save permission state for ${state.pluginId}: ${e.message}")
        }
    }

    /**
     * Load permission state for a plugin.
     */
    override suspend fun load(pluginId: String): PluginPermissionState? {
        try {
            val file = getPluginFile(pluginId)
            if (!file.exists()) {
                return null
            }

            val jsonString = file.readText()
            return json.decodeFromString<PluginPermissionState>(jsonString)
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to load permission state for $pluginId",
                e
            )
            return null
        }
    }

    /**
     * Delete permission state for a plugin.
     */
    override suspend fun delete(pluginId: String) {
        try {
            val file = getPluginFile(pluginId)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to delete permission state for $pluginId",
                e
            )
        }
    }

    /**
     * Load all permission states.
     */
    override suspend fun loadAll(): Map<String, PluginPermissionState> {
        val result = mutableMapOf<String, PluginPermissionState>()

        try {
            storageDirectory.listFiles { file ->
                file.isFile && file.name.endsWith(FILE_EXTENSION)
            }?.forEach { file ->
                try {
                    val jsonString = file.readText()
                    val state = json.decodeFromString<PluginPermissionState>(jsonString)
                    result[state.pluginId] = state
                } catch (e: Exception) {
                    // Skip invalid files
                    com.augmentalis.magiccode.plugins.core.PluginLog.e(
                        "PermissionStorage",
                        "Failed to load permission state from ${file.name}",
                        e
                    )
                }
            }
        } catch (e: Exception) {
            com.augmentalis.magiccode.plugins.core.PluginLog.e(
                "PermissionStorage",
                "Failed to load all permission states",
                e
            )
        }

        return result
    }
}

/**
 * Factory for creating JVM PermissionStorage instances.
 */
actual object PermissionStorageFactory {
    private var storageDirectoryProvider: (() -> File)? = null

    /**
     * Set the storage directory provider for creating PermissionStorage instances.
     * This is optional - if not set, default directory will be used.
     *
     * @param provider Lambda that provides File for storage directory
     */
    fun setStorageDirectoryProvider(provider: () -> File) {
        storageDirectoryProvider = provider
    }

    /**
     * Create a PermissionStorage instance.
     * Uses the registered storage directory provider if available.
     */
    actual fun create(): PermissionStorage {
        val storageDir = storageDirectoryProvider?.invoke()
        return if (storageDir != null) {
            JvmPermissionStorage(storageDir)
        } else {
            JvmPermissionStorage()
        }
    }

    /**
     * Create a PermissionStorage with a specific storage directory.
     *
     * @param storageDirectory File directory for JSON storage
     */
    fun createWithDirectory(storageDirectory: File): PermissionStorage {
        return JvmPermissionStorage(storageDirectory)
    }
}
