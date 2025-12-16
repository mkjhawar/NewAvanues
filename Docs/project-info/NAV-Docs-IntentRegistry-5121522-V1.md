# NewAvanues - Intent Registry

**Version:** 12.0.0 | **Updated:** 2025-12-15

---

## Overview

Complete registry of Android intents used across NewAvanues modules.

---

## VoiceOS Intents

### Outgoing (VoiceOS → Other Modules)

| Intent | Target | Purpose | Extras |
|--------|--------|---------|--------|
| `com.augmentalis.ava.PROCESS_COMMAND` | AVA | Send voice command | `command_text`, `confidence` |
| `com.augmentalis.nlu.EXTRACT_INTENT` | NLU | Extract intent | `text`, `context` |
| `com.augmentalis.system.MODULE_STATUS` | System | Report status | `module_id`, `status`, `health` |

### Incoming (Other Modules → VoiceOS)

| Intent | Source | Purpose | Extras |
|--------|--------|---------|--------|
| `com.augmentalis.voiceos.ASSISTANT_RESPONSE` | AVA | Assistant response | `response_text`, `actions` |
| `com.augmentalis.voiceos.COMMAND_RESULT` | AVA | Command result | `result_text`, `success` |
| `com.augmentalis.voiceos.CONFIG_UPDATE` | Cockpit | Configuration update | `config_data` |

---

## AVA Intents

### Outgoing (AVA → Other Modules)

| Intent | Target | Purpose | Extras |
|--------|--------|---------|--------|
| `com.augmentalis.voiceos.ASSISTANT_RESPONSE` | VoiceOS | Send response | `response_text`, `actions` |
| `com.augmentalis.nlu.PROCESS_TEXT` | NLU | Process text | `text`, `context` |

### Incoming (Other Modules → AVA)

| Intent | Source | Purpose | Extras |
|--------|--------|---------|--------|
| `com.augmentalis.ava.PROCESS_COMMAND` | VoiceOS | Process command | `command_text`, `confidence` |
| `com.augmentalis.ava.CONFIG_UPDATE` | Cockpit | Configuration update | `config_data` |

---

## Cockpit Intents

### Broadcast Intents

| Intent | Direction | Purpose | Extras |
|--------|-----------|---------|--------|
| `com.augmentalis.system.MODULE_STATUS_CHANGED` | Outgoing | Module status change | `module_id`, `status` |
| `com.augmentalis.system.CONFIG_UPDATED` | Outgoing | Configuration updated | `config_path`, `module_id` |
| `com.augmentalis.system.HEALTH_CHECK` | Incoming | Health check request | `timestamp` |

---

## NLU Intents

### Service Intents

| Intent | Type | Purpose | Extras |
|--------|------|---------|--------|
| `com.augmentalis.nlu.EXTRACT_INTENT` | Service | Extract intent from text | `text`, `context` |
| `com.augmentalis.nlu.EXTRACT_ENTITIES` | Service | Extract entities | `text` |
| `com.augmentalis.nlu.PROCESS_TEXT` | Service | Full NLU processing | `text`, `context` |

---

## Permission Requirements

### Custom Permissions

```xml
<!-- Voice command processing -->
<permission
    android:name="com.augmentalis.permission.VOICE_COMMAND"
    android:protectionLevel="signature" />

<!-- Module management -->
<permission
    android:name="com.augmentalis.permission.MODULE_MANAGEMENT"
    android:protectionLevel="signature" />

<!-- System configuration -->
<permission
    android:name="com.augmentalis.permission.SYSTEM_CONFIG"
    android:protectionLevel="signature" />
```

### Required Permissions by Module

| Module | Permissions |
|--------|-------------|
| VoiceOS | `VOICE_COMMAND`, `BIND_ACCESSIBILITY_SERVICE` |
| AVA | `VOICE_COMMAND`, `INTERNET` |
| Cockpit | `MODULE_MANAGEMENT`, `SYSTEM_CONFIG` |
| NLU | `INTERNET` (for cloud models, optional) |

---

## Intent Filters

### VoiceOS
```xml
<intent-filter>
    <action android:name="com.augmentalis.voiceos.ASSISTANT_RESPONSE" />
    <action android:name="com.augmentalis.voiceos.COMMAND_RESULT" />
    <action android:name="com.augmentalis.voiceos.CONFIG_UPDATE" />
</intent-filter>
```

### AVA
```xml
<intent-filter>
    <action android:name="com.augmentalis.ava.PROCESS_COMMAND" />
    <action android:name="com.augmentalis.ava.CONFIG_UPDATE" />
</intent-filter>
```

---

## Broadcast Receivers

### System-Wide Broadcasts

```kotlin
// Register for module status changes
val filter = IntentFilter("com.augmentalis.system.MODULE_STATUS_CHANGED")
registerReceiver(moduleStatusReceiver, filter,
    "com.augmentalis.permission.MODULE_MANAGEMENT", null)
```

---

## Best Practices

1. **Always use explicit intents** for inter-module communication
2. **Validate all extras** before processing
3. **Use permissions** to restrict access
4. **Handle failures gracefully** with fallback mechanisms
5. **Log all IPC calls** for debugging

---

## See Also

- [IPC Methods](IPC-METHODS.md) - Complete IPC documentation
- [API Contracts](API-CONTRACTS.md) - API specifications

---

**Maintained By:** NewAvanues Team
