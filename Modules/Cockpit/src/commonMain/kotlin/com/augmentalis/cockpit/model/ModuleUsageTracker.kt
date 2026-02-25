/**
 * ModuleUsageTracker.kt - Usage-based module ranking for spatial layouts
 *
 * Tracks module launch frequency and provides ranked lists with depth tiers.
 * Drives the SpaceAvanue shell's island layout: frequently-used modules get
 * larger islands closer to center, rarely-used modules shrink to the periphery.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.cockpit.model

/**
 * Depth tier for spatial island positioning.
 * Determines island size and distance from canvas center.
 *
 * @param sizeDp Island size on phone/tablet displays
 * @param glassSizeDp Island size on glass/HUD displays
 */
enum class IslandDepthTier(val sizeDp: Int, val glassSizeDp: Int) {
    /** Top 3 by usage — largest, center area */
    NEAR(100, 72),
    /** Next 4 by usage — medium, first ring */
    MID(80, 60),
    /** Remaining — smallest, outer ring */
    FAR(64, 48);
}

/**
 * A module paired with its usage score and computed depth tier.
 */
data class RankedModule(
    val module: DashboardModule,
    val usageScore: Float,
    val tier: IslandDepthTier,
)

/**
 * Tracks module usage and provides ranked lists for spatial layouts.
 *
 * Usage scoring drives the SpaceAvanue shell's island layout:
 * - Top 3 modules by usage → [IslandDepthTier.NEAR] (large islands, center of canvas)
 * - Next 4 modules → [IslandDepthTier.MID] (medium islands, first ring)
 * - Remaining → [IslandDepthTier.FAR] (small islands, outer ring)
 *
 * This creates a spatial memory model where frequently-used modules
 * occupy prominent positions. The layout self-organizes over time.
 *
 * Currently in-memory with sensible seed defaults. Will be persisted
 * to DataStore in a future phase for cross-session continuity.
 */
class ModuleUsageTracker {

    /**
     * In-memory usage scores (module ID → cumulative score).
     * Seeded with reasonable defaults so the first launch shows a
     * meaningful spatial hierarchy rather than a flat grid.
     */
    private val usageScores = mutableMapOf(
        "webavanue" to 87f,
        "noteavanue" to 72f,
        "voiceavanue" to 50f,
        "pdfavanue" to 45f,
        "videoavanue" to 38f,
        "photoavanue" to 25f,
        "voicecursor" to 20f,
        "annotationavanue" to 15f,
        "fileavanue" to 12f,
        "imageavanue" to 8f,
        "remotecast" to 5f,
    )

    /**
     * Record a module launch, incrementing its usage score.
     * Each launch adds a fixed increment; decay is not yet implemented.
     */
    fun recordLaunch(moduleId: String) {
        val current = usageScores.getOrElse(moduleId) { 0f }
        usageScores[moduleId] = current + 10f
    }

    /**
     * Get usage score for a specific module.
     */
    fun getScore(moduleId: String): Float =
        usageScores.getOrElse(moduleId) { 0f }

    /**
     * Get all usage scores as an immutable snapshot.
     * Used to pass scores through the state pipeline without exposing
     * the mutable tracker directly.
     */
    fun getScoresSnapshot(): Map<String, Float> =
        usageScores.toMap()

    /**
     * Rank all modules by usage and assign depth tiers.
     *
     * Tier assignment:
     * - Indices 0–2 → [IslandDepthTier.NEAR]
     * - Indices 3–6 → [IslandDepthTier.MID]
     * - Indices 7+ → [IslandDepthTier.FAR]
     *
     * @param modules The full list of dashboard modules
     * @return Modules sorted by usage (highest first) with tier assignments
     */
    fun rankModules(modules: List<DashboardModule>): List<RankedModule> {
        val sorted = modules.sortedByDescending { usageScores.getOrElse(it.id) { 0f } }
        return sorted.mapIndexed { index, module ->
            val tier = when {
                index < 3 -> IslandDepthTier.NEAR
                index < 7 -> IslandDepthTier.MID
                else -> IslandDepthTier.FAR
            }
            RankedModule(
                module = module,
                usageScore = usageScores.getOrElse(module.id) { 0f },
                tier = tier,
            )
        }
    }

    companion object {
        /**
         * Compute depth tier for a module given its rank index (0-based).
         * Utility for layouts that don't use the full [rankModules] pipeline.
         */
        fun tierForIndex(index: Int): IslandDepthTier = when {
            index < 3 -> IslandDepthTier.NEAR
            index < 7 -> IslandDepthTier.MID
            else -> IslandDepthTier.FAR
        }
    }
}
