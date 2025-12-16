# NewAvanues - IPC Methods

**Version:** 12.0.0
**Updated:** 2025-12-15

---

## Overview

This document defines Inter-Process Communication (IPC) methods used for cross-module communication in the NewAvanues Android environment.

## IPC Mechanisms

### 1. Android Intents

#### Explicit Intents

**VoiceOS → AVA Command Execution**
```kotlin
val intent = Intent(context, AVACommandService::class.java).apply {
    action = "com.augmentalis.ava.EXECUTE_COMMAND"
    putExtra("command", commandText)
    putExtra("context", screenContextJson)
    putExtra("timestamp", System.currentTimeMillis())
}
context.startService(intent)
```

**AVA → VoiceOS Accessibility Action**
```kotlin
val intent = Intent(context, VoiceOSAccessibilityService::class.java).apply {
    action = "com.augmentalis.voiceos.PERFORM_ACTION"
    putExtra("nodeId", targetNodeId)
    putExtra("action", actionType)
}
context.startService(intent)
```

#### Broadcast Intents

**System-Wide Voice Command Broadcast**
```kotlin
// Sender (VoiceOS)
val intent = Intent("com.augmentalis.VOICE_COMMAND").apply {
    putExtra("command", recognizedCommand)
    putExtra("confidence", confidenceScore)
}
sendBroadcast(intent)

// Receiver (AVA, WebAvanue, etc.)
class VoiceCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val command = intent.getStringExtra("command")
        // Process command
    }
}
```

### 2. Content Providers

#### Screen Context Provider

**Authority:** `com.augmentalis.voiceos.provider.screencontext`

**Query Screen Context**
```kotlin
val uri = Uri.parse("content://com.augmentalis.voiceos.provider.screencontext/current")
val cursor = contentResolver.query(
    uri,
    arrayOf("packageName", "activityName", "elements"),
    null,
    null,
    null
)
```

**Content Provider Implementation**
```kotlin
class ScreenContextProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.augmentalis.voiceos.provider.screencontext"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            CURRENT_CONTEXT -> getCurrentContextCursor()
            CONTEXT_HISTORY -> getContextHistoryCursor()
            else -> null
        }
    }
}
```

#### Command History Provider

**Authority:** `com.augmentalis.ava.provider.commands`

**Query Command History**
```kotlin
val uri = Uri.parse("content://com.augmentalis.ava.provider.commands/history")
val cursor = contentResolver.query(
    uri,
    arrayOf("id", "command", "timestamp", "result"),
    "timestamp > ?",
    arrayOf(startTime.toString()),
    "timestamp DESC"
)
```

### 3. Bound Services

#### AVA Service Binding

**Service Interface (AIDL)**
```aidl
// IAVAService.aidl
interface IAVAService {
    CommandResult executeCommand(String command, String context);
    List<Message> getConversationHistory(String conversationId);
    void cancelCommand(String commandId);
}
```

**Client Binding**
```kotlin
class AVAServiceConnection : ServiceConnection {
    private var avaService: IAVAService? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        avaService = IAVAService.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        avaService = null
    }
}

// Bind to service
val intent = Intent(context, AVAService::class.java)
val connection = AVAServiceConnection()
bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

### 4. Message Passing (Messenger)

#### VoiceOS ↔ Cockpit Monitoring

**Cockpit Messenger Service**
```kotlin
class CockpitMonitorService : Service() {
    private val messenger = Messenger(IncomingHandler())

    inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_HEALTH_CHECK -> handleHealthCheck(msg)
                MSG_STATUS_UPDATE -> handleStatusUpdate(msg)
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return messenger.binder
    }
}
```

**Client Sending Messages**
```kotlin
val msg = Message.obtain(null, MSG_STATUS_UPDATE).apply {
    data = Bundle().apply {
        putString("moduleId", "voiceos")
        putString("status", "running")
        putLong("timestamp", System.currentTimeMillis())
    }
}
messenger.send(msg)
```

### 5. Shared Preferences (Config Sync)

**Shared Configuration**
```kotlin
val sharedPrefs = context.getSharedPreferences(
    "com.augmentalis.shared_config",
    Context.MODE_PRIVATE
)

