package com.augmentalis.voiceoscoreng.avu

import com.augmentalis.voiceoscoreng.functions.getCurrentTimeMillis

/**
 * Quantized Navigation - Screen transition for AVU format.
 *
 * Represents a navigation path between two screens triggered
 * by a UI element action.
 *
 * @property fromScreenHash Source screen hash
 * @property toScreenHash Destination screen hash
 * @property triggerLabel Label of the triggering element
 * @property triggerVuid VUID of the triggering element
 * @property timestamp When this navigation was recorded
 */
data class QuantizedNavigation(
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerLabel: String,
    val triggerVuid: String,
    val timestamp: Long = getCurrentTimeMillis()
) {
    /**
     * Generate AVU NAV line format.
     *
     * Format: NAV:from_hash:to_hash:trigger_uuid:trigger_label:timestamp
     */
    fun toNavLine(): String {
        return "NAV:$fromScreenHash:$toScreenHash:$triggerVuid:$triggerLabel:$timestamp"
    }

    companion object {
        /**
         * Parse NAV line to QuantizedNavigation.
         *
         * @param line NAV line
         * @return QuantizedNavigation or null if invalid
         */
        fun fromNavLine(line: String): QuantizedNavigation? {
            if (!line.startsWith("NAV:")) return null
            val parts = line.substring(4).split(":")
            if (parts.size < 5) return null

            return try {
                QuantizedNavigation(
                    fromScreenHash = parts[0],
                    toScreenHash = parts[1],
                    triggerVuid = parts[2],
                    triggerLabel = parts[3],
                    timestamp = parts[4].toLong()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
