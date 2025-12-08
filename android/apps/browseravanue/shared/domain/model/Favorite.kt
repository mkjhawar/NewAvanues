package com.augmentalis.browseravanue.domain.model

import java.util.UUID

/**
 * Domain model for browser Favorite/Bookmark
 * Pure Kotlin, no Android/Room dependencies
 *
 * Following SOLID principles:
 * - Single Responsibility: Represents a favorite/bookmark only
 * - Open/Closed: Immutable data class
 */
data class Favorite(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val favicon: String? = null,
    val folder: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 0,
    val lastVisited: Long? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null
) {
    /**
     * Get display title with fallback to URL
     */
    fun getDisplayTitle(): String {
        return title.ifBlank {
            url.let { url ->
                if (url.length > 30) url.take(27) + "..."
                else url
            }
        }
    }

    /**
     * Extract domain from URL
     */
    fun getDomain(): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Check if URL is HTTPS
     */
    fun isSecure(): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }

    /**
     * Check if favorite has been visited
     */
    fun hasBeenVisited(): Boolean {
        return visitCount > 0 && lastVisited != null
    }

    /**
     * Check if favorite is recently visited (within 7 days)
     */
    fun isRecentlyVisited(): Boolean {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return lastVisited?.let { it > sevenDaysAgo } ?: false
    }

    /**
     * Check if favorite has specific tag
     */
    fun hasTag(tag: String): Boolean {
        return tags.any { it.equals(tag, ignoreCase = true) }
    }

    /**
     * Get age of favorite in milliseconds
     */
    fun getAge(): Long {
        return System.currentTimeMillis() - createdAt
    }

    /**
     * Get days since last visit
     */
    fun getDaysSinceLastVisit(): Long? {
        return lastVisited?.let {
            (System.currentTimeMillis() - it) / (24 * 60 * 60 * 1000)
        }
    }

    /**
     * Record a visit to this favorite
     */
    fun recordVisit(): Favorite {
        return copy(
            visitCount = visitCount + 1,
            lastVisited = System.currentTimeMillis()
        )
    }

    /**
     * Update title
     */
    fun updateTitle(newTitle: String): Favorite {
        return copy(title = newTitle)
    }

    /**
     * Update favicon
     */
    fun updateFavicon(newFavicon: String?): Favorite {
        return copy(favicon = newFavicon)
    }

    /**
     * Move to folder
     */
    fun moveToFolder(newFolder: String?): Favorite {
        return copy(folder = newFolder)
    }

    /**
     * Add tag
     */
    fun addTag(tag: String): Favorite {
        return if (!hasTag(tag)) {
            copy(tags = tags + tag)
        } else {
            this
        }
    }

    /**
     * Remove tag
     */
    fun removeTag(tag: String): Favorite {
        return copy(tags = tags.filter { !it.equals(tag, ignoreCase = true) })
    }

    /**
     * Update notes
     */
    fun updateNotes(newNotes: String?): Favorite {
        return copy(notes = newNotes)
    }

    companion object {
        /**
         * Create a favorite from a Tab
         */
        fun fromTab(tab: Tab): Favorite {
            return Favorite(
                url = tab.url,
                title = tab.title ?: tab.getDisplayTitle(),
                favicon = tab.favicon
            )
        }

        /**
         * Create a favorite with minimal info
         */
        fun create(url: String, title: String): Favorite {
            return Favorite(url = url, title = title)
        }

        /**
         * Create a favorite in a specific folder
         */
        fun createInFolder(url: String, title: String, folder: String): Favorite {
            return Favorite(url = url, title = title, folder = folder)
        }
    }
}
