/**
 * SecurityAuditLogger.kt - Comprehensive security event audit logging
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides centralized logging for security-related events in the plugin system.
 * Supports structured logging, event filtering, and integration with external
 * logging systems.
 */
package com.augmentalis.magiccode.plugins.security

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock

/**
 * Categories of security events for filtering and analysis.
 *
 * @since 2.0.0
 */
enum class SecurityEventCategory {
    /** Signature and certificate verification events */
    SIGNATURE_VERIFICATION,

    /** Permission grant, denial, and check events */
    PERMISSION,

    /** Permission escalation and violation events */
    ESCALATION,

    /** Trust store modifications */
    TRUST_STORE,

    /** Plugin lifecycle security events */
    PLUGIN_LIFECYCLE,

    /** Encryption key and credential events */
    ENCRYPTION,

    /** Configuration and policy changes */
    CONFIGURATION,

    /** Authentication and authorization events */
    AUTHENTICATION,

    /** Sandbox and isolation events */
    SANDBOX
}

/**
 * Severity levels for security events.
 *
 * @since 2.0.0
 */
enum class SecurityEventSeverity {
    /** Informational event - no action required */
    INFO,

    /** Warning - potential security concern */
    WARNING,

    /** Error - security operation failed */
    ERROR,

    /** Critical - immediate attention required */
    CRITICAL,

    /** Alert - active security threat detected */
    ALERT
}

/**
 * Represents a security audit event.
 *
 * Contains all relevant information about a security-related action
 * for logging, monitoring, and compliance purposes.
 *
 * @param eventId Unique identifier for this event
 * @param category Category of security event
 * @param severity Severity level
 * @param message Human-readable description
 * @param timestamp When the event occurred
 * @param pluginId Plugin involved (if any)
 * @param details Additional structured details
 * @param sourceComponent Component that generated the event
 * @since 2.0.0
 */
data class SecurityAuditEvent(
    val eventId: String,
    val category: SecurityEventCategory,
    val severity: SecurityEventSeverity,
    val message: String,
    val timestamp: Long,
    val pluginId: String? = null,
    val details: Map<String, String> = emptyMap(),
    val sourceComponent: String = "PluginSystem"
) {
    /**
     * Format the event as a log line.
     */
    fun toLogLine(): String {
        val pluginPart = pluginId?.let { " [plugin:$it]" } ?: ""
        val detailsPart = if (details.isNotEmpty()) {
            " " + details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else ""
        return "[${severity.name}] [${category.name}]$pluginPart $message$detailsPart"
    }

    /**
     * Convert to JSON representation.
     */
    fun toJson(): String {
        val detailsJson = details.entries.joinToString(",") { (k, v) ->
            "\"$k\":\"${v.replace("\"", "\\\"")}\""
        }
        return buildString {
            append("{")
            append("\"eventId\":\"$eventId\",")
            append("\"category\":\"${category.name}\",")
            append("\"severity\":\"${severity.name}\",")
            append("\"message\":\"${message.replace("\"", "\\\"")}\",")
            append("\"timestamp\":$timestamp,")
            pluginId?.let { append("\"pluginId\":\"$it\",") }
            append("\"sourceComponent\":\"$sourceComponent\",")
            append("\"details\":{$detailsJson}")
            append("}")
        }
    }
}

/**
 * Callback interface for external audit log handlers.
 *
 * Implement this to integrate with external logging systems, SIEMs,
 * or compliance monitoring tools.
 *
 * @since 2.0.0
 */
interface SecurityAuditHandler {
    /**
     * Handle a security audit event.
     *
     * @param event The security event to handle
     */
    fun handleEvent(event: SecurityAuditEvent)
}

/**
 * Configuration for the security audit logger.
 *
 * @param minSeverity Minimum severity level to log (default: INFO)
 * @param enabledCategories Categories to log (empty = all)
 * @param retainEventsCount Maximum number of events to retain in memory
 * @param logToPluginLog Whether to also log to PluginLog.security
 * @since 2.0.0
 */
