# AVA LLM System - Living Document
**Created:** 2025-11-13
**Last Updated:** 2025-11-17
**Status:** 🟢 Active Development
**Owner:** AVA AI Team

---

## 📋 Document Purpose

This living document tracks all critical decisions, dependencies, file locations, and configurations for the AVA LLM (Large Language Model) system. It serves as the single source of truth for understanding how the LLM subsystem works and integrates with the rest of AVA.

**Update Frequency:** Update this document whenever:
- New dependencies are added
- File locations change
- Configuration decisions are made
- Architecture changes occur
- Integration points are modified

---

## 🏗️ Architecture Overview

### System Components

```
AVA LLM System Architecture:

┌─────────────────────────────────────────────────────────────┐
│                     ChatViewModel                            │
│  (User-facing conversation management)                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│              HybridResponseGenerator                         │
│  Strategy: LLM First → Template Fallback (2s timeout)       │
└──────┬────────────────────────────────┬─────────────────────┘
       │                                │
       ▼                                ▼
┌──────────────────┐          ┌─────────────────────┐
│ LLMResponseGen   │          │ TemplateResponseGen │
│ (On-device/Cloud)│          │ (Intent→Template)   │
└─────┬────────────┘          └─────────────────────┘
      │
      ▼
┌──────────────────────────────────────────────────────┐
│              LocalLLMProvider                        │
│  Manages on-device LLM inference via TVM/MLC         │
└──────┬───────────────────────────────────────────────┘
       │
       ├─────────────┬──────────────┬──────────────┐
       ▼             ▼              ▼              ▼
┌─────────────┐ ┌────────────┐ ┌──────────┐ ┌─────────┐
│TVMTokenizer │ │ TVMRuntime │ │ ALCEngine│ │StopToken│
│(P7)         │ │            │ │          │ │Detector │
└─────────────┘ └────────────┘ └──────────┘ └─────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│         DJL SentencePiece Library                   │
│  (ai.djl.sentencepiece:sentencepiece:0.33.0)       │
└─────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────┐
│    AVA Models (Proprietary Naming Convention)      │
│  Location: /data/data/{app}/files/models/          │
│  - AVA-GEM-2B-Q4/ (Gemma 2B - Default)             │
│  - AVA-QWN-1B-Q4/ (Qwen 1.5B - Multilingual)       │
│  - AVA-LLM-3B-Q4/ (Llama 3.2 - Balanced)           │
│  - AVA-PHI-3B-Q4/ (Phi 3.5 - English)              │
│  - AVA-MST-7B-Q4/ (Mistral 7B - Premium)           │
└─────────────────────────────────────────────────────┘
```

### Data Flow

**User Message → LLM Response:**

1. **ChatViewModel** receives user message
2. **NLU** classifies intent (IntentClassifier)
3. **HybridResponseGenerator** attempts LLM generation
4. **ModelSelector** detects language and selects best model:
   - English → AVA-GEM-2B-Q4 (Gemma)
   - Chinese/Japanese/Korean → AVA-QWN-1B-Q4 (Qwen)
   - European languages → AVA-GEM-2B-Q4 or AVA-LLM-3B-Q4
5. **HuggingFaceModelDownloader** checks if model is downloaded:
   - If local source configured → Copy from local storage (fast)
   - Else → Download from HuggingFace (first time only)
6. **LLMResponseGenerator**:
   - Encodes prompt text → token IDs (TVMTokenizer)
   - Runs inference → output token IDs (TVMRuntime)
   - Decodes token IDs → response text (TVMTokenizer)
   - Streams chunks back as Flow<ResponseChunk>
7. **ChatViewModel** displays streaming response
8. On error/timeout: Falls back to TemplateResponseGenerator

---

## 📦 Critical Dependencies

### Gradle Dependencies

#### LLM Module (`Universal/AVA/Features/LLM/build.gradle.kts`)

