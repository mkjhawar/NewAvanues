package com.augmentalis.voiceoscoreng.avu

/**
 * Quantized Command - Voice command representation for AVU format.
 *
 * Represents a learned voice command that triggers an action
 * on a UI element.
 *
 * @property uuid Command unique identifier
 * @property phrase Voice phrase that triggers this command
 * @property actionType Type of action to perform
 * @property targetVuid Target element VUID (nullable for navigation)
 * @property confidence Confidence score (0.0 - 1.0)
 */
data class QuantizedCommand(
    val uuid: String = "",
    val phrase: String,
    val actionType: CommandActionType,
    val targetVuid: String?,
    val confidence: Float
) {
    /**
     * Generate AVU CMD line format.
     *
     * Format: CMD:uuid:trigger:action:element_uuid:confidence
     */
    fun toCmdLine(): String {
        val formattedConfidence = "%.2f".format(confidence)
        return "CMD:$uuid:$phrase:${actionType.name}:${targetVuid ?: ""}:$formattedConfidence"
    }

    companion object {
        /**
         * Parse CMD line to QuantizedCommand.
         *
         * @param line CMD line (e.g., "CMD:uuid:phrase:CLICK:vuid:0.95")
         * @return QuantizedCommand or null if invalid
         */
        fun fromCmdLine(line: String): QuantizedCommand? {
            if (!line.startsWith("CMD:")) return null
            val parts = line.substring(4).split(":")
            if (parts.size < 5) return null

            return try {
                QuantizedCommand(
                    uuid = parts[0],
                    phrase = parts[1],
                    actionType = CommandActionType.fromString(parts[2]),
                    targetVuid = parts[3].takeIf { it.isNotBlank() },
                    confidence = parts[4].toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
