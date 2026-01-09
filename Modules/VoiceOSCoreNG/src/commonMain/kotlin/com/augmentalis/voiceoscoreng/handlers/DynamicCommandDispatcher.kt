/**
 * DynamicCommandDispatcher.kt - Handles dynamic command matching and execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Single Responsibility: Matches and executes dynamic (screen-specific) commands.
 * Dynamic commands are generated from UI scraping and change per screen.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.CommandMatcher
import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Interface for dynamic command dispatching.
 *
 * Enables dependency injection and testing by abstracting
 * the dynamic command dispatch behavior.
 */
interface IDynamicCommandDispatcher {
    /**
     * Current number of dynamic commands in registry.
     */
    val commandCount: Int

    /**
     * Update the dynamic command registry.
     *
     * @param commands New list of quantized commands from screen
     */
    suspend fun updateCommands(commands: List<QuantizedCommand>)

    /**
     * Match a voice input to dynamic commands.
     *
     * @param voiceInput Raw voice input string
     * @param threshold Minimum similarity score (0.0 - 1.0)
     * @return DynamicMatchResult indicating match outcome
     */
    fun match(voiceInput: String, threshold: Float): DynamicMatchResult

    /**
     * Execute a quantized command.
     *
     * @param command Quantized command to execute
     * @return ActionResult from execution
     */
    suspend fun execute(command: QuantizedCommand): ActionResult
}

/**
 * Result of dynamic command matching.
 *
 * Provides rich information about the match outcome:
 * - Matched: Single command matched with confidence
 * - Ambiguous: Multiple commands matched with similar scores
 * - NoMatch: No command matched the threshold
 */
sealed class DynamicMatchResult {
    /**
     * Single command matched successfully.
     *
     * @param command The matched quantized command
     * @param confidence Match confidence (0.0 - 1.0), 1.0 for exact matches
     * @param isExact True if this was an exact phrase match
     */
    data class Matched(
        val command: QuantizedCommand,
        val confidence: Float,
        val isExact: Boolean
    ) : DynamicMatchResult()

    /**
     * Multiple commands matched with similar scores.
     *
     * Requires user disambiguation before execution.
     *
     * @param candidates List of ambiguous command matches
     */
    data class Ambiguous(val candidates: List<QuantizedCommand>) : DynamicMatchResult()

    /**
     * No command matched above the threshold.
     */
    data object NoMatch : DynamicMatchResult()
}

/**
 * Dispatcher for dynamic (screen-specific) commands.
 *
 * Responsibilities:
 * - Maintain registry of current screen commands
 * - Match voice input against dynamic commands with fuzzy matching
 * - Execute matched dynamic commands via the action executor
 *
 * Does NOT handle:
 * - Static/system-wide commands (see StaticCommandDispatcher)
 * - Mode management (see CommandDispatcher)
 * - Event emission (see CommandDispatcher)
 */
class DynamicCommandDispatcher(
    private val executor: IActionExecutor,
    private val registry: CommandRegistry = CommandRegistry()
) : IDynamicCommandDispatcher {
    /**
     * Current number of dynamic commands in registry.
     */
    override val commandCount: Int get() = registry.size

    /**
     * Update the dynamic command registry.
     *
     * Called after screen scraping to replace all commands
     * with the current screen's available actions.
     *
     * @param commands New list of quantized commands from screen
     */
    override suspend fun updateCommands(commands: List<QuantizedCommand>) {
        registry.update(commands)
    }

    /**
     * Match a voice input to dynamic commands.
     *
     * Uses fuzzy matching with Jaccard similarity to handle
     * slight variations in voice input.
     *
     * @param voiceInput Raw voice input string
     * @param threshold Minimum similarity score (0.0 - 1.0)
     * @return DynamicMatchResult indicating match outcome
     */
    override fun match(voiceInput: String, threshold: Float): DynamicMatchResult {
        val result = CommandMatcher.match(
            voiceInput = voiceInput,
            registry = registry,
            threshold = threshold
        )

        return when (result) {
            is CommandMatcher.MatchResult.Exact ->
                DynamicMatchResult.Matched(result.command, 1.0f, true)
            is CommandMatcher.MatchResult.Fuzzy ->
                DynamicMatchResult.Matched(result.command, result.confidence, false)
            is CommandMatcher.MatchResult.Ambiguous ->
                DynamicMatchResult.Ambiguous(result.candidates)
            is CommandMatcher.MatchResult.NoMatch ->
                DynamicMatchResult.NoMatch
        }
    }

    /**
     * Execute a quantized command.
     *
     * @param command Quantized command to execute
     * @return ActionResult from execution
     */
    override suspend fun execute(command: QuantizedCommand): ActionResult {
        return executor.executeCommand(command)
    }
}
