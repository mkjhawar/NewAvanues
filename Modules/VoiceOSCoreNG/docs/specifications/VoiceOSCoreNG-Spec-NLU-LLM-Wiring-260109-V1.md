# VoiceOSCoreNG NLU and LLM Integration Specification

**Version:** 1.0
**Date:** 2026-01-09
**Author:** VOS4 Development Team
**Status:** Draft

---

## 1. Executive Summary

This specification defines the integration of NLU (Natural Language Understanding) and LLM (Large Language Model) capabilities into VoiceOSCoreNG. The integration enables intelligent voice command interpretation beyond simple keyword matching, providing semantic understanding and natural language fallback when exact matches fail.

### 1.1 Scope

- **Phase 1:** Wire BERT-based NLU (IntentClassifier) for semantic intent classification
- **Phase 2:** Wire LLM (LocalLLMProvider) for natural language understanding fallback
- **Model Storage:** BERT model bundled in assets; LLM models loaded from external storage

---

## 2. Problem Statement

### Current State
VoiceOSCoreNG currently processes voice commands through:
1. Dynamic command registry lookup (exact/fuzzy phrase matching)
2. Static handler lookup (system commands)
3. DefaultVoiceCommandInterpreter (hardcoded phrase mappings)

### Limitations
- No semantic understanding - "open settings" doesn't match "go to settings"
- No intent classification - cannot disambiguate similar commands
- No natural language support - "I want to scroll down a bit" fails
- DefaultVoiceCommandInterpreter has limited coverage

### Desired State
- Semantic matching via BERT embeddings (NLU)
- Intent classification for command disambiguation
- Natural language fallback via LLM for complex queries
- Graceful degradation when models unavailable

---

## 3. Functional Requirements

### FR-1: NLU Integration (Phase 1)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | IntentClassifier shall classify voice input against available commands | P0 |
| FR-1.2 | Classification shall use BERT embeddings with cosine similarity | P0 |
| FR-1.3 | Confidence threshold shall be configurable (default: 0.6) | P1 |
| FR-1.4 | NLU shall fall back to keyword matching if embeddings unavailable | P1 |
| FR-1.5 | BERT model (mALBERT) shall be bundled in APK assets | P0 |
| FR-1.6 | NLU initialization shall be async and non-blocking | P0 |

### FR-2: LLM Integration (Phase 2)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | LocalLLMProvider shall be available as fallback interpreter | P0 |
| FR-2.2 | LLM shall receive NLU schema as context for command matching | P1 |
| FR-2.3 | LLM models shall be loaded from external storage `/sdcard/ava-ai-models/llm/` | P0 |
| FR-2.4 | LLM shall be optional - system works without it | P0 |
| FR-2.5 | LLM responses shall be streamed for responsive UX | P1 |
| FR-2.6 | LLM shall have response timeout (default: 10s) | P1 |

### FR-3: Processing Pipeline

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | Processing order: Registry → NLU → LLM → Fail | P0 |
| FR-3.2 | NLU shall only be invoked if registry match confidence < threshold | P1 |
| FR-3.3 | LLM shall only be invoked if NLU confidence < threshold | P1 |
| FR-3.4 | Each stage shall have independent timeout | P1 |

---

## 4. Non-Functional Requirements

### NFR-1: Performance

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-1.1 | NLU classification latency | < 100ms |
| NFR-1.2 | LLM first token latency | < 500ms |
| NFR-1.3 | NLU model load time | < 2s |
| NFR-1.4 | Memory overhead (NLU active) | < 50MB |

### NFR-2: Reliability

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-2.1 | System shall work without NLU model | 100% |
| NFR-2.2 | System shall work without LLM model | 100% |
| NFR-2.3 | Model loading failures shall not crash app | 100% |

### NFR-3: Storage

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-3.1 | BERT model size (bundled) | < 20MB |
| NFR-3.2 | LLM model location | External storage |
| NFR-3.3 | LLM model discovery | Automatic |

---

## 5. Technical Architecture

### 5.1 Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         VoiceOSCoreNG                               │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     ActionCoordinator                        │   │
│  │  processVoiceCommand(text) ─────────────────────────────►   │   │
│  └───────────────┬──────────────────┬───────────────────────────┘   │
│                  │                  │                               │
│         ┌────────▼────────┐  ┌──────▼───────┐                      │
│         │ CommandRegistry │  │ NLUProcessor │ ◄─── NEW             │
│         │  (Exact/Fuzzy)  │  │  (BERT/NLU)  │                      │
│         └────────┬────────┘  └──────┬───────┘                      │
│                  │                  │                               │
│                  │           ┌──────▼───────┐                      │
│                  │           │ LLMProcessor │ ◄─── NEW             │
│                  │           │  (Fallback)  │                      │
│                  │           └──────┬───────┘                      │
│                  │                  │                               │
│         ┌────────▼──────────────────▼────────┐                     │
│         │           HandlerRegistry          │                      │
│         │     (Execute matched command)      │                      │
│         └────────────────────────────────────┘                     │
└─────────────────────────────────────────────────────────────────────┘

