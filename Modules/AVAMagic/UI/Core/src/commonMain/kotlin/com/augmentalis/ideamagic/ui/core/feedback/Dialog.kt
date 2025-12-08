package com.augmentalis.avanues.avamagic.ui.core.feedback
import com.augmentalis.avanues.avamagic.components.core.*
data class DialogComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val title: String, val content: String, val confirmLabel: String = "OK", val cancelLabel: String? = null, val dismissible: Boolean = true, val child: Component? = null) : Component {
    init { require(title.isNotBlank()) { "Dialog title cannot be blank" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    companion object { fun alert(title: String, message: String) = DialogComponent(title = title, content = message, cancelLabel = null); fun confirm(title: String, message: String) = DialogComponent(title = title, content = message, confirmLabel = "Yes", cancelLabel = "No") }
}