```kotlin
dependencies {
    // Core modules
    implementation(project(":Universal:AVA:Core:Domain"))
    implementation(project(":Universal:AVA:Core:Common"))
    implementation(project(":Universal:AVA:Features:NLU"))

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Kotlin Serialization (for OpenAI protocol)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // ⭐ TVM Runtime (for MLC LLM)
    implementation(files("libs/tvm4j_core.jar"))

    // ⭐ SentencePiece tokenizer (P7 - CRITICAL)
    implementation("ai.djl.sentencepiece:sentencepiece:0.33.0")

    // HTTP client for cloud LLM APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

#### Native Libraries

```
Location: Universal/AVA/Features/LLM/libs/
Required files:
  - tvm4j_core.jar (TVM runtime JAR)
  - arm64-v8a/libtvm4j_runtime_packed.so (TVM native lib)
  - arm64-v8a/libc++_shared.so (C++ standard library)
```

**Build Configuration:**
```kotlin
packaging {
    jniLibs {
        pickFirsts.add("**/libc++_shared.so")
        pickFirsts.add("**/libtvm4j_runtime_packed.so")
    }
}
```

### Module Dependencies

**Dependency Graph:**
```
apps/ava-standalone
  └── Universal/AVA/Features/Chat
       ├── Universal/AVA/Features/LLM ⭐ (This module)
       │    ├── Universal/AVA/Features/NLU
       │    ├── Universal/AVA/Core/Domain
       │    └── Universal/AVA/Core/Common
       ├── Universal/AVA/Features/Actions
       └── Universal/AVA/Core/Data
```

**Critical Decision:** Moved `IntentTemplates` from Chat → LLM module to break circular dependency (Commit: 2f4fa69)

---

## 📁 File Locations & Paths

### Model Files

#### AVA Model Storage (On-Device)

**Location (Android):**
```
/data/data/com.augmentalis.ava/files/models/
```

**Directory Structure:**
```
models/
├── AVA-GEM-2B-Q4/           ⭐ Default model (Gemma 2B)
│   └── AVA-GEM-2B-Q4.tar    1.2GB - Model + tokenizer
├── AVA-QWN-1B-Q4/           Multilingual (Qwen 1.5B)
│   └── AVA-QWN-1B-Q4.tar    1.0GB
├── AVA-LLM-3B-Q4/           Balanced (Llama 3.2 3B)
│   └── AVA-LLM-3B-Q4.tar    1.9GB
├── AVA-PHI-3B-Q4/           English specialist (Phi 3.5)
│   └── AVA-PHI-3B-Q4.tar    2.4GB
└── AVA-MST-7B-Q4/           Premium (Mistral 7B)
    └── AVA-MST-7B-Q4.tar    4.5GB
```

**Access Pattern:**
```kotlin
// Models downloaded to internal storage
val modelsDir = File(context.filesDir, "models")
val modelDir = File(modelsDir, "AVA-GEM-2B-Q4")
val modelTar = File(modelDir, "AVA-GEM-2B-Q4.tar")
```

**Download Behavior:**
- First run: HuggingFaceModelDownloader checks if model exists
- If localSourcePath configured: Copies from local storage (dev/testing)
- Else: Downloads from HuggingFace repository (production)
- Saves with AVA naming convention for security

**CRITICAL:** Models use proprietary AVA naming to obscure origins. See `docs/AVA-MODEL-NAMING-REGISTRY.md` for mappings (INTERNAL ONLY).

#### NLU Models (Intent Classification)

**Location:**
```
apps/ava-standalone/src/main/assets/models/
```

**Files:**
```
models/
├── AVA-ONX-384-BASE-INT8.onnx  22.9 MB - ONNX NLU model (full)
├── mobilebert_int8.onnx        22.9 MB - MobileBERT (lite)
└── vocab.txt                   231 KB  - BERT vocabulary
```

### Source Code Locations

#### LLM Feature Module

```
Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/

