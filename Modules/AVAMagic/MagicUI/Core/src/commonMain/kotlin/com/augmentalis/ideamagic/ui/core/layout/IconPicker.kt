package com.augmentalis.magicui.ui.core.layout
import com.augmentalis.magicui.components.core.*
data class IconPickerComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val icons: List<String>, val selectedIcon: String? = null, val columns: Int = 6, val searchable: Boolean = true, val size: ComponentSize = ComponentSize.MD) : Component {
    init { require(icons.isNotEmpty()) { "icons cannot be empty" }; require(columns > 0) { "columns must be positive" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun selectIcon(icon: String): IconPickerComponent = copy(selectedIcon = icon)
    companion object { fun material(icons: List<String>) = IconPickerComponent(icons = icons, columns = 8) }
}
