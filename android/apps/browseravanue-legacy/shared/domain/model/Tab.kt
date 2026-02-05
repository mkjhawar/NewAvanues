package com.augmentalis.browseravanue.domain.model

import java.util.UUID

/**
 * Domain model for browser Tab
 * Pure Kotlin, no Android/Room dependencies
 *
 * Following SOLID principles:
 * - Single Responsibility: Represents a tab only
 * - Open/Closed: Immutable data class
 */
data class Tab(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String? = null,
    val favicon: String? = null,
    val isDesktopMode: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
) {
    /**
     * Get display title with fallback to URL
     * Truncates long URLs for UI display
     */
    fun getDisplayTitle(): String {
        return title ?: url.let { url ->
            if (url.length > 30) url.take(27) + "..."
            else url
        }
    }

    /**
     * Extract domain from URL
     * Returns domain or full URL if extraction fails
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
     * Get short URL for compact display
     * Shows domain only
     */
    fun getShortUrl(): String {
        return getDomain()
    }

    /**
     * Check if URL is HTTPS
     */
    fun isSecure(): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }

    /**
     * Check if tab is active (recently accessed)
     * Within last 5 minutes
     */
    fun isRecentlyActive(): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return lastAccessed > fiveMinutesAgo
    }

    /**
     * Get age of tab in milliseconds
     */
    fun getAge(): Long {
        return System.currentTimeMillis() - createdAt
    }

    /**
     * Get time since last access in milliseconds
     */
    fun getTimeSinceLastAccess(): Long {
        return System.currentTimeMillis() - lastAccessed
    }

    /**
     * Update last accessed timestamp
     */
    fun markAccessed(): Tab {
        return copy(lastAccessed = System.currentTimeMillis())
    }

    /**
     * Update loading state
     */
    fun setLoading(loading: Boolean): Tab {
        return copy(isLoading = loading)
    }

    /**
     * Update navigation state
     */
    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean): Tab {
        return copy(
            canGoBack = canGoBack,
            canGoForward = canGoForward
        )
    }

    /**
     * Update page info
     */
    fun updatePageInfo(url: String, title: String? = null, favicon: String? = null): Tab {
        return copy(
            url = url,
            title = title ?: this.title,
            favicon = favicon ?: this.favicon
        )
    }

    companion object {
        /**
         * Create a new tab with default URL
         */
        fun createNew(url: String = "https://www.google.com"): Tab {
            return Tab(url = url)
        }

        /**
         * Create a tab with Google search query
         */
        fun createWithSearch(query: String): Tab {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            return Tab(url = "https://www.google.com/search?q=$encodedQuery")
        }
    }
}
