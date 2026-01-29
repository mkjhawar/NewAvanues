# SpeechTranslation KMP Module Plan

**Created:** 2026-01-29
**Author:** Claude Code
**Status:** Draft

---

## Overview

Real-time translation with speaker attribution. Combines speaker identification with transcription and translation for multi-language conversations.

**Requires:** VoiceIsolation, optionally SpeakerIdentification plugin

**Reference:** [MentraOS LiveCaptions](https://github.com/Mentra-Community/LiveCaptionsOnSmartGlasses) - Similar architecture for smart glasses

---

## Architecture

```
Audio → [VoiceIsolation] → [SpeakerID?] → [ASR] → [Translation] → [TTS?]
              ↓                  ↓           ↓           ↓
         Clean audio        "Owner"    "Hello"    "Hola" (Spanish)
                           "Speaker 2"  "Gracias"  "Thank you"
```

### Data Flow for Translation Scenario
```
┌─────────────────────────────────────────────────────────────────┐
│ You (English speaker with glasses)                              │
│ ────────────────────────────────────────────────────────────────│
│ 1. You speak: "Where is the pharmacy?"                          │
│    → VoiceIsolation cleans audio                                │
│    → SpeakerID confirms: "owner"                                │
│    → No translation needed (your language)                      │
│    → Optional: TTS speaks Spanish to them                       │
│                                                                  │
│ 2. Speaker 2 speaks: "La farmacia está en la esquina"          │
│    → VoiceIsolation cleans audio                                │
│    → SpeakerID detects: "speaker_2" (new voice)                │
│    → ASR transcribes (Spanish detected)                         │
│    → Translation: "The pharmacy is on the corner"               │
│    → Display on glasses + optional TTS                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
Modules/SpeechTranslation/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/speechtranslation/
│   │   ├── SpeechTranslationCoordinator.kt   # Main orchestrator
│   │   ├── TranslationPipeline.kt            # ASR → Translate flow
│   │   ├── TranslationModels.kt              # Data classes
│   │   ├── AttributedTranscript.kt           # Speaker + translation
│   │   └── LanguageDetector.kt               # Auto-detect language
│   │
│   ├── androidMain/kotlin/com/augmentalis/speechtranslation/
│   │   ├── SpeechTranslationCoordinator.android.kt
│   │   ├── MLKitTranslator.kt                # Google ML Kit
│   │   └── WhisperTranslator.kt              # Whisper translation mode
│   │
│   ├── iosMain/kotlin/com/augmentalis/speechtranslation/
│   │   └── AppleTranslationCoordinator.kt    # Apple Translation Framework
│   │
│   └── desktopMain/kotlin/com/augmentalis/speechtranslation/
│       └── DesktopTranslationCoordinator.kt  # LibreTranslate/NLLB
```

---

## Key Interfaces

```kotlin
expect class SpeechTranslationCoordinator {
    suspend fun startRealtimeTranslation(
        ownerLanguage: String,             // User's language (e.g., "en")
        targetLanguages: List<String>      // Languages to translate TO
    ): Flow<TranslationEvent>

    suspend fun stop()

    fun setTranslationMode(mode: TranslationMode)
}

enum class TranslationMode {
    TRANSLATE_OTHERS,    // Only translate non-owner speech
    TRANSLATE_ALL,       // Translate everything (for transcript)
    BIDIRECTIONAL        // Translate both directions with TTS output
}

sealed class TranslationEvent {
    data class Transcription(
        val text: String,
        val speakerId: String?,
        val speakerName: String?,
        val detectedLanguage: String,
        val isFinal: Boolean
    ) : TranslationEvent()

    data class Translation(
        val originalText: String,
        val translatedText: String,
        val sourceLanguage: String,
        val targetLanguage: String,
        val speakerId: String?,
        val speakerName: String?
    ) : TranslationEvent()

    data class SpeakerChanged(
        val speakerId: String,
        val speakerName: String?
    ) : TranslationEvent()
}

data class AttributedTranscript(
    val speakerId: String,
    val speakerName: String?,
    val speakerType: SpeakerType,
    val originalText: String,
    val originalLanguage: String,
    val translations: Map<String, String>,  // languageCode → text
    val timestamp: Long
)
```

---

## Translation Technology

| Platform | Primary | Languages | Offline |
|----------|---------|-----------|---------|
| Android | Google ML Kit | 59+ | Yes (30MB/lang) |
| iOS | Apple Translation | 16+ | Yes (iOS 17+) |
| Desktop | LibreTranslate/NLLB | 200+ | Yes |

### Whisper Translation Mode (Alternative)
Whisper can transcribe AND translate in one pass:
```kotlin
// Whisper with translation
val result = whisper.transcribe(audio, task = "translate", targetLang = "en")
// Returns English translation directly from any source language
```

---

## UI Display

### Smart Glasses View
```
┌─────────────────────────────────────┐
│ 👤 You                              │
│ "Where is the pharmacy?"            │
├─────────────────────────────────────┤
│ 👥 Speaker 2                        │
│ "La farmacia está en la esquina"    │
│ 🌐 "The pharmacy is on the corner"  │
├─────────────────────────────────────┤
│ 👥 Speaker 3                        │
│ "Está muy cerca de aquí"            │
│ 🌐 "It's very close to here"        │
└─────────────────────────────────────┘
```

### Phone Companion View
```
┌─────────────────────────────────────┐
│ Translation Mode: Spanish ↔ English │
│ ─────────────────────────────────── │
│                                     │
│ 14:32 👤 You                        │
│ "Where is the pharmacy?"            │
│ 🔊 "¿Dónde está la farmacia?"       │
│                                     │
│ 14:32 👥 Speaker 2                  │
│ "La farmacia está en la esquina"    │
│ → "The pharmacy is on the corner"   │
│                                     │
│ 14:33 👥 Speaker 3                  │
│ "Está muy cerca de aquí"            │
│ → "It's very close to here"         │
│                                     │
│ [Name Speaker 2] [Name Speaker 3]   │
└─────────────────────────────────────┘
```

---

## Voice Commands

```
"translate to Spanish"
"start translation mode"
"translate conversation"
"show transcript"
"name that speaker Maria"
"stop translation"
"speak my response"        // TTS output for bidirectional
```

---

## MentraOS Pattern Reference

Based on [MentraOS LiveCaptions](https://github.com/Mentra-Community/LiveCaptionsOnSmartGlasses):

```typescript
// MentraOS subscription pattern (TypeScript)
session.subscribe("transcription")
session.events.onTranscription((data) => {
    console.log("Caption:", data.text)
    session.layouts.updateText({ text: data.text })
})
```

**Our equivalent (Kotlin):**
```kotlin
// Subscribe to translation events
translationCoordinator.startRealtimeTranslation("en", listOf("es"))
    .collect { event ->
        when (event) {
            is TranslationEvent.Translation -> {
                displayOnGlasses(event.translatedText)
            }
            is TranslationEvent.SpeakerChanged -> {
                showSpeakerIndicator(event.speakerName)
            }
        }
    }
```

---

## Bidirectional Translation (TTS Output)

For conversations where you need to speak back:

```kotlin
data class BidirectionalConfig(
    val ownerLanguage: String,           // "en"
    val otherLanguage: String,           // "es"
    val speakTranslationsToOthers: Boolean = true,
    val speakTranslationsToOwner: Boolean = false  // Usually read on glasses
)

// When owner speaks
fun onOwnerSpeech(text: String) {
    val translated = translate(text, "en", "es")
    tts.speak(translated, language = "es")  // Speak Spanish to them
}

// When others speak
fun onOtherSpeech(text: String, speakerId: String) {
    val translated = translate(text, "es", "en")
    displayOnGlasses(translated)  // Show English to owner
}
```

---

## Files to Create

| File | Purpose |
|------|---------|
| `Modules/SpeechTranslation/build.gradle.kts` | Build config |
| `src/commonMain/.../SpeechTranslationCoordinator.kt` | Orchestrator |
| `src/commonMain/.../TranslationModels.kt` | Data classes |
| `src/commonMain/.../LanguageDetector.kt` | Auto-detect language |
| `src/androidMain/.../MLKitTranslator.kt` | ML Kit wrapper |
| `src/iosMain/.../AppleTranslationCoordinator.kt` | Apple Translation |

---

## Dependencies

```kotlin
// commonMain
implementation(project(":Modules:VoiceIsolation"))
implementation(project(":Modules:SpeechRecognition"))

// Optional speaker identification
compileOnly(project(":Modules:AI:SpeakerIdentification"))

// androidMain
implementation("com.google.mlkit:translate:17.0.1")

// iosMain - uses Apple Translation framework (iOS 17+)
```

---

## Commits

1. `feat(speechtranslation): create module structure`
2. `feat(speechtranslation): implement ML Kit integration`
3. `feat(speechtranslation): add speaker-attributed transcription`
4. `feat(speechtranslation): add bidirectional TTS support`
5. `feat(speechtranslation): add conversation UI for glasses and phone`
