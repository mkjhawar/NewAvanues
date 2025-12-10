# NLU Unification Proposal: VoiceOS + AVA

**Date:** 2025-12-07
**Status:** Proposal
**Branch:** feature/unified-nlu
**Format:** AVU (Avanues Universal Format)

---

## Executive Summary

Analysis of VoiceOS and AVA NLU systems reveals significant opportunity for unification. Currently:

| System | Approach | Strengths | Weaknesses |
|--------|----------|-----------|------------|
| **VoiceOS** | Pattern matching + fuzzy | Fast, deterministic, rich metadata | No semantic understanding |
| **AVA** | ONNX/BERT embeddings | Semantic similarity, learns | Slower, requires model |

**Proposed Solution:** Use AVU (Avanues Universal Format) as the single file format for all intents/commands, with a hybrid classifier that combines pattern matching and semantic understanding.

**Key Insight:** AVU already exists as a cross-project format (spec: `AVU-UNIVERSAL-FORMAT-SPEC.md`). We don't need separate `.vos` and `.ava` formats - use `.aai` (AVA AI Intent) for unified NLU.

---

## Current Architecture

### VoiceOS CommandManager

```
User Speech → CommandResolver → Pattern Match → CommandManager → Action
                    ↓
            Levenshtein Fuzzy → ConfidenceScorer
```

**Key Files:**
- `CommandManager.kt` - Main execution with confidence filtering
- `CommandResolver.kt` - Pattern/fuzzy matching with locale fallback
- `ActionFactory.kt` - Dynamic action creation from category

**Features:**
- Exact + fuzzy matching (Levenshtein distance ≤ 3)
- Locale fallback chain (user locale → en-US)
- Confidence levels: HIGH (>0.80), MEDIUM (0.60-0.80), LOW (<0.60), REJECT (<0.40)
- Usage tracking for analytics

### AVA IntentClassifier

```
User Speech → BertTokenizer → ONNX Model → Embedding → Cosine Similarity → Intent
                                                ↓
                                    Pre-computed Intent Embeddings
```

**Key Files:**
- `IntentClassifier.kt` - ONNX-based semantic classification
- `ClassifyIntentUseCase.kt` - Orchestration with training
- `VoiceOSToAvaConverter.kt` - Converts VoiceOS commands to AVA format

**Features:**
- ONNX Runtime with QNN/NNAPI acceleration
- Mean-pooled BERT embeddings (384-dim MobileBERT)
- L2-normalized cosine similarity
- Self-learning with LLM-as-Teacher (ADR-013)
- Multi-locale support with fallback chain

---

## Overlap Analysis

### Already Connected

AVA already has VoiceOS integration:
```
Modules/AVA/NLU/src/androidMain/kotlin/
├── voiceos/
│   ├── converter/VoiceOSToAvaConverter.kt  # VOS → AVA format
│   ├── parser/VoiceOSParser.kt              # Parse .vos files
│   └── model/VoiceOSCommand.kt              # Data classes
```

The `IntentSourceCoordinator` loads from:
1. Core .ava files (bundled intents)
2. VoiceOS .vos files (converted to .ava format)
3. User-taught intents

---

## Unification Options

### Option 1: AVU-Based Unified NLU (Recommended)

Use AVU format with new NLU-specific IPC codes for unified intent management:

```
Modules/Shared/
└── NLU/
    ├── build.gradle.kts
    └── src/commonMain/
        ├── model/
        │   ├── UnifiedIntent.kt           # Shared intent model
        │   └── IntentCategory.kt          # Categories (nav, media, etc.)
        ├── parser/
        │   └── AvuIntentParser.kt         # Parse AVU .aai files
        ├── repository/
        │   └── IntentOntologyRepository.kt # Single source of truth
        └── sqldelight/
            └── UnifiedIntent.sq           # Shared schema
```

### AVU NLU IPC Codes (New)

