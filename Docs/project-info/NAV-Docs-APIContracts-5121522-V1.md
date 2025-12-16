# NewAvanues - API Contracts

**Version:** 12.0.0 | **Updated:** 2025-12-15

---

## Overview

This document defines API contracts between NewAvanues modules.

---

## VoiceOS APIs

### Voice Command Processing
```kotlin
interface VoiceCommandProcessor {
    suspend fun processCommand(text: String): CommandResult

    data class CommandResult(
        val intent: String,
        val confidence: Float,
        val entities: Map<String, String>
    )
}
```

**See:** [VoiceOS API Contracts](../Docs/VoiceOS/LivingDocs/LD-VOS-API-Contracts-V1.md)

---

## AVA APIs

### Assistant Core
```kotlin
interface AssistantCore {
    suspend fun processQuery(
        query: String,
        context: ConversationContext
    ): AssistantResponse
}
```

**See:** [AVA API Contracts](../Docs/AVA/LivingDocs/LD-AVA-API-Contracts-V1.md)

---

## WebAvanue APIs

### REST Endpoints
- `POST /api/auth/login` - User authentication
- `GET /api/dashboard/stats` - Dashboard statistics
- `POST /api/modules/config` - Module configuration

**See:** [WebAvanue API Contracts](../Docs/WebAvanue/LivingDocs/LD-WEB-API-Contracts-V1.md)

---

## Cockpit APIs

### Module Management
```kotlin
interface ModuleManager {
    suspend fun getModuleStatus(moduleId: String): ModuleStatus
    suspend fun updateModuleConfig(moduleId: String, config: Map<String, Any>)
}
```

**See:** [Cockpit API Contracts](../Docs/Cockpit/LivingDocs/LD-CPT-API-Contracts-V1.md)

---

## NLU APIs

### Text Processing
```python
def process_text(text: str, context: Dict) -> NLUResult
```

**See:** [NLU API Contracts](../Docs/NLU/LivingDocs/LD-NLU-API-Contracts-V1.md)

---

## Cross-Module Contracts

| From | To | Contract Type | Reference |
|------|----|--------------| ----------|
| VoiceOS | AVA | Intent | [IPC Methods](IPC-METHODS.md) |
| AVA | NLU | API Call | [NLU API](../Docs/NLU/LivingDocs/LD-NLU-API-Contracts-V1.md) |
| WebAvanue | Cockpit | REST | [Cockpit API](../Docs/Cockpit/LivingDocs/LD-CPT-API-Contracts-V1.md) |

---

## Versioning

All APIs follow semantic versioning:
- Major: Breaking changes
- Minor: New features (backwards compatible)
- Patch: Bug fixes

Current Version: **12.0.0**

---

**Maintained By:** NewAvanues Team
