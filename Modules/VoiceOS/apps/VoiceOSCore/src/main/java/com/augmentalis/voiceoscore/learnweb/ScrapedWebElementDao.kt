/**
 * ScrapedWebElementDao.kt - SQLDelight repository for web element operations
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebElementDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 * Migrated to SQLDelight: 2025-12-17
 *
 * Repository for scraped web element CRUD operations
 */

package com.augmentalis.voiceoscore.learnweb

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.web.ScrapedWebElementQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scraped Web Element Repository
 *
 * Repository for scraped web element operations using SQLDelight.
 * Supports hierarchy queries and element deduplication.
 *
 * @since 1.0.0
 */
class ScrapedWebElementDao(private val database: VoiceOSDatabase) {

    private val queries: ScrapedWebElementQueries = database.scrapedWebElementQueries

    /**
     * Insert or replace element
     *
     * @param element Element to insert
     * @return Row ID
     */
    suspend fun insert(element: ScrapedWebElement): Long = withContext(Dispatchers.IO) {
        queries.insertScrapedWebElementAuto(
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
        queries.getByElementHash(element.elementHash).executeAsOneOrNull()?.id ?: 0L
    }

    /**
     * Insert multiple elements
     *
     * @param elements Elements to insert
     * @return List of row IDs
     */
    suspend fun insertAll(elements: List<ScrapedWebElement>): List<Long> = withContext(Dispatchers.IO) {
        elements.map { insert(it) }
    }

    /**
     * Get all elements for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return List of elements
     */
    suspend fun getByWebsiteUrlHash(websiteUrlHash: String): List<ScrapedWebElement> = withContext(Dispatchers.IO) {
        queries.getByWebsiteUrlHash(websiteUrlHash).executeAsList().map { mapToScrapedWebElement(it) }
    }

    /**
     * Get element by hash
     *
     * @param elementHash Element hash
     * @return Element or null
     */
    suspend fun getByElementHash(elementHash: String): ScrapedWebElement? = withContext(Dispatchers.IO) {
        queries.getByElementHash(elementHash).executeAsOneOrNull()?.let { mapToScrapedWebElement(it) }
    }

    /**
     * Get element by ID
     *
     * @param elementId Element ID (primary key)
     * @return Element or null
     */
    suspend fun getElementById(elementId: Long): ScrapedWebElement? = withContext(Dispatchers.IO) {
        queries.getElementById(elementId).executeAsOneOrNull()?.let { mapToScrapedWebElement(it) }
    }

    /**
     * Get child elements (by parent element hash)
     *
     * @param parentElementHash Parent element hash
     * @return List of child elements
     */
    suspend fun getChildren(parentElementHash: String): List<ScrapedWebElement> = withContext(Dispatchers.IO) {
        queries.getChildren(parentElementHash).executeAsList().map { mapToScrapedWebElement(it) }
    }

    /**
     * Get clickable elements for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return List of clickable elements
     */
    suspend fun getClickableElements(websiteUrlHash: String): List<ScrapedWebElement> = withContext(Dispatchers.IO) {
        queries.getClickableElements(websiteUrlHash).executeAsList().map { mapToScrapedWebElement(it) }
    }

    /**
     * Get elements by tag name
     *
     * @param websiteUrlHash Website URL hash
     * @param tagName Tag name (e.g., "BUTTON", "A")
     * @return List of elements
     */
    suspend fun getByTagName(websiteUrlHash: String, tagName: String): List<ScrapedWebElement> = withContext(Dispatchers.IO) {
        queries.getByTagName(websiteUrlHash, tagName).executeAsList().map { mapToScrapedWebElement(it) }
    }

    /**
     * Search elements by text
     *
     * @param websiteUrlHash Website URL hash
     * @param searchText Search text (case-insensitive)
     * @return List of matching elements
     */
    suspend fun searchByText(websiteUrlHash: String, searchText: String): List<ScrapedWebElement> = withContext(Dispatchers.IO) {
        queries.searchByText(websiteUrlHash, searchText).executeAsList().map { mapToScrapedWebElement(it) }
    }

    /**
     * Delete all elements for a website
     *
     * @param websiteUrlHash Website URL hash
     */
    suspend fun deleteByWebsiteUrlHash(websiteUrlHash: String) = withContext(Dispatchers.IO) {
        queries.deleteByWebsiteUrlHash(websiteUrlHash)
    }

    /**
     * Delete all elements
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        queries.deleteAll()
    }

    /**
     * Get element count for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Element count
     */
    suspend fun getElementCount(websiteUrlHash: String): Int = withContext(Dispatchers.IO) {
        queries.getElementCount(websiteUrlHash).executeAsOne().toInt()
    }

    /**
     * Get clickable element count for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Clickable element count
     */
    suspend fun getClickableElementCount(websiteUrlHash: String): Int = withContext(Dispatchers.IO) {
        queries.getClickableElementCount(websiteUrlHash).executeAsOne().toInt()
    }

    /**
     * Map SQLDelight result to ScrapedWebElement data class
     */
    private fun mapToScrapedWebElement(result: com.augmentalis.database.web.GetByWebsiteUrlHash): ScrapedWebElement {
        return ScrapedWebElement(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            tagName = result.tag_name,
            xpath = result.xpath,
            text = result.text,
            ariaLabel = result.aria_label,
            role = result.role,
            parentElementHash = result.parent_element_hash,
            clickable = result.clickable == 1L,
            visible = result.visible == 1L,
            bounds = result.bounds
        )
    }

    private fun mapToScrapedWebElement(result: com.augmentalis.database.web.GetByElementHash): ScrapedWebElement {
        return ScrapedWebElement(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            tagName = result.tag_name,
            xpath = result.xpath,
            text = result.text,
            ariaLabel = result.aria_label,
            role = result.role,
            parentElementHash = result.parent_element_hash,
            clickable = result.clickable == 1L,
            visible = result.visible == 1L,
            bounds = result.bounds
        )
    }

    private fun mapToScrapedWebElement(result: com.augmentalis.database.web.GetElementById): ScrapedWebElement {
        return ScrapedWebElement(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            tagName = result.tag_name,
            xpath = result.xpath,
            text = result.text,
            ariaLabel = result.aria_label,
            role = result.role,
            parentElementHash = result.parent_element_hash,
            clickable = result.clickable == 1L,
            visible = result.visible == 1L,
            bounds = result.bounds
        )
    }

    private fun mapToScrapedWebElement(result: com.augmentalis.database.web.GetChildren): ScrapedWebElement {
        return ScrapedWebElement(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            tagName = result.tag_name,
            xpath = result.xpath,
            text = result.text,
            ariaLabel = result.aria_label,
            role = result.role,
            parentElementHash = result.parent_element_hash,
            clickable = result.clickable == 1L,
            visible = result.visible == 1L,
            bounds = result.bounds
        )
    }

    private fun mapToScrapedWebElement(result: com.augmentalis.database.web.GetClickableElements): ScrapedWebElement {
        return ScrapedWebElement(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            tagName = result.tag_name,
            xpath = result.xpath,
            text = result.text,
            ariaLabel = result.aria_label,
            role = result.role,
            parentElementHash = result.parent_element_hash,
            clickable = result.clickable == 1L,
            visible = result.visible == 1L,
            bounds = result.bounds
        )
    }

    private fun mapToScrapedWebElement(result: com.augmentalis.database.web.GetByTagName): ScrapedWebElement {
        return ScrapedWebElement(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            tagName = result.tag_name,
            xpath = result.xpath,
            text = result.text,
            ariaLabel = result.aria_label,
            role = result.role,
            parentElementHash = result.parent_element_hash,
            clickable = result.clickable == 1L,
            visible = result.visible == 1L,
            bounds = result.bounds
        )
    }

    private fun mapToScrapedWebElement(result: com.augmentalis.database.web.SearchByText): ScrapedWebElement {
        return ScrapedWebElement(
            id = result.id,
            websiteUrlHash = result.website_url_hash,
            elementHash = result.element_hash,
            tagName = result.tag_name,
            xpath = result.xpath,
            text = result.text,
            ariaLabel = result.aria_label,
            role = result.role,
            parentElementHash = result.parent_element_hash,
            clickable = result.clickable == 1L,
            visible = result.visible == 1L,
            bounds = result.bounds
        )
    }
}
