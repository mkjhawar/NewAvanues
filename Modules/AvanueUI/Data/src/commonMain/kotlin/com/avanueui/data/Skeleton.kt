package com.avanueui.data

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.ComponentStyle
import com.augmentalis.avanueui.core.Size
import com.augmentalis.avanueui.core.Color
import com.augmentalis.avanueui.core.Spacing
import com.augmentalis.avanueui.core.Border
import com.augmentalis.avanueui.core.Shadow

/**
 * Skeleton Component
 *
 * A skeleton (loading placeholder) component that displays an animated
 * placeholder while content is loading.
 *
 * Features:
 * - Multiple variants (text, rectangular, circular)
 * - Customizable dimensions
 * - Animation types (pulse, wave, none)
 *
 * Platform mappings:
 * - Android: Shimmer effect / placeholder
 * - iOS: Skeleton view
 * - Web: Skeleton loader
 *
 * Usage:
 * ```kotlin
 * Skeleton(
 *     variant = SkeletonVariant.Text,
 *     width = Size.Fixed(200f),
 *     animation = SkeletonAnimation.Pulse
 * )
 * ```
 */
data class SkeletonComponent(
    val type: String = "Skeleton",
    val variant: SkeletonVariant = SkeletonVariant.Text,
    val width: Size? = null,
    val height: Size? = null,
    val animation: SkeletonAnimation = SkeletonAnimation.Pulse,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Skeleton variant types
 */
enum class SkeletonVariant {
    Text,         // Text line placeholder
    Rectangular,  // Rectangular block
    Circular      // Circular placeholder (e.g., avatar)
}

/**
 * Skeleton animation types
 */
enum class SkeletonAnimation {
    Pulse,  // Pulsing opacity
    Wave,   // Wave/shimmer effect
    None    // No animation
}
