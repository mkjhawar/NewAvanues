package com.augmentalis.avaelements.components.form

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Rating Component
 *
 * A star rating component for displaying and collecting ratings.
 *
 * Features:
 * - Configurable number of stars (default 5)
 * - Half-star support
 * - Read-only mode for display
 * - Custom icon support (stars, hearts, etc.)
 * - Interactive rating selection
 * - Hover effects
 * - Keyboard navigation
 *
 * Platform mappings:
 * - Android: Custom rating bar
 * - iOS: Custom rating view
 * - Web: Custom star rating component
 *
 * Usage:
 * ```kotlin
 * Rating(
 *     value = 4.5f,
 *     maxRating = 5,
 *     allowHalf = true,
 *     readonly = false,
 *     icon = "star",
 *     onRatingChange = { rating -> println("Rated: $rating") }
 * )
 * ```
 */
data class RatingComponent(
    override val type: String = "Rating",
    val value: Float = 0f,
    val maxRating: Int = 5,
    val allowHalf: Boolean = false,
    val readonly: Boolean = false,
    val icon: String = "star",
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onRatingChange: ((Float) -> Unit)? = null
) : Component {
    init {
        require(maxRating > 0) { "Max rating must be positive" }
        require(value >= 0f && value <= maxRating) {
            "Rating value must be between 0 and maxRating"
        }
        if (!allowHalf) {
            require(value == value.toInt().toFloat()) {
                "Rating value must be a whole number when allowHalf is false"
            }
        }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}
