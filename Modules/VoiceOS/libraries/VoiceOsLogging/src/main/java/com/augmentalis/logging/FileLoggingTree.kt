/**
 * FileLoggingTree.kt - File-Based Logging Tree for Timber
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-23 16:41:59 PDT
 * Part of: VOS4 Phase 3 - Conciseness Refactoring
 *
 * Purpose:
 * Custom Timber.Tree that logs to local files with daily rotation and export
 * capabilities. Replaces VoiceOsLogger's file logging functionality.
 *
 * Features:
 * - Daily log file rotation (one file per day)
 * - Automatic old log cleanup (configurable retention)
 * - Thread-safe file I/O (using coroutines)
 * - Export logs to external storage
 * - Log level filtering
 * - Structured log format (timestamp, level, tag, message)
 *
 * Usage:
 * ```kotlin
 * val fileTree = FileLoggingTree(context, retentionDays = 7)
 * Timber.plant(fileTree)
 * Timber.d("This will be logged to file")
 *
 * // Export logs
 * fileTree.exportLogs(destinationPath)
 * ```
 */
package com.augmentalis.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * File Logging Tree - Logs to daily rotating files
 *
 * Thread Safety:
 * - Uses ReentrantReadWriteLock for file access
 * - Async I/O on Dispatchers.IO
 * - Safe for concurrent logging from multiple threads
 *
 * File Format:
 * ```
 * 2025-10-23 16:42:00.123 D/MyTag: Log message
 * 2025-10-23 16:42:01.456 E/MyTag: Error message
 * ```
 *
 * File Naming:
 * - Pattern: voiceos_YYYYMMDD.log
 * - Example: voiceos_20251023.log
 * - Location: app's files directory (internal storage)
 *
 * @param context Application context for file access
 * @param retentionDays Number of days to retain logs (default: 7)
 * @param minLogLevel Minimum log level to write (default: Log.DEBUG)
 */
class FileLoggingTree(
    context: Context,
    private val retentionDays: Int = 7,
    private val minLogLevel: Int = Log.DEBUG
) : Timber.Tree() {

    private val appContext = context.applicationContext
    private val logDir = File(appContext.filesDir, "logs")
    private val lock = ReentrantReadWriteLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Date format for log timestamps
     * Example: 2025-10-23 16:42:00.123
     */
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * Date format for log file names
     * Example: 20251023
     */
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    init {
        // Ensure log directory exists
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        // Clean up old logs
        cleanupOldLogs()
    }

    /**
     * Log message to file
     *
     * Called by Timber for each log statement. Formats the message and
     * appends it to today's log file asynchronously.
     *
     * @param priority Log level (Log.DEBUG, Log.INFO, etc.)
     * @param tag Log tag (usually class name)
     * @param message Log message (already formatted)
     * @param t Optional throwable for errors
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Filter by minimum log level
        if (priority < minLogLevel) return

        // Format log entry
        val timestamp = timestampFormat.format(Date())
        val level = priorityToString(priority)
        val logTag = tag ?: "Unknown"
        val logEntry = buildString {
            append(timestamp)
            append(" ")
            append(level)
            append("/")
            append(logTag)
            append(": ")
            append(message)

            // Append exception stack trace if present
            if (t != null) {
                append("\n")
                append(Log.getStackTraceString(t))
            }

            append("\n")
        }

        // Write to file asynchronously
        scope.launch {
            writeToFile(logEntry)
        }
    }

    /**
     * Write log entry to current day's file
     *
     * Thread-safe file writing with proper locking.
     *
     * @param logEntry Formatted log entry to write
     */
    private fun writeToFile(logEntry: String) {
        lock.write {
            try {
                val logFile = getCurrentLogFile()
                logFile.appendText(logEntry)
            } catch (e: Exception) {
                // Fallback to Android log if file write fails
                Log.e("FileLoggingTree", "Failed to write to log file", e)
            }
        }
    }

    /**
     * Get current day's log file
     *
     * Creates file if it doesn't exist.
     *
     * @return Log file for current day
     */
    private fun getCurrentLogFile(): File {
        val today = fileNameDateFormat.format(Date())
        val fileName = "voiceos_$today.log"
        return File(logDir, fileName)
    }

    /**
     * Convert priority int to string
     *
     * @param priority Log level constant
     * @return Single-character level string (D/I/W/E/V/A)
     */
    private fun priorityToString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }
    }

    /**
     * Clean up old log files
     *
     * Deletes files older than retentionDays. Called during initialization
     * and can be called manually.
     */
    fun cleanupOldLogs() {
        scope.launch {
            lock.write {
                try {
                    val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
                    logDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < cutoffTime) {
                            file.delete()
                            Log.d("FileLoggingTree", "Deleted old log: ${file.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FileLoggingTree", "Failed to cleanup old logs", e)
                }
            }
        }
    }

    /**
     * Export all logs to destination directory
     *
     * Copies all log files to the specified destination (e.g., external storage).
     *
     * @param destinationDir Destination directory for exported logs
     * @return Number of files exported
     * @throws IllegalArgumentException if destination is not a directory
     */
    suspend fun exportLogs(destinationDir: File): Int {
        require(destinationDir.isDirectory) { "Destination must be a directory" }

        var exportedCount = 0

        lock.read {
            try {
                logDir.listFiles()?.forEach { sourceFile ->
                    val destFile = File(destinationDir, sourceFile.name)
                    sourceFile.copyTo(destFile, overwrite = true)
                    exportedCount++
                }
            } catch (e: Exception) {
                Log.e("FileLoggingTree", "Failed to export logs", e)
                throw e
            }
        }

        return exportedCount
    }

    /**
     * Get all log files
     *
     * @return List of log files, sorted by date (newest first)
     */
    fun getLogFiles(): List<File> {
        return lock.read {
            logDir.listFiles()
                ?.filter { it.extension == "log" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    /**
     * Get total size of all log files
     *
     * @return Total size in bytes
     */
    fun getTotalLogSize(): Long {
        return lock.read {
            logDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
    }

    /**
     * Clear all log files
     *
     * **Warning:** This deletes all logs permanently.
     */
    fun clearAllLogs() {
        scope.launch {
            lock.write {
                try {
                    logDir.listFiles()?.forEach { it.delete() }
                    Log.d("FileLoggingTree", "All logs cleared")
                } catch (e: Exception) {
                    Log.e("FileLoggingTree", "Failed to clear logs", e)
                }
            }
        }
    }
}
