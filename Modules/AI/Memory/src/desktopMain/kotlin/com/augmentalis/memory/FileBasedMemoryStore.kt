/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.memory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.io.File

/**
 * Desktop (JVM) file-based implementation of MemoryStore.
 *
 * Persists memories to ~/.augmentalis/memory/ directory using a simple
 * text-based format for easy debugging and portability.
 *
 * File structure:
 * - ~/.augmentalis/memory/
 *   - entries/ (individual memory files)
 *     - {id}.mem
 *
 * Memory file format:
 * ```
 * ID: {id}
 * TYPE: {type}
 * TIMESTAMP: {iso-timestamp}
 * IMPORTANCE: {float}
 * LAST_ACCESSED: {iso-timestamp or null}
 * ACCESS_COUNT: {int}
 * METADATA_COUNT: {int}
 * METADATA: key=value (one per line)
 * CONTENT_LENGTH: {int}
 * CONTENT:
 * {content}
 * ```
 */
class FileBasedMemoryStore : MemoryStore {

    private val baseDir = File(System.getProperty("user.home"), ".augmentalis/memory")
    private val entriesDir = File(baseDir, "entries")

    // In-memory cache for fast access
    private val cache = mutableMapOf<String, MemoryEntry>()
    private val mutex = Mutex()
    private var cacheLoaded = false

    init {
        // Ensure directories exist
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        if (!entriesDir.exists()) {
            entriesDir.mkdirs()
        }
    }

    /**
     * Load all memories from disk into cache
     */
    private suspend fun ensureCacheLoaded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (cacheLoaded) return@withContext

