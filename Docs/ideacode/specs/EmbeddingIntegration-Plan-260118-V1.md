# Pre-trained Embeddings for Dynamic Command Matching

**Date:** 2026-01-18 | **Version:** V1 | **Status:** Draft

---

## Executive Summary

This plan addresses integrating pre-trained multilingual embeddings into CommandMatchingService for **dynamic command matching** (UI elements, app-specific commands) while maintaining mobile performance constraints.

**Key Questions Addressed:**
1. Which embedding model for mobile? → **distiluse-base-multilingual** (512-dim, ONNX, 93% faster)
2. Do embeddings replace ASR? → **No** - ASR still needed for dictation; embeddings enhance command matching
3. Vosk + Semantic hybrid? → **Yes** - Recommended architecture for commands
4. Phoneme for dictation? → **Limited** - Phoneme works for fixed commands, not open vocabulary

---

## Mobile Constraints

| Constraint | Target | Impact |
|------------|--------|--------|
| Model size | <50MB | Rules out full XLM-RoBERTa (559MB) |
| Inference latency | <50ms | Requires quantization + ONNX |
| CPU usage | <20% sustained | Must cache embeddings, batch when possible |
| Memory footprint | <100MB RAM | Pre-compute command embeddings at startup |
| Battery impact | Minimal | Avoid continuous GPU use |

---

## Embedding Model Comparison

### Models Evaluated

| Model | Dimensions | Size | Languages | Mobile Latency | Quality |
|-------|------------|------|-----------|----------------|---------|
| FastText (Aligned) | 300 | 2.3GB raw | 157 | ~5ms (word) | Medium |
| FastText (Compressed) | 300 | ~50MB | 1 per file | ~5ms | Medium |
| **distiluse-multilingual-v2** | 512 | 480MB (ONNX quantized: ~120MB) | 50+ | ~30ms | High |
| mBERT | 768 | 700MB | 104 | ~100ms | High |
| XLM-RoBERTa | 768 | 559MB | 100 | ~150ms | Highest |
| MiniLM-L6 | 384 | 80MB (22MB quantized) | English | ~10ms | Medium |
| paraphrase-multilingual-MiniLM | 384 | 418MB (quantized: ~100MB) | 50+ | ~20ms | Good |

### Recommendation: Hybrid Approach

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     MOBILE EMBEDDING ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Tier 1: FastText Aligned (Primary - Static Commands)                   │  │
│  │ • 300-dim word vectors, ~50MB per language                            │  │
│  │ • Pre-aligned across languages (cross-lingual similarity)             │  │
│  │ • <5ms per word, average for sentence                                 │  │
│  │ • Use for: "open settings", "scroll down", "go back"                  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│       │ If confidence < 0.7                                                  │
│       ▼                                                                      │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Tier 2: MiniLM Quantized (Dynamic UI Elements)                         │  │
│  │ • 384-dim sentence embeddings, ~22MB INT8 quantized                   │  │
│  │ • ONNX Runtime Mobile optimized                                        │  │
│  │ • ~10ms inference on mobile                                           │  │
│  │ • Use for: Dynamic UI → "Settings" ≈ "configuration" ≈ "preferences"  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│       │ If multi-lingual needed                                              │
│       ▼                                                                      │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Tier 3: distiluse-multilingual (Server/Optional)                       │  │
│  │ • 512-dim, 50+ languages, highest quality                             │  │
│  │ • Use for: Complex semantic matching, edge cases                       │  │
│  │ • Can be server-side for non-latency-critical                         │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ASR Architecture Decision

### Question: Do embeddings replace ASR?

