package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a browsing history entry in WebAvanue.
 * Supports full-text search and intelligent grouping.
 */
@Serializable
data class HistoryEntry(
    val id: String,
    val url: String,
    val title: String,
    val favicon: String? = null,
    val visitedAt: Instant,
    val visitCount: Int = 1,
    val visitDuration: Long = 0, // Time spent on page in seconds
    val referrer: String? = null, // Where the user came from
    val searchTerms: String? = null, // Search terms if from search engine
    val isIncognito: Boolean = false,
    val deviceId: String? = null // For sync across devices
) {
    companion object {
        /**
         * Creates a new history entry
         */
        fun create(
            url: String,
            title: String,
            referrer: String? = null,
            isIncognito: Boolean = false
        ): HistoryEntry {
            return HistoryEntry(
                id = generateHistoryId(),
                url = url,
                title = title,
                visitedAt = kotlinx.datetime.Clock.System.now(),
                referrer = referrer,
                isIncognito = isIncognito
            )
        }

        private fun generateHistoryId(): String {
            return "history_${System.currentTimeMillis()}_${(0..9999).random()}"
        }

        /**
         * Maximum history entries to keep
         */
        const val MAX_HISTORY_ENTRIES = 10000

        /**
         * History retention period in days
         */
        const val RETENTION_DAYS = 90
    }
}

/**
 * Represents a grouped history session
 */
@Serializable
data class HistorySession(
    val id: String,
    val title: String, // e.g., "Morning browsing", "Research session"
    val startTime: Instant,
    val endTime: Instant?,
    val entryIds: List<String>, // IDs of history entries in this session
    val tabCount: Int,
    val totalDuration: Long // in seconds
)