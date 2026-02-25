package com.augmentalis.avanueui.ui.core.layout

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.Position

/**
 * Application top bar with title, navigation, and actions.
 *
 * AppBar (also called TopAppBar or Navbar) provides navigation and actions
 * at the top of the screen.
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic app bar with title
 * val appBar = AppBarComponent(
 *     title = "My App"
 * )
 *
 * // With navigation icon
 * val appBar = AppBarComponent(
 *     title = "Settings",
 *     navigationIcon = "back",
 *     showNavigationIcon = true
 * )
 *
 * // With action buttons
 * val appBar = AppBarComponent(
 *     title = "Messages",
 *     actions = listOf("search", "more_vert")
 * )
 *
 * // Bottom app bar
 * val appBar = AppBarComponent(
 *     title = "Photos",
 *     position = Position.BOTTOM
 * )
 * ```
 *
 * @property title App bar title text
 * @property subtitle Optional subtitle text
 * @property navigationIcon Icon for navigation (back, menu, etc.)
 * @property showNavigationIcon Whether to show navigation icon (default true)
 * @property actions List of action icon names
 * @property position TOP or BOTTOM (default TOP)
 * @property size App bar size (default MD = 56dp)
 * @property elevated Whether to show shadow/elevation (default true)
 * @since 1.0.0
 */
data class AppBarComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val title: String,
    val subtitle: String? = null,
    val navigationIcon: String = "menu",
    val showNavigationIcon: Boolean = true,
    val actions: List<String> = emptyList(),
    val position: Position = Position.TOP,
    val size: ComponentSize = ComponentSize.MD,
    val elevated: Boolean = true
) : Component {
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(position == Position.TOP || position == Position.BOTTOM) {
            "AppBar position must be TOP or BOTTOM"
        }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    companion object {
        /**
         * Creates a standard top app bar with menu icon.
         */
        fun top(title: String, actions: List<String> = emptyList()) =
            AppBarComponent(title = title, actions = actions)

        /**
         * Creates an app bar with back navigation.
         */
        fun withBack(title: String, actions: List<String> = emptyList()) =
            AppBarComponent(
                title = title,
                navigationIcon = "back",
                actions = actions
            )

        /**
         * Creates a bottom app bar.
         */
        fun bottom(title: String, actions: List<String> = emptyList()) =
            AppBarComponent(
                title = title,
                position = Position.BOTTOM,
                actions = actions
            )

        /**
         * Creates a search app bar.
         */
        fun search(title: String = "Search") =
            AppBarComponent(
                title = title,
                navigationIcon = "back",
                actions = listOf("search", "more_vert")
            )
    }
}
