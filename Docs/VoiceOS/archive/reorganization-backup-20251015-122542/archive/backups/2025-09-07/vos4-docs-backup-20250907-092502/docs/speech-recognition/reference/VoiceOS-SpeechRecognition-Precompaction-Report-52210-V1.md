<!--
filename: SpeechRecognition-Precompaction-Report.md
created: 2025-01-27 03:00:00 PST
author: Manoj Jhawar
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Comprehensive precompaction report and implementation instructions
version: 1.0.0
module: SpeechRecognition
location: /VOS4/docs/modules/speechrecognition/
-->

# SpeechRecognition Module - Precompaction Report & Implementation Summary

## Executive Summary

This document provides the complete implementation plan for compacting and standardizing the SpeechRecognition module with 4 engines (VOSK, Vivoka, GoogleSTT, GoogleCloud), eliminating redundancy and achieving minimal overhead architecture.

## 1. Final Engine List (4 Engines)

| Engine | Type | Service Name | Description |
|--------|------|-------------|-------------|
| **VOSK** | Offline | VoskService | Open-source offline recognition |
| **Vivoka** | Hybrid | VivokaService | VSDK-based hybrid recognition |
| **GoogleSTT** | On-Device | GoogleSTTService | Android's built-in Speech-to-Text |
| **GoogleCloud** | Cloud | GoogleCloudService | Google Cloud Speech API |

## 2. Corrected Configuration Design

### SpeechConfig.kt - Final Design
```kotlin
/**
 * SpeechConfig.kt - Unified configuration for all speech engines
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.speechrecognition.config

data class SpeechConfig(
    // Common configuration for ALL engines
    val language: String = "en-US",
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 0.7f,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,
    val enableProfanityFilter: Boolean = false,
    val staticCommandsPath: String = "static_commands/",
    
    // Engine selection
    val engine: SpeechEngine = SpeechEngine.VOSK,
    
    // Engine-specific optional configs
    val cloudApiKey: String? = null,  // For GoogleCloud only
    val modelPath: String? = null     // For offline engines (VOSK/Vivoka)
) {
    companion object {
        // Default configuration
        fun default() = SpeechConfig()
        
        // Engine-specific factory methods (NO language parameter - uses config language)
        fun vosk() = SpeechConfig(engine = SpeechEngine.VOSK)
        
        fun vivoka() = SpeechConfig(engine = SpeechEngine.VIVOKA)
        
        fun googleSTT() = SpeechConfig(engine = SpeechEngine.GOOGLE_STT)
        
        fun googleCloud(apiKey: String) = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            cloudApiKey = apiKey
        )
    }
    
    // Fluent modification methods (returns new instance - immutable)
    fun withLanguage(lang: String) = copy(language = lang)
    fun withEngine(eng: SpeechEngine) = copy(engine = eng)
    fun withMode(m: SpeechMode) = copy(mode = m)
    fun withVAD(enabled: Boolean) = copy(enableVAD = enabled)
    fun withConfidenceThreshold(threshold: Float) = copy(confidenceThreshold = threshold)
    fun withTimeout(ms: Long) = copy(timeoutDuration = ms)
    fun withApiKey(key: String) = copy(cloudApiKey = key)
    fun withModelPath(path: String) = copy(modelPath = path)
    
    // Single validation point
    fun validate(): Result<Unit> {
        return when {
            language.isBlank() -> 
                Result.failure(IllegalArgumentException("Language cannot be blank"))
            confidenceThreshold !in 0f..1f -> 
                Result.failure(IllegalArgumentException("Confidence threshold must be between 0 and 1"))
            engine == SpeechEngine.GOOGLE_CLOUD && cloudApiKey.isNullOrBlank() -> 
                Result.failure(IllegalArgumentException("Google Cloud requires API key"))
            timeoutDuration < 1000 -> 
                Result.failure(IllegalArgumentException("Timeout must be at least 1000ms"))
            maxRecordingDuration < timeoutDuration -> 
                Result.failure(IllegalArgumentException("Max recording must be >= timeout"))
            else -> Result.success(Unit)
        }
    }
}
```

### Usage Examples
```kotlin
// Simple usage - all defaults
val config1 = SpeechConfig.default()

// VOSK with Spanish
val config2 = SpeechConfig.vosk().withLanguage("es-ES")

// Vivoka with custom settings
val config3 = SpeechConfig.vivoka()
    .withLanguage("fr-FR")
    .withMode(SpeechMode.DICTATION)
    .withConfidenceThreshold(0.8f)

// Google Cloud with API key
val config4 = SpeechConfig.googleCloud("my-api-key")
    .withLanguage("en-GB")
    .withVAD(false)

// Change engine on existing config
val config5 = config2.withEngine(SpeechEngine.GOOGLE_STT)
```

