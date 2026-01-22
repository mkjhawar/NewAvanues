package com.augmentalis.avamagic.ui.core.layout

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.Orientation

/**
 * Visual separator between content sections.
 *
 * A divider is a thin line that groups content in lists and layouts.
 * Can be horizontal (default) or vertical.
 *
 * ## Usage Examples
 * ```kotlin
 * // Horizontal divider (default)
 * val divider = DividerComponent()
 *
 * // Custom color and thickness
 * val boldDivider = DividerComponent(
 *     thickness = 2f,
 *     color = Color.Blue
 * )
 *
 * // Vertical divider
 * val verticalDivider = DividerComponent(
 *     orientation = Orientation.Vertical,
 *     thickness = 1f
 * )
 *
 * // With margin spacing
 * val spacedDivider = DividerComponent(
 *     style = ComponentStyle(
 *         margin = Margin(vertical = 16f)
 *     )
 * )
 * ```
 *
 * @property orientation Horizontal or vertical divider
 * @property thickness Thickness in dp/pt (default 1f)
 * @property color Divider color (default DARK with opacity)
 * @property indent Start/end indent in dp/pt (default 0f)
 * @since 1.0.0
 */
data class DividerComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val orientation: Orientation = Orientation.Horizontal,
    val thickness: Float = 1f,
    val color: Color = Color.Black,
    val indent: Float = 0f
) : Component {
    init {
        require(thickness > 0) { "Thickness must be positive (got $thickness)" }
        require(indent >= 0) { "Indent must be non-negative (got $indent)" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    companion object {
        /** Standard horizontal divider */
        val HORIZONTAL = DividerComponent()

        /** Standard vertical divider */
        val VERTICAL = DividerComponent(orientation = Orientation.Vertical)

        /** Thick horizontal divider (2dp) */
        val THICK = DividerComponent(thickness = 2f)

        /** Thin horizontal divider (0.5dp) */
        val THIN = DividerComponent(thickness = 0.5f)

        /** Indented horizontal divider (16dp indent) */
        val INDENTED = DividerComponent(indent = 16f)
    }
}
