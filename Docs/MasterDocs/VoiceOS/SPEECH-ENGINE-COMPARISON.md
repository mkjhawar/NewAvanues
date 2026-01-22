# Speech Recognition Engine Comparison

**Version:** 1.1
**Date:** January 2026
**Updated:** 2026-01-18 - Added KMP CommandMatchingService integration status
**Reference Implementation:** Vivoka SDK

---

## Recent Update: Unified Command Matching (COMPLETED)

A unified `CommandMatchingService` has been implemented and integrated with all engines via the KMP `ResultProcessor`. This provides:

| Feature | Status | Location |
|---------|--------|----------|
| **Fuzzy Matching (Levenshtein)** | ✅ Complete | `NLU/matching/CommandMatchingService.kt` |
| **Word Overlap (Jaccard)** | ✅ Complete | `NLU/matching/CommandMatchingService.kt` |
| **Semantic Matching** | ✅ Complete | `NLU/matching/CommandMatchingService.kt` |
| **Synonym Expansion** | ✅ Complete | `NLU/matching/CommandMatchingService.kt` |
| **User Correction Learning** | ✅ Complete | `learn(misrecognized, correct)` API |
| **Multilingual Normalization** | ✅ Complete | `MultilingualNormalizer` |
| **ResultProcessor Integration** | ✅ Complete | `SpeechRecognition/commonMain/ResultProcessor.kt` |

**Architecture:**
```
Any Speech Engine (Whisper/Vosk/etc) → raw text
         ↓
ResultProcessor → exact match attempt
         ↓ (fallback if no exact match)
CommandMatchingService → 6-stage cascade
         ↓
1. Learned (user corrections)
2. Exact (direct match)
3. Synonym (expansion + exact)
4. Levenshtein (edit distance)
5. Jaccard (word overlap)
6. Semantic (embeddings)
         ↓
Matched command or NoMatch
```

This means **ALL engines** now have "pseudo-command mode" via post-processing.

---

## Engine Status Summary

| Engine | Location | Status | Lines of Code |
|--------|----------|--------|---------------|
| **Vivoka** | `SpeechRecognition/.../vivoka/` | Complete (Reference) | ~1,059+ |
| **Whisper** | `SpeechRecognition/.../whisper/` | Complete | ~800+ |
| **Vosk** | `SpeechRecognition/.../vosk/` | Complete | ~600+ |
| **Android STT** | `SpeechRecognition/.../android/` | Complete | ~797+ |
| **Google Cloud** | `SpeechRecognition/.../google/` | Partial (Disabled) | ~400+ |
| **Azure** | `VoiceOSCore/.../AzureEngineAdapter.kt` | Complete | ~300+ |
| **Apple Speech** | `Voice/Core/.../AppleSpeechEngine.kt` | Stub Only | ~100 |

---

## Core Feature Comparison Matrix

| Feature | Vivoka | Whisper | Vosk | Android STT | Google Cloud | Azure | Apple |
|---------|:------:|:-------:|:----:|:-----------:|:------------:|:-----:|:-----:|
| **RECOGNITION** ||||||||
| Streaming Recognition | YES | YES | YES | YES | YES | YES | NO |
| Offline Support | YES | YES | YES | YES | NO | NO | NO |
| Command Mode (Grammar) | NATIVE | POST* | NATIVE | NATIVE | NATIVE | NATIVE | NO |
| Dictation Mode | YES | YES | YES | YES | YES | YES | NO |
| Hybrid Mode | YES | YES* | PARTIAL | PARTIAL | NO | PARTIAL | NO |
| **LANGUAGE** ||||||||
| Multi-Language | 50+ | 99 | 32 | 100+ | 125+ | 147+ | NO |
| Dynamic Language Download | YES | YES | PARTIAL | PARTIAL | N/A | N/A | NO |
| Language Auto-Detection | PARTIAL | YES | NO | NO | YES | YES | NO |
| Model Merging | YES | NO | NO | NO | N/A | N/A | NO |
| **ADVANCED** ||||||||
| Wake Word Detection | YES | NO | NO | NO | NO | NO | NO |
| Dynamic Command Vocab | YES | YES* | YES | YES | YES | YES | NO |
| Word-Level Timestamps | PARTIAL | YES | PARTIAL | NO | YES | YES | NO |
| Translation | NO | YES | NO | NO | YES | YES | NO |
| Speaker Diarization | NO | NO | NO | NO | YES | YES | NO |
| **INTEGRATION** ||||||||
| Learning System | YES | YES* | YES* | YES | YES* | YES* | NO |
| Fuzzy Matching | YES | YES* | YES* | YES* | YES* | YES* | NO |
| Performance Monitoring | YES | PARTIAL | PARTIAL | YES | PARTIAL | PARTIAL | NO |
| State Management | YES | YES | YES | YES | PARTIAL | PARTIAL | PARTIAL |
| Error Recovery | YES | YES | YES | YES | PARTIAL | PARTIAL | NO |
| **ARCHITECTURE** ||||||||
| Two-Phase Init | YES | PARTIAL | NO | NO | NO | NO | NO |
| SOLID Components | YES | PARTIAL | PARTIAL | YES | PARTIAL | PARTIAL | NO |
| Common Components | YES | YES* | YES* | YES | NO | NO | NO |
| Config Management | YES | YES | YES | YES | YES | PARTIAL | NO |

