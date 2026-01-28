/**
 * CommandCache.kt - 3-tier caching system for command resolution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-10
 *
 * Implements tiered caching for <100ms command resolution
 * Based on Q3 Decision: Tiered Caching (Tier 1/2/3)
 */
package com.augmentalis.commandmanager.cache

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import android.util.LruCache
import com.augmentalis.commandmanager.database.CommandDatabase
import com.augmentalis.commandmanager.Command
import com.augmentalis.commandmanager.CommandSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 3-tier command cache
 *
 * Architecture:
 * - Tier 1: Top 20 preloaded commands (~10KB, <0.5ms)
 * - Tier 2: LRU cache of 50 recently used (~25KB, <0.5ms)
 * - Tier 3: Database fallback (5-15ms) - Currently stubbed
 *
 * Performance: <100ms total command resolution
 * Memory: ~35KB total footprint
 */
class CommandCache(private val context: Context) {

    companion object {
        private const val TAG = "CommandCache"
        private const val TIER_1_SIZE = 20
        private const val TIER_2_SIZE = 50
    }

    // Tier 1: Preloaded top 20 commands (~10KB, instant access)
    private val tier1Cache: MutableMap<String, Command> = mutableMapOf()

    // Tier 2: LRU cache for recently used (max 50, ~25KB, fast access)
    private val tier2LRUCache = LruCache<String, Command>(TIER_2_SIZE)

    // Cache hit statistics
    private var tier1Hits = 0L
    private var tier2Hits = 0L
    private var tier3Hits = 0L
    private var cacheMisses = 0L

    // Q3 Enhancement 4: Performance metrics
    private var totalQueryTimeMs = 0L
    private var queryCount = 0L
    private var lastMemoryCheck = 0L
    private var currentMemoryLevel = MemoryLevel.NORMAL

    // Q3 Enhancement 5: Adaptive sizing
    private var currentTier1Size = TIER_1_SIZE
    private var currentTier2Size = TIER_2_SIZE

    // Usage patterns for predictive preloading
    private val appUsagePatterns = mutableMapOf<String, MutableList<String>>()

    // Database for Tier 3 fallback
    private val database by lazy {
        CommandDatabase.getInstance(context).voiceCommandDao()
    }

    init {
        // Load Tier 1 cache at initialization
        loadTier1Cache()
    }

