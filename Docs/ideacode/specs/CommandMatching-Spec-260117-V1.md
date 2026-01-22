# Command Matching Service - Specification

**Date:** 2026-01-17 | **Version:** V1 | **Status:** Draft

---

## Overview

Unified command matching service for voice recognition across all platforms and modules. Consolidates 6+ existing implementations into a single, multilingual-aware, multi-strategy matching system.

---

## Problem Statement

### Current State
- **6+ implementations** doing similar things (CommandCache, SimilarityMatcher, FuzzyMatcher, SemanticMatcher, CommandMatcher, HybridClassifier)
- **Critical gap**: Speech pipeline only does exact matching
- **SimilarityMatcher exists but NOT wired** to speech recognition
- **No unified API** for consumers (SpeechRecognition, VoiceOSCore, etc.)
- **Multilingual support fragmented** across implementations

### Target State
- Single `CommandMatchingService` in NLU module (KMP)
- Multi-strategy matching (exact → synonym → fuzzy → semantic → phoneme)
- Multilingual-aware normalization
- Self-learning from user corrections
- Direct integration with SpeechRecognition (no unnecessary bridge)

---

## Architecture

### Module Location
```
Modules/AI/NLU/src/
├── commonMain/kotlin/com/augmentalis/nlu/matching/
│   ├── CommandMatchingService.kt      # Main service
│   ├── MultilingualSupport.kt         # Normalization, synonyms, language detection
│   └── PhonemeMatching.kt             # Phoneme-based matching (future)
├── androidMain/kotlin/com/augmentalis/nlu/matching/
│   ├── PlatformUtils.android.kt       # Unicode normalization, time
│   └── SpeechRecognitionIntegration.kt # Direct integration with SpeechRecognition
├── iosMain/kotlin/.../
├── desktopMain/kotlin/.../
└── jsMain/kotlin/.../
```

### Why NLU Module?
1. Already KMP (works on all platforms)
2. Already has matchers (Pattern, Fuzzy, Semantic)
3. Is the "intelligence" layer - matching is intelligence
4. SpeechRecognition can depend on NLU
5. VoiceOSCore already depends on NLU

### Why Direct Integration (Not a Bridge)?

**Bridge pattern is unnecessary because:**

| Concern | Reality |
|---------|---------|
| Different platforms | NLU is KMP, has androidMain for Android-specific |
| Different data types | Can use extension functions to convert |
| Decoupling | Over-engineering for this use case |
| Testing | Can mock CommandMatchingService directly |

**Direct integration approach:**
```kotlin
// In SpeechRecognition module (Android)
// Just add NLU as a dependency and use CommandMatchingService directly

class ResultProcessor(
    private val commandCache: CommandCache,
    private val commandMatcher: CommandMatchingService? = null  // Optional NLU integration
) {
    fun processResult(...): RecognitionResult {
        // Try exact match first (existing behavior)
        commandCache.findMatch(normalizedText)?.let { return ... }

        // If no exact match and matcher available, try fuzzy/semantic
        commandMatcher?.match(normalizedText)?.let { result ->
            when (result) {
                is MatchResult.Fuzzy -> return createResult(result.command, result.confidence)
                is MatchResult.Ambiguous -> return createAmbiguousResult(result.candidates)
                else -> { /* fall through */ }
            }
        }

        return createNoMatchResult()
    }
}
```

---

## Matching Strategies

### Strategy Pipeline (Cascading)