External Modules:
┌──────────────────┐     ┌──────────────────┐
│  Shared/NLU      │     │  Modules/LLM     │
│  ├ IntentClass.  │     │  ├ LocalLLMProv. │
│  ├ BertTokenizer │     │  ├ ModelDiscovery│
│  └ OnnxSession   │     │  └ ALCEngine     │
└──────────────────┘     └──────────────────┘
```

### 5.2 Processing Flow

```
Voice Input
     │
     ▼
┌────────────────────┐
│  1. Registry Match │ ── confidence ≥ 0.85 ──► Execute
│  (CommandRegistry) │
└────────┬───────────┘
         │ confidence < 0.85
         ▼
┌────────────────────┐
│  2. NLU Classify   │ ── confidence ≥ 0.6 ──► Execute
│  (IntentClassifier)│
└────────┬───────────┘
         │ confidence < 0.6 OR NLU unavailable
         ▼
┌────────────────────┐
│  3. LLM Interpret  │ ── valid command ──► Execute
│  (LocalLLMProvider)│
└────────┬───────────┘
         │ LLM unavailable OR no match
         ▼
┌────────────────────┐
│  4. Fail Gracefully│ ── "Command not understood"
└────────────────────┘
```

### 5.3 Class Design

#### 5.3.1 New Interface: INluProcessor

```kotlin
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
        val intent: String?
    ) : NluResult()

    data class Ambiguous(
        val candidates: List<Pair<QuantizedCommand, Float>>
    ) : NluResult()

    data object NoMatch : NluResult()
    data class Error(val message: String) : NluResult()
}
```

#### 5.3.2 New Interface: ILlmProcessor

```kotlin
interface ILlmProcessor {
    suspend fun initialize(): Result<Unit>
    suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult
    fun isAvailable(): Boolean
    suspend fun dispose()
}

sealed class LlmResult {
    data class Interpreted(
        val matchedCommand: String,
        val confidence: Float,
        val explanation: String?
    ) : LlmResult()

    data object NoMatch : LlmResult()
    data class Error(val message: String) : LlmResult()
}
```

#### 5.3.3 Android Implementations

```kotlin
// NLU Processor using Shared/NLU module
class AndroidNluProcessor(
    private val context: Context,
    private val config: NluConfig = NluConfig.DEFAULT
) : INluProcessor {
    private var intentClassifier: IntentClassifier? = null
    // Wraps IntentClassifier from Shared/NLU
}

// LLM Processor using Modules/LLM module
class AndroidLlmProcessor(
    private val context: Context,
    private val config: LlmConfig = LlmConfig.DEFAULT
) : ILlmProcessor {
    private var llmProvider: LocalLLMProvider? = null
    // Wraps LocalLLMProvider from Modules/LLM
}
```

---

## 6. Integration Points

### 6.1 Gradle Dependencies

```kotlin
// VoiceOSCoreNG/build.gradle.kts
val androidMain by getting {
    dependencies {
        // Existing dependencies...

        // NLU (BERT-based intent classification)
        implementation(project(":Modules:Shared:NLU"))

        // LLM (Local language model for fallback)
        implementation(project(":Modules:LLM"))
    }
}
```

### 6.2 Model Assets

#### BERT NLU Model (Bundled)
```
VoiceOSCoreNG/
└── src/androidMain/assets/
    └── models/
        └── nlu/
            ├── malbert-intent-v1.onnx  (~15MB)
            └── vocab.txt               (~200KB)
