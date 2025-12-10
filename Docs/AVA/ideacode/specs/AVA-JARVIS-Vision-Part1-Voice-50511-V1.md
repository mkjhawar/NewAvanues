# AVA JARVIS Vision - Part 1: Voice-First Interaction

**Date:** 2025-11-05
**Status:** Planning Phase
**Priority:** CRITICAL

---

## 1. Always-Listening Hotword Detection

### Overview
Low-power wake word detection for instant activation without manual interaction.

### Implementation
**Library:** Picovoice Porcupine
- Free tier: 1 custom wake word
- Wake words: "Hey AVA" or "Computer" (Star Trek style)
- Offline: On-device processing, zero cloud
- Battery: <1% battery per day
- Latency: <100ms to activate

### Technical Specs
```kotlin
class HotwordDetector(
    private val porcupineManager: PorcupineManager,
    private val wakeWord: String = "Hey AVA"
) {
    fun startListening() {
        porcupineManager.start()
    }

    fun onWakeWordDetected(callback: () -> Unit) {
        // Trigger main AVA interface
        // Start microphone capture
        // Warm up LLM if needed
    }
}
```

### Integration Points
- Background service (persistent)
- Low-power audio processing
- System-wide availability
- Privacy: No audio sent to cloud

### Success Metrics
- Detection accuracy: >95%
- False positives: <1 per day
- Latency: <100ms
- Battery impact: <1%

**Priority:** 🔴 CRITICAL

---

## 2. Natural Speech Recognition

### Overview
Offline speech-to-text using OpenAI Whisper model.

### Implementation
**Model:** Whisper Small (244MB) or Tiny (75MB)
**Library:** whisper.cpp Android bindings

### Performance Targets
- Latency: 200-500ms for 5-second audio clip
- Accuracy: 95%+ (Whisper Small)
- Languages: 99 languages supported
- Real-time processing on mid-range devices

### Technical Specs
```kotlin
class AVASpeechRecognizer(
    private val whisper: WhisperModel,
    private val modelSize: ModelSize = ModelSize.SMALL
) {
    suspend fun transcribe(audioBuffer: ByteArray): TranscriptionResult {
        return withContext(Dispatchers.Default) {
            whisper.transcribe(
                audio = audioBuffer,
                language = "auto", // Auto-detect
                task = Task.TRANSCRIBE
            )
        }
    }

    fun transcribeStream(audioFlow: Flow<ByteArray>): Flow<String> {
        // Streaming transcription for real-time display
    }
}
```

### Model Selection
| Model | Size | Speed | Accuracy | Recommended |
|-------|------|-------|----------|-------------|
| Tiny | 75MB | Fast (100ms) | 85% | Budget devices |
| Small | 244MB | Medium (300ms) | 95% | **Recommended** |
| Medium | 769MB | Slow (1000ms) | 97% | Flagship only |

### Features
- Streaming transcription (show text as user speaks)
- Punctuation and capitalization
- Multi-language support
- Noise robustness
- Speaker diarization (future)

**Priority:** 🔴 CRITICAL

---

## 3. Natural Voice Response (TTS)

### Overview
High-quality neural text-to-speech for natural-sounding responses.

### Implementation
**Model:** Piper TTS
**Voice Options:**
- Male/female voices
- Multiple accents (US, UK, etc.)
- Professional tone (JARVIS-like)
- Size: 20-50MB per voice

### Performance Targets
- First word latency: 100-200ms
- Streaming: Start speaking while generating text
- Quality: Near human-like (MOS >4.0)
- Speed: Adjustable (0.8x - 1.5x)

### Technical Specs
```kotlin
class AVAVoice(
    private val piperTTS: PiperTTS,
    private val voice: VoiceModel = VoiceModel.PROFESSIONAL_MALE
) {
    suspend fun speak(text: String, interrupt: Boolean = false) {
        if (interrupt) stopCurrentSpeech()

        piperTTS.synthesize(text).collect { audioChunk ->
            audioPlayer.play(audioChunk)
        }
    }

    fun speakStream(textFlow: Flow<String>) {
        // Speak LLM output as it's generated
        // Start speaking first sentence while rest generates
    }
}
```

### Voice Personalities
```kotlin
enum class VoicePersonality {
    JARVIS,      // Professional, measured, slight British accent
    TECHNICAL,   // Precise, neutral, clear
    FRIENDLY,    // Warm, casual, conversational
    CUSTOM       // User-configurable
}
```

### Features
- Streaming TTS (low latency)
- SSML support (emphasis, pauses)
- Background playback
- Interrupt capability
- Speed/pitch adjustment
- Offline processing

**Priority:** 🔴 CRITICAL

---

## 4. Voice Pipeline Integration

