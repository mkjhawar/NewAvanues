# Implementation Plan: VoiceOSCore AI Module Integration

**Date:** 2026-01-19
**Branch:** legacy-consolidation
**Mode:** .auto .cot .tasks

---

## Chain of Thought: Architecture Decision

### Problem Statement
VoiceOSCore needs NLU (intent classification) and LLM (command interpretation) capabilities. Previously, this was done through stub interfaces (`INluProcessor`, `ILlmProcessor`) that returned `NoMatch` - they were placeholders.

### Decision: Enrich AI Modules (Not Adapters)
**Reasoning:**
1. AI modules should be the **single source of truth** for AI capabilities
2. Command interpretation IS an AI capability, not a VoiceOS-specific thing
3. Other modules may need the same APIs in the future
4. Keeps VoiceOSCore thin - just orchestration, not AI logic
5. Dependency flows correctly: VoiceOSCore → AI (not reverse)

### API Design Approach
- AI modules expose **specialized command APIs** alongside generic APIs
- VoiceOSCore imports and calls AI module classes directly
- Result types defined in AI modules, used by VoiceOSCore
- No adapter classes needed - direct wiring

---

## Overview

| Metric | Value |
|--------|-------|
| Phases | 5 |
| Estimated Tasks | 18 |
| Platforms Affected | Android, iOS, Desktop |
| Modules Modified | AI/NLU, AI/LLM, VoiceOSCore |

---

## Phase 1: Enrich AI/NLU Module APIs (P0)

### Goal
Add command classification APIs to IntentClassifier that VoiceOSCore can call directly.

### Task 1.1: Add Command Result Types to NLU Module
**File:** `Modules/AI/NLU/src/commonMain/kotlin/com/augmentalis/nlu/CommandClassification.kt`

```kotlin
package com.augmentalis.nlu

/**
 * Result of command classification - what VoiceOSCore needs.
 */
sealed class CommandClassificationResult {
    /**
     * Confident match found.
     */
    data class Match(
        val commandId: String,
        val confidence: Float,
        val matchMethod: MatchMethod = MatchMethod.SEMANTIC
    ) : CommandClassificationResult()

    /**
     * Multiple candidates with similar confidence.
     */
    data class Ambiguous(
        val candidates: List<CommandCandidate>
    ) : CommandClassificationResult()

    /**
     * No matching command found.
     */
    data object NoMatch : CommandClassificationResult()

    /**
     * Error during classification.
     */
    data class Error(val message: String) : CommandClassificationResult()
}

data class CommandCandidate(
    val commandId: String,
    val confidence: Float,
    val phrase: String? = null
)

enum class MatchMethod {
    EXACT, FUZZY, SEMANTIC, HYBRID, LEARNED
}
```

### Task 1.2: Add classifyCommand() to IntentClassifier
**File:** `Modules/AI/NLU/src/commonMain/kotlin/com/augmentalis/nlu/IntentClassifier.kt`

Add to expect class:
```kotlin
/**
 * Classify utterance against command candidates.
 * Returns Match/Ambiguous/NoMatch/Error result.
 *
 * @param utterance User's voice input
 * @param commandPhrases List of command phrases to match against
 * @param confidenceThreshold Minimum confidence for a match (default 0.6)
 * @param ambiguityThreshold Gap between top candidates to consider ambiguous (default 0.15)
 * @return CommandClassificationResult
 */
suspend fun classifyCommand(
    utterance: String,
    commandPhrases: List<String>,
    confidenceThreshold: Float = 0.6f,
    ambiguityThreshold: Float = 0.15f
): CommandClassificationResult
```

### Task 1.3: Implement classifyCommand() for Android
**File:** `Modules/AI/NLU/src/androidMain/kotlin/com/augmentalis/nlu/IntentClassifier.kt`

