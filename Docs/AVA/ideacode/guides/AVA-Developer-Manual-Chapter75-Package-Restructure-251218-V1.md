# Developer Manual - Chapter 75: Package Structure Simplification

**Version:** 1.0
**Created:** 2025-12-18
**Author:** Manoj Jhawar
**Status:** Complete

---

## Overview

This chapter documents the package restructure migration applied to AVA's feature modules. The migration simplified deeply nested package structures to improve code maintainability and reduce import verbosity.

---

## Migration Summary

| Metric | Value |
|--------|-------|
| Files Migrated | 391 |
| Modules Affected | 9 |
| Commit | `b682983d` |
| Build Status | ✅ Verified |

---

## Package Mapping

### Before vs After

| Module | Old Package | New Package |
|--------|-------------|-------------|
| Chat | `com.augmentalis.ava.features.chat` | `com.augmentalis.chat` |
| RAG | `com.augmentalis.ava.features.rag` | `com.augmentalis.rag` |
| LLM | `com.augmentalis.ava.features.llm` | `com.augmentalis.llm` |
| NLU | `com.augmentalis.ava.features.nlu` | `com.augmentalis.nlu` |
| Actions | `com.augmentalis.ava.features.actions` | `com.augmentalis.actions` |
| Teach | `com.augmentalis.ava.features.teach` | `com.augmentalis.teach` |
| WakeWord | `com.augmentalis.ava.features.wakeword` | `com.augmentalis.wakeword` |
| memory | `com.augmentalis.ava.features.memory` | `com.augmentalis.memory` |
| Overlay | `com.augmentalis.ava.features.overlay` | `com.augmentalis.overlay` |

### Subpackage Flattening

| Old Subpackage | New Subpackage |
|----------------|----------------|
| `chat.ui.state` | `chat.state` |
| `chat.ui.components` | `chat.components` |
| `rag.data.handlers` | `rag.handlers` |
| `rag.data.clustering` | `rag.clustering` |

---

## Module Directory Structure

### Chat Module

```
Modules/AVA/Chat/src/
├── androidMain/kotlin/com/augmentalis/chat/
│   └── event/
│       ├── WakeWordEventBusModule.kt
│       └── WakeWordEventBusProvider.kt
├── commonMain/kotlin/com/augmentalis/chat/
│   ├── coordinator/
│   │   ├── IActionCoordinator.kt
│   │   ├── INLUCoordinator.kt
│   │   ├── IRAGCoordinator.kt
│   │   └── IResponseCoordinator.kt
│   ├── data/
│   │   └── BuiltInIntents.kt
│   ├── domain/
│   │   └── RAGContextBuilder.kt
│   ├── event/
│   │   └── WakeWordEventBus.kt
│   └── tts/
│       └── TTSSettings.kt
└── main/kotlin/com/augmentalis/chat/
    ├── ChatScreen.kt
    ├── ChatViewModel.kt
    ├── ConversationListScreen.kt
    ├── components/
    │   ├── AppPreferenceBottomSheet.kt
    │   ├── ConfidenceLearningDialog.kt
    │   ├── DeviceConfigSelector.kt
    │   ├── HistoryOverlay.kt
    │   ├── MessageBubble.kt
    │   ├── ModelStatusBanner.kt
    │   ├── StatusIndicator.kt
    │   ├── TeachAvaBottomSheet.kt
    │   └── TTSControls.kt
    ├── coordinator/
    │   ├── ActionCoordinator.kt
    │   ├── NLUCoordinator.kt
    │   ├── NLUDispatcher.kt
    │   ├── RAGCoordinator.kt
    │   ├── ResponseCoordinator.kt
    │   └── TTSCoordinator.kt
    ├── di/
    │   └── ChatModule.kt
    ├── dialogs/
    │   └── DocumentSelectorDialog.kt
    ├── settings/
    │   └── RAGSettingsSection.kt
    ├── state/
    │   ├── ChatUIStateManager.kt
    │   └── StatusIndicatorState.kt
    ├── tts/
    │   ├── TTSManager.kt
    │   ├── TTSPreferences.kt
    │   └── TTSViewModel.kt
    └── voice/
        └── VoiceOSStub.kt
```

