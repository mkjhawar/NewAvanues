/**
 * ScrapedWebCommandDTO.kt - Data Transfer Object for web voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-14
 */
package com.augmentalis.database.dto

import com.augmentalis.database.web.Scraped_web_command

/**
 * DTO for scraped web voice commands.
 *
 * Represents a voice command extracted from a web page element.
 * Used for database persistence and transfer between layers.
 */
data class ScrapedWebCommandDTO(
    val id: Long = 0,

    // Element identification
    val elementHash: String,
    val domainId: String,
    val urlPattern: String? = null,

    // Selectors for execution
    val cssSelector: String,
    val xpath: String? = null,

    // Command data
    val commandText: String,
    val elementText: String? = null,
    val elementTag: String,
    val elementType: String,

    // Actions allowed (stored as JSON array)
    val allowedActions: List<String> = listOf("click"),
    val primaryAction: String = "click",

    // Confidence and approval
    val confidence: Float = 0.5f,
    val isUserApproved: Boolean = false,
    val userApprovedAt: Long? = null,

    // Synonyms (stored as JSON array)
    val synonyms: List<String>? = null,

    // Usage tracking
    val usageCount: Int = 0,
    val lastUsed: Long? = null,

    // Lifecycle
    val createdAt: Long,
    val lastVerified: Long? = null,
    val isDeprecated: Boolean = false,

    // Position for overlay
    val boundLeft: Int? = null,
    val boundTop: Int? = null,
    val boundWidth: Int? = null,
    val boundHeight: Int? = null
) {
    /**
     * Convert allowed actions list to JSON string for database storage.
     */
    fun allowedActionsJson(): String {
        return allowedActions.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }

    /**
     * Convert synonyms list to JSON string for database storage.
     */
    fun synonymsJson(): String? {
        return synonyms?.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }

    companion object {
        /**
         * Parse allowed actions from JSON string.
         */
        fun parseAllowedActions(json: String?): List<String> {
            if (json.isNullOrBlank()) return listOf("click")
            return json
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        }

        /**
         * Parse synonyms from JSON string.
         */
        fun parseSynonyms(json: String?): List<String>? {
            if (json.isNullOrBlank()) return null
            return json
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
                .ifEmpty { null }
        }
    }
}

/**
 * Extension to convert SQLDelight generated type to DTO.
 */
fun Scraped_web_command.toScrapedWebCommandDTO(): ScrapedWebCommandDTO {
    return ScrapedWebCommandDTO(
        id = id,
        elementHash = element_hash,
        domainId = domain_id,
        urlPattern = url_pattern,
        cssSelector = css_selector,
        xpath = xpath,
        commandText = command_text,
        elementText = element_text,
        elementTag = element_tag,
        elementType = element_type,
        allowedActions = ScrapedWebCommandDTO.parseAllowedActions(allowed_actions),
        primaryAction = primary_action,
        confidence = confidence.toFloat(),
        isUserApproved = is_user_approved == 1L,
        userApprovedAt = user_approved_at,
        synonyms = ScrapedWebCommandDTO.parseSynonyms(synonyms),
        usageCount = usage_count.toInt(),
        lastUsed = last_used,
        createdAt = created_at,
        lastVerified = last_verified,
        isDeprecated = is_deprecated == 1L,
        boundLeft = bound_left?.toInt(),
        boundTop = bound_top?.toInt(),
        boundWidth = bound_width?.toInt(),
        boundHeight = bound_height?.toInt()
    )
}
