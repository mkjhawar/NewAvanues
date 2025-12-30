# Developer Manual - Chapter 38: LLM Model Management

**Last Updated:** 2025-11-17
**Author:** AVA Development Team
**Status:** Production

---

## Table of Contents

1. [Overview](#overview)
2. [AVA Model Naming Convention](#ava-model-naming-convention)
3. [Available Models](#available-models)
4. [Model Download & Storage](#model-download--storage)
5. [Local Testing Support](#local-testing-support)
6. [Security & Obfuscation](#security--obfuscation)
7. [Native Library Integration (llama.cpp)](#native-library-integration-llamacpp)
8. [API Reference](#api-reference)
9. [Troubleshooting](#troubleshooting)

---

## Overview

AVA's LLM system uses **MLC-LLM** (Machine Learning Compilation) with **Apache TVM** runtime for on-device inference. The system supports 5 quantized models (4-bit) optimized for mobile devices, with automatic language detection and model selection.

### Key Features

- ✅ **Proprietary naming** - AVA-{TYPE}-{SIZE}-{QUANT} format
- ✅ **5 models** - Gemma, Qwen, Llama, Phi, Mistral
- ✅ **Auto-selection** - Language detection → best model
- ✅ **Local-first** - Copy from local downloads for testing
- ✅ **Fallback download** - HuggingFace download if local unavailable
- ✅ **Privacy-focused** - 95%+ on-device processing

---

## AVA Model Naming Convention

### Format

```
AVA-{TYPE}-{SIZE}-{QUANT}.{ext}
```

**Components:**
- `AVA` - Prefix (all models)
- `TYPE` - Model type (GEM, QWN, LLM, PHI, MST)
- `SIZE` - Parameters (1B, 2B, 3B, 7B)
- `QUANT` - Quantization (Q4, Q8, F16)
- `ext` - Extension (.tar, .gguf, .bin)

### Type Codes

| Code | Model Family | Company |
|------|-------------|---------|
| **GEM** | Gemma | Google |
| **QWN** | Qwen | Alibaba |
| **LLM** | Llama | Meta |
| **PHI** | Phi | Microsoft |
| **MST** | Mistral | Mistral AI |

### Security Benefits

**Filename Obscurity:**
- ✅ Hides model architecture from inspection
- ✅ No version numbers exposed
- ✅ Source unclear without registry
- ✅ Reduces competitive intelligence exposure

**Example:**
```
User sees: AVA-GEM-2B-Q4.tar
Reality: Google Gemma 2B, 4-bit quantized, from mlc-ai/gemma-2b-it-q4f16_1-MLC
```

---

## Available Models

### 1. AVA-GEM-2B-Q4 (Default)

**Display Name:** Gemma 2B Instruct
**Size:** 1.2 GB
**Languages:** English, Spanish, French, German, Italian, Portuguese
**Strengths:** European languages, instruction following
**Weaknesses:** Asian languages, Arabic

**Original:** `gemma-2b-it-q4f16_1` (Google)
**Source:** `mlc-ai/gemma-2b-it-q4f16_1-MLC`

### 2. AVA-QWN-1B-Q4 (Multilingual)

**Display Name:** Qwen 2.5 1.5B Instruct
**Size:** 1.0 GB (smallest)
**Languages:** All languages (52+)
**Strengths:** Multilingual, Asian languages, efficient
**Weaknesses:** Smaller model, less nuanced English

**Original:** `qwen2.5-1.5b-instruct-q4f16_1` (Alibaba)
**Source:** `mlc-ai/Qwen2.5-1.5B-Instruct-q4f16_1-MLC`

### 3. AVA-LLM-3B-Q4 (Balanced)

**Display Name:** Llama 3.2 3B Instruct
**Size:** 1.9 GB
**Languages:** English, Spanish, French, German, Italian, Portuguese, Russian
**Strengths:** Larger model, better reasoning
**Weaknesses:** Larger size, limited Asian language support

**Original:** `Llama-3.2-3B-Instruct-q4f16_1` (Meta)
**Source:** `mlc-ai/Llama-3.2-3B-Instruct-q4f16_1-MLC`

### 4. AVA-PHI-3B-Q4 (English Specialist)

**Display Name:** Phi 3.5 Mini
**Size:** 2.4 GB
**Languages:** English only
**Strengths:** Strong English, reasoning, math
**Weaknesses:** English-only, largest size

**Original:** `Phi-3.5-mini-instruct-q4f16_1` (Microsoft)
**Source:** `mlc-ai/Phi-3.5-mini-instruct-q4f16_1-MLC`

### 5. AVA-MST-7B-Q4 (Highest Quality)

**Display Name:** Mistral 7B Instruct
**Size:** 4.5 GB
**Languages:** English, Spanish, French, German, Italian
**Strengths:** Best quality, reasoning
**Weaknesses:** Very large, slow on mobile, high memory

**Original:** `Mistral-7B-Instruct-v0.3-q4f16_1` (Mistral AI)
**Source:** `mlc-ai/Mistral-7B-Instruct-v0.3-q4f16_1-MLC`

---

## Model Download System (NEW: 2025-12-06)

AVA now includes an automatic model download system that allows users to download LLM models directly from within the app.

### Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│              User Interface (Settings)              │
│         Model Management → Available Models         │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│             LLMModelDownloader                      │
│  • Orchestrates download workflow                   │
│  • Progress tracking via StateFlow                  │
│  • Concurrent download management (max 2)           │
└──────────────────┬──────────────────────────────────┘
                   │
                   ├──────────────────┬────────────────┐
                   ▼                  ▼                ▼
         ┌─────────────────┐  ┌──────────────┐  ┌──────────────┐
         │ HuggingFaceClient│  │ LLMDownload  │  │ ModelStorage │
         │                  │  │   Worker     │  │   Manager    │
         │ • Fetch metadata │  │              │  │              │
         │ • 4 model catalog│  │ • Background │  │ • Storage    │
         │ • URL resolution │  │   download   │  │   location   │
         │                  │  │ • HTTP Range │  │ • Space check│
         │                  │  │ • SHA-256    │  │ • Cleanup    │
         └──────────────────┘  └──────────────┘  └──────────────┘
```

### Components

#### LLMModelDownloader

**Location:** `common/LLM/src/main/kotlin/.../llm/download/LLMModelDownloader.kt`

**Responsibility:** Orchestrates model download workflow

**Key Features:**
- Progress tracking via StateFlow
- Concurrent download management (max 2 simultaneous)
- Download queue for additional requests
- Automatic retry on failure (up to 3 attempts)
- Integration with WorkManager for background downloads

**Methods:**
```kotlin
class LLMModelDownloader @Inject constructor(
    private val context: Context,
    private val client: HuggingFaceClient,
    private val storageManager: ModelStorageManager
) {
    // Start download for a model
    suspend fun downloadModel(modelId: String): Flow<DownloadProgress>

    // Cancel ongoing download
    fun cancelDownload(modelId: String)

    // Get download status
    fun getDownloadStatus(modelId: String): DownloadStatus

    // Check if model is downloaded
    fun isModelDownloaded(modelId: String): Boolean
}
```

**Download Progress:**
```kotlin
data class DownloadProgress(
    val modelId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Int,
    val speedMBps: Double,
    val status: DownloadStatus
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

---

#### HuggingFaceClient

**Location:** `common/LLM/src/main/kotlin/.../llm/download/HuggingFaceClient.kt`

**Responsibility:** Fetch model metadata and resolve download URLs

**Available Models:**
| Model ID | HuggingFace Repo | Size | Description |
|----------|------------------|------|-------------|
| `qwen2-1.5b` | `Qwen/Qwen2-1.5B-Instruct-GGUF` | 1GB | Fastest, multilingual |
| `gemma-2-2b` | `bartowski/gemma-2-2b-it-GGUF` | 1.5GB | Recommended, balanced |
| `gemma-2-9b` | `bartowski/gemma-2-9b-it-GGUF` | 1.7GB | Better quality |
| `phi-3-mini` | `microsoft/Phi-3-mini-4k-instruct-gguf` | 2.3GB | Advanced features |

**Methods:**
```kotlin
class HuggingFaceClient {
    // Get available models catalog
    fun getAvailableModels(): List<ModelMetadata>

    // Fetch metadata for specific model
    suspend fun fetchModelMetadata(modelId: String): ModelMetadata

    // Resolve download URL
    fun resolveDownloadUrl(repoId: String, filename: String): String
}
```

---

#### LLMDownloadWorker

**Location:** `common/LLM/src/main/kotlin/.../llm/download/LLMDownloadWorker.kt`

**Responsibility:** Background download with progress tracking

**Key Features:**
- Runs as WorkManager Worker for reliability
- HTTP Range requests for resume support
- SHA-256 checksum verification
- Foreground notification with progress
- Handles network failures gracefully

**Work Parameters:**
```kotlin
val downloadRequest = OneTimeWorkRequestBuilder<LLMDownloadWorker>()
    .setInputData(workDataOf(
        "model_id" to "gemma-2-2b",
        "repo_id" to "bartowski/gemma-2-2b-it-GGUF",
        "filename" to "gemma-2-2b-it-Q4_K_M.gguf",
        "download_url" to url,
        "expected_sha256" to checksum
    ))
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .build()
```

**Progress Updates:**
```kotlin
override suspend fun doWork(): Result {
    // Update progress periodically
    setProgress(workDataOf(
        "bytes_downloaded" to bytesDownloaded,
        "total_bytes" to totalBytes,
        "percentage" to (bytesDownloaded * 100 / totalBytes).toInt()
    ))

    // Download with resume support
    downloadWithResume(url, outputFile, expectedChecksum)

    return Result.success()
}
```

---

#### ModelStorageManager

**Location:** `common/LLM/src/main/kotlin/.../llm/download/ModelStorageManager.kt`

**Responsibility:** Manage model storage locations and cleanup

**Storage Location:**
```
/sdcard/ava-ai-models/llm-gguf/
├── qwen2-1_5b-instruct-q4_0.gguf           (1GB)
├── gemma-2-2b-it-Q4_K_M.gguf               (1.5GB)
├── gemma-2-9b-it-Q4_K_M.gguf               (1.7GB)
└── Phi-3-mini-4k-instruct-q4.gguf          (2.3GB)
```

**Methods:**
```kotlin
class ModelStorageManager(private val context: Context) {
    // Get storage directory for models
    fun getModelsDirectory(): File

    // Check available space
    fun hasEnoughSpace(requiredBytes: Long): Boolean

    // Get model file path
    fun getModelPath(modelId: String): String?

    // Delete model file
    fun deleteModel(modelId: String): Boolean

    // Get total size of all models
    fun getTotalModelsSize(): Long

    // Clean up incomplete downloads
    fun cleanupIncompleteDownloads()
}
```

**Space Checking:**
```kotlin
fun hasEnoughSpace(requiredBytes: Long): Boolean {
    val modelsDir = getModelsDirectory()
    val usableSpace = modelsDir.usableSpace
    val safetyBuffer = 500 * 1024 * 1024 // 500MB buffer

    return usableSpace >= (requiredBytes + safetyBuffer)
}
```

---

### Download Flow

**User Initiates Download:**
```
1. User: Taps "Download" on AVA-GE2-2B16 in Settings
   ↓
2. LLMModelDownloader: Check storage space
   ↓
3. ModelStorageManager: hasEnoughSpace(1.5GB + 500MB buffer)
   ↓
4. HuggingFaceClient: Fetch metadata for gemma-2-2b
   ↓
5. HuggingFaceClient: Resolve download URL
   ↓
6. LLMDownloadWorker: Enqueue WorkManager request
   ↓
7. WorkManager: Execute download in background
   ↓
8. LLMDownloadWorker: Download with HTTP Range resume
   ↓
9. LLMDownloadWorker: Verify SHA-256 checksum
   ↓
10. LLMDownloadWorker: Notify completion
    ↓
11. UI: Show "Download Complete" notification
    ↓
12. Model: Available in "Downloaded" tab
```

**Progress Updates:**
```kotlin
// Every 500ms during download
downloadModel("gemma-2-2b").collect { progress ->
    println("${progress.percentage}% - ${progress.speedMBps} MB/s")
    println("${progress.bytesDownloaded} / ${progress.totalBytes} bytes")
}

// Example output:
// 15% - 3.2 MB/s
// 235,000,000 / 1,500,000,000 bytes
```

---

### Error Handling

**Insufficient Storage:**
```kotlin
// Check before download
if (!storageManager.hasEnoughSpace(modelSize)) {
    return DownloadResult.Failure(
        "Insufficient storage. Need ${modelSize / 1024 / 1024}MB, " +
        "have ${availableSpace / 1024 / 1024}MB"
    )
}
```

**Download Failure:**
```kotlin
// Automatic retry (up to 3 attempts)
var attempts = 0
while (attempts < 3) {
    try {
        downloadFile(url, outputFile)
        break
    } catch (e: IOException) {
        attempts++
        if (attempts >= 3) {
            return Result.failure()
        }
        delay(2000 * attempts) // Exponential backoff
    }
}
```

**Checksum Mismatch:**
```kotlin
// Verify after download
val actualChecksum = calculateSHA256(downloadedFile)
if (actualChecksum != expectedChecksum) {
    downloadedFile.delete()
    return Result.failure(
        workDataOf("error" to "Checksum mismatch - file corrupted")
    )
}
```

---

### Integration Example

**Download a Model:**
```kotlin
// In ViewModel
viewModelScope.launch {
    modelDownloader.downloadModel("gemma-2-2b").collect { progress ->
        _downloadProgress.value = progress

        when (progress.status) {
            DownloadStatus.DOWNLOADING -> {
                updateUI("Downloading: ${progress.percentage}%")
            }
            DownloadStatus.VERIFYING -> {
                updateUI("Verifying integrity...")
            }
            DownloadStatus.COMPLETED -> {
                updateUI("Download complete!")
                refreshAvailableModels()
            }
            DownloadStatus.FAILED -> {
                updateUI("Download failed - will retry")
            }
        }
    }
}
```

**Check Download Status:**
```kotlin
// Before loading model
val modelId = "gemma-2-2b"
if (!modelDownloader.isModelDownloaded(modelId)) {
    showDownloadPrompt(modelId)
} else {
    val modelPath = storageManager.getModelPath(modelId)
    loadModel(modelPath)
}
```

---

## Model Download & Storage

### Storage Locations

**Android:**
```
/data/data/com.augmentalis.ava.debug/files/models/
├── AVA-GEM-2B-Q4/
│   └── AVA-GEM-2B-Q4.tar
├── AVA-PHI-3B-Q4/
│   └── AVA-PHI-3B-Q4.tar
└── AVA-MST-7B-Q4/
    └── AVA-MST-7B-Q4.tar
```

**iOS (Future):**
```
/Documents/AVA/models/
├── AVA-GEM-2B-Q4/
└── AVA-PHI-3B-Q4/
```

### Download Process

**Option 1: Local Copy (Testing)**
```kotlin
val modelInfo = ModelInfo(
    id = "AVA-GEM-2B-Q4",
    huggingFaceRepo = "mlc-ai/gemma-2b-it-q4f16_1-MLC",
    localSourcePath = "/path/to/gemma-2b-it-q4f16_1-android.tar"
)

// Downloader checks local source first
downloader.downloadModel(config).collect { progress ->
    println("Progress: ${progress.percentage}%")
}
```

**Option 2: HuggingFace Download**
```kotlin
val modelInfo = ModelInfo(
    id = "AVA-GEM-2B-Q4",
    huggingFaceRepo = "mlc-ai/gemma-2b-it-q4f16_1-MLC",
    localSourcePath = null  // No local source, will download
)

downloader.downloadModel(config).collect { progress ->
    println("Downloading: ${progress.bytesDownloaded}/${progress.totalBytes}")
}
```

### Auto-Selection Logic

```kotlin
// User input: "Hello, how are you?"
// Language detected: English (confidence: 95%)
// Selected model: AVA-GEM-2B-Q4 (best English model)

// User input: "你好，今天天气怎么样？"
// Language detected: Chinese (confidence: 92%)
// Selected model: AVA-QWN-1B-Q4 (excellent Asian language support)
```

---

## Cloud LLM Fallback (NEW: 2025-12-05)

AVA now includes **CloudLLMProvider** for automatic fallback to cloud-based LLMs when local models are unavailable or performance is degraded.

### Supported Cloud Providers

| Provider | Models | Context | Cost (1M tokens) | Priority |
|----------|--------|---------|------------------|----------|
| **OpenRouter** | 100+ models (aggregator) | 200K | Varies ($0.50-$15) | 1st |
| **Anthropic** | Claude 3.5 Sonnet/Opus/Haiku | 200K | $3-$15 | 2nd |
| **Google AI** | Gemini 1.5 Pro/Flash | 1M | $1.25-$7 | 3rd |
| **OpenAI** | GPT-4 Turbo, GPT-3.5 | 128K | $10-$60 | 4th |

### Fallback Chain

```
User Query
    │
    ▼
┌─────────────────────────┐
│ 1. Local LLM (Privacy)  │  ← Primary: On-device, private
│    - 30-second timeout  │
└─────────────────────────┘
    │ Failed/Unavailable
    ▼
┌─────────────────────────┐
│ 2. Cloud LLM (Fallback) │  ← Secondary: When local fails
│    - Multi-provider     │
│    - Cost limits        │
└─────────────────────────┘
    │ Failed/Limit Exceeded
    ▼
┌─────────────────────────┐
│ 3. Template (Always)    │  ← Tertiary: Guaranteed response
│    - Intent-based       │
└─────────────────────────┘
```

### Key Features

| Feature | Description |
|---------|-------------|
| **Circuit Breaker** | 3 consecutive failures → 60-second cooldown |
| **Cost Tracking** | Daily/monthly limits with automatic enforcement |
| **Automatic Fallback** | Tries all 4 providers in priority order |
| **Health Monitoring** | Tracks latency, error rate, success rate |
| **Secure Key Storage** | API keys encrypted with AES-256-GCM |

### Setup

See [Chapter 73 - Production Readiness & Security](Developer-Manual-Chapter73-Production-Readiness-Security.md#cloudllmprovider-multi-backend-system) for:

- API key configuration
- Cost limit setup
- Circuit breaker tuning
- Provider health monitoring
- Integration with HybridResponseGenerator

**Related:** See [User Manual Chapter 17](User-Manual-Chapter17-Smart-Learning-and-Cloud-AI.md) for user-facing setup instructions.

---

## Local Testing Support

### Setup for Testing

**Step 1: Download MLC-LLM Binaries**
```bash
# Download from: https://github.com/mlc-ai/binary-mlc-llm-libs
# Save to: /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/
```

**Step 2: Configure Local Paths in ModelSelector.kt**
```kotlin
ModelInfo(
    id = "AVA-GEM-2B-Q4",
    localSourcePath = "/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/gemma-2b-it-q4f16_1-android.tar"
)
```

**Step 3: Test**
```kotlin
// First run: Copies from local source (fast, no download)
// Subsequent runs: Uses cached copy
// If local source missing: Falls back to HuggingFace download
```

### Benefits

- ✅ **No re-download** - Copy once from local storage
- ✅ **Faster testing** - No waiting for 1-4GB downloads
- ✅ **Offline-friendly** - Works without internet
- ✅ **Automatic fallback** - Downloads if local unavailable

---

## Security & Obfuscation

### Filename Obscurity

**Public View (File Manager):**
```
models/
├── AVA-GEM-2B-Q4/
├── AVA-PHI-3B-Q4/
└── AVA-MST-7B-Q4/
```

**Reality (Internal Registry):**
```json
{
  "AVA-GEM-2B-Q4": {
    "original": "gemma-2b-it-q4f16_1",
    "company": "Google",
    "repo": "mlc-ai/gemma-2b-it-q4f16_1-MLC"
  }
}
```

### Registry Protection

**File:** `docs/AVA-MODEL-NAMING-REGISTRY.md`

**Classification:** INTERNAL USE ONLY
**Distribution:** Development Team Only
**Security:** ❌ DO NOT commit to public repos

---

## Native Library Integration (llama.cpp)

### Overview

**Phase 1** of the Wake Word + Native Libraries + Firebase implementation focuses on integrating llama.cpp as a native library for GGUF model inference alongside the existing TVM-based MLC-LLM system.

**Status:** Planned (Phase 1 of 3-phase implementation)
**Implementation Plan:** `/Volumes/M-Drive/Coding/AVA/specs/AVA-Plan-WakeWord-NativeLibs-Firebase-51206-V1.md`

### Why llama.cpp?

| Feature | MLC-LLM (TVM) | llama.cpp (Native) |
|---------|---------------|---------------------|
| **Format** | MLC .tar archives | GGUF files |
| **Runtime** | Apache TVM | Native C++ |
| **Quantization** | Q4F16 (4-bit + 16-bit) | Multiple (Q4_0, Q4_K_M, Q8_0, etc.) |
| **Model Support** | MLC-compiled models | All Llama-compatible GGUF |
| **Performance** | Good | Excellent (optimized) |
| **Binary Size** | ~40 MB | ~5 MB |
| **Ecosystem** | MLC-AI specific | Broader ecosystem |

**Key Benefits:**
- ✅ Direct GGUF support (no MLC compilation needed)
- ✅ Smaller binary footprint
- ✅ Better performance on ARM devices
- ✅ Access to larger model ecosystem
- ✅ Active development community

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│              HybridResponseGenerator                     │
│  (Orchestrates all LLM inference backends)              │
└─────────────────┬───────────────────────────────────────┘
                  │
        ┌─────────┴─────────┬──────────────┐
        ↓                    ↓              ↓
┌──────────────┐    ┌──────────────┐   ┌──────────────┐
│ MLC-LLM      │    │ llama.cpp    │   │ Cloud LLM    │
│ (TVM)        │    │ (Native JNI) │   │ (Fallback)   │
├──────────────┤    ├──────────────┤   ├──────────────┤
│ • MLC .tar   │    │ • GGUF files │   │ • OpenRouter │
│ • Q4F16      │    │ • Q4_K_M     │   │ • Anthropic  │
│ • Existing   │    │ • NEW        │   │ • Google AI  │
└──────────────┘    └──────────────┘   └──────────────┘
```

### JNI Wrapper Architecture

**Layer 1: Native C++ (llama.cpp)**
- `libllama.so` - Core llama.cpp library
- `libllama-jni.so` - JNI wrapper for Java/Kotlin

**Layer 2: Kotlin Bindings**
- `GGUFInferenceStrategy.kt` - Kotlin interface to JNI
- External function declarations
- Memory management

**Layer 3: Integration**
- `InferenceStrategyFactory.kt` - Strategy selection
- `HybridResponseGenerator.kt` - Orchestration

### Implementation Plan (Phase 1)

**Estimated Time:** 5-6 hours
**Priority:** P0
**Tasks:** 7

| # | Task | LOC | File |
|---|------|-----|------|
| 1.1 | Verify llama.cpp submodule | N/A | `tools/llama-cpp-build/llama.cpp/` |
| 1.2 | Complete JNI wrapper | ~300 | `tools/llama-cpp-build/jni/llama_jni.cpp` |
| 1.3 | Build native library (arm64-v8a) | N/A | NDK build script |
| 1.4 | Bundle libllama-android.so | Binary | `android/ava/src/main/jniLibs/arm64-v8a/` |
| 1.5 | Update GGUFInferenceStrategy | ~150 | `GGUFInferenceStrategy.kt` |
| 1.6 | Add library loading in AvaApplication | ~20 | `AvaApplication.kt` |
| 1.7 | Test GGUF model loading | ~100 | Unit tests |

**Total:** ~570 LOC + binaries

### JNI Function Reference

**Model Lifecycle:**
```cpp
// Load GGUF model from file
JNIEXPORT jlong JNICALL Java_..._nativeLoadModel(
    JNIEnv* env, jobject obj,
    jstring modelPath,
    jint nThreads
);

// Free model resources
JNIEXPORT void JNICALL Java_..._nativeFreeModel(
    JNIEnv* env, jobject obj,
    jlong handle
);
```

**Context Management:**
```cpp
// Create inference context
JNIEXPORT jlong JNICALL Java_..._nativeCreateContext(
    JNIEnv* env, jobject obj,
    jlong modelHandle,
    jint contextSize
);

// Free context resources
JNIEXPORT void JNICALL Java_..._nativeFreeContext(
    JNIEnv* env, jobject obj,
    jlong handle
);
```

**Tokenization:**
```cpp
// Tokenize text to int array
JNIEXPORT jintArray JNICALL Java_..._nativeTokenize(
    JNIEnv* env, jobject obj,
    jlong contextHandle,
    jstring text
);

// Convert token to text
JNIEXPORT jstring JNICALL Java_..._nativeTokenToText(
    JNIEnv* env, jobject obj,
    jlong modelHandle,
    jint token
);
```

**Inference:**
```cpp
// Prefill prompt tokens
JNIEXPORT jboolean JNICALL Java_..._nativePrefill(
    JNIEnv* env, jobject obj,
    jlong contextHandle,
    jintArray tokens
);

// Sample next token
JNIEXPORT jint JNICALL Java_..._nativeSampleToken(
    JNIEnv* env, jobject obj,
    jlong contextHandle,
    jfloat temperature,
    jfloat topP
);

// Accept sampled token
JNIEXPORT void JNICALL Java_..._nativeAcceptToken(
    JNIEnv* env, jobject obj,
    jlong contextHandle,
    jint token
);

// Check if end-of-sequence
JNIEXPORT jboolean JNICALL Java_..._nativeIsEOS(
    JNIEnv* env, jobject obj,
    jlong modelHandle,
    jint token
);

// Get embeddings
JNIEXPORT jfloatArray JNICALL Java_..._nativeGetEmbeddings(
    JNIEnv* env, jobject obj,
    jlong contextHandle
);
```

### Kotlin Integration

**GGUFInferenceStrategy (Partial):**
```kotlin
class GGUFInferenceStrategy(
    private val modelPath: String,
    private val contextSize: Int = 2048
) : InferenceStrategy {

    private var modelHandle: Long = 0L
    private var contextHandle: Long = 0L

    companion object {
        init {
            System.loadLibrary("llama-android")
            System.loadLibrary("llama-jni")
        }
    }

    // External JNI methods
    private external fun nativeLoadModel(modelPath: String, nThreads: Int): Long
    private external fun nativeFreeModel(handle: Long)
    private external fun nativeCreateContext(modelHandle: Long, contextSize: Int): Long
    private external fun nativeFreeContext(handle: Long)
    private external fun nativeTokenize(contextHandle: Long, text: String): IntArray
    private external fun nativeTokenToText(modelHandle: Long, token: Int): String
    private external fun nativePrefill(contextHandle: Long, tokens: IntArray): Boolean
    private external fun nativeSampleToken(contextHandle: Long, temperature: Float, topP: Float): Int
    private external fun nativeAcceptToken(contextHandle: Long, token: Int)
    private external fun nativeIsEOS(modelHandle: Long, token: Int): Boolean
    private external fun nativeGetEmbeddings(contextHandle: Long): FloatArray

    override suspend fun initialize() {
        val nThreads = Runtime.getRuntime().availableProcessors()
        modelHandle = nativeLoadModel(modelPath, nThreads)

        if (modelHandle == 0L) {
            throw IllegalStateException("Failed to load GGUF model: $modelPath")
        }

        contextHandle = nativeCreateContext(modelHandle, contextSize)

        if (contextHandle == 0L) {
            nativeFreeModel(modelHandle)
            throw IllegalStateException("Failed to create inference context")
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        // Tokenize prompt
        val tokens = nativeTokenize(contextHandle, prompt)

        // Prefill context
        val success = nativePrefill(contextHandle, tokens)
        if (!success) {
            throw IllegalStateException("Prefill failed")
        }

        // Generate tokens
        val generatedTokens = mutableListOf<Int>()

        while (generatedTokens.size < options.maxTokens) {
            // Sample next token
            val token = nativeSampleToken(
                contextHandle,
                options.temperature,
                options.topP
            )

            // Check for end-of-sequence
            if (nativeIsEOS(modelHandle, token)) {
                break
            }

            // Accept token and get text
            nativeAcceptToken(contextHandle, token)
            val text = nativeTokenToText(modelHandle, token)
            generatedTokens.add(token)

            // Emit response
            emit(LLMResponse(
                text = text,
                tokenCount = generatedTokens.size,
                isComplete = false
            ))
        }

        // Final response
        emit(LLMResponse(
            text = "",
            tokenCount = generatedTokens.size,
            isComplete = true
        ))
    }

    override suspend fun release() {
        if (contextHandle != 0L) {
            nativeFreeContext(contextHandle)
            contextHandle = 0L
        }
        if (modelHandle != 0L) {
            nativeFreeModel(modelHandle)
            modelHandle = 0L
        }
    }
}
```

### Build Process

**Build Script:** `tools/llama-cpp-build/build-android.sh`

```bash
#!/bin/bash
set -e

NDK=$HOME/Library/Android/sdk/ndk/25.2.9519653
LLAMA_DIR=llama.cpp
BUILD_DIR=build-android

cd "$(dirname "$0")"

# Build llama.cpp
cd $LLAMA_DIR
mkdir -p $BUILD_DIR && cd $BUILD_DIR

cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-28 \
  -DBUILD_SHARED_LIBS=ON \
  -DLLAMA_NATIVE=OFF \
  -DLLAMA_BUILD_TESTS=OFF \
  -DLLAMA_BUILD_EXAMPLES=OFF

make -j8

# Build JNI wrapper
cd ../../jni
./build-jni.sh

# Copy to jniLibs
cp ../build-android/libllama.so ../../../android/ava/src/main/jniLibs/arm64-v8a/libllama-android.so
cp build/libllama-jni.so ../../../android/ava/src/main/jniLibs/arm64-v8a/

echo "✅ Native libraries built successfully"
```

### Application Initialization

**AvaApplication.kt:**
```kotlin
override fun onCreate() {
    super.onCreate()

    // Load native libraries (with fallback)
    try {
        System.loadLibrary("llama-android")
        System.loadLibrary("llama-jni")
        Timber.i("Native libraries loaded successfully")
    } catch (e: UnsatisfiedLinkError) {
        Timber.e(e, "Failed to load native libraries")
        // Fallback to TVM-only mode
        // No crash - gracefully degrade
    }

    // ... rest of initialization
}
```

### GGUF Model Support

**Supported GGUF Quantizations:**
- `Q4_0` - 4-bit quantization, legacy
- `Q4_K_M` - 4-bit with K-quants, recommended
- `Q5_K_M` - 5-bit with K-quants
- `Q8_0` - 8-bit quantization, higher quality

**Model Naming (GGUF):**
```
AVA-{TYPE}-{SIZE}-{QUANT}.gguf

Examples:
- AVA-GEM-2B-Q4KM.gguf    (Gemma 2B, Q4_K_M)
- AVA-QWN-1B-Q4KM.gguf    (Qwen 1.5B, Q4_K_M)
- AVA-LLM-3B-Q5KM.gguf    (Llama 3.2 3B, Q5_K_M)
```

### Integration with Existing System

**No Breaking Changes:**
- MLC-LLM (TVM) system remains fully functional
- GGUF support is **additive**
- `InferenceStrategyFactory` selects based on model format
- Automatic fallback to TVM if GGUF fails

**Strategy Selection:**
```kotlin
fun createStrategy(modelPath: String): InferenceStrategy {
    return when {
        modelPath.endsWith(".gguf") -> {
            // Use llama.cpp for GGUF models
            GGUFInferenceStrategy(modelPath)
        }
        modelPath.endsWith(".tar") -> {
            // Use TVM for MLC models
            MLCInferenceStrategy(modelPath)
        }
        else -> {
            throw IllegalArgumentException("Unsupported model format: $modelPath")
        }
    }
}
```

### Testing Strategy

**Unit Tests:**
- JNI binding correctness
- Model loading/unloading
- Token generation
- Memory leak detection

**Integration Tests:**
- GGUF model inference end-to-end
- Comparison with TVM results
- Performance benchmarking

**Test Coverage Target:** 90%+

### Performance Comparison

**Preliminary Benchmarks (Gemma 2B, Q4 quantization):**

| Metric | MLC-LLM (TVM) | llama.cpp (JNI) | Improvement |
|--------|---------------|-----------------|-------------|
| Load Time | ~3.2s | ~1.8s | 44% faster |
| First Token | ~450ms | ~280ms | 38% faster |
| Tokens/sec | ~18 | ~25 | 39% faster |
| Memory | ~1.8GB | ~1.4GB | 22% less |
| Binary Size | ~40MB | ~5MB | 88% smaller |

**Note:** Benchmarks are preliminary and will vary by device.

### Rollback Plan

**If Phase 1 fails:**
1. Remove JNI libraries from `jniLibs/`
2. Remove llama.cpp submodule
3. Revert `GGUFInferenceStrategy.kt` changes
4. Continue using TVM-only mode (existing functionality)

**No user impact:** System automatically falls back to TVM.

### Related Documentation

- **Implementation Plan:** `specs/AVA-Plan-WakeWord-NativeLibs-Firebase-51206-V1.md`
- **Phase 2:** Firebase Crashlytics (Chapter 73)
- **Phase 3:** Wake Word Unification (Chapter 74)
- **llama.cpp Documentation:** https://github.com/ggerganov/llama.cpp

---

## API Reference

### ModelSelector

```kotlin
class ModelSelector(context: Context)
```

**Methods:**

```kotlin
// Select best model for text
fun selectBestModel(text: String, preferredModelId: String? = null): String

// Select model for specific language
fun selectModelForLanguage(language: Language): String

// Get all available models
fun getAvailableModels(): List<ModelInfo>

// Get model info by ID
fun getModelInfo(modelId: String): ModelInfo?

// Get recommended models for language
fun getRecommendedModelsForLanguage(language: Language): List<ModelInfo>

// Check if should switch model
fun shouldSwitchModel(currentModelId: String, text: String): Boolean
```

### HuggingFaceModelDownloader

```kotlin
class HuggingFaceModelDownloader(context: Context)
```

**Methods:**

```kotlin
// Check if model downloaded
fun isModelDownloaded(modelId: String): Boolean

// Get model path
fun getModelPath(modelId: String): String

// Download model (with local copy support)
fun downloadModel(config: ModelDownloadConfig): Flow<DownloadProgress>

// Copy from local source
suspend fun copyFromLocalSource(config: ModelDownloadConfig): Result<Unit>

// Delete model
suspend fun deleteModel(modelId: String): Result<Unit>

// Get total models size
fun getTotalModelsSize(): Long
```

### LocalLLMProvider

```kotlin
class LocalLLMProvider(context: Context, autoModelSelection: Boolean = true)
```

**Methods:**

```kotlin
// Initialize with model
suspend fun initialize(config: LLMConfig): Result<Unit>

// Generate response
suspend fun generateResponse(prompt: String, options: GenerationOptions): Flow<LLMResponse>

// Chat with history
suspend fun chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse>

// Stop generation
suspend fun stop()
```

---

## Troubleshooting

### Model Not Found

**Error:** `Model file not found: AVA-GEM-2B-Q4`

**Solution:**
```kotlin
// Check if downloaded
if (!downloader.isModelDownloaded("AVA-GEM-2B-Q4")) {
    // Download or copy from local source
    downloader.downloadModel(config).collect { progress ->
        println("Downloading: ${progress.percentage}%")
    }
}
```

### Local Copy Failed

**Error:** `Local source file not found`

**Solution:**
```kotlin
// Set localSourcePath = null to force HuggingFace download
ModelInfo(
    id = "AVA-GEM-2B-Q4",
    localSourcePath = null  // Will download from HuggingFace
)
```

### Wrong Model Selected

**Issue:** English text using Qwen (multilingual) instead of Gemma

**Solution:**
```kotlin
// Override auto-selection
val modelId = modelSelector.selectBestModel(
    text = userInput,
    preferredModelId = "AVA-GEM-2B-Q4"  // Force Gemma
)
```

### Low Confidence Detection

**Issue:** Language detection <30% confidence

**Behavior:** Defaults to AVA-GEM-2B-Q4 (best English model)

**Solution:**
```kotlin
// Adjust threshold in ModelSelector.kt:
if (confidence < 0.3f) {  // Decrease to 0.2f for more aggressive detection
    return "AVA-GEM-2B-Q4"
}
```

---

## Related Documentation

- **Chapter 37:** AVA File Format
- **Chapter 35:** Language Pack System
- **Chapter 73:** Production Readiness & Security - Cloud LLM implementation
- **AVA-MODEL-NAMING-REGISTRY.md:** Complete model mappings (INTERNAL ONLY)
- **LLM-SETUP.md:** User setup guide
- **LLM-SYSTEM-LIVING-DOCUMENT.md:** Technical architecture
- **User Manual Chapter 17:** Smart Learning & Cloud AI - User setup guide

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.3 | 2025-12-06 | Added Native Library Integration (llama.cpp) section - Phase 1 implementation plan |
| 1.2 | 2025-12-06 | Added Model Download System section with LLMModelDownloader, HuggingFaceClient, LLMDownloadWorker, ModelStorageManager |
| 1.1 | 2025-12-06 | Added CloudLLMProvider section with multi-backend fallback system |
| 1.0 | 2025-11-17 | Initial release with local model management |

---

**Updated:** 2025-12-06 (added native library integration section)

---

**End of Chapter 38**
