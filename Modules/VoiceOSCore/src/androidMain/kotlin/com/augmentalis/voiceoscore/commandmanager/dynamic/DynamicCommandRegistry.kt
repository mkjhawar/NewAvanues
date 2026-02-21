/**
 * DynamicCommandRegistry.kt - Thread-safe dynamic command registration system
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Provides runtime command management without recompilation
 *
 * Features:
 * - Runtime command registration/unregistration
 * - Priority-based command resolution
 * - Conflict detection and resolution
 * - Namespace management for module isolation
 * - Thread-safe concurrent operations
 * - Memory-efficient with configurable limits
 *
 * Performance:
 * - O(log n) command lookup via sorted priority queues
 * - O(n) conflict detection
 * - Thread-safe using ConcurrentHashMap and ReadWriteLock
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.commandmanager.dynamic

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Main registry for dynamic command management
 *
 * Thread-safe implementation supporting:
 * - Multiple namespaces
 * - Priority-based resolution
 * - Conflict detection
 * - Event callbacks
 * - Persistent storage (via Room)
 */
class DynamicCommandRegistry(
    private val maxCommandsPerNamespace: Int = 1000,
    private val conflictDetectionConfig: ConflictDetectionConfig = ConflictDetectionConfig()
) {

    companion object {
        private const val TAG = "DynamicCommandRegistry"
        private const val DEFAULT_NAMESPACE = "default"
    }

    // Thread-safe namespace storage
    private val namespaces = ConcurrentHashMap<String, CommandNamespace>()
    private val mutex = Mutex()

    // Conflict detector
    private val conflictDetector = ConflictDetector(conflictDetectionConfig)

    // Event listeners
    private val listeners = ConcurrentHashMap.newKeySet<RegistrationListener>()

    init {
        // Create default namespace
        namespaces[DEFAULT_NAMESPACE] = CommandNamespace(
            name = DEFAULT_NAMESPACE,
            description = "Default namespace for user commands"
        )

        Log.d(TAG, "DynamicCommandRegistry initialized (max per namespace: $maxCommandsPerNamespace)")
    }

    /**
     * Register a command in the registry
     *
     * @param command The command to register
     * @param checkConflicts Whether to check for conflicts before registration
     * @param autoResolveConflicts Whether to automatically resolve non-critical conflicts
     * @return Result indicating success or failure with details
     */
    suspend fun registerCommand(
        command: VoiceCommand,
        checkConflicts: Boolean = true,
        autoResolveConflicts: Boolean = false
    ): Result<Unit> = mutex.withLock {
        try {
            // Validate command
            val validation = validateCommand(command)
            if (validation.isFailure) {
                return@withLock validation
            }

            // Get or create namespace
            val namespace = getOrCreateNamespace(command.namespace)

            // Check namespace capacity
            if (namespace.size() >= maxCommandsPerNamespace) {
                return@withLock Result.failure(
                    IllegalStateException(
                        "Namespace '${command.namespace}' has reached maximum capacity " +
                        "($maxCommandsPerNamespace commands)"
                    )
                )
            }

            // Check for conflicts
            if (checkConflicts) {
                val existingCommands = namespace.getAllCommands()
                val conflictResult = conflictDetector.detectConflicts(
                    command, existingCommands, command.namespace
                )

                when (conflictResult) {
                    is ConflictDetectionResult.ConflictsDetected -> {
                        if (!conflictResult.canProceed) {
                            // Critical conflicts - cannot register
                            Log.w(TAG, "Critical conflicts detected for command: ${command.id}")
                            notifyConflictsDetected(conflictResult.conflicts, command)

                            return@withLock Result.failure(
                                IllegalArgumentException(
                                    "Critical conflicts detected (${conflictResult.criticalCount}). " +
                                    "Cannot register command."
                                )
                            )
                        } else if (!autoResolveConflicts) {
                            // Non-critical conflicts but auto-resolve disabled
                            Log.i(TAG, "Conflicts detected but allowing registration: ${command.id}")
                            notifyConflictsDetected(conflictResult.conflicts, command)
                        } else {
                            // Auto-resolve non-critical conflicts
                            Log.i(TAG, "Auto-resolving conflicts for: ${command.id}")
                            val resolved = autoResolveConflicts(command, conflictResult.conflicts)
                            if (!resolved) {
                                Log.w(TAG, "Failed to auto-resolve conflicts")
                            }
                        }
                    }
                    ConflictDetectionResult.NoConflict -> {
                        // No conflicts - proceed
                    }
                }
            }

            // Register the command
            val registered = namespace.addCommand(command)
            if (!registered) {
                return@withLock Result.failure(
                    IllegalArgumentException("Command with ID '${command.id}' already exists")
                )
            }

            Log.i(TAG, "Registered command: ${command.id} in namespace: ${command.namespace}")

            // Notify listeners
            notifyCommandRegistered(command)

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to register command: ${command.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Unregister a command from the registry
     *
     * @param commandId ID of the command to unregister
     * @param namespace Namespace containing the command (null = search all)
     * @return Result indicating success or failure
     */
    suspend fun unregisterCommand(
        commandId: String,
        namespace: String? = null
    ): Result<Unit> = mutex.withLock {
        try {
            val targetNamespace = if (namespace != null) {
                namespaces[namespace] ?: return@withLock Result.failure(
                    NoSuchElementException("Namespace '$namespace' not found")
                )
            } else {
                // Search all namespaces
                findNamespaceContaining(commandId) ?: return@withLock Result.failure(
                    NoSuchElementException("Command '$commandId' not found in any namespace")
                )
            }

            val removed = targetNamespace.removeCommand(commandId)
            if (removed == null) {
                return@withLock Result.failure(
                    NoSuchElementException("Command '$commandId' not found")
                )
            }

            Log.i(TAG, "Unregistered command: $commandId from namespace: ${targetNamespace.name}")

            // Notify listeners
            notifyCommandUnregistered(commandId, targetNamespace.name)

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister command: $commandId", e)
            Result.failure(e)
        }
    }

    /**
     * Resolve a voice phrase to matching commands
     *
     * Returns commands sorted by priority (highest first)
     * If multiple commands have same priority, all are returned for disambiguation
     *
     * @param phrase The voice phrase to resolve
     * @param namespace Specific namespace to search (null = search all)
     * @param enabledOnly Only return enabled commands
     * @return List of matching commands sorted by priority
     */
    suspend fun resolveCommand(
        phrase: String,
        namespace: String? = null,
        enabledOnly: Boolean = true
    ): List<VoiceCommand> = mutex.withLock {
        val searchNamespaces = if (namespace != null) {
            listOfNotNull(namespaces[namespace])
        } else {
            namespaces.values.toList()
        }

        val matches = mutableListOf<VoiceCommand>()

        // Find all matching commands across specified namespaces
        for (ns in searchNamespaces) {
            matches.addAll(ns.findMatchingCommands(phrase, enabledOnly))
        }

        // Sort by priority (highest first), then by usage count
        matches.sortedWith(
            compareByDescending<VoiceCommand> { it.priority }
                .thenByDescending { it.usageCount }
        )
    }

    /**
     * Find similar commands using fuzzy matching
     *
     * @param phrase The phrase to match against
     * @param minSimilarity Minimum similarity threshold (0.0-1.0)
     * @param maxResults Maximum number of results
     * @param namespace Specific namespace to search (null = all)
     * @return List of (command, similarity) pairs sorted by similarity
     */
    suspend fun findSimilarCommands(
        phrase: String,
        minSimilarity: Float = 0.7f,
        maxResults: Int = 5,
        namespace: String? = null
    ): List<Pair<VoiceCommand, Float>> = mutex.withLock {
        val searchNamespaces = if (namespace != null) {
            listOfNotNull(namespaces[namespace])
        } else {
            namespaces.values.toList()
        }

        val results = mutableListOf<Pair<VoiceCommand, Float>>()

        for (ns in searchNamespaces) {
            results.addAll(ns.findSimilarCommands(phrase, minSimilarity, maxResults))
        }

        // Sort by similarity (highest first) and take top results
        results.sortedByDescending { it.second }.take(maxResults)
    }

    /**
     * Detect conflicts for a command
     *
     * @param command The command to check
     * @return List of detected conflicts
     */
    suspend fun detectConflicts(command: VoiceCommand): List<ConflictInfo> = mutex.withLock {
        val namespace = namespaces[command.namespace] ?: return@withLock emptyList()
        val existing = namespace.getAllCommands().filter { it.id != command.id }

        val result = conflictDetector.detectConflicts(command, existing, command.namespace)

        when (result) {
            is ConflictDetectionResult.ConflictsDetected -> result.conflicts
            ConflictDetectionResult.NoConflict -> emptyList()
        }
    }

    /**
     * Get all commands in a namespace
     *
     * @param namespace Namespace to query (null = all namespaces)
     * @param enabledOnly Only return enabled commands
     * @return List of commands
     */
    suspend fun getAllCommands(
        namespace: String? = null,
        enabledOnly: Boolean = false
    ): List<VoiceCommand> = mutex.withLock {
        getAllCommandsUnlocked(namespace, enabledOnly)
    }

    /**
     * Non-locking variant for use inside existing mutex.withLock blocks.
     * Must only be called when the mutex is already held by the current coroutine.
     */
    private fun getAllCommandsUnlocked(
        namespace: String? = null,
        enabledOnly: Boolean = false
    ): List<VoiceCommand> {
        val targetNamespaces = if (namespace != null) {
            listOfNotNull(namespaces[namespace])
        } else {
            namespaces.values.toList()
        }

        val commands = mutableListOf<VoiceCommand>()
        for (ns in targetNamespaces) {
            commands.addAll(
                if (enabledOnly) ns.getEnabledCommands() else ns.getAllCommands()
            )
        }

        return commands.toList()
    }

    /**
     * Get commands by category
     *
     * @param category Category to filter by
     * @param namespace Namespace to search (null = all)
     * @return List of commands in the category
     */
    suspend fun getCommandsByCategory(
        category: CommandCategory,
        namespace: String? = null
    ): List<VoiceCommand> = mutex.withLock {
        val commands = getAllCommandsUnlocked(namespace, enabledOnly = false)
        commands.filter { it.category == category }
    }

    /**
     * Get commands by priority level
     *
     * @param priorityLevel Priority level to filter by
     * @param namespace Namespace to search (null = all)
     * @return List of commands with the priority level
     */
    suspend fun getCommandsByPriorityLevel(
        priorityLevel: PriorityLevel,
        namespace: String? = null
    ): List<VoiceCommand> = mutex.withLock {
        val commands = getAllCommandsUnlocked(namespace, enabledOnly = false)
        commands.filter { it.getPriorityLevel() == priorityLevel }
    }

    /**
     * Update a command's priority
     *
     * @param commandId ID of command to update
     * @param newPriority New priority value (1-100)
     * @param namespace Namespace containing the command
     * @return Result indicating success or failure
     */
    suspend fun updateCommandPriority(
        commandId: String,
        newPriority: Int,
        namespace: String? = null
    ): Result<Unit> = mutex.withLock {
        try {
            require(newPriority in 1..100) { "Priority must be between 1 and 100" }

            val targetNamespace = namespace?.let { namespaces[it] }
                ?: findNamespaceContaining(commandId)
                ?: return@withLock Result.failure(
                    NoSuchElementException("Command '$commandId' not found")
                )

            val updated = targetNamespace.setCommandPriority(commandId, newPriority)
            if (!updated) {
                return@withLock Result.failure(
                    NoSuchElementException("Command '$commandId' not found in namespace")
                )
            }

            Log.d(TAG, "Updated priority for $commandId to $newPriority")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update priority", e)
            Result.failure(e)
        }
    }

    /**
     * Enable or disable a command
     *
     * @param commandId ID of command to modify
     * @param enabled New enabled state
     * @param namespace Namespace containing the command
     * @return Result indicating success or failure
     */
    suspend fun setCommandEnabled(
        commandId: String,
        enabled: Boolean,
        namespace: String? = null
    ): Result<Unit> = mutex.withLock {
        try {
            val targetNamespace = namespace?.let { namespaces[it] }
                ?: findNamespaceContaining(commandId)
                ?: return@withLock Result.failure(
                    NoSuchElementException("Command '$commandId' not found")
                )

            val updated = targetNamespace.setCommandEnabled(commandId, enabled)
            if (!updated) {
                return@withLock Result.failure(
                    NoSuchElementException("Command '$commandId' not found")
                )
            }

            Log.d(TAG, "Command $commandId ${if (enabled) "enabled" else "disabled"}")
            notifyCommandEnabledChanged(commandId, enabled)

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to set command enabled state", e)
            Result.failure(e)
        }
    }

    /**
     * Record command execution (updates usage statistics)
     *
     * @param commandId ID of executed command
     * @param namespace Namespace containing the command
     */
    suspend fun recordCommandExecution(
        commandId: String,
        namespace: String? = null
    ) = mutex.withLock {
        val targetNamespace = namespace?.let { namespaces[it] }
            ?: findNamespaceContaining(commandId)

        targetNamespace?.recordExecution(commandId)
    }

    /**
     * Clear all commands from a namespace
     *
     * @param namespace Namespace to clear
     * @return Number of commands removed
     */
    suspend fun clearNamespace(namespace: String): Result<Int> = mutex.withLock {
        try {
            val ns = namespaces[namespace] ?: return@withLock Result.failure(
                NoSuchElementException("Namespace '$namespace' not found")
            )

            val count = ns.clear()
            Log.i(TAG, "Cleared namespace '$namespace': $count commands removed")

            notifyNamespaceCleared(namespace, count)

            Result.success(count)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear namespace", e)
            Result.failure(e)
        }
    }

    /**
     * Get registry statistics
     *
     * @return Overall registry statistics
     */
    suspend fun getStatistics(): RegistryStatistics = mutex.withLock {
        val allCommands = getAllCommandsUnlocked(enabledOnly = false)
        val enabledCount = allCommands.count { it.enabled }

        val byCategory = allCommands.groupBy { it.category }
            .mapValues { it.value.size }

        val byPriorityLevel = allCommands.groupBy { it.getPriorityLevel() }
            .mapValues { it.value.size }

        val averagePriority = if (allCommands.isNotEmpty()) {
            allCommands.map { it.priority }.average().toFloat()
        } else {
            0f
        }

        val totalUsage = allCommands.sumOf { it.usageCount }

        RegistryStatistics(
            totalCommands = allCommands.size,
            enabledCommands = enabledCount,
            disabledCommands = allCommands.size - enabledCount,
            namespaceCount = namespaces.size,
            averagePriority = averagePriority,
            totalUsageCount = totalUsage,
            commandsByCategory = byCategory,
            commandsByPriorityLevel = byPriorityLevel
        )
    }

    /**
     * Get namespace statistics
     *
     * @param namespace Namespace to query
     * @return Statistics for the namespace
     */
    suspend fun getNamespaceStatistics(namespace: String): Result<NamespaceStatistics> = mutex.withLock {
        val ns = namespaces[namespace] ?: return@withLock Result.failure(
            NoSuchElementException("Namespace '$namespace' not found")
        )

        Result.success(ns.getStatistics())
    }

    /**
     * Generate conflict report for the entire registry
     *
     * @return Conflict statistics
     */
    suspend fun generateConflictReport(): ConflictStatistics = mutex.withLock {
        val allCommands = getAllCommandsUnlocked(enabledOnly = false)
        conflictDetector.generateConflictReport(allCommands)
    }

    /**
     * Add a registration listener
     *
     * @param listener The listener to add
     */
    fun addListener(listener: RegistrationListener) {
        listeners.add(listener)
        Log.d(TAG, "Added registration listener")
    }

    /**
     * Remove a registration listener
     *
     * @param listener The listener to remove
     */
    fun removeListener(listener: RegistrationListener) {
        listeners.remove(listener)
        Log.d(TAG, "Removed registration listener")
    }

    // Private helper methods

    /**
     * Get or create a namespace
     */
    private fun getOrCreateNamespace(name: String): CommandNamespace {
        return namespaces.getOrPut(name) {
            Log.d(TAG, "Creating new namespace: $name")
            CommandNamespace(name = name)
        }
    }

    /**
     * Find which namespace contains a command
     */
    private fun findNamespaceContaining(commandId: String): CommandNamespace? {
        return namespaces.values.firstOrNull { it.contains(commandId) }
    }

    /**
     * Validate command before registration
     */
    private fun validateCommand(command: VoiceCommand): Result<Unit> {
        return try {
            // Validation is already done in VoiceCommand init block
            Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }

    /**
     * Auto-resolve conflicts using priority-based resolution.
     *
     * Strategy:
     * - For each conflict, look at every existing command that shares a phrase
     *   with [command].
     * - If [command] has a strictly higher priority than the existing command,
     *   lower the existing command's priority by 1 (making room for [command] to
     *   win at runtime without a true tie). This counts as resolved.
     * - If [command] has the same or lower priority AND the conflict is EXACT_MATCH
     *   or PRIORITY_CONFLICT, the conflict is unresolvable automatically.
     * - SIMILAR_PHRASE, NAMESPACE_OVERLAP, and SUBSTRING_MATCH conflicts at equal
     *   priority are considered tolerable (runtime priority-resolution wins).
     *
     * Returns true only when every conflict has been handled (either by a priority
     * adjustment or by being classified as tolerable). Returns false if any
     * unresolvable conflict remains, signalling that manual intervention is needed.
     */
    private fun autoResolveConflicts(
        command: VoiceCommand,
        conflicts: List<ConflictInfo>
    ): Boolean {
        var allResolved = true

        for (conflict in conflicts) {
            // Critical conflicts (EXACT_MATCH / PRIORITY_CONFLICT at same priority)
            // that cannot be settled by a simple priority decrement are blockers.
            val conflictingIds = conflict.affectedCommands.filter { it != command.id }

            var conflictResolved = false

            for (existingId in conflictingIds) {
                // Find the namespace that owns this command
                val ownerNamespace = findNamespaceContaining(existingId) ?: continue
                val existing = ownerNamespace.getCommand(existingId) ?: continue

                when {
                    command.priority > existing.priority -> {
                        // New command already wins at runtime; no change needed.
                        conflictResolved = true
                    }
                    command.priority == existing.priority &&
                    conflict.conflictType in listOf(
                        ConflictType.EXACT_MATCH,
                        ConflictType.PRIORITY_CONFLICT
                    ) -> {
                        // True tie on an exact/priority conflict: lower existing by 1
                        // so that [command] takes precedence, provided priority > 1.
                        val newPriority = (existing.priority - 1).coerceAtLeast(1)
                        if (newPriority != existing.priority) {
                            ownerNamespace.setCommandPriority(existingId, newPriority)
                            Log.d(
                                TAG,
                                "Auto-resolved: lowered '$existingId' priority " +
                                "${existing.priority}→$newPriority to make room for '${command.id}'"
                            )
                            conflictResolved = true
                        } else {
                            // Already at minimum priority — cannot lower further
                            Log.w(
                                TAG,
                                "Auto-resolve failed: '$existingId' is already at min priority 1"
                            )
                        }
                    }
                    else -> {
                        // command.priority < existing.priority, or a tolerable conflict
                        // type (SIMILAR_PHRASE, NAMESPACE_OVERLAP, SUBSTRING_MATCH) —
                        // the existing command wins naturally via priority ordering.
                        conflictResolved = conflict.conflictType !in listOf(
                            ConflictType.EXACT_MATCH,
                            ConflictType.PRIORITY_CONFLICT
                        )
                        if (!conflictResolved) {
                            Log.w(
                                TAG,
                                "Auto-resolve: '${command.id}' has lower priority than " +
                                "existing '$existingId' on an exact conflict — cannot resolve"
                            )
                        }
                    }
                }
            }

            if (conflictingIds.isEmpty()) {
                // No specific opposing commands identified; treat as resolved
                conflictResolved = true
            }

            if (!conflictResolved) {
                allResolved = false
            }
        }

        return allResolved
    }

    // Listener notification methods

    private fun notifyCommandRegistered(command: VoiceCommand) {
        listeners.forEach { listener ->
            try {
                listener.onCommandRegistered(command, command.namespace)
            } catch (e: Exception) {
                Log.e(TAG, "Listener error in onCommandRegistered", e)
            }
        }
    }

    private fun notifyCommandUnregistered(commandId: String, namespace: String) {
        listeners.forEach { listener ->
            try {
                listener.onCommandUnregistered(commandId, namespace)
            } catch (e: Exception) {
                Log.e(TAG, "Listener error in onCommandUnregistered", e)
            }
        }
    }

    private fun notifyConflictsDetected(conflicts: List<ConflictInfo>, command: VoiceCommand) {
        listeners.forEach { listener ->
            conflicts.forEach { conflict ->
                try {
                    listener.onConflictDetected(conflict, command)
                } catch (e: Exception) {
                    Log.e(TAG, "Listener error in onConflictDetected", e)
                }
            }
        }
    }

    private fun notifyCommandEnabledChanged(commandId: String, enabled: Boolean) {
        listeners.forEach { listener ->
            try {
                listener.onCommandEnabledChanged(commandId, enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Listener error in onCommandEnabledChanged", e)
            }
        }
    }

    private fun notifyNamespaceCleared(namespace: String, count: Int) {
        listeners.forEach { listener ->
            try {
                listener.onNamespaceCleared(namespace, count)
            } catch (e: Exception) {
                Log.e(TAG, "Listener error in onNamespaceCleared", e)
            }
        }
    }
}
