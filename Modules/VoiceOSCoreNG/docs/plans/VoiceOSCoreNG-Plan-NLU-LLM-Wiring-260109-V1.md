# VoiceOSCoreNG NLU/LLM Integration - Implementation Plan

**Version:** 1.0
**Date:** 2026-01-09
**Spec:** VoiceOSCoreNG-Spec-NLU-LLM-Wiring-260109-V1.md
**Status:** Ready for Implementation

---

## Overview

This plan implements the NLU and LLM integration specified in the companion spec document. The implementation is divided into two phases: Phase 1 (NLU) and Phase 2 (LLM), with the option to execute them in parallel using swarm agents.

---

## Phase 1: NLU Integration

### 1.1 Add Gradle Dependencies

**File:** `Modules/VoiceOSCoreNG/build.gradle.kts`

**Task:** Add Shared:NLU dependency to androidMain

```kotlin
val androidMain by getting {
    dependencies {
        // ... existing dependencies

        // NLU (BERT-based intent classification)
        implementation(project(":Modules:Shared:NLU"))
    }
}
```

**Acceptance:** Build compiles without errors

---

### 1.2 Create INluProcessor Interface

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/nlu/INluProcessor.kt`

**Task:** Define platform-agnostic NLU processor interface

```kotlin
package com.augmentalis.voiceoscoreng.nlu

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

interface INluProcessor {
    suspend fun initialize(): Result<Unit>
    suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult
    fun isAvailable(): Boolean
    suspend fun dispose()
}

sealed class NluResult {
    data class Match(
        val command: QuantizedCommand,
        val confidence: Float,
        val intent: String? = null
    ) : NluResult()

    data class Ambiguous(
        val candidates: List<Pair<QuantizedCommand, Float>>
    ) : NluResult()

    data object NoMatch : NluResult()
    data class Error(val message: String) : NluResult()
}

data class NluConfig(
    val confidenceThreshold: Float = 0.6f,
    val enabled: Boolean = true
) {
    companion object {
        val DEFAULT = NluConfig()
    }
}
```

---

### 1.3 Implement AndroidNluProcessor

**File:** `src/androidMain/kotlin/com/augmentalis/voiceoscoreng/nlu/AndroidNluProcessor.kt`

**Task:** Wrap IntentClassifier from Shared/NLU module

```kotlin
package com.augmentalis.voiceoscoreng.nlu

import android.content.Context
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.voiceoscoreng.common.QuantizedCommand

