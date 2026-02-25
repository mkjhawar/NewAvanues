package com.augmentalis.avanueui.ui.core.form
import com.augmentalis.avanueui.core.*

data class SliderComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val value: Float,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val label: String? = null,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true,
    val showValue: Boolean = false,
    val onValueChange: ((Float) -> Unit)? = null
) : Component {
    init {
        require(min < max) { "min must be less than max" }
        require(value in min..max) { "value must be in range" }
        require(step > 0) { "step must be positive" }
    }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun updateValue(newValue: Float): SliderComponent = if (newValue in min..max) copy(value = newValue) else this
}

typealias Slider = SliderComponent
