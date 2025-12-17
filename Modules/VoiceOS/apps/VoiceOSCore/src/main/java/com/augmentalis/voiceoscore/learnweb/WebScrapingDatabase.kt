/**
 * WebScrapingDatabase.kt - Room database for LearnWeb
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/WebScrapingDatabase.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Room database definition for LearnWeb persistent storage with Hybrid Smart caching
 */

package com.augmentalis.voiceoscore.learnweb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Web Scraping Database
 *
 * Room database for persistent storage of scraped website data with Hybrid Smart caching.
 * Implements 24-hour TTL, background refresh, and hierarchy tracking.
 *
 * @property scrapedWebsiteDao DAO for website operations
 * @property scrapedWebElementDao DAO for element operations
 * @property generatedWebCommandDao DAO for command operations
 *
 * @since 1.0.0
 */
@Database(
    entities = [
        ScrapedWebsite::class,
        ScrapedWebElement::class,
        GeneratedWebCommand::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WebScrapingDatabase : RoomDatabase() {

    /**
     * DAO for website operations
     */
    abstract fun scrapedWebsiteDao(): ScrapedWebsiteDao

    /**
     * DAO for web element operations
     */
    abstract fun scrapedWebElementDao(): ScrapedWebElementDao

    /**
     * DAO for generated command operations
     */
    abstract fun generatedWebCommandDao(): GeneratedWebCommandDao

    companion object {
        /**
         * Database name
         */
        private const val DATABASE_NAME = "web_scraping_database"

        /**
         * Singleton instance
         */
        @Volatile
        private var INSTANCE: WebScrapingDatabase? = null

        /**
         * Get database instance (singleton)
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): WebScrapingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Build database instance
         *
         * @param context Application context
         * @return Database instance
         */
        private fun buildDatabase(context: Context): WebScrapingDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WebScrapingDatabase::class.java,
                DATABASE_NAME
            )
            // MIGRATION REQUIRED: Implement migrations before schema changes
            .build()
        }

        /**
         * Clear database instance (for testing)
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }
    }
}

/**
 * Scraped Website Entity
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
@androidx.room.Entity(tableName = "scraped_websites")
data class ScrapedWebsite(
    @androidx.room.PrimaryKey
    @androidx.room.ColumnInfo(name = "url_hash")
    val urlHash: String,

    @androidx.room.ColumnInfo(name = "url")
    val url: String,

    @androidx.room.ColumnInfo(name = "domain")
    val domain: String,

    @androidx.room.ColumnInfo(name = "title")
    val title: String,

    @androidx.room.ColumnInfo(name = "structure_hash")
    val structureHash: String,

    @androidx.room.ColumnInfo(name = "parent_url_hash")
    val parentUrlHash: String?,

    @androidx.room.ColumnInfo(name = "scraped_at")
    val scrapedAt: Long,

    @androidx.room.ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: Long,

    @androidx.room.ColumnInfo(name = "access_count")
    val accessCount: Int,

    @androidx.room.ColumnInfo(name = "is_stale")
    val isStale: Boolean = false
)

/**
 * Scraped Web Element Entity
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
@androidx.room.Entity(
    tableName = "scraped_web_elements",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ScrapedWebsite::class,
            parentColumns = ["url_hash"],
            childColumns = ["website_url_hash"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("website_url_hash"),
        androidx.room.Index("element_hash")
    ]
)
data class ScrapedWebElement(
    @androidx.room.PrimaryKey(autoGenerate = true)
    @androidx.room.ColumnInfo(name = "id")
    val id: Long = 0,

    @androidx.room.ColumnInfo(name = "website_url_hash")
    val websiteUrlHash: String,

    @androidx.room.ColumnInfo(name = "element_hash")
    val elementHash: String,

    @androidx.room.ColumnInfo(name = "tag_name")
    val tagName: String,

    @androidx.room.ColumnInfo(name = "xpath")
    val xpath: String,

    @androidx.room.ColumnInfo(name = "text")
    val text: String?,

    @androidx.room.ColumnInfo(name = "aria_label")
    val ariaLabel: String?,

    @androidx.room.ColumnInfo(name = "role")
    val role: String?,

    @androidx.room.ColumnInfo(name = "parent_element_hash")
    val parentElementHash: String?,

    @androidx.room.ColumnInfo(name = "clickable")
    val clickable: Boolean,

    @androidx.room.ColumnInfo(name = "visible")
    val visible: Boolean,

    @androidx.room.ColumnInfo(name = "bounds")
    val bounds: String  // JSON: {x, y, width, height}
)

/**
 * Generated Web Command Entity
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
@androidx.room.Entity(
    tableName = "generated_web_commands",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = ScrapedWebsite::class,
            parentColumns = ["url_hash"],
            childColumns = ["website_url_hash"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("website_url_hash"),
        androidx.room.Index("element_hash")
    ]
)
data class GeneratedWebCommand(
    @androidx.room.PrimaryKey(autoGenerate = true)
    @androidx.room.ColumnInfo(name = "id")
    val id: Long = 0,

    @androidx.room.ColumnInfo(name = "website_url_hash")
    val websiteUrlHash: String,

    @androidx.room.ColumnInfo(name = "element_hash")
    val elementHash: String,

    @androidx.room.ColumnInfo(name = "command_text")
    val commandText: String,

    @androidx.room.ColumnInfo(name = "synonyms")
    val synonyms: String,

    @androidx.room.ColumnInfo(name = "action")
    val action: String,

    @androidx.room.ColumnInfo(name = "xpath")
    val xpath: String,

    @androidx.room.ColumnInfo(name = "generated_at")
    val generatedAt: Long,

    @androidx.room.ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    @androidx.room.ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long? = null
)
