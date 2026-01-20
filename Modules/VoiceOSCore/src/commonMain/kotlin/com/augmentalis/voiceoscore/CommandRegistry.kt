package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.LoggingUtils
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
 *
 * IMPORTANT: Commands are keyed by PHRASE (not targetVuid) to allow multiple
 * command types to coexist for the same element:
 * - Label commands: "Gmail", "Settings", "Arby's"
 * - Index commands (DynamicLists): "first", "second", "third"
 * - Numeric commands: "1", "2", "3"
 * All can target the same element but have different phrases.
 */
class CommandRegistry {
    /**
     * Immutable snapshot of commands, keyed by phrase (lowercase).
     * Volatile ensures visibility of writes across threads.
     * All reads see a consistent snapshot; writes replace atomically.
     *
     * Key: phrase.lowercase() - allows multiple commands per targetVuid
     * Value: QuantizedCommand
     */
    @Volatile
    private var commandsSnapshot: Map<String, QuantizedCommand> = emptyMap()

    private val mutex = Mutex() // Protects concurrent writes

    /**
     * Update registry with new commands, replacing all existing.
     * Commands with null targetVuid are ignored (cannot be executed).
     *
     * Thread-safe: Uses mutex to serialize writes.
     *
     * @param newCommands List of commands from current scan
     */
    suspend fun update(newCommands: List<QuantizedCommand>) {
        mutex.withLock {
            commandsSnapshot = newCommands
                .filter { it.targetVuid != null && it.phrase.isNotBlank() }
                .associateBy { it.phrase.lowercase() }
        }
    }

    /**
     * Synchronous update for use in non-coroutine contexts.
     * Commands are keyed by phrase to allow multiple command types per element.
     *
     * Thread-safe: Volatile reference ensures atomic visibility of writes.
     * Prefer suspend update() when in a coroutine context.
     *
     * @param newCommands List of commands from current scan
     */
    fun updateSync(newCommands: List<QuantizedCommand>) {
        val validCommands = newCommands.filter { it.targetVuid != null && it.phrase.isNotBlank() }
        LoggingUtils.d("updateSync: received ${newCommands.size} commands, ${validCommands.size} valid", TAG)
        if (validCommands.isNotEmpty()) {
            LoggingUtils.d("updateSync: first 3 commands: ${validCommands.take(3).map { "'${it.phrase}'" }}", TAG)
        }
        // Key by phrase (lowercase) to allow multiple commands per targetVuid
        commandsSnapshot = validCommands.associateBy { it.phrase.lowercase() }
    }

    /**
     * Find command by voice phrase.
     * Supports exact match (direct key lookup) and partial label match.
     *
     * Thread-safe: Reads from immutable snapshot.
     *
     * @param phrase Voice input phrase
     * @return Matching QuantizedCommand or null
     */
    fun findByPhrase(phrase: String): QuantizedCommand? {
        val normalized = phrase.lowercase().trim()
        val snapshot = commandsSnapshot // Read volatile once
        LoggingUtils.d("findByPhrase('$phrase'): searching ${snapshot.size} commands", TAG)

        // Direct key lookup (O(1) - commands are keyed by phrase)
        val exactMatch = snapshot[normalized]
        if (exactMatch != null) {
            LoggingUtils.d("findByPhrase: exact match found - '${exactMatch.phrase}'", TAG)
            return exactMatch
        }

        // Partial match - check if input matches just the label part
        // Skip empty labels to avoid false matches
        val partialMatch = snapshot.values.firstOrNull { cmd: QuantizedCommand ->
            val label = cmd.phrase.substringAfter(" ").lowercase()
            label.isNotBlank() && (normalized == label || normalized.endsWith(label, ignoreCase = true))
        }
        if (partialMatch != null) {
            LoggingUtils.d("findByPhrase: partial match found - '${partialMatch.phrase}'", TAG)
        } else {
            LoggingUtils.d("findByPhrase: no match for '$phrase'. Available: ${snapshot.keys.take(5)}", TAG)
        }
        return partialMatch
    }

    companion object {
        private const val TAG = "CommandRegistry"
    }

    /**
     * Find command by target VUID.
     * Searches through all commands since they're keyed by phrase, not VUID.
     *
     * Thread-safe: Reads from immutable snapshot.
     *
     * @param vuid Target element VUID
     * @return First matching QuantizedCommand or null
     */
    fun findByVuid(vuid: String): QuantizedCommand? {
        return commandsSnapshot.values.firstOrNull { it.targetVuid == vuid }
    }

    /**
     * Find all commands targeting a specific VUID.
     * Returns all command types (label, index, numeric) for that element.
     *
     * Thread-safe: Reads from immutable snapshot.
     *
     * @param vuid Target element VUID
     * @return List of commands targeting this element
     */
    fun findAllByVuid(vuid: String): List<QuantizedCommand> {
        return commandsSnapshot.values.filter { it.targetVuid == vuid }
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
     * Commands are keyed by phrase, so multiple commands can target the same element.
     *
     * Thread-safe: Volatile reference ensures atomic visibility.
     *
     * @param commands List of commands to add
     */
    fun addAll(commands: List<QuantizedCommand>) {
        if (commands.isEmpty()) return
        val toAdd = commands
            .filter { it.targetVuid != null && it.phrase.isNotBlank() }
            .associateBy { it.phrase.lowercase() }
        LoggingUtils.d("addAll: adding ${toAdd.size} commands: ${toAdd.keys.take(5)}", TAG)
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
