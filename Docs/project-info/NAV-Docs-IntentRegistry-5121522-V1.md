# NewAvanues - Intent Registry

**Version:** 12.0.0
**Updated:** 2025-12-15

---

## Overview

This document catalogs all Android Intents used across the NewAvanues monorepo for inter-module communication.

## Intent Naming Convention

**Format:** `com.augmentalis.{module}.{ACTION_NAME}`

**Rules:**
- All lowercase module names
- UPPERCASE action names
- Descriptive action naming

## Intent Categories

### System-Wide Intents

#### Voice Command Intent
```xml
<intent-filter>
    <action android:name="com.augmentalis.VOICE_COMMAND" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

**Purpose:** Broadcast voice commands to all listening modules

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `command` | String | Yes | Voice command text |
| `confidence` | Float | Yes | Recognition confidence (0.0-1.0) |
| `timestamp` | Long | Yes | Command timestamp |
| `sessionId` | String | No | Voice session identifier |

**Senders:** VoiceOS
**Receivers:** AVA, WebAvanue, Cockpit

---

### VoiceOS Intents

#### VOS_PERFORM_ACTION
```kotlin
Action: "com.augmentalis.voiceos.PERFORM_ACTION"
```

**Purpose:** Request accessibility action

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `nodeId` | String | Yes | Target node identifier |
| `action` | String | Yes | Action type (CLICK, LONG_CLICK, SCROLL, etc.) |
| `parameters` | Bundle | No | Action parameters |

**Senders:** AVA, WebAvanue
**Receivers:** VoiceOS Accessibility Service

---

#### VOS_GET_CONTEXT
```kotlin
Action: "com.augmentalis.voiceos.GET_CONTEXT"
```

**Purpose:** Request current screen context

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `includeElements` | Boolean | No | Include UI elements (default: true) |
| `maxDepth` | Int | No | Element tree depth (default: 5) |

**Response:** Intent with screen context JSON

**Senders:** AVA, Cockpit
**Receivers:** VoiceOS Context Service

---

#### VOS_REGISTER_COMMAND
```kotlin
Action: "com.augmentalis.voiceos.REGISTER_COMMAND"
```

**Purpose:** Register custom voice command

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `commandPattern` | String | Yes | Command pattern/regex |
| `handlerClass` | String | Yes | Handler class name |
| `priority` | Int | No | Command priority (0-100) |

**Senders:** AVA, WebAvanue, User Apps
**Receivers:** VoiceOS Command Registry

---

### AVA Intents

#### AVA_EXECUTE_COMMAND
```kotlin
Action: "com.augmentalis.ava.EXECUTE_COMMAND"
```

**Purpose:** Execute AI-processed command

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `command` | String | Yes | Command to execute |
| `context` | String | Yes | JSON context data |
| `conversationId` | String | No | Conversation thread ID |

**Senders:** VoiceOS, WebAvanue, User Apps
**Receivers:** AVA Service

---

#### AVA_QUERY_AI
```kotlin
Action: "com.augmentalis.ava.QUERY_AI"
```

**Purpose:** Query AI assistant

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `query` | String | Yes | User query |
| `conversationId` | String | No | Conversation ID |
| `resultReceiver` | ResultReceiver | Yes | Response callback |

**Senders:** Any module
**Receivers:** AVA AI Service

---

### WebAvanue Intents

#### WEB_NAVIGATE
```kotlin
Action: "com.augmentalis.web.NAVIGATE"
```

**Purpose:** Navigate to URL

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `url` | String | Yes | Target URL |
| `newTab` | Boolean | No | Open in new tab (default: false) |
| `privateMode` | Boolean | No | Use private mode (default: false) |

**Senders:** AVA, VoiceOS
**Receivers:** WebAvanue Browser

---

#### WEB_EXTRACT_CONTENT
```kotlin
Action: "com.augmentalis.web.EXTRACT_CONTENT"
```

**Purpose:** Extract page content

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `format` | String | No | Output format (TEXT, HTML, JSON) |
| `includeImages` | Boolean | No | Include images (default: false) |

**Response:** Intent with extracted content

**Senders:** AVA
**Receivers:** WebAvanue Content Extractor

---

#### WEB_PRIVACY_SETTINGS
```kotlin
Action: "com.augmentalis.web.PRIVACY_SETTINGS"
```

**Purpose:** Configure privacy settings

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `blockAds` | Boolean | No | Enable ad blocking |
| `blockTrackers` | Boolean | No | Enable tracker blocking |
| `clearCookies` | Boolean | No | Clear all cookies |

**Senders:** Cockpit, AVA
**Receivers:** WebAvanue Privacy Manager

---

### Cockpit Intents

#### COCKPIT_MODULE_STATUS
```kotlin
Action: "com.augmentalis.cockpit.MODULE_STATUS"
```

**Purpose:** Report module status

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `moduleId` | String | Yes | Module identifier |
| `status` | String | Yes | Status (RUNNING, STOPPED, ERROR) |
| `metrics` | Bundle | No | Performance metrics |

**Senders:** All modules
**Receivers:** Cockpit Monitor Service

---

#### COCKPIT_CONFIG_UPDATE
```kotlin
Action: "com.augmentalis.cockpit.CONFIG_UPDATE"
```

**Purpose:** Notify configuration change

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `configKey` | String | Yes | Configuration key |
| `configValue` | String | Yes | New value |
| `scope` | String | No | Config scope (GLOBAL, MODULE) |

**Senders:** Cockpit Config Service
**Receivers:** All modules

---

### NLU Intents

#### NLU_PROCESS_TEXT
```kotlin
Action: "com.augmentalis.nlu.PROCESS_TEXT"
```

**Purpose:** Process text for NLU

**Extras:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `text` | String | Yes | Input text |
| `context` | Bundle | No | Context metadata |
| `resultReceiver` | ResultReceiver | Yes | Response callback |

**Senders:** VoiceOS, AVA
**Receivers:** NLU Service

---

## Intent Registration Requirements

### Manifest Declaration

**All services receiving intents MUST declare:**
```xml
<service
    android:name=".YourService"
    android:exported="true"
    android:permission="com.augmentalis.permission.EXECUTE_COMMAND">
    <intent-filter>
        <action android:name="com.augmentalis.your.ACTION" />
    </intent-filter>
