package com.augmentalis.avanues.avamagic.ui.core.form
import com.augmentalis.avanues.avamagic.components.core.*
data class ColorPickerComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val selectedColor: String = "#000000", val label: String? = null, val showAlpha: Boolean = false, val presetColors: List<String> = emptyList(), val size: ComponentSize = ComponentSize.MD, val enabled: Boolean = true) : Component {
    init { require(selectedColor.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"))) { "Invalid color format" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun selectColor(color: String): ColorPickerComponent = copy(selectedColor = color)
    companion object { fun basic() = ColorPickerComponent(presetColors = listOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF")) }
}
