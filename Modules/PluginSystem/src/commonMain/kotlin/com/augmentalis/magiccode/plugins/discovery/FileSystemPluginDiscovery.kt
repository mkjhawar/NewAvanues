/**
 * FileSystemPluginDiscovery.kt - Discovers plugins from file system
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Discovery source for plugins stored on the local file system.
 * Scans configured directories for plugin manifests and supports
 * hot-reload through file system watching.
 */
package com.augmentalis.magiccode.plugins.discovery

import com.augmentalis.magiccode.plugins.platform.FileIO
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Discovers plugins from the file system.
 *
 * Scans configured directories for plugin manifests (plugin.avu, plugin.json)
 * and creates descriptors for discovered plugins. Supports hot-reload by
 * watching for file changes and notifying listeners.
 *
 * ## Directory Structure
 * Plugins should be organized in directories with the following structure:
 * ```
 * plugins/
 *   my-plugin/
 *     plugin.avu       # or plugin.json
 *     plugin.jar       # JVM/Android plugin code
 *     assets/
 *       ...
 *   another-plugin/
 *     plugin.avu
 *     ...
 * ```
 *
 * ## Manifest Files
 * Supported manifest formats:
 * - `plugin.avu` - Avanues Universal format (preferred)
 * - `plugin.json` - JSON format for compatibility
 *
 * ## Usage Example
 * ```kotlin
 * val fileIO = FileIO()  // Platform-specific implementation
 * val manifestReader = PluginManifestReader(fileIO)
 *
 * val discovery = FileSystemPluginDiscovery(
 *     pluginDirectories = listOf("/data/plugins", "/sdcard/plugins"),
 *     manifestReader = manifestReader,
 *     fileIO = fileIO
 * )
 *
 * // Discover all plugins
 * val plugins = discovery.discoverPlugins()
 *
 * // Watch for changes
 * discovery.watchForChanges { event ->
 *     when (event) {
 *         is PluginChangeEvent.Added -> println("New plugin: ${event.descriptor.pluginId}")
 *         is PluginChangeEvent.Removed -> println("Removed: ${event.pluginId}")
 *         is PluginChangeEvent.Updated -> println("Updated: ${event.descriptor.pluginId}")
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * All operations are thread-safe. Discovery and watching can occur concurrently.
 *
 * @param pluginDirectories List of directories to scan for plugins
 * @param manifestReader Reader for parsing plugin manifests
 * @param fileIO Platform-specific file I/O operations
 * @param pluginLoader Optional custom plugin loader
 * @since 1.0.0
 * @see PluginDiscovery
 * @see PluginManifestReader
 */
class FileSystemPluginDiscovery(
    private val pluginDirectories: List<String>,
    private val manifestReader: PluginManifestReader,
    private val fileIO: FileIO,
    private val pluginLoader: PluginLoader? = null
) : PluginDiscovery {

    /**
     * Priority for file system plugins.
     */
    override val priority: Int = PRIORITY_FILESYSTEM

    /**
     * Cache of discovered plugin descriptors.
     */
    private val discoveryCache = mutableMapOf<String, CachedDescriptor>()

    /**
     * Mutex for thread-safe cache access.
     */
    private val mutex = Mutex()

    /**
     * Change listeners.
     */
    private val changeListeners = mutableListOf<(PluginChangeEvent) -> Unit>()

    /**
     * File watcher job.
     */
    private var watchJob: Job? = null

    /**
     * Watch polling interval in milliseconds.
     */
    private var watchIntervalMs: Long = DEFAULT_WATCH_INTERVAL_MS

    /**
     * Last modification times for watched paths.
     */
    private val lastModifiedTimes = mutableMapOf<String, Long>()

    /**
     * Discover available plugins from configured directories.
     *
     * Scans all configured directories for plugin manifests and returns
     * descriptors for valid plugins found.
     *
     * @return List of discovered plugin descriptors
     */
    override suspend fun discoverPlugins(): List<PluginDescriptor> {
        return mutex.withLock {
            val results = mutableListOf<PluginDescriptor>()
            val errors = mutableListOf<DiscoveryError>()

            for (directory in pluginDirectories) {
                try {
                    val discovered = scanDirectory(directory)
                    results.addAll(discovered.plugins)
                    errors.addAll(discovered.errors)
                } catch (e: Exception) {
                    errors.add(DiscoveryError.fromException(e, "FileSystem:$directory"))
                }
            }

            // Update cache with fresh results
            discoveryCache.clear()
            for (descriptor in results) {
                val sourcePath = (descriptor.source as? PluginSource.FileSystem)?.path ?: ""
                discoveryCache[descriptor.pluginId] = CachedDescriptor(
                    descriptor = descriptor,
                    path = sourcePath,
                    lastModified = if (sourcePath.isNotEmpty()) fileIO.getLastModified(sourcePath) else 0L
                )
            }

            results
        }
    }

    /**
     * Load a plugin from its descriptor.
     *
     * Creates a plugin instance from the file system path in the descriptor.
     * The actual loading mechanism depends on the platform (class loading on JVM,
     * dynamic framework on iOS, etc.).
     *
     * @param descriptor Plugin descriptor with file system source
     * @return Result containing loaded plugin or error
     */
    override suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin> {
        val source = descriptor.source as? PluginSource.FileSystem
            ?: return Result.failure(IllegalArgumentException(
                "Descriptor source is not FileSystem: ${descriptor.source}"
            ))

        return try {
            // Use custom loader if provided, otherwise use reflection-based loading
            if (pluginLoader != null) {
                pluginLoader.loadPlugin(descriptor, source.path)
            } else {
                loadPluginDefault(descriptor, source.path)
            }
        } catch (e: Exception) {
            Result.failure(PluginLoadException(
                "Failed to load plugin from '${source.path}'",
                descriptor.pluginId,
                e
            ))
        }
    }

    /**
     * Check if this source can load a specific plugin.
     *
     * @param descriptor Plugin descriptor to check
     * @return true if the descriptor has a FileSystem source
     */
    override fun canLoad(descriptor: PluginDescriptor): Boolean {
        return descriptor.source is PluginSource.FileSystem
    }

    /**
     * Refresh the discovery cache.
     *
     * Forces a re-scan of all directories, updating the cache with fresh data.
     */
    override suspend fun refresh() {
        discoverPlugins()
    }

    /**
     * Scan a directory for plugin manifests.
     *
     * @param directory Directory path to scan
     * @return DiscoveryResult with found plugins and any errors
     */
    private suspend fun scanDirectory(directory: String): DiscoveryResult {
        val plugins = mutableListOf<PluginDescriptor>()
        val errors = mutableListOf<DiscoveryError>()
        val startTime = currentTimeMillis()

        if (!fileIO.directoryExists(directory)) {
            return DiscoveryResult(
                plugins = emptyList(),
                errors = listOf(DiscoveryError("Directory does not exist: $directory", directory)),
                sourceType = "FileSystem",
                durationMs = currentTimeMillis() - startTime
            )
        }

        val entries = fileIO.listFiles(directory)

        for (entry in entries) {
            val pluginDir = "$directory/$entry"

            // Skip if not a directory
            if (!fileIO.directoryExists(pluginDir)) {
                continue
            }

            // Look for manifest files
            val manifestResult = findAndReadManifest(pluginDir)

            when {
                manifestResult.isSuccess -> {
                    val descriptor = manifestResult.getOrThrow()
                    plugins.add(descriptor)
                }
                manifestResult.isFailure -> {
                    val error = manifestResult.exceptionOrNull()
                    if (error !is NoManifestException) {
                        errors.add(DiscoveryError(
                            message = error?.message ?: "Unknown error",
                            source = pluginDir,
                            pluginId = entry
                        ))
                    }
                    // NoManifestException is normal for non-plugin directories
                }
            }
        }

        return DiscoveryResult(
            plugins = plugins,
            errors = errors,
            sourceType = "FileSystem",
            durationMs = currentTimeMillis() - startTime
        )
    }

    /**
     * Find and read manifest file in a plugin directory.
     *
     * @param pluginDir Plugin directory path
     * @return Result containing PluginDescriptor or error
     */
    private fun findAndReadManifest(pluginDir: String): Result<PluginDescriptor> {
        // Try AVU format first (preferred)
        val avuManifestPath = "$pluginDir/$MANIFEST_FILE_AVU"
        if (fileIO.fileExists(avuManifestPath)) {
            return manifestReader.readManifest(avuManifestPath).map { descriptor ->
                descriptor.copy(
                    source = PluginSource.FileSystem(pluginDir, avuManifestPath)
                )
            }
        }

        // Try JSON format
        val jsonManifestPath = "$pluginDir/$MANIFEST_FILE_JSON"
        if (fileIO.fileExists(jsonManifestPath)) {
            return manifestReader.readManifest(jsonManifestPath).map { descriptor ->
                descriptor.copy(
                    source = PluginSource.FileSystem(pluginDir, jsonManifestPath)
                )
            }
        }

        // No manifest found
        return Result.failure(NoManifestException("No manifest found in $pluginDir"))
    }

    /**
     * Default plugin loading using reflection.
     *
     * This is a fallback when no custom loader is provided.
     *
     * @param descriptor Plugin descriptor
     * @param path Plugin directory path
     * @return Result containing loaded plugin
     */
    private fun loadPluginDefault(descriptor: PluginDescriptor, path: String): Result<UniversalPlugin> {
        val entrypoint = descriptor.entrypoint
            ?: return Result.failure(IllegalStateException(
                "No entrypoint specified for plugin '${descriptor.pluginId}'"
            ))

        // Platform-specific class loading would go here
        // For now, return a failure indicating manual loading is required
        return Result.failure(UnsupportedOperationException(
            "Default plugin loading not implemented. Use a custom PluginLoader for '$entrypoint'"
        ))
    }

    /**
     * Watch for plugin changes in configured directories.
     *
     * Starts a background coroutine that polls for file system changes
     * and notifies the callback when plugins are added, removed, or updated.
     *
     * @param scope CoroutineScope for the watcher
     * @param onChange Callback for change events
     */
    fun watchForChanges(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        onChange: (PluginChangeEvent) -> Unit
    ) {
        changeListeners.add(onChange)

        // Initialize last modified times
        pluginDirectories.forEach { dir ->
            if (fileIO.directoryExists(dir)) {
                lastModifiedTimes[dir] = fileIO.getLastModified(dir)
            }
        }

        // Start watching if not already watching
        if (watchJob == null || watchJob?.isActive != true) {
            watchJob = scope.launch {
                watchLoop()
            }
        }
    }

    /**
     * Stop watching for changes.
     */
    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
        changeListeners.clear()
    }

    /**
     * Set the watch polling interval.
     *
     * @param intervalMs Polling interval in milliseconds
     */
    fun setWatchInterval(intervalMs: Long) {
        watchIntervalMs = intervalMs.coerceAtLeast(MIN_WATCH_INTERVAL_MS)
    }

    /**
     * Main watch loop that polls for changes.
     */
    private suspend fun watchLoop() {
        while (true) {
            try {
                delay(watchIntervalMs)
                checkForChanges()
            } catch (e: Exception) {
                // Log error but continue watching
                // In production, this would use proper logging
            }
        }
    }

    /**
     * Check for changes in plugin directories.
     */
    private suspend fun checkForChanges() {
        mutex.withLock {
            val currentDescriptors = mutableMapOf<String, PluginDescriptor>()

            // Scan all directories
            for (directory in pluginDirectories) {
                if (!fileIO.directoryExists(directory)) continue

                val entries = fileIO.listFiles(directory)
                for (entry in entries) {
                    val pluginDir = "$directory/$entry"
                    if (!fileIO.directoryExists(pluginDir)) continue

                    val manifestResult = findAndReadManifest(pluginDir)
                    if (manifestResult.isSuccess) {
                        val descriptor = manifestResult.getOrThrow()
                        currentDescriptors[descriptor.pluginId] = descriptor
                    }
                }
            }

            // Find added plugins
            for ((pluginId, descriptor) in currentDescriptors) {
                if (pluginId !in discoveryCache) {
                    notifyListeners(PluginChangeEvent.Added(descriptor))
                    val sourcePath = (descriptor.source as? PluginSource.FileSystem)?.path ?: ""
                    discoveryCache[pluginId] = CachedDescriptor(
                        descriptor = descriptor,
                        path = sourcePath,
                        lastModified = fileIO.getLastModified(sourcePath)
                    )
                }
            }

            // Find removed plugins
            val removedIds = discoveryCache.keys - currentDescriptors.keys
            for (pluginId in removedIds) {
                notifyListeners(PluginChangeEvent.Removed(pluginId))
                discoveryCache.remove(pluginId)
            }

            // Find updated plugins
            for ((pluginId, descriptor) in currentDescriptors) {
                val cached = discoveryCache[pluginId] ?: continue
                val sourcePath = (descriptor.source as? PluginSource.FileSystem)?.path ?: ""
                val currentModified = if (sourcePath.isNotEmpty()) fileIO.getLastModified(sourcePath) else 0L

                if (currentModified > cached.lastModified || descriptor.version != cached.descriptor.version) {
                    notifyListeners(PluginChangeEvent.Updated(descriptor))
                    discoveryCache[pluginId] = CachedDescriptor(
                        descriptor = descriptor,
                        path = sourcePath,
                        lastModified = currentModified
                    )
                }
            }
        }
    }

    /**
     * Notify all listeners of a change event.
     */
    private fun notifyListeners(event: PluginChangeEvent) {
        changeListeners.forEach { listener ->
            try {
                listener(event)
            } catch (e: Exception) {
                // Listener error should not stop other listeners
            }
        }
    }

    /**
     * Add a plugin directory to the watch list.
     *
     * @param directory Directory path to add
     */
    fun addDirectory(directory: String) {
        if (directory !in pluginDirectories) {
            (pluginDirectories as? MutableList)?.add(directory)
        }
    }

    /**
     * Remove a plugin directory from the watch list.
     *
     * @param directory Directory path to remove
     */
    fun removeDirectory(directory: String) {
        (pluginDirectories as? MutableList)?.remove(directory)
    }

    /**
     * Get the list of configured plugin directories.
     *
     * @return List of directory paths
     */
    fun getDirectories(): List<String> = pluginDirectories.toList()

    /**
     * Get cached descriptor for a plugin.
     *
     * @param pluginId Plugin ID
     * @return Cached descriptor or null
     */
    suspend fun getCachedDescriptor(pluginId: String): PluginDescriptor? {
        return mutex.withLock {
            discoveryCache[pluginId]?.descriptor
        }
    }

    /**
     * Cached descriptor with metadata.
     */
    private data class CachedDescriptor(
        val descriptor: PluginDescriptor,
        val path: String,
        val lastModified: Long
    )

    companion object {
        /** Priority value for file system plugins. */
        const val PRIORITY_FILESYSTEM = 100

        /** Default manifest file name (AVU format). */
        const val MANIFEST_FILE_AVU = "plugin.avu"

        /** Alternate manifest file name (JSON format). */
        const val MANIFEST_FILE_JSON = "plugin.json"

        /** Default watch interval (5 seconds). */
        const val DEFAULT_WATCH_INTERVAL_MS = 5000L

        /** Minimum watch interval (1 second). */
        const val MIN_WATCH_INTERVAL_MS = 1000L

        /**
         * Create a discovery for a single directory.
         *
         * @param directory Plugin directory path
         * @param manifestReader Manifest reader
         * @param fileIO File I/O operations
         * @return FileSystemPluginDiscovery instance
         */
        fun forDirectory(
            directory: String,
            manifestReader: PluginManifestReader,
            fileIO: FileIO
        ): FileSystemPluginDiscovery {
            return FileSystemPluginDiscovery(
                pluginDirectories = listOf(directory),
                manifestReader = manifestReader,
                fileIO = fileIO
            )
        }
    }
}

/**
 * Event representing a change to a plugin.
 *
 * Sealed class representing the three types of plugin changes:
 * addition, removal, and update.
 *
 * @since 1.0.0
 */
sealed class PluginChangeEvent {
    /**
     * A new plugin was discovered.
     *
     * @property descriptor Descriptor of the new plugin
     */
    data class Added(val descriptor: PluginDescriptor) : PluginChangeEvent()

    /**
     * A plugin was removed.
     *
     * @property pluginId ID of the removed plugin
     */
    data class Removed(val pluginId: String) : PluginChangeEvent()

    /**
     * A plugin was updated.
     *
     * @property descriptor Updated descriptor of the plugin
     */
    data class Updated(val descriptor: PluginDescriptor) : PluginChangeEvent()
}

/**
 * Interface for custom plugin loading.
 *
 * Implement this interface to provide platform-specific plugin loading
 * mechanisms (e.g., class loading on JVM, dynamic frameworks on iOS).
 *
 * @since 1.0.0
 */
interface PluginLoader {
    /**
     * Load a plugin from a path.
     *
     * @param descriptor Plugin descriptor
     * @param path Path to plugin files
     * @return Result containing loaded plugin or error
     */
    fun loadPlugin(descriptor: PluginDescriptor, path: String): Result<UniversalPlugin>
}

/**
 * Exception thrown when no manifest is found.
 *
 * This is a normal condition for non-plugin directories and should
 * be handled silently.
 */
internal class NoManifestException(message: String) : Exception(message)

// Uses currentTimeMillis() from com.augmentalis.magiccode.plugins.universal
