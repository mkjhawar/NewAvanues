package com.augmentalis.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android platform implementation of [MemoryStore].
 *
 * Provides two storage strategies:
 *
 * **Scoped in-memory store (default)**: All entries are kept in a thread-safe
 * in-memory map grouped by [conversationId]. This is the fast path for active
 * conversation contexts and does not touch disk.
 *
 * **File-backed persistence (opt-in)**: When [persistToDisk] is `true` the store
 * additionally serialises every entry to a per-conversation JSON file under the
 * application's `filesDir/memory/<conversationId>/` directory. On first access
 * for a conversation the store lazy-loads the file if it exists so entries survive
 * process restarts.
 *
 * Thread safety: all mutable state is protected by a [Mutex]. Disk I/O is
 * dispatched to [Dispatchers.IO] so it never blocks the calling coroutine.
 *
 * Serialization: entries are serialised via [kotlinx.serialization] (JSON format).
 * The on-disk schema is a single JSON array of [PersistedEntry] objects, one file
 * per conversation.
 *
 * Usage:
 * ```kotlin
 * val store = AndroidMemoryStore(
 *     context       = applicationContext,
 *     conversationId = "session_abc123",
 *     persistToDisk = true
 * )
 *
 * store.store(MemoryEntry(
 *     id         = "mem_001",
 *     type       = MemoryType.SHORT_TERM,
 *     content    = "User prefers dark mode",
 *     timestamp  = Clock.System.now(),
 *     importance = 0.8f,
 *     metadata   = mapOf("source" to "settings_observation")
 * ))
 *
 * val results = store.search("dark mode", limit = 5)
 * ```
 *
 * @param context Android [Context] used to resolve [filesDir] for on-disk persistence.
 * @param conversationId Logical scope that groups entries. Defaults to `"global"`.
 * @param persistToDisk Whether to write entries to disk for cross-process durability.
 *   Defaults to `true`.
 */
