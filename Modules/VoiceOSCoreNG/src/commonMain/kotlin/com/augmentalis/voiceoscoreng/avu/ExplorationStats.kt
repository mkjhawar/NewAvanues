package com.augmentalis.voiceoscoreng.avu

/**
 * Exploration Stats - Statistics for AVU STA line.
 *
 * Captures exploration metrics for reporting and analysis.
 *
 * @property screenCount Number of screens explored
 * @property elementCount Total elements discovered
 * @property commandCount Generated commands count
 * @property avgDepth Average UI hierarchy depth
 * @property maxDepth Maximum UI hierarchy depth
 * @property coverage Exploration coverage (0.0 - 1.0)
 * @property durationMs Exploration duration in milliseconds
 */
data class ExplorationStats(
    val screenCount: Int,
    val elementCount: Int,
    val commandCount: Int,
    val avgDepth: Float,
    val maxDepth: Int,
    val coverage: Float,
    val durationMs: Long = 0L
) {
    /**
     * Generate AVU STA line format.
     *
     * Format: STA:screens:elements:commands:avg_depth:max_depth:coverage
     */
    fun toStaLine(): String {
        val formattedAvgDepth = formatFloat(avgDepth)
        val formattedCoverage = formatFloat(coverage)
        return "STA:$screenCount:$elementCount:$commandCount:$formattedAvgDepth:$maxDepth:$formattedCoverage"
    }

    private fun formatFloat(value: Float): String {
        val rounded = (value * 100).toInt()
        val intPart = rounded / 100
        val decPart = rounded % 100
        return "$intPart.${decPart.toString().padStart(2, '0')}"
    }

    companion object {
        /**
         * Parse STA line to ExplorationStats.
         *
         * @param line STA line (e.g., "STA:10:150:45:3.50:6:0.85")
         * @return ExplorationStats or null if invalid
         */
        fun fromStaLine(line: String): ExplorationStats? {
            if (!line.startsWith("STA:")) return null
            val parts = line.substring(4).split(":")
            if (parts.size < 6) return null

            return try {
                ExplorationStats(
                    screenCount = parts[0].toInt(),
                    elementCount = parts[1].toInt(),
                    commandCount = parts[2].toInt(),
                    avgDepth = parts[3].toFloat(),
                    maxDepth = parts[4].toInt(),
                    coverage = parts[5].toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Create empty stats.
         */
        fun empty(): ExplorationStats = ExplorationStats(
            screenCount = 0,
            elementCount = 0,
            commandCount = 0,
            avgDepth = 0f,
            maxDepth = 0,
            coverage = 0f
        )
    }
}
