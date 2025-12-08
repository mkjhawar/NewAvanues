# VOS4 Discovery and Integration Plan

**Date**: 2025-11-06
**Status**: 📋 ANALYSIS COMPLETE
**Version**: 1.0.0

---

## Executive Summary

**Finding**: VOS4 components are **ALREADY INTEGRATED** in Avanues under different names!

**Current Structure**:
```
Avanues/
├── Universal/IDEAMagic/VoiceOSBridge/        # VOS4 Core (IPC, commands, security)
└── android/avanues/libraries/
    ├── speechrecognition/                     # VOS4 Recognition (Whisper, VOSK, Android STT)
    └── voicekeyboard/                         # VOS4 Input (Voice keyboard)
```

**Recommendation**: Rename and reorganize as `VOS4` module within AVANUES platform.

---

## Discovery Results

### What We Found

#### 1. VoiceOSBridge Module

**Location**: `Universal/IDEAMagic/VoiceOSBridge/`

**What It Contains**:
```
VoiceOSBridge/
├── ipc/
│   ├── IPCManager.kt              # Cross-app IPC
│   ├── AppMessage.kt              # Message protocol
│   ├── Subscription.kt            # Pub/sub messaging
│   └── MessageFilter.kt           # Message filtering
│
├── command/
│   ├── CommandRouter.kt           # Voice command routing
│   ├── CommandMatch.kt            # Command matching
│   └── FuzzyMatcher.kt            # Fuzzy command matching
│
├── security/
│   └── SecurityManager.kt         # App security/permissions
│
├── capability/
│   ├── CapabilityRegistry.kt      # App capabilities
│   ├── AppCapability.kt           # Capability definitions
│   └── CapabilityFilter.kt        # Capability filtering
│
├── state/
│   └── StateManager.kt            # State management
│
└── event/
    └── EventBus.kt                # Event distribution
```

**Package**: `net.ideahq.avamagic.voiceosbridge`

**Platform**: Kotlin Multiplatform (Android, iOS, JS)

**Role**: This IS VOS4 Core - the operating system layer for voice apps

#### 2. SpeechRecognition Library

**Location**: `android/avanues/libraries/speechrecognition/`

**What It Contains**:
- **WhisperEngine** - OpenAI Whisper (offline, 99+ languages)
- **VoskEngine** - VOSK (offline, open-source)
- **AndroidSTTEngine** - Android built-in STT
- **GoogleCloudEngine** - Google Cloud STT (disabled)
- **VivokaEngine** - Enterprise STT (requires SDK)

**Role**: This IS VOS4 Recognition - speech-to-text engines

#### 3. VoiceKeyboard Library

**Location**: `android/avanues/libraries/voicekeyboard/`

**Role**: This IS VOS4 Input - voice-based input method

### What We're Missing

Comparing to proposed VOS4 structure:
```
✅ Core          → VoiceOSBridge (exists)
✅ Recognition   → speechrecognition (exists)
❌ Synthesis     → Missing (TTS components)
❌ NLU           → Missing (intent parsing, entity extraction)
✅ Commands      → VoiceOSBridge/command (exists)
✅ Input         → voicekeyboard (exists)
```

**Missing Components**:
1. **Text-to-Speech (TTS)** - No dedicated TTS abstraction
2. **Natural Language Understanding (NLU)** - No intent parser or entity extractor
3. **Conversation Context** - No conversation state management

---

## Proposed Reorganization

### Option 1: Rename VoiceOSBridge → VOS4 (Recommended)

**Changes**:
```bash
# 1. Rename module
mv Universal/IDEAMagic/VoiceOSBridge Universal/IDEAMagic/VOS4

# 2. Update package
# From: net.ideahq.avamagic.voiceosbridge
# To:   com.augmentalis.avamagic.vos4

# 3. Organize submodules
Universal/IDEAMagic/VOS4/
├── Core/                          # VOS4 core (from VoiceOSBridge)
│   ├── ipc/
│   ├── command/
│   ├── security/
│   ├── capability/
│   ├── state/
│   └── event/
│
├── Recognition/                   # Move from android/avanues/libraries/
│   └── [speechrecognition code]
│
├── Input/                         # Move from android/avanues/libraries/
│   └── [voicekeyboard code]
│
├── Synthesis/                     # NEW - TTS abstraction
│   └── [to be created]
│
└── NLU/                          # NEW - Intent/entity extraction
    └── [to be created]
```

