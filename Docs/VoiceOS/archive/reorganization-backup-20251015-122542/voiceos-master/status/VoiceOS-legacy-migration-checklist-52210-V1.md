# Legacy to VOS3 Migration Checklist

## Overview
This document provides a comprehensive checklist for migrating components from the Legacy VoiceOS codebase at `/Volumes/M Drive/Coding/Warp/LegacyAvenueRedux/voiceos/` to the VOS3 modular architecture.

**Generated:** August 20, 2025  
**Source:** `/Volumes/M Drive/Coding/Warp/LegacyAvenueRedux/voiceos/`  
**Target:** `/Volumes/M Drive/Coding/Warp/vos3-dev/`

## Migration Status Legend
- ✅ **Completed** - Fully migrated and tested
- 🔄 **In Progress** - Currently being worked on
- 📋 **Not Started** - Pending migration
- ⚠️ **Needs Review** - Requires architectural decision
- 🔧 **Partial** - Some functionality migrated

---

## 1. Speech Recognition Module

### Core Interfaces and Abstractions

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **OnSpeechRecognitionResultListener** | `/audio/OnSpeechRecognitionResultListener.kt` | `/speechrecognition/api/RecognitionResult.kt` (Flow) | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Convert callback to Flow pattern | Kotlin Coroutines, Flow |
| **SpeechRecognitionMode** | `/audio/SpeechRecognitionMode.kt` | `/speechrecognition/api/IRecognitionModule.kt` (RecognitionMode) | ✅ Completed | ✅ Completed | ✅ Compliant | ✅ Completed | ✅ Compliant | Enum values match VOS3 | None |
| **SpeechRecognitionServiceInterface** | `/audio/SpeechRecognitionServiceInterface.kt` | `/speechrecognition/engines/IRecognitionEngine.kt` | 🔄 In Progress | 🔄 In Progress | ⚠️ Needs Review | 📋 Not Started | ⚠️ Needs Review | Convert to suspend functions, Flow-based results | Kotlin Coroutines, Result types |
| **VoiceRecognitionServiceState** | `/audio/VoiceRecognitionServiceState.kt` | `/speechrecognition/engines/IRecognitionEngine.kt` (EngineState) | 🔄 In Progress | 🔄 In Progress | ✅ Compliant | 📋 Not Started | ✅ Compliant | Map state values to EngineState enum | None |

### Configuration Management

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **SpeechRecognitionConfig** | `/config/SpeechRecognitionConfig.kt` | `/speechrecognition/config/RecognitionConfig.kt` | 🔄 In Progress | 🔄 In Progress | ✅ Compliant | 📋 Not Started | ✅ Compliant | Convert Duration types, add new VOS3 fields | Kotlin Duration, VOS3 config structure |
| **SpeechRecognitionConfigBuilder** | `/config/SpeechRecognitionConfigBuilder.kt` | `/speechrecognition/config/RecognitionParameters.kt` | 📋 Not Started | 📋 Not Started | ✅ Compliant | 📋 Not Started | ✅ Compliant | Convert to data class with defaults, eliminate builder pattern | None |

### Provider Pattern

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **SpeechRecognitionServiceProvider** | `/provider/SpeechRecognitionServiceProvider.kt` | `/speechrecognition/engines/RecognitionEngineFactory.kt` | ✅ Completed | ✅ Completed | ✅ Compliant | ✅ Completed | ✅ Compliant | Factory pattern already implemented | DI framework integration |
| **SpeechRecognitionProvider (enum)** | `/provider/SpeechRecognitionServiceProvider.kt` | `/speechrecognition/api/RecognitionEngine.kt` | ✅ Completed | ✅ Completed | ✅ Compliant | ✅ Completed | ✅ Compliant | Expanded with additional engines | None |

---

## 2. Speech Recognition Engines

### Google Speech Recognition

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **GoogleSpeechRecognitionService** | `/speech/GoogleSpeechRecognitionService.kt` | `/speechrecognition/engines/implementations/GoogleCloudEngine.kt` | 🔄 In Progress | 🔄 In Progress | ⚠️ Needs Review | 📋 Not Started | ⚠️ Needs Review | • `initialize()` → suspend fun<br>• Callback listeners → Flow<br>• Grammar constraints logic<br>• Command categorization<br>• Timeout handling<br>• Mode switching<br>• State management | Kotlin Coroutines, Flow, Result types, VOS3 audio module |

