# VoiceOS & AI Features Integration Guide

**Purpose**: Instructions for integrating VoiceOS library and AI-related features into AVA AI
**Source**: AVA-VoiceOS-Avanue codebase (Apr 2025, v1.5.1)
**Target**: AVA AI (Week 7-8 integration)
**Priority**: P0 (High Priority)

---

## Overview

VoiceOS is a production-ready multi-provider speech recognition library with:
- **Providers**: Google Speech, Vivoka SDK v5, Vosk (offline), Whisper (planned)
- **Architecture**: Clean layered design with Factory, Adapter, Template Method patterns
- **Performance**: Memory-optimized, battery-aware, adaptive provider selection
- **Alignment**: Privacy-first (offline support), perfect fit for AVA AI

---

## Source Files Location

**Base Path**: `/Users/manoj_mbpm14/Downloads/Coding/AVA-VoiceOS-Avanue/AVA/voiceos/`

### 1. Core API Layer

**Factory & Provider Management:**
```
src/main/java/com/augmentalis/voiceos/
├── api/
│   ├── VoiceOSFactory.kt                    # Main factory for service creation
│   └── VoiceOSFactoryV2.kt                  # Enhanced factory (use this)
├── provider/
│   ├── VoiceRecognitionProviderFactory.kt   # Provider instantiation
│   ├── VoiceOSProviderManager.kt            # Provider lifecycle management
│   └── SpeechRecognitionServiceProvider.kt  # Service provider interface
```

**Configuration System:**
```
src/main/java/com/augmentalis/voiceos/config/
├── SpeechRecognitionConfig.kt               # Main config data class
├── SpeechRecognitionConfigBuilder.kt        # Builder pattern for config
├── DefaultProviderConfigProvider.kt         # Provider selection logic
├── DefaultTimeoutConfigProvider.kt          # Timeout management
├── DefaultNetworkConfigProvider.kt          # Network-aware config
├── DefaultAudioProcessingConfigProvider.kt  # Audio processing settings
└── DefaultErrorHandlingConfigProvider.kt    # Error handling strategies
```

### 2. Service Implementations

**Speech Recognition Services:**
```
src/main/java/com/augmentalis/voiceos/impl/service/
├── GoogleSpeechRecognitionService.kt        # Google Speech API wrapper
├── VivokaSpeechRecognitionService.kt        # Vivoka SDK v5 integration
├── VoskSpeechRecognitionService.kt          # Vosk offline recognition
├── AVAChatRecognitionService.kt             # Custom AVA chat service
└── common/
    └── TimeoutManager.kt                    # Service timeout handling
```

**Service Wrappers (Adapter Pattern):**
```
src/main/java/com/augmentalis/voiceos/impl/service/
├── VivokaSpeechServiceWrapper.kt            # Vivoka adapter
src/main/java/com/augmentalis/voiceos/speech/
├── VivokaSpeechServiceWrapper.kt            # Alternative Vivoka wrapper
└── VivokaSpeechRecognitionService.kt        # Vivoka service implementation
```

**Adapter Layer:**
```
src/main/java/com/augmentalis/voiceos/adapter/
└── LegacyServiceAdapter.kt                  # Adapts old services to new API
```

### 3. Performance & Resource Management

**Memory Management:**
```
src/main/java/com/augmentalis/voiceos/impl/memory/
├── MemoryPressureMonitor.kt                 # Monitors memory usage (CRITICAL for AVA)
└── AudioBufferPool.kt                       # Reusable audio buffers (reduces GC)
```

**Power Management:**
```
src/main/java/com/augmentalis/voiceos/impl/power/
└── WakeLockManager.kt                       # Battery-aware wake lock management
```

**Lifecycle Management:**
```
src/main/java/com/augmentalis/voiceos/impl/lifecycle/
└── ServiceLifecycleManager.kt               # Service start/stop/cleanup
```

### 4. Audio Processing

**Buffer & Hardware Optimization:**
```
src/main/java/com/augmentalis/voiceos/processing/
├── AdaptiveBufferSizeManager.kt             # Dynamic buffer sizing
├── HardwareAccelerationFactory.kt           # GPU/DSP acceleration
├── EarlyRecognitionService.kt               # Low-latency recognition
└── SharedBufferManager.kt                   # Shared buffer pool
```

### 5. Dependency Injection

**Hilt Modules:**
```
src/main/java/com/augmentalis/voiceos/di/
└── ConfigModule.kt                          # DI configuration
```

### 6. Common Utilities

**Base Path**: `src/main/java/com/augmentalis/voiceos/common/`

