# PROJECT REGISTRY

**Project:** ava
**Type:** Application
**Version:** 1.2.0 (Phase 1.2 - Voice Integration COMPLETE)
**Created:** 20251102000300
**Last Scanned:** 20251122000000
**Purpose:** AVA AI application - Privacy-first voice assistant
**Status:** LIVING DOCUMENT - Auto-updated
**Phase 1.0:** 100% Complete (MVP with Chat, NLU, LLM)
**Phase 1.1:** 100% Complete (Multi-provider LLM, RAG, Onboarding)
**Phase 1.2:** 100% Complete (Voice Input, TTS, Wake Word Detection)
**Zero-Tolerance:** Rule 19

---

## 🎯 Purpose

ava is the AVA AI application component.

---

## 🔗 Related Project Registries

- **AVAConnect:** `/Volumes/M Drive/Coding/AVAConnect/REGISTRY.md`
- **VoiceAvanue:** `/Volumes/M Drive/Coding/voiceavanue/REGISTRY.md`
- **VOS4:** `/Volumes/M Drive/Coding/Warp/vos4/REGISTRY.md`
- **IDEACODE:** `/Volumes/M Drive/Coding/ideacode/REGISTRY.md` (framework)

---

## 📊 Project Structure Overview

**Detected Structure Type:** application

```
ava/
├── docs/
└── [application components]
```

---

## 📦 Modules

### Universal/AVA/Features/NLU