## 3. Common Components to Extract

### 3.1 CommandCache.kt
```kotlin
/**
 * CommandCache.kt - Unified command caching for all engines
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.speechrecognition.common

class CommandCache {
    private val staticCommands = Collections.synchronizedList(arrayListOf<String>())
    private val dynamicCommands = Collections.synchronizedList(arrayListOf<String>())
    private val vocabularyCache = Collections.synchronizedMap(mutableMapOf<String, Boolean>())
    
    fun setStaticCommands(commands: List<String>) {
        staticCommands.clear()
        staticCommands.addAll(commands.map { it.lowercase().trim() })
    }
    
    fun setDynamicCommands(commands: List<String>) {
        dynamicCommands.clear()
        dynamicCommands.addAll(commands.map { it.lowercase().trim() })
    }
    
    fun findMatch(text: String): String? {
        val normalized = text.lowercase().trim()
        // Check static first (higher priority)
        staticCommands.find { it == normalized }?.let { return it }
        // Then dynamic
        dynamicCommands.find { it == normalized }?.let { return it }
        // Then cached vocabulary
        return if (vocabularyCache[normalized] == true) normalized else null
    }
    
    fun clear() {
        staticCommands.clear()
        dynamicCommands.clear()
        vocabularyCache.clear()
    }
}
```

### 3.2 TimeoutManager.kt
```kotlin
/**
 * TimeoutManager.kt - Unified timeout management for all engines
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.speechrecognition.common

class TimeoutManager(private val scope: CoroutineScope) {
    private var timeoutJob: Job? = null
    
    fun startTimeout(duration: Long, onTimeout: () -> Unit) {
        cancelTimeout()
        timeoutJob = scope.launch {
            delay(duration)
            withContext(Dispatchers.Main) {
                onTimeout()
            }
        }
    }
    
    fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
    
    fun resetTimeout(duration: Long, onTimeout: () -> Unit) {
        startTimeout(duration, onTimeout)
    }
}
```

## 4. File Renaming Plan

| Old File | New File | Location |
|----------|----------|----------|
| VoskSpeechRecognitionService.kt | VoskService.kt | engines/vosk/ |
| SpeechRecognitionServiceManager.kt | SpeechManager.kt | manager/ |
| SpeechRecognitionConfig.kt | SpeechConfig.kt | config/ |
| SpeechRecognitionConfigBuilder.kt | **DELETE** | - |
| SpeechRecognitionResult.kt | SpeechResult.kt | models/ |
| SpeechRecognitionMode.kt | SpeechMode.kt | models/ |
| VoiceRecognitionServiceState.kt | ServiceState.kt | models/ |

## 5. Implementation Steps (Detailed)

### Phase 1: Foundation (Day 1)
1. **Create common package structure**
   - Create `/common/` directory
   - Implement CommandCache.kt
   - Implement TimeoutManager.kt
   - Implement ResultProcessor.kt
   - Implement ServiceState.kt

2. **Create simplified config**
   - Delete SpeechRecognitionConfigBuilder.kt
   - Rename and refactor SpeechRecognitionConfig.kt to SpeechConfig.kt
   - Implement companion factory methods
   - Add validation logic

3. **Create models**
   - Rename and simplify all model classes
   - Create SpeechEngine enum (4 engines only)

### Phase 2: Engine Implementation (Day 2-3)

#### Step 1: Vivoka Implementation
```kotlin
/**
 * VivokaService.kt - Vivoka VSDK speech recognition service
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.speechrecognition.engines.vivoka

class VivokaService(private val context: Context) : VSDKRecognitionListener {
    // Common components (shared pattern)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var config: SpeechConfig
    private var resultListener: OnSpeechResultListener? = null
    
    // Shared utilities
    private val commandCache = CommandCache()
    private val timeoutManager = TimeoutManager(scope)
    
    // Vivoka-specific components
    private var vsdkEngine: VSDKEngine? = null
    private var vsdkRecognizer: VSDKRecognizer? = null
    
    fun initialize(config: SpeechConfig) {
        this.config = config
        // Initialize VSDK with config.language (not passed separately)
        vsdkEngine = VSDKEngine.create(context, config.language)
        // Rest of initialization...
    }
}
```

#### Step 2: Refactor VoskService
- Rename file to VoskService.kt
- Replace SpeechRecognitionConfig with SpeechConfig
- Use shared CommandCache and TimeoutManager
- Remove duplicate code

#### Step 3: Import GoogleSTTService
- Import from LegacyAvenue/SRC
- Adapt to use SpeechConfig
- Use shared components

