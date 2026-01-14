# Chapter 11: VoiceOSBridge Architecture

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~4,000 words

---

## Current Status

**⚠️ CRITICAL GAP:** VoiceOSBridge folder is **EMPTY** (only `build.gradle.kts` exists)

**Location:** `avanues/core/voiceosbridge/`

**Required Effort:** 80 hours (Weeks 1-2 of implementation plan)

## Architecture Design

```
┌──────────────────────────────────────────────────────────────┐
│                    VoiceOSBridge                             │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐   │
│  │ Capability  │  │   Command    │  │   IPC           │   │
│  │ Registry    │  │   Router     │  │   Manager       │   │
│  └─────────────┘  └──────────────┘  └─────────────────┘   │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐   │
│  │ State       │  │   Event      │  │   Security      │   │
│  │ Manager     │  │   Bus        │  │   Manager       │   │
│  └─────────────┘  └──────────────┘  └─────────────────┘   │
└──────────────────────────────────────────────────────────────┘
          │                    │                    │
     ┌────┴────┐         ┌─────┴─────┐       ┌─────┴─────┐
     │ VoiceOS │         │AIAvanue   │       │Browser    │
     └─────────┘         └───────────┘       │Avanue     │
                                              └───────────┘
```

## Core Interface

```kotlin
// avanues/core/voiceosbridge/src/commonMain/kotlin/VoiceOSBridge.kt

/**
 * VoiceOSBridge - Central communication hub for Avanues ecosystem
 */
interface VoiceOSBridge {
    // 1. Capability Discovery
    suspend fun registerCapability(capability: AppCapability): Result<Unit>
    suspend fun unregisterCapability(appId: String): Result<Unit>
    suspend fun queryCapabilities(filter: CapabilityFilter): Result<List<AppCapability>>
    suspend fun getCapability(appId: String): Result<AppCapability?>

    // 2. Voice Command Routing
    suspend fun registerVoiceCommand(command: VoiceCommand): Result<Unit>
    suspend fun unregisterVoiceCommand(commandId: String): Result<Unit>
    suspend fun routeCommand(voiceInput: String): Result<CommandResult>
    suspend fun matchCommand(voiceInput: String): Result<List<CommandMatch>>

    // 3. Inter-App Communication
    suspend fun sendMessage(message: AppMessage): Result<MessageResult>
    suspend fun sendBroadcast(broadcast: BroadcastMessage): Result<Int>
    suspend fun subscribeToMessages(filter: MessageFilter, handler: MessageHandler): Subscription
    suspend fun unsubscribe(subscription: Subscription): Result<Unit>

    // 4. State Management
    suspend fun publishState(key: String, value: Any, scope: StateScope): Result<Unit>
    suspend fun getState(key: String): Result<Any?>
    suspend fun subscribeToState(key: String, observer: StateObserver): Subscription
    suspend fun deleteState(key: String): Result<Unit>

    // 5. Event Bus
    suspend fun emitEvent(event: BridgeEvent): Result<Unit>
    suspend fun subscribeToEvents(eventType: String, handler: EventHandler): Subscription

    // 6. Security & Permissions
    suspend fun requestPermission(permission: BridgePermission): Result<PermissionResult>
    suspend fun checkPermission(permission: BridgePermission): Result<Boolean>
    suspend fun revokePermission(appId: String, permission: BridgePermission): Result<Unit>

    companion object {
        fun getInstance(): VoiceOSBridge
    }
}
```

## Implementation Plan

### 1. Capability Registry (16 hours)

```kotlin
// CapabilityRegistry.kt

class CapabilityRegistry {
    private val capabilities = mutableMapOf<String, AppCapability>()
    private val mutex = Mutex()

    suspend fun register(capability: AppCapability): Result<Unit> = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (capabilities.containsKey(capability.id)) {
                return@withContext Result.failure(Exception("Capability already registered: ${capability.id}"))
            }

            capabilities[capability.id] = capability
            Result.success(Unit)
        }
    }

    suspend fun query(filter: CapabilityFilter): List<AppCapability> = mutex.withLock {
        capabilities.values.filter { capability ->
            filter.matches(capability)
        }
    }
}

data class AppCapability(
    val id: String,                           // "com.augmentalis.avanue.ai"
    val name: String,                         // "AIAvanue"
    val version: String,                      // "1.0.0"
    val category: String,                     // "ai", "browser", "notes"
    val voiceCommands: List<String>,          // ["ask AI", "generate text"]
    val actions: List<Action>,                // [Action("ai.query", ...)]
    val permissions: List<String>,            // ["INTERNET", "STORAGE"]
    val icon: String? = null,                 // Icon URL/resource
    val description: String? = null           // Short description
)

data class Action(
    val id: String,                           // "ai.query"
    val name: String,                         // "Query AI"
    val inputSchema: Map<String, Any>,        // JSON schema
    val outputSchema: Map<String, Any>
)

data class CapabilityFilter(
    val category: String? = null,
    val actions: List<String>? = null,
    val voiceCommands: List<String>? = null,
    val minVersion: String? = null
) {
    fun matches(capability: AppCapability): Boolean {
        if (category != null && capability.category != category) return false
        if (actions != null && !capability.actions.any { it.id in actions }) return false
        // ... more filters
        return true
    }
}
```

