package com.augmentalis.avaelements.components.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

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
    override val type: String = "Paper",
    val elevation: Int = 1,
    val children: List<Component> = emptyList(),
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    init {
        require(elevation >= 0) { "elevation must be non-negative" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}