            if (entriesDir.exists()) {
                entriesDir.listFiles()?.filter { it.extension == "mem" }?.forEach { file ->
                    try {
                        val entry = parseMemoryFile(file)
                        if (entry != null) {
                            cache[entry.id] = entry
                        }
                    } catch (e: Exception) {
                        println("[FileBasedMemoryStore] Error loading ${file.name}: ${e.message}")
                    }
                }
            }
            cacheLoaded = true
            println("[FileBasedMemoryStore] Loaded ${cache.size} memories from disk")
        }
    }

    override suspend fun store(entry: MemoryEntry) {
        ensureCacheLoaded()
        mutex.withLock {
            cache[entry.id] = entry
        }
        persistEntry(entry)
    }

    override suspend fun retrieve(id: String): MemoryEntry? {
        ensureCacheLoaded()
        return mutex.withLock {
            cache[id]
        }
    }

    override suspend fun findByType(type: MemoryType): List<MemoryEntry> {
        ensureCacheLoaded()
        return mutex.withLock {
            cache.values.filter { it.type == type }.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun findByTimeRange(start: Instant, end: Instant): List<MemoryEntry> {
        ensureCacheLoaded()
        return mutex.withLock {
            cache.values.filter { it.timestamp >= start && it.timestamp <= end }
                .sortedByDescending { it.timestamp }
        }
    }

    override suspend fun search(query: String, limit: Int): List<MemoryEntry> {
        ensureCacheLoaded()
        val lowerQuery = query.lowercase()
        return mutex.withLock {
            cache.values
                .filter { entry ->
                    entry.content.lowercase().contains(lowerQuery) ||
                    entry.metadata.values.any { it.lowercase().contains(lowerQuery) }
                }
                .sortedByDescending { it.importance }
                .take(limit)
        }
    }

    override suspend fun delete(id: String) {
        ensureCacheLoaded()
        mutex.withLock {
            cache.remove(id)
        }
        withContext(Dispatchers.IO) {
            val file = File(entriesDir, "$id.mem")
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override suspend fun deleteByType(type: MemoryType) {
        ensureCacheLoaded()
        val toDelete = mutex.withLock {
            cache.values.filter { it.type == type }.map { it.id }
        }
        toDelete.forEach { id ->
            delete(id)
        }
    }

    override suspend fun findByImportance(threshold: Float): List<MemoryEntry> {
        ensureCacheLoaded()
        return mutex.withLock {
            cache.values.filter { it.importance >= threshold }
                .sortedByDescending { it.importance }
        }
    }

    override suspend fun update(entry: MemoryEntry) {
        ensureCacheLoaded()
        val exists = mutex.withLock { cache.containsKey(entry.id) }
        if (exists) {
            mutex.withLock {
                cache[entry.id] = entry
            }
            persistEntry(entry)
        }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            cache.clear()
        }
        withContext(Dispatchers.IO) {
            entriesDir.listFiles()?.forEach { it.delete() }
        }
        println("[FileBasedMemoryStore] Cleared all memories")
    }

    /**
     * Persist a single memory entry to disk
     */
    private suspend fun persistEntry(entry: MemoryEntry) = withContext(Dispatchers.IO) {
        try {
            val file = File(entriesDir, "${entry.id}.mem")
            val content = buildString {
                appendLine("ID: ${entry.id}")
                appendLine("TYPE: ${entry.type.name}")
                appendLine("TIMESTAMP: ${entry.timestamp}")
                appendLine("IMPORTANCE: ${entry.importance}")
                appendLine("LAST_ACCESSED: ${entry.lastAccessed ?: "null"}")
                appendLine("ACCESS_COUNT: ${entry.accessCount}")
                appendLine("METADATA_COUNT: ${entry.metadata.size}")
                entry.metadata.forEach { (key, value) ->
                    appendLine("METADATA: $key=$value")
                }
                appendLine("CONTENT_LENGTH: ${entry.content.length}")
                appendLine("CONTENT:")
                append(entry.content)
            }
            file.writeText(content)
        } catch (e: Exception) {
            println("[FileBasedMemoryStore] Error persisting ${entry.id}: ${e.message}")
        }
    }

    /**
     * Parse a memory file back into a MemoryEntry
     */
    private fun parseMemoryFile(file: File): MemoryEntry? {
        try {
            val lines = file.readLines()
            if (lines.isEmpty()) return null

            var id: String? = null
            var type: MemoryType? = null
            var timestamp: Instant? = null
            var importance = 0.5f
            var lastAccessed: Instant? = null
            var accessCount = 0
            val metadata = mutableMapOf<String, String>()
            var contentStartLine = -1

            for ((index, line) in lines.withIndex()) {
                when {
                    line.startsWith("ID: ") -> id = line.removePrefix("ID: ")
                    line.startsWith("TYPE: ") -> type = MemoryType.valueOf(line.removePrefix("TYPE: "))
                    line.startsWith("TIMESTAMP: ") -> timestamp = Instant.parse(line.removePrefix("TIMESTAMP: "))
                    line.startsWith("IMPORTANCE: ") -> importance = line.removePrefix("IMPORTANCE: ").toFloat()
                    line.startsWith("LAST_ACCESSED: ") -> {
                        val value = line.removePrefix("LAST_ACCESSED: ")
                        lastAccessed = if (value == "null") null else Instant.parse(value)
                    }
                    line.startsWith("ACCESS_COUNT: ") -> accessCount = line.removePrefix("ACCESS_COUNT: ").toInt()
                    line.startsWith("METADATA: ") -> {
                        val metaPart = line.removePrefix("METADATA: ")
                        val eqIndex = metaPart.indexOf('=')
                        if (eqIndex > 0) {
                            metadata[metaPart.substring(0, eqIndex)] = metaPart.substring(eqIndex + 1)
                        }
                    }
                    line == "CONTENT:" -> {
                        contentStartLine = index + 1
                        break
                    }
                }
            }

            if (id == null || type == null || timestamp == null || contentStartLine < 0) {
                return null
            }

            val content = if (contentStartLine < lines.size) {
                lines.subList(contentStartLine, lines.size).joinToString("\n")
            } else {
                ""
            }

            return MemoryEntry(
                id = id,
                type = type,
                content = content,
                timestamp = timestamp,
                importance = importance,
                metadata = metadata,
                lastAccessed = lastAccessed,
                accessCount = accessCount
            )
        } catch (e: Exception) {
            println("[FileBasedMemoryStore] Error parsing ${file.name}: ${e.message}")
            return null
        }
    }

    /**
     * Get storage statistics
     */
    fun getStorageStats(): StorageStats {
        val entryCount = entriesDir.listFiles()?.size ?: 0
        val totalSize = entriesDir.listFiles()?.sumOf { it.length() } ?: 0L
        return StorageStats(
            entryCount = entryCount,
            totalSizeBytes = totalSize,
            storageDirectory = baseDir.absolutePath
        )
    }

    /**
     * Force reload from disk (useful after external modifications)
     */
    suspend fun reload() {
        mutex.withLock {
            cache.clear()
            cacheLoaded = false
        }
        ensureCacheLoaded()
    }
}

/**
 * Storage statistics for the file-based memory store
 */
data class StorageStats(
    val entryCount: Int,
    val totalSizeBytes: Long,
    val storageDirectory: String
) {
    val totalSizeKB: Double get() = totalSizeBytes / 1024.0
    val totalSizeMB: Double get() = totalSizeBytes / (1024.0 * 1024.0)
}
