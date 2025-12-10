# AVA AI Developer Manual - Addendum (2025-11-03)

**Purpose:** This addendum documents the significant updates made to AVA on November 3, 2025, including cloud LLM provider support, model download manager, critical test suites, and null safety improvements.

**To Integrate:** This content should be integrated into the main Developer Manual chapters as indicated below.

---

## Update Summary

### What Changed (2025-11-03)

1. **Cloud LLM Provider Support** → Integrate into Chapter 11 (ALC Engine)
2. **Model Download Manager** → Integrate into Chapter 11 (ALC Engine)
3. **Critical Test Suites (82 tests)** → Integrate into Chapter 20 (Testing)
4. **Null Safety Improvements** → Integrate into Chapter 22 (Best Practices)
5. **Unsafe Cast Fixes** → Integrate into Chapter 22 (Best Practices)

---

## 1. Cloud LLM Provider Support (→ Chapter 11)

### Overview

The LLM module now supports cloud-based providers alongside local on-device models:

- **Anthropic Claude** - Advanced reasoning and long-context understanding
- **OpenRouter** - Aggregated access to 50+ models (Mistral, Llama, GPT-4, etc.)
- **Local Models** - Private, on-device inference with Gemma 2B

### Architecture

```
LLMProvider (interface)
├── LocalLLMProvider       - On-device models (Gemma 2B)
├── AnthropicProvider      - Claude 3.5 Sonnet, Claude 3 Opus
└── OpenRouterProvider     - Mistral, Llama, GPT-4, and 50+ others
```

### New Interface Methods

```kotlin
// Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/domain/LLMProvider.kt

interface LLMProvider {
    // Existing methods...
    suspend fun generate(prompt: String): Result<String>
    suspend fun chat(messages: List<Message>): Result<String>

    // NEW: Health monitoring
    suspend fun checkHealth(): Result<ProviderHealth>

    // NEW: Cost estimation
    fun estimateCost(inputTokens: Int, outputTokens: Int): Double
}

data class ProviderHealth(
    val isAvailable: Boolean,
    val latency: Long?,              // milliseconds
    val errorRate: Double?,          // 0.0 to 1.0
    val lastChecked: Long,           // timestamp
    val message: String?             // optional status message
)
```

### API Key Security

API keys are stored with AES-256 encryption using Android's EncryptedSharedPreferences:

```kotlin
// Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/security/ApiKeyManager.kt

class ApiKeyManager(context: Context) {
    suspend fun saveApiKey(provider: ProviderType, key: String): Result<Unit>
    suspend fun getApiKey(provider: ProviderType): Result<String>
    suspend fun deleteApiKey(provider: ProviderType): Result<Unit>
    fun hasApiKey(provider: ProviderType): Boolean
    fun validateKeyFormat(provider: ProviderType, key: String): Boolean
}
```

**Key Features:**
- Keys encrypted at rest with AES-256-GCM
- Keys never logged in plaintext (masked as `sk-ant-...***`)
- Format validation per provider
- Environment variable support (`AVA_ANTHROPIC_API_KEY`)

### Provider Configuration

```kotlin
// Example: Configure Anthropic provider
val apiKeyManager = ApiKeyManager(context)
apiKeyManager.saveApiKey(ProviderType.ANTHROPIC, "sk-ant-api03-...")

val provider = AnthropicProvider(
    apiKeyManager = apiKeyManager,
    model = "claude-3-5-sonnet-20241022"
)

// Check health before use
when (val health = provider.checkHealth()) {
    is Result.Success -> {
        if (health.data.isAvailable) {
            val response = provider.chat(messages)
        }
    }
    is Result.Error -> {
        // Fallback to local model
    }
}
```

### Cost Estimation

```kotlin
// Estimate cost before sending request
val inputTokens = 1000
val outputTokens = 500

val anthropicCost = anthropicProvider.estimateCost(inputTokens, outputTokens)
// Returns: $0.015 (3000 chars × $3/$1M + 500 × $15/$1M)

val localCost = localProvider.estimateCost(inputTokens, outputTokens)
// Returns: 0.0 (local inference is free)
```

**Provider Pricing:**
- **Anthropic Claude 3.5 Sonnet:** $3/1M input, $15/1M output tokens
- **OpenRouter (varies):** $0.50-$30/1M tokens depending on model
- **Local Models:** Free

### Dependencies Added

