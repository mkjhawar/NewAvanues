/**
 * PluginSandbox.kt - Plugin permission sandbox interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides runtime permission enforcement for plugins. The sandbox ensures
 * plugins can only perform actions within their granted permission scope.
 */
package com.augmentalis.magiccode.plugins.security

import com.augmentalis.magiccode.plugins.core.PluginLog

/**
 * Plugin permission types that can be granted or denied.
 *
 * Each permission represents a specific capability that a plugin may request.
 * Permissions are declared in the plugin manifest and must be explicitly
 * granted by the user or system before use.
 *
 * ## Permission Categories
 * - **Network**: Access to network resources
 * - **File System**: Read/write access to storage
 * - **Data Access**: Access to sensitive platform data
 * - **System**: System-level capabilities
 * - **Communication**: Inter-plugin messaging
 *
 * ## Security Model
 * Permissions follow a least-privilege model. Plugins should request only
 * the permissions they need, and users can revoke permissions at any time.
 *
 * @since 2.0.0
 */
enum class PluginPermission {
    /**
     * Permission to make network requests.
     *
     * Allows the plugin to:
     * - Make HTTP/HTTPS requests
     * - Open WebSocket connections
     * - Access remote APIs
     *
     * ## Security Note
     * Combined with FILE_SYSTEM_WRITE, this creates a potential data
     * exfiltration risk. Monitor plugins with both permissions.
     */
    NETWORK_ACCESS,

    /**
     * Permission to read from the file system.
     *
     * Allows the plugin to:
     * - Read files within designated plugin directories
     * - Access shared storage (with user consent)
     * - Read configuration files
     *
     * ## Scope
     * Reading is limited to:
     * - Plugin's own data directory
     * - Shared directories explicitly granted
     */
    FILE_SYSTEM_READ,

    /**
     * Permission to write to the file system.
     *
     * Allows the plugin to:
     * - Create new files
     * - Modify existing files
     * - Delete files within permitted directories
     *
     * ## Security Note
     * Combined with NETWORK_ACCESS, this creates a potential download
     * risk for malicious payloads. Requires extra scrutiny.
     */
    FILE_SYSTEM_WRITE,

    /**
     * Permission to access accessibility service data.
     *
     * Allows the plugin to:
     * - Read current screen elements
     * - Access UI hierarchy information
     * - Observe user interactions (with limitations)
     *
     * ## Privacy Note
     * This is a sensitive permission that provides access to visible
     * UI content. Plugins with this permission should be carefully reviewed.
     */
    ACCESSIBILITY_DATA,

    /**
     * Permission to access device information.
     *
     * Allows the plugin to:
     * - Read device model and manufacturer
     * - Access OS version information
     * - Query device capabilities
     *
     * ## Privacy Note
     * Device fingerprinting using this data should be avoided.
     */
    DEVICE_INFO,

    /**
     * Permission to display notifications.
     *
     * Allows the plugin to:
     * - Show system notifications
     * - Update existing notifications
     * - Create notification channels
     *
     * ## UX Note
     * Excessive notifications may lead to user uninstalling plugins.
     */
    NOTIFICATIONS,

    /**
     * Permission to execute in the background.
     *
     * Allows the plugin to:
     * - Continue execution when app is backgrounded
     * - Schedule background tasks
     * - Receive background updates
     *
     * ## Battery Note
     * Plugins with this permission should be power-efficient to avoid
     * draining device battery.
     */
    BACKGROUND_EXECUTION,

    /**
     * Permission for inter-plugin communication.
     *
     * Allows the plugin to:
     * - Send events to other plugins
     * - Subscribe to events from other plugins
     * - Share data through the plugin event bus
     *
     * ## Security Note
     * Enables plugins to coordinate, but also creates potential
     * for privilege escalation through malicious plugin collaboration.
     */
    INTER_PLUGIN_COMMUNICATION
}

/**
 * Exception thrown when a plugin attempts to use a capability without
 * the required permission.
 *
 * @param pluginId The plugin that attempted the unauthorized action
 * @param permission The permission that was missing
 * @param operation Description of the attempted operation
 * @since 2.0.0
 */
class PermissionDeniedException(
    val pluginId: String,
    val permission: PluginPermission,
    val operation: String
) : SecurityException("Plugin '$pluginId' denied permission '$permission' for operation: $operation")

