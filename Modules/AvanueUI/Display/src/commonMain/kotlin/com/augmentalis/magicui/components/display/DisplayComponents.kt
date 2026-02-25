package com.augmentalis.avanueui.display

import com.augmentalis.avanueui.core.Color
import kotlinx.serialization.Serializable

/**
 * MagicUI Display Components
 *
 * 8 display components for showing information and feedback
 */

/**
 * Badge component for notifications
 */
@Serializable
data class Badge(
    val id: String,
    val content: String? = null,
    val visible: Boolean = true,
    val color: BadgeColor = BadgeColor.Primary,
    val size: BadgeSize = BadgeSize.Medium,
    val position: BadgePosition = BadgePosition.TopEnd,
    val maxCount: Int = 99
)

/**
 * Chip component for tags/filters
 */
@Serializable
data class Chip(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val closeable: Boolean = false,
    val variant: ChipVariant = ChipVariant.Filled
)

/**
 * Avatar component for user images
 */
@Serializable
data class Avatar(
    val id: String,
    val imageUrl: String? = null,
    val initials: String? = null,
    val size: AvatarSize = AvatarSize.Medium,
    val shape: AvatarShape = AvatarShape.Circle,
    val backgroundColor: Color? = null
)

/**
 * Divider component for visual separation
 */
@Serializable
data class Divider(
    val id: String,
    val orientation: DividerOrientation = DividerOrientation.Horizontal,
    val thickness: Float = 1f,
    val color: Color? = null,
    val startIndent: Float = 0f,
    val endIndent: Float = 0f
)

/**
 * Skeleton component for loading states
 */
@Serializable
data class Skeleton(
    val id: String,
    val variant: SkeletonVariant = SkeletonVariant.Text,
    val width: Float? = null,
    val height: Float? = null,
    val animated: Boolean = true,
    val count: Int = 1
)

/**
 * Spinner component for loading indicator
 */
@Serializable
data class Spinner(
    val id: String,
    val size: SpinnerSize = SpinnerSize.Medium,
    val color: Color? = null,
    val label: String? = null
)

/**
 * ProgressBar component for progress indication
 */
@Serializable
data class ProgressBar(
    val id: String,
    val progress: Float,
    val variant: ProgressVariant = ProgressVariant.Linear,
    val indeterminate: Boolean = false,
    val color: Color? = null,
    val backgroundColor: Color? = null,
    val height: Float = 4f,
    val showPercentage: Boolean = false,
    val label: String? = null
)

/**
 * Tooltip component for hover information
 */
@Serializable
data class Tooltip(
    val id: String,
    val content: String,
    val position: TooltipPosition = TooltipPosition.Top,
    val arrow: Boolean = true,
    val maxWidth: Float? = 200f,
    val delay: Long = 500
)

// Supporting enums

@Serializable
enum class BadgeColor {
    Primary,
    Secondary,
    Error,
    Success,
    Warning,
    Info
}

@Serializable
enum class BadgeSize {
    Small,
    Medium,
    Large
}

@Serializable
enum class BadgePosition {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd
}

@Serializable
enum class ChipVariant {
    Filled,
    Outlined,
    Elevated
}

@Serializable
enum class AvatarSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge
}

@Serializable
enum class AvatarShape {
    Circle,
    Square,
    Rounded
}

@Serializable
enum class DividerOrientation {
    Horizontal,
    Vertical
}

@Serializable
enum class SkeletonVariant {
    Text,
    Circular,
    Rectangular,
    Rounded
}

@Serializable
enum class SpinnerSize {
    Small,
    Medium,
    Large,
    ExtraLarge
}

@Serializable
enum class ProgressVariant {
    Linear,
    Circular
}

@Serializable
enum class TooltipPosition {
    Top,
    Bottom,
    Start,
    End
}
