/**
 * WebScrapingDatabase.kt - SQLDelight data models for LearnWeb
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/WebScrapingDatabase.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 * Migrated to SQLDelight: 2025-12-17
 *
 * Data models for LearnWeb persistent storage with Hybrid Smart caching
 * Uses VoiceOSDatabase from core/database module
 */

package com.augmentalis.voiceoscore.learnweb

/**
 * Scraped Website Data Model
 *
 * Represents a scraped website with hierarchy tracking and cache metadata.
 *
 * @property urlHash SHA-256 hash of URL (primary key)
 * @property url Full URL
 * @property domain Domain name (e.g., "google.com")
 * @property title Page title
 * @property structureHash Hash of DOM structure for invalidation detection
 * @property parentUrlHash Parent URL hash for hierarchy tracking (nullable)
 * @property scrapedAt Timestamp when scraped
 * @property lastAccessedAt Timestamp of last access
 * @property accessCount Number of times accessed
 * @property isStale Whether cache is stale (> 12 hours)
 */
data class ScrapedWebsite(
    val urlHash: String,
    val url: String,
    val domain: String,
    val title: String,
    val structureHash: String,
    val parentUrlHash: String?,
    val scrapedAt: Long,
    val lastAccessedAt: Long,
    val accessCount: Int,
    val isStale: Boolean = false
)

/**
 * Scraped Web Element Data Model
 *
 * Represents a DOM element with hierarchy tracking.
 *
 * @property id Auto-generated primary key
 * @property websiteUrlHash Foreign key to scraped_websites
 * @property elementHash Hash of element for deduplication
 * @property tagName HTML tag name (e.g., "BUTTON", "A", "INPUT")
 * @property xpath XPath selector for element
 * @property text Visible text content (truncated to 100 chars)
 * @property ariaLabel ARIA label for accessibility
 * @property role ARIA role
 * @property parentElementHash Parent element hash for hierarchy
 * @property clickable Whether element is clickable
 * @property visible Whether element is visible
 * @property bounds JSON string of bounding rect: {x, y, width, height}
 */
data class ScrapedWebElement(
    val id: Long = 0,
    val websiteUrlHash: String,
    val elementHash: String,
    val tagName: String,
    val xpath: String,
    val text: String?,
    val ariaLabel: String?,
    val role: String?,
    val parentElementHash: String?,
    val clickable: Boolean,
    val visible: Boolean,
    val bounds: String  // JSON: {x, y, width, height}
)

/**
 * Generated Web Command Data Model
 *
 * Represents a generated voice command for a web element.
 *
 * @property id Auto-generated primary key
 * @property websiteUrlHash Foreign key to scraped_websites
 * @property elementHash Hash of associated element
 * @property commandText Primary command text (e.g., "Click login button")
 * @property synonyms Comma-separated synonyms (e.g., "sign in,log in")
 * @property action Action type (CLICK, SCROLL_TO, FOCUS, etc.)
 * @property xpath XPath for executing action
 * @property generatedAt Timestamp when generated
 * @property usageCount Number of times used
 * @property lastUsedAt Timestamp of last usage (nullable)
 */
data class GeneratedWebCommand(
    val id: Long = 0,
    val websiteUrlHash: String,
    val elementHash: String,
    val commandText: String,
    val synonyms: String,
    val action: String,
    val xpath: String,
    val generatedAt: Long,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null
)

/**
 * Cache statistics data class
 */
data class CacheStats(
    val total: Long,
    val stale: Long?,
    val avgAccess: Double?
)