### 2. Command Router (24 hours)

```kotlin
// CommandRouter.kt

class CommandRouter(
    private val capabilityRegistry: CapabilityRegistry,
    private val commandMatcher: CommandMatcher
) {
    private val commandRegistry = mutableMapOf<String, RegisteredCommand>()

    suspend fun register(command: VoiceCommand): Result<Unit> {
        if (!capabilityRegistry.hasCapability(command.appId)) {
            return Result.failure(Exception("App not registered: ${command.appId}"))
        }

        commandRegistry[command.id] = RegisteredCommand(command, System.currentTimeMillis())
        return Result.success(Unit)
    }

    suspend fun route(voiceInput: String): Result<CommandResult> {
        // 1. Match voice input to commands
        val matches = matchCommand(voiceInput)
        if (matches.isEmpty()) {
            return Result.failure(NoMatchException("No command matched: $voiceInput"))
        }

        // 2. Select best match
        val bestMatch = matches.maxByOrNull { it.confidence }!!

        // 3. Route to target app
        return routeToApp(bestMatch)
    }

    private suspend fun matchCommand(voiceInput: String): List<CommandMatch> {
        return commandRegistry.values.mapNotNull { registered ->
            val confidence = commandMatcher.match(voiceInput, registered.command.trigger)
            if (confidence > 0.7f) {
                CommandMatch(registered.command, confidence)
            } else null
        }
    }

    private suspend fun routeToApp(match: CommandMatch): Result<CommandResult> {
        val command = match.command
        val message = AppMessage(
            id = UUID.randomUUID().toString(),
            senderId = "voiceos.bridge",
            targetId = command.appId,
            type = "voice.command",
            payload = mapOf(
                "command" to command.trigger,
                "action" to command.action,
                "confidence" to match.confidence
            ),
            timestamp = System.currentTimeMillis()
        )

        return ipcManager.sendMessage(message)
    }
}

data class VoiceCommand(
    val id: String,
    val trigger: String,              // "ask AI to summarize"
    val action: String,               // "ai.summarize"
    val appId: String,                // Target app
    val parameters: Map<String, Any> = emptyMap()
)

data class CommandMatch(
    val command: VoiceCommand,
    val confidence: Float             // 0.7 - 1.0
)

data class CommandResult(
    val handled: Boolean,
    val result: Any?,
    val error: String?
)
```

### 3. IPC Manager (24 hours)

```kotlin
// IPCManager.kt

interface IPCManager {
    suspend fun sendMessage(message: AppMessage): Result<MessageResult>
    suspend fun sendBroadcast(broadcast: BroadcastMessage): Result<Int>
    suspend fun subscribeToMessages(filter: MessageFilter, handler: MessageHandler): Subscription
}

// Android Implementation
class AndroidIPCManager : IPCManager {
    override suspend fun sendMessage(message: AppMessage): Result<MessageResult> {
        // Use Android Intents
        val intent = Intent("com.augmentalis.voiceos.MESSAGE")
        intent.setPackage(message.targetId)
        intent.putExtra("messageId", message.id)
        intent.putExtra("senderId", message.senderId)
        intent.putExtra("type", message.type)
        intent.putExtra("payload", serializePayload(message.payload))

        return try {
            context.startService(intent)
            Result.success(MessageResult(success = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendBroadcast(broadcast: BroadcastMessage): Result<Int> {
        val intent = Intent("com.augmentalis.voiceos.BROADCAST")
        intent.putExtra("type", broadcast.type)
        intent.putExtra("payload", serializePayload(broadcast.payload))

        context.sendBroadcast(intent)
        return Result.success(1) // Can't know recipient count
    }
}

// iOS Implementation
class IOSIPCManager : IPCManager {
    override suspend fun sendMessage(message: AppMessage): Result<MessageResult> {
        // Use URL schemes
        val urlString = "voiceos://message?" +
                        "id=${message.id}&" +
                        "sender=${message.senderId}&" +
                        "type=${message.type}&" +
                        "payload=${encodePayload(message.payload)}"

        val url = NSURL(string = urlString)
        UIApplication.sharedApplication.openURL(url)

        return Result.success(MessageResult(success = true))
    }
}

data class AppMessage(
    val id: String,
    val senderId: String,
    val targetId: String,
    val type: String,
    val payload: Map<String, Any>,
    val timestamp: Long,
    val replyTo: String? = null
)

data class MessageResult(
    val success: Boolean,
    val response: Any? = null,
    val error: String? = null
)
```

### 4. State Manager (16 hours)

