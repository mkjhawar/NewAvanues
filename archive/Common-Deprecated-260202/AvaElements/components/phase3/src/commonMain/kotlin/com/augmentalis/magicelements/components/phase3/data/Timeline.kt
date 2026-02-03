package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Color
import kotlinx.serialization.Transient

data class Timeline(
    override val type: String = "Timeline",
    override val id: String? = null,
    val items: List<TimelineItem>,
    val orientation: Orientation = Orientation.Vertical,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class TimelineItem(
    val id: String,
    val timestamp: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: Color? = null
)