| Code  | Meaning          | Format | Purpose |
|-------|------------------|--------|---------|
| **INT** | Intent Definition | `id:canonical:category:priority:action` | Intent metadata |
| **PAT** | Pattern          | `intent_id:pattern_text` | Pattern for matching |
| **SYN** | Synonym          | `intent_id:synonym_text` | Alternative phrases |
| **EMB** | Embedding        | `intent_id:model:dimension:base64_vector` | Pre-computed BERT embedding |
| **ACT** | Action           | `intent_id:action_type:params` | Execution action |

### AVU NLU File Example (`.aai`)

```
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .aai
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: shared
metadata:
  file: navigation.aai
  category: nlu_intents
  count: 12
---
INT:nav_back:go back:navigation:1:goBack()
PAT:nav_back:go back
PAT:nav_back:navigate back
PAT:nav_back:previous screen
SYN:nav_back:back
SYN:nav_back:return
EMB:nav_back:mobilebert-384:384:QWFBYUFhQWF...
INT:vol_up:volume up:media:1:volumeUp()
PAT:vol_up:volume up
PAT:vol_up:turn up volume
PAT:vol_up:louder
SYN:vol_up:increase volume
SYN:vol_up:raise volume
EMB:vol_up:mobilebert-384:384:QmJCYkJiQmI...
---
synonyms:
  back: [return, previous, go back]
  volume: [sound, audio, loudness]
```

**Benefits:**
- Single file format across all Avanues projects
- VoiceOS can use AVA's embeddings for better matching
- AVA can use VoiceOS's rich command metadata
- Shared parser = no conversion needed
- 60-80% smaller than JSON

**Architecture:**

```
┌──────────────────────────────────────────────────────────────────────┐
│                    AVU-Based Intent Ontology                         │
│                    (UnifiedIntentRepository)                         │
├──────────────────────────────────────────────────────────────────────┤
│  .aai files (AVU format) → AvuIntentParser → SQLDelight Database    │
│                                                                      │
│  INT:nav_back:go back:nav:1:goBack()                                │
│  PAT:nav_back:go back | PAT:nav_back:previous                       │
│  EMB:nav_back:mobilebert:384:base64...                              │
└──────────────────────────────────────────────────────────────────────┘
                        ↑                    ↑
            ┌───────────┴───────────┐        │
            │                       │        │
      ┌─────┴─────┐          ┌──────┴────┐   │
      │  VoiceOS  │          │    AVA    │   │
      │ (Pattern) │          │ (Semantic)│   │
      └───────────┘          └───────────┘   │
            │                       │        │
            └───────────┬───────────┘        │
                        │                    │
                  User Speech ───────────────┘
```

### Option 2: AVA as NLU Provider for VoiceOS

VoiceOS queries AVA's IntentClassifier via AIDL:

```kotlin
// VoiceOS CommandResolver.kt
suspend fun resolveCommand(userInput: String): ResolveResult {
    // Try AVA semantic first (if available)
    val avaResult = voiceOSConnection.classifyIntent(userInput)
    if (avaResult.confidence > 0.7f) {
        return ResolveResult.Match(
            command = mapAvaIntentToCommand(avaResult.intent),
            matchType = MatchType.SEMANTIC,
            locale = avaResult.locale
        )
    }

    // Fallback to pattern matching
    return fallbackToPatternMatch(userInput)
}
```

**Benefits:**
- Minimal VoiceOS changes
- AVA does heavy lifting

**Drawbacks:**
- AIDL latency (~5-10ms per call)
- Requires AVA running

### Option 3: Embed ONNX in VoiceOS

Copy AVA's IntentClassifier directly into VoiceOS:

```
Modules/VoiceOS/managers/CommandManager/
└── nlu/
    ├── IntentClassifier.kt    # Copy from AVA
    └── BertTokenizer.kt       # Copy from AVA
```

**Benefits:**
- No IPC latency
- VoiceOS self-contained

