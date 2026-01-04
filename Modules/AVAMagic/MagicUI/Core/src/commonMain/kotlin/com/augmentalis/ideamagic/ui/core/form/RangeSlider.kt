package com.augmentalis.magicui.ui.core.form
import com.augmentalis.magicui.components.core.*
data class RangeSliderComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val minValue: Float, val maxValue: Float, val min: Float = 0f, val max: Float = 100f, val step: Float = 1f, val label: String? = null, val showValues: Boolean = true, val size: ComponentSize = ComponentSize.MD, val enabled: Boolean = true) : Component {
    init { require(min < max) { "min must be less than max" }; require(minValue in min..max && maxValue in min..max) { "Values must be within range" }; require(minValue <= maxValue) { "minValue must be <= maxValue" }; require(step > 0) { "step must be positive" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun updateMin(value: Float): RangeSliderComponent = if (value in min..maxValue) copy(minValue = value) else this
    fun updateMax(value: Float): RangeSliderComponent = if (value in minValue..max) copy(maxValue = value) else this
    companion object { fun price(minPrice: Float = 0f, maxPrice: Float = 1000f) = RangeSliderComponent(minValue = minPrice, maxValue = maxPrice, min = 0f, max = 1000f, label = "Price Range") }
}