**Answer: No, but they change HOW we use ASR.**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     CURRENT ARCHITECTURE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Audio ─▶ Google/Android ASR ─▶ Text ─▶ Exact Match ─▶ Command              │
│                                                                              │
│  Problems:                                                                   │
│  • ASR trained on general speech, not commands                              │
│  • "Settings" → "settings" (exact match only)                               │
│  • "configuration" → NO MATCH (even though semantically same)               │
│  • Heavy ASR dependency, vendor lock-in                                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                     PROPOSED HYBRID ARCHITECTURE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                        ┌──────────────┐                                     │
│                        │   Audio In   │                                     │
│                        └──────┬───────┘                                     │
│                               │                                              │
│                ┌──────────────┴──────────────┐                              │
│                │       Speech Mode?          │                              │
│                └──────────────┬──────────────┘                              │
│                               │                                              │
│        ┌──────────────────────┼──────────────────────┐                      │
│        │ COMMAND              │ DICTATION            │                      │
│        ▼                      ▼                      │                      │
│  ┌───────────────┐    ┌───────────────────────┐     │                      │
│  │ Vosk (Light)  │    │ Google/Whisper (Full) │     │                      │
│  │ Local, Fast   │    │ Cloud, Accurate       │     │                      │
│  │ ~20MB model   │    │ General vocabulary    │     │                      │
│  └───────┬───────┘    └───────────┬───────────┘     │                      │
│          │                        │                  │                      │
│          ▼                        ▼                  │                      │
│  ┌───────────────────┐    ┌───────────────────┐     │                      │
│  │ Semantic Matcher  │    │ Direct Text Out   │     │                      │
│  │ (Embeddings)      │    │                   │     │                      │
│  │ • FastText/MiniLM │    │ User dictates:    │     │                      │
│  │ • Dynamic UI match│    │ "Send email to    │     │                      │
│  │ • Fuzzy fallback  │    │  John about the   │     │                      │
│  └───────┬───────────┘    │  meeting"         │     │                      │
│          │                └───────────────────┘     │                      │
│          ▼                                          │                      │
│  ┌───────────────────┐                              │                      │
│  │ Command Execution │                              │                      │
│  │ "open settings"   │                              │                      │
│  │ "tap preferences" │                              │                      │
│  │ "scroll down"     │                              │                      │
│  └───────────────────┘                              │                      │
│                                                      │                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Vosk + Semantic for Commands

**Why Vosk?**
| Advantage | Explanation |
|-----------|-------------|
| Offline | No network latency |
| Small models | English ~50MB, lightweight variants ~20MB |
| Fast | <100ms recognition |
| Customizable | Can train on command vocabulary |
| Open source | No vendor lock-in, MIT license |

**Vosk + Semantic Pipeline:**

```kotlin
// Proposed CommandRecognitionPipeline
class CommandRecognitionPipeline(
    private val vosk: VoskRecognizer,
    private val matcher: CommandMatchingService,
    private val uiEmbeddings: UIElementEmbeddings
) {
    suspend fun recognizeCommand(audio: ByteArray): CommandResult {
        // Step 1: Vosk transcription (local, fast)
        val transcript = vosk.recognize(audio)  // "tap configuration"

        // Step 2: Get current screen UI elements
        val screenElements = uiEmbeddings.getCurrentScreen()
        // ["Settings", "Profile", "About", "Help"]

        // Step 3: Semantic match against UI elements
        val inputEmbedding = embedder.embed(transcript)  // 384-dim

        val matches = screenElements.map { element ->
            element to cosineSimilarity(inputEmbedding, element.embedding)
        }.sortedByDescending { it.second }

        // "tap configuration" → "Settings" (similarity: 0.87)

        return if (matches.first().second > 0.7) {
            CommandResult.Match(matches.first().first, matches.first().second)
        } else {
            // Fallback to fuzzy text matching
            matcher.match(transcript)
        }
    }
}
```

---

## Phoneme-Based Matching: Dictation Viability

### Question: Can phoneme matching work for dictation?

**Answer: No - phoneme matching is for fixed commands only.**

| Aspect | Phoneme Matching | ASR (Dictation) |
|--------|------------------|-----------------|
| Vocabulary | Fixed (~100-1000 commands) | Open (unlimited) |
| Matching | Pattern match against known phoneme sequences | Statistical language model |
| Memory | Small (phoneme dictionary) | Large (language model) |
| Accuracy | High for known commands | Variable for any text |
| Use case | "scroll down", "go back" | "Send email to John about..." |

**Why phoneme works for commands but not dictation:**

