package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.LoggingUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
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
 * IMPORTANT: Commands are keyed by PHRASE (not targetAvid) to allow multiple
 * command types to coexist for the same element:
 * - Label commands: "Gmail", "Settings", "Arby's"
 * - Index commands (DynamicLists): "first", "second", "third"
 * - Numeric commands: "1", "2", "3"
 * All can target the same element but have different phrases.
 */
class CommandRegistry {
    /**
     * Atomic snapshot of commands and label cache.
     * Using a single reference ensures consistent reads without the risk of
     * reading commands from one update and labelCache from another.
     *
     * sourceKeys tracks which phrase keys belong to which source (e.g., "accessibility", "web").
     * This enables source-isolated updates: updating one source never removes another's commands.
     */
    private data class CommandSnapshot(
        val commands: Map<String, QuantizedCommand>,
        val labelCache: Map<String, String>,
        val sourceKeys: Map<String, Set<String>> = emptyMap()
    )

    /**
     * Immutable snapshot of commands and labels.
     * Volatile ensures visibility of writes across threads.
     * All reads see a consistent snapshot; writes replace atomically.
     *
     * Key: phrase.lowercase() - allows multiple commands per targetAvid
     * Value: QuantizedCommand
     */
    @kotlin.concurrent.Volatile
    private var snapshot: CommandSnapshot = CommandSnapshot(emptyMap(), emptyMap())

    private val mutex = Mutex() // Protects ALL concurrent writes

    /** Monotonically increasing counter — incremented on every write for cache invalidation */
    @kotlin.concurrent.Volatile
    private var _generation: Long = 0

    /** Current generation (snapshot version). Used by CommandMatcher for cache invalidation. */
    fun generation(): Long = _generation

