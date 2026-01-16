/**
 * ExploreScreenResult.kt - Result types for screen exploration
 *
 * Sealed class hierarchy representing possible outcomes of exploring a screen.
 * KMP-compatible - no platform dependencies.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

/**
 * Result of a single screen exploration cycle.
 *
 * Used by the DFS algorithm to determine next actions after
 * attempting to explore/click elements on a screen.
 */
sealed class ExploreScreenResult {
    /**
     * Continue to next iteration.
     * May have clicked elements or completed screen exploration.
     *
     * @property clickedCount Number of elements successfully clicked
     */
    data class Continue(val clickedCount: Int = 0) : ExploreScreenResult()

    /**
     * Screen changed after clicking an element.
     * Need to handle navigation to new screen.
     *
     * @property newScreenHash Hash of the new screen navigated to
     */
    data class Navigated(val newScreenHash: String) : ExploreScreenResult()

    /**
     * External app navigation detected.
     * Need to recover back to target app.
     *
     * @property packageName Package name of the external app
     */
    data class ExternalApp(val packageName: String) : ExploreScreenResult()

    /**
     * Screen exploration complete - all elements processed
     */
    data object ScreenComplete : ExploreScreenResult()

    /**
     * Exploration should stop due to error or termination condition
     *
     * @property reason Reason for stopping
     */
    data class Stop(val reason: String) : ExploreScreenResult()

    /**
     * Timeout reached for current screen
     */
    data object Timeout : ExploreScreenResult()
}
