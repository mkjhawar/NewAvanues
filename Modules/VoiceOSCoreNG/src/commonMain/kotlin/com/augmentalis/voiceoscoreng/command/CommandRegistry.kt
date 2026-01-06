package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.avu.QuantizedCommand

/**
 * Command Registry - In-memory storage for active screen commands.
 *
 * Stores commands for the current screen. Replaced on each scan - no accumulation.
 * Designed for voice command lookup during execution.
 */
class CommandRegistry {
    private val commands = mutableMapOf<String, QuantizedCommand>() // keyed by VUID

    /**
     * Update registry with new commands, replacing all existing.
     * Commands with null targetVuid are ignored (cannot be indexed).
     *
     * @param newCommands List of commands from current scan
     */
    fun update(newCommands: List<QuantizedCommand>) {
        commands.clear()
        newCommands.forEach { cmd ->
            cmd.targetVuid?.let { vuid ->
                commands[vuid] = cmd
            }
        }
    }

    /**
     * Find command by voice phrase.
     * Supports exact match and partial label match (case insensitive).
     *
     * @param phrase Voice input phrase
     * @return Matching QuantizedCommand or null
     */
    fun findByPhrase(phrase: String): QuantizedCommand? {
        val normalized = phrase.lowercase().trim()

        // Exact match first
        commands.values.firstOrNull { cmd ->
            cmd.phrase.lowercase() == normalized
        }?.let { return it }

        // Partial match - check if input matches just the label part
        return commands.values.firstOrNull { cmd ->
            val label = cmd.phrase.substringAfter(" ").lowercase()
            normalized == label || normalized.endsWith(label)
        }
    }

    /**
     * Find command by target VUID.
     *
     * @param vuid Target element VUID
     * @return QuantizedCommand or null
     */
    fun findByVuid(vuid: String): QuantizedCommand? = commands[vuid]

    /**
     * Get all commands in registry.
     *
     * @return List of all commands
     */
    fun all(): List<QuantizedCommand> = commands.values.toList()

    /**
     * Clear all commands.
     */
    fun clear() = commands.clear()

    /**
     * Current command count.
     */
    val size: Int get() = commands.size
}
