package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Stepper(
    override val type: String = "Stepper",
    override val id: String? = null,
    val steps: List<Step>,
    val currentStep: Int = 0,
    val orientation: Orientation = Orientation.Horizontal,
    @Transient val onStepClick: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class Step(
    val label: String,
    val description: String? = null,
    val status: StepStatus = StepStatus.Pending
)

enum class StepStatus {
    Pending,
    Active,
    Completed,
    Error
}

enum class Orientation {
    Horizontal,
    Vertical
}