```kotlin
actual suspend fun classifyCommand(
    utterance: String,
    commandPhrases: List<String>,
    confidenceThreshold: Float,
    ambiguityThreshold: Float
): CommandClassificationResult {
    if (commandPhrases.isEmpty()) {
        return CommandClassificationResult.NoMatch
    }

    return try {
        val result = classifyIntent(utterance, commandPhrases)
        result.fold(
            onSuccess = { classification ->
                val topScore = classification.confidence
                val allScores = classification.allScores

                when {
                    topScore < confidenceThreshold -> {
                        CommandClassificationResult.NoMatch
                    }
                    allScores.size > 1 -> {
                        // Check for ambiguity
                        val sorted = allScores.entries.sortedByDescending { it.value }
                        val gap = sorted[0].value - sorted.getOrNull(1)?.value ?: 0f
                        if (gap < ambiguityThreshold && sorted.size > 1) {
                            CommandClassificationResult.Ambiguous(
                                sorted.take(3).map { (cmd, score) ->
                                    CommandCandidate(cmd, score, cmd)
                                }
                            )
                        } else {
                            CommandClassificationResult.Match(
                                commandId = classification.intent,
                                confidence = topScore,
                                matchMethod = MatchMethod.SEMANTIC
                            )
                        }
                    }
                    else -> {
                        CommandClassificationResult.Match(
                            commandId = classification.intent,
                            confidence = topScore,
                            matchMethod = MatchMethod.SEMANTIC
                        )
                    }
                }
            },
            onFailure = { error ->
                CommandClassificationResult.Error(error.message ?: "Classification failed")
            }
        )
    } catch (e: Exception) {
        CommandClassificationResult.Error(e.message ?: "Classification error")
    }
}
```

### Task 1.4: Implement classifyCommand() for iOS/Desktop (Stub → Real)
**Files:**
- `Modules/AI/NLU/src/iosMain/kotlin/com/augmentalis/nlu/IntentClassifier.kt`
- `Modules/AI/NLU/src/desktopMain/kotlin/com/augmentalis/nlu/IntentClassifier.kt`

Similar implementation using platform-specific classifyIntent().

---

## Phase 2: Enrich AI/LLM Module APIs (P0)

### Goal
Add command interpretation APIs to LLMProvider that VoiceOSCore can call directly.

### Task 2.1: Add Command Interpretation Types to LLM Module
**File:** `Modules/AI/LLM/src/commonMain/kotlin/com/augmentalis/llm/CommandInterpretation.kt`

```kotlin
package com.augmentalis.llm

/**
 * Result of LLM command interpretation.
 */
sealed class CommandInterpretationResult {
    /**
     * Successfully interpreted the command.
     */
    data class Interpreted(
        val matchedCommand: String,
        val confidence: Float,
        val reasoning: String? = null
    ) : CommandInterpretationResult()

    /**
     * Could not determine a matching command.
     */
    data object NoMatch : CommandInterpretationResult()

    /**
     * Error during interpretation.
     */
    data class Error(val message: String) : CommandInterpretationResult()
}

/**
 * Result of LLM clarification for ambiguous commands.
 */
data class ClarificationResult(
    val selectedCommand: String?,
    val confidence: Float,
    val clarificationQuestion: String? = null
)
```

### Task 2.2: Add interpretCommand() to LLMProvider Interface
**File:** `Modules/AI/LLM/src/commonMain/kotlin/com/augmentalis/llm/LLMProvider.kt`

Add to interface:
```kotlin
/**
 * Interpret a natural language command against available options.
 * Uses LLM reasoning to match utterance to best command.
 *
 * @param utterance User's voice input
 * @param availableCommands List of valid command strings
 * @param context Optional context about current screen/state
 * @return CommandInterpretationResult
 */
suspend fun interpretCommand(
    utterance: String,
    availableCommands: List<String>,
    context: String? = null
): CommandInterpretationResult

/**
 * Clarify an ambiguous command by asking LLM to choose.
 *
 * @param utterance Original user input
 * @param candidates Ambiguous command candidates
 * @return ClarificationResult with selected command or question
 */
suspend fun clarifyCommand(
    utterance: String,
    candidates: List<String>
): ClarificationResult
```

### Task 2.3: Implement interpretCommand() in LocalLLMProvider
**File:** `Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/provider/LocalLLMProvider.kt`

