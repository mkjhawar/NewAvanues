/**
 * CommandManager - Core Interfaces
 *
 * Defines the contract for command management operations.
 * Implementations are provided by VoiceOSCore or other modules.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 */
package com.augmentalis.commandmanager

import kotlinx.coroutines.flow.Flow

/**
 * Core interface for command registry operations.
 *
 * Manages the collection of registered commands for voice lookup.
 */
interface ICommandRegistry {

    /**
     * Register a command definition.
     *
     * @param definition The command definition to register
     */
    fun register(definition: CommandDefinition)

    /**
     * Unregister a command by ID.
     *
     * @param commandId The command ID to unregister
     * @return true if command was found and removed
     */
    fun unregister(commandId: String): Boolean

    /**
     * Find command by exact phrase match.
     *
     * @param phrase The voice input phrase
     * @return Matching command definition or null
     */
    fun findByPhrase(phrase: String): CommandDefinition?

    /**
     * Find commands matching a pattern.
     *
     * @param pattern Pattern to match (supports wildcards)
     * @return List of matching command definitions
     */
    fun findByPattern(pattern: String): List<CommandDefinition>

    /**
     * Get all registered commands.
     *
     * @return List of all command definitions
     */
    fun getAll(): List<CommandDefinition>

    /**
     * Get commands by category.
     *
     * @param category The category to filter by
     * @return List of commands in that category
     */
    fun getByCategory(category: CommandCategory): List<CommandDefinition>

    /**
     * Clear all registered commands.
     */
    fun clear()

    /**
     * Number of registered commands.
     */
    val size: Int
}

/**
 * Interface for command execution.
 *
 * Handles the execution of recognized voice commands.
 */
interface ICommandExecutor {

    /**
     * Execute a command.
     *
     * @param command The command to execute
     * @return Result of the execution
     */
    suspend fun execute(command: Command): CommandResult

    /**
     * Execute a command by phrase.
     *
     * @param phrase The voice phrase to execute
     * @param context Optional execution context
     * @return Result of the execution
     */
    suspend fun executePhrase(phrase: String, context: CommandContext? = null): CommandResult

    /**
     * Check if a command can be executed in the current context.
     *
     * @param command The command to check
     * @return true if command can be executed
     */
    fun canExecute(command: Command): Boolean
}

/**
 * Interface for command matching/recognition.
 *
 * Handles fuzzy matching and synonym resolution.
 */
interface ICommandMatcher {

    /**
     * Match a voice input to a command.
     *
     * @param input The raw voice input
     * @param context Optional context for context-aware matching
     * @return Best matching command or null
     */
    fun match(input: String, context: CommandContext? = null): MatchResult?

    /**
     * Get multiple match candidates with confidence scores.
     *
     * @param input The raw voice input
     * @param maxResults Maximum number of results
     * @return List of match candidates
     */
    fun matchAll(input: String, maxResults: Int = 5): List<MatchResult>
}

/**
 * Result of a command match operation.
 *
 * @property definition The matched command definition
 * @property confidence Confidence score (0.0-1.0)
 * @property matchedPattern The pattern that matched
 * @property extractedParams Parameters extracted from the input
 */
data class MatchResult(
    val definition: CommandDefinition,
    val confidence: Float,
    val matchedPattern: String,
    val extractedParams: Map<String, String> = emptyMap()
)

/**
 * Interface for command event observation.
 *
 * Allows listening to command lifecycle events.
 */
interface ICommandObserver {

    /**
     * Flow of command events.
     */
    val events: Flow<CommandEvent>

    /**
     * Add a listener for command events.
     *
     * @param listener The listener callback
     */
    fun addListener(listener: CommandEventListener)

    /**
     * Remove a listener.
     *
     * @param listener The listener to remove
     */
    fun removeListener(listener: CommandEventListener)
}

/**
 * Callback interface for command events.
 */
interface CommandEventListener {
    fun onCommandReceived(command: Command)
    fun onCommandExecuted(command: Command, result: CommandResult)
    fun onCommandFailed(command: Command, error: ErrorCode)
}

// CommandEvent and EventType are defined in CommandModels.kt

/**
 * Unified CommandManager facade interface.
 *
 * Combines registry, executor, matcher, and observer interfaces.
 */
interface ICommandManager : ICommandRegistry, ICommandExecutor, ICommandMatcher, ICommandObserver
