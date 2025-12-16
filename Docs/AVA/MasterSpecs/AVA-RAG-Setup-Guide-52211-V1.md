# RAG Setup Guide - Complete Configuration for AVA AI

**Date:** 2025-11-22
**Phase:** 2.0 - RAG Integration Documentation
**Audience:** Developers & Power Users
**Estimated Time:** 45 minutes

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Model Download & Installation](#model-download--installation)
3. [Document Ingestion Workflow](#document-ingestion-workflow)
4. [Settings Configuration](#settings-configuration)
5. [First Chat Example](#first-chat-example)
6. [Advanced Configuration](#advanced-configuration)
7. [Troubleshooting Setup](#troubleshooting-setup)

---

## Prerequisites

### System Requirements

**Minimum:**
- Android 8.0+ (API 26+)
- 2GB RAM
- 500MB free storage
- ARMv7 or ARM64 processor

**Recommended:**
- Android 11+ (API 30+)
- 4GB RAM
- 2GB free storage
- ARM64 processor
- Physical device (not emulator)

### Software Requirements

- **Android Studio:** Arctic Fox or later
- **ADB:** Installed and working
- **Internet:** For one-time model download

### Verification

```bash
# Check device connection
adb devices

# Expected output:
# List of devices attached
# AB1234567    device

# Check Android version
adb shell getprop ro.build.version.release
# Should be >= 8.0

# Check available storage
adb shell df -h /sdcard
# Should have at least 500MB free
```

---

## Model Download & Installation

### Option 1: Bundled Model (Recommended - Already Included!)

**AVA now bundles the English model** - no download required for English documents.

**Model:** AVA-ONX-384-BASE-INT8
**Size:** 22 MB (included in APK)
**Languages:** English only
**Quality:** High (95%)

**Status Check:**
```kotlin
// The bundled model is automatically loaded
val embeddingProvider = ONNXEmbeddingProvider(context)
// Model is ready immediately!
```

**Skip to:** [Document Ingestion](#document-ingestion-workflow) if using English documents only.

---

### Option 2: Multilingual Model (For Non-English Documents)

If you need support for Chinese, Spanish, Arabic, Japanese, or other languages, download the multilingual model.

#### Step 1: Download Model

**Download from HuggingFace:**
```bash
# Create local directory
mkdir -p ~/ava-models

# Download multilingual model (449 MB)
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o ~/ava-models/AVA-ONX-384-MULTI.onnx

# Verify download
ls -lh ~/ava-models/AVA-ONX-384-MULTI.onnx
# Should show ~449 MB
```

#### Step 2: Quantize Model (Optional but Recommended)

**Reduce size by 75% with minimal quality loss:**

```bash
# Install quantization tool
pip3 install onnxruntime

# Quantize to INT8 (449 MB → 113 MB)
python3 /Volumes/M-Drive/Coding/AVA/scripts/required/quantize-models.py \
  ~/ava-models/AVA-ONX-384-MULTI.onnx \
  ~/ava-models/AVA-ONX-384-MULTI-INT8.onnx \
  int8

# Output:
# Original size: 449.00 MB
# Quantized size: 113.00 MB
# Size reduction: 74.8%
# Quality retention: ~95%
```

#### Step 3: Push to Device

```bash
# Create models directory on device
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/

# Push quantized model
adb push ~/ava-models/AVA-ONX-384-MULTI-INT8.onnx \
  /sdcard/Android/data/com.augmentalis.ava/files/models/

# Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
# Should show AVA-ONX-384-MULTI-INT8.onnx (113 MB)
```

#### Step 4: Configure in App

1. Open AVA app
2. Navigate to **Settings** → **Developer Settings**
3. Tap **"RAG Embedding Model"**
4. Select **"AVA-ONX-384-MULTI-INT8"**
5. Restart RAG system (app will prompt)

**Verification:**
```kotlin
// In code or via log inspection
val provider = ONNXEmbeddingProvider(context, modelId = "AVA-ONX-384-MULTI-INT8")
val isAvailable = provider.isAvailable()
// Should return: true
```

---

### Model Options Summary

| Model | Size | Languages | Download Required | Use Case |
|-------|------|-----------|-------------------|----------|
| **AVA-ONX-384-BASE-INT8** | 22 MB | English | ❌ Bundled | **Default** - English docs |
| AVA-ONX-384-MULTI-INT8 | 113 MB | 50+ | ✅ Yes | Multilingual docs |
| AVA-ONX-384-MULTI (FP32) | 449 MB | 50+ | ✅ Yes | Highest quality |
| AVA-ONX-768-QUAL-INT8 | 105 MB | English | ✅ Yes | Production quality |

**Recommendation:** Use bundled model for English. Download MULTI-INT8 only if you need other languages.

---

## Document Ingestion Workflow

### Supported Formats

AVA supports **6 document formats** with varying processing speeds:

| Format | Speed | Best For | Example |
|--------|-------|----------|---------|
| **DOCX** | ⚡ 10-20 pages/sec | Modern manuals, reports | `manual.docx` |
| **TXT** | ⚡ Instant | Logs, notes, plain text | `notes.txt` |
| **HTML** | ⚡ 20-50 pages/sec | Web docs, saved pages | `guide.html` |
| **Markdown** | ⚡ Instant | README files, GitHub docs | `README.md` |
| **RTF** | ⚡ 5-10 pages/sec | Legacy Word docs | `manual.rtf` |
| **PDF** | 🐌 2 pages/sec | Scanned manuals, official docs | `manual.pdf` |

**💡 Pro Tip:** Use DOCX instead of PDF when possible - it's **5-10x faster!**

---

### Workflow Steps

#### Step 1: Prepare Documents

**Option A: Copy to Device Storage**

```bash
# Copy PDF to device
adb push /path/to/your/manual.pdf /sdcard/Download/

# Copy multiple documents
adb push /path/to/docs/*.pdf /sdcard/Download/
adb push /path/to/docs/*.docx /sdcard/Download/
```

**Option B: Use Web URLs** (for HTML documents)

No file needed! Just use the URL directly in Step 2.

---

#### Step 2: Add Document via UI

**Method 1: File Picker (Most Common)**

1. Open AVA app
2. Navigate to **RAG** → **Documents**
3. Tap **+ Add Document** (FAB button)
4. Select **"From File"**
5. Browse to `/sdcard/Download/`
6. Select your document
7. (Optional) Edit title
8. Tap **"Add"**

**Method 2: URL Import** (for web documentation)

1. Navigate to **RAG** → **Documents**
2. Tap **+ Add Document**
3. Select **"From URL"**
4. Enter URL: `https://developer.android.com/guide/components/activities`
5. Enter title: `Android Activities Guide`
6. Tap **"Import"**

**Method 3: Programmatic** (for automation)

```kotlin
// In your code or test script
val repository = SQLiteRAGRepository(
    context = context,
    embeddingProvider = ONNXEmbeddingProvider(context)
)

lifecycleScope.launch {
    // Add local file
    val result = repository.addDocument(
        AddDocumentRequest(
            filePath = "/sdcard/Download/manual.pdf",
            title = "User Manual",
            documentType = DocumentType.PDF,
            processImmediately = true
        )
    )

    when {
        result.isSuccess -> {
            Log.i("RAG", "Document added: ${result.getOrNull()?.documentId}")
        }
        result.isFailure -> {
            Log.e("RAG", "Failed: ${result.exceptionOrNull()?.message}")
        }
    }
}
```

---

#### Step 3: Monitor Processing

**UI Indicators:**

The document card shows real-time status:

```
┌────────────────────────────────────────┐
│ 📄 User Manual                         │
│ Status: PROCESSING (45%)               │
│ ▓▓▓▓▓▓▓▓░░░░░░░░░░░                   │
│ Type: PDF · 150 pages                  │
│ Added: 2025-11-22 14:30                │
└────────────────────────────────────────┘
```

**Status Values:**
- **PENDING** - Queued for processing
- **PROCESSING** - Currently parsing/chunking/embedding
- **INDEXED** - Ready for search
- **FAILED** - Error occurred (see logs)

**Via Logcat:**

```bash
# Monitor processing
adb logcat | grep "RAG"

# Expected output:
# 14:30:00 I RAG: Parsing document: manual.pdf
# 14:30:05 I RAG: Extracted 150 pages
# 14:30:08 I RAG: Generated 450 chunks (512 tokens each)
# 14:30:40 I RAG: Generated embeddings for 450 chunks
# 14:30:42 I RAG: Stored 450 chunks in database
# 14:30:42 I RAG: Document indexed successfully (ID: doc_abc123)
```

**Processing Time Estimates:**

| Document | Pages | Format | Time (Parallel Mode) |
|----------|-------|--------|---------------------|
| Small Manual | 50 | PDF | ~30 sec |
| Medium Manual | 150 | PDF | ~1.5 min |
| Large Manual | 500 | PDF | ~4.5 min |
| Small Manual | 50 | DOCX | ~15 sec |
| Medium Manual | 150 | DOCX | ~30 sec |
| Large Manual | 500 | DOCX | ~1.5 min |

---

#### Step 4: Verify Indexing

**Check Document Status:**

1. Navigate to **RAG** → **Documents**
2. Verify status shows **"INDEXED"**
3. Check chunk count (should be > 0)

**Via Code:**

```kotlin
// Check statistics
val stats = repository.getStatistics().getOrThrow()

Log.i("RAG", "Total documents: ${stats.totalDocuments}")
Log.i("RAG", "Indexed documents: ${stats.indexedDocuments}")
Log.i("RAG", "Total chunks: ${stats.totalChunks}")
Log.i("RAG", "Storage used: ${stats.storageUsedBytes / 1024 / 1024} MB")
```

**Expected Output:**
```
Total documents: 3
Indexed documents: 3
Total chunks: 1,247
Storage used: 2.7 MB
```

---

#### Step 5: Rebuild Clusters (After Bulk Import)

**Why:** Enables 40x faster search for large document collections.

**When:**
- After adding first 1,000+ chunks
- After bulk document imports (10+ documents)
- When search feels slow

**How:**

**Via UI:**
1. Navigate to **RAG** → **Documents**
2. Tap **⋮** (overflow menu)
3. Select **"Rebuild Clusters"**
4. Wait for completion (shows progress toast)

**Via Code:**
```kotlin
lifecycleScope.launch {
    val stats = repository.rebuildClusters().getOrThrow()

    Log.i("RAG", "Clustering complete:")
    Log.i("RAG", "  Chunks: ${stats.chunkCount}")
    Log.i("RAG", "  Clusters: ${stats.clusterCount}")
    Log.i("RAG", "  Time: ${stats.timeMs / 1000}s")
}
```

**Performance Impact:**

| Chunks | Before Clustering | After Clustering | Speedup |
|--------|-------------------|------------------|---------|
| 1,000 | 5ms | 5ms | 1x |
| 10,000 | 50ms | 15ms | 3.3x |
| 100,000 | 500ms | 25ms | 20x |
| 200,000 | 1,000ms | 25ms | **40x** |

---

## Settings Configuration

### RAG Settings Location

**Path:** Settings → Chat → RAG Configuration

### Available Settings

#### 1. Enable RAG

**Toggle:** On/Off
**Default:** Off
**Description:** Enable document-based responses in chat

```kotlin
// In code
chatPreferences.setRAGEnabled(true)

// Via UI
Settings → Chat → RAG Configuration → ☑ Enable RAG
```

#### 2. Select Documents

**Type:** Multi-select dialog
**Default:** All indexed documents
**Description:** Choose which documents to search

**Steps:**
1. Settings → Chat → RAG Configuration
2. Tap **"Select Documents"**
3. Check documents to include
4. Tap **"Save"**

**Filtering:**
```kotlin
// Select specific documents
val selectedDocs = listOf("doc_manual", "doc_guide")
chatPreferences.setRAGDocuments(selectedDocs)

// Or select all
val allDocs = repository.listDocuments(status = DocumentStatus.INDEXED)
    .map { it.id }
    .toList()
chatPreferences.setRAGDocuments(allDocs)
```

#### 3. Similarity Threshold

**Range:** 0.5 - 0.9
**Default:** 0.7 (70%)
**Description:** Minimum relevance score to include chunk

**Interpretation:**
- **0.5 - 0.6:** Broad search, more results, less precise
- **0.7 (default):** Balanced - good precision and recall
- **0.8 - 0.9:** Narrow search, fewer results, very precise

**Tuning:**
```kotlin
// Lower for exploratory queries
chatPreferences.setRAGSimilarity(0.6f)

// Higher for exact lookups
chatPreferences.setRAGSimilarity(0.85f)
```

#### 4. Max Context Chunks

**Range:** 3 - 10
**Default:** 5
**Description:** How many document chunks to include in context

**Trade-offs:**

| Value | Context Quality | Response Speed | Memory |
|-------|----------------|----------------|--------|
| 3 | Lower | Faster | ~300 MB |
| **5 (default)** | **Good** | **Good** | **~400 MB** |
| 7 | Better | Slower | ~500 MB |
| 10 | Best | Slowest | ~600 MB |

```kotlin
// Quick responses, lower quality
chatPreferences.setRAGMaxChunks(3)

// Best quality, slower
chatPreferences.setRAGMaxChunks(10)
```

---

### Complete Settings Example

**File:** `Core/Data/prefs/ChatPreferences.kt`

```kotlin
data class ChatPreferences(
    // Existing fields...
    val conversationMode: ConversationMode = ConversationMode.VOICE_FIRST,

    // RAG Settings
    val ragEnabled: Boolean = false,
    val ragSelectedDocumentIds: List<String> = emptyList(),
    val ragMinSimilarity: Float = 0.7f,
    val ragMaxChunks: Int = 5,
    val ragAutoDetect: Boolean = true // Auto-decide when to use RAG
)

// Extension functions for settings updates
suspend fun ChatPreferences.updateRAGEnabled(enabled: Boolean) {
    dataStore.edit { preferences ->
        preferences[PreferencesKeys.RAG_ENABLED] = enabled
    }
}

suspend fun ChatPreferences.updateRAGDocuments(documentIds: List<String>) {
    dataStore.edit { preferences ->
        preferences[PreferencesKeys.RAG_DOCUMENT_IDS] = documentIds.joinToString(",")
    }
}

suspend fun ChatPreferences.updateRAGSimilarity(similarity: Float) {
    dataStore.edit { preferences ->
        preferences[PreferencesKeys.RAG_MIN_SIMILARITY] = similarity
    }
}
```

---

## First Chat Example

### Scenario: Ask About Indexed Document

**Setup:**
1. ✅ Model installed (bundled or downloaded)
2. ✅ Document indexed (status: INDEXED)
3. ✅ RAG enabled in settings
4. ✅ Document selected

**Example Conversation:**

```
USER: How do I reset the device?

[RAG Active 📚]
ASSISTANT: According to the User Manual (page 42), to reset the device:

1. Turn off the device completely
2. Press and hold the power button for 10 seconds
3. Release when you see the reset screen
4. Select "Factory Reset" from the menu
5. Confirm your choice

Note: This will erase all data. Make sure to backup first.

Sources:
📄 User Manual (p.42) - 95% relevance
📄 Troubleshooting Guide (p.15) - 87% relevance
```

### Behind the Scenes

**What happens:**

```
1. User query: "How do I reset the device?"
   ↓
2. ChatViewModel.sendMessage("How do I reset the device?")
   ↓
3. shouldUseRAG() → true (factual question, RAG enabled, has docs)
   ↓
4. RAGRepository.search(query="How do I reset the device?", maxResults=5, minSimilarity=0.7)
   ↓
5. Returns 3 relevant chunks:
   - Manual.pdf, page 42, similarity: 0.95
   - Troubleshooting.pdf, page 15, similarity: 0.87
   - FAQ.pdf, page 8, similarity: 0.73
   ↓
6. Assemble context (chunks + metadata)
   ↓
7. ResponseGenerator.generateWithContext(query, context, history)
   ↓
8. Stream response to UI
   ↓
9. Display message with sources
```

**Logs:**

```bash
14:45:00 I ChatViewModel: Received message: "How do I reset the device?"
14:45:00 D ChatViewModel: RAG enabled: true, Has docs: true
14:45:00 D ChatViewModel: Query is factual: true
14:45:00 I ChatViewModel: Using RAG pathway
14:45:00 D RAGRepository: Searching for: "How do I reset the device?"
14:45:00 D RAGRepository: Cluster search: 256 clusters -> top 3
14:45:00 D RAGRepository: Chunk search: ~2,340 candidates
14:45:00 I RAGRepository: Found 3 results (>= 0.7 similarity) in 28ms
14:45:00 D ChatViewModel: Assembling context from 3 chunks
14:45:01 I ResponseGenerator: Generating response with RAG context
14:45:02 D ChatViewModel: Streaming response (chunk 1/15)
14:45:03 D ChatViewModel: Streaming response (chunk 15/15, final)
14:45:03 I ChatViewModel: Response complete with 3 sources
```

---

### Example Code: Complete Flow

```kotlin
// Example: Send message programmatically
class ExampleActivity : ComponentActivity() {

    @Inject
    lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // 1. Enable RAG
            chatViewModel.setRAGEnabled(true)

            // 2. Load and select documents
            val documents = mutableListOf<Document>()
            chatViewModel.ragRepository.listDocuments(status = DocumentStatus.INDEXED)
                .collect { doc ->
                    documents.add(doc)
                }
            chatViewModel.setSelectedDocuments(documents)

            // 3. Send query
            chatViewModel.sendMessage("How do I reset the device?")

            // 4. Observe response
            chatViewModel.messages.collect { messages ->
                val latestMessage = messages.lastOrNull()
                if (latestMessage?.role == MessageRole.ASSISTANT && !latestMessage.isStreaming) {
                    Log.i("Example", "Response: ${latestMessage.content}")
                    Log.i("Example", "Sources: ${latestMessage.sources?.size ?: 0}")

                    latestMessage.sources?.forEach { source ->
                        Log.i("Example", "  - ${source.documentTitle} (p.${source.pageNumber})")
                    }
                }
            }
        }
    }
}
```

---

## Advanced Configuration

### Custom Embedding Model

```kotlin
// Use custom model path
val customEmbeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-768-QUAL", // High-quality model
    modelPath = "/path/to/custom/model.onnx" // Optional override
)

val repository = SQLiteRAGRepository(
    context = context,
    embeddingProvider = customEmbeddingProvider,
    enableClustering = true
)
```

### Custom Chunking Strategy

```kotlin
// Configure chunking
val customConfig = ChunkingConfig(
    strategy = ChunkingStrategy.HYBRID,
    maxTokens = 512, // Chunk size
    overlapTokens = 50, // Overlap between chunks
    respectSectionBoundaries = true,
    minChunkTokens = 100
)

// Use in document processor
// (currently internal - will be exposed in future version)
```

### Cluster Tuning

```kotlin
// Fine-tune clustering for your dataset
val repository = SQLiteRAGRepository(
    context = context,
    embeddingProvider = embeddingProvider,
    enableClustering = true,
    clusterCount = 512, // More clusters for very large datasets (default: 256)
    topClusters = 5 // Search more clusters (default: 3)
)
```

**Guidelines:**

| Total Chunks | Recommended Clusters | Top Clusters to Search |
|--------------|---------------------|----------------------|
| < 10,000 | 128 | 2 |
| 10,000 - 100,000 | 256 (default) | 3 (default) |
| 100,000 - 500,000 | 512 | 5 |
| > 500,000 | 1,024 | 7 |

---

### Batch Document Import

```kotlin
// Import multiple documents efficiently
suspend fun importDocumentBatch(filePaths: List<String>) {
    filePaths.forEach { path ->
        repository.addDocument(
            AddDocumentRequest(
                filePath = path,
                processImmediately = false // Don't process yet
            )
        )
    }

    // Process all at once
    val processedCount = repository.processDocuments().getOrThrow()
    Log.i("RAG", "Processed $processedCount documents")

    // Rebuild clusters after batch
    repository.rebuildClusters()
}
```

---

## Troubleshooting Setup

### Issue: Model Not Found

**Error:** `FileNotFoundException: AVA-ONX-384-BASE-INT8.onnx`

**Cause:** Model not in expected location

**Solution:**

```bash
# Check if bundled model is in APK
adb shell ls /data/app/com.augmentalis.ava*/base.apk

# For external models:
adb shell ls /sdcard/Android/data/com.augmentalis.ava/files/models/

# Re-push if missing
adb push ~/ava-models/AVA-ONX-384-BASE-INT8.onnx \
  /sdcard/Android/data/com.augmentalis.ava/files/models/
```

---

### Issue: Document Processing Stuck

**Symptom:** Status shows "PROCESSING" for > 10 minutes

**Solution:**

```bash
# Check logs for errors
adb logcat | grep -E "RAG|Error"

# Common issues:
# 1. Out of memory → Restart app
# 2. Corrupted file → Re-upload document
# 3. Unsupported format → Check file type

# Force restart processing
# (delete and re-add document)
```

---

### Issue: No Search Results

**Symptom:** Search returns empty list

**Checklist:**
1. ✅ Document status is INDEXED (not PENDING/PROCESSING)
2. ✅ Similarity threshold not too high (try 0.5)
3. ✅ Query matches document content
4. ✅ Embeddings generated successfully

**Debug:**

```kotlin
// Check statistics
val stats = repository.getStatistics().getOrThrow()
Log.i("Debug", "Total chunks: ${stats.totalChunks}")

// Try broader search
val results = repository.search(
    SearchQuery(
        query = "device", // Simple query
        maxResults = 20,
        minSimilarity = 0.3f // Lower threshold
    )
)
Log.i("Debug", "Results: ${results.getOrNull()?.results?.size ?: 0}")
```

---

### Issue: Slow Performance

**Symptom:** Search takes > 500ms

**Solution:**

```kotlin
// Rebuild clusters
repository.rebuildClusters()

// Expected improvement:
// Before: 1000ms
// After: 25-50ms (40x faster)
```

---

## Summary

**Setup Checklist:**

- ✅ Verified system requirements
- ✅ Model installed (bundled for English, download for multilingual)
- ✅ Documents added and indexed
- ✅ Clusters rebuilt (for bulk imports)
- ✅ RAG enabled in settings
- ✅ Documents selected
- ✅ First successful chat with sources

**What's Next:**

1. **Add more documents** - Build your knowledge base
2. **Tune settings** - Adjust similarity threshold and max chunks
3. **Test different queries** - Try factual vs conversational
4. **Monitor performance** - Check search latency
5. **Read Performance Tuning Guide** - Optimize for your use case

---

**Author:** AVA AI Documentation Team
**Date:** 2025-11-22
**Phase:** 2.0 - Task 4/4
**Status:** ✅ Complete
