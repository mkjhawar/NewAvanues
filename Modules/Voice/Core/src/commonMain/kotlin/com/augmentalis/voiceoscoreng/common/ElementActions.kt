/**
 * ElementActions.kt - Unified action verb definitions for voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-12
 *
 * Defines allowed voice actions for UI elements across platforms (Android, Web, iOS).
 * Maps spoken phrases (verbs) to CommandActionType for execution.
 */
package com.augmentalis.voiceoscoreng.common

/**
 * Action verb definitions with aliases for voice recognition.
 *
 * Each action has:
 * - Primary name (e.g., "click")
 * - Spoken aliases (e.g., "tap", "press", "select")
 * - Corresponding CommandActionType
 */
object ElementActions {

    /**
     * Action verbs that users can speak, mapped to CommandActionType.
     *
     * @property aliases All spoken phrases that trigger this action
     * @property actionType The CommandActionType to execute
     * @property requiresTarget Whether this action requires a specific element target
     */
    enum class ActionVerb(
        val aliases: List<String>,
        val actionType: CommandActionType,
        val requiresTarget: Boolean = true
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // Click/Tap Actions
        // ═══════════════════════════════════════════════════════════════════

        CLICK(
            aliases = listOf("click", "tap", "press", "select", "activate", "choose", "pick"),
            actionType = CommandActionType.CLICK
        ),

        LONG_PRESS(
            aliases = listOf("long press", "hold", "long click", "press and hold", "long tap", "hold down"),
            actionType = CommandActionType.LONG_CLICK
        ),

        DOUBLE_TAP(
            aliases = listOf("double click", "double tap", "double press"),
            actionType = CommandActionType.CLICK  // Will be handled specially
        ),

        // ═══════════════════════════════════════════════════════════════════
        // Text Input Actions
        // ═══════════════════════════════════════════════════════════════════

        FOCUS(
            aliases = listOf("focus", "go to", "move to", "select field", "focus on"),
            actionType = CommandActionType.FOCUS
        ),

        TYPE(
            aliases = listOf("type", "enter", "input", "write", "fill", "fill in"),
            actionType = CommandActionType.TYPE
        ),

        CLEAR(
            aliases = listOf("clear", "delete", "erase", "remove text", "clear text", "empty"),
            actionType = CommandActionType.TYPE  // Clear is a type with empty string
        ),

        // ═══════════════════════════════════════════════════════════════════
        // Scroll Actions
        // ═══════════════════════════════════════════════════════════════════

        SCROLL_TO(
            aliases = listOf("scroll to", "show", "reveal", "bring into view", "go to"),
            actionType = CommandActionType.SCROLL
        ),

        SCROLL_UP(
            aliases = listOf("scroll up", "page up", "swipe down"),
            actionType = CommandActionType.SCROLL_UP,
            requiresTarget = false
        ),

        SCROLL_DOWN(
            aliases = listOf("scroll down", "page down", "swipe up"),
            actionType = CommandActionType.SCROLL_DOWN,
            requiresTarget = false
        ),

        SCROLL_LEFT(
            aliases = listOf("scroll left", "swipe right"),
            actionType = CommandActionType.SCROLL_LEFT,
            requiresTarget = false
        ),

        SCROLL_RIGHT(
            aliases = listOf("scroll right", "swipe left"),
            actionType = CommandActionType.SCROLL_RIGHT,
            requiresTarget = false
        ),

        // ═══════════════════════════════════════════════════════════════════
        // Toggle Actions (Checkboxes, Switches)
        // ═══════════════════════════════════════════════════════════════════

        CHECK(
            aliases = listOf("check", "enable", "turn on", "toggle on", "switch on", "mark"),
            actionType = CommandActionType.CLICK
        ),

        UNCHECK(
            aliases = listOf("uncheck", "disable", "turn off", "toggle off", "switch off", "unmark"),
            actionType = CommandActionType.CLICK
        ),

        TOGGLE(
            aliases = listOf("toggle", "switch", "flip"),
            actionType = CommandActionType.CLICK
        ),

        // ═══════════════════════════════════════════════════════════════════
        // Expand/Collapse Actions
        // ═══════════════════════════════════════════════════════════════════

        EXPAND(
            aliases = listOf("expand", "open", "show more", "unfold"),
            actionType = CommandActionType.CLICK
        ),

        COLLAPSE(
            aliases = listOf("collapse", "close", "show less", "fold"),
            actionType = CommandActionType.CLICK
        ),

        // ═══════════════════════════════════════════════════════════════════
        // Navigation Actions (targetless)
        // ═══════════════════════════════════════════════════════════════════

        GO_BACK(
            aliases = listOf("go back", "back", "previous", "return"),
            actionType = CommandActionType.BACK,
            requiresTarget = false
        ),

        GO_HOME(
            aliases = listOf("go home", "home", "home screen"),
            actionType = CommandActionType.HOME,
            requiresTarget = false
        );

        companion object {
            /**
             * Find action verb from spoken phrase.
             *
             * Checks if the phrase starts with any known action alias.
             *
             * @param phrase The spoken phrase (e.g., "click submit button")
             * @return Matching ActionVerb or null
             */
            fun fromPhrase(phrase: String): ActionVerb? {
                val normalized = phrase.lowercase().trim()
                return entries.find { action ->
                    action.aliases.any { alias ->
                        normalized.startsWith(alias) || normalized == alias
                    }
                }
            }

            /**
             * Extract the action verb and remaining target from a phrase.
             *
             * @param phrase The full spoken phrase
             * @return Pair of (ActionVerb?, targetText) or (null, phrase) if no verb found
             */
            fun parsePhrase(phrase: String): Pair<ActionVerb?, String> {
                val normalized = phrase.lowercase().trim()
                for (action in entries) {
                    for (alias in action.aliases.sortedByDescending { it.length }) {
                        if (normalized.startsWith(alias)) {
                            val target = normalized.removePrefix(alias).trim()
                            return action to target
                        }
                    }
                }
                return null to phrase
            }

            /**
             * Get all aliases for a CommandActionType.
             */
            fun aliasesForAction(actionType: CommandActionType): List<String> {
                return entries
                    .filter { it.actionType == actionType }
                    .flatMap { it.aliases }
                    .distinct()
            }
        }
    }

