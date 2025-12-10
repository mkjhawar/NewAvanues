package com.augmentalis.ava.features.chat.data

/**
 * Built-in intent definitions for AVA AI
 *
 * These are the core intents that AVA understands out-of-the-box,
 * before any user training. They cover common smart assistant operations.
 *
 * Design rationale (Task P2T02):
 * - Keep list focused on essential operations (~6-10 intents)
 * - Use snake_case naming convention for consistency with VOS4
 * - Intents are language-agnostic (same intent for "turn on lights" vs "encender luces")
 * - User can add more intents via Teach-AVA (stored in TrainExample table)
 *
 * Intent categories:
 * 1. Device control: control_lights, control_temperature
 * 2. Information: check_weather, show_time
 * 3. Productivity: set_alarm, set_reminder
 * 4. System/Meta: show_history, new_conversation, teach_ava
 *
 * Future expansion (Phase 3+):
 * - Smart home: control_music, control_tv, control_security
 * - Productivity: add_calendar_event, send_message, make_call
 * - Information: search_web, translate_text, calculate_math
 *
 * @see IntentTemplates for response templates
 * @see TrainExampleRepository for user-taught intents
 */
object BuiltInIntents {

    // ==================== Device Control ====================

    /**
     * Control smart lights (turn on/off, dim, change color)
     * Examples: "Turn on the lights", "Dim the bedroom lights", "Make lights blue"
     */
    const val CONTROL_LIGHTS = "control_lights"

    /**
     * Control temperature/thermostat
     * Examples: "Set temperature to 72", "Turn on AC", "Make it warmer"
     */
    const val CONTROL_TEMPERATURE = "control_temperature"

    // ==================== Information Queries ====================

    /**
     * Check current or forecast weather
     * Examples: "What's the weather?", "Will it rain tomorrow?", "Weather in Seattle"
     */
    const val CHECK_WEATHER = "check_weather"

    /**
     * Show current time or time in another timezone
     * Examples: "What time is it?", "Time in Tokyo?", "Show clock"
     */
    const val SHOW_TIME = "show_time"

    // ==================== Productivity ====================

    /**
     * Set an alarm
     * Examples: "Set alarm for 7am", "Wake me up at 6:30", "Alarm in 30 minutes"
     */
    const val SET_ALARM = "set_alarm"

    /**
     * Set a reminder
     * Examples: "Remind me to call mom", "Reminder at 3pm", "Don't let me forget"
     */
    const val SET_REMINDER = "set_reminder"

    // ==================== System/Meta Commands ====================

    /**
     * Show conversation history overlay (Task P4T02)
     * Examples: "Show history", "Show transcript", "View past conversations"
     */
    const val SHOW_HISTORY = "show_history"

    /**
     * Start a new conversation
     * Examples: "New conversation", "Start fresh", "Clear chat"
     */
    const val NEW_CONVERSATION = "new_conversation"

    /**
     * Enter Teach-AVA mode or teach a specific intent
     * Examples: "Teach AVA", "I want to train you", "Learn this command"
     */
    const val TEACH_AVA = "teach_ava"

    // ==================== Fallback ====================

    /**
     * Unknown intent (low confidence or unresolved)
     * Triggers auto-prompt to teach AVA (Task P2T06)
     */
    const val UNKNOWN = "unknown"

    /**
     * All built-in intents that AVA understands by default
     */
    val ALL_INTENTS = listOf(
        // Device Control
        CONTROL_LIGHTS,
        CONTROL_TEMPERATURE,

        // Information Queries
        CHECK_WEATHER,
        SHOW_TIME,

        // Productivity
        SET_ALARM,
        SET_REMINDER,

        // System/Meta Commands
        SHOW_HISTORY,
        NEW_CONVERSATION,
        TEACH_AVA,

        // Fallback
        UNKNOWN
    )

    // ==================== Helper Functions ====================

    /**
     * Check if an intent is a built-in intent
     * @param intent Intent name to check
     * @return True if intent is in built-in list
     */
    fun isBuiltIn(intent: String): Boolean {
        return intent in ALL_INTENTS
    }

    /**
     * Get intent category for grouping/filtering
     * @param intent Intent name
     * @return Category string (e.g., "device_control", "information", "productivity")
     */
    fun getCategory(intent: String): String {
        return when (intent) {
            CONTROL_LIGHTS, CONTROL_TEMPERATURE -> "device_control"
            CHECK_WEATHER, SHOW_TIME -> "information"
            SET_ALARM, SET_REMINDER -> "productivity"
            SHOW_HISTORY, NEW_CONVERSATION, TEACH_AVA -> "system"
            else -> "custom"
        }
    }

    /**
     * Get human-readable label for intent (for Teach-AVA UI)
     * @param intent Intent name
     * @return Localized display label
     */
    fun getDisplayLabel(intent: String): String {
        return when (intent) {
            CONTROL_LIGHTS -> "Control Lights"
            CONTROL_TEMPERATURE -> "Control Temperature"
            CHECK_WEATHER -> "Check Weather"
            SHOW_TIME -> "Show Time"
            SET_ALARM -> "Set Alarm"
            SET_REMINDER -> "Set Reminder"
            SHOW_HISTORY -> "Show History"
            NEW_CONVERSATION -> "New Conversation"
            TEACH_AVA -> "Teach AVA"
            UNKNOWN -> "Unknown"
            else -> intent.replace("_", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    /**
     * Get example utterances for an intent (for Teach-AVA suggestions)
     * @param intent Intent name
     * @return List of example utterances
     */
    fun getExampleUtterances(intent: String): List<String> {
        return when (intent) {
            CONTROL_LIGHTS -> listOf(
                "Turn on the lights",
                "Turn off the lights",
                "Dim the bedroom lights",
                "Make lights brighter"
            )
            CONTROL_TEMPERATURE -> listOf(
                "Set temperature to 72",
                "Turn on AC",
                "Make it warmer",
                "Cool down the room"
            )
            CHECK_WEATHER -> listOf(
                "What's the weather?",
                "Will it rain tomorrow?",
                "Weather forecast",
                "Check weather in Seattle"
            )
            SHOW_TIME -> listOf(
                "What time is it?",
                "Show clock",
                "Time in Tokyo?",
                "Current time"
            )
            SET_ALARM -> listOf(
                "Set alarm for 7am",
                "Wake me up at 6:30",
                "Alarm in 30 minutes",
                "Set morning alarm"
            )
            SET_REMINDER -> listOf(
                "Remind me to call mom",
                "Reminder at 3pm",
                "Don't let me forget",
                "Set reminder for meeting"
            )
            SHOW_HISTORY -> listOf(
                "Show history",
                "Show transcript",
                "View past conversations",
                "Open history"
            )
            NEW_CONVERSATION -> listOf(
                "New conversation",
                "Start fresh",
                "Clear chat",
                "Begin new chat"
            )
            TEACH_AVA -> listOf(
                "Teach AVA",
                "I want to train you",
                "Learn this command",
                "Teach you a new intent",
                "Train AVA",
                "Teach this",
                "Add intent",
                "Show me how",
                "I want to teach you something"
            )
            else -> emptyList()
        }
    }
}
