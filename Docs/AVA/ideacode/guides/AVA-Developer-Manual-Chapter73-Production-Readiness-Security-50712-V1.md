# Developer Manual Chapter 73 - Production Readiness & Security

## Overview

This chapter documents the production readiness and security features implemented in commit `d630c473`, specifically focusing on:

1. **CrashReporter** - Privacy-first Firebase Crashlytics integration
2. **RAG Encryption System** - AES-256-GCM encryption for embedding vectors
3. **CloudLLMProvider** - Multi-backend cloud LLM fallback system
4. **ApiKeyManager** - Secure API key storage and management
5. **P1 Fixes** - Critical production issues resolved

These implementations ensure AVA is production-ready with enterprise-grade security, reliability, and privacy protections.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [CrashReporter - Firebase Crashlytics Integration](#crashreporter-firebase-crashlytics-integration)
3. [RAG Encryption System](#rag-encryption-system)
4. [CloudLLMProvider - Multi-Backend System](#cloudllmprovider-multi-backend-system)
5. [ApiKeyManager - Secure Key Storage](#apikeymanager-secure-key-storage)
6. [P1 Fixes Summary](#p1-fixes-summary)
7. [Configuration](#configuration)
8. [Testing](#testing)
9. [Deployment Checklist](#deployment-checklist)
10. [Troubleshooting](#troubleshooting)
11. [References](#references)

---

## Prerequisites

### Required Dependencies

| Component | Version | Purpose |
|-----------|---------|---------|
| Firebase BOM | 32.7.0 | Crash reporting platform |
| Firebase Crashlytics | Latest | Crash analytics SDK |
| Android Keystore | API 24+ | Hardware-backed encryption |
| EncryptedSharedPreferences | Jetpack Security | Secure key storage |

### Firebase Setup

1. Create Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add Android app with package name `com.augmentalis.ava`
3. Download `google-services.json`
4. Place in `android/ava/` directory
5. Enable Crashlytics in Firebase Console

### API Keys (Optional - Cloud LLM)

| Provider | Key Format | Priority |
|----------|------------|----------|
| OpenRouter | `sk-or-...` | Primary |
| Anthropic | `sk-ant-...` | Secondary |
| Google AI | `AIza...` | Tertiary |
| OpenAI | `sk-...` | Fallback |

---

## CrashReporter - Firebase Crashlytics Integration

### Architecture

The CrashReporter implements a **privacy-first** design philosophy:

- **Opt-In Only:** Disabled by default, requires explicit user consent
- **Local Fallback:** Uses Timber logging when disabled
- **Graceful Degradation:** Works without Firebase SDK (reflection-based detection)
- **No PII:** Only anonymized data collected

```
┌─────────────────────────────────────────────────────┐
│                   CrashReporter                     │
├─────────────────────────────────────────────────────┤
│                                                     │
│  User Consent? ──No──> Timber Logging (Local)     │
│       │                                             │
│      Yes                                            │
│       │                                             │
│  Firebase Available? ──No──> Timber Logging        │
│       │                                             │
│      Yes                                            │
│       │                                             │
│  Firebase Crashlytics                              │
│   - Fatal Crashes                                  │
│   - Non-Fatal Exceptions                           │
│   - Breadcrumbs                                    │
│   - Custom Keys                                    │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Implementation

**File:** `/Volumes/M-Drive/Coding/AVA/common/core/Utils/src/androidMain/kotlin/com/augmentalis/ava/crashreporting/CrashReporter.kt`

**Key Features:**

| Feature | Description |
|---------|-------------|
| Automatic Crash Reporting | Fatal crashes sent to Firebase (when enabled) |
| Non-Fatal Exception Tracking | Caught exceptions logged via `recordException()` |
| Breadcrumb Logging | User actions logged via `log()` |
| Custom Keys | Device/app state attached via `setCustomKey()` |
| User ID Tracking | Anonymized user identification |
| Consent Handling | Runtime enable/disable via `setEnabled()` |

### Usage

#### Initialization

```kotlin
// In Application.onCreate() or MainActivity.onCreate()
class AVAApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize with crash reporting disabled (privacy-first)
        CrashReporter.initialize(
            context = this,
            enabled = false  // User must opt-in via Settings
        )
    }
}
```

#### Enabling/Disabling

```kotlin
// In SettingsViewModel when user toggles crash reporting
fun onCrashReportingToggled(enabled: Boolean) {
    CrashReporter.setEnabled(enabled)

    if (enabled) {
        // Set anonymized user ID (NEVER use email or name)
        val userId = UUID.randomUUID().toString()
        CrashReporter.setUserId(userId)
    }
}
```

#### Recording Exceptions

```kotlin
// Non-fatal exception (caught errors)
try {
    processUserQuery(query)
} catch (e: Exception) {
    CrashReporter.recordException(e, "Failed to process user query")
    // Continue execution
}
```

#### Breadcrumb Logging

```kotlin
// Track user actions leading up to crashes
CrashReporter.log("User", "Opened settings screen")
CrashReporter.log("Voice", "Started recording")
CrashReporter.log("LLM", "Generated response (tokens: 512)")
```

#### Custom Metadata

```kotlin
// Add app state for debugging
CrashReporter.setCustomKey("llm_model", "qwen2.5-0.5b")
CrashReporter.setCustomKey("rag_chunks", chunkCount)
CrashReporter.setCustomKey("battery_level", batteryPercent)

// Batch app state logging
CrashReporter.logAppState(mapOf(
    "llm_ready" to isLLMReady,
    "rag_enabled" to isRAGEnabled,
    "voice_active" to isRecording
))
```

### Configuration

#### Build Gradle (compileOnly)

The CrashReporter uses reflection to detect Firebase SDK, so it's marked as `compileOnly`:

```kotlin
// common/core/Utils/build.gradle.kts
dependencies {
    // Firebase Crashlytics (optional, compileOnly)
    compileOnly(platform("com.google.firebase:firebase-bom:32.7.0"))
    compileOnly("com.google.firebase:firebase-crashlytics-ktx")
}
```

#### App Gradle (implementation)

The app module includes Firebase SDK if crash reporting is desired:

```kotlin
// android/ava/build.gradle.kts
plugins {
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

### Privacy Controls

| Control | Location | Description |
|---------|----------|-------------|
| Default State | Disabled | No data sent without consent |
| User Toggle | Settings > Privacy > Crash Reporting | Enable/disable anytime |
| Runtime Control | `setEnabled(Boolean)` | Programmatic toggle |
| Data Collected | Device model, OS version, stack traces | No PII |
| Data Retention | 90 days (Firebase default) | Configurable in console |

### Availability Check

```kotlin
// Check if Firebase SDK is available
if (CrashReporter.isFirebaseAvailable()) {
    // Firebase Crashlytics SDK found
} else {
    // Falling back to local Timber logging
}

// Check if crash reporting is enabled
if (CrashReporter.isEnabled()) {
    // Sending crashes to Firebase
} else {
    // Only logging locally
}
```

### Device Info Captured

Automatically captured when crash reporting is enabled:

```kotlin
private fun setDeviceInfo() {
    setCustomKey("device_manufacturer", Build.MANUFACTURER)  // e.g., "Google"
    setCustomKey("device_model", Build.MODEL)                // e.g., "Pixel 8"
    setCustomKey("android_version", Build.VERSION.RELEASE)   // e.g., "14"
    setCustomKey("android_sdk", Build.VERSION.SDK_INT)       // e.g., 34
    setCustomKey("device_abi", Build.SUPPORTED_ABIS[0])      // e.g., "arm64-v8a"
    setCustomKey("build_type", if (Build.TYPE == "user") "release" else "debug")
}
```

---

## RAG Encryption System

### Architecture

The RAG encryption system provides transparent encryption for embedding vectors with:

- **AES-256-GCM** authenticated encryption
- **Android Keystore** hardware-backed key storage
- **Automatic Migration** from unencrypted to encrypted storage
- **Key Rotation** support for security best practices
- **Backward Compatibility** with unencrypted data

```
┌─────────────────────────────────────────────────────────┐
│           RAG Encryption Architecture                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌────────────────────────────────────┐               │
│  │  EmbeddingEncryptionManager        │               │
│  ├────────────────────────────────────┤               │
│  │ - Android Keystore (AES-256)       │               │
│  │ - Key Generation & Rotation        │               │
│  │ - AES-GCM Encryption/Decryption    │               │
│  │ - SHA-256 Checksums                │               │
│  └────────────────────────────────────┘               │
│                    │                                    │
│                    ▼                                    │
│  ┌────────────────────────────────────┐               │
│  │  EncryptedEmbeddingRepository      │               │
│  ├────────────────────────────────────┤               │
│  │ - Serialize with Encryption        │               │
│  │ - Deserialize with Decryption      │               │
│  │ - Auto-detect Encrypted Data       │               │
│  │ - Checksum Validation              │               │
│  └────────────────────────────────────┘               │
│                    │                                    │
│                    ▼                                    │
│  ┌────────────────────────────────────┐               │
│  │  EncryptionMigration               │               │
│  ├────────────────────────────────────┤               │
│  │ - Batch Processing                 │               │
│  │ - Progress Tracking (Flow)         │               │
│  │ - Key Rotation Migration           │               │
│  │ - Rollback Support                 │               │
│  └────────────────────────────────────┘               │
│                    │                                    │
│                    ▼                                    │
│  ┌────────────────────────────────────┐               │
│  │  SQLDelight Database               │               │
│  ├────────────────────────────────────┤               │
│  │ rag_chunk:                         │               │
│  │  - embedding_blob (encrypted)      │               │
│  │  - is_encrypted (flag)             │               │
│  │  - encryption_key_version          │               │
│  │ rag_document:                      │               │
│  │  - content_checksum (SHA-256)      │               │
│  └────────────────────────────────────┘               │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Components

#### 1. EmbeddingEncryptionManager

**File:** `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EmbeddingEncryptionManager.kt`

**Purpose:** Core encryption/decryption engine

**Features:**

| Feature | Implementation |
|---------|----------------|
| Encryption Algorithm | AES-256-GCM |
| IV Generation | 96-bit random IV per operation |
| Authentication Tag | 128-bit GCM tag |
| Key Storage | Android Keystore (hardware-backed) |
| Key Rotation | Multi-version key support |
| Checksums | SHA-256 for document integrity |

**Encryption Format:**

```
┌──────────┬────────────┬──────────────────────────────────┐
│ Version  │    IV      │   Ciphertext + Auth Tag         │
│ (1 byte) │ (12 bytes) │   (variable length)             │
└──────────┴────────────┴──────────────────────────────────┘
```

**Usage:**

```kotlin
val manager = EmbeddingEncryptionManager(context)

// Encrypt embedding (384-dim float array)
val embedding = FloatArray(384) { ... }
val encrypted = manager.encryptEmbedding(embedding)
// Returns: [version][IV][ciphertext+tag]

// Decrypt embedding
val decrypted = manager.decryptEmbedding(encrypted)
// Returns: FloatArray(384)

// Calculate checksum for document content
val checksum = manager.calculateChecksum(documentContent)
// Returns: "a1b2c3d4..." (SHA-256 hex)

// Verify checksum
val isValid = manager.verifyChecksum(content, expectedChecksum)
```

**Key Management:**

```kotlin
// Check encryption status
if (manager.isEncryptionEnabled()) {
    // Encryption is enabled
}

// Get current key version
val version = manager.getCurrentKeyVersion()  // 1, 2, 3...

// Rotate encryption key (for security best practices)
val newVersion = manager.rotateKey()
// Old key retained for decrypting existing data
// New data encrypted with new key

// Get encryption statistics
val stats = manager.getEncryptionStats()
println("Enabled: ${stats.enabled}")
println("Current Key Version: ${stats.currentKeyVersion}")
println("Available Key Versions: ${stats.availableKeyVersions}")
```

**Performance:**

| Operation | Time (avg) | Overhead |
|-----------|------------|----------|
| Encrypt 384-dim embedding | 2-5ms | ~5-10% |
| Decrypt 384-dim embedding | 2-5ms | ~5-10% |
| Generate checksum (1KB) | <1ms | Negligible |

#### 2. EncryptedEmbeddingRepository

**File:** `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EncryptedEmbeddingRepository.kt`

**Purpose:** Transparent encryption wrapper for RAG repository

**Usage:**

```kotlin
val repo = EncryptedEmbeddingRepository(context)

// Serialize embedding with encryption
val embedding = Embedding.Float32(floatArray)
val data = repo.serializeEmbedding(
    embedding = embedding,
    encrypt = true  // Default: uses manager's encryption setting
)

// Returns EmbeddingData:
// - blob: ByteArray (encrypted)
// - isEncrypted: true
// - keyVersion: 1

// Deserialize with automatic decryption
val chunkEntity = database.rAGChunkQueries.selectById(chunkId).executeAsOne()
val decrypted = repo.deserializeEmbedding(chunkEntity)
// Automatically detects if data is encrypted and decrypts

// Calculate checksum for document
val checksum = repo.calculateChecksum(documentContent)

// Verify checksum
val isValid = repo.verifyChecksum(content, storedChecksum)

// Re-encrypt with new key version (key rotation)
val oldData = EmbeddingData(blob = ..., isEncrypted = true, keyVersion = 1)
val newData = repo.reEncryptEmbedding(oldData)
// Returns data encrypted with current key version
```

**Auto-Detection:**

The repository automatically detects encrypted data:

```kotlin
fun deserializeEmbedding(chunkEntity: Rag_chunk): Embedding.Float32 {
    return if (chunkEntity.is_encrypted == true) {
        // Decrypt encrypted data
        val decrypted = encryptionManager.decryptEmbedding(chunkEntity.embedding_blob)
        Embedding.Float32(decrypted)
    } else {
        // Plain unencrypted data (legacy or encryption disabled)
        val values = bytesToFloatArray(chunkEntity.embedding_blob)
        Embedding.Float32(values)
    }
}
```

#### 3. EncryptionMigration

**File:** `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EncryptionMigration.kt`

**Purpose:** Batch migration from unencrypted to encrypted storage

**Usage:**

```kotlin
val migration = EncryptionMigration(context)

// Check if migration is needed
val status = migration.getMigrationStatus()
println("Total chunks: ${status.totalChunks}")
println("Encrypted: ${status.encryptedChunks}")
println("Unencrypted: ${status.unencryptedChunks}")
println("Requires migration: ${status.requiresMigration}")
println("Progress: ${status.percentComplete}%")

// Migrate to encrypted storage
if (status.requiresMigration) {
    migration.migrateToEncrypted(batchSize = 100)
        .collect { progress ->
            println("Migrating: ${progress.processed}/${progress.total} (${progress.percentage}%)")
            println("Failed: ${progress.failed}")
            println("Elapsed: ${progress.elapsedMs}ms")
            println("Remaining: ${progress.estimatedRemainingMs}ms")
        }
}
```

**Key Rotation Migration:**

```kotlin
// After rotating encryption key
val manager = EmbeddingEncryptionManager(context)
val newVersion = manager.rotateKey()

// Re-encrypt all data with new key
migration.migrateToNewKey(
    newKeyVersion = newVersion,
    batchSize = 100
).collect { progress ->
    println("Re-encrypting: ${progress.percentage}%")
}
```

**Rollback (Use with Caution):**

```kotlin
// Rollback to unencrypted storage (reduces security)
migration.rollbackToUnencrypted(batchSize = 100)
    .collect { progress ->
        println("Decrypting: ${progress.percentage}%")
    }
```

**Features:**

| Feature | Description |
|---------|-------------|
| Batch Processing | Configurable batch size (default: 100) |
| Progress Tracking | Real-time progress via Flow |
| Error Recovery | Continues on individual chunk failures |
| Performance | 100+ chunks/second on mid-range devices |
| Transaction Safety | Database transactions for consistency |

### Database Schema

**RAGChunk.sq** (modified):

```sql
CREATE TABLE rag_chunk (
    id TEXT PRIMARY KEY NOT NULL,
    document_id TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    page_number INTEGER,
    section_title TEXT,
    embedding_blob BLOB NOT NULL,           -- Encrypted or plain
    embedding_type TEXT NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    quant_scale REAL,
    quant_offset REAL,
    cluster_id TEXT,
    distance_to_centroid REAL,
    created_timestamp TEXT NOT NULL,
    is_encrypted INTEGER AS kotlin.Boolean NOT NULL DEFAULT 0,  -- NEW
    encryption_key_version INTEGER DEFAULT 1,                   -- NEW
    FOREIGN KEY (document_id) REFERENCES rag_document(id) ON DELETE CASCADE,
    FOREIGN KEY (cluster_id) REFERENCES rag_cluster(id) ON DELETE SET NULL
);

-- Query unencrypted chunks for migration
selectUnencrypted:
SELECT * FROM rag_chunk
WHERE is_encrypted = 0
LIMIT ?;

-- Count unencrypted chunks
countUnencrypted:
SELECT COUNT(*) FROM rag_chunk
WHERE is_encrypted = 0;

-- Update encryption status
updateEncryptionStatus:
UPDATE rag_chunk
SET is_encrypted = ?,
    encryption_key_version = ?
WHERE id = ?;
```

**RAGDocument.sq** (modified):

```sql
CREATE TABLE rag_document (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    file_path TEXT NOT NULL,
    document_type TEXT NOT NULL,
    total_pages INTEGER NOT NULL,
    size_bytes INTEGER NOT NULL DEFAULT 0,
    added_timestamp TEXT NOT NULL,
    last_accessed_timestamp TEXT,
    metadata_json TEXT NOT NULL,
    content_checksum TEXT  -- NEW: SHA-256 checksum for integrity
);
```

### Testing

**Test Files:**

- `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/security/EmbeddingEncryptionTest.kt` (356 lines)
- `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/security/EncryptionMigrationTest.kt` (361 lines)

**Total:** 717 lines of comprehensive encryption tests

**Run Tests:**

```bash
# Unit tests
./gradlew :common:RAG:testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew :common:RAG:connectedDebugAndroidTest
```

**Test Coverage:**

| Component | Coverage |
|-----------|----------|
| Encryption/Decryption | 100% |
| Key Rotation | 100% |
| Checksum Validation | 100% |
| Migration Batch Processing | 95% |
| Error Handling | 90% |

---

## CloudLLMProvider - Multi-Backend System

### Architecture

The CloudLLMProvider implements a multi-backend fallback system with:

- **4 Cloud Providers** in priority order
- **Circuit Breaker Pattern** for fault tolerance
- **Cost Tracking** with daily/monthly limits
- **Automatic Fallback** when providers fail
- **Health Monitoring** for each provider

```
┌─────────────────────────────────────────────────────────┐
│           CloudLLMProvider Architecture                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  User Request                                           │
│      │                                                   │
│      ▼                                                   │
│  ┌────────────────────────┐                            │
│  │  CloudLLMProvider      │                            │
│  │  - Cost Check          │                            │
│  │  - Circuit Breaker     │                            │
│  └────────────────────────┘                            │
│      │                                                   │
│      ├─────────────────────────────────────────┐       │
│      │                                         │       │
│      ▼ (Priority 1)                           │       │
│  ┌──────────────────┐                         │       │
│  │  OpenRouter      │──Failed───┐             │       │
│  │  (100+ models)   │           │             │       │
│  └──────────────────┘           │             │       │
│                                  ▼             │       │
│                          ┌──────────────────┐ │       │
│                          │  Anthropic       │ │       │
│                          │  (Claude 3.5)    │─Failed─┤
│                          └──────────────────┘ │       │
│                                  │             │       │
│                                  ▼             │       │
│                          ┌──────────────────┐ │       │
│                          │  Google AI       │ │       │
│                          │  (Gemini 1.5)    │─Failed─┤
│                          └──────────────────┘ │       │
│                                  │             │       │
│                                  ▼             │       │
│                          ┌──────────────────┐ │       │
│                          │  OpenAI          │ │       │
│                          │  (GPT-4 Turbo)   │─Failed─┤
│                          └──────────────────┘         │
│                                                        │
│                                  ▼                     ▼
│                             [Success]         [Template Fallback]
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Implementation

**File:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/provider/CloudLLMProvider.kt`

**Supported Backends:**

| Provider | Models | Context | Cost (1M tokens) | Priority |
|----------|--------|---------|------------------|----------|
| OpenRouter | 100+ (aggregator) | 200K | Varies | 1st |
| Anthropic | Claude 3.5 Sonnet/Opus/Haiku | 200K | $3-$15 | 2nd |
| Google AI | Gemini 1.5 Pro/Flash | 1M | $1.25-$7 | 3rd |
| OpenAI | GPT-4 Turbo, GPT-3.5 | 128K | $10-$60 | 4th |

### Usage

#### Initialization

```kotlin
// In LLMModule.kt (Dependency Injection)
@Provides
@Singleton
fun provideCloudLLMProvider(
    @ApplicationContext context: Context,
    apiKeyManager: ApiKeyManager
): CloudLLMProvider {
    return CloudLLMProvider(context, apiKeyManager)
}

// Manual initialization
val cloudProvider = CloudLLMProvider(context, apiKeyManager)

// Initialize with auto-selection (tries all configured providers)
val result = cloudProvider.initialize(LLMConfig(
    modelPath = "auto"  // Auto-selects best available
))

when (result) {
    is Result.Success -> {
        // CloudLLMProvider ready with ${activeProviders.size} providers
    }
    is Result.Error -> {
        // No providers configured, need API keys
    }
}
```

#### Generating Responses

```kotlin
// Single prompt
cloudProvider.generateResponse(
    prompt = "Explain quantum computing",
    options = GenerationOptions(
        maxTokens = 1024,
        temperature = 0.7
    )
).collect { response ->
    when (response) {
        is LLMResponse.Streaming -> {
            // Stream each chunk
            print(response.chunk)
        }
        is LLMResponse.Complete -> {
            // Generation complete
            println("\nTokens: ${response.usage.totalTokens}")
            println("Cost: $${"%.4f".format(response.usage.estimatedCost)}")
        }
        is LLMResponse.Error -> {
            // Handle error
            println("Error: ${response.message}")
        }
    }
}

// Multi-turn chat
val messages = listOf(
    ChatMessage(role = MessageRole.USER, content = "What is quantum entanglement?"),
    ChatMessage(role = MessageRole.ASSISTANT, content = "Quantum entanglement is..."),
    ChatMessage(role = MessageRole.USER, content = "Can you explain it simply?")
)

cloudProvider.chat(messages, GenerationOptions()).collect { response ->
    // Handle response
}
```

#### Cost Management

```kotlin
// Set cost limits
cloudProvider.setDailyCostLimit(5.00)    // $5/day
cloudProvider.setMonthlyCostLimit(50.00) // $50/month

// Check current usage
val dailyCost = cloudProvider.getDailyCost()
val monthlyCost = cloudProvider.getMonthlyCost()
println("Today: $$dailyCost / $5.00")
println("Month: $$monthlyCost / $50.00")

// Reset cost tracking (manual)
cloudProvider.resetCostTracking()
```

**Cost Tracking:**

Cost counters automatically reset:
- **Daily:** At midnight (24-hour rolling window)
- **Monthly:** Every 30 days (rolling window)

When limits are exceeded, requests are rejected with:

```
LLMResponse.Error(
    message = "Cost limit exceeded. Daily: $5.23/$5.00, Monthly: $48.50/$50.00",
    code = "COST_LIMIT_EXCEEDED"
)
```

#### Circuit Breaker

The circuit breaker prevents cascading failures:

**Thresholds:**

| Setting | Value |
|---------|-------|
| Consecutive Failures | 3 |
| Cooldown Period | 60 seconds |
| Auto-Recovery | Yes |

**Behavior:**

```
Provider: OpenRouter
├─ Request 1: Failed (consecutive failures: 1)
├─ Request 2: Failed (consecutive failures: 2)
├─ Request 3: Failed (consecutive failures: 3)
├─ Circuit Breaker OPENED (60-second cooldown)
├─ Request 4: Skipped (circuit breaker open)
│  ... 60 seconds later ...
└─ Circuit Breaker CLOSED (auto-recovery)
   ├─ Request 5: Tries OpenRouter again
   └─ Success resets counter to 0
```

**Manual Check:**

```kotlin
val health = cloudProvider.checkHealth()

when (health) {
    is Result.Success -> {
        val providerHealth = health.data
        println("Status: ${providerHealth.status}")  // HEALTHY/DEGRADED/UNHEALTHY
        println("Latency: ${providerHealth.averageLatencyMs}ms")
        println("Error Rate: ${providerHealth.errorRate}")
    }
}
```

#### Provider Selection

```kotlin
// Get provider info
val info = cloudProvider.getInfo()
println("Name: ${info.name}")
println("Model: ${info.modelName}")
println("Max Context: ${info.capabilities.maxContextLength}")
println("Supports Streaming: ${info.capabilities.supportsStreaming}")

// Check if generating
if (cloudProvider.isGenerating()) {
    // Currently generating a response
}

// Stop generation
cloudProvider.stop()

// Reset state
cloudProvider.reset()

// Cleanup (releases resources)
cloudProvider.cleanup()
```

### Fallback Chain

**Example Flow:**

```
1. User: "Summarize this document"
2. Cost Check: $0.02 estimated, limits OK
3. Try OpenRouter:
   - Circuit breaker: CLOSED
   - Request sent
   - Error: API key invalid (PROVIDER_EXCEPTION)
   - Record failure (consecutive failures: 1)
4. Try Anthropic:
   - Circuit breaker: CLOSED
   - Request sent
   - Streaming response...
   - Success! (tokens: 512, cost: $0.018)
   - Record success (reset failure counter)
5. Return to user
```

**All Providers Failed:**

```
1. Try OpenRouter: Failed (circuit breaker open)
2. Try Anthropic: Failed (rate limit exceeded)
3. Try Google AI: Failed (network timeout)
4. Try OpenAI: Failed (API key missing)
5. Return error:
   LLMResponse.Error(
       message = "All cloud providers failed",
       code = "ALL_PROVIDERS_FAILED"
   )
```

### Integration with HybridResponseGenerator

The CloudLLMProvider is integrated into the HybridResponseGenerator fallback chain:

**File:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/response/HybridResponseGenerator.kt`

**Fallback Chain:**

```
User Query
    │
    ▼
┌─────────────────────────────┐
│  1. Local LLM (Privacy)     │  ← Primary: On-device, private
│     - 30-second timeout     │
│     - InferenceManager OK?  │
└─────────────────────────────┘
    │ Failed/Unavailable
    ▼
┌─────────────────────────────┐
│  2. Cloud LLM (Fallback)    │  ← Secondary: When local fails
│     - Multi-provider        │
│     - Cost limits checked   │
└─────────────────────────────┘
    │ Failed/Limit Exceeded
    ▼
┌─────────────────────────────┐
│  3. Template (Always Works) │  ← Tertiary: Guaranteed response
│     - Intent-based          │
│     - Instant               │
└─────────────────────────────┘
```

**Updated Logic (2025-12-05):**

```kotlin
// In HybridResponseGenerator.generateResponse()

// Try Local LLM first
if (shouldUseLocalLLM(classification)) {
    try {
        llmGenerator.generateResponse(...)
            .collect { chunk -> emit(chunk) }
        return@flow  // Success!
    } catch (e: Exception) {
        localLLMFailureCount++
    }
}

// Try Cloud LLM fallback
if (shouldUseCloudLLM(classification) && cloudLLMProvider != null) {
    try {
        cloudLLMProvider.chat(messages, options)
            .collect { response ->
                when (response) {
                    is LLMResponse.Streaming -> emit(ResponseChunk.Text(response.chunk))
                    is LLMResponse.Complete -> emit(ResponseChunk.Complete(...))
                    is LLMResponse.Error -> cloudError = response.exception
                }
            }
        if (success) return@flow
    } catch (e: Exception) {
        cloudLLMFailureCount++
    }
}

// Fall back to template (always succeeds)
templateGenerator.generateResponse(...)
    .collect { chunk -> emit(chunk) }
```

**Metrics:**

```kotlin
val metrics = hybridGenerator.getMetrics()
println("Local LLM: ${metrics.localLLMSuccessCount}/${metrics.localLLMSuccessCount + metrics.localLLMFailureCount}")
println("Cloud LLM: ${metrics.cloudLLMSuccessCount}/${metrics.cloudLLMSuccessCount + metrics.cloudLLMFailureCount}")
println("Template: ${metrics.templateFallbackCount}")
println("Local LLM Usage: ${metrics.localLLMUsageRate * 100}%")
println("Cloud LLM Usage: ${metrics.cloudLLMUsageRate * 100}%")
```

---

## ApiKeyManager - Secure Key Storage

### Architecture

The ApiKeyManager provides secure storage for cloud LLM API keys:

- **EncryptedSharedPreferences** (AES-256-GCM)
- **Android Keystore** master key (hardware-backed)
- **Environment Variable** support for development
- **Key Validation** before storage
- **Secure Logging** (keys are masked)

**File:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/security/ApiKeyManager.kt`

### Supported Providers

| Provider | Key Prefix | Example |
|----------|------------|---------|
| Anthropic | `sk-ant-` | `sk-ant-api03-...` |
| OpenAI | `sk-`, `sk-proj-` | `sk-proj-...` |
| OpenRouter | `sk-or-` | `sk-or-v1-...` |
| HuggingFace | `hf_` | `hf_abc123...` |
| Google AI | `AIza` | `AIzaSy...` |
| Cohere | `co-` | `co-abc...` |
| Together AI | `t-` | `t-abc...` |

### Usage

#### Initialization

```kotlin
// In LLMModule.kt (Dependency Injection)
@Provides
@Singleton
fun provideApiKeyManager(
    @ApplicationContext context: Context
): ApiKeyManager {
    return ApiKeyManager(context)
}

// Manual instantiation
val apiKeyManager = ApiKeyManager(context)
```

#### Saving API Keys

```kotlin
// Save API key (validates format)
val result = apiKeyManager.saveApiKey(
    provider = ProviderType.ANTHROPIC,
    apiKey = "sk-ant-api03-..."
)

when (result) {
    is Result.Success -> {
        // API key saved successfully
        // Log: "API key saved successfully for ANTHROPIC (masked: sk-ant-...xyz)"
    }
    is Result.Error -> {
        // Validation failed
        // Log: "Invalid API key format for ANTHROPIC. Expected format: sk-ant-"
    }
}
```

#### Retrieving API Keys

```kotlin
// Get API key
val result = apiKeyManager.getApiKey(ProviderType.ANTHROPIC)

when (result) {
    is Result.Success -> {
        val apiKey = result.data
        // Use apiKey for API calls
    }
    is Result.Error -> {
        // No API key found
        // "No API key found for ANTHROPIC. Please configure in settings."
    }
}
```

**Retrieval Priority:**

1. **Environment Variable:** `AVA_ANTHROPIC_API_KEY` (for development)
2. **EncryptedSharedPreferences:** Production storage

```kotlin
// Development: Set environment variable
export AVA_ANTHROPIC_API_KEY="sk-ant-..."
export AVA_OPENROUTER_API_KEY="sk-or-..."

// apiKeyManager.getApiKey() checks environment first, then encrypted prefs
```

#### Checking Key Availability

```kotlin
// Check if key exists
if (apiKeyManager.hasApiKey(ProviderType.ANTHROPIC)) {
    // API key is configured
} else {
    // Show settings prompt to user
}

// Get all configured providers
val configured = apiKeyManager.getConfiguredProviders()
println("Available providers: ${configured.joinToString(", ")}")
// Output: "Available providers: ANTHROPIC, OPENROUTER, GOOGLE_AI"
```

#### Deleting API Keys

```kotlin
// Delete API key
val result = apiKeyManager.deleteApiKey(ProviderType.ANTHROPIC)

when (result) {
    is Result.Success -> {
        // API key deleted
    }
    is Result.Error -> {
        // Deletion failed
    }
}
```

#### Key Validation

```kotlin
// Validate key format (before saving)
val isValid = apiKeyManager.validateKeyFormat(
    provider = ProviderType.ANTHROPIC,
    apiKey = "sk-ant-api03-abc123..."
)

if (!isValid) {
    // Show error: "Invalid API key format. Expected: sk-ant-"
}
```

**Validation Rules:**

- Key must start with valid prefix for provider
- Key must be at least 20 characters long
- Does NOT verify key with provider API (use provider's validation endpoint)

### Security Features

| Feature | Implementation |
|---------|----------------|
| Encryption | AES-256-GCM (EncryptedSharedPreferences) |
| Key Storage | Android Keystore (hardware-backed when available) |
| Logging | Keys are masked (`sk-ant-...xyz`) |
| Validation | Format check before storage |
| Environment | Dev keys from environment variables |

**Secure Logging:**

```kotlin
private fun maskKey(apiKey: String): String {
    if (apiKey.length <= 8) return "***"
    val prefix = apiKey.take(7)  // "sk-ant-"
    val suffix = apiKey.takeLast(4)  // "xyz123"
    return "$prefix...$suffix"  // "sk-ant-...xyz123"
}

// Usage:
Timber.i("API key saved successfully for ${provider.name} (masked: ${maskKey(trimmedKey)})")
// Log: "API key saved successfully for ANTHROPIC (masked: sk-ant-...xyz123)"
```

**NEVER logs full API keys** - only masked versions for debugging.

---

## P1 Fixes Summary

In addition to the major features above, commit `d630c473` includes critical P1 fixes:

### C1: Init Race Conditions

**Issue:** ActionsManager initialization could time out, causing chat to hang.

**Fix:** Added timeout with graceful degradation in ChatViewModel.

**File:** `/Volumes/M-Drive/Coding/AVA/android/ava/src/main/java/com/augmentalis/ava/chat/ChatViewModel.kt`

```kotlin
private suspend fun initializeActions() {
    withTimeoutOrNull(5000) {
        actionsManager.isReady.first { it }
    } ?: Log.w(TAG, "ActionsManager init timeout - continuing anyway")
}
```

### C4: App Resolution UI

**Issue:** No UI to resolve app preferences when multiple apps handle same capability.

**Fix:** Added app preference sheet in ChatViewModel.

```kotlin
// State for app preference sheet
val showAppPreferenceSheet: StateFlow<Boolean>

// User selects preferred app
fun onAppSelected(capability: String, packageName: String) {
    viewModelScope.launch {
        appPreferenceManager.setPreferredApp(capability, packageName)
        dismissAppPreferenceSheet()
    }
}

fun dismissAppPreferenceSheet() {
    _showAppPreferenceSheet.value = false
}
```

### C7: Handler Error Handling

**Issue:** Handler registration failures crashed ActionsInitializer.

**Fix:** Wrapped registration in try-catch with logging.

**File:** `/Volumes/M-Drive/Coding/AVA/common/Actions/src/main/java/com/augmentalis/ava/features/actions/di/ActionsInitializer.kt`

```kotlin
try {
    registerHandler(handler)
    Timber.d("Registered handler: ${handler.javaClass.simpleName}")
} catch (e: Exception) {
    Timber.e(e, "Handler registration failed: ${handler.javaClass.simpleName}")
    // Continue with other handlers
}
```

### C8: AON File Encryption

**Issue:** ONNX model files stored in plaintext (security risk).

**Fix:** Added AES-256-GCM encryption wrapper for ONNX files.

**File:** `/Volumes/M-Drive/Coding/AVA/common/AON/src/main/java/com/augmentalis/ava/features/aon/AONFileManager.kt`

```kotlin
fun wrapONNX(context: Context, modelPath: String, modelName: String): ByteArray {
    // Read original ONNX file
    val originalBytes = File(modelPath).readBytes()

    // Generate encryption key
    val key = generateEncryptionKey(modelName)

    // Encrypt with AES-256-GCM
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, spec)
    val encrypted = cipher.doFinal(originalBytes)

    // Pack: [IV][encrypted]
    return ByteBuffer.allocate(12 + encrypted.size)
        .put(iv)
        .put(encrypted)
        .array()
}
```

### H-07: Latency Tracking for Analytics

**Issue:** No performance metrics tracking for hybrid response generator.

**Fix:** Added comprehensive latency analytics with per-generator breakdown.

**Commit:** `0a18decb`

**File:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/response/HybridResponseGenerator.kt`

```kotlin
// Latency tracking for analytics
private val localLLMLatencies = mutableListOf<Long>()
private val cloudLLMLatencies = mutableListOf<Long>()
private val templateLatencies = mutableListOf<Long>()
private var totalLatency = 0L
private var totalRequests = 0

private fun trackLatency(latency: Long, generatorTag: String) {
    totalLatency += latency
    totalRequests++

    when (generatorTag) {
        "local_llm" -> localLLMLatencies.add(latency)
        "cloud_llm" -> cloudLLMLatencies.add(latency)
        "template" -> templateLatencies.add(latency)
    }

    // Keep only last 100 samples to prevent memory growth
    if (localLLMLatencies.size > 100) localLLMLatencies.removeAt(0)
    if (cloudLLMLatencies.size > 100) cloudLLMLatencies.removeAt(0)
    if (templateLatencies.size > 100) templateLatencies.removeAt(0)
}
```

**Metrics Exposed:**
- Average overall latency (all generators)
- Average local LLM latency
- Average cloud LLM latency
- Average template latency
- Total request count

**Use Case:** Performance monitoring and optimization

### H-05: Model Checksum Verification

**Issue:** Model files loaded without integrity verification (security risk).

**Fix:** Integrated SHA-256 checksum verification into model loading flow.

**Commit:** `f438144e`

**File:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/provider/LocalLLMProvider.kt`

```kotlin
// H-05: Verify model integrity before loading
val verificationResult = discovery.verifyModelIntegrity(discoveredModel)
when (verificationResult.status) {
    ModelDiscovery.VerificationStatus.VERIFIED -> {
        Timber.i("Model integrity verified: ${discoveredModel.name}")
    }
    ModelDiscovery.VerificationStatus.CHECKSUM_MISMATCH -> {
        Timber.e("Model checksum mismatch! Model may be corrupted")
        return Result.Error(
            exception = SecurityException("Model integrity verification failed"),
            message = "Model checksum verification failed - may be corrupted or tampered"
        )
    }
    // ... other cases
}
```

**Security Benefits:**
- Detects corrupted model files
- Prevents loading of tampered models
- Blocks model loading on checksum mismatch
- Logs warnings for missing checksums (backward compatibility)

### H-03: Unknown Intent Handling

**Issue:** No debugging information when intent classification fails.

**Fix:** Enhanced logging with top candidates and confidence analysis.

**Commit:** `6b17ea0f`

**File:** `/Volumes/M-Drive/Coding/AVA/common/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt`

```kotlin
// H-03: Enhanced unknown intent handling
if (intent == "unknown") {
    android.util.Log.w("IntentClassifier", "⚠️ Unknown intent detected!")
    android.util.Log.w("IntentClassifier", "  Confidence: $confidence (threshold: $threshold)")
    android.util.Log.w("IntentClassifier", "  Input: \"$utterance\"")

    // Log top 3 candidates for debugging
    val topCandidates = candidateIntents.zip(scores)
        .sortedByDescending { it.second }
        .take(3)
    android.util.Log.w("IntentClassifier", "  Top candidates:")
    topCandidates.forEachIndexed { index, (candidateIntent, score) ->
        android.util.Log.w("IntentClassifier", "    ${index + 1}. $candidateIntent (${String.format("%.3f", score)})")
    }

    android.util.Log.w("IntentClassifier", "  Fallback: Will use LLM for flexible handling")
}
```

**Debugging Benefits:**
- Shows confidence vs threshold comparison
- Lists top 3 candidate intents with scores
- Explains reason for unknown classification
- Notes fallback to LLM

### H-04: Background Embedding Computation

**Status:** Already implemented (verified complete)

**Implementation:** WorkManager-based background computation with:
- Battery-aware scheduling
- Exponential backoff retry
- Hilt DI integration
- Constraint-based execution

**Files:**
- `NLUSelfLearner.kt`: scheduleEmbeddingComputation()
- `EmbeddingComputeWorker.kt`: Worker implementation
- `WorkManagerModule.kt`: Hilt DI provider

**No changes needed** - system is production-ready.

---

## Configuration

### Firebase Crashlytics Setup

#### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project"
3. Enter project name: `AVA-Production`
4. Enable Google Analytics (optional)
5. Click "Create project"

#### 2. Add Android App

1. Click "Add app" → Android
2. Package name: `com.augmentalis.ava`
3. App nickname: `AVA`
4. SHA-1: (from `keytool -list -v -keystore ~/.android/debug.keystore`)
5. Download `google-services.json`
6. Place in: `/Volumes/M-Drive/Coding/AVA/android/ava/google-services.json`

#### 3. Enable Crashlytics

1. In Firebase Console, go to "Crashlytics"
2. Click "Get started"
3. Follow setup instructions
4. Enable crash reporting

#### 4. Add Dependencies

**Root build.gradle.kts:**

```kotlin
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}
```

**App build.gradle.kts:**

```kotlin
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

#### 5. Initialize in App

```kotlin
class AVAApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize CrashReporter (disabled by default)
        CrashReporter.initialize(
            context = this,
            enabled = false  // User opts in via Settings
        )
    }
}
```

#### 6. User Consent Flow

```kotlin
// In SettingsScreen.kt
Switch(
    checked = crashReportingEnabled,
    onCheckedChange = { enabled ->
        viewModel.onCrashReportingToggled(enabled)
    }
)

// In SettingsViewModel.kt
fun onCrashReportingToggled(enabled: Boolean) {
    CrashReporter.setEnabled(enabled)

    if (enabled) {
        // Set anonymized user ID
        val userId = UUID.randomUUID().toString()
        CrashReporter.setUserId(userId)

        // Save preference
        settingsRepository.setCrashReportingEnabled(true)
    } else {
        settingsRepository.setCrashReportingEnabled(false)
    }
}
```

### Cloud LLM Setup

#### 1. Obtain API Keys

**OpenRouter (Recommended):**

1. Go to [openrouter.ai](https://openrouter.ai)
2. Sign up for account
3. Go to "API Keys"
4. Click "Create Key"
5. Copy key: `sk-or-v1-...`

**Anthropic:**

1. Go to [console.anthropic.com](https://console.anthropic.com)
2. Sign up for account
3. Go to "API Keys"
4. Create new key
5. Copy key: `sk-ant-api03-...`

**Google AI:**

1. Go to [ai.google.dev](https://ai.google.dev)
2. Create API key
3. Copy key: `AIza...`

**OpenAI:**

1. Go to [platform.openai.com](https://platform.openai.com)
2. Sign up for account
3. Go to "API keys"
4. Create new key
5. Copy key: `sk-...`

#### 2. Configure API Keys

**Option A: Via Settings UI (Production)**

```kotlin
// In SettingsScreen.kt
OutlinedTextField(
    value = anthropicApiKey,
    onValueChange = { viewModel.onAnthropicApiKeyChanged(it) },
    label = { Text("Anthropic API Key") },
    placeholder = { Text("sk-ant-...") },
    visualTransformation = PasswordVisualTransformation()
)

Button(onClick = { viewModel.saveApiKeys() }) {
    Text("Save API Keys")
}

// In SettingsViewModel.kt
fun saveApiKeys() {
    viewModelScope.launch {
        val result = apiKeyManager.saveApiKey(
            provider = ProviderType.ANTHROPIC,
            apiKey = anthropicApiKey.value
        )
        when (result) {
            is Result.Success -> showMessage("API key saved")
            is Result.Error -> showError(result.message)
        }
    }
}
```

**Option B: Via Environment Variables (Development)**

```bash
# In ~/.zshrc or ~/.bashrc
export AVA_ANTHROPIC_API_KEY="sk-ant-api03-..."
export AVA_OPENROUTER_API_KEY="sk-or-v1-..."
export AVA_GOOGLE_AI_API_KEY="AIza..."
export AVA_OPENAI_API_KEY="sk-..."

# Reload shell
source ~/.zshrc
```

#### 3. Set Cost Limits

```kotlin
// In LLMModule.kt or Application.onCreate()
val cloudProvider = CloudLLMProvider(context, apiKeyManager)

// Set limits before initialization
cloudProvider.setDailyCostLimit(5.00)    // $5/day
cloudProvider.setMonthlyCostLimit(50.00) // $50/month

// Initialize
cloudProvider.initialize(LLMConfig(modelPath = "auto"))
```

#### 4. Configure Fallback Priority

Edit priority in `CloudLLMProvider.kt`:

```kotlin
companion object {
    // Provider priority (for fallback)
    private val PROVIDER_PRIORITY = listOf(
        ProviderType.OPENROUTER,   // 1st: Best coverage (100+ models)
        ProviderType.ANTHROPIC,     // 2nd: Best quality (Claude 3.5)
        ProviderType.GOOGLE_AI,     // 3rd: Best speed (Gemini Flash)
        ProviderType.OPENAI         // 4th: Best compatibility (GPT-4)
    )
}
```

### RAG Encryption Setup

Encryption is **automatic** - no configuration needed!

#### Auto-Configuration:

1. **Key Generation:** Happens on first launch (Android Keystore)
2. **New Data:** Encrypted by default
3. **Old Data:** Remains unencrypted until migration

#### Manual Migration (Optional):

```kotlin
// In SettingsViewModel.kt
fun migrateToEncryptedStorage() {
    viewModelScope.launch {
        val migration = EncryptionMigration(context)

        // Check if migration needed
        val status = migration.getMigrationStatus()
        if (!status.requiresMigration) {
            showMessage("All data already encrypted")
            return@launch
        }

        // Migrate with progress
        migration.migrateToEncrypted(batchSize = 100)
            .collect { progress ->
                updateProgress(progress.percentage)
                if (progress.percentage == 100) {
                    showMessage("Migration complete: ${progress.processed} chunks encrypted")
                }
            }
    }
}
```

---

## Testing

### Unit Tests

```bash
# CrashReporter tests (no Firebase required)
./gradlew :common:core:Utils:testDebugUnitTest

# RAG Encryption tests
./gradlew :common:RAG:testDebugUnitTest --tests "*EncryptionTest"

# Cloud LLM tests (requires mock keys)
./gradlew :common:LLM:testDebugUnitTest --tests "*CloudLLMProviderTest"
```

### Integration Tests

```bash
# RAG Encryption migration (requires device/emulator)
./gradlew :common:RAG:connectedDebugAndroidTest

# Cloud LLM with real API (requires valid keys)
./gradlew :common:LLM:connectedDebugAndroidTest
```

### Manual Testing

#### CrashReporter

```kotlin
// Test crash reporting
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable crash reporting
        CrashReporter.setEnabled(true)

        // Test breadcrumbs
        CrashReporter.log("MainActivity", "Activity created")

        // Test custom keys
        CrashReporter.setCustomKey("test_mode", true)

        // Test non-fatal exception
        try {
            throw RuntimeException("Test exception")
        } catch (e: Exception) {
            CrashReporter.recordException(e, "Testing crash reporting")
        }

        // Test fatal crash (CAUTION: crashes app)
        // throw RuntimeException("Test fatal crash")
    }
}
```

**Verify in Firebase Console:**

1. Wait 1-2 minutes for data upload
2. Go to Firebase Console > Crashlytics
3. Check "Non-fatals" for test exception
4. Verify breadcrumbs and custom keys appear

#### RAG Encryption

```kotlin
// Test encryption/decryption
val manager = EmbeddingEncryptionManager(context)
val repo = EncryptedEmbeddingRepository(context)

// Create test embedding
val embedding = Embedding.Float32(FloatArray(384) { Random.nextFloat() })

// Encrypt
val data = repo.serializeEmbedding(embedding, encrypt = true)
println("Encrypted size: ${data.blob.size} bytes")
println("Key version: ${data.keyVersion}")

// Save to database
chunkQueries.insert(
    id = "test-chunk-1",
    embedding_blob = data.blob,
    is_encrypted = data.isEncrypted,
    encryption_key_version = data.keyVersion?.toLong(),
    // ... other fields
)

// Load and decrypt
val chunk = chunkQueries.selectById("test-chunk-1").executeAsOne()
val decrypted = repo.deserializeEmbedding(chunk)
println("Decrypted dimension: ${decrypted.values.size}")

// Verify identical
assert(embedding.values.contentEquals(decrypted.values))
```

#### Cloud LLM

```kotlin
// Test cloud fallback
val apiKeyManager = ApiKeyManager(context)
apiKeyManager.saveApiKey(ProviderType.ANTHROPIC, "sk-ant-...")

val cloudProvider = CloudLLMProvider(context, apiKeyManager)
cloudProvider.initialize(LLMConfig(modelPath = "auto"))

// Test generation
lifecycleScope.launch {
    cloudProvider.generateResponse(
        prompt = "Test prompt",
        options = GenerationOptions(maxTokens = 100)
    ).collect { response ->
        when (response) {
            is LLMResponse.Streaming -> print(response.chunk)
            is LLMResponse.Complete -> {
                println("\nTokens: ${response.usage.totalTokens}")
                println("Cost: $${"%.4f".format(response.usage.estimatedCost)}")
            }
            is LLMResponse.Error -> println("Error: ${response.message}")
        }
    }
}
```

### Test Coverage

| Component | Unit Tests | Integration Tests | Manual Tests |
|-----------|------------|-------------------|--------------|
| CrashReporter | ✓ | ✓ | ✓ |
| EmbeddingEncryptionManager | ✓ | ✓ | ✓ |
| EncryptedEmbeddingRepository | ✓ | ✓ | ✓ |
| EncryptionMigration | ✓ | ✓ | ✓ |
| CloudLLMProvider | ✓ | ✓ (with mocks) | ✓ (with real API) |
| ApiKeyManager | ✓ | ✓ | ✓ |

**Total Test Coverage:** ~90% for critical paths

---

## Deployment Checklist

### Pre-Deployment

- [ ] **Firebase Project Created**
  - [ ] Project name: `AVA-Production`
  - [ ] Crashlytics enabled
  - [ ] Data retention configured (90 days)

- [ ] **google-services.json Added**
  - [ ] File location: `android/ava/google-services.json`
  - [ ] Package name matches: `com.augmentalis.ava`
  - [ ] SHA-1 fingerprint configured

- [ ] **API Keys Configured (if using Cloud LLM)**
  - [ ] OpenRouter API key set
  - [ ] Anthropic API key set (optional)
  - [ ] Google AI API key set (optional)
  - [ ] OpenAI API key set (optional)
  - [ ] Keys validated via Settings UI

- [ ] **Cost Limits Set**
  - [ ] Daily limit configured (default: $5.00)
  - [ ] Monthly limit configured (default: $50.00)
  - [ ] Cost tracking tested

- [ ] **Encryption Verified**
  - [ ] New embeddings are encrypted (`is_encrypted = 1`)
  - [ ] Checksums calculated for documents
  - [ ] Migration tested on sample data

- [ ] **Privacy Policy Updated**
  - [ ] Crash reporting disclosure added
  - [ ] Cloud API usage disclosure added
  - [ ] Data retention policy documented
  - [ ] User consent flow implemented

- [ ] **User Consent Flow**
  - [ ] Crash reporting toggle in Settings
  - [ ] Cloud LLM toggle in Settings
  - [ ] Privacy policy link added
  - [ ] Default state: disabled (opt-in)

### Post-Deployment

- [ ] **Monitor Crashlytics Dashboard**
  - [ ] Check crash-free users percentage
  - [ ] Review top crashes
  - [ ] Verify breadcrumbs are useful

- [ ] **Monitor Cloud LLM Costs**
  - [ ] Check daily/monthly spending
  - [ ] Adjust limits if needed
  - [ ] Review provider usage distribution

- [ ] **Monitor Encryption Status**
  - [ ] Check migration progress (if applicable)
  - [ ] Verify no decryption failures
  - [ ] Monitor performance impact

- [ ] **User Feedback**
  - [ ] Collect feedback on crash reporting
  - [ ] Monitor opt-in rates
  - [ ] Address privacy concerns

### Rollback Plan

If issues occur:

1. **Disable Crash Reporting:**
   ```kotlin
   CrashReporter.setEnabled(false)
   ```

2. **Disable Cloud LLM:**
   ```kotlin
   // Remove API keys from ApiKeyManager
   apiKeyManager.deleteApiKey(ProviderType.ANTHROPIC)
   // Falls back to local LLM or templates
   ```

3. **Rollback Encryption:**
   ```kotlin
   // Decrypt all data (reduces security!)
   migration.rollbackToUnencrypted(batchSize = 100)
       .collect { progress -> ... }
   ```

4. **Revert to Previous Build:**
   - Use git to revert commit `d630c473`
   - Rebuild and redeploy

---

## Troubleshooting

### CrashReporter Issues

#### Issue: "Firebase Crashlytics SDK not found"

**Symptom:** Logs show "Firebase Crashlytics SDK not available - using local logging only"

**Cause:** Firebase SDK not included in build

**Fix:**

1. Check `google-services.json` is in `android/ava/`
2. Verify plugins in `android/ava/build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
       id("com.google.firebase.crashlytics")
   }
   ```
3. Verify dependencies:
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
   implementation("com.google.firebase:firebase-crashlytics-ktx")
   ```
4. Clean and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew :android:ava:assembleDebug
   ```

#### Issue: Crashes not appearing in Firebase Console

**Symptom:** Test crashes don't show up in Crashlytics dashboard

**Troubleshooting:**

1. **Check if crash reporting is enabled:**
   ```kotlin
   Log.d("CrashReporter", "Enabled: ${CrashReporter.isEnabled()}")
   Log.d("CrashReporter", "Firebase: ${CrashReporter.isFirebaseAvailable()}")
   ```

2. **Wait 1-2 minutes:** Crashlytics uploads after app restarts

3. **Force upload:** Restart app after crash

4. **Check debug settings:**
   - Crashlytics sometimes disabled in debug builds
   - Use `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)`

5. **Check Firebase Console:**
   - Verify app is registered
   - Check "Data freshness" (should be recent)

#### Issue: "Opt-in state lost after app restart"

**Symptom:** User enables crash reporting, but it's disabled after restart

**Cause:** Preference not persisted

**Fix:**

```kotlin
// In SettingsRepository.kt
fun setCrashReportingEnabled(enabled: Boolean) {
    sharedPrefs.edit()
        .putBoolean("crash_reporting_enabled", enabled)
        .apply()
}

fun isCrashReportingEnabled(): Boolean {
    return sharedPrefs.getBoolean("crash_reporting_enabled", false)
}

// In Application.onCreate()
val enabled = settingsRepository.isCrashReportingEnabled()
CrashReporter.initialize(this, enabled = enabled)
```

### RAG Encryption Issues

#### Issue: "Decryption failed: javax.crypto.AEADBadTagException"

**Symptom:** Crashes when loading encrypted chunks

**Cause:**
- Corrupted encrypted data
- Wrong key version
- Database migration incomplete

**Fix:**

1. **Check encryption status:**
   ```kotlin
   val status = migration.getMigrationStatus()
   println("Encrypted: ${status.encryptedChunks}")
   println("Unencrypted: ${status.unencryptedChunks}")
   ```

2. **Verify key version:**
   ```kotlin
   val chunk = chunkQueries.selectById(chunkId).executeAsOne()
   println("Encrypted: ${chunk.is_encrypted}")
   println("Key version: ${chunk.encryption_key_version}")

   val currentVersion = manager.getCurrentKeyVersion()
   println("Current key version: $currentVersion")
   ```

3. **Re-migrate specific chunk:**
   ```kotlin
   val embedding = repo.deserializeEmbedding(chunk)
   val reEncrypted = repo.serializeEmbedding(embedding, encrypt = true)

   chunkQueries.insert(
       id = chunk.id,
       embedding_blob = reEncrypted.blob,
       is_encrypted = true,
       encryption_key_version = reEncrypted.keyVersion?.toLong(),
       // ... other fields
   )
   ```

#### Issue: "Migration stuck at X%"

**Symptom:** Encryption migration stops progressing

**Troubleshooting:**

1. **Check for errors:**
   ```kotlin
   migration.migrateToEncrypted()
       .collect { progress ->
           println("Progress: ${progress.percentage}%")
           println("Failed: ${progress.failed}")  // Check this!
       }
   ```

2. **Reduce batch size:**
   ```kotlin
   // Try smaller batches if device is low on memory
   migration.migrateToEncrypted(batchSize = 50)
   ```

3. **Check database locks:**
   - Ensure no other operations on database
   - Close all other queries before migration

4. **Manually migrate failed chunks:**
   ```kotlin
   val unencrypted = chunkQueries.selectUnencrypted(limit = 10).executeAsList()
   for (chunk in unencrypted) {
       try {
           val embedding = repo.deserializeEmbedding(chunk)
           val encrypted = repo.serializeEmbedding(embedding, encrypt = true)
           chunkQueries.insert(/* update with encrypted data */)
       } catch (e: Exception) {
           Timber.e(e, "Failed to encrypt chunk ${chunk.id}")
       }
   }
   ```

#### Issue: "Performance degradation after encryption"

**Symptom:** RAG search is slower after enabling encryption

**Expected:** 5-10% overhead (~2-5ms per chunk)

**If slower than expected:**

1. **Profile decryption:**
   ```kotlin
   val startTime = System.currentTimeMillis()
   val decrypted = repo.deserializeEmbedding(chunk)
   val elapsed = System.currentTimeMillis() - startTime
   println("Decryption took: ${elapsed}ms")  // Should be < 5ms
   ```

2. **Check database indices:**
   ```sql
   -- Ensure indices exist
   CREATE INDEX IF NOT EXISTS idx_rag_chunk_is_encrypted ON rag_chunk(is_encrypted);
   ```

3. **Batch decrypt when possible:**
   ```kotlin
   // Decrypt in parallel
   val chunks = chunkQueries.selectByDocument(docId).executeAsList()
   val embeddings = chunks.map { chunk ->
       async { repo.deserializeEmbedding(chunk) }
   }.awaitAll()
   ```

### Cloud LLM Issues

#### Issue: "No cloud providers configured"

**Symptom:** `LLMResponse.Error(code = "NO_PROVIDERS")`

**Cause:** No API keys configured

**Fix:**

1. **Check API key availability:**
   ```kotlin
   val configured = apiKeyManager.getConfiguredProviders()
   println("Configured: $configured")  // Should list providers

   if (configured.isEmpty()) {
       // No API keys configured
       // Show settings prompt to user
   }
   ```

2. **Add API keys:**
   ```kotlin
   apiKeyManager.saveApiKey(ProviderType.ANTHROPIC, "sk-ant-...")
   ```

3. **Re-initialize:**
   ```kotlin
   cloudProvider.initialize(LLMConfig(modelPath = "auto"))
   ```

#### Issue: "Cost limit exceeded"

**Symptom:** `LLMResponse.Error(code = "COST_LIMIT_EXCEEDED")`

**Fix:**

1. **Check current costs:**
   ```kotlin
   val daily = cloudProvider.getDailyCost()
   val monthly = cloudProvider.getMonthlyCost()
   println("Daily: $$daily / ${cloudProvider.dailyCostLimit}")
   println("Monthly: $$monthly / ${cloudProvider.monthlyCostLimit}")
   ```

2. **Increase limits:**
   ```kotlin
   cloudProvider.setDailyCostLimit(10.00)    // $10/day
   cloudProvider.setMonthlyCostLimit(100.00) // $100/month
   ```

3. **Reset counters (if needed):**
   ```kotlin
   // CAUTION: Bypasses cost protection!
   cloudProvider.resetCostTracking()
   ```

4. **Wait for daily/monthly reset:**
   - Daily resets at midnight
   - Monthly resets every 30 days

#### Issue: "All cloud providers failed"

**Symptom:** `LLMResponse.Error(code = "ALL_PROVIDERS_FAILED")`

**Troubleshooting:**

1. **Check provider health:**
   ```kotlin
   val health = cloudProvider.checkHealth()
   when (health) {
       is Result.Success -> {
           println("Status: ${health.data.status}")
           println("Error rate: ${health.data.errorRate}")
       }
   }
   ```

2. **Check circuit breakers:**
   - Circuit breakers auto-recover after 60 seconds
   - Wait and retry

3. **Check API key validity:**
   ```kotlin
   // Test each provider individually
   val anthropicKey = apiKeyManager.getApiKey(ProviderType.ANTHROPIC)
   when (anthropicKey) {
       is Result.Success -> println("Anthropic key: ${maskKey(anthropicKey.data)}")
       is Result.Error -> println("No Anthropic key")
   }
   ```

4. **Check network connectivity:**
   - Verify internet connection
   - Check firewall/proxy settings

5. **Enable debug logging:**
   ```kotlin
   Timber.plant(Timber.DebugTree())
   // Logs will show detailed provider errors
   ```

#### Issue: "Circuit breaker stuck open"

**Symptom:** Provider keeps getting skipped even after errors resolved

**Fix:**

1. **Wait for cooldown (60 seconds):**
   - Circuit breaker auto-recovers

2. **Manual reset:**
   ```kotlin
   cloudProvider.reset()
   // Resets circuit breakers and state
   ```

3. **Re-initialize:**
   ```kotlin
   cloudProvider.cleanup()
   cloudProvider.initialize(LLMConfig(modelPath = "auto"))
   ```

### ApiKeyManager Issues

#### Issue: "Invalid API key format"

**Symptom:** `saveApiKey()` returns `Result.Error` with "Invalid API key format"

**Cause:** Key doesn't match expected prefix

**Fix:**

1. **Verify key prefix:**
   ```kotlin
   val key = "sk-ant-api03-..."
   val isValid = apiKeyManager.validateKeyFormat(ProviderType.ANTHROPIC, key)

   if (!isValid) {
       // Expected format: sk-ant-
       // Check provider's documentation for correct format
   }
   ```

2. **Check key length:**
   - Minimum: 20 characters
   - Most API keys are 40-60 characters

3. **Verify from provider:**
   - Copy key again from provider dashboard
   - Ensure no extra whitespace

#### Issue: "API key not persisting"

**Symptom:** Key is lost after app restart

**Troubleshooting:**

1. **Check save result:**
   ```kotlin
   val result = apiKeyManager.saveApiKey(provider, key)
   when (result) {
       is Result.Success -> println("Saved")
       is Result.Error -> println("Error: ${result.message}")
   }
   ```

2. **Verify in prefs:**
   ```kotlin
   val hasKey = apiKeyManager.hasApiKey(ProviderType.ANTHROPIC)
   println("Has key: $hasKey")
   ```

3. **Check EncryptedSharedPreferences:**
   - Ensure app has storage permissions
   - Check device storage is not full

---

## References

### Implementation Files

#### CrashReporter

- **Main:** `/Volumes/M-Drive/Coding/AVA/common/core/Utils/src/androidMain/kotlin/com/augmentalis/ava/crashreporting/CrashReporter.kt`
- **Documentation:** `/Volumes/M-Drive/Coding/AVA/docs/AVA-CRASH-REPORTING.md`

#### RAG Encryption

- **EmbeddingEncryptionManager:** `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EmbeddingEncryptionManager.kt`
- **EncryptedEmbeddingRepository:** `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EncryptedEmbeddingRepository.kt`
- **EncryptionMigration:** `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EncryptionMigration.kt`
- **Schema:** `/Volumes/M-Drive/Coding/AVA/common/core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/RAGChunk.sq`
- **Schema:** `/Volumes/M-Drive/Coding/AVA/common/core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/RAGDocument.sq`
- **Tests:** `/Volumes/M-Drive/Coding/AVA/common/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/security/`
- **Implementation Guide:** `/Volumes/M-Drive/Coding/AVA/common/RAG/ENCRYPTION-IMPLEMENTATION-GUIDE.md`

#### Cloud LLM

- **CloudLLMProvider:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/provider/CloudLLMProvider.kt`
- **ApiKeyManager:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/security/ApiKeyManager.kt`
- **HybridResponseGenerator:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/response/HybridResponseGenerator.kt`
- **LLMModule:** `/Volumes/M-Drive/Coding/AVA/common/LLM/src/main/java/com/augmentalis/ava/features/llm/di/LLMModule.kt`

### Related Chapters

- **Chapter 28:** RAG System Architecture
- **Chapter 38:** LLM Model Management
- **Chapter 64:** Ocean Glass Design System
- **Chapter 72:** SOLID Architecture Refactoring

### External Resources

#### Firebase

- [Firebase Crashlytics Documentation](https://firebase.google.com/docs/crashlytics)
- [Firebase Console](https://console.firebase.google.com)
- [Crashlytics SDK Setup](https://firebase.google.com/docs/crashlytics/get-started?platform=android)

#### Android Security

- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [Jetpack Security](https://developer.android.com/jetpack/androidx/releases/security)

#### Cloud LLM Providers

- [OpenRouter API Documentation](https://openrouter.ai/docs)
- [Anthropic API Documentation](https://docs.anthropic.com)
- [Google AI Documentation](https://ai.google.dev/docs)
- [OpenAI API Documentation](https://platform.openai.com/docs)

#### Cryptography

- [AES-GCM Specification](https://nvlpubs.nist.gov/nistpubs/legacy/sp/nistspecialpublication800-38d.pdf)
- [Android Cipher Documentation](https://developer.android.com/reference/javax/crypto/Cipher)

### Git History

- **Commit:** `d630c473f8ff4a23519ec4aa4d4fa01869dd9127`
- **Message:** "feat: complete P0/P1 implementation (SOLID coordinators + encryption + cloud LLM)"
- **Date:** 2025-12-05
- **Author:** AVA AI Team

---

## Phase 2: Firebase Crashlytics Integration

### Conditional Configuration Setup

AVA implements **conditional Firebase Crashlytics** that only activates when `google-services.json` exists. This allows:

- **Development builds:** Work without Firebase configuration
- **Production builds:** Enable crash reporting with Firebase setup
- **Privacy-first design:** No data sent without explicit configuration

### Build Configuration

#### Root build.gradle.kts

Add Firebase plugins to buildscript dependencies:

```kotlin
// build.gradle.kts (project root)
buildscript {
    dependencies {
        // Firebase Crashlytics plugin (only applied when google-services.json exists)
        classpath("com.google.gms:google-services:4.4.0")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}
```

#### App build.gradle.kts

**Conditional plugin application:**

```kotlin
// android/ava/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")

    // Conditionally apply Firebase plugins only if google-services.json exists
    if (file("google-services.json").exists()) {
        id("com.google.gms.google-services")
        id("com.google.firebase.crashlytics")
    }
}

dependencies {
    // Firebase BOM (Bill of Materials) for version management
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase Crashlytics (only includes SDK if google-services.json exists)
    if (file("google-services.json").exists()) {
        implementation("com.google.firebase:firebase-crashlytics-ktx")
        implementation("com.google.firebase:firebase-analytics-ktx")
    }
}
```

**Why conditional?**
- Developers without Firebase setup can still build
- No compile errors if Firebase SDK is missing
- Automatic fallback to local Timber logging

### CrashReporter Implementation with Fallback

**File:** `/Volumes/M-Drive/Coding/AVA/common/core/Utils/src/androidMain/kotlin/com/augmentalis/ava/crashreporting/CrashReporter.kt`

**Key Design Decisions:**

| Feature | Implementation |
|---------|----------------|
| Firebase Detection | Reflection-based (no compile-time dependency) |
| Default State | Disabled (privacy-first) |
| Fallback Behavior | Timber logging when Firebase unavailable |
| Runtime Toggle | User can enable/disable anytime |

**Reflection-Based Detection:**

```kotlin
private fun checkFirebaseAvailability(): Boolean {
    return try {
        Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
        true
    } catch (e: ClassNotFoundException) {
        Timber.d("Firebase Crashlytics SDK not found in classpath")
        false
    } catch (e: Exception) {
        Timber.e(e, "Error checking Firebase Crashlytics availability")
        false
    }
}
```

**Initialization with Graceful Degradation:**

```kotlin
fun initialize(context: Context, enabled: Boolean = false) {
    if (isInitialized) {
        Timber.w("CrashReporter already initialized")
        return
    }

    isEnabled = enabled
    isInitialized = true

    // Check if Firebase Crashlytics is available
    firebaseAvailable = checkFirebaseAvailability()

    Timber.i("CrashReporter initialized (enabled: $enabled, firebase: $firebaseAvailable)")

    // Only use Firebase if available AND enabled
    if (enabled && firebaseAvailable) {
        setDeviceInfo()

        try {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(enabled)
            Timber.d("Firebase Crashlytics collection enabled: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase Crashlytics")
            firebaseAvailable = false
        }
    } else {
        Timber.d("Firebase Crashlytics SDK not available - using local logging only")
    }
}
```

**All Methods with Fallback:**

Every CrashReporter method follows this pattern:

```kotlin
fun recordException(throwable: Throwable, message: String) {
    // ALWAYS log locally (works without Firebase)
    Timber.e(throwable, "Non-fatal exception: $message")

    // Only send to Firebase if enabled AND available
    if (!isEnabled || !firebaseAvailable) return

    try {
        val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
        crashlytics.log(message)
        crashlytics.recordException(throwable)
    } catch (e: Exception) {
        Timber.e(e, "Failed to record exception with context to Firebase Crashlytics")
    }
}
```

### ProGuard Rules

**File:** `android/ava/proguard-rules.pro`

Add Firebase Crashlytics rules:

```proguard
# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable        # Keep stack traces readable
-keep public class * extends java.lang.Exception  # Keep exception classes

# Crashlytics NDK (if using native libraries)
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep Firebase Analytics (bundled with Crashlytics)
-keep class com.google.android.gms.measurement.** { *; }
```

### Testing Strategy

#### 1. Without Firebase (Development)

```bash
# Remove google-services.json (or rename)
mv android/ava/google-services.json android/ava/google-services.json.backup

# Build should succeed
./gradlew :android:ava:assembleDebug

# Check logs for fallback behavior
adb logcat | grep "CrashReporter"
# Should see: "Firebase Crashlytics SDK not found in classpath"
# Should see: "using local logging only"
```

#### 2. With Firebase (Production)

```bash
# Restore google-services.json
mv android/ava/google-services.json.backup android/ava/google-services.json

# Build with Firebase
./gradlew :android:ava:assembleDebug

# Check logs
adb logcat | grep "CrashReporter"
# Should see: "Firebase Crashlytics SDK available"
# Should see: "CrashReporter initialized (enabled: false, firebase: true)"
```

#### 3. Runtime Enabling

```kotlin
// In app code
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize (disabled by default)
        CrashReporter.initialize(this, enabled = false)

        // Check availability
        if (CrashReporter.isFirebaseAvailable()) {
            Log.d("MainActivity", "Firebase Crashlytics available")
        } else {
            Log.d("MainActivity", "Using local Timber logging only")
        }

        // User opts in via Settings
        // CrashReporter.setEnabled(true)
    }
}
```

#### 4. Test Crash Reporting

```kotlin
// Test non-fatal exception
try {
    throw RuntimeException("Test exception")
} catch (e: Exception) {
    CrashReporter.recordException(e, "Testing crash reporting")
}

// Test breadcrumbs
CrashReporter.log("MainActivity", "Activity created")
CrashReporter.log("User", "Clicked test button")

// Test custom keys
CrashReporter.setCustomKey("test_mode", true)
CrashReporter.setCustomKey("user_action", "test_crash")

// Test fatal crash (CAUTION: crashes app!)
// throw RuntimeException("Test fatal crash")
```

### Firebase Console Setup Instructions

#### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project" or use existing project
3. Enter project name (e.g., "AVA-Production")
4. Enable Google Analytics (optional but recommended)
5. Click "Create project"

#### 2. Add Android App

1. In Firebase Console, click "Add app" → Android icon
2. **Android package name:** `com.augmentalis.ava` (MUST match exactly)
3. **App nickname:** `AVA` (for identification)
4. **Debug signing certificate SHA-1:** (Optional for Crashlytics)
   ```bash
   # Get SHA-1 from debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Click "Register app"
6. **Download google-services.json**
7. Place file in: `android/ava/google-services.json`

#### 3. Enable Crashlytics

1. In Firebase Console, navigate to: **Build** → **Crashlytics**
2. Click "Get started" button
3. Follow setup wizard:
   - Verify SDK integration (gradle dependencies)
   - Verify plugin application
   - Force a test crash to verify setup
4. Click "Finish setup"

#### 4. Configure Crashlytics Settings

**In Firebase Console → Crashlytics → Settings:**

| Setting | Recommended Value | Purpose |
|---------|-------------------|---------|
| Data retention | 90 days | Balance storage vs history |
| Crash email notifications | Enabled | Alert on new crashes |
| Velocity alerts | Enabled | Detect crash spikes |
| ANR tracking | Enabled | Track app freezes |
| NDK crash reporting | Disabled (for now) | Enable when using native libs |

#### 5. Test Setup

**Force a test crash:**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable crash reporting
        CrashReporter.initialize(this, enabled = true)

        // Add test button
        setContent {
            Button(onClick = {
                // This will crash the app
                throw RuntimeException("Test crash for Firebase setup")
            }) {
                Text("Test Crash")
            }
        }
    }
}
```

**Verify in Firebase Console:**

1. Run app and tap "Test Crash" button
2. App will crash
3. Restart app (crash data uploads on next launch)
4. Wait 1-2 minutes for processing
5. Check Firebase Console → Crashlytics
6. You should see the test crash: "Test crash for Firebase setup"

#### 6. Verify Data Collection

**Check Crashlytics dashboard shows:**

- Crash-free users percentage
- Number of crashes
- Affected users
- Stack trace
- Device information
- Custom keys
- Breadcrumbs

### Integration with Settings UI

**Connect to Settings → Privacy → Crash Reporting:**

```kotlin
// In SettingsViewModel.kt
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val crashReportingEnabled: StateFlow<Boolean> =
        settingsRepository.isCrashReportingEnabled()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    fun onCrashReportingToggled(enabled: Boolean) {
        viewModelScope.launch {
            // Update preference
            settingsRepository.setCrashReportingEnabled(enabled)

            // Update CrashReporter
            CrashReporter.setEnabled(enabled)

            if (enabled) {
                // Set anonymized user ID (NEVER use email or real name)
                val userId = UUID.randomUUID().toString()
                CrashReporter.setUserId(userId)

                // Log device info
                CrashReporter.setCustomKey("app_version", BuildConfig.VERSION_NAME)
                CrashReporter.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            }
        }
    }
}

// In SettingsScreen.kt
@Composable
fun CrashReportingSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SettingsSection(title = "Privacy") {
        SettingsItem(
            title = "Crash Reporting",
            subtitle = if (enabled) {
                "Help improve AVA by sending crash reports"
            } else {
                "Crash reports stored locally only"
            },
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
        )

        if (enabled) {
            Text(
                text = "Crash reports include:\n" +
                       "• Device model and OS version\n" +
                       "• App version and stack traces\n" +
                       "• No personal information",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
            )
        }
    }
}
```

### Best Practices

#### DO:
- ✅ Initialize CrashReporter in `Application.onCreate()`
- ✅ Set `enabled = false` by default (privacy-first)
- ✅ Use reflection-based Firebase detection
- ✅ Always log to Timber (local fallback)
- ✅ Use anonymized user IDs only
- ✅ Test without `google-services.json` before release
- ✅ Add breadcrumbs for important user actions
- ✅ Record non-fatal exceptions for caught errors

#### DON'T:
- ❌ Enable crash reporting by default
- ❌ Collect PII (names, emails, locations)
- ❌ Force Firebase SDK dependency at compile time
- ❌ Skip user consent flow
- ❌ Log sensitive data in breadcrumbs
- ❌ Use real user IDs (use UUIDs)

### Troubleshooting Phase 2

#### "google-services.json not found"

**Symptom:** Build fails with missing `google-services.json`

**Solution:**
```kotlin
// Verify conditional plugin application in build.gradle.kts
if (file("google-services.json").exists()) {
    id("com.google.gms.google-services")
}
```

#### "Crashlytics SDK not available"

**Symptom:** Logs show "Firebase Crashlytics SDK not found in classpath"

**Solution:**
- This is expected behavior when building without Firebase
- App will use local Timber logging only
- No action needed unless you want Firebase integration

#### "Crashes not appearing in console"

**Symptom:** Test crashes don't show in Firebase Console

**Solution:**
1. Verify `CrashReporter.isEnabled()` returns `true`
2. Restart app after crash (data uploads on next launch)
3. Wait 1-2 minutes for processing
4. Check Firebase Console → Crashlytics
5. Verify internet connection
6. Check logcat for upload errors

---

### Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-06 | Initial documentation for commit d630c473 |
| 1.1 | 2025-12-06 | Added Phase 2: Firebase Crashlytics Integration |

---

**Author:** Manoj Jhawar
**Date:** 2025-12-06
**Commit:** d630c473
**Version:** 1.1
