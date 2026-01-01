package com.augmentalis.voiceoscoreng.avu

import com.augmentalis.voiceoscoreng.functions.getCurrentTimeMillis

/**
 * Quantized Screen - Screen representation for AVU format.
 *
 * Represents a screen/activity with its actionable elements.
 *
 * @property screenHash Unique hash identifying this screen
 * @property screenTitle Display title for the screen
 * @property activityName Android activity name (nullable)
 * @property elements List of quantized elements on this screen
 * @property timestamp When this screen was captured
 */
data class QuantizedScreen(
    val screenHash: String,
    val screenTitle: String,
    val activityName: String?,
    val elements: List<QuantizedElement>,
    val timestamp: Long = getCurrentTimeMillis()
) {
    /**
     * Find element by label (case-insensitive).
     *
     * @param label Label to search for
     * @return QuantizedElement or null if not found
     */
    fun findElementByLabel(label: String): QuantizedElement? {
        return elements.find { it.label.equals(label, ignoreCase = true) }
    }

    /**
     * Find elements by type.
     *
     * @param type Element type to filter by
     * @return List of matching elements
     */
    fun findElementsByType(type: ElementType): List<QuantizedElement> {
        return elements.filter { it.type == type }
    }

    /**
     * Generate AVU SCR line format.
     *
     * Format: SCR:hash:activity:timestamp:element_count
     */
    fun toScrLine(): String {
        return "SCR:$screenHash:${activityName ?: screenTitle}:$timestamp:${elements.size}"
    }

    companion object {
        /**
         * Parse SCR line to QuantizedScreen (without elements).
         *
         * @param line SCR line
         * @return QuantizedScreen or null if invalid
         */
        fun fromScrLine(line: String): QuantizedScreen? {
            if (!line.startsWith("SCR:")) return null
            val parts = line.substring(4).split(":")
            if (parts.size < 4) return null

            return try {
                QuantizedScreen(
                    screenHash = parts[0],
                    screenTitle = parts[1],
                    activityName = parts[1],
                    elements = emptyList(),
                    timestamp = parts[2].toLong()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
