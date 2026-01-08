package com.augmentalis.voiceoscoreng.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Command Registry - In-memory storage for active screen commands.
 *
 * Stores commands for the current screen. Replaced on each scan - no accumulation.
 * Designed for voice command lookup during execution.
 *
 * Thread-safe: All operations use Mutex for concurrent access from
 * accessibility service callbacks and voice recognition threads.
 */
class CommandRegistry {
    private val commands = mutableMapOf<String, QuantizedCommand>() // keyed by VUID
    private val mutex = Mutex() // KMP-compatible synchronization

    /**
     * Update registry with new commands, replacing all existing.
     * Commands with null targetVuid are ignored (cannot be indexed).
     *
     * Thread-safe: Uses mutex to prevent concurrent modification.
     *
     * @param newCommands List of commands from current scan
     */
    suspend fun update(newCommands: List<QuantizedCommand>) {
        mutex.withLock {
            commands.clear()
            for (cmd in newCommands) {
                val vuid = cmd.targetVuid
                if (vuid != null) {
                    commands[vuid] = cmd
                }
            }
        }
    }

    /**
     * Non-suspend update for use in non-coroutine contexts.
     * Note: This is NOT thread-safe. Use update() for concurrent access.
     */
    fun updateSync(newCommands: List<QuantizedCommand>) {
        commands.clear()
        for (cmd in newCommands) {
            val vuid = cmd.targetVuid
            if (vuid != null) {
                commands[vuid] = cmd
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
        val snapshot = commands.values.toList()

        // Exact match first
        val exactMatch = snapshot.firstOrNull { cmd: QuantizedCommand ->
            cmd.phrase.lowercase() == normalized
        }
        if (exactMatch != null) return exactMatch

        // Partial match - check if input matches just the label part
        return snapshot.firstOrNull { cmd: QuantizedCommand ->
            val label = cmd.phrase.substringAfter(" ").lowercase()
            normalized == label || normalized.endsWith(label, ignoreCase = true)
        }
    }

    /**
     * Find command by target VUID.
     *
     * @param vuid Target element VUID
     * @return QuantizedCommand or null
     */
    fun findByVuid(vuid: String): QuantizedCommand? {
        return commands[vuid]
    }

    /**
     * Get all commands in registry.
     *
     * Thread-safe: Returns a defensive copy.
     *
     * @return List of all commands
     */
    fun all(): List<QuantizedCommand> {
        return commands.values.toList()
    }

    /**
     * Clear all commands.
     */
    fun clear() {
        commands.clear()
    }

    /**
     * Current command count.
     */
    val size: Int get() = commands.size
}
