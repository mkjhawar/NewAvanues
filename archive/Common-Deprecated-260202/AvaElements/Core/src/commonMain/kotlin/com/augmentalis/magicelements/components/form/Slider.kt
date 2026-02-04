package com.augmentalis.avaelements.components.form

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Slider Component
 *
 * A range slider component for selecting numeric values within a range.
 *
 * Features:
 * - Continuous or discrete value selection
 * - Configurable min/max range
 * - Optional step increments
 * - Value label display
 * - Custom label formatting
 * - Real-time value change events
 *
 * Platform mappings:
 * - Android: Slider from Material Components
 * - iOS: UISlider
 * - Web: Range input
 *
 * Usage:
 * ```kotlin
 * Slider(
 *     value = 50f,
 *     valueRange = 0f..100f,
 *     steps = 10,
 *     showLabel = true,
 *     labelFormatter = { "${it.toInt()}%" },
 *     onValueChange = { value -> println("Value: $value") }
 * )
 * ```
 */
data class SliderComponent(
    override val type: String = "Slider",
    val value: Float,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    val steps: Int = 0,
    val showLabel: Boolean = true,
    val labelFormatter: ((Float) -> String)? = null,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onValueChange: ((Float) -> Unit)? = null
) : Component {
    init {
        require(value in valueRange) {
            "Slider value must be within the specified range"
        }
        require(steps >= 0) { "Slider steps must be non-negative" }
        require(valueRange.start < valueRange.endInclusive) {
            "Slider range start must be less than end"
        }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}