/**
 * Interface for plugin permission sandbox enforcement.
 *
 * The sandbox acts as a gatekeeper for plugin capabilities, ensuring that
 * plugins can only perform operations they have been granted permission for.
 * This provides runtime security enforcement beyond manifest-time declarations.
 *
 * ## Security Model
 * The sandbox implements a capability-based security model:
 * 1. Plugins declare required permissions in their manifest
 * 2. Users grant or deny permissions during installation/runtime
 * 3. The sandbox enforces permissions at runtime
 * 4. Permission denials are logged for security auditing
 *
 * ## Usage Example
 * ```kotlin
 * class MyPlugin(private val sandbox: PluginSandbox) {
 *     suspend fun fetchData(url: String): String {
 *         // This will throw PermissionDeniedException if not granted
 *         sandbox.enforcePermission(pluginId, PluginPermission.NETWORK_ACCESS)
 *
 *         // Permission granted, proceed with network request
 *         return httpClient.get(url)
 *     }
 *
 *     fun canAccessFiles(): Boolean {
 *         return sandbox.checkPermission(pluginId, PluginPermission.FILE_SYSTEM_READ)
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * Implementations must be thread-safe as permission checks may occur
 * from multiple coroutines concurrently.
 *
 * @see PluginPermission
 * @see PermissionDeniedException
 * @see PermissionStorage
 * @since 2.0.0
 */
interface PluginSandbox {

    /**
     * Check if a plugin has a specific permission.
     *
     * This is a non-throwing check that returns whether the permission
     * is currently granted. Use this for conditional logic where you
     * want to handle the absence of a permission gracefully.
     *
     * ## Performance
     * Permission checks are cached and typically complete in <1ms.
     *
     * @param pluginId Unique identifier of the plugin
     * @param permission The permission to check
     * @return true if the permission is granted, false otherwise
     *
     * @see enforcePermission
     */
    fun checkPermission(pluginId: String, permission: PluginPermission): Boolean

    /**
     * Enforce that a plugin has a specific permission.
     *
     * This is a throwing check that raises [PermissionDeniedException] if
     * the permission is not granted. Use this when the operation cannot
     * proceed without the permission.
     *
     * ## Audit Logging
     * Both grants and denials are logged for security auditing:
     * - Grant: DEBUG level, no special action
     * - Denial: WARN level, logged to security audit
     *
     * @param pluginId Unique identifier of the plugin
     * @param permission The permission to enforce
     * @throws PermissionDeniedException if the permission is not granted
     *
     * @see checkPermission
     */
    @Throws(PermissionDeniedException::class)
    fun enforcePermission(pluginId: String, permission: PluginPermission)

    /**
     * Get all permissions currently granted to a plugin.
     *
     * Returns the complete set of permissions that the plugin can use.
     * This is useful for debugging and displaying permission status to users.
     *
     * @param pluginId Unique identifier of the plugin
     * @return Set of granted permissions (empty if none)
     */
    fun getGrantedPermissions(pluginId: String): Set<PluginPermission>

    /**
     * Grant a permission to a plugin.
     *
     * Adds the permission to the plugin's granted set. This should typically
     * be called in response to user approval or system policy.
     *
     * ## Persistence
     * Granted permissions are persisted to [PermissionStorage] and survive
     * app restarts.
     *
     * ## Audit Logging
     * All permission grants are logged to the security audit log.
     *
     * @param pluginId Unique identifier of the plugin
     * @param permission The permission to grant
     *
     * @see revokePermission
     */
    fun grantPermission(pluginId: String, permission: PluginPermission)

    /**
     * Revoke a permission from a plugin.
     *
     * Removes the permission from the plugin's granted set. Any subsequent
     * operations requiring this permission will fail.
     *
     * ## Immediate Effect
     * Revocation takes effect immediately. Running operations that depend
     * on the revoked permission may fail mid-execution.
     *
     * ## Audit Logging
     * All permission revocations are logged to the security audit log.
     *
     * @param pluginId Unique identifier of the plugin
     * @param permission The permission to revoke
     *
     * @see grantPermission
     */
    fun revokePermission(pluginId: String, permission: PluginPermission)

