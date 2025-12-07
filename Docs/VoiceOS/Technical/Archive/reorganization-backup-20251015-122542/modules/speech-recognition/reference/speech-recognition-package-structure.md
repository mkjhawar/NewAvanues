# SpeechRecognition Package Structure

**Created:** 2025-09-03
**Purpose:** Define clean package structure with domain-specific commons

## Package Structure Philosophy

Each domain has its own `common` folder for shared components within that domain. This prevents:
- Unnecessary coupling between unrelated components
- Overly generic "utility" classes
- Confusion about where shared code belongs

## Complete Package Structure

```
libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/
├── common/                          # Minimal - only truly universal constants
│   └── Constants.kt                 # Global constants for entire speech module
│
├── engines/
│   ├── common/                      # Shared by ALL engines
│   │   ├── PerformanceMonitor.kt    # Performance tracking (all engines use)
│   │   ├── LearningSystem.kt        # Command learning (all engines use)
│   │   ├── AudioStateManager.kt     # Audio state management (all engines use)
│   │   ├── ErrorRecoveryManager.kt  # Error recovery (all engines use)
│   │   ├── ServiceState.kt          # Service state tracking (all engines use)
│   │   ├── CommandCache.kt          # Command caching (all engines use)
│   │   ├── TimeoutManager.kt        # Timeout handling (all engines use)
│   │   └── ResultProcessor.kt       # Result processing (all engines use)
│   │
│   ├── vivoka/                      # Vivoka-specific components
│   │   ├── VivokaEngine.kt          # Main orchestrator
│   │   ├── VivokaConfig.kt          # Vivoka configuration
│   │   ├── VivokaState.kt           # State management
│   │   ├── VivokaAudio.kt           # Audio pipeline
│   │   ├── VivokaModel.kt           # Model management
│   │   ├── VivokaRecognizer.kt      # Recognition processing
│   │   └── VivokaAssets.kt          # Asset management
│   │
│   ├── vosk/                        # Vosk-specific components
│   │   ├── VoskEngine.kt            # Main orchestrator
│   │   ├── VoskConfig.kt            # Configuration
│   │   ├── VoskModel.kt             # Model unpacking
│   │   ├── VoskRecognizer.kt        # Recognition
│   │   ├── VoskGrammar.kt           # Grammar generation
│   │   └── VoskStorage.kt           # Storage service
│   │
│   ├── google/                      # Google Cloud Speech components
│   │   ├── GoogleCloudEngine.kt     # Main orchestrator
│   │   ├── GoogleConfig.kt          # API configuration
│   │   ├── GoogleAuth.kt            # Authentication
│   │   ├── GoogleStreaming.kt       # Streaming recognition
│   │   ├── GoogleTranscript.kt      # Transcript processing
│   │   └── GoogleNetwork.kt         # Network handling
│   │
│   ├── android/                     # Android STT components
│   │   ├── AndroidSTTEngine.kt      # Main orchestrator
│   │   ├── AndroidConfig.kt         # Configuration
│   │   ├── AndroidRecognizer.kt     # SpeechRecognizer wrapper
│   │   ├── AndroidListener.kt       # RecognitionListener
│   │   ├── AndroidIntent.kt         # Intent management
│   │   └── AndroidLanguage.kt       # Language mapping
│   │
│   └── whisper/                     # Whisper components
│       ├── WhisperEngine.kt         # Main orchestrator
│       ├── WhisperModel.kt          # Model management
│       ├── WhisperNative.kt         # Native integration (whisper-cpp)
│       ├── WhisperProcessor.kt      # Audio processing
│       └── WhisperConfig.kt         # Configuration
│
├── api/
│   ├── common/                      # Shared API utilities
│   │   └── BaseListener.kt          # Base listener implementations
│   ├── SpeechListeners.kt           # All listener interfaces
│   └── RecognitionResult.kt         # Result data class
│
└── data/
    ├── common/                      # Shared data utilities
    │   └── DataValidation.kt        # Data validation utilities
    ├── SpeechConfig.kt              # Configuration data
    └── Models.kt                    # Data models
```

## Package Naming Convention

```kotlin
// Engine common components
package com.augmentalis.voiceos.speech.engines.common

// Specific engine
package com.augmentalis.voiceos.speech.engines.vivoka

// API common
package com.augmentalis.voiceos.speech.api.common

// Data common
package com.augmentalis.voiceos.speech.data.common
```

## Benefits of Domain-Specific Commons

1. **Clear Ownership**: Each domain owns its common components
2. **Reduced Coupling**: Engines don't depend on API commons, etc.
3. **Better Organization**: Easy to find shared code for a specific domain
4. **Scalability**: Can add new domains without affecting others
5. **Testability**: Domain-specific test utilities stay with their domain

## Migration from Old Structure

### Before (problematic):
```
/speechrecognition/common/           # Everything mixed together
/speechrecognition/engines/vivoka/   # Path redundancy
/speechrecognition/speechengines/    # Confusing duplication
```

### After (clean):
```
/voiceos/speech/engines/common/      # Engine-specific shared code
/voiceos/speech/engines/vivoka/      # Clean, no redundancy
/voiceos/speech/api/common/          # API-specific shared code
```

## Usage Examples

### Engine using common components:
```kotlin
package com.augmentalis.voiceos.speech.engines.vivoka

import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
import com.augmentalis.voiceos.speech.engines.common.LearningSystem
import com.augmentalis.voiceos.speech.engines.common.AudioStateManager

class VivokaEngine {
    private val performanceMonitor = PerformanceMonitor("Vivoka")
    private val learningSystem = LearningSystem("Vivoka", context)
    private val audioStateManager = AudioStateManager("Vivoka")
    // ...
}
```

### API using its own commons:
```kotlin
package com.augmentalis.voiceos.speech.api

import com.augmentalis.voiceos.speech.api.common.BaseListener

interface OnSpeechResultListener : BaseListener {
    // ...
}
```

## Rules for Common Components

1. **Engine Common**: Only components used by 2+ engines
2. **API Common**: Only base interfaces/utilities for API layer
3. **Data Common**: Only data validation/transformation utilities
4. **Module Common**: Only true constants that span entire module

If something is only used by one engine, it stays in that engine's package.

---
**Status:** Structure defined, migration in progress