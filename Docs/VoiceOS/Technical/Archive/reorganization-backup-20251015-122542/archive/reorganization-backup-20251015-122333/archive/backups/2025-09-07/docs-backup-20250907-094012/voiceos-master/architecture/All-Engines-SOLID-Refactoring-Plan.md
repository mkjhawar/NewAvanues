# All Speech Engines SOLID Refactoring Plan

**Created:** 2025-09-03
**Purpose:** Refactor ALL speech engines using same SOLID methodology

## Summary
Refactor all 5 monolithic speech engines (8,186 total lines) into SOLID-compliant component-based architecture using shared common components.

## Engines to Refactor

| Engine | Current Lines | Target Components | Status |
|--------|--------------|-------------------|---------|
| VivokaEngine | 2,414 | 10 components | ⏳ In Progress |
| VoskEngine | 1,823 | 8 components | ⏳ Pending |
| GoogleCloudEngine | 1,687 | 7 components | ⏳ Pending |
| AndroidSTTEngine | 1,452 | 7 components | ⏳ Pending |
| WhisperEngine | 810 | 6 components | ⏳ Pending |

## Shared Common Components (ALL Engines Use These)

### Already Created:
1. **PerformanceMonitor** - Performance tracking & bottleneck detection
2. **LearningSystem** - Command learning & vocabulary caching
3. **AudioStateManager** - Audio state & mode management
4. **ServiceState** - Service state transitions
5. **CommandCache** - Command caching & matching
6. **TimeoutManager** - Timeout handling
7. **ResultProcessor** - Result normalization

### To Create:
8. **ErrorRecoveryManager** - Error handling & recovery strategies
9. **ConfigManager** - Configuration validation & management

## Refactoring Pattern for Each Engine

### 1. VivokaEngine → Vivoka Components
```
/voiceos/engines/vivoka/
├── Vivoka.kt              # Main orchestrator (200 lines)
├── VivokaConfig.kt        # VSDK configuration (150 lines)
├── VivokaState.kt         # State management (120 lines)
├── VivokaAudio.kt         # Audio pipeline (250 lines)
├── VivokaModel.kt         # Model management (300 lines)
├── VivokaRecognizer.kt    # Recognition processing (200 lines)
└── VivokaAssets.kt        # Asset extraction (250 lines)
```
**Uses Common:** PerformanceMonitor, LearningSystem, AudioStateManager, ServiceState, CommandCache, TimeoutManager, ResultProcessor

### 2. VoskEngine → Vosk Components
```
/voiceos/engines/vosk/
├── Vosk.kt                # Main orchestrator (200 lines)
├── VoskConfig.kt          # Configuration (150 lines)
├── VoskModel.kt           # Model unpacking (350 lines)
├── VoskRecognizer.kt      # Recognition (250 lines)
├── VoskGrammar.kt         # Grammar generation (200 lines)
└── VoskStorage.kt         # Storage service (150 lines)
```
**Uses Common:** PerformanceMonitor, LearningSystem, AudioStateManager, ServiceState, CommandCache, TimeoutManager, ResultProcessor

### 3. GoogleCloudEngine → GoogleCloud Components
```
/voiceos/engines/google/
├── GoogleCloud.kt         # Main orchestrator (200 lines)
├── GoogleConfig.kt        # API configuration (150 lines)
├── GoogleAuth.kt          # Authentication (200 lines)
├── GoogleStreaming.kt     # Streaming recognition (300 lines)
├── GoogleTranscript.kt    # Transcript processing (200 lines)
└── GoogleNetwork.kt       # Network handling (150 lines)
```
**Uses Common:** PerformanceMonitor, LearningSystem, AudioStateManager, ServiceState, ResultProcessor, ErrorRecoveryManager

