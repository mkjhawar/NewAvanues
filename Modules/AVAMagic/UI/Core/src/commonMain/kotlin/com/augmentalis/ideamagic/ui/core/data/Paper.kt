package com.augmentalis.avanues.avamagic.ui.core.data

import com.augmentalis.avanues.avamagic.components.core.*

/**
 * Paper Component
 *
 * A paper (surface) component that provides an elevated surface for content,
 * typically with a shadow.
 *
 * Features:
 * - Elevation levels (shadow depth)
 * - Container for child components
 * - Rounded corners
 *
 * Platform mappings:
 * - Android: MaterialCardView / Surface
 * - iOS: UIView with shadow
 * - Web: Div with box-shadow
 *
 * Usage:
 * ```kotlin
 * Paper(
 *     elevation = 2,
 *     children = listOf(
 *         TextComponent("Content inside paper"),
 *         ButtonComponent("Action")
 *     )
 * )
 * ```
 */
data class PaperComponent(
    val elevation: Int = 1,
    val children: List<Any> = emptyList(),
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList()
) {
    init {
        require(elevation >= 0) { "elevation must be non-negative" }
    }

}
