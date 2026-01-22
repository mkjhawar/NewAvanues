/**
 * PluginStateStorage.kt - Persistent storage for plugin state during hot reload
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides filesystem-based persistence for plugin state during hot reload.
 * This enables state to survive app restarts or crashes during reload operations.
 *
 * ## Overview
 * When a plugin is being reloaded, its state is saved to disk before shutdown.
 * If the reload fails or the app crashes, the state can be recovered from disk
 * when the plugin is next initialized.
 *
 * ## Storage Format
 * States are stored as JSON files in the configured cache directory:
 * ```
 * {cacheDir}/
 *   plugin_states/
 *     com.example.plugin1.state.json
 *     com.example.plugin2.state.json
 *     ...
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * val storage = PluginStateStorage(
 *     cacheDir = "/data/data/com.app/cache",
 *     fileIO = FileIO(),
 *     serializer = JsonPluginStateSerializer()
 * )
 *
 * // Save state before reload
 * val state = mapOf("counter" to 42)
 * storage.saveState("com.example.plugin", state)
 *
 * // Load state after reload
 * val restored = storage.loadState("com.example.plugin")
 *
 * // Clear state after successful restoration
 * storage.clearState("com.example.plugin")
 * ```
 */
package com.augmentalis.magiccode.plugins.hotreload

import com.augmentalis.magiccode.plugins.platform.FileIO
import com.augmentalis.magiccode.plugins.universal.JsonPluginStateSerializer
import com.augmentalis.magiccode.plugins.universal.PluginStateSerializer
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistent storage for plugin state during hot reload.
 *
 * Saves and loads plugin state to/from the filesystem, enabling state
 * recovery across reload operations, app restarts, and crash scenarios.
 *
 * ## Thread Safety
 * All operations are thread-safe using a mutex to prevent concurrent
 * access to the same state file.
 *
 * ## Error Handling
 * Read operations return null on failure (file not found, parse error).
 * Write operations throw exceptions on failure that should be caught
 * and handled by the caller.
 *
 * @param cacheDir Base directory for storing state files
 * @param fileIO Platform-specific file I/O implementation
 * @param serializer Serializer for converting state to/from bytes
 * @since 1.0.0
 */
