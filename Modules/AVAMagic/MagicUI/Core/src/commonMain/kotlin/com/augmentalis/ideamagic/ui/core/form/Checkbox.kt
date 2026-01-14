package com.augmentalis.magicui.ui.core.form
import com.augmentalis.magicui.components.core.*
data class CheckboxComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val label: String, val checked: Boolean = false, val indeterminate: Boolean = false, val size: ComponentSize = ComponentSize.MD, val enabled: Boolean = true, val onCheckedChange: ((Boolean) -> Unit)? = null) : Component {
    init { require(label.isNotBlank()) { "label cannot be blank" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun toggle(): CheckboxComponent = copy(checked = !checked, indeterminate = false)
}
