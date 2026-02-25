/**
 * PermissionEscalationDetector.kt - Runtime permission escalation detection
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Monitors runtime permission requests and detects when plugins attempt
 * to access capabilities beyond their declared manifest permissions.
 */
package com.augmentalis.magiccode.plugins.security

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock

/**
 * Represents an attempted permission escalation event.
 *
 * Captured when a plugin attempts to use a capability it has not been granted
 * or that was not declared in its manifest.
 *
 * @param pluginId The plugin that attempted the escalation
 * @param requestedPermission The permission that was requested
 * @param operation The operation that triggered the escalation attempt
 * @param timestamp When the escalation attempt occurred
 * @param escalationType The type of escalation detected
 * @param context Additional context about the escalation attempt
 * @since 2.0.0
 */
data class EscalationEvent(
    val pluginId: String,
    val requestedPermission: PluginPermission,
    val operation: String,
    val timestamp: Long,
    val escalationType: EscalationType,
    val context: Map<String, String> = emptyMap()
) {
    /**
     * Human-readable description of the escalation event.
     */
    val description: String
        get() = "Plugin '$pluginId' attempted ${escalationType.name.lowercase().replace("_", " ")} " +
                "for permission '$requestedPermission' via operation: $operation"
}

/**
 * Types of permission escalation attempts.
 *
 * @since 2.0.0
 */
enum class EscalationType {
    /**
     * Permission was not declared in manifest.
     * Plugin is trying to use a capability it never declared it needed.
     */
    UNDECLARED_PERMISSION,

    /**
     * Permission was declared but not granted by user.
     * Plugin is trying to use a capability the user denied.
     */
    DENIED_PERMISSION,

    /**
     * Permission was previously granted but has been revoked.
     * Plugin is trying to use a capability that was taken away.
     */
    REVOKED_PERMISSION,

    /**
     * Permission request exceeded declared scope.
     * Plugin declared a limited permission but is trying broader access.
     */
    SCOPE_EXCEEDED,

    /**
     * Runtime permission request that was not pre-declared.
     * Plugin is dynamically requesting permissions at runtime.
     */
    DYNAMIC_REQUEST,

    /**
     * Repeated attempts after denial.
     * Plugin is persistently trying to access denied capability.
     */
    REPEATED_DENIAL
}

/**
 * Callback interface for escalation event notifications.
 *
 * Implement this interface to receive notifications when escalation
 * attempts are detected.
 *
 * @since 2.0.0
 */
interface EscalationCallback {
    /**
     * Called when an escalation attempt is detected.
     *
     * @param event The escalation event details
     */
    fun onEscalationDetected(event: EscalationEvent)

    /**
     * Called when an escalation attempt reaches a threshold.
     *
     * @param pluginId The plugin that reached the threshold
     * @param attemptCount Number of escalation attempts
     * @param period Time period in milliseconds over which attempts occurred
     */
    fun onEscalationThresholdReached(pluginId: String, attemptCount: Int, period: Long)
}

/**
 * Configuration for the escalation detector.
 *
 * @param trackingWindowMs Time window for tracking repeated attempts (default: 5 minutes)
 * @param maxAttemptsBeforeAlert Threshold for triggering threshold alerts (default: 5)
 * @param maxAttemptsBeforeBlock Threshold for automatic blocking (default: 10)
 * @param enableAutomaticBlocking Whether to automatically block plugins after threshold (default: false)
 * @param retainHistoryMs How long to retain escalation history (default: 1 hour)
 * @since 2.0.0
 */
data class EscalationDetectorConfig(
    val trackingWindowMs: Long = 5 * 60 * 1000L,  // 5 minutes
    val maxAttemptsBeforeAlert: Int = 5,
    val maxAttemptsBeforeBlock: Int = 10,
    val enableAutomaticBlocking: Boolean = false,
    val retainHistoryMs: Long = 60 * 60 * 1000L  // 1 hour
)

/**
 * Statistics for a plugin's escalation history.
 *
 * @param pluginId The plugin identifier
 * @param totalAttempts Total number of escalation attempts
 * @param recentAttempts Attempts within the tracking window
 * @param lastAttemptTime Timestamp of most recent attempt
 * @param permissionBreakdown Count of attempts per permission
 * @param isBlocked Whether the plugin is currently blocked
 * @since 2.0.0
 */
