# TVM Runtime Integration Plan

**Created:** 2025-11-07
**Status:** In Progress
**Effort:** 2-3 days
**Priority:** High (Backlog #2)

---

## Overview

Complete TVM (Apache TVM + MLC-LLM) runtime integration for on-device LLM inference in AVA AI.

**Current State:**
- ✅ TVM runtime libraries (JAR + .so) already built and integrated
- ✅ Stub implementations exist for all components
- ⚠️ No actual model loading, tokenization, or inference implemented

**Goal State:**
- ✅ Load MLC-LLM quantized models (Gemma-2b-it-q4f16_1)
- ✅ Tokenize/detokenize text using model tokenizer
- ✅ Execute forward passes with KV cache
- ✅ Stream generated tokens back to application
- ✅ Proper error handling and resource cleanup

---

## Components to Implement

### 1. TVMRuntime.kt (Core Runtime)

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/TVMRuntime.kt`

**TODOs:**
- [ ] `loadModule()` - Load .so model library via TVM
- [ ] `tokenize()` - Convert text to token IDs
- [ ] `detokenize()` - Convert token IDs to text
- [ ] `dispose()` - Clean up TVM resources

**Implementation Details:**

```kotlin
fun loadModule(
    modelPath: String,
    modelLib: String,
    device: String = "opencl"
): TVMModule {
    // 1. Load compiled model library (.so file)
    val module = org.apache.tvm.Module.load(modelPath)

    // 2. Get model functions
    val prefillFunc = module.getFunction("prefill")
    val decodeFunc = module.getFunction("decode")
    val resetCacheFunc = module.getFunction("reset_kv_cache")

    // 3. Create TVMModule wrapper
    return TVMModule(
        module = module,
        prefillFunc = prefillFunc,
        decodeFunc = decodeFunc,
        resetCacheFunc = resetCacheFunc,
        device = this.device
    )
}
```

**Dependencies:**
- org.apache.tvm.Module (TVM Java API)
- org.apache.tvm.Function
- Model files in correct format (.so or .tar)

---

### 2. TVMModule.kt (Model Wrapper)

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/TVMRuntime.kt` (class TVMModule)

**TODOs:**
- [ ] `forward()` - Execute single token prediction
- [ ] `resetCache()` - Clear KV cache for new conversation
- [ ] `dispose()` - Release model resources

**Implementation Details:**

```kotlin
class TVMModule(
    private val module: org.apache.tvm.Module,
    private val prefillFunc: Function?,
    private val decodeFunc: Function?,
    private val resetCacheFunc: Function?,
    private val device: Device
) {
    fun forward(tokenIds: IntArray): FloatArray {
        // 1. Convert to TVM NDArray
        val inputTensor = NDArray.empty(
            longArrayOf(1, tokenIds.size.toLong()),
            DataType.Int(32),
            device
        )
        inputTensor.copyFrom(tokenIds)

        // 2. Execute inference (prefill or decode)
        val outputTensor = if (tokenIds.size > 1) {
            // Prefill (first tokens)
            prefillFunc?.invoke(inputTensor) as NDArray
        } else {
            // Decode (single token)
            decodeFunc?.invoke(inputTensor) as NDArray
        }

        // 3. Convert output to FloatArray
        val logits = FloatArray(outputTensor.shape()[1].toInt())
        outputTensor.copyTo(logits)

        return logits
    }

    fun resetCache() {
        resetCacheFunc?.invoke()
    }

    fun dispose() {
        module.release()
    }
}
```

---

### 3. Tokenizer Integration

**Option A: Use MLC-LLM's Tokenizer**
```kotlin
// Create TVMTokenizer class
class TVMTokenizer(
    private val vocabPath: String,
    private val tokenizerPath: String
) {
    private val tokenizer: SentencePieceProcessor // or HuggingFace tokenizer

    fun encode(text: String): List<Int> {
        return tokenizer.encode(text)
    }

    fun decode(tokenIds: List<Int>): String {
        return tokenizer.decode(tokenIds)
    }
}
```

**Option B: Use Existing Tokenizer Library**
- SentencePiece (for Gemma models)
- HuggingFace Tokenizers library
- Tiktoken (if needed)

**Recommendation:** Option B with SentencePiece for Gemma-2b-it

---

### 4. LocalLLMProvider.kt (Full Initialization)

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/provider/LocalLLMProvider.kt`

**TODOs:**
- [ ] Implement full component initialization (line 47)
- [ ] Integrate tokenizer
- [ ] Wire up ALC Engine with TVM components

**Implementation Details:**

```kotlin
override suspend fun initialize(config: LLMConfig): Result<Unit> {
    return try {
        Timber.i("Initializing LocalLLMProvider with model: ${config.modelPath}")

        // 1. Create TVM runtime
        val runtime = TVMRuntime.create(context, config.device)

        // 2. Load model
        val tvmModule = runtime.loadModule(
            modelPath = config.modelPath,
            modelLib = config.modelLib ?: "gemma_2b_it_q4f16_1",
            device = config.device
        )

        // 3. Create tokenizer
        val tokenizer = TVMTokenizer(
            vocabPath = "${context.filesDir}/models/tokenizer.model",
            tokenizerPath = "${context.filesDir}/models/tokenizer.json"
        )

        // 4. Create inference strategy
        val inferenceStrategy = MLCInferenceStrategy(tvmModule)

        // 5. Create ALC Engine
        alcEngine = ALCEngine(
            modelLoader = TVMModelLoader(context),
            inferenceStrategy = inferenceStrategy,
            tokenizer = tokenizer,
            samplerFactory = { TopPSampler() },
            streamingManager = BackpressureStreamingManager(),
            memoryManager = KVCacheMemoryManager(config.maxMemoryMB)
        )

        currentConfig = config
        Result.Success(Unit)

    } catch (e: Exception) {
        Timber.e(e, "Failed to initialize LocalLLMProvider")
        Result.Error(
            exception = e,
            message = "Initialization failed: ${e.message}"
        )
    }
}
```

---

### 5. TVMModelLoader.kt (Metadata Reading)

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/loader/TVMModelLoader.kt`

**TODO:**
- [ ] Get vocabulary size from model metadata (line 57)

**Implementation:**

```kotlin
// Read model config JSON
val configPath = "${modelPath}/mlc-chat-config.json"
val configJson = File(configPath).readText()
val config = Json.decodeFromString<MLCModelConfig>(configJson)

val model = LoadedModel(
    config = config,
    handle = tvmModule,
    vocabSize = config.vocab_size, // From model metadata
    metadata = mapOf(
        "device" to config.deviceType,
        "context_length" to config.contextLength,
        "quantization" to config.quantization
    )
)
```

---

### 6. MLCInferenceStrategy.kt (Availability Check)

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/inference/MLCInferenceStrategy.kt`

**TODO:**
- [ ] Check if MLC runtime is actually available (line 55)

**Implementation:**

```kotlin
override fun isAvailable(): Boolean {
    return try {
        // Check if TVM libraries are loaded
        Class.forName("org.apache.tvm.Module")

        // Check if model is loaded
        model != null
    } catch (e: ClassNotFoundException) {
        Timber.w("TVM runtime not available: ${e.message}")
        false
    } catch (e: Exception) {
        Timber.e(e, "Error checking MLC availability")
        false
    }
}
```

---

## Dependencies

### Required Libraries
- ✅ Apache TVM Runtime (JAR + .so)
- ⚠️ SentencePiece tokenizer (for Gemma models)
- ⚠️ Model files (Gemma-2b-it-q4f16_1)

### Model Files Required
```
models/
├── gemma-2b-it-q4f16_1/
│   ├── params_shard_0.bin          # Model weights
│   ├── gemma_2b_it_q4f16_1.so      # Compiled TVM library
│   ├── mlc-chat-config.json        # Model metadata
│   ├── tokenizer.model             # SentencePiece model
│   └── tokenizer.json              # Tokenizer config
```

---

## Implementation Phases

### Phase 1: Foundation (4-6 hours)
- [ ] Add SentencePiece dependency to build.gradle
- [ ] Create TVMTokenizer class
- [ ] Implement TVMModule.forward() with TVM API
- [ ] Test tokenization with sample text

### Phase 2: Model Loading (4-6 hours)
- [ ] Implement TVMRuntime.loadModule() with real TVM calls
- [ ] Read model metadata from mlc-chat-config.json
- [ ] Implement model availability checks
- [ ] Test model loading with actual .so file

### Phase 3: Integration (4-6 hours)
- [ ] Wire up LocalLLMProvider initialization
- [ ] Connect ALC Engine components
- [ ] Implement full inference pipeline
- [ ] Test end-to-end generation

### Phase 4: Testing & Optimization (2-4 hours)
- [ ] Unit tests for tokenizer
- [ ] Integration tests for inference
- [ ] Memory profiling
- [ ] Performance benchmarks

**Total:** 14-22 hours (2-3 days)

---

## Risks & Mitigations

### Risk 1: TVM API Complexity
**Impact:** High
**Likelihood:** Medium
**Mitigation:**
- Study TVM Java API documentation
- Reference MLC-LLM Android examples
- Start with minimal working example

### Risk 2: Model Format Incompatibility
**Impact:** High
**Likelihood:** Low
**Mitigation:**
- Verify model format matches MLC-LLM expectations
- Test with known-good model files
- Have fallback to simpler inference method

### Risk 3: Tokenizer Integration Issues
**Impact:** Medium
**Likelihood:** Medium
**Mitigation:**
- Use well-tested SentencePiece library
- Validate token IDs match expected vocabulary
- Compare output with reference implementation

### Risk 4: Performance Issues
**Impact:** Medium
**Likelihood:** Low
**Mitigation:**
- Profile memory usage early
- Use KV cache correctly
- Optimize data transfers to/from TVM

---

## Success Criteria

### Minimum Viable Implementation
- ✅ Load Gemma-2b-it model successfully
- ✅ Tokenize "Hello, how are you?" correctly
- ✅ Generate at least one token
- ✅ Detokenize output back to text
- ✅ No crashes or memory leaks

### Full Implementation
- ✅ All above + streaming generation
- ✅ KV cache management working
- ✅ Multi-turn conversations supported
- ✅ Performance: <100ms first token, >10 tokens/sec
- ✅ Memory usage: <2GB for 2B model

---

## Testing Plan

### Unit Tests
```kotlin
@Test
fun testTokenization() {
    val tokenizer = TVMTokenizer(vocabPath, tokenizerPath)
    val tokens = tokenizer.encode("Hello world")
    assertEquals(listOf(23674, 2032), tokens) // Expected token IDs
}

@Test
fun testModelForward() {
    val module = loadTestModel()
    val input = intArrayOf(1, 23674, 2032) // BOS + "Hello world"
    val logits = module.forward(input)
    assertTrue(logits.size > 0)
}
```

### Integration Tests
```kotlin
@Test
suspend fun testEndToEndGeneration() {
    val provider = LocalLLMProvider(context)
    provider.initialize(testConfig)

    val response = provider.generateResponse("Hello")
        .first { it is LLMResponse.Complete }

    assertTrue((response as LLMResponse.Complete).fullText.isNotEmpty())
}
```

---

## Next Steps

1. **Immediate:** Add SentencePiece dependency
2. **Short-term:** Implement TVMTokenizer
3. **Medium-term:** Implement TVMModule.forward()
4. **Long-term:** Full end-to-end testing

---

**Author:** AVA AI Team
**Last Updated:** 2025-11-07
**Status:** Planning Complete, Implementation Pending