</service>
```

### Quality Gate

**Intent Registration:** 100% coverage required

**All intents must:**
- Be documented in this registry
- Have unit tests
- Specify required permissions
- Define all extras with types
- Implement timeout handling

## Testing Requirements

**Intent Tests:**
```kotlin
@Test
fun testVoiceCommandIntent() {
    val intent = Intent("com.augmentalis.VOICE_COMMAND").apply {
        putExtra("command", "test command")
        putExtra("confidence", 0.95f)
        putExtra("timestamp", System.currentTimeMillis())
    }

    assertNotNull(intent.getStringExtra("command"))
    assertEquals(0.95f, intent.getFloatExtra("confidence", 0f))
}
```

## Intent Flow Diagrams

### Voice Command Flow
```
User Voice
    │
    ▼
VoiceOS (Speech Recognition)
    │
    │ Intent: VOICE_COMMAND
    ├─────────┬──────────┐
    ▼         ▼          ▼
  AVA    WebAvanue   Cockpit
    │
    │ Intent: AVA_EXECUTE_COMMAND
    ▼
VoiceOS (Perform Action)
    │
    ▼
System Action
```

### Web Navigation Flow
```
AVA Command
    │
    │ Intent: WEB_NAVIGATE
    ▼
WebAvanue
    │
    │ Intent: WEB_EXTRACT_CONTENT
    ▼
AVA (Process Content)
    │
    │ Intent: AVA_QUERY_AI
    ▼
NLU (Understand Content)
```

## Security Model

### Permission Protection

**All custom permissions:**
```xml
<permission
    android:name="com.augmentalis.permission.VOICE_COMMAND"
    android:protectionLevel="signature" />

<permission
    android:name="com.augmentalis.permission.EXECUTE_COMMAND"
    android:protectionLevel="signature" />

<permission
    android:name="com.augmentalis.permission.MODULE_STATUS"
    android:protectionLevel="signature" />
```

**Protection Level:** `signature` (only apps signed with same key can use)

## Deprecated Intents

*None currently*

## Future Intents (Planned)

| Intent | Purpose | Target Version |
|--------|---------|----------------|
| `com.augmentalis.gesture.RECOGNIZE` | Gesture recognition | 12.1.0 |
| `com.augmentalis.multimodal.PROCESS` | Multi-modal input | 12.2.0 |

## References

- **IPC Methods:** NAV-Docs-IPCMethods-5121522-V1.md
- **API Contracts:** NAV-Docs-APIContracts-5121522-V1.md
- **Architecture:** NAV-Docs-Architecture-5121522-V1.md

---

All new intents MUST be added to this registry before implementation.