    /**
     * Load Tier 1 cache with top 20 commands
     * Q3 User Clarification: Load only English + device locale
     */
    private fun loadTier1Cache() {
        val deviceLocale = Locale.getDefault().toLanguageTag()
        val locales = setOf("en-US", deviceLocale)

        Log.i(TAG, "Loading Tier 1 cache for locales: $locales")

        // Load global commands from database asynchronously
        // For now, also preload common navigation commands as fallback
        val commonCommands = listOf(
            Command("nav_back", "back", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("nav_home", "home", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("nav_recent", "recent apps", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("volume_up", "volume up", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("volume_down", "volume down", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("mute", "mute", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("wifi_toggle", "toggle wifi", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("bluetooth_toggle", "toggle bluetooth", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("open_settings", "open settings", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("screenshot", "take screenshot", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("notifications", "open notifications", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("quick_settings", "quick settings", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("power_dialog", "power menu", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("lock_screen", "lock screen", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("brightness_up", "brightness up", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("brightness_down", "brightness down", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("rotate_screen", "rotate screen", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("flashlight", "flashlight", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("airplane_mode", "airplane mode", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis()),
            Command("do_not_disturb", "do not disturb", CommandSource.SYSTEM, confidence = 1.0f, timestamp = System.currentTimeMillis())
        )

        commonCommands.forEach { command ->
            tier1Cache[command.text] = command
        }

        Log.i(TAG, "Tier 1 cache loaded with ${tier1Cache.size} commands (fallback)")

        // Asynchronously load from database to replace fallback commands
        loadGlobalCommandsFromDatabase()
    }

    /**
     * Load global commands from database
     * Replaces fallback commands with database entries
     */
    private fun loadGlobalCommandsFromDatabase() {
        try {
            // Launch coroutine to load from database
        @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val deviceLocale = Locale.getDefault().toLanguageTag()
                    val entities = database.getGlobalCommands(deviceLocale)

                    // Convert entities to Command objects and add to tier1Cache
                    entities.take(TIER_1_SIZE).forEach { entity ->
                        val command = Command(
                            id = entity.id,
                            text = entity.primaryText,
                            source = CommandSource.VOICE,
                            confidence = 1.0f,
                            timestamp = System.currentTimeMillis()
                        )
                        tier1Cache[command.text.lowercase()] = command
                    }

                    Log.i(TAG, "Tier 1 cache updated with ${entities.size} commands from database")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load global commands from database", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch database load coroutine", e)
        }
    }

    /**
     * Resolve command using tiered caching
     *
     * @param text Command text to resolve
     * @param context App context (package name) for context-aware resolution
     * @return Resolved command or null if not found
     */
    suspend fun resolveCommand(text: String, context: String?): Command? {
        val normalizedText = text.lowercase().trim()

        // Tier 1: Check preloaded cache (instant)
        tier1Cache[normalizedText]?.let {
            tier1Hits++
            trackCacheHit(CacheTier.TIER_1)
            Log.v(TAG, "Tier 1 hit: $normalizedText")
            return it
        }

        // Tier 2: Check LRU cache (fast)
        tier2LRUCache.get(normalizedText)?.let {
            tier2Hits++
            trackCacheHit(CacheTier.TIER_2)
            Log.v(TAG, "Tier 2 hit: $normalizedText")
            return it
        }

        // Tier 3: Query database (slower but acceptable)
        // TODO: Implement database query when available
        val command = queryDatabase(normalizedText, context)
        command?.let {
            tier3Hits++
            trackCacheHit(CacheTier.TIER_3)
            // Promote to Tier 2 for future fast access
            tier2LRUCache.put(normalizedText, it)
            Log.v(TAG, "Tier 3 hit: $normalizedText (promoted to Tier 2)")
            return it
        }

        // Cache miss
        cacheMisses++
        Log.v(TAG, "Cache miss: $normalizedText")
        return null
    }

    /**
     * Tier 3: Database query
     * Searches database for command by text with fuzzy matching
     */
    private suspend fun queryDatabase(text: String, context: String?): Command? {
        return withContext(Dispatchers.IO) {
            try {
                val deviceLocale = Locale.getDefault().toLanguageTag()

                // Search commands in database
                val entities = database.searchCommands(deviceLocale, text)

                // Return first match if found
                if (entities.isNotEmpty()) {
                    val entity = entities.first()
                    Command(
                        id = entity.id,
                        text = entity.primaryText,
                        source = CommandSource.VOICE,
                        confidence = 1.0f,
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    // Try fallback to English if device locale failed
                    if (deviceLocale != "en-US") {
                        val fallbackEntities = database.searchCommands("en-US", text)
                        if (fallbackEntities.isNotEmpty()) {
                            val entity = fallbackEntities.first()
                            Command(
                                id = entity.id,
                                text = entity.primaryText,
                                source = CommandSource.VOICE,
                                confidence = 0.8f, // Lower confidence for fallback
                                timestamp = System.currentTimeMillis()
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Database query failed for: $text", e)
                null
            }
        }
    }

    /**
     * Track cache hit for analytics
     */
    private fun trackCacheHit(tier: CacheTier) {
        Log.v(TAG, "Cache hit: $tier")
        // TODO: Send to performance analytics when Q3 Enhancement 4 is implemented
    }

    /**
     * Set priority commands for foreground app (context rotation)
     * Used by CommandContextManager to optimize cache for current app
     */
    fun setPriorityCommands(commands: List<Command>) {
        Log.d(TAG, "Updating priority commands: ${commands.size} commands")

        // Clear Tier 2 and reload with priority commands
        tier2LRUCache.evictAll()

        commands.forEach { command ->
            tier2LRUCache.put(command.text, command)
        }

        Log.i(TAG, "Priority commands loaded into Tier 2 cache")
    }

    /**
     * Get cache statistics
     */
    fun getStatistics(): CacheStatistics {
        val total = tier1Hits + tier2Hits + tier3Hits + cacheMisses
        return CacheStatistics(
            tier1Hits = tier1Hits,
            tier2Hits = tier2Hits,
            tier3Hits = tier3Hits,
            cacheMisses = cacheMisses,
            tier1HitRate = if (total > 0) tier1Hits.toFloat() / total else 0f,
            tier2HitRate = if (total > 0) tier2Hits.toFloat() / total else 0f,
            tier3HitRate = if (total > 0) tier3Hits.toFloat() / total else 0f,
            totalQueries = total
        )
    }

    /**
     * Reset cache statistics
     */
    fun resetStatistics() {
        tier1Hits = 0
        tier2Hits = 0
        tier3Hits = 0
        cacheMisses = 0
        Log.d(TAG, "Cache statistics reset")
    }

    /**
     * Clear all caches
     */
    fun clearAll() {
        tier2LRUCache.evictAll()
        Log.i(TAG, "All caches cleared (Tier 1 preserved)")
    }

    // ========== Q3 CACHE OPTIMIZATIONS ==========

    /**
     * Q3 Enhancement 1: Predictive Preloading
     * Preload commands likely to be used next based on app context and usage patterns
     */
    fun predictivePreload(appContext: String) {
        Log.d(TAG, "Predictive preloading for app: $appContext")

        // Get usage patterns for this app
        val patterns = appUsagePatterns[appContext] ?: return

        // Load most frequently used commands for this app into Tier 2
        val frequentCommands = patterns
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(currentTier2Size / 2)

        frequentCommands.forEach { (commandText, _) ->
            // Check if already in cache
            if (tier1Cache[commandText] == null && tier2LRUCache.get(commandText) == null) {
                // Create placeholder command (will be resolved on first use)
                val command = Command(
                    id = "predicted_$commandText",
                    text = commandText,
                    source = CommandSource.SYSTEM,
                    confidence = 0.9f,
                    timestamp = System.currentTimeMillis()
                )
                tier2LRUCache.put(commandText, command)
            }
        }

        Log.i(TAG, "Predictive preload: loaded ${frequentCommands.size} commands for $appContext")
    }

    /**
     * Record command usage for pattern learning
     */
    fun recordUsage(appContext: String, commandText: String) {
        val patterns = appUsagePatterns.getOrPut(appContext) { mutableListOf() }
        patterns.add(commandText)

        // Keep only last 100 commands per app
        if (patterns.size > 100) {
            patterns.removeAt(0)
        }
    }

    /**
     * Q3 Enhancement 2: Cache Warming
     * Warm cache on service start with user's frequent commands
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun warmCache() {
        Log.d(TAG, "Warming cache with frequent commands")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val deviceLocale = Locale.getDefault().toLanguageTag()

                // Load global commands (SYSTEM, NAVIGATION) which are most frequently used
                val frequentCommands = database.getGlobalCommands(deviceLocale).take(currentTier2Size)

                frequentCommands.forEach { entity ->
                    val command = Command(
                        id = entity.id,
                        text = entity.primaryText,
                        source = CommandSource.VOICE,
                        confidence = 1.0f,
                        timestamp = System.currentTimeMillis()
                    )
                    tier2LRUCache.put(command.text.lowercase(), command)
                }

                Log.i(TAG, "Cache warmed with ${frequentCommands.size} frequent commands")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to warm cache", e)
            }
        }
    }

    /**
     * Q3 Enhancement 3: Memory Pressure Monitoring
     * Monitor memory usage and adjust cache size dynamically
     */
    fun monitorMemoryPressure() {
        val now = System.currentTimeMillis()

        // Only check every 30 seconds
        if (now - lastMemoryCheck < 30_000) return
        lastMemoryCheck = now

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val usedPercent = ((totalMemoryMB - availableMemoryMB).toFloat() / totalMemoryMB) * 100

        val newLevel = when {
            usedPercent > 90 || memoryInfo.lowMemory -> MemoryLevel.CRITICAL
            usedPercent > 80 -> MemoryLevel.HIGH
            usedPercent > 60 -> MemoryLevel.MEDIUM
            else -> MemoryLevel.NORMAL
        }

        if (newLevel != currentMemoryLevel) {
            Log.i(TAG, "Memory level changed: $currentMemoryLevel -> $newLevel (${usedPercent.toInt()}% used)")
            currentMemoryLevel = newLevel

            // Adjust cache based on memory pressure
            when (newLevel) {
                MemoryLevel.CRITICAL -> {
                    tier2LRUCache.evictAll()
                    Log.w(TAG, "Critical memory: evicted Tier 2 cache")
                }
                MemoryLevel.HIGH -> {
                    // Reduce Tier 2 to half
                    val currentSize = tier2LRUCache.size()
                    tier2LRUCache.trimToSize(currentSize / 2)
                    Log.w(TAG, "High memory pressure: trimmed Tier 2 cache")
                }
                MemoryLevel.MEDIUM -> {
                    // Trim to 75%
                    val currentSize = tier2LRUCache.size()
                    tier2LRUCache.trimToSize((currentSize * 0.75).toInt())
                }
                MemoryLevel.NORMAL -> {
                    // No action needed
                }
            }
        }
    }

    /**
     * Q3 Enhancement 4: Performance Analytics
     * Get detailed performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics {
        val total = tier1Hits + tier2Hits + tier3Hits + cacheMisses
        val avgQueryTime = if (queryCount > 0) totalQueryTimeMs.toFloat() / queryCount else 0f

        return PerformanceMetrics(
            cacheStatistics = getStatistics(),
            averageQueryTimeMs = avgQueryTime,
            totalQueries = queryCount,
            memoryLevel = currentMemoryLevel,
            tier1Size = tier1Cache.size,
            tier2Size = tier2LRUCache.size(),
            tier1MaxSize = currentTier1Size,
            tier2MaxSize = currentTier2Size
        )
    }

    /**
     * Track query performance
     */
    fun trackQueryTime(timeMs: Long) {
        totalQueryTimeMs += timeMs
        queryCount++
    }

    /**
     * Q3 Enhancement 5: Adaptive Cache Sizing
     * Adjust Tier 1/2 sizes based on device capabilities
     */
    fun adaptiveCacheSize() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = activityManager.memoryClass // MB available to app

        // Adjust cache sizes based on available memory
        when {
            memoryClass >= 512 -> {
                // High-end device
                currentTier1Size = 30
                currentTier2Size = 100
            }
            memoryClass >= 256 -> {
                // Mid-range device
                currentTier1Size = 20
                currentTier2Size = 50
            }
            memoryClass >= 128 -> {
                // Low-end device
                currentTier1Size = 15
                currentTier2Size = 30
            }
            else -> {
                // Very low memory device
                currentTier1Size = 10
                currentTier2Size = 20
            }
        }

        // Resize LRU cache
        tier2LRUCache.resize(currentTier2Size)

        Log.i(TAG, "Adaptive cache sizing: memoryClass=$memoryClass MB, tier1=$currentTier1Size, tier2=$currentTier2Size")
    }

    /**
     * Get current memory level
     */
    fun getMemoryLevel(): MemoryLevel = currentMemoryLevel
}

/**
 * Memory pressure levels
 */
enum class MemoryLevel {
    NORMAL,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val cacheStatistics: CacheStatistics,
    val averageQueryTimeMs: Float,
    val totalQueries: Long,
    val memoryLevel: MemoryLevel,
    val tier1Size: Int,
    val tier2Size: Int,
    val tier1MaxSize: Int,
    val tier2MaxSize: Int
)

/**
 * Cache statistics data class
 */
data class CacheStatistics(
    val tier1Hits: Long,
    val tier2Hits: Long,
    val tier3Hits: Long,
    val cacheMisses: Long,
    val tier1HitRate: Float,
    val tier2HitRate: Float,
    val tier3HitRate: Float,
    val totalQueries: Long
)
