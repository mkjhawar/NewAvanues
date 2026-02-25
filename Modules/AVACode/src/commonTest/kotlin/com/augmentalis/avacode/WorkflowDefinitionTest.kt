package com.augmentalis.avacode

import com.augmentalis.avacode.workflows.StepState
import com.augmentalis.avacode.workflows.WorkflowResult
import com.augmentalis.avacode.workflows.WorkflowState
import com.augmentalis.avacode.workflows.workflow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkflowDefinitionTest {

    private fun threeStepWorkflow() = workflow("onboarding") {
        step("step_one") {}
        step("step_two") {}
        step("step_three") {}
    }

    // ─── WorkflowDefinition tests ──────────────────────────────────────────────

    @Test
    fun workflowIdAndStepCountAreCorrect() {
        val wf = threeStepWorkflow()
        assertEquals("onboarding", wf.id)
        assertEquals(3, wf.steps.size)
    }

    @Test
    fun blankWorkflowIdThrows() {
        assertFailsWith<IllegalArgumentException> {
            workflow("") { step("s") {} }
        }
    }

    @Test
    fun emptyStepsThrows() {
        assertFailsWith<IllegalArgumentException> {
            workflow("empty") { /* no steps */ }
        }
    }

    @Test
    fun duplicateStepIdThrows() {
        assertFailsWith<IllegalArgumentException> {
            workflow("dup_wf") {
                step("step_one") {}
                step("step_one") {}
            }
        }
    }

    @Test
    fun createInstanceStartsAtStepZeroInProgress() {
        val instance = threeStepWorkflow().createInstance()

        assertEquals(0, instance.currentStepIndex)
        assertEquals(WorkflowState.IN_PROGRESS, instance.state)
        assertNotNull(instance.currentStep)
        assertEquals("step_one", instance.currentStep?.id)
    }

    @Test
    fun nextAdvancesToSecondStep() {
        val instance = threeStepWorkflow().createInstance()
        val result = instance.next()

        assertIs<WorkflowResult.Success>(result)
        assertEquals(1, result.instance.currentStepIndex)
        assertEquals(StepState.COMPLETED, result.instance.stepStates["step_one"])
    }

    @Test
    fun nextOnLastStepCompletesWorkflow() {
        val wf = workflow("short") { step("only") {} }
        val instance = wf.createInstance()
        val result = instance.next()

        assertIs<WorkflowResult.Success>(result)
        assertEquals(WorkflowState.COMPLETED, result.instance.state)
        assertTrue(result.instance.isComplete)
    }

    @Test
    fun getStepByIdReturnsCorrectStep() {
        val wf = threeStepWorkflow()
        val step = wf.getStep("step_two")
        assertNotNull(step)
        assertEquals("step_two", step.id)
        assertNull(wf.getStep("nonexistent"))
    }

    @Test
    fun progressCalculatedCorrectly() {
        var instance = threeStepWorkflow().createInstance()
        instance = (instance.next() as WorkflowResult.Success).instance // complete step_one

        val progress = instance.getProgress()
        assertEquals(3, progress.totalSteps)
        assertEquals(1, progress.completedSteps)
        assertEquals(2, progress.currentStep)
    }

    @Test
    fun cancelSetsStateToCancelled() {
        val instance = threeStepWorkflow().createInstance()
        val cancelled = instance.cancel()
        assertEquals(WorkflowState.CANCELLED, cancelled.state)
    }
}
