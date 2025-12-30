# LD-VOS-API-Contracts-V1

**Living Document** | VoiceOS API Contracts
**Version:** 1.0 | **Created:** 2025-12-15 | **Status:** Active

---

## API Overview

VoiceOS exposes APIs for voice command processing and accessibility integration.

---

## Intent Contracts

### Voice Command Intent
```kotlin
Intent("com.augmentalis.voiceos.VOICE_COMMAND").apply {
    putExtra("command", commandText)
    putExtra("confidence", confidenceScore)
}
```

### Accessibility Action Intent
```kotlin
Intent("com.augmentalis.voiceos.ACCESSIBILITY_ACTION").apply {
    putExtra("action", actionType)
    putExtra("target", targetElement)
}
```

---

## IPC Methods

See: `/Volumes/M-Drive/Coding/NewAvanues/docs/project-info/IPC-METHODS.md`

---

## Data Contracts

### Voice Command
```kotlin
data class VoiceCommand(
    val text: String,
    val intent: String,
    val entities: Map<String, String>,
    val confidence: Float
)
```

### Accessibility Node
```kotlin
data class AccessibilityNode(
    val id: String,
    val text: String,
    val actionable: Boolean,
    val bounds: Rect
)
```

---

**Last Updated:** 2025-12-15 | **Version:** 12.0.0