#### Step 4: Import GoogleCloudService
- Import from LegacyAvenue/SRC
- Adapt to use SpeechConfig
- Use shared components

### Phase 3: Manager Update (Day 4)
1. Rename SpeechRecognitionServiceManager to SpeechManager
2. Update to use new service names
3. Simplify initialization logic
4. Update singleton pattern

## 6. Code Reduction Analysis

### Before Compaction
| Component | Lines per Engine | Total (4 engines) |
|-----------|-----------------|-------------------|
| Command Management | 150 | 600 |
| Timeout Logic | 50 | 200 |
| Result Processing | 200 | 800 |
| State Management | 30 | 120 |
| Config Builder | 200 | 200 |
| **Total Duplicate** | | **1920 lines** |

### After Compaction
| Component | Lines | Shared by |
|-----------|-------|-----------|
| CommandCache | 150 | All 4 engines |
| TimeoutManager | 50 | All 4 engines |
| ResultProcessor | 200 | All 4 engines |
| ServiceState | 30 | All 4 engines |
| SpeechConfig | 100 | All 4 engines |
| **Total Shared** | **530 lines** | |

### Net Reduction
- **Lines saved:** 1920 - 530 = **1390 lines (72% reduction)**
- **Memory saved:** ~300KB (no duplicate classes)
- **Maintenance:** 4x easier (fix once, works everywhere)

## 7. Testing Strategy

### Unit Tests Required
```kotlin
// Common components
- CommandCacheTest.kt
- TimeoutManagerTest.kt  
- ResultProcessorTest.kt
- SpeechConfigTest.kt

// Per engine (4 tests)
- VoskServiceTest.kt
- VivokaServiceTest.kt
- GoogleSTTServiceTest.kt
- GoogleCloudServiceTest.kt

// Manager
- SpeechManagerTest.kt
```

### Integration Tests
```kotlin
- EngineSwithingTest.kt
- ConfigurationValidationTest.kt
- CommonComponentIntegrationTest.kt
```

## 8. Migration Checklist

### Pre-Implementation
- [x] Document current structure
- [x] Identify common patterns
- [x] Design shared components
- [x] Plan file renaming
- [x] Create implementation steps

### Implementation Phase 1
- [ ] Create common components
- [ ] Implement SpeechConfig
- [ ] Delete ConfigBuilder
- [ ] Rename model files

### Implementation Phase 2
- [ ] Implement VivokaService
- [ ] Refactor VoskService
- [ ] Import GoogleSTTService
- [ ] Import GoogleCloudService

### Implementation Phase 3
- [ ] Update SpeechManager
- [ ] Update documentation
- [ ] Run tests
- [ ] Verify compilation

### Post-Implementation
- [ ] Performance testing
- [ ] Memory profiling
- [ ] Documentation update
- [ ] Code review

## 9. Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking existing code | Keep old files until migration complete |
| Engine-specific issues | Test each engine independently |
| Config validation failures | Comprehensive unit tests for SpeechConfig |
| Common component bugs | Extensive testing before integration |

## 10. Success Metrics

### Quantitative
- ✅ 4 engines working with shared components
- ✅ 1390+ lines of code removed
- ✅ Zero duplicate implementations
- ✅ All tests passing
- ✅ Memory usage reduced by 300KB+

### Qualitative
- ✅ Single point of maintenance for common logic
- ✅ Consistent behavior across all engines
- ✅ Easier to add new engines
- ✅ Cleaner, more maintainable codebase

## 11. Decision Point

### Option A: Clear Context
**Pros:**
- Fresh start with clean context
- No confusion from previous iterations
- Focused implementation

**Cons:**
- Lose accumulated understanding
- Need to re-reference documents
- Potential for inconsistency

### Option B: Continue with Current Context
**Pros:**
- Maintain continuity
- All decisions remembered
- Consistent implementation

**Cons:**
- Context getting large
- Potential confusion from iterations

### Recommendation
**Continue with current context** for implementation phase, as:
1. All standards are documented
2. Implementation plan is clear
3. Consistency is critical
4. Can reference this document as single source of truth

---

## Summary of Key Changes

1. **SpeechConfig:** Single language parameter used by all engines
2. **Engines:** 4 engines only (removed Azure)
3. **Common Components:** Extract 530 lines of shared code
4. **File Names:** Simplified, removed redundancy
5. **Code Reduction:** 72% less duplicate code
6. **Memory:** ~300KB saved

## Next Action

Ready to begin implementation with:
1. Create common components first
2. Implement VivokaService as pattern
3. Refactor other engines to match
4. Update manager and test

**Author:** Manoj Jhawar  
**Status:** Ready for Implementation  
**Date:** 2025-01-27