class AndroidMemoryStore(
    private val context: Context,
    val conversationId: String = "global",
    private val persistToDisk: Boolean = true
) : MemoryStore {

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, MemoryEntry>()
    private var diskLoaded = false

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // Reactive state so callers can observe store changes
    private val _changeSignal = MutableStateFlow(0L)
    val changeSignal: StateFlow<Long> = _changeSignal.asStateFlow()

    // ── MemoryStore interface ───────────────────────────────────────────────

    override suspend fun store(entry: MemoryEntry) {
        ensureDiskLoaded()
        mutex.withLock { entries[entry.id] = entry }
        if (persistToDisk) persistEntryToDisk(entry)
        _changeSignal.value = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun retrieve(id: String): MemoryEntry? {
        ensureDiskLoaded()
        return mutex.withLock {
            entries[id]?.also { existing ->
                val accessed = existing.withAccess(Clock.System.now())
                entries[id] = accessed
            }
        }
    }

    override suspend fun findByType(type: MemoryType): List<MemoryEntry> {
        ensureDiskLoaded()
        return mutex.withLock {
            entries.values
                .filter { it.type == type }
                .sortedByDescending { it.timestamp }
        }
    }

    override suspend fun findByTimeRange(start: Instant, end: Instant): List<MemoryEntry> {
        ensureDiskLoaded()
        return mutex.withLock {
            entries.values
                .filter { it.timestamp >= start && it.timestamp <= end }
                .sortedByDescending { it.timestamp }
        }
    }

    /**
     * Keyword-based content search with relevance ranking.
     *
     * Scoring logic:
     * - +2 points per query term matched in [MemoryEntry.content]
     * - +1 point per query term matched in any metadata value
     * - Results are ranked by (score DESC, importance DESC, timestamp DESC)
     *
     * This is intentionally a lightweight implementation. For semantic/embedding-based
     * retrieval the caller should layer an embedding index on top.
     */
    override suspend fun search(query: String, limit: Int): List<MemoryEntry> {
        ensureDiskLoaded()
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()

        return mutex.withLock {
            entries.values
                .mapNotNull { entry ->
                    val contentLower = entry.content.lowercase()
                    val metaLower = entry.metadata.values.joinToString(" ").lowercase()

                    val score = terms.sumOf { term ->
                        val contentHits = contentLower.split(term).size - 1
                        val metaHits = metaLower.split(term).size - 1
                        (contentHits * 2) + metaHits
                    }

                    if (score > 0) Pair(entry, score) else null
                }
                .sortedWith(compareByDescending<Pair<MemoryEntry, Int>> { it.second }
                    .thenByDescending { it.first.importance }
                    .thenByDescending { it.first.timestamp })
                .take(limit)
                .map { it.first }
        }
    }

    override suspend fun delete(id: String) {
        ensureDiskLoaded()
        mutex.withLock { entries.remove(id) }
        if (persistToDisk) rewriteDiskFile()
        _changeSignal.value = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun deleteByType(type: MemoryType) {
        ensureDiskLoaded()
        mutex.withLock {
            entries.values
                .filter { it.type == type }
                .map { it.id }
                .forEach { entries.remove(it) }
        }
        if (persistToDisk) rewriteDiskFile()
        _changeSignal.value = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun findByImportance(threshold: Float): List<MemoryEntry> {
        ensureDiskLoaded()
        return mutex.withLock {
            entries.values
                .filter { it.importance >= threshold }
                .sortedByDescending { it.importance }
        }
    }

    override suspend fun update(entry: MemoryEntry) {
        ensureDiskLoaded()
        val existed = mutex.withLock {
            if (entries.containsKey(entry.id)) {
                entries[entry.id] = entry
                true
            } else false
        }
        if (existed && persistToDisk) rewriteDiskFile()
        if (existed) _changeSignal.value = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun clearAll() {
        mutex.withLock { entries.clear() }
        if (persistToDisk) {
            withContext(Dispatchers.IO) {
                diskFileFor(conversationId).delete()
            }
        }
        _changeSignal.value = Clock.System.now().toEpochMilliseconds()
    }

    // ── Extended Android-specific API ───────────────────────────────────────

    /**
     * Return all entries currently held in memory (no search filter).
     * Sorted by timestamp descending (most recent first).
     */
    suspend fun allEntries(): List<MemoryEntry> {
        ensureDiskLoaded()
        return mutex.withLock {
            entries.values.sortedByDescending { it.timestamp }
        }
    }

    /**
     * Return the number of entries currently stored.
     */
    suspend fun count(): Int {
        ensureDiskLoaded()
        return mutex.withLock { entries.size }
    }

    /**
     * Return storage metadata for diagnostic purposes.
     */
    suspend fun storageInfo(): StorageInfo {
        ensureDiskLoaded()
        val entryCount = mutex.withLock { entries.size }
        val file = diskFileFor(conversationId)
        return StorageInfo(
            conversationId = conversationId,
            entryCount = entryCount,
            persistedToDisk = persistToDisk,
            diskFilePath = if (persistToDisk) file.absolutePath else null,
            diskFileSizeBytes = if (persistToDisk && file.exists()) file.length() else 0L
        )
    }

    /**
     * Force a reload from disk, discarding any current in-memory state.
     * Only meaningful when [persistToDisk] is `true`.
     */
    suspend fun reload() {
        mutex.withLock {
            entries.clear()
            diskLoaded = false
        }
        ensureDiskLoaded()
    }

    // ── Disk persistence helpers ────────────────────────────────────────────

    private suspend fun ensureDiskLoaded() {
        if (!persistToDisk) return
        val needsLoad = mutex.withLock { !diskLoaded }
        if (!needsLoad) return

        val loaded = withContext(Dispatchers.IO) {
            loadFromDisk(conversationId)
        }

        mutex.withLock {
            if (!diskLoaded) {
                loaded.forEach { entries[it.id] = it }
                diskLoaded = true
            }
        }
    }

    private suspend fun persistEntryToDisk(entry: MemoryEntry) {
        withContext(Dispatchers.IO) {
            try {
                // Append-safe strategy: read existing, merge, rewrite.
                // For single-entry updates this is acceptable; for bulk writes
                // callers should batch and call rewriteDiskFile() once.
                val file = diskFileFor(conversationId)
                val existing = if (file.exists()) {
                    runCatching {
                        json.decodeFromString<List<PersistedEntry>>(file.readText())
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }

                val updated = existing
                    .filter { it.id != entry.id }
                    .plus(entry.toPersistedEntry())

                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(updated))
            } catch (e: Exception) {
                // Disk write failure must not crash the store. Log and continue.
                android.util.Log.w(
                    "AndroidMemoryStore",
                    "Failed to persist entry ${entry.id} for conversation=$conversationId",
                    e
                )
            }
        }
    }

    private suspend fun rewriteDiskFile() {
        val snapshot = mutex.withLock { entries.values.toList() }
        withContext(Dispatchers.IO) {
            try {
                val file = diskFileFor(conversationId)
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(snapshot.map { it.toPersistedEntry() }))
            } catch (e: Exception) {
                android.util.Log.w(
                    "AndroidMemoryStore",
                    "Failed to rewrite disk file for conversation=$conversationId",
                    e
                )
            }
        }
    }

    private fun loadFromDisk(conversationId: String): List<MemoryEntry> {
        val file = diskFileFor(conversationId)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<PersistedEntry>>(file.readText())
                .map { it.toMemoryEntry() }
        } catch (e: Exception) {
            android.util.Log.w(
                "AndroidMemoryStore",
                "Failed to load disk file for conversation=$conversationId — starting fresh.",
                e
            )
            emptyList()
        }
    }

    private fun diskFileFor(conversationId: String): File {
        val safeId = conversationId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return File(context.filesDir, "memory/$safeId/entries.json")
    }

    // ── Serialization models ────────────────────────────────────────────────

    @Serializable
    private data class PersistedEntry(
        val id: String,
        val type: String,
        val content: String,
        val timestampEpochMs: Long,
        val importance: Float,
        val metadata: Map<String, String>,
        val lastAccessedEpochMs: Long?,
        val accessCount: Int
    )

    private fun MemoryEntry.toPersistedEntry() = PersistedEntry(
        id = id,
        type = type.name,
        content = content,
        timestampEpochMs = timestamp.toEpochMilliseconds(),
        importance = importance,
        metadata = metadata,
        lastAccessedEpochMs = lastAccessed?.toEpochMilliseconds(),
        accessCount = accessCount
    )

    private fun PersistedEntry.toMemoryEntry() = MemoryEntry(
        id = id,
        type = MemoryType.valueOf(type),
        content = content,
        timestamp = Instant.fromEpochMilliseconds(timestampEpochMs),
        importance = importance,
        metadata = metadata,
        lastAccessed = lastAccessedEpochMs?.let { Instant.fromEpochMilliseconds(it) },
        accessCount = accessCount
    )
}

/**
 * Diagnostic metadata for an [AndroidMemoryStore] instance.
 */
data class StorageInfo(
    val conversationId: String,
    val entryCount: Int,
    val persistedToDisk: Boolean,
    val diskFilePath: String?,
    val diskFileSizeBytes: Long
) {
    val diskFileSizeKB: Double get() = diskFileSizeBytes / 1024.0
}
