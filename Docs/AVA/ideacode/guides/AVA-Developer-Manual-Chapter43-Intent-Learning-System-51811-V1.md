# Chapter 43: Intent Learning System

## Overview

The Intent Learning System enables AVA to automatically learn new intents from successful LLM responses, progressively improving NLU coverage while reducing battery usage and response latency over time.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   User Query    │────▶│  NLU Classifier  │────▶│  Confidence     │
│  "hello ava"    │     │  IntentClassifier│     │  Check          │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                          │
                        ┌─────────────────────────────────┼─────────────────────────────────┐
                        │                                 │                                 │
                        ▼                                 ▼                                 ▼
              ┌─────────────────┐             ┌─────────────────┐             ┌─────────────────┐
              │  HIGH (≥0.6)    │             │  LOW (<0.6)     │             │  VERY LOW (<0.3)│
              │  Use Templates  │             │  Use LLM        │             │  Teach Mode     │
              │  (Fast/Battery) │             │  (Flexible)     │             │                 │
              └─────────────────┘             └────────┬────────┘             └─────────────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │  LLM Response   │
                                              │  with markers   │
                                              │  [INTENT:greet] │
                                              │  [CONFIDENCE:95]│
                                              └────────┬────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │  IntentLearning │
                                              │  Manager        │
                                              │  - Extract hint │
                                              │  - Validate     │
                                              │  - Store in DB  │
                                              └────────┬────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │  Database       │
                                              │  intent_examples│
                                              │  source=        │
                                              │  "LLM_LEARNED"  │
                                              └────────┬────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │  Next Init      │
                                              │  Classifier     │
                                              │  loads ALL      │
                                              │  examples       │
                                              └─────────────────┘
```

## Key Components

### 1. IntentLearningManager

**Location:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/learning/IntentLearningManager.kt`

**Purpose:** Captures and stores learned examples from LLM responses.

```kotlin
class IntentLearningManager(private val context: Context) {

    companion object {
        // Minimum confidence from LLM to learn intent
        private const val LEARNING_CONFIDENCE_THRESHOLD = 70

        // Regex patterns to extract intent hints
        private val INTENT_PATTERN = """\[INTENT:\s*(\w+)\]""".toRegex()
        private val CONFIDENCE_PATTERN = """\[CONFIDENCE:\s*(\d+)\]""".toRegex()
    }

    // Learn from LLM response
    suspend fun learnFromResponse(
        userMessage: String,
        llmResponse: String
    ): Boolean

    // Extract intent hint from LLM response
    fun extractIntentHint(llmResponse: String): IntentHint?

    // Clean response by removing intent markers
    fun cleanResponse(llmResponse: String): String
}
```

### 2. SystemPromptManager

**Location:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/SystemPromptManager.kt`

**Purpose:** Instructs LLM to output intent markers in responses.

```kotlin
// From getBehavioralGuidelines()
Intent Learning System:
• When you understand what the user is asking, include an intent hint in your response
• Format: [INTENT: intent_name] [CONFIDENCE: 0-100]
• Only include hints when confidence >= 70
• Intent names: greeting, wifi_on, wifi_off, bluetooth_on, bluetooth_off,
  volume_up, volume_down, play_music, pause_music, navigate, search,
  open_app, close_app, battery_status, device_info, etc.
• Example: "Hello! I'm AVA, your AI assistant. How can I help you today?
  [INTENT: greeting] [CONFIDENCE: 95]"
```

### 3. HybridResponseGenerator

**Location:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/response/HybridResponseGenerator.kt`

**Purpose:** Decides when to use templates vs LLM based on confidence.

```kotlin
companion object {
    // Confidence threshold for template vs LLM decision
    // Above: Templates (fast, battery-efficient)
    // Below: LLM (flexible, handles unknown)
    private const val TEMPLATE_CONFIDENCE_THRESHOLD = 0.6f
}

private fun shouldUseLLM(classification: IntentClassification): Boolean {
    if (!llmGenerator.isReady()) return false

    // HIGH confidence → Templates, LOW confidence → LLM
    if (classification.confidence >= TEMPLATE_CONFIDENCE_THRESHOLD) {
        return false  // Use template
    }

    return true  // Use LLM
}
```

