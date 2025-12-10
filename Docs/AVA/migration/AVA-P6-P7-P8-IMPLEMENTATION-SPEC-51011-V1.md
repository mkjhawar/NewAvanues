# P6-P7-P8 Implementation Specification

**Date**: 2025-11-10
**Author**: Claude Code (Analysis & Specification)
**Status**: Ready for Implementation
**Total Estimated Time**: 46 hours

---

## Overview

This document provides comprehensive specifications for completing the three remaining high-priority production blockers:
- **P6**: LocalLLMProvider stub completion (2hrs)
- **P7**: TVMTokenizer real implementation (4hrs)
- **P8**: Test coverage 23% → 90%+ (40hrs)

---

## P6: LocalLLMProvider Stub Completion

**File**: `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/provider/LocalLLMProvider.kt`
**Lines**: 58-76 (stub at 62-64)
**Estimated Time**: 2 hours
**Priority**: 🔴 High (blocks LLM functionality)

### Current State

```kotlin
override suspend fun initialize(config: LLMConfig): Result<Unit> {
    return try {
        Timber.i("Initializing LocalLLMProvider with model: ${config.modelPath}")

        // TODO: Implement full component initialization when TVM integration is complete
        // For now, return success to allow compilation
        Timber.w("LocalLLMProvider initialization stub - TVM integration pending")

        currentConfig = config
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to initialize LocalLLMProvider")
        Result.Error(exception = e, message = "Initialization failed: ${e.message}")
    }
}
```

### Required Dependencies

Based on ALCEngine.kt analysis, the following components are needed:

1. **LanguagePackManager** - Manages multilingual language packs
2. **IInferenceStrategy** → MLCInferenceStrategy
3. **IStreamingManager** → BackpressureStreamingManager
4. **IMemoryManager** → KVCacheMemoryManager
5. **ISamplerStrategy** → TopPSampler
6. **LatencyMetrics** - For performance tracking

### Implementation Specification

```kotlin
override suspend fun initialize(config: LLMConfig): Result<Unit> {
    return try {
        Timber.i("Initializing LocalLLMProvider with model: ${config.modelPath}")
        val startTime = System.currentTimeMillis()

        // 1. Validate model path
        val modelFile = File(config.modelPath)
        if (!modelFile.exists()) {
            return Result.Error(
                exception = FileNotFoundException("Model not found: ${config.modelPath}"),
                message = "Model file not found"
            )
        }

        // 2. Create dependencies
        val languagePackManager = LanguagePackManager(context)
        val memoryManager = KVCacheMemoryManager(
            maxCacheSize = config.contextLength ?: 2048
        )
        val samplerStrategy = TopPSampler(
            temperature = config.temperature ?: 0.7f,
            topP = config.topP ?: 0.9f
        )
        val streamingManager = BackpressureStreamingManager(
            bufferSize = 256 // tokens
        )
        val inferenceStrategy = MLCInferenceStrategy(
            device = config.device ?: "opencl"
        )

        // 3. Initialize ALCEngine
        val engine = ALCEngine(
            context = context,
            languagePackManager = languagePackManager,
            inferenceStrategy = inferenceStrategy,
            streamingManager = streamingManager,
            memoryManager = memoryManager,
            samplerStrategy = samplerStrategy
        )

        // 4. Initialize with default language (English)
        when (val result = engine.initialize()) {
            is Result.Success -> {
                alcEngine = engine
                currentConfig = config

                // 5. Track initialization latency
                val initTime = System.currentTimeMillis() - startTime
                Timber.i("LocalLLMProvider initialized successfully in ${initTime}ms")

                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.e(result.exception, "ALCEngine initialization failed")
                result
            }
        }

    } catch (e: Exception) {
        Timber.e(e, "Failed to initialize LocalLLMProvider")
        Result.Error(exception = e, message = "Initialization failed: ${e.message}")
    }
}
```

### Testing Requirements

1. Unit test: Valid model path → Success
2. Unit test: Invalid model path → Error with clear message
3. Unit test: Missing dependencies → Graceful error
4. Integration test: Full initialization with real model → Success
5. Performance test: Initialization completes in < 10s

### Metrics to Track