### End-to-End Flow
```
User says "Hey AVA"
  ↓ <100ms
Hotword detected
  ↓ <50ms
Microphone activated
  ↓ User speaks (3-5 seconds)
Audio buffered
  ↓ <300ms
Whisper transcription
  ↓ <50ms
Text → RAG search + LLM
  ↓ <500ms (with streaming)
First words generated
  ↓ <200ms
Piper TTS starts
  ↓ Immediate
User hears response
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL: <1.2 seconds
```

### Technical Implementation
```kotlin
class VoicePipeline(
    private val hotword: HotwordDetector,
    private val stt: AVASpeechRecognizer,
    private val rag: RAGRepository,
    private val llm: MLCEngine,
    private val tts: AVAVoice
) {
    suspend fun handleVoiceInteraction() {
        // 1. Wait for wake word
        hotword.awaitWakeWord()

        // 2. Capture audio
        val audio = microphoneCapture()

        // 3. Transcribe
        val text = stt.transcribe(audio)

        // 4. Search + Generate
        val context = rag.search(SearchQuery(text))
        val responseFlow = llm.generateStream(
            prompt = assemblePrompt(text, context)
        )

        // 5. Speak (streaming)
        tts.speakStream(responseFlow)
    }
}
```

### Performance Optimization
- Pre-load Whisper model on boot
- Keep Piper TTS warm
- LLM warmup on wake word detection
- Parallel processing where possible
- Aggressive caching

### Error Handling
```kotlin
sealed class VoiceError {
    object NoSpeechDetected : VoiceError()
    object TranscriptionFailed : VoiceError()
    object AmbiguousInput : VoiceError()
    object NoRelevantContext : VoiceError()
}

fun handleError(error: VoiceError) {
    when (error) {
        NoSpeechDetected -> tts.speak("I didn't hear anything. Try again?")
        TranscriptionFailed -> tts.speak("Sorry, I didn't catch that.")
        AmbiguousInput -> tts.speak("Could you be more specific?")
        NoRelevantContext -> tts.speak("I don't have information on that.")
    }
}
```

---

## 5. Voice UX Best Practices

### Feedback Mechanisms
```kotlin
class VoiceUI {
    fun showListeningIndicator() {
        // Pulsing circular waveform
        // Real-time audio visualization
    }

    fun showTranscriptionLive(text: String) {
        // Show what user is saying in real-time
        // Builds confidence in accuracy
    }

    fun showProcessingIndicator() {
        // Spinning/pulsing while thinking
        // "Searching documents..."
    }

    fun showSpeakingIndicator() {
        // Animated waveform matching speech
        // Visual confirmation of response
    }
}
```

### Interruption Handling
```kotlin
fun handleInterruption() {
    // User can interrupt AVA mid-response
    // "Stop" or new wake word stops current speech
    // Immediate responsiveness

    hotword.onWakeWordDuringPlayback {
        tts.stop()
        startNewInteraction()
    }
}
```

### Privacy Controls
```kotlin
data class VoicePrivacy(
    val alwaysListening: Boolean = true,  // Hotword detection
    val saveAudioHistory: Boolean = false, // Don't store audio
    val saveTranscripts: Boolean = true,   // Store text only
    val muteButton: Boolean = true         // Hardware mute support
)
```

---

## 6. Implementation Roadmap

### Week 1: Hotword Detection
- [ ] Integrate Picovoice Porcupine
- [ ] Create background service
- [ ] Test wake word accuracy
- [ ] Optimize battery usage
- [ ] Add settings UI

### Week 2: Speech Recognition
- [ ] Integrate whisper.cpp
- [ ] Download/bundle Whisper Small model
- [ ] Implement streaming transcription
- [ ] Test accuracy across accents
- [ ] Optimize latency

### Week 3: Text-to-Speech
- [ ] Integrate Piper TTS
- [ ] Select/customize voice
- [ ] Implement streaming playback
- [ ] Test JARVIS-like personality
- [ ] Audio quality tuning

### Week 4: Integration
- [ ] Connect all components
- [ ] End-to-end testing
- [ ] Latency optimization
- [ ] Error handling
- [ ] User testing

---

## 7. Dependencies

### Required Libraries
```kotlin
dependencies {
    // Hotword detection
    implementation("ai.picovoice:porcupine-android:3.0.0")

    // Speech recognition
    implementation("com.whispercpp:whisper-android:1.5.0")

    // Text-to-speech
    implementation("com.github.rhasspy:piper-android:1.2.0")

    // Audio processing
    implementation("androidx.media3:media3-exoplayer:1.1.1")
}
```

### Model Assets
- Porcupine wake word model: 2MB
- Whisper Small: 244MB
- Piper TTS voice: 20-50MB
- **Total: ~280MB**

### Permissions
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

---

## 8. Success Criteria

### Phase 1 Complete When:
- [x] Wake word detection works >95% accuracy
- [x] Speech transcription <500ms latency
- [x] TTS sounds natural and JARVIS-like
- [x] End-to-end interaction <2 seconds
- [x] Battery impact <2% per day
- [x] Works 100% offline

---

**Next:** Part 2 - Contextual Intelligence
