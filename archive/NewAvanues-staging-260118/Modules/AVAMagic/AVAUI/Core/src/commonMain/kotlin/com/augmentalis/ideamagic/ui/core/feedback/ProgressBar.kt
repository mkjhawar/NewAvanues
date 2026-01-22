package com.augmentalis.avamagic.ui.core.feedback
import com.augmentalis.avamagic.components.core.*
data class ProgressBarComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val value: Float, val max: Float = 100f, val indeterminate: Boolean = false, val color: Color = Color.Blue, val size: ComponentSize = ComponentSize.MD) : Component {
    init { require(max > 0) { "max must be positive" }; if (!indeterminate) require(value in 0f..max) { "value must be in range" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    val percentage: Float get() = if (indeterminate) 0f else (value / max) * 100f
}
