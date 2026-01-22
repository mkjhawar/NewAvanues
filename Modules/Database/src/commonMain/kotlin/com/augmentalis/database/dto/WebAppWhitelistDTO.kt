/**
 * WebAppWhitelistDTO.kt - Data Transfer Object for web app whitelist entries
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-14
 */
package com.augmentalis.database.dto

import com.augmentalis.database.web.Web_app_whitelist

/**
 * DTO for web app whitelist entries.
 *
 * Represents a user-designated web application for persistent voice command storage.
 * Used for database persistence and transfer between layers.
 */
data class WebAppWhitelistDTO(
    val id: Long = 0,

    // Identity
    val domainId: String,           // e.g., "mail.google.com"
    val displayName: String,        // e.g., "Gmail"
    val baseUrl: String? = null,    // e.g., "https://mail.google.com"
    val category: String? = null,   // email, social, productivity, shopping, etc.

    // Settings
    val isEnabled: Boolean = true,
    val autoScan: Boolean = true,
    val saveCommands: Boolean = true,

    // Statistics
    val commandCount: Int = 0,
    val lastVisited: Long? = null,
    val visitCount: Int = 0,

    // Lifecycle
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        /**
         * Common categories for web apps.
         */
        object Categories {
            const val EMAIL = "email"
            const val SOCIAL = "social"
            const val PRODUCTIVITY = "productivity"
            const val SHOPPING = "shopping"
            const val BANKING = "banking"
            const val ENTERTAINMENT = "entertainment"
            const val NEWS = "news"
            const val TRAVEL = "travel"
            const val EDUCATION = "education"
            const val HEALTH = "health"
            const val OTHER = "other"
        }

        /**
         * Popular web apps with suggested categories.
         */
        val POPULAR_APPS = mapOf(
            "mail.google.com" to Pair("Gmail", Categories.EMAIL),
            "outlook.live.com" to Pair("Outlook", Categories.EMAIL),
            "calendar.google.com" to Pair("Google Calendar", Categories.PRODUCTIVITY),
            "docs.google.com" to Pair("Google Docs", Categories.PRODUCTIVITY),
            "drive.google.com" to Pair("Google Drive", Categories.PRODUCTIVITY),
            "sheets.google.com" to Pair("Google Sheets", Categories.PRODUCTIVITY),
            "github.com" to Pair("GitHub", Categories.PRODUCTIVITY),
            "twitter.com" to Pair("Twitter/X", Categories.SOCIAL),
            "x.com" to Pair("X", Categories.SOCIAL),
            "facebook.com" to Pair("Facebook", Categories.SOCIAL),
            "linkedin.com" to Pair("LinkedIn", Categories.SOCIAL),
            "youtube.com" to Pair("YouTube", Categories.ENTERTAINMENT),
            "netflix.com" to Pair("Netflix", Categories.ENTERTAINMENT),
            "amazon.com" to Pair("Amazon", Categories.SHOPPING),
            "ebay.com" to Pair("eBay", Categories.SHOPPING)
        )

        /**
         * Suggest display name and category for a domain.
         */
        fun suggestMetadata(domainId: String): Pair<String, String> {
            val known = POPULAR_APPS[domainId]
            if (known != null) return known

            // Generate name from domain
            val name = domainId
                .removePrefix("www.")
                .substringBefore(".")
                .replaceFirstChar { it.uppercase() }

            return Pair(name, Categories.OTHER)
        }
    }
}

/**
 * Extension to convert SQLDelight generated type to DTO.
 */
fun Web_app_whitelist.toWebAppWhitelistDTO(): WebAppWhitelistDTO {
    return WebAppWhitelistDTO(
        id = id,
        domainId = domain_id,
        displayName = display_name,
        baseUrl = base_url,
        category = category,
        isEnabled = is_enabled == 1L,
        autoScan = auto_scan == 1L,
        saveCommands = save_commands == 1L,
        commandCount = command_count.toInt(),
        lastVisited = last_visited,
        visitCount = visit_count.toInt(),
        createdAt = created_at,
        updatedAt = updated_at
    )
}
