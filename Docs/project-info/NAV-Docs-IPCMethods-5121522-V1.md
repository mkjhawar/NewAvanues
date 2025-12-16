# NewAvanues - IPC Methods

**Version:** 12.0.0 | **Updated:** 2025-12-15

---

## Overview

Inter-Process Communication methods between NewAvanues modules.

---

## Android Intents

### VoiceOS → AVA

#### Voice Command Intent
```kotlin
val intent = Intent("com.augmentalis.ava.PROCESS_COMMAND")
intent.putExtra("command_text", text)
intent.putExtra("confidence", confidence)
intent.putExtra("timestamp", System.currentTimeMillis())
```

**Response:**
```kotlin
// Broadcast response
val response = Intent("com.augmentalis.voiceos.COMMAND_RESULT")
response.putExtra("result_text", responseText)
response.putExtra("success", true)
```

---

### AVA → VoiceOS

#### Assistant Response Intent
```kotlin
val intent = Intent("com.augmentalis.voiceos.ASSISTANT_RESPONSE")
intent.putExtra("response_text", text)
intent.putExtra("actions", actionsList)
```

---

## Service Bindings

### AVA Service
```kotlin
interface IAVAService : IInterface {
    fun processQuery(query: String, context: Bundle): Bundle
    fun getStatus(): ServiceStatus
}
```

**Binding:**
```kotlin
val intent = Intent(context, AVAService::class.java)
bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
```

---

## ContentProvider Contracts

### Voice Command History
```kotlin
content://com.augmentalis.voiceos.provider/commands

Columns:
- _ID: Long
- COMMAND_TEXT: String
- TIMESTAMP: Long
- CONFIDENCE: Float
- RESULT: String
```

---

## Broadcast Receivers

### System Events
```kotlin
// Module status change
Intent("com.augmentalis.system.MODULE_STATUS_CHANGED")
    .putExtra("module_id", moduleId)
    .putExtra("status", status)

// Configuration update
Intent("com.augmentalis.system.CONFIG_UPDATED")
    .putExtra("config_path", path)
```

---

## WebSocket Channels

### Real-Time Updates (WebAvanue ↔ Backend)
```typescript
// Connect
ws://localhost:8080/ws/updates

// Subscribe to module updates
{
    "type": "subscribe",
    "channel": "module:voiceos:status"
}

// Receive updates
{
    "type": "update",
    "channel": "module:voiceos:status",
    "data": { "status": "active", "health": 95 }
}
```

---

## HTTP APIs

### Cockpit Management
```http
POST /api/v1/modules/{moduleId}/command
Content-Type: application/json

{
    "command": "restart",
    "params": {}
}
```

---

## Security

### Intent Permissions
```xml
<permission
    android:name="com.augmentalis.permission.VOICE_COMMAND"
    android:protectionLevel="signature" />
```

### API Authentication
```http
Authorization: Bearer {jwt_token}
```

---

## See Also

- [Intent Registry](INTENT-REGISTRY.md) - Complete Android intent catalog
- [API Contracts](API-CONTRACTS.md) - API specifications

---

**Maintained By:** NewAvanues Team