```
Input Text
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 1: Normalization (Multilingual)                           │
│ - Unicode NFKC normalization                                    │
│ - Locale-aware lowercase                                        │
│ - Diacritics handling (language-dependent)                      │
│ - Character equivalencies (Arabic alif, Cyrillic ё)             │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 2: Learned Mappings (O(1))                                │
│ - Check user corrections cache                                  │
│ - "opn calculater" → "open calculator" (learned)                │
│ - Return immediately if found                                   │
└─────────────────────────────────────────────────────────────────┘
    │ Miss
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 3: Exact Match (O(1))                                     │
│ - HashMap lookup on normalized text                             │
│ - Return immediately if found                                   │
└─────────────────────────────────────────────────────────────────┘
    │ Miss
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 4: Synonym Expansion + Exact (O(n) synonyms)              │
│ - Expand "tap" → "click", "press" → "click"                     │
│ - Locale-aware synonyms                                         │
│ - Try exact match with expanded text                            │
└─────────────────────────────────────────────────────────────────┘
    │ Miss
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 5: Fuzzy Matching (O(n×m) per command)                    │
│ - Levenshtein distance (character-level typos)                  │
│ - Jaccard similarity (word-level, handles reordering)           │
│ - Return if confidence > 0.8                                    │
└─────────────────────────────────────────────────────────────────┘
    │ Low confidence or miss
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 6: Semantic Matching (O(embedding))                       │
│ - MobileBERT embeddings (384-dim)                               │
│ - Cosine similarity                                             │
│ - Handles paraphrasing: "go back" ≈ "return to previous"        │
└─────────────────────────────────────────────────────────────────┘
    │ Low confidence or miss
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 7: Ensemble Voting (if multiple candidates)               │
│ - Combine scores from fuzzy + semantic                          │
│ - Agreement bonus (multiple strategies agree)                   │
│ - Ambiguity detection (top 2 within 10%)                        │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Stage 8: Phoneme Matching (Future - for fixed commands)         │
│ - Audio → Phoneme extraction                                    │
│ - IPA pattern matching                                          │
│ - Bypasses text entirely for known commands                     │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
Result: Exact | Fuzzy | Ambiguous | NoMatch
```

### Algorithm Comparison

| Strategy | Complexity | Handles | Misses |
|----------|------------|---------|--------|
| Exact | O(1) | Perfect input | Any variation |
| Synonym | O(words) | Word substitutions | Typos |
| Levenshtein | O(m×n) | Typos, spelling | Word reordering |
| Jaccard | O(words) | Word reordering | Character typos |
| Semantic | O(embedding) | Paraphrasing | Rare words |
| Phoneme | O(phonemes) | Accents, homophones | Unknown commands |

---

## Multilingual Support

### Supported Locales (Initial)

| Locale | Script | RTL | Diacritics | Notes |
|--------|--------|-----|------------|-------|
| English | Latin | No | Remove | Default |
| Spanish | Latin | No | Remove | |
| French | Latin | No | Remove | |
| German | Latin | No | Remove | ß handling |
| Arabic | Arabic | Yes | Keep | Alif equivalencies |
| Hindi | Devanagari | No | Keep | |
| Chinese | CJK | No | N/A | Word segmentation needed |
| Japanese | CJK | No | N/A | Mixed scripts |

### Normalization Pipeline

```kotlin
fun normalize(text: String, locale: SupportedLocale): String {
    var result = text

    // 1. Unicode NFKC (compatibility decomposition + canonical composition)
    result = Normalizer.normalize(result, Normalizer.Form.NFKC)

    // 2. Locale-aware lowercase
    result = lowercaseLocaleAware(result, locale)  // Turkish İ→i, etc.

    // 3. Trim + collapse whitespace
    result = result.trim().replace(Regex("\\s+"), " ")

    // 4. Diacritics (locale-dependent)
    if (shouldRemoveDiacritics(locale)) {
        result = stripDiacritics(result)
    }

    // 5. Character equivalencies
    result = applyCharacterEquivalencies(result, locale)

    return result
}
```

### Locale-Specific Synonyms

```kotlin
// Example: "click" action synonyms per locale
val clickSynonyms = mapOf(
    ENGLISH to listOf("tap", "press", "hit", "touch", "select"),
    SPANISH to listOf("tocar", "pulsar", "presionar"),
    FRENCH to listOf("appuyer", "toucher"),
    GERMAN to listOf("drücken", "tippen"),
    ARABIC to listOf("اضغط", "انقر"),
    HINDI to listOf("दबाएं", "टैप करें"),
    CHINESE to listOf("点击", "按"),
    JAPANESE to listOf("タップ", "クリック", "押す")
)
```

---

## API Design

### Core Interface

