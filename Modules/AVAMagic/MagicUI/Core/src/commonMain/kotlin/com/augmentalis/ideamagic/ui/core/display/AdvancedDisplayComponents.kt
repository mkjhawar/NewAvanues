package com.augmentalis.magicui.ui.core.display

import com.augmentalis.magicui.components.core.*

/**
 * ListTile - Standard list row with leading/trailing icons and text
 */
data class ListTileComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val title: String,
    val subtitle: String? = null,
    val leading: Component? = null,
    val trailing: Component? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val onClick: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * TabBar - Horizontal tabs for content switching
 */
data class TabBarComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val tabs: List<TabItem>,
    val selectedIndex: Int = 0,
    val scrollable: Boolean = false,
    val onTabSelected: ((Int) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

data class TabItem(
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true
)

/**
 * CircularProgressIndicator - Circular loading spinner
 */
data class CircularProgressComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val progress: Float? = null, // null = indeterminate
    val color: Color? = null,
    val strokeWidth: Float = 4f,
    val size: Float = 48f
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}
