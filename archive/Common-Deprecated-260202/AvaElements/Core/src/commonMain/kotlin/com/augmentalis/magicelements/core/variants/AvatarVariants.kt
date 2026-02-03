package com.augmentalis.magicelements.core.variants

import kotlinx.serialization.Serializable

@Serializable
enum class AvatarShape {
    Circle, Square, Rounded
}

@Serializable
enum class AvatarSize {
    XSmall, Small, Medium, Large, XLarge
}
