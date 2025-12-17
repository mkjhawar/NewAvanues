/**
 * ScrapedWebElementDao.kt - DAO for web element operations
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebElementDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Data Access Object for scraped web element CRUD operations
 */

package com.augmentalis.voiceoscore.learnweb

import androidx.room.*

/**
 * Scraped Web Element DAO
 *
 * Data Access Object for scraped web element operations.
 * Supports hierarchy queries and element deduplication.
 *
 * @since 1.0.0
 */
@Dao
interface ScrapedWebElementDao {

    /**
     * Insert or replace element
     *
     * @param element Element to insert
     * @return Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: ScrapedWebElement): Long

    /**
     * Insert multiple elements
     *
     * @param elements Elements to insert
     * @return List of row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(elements: List<ScrapedWebElement>): List<Long>

    /**
     * Get all elements for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return List of elements
     */
    @Query("SELECT * FROM scraped_web_elements WHERE website_url_hash = :websiteUrlHash")
    suspend fun getByWebsiteUrlHash(websiteUrlHash: String): List<ScrapedWebElement>

    /**
     * Get element by hash
     *
     * @param elementHash Element hash
     * @return Element or null
     */
    @Query("SELECT * FROM scraped_web_elements WHERE element_hash = :elementHash LIMIT 1")
    suspend fun getByElementHash(elementHash: String): ScrapedWebElement?

    /**
     * Get element by ID
     *
     * @param elementId Element ID (primary key)
     * @return Element or null
     */
    @Query("SELECT * FROM scraped_web_elements WHERE id = :elementId LIMIT 1")
    suspend fun getElementById(elementId: Long): ScrapedWebElement?

    /**
     * Get child elements (by parent element hash)
     *
     * @param parentElementHash Parent element hash
     * @return List of child elements
     */
    @Query("SELECT * FROM scraped_web_elements WHERE parent_element_hash = :parentElementHash")
    suspend fun getChildren(parentElementHash: String): List<ScrapedWebElement>

    /**
     * Get clickable elements for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return List of clickable elements
     */
    @Query("SELECT * FROM scraped_web_elements WHERE website_url_hash = :websiteUrlHash AND clickable = 1 AND visible = 1")
    suspend fun getClickableElements(websiteUrlHash: String): List<ScrapedWebElement>

    /**
     * Get elements by tag name
     *
     * @param websiteUrlHash Website URL hash
     * @param tagName Tag name (e.g., "BUTTON", "A")
     * @return List of elements
     */
    @Query("SELECT * FROM scraped_web_elements WHERE website_url_hash = :websiteUrlHash AND tag_name = :tagName")
    suspend fun getByTagName(websiteUrlHash: String, tagName: String): List<ScrapedWebElement>

    /**
     * Search elements by text
     *
     * @param websiteUrlHash Website URL hash
     * @param searchText Search text (case-insensitive)
     * @return List of matching elements
     */
    @Query("SELECT * FROM scraped_web_elements WHERE website_url_hash = :websiteUrlHash AND text LIKE '%' || :searchText || '%' COLLATE NOCASE")
    suspend fun searchByText(websiteUrlHash: String, searchText: String): List<ScrapedWebElement>

    /**
     * Delete all elements for a website
     *
     * @param websiteUrlHash Website URL hash
     */
    @Query("DELETE FROM scraped_web_elements WHERE website_url_hash = :websiteUrlHash")
    suspend fun deleteByWebsiteUrlHash(websiteUrlHash: String)

    /**
     * Delete all elements
     */
    @Query("DELETE FROM scraped_web_elements")
    suspend fun deleteAll()

    /**
     * Get element count for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Element count
     */
    @Query("SELECT COUNT(*) FROM scraped_web_elements WHERE website_url_hash = :websiteUrlHash")
    suspend fun getElementCount(websiteUrlHash: String): Int

    /**
     * Get clickable element count for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Clickable element count
     */
    @Query("SELECT COUNT(*) FROM scraped_web_elements WHERE website_url_hash = :websiteUrlHash AND clickable = 1 AND visible = 1")
    suspend fun getClickableElementCount(websiteUrlHash: String): Int
}
