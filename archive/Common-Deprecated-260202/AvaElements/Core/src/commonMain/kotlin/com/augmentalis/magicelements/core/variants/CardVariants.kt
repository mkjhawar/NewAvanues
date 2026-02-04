package com.augmentalis.magicelements.core.variants

import kotlinx.serialization.Serializable

@Serializable
enum class CardVariant {
    Elevated, Filled, Outlined, Ghost
}

@Serializable
enum class CardShape {
    Rounded, Square, RoundedLarge
}
