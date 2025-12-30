package com.augmentalis.magicelements.core.variants

import kotlinx.serialization.Serializable

@Serializable
enum class BadgeVariant {
    Standard, Dot, Counter
}

@Serializable
enum class BadgePosition {
    TopRight, TopLeft, BottomRight, BottomLeft
}