data class EscalationStats(
    val pluginId: String,
    val totalAttempts: Int,
    val recentAttempts: Int,
    val lastAttemptTime: Long?,
    val permissionBreakdown: Map<PluginPermission, Int>,
    val isBlocked: Boolean
)

/**
 * Monitors runtime permission requests and detects escalation attempts.
 *
 * The detector tracks when plugins attempt to access capabilities they are
 * not authorized for, providing security monitoring and alerting.
 *
 * ## Detection Capabilities
 * - Undeclared permission usage
 * - Denied permission bypass attempts
 * - Revoked permission continued usage
 * - Scope violation detection
 * - Rate limiting and threshold alerts
 *
 * ## Integration
 * The detector integrates with [PluginSandbox] to receive permission check
 * failures and with [SecurityAuditLogger] to log security events.
 *
 * ## Usage Example
 * ```kotlin
 * val detector = PermissionEscalationDetector(
 *     config = EscalationDetectorConfig(
 *         maxAttemptsBeforeAlert = 5,
 *         enableAutomaticBlocking = true
 *     ),
 *     auditLogger = securityLogger
 * )
 *
 * // Register callback
 * detector.addCallback(object : EscalationCallback {
 *     override fun onEscalationDetected(event: EscalationEvent) {
 *         showSecurityAlert(event.description)
 *     }
 *
 *     override fun onEscalationThresholdReached(pluginId: String, count: Int, period: Long) {
 *         disablePlugin(pluginId)
 *     }
 * })
 *
 * // Record an escalation attempt
 * detector.recordEscalation(
 *     pluginId = "com.suspicious.plugin",
 *     permission = PluginPermission.NETWORK_ACCESS,
 *     operation = "httpRequest",
 *     type = EscalationType.DENIED_PERMISSION
 * )
 *
 * // Check stats
 * val stats = detector.getStats("com.suspicious.plugin")
 * println("Total attempts: ${stats.totalAttempts}")
 * ```
 *
 * @param config Configuration for detection thresholds and behavior
 * @param auditLogger Optional logger for security events
 * @param manifestPermissions Optional map of plugin IDs to their declared permissions
 * @since 2.0.0
 */