### RAG Module

```
Modules/AVA/RAG/src/
├── androidMain/kotlin/com/augmentalis/rag/
│   ├── chat/
│   │   ├── LocalLLMProviderAdapter.kt
│   │   └── RAGLLMIntegrationExample.kt
│   ├── clustering/
│   │   └── KMeansClustering.kt
│   ├── data/
│   │   └── SQLiteRAGRepository.kt
│   ├── embeddings/
│   │   ├── AONFileManager.kt
│   │   ├── AONPackageManager.kt
│   │   ├── AONWrapperTool.kt
│   │   ├── AndroidModelDownloadManager.kt
│   │   ├── BatchModelWrapper.kt
│   │   ├── EmbeddingProviderFactory.android.kt
│   │   ├── ONNXEmbeddingProvider.android.kt
│   │   └── PlatformDetector.kt
│   ├── handlers/
│   │   ├── ChunkEmbeddingHandler.kt
│   │   ├── ClusteredSearchHandler.kt
│   │   └── DocumentIngestionHandler.kt
│   ├── llm/
│   │   └── MLCLLMProvider.android.kt
│   └── parser/
│       ├── DocumentParserFactory.android.kt
│       ├── DocxParser.android.kt
│       ├── HtmlParser.android.kt
│       ├── MarkdownParser.android.kt
│       ├── PdfParser.android.kt
│       ├── RtfParser.android.kt
│       └── TxtParser.android.kt
└── commonMain/kotlin/com/augmentalis/rag/
    └── domain/
        └── [domain models]
```

### LLM Module

```
Modules/AVA/LLM/src/
├── androidMain/kotlin/com/augmentalis/llm/
│   └── response/
│       └── TimeProvider.android.kt
├── commonMain/kotlin/com/augmentalis/llm/
│   ├── LanguageDetector.kt
│   ├── alc/
│   │   ├── StopTokenDetector.kt
│   │   ├── TokenSampler.kt
│   │   ├── interfaces/
│   │   ├── models/
│   │   └── samplers/
│   ├── domain/
│   │   ├── ChatMessage.kt
│   │   ├── LLMProvider.kt
│   │   └── LLMResponse.kt
│   ├── download/
│   │   └── DownloadState.kt
│   ├── metrics/
│   │   └── LatencyMetrics.kt
│   └── response/
│       ├── ImprovedIntentTemplates.kt
│       ├── IntentTemplates.kt
│       ├── LLMContextBuilder.kt
│       ├── ResponseGenerator.kt
│       └── TemplateResponseGenerator.kt
└── main/java/com/augmentalis/llm/
    ├── LanguageDetector.kt
    ├── ModelSelector.kt
    ├── SystemPromptManager.kt
    ├── alc/
    ├── cache/
    ├── config/
    ├── di/
    ├── domain/
    ├── download/
    ├── inference/
    ├── metrics/
    ├── provider/
    ├── response/
    ├── security/
    └── teacher/
```

---

## Build Configuration Updates

### Gradle Namespace Changes

All module `build.gradle.kts` files updated with new namespaces:

```kotlin
// Chat Module
android {
    namespace = "com.augmentalis.chat"  // was: "com.augmentalis.ava.features.chat"
}

// RAG Module
android {
    namespace = "com.augmentalis.rag"   // was: "com.augmentalis.ava.features.rag"
}

// LLM Module
android {
    namespace = "com.augmentalis.llm"   // was: "com.augmentalis.ava.features.llm"
}

// Actions Module
android {
    namespace = "com.augmentalis.actions"  // was: "com.augmentalis.ava.features.actions"
}

// WakeWord Module
android {
    namespace = "com.augmentalis.wakeword"  // was: "com.augmentalis.wakeword"
}

// Overlay Module
android {
    namespace = "com.augmentalis.overlay"  // was: "com.augmentalis.overlay"
}
```

---

## Developer Migration Guide

### Updating Imports

If you have external code referencing AVA modules, update imports:

```kotlin
// OLD
import com.augmentalis.ava.features.chat.ChatViewModel
import com.augmentalis.ava.features.chat.ui.state.ChatUIStateManager
import com.augmentalis.ava.features.rag.data.handlers.DocumentIngestionHandler

// NEW
import com.augmentalis.chat.ChatViewModel
import com.augmentalis.chat.state.ChatUIStateManager
import com.augmentalis.rag.handlers.DocumentIngestionHandler
```

### IDE Find & Replace Pattern

Use regex find/replace to update imports:

| Find Pattern | Replace Pattern |
|--------------|-----------------|
| `com\.augmentalis\.ava\.features\.chat\.ui\.state` | `com.augmentalis.chat.state` |
| `com\.augmentalis\.ava\.features\.chat\.ui\.components` | `com.augmentalis.chat.components` |
| `com\.augmentalis\.ava\.features\.rag\.data\.handlers` | `com.augmentalis.rag.handlers` |
| `com\.augmentalis\.ava\.features\.(\w+)` | `com.augmentalis.$1` |

---

## Hilt DI Module Updates

All Hilt modules updated with new package paths:

### ChatModule

```kotlin
package com.augmentalis.chat.di

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {
    // Provides Chat-related dependencies
}
```

### LLMModule

```kotlin
package com.augmentalis.llm.di

@Module
@InstallIn(SingletonComponent::class)
object LLMModule {
    // Provides LLM-related dependencies
}
```

### WakeWordModule

```kotlin
package com.augmentalis.wakeword.di

@Module
@InstallIn(SingletonComponent::class)
object WakeWordModule {
    // Provides WakeWord-related dependencies
}
```

---

## Cross-Module Dependencies

### Updated Import Patterns

```kotlin
// Chat → NLU
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.domain.IntentClassification

// Chat → Actions
import com.augmentalis.actions.ActionsManager
import com.augmentalis.actions.ActionResult

// Chat → RAG
import com.augmentalis.rag.domain.SearchRequest
import com.augmentalis.rag.domain.SearchResult

// Chat → LLM
import com.augmentalis.llm.domain.LLMResponse
import com.augmentalis.llm.response.ResponseGenerator
```

---

## KMP Source Set Organization

### Multi-Platform Package Structure

```
src/
├── commonMain/kotlin/com/augmentalis/{module}/
│   └── [shared code - interfaces, domain models]
├── androidMain/kotlin/com/augmentalis/{module}/
│   └── [Android implementations]
├── iosMain/kotlin/com/augmentalis/{module}/
│   └── [iOS implementations]
├── desktopMain/kotlin/com/augmentalis/{module}/
│   └── [Desktop implementations]
└── jsMain/kotlin/com/augmentalis/{module}/
    └── [Web implementations]
```

---

## Rationale

### Why This Change?

1. **Reduced Import Verbosity**
   - Before: `import com.augmentalis.ava.features.chat.ui.state.ChatUIStateManager`
   - After: `import com.augmentalis.chat.state.ChatUIStateManager`
   - **Savings:** 18 characters per import

2. **Cleaner Package Hierarchy**
   - Removed redundant `ava.features` segment
   - Flattened unnecessary `ui` and `data` intermediate packages
   - Matches industry standard for module organization

3. **Improved KMP Readiness**
   - Platform-agnostic package names (`com.augmentalis.chat`)
   - Clear separation between shared and platform-specific code
   - Consistent with KMP best practices

4. **Better IDE Navigation**
   - Shorter package paths in file tree
   - Faster auto-complete suggestions
   - Reduced cognitive load

---

## Verification

### Build Verification

```bash
cd android/apps/ava
./gradlew assembleDebug
# BUILD SUCCESSFUL in 1m 38s (465 tasks)
```

### Package Verification

```bash
# Verify no old packages remain
grep -r "com\.augmentalis\.ava\.features\." --include="*.kt" | wc -l
# 0
```

---

## Related Documentation

- [Chapter 72: SOLID Architecture](/Docs/AVA/ideacode/guides/Developer-Manual-Chapter72-SOLID-Architecture.md)
- [Chapter 59: NLU Multiplatform](/Docs/AVA/ideacode/guides/Developer-Manual-Chapter59-NLU-Multiplatform.md)
- [Package Mapping JSON](/Docs/AVA/migrations/package-mapping.json)

---

**End of Chapter 75**