data class SecurityAuditConfig(
    val minSeverity: SecurityEventSeverity = SecurityEventSeverity.INFO,
    val enabledCategories: Set<SecurityEventCategory> = emptySet(),
    val retainEventsCount: Int = 1000,
    val logToPluginLog: Boolean = true
)

/**
 * Centralized security audit logger for the plugin system.
 *
 * Provides comprehensive logging for all security-related events including:
 * - Signature verification (pass/fail)
 * - Permission checks, grants, and revocations
 * - Escalation attempts and violations
 * - Trust store modifications
 * - Plugin lifecycle security events
 *
 * ## Features
 * - Structured event logging with categories and severity levels
 * - In-memory event buffer for recent event queries
 * - Flow-based event streaming for reactive consumers
 * - Integration with external audit handlers (SIEM, compliance tools)
 * - Configurable filtering and retention
 *
 * ## Usage Example
 * ```kotlin
 * // Create logger
 * val logger = SecurityAuditLogger.create(
 *     config = SecurityAuditConfig(
 *         minSeverity = SecurityEventSeverity.INFO,
 *         retainEventsCount = 500
 *     )
 * )
 *
 * // Log events
 * logger.logSignatureVerification("com.example.plugin", success = true)
 * logger.logPermissionGranted("com.example.plugin", PluginPermission.NETWORK_ACCESS)
 *
 * // Query recent events
 * val recentEvents = logger.getRecentEvents(100)
 *
 * // Stream events
 * logger.eventFlow.collect { event ->
 *     sendToSiem(event)
 * }
 *
 * // Add external handler
 * logger.addHandler(object : SecurityAuditHandler {
 *     override fun handleEvent(event: SecurityAuditEvent) {
 *         complianceSystem.record(event)
 *     }
 * })
 * ```
 *
 * ## Thread Safety
 * This class is thread-safe. Events can be logged from multiple coroutines
 * concurrently.
 *
 * @since 2.0.0
 */