Look for shared models, utilities, and interfaces (not listed in find output, but likely present).

### 7. Tests (Reference for Usage Patterns)

**Unit Tests:**
```
src/test/java/com/augmentalis/voiceos/
├── config/
│   ├── TimeoutConfigTest.kt
│   ├── ProviderConfigTest.kt
│   └── TimeoutProviderIntegrationTest.kt
└── api/
    ├── VoiceOSFactoryV2Test.kt
    └── VoiceOSFactoryProviderTest.kt
```

**Instrumentation Tests:**
```
src/androidTest/java/com/augmentalis/voiceos/
└── ExampleInstrumentedTest.kt
```

---

## Integration Steps (Week 7-8)

### Step 1: Add VoiceOS as Git Submodule
```bash
cd "/Volumes/M Drive/Coding/AVA AI"
git submodule add https://gitlab.com/AugmentalisES/voiceos.git external/voiceos
git submodule update --init --recursive
```

### Step 2: Update settings.gradle
```kotlin
// AVA AI/settings.gradle
include(":external:voiceos")
project(":external:voiceos").projectDir = file("external/voiceos/voiceos")
```

### Step 3: Add Dependency to Core Module
```kotlin
// core/common/build.gradle.kts or features/nlu/build.gradle.kts
dependencies {
    implementation(project(":external:voiceos"))

    // VoiceOS dependencies (if not included)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}
```

### Step 4: Copy Memory/Power Utilities
**Option A**: Use entire VoiceOS library
**Option B**: Extract specific utilities

For **memory optimization** (recommended for AVA):
```bash
# Copy memory utilities to AVA AI
cp "/Users/manoj_mbpm14/Downloads/Coding/AVA-VoiceOS-Avanue/AVA/voiceos/src/main/java/com/augmentalis/voiceos/impl/memory/MemoryPressureMonitor.kt" \
   "/Volumes/M Drive/Coding/AVA AI/core/common/src/main/java/com/augmentalis/ava/core/common/memory/"

cp "/Users/manoj_mbpm14/Downloads/Coding/AVA-VoiceOS-Avanue/AVA/voiceos/src/main/java/com/augmentalis/voiceos/impl/memory/AudioBufferPool.kt" \
   "/Volumes/M Drive/Coding/AVA AI/core/common/src/main/java/com/augmentalis/ava/core/common/memory/"

cp "/Users/manoj_mbpm14/Downloads/Coding/AVA-VoiceOS-Avanue/AVA/voiceos/src/main/java/com/augmentalis/voiceos/impl/power/WakeLockManager.kt" \
   "/Volumes/M Drive/Coding/AVA AI/core/common/src/main/java/com/augmentalis/ava/core/common/power/"
```

Update package names from `com.augmentalis.voiceos` → `com.augmentalis.ava.core.common`.

### Step 5: Create AVA Speech Recognition Use Case
```kotlin
// features/nlu/src/main/java/com/augmentalis/ava/features/nlu/SpeechRecognitionUseCase.kt
package com.augmentalis.ava.features.nlu

import com.augmentalis.voiceos.api.VoiceOSFactoryV2
import com.augmentalis.voiceos.config.SpeechRecognitionConfigBuilder
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.Flow

class SpeechRecognitionUseCase(
    private val factory: VoiceOSFactoryV2
) {
    fun recognizeSpeech(
        languageCode: String = "en-US",
        offline: Boolean = false
    ): Flow<Result<String>> {
        val config = SpeechRecognitionConfigBuilder()
            .setLanguage(languageCode)
            .setOfflineMode(offline)
            .setMaxResults(1)
            .build()

        val service = factory.createSpeechRecognitionService(config)
        return service.startListening()
    }
}
```

### Step 6: Integrate with Existing NLU Pipeline
```kotlin
// features/nlu/src/main/java/com/augmentalis/ava/features/nlu/NLUViewModel.kt
class NLUViewModel(
    private val speechRecognition: SpeechRecognitionUseCase,
    private val intentClassifier: ClassifyIntentUseCase
) : ViewModel() {

    fun processVoiceInput() = viewModelScope.launch {
        speechRecognition.recognizeSpeech()
            .collect { result ->
                when (result) {
                    is Result.Success -> {
                        val transcript = result.data
                        // Pass to NLU classifier
                        classifyIntent(transcript)
                    }
                    is Result.Error -> {
                        // Handle error
                    }
                }
            }
    }
}
```