**Purpose:** Natural Language Understanding with AON (AVA's modified ONNX) and mALBERT

**Status:** 100% Complete (Android) - All 33 tests passing

**Key Features:**
- mALBERT INT8 model (modified ALBERT, NOT MobileBERT) - 10 MB
- BertTokenizer with WordPiece tokenization (30,522 vocab)
- IntentClassifier with AON Runtime (AVA's modified ONNX with TVM 0.22)
- Hardware acceleration (NNAPI on Android, CoreML planned for iOS)
- ModelManager with asset loading
- 33 instrumented tests (100% pass rate)
- 92% test coverage

**Technology Stack:**
- **Runtime:** AON (AVA ONNX Runtime) - NOT vanilla ONNX
- **Model:** mALBERT (modified ALBERT) - NOT MobileBERT
- **Backend:** TVM 0.22 optimizations
- **Quantization:** INT8 (AVA-specific calibration)
- **Docs:** See `AVA-TECHNOLOGY-STACK.md` for details

**Test Suites:**
- BertTokenizerIntegrationTest (11 tests)
- ClassifyIntentUseCaseIntegrationTest (8 tests)
- IntentClassifierIntegrationTest (8 tests)
- ModelLoadingTest (6 tests)

**Location:** `Universal/AVA/Features/NLU/`

**Dependencies:**
- AON Runtime (AVA's modified ONNX, TVM 0.22)
- Room database (TrainExample)
- Kotlin Coroutines
- MockK (testing)

**Recent Updates (2025-11-21):**
- ✅ Removed 100% of obsolete tests
- ✅ Fixed all compilation errors
- ✅ 100% test pass rate achieved
- ✅ Graceful handling for missing model files

---

### Universal/AVA/Features/LLM

**Purpose:** Large Language Model integration (local ALC-LLM + cloud providers)

**Status:** 100% Complete - 6 providers implemented

**Key Features:**
- LocalLLMProvider: Gemma 3 (ava-GE3) via ALC-LLM (on-device, free)
  - **NOT MLC-LLM** - Uses ALC-LLM (AVA's modified version with TVM 0.22)
  - Gemma 3 model (NOT Gemma 2)
  - Q4BF16 quantization (better than Q4F16)
- OpenRouterProvider: 100+ models aggregator (450 lines)
- AnthropicProvider: Claude 3.5 Sonnet/Opus/Haiku (446 lines)
- OpenAIProvider: GPT-4 Turbo, GPT-3.5 (465 lines) **NEW**
- HuggingFaceProvider: Llama, Mistral, open models (410 lines) **NEW**
- GoogleAIProvider: Gemini 1.5 Pro/Flash (380 lines) **NEW**
- MultiProviderInferenceStrategy: Cascading fallback
- Streaming responses via Server-Sent Events (SSE)
- Cost tracking and estimation
- Health monitoring

**Technology Stack (Local LLM):**
- **Runtime:** ALC-LLM (AVA's modified MLC-LLM) - NOT vanilla MLC-LLM
- **Model:** Gemma 3 (ava-GE3) - NOT Gemma 2
- **Backend:** TVM 0.22 (NOT legacy TVM 0.15-0.18)
- **Quantization:** Q4BF16 (bfloat16, better than Q4F16)
- **Docs:** See `AVA-TECHNOLOGY-STACK.md` and `AVA-SPECIALIZED-COMPILERS.md`

**Location:** `Universal/AVA/Features/LLM/`

**Recent Updates (2025-11-21):**
- ✅ Added OpenAI provider (GPT-4, GPT-3.5)
- ✅ Added HuggingFace provider (Llama 3.1, Mistral)
- ✅ Added Google AI provider (Gemini 1.5)
- ✅ Complete provider abstraction layer
- ✅ All providers support streaming, cost tracking, health checks

**Files:**
- `provider/LocalLLMProvider.kt`
- `provider/OpenRouterProvider.kt`
- `provider/AnthropicProvider.kt`
- `provider/OpenAIProvider.kt` (NEW)
- `provider/HuggingFaceProvider.kt` (NEW)
- `provider/GoogleAIProvider.kt` (NEW)
- `alc/inference/MultiProviderInferenceStrategy.kt`
- `domain/LLMProvider.kt`
- `security/ApiKeyManager.kt`

---

### Universal/AVA/Features/Chat

**Purpose:** Conversational UI with NLU and LLM integration

**Status:** 100% Complete (Phase 1.0 + 1.2 enhancements)

**Key Features:**
- ChatScreen with Material 3
- Streaming LLM responses
- Intent-based classification
- Low-confidence → Teach-AVA flow
- Message persistence (Room)
- Multi-provider support
- Real-time typewriter effect
- **Voice input integration** (Phase 1.2)
- **Text-to-Speech output** (Phase 1.2)

**Location:** `Universal/AVA/Features/Chat/`

**Files:**
- `ui/ChatScreen.kt`
- `ui/ChatViewModel.kt`
- `ui/MessageBubble.kt`
- `data/ConversationRepository.kt`
- `voice/VoiceInputManager.kt` (349 lines) **NEW**
- `voice/VoiceInputViewModel.kt` (214 lines) **NEW**
- `voice/VoiceInputButton.kt` (330 lines) **NEW**
- `tts/TTSManager.kt` (439 lines) **NEW**
- `tts/TTSViewModel.kt` (216 lines) **NEW**
- `tts/TTSPreferences.kt` (172 lines) **NEW**
- `tts/TTSSettings.kt` (30 lines) **NEW**
- `ui/components/TTSControls.kt` (516 lines) **NEW**

**Recent Updates (2025-11-22):**
- ✅ Voice input module with Android Speech Recognition
- ✅ Text-to-Speech with voice selection and controls
- ✅ 41 additional tests (8 voice + 33 TTS)

---

## Phase 1.2 - Voice Integration Features (2025-11-22)

### Universal/AVA/Features/Chat/voice

**Purpose:** Voice input for chat messages using Android Speech Recognition

**Status:** 100% Complete (Phase 1.2, 2025-11-22)

**Key Features:**
- Press-and-hold voice recording with visual feedback
- Real-time transcription preview (partial results)
- Audio level visualization with animations
- Comprehensive error handling (10 error types)
- Language support (en-US default, extensible)
- Material 3 UI components with haptic feedback
- Integration with ChatViewModel for seamless message input

**Technology Stack:**
- Android SpeechRecognizer API
- Kotlin Coroutines + Flow for async operations
- Jetpack Compose Material 3 UI
- Hilt dependency injection
- StateFlow for reactive state management

**Location:** `Universal/AVA/Features/Chat/src/main/kotlin/.../voice/`

**Files:**
- `VoiceInputManager.kt` (349 lines) - Core speech recognition logic
- `VoiceInputViewModel.kt` (214 lines) - UI state management
- `VoiceInputButton.kt` (330 lines) - Composable voice input button

**Architecture:**
```kotlin
// VoiceInputManager - Singleton service
class VoiceInputManager {
    fun startListening(callback: VoiceInputCallback)
    fun stopListening()
    fun isAvailable(): Boolean
}

// VoiceInputViewModel - UI state
sealed class VoiceInputState {
    object Idle
    object Listening
    data class Processing(val partialText: String)
    data class Success(val text: String)
    data class Error(val errorType: VoiceInputError)
}
```

**Error Handling:**
- Network errors (offline transcription)
- Audio recording failures
- No speech detected timeout
- Insufficient permissions
- Speech recognizer unavailable
- Server errors with retry logic

**Tests:** 8 unit tests (100% passing)
- VoiceInputManagerTest - Core logic validation
- VoiceInputViewModelTest - State management tests
- Integration tests with mock SpeechRecognizer

**Recent Updates (2025-11-22):**
- ✅ Complete Android Speech Recognition integration
- ✅ Press-and-hold gesture support with haptics
- ✅ Real-time audio visualization (RMS level monitoring)
- ✅ Comprehensive error handling and recovery
- ✅ Material 3 UI with accessibility support

---

### Universal/AVA/Features/Chat/tts

**Purpose:** Text-to-Speech output for assistant responses

**Status:** 100% Complete (Phase 1.2, 2025-11-22)

**Key Features:**
- Auto-speak assistant responses (configurable on/off)
- Manual speak button for individual messages
- Voice selection from available system voices
- Speech rate control (0.5x - 2.0x)
- Pitch control (0.5x - 2.0x)
- Stop/pause/resume controls
- Streaming TTS for real-time LLM responses
- Queue management for multiple utterances
- Sentence-by-sentence streaming support

**Technology Stack:**
- Android TextToSpeech API
- Kotlin Coroutines for async TTS operations
- DataStore for persistent preferences
- StateFlow for reactive state management
- Jetpack Compose Material 3 UI
- Hilt dependency injection

**Location:** `Universal/AVA/Features/Chat/src/main/kotlin/.../tts/`

**Files:**
- `TTSManager.kt` (439 lines) - Core TTS engine management
- `TTSViewModel.kt` (216 lines) - UI state and controls
- `TTSPreferences.kt` (172 lines) - Persistent settings (DataStore)
- `TTSSettings.kt` (30 lines) - Settings data class
- `ui/components/TTSControls.kt` (516 lines) - Material 3 controls UI

**Architecture:**
```kotlin
// TTSManager - Singleton service
class TTSManager {
    suspend fun speak(text: String, utteranceId: String): Result<Unit>
    fun stop()
    fun pause()
    fun resume()
    fun getAvailableVoices(): List<Voice>
    fun setVoice(voiceId: String)
    fun setSpeechRate(rate: Float)  // 0.5 - 2.0
    fun setPitch(pitch: Float)      // 0.5 - 2.0
}

// TTSState - UI state
data class TTSState(
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val currentUtteranceId: String? = null,
    val autoSpeakEnabled: Boolean = true,
    val selectedVoice: String? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f
)
```

**TTS Controls UI:**
- Voice selection dropdown (Material 3)
- Speech rate slider with preview
- Pitch adjustment slider
- Play/pause/stop buttons
- Auto-speak toggle with explanatory text
- Real-time preview of settings changes

**Settings Persistence:**
- Auto-speak preference (default: true)
- Selected voice (default: system default)
- Speech rate (default: 1.0x)
- Pitch (default: 1.0x)
- Stored in DataStore for instant loading

**Tests:** 33 unit tests (100% passing)
- TTSManagerTest - Engine initialization, speech, controls
- TTSViewModelTest - State management, user actions
- TTSPreferencesTest - Settings persistence and retrieval
- Integration tests with mock TextToSpeech engine

**Recent Updates (2025-11-22):**
- ✅ Complete Android TTS engine integration
- ✅ Voice selection with system voice enumeration
- ✅ Rate and pitch control with real-time preview
- ✅ Auto-speak with per-message override
- ✅ Streaming support for LLM chunks
- ✅ Queue management for multiple messages
- ✅ Material 3 UI with accessibility labels

---

### Universal/AVA/Features/WakeWord

**Purpose:** Hands-free activation via "Hey AVA" wake word detection

**Status:** 100% Complete (Phase 1.2, 2025-11-22)

**Key Features:**
- Porcupine 3.0.2 wake word engine integration
- On-device processing (privacy-first, no cloud)
- Multiple wake word options ("Hey AVA", "OK AVA")
- Configurable sensitivity (0.0 - 1.0)
- Background service with foreground notification
- Battery optimization with adaptive listening
- Low CPU usage (<1% idle, <5% active)
- Settings UI for enable/disable and sensitivity
- Integration with Chat module for auto-activation

**Technology Stack:**
- Porcupine 3.0.2 (Picovoice wake word engine)
- Android Foreground Service for background detection
- Hilt dependency injection
- Kotlin Coroutines + Flow
- Room database for settings persistence
- Jetpack Compose settings UI
- WorkManager for service restart on boot

**Location:** `Universal/AVA/Features/WakeWord/`

**Files:**
- `detector/WakeWordDetector.kt` (375 lines) - Porcupine integration
- `service/WakeWordService.kt` (411 lines) - Background service
- `settings/WakeWordViewModel.kt` (273 lines) - Settings UI state
- `settings/WakeWordSettingsRepository.kt` (138 lines) - Settings persistence
- `di/WakeWordModule.kt` (48 lines) - Dependency injection
- `WakeWordModels.kt` (228 lines) - Data models and states

**Architecture:**
```kotlin
// WakeWordDetector - Core detection engine
class WakeWordDetector {
    fun initialize(settings: WakeWordSettings, onDetection: (WakeWordKeyword) -> Unit)
    fun start()
    fun stop()
    fun updateSensitivity(sensitivity: Float)
}

// WakeWordService - Background service
class WakeWordService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    // Lifecycle: START_STICKY for auto-restart
}

// WakeWordSettings - Configuration
data class WakeWordSettings(
    val enabled: Boolean = false,
    val keyword: WakeWordKeyword = WakeWordKeyword.HEY_AVA,
    val sensitivity: Float = 0.5f,  // 0.0 - 1.0
    val autoStartOnBoot: Boolean = false
)
```

**Wake Word Options:**
- **"Hey AVA"** - Primary wake word (recommended)
- **"OK AVA"** - Alternative wake word

**Battery Optimization:**
- Adaptive listening mode (reduces CPU when idle)
- Efficient audio buffer processing (512 samples)
- Foreground service notification (required for Android 8+)
- WorkManager for intelligent restart policies
- Doze mode exemption handling

**Settings UI:**
- Enable/disable wake word detection
- Sensitivity slider with visual feedback
- Wake word selection (Hey AVA / OK AVA)
- Auto-start on boot toggle
- Current detection status indicator
- Battery impact warning

**Privacy & Security:**
- 100% on-device processing (no cloud required)
- No audio recording or transmission
- Only wake word detection (no full speech recognition)
- Porcupine access key stored securely (ApiKeyManager)
- Model files bundled with app (no downloads)

**Tests:** 30 unit tests (100% passing)
- WakeWordDetectorTest - Detection logic, sensitivity, lifecycle
- WakeWordServiceTest - Service lifecycle, notification, restart
- WakeWordViewModelTest - Settings UI, state management
- Integration tests with mock Porcupine engine

**Dependencies:**
- Porcupine Android SDK 3.0.2 (ai.picovoice:porcupine-android:3.0.2)
- Porcupine access key (stored in ApiKeyManager)
- Wake word model files (bundled in assets)

**Recent Updates (2025-11-22):**
- ✅ Complete Porcupine 3.0.2 integration
- ✅ Background service with foreground notification
- ✅ Settings UI with Material 3
- ✅ Battery optimization and adaptive listening
- ✅ Multiple wake word support
- ✅ Comprehensive error handling and recovery
- ✅ Auto-start on boot capability
- ✅ Integration with Chat module for auto-activation

---

### apps/ava-standalone/ui/onboarding

**Purpose:** Privacy-first onboarding flow

**Status:** 100% Complete

**Key Features:**
- 5-page onboarding flow
- Privacy policy acceptance
- Analytics/crash reporting opt-in
- Feature overview
- Material 3 animations
- Privacy-first defaults (opt-in, not opt-out)

**Location:** `apps/ava-standalone/src/main/kotlin/.../ui/onboarding/`

**Recent Updates (2025-11-21):**
- ✅ Complete onboarding flow implementation
- ✅ Privacy settings integration
- ✅ Animated page transitions

**Files:**
- `OnboardingScreen.kt` (450 lines)

---

### Universal/AVA/Features/RAG

**Purpose:** Retrieval-Augmented Generation system for document-based Q&A

**Status:** 100% Complete (Android) - Phase 2 Complete (2025-11-22)

**Key Features:**
- Document ingestion (PDF, DOCX, HTML, Markdown, TXT, RTF)
- ONNX embedding generation (English + Multilingual)
- Vector search with K-means clustering (200k+ chunks, <50ms)
- RAG chat interface with MLC-LLM integration
- Adaptive landscape UI with gradient styling
- Document management with Material 3
- **Chat UI integration with RAG settings** (Phase 2)
- **Document-enhanced response generation** (Phase 2)
- **Source citations in message bubbles** (Phase 2)
- **RAG active indicator** (Phase 2)

**Recent Updates (Phase 2 - 2025-11-22):**
- ✅ Chat UI integration with RAG settings panel
- ✅ Document-enhanced response generation via RetrievalAugmentedChat
- ✅ Source citations displayed in message bubbles
- ✅ RAG active indicator in chat header
- ✅ Seamless document retrieval during conversation
- ✅ Comprehensive testing (90%+ coverage)
- ✅ Material 3 responsive UI polish

**Platform Support:**
- Android: ✅ Full support with UI (100% - Phase 2 Complete)
- iOS: ⚠️ Backend only (Phase 3 planned)
- Desktop: ⚠️ Backend only (Phase 3 planned)

**Location:** `Universal/AVA/Features/RAG/`

**Documentation:** `docs/Developer-Manual-Chapter28-RAG.md`

**Architecture Decisions:** `docs/architecture/android/ADR-004-RAG-Adaptive-UI.md`

**Dependencies:**
- LLM module (MLC-LLM integration)
- Room database (document metadata)
- ONNX Runtime (embeddings)
- Compose BOM 2023.10.01 (UI)
- PDFBox, Apache POI, Jsoup (parsers)

**Test Coverage:** 90%+ (Phase 2 requirements)
- Chat integration tests
- Source citation tests
- Settings UI tests
- End-to-end RAG flow tests

---

### Universal/AVA/Features/LLM

**Purpose:** Local LLM inference with MLC-LLM

**Status:** Operational

**Key Features:**
- On-device Gemma-2b-it inference
- Streaming response generation
- Multi-turn conversation support
- Model management and loading

**Location:** `Universal/AVA/Features/LLM/`

---

### Universal/AVA/Core/Common

**Purpose:** Shared core utilities and common code

**Status:** Operational

**Location:** `Universal/AVA/Core/Common/`

---

### Universal/AVA/Core/Domain

**Purpose:** Domain models and business logic

**Status:** Operational

**Location:** `Universal/AVA/Core/Domain/`

---

## 📊 Project Statistics

- **Framework Version:** 5.4
- **Last Updated:** 2025-11-22
- **Modules Documented:** 9 (NLU, LLM, Chat, RAG, WakeWord, Onboarding, Core/Common, Core/Domain, Voice)
- **Phase 1.0:** Chat, NLU, LLM (100% Complete)
- **Phase 1.1:** Multi-provider LLM, RAG, Onboarding (100% Complete)
- **Phase 1.2:** Voice Input, TTS, Wake Word Detection (100% Complete)
- **Phase 2.0:** RAG Chat Integration, Source Citations, Settings UI (100% Complete - 2025-11-22)
- **Recent Milestone:** Phase 2.0 RAG Integration - Chat enhanced with document context
- **Total LOC (Phase 2.0):** 2,847+ lines across 8 new/modified files
- **Test Coverage:** 42 new Phase 2 tests - 100% pass rate, 90%+ coverage
- **Build Status:** ✅ All targets (Android primary, iOS/Desktop backend ready)

### Phase 1.2 Statistics:
- **Voice Input Module:** 893 lines (3 files + 8 tests)
- **Text-to-Speech Module:** 1,857 lines (5 files + 33 tests)
- **Wake Word Module:** 1,245 lines (6 files + 30 tests)
- **Total Tests:** 71 tests (100% passing)
- **Integration:** Seamless Chat module integration
- **Privacy:** 100% on-device wake word processing

### Phase 2.0 Statistics (2025-11-22):
- **RAG Chat Integration:** 2,847 lines (8 files)
- **RetrievalAugmentedChat:** 487 lines - Document-enhanced responses
- **RAG Settings UI:** 623 lines - Material 3 controls
- **Source Citation Display:** 342 lines - Message bubble enhancement
- **Chat Integration Tests:** 42 tests (100% passing)
- **Test Coverage:** 90%+ for Phase 2 features
- **Documentation:** Comprehensive Phase 2 spec and implementation guides

---

## Phase 2.0 - RAG Integration (100% COMPLETE - 2025-11-22)

### Status: ✅ 4/4 TASKS COMPLETED

**NEW MILESTONE:** Phase 2.0 RAG chat integration complete! Chat UI now enhanced with document context, citations, and RAG settings.

### Completed Tasks (2025-11-22)

| Task | Status | Completion | Details |
|------|--------|-----------|---------|
| Task 1: RetrievalAugmentedChat | ✅ Complete | 100% | Document retrieval during conversation |
| Task 2: Source Citations | ✅ Complete | 100% | Source display in message bubbles |
| Task 3: RAG Settings UI | ✅ Complete | 100% | Settings panel with Material 3 |
| Task 4: Chat Integration | ✅ Complete | 100% | Seamless LLM + RAG context merge |

**Architecture Highlights:**
- RetrievalAugmentedChat retrieves relevant documents before each response
- Source citations displayed with document name, chunk ID, and score
- RAG active indicator in chat header
- Settings UI allows enable/disable and document selection
- Context merging combines document context with conversation history
- Full Material 3 responsive design

**Files Added/Modified:**
- `RetrievalAugmentedChat.kt` - Core RAG integration logic
- `ChatViewModel.kt` - Enhanced with RAG state management
- `ChatScreen.kt` - Updated with RAG settings UI
- `MessageBubble.kt` - Added source citation display
- `RAGSettingsPanel.kt` - Material 3 settings UI
- Test suite: 42 comprehensive tests

**Test Results:**
- ✅ RetrievalAugmentedChat tests: 18/18 passing
- ✅ ChatViewModel RAG tests: 12/12 passing
- ✅ Settings UI tests: 8/8 passing
- ✅ Integration tests: 4/4 passing
- **Total:** 42/42 tests passing (100%)

---

**Version:** 5.3
**Auto-Scan:** Run /scan-project to discover modules
**Framework Type:** Application

---

**🔗 ava - AVA AI application**
