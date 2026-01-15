package com.augmentalis.avacode.plugins

import com.augmentalis.avacode.plugins.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * JVM file-based plugin persistence implementation.
 *
 * Uses JSON files to persist plugin information. Suitable for desktop/server environments.
 * Each plugin is stored as a separate JSON file for easy inspection and debugging.
 */
class JvmFilePluginPersistence(
    private val storageDir: String,
    private val appDataDir: String
) : PluginPersistence {

    companion object {
        private const val TAG = "JvmFilePluginPersistence"
        private const val PLUGIN_FILE_EXTENSION = ".plugin.json"

        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }

    private val storageDirFile = File(storageDir).also { dir ->
        if (!dir.exists()) {
            dir.mkdirs()
            PluginLog.i(TAG, "Created plugin storage directory: ${dir.absolutePath}")
        }
    }

    override suspend fun savePlugin(pluginInfo: PluginRegistry.PluginInfo): Result<Unit> {
        return try {
            val data = PluginPersistenceData.fromPluginInfo(pluginInfo)
            val jsonString = json.encodeToString(data)
            val file = getPluginFile(pluginInfo.manifest.id)
            file.writeText(jsonString)
            PluginLog.d(TAG, "Saved plugin to file: ${file.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to save plugin: ${pluginInfo.manifest.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun loadPlugin(pluginId: String): Result<PluginRegistry.PluginInfo?> {
        return try {
            val file = getPluginFile(pluginId)
            if (!file.exists()) {
                PluginLog.d(TAG, "Plugin file not found: $pluginId")
                return Result.success(null)
            }

            val jsonString = file.readText()
            val data = json.decodeFromString<PluginPersistenceData>(jsonString)
            val pluginInfo = data.toPluginInfo(appDataDir)
            PluginLog.d(TAG, "Loaded plugin from file: ${file.absolutePath}")
            Result.success(pluginInfo)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to load plugin: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun loadAllPlugins(): Result<List<PluginRegistry.PluginInfo>> {
        return try {
            val pluginFiles = storageDirFile.listFiles { file ->
                file.isFile && file.name.endsWith(PLUGIN_FILE_EXTENSION)
            } ?: emptyArray()

            val plugins = pluginFiles.mapNotNull { file ->
                try {
                    val jsonString = file.readText()
                    val data = json.decodeFromString<PluginPersistenceData>(jsonString)
                    data.toPluginInfo(appDataDir)
                } catch (e: Exception) {
                    PluginLog.e(TAG, "Failed to load plugin from file: ${file.name}", e)
                    null
                }
            }

            PluginLog.d(TAG, "Loaded ${plugins.size} plugins from storage directory")
            Result.success(plugins)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to load all plugins", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePlugin(pluginId: String): Result<Unit> {
        return try {
            val file = getPluginFile(pluginId)
            if (file.exists()) {
                file.delete()
                PluginLog.d(TAG, "Deleted plugin file: ${file.absolutePath}")
            } else {
                PluginLog.w(TAG, "Plugin file not found for deletion: $pluginId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to delete plugin: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePluginState(pluginId: String, state: PluginState): Result<Unit> {
        return try {
            val file = getPluginFile(pluginId)
            if (!file.exists()) {
                return Result.failure(Exception("Plugin not found: $pluginId"))
            }

            val jsonString = file.readText()
            val data = json.decodeFromString<PluginPersistenceData>(jsonString)
            val updatedData = data.copy(state = state.name)
            val updatedJson = json.encodeToString(updatedData)
            file.writeText(updatedJson)
            PluginLog.d(TAG, "Updated plugin state: $pluginId -> $state")
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to update plugin state: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun exists(pluginId: String): Result<Boolean> {
        return try {
            val file = getPluginFile(pluginId)
            Result.success(file.exists())
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to check plugin existence: $pluginId", e)
            Result.failure(e)
        }
    }

    override suspend fun getPluginCount(): Result<Int> {
        return try {
            val pluginFiles = storageDirFile.listFiles { file ->
                file.isFile && file.name.endsWith(PLUGIN_FILE_EXTENSION)
            } ?: emptyArray()
            Result.success(pluginFiles.size)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to get plugin count", e)
            Result.failure(e)
        }
    }

    override suspend fun clearAll(): Result<Unit> {
        return try {
            val pluginFiles = storageDirFile.listFiles { file ->
                file.isFile && file.name.endsWith(PLUGIN_FILE_EXTENSION)
            } ?: emptyArray()

            pluginFiles.forEach { it.delete() }
            PluginLog.w(TAG, "Cleared all plugin files from storage directory")
            Result.success(Unit)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to clear all plugins", e)
            Result.failure(e)
        }
    }

    /**
     * Get file for a specific plugin.
     */
    private fun getPluginFile(pluginId: String): File {
        // Sanitize plugin ID for filename (replace dots with underscores)
        val safeFileName = pluginId.replace(".", "_") + PLUGIN_FILE_EXTENSION
        return File(storageDirFile, safeFileName)
    }
}

/**
 * Serializable data model for plugin persistence.
 *
 * This is a flattened representation of PluginInfo suitable for JSON serialization.
 */
@Serializable
private data class PluginPersistenceData(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String? = null,
    val entrypoint: String,
    val capabilities: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val source: String,
    val verificationLevel: String,
    val state: String,
    val loadedAt: Long,
    val namespaceBaseDir: String,
    val namespaceCacheDir: String,
    val namespaceTempDir: String,
    val namespacePreferencesFile: String,
    val namespaceDiskUsage: Long = 0L
) {
    companion object {
        fun fromPluginInfo(pluginInfo: PluginRegistry.PluginInfo): PluginPersistenceData {
            return PluginPersistenceData(
                id = pluginInfo.manifest.id,
                name = pluginInfo.manifest.name,
                version = pluginInfo.manifest.version,
                author = pluginInfo.manifest.author,
                description = pluginInfo.manifest.description,
                entrypoint = pluginInfo.manifest.entrypoint,
                capabilities = pluginInfo.manifest.capabilities,
                permissions = pluginInfo.manifest.permissions,
                source = pluginInfo.manifest.source,
                verificationLevel = pluginInfo.manifest.verificationLevel,
                state = pluginInfo.state.name,
                loadedAt = pluginInfo.loadedAt,
                namespaceBaseDir = pluginInfo.namespace.baseDir,
                namespaceCacheDir = pluginInfo.namespace.cacheDir,
                namespaceTempDir = pluginInfo.namespace.tempDir,
                namespacePreferencesFile = pluginInfo.namespace.preferencesFile,
                namespaceDiskUsage = pluginInfo.namespace.diskUsageBytes
            )
        }
    }

    fun toPluginInfo(appDataDir: String): PluginRegistry.PluginInfo {
        val manifest = PluginManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            entrypoint = entrypoint,
            capabilities = capabilities,
            dependencies = emptyList(), // Not persisted in simple file storage
            permissions = permissions,
            source = source,
            verificationLevel = verificationLevel,
            assets = null,
            manifestVersion = "1.0",
            homepage = null,
            license = null
        )

        // Recreate namespace - use persisted values if available, otherwise create fresh
        val namespace = PluginNamespace(
            pluginId = id,
            baseDir = namespaceBaseDir,
            cacheDir = namespaceCacheDir,
            tempDir = namespaceTempDir,
            preferencesFile = namespacePreferencesFile,
            diskUsageBytes = namespaceDiskUsage
        )

        return PluginRegistry.PluginInfo(
            manifest = manifest,
            state = PluginState.valueOf(state),
            loadedAt = loadedAt,
            namespace = namespace
        )
    }
}

/**
 * Create default JVM persistence implementation.
 *
 * Uses in-memory storage by default. Can be configured to use file-based storage.
 */
actual fun createDefaultPluginPersistence(appDataDir: String): PluginPersistence {
    // For JVM, default to in-memory for simplicity
    // Production code can explicitly create JvmFilePluginPersistence if needed
    PluginLog.i("JvmPluginPersistence", "Using in-memory plugin persistence for JVM platform")
    return InMemoryPluginPersistence()
}
