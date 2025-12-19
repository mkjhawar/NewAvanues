package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ProgressCircle component - Flutter Material parity
 *
 * A Material Design 3 circular progress indicator (determinate or indeterminate).
 * Shows task progress or loading state.
 *
 * **Web Equivalent:** `CircularProgress` (MUI)
 * **Material Design 3:** https://m3.material.io/components/progress-indicators/overview
 *
 * ## Features
 * - Determinate (0-100%) or indeterminate mode
 * - Configurable size and thickness
 * - Optional label/percentage display
 * - Customizable colors
 * - Clockwise or counter-clockwise rotation
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with progress announcements
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * // Indeterminate
 * ProgressCircle(
 *     size = 48f,
 *     strokeWidth = 4f
 * )
 *
 * // Determinate with percentage
 * ProgressCircle(
 *     value = 0.75f,
 *     size = 64f,
 *     strokeWidth = 6f,
 *     showLabel = true,
 *     labelText = "75%"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Progress value (0.0-1.0), null for indeterminate
 * @property size Circle diameter in dp
 * @property strokeWidth Stroke thickness in dp
 * @property color Optional color override
 * @property backgroundColor Optional background track color
 * @property showLabel Whether to show centered label
 * @property labelText Optional custom label text (defaults to percentage)
 * @property rotation Starting rotation in degrees
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ProgressCircle(
    override val type: String = "ProgressCircle",
    override val id: String? = null,
    val value: Float? = null,
    val size: Float = 48f,
    val strokeWidth: Float = 4f,
    val color: String? = null,
    val backgroundColor: String? = null,
    val showLabel: Boolean = false,
    val labelText: String? = null,
    val rotation: Float = 0f,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return if (value != null) {
            val percentage = (value * 100).toInt()
            contentDescription ?: "Progress $percentage percent"
        } else {
            contentDescription ?: "Loading"
        }
    }

    /**
     * Check if indeterminate mode
     */
    fun isIndeterminate(): Boolean {
        return value == null
    }

    /**
     * Get label text (custom or percentage)
     */
    fun getEffectiveLabelText(): String? {
        if (!showLabel) return null
        return labelText ?: value?.let { "${(it * 100).toInt()}%" }
    }

    /**
     * Validate value is in valid range
     */
    fun isValueValid(): Boolean {
        return value == null || value in 0f..1f
    }

    companion object {
        /**
         * Create an indeterminate progress circle
         */
        fun indeterminate(
            size: Float = 48f,
            strokeWidth: Float = 4f,
            color: String? = null
        ) = ProgressCircle(
            value = null,
            size = size,
            strokeWidth = strokeWidth,
            color = color
        )

        /**
         * Create a determinate progress circle
         */
        fun determinate(
            value: Float,
            size: Float = 48f,
            strokeWidth: Float = 4f,
            showLabel: Boolean = false,
            color: String? = null
        ) = ProgressCircle(
            value = value,
            size = size,
            strokeWidth = strokeWidth,
            showLabel = showLabel,
            color = color
        )

        /**
         * Create a progress circle with percentage label
         */
        fun withLabel(
            value: Float,
            size: Float = 64f,
            strokeWidth: Float = 6f,
            color: String? = null
        ) = ProgressCircle(
            value = value,
            size = size,
            strokeWidth = strokeWidth,
            showLabel = true,
            color = color
        )
    }
}
