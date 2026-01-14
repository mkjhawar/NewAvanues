package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.*
import kotlinx.serialization.Serializable

/**
 * Security Sandbox
 *
 * Provides isolated execution environment for plugins to prevent
 * malicious or buggy plugins from affecting the system.
 *
 * Key features:
 * - Resource limits (memory, CPU, component count)
 * - API restrictions
 * - Permission enforcement
 * - Isolation between plugins
 */
class SecuritySandbox {
    private val environments = mutableMapOf<String, SandboxedEnvironment>()

    /**
     * Create isolated environment for a plugin
     *
     * @param pluginId Unique plugin identifier
     * @param permissions Requested permissions
     * @param resourceLimits Resource usage limits
     * @return Sandboxed environment instance
     */
    fun createIsolatedEnvironment(
        pluginId: String,
        permissions: Set<Permission>,
        resourceLimits: ResourceLimits
    ): SandboxedEnvironment {
        // Validate permissions
        val deniedPermissions = permissions.filter { it in Permission.BLACKLISTED }
        if (deniedPermissions.isNotEmpty()) {
            throw PluginException.SecurityException(
                "Blacklisted permissions requested: ${deniedPermissions.joinToString()}"
            )
        }

        val environment = SandboxedEnvironment(
            pluginId = pluginId,
            permissions = permissions,
            resourceLimits = resourceLimits,
            allowedAPIs = determineAllowedAPIs(permissions),
            networkPolicy = NetworkPolicy.NONE,
            fileSystemAccess = FileSystemAccess.NONE,
            reflectionPolicy = ReflectionPolicy.RESTRICTED
        )

        environments[pluginId] = environment
        return environment
    }

    /**
     * Get sandboxed environment for a plugin
     */
    fun getEnvironment(pluginId: String): SandboxedEnvironment? {
        return environments[pluginId]
    }

    /**
     * Destroy sandboxed environment
     */
    fun destroy(pluginId: String) {
        environments.remove(pluginId)
    }

    /**
     * Check if plugin has permission
     */
    fun hasPermission(pluginId: String, permission: Permission): Boolean {
        val environment = environments[pluginId] ?: return false
        return permission in environment.permissions
    }

    /**
     * Enforce resource limits for a plugin
     *
     * @throws PluginException.SecurityException if limit exceeded
     */
    fun enforceResourceLimits(pluginId: String, usage: ResourceUsage) {
        val environment = environments[pluginId]
            ?: throw PluginException.SecurityException("Plugin not found: $pluginId")

        val limits = environment.resourceLimits

        if (usage.memoryBytes > limits.memory) {
            throw PluginException.SecurityException("Memory limit exceeded: ${usage.memoryBytes} > ${limits.memory}")
        }

        if (usage.componentCount > limits.componentCount) {
            throw PluginException.SecurityException("Component count limit exceeded: ${usage.componentCount} > ${limits.componentCount}")
        }

        if (usage.nestingDepth > limits.nestingDepth) {
            throw PluginException.SecurityException("Nesting depth limit exceeded: ${usage.nestingDepth} > ${limits.nestingDepth}")
        }
    }

    /**
     * Determine allowed APIs based on permissions
     */
    private fun determineAllowedAPIs(permissions: Set<Permission>): Set<String> {
        val apis = mutableSetOf(
            // Core APIs (always allowed)
            "com.augmentalis.avaelements.core.*"
        )

        // Add APIs based on permissions
        if (Permission.READ_THEME in permissions) {
            apis.add("com.augmentalis.avaelements.core.Theme")
        }

        if (Permission.READ_USER_PREFERENCES in permissions) {
            apis.add("com.augmentalis.avaelements.preferences.*")
        }

        return apis
    }
}

/**
 * Sandboxed execution environment
 */
data class SandboxedEnvironment(
    val pluginId: String,
    val permissions: Set<Permission>,
    val resourceLimits: ResourceLimits,
    val allowedAPIs: Set<String>,
    val networkPolicy: NetworkPolicy,
    val fileSystemAccess: FileSystemAccess,
    val reflectionPolicy: ReflectionPolicy
)

/**
 * Resource limits for plugin execution
 */
@Serializable
data class ResourceLimits(
    val memory: Long,              // Bytes
    val cpuTimeMs: Long,           // Milliseconds
    val fileSize: Long,            // Bytes
    val componentCount: Int,       // Max components per plugin
    val nestingDepth: Int          // Max component nesting depth
) {
    companion object {
        fun default() = ResourceLimits(
            memory = 10_000_000,        // 10 MB
            cpuTimeMs = 100,            // 100 ms
            fileSize = 1_000_000,       // 1 MB
            componentCount = 100,       // 100 components
            nestingDepth = 10           // 10 levels deep
        )

        fun generous() = ResourceLimits(
            memory = 50_000_000,        // 50 MB
            cpuTimeMs = 500,            // 500 ms
            fileSize = 5_000_000,       // 5 MB
            componentCount = 500,       // 500 components
            nestingDepth = 20           // 20 levels deep
        )

        fun strict() = ResourceLimits(
            memory = 5_000_000,         // 5 MB
            cpuTimeMs = 50,             // 50 ms
            fileSize = 500_000,         // 500 KB
            componentCount = 50,        // 50 components
            nestingDepth = 5            // 5 levels deep
        )
    }
}

/**
 * Current resource usage
 */
data class ResourceUsage(
    val memoryBytes: Long = 0,
    val cpuTimeMs: Long = 0,
    val componentCount: Int = 0,
    val nestingDepth: Int = 0
)

/**
 * Network access policy
 */
enum class NetworkPolicy {
    NONE,           // No network access
    READ_ONLY,      // Can read but not send
    WHITELIST,      // Only whitelisted domains
    FULL            // Full network access (not recommended)
}

/**
 * File system access policy
 */
enum class FileSystemAccess {
    NONE,           // No file system access
    PLUGIN_DIR,     // Only plugin's own directory
    TEMP_DIR,       // Only temp directory
    FULL            // Full file system access (not recommended)
}

/**
 * Reflection policy
 */
enum class ReflectionPolicy {
    NONE,           // No reflection
    RESTRICTED,     // Only AvaElements types
    FULL            // Full reflection (not recommended)
}

/**
 * Permission manager
 */
object PermissionManager {
    /**
     * Check if permission is allowed for plugin
     */
    fun checkPermission(pluginId: String, permission: Permission): Boolean {
        // Platform-specific implementation
        return expect_checkPermission(pluginId, permission)
    }

    /**
     * Request permission from user
     */
    suspend fun requestPermission(pluginId: String, permission: Permission): Boolean {
        // Platform-specific implementation
        return expect_requestPermission(pluginId, permission)
    }
}

/**
 * Platform-specific permission checking
 */
expect fun expect_checkPermission(pluginId: String, permission: Permission): Boolean

/**
 * Platform-specific permission requesting
 */
expect suspend fun expect_requestPermission(pluginId: String, permission: Permission): Boolean
