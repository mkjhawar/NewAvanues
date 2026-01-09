package com.augmentalis.llm.response

import com.augmentalis.nlu.IntentClassification

/**
 * Builds context for LLM-based response generation
 *
 * Creates concise prompts optimized for mobile LLMs (50-100 tokens recommended).
 * Includes: user message, detected intent, confidence, action result.
 *
 * Design:
 * - Mobile-optimized: Short, focused prompts for resource-constrained devices
 * - Context-aware: Uses intent classification to guide response generation
 * - Fallback-ready: Supports graceful degradation to templates if LLM fails
 *
 * Created: 2025-11-10
 * Author: Claude Code (Agent 3)
 */
class LLMContextBuilder {

    /**
     * Build prompt for LLM response generation
     *
     * Creates a concise prompt that includes:
     * 1. Base system instructions (AVA identity)
     * 2. User message
     * 3. Detected intent + confidence
     * 4. Optional action result (if action was executed)
     *
     * Token budget: ~50-100 tokens input, 30-50 tokens output
     *
     * @param userMessage Original user utterance
     * @param classification NLU classification result (intent + confidence)
     * @param actionResult Optional result from action execution
     * @return Prompt string ready for LLM inference
     */
    fun buildPrompt(
        userMessage: String,
        classification: IntentClassification,
        actionResult: ActionResult? = null
    ): String {
        val builder = StringBuilder()

        // System instructions (mobile-optimized, concise)
        builder.append("You are AVA, a helpful AI assistant. ")
        builder.append("Respond naturally and concisely.\n\n")

        // User message
        builder.append("User: $userMessage\n")

        // Intent context
        builder.append("Detected intent: ${classification.intent} (confidence: ${String.format("%.2f", classification.confidence)})\n")

        // Action result (if available)
        actionResult?.let {
            if (it.success) {
                builder.append("Action result: ${it.message}\n")
            } else {
                builder.append("Action failed: ${it.errorMessage}\n")
            }
        }

        // Response instruction
        builder.append("\nAVA: ")

        return builder.toString()
    }

    /**
     * Build prompt for low-confidence scenarios (teach mode)
     *
     * When confidence is below threshold, creates a prompt that:
     * - Acknowledges uncertainty
     * - Invites user to teach AVA
     * - Remains helpful and friendly
     *
     * @param userMessage Original user utterance
     * @param classification NLU classification result
     * @return Prompt for uncertain responses
     */
    fun buildLowConfidencePrompt(
        userMessage: String,
        classification: IntentClassification
    ): String {
        return """
            |You are AVA, a helpful AI assistant learning from users.
            |
            |User: $userMessage
            |
            |You're not sure how to interpret this (confidence: ${String.format("%.2f", classification.confidence)}).
            |Respond helpfully and invite the user to teach you what they meant.
            |
            |AVA: """.trimMargin()
    }

    /**
     * Build prompt for specific intent types
     *
     * Uses intent-specific templates to guide LLM generation.
     * More natural than hardcoded templates, but maintains consistency.
     *
     * @param intent Detected intent
     * @param userMessage Original user utterance
     * @param context Additional context (time, location, etc.)
     * @return Intent-specific prompt
     */
    fun buildIntentPrompt(
        intent: String,
        userMessage: String,
        context: Map<String, String> = emptyMap()
    ): String {
        val builder = StringBuilder()

        builder.append("You are AVA. User asked: \"$userMessage\"\n")
        builder.append("Intent: $intent\n")

        // Add context if available
        if (context.isNotEmpty()) {
            builder.append("Context: ${context.entries.joinToString { "${it.key}=${it.value}" }}\n")
        }

        // Intent-specific guidance
        val guidance = when (intent) {
            "show_time" -> "Tell the time naturally and contextually (e.g., 'It's 3:45 PM - afternoon is flying by!')."
            "check_weather" -> "Explain you'll check the weather, mention you need their location."
            "set_alarm" -> "Confirm you'll set the alarm, ask for time if not specified."
            "control_lights" -> "Confirm the action, be friendly and concise."
            "unknown" -> "Acknowledge you don't understand, invite them to rephrase or teach you."
            else -> "Respond naturally and helpfully."
        }

        builder.append("Guidance: $guidance\n")
        builder.append("\nAVA: ")

        return builder.toString()
    }

    /**
     * Build conversation history context
     *
     * Formats recent conversation history for multi-turn context.
     * Truncates to stay within token budget.
     *
     * @param messages Recent message history (user + assistant)
     * @param maxMessages Maximum messages to include (default: 5 pairs)
     * @return Formatted conversation history
     */
    fun buildConversationContext(
        messages: List<Pair<String, String>>, // (user, assistant) pairs
        maxMessages: Int = 5
    ): String {
        if (messages.isEmpty()) return ""

        val builder = StringBuilder()
        builder.append("Recent conversation:\n")

        messages.takeLast(maxMessages).forEach { (user, assistant) ->
            builder.append("User: $user\n")
            builder.append("AVA: $assistant\n")
        }

        return builder.toString()
    }

    /**
     * Estimate token count for prompt
     *
     * Rough estimate: ~4 chars per token (English).
     * Used for validation before sending to LLM.
     *
     * @param prompt Prompt text
     * @return Estimated token count
     */
    fun estimateTokens(prompt: String): Int {
        return (prompt.length / 4).coerceAtLeast(1)
    }
}

/**
 * Result from action execution
 *
 * Used to inform LLM about what actions were performed.
 */
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val errorMessage: String? = null,
    val data: Map<String, Any> = emptyMap()
)