    /**
     * Get allowed actions for an element based on its capabilities.
     *
     * @param isClickable Can be clicked/tapped
     * @param isLongClickable Can be long-pressed
     * @param isEditable Can receive text input
     * @param isScrollable Can be scrolled
     * @param isCheckable Is a checkbox/radio/switch
     * @param isFocusable Can receive focus
     * @param isExpandable Can be expanded/collapsed (e.g., accordion)
     * @return List of allowed ActionVerb values
     */
    fun getAllowedActions(
        isClickable: Boolean = false,
        isLongClickable: Boolean = false,
        isEditable: Boolean = false,
        isScrollable: Boolean = false,
        isCheckable: Boolean = false,
        isFocusable: Boolean = false,
        isExpandable: Boolean = false
    ): List<ActionVerb> {
        val actions = mutableListOf<ActionVerb>()

        if (isClickable) {
            actions.add(ActionVerb.CLICK)
        }

        if (isLongClickable) {
            actions.add(ActionVerb.LONG_PRESS)
        }

        if (isEditable) {
            actions.add(ActionVerb.TYPE)
            actions.add(ActionVerb.CLEAR)
            actions.add(ActionVerb.FOCUS)
        } else if (isFocusable) {
            actions.add(ActionVerb.FOCUS)
        }

        if (isScrollable) {
            actions.add(ActionVerb.SCROLL_TO)
        }

        if (isCheckable) {
            actions.add(ActionVerb.CHECK)
            actions.add(ActionVerb.UNCHECK)
            actions.add(ActionVerb.TOGGLE)
        }

        if (isExpandable) {
            actions.add(ActionVerb.EXPAND)
            actions.add(ActionVerb.COLLAPSE)
        }

        // All visible elements can be scrolled to
        actions.add(ActionVerb.SCROLL_TO)

        return actions.distinct()
    }

    /**
     * Get allowed actions from Android ScrapedElement fields.
     */
    fun fromScrapedElement(
        isClickable: Int,
        isLongClickable: Int,
        isEditable: Int,
        isScrollable: Int,
        isCheckable: Int,
        isFocusable: Int
    ): List<ActionVerb> {
        return getAllowedActions(
            isClickable = isClickable == 1,
            isLongClickable = isLongClickable == 1,
            isEditable = isEditable == 1,
            isScrollable = isScrollable == 1,
            isCheckable = isCheckable == 1,
            isFocusable = isFocusable == 1
        )
    }

    /**
     * Get the primary (default) action for an element.
     *
     * Priority: CLICK > FOCUS > TYPE > SCROLL_TO
     */
    fun getPrimaryAction(
        isClickable: Boolean = false,
        isEditable: Boolean = false,
        isFocusable: Boolean = false
    ): ActionVerb {
        return when {
            isClickable -> ActionVerb.CLICK
            isEditable -> ActionVerb.TYPE
            isFocusable -> ActionVerb.FOCUS
            else -> ActionVerb.SCROLL_TO
        }
    }

    /**
     * Convert actions list to JSON string for database storage.
     */
    fun toJson(actions: List<ActionVerb>): String {
        return actions.joinToString(prefix = "[", postfix = "]") { "\"${it.name.lowercase()}\"" }
    }

    /**
     * Parse actions from JSON string.
     */
    fun fromJson(json: String): List<ActionVerb> {
        if (json.isBlank()) return listOf(ActionVerb.CLICK)
        return json
            .removeSurrounding("[", "]")
            .split(",")
            .mapNotNull { it.trim().removeSurrounding("\"").uppercase().let { name ->
                try { ActionVerb.valueOf(name) } catch (e: Exception) { null }
            }}
    }

    /**
     * Get all spoken aliases for display in help/hints.
     */
    fun getAllAliases(): Map<ActionVerb, List<String>> {
        return ActionVerb.entries.associateWith { it.aliases }
    }
}