```kotlin
// Universal/AVA/Features/LLM/build.gradle.kts

dependencies {
    // NEW: HTTP client for cloud providers
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // NEW: Secure key storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

### Files Added

1. `AnthropicProvider.kt` (387 lines)
2. `OpenRouterProvider.kt` (412 lines)
3. `ApiKeyManager.kt` (298 lines)

**Commit:** 17f626d

---

## 2. Model Download Manager (→ Chapter 11)

### Problem Solved

Original APK size: **160 MB** (with bundled Gemma 2B model)
Target APK size: **8 MB** (95% reduction)

Solution: On-demand model downloads with progress tracking, pause/resume, and SHA-256 verification.

### Architecture

```
ModelDownloadManager
├── DownloadState      - State machine (Idle, Downloading, Paused, Completed, Error)
├── ModelDownloadConfig - Model metadata (URL, checksum, size, version)
├── ModelCacheManager  - Local storage with cache-first loading
└── DownloadWorker     - Background download with HTTP range requests
```

### Core API

```kotlin
// Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/download/ModelDownloadManager.kt

class ModelDownloadManager(
    private val context: Context,
    private val cacheManager: ModelCacheManager
) {
    /**
     * Download a model with progress updates
     * Returns Flow<DownloadState> for UI binding
     */
    suspend fun downloadModel(config: ModelDownloadConfig): Flow<DownloadState>

    /**
     * Ensure model is available (download if needed)
     * Returns cached model immediately if available
     */
    suspend fun ensureModelAvailable(config: ModelDownloadConfig): Flow<DownloadState>

    /**
     * Pause an in-progress download
     */
    suspend fun pauseDownload(modelId: String)

    /**
     * Resume a paused download
     */
    suspend fun resumeDownload(modelId: String)

    /**
     * Cancel and delete partial download
     */
    suspend fun cancelDownload(modelId: String)
}
```

### Download States

```kotlin
sealed class DownloadState {
    object Idle : DownloadState()

    data class Downloading(
        val progress: Float,          // 0.0 to 1.0
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long
    ) : DownloadState()

    data class Paused(
        val progress: Float,
        val bytesDownloaded: Long
    ) : DownloadState()

    data class Completed(
        val modelPath: String,
        val sizeBytes: Long
    ) : DownloadState()

    data class Error(
        val exception: Throwable,
        val message: String,
        val canRetry: Boolean
    ) : DownloadState()
}
```

### Model Registry

```kotlin
// Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/download/ModelDownloadConfig.kt

object ModelRegistry {
    val GEMMA_2B_IT_INT4 = ModelDownloadConfig(
        modelId = "gemma-2b-it-q4f16_1-MLC",
        displayName = "Gemma 2B Instruct (Quantized)",
        url = "https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC/resolve/main/",
        sizeBytes = 1_500_000_000L,  // 1.5 GB
        checksum = "a1b2c3d4e5f6...",  // SHA-256
        version = "1.0.0",
        requirements = ModelRequirements(
            minAndroidSdk = 24,
            minRamMb = 2048,
            minStorageMb = 2000
        )
    )

