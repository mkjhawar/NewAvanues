package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * RefreshIndicator component - Flutter Material parity
 *
 * A pull-to-refresh indicator that triggers a refresh callback when pulled down,
 * following Material Design 3 specifications.
 *
 * **Flutter Equivalent:** `RefreshIndicator`
 * **Material Design 3:** https://m3.material.io/components/progress-indicators/overview
 *
 * ## Features
 * - Pull-to-refresh gesture detection
 * - Circular progress indicator animation
 * - Customizable refresh threshold
 * - Smooth animation and physics
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * RefreshIndicator(
 *     onRefresh = {
 *         // Perform async refresh operation
 *         // Return when complete
 *     },
 *     child = ScrollableList(...),
 *     color = "primary",
 *     backgroundColor = "surface"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property child Child component (typically a scrollable)
 * @property displacement Distance from top before indicator appears
 * @property color Color of the refresh indicator
 * @property backgroundColor Background color of the indicator
 * @property strokeWidth Width of the circular progress stroke
 * @property semanticsLabel Accessibility label for the refresh action
 * @property semanticsValue Accessibility value (e.g., "refreshing")
 * @property triggerMode When to trigger the refresh action
 * @property edgeOffset Offset from edge where indicator appears
 * @property notificationPredicate Predicate to determine if scroll notification should trigger
 * @property contentDescription Accessibility description for TalkBack
 * @property onRefresh Callback invoked when refresh is triggered (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class RefreshIndicator(
    override val type: String = "RefreshIndicator",
    override val id: String? = null,
    val child: Component? = null,
    val displacement: Float = 40f,
    val color: String? = null,
    val backgroundColor: String? = null,
    val strokeWidth: Float = 2f,
    val semanticsLabel: String? = null,
    val semanticsValue: String? = null,
    val triggerMode: RefreshIndicatorTriggerMode = RefreshIndicatorTriggerMode.OnEdge,
    val edgeOffset: Float = 0f,
    val notificationPredicate: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onRefresh: (suspend () -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: semanticsLabel ?: "Pull to refresh"
    }

    /**
     * When to trigger the refresh action
     */
    enum class RefreshIndicatorTriggerMode {
        /** Trigger when indicator is at the edge */
        OnEdge,

        /** Trigger anywhere during the drag */
        Anywhere
    }

    companion object {
        /**
         * Default displacement from top (in dp)
         */
        const val DEFAULT_DISPLACEMENT = 40f

        /**
         * Default stroke width (in dp)
         */
        const val DEFAULT_STROKE_WIDTH = 2f

        /**
         * Create a simple refresh indicator
         */
        fun simple(
            child: Component,
            onRefresh: (suspend () -> Unit)? = null
        ) = RefreshIndicator(
            child = child,
            onRefresh = onRefresh
        )

        /**
         * Create a refresh indicator with custom colors
         */
        fun withColors(
            child: Component,
            color: String,
            backgroundColor: String,
            onRefresh: (suspend () -> Unit)? = null
        ) = RefreshIndicator(
            child = child,
            color = color,
            backgroundColor = backgroundColor,
            onRefresh = onRefresh
        )

        /**
         * Create a refresh indicator with custom displacement
         */
        fun withDisplacement(
            child: Component,
            displacement: Float,
            onRefresh: (suspend () -> Unit)? = null
        ) = RefreshIndicator(
            child = child,
            displacement = displacement,
            onRefresh = onRefresh
        )
    }
}
