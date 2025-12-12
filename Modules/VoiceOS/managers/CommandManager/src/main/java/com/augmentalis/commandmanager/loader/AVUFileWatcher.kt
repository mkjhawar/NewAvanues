/**
 * AVUFileWatcher.kt - Watch learned_apps folder for new AVU format .vos files
 *
 * Purpose: Auto-ingest learned app commands when LearnApp exports new .vos files
 * Features:
 * - Watch learned_apps folder for new .vos files
 * - Debounced ingestion (avoids multiple reloads)
 * - Auto-ingest on file creation
 * - Integration with VOSCommandIngestion
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-AVU-UNIVERSAL-FORMAT-SPEC-50312-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.commandmanager.loader

import android.content.Context
import android.os.FileObserver
import android.os.FileObserver.CLOSE_WRITE
import android.os.FileObserver.CREATE
import android.os.FileObserver.MOVED_TO
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * Result of AVU file ingestion
 */
data class AVUIngestionResult(
    val success: Boolean,
    val filesProcessed: Int,
    val commandsIngested: Int,
    val errors: List<String>,
    val durationMs: Long
)

/**
 * AVU File Watcher
 *
 * Watches the learned_apps folder for new .vos files exported by LearnApp
 * and automatically ingests them into the command database.
 *
 * Usage:
 * ```kotlin
 * val watcher = AVUFileWatcher(context, ingestion)
 * watcher.onIngestionComplete = { result ->
 *     Log.i(TAG, "Ingested ${result.commandsIngested} learned commands")
 * }
 * watcher.startWatching()
 *
 * // ... later ...
 * watcher.stopWatching()
 * ```
 */
