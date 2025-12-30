package com.augmentalis.magicui.ui.core.form
import com.augmentalis.magicui.components.core.*
data class DateRangePickerComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val startDate: String? = null, val endDate: String? = null, val label: String? = null, val minDate: String? = null, val maxDate: String? = null, val size: ComponentSize = ComponentSize.MD, val enabled: Boolean = true) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun selectStart(date: String): DateRangePickerComponent = copy(startDate = date)
    fun selectEnd(date: String): DateRangePickerComponent = copy(endDate = date)
    fun clear(): DateRangePickerComponent = copy(startDate = null, endDate = null)
}
