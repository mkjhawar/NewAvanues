package com.augmentalis.avamagic.ui.core.navigation

import com.augmentalis.avamagic.components.core.*

/**
 * NavigationDrawer - Slide-out navigation panel
 */
data class NavigationDrawerComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val items: List<NavDrawerItem>,
    val selectedIndex: Int = 0,
    val header: Component? = null,
    val drawerType: DrawerType = DrawerType.MODAL,
    val onItemSelected: ((Int) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

data class NavDrawerItem(
    val label: String,
    val icon: String? = null,
    val badge: String? = null,
    val enabled: Boolean = true
)

enum class DrawerType {
    MODAL, DISMISSIBLE, PERMANENT
}

/**
 * NavigationRail - Vertical navigation for tablet/desktop
 */
data class NavigationRailComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val items: List<RailItem>,
    val selectedIndex: Int = 0,
    val header: Component? = null,
    val labelType: RailLabelType = RailLabelType.SELECTED,
    val onItemSelected: ((Int) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

data class RailItem(
    val label: String,
    val icon: String,
    val badge: String? = null,
    val enabled: Boolean = true
)

enum class RailLabelType {
    NONE, SELECTED, ALL
}

/**
 * BottomAppBar - Bottom bar with actions and optional FAB
 */
data class BottomAppBarComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val actions: List<Component> = emptyList(),
    val floatingActionButton: Component? = null,
    val fabCutout: Boolean = true
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}
