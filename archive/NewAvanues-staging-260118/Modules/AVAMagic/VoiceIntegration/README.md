# Voice Integration Module

**Version:** 1.0.0 (STUB)
**Date:** 2025-11-19
**Author:** Manoj Jhawar (manoj@ideahq.net)
**Status:** Non-functional stub - pending VoiceOS/AVA integration

---

## Overview

Voice integration module for connecting AVAMagic apps with the VoiceOS/AVA NLU and LLM systems. Currently implements stub interfaces that will be wired to actual services when VoiceOS/AVA hybrid app is ready.

### Features

- Voice command registration and routing
- Pattern matching with parameters
- Intent-based routing (via NLU)
- LLM query interface
- Text-to-speech interface
- State management

---

## Current Status: STUB

All methods return mock responses. When VoiceOS/AVA is available:
1. `connect()` will establish IPC connection
2. `startListening()` will activate voice recognition
3. `processCommand()` will send to NLU
4. `askLLM()` will query the AI model
5. `speak()` will use TTS engine

---

## Quick Start

```kotlin
// Create integration
val voice = VoiceIntegration.create(
    config = VoiceConfig(
        appId = "com.myapp",
        voiceOSAppId = "com.avanue.voiceos"
    )
)

// Connect (stub - always succeeds)
voice.connect()

// Register commands
voice.registerCommand("open settings") { params ->
    navigateToSettings()
    VoiceResponse.success("Opening settings")
}

voice.registerCommand("search for {query}") { params ->
    val query = params["query"] as? String ?: ""
    search(query)
    VoiceResponse.success("Searching for $query")
}

// Start listening
voice.startListening()

// Process commands
voice.recognizedText.collect { text ->
    val result = voice.processCommand(text)
    if (result.success) {
        voice.speak(result.response ?: "Done")
    }
}
```

---

## Voice Command Router

Advanced routing with patterns and intents:

```kotlin
val router = voiceCommands {
    // Exact match
    command("open settings") { params ->
        VoiceResponse.success("Opening settings")
    }

    // Pattern with parameter
    command("set volume to {level}") { params ->
        val level = params["level"]?.toString()?.toIntOrNull() ?: 50
        VoiceResponse.success("Volume set to $level")
    }

    // Multiple parameters
    command("remind me to {task} at {time}") { params ->
        val task = params["task"]
        val time = params["time"]
        VoiceResponse.success("Reminder set: $task at $time")
    }

    // Intent-based (from NLU)
    intent("weather.query") { intent ->
        val location = intent.entities["location"] ?: "current"
        VoiceResponse.success("Weather for $location: Sunny, 72°F")
    }

    // Fallback
    fallback { params ->
        val text = params["text"]
        VoiceResponse.success("I didn't understand: $text")
    }
}

// Process
val result = router.process("set volume to 80")
// result.parameters = {level: "80"}
```

---

## API Reference

### VoiceIntegration

| Method | Description |
|--------|-------------|
| `connect()` | Connect to VoiceOS |
| `disconnect()` | Disconnect |
| `startListening()` | Start voice recognition |
| `stopListening()` | Stop recognition |
| `processCommand(text)` | Process command text |
| `askLLM(prompt)` | Query LLM |
| `speak(text)` | Text-to-speech |
| `registerCommand(pattern, handler)` | Register command |
| `registerIntent(name, handler)` | Register intent |

### VoiceCommandRouter

| Method | Description |
|--------|-------------|
| `route(pattern, handler)` | Add pattern route |
| `intentRoute(name, handler)` | Add intent route |
| `fallback(handler)` | Set fallback |
| `process(text)` | Process text |
| `processIntent(intent)` | Process NLU intent |

---

## Configuration

```kotlin
VoiceConfig(
    appId = "com.myapp",           // Your app ID
    voiceOSAppId = "com.avanue.voiceos",  // VoiceOS app ID
    language = "en-US",            // Recognition language
    continuous = false,            // Continuous listening
    interimResults = true,         // Show interim results
    maxAlternatives = 1,           // Number of alternatives
    timeout = 10000                // Recognition timeout
)
```

---

## Voice States

- `DISCONNECTED` - Not connected
- `CONNECTING` - Establishing connection
- `CONNECTED` - Connected, idle
- `LISTENING` - Active recognition
- `PROCESSING` - Processing command
- `SPEAKING` - TTS active
- `ERROR` - Error state

---

## Data Types

### VoiceResponse
```kotlin
VoiceResponse(
    message = "Success message",
    action = "optional.action",
    data = mapOf("key" to "value")
)
```

### CommandResult
```kotlin
CommandResult(
    success = true,
    intent = "detected.intent",
    response = "Response text",
    action = "action.to.perform",
    confidence = 0.95f,
    error = null
)
```

### Intent
```kotlin
Intent(
    name = "weather.query",
    confidence = 0.92f,
    entities = mapOf("location" to "New York"),
    parameters = mapOf("unit" to "fahrenheit")
)
```

### LLMResponse
```kotlin
LLMResponse(
    text = "AI response",
    confidence = 0.88f,
    tokens = 150,
    model = "ava-2"
)
```

---

## Integration Points

### When VoiceOS/AVA is ready:

1. **Voice Recognition**
   ```kotlin
   // Will send IPC:
   // action: "voice.startListening"
   // Response via flow: recognizedText
   ```

2. **NLU Processing**
   ```kotlin
   // Will send IPC:
   // action: "nlu.parse"
   // payload: {text: "..."}
   // Returns: Intent
   ```

3. **LLM Query**
   ```kotlin
   // Will send IPC:
   // action: "llm.generate"
   // payload: {prompt: "...", context: {...}}
   // Returns: LLMResponse
   ```

4. **TTS**
   ```kotlin
   // Will send IPC:
   // action: "tts.speak"
   // payload: {text: "...", voice: "...", rate: ...}
   ```

---

## Testing

### Simulate recognition:
```kotlin
// In tests
voice.simulateRecognition("open settings")
```

### Mock handlers:
```kotlin
voice.registerCommand("test") { params ->
    VoiceResponse.success("Test passed")
}

val result = voice.processCommand("test")
assert(result.success)
```

---

## Files

```
modules/AVAMagic/VoiceIntegration/
├── build.gradle.kts
├── README.md
└── src/
    └── commonMain/
        └── kotlin/
            └── com/augmentalis/avamagic/voice/
                ├── VoiceIntegration.kt
                └── VoiceCommandRouter.kt
```

---

## Next Steps

When VoiceOS/AVA hybrid is ready:

1. Implement IPC connection in `connect()`
2. Wire `startListening()` to recognition service
3. Send `processCommand()` to NLU
4. Connect `askLLM()` to AI model
5. Implement `speak()` TTS
6. Add proper error handling
7. Add reconnection logic

---

## Dependencies

- `kotlinx-serialization-json:1.6.0`
- `kotlinx-coroutines-core:1.7.3`

---

## License

Proprietary - Augmentalis ES

---

**IDEACODE Version:** 8.4
**Created by:** Manoj Jhawar (manoj@ideahq.net)