class AndroidNluProcessor(
    private val context: Context,
    private val config: NluConfig = NluConfig.DEFAULT
) : INluProcessor {

    private var intentClassifier: IntentClassifier? = null
    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> {
        return try {
            val result = IntentClassifier.create(context)
            if (result.isSuccess) {
                intentClassifier = result.getOrNull()
                isInitialized = true
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to create IntentClassifier"))
            }
        } catch (e: Exception) {
            println("[AndroidNluProcessor] Initialization failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult {
        val classifier = intentClassifier ?: return NluResult.Error("NLU not initialized")

        if (candidateCommands.isEmpty()) {
            return NluResult.NoMatch
        }

        val candidateIntents = candidateCommands.map { it.phrase }

        return try {
            val result = classifier.classifyIntent(utterance, candidateIntents)

            if (result.isSuccess) {
                val classification = result.getOrNull()!!
                val matchedCommand = candidateCommands.find {
                    it.phrase.equals(classification.intent, ignoreCase = true)
                }

                when {
                    matchedCommand != null && classification.confidence >= config.confidenceThreshold -> {
                        NluResult.Match(
                            command = matchedCommand,
                            confidence = classification.confidence,
                            intent = classification.intent
                        )
                    }
                    classification.confidence >= config.confidenceThreshold * 0.8f -> {
                        // Ambiguous - multiple close matches
                        val candidates = classification.allScores
                            .filter { it.value >= config.confidenceThreshold * 0.7f }
                            .mapNotNull { (intent, score) ->
                                candidateCommands.find { it.phrase.equals(intent, ignoreCase = true) }
                                    ?.let { it to score }
                            }
                            .sortedByDescending { it.second }

                        if (candidates.size > 1) {
                            NluResult.Ambiguous(candidates)
                        } else if (candidates.isNotEmpty()) {
                            NluResult.Match(candidates.first().first, candidates.first().second)
                        } else {
                            NluResult.NoMatch
                        }
                    }
                    else -> NluResult.NoMatch
                }
            } else {
                NluResult.Error(result.exceptionOrNull()?.message ?: "Classification failed")
            }
        } catch (e: Exception) {
            NluResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun isAvailable(): Boolean = isInitialized && intentClassifier != null

    override suspend fun dispose() {
        intentClassifier = null
        isInitialized = false
    }
}
```

---

### 1.4 Create Stub NluProcessor for Other Platforms

**File:** `src/iosMain/kotlin/com/augmentalis/voiceoscoreng/nlu/NluProcessor.kt`

```kotlin
package com.augmentalis.voiceoscoreng.nlu

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

class IOSNluProcessor : INluProcessor {
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult = NluResult.NoMatch
    override fun isAvailable(): Boolean = false
    override suspend fun dispose() {}
}
```

**File:** `src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/nlu/NluProcessor.kt`

```kotlin
package com.augmentalis.voiceoscoreng.nlu

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

class DesktopNluProcessor : INluProcessor {
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    override suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult = NluResult.NoMatch
    override fun isAvailable(): Boolean = false
    override suspend fun dispose() {}
}
```

---

### 1.5 Create NluProcessorFactory

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/nlu/NluProcessorFactory.kt`

```kotlin
package com.augmentalis.voiceoscoreng.nlu

expect object NluProcessorFactory {
    fun create(config: NluConfig = NluConfig.DEFAULT): INluProcessor
}
```

**File:** `src/androidMain/kotlin/com/augmentalis/voiceoscoreng/nlu/NluProcessorFactory.kt`

```kotlin
package com.augmentalis.voiceoscoreng.nlu

import android.content.Context

actual object NluProcessorFactory {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun create(config: NluConfig): INluProcessor {
        val ctx = context ?: throw IllegalStateException(
            "NluProcessorFactory not initialized. Call initialize(context) first."
        )
        return AndroidNluProcessor(ctx, config)
    }
}
```

---

### 1.6 Update VoiceOSCoreNG Builder

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/VoiceOSCoreNG.kt`

**Task:** Add NLU processor support to Builder and main class

**Changes:**
1. Add `nluProcessor: INluProcessor?` constructor parameter
2. Add `nluConfig: NluConfig` to Builder
3. Initialize NLU in `initialize()` method
4. Dispose NLU in `dispose()` method

---

### 1.7 Integrate NLU in ActionCoordinator

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/ActionCoordinator.kt`

**Task:** Add NLU classification step after registry lookup

**Changes:**
1. Add `nluProcessor: INluProcessor?` constructor parameter
2. Add NLU classification in `processVoiceCommand()` after fuzzy match fails
3. Track NLU metrics

---

### 1.8 Bundle BERT Model Assets

**Directory:** `src/androidMain/assets/models/nlu/`

**Files to add:**
- `malbert-intent-v1.onnx` - Pre-trained mALBERT model (~15MB)
- `vocab.txt` - WordPiece vocabulary

**Note:** Model files need to be copied from Shared/NLU assets or downloaded from model repository.

---

## Phase 2: LLM Integration

### 2.1 Add Gradle Dependencies

**File:** `Modules/VoiceOSCoreNG/build.gradle.kts`

**Task:** Add Modules:LLM dependency to androidMain

```kotlin
val androidMain by getting {
    dependencies {
        // ... existing dependencies

        // LLM (Local language model for fallback)
        implementation(project(":Modules:LLM"))
    }
}
```

---

### 2.2 Create ILlmProcessor Interface

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/llm/ILlmProcessor.kt`

```kotlin
package com.augmentalis.voiceoscoreng.llm

interface ILlmProcessor {
    suspend fun initialize(): Result<Unit>
    suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult
    fun isAvailable(): Boolean
    fun isModelLoaded(): Boolean
    suspend fun dispose()
}

sealed class LlmResult {
    data class Interpreted(
        val matchedCommand: String,
        val confidence: Float,
        val explanation: String? = null
    ) : LlmResult()

    data object NoMatch : LlmResult()
    data class Error(val message: String) : LlmResult()
}

data class LlmConfig(
    val modelBasePath: String = "/sdcard/ava-ai-models/llm",
    val responseTimeout: Long = 10_000L,
    val maxTokens: Int = 50,
    val temperature: Float = 0.3f,
    val enabled: Boolean = true
) {
    companion object {
        val DEFAULT = LlmConfig()
    }
}
```

---

### 2.3 Create VoiceCommandPrompt

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/llm/VoiceCommandPrompt.kt`

```kotlin
package com.augmentalis.voiceoscoreng.llm

object VoiceCommandPrompt {
    fun create(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): String = buildString {
        appendLine("You are a voice command interpreter for an accessibility app.")
        appendLine()
        appendLine("Available commands:")
        availableCommands.forEach { cmd ->
            appendLine("- $cmd")
        }
        appendLine()
        appendLine("NLU Schema:")
        appendLine(nluSchema)
        appendLine()
        appendLine("User said: \"$utterance\"")
        appendLine()
        appendLine("Match this to the most appropriate command from the list above.")
        appendLine("Respond with ONLY the exact command phrase, nothing else.")
        appendLine("If no command matches, respond with: NO_MATCH")
        appendLine()
        append("Response: ")
    }

    fun parseResponse(response: String, availableCommands: List<String>): String? {
        val trimmed = response.trim().lowercase()

        if (trimmed == "no_match" || trimmed.contains("no match")) {
            return null
        }

        // Try exact match first
        val exactMatch = availableCommands.find {
            it.lowercase() == trimmed
        }
        if (exactMatch != null) return exactMatch

        // Try partial match
        val partialMatch = availableCommands.find { cmd ->
            trimmed.contains(cmd.lowercase()) || cmd.lowercase().contains(trimmed)
        }

        return partialMatch
    }
}
```

---

### 2.4 Implement AndroidLlmProcessor

**File:** `src/androidMain/kotlin/com/augmentalis/voiceoscoreng/llm/AndroidLlmProcessor.kt`

```kotlin
package com.augmentalis.voiceoscoreng.llm

import android.content.Context
import com.augmentalis.llm.provider.LocalLLMProvider
import com.augmentalis.llm.domain.GenerationOptions
import com.augmentalis.llm.domain.LLMConfig
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class AndroidLlmProcessor(
    private val context: Context,
    private val config: LlmConfig = LlmConfig.DEFAULT
) : ILlmProcessor {

    private var llmProvider: LocalLLMProvider? = null
    private var isInitialized = false

    override suspend fun initialize(): Result<Unit> {
        if (!config.enabled) {
            println("[AndroidLlmProcessor] LLM disabled in config")
            return Result.success(Unit)
        }

        return try {
            llmProvider = LocalLLMProvider(
                context = context,
                autoModelSelection = true
            )

            val llmConfig = LLMConfig(
                modelPath = config.modelBasePath,
                maxTokens = config.maxTokens,
                temperature = config.temperature
            )

            val result = llmProvider?.initialize(llmConfig)
            if (result?.isSuccess == true) {
                isInitialized = true
                println("[AndroidLlmProcessor] LLM initialized successfully")
                Result.success(Unit)
            } else {
                println("[AndroidLlmProcessor] LLM initialization failed: ${result?.exceptionOrNull()?.message}")
                Result.failure(result?.exceptionOrNull() ?: Exception("LLM init failed"))
            }
        } catch (e: Exception) {
            println("[AndroidLlmProcessor] LLM initialization exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult {
        val provider = llmProvider
        if (provider == null || !provider.isModelLoaded()) {
            return LlmResult.Error("LLM not available")
        }

        val prompt = VoiceCommandPrompt.create(utterance, nluSchema, availableCommands)

        return try {
            val response = withTimeoutOrNull(config.responseTimeout) {
                val options = GenerationOptions(
                    maxTokens = config.maxTokens,
                    temperature = config.temperature,
                    stopTokens = listOf("\n", ".", "!")
                )

                val responseFlow = provider.generateResponse(prompt, options)
                val fullResponse = StringBuilder()

                responseFlow.collect { chunk ->
                    fullResponse.append(chunk.text)
                }

                fullResponse.toString()
            }

            if (response == null) {
                return LlmResult.Error("LLM response timeout")
            }

            val matchedCommand = VoiceCommandPrompt.parseResponse(response, availableCommands)

            if (matchedCommand != null) {
                LlmResult.Interpreted(
                    matchedCommand = matchedCommand,
                    confidence = 0.75f, // LLM matches have moderate confidence
                    explanation = "Matched via LLM interpretation"
                )
            } else {
                LlmResult.NoMatch
            }
        } catch (e: Exception) {
            LlmResult.Error(e.message ?: "LLM error")
        }
    }

    override fun isAvailable(): Boolean = isInitialized && config.enabled

    override fun isModelLoaded(): Boolean = llmProvider?.isModelLoaded() == true

    override suspend fun dispose() {
        llmProvider?.shutdown()
        llmProvider = null
        isInitialized = false
    }
}
```

---

### 2.5 Create Stub LlmProcessor for Other Platforms

**File:** `src/iosMain/kotlin/com/augmentalis/voiceoscoreng/llm/LlmProcessor.kt`

```kotlin
package com.augmentalis.voiceoscoreng.llm

class IOSLlmProcessor : ILlmProcessor {
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    override suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult = LlmResult.NoMatch
    override fun isAvailable(): Boolean = false
    override fun isModelLoaded(): Boolean = false
    override suspend fun dispose() {}
}
```

**File:** `src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/llm/LlmProcessor.kt`

```kotlin
package com.augmentalis.voiceoscoreng.llm

class DesktopLlmProcessor : ILlmProcessor {
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    override suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult = LlmResult.NoMatch
    override fun isAvailable(): Boolean = false
    override fun isModelLoaded(): Boolean = false
    override suspend fun dispose() {}
}
```

---

### 2.6 Create LlmProcessorFactory

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/llm/LlmProcessorFactory.kt`

```kotlin
package com.augmentalis.voiceoscoreng.llm

expect object LlmProcessorFactory {
    fun create(config: LlmConfig = LlmConfig.DEFAULT): ILlmProcessor
}
```

**File:** `src/androidMain/kotlin/com/augmentalis/voiceoscoreng/llm/LlmProcessorFactory.kt`

```kotlin
package com.augmentalis.voiceoscoreng.llm

import android.content.Context

actual object LlmProcessorFactory {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun create(config: LlmConfig): ILlmProcessor {
        val ctx = context ?: throw IllegalStateException(
            "LlmProcessorFactory not initialized. Call initialize(context) first."
        )
        return AndroidLlmProcessor(ctx, config)
    }
}
```

---

### 2.7 Update VoiceOSCoreNG Builder (LLM)

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/VoiceOSCoreNG.kt`

**Task:** Add LLM processor support

**Changes:**
1. Add `llmProcessor: ILlmProcessor?` constructor parameter
2. Add `llmConfig: LlmConfig` to Builder
3. Initialize LLM in `initialize()` method (after NLU)
4. Dispose LLM in `dispose()` method

---

### 2.8 Complete ActionCoordinator Integration

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/ActionCoordinator.kt`

**Task:** Add LLM fallback step after NLU

**Final processVoiceCommand flow:**
1. Registry exact/fuzzy match (existing)
2. NLU classification (Phase 1)
3. LLM interpretation (Phase 2)
4. Fail gracefully

---

## Task Summary

| Phase | Task ID | Task | File(s) |
|-------|---------|------|---------|
| 1 | 1.1 | Add NLU dependency | build.gradle.kts |
| 1 | 1.2 | Create INluProcessor | commonMain/.../nlu/ |
| 1 | 1.3 | Implement AndroidNluProcessor | androidMain/.../nlu/ |
| 1 | 1.4 | Create stub NluProcessor | iosMain, desktopMain |
| 1 | 1.5 | Create NluProcessorFactory | commonMain, androidMain |
| 1 | 1.6 | Update VoiceOSCoreNG Builder | VoiceOSCoreNG.kt |
| 1 | 1.7 | Integrate NLU in ActionCoordinator | ActionCoordinator.kt |
| 1 | 1.8 | Bundle BERT model | assets/models/nlu/ |
| 2 | 2.1 | Add LLM dependency | build.gradle.kts |
| 2 | 2.2 | Create ILlmProcessor | commonMain/.../llm/ |
| 2 | 2.3 | Create VoiceCommandPrompt | commonMain/.../llm/ |
| 2 | 2.4 | Implement AndroidLlmProcessor | androidMain/.../llm/ |
| 2 | 2.5 | Create stub LlmProcessor | iosMain, desktopMain |
| 2 | 2.6 | Create LlmProcessorFactory | commonMain, androidMain |
| 2 | 2.7 | Update VoiceOSCoreNG Builder (LLM) | VoiceOSCoreNG.kt |
| 2 | 2.8 | Complete ActionCoordinator | ActionCoordinator.kt |

---

## Execution Strategy

### Sequential (Recommended)
1. Complete Phase 1 (NLU) first - establishes patterns
2. Then complete Phase 2 (LLM) - follows same patterns

### Parallel (Swarm Mode)
- Agent 1: Tasks 1.1-1.5 (NLU infrastructure)
- Agent 2: Tasks 2.1-2.6 (LLM infrastructure)
- Merge: Tasks 1.6-1.8 and 2.7-2.8 (integration)

---

## Testing Checklist

### Unit Tests
- [ ] AndroidNluProcessor classification
- [ ] AndroidLlmProcessor interpretation
- [ ] VoiceCommandPrompt parsing
- [ ] NluResult/LlmResult handling

### Integration Tests
- [ ] Full pipeline: Registry → NLU → LLM → Fail
- [ ] Graceful degradation when NLU unavailable
- [ ] Graceful degradation when LLM unavailable
- [ ] Timeout handling

### Manual Tests
- [ ] "go to settings" matches "open settings" via NLU
- [ ] "I want to scroll down a bit" interpreted by LLM as "scroll down"
- [ ] Performance on mid-range device (latency < 200ms total)
