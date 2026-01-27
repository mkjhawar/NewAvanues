/**
 * DynamicCommandRegistry.kt - Dynamic command registration with priority and conflict detection
 * Extends CommandRegistry with runtime management features
 *
 * Features:
 * - Priority-based command resolution (1-100, higher = higher priority)
 * - Command conflict detection (multiple commands matching same phrase)
 * - Namespace management for module isolation
 * - Real-time command matching and resolution
 */

package com.augmentalis.commandmanager.registry

import com.augmentalis.voiceoscore.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import android.util.Log

/**
 * Enhanced command registry with dynamic features
 */
class DynamicCommandRegistry {

    companion object {
        private const val TAG = "DynamicCommandRegistry"
        private const val MIN_PRIORITY = 1
        private const val MAX_PRIORITY = 100
        private const val DEFAULT_PRIORITY = 50
    }

    // Core registry for base functionality
    private val baseRegistry = CommandRegistry()

    // Voice command storage with priority
    private val voiceCommands = ConcurrentHashMap<String, VoiceCommand>()

    // Namespace to command ID mapping
    private val namespaceMap = ConcurrentHashMap<String, MutableSet<String>>()

    // Phrase to command ID mapping (for fast lookup)
    private val phraseIndex = ConcurrentHashMap<String, MutableSet<String>>()

    // Command events flow
    private val _registryEvents = MutableSharedFlow<DynamicRegistryEvent>()
    val registryEvents: Flow<DynamicRegistryEvent> = _registryEvents.asSharedFlow()