### 4. ChatViewModel Integration

**Location:** `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/ChatViewModel.kt:1124-1138`

```kotlin
// Phase 2: Learn from LLM response if low confidence
val responseContent = if (confidenceScore != null && confidenceScore < 0.7f) {
    Log.d(TAG, "Low confidence ($confidenceScore), attempting to learn from LLM response")
    val learned = learningManager.learnFromResponse(
        userMessage = text.trim(),
        llmResponse = rawResponseContent
    )
    if (learned) {
        Log.i(TAG, "Successfully learned intent from LLM response")
    }
    // Clean response by removing intent markers before showing to user
    learningManager.cleanResponse(rawResponseContent)
} else {
    rawResponseContent
}
```

### 5. IntentClassifier Database Loading

**Location:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt:523`

```kotlin
private suspend fun precomputeIntentEmbeddings() = withContext(Dispatchers.IO) {
    // Load examples from database (includes LLM_LEARNED)
    val database = DatabaseProvider.getDatabase(context)
    val dao = database.intentExampleDao()

    // Get ALL examples from database
    val allExamples = dao.getAllExamplesOnce()

    // Group examples by intent and compute embeddings
    val examplesByIntent = allExamples.groupBy { it.intentId }
    // ... compute embeddings for each intent
}
```

## Database Schema

### IntentExampleEntity

**Location:** `Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/entity/IntentExampleEntity.kt`

```kotlin
@Entity(tableName = "intent_examples")
data class IntentExampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val exampleHash: String,      // MD5(intent_id + example_text) - dedup
    val intentId: String,          // e.g., "greeting"
    val exampleText: String,       // e.g., "hello ava"
    val isPrimary: Boolean = false,
    val source: String = "STATIC_JSON",  // STATIC_JSON, USER_TAUGHT, LLM_LEARNED
    val locale: String = "en-US",
    val createdAt: Long,
    val usageCount: Int = 0,
    val lastUsed: Long? = null
)
```

## Learning Flow Example

### Step 1: User Query with Unknown Intent

```
User: "hello ava"
NLU: unknown (confidence 0.3)
```

### Step 2: LLM Generates Response with Markers

```
LLM: "Hello! I'm AVA, your AI assistant. How can I help you today?
      [INTENT: greeting] [CONFIDENCE: 95]"
```

### Step 3: IntentLearningManager Captures

```kotlin
// Extract intent hint
val hint = extractIntentHint(llmResponse)
// → IntentHint(intentName = "greeting", confidence = 95)