class AVUFileWatcher(
    private val context: Context,
    private val ingestion: VOSCommandIngestion
) {

    companion object {
        private const val TAG = "AVUFileWatcher"
        private const val DEBOUNCE_DELAY_MS = 3000L // Wait 3s after last change
        private const val LEARNED_APPS_FOLDER = "learned_apps"
    }

    private var fileObserver: FileObserver? = null
    private var isWatching = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var ingestionJob: Job? = null

    // Callback for ingestion completion
    var onIngestionComplete: ((AVUIngestionResult) -> Unit)? = null

    // AVU parser instance
    private val avuParser by lazy { AVUFileParser(context) }

    /**
     * Get the learned apps folder path
     */
    fun getLearnedAppsFolder(): File {
        return File(context.getExternalFilesDir(null), LEARNED_APPS_FOLDER)
    }

    /**
     * Start watching the learned_apps folder
     *
     * Creates the folder if it doesn't exist.
     */
    fun startWatching() {
        if (isWatching) {
            Log.w(TAG, "Already watching learned_apps folder")
            return
        }

        val watchDir = getLearnedAppsFolder()

        // Create directory if it doesn't exist
        if (!watchDir.exists()) {
            watchDir.mkdirs()
            Log.i(TAG, "Created learned_apps folder: ${watchDir.absolutePath}")
        }

        Log.i(TAG, "Starting AVU file watcher on: ${watchDir.absolutePath}")

        fileObserver = object : FileObserver(watchDir, CREATE or CLOSE_WRITE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null && path.endsWith(".vos")) {
                    Log.d(TAG, "AVU file detected: $path (event: ${eventToString(event)})")
                    scheduleIngestion()
                }
            }
        }

        fileObserver?.startWatching()
        isWatching = true

        // Initial ingestion of existing files
        coroutineScope.launch {
            delay(1000) // Small delay to let system settle
            ingestExistingFiles()
        }

        Log.i(TAG, "AVU file watcher started")
    }

    /**
     * Stop watching
     */
    fun stopWatching() {
        if (!isWatching) {
            Log.w(TAG, "Not currently watching")
            return
        }

        Log.i(TAG, "Stopping AVU file watcher")
        fileObserver?.stopWatching()
        fileObserver = null
        isWatching = false

        ingestionJob?.cancel()
        ingestionJob = null

        Log.i(TAG, "AVU file watcher stopped")
    }

    /**
     * Schedule ingestion with debouncing
     */
    private fun scheduleIngestion() {
        ingestionJob?.cancel()

        ingestionJob = coroutineScope.launch {
            Log.d(TAG, "Scheduling AVU ingestion (debounce: ${DEBOUNCE_DELAY_MS}ms)")
            delay(DEBOUNCE_DELAY_MS)
            Log.i(TAG, "Debounce complete, starting AVU ingestion...")
            executeIngestion()
        }
    }

    /**
     * Ingest existing .vos files in the learned_apps folder
     */
    private suspend fun ingestExistingFiles() {
        val folder = getLearnedAppsFolder()
        val vosFiles = folder.listFiles { file ->
            file.isFile && file.extension == "vos"
        } ?: emptyArray()

        if (vosFiles.isEmpty()) {
            Log.i(TAG, "No existing .vos files in learned_apps folder")
            return
        }

        Log.i(TAG, "Found ${vosFiles.size} existing .vos files, ingesting...")
        executeIngestion()
    }

    /**
     * Execute the actual AVU ingestion
     */
    private suspend fun executeIngestion() {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var filesProcessed = 0
        var commandsIngested = 0

        try {
            withContext(Dispatchers.IO) {
                val folder = getLearnedAppsFolder()

                // Parse all AVU files
                val parseResult = avuParser.parseFolder(folder.absolutePath)

                parseResult.onSuccess { parsedFiles ->
                    filesProcessed = parsedFiles.size
                    Log.i(TAG, "Parsed $filesProcessed AVU files")

                    // Convert and ingest each file
                    parsedFiles.forEach { avuFile ->
                        try {
                            val entities = avuParser.convertToEntities(avuFile)
                            if (entities.isNotEmpty()) {
                                val result = ingestion.ingestAVUEntities(
                                    entities = entities,
                                    packageName = avuFile.packageName,
                                    appName = avuFile.appName
                                )
                                if (result.success) {
                                    commandsIngested += result.commandsLoaded
                                    Log.d(TAG, "Ingested ${result.commandsLoaded} commands from ${avuFile.packageName}")
                                } else {
                                    errors.addAll(result.errors)
                                }
                            }
                        } catch (e: Exception) {
                            val error = "Failed to ingest ${avuFile.packageName}: ${e.message}"
                            Log.w(TAG, error)
                            errors.add(error)
                        }
                    }
                }.onFailure { e ->
                    val error = "Failed to parse AVU folder: ${e.message}"
                    Log.e(TAG, error, e)
                    errors.add(error)
                }
            }

            val duration = System.currentTimeMillis() - startTime
            val result = AVUIngestionResult(
                success = errors.isEmpty(),
                filesProcessed = filesProcessed,
                commandsIngested = commandsIngested,
                errors = errors,
                durationMs = duration
            )

            Log.i(TAG, "AVU ingestion complete: $commandsIngested commands from $filesProcessed files in ${duration}ms")

            // Callback
            onIngestionComplete?.invoke(result)

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "AVU ingestion failed", e)

            val result = AVUIngestionResult(
                success = false,
                filesProcessed = filesProcessed,
                commandsIngested = commandsIngested,
                errors = errors + "Exception: ${e.message}",
                durationMs = duration
            )

            onIngestionComplete?.invoke(result)
        }
    }

    /**
     * Manually trigger ingestion
     */
    fun triggerIngestion() {
        coroutineScope.launch {
            executeIngestion()
        }
    }

    /**
     * Check if watching
     */
    fun isWatching(): Boolean = isWatching

    /**
     * Get count of .vos files in learned_apps folder
     */
    fun getVosFileCount(): Int {
        val folder = getLearnedAppsFolder()
        return folder.listFiles { file ->
            file.isFile && file.extension == "vos"
        }?.size ?: 0
    }

    /**
     * Convert event code to string
     */
    private fun eventToString(event: Int): String {
        return when (event) {
            CREATE -> "CREATE"
            CLOSE_WRITE -> "CLOSE_WRITE"
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
        Log.d(TAG, "AVUFileWatcher cleanup complete")
    }
}
