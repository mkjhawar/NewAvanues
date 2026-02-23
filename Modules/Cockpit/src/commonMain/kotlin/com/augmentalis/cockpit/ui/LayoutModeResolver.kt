package com.augmentalis.cockpit.ui

import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.cockpit.model.LayoutMode

/**
 * Determines layout mode availability, defaults, and constraints per device profile.
 *
 * Different devices have different optimal layouts:
 * - **Phone**: Carousel is default (best for small screens, swipe-through)
 * - **Tablet**: Cockpit/Flight Deck is default (full 6-slot instrument panel)
 * - **Glass**: Fullscreen is default (single frame, voice-controlled switching)
 * - **All**: Dashboard is available on all profiles as the home/launcher view
 *
 * This resolver is used by the UI to:
 * 1. Set the initial layout mode for new sessions
 * 2. Hide unavailable modes from the layout picker
 * 3. Enforce frame count limits per mode/device
 */
object LayoutModeResolver {

    /**
     * Returns the optimal default layout mode for the given device profile.
     * This is the default for *active sessions* with frames.
     * Use [LayoutMode.DASHBOARD] when no session is active or user navigates home.
     */
    fun defaultMode(profile: DisplayProfile): LayoutMode = when {
        profile.isGlass -> LayoutMode.FULLSCREEN
        profile == DisplayProfile.PHONE -> LayoutMode.CAROUSEL
        else -> LayoutMode.COCKPIT
    }

    /**
     * Whether the given layout mode is available on this device profile.
     * Some modes don't make sense on certain devices (e.g., freeform on glass).
     */
    fun isAvailable(mode: LayoutMode, profile: DisplayProfile): Boolean = when (mode) {
        // Dashboard is always available — it's the home/launcher
        LayoutMode.DASHBOARD -> true

        // Freeform requires precise touch — not available on glass
        LayoutMode.FREEFORM -> !profile.isGlass

        // Grid adapts to any screen size
        LayoutMode.GRID -> true

        // Split requires enough width — not on micro glass
        LayoutMode.SPLIT_LEFT, LayoutMode.SPLIT_RIGHT ->
            profile != DisplayProfile.GLASS_MICRO

        // Cockpit works everywhere but limited on phone/glass
        LayoutMode.COCKPIT -> true

        // T-Panel needs vertical space
        LayoutMode.T_PANEL -> profile != DisplayProfile.GLASS_MICRO

        // Mosaic needs enough area for primary + secondaries
        LayoutMode.MOSAIC -> !profile.isGlass

        // Fullscreen works everywhere
        LayoutMode.FULLSCREEN -> true

        // Workflow works everywhere (voice-navigable on glass)
        LayoutMode.WORKFLOW -> true

        // Row requires horizontal space
        LayoutMode.ROW -> profile != DisplayProfile.GLASS_MICRO

        // Carousel works everywhere (ideal for phone)
        LayoutMode.CAROUSEL -> true

        // Dice-5 needs enough area for 5 frames
        LayoutMode.SPATIAL_DICE -> profile == DisplayProfile.TABLET

        // Gallery works everywhere with adaptive columns
        LayoutMode.GALLERY -> true

        // Triptych needs enough width for 3 panels
        LayoutMode.TRIPTYCH -> profile != DisplayProfile.GLASS_MICRO
    }

    /**
     * Maximum number of frames allowed in the given mode on this device.
     */
    fun maxFrames(mode: LayoutMode, profile: DisplayProfile): Int = when (mode) {
        LayoutMode.DASHBOARD -> 0   // Dashboard doesn't host frames
        LayoutMode.FULLSCREEN -> 20 // All frames exist, only one shown
        LayoutMode.CAROUSEL -> 20   // Paged, all frames exist
        LayoutMode.GALLERY -> 20    // Scrollable grid

        LayoutMode.FREEFORM -> when {
            profile == DisplayProfile.PHONE -> 4
            profile == DisplayProfile.TABLET -> 10
            else -> 6
        }

        LayoutMode.GRID -> when {
            profile == DisplayProfile.PHONE -> 4
            profile == DisplayProfile.TABLET -> 9
            profile.isGlass -> 2
            else -> 6
        }

        LayoutMode.SPLIT_LEFT, LayoutMode.SPLIT_RIGHT -> when {
            profile == DisplayProfile.PHONE -> 3
            else -> 5
        }

        LayoutMode.COCKPIT -> when {
            profile.isGlass -> 2
            profile == DisplayProfile.PHONE -> 3
            else -> 6
        }

        LayoutMode.T_PANEL -> when {
            profile == DisplayProfile.PHONE -> 3
            else -> 5
        }

        LayoutMode.MOSAIC -> when {
            profile == DisplayProfile.PHONE -> 3
            else -> 6
        }

        LayoutMode.WORKFLOW -> 10

        LayoutMode.ROW -> when {
            profile == DisplayProfile.PHONE -> 3
            profile.isGlass -> 2
            else -> 6
        }

        LayoutMode.SPATIAL_DICE -> 5 // Fixed: 4 corners + 1 center

        LayoutMode.TRIPTYCH -> 3 // Fixed: left + center + right
    }

    /**
     * Returns all available layout modes for the given profile, sorted by relevance.
     * The default mode is always first.
     */
    fun availableModes(profile: DisplayProfile): List<LayoutMode> {
        val default = defaultMode(profile)
        return listOf(default) + LayoutMode.entries.filter {
            it != default && isAvailable(it, profile)
        }
    }
}
