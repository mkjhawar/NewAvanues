package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Semantic accent identifier for each content type.
 *
 * Maps content types to semantic color roles that are resolved to actual
 * Color values at Compose render time via AvanueTheme. This keeps the
 * model layer free of Compose color dependencies while enabling themed
 * frame borders and content indicators.
 *
 * Resolution at render time:
 * ```
 * val color = when (accent) {
 *     INFO -> AvanueTheme.colors.info
 *     ERROR -> AvanueTheme.colors.error
 *     PRIMARY -> AvanueTheme.colors.primary
 *     SECONDARY -> AvanueTheme.colors.secondary
 *     SUCCESS -> AvanueTheme.colors.success
 *     WARNING -> AvanueTheme.colors.warning
 *     TERTIARY -> AvanueTheme.colors.tertiary
 * }
 * ```
 */
@Serializable
enum class ContentAccent {
    /** Web content — informational blue */
    INFO,
    /** PDF content — document red */
    ERROR,
    /** Image content — primary sapphire */
    PRIMARY,
    /** Video content — secondary purple */
    SECONDARY,
    /** Note content — success green */
    SUCCESS,
    /** Camera content — warning amber */
    WARNING,
    /** Other/unknown content — neutral tertiary */
    TERTIARY;

    companion object {
        /** Map content type ID to the appropriate accent. Exhaustive — every type explicit. */
        fun forContentType(typeId: String): ContentAccent = when (typeId) {
            // P0: Core
            "web" -> INFO
            "pdf" -> ERROR
            "image" -> PRIMARY
            "video" -> SECONDARY
            "note", "voice_note" -> SUCCESS
            "camera" -> WARNING
            // P1: Extended
            "form" -> INFO
            "signature" -> WARNING
            "voice" -> SECONDARY
            // P2: Advanced
            "map" -> SUCCESS
            "whiteboard" -> PRIMARY
            "terminal" -> TERTIARY
            // Killer Features
            "ai_summary" -> INFO
            "screen_cast" -> SECONDARY
            // Mini Widgets & External
            "widget" -> TERTIARY
            "external_app" -> INFO
            // Safety net — should never reach here if ALL_TYPES is in sync
            else -> TERTIARY
        }
    }
}