├── di/
│   └── LLMModule.kt                    ⭐ Hilt DI configuration
│
├── response/
│   ├── ResponseGenerator.kt            Interface (strategy pattern)
│   ├── HybridResponseGenerator.kt      ⭐ LLM + Template fallback
│   ├── LLMResponseGenerator.kt         On-device/cloud LLM
│   ├── TemplateResponseGenerator.kt    Intent → Template mapping
│   ├── CloudLLMProvider.kt             OpenAI/Anthropic APIs
│   ├── LocalLLMProvider.kt             ⭐ On-device LLM manager
│   ├── ResponseChunk.kt                Streaming response types
│   ├── ResponseContext.kt              Context for generation
│   └── IntentTemplates.kt              ⭐ Template definitions
│
├── alc/                                ALC (Auto-regressive Loop Controller)
│   ├── ALCEngine.kt                    Main inference loop
│   ├── TVMRuntime.kt                   TVM model runtime
│   ├── StopTokenDetector.kt            EOS/stop sequence detection
│   ├── TokenSampler.kt                 Sampling strategies
│   ├── LanguageDetector.kt             Multilingual support
│   │
│   ├── loader/
│   │   └── TVMTokenizer.kt             ⭐ P7 - SentencePiece tokenizer
│   │
│   ├── tokenizer/
│   │   └── TVMTokenizer.kt             Caching wrapper
│   │
│   └── loader/
│       └── HuggingFaceModelDownloader.kt  ⭐ Model download/copy
│
├── ModelSelector.kt                    ⭐ Language-aware model selection
│
└── config/
    └── LLMConfig.kt                    Configuration data classes
```

#### Tests

```
Universal/AVA/Features/LLM/src/test/java/com/augmentalis/ava/features/llm/

├── alc/
│   ├── ALCEngineTest.kt
│   ├── StopTokenDetectorTest.kt
│   ├── TokenSamplerTest.kt
│   └── loader/
│       └── TVMTokenizerTest.kt         ⭐ 30+ tokenizer tests
│
└── LanguageDetectorTest.kt
```

---

## ⚙️ Configuration & Initialization

### Hilt Dependency Injection

**File:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/di/LLMModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LLMModule {

    @Provides
    @Singleton
    fun provideLocalLLMProvider(
        @ApplicationContext context: Context
    ): LocalLLMProvider {
        return LocalLLMProvider(context)
    }

    @Provides
    @Singleton
    fun provideResponseGenerator(
        @ApplicationContext context: Context,
        llmProvider: LocalLLMProvider
    ): ResponseGenerator {
        return HybridResponseGenerator(
            context = context,
            llmProvider = llmProvider
        )
    }
}
```

**Decision:** Use Hilt for DI to enable easy testing and swapping implementations (Commit: 9719325)

### TVMTokenizer Initialization

**Current Implementation:**

```kotlin
// In TVMTokenizer.kt (companion object)
fun create(
    context: Context,
    modelDir: String,
    type: TokenizerType = TokenizerType.SENTENCEPIECE
): TVMTokenizer {
    val tokenizerPath = when (type) {
        TokenizerType.SENTENCEPIECE -> "$modelDir/tokenizer.model"
        TokenizerType.HUGGINGFACE -> "$modelDir/tokenizer.json"
    }

    Timber.d("Creating tokenizer: $tokenizerPath (type: $type)")

    if (!File(tokenizerPath).exists()) {
        Timber.w("Tokenizer file not found: $tokenizerPath")
    }

    return TVMTokenizer(tokenizerPath, type)
}
```

**Expected Usage:**

```kotlin
// In LocalLLMProvider or LLMResponseGenerator
val context = applicationContext
val modelDir = "models/gemma-2b-it"  // Relative to assets/
val tokenizer = TVMTokenizer.create(context, modelDir, TokenizerType.SENTENCEPIECE)
```

**⚠️ ISSUE:** Current code uses `File(tokenizerPath)` which won't work for assets.
**✅ SOLUTION NEEDED:** Must use `context.assets.open()` for asset files.

---

## 🔑 Critical Decisions Log

### Decision 0: AVA Model Naming Convention (2025-11-17)

**What:** Use proprietary AVA naming format: AVA-{TYPE}-{SIZE}-{QUANT}

**Why:**
- Security: Obscures model origins from competitive intelligence
- Branding: Consistent naming across all AVA features
- Privacy: User doesn't know which vendor's model they're using
- Flexibility: Can swap underlying models without user-facing changes

**Mapping Examples:**
- AVA-GEM-2B-Q4 ← gemma-2b-it-q4f16_1 (Google)
- AVA-PHI-3B-Q4 ← phi-2-q4f16_1 (Microsoft)
- AVA-MST-7B-Q4 ← mistral-7b-instruct-v0.2 (Mistral AI)

**Impact:**
- Downloads use original HuggingFace repo names
- Files saved with AVA names on device
- User only sees AVA naming convention
- Internal registry maps AVA → original names