**Legend:**
- NATIVE = Engine has native grammar/command constraint support
- POST* = Via CommandMatchingService post-processing (fuzzy matching)
- YES* = Newly implemented via KMP CommandMatchingService integration

---

## Gap Analysis By Engine

### 1. Whisper (Feature Complete via Post-Processing)

| Gap | Priority | Effort | Status | Description |
|-----|----------|--------|--------|-------------|
| Command Mode (Grammar) | - | - | ✅ DONE | Via CommandMatchingService post-processing |
| Learning System Integration | - | - | ✅ DONE | Via CommandMatchingService.learn() API |
| Common Components | - | - | ✅ DONE | KMP ResultProcessor + CommandCache |
| Fuzzy Matching | - | - | ✅ DONE | Levenshtein + Jaccard + Semantic |
| Wake Word Detection | Medium | High | ⬜ TODO | Add wake word via separate detector |
| Two-Phase Init | Low | Medium | ⬜ TODO | Add proper init manager |

### 2. Vosk (Feature Complete via Post-Processing)

| Gap | Priority | Effort | Status | Description |
|-----|----------|--------|--------|-------------|
| Learning System Integration | - | - | ✅ DONE | Via CommandMatchingService.learn() API |
| Common Components | - | - | ✅ DONE | KMP ResultProcessor + CommandCache |
| Fuzzy Matching | - | - | ✅ DONE | Via CommandMatchingService |
| Language Auto-Detection | Low | High | ⬜ TODO | Not natively supported |
| Dynamic Language Download | Medium | Medium | ⬜ TODO | Add download manager like Whisper |
| Performance Monitoring | Low | Low | ⬜ TODO | Add PerformanceMonitor usage |

### 3. Android STT (Reference Quality)

| Gap | Priority | Effort | Status | Description |
|-----|----------|--------|--------|-------------|
| Fuzzy Matching | - | - | ✅ DONE | Via CommandMatchingService |
| Wake Word Detection | Medium | High | ⬜ TODO | Add wake word detector |
| Word-Level Timestamps | Low | Medium | ⬜ N/A | Not supported by API |
| Translation | Low | N/A | ⬜ N/A | Not supported |

### 4. Google Cloud (Disabled - Needs Activation)

| Gap | Priority | Effort | Status | Description |
|-----|----------|--------|--------|-------------|
| Re-Enable Engine | High | Low | ⬜ TODO | Remove fallback, test REST API |
| Learning System Integration | - | - | ✅ DONE | Via CommandMatchingService (when enabled) |
| Common Components | - | - | ✅ DONE | Can use KMP ResultProcessor (when enabled) |
| Two-Phase Init | Low | Medium | ⬜ TODO | Add init manager |
| Wake Word Detection | Low | High | ⬜ TODO | Add separate detector |

### 5. Azure (Complete - Needs Alignment)

| Gap | Priority | Effort | Status | Description |
|-----|----------|--------|--------|-------------|
| Learning System Integration | - | - | ✅ DONE | Via CommandMatchingService |
| Common Components | Medium | Medium | ⬜ TODO | Migrate to use KMP ResultProcessor |
| Performance Monitoring | Low | Low | ⬜ TODO | Add PerformanceMonitor |
| SOLID Refactoring | Medium | High | ⬜ TODO | Split into components like Vivoka |

### 6. Apple Speech (Stub - Needs Full Implementation)

| Gap | Priority | Effort | Status | Description |
|-----|----------|--------|--------|-------------|
| **Full Implementation** | **Critical** | **Very High** | ⬜ TODO | Complete iOS implementation |
| Streaming Recognition | Critical | High | ⬜ TODO | Implement SFSpeechRecognizer |
| Offline Support | High | Medium | ⬜ TODO | Use on-device recognition |
| Command/Dictation Modes | High | Medium | ⬜ TODO | Add mode switching |
| Learning/Fuzzy | - | - | ✅ READY | Will use CommandMatchingService when implemented |

---

