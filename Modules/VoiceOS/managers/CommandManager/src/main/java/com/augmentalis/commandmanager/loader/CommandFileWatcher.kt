/**
 * CommandFileWatcher.kt - Watch JSON files for changes in developer mode
 *
 * Purpose: Auto-reload commands when JSON files change (developer mode only)
 * Features:
 * - Watch assets folder for JSON changes
 * - Debounced reload (avoids multiple reloads for batch edits)
 * - Enable/disable toggle
 * - Notification on reload
 *
 * Phase 2.4c: Dynamic Command Updates
 *
 * NOTE: This is primarily for development. In production, JSON files are in APK
 * and cannot be modified without app update.
 */

package com.augmentalis.commandmanager.loader

import android.content.Context
import android.os.FileObserver
import android.os.FileObserver.MODIFY
import android.os.FileObserver.CLOSE_WRITE
import android.os.FileObserver.CREATE
import android.os.FileObserver.DELETE
import android.os.FileObserver.MOVED_FROM
import android.os.FileObserver.MOVED_TO
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * File watcher for command JSON files
 * Useful for development when testing JSON changes
 *
 * Usage:
 * ```
 * val watcher = CommandFileWatcher(context, commandLoader)
 * watcher.startWatching()
 * // ... later ...
 * watcher.stopWatching()
 * ```
 */
class CommandFileWatcher(
    private val context: Context,
    private val commandLoader: CommandLoader,
    private val onReloadComplete: ((CommandLoader.LoadResult) -> Unit)? = null
) {

    companion object {
        private const val TAG = "CommandFileWatcher"
        private const val DEBOUNCE_DELAY_MS = 2000L // Wait 2s after last change before reload
    }

    private var fileObserver: FileObserver? = null
    private var isWatching = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var reloadJob: Job? = null

    /**
     * Start watching JSON files for changes
     *
     * NOTE: This only works if JSON files are in a writable directory.
     * In production APKs, JSON files are in assets (read-only), so this
     * is primarily useful for development builds with external JSON files.
     *
     * @param watchPath Optional custom path to watch (defaults to app's files directory)
     */
    fun startWatching(watchPath: String? = null) {
        if (isWatching) {
            Log.w(TAG, "Already watching files")
            return
        }

        val pathToWatch = watchPath ?: getDefaultWatchPath()
        val watchDir = File(pathToWatch)

        if (!watchDir.exists()) {
            Log.w(TAG, "Watch directory does not exist: $pathToWatch")
            Log.i(TAG, "Note: In production, JSON files are in APK assets (read-only)")
            return
        }

        Log.i(TAG, "Starting file watcher on: $pathToWatch")

        fileObserver = object : FileObserver(watchDir, MODIFY or CLOSE_WRITE or CREATE) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null && path.endsWith(".json")) {
                    Log.d(TAG, "JSON file changed: $path (event: ${eventToString(event)})")
                    scheduleReload()
                }
            }
        }

        fileObserver?.startWatching()
        isWatching = true
        Log.i(TAG, "File watcher started successfully")
    }

    /**
     * Stop watching files
     */
    fun stopWatching() {
        if (!isWatching) {
            Log.w(TAG, "Not currently watching files")
            return
        }

        Log.i(TAG, "Stopping file watcher")
        fileObserver?.stopWatching()
        fileObserver = null
        isWatching = false

        // Cancel any pending reload
        reloadJob?.cancel()
        reloadJob = null

        Log.i(TAG, "File watcher stopped")
    }

    /**
     * Schedule a reload with debouncing
     * Multiple file changes within DEBOUNCE_DELAY_MS will result in single reload
     */
    private fun scheduleReload() {
        // Cancel existing reload job
        reloadJob?.cancel()

        // Schedule new reload after debounce delay
        reloadJob = coroutineScope.launch {
            Log.d(TAG, "Scheduling reload (debounce: ${DEBOUNCE_DELAY_MS}ms)")
            delay(DEBOUNCE_DELAY_MS)

            Log.i(TAG, "Debounce period elapsed, reloading commands...")
            executeReload()
        }
    }

    /**
     * Execute the actual reload
     */
    private suspend fun executeReload() {
        try {
            val result = withContext(Dispatchers.IO) {
                commandLoader.forceReload()
            }

            when (result) {
                is CommandLoader.LoadResult.Success -> {
                    Log.i(TAG, "✅ Auto-reload successful: ${result.commandCount} commands")
                    onReloadComplete?.invoke(result)
                }
                is CommandLoader.LoadResult.Error -> {
                    Log.e(TAG, "❌ Auto-reload failed: ${result.message}")
                    onReloadComplete?.invoke(result)
                }
                else -> {
                    Log.w(TAG, "⚠️ Unexpected reload result: $result")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload commands", e)
            onReloadComplete?.invoke(CommandLoader.LoadResult.Error("Reload exception: ${e.message}"))
        }
    }

    /**
     * Get default watch path
     * Uses app's files directory for development JSON files
     */
    private fun getDefaultWatchPath(): String {
        // For development: watch app's files/localization/commands directory
        val filesDir = context.filesDir
        val localizationDir = File(filesDir, "localization/commands")

        // Create directory if it doesn't exist
        if (!localizationDir.exists()) {
            localizationDir.mkdirs()
            Log.i(TAG, "Created watch directory: ${localizationDir.absolutePath}")
        }

        return localizationDir.absolutePath
    }

    /**
     * Check if currently watching
     */
    fun isWatching(): Boolean = isWatching

    /**
     * Convert FileObserver event code to string for logging
     */
    private fun eventToString(event: Int): String {
        return when (event) {
            MODIFY -> "MODIFY"
            CLOSE_WRITE -> "CLOSE_WRITE"
            CREATE -> "CREATE"
            DELETE -> "DELETE"
            MOVED_FROM -> "MOVED_FROM"
            MOVED_TO -> "MOVED_TO"
            else -> "UNKNOWN($event)"
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopWatching()
        coroutineScope.cancel()
        Log.d(TAG, "Cleanup complete")
    }
}
