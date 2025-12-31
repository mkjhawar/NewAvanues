package com.augmentalis.voiceoscoreng.avu

/**
 * Quantized Context - Full app context for AVU format.
 *
 * Represents the complete learned structure of an application
 * optimized for LLM/NLU consumption.
 *
 * @property packageName App package name
 * @property appName Human-readable app name
 * @property versionCode App version code
 * @property versionName App version name
 * @property generatedAt When this context was generated
 * @property screens List of quantized screens
 * @property navigation List of screen transitions
 * @property vocabulary Set of unique element labels
 * @property knownCommands List of learned voice commands
 */
data class QuantizedContext(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val generatedAt: Long,
    val screens: List<QuantizedScreen>,
    val navigation: List<QuantizedNavigation>,
    val vocabulary: Set<String>,
    val knownCommands: List<QuantizedCommand>
) {
    /**
     * Find screen by hash.
     *
     * @param screenHash Screen hash to find
     * @return QuantizedScreen if found, null otherwise
     */
    fun findScreen(screenHash: String): QuantizedScreen? =
        screens.find { it.screenHash == screenHash }

    /**
     * Get all navigation edges from a screen.
     *
     * @param screenHash Source screen hash
     * @return List of navigation edges from this screen
     */
    fun getNavigationFrom(screenHash: String): List<QuantizedNavigation> =
        navigation.filter { it.fromScreenHash == screenHash }

    /**
     * Find screens containing elements with matching label.
     *
     * @param label Label to search for (case-insensitive)
     * @return List of screens containing matching elements
     */
    fun findScreensWithElement(label: String): List<QuantizedScreen> =
        screens.filter { screen ->
            screen.elements.any { it.label.contains(label, ignoreCase = true) }
        }

    /**
     * Generate AVU APP line format.
     *
     * Format: APP:package:name:timestamp
     */
    fun toAppLine(): String {
        return "APP:$packageName:$appName:$generatedAt"
    }

    /**
     * Get total element count across all screens.
     */
    val totalElements: Int
        get() = screens.sumOf { it.elements.size }

    /**
     * Get total actionable element count.
     */
    val actionableElements: Int
        get() = screens.sumOf { screen ->
            screen.elements.count { it.actions.isNotBlank() }
        }

    companion object {
        /**
         * Parse APP line to extract basic context info.
         *
         * @param line APP line
         * @return Triple of (packageName, appName, timestamp) or null
         */
        fun parseAppLine(line: String): Triple<String, String, Long>? {
            if (!line.startsWith("APP:")) return null
            val parts = line.substring(4).split(":")
            if (parts.size < 3) return null

            return try {
                Triple(parts[0], parts[1], parts[2].toLong())
            } catch (e: Exception) {
                null
            }
        }
    }
}