```kotlin
class CommandMatchingService(config: MatchingConfig = MatchingConfig()) {

    // Registration
    fun registerCommands(commands: List<String>, priority: Int = 0)
    fun registerCommand(phrase: String, priority: Int, category: String?, actionId: String?, alternatives: List<String>)
    fun setSynonyms(synonymMap: Map<String, String>)
    fun setEmbeddingProvider(provider: EmbeddingProvider)

    // Matching
    fun match(input: String, strategies: Set<MatchStrategy>, locale: SupportedLocale?): MatchResult
    fun matchExact(input: String): String?  // Fast path
    fun matchWith(input: String, strategy: MatchStrategy): MatchResult  // Single strategy

    // Learning
    fun learn(misrecognized: String, correct: String)
    fun unlearn(misrecognized: String)
    fun getLearnedMappings(): Map<String, LearnedMapping>
    fun restoreLearnedMappings(mappings: Map<String, LearnedMapping>)

    // Configuration
    var defaultLocale: SupportedLocale

    // Statistics
    fun getStatistics(): MatchingStatistics
    fun resetStatistics()
    fun clear()
}
```

### Result Types

```kotlin
sealed class MatchResult {
    data class Exact(val command: String, val strategy: MatchStrategy, val metadata: Map<String, Any?>)
    data class Fuzzy(val command: String, val confidence: Float, val strategy: MatchStrategy, val metadata: Map<String, Any?>)
    data class Ambiguous(val candidates: List<AmbiguousCandidate>)
    data object NoMatch

    fun commandOrNull(): String?
    fun isMatch(): Boolean
}

enum class MatchStrategy {
    LEARNED, EXACT, SYNONYM, LEVENSHTEIN, JACCARD, SEMANTIC, PHONEME
}
```

---

## Integration Points

### 1. SpeechRecognition Module (Android)

```kotlin
// Option A: Constructor injection
class ResultProcessor(
    private val commandCache: CommandCache,
    private val commandMatcher: CommandMatchingService = CommandMatchingService()
) {
    init {
        // Sync commands from cache to matcher
        commandMatcher.registerCommands(commandCache.getAllCommands())
    }
}

// Option B: Extension function
fun ResultProcessor.withNLU(matcher: CommandMatchingService): ResultProcessor {
    // Configure matcher with current commands
    return this
}
```

### 2. VoiceOSCoreNG Module

```kotlin
// Replace existing CommandMatcher usage
object VoiceCommandProcessor {
    private val matcher = CommandMatchingService()

    fun process(voiceInput: String, registry: CommandRegistry): ProcessResult {
        // Register commands from registry
        matcher.registerCommands(registry.all().map { it.phrase })

        // Match
        return when (val result = matcher.match(voiceInput)) {
            is MatchResult.Exact -> ProcessResult.Matched(result.command)
            is MatchResult.Fuzzy -> ProcessResult.FuzzyMatched(result.command, result.confidence)
            is MatchResult.Ambiguous -> ProcessResult.NeedsClarification(result.candidates)
            is MatchResult.NoMatch -> ProcessResult.NotFound
        }
    }
}
```

### 3. HybridClassifier Integration

```kotlin
// HybridClassifier can delegate to CommandMatchingService for text matching
// while keeping its own ensemble logic and self-learning

class HybridClassifier {
    private val textMatcher = CommandMatchingService()

    fun classify(input: String, embedding: FloatArray?): ClassificationResult {
        // Use textMatcher for pattern/fuzzy/synonym
        // Use semantic matcher with provided embedding
        // Ensemble vote across all
    }
}
```

---

## Configuration

### Default Configuration

```kotlin
data class MatchingConfig(
    // Thresholds
    val fuzzyThreshold: Float = 0.7f,
    val semanticThreshold: Float = 0.6f,
    val minimumConfidence: Float = 0.5f,
    val ambiguityThreshold: Float = 0.1f,

    // Limits
    val maxCandidates: Int = 5,

    // Ensemble
    val agreementBonus: Float = 0.05f,

    // Enabled strategies (in order)
    val enabledStrategies: Set<MatchStrategy> = setOf(
        LEARNED, EXACT, SYNONYM, LEVENSHTEIN, JACCARD, SEMANTIC
    ),

    // Strategy weights for ensemble
    val strategyWeights: Map<MatchStrategy, Float> = mapOf(
        LEARNED to 1.0f,
        EXACT to 1.0f,
        SYNONYM to 0.95f,
        LEVENSHTEIN to 0.85f,
        JACCARD to 0.8f,
        SEMANTIC to 0.9f
    ),

    // Normalization
    val normalizationConfig: NormalizationConfig = NormalizationConfig()
)
```

