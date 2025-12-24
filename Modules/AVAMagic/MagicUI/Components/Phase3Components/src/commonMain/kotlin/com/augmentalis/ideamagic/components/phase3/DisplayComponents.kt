package com.augmentalis.avaelements.phase3

import com.augmentalis.avaelements.core.*

/**
 * Phase 3 Display Components - Common Interface
 *
 * 8 display components for showing information and feedback
 */

/**
 * Badge component for notifications
 */
data class Badge(
    val id: String,
    val content: String? = null, // null for dot badge
    val visible: Boolean = true,
    val color: BadgeColor = BadgeColor.Primary,
    val size: BadgeSize = BadgeSize.Medium,
    val position: BadgePosition = BadgePosition.TopEnd,
    val maxCount: Int = 99 // Shows "99+" if content is number > maxCount
) : Component

/**
 * Chip component for tags/filters
 */
data class Chip(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val closeable: Boolean = false,
    val variant: ChipVariant = ChipVariant.Filled,
    val onSelected: (() -> Unit)? = null,
    val onClose: (() -> Unit)? = null
) : Component

/**
 * Avatar component for user images
 */
data class Avatar(
    val id: String,
    val imageUrl: String? = null,
    val initials: String? = null,
    val size: AvatarSize = AvatarSize.Medium,
    val shape: AvatarShape = AvatarShape.Circle,
    val backgroundColor: Color? = null,
    val onClick: (() -> Unit)? = null
) : Component

/**
 * Divider component for visual separation
 */
data class Divider(
    val id: String,
    val orientation: DividerOrientation = DividerOrientation.Horizontal,
    val thickness: Float = 1f,
    val color: Color? = null,
    val startIndent: Float = 0f,
    val endIndent: Float = 0f
) : Component

/**
 * Skeleton component for loading states
 */
data class Skeleton(
    val id: String,
    val variant: SkeletonVariant = SkeletonVariant.Text,
    val width: Float? = null,
    val height: Float? = null,
    val animated: Boolean = true,
    val count: Int = 1
) : Component

/**
 * Spinner component for loading indicator
 */
data class Spinner(
    val id: String,
    val size: SpinnerSize = SpinnerSize.Medium,
    val color: Color? = null,
    val label: String? = null
) : Component

/**
 * ProgressBar component for progress indication
 */
data class ProgressBar(
    val id: String,
    val progress: Float, // 0.0 - 1.0
    val variant: ProgressVariant = ProgressVariant.Linear,
    val indeterminate: Boolean = false,
    val color: Color? = null,
    val backgroundColor: Color? = null,
    val height: Float = 4f,
    val showPercentage: Boolean = false,
    val label: String? = null
) : Component

/**
 * Tooltip component for hover information
 */
data class Tooltip(
    val id: String,
    val content: String,
    val position: TooltipPosition = TooltipPosition.Top,
    val arrow: Boolean = true,
    val maxWidth: Float? = 200f,
    val delay: Long = 500 // milliseconds before showing
) : Component

// Supporting enums and data classes

/**
 * Badge color variants
 */
enum class BadgeColor {
    Primary,
    Secondary,
    Error,
    Success,
    Warning,
    Info
}

/**
 * Badge size
 */
enum class BadgeSize {
    Small,   // 12dp
    Medium,  // 16dp
    Large    // 20dp
}

/**
 * Badge position relative to anchor
 */
enum class BadgePosition {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd
}

/**
 * Chip variant
 */
enum class ChipVariant {
    Filled,
    Outlined,
    Elevated
}

/**
 * Avatar size
 */
enum class AvatarSize {
    ExtraSmall, // 24dp
    Small,      // 32dp
    Medium,     // 40dp
    Large,      // 56dp
    ExtraLarge  // 72dp
}

/**
 * Avatar shape
 */
enum class AvatarShape {
    Circle,
    Square,
    Rounded // Rounded square
}

/**
 * Divider orientation
 */
enum class DividerOrientation {
    Horizontal,
    Vertical
}

/**
 * Skeleton variant
 */
enum class SkeletonVariant {
    Text,        // Text line
    Circular,    // Circle (avatar)
    Rectangular, // Rectangle (image)
    Rounded      // Rounded rectangle (button)
}

/**
 * Spinner size
 */
enum class SpinnerSize {
    Small,   // 16dp
    Medium,  // 24dp
    Large,   // 32dp
    ExtraLarge // 48dp
}

/**
 * Progress variant
 */
enum class ProgressVariant {
    Linear,     // Horizontal bar
    Circular    // Circle
}

/**
 * Tooltip position
 */
enum class TooltipPosition {
    Top,
    Bottom,
    Start,
    End
}