```kotlin
// Add private field
private val latencyMetrics = LatencyMetrics()

// Track in initialize()
latencyMetrics.recordInitialization(initTime)

// Update checkHealth()
override suspend fun checkHealth(): Result<ProviderHealth> {
    return try {
        val status = if (alcEngine != null) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY

        Result.Success(
            ProviderHealth(
                status = status,
                averageLatencyMs = latencyMetrics.getAverageLatency(), // NOW TRACKED
                errorRate = latencyMetrics.getErrorRate(), // NOW TRACKED
                lastError = if (status == HealthStatus.UNHEALTHY) "Engine not initialized" else null,
                lastChecked = System.currentTimeMillis()
            )
        )
    } catch (e: Exception) {
        Result.Error(exception = e, message = "Health check failed: ${e.message}")
    }
}
```

### Hot-Swapping Enhancement

The `switchModel()` function (lines 286-316) already implements basic hot-swapping. Enhance with:

```kotlin
suspend fun switchModel(modelId: String): Result<Unit> {
    return try {
        Timber.i("Switching model to: $modelId")
        val startTime = System.currentTimeMillis()

        // 1. Save current engine state (for rollback)
        val previousModelId = currentModelId
        val previousEngine = alcEngine

        // 2. Load new model in background
        val modelInfo = modelSelector.getModelInfo(modelId)
            ?: return Result.Error(
                exception = IllegalArgumentException("Model not found: $modelId"),
                message = "Model not found: $modelId"
            )

        val config = LLMConfig(
            modelPath = modelInfo.huggingFaceRepo,
            modelLib = modelId,
            device = currentConfig?.device ?: "opencl"
        )

        // 3. Initialize new engine
        val initResult = initialize(config)

        if (initResult is Result.Success) {
            // 4. Clean up old engine AFTER new one is ready
            previousEngine?.cleanup()

            currentModelId = modelId

            val switchTime = System.currentTimeMillis() - startTime
            Timber.i("Model switched successfully in ${switchTime}ms")
            latencyMetrics.recordModelSwitch(switchTime)

            Result.Success(Unit)
        } else {
            // 5. Rollback on failure
            Timber.w("Model switch failed, rolling back to $previousModelId")
            alcEngine = previousEngine
            currentModelId = previousModelId

            initResult
        }

    } catch (e: Exception) {
        Timber.e(e, "Failed to switch model")
        Result.Error(exception = e, message = "Model switch failed: ${e.message}")
    }
}
```

---

## P7: TVMTokenizer Real Implementation

**File**: `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/loader/TVMTokenizer.kt`
**Lines**: 164-215 (placeholders at 164, 186)
**Estimated Time**: 4 hours
**Priority**: 🔴 Critical (blocks tokenization)

### Current State

```kotlin
// Line 164: TODO: Use SentencePiece JNI
// PLACEHOLDER: Simple whitespace tokenization with mock IDs
Timber.w("Using placeholder SentencePiece tokenization")

// Line 186: TODO: Use SentencePiece JNI
// PLACEHOLDER: Return mock decoded text
Timber.w("Using placeholder SentencePiece detokenization")
```

### Architecture Decision

**Option 1: Native SentencePiece JNI** (Recommended)
- Pros: Production-ready, official implementation, fast
- Cons: Requires JNI setup, platform-specific builds
- Time: 4 hours

**Option 2: Pure Kotlin Port**
- Pros: No JNI, easier debugging
- Cons: Slower, more maintenance
- Time: 8+ hours

**Decision**: Use Option 1 (Native SentencePiece JNI)

### Implementation Specification

#### Step 1: Add SentencePiece Dependency

```kotlin
// build.gradle.kts
dependencies {
    // SentencePiece for tokenization
    implementation("com.google.protobuf:protobuf-java:3.21.12")

    // Native library (will need to bundle .so files)
    implementation(files("libs/sentencepiece-jni.jar"))
}
```

#### Step 2: Bundle Native Libraries

```
Universal/AVA/Features/LLM/src/main/jniLibs/
├── arm64-v8a/
│   └── libsentencepiece_jni.so
├── armeabi-v7a/
│   └── libsentencepiece_jni.so
└── x86_64/
    └── libsentencepiece_jni.so
```

#### Step 3: Implement Real Tokenization

