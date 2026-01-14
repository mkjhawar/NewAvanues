package com.augmentalis.magicelements.core.variants

import kotlinx.serialization.Serializable

@Serializable
enum class ButtonVariant {
    Filled, Outlined, Text, Elevated, Tonal, Pill, Square, Ghost
}

@Serializable
enum class ButtonSize {
    XSmall, Small, Medium, Large, XLarge
}

@Serializable
enum class ButtonShape {
    Rounded, Square, Pill, Circle
}
