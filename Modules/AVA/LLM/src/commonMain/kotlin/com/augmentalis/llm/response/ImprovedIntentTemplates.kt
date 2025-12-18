package com.augmentalis.llm.response

import kotlin.random.Random

/**
 * Improved Intent Template System with Best Practices
 *
 * Implements conversational AI best practices:
 * - 3-strikes rule for fallbacks
 * - Multiple response variations to avoid repetition
 * - Contextual guidance and suggestions
 * - Clear calls to action
 * - Escalation options after repeated failures
 *
 * Based on best practices from:
 * - LivePerson Developer Center
 * - UX Collective research on conversational experiences
 * - Chatbot Academy design patterns
 */
object ImprovedIntentTemplates {

    // Track fallback attempts per conversation
    private val fallbackCounter = mutableMapOf<String, Int>()
    private var lastFallbackIndex = -1

    /**
     * Enhanced templates with multiple variations per intent
     */
    private val enhancedTemplates = mapOf(
        // Device Control - More specific and actionable
        "control_lights" to listOf(
            "I'll adjust the lights for you. Which lights would you like me to control?",
            "Controlling your lights now. Would you like them on, off, or dimmed?",
            "Light control activated. Just tell me which room and what brightness you prefer."
        ),

        "control_temperature" to listOf(
            "I'll adjust the temperature for you. What temperature would you like?",
            "Temperature control ready. Would you like it warmer or cooler?",
            "Setting the thermostat now. What's your preferred temperature?"
        ),

        // Information Queries - More informative
        "check_weather" to listOf(
            "I'll check the current weather conditions. For which location would you like the weather?",
            "Getting weather information now. Would you like today's forecast or the weekly outlook?",
            "Weather update coming up. I can provide temperature, precipitation, and wind conditions."
        ),

        "show_time" to listOf(
            "The current time is ${getCurrentTime()}. Would you like to set an alarm or timer?",
            "It's ${getCurrentTime()} right now. Need the time in another timezone?",
            "Current time: ${getCurrentTime()}. I can also show world clocks if needed."
        ),

        // Productivity - More helpful
        "set_alarm" to listOf(
            "I'll set that alarm for you. What time should I wake you?",
            "Creating your alarm now. Would you like a one-time alarm or recurring?",
            "Alarm ready to set. Just tell me the time and I'll handle it."
        ),

        "set_reminder" to listOf(
            "I'll create that reminder. What would you like to be reminded about and when?",
            "Setting up your reminder now. Should I send you a notification or just save it?",
            "Reminder noted. When should I remind you about this?"
        ),

        // System Commands - More engaging
        "show_history" to listOf(
            "Here's your recent conversation history. You can scroll up to see older messages.",
            "Displaying conversation history. Would you like to search for something specific?",
            "Your chat history is shown above. Need help finding a particular conversation?"
        ),

        "new_conversation" to listOf(
            "Starting fresh! How can I help you today?",
            "New conversation started. What would you like to talk about?",
            "Clean slate! I'm ready to assist with whatever you need."
        ),

        "teach_ava" to listOf(
            "I'm eager to learn! Show me what this phrase should do and I'll remember it.",
            "Teaching mode activated! What would you like me to learn today?",
            "Ready to learn something new! Just tell me the phrase and what action it should trigger."
        )
    )

    /**
     * Fallback responses using 3-strikes rule
     * Progressive guidance from gentle correction to escalation
     */
    private val fallbackResponses = listOf(
        // Strike 1 - Gentle guidance
        listOf(
            "I didn't quite catch that. Could you rephrase it for me? I can help with lights, temperature, weather, alarms, and reminders.",
            "Hmm, I'm not sure what you mean. Try asking about the weather, setting an alarm, or controlling your smart devices.",
            "I might have misunderstood. I'm best at device control, weather updates, and setting reminders. What would you like to do?"
        ),

        // Strike 2 - More specific help
        listOf(
            "I'm still having trouble understanding. Here are some things you can try:\n• \"Turn on the lights\"\n• \"What's the weather?\"\n• \"Set an alarm for 7 AM\"\n• \"Remind me to call mom\"",
            "Let me help you get started. You can say things like:\n• \"Control the temperature\"\n• \"Show me the time\"\n• \"Start a new conversation\"\nOr teach me something new!",
            "I want to help but I'm not understanding. Try these examples:\n• \"Check the weather in New York\"\n• \"Turn off bedroom lights\"\n• \"Set reminder for meeting at 3 PM\""
        ),

        // Strike 3 - Escalation options
        listOf(
            "I'm having difficulty understanding your request. Would you like to:\n• Teach me this phrase so I can learn it\n• Try a different command\n• Get help with what I can do",
            "I apologize, but I can't understand this request after several attempts. You can:\n• Tap the 'Teach' button to train me\n• Ask something else\n• Say \"help\" to see all my capabilities",
            "I'm sorry I keep missing this. Let's try something different:\n• Would you like to teach me this command?\n• Should I show you what I can do?\n• Or we can start over with a new conversation"
        )
    )

