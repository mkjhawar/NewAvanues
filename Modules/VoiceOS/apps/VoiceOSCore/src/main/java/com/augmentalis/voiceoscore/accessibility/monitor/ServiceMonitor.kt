/**
 * ServiceMonitor.kt - Service lifetime management with health monitoring
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-10
 */
package com.augmentalis.voiceoscore.accessibility.monitor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.commandmanager.monitor.ServiceCallback
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Monitors CommandManager service lifetime and health
 *
 * Features:
 * - Health monitoring every 30s
 * - Reconnection callback pattern
 * - Graceful degradation on failure
 * - Connection state UI (notification)
 * - Lifecycle logging
 * - Configuration persistence (SharedPreferences)
 */
class ServiceMonitor(
    private val service: VoiceOSService,
    private val context: Context
) {
    companion object {
        private const val TAG = "ServiceMonitor"
        private const val HEALTH_CHECK_INTERVAL_MS = 30_000L // 30 seconds
        private const val PREFS_NAME = "service_monitor"
        private const val KEY_LAST_STATE = "last_connection_state"
        private const val KEY_RESTART_COUNT = "restart_count"
        private const val MAX_RESTART_ATTEMPTS = 3
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var commandManager: CommandManager? = null
    private var currentState: ConnectionState = ConnectionState.DISCONNECTED
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var restartAttempts = 0
    private var healthCheckActive = false

    init {
        // Load persisted state
        loadPersistedState()
    }

    /**
     * Q1 Enhancement 5: Configuration Persistence
     * Load persisted state from SharedPreferences
     */
    private fun loadPersistedState() {
        val savedState = prefs.getString(KEY_LAST_STATE, ConnectionState.DISCONNECTED.name)
        restartAttempts = prefs.getInt(KEY_RESTART_COUNT, 0)

        savedState?.let {
            try {
                currentState = ConnectionState.valueOf(it)
                Log.d(TAG, "Loaded persisted state: $currentState, restart attempts: $restartAttempts")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid persisted state: $savedState", e)
                currentState = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Q1 Enhancement 5: Configuration Persistence
     * Save state to SharedPreferences
     */
    private fun persistState() {
        prefs.edit()
            .putString(KEY_LAST_STATE, currentState.name)
            .putInt(KEY_RESTART_COUNT, restartAttempts)
            .apply()
    }

    /**
     * Bind CommandManager and set up callbacks
     */
    fun bindCommandManager(manager: CommandManager) {
        commandManager = manager

        // Set up service callback
        manager.setServiceCallback(object : ServiceCallback {
            override fun onServiceBound() {
                logLifecycleEvent("CommandManager bound to VoiceOSService")
                updateConnectionState(ConnectionState.CONNECTED)
                restartAttempts = 0 // Reset on successful connection
                persistState()
            }

            override fun onServiceDisconnected() {
                logLifecycleEvent("CommandManager disconnected from VoiceOSService", level = Log.WARN)
                updateConnectionState(ConnectionState.DISCONNECTED)
                persistState()

                // Attempt reconnection
                scheduleReconnection()
            }
        })

        // Initial connection established
        updateConnectionState(ConnectionState.CONNECTED)
        Log.i(TAG, "CommandManager successfully bound")
    }

    /**
     * Q1 Enhancement 1: Service Health Monitoring
     * Start periodic health checks (every 30s)
     */
    fun startHealthCheck() {
        if (healthCheckActive) {
            Log.w(TAG, "Health check already active")
            return
        }

        healthCheckActive = true
        scope.launch {
            while (isActive && healthCheckActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                performHealthCheck()
            }
        }
        Log.i(TAG, "Health monitoring started (interval: ${HEALTH_CHECK_INTERVAL_MS}ms)")
    }

    /**
     * Stop health monitoring
     */
    fun stopHealthCheck() {
        healthCheckActive = false
        Log.i(TAG, "Health monitoring stopped")
    }

    /**
     * Perform health check on CommandManager
     */
    private suspend fun performHealthCheck() {
        val manager = commandManager
        if (manager == null) {
            logLifecycleEvent("Health check failed: CommandManager is null", level = Log.WARN)
            updateConnectionState(ConnectionState.DISCONNECTED)
            return
        }

        try {
            val isHealthy = manager.healthCheck()

            if (!isHealthy) {
                logLifecycleEvent("Health check failed: CommandManager unhealthy", level = Log.WARN)
                attemptRecovery()
            } else if (currentState != ConnectionState.CONNECTED) {
                // Recovered from unhealthy state
                logLifecycleEvent("Health check passed: CommandManager recovered")
                updateConnectionState(ConnectionState.CONNECTED)
                restartAttempts = 0
                persistState()
            }
        } catch (e: Exception) {
            logLifecycleEvent("Health check exception: ${e.message}", level = Log.ERROR)
            attemptRecovery()
        }
    }

    /**
     * Q1 Enhancement 2: Graceful Degradation
     * Attempt to recover CommandManager
     */
    private suspend fun attemptRecovery() {
        if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
            logLifecycleEvent(
                "Max restart attempts ($MAX_RESTART_ATTEMPTS) reached, entering degraded mode",
                level = Log.ERROR
            )
            updateConnectionState(ConnectionState.DEGRADED)
            service.enableFallbackMode()
            persistState()
            return
        }

        restartAttempts++
        updateConnectionState(ConnectionState.RECOVERING)
        logLifecycleEvent("Recovery attempt $restartAttempts of $MAX_RESTART_ATTEMPTS")

        try {
            commandManager?.restart()
            updateConnectionState(ConnectionState.CONNECTED)
            logLifecycleEvent("Recovery successful")
            restartAttempts = 0
            persistState()
        } catch (e: Exception) {
            logLifecycleEvent("Recovery failed: ${e.message}", level = Log.ERROR)

            if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
                updateConnectionState(ConnectionState.DEGRADED)
                service.enableFallbackMode()
            } else {
                updateConnectionState(ConnectionState.DISCONNECTED)
            }
            persistState()
        }
    }

    /**
     * Schedule reconnection attempt
     */
    private fun scheduleReconnection() {
        scope.launch {
            delay(5000) // Wait 5s before reconnection
            attemptRecovery()
        }
    }

    /**
     * Q1 Enhancement 3: Connection State UI
     * Update connection state and notify UI
     */
    private fun updateConnectionState(newState: ConnectionState) {
        if (currentState == newState) return

        val oldState = currentState
        currentState = newState

        logLifecycleEvent("Connection state changed: $oldState → $newState")

        // Update notification indicator
        updateNotificationIndicator(newState)
    }

    /**
     * Q1 Enhancement 3: Connection State UI
     * Update notification with connection state indicator
     *
     * Colors:
     * - Green: CONNECTED
     * - Yellow: RECOVERING
     * - Red: DEGRADED
     * - Gray: DISCONNECTED
     */
    private fun updateNotificationIndicator(state: ConnectionState) {
        // TODO: Implement notification update
        // This would integrate with VoiceOSService's foreground notification
        Log.d(TAG, "Notification indicator updated: $state")
    }

    /**
     * Q1 Enhancement 4: Lifecycle Logging
     * Log lifecycle events with timestamp
     */
    private fun logLifecycleEvent(message: String, level: Int = Log.INFO) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logMessage = "[$timestamp] $message"

        when (level) {
            Log.DEBUG -> Log.d(TAG, logMessage)
            Log.INFO -> Log.i(TAG, logMessage)
            Log.WARN -> Log.w(TAG, logMessage)
            Log.ERROR -> Log.e(TAG, logMessage)
            else -> Log.v(TAG, logMessage)
        }

        // TODO: Write to exportable log file for support tickets
        // This would append to a rolling log file that can be exported
    }

    /**
     * Get current connection state
     */
    fun getCurrentState(): ConnectionState = currentState

    /**
     * Cleanup and cancel all monitoring
     */
    fun cleanup() {
        stopHealthCheck()
        scope.cancel()
        persistState()
        Log.i(TAG, "ServiceMonitor cleaned up")
    }
}
