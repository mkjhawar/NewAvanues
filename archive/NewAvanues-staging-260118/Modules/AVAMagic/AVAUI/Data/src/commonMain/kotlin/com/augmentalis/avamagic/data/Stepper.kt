package com.augmentalis.avamagic.avaui.data

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.ComponentStyle
import com.augmentalis.avamagic.components.core.Size
import com.augmentalis.avamagic.components.core.Color
import com.augmentalis.avamagic.components.core.Spacing
import com.augmentalis.avamagic.components.core.Border
import com.augmentalis.avamagic.components.core.Shadow

/**
 * Stepper Component
 *
 * A stepper component that displays progress through numbered steps,
 * commonly used in multi-step forms or processes.
 *
 * Features:
 * - Horizontal or vertical orientation
 * - Step status (pending, active, complete, error)
 * - Optional descriptions
 * - Clickable steps
 *
 * Platform mappings:
 * - Android: Custom stepper widget
 * - iOS: Custom progress indicator
 * - Web: Step progress indicator
 *
 * Usage:
 * ```kotlin
 * Stepper(
 *     steps = listOf(
 *         Step("Account", description = "Create account", status = StepStatus.Complete),
 *         Step("Profile", description = "Fill profile", status = StepStatus.Active),
 *         Step("Verify", description = "Verify email", status = StepStatus.Pending)
 *     ),
 *     currentStep = 1,
 *     orientation = Orientation.Horizontal,
 *     onStepClick = { index -> /* handle click */ }
 * )
 * ```
 */
data class StepperComponent(
    val type: String = "Stepper",
    val steps: List<Step>,
    val currentStep: Int = 0,
    val orientation: Orientation = Orientation.Horizontal,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onStepClick: ((Int) -> Unit)? = null
) : Component {
    init {
        require(steps.isNotEmpty()) { "Stepper must have at least one step" }
        require(currentStep in steps.indices) { "currentStep must be valid" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Individual step in the stepper
 */
data class Step(
    val label: String,
    val description: String? = null,
    val status: StepStatus = StepStatus.Pending
)

/**
 * Status of a step
 */
enum class StepStatus {
    Pending, Active, Complete, Error
}
