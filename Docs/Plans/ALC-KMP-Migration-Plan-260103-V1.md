# Implementation Plan: ALC KMP Migration

## Overview

| Attribute | Value |
|-----------|-------|
| **Feature** | Convert ALC-LLM to Kotlin Multiplatform |
| **Platforms** | Android, iOS, macOS, Windows, Linux |
| **Swarm Recommended** | YES (5 platforms, 25+ tasks) |
| **Estimated Tasks** | 28 tasks |
| **KMP Benefit** | 70% code reuse |

---

## Chain of Thought Reasoning (.cot)

### Current State Analysis

| Package | Files | Platform-Dependent | Target |
|---------|-------|-------------------|--------|
| alc/ | 59 | Android (TVM) | Platform-specific |
| domain/ | 13 | None | commonMain |
| provider/ | 38 | Partial (Context) | commonMain + expect/actual |
| response/ | 15 | None | commonMain |
| config/ | 5 | Partial | commonMain |
| cache/ | 3 | None | commonMain |
| download/ | 12 | Android | Platform-specific |

### Platform Strategy

| Platform | Inference Engine | Priority |
|----------|-----------------|----------|
| Android | TVM Runtime (existing) | P0 |
| iOS | Core ML / ONNX | P1 |
| macOS | ONNX Runtime | P2 |
| Linux | llama.cpp / ONNX | P2 |
| Windows | ONNX Runtime | P3 |

---

## Tree of Thought Analysis (.tot)

### Approach Comparison

| Approach | Description | Risk | Effort | Selected |
|----------|-------------|------|--------|----------|
| A: Full Rewrite | Start fresh | High | High | No |
| B: Gradual Migration | Migrate piece by piece | Low | Medium | No |
| **C: Modular Split** | Shared core + platform engines | Low | Medium | **YES** |

### Architecture Decision

```
Modules/ALC/                          # NEW KMP Module
├── build.gradle.kts                  # KMP configuration
├── src/
│   ├── commonMain/kotlin/com/augmentalis/alc/
│   │   ├── domain/                   # Pure models
│   │   ├── provider/                 # Cloud LLM providers
│   │   ├── response/                 # Response generators
│   │   ├── config/                   # Configuration
│   │   ├── cache/                    # Token caching
│   │   └── engine/                   # expect declarations
│   ├── androidMain/kotlin/com/augmentalis/alc/
│   │   └── engine/                   # TVM implementation
│   ├── iosMain/kotlin/com/augmentalis/alc/
│   │   └── engine/                   # Core ML implementation
│   ├── desktopMain/kotlin/com/augmentalis/alc/
│   │   └── engine/                   # ONNX/llama.cpp implementation
│   └── commonTest/
└── libs/                             # Native libraries
```

---

## Phases

### Phase 1: Foundation (commonMain)
**Rationale:** Domain models and interfaces are platform-agnostic

| # | Task | Files | Priority |
|---|------|-------|----------|
| 1.1 | Create ALC module structure | build.gradle.kts | P0 |
| 1.2 | Create domain models | ChatMessage, LLMResponse, TokenUsage, MessageRole, GenerationOptions | P0 |
| 1.3 | Create provider interfaces | ILLMProvider, IInferenceEngine, IStreamingManager | P0 |
| 1.4 | Create config models | ModelConfig, DeviceProfile, LLMConfig | P0 |
| 1.5 | Create cache module | TokenCacheManager, TokenCacheStats | P1 |

### Phase 2: Cloud Providers (commonMain)
**Rationale:** HTTP-based, no platform dependencies

| # | Task | Files | Priority |
|---|------|-------|----------|
| 2.1 | Create Anthropic provider | AnthropicProvider.kt | P0 |
| 2.2 | Create OpenAI provider | OpenAIProvider.kt | P0 |
| 2.3 | Create Google AI provider | GoogleAIProvider.kt | P1 |
| 2.4 | Create OpenRouter provider | OpenRouterProvider.kt | P1 |
| 2.5 | Create Groq provider | GroqProvider.kt | P2 |
| 2.6 | Create provider factory | LLMProviderFactory.kt | P0 |

### Phase 3: Response Generation (commonMain)
**Rationale:** Pure Kotlin logic, no platform deps

| # | Task | Files | Priority |
|---|------|-------|----------|
| 3.1 | Create template response generator | TemplateResponseGenerator.kt | P1 |
| 3.2 | Create hybrid response generator | HybridResponseGenerator.kt | P1 |
| 3.3 | Create intent templates | IntentTemplates.kt | P2 |
| 3.4 | Create context builder | LLMContextBuilder.kt | P1 |

