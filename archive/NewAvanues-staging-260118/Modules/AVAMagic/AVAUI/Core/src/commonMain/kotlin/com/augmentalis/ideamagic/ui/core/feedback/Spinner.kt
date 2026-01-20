package com.augmentalis.avamagic.ui.core.feedback

import com.augmentalis.avamagic.components.core.*

/**
 * Spinner Component
 *
 * A circular loading indicator component.
 *
 * Features:
 * - Circular indeterminate animation
 * - Size variants (small/medium/large)
 * - Optional label text
 * - Color customization
 * - Centered or inline display
 * - Smooth animations
 *
 * Platform mappings:
 * - Android: CircularProgressIndicator
 * - iOS: UIActivityIndicatorView
 * - Web: Spinner element
 *
 * Usage:
 * ```kotlin
 * Spinner(
 *     size = SpinnerSize.Medium,
 *     label = "Loading..."
 * )
 * ```
 */
data class SpinnerComponent(
    val size: SpinnerSize = SpinnerSize.Medium,
    val label: String? = null,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList()
) {
}

/**
 * Spinner size variants
 */
enum class SpinnerSize {
    Small,
    Medium,
    Large
}