```kotlin
override suspend fun interpretCommand(
    utterance: String,
    availableCommands: List<String>,
    context: String?
): CommandInterpretationResult {
    if (availableCommands.isEmpty()) {
        return CommandInterpretationResult.NoMatch
    }

    val prompt = buildCommandInterpretationPrompt(utterance, availableCommands, context)

    return try {
        val response = generateResponse(prompt, GenerationOptions(maxTokens = 50, temperature = 0.1f))
            .filterIsInstance<LLMResponse.Complete>()
            .firstOrNull()

        if (response != null) {
            parseCommandInterpretation(response.fullText, availableCommands)
        } else {
            CommandInterpretationResult.NoMatch
        }
    } catch (e: Exception) {
        CommandInterpretationResult.Error(e.message ?: "Interpretation failed")
    }
}

private fun buildCommandInterpretationPrompt(
    utterance: String,
    commands: List<String>,
    context: String?
): String {
    return """
        |Match the user's request to one of these commands:
        |${commands.mapIndexed { i, c -> "${i+1}. $c" }.joinToString("\n")}
        |
        |User said: "$utterance"
        |${context?.let { "Context: $it" } ?: ""}
        |
        |Reply with ONLY the matching command number, or "none" if no match.
    """.trimMargin()
}

private fun parseCommandInterpretation(
    response: String,
    commands: List<String>
): CommandInterpretationResult {
    val cleaned = response.trim().lowercase()

    if (cleaned == "none" || cleaned.contains("no match")) {
        return CommandInterpretationResult.NoMatch
    }

    // Try to parse number
    val number = cleaned.filter { it.isDigit() }.toIntOrNull()
    if (number != null && number in 1..commands.size) {
        return CommandInterpretationResult.Interpreted(
            matchedCommand = commands[number - 1],
            confidence = 0.7f,
            reasoning = "LLM selected option $number"
        )
    }

    // Try exact match
    commands.find { it.equals(cleaned, ignoreCase = true) }?.let {
        return CommandInterpretationResult.Interpreted(
            matchedCommand = it,
            confidence = 0.8f,
            reasoning = "LLM exact match"
        )
    }

    return CommandInterpretationResult.NoMatch
}
```

### Task 2.4: Implement clarifyCommand() in LocalLLMProvider
Similar pattern for disambiguation.

### Task 2.5: Add default implementations for other providers
**Files:**
- `OllamaProvider.kt` (Desktop)
- `CloudLLMProvider.kt` (fallback)

---

## Phase 3: Merge VoiceOSCoreNG into VoiceOSCore (P0)

### Goal
Consolidate the confusing VoiceOSCore.kt + VoiceOSCoreNG.kt into a single clean class.

### Task 3.1: Analyze Current Structure
- `VoiceOSCore.kt` - Just metadata singleton (VERSION, MODULE_NAME)
- `VoiceOSCoreNG.kt` - Main facade with Builder, coordinator, handlers

### Task 3.2: Merge VoiceOSCoreNG Content into VoiceOSCore
**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/VoiceOSCore.kt`

- Move all VoiceOSCoreNG class content into VoiceOSCore
- Keep VERSION and MODULE_NAME as companion object properties
- Update Builder to be inner class of VoiceOSCore
- Rename all internal references

### Task 3.3: Delete VoiceOSCoreNG.kt
**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/VoiceOSCoreNG.kt`

### Task 3.4: Update All References
- `VoiceOSCoreAndroidFactory.kt` → use VoiceOSCore
- `ActionCoordinator.kt` → update imports
- Any other files referencing VoiceOSCoreNG

---

## Phase 4: Direct Wire VoiceOSCore to AI APIs (P0)

### Goal
VoiceOSCore directly imports and uses AI module classes - no interfaces needed.

### Task 4.1: Update VoiceOSCore to Use IntentClassifier Directly
**File:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/VoiceOSCoreAndroid.kt`

```kotlin
import com.augmentalis.nlu.IntentClassifier
import com.augmentalis.nlu.CommandClassificationResult
import com.augmentalis.llm.provider.LocalLLMProvider
import com.augmentalis.llm.CommandInterpretationResult

class VoiceOSCoreAndroid(context: Context) {
    private val intentClassifier = IntentClassifier.getInstance(context)
    private val llmProvider = LocalLLMProvider(context)

