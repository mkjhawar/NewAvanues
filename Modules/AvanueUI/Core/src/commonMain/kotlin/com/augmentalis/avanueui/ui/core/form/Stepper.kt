package com.augmentalis.avanueui.ui.core.form

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.Orientation

/**
 * Numeric stepper for incrementing/decrementing values.
 *
 * A stepper provides increment (+) and decrement (-) buttons for
 * adjusting numeric values within a defined range.
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic stepper
 * val stepper = StepperComponent(
 *     value = 5,
 *     min = 0,
 *     max = 10
 * )
 *
 * // With step size
 * val stepper = StepperComponent(
 *     value = 50,
 *     min = 0,
 *     max = 100,
 *     step = 10
 * )
 *
 * // With label
 * val stepper = StepperComponent(
 *     label = "Quantity",
 *     value = 1,
 *     min = 1,
 *     max = 99
 * )
 *
 * // Vertical orientation
 * val stepper = StepperComponent(
 *     value = 0,
 *     orientation = Orientation.Vertical
 * )
 * ```
 *
 * @property value Current numeric value
 * @property min Minimum allowed value
 * @property max Maximum allowed value
 * @property step Increment/decrement amount (default 1)
 * @property label Optional label text
 * @property orientation Button layout (default HORIZONTAL)
 * @property size Stepper size (default MD)
 * @property enabled Whether user can interact (default true)
 * @property wraparound Whether to wrap from max to min and vice versa (default false)
 * @since 1.0.0
 */
data class StepperComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val value: Int,
    val min: Int = 0,
    val max: Int = 100,
    val step: Int = 1,
    val label: String? = null,
    val orientation: Orientation = Orientation.Horizontal,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true,
    val wraparound: Boolean = false
) : Component {
    init {
        require(min <= max) { "min ($min) must be <= max ($max)" }
        require(value in min..max) { "value ($value) must be in range $min..$max" }
        require(step > 0) { "step must be positive (got $step)" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Returns a copy with value incremented by step.
     * Respects max bound or wraps if wraparound is enabled.
     */
    fun increment(): StepperComponent {
        val newValue = value + step
        return when {
            newValue <= max -> copy(value = newValue)
            wraparound -> copy(value = min)
            else -> copy(value = max)
        }
    }

    /**
     * Returns a copy with value decremented by step.
     * Respects min bound or wraps if wraparound is enabled.
     */
    fun decrement(): StepperComponent {
        val newValue = value - step
        return when {
            newValue >= min -> copy(value = newValue)
            wraparound -> copy(value = max)
            else -> copy(value = min)
        }
    }

    /**
     * Returns true if value can be incremented.
     */
    val canIncrement: Boolean get() = wraparound || value < max

    /**
     * Returns true if value can be decremented.
     */
    val canDecrement: Boolean get() = wraparound || value > min

    companion object {
        /**
         * Creates a stepper for quantity selection (1-99).
         */
        fun quantity(value: Int = 1) =
            StepperComponent(value = value, min = 1, max = 99, label = "Quantity")

        /**
         * Creates a stepper for percentage (0-100, step 5).
         */
        fun percentage(value: Int = 50) =
            StepperComponent(value = value, min = 0, max = 100, step = 5, label = "%")

        /**
         * Creates a stepper with wraparound enabled.
         */
        fun wraparound(value: Int, min: Int, max: Int) =
            StepperComponent(value = value, min = min, max = max, wraparound = true)
    }
}