class SecurityAuditLogger private constructor(
    private val config: SecurityAuditConfig
) {

    companion object {
        private const val TAG = "SecurityAudit"
        private var eventCounter = 0L

        /**
         * Create a new SecurityAuditLogger instance.
         *
         * @param config Configuration for the logger
         * @return New logger instance
         */
        fun create(config: SecurityAuditConfig = SecurityAuditConfig()): SecurityAuditLogger {
            return SecurityAuditLogger(config)
        }

        /**
         * Generate a unique event ID.
         */
        private fun generateEventId(): String {
            return "SEC_${Clock.System.now().toEpochMilliseconds()}_${eventCounter++}"
        }
    }

    // Event buffer
    private val eventBuffer = mutableListOf<SecurityAuditEvent>()

    // External handlers
    private val handlers = mutableListOf<SecurityAuditHandler>()

    // Event flow
    private val _eventFlow = MutableSharedFlow<SecurityAuditEvent>(extraBufferCapacity = 100)

    /**
     * Flow of security audit events for reactive subscribers.
     */
    val eventFlow: SharedFlow<SecurityAuditEvent> = _eventFlow.asSharedFlow()

    private val lock = Any()

    // ==========================================================================
    // Signature Verification Events
    // ==========================================================================

    /**
     * Log a signature verification event.
     *
     * @param pluginId The plugin being verified
     * @param success Whether verification succeeded
     * @param algorithm Algorithm used (if known)
     * @param reason Failure reason (if failed)
     */
    fun logSignatureVerification(
        pluginId: String,
        success: Boolean,
        algorithm: String? = null,
        reason: String? = null
    ) {
        val severity = if (success) SecurityEventSeverity.INFO else SecurityEventSeverity.WARNING
        val message = if (success) {
            "Signature verification succeeded"
        } else {
            "Signature verification failed: ${reason ?: "unknown reason"}"
        }

        val details = mutableMapOf<String, String>()
        details["success"] = success.toString()
        algorithm?.let { details["algorithm"] = it }
        reason?.let { details["reason"] = it }

        log(
            category = SecurityEventCategory.SIGNATURE_VERIFICATION,
            severity = severity,
            message = message,
            pluginId = pluginId,
            details = details
        )
    }

    // ==========================================================================
    // Permission Events
    // ==========================================================================

    /**
     * Log a permission granted event.
     *
     * @param pluginId The plugin receiving the permission
     * @param permission The permission granted
     */
    fun logPermissionGranted(pluginId: String, permission: PluginPermission) {
        log(
            category = SecurityEventCategory.PERMISSION,
            severity = SecurityEventSeverity.INFO,
            message = "Permission granted: ${permission.name}",
            pluginId = pluginId,
            details = mapOf("permission" to permission.name, "action" to "GRANT")
        )
    }

    /**
     * Log a permission revoked event.
     *
     * @param pluginId The plugin losing the permission
     * @param permission The permission revoked
     */
    fun logPermissionRevoked(pluginId: String, permission: PluginPermission) {
        log(
            category = SecurityEventCategory.PERMISSION,
            severity = SecurityEventSeverity.INFO,
            message = "Permission revoked: ${permission.name}",
            pluginId = pluginId,
            details = mapOf("permission" to permission.name, "action" to "REVOKE")
        )
    }

    /**
     * Log all permissions revoked for a plugin.
     *
     * @param pluginId The plugin losing permissions
     * @param count Number of permissions revoked
     */
    fun logAllPermissionsRevoked(pluginId: String, count: Int) {
        log(
            category = SecurityEventCategory.PERMISSION,
            severity = SecurityEventSeverity.INFO,
            message = "All permissions revoked ($count permissions)",
            pluginId = pluginId,
            details = mapOf("action" to "REVOKE_ALL", "count" to count.toString())
        )
    }

    /**
     * Log a permission check event.
     *
     * @param pluginId The plugin being checked
     * @param permission The permission checked
     * @param granted Whether the permission was granted
     */
    fun logPermissionChecked(pluginId: String, permission: PluginPermission, granted: Boolean) {
        // Only log at DEBUG level for granted checks
        if (granted && config.minSeverity != SecurityEventSeverity.INFO) return

        log(
            category = SecurityEventCategory.PERMISSION,
            severity = if (granted) SecurityEventSeverity.INFO else SecurityEventSeverity.WARNING,
            message = "Permission check: ${permission.name} = ${if (granted) "GRANTED" else "DENIED"}",
            pluginId = pluginId,
            details = mapOf("permission" to permission.name, "result" to if (granted) "GRANTED" else "DENIED")
        )
    }

    /**
     * Log a permission denied event.
     *
     * @param pluginId The plugin that was denied
     * @param permission The permission denied
     * @param operation The operation that was attempted
     */
    fun logPermissionDenied(pluginId: String, permission: PluginPermission, operation: String) {
        log(
            category = SecurityEventCategory.PERMISSION,
            severity = SecurityEventSeverity.WARNING,
            message = "Permission denied: ${permission.name} for operation: $operation",
            pluginId = pluginId,
            details = mapOf(
                "permission" to permission.name,
                "operation" to operation,
                "action" to "DENY"
            )
        )
    }

    // ==========================================================================
    // Escalation Events
    // ==========================================================================

    /**
     * Log an escalation attempt.
     *
     * @param event The escalation event details
     */
    fun logEscalationAttempt(event: EscalationEvent) {
        log(
            category = SecurityEventCategory.ESCALATION,
            severity = SecurityEventSeverity.WARNING,
            message = event.description,
            pluginId = event.pluginId,
            details = mapOf(
                "permission" to event.requestedPermission.name,
                "operation" to event.operation,
                "escalationType" to event.escalationType.name
            ) + event.context
        )
    }

    /**
     * Log a plugin being blocked.
     *
     * @param pluginId The plugin that was blocked
     * @param reason The reason for blocking
     */
    fun logPluginBlocked(pluginId: String, reason: String) {
        log(
            category = SecurityEventCategory.ESCALATION,
            severity = SecurityEventSeverity.ALERT,
            message = "Plugin blocked: $reason",
            pluginId = pluginId,
            details = mapOf("reason" to reason, "action" to "BLOCK")
        )
    }

    /**
     * Log a plugin being unblocked.
     *
     * @param pluginId The plugin that was unblocked
     */
    fun logPluginUnblocked(pluginId: String) {
        log(
            category = SecurityEventCategory.ESCALATION,
            severity = SecurityEventSeverity.INFO,
            message = "Plugin unblocked",
            pluginId = pluginId,
            details = mapOf("action" to "UNBLOCK")
        )
    }

    // ==========================================================================
    // Trust Store Events
    // ==========================================================================

    /**
     * Log a trusted key added event.
     *
     * @param publisherId The publisher whose key was added
     * @param keyPath Path to the public key file
     */
    fun logTrustedKeyAdded(publisherId: String, keyPath: String) {
        log(
            category = SecurityEventCategory.TRUST_STORE,
            severity = SecurityEventSeverity.INFO,
            message = "Trusted key added for publisher: $publisherId",
            details = mapOf(
                "publisherId" to publisherId,
                "keyPath" to keyPath,
                "action" to "ADD"
            )
        )
    }

    /**
     * Log a trusted key removed event.
     *
     * @param publisherId The publisher whose key was removed
     */
    fun logTrustedKeyRemoved(publisherId: String) {
        log(
            category = SecurityEventCategory.TRUST_STORE,
            severity = SecurityEventSeverity.INFO,
            message = "Trusted key removed for publisher: $publisherId",
            details = mapOf("publisherId" to publisherId, "action" to "REMOVE")
        )
    }

    /**
     * Log trust store cleared event.
     *
     * @param count Number of keys that were cleared
     */
    fun logTrustStoreCleared(count: Int) {
        log(
            category = SecurityEventCategory.TRUST_STORE,
            severity = SecurityEventSeverity.WARNING,
            message = "Trust store cleared ($count keys removed)",
            details = mapOf("count" to count.toString(), "action" to "CLEAR")
        )
    }

    // ==========================================================================
    // Plugin Lifecycle Events
    // ==========================================================================

    /**
     * Log a plugin installation event.
     *
     * @param pluginId The plugin that was installed
     * @param version The version installed
     * @param source The installation source
     */
    fun logPluginInstalled(pluginId: String, version: String, source: String) {
        log(
            category = SecurityEventCategory.PLUGIN_LIFECYCLE,
            severity = SecurityEventSeverity.INFO,
            message = "Plugin installed: $version from $source",
            pluginId = pluginId,
            details = mapOf("version" to version, "source" to source, "action" to "INSTALL")
        )
    }

    /**
     * Log a plugin uninstallation event.
     *
     * @param pluginId The plugin that was uninstalled
     */
    fun logPluginUninstalled(pluginId: String) {
        log(
            category = SecurityEventCategory.PLUGIN_LIFECYCLE,
            severity = SecurityEventSeverity.INFO,
            message = "Plugin uninstalled",
            pluginId = pluginId,
            details = mapOf("action" to "UNINSTALL")
        )
    }

    /**
     * Log a plugin update event.
     *
     * @param pluginId The plugin that was updated
     * @param fromVersion Previous version
     * @param toVersion New version
     */
    fun logPluginUpdated(pluginId: String, fromVersion: String, toVersion: String) {
        log(
            category = SecurityEventCategory.PLUGIN_LIFECYCLE,
            severity = SecurityEventSeverity.INFO,
            message = "Plugin updated: $fromVersion -> $toVersion",
            pluginId = pluginId,
            details = mapOf(
                "fromVersion" to fromVersion,
                "toVersion" to toVersion,
                "action" to "UPDATE"
            )
        )
    }

    // ==========================================================================
    // General Logging
    // ==========================================================================

    /**
     * Log a security event.
     *
     * @param category Event category
     * @param severity Severity level
     * @param message Human-readable message
     * @param pluginId Plugin involved (if any)
     * @param details Additional details
     */
    fun log(
        category: SecurityEventCategory,
        severity: SecurityEventSeverity,
        message: String,
        pluginId: String? = null,
        details: Map<String, String> = emptyMap()
    ) {
        // Check severity filter
        if (severity.ordinal < config.minSeverity.ordinal) return

        // Check category filter
        if (config.enabledCategories.isNotEmpty() && !config.enabledCategories.contains(category)) return

        val event = SecurityAuditEvent(
            eventId = generateEventId(),
            category = category,
            severity = severity,
            message = message,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            pluginId = pluginId,
            details = details
        )

        synchronized(lock) {
            // Add to buffer
            eventBuffer.add(event)

            // Trim buffer if needed
            while (eventBuffer.size > config.retainEventsCount) {
                eventBuffer.removeAt(0)
            }

            // Notify handlers
            handlers.forEach { handler ->
                try {
                    handler.handleEvent(event)
                } catch (e: Exception) {
                    PluginLog.e(TAG, "Handler error", e)
                }
            }
        }

        // Log to PluginLog if enabled
        if (config.logToPluginLog) {
            PluginLog.security(TAG, event.toLogLine())
        }

        // Emit to flow (non-blocking)
        _eventFlow.tryEmit(event)
    }

    // ==========================================================================
    // Query Methods
    // ==========================================================================

    /**
     * Get recent security events.
     *
     * @param limit Maximum number of events to return
     * @return List of recent events (most recent first)
     */
    fun getRecentEvents(limit: Int = 100): List<SecurityAuditEvent> {
        synchronized(lock) {
            return eventBuffer.takeLast(limit).reversed()
        }
    }

    /**
     * Get events filtered by category.
     *
     * @param category The category to filter by
     * @param limit Maximum number of events
     * @return Filtered events (most recent first)
     */
    fun getEventsByCategory(
        category: SecurityEventCategory,
        limit: Int = 100
    ): List<SecurityAuditEvent> {
        synchronized(lock) {
            return eventBuffer
                .filter { it.category == category }
                .takeLast(limit)
                .reversed()
        }
    }

    /**
     * Get events filtered by plugin.
     *
     * @param pluginId The plugin to filter by
     * @param limit Maximum number of events
     * @return Filtered events (most recent first)
     */
    fun getEventsByPlugin(pluginId: String, limit: Int = 100): List<SecurityAuditEvent> {
        synchronized(lock) {
            return eventBuffer
                .filter { it.pluginId == pluginId }
                .takeLast(limit)
                .reversed()
        }
    }

    /**
     * Get events filtered by severity (and above).
     *
     * @param minSeverity Minimum severity level
     * @param limit Maximum number of events
     * @return Filtered events (most recent first)
     */
    fun getEventsBySeverity(
        minSeverity: SecurityEventSeverity,
        limit: Int = 100
    ): List<SecurityAuditEvent> {
        synchronized(lock) {
            return eventBuffer
                .filter { it.severity.ordinal >= minSeverity.ordinal }
                .takeLast(limit)
                .reversed()
        }
    }

    /**
     * Get event count by category.
     *
     * @return Map of category to event count
     */
    fun getEventCountByCategory(): Map<SecurityEventCategory, Int> {
        synchronized(lock) {
            return eventBuffer.groupingBy { it.category }.eachCount()
        }
    }

    /**
     * Get event count by severity.
     *
     * @return Map of severity to event count
     */
    fun getEventCountBySeverity(): Map<SecurityEventSeverity, Int> {
        synchronized(lock) {
            return eventBuffer.groupingBy { it.severity }.eachCount()
        }
    }

    // ==========================================================================
    // Handler Management
    // ==========================================================================

    /**
     * Add an external audit handler.
     *
     * @param handler The handler to add
     */
    fun addHandler(handler: SecurityAuditHandler) {
        synchronized(lock) {
            handlers.add(handler)
        }
    }

    /**
     * Remove an external audit handler.
     *
     * @param handler The handler to remove
     */
    fun removeHandler(handler: SecurityAuditHandler) {
        synchronized(lock) {
            handlers.remove(handler)
        }
    }

    /**
     * Clear all events from the buffer.
     */
    fun clearEvents() {
        synchronized(lock) {
            eventBuffer.clear()
        }
    }

    /**
     * Export all events as JSON array.
     *
     * @return JSON string containing all events
     */
    fun exportToJson(): String {
        synchronized(lock) {
            val eventsJson = eventBuffer.joinToString(",\n") { it.toJson() }
            return "[\n$eventsJson\n]"
        }
    }
}
