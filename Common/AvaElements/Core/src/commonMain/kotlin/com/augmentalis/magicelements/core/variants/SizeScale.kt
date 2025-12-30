package com.augmentalis.magicelements.core.variants

import kotlinx.serialization.Serializable

@Serializable
enum class SizeScale {
    XS, SM, MD, LG, XL, XXL
}

@Serializable
enum class ElevationLevel {
    None, Low, Medium, High, Highest
}
