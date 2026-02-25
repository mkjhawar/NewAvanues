package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Which simplified UI shell is active for the Cockpit.
 *
 * Users choose their preferred interaction paradigm in Settings.
 * All three shells share the same [ArrangementIntent] layout abstraction
 * and [ContextualActionProvider] action system — they only differ in how
 * the home/launcher screen is presented and how navigation works.
 *
 * The [CLASSIC] mode preserves the existing Cockpit Dashboard + CommandBar
 * behavior for users who prefer the original UI.
 */
@Serializable
enum class SimplifiedShellMode(
    val displayLabel: String,
    val description: String,
) {
    /**
     * Original Cockpit Dashboard + hierarchical CommandBar.
     * Preserved for backward compatibility and power users who prefer
     * the full 15-layout mode selection.
     */
    CLASSIC(
        displayLabel = "Classic",
        description = "Traditional dashboard with module tiles and command bar"
    ),

    /**
     * AvanueViews — Ambient Card Stream.
     * Cards surface based on context priority (active work, ambient state, suggestions).
     * Best for: casual users, smart glasses, ambient computing.
     */
    AVANUE_VIEWS(
        displayLabel = "AvanueViews",
        description = "Ambient card stream — context-aware, minimal"
    ),

    /**
     * Lens — Command Palette Focus.
     * Single search/voice entry point for everything (modules, commands, settings).
     * Best for: power users, keyboard warriors, desktop.
     */
    LENS(
        displayLabel = "Lens",
        description = "Universal command palette — one search bar for everything"
    ),

    /**
     * Canvas — Spatial Zen.
     * Infinite zoomable canvas with module islands and semantic zoom levels.
     * Best for: creative workers, tablet users, spatial computing.
     */
    CANVAS(
        displayLabel = "Canvas",
        description = "Spatial zen canvas — zoom to navigate, organic layout"
    );

    companion object {
        val DEFAULT = LENS

        fun fromString(value: String): SimplifiedShellMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