    val MOBILE_BERT_INT8 = ModelDownloadConfig(
        modelId = "mobilebert-uncased-squad-int8",
        displayName = "MobileBERT (SQuAD)",
        url = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/bert_qa/mobile_bert/",
        sizeBytes = 100_000_000L,  // 100 MB
        checksum = "f1e2d3c4b5a6...",
        version = "1.0.0",
        requirements = ModelRequirements(
            minAndroidSdk = 24,
            minRamMb = 512,
            minStorageMb = 150
        )
    )
}
```

### Usage Example

```kotlin
// In your ViewModel or Repository
class ChatViewModel(
    private val downloadManager: ModelDownloadManager
) : ViewModel() {

    val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    fun ensureModelReady() {
        viewModelScope.launch {
            downloadManager.ensureModelAvailable(ModelRegistry.GEMMA_2B_IT_INT4)
                .collect { state ->
                    downloadState.value = state

                    when (state) {
                        is DownloadState.Completed -> {
                            // Initialize LocalLLMProvider with model path
                            initializeLLM(state.modelPath)
                        }
                        is DownloadState.Error -> {
                            // Show error, offer cloud fallback
                            showError(state.message)
                        }
                        else -> {}
                    }
                }
        }
    }

    fun pauseDownload() {
        viewModelScope.launch {
            downloadManager.pauseDownload(ModelRegistry.GEMMA_2B_IT_INT4.modelId)
        }
    }
}
```

### Features

1. **Pause/Resume:** Uses HTTP Range requests to resume from byte offset
2. **SHA-256 Verification:** Validates integrity after download
3. **Cache-First Loading:** Checks local cache before downloading
4. **Progress Tracking:** Flow-based updates for UI binding
5. **Concurrent Downloads:** Manages multiple models simultaneously
6. **Storage Quotas:** Enforces max cache size, auto-cleanup old models
7. **Network Awareness:** Detects WiFi vs cellular, adjusts behavior

### Files Added

1. `DownloadState.kt` (235 lines)
2. `ModelDownloadConfig.kt` (343 lines)
3. `ModelCacheManager.kt` (387 lines)
4. `ModelDownloadManager.kt` (550 lines)

**Total:** 1,877 lines
**Commit:** 5881fea

---

## 3. Critical Test Suites (→ Chapter 20)

### Overview

Created **5 comprehensive test suites** with **82 tests** covering critical failure modes:

1. **DatabaseMigrationTest.kt** (10 tests) - Room schema migrations
2. **ModelLoadingCrashTest.kt** (17 tests) - ML model loading failures
3. **NullSafetyRegressionTest.kt** (18 tests) - Null pointer exception prevention
4. **ApiKeyEncryptionTest.kt** (22 tests) - API key security
5. **LLMProviderFallbackTest.kt** (15 tests) - Provider fallback chain

**Total:** 82 tests, 2,075 lines of test code

### 3.1 DatabaseMigrationTest.kt

**Location:** `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/ava/core/data/migration/DatabaseMigrationTest.kt`

**Purpose:** Validates Room database schema migrations don't lose data

**Test Cases:**
- `migrate_v1_to_v2_preserves_conversation_data()` - Future migration test
- `migrate_preserves_foreign_key_constraints()` - FK constraint survival
- `migrate_preserves_unique_constraints()` - Unique constraint survival
- `migrate_preserves_indices()` - Index maintenance
- `downgrade_from_v2_to_v1_fails_gracefully()` - Downgrade handling
- `cascade_delete_survives_migration()` - Cascade delete behavior
- `empty_database_migration_succeeds()` - Empty DB migration
- `large_dataset_migration_preserves_all_data()` - Stress test (1000 records)
- `migration_maintains_column_types()` - Column type preservation

**Run Command:**
```bash
./gradlew :Universal:AVA:Core:Data:connectedAndroidTest --tests "DatabaseMigrationTest"
```

### 3.2 ModelLoadingCrashTest.kt

**Location:** `Universal/AVA/Features/LLM/src/test/java/com/augmentalis/ava/features/llm/provider/ModelLoadingCrashTest.kt`

**Purpose:** Tests graceful degradation when ML models fail to load

**Key Tests:**
- `missing model file returns error instead of crashing()`
- `corrupted model file returns clear error message()`
- `null model path returns error()`
- `inference before initialization returns error()`
- `provider health check fails gracefully when not initialized()`
- `concurrent initialization attempts are safe()`

**Coverage:** Missing/corrupted models, uninitialized provider state, invalid inputs, concurrent access

**Run Command:**
```bash
./gradlew :Universal:AVA:Features:LLM:testDebugUnitTest --tests "ModelLoadingCrashTest"
```

### 3.3 NullSafetyRegressionTest.kt ✅ PASSING

**Location:** `Universal/AVA/Core/Common/src/androidUnitTest/kotlin/com/augmentalis/ava/core/common/regression/NullSafetyRegressionTest.kt`

**Purpose:** Prevents regression of null pointer exceptions from previous fixes

**Key Tests:**
- `Result_Error with exception does not crash()`
- `Result_Error with null message does not crash()`
- `Result_Success with null data throws appropriate exception()`
- `nested Result types handle null correctly()`
- `Result map function handles null transform gracefully()`
- `equals implementation handles null correctly()`
- `Result in collections handles correctly()`

**Coverage:** Tests all Result.Success and Result.Error operations with null inputs

**Run Command:**
```bash
./gradlew :Universal:AVA:Core:Common:testDebugUnitTest --tests "NullSafetyRegressionTest"
```

**Status:** ✅ All tests passing

### 3.4 ApiKeyEncryptionTest.kt

**Location:** `Universal/AVA/Features/LLM/src/test/java/com/augmentalis/ava/features/llm/security/ApiKeyEncryptionTest.kt`

**Purpose:** Validates secure storage of LLM provider API keys

**Key Tests:**
- `API keys are stored using EncryptedSharedPreferences()`
- `API keys are encrypted with AES-256()`
- `API keys are never logged in plaintext()`
- `API keys are masked in logs()`
- `encryption decryption round-trip preserves key()`
- `invalid Anthropic key format is rejected()`
- `environment variable takes precedence over stored key()`
- `concurrent key operations are safe()`

**Coverage:** AES-256 encryption, key validation, logging security, round-trip integrity

**Run Command:**
```bash
./gradlew :Universal:AVA:Features:LLM:testDebugUnitTest --tests "ApiKeyEncryptionTest"
```

### 3.5 LLMProviderFallbackTest.kt

**Location:** `Universal/AVA/Features/LLM/src/test/java/com/augmentalis/ava/features/llm/provider/LLMProviderFallbackTest.kt`

**Purpose:** Tests automatic fallback chain (Local → Anthropic → OpenRouter)

**Key Tests:**
- `fallback uses highest priority provider first()`
- `fallback to second provider when first fails()`
- `fallback to third provider when first two fail()`
- `throws exception when all providers fail()`
- `result includes metadata about provider used()`
- `getProviderStats returns statistics for all providers()`

**Coverage:** Priority-based fallback, provider availability, error propagation, metadata enrichment

**Run Command:**
```bash
./gradlew :Universal:AVA:Features:LLM:testDebugUnitTest --tests "LLMProviderFallbackTest"
```

### Summary

| Metric | Value |
|--------|-------|
| **Test Suites** | 5 files |
| **Total Tests** | 82 tests |
| **Lines of Code** | 2,075 LOC |
| **Currently Passing** | 1 suite (NullSafetyRegressionTest) |
| **Ready for Testing** | 1 suite (DatabaseMigrationTest - needs device) |
| **Need Minor Fixes** | 3 suites (ModelLoadingCrashTest, ApiKeyEncryptionTest, LLMProviderFallbackTest) |

**Commit:** a96acd6

---

## 4. Null Safety Improvements (→ Chapter 22)

### Critical Violations Fixed

Fixed **6 critical null safety violations** that could crash the app:

#### 4.1 IntentTemplates.kt (Line 80)

**Location:** `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/data/IntentTemplates.kt`

**Issue:** Force unwrap on fallback template
```kotlin
// BEFORE (crashes if "unknown" template missing):
return templates[intent] ?: templates["unknown"]!!

