package com.augmentalis.avanues.avacode.workflows

/**
 * Workflow persistence for saving and restoring workflow state.
 *
 * Allows workflows to be saved (for draft/resume functionality) and
 * restored later. Supports serialization to key-value format.
 *
 * @since 1.3.0
 */
object WorkflowPersistence {
    /**
     * Serialize workflow instance to map for storage.
     */
    fun serialize(instance: WorkflowInstance): Map<String, Any?> {
        return mapOf(
            "workflow_id" to instance.workflow.id,
            "current_step_index" to instance.currentStepIndex,
            "state" to instance.state.name,
            "data" to instance.data,
            "step_states" to instance.stepStates.mapValues { it.value.name },
            "history" to instance.history.map { transition ->
                mapOf(
                    "from_step" to transition.fromStep,
                    "to_step" to transition.toStep,
                    "timestamp" to transition.timestamp,
                    "action" to transition.action.name
                )
            },
            "saved_at" to System.currentTimeMillis()
        )
    }

    /**
     * Deserialize workflow instance from stored map.
     *
     * Note: Requires the original WorkflowDefinition to reconstruct the instance.
     */
    fun deserialize(
        data: Map<String, Any?>,
        workflow: WorkflowDefinition
    ): WorkflowInstance? {
        try {
            val workflowId = data["workflow_id"] as? String
            if (workflowId != workflow.id) {
                return null // Wrong workflow
            }

            val currentStepIndex = (data["current_step_index"] as? Number)?.toInt() ?: 0
            val stateName = data["state"] as? String ?: WorkflowState.IN_PROGRESS.name
            val state = WorkflowState.valueOf(stateName)

            @Suppress("UNCHECKED_CAST")
            val workflowData = (data["data"] as? Map<String, Any?>) ?: emptyMap()

            @Suppress("UNCHECKED_CAST")
            val stepStatesMap = (data["step_states"] as? Map<String, String>) ?: emptyMap()
            val stepStates = stepStatesMap.mapValues { StepState.valueOf(it.value) }.toMutableMap()

            @Suppress("UNCHECKED_CAST")
            val historyList = (data["history"] as? List<Map<String, Any?>>) ?: emptyList()
            val history = historyList.mapNotNull { transitionData ->
                val fromStep = (transitionData["from_step"] as? Number)?.toInt() ?: return@mapNotNull null
                val toStep = (transitionData["to_step"] as? Number)?.toInt() ?: return@mapNotNull null
                val timestamp = (transitionData["timestamp"] as? Number)?.toLong() ?: return@mapNotNull null
                val actionName = transitionData["action"] as? String ?: return@mapNotNull null
                val action = TransitionAction.valueOf(actionName)

                WorkflowTransition(fromStep, toStep, timestamp, action)
            }.toMutableList()

            return WorkflowInstance(
                workflow = workflow,
                currentStepIndex = currentStepIndex,
                data = workflowData.toMutableMap(),
                state = state,
                stepStates = stepStates,
                history = history
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Create a checkpoint of workflow state.
     */
    fun checkpoint(instance: WorkflowInstance): WorkflowCheckpoint {
        return WorkflowCheckpoint(
            workflowId = instance.workflow.id,
            stepIndex = instance.currentStepIndex,
            data = instance.data.toMap(),
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Restore workflow from checkpoint.
     */
    fun restore(
        checkpoint: WorkflowCheckpoint,
        workflow: WorkflowDefinition
    ): WorkflowInstance? {
        if (checkpoint.workflowId != workflow.id) {
            return null
        }

        val instance = workflow.createInstance(checkpoint.data)
        return instance.copy(currentStepIndex = checkpoint.stepIndex)
    }
}

/**
 * Workflow checkpoint for save/restore.
 */
data class WorkflowCheckpoint(
    val workflowId: String,
    val stepIndex: Int,
    val data: Map<String, Any?>,
    val timestamp: Long
)

/**
 * Workflow storage interface.
 *
 * Implement this interface to provide custom storage backends
 * (e.g., SQLite, SharedPreferences, localStorage).
 */
interface WorkflowStorage {
    /**
     * Save workflow instance.
     */
    suspend fun save(key: String, instance: WorkflowInstance): Boolean

    /**
     * Load workflow instance.
     */
    suspend fun load(key: String, workflow: WorkflowDefinition): WorkflowInstance?

    /**
     * Delete saved workflow.
     */
    suspend fun delete(key: String): Boolean

    /**
     * List all saved workflow keys.
     */
    suspend fun listKeys(): List<String>
}

/**
 * In-memory workflow storage (for testing/demo).
 */
class InMemoryWorkflowStorage : WorkflowStorage {
    private val storage = mutableMapOf<String, Map<String, Any?>>()

    override suspend fun save(key: String, instance: WorkflowInstance): Boolean {
        storage[key] = WorkflowPersistence.serialize(instance)
        return true
    }

    override suspend fun load(key: String, workflow: WorkflowDefinition): WorkflowInstance? {
        val data = storage[key] ?: return null
        return WorkflowPersistence.deserialize(data, workflow)
    }

    override suspend fun delete(key: String): Boolean {
        return storage.remove(key) != null
    }

    override suspend fun listKeys(): List<String> {
        return storage.keys.toList()
    }
}
