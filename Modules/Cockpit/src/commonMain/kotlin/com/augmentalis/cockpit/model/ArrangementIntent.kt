package com.augmentalis.cockpit.model

import com.augmentalis.avanueui.display.DisplayProfile
import kotlinx.serialization.Serializable

/**
 * User-facing layout intents that abstract away the 15 raw [LayoutMode] values.
 *
 * Instead of asking users to choose between FREEFORM, GRID, T_PANEL, SPATIAL_DICE, etc.,
 * the UI presents 4 natural intents that map to voice commands:
 * - "Focus" → single fullscreen frame
 * - "Compare" → two frames side by side
 * - "Overview" → 3-6 frames in an auto-arranged grid
 * - "Present" → showcase/carousel mode for presentations
 *
 * The [IntentResolver] maps each intent to the optimal [LayoutMode] based on
 * frame count and display profile.
 */
@Serializable
enum class ArrangementIntent(
    val displayLabel: String,
    val voiceCommand: String,
    val iconName: String,
    val description: String,
) {
    FOCUS(
        displayLabel = "Focus",
        voiceCommand = "focus",
        iconName = "fullscreen",
        description = "Single frame fills the screen"
    ),
    COMPARE(
        displayLabel = "Compare",
        voiceCommand = "compare",
        iconName = "vertical_split",
        description = "Two frames side by side"
    ),
    OVERVIEW(
        displayLabel = "Overview",
        voiceCommand = "overview",
        iconName = "grid_view",
        description = "All frames in an auto-arranged grid"
    ),
    PRESENT(
        displayLabel = "Present",
        voiceCommand = "present",
        iconName = "slideshow",
        description = "Showcase mode for presentations"
    );

    companion object {
        fun fromVoiceCommand(command: String): ArrangementIntent? =
            entries.firstOrNull { it.voiceCommand.equals(command, ignoreCase = true) }

        fun fromString(value: String): ArrangementIntent? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * Resolves an [ArrangementIntent] to the optimal [LayoutMode] based on context.
 *
 * Takes into account:
 * - The user's intent (Focus, Compare, Overview, Present)
 * - How many frames are in the session
 * - The display profile (phone, tablet, glass, desktop)
 * - Whether spatial canvas is available
 *
 * This is the bridge between the simplified UI (4 intents) and the full
 * layout engine (15 modes). Users interact with intents; the system resolves
 * to specific layout modes internally.
 */
object IntentResolver {

    /**
     * Resolve an arrangement intent to the best layout mode.
     *
     * @param intent The user's arrangement intent
     * @param frameCount Number of frames in the current session
     * @param displayProfile Device display profile for adaptive layout
     * @param spatialAvailable Whether spatial canvas (head tracking) is available
     * @return The optimal [LayoutMode] for the given context
     */
    fun resolve(
        intent: ArrangementIntent,
        frameCount: Int,
        displayProfile: DisplayProfile = DisplayProfile.PHONE,
        spatialAvailable: Boolean = false,
    ): LayoutMode = when (intent) {
        ArrangementIntent.FOCUS -> resolveForFocus()

        ArrangementIntent.COMPARE -> resolveForCompare(frameCount, displayProfile)

        ArrangementIntent.OVERVIEW -> resolveForOverview(frameCount, displayProfile, spatialAvailable)

        ArrangementIntent.PRESENT -> resolveForPresent(frameCount, displayProfile)
    }

    /**
     * Infer the most likely [ArrangementIntent] from an existing [LayoutMode].
     * Used when restoring sessions or when the user manually changes layout mode.
     */
    fun inferIntent(layoutMode: LayoutMode): ArrangementIntent = when (layoutMode) {
        LayoutMode.FULLSCREEN -> ArrangementIntent.FOCUS

        LayoutMode.SPLIT_LEFT, LayoutMode.SPLIT_RIGHT -> ArrangementIntent.COMPARE

        LayoutMode.GRID, LayoutMode.MOSAIC, LayoutMode.T_PANEL,
        LayoutMode.COCKPIT, LayoutMode.ROW, LayoutMode.FREEFORM,
        LayoutMode.SPATIAL_DICE, LayoutMode.WORKFLOW, LayoutMode.GALLERY -> ArrangementIntent.OVERVIEW

        LayoutMode.CAROUSEL, LayoutMode.TRIPTYCH -> ArrangementIntent.PRESENT

        LayoutMode.DASHBOARD -> ArrangementIntent.OVERVIEW
    }

    // ── Private resolution strategies ──────────────────────────────────────

    private fun resolveForFocus(): LayoutMode = LayoutMode.FULLSCREEN

    private fun resolveForCompare(
        frameCount: Int,
        displayProfile: DisplayProfile,
    ): LayoutMode {
        if (frameCount < 2) return LayoutMode.FULLSCREEN

        return when (displayProfile) {
            // Glasses: always split left (primary content on left, reference on right)
            DisplayProfile.GLASS_MICRO,
            DisplayProfile.GLASS_COMPACT,
            DisplayProfile.GLASS_STANDARD -> LayoutMode.SPLIT_LEFT

            // Phone in portrait: T_PANEL (top/bottom) works better than side-by-side
            DisplayProfile.PHONE -> LayoutMode.T_PANEL

            // Tablet/Desktop/HD: true side-by-side split
            DisplayProfile.TABLET,
            DisplayProfile.GLASS_HD -> LayoutMode.SPLIT_LEFT
        }
    }

    private fun resolveForOverview(
        frameCount: Int,
        displayProfile: DisplayProfile,
        spatialAvailable: Boolean,
    ): LayoutMode {
        // Special cases for low frame count
        if (frameCount <= 1) return LayoutMode.FULLSCREEN
        if (frameCount == 2) return resolveForCompare(frameCount, displayProfile)

        return when {
            // Glass displays: limited space, use row for scrollable overview
            displayProfile.isGlass -> LayoutMode.ROW

            // 3 frames: mosaic (1 large + 2 smaller) looks better than grid
            frameCount == 3 -> LayoutMode.MOSAIC

            // 4 frames on tablet+: clean 2x2 grid
            frameCount == 4 -> LayoutMode.GRID

            // 5 frames with spatial: dice layout (4 corners + center)
            frameCount == 5 && spatialAvailable -> LayoutMode.SPATIAL_DICE

            // 5-6 frames: mosaic adapts well
            frameCount in 5..6 -> LayoutMode.MOSAIC

            // 7+ frames: grid scales best
            else -> LayoutMode.GRID
        }
    }

    private fun resolveForPresent(
        frameCount: Int,
        displayProfile: DisplayProfile,
    ): LayoutMode = when {
        // Single frame or glass: fullscreen is the only sensible presentation
        frameCount <= 1 || displayProfile.isGlass -> LayoutMode.FULLSCREEN

        // 3 frames exactly: triptych (book spread) is ideal
        frameCount == 3 -> LayoutMode.TRIPTYCH

        // Other frame counts: carousel with perspective scaling
        else -> LayoutMode.CAROUSEL
    }
}