### Step 7: Add Memory Monitoring
```kotlin
// platform/app/src/main/java/com/augmentalis/ava/AvaApplication.kt
import com.augmentalis.ava.core.common.memory.MemoryPressureMonitor

class AvaApplication : Application() {
    private lateinit var memoryMonitor: MemoryPressureMonitor

    override fun onCreate() {
        super.onCreate()

        // Initialize memory monitoring
        memoryMonitor = MemoryPressureMonitor(this)
        memoryMonitor.startMonitoring { memoryLevel ->
            when (memoryLevel) {
                MemoryLevel.CRITICAL -> {
                    // Reduce audio buffer sizes
                    // Pause non-critical background tasks
                }
                MemoryLevel.LOW -> {
                    // Optimize memory usage
                }
                else -> {
                    // Normal operation
                }
            }
        }
    }
}
```

---

## Key Classes to Use

### 1. VoiceOSFactoryV2
**Purpose**: Main entry point for creating speech recognition services
**Usage**:
```kotlin
val factory = VoiceOSFactoryV2(context)
val config = SpeechRecognitionConfigBuilder()
    .setProvider(ProviderType.GOOGLE)  // or VIVOKA, VOSK
    .setLanguage("en-US")
    .setOfflineMode(false)
    .build()

val service = factory.createSpeechRecognitionService(config)
```

### 2. MemoryPressureMonitor
**Purpose**: Monitors memory usage and triggers callbacks on low memory
**Usage**:
```kotlin
val monitor = MemoryPressureMonitor(context)
monitor.startMonitoring { level ->
    if (level == MemoryLevel.CRITICAL) {
        // Reduce memory footprint
    }
}
```

### 3. AudioBufferPool
**Purpose**: Reusable audio buffer pool (reduces GC pressure)
**Usage**:
```kotlin
val bufferPool = AudioBufferPool(bufferSize = 1024, poolSize = 10)
val buffer = bufferPool.acquire()
try {
    // Use buffer for audio processing
} finally {
    bufferPool.release(buffer)
}
```

### 4. WakeLockManager
**Purpose**: Battery-aware wake lock management
**Usage**:
```kotlin
val wakeLockManager = WakeLockManager(context)
wakeLockManager.acquire(timeout = 30_000L)  // 30 seconds
try {
    // Perform speech recognition
} finally {
    wakeLockManager.release()
}
```

---

## Testing Checklist

- [ ] VoiceOS builds successfully with AVA AI
- [ ] Google Speech provider works online
- [ ] Vosk provider works offline
- [ ] Memory usage stays below 512MB on low-end device
- [ ] Battery drain is <10% per hour during active listening
- [ ] Provider switching works (online → offline)
- [ ] Error handling triggers fallback provider
- [ ] Integration tests pass (speech → NLU → response)

---

## Performance Targets

| Metric | Target | VoiceOS Capability |
|--------|--------|-------------------|
| Speech-to-Text Latency | <500ms | ✅ Supported |
| Memory Peak | <512MB | ✅ MemoryPressureMonitor |
| Battery Drain | <10%/hour | ✅ WakeLockManager |
| Offline Mode | 100% functional | ✅ Vosk provider |
| Provider Switch Time | <1s | ✅ Adaptive selection |

---

## Dependencies Required

**VoiceOS Core** (included in submodule):
- Kotlin Coroutines
- AndroidX Lifecycle
- Hilt (Dependency Injection)

**Provider SDKs** (add separately):
```kotlin
// Google Speech
implementation("com.google.cloud:google-cloud-speech:2.x.x")

// Vosk (offline)
implementation("com.alphacephei:vosk-android:0.3.x")

// Vivoka SDK v5 (contact vendor)
// Custom SDK files in external/vsdk-v5/
```

---

## Migration Strategy

**Phase 1** (Week 7): Add VoiceOS as submodule
**Phase 2** (Week 7): Integrate Google Speech provider (online)
**Phase 3** (Week 8): Integrate Vosk provider (offline)
**Phase 4** (Week 8): Add memory/power optimization
**Phase 5** (Week 8): End-to-end testing

---

## References

- VoiceOS README: `/Users/manoj_mbpm14/Downloads/Coding/AVA-VoiceOS-Avanue/AVA/README.md`
- Architecture Docs: `/Users/manoj_mbpm14/Downloads/Coding/AVA-VoiceOS-Avanue/AVA/Documentation/`
- Legacy Analysis: `docs/active/Analysis-Legacy-Codebases-251030-0210.md`

---

**Created**: 2025-10-30 02:15 PDT
**Next Review**: After ALC Engine rewrite (Week 6)
**Priority**: Integrate in Week 7-8 (after Chat UI + ALC)

Created by Manoj Jhawar, manoj@ideahq.net
