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
 * - ALL writes are protected by mutex (both suspend and sync versions)
 * - Reads access the volatile snapshot directly (no locking needed)
 * - This provides consistent reads without blocking and safe concurrent writes
 *
 * Performance optimization:
 * - Caches lowercase phrases during registration to avoid repeated string operations
 * - Direct key lookup is O(1), partial match is O(n) but with cached labels
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

    /**
     * Cached lowercase labels for partial matching (avoids repeated string operations).
     * Key: phrase.lowercase(), Value: label portion lowercase
     */
    @Volatile
    private var labelCache: Map<String, String> = emptyMap()

    private val mutex = Mutex() // Protects ALL concurrent writes

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
            updateInternal(newCommands)
        }
    }

    /**
     * Synchronous update for use in non-coroutine contexts.
     * Commands are keyed by phrase to allow multiple command types per element.
     *
     * Thread-safe: Uses mutex via tryLock with spinlock fallback.
     * This ensures consistency with the suspend update() method.
     *
     * @param newCommands List of commands from current scan
     */
    fun updateSync(newCommands: List<QuantizedCommand>) {
        // Spin until we acquire the mutex (avoids mixing @Synchronized with Mutex)
        while (!mutex.tryLock()) {
            // Brief yield to avoid busy-wait
            Thread.yield()
        }
        try {
            updateInternal(newCommands)
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Internal update logic - must be called while holding the mutex.
     */
    private fun updateInternal(newCommands: List<QuantizedCommand>) {
        val validCommands = newCommands.filter { it.targetVuid != null && it.phrase.isNotBlank() }
        LoggingUtils.d("update: received ${newCommands.size} commands, ${validCommands.size} valid", TAG)
        if (validCommands.isNotEmpty()) {
            LoggingUtils.d("update: first 3 commands: ${validCommands.take(3).map { "'${it.phrase}'" }}", TAG)
        }

        // Build command map and label cache together for efficiency
        val newSnapshot = mutableMapOf<String, QuantizedCommand>()
        val newLabelCache = mutableMapOf<String, String>()

        for (cmd in validCommands) {
            val key = cmd.phrase.lowercase()
            newSnapshot[key] = cmd
            // Cache the label portion (everything after first space)
            val label = cmd.phrase.substringAfter(" ").lowercase()
            if (label.length > 1) {
                newLabelCache[key] = label
            }
        }

        commandsSnapshot = newSnapshot
        labelCache = newLabelCache
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
        val labels = labelCache // Read volatile once
        LoggingUtils.d("findByPhrase('$phrase'): searching ${snapshot.size} commands", TAG)

        // Direct key lookup (O(1) - commands are keyed by phrase)
        val exactMatch = snapshot[normalized]
        if (exactMatch != null) {
            LoggingUtils.d("findByPhrase: exact match found - '${exactMatch.phrase}'", TAG)
            return exactMatch
        }

        // Partial match using cached labels (avoids repeated string operations)
        // BUG FIX: Skip empty labels to avoid false matches (endsWith("") always returns true)
        for ((key, label) in labels) {
            if (normalized == label || normalized.endsWith(label, ignoreCase = true)) {
                val match = snapshot[key]
                if (match != null) {
                    LoggingUtils.d("findByPhrase: partial match found - '${match.phrase}'", TAG)
                    return match
                }
            }
        }

        LoggingUtils.d("findByPhrase: no match for '$phrase'. Available: ${snapshot.keys.take(5)}", TAG)
        return null
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
     * Thread-safe: Uses mutex via tryLock with spinlock fallback.
     * This ensures consistency with the suspend update() method.
     *
     * @param commands List of commands to add
     */
    fun addAll(commands: List<QuantizedCommand>) {
        if (commands.isEmpty()) return

        // Spin until we acquire the mutex
        while (!mutex.tryLock()) {
            Thread.yield()
        }
        try {
            val toAdd = commands
                .filter { it.targetVuid != null && it.phrase.isNotBlank() }

            if (toAdd.isEmpty()) return

            val newSnapshot = commandsSnapshot.toMutableMap()
            val newLabelCache = labelCache.toMutableMap()

            for (cmd in toAdd) {
                val key = cmd.phrase.lowercase()
                newSnapshot[key] = cmd
                val label = cmd.phrase.substringAfter(" ").lowercase()
                if (label.length > 1) {
                    newLabelCache[key] = label
                }
            }

            LoggingUtils.d("addAll: adding ${toAdd.size} commands: ${toAdd.take(5).map { it.phrase }}", TAG)
            commandsSnapshot = newSnapshot
            labelCache = newLabelCache
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Clear all commands.
     *
     * Thread-safe: Uses mutex for consistency with other write operations.
     */
    fun clear() {
        while (!mutex.tryLock()) {
            Thread.yield()
        }
        try {
            commandsSnapshot = emptyMap()
            labelCache = emptyMap()
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Current command count.
     *
     * Thread-safe: Reads from immutable snapshot.
     */
    val size: Int get() = commandsSnapshot.size
}
