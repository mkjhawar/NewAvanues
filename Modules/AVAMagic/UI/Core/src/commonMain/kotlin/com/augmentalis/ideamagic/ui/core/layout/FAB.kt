package com.augmentalis.avanues.avamagic.ui.core.layout

import com.augmentalis.avanues.avamagic.components.core.*
import com.augmentalis.avanues.avamagic.components.core.Position

/**
 * Floating Action Button - prominent action button.
 *
 * A FAB represents the primary action in an application. It floats above
 * the content and is typically positioned in the bottom-right corner.
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic FAB with icon
 * val fab = FABComponent(
 *     icon = "add",
 *     position = Position.BOTTOM_RIGHT
 * )
 *
 * // FAB with text label
 * val fab = FABComponent(
 *     icon = "edit",
 *     label = "Edit",
 *     position = Position.BOTTOM_RIGHT
 * )
 *
 * // Mini FAB
 * val fab = FABComponent(
 *     icon = "favorite",
 *     size = ComponentSize.SM,
 *     position = Position.BOTTOM_RIGHT
 * )
 *
 * // Extended FAB (text + icon)
 * val fab = FABComponent(
 *     icon = "add",
 *     label = "Create New",
 *     extended = true
 * )
 * ```
 *
 * @property icon Icon name/identifier
 * @property label Optional text label
 * @property extended Whether to show expanded FAB with text (default false)
 * @property size FAB size (SM = mini, MD = regular, LG = large)
 * @property position Screen position (default BOTTOM_RIGHT)
 * @property elevation Shadow elevation in dp/pt (default 6f)
 * @since 1.0.0
 */
data class FABComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val icon: String,
    val label: String? = null,
    val extended: Boolean = false,
    val size: ComponentSize = ComponentSize.MD,
    val position: Position = Position.BOTTOM_RIGHT,
    val elevation: Float = 6f
) : Component {
    init {
        require(icon.isNotBlank()) { "Icon cannot be blank" }
        if (extended) {
            require(!label.isNullOrBlank()) { "Extended FAB requires label" }
        }
        require(elevation >= 0) { "Elevation must be non-negative (got $elevation)" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    companion object {
        /**
         * Creates a standard FAB with add icon.
         */
        fun add(position: Position = Position.BOTTOM_RIGHT) =
            FABComponent(icon = "add", position = position)

        /**
         * Creates a standard FAB with edit icon.
         */
        fun edit(position: Position = Position.BOTTOM_RIGHT) =
            FABComponent(icon = "edit", position = position)

        /**
         * Creates a mini FAB (small size).
         */
        fun mini(icon: String, position: Position = Position.BOTTOM_RIGHT) =
            FABComponent(icon = icon, size = ComponentSize.SM, position = position)

        /**
         * Creates an extended FAB with icon and label.
         */
        fun extended(icon: String, label: String, position: Position = Position.BOTTOM_RIGHT) =
            FABComponent(icon = icon, label = label, extended = true, position = position)
    }
}
