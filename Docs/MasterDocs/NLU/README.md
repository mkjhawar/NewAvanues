# NLU Module - Natural Language Understanding

**Version:** 1.0 | **Platform:** Kotlin Multiplatform | **Last Updated:** 2026-01-11

---

## Executive Summary

The **NLU (Natural Language Understanding) module** provides intent classification for the Avanues platform using on-device ONNX-based BERT models.

### Key Capabilities

| Capability | Description |
|------------|-------------|
| **Fast Pattern Matching** | ~1ms for exact commands |
| **Semantic Understanding** | ~50ms for novel phrasings via BERT |
| **Multilingual Support** | 52+ languages via mALBERT |
| **Hybrid Classification** | Pattern + fuzzy + semantic matching |
| **Self-Learning** | Continuous intent refinement |

### Performance Metrics

| Metric | Value |
|--------|-------|
| Intent Accuracy | 89% |
| Response Time | <100ms |
| Languages | 52+ |
| Model Size (mALBERT) | 41 MB |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    NLU Module (Shared)                       │
├─────────────────────────────────────────────────────────────┤
│  Input: User Utterance (Text)                                │
│                    │                                         │
│                    ▼                                         │
│             ┌──────────────┐                                 │
│             │  Tokenizer   │                                 │
│             │ (WordPiece)  │                                 │
│             └──────┬───────┘                                 │
│                    │                                         │
│     ┌──────────────┼──────────────┐                         │
│     │              │              │                         │
│  ┌──▼───┐    ┌─────▼─────┐   ┌───▼────┐                    │
│  │Exact │    │  Fuzzy    │   │Semantic│                    │
│  │Match │    │  Match    │   │ Match  │                    │
│  └──┬───┘    └─────┬─────┘   └───┬────┘                    │
│  (<1ms)      (5-20ms)       (30-50ms)                       │
│     │              │              │                         │
│     └──────────────┼──────────────┘                         │
│                    │                                         │
│           ┌────────▼─────────┐                              │
│           │ Hybrid Classifier│                              │
│           │ (Combine Results)│                              │
│           └────────┬─────────┘                              │
│                    │                                         │
│           Intent Classification                              │
└─────────────────────────────────────────────────────────────┘
```

### Three-Stage Classification Pipeline

```
Stage 1: Pattern Matching (Exact)
┌─────────────────────────────────────┐
│ Input: "go back"                    │
│ Check: Exact matches in pattern DB  │
│ Time: <1ms                          │
│ Confidence: 1.0 if match found      │
└─────────────────────────────────────┘
         │
         └─→ Match found? → Return ✓
             └─→ No match? → Stage 2

Stage 2: Fuzzy Matching (Levenshtein)
┌─────────────────────────────────────┐
│ Input: "go bck" (typo)              │
│ Algorithm: Levenshtein distance ≤ 3 │
│ Time: 5-20ms                        │
│ Confidence: 0.7-0.95                │
└─────────────────────────────────────┘
         │
         └─→ Match found? → Return ✓
             └─→ No match? → Stage 3

Stage 3: Semantic Matching (BERT)
┌─────────────────────────────────────┐
│ Input: "navigate backwards"         │
│ Compute: BERT embedding (384-dim)   │
│ Compare: Cosine similarity          │
│ Time: 30-50ms                       │
└─────────────────────────────────────┘
```

---

## Module Structure

```
Modules/Shared/NLU/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/shared/nlu/
│   │   ├── classifier/              # Hybrid classification
│   │   │   ├── HybridIntentClassifier.kt
│   │   │   └── EnhancedHybridClassifier.kt
│   │   ├── matcher/                 # Matching strategies
│   │   │   ├── PatternMatcher.kt
│   │   │   ├── FuzzyMatcher.kt
│   │   │   └── SemanticMatcher.kt
│   │   ├── model/                   # Data models
│   │   │   ├── UnifiedIntent.kt
│   │   │   └── IntentMatch.kt
│   │   ├── repository/              # Data access
│   │   └── service/                 # High-level APIs
│   │
│   └── androidMain/kotlin/com/augmentalis/nlu/
│       ├── IntentClassifier.kt      # ONNX inference
│       ├── BertTokenizer.kt         # WordPiece tokenization
│       ├── ModelManager.kt          # Model lifecycle
│       ├── learning/                # Self-learning
│       └── migration/               # VoiceOS bridge
```

---

## Class Inventory

### Common Classes

| Class | Purpose |
|-------|---------|
| `HybridIntentClassifier` | Multi-strategy matching engine |
| `PatternMatcher` | Exact pattern matching |
| `FuzzyMatcher` | Levenshtein fuzzy matching |
| `SemanticMatcher` | Embedding-based matching |
| `UnifiedNluService` | High-level NLU API |
| `BertTokenizer` | WordPiece tokenization |

### Android Classes

| Class | Purpose |
|-------|---------|
| `IntentClassifier` | ONNX inference engine |
| `ModelManager` | Model download & lifecycle |
| `IntentSourceCoordinator` | Multi-source intent loading |
| `ClassifyIntentUseCase` | High-level use case |
| `IntentLearningManager` | Self-learning support |

---

## Model Information

### MobileBERT (Lite)

| Property | Value |
|----------|-------|
| Size | 25 MB (INT8) |
| Languages | 1 (English) |
| Embedding Dim | 384 |
| Inference | <50ms |

### mALBERT (Full)

| Property | Value |
|----------|-------|
| Size | 41 MB (INT8) |
| Languages | 52 |
| Embedding Dim | 384 |
| Inference | <80ms |

**Supported Languages:** English, Spanish, French, German, Italian, Portuguese, Dutch, Polish, Romanian, Czech, Swedish, Japanese, Chinese, Korean, Thai, Vietnamese, Indonesian, Hindi, Arabic, Hebrew, Turkish, Russian, and 30+ more.

---

## API Reference

### ClassifyIntentUseCase

```kotlin
val useCase = ClassifyIntentUseCase.getInstance(context)