```kotlin
class TVMTokenizer(private val vocabPath: String) : ITokenizer {

    private var processor: SentencePieceProcessor? = null
    private val vocab = mutableMapOf<String, Long>()
    private val reverseVocab = mutableMapOf<Long, String>()

    // Special tokens
    private val BOS_ID = 1L
    private val EOS_ID = 2L
    private val UNK_ID = 0L

    init {
        try {
            // Load SentencePiece model
            processor = SentencePieceProcessor(vocabPath)

            // Build vocabulary maps
            val vocabSize = processor.getPieceSize()
            for (i in 0 until vocabSize) {
                val piece = processor.idToPiece(i)
                vocab[piece] = i.toLong()
                reverseVocab[i.toLong()] = piece
            }

            Timber.i("TVMTokenizer initialized: vocab_size=$vocabSize, path=$vocabPath")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load SentencePiece model")
            throw IllegalStateException("Tokenizer initialization failed", e)
        }
    }

    override fun encode(text: String, addBos: Boolean, addEos: Boolean): LongArray {
        val processor = this.processor
            ?: throw IllegalStateException("Tokenizer not initialized")

        try {
            // Encode using SentencePiece
            val pieces = processor.encode(text)
            val tokens = mutableListOf<Long>()

            // Add BOS token if requested
            if (addBos) {
                tokens.add(BOS_ID)
            }

            // Add encoded pieces
            tokens.addAll(pieces.map { it.toLong() })

            // Add EOS token if requested
            if (addEos) {
                tokens.add(EOS_ID)
            }

            Timber.d("Encoded \"$text\" -> ${tokens.size} tokens")
            return tokens.toLongArray()

        } catch (e: Exception) {
            Timber.e(e, "Encoding failed for text: $text")
            // Fallback: Return UNK token
            return longArrayOf(UNK_ID)
        }
    }

    override fun decode(tokenIds: LongArray): String {
        val processor = this.processor
            ?: throw IllegalStateException("Tokenizer not initialized")

        try {
            // Filter out special tokens
            val filteredIds = tokenIds.filter { id ->
                id != BOS_ID && id != EOS_ID && id != UNK_ID
            }.map { it.toInt() }

            // Decode using SentencePiece
            val text = processor.decode(filteredIds)

            Timber.d("Decoded ${tokenIds.size} tokens -> \"$text\"")
            return text

        } catch (e: Exception) {
            Timber.e(e, "Decoding failed for token IDs: ${tokenIds.joinToString()}")
            return "[DECODING_ERROR]"
        }
    }

    override fun getVocabSize(): Int {
        return vocab.size
    }

    override fun tokenToId(token: String): Long? {
        return vocab[token]
    }

    override fun idToToken(id: Long): String? {
        return reverseVocab[id]
    }

    fun cleanup() {
        processor?.close()
        processor = null
        vocab.clear()
        reverseVocab.clear()
    }
}
```

#### Step 4: SentencePiece JNI Wrapper

```kotlin
// Create: SentencePieceProcessor.kt
class SentencePieceProcessor(modelPath: String) : AutoCloseable {

    private var nativeHandle: Long = 0L

    init {
        System.loadLibrary("sentencepiece_jni")
        nativeHandle = nativeLoad(modelPath)
        if (nativeHandle == 0L) {
            throw IllegalArgumentException("Failed to load model: $modelPath")
        }
    }

    fun encode(text: String): IntArray {
        return nativeEncode(nativeHandle, text)
    }

    fun decode(ids: List<Int>): String {
        return nativeDecode(nativeHandle, ids.toIntArray())
    }

    fun getPieceSize(): Int {
        return nativeGetPieceSize(nativeHandle)
    }

    fun idToPiece(id: Int): String {
        return nativeIdToPiece(nativeHandle, id)
    }

    override fun close() {
        if (nativeHandle != 0L) {
            nativeUnload(nativeHandle)
            nativeHandle = 0L
        }
    }

    // Native methods
    private external fun nativeLoad(modelPath: String): Long
    private external fun nativeEncode(handle: Long, text: String): IntArray
    private external fun nativeDecode(handle: Long, ids: IntArray): String
    private external fun nativeGetPieceSize(handle: Long): Int
    private external fun nativeIdToPiece(handle: Long, id: Int): String
    private external fun nativeUnload(handle: Long)
}
```

### Testing Requirements

1. Unit test: Encode simple English text → correct token IDs
2. Unit test: Decode token IDs → correct text
3. Unit test: Handle unknown tokens → UNK_ID
4. Unit test: BOS/EOS token addition
5. Integration test: Round-trip (encode → decode) preserves meaning
6. Performance test: Tokenize 1000 words in < 100ms

### Edge Cases to Handle