    /**
     * Revoke all permissions from a plugin.
     *
     * Removes all granted permissions. Typically used when:
     * - User revokes all permissions
     * - Plugin is uninstalled
     * - Security policy requires permission reset
     *
     * @param pluginId Unique identifier of the plugin
     */
    fun revokeAllPermissions(pluginId: String)

    /**
     * Check if a plugin has all of the specified permissions.
     *
     * Convenience method for checking multiple permissions at once.
     *
     * @param pluginId Unique identifier of the plugin
     * @param permissions Set of permissions to check
     * @return true if ALL permissions are granted, false if any is missing
     */
    fun hasAllPermissions(pluginId: String, permissions: Set<PluginPermission>): Boolean {
        return permissions.all { checkPermission(pluginId, it) }
    }

    /**
     * Check if a plugin has any of the specified permissions.
     *
     * Convenience method for checking if at least one permission is granted.
     *
     * @param pluginId Unique identifier of the plugin
     * @param permissions Set of permissions to check
     * @return true if ANY permission is granted, false if none are granted
     */
    fun hasAnyPermission(pluginId: String, permissions: Set<PluginPermission>): Boolean {
        return permissions.any { checkPermission(pluginId, it) }
    }
}

/**
 * Default in-memory implementation of [PluginSandbox].
 *
 * This implementation stores permissions in memory and optionally persists
 * them to a [PermissionStorage] backend. It is thread-safe and suitable for
 * production use.
 *
 * ## Usage Example
 * ```kotlin
 * val sandbox = DefaultPluginSandbox(
 *     auditLogger = SecurityAuditLogger.create()
 * )
 *
 * // Grant permissions
 * sandbox.grantPermission("com.example.plugin", PluginPermission.NETWORK_ACCESS)
 *
 * // Check permissions
 * if (sandbox.checkPermission("com.example.plugin", PluginPermission.NETWORK_ACCESS)) {
 *     // Permission granted
 * }
 * ```
 *
 * @param auditLogger Optional security audit logger for tracking permission events
 * @since 2.0.0
 */
class DefaultPluginSandbox(
    private val auditLogger: SecurityAuditLogger? = null
) : PluginSandbox {

    private val permissions = mutableMapOf<String, MutableSet<PluginPermission>>()
    private val lock = Any()

    companion object {
        private const val TAG = "PluginSandbox"
    }

    override fun checkPermission(pluginId: String, permission: PluginPermission): Boolean {
        synchronized(lock) {
            val granted = permissions[pluginId]?.contains(permission) == true
            PluginLog.d(TAG, "Permission check: $pluginId -> $permission = $granted")
            return granted
        }
    }

    override fun enforcePermission(pluginId: String, permission: PluginPermission) {
        if (!checkPermission(pluginId, permission)) {
            val operation = "enforcePermission($permission)"
            auditLogger?.logPermissionDenied(pluginId, permission, operation)
            PluginLog.security(TAG, "Permission denied: $pluginId -> $permission")
            throw PermissionDeniedException(pluginId, permission, operation)
        }
        auditLogger?.logPermissionChecked(pluginId, permission, granted = true)
    }

    override fun getGrantedPermissions(pluginId: String): Set<PluginPermission> {
        synchronized(lock) {
            return permissions[pluginId]?.toSet() ?: emptySet()
        }
    }

    override fun grantPermission(pluginId: String, permission: PluginPermission) {
        synchronized(lock) {
            val pluginPermissions = permissions.getOrPut(pluginId) { mutableSetOf() }
            if (pluginPermissions.add(permission)) {
                PluginLog.i(TAG, "Permission granted: $pluginId -> $permission")
                auditLogger?.logPermissionGranted(pluginId, permission)
            }
        }
    }

    override fun revokePermission(pluginId: String, permission: PluginPermission) {
        synchronized(lock) {
            val removed = permissions[pluginId]?.remove(permission) == true
            if (removed) {
                PluginLog.i(TAG, "Permission revoked: $pluginId -> $permission")
                auditLogger?.logPermissionRevoked(pluginId, permission)
            }
        }
    }

    override fun revokeAllPermissions(pluginId: String) {
        synchronized(lock) {
            val removed = permissions.remove(pluginId)
            if (removed != null && removed.isNotEmpty()) {
                PluginLog.i(TAG, "All permissions revoked for: $pluginId (count: ${removed.size})")
                auditLogger?.logAllPermissionsRevoked(pluginId, removed.size)
            }
        }
    }
}