    /**
     * Update registry with new commands, replacing all existing.
     * Commands with null targetAvid are ignored (cannot be executed).
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
     * Thread-safe: Uses runBlocking with timeout to acquire the mutex.
     * This avoids spinlock issues that could cause command execution to stop
     * after repeated app switches.
     *
     * @param newCommands List of commands from current scan
     */
    @Deprecated(
        message = "Use the suspend update() instead. runBlocking risks blocking the calling thread " +
            "for up to 5 seconds under mutex contention.",
        replaceWith = ReplaceWith("update(newCommands)")
    )
    fun updateSync(newCommands: List<QuantizedCommand>) {
        runBlocking {
            withTimeout(5000L) {
                mutex.withLock {
                    updateInternal(newCommands)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Source-Aware Updates
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Update commands from a specific source, preserving commands from other sources.
     *
     * Each source (e.g., "accessibility", "web") manages its own set of commands.
     * Calling updateBySource("web", webCommands) replaces ONLY web commands;
     * accessibility commands remain untouched.
     *
     * This eliminates the race condition where accessibility tree scans
     * overwrite web DOM commands (and vice versa).
     *
     * Thread-safe: Uses runBlocking with timeout to acquire the mutex.
     *
     * @param source Source identifier (e.g., "accessibility", "web", "plugin")
     * @param newCommands Commands from this source
     */
    @Deprecated(
        message = "Use the suspend updateBySourceSuspend() instead. runBlocking risks blocking the " +
            "calling thread for up to 5 seconds under mutex contention.",
        replaceWith = ReplaceWith("updateBySourceSuspend(source, newCommands)")
    )
    fun updateBySource(source: String, newCommands: List<QuantizedCommand>) {
        runBlocking {
            withTimeout(5000L) {
                mutex.withLock {
                    updateBySourceInternal(source, newCommands)
                }
            }
        }
    }

    /**
     * Suspend version of [updateBySource] for coroutine contexts.
     */
    suspend fun updateBySourceSuspend(source: String, newCommands: List<QuantizedCommand>) {
        mutex.withLock {
            updateBySourceInternal(source, newCommands)
        }
    }

    /**
     * Internal source-aware update logic - must be called while holding the mutex.
     *
     * Steps:
     * 1. Remove all commands belonging to [source] from the current snapshot
     * 2. Add new valid commands
     * 3. Update sourceKeys index: new key ownership goes to this source,
     *    removed from any other source that previously owned it
     * 4. Rebuild label cache
     * 5. Single atomic snapshot write
     */
    private fun updateBySourceInternal(source: String, newCommands: List<QuantizedCommand>) {
        val snap = snapshot
        val updatedCommands = snap.commands.toMutableMap()
        val updatedSourceKeys = snap.sourceKeys.toMutableMap()

        // Remove all commands belonging to this source
        val oldKeys = updatedSourceKeys[source] ?: emptySet()
        oldKeys.forEach { updatedCommands.remove(it) }

        // Add new valid commands
        val validCommands = newCommands.filter { it.targetAvid != null && it.phrase.isNotBlank() }
        val newKeys = mutableSetOf<String>()

        for (cmd in validCommands) {
            val key = cmd.phrase.lowercase()
            updatedCommands[key] = cmd
            newKeys.add(key)

            // If another source owned this key, transfer ownership
            // Snapshot before mutation to avoid ConcurrentModificationException
            for ((otherSource, otherKeys) in updatedSourceKeys.toMap()) {
                if (otherSource != source && key in otherKeys) {
                    LoggingUtils.d("Key conflict: '$key' transferred from $otherSource to $source", TAG)
                    updatedSourceKeys[otherSource] = otherKeys - key
                }
            }
        }
        updatedSourceKeys[source] = newKeys

        // Rebuild label cache
        val newLabelCache = mutableMapOf<String, String>()
        for ((key, cmd) in updatedCommands) {
            val label = cmd.phrase.substringAfter(" ").lowercase()
            if (label.length > 1) {
                newLabelCache[key] = label
            }
        }

        LoggingUtils.d("updateBySource('$source'): removed ${oldKeys.size}, added ${newKeys.size}, total ${updatedCommands.size}", TAG)

        // Single atomic write
        snapshot = CommandSnapshot(updatedCommands, newLabelCache, updatedSourceKeys)
        _generation++
    }

    /**
     * Clear commands from a specific source only.
     * Other sources' commands remain untouched.
     *
     * Thread-safe: Uses runBlocking with timeout to acquire the mutex.
     *
     * @param source Source identifier to clear
     */
    @Deprecated(
        message = "Use the suspend clearBySourceSuspend() instead. runBlocking risks blocking the " +
            "calling thread for up to 5 seconds under mutex contention.",
        replaceWith = ReplaceWith("clearBySourceSuspend(source)")
    )
    fun clearBySource(source: String) {
        runBlocking {
            withTimeout(5000L) {
                mutex.withLock {
                    updateBySourceInternal(source, emptyList())
                }
            }
        }
    }

    /**
     * Suspend variant of [clearBySource] for coroutine contexts.
     *
     * @param source Source identifier to clear
     */
    suspend fun clearBySourceSuspend(source: String) {
        mutex.withLock {
            updateBySourceInternal(source, emptyList())
        }
    }

    /**
     * Internal update logic - must be called while holding the mutex.
     * Replaces ALL commands regardless of source (clears sourceKeys).
     */
    private fun updateInternal(newCommands: List<QuantizedCommand>) {
        val validCommands = newCommands.filter { it.targetAvid != null && it.phrase.isNotBlank() }
        LoggingUtils.d("update: received ${newCommands.size} commands, ${validCommands.size} valid", TAG)
        if (validCommands.isNotEmpty()) {
            LoggingUtils.d("update: first 3 commands: ${validCommands.take(3).map { "'${it.phrase}'" }}", TAG)
        }

        // Build command map and label cache together for efficiency
        val newCommands = mutableMapOf<String, QuantizedCommand>()
        val newLabelCache = mutableMapOf<String, String>()

        for (cmd in validCommands) {
            val key = cmd.phrase.lowercase()
            newCommands[key] = cmd
            // Cache the label portion (everything after first space)
            val label = cmd.phrase.substringAfter(" ").lowercase()
            if (label.length > 1) {
                newLabelCache[key] = label
            }
        }

        // Single atomic write ensures consistent reads
        snapshot = CommandSnapshot(newCommands, newLabelCache)
        _generation++
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
        val snap = snapshot // Single atomic read
        LoggingUtils.d("findByPhrase('$phrase'): searching ${snap.commands.size} commands", TAG)

        // Direct key lookup (O(1) - commands are keyed by phrase)
        val exactMatch = snap.commands[normalized]
        if (exactMatch != null) {
            LoggingUtils.d("findByPhrase: exact match found - '${exactMatch.phrase}'", TAG)
            return exactMatch
        }

        // Partial match using cached labels (avoids repeated string operations)
        // BUG FIX: Skip empty labels to avoid false matches (endsWith("") always returns true)
        for ((key, label) in snap.labelCache) {
            if (normalized == label || normalized.endsWith(label, ignoreCase = true)) {
                val match = snap.commands[key]
                if (match != null) {
                    LoggingUtils.d("findByPhrase: partial match found - '${match.phrase}'", TAG)
                    return match
                }
            }
        }

        LoggingUtils.d("findByPhrase: no match for '$phrase'. Available: ${snap.commands.keys.take(5)}", TAG)
        return null
    }

    companion object {
        private const val TAG = "CommandRegistry"
    }

    /**
     * Find command by target AVID.
     * Searches through all commands since they're keyed by phrase, not AVID.
     *
     * Thread-safe: Reads from immutable snapshot.
     *
     * @param avid Target element AVID
     * @return First matching QuantizedCommand or null
     */
    fun findByAvid(avid: String): QuantizedCommand? {
        return snapshot.commands.values.firstOrNull { it.targetAvid == avid }
    }

    /**
     * Find all commands targeting a specific AVID.
     * Returns all command types (label, index, numeric) for that element.
     *
     * Thread-safe: Reads from immutable snapshot.
     *
     * @param avid Target element AVID
     * @return List of commands targeting this element
     */
    fun findAllByAvid(avid: String): List<QuantizedCommand> {
        return snapshot.commands.values.filter { it.targetAvid == avid }
    }

    /**
     * Get all commands in registry.
     *
     * Thread-safe: Returns values from immutable snapshot.
     *
     * @return List of all commands
     */
    fun all(): List<QuantizedCommand> {
        return snapshot.commands.values.toList()
    }

    /**
     * Add commands to the registry without replacing existing ones.
     * Useful for adding index commands ("first", "second") alongside existing commands.
     * Commands are keyed by phrase, so multiple commands can target the same element.
     *
     * Thread-safe: Uses runBlocking with timeout to acquire the mutex.
     * This avoids spinlock issues that could cause command execution to stop
     * after repeated app switches.
     *
     * @param commands List of commands to add
     */
    @Deprecated(
        message = "Use the suspend addAllSuspend() instead. runBlocking risks blocking the calling " +
            "thread for up to 5 seconds under mutex contention.",
        replaceWith = ReplaceWith("addAllSuspend(commands)")
    )
    fun addAll(commands: List<QuantizedCommand>) {
        if (commands.isEmpty()) return

        runBlocking {
            withTimeout(5000L) {
                mutex.withLock {
                    addAllInternal(commands)
                }
            }
        }
    }

    /**
     * Suspend variant of [addAll] for coroutine contexts.
     *
     * @param commands List of commands to add
     */
    suspend fun addAllSuspend(commands: List<QuantizedCommand>) {
        if (commands.isEmpty()) return
        mutex.withLock {
            addAllInternal(commands)
        }
    }

    /** Internal add-all logic — must be called while holding the mutex. */
    private fun addAllInternal(commands: List<QuantizedCommand>) {
        val toAdd = commands.filter { it.targetAvid != null && it.phrase.isNotBlank() }
        if (toAdd.isEmpty()) return

        val snap = snapshot
        val newCommands = snap.commands.toMutableMap()
        val newLabelCache = snap.labelCache.toMutableMap()

        for (cmd in toAdd) {
            val key = cmd.phrase.lowercase()
            newCommands[key] = cmd
            val label = cmd.phrase.substringAfter(" ").lowercase()
            if (label.length > 1) {
                newLabelCache[key] = label
            }
        }

        LoggingUtils.d("addAll: adding ${toAdd.size} commands: ${toAdd.take(5).map { it.phrase }}", TAG)
        // Single atomic write ensures consistent reads — preserve sourceKeys
        snapshot = CommandSnapshot(newCommands, newLabelCache, snap.sourceKeys)
        _generation++
    }

    /**
     * Clear all commands.
     *
     * Thread-safe: Uses runBlocking with timeout to acquire the mutex.
     * This avoids spinlock issues that could cause command execution to stop
     * after repeated app switches.
     */
    @Deprecated(
        message = "Use the suspend clearSuspend() instead. runBlocking risks blocking the calling " +
            "thread for up to 5 seconds under mutex contention.",
        replaceWith = ReplaceWith("clearSuspend()")
    )
    fun clear() {
        runBlocking {
            withTimeout(5000L) {
                mutex.withLock {
                    snapshot = CommandSnapshot(emptyMap(), emptyMap())
                    _generation++
                }
            }
        }
    }

    /**
     * Suspend variant of [clear] for coroutine contexts.
     */
    suspend fun clearSuspend() {
        mutex.withLock {
            snapshot = CommandSnapshot(emptyMap(), emptyMap())
            _generation++
        }
    }

    /**
     * Current command count.
     *
     * Thread-safe: Reads from immutable snapshot.
     */
    val size: Int get() = snapshot.commands.size
}