**Functions to Migrate:**
- `initialize(config: SpeechRecognitionConfig?)` → `initialize(config: RecognitionConfig?): Result<Unit>`
- `startListening()` → `startRecognition(audioFlow: Flow<ByteArray>): Result<Unit>`
- `stopListening()` → `stopRecognition(): Result<Unit>`
- `setContextPhrases(phrases: List<String>)` → `setVocabulary(words: List<String>): Result<Unit>`
- `setStaticCommands(commands: List<String>)` → Integrate with vocabulary system
- `changeMode(mode: SpeechRecognitionMode)` → `setMode(mode: RecognitionMode): Result<Unit>`
- `processRecognitionResult(command: String?)` → Internal result processing
- `updateVoiceStatus()` → State flow updates
- `runTimeout()` → Timeout coroutine management

### Vivoka Speech Recognition

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **VivokaSpeechRecognitionService** | `/speech/VivokaSpeechRecognitionService.kt` | `/speechrecognition/engines/vivoka/VivokaEngineImpl.kt` | ✅ Completed | 🔄 In Progress | ⚠️ Needs Review | 📋 Not Started | ⚠️ Needs Review | • Vivoka SDK integration<br>• Model compilation<br>• Dynamic model management<br>• Pipeline management<br>• Command processing<br>• Language resource handling | Vivoka SDK, VOS3 audio module, Kotlin Coroutines |

**Functions to Migrate:**
- `initialize()` → `initialize(config: RecognitionConfig?): Result<Unit>`
- `initRecognizerListener()` → Internal engine setup
- `compileModels(commands: List<String>)` → Vocabulary management
- `processCommands(commands: List<String>)` → Command processing
- `startPipeline()` → Audio pipeline setup
- `onResult()` → Result flow emission
- `onError()` → Error handling with Result types

### VOSK Speech Recognition

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **VoskSpeechRecognitionService** | `/speech/VoskSpeechRecognitionService.kt` | `/speechrecognition/engines/implementations/VoskEngine.kt` | 🔄 In Progress | 🔄 In Progress | ⚠️ Needs Review | 📋 Not Started | ⚠️ Needs Review | • VOSK model management<br>• Grammar constraints<br>• Vocabulary caching<br>• Command learning<br>• Dual recognizer system<br>• Fallback mechanisms | VOSK SDK, VOS3 audio module, Kotlin Coroutines |

**Functions to Migrate:**
- `initialize()` → `initialize(config: RecognitionConfig?): Result<Unit>`
- `initRecognizers()` → Dual recognizer setup
- `createGrammarJson()` → Grammar constraint generation
- `categorizeCommands()` → Vocabulary analysis
- `rebuildCommandRecognizer()` → Dynamic grammar updates
- `loadLearnedCommands()` → Persistent learning system
- `saveLearnedCommand()` → Learning persistence

---

## 3. Utility Classes

### Voice Processing Utilities

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **VoiceUtils** | `/utils/VoiceUtils.kt` | `/speechrecognition/utils/SimilarityMatcher.kt` | 📋 Not Started | 📋 Not Started | ✅ Compliant | 📋 Not Started | ✅ Compliant | • `findMostSimilarWithConfidence()`<br>• `calculateConfidence()`<br>• `levenshteinDistance()` | None |
| **VoskResult** | `/utils/VoskResult.kt` | `/speechrecognition/engines/vosk/VoskResult.kt` | 📋 Not Started | 📋 Not Started | ✅ Compliant | 📋 Not Started | ✅ Compliant | Data class mapping | Gson annotations |
| **GoogleSpeechNoFilesException** | `/utils/GoogleSpeechNoFilesException.kt` | `/speechrecognition/engines/EngineError.kt` | 📋 Not Started | 📋 Not Started | ✅ Compliant | 📋 Not Started | ✅ Compliant | Convert to sealed class hierarchy | None |

---

## 4. Audio Services Integration

### Audio Capture and Processing

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **Audio Recording Logic** | Embedded in service classes | `/modules/audio/AudioModule.kt` | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Extract audio recording from speech services | Audio module, Android AudioRecord API |
| **Audio Streaming** | Manual implementation | Flow-based streaming | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Convert to Flow<ByteArray> | Kotlin Flow, Audio module |

---

## 5. Data Persistence and Caching

### Recognition Data Storage

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **Vocabulary Cache** | File-based JSON storage | `/modules/data/` entities | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Convert to ObjectBox entities | Data module, ObjectBox |
| **Learned Commands** | File-based JSON storage | `/modules/data/` entities | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Convert to ObjectBox entities | Data module, ObjectBox |
| **Recognition History** | Implicit in services | `/data/entities/RecognitionHistoryEntity.kt` | ✅ Completed | ✅ Completed | ✅ Compliant | ✅ Completed | ✅ Compliant | Already implemented | Data module |