```kotlin
// StateManager.kt

class StateManager {
    private val globalState = mutableMapOf<String, StateEntry>()
    private val appState = mutableMapOf<String, MutableMap<String, StateEntry>>()
    private val observers = mutableMapOf<String, MutableList<StateObserver>>()

    suspend fun publish(key: String, value: Any, scope: StateScope): Result<Unit> {
        val entry = StateEntry(value, System.currentTimeMillis(), scope)

        when (scope) {
            StateScope.GLOBAL -> globalState[key] = entry
            is StateScope.APP -> {
                appState.getOrPut(scope.appId) { mutableMapOf() }[key] = entry
            }
        }

        // Notify observers
        observers[key]?.forEach { observer ->
            observer.onStateChanged(key, value)
        }

        return Result.success(Unit)
    }

    suspend fun get(key: String, appId: String? = null): Result<Any?> {
        // Check app scope first
        if (appId != null) {
            appState[appId]?.get(key)?.let { return Result.success(it.value) }
        }

        // Check global scope
        globalState[key]?.let { return Result.success(it.value) }

        return Result.success(null)
    }

    fun subscribe(key: String, observer: StateObserver): Subscription {
        observers.getOrPut(key) { mutableListOf() }.add(observer)
        return Subscription(key, observer)
    }
}

data class StateEntry(
    val value: Any,
    val timestamp: Long,
    val scope: StateScope
)

sealed class StateScope {
    object GLOBAL : StateScope()
    data class APP(val appId: String) : StateScope()
}

interface StateObserver {
    fun onStateChanged(key: String, value: Any)
}
```

## Security Model

```kotlin
// SecurityManager.kt

class SecurityManager {
    private val permissions = mutableMapOf<String, Set<BridgePermission>>()

    suspend fun requestPermission(appId: String, permission: BridgePermission): Result<PermissionResult> {
        // Check if permission already granted
        if (hasPermission(appId, permission)) {
            return Result.success(PermissionResult.GRANTED)
        }

        // Request from user (show dialog)
        val result = showPermissionDialog(appId, permission)

        if (result == PermissionResult.GRANTED) {
            grantPermission(appId, permission)
        }

        return Result.success(result)
    }

    private fun hasPermission(appId: String, permission: BridgePermission): Boolean {
        return permissions[appId]?.contains(permission) == true
    }

    private fun grantPermission(appId: String, permission: BridgePermission) {
        permissions.getOrPut(appId) { mutableSetOf() } += permission
    }
}

enum class BridgePermission {
    SEND_MESSAGES,
    RECEIVE_BROADCASTS,
    ACCESS_GLOBAL_STATE,
    REGISTER_VOICE_COMMANDS,
    ACCESS_CONTACTS,
    ACCESS_LOCATION,
    ACCESS_MICROPHONE,
    ACCESS_CAMERA
}

enum class PermissionResult {
    GRANTED,
    DENIED,
    DENIED_PERMANENTLY
}
```

## Usage Examples

### Example 1: AIAvanue Registration

```kotlin
// AIAvanue startup
val bridge = VoiceOSBridge.getInstance()

bridge.registerCapability(AppCapability(
    id = "com.augmentalis.avanue.ai",
    name = "AIAvanue",
    version = "1.0.0",
    category = "ai",
    voiceCommands = listOf("ask AI", "generate text", "summarize"),
    actions = listOf(
        Action(
            id = "ai.query",
            name = "Query AI",
            inputSchema = mapOf("query" to "string"),
            outputSchema = mapOf("response" to "string")
        )
    ),
    permissions = listOf("INTERNET")
))
```

### Example 2: Browser → AI Communication

```kotlin
// BrowserAvanue sends summarization request
bridge.sendMessage(AppMessage(
    id = UUID.randomUUID().toString(),
    senderId = "com.augmentalis.avanue.browser",
    targetId = "com.augmentalis.avanue.ai",
    type = "ai.summarize",
    payload = mapOf(
        "content" to pageContent,
        "url" to pageUrl,
        "title" to pageTitle
    ),
    timestamp = System.currentTimeMillis()
))

// AIAvanue receives and responds
bridge.subscribeToMessages(MessageFilter(type = "ai.summarize")) { message ->
    val content = message.payload["content"] as String
    val summary = aiService.summarize(content)

    bridge.sendMessage(AppMessage(
        id = UUID.randomUUID().toString(),
        senderId = "com.augmentalis.avanue.ai",
        targetId = message.senderId,
        type = "ai.result",
        payload = mapOf("summary" to summary),
        timestamp = System.currentTimeMillis(),
        replyTo = message.id
    ))
}
```

## Summary

VoiceOSBridge requires complete implementation across 6 subsystems:
1. **Capability Registry** (16h) - App discovery
2. **Command Router** (24h) - Voice routing
3. **IPC Manager** (24h) - Inter-app communication
4. **State Manager** (16h) - Shared state
5. **Event Bus** (8h) - Event propagation
6. **Security Manager** (12h) - Permissions

**Total Effort:** 80 hours (2 weeks)

**Next:** Chapter 12 covers Cross-Platform Communication patterns.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