// Write
sharedPrefs.edit {
    putString("voice_language", "en-US")
    putBoolean("offline_mode", false)
}

// Read
val language = sharedPrefs.getString("voice_language", "en-US")
```

## IPC Contract Definitions

### Intent Actions

| Action | Purpose | Sender | Receiver |
|--------|---------|--------|----------|
| `com.augmentalis.VOICE_COMMAND` | Voice command broadcast | VoiceOS | AVA, WebAvanue |
| `com.augmentalis.ava.EXECUTE_COMMAND` | Execute AI command | AVA | VoiceOS |
| `com.augmentalis.voiceos.PERFORM_ACTION` | Accessibility action | Any | VoiceOS |
| `com.augmentalis.web.NAVIGATE` | Web navigation | AVA | WebAvanue |
| `com.augmentalis.cockpit.STATUS_UPDATE` | Module status | Any | Cockpit |

### Intent Extras

| Key | Type | Description |
|-----|------|-------------|
| `command` | String | Command text |
| `context` | String (JSON) | Screen context |
| `timestamp` | Long | Event timestamp |
| `nodeId` | String | Accessibility node ID |
| `action` | String | Action type |
| `url` | String | Target URL |
| `confidence` | Float | Confidence score |

### Content Provider URIs

| URI | Purpose | Access |
|-----|---------|--------|
| `content://com.augmentalis.voiceos.provider.screencontext/current` | Current screen | Read |
| `content://com.augmentalis.voiceos.provider.screencontext/history` | Context history | Read |
| `content://com.augmentalis.ava.provider.commands/history` | Command log | Read |
| `content://com.augmentalis.ava.provider.commands/active` | Active commands | Read/Write |

## Security & Permissions

### Required Permissions

**VoiceOS Service**
```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="com.augmentalis.permission.VOICE_COMMAND" />
```

**AVA Service**
```xml
<uses-permission android:name="com.augmentalis.permission.EXECUTE_COMMAND" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### Custom Permissions

**Define in Manifest**
```xml
<permission
    android:name="com.augmentalis.permission.VOICE_COMMAND"
    android:protectionLevel="signature" />

<permission
    android:name="com.augmentalis.permission.EXECUTE_COMMAND"
    android:protectionLevel="signature" />
```

## Performance Considerations

### Asynchronous Communication

**Always use async for IPC:**
```kotlin
lifecycleScope.launch {
    withContext(Dispatchers.IO) {
        val result = executeIPCCall()
        withContext(Dispatchers.Main) {
            updateUI(result)
        }
    }
}
```

### Batching Requests

**Batch multiple updates:**
```kotlin
val batch = ArrayList<ContentProviderOperation>()
batch.add(ContentProviderOperation.newInsert(uri)
    .withValues(values1)
    .build())
batch.add(ContentProviderOperation.newInsert(uri)
    .withValues(values2)
    .build())
contentResolver.applyBatch(authority, batch)
```

## Testing IPC

**Coverage Required:** 100%

**Test Types:**
- Intent delivery tests
- Content Provider queries
- Service binding tests
- Broadcast receiver tests
- Permission enforcement tests

**Example Test:**
```kotlin
@Test
fun testVoiceCommandBroadcast() {
    val intent = Intent("com.augmentalis.VOICE_COMMAND").apply {
        putExtra("command", "test command")
    }

    val receiver = VoiceCommandReceiver()
    receiver.onReceive(context, intent)

    verify { commandProcessor.process("test command") }
}
```

## References

- **Intent Registry:** NAV-Docs-IntentRegistry-5121522-V1.md
- **API Contracts:** NAV-Docs-APIContracts-5121522-V1.md
- **Architecture:** NAV-Docs-Architecture-5121522-V1.md

---

All IPC methods must follow these patterns for consistency and security.
