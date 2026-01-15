# VoiceOSCoreNG Fix Tasks
**Plan Reference:** VoiceOSCoreNG-Fix-Plan-260116-V1.md
**Generated:** 2026-01-16

---

## Terminal Assignment Overview

```
┌─────────────────────────────────────────────────────────────────┐
│ Terminal 1: Android Core     │ Terminal 2: iOS Platform        │
│ Tasks: 1.1, 1.2, 1.3, 1.4   │ Tasks: 2.1, 2.2, 2.3, 2.4, 2.5 │
├─────────────────────────────────────────────────────────────────┤
│ Terminal 3: Desktop Platform │ Terminal 4: Speech Engines      │
│ Tasks: 3.1, 3.2, 3.3        │ Tasks: 4.1, 4.2, 4.3            │
├─────────────────────────────────────────────────────────────────┤
│ Terminal 5: NLU Enhancements │ Terminal 6: Testing             │
│ Tasks: 5.1, 5.2             │ Tasks: 6.1, 6.2, 6.3, 6.4       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Tasks

### TERMINAL 1: Android Core Fixes

```yaml
- id: 1.1
  name: Fix StaticCommandPersistence.refresh()
  priority: P1
  effort: 1h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/persistence/StaticCommandPersistence.kt
  line: 95-100
  description: Complete deletion logic in refresh() method
  verification: ./gradlew :Modules:VoiceOSCoreNG:testDebugUnitTest --tests "*StaticCommandPersistenceTest*"

- id: 1.2
  name: Implement LearnAppCore Integration
  priority: P1
  effort: 4h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/exploration/ElementRegistrar.kt
  line: 86
  description: Replace TODO with voice command generation from discovered UI elements
  depends_on: []

- id: 1.3
  name: Verify Model File Availability
  priority: P1
  effort: 30m
  status: pending
  description: Check NLU model at models/nlu/malbert-intent-v1.onnx, LLM at /sdcard/ava-ai-models/llm/
  verification: adb shell "ls -la /sdcard/ava-ai-models/llm/"

- id: 1.4
  name: Add packageName Validation
  priority: P2
  effort: 1h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/QuantizedCommand.kt
  description: Enforce packageName in metadata at creation time, not just persistence
```

### TERMINAL 2: iOS Platform Implementation

```yaml
- id: 2.1
  name: Implement iOS BertTokenizer
  priority: P0
  effort: 8h
  status: pending
  file: /Modules/Shared/NLU/src/iosMain/kotlin/com/augmentalis/nlu/BertTokenizer.kt
  description: WordPiece tokenization with vocab.txt loading from bundle
  verification: ./gradlew :Modules:Shared:NLU:iosSimulatorArm64Test

- id: 2.2
  name: Implement iOS NLU Processor with CoreML
  priority: P0
  effort: 12h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/nlu/IOSNluProcessor.kt
  description: CoreML BERT model loading and inference
  depends_on: [2.1]

- id: 2.3
  name: Implement iOS LLM Processor
  priority: P0
  effort: 16h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/llm/IOSLlmProcessor.kt
  description: llama.cpp Swift bindings or CoreML LLM
  depends_on: []

- id: 2.4
  name: Implement iOS Speech Engine
  priority: P0
  effort: 8h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/features/AppleSpeechEngine.kt
  description: SFSpeechRecognizer with AVAudioEngine
  depends_on: []

- id: 2.5
  name: Implement iOS Handler Executors
  priority: P0
  effort: 8h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/handlers/IOSHandlerFactory.kt
  description: UIAccessibility APIs for navigation, system, UI executors
  depends_on: []
```

### TERMINAL 3: Desktop Platform Implementation

```yaml
- id: 3.1
  name: Implement Desktop NLU with ONNX Runtime
  priority: P0
  effort: 6h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/nlu/DesktopNluProcessor.kt
  description: ONNX Runtime JVM for BERT inference
  depends_on: []

- id: 3.2
  name: Implement Desktop LLM with llama.cpp JNI
  priority: P0
  effort: 12h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/llm/DesktopLlmProcessor.kt
  description: JNI bindings to llama.cpp for GGUF model inference
  depends_on: []

- id: 3.3
  name: Implement Desktop Executors with AWT Robot
  priority: P0
  effort: 6h
  status: pending
  file: /Modules/VoiceOSCoreNG/src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/handlers/StubExecutors.kt
  description: AWT Robot for keyboard/mouse automation
  depends_on: []
```

### TERMINAL 4: Speech Engine Fixes

```yaml
- id: 4.1
  name: Bind Whisper JNI Methods
  priority: P0
  effort: 8h
  status: pending
  file: /Modules/VoiceOS/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperNative.kt
  description: Build whisper.cpp as .so, implement JNI native methods
  depends_on: []

