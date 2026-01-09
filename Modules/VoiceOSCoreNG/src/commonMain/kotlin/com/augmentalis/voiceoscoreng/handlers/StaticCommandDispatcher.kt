/**
 * StaticCommandDispatcher.kt - Handles static command matching and execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Single Responsibility: Matches and executes static (system-wide) commands.
 * Static commands are predefined phrases that work regardless of screen context.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.StaticCommand
import com.augmentalis.voiceoscoreng.common.StaticCommandRegistry

/**
 * Dispatcher for static (system-wide) commands.
 *
 * Responsibilities:
 * - Match voice input against predefined static commands
 * - Execute matched static commands via the action executor
 *
 * Does NOT handle:
 * - Dynamic/screen-specific commands (see DynamicCommandDispatcher)
 * - Mode management (see CommandDispatcher)
 * - Event emission (see CommandDispatcher)
 */
class StaticCommandDispatcher(
    private val executor: IActionExecutor
) {
    /**
     * Match a voice input to a static command.
     *
     * @param voiceInput Raw voice input string
     * @return Matched StaticCommand or null if no match
     */
    fun match(voiceInput: String): StaticCommand? {
        return StaticCommandRegistry.findByPhrase(voiceInput)
    }

    /**
     * Execute a static command.
     *
     * @param command Static command to execute
     * @return ActionResult from execution
     */
    suspend fun execute(command: StaticCommand): ActionResult {
        return executor.executeAction(command.actionType, command.metadata)
    }

    /**
     * Try to match and execute a static command.
     *
     * Combines match and execute for convenience when caller
     * doesn't need to inspect the matched command.
     *
     * @param voiceInput Raw voice input string
     * @return ActionResult if matched, null if no match
     */
    suspend fun dispatch(voiceInput: String): ActionResult? {
        val command = match(voiceInput) ?: return null
        return execute(command)
    }
}
