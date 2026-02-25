package com.augmentalis.avanueui.ui.core.feedback
import com.augmentalis.avanueui.core.*
data class ProgressCircleComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val value: Float, val max: Float = 100f, val indeterminate: Boolean = false, val color: Color = Color.Blue, val size: ComponentSize = ComponentSize.MD, val strokeWidth: Float = 4f) : Component {
    init { require(max > 0) { "max must be positive" }; if (!indeterminate) require(value in 0f..max) { "value must be in range" }; require(strokeWidth > 0) { "strokeWidth must be positive" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    val percentage: Float get() = if (indeterminate) 0f else (value / max) * 100f
}
