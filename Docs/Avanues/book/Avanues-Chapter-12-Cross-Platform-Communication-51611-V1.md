# Chapter 12: Cross-Platform Communication

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~3,000 words

---

## Overview

Cross-platform communication enables sharing data, events, and commands between Android, iOS, and Web instances of Avanues apps.

## Serialization Strategy

### kotlinx.serialization

```kotlin
@Serializable
data class Message(
    val id: String,
    val type: String,
    val payload: Map<String, @Contextual Any>
)

// Serialize
val json = Json.encodeToString(Message.serializer(), message)

// Deserialize
val message = Json.decodeFromString<Message>(json)
```

## Platform-Specific IPC

### Android: Intents + AIDL + Content Provider

**Intent-based:**
```kotlin
val intent = Intent("com.augmentalis.voiceos.ACTION")
intent.putExtra("data", jsonData)
context.startService(intent)
```

**AIDL (Background Service):**
```aidl
// IVoiceOSBridge.aidl
interface IVoiceOSBridge {
    String routeCommand(String voiceInput);
    void sendMessage(String targetApp, String messageJson);
}
```

**Content Provider (Large Data):**
```kotlin
contentResolver.insert(
    Uri.parse("content://com.augmentalis.voiceos/messages"),
    ContentValues().apply {
        put("message_json", json)
    }
)
```

### iOS: URL Schemes + XPC + Universal Links

**URL Schemes:**
```swift
// voiceos://command?action=summarize&content=...
let url = URL(string: "voiceos://command?action=summarize")!
UIApplication.shared.open(url)
```

**XPC (Inter-Process):**
```swift
let connection = NSXPCConnection(serviceName: "com.augmentalis.voiceos.bridge")
connection.remoteObjectInterface = NSXPCInterface(with: VoiceOSBridgeProtocol.self)
connection.resume()

let service = connection.remoteObjectProxy as! VoiceOSBridgeProtocol
service.routeCommand("ask AI") { result in
    // Handle result
}
```

### Web: WebSockets + REST API + localStorage

**WebSocket (Real-time):**
```typescript
const ws = new WebSocket('ws://localhost:8080/voiceos-bridge');

ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    handleMessage(message);
};

ws.send(JSON.stringify({
    type: 'voice.command',
    payload: { command: 'ask AI' }
}));
```

**REST API (HTTP):**
```typescript
fetch('http://localhost:8080/api/command', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command: 'ask AI' })
})
.then(response => response.json())
.then(result => handleResult(result));
```

## Event Bus Architecture

```kotlin
// Common (KMP)
interface EventBus {
    suspend fun emit(event: BridgeEvent)
    fun subscribe(eventType: String, handler: EventHandler): Subscription
}

// Platform implementations
expect class PlatformEventBus : EventBus

// Android
actual class PlatformEventBus : EventBus {
    private val localBroadcastManager = LocalBroadcastManager.getInstance(context)

    override suspend fun emit(event: BridgeEvent) {
        val intent = Intent("voiceos.event.${event.type}")
        intent.putExtra("event_json", Json.encodeToString(event))
        localBroadcastManager.sendBroadcast(intent)
    }
}

// iOS
actual class PlatformEventBus : EventBus {
    override suspend fun emit(event: BridgeEvent) {
        NSNotificationCenter.defaultCenter.postNotificationName(
            "VoiceOS.Event.${event.type}",
            object = nil,
            userInfo = mapOf("event" to event.toNSMap())
        )
    }
}
```

## Message Protocol

### Message Format

```kotlin
@Serializable
data class BridgeMessage(
    val id: String,
    val type: MessageType,
    val source: AppIdentifier,
    val target: AppIdentifier?,
    val payload: Map<String, @Contextual Any>,
    val timestamp: Long,
    val priority: Priority = Priority.NORMAL
)

enum class MessageType {
    COMMAND,
    QUERY,
    RESPONSE,
    EVENT,
    BROADCAST
}

enum class Priority {
    LOW, NORMAL, HIGH, URGENT
}
```

### Example Messages

**Voice Command:**
```json
{
  "id": "msg-123",
  "type": "COMMAND",
  "source": { "appId": "com.augmentalis.voiceos", "platform": "android" },
  "target": { "appId": "com.augmentalis.avanue.ai", "platform": "android" },
  "payload": {
    "command": "ask AI",
    "input": "What is the weather?",
    "context": { "location": "San Francisco" }
  },
  "timestamp": 1699000000000,
  "priority": "HIGH"
}
```

**Query:**
```json
{
  "id": "msg-456",
  "type": "QUERY",
  "source": { "appId": "com.augmentalis.avanue.browser", "platform": "web" },
  "target": { "appId": "com.augmentalis.avanue.ai", "platform": "android" },
  "payload": {
    "action": "ai.summarize",
    "data": { "content": "..." }
  },
  "timestamp": 1699000001000
}
```

**Response:**
```json
{
  "id": "msg-789",
  "type": "RESPONSE",
  "source": { "appId": "com.augmentalis.avanue.ai", "platform": "android" },
  "target": { "appId": "com.augmentalis.avanue.browser", "platform": "web" },
  "payload": {
    "requestId": "msg-456",
    "result": { "summary": "..." }
  },
  "timestamp": 1699000002000
}
```

## Cross-Platform State Sync

### State Synchronization

```kotlin
class StateSyncManager(
    private val localStorage: LocalStorage,
    private val remoteSync: RemoteSync
) {
    suspend fun syncState(key: String, value: Any) {
        // 1. Save locally
        localStorage.put(key, value)

        // 2. Sync to other platforms
        remoteSync.sync(StateUpdate(
            key = key,
            value = value,
            timestamp = System.currentTimeMillis(),
            platform = getCurrentPlatform()
        ))
    }

    suspend fun subscribeToSync(key: String, observer: (Any) -> Unit) {
        remoteSync.subscribe(key) { update ->
            if (update.platform != getCurrentPlatform()) {
                localStorage.put(key, update.value)
                observer(update.value)
            }
        }
    }
}
```

## Deep Linking

### Universal Links / App Links

**iOS (Universal Links):**
```swift
// apple-app-site-association
{
  "applinks": {
    "apps": [],
    "details": [{
      "appID": "TEAM_ID.com.augmentalis.avanue",
      "paths": ["/open/*", "/action/*"]
    }]
  }
}

// Handle link
func application(_ application: UIApplication, continue userActivity: NSUserActivity, ...) -> Bool {
    guard let url = userActivity.webpageURL else { return false }
    // Parse: https://avanues.com/open?command=askAI
    handleDeepLink(url)
    return true
}
```

**Android (App Links):**
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https"
          android:host="avanues.com"
          android:pathPrefix="/open" />
</intent-filter>
```

## Summary

Cross-platform communication uses:
- **Serialization**: kotlinx.serialization for data exchange
- **IPC**: Platform-specific (Intents/XPC/WebSockets)
- **Event Bus**: Shared event system
- **Message Protocol**: Standardized message format
- **State Sync**: Cross-platform state synchronization
- **Deep Linking**: Universal/App Links for navigation

**Next:** Chapter 13 covers Web Interface implementation.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
