package com.augmentalis.ava.features.llm.response

/**
 * Intent template system for mapping intents to response templates.
 *
 * Provides template-based responses for recognized intents.
 * Used as fallback when LLM response generation is unavailable or fails.
 *
 * Design decisions:
 * - Simple key-value mapping for fast lookup
 * - Immutable templates to ensure thread safety
 * - Unknown intent defaults to teaching prompt
 * - Templates are concise and action-oriented
 *
 * @see com.augmentalis.ava.features.chat.ui.ChatViewModel for usage in message responses
 * @see getResponse for template retrieval
 */
object IntentTemplates {

    /**
     * Intent to response template mapping.
     *
     * Templates follow AVA's communication style:
     * - Action-oriented (describes what AVA will do)
     * - Concise (1-2 sentences max)
     * - Friendly but professional tone
     * - Invites user teaching on unknown intents
     *
     * Updated for Task P2T04: Include all built-in intents from BuiltInIntents.kt
     */
    private val templates = mapOf(
        // Device Control
        "control_lights" to "I'll control the lights for you.",
        "control_temperature" to "Adjusting the temperature.",

        // Information Queries
        "check_weather" to "Let me check the weather for you.",
        "show_time" to "Here's the current time.",

        // Productivity
        "set_alarm" to "Setting an alarm for you.",
        "set_reminder" to "I've set a reminder.",

        // System/Meta Commands
        "show_history" to "Here's your conversation history.",
        "new_conversation" to "Starting a new conversation.",
        "teach_ava" to "I'm ready to learn! What would you like to teach me?",

        // Overlay-specific intents (for voice overlay integration)
        "search" to "I can help you search for that. Would you like me to open your browser or provide a summary?",
        "translate" to "I'll translate that for you. What would you like to translate?",
        "reminder" to "I can set a reminder for you. When would you like to be reminded?",
        "message" to "I can help you send a message. Who would you like to message?",
        "summarize" to "I'll summarize that for you. Please share the text you'd like summarized.",
        "query" to "Let me find that information for you. One moment...",
        "general" to "I'm here to help! I can search, translate, set reminders, send messages, and more. What would you like to do?",

        // Fallback
        "unknown" to "I'm not sure I understood. Would you like to teach me?"
    )

    /**
     * Retrieves the response template for a given intent.
     *
     * Returns a template string that describes AVA's response to the classified intent.
     * If the intent is not recognized, defaults to the "unknown" template which prompts
     * the user to teach AVA.
     *
     * Usage example:
     * ```kotlin
     * val intent = "control_lights"
     * val response = IntentTemplates.getResponse(intent)
     * // response = "I'll control the lights."
     * ```
     *
     * @param intent The classified intent string (e.g., "control_lights", "check_weather")
     * @return The response template for the intent, or the unknown template if not found
     */
    fun getResponse(intent: String): String {
        return templates[intent] ?: templates["unknown"]
            ?: "I didn't understand that. Can you rephrase?"
    }

    /**
     * Returns all available intent templates.
     *
     * Useful for testing and documentation purposes.
     * The returned map is immutable to prevent accidental modification.
     *
     * @return Map of all intent-template pairs
     */
    fun getAllTemplates(): Map<String, String> {
        return templates.toMap()
    }

    /**
     * Checks if a template exists for the given intent.
     *
     * @param intent The intent to check
     * @return true if a specific template exists for this intent, false otherwise
     */
    fun hasTemplate(intent: String): Boolean {
        return intent in templates && intent != "unknown"
    }

    /**
     * Gets the list of all supported intents (excluding "unknown").
     *
     * @return List of supported intent keys
     */
    fun getSupportedIntents(): List<String> {
        return templates.keys.filter { it != "unknown" }
    }
}