### Phase 4: Android Platform (androidMain)
**Rationale:** Migrate existing TVM implementation

| # | Task | Files | Priority |
|---|------|-------|----------|
| 4.1 | Create TVM runtime wrapper | TVMRuntime.kt | P0 |
| 4.2 | Create TVM module loader | TVMModule.kt | P0 |
| 4.3 | Create ALC engine (Android) | ALCEngineAndroid.kt | P0 |
| 4.4 | Create token sampler | TokenSampler.kt | P0 |
| 4.5 | Create streaming manager | StreamingManagerAndroid.kt | P1 |
| 4.6 | Create memory manager | MemoryManagerAndroid.kt | P1 |
| 4.7 | Create model loader | ModelLoaderAndroid.kt | P1 |
| 4.8 | Deprecate old AVA/LLM code | Add @Deprecated annotations | P0 |

### Phase 5: iOS Platform (iosMain)
**Rationale:** Core ML for on-device inference

| # | Task | Files | Priority |
|---|------|-------|----------|
| 5.1 | Create Core ML wrapper | CoreMLRuntime.kt | P1 |
| 5.2 | Create ALC engine (iOS) | ALCEngineIOS.kt | P1 |
| 5.3 | Create model loader | ModelLoaderIOS.kt | P1 |
| 5.4 | Create memory manager | MemoryManagerIOS.kt | P2 |

### Phase 6: Desktop Platform (desktopMain)
**Rationale:** ONNX Runtime for macOS/Windows/Linux

| # | Task | Files | Priority |
|---|------|-------|----------|
| 6.1 | Create ONNX runtime wrapper | ONNXRuntime.kt | P2 |
| 6.2 | Create ALC engine (Desktop) | ALCEngineDesktop.kt | P2 |
| 6.3 | Create llama.cpp integration | LlamaCppEngine.kt | P3 |
| 6.4 | Create model loader | ModelLoaderDesktop.kt | P2 |

### Phase 7: Testing & Integration
| # | Task | Files | Priority |
|---|------|-------|----------|
| 7.1 | Create common unit tests | commonTest/ | P0 |
| 7.2 | Create Android integration tests | androidTest/ | P1 |
| 7.3 | Update settings.gradle.kts | Include :Modules:ALC | P0 |
| 7.4 | Update AVA app dependencies | Replace AVA/LLM with ALC | P0 |

---

## Time Estimates

| Execution | Hours | Days |
|-----------|-------|------|
| Sequential | 40h | 5 days |
| Parallel (Swarm) | 16h | 2 days |
| **Savings** | 24h | 60% |

---

## Known Issues to Fix

| Issue | Location | Fix |
|-------|----------|-----|
| Context dependency in providers | provider/*.kt | Use expect/actual for Context |
| Hardcoded Android paths | download/*.kt | Platform-specific path resolution |
| TVM native library loading | alc/loader/ | Platform-specific library loading |
| Memory pressure handling | alc/memory/ | Platform-specific memory APIs |
| Async/coroutine dispatcher | alc/*.kt | Use Dispatchers.Default in common |

---

## Deprecation Strategy

```kotlin
// In AVA/LLM module
@Deprecated(
    message = "Moved to ALC KMP module",
    replaceWith = ReplaceWith("com.augmentalis.alc.engine.ALCEngine"),
    level = DeprecationLevel.WARNING
)
class ALCEngine { ... }
```

---

## Swarm Agent Assignment

| Agent | Phases | Tasks |
|-------|--------|-------|
| Agent 1 | Phase 1, 7 | Foundation + Testing |
| Agent 2 | Phase 2 | Cloud Providers |
| Agent 3 | Phase 3, 4 | Response + Android |
| Agent 4 | Phase 5 | iOS Platform |
| Agent 5 | Phase 6 | Desktop Platform |

---

## Success Criteria

| Criteria | Metric |
|----------|--------|
| All domain models in commonMain | 13/13 files |
| Cloud providers in commonMain | 5/5 providers |
| Android TVM working | Pass existing tests |
| iOS stubs compilable | Build succeeds |
| Desktop stubs compilable | Build succeeds |
| Old code deprecated | All 164 files annotated |
| No functionality lost | Feature parity verified |

---

## File: build.gradle.kts (Preview)

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("io.ktor:ktor-client-core:2.3.7")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
            }
        }
        val iosMain by creating {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.7")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.7")
            }
        }
    }
}
```

---

**Created:** 2026-01-03
**Author:** Claude (IDEACode)
**Branch:** VoiceOSCoreNG
