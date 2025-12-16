package com.avanues.voiceos.queue

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.avanues.voiceos.accessibility.AccessibilityCommandExecutor
import com.avanues.voiceos.provider.VoiceOSProvider
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Command Queue Processor for VoiceOS
 *
 * Processes queued execution requests in FIFO/priority order and delegates
 * to AccessibilityService for actual execution.
 *
 * Architecture:
 * 1. Polls command_queue table for pending commands
 * 2. Fetches command hierarchy from VoiceOS database
 * 3. Delegates to AccessibilityCommandExecutor
 * 4. Updates execution status via VoiceOSProvider
 *
 * Reference: Developer Manual Chapter 36, ADR-006
 */
class CommandQueueProcessor(
    private val context: Context,
    private val voiceOSProvider: VoiceOSProvider
) {

    companion object {
        private const val TAG = "CommandQueueProcessor"
        private const val POLL_INTERVAL_MS = 500L // Poll every 500ms
        private const val DATABASE_NAME = "voiceos_execution.db"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isProcessing = false
    private var processingJob: Job? = null

    private lateinit var dbHelper: SQLiteOpenHelper
    private lateinit var accessibilityExecutor: AccessibilityCommandExecutor

    /**
     * Command data pulled from queue
     */
    private data class QueuedCommand(
        val queueId: Long,
        val executionId: String,
        val commandId: String,
        val parameters: Map<String, String>,
        val requestedBy: String,
        val priority: Int
    )

    /**
     * Command hierarchy for execution
     */
    private data class CommandHierarchy(
        val commandId: String,
        val steps: List<CommandStep>
    )

    /**
     * Individual command step
     */
    private data class CommandStep(
        val stepNumber: Int,
        val action: String, // OPEN_APP, CLICK, INPUT_TEXT, SELECT, WAIT
        val targetElementId: String? = null,
        val targetText: String? = null,
        val targetContentDescription: String? = null,
        val parameters: Map<String, String> = emptyMap()
    )

    fun initialize(dbHelper: SQLiteOpenHelper, accessibilityExecutor: AccessibilityCommandExecutor) {
        this.dbHelper = dbHelper
        this.accessibilityExecutor = accessibilityExecutor
        Log.d(TAG, "CommandQueueProcessor initialized")
    }

    /**
     * Start processing command queue
     *
     * Runs in background coroutine, polling for new commands.
     */
    fun start() {
        if (isProcessing) {
            Log.w(TAG, "Queue processor already running")
            return
        }

        isProcessing = true

        processingJob = coroutineScope.launch {
            Log.d(TAG, "Command queue processing started")

            while (isProcessing) {
                try {
                    processNextCommand()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing command queue: ${e.message}", e)
                }

                // Poll interval
                delay(POLL_INTERVAL_MS)
            }

            Log.d(TAG, "Command queue processing stopped")
        }
    }

    /**
     * Stop processing command queue
     */
    fun stop() {
        isProcessing = false
        processingJob?.cancel()
        processingJob = null
        Log.d(TAG, "Command queue processing stopped")
    }

    /**
     * Process next command from queue
     *
     * Steps:
     * 1. Pull highest priority/oldest command from queue
     * 2. Fetch command hierarchy from VoiceOS database
     * 3. Execute command via AccessibilityService
     * 4. Update execution status
     * 5. Remove from queue
     */
    private suspend fun processNextCommand() {
        withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase

            // Query next command (priority DESC, queued_at ASC)
            val cursor = db.rawQuery(
                """
                SELECT
                    q.queue_id,
                    q.execution_id,
                    q.priority,
                    e.command_id,
                    e.parameters,
                    e.requested_by
                FROM command_queue q
                JOIN execution_requests e ON q.execution_id = e.execution_id
                WHERE e.status = 'pending'
                ORDER BY q.priority DESC, q.queued_at ASC
                LIMIT 1
                """,
                null
            )

            if (!cursor.moveToFirst()) {
                cursor.close()
                return@withContext // No commands in queue
            }

            val queuedCommand = QueuedCommand(
                queueId = cursor.getLong(0),
                executionId = cursor.getString(1),
                priority = cursor.getInt(2),
                commandId = cursor.getString(3),
                parameters = parseParameters(cursor.getString(4)),
                requestedBy = cursor.getString(5)
            )

            cursor.close()

            Log.d(TAG, "Processing command: ${queuedCommand.executionId} (${queuedCommand.commandId})")

            // Update status to executing
            voiceOSProvider.updateExecutionStatus(
                executionId = queuedCommand.executionId,
                status = "executing",
                message = "Executing command: ${queuedCommand.commandId}"
            )

            try {
                // Fetch command hierarchy from VoiceOS database
                val commandHierarchy = fetchCommandHierarchy(
                    queuedCommand.commandId,
                    queuedCommand.parameters
                )

                if (commandHierarchy == null) {
                    voiceOSProvider.updateExecutionStatus(
                        executionId = queuedCommand.executionId,
                        status = "error",
                        message = "Command not found: ${queuedCommand.commandId}",
                        failedAtStep = null
                    )
                    removeFromQueue(queuedCommand.queueId)
                    return@withContext
                }

                // Execute command hierarchy via AccessibilityService
                val startTime = System.currentTimeMillis()
                val result = executeCommandHierarchy(commandHierarchy, queuedCommand.executionId)
                val executionTime = System.currentTimeMillis() - startTime

                // Update execution status based on result
                if (result.success) {
                    voiceOSProvider.updateExecutionStatus(
                        executionId = queuedCommand.executionId,
                        status = "success",
                        message = result.message ?: "Command executed successfully",
                        executedSteps = result.executedSteps,
                        executionTimeMs = executionTime
                    )
                } else {
                    voiceOSProvider.updateExecutionStatus(
                        executionId = queuedCommand.executionId,
                        status = "error",
                        message = result.message ?: "Command execution failed",
                        executedSteps = result.executedSteps,
                        executionTimeMs = executionTime,
                        failedAtStep = result.failedAtStep
                    )
                }

                // Remove from queue
                removeFromQueue(queuedCommand.queueId)

            } catch (e: Exception) {
                Log.e(TAG, "Error executing command: ${e.message}", e)

                voiceOSProvider.updateExecutionStatus(
                    executionId = queuedCommand.executionId,
                    status = "error",
                    message = "Execution error: ${e.message}",
                    failedAtStep = null
                )

                removeFromQueue(queuedCommand.queueId)
            }
        }
    }

    /**
     * Fetch command hierarchy from VoiceOS database
     *
     * @param commandId Command ID (e.g., "cmd_call_teams")
     * @param parameters Runtime parameters (e.g., {"contact": "John Doe"})
     * @return CommandHierarchy with steps, or null if not found
     */
    private fun fetchCommandHierarchy(
        commandId: String,
        parameters: Map<String, String>
    ): CommandHierarchy? {
        // TODO: Query VoiceOS commands database
        // For reference implementation, return mock hierarchy

        // Example: Call on Teams
        if (commandId.startsWith("cmd_call")) {
            return CommandHierarchy(
                commandId = commandId,
                steps = listOf(
                    CommandStep(
                        stepNumber = 1,
                        action = "OPEN_APP",
                        parameters = mapOf("package" to "com.microsoft.teams")
                    ),
                    CommandStep(
                        stepNumber = 2,
                        action = "CLICK",
                        targetElementId = "call_button",
                        targetContentDescription = "Call"
                    ),
                    CommandStep(
                        stepNumber = 3,
                        action = "SELECT",
                        targetText = parameters["contact"] ?: "Unknown",
                        targetContentDescription = "Contact"
                    )
                )
            )
        }

        // Example: Play music on Spotify
        if (commandId.startsWith("cmd_spotify")) {
            return CommandHierarchy(
                commandId = commandId,
                steps = listOf(
                    CommandStep(
                        stepNumber = 1,
                        action = "OPEN_APP",
                        parameters = mapOf("package" to "com.spotify.music")
                    ),
                    CommandStep(
                        stepNumber = 2,
                        action = "CLICK",
                        targetElementId = "search_button"
                    ),
                    CommandStep(
                        stepNumber = 3,
                        action = "INPUT_TEXT",
                        targetElementId = "search_input",
                        parameters = mapOf("text" to (parameters["song"] ?: ""))
                    ),
                    CommandStep(
                        stepNumber = 4,
                        action = "CLICK",
                        targetElementId = "first_result"
                    ),
                    CommandStep(
                        stepNumber = 5,
                        action = "CLICK",
                        targetElementId = "play_button"
                    )
                )
            )
        }

        Log.w(TAG, "Command hierarchy not found for: $commandId")
        return null
    }

    /**
     * Execute command hierarchy via AccessibilityService
     *
     * @param hierarchy Command hierarchy with steps
     * @param executionId Execution ID for status tracking
     * @return Execution result
     */
    private suspend fun executeCommandHierarchy(
        hierarchy: CommandHierarchy,
        executionId: String
    ): ExecutionResult {
        return withContext(Dispatchers.Main) {
            try {
                var executedSteps = 0

                for (step in hierarchy.steps) {
                    Log.d(TAG, "Executing step ${step.stepNumber}: ${step.action}")

                    // Update execution_steps table
                    insertExecutionStep(executionId, step, "executing")

                    val stepResult = when (step.action) {
                        "OPEN_APP" -> {
                            val packageName = step.parameters["package"]
                            if (packageName != null) {
                                accessibilityExecutor.openApp(packageName)
                            } else {
                                ExecutionResult(
                                    success = false,
                                    message = "Missing package parameter",
                                    executedSteps = executedSteps,
                                    failedAtStep = step.stepNumber
                                )
                            }
                        }

                        "CLICK" -> {
                            accessibilityExecutor.clickElement(
                                elementId = step.targetElementId,
                                text = step.targetText,
                                contentDescription = step.targetContentDescription
                            )
                        }

                        "INPUT_TEXT" -> {
                            val text = step.parameters["text"]
                            if (text != null) {
                                accessibilityExecutor.inputText(
                                    elementId = step.targetElementId,
                                    text = text
                                )
                            } else {
                                ExecutionResult(
                                    success = false,
                                    message = "Missing text parameter",
                                    executedSteps = executedSteps,
                                    failedAtStep = step.stepNumber
                                )
                            }
                        }

                        "SELECT" -> {
                            accessibilityExecutor.selectElement(
                                text = step.targetText,
                                contentDescription = step.targetContentDescription
                            )
                        }

                        "WAIT" -> {
                            val delayMs = step.parameters["ms"]?.toLongOrNull() ?: 1000L
                            delay(delayMs)
                            ExecutionResult(success = true, executedSteps = executedSteps + 1)
                        }

                        else -> {
                            ExecutionResult(
                                success = false,
                                message = "Unknown action: ${step.action}",
                                executedSteps = executedSteps,
                                failedAtStep = step.stepNumber
                            )
                        }
                    }

                    if (!stepResult.success) {
                        // Update execution_steps table
                        insertExecutionStep(executionId, step, "error", stepResult.message)
                        return@withContext stepResult
                    }

                    executedSteps++

                    // Update execution_steps table
                    insertExecutionStep(executionId, step, "success")
                }

                ExecutionResult(
                    success = true,
                    message = "Command executed successfully",
                    executedSteps = executedSteps
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error executing command hierarchy: ${e.message}", e)
                ExecutionResult(
                    success = false,
                    message = "Execution error: ${e.message}",
                    executedSteps = 0,
                    failedAtStep = null
                )
            }
        }
    }

    /**
     * Insert execution step into database
     */
    private fun insertExecutionStep(
        executionId: String,
        step: CommandStep,
        status: String,
        errorMessage: String? = null
    ) {
        try {
            val db = dbHelper.writableDatabase

            db.execSQL(
                """
                INSERT INTO execution_steps (
                    execution_id, step_number, action, target_element_id,
                    target_text, parameters, status, error_message,
                    started_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    executionId,
                    step.stepNumber,
                    step.action,
                    step.targetElementId,
                    step.targetText,
                    JSONObject(step.parameters).toString(),
                    status,
                    errorMessage,
                    System.currentTimeMillis(),
                    if (status != "executing") System.currentTimeMillis() else null
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error inserting execution step: ${e.message}", e)
        }
    }

    /**
     * Remove command from queue
     */
    private fun removeFromQueue(queueId: Long) {
        try {
            val db = dbHelper.writableDatabase
            db.delete("command_queue", "queue_id = ?", arrayOf(queueId.toString()))
            Log.d(TAG, "Removed command from queue: $queueId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from queue: ${e.message}", e)
        }
    }

    /**
     * Parse JSON parameters
     */
    private fun parseParameters(json: String?): Map<String, String> {
        if (json == null) return emptyMap()

        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing parameters: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Execution result
     */
    data class ExecutionResult(
        val success: Boolean,
        val message: String? = null,
        val executedSteps: Int = 0,
        val failedAtStep: Int? = null
    )
}
