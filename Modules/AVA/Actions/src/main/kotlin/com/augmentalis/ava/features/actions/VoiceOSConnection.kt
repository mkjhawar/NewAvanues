package com.augmentalis.ava.features.actions

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * VoiceOS Connection Manager
 *
 * Manages IPC connection to VoiceOS accessibility service for forwarding
 * commands that require accessibility permissions.
 *
 * Architecture:
 * ```
 * AVA → VoiceOSConnection → ContentProvider → VoiceOS Service
 *                                               ↓
 *                                     Accessibility API
 *                                  (Gestures, Cursor, etc.)
 * ```
 *
 * IPC Flow:
 * 1. Check if VoiceOS is installed
 * 2. Check if accessibility service is running
 * 3. INSERT command via ContentProvider
 * 4. Poll for execution result
 * 5. Return result to caller
 *
 * Created: 2025-11-17 (Phase 3)
 * Author: AVA AI Team
 */
class VoiceOSConnection private constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "VoiceOSConnection"

        @Volatile
        private var instance: VoiceOSConnection? = null

        /**
         * Get singleton instance of VoiceOSConnection.
         * This prevents memory leaks from creating new instances on each command.
         *
         * @param context Application context (should be applicationContext)
         * @return Singleton VoiceOSConnection instance
         */
        fun getInstance(context: Context): VoiceOSConnection {
            return instance ?: synchronized(this) {
                instance ?: VoiceOSConnection(context.applicationContext).also {
                    instance = it
                    Log.d(TAG, "VoiceOSConnection singleton created")
                }
            }
        }

        // VoiceOS package names
        private const val VOICEOS_PACKAGE = "com.avanues.voiceos"
        private const val VOICEOS_LAUNCHER_PACKAGE = "com.avanues.launcher"
        private const val VOICEOS_FRAMEWORK_PACKAGE = "com.ideahq.voiceos"

        // ContentProvider authority and URIs
        private const val AUTHORITY = "com.avanues.voiceos.provider"
        private val EXECUTE_COMMAND_URI = Uri.parse("content://$AUTHORITY/execute_command")
        private fun executionResultUri(executionId: String) =
            Uri.parse("content://$AUTHORITY/execution_result/$executionId")

        // Polling configuration
        private const val POLL_INTERVAL_MS = 100L
        private const val EXECUTION_TIMEOUT_MS = 30_000L

        // Accessibility service class names
        private val VOICEOS_ACCESSIBILITY_SERVICES = listOf(
            "$VOICEOS_PACKAGE/.service.VoiceOSAccessibilityService",
            "$VOICEOS_PACKAGE/com.avanues.voiceos.service.VoiceOSAccessibilityService",
            "$VOICEOS_LAUNCHER_PACKAGE/.service.LauncherAccessibilityService",
            "$VOICEOS_FRAMEWORK_PACKAGE/.VoiceOSAccessibilityService"
        )
    }

    /**
     * Connection state
     */
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    /**
     * Command execution result
     */
    sealed class CommandResult {
        data class Success(val message: String, val executedSteps: Int = 0, val executionTimeMs: Long = 0) : CommandResult()
        data class Failure(val error: String, val failedAtStep: Int? = null) : CommandResult()
    }

    private var connectionState: ConnectionState = ConnectionState.Disconnected

    /**
     * Check if VoiceOS is installed
     *
     * @return True if any VoiceOS package is installed
     */
    fun isVoiceOSInstalled(): Boolean {
        val packages = listOf(VOICEOS_PACKAGE, VOICEOS_LAUNCHER_PACKAGE, VOICEOS_FRAMEWORK_PACKAGE)

        for (pkg in packages) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                Log.d(TAG, "VoiceOS found: $pkg")
                return true
            } catch (e: Exception) {
                // Package not found, try next
            }
        }

        Log.d(TAG, "VoiceOS not installed")
        return false
    }

    /**
     * Check if VoiceOS accessibility service is running
     *
     * @return True if accessibility service is active
     */
    fun isAccessibilityServiceRunning(): Boolean {
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            for (serviceName in VOICEOS_ACCESSIBILITY_SERVICES) {
                if (enabledServices.contains(serviceName.substringBefore("/"))) {
                    Log.d(TAG, "VoiceOS accessibility service is running: $serviceName")
                    return true
                }
            }

            // Also check for partial matches (package name only)
            val packages = listOf(VOICEOS_PACKAGE, VOICEOS_LAUNCHER_PACKAGE, VOICEOS_FRAMEWORK_PACKAGE)
            for (pkg in packages) {
                if (enabledServices.contains(pkg)) {
                    Log.d(TAG, "VoiceOS accessibility service found via package: $pkg")
                    return true
                }
            }

            Log.d(TAG, "VoiceOS accessibility service not running. Enabled services: $enabledServices")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status: ${e.message}")
            return false
        }
    }

    /**
     * Execute command via VoiceOS IPC
     *
     * @param intent Intent identifier (e.g., "cursor_move_up", "swipe_left")
     * @param category Command category (e.g., "cursor", "gesture")
     * @param parameters Optional parameters for the command
     * @return Result of command execution
     */
    suspend fun executeCommand(
        intent: String,
        category: String,
        parameters: Map<String, String> = emptyMap()
    ): CommandResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Forwarding to VoiceOS: $intent (category: $category)")

        // Check if VoiceOS is available
        if (!isVoiceOSInstalled()) {
            return@withContext CommandResult.Failure(
                "VoiceOS is not installed. Please install VoiceOS for accessibility commands."
            )
        }

        if (!isAccessibilityServiceRunning()) {
            return@withContext CommandResult.Failure(
                "VoiceOS accessibility service is not running. Please enable it in Settings > Accessibility."
            )
        }

        try {
            // Prepare command parameters
            val paramsJson = JSONObject().apply {
                parameters.forEach { (key, value) -> put(key, value) }
                put("category", category)
            }.toString()

            // Insert execution request
            val contentValues = ContentValues().apply {
                put("command_id", intent)
                put("parameters", paramsJson)
                put("requested_by", context.packageName)
            }

            val resultUri = context.contentResolver.insert(EXECUTE_COMMAND_URI, contentValues)

            if (resultUri == null) {
                Log.e(TAG, "Failed to insert command request")
                return@withContext CommandResult.Failure(
                    "Failed to send command to VoiceOS"
                )
            }

            // Extract execution ID from URI
            val executionId = resultUri.lastPathSegment
                ?: resultUri.toString().substringAfterLast("/")

            Log.d(TAG, "Command queued with execution ID: $executionId")

            // Poll for result
            val result = pollForResult(executionId)
            return@withContext result

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing VoiceOS provider: ${e.message}")
            return@withContext CommandResult.Failure(
                "Permission denied accessing VoiceOS. Check app permissions."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command via VoiceOS: ${e.message}", e)
            return@withContext CommandResult.Failure(
                "Failed to execute command: ${e.message}"
            )
        }
    }

    /**
     * Poll for execution result
     *
     * @param executionId The execution ID to poll for
     * @return Command result
     */
    private suspend fun pollForResult(executionId: String): CommandResult {
        val resultUri = executionResultUri(executionId)

        val result = withTimeoutOrNull(EXECUTION_TIMEOUT_MS) {
            while (true) {
                val cursor = context.contentResolver.query(
                    resultUri,
                    arrayOf("status", "message", "executed_steps", "execution_time_ms", "failed_at_step"),
                    null,
                    null,
                    null
                )

                cursor?.use {
                    if (it.moveToFirst()) {
                        val status = it.getString(it.getColumnIndexOrThrow("status"))
                        val message = it.getString(it.getColumnIndexOrThrow("message"))
                        val executedSteps = it.getInt(it.getColumnIndexOrThrow("executed_steps"))
                        val executionTimeMs = it.getLong(it.getColumnIndexOrThrow("execution_time_ms"))
                        val failedAtStep = if (!it.isNull(it.getColumnIndexOrThrow("failed_at_step"))) {
                            it.getInt(it.getColumnIndexOrThrow("failed_at_step"))
                        } else null

                        when (status) {
                            "success" -> {
                                Log.i(TAG, "Command executed successfully: $message ($executedSteps steps, ${executionTimeMs}ms)")
                                return@withTimeoutOrNull CommandResult.Success(
                                    message = message ?: "Command executed",
                                    executedSteps = executedSteps,
                                    executionTimeMs = executionTimeMs
                                )
                            }
                            "error" -> {
                                Log.e(TAG, "Command execution failed: $message (failed at step $failedAtStep)")
                                return@withTimeoutOrNull CommandResult.Failure(
                                    error = message ?: "Command failed",
                                    failedAtStep = failedAtStep
                                )
                            }
                            "pending", "executing" -> {
                                // Continue polling
                                Log.v(TAG, "Command status: $status, steps: $executedSteps")
                            }
                        }
                    }
                }

                delay(POLL_INTERVAL_MS)
            }
            @Suppress("UNREACHABLE_CODE")
            CommandResult.Failure("Polling loop error")
        }

        return result ?: CommandResult.Failure(
            "Command execution timed out after ${EXECUTION_TIMEOUT_MS / 1000} seconds"
        )
    }

    /**
     * Execute a simple command (convenience method)
     *
     * @param intent Intent identifier
     * @param category Command category
     * @return Result of command execution
     */
    suspend fun execute(intent: String, category: String): CommandResult {
        return executeCommand(intent, category)
    }

    /**
     * Connect to VoiceOS service (validates connection)
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        if (connectionState is ConnectionState.Connected) {
            Log.d(TAG, "Already connected to VoiceOS")
            return@withContext true
        }

        connectionState = ConnectionState.Connecting
        Log.i(TAG, "Connecting to VoiceOS service...")

        if (!isVoiceOSInstalled()) {
            connectionState = ConnectionState.Error("VoiceOS not installed")
            return@withContext false
        }

        if (!isAccessibilityServiceRunning()) {
            connectionState = ConnectionState.Error("Accessibility service not running")
            return@withContext false
        }

        // Verify ContentProvider is accessible
        try {
            val testUri = Uri.parse("content://$AUTHORITY")
            context.contentResolver.getType(testUri)
            connectionState = ConnectionState.Connected
            Log.i(TAG, "Connected to VoiceOS")
            return@withContext true
        } catch (e: Exception) {
            connectionState = ConnectionState.Error("Cannot access VoiceOS provider: ${e.message}")
            Log.e(TAG, "Failed to connect to VoiceOS: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Disconnect from VoiceOS service
     */
    fun disconnect() {
        if (connectionState is ConnectionState.Connected) {
            Log.i(TAG, "Disconnecting from VoiceOS service")
            connectionState = ConnectionState.Disconnected
        }
    }

    /**
     * Get current connection state
     */
    fun getConnectionState(): ConnectionState = connectionState

    /**
     * Check if connected and ready
     */
    fun isReady(): Boolean {
        return isVoiceOSInstalled() && isAccessibilityServiceRunning()
    }
}
