/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.Avanues.web.universal.actions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of actionable web elements
 */
enum class WebActionType {
    BUTTON,
    LINK,
    INPUT,
    SELECT,
    CHECKBOX,
    RADIO,
    TOGGLE,
    MENU_ITEM,
    TAB,
    MEDIA_CONTROL,
    CUSTOM
}

/**
 * Coordinates and dimensions of a web element
 */
@Serializable
data class ElementCoordinates(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * Single actionable web element extracted from page
 */
@Serializable
data class WebAction(
    val type: String,
    val label: String?,
    val voiceCommand: String,
    val selector: String,
    val coordinates: ElementCoordinates,
    val attributes: Map<String, String> = emptyMap()
) {
    /**
     * Get typed action type
     */
    val actionType: WebActionType
        get() = when (type.uppercase()) {
            "BUTTON" -> WebActionType.BUTTON
            "LINK" -> WebActionType.LINK
            "INPUT" -> WebActionType.INPUT
            "SELECT" -> WebActionType.SELECT
            "CHECKBOX" -> WebActionType.CHECKBOX
            "RADIO" -> WebActionType.RADIO
            "TOGGLE" -> WebActionType.TOGGLE
            "MENU_ITEM" -> WebActionType.MENU_ITEM
            "TAB" -> WebActionType.TAB
            "MEDIA_CONTROL" -> WebActionType.MEDIA_CONTROL
            else -> WebActionType.CUSTOM
        }

    /**
     * Check if this action matches a voice command (fuzzy match)
     */
    fun matchesCommand(query: String): Boolean {
        val normalizedQuery = query.lowercase().trim()
        val normalizedCommand = voiceCommand.lowercase()
        val normalizedLabel = label?.lowercase() ?: ""

        return normalizedCommand == normalizedQuery ||
               normalizedCommand.contains(normalizedQuery) ||
               normalizedLabel.contains(normalizedQuery) ||
               normalizedQuery.contains(normalizedCommand)
    }

    /**
     * Get score for how well this action matches a query (higher = better)
     */
    fun matchScore(query: String): Int {
        val normalizedQuery = query.lowercase().trim()
        val normalizedCommand = voiceCommand.lowercase()
        val normalizedLabel = label?.lowercase() ?: ""

        return when {
            normalizedCommand == normalizedQuery -> 100
            normalizedLabel == normalizedQuery -> 90
            normalizedCommand.startsWith(normalizedQuery) -> 80
            normalizedLabel.startsWith(normalizedQuery) -> 70
            normalizedCommand.contains(normalizedQuery) -> 60
            normalizedLabel.contains(normalizedQuery) -> 50
            else -> 0
        }
    }
}

/**
 * Collection of extracted web actions from a page
 */
@Serializable
data class WebActionsResult(
    val buttons: List<WebAction> = emptyList(),
    val links: List<WebAction> = emptyList(),
    val inputs: List<WebAction> = emptyList(),
    val menuItems: List<WebAction> = emptyList(),
    val mediaControls: List<WebAction> = emptyList(),
    val meta: WebActionsMeta = WebActionsMeta()
) {
    /**
     * Get all actions as a flat list
     */
    val allActions: List<WebAction>
        get() = buttons + links + inputs + menuItems + mediaControls

    /**
     * Find best matching action for a voice command
     */
    fun findByCommand(command: String): WebAction? {
        return allActions
            .filter { it.matchesCommand(command) }
            .maxByOrNull { it.matchScore(command) }
    }

    /**
     * Get all voice commands (unique, sorted by position)
     */
    val voiceCommands: List<String>
        get() = allActions.map { it.voiceCommand }.distinct()
}

/**
 * Metadata about the extraction
 */
@Serializable
data class WebActionsMeta(
    val url: String = "",
    val title: String = "",
    val timestamp: Long = 0,
    val extractionTime: Int = 0,
    val totalCount: Int = 0
)

/**
 * Simplified voice command representation
 */
@Serializable
data class VoiceCommand(
    val command: String,
    val type: String,
    val label: String?,
    val selector: String,
    val coordinates: ElementCoordinates
) {
    /**
     * Generate spoken feedback for this command
     */
    fun getFeedback(): String {
        return when (type.lowercase()) {
            "button" -> "Press $command"
            "link" -> "Go to ${label ?: command}"
            "input" -> "Enter text in ${label ?: command}"
            "checkbox", "radio" -> "Toggle ${label ?: command}"
            "media_control" -> command.capitalize()
            else -> "Select ${label ?: command}"
        }
    }
}

/**
 * Result of clicking a web element
 */
@Serializable
data class ClickResult(
    val success: Boolean,
    val type: String? = null,
    val command: String? = null,
    val label: String? = null,
    val error: String? = null,
    val availableCommands: List<String>? = null
)
