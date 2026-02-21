package com.augmentalis.llm

/**
 * Result of LLM command interpretation.
 *
 * Used by VoiceOS to interpret voice commands via LLM when the NLU
 * classification confidence is low or when dealing with complex commands.
 *
 * Created: 2026-01-19
 */
sealed class CommandInterpretationResult {
    /**
     * Successfully interpreted the command.
     *
     * @param matchedCommand The command that was matched (e.g., "open_app", "send_message")
     * @param confidence Confidence score from the LLM (0.0 to 1.0)
     * @param reasoning Optional LLM reasoning for the interpretation
     */
    data class Interpreted(
        val matchedCommand: String,
        val confidence: Float,
        val reasoning: String? = null
    ) : CommandInterpretationResult()

    /**
     * No matching command found.
     *
     * The utterance could not be mapped to any available command.
     * May indicate the user needs clarification or the command is unsupported.
     */
    data object NoMatch : CommandInterpretationResult()

    /**
     * Error during interpretation.
     *
     * @param message Human-readable error description
     */
    data class Error(val message: String) : CommandInterpretationResult()
}

/**
 * Result of command clarification dialog.
 *
 * Used when multiple commands could match the user's intent and
 * the LLM needs to help disambiguate between them.
 */
data class ClarificationResult(
    /**
     * The command selected by the LLM, or null if clarification is needed
     */
    val selectedCommand: String?,

    /**
     * Confidence in the selection (0.0 to 1.0)
     */
    val confidence: Float,

    /**
     * Question to ask the user if selectedCommand is null
     * Example: "Did you mean to open the camera app or take a photo?"
     */
    val clarificationQuestion: String? = null
)