// AFTER (hardcoded fallback):
return templates[intent] ?: templates["unknown"]
    ?: "I didn't understand that. Can you rephrase?"
```

**Impact:** Prevents crash when intent not recognized and fallback template missing

#### 4.2 TVMModelLoader.kt (Line 46)

**Location:** `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/loader/TVMModelLoader.kt`

**Issue:** Force unwrap on TVM runtime instance
```kotlin
// BEFORE (crashes if TVM runtime initialization failed):
val tvmModule = runtime!!.loadModule(...)

// AFTER (proper null check with exception):
val runtimeInstance = runtime ?: throw ModelLoadException(
    "TVM runtime not initialized. Call initialize() first."
)
val tvmModule = runtimeInstance.loadModule(...)
```

**Impact:** Prevents crash when TVM runtime initialization fails

#### 4.3 NluConnector.kt (Lines 66, 72, 77, 86)

**Location:** `Universal/AVA/Features/Overlay/src/main/java/com/augmentalis/ava/features/overlay/integration/NluConnector.kt`

**Issue:** 4 force unwraps on nullable dependencies
```kotlin
// BEFORE (crashes if models unavailable):
if (!modelManager!!.isModelAvailable()) {
    when (modelManager!!.downloadModelsIfNeeded()) { ... }
}
val modelPath = modelManager!!.getModelPath()

