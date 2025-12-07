/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.Avanues.web.universal.actions

import com.augmentalis.Avanues.web.universal.platform.Log

/**
 * WebVoiceCommandIntegration - Bridges web actions to voice commands (KMP)
 *
 * Converts extracted web page elements (buttons, links, inputs) into
 * voice commands that can be recognized and executed.
 *
 * This integrates with WebAvanue's WebActionExtractor to provide:
 * - Dynamic voice commands based on current page content
 * - Fuzzy matching for natural voice queries
 * - Feedback generation for TTS responses
 *
 * Usage Flow:
 * 1. User navigates to a web page in WebAvanue browser
 * 2. WebActionExtractor scans page for actionable elements
 * 3. WebVoiceCommandIntegration creates voice-friendly mappings
 * 4. User says "click submit" or "go to home"
 * 5. Matches to web action and executes via WebActionExtractor
 *
 * @created 2025-12-01
 */
class WebVoiceCommandIntegration {

    companion object {
        private const val TAG = "WebVoiceCmd"

        // Common action verbs and their synonyms
        private val ACTION_SYNONYMS = mapOf(
            "click" to listOf("press", "tap", "select", "choose", "hit"),
            "open" to listOf("go to", "navigate to", "visit", "show"),
            "enter" to listOf("type", "input", "fill in", "write"),
            "submit" to listOf("send", "confirm", "done", "finish"),
            "search" to listOf("find", "look for", "look up"),
            "play" to listOf("start", "begin", "watch"),
            "pause" to listOf("stop", "hold"),
            "next" to listOf("forward", "skip"),
            "previous" to listOf("back", "last", "before"),
            "close" to listOf("exit", "dismiss", "cancel"),
            "scroll" to listOf("move", "go")
        )

        // Pattern prefixes that indicate web action intent
        val WEB_ACTION_PREFIXES = listOf(
            "click",
            "press",
            "tap",
            "select",
            "open",
            "go to",
            "navigate to",
            "type",
            "enter",
            "fill in",
            "submit",
            "play",
            "pause"
        )
    }

    /**
     * Extracted page action that can be executed
     */
    data class PageAction(
        val command: String,           // Voice command (e.g., "submit")
        val label: String?,            // Human-readable label
        val type: ActionType,          // Type of action
        val selector: String,          // CSS selector for element
        val x: Int,                    // X coordinate
        val y: Int,                    // Y coordinate
        val variants: List<String>     // Alternative voice commands
    ) {
        /**
         * Check if this action matches a voice query
         */
        fun matches(query: String): Boolean {
            val normalized = normalizeQuery(query)
            return command == normalized ||
                   variants.any { it == normalized } ||
                   (label != null && label.equals(normalized, ignoreCase = true))
        }

        /**
         * Get match confidence score (0-100)
         */
        fun matchScore(query: String): Int {
            val normalized = normalizeQuery(query)
            return when {
                command == normalized -> 100
                variants.any { it == normalized } -> 95
                label?.equals(normalized, ignoreCase = true) == true -> 90
                command.startsWith(normalized) -> 80
                variants.any { it.startsWith(normalized) } -> 75
                command.contains(normalized) -> 60
                variants.any { it.contains(normalized) } -> 55
                label?.contains(normalized, ignoreCase = true) == true -> 50
                else -> 0
            }
        }

        /**
         * Generate TTS feedback for this action
         */
        fun getFeedback(): String {
            return when (type) {
                ActionType.BUTTON -> "Clicking ${label ?: command}"
                ActionType.LINK -> "Opening ${label ?: command}"
                ActionType.INPUT -> "Focus on ${label ?: command}"
                ActionType.MEDIA -> "${command.replaceFirstChar { it.uppercase() }}ing"
                ActionType.MENU -> "Selecting ${label ?: command}"
            }
        }

        private fun normalizeQuery(query: String): String {
            return query.lowercase().trim()
                .replace(Regex("^(please |can you |could you )"), "")
                .replace(Regex("^(click on |press on |tap on )"), "click ")
                .replace(Regex("^(go to |navigate to )"), "open ")
                .trim()
        }
    }

    enum class ActionType {
        BUTTON,
        LINK,
        INPUT,
        MEDIA,
        MENU
    }

    private var currentPageActions: List<PageAction> = emptyList()
    private var currentPageUrl: String = ""

    /**
     * Update available actions from page extraction result
     *
     * Call this when page content changes or after navigation.
     *
     * @param url Current page URL
     * @param actions Raw actions from WebActionExtractor (as JSON-like maps)
     */
    fun updatePageActions(url: String, actions: List<Map<String, Any?>>) {
        currentPageUrl = url
        currentPageActions = actions.mapNotNull { parseAction(it) }
        Log.i(TAG, "Updated ${currentPageActions.size} actions for $url")
    }

    /**
     * Update from VoiceCommand objects (from WebActionExtractor)
     */
    fun updateFromVoiceCommands(url: String, commands: List<VoiceCommand>) {
        currentPageUrl = url
        currentPageActions = commands.map { cmd ->
            PageAction(
                command = cmd.command,
                label = cmd.label,
                type = mapType(cmd.type),
                selector = cmd.selector,
                x = cmd.coordinates.x,
                y = cmd.coordinates.y,
                variants = generateVariants(cmd.command, cmd.label)
            )
        }
        Log.i(TAG, "Updated ${currentPageActions.size} actions for $url")
    }