    /**
     * Register a voice command with priority and namespace
     */
    fun registerCommand(command: VoiceCommand): Result<Unit> {
        return try {
            // Validate command
            val validation = validateVoiceCommand(command)
            if (!validation.isValid) {
                return Result.failure(IllegalArgumentException(validation.errors.joinToString(", ")))
            }

            // Check for existing command
            if (voiceCommands.containsKey(command.id)) {
                return Result.failure(IllegalStateException("Command with ID '${command.id}' already exists"))
            }

            // Detect conflicts
            val conflicts = detectConflicts(command)
            if (conflicts.isNotEmpty()) {
                Log.w(TAG, "Command '${command.id}' has conflicts: ${conflicts.size} conflicts detected")
                // Emit conflict event but still register (user can resolve later)
                _registryEvents.tryEmit(DynamicRegistryEvent.ConflictsDetected(command.id, conflicts))
            }

            // Register the command
            voiceCommands[command.id] = command

            // Update namespace map
            namespaceMap.getOrPut(command.namespace) { mutableSetOf() }.add(command.id)

            // Update phrase index
            for (phrase in command.phrases) {
                val normalizedPhrase = normalizePhrase(phrase)
                phraseIndex.getOrPut(normalizedPhrase) { mutableSetOf() }.add(command.id)
            }

            // Emit registration event
            _registryEvents.tryEmit(DynamicRegistryEvent.CommandRegistered(command))

            Log.i(TAG, "Registered command: ${command.id} (priority: ${command.priority}, namespace: ${command.namespace})")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to register command: ${command.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Unregister a command by ID
     */
    fun unregisterCommand(commandId: String): Result<Unit> {
        return try {
            val command = voiceCommands.remove(commandId)

            if (command != null) {
                // Remove from namespace map
                namespaceMap[command.namespace]?.remove(commandId)
                if (namespaceMap[command.namespace]?.isEmpty() == true) {
                    namespaceMap.remove(command.namespace)
                }

                // Remove from phrase index
                for (phrase in command.phrases) {
                    val normalizedPhrase = normalizePhrase(phrase)
                    phraseIndex[normalizedPhrase]?.remove(commandId)
                    if (phraseIndex[normalizedPhrase]?.isEmpty() == true) {
                        phraseIndex.remove(normalizedPhrase)
                    }
                }

                // Emit unregistration event
                _registryEvents.tryEmit(DynamicRegistryEvent.CommandUnregistered(commandId))

                Log.i(TAG, "Unregistered command: $commandId")
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Command not found: $commandId"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister command: $commandId", e)
            Result.failure(e)
        }
    }

    /**
     * Resolve a spoken phrase to matching commands (priority-sorted)
     */
    fun resolveCommand(phrase: String): List<VoiceCommand> {
        val normalizedPhrase = normalizePhrase(phrase)

        // Get all matching command IDs
        val matchingCommandIds = mutableSetOf<String>()

        // Exact match
        phraseIndex[normalizedPhrase]?.let { matchingCommandIds.addAll(it) }

        // Partial match (contains)
        for ((indexedPhrase, commandIds) in phraseIndex) {
            if (normalizedPhrase.contains(indexedPhrase) || indexedPhrase.contains(normalizedPhrase)) {
                matchingCommandIds.addAll(commandIds)
            }
        }

        // Get commands and sort by priority (descending)
        val matchingCommands = matchingCommandIds.mapNotNull { voiceCommands[it] }

        return matchingCommands.sortedByDescending { it.priority }
    }

    /**
     * Detect conflicts with existing commands
     */
    fun detectConflicts(command: VoiceCommand): List<ConflictInfo> {
        val conflicts = mutableListOf<ConflictInfo>()

        for (phrase in command.phrases) {
            val normalizedPhrase = normalizePhrase(phrase)

            // Check for exact phrase matches
            phraseIndex[normalizedPhrase]?.forEach { existingId ->
                val existingCommand = voiceCommands[existingId]
                if (existingCommand != null && existingCommand.id != command.id) {
                    conflicts.add(
                        ConflictInfo(
                            phrase = phrase,
                            conflictingCommandId = existingId,
                            conflictingCommandName = existingCommand.phrases.firstOrNull() ?: existingId,
                            conflictType = ConflictType.EXACT_MATCH,
                            priority = existingCommand.priority,
                            namespace = existingCommand.namespace
                        )
                    )
                }
            }

            // Check for similar phrases (fuzzy matching)
            for ((indexedPhrase, commandIds) in phraseIndex) {
                if (calculateSimilarity(normalizedPhrase, indexedPhrase) > 0.8f) {
                    commandIds.forEach { existingId ->
                        val existingCommand = voiceCommands[existingId]
                        if (existingCommand != null && existingCommand.id != command.id) {
                            // Avoid duplicates
                            if (conflicts.none { it.conflictingCommandId == existingId && it.phrase == phrase }) {
                                conflicts.add(
                                    ConflictInfo(
                                        phrase = phrase,
                                        conflictingCommandId = existingId,
                                        conflictingCommandName = existingCommand.phrases.firstOrNull() ?: existingId,
                                        conflictType = ConflictType.SIMILAR_PHRASE,
                                        priority = existingCommand.priority,
                                        namespace = existingCommand.namespace
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return conflicts
    }

    /**
     * Get command by ID
     */
    fun getCommand(commandId: String): VoiceCommand? {
        return voiceCommands[commandId]
    }

    /**
     * Get all commands in a namespace
     */
    fun getCommandsByNamespace(namespace: String): List<VoiceCommand> {
        val commandIds = namespaceMap[namespace] ?: return emptyList()
        return commandIds.mapNotNull { voiceCommands[it] }
    }

    /**
     * Get all registered commands
     */
    fun getAllCommands(): List<VoiceCommand> {
        return voiceCommands.values.toList()
    }

    /**
     * Update command priority
     */
    fun updatePriority(commandId: String, newPriority: Int): Result<Unit> {
        return try {
            val command = voiceCommands[commandId]
                ?: return Result.failure(NoSuchElementException("Command not found: $commandId"))

            val clampedPriority = newPriority.coerceIn(MIN_PRIORITY, MAX_PRIORITY)
            val updatedCommand = command.copy(priority = clampedPriority)

            voiceCommands[commandId] = updatedCommand

            _registryEvents.tryEmit(DynamicRegistryEvent.PriorityUpdated(commandId, clampedPriority))

            Log.i(TAG, "Updated priority for command $commandId: $clampedPriority")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update priority for command: $commandId", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all commands in a namespace
     */
    fun clearNamespace(namespace: String): Int {
        val commandIds = namespaceMap.remove(namespace) ?: return 0

        var count = 0
        commandIds.forEach { commandId ->
            if (unregisterCommand(commandId).isSuccess) {
                count++
            }
        }

        Log.i(TAG, "Cleared namespace '$namespace': $count commands removed")
        return count
    }

    /**
     * Get registry statistics
     */
    fun getStatistics(): DynamicRegistryStatistics {
        val commands = voiceCommands.values

        return DynamicRegistryStatistics(
            totalCommands = commands.size,
            namespaceCount = namespaceMap.size,
            totalPhrases = phraseIndex.size,
            averagePriority = commands.map { it.priority }.average().toFloat(),
            commandsByNamespace = namespaceMap.mapValues { it.value.size },
            priorityDistribution = commands.groupBy { it.priority / 10 * 10 }.mapValues { it.value.size }
        )
    }

    // Private helper methods

    /**
     * Validate voice command
     */
    private fun validateVoiceCommand(command: VoiceCommand): CommandValidationResult {
        val errors = mutableListOf<String>()

        if (command.id.isBlank()) {
            errors.add("Command ID cannot be blank")
        }

        if (command.phrases.isEmpty()) {
            errors.add("Command must have at least one phrase")
        }

        if (command.phrases.any { it.isBlank() }) {
            errors.add("Command phrases cannot be blank")
        }

        if (command.priority !in MIN_PRIORITY..MAX_PRIORITY) {
            errors.add("Priority must be between $MIN_PRIORITY and $MAX_PRIORITY")
        }

        if (command.namespace.isBlank()) {
            errors.add("Namespace cannot be blank")
        }

        return CommandValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Normalize phrase for matching (lowercase, trim, remove extra spaces)
     */
    private fun normalizePhrase(phrase: String): String {
        return phrase.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    /**
     * Calculate similarity between two phrases using Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1.0f

        val distance = levenshteinDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Calculate Levenshtein distance
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }
}

/**
 * Voice command data class
 */
data class VoiceCommand(
    val id: String,
    val phrases: List<String>,
    val priority: Int = 50,
    val namespace: String = "default",
    val actionType: ActionType = ActionType.CUSTOM_ACTION,
    val actionParams: Map<String, Any> = emptyMap(),
    val action: (suspend () -> Unit)? = null,
    val enabled: Boolean = true,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Action types for voice commands
 */
enum class ActionType {
    LAUNCH_APP,
    NAVIGATE,
    SYSTEM_COMMAND,
    CUSTOM_ACTION,
    TEXT_EDITING,
    MEDIA_CONTROL,
    ACCESSIBILITY
}

/**
 * Conflict information
 */
data class ConflictInfo(
    val phrase: String,
    val conflictingCommandId: String,
    val conflictingCommandName: String,
    val conflictType: ConflictType,
    val priority: Int,
    val namespace: String
)

/**
 * Conflict types
 */
enum class ConflictType {
    EXACT_MATCH,
    SIMILAR_PHRASE,
    SAME_NAMESPACE
}

/**
 * Dynamic registry events
 */
sealed class DynamicRegistryEvent {
    data class CommandRegistered(val command: VoiceCommand) : DynamicRegistryEvent()
    data class CommandUnregistered(val commandId: String) : DynamicRegistryEvent()
    data class ConflictsDetected(val commandId: String, val conflicts: List<ConflictInfo>) : DynamicRegistryEvent()
    data class PriorityUpdated(val commandId: String, val newPriority: Int) : DynamicRegistryEvent()
}

/**
 * Dynamic registry statistics
 */
data class DynamicRegistryStatistics(
    val totalCommands: Int,
    val namespaceCount: Int,
    val totalPhrases: Int,
    val averagePriority: Float,
    val commandsByNamespace: Map<String, Int>,
    val priorityDistribution: Map<Int, Int>
)
