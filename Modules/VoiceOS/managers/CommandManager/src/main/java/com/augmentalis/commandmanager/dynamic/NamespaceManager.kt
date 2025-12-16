/**
 * NamespaceManager.kt - Facade for namespace management operations
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Provides high-level namespace management and module isolation
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.commandmanager.dynamic

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Manager for namespace operations
 *
 * Provides:
 * - Namespace creation and deletion
 * - Cross-namespace query and management
 * - Namespace validation
 * - Isolation policies
 * - Access control
 *
 * Thread-safe implementation using ConcurrentHashMap and ReentrantReadWriteLock
 */
class NamespaceManager(
    private val defaultConfig: NamespaceConfig = NamespaceConfig()
) {

    companion object {
        private const val TAG = "NamespaceManager"

        // Reserved namespace names
        val RESERVED_NAMESPACES = setOf(
            NamespaceHelper.DEFAULT_NAMESPACE,
            NamespaceHelper.SYSTEM_NAMESPACE,
            NamespaceHelper.USER_NAMESPACE
        )
    }

    // Thread-safe namespace storage
    private val namespaces = ConcurrentHashMap<String, CommandNamespace>()
    private val namespaceConfigs = ConcurrentHashMap<String, NamespaceConfig>()
    private val lock = ReentrantReadWriteLock()

    init {
        // Create default namespaces
        createDefaultNamespaces()
    }

    /**
     * Create a new namespace
     *
     * @param name Namespace name (must be unique)
     * @param description Optional description
     * @param owner Optional owner identifier
     * @param config Namespace configuration (uses default if not specified)
     * @param metadata Additional metadata
     * @return Result indicating success or failure
     */
    suspend fun createNamespace(
        name: String,
        description: String = "",
        owner: String? = null,
        config: NamespaceConfig = defaultConfig,
        metadata: Map<String, String> = emptyMap()
    ): Result<CommandNamespace> = lock.write {
        try {
            // Validate namespace name
            val validation = NamespaceHelper.validateNamespaceName(name)
            if (validation is NamespaceValidationResult.Invalid) {
                return@write Result.failure(
                    IllegalArgumentException("Invalid namespace name: ${validation.getErrorMessage()}")
                )
            }

            // Check if namespace already exists
            if (namespaces.containsKey(name)) {
                return@write Result.failure(
                    IllegalStateException("Namespace '$name' already exists")
                )
            }

            // Create namespace
            val namespace = CommandNamespace(
                name = name,
                description = description,
                owner = owner,
                metadata = metadata
            )

            namespaces[name] = namespace
            namespaceConfigs[name] = config

            Log.i(TAG, "Created namespace: $name")
            Result.success(namespace)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create namespace: $name", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a namespace
     *
     * @param name Namespace to delete
     * @param force Force deletion even if namespace contains commands
     * @return Result indicating success or failure
     */
    suspend fun deleteNamespace(
        name: String,
        force: Boolean = false
    ): Result<Unit> = lock.write {
        try {
            // Check if namespace is reserved
            if (name in RESERVED_NAMESPACES) {
                return@write Result.failure(
                    IllegalArgumentException("Cannot delete reserved namespace: $name")
                )
            }

            val namespace = namespaces[name] ?: return@write Result.failure(
                NoSuchElementException("Namespace '$name' not found")
            )

            // Check if namespace has commands
            if (!force && !namespace.isEmpty()) {
                return@write Result.failure(
                    IllegalStateException(
                        "Namespace '$name' contains ${namespace.size()} commands. " +
                        "Use force=true to delete anyway."
                    )
                )
            }

            namespaces.remove(name)
            namespaceConfigs.remove(name)

            Log.i(TAG, "Deleted namespace: $name")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete namespace: $name", e)
            Result.failure(e)
        }
    }

    /**
     * Get a namespace by name
     *
     * @param name Namespace name
     * @return The namespace, or null if not found
     */
    fun getNamespace(name: String): CommandNamespace? = lock.read {
        namespaces[name]
    }

    /**
     * Get all namespaces
     *
     * @return List of all namespaces
     */
    fun getAllNamespaces(): List<CommandNamespace> = lock.read {
        namespaces.values.toList()
    }

    /**
     * Get namespace names
     *
     * @param includeReserved Include reserved namespaces
     * @return List of namespace names
     */
    fun getNamespaceNames(includeReserved: Boolean = true): List<String> = lock.read {
        if (includeReserved) {
            namespaces.keys.toList()
        } else {
            namespaces.keys.filter { it !in RESERVED_NAMESPACES }
        }
    }

    /**
     * Get namespace configuration
     *
     * @param name Namespace name
     * @return Configuration, or default if not found
     */
    fun getNamespaceConfig(name: String): NamespaceConfig {
        return namespaceConfigs[name] ?: defaultConfig
    }

    /**
     * Update namespace configuration
     *
     * @param name Namespace name
     * @param config New configuration
     * @return Result indicating success or failure
     */
    suspend fun updateNamespaceConfig(
        name: String,
        config: NamespaceConfig
    ): Result<Unit> = lock.write {
        try {
            if (!namespaces.containsKey(name)) {
                return@write Result.failure(
                    NoSuchElementException("Namespace '$name' not found")
                )
            }

            namespaceConfigs[name] = config
            Log.i(TAG, "Updated config for namespace: $name")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update namespace config: $name", e)
            Result.failure(e)
        }
    }

    /**
     * Get commands from all namespaces matching a phrase
     *
     * @param phrase Phrase to match
     * @param namespaceFilter Optional namespace name filter
     * @param enabledOnly Only return enabled commands
     * @return Map of namespace name to matching commands
     */
    suspend fun findCommandsAcrossNamespaces(
        phrase: String,
        namespaceFilter: String? = null,
        enabledOnly: Boolean = true
    ): Map<String, List<VoiceCommand>> = lock.read {
        val targetNamespaces = if (namespaceFilter != null) {
            listOfNotNull(namespaces[namespaceFilter])
        } else {
            namespaces.values.toList()
        }

        targetNamespaces.associate { namespace ->
            namespace.name to namespace.findMatchingCommands(phrase, enabledOnly)
        }.filterValues { it.isNotEmpty() }
    }

    /**
     * Get statistics for all namespaces
     *
     * @return Map of namespace name to statistics
     */
    suspend fun getAllNamespaceStatistics(): Map<String, NamespaceStatistics> = lock.read {
        namespaces.mapValues { (_, namespace) ->
            namespace.getStatistics()
        }
    }

    /**
     * Get total command count across all namespaces
     *
     * @param enabledOnly Only count enabled commands
     * @return Total number of commands
     */
    fun getTotalCommandCount(enabledOnly: Boolean = false): Int = lock.read {
        namespaces.values.sumOf {
            if (enabledOnly) it.getEnabledCommands().size else it.size()
        }
    }

    /**
     * Merge commands from one namespace to another
     *
     * @param sourceNamespace Source namespace name
     * @param targetNamespace Target namespace name
     * @param deleteSource Delete source namespace after merge
     * @return Result indicating success or failure
     */
    suspend fun mergeNamespaces(
        sourceNamespace: String,
        targetNamespace: String,
        deleteSource: Boolean = false
    ): Result<Int> = lock.write {
        try {
            val source = namespaces[sourceNamespace] ?: return@write Result.failure(
                NoSuchElementException("Source namespace '$sourceNamespace' not found")
            )

            val target = namespaces[targetNamespace] ?: return@write Result.failure(
                NoSuchElementException("Target namespace '$targetNamespace' not found")
            )

            val commands = source.getAllCommands()
            var merged = 0

            for (command in commands) {
                // Update namespace
                val updated = command.copy(namespace = targetNamespace)
                if (target.addCommand(updated)) {
                    merged++
                }
            }

            if (deleteSource) {
                namespaces.remove(sourceNamespace)
                namespaceConfigs.remove(sourceNamespace)
            } else {
                source.clear()
            }

            Log.i(TAG, "Merged $merged commands from '$sourceNamespace' to '$targetNamespace'")
            Result.success(merged)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge namespaces", e)
            Result.failure(e)
        }
    }

    /**
     * Clone a namespace
     *
     * @param sourceNamespace Source namespace name
     * @param targetNamespace New namespace name
     * @return Result indicating success or failure
     */
    suspend fun cloneNamespace(
        sourceNamespace: String,
        targetNamespace: String
    ): Result<CommandNamespace> = lock.write {
        try {
            val source = namespaces[sourceNamespace] ?: return@write Result.failure(
                NoSuchElementException("Source namespace '$sourceNamespace' not found")
            )

            // Validate target namespace name
            val validation = NamespaceHelper.validateNamespaceName(targetNamespace)
            if (validation is NamespaceValidationResult.Invalid) {
                return@write Result.failure(
                    IllegalArgumentException("Invalid namespace name: ${validation.getErrorMessage()}")
                )
            }

            if (namespaces.containsKey(targetNamespace)) {
                return@write Result.failure(
                    IllegalStateException("Target namespace '$targetNamespace' already exists")
                )
            }

            // Create new namespace
            val target = CommandNamespace(
                name = targetNamespace,
                description = "Clone of $sourceNamespace",
                owner = source.owner
            )

            // Copy all commands
            val commands = source.getAllCommands()
            for (command in commands) {
                val cloned = command.copy(
                    namespace = targetNamespace,
                    createdAt = System.currentTimeMillis(),
                    usageCount = 0L,
                    lastUsed = 0L
                )
                target.addCommand(cloned)
            }

            namespaces[targetNamespace] = target
            namespaceConfigs[targetNamespace] = namespaceConfigs[sourceNamespace] ?: defaultConfig

            Log.i(TAG, "Cloned namespace '$sourceNamespace' to '$targetNamespace'")
            Result.success(target)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clone namespace", e)
            Result.failure(e)
        }
    }

    /**
     * Export commands from a namespace
     *
     * @param namespaceName Namespace to export
     * @return List of VoiceCommandData for persistence
     */
    suspend fun exportNamespace(namespaceName: String): Result<List<VoiceCommandData>> = lock.read {
        try {
            val namespace = namespaces[namespaceName] ?: return@read Result.failure(
                NoSuchElementException("Namespace '$namespaceName' not found")
            )

            val exportedCommands = namespace.exportCommands()

            // Warn if exporting empty namespace
            if (exportedCommands.isEmpty()) {
                Log.w(TAG, "WARNING: Exporting empty namespace '$namespaceName' - " +
                        "no commands will be included in export")
            }

            Result.success(exportedCommands)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export namespace: $namespaceName", e)
            Result.failure(e)
        }
    }

    /**
     * Import commands into a namespace
     *
     * @param namespaceName Target namespace
     * @param commands Commands to import
     * @param actionProvider Function to provide command actions
     * @return Result with number of commands imported
     */
    suspend fun importNamespace(
        namespaceName: String,
        commands: List<VoiceCommandData>,
        actionProvider: (VoiceCommandData) -> suspend (CommandExecutionContext) -> CommandResult
    ): Result<Int> = lock.write {
        try {
            // Warn if importing empty command list
            if (commands.isEmpty()) {
                Log.w(TAG, "WARNING: Importing empty command list into namespace '$namespaceName' - " +
                        "no commands will be added")
                return@write Result.success(0)
            }

            val namespace = getOrCreateNamespace(namespaceName)
            var imported = 0

            for (commandData in commands) {
                val command = VoiceCommand(
                    id = commandData.id,
                    phrases = commandData.phrases,
                    priority = commandData.priority,
                    namespace = namespaceName, // Override with target namespace
                    description = commandData.description,
                    category = commandData.category,
                    enabled = commandData.enabled,
                    createdAt = commandData.createdAt,
                    lastUsed = commandData.lastUsed,
                    usageCount = commandData.usageCount,
                    metadata = commandData.metadata,
                    action = actionProvider(commandData)
                )

                if (namespace.addCommand(command)) {
                    imported++
                }
            }

            Log.i(TAG, "Imported $imported commands into namespace: $namespaceName")
            Result.success(imported)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to import namespace: $namespaceName", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all commands from all namespaces
     *
     * @param preserveReserved Keep reserved namespaces (system, default, user)
     * @return Number of commands removed
     */
    suspend fun clearAll(preserveReserved: Boolean = true): Int = lock.write {
        var totalRemoved = 0

        for ((name, namespace) in namespaces) {
            if (preserveReserved && name in RESERVED_NAMESPACES) {
                continue
            }

            totalRemoved += namespace.clear()
        }

        Log.i(TAG, "Cleared $totalRemoved commands from all namespaces")
        totalRemoved
    }

    /**
     * Validate namespace isolation (check for cross-namespace conflicts)
     *
     * @return Map of namespace pairs to conflicts
     */
    suspend fun validateIsolation(): Map<Pair<String, String>, List<String>> = lock.read {
        val conflicts = mutableMapOf<Pair<String, String>, List<String>>()

        val namespaceList = namespaces.values.toList()
        for (i in namespaceList.indices) {
            for (j in (i + 1) until namespaceList.size) {
                val ns1 = namespaceList[i]
                val ns2 = namespaceList[j]

                val conflictingPhrases = findConflictingPhrases(ns1, ns2)
                if (conflictingPhrases.isNotEmpty()) {
                    conflicts[Pair(ns1.name, ns2.name)] = conflictingPhrases
                }
            }
        }

        conflicts
    }

    /**
     * Get health report for all namespaces
     *
     * @return Health report
     */
    suspend fun getHealthReport(): NamespaceHealthReport = lock.read {
        val stats = getAllNamespaceStatistics()
        val totalCommands = stats.values.sumOf { it.totalCommands }
        val totalEnabled = stats.values.sumOf { it.enabledCommands }
        val averageCommandsPerNamespace = if (stats.isNotEmpty()) {
            totalCommands.toFloat() / stats.size
        } else {
            0f
        }

        val largestNamespace = stats.maxByOrNull { it.value.totalCommands }
        val mostActiveNamespace = stats.maxByOrNull { it.value.totalExecutions }
        val conflicts = validateIsolation()

        NamespaceHealthReport(
            totalNamespaces = namespaces.size,
            totalCommands = totalCommands,
            totalEnabledCommands = totalEnabled,
            averageCommandsPerNamespace = averageCommandsPerNamespace,
            largestNamespace = largestNamespace?.key,
            mostActiveNamespace = mostActiveNamespace?.key,
            crossNamespaceConflicts = conflicts.size,
            namespaceStatistics = stats
        )
    }

    // Private helper methods

    /**
     * Get or create a namespace
     */
    private fun getOrCreateNamespace(name: String): CommandNamespace {
        return namespaces.getOrPut(name) {
            CommandNamespace(name = name).also {
                namespaceConfigs[name] = defaultConfig
            }
        }
    }

    /**
     * Create default namespaces
     */
    private fun createDefaultNamespaces() {
        namespaces[NamespaceHelper.DEFAULT_NAMESPACE] = CommandNamespace(
            name = NamespaceHelper.DEFAULT_NAMESPACE,
            description = "Default namespace for user commands"
        )

        namespaces[NamespaceHelper.SYSTEM_NAMESPACE] = CommandNamespace(
            name = NamespaceHelper.SYSTEM_NAMESPACE,
            description = "System-level commands"
        )

        namespaces[NamespaceHelper.USER_NAMESPACE] = CommandNamespace(
            name = NamespaceHelper.USER_NAMESPACE,
            description = "User-created custom commands"
        )

        Log.d(TAG, "Created default namespaces")
    }

    /**
     * Find conflicting phrases between two namespaces
     */
    private fun findConflictingPhrases(
        ns1: CommandNamespace,
        ns2: CommandNamespace
    ): List<String> {
        val phrases1 = ns1.getAllCommands().flatMap { it.phrases }.toSet()
        val phrases2 = ns2.getAllCommands().flatMap { it.phrases }.toSet()
        return phrases1.intersect(phrases2).toList()
    }
}

/**
 * Namespace health report
 */
data class NamespaceHealthReport(
    val totalNamespaces: Int,
    val totalCommands: Int,
    val totalEnabledCommands: Int,
    val averageCommandsPerNamespace: Float,
    val largestNamespace: String?,
    val mostActiveNamespace: String?,
    val crossNamespaceConflicts: Int,
    val namespaceStatistics: Map<String, NamespaceStatistics>
) {
    /**
     * Check if system is healthy
     */
    fun isHealthy(): Boolean {
        return crossNamespaceConflicts == 0 &&
               totalNamespaces > 0 &&
               averageCommandsPerNamespace > 0
    }

    /**
     * Get health score (0.0-1.0)
     */
    fun getHealthScore(): Float {
        var score = 1.0f

        // Deduct for conflicts
        score -= minOf(crossNamespaceConflicts * 0.1f, 0.3f)

        // Deduct if empty
        if (totalCommands == 0) {
            score -= 0.2f
        }

        // Deduct if imbalanced (one namespace has >80% of commands)
        val maxPercentage = namespaceStatistics.values
            .map { it.totalCommands.toFloat() / totalCommands }
            .maxOrNull() ?: 0f

        if (maxPercentage > 0.8f) {
            score -= 0.2f
        }

        return score.coerceIn(0f, 1f)
    }
}
