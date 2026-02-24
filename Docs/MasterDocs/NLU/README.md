# NLU Module - Natural Language Understanding

**Version:** 1.1 | **Platform:** Kotlin Multiplatform (Android, iOS, macOS, Desktop) | **Last Updated:** 2026-02-24

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
Modules/AI/NLU/
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
│   ├── androidMain/kotlin/com/augmentalis/nlu/
│   │   ├── IntentClassifier.kt      # ONNX inference (BERT embedding)
│   │   ├── BertTokenizer.kt         # WordPiece tokenization
│   │   ├── ModelManager.kt          # Model lifecycle
│   │   ├── learning/                # Self-learning
│   │   └── migration/               # VoiceOS bridge
│   │
│   ├── darwinMain/kotlin/com/augmentalis/nlu/  # Shared iOS + macOS
│   │   ├── BertTokenizer.kt         # Shared WordPiece tokenization (CoreML)
│   │   ├── IntentClassifier.kt      # Shared CoreML inference + keyword fallback
│   │   ├── NluLogger.darwin.kt      # Platform logging via NSLog
│   │   ├── coreml/                  # CoreML model loading
│   │   ├── learning/                # Learning domain shared impl
│   │   ├── locale/                  # NSLocale wrapper
│   │   ├── matching/                # Platform utilities
│   │   └── repository/              # UserDefaults-backed storage
│   │
│   ├── iosMain/kotlin/com/augmentalis/nlu/
│   │   ├── IntentClassifier.kt      # CoreML inference + keyword fallback
│   │   ├── BertTokenizer.kt         # WordPiece tokenization (CoreML)
│   │   ├── ModelManager.kt          # NSDocumentDirectory model storage
│   │   ├── coreml/CoreMLModelManager.kt  # CoreML model loading
│   │   ├── locale/LocaleManager.kt       # NSLocale wrapper
│   │   ├── matching/PlatformUtils.ios.kt # iOS platform utilities
│   │   ├── repository/IosIntentRepository.kt  # UserDefaults-backed
│   │   └── learning/domain/LearningDomainIos.kt
│   │
│   └── macosMain/kotlin/com/augmentalis/nlu/
│       ├── IntentClassifier.kt      # CoreML inference + keyword fallback
│       ├── BertTokenizer.kt         # WordPiece tokenization (stub — returns zeros)
│       ├── ModelManager.kt          # NSApplicationSupportDirectory storage
│       ├── coreml/CoreMLModelManager.kt  # CoreML model loading
│       ├── locale/LocaleManager.kt       # NSLocale wrapper
│       ├── matching/PlatformUtils.macos.kt # macOS platform utilities
│       ├── repository/MacosIntentRepository.kt  # UserDefaults-backed
│       └── learning/domain/LearningDomainMacos.kt
```

> **Note**: The `darwinMain` shared source set deduplicates ~1,500 lines between iOS and macOS. It provides shared implementations for WordPiece tokenization, CoreML inference, logging, and data access. Platform-specific overrides remain in `iosMain/` and `macosMain/`.

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

### iOS Classes

| Class | Purpose |
|-------|---------|
| `IntentClassifier` | CoreML inference + keyword fallback |
| `BertTokenizer` | WordPiece tokenization (CoreML) |
| `ModelManager` | NSDocumentDirectory model storage |
| `CoreMLModelManager` | CoreML model loading + `runInference()` |
| `LocaleManager` | NSLocale-backed locale management |
| `IosIntentRepository` | UserDefaults-backed intent storage |
| `LearningDomainIos` | iOS learning domain implementation |

### darwinMain Classes (iOS + macOS Shared)

| Class | Purpose |
|-------|---------|
| `BertTokenizer` | Shared WordPiece tokenization (CoreML) |
| `IntentClassifier` | Shared CoreML inference + keyword fallback |
| `NluLogger.darwin` | Platform logging via NSLog |
| `CoreMLModelManager` | Shared CoreML model loading |
| `LocaleManager` | Shared NSLocale wrapper |
| `IntentRepository` | Shared UserDefaults-backed storage |
| `LearningDomain` | Shared learning domain implementation |

> **Note**: The darwinMain `BertTokenizer` emits a one-time `nluLogWarn()` on the first `tokenize()` call when CoreML tensor interop is not fully configured. This is expected on macOS until the CoreML pipeline is complete.

### macOS Classes

| Class | Purpose |
|-------|---------|
| `IntentClassifier` | CoreML inference + keyword fallback |
| `BertTokenizer` | WordPiece tokenization (stub — returns zeros) |
| `ModelManager` | NSApplicationSupportDirectory storage |
| `CoreMLModelManager` | CoreML model loading |
| `LocaleManager` | NSLocale-backed locale management |
| `MacosIntentRepository` | UserDefaults-backed intent storage |
| `LearningDomainMacos` | macOS learning domain implementation |

> **macOS classification behavior**: When CoreML tensor interop is not configured (the `BertTokenizer` returns zero embeddings), `IntentClassifier.classifyIntent()` falls back to keyword matching via `computeKeywordScore()`. This provides basic intent classification (~70% accuracy) without requiring a fully configured CoreML pipeline.

### Cross-Platform Logging (NluLogger)

The NLU module uses an expect/actual logging abstraction to provide consistent, PII-safe logging across all platforms.

**API:**
- `nluLogDebug(tag: String, message: String)` — Debug-level messages
- `nluLogInfo(tag: String, message: String)` — Information messages
- `nluLogWarn(tag: String, message: String)` — Warning messages
- `nluLogError(tag: String, message: String, throwable: Throwable? = null)` — Error messages

**Implementations:**

| Platform | File | Wrapper |
|----------|------|---------|
| **commonMain** | `NluLogger.kt` | expect functions (abstract) |
| **androidMain** | `NluLogger.android.kt` | wraps `android.util.Log.d/i/w/e` |
| **darwinMain** | `NluLogger.darwin.kt` | wraps NSLog |
| **desktopMain** | `NluLogger.desktop.kt` | wraps `java.util.logging.Logger` |
| **jsMain** | `NluLogger.js.kt` | wraps `console.log/warn/error` |

**Usage:** All NLU code across all platforms should use `nluLogDebug()`, `nluLogInfo()`, `nluLogWarn()`, or `nluLogError()` instead of direct platform logging calls (e.g., `android.util.Log`, NSLog, `java.util.logging`). This ensures consistent behavior and makes it easier to add cross-cutting concerns like PII redaction.

### NluThresholds (Named Constants)

The NLU module defines all classification thresholds and tuning parameters in a single, centralized object to eliminate magic numbers and improve maintainability.

**File:** `commonMain/NluThresholds.kt`

**Organization:** 50+ named constants grouped into 13 semantic categories:

| Category | Key Constants | Purpose |
|----------|---------------|---------|
| **Classification Confidence** | `SEMANTIC_CONFIDENCE_THRESHOLD`, `KEYWORD_CONFIDENCE_THRESHOLD`, `HIGH_CONFIDENCE` | Control confidence floor for accepting classifications |
| **Exact Match / Fast Path** | `EXACT_MATCH_THRESHOLD`, `FAST_PATH_THRESHOLD`, `PREFIX_MATCH_MIN_SIMILARITY` | Define when to shortcut to Stage 1 (pattern matching) |
| **Fuzzy Matching** | `FUZZY_MIN_SIMILARITY`, `FUZZY_ACCEPT_THRESHOLD`, `FUZZY_MAX_DISTANCE` | Configure Levenshtein distance tuning (Stage 2) |
| **Semantic Matching** | `SEMANTIC_MIN_SIMILARITY`, `SEMANTIC_BOOST_FACTOR` | Cosine similarity floor for BERT embeddings (Stage 3) |
| **Hybrid / Ensemble** | `HYBRID_MIN_SCORE`, `HYBRID_WEIGHT_BALANCE` | Weight combination of all three matchers |
| **Ambiguity Detection** | `DEFAULT_AMBIGUITY_THRESHOLD`, `COMMAND_AMBIGUITY_THRESHOLD` | When to flag multiple possible intents |
| **BERT Verification** | `VERIFICATION_RANGE_LOW`, `VERIFICATION_RANGE_HIGH`, `AGREEMENT_BOOST` | Verify classification confidence with embedding agreement |
| **Strategy Weights** | `FUZZY_WEIGHT`, `SEMANTIC_WEIGHT`, `PATTERN_WEIGHT` | Per-classifier contribution to final score |
| **Priority Boost Factors** | `FREQUENTLY_USED_BOOST`, `CONTEXT_BOOST`, `RECENT_BOOST` | Increase confidence for high-priority intents |
| **Keyword Scoring** | `KEYWORD_MATCH_THRESHOLD`, `KEYWORD_EXACT_BOOST` | Fast keyword fallback scoring (especially for macOS) |
| **Calibration / Self-Learning** | `LEARNING_THRESHOLD`, `RETRAINING_SAMPLE_SIZE`, `CONFIDENCE_DECAY` | Configure active learning feedback loops |
| **Embedding Quality** | `EMBEDDING_NORM_TOLERANCE`, `EMBEDDING_DIM_MISMATCH_PENALTY` | Validate embedding vectors (BERT dimension check, normalization) |
| **Language Detection** | `SCRIPT_MAJORITY_THRESHOLD`, `LANGUAGE_CONFIDENCE_MIN` | Multi-language input handling and fallback |

**Usage Pattern:**

```kotlin
val hybridResult = classifier.classify(input)
if (hybridResult.confidence > NluThresholds.HIGH_CONFIDENCE) {
    // Highly confident — use immediately
    executeIntent(hybridResult)
} else if (hybridResult.confidence > NluThresholds.SEMANTIC_CONFIDENCE_THRESHOLD) {
    // Moderately confident — confirm with user
    askUserConfirmation(hybridResult)
} else {
    // Low confidence — escalate to keyword fallback
    keywordFallback(input)
}
```

**Benefits:** Each threshold is self-documenting and tunable in one location, eliminating hardcoded floats scattered across 8+ classifiers.

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

## PII-Safe Logging Policy

User utterances and sensitive data must never be logged verbatim in any NLU log statement. Instead, follow these patterns:

| Data Type | Bad | Good |
|-----------|-----|------|
| User utterance | `nluLogDebug("NLU", "input: $utterance")` | `nluLogDebug("NLU", "${utterance.length}-char input")` |
| Command text | `nluLogDebug("NLU", "matched: $command")` | `nluLogDebug("NLU", "matched command ID: ${command.id}")` |
| User preferences | `nluLogDebug("NLU", "user language: $lang")` | `nluLogDebug("NLU", "language code set")` |

**Scope:** This policy applies consistently across all platforms (androidMain, iosMain, darwinMain, desktopMain, jsMain). 14 call sites in `IntentClassifier`, `HybridClassifier`, `CommandMatchingService`, and `NluService` currently enforce this pattern.

**Justification:** User utterances may contain sensitive personal information (health conditions, financial details, identity information). Logging them verbatim creates an audit trail that violates user privacy. Length/ID logging provides debugging information without privacy risk.

**Related:** See SpeechRecognition Chapter 102, Section 9.5 (Cross-Platform Logging & PII Protection).

---

## Related Documentation

- [VoiceOSCoreNG](../VoiceOSCoreNG/README.md)
- [AVA Module](../AVA/README.md)
- [LLM Module](../LLM/README.md)

---

**Author:** Avanues NLU Team | **Last Updated:** 2026-02-24