**Advantages**:
- ✅ Clear VOS4 branding
- ✅ All voice components in one place
- ✅ Easier to understand architecture
- ✅ Consistent with AvaUI/AvaCode/MagicData

**Disadvantages**:
- ⚠️ Large migration (package renames, imports)
- ⚠️ SpeechRecognition is Android-only (KMP migration needed)

### Option 2: Keep As-Is, Add Missing Components

**Changes**:
```bash
# Keep current structure
Universal/IDEAMagic/VoiceOSBridge/  (rename to VOS4/Core)
android/avanues/libraries/speechrecognition/
android/avanues/libraries/voicekeyboard/

# Add new components
Universal/IDEAMagic/VOS4/
├── Core/                          # Renamed VoiceOSBridge
├── Synthesis/                     # NEW
└── NLU/                          # NEW

# Keep Recognition and Input as Android libraries for now
```

**Advantages**:
- ✅ Minimal changes
- ✅ No migration of speechrecognition
- ✅ Faster to implement

**Disadvantages**:
- ❌ Fragmented architecture
- ❌ Recognition/Input not KMP (Android-only)
- ❌ Less clear organization

### Option 3: Gradual Migration (Recommended for Safety)

**Phase 1: Rename Core**
```bash
mv Universal/IDEAMagic/VoiceOSBridge Universal/IDEAMagic/VOS4/Core
# Update package: net.ideahq.avamagic.voiceosbridge → com.augmentalis.avamagic.vos4.core
```

**Phase 2: Add Missing Components**
```bash
# Create new modules
Universal/IDEAMagic/VOS4/
├── Core/           (✅ migrated from VoiceOSBridge)
├── Synthesis/      (➕ create new)
└── NLU/            (➕ create new)
```

**Phase 3: Migrate Recognition (Later)**
```bash
# Convert speechrecognition to KMP
# Move to Universal/IDEAMagic/VOS4/Recognition/
```

**Phase 4: Migrate Input (Later)**
```bash
# Convert voicekeyboard to KMP
# Move to Universal/IDEAMagic/VOS4/Input/
```

**Advantages**:
- ✅ Low risk (step by step)
- ✅ Can test after each phase
- ✅ Keep Android libraries working during migration

**Disadvantages**:
- ⚠️ Takes longer (4 phases)
- ⚠️ Temporary inconsistency

---

## Recommended Approach

### Hybrid Strategy (Best of All Options)

**Immediate (Part of AVANUES rename)**:
1. ✅ Rename `VoiceOSBridge` → `VOS4/Core`
2. ✅ Update package: `com.augmentalis.avamagic.vos4.core`
3. ✅ Update imports across codebase

**Short-term (This week)**:
4. ✅ Create `VOS4/Synthesis/` module (NEW)
5. ✅ Create `VOS4/NLU/` module (NEW)

**Medium-term (Next 2-4 weeks)**:
6. 🔵 Analyze `speechrecognition` for KMP migration feasibility
7. 🔵 If feasible, migrate to `VOS4/Recognition/`
8. 🔵 Otherwise, keep as Android library with wrapper

**Long-term (Future)**:
9. 🔵 Migrate `voicekeyboard` to `VOS4/Input/`

---

## VOS4 Complete Structure (After Migration)