**Implementation:** Commit ae7b738

**Registry:** `docs/AVA-MODEL-NAMING-REGISTRY.md` (INTERNAL USE ONLY)

---

### Decision 1: Hybrid Response Strategy (2025-11-13)

**What:** Use HybridResponseGenerator with automatic LLM → Template fallback

**Why:**
- Zero-risk deployment (templates always work)
- Progressive enhancement (LLM activates when ready)
- Automatic error recovery (2-second timeout)

**Impact:**
- User always gets a response (never fails silently)
- Easy to test LLM integration incrementally
- Safe to deploy before P7 complete

**Implementation:** Commit ae05cc3

---

### Decision 2: DJL SentencePiece Library (2025-11-13)

**What:** Use `ai.djl.sentencepiece:sentencepiece:0.33.0` for tokenization

**Alternatives Considered:**
1. Google SentencePiece JNI - Harder to integrate, unclear Maven availability
2. Pure Kotlin tokenizer - Slower (5-10x), more complex to implement
3. HTTP tokenizer service - Privacy concerns, latency overhead

**Why DJL:**
- Native Android ARM64 support included
- Well-maintained by AWS Deep Java Library team
- Production-ready, used by major projects
- Simple API, easy integration

**Impact:**
- Real tokenization working in 3 hours
- No custom JNI needed
- ~10MB added to APK size (acceptable)

**Implementation:** Commit 069e3b4

---

### Decision 3: Move IntentTemplates to LLM Module (2025-11-13)

**What:** Moved `IntentTemplates` from `Chat` module to `LLM` module

**Why:**
- Broke circular dependency: Chat ↔ LLM
- Templates are used by TemplateResponseGenerator (in LLM)
- Chat module should only depend on LLM, not vice versa

**Impact:**
- Clean dependency graph
- LLM module self-contained
- Updated 4 import statements across codebase

**Implementation:** Commit 2f4fa69

---

### Decision 4: Lazy Tokenizer Initialization (2025-11-13)

**What:** TVMTokenizer uses lazy initialization (`getTokenizer()` helper)

**Why:**
- Don't load 4MB model file until first use
- Saves startup time and memory
- Safe for multiple instances (singleton pattern)

**Implementation:**
```kotlin
private var spTokenizer: SpTokenizer? = null

private fun getTokenizer(): SpTokenizer {
    if (spTokenizer == null) {
        // Load model only on first use
        spTokenizer = SpTokenizer(Paths.get(modelPath))
    }
    return spTokenizer!!
}
```

---

### Decision 5: Graceful Fallback on Tokenization Errors (2025-11-13)

**What:** If SentencePiece fails, fall back to hash-based tokenization

