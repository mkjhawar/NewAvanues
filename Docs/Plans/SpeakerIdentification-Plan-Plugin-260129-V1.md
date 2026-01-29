# SpeakerIdentification Plugin Plan

**Created:** 2026-01-29
**Author:** Claude Code
**Status:** Draft

---

## Overview

Optional plugin/addon for identifying WHO is speaking. Enables user recognition, conversation participant tracking, and speaker-attributed transcription.

**Architecture:** Plugin that consumes VoiceIsolation output (not baked in)

---

## Multi-Speaker Strategy

### Speaker 1: Device Owner / Glass Wearer
- **Full enrollment** during initial setup
- Stored voice profile (embedding)
- Always identified as "You" or "Owner"

### Speakers 2, 3, ... (Others)
- **No pre-enrollment required**
- Real-time speaker clustering automatically detects voice changes
- Assigns temporary IDs: "Speaker 2", "Speaker 3"
- User can optionally name speakers during/after conversation

### Identification Flow
```
Audio → Extract Embedding → Compare to known embeddings
                                    ↓
                        ┌─────────────────────────────────┐
                        │ Match Owner? → "You"            │
                        │ Match Enrolled? → "Maria"       │
                        │ Match Cluster 1? → "Speaker 2"  │
                        │ New voice? → Create "Speaker 3" │
                        │ Low confidence? → "Ambient"     │
                        └─────────────────────────────────┘
```

---

## Plugin Architecture

```
┌──────────────────────────────────────────────────────┐
│                   VoiceIsolation                     │
│              (standalone, always available)          │
└─────────────────────────┬────────────────────────────┘
                          ↓ Clean audio
┌──────────────────────────────────────────────────────┐
│            SpeakerIdentification (PLUGIN)            │
│                  (optional addon)                    │
│  ┌────────────────────────────────────────────────┐  │
│  │ VoiceEnrollment → EmbeddingModel → Matching   │  │
│  │                         ↓                      │  │
│  │              Real-time Clustering              │  │
│  └────────────────────────────────────────────────┘  │
└─────────────────────────┬────────────────────────────┘
                          ↓ Speaker ID + audio
┌──────────────────────────────────────────────────────┐
│               Speech Recognition                     │
└──────────────────────────────────────────────────────┘
```

---

## Leveraging Existing AI Infrastructure

**Reuse from /Modules/AI/:**

| Existing | Reuse For |
|----------|-----------|
| `IInferenceStrategy` | Create `SpeakerEmbeddingStrategy` |
| `LiteRTInferenceStrategy` | Adapt for ECAPA-TDNN (TFLite) |
| `ModelManager` (expect/actual) | Model download/caching |
| Model download patterns | Embedding model download |

### New: SpeakerEmbeddingStrategy
```kotlin
// Follows existing IInferenceStrategy pattern
class SpeakerEmbeddingStrategy(
    private val context: Context,
    private val modelPath: String
) : IEmbeddingStrategy {

    // Reuse LiteRT interpreter pattern
    private var interpreter: InterpreterApi? = null

    fun extractEmbedding(audioData: FloatArray): FloatArray {
        // Run ECAPA-TDNN inference
        // Return 192-dim or 256-dim embedding
    }
}
```

---

## Module Structure

```
Modules/AI/SpeakerIdentification/       # Under AI folder
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/speakerid/
│   │   ├── SpeakerIdentification.kt    # expect class
│   │   ├── VoiceEnrollment.kt          # Enrollment flow
│   │   ├── SpeakerClustering.kt        # Real-time clustering
│   │   ├── SpeakerProfile.kt           # Profile data class
│   │   └── SpeakerResult.kt            # Result model
│   │
│   ├── androidMain/kotlin/com/augmentalis/speakerid/
│   │   ├── SpeakerIdentification.android.kt
│   │   ├── EcapaTdnnStrategy.kt        # Extends LiteRT pattern
│   │   └── SpeakerModelManager.kt      # Extends ModelManager
│   │
│   ├── iosMain/kotlin/com/augmentalis/speakerid/
│   │   └── SpeakerIdentification.ios.kt  # CoreML implementation
│   │
│   └── desktopMain/kotlin/com/augmentalis/speakerid/
│       └── SpeakerIdentification.desktop.kt  # ONNX implementation
```

---

## Voice Enrollment Flow

### Owner Enrollment (Required Once)
1. Open Settings → Voice Profile → "Set Up Your Voice"
2. App shows: "Say the following phrases clearly..."
3. User speaks 3-5 sentences (5-10 seconds total)
4. Progress indicator shows enrollment quality
5. "Voice profile saved" confirmation

**Enrollment Phrases:**
```
"The quick brown fox jumps over the lazy dog"
"Please call Stella and ask her to bring these things"
"Oak is strong and also gives us shade"
```

### Adding Known People (Optional)
1. Settings → Voice Profiles → "Add Another Person"
2. Select contact or enter name
3. That person speaks enrollment phrases
4. Profile saved for future recognition

### On-the-fly Naming (During Conversation)
```
User: "Name that speaker Maria"
→ Associates last unknown embedding with "Maria"
```

---

