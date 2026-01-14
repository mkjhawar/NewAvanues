# SpeechRecognition Path Refactoring Plan

**Created:** 2025-09-03
**Purpose:** Fix all path redundancies in SpeechRecognition module

## Current Issues

### 1. Path Redundancy
- **Current:** `/libraries/SpeechRecognition/.../speechrecognition/`
- **Issue:** "speechrecognition" repeated in path
- **Fix:** Use `/libraries/SpeechRecognition/.../voiceos/`

### 2. Multiple Engine Folders
- **Current:** Both `engines/` and `speechengines/` exist
- **Issue:** Confusing and redundant
- **Fix:** Use single `engines/` folder

### 3. Package Names
- **Current:** `com.augmentalis.speechrecognition.*`
- **Issue:** Doesn't follow voiceos standard
- **Fix:** `com.augmentalis.voiceos.*`

## Migration Plan

### Step 1: Create New Structure
```
/libraries/SpeechRecognition/src/.../voiceos/
├── api/                # API interfaces and listeners
├── common/             # Shared components (✅ DONE)
├── engines/            # All engine implementations
│   ├── vivoka/         # Vivoka components
│   ├── vosk/          # Vosk components
│   ├── android/       # Android STT components
│   ├── google/        # Google Cloud components
│   └── whisper/       # Whisper components
├── data/              # Data models
└── services/          # Service implementations
```

### Step 2: Move Files
1. ✅ Common components moved to `/voiceos/common/`
2. ⏳ API classes to `/voiceos/api/`
3. ⏳ Engines to `/voiceos/engines/[provider]/`
4. ⏳ Data models to `/voiceos/data/`
5. ⏳ Services to `/voiceos/services/`

### Step 3: Update Imports
- All `com.augmentalis.speechrecognition` → `com.augmentalis.voiceos`
- Update cross-references between components

### Step 4: Clean Up Old Structure
- Remove empty `speechrecognition` folders
- Remove redundant `speechengines` folder

## Engine Refactoring Structure

Each engine will be refactored to components:

### Vivoka Example:
```
/voiceos/engines/vivoka/
├── Vivoka.kt              # Main orchestrator (was VivokaEngine)
├── VivokaConfig.kt        # Configuration
├── VivokaState.kt         # State management
├── VivokaAudio.kt         # Audio pipeline
├── VivokaModel.kt         # Model management
├── VivokaRecognizer.kt    # Recognition processing
└── VivokaAssets.kt        # Asset management
```

### Common Components Used by ALL Engines:
```
/voiceos/common/
├── PerformanceMonitor.kt   # ✅ Created - Performance tracking
├── LearningSystem.kt       # ✅ Created - Command learning
├── AudioStateManager.kt    # ✅ Created - Audio state
├── ServiceState.kt         # ✅ Existing - Service states
├── CommandCache.kt         # ✅ Existing - Command caching
├── TimeoutManager.kt       # ✅ Existing - Timeout handling
├── ResultProcessor.kt      # ✅ Existing - Result processing
├── ErrorRecoveryManager.kt # ⏳ TODO - Error recovery
└── ConfigManager.kt        # ⏳ TODO - Config management
```

## Benefits
- **Cleaner paths:** 30-40% shorter
- **No redundancy:** Each term appears once
- **Consistent:** Follows voiceos standard
- **Maintainable:** Clear structure

## Status
- ✅ Common components moved and fixed
- ⏳ API classes need moving
- ⏳ Engines need refactoring
- ⏳ Data models need moving
- ⏳ Services need moving