**Why:**
- Enables testing without model file
- Robust error handling (no crashes)
- LLM can still attempt inference (may produce garbage, but won't crash)

**Implementation:**
```kotlin
private fun encodeSentencePiece(text: String): List<Int> {
    return try {
        val processor = getTokenizer().processor
        processor.encode(text).toList()
    } catch (e: Exception) {
        Timber.e(e, "SentencePiece encoding failed")
        // Fallback: hash-based tokenization
        text.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map { word -> (word.hashCode() and 0x7FFF) % getVocabSize() + 100 }
    }
}
```

---

## 🚧 Known Issues & Blockers

### BLOCKER #1: Asset File Access (CRITICAL)

**Issue:** TVMTokenizer uses `File(tokenizerPath)` which won't work for Android assets

**Current Code (WRONG):**
```kotlin
val modelFile = File(modelPath)  // ❌ Won't find assets
if (!modelFile.exists()) {
    throw IllegalStateException("Tokenizer model not found: $modelPath")
}
spTokenizer = SpTokenizer(Paths.get(modelPath))  // ❌ Path won't exist
```

**Problem:**
- Android assets are inside the APK, not the filesystem
- Can't use `File()` or `Paths.get()` directly
- Must copy asset to cache directory first, or use InputStream

**Solution Options:**

**Option A: Copy Asset to Cache (Recommended)**
```kotlin
private fun getTokenizer(): SpTokenizer {
    if (spTokenizer == null) {
        // Copy asset to cache directory
        val cacheFile = File(context.cacheDir, "tokenizer.model")

        if (!cacheFile.exists()) {
            context.assets.open(modelPath).use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        spTokenizer = SpTokenizer(cacheFile.toPath())
    }
    return spTokenizer!!
}
```

**Option B: Use InputStream Constructor**
```kotlin
private fun getTokenizer(): SpTokenizer {
    if (spTokenizer == null) {
        context.assets.open(modelPath).use { inputStream ->
            spTokenizer = SpTokenizer(inputStream)
        }
    }
    return spTokenizer!!
}
```

**Status:** ⚠️ NEEDS FIX - Required for tokenizer to work on device

---

### Decision 6: Local Storage Support for Testing (2025-11-17)

**What:** HuggingFaceModelDownloader checks for local source before downloading

**Why:**
- Testing: Avoid re-downloading 1-4GB models repeatedly
- Development: Faster iteration when testing model integration
- Offline: Works without internet during development
- Automatic: Falls back to HuggingFace if local unavailable

**Implementation:**
```kotlin
ModelInfo(
    id = "AVA-GEM-2B-Q4",
    localSourcePath = "/path/to/gemma-2b-it-q4f16_1-android.tar"
)

// Downloader checks local source first:
if (localSourcePath != null) {
    val result = copyFromLocalSource(config)
    if (result is Success) return@flow
}

// Falls back to HuggingFace download
downloadFile(huggingFaceRepo)
```

**Impact:**
- First test run: Fast local copy (seconds, not minutes)
- Subsequent runs: Uses cached copy
- Production: localSourcePath = null, automatic download
- Zero manual file management for users

**Implementation:** Commit ae7b738

---

### Decision 7: Language-Aware Model Selection (2025-11-17)

**What:** ModelSelector automatically selects best model for detected language

**Why:**
- Multilingual: Different models excel at different languages
- Quality: Qwen for Asian languages, Gemma for European
- Efficiency: Use smallest capable model (saves RAM, faster)
- Transparent: User doesn't need to choose manually

**Selection Logic:**
```kotlin
val (language, confidence) = languageDetector.detectWithConfidence(text)

when (language) {
    CHINESE, JAPANESE, KOREAN -> "AVA-QWN-1B-Q4"  // Qwen excels here
    ENGLISH, SPANISH, FRENCH -> "AVA-GEM-2B-Q4"   // Gemma default
    else -> findBestModelForLanguage(language)     // Check support
}
```

**Impact:**
- Automatic best-model selection
- No user configuration needed
- Supports 52+ languages via Qwen fallback
- Prefers downloaded models to avoid re-downloading

**Implementation:** Commit ae7b738

---

### Issue #2: Model Directory Path Configuration

**Issue:** No centralized configuration for model paths

**Current State:**
- Hardcoded: `"models/gemma-2b-it"` in various places
- No environment detection (emulator vs. device vs. test)

**Solution Needed:**
```kotlin
object ModelConfig {
    const val GEMMA_2B_MODEL_DIR = "models/gemma-2b-it"
    const val NLU_MODEL_DIR = "models"

    fun getGemmaTokenizerPath(): String {
        return "$GEMMA_2B_MODEL_DIR/tokenizer.model"
    }
}
```

**Status:** 🟡 Low Priority - Works with hardcoded paths for now

---

### Issue #3: LLMResponseGenerator Not Using Tokenizer Yet

**Issue:** LLMResponseGenerator exists but doesn't actually initialize/use TVMTokenizer

**Current State:**
- HybridResponseGenerator calls LLMResponseGenerator
- LLMResponseGenerator has TODO comments for P7
- No actual LLM inference happening yet

**What's Needed:**
1. Initialize TVMTokenizer in LocalLLMProvider
2. Pass tokenizer to LLMResponseGenerator
3. Use tokenizer in generation loop:
   ```kotlin
   val inputIds = tokenizer.encode(prompt, addBos = true)
   val outputIds = tvmRuntime.generate(inputIds)
   val responseText = tokenizer.decode(outputIds, skipSpecialTokens = true)
   ```

**Status:** 🟡 Medium Priority - Next step after fixing asset access

---

## ✅ Testing Status

### Unit Tests

**LLM Module:**
```
:Universal:AVA:Features:LLM:testDebugUnitTest
✅ ALCEngineTest - PASSING
✅ StopTokenDetectorTest - PASSING (30+ tests)
✅ TokenSamplerTest - PASSING
✅ LanguageDetectorTest - PASSING
✅ TVMTokenizerTest - PASSING (30+ tests)

Result: ALL PASSING ✅
```

**Chat Module:**
```
:Universal:AVA:Features:Chat:testDebugUnitTest
✅ ChatViewModelTest - PASSING (4 tests)
✅ IntentTemplatesTest - PASSING (19 tests)

Result: 23/23 PASSING ✅
```

### Integration Tests

**Status:** ⚠️ NOT YET CREATED

**Needed:**
1. TVMTokenizerIntegrationTest - Test with real tokenizer.model file
2. LLMResponseGeneratorIntegrationTest - End-to-end generation
3. HybridResponseGeneratorIntegrationTest - Fallback behavior

---

## 🔄 Recent Changes

### 2025-11-13: P7 (TVMTokenizer) Implementation

**Commits:**
- `069e3b4` - feat: implement real SentencePiece tokenization for P7
- `fbef246` - test: add comprehensive unit tests for TVMTokenizer
- `7ecde62` - docs: update P7 status with completion summary

**What Changed:**
- Added DJL SentencePiece dependency
- Implemented real encode/decode methods
- Created 30+ unit tests
- Documented completion

**Impact:**
- P7 core implementation complete
- Tokenization API ready for use
- ⚠️ Still needs asset file access fix

---

### 2025-11-13: LLM Integration into ChatViewModel

**Commits:**
- `9719325` - feat: add Hilt dependency injection to LLM module
- `2f4fa69` - refactor: move IntentTemplates from Chat to LLM module
- `ae05cc3` - feat: integrate LLM response generation into ChatViewModel
- `68bf6d0` - test: update ChatViewModel tests for ResponseGenerator
- `e2c36ae` - fix: update IntentTemplates imports and ResponseGenerator initialization

**What Changed:**
- Added HybridResponseGenerator to ChatViewModel
- Set up Hilt DI for LLM module
- Broke circular dependency (moved IntentTemplates)
- Updated all imports and tests

**Impact:**
- LLM system wired into UI layer
- Automatic LLM → Template fallback working
- Zero breaking changes, all tests passing

---

## 📊 Performance Characteristics

### Expected Latency (On-Device)

**Tokenization:**
- Short text (10 words): <10ms
- Medium text (50 words): <20ms
- Long text (200 words): <50ms

**LLM Inference (Gemma 2B, Quantized Q4F16):**
- First token (prompt processing): 200-500ms
- Subsequent tokens: 50-100ms each
- Total for 50-token response: 3-6 seconds

**Total End-to-End:**
- User sends message → LLM response visible: <7 seconds (acceptable)

### Memory Usage

**Expected:**
- TVMTokenizer: ~10MB (loaded model)
- TVM Runtime: ~50MB (model weights in memory)
- ALCEngine: ~5MB (inference buffers)
- Total LLM system: ~65MB

**APK Size Impact:**
- DJL SentencePiece: ~2MB
- TVM Runtime JARs: ~5MB
- Native libs: ~8MB
- Model files (in assets): ~25MB (Gemma 2B quantized)
- Total addition: ~40MB

---

## 🎯 Next Steps & Priorities

### Priority 1: CRITICAL - Fix Asset File Access

**Task:** Update TVMTokenizer to handle Android asset files correctly

**Implementation:**
1. Add Context parameter to `getTokenizer()`
2. Copy asset to cache directory on first use
3. Update tests to mock asset access
4. Verify on emulator/device

**ETA:** 1-2 hours

---

### Priority 2: HIGH - Enable LLM Inference

**Task:** Wire up TVMTokenizer in LLMResponseGenerator

**Implementation:**
1. Initialize tokenizer in LocalLLMProvider
2. Update LLMResponseGenerator.generateResponse():
   ```kotlin
   val inputIds = tokenizer.encode(prompt, addBos = true)
   val outputIds = tvmRuntime.generate(inputIds, maxTokens = 100)
   val text = tokenizer.decode(outputIds, skipSpecialTokens = true)
   ```
3. Stream chunks as ResponseChunk.Text
4. Handle errors gracefully

**ETA:** 2-3 hours

---

### Priority 3: MEDIUM - Integration Testing

**Task:** Create integration tests with real model file

**Tests:**
1. TVMTokenizerIntegrationTest:
   - Load real tokenizer.model from assets
   - Test encode/decode roundtrip accuracy
   - Verify vocabulary size matches (32000)

2. LLMResponseGeneratorIntegrationTest:
   - Generate real LLM responses
   - Verify streaming works
   - Test timeout/error handling

**ETA:** 2-3 hours

---

### Priority 4: LOW - Configuration Management

**Task:** Centralize model path configuration

**Implementation:**
1. Create ModelConfig object
2. Move all hardcoded paths to config
3. Support environment-specific paths
4. Document in this file

**ETA:** 1 hour

---

## 📚 Reference Documentation

### Internal Docs

- `docs/P7-COMPLETION-2025-11-13.md` - P7 implementation summary
- `docs/P7-TVMTOKENIZER-STATUS.md` - Original P7 analysis
- `docs/LLM-INTEGRATION-2025-11-13.md` - ChatViewModel integration
- `docs/Developer-Manual-Chapter28-RAG.md` - RAG system (related)

### External References

**DJL SentencePiece:**
- GitHub: https://github.com/deepjavalibrary/djl
- JavaDoc: https://javadoc.io/doc/ai.djl.sentencepiece/sentencepiece

**SentencePiece:**
- Paper: https://arxiv.org/abs/1808.06226
- GitHub: https://github.com/google/sentencepiece

**Gemma Model:**
- Model Card: https://ai.google.dev/gemma
- HuggingFace: https://huggingface.co/google/gemma-2b-it

**TVM/MLC-LLM:**
- TVM: https://tvm.apache.org/
- MLC-LLM: https://llm.mlc.ai/
- Android Deploy: https://tvm.apache.org/docs/deploy/android.html

---

## 🔍 Troubleshooting Guide

### Problem: Tokenizer fails to load model

**Symptoms:**
```
E/TVMTokenizer: SentencePiece encoding failed
W/TVMTokenizer: Using placeholder SentencePiece tokenization
```

**Diagnosis:**
1. Check if model file exists:
   ```bash
   adb shell ls /data/data/com.augmentalis.ava/cache/tokenizer.model
   ```
2. Check file permissions
3. Check logcat for "Tokenizer model not found" warning

**Solutions:**
- Verify asset file is in APK: `unzip -l app-debug.apk | grep tokenizer`
- Check asset path is correct (no leading `/`)
- Try copying asset to cache directory explicitly

---

### Problem: LLM responses are templates, not actual LLM

**Symptoms:**
- Responses are static template strings
- No typewriter/streaming effect
- Fast responses (<100ms)

**Diagnosis:**
- Check logs for "LLM generation failed, falling back to template"
- Verify tokenizer initialized successfully
- Check TVM runtime loaded model

**Solutions:**
- Fix tokenizer asset access (see Priority 1)
- Enable LLM inference in LLMResponseGenerator (see Priority 2)
- Verify model files are complete in assets

---

### Problem: OOM (Out of Memory) on LLM inference

**Symptoms:**
```
E/AndroidRuntime: java.lang.OutOfMemoryError: Failed to allocate
```

**Diagnosis:**
- Check heap usage before inference
- Verify model is quantized (Q4F16, not full precision)
- Check if multiple model instances loaded

**Solutions:**
- Use quantized model (gemma-2b-it-q4f16)
- Implement singleton pattern for LocalLLMProvider
- Close tokenizer when not in use: `tokenizer.close()`
- Reduce max context length

---

## 📝 Update History

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-13 | Claude (Sonnet 4.5) | Initial creation - comprehensive living document |
| 2025-11-17 | Claude (Sonnet 4.5) | Updated for AVA naming, ModelSelector, local storage support |

---

## 🏷️ Tags

`#llm` `#tokenizer` `#p7` `#sentencepiece` `#gemma` `#tvm` `#mlc-llm` `#living-document` `#dependencies` `#architecture`

---

**END OF LIVING DOCUMENT**

*This document is maintained automatically. Last generated: 2025-11-13*