    /**
     * Check if a query is likely a web action command
     */
    fun isWebActionQuery(query: String): Boolean {
        val normalized = query.lowercase().trim()

        // Check for action prefixes
        if (WEB_ACTION_PREFIXES.any { normalized.startsWith(it) }) {
            return true
        }

        // Check if it matches any current page action
        return currentPageActions.any { it.matchScore(normalized) > 30 }
    }

    /**
     * Find best matching action for a voice query
     *
     * @param query User's voice query
     * @return Best matching PageAction, or null if no good match
     */
    fun findAction(query: String): PageAction? {
        val normalized = extractActionTarget(query)

        val matches = currentPageActions
            .map { it to it.matchScore(normalized) }
            .filter { it.second > 30 }
            .sortedByDescending { it.second }

        if (matches.isEmpty()) {
            Log.d(TAG, "No matches for '$query'")
            return null
        }

        val best = matches.first()
        Log.d(TAG, "Best match for '$query': ${best.first.command} (score: ${best.second})")
        return best.first
    }

    /**
     * Get all available voice commands for current page
     */
    fun getAvailableCommands(): List<String> {
        return currentPageActions.flatMap { action ->
            listOf(action.command) + action.variants
        }.distinct().sorted()
    }

    /**
     * Get primary commands only (no variants)
     */
    fun getPrimaryCommands(): List<String> {
        return currentPageActions.map { it.command }.distinct()
    }

    /**
     * Get summary of available actions for TTS
     */
    fun getSpokenSummary(maxItems: Int = 8): String {
        if (currentPageActions.isEmpty()) {
            return "No interactive elements found on this page."
        }

        // Group by type
        val buttons = currentPageActions.filter { it.type == ActionType.BUTTON }
        val links = currentPageActions.filter { it.type == ActionType.LINK }
        val inputs = currentPageActions.filter { it.type == ActionType.INPUT }

        return buildString {
            if (buttons.isNotEmpty()) {
                append("${buttons.size} buttons including ${buttons.take(3).joinToString(", ") { it.label ?: it.command }}. ")
            }
            if (links.isNotEmpty()) {
                append("${links.size} links including ${links.take(3).joinToString(", ") { it.label ?: it.command }}. ")
            }
            if (inputs.isNotEmpty()) {
                append("${inputs.size} input fields. ")
            }
        }.trim().ifEmpty {
            "Found ${currentPageActions.size} interactive elements."
        }
    }

    /**
     * Get current page URL
     */
    fun getCurrentPageUrl(): String = currentPageUrl

    /**
     * Get action count
     */
    fun getActionCount(): Int = currentPageActions.size

    /**
     * Clear current page actions
     */
    fun clear() {
        currentPageActions = emptyList()
        currentPageUrl = ""
    }

    // ==================== Private Helpers ====================

    private fun parseAction(data: Map<String, Any?>): PageAction? {
        val command = data["command"] as? String ?: return null
        val type = data["type"] as? String ?: "custom"
        val label = data["label"] as? String

        @Suppress("UNCHECKED_CAST")
        val coords = data["coordinates"] as? Map<String, Number>
        val x = coords?.get("x")?.toInt() ?: 0
        val y = coords?.get("y")?.toInt() ?: 0

        return PageAction(
            command = command,
            label = label,
            type = mapType(type),
            selector = data["selector"] as? String ?: "",
            x = x,
            y = y,
            variants = generateVariants(command, label)
        )
    }

    private fun mapType(type: String): ActionType {
        return when (type.lowercase()) {
            "button" -> ActionType.BUTTON
            "link" -> ActionType.LINK
            "input", "select", "checkbox", "radio" -> ActionType.INPUT
            "media_control" -> ActionType.MEDIA
            "menu_item", "tab" -> ActionType.MENU
            else -> ActionType.BUTTON
        }
    }

    private fun generateVariants(command: String, label: String?): List<String> {
        val variants = mutableListOf<String>()

        // Add label as variant
        label?.let {
            if (it != command) {
                variants.add(it.lowercase())
            }
        }

        // Add synonym-based variants
        for ((action, synonyms) in ACTION_SYNONYMS) {
            if (command.startsWith(action)) {
                val base = command.removePrefix(action).trim()
                for (synonym in synonyms) {
                    variants.add("$synonym $base".trim())
                }
            }
        }

        // Add common prefixes
        variants.add("click $command")
        variants.add("press $command")
        variants.add("select $command")

        return variants.distinct()
    }

    private fun extractActionTarget(query: String): String {
        var target = query.lowercase().trim()

        // Remove common prefixes
        for (prefix in WEB_ACTION_PREFIXES) {
            if (target.startsWith(prefix)) {
                target = target.removePrefix(prefix).trim()
                break
            }
        }

        // Remove "the" article
        target = target.removePrefix("the ").trim()

        // Remove "button", "link", etc. suffix
        target = target
            .removeSuffix(" button")
            .removeSuffix(" link")
            .removeSuffix(" field")
            .trim()

        return target
    }
}
