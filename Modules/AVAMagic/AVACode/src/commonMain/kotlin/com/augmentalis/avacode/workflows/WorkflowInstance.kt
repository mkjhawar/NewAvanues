package com.augmentalis.avacode.workflows

import com.augmentalis.avanues.avacode.forms.ValidationResult

/**
 * Active workflow instance with state management.
 *
 * Represents a running workflow with current state, progress, and data.
 * Provides methods for navigation (next, back, jumpTo) and state management.
 *
 * @property workflow Workflow definition
 * @property currentStepIndex Current step index
 * @property data Accumulated workflow data
 * @property state Current workflow state
 * @property stepStates State of each step
 * @property history Navigation history
 * @since 1.3.0
 */
data class WorkflowInstance(
    val workflow: WorkflowDefinition,
    val currentStepIndex: Int,
    val data: MutableMap<String, Any?>,
    val state: WorkflowState,
    val stepStates: MutableMap<String, StepState>,
    val history: MutableList<WorkflowTransition>
) {
    /**
     * Get current step definition.
     */
    val currentStep: StepDefinition?
        get() = workflow.getStepAt(currentStepIndex)

    /**
     * Check if workflow is complete.
     */
    val isComplete: Boolean
        get() = state == WorkflowState.COMPLETED

    /**
     * Check if workflow can go back.
     */
    val canGoBack: Boolean
        get() = currentStepIndex > 0 &&
                workflow.metadata.allowBack &&
                (currentStep?.allowBack ?: true)

    /**
     * Check if workflow can go forward.
     */
    val canGoForward: Boolean
        get() = currentStepIndex < workflow.steps.size - 1

    /**
     * Check if current step can be skipped.
     */
    val canSkip: Boolean
        get() = workflow.metadata.allowSkip || (currentStep?.allowSkip ?: false)

    /**
     * Calculate workflow progress (0.0 to 1.0).
     */
    fun getProgress(): WorkflowProgress {
        val totalSteps = workflow.steps.size
        val completedSteps = stepStates.count { it.value == StepState.COMPLETED || it.value == StepState.SKIPPED }
        val currentStepNum = currentStepIndex + 1

        return WorkflowProgress(
            totalSteps = totalSteps,
            completedSteps = completedSteps,
            currentStep = currentStepNum,
            percentage = (completedSteps.toFloat() / totalSteps) * 100f,
            isComplete = state == WorkflowState.COMPLETED
        )
    }

    /**
     * Move to next step with validation.
     */
    fun next(stepData: Map<String, Any?> = emptyMap()): WorkflowResult {
        val step = currentStep ?: return WorkflowResult.Error("No current step")

        // Validate current step
        data.putAll(stepData)
        val validationResult = step.validate(data)
        if (validationResult is ValidationResult.Failure) {
            return WorkflowResult.ValidationFailed(validationResult.errors)
        }

        // Mark current step as completed
        stepStates[step.id] = StepState.COMPLETED
        history.add(WorkflowTransition(
            fromStep = currentStepIndex,
            toStep = currentStepIndex + 1,
            timestamp = System.currentTimeMillis(),
            action = TransitionAction.NEXT
        ))

        // Execute onComplete callback
        step.onComplete?.invoke(data)

        // Find next visible step
        var nextIndex = currentStepIndex + 1
        while (nextIndex < workflow.steps.size) {
            val nextStep = workflow.steps[nextIndex]

            // Check skip condition
            if (nextStep.shouldSkip(data)) {
                stepStates[nextStep.id] = StepState.SKIPPED
                nextStep.onSkip?.invoke(data)
                nextIndex++
                continue
            }

            // Check condition
            if (!nextStep.shouldShow(data)) {
                stepStates[nextStep.id] = StepState.SKIPPED
                nextIndex++
                continue
            }

            break
        }

        // Check if workflow is complete
        if (nextIndex >= workflow.steps.size) {
            return WorkflowResult.Success(
                copy(
                    state = WorkflowState.COMPLETED,
                    currentStepIndex = workflow.steps.size - 1
                )
            )
        }

        // Move to next step
        val nextStep = workflow.steps[nextIndex]
        stepStates[nextStep.id] = StepState.IN_PROGRESS
        nextStep.onEnter?.invoke(data)

        return WorkflowResult.Success(
            copy(currentStepIndex = nextIndex)
        )
    }

    /**
     * Move to previous step.
     */
    fun back(): WorkflowResult {
        if (!canGoBack) {
            return WorkflowResult.Error("Cannot go back")
        }

        // Mark current step as pending
        currentStep?.let { stepStates[it.id] = StepState.PENDING }

        history.add(WorkflowTransition(
            fromStep = currentStepIndex,
            toStep = currentStepIndex - 1,
            timestamp = System.currentTimeMillis(),
            action = TransitionAction.BACK
        ))

        // Find previous visible step
        var prevIndex = currentStepIndex - 1
        while (prevIndex >= 0) {
            val prevStep = workflow.steps[prevIndex]
            if (prevStep.shouldShow(data) && !prevStep.shouldSkip(data)) {
                break
            }
            prevIndex--
        }

        if (prevIndex < 0) {
            return WorkflowResult.Error("No previous step found")
        }

        // Move to previous step
        val prevStep = workflow.steps[prevIndex]
        stepStates[prevStep.id] = StepState.IN_PROGRESS
        prevStep.onEnter?.invoke(data)

        return WorkflowResult.Success(
            copy(currentStepIndex = prevIndex)
        )
    }

    /**
     * Skip current step.
     */
    fun skip(): WorkflowResult {
        if (!canSkip) {
            return WorkflowResult.Error("Cannot skip current step")
        }

        val step = currentStep ?: return WorkflowResult.Error("No current step")

        stepStates[step.id] = StepState.SKIPPED
        step.onSkip?.invoke(data)

        history.add(WorkflowTransition(
            fromStep = currentStepIndex,
            toStep = currentStepIndex + 1,
            timestamp = System.currentTimeMillis(),
            action = TransitionAction.SKIP
        ))

        return next()
    }

    /**
     * Jump to specific step by ID.
     */
    fun jumpTo(stepId: String): WorkflowResult {
        val targetIndex = workflow.steps.indexOfFirst { it.id == stepId }
        if (targetIndex < 0) {
            return WorkflowResult.Error("Step '$stepId' not found")
        }

        val targetStep = workflow.steps[targetIndex]
        if (!targetStep.shouldShow(data)) {
            return WorkflowResult.Error("Step '$stepId' is not visible")
        }

        // Mark current step as pending
        currentStep?.let { stepStates[it.id] = StepState.PENDING }

        history.add(WorkflowTransition(
            fromStep = currentStepIndex,
            toStep = targetIndex,
            timestamp = System.currentTimeMillis(),
            action = TransitionAction.JUMP
        ))

        // Move to target step
        stepStates[targetStep.id] = StepState.IN_PROGRESS
        targetStep.onEnter?.invoke(data)

        return WorkflowResult.Success(
            copy(currentStepIndex = targetIndex)
        )
    }

    /**
     * Cancel workflow.
     */
    fun cancel(): WorkflowInstance {
        return copy(state = WorkflowState.CANCELLED)
    }

    /**
     * Reset workflow to initial state.
     */
    fun reset(): WorkflowInstance {
        return workflow.createInstance()
    }
}

/**
 * Workflow progress information.
 */
data class WorkflowProgress(
    val totalSteps: Int,
    val completedSteps: Int,
    val currentStep: Int,
    val percentage: Float,
    val isComplete: Boolean
)

/**
 * Workflow transition record.
 */
data class WorkflowTransition(
    val fromStep: Int,
    val toStep: Int,
    val timestamp: Long,
    val action: TransitionAction
)

/**
 * Transition action enumeration.
 */
enum class TransitionAction {
    NEXT,
    BACK,
    SKIP,
    JUMP
}

/**
 * Workflow operation result.
 */
sealed class WorkflowResult {
    data class Success(val instance: WorkflowInstance) : WorkflowResult()
    data class ValidationFailed(val errors: Map<String, List<String>>) : WorkflowResult()
    data class Error(val message: String) : WorkflowResult()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
