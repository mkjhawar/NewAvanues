/**
 * ExplorationConfig.kt - Configurable exploration parameters
 *
 * Contains all tunable parameters for app exploration.
 * KMP-compatible - provides defaults that can be overridden per-platform.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

/**
 * Configuration for exploration behavior.
 *
 * All timing values are in milliseconds unless otherwise noted.
 * These defaults are tuned for typical Android app exploration.
 */
data class ExplorationConfig(
    // ═══════════════════════════════════════════════════════════════════
    // TIMING
    // ═══════════════════════════════════════════════════════════════════

    /** Delay after clicking an element (ms) */
    val clickDelayMs: Long = 300L,

    /** Delay after scrolling (ms) */
    val scrollDelayMs: Long = 500L,

    /** Delay for screen processing/settling (ms) */
    val screenProcessingDelayMs: Long = 1000L,

    /** Tap duration for gesture clicks (ms) */
    val tapDurationMs: Long = 50L,

    // ═══════════════════════════════════════════════════════════════════
    // LIMITS
    // ═══════════════════════════════════════════════════════════════════

    /** Maximum depth for DFS exploration */
    val maxExplorationDepth: Int = 15,

    /** Maximum click attempts per element */
    val maxClickAttempts: Int = 3,

    /** Maximum consecutive click failures before moving on */
    val maxConsecutiveClickFailures: Int = 5,

    /** Maximum times a navigation path can be revisited */
    val maxPathRevisits: Int = 2,

    /** Maximum BACK press attempts for app recovery */
    val maxBackPressAttempts: Int = 5,

    /** Global exploration timeout (ms) - default 30 minutes */
    val explorationTimeoutMs: Long = 30 * 60 * 1000L,

    /** Per-screen exploration timeout (ms) - default 2 minutes */
    val screenExplorationTimeoutMs: Long = 2 * 60 * 1000L,

    // ═══════════════════════════════════════════════════════════════════
    // THRESHOLDS
    // ═══════════════════════════════════════════════════════════════════

    /** Completeness percentage threshold for "fully learned" */
    val completenessThresholdPercent: Float = 85f,

    /** Minimum alias text length to accept */
    val minAliasTextLength: Int = 2,

    /** Bounds tolerance for fuzzy node matching (pixels) */
    val boundsTolerance: Int = 5,

    /** Estimated initial screen count for progress calculation */
    val estimatedInitialScreenCount: Int = 10,

    // ═══════════════════════════════════════════════════════════════════
    // BEHAVIOR
    // ═══════════════════════════════════════════════════════════════════

    /** Enable verbose logging */
    val verboseLogging: Boolean = false,

    /** Enable developer mode (debug overlays) */
    val developerMode: Boolean = false,

    /** Enable sound notifications */
    val soundEnabled: Boolean = true,

    /** Skip critical dangerous elements entirely */
    val skipCriticalDangerous: Boolean = true,

    /** Click dangerous elements last */
    val clickDangerousLast: Boolean = true
) {
    companion object {
        /**
         * Default configuration
         */
        val DEFAULT = ExplorationConfig()

        /**
         * Fast configuration for testing (shorter delays)
         */
        val FAST = ExplorationConfig(
            clickDelayMs = 100L,
            scrollDelayMs = 200L,
            screenProcessingDelayMs = 500L,
            explorationTimeoutMs = 5 * 60 * 1000L,
            screenExplorationTimeoutMs = 30 * 1000L
        )

        /**
         * Thorough configuration (longer delays, more retries)
         */
        val THOROUGH = ExplorationConfig(
            clickDelayMs = 500L,
            scrollDelayMs = 800L,
            screenProcessingDelayMs = 1500L,
            maxClickAttempts = 5,
            maxConsecutiveClickFailures = 8,
            maxPathRevisits = 3
        )
    }
}