class PluginStateStorage(
    private val cacheDir: String,
    private val fileIO: FileIO,
    private val serializer: PluginStateSerializer = JsonPluginStateSerializer.Default
) {
    /**
     * Directory for storing plugin states.
     */
    private val stateDir: String = "$cacheDir/$STATE_DIRECTORY_NAME"

    /**
     * Mutex for thread-safe file operations.
     */
    private val mutex = Mutex()

    /**
     * Initialize the storage directory.
     *
     * Creates the state directory if it doesn't exist.
     *
     * @return true if directory exists or was created successfully
     */
    suspend fun initialize(): Boolean = mutex.withLock {
        if (!fileIO.directoryExists(stateDir)) {
            return@withLock fileIO.createDirectory(stateDir)
        }
        true
    }

    /**
     * Save plugin state to persistent storage.
     *
     * Serializes the state map and writes it to a file named after the plugin ID.
     * Overwrites any existing state for the same plugin.
     *
     * @param pluginId The unique plugin identifier
     * @param state The state map to save
     * @throws StatePersistenceException if save fails
     */
    suspend fun saveState(pluginId: String, state: Map<String, Any>) = mutex.withLock {
        ensureDirectoryExists()

        val filePath = getStateFilePath(pluginId)
        val metadataPath = getMetadataFilePath(pluginId)

        try {
            // Serialize state to bytes
            val bytes = serializer.serializeState(state)

            // Write state file
            fileIO.writeFileAsBytes(filePath, bytes)

            // Write metadata file with timestamp
            val metadata = StateMetadata(
                pluginId = pluginId,
                savedAt = currentTimeMillis(),
                stateSize = bytes.size.toLong(),
                version = STORAGE_VERSION
            )
            val metadataJson = buildMetadataJson(metadata)
            fileIO.writeFileAsString(metadataPath, metadataJson)
        } catch (e: Exception) {
            throw StatePersistenceException(
                "Failed to save state for plugin '$pluginId'",
                pluginId,
                e
            )
        }
    }

    /**
     * Load plugin state from persistent storage.
     *
     * Reads and deserializes the state file for the specified plugin.
     * Returns null if no state file exists or if reading fails.
     *
     * @param pluginId The unique plugin identifier
     * @return The restored state map, or null if not found or error
     */
    suspend fun loadState(pluginId: String): Map<String, Any>? = mutex.withLock {
        val filePath = getStateFilePath(pluginId)

        if (!fileIO.fileExists(filePath)) {
            return@withLock null
        }

        try {
            val bytes = fileIO.readFileAsBytes(filePath)
            serializer.deserializeState(bytes)
        } catch (e: Exception) {
            // Return null on any error - caller can decide how to handle
            null
        }
    }

    /**
     * Clear saved state for a specific plugin.
     *
     * Deletes the state and metadata files for the specified plugin.
     * Safe to call even if no state exists.
     *
     * @param pluginId The unique plugin identifier
     * @return true if state was cleared (or didn't exist)
     */
    suspend fun clearState(pluginId: String): Boolean = mutex.withLock {
        val stateFile = getStateFilePath(pluginId)
        val metadataFile = getMetadataFilePath(pluginId)

        var success = true

        if (fileIO.fileExists(stateFile)) {
            success = fileIO.delete(stateFile) && success
        }

        if (fileIO.fileExists(metadataFile)) {
            success = fileIO.delete(metadataFile) && success
        }

        success
    }

    /**
     * Clear all saved plugin states.
     *
     * Removes all state and metadata files from the storage directory.
     * Useful for cleanup during testing or app uninstall.
     *
     * @return true if all states were cleared
     */
    suspend fun clearAllStates(): Boolean = mutex.withLock {
        if (!fileIO.directoryExists(stateDir)) {
            return@withLock true
        }

        try {
            val files = fileIO.listFiles(stateDir)
            var success = true

            for (file in files) {
                val filePath = "$stateDir/$file"
                success = fileIO.delete(filePath) && success
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if state exists for a plugin.
     *
     * @param pluginId The unique plugin identifier
     * @return true if saved state exists
     */
    suspend fun hasState(pluginId: String): Boolean = mutex.withLock {
        fileIO.fileExists(getStateFilePath(pluginId))
    }

    /**
     * Get metadata for saved state.
     *
     * @param pluginId The unique plugin identifier
     * @return StateMetadata if state exists, null otherwise
     */
    suspend fun getStateMetadata(pluginId: String): StateMetadata? = mutex.withLock {
        val metadataPath = getMetadataFilePath(pluginId)

        if (!fileIO.fileExists(metadataPath)) {
            return@withLock null
        }

        try {
            val json = fileIO.readFileAsString(metadataPath)
            parseMetadataJson(json, pluginId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get list of all plugins with saved state.
     *
     * @return List of plugin IDs that have saved state
     */
    suspend fun listSavedPlugins(): List<String> = mutex.withLock {
        if (!fileIO.directoryExists(stateDir)) {
            return@withLock emptyList()
        }

        try {
            fileIO.listFiles(stateDir)
                .filter { it.endsWith(STATE_FILE_EXTENSION) }
                .map { it.removeSuffix(STATE_FILE_EXTENSION) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get total size of all saved states.
     *
     * @return Total size in bytes
     */
    suspend fun getTotalStateSize(): Long = mutex.withLock {
        if (!fileIO.directoryExists(stateDir)) {
            return@withLock 0L
        }

        try {
            fileIO.listFiles(stateDir)
                .filter { it.endsWith(STATE_FILE_EXTENSION) }
                .sumOf { fileIO.getFileSize("$stateDir/$it") }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Clean up stale states older than the specified age.
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return Number of states cleaned up
     */
    suspend fun cleanupStaleStates(maxAgeMs: Long): Int = mutex.withLock {
        if (!fileIO.directoryExists(stateDir)) {
            return@withLock 0
        }

        var cleanedCount = 0
        val now = currentTimeMillis()

        try {
            val plugins = fileIO.listFiles(stateDir)
                .filter { it.endsWith(METADATA_FILE_EXTENSION) }
                .map { it.removeSuffix(METADATA_FILE_EXTENSION) }

            for (pluginId in plugins) {
                val metadata = getStateMetadataInternal(pluginId)
                if (metadata != null && (now - metadata.savedAt) > maxAgeMs) {
                    if (clearStateInternal(pluginId)) {
                        cleanedCount++
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }

        cleanedCount
    }

    /**
     * Internal method to get metadata without mutex.
     */
    private fun getStateMetadataInternal(pluginId: String): StateMetadata? {
        val metadataPath = getMetadataFilePath(pluginId)

        if (!fileIO.fileExists(metadataPath)) {
            return null
        }

        return try {
            val json = fileIO.readFileAsString(metadataPath)
            parseMetadataJson(json, pluginId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Internal method to clear state without mutex.
     */
    private fun clearStateInternal(pluginId: String): Boolean {
        val stateFile = getStateFilePath(pluginId)
        val metadataFile = getMetadataFilePath(pluginId)

        var success = true

        if (fileIO.fileExists(stateFile)) {
            success = fileIO.delete(stateFile) && success
        }

        if (fileIO.fileExists(metadataFile)) {
            success = fileIO.delete(metadataFile) && success
        }

        return success
    }

    /**
     * Ensure the state directory exists.
     */
    private fun ensureDirectoryExists() {
        if (!fileIO.directoryExists(stateDir)) {
            fileIO.createDirectory(stateDir)
        }
    }

    /**
     * Get the file path for a plugin's state.
     */
    private fun getStateFilePath(pluginId: String): String {
        return "$stateDir/${sanitizePluginId(pluginId)}$STATE_FILE_EXTENSION"
    }

    /**
     * Get the file path for a plugin's metadata.
     */
    private fun getMetadataFilePath(pluginId: String): String {
        return "$stateDir/${sanitizePluginId(pluginId)}$METADATA_FILE_EXTENSION"
    }

    /**
     * Sanitize plugin ID for use in filenames.
     * Replaces dots and special characters with underscores.
     */
    private fun sanitizePluginId(pluginId: String): String {
        return pluginId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    /**
     * Build metadata JSON string.
     */
    private fun buildMetadataJson(metadata: StateMetadata): String {
        return """
            {
                "pluginId": "${metadata.pluginId}",
                "savedAt": ${metadata.savedAt},
                "stateSize": ${metadata.stateSize},
                "version": ${metadata.version}
            }
        """.trimIndent()
    }

    /**
     * Parse metadata from JSON string.
     */
    private fun parseMetadataJson(json: String, defaultPluginId: String): StateMetadata {
        // Simple JSON parsing without external dependencies
        val pluginId = extractJsonString(json, "pluginId") ?: defaultPluginId
        val savedAt = extractJsonLong(json, "savedAt") ?: 0L
        val stateSize = extractJsonLong(json, "stateSize") ?: 0L
        val version = extractJsonInt(json, "version") ?: STORAGE_VERSION

        return StateMetadata(
            pluginId = pluginId,
            savedAt = savedAt,
            stateSize = stateSize,
            version = version
        )
    }

    /**
     * Extract a string value from JSON.
     */
    private fun extractJsonString(json: String, key: String): String? {
        val regex = """"$key"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)
    }

    /**
     * Extract a long value from JSON.
     */
    private fun extractJsonLong(json: String, key: String): Long? {
        val regex = """"$key"\s*:\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    /**
     * Extract an int value from JSON.
     */
    private fun extractJsonInt(json: String, key: String): Int? {
        val regex = """"$key"\s*:\s*(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    companion object {
        /** Name of the directory for storing plugin states. */
        const val STATE_DIRECTORY_NAME = "plugin_states"

        /** File extension for state files. */
        const val STATE_FILE_EXTENSION = ".state.json"

        /** File extension for metadata files. */
        const val METADATA_FILE_EXTENSION = ".meta.json"

        /** Current storage format version. */
        const val STORAGE_VERSION = 1

        /** Default max age for stale state cleanup (24 hours). */
        const val DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}

/**
 * Metadata about saved plugin state.
 *
 * @property pluginId The plugin identifier
 * @property savedAt Timestamp when state was saved (epoch millis)
 * @property stateSize Size of the state file in bytes
 * @property version Storage format version
 */
data class StateMetadata(
    val pluginId: String,
    val savedAt: Long,
    val stateSize: Long,
    val version: Int
) {
    /**
     * Get age of the saved state in milliseconds.
     */
    fun getAgeMs(): Long = currentTimeMillis() - savedAt

    /**
     * Check if state is stale (older than max age).
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return true if state is older than maxAgeMs
     */
    fun isStale(maxAgeMs: Long = PluginStateStorage.DEFAULT_MAX_AGE_MS): Boolean {
        return getAgeMs() > maxAgeMs
    }
}

/**
 * Exception thrown when state persistence operations fail.
 *
 * @param message Error message
 * @param pluginId Plugin ID associated with the failure
 * @param cause Underlying cause
 */
class StatePersistenceException(
    message: String,
    val pluginId: String,
    cause: Throwable? = null
) : Exception(message, cause)