**Drawbacks:**
- Code duplication
- Dual model maintenance
- 30MB+ APK size increase

---

## Recommended Approach: AVU-Based Unification

### Phase 1: Shared NLU Module with AVU Parser

Create `Modules/Shared/NLU`:

```kotlin
// UnifiedIntent.kt - Parsed from AVU .aai files
data class UnifiedIntent(
    val id: String,                    // From INT: e.g., "nav_back"
    val canonicalPhrase: String,       // From INT: e.g., "go back"
    val patterns: List<String>,        // From PAT: entries
    val synonyms: List<String>,        // From SYN: entries
    val embedding: FloatArray?,        // From EMB: base64 decoded
    val category: String,              // From INT: nav, media, etc.
    val actionId: String,              // From INT/ACT: action to execute
    val priority: Int,                 // From INT: priority
    val locale: String                 // From schema header
)

// AvuIntentParser.kt - Universal AVU parser for NLU
class AvuIntentParser {
    fun parse(avuContent: String): List<UnifiedIntent> {
        val sections = avuContent.split("---")
        val header = parseHeader(sections[1])
        val data = parseDataSection(sections[2])

        // Group by intent ID
        val intents = mutableMapOf<String, IntentBuilder>()

        data.forEach { line ->
            val parts = line.split(":")
            when (parts[0]) {
                "INT" -> intents[parts[1]] = IntentBuilder(
                    id = parts[1],
                    canonical = parts[2],
                    category = parts[3],
                    priority = parts[4].toInt(),
                    action = parts[5]
                )
                "PAT" -> intents[parts[1]]?.patterns?.add(parts[2])
                "SYN" -> intents[parts[1]]?.synonyms?.add(parts[2])
                "EMB" -> intents[parts[1]]?.embedding = decodeBase64(parts[4])
            }
        }

        return intents.values.map { it.build(header.locale) }
    }
}

// IntentOntologyRepository.kt
interface IntentOntologyRepository {
    suspend fun loadFromAvu(filePath: String)
    suspend fun getAllIntents(locale: String): List<UnifiedIntent>
    suspend fun findByPattern(pattern: String): UnifiedIntent?
    suspend fun findBySemantic(embedding: FloatArray, threshold: Float): List<IntentMatch>
    suspend fun registerIntent(intent: UnifiedIntent)
    suspend fun updateEmbedding(intentId: String, embedding: FloatArray)
    suspend fun exportToAvu(locale: String): String  // Export back to AVU format
}
```

### Phase 2: Hybrid Classifier

Create shared classifier that uses both methods:

```kotlin
// HybridIntentClassifier.kt
class HybridIntentClassifier(
    private val ontologyRepository: IntentOntologyRepository,
    private val embeddingComputer: EmbeddingComputer? = null  // Optional ONNX
) {

    /**
     * Resolution strategy:
     * 1. Exact pattern match → immediate return (HIGH confidence)
     * 2. Fuzzy pattern match → return if HIGH confidence
     * 3. Semantic similarity → return if above threshold
     * 4. Combined ranking → best of fuzzy + semantic
     */
    suspend fun classify(
        utterance: String,
        locale: String
    ): ClassificationResult {
        val intents = ontologyRepository.getAllIntents(locale)

        // 1. Exact match (fastest)
        intents.find { intent ->
            intent.patterns.any { it.equals(utterance, ignoreCase = true) }
        }?.let { return ClassificationResult(it, 1.0f, MatchMethod.EXACT) }

        // 2. Fuzzy match
        val fuzzyMatches = intents.mapNotNull { intent ->
            val score = bestFuzzyScore(utterance, intent.patterns + intent.synonyms)
            if (score > 0.7f) intent to score else null
        }.sortedByDescending { it.second }

        // 3. Semantic match (if embeddings available)
        val semanticMatches = if (embeddingComputer != null) {
            val queryEmbed = embeddingComputer.computeEmbedding(utterance)
            intents.filter { it.embedding != null }.mapNotNull { intent ->
                val score = cosineSimilarity(queryEmbed, intent.embedding!!)
                if (score > 0.6f) intent to score else null
            }.sortedByDescending { it.second }
        } else emptyList()

        // 4. Combined ranking
        val combined = (fuzzyMatches + semanticMatches)
            .groupBy { it.first.id }
            .mapValues { (_, scores) -> scores.maxOf { it.second } }
            .entries
            .sortedByDescending { it.value }

        return combined.firstOrNull()?.let { (intentId, score) ->
            val intent = intents.find { it.id == intentId }!!
            val method = when {
                fuzzyMatches.any { it.first.id == intentId } &&
                semanticMatches.any { it.first.id == intentId } -> MatchMethod.HYBRID
                fuzzyMatches.any { it.first.id == intentId } -> MatchMethod.FUZZY
                else -> MatchMethod.SEMANTIC
            }
            ClassificationResult(intent, score, method)
        } ?: ClassificationResult.NotFound
    }

    enum class MatchMethod { EXACT, FUZZY, SEMANTIC, HYBRID }
}
```

