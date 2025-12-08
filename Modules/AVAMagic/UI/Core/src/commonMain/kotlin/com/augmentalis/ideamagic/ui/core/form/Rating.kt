package com.augmentalis.avanues.avamagic.ui.core.form

import com.augmentalis.avanues.avamagic.components.core.*

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
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val value: Float = 0f,
    val maxRating: Int = 5,
    val allowHalf: Boolean = false,
    val readonly: Boolean = false,
    val icon: String = "star",
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

    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}
