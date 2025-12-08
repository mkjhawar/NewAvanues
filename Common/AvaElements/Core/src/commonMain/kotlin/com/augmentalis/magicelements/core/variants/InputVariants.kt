package com.augmentalis.magicelements.core.variants

import kotlinx.serialization.Serializable

@Serializable
enum class InputVariant {
    Outlined, Filled, Underlined, Ghost
}

@Serializable
enum class InputSize {
    Small, Medium, Large
}

@Serializable
enum class InputState {
    Default, Focused, Error, Success, Disabled
}
