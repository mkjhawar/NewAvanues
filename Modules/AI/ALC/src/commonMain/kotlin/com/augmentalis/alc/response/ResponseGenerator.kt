package com.augmentalis.alc.response

import com.augmentalis.alc.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Template-based response generator for fast, deterministic responses
 * Used as fallback when LLM is unavailable or for simple queries
 */
class TemplateResponseGenerator {

    private val templates = mutableMapOf<String, List<String>>()

    init {
        loadDefaultTemplates()
    }

    private fun loadDefaultTemplates() {
        templates["greeting"] = listOf(
            "Hello! How can I help you today?",
            "Hi there! What would you like to do?",
            "Hey! I'm ready to assist you."
        )
        templates["farewell"] = listOf(
            "Goodbye! Have a great day!",
            "See you later!",
            "Take care!"
        )
        templates["confirmation"] = listOf(
            "Done!",
            "Got it!",
            "Completed successfully."
        )
        templates["error"] = listOf(
            "Sorry, I couldn't complete that action.",
            "Something went wrong. Please try again.",
            "I encountered an error processing your request."
        )
        templates["unknown"] = listOf(
            "I'm not sure how to help with that.",
            "Could you please rephrase your request?",
            "I didn't understand. Can you try again?"
        )
    }

    fun addTemplate(category: String, responses: List<String>) {
        templates[category] = responses
    }

    fun generate(category: String, variables: Map<String, String> = emptyMap()): String {
        val templateList = templates[category] ?: templates["unknown"]!!
        var response = templateList.random()

        variables.forEach { (key, value) ->
            response = response.replace("{$key}", value)
        }

        return response
    }

    fun generateFlow(category: String, variables: Map<String, String> = emptyMap()): Flow<LLMResponse> = flow {
        val text = generate(category, variables)
        emit(LLMResponse.Complete(
            fullText = text,
            usage = TokenUsage(0, text.split(" ").size),
            model = "template"
        ))
    }
}

/**
 * Intent-specific response templates
 */
object IntentTemplates {

    private val intentResponses = mapOf(
        // Navigation intents
        "open_app" to "Opening {app_name}...",
        "go_back" to "Going back.",
        "go_home" to "Going to home screen.",
        "scroll_up" to "Scrolling up.",
        "scroll_down" to "Scrolling down.",

        // Action intents
        "click" to "Tapping on {target}.",
        "type_text" to "Typing: {text}",
        "search" to "Searching for {query}...",
        "select" to "Selecting {item}.",

        // System intents
        "volume_up" to "Increasing volume.",
        "volume_down" to "Decreasing volume.",
        "brightness_up" to "Increasing brightness.",
        "brightness_down" to "Decreasing brightness.",
        "wifi_on" to "Turning on WiFi.",
        "wifi_off" to "Turning off WiFi.",
        "bluetooth_on" to "Turning on Bluetooth.",
        "bluetooth_off" to "Turning off Bluetooth.",

        // Communication intents
        "call" to "Calling {contact}...",
        "send_message" to "Sending message to {contact}: {message}",
        "read_messages" to "You have {count} new messages.",

        // Query intents
        "weather" to "The weather in {location} is {conditions}, {temperature}.",
        "time" to "The current time is {time}.",
        "date" to "Today is {date}.",
        "battery" to "Battery is at {level}%."
    )

    fun getResponse(intent: String, params: Map<String, String> = emptyMap()): String {
        var template = intentResponses[intent] ?: "Executing $intent..."
        params.forEach { (key, value) ->
            template = template.replace("{$key}", value)
        }
        return template
    }

    fun hasTemplate(intent: String): Boolean = intentResponses.containsKey(intent)
}

/**
 * Hybrid response generator combining templates and LLM
 */
class HybridResponseGenerator(
    private val templateGenerator: TemplateResponseGenerator,
    private val llmProvider: com.augmentalis.alc.engine.ILLMProvider?
) {

    /**
     * Generate response using best available method
     */
    fun generate(
        messages: List<ChatMessage>,
        intent: String? = null,
        params: Map<String, String> = emptyMap(),
        options: GenerationOptions = GenerationOptions.DEFAULT
    ): Flow<LLMResponse> = flow {
        // Try intent template first for known intents
        if (intent != null && IntentTemplates.hasTemplate(intent)) {
            val text = IntentTemplates.getResponse(intent, params)
            emit(LLMResponse.Complete(
                fullText = text,
                usage = TokenUsage(0, text.split(" ").size),
                model = "template"
            ))
            return@flow
        }

        // Try LLM if available
        if (llmProvider != null) {
            try {
                llmProvider.chat(messages, options).collect { emit(it) }
                return@flow
            } catch (e: Exception) {
                // Fall through to template fallback
            }
        }

        // Fallback to generic template
        val category = categorizeQuery(messages.lastOrNull()?.content ?: "")
        templateGenerator.generateFlow(category, params).collect { emit(it) }
    }

    private fun categorizeQuery(query: String): String {
        val lower = query.lowercase()
        return when {
            lower.matches(Regex(".*(hello|hi|hey).*")) -> "greeting"
            lower.matches(Regex(".*(bye|goodbye|see you).*")) -> "farewell"
            lower.matches(Regex(".*(thanks|thank you|done).*")) -> "confirmation"
            else -> "unknown"
        }
    }
}

/**
 * Context builder for LLM requests
 */
class LLMContextBuilder {
    private val messages = mutableListOf<ChatMessage>()
    private var systemPrompt: String? = null

    fun system(prompt: String): LLMContextBuilder {
        systemPrompt = prompt
        return this
    }

    fun user(content: String): LLMContextBuilder {
        messages.add(ChatMessage.user(content))
        return this
    }

    fun assistant(content: String): LLMContextBuilder {
        messages.add(ChatMessage.assistant(content))
        return this
    }

    fun addScreenContext(screenInfo: String): LLMContextBuilder {
        systemPrompt = (systemPrompt ?: "") + "\n\nCurrent screen context:\n$screenInfo"
        return this
    }

    fun addUserPreferences(prefs: Map<String, String>): LLMContextBuilder {
        val prefsStr = prefs.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        systemPrompt = (systemPrompt ?: "") + "\n\nUser preferences:\n$prefsStr"
        return this
    }

    fun build(): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        systemPrompt?.let { result.add(ChatMessage.system(it)) }
        result.addAll(messages)
        return result
    }
}
