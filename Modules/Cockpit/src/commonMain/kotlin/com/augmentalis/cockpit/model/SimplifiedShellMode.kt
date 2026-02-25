package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Which simplified UI shell is active for the Cockpit.
 *
 * Users choose their preferred interaction paradigm in Settings.
 * All four shells share the same [ArrangementIntent] layout abstraction
 * and [ContextualActionProvider] action system — they only differ in how
 * the home/launcher screen is presented and how navigation works.
 *
 * Shell names follow the Avanues brand convention (see Chapter 99):
 * - CockpitAvanue = the hub where avenues meet (dashboard + workspace)
 * - MapViews = ambient card stream mapped by context priority
 * - SearchAvanue = universal command palette for finding anything
 * - SpaceAvanue = spatial zen canvas with usage-based module islands
 */
@Serializable
enum class SimplifiedShellMode(
    val displayLabel: String,
    val description: String,
) {
    /**
     * CockpitAvanue — Dashboard + multi-panel workspace.
     * Traditional dashboard with module tiles, command bar, and 3-panel
     * Cockpit workspace with voice navigation (next/prev/open/close screen).
     * Best for: power users, multitaskers, desktop.
     */
    COCKPIT_AVANUE(
        displayLabel = "CockpitAvanue",
        description = "Dashboard + workspace with voice-navigable panels"
    ),

    /**
     * MapViews — Ambient Card Stream.
     * Cards surface based on context priority (active work, ambient state, suggestions).
     * Best for: casual users, smart glasses, ambient computing.
     */
    MAP_VIEWS(
        displayLabel = "MapViews",
        description = "Ambient card stream — context-aware, minimal"
    ),

    /**
     * SearchAvanue — Command Palette Focus.
     * Single search/voice entry point for everything (modules, commands, settings).
     * Best for: power users, keyboard warriors, desktop.
     */
    SEARCH_AVANUE(
        displayLabel = "SearchAvanue",
        description = "Universal command palette — one search bar for everything"
    ),

    /**
     * SpaceAvanue — Spatial Zen Canvas.
     * Infinite zoomable canvas with usage-based module islands and semantic zoom levels.
     * Higher-usage modules appear larger and closer (near depth), lower-usage modules
     * appear smaller and further away (far depth).
     * Best for: creative workers, tablet users, spatial computing.
     */
    SPACE_AVANUE(
        displayLabel = "SpaceAvanue",
        description = "Spatial zen canvas — usage-based islands, zoom to navigate"
    );

    companion object {
        val DEFAULT = SEARCH_AVANUE

        fun fromString(value: String): SimplifiedShellMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            // Backward compat: accept old names during migration
                ?: when (value.uppercase()) {
                    "CLASSIC" -> COCKPIT_AVANUE
                    "AVANUE_VIEWS" -> MAP_VIEWS
                    "LENS" -> SEARCH_AVANUE
                    "CANVAS" -> SPACE_AVANUE
                    else -> DEFAULT
                }
    }
}
