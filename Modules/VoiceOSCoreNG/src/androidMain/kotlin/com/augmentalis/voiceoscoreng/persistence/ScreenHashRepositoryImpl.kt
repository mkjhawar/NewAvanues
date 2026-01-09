package com.augmentalis.voiceoscoreng.persistence

import android.util.Log
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ScreenHashRepo"

/**
 * In-memory implementation of ScreenHashRepository.
 *
 * For production, this should be backed by SQLDelight database.
 * This implementation provides fast lookups with LRU eviction.
 */
class ScreenHashRepositoryImpl : ScreenHashRepository {

    private val mutex = Mutex()

    // Screen hash -> ScreenCacheEntry
    private val screenCache = ConcurrentHashMap<String, ScreenCacheEntry>()

    // Screen hash -> List of commands
    private val commandCache = ConcurrentHashMap<String, List<QuantizedCommand>>()

    // Maximum cache size before LRU eviction
    private val maxCacheSize = 500

    // Access order tracking for LRU
    private val accessOrder = LinkedHashMap<String, Long>(100, 0.75f, true)

    override suspend fun hasScreen(hash: String): Boolean {
        val exists = screenCache.containsKey(hash)
        if (exists) {
            mutex.withLock {
                accessOrder[hash] = System.currentTimeMillis()
            }
        }
        return exists
    }

    override suspend fun saveScreen(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int
    ) {
        mutex.withLock {
            // Evict oldest if at capacity
            if (screenCache.size >= maxCacheSize) {
                evictOldest()
            }

            val entry = ScreenCacheEntry(
                hash = hash,
                packageName = packageName,
                activityName = activityName,
                appVersion = appVersion,
                elementCount = elementCount,
                actionableCount = 0,
                commandCount = 0,
                scannedAt = System.currentTimeMillis()
            )
            screenCache[hash] = entry
            accessOrder[hash] = System.currentTimeMillis()
            Log.d(TAG, "Saved screen hash: ${hash.take(16)}... for $packageName")
        }
    }

    override suspend fun getAppVersion(hash: String): String? {
        return screenCache[hash]?.appVersion
    }

    override suspend fun getCommandsForScreen(hash: String): List<QuantizedCommand> {
        mutex.withLock {
            accessOrder[hash] = System.currentTimeMillis()
        }
        return commandCache[hash] ?: emptyList()
    }

    override suspend fun saveCommandsForScreen(hash: String, commands: List<QuantizedCommand>) {
        mutex.withLock {
            commandCache[hash] = commands
            // Update command count in screen entry
            screenCache[hash]?.let { entry ->
                screenCache[hash] = entry.copy(
                    commandCount = commands.size,
                    actionableCount = commands.count { it.actionType != null }
                )
            }
            Log.d(TAG, "Saved ${commands.size} commands for screen ${hash.take(16)}...")
        }
    }

    override suspend fun clearScreen(hash: String) {
        mutex.withLock {
            screenCache.remove(hash)
            commandCache.remove(hash)
            accessOrder.remove(hash)
            Log.d(TAG, "Cleared screen: ${hash.take(16)}...")
        }
    }

    override suspend fun clearScreensForPackage(packageName: String): Int {
        mutex.withLock {
            val hashesToRemove = screenCache.entries
                .filter { it.value.packageName == packageName }
                .map { it.key }

            hashesToRemove.forEach { hash ->
                screenCache.remove(hash)
                commandCache.remove(hash)
                accessOrder.remove(hash)
            }

            Log.d(TAG, "Cleared ${hashesToRemove.size} screens for package: $packageName")
            return hashesToRemove.size
        }
    }

    override suspend fun clearAllScreens(): Int {
        mutex.withLock {
            val count = screenCache.size
            screenCache.clear()
            commandCache.clear()
            accessOrder.clear()
            Log.d(TAG, "Cleared ALL $count cached screens")
            return count
        }
    }

    override suspend fun getScreenCount(): Int {
        return screenCache.size
    }

    override suspend fun getScreenCountForPackage(packageName: String): Int {
        return screenCache.values.count { it.packageName == packageName }
    }

    override suspend fun getScreenInfo(hash: String): ScreenInfo? {
        val entry = screenCache[hash] ?: return null
        return ScreenInfo(
            hash = entry.hash,
            packageName = entry.packageName,
            activityName = entry.activityName,
            appVersion = entry.appVersion,
            elementCount = entry.elementCount,
            actionableCount = entry.actionableCount,
            commandCount = entry.commandCount,
            scannedAt = entry.scannedAt,
            isCached = true
        )
    }

    private fun evictOldest() {
        // Remove oldest 10% when at capacity
        val toRemove = (maxCacheSize * 0.1).toInt().coerceAtLeast(1)
        val oldestHashes = accessOrder.keys.take(toRemove)

        oldestHashes.forEach { hash ->
            screenCache.remove(hash)
            commandCache.remove(hash)
            accessOrder.remove(hash)
        }
        Log.d(TAG, "Evicted $toRemove oldest screen cache entries")
    }

    /**
     * Internal cache entry.
     */
    private data class ScreenCacheEntry(
        val hash: String,
        val packageName: String,
        val activityName: String?,
        val appVersion: String,
        val elementCount: Int,
        val actionableCount: Int,
        val commandCount: Int,
        val scannedAt: Long
    )
}
