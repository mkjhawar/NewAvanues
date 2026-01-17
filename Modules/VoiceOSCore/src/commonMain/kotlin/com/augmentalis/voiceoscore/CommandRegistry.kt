package com.augmentalis.voiceoscore

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Command Registry - In-memory storage for active screen commands.
 *
 * Stores commands for the current screen. Replaced on each scan - no accumulation.
 * Designed for voice command lookup during execution.
 *
 * Thread-safety strategy:
 * - Uses immutable snapshot pattern with @Volatile reference
 * - Writes are protected by mutex and replace the entire map atomically
 * - Reads access the volatile snapshot directly (no locking needed)
 * - This provides consistent reads without blocking and safe concurrent writes
 */
class CommandRegistry {
    /**
     * Immutable snapshot of commands, keyed by VUID.
     * Volatile ensures visibility of writes across threads.
     * All reads see a consistent snapshot; writes replace atomically.
     */
    @Volatile
    private var commandsSnapshot: Map<String, QuantizedCommand> = emptyMap()

    private val mutex = Mutex() // Protects concurrent writes

    /**
     * Update registry with new commands, replacing all existing.
     * Commands with null targetVuid are ignored (cannot be indexed).
     *
     * Thread-safe: Uses mutex to serialize writes.
     *
     * @param newCommands List of commands from current scan
     */
    suspend fun update(newCommands: List<QuantizedCommand>) {
        mutex.withLock {
            commandsSnapshot = newCommands
                .filter { it.targetVuid != null }
                .associateBy { it.targetVuid!! }
        }
    }

    /**
     * Synchronous update for use in non-coroutine contexts.
     *
     * Thread-safe: Volatile reference ensures atomic visibility of writes.
     * Prefer suspend update() when in a coroutine context.
     *
     * @param newCommands List of commands from current scan
     */
    fun updateSync(newCommands: List<QuantizedCommand>) {
        commandsSnapshot = newCommands
            .filter { it.targetVuid != null }
            .associateBy { it.targetVuid!! }
    }

    /**
     * Find command by voice phrase.
     * Supports exact match and partial label match (case insensitive).
     *
     * Thread-safe: Reads from immutable snapshot.
     *
     * @param phrase Voice input phrase
     * @return Matching QuantizedCommand or null
     */
    fun findByPhrase(phrase: String): QuantizedCommand? {
        val normalized = phrase.lowercase().trim()
        val snapshot = commandsSnapshot.values // Read volatile once

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
     * Thread-safe: Reads from immutable snapshot.
     *
     * @param vuid Target element VUID
     * @return QuantizedCommand or null
     */
    fun findByVuid(vuid: String): QuantizedCommand? {
        return commandsSnapshot[vuid]
    }

    /**
     * Get all commands in registry.
     *
     * Thread-safe: Returns values from immutable snapshot.
     *
     * @return List of all commands
     */
    fun all(): List<QuantizedCommand> {
        return commandsSnapshot.values.toList()
    }

    /**
     * Add commands to the registry without replacing existing ones.
     * Useful for adding index commands ("first", "second") alongside existing commands.
     *
     * Thread-safe: Volatile reference ensures atomic visibility.
     *
     * @param commands List of commands to add
     */
    fun addAll(commands: List<QuantizedCommand>) {
        if (commands.isEmpty()) return
        val toAdd = commands.filter { it.targetVuid != null }.associateBy { it.targetVuid!! }
        commandsSnapshot = commandsSnapshot + toAdd
    }

    /**
     * Clear all commands.
     *
     * Thread-safe: Volatile reference ensures atomic visibility.
     */
    fun clear() {
        commandsSnapshot = emptyMap()
    }

    /**
     * Current command count.
     *
     * Thread-safe: Reads from immutable snapshot.
     */
    val size: Int get() = commandsSnapshot.size
}