```
PHONEME MATCHING (Fixed Commands)
─────────────────────────────────
Audio: [sound wave]
   │
   ▼
Phoneme Extractor: "S K R OW L D AW N"
   │
   ▼
Pattern Dictionary:
  "S K R OW L D AW N" → scroll_down  ✓
  "G OW B AE K"       → go_back
  "N EH K S T"        → next
   │
   ▼
Result: scroll_down (O(1) lookup)

DICTATION (Open Vocabulary)
─────────────────────────────
Audio: [sound wave]
   │
   ▼
Phonemes: "S EH N D AH N IY M EY L T UW JH AA N..."
   │
   ▼
Problem: Infinite combinations possible
  "send a male to john" ✗
  "send an email to john" ✓
  "send any mail to john" ✗
   │
   ▼
Requires: Statistical language model + context

→ Phoneme alone cannot disambiguate open vocabulary
→ Need ASR's language model for dictation
```

### Recommendation: Hybrid Usage

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     RECOMMENDED ARCHITECTURE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ COMMAND MODE                                                           │  │
│  │                                                                        │  │
│  │ Option A: Vosk (Light) → Semantic Embeddings → Command                 │  │
│  │           Fast, offline, good for dynamic UI matching                  │  │
│  │                                                                        │  │
│  │ Option B: Phoneme Extraction → Pattern Match → Command (Phase 5)      │  │
│  │           Fastest, offline, best for fixed 100 commands                │  │
│  │                                                                        │  │
│  │ Combination: Phoneme for fixed → Vosk+Semantic for dynamic            │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ DICTATION MODE                                                         │  │
│  │                                                                        │  │
│  │ Online: Google Cloud Speech / Whisper API                              │  │
│  │         High accuracy, streaming, punctuation                          │  │
│  │                                                                        │  │
│  │ Offline: Vosk (Full model) / Whisper.cpp                               │  │
│  │          ~500MB model, good accuracy, slower                           │  │
│  │                                                                        │  │
│  │ → Phoneme NOT viable for dictation (open vocabulary)                   │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Dynamic UI Element Matching

### The Key Use Case

User says: "tap configuration"
Screen shows: `[Settings] [Profile] [About]`
Expected: Click on "Settings" button

```kotlin
// UIElementEmbeddings - Pre-compute embeddings for screen elements
class UIElementEmbeddings(
    private val embedder: SentenceEmbedder  // MiniLM quantized
) {
    private val cache = mutableMapOf<String, FloatArray>()

    fun embedScreenElements(elements: List<UIElement>): List<EmbeddedElement> {
        return elements.map { element ->
            val text = element.contentDescription ?: element.text ?: element.id
            val embedding = cache.getOrPut(text) { embedder.embed(text) }
            EmbeddedElement(element, embedding)
        }
    }

    fun findMatch(
        spokenCommand: String,
        screenElements: List<EmbeddedElement>,
        threshold: Float = 0.7f
    ): MatchResult {
        val commandEmbedding = embedder.embed(spokenCommand)

        val scored = screenElements.map { element ->
            element to cosineSimilarity(commandEmbedding, element.embedding)
        }.sortedByDescending { it.second }

        return when {
            scored.isEmpty() -> MatchResult.NoMatch
            scored[0].second >= threshold -> MatchResult.Match(scored[0].first)
            scored[0].second - scored.getOrNull(1)?.second ?: 0f < 0.1f ->
                MatchResult.Ambiguous(scored.take(3))
            else -> MatchResult.LowConfidence(scored[0].first, scored[0].second)
        }
    }
}

// Semantic similarity examples (MiniLM):
// "settings"       ↔ "configuration"    = 0.89
// "settings"       ↔ "preferences"      = 0.85
// "tap settings"   ↔ "click settings"   = 0.96
// "open camera"    ↔ "take photo"       = 0.82
// "go back"        ↔ "return"           = 0.88
// "scroll down"    ↔ "Settings button"  = 0.23 (low - different intent)
```

### Cross-Lingual Dynamic Matching

With FastText aligned vectors or multilingual MiniLM:

```
User speaks (Spanish): "tocar configuración"
Screen (English):      [Settings] [Profile] [About]
                              ↑
Embedding similarity:  "configuración" ↔ "Settings" = 0.84 ✓

User speaks (Arabic):  "افتح الإعدادات"
Screen (English):      [Settings] [Profile] [About]
                              ↑
Embedding similarity:  "الإعدادات" ↔ "Settings" = 0.81 ✓
```

---

## Implementation Plan

### Phase 2A: Add Embedding Layer to CommandMatchingService

**Tasks:**

