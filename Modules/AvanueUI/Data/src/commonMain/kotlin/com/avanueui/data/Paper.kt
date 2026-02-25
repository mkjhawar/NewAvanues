package com.avanueui.data

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.ComponentStyle
import com.augmentalis.avanueui.core.Size
import com.augmentalis.avanueui.core.Color
import com.augmentalis.avanueui.core.Spacing
import com.augmentalis.avanueui.core.Border
import com.augmentalis.avanueui.core.Shadow

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
    val type: String = "Paper",
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
