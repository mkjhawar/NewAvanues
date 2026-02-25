package com.augmentalis.avanueui.ui.core.display
import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.Orientation
data class TimelineComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val items: List<TimelineItem>, val orientation: Orientation = Orientation.Vertical) : Component {
    init { require(items.isNotEmpty()) { "Timeline items cannot be empty" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}
data class TimelineItem(val id: String, val title: String, val description: String? = null, val timestamp: String? = null, val icon: String? = null, val completed: Boolean = false)