| # | Task | Effort |
|---|------|--------|
| 2A.1 | Add EmbeddingProvider interface to CommandMatchingService | 30 min |
| 2A.2 | Implement FastTextEmbedder (word vectors, 300-dim) | 2 hours |
| 2A.3 | Implement MiniLMEmbedder (sentence, 384-dim, ONNX) | 3 hours |
| 2A.4 | Add semantic stage to match() pipeline | 1 hour |
| 2A.5 | Implement UIElementEmbeddings for dynamic matching | 2 hours |
| 2A.6 | Add embedding caching with LRU eviction | 1 hour |
| 2A.7 | Performance testing on mobile devices | 2 hours |

**Total:** ~11 hours

### Phase 2B: Vosk Integration for Commands

**Tasks:**

| # | Task | Effort |
|---|------|--------|
| 2B.1 | Add Vosk KMP wrapper (expect/actual) | 2 hours |
| 2B.2 | Create lightweight command vocabulary model | 1 hour |
| 2B.3 | Implement CommandRecognitionPipeline | 2 hours |
| 2B.4 | Wire Vosk output → CommandMatchingService → UI | 1 hour |
| 2B.5 | A/B test vs Google ASR for command accuracy | 2 hours |

**Total:** ~8 hours

### Phase 2C: UI Element Integration

**Tasks:**

| # | Task | Effort |
|---|------|--------|
| 2C.1 | Create AccessibilityService → UIElement extractor | 2 hours |
| 2C.2 | Pre-compute embeddings on screen change | 1 hour |
| 2C.3 | Integrate with VoiceOSCoreNG overlay service | 2 hours |
| 2C.4 | Handle dynamic content (RecyclerView items) | 2 hours |
| 2C.5 | Test with various apps (Settings, Chrome, etc.) | 2 hours |

**Total:** ~9 hours

---

## Model Files

### Recommended Downloads

| Model | File | Size | Use |
|-------|------|------|-----|
| FastText EN | `cc.en.300.vec.gz` | 1.5GB → 50MB pruned | Static commands |
| FastText ES | `cc.es.300.vec.gz` | 1.5GB → 50MB pruned | Spanish |
| MiniLM Quantized | `all-MiniLM-L6-v2-int8.onnx` | 22MB | Dynamic UI |
| Vosk Small EN | `vosk-model-small-en-us-0.15` | 40MB | Command ASR |

### Model Storage Strategy

```
app/
├── assets/
│   └── models/
│       ├── fasttext/
│       │   ├── en.pruned.bin     # 50MB - English word vectors (top 100k)
│       │   ├── es.pruned.bin     # 50MB - Spanish (downloaded on demand)
│       │   └── ar.pruned.bin     # 50MB - Arabic (downloaded on demand)
│       ├── sentence/
│       │   └── minilm-l6-int8.onnx  # 22MB - Sentence embeddings
│       └── vosk/
│           └── small-en/          # 40MB - Command recognition
│
└── cache/
    └── embeddings/
        ├── commands.emb          # Pre-computed command embeddings
        └── ui_cache.emb          # LRU cache of UI element embeddings
```

**Total on-device:** ~112MB (single language) to ~212MB (3 languages)

---

## API Changes

### CommandMatchingService Updates

```kotlin
// New: EmbeddingProvider interface
interface EmbeddingProvider {
    fun embed(text: String): FloatArray
    fun embedBatch(texts: List<String>): List<FloatArray>
    val dimensions: Int
    val isReady: Boolean
}

// CommandMatchingService additions
class CommandMatchingService(
    private val config: MatchingConfig = MatchingConfig()
) {
    // Existing
    private val normalizer = MultilingualNormalizer(config.normalizationConfig)
    private val synonymProvider = LocalizedSynonymProvider()

    // New: Embedding support
    private var embeddingProvider: EmbeddingProvider? = null
    private val embeddingCache = LruCache<String, FloatArray>(1000)
    private val commandEmbeddings = mutableMapOf<String, FloatArray>()

    fun setEmbeddingProvider(provider: EmbeddingProvider) {
        this.embeddingProvider = provider
        recomputeCommandEmbeddings()
    }

    private fun recomputeCommandEmbeddings() {
        embeddingProvider?.let { provider ->
            val commands = registeredCommands.toList()
            val embeddings = provider.embedBatch(commands)
            commands.zip(embeddings).forEach { (cmd, emb) ->
                commandEmbeddings[cmd] = emb
            }
        }
    }

    // New: Match against dynamic UI elements
    fun matchDynamic(
        input: String,
        candidates: List<String>,
        threshold: Float = 0.7f
    ): MatchResult {
        val provider = embeddingProvider ?: return MatchResult.NoMatch

        val inputEmb = embeddingCache.getOrPut(input) { provider.embed(input) }
        val candidateEmbs = candidates.map { c ->
            c to (embeddingCache.getOrPut(c) { provider.embed(c) })
        }

        val scored = candidateEmbs.map { (text, emb) ->
            text to cosineSimilarity(inputEmb, emb)
        }.sortedByDescending { it.second }

        // ... return appropriate MatchResult
    }
}
```

