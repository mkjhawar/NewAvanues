package com.augmentalis.magicelements.components.phase3.display

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component

@Serializable
data class HeadingText(
    val text: String,
    val level: HeadingLevel = HeadingLevel.H1,
    val color: String? = null,
    val fontWeight: String? = null,
    val textAlign: TextAlignment = TextAlignment.Start,
    val maxLines: Int? = null
) : Component

@Serializable
enum class HeadingLevel {
    H1, H2, H3, H4, H5, H6
}

@Serializable
enum class TextAlignment {
    Start, Center, End, Justify
}

@Serializable
data class DisplayText(
    val text: String,
    val size: DisplaySize = DisplaySize.Medium,
    val color: String? = null,
    val gradient: List<String>? = null,
    val textAlign: TextAlignment = TextAlignment.Start
) : Component

@Serializable
enum class DisplaySize {
    Small, Medium, Large, XLarge
}

@Serializable
data class LabelText(
    val text: String,
    val size: LabelSize = LabelSize.Medium,
    val color: String? = null,
    val isRequired: Boolean = false
) : Component

@Serializable
enum class LabelSize {
    Small, Medium, Large
}

@Serializable
data class CaptionText(
    val text: String,
    val color: String? = null,
    val icon: String? = null
) : Component

@Serializable
data class BodyText(
    val text: String,
    val size: BodySize = BodySize.Medium,
    val color: String? = null,
    val lineHeight: Float? = null
) : Component

@Serializable
enum class BodySize {
    Small, Medium, Large
}
