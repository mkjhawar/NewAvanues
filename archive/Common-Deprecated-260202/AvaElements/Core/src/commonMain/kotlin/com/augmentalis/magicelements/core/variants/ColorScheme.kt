package com.augmentalis.magicelements.core.variants

import kotlinx.serialization.Serializable

@Serializable
enum class ColorScheme {
    Primary, Secondary, Success, Warning, Danger, Info, Light, Dark, Neutral
}

@Serializable
enum class ColorIntensity {
    Light, Normal, Dark
}