    /**
     * General help response when user asks for help
     */
    private val helpResponse = """
        I'm AVA, your AI assistant! Here's what I can help you with:

        🏠 **Smart Home Control**
        • Lights: "Turn on the living room lights"
        • Temperature: "Set temperature to 72 degrees"

        📱 **Information**
        • Weather: "What's the weather like?"
        • Time: "What time is it?"

        ⏰ **Productivity**
        • Alarms: "Set an alarm for 7:30 AM"
        • Reminders: "Remind me to buy milk"

        💬 **Conversation**
        • History: "Show my chat history"
        • New chat: "Start a new conversation"
        • Teaching: "I want to teach you something"

        Just ask naturally and I'll do my best to help!
    """.trimIndent()

    /**
     * Get response for an intent with variation
     */
    fun getResponse(intent: String, conversationId: String? = null): String {
        // Reset counter if switching to a known intent
        if (intent != "unknown" && conversationId != null) {
            fallbackCounter[conversationId] = 0
        }

        // Handle help requests
        if (intent == "help" || intent == "get_help") {
            return helpResponse
        }

        // Handle known intents with variation
        enhancedTemplates[intent]?.let { variations ->
            return variations.random()
        }

        // Handle unknown/fallback with progressive responses
        if (intent == "unknown") {
            return getFallbackResponse(conversationId ?: "default")
        }

        // Final fallback
        return "I'm ready to help! You can ask about weather, control devices, or set reminders. What would you like to do?"
    }

    /**
     * Get progressive fallback response using 3-strikes rule
     */
    private fun getFallbackResponse(conversationId: String): String {
        val strikes = (fallbackCounter[conversationId] ?: 0) + 1
        fallbackCounter[conversationId] = strikes

        // Determine which strike level we're at (cap at 3)
        val strikeLevel = when {
            strikes <= 1 -> 0  // First strike
            strikes == 2 -> 1  // Second strike
            else -> 2          // Third+ strike
        }

        // Get responses for this strike level
        val levelResponses = fallbackResponses[strikeLevel]

        // Select a different response than last time if possible
        var selectedIndex = Random.nextInt(levelResponses.size)
        if (selectedIndex == lastFallbackIndex && levelResponses.size > 1) {
            selectedIndex = (selectedIndex + 1) % levelResponses.size
        }
        lastFallbackIndex = selectedIndex

        return levelResponses[selectedIndex]
    }

    /**
     * Reset fallback counter for a conversation
     */
    fun resetFallbackCounter(conversationId: String) {
        fallbackCounter[conversationId] = 0
    }

    /**
     * Get current time helper (should be replaced with actual time)
     */
    private fun getCurrentTime(): String {
        // This would normally use actual time
        return "12:40 PM"
    }

    /**
     * Get contextual response based on confidence
     */
    fun getConfidenceBasedResponse(intent: String, confidence: Float, conversationId: String? = null): String {
        return when {
            confidence > 0.8f -> {
                // High confidence - use standard response
                getResponse(intent, conversationId)
            }
            confidence > 0.5f -> {
                // Medium confidence - add confirmation
                val baseResponse = getResponse(intent, conversationId)
                "$baseResponse\n(Did I understand correctly?)"
            }
            else -> {
                // Low confidence - use fallback
                getFallbackResponse(conversationId ?: "default")
            }
        }
    }
}
