# Implementation Plan: VoiceOSCore Module Streamlining (v4 - Simplified)

**Date:** 2026-01-19
**Branch:** legacy-consolidation
**Revision:** 4 - Direct usage of AI modules, no factories/interfaces

---

## Overview

| Metric | Value |
|--------|-------|
| Approach | Delete abstractions, use AI modules directly |
| Files to Delete | ~15 |
| Lines Removed | ~1,200+ |
| Lines Added | ~50 (dependency wiring) |
| Estimated Time | 2 hours |

---

## The Simple Solution

Each platform compiles separately. No need for runtime abstraction layers.

```
BEFORE (over-engineered):
┌─────────────────────────────────────────────────────────────┐
│ VoiceOSCore                                                 │
│  ├── ILlmProcessor.kt (interface)                          │
│  ├── INluProcessor.kt (interface)                          │
│  ├── ILlmFallbackHandler.kt (interface)                    │
│  ├── LlmProcessorFactory.kt (expect)                       │
│  ├── NluProcessorFactory.kt (expect)                       │
│  ├── LlmFallbackHandlerFactory.kt (expect)                 │
│  ├── StubVivokaEngine.kt                                   │
│  ├── androidMain/LlmProcessorFactory.android.kt + Stub     │
│  ├── androidMain/NluProcessorFactory.android.kt + Stub     │
│  ├── androidMain/LlmFallbackHandlerFactory.android.kt +Stub│
│  ├── iosMain/LlmProcessorFactory.ios.kt + Stub             │
│  ├── iosMain/NluProcessorFactory.ios.kt + Stub             │
│  ├── iosMain/LlmFallbackHandlerFactory.ios.kt + Stub       │
│  ├── desktopMain/LlmProcessorFactory.desktop.kt + Stub     │
│  ├── desktopMain/NluProcessorFactory.desktop.kt + Stub     │
│  └── desktopMain/LlmFallbackHandlerFactory.desktop.kt +Stub│
└─────────────────────────────────────────────────────────────┘

AFTER (simple):
┌─────────────────────────────────────────────────────────────┐
│ VoiceOSCore                                                 │
│  └── depends on Modules/AI/LLM and Modules/AI/NLU          │
│                                                             │
│ Usage in code:                                              │
│  val llm = LocalLLMProvider(context)  // Android           │
│  val nlu = IntentClassifier(context)  // Android           │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Delete Abstraction Layer (P0)

**Goal:** Remove all factory/interface/stub code from VoiceOSCore

### Files to Delete

#### CommonMain (6 files)
| File | Reason |
|------|--------|
| `ILlmProcessor.kt` | Use `LocalLLMProvider` directly |
| `INluProcessor.kt` | Use `IntentClassifier` directly |
| `ILlmFallbackHandler.kt` | Use `HybridResponseGenerator` directly |
| `LlmProcessorFactory.kt` | No factory needed |
| `NluProcessorFactory.kt` | No factory needed |
| `LlmFallbackHandlerFactory.kt` | No factory needed |
| `StubVivokaEngine.kt` | No stubs allowed |

#### AndroidMain (3 files)
| File | Reason |
|------|--------|
| `LlmProcessorFactory.android.kt` | Delete (contains StubLlmProcessor) |
| `NluProcessorFactory.android.kt` | Delete (contains StubNluProcessor) |
| `LlmFallbackHandlerFactory.android.kt` | Delete (contains StubLlmFallbackHandler) |

#### iOSMain (3 files)
| File | Reason |
|------|--------|
| `LlmProcessorFactory.ios.kt` | Delete (contains StubLlmProcessorIOS) |
| `NluProcessorFactory.ios.kt` | Delete (contains StubNluProcessorIOS) |
| `LlmFallbackHandlerFactory.ios.kt` | Delete (contains StubLlmFallbackHandlerIOS) |

#### DesktopMain (3 files)
| File | Reason |
|------|--------|
| `LlmProcessorFactory.desktop.kt` | Delete (contains StubLlmProcessorDesktop) |
| `NluProcessorFactory.desktop.kt` | Delete (contains StubNluProcessorDesktop) |
| `LlmFallbackHandlerFactory.desktop.kt` | Delete (contains StubLlmFallbackHandlerDesktop) |

### Also Remove from Interface Files
| File | Action |
|------|--------|
| `IResourceMonitor.kt` | Remove `StubResourceMonitor` class (keep interface) |
| `IAppVersionDetector.kt` | Remove `StubAppVersionDetector` class (keep interface) |

**Total: 15 files deleted + 2 stub classes removed**

---

## Phase 2: Add Dependencies (P0)

**Goal:** VoiceOSCore depends on AI modules

### Task 2.1: Update build.gradle.kts

**File:** `Modules/VoiceOSCore/build.gradle.kts`

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Existing dependencies...
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":Modules:AI:LLM"))
                implementation(project(":Modules:AI:NLU"))
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(project(":Modules:AI:NLU"))  // CoreML-based
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(project(":Modules:AI:LLM"))  // OllamaProvider
            }
        }
    }
}
```

---

