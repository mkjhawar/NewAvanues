package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Gauge component - Flutter Material parity
 *
 * A circular gauge/meter displaying a value within a range using Canvas rendering.
 *
 * **Flutter Equivalent:** `syncfusion_flutter_gauges` RadialGauge
 * **Material Design 3:** Custom data visualization component with Material theming
 *
 * ## Features
 * - Circular arc gauge
 * - Configurable start/end angles
 * - Multiple colored segments
 * - Center value display
 * - Custom thickness
 * - Smooth animations
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * Gauge(
 *     value = 75f,
 *     min = 0f,
 *     max = 100f,
 *     label = "CPU Usage",
 *     unit = "%",
 *     segments = listOf(
 *         Gauge.GaugeSegment(0f, 60f, "#4CAF50", "Normal"),
 *         Gauge.GaugeSegment(60f, 80f, "#FF9800", "Warning"),
 *         Gauge.GaugeSegment(80f, 100f, "#F44336", "Critical")
 *     )
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current value to display
 * @property min Minimum value
 * @property max Maximum value
 * @property label Optional label below the value
 * @property unit Optional unit (e.g., "%", "°C")
 * @property startAngle Start angle in degrees (0 = right, 90 = bottom, 180 = left, 270 = top)
 * @property sweepAngle Total sweep angle in degrees
 * @property thickness Arc thickness in dp
 * @property segments Colored segments for different value ranges
 * @property size Gauge size in dp
 * @property showValue Whether to show the value text
 * @property valueFormat Format string for value display
 * @property animated Whether to animate the gauge
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Gauge(
    override val type: String = "Gauge",
    override val id: String? = null,
    val value: Float,
    val min: Float = 0f,
    val max: Float = 100f,
    val label: String? = null,
    val unit: String? = null,
    val startAngle: Float = 135f, // Bottom-left
    val sweepAngle: Float = 270f, // 3/4 circle
    val thickness: Float = 20f,
    val segments: List<GaugeSegment> = emptyList(),
    val size: Float = 200f,
    val showValue: Boolean = true,
    val valueFormat: String? = null,
    val animated: Boolean = true,
    val animationDuration: Int = 1000,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Colored segment in the gauge
     *
     * @property start Start value for this segment
     * @property end End value for this segment
     * @property color Hex color string (e.g., "#4CAF50")
     * @property label Optional label for this segment
     */
    data class GaugeSegment(
        val start: Float,
        val end: Float,
        val color: String,
        val label: String? = null
    ) {
        /**
         * Check if a value falls within this segment
         */
        fun contains(value: Float): Boolean {
            return value in start..end
        }

        /**
         * Get the percentage of this segment within the total range
         */
        fun getPercentage(min: Float, max: Float): Float {
            val range = max - min
            if (range == 0f) return 0f
            return ((end - start) / range) * 100f
        }
    }

    /**
     * Get the normalized value (0.0 to 1.0)
     */
    fun getNormalizedValue(): Float {
        val range = max - min
        if (range == 0f) return 0f
        return ((value - min) / range).coerceIn(0f, 1f)
    }

    /**
     * Get the sweep angle for the current value
     */
    fun getValueSweepAngle(): Float {
        return sweepAngle * getNormalizedValue()
    }

    /**
     * Get the color for the current value based on segments
     */
    fun getValueColor(): String? {
        return segments.find { it.contains(value) }?.color
    }

    /**
     * Get formatted value string
     */
    fun getFormattedValue(): String {
        val formattedValue = if (valueFormat == null) {
            // Default: one decimal place
            val rounded = (value * 10).toInt() / 10.0
            rounded.toString()
        } else {
            // Custom format not fully supported in K/N, use simple toString
            value.toString()
        }

        return if (unit != null) {
            "$formattedValue$unit"
        } else {
            formattedValue
        }
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val labelPart = label?.let { "$it: " } ?: "Gauge: "
        val valuePart = getFormattedValue()
        val rangePart = " (range: $min to $max)"

        // Add status based on segments
        val statusPart = segments.find { it.contains(value) }?.label?.let { " - $it" } ?: ""

        return "$labelPart$valuePart$rangePart$statusPart"
    }

    /**
     * Validate gauge data
     */
    fun isValid(): Boolean {
        return min < max && value in min..max
    }

    companion object {
        /** Default gauge size in dp */
        const val DEFAULT_SIZE = 200f

        /** Default animation duration in milliseconds */
        const val DEFAULT_ANIMATION_DURATION = 1000

        /** Default start angle (bottom-left) */
        const val DEFAULT_START_ANGLE = 135f

        /** Default sweep angle (3/4 circle) */
        const val DEFAULT_SWEEP_ANGLE = 270f

        /** Default thickness */
        const val DEFAULT_THICKNESS = 20f

        /**
         * Create a simple progress gauge
         */
        fun progress(
            value: Float,
            max: Float = 100f,
            label: String? = null
        ) = Gauge(
            value = value,
            min = 0f,
            max = max,
            label = label,
            unit = "%",
            segments = listOf(
                GaugeSegment(0f, max, "#2196F3")
            )
        )

        /**
         * Create a temperature gauge
         */
        fun temperature(
            value: Float,
            min: Float = -20f,
            max: Float = 50f
        ) = Gauge(
            value = value,
            min = min,
            max = max,
            label = "Temperature",
            unit = "°C",
            segments = listOf(
                GaugeSegment(min, 0f, "#2196F3", "Cold"),
                GaugeSegment(0f, 20f, "#4CAF50", "Normal"),
                GaugeSegment(20f, 30f, "#FF9800", "Warm"),
                GaugeSegment(30f, max, "#F44336", "Hot")
            )
        )

        /**
         * Create a speed gauge
         */
        fun speed(
            value: Float,
            max: Float = 200f,
            unit: String = "km/h"
        ) = Gauge(
            value = value,
            min = 0f,
            max = max,
            label = "Speed",
            unit = unit,
            startAngle = 180f,
            sweepAngle = 180f,
            segments = listOf(
                GaugeSegment(0f, max * 0.6f, "#4CAF50", "Safe"),
                GaugeSegment(max * 0.6f, max * 0.8f, "#FF9800", "Caution"),
                GaugeSegment(max * 0.8f, max, "#F44336", "Danger")
            )
        )
    }
}