---

## 6. Configuration and Language Management

### Language Support

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **Language Utils Integration** | External dependency | `/speechrecognition/utils/LanguageUtils.kt` | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Integrate language utilities | VOS3 localization module |
| **Dynamic Language Loading** | Firebase Remote Config | `/speechrecognition/models/ModelManager.kt` | 🔄 In Progress | 🔄 In Progress | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Model download and management | Firebase, VOS3 config system |

---

## 7. Testing Infrastructure

### Unit Tests

| Component | Legacy Path | VOS3 Target | Porting Status | Architecture Conversion | SOLID Compliance | TCR Review | Coding Standards | Functions/Classes to Migrate | Dependencies |
|-----------|-------------|-------------|----------------|------------------------|------------------|------------|------------------|----------------------------|--------------|
| **Engine Tests** | Not present | `/speechrecognition/src/test/` | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | Create comprehensive test suite | JUnit, Mockk, Coroutines Test |
| **Integration Tests** | Manual testing | Automated integration tests | 📋 Not Started | 📋 Not Started | 📋 Not Checked | 📋 Not Started | 📋 Not Checked | End-to-end recognition tests | Android Test, Espresso |

---

## Migration Priority Matrix

### High Priority (Complete First)
1. **Core Interfaces** - Foundation for all other components
2. **Configuration Management** - Required for engine initialization
3. **VOSK Engine** - Primary offline recognition engine
4. **Provider Pattern** - Engine selection and factory

### Medium Priority (Complete Second)
1. **Vivoka Engine** - Enhanced embedded recognition
2. **Google Cloud Engine** - Cloud-based recognition
3. **Utility Classes** - Supporting functionality
4. **Data Persistence** - Caching and learning systems

### Low Priority (Complete Last)
1. **Audio Integration** - Depends on Audio module completion
2. **Testing Infrastructure** - After core functionality
3. **Advanced Features** - Wake word, continuous recognition

---

## Key Architectural Changes

### From Legacy to VOS3

| Aspect | Legacy Approach | VOS3 Approach | Migration Notes |
|--------|----------------|---------------|-----------------|
| **Callbacks** | Interface-based callbacks | Kotlin Flow/suspend functions | Convert all listeners to Flow emissions |
| **Error Handling** | Exception throwing | Result<T> return types | Wrap all operations in Result types |
| **Threading** | Manual coroutine management | Structured concurrency | Use proper coroutine scopes |
| **Configuration** | Mutable objects | Immutable data classes | Convert builders to data classes with defaults |
| **State Management** | Manual state variables | StateFlow/Flow patterns | Use reactive state management |
| **Dependency Injection** | Manual instantiation | DI framework integration | Integrate with VOS3 DI system |
| **Testing** | Limited unit tests | Comprehensive test coverage | Add unit, integration, and UI tests |

---

## Dependencies to Address

### External Dependencies
- **Vivoka SDK** - Ensure compatibility with VOS3 architecture
- **VOSK SDK** - Update to latest version if needed
- **Google Cloud Speech** - Migrate to newer API versions
- **Firebase Remote Config** - Integrate with VOS3 config system

### Internal Dependencies
- **Audio Module** - Required for audio streaming
- **Data Module** - Required for persistence
- **Core Module** - Required for module interface
- **Localization Module** - Required for language support

---

## Success Criteria

### Functional Requirements
- [ ] All legacy speech recognition functionality preserved
- [ ] New VOS3 features integrated (Flow-based APIs, Result types)
- [ ] Performance maintained or improved
- [ ] All engines working with new architecture

### Technical Requirements
- [ ] SOLID principles compliance
- [ ] Comprehensive test coverage (>80%)
- [ ] Documentation complete
- [ ] Code review (TCR) completed
- [ ] Coding standards compliance

### Integration Requirements
- [ ] Works with VOS3 module system
- [ ] Integrates with Audio module
- [ ] Integrates with Data module
- [ ] Proper dependency injection

---

## Next Steps

1. **Complete Core Interfaces** - Finish IRecognitionEngine implementation
2. **Migrate Configuration** - Complete RecognitionConfig conversion
3. **Implement VOSK Engine** - Port grammar constraints and caching
4. **Add Comprehensive Testing** - Unit and integration tests
5. **Integration Testing** - Test with full VOS3 system
6. **Performance Optimization** - Ensure performance parity
7. **Documentation** - Complete API documentation
8. **Code Review** - TCR process for all migrated code

---

**Last Updated:** August 20, 2025  
**Next Review:** Weekly progress review  
**Migration Lead:** Development Team  
**Estimated Completion:** Based on priority matrix and resource allocation