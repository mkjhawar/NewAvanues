# Developer Manual - Chapter 68: Workflow Engine Architecture

## Overview

AVA's workflow engine enables users to create automations through natural language. This chapter covers the complete architecture including database schema, state machine implementation, RAG integration, and offline-first execution.

---

## Architecture Overview

```
User: "When I get home, turn on lights and set temp to 72"
                    ↓
               NLU (classify)
                    ↓
           LLM (parse workflow)
                    ↓
         WorkflowStateMachine
                    ↓
    ┌───────────────┴───────────────┐
    ↓                               ↓
SQLite State                   Task Queue
(Execution Log)                (Background)
    ↓                               ↓
    └───────────────┬───────────────┘
                    ↓
              ActionHandlers
    (Lights, Thermostat, Notifications)
```

---

## Core Components

### 1. Workflow State Machine

Based on research from [Temporal](https://temporal.io/blog/workflow-engine-principles), [XState](https://stately.ai/docs/machines), and [KStateMachine](https://github.com/KStateMachine/kstatemachine).

| Component | Purpose |
|-----------|---------|
| States | Defined finite states (start, executing, waiting, completed, error) |
| Transitions | Event → state mapping with guards |
| Context | Infinite-state data (variables, user input) |
| Actions | Side effects (call handler, log, notify) |

### 2. Task Queue Pattern

From [n8n](https://docs.n8n.io/hosting/architecture/database-structure/) and [Conductor](https://orkes.io/content/conductor-architecture):

| Component | Purpose |
|-----------|---------|
| Central Coordinator | Routes tasks, tracks state |
| Task Queue | SQLite-based pending tasks |
| Background Worker | Coroutine-based executor |
| State Store | Execution history, recovery |

### 3. RAG Integration

Based on [EdgeRAG](https://arxiv.org/html/2412.21023v1) for mobile:

| Phase | Purpose |
|-------|---------|
| Design-time RAG | Suggest workflow steps from docs |
| Execution RAG | Dynamic decisions from knowledge base |
| Error RAG | Troubleshooting from guides |

---

## Database Schema

### SQLite Schema (SQLDelight)

```sql
-- Workflow Definitions
CREATE TABLE workflow (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  definition JSON NOT NULL,  -- AVU-1.0 workflow format
  version INTEGER DEFAULT 1,
  enabled INTEGER DEFAULT 1,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

-- State Machine States
CREATE TABLE workflow_state (
  id TEXT PRIMARY KEY,
  workflow_id TEXT NOT NULL,
  name TEXT NOT NULL,
  type TEXT CHECK(type IN ('start', 'normal', 'complete', 'error', 'cancelled')) NOT NULL,
  metadata JSON,
  FOREIGN KEY(workflow_id) REFERENCES workflow(id) ON DELETE CASCADE
);

-- State Transitions
CREATE TABLE transition (
  id TEXT PRIMARY KEY,
  workflow_id TEXT NOT NULL,
  from_state_id TEXT NOT NULL,
  to_state_id TEXT NOT NULL,
  event_type TEXT NOT NULL,
  guard_condition TEXT,
  actions JSON,
  FOREIGN KEY(workflow_id) REFERENCES workflow(id) ON DELETE CASCADE,
  FOREIGN KEY(from_state_id) REFERENCES workflow_state(id),
  FOREIGN KEY(to_state_id) REFERENCES workflow_state(id)
);

-- Workflow Execution Instances
CREATE TABLE execution (
  id TEXT PRIMARY KEY,
  workflow_id TEXT NOT NULL,
  current_state_id TEXT NOT NULL,
  context JSON NOT NULL,  -- Variables, user data
  status TEXT CHECK(status IN ('running', 'paused', 'waiting', 'completed', 'failed')) DEFAULT 'running',
  error_message TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  completed_at INTEGER,
  synced INTEGER DEFAULT 0,
  FOREIGN KEY(workflow_id) REFERENCES workflow(id),
  FOREIGN KEY(current_state_id) REFERENCES workflow_state(id)
);

-- Execution Step Log (Durable Execution)
CREATE TABLE execution_step (
  id TEXT PRIMARY KEY,
  execution_id TEXT NOT NULL,
  step_number INTEGER NOT NULL,
  state_id TEXT NOT NULL,
  action_type TEXT NOT NULL,
  input_data JSON,
  output_data JSON,
  status TEXT CHECK(status IN ('pending', 'running', 'completed', 'failed', 'skipped')) NOT NULL,
  error_message TEXT,
  idempotency_key TEXT UNIQUE,  -- Prevent duplicate side effects
  started_at INTEGER,
  completed_at INTEGER,
  FOREIGN KEY(execution_id) REFERENCES execution(id) ON DELETE CASCADE,
  FOREIGN KEY(state_id) REFERENCES workflow_state(id)
);

-- Background Task Queue
CREATE TABLE task_queue (
  id TEXT PRIMARY KEY,
  execution_id TEXT NOT NULL,
  step_id TEXT,
  task_type TEXT NOT NULL,
  payload JSON NOT NULL,
  priority INTEGER DEFAULT 0,
  retry_count INTEGER DEFAULT 0,
  max_retries INTEGER DEFAULT 3,
  status TEXT CHECK(status IN ('pending', 'processing', 'completed', 'failed', 'cancelled')) DEFAULT 'pending',
  scheduled_at INTEGER,
  started_at INTEGER,
  completed_at INTEGER,
  error_message TEXT,
  created_at INTEGER NOT NULL,
  FOREIGN KEY(execution_id) REFERENCES execution(id) ON DELETE CASCADE
);

-- Triggers (What starts a workflow)
CREATE TABLE workflow_trigger (
  id TEXT PRIMARY KEY,
  workflow_id TEXT NOT NULL,
  trigger_type TEXT NOT NULL,  -- time, location, event, manual
  trigger_config JSON NOT NULL,
  enabled INTEGER DEFAULT 1,
  last_fired_at INTEGER,
  FOREIGN KEY(workflow_id) REFERENCES workflow(id) ON DELETE CASCADE
);

-- RAG Document Cache (for workflow guidance)
CREATE TABLE rag_workflow_doc (
  id TEXT PRIMARY KEY,
  document_type TEXT NOT NULL,  -- guide, example, troubleshooting
  content TEXT NOT NULL,
  embedding BLOB,
  embedding_dims INTEGER DEFAULT 384,
  metadata JSON,
  synced_at INTEGER
);

-- Indexes
CREATE INDEX idx_execution_workflow ON execution(workflow_id);
CREATE INDEX idx_execution_status ON execution(status);
CREATE INDEX idx_task_queue_status ON task_queue(status, scheduled_at);
CREATE INDEX idx_trigger_type ON workflow_trigger(trigger_type, enabled);
CREATE INDEX idx_step_execution ON execution_step(execution_id, step_number);
CREATE INDEX idx_step_idempotency ON execution_step(idempotency_key);
```

---

## State Machine Implementation

### Kotlin State Machine (KStateMachine-inspired)

```kotlin
// WorkflowState.kt
sealed class WorkflowState {
    object Idle : WorkflowState()
    object Start : WorkflowState()
    data class Executing(val stepId: String) : WorkflowState()
    data class WaitingForInput(val prompt: String) : WorkflowState()
    data class WaitingForTrigger(val triggerId: String) : WorkflowState()
    object Completed : WorkflowState()
    data class Failed(val error: String) : WorkflowState()
    object Cancelled : WorkflowState()
}

// WorkflowEvent.kt
sealed class WorkflowEvent {
    object Start : WorkflowEvent()
    data class StepCompleted(val result: Any?) : WorkflowEvent()
    data class StepFailed(val error: String) : WorkflowEvent()
    data class InputReceived(val data: Map<String, Any>) : WorkflowEvent()
    data class TriggerFired(val triggerId: String, val data: Any?) : WorkflowEvent()
    object Pause : WorkflowEvent()
    object Resume : WorkflowEvent()
    object Cancel : WorkflowEvent()
    object Retry : WorkflowEvent()
}

// WorkflowContext.kt
@Serializable
data class WorkflowContext(
    val executionId: String,
    val workflowId: String,
    val variables: MutableMap<String, JsonElement> = mutableMapOf(),
    val stepHistory: MutableList<ExecutedStep> = mutableListOf(),
    val retryCount: Int = 0
)

@Serializable
data class ExecutedStep(
    val stepId: String,
    val actionType: String,
    val status: String,
    val output: JsonElement?,
    val timestamp: Long
)
```

### State Machine Engine

```kotlin
// WorkflowStateMachine.kt
class WorkflowStateMachine(
    private val workflowRepository: WorkflowRepository,
    private val taskQueue: TaskQueue,
    private val actionHandlers: Map<String, ActionHandler>
) {
    private var currentState: WorkflowState = WorkflowState.Idle
    private var context: WorkflowContext? = null

    suspend fun start(workflowId: String, initialData: Map<String, Any>? = null): String {
        val workflow = workflowRepository.getWorkflow(workflowId)
            ?: throw WorkflowNotFoundException(workflowId)

        val execution = workflowRepository.createExecution(
            workflowId = workflowId,
            initialContext = WorkflowContext(
                executionId = UUID.randomUUID().toString(),
                workflowId = workflowId,
                variables = initialData?.toJsonMap() ?: mutableMapOf()
            )
        )

        context = execution.context
        currentState = WorkflowState.Start

        // Execute first step
        executeNextStep()

        return execution.id
    }

    suspend fun processEvent(event: WorkflowEvent) {
        val ctx = context ?: throw IllegalStateException("No active execution")

        val newState = when (currentState) {
            is WorkflowState.Idle -> handleIdleEvent(event)
            is WorkflowState.Start -> handleStartEvent(event)
            is WorkflowState.Executing -> handleExecutingEvent(event)
            is WorkflowState.WaitingForInput -> handleWaitingEvent(event)
            is WorkflowState.WaitingForTrigger -> handleTriggerEvent(event)
            is WorkflowState.Completed -> currentState
            is WorkflowState.Failed -> handleFailedEvent(event)
            is WorkflowState.Cancelled -> currentState
        }

        if (newState != currentState) {
            transition(newState)
        }
    }

    private suspend fun transition(newState: WorkflowState) {
        // Log transition
        workflowRepository.logTransition(
            executionId = context!!.executionId,
            fromState = currentState,
            toState = newState
        )

        currentState = newState

        // Persist state
        workflowRepository.updateExecutionState(
            executionId = context!!.executionId,
            state = newState,
            context = context!!
        )

        // Execute side effects
        when (newState) {
            is WorkflowState.Executing -> executeStep(newState.stepId)
            is WorkflowState.Completed -> onComplete()
            is WorkflowState.Failed -> onFailed(newState.error)
            else -> { /* No side effects */ }
        }
    }

    private suspend fun executeStep(stepId: String) {
        val ctx = context ?: return
        val step = workflowRepository.getStep(ctx.workflowId, stepId) ?: return

        // Check idempotency
        val idempotencyKey = "${ctx.executionId}:${stepId}"
        val existingResult = workflowRepository.getStepByIdempotencyKey(idempotencyKey)
        if (existingResult?.status == "completed") {
            // Skip already completed step
            processEvent(WorkflowEvent.StepCompleted(existingResult.output))
            return
        }

        // Queue task for background execution
        taskQueue.enqueue(
            Task(
                id = UUID.randomUUID().toString(),
                executionId = ctx.executionId,
                stepId = stepId,
                taskType = step.actionType,
                payload = step.config,
                idempotencyKey = idempotencyKey
            )
        )
    }
}
```

---

## Task Queue & Background Worker

### Task Queue Implementation

```kotlin
// TaskQueue.kt
class TaskQueue(
    private val database: WorkflowDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _taskFlow = MutableSharedFlow<Task>()

    fun enqueue(task: Task) {
        database.taskQueueQueries.insert(
            id = task.id,
            execution_id = task.executionId,
            step_id = task.stepId,
            task_type = task.taskType,
            payload = task.payload.toString(),
            priority = task.priority,
            status = "pending",
            created_at = System.currentTimeMillis()
        )

        scope.launch {
            _taskFlow.emit(task)
        }
    }

    suspend fun poll(): Task? {
        return database.taskQueueQueries.getNextPendingTask().executeAsOneOrNull()?.toTask()
    }

    fun markProcessing(taskId: String) {
        database.taskQueueQueries.updateStatus(
            id = taskId,
            status = "processing",
            started_at = System.currentTimeMillis()
        )
    }

    fun markCompleted(taskId: String, result: Any?) {
        database.taskQueueQueries.complete(
            id = taskId,
            status = "completed",
            completed_at = System.currentTimeMillis()
        )
    }

    fun markFailed(taskId: String, error: String) {
        val task = database.taskQueueQueries.getById(taskId).executeAsOne()
        if (task.retry_count < task.max_retries) {
            // Retry with exponential backoff
            val delay = (2.0.pow(task.retry_count) * 1000).toLong()
            database.taskQueueQueries.retry(
                id = taskId,
                retry_count = task.retry_count + 1,
                scheduled_at = System.currentTimeMillis() + delay,
                status = "pending"
            )
        } else {
            database.taskQueueQueries.fail(
                id = taskId,
                status = "failed",
                error_message = error,
                completed_at = System.currentTimeMillis()
            )
        }
    }
}
```

### Background Worker

```kotlin
// WorkflowWorker.kt
class WorkflowWorker(
    private val taskQueue: TaskQueue,
    private val actionHandlers: Map<String, ActionHandler>,
    private val workflowStateMachine: WorkflowStateMachine
) {
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        isRunning = true
        scope.launch {
            while (isRunning) {
                val task = taskQueue.poll()
                if (task != null) {
                    processTask(task)
                } else {
                    delay(1000) // Polling interval
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.cancel()
    }

    private suspend fun processTask(task: Task) {
        taskQueue.markProcessing(task.id)

        try {
            val handler = actionHandlers[task.taskType]
                ?: throw ActionNotFoundException(task.taskType)

            val result = handler.execute(task.payload)

            taskQueue.markCompleted(task.id, result)

            // Notify state machine
            workflowStateMachine.processEvent(
                WorkflowEvent.StepCompleted(result)
            )
        } catch (e: Exception) {
            taskQueue.markFailed(task.id, e.message ?: "Unknown error")

            workflowStateMachine.processEvent(
                WorkflowEvent.StepFailed(e.message ?: "Unknown error")
            )
        }
    }
}
```

---

## RAG Integration for Workflows

### Workflow RAG Service

```kotlin
// WorkflowRagService.kt
class WorkflowRagService(
    private val embeddingModel: EmbeddingModel,
    private val vectorDb: WorkflowVectorDb,
    private val llmService: LlmActionService
) {
    /**
     * Suggest workflow steps based on user intent
     */
    suspend fun suggestWorkflowSteps(userIntent: String): List<WorkflowSuggestion> {
        // Embed user intent
        val queryEmbedding = embeddingModel.embed(userIntent)

        // Retrieve similar workflow examples
        val docs = vectorDb.search(
            embedding = queryEmbedding,
            topK = 5,
            filter = mapOf("type" to "workflow_example")
        )

        // Use LLM to refine suggestions
        val context = docs.joinToString("\n\n") { it.content }

        val response = llmService.process(
            promptTemplate = "suggest_workflow_steps",
            context = context,
            userInput = userIntent
        )

        return parseWorkflowSuggestions(response)
    }

    /**
     * Get next step recommendation during execution
     */
    suspend fun getNextStepRecommendation(
        executionContext: WorkflowContext,
        currentStep: String
    ): String {
        val query = "workflow step after $currentStep with context: ${executionContext.variables}"

        val docs = vectorDb.search(
            embedding = embeddingModel.embed(query),
            topK = 3,
            filter = mapOf("type" to "workflow_guide")
        )

        return llmService.process(
            promptTemplate = "workflow_next_step",
            context = docs.joinToString("\n") { it.content },
            userInput = query
        ).content
    }

    /**
     * Get troubleshooting guidance for errors
     */
    suspend fun getTroubleshootingGuide(
        errorType: String,
        errorMessage: String
    ): TroubleshootingResult {
        val query = "troubleshoot workflow error: $errorType - $errorMessage"

        val docs = vectorDb.search(
            embedding = embeddingModel.embed(query),
            topK = 3,
            filter = mapOf("type" to "troubleshooting")
        )

        val response = llmService.process(
            promptTemplate = "troubleshoot_workflow",
            context = docs.joinToString("\n") { it.content },
            userInput = errorMessage
        )

        return parseTroubleshootingResult(response)
    }
}
```

### Pre-seeded RAG Documents

```kotlin
// Seed workflow guidance documents
val workflowExamples = listOf(
    RagDocument(
        id = "wf-example-1",
        type = "workflow_example",
        content = """
            Workflow: Morning Routine
            Trigger: time at 07:00 weekdays
            Steps:
            1. turn_on_lights (bedroom, 50%)
            2. speak ("Good morning! Here's your weather")
            3. check_weather (current location)
            4. speak (weather_result)
            5. play_music (morning playlist)
        """.trimIndent()
    ),
    RagDocument(
        id = "wf-example-2",
        type = "workflow_example",
        content = """
            Workflow: Leaving Home
            Trigger: location_leave (home)
            Steps:
            1. turn_off_lights (all)
            2. set_thermostat (eco mode)
            3. lock_doors (all)
            4. send_text (spouse, "Leaving home now")
        """.trimIndent()
    ),
    // ... more examples
)

val troubleshootingDocs = listOf(
    RagDocument(
        id = "ts-1",
        type = "troubleshooting",
        content = """
            Error: Smart home device unreachable
            Causes:
            - Device offline (check WiFi)
            - Hub disconnected
            - Network timeout
            Solution:
            1. Check device power
            2. Verify WiFi connection
            3. Restart hub
            4. Retry action with longer timeout
        """.trimIndent()
    ),
    // ... more troubleshooting guides
)
```

---

## Workflow Definition Format (AVU-1.0)

### Standard Workflow Schema

```yaml
---
schema: avu-1.0
type: workflow
version: 1.0.0
---
WORKFLOW:id:wf-morning-routine
WORKFLOW:name:Morning Routine
WORKFLOW:description:Automated morning tasks
WORKFLOW:enabled:true
---
TRIGGER:type:time
TRIGGER:param:at:07:00
TRIGGER:param:days:mon,tue,wed,thu,fri
---
STATE:start:start_state
STATE:normal:lights_on
STATE:normal:weather_check
STATE:normal:play_music
STATE:complete:done
---
TRANSITION:start_state:lights_on:on_start
TRANSITION:lights_on:weather_check:step_complete
TRANSITION:weather_check:play_music:step_complete
TRANSITION:play_music:done:step_complete
---
ACTION:1:state:lights_on:type:control_light
ACTION:1:param:device:bedroom
ACTION:1:param:state:true
ACTION:1:param:brightness:50
---
ACTION:2:state:weather_check:type:check_weather
ACTION:2:param:location:current
---
ACTION:3:state:weather_check:type:speak
ACTION:3:param:text:{{weather_result}}
---
ACTION:4:state:play_music:type:play_media
ACTION:4:param:playlist:morning
ACTION:4:param:shuffle:true
```

---

## Available Triggers

| Trigger Type | Config | Description |
|--------------|--------|-------------|
| time | at, days, repeat | Schedule-based |
| location_arrive | place (home/work/address) | Geofence entry |
| location_leave | place | Geofence exit |
| battery_low | threshold (%) | Battery below level |
| battery_charging | - | Plugged in |
| wifi_connected | ssid | Connected to network |
| wifi_disconnected | - | Lost connection |
| bluetooth_connected | device | Device paired |
| app_opened | package | App launched |
| notification | app, keyword | Notification received |
| manual | - | User-triggered only |

---

## Available Actions

| Action Type | Parameters | Description |
|-------------|------------|-------------|
| control_light | device, state, brightness, color | Smart lights |
| set_thermostat | temperature, mode | Temperature |
| lock_door | device, state | Smart locks |
| send_text | to, message | SMS |
| send_email | to, subject, body | Email |
| speak | text | TTS output |
| notification | title, body, channel | Show notification |
| play_media | action, playlist, artist | Media control |
| toggle_wifi | state | WiFi on/off |
| toggle_bluetooth | state | Bluetooth on/off |
| toggle_dnd | state | Do not disturb |
| launch_app | package | Open app |
| set_volume | stream, level | Volume control |
| set_brightness | level | Screen brightness |
| wait | duration | Delay execution |
| condition | expression | Branch logic |

---

## Best Practices

### Durable Execution

| Practice | Description |
|----------|-------------|
| Idempotency keys | Prevent duplicate side effects on retry |
| Step logging | Record every step's input/output |
| Atomic persistence | Transaction for state + queue updates |
| Exponential backoff | Retry with increasing delays |

### Performance

| Metric | Target |
|--------|--------|
| Workflow parse | <50ms |
| Step execution | <100ms (local), <2s (network) |
| State persistence | <20ms |
| RAG retrieval | <200ms |

### Offline-First

| Pattern | Implementation |
|---------|---------------|
| Local state | SQLite for all workflow state |
| Task queue | Persist tasks, execute when possible |
| Sync on connect | Outbox pattern for cloud sync |
| Conflict resolution | Last-write-wins or manual merge |

---

## Sources

- [QCon SF: Database-Backed Workflow Orchestration](https://www.infoq.com/news/2025/11/database-backed-workflow/)
- [Temporal Workflow Engine Principles](https://temporal.io/blog/workflow-engine-principles)
- [n8n Database Structure](https://docs.n8n.io/hosting/architecture/database-structure/)
- [XState Documentation](https://stately.ai/docs/machines)
- [EdgeRAG for Mobile](https://arxiv.org/html/2412.21023v1)
- [Building Durable Execution with SQLite](https://www.morling.dev/blog/building-durable-execution-engine-with-sqlite/)

---

## Author

Manoj Jhawar
