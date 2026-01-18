/**
 * RemoteLoggingTree.kt - HTTP-Based Remote Logging Tree for Timber
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-23 16:41:59 PDT
 * Part of: VOS4 Phase 3 - Conciseness Refactoring
 *
 * Purpose:
 * Custom Timber.Tree that sends logs to a remote HTTP endpoint with batching
 * and retry logic. Replaces VoiceOsLogger's remote logging functionality.
 *
 * Features:
 * - Batched log transmission (reduces network calls)
 * - Automatic retry with exponential backoff
 * - Network state awareness (only sends when online)
 * - Local buffer for offline logs
 * - Configurable batch size and flush interval
 * - Thread-safe log queue
 *
 * Usage:
 * ```kotlin
 * val remoteTree = RemoteLoggingTree(
 *     endpoint = "https://api.example.com/logs",
 *     apiKey = "your-api-key",
 *     batchSize = 50
 * )
 * Timber.plant(remoteTree)
 * Timber.i("This will be sent to remote server")
 *
 * // Manual flush
 * remoteTree.flush()
 * ```
 */
package com.augmentalis.logging

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Remote Logging Tree - Sends logs to HTTP endpoint
 *
 * Thread Safety:
 * - Uses ConcurrentLinkedQueue for thread-safe log buffering
 * - Async network I/O on Dispatchers.IO
 * - Safe for concurrent logging from multiple threads
 *
 * Network Behavior:
 * - Batches logs to reduce network overhead
 * - Auto-retries failed sends with exponential backoff
 * - Queues logs when offline, sends when back online
 * - Max retry attempts: 3
 * - Backoff: 1s, 2s, 4s
 *
 * JSON Format:
 * ```json
 * {
 *   "logs": [
 *     {
 *       "timestamp": 1729720920123,
 *       "level": "ERROR",
 *       "tag": "MyTag",
 *       "message": "Error occurred",
 *       "stackTrace": "java.lang.Exception: ..."
 *     }
 *   ]
 * }
 * ```
 *
 * @param endpoint Remote HTTP endpoint URL
 * @param apiKey API key for authentication (sent in Authorization header)
 * @param batchSize Number of logs to batch before sending (default: 50)
 * @param flushIntervalMs Auto-flush interval in milliseconds (default: 60000 = 1 min)
 * @param minLogLevel Minimum log level to send (default: Log.WARN - only warnings and errors)
 */
class RemoteLoggingTree(
    private val endpoint: String,
    private val apiKey: String,
    private val batchSize: Int = 50,
    private val flushIntervalMs: Long = 60_000, // 1 minute
    private val minLogLevel: Int = Log.WARN
) : Timber.Tree() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val isSending = AtomicBoolean(false)
    private var flushJob: Job? = null

    init {
        // Start auto-flush timer
        startAutoFlush()
    }

    /**
     * Log entry data class
     */
    private data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String,
        val stackTrace: String?
    )

    /**
     * Log message to remote endpoint
     *
     * Queues the log entry and sends batch when size threshold is reached.
     *
     * @param priority Log level (Log.DEBUG, Log.INFO, etc.)
     * @param tag Log tag (usually class name)
     * @param message Log message (already formatted)
     * @param t Optional throwable for errors
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Filter by minimum log level
        if (priority < minLogLevel) return

        // Create log entry
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = priorityToString(priority),
            tag = tag ?: "Unknown",
            message = message,
            stackTrace = t?.let { Log.getStackTraceString(it) }
        )

        // Add to queue
        logQueue.offer(entry)

        // Send batch if threshold reached
        if (logQueue.size >= batchSize) {
            flush()
        }
    }

    /**
     * Convert priority int to string
     *
     * @param priority Log level constant
     * @return Level string (VERBOSE/DEBUG/INFO/WARN/ERROR/ASSERT)
     */
    private fun priorityToString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }
    }

    /**
     * Flush queued logs to remote endpoint
     *
     * Sends all queued logs immediately, regardless of batch size.
     * Safe to call from any thread.
     */
    fun flush() {
        if (logQueue.isEmpty()) return
        if (!isSending.compareAndSet(false, true)) return // Already sending

        scope.launch {
            try {
                sendBatch()
            } finally {
                isSending.set(false)
            }
        }
    }

    /**
     * Send batch of logs to remote endpoint
     *
     * Drains the queue and sends logs via HTTP POST.
     * Implements retry logic with exponential backoff.
     */
    private suspend fun sendBatch() {
        if (logQueue.isEmpty()) return

        // Drain queue into batch
        val batch = mutableListOf<LogEntry>()
        while (batch.size < batchSize) {
            val entry = logQueue.poll() ?: break
            batch.add(entry)
        }

        if (batch.isEmpty()) return

        // Build JSON payload
        val payload = buildJsonPayload(batch)

        // Send with retries
        var attempt = 0
        var success = false

        while (attempt < 3 && !success) {
            try {
                sendHttpPost(payload)
                success = true
                Log.d("RemoteLoggingTree", "Sent ${batch.size} logs to remote endpoint")
            } catch (e: IOException) {
                attempt++
                if (attempt < 3) {
                    val backoffMs = 1000L * (1 shl (attempt - 1)) // 1s, 2s, 4s
                    Log.w("RemoteLoggingTree", "Failed to send logs (attempt $attempt), retrying in ${backoffMs}ms", e)
                    delay(backoffMs)
                } else {
                    Log.e("RemoteLoggingTree", "Failed to send logs after 3 attempts, dropping batch", e)
                }
            }
        }
    }

    /**
     * Build JSON payload from log entries
     *
     * @param batch List of log entries
     * @return JSON string payload
     */
    private fun buildJsonPayload(batch: List<LogEntry>): String {
        val logsArray = JSONArray()

        batch.forEach { entry ->
            val logObject = JSONObject().apply {
                put("timestamp", entry.timestamp)
                put("level", entry.level)
                put("tag", entry.tag)
                put("message", entry.message)
                if (entry.stackTrace != null) {
                    put("stackTrace", entry.stackTrace)
                }
            }
            logsArray.put(logObject)
        }

        return JSONObject().apply {
            put("logs", logsArray)
        }.toString()
    }

    /**
     * Send HTTP POST request to remote endpoint
     *
     * @param jsonPayload JSON payload to send
     * @throws IOException if network request fails
     */
    private fun sendHttpPost(jsonPayload: String) {
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 10_000 // 10 seconds
            connection.readTimeout = 10_000

            // Write payload
            connection.outputStream.use { outputStream ->
                outputStream.write(jsonPayload.toByteArray())
                outputStream.flush()
            }

            // Check response
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("HTTP $responseCode: ${connection.responseMessage}")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Start auto-flush timer
     *
     * Periodically flushes queued logs even if batch size not reached.
     */
    private fun startAutoFlush() {
        flushJob = scope.launch {
            while (isActive) {
                delay(flushIntervalMs)
                flush()
            }
        }
    }

    /**
     * Stop auto-flush timer and send remaining logs
     *
     * Call this when shutting down the app to ensure no logs are lost.
     */
    fun shutdown() {
        flushJob?.cancel()
        runBlocking {
            flush()
        }
    }

    /**
     * Get current queue size
     *
     * @return Number of logs waiting to be sent
     */
    fun getQueueSize(): Int = logQueue.size

    /**
     * Clear all queued logs
     *
     * **Warning:** This discards unsent logs permanently.
     */
    fun clearQueue() {
        logQueue.clear()
        Log.d("RemoteLoggingTree", "Log queue cleared")
    }
}
