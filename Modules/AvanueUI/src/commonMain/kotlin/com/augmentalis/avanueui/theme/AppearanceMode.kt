/**
 * AppearanceMode.kt - Light/Dark/Auto appearance axis (Theme v5.1)
 *
 * Third independent axis of the Avanues theme system.
 * Combines with AvanueColorPalette (4 palettes) x MaterialMode (4 styles)
 * for 4x4x2 = 32 visual configurations.
 *
 * Auto follows the system dark/light preference via isSystemInDarkTheme().
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.theme

enum class AppearanceMode(val displayName: String) {
    Light("Light"),
    Dark("Dark"),
    Auto("Auto");

    companion object {
        val DEFAULT = Auto

        fun fromString(value: String): AppearanceMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
