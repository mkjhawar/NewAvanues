package com.augmentalis.avaelements.flutter.material.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ProgressStepper component - Flutter Material parity
 *
 * A multi-step progress indicator showing completed, current, and upcoming steps.
 * Useful for wizards, checkout flows, and multi-step forms.
 *
 * **Flutter Equivalent:** `Stepper`, custom step indicator
 * **Material Design 3:** https://m3.material.io/components/progress-indicators/overview
 *
 * ## Features
 * - Three states per step: completed, current, upcoming
 * - Optional step labels and descriptions
 * - Clickable steps (navigate to completed steps)
 * - Vertical or horizontal orientation
 * - Customizable step indicators (numbers, icons, checkmarks)
 * - Connecting lines between steps
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ProgressStepper(
 *     steps = listOf(
 *         ProgressStepper.Step(
 *             label = "Account Info",
 *             description = "Enter your details",
 *             icon = "person"
 *         ),
 *         ProgressStepper.Step(
 *             label = "Verification",
 *             description = "Verify your email"
 *         ),
 *         ProgressStepper.Step(
 *             label = "Complete",
 *             description = "Finish setup"
 *         )
 *     ),
 *     currentStep = 1,
 *     orientation = ProgressStepper.Orientation.Horizontal,
 *     clickable = true,
 *     onStepClicked = { stepIndex -> println("Clicked step: $stepIndex") }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property steps List of step definitions
 * @property currentStep Index of the currently active step (0-based)
 * @property orientation Layout orientation (Horizontal or Vertical)
 * @property clickable Whether completed steps can be clicked to navigate
 * @property showStepNumbers Whether to show step numbers (1, 2, 3, ...)
 * @property connectorType Type of connector line between steps
 * @property completedStepColor Color for completed steps
 * @property currentStepColor Color for current step
 * @property upcomingStepColor Color for upcoming steps
 * @property connectorColor Color for connector lines
 * @property backgroundColor Background color of the stepper
 * @property contentPadding Custom padding for the stepper
 * @property contentDescription Accessibility description for TalkBack
 * @property onStepClicked Callback invoked when a step is clicked (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class ProgressStepper(
    override val type: String = "ProgressStepper",
    override val id: String? = null,
    val steps: List<Step>,
    val currentStep: Int = 0,
    val orientation: Orientation = Orientation.Horizontal,
    val clickable: Boolean = false,
    val showStepNumbers: Boolean = true,
    val connectorType: ConnectorType = ConnectorType.Line,
    val completedStepColor: String? = null,
    val currentStepColor: String? = null,
    val upcomingStepColor: String? = null,
    val connectorColor: String? = null,
    val backgroundColor: String? = null,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val contentDescription: String? = null,
    @Transient
    val onStepClicked: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Step data class
     *
     * @property label Step label text
     * @property description Optional step description text
     * @property icon Optional icon identifier (overrides step number)
     * @property optional Whether this step is optional
     * @property error Whether this step has an error state
     */
    data class Step(
        val label: String,
        val description: String? = null,
        val icon: String? = null,
        val optional: Boolean = false,
        val error: Boolean = false
    ) {
        /**
         * Get accessibility description
         */
        fun getAccessibilityDescription(state: StepState, stepNumber: Int): String {
            val parts = mutableListOf("Step $stepNumber: $label")
            when (state) {
                StepState.Completed -> parts.add("completed")
                StepState.Current -> parts.add("current")
                StepState.Upcoming -> parts.add("upcoming")
            }
            if (optional) parts.add("optional")
            if (error) parts.add("error")
            if (description != null) parts.add(description)
            return parts.joinToString(", ")
        }
    }

    /**
     * Step state enumeration
     */
    enum class StepState {
        /** Step has been completed */
        Completed,

        /** Step is currently active */
        Current,

        /** Step has not been reached yet */
        Upcoming
    }

    /**
     * Stepper orientation
     */
    enum class Orientation {
        /** Horizontal layout (steps arranged left to right) */
        Horizontal,

        /** Vertical layout (steps arranged top to bottom) */
        Vertical
    }

    /**
     * Connector type between steps
     */
    enum class ConnectorType {
        /** Solid line connector */
        Line,

        /** Dashed line connector */
        Dashed,

        /** Dotted line connector */
        Dotted,

        /** No connector (discrete steps) */
        None
    }

    /**
     * Get the state of a step at the given index
     */
    fun getStepState(index: Int): StepState = when {
        index < currentStep -> StepState.Completed
        index == currentStep -> StepState.Current
        else -> StepState.Upcoming
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Progress stepper"
        val totalSteps = steps.size
        val currentStepNumber = currentStep + 1
        val orientationDesc = when (orientation) {
            Orientation.Horizontal -> "horizontal"
            Orientation.Vertical -> "vertical"
        }
        return "$base, step $currentStepNumber of $totalSteps, $orientationDesc"
    }

    /**
     * Check if a step can be clicked
     */
    fun canClickStep(index: Int): Boolean {
        return clickable && index < currentStep
    }

    /**
     * Get progress percentage (0.0 to 1.0)
     */
    fun getProgressPercentage(): Float {
        if (steps.isEmpty()) return 0f
        return currentStep.toFloat() / (steps.size - 1).toFloat()
    }

    /**
     * Check if stepper is complete
     */
    fun isComplete(): Boolean {
        return currentStep >= steps.size - 1
    }

    companion object {
        /**
         * Create a horizontal stepper
         */
        fun horizontal(
            steps: List<Step>,
            currentStep: Int = 0,
            onStepClicked: ((Int) -> Unit)? = null
        ) = ProgressStepper(
            steps = steps,
            currentStep = currentStep,
            orientation = Orientation.Horizontal,
            onStepClicked = onStepClicked
        )

        /**
         * Create a vertical stepper
         */
        fun vertical(
            steps: List<Step>,
            currentStep: Int = 0,
            onStepClicked: ((Int) -> Unit)? = null
        ) = ProgressStepper(
            steps = steps,
            currentStep = currentStep,
            orientation = Orientation.Vertical,
            onStepClicked = onStepClicked
        )

        /**
         * Create a simple progress indicator (just circles, no labels)
         */
        fun simple(
            stepCount: Int,
            currentStep: Int = 0
        ) = ProgressStepper(
            steps = List(stepCount) { Step(label = "${it + 1}") },
            currentStep = currentStep,
            orientation = Orientation.Horizontal,
            showStepNumbers = true
        )
    }
}
