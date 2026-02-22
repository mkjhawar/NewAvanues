/**
 * CommandNamespace.kt - Namespace management for command isolation
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Provides namespace-based isolation and scoping for commands
 *
 * @since VOS4 Week 4
 */

package com.augmentalis.voiceoscore.commandmanager.dynamic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Manages namespaces for command isolation
 *
 * Namespaces provide:
 * - Module isolation (prevent cross-module conflicts)
 * - Scoped command queries
 * - Namespace-specific configuration
 * - Access control
 *
 * Thread-safe implementation using ReentrantReadWriteLock
 */
class CommandNamespace(
    val name: String,
    val description: String = "",
    val owner: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {

    // Thread-safe command storage
    private val commands = ConcurrentHashMap<String, VoiceCommand>()
    private val lock = ReentrantReadWriteLock()

    // Namespace statistics
    private var lastModified: Long = createdAt
    private var totalExecutions: Long = 0L

    init {
        require(name.isNotBlank()) { "Namespace name cannot be blank" }
        require(name.matches(Regex("^[a-z][a-z0-9_-]*$"))) {
            "Namespace name must start with lowercase letter and contain only lowercase letters, numbers, hyphens, and underscores"
        }
    }

    /**
     * Add a command to this namespace
     *
     * @param command The command to add
     * @return true if added successfully, false if command ID already exists
     */
    fun addCommand(command: VoiceCommand): Boolean = lock.write {
        if (commands.containsKey(command.id)) {
            false
        } else {
            // Ensure command namespace matches this namespace
            val namespacedCommand = if (command.namespace != name) {
                command.copy(namespace = name)
            } else {
                command
            }

            commands[command.id] = namespacedCommand
            lastModified = System.currentTimeMillis()
            true
        }
    }

    /**
     * Remove a command from this namespace
     *
     * @param commandId ID of the command to remove
     * @return The removed command, or null if not found
     */
    fun removeCommand(commandId: String): VoiceCommand? = lock.write {
        val removed = commands.remove(commandId)
        if (removed != null) {
            lastModified = System.currentTimeMillis()
        }
        removed
    }

    /**
     * Get a command by ID
     *
     * @param commandId The command ID to look up
     * @return The command, or null if not found
     */
    fun getCommand(commandId: String): VoiceCommand? = lock.read {
        commands[commandId]
    }

    /**
     * Get all commands in this namespace
     *
     * @return List of all commands
     */
    fun getAllCommands(): List<VoiceCommand> = lock.read {
        commands.values.toList()
    }

    /**
     * Get enabled commands only
     *
     * @return List of enabled commands
     */
    fun getEnabledCommands(): List<VoiceCommand> = lock.read {
        commands.values.filter { it.enabled }.toList()
    }

    /**
     * Get commands by category
     *
     * @param category The category to filter by
     * @return List of commands in the specified category
     */
    fun getCommandsByCategory(category: CommandCategory): List<VoiceCommand> = lock.read {
        commands.values.filter { it.category == category }.toList()
    }

    /**
     * Get commands by priority level
     *
     * @param priorityLevel The priority level to filter by
     * @return List of commands with the specified priority level
     */
    fun getCommandsByPriorityLevel(priorityLevel: PriorityLevel): List<VoiceCommand> = lock.read {
        commands.values.filter { it.getPriorityLevel() == priorityLevel }.toList()
    }

    /**
     * Find commands matching a phrase
     *
     * @param phrase The phrase to match
     * @param enabledOnly If true, only return enabled commands
     * @return List of commands that match the phrase, sorted by priority (highest first)
     */
    fun findMatchingCommands(phrase: String, enabledOnly: Boolean = true): List<VoiceCommand> = lock.read {
        val matchingCommands = commands.values
            .filter { if (enabledOnly) it.enabled else true }
            .filter { it.matches(phrase) }
            .toList()

        // Sort by priority (highest first)
        matchingCommands.sortedByDescending { it.priority }
    }

    /**
     * Find similar commands using fuzzy matching
     *
     * @param phrase The phrase to match against
     * @param minSimilarity Minimum similarity threshold (0.0 - 1.0)
     * @param maxResults Maximum number of results to return
     * @param enabledOnly If true, only return enabled commands
     * @return List of commands sorted by similarity (highest first)
     */
    fun findSimilarCommands(
        phrase: String,
        minSimilarity: Float = 0.7f,
        maxResults: Int = 5,
        enabledOnly: Boolean = true
    ): List<Pair<VoiceCommand, Float>> = lock.read {
        commands.values
            .filter { if (enabledOnly) it.enabled else true }
            .map { cmd -> cmd to cmd.getSimilarity(phrase) }
            .filter { it.second >= minSimilarity }
            .sortedByDescending { it.second }
            .take(maxResults)
    }

    /**
     * Update a command in this namespace
     *
     * @param commandId ID of command to update
     * @param updater Function to transform the command
     * @return true if updated successfully, false if command not found
     */
    fun updateCommand(commandId: String, updater: (VoiceCommand) -> VoiceCommand): Boolean = lock.write {
        val existing = commands[commandId]
        if (existing != null) {
            val updated = updater(existing)
            // Ensure namespace stays the same
            commands[commandId] = updated.copy(namespace = name)
            lastModified = System.currentTimeMillis()
            true
        } else {
            false
        }
    }

    /**
     * Enable or disable a command
     *
     * @param commandId ID of command to modify
     * @param enabled New enabled state
     * @return true if updated, false if command not found
     */
    fun setCommandEnabled(commandId: String, enabled: Boolean): Boolean {
        return updateCommand(commandId) { it.copy(enabled = enabled) }
    }

    /**
     * Update command priority
     *
     * @param commandId ID of command to modify
     * @param priority New priority value (1-100)
     * @return true if updated, false if command not found
     */
    fun setCommandPriority(commandId: String, priority: Int): Boolean {
        require(priority in 1..100) { "Priority must be between 1 and 100" }
        return updateCommand(commandId) { it.copy(priority = priority) }
    }

    /**
     * Record command execution
     *
     * @param commandId ID of executed command
     */
    fun recordExecution(commandId: String) = lock.write {
        val command = commands[commandId]
        if (command != null) {
            commands[commandId] = command.recordUsage()
            totalExecutions++
            lastModified = System.currentTimeMillis()
        }
    }

    /**
     * Clear all commands from this namespace
     *
     * @return Number of commands removed
     */
    fun clear(): Int = lock.write {
        val count = commands.size
        commands.clear()
        lastModified = System.currentTimeMillis()
        count
    }

    /**
     * Check if namespace contains a command
     *
     * @param commandId The command ID to check
     * @return true if command exists in this namespace
     */
    fun contains(commandId: String): Boolean = lock.read {
        commands.containsKey(commandId)
    }

    /**
     * Check if namespace is empty
     *
     * @return true if no commands are registered
     */
    fun isEmpty(): Boolean = lock.read {
        commands.isEmpty()
    }

    /**
     * Get number of commands in namespace
     *
     * @return Count of commands
     */
    fun size(): Int = lock.read {
        commands.size
    }

    /**
     * Get namespace statistics
     *
     * @return Statistics for this namespace
     */
    fun getStatistics(): NamespaceStatistics = lock.read {
        val allCommands = commands.values.toList()
        val enabledCount = allCommands.count { it.enabled }
        val disabledCount = allCommands.size - enabledCount
        val averagePriority = if (allCommands.isNotEmpty()) {
            allCommands.map { it.priority }.average().toFloat()
        } else {
            0f
        }
        val totalUsage = allCommands.sumOf { it.usageCount }

        val byCategory = allCommands.groupBy { it.category }
            .mapValues { it.value.size }

        val byPriorityLevel = allCommands.groupBy { it.getPriorityLevel() }
            .mapValues { it.value.size }

        NamespaceStatistics(
            namespace = name,
            totalCommands = allCommands.size,
            enabledCommands = enabledCount,
            disabledCommands = disabledCount,
            averagePriority = averagePriority,
            totalUsageCount = totalUsage,
            totalExecutions = totalExecutions,
            commandsByCategory = byCategory,
            commandsByPriorityLevel = byPriorityLevel,
            createdAt = createdAt,
            lastModified = lastModified
        )
    }

    /**
     * Export all commands to a list
     *
     * @return List of VoiceCommandData for persistence/transfer
     */
    fun exportCommands(): List<VoiceCommandData> = lock.read {
        commands.values.map { VoiceCommandData.from(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandNamespace) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String =
        "CommandNamespace(name='$name', commands=${commands.size}, owner=$owner)"
}

/**
 * Statistics for a namespace
 */
data class NamespaceStatistics(
    val namespace: String,
    val totalCommands: Int,
    val enabledCommands: Int,
    val disabledCommands: Int,
    val averagePriority: Float,
    val totalUsageCount: Long,
    val totalExecutions: Long,
    val commandsByCategory: Map<CommandCategory, Int>,
    val commandsByPriorityLevel: Map<PriorityLevel, Int>,
    val createdAt: Long,
    val lastModified: Long
) {
    /**
     * Get enabled percentage
     */
    fun getEnabledPercentage(): Float =
        if (totalCommands > 0) (enabledCommands.toFloat() / totalCommands) * 100f else 0f

    /**
     * Get average usage per command
     */
    fun getAverageUsagePerCommand(): Float =
        if (totalCommands > 0) totalUsageCount.toFloat() / totalCommands else 0f

    /**
     * Check if namespace is active (has been used recently)
     */
    fun isActive(thresholdMillis: Long = 24 * 60 * 60 * 1000L): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastModified) < thresholdMillis
    }
}

/**
 * Namespace configuration
 */
data class NamespaceConfig(
    val allowConflicts: Boolean = false,
    val maxCommands: Int = 1000,
    val defaultPriority: Int = 50,
    val autoEnableNew: Boolean = true,
    val crossNamespaceConflictCheck: Boolean = false
) {
    init {
        require(maxCommands > 0) { "Max commands must be positive" }
        require(defaultPriority in 1..100) { "Default priority must be between 1 and 100" }
    }
}

/**
 * Result of namespace validation
 */
sealed class NamespaceValidationResult {
    object Valid : NamespaceValidationResult()

    data class Invalid(
        val errors: List<String>
    ) : NamespaceValidationResult() {
        fun getErrorMessage(): String = errors.joinToString("; ")
    }
}

/**
 * Namespace manager helper functions
 */
object NamespaceHelper {

    /**
     * Standard namespace names
     */
    const val DEFAULT_NAMESPACE = "default"
    const val SYSTEM_NAMESPACE = "system"
    const val USER_NAMESPACE = "user"
    const val APP_NAMESPACE_PREFIX = "app-"
    const val MODULE_NAMESPACE_PREFIX = "module-"

    /**
     * Validate namespace name
     *
     * @param name The namespace name to validate
     * @return Validation result
     */
    fun validateNamespaceName(name: String): NamespaceValidationResult {
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("Namespace name cannot be blank")
        }

        if (!name.matches(Regex("^[a-z][a-z0-9_-]*$"))) {
            errors.add("Namespace name must start with lowercase letter and contain only lowercase letters, numbers, hyphens, and underscores")
        }

        if (name.length > 50) {
            errors.add("Namespace name cannot exceed 50 characters")
        }

        return if (errors.isEmpty()) {
            NamespaceValidationResult.Valid
        } else {
            NamespaceValidationResult.Invalid(errors)
        }
    }

    /**
     * Create namespace name for an app
     *
     * @param packageName The app's package name
     * @return Formatted namespace name
     */
    fun createAppNamespace(packageName: String): String {
        val sanitized = packageName
            .replace(".", "-")
            .replace("_", "-")
            .lowercase()
        return "$APP_NAMESPACE_PREFIX$sanitized"
    }

    /**
     * Create namespace name for a module
     *
     * @param moduleName The module name
     * @return Formatted namespace name
     */
    fun createModuleNamespace(moduleName: String): String {
        val sanitized = moduleName
            .replace(Regex("[^a-z0-9]"), "-")
            .lowercase()
        return "$MODULE_NAMESPACE_PREFIX$sanitized"
    }

    /**
     * Check if namespace is a system namespace
     *
     * @param name Namespace name to check
     * @return true if this is a system namespace
     */
    fun isSystemNamespace(name: String): Boolean =
        name == SYSTEM_NAMESPACE

    /**
     * Check if namespace is an app namespace
     *
     * @param name Namespace name to check
     * @return true if this is an app namespace
     */
    fun isAppNamespace(name: String): Boolean =
        name.startsWith(APP_NAMESPACE_PREFIX)

    /**
     * Check if namespace is a module namespace
     *
     * @param name Namespace name to check
     * @return true if this is a module namespace
     */
    fun isModuleNamespace(name: String): Boolean =
        name.startsWith(MODULE_NAMESPACE_PREFIX)
}
