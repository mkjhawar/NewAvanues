/**
 * CommandRegistry.kt - Custom command registration and management
 * Manage custom command registration and lifecycle
 */

package com.augmentalis.commandmanager.registry

import com.augmentalis.voiceos.command.*
import com.augmentalis.commandmanager.definitions.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Command registry for managing custom commands
 * Handles registration, unregistration, and lifecycle of custom commands
 */
class CommandRegistry {
    
    companion object {
        private const val TAG = "CommandRegistry"
    }
    
    // Custom command storage
    private val customCommands = ConcurrentHashMap<String, RegisteredCommand>()
    
    // Command events flow
    private val _commandEvents = MutableSharedFlow<CommandRegistryEvent>()
    val commandEvents: Flow<CommandRegistryEvent> = _commandEvents.asSharedFlow()
    
    // Registration validation rules
    private val validationRules = mutableListOf<CommandValidationRule>()
    
    /**
     * Register a custom command
     */
    fun registerCommand(
        definition: CommandDefinition,
        handler: CommandHandler,
        metadata: CommandMetadata = CommandMetadata()
    ): CommandRegistrationResult {
        return try {
            // Validate command definition
            val validationResult = validateCommand(definition)
            if (!validationResult.isValid) {
                return CommandRegistrationResult.Failure(
                    definition.id,
                    "Validation failed: ${validationResult.errors.joinToString(", ")}"
                )
            }
            
            // Check for conflicts
            if (customCommands.containsKey(definition.id)) {
                return CommandRegistrationResult.Failure(
                    definition.id,
                    "Command with ID '${definition.id}' already exists"
                )
            }
            
            // Create registered command
            val registeredCommand = RegisteredCommand(
                definition = definition,
                handler = handler,
                metadata = metadata,
                registrationTime = System.currentTimeMillis(),
                lastUsed = 0L,
                usageCount = 0L
            )
            
            // Register the command
            customCommands[definition.id] = registeredCommand
            
            // Emit registration event
            _commandEvents.tryEmit(
                CommandRegistryEvent.CommandRegistered(definition.id, definition.category)
            )
            
            android.util.Log.i(TAG, "Registered custom command: ${definition.id}")
            
            CommandRegistrationResult.Success(definition.id)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to register command: ${definition.id}", e)
            CommandRegistrationResult.Failure(definition.id, "Registration error: ${e.message}")
        }
    }
    
