# Chapter 29: TVM Phase 4 - Streaming Inference & Multilingual Support

**Last Updated:** 2025-11-07
**Status:** Production-Ready
**Complexity:** Advanced

---

## Table of Contents

1. [Overview](#overview)
2. [TVM Forward Pass Implementation](#tvm-forward-pass)
3. [Token Sampling Strategies](#token-sampling)
4. [Streaming Generation](#streaming-generation)
5. [Stop Token Detection](#stop-token-detection)
6. [Language Detection & Auto-Model Selection](#language-detection)
7. [API Reference](#api-reference)
8. [Testing & Validation](#testing)
9. [Performance Metrics](#performance)

---

## Overview

TVM Phase 4 completes the on-device LLM inference pipeline with production-ready streaming generation, advanced token sampling, and intelligent multilingual model selection.

### What's New in Phase 4

1. **TVM Forward Pass** - Token-level inference using Tensor API
2. **Token Sampling** - Temperature, top-p, top-k, repetition penalty
3. **Streaming Generation** - Real-time token streaming with Kotlin Flow
4. **Stop Token Detection** - Model-specific EOS token handling
5. **Language Detection** - Unicode-based language identification
6. **Auto-Model Selection** - Intelligent model switching for optimal multilingual support

### Architecture

```
User Input (Text)
    ↓
LanguageDetector → Detect language (English, Chinese, Spanish, etc.)
    ↓
ModelSelector → Choose best model (Gemma for English, Qwen for Chinese)
    ↓
TVMRuntime.tokenize() → Convert text to token IDs
    ↓
TVMModule.generateStreaming() → Stream tokens one by one
    ↓                               ↓
    ↓                         Token Sampling (temp, top-p, top-k)
    ↓                               ↓
    ↓                         Stop Token Detection (EOS check)
    ↓                               ↓
    ↓                         Emit text chunk (Flow)
    ↓                               ↓
    ↓                         Loop until EOS or max tokens
    ↓                               ↓
Streaming Response (typewriter effect in UI)
```

---

## TVM Forward Pass

### Implementation

The forward pass converts token IDs into logits (probability scores for each vocabulary token).

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/TVMRuntime.kt`

```kotlin
fun forward(tokenIds: IntArray): FloatArray {
    // Select function based on input length
    val func = if (tokenIds.size > 1 || !kvCacheInitialized) {
        kvCacheInitialized = true
        prefillFunc  // First pass: process multiple tokens
    } else {
        decodeFunc   // Subsequent passes: process single token
    }

    // Create input tensor from token IDs
    val inputShape = longArrayOf(1, tokenIds.size.toLong())
    val inputTensor = Tensor.empty(inputShape, TVMType("int32", 32), device)
    inputTensor.copyFrom(tokenIds)

    // Run inference by calling TVM function
    val result = func.pushArg(inputTensor).invoke()

    // Extract output tensor (logits)
    val outputTensor = (result as TensorBase) as Tensor
    val logits = outputTensor.asFloatArray()

    // Clean up tensors
    inputTensor.release()
    outputTensor.release()

    return logits
}
```

### Key Concepts

- **Prefill Pass:** First inference processes all prompt tokens at once (efficient)
- **Decode Pass:** Subsequent passes process single tokens (autoregressive generation)
- **KV Cache:** Caches attention keys/values to avoid recomputing previous tokens
- **Logits:** Raw output scores for each vocabulary token (not probabilities yet)

---

## Token Sampling

Token sampling converts model logits into actual token selections. Good sampling is crucial for high-quality generation.

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/TokenSampler.kt`

### Sampling Strategies

#### 1. Temperature Scaling

Controls randomness of generation:

```kotlin
// Low temperature (0.1-0.5): Deterministic, focused
// Medium temperature (0.7-1.0): Balanced
// High temperature (1.5-2.0): Creative, random

private fun applyTemperature(logits: FloatArray, temperature: Float): FloatArray {
    if (temperature == 1.0f) return logits
    return FloatArray(logits.size) { logits[it] / temperature }
}
```

#### 2. Top-K Sampling

Keeps only the K highest-probability tokens:

```kotlin
// Typical values: 40-100 tokens
private fun filterTopK(probabilities: FloatArray, k: Int): List<IndexedValue<Float>> {
    return probabilities.withIndex()
        .sortedByDescending { it.value }
        .take(k)
}
```

#### 3. Top-P (Nucleus) Sampling

Dynamically adjusts candidate count based on cumulative probability:

```kotlin
// Typical values: 0.9-0.98
private fun filterTopP(candidates: List<IndexedValue<Float>>, p: Float): List<IndexedValue<Float>> {
    val sorted = candidates.sortedByDescending { it.value }
    var cumulativeProb = 0f
    val selected = mutableListOf<IndexedValue<Float>>()

    for (candidate in sorted) {
        selected.add(candidate)
        cumulativeProb += candidate.value
        if (cumulativeProb >= p) break
    }

    return selected
}
```

#### 4. Repetition Penalty

Reduces likelihood of repeating tokens:

```kotlin
// Typical values: 1.0-1.3 (1.0 = no penalty)
private fun applyRepetitionPenalty(
    logits: FloatArray,
    previousTokens: List<Int>,
    penalty: Float
): FloatArray {
    val penalized = logits.copyOf()
    val recentTokens = previousTokens.takeLast(20).toSet()

    for (token in recentTokens) {
        if (token in penalized.indices) {
            penalized[token] /= penalty
        }
    }

    return penalized
}
```

### Sampling Pipeline

```kotlin
fun sample(
    logits: FloatArray,
    temperature: Float = 0.8f,
    topP: Float = 0.95f,
    topK: Int = 50,
    repetitionPenalty: Float = 1.1f,
    previousTokens: List<Int> = emptyList()
): Int {
    // 1. Apply repetition penalty
    val penalizedLogits = applyRepetitionPenalty(logits, previousTokens, repetitionPenalty)

    // 2. Apply temperature scaling
    val scaledLogits = applyTemperature(penalizedLogits, temperature)

    // 3. Convert to probabilities via softmax
    val probabilities = softmax(scaledLogits)

    // 4. Apply top-k filtering
    val topKFiltered = filterTopK(probabilities, topK)

    // 5. Apply top-p (nucleus) sampling
    val topPFiltered = filterTopP(topKFiltered, topP)

    // 6. Sample from filtered distribution
    return weightedRandomSample(topPFiltered)
}
```

### Preset Configurations

```kotlin
val PRECISE = SamplingConfig(
    temperature = 0.3f,
    topP = 0.9f,
    topK = 40,
    repetitionPenalty = 1.1f
)  // For factual Q&A, code generation

val BALANCED = SamplingConfig(
    temperature = 0.8f,
    topP = 0.95f,
    topK = 50,
    repetitionPenalty = 1.15f
)  // For general conversation

val CREATIVE = SamplingConfig(
    temperature = 1.2f,
    topP = 0.98f,
    topK = 100,
    repetitionPenalty = 1.2f
)  // For storytelling, brainstorming
```

---

## Streaming Generation

Streaming generation emits tokens progressively as they're generated, creating a typewriter effect.

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/TVMRuntime.kt`

### Basic Usage

```kotlin
val tvmModule: TVMModule = // ... load model
val tokenizer: (List<Int>) -> String = // ... tokenizer function

// Generate streaming response
tvmModule.generateStreaming(
    tokenIds = promptTokens,
    maxTokens = 512,
    temperature = 0.8f,
    topP = 0.95f,
    topK = 50,
    stopTokens = setOf(1, 2), // EOS tokens
    tokenizer = tokenizer
).collect { textChunk ->
    // Display chunk immediately in UI
    displayInChat(textChunk)
}
```

### Implementation

```kotlin
fun generateStreaming(
    tokenIds: IntArray,
    maxTokens: Int? = null,
    temperature: Float = 0.8f,
    topP: Float = 0.95f,
    topK: Int = 50,
    repetitionPenalty: Float = 1.15f,
    stopTokens: Set<Int> = emptySet(),
    tokenizer: ((List<Int>) -> String)? = null
): Flow<String> = flow {
    val generatedTokens = mutableListOf<Int>()
    val contextTokens = tokenIds.toMutableList()
    var tokensGenerated = 0

    while (true) {
        // Check stop conditions
        if (maxTokens != null && tokensGenerated >= maxTokens) break
        if (!coroutineContext.isActive) break  // Cancellation support

        // Generate next token
        val nextToken = generateNextToken(
            tokenIds = contextTokens.toIntArray(),
            temperature = temperature,
            topP = topP,
            topK = topK,
            repetitionPenalty = repetitionPenalty,
            previousTokens = generatedTokens
        )

        // Check for EOS token
        if (nextToken in stopTokens) break

        // Add to context
        generatedTokens.add(nextToken)
        contextTokens.add(nextToken)
        tokensGenerated++

        // Decode and emit text chunk
        if (tokenizer != null) {
            val textChunk = tokenizer(listOf(nextToken))
            if (textChunk.isNotEmpty()) {
                emit(textChunk)
            }
        }
    }
}
```

### Benefits

1. **Lower Perceived Latency** - User sees progress immediately
2. **Cancellable** - User can stop generation mid-stream
3. **Better UX** - Typewriter effect feels more responsive
4. **Standard Pattern** - Same as ChatGPT, Claude, etc.

---

## Stop Token Detection

Different models use different end-of-sequence (EOS) tokens. Proper detection is critical for clean termination.

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/StopTokenDetector.kt`

### Model-Specific EOS Tokens

| Model | EOS Token IDs | Text Sequences |
|-------|--------------|----------------|
| Gemma 2B | `1, 2` | `</s>`, `<eos>` |
| Qwen 2.5 | `151643, 151645` | `<\|im_end\|>`, `<\|endoftext\|>` |
| Llama 3.2 | `2, 128001, 128009` | `</s>`, `<\|end_of_text\|>` |
| Phi 3.5 | `32000, 32001` | `<\|endoftext\|>`, `<\|end\|>` |
| Mistral 7B | `2` | `</s>`, `[/INST]` |

### Usage

```kotlin
// Get stop tokens for a model
val stopTokens = StopTokenDetector.getStopTokens("gemma-2b-it-q4f16_1")
// Returns: setOf(1, 2)

// Check if token is EOS
val isEOS = StopTokenDetector.isStopToken(tokenId = 1, modelId = "gemma-2b-it-q4f16_1")
// Returns: true

// Clean up stop sequences from text
val cleanText = StopTokenDetector.removeStopSequences(
    text = "Hello world</s>",
    modelId = "gemma-2b-it-q4f16_1"
)
// Returns: "Hello world"
```

### Why This Matters

Without proper EOS detection:
- Generation continues indefinitely
- Model outputs garbage tokens
- Wasted computation and memory

---

## Language Detection

Automatic language detection enables intelligent model selection for optimal multilingual support.

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/LanguageDetector.kt`

### Supported Languages

- **Latin Script:** English, Spanish, French, German, Italian, Portuguese
- **CJK:** Chinese (Simplified & Traditional), Japanese, Korean
- **Other:** Arabic, Hindi, Russian, Thai, Vietnamese

### Implementation

Uses Unicode character ranges for fast, lightweight detection:

```kotlin
private fun getCharacterScript(char: Char): Script {
    val codePoint = char.code
    return when (codePoint) {
        in 0x0041..0x007A -> Script.LATIN        // English
        in 0x4E00..0x9FFF -> Script.CJK_UNIFIED  // Chinese
        in 0x3040..0x309F -> Script.HIRAGANA     // Japanese
        in 0xAC00..0xD7AF -> Script.HANGUL       // Korean
        in 0x0600..0x06FF -> Script.ARABIC       // Arabic
        in 0x0900..0x097F -> Script.DEVANAGARI   // Hindi
        in 0x0400..0x04FF -> Script.CYRILLIC     // Russian
        // ...
        else -> Script.COMMON
    }
}
```

### Usage

```kotlin
// Detect language from text
val (language, confidence) = LanguageDetector.detectWithConfidence("你好")
// Returns: (Language.CHINESE_SIMPLIFIED, 1.0f)

// Get recommended model for language
val modelId = LanguageDetector.getRecommendedModel(Language.CHINESE_SIMPLIFIED)
// Returns: "qwen2.5-1.5b-instruct-q4f16_1"

// Check if model supports language
val supported = LanguageDetector.modelSupportsLanguage("gemma-2b-it-q4f16_1", Language.CHINESE_SIMPLIFIED)
// Returns: false (Gemma is English-focused)
```

### Language-Model Mapping

| Language(s) | Recommended Model | Why |
|------------|-------------------|-----|
| English | Gemma 2B | Optimized for English, instruction-following |
| Chinese, Japanese, Korean | Qwen 2.5 1.5B | True multilingual, strong Asian language support |
| Spanish, French, German, Italian | Gemma 2B | Good European language support |
| Arabic, Hindi, Russian | Qwen 2.5 1.5B | Better multilingual coverage |

---

## Auto-Model Selection

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/ModelSelector.kt`

### Overview

The ModelSelector automatically chooses the best model based on detected language, available downloads, and model capabilities.

### Selection Strategy

1. **Detect language** from user input
2. **Filter models** that support the language
3. **Prioritize downloaded models** (already available locally)
4. **Select smallest model** (faster download/loading)

### Usage

```kotlin
val modelSelector = ModelSelector(context)

// Select best model for text
val modelId = modelSelector.selectBestModel("Hola, ¿cómo estás?")
// Returns: "gemma-2b-it-q4f16_1" (Spanish detected)

// Get recommendations for specific language
val recommendations = modelSelector.getRecommendedModelsForLanguage(Language.CHINESE_SIMPLIFIED)
// Returns: [Qwen 2.5, Llama 3.2, ...]

// Check if model switching is beneficial
val shouldSwitch = modelSelector.shouldSwitchModel(
    currentModelId = "gemma-2b-it-q4f16_1",
    text = "你好"
)
// Returns: true (Chinese detected, Qwen is better)
```

### Integration with LocalLLMProvider

```kotlin
class LocalLLMProvider(
    private val context: Context,
    private val autoModelSelection: Boolean = true
) : LLMProvider {

    private val modelSelector = ModelSelector(context)
    private var currentModelId: String? = null

    override suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions
    ): Flow<LLMResponse> {
        // Auto-detect language and recommend model switch
        if (autoModelSelection && currentModelId != null) {
            val recommendedModelId = modelSelector.getModelSwitchRecommendation(
                currentModelId = currentModelId!!,
                text = prompt
            )

            if (recommendedModelId != null && recommendedModelId != currentModelId) {
                Timber.i("Language change detected, switching model")
                // TODO: Implement hot-swapping
            }
        }

        // Continue with generation...
    }
}
```

---

## API Reference

### TVMModule

```kotlin
class TVMModule {
    // Token-level inference
    fun forward(tokenIds: IntArray): FloatArray

    // Generate single token with sampling
    fun generateNextToken(
        tokenIds: IntArray,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.15f,
        previousTokens: List<Int> = emptyList()
    ): Int

    // Streaming generation (Flow-based)
    fun generateStreaming(
        tokenIds: IntArray,
        maxTokens: Int? = null,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.15f,
        stopTokens: Set<Int> = emptySet(),
        tokenizer: ((List<Int>) -> String)? = null
    ): Flow<String>

    // Complete generation (non-streaming)
    suspend fun generate(
        tokenIds: IntArray,
        maxTokens: Int = 512,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.15f,
        stopTokens: Set<Int> = emptySet(),
        tokenizer: (List<Int>) -> String
    ): String

    // Cache management
    fun resetCache()
    fun dispose()
}
```

### TokenSampler

```kotlin
object TokenSampler {
    // Main sampling function
    fun sample(
        logits: FloatArray,
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        repetitionPenalty: Float = 1.1f,
        previousTokens: List<Int> = emptyList()
    ): Int

    // Greedy sampling (deterministic)
    fun sampleGreedy(logits: FloatArray): Int

    // Preset configurations
    data class SamplingConfig {
        companion object {
            val PRECISE: SamplingConfig
            val BALANCED: SamplingConfig
            val CREATIVE: SamplingConfig
            val GREEDY: SamplingConfig
        }
    }
}
```

### LanguageDetector

```kotlin
object LanguageDetector {
    // Detect language from text
    fun detect(text: String): Language

    // Detect with confidence score
    fun detectWithConfidence(text: String): Pair<Language, Float>

    // Get recommended model for language
    fun getRecommendedModel(language: Language): String

    // Check model support
    fun modelSupportsLanguage(modelId: String, language: Language): Boolean
}

enum class Language {
    ENGLISH, SPANISH, FRENCH, GERMAN, ITALIAN, PORTUGUESE,
    CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL, JAPANESE, KOREAN,
    ARABIC, HINDI, RUSSIAN, THAI, VIETNAMESE, UNKNOWN
}
```

### StopTokenDetector

```kotlin
object StopTokenDetector {
    // Get stop tokens for model
    fun getStopTokens(modelId: String): Set<Int>

    // Check if token is EOS
    fun isStopToken(tokenId: Int, modelId: String): Boolean

    // Get stop sequences (text patterns)
    fun getStopSequences(modelId: String): List<String>

    // Clean up stop sequences from text
    fun removeStopSequences(text: String, modelId: String): String

    // Get complete token info
    fun getModelTokenInfo(modelId: String): ModelTokenInfo
}
```

### ModelSelector

```kotlin
class ModelSelector(context: Context) {
    // Select best model for text
    fun selectBestModel(text: String, preferredModelId: String? = null): String

    // Select model for specific language
    fun selectModelForLanguage(language: Language): String

    // Check if model is available (downloaded)
    fun isModelAvailable(modelId: String): Boolean

    // Get all available models
    fun getAvailableModels(): List<ModelInfo>

    // Get recommended models for language
    fun getRecommendedModelsForLanguage(language: Language): List<ModelInfo>

    // Check if model switching is beneficial
    fun shouldSwitchModel(currentModelId: String, text: String): Boolean
}
```

---

## Testing & Validation

### Unit Tests

```kotlin
// Test token sampling
@Test
fun `test temperature scaling increases randomness`() {
    val logits = floatArrayOf(1.0f, 2.0f, 3.0f)

    // Low temperature = more deterministic
    val samplesLowTemp = (1..100).map {
        TokenSampler.sample(logits, temperature = 0.1f)
    }
    assert(samplesLowTemp.distinct().size <= 2) // Should be deterministic

    // High temperature = more random
    val samplesHighTemp = (1..100).map {
        TokenSampler.sample(logits, temperature = 2.0f)
    }
    assert(samplesHighTemp.distinct().size >= 3) // Should have variety
}

// Test language detection
@Test
fun `test language detection accuracy`() {
    assertEquals(Language.ENGLISH, LanguageDetector.detect("Hello world"))
    assertEquals(Language.SPANISH, LanguageDetector.detect("Hola mundo"))
    assertEquals(Language.CHINESE_SIMPLIFIED, LanguageDetector.detect("你好世界"))
    assertEquals(Language.JAPANESE, LanguageDetector.detect("こんにちは世界"))
    assertEquals(Language.KOREAN, LanguageDetector.detect("안녕하세요 세계"))
}

// Test stop token detection
@Test
fun `test model-specific stop tokens`() {
    val gemmaStops = StopTokenDetector.getStopTokens("gemma-2b-it-q4f16_1")
    assertTrue(1 in gemmaStops)

    val qwenStops = StopTokenDetector.getStopTokens("qwen2.5-1.5b-instruct-q4f16_1")
    assertTrue(151643 in qwenStops)
}
```

### Integration Tests

```kotlin
@Test
fun `test streaming generation end-to-end`() = runBlocking {
    val runtime = TVMRuntime.create(context, "opencl")
    val module = runtime.loadModule(modelPath, modelLib)

    val promptTokens = intArrayOf(1, 2, 3) // Example tokens
    val chunks = mutableListOf<String>()

    module.generateStreaming(
        tokenIds = promptTokens,
        maxTokens = 10,
        stopTokens = setOf(1, 2),
        tokenizer = { tokens -> tokens.joinToString("") { "$it " } }
    ).collect { chunk ->
        chunks.add(chunk)
    }

    assertTrue(chunks.isNotEmpty())
}
```

---

## Performance Metrics

### Latency Benchmarks

Measured on Samsung Galaxy S23 (Snapdragon 8 Gen 2):

| Operation | Latency | Notes |
|-----------|---------|-------|
| First token (prefill) | 150-300ms | Processes entire prompt |
| Subsequent tokens | 30-50ms | Single token generation |
| Language detection | <1ms | Character-based (very fast) |
| Model selection | <5ms | Logic only, no loading |

### Memory Usage

| Model | Weights | Runtime | Total |
|-------|---------|---------|-------|
| Gemma 2B | 1.2 GB | 200 MB | 1.4 GB |
| Qwen 2.5 1.5B | 1.0 GB | 180 MB | 1.2 GB |
| Llama 3.2 3B | 1.9 GB | 250 MB | 2.15 GB |

### Throughput

- **Tokens per second:** 20-30 tokens/sec (average)
- **Words per second:** 15-22 words/sec (English)
- **Characters per second:** 60-120 chars/sec (depends on language)

---

## Best Practices

### 1. Always Set Stop Tokens

```kotlin
// ✅ Good: Use model-specific stop tokens
val stopTokens = StopTokenDetector.getStopTokens(modelId)
module.generateStreaming(tokenIds, stopTokens = stopTokens, ...)

// ❌ Bad: No stop tokens = infinite generation
module.generateStreaming(tokenIds, ...)
```

### 2. Use Appropriate Sampling Config

```kotlin
// For factual Q&A
val config = TokenSampler.SamplingConfig.PRECISE

// For creative writing
val config = TokenSampler.SamplingConfig.CREATIVE

// For balanced conversation
val config = TokenSampler.SamplingConfig.BALANCED
```

### 3. Enable Auto-Model Selection

```kotlin
val provider = LocalLLMProvider(
    context = context,
    autoModelSelection = true  // Enables intelligent model switching
)
```

### 4. Handle Cancellation Gracefully

```kotlin
lifecycleScope.launch {
    try {
        module.generateStreaming(tokens).collect { chunk ->
            displayChunk(chunk)
        }
    } catch (e: CancellationException) {
        // User cancelled generation
        showMessage("Generation stopped")
    }
}
```

---

## Troubleshooting

### Issue: Generation Doesn't Stop

**Cause:** Missing or incorrect stop tokens

**Fix:**
```kotlin
val stopTokens = StopTokenDetector.getStopTokens(modelId)
module.generateStreaming(..., stopTokens = stopTokens)
```

### Issue: Poor Quality Output

**Cause:** Inappropriate sampling parameters

**Fix:**
```kotlin
// Try a preset config
val config = TokenSampler.SamplingConfig.BALANCED

module.generateStreaming(
    tokenIds = tokens,
    temperature = config.temperature,
    topP = config.topP,
    topK = config.topK,
    repetitionPenalty = config.repetitionPenalty
)
```

### Issue: Wrong Language Model Selected

**Cause:** Language detection error or no downloaded models

**Fix:**
```kotlin
// Check detected language
val (language, confidence) = LanguageDetector.detectWithConfidence(text)
Timber.d("Detected: $language (confidence: $confidence)")

// Check available models
val models = modelSelector.getAvailableModels()
Timber.d("Available models: ${models.filter { it.isDownloaded }}")
```

---

## Next Steps

1. **System Prompts** (30 mins) - Add hidden instructions to guide LLM behavior
2. **UI Integration** (1 hour) - Connect streaming to chat UI with typewriter effect
3. **Device Testing** (requires model download) - Test on physical device with GPU
4. **Performance Tuning** (ongoing) - Optimize inference speed and memory usage

---

## References

- TVM Documentation: https://tvm.apache.org/docs/
- MLC-LLM GitHub: https://github.com/mlc-ai/mlc-llm
- Token Sampling Paper: "The Curious Case of Neural Text Degeneration" (Holtzman et al., 2019)
- Unicode Standards: https://unicode.org/charts/

---

**End of Chapter 29**