// Validate confidence (95 >= 70)
// Store in database
val newExample = IntentExampleEntity(
    exampleHash = generateHash("greeting", "hello ava"),
    intentId = "greeting",
    exampleText = "hello ava",
    source = "LLM_LEARNED",
    createdAt = System.currentTimeMillis()
)
dao.insertIntentExample(newExample)
```

### Step 4: User Sees Clean Response

```
AVA: "Hello! I'm AVA, your AI assistant. How can I help you today?"
```

### Step 5: Future Query Uses NLU

```
User: "hello ava"
NLU: greeting (confidence 0.95) ← Now recognized!
Response: Template (fast, no LLM needed)
```

## Configuration

### Confidence Thresholds

| Threshold | Value | Purpose |
|-----------|-------|---------|
| `TEMPLATE_CONFIDENCE_THRESHOLD` | 0.6 | Above: templates, Below: LLM |
| `LEARNING_CONFIDENCE_THRESHOLD` | 70 | Minimum LLM confidence to learn |
| Teach Mode Threshold | 0.3 | Show teach button to user |

### Intent Marker Format

```
[INTENT: intent_name] [CONFIDENCE: 0-100]
```

- Intent names should match existing intents or create new ones
- Confidence should be 0-100 (not 0.0-1.0)
- Markers are stripped from user-visible response

## Benefits

### 1. Progressive Optimization
- First query: LLM (slow, ~500ms)
- Second query: NLU (fast, ~50ms)
- Battery savings increase over time

### 2. Automatic Coverage Expansion
- No manual training data creation
- LLM acts as teacher for NLU
- Domain-specific vocabulary learned naturally

### 3. User Feedback Loop
- Teach mode for corrections
- Positive reinforcement through usage
- Analytics on learned patterns

## Metrics & Monitoring

### Learning Statistics

```kotlin
val stats = learningManager.getStats()
// Returns:
// - total_examples: All intent examples
// - learned_examples: LLM_LEARNED source count
// - learned_intents: Distinct learned intent count
// - learned_intent_list: List of learned intent IDs
```

### HybridResponseGenerator Metrics

```kotlin
val metrics = hybridGenerator.getMetrics()
// Returns:
// - llmSuccessCount
// - llmFailureCount
// - templateFallbackCount
// - llmSuccessRate
// - llmUsageRate
```

## LLM Model Support

### AVA-GEM-2B-Q4 (Current)

**Specifications:**
- Parameters: 2B
- Quantized Size: ~1.2GB
- Training Tokens: 2T
- Multilingual: Limited (primarily English)

**Configuration:**
```kotlin
val llmConfig = LLMConfig(
    modelPath = "models/AVA-GEM-2B-Q4",
    modelLib = "gemma_q4f16_1_devc.o",
    device = "cpu",
    maxMemoryMB = 1536
)
```

### Gemma Model Comparison

| Model | Parameters | Size (Q4) | Training | Multilingual |
|-------|------------|-----------|----------|--------------|
| AVA-GEM-2B-Q4 | 2B | ~1.2GB | 2T tokens | Limited |
| Gemma 2 9B | 9B | ~5GB | 8T tokens | Good |
| Gemma 2 27B | 27B | ~15GB | 13T tokens | Excellent |
| Gemma 3 4B | 4B | ~2.5GB | - | Wide coverage |

### Multilingual Alternatives

For non-English support, AVA includes:
- **Qwen 2.5 1.5B** - Better for Chinese/Asian languages
- See `ModelSelector.kt` and `LanguageDetector.kt` for language routing

## Testing

### Unit Tests

```kotlin
// Test intent extraction
@Test
fun `test extractIntentHint`() {
    val manager = IntentLearningManager(context)
    val hint = manager.extractIntentHint(
        "Hello! [INTENT: greeting] [CONFIDENCE: 95]"
    )
    assertEquals("greeting", hint?.intentName)
    assertEquals(95, hint?.confidence)
}

// Test response cleaning
@Test
fun `test cleanResponse`() {
    val cleaned = manager.cleanResponse(
        "Hello! [INTENT: greeting] [CONFIDENCE: 95]"
    )
    assertEquals("Hello!", cleaned)
}
```

### Integration Test

1. Send unknown query to trigger LLM
2. Verify LLM response contains markers
3. Verify example stored in database
4. Restart classifier
5. Verify same query now classified by NLU

## Troubleshooting

### Learning Not Working

1. **Check LLM is generating markers**
   - View raw LLM output in logs
   - Verify SystemPromptManager instructions included

2. **Check confidence threshold**
   - LLM confidence must be ≥70
   - NLU confidence must be <0.7 to trigger learning

3. **Check database storage**
   - Query `intent_examples` for `source = 'LLM_LEARNED'`
   - Verify no hash collisions

### Re-initialization Required

After learning new examples, the classifier must be re-initialized to include them in embeddings. This happens automatically in `IntentLearningManager.learnIntent()`:

```kotlin
// Re-initialize classifier to recompute embeddings
IntentClassifier.getInstance(context).initialize(modelPath)
```

## Future Enhancements

1. **Incremental Embedding Update** - Add new embeddings without full re-init
2. **Confidence Decay** - Reduce confidence for unused learned examples
3. **User Validation** - Ask user to confirm learned intents
4. **Export/Import** - Share learned examples across devices
5. **A/B Testing** - Compare learned vs static intent accuracy

## Related Chapters

- Chapter 38: LLM Model Management
- Chapter 39: Intent Routing Architecture
- Chapter 40: NLU Initialization Fix
- Chapter 42: LLM Model Setup

---

**Author:** AVA AI Team
**Created:** 2025-11-18
**Version:** 1.0
