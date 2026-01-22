package com.augmentalis.avacode.workflows

import com.augmentalis.avacode.forms.FormDefinition

/**
 * Workflow definition for multi-step processes.
 *
 * Defines a sequence of steps with conditional branching, validation,
 * and state management. Workflows can integrate with forms for data collection
 * across multiple steps.
 *
 * ## Example
 * ```kotlin
 * val onboarding = workflow("user_onboarding") {
 *     step("registration") {
 *         form(userRegistrationForm)
 *         onComplete { data -> sendVerificationEmail(data["email"]) }
 *     }
 *
 *     step("profile_setup") {
 *         form(profileForm)
 *         condition { data -> data["account_type"] == "premium" }
 *     }
 *
 *     step("payment") {
 *         form(paymentForm)
 *         skipIf { data -> data["account_type"] == "free" }
 *     }
 * }
 * ```
 *
 * @property id Unique workflow identifier
 * @property steps List of workflow steps
 * @property metadata Workflow configuration
 * @since 1.3.0
 */
data class WorkflowDefinition(
    val id: String,
    val steps: List<StepDefinition>,
    val metadata: WorkflowMetadata = WorkflowMetadata()
) {
    init {
        require(id.isNotBlank()) { "Workflow ID cannot be blank" }
        require(id.matches(Regex("[a-z][a-z0-9_]*"))) {
            "Workflow ID must be lowercase alphanumeric with underscores"
        }
        require(steps.isNotEmpty()) { "Workflow must have at least one step" }

        // Check for duplicate step IDs
        val duplicates = steps.groupBy { it.id }.filter { it.value.size > 1 }
        require(duplicates.isEmpty()) {
            "Duplicate step IDs: ${duplicates.keys.joinToString()}"
        }
    }

    /**
     * Create a new workflow instance with initial state.
     */
    fun createInstance(initialData: Map<String, Any?> = emptyMap()): WorkflowInstance =
        WorkflowInstance(
            workflow = this,
            currentStepIndex = 0,
            data = initialData.toMutableMap(),
            state = WorkflowState.IN_PROGRESS,
            stepStates = steps.map { it.id to StepState.PENDING }.toMap().toMutableMap(),
            history = mutableListOf()
        )

    /**
     * Get step by ID.
     */
    fun getStep(stepId: String): StepDefinition? = steps.find { it.id == stepId }

    /**
     * Get step by index.
     */
    fun getStepAt(index: Int): StepDefinition? = steps.getOrNull(index)
}

/**
 * Workflow metadata configuration.
 */
data class WorkflowMetadata(
    val title: String? = null,
    val description: String? = null,
    val version: String = "1.0.0",
    val allowBack: Boolean = true,
    val allowSkip: Boolean = false,
    val persistState: Boolean = true,
    val autoSave: Boolean = true,
    val autoSaveInterval: Long = 30000 // 30 seconds
)

/**
 * Workflow state enumeration.
 */
enum class WorkflowState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    FAILED
}

/**
 * Step state enumeration.
 */
enum class StepState {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    FAILED
}

/**
 * DSL builder function for creating workflows.
 */
fun workflow(
    id: String,
    metadata: WorkflowMetadata = WorkflowMetadata(),
    builder: WorkflowBuilder.() -> Unit
): WorkflowDefinition {
    val workflowBuilder = WorkflowBuilder(id, metadata)
    workflowBuilder.builder()
    return workflowBuilder.build()
}

/**
 * Workflow builder DSL implementation.
 */
class WorkflowBuilder(
    private val id: String,
    private val metadata: WorkflowMetadata
) {
    private val steps = mutableListOf<StepDefinition>()

    fun step(id: String, builder: StepBuilder.() -> Unit) {
        val stepBuilder = StepBuilder(id)
        stepBuilder.builder()
        steps.add(stepBuilder.build())
    }

    internal fun build(): WorkflowDefinition = WorkflowDefinition(id, steps, metadata)
}
