package com.augmentalis.voiceoscoreng.common

/**
 * Quantized Command - Voice command representation for AVU format.
 *
 * Represents a learned voice command that triggers an action
 * on a UI element.
 *
 * @property avid Command unique identifier (AVID format)
 * @property phrase Voice phrase that triggers this command
 * @property aliases Alternative phrases that trigger this command
 * @property actionType Type of action to perform
 * @property targetAvid Target element AVID fingerprint (nullable for navigation)
 * @property confidence Confidence score (0.0 - 1.0)
 * @property metadata Additional data (packageName, screenId, appVersion, etc.)
 */
data class QuantizedCommand(
    val avid: String = "",
    val phrase: String,
    val aliases: List<String> = emptyList(),
    val actionType: CommandActionType,
    val targetAvid: String?,
    val confidence: Float,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Legacy alias for avid (deprecated, use avid directly).
     */
    @Deprecated("Use avid instead", ReplaceWith("avid"))
    val uuid: String get() = avid

    /**
     * Legacy alias for avid (deprecated, use avid directly).
     */
    @Deprecated("Use avid instead", ReplaceWith("avid"))
    val vuid: String get() = avid

    /**
     * Legacy alias for targetAvid (deprecated, use targetAvid directly).
     */
    @Deprecated("Use targetAvid instead", ReplaceWith("targetAvid"))
    val targetVuid: String? get() = targetAvid

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
     * Format: CMD:avid:trigger:action:element_avid:confidence
     */
    fun toCmdLine(): String {
        val formattedConfidence = formatFloat(confidence)
        return "CMD:$avid:$phrase:${actionType.name}:${targetAvid ?: ""}:$formattedConfidence"
    }

    private fun formatFloat(value: Float): String {
        val rounded = (value * 100).toInt()
        val intPart = rounded / 100
        val decPart = rounded % 100
        return "$intPart.${decPart.toString().padStart(2, '0')}"
    }

    companion object {
        /**
         * Create a QuantizedCommand with required packageName validation.
         *
         * This is the recommended factory method for creating commands as it
         * enforces that packageName is always present in metadata.
         *
         * @param avid Command unique identifier (AVID format)
         * @param phrase Voice phrase that triggers this command
         * @param actionType Type of action to perform
         * @param packageName Package name of the app (required)
         * @param targetAvid Target element AVID fingerprint (nullable for navigation)
         * @param confidence Confidence score (0.0 - 1.0)
         * @param screenId Optional screen ID
         * @param appVersion Optional app version
         * @param additionalMetadata Additional metadata entries
         * @return QuantizedCommand with validated metadata
         * @throws IllegalArgumentException if packageName is blank
         */
        fun create(
            avid: String,
            phrase: String,
            actionType: CommandActionType,
            packageName: String,
            targetAvid: String? = null,
            confidence: Float = 1.0f,
            aliases: List<String> = emptyList(),
            screenId: String? = null,
            appVersion: String? = null,
            additionalMetadata: Map<String, String> = emptyMap()
        ): QuantizedCommand {
            require(packageName.isNotBlank()) {
                "packageName is required for QuantizedCommand creation"
            }
            require(phrase.isNotBlank()) {
                "phrase cannot be blank"
            }
            require(confidence in 0.0f..1.0f) {
                "confidence must be between 0.0 and 1.0"
            }

            val metadata = buildMap {
                put("packageName", packageName)
                screenId?.let { put("screenId", it) }
                appVersion?.let { put("appVersion", it) }
                putAll(additionalMetadata)
            }

            return QuantizedCommand(
                avid = avid,
                phrase = phrase,
                aliases = aliases,
                actionType = actionType,
                targetAvid = targetAvid,
                confidence = confidence,
                metadata = metadata
            )
        }

        /**
         * Parse CMD line to QuantizedCommand.
         *
         * @param line CMD line (e.g., "CMD:avid:phrase:CLICK:target_avid:0.95")
         * @return QuantizedCommand or null if invalid
         */
        fun fromCmdLine(line: String): QuantizedCommand? {
            if (!line.startsWith("CMD:")) return null
            val parts = line.substring(4).split(":")
            if (parts.size < 5) return null

            return try {
                QuantizedCommand(
                    avid = parts[0],
                    phrase = parts[1],
                    actionType = CommandActionType.fromString(parts[2]),
                    targetAvid = parts[3].takeIf { it.isNotBlank() },
                    confidence = parts[4].toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Parse CMD line to QuantizedCommand with packageName.
         *
         * @param line CMD line (e.g., "CMD:avid:phrase:CLICK:target_avid:0.95")
         * @param packageName Package name to attach to the command
         * @return QuantizedCommand or null if invalid
         */
        fun fromCmdLine(line: String, packageName: String): QuantizedCommand? {
            val command = fromCmdLine(line) ?: return null
            return command.withMetadata("packageName", packageName)
        }
    }

    /**
     * Validates that this command has required metadata.
     *
     * @return true if packageName is present
     */
    fun isValid(): Boolean = !packageName.isNullOrBlank()

    /**
     * Returns validation errors for this command.
     *
     * @return List of validation error messages, empty if valid
     */
    fun validationErrors(): List<String> = buildList {
        if (packageName.isNullOrBlank()) add("Missing packageName in metadata")
        if (phrase.isBlank()) add("Phrase cannot be blank")
        if (avid.isBlank()) add("AVID cannot be blank")
    }
}
