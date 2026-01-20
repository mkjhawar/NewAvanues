package com.augmentalis.avamagic.ui.core.display
import com.augmentalis.avamagic.components.core.*
data class ChipComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val label: String, val icon: String? = null, val color: Color = Color.White, val size: ComponentSize = ComponentSize.SM, val deletable: Boolean = false, val selected: Boolean = false) : Component {
    init { require(label.isNotBlank()) { "Chip label cannot be blank" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun toggleSelection(): ChipComponent = copy(selected = !selected)
    companion object { fun filter(label: String, selected: Boolean = false) = ChipComponent(label = label, selected = selected, deletable = true) }
}