### Phase 3: Integration Points

**VoiceOS:**
```kotlin
// CommandManager.kt - Updated
class CommandManager(
    private val hybridClassifier: HybridIntentClassifier
) {
    suspend fun executeCommand(command: Command): CommandResult {
        val result = hybridClassifier.classify(command.text, getCurrentLocale())

        if (result.confidence >= confidenceThresholds.high) {
            return executeAction(result.intent.actionId, command)
        }
        // ... confidence handling
    }
}
```

**AVA:**
```kotlin
// ClassifyIntentUseCase.kt - Updated
class ClassifyIntentUseCase(
    private val hybridClassifier: HybridIntentClassifier
) {
    suspend operator fun invoke(utterance: String): ClassificationResult {
        return hybridClassifier.classify(utterance, locale)
    }
}
```

---

## Embedding Synchronization

### Compute Once, Share Everywhere

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Embedding Computation Pipeline                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  VoiceOS Commands → VoiceOSToAvaConverter → UnifiedIntent           │
│         ↓                                          ↓                │
│  .vos files              ┌──────────────────────────────────────┐   │
│                          │   Embedding Compute Worker           │   │
│  AVA Intents ───────────→│   (ONNX MobileBERT)                  │   │
│         ↓                │                                      │   │
│  .ava files              │   1. Tokenize canonical phrase       │   │
│                          │   2. Run ONNX inference              │   │
│  User Taught ───────────→│   3. Mean pool + L2 normalize        │   │
│         ↓                │   4. Save to unified_intent.embedding│   │
│  Teach AVA UI            └──────────────────────────────────────┘   │
│                                          ↓                          │
│                          ┌──────────────────────────────────────┐   │
│                          │   Shared Intent Database             │   │
│                          │   (SQLDelight unified_intent table)  │   │
│                          └──────────────────────────────────────┘   │
│                                    ↓           ↓                    │
│                              VoiceOS        AVA                     │
│                              (reads)       (reads)                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Database Schema

```sql
-- unified_intent.sq
CREATE TABLE unified_intent (
    id TEXT PRIMARY KEY NOT NULL,
    canonical_phrase TEXT NOT NULL,
    patterns TEXT NOT NULL,           -- JSON array
    synonyms TEXT NOT NULL,           -- JSON array
    category TEXT NOT NULL,
    action_id TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1,
    locale TEXT NOT NULL DEFAULT 'en-US',
    source TEXT NOT NULL,             -- 'core', 'voiceos', 'user'

    -- Embedding (nullable - computed async)
    embedding_vector BLOB,
    embedding_dimension INTEGER,
    embedding_model_version TEXT,

    -- Timestamps
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    UNIQUE(id, locale)
);

CREATE INDEX idx_unified_intent_locale ON unified_intent(locale);
CREATE INDEX idx_unified_intent_category ON unified_intent(category);
```

