/**
 * WebCommandCache.kt - Hybrid Smart cache manager for web commands
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/WebCommandCache.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Hybrid Smart caching implementation with 24-hour TTL, background refresh, and hierarchy tracking
 */

package com.augmentalis.voiceoscore.learnweb

import android.util.Log
import kotlinx.coroutines.*
import java.net.URL
import java.security.MessageDigest

/**
 * Web Command Cache
 *
 * Implements Hybrid Smart caching strategy:
 * - 24-hour TTL for cache entries
 * - 12-hour staleness threshold for background refresh
 * - Structure hash-based invalidation
 * - Parent-child hierarchy tracking
 * - URL change detection
 *
 * @property database Web scraping database instance
 *
 * @since 1.0.0
 */
class WebCommandCache(private val database: WebScrapingDatabase) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "WebCommandCache"

        /**
         * Cache TTL: 24 hours in milliseconds
         */
        const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L

        /**
         * Stale threshold: 12 hours in milliseconds
         */
        const val STALE_THRESHOLD_MS = 12 * 60 * 60 * 1000L
    }

    /**
     * Get commands for a URL
     *
     * Returns cached commands if available and fresh.
     * If cache is stale (> 12 hours), returns commands and triggers background refresh.
     * If cache miss or expired (> 24 hours), returns Miss.
     *
     * @param url Full URL
     * @return Cache result (Hit, Stale, or Miss)
     */
    suspend fun getCommands(url: String): CacheResult {
        val urlHash = hashURL(url)
        val now = System.currentTimeMillis()

        val website = database.scrapedWebsiteDao().getByUrlHash(urlHash)

        return when {
            website == null -> {
                Log.d(TAG, "Cache MISS for $url")
                CacheResult.Miss
            }

            (now - website.scrapedAt) > CACHE_TTL_MS -> {
                Log.d(TAG, "Cache EXPIRED for $url (age: ${(now - website.scrapedAt) / 1000}s)")
                CacheResult.Miss
            }

            website.isStale || (now - website.scrapedAt) > STALE_THRESHOLD_MS -> {
                Log.d(TAG, "Cache STALE for $url, returning cached + background refresh")

                // Update access metadata
                database.scrapedWebsiteDao().updateAccessMetadata(
                    urlHash,
                    now,
                    website.accessCount + 1
                )

                // Trigger background refresh
                scope.launch {
                    try {
                        refreshStaleCache(url)
                    } catch (e: Exception) {
                        Log.e(TAG, "Background refresh failed for $url", e)
                    }
                }

                // Return stale commands
                val commands = database.generatedWebCommandDao().getByWebsiteUrlHash(urlHash)
                CacheResult.Stale(commands)
            }

            else -> {
                Log.d(TAG, "Cache HIT for $url (age: ${(now - website.scrapedAt) / 1000}s)")

                // Update access metadata
                database.scrapedWebsiteDao().updateAccessMetadata(
                    urlHash,
                    now,
                    website.accessCount + 1
                )

                val commands = database.generatedWebCommandDao().getByWebsiteUrlHash(urlHash)
                CacheResult.Hit(commands)
            }
        }
    }

    /**
     * Refresh stale cache
     *
     * Called in background to refresh stale cache entries.
     * Must be implemented by caller to trigger actual scraping.
     *
     * @param url Full URL
     */
    suspend fun refreshStaleCache(url: String) {
        val urlHash = hashURL(url)
        database.scrapedWebsiteDao().markAsStale(urlHash)

        // NOTE: Caller must implement actual scraping logic
        // This is a hook for external scraping trigger
        Log.d(TAG, "Marked $url as stale, awaiting rescrape")
    }

    /**
     * Invalidate cache by URL change
     *
     * Called when URL changes (e.g., navigation within single-page app).
     * Marks old cache as stale and creates new cache entry relationship.
     *
     * @param oldUrl Previous URL
     * @param newUrl New URL
     */
    suspend fun invalidateByUrlChange(oldUrl: String, newUrl: String) {
        val oldUrlHash = hashURL(oldUrl)
        val newUrlHash = hashURL(newUrl)

        // Mark old URL as stale
        database.scrapedWebsiteDao().markAsStale(oldUrlHash)

        // Check if new URL exists
        val existingWebsite = database.scrapedWebsiteDao().getByUrlHash(newUrlHash)
        if (existingWebsite != null) {
            Log.d(TAG, "URL changed from $oldUrl to $newUrl (cached)")
        } else {
            Log.d(TAG, "URL changed from $oldUrl to $newUrl (needs scraping)")
        }
    }

    /**
     * Invalidate cache by structure change
     *
     * Called when DOM structure changes significantly.
     * Updates structure hash and marks cache for refresh.
     *
     * @param url Full URL
     * @param newStructureHash New structure hash
     */
    suspend fun invalidateByStructureChange(url: String, newStructureHash: String) {
        val urlHash = hashURL(url)
        val website = database.scrapedWebsiteDao().getByUrlHash(urlHash)

        if (website != null && website.structureHash != newStructureHash) {
            Log.d(TAG, "Structure changed for $url, invalidating cache")

            // Delete old elements and commands
            database.scrapedWebElementDao().deleteByWebsiteUrlHash(urlHash)
            database.generatedWebCommandDao().deleteByWebsiteUrlHash(urlHash)

            // Update structure hash
            database.scrapedWebsiteDao().updateStructureHash(
                urlHash,
                newStructureHash,
                System.currentTimeMillis()
            )
        }
    }

    /**
     * Store website with elements and commands
     *
     * @param website Website metadata
     * @param elements List of elements
     * @param commands List of commands
     */
    suspend fun store(
        website: ScrapedWebsite,
        elements: List<ScrapedWebElement>,
        commands: List<GeneratedWebCommand>
    ) {
        database.scrapedWebsiteDao().insert(website)
        database.scrapedWebElementDao().insertAll(elements)
        database.generatedWebCommandDao().insertAll(commands)

        Log.d(TAG, "Stored ${elements.size} elements and ${commands.size} commands for ${website.url}")
    }

    /**
     * Get all stale websites for background refresh
     *
     * @return List of stale websites
     */
    suspend fun getStaleWebsites(): List<ScrapedWebsite> {
        val threshold = System.currentTimeMillis() - STALE_THRESHOLD_MS
        return database.scrapedWebsiteDao().getStaleWebsites(threshold)
    }

    /**
     * Get cache statistics
     *
     * @return Cache statistics
     */
    suspend fun getCacheStats(): WebCacheStats {
        val stats = database.scrapedWebsiteDao().getCacheStats()

        return WebCacheStats(
            totalWebsites = stats.total,
            staleWebsites = stats.stale,
            freshWebsites = stats.total - stats.stale,
            averageAccessCount = stats.avg_access
        )
    }

    /**
     * Clear all cache
     */
    suspend fun clearAll() {
        database.scrapedWebsiteDao().deleteAll()
        database.scrapedWebElementDao().deleteAll()
        database.generatedWebCommandDao().deleteAll()
        Log.d(TAG, "Cleared all cache")
    }

    /**
     * Hash URL for storage
     *
     * Normalizes URL to protocol://host/path and generates SHA-256 hash.
     *
     * @param url Full URL
     * @return SHA-256 hash
     */
    fun hashURL(url: String): String {
        return try {
            val normalizedUrl = URL(url).run {
                "$protocol://$host$path"
            }
            hashString(normalizedUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hash URL: $url", e)
            hashString(url)
        }
    }

    /**
     * Hash structure for invalidation detection
     *
     * @param elements List of elements
     * @return Structure hash
     */
    fun hashStructure(elements: List<ScrapedWebElement>): String {
        val structureString = elements
            .sortedBy { it.xpath }
            .joinToString("|") { "${it.tagName}:${it.xpath}" }
        return hashString(structureString)
    }

    /**
     * Generate SHA-256 hash of string
     *
     * @param input Input string
     * @return SHA-256 hash
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Extract domain from URL
     *
     * @param url Full URL
     * @return Domain name
     */
    fun extractDomain(url: String): String {
        return try {
            URL(url).host
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract domain from: $url", e)
            "unknown"
        }
    }

    /**
     * Cancel all background operations
     */
    fun close() {
        scope.cancel()
    }
}

/**
 * Cache Result
 *
 * Sealed class representing cache lookup results.
 */
sealed class CacheResult {
    /**
     * Cache hit - fresh data available
     *
     * @property commands List of commands
     */
    data class Hit(val commands: List<GeneratedWebCommand>) : CacheResult()

    /**
     * Cache stale - data available but needs refresh
     *
     * @property commands List of commands (stale)
     */
    data class Stale(val commands: List<GeneratedWebCommand>) : CacheResult()

    /**
     * Cache miss - no data available, scraping required
     */
    object Miss : CacheResult()
}

/**
 * Web Cache Statistics
 *
 * @property totalWebsites Total cached websites
 * @property staleWebsites Number of stale websites
 * @property freshWebsites Number of fresh websites
 * @property averageAccessCount Average access count per website
 */
data class WebCacheStats(
    val totalWebsites: Int,
    val staleWebsites: Int,
    val freshWebsites: Int,
    val averageAccessCount: Double
)
