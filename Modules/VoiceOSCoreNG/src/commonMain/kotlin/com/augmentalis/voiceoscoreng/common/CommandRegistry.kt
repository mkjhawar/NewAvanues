package com.augmentalis.voiceoscoreng.common

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Command Registry - In-memory storage for active screen commands.
 *
 * Stores commands for the current screen. Replaced on each scan - no accumulation.
 * Designed for voice command lookup during execution.
 *
 * Thread-safe: All operations are synchronized for concurrent access from
 * accessibility service callbacks and voice recognition threads.
 */
class CommandRegistry {
    private val commands = mutableMapOf<String, QuantizedCommand>() // keyed by VUID
    private val lock = Any() // Synchronization lock for thread safety

    /**
     * Update registry with new commands, replacing all existing.
     * Commands with null targetVuid are ignored (cannot be indexed).
     *
     * Thread-safe: Synchronized to prevent concurrent modification.
     *
     * @param newCommands List of commands from current scan
     */
    fun update(newCommands: List<QuantizedCommand>) {
        synchronized(lock) {
            commands.clear()
            newCommands.forEach { cmd ->
                cmd.targetVuid?.let { vuid ->
                    commands[vuid] = cmd
                }
            }
        }
    }

    /**
     * Find command by voice phrase.
     * Supports exact match and partial label match (case insensitive).
     *
     * Thread-safe: Takes a snapshot of values for iteration.
     *
     * @param phrase Voice input phrase
     * @return Matching QuantizedCommand or null
     */
    fun findByPhrase(phrase: String): QuantizedCommand? {
        val normalized = phrase.lowercase().trim()

        // Take a snapshot for thread-safe iteration
        val snapshot = synchronized(lock) { commands.values.toList() }

        // Exact match first
        snapshot.firstOrNull { cmd ->
            cmd.phrase.lowercase() == normalized
        }?.let { return it }

        // Partial match - check if input matches just the label part
        return snapshot.firstOrNull { cmd ->
            val label = cmd.phrase.substringAfter(" ").lowercase()
            normalized == label || normalized.endsWith(label)
        }
    }

    /**
     * Find command by target VUID.
     *
     * Thread-safe: Synchronized access.
     *
     * @param vuid Target element VUID
     * @return QuantizedCommand or null
     */
    fun findByVuid(vuid: String): QuantizedCommand? = synchronized(lock) {
        commands[vuid]
    }

    /**
     * Get all commands in registry.
     *
     * Thread-safe: Returns a defensive copy.
     *
     * @return List of all commands
     */
    fun all(): List<QuantizedCommand> = synchronized(lock) {
        commands.values.toList()
    }

    /**
     * Clear all commands.
     *
     * Thread-safe: Synchronized access.
     */
    fun clear() = synchronized(lock) {
        commands.clear()
    }

    /**
     * Current command count.
     *
     * Thread-safe: Synchronized access.
     */
    val size: Int get() = synchronized(lock) { commands.size }
}
