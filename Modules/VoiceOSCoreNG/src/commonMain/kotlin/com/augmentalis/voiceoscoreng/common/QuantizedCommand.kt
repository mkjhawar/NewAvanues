package com.augmentalis.voiceoscoreng.common

/**
 * Quantized Command - Voice command representation for AVU format.
 *
 * Represents a learned voice command that triggers an action
 * on a UI element.
 *
 * @property uuid Command unique identifier (same as vuid for consistency)
 * @property phrase Voice phrase that triggers this command
 * @property actionType Type of action to perform
 * @property targetVuid Target element VUID (nullable for navigation)
 * @property confidence Confidence score (0.0 - 1.0)
 * @property metadata Additional data (packageName, screenId, appVersion, etc.)
 */
data class QuantizedCommand(
    val uuid: String = "",
    val phrase: String,
    val actionType: CommandActionType,
    val targetVuid: String?,
    val confidence: Float,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * VUID alias for uuid (for repository consistency).
     */
    val vuid: String get() = uuid

    /**
     * Package name from metadata.
     */
    val packageName: String? get() = metadata["packageName"]

    /**
     * Screen ID from metadata.
     */
    val screenId: String? get() = metadata["screenId"]

    /**
     * App version from metadata.
     */
    val appVersion: String? get() = metadata["appVersion"]

    /**
     * Create a copy with additional metadata.
     */
    fun withMetadata(key: String, value: String): QuantizedCommand =
        copy(metadata = metadata + (key to value))

    /**
     * Create a copy with multiple metadata entries.
     */
    fun withMetadata(entries: Map<String, String>): QuantizedCommand =
        copy(metadata = metadata + entries)
    /**
     * Generate AVU CMD line format.
     *
     * Format: CMD:uuid:trigger:action:element_uuid:confidence
     */
    fun toCmdLine(): String {
        val formattedConfidence = formatFloat(confidence)
        return "CMD:$uuid:$phrase:${actionType.name}:${targetVuid ?: ""}:$formattedConfidence"
    }

    private fun formatFloat(value: Float): String {
        val rounded = (value * 100).toInt()
        val intPart = rounded / 100
        val decPart = rounded % 100
        return "$intPart.${decPart.toString().padStart(2, '0')}"
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
