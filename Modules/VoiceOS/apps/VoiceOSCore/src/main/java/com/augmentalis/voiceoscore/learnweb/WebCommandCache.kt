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
import com.augmentalis.database.VoiceOSDatabaseManager
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
 * @property databaseManager VoiceOS database manager instance
 *
 * @since 1.0.0
 */
class WebCommandCache(private val databaseManager: VoiceOSDatabaseManager) {

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

        val website = databaseManager.scrapedWebsiteQueries.getByUrlHash(urlHash).executeAsOneOrNull()

        return when {
            website == null -> {
                Log.d(TAG, "Cache MISS for $url")
                CacheResult.Miss
            }

            (now - website.scraped_at) > CACHE_TTL_MS -> {
                Log.d(TAG, "Cache EXPIRED for $url (age: ${(now - website.scraped_at) / 1000}s)")
                CacheResult.Miss
            }

            (website.is_stale != 0L) || (now - website.scraped_at) > STALE_THRESHOLD_MS -> {
                Log.d(TAG, "Cache STALE for $url, returning cached + background refresh")

                // Update access metadata
                databaseManager.scrapedWebsiteQueries.updateAccessMetadata(
                    last_accessed_at = now,
                    access_count = website.access_count + 1,
                    url_hash = urlHash
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
                val commands = databaseManager.generatedWebCommandQueries.getByWebsiteUrlHash(urlHash)
                    .executeAsList()
                    .map { it.toGeneratedWebCommand() }
                CacheResult.Stale(commands)
            }

            else -> {
                Log.d(TAG, "Cache HIT for $url (age: ${(now - website.scraped_at) / 1000}s)")

                // Update access metadata
                databaseManager.scrapedWebsiteQueries.updateAccessMetadata(
                    last_accessed_at = now,
                    access_count = website.access_count + 1,
                    url_hash = urlHash
                )

                val commands = databaseManager.generatedWebCommandQueries.getByWebsiteUrlHash(urlHash)
                    .executeAsList()
                    .map { it.toGeneratedWebCommand() }
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
        databaseManager.scrapedWebsiteQueries.markAsStale(urlHash)

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
        databaseManager.scrapedWebsiteQueries.markAsStale(oldUrlHash)

        // Check if new URL exists
        val existingWebsite = databaseManager.scrapedWebsiteQueries.getByUrlHash(newUrlHash).executeAsOneOrNull()
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
        val website = databaseManager.scrapedWebsiteQueries.getByUrlHash(urlHash).executeAsOneOrNull()

        if (website != null && website.structure_hash != newStructureHash) {
            Log.d(TAG, "Structure changed for $url, invalidating cache")

            // Delete old elements and commands
            databaseManager.scrapedWebElementQueries.deleteByWebsiteUrlHash(urlHash)
            databaseManager.generatedWebCommandQueries.deleteByWebsiteUrlHash(urlHash)

            // Update structure hash
            databaseManager.scrapedWebsiteQueries.updateStructureHash(
                structure_hash = newStructureHash,
                scraped_at = System.currentTimeMillis(),
                url_hash = urlHash
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
        databaseManager.scrapedWebsiteQueries.insertScrapedWebsite(
            url_hash = website.urlHash,
            url = website.url,
            domain = website.domain,
            title = website.title,
            structure_hash = website.structureHash,
            parent_url_hash = website.parentUrlHash,
            scraped_at = website.scrapedAt,
            last_accessed_at = website.lastAccessedAt,
            access_count = website.accessCount.toLong(),
            is_stale = if (website.isStale) 1L else 0L
        )

        elements.forEach { element ->
            databaseManager.scrapedWebElementQueries.insertScrapedWebElementAuto(
                website_url_hash = element.websiteUrlHash,
                element_hash = element.elementHash,
                tag_name = element.tagName,
                xpath = element.xpath,
                text = element.text,
                aria_label = element.ariaLabel,
                role = element.role,
                parent_element_hash = element.parentElementHash,
                clickable = if (element.clickable) 1L else 0L,
                visible = if (element.visible) 1L else 0L,
                bounds = element.bounds
            )
        }

        commands.forEach { command ->
            databaseManager.generatedWebCommandQueries.insertGeneratedWebCommandAuto(
                website_url_hash = command.websiteUrlHash,
                element_hash = command.elementHash,
                command_text = command.commandText,
                synonyms = command.synonyms,
                action = command.action,
                xpath = command.xpath,
                generated_at = command.generatedAt,
                usage_count = command.usageCount.toLong(),
                last_used_at = command.lastUsedAt
            )
        }

        Log.d(TAG, "Stored ${elements.size} elements and ${commands.size} commands for ${website.url}")
    }

    /**
     * Get all stale websites for background refresh
     *
     * @return List of stale websites
     */
    suspend fun getStaleWebsites(): List<ScrapedWebsite> {
        val threshold = System.currentTimeMillis() - STALE_THRESHOLD_MS
        return databaseManager.scrapedWebsiteQueries.getStaleWebsites(threshold)
            .executeAsList()
            .map { it.toScrapedWebsite() }
    }

    /**
     * Get cache statistics
     *
     * @return Cache statistics
     */
    suspend fun getCacheStats(): WebCacheStats {
        val stats = databaseManager.scrapedWebsiteQueries.getCacheStats().executeAsOne()

        return WebCacheStats(
            totalWebsites = stats.total.toInt(),
            staleWebsites = stats.stale?.toInt() ?: 0,
            freshWebsites = stats.total.toInt() - (stats.stale?.toInt() ?: 0),
            averageAccessCount = stats.avg_access ?: 0.0
        )
    }

    /**
     * Clear all cache
     */
    suspend fun clearAll() {
        databaseManager.scrapedWebsiteQueries.deleteAll()
        databaseManager.scrapedWebElementQueries.deleteAll()
        databaseManager.generatedWebCommandQueries.deleteAll()
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

/**
 * Extension functions to convert between SQLDelight types and domain models
 */

fun com.augmentalis.database.web.Scraped_websites.toScrapedWebsite() = ScrapedWebsite(
    urlHash = url_hash,
    url = url,
    domain = domain,
    title = title,
    structureHash = structure_hash,
    parentUrlHash = parent_url_hash,
    scrapedAt = scraped_at,
    lastAccessedAt = last_accessed_at,
    accessCount = access_count.toInt(),
    isStale = is_stale != 0L
)

fun com.augmentalis.database.web.Scraped_web_elements.toScrapedWebElement() = ScrapedWebElement(
    id = id,
    websiteUrlHash = website_url_hash,
    elementHash = element_hash,
    tagName = tag_name,
    xpath = xpath,
    text = text,
    ariaLabel = aria_label,
    role = role,
    parentElementHash = parent_element_hash,
    clickable = clickable != 0L,
    visible = visible != 0L,
    bounds = bounds
)

fun com.augmentalis.database.web.Generated_web_commands.toGeneratedWebCommand() = GeneratedWebCommand(
    id = id,
    websiteUrlHash = website_url_hash,
    elementHash = element_hash,
    commandText = command_text,
    synonyms = synonyms,
    action = action,
    xpath = xpath,
    generatedAt = generated_at,
    usageCount = usage_count.toInt(),
    lastUsedAt = last_used_at
)
