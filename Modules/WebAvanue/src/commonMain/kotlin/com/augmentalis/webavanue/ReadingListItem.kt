package com.augmentalis.webavanue

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * ReadingListItem - Represents an article saved for later reading
 *
 * Supports offline reading by storing the cleaned HTML content.
 * Works with ReadingModeExtractor to save article content.
 */
@Serializable
data class ReadingListItem(
    val id: String,
    val url: String,
    val title: String,
    val excerpt: String? = null,
    val thumbnail: String? = null,
    val offlineHtml: String? = null,
    val savedAt: Instant,
    val isRead: Boolean = false
) {
    /**
     * Check if this item has offline content available
     */
    fun hasOfflineContent(): Boolean = !offlineHtml.isNullOrBlank()

    /**
     * Get a display-friendly date string
     */
    fun savedAtDisplay(): String {
        val now = Clock.System.now()
        val diff = now - savedAt
        val days = diff.inWholeDays
        val hours = diff.inWholeHours
        val minutes = diff.inWholeMinutes

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }

    /**
     * Get domain from URL
     */
    fun domain(): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
    }

    /**
     * Get excerpt preview (truncated)
     */
    fun excerptPreview(maxLength: Int = 150): String {
        return excerpt?.let {
            if (it.length > maxLength) "${it.take(maxLength)}..." else it
        } ?: ""
    }

    companion object {
        /**
         * Create a new ReadingListItem
         */
        fun create(
            url: String,
            title: String,
            excerpt: String? = null,
            thumbnail: String? = null,
            offlineHtml: String? = null
        ): ReadingListItem {
            return ReadingListItem(
                id = "rl_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}",
                url = url,
                title = title,
                excerpt = excerpt,
                thumbnail = thumbnail,
                offlineHtml = offlineHtml,
                savedAt = Clock.System.now(),
                isRead = false
            )
        }
    }
}