## Phase 3: Update Consumers (P0)

**Goal:** Replace factory calls with direct instantiation

### Find and Replace Pattern

**Before:**
```kotlin
val llmProcessor = LlmProcessorFactory.create(config)
val result = llmProcessor.interpretCommand(utterance, schema, commands)
```

**After:**
```kotlin
val llmProvider = LocalLLMProvider(context)
llmProvider.initialize()
val result = llmProvider.generate(prompt)
```

### Files to Update

Search for usages:
```bash
grep -r "LlmProcessorFactory\|NluProcessorFactory\|LlmFallbackHandlerFactory" \
  --include="*.kt" Modules/VoiceOSCore/
```

Update each call site to use the AI module classes directly.

---

## Phase 4: Clean Up Directories (P1)

### Task 4.1: Remove empty SQLDelight directory
```bash
rm -rf Modules/VoiceOSCore/src/commonMain/sqldelight/
```

### Task 4.2: Remove empty AIDL directory
```bash
rm -rf Modules/VoiceOSCore/src/androidMain/aidl/
```

---

## Phase 5: Verify & Compile (P0)

### Task 5.1: Compile Android
```bash
./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid
```

### Task 5.2: Compile iOS
```bash
./gradlew :Modules:VoiceOSCore:compileKotlinIosArm64
```

### Task 5.3: Compile Desktop
```bash
./gradlew :Modules:VoiceOSCore:compileKotlinDesktop
```

### Task 5.4: Build VoiceOSCoreNG app
```bash
./gradlew :android:apps:voiceoscoreng:compileDebugKotlin
```

---

## Execution Summary

| Phase | Tasks | Est. Time | Result |
|-------|-------|-----------|--------|
| 1 | Delete 15 files + 2 stub classes | 20 min | Abstractions removed |
| 2 | Update build.gradle.kts | 10 min | Dependencies added |
| 3 | Update consumer code | 45 min | Direct usage |
| 4 | Remove empty directories | 5 min | Cleanup |
| 5 | Verify compilation | 30 min | All platforms build |

**Total: ~2 hours**

---

## Direct Usage Examples

### Android - LLM
```kotlin
// In VoiceOSCore/androidMain
import com.augmentalis.llm.provider.LocalLLMProvider

class VoiceCommandProcessor(private val context: Context) {
    private val llmProvider = LocalLLMProvider(context)

    suspend fun processWithLLM(utterance: String): String? {
        if (!llmProvider.isModelLoaded()) {
            llmProvider.initialize()
        }
        return llmProvider.generate(buildPrompt(utterance))
    }
}
```

### Android - NLU
```kotlin
// In VoiceOSCore/androidMain
import com.augmentalis.nlu.IntentClassifier

class IntentProcessor(private val context: Context) {
    private val classifier = IntentClassifier(context)

    suspend fun classify(utterance: String): ClassificationResult {
        return classifier.classify(utterance)
    }
}
```

### iOS - NLU (when needed)
```kotlin
// In VoiceOSCore/iosMain
import com.augmentalis.nlu.IntentClassifier  // CoreML version

class IntentProcessorIOS {
    private val classifier = IntentClassifier()

    suspend fun classify(utterance: String): ClassificationResult {
        return classifier.classify(utterance)
    }
}
```

### Desktop - LLM (when needed)
```kotlin
// In VoiceOSCore/desktopMain
import com.augmentalis.llm.OllamaProvider

class LlmProcessorDesktop {
    private val provider = OllamaProvider()

    suspend fun generate(prompt: String): String {
        return provider.generate(prompt)
    }
}
```

---

## Commit Strategy

1. **Commit 1:** `refactor(voiceoscore): Remove factory/interface abstraction layer`
   - Delete 15 files
   - Remove stub classes from interface files

2. **Commit 2:** `build(voiceoscore): Add direct dependencies on AI modules`
   - Update build.gradle.kts

3. **Commit 3:** `refactor(voiceoscore): Update consumers to use AI modules directly`
   - Replace factory calls with direct instantiation

4. **Commit 4:** `chore(voiceoscore): Remove empty directories`

---

## Verification Checklist

- [ ] All 15 factory/interface/stub files deleted
- [ ] StubResourceMonitor removed from IResourceMonitor.kt
- [ ] StubAppVersionDetector removed from IAppVersionDetector.kt
- [ ] build.gradle.kts updated with AI module dependencies
- [ ] All consumer code updated to direct usage
- [ ] Android compiles
- [ ] iOS compiles
- [ ] Desktop compiles
- [ ] VoiceOSCoreNG app compiles
- [ ] Empty directories removed

---

## Summary

| Metric | Before | After |
|--------|--------|-------|
| Abstraction files | 15 | 0 |
| Stub classes | 12 | 0 |
| Factory classes | 6 | 0 |
| Interface classes | 3 | 0 |
| Lines of code | ~1,200 | ~50 |
| Complexity | High | Low |

**Result:** Clean, direct usage of AI modules with no unnecessary abstraction.

---

**Plan Status:** Ready for implementation
**Next Command:** `/i.implement` or approve to proceed
