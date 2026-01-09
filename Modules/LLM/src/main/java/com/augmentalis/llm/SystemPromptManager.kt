/**
 * System Prompt Manager for AVA AI
 *
 * Manages system prompts that are prepended to user queries to guide
 * LLM behavior. System prompts are hidden from the user but influence
 * how the model responds.
 *
 * Examples:
 * - Identity: "You are AVA, a helpful AI assistant..."
 * - Context: "The current date is 2025-11-07..."
 * - Constraints: "Keep responses concise and mobile-friendly"
 * - Instructions: "If the user asks about X, respond with Y"
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.llm

import android.content.Context
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages system prompts for LLM generation
 */
class SystemPromptManager(
    private val context: Context
) {

    /**
     * Build complete system prompt
     *
     * Combines base identity, current context, and custom instructions
     * into a single system prompt.
     *
     * @param customInstructions Optional custom instructions to append
     * @param includeDateTime Whether to include current date/time
     * @param includeAppContext Whether to include app-specific context
     * @return Complete system prompt
     */
    fun buildSystemPrompt(
        customInstructions: String? = null,
        includeDateTime: Boolean = true,
        includeAppContext: Boolean = true
    ): String {
        val sections = mutableListOf<String>()

        // 1. Core identity
        sections.add(getIdentityPrompt())

        // 2. Current date/time context
        if (includeDateTime) {
            sections.add(getDateTimeContext())
        }

        // 3. App-specific context
        if (includeAppContext) {
            sections.add(getAppContext())
        }

        // 4. Behavioral guidelines
        sections.add(getBehavioralGuidelines())

        // 5. Custom instructions (if provided)
        if (!customInstructions.isNullOrBlank()) {
            sections.add("\n$customInstructions")
        }

        val systemPrompt = sections.joinToString("\n\n")
        Timber.d("System prompt built: ${systemPrompt.length} chars")

        return systemPrompt
    }

    /**
     * Format user message with system prompt
     *
     * Prepends system prompt to user message in the format expected
     * by different models.
     *
     * @param userMessage User's input text
     * @param modelId Model identifier (for model-specific formatting)
     * @param systemPrompt System prompt to prepend
     * @return Formatted message
     */
    fun formatWithSystemPrompt(
        userMessage: String,
        modelId: String,
        systemPrompt: String = buildSystemPrompt()
    ): String {
        return when {
            modelId.contains("gemma", ignoreCase = true) -> {
                // Gemma format: <start_of_turn>user\n{prompt}<end_of_turn>\n
                "<start_of_turn>system\n$systemPrompt<end_of_turn>\n<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
            }
            modelId.contains("qwen", ignoreCase = true) -> {
                // Qwen format: <|im_start|>system\n{prompt}<|im_end|>\n
                "<|im_start|>system\n$systemPrompt<|im_end|>\n<|im_start|>user\n$userMessage<|im_end|>\n<|im_start|>assistant\n"
            }
            modelId.contains("llama", ignoreCase = true) -> {
                // Llama format: <|begin_of_text|><|start_header_id|>system<|end_header_id|>\n{prompt}
                "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n$userMessage<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
            }
            modelId.contains("phi", ignoreCase = true) -> {
                // Phi format: <|system|>\n{prompt}<|end|>\n
                "<|system|>\n$systemPrompt<|end|>\n<|user|>\n$userMessage<|end|>\n<|assistant|>\n"
            }
            modelId.contains("mistral", ignoreCase = true) -> {
                // Mistral format: [INST] {system}\n{user} [/INST]
                "[INST] $systemPrompt\n\n$userMessage [/INST]"
            }
            else -> {
                // Generic format (fallback)
                "$systemPrompt\n\nUser: $userMessage\n\nAssistant:"
            }
        }
    }

    /**
     * Core identity prompt
     */
    private fun getIdentityPrompt(): String {
        return """
            You are AVA (Augmented Virtual Assistant), a sophisticated AI companion modeled after JARVIS from Iron Man.

            Your name is AVA, and you should respond to greetings warmly:
            • When greeted with "hello", "hi", "hey ava", etc., respond naturally and ask how you can help
            • Example: "Hello! I'm AVA, your AI assistant. How can I help you today?"

            Your personality:
            • Professional yet personable - like JARVIS, you're polished but not cold
            • Proactive - anticipate user needs and offer relevant suggestions
            • Precise - provide exact information, not vague generalities
            • Efficient - keep responses concise but complete
            • Humble - acknowledge limitations honestly

            Your capabilities:
            • Voice command processing (wifi, bluetooth, media, navigation, system control)
            • Natural conversation and question-answering
            • Learning from user interactions to improve
            • Device control and automation
            • Information retrieval and task assistance

            Your limitations (be honest about these):
            • You cannot access the internet (on-device processing only)
            • You cannot perform actions you don't have permissions for
            • You cannot access user's personal data without explicit consent

            Communication style:
            • Use "I" when referring to yourself ("I can help with that")
            • Address the user directly ("You can...")
            • Be conversational but professional
            • Use technical terms when appropriate, but explain them briefly
        """.trimIndent()
    }

    /**
     * Current date/time context
     */
    private fun getDateTimeContext(): String {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())

        return "Current date and time: $currentDateTime"
    }

    /**
     * App-specific context
     */
    private fun getAppContext(): String {
        return """
            You are running on-device (local processing) for privacy.
            Keep responses concise and mobile-friendly (avoid long paragraphs).
            Format lists with bullet points (•) instead of numbers when appropriate.
        """.trimIndent()
    }

    /**
     * Behavioral guidelines
     */
    private fun getBehavioralGuidelines(): String {
        return """
            Guidelines:
            • Keep responses under 200 words when possible
            • Use simple, clear language
            • If asked to perform actions you can't do, explain your limitations politely
            • Never hallucinate or make up facts
            • If uncertain, say so

            Intent Learning System:
            • When you understand what the user is asking, include an intent hint in your response
            • Format: [INTENT: intent_name] [CONFIDENCE: 0-100]
            • Only include hints when confidence >= 70
            • Intent names: greeting, wifi_on, wifi_off, bluetooth_on, bluetooth_off, volume_up, volume_down, play_music, pause_music, navigate, search, open_app, close_app, battery_status, device_info, etc.
            • Example: "Hello! I'm AVA, your AI assistant. How can I help you today? [INTENT: greeting] [CONFIDENCE: 95]"
            • The intent markers will be removed before showing to the user, so include them freely
            • This helps me learn and respond faster next time by understanding patterns
        """.trimIndent()
    }

    /**
     * Build context-aware system prompt
     *
     * Includes dynamic context based on current app state, user activity, etc.
     *
     * @param screenContext Current screen/activity context
     * @param userContext User-specific context (preferences, history, etc.)
     * @return Context-aware system prompt
     */
    fun buildContextAwarePrompt(
        screenContext: ScreenContext? = null,
        userContext: UserContext? = null
    ): String {
        val sections = mutableListOf<String>()

        // Base prompt
        sections.add(buildSystemPrompt(includeAppContext = false))

        // Screen-specific context
        screenContext?.let { screen ->
            sections.add(getScreenSpecificPrompt(screen))
        }

        // User-specific context
        userContext?.let { user ->
            sections.add(getUserSpecificPrompt(user))
        }

        return sections.joinToString("\n\n")
    }

    /**
     * Get screen-specific prompt additions
     */
    private fun getScreenSpecificPrompt(screen: ScreenContext): String {
        return when (screen) {
            is ScreenContext.ChatScreen -> {
                "You are in a chat conversation. Engage naturally and ask follow-up questions when appropriate."
            }
            is ScreenContext.TeachScreen -> {
                "You are helping the user teach you new information. Be receptive to learning and ask clarifying questions."
            }
            is ScreenContext.SettingsScreen -> {
                "You are in the settings screen. Help the user configure their preferences and explain what each setting does."
            }
            is ScreenContext.Custom -> {
                screen.description
            }
        }
    }

    /**
     * Get user-specific prompt additions
     */
    private fun getUserSpecificPrompt(user: UserContext): String {
        val sections = mutableListOf<String>()

        user.name?.let {
            sections.add("The user's name is $it.")
        }

        user.language?.let {
            sections.add("Respond in ${it.displayName}.")
        }

        user.expertiseLevel?.let { level ->
            sections.add(when (level) {
                ExpertiseLevel.BEGINNER -> "Use simple explanations, avoid jargon."
                ExpertiseLevel.INTERMEDIATE -> "Use clear explanations with some technical terms."
                ExpertiseLevel.EXPERT -> "Use technical language freely, assume domain knowledge."
            })
        }

        return sections.joinToString(" ")
    }

    /**
     * Strip system prompt from response
     *
     * Removes system prompt markers and formatting from model output
     * to return clean user-facing text.
     *
     * @param response Raw model response
     * @param modelId Model identifier
     * @return Cleaned response
     */
    fun stripSystemPromptMarkers(response: String, modelId: String): String {
        var cleaned = response

        // Remove common markers
        val markers = listOf(
            "<start_of_turn>", "<end_of_turn>",
            "<|im_start|>", "<|im_end|>",
            "<|begin_of_text|>", "<|eot_id|>",
            "<|start_header_id|>", "<|end_header_id|>",
            "<|system|>", "<|user|>", "<|assistant|>", "<|end|>",
            "[INST]", "[/INST]",
            "system", "user", "assistant"
        )

        for (marker in markers) {
            cleaned = cleaned.replace(marker, "")
        }

        return cleaned.trim()
    }
}

/**
 * Screen context for dynamic system prompts
 */
sealed class ScreenContext {
    object ChatScreen : ScreenContext()
    object TeachScreen : ScreenContext()
    object SettingsScreen : ScreenContext()
    data class Custom(val description: String) : ScreenContext()
}

/**
 * User context for personalized system prompts
 */
data class UserContext(
    val name: String? = null,
    val language: Language? = null,
    val expertiseLevel: ExpertiseLevel? = null,
    val preferences: Map<String, String> = emptyMap()
)

/**
 * User expertise level
 */
enum class ExpertiseLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}