val result = useCase.execute("turn on lights", language = "en-US")

when (result) {
    is Result.Success -> {
        val intent = result.data
        println("Intent: ${intent.name}")
        println("Confidence: ${intent.confidence}")
    }
    is Result.Error -> println("Error: ${result.message}")
}
```

### HybridIntentClassifier

```kotlin
class HybridIntentClassifier(config: ClassifierConfig) {
    fun classify(input: String, inputEmbedding: FloatArray? = null): ClassificationResult
    fun classifyFast(input: String): IntentMatch?
    fun index(intents: List<UnifiedIntent>)
}

data class ClassificationResult(
    val matches: List<IntentMatch>,
    val method: MatchMethod,  // EXACT, FUZZY, SEMANTIC, HYBRID
    val confidence: Float,
    val processingTimeMs: Long
)
```

### Configuration

```kotlin
data class ClassifierConfig(
    val exactMatchThreshold: Float = 0.95f,
    val fuzzyMinSimilarity: Float = 0.7f,
    val fuzzyAcceptThreshold: Float = 0.85f,
    val semanticMinSimilarity: Float = 0.6f,
    val hybridMinScore: Float = 0.5f,
    val maxCandidates: Int = 5
)
```

---

## Usage Examples

### Basic Classification

```kotlin
val useCase = ClassifyIntentUseCase.getInstance(context)

val result = useCase.execute("go back", language = "en-US")
when (result) {
    is Result.Success -> handleIntent(result.data)
    is Result.Error -> showError(result.message)
}
```

### Hybrid Classification (VoiceOS + AVA)

```kotlin
val bridge = UnifiedNluBridge.getInstance(context)
bridge.initialize()

val result = bridge.classifyHybrid(
    utterance = "navigate backwards",
    intentClassifier = bertClassifier,
    candidateIntents = listOf("nav_back", "nav_home")
)

println("Intent: ${result.intent}")
println("Method: ${result.method}")  // PATTERN_ONLY, BERT_ONLY, HYBRID
println("Confidence: ${result.confidence}")
```

### Multilingual Support

```kotlin
if (useCase.supportsLanguage("es")) {
    val result = useCase.execute("enciende las luces", language = "es")
}

val languages = useCase.supportedLanguages()
println("Supported: ${languages.joinToString(", ")}")
```

---

## Training Data Format

### AVU Intent Format (.aai)

```
# Avanues Universal Format v1.0
schema: avu-1.0
locale: en-US
---
INT:nav_back:go back:navigation:10:GLOBAL_ACTION_BACK
PAT:nav_back:go back
PAT:nav_back:navigate back
PAT:nav_back:previous screen
SYN:nav_back:return
EMB:nav_back:mobilebert-384:384:QWFBYUFhQW...
```

### Format Codes

| Code | Name | Example |
|------|------|---------|
| `INT` | Intent | `INT:nav_back:go back:nav:10:BACK` |
| `PAT` | Pattern | `PAT:nav_back:previous` |
| `SYN` | Synonym | `SYN:nav_back:return` |
| `EMB` | Embedding | `EMB:nav_back:mobilebert-384:384:...` |

---

## Integration with VoiceOSCoreNG

```kotlin
class CommandProcessor(
    private val accessibilityService: AccessibilityService
) {
    private val nluUseCase = ClassifyIntentUseCase.getInstance(accessibilityService)

    suspend fun processCommand(voiceText: String) {
        val result = nluUseCase.execute(voiceText)
        when (result) {
            is Result.Success -> executeVoiceCommand(result.data)
            is Result.Error -> Log.e("VoiceOS", "NLU failed: ${result.message}")
        }
    }
}
```

---

## Performance

### Latency

| Operation | Time |
|-----------|------|
| Pattern match | <1ms |
| Fuzzy match | 5-20ms |
| Semantic match | 30-50ms |
| Hybrid (all) | 50-80ms |

### Memory

| Component | Size |
|-----------|------|
| MobileBERT | ~100 MB |
| mALBERT | ~200 MB |
| Pattern index | 5-10 MB |
| Embedding cache | ~50 MB |

---

## Related Documentation

- [VoiceOSCoreNG](../VoiceOSCoreNG/README.md)
- [AVA Module](../AVA/README.md)
- [LLM Module](../LLM/README.md)

---

**Author:** Avanues NLU Team | **Last Updated:** 2026-01-11
