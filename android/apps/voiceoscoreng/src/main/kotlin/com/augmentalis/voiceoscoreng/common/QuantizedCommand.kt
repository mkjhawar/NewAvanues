package com.augmentalis.voiceoscoreng.common

/**
 * Represents a quantized voice command mapped to an action.
 *
 * @property phrase The voice phrase that triggers this command
 * @property action The action type to execute
 * @property targetId Optional target element ID (AVID)
 * @property parameters Optional action parameters
 */
data class QuantizedCommand(
    val phrase: String,
    val action: String,
    val targetId: String? = null,
    val parameters: Map<String, String> = emptyMap()
)