---

## Migration Path

### Step 1: Define AVU NLU Codes (0.5 day)

1. Add INT/PAT/SYN/EMB/ACT codes to `AVU-UNIVERSAL-FORMAT-SPEC.md`
2. Document field formats and examples
3. Create validation rules

### Step 2: Create Shared NLU Module (1 day)

1. Create `Modules/Shared/NLU`
2. Implement `AvuIntentParser` for `.aai` files
3. Define `UnifiedIntent` data class
4. Create SQLDelight schema
5. Implement `IntentOntologyRepository`

### Step 3: Convert VoiceOS Commands to AVU (1 day)

1. Create `VosToAvuConverter` - converts `.vos` commands to `.aai` format
2. Generate `.aai` files with patterns from VoiceOS database
3. Update `CommandLoader` to read from shared `.aai` files
4. Keep `CommandResolver` for pattern matching

### Step 4: Convert AVA Intents to AVU (1 day)

1. Create `AvaToAvuConverter` - converts current `.ava` files to `.aai`
2. Include embeddings as base64-encoded EMB entries
3. Update `IntentSourceCoordinator` to load from `.aai`
4. Keep AVA's embedding computation for new intents

### Step 5: Implement Hybrid Classifier (1.5 days)

1. Create `HybridIntentClassifier` in shared module
2. Pattern matching from PAT/SYN entries
3. Semantic matching from EMB entries
4. Combined ranking with configurable weights

### Step 6: Integration Testing (1 day)

1. Test VoiceOS with shared `.aai` files
2. Test AVA with shared `.aai` files
3. Verify cross-project parsing works
4. Benchmark performance (target: <50ms classification)

---

## Benefits Summary

| Benefit | Impact |
|---------|--------|
| **AVU format** | Single parser for all Avanues projects |
| **60-80% smaller files** | Compact IPC codes vs JSON |
| **Shared embeddings** | EMB entries store pre-computed vectors |
| **Cross-project compatible** | VoiceOS/AVA/WebAvanue all read same files |
| **Human-readable** | Easy to inspect and debug |
| **Better VoiceOS accuracy** | Semantic understanding via shared EMB |
| **Faster AVA classification** | Pre-computed embeddings included |
| **Extensible** | Add new IPC codes without breaking parsers |

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Database migration complexity | Use SQLDelight migrations, backup/restore |
| ONNX model version mismatch | Store model version with embeddings, recompute on upgrade |
| Performance regression | Hybrid classifier has fast path (exact match first) |
| Circular dependency | Shared module has no app dependencies |

---

## Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| 1. Define AVU NLU Codes | 0.5 day | None |
| 2. Shared NLU Module | 1 day | Phase 1 |
| 3. VoiceOS → AVU | 1 day | Phase 2 |
| 4. AVA → AVU | 1 day | Phase 2 |
| 5. Hybrid Classifier | 1.5 days | Phases 3, 4 |
| 6. Testing | 1 day | Phase 5 |
| **Total** | **6 days** | |

---

## References

- [AVU Universal Format Spec](../../Docs/VoiceOS/Technical/specifications/AVU-UNIVERSAL-FORMAT-SPEC.md)
- [AVU Learned App Spec](../../Docs/VoiceOS/Technical/specifications/avu-learned-app-format-spec.md)
- [VoiceOS CommandManager](../../Modules/VoiceOS/managers/CommandManager/)
- [AVA IntentClassifier](../../Modules/AVA/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt)
- [APP-CONSOLIDATION-ANALYSIS.md](./APP-CONSOLIDATION-ANALYSIS.md)
- [CONSOLIDATION-IMPLEMENTATION-PLAN.md](./CONSOLIDATION-IMPLEMENTATION-PLAN.md)

---

Updated: 2025-12-07 | IDEACODE v10.3.1