## Vivoka Cleanup Opportunities

Since Vivoka is the complete reference, here are potential cleanup/enhancement areas:

| Area | Type | Description |
|------|------|-------------|
| `VivokaEngine.kt` | Cleanup | 1,059 lines - could extract more to components |
| Firebase Dependency | Enhancement | Abstract Firebase for testability |
| Model Merging | Enhancement | Document merging algorithm |
| Wake Word | Enhancement | Make wake word configurable |
| Translation | Feature Gap | Add translation support (Whisper has it) |
| Speaker Diarization | Feature Gap | Consider adding (Azure/Google have it) |
| Word Timestamps | Enhancement | Improve timestamp accuracy |

---

## Priority Action Plan (Updated)

### Completed ✅

| Engine | Action | Status |
|--------|--------|--------|
| All | Learning System via CommandMatchingService | ✅ Done |
| Whisper | Pseudo-command mode via fuzzy matching | ✅ Done |
| Vosk/Whisper | Use common KMP components | ✅ Done |
| All | Fuzzy matching (Levenshtein/Jaccard/Semantic) | ✅ Done |

### Remaining Tasks

| Priority | Engine | Action | Effort |
|----------|--------|--------|--------|
| **1** | Apple Speech | Complete iOS implementation | Very High |
| **2** | Google Cloud | Re-enable and test REST API | Low |
| **3** | Azure | Migrate to KMP ResultProcessor | Medium |
| **4** | Azure | SOLID refactoring to match Vivoka | High |
| **5** | All | Add wake word detection framework | High |
| **6** | Vosk | Add dynamic language download | Medium |

---

## File Locations

### NEW: KMP SpeechRecognition Module (Cross-Platform)
```
Modules/SpeechRecognition/src/
├── commonMain/kotlin/.../speechrecognition/    # ★ NEW: Shared KMP components
│   ├── SpeechRecognitionService.kt             # Core service interface
│   ├── ResultProcessor.kt                      # ★ Integrates CommandMatchingService
│   ├── CommandCache.kt                         # Unified command storage
│   ├── SpeechEngine.kt                         # Engine interface
│   ├── SpeechMode.kt                           # Recognition modes
│   ├── RecognitionResult.kt                    # Result data class
│   ├── ServiceState.kt                         # State machine
│   └── SpeechListeners.kt                      # Callback interfaces
├── androidMain/kotlin/.../
│   └── AndroidSpeechRecognitionService.kt      # Android platform impl
├── iosMain/kotlin/.../
│   └── IosSpeechRecognitionService.kt          # iOS platform impl
├── desktopMain/kotlin/.../
│   └── DesktopSpeechRecognitionService.kt      # Desktop platform impl
└── jsMain/kotlin/.../
    └── JsSpeechRecognitionService.kt           # Web platform impl
```

### NEW: NLU Command Matching Service
```
Modules/AI/NLU/src/commonMain/kotlin/.../
├── matching/                                    # ★ NEW: Unified matching
│   ├── CommandMatchingService.kt               # 6-stage cascade matching
│   ├── MatchResult.kt                          # Exact/Fuzzy/Ambiguous/NoMatch
│   ├── MatchingConfig.kt                       # Thresholds & weights
│   ├── MultilingualNormalizer.kt               # Locale-aware normalization
│   ├── LocalizedSynonymProvider.kt             # Language-specific synonyms
│   └── LanguageDetector.kt                     # Auto locale detection
├── matcher/
│   ├── PatternMatcher.kt                       # Exact/regex matching
│   ├── FuzzyMatcher.kt                         # Levenshtein distance
│   └── SemanticMatcher.kt                      # Embedding similarity
```