1. Empty string → Return empty array or [BOS, EOS]
2. Very long text (>8192 chars) → Truncate or chunk
3. Invalid UTF-8 → Replace with replacement character
4. Unknown tokens → Use UNK_ID
5. Model file not found → Clear error message

---

## P8: Test Coverage 23% → 90%+

**Current Coverage**: 23% overall
**Target Coverage**: 90%+ critical paths, 100% IPC/API/Intent
**Estimated Time**: 40 hours
**Priority**: 🟡 High (quality gate)

### Current Coverage Breakdown

| Module | Current | Target | Gap | Files | Est. Time |
|--------|---------|--------|-----|-------|-----------|
| NLU | 0% | 90% | 90% | 15 | 8hrs |
| RAG | 7% | 90% | 83% | 20 | 10hrs |
| LLM | 10% | 90% | 80% | 25 | 12hrs |
| Chat | 10% | 90% | 80% | 10 | 4hrs |
| Overlay | 0% | 90% | 90% | 5 | 3hrs |
| Teach | 0% | 90% | 90% | 5 | 3hrs |

### Test Compilation Errors (NLU Module)

From background process output, there are 100+ compilation errors in NLU tests:

**Common Issues**:
1. Missing test dependencies (JUnit, Mockito, Coroutines Test)
2. Unresolved references (runTest, Room, test framework)
3. Type mismatches (Long vs Int)
4. Missing context parameter

**Fix Strategy**:

```kotlin
// Universal/AVA/Features/NLU/build.gradle.kts
dependencies {
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("org.robolectric:robolectric:4.11")

    // Android instrumented test dependencies
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.room:room-testing:2.6.0")
    androidTestImplementation(project(":Universal:AVA:Core:Data"))
}
```

### TDD Workflow (MANDATORY)

Per IDEACODE v7.2 protocols, all new code MUST follow TDD:

1. **Write test first** (Red)
2. **Implement minimum code** (Green)
3. **Refactor** (Refactor)
4. **Commit**

**Example for NLU**:

```kotlin
// 1. Write test (Red)
@Test
fun `classify intent with high confidence returns correct intent`() = runTest {
    // Arrange
    val classifier = IntentClassifier.getInstance(context)
    val modelManager = ModelManager(context)
    classifier.initialize(modelManager.getModelPath())

    val utterance = "Turn on the lights"
    val candidates = listOf("lights_on", "lights_off", "unknown")

    // Act
    val result = classifier.classifyIntent(utterance, candidates)

    // Assert
    assertTrue(result is Result.Success)
    val classification = (result as Result.Success).data
    assertEquals("lights_on", classification.intent)
    assertTrue(classification.confidence > 0.7f)
}

// 2. Implement (Green)
// ... implementation ...

// 3. Verify test passes
// ./gradlew :Universal:AVA:Features:NLU:test

// 4. Commit
git commit -m "feat(nlu): add intent classification with confidence threshold"
```

### Test Suite Structure

```
Universal/AVA/Features/
├── NLU/
│   ├── src/test/ (Unit tests - JVM)
│   │   ├── IntentClassifierTest.kt
│   │   ├── BertTokenizerTest.kt
│   │   ├── ModelManagerTest.kt
│   │   └── EmbeddingProviderTest.kt
│   └── src/androidTest/ (Instrumented - Device/Emulator)
│       ├── IntentClassifierIntegrationTest.kt
│       └── ClassifyIntentUseCaseIntegrationTest.kt
├── RAG/
│   ├── src/test/
│   │   ├── DocumentParserTest.kt
│   │   ├── EmbeddingGeneratorTest.kt
│   │   └── VectorStoreTest.kt
│   └── src/androidTest/
│       └── RAGPipelineIntegrationTest.kt
├── LLM/
│   ├── src/test/
│   │   ├── LocalLLMProviderTest.kt
│   │   ├── ALCEngineTest.kt
│   │   └── TVMTokenizerTest.kt
│   └── src/androidTest/
│       └── LLMInferenceIntegrationTest.kt
└── ... (similar structure for Chat, Overlay, Teach)
```

### Coverage Measurement

```bash
# Run tests with coverage
./gradlew :Universal:AVA:Features:NLU:testDebugUnitTestCoverage
./gradlew :Universal:AVA:Features:RAG:testDebugUnitTestCoverage
./gradlew :Universal:AVA:Features:LLM:testDebugUnitTestCoverage

# Generate combined report
./gradlew jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html
```

