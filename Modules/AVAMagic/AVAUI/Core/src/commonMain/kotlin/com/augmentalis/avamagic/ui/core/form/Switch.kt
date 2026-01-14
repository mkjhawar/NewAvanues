package com.augmentalis.avamagic.ui.core.form
import com.augmentalis.avamagic.components.core.*
data class SwitchComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val label: String? = null, val checked: Boolean = false, val size: ComponentSize = ComponentSize.MD, val enabled: Boolean = true, val onCheckedChange: ((Boolean) -> Unit)? = null) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun toggle(): SwitchComponent = copy(checked = !checked)
}
