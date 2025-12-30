# AVA AON Models - Developer Guide

**Version:** 1.0
**Last Updated:** 2025-11-23
**For:** AVA Developers

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [AON File Format](#aon-file-format)
3. [Model Wrapping](#model-wrapping)
4. [Model Loading](#model-loading)
5. [Integration Guide](#integration-guide)
6. [Testing](#testing)
7. [Deployment](#deployment)
8. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Components

```
┌─────────────────────────────────────────┐
│         RAG Application Layer           │
│  (ChatViewModel, RAGChatEngine)         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      ONNXEmbeddingProvider              │
│  - Model loading (AON/ONNX)             │
│  - Embedding generation                 │
│  - Caching                              │
└──────────────┬──────────────────────────┘
               │
      ┌────────┴────────┐
      │                 │
┌─────▼─────┐    ┌─────▼──────────┐
│ AONFile   │    │ ONNX Runtime   │
│ Manager   │    │ (ai.onnxruntime)│
│           │    │                │
│ -unwrap() │    │ -createSession()│
│ -verify() │    │ -run()          │
└───────────┘    └─────────────────┘
```

### File Hierarchy

```
Project Root
├── Universal/AVA/Features/RAG/
│   ├── src/commonMain/kotlin/.../rag/
│   │   ├── domain/
│   │   │   ├── Chunk.kt            # Embedding models (Float32/Int8)
│   │   │   └── RAGConfig.kt        # Configuration
│   │   └── embeddings/
│   │       └── EmbeddingProvider.kt # Interface
│   └── src/androidMain/kotlin/.../rag/embeddings/
│       ├── ONNXEmbeddingProvider.android.kt  # ONNX implementation
│       ├── AONFileManager.kt                 # AON wrapper/unwrapper
│       ├── AONPackageManager.kt              # Package whitelist presets
│       └── AndroidModelDownloadManager.kt    # Download manager
│
├── apps/ava-standalone/src/main/kotlin/.../ui/settings/
│   └── RAGModelDownloadScreen.kt    # UI for model downloads
│
└── docs/
    ├── AON-FILE-FORMAT.md           # Technical spec
    ├── USER-GUIDE-RAG-MODELS.md     # User documentation
    ├── DEVELOPER-GUIDE-AON-MODELS.md # This file
    └── backlog/
        └── SECURE-KEY-ROTATION.md    # Future enhancement
```

---

## AON File Format

### Structure

```
┌─────────────────────────────────────┐
│  AON Header (256 bytes)             │
│  - Magic: "AVA-AON\x01"             │
│  - HMAC-SHA256 signature            │
│  - Package whitelist (3 max)        │
│  - Expiry timestamp                 │
│  - License tier (0/1/2)             │
├─────────────────────────────────────┤
│  ONNX Model Data (variable)         │
│  - Standard ONNX Protocol Buffer    │
├─────────────────────────────────────┤
│  AON Footer (128 bytes)             │
│  - Header SHA-256                   │
│  - ONNX SHA-256                     │
│  - CRC32 checksum                   │
└─────────────────────────────────────┘
```

### Security Features

| Feature | Purpose | Implementation |
|---------|---------|----------------|
| **Custom Magic Bytes** | Break third-party loaders | `0x41 0x56 0x41 0x2D...` |
| **HMAC Signature** | Verify authenticity | HMAC-SHA256 with master key |
| **Package Whitelist** | Restrict app access | MD5 hashes (up to 3) |
| **Integrity Checks** | Detect tampering | SHA-256 (header + ONNX + footer) |
| **Expiry Timestamp** | Time-limited licenses | Unix timestamp (0 = never) |
| **License Tiers** | Feature gating | 0=free, 1=pro, 2=enterprise |

### File Format Reference

See full specification: [`docs/AON-FILE-FORMAT.md`](AON-FILE-FORMAT.md)

---

## Model Wrapping

### Basic Usage

```kotlin
import com.augmentalis.ava.features.rag.embeddings.AONFileManager

// Wrap ONNX file
val aonFile = AONFileManager.wrapONNX(
    onnxFile = File("/path/to/all-MiniLM-L6-v2.onnx"),
    outputFile = File("/path/to/AVA-384-Base-INT8.aon"),
    modelId = "AVA-384-Base-INT8",
    modelVersion = 1,
    allowedPackages = listOf(
        "com.augmentalis.ava",
        "com.augmentalis.avaconnect",
        "com.augmentalis.voiceos"
    ),
    expiryTimestamp = 0,  // Never expires
    licenseTier = 0       // Free tier
)
```

### Using Presets

```kotlin
import com.augmentalis.ava.features.rag.embeddings.AONPackageManager
import com.augmentalis.ava.features.rag.embeddings.wrapWithPreset

// Wrap with standard AVA apps preset
AONFileManager.wrapWithPreset(
    onnxFile = File("model.onnx"),
    outputFile = File("model.aon"),
    modelId = "AVA-384-Base-INT8",
    strategy = AONPackageManager.DistributionStrategy.AVA_STANDARD
)

// Wrap with Avanues platform preset
AONFileManager.wrapWithPreset(
    onnxFile = File("model.onnx"),
    outputFile = File("model-avanues.aon"),
    modelId = "AVA-384-Base-INT8",
    strategy = AONPackageManager.DistributionStrategy.AVANUES_PLATFORM
)
```

### Command-Line Tool

```bash
# Wrap single model
./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
  --args="wrap \
  --input=/path/to/model.onnx \
  --output=/path/to/model.aon \
  --model-id=AVA-384-Base-INT8 \
  --version=1 \
  --license=0 \
  --expiry-days=365"

# Batch wrap all models in directory
./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
  --args="batch \
  --input-dir=/path/to/onnx-models \
  --output-dir=/path/to/aon-models"

# Verify AON file
./gradlew :Universal:AVA:Features:RAG:runAONWrapper \
  --args="verify --file=/path/to/model.aon"
```

---

## Model Loading

### Automatic Loading (Recommended)

```kotlin
// ONNXEmbeddingProvider handles everything
val provider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-384-Base-INT8"
)

// Automatically:
// 1. Checks unified model repository
// 2. Detects AON vs ONNX format
// 3. Unwraps AON files (with verification)
// 4. Loads into ONNX Runtime
// 5. Caches unwrapped model

val embedding = provider.embed("Hello world")
```

### Loading Priority

```
1. /sdcard/ava-ai-models/embeddings/{modelId}.aon    (PRIMARY - unified repo)
2. /sdcard/ava-ai-models/embeddings/{modelId}.onnx   (legacy)
3. assets/models/{modelId}.aon                        (bundled)
4. assets/models/{modelId}.onnx                       (legacy bundled)
5. /sdcard/Android/data/{package}/files/models/*.aon  (app-specific)
6. ModelDownloadManager (download on-demand)
```

### Manual Unwrapping

```kotlin
import com.augmentalis.ava.features.rag.embeddings.AONFileManager

// Unwrap and verify
try {
    val onnxBytes = AONFileManager.unwrapAON(
        aonFile = File("/path/to/model.aon"),
        context = context
    )

    // onnxBytes ready for ONNX Runtime
    // Verification passed: package, expiry, integrity, signature

} catch (e: SecurityException) {
    // Authentication failed:
    // - Wrong package name
    // - Expired timestamp
    // - Tampered file
    // - Invalid signature
    Log.e("AON", "Security violation: ${e.message}")
}
```

### File Type Detection

```kotlin
val file = File("/path/to/model.aon")

if (AONFileManager.isAONFile(file)) {
    // New wrapped format - requires unwrapping
    val onnxBytes = AONFileManager.unwrapAON(file, context)
} else {
    // Legacy ONNX format - use directly
    val onnxBytes = file.readBytes()
}
```

---

## Integration Guide

### Step 1: Add Dependencies

```kotlin
// In Universal/AVA/Features/RAG/build.gradle.kts
dependencies {
    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### Step 2: Configure RAG

```kotlin
import com.augmentalis.ava.features.rag.domain.RAGConfig
import com.augmentalis.ava.features.rag.domain.EmbeddingConfig

val ragConfig = RAGConfig(
    embeddingConfig = EmbeddingConfig(
        preferredProvider = EmbeddingProvider.ONNX,
        modelName = "AVA-384-Base-INT8",  // AON file auto-detected
        dimension = 384,
        batchSize = 32,
        quantize = true  // Int8 quantization (75% space savings)
    ),
    chunkingConfig = ChunkingConfig(
        strategy = ChunkingStrategy.HYBRID,
        maxTokens = 512,
        overlapTokens = 50
    )
)
```

### Step 3: Use in Application

```kotlin
import com.augmentalis.ava.features.rag.chat.RAGChatEngine

class ChatViewModel(
    private val ragConfig: RAGConfig,
    context: Context
) : ViewModel() {

    private val ragEngine = RAGChatEngine(
        config = ragConfig,
        context = context
    )

    suspend fun searchDocuments(query: String): List<SearchResult> {
        return ragEngine.search(query, topK = 5)
    }

    suspend fun answerQuestion(query: String): String {
        return ragEngine.answerWithContext(query)
    }
}
```

### Step 4: Add UI (Optional)

```kotlin
import com.augmentalis.ava.ui.settings.RAGModelDownloadScreen

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    // ... other settings ...

    ListItem(
        headlineContent = { Text("RAG Embedding Models") },
        supportingContent = { Text("Download models for document search") },
        leadingContent = { Icon(Icons.Default.Download, null) },
        modifier = Modifier.clickable {
            navController.navigate("rag-models")
        }
    )
}

// Navigation
composable("rag-models") {
    RAGModelDownloadScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

---

## Testing

### Unit Tests

```kotlin
import com.augmentalis.ava.features.rag.embeddings.AONFileManager
import org.junit.Test
import org.junit.Assert.*

class AONFileManagerTest {

    @Test
    fun `wrapONNX creates valid AON file`() {
        val onnxFile = createTempONNXFile()
        val aonFile = File.createTempFile("test", ".aon")

        AONFileManager.wrapONNX(
            onnxFile = onnxFile,
            outputFile = aonFile,
            modelId = "test-model"
        )

        assertTrue(AONFileManager.isAONFile(aonFile))
        assertEquals(onnxFile.length() + 384, aonFile.length())
    }

    @Test
    fun `unwrapAON rejects unauthorized package`() {
        val aonFile = createTestAONFile(
            allowedPackages = listOf("com.other.app")
        )

        val exception = assertThrows<SecurityException> {
            AONFileManager.unwrapAON(aonFile, mockContext("com.augmentalis.ava"))
        }

        assertTrue(exception.message!!.contains("not authorized"))
    }

    @Test
    fun `unwrapAON rejects expired model`() {
        val aonFile = createTestAONFile(
            expiryTimestamp = 1000L  // Jan 1970
        )

        assertThrows<SecurityException> {
            AONFileManager.unwrapAON(aonFile, mockContext())
        }
    }
}
```

### Integration Tests

```kotlin
@Test
fun `ONNXEmbeddingProvider loads AON file transparently`() = runTest {
    // Place test AON file
    val aonFile = File(context.cacheDir, "test-model.aon")
    createTestAONFile().copyTo(aonFile)

    // Provider should unwrap automatically
    val provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "test-model"
    )

    assertTrue(provider.isAvailable())

    val embedding = provider.embed("test text")
    assertEquals(384, embedding.dimension)
}
```

---

## Deployment

### Development Build

```bash
# Bundle free model only (90 MB)
mkdir -p app/src/main/assets/models/
cp AVA-384-Base-INT8.aon app/src/main/assets/models/

# Build APK (~120 MB)
./gradlew assembleDebug
```

### Production Build

```bash
# NO models bundled (small APK ~30 MB)
# Models downloaded on-demand

./gradlew assembleRelease

# Deploy to unified repository
adb push AVA-*.aon /sdcard/ava-ai-models/embeddings/
```

### Model Distribution

**Option 1: Download Server**
```
https://models.augmentalis.com/embeddings/
├── AVA-384-Base-INT8.aon          (90 MB)
├── AVA-384-Fast-INT8.aon          (61 MB)
├── AVA-768-Qual-INT8.aon          (420 MB)
├── AVA-384-Multi-INT8.aon         (470 MB)
└── checksums.sha256
```

**Option 2: HuggingFace Mirror**
```
https://huggingface.co/augmentalis/ava-embeddings/
└── resolve/main/
    ├── AVA-384-Base-INT8.aon
    └── ...
```

**Option 3: Google Play Dynamic Delivery**
```kotlin
// In build.gradle.kts
android {
    dynamicFeatures = setOf(":feature:rag-models")
}
```

---

## Troubleshooting

### "Invalid AON file: bad magic bytes"

**Cause:** File is not AON format or corrupted

**Solutions:**
```bash
# Check file type
xxd -l 8 model.aon
# Should show: 41 56 41 2d 41 4f 4e 01 ("AVA-AON\x01")

# Re-wrap if needed
./gradlew runAONWrapper --args="wrap --input=model.onnx --output=model.aon ..."
```

### "Package not authorized to use this AON file"

**Cause:** App package name not in whitelist

**Solutions:**
```kotlin
// Check current package
Log.d("AON", "Package: ${context.packageName}")

// Re-wrap with correct packages
AONFileManager.wrapWithPreset(
    ...,
    strategy = AONPackageManager.DistributionStrategy.AVA_STANDARD
)
```

### "AON file has expired"

**Cause:** Past expiry timestamp

**Solutions:**
```bash
# Re-wrap without expiry
./gradlew runAONWrapper --args="wrap ... --expiry-days=0"

# Or with new expiry
./gradlew runAONWrapper --args="wrap ... --expiry-days=365"
```

### "ONNX data integrity check failed"

**Cause:** File corrupted or tampered

**Solutions:**
```bash
# Verify SHA256
sha256sum model.aon

# Re-download from trusted source
curl -O https://models.augmentalis.com/embeddings/AVA-384-Base-INT8.aon
```

### Model Not Found

**Cause:** Model not in search paths

**Solutions:**
```bash
# Check unified repository
ls /sdcard/ava-ai-models/embeddings/

# Check app-specific
ls /sdcard/Android/data/com.augmentalis.ava/files/models/

# Check bundled assets
./gradlew :app:listAssets | grep models
```

---

## Performance Optimization

### Batch Embedding

```kotlin
// Process multiple chunks at once
val chunks = listOf("text1", "text2", "text3", ...)

val embeddings = provider.embedBatch(
    texts = chunks,
    batchSize = 50  // Process 50 at a time
)

// 8.3x faster than sequential for large batches
```

### Caching

```kotlin
// LRU cache for query embeddings
val cache = LruCache<String, Embedding>(maxSize = 100)

fun getEmbedding(text: String): Embedding {
    return cache[text] ?: provider.embed(text).also {
        cache.put(text, it)
    }
}
```

### Memory Management

```kotlin
// Release provider when done
override fun onCleared() {
    super.onCleared()
    provider.release()
}
```

---

## Best Practices

### DO ✅

- ✅ Use presets (`AONPackageManager`) for package whitelists
- ✅ Check `isAONFile()` before unwrapping
- ✅ Handle `SecurityException` gracefully
- ✅ Cache unwrapped models
- ✅ Batch embed operations
- ✅ Use Int8 quantization for storage
- ✅ Test with both AON and ONNX files

### DON'T ❌

- ❌ Hardcode package names (use presets)
- ❌ Skip security checks
- ❌ Ignore `SecurityException`
- ❌ Unwrap on UI thread (use coroutines)
- ❌ Bundle all models in APK
- ❌ Store master key in source code
- ❌ Exceed 3 package whitelist limit

---

## API Reference

### AONFileManager

```kotlin
object AONFileManager {
    // Wrap ONNX file
    fun wrapONNX(
        onnxFile: File,
        outputFile: File,
        modelId: String,
        modelVersion: Int = 1,
        allowedPackages: List<String> = listOf(...),
        expiryTimestamp: Long = 0,
        licenseTier: Int = 0,
        encrypt: Boolean = false
    ): File

    // Unwrap AON file
    fun unwrapAON(aonFile: File, context: Context): ByteArray

    // Check if file is AON format
    fun isAONFile(file: File): Boolean
}
```

### AONPackageManager

```kotlin
object AONPackageManager {
    val AVA_STANDARD_APPS: List<String>
    val AVANUES_PLATFORM_APPS: List<String>
    val DEVELOPMENT_APPS: List<String>

    fun getPackagesForStrategy(strategy: DistributionStrategy): List<String>
    fun isAVAEcosystemPackage(packageName: String): Boolean
    fun validatePackageList(packages: List<String>)
}
```

### ONNXEmbeddingProvider

```kotlin
class ONNXEmbeddingProvider(
    private val context: Context,
    private val modelPath: String? = null,
    private val modelId: String = "AVA-384-Base-INT8"
) : EmbeddingProvider {

    override val name: String
    override val dimension: Int

    override suspend fun isAvailable(): Boolean
    override suspend fun embed(text: String): Embedding
    override suspend fun embedBatch(texts: List<String>): List<Embedding>
    override fun release()
}
```

---

## Additional Resources

- **AON Format Spec:** `docs/AON-FILE-FORMAT.md`
- **User Guide:** `docs/USER-GUIDE-RAG-MODELS.md`
- **Backlog:** `docs/backlog/SECURE-KEY-ROTATION.md`
- **Source Code:** `Universal/AVA/Features/RAG/src/androidMain/.../embeddings/`

---

**Last Updated:** 2025-11-23
**AVA Version:** 3.0+
**Maintained By:** AVA AI Team