// AFTER (refactored to safe null checks):
private suspend fun ensureInitialized(): Boolean {
    if (!initialized) {
        val manager = modelManager ?: return false
        val classifier = intentClassifier ?: return false

        if (!manager.isModelAvailable()) {
            when (manager.downloadModelsIfNeeded()) {
                is Result.Error -> return false
                else -> {}
            }
        }

        initialized = true
    }
    return initialized
}
```

**Impact:** Prevents overlay crash when NLU models unavailable

#### 4.4 LLM Module Result.Error (3 files)

**Files:**
- `AnthropicProvider.kt`
- `OpenRouterProvider.kt`
- `ApiKeyManager.kt`

**Issue:** Passing null Throwable to Result.Error (requires non-null)
```kotlin
// BEFORE (type violation):
return Result.Error(exception = null, message = "No API key found")

// AFTER (proper exception):
return Result.Error(
    exception = IllegalStateException("No API key found for Anthropic"),
    message = "No API key found for Anthropic. ${it.message}"
)
```

**Impact:** Prevents type system violation and potential crashes

### Commit

**Commit:** 6d5ea80 (first 6 fixes)

---

## 5. Unsafe Cast Fixes (→ Chapter 22)

### Overview

Fixed **10 unsafe casts** in production code (left 45 test casts as standard testing pattern):

### High Risk Fixes

#### 5.1 OverlayService.kt
```kotlin
// BEFORE (crashes if WindowManager unavailable):
val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

// AFTER (safe cast with validation):
val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    ?: throw IllegalStateException("WindowManager service not available")
```

#### 5.2 IntentClassifier.kt
```kotlin
// BEFORE (crashes if model output wrong type):
val outputArray = output[0] as Array<FloatArray>

// AFTER (safe cast with validation):
val outputArray = output[0] as? Array<FloatArray>
    ?: throw ModelOutputException("Unexpected model output format")
```

#### 5.3 ModelManager.kt
```kotlin
// BEFORE (crashes if HTTP connection fails):
val connection = url.openConnection() as HttpURLConnection

// AFTER (safe cast with validation):
val connection = url.openConnection() as? HttpURLConnection
    ?: throw IOException("Failed to establish HTTP connection")
```

### Medium Risk Fixes

- **LanguagePackManager.kt** (2 casts) - Result type casting
- **ChatViewModelConfidenceTest.kt** (2 casts) - Reflection type casting

### Low Risk Fixes

- **IntentClassifier.kt** - Model input casting
- **Models.kt** - equals() type checking
- **BertTokenizer.kt** - equals() type checking

### Analysis

- **Total unsafe casts found:** 55
- **Production code:** 10 (ALL FIXED)
- **Test code:** 45 (LEFT AS-IS - standard testing pattern)

**Rationale for test casts:** Test code commonly uses unsafe casts after type checks (e.g., `assertTrue(x is Type); val y = x as Type`). This is acceptable because:
1. Tests are controlled environments
2. Casts follow explicit type checks
3. Crashes in tests are intentional (fail-fast)

### Commit

**Commit:** 07f3c5b

---

## 6. APK Build Results

### Build Information

**Command:**
```bash
./gradlew :apps:ava-standalone:assembleDebug
```

**Result:** BUILD SUCCESSFUL in 9s

**APK Location:**
```
apps/ava-standalone/build/outputs/apk/debug/ava-standalone-debug.apk
```

**APK Size:** 87 MB (current, without model download optimization)

**Note:** APK size will reduce to ~8 MB once models are moved to cloud storage and downloaded on-demand using ModelDownloadManager.

---

## 7. Integration Checklist

To integrate this addendum into the main Developer Manual:

- [ ] Add Cloud LLM Provider section to Chapter 11 (ALC Engine)
- [ ] Add Model Download Manager section to Chapter 11 (ALC Engine)
- [ ] Add Test Suite documentation to Chapter 20 (Testing)
- [ ] Add Null Safety best practices to Chapter 22 (Best Practices)
- [ ] Add Unsafe Cast prevention to Chapter 22 (Best Practices)
- [ ] Update version number to 1.1
- [ ] Update "Last Updated" date to 2025-11-03
- [ ] Add changelog entry for November 2025 updates

---

## 8. Related Documents

- **Test Suite Summary:** `docs/active/Test-Suite-Creation-Summary-251103.md`
- **Unsafe Casts Report:** `docs/active/Unsafe-Casts-Fix-Report-251103.md`
- **Code Review:** `docs/active/Code-Review-251103-1800.md`

---

**Document Version:** 1.0
**Created:** 2025-11-03
**Author:** AVA AI Team
**Status:** Ready for integration