### Prioritized Test Creation Order

**Week 1 (16hrs)**: Fix compilation errors + NLU coverage
1. Fix test dependencies (2hrs)
2. Fix NLU test compilation errors (2hrs)
3. NLU unit tests (8hrs) → Target 90%
4. NLU integration tests (4hrs)

**Week 2 (12hrs)**: RAG coverage
1. RAG unit tests (8hrs) → Target 90%
2. RAG integration tests (4hrs)

**Week 3 (12hrs)**: LLM + remaining modules
1. LLM unit tests (6hrs) → Target 90%
2. Chat tests (3hrs) → Target 90%
3. Overlay tests (2hrs) → Target 90%
4. Teach tests (1hr) → Target 90%

---

## Implementation Checklist

### P6: LocalLLMProvider (2hrs)
- [ ] Add dependency injection for ALCEngine components
- [ ] Implement full initialize() with error handling
- [ ] Add LatencyMetrics tracking
- [ ] Enhance switchModel() with rollback
- [ ] Write unit tests (5 tests)
- [ ] Write integration test
- [ ] Test on device
- [ ] Commit and push

### P7: TVMTokenizer (4hrs)
- [ ] Research SentencePiece JNI bindings
- [ ] Add SentencePiece dependency to build.gradle
- [ ] Bundle native .so libraries
- [ ] Create SentencePieceProcessor JNI wrapper
- [ ] Implement real encode() method
- [ ] Implement real decode() method
- [ ] Handle edge cases (empty, long, invalid)
- [ ] Write unit tests (10 tests)
- [ ] Write performance tests
- [ ] Test on device
- [ ] Commit and push

### P8: Test Coverage (40hrs)
- [ ] Fix test dependencies in all modules
- [ ] Fix NLU test compilation errors
- [ ] Create test suite structure
- [ ] Implement NLU tests (90% coverage)
- [ ] Implement RAG tests (90% coverage)
- [ ] Implement LLM tests (90% coverage)
- [ ] Implement Chat tests (90% coverage)
- [ ] Implement Overlay tests (90% coverage)
- [ ] Implement Teach tests (90% coverage)
- [ ] Generate coverage report
- [ ] Verify 90%+ overall coverage
- [ ] Commit and push

---

## Success Criteria

### P6 Complete When:
- ✅ LocalLLMProvider.initialize() creates fully functional ALCEngine
- ✅ All dependencies properly injected
- ✅ Latency metrics tracked
- ✅ Hot-swapping works with rollback
- ✅ All tests pass
- ✅ Device testing successful

### P7 Complete When:
- ✅ TVMTokenizer uses real SentencePiece JNI
- ✅ Encoding produces correct token IDs
- ✅ Decoding produces correct text
- ✅ Round-trip preserves meaning
- ✅ Edge cases handled gracefully
- ✅ Performance meets targets (<100ms for 1000 words)
- ✅ All tests pass

### P8 Complete When:
- ✅ All test compilation errors fixed
- ✅ NLU module: 90%+ coverage
- ✅ RAG module: 90%+ coverage
- ✅ LLM module: 90%+ coverage
- ✅ Chat module: 90%+ coverage
- ✅ Overlay module: 90%+ coverage
- ✅ Teach module: 90%+ coverage
- ✅ Overall coverage: 90%+
- ✅ Jacoco report generated

---

## Estimated Timeline

| Phase | Tasks | Duration | Dependencies |
|-------|-------|----------|--------------|
| **Phase 1** | P6 Implementation | 2 hrs | None |
| **Phase 2** | P7 Implementation | 4 hrs | None (parallel with P6) |
| **Phase 3** | P8 Week 1 (NLU) | 16 hrs | None |
| **Phase 4** | P8 Week 2 (RAG) | 12 hrs | Week 1 complete |
| **Phase 5** | P8 Week 3 (LLM+) | 12 hrs | Week 2 complete |

**Total**: 46 hours over 2-3 weeks

---

## Next Steps

1. **Immediate**: Fix IDEACODE config (already done)
2. **Phase 1**: Implement P6 (2hrs) - Can start now
3. **Phase 2**: Implement P7 (4hrs) - Can parallel with P6
4. **Phase 3-5**: Implement P8 (40hrs) - Follow TDD workflow

**Recommendation**: Start with P6 today, parallel P7 tomorrow, then systematic P8 over 2-3 weeks with daily commits.
