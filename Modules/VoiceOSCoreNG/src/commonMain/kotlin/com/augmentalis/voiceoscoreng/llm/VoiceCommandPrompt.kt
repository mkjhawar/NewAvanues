/**
 * VoiceCommandPrompt.kt - LLM Prompt Templates for Voice Commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Prompt templates for interpreting voice commands via LLM.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * Prompt templates for voice command interpretation.
 */
object VoiceCommandPrompt {

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
        availableCommands.take(50).forEach { cmd ->
            appendLine("- $cmd")
        }
        if (availableCommands.size > 50) {
            appendLine("... and ${availableCommands.size - 50} more commands")
        }
        appendLine()
        if (nluSchema.isNotBlank()) {
            appendLine("## Command Schema")
            appendLine(nluSchema.take(500))
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
     * @param response Raw LLM response text
     * @param availableCommands List of valid command phrases
     * @return Matched command phrase or null if no match
     */
    fun parseResponse(response: String, availableCommands: List<String>): String? {
        val trimmed = response.trim()
            .removePrefix("Response:")
            .trim()
            .lowercase()
            .removeSuffix(".")
            .removeSuffix("!")
            .trim()

        // Check for explicit no match
        if (trimmed == "no_match" ||
            trimmed.contains("no match") ||
            trimmed.contains("cannot match") ||
            trimmed.contains("unable to match") ||
            trimmed.contains("no appropriate command")) {
            return null
        }

        // Try exact match (case-insensitive)
        val exactMatch = availableCommands.find { cmd ->
            cmd.lowercase() == trimmed
        }
        if (exactMatch != null) return exactMatch

        // Try finding command contained in response
        val containedMatch = availableCommands.find { cmd ->
            trimmed.contains(cmd.lowercase())
        }
        if (containedMatch != null) return containedMatch

        // Try finding response contained in command (for partial matches)
        val partialMatch = availableCommands.find { cmd ->
            cmd.lowercase().contains(trimmed) && trimmed.length >= 3
        }
        if (partialMatch != null) return partialMatch

        // No match found
        return null
    }

    /**
     * Create a concise prompt for quick interpretation.
     * Uses less tokens for faster response.
     */
    fun createConcise(
        utterance: String,
        availableCommands: List<String>
    ): String = buildString {
        appendLine("Commands: ${availableCommands.take(30).joinToString(", ")}")
        appendLine("User: \"$utterance\"")
        append("Match (or NO_MATCH): ")
    }
}