    suspend fun processCommand(utterance: String, commands: List<String>): ProcessingResult {
        // 1. Try NLU classification first
        val nluResult = intentClassifier.classifyCommand(utterance, commands)

        when (nluResult) {
            is CommandClassificationResult.Match -> {
                return ProcessingResult.Success(nluResult.commandId, nluResult.confidence)
            }
            is CommandClassificationResult.Ambiguous -> {
                // 2. Use LLM to clarify
                val clarification = llmProvider.clarifyCommand(utterance, nluResult.candidates.map { it.commandId })
                // Handle clarification...
            }
            is CommandClassificationResult.NoMatch -> {
                // 3. Fallback to LLM interpretation
                val llmResult = llmProvider.interpretCommand(utterance, commands)
                // Handle LLM result...
            }
            is CommandClassificationResult.Error -> {
                return ProcessingResult.Error(nluResult.message)
            }
        }
    }
}
```

### Task 4.2: Update ActionCoordinator to Use AI Module Types
**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/ActionCoordinator.kt`

- Remove `INluProcessor` and `ILlmProcessor` parameters
- Remove `nluConfig` and `llmConfig` parameters
- Processing logic moves to platform-specific code (androidMain)

### Task 4.3: Create Platform-Specific AI Integration
**Android:** Uses IntentClassifier + LocalLLMProvider directly
**iOS:** Uses IntentClassifier (CoreML) directly
**Desktop:** Uses IntentClassifier + OllamaProvider directly

---

## Phase 5: Cleanup and Verification (P0)

### Goal
Remove orphaned code and verify everything compiles.

### Task 5.1: Delete Orphaned Interface Files (Already Done)
Verify these are deleted:
- `ILlmProcessor.kt`
- `INluProcessor.kt`
- All factory files
- `StubVivokaEngine.kt`

### Task 5.2: Remove Stub Classes from Interface Files (Already Done)
Verify:
- `StubResourceMonitor` removed from `IResourceMonitor.kt`
- `StubAppVersionDetector` removed from `IAppVersionDetector.kt`

### Task 5.3: Clean Up Empty Directories
```bash
rm -rf Modules/VoiceOSCore/src/commonMain/sqldelight/
rm -rf Modules/VoiceOSCore/src/androidMain/aidl/
```

### Task 5.4: Verify Compilation - All Platforms
```bash
./gradlew :Modules:AI:NLU:compileDebugKotlinAndroid
./gradlew :Modules:AI:LLM:compileDebugKotlinAndroid
./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid
./gradlew :Modules:VoiceOSCore:compileKotlinIosArm64
./gradlew :Modules:VoiceOSCore:compileKotlinDesktop
```

### Task 5.5: Update Build Dependencies
Ensure `build.gradle.kts` has correct dependencies:
- VoiceOSCore depends on AI:NLU and AI:LLM
- Already added in Phase 2 of original plan

---

## Execution Order

| # | Phase | Priority | Dependencies |
|---|-------|----------|--------------|
| 1 | Enrich AI/NLU APIs | P0 | None |
| 2 | Enrich AI/LLM APIs | P0 | None (parallel with Phase 1) |
| 3 | Merge VoiceOSCoreNG | P0 | Phases 1, 2 |
| 4 | Direct Wire VoiceOSCore | P0 | Phase 3 |
| 5 | Cleanup & Verify | P0 | Phase 4 |

---

## Verification Checklist

- [ ] `CommandClassificationResult` type exists in AI/NLU
- [ ] `classifyCommand()` method exists in IntentClassifier (all platforms)
- [ ] `CommandInterpretationResult` type exists in AI/LLM
- [ ] `interpretCommand()` and `clarifyCommand()` exist in LLMProvider
- [ ] VoiceOSCoreNG.kt deleted
- [ ] VoiceOSCore.kt contains merged facade class
- [ ] All references to VoiceOSCoreNG updated
- [ ] No references to INluProcessor or ILlmProcessor remain
- [ ] Android compiles
- [ ] iOS compiles
- [ ] Desktop compiles

---

## Summary

| Metric | Before | After |
|--------|--------|-------|
| Stub interfaces | 2 (INluProcessor, ILlmProcessor) | 0 |
| Factory classes | 6 | 0 |
| Adapter layers | 0 | 0 |
| Direct AI module usage | No | Yes |
| VoiceOSCore files | 2 (Core + NG) | 1 |
| AI module command APIs | 0 | 4 (classify, interpret, clarify, results) |

**Result:** Clean, direct integration between VoiceOSCore and AI modules with no abstraction overhead.

---

**Plan Status:** Ready for execution
**Mode:** .auto - Will execute autonomously