## Key Interfaces

### SpeakerIdentification.kt (expect)
```kotlin
expect class SpeakerIdentification {
    // Enrollment
    suspend fun enrollOwner(
        audioSamples: List<ByteArray>,
        onProgress: (Float) -> Unit
    ): EnrollmentResult

    suspend fun enrollPerson(
        name: String,
        audioSamples: List<ByteArray>
    ): EnrollmentResult

    // Identification
    suspend fun identifySpeaker(audioData: ByteArray): SpeakerResult

    // Real-time clustering
    fun startSession()
    fun endSession()
    fun getSessionSpeakers(): List<SessionSpeaker>

    // Profile management
    fun isOwnerEnrolled(): Boolean
    fun nameSpeaker(clusterId: String, name: String)
    fun deleteProfile(speakerId: String)
    fun listProfiles(): List<SpeakerProfile>
}

data class SpeakerResult(
    val speakerId: String,           // "owner", "maria", "speaker_2"
    val speakerType: SpeakerType,    // OWNER, ENROLLED, CLUSTERED, AMBIENT
    val displayName: String,         // "You", "Maria", "Speaker 2"
    val confidence: Float,           // 0.0 - 1.0
    val isNewSpeaker: Boolean        // First time seeing this voice in session
)

enum class SpeakerType {
    OWNER,      // Device owner (enrolled)
    ENROLLED,   // Pre-enrolled known person
    CLUSTERED,  // Detected via real-time clustering (no enrollment)
    AMBIENT     // Background noise / non-speech
}

data class SessionSpeaker(
    val clusterId: String,
    val displayName: String,
    val speakCount: Int,
    val totalDurationMs: Long
)
```

---

## Real-time Speaker Clustering

### Algorithm
```kotlin
class SpeakerClustering {
    private val speakerEmbeddings = mutableMapOf<String, FloatArray>()
    private val similarityThreshold = 0.75f

    fun processAudio(embedding: FloatArray): String {
        // 1. Compare against owner
        if (isOwnerMatch(embedding)) return "owner"

        // 2. Compare against enrolled profiles
        for ((id, profile) in enrolledProfiles) {
            if (cosineSimilarity(embedding, profile) > similarityThreshold) {
                return id
            }
        }

        // 3. Compare against session clusters
        for ((clusterId, clusterEmbedding) in speakerEmbeddings) {
            if (cosineSimilarity(embedding, clusterEmbedding) > similarityThreshold) {
                return clusterId
            }
        }

        // 4. New speaker - create cluster
        val newId = "speaker_${speakerEmbeddings.size + 2}"
        speakerEmbeddings[newId] = embedding
        return newId
    }
}
```

---

## Model Strategy

| Platform | Model | Size | Format |
|----------|-------|------|--------|
| Android | ECAPA-TDNN | ~20MB | TFLite (INT8) |
| iOS | ECAPA-TDNN | ~20MB | CoreML |
| Desktop | ECAPA-TDNN | ~25MB | ONNX |

**Model Loading (reuse existing pattern):**
```kotlin
class SpeakerModelManager(context: Context) {
    // Follows same pattern as existing ModelManager
    suspend fun downloadModelIfNeeded(onProgress: (Float) -> Unit)
    fun getModelPath(): String
    fun isModelAvailable(): Boolean
}
```

---

## Plugin Registration

```kotlin
// In app initialization
if (userHasEnabledSpeakerIdentification) {
    val speakerIdPlugin = SpeakerIdentificationPlugin(context)
    voiceIsolation.registerPlugin(speakerIdPlugin)
}

// Plugin interface
interface VoiceIsolationPlugin {
    fun onAudioProcessed(audio: ByteArray, metadata: AudioMetadata): PluginResult
}
```

---

## Smart Glasses Integration

For glass wearers, additional context:
- **Directional audio** (if multiple mics): "Voice from front" vs "Voice from side"
- **Visual cues** (if camera): Face detection + voice = better speaker tracking
- **Gaze direction**: Who the wearer is looking at

---

## Privacy & Security

- Voice embeddings stored locally only (encrypted)
- Raw audio deleted after embedding extraction
- User can delete any profile anytime
- Session clusters cleared after conversation ends
- GDPR compliant: biometric data on-device only

---

## Files to Create

| File | Purpose |
|------|---------|
| `Modules/AI/SpeakerIdentification/build.gradle.kts` | Build config |
| `src/commonMain/.../SpeakerIdentification.kt` | expect class |
| `src/commonMain/.../VoiceEnrollment.kt` | Enrollment flow |
| `src/commonMain/.../SpeakerClustering.kt` | Real-time clustering |
| `src/androidMain/.../EcapaTdnnStrategy.kt` | TFLite embedding |
| `src/androidMain/.../SpeakerModelManager.kt` | Model management |

---

## Commits

1. `feat(speakerid): create plugin module structure`
2. `feat(speakerid): implement ECAPA-TDNN embedding (reuse LiteRT)`
3. `feat(speakerid): add voice enrollment flow`
4. `feat(speakerid): implement real-time speaker clustering`
5. `feat(speakerid): integrate as VoiceIsolation plugin`
