package com.augmentalis.avanues.avaui.voice

/**
 * Routes voice commands to app actions.
 */
class VoiceCommandRouter {
    private val commands = mutableMapOf<String, VoiceCommand>()

    /**
     * Register a voice command.
     */
    fun register(trigger: String, action: String, componentId: String? = null) {
        val command = VoiceCommand(
            trigger = trigger.lowercase(),
            action = action,
            componentId = componentId
        )
        commands[trigger.lowercase()] = command
    }

    /**
     * Unregister a voice command.
     */
    fun unregister(trigger: String) {
        commands.remove(trigger.lowercase())
    }

    /**
     * Match voice input to command.
     */
    fun match(voiceInput: String): CommandMatch? {
        val normalized = voiceInput.lowercase().trim()

        // Exact match
        commands[normalized]?.let {
            return CommandMatch(it, 1.0f)
        }

        // Fuzzy match
        val matches = commands.values.mapNotNull { command ->
            val similarity = calculateSimilarity(normalized, command.trigger)
            if (similarity > 0.7f) {
                CommandMatch(command, similarity)
            } else null
        }

        return matches.maxByOrNull { it.confidence }
    }

    /**
     * Get all registered commands.
     */
    fun getAll(): List<VoiceCommand> {
        return commands.values.toList()
    }

    /**
     * Clear all commands.
     */
    fun clear() {
        commands.clear()
    }

    private fun calculateSimilarity(a: String, b: String): Float {
        // Simple word-based similarity
        val aWords = a.split(" ").toSet()
        val bWords = b.split(" ").toSet()

        val intersection = aWords.intersect(bWords).size
        val union = aWords.union(bWords).size

        return if (union > 0) {
            intersection.toFloat() / union.toFloat()
        } else {
            0f
        }
    }
}

data class VoiceCommand(
    val trigger: String,
    val action: String,
    val componentId: String?
)

data class CommandMatch(
    val command: VoiceCommand,
    val confidence: Float
)