```

#### LLM Model (External)
```
/sdcard/ava-ai-models/llm/
├── gemma-3n-2b-q4.tflite
├── phi-3-mini-q4.gguf
└── model-config.json
```

### 6.3 Configuration

```kotlin
data class NluConfig(
    val confidenceThreshold: Float = 0.6f,
    val embeddingDimension: Int = 384,
    val maxSequenceLength: Int = 64,
    val modelPath: String = "models/nlu/malbert-intent-v1.onnx",
    val vocabPath: String = "models/nlu/vocab.txt"
) {
    companion object {
        val DEFAULT = NluConfig()
    }
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

## 7. Phase 1: NLU Wiring

### 7.1 Tasks

| # | Task | Description |
|---|------|-------------|
| 1.1 | Add NLU dependency | Add `Shared:NLU` to build.gradle.kts |
| 1.2 | Create INluProcessor interface | Define common interface in commonMain |
| 1.3 | Implement AndroidNluProcessor | Wrap IntentClassifier for Android |
| 1.4 | Create stub NluProcessor | For iOS/Desktop platforms |
| 1.5 | Update VoiceOSCoreNG.Builder | Add optional NLU processor |
| 1.6 | Integrate in ActionCoordinator | Call NLU after registry lookup fails |
| 1.7 | Bundle BERT model | Add ONNX model to assets |
| 1.8 | Add NLU metrics | Track classification latency/accuracy |

### 7.2 Modified ActionCoordinator.processVoiceCommand()

```kotlin
suspend fun processVoiceCommand(text: String, confidence: Float = 1.0f): HandlerResult {
    val normalizedText = text.lowercase().trim()

    // Step 1: Dynamic command registry (existing)
    if (commandRegistry.size > 0) {
        val matchResult = CommandMatcher.match(normalizedText, commandRegistry, 0.85f)
        when (matchResult) {
            is CommandMatcher.MatchResult.Exact -> return processCommand(matchResult.command)
            is CommandMatcher.MatchResult.Fuzzy -> {
                if (matchResult.score >= 0.85f) {
                    return processCommand(matchResult.command)
                }
            }
            // Continue to NLU if low confidence
        }
    }

    // Step 2: NLU classification (NEW)
    val nluResult = nluProcessor?.classify(normalizedText, getAllQuantizedCommands())
    when (nluResult) {
        is NluResult.Match -> {
            if (nluResult.confidence >= nluConfig.confidenceThreshold) {
                return processCommand(nluResult.command)
            }
        }
        is NluResult.Ambiguous -> {
            return HandlerResult.awaitingSelection(
                message = "Multiple matches found",
                matchCount = nluResult.candidates.size
            )
        }
    }

    // Step 3: LLM fallback (Phase 2)
    // ...

    return HandlerResult.failure("Unknown command: $text")
}
```

---

## 8. Phase 2: LLM Wiring

### 8.1 Tasks

| # | Task | Description |
|---|------|-------------|
| 2.1 | Add LLM dependency | Add `Modules:LLM` to build.gradle.kts |
| 2.2 | Create ILlmProcessor interface | Define common interface in commonMain |
| 2.3 | Implement AndroidLlmProcessor | Wrap LocalLLMProvider for Android |
| 2.4 | Create stub LlmProcessor | For iOS/Desktop platforms |
| 2.5 | Update VoiceOSCoreNG.Builder | Add optional LLM processor |
| 2.6 | Create LLM prompt template | Voice command interpretation prompt |
| 2.7 | Integrate in ActionCoordinator | Call LLM after NLU fails |
| 2.8 | Handle model discovery | Check external storage for models |
| 2.9 | Add LLM metrics | Track inference latency |

### 8.2 LLM Prompt Template

```kotlin
object VoiceCommandPrompt {
    fun create(utterance: String, nluSchema: String): String = """
        You are a voice command interpreter for an accessibility app.

        Available commands:
        $nluSchema

        User said: "$utterance"

        Match this to the most appropriate command. Respond with ONLY the command phrase.
        If no command matches, respond with "NO_MATCH".

        Response:
    """.trimIndent()
}
```

---

## 9. Acceptance Criteria

### AC-1: NLU Integration
- [ ] IntentClassifier loads BERT model from assets
- [ ] Classification returns confidence scores
- [ ] "go to settings" matches "open settings" with confidence > 0.6
- [ ] NLU failure doesn't crash the app
- [ ] NLU latency < 100ms on mid-range device

### AC-2: LLM Integration
- [ ] LocalLLMProvider discovers models from external storage
- [ ] LLM interprets natural language commands
- [ ] "I want to scroll down" returns "scroll down"
- [ ] LLM timeout works (10s)
- [ ] LLM unavailable doesn't break command processing

### AC-3: Pipeline Integration
- [ ] Commands flow through Registry → NLU → LLM → Fail
- [ ] Each stage respects confidence thresholds
- [ ] Metrics track each stage's performance
- [ ] Debug info shows which stage matched

---

## 10. Out of Scope

- Cloud LLM providers (OpenAI, Anthropic) - external dependency
- NLU model training - use pre-trained mALBERT
- LLM model training - use pre-quantized models
- iOS/Desktop NLU implementation - stubs only for now
- Custom wake word training

---

## 11. Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Shared:NLU | local | BERT intent classification |
| Modules:LLM | local | Local LLM inference |
| onnxruntime-android | 1.16.0 | ONNX inference runtime |
| tensorflow-lite | 2.14.0 | TFLite for LLM |

---

## 12. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| BERT model too large for APK | High | Use quantized model (~15MB) |
| LLM latency too high | Medium | Add timeout, skip on slow devices |
| NLU accuracy insufficient | Medium | Fall back to keyword matching |
| Memory pressure | Medium | Lazy load, dispose when idle |

---

## Appendix A: Existing Module APIs

### A.1 IntentClassifier (Shared/NLU)

```kotlin
actual class IntentClassifier private constructor(
    private val context: Context,
    private val onnxSessionManager: OnnxSessionManager,
    private val embeddingManager: IntentEmbeddingManager
) {
    actual suspend fun classifyIntent(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification>

    suspend fun computeEmbedding(text: String): FloatArray?

    companion object {
        fun create(context: Context): Result<IntentClassifier>
    }
}

data class IntentClassification(
    val intent: String,
    val confidence: Float,
    val allScores: Map<String, Float>
)
```

### A.2 LocalLLMProvider (Modules/LLM)

```kotlin
class LocalLLMProvider(
    private val context: Context,
    private val autoModelSelection: Boolean = true,
    private val tokenCacheManager: TokenCacheManager? = null
) : LLMProvider {
    override suspend fun initialize(config: LLMConfig): Result<Unit>
    override suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions
    ): Flow<LLMResponse>
    override suspend fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse>
    override fun isModelLoaded(): Boolean
    override suspend fun shutdown()
}
```
