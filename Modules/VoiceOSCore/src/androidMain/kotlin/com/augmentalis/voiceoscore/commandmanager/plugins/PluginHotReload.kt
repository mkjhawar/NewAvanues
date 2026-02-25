/**
 * PluginHotReload.kt - Enhancement 3: Hot-Reload Development Mode
 *
 * Allows developers to reload plugins without restarting the app.
 * Watches plugin directory for file changes and automatically reloads.
 *
 * Part of Q12 Enhancement 3
 *
 * @since VOS4 Phase 4.1
 */

package com.augmentalis.voiceoscore.commandmanager.plugins

import android.os.FileObserver
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * Hot-reload service for plugin development
 *
 * Features:
 * - Watches plugin directory for file changes
 * - Automatically reloads modified plugins
 * - Debounces rapid file changes
 * - Notifies developer of reload status
 * - Only active when developer mode is enabled
 *
 * Use cases:
 * - Plugin development: Edit code, see changes instantly
 * - Testing: Iterate quickly without app restart
 * - Debugging: Fix issues and reload immediately
 *
 * SECURITY: Only enabled in developer mode (not in production)
 */
class PluginHotReload(
    private val pluginManager: PluginManager,
    private val pluginDirectory: File,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private const val TAG = "PluginHotReload"

        /** Debounce delay (milliseconds) - wait for file changes to settle */
        private const val DEBOUNCE_DELAY_MS = 500L

        /** Events to watch for */
        private const val WATCH_EVENTS = FileObserver.MODIFY or
                                        FileObserver.CLOSE_WRITE or
                                        FileObserver.DELETE or
                                        FileObserver.CREATE
    }

    /** File observer for plugin directory */
    private var fileObserver: FileObserver? = null

    /** Hot-reload enabled flag */
    private var isEnabled = false

    /** Pending reload jobs (debouncing) */
    private val pendingReloads = mutableMapOf<String, Job>()

    /** Hot-reload listeners */
    private val listeners = mutableListOf<HotReloadListener>()

    /** File-to-pluginId mapping for reload tracking */
    private val fileToPluginId = mutableMapOf<String, String>()

    /**
     * Start hot-reload monitoring
     *
     * Begins watching plugin directory for file changes.
     */
    fun start() {
        if (isEnabled) {
            Log.w(TAG, "Hot-reload already started")
            return
        }

        Log.i(TAG, "Starting hot-reload monitoring: ${pluginDirectory.absolutePath}")

        fileObserver = object : FileObserver(pluginDirectory, WATCH_EVENTS) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return

                // Only watch plugin files
                val file = File(pluginDirectory, path)
                if (!isPluginFile(file)) return

                when (event and ALL_EVENTS) {
                    MODIFY, CLOSE_WRITE -> handleFileModified(file)
                    DELETE -> handleFileDeleted(file)
                    CREATE -> handleFileCreated(file)
                }
            }
        }

        fileObserver?.startWatching()
        isEnabled = true

        Log.i(TAG, "Hot-reload monitoring started")
        notifyStarted()
    }

    /**
     * Stop hot-reload monitoring
     */
    fun stop() {
        if (!isEnabled) return

        Log.i(TAG, "Stopping hot-reload monitoring")

        fileObserver?.stopWatching()
        fileObserver = null
        isEnabled = false

        // Cancel pending reloads
        pendingReloads.values.forEach { it.cancel() }
        pendingReloads.clear()

        Log.i(TAG, "Hot-reload monitoring stopped")
        notifyStopped()
    }

    /**
     * Check if hot-reload is enabled
     */
    fun isHotReloadEnabled(): Boolean = isEnabled

    /**
     * Handle file modified event
     *
     * Debounces rapid changes and schedules plugin reload.
     */
    private fun handleFileModified(file: File) {
        if (!file.exists() || !file.canRead()) return

        Log.d(TAG, "Plugin file modified: ${file.name}")

        // Cancel existing pending reload for this file
        pendingReloads[file.name]?.cancel()

        // Schedule new reload after debounce delay
        val reloadJob = scope.launch {
            delay(DEBOUNCE_DELAY_MS)

            Log.i(TAG, "Hot-reloading plugin: ${file.name}")
            notifyReloadStarted(file.name)

            try {
                reloadPlugin(file)
                Log.i(TAG, "Hot-reload successful: ${file.name}")
                notifyReloadSuccess(file.name)
            } catch (e: Exception) {
                Log.e(TAG, "Hot-reload failed: ${file.name}", e)
                notifyReloadFailed(file.name, e)
            } finally {
                pendingReloads.remove(file.name)
            }
        }

        pendingReloads[file.name] = reloadJob
    }

    /**
     * Handle file deleted event
     */
    private fun handleFileDeleted(file: File) {
        Log.i(TAG, "Plugin file deleted: ${file.name}")

        val pluginId = getPluginIdFromFile(file)
        if (pluginId != null) {
            pluginManager.unloadPlugin(pluginId)
            notifyPluginDeleted(file.name)
        }
    }

    /**
     * Handle file created event
     */
    private fun handleFileCreated(file: File) {
        if (!file.exists() || !file.canRead()) return

        Log.i(TAG, "New plugin file detected: ${file.name}")

        // Wait a bit for file to be fully written
        scope.launch {
            delay(DEBOUNCE_DELAY_MS)

            try {
                pluginManager.loadPlugin(file)
                Log.i(TAG, "New plugin loaded: ${file.name}")
                notifyPluginAdded(file.name)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load new plugin: ${file.name}", e)
                notifyReloadFailed(file.name, e)
            }
        }
    }

    /**
     * Reload a plugin
     *
     * Steps:
     * 1. Unload existing plugin (if loaded)
     * 2. Load new version from file
     */
    private fun reloadPlugin(file: File) {
        // Get plugin ID from existing loaded plugin
        val pluginId = getPluginIdFromFile(file)

        // Unload existing plugin
        if (pluginId != null) {
            pluginManager.unloadPlugin(pluginId)
            Log.d(TAG, "Unloaded existing plugin: $pluginId")
        }

        // Load new version
        pluginManager.loadPlugin(file)
        Log.d(TAG, "Loaded new plugin version from: ${file.name}")
    }

    /**
     * Get plugin ID from file
     *
     * Looks up currently loaded plugins to find which one was loaded from this file.
     */
    private fun getPluginIdFromFile(file: File): String? {
        // Use tracked mapping if available
        val filePath = file.absolutePath
        return fileToPluginId[filePath] ?: file.nameWithoutExtension
    }

    /**
     * Register file-to-pluginId mapping
     * Call this when a plugin is loaded to enable hot-reload tracking
     */
    fun registerPluginFile(file: File, pluginId: String) {
        fileToPluginId[file.absolutePath] = pluginId
        Log.d(TAG, "Registered plugin mapping: ${file.name} -> $pluginId")
    }

    /**
     * Unregister file-to-pluginId mapping
     * Call this when a plugin is unloaded
     */
    fun unregisterPluginFile(file: File) {
        fileToPluginId.remove(file.absolutePath)
        Log.d(TAG, "Unregistered plugin mapping: ${file.name}")
    }

    /**
     * Get all registered plugin mappings
     */
    fun getPluginMappings(): Map<String, String> = fileToPluginId.toMap()

    /**
     * Check if file is a plugin file
     */
    private fun isPluginFile(file: File): Boolean {
        return file.extension.lowercase() in setOf("apk", "jar")
    }

    /**
     * Add hot-reload listener
     */
    fun addListener(listener: HotReloadListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    /**
     * Remove hot-reload listener
     */
    fun removeListener(listener: HotReloadListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    /**
     * Notify listeners that monitoring started
     */
    private fun notifyStarted() {
        synchronized(listeners) {
            listeners.forEach { it.onHotReloadStarted() }
        }
    }

    /**
     * Notify listeners that monitoring stopped
     */
    private fun notifyStopped() {
        synchronized(listeners) {
            listeners.forEach { it.onHotReloadStopped() }
        }
    }

    /**
     * Notify listeners that reload started
     */
    private fun notifyReloadStarted(fileName: String) {
        synchronized(listeners) {
            listeners.forEach { it.onReloadStarted(fileName) }
        }
    }

    /**
     * Notify listeners that reload succeeded
     */
    private fun notifyReloadSuccess(fileName: String) {
        synchronized(listeners) {
            listeners.forEach { it.onReloadSuccess(fileName) }
        }
    }

    /**
     * Notify listeners that reload failed
     */
    private fun notifyReloadFailed(fileName: String, error: Throwable) {
        synchronized(listeners) {
            listeners.forEach { it.onReloadFailed(fileName, error) }
        }
    }

    /**
     * Notify listeners that plugin was deleted
     */
    private fun notifyPluginDeleted(fileName: String) {
        synchronized(listeners) {
            listeners.forEach { it.onPluginDeleted(fileName) }
        }
    }

    /**
     * Notify listeners that plugin was added
     */
    private fun notifyPluginAdded(fileName: String) {
        synchronized(listeners) {
            listeners.forEach { it.onPluginAdded(fileName) }
        }
    }

    /**
     * Manually trigger reload of a specific plugin
     *
     * Useful for testing or forcing a reload.
     *
     * @param fileName Plugin file name
     */
    fun manualReload(fileName: String) {
        val file = File(pluginDirectory, fileName)

        if (!file.exists()) {
            throw IllegalArgumentException("Plugin file not found: $fileName")
        }

        if (!isPluginFile(file)) {
            throw IllegalArgumentException("Not a plugin file: $fileName")
        }

        scope.launch {
            try {
                Log.i(TAG, "Manual reload triggered: $fileName")
                reloadPlugin(file)
                notifyReloadSuccess(fileName)
            } catch (e: Exception) {
                Log.e(TAG, "Manual reload failed: $fileName", e)
                notifyReloadFailed(fileName, e)
                throw e
            }
        }
    }

    /**
     * Get list of watched files
     */
    fun getWatchedFiles(): List<String> {
        return pluginDirectory.listFiles { file ->
            isPluginFile(file)
        }?.map { it.name } ?: emptyList()
    }
}

/**
 * Hot-reload event listener
 *
 * Implement this to receive notifications about hot-reload events.
 * Useful for displaying reload status in developer UI.
 */
interface HotReloadListener {
    /**
     * Called when hot-reload monitoring starts
     */
    fun onHotReloadStarted()

    /**
     * Called when hot-reload monitoring stops
     */
    fun onHotReloadStopped()

    /**
     * Called when a plugin reload starts
     *
     * @param fileName Plugin file being reloaded
     */
    fun onReloadStarted(fileName: String)

    /**
     * Called when a plugin reload succeeds
     *
     * @param fileName Plugin file that was reloaded
     */
    fun onReloadSuccess(fileName: String)

    /**
     * Called when a plugin reload fails
     *
     * @param fileName Plugin file that failed to reload
     * @param error Error that occurred
     */
    fun onReloadFailed(fileName: String, error: Throwable)

    /**
     * Called when a plugin file is deleted
     *
     * @param fileName Plugin file that was deleted
     */
    fun onPluginDeleted(fileName: String)

    /**
     * Called when a new plugin file is added
     *
     * @param fileName Plugin file that was added
     */
    fun onPluginAdded(fileName: String)
}

/**
 * Simple hot-reload listener adapter
 *
 * Override only the methods you need.
 */
open class HotReloadListenerAdapter : HotReloadListener {
    override fun onHotReloadStarted() {}
    override fun onHotReloadStopped() {}
    override fun onReloadStarted(fileName: String) {}
    override fun onReloadSuccess(fileName: String) {}
    override fun onReloadFailed(fileName: String, error: Throwable) {}
    override fun onPluginDeleted(fileName: String) {}
    override fun onPluginAdded(fileName: String) {}
}