---

## Performance Benchmarks (Target)

| Operation | Target | Measurement |
|-----------|--------|-------------|
| FastText word lookup | <5ms | Single word embedding |
| MiniLM inference | <15ms | 10-word sentence |
| UI element embedding (5 elements) | <20ms | Batch embed |
| Full command match (all strategies) | <50ms | End-to-end |
| Vosk transcription | <100ms | 2-second audio |
| Memory footprint (embeddings) | <50MB | Runtime RAM |

---

## Summary: Complete Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     VOICE COMMAND MATCHING ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  AUDIO INPUT                                                                 │
│       │                                                                      │
│       ├───────────────────┬──────────────────────────────────────┐          │
│       │                   │                                      │          │
│       ▼                   ▼                                      ▼          │
│  ┌─────────┐       ┌───────────┐                        ┌──────────────┐   │
│  │ PHONEME │       │   VOSK    │                        │ CLOUD ASR    │   │
│  │ Extract │       │  (Local)  │                        │ (Dictation)  │   │
│  │ Phase 5 │       │  40MB     │                        │              │   │
│  └────┬────┘       └─────┬─────┘                        └──────────────┘   │
│       │                  │                                                   │
│       │ Fixed            │ Any                                              │
│       │ Commands         │ Commands                                          │
│       │                  │                                                   │
│       ▼                  ▼                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    CommandMatchingService                            │   │
│  │  ┌─────────────────────────────────────────────────────────────┐    │   │
│  │  │ Stage 1: Learned Mappings     (O(1))                        │    │   │
│  │  │ Stage 2: Exact Match          (O(1))                        │    │   │
│  │  │ Stage 3: Synonym Expansion    (locale-aware)                │    │   │
│  │  │ Stage 4: Fuzzy (Levenshtein)  (typo tolerance)              │    │   │
│  │  │ Stage 5: Fuzzy (Jaccard)      (word reorder)                │    │   │
│  │  │ Stage 6: SEMANTIC (NEW)       (embeddings)                  │    │   │
│  │  │          ├─ FastText (words)   → Static commands            │    │   │
│  │  │          └─ MiniLM (sentences) → Dynamic UI matching        │    │   │
│  │  │ Stage 7: Phoneme Match        (audio patterns) [Phase 5]    │    │   │
│  │  │ Stage 8: Ensemble Vote        (combine scores)              │    │   │
│  │  └─────────────────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    UI Element Matching                               │   │
│  │  Screen: [Settings] [Profile] [About]                               │   │
│  │  Input:  "tap configuration"                                         │   │
│  │  Match:  "Settings" (similarity: 0.89) ✓                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    Action Execution                                  │   │
│  │  PerformClick(Settings), PerformScroll(Down), etc.                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Decision Summary

| Question | Decision |
|----------|----------|
| Do embeddings replace ASR? | **No** - ASR still needed for dictation |
| Vosk + Semantic for commands? | **Yes** - Recommended for command mode |
| Phoneme for dictation? | **No** - Phoneme for fixed commands only |
| Which embedding model? | **FastText (words) + MiniLM (sentences)** |
| Mobile overhead? | **~112MB models, <50ms matching** |

---

## Next Steps

1. **Implement Phase 2A** - Add embedding layer to CommandMatchingService
2. **Download and prune FastText** - Create 50MB vocabulary-limited model
3. **Integrate MiniLM ONNX** - For dynamic UI element matching
4. **Test Vosk** - A/B test against Google ASR for command accuracy
5. **Continue Phase 5** - Phoneme matching for ultra-fast fixed commands

---

*Plan created: 2026-01-18 | Author: Claude*