### 4. AndroidSTTEngine → AndroidSTT Components
```
/voiceos/engines/android/
├── AndroidSTT.kt          # Main orchestrator (200 lines)
├── AndroidConfig.kt       # Configuration (150 lines)
├── AndroidRecognizer.kt   # SpeechRecognizer wrapper (250 lines)
├── AndroidListener.kt     # RecognitionListener (200 lines)
├── AndroidIntent.kt       # Intent management (150 lines)
└── AndroidLanguage.kt     # Language mapping (100 lines)
```
**Uses Common:** PerformanceMonitor, AudioStateManager, ServiceState, CommandCache, ResultProcessor

### 5. WhisperEngine → Whisper Components
```
/voiceos/engines/whisper/
├── Whisper.kt             # Main orchestrator (150 lines)
├── WhisperModel.kt        # Model management (200 lines)
├── WhisperNative.kt       # Native integration (150 lines)
├── WhisperProcessor.kt    # Audio processing (150 lines)
└── WhisperConfig.kt       # Configuration (100 lines)
```
**Uses Common:** PerformanceMonitor, AudioStateManager, ServiceState, ResultProcessor

## Component Responsibilities

### Common Pattern for All Engines:

1. **Main Orchestrator** (e.g., Vivoka.kt)
   - Coordinates all components
   - Implements SpeechEngine interface
   - Manages lifecycle
   - Delegates to specialized components

2. **Config Component** (e.g., VivokaConfig.kt)
   - Loads configuration
   - Validates settings
   - Language mapping
   - Uses common ConfigManager

3. **Model/Recognizer Components**
   - Engine-specific recognition logic
   - Model loading/compilation
   - Result processing
   - Uses common ResultProcessor

4. **State Management**
   - Uses common ServiceState
   - Uses common AudioStateManager
   - Engine-specific states if needed

5. **Performance & Learning**
   - Uses common PerformanceMonitor
   - Uses common LearningSystem
   - No duplication needed

## Migration Strategy

### Phase 1: Common Components (✅ DONE)
- Created PerformanceMonitor
- Created LearningSystem
- Created AudioStateManager
- Existing common components ready

### Phase 2: Vivoka Refactoring (⏳ IN PROGRESS)
1. Create component files in `/voiceos/engines/vivoka/`
2. Extract code from monolithic VivokaEngine
3. Wire up common components
4. Test thoroughly

### Phase 3: Vosk Refactoring
1. Create component files in `/voiceos/engines/vosk/`
2. Extract code from monolithic VoskEngine
3. Wire up common components
4. Test thoroughly

### Phase 4: GoogleCloud Refactoring
1. Create component files in `/voiceos/engines/google/`
2. Extract code from monolithic GoogleCloudEngine
3. Wire up common components
4. Test thoroughly

### Phase 5: AndroidSTT Refactoring
1. Create component files in `/voiceos/engines/android/`
2. Extract code from monolithic AndroidSTTEngine
3. Wire up common components
4. Test thoroughly

### Phase 6: Whisper Refactoring
1. Create component files in `/voiceos/engines/whisper/`
2. Extract code from monolithic WhisperEngine
3. Wire up common components
4. Test thoroughly

## Benefits After Refactoring

### Code Reduction:
- **Before:** 8,186 lines across 5 engines
- **After:** ~4,000 lines (50% reduction)
- **Shared:** ~1,000 lines in common components

### Quality Improvements:
- Single Responsibility: Each component has one job
- Open/Closed: Easy to extend without modifying
- Testability: Each component independently testable
- Maintainability: 5x improvement
- Bug fixes: Fix once in common, all engines benefit

### Performance:
- Reduced memory footprint
- Faster initialization
- Better garbage collection
- Consistent monitoring across all engines

## Success Criteria
1. ✅ 100% functional equivalency maintained
2. ✅ All 92+ methods preserved (distributed across components)
3. ✅ No functionality lost
4. ✅ Performance improved or same
5. ✅ All engines use common components
6. ✅ Code duplication eliminated
7. ✅ Each component < 400 lines
8. ✅ SOLID principles followed

## Testing Strategy
1. Unit tests for each component
2. Integration tests for each engine
3. Performance benchmarks
4. Memory profiling
5. Cross-engine consistency tests

---
**Status:** Phase 1 Complete, Phase 2 In Progress