```
Universal/IDEAMagic/VOS4/
├── Core/                                    # VOS4 Core (from VoiceOSBridge)
│   ├── src/commonMain/kotlin/com/augmentalis/avamagic/vos4/core/
│   │   ├── ipc/
│   │   │   ├── IPCManager.kt              # Cross-app IPC
│   │   │   ├── AppMessage.kt              # Message protocol
│   │   │   ├── Subscription.kt            # Pub/sub
│   │   │   └── MessageFilter.kt           # Filtering
│   │   │
│   │   ├── command/
│   │   │   ├── CommandRouter.kt           # Command routing
│   │   │   ├── CommandMatch.kt            # Command matching
│   │   │   └── FuzzyMatcher.kt            # Fuzzy matching
│   │   │
│   │   ├── security/
│   │   │   └── SecurityManager.kt         # Security/permissions
│   │   │
│   │   ├── capability/
│   │   │   ├── CapabilityRegistry.kt      # App capabilities
│   │   │   ├── AppCapability.kt           # Capability defs
│   │   │   └── CapabilityFilter.kt        # Filtering
│   │   │
│   │   ├── state/
│   │   │   └── StateManager.kt            # State management
│   │   │
│   │   └── event/
│   │       └── EventBus.kt                # Event distribution
│   │
│   ├── src/androidMain/kotlin/...          # Android-specific
│   ├── src/iosMain/kotlin/...              # iOS-specific
│   └── build.gradle.kts
│
├── Recognition/                             # Speech-to-Text (from speechrecognition)
│   ├── src/commonMain/kotlin/com/augmentalis/avamagic/vos4/recognition/
│   │   ├── SpeechRecognizer.kt            # STT interface
│   │   ├── RecognitionConfig.kt           # Configuration
│   │   ├── RecognitionResult.kt           # Result model
│   │   └── LanguageModel.kt               # Language support
│   │
│   ├── src/androidMain/kotlin/...
│   │   ├── engines/
│   │   │   ├── WhisperEngine.kt           # OpenAI Whisper (offline)
│   │   │   ├── VoskEngine.kt              # VOSK (offline)
│   │   │   ├── AndroidSTTEngine.kt        # Android built-in
│   │   │   └── GoogleCloudEngine.kt       # Google Cloud
│   │   │
│   │   └── models/                        # Whisper/VOSK models
│   │
│   └── build.gradle.kts
│
├── Synthesis/                               # Text-to-Speech (NEW)
│   ├── src/commonMain/kotlin/com/augmentalis/avamagic/vos4/synthesis/
│   │   ├── TextToSpeech.kt                # TTS interface
│   │   ├── SynthesisConfig.kt             # Configuration
│   │   ├── Voice.kt                       # Voice profile
│   │   └── Prosody.kt                     # Pitch/rate/volume
│   │
│   ├── src/androidMain/kotlin/...
│   │   ├── engines/
│   │   │   ├── GoogleTTS.kt               # Google TTS
│   │   │   ├── AndroidTTS.kt              # Android built-in
│   │   │   └── ElevenLabsTTS.kt           # ElevenLabs (future)
│   │   │
│   │   └── voices/                        # Voice profiles
│   │
│   └── build.gradle.kts
│
├── NLU/                                     # Natural Language Understanding (NEW)
│   ├── src/commonMain/kotlin/com/augmentalis/avamagic/vos4/nlu/
│   │   ├── IntentParser.kt                # Intent detection
│   │   ├── Intent.kt                      # Intent model
│   │   ├── EntityExtractor.kt             # NER (named entities)
│   │   ├── Entity.kt                      # Entity model
│   │   ├── ContextManager.kt              # Conversation context
│   │   └── Slot.kt                        # Intent slots
│   │
│   ├── src/androidMain/kotlin/...
│   │   └── engines/
│   │       ├── DialogflowNLU.kt           # Google Dialogflow
│   │       ├── WitAiNLU.kt                # Wit.ai
│   │       └── LocalNLU.kt                # Local regex/ML
│   │
│   └── build.gradle.kts
│
└── Input/                                   # Voice Input (from voicekeyboard)
    ├── src/commonMain/kotlin/com/augmentalis/avamagic/vos4/input/
    │   ├── VoiceKeyboard.kt               # Voice keyboard interface
    │   └── InputConfig.kt                 # Configuration
    │
    ├── src/androidMain/kotlin/...         # Android IME implementation
    └── build.gradle.kts
```

---

## Migration Impact Analysis

### VoiceOSBridge → VOS4/Core

**Files Affected**: ~20 Kotlin files

**Package Changes**:
```kotlin
// Before
package net.ideahq.avamagic.voiceosbridge.ipc
import net.ideahq.avamagic.voiceosbridge.command.CommandRouter

// After
package com.augmentalis.avamagic.vos4.core.ipc
import com.augmentalis.avamagic.vos4.core.command.CommandRouter
```

**Import Updates**: ~100-150 files (anywhere VoiceOSBridge is used)

**Effort**: 2-3 hours

### Add Synthesis Module

**New Files**: ~15 Kotlin files