---

## Testing Strategy

### Unit Tests

| Test Case | Input | Expected |
|-----------|-------|----------|
| Exact match | "open calculator" | Exact("open calculator") |
| Typo correction | "opn calculator" | Fuzzy("open calculator", ~0.85) |
| Synonym expansion | "tap settings" | Exact("click settings") via SYNONYM |
| Word reorder | "calculator open" | Fuzzy("open calculator") via JACCARD |
| Ambiguous | "open" (with "open camera", "open calendar") | Ambiguous([...]) |
| No match | "xyzzy foo bar" | NoMatch |
| Learned mapping | "opn calc" (after learning) | Exact("open calculator") via LEARNED |

### Multilingual Tests

| Locale | Input | Expected |
|--------|-------|----------|
| Arabic | "اضغط الإعدادات" | Exact("click settings") via SYNONYM |
| Spanish | "tocar configuración" | Exact("click settings") via SYNONYM |
| Hindi | "सेटिंग्स दबाएं" | Exact("click settings") via SYNONYM |
| Accented | "café" normalized | "cafe" (diacritics removed for Latin) |
| Turkish | "İstanbul" lowercase | "istanbul" (not "i̇stanbul") |

### Performance Benchmarks

| Scenario | Target | Measurement |
|----------|--------|-------------|
| Exact match | <1ms | Time from input to result |
| Fuzzy match (100 commands) | <10ms | Time from input to result |
| Semantic match | <50ms | Time from input to result |
| Full pipeline | <100ms | Time through all stages |

---

## Implementation Phases

### Phase 1: Core Service (Completed ✓)
- [x] `CommandMatchingService.kt` - Main service class
- [x] `MultilingualSupport.kt` - Normalization, synonyms, language detection
- [x] Platform implementations (Android, iOS, Desktop, JS)

### Phase 2: Integration (Next)
- [ ] Add `CommandMatchingService` to `SpeechRecognition/ResultProcessor`
- [ ] Update `VoiceOSCoreNG/CommandMatcher` to use service
- [ ] Add tests for integration points

### Phase 3: Phoneme Matching (Future)
- [ ] Phoneme extraction interface
- [ ] IPA command dictionary
- [ ] Phoneme pattern matching algorithm
- [ ] Integration with audio pipeline

---

## Files Created

| File | Purpose | Lines |
|------|---------|-------|
| `NLU/matching/CommandMatchingService.kt` | Main service | ~500 |
| `NLU/matching/MultilingualSupport.kt` | i18n support | ~300 |
| `NLU/matching/PlatformUtils.android.kt` | Android impl | ~20 |
| `NLU/matching/PlatformUtils.desktop.kt` | Desktop impl | ~20 |
| `NLU/matching/PlatformUtils.ios.kt` | iOS impl | ~25 |
| `NLU/matching/PlatformUtils.js.kt` | JS impl | ~20 |

---

## References

- [PATCorrect - Phoneme-augmented ASR](https://www.amazon.science/publications/patcorrect-non-autoregressive-phoneme-augmented-transformer-for-asr-error-correction)
- [Voice Activation Systems for Embedded Devices](https://content.iospress.com/articles/informatica/infor398)
- [Microsoft Phonetic Matching](https://www.microsoft.com/en-us/research/blog/a-phonetic-matching-made-in%CB%88h%C9%9Bv%C9%99n/)
- [Voiceflow Hybrid LLM Classification](https://www.voiceflow.com/pathways/benchmarking-hybrid-llm-classification-systems)
- [Levenshtein for Cross-linguistic Matching](https://aclanthology.org/N06-1060.pdf)

---

*Specification created: 2026-01-17 | Author: Claude*
