/**
 * VoiceCommandPrompt.kt - LLM Prompt Templates for Voice Commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-09
 *
 * Prompt templates for interpreting voice commands via LLM.
 */
package com.augmentalis.voiceoscore

/**
 * Prompt templates for voice command interpretation.
 */
object VoiceCommandPrompt {

    /**
     * Maximum number of commands to include in the full prompt.
     * Limited to prevent token overflow and maintain response quality.
     * 50 commands at ~10 chars average = ~500 tokens, leaving room for
     * system prompt, schema, and response.
     */
    const val MAX_COMMANDS_FULL_PROMPT = 50

    /**
     * Maximum number of commands to include in concise prompt.
     * Smaller limit for faster, cheaper inference.
     */
    const val MAX_COMMANDS_CONCISE_PROMPT = 30

    /**
     * Maximum characters for NLU schema in prompt.
     * Limited to prevent overwhelming the LLM with context.
     * 500 chars is roughly 125 tokens.
     */
    const val MAX_SCHEMA_CHARS = 500

    /**
     * Minimum match length for partial matching.
     * Prevents matching on very short strings like "a" or "go".
     */
    const val MIN_PARTIAL_MATCH_LENGTH = 3

    /**
     * Create a prompt for interpreting a voice command.
     *
     * @param utterance The user's spoken input
     * @param nluSchema Schema describing available commands
     * @param availableCommands List of valid command phrases
     * @return Formatted prompt string for LLM
     */
    fun create(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): String = buildString {
        appendLine("You are a voice command interpreter for an accessibility app.")
        appendLine("Your task is to match user speech to the most appropriate command.")
        appendLine()
        appendLine("## Available Commands")
        availableCommands.take(MAX_COMMANDS_FULL_PROMPT).forEach { cmd ->
            appendLine("- $cmd")
        }
        if (availableCommands.size > MAX_COMMANDS_FULL_PROMPT) {
            appendLine("... and ${availableCommands.size - MAX_COMMANDS_FULL_PROMPT} more commands")
        }
        appendLine()
        if (nluSchema.isNotBlank()) {
            appendLine("## Command Schema")
            appendLine(nluSchema.take(MAX_SCHEMA_CHARS))
            appendLine()
        }
        appendLine("## User Input")
        appendLine("\"$utterance\"")
        appendLine()
        appendLine("## Instructions")
        appendLine("Match the user input to the most appropriate command from the list.")
        appendLine("Consider synonyms, paraphrases, and natural language variations.")
        appendLine("Respond with ONLY the exact command phrase, nothing else.")
        appendLine("If no command matches, respond with: NO_MATCH")
        appendLine()
        append("Response: ")
    }

    /**
     * Parse the LLM response to extract the matched command.
     *
     * Uses a multi-stage matching strategy:
     * 1. Exact match (highest confidence)
     * 2. Command contained in response
     * 3. Response contained in command (partial match)
     *
     * @param response Raw LLM response text
     * @param availableCommands List of valid command phrases
     * @param debug If true, prints debug information about parse attempts
     * @return Matched command phrase or null if no match
     */
    fun parseResponse(
        response: String,
        availableCommands: List<String>,
        debug: Boolean = false
    ): String? {
        // Preserve original for logging, normalize for matching
        val original = response.trim()
        val normalized = original
            .removePrefix("Response:")
            .trim()
            .removeSuffix(".")
            .removeSuffix("!")
            .trim()
        val lowercased = normalized.lowercase()

        if (debug) {
            println("[VoiceCommandPrompt] Parsing response: '$original' -> normalized: '$normalized'")
        }

        // Check for explicit no match indicators
        val noMatchIndicators = listOf(
            "no_match", "no match", "cannot match", "unable to match",
            "no appropriate command", "none of the commands", "doesn't match",
            "does not match", "no suitable"
        )
        if (noMatchIndicators.any { lowercased.contains(it) }) {
            if (debug) println("[VoiceCommandPrompt] Explicit NO_MATCH detected")
            return null
        }

        // Stage 1: Exact match (case-insensitive) - highest confidence
        val exactMatch = availableCommands.find { cmd ->
            cmd.equals(normalized, ignoreCase = true)
        }
        if (exactMatch != null) {
            if (debug) println("[VoiceCommandPrompt] Exact match: '$exactMatch'")
            return exactMatch
        }

        // Stage 2: Command contained in response (LLM added extra text)
        val containedMatch = availableCommands.find { cmd ->
            lowercased.contains(cmd.lowercase())
        }
        if (containedMatch != null) {
            if (debug) println("[VoiceCommandPrompt] Contained match: '$containedMatch'")
            return containedMatch
        }

        // Stage 3: Response contained in command (partial/abbreviated match)
        if (normalized.length >= MIN_PARTIAL_MATCH_LENGTH) {
            val partialMatch = availableCommands.find { cmd ->
                cmd.lowercase().contains(lowercased)
            }
            if (partialMatch != null) {
                if (debug) println("[VoiceCommandPrompt] Partial match: '$partialMatch'")
                return partialMatch
            }
        }

        // No match found
        if (debug) {
            println("[VoiceCommandPrompt] No match found for: '$normalized'")
            println("[VoiceCommandPrompt] Available commands (first 10): ${availableCommands.take(10)}")
        }
        return null
    }

    /**
     * Parse response with debug logging enabled.
     * Convenience wrapper for debugging parse issues.
     */
    fun parseResponseDebug(response: String, availableCommands: List<String>): String? =
        parseResponse(response, availableCommands, debug = true)

    /**
     * Create a concise prompt for quick interpretation.
     * Uses fewer tokens for faster, cheaper inference.
     *
     * @param utterance The user's spoken input
     * @param availableCommands List of valid command phrases
     * @return Concise prompt string for LLM
     */
    fun createConcise(
        utterance: String,
        availableCommands: List<String>
    ): String = buildString {
        appendLine("Commands: ${availableCommands.take(MAX_COMMANDS_CONCISE_PROMPT).joinToString(", ")}")
        appendLine("User: \"$utterance\"")
        append("Match (or NO_MATCH): ")
    }
}