    /**
     * Unregister a custom command
     */
    fun unregisterCommand(commandId: String): CommandUnregistrationResult {
        return try {
            val removedCommand = customCommands.remove(commandId)
            
            if (removedCommand != null) {
                // Emit unregistration event
                _commandEvents.tryEmit(
                    CommandRegistryEvent.CommandUnregistered(commandId, removedCommand.definition.category)
                )
                
                android.util.Log.i(TAG, "Unregistered custom command: $commandId")
                CommandUnregistrationResult.Success(commandId)
            } else {
                CommandUnregistrationResult.Failure(commandId, "Command not found")
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to unregister command: $commandId", e)
            CommandUnregistrationResult.Failure(commandId, "Unregistration error: ${e.message}")
        }
    }
    
    /**
     * Get a registered command
     */
    fun getCommand(commandId: String): RegisteredCommand? {
        return customCommands[commandId]
    }
    
    /**
     * Get all registered commands
     */
    fun getAllCommands(): List<RegisteredCommand> {
        return customCommands.values.toList()
    }
    
    /**
     * Get commands by category
     */
    fun getCommandsByCategory(category: String): List<RegisteredCommand> {
        return customCommands.values.filter { it.definition.category == category }
    }
    
    /**
     * Get commands by metadata tag
     */
    fun getCommandsByTag(tag: String): List<RegisteredCommand> {
        return customCommands.values.filter { it.metadata.tags.contains(tag) }
    }
    
    /**
     * Get commands by owner
     */
    fun getCommandsByOwner(owner: String): List<RegisteredCommand> {
        return customCommands.values.filter { it.metadata.owner == owner }
    }
    
    /**
     * Update command metadata
     */
    fun updateCommandMetadata(commandId: String, metadata: CommandMetadata): Boolean {
        val command = customCommands[commandId]
        return if (command != null) {
            customCommands[commandId] = command.copy(metadata = metadata)
            android.util.Log.d(TAG, "Updated metadata for command: $commandId")
            true
        } else {
            false
        }
    }
    
    /**
     * Record command usage
     */
    fun recordUsage(commandId: String) {
        val command = customCommands[commandId]
        if (command != null) {
            customCommands[commandId] = command.copy(
                lastUsed = System.currentTimeMillis(),
                usageCount = command.usageCount + 1
            )
        }
    }
    
    /**
     * Get command usage statistics
     */
    fun getUsageStatistics(): CommandRegistryStatistics {
        val commands = customCommands.values
        val totalCommands = commands.size
        val categoryCounts = commands.groupBy { it.definition.category }
            .mapValues { it.value.size }
        val ownerCounts = commands.groupBy { it.metadata.owner }
            .mapValues { it.value.size }
        val totalUsage = commands.sumOf { it.usageCount }
        val averageUsage = if (totalCommands > 0) totalUsage.toFloat() / totalCommands else 0f
        
        val currentTime = System.currentTimeMillis()
        val last24Hours = currentTime - (24 * 60 * 60 * 1000L)
        val recentlyUsedCount = commands.count { it.lastUsed >= last24Hours }
        
        return CommandRegistryStatistics(
            totalCommands = totalCommands,
            categoryCounts = categoryCounts,
            ownerCounts = ownerCounts,
            totalUsage = totalUsage,
            averageUsage = averageUsage,
            recentlyUsedCount = recentlyUsedCount,
            oldestRegistration = commands.minOfOrNull { it.registrationTime } ?: 0L,
            newestRegistration = commands.maxOfOrNull { it.registrationTime } ?: 0L
        )
    }
    
    /**
     * Clear all custom commands
     */
    fun clearAllCommands() {
        val count = customCommands.size
        customCommands.clear()
        
        _commandEvents.tryEmit(CommandRegistryEvent.AllCommandsCleared(count))
        android.util.Log.i(TAG, "Cleared all custom commands ($count total)")
    }
    
    /**
     * Export commands
     */
    fun exportCommands(): CommandRegistryExport {
        return CommandRegistryExport(
            exportTimestamp = System.currentTimeMillis(),
            commands = customCommands.values.toList(),
            statistics = getUsageStatistics()
        )
    }
    
    /**
     * Import commands
     */
    fun importCommands(
        export: CommandRegistryExport,
        overwriteExisting: Boolean = false
    ): CommandImportResult {
        var successCount = 0
        var failureCount = 0
        val failures = mutableListOf<String>()
        
        for (command in export.commands) {
            if (!overwriteExisting && customCommands.containsKey(command.definition.id)) {
                failureCount++
                failures.add("Command ${command.definition.id} already exists")
                continue
            }
            
            try {
                customCommands[command.definition.id] = command
                successCount++
            } catch (e: Exception) {
                failureCount++
                failures.add("Failed to import ${command.definition.id}: ${e.message}")
            }
        }
        
        android.util.Log.i(TAG, "Imported commands: $successCount success, $failureCount failures")
        
        return CommandImportResult(
            successCount = successCount,
            failureCount = failureCount,
            failures = failures
        )
    }
    
    /**
     * Add validation rule
     */
    fun addValidationRule(rule: CommandValidationRule) {
        validationRules.add(rule)
        android.util.Log.d(TAG, "Added validation rule: ${rule.name}")
    }
    
    /**
     * Remove validation rule
     */
    fun removeValidationRule(ruleName: String) {
        validationRules.removeAll { it.name == ruleName }
        android.util.Log.d(TAG, "Removed validation rule: $ruleName")
    }
    
    /**
     * Search commands
     */
    fun searchCommands(query: String): List<RegisteredCommand> {
        val normalizedQuery = query.lowercase()
        return customCommands.values.filter { command ->
            command.definition.id.lowercase().contains(normalizedQuery) ||
            command.definition.patterns.any { 
                it.lowercase().contains(normalizedQuery) 
            } ||
            command.definition.description.lowercase().contains(normalizedQuery) ||
            command.metadata.tags.any { 
                it.lowercase().contains(normalizedQuery) 
            }
        }
    }
    
    // Private methods
    
    /**
     * Validate command definition
     */
    private fun validateCommand(definition: CommandDefinition): CommandValidationResult {
        val errors = mutableListOf<String>()
        
        // Basic validation
        if (definition.id.isBlank()) {
            errors.add("Command ID cannot be blank")
        }
        
        if (definition.patterns.isEmpty()) {
            errors.add("Command must have at least one pattern")
        }
        
        if (definition.patterns.any { it.isBlank() }) {
            errors.add("Command patterns cannot be blank")
        }
        
        // ID format validation
        if (!definition.id.matches(Regex("^[a-z0-9_]+$"))) {
            errors.add("Command ID must contain only lowercase letters, numbers, and underscores")
        }
        
        // Custom validation rules
        for (rule in validationRules) {
            val ruleResult = rule.validate(definition)
            if (!ruleResult.isValid) {
                errors.addAll(ruleResult.errors)
            }
        }
        
        return CommandValidationResult(errors.isEmpty(), errors)
    }
}

/**
 * Registered command data
 */
data class RegisteredCommand(
    val definition: CommandDefinition,
    val handler: CommandHandler,
    val metadata: CommandMetadata,
    val registrationTime: Long,
    val lastUsed: Long,
    val usageCount: Long
)

/**
 * Command metadata
 */
data class CommandMetadata(
    val owner: String = "unknown",
    val version: String = "1.0.0",
    val description: String = "",
    val tags: Set<String> = emptySet(),
    val priority: Int = 0,
    val customData: Map<String, Any> = emptyMap()
)

/**
 * Registry events
 */
sealed class CommandRegistryEvent {
    data class CommandRegistered(val commandId: String, val category: String) : CommandRegistryEvent()
    data class CommandUnregistered(val commandId: String, val category: String) : CommandRegistryEvent()
    data class AllCommandsCleared(val count: Int) : CommandRegistryEvent()
}

/**
 * Registration results
 */
sealed class CommandRegistrationResult {
    data class Success(val commandId: String) : CommandRegistrationResult()
    data class Failure(val commandId: String, val reason: String) : CommandRegistrationResult()
}

/**
 * Unregistration results
 */
sealed class CommandUnregistrationResult {
    data class Success(val commandId: String) : CommandUnregistrationResult()
    data class Failure(val commandId: String, val reason: String) : CommandUnregistrationResult()
}

/**
 * Registry statistics
 */
data class CommandRegistryStatistics(
    val totalCommands: Int,
    val categoryCounts: Map<String, Int>,
    val ownerCounts: Map<String, Int>,
    val totalUsage: Long,
    val averageUsage: Float,
    val recentlyUsedCount: Int,
    val oldestRegistration: Long,
    val newestRegistration: Long
)

/**
 * Registry export
 */
data class CommandRegistryExport(
    val exportTimestamp: Long,
    val commands: List<RegisteredCommand>,
    val statistics: CommandRegistryStatistics
)

/**
 * Import result
 */
data class CommandImportResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<String>
)

/**
 * Validation rule interface
 */
interface CommandValidationRule {
    val name: String
    fun validate(definition: CommandDefinition): CommandValidationResult
}

/**
 * Validation result
 */
data class CommandValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)