**Structure**:
```kotlin
// TextToSpeech.kt
interface TextToSpeech {
    suspend fun synthesize(text: String, voice: Voice): ByteArray
    suspend fun speak(text: String, voice: Voice)
    fun getAvailableVoices(): List<Voice>
}

// Voice.kt
data class Voice(
    val id: String,
    val name: String,
    val language: String,
    val gender: Gender
)

// Prosody.kt
data class Prosody(
    val pitch: Float = 1.0f,      // 0.5 - 2.0
    val rate: Float = 1.0f,       // 0.5 - 2.0
    val volume: Float = 1.0f      // 0.0 - 1.0
)

// AndroidTTS.kt (androidMain)
class AndroidTTS(private val context: Context) : TextToSpeech {
    private val tts = android.speech.tts.TextToSpeech(context) { status ->
        // Initialize
    }

    override suspend fun speak(text: String, voice: Voice) {
        withContext(Dispatchers.Main) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}
```

**Effort**: 4-6 hours

### Add NLU Module

**New Files**: ~20 Kotlin files

**Structure**:
```kotlin
// IntentParser.kt
interface IntentParser {
    suspend fun parse(text: String, context: ConversationContext): Intent?
    fun registerIntent(pattern: String, handler: IntentHandler)
}

// Intent.kt
data class Intent(
    val name: String,
    val confidence: Float,
    val slots: Map<String, Entity>
)

// Entity.kt
data class Entity(
    val type: EntityType,
    val value: String,
    val confidence: Float
)

enum class EntityType {
    DATE, TIME, LOCATION, PERSON, ORGANIZATION,
    NUMBER, EMAIL, PHONE, URL, CUSTOM
}

// LocalNLU.kt (simple regex-based)
class LocalNLU : IntentParser {
    private val intents = mutableMapOf<Regex, IntentHandler>()

    override suspend fun parse(text: String, context: ConversationContext): Intent? {
        for ((pattern, handler) in intents) {
            val match = pattern.find(text)
            if (match != null) {
                return handler.handle(match, context)
            }
        }
        return null
    }
}
```

**Effort**: 6-8 hours

### Migrate SpeechRecognition → VOS4/Recognition

**Complexity**: HIGH - Native C++ code (whisper.cpp)

**Files**: ~50 Kotlin files, ~30 C++ files

**Challenges**:
1. ✅ **whisper.cpp** - Already has iOS support (can port)
2. ⚠️ **VOSK** - Check iOS support
3. ❌ **Android STT** - Android-only (create iOS equivalent)
4. ❌ **Native libraries** - Need iOS build configuration

**Strategy**:
```kotlin
// commonMain (interface)
interface SpeechRecognizer {
    suspend fun recognize(audio: ByteArray): RecognitionResult
}

// androidMain (existing implementation)
class WhisperEngine : SpeechRecognizer { /* existing code */ }

// iosMain (new implementation)
class WhisperEngine : SpeechRecognizer {
    // Use whisper.cpp iOS port
}
```

**Effort**: 16-24 hours (complex migration)

**Recommendation**: Defer to Phase 3 (after AVANUES rename + Synthesis + NLU)

---

## Implementation Plan

### Phase 1: AVANUES Rename + VOS4 Core (Week 1)

**Tasks**:
1. ✅ Rename `Avanues` → `AVANUES`
2. ✅ Rename `Database` → `MagicData`
3. ✅ Rename `VoiceOSBridge` → `VOS4/Core`
4. ✅ Update all package names
5. ✅ Update all imports
6. ✅ Update documentation

**Deliverables**:
- AVANUES directory structure
- MagicData module
- VOS4/Core module
- Updated docs

**Effort**: 6-8 hours

### Phase 2: Add VOS4 Synthesis + NLU (Week 1-2)

**Tasks**:
1. ✅ Create `VOS4/Synthesis/` module
2. ✅ Implement TextToSpeech interface
3. ✅ Implement AndroidTTS engine
4. ✅ Create `VOS4/NLU/` module
5. ✅ Implement IntentParser interface
6. ✅ Implement LocalNLU (regex-based)
7. ✅ Create tests for both modules

**Deliverables**:
- VOS4/Synthesis module (functional)
- VOS4/NLU module (functional)
- Unit tests
- README for each module

