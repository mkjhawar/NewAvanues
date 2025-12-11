package com.avanues.voiceos.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * VoiceOS ContentProvider for Command Execution
 *
 * Provides IPC endpoints for AVA and other apps to delegate command execution to VoiceOS.
 *
 * Endpoints:
 * - content://com.avanues.voiceos.provider/execute_command (INSERT)
 * - content://com.avanues.voiceos.provider/execution_result/{execution_id} (QUERY)
 *
 * Architecture:
 * 1. Apps INSERT execution request → Returns execution_id
 * 2. VoiceOS queues command for execution
 * 3. AccessibilityService executes command steps
 * 4. Apps QUERY execution_result/{id} → Returns status/message/steps
 *
 * Reference: Developer Manual Chapter 36, ADR-006
 */
class VoiceOSProvider : ContentProvider() {

    companion object {
        private const val TAG = "VoiceOSProvider"

        // Authority and URIs
        const val AUTHORITY = "com.avanues.voiceos.provider"
        private val BASE_URI = Uri.parse("content://$AUTHORITY")

        // URI codes
        private const val EXECUTE_COMMAND = 1
        private const val EXECUTION_RESULT = 2

        // URI matcher
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "execute_command", EXECUTE_COMMAND)
            addURI(AUTHORITY, "execution_result/*", EXECUTION_RESULT)
        }

        // Database configuration
        private const val DATABASE_NAME = "voiceos_execution.db"
        private const val DATABASE_VERSION = 1
    }

    private lateinit var dbHelper: DatabaseHelper
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // In-memory queue for active executions (complementing DB storage)
    private val activeExecutions = ConcurrentHashMap<String, ExecutionState>()

    /**
     * Execution state for active commands
     */
    private data class ExecutionState(
        val executionId: String,
        val commandId: String,
        val parameters: Map<String, String>,
        val requestedBy: String,
        var status: String, // pending, executing, success, error
        var message: String? = null,
        var executedSteps: Int = 0,
        var executionTimeMs: Long = 0,
        var failedAtStep: Int? = null,
        val startTime: Long = System.currentTimeMillis()
    )

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context!!)
        Log.d(TAG, "VoiceOSProvider initialized")
        return true
    }

    /**
     * INSERT /execute_command
     *
     * Request execution of a command.
     *
     * @param uri content://com.avanues.voiceos.provider/execute_command
     * @param values ContentValues with:
     *   - command_id (String): Command ID from VoiceOS database
     *   - parameters (String): JSON-encoded parameters
     *   - requested_by (String): Package name of requesting app
     * @return Uri with execution_id appended
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != EXECUTE_COMMAND) {
            throw IllegalArgumentException("Unknown URI: $uri")
        }

        if (values == null) {
            Log.e(TAG, "Insert called with null values")
            return null
        }

        try {
            // Extract request data
            val commandId = values.getAsString("command_id")
            val parametersJson = values.getAsString("parameters") ?: "{}"
            val requestedBy = values.getAsString("requested_by")

            if (commandId == null || requestedBy == null) {
                Log.e(TAG, "Missing required fields: command_id or requested_by")
                return null
            }

            // Parse parameters
            val parameters = try {
                val json = JSONObject(parametersJson)
                json.keys().asSequence().associateWith { json.getString(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse parameters JSON: ${e.message}")
                emptyMap()
            }

            // Generate execution ID
            val executionId = UUID.randomUUID().toString()
            val requestedAt = System.currentTimeMillis()

            // Insert into database
            val db = dbHelper.writableDatabase
            val insertValues = ContentValues().apply {
                put("execution_id", executionId)
                put("command_id", commandId)
                put("parameters", parametersJson)
                put("requested_by", requestedBy)
                put("requested_at", requestedAt)
                put("status", "pending")
                put("executed_steps", 0)
            }

            val rowId = db.insert("execution_requests", null, insertValues)

            if (rowId == -1L) {
                Log.e(TAG, "Failed to insert execution request into database")
                return null
            }

            // Create in-memory execution state
            val executionState = ExecutionState(
                executionId = executionId,
                commandId = commandId,
                parameters = parameters,
                requestedBy = requestedBy,
                status = "pending"
            )

            activeExecutions[executionId] = executionState

            // Queue command for execution (async)
            coroutineScope.launch {
                queueCommandExecution(executionState)
            }

            // Return URI with execution_id
            val resultUri = ContentUris.withAppendedId(BASE_URI, executionId.hashCode().toLong())

            Log.d(TAG, "Execution request created: $executionId for command: $commandId")

            // Notify observers
            context?.contentResolver?.notifyChange(resultUri, null)

            return resultUri

        } catch (e: Exception) {
            Log.e(TAG, "Error inserting execution request: ${e.message}", e)
            return null
        }
    }

    /**
     * QUERY /execution_result/{execution_id}
     *
     * Query execution status and result.
     *
     * @param uri content://com.avanues.voiceos.provider/execution_result/{execution_id}
     * @return Cursor with columns: status, message, executed_steps, execution_time_ms, failed_at_step
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != EXECUTION_RESULT) {
            throw IllegalArgumentException("Unknown URI: $uri")
        }

        val executionId = uri.lastPathSegment

        if (executionId == null) {
            Log.e(TAG, "Query called without execution_id")
            return null
        }

        try {
            val db = dbHelper.readableDatabase

            val cursor = db.query(
                "execution_requests",
                arrayOf(
                    "status",
                    "message",
                    "executed_steps",
                    "execution_time_ms",
                    "failed_at_step"
                ),
                "execution_id = ?",
                arrayOf(executionId),
                null,
                null,
                null
            )

            cursor?.setNotificationUri(context?.contentResolver, uri)

            return cursor

        } catch (e: Exception) {
            Log.e(TAG, "Error querying execution result: ${e.message}", e)
            return null
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Update not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Delete not supported")
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            EXECUTE_COMMAND -> "vnd.android.cursor.item/vnd.voiceos.execution"
            EXECUTION_RESULT -> "vnd.android.cursor.item/vnd.voiceos.result"
            else -> null
        }
    }

    // ========================================
    // Command Execution Queue
    // ========================================

    /**
     * Queue command for execution by AccessibilityService
     *
     * Process:
     * 1. Insert into command_queue table (FIFO)
     * 2. Notify CommandQueueProcessor
     * 3. AccessibilityService pulls from queue
     * 4. Executes command steps
     * 5. Updates execution_requests table
     */
    private suspend fun queueCommandExecution(executionState: ExecutionState) {
        withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.writableDatabase

                // Insert into command_queue
                val queueValues = ContentValues().apply {
                    put("execution_id", executionState.executionId)
                    put("priority", 0) // Default priority
                    put("queued_at", System.currentTimeMillis())
                }

                db.insert("command_queue", null, queueValues)

                Log.d(TAG, "Command queued for execution: ${executionState.executionId}")

                // Notify CommandQueueProcessor to pick up the command
                // (In production, this would broadcast to AccessibilityService)
                notifyCommandQueued(executionState.executionId)

            } catch (e: Exception) {
                Log.e(TAG, "Error queuing command: ${e.message}", e)
                updateExecutionStatus(
                    executionState.executionId,
                    "error",
                    "Failed to queue command: ${e.message}",
                    failedAtStep = null
                )
            }
        }
    }

    /**
     * Notify AccessibilityService that command is queued
     *
     * Production implementation:
     * - Send broadcast to AccessibilityService
     * - AccessibilityService pulls from command_queue
     * - Executes command hierarchy
     * - Calls updateExecutionStatus() with result
     */
    private fun notifyCommandQueued(executionId: String) {
        // TODO: Broadcast to AccessibilityService
        // val intent = Intent("com.avanues.voiceos.COMMAND_QUEUED")
        // intent.putExtra("execution_id", executionId)
        // context?.sendBroadcast(intent)

        Log.d(TAG, "Command queue notification sent for: $executionId")
    }

    /**
     * Update execution status (called by AccessibilityService)
     *
     * @param executionId Execution ID
     * @param status New status (pending, executing, success, error)
     * @param message Status message
     * @param executedSteps Number of steps executed
     * @param executionTimeMs Total execution time
     * @param failedAtStep Step number where execution failed (null if not applicable)
     */
    fun updateExecutionStatus(
        executionId: String,
        status: String,
        message: String? = null,
        executedSteps: Int = 0,
        executionTimeMs: Long = 0,
        failedAtStep: Int? = null
    ) {
        try {
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put("status", status)
                put("message", message)
                put("executed_steps", executedSteps)
                put("execution_time_ms", executionTimeMs)
                put("failed_at_step", failedAtStep)

                if (status == "executing" && executedSteps == 0) {
                    put("started_at", System.currentTimeMillis())
                }

                if (status == "success" || status == "error") {
                    put("completed_at", System.currentTimeMillis())
                }
            }

            val rowsUpdated = db.update(
                "execution_requests",
                values,
                "execution_id = ?",
                arrayOf(executionId)
            )

            // Update in-memory state
            activeExecutions[executionId]?.let { state ->
                state.status = status
                state.message = message
                state.executedSteps = executedSteps
                state.executionTimeMs = executionTimeMs
                state.failedAtStep = failedAtStep
            }

            // Notify observers
            val uri = Uri.parse("content://$AUTHORITY/execution_result/$executionId")
            context?.contentResolver?.notifyChange(uri, null)

            Log.d(TAG, "Execution status updated: $executionId -> $status (rows: $rowsUpdated)")

            // Remove from active executions if completed
            if (status == "success" || status == "error") {
                activeExecutions.remove(executionId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating execution status: ${e.message}", e)
        }
    }

    // ========================================
    // Database Helper
    // ========================================

    /**
     * SQLite database helper
     */
    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            // Create execution_requests table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS execution_requests (
                    execution_id TEXT PRIMARY KEY NOT NULL,
                    command_id TEXT NOT NULL,
                    parameters TEXT,
                    requested_by TEXT NOT NULL,
                    requested_at INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'pending',
                    message TEXT,
                    executed_steps INTEGER DEFAULT 0,
                    execution_time_ms INTEGER,
                    failed_at_step INTEGER,
                    started_at INTEGER,
                    completed_at INTEGER,
                    created_at INTEGER DEFAULT (strftime('%s','now') * 1000),
                    CHECK (status IN ('pending', 'executing', 'success', 'error'))
                )
            """)

            // Create indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_execution_id ON execution_requests(execution_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_status ON execution_requests(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_requested_by ON execution_requests(requested_by)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_created_at ON execution_requests(created_at)")

            // Create command_queue table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS command_queue (
                    queue_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    execution_id TEXT NOT NULL,
                    priority INTEGER DEFAULT 0,
                    queued_at INTEGER NOT NULL,
                    FOREIGN KEY (execution_id) REFERENCES execution_requests(execution_id)
                )
            """)

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_queue_priority ON command_queue(priority DESC, queued_at ASC)")

            // Create execution_steps table (for detailed step tracking)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS execution_steps (
                    step_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    execution_id TEXT NOT NULL,
                    step_number INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    target_element_id TEXT,
                    target_text TEXT,
                    parameters TEXT,
                    status TEXT DEFAULT 'pending',
                    error_message TEXT,
                    started_at INTEGER,
                    completed_at INTEGER,
                    FOREIGN KEY (execution_id) REFERENCES execution_requests(execution_id),
                    CHECK (status IN ('pending', 'executing', 'success', 'error'))
                )
            """)

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_execution_steps ON execution_steps(execution_id, step_number)")

            Log.d(TAG, "VoiceOS execution database created (version $DATABASE_VERSION)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(TAG, "Upgrading database from version $oldVersion to $newVersion")

            // Future migrations will go here
            // For now, drop and recreate (development only)
            db.execSQL("DROP TABLE IF EXISTS execution_steps")
            db.execSQL("DROP TABLE IF EXISTS command_queue")
            db.execSQL("DROP TABLE IF EXISTS execution_requests")
            onCreate(db)
        }
    }

    /**
     * Cleanup old execution requests (call periodically)
     *
     * Removes completed executions older than 7 days to prevent database bloat.
     */
    fun cleanupOldExecutions() {
        coroutineScope.launch {
            try {
                val db = dbHelper.writableDatabase
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

                val deleted = db.delete(
                    "execution_requests",
                    "created_at < ? AND status IN ('success', 'error')",
                    arrayOf(sevenDaysAgo.toString())
                )

                Log.d(TAG, "Cleanup: Removed $deleted old execution requests")

            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup: ${e.message}", e)
            }
        }
    }
}
