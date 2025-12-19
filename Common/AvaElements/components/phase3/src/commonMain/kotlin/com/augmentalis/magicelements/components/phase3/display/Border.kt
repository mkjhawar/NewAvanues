package com.augmentalis.magicelements.components.phase3.display

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component

@Serializable
data class BorderDecorator(
    val content: List<Component> = emptyList(),
    val width: Float = 1f,
    val color: String = "#000000",
    val style: BorderStyle = BorderStyle.Solid,
    val radius: Float = 0f,
    val sides: BorderSides = BorderSides.All
) : Component

@Serializable
enum class BorderStyle {
    Solid, Dashed, Dotted, Double, None
}

@Serializable
data class BorderSides(
    val top: Boolean = true,
    val right: Boolean = true,
    val bottom: Boolean = true,
    val left: Boolean = true
) {
    companion object {
        val All = BorderSides()
        val Horizontal = BorderSides(top = false, bottom = false)
        val Vertical = BorderSides(left = false, right = false)
        val Top = BorderSides(right = false, bottom = false, left = false)
        val Bottom = BorderSides(top = false, right = false, left = false)
    }
}
