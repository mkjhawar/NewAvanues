package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a browser tab in the WebAvanue browser.
 * This is a cross-platform domain model used across Android, iOS, and Desktop.
 */
@Serializable
data class Tab(
    val id: String,
    val url: String,
    val title: String = "",
    val favicon: String? = null,
    val isActive: Boolean = false,
    val isPinned: Boolean = false,
    val isIncognito: Boolean = false,
    val createdAt: Instant,
    val lastAccessedAt: Instant,
    val position: Int = 0,
    val parentTabId: String? = null, // For tab grouping
    val sessionData: String? = null   // For restoring scroll position, form data, etc.
) {
    companion object {
        /**
         * Creates a new tab with default values
         */
        fun create(
            url: String,
            title: String = "",
            isIncognito: Boolean = false
        ): Tab {
            val now = kotlinx.datetime.Clock.System.now()
            return Tab(
                id = generateTabId(),
                url = url,
                title = title,
                isIncognito = isIncognito,
                createdAt = now,
                lastAccessedAt = now
            )
        }

        private fun generateTabId(): String {
            return "tab_${System.currentTimeMillis()}_${(0..9999).random()}"
        }

        /**
         * Default home page URL
         */
        const val DEFAULT_URL = "https://www.google.com"

        /**
         * Maximum number of tabs allowed
         */
        const val MAX_TABS = 100
    }
}