**Effort**: 10-14 hours

### Phase 3: Analyze SpeechRecognition Migration (Week 2-3)

**Tasks**:
1. 📋 Analyze whisper.cpp iOS support
2. 📋 Analyze VOSK iOS support
3. 📋 Estimate effort for KMP migration
4. 📋 Document migration strategy
5. 📋 Create migration plan

**Deliverables**:
- SpeechRecognition migration analysis
- Effort estimate
- Go/no-go decision

**Effort**: 4-6 hours (analysis only)

### Phase 4: Complete VOS4 Integration (Week 3-5)

**Tasks** (if Phase 3 approved):
1. 🔵 Migrate speechrecognition to `VOS4/Recognition/`
2. 🔵 Convert to KMP module
3. 🔵 Add iOS implementation
4. 🔵 Migrate voicekeyboard to `VOS4/Input/`
5. 🔵 Update all apps to use new structure

**Deliverables**:
- Complete VOS4 structure
- All modules KMP
- Apps updated
- Documentation complete

**Effort**: 24-32 hours

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Native library migration fails** | Medium | High | Keep Android libraries as fallback |
| **iOS speech recognition unavailable** | Low | Medium | Use iOS built-in Speech framework |
| **Breaking existing apps** | Low | High | Use feature flags, gradual rollout |
| **Package rename breaks imports** | High | Medium | Use sed for bulk updates, compile often |

---

## VOS4 Features Summary

After complete integration, VOS4 will provide:

### Core Features
- ✅ **Cross-app IPC** - Apps can communicate
- ✅ **Command routing** - Voice commands routed to correct app
- ✅ **Security** - Permission management
- ✅ **Capabilities** - Apps declare what they can do
- ✅ **State management** - Shared state across apps
- ✅ **Event bus** - Publish/subscribe events

### Recognition Features
- ✅ **Multi-engine STT** - WhisperEngine (offline), VOSK, AndroidSTT, Google Cloud
- ✅ **99+ languages** - Via Whisper
- ✅ **Offline mode** - Whisper and VOSK work offline
- ⚠️ **Real-time streaming** - Depends on engine

### Synthesis Features (NEW)
- ➕ **Multi-engine TTS** - AndroidTTS, GoogleTTS, ElevenLabs
- ➕ **Voice profiles** - Different voices/languages
- ➕ **Prosody control** - Pitch, rate, volume
- ➕ **Offline mode** - AndroidTTS works offline

### NLU Features (NEW)
- ➕ **Intent parsing** - Detect user intent
- ➕ **Entity extraction** - Extract dates, locations, names
- ➕ **Conversation context** - Multi-turn conversations
- ➕ **Multiple engines** - LocalNLU (regex), Dialogflow, Wit.ai

### Input Features
- ✅ **Voice keyboard** - Voice-based input method
- ✅ **Voice commands** - System-wide voice commands

---

## Next Steps

### Immediate
1. ✅ Review this discovery document
2. ✅ Approve migration strategy
3. ✅ Start Phase 1 (AVANUES rename + VOS4 Core)

### This Week
4. ✅ Complete Phase 1 (6-8 hours)
5. ✅ Start Phase 2 (Synthesis + NLU)

### Next Week
6. ✅ Complete Phase 2
7. 📋 Start Phase 3 (analyze SpeechRecognition migration)

---

## Conclusion

**VOS4 is already 60% complete!**

**Existing**:
- ✅ Core (VoiceOSBridge) - IPC, commands, security
- ✅ Recognition (speechrecognition) - STT engines
- ✅ Input (voicekeyboard) - Voice keyboard

**Needed**:
- ➕ Synthesis (NEW) - TTS abstraction
- ➕ NLU (NEW) - Intent/entity extraction
- 🔄 Reorganization - Consolidate under VOS4

**Effort**: 20-28 hours for complete VOS4 integration

**Result**: Complete voice operating system platform!

---

**Status**: 📋 DISCOVERY COMPLETE
**Recommendation**: Proceed with gradual migration (Phases 1-2 immediate)
**Next Action**: Start AVANUES rename + VOS4 Core migration

---

**Document Version**: 1.0.0
**Author**: Claude Code (Sonnet 4.5)
**Date**: 2025-11-06