- id: 4.2
  name: Integrate Google Cloud Speech Library
  priority: P1
  effort: 4h
  status: blocked
  blocker: GoogleCloudSpeechLite library not available
  file: /Modules/VoiceOS/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/google/
  description: Complete Google Cloud STT integration when library available

- id: 4.3
  name: Re-enable Learning System
  priority: P1
  effort: 6h
  status: pending
  file: /Modules/VoiceOS/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/common/LearningSystem.kt
  description: Create ILearningRepository, implement with SQLDelight, re-enable in engines
  depends_on: []
```

### TERMINAL 5: NLU Enhancements

```yaml
- id: 5.1
  name: Fix OnnxEmbeddingProvider Placeholder Mode
  priority: P1
  effort: 4h
  status: pending
  file: /Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/shared/nlu/embedding/OnnxEmbeddingProvider.kt
  description: Replace hash-based placeholders with real BERT embeddings
  depends_on: []

- id: 5.2
  name: Implement iOS CoreMLBackendSelector
  priority: P1
  effort: 4h
  status: pending
  file: /Modules/Shared/NLU/src/iosMain/kotlin/com/augmentalis/nlu/coreml/CoreMLBackendSelector.kt
  description: Device capability detection for optimal CoreML backend selection
  depends_on: [2.1, 2.2]
```

### TERMINAL 6: Testing & Verification

```yaml
- id: 6.1
  name: Create E2E Test for Android Command Flow
  priority: P1
  effort: 4h
  status: pending
  description: Test full command flow from speech to handler execution
  verification: ./gradlew :Modules:VoiceOSCoreNG:testDebugUnitTest --tests "*E2ECommandFlowTest*"

- id: 6.2
  name: Create NLU Integration Test
  priority: P1
  effort: 2h
  status: pending
  description: Test NLU classification accuracy with known intents
  depends_on: [1.3]

- id: 6.3
  name: Create LLM Fallback Test
  priority: P1
  effort: 2h
  status: pending
  description: Test LLM interpretation for ambiguous commands
  depends_on: [1.3]

- id: 6.4
  name: Create Screen Caching Test
  priority: P2
  effort: 2h
  status: pending
  description: Verify screen hash caching works correctly
  depends_on: []
```

---

## Quick Commands for Each Terminal

### Terminal 1 (Android Core)
```bash
# Start work
cd /Volumes/M-Drive/Coding/NewAvanues
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# After each change
./gradlew :Modules:VoiceOSCoreNG:compileDebugKotlinAndroid

# Run tests
./gradlew :Modules:VoiceOSCoreNG:testDebugUnitTest
```

### Terminal 2 (iOS Platform)
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Build iOS
./gradlew :Modules:VoiceOSCoreNG:compileKotlinIosArm64
./gradlew :Modules:Shared:NLU:compileKotlinIosArm64

# Run iOS tests
./gradlew :Modules:VoiceOSCoreNG:iosSimulatorArm64Test
```

### Terminal 3 (Desktop Platform)
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Build Desktop
./gradlew :Modules:VoiceOSCoreNG:compileKotlinDesktop

# Run Desktop tests
./gradlew :Modules:VoiceOSCoreNG:desktopTest
```

### Terminal 4 (Speech Engines)
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Build SpeechRecognition
./gradlew :Modules:VoiceOS:libraries:SpeechRecognition:assembleDebug

# Run tests
./gradlew :Modules:VoiceOS:libraries:SpeechRecognition:testDebugUnitTest
```

### Terminal 5 (NLU)
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Build NLU
./gradlew :Modules:Shared:NLU:assembleDebug

# Run tests
./gradlew :Modules:Shared:NLU:testDebugUnitTest
```

### Terminal 6 (Testing)
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Run all tests
./gradlew :Modules:VoiceOSCoreNG:allTests

# Run instrumented tests (requires device/emulator)
./gradlew :android:apps:voiceoscoreng:connectedDebugAndroidTest
```

---

## Dependency Graph

```
Task 2.1 (BertTokenizer iOS)
    │
    ├──► Task 2.2 (iOS NLU)
    │        │
    │        └──► Task 5.2 (CoreMLBackendSelector)
    │
    └──► Task 5.2 (CoreMLBackendSelector)

Task 1.3 (Verify Models)
    │
    ├──► Task 6.2 (NLU Integration Test)
    │
    └──► Task 6.3 (LLM Fallback Test)

All other tasks: No dependencies (can run in parallel)
```

---

## Status Legend

| Status | Meaning |
|--------|---------|
| `pending` | Not started |
| `in_progress` | Being worked on |
| `blocked` | Waiting on external dependency |
| `complete` | Done and verified |
| `failed` | Needs retry |

---

*Tasks file generated 2026-01-16*
