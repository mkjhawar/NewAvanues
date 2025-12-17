# LD-AVA-API-Contracts-V1

**Living Document** | AVA API Contracts
**Version:** 1.0 | **Created:** 2025-12-15 | **Status:** Active

---

## API Overview

AVA provides cross-platform AI assistant APIs for voice and text interaction.

---

## Core APIs

### Process Query
```kotlin
suspend fun processQuery(
    query: String,
    context: ConversationContext
): AssistantResponse
```

### Get Response
```kotlin
data class AssistantResponse(
    val text: String,
    val intent: String,
    val confidence: Float,
    val actions: List<Action>
)
```

---

## Platform-Specific APIs

### Android
```kotlin
class AVAAndroid(context: Context) : AssistantCore() {
    fun initialize()
    suspend fun processVoiceInput(audio: ByteArray): AssistantResponse
}
```

### Web
```typescript
interface AVAWeb {
    initialize(): Promise<void>;
    processTextInput(text: string): Promise<AssistantResponse>;
}
```

---

## IPC with NLU

See: `/Volumes/M-Drive/Coding/NewAvanues/docs/project-info/IPC-METHODS.md`

---

**Last Updated:** 2025-12-15 | **Version:** 12.0.0
