package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Stat component - Flutter Material parity
 *
 * A single statistic card for displaying a key metric with optional change indicator.
 * Used in dashboards, analytics, and reporting interfaces.
 *
 * **Flutter Equivalent:** Custom implementation
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Large value display with label
 * - Optional change indicator
 * - Icon support
 * - Trend visualization
 * - Click handler support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Stat(
 *     label = "Total Revenue",
 *     value = "$45,231.89",
 *     change = "+20.1%",
 *     changeType = ChangeType.Positive,
 *     icon = "attach_money",
 *     description = "from last month"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Label describing the statistic
 * @property value The statistic value to display
 * @property change Optional change indicator (e.g., "+12%")
 * @property changeType Type of change (positive, negative, neutral)
 * @property icon Optional icon identifier
 * @property description Optional description text
 * @property onClick Optional click handler
 * @property elevated Whether to show elevated card style
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Stat(
    override val type: String = "Stat",
    override val id: String? = null,
    val label: String,
    val value: String,
    val change: String? = null,
    val changeType: ChangeType = ChangeType.Neutral,
    val icon: String? = null,
    val description: String? = null,
    val elevated: Boolean = false,
    val contentDescription: String? = null,
    @Transient
    val onClick: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Change type indicator
     */
    enum class ChangeType {
        /** Positive change (green) */
        Positive,

        /** Negative change (red) */
        Negative,

        /** Neutral change (default color) */
        Neutral
    }

    companion object {
        /**
         * Create a stat with positive change
         */
        fun positive(
            label: String,
            value: String,
            change: String
        ) = Stat(
            label = label,
            value = value,
            change = change,
            changeType = ChangeType.Positive
        )

        /**
         * Create a stat with negative change
         */
        fun negative(
            label: String,
            value: String,
            change: String
        ) = Stat(
            label = label,
            value = value,
            change = change,
            changeType = ChangeType.Negative
        )

        /**
         * Create a simple stat without change
         */
        fun simple(
            label: String,
            value: String,
            icon: String? = null
        ) = Stat(
            label = label,
            value = value,
            icon = icon
        )
    }
}
