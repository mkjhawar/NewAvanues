# VoiceIsolation KMP Module Plan

**Created:** 2026-01-29
**Author:** Claude Code
**Status:** Draft

---

## Overview

Create a KMP module for audio preprocessing before speech recognition. Provides noise suppression, echo cancellation, and automatic gain control with user-configurable settings.

**Key Features:**
- Toggle ON/OFF (default: ON) for A/B testing
- User settings UI with sliders for levels
- Voice commands for adjustment
- Platform-specific implementations

---

## Module Structure

```
Modules/VoiceIsolation/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceisolation/
│   │   ├── VoiceIsolation.kt              # expect class
│   │   ├── VoiceIsolationConfig.kt        # Configuration
│   │   ├── VoiceIsolationState.kt         # State & availability
│   │   └── ProcessingMode.kt              # Enum
│   │
│   ├── androidMain/kotlin/com/augmentalis/voiceisolation/
│   │   ├── VoiceIsolation.android.kt      # actual (NoiseSuppressor, etc.)
│   │   └── VoiceIsolationSettings.kt      # DataStore integration
│   │
│   ├── iosMain/kotlin/com/augmentalis/voiceisolation/
│   │   └── VoiceIsolation.ios.kt          # actual (AVAudioEngine stub)
│   │
│   └── desktopMain/kotlin/com/augmentalis/voiceisolation/
│       └── VoiceIsolation.desktop.kt      # actual (WebRTC stub)
```

---

## Key Interfaces

### VoiceIsolationConfig.kt (commonMain)
```kotlin
data class VoiceIsolationConfig(
    // Master toggle (default ON for testing)
    val enabled: Boolean = true,

    // Feature toggles
    val noiseSuppression: Boolean = true,
    val echoCancellation: Boolean = false,
    val automaticGainControl: Boolean = true,

    // Adjustable levels (0.0 to 1.0)
    val noiseSuppressionLevel: Float = 0.7f,
    val gainLevel: Float = 0.5f,

    // Processing mode
    val mode: ProcessingMode = ProcessingMode.BALANCED
)

enum class ProcessingMode {
    LOW_LATENCY,    // Minimal processing, fastest
    BALANCED,       // Default
    HIGH_QUALITY    // Maximum processing
}
```

### VoiceIsolation.kt (expect/actual)
```kotlin
expect class VoiceIsolation {
    fun initialize(audioSessionId: Int, config: VoiceIsolationConfig): Boolean
    fun process(audioData: ByteArray): ByteArray
    fun updateConfig(config: VoiceIsolationConfig)
    fun isEnabled(): Boolean
    fun toggle(enabled: Boolean)  // For A/B testing
    fun getAvailability(): Map<String, Boolean>
    fun release()
}
```

---

## User Settings UI

### Settings Screen
- **Master Toggle**: "Voice Isolation" ON/OFF (default: ON)
- **Noise Suppression**: Toggle + slider (0-100%)
- **Echo Cancellation**: Toggle
- **Gain Control**: Toggle + slider (0-100%)
- **Mode**: Low Latency / Balanced / High Quality
- **Test Button**: Compare with/without isolation

### Voice Commands
```
"turn on voice isolation"
"turn off voice isolation"
"increase noise suppression"
"decrease gain"
"reset voice settings"
```

---

## Speech Engine Integration

### Vivoka (Pipeline middleware)
```kotlin
val voiceIsolation = VoiceIsolationFactory.create(context)
if (voiceIsolation.isEnabled()) {
    pipeline.pushBackConsumer(VoiceIsolationAdapter(voiceIsolation))
}
pipeline.pushBackConsumer(recognizer)
```

### Whisper (processAudioStream hook)
```kotlin
val processedAudio = if (voiceIsolation.isEnabled()) {
    voiceIsolation.process(audioData)
} else {
    audioData
}
```

### Google (Flow transformation)
```kotlin
val processedFlow = audioFlow.map { audioBytes ->
    if (voiceIsolation.isEnabled()) voiceIsolation.process(audioBytes) else audioBytes
}
```

### Vosk (AudioRecord wrapper)
```kotlin
class VoskAudioPreprocessor(private val voiceIsolation: VoiceIsolation) {
    fun processAudio(audio: ByteArray): ByteArray {
        return if (voiceIsolation.isEnabled()) voiceIsolation.process(audio) else audio
    }
}
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `Modules/VoiceIsolation/build.gradle.kts` | Build config |
| `src/commonMain/.../VoiceIsolation.kt` | expect class |
| `src/commonMain/.../VoiceIsolationConfig.kt` | Configuration |
| `src/androidMain/.../VoiceIsolation.android.kt` | Android actual |
| `src/iosMain/.../VoiceIsolation.ios.kt` | iOS stub |
| `src/desktopMain/.../VoiceIsolation.desktop.kt` | Desktop stub |

## Files to Modify

| File | Change |
|------|--------|
| `settings.gradle.kts` | Add `:Modules:VoiceIsolation` |
| `Modules/SpeechRecognition/build.gradle.kts` | Add dependency |
| `.../vivoka/VivokaEngine.kt` | Add VoiceIsolation integration |
| `.../whisper/WhisperProcessor.kt` | Add processing hook |

---

## Verification

```bash
./gradlew :Modules:VoiceIsolation:compileDebugKotlinAndroid
./gradlew :Modules:SpeechRecognition:compileDebugKotlinAndroid
```

**Manual Testing:**
1. Toggle voice isolation OFF → speak in noisy environment → note recognition quality
2. Toggle voice isolation ON → speak in same environment → compare quality
3. Adjust sliders → verify effect on audio

---

## Commits

1. `feat(voiceisolation): create KMP module structure`
2. `feat(voiceisolation): implement Android voice isolation with toggle`
3. `feat(voiceisolation): add user settings UI`
4. `feat(speechrecognition): integrate VoiceIsolation with engines`