class PermissionEscalationDetector(
    private val config: EscalationDetectorConfig = EscalationDetectorConfig(),
    private val auditLogger: SecurityAuditLogger? = null,
    private val manifestPermissions: MutableMap<String, Set<PluginPermission>> = mutableMapOf()
) {

    companion object {
        private const val TAG = "EscalationDetector"
    }

    // Escalation history per plugin
    private val escalationHistory = mutableMapOf<String, MutableList<EscalationEvent>>()

    // Blocked plugins
    private val blockedPlugins = mutableSetOf<String>()

    // Callbacks
    private val callbacks = mutableListOf<EscalationCallback>()

    // Flow for escalation events
    private val _escalationFlow = MutableSharedFlow<EscalationEvent>(extraBufferCapacity = 100)

    /**
     * Flow of escalation events for reactive subscribers.
     */
    val escalationFlow: SharedFlow<EscalationEvent> = _escalationFlow.asSharedFlow()

    private val lock = Any()

    /**
     * Register declared manifest permissions for a plugin.
     *
     * This allows the detector to identify undeclared permission usage.
     *
     * @param pluginId The plugin identifier
     * @param permissions Set of permissions declared in the plugin's manifest
     */
    fun registerManifestPermissions(pluginId: String, permissions: Set<PluginPermission>) {
        synchronized(lock) {
            manifestPermissions[pluginId] = permissions
            PluginLog.d(TAG, "Registered ${permissions.size} manifest permissions for $pluginId")
        }
    }

    /**
     * Get the declared manifest permissions for a plugin.
     *
     * @param pluginId The plugin identifier
     * @return Set of declared permissions, or null if not registered
     */
    fun getManifestPermissions(pluginId: String): Set<PluginPermission>? {
        synchronized(lock) {
            return manifestPermissions[pluginId]
        }
    }

    /**
     * Record a permission escalation attempt.
     *
     * @param pluginId The plugin that attempted the escalation
     * @param permission The permission that was requested
     * @param operation Description of the operation that triggered the attempt
     * @param type The type of escalation
     * @param context Additional context information
     */
    fun recordEscalation(
        pluginId: String,
        permission: PluginPermission,
        operation: String,
        type: EscalationType,
        context: Map<String, String> = emptyMap()
    ) {
        val event = EscalationEvent(
            pluginId = pluginId,
            requestedPermission = permission,
            operation = operation,
            timestamp = currentTimeMillis(),
            escalationType = type,
            context = context
        )

        synchronized(lock) {
            // Add to history
            val history = escalationHistory.getOrPut(pluginId) { mutableListOf() }
            history.add(event)

            // Clean old history
            cleanHistory(pluginId)

            // Log the event
            PluginLog.w(TAG, event.description)
            auditLogger?.logEscalationAttempt(event)

            // Check thresholds
            val recentAttempts = getRecentAttemptCount(pluginId)

            // Notify callbacks
            callbacks.forEach { callback ->
                try {
                    callback.onEscalationDetected(event)
                } catch (e: Exception) {
                    PluginLog.e(TAG, "Callback error", e)
                }
            }

            // Check if threshold reached
            if (recentAttempts == config.maxAttemptsBeforeAlert) {
                notifyThresholdReached(pluginId, recentAttempts)
            }

            // Auto-block if enabled
            if (config.enableAutomaticBlocking && recentAttempts >= config.maxAttemptsBeforeBlock) {
                blockPlugin(pluginId)
            }
        }

        // Emit to flow (non-blocking)
        _escalationFlow.tryEmit(event)
    }

    /**
     * Record an undeclared permission access attempt.
     *
     * Convenience method for recording when a plugin tries to use a permission
     * it did not declare in its manifest.
     *
     * @param pluginId The plugin identifier
     * @param permission The undeclared permission
     * @param operation The operation that triggered the attempt
     */
    fun recordUndeclaredAccess(
        pluginId: String,
        permission: PluginPermission,
        operation: String
    ) {
        recordEscalation(
            pluginId = pluginId,
            permission = permission,
            operation = operation,
            type = EscalationType.UNDECLARED_PERMISSION,
            context = mapOf("declared" to (manifestPermissions[pluginId]?.joinToString(",") ?: "none"))
        )
    }

    /**
     * Record a denied permission access attempt.
     *
     * Convenience method for recording when a plugin tries to use a permission
     * that was denied by the user.
     *
     * @param pluginId The plugin identifier
     * @param permission The denied permission
     * @param operation The operation that triggered the attempt
     */
    fun recordDeniedAccess(
        pluginId: String,
        permission: PluginPermission,
        operation: String
    ) {
        recordEscalation(
            pluginId = pluginId,
            permission = permission,
            operation = operation,
            type = EscalationType.DENIED_PERMISSION
        )
    }

    /**
     * Check if a plugin is currently blocked due to excessive escalation attempts.
     *
     * @param pluginId The plugin identifier
     * @return true if blocked
     */
    fun isBlocked(pluginId: String): Boolean {
        synchronized(lock) {
            return blockedPlugins.contains(pluginId)
        }
    }

    /**
     * Manually block a plugin from further operations.
     *
     * @param pluginId The plugin to block
     */
    fun blockPlugin(pluginId: String) {
        synchronized(lock) {
            if (blockedPlugins.add(pluginId)) {
                PluginLog.security(TAG, "Plugin blocked due to escalation attempts: $pluginId")
                auditLogger?.logPluginBlocked(pluginId, "excessive_escalation_attempts")
            }
        }
    }

    /**
     * Unblock a previously blocked plugin.
     *
     * @param pluginId The plugin to unblock
     */
    fun unblockPlugin(pluginId: String) {
        synchronized(lock) {
            if (blockedPlugins.remove(pluginId)) {
                PluginLog.security(TAG, "Plugin unblocked: $pluginId")
                auditLogger?.logPluginUnblocked(pluginId)
            }
        }
    }

    /**
     * Get escalation statistics for a plugin.
     *
     * @param pluginId The plugin identifier
     * @return Statistics about the plugin's escalation history
     */
    fun getStats(pluginId: String): EscalationStats {
        synchronized(lock) {
            val history = escalationHistory[pluginId] ?: emptyList()
            val now = currentTimeMillis()
            val recentEvents = history.filter { now - it.timestamp < config.trackingWindowMs }

            val permissionBreakdown = history
                .groupingBy { it.requestedPermission }
                .eachCount()

            return EscalationStats(
                pluginId = pluginId,
                totalAttempts = history.size,
                recentAttempts = recentEvents.size,
                lastAttemptTime = history.maxOfOrNull { it.timestamp },
                permissionBreakdown = permissionBreakdown,
                isBlocked = blockedPlugins.contains(pluginId)
            )
        }
    }

    /**
     * Get all escalation events for a plugin.
     *
     * @param pluginId The plugin identifier
     * @param limit Maximum number of events to return (most recent first)
     * @return List of escalation events
     */
    fun getHistory(pluginId: String, limit: Int = 100): List<EscalationEvent> {
        synchronized(lock) {
            val history = escalationHistory[pluginId] ?: return emptyList()
            return history.sortedByDescending { it.timestamp }.take(limit)
        }
    }

    /**
     * Get all blocked plugins.
     *
     * @return Set of blocked plugin IDs
     */
    fun getBlockedPlugins(): Set<String> {
        synchronized(lock) {
            return blockedPlugins.toSet()
        }
    }

    /**
     * Add an escalation callback.
     *
     * @param callback The callback to add
     */
    fun addCallback(callback: EscalationCallback) {
        synchronized(lock) {
            callbacks.add(callback)
        }
    }

    /**
     * Remove an escalation callback.
     *
     * @param callback The callback to remove
     */
    fun removeCallback(callback: EscalationCallback) {
        synchronized(lock) {
            callbacks.remove(callback)
        }
    }

    /**
     * Clear all escalation history for a plugin.
     *
     * @param pluginId The plugin identifier
     */
    fun clearHistory(pluginId: String) {
        synchronized(lock) {
            escalationHistory.remove(pluginId)
            PluginLog.d(TAG, "Cleared escalation history for: $pluginId")
        }
    }

    /**
     * Clear all escalation history.
     */
    fun clearAllHistory() {
        synchronized(lock) {
            escalationHistory.clear()
            PluginLog.d(TAG, "Cleared all escalation history")
        }
    }

    /**
     * Check if a permission access would be an escalation.
     *
     * Does not record the event, just checks if it would be flagged.
     *
     * @param pluginId The plugin identifier
     * @param permission The permission to check
     * @param grantedPermissions Currently granted permissions
     * @return The type of escalation if detected, null if allowed
     */
    fun checkEscalation(
        pluginId: String,
        permission: PluginPermission,
        grantedPermissions: Set<PluginPermission>
    ): EscalationType? {
        synchronized(lock) {
            // Check if declared in manifest
            val declared = manifestPermissions[pluginId]
            if (declared != null && !declared.contains(permission)) {
                return EscalationType.UNDECLARED_PERMISSION
            }

            // Check if granted
            if (!grantedPermissions.contains(permission)) {
                return EscalationType.DENIED_PERMISSION
            }

            return null
        }
    }

    // ==========================================================================
    // Private Methods
    // ==========================================================================

    private fun getRecentAttemptCount(pluginId: String): Int {
        val now = currentTimeMillis()
        val history = escalationHistory[pluginId] ?: return 0
        return history.count { now - it.timestamp < config.trackingWindowMs }
    }

    private fun cleanHistory(pluginId: String) {
        val now = currentTimeMillis()
        val history = escalationHistory[pluginId] ?: return
        history.removeAll { now - it.timestamp > config.retainHistoryMs }
    }

    private fun notifyThresholdReached(pluginId: String, attemptCount: Int) {
        PluginLog.security(TAG, "Escalation threshold reached for $pluginId: $attemptCount attempts")
        callbacks.forEach { callback ->
            try {
                callback.onEscalationThresholdReached(pluginId, attemptCount, config.trackingWindowMs)
            } catch (e: Exception) {
                PluginLog.e(TAG, "Callback error on threshold", e)
            }
        }
    }

    /**
     * Get current time in milliseconds.
     * Extracted for testability.
     */
    internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