### SpeechRecognition Module (Android Engine Implementations)
```
Modules/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/
├── vivoka/          # Reference implementation
│   ├── VivokaEngine.kt
│   ├── VivokaInitializationManager.kt
│   ├── VivokaRecognizer.kt
│   ├── VivokaModel.kt
│   ├── VivokaAudio.kt
│   ├── VivokaConfig.kt
│   ├── VivokaState.kt
│   ├── VivokaPerformance.kt
│   ├── VivokaLearning.kt
│   └── VivokaErrorMapper.kt
├── whisper/
│   ├── WhisperEngine.kt
│   ├── WhisperAndroid.kt
│   ├── WhisperNative.kt
│   ├── WhisperConfig.kt
│   ├── WhisperModelManager.kt
│   ├── WhisperProcessor.kt
│   └── WhisperErrorHandler.kt
├── vosk/
│   ├── VoskEngine.kt
│   ├── VoskRecognizer.kt
│   ├── VoskModel.kt
│   ├── VoskConfig.kt
│   ├── VoskGrammar.kt
│   ├── VoskErrorHandler.kt
│   └── VoskState.kt
├── android/
│   ├── AndroidSTTEngine.kt
│   ├── AndroidConfig.kt
│   ├── AndroidLanguage.kt
│   ├── AndroidIntent.kt
│   ├── AndroidListener.kt
│   ├── AndroidRecognizer.kt
│   └── AndroidErrorHandler.kt
├── google/
│   ├── GoogleConfig.kt
│   ├── GoogleAuth.kt
│   ├── GoogleNetwork.kt
│   ├── GoogleStreaming.kt
│   └── GoogleErrorHandler.kt
└── common/          # Legacy Android-only components (being deprecated)
    ├── ServiceState.kt
    ├── CommandCache.kt
    ├── TimeoutManager.kt
    ├── ResultProcessor.kt
    ├── PerformanceMonitor.kt
    ├── ErrorRecoveryManager.kt
    ├── UniversalInitializationManager.kt
    ├── SdkInitializationManager.kt
    ├── VoiceStateManager.kt
    ├── LearningSystem.kt           # STUB - replaced by CommandMatchingService
    └── AudioStateManager.kt
```

### VoiceOSCore KMP Abstraction Layer
```
Modules/VoiceOSCore/src/
├── commonMain/kotlin/.../
│   ├── ISpeechEngine.kt
│   ├── SpeechEngineManager.kt
│   ├── ISpeechEngineFactory.kt
│   ├── SpeechConfig.kt
│   └── SpeechMode.kt
├── androidMain/kotlin/.../
│   └── VivokaEngineFactory.android.kt
├── iosMain/kotlin/.../
│   └── AppleSpeechEngine.kt
└── desktopMain/kotlin/.../
    └── (stubs)
```

### Azure Implementation
```
Modules/VoiceOS/VoiceOSCore/src/main/java/.../
└── AzureEngineAdapter.kt

Modules/Voice/Core/src/androidMain/kotlin/.../
└── AzureEngineImpl.kt
```

---

## Memory & Performance Comparison

| Engine | Memory | Startup Time | Recognition Latency | Battery Impact |
|--------|--------|--------------|---------------------|----------------|
| **Vivoka** | ~60MB | 2-3s | <100ms | Medium |
| **Whisper (Tiny)** | ~39MB | 1-2s | ~2s/30s audio | High |
| **Whisper (Base)** | ~74MB | 2-3s | ~1s/30s audio | High |
| **Vosk** | ~30MB | 1-2s | <100ms | Low |
| **Android STT** | ~20MB | <1s | <100ms | Low |
| **Google Cloud** | ~15MB | <1s | 200-500ms | Low (network) |
| **Azure** | Variable | <1s | 200-500ms | Low (network) |
| **Apple Speech** | Variable | <1s | <100ms | Low |

---

## Recommendations

### Completed ✅
1. ~~Add Learning System to Whisper and Vosk~~ → Done via CommandMatchingService
2. ~~Standardize common component usage~~ → Done via KMP SpeechRecognition module
3. ~~Add grammar/command mode to Whisper~~ → Done via fuzzy matching post-processing

### Short-Term (1-2 Sprints)
1. Re-enable Google Cloud REST API
2. Migrate Azure to use KMP ResultProcessor
3. Add comprehensive tests for CommandMatchingService

### Medium-Term (3-4 Sprints)
1. SOLID refactor Azure engine
2. Add wake word detection framework
3. Improve semantic matching with better embeddings

### Long-Term (5+ Sprints)
1. Complete Apple Speech iOS implementation
2. Add translation to Vivoka
3. Implement speaker diarization where supported
4. Add phoneme-based matching for better typo tolerance

---

## Usage Example

```kotlin
// Create CommandMatchingService
val matchingService = CommandMatchingService()
matchingService.registerCommands(listOf(
    "open calculator",
    "open camera",
    "go back",
    "scroll down"
))
matchingService.setSynonyms(mapOf(
    "tap" to "click",
    "press" to "click"
))

// Create ResultProcessor with fuzzy matching
val resultProcessor = ResultProcessor(
    commandCache = commandCache,
    commandMatcher = matchingService
)

// Process speech recognition result
val result = resultProcessor.processResult(
    text = "opn calculater",  // Typo in input
    confidence = 0.9f,
    engine = SpeechEngine.WHISPER
)
// Returns: "open calculator" (via Levenshtein fuzzy match)

// Learn from user correction
matchingService.learn("calculater", "open calculator")
// Next time "calculater" is instant exact match
```

---

*Document generated from codebase analysis - January 2026*
*Updated: 2026-01-18 - Added CommandMatchingService integration status*
