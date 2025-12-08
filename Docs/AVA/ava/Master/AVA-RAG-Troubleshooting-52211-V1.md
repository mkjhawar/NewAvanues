# RAG Troubleshooting Guide

**Date:** 2025-11-22
**Phase:** 2.0 - RAG Integration Documentation
**Audience:** Developers & Support Engineers
**Goal:** Quickly diagnose and resolve common RAG issues

---

## Table of Contents

1. [Common Issues Quick Reference](#common-issues-quick-reference)
2. [Model & Initialization Issues](#model--initialization-issues)
3. [Document Ingestion Problems](#document-ingestion-problems)
4. [Search & Retrieval Issues](#search--retrieval-issues)
5. [Performance Problems](#performance-problems)
6. [Memory & Crash Issues](#memory--crash-issues)
7. [Integration Issues](#integration-issues)
8. [Debug Logging Guide](#debug-logging-guide)
9. [FAQ](#faq)

---

## Common Issues Quick Reference

| Symptom | Likely Cause | Quick Fix | Section |
|---------|--------------|-----------|---------|
| "Model not found" error | Missing ONNX model | Check model location | [#1](#issue-1-model-not-found) |
| Document stuck "PROCESSING" | Memory issue or corrupted file | Restart app, check logs | [#6](#issue-6-document-processing-stuck) |
| Search returns no results | Threshold too high or no matching docs | Lower minSimilarity to 0.5 | [#11](#issue-11-search-returns-no-results) |
| Search very slow (>500ms) | Clustering not enabled | Rebuild clusters | [#16](#issue-16-slow-search-performance) |
| App crashes during indexing | Out of memory | Reduce batch size | [#21](#issue-21-out-of-memory-crash) |
| Sources not showing in chat | Message model missing fields | Update Message model | [#26](#issue-26-sources-not-displayed) |

---

## Model & Initialization Issues

### Issue #1: Model Not Found

**Error:**
```
FileNotFoundException: AVA-ONX-384-BASE-INT8.onnx
```

**Cause:** Model file not in expected location

**Resolution:**

**Step 1: Check bundled model (English only)**
```bash
# For bundled model, check if APK contains it
adb shell pm path com.augmentalis.ava
# Output: package:/data/app/com.augmentalis.ava-xxx/base.apk

# Extract and check assets
# (Bundled model is automatically loaded, no action needed if app is installed)
```

**Step 2: Check external storage (for multilingual/custom models)**
```bash
# List models directory
adb shell ls -la /sdcard/Android/data/com.augmentalis.ava/files/models/

# Expected output:
# drwxrwx--- 2 u0_a123 sdcard_rw 4096 2025-11-22 14:00 .
# -rw-rw---- 1 u0_a123 sdcard_rw 23068672 2025-11-22 14:00 AVA-ONX-384-BASE-INT8.onnx
```

**Step 3: Re-download if missing**
```bash
# Download model
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o ~/AVA-ONX-384-BASE.onnx

# Quantize (optional but recommended)
python3 scripts/required/quantize-models.py \
  ~/AVA-ONX-384-BASE.onnx \
  ~/AVA-ONX-384-BASE-INT8.onnx \
  int8

# Push to device
adb push ~/AVA-ONX-384-BASE-INT8.onnx \
  /sdcard/Android/data/com.augmentalis.ava/files/models/
```

**Step 4: Verify permissions**
```bash
# Check directory permissions
adb shell ls -ld /sdcard/Android/data/com.augmentalis.ava/files/models/

# Should show: drwxrwx--- (directory is readable)
# If not, recreate directory:
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/
```

---

### Issue #2: ONNX Runtime Initialization Failed

**Error:**
```
OrtException: Failed to load ONNX model
```

**Cause:** ONNX Runtime incompatibility or corrupted model

**Resolution:**

**Step 1: Check ONNX Runtime version**
```kotlin
// In build.gradle.kts
dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.0") // Ensure latest
}
```

**Step 2: Verify model integrity**
```bash
# Check file size (should be ~22 MB for AVA-ONX-384-BASE-INT8)
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/AVA-ONX-384-BASE-INT8.onnx

# If size is wrong, re-download
```

**Step 3: Check device architecture**
```bash
# Get device ABI
adb shell getprop ro.product.cpu.abi

# ONNX Runtime supports:
# - armeabi-v7a (ARM 32-bit)
# - arm64-v8a (ARM 64-bit) ← Most common
# - x86 (Intel 32-bit)
# - x86_64 (Intel 64-bit)
```

**Step 4: Enable verbose logging**
```kotlin
val ortEnv = OrtEnvironment.getEnvironment(OrtLoggingLevel.ORT_LOGGING_LEVEL_VERBOSE)
val ortSession = ortEnv.createSession(modelPath)
```

---

### Issue #3: Wrong Model Dimensions

**Error:**
```
IllegalStateException: Expected 384 dimensions, got 768
```

**Cause:** Model ID mismatch

**Resolution:**

**Check model configuration:**
```kotlin
// Ensure model ID matches actual model
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-384-BASE-INT8" // 384 dimensions
)

// If using 768-dim model:
modelId = "AVA-ONX-768-QUAL-INT8" // 768 dimensions
```

**Verify model mapping:**
```kotlin
// In ONNXEmbeddingProvider
private val modelIdMap = mapOf(
    "AVA-ONX-384-BASE-INT8" to ModelConfig(dims = 384, file = "AVA-ONX-384-BASE-INT8.onnx"),
    "AVA-ONX-768-QUAL-INT8" to ModelConfig(dims = 768, file = "AVA-ONX-768-QUAL-INT8.onnx")
)
```

---

## Document Ingestion Problems

### Issue #6: Document Processing Stuck

**Symptom:** Status shows "PROCESSING" for >10 minutes

**Diagnosis:**

**Step 1: Check logs**
```bash
adb logcat | grep -E "RAG|Error|OutOfMemory"

# Look for:
# - OutOfMemoryError
# - "Failed to parse document"
# - "Embedding generation timed out"
```

**Step 2: Check document status**
```kotlin
val stats = repository.getStatistics().getOrThrow()
Log.i("Debug", "Processing: ${stats.totalDocuments - stats.indexedDocuments}")
```

**Resolutions:**

**A. Out of Memory**
```kotlin
// Reduce batch size
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    batchSize = 16 // Reduced from 32
)
```

**B. Corrupted Document**
```bash
# Pull document from device
adb pull /sdcard/Download/problem_document.pdf ~/

# Try opening on computer
# If corrupted, re-download or fix source
```

**C. Force Restart**
```kotlin
// Delete and re-add document
repository.deleteDocument(documentId)
repository.addDocument(AddDocumentRequest(filePath = ...))
```

**D. Database Lock**
```bash
# Check if database is locked
adb shell "lsof | grep rag_database"

# Force close app if necessary
adb shell am force-stop com.augmentalis.ava

# Restart app
adb shell am start -n com.augmentalis.ava/.MainActivity
```

---

### Issue #7: PDF Parsing Failed

**Error:**
```
IOException: Failed to parse PDF document
```

**Causes & Resolutions:**

**A. Encrypted/Password-Protected PDF**
```bash
# Check if PDF is encrypted
qpdf --show-encryption problem.pdf

# If encrypted, remove password:
qpdf --password=PASSWORD --decrypt problem.pdf unlocked.pdf

# Re-upload unlocked PDF
adb push unlocked.pdf /sdcard/Download/
```

**B. Scanned PDF (Images Only)**
```
Issue: PDF contains scanned images, no extractable text
Solution: Use OCR tool first
  - Adobe Acrobat: Tools > Recognize Text
  - Online: smallpdf.com/pdf-ocr
  - CLI: tesseract (Google OCR)
```

**C. Corrupted PDF**
```bash
# Try repair with qpdf
qpdf --check problem.pdf

# Repair
qpdf problem.pdf repaired.pdf

# Re-upload
adb push repaired.pdf /sdcard/Download/
```

---

### Issue #8: DOCX Parsing Failed

**Error:**
```
ZipException: error in opening zip file
```

**Cause:** Corrupted DOCX file (DOCX is a ZIP archive)

**Resolution:**

**Step 1: Verify file integrity**
```bash
# DOCX is a ZIP file
unzip -t problem.docx

# If errors, try repair:
zip -FF problem.docx --out repaired.docx
```

**Step 2: Convert to supported format**
```
If repair fails:
1. Open in Microsoft Word / LibreOffice
2. Save As → PDF or RTF
3. Use that format instead
```

---

### Issue #9: Web Document Import Timeout

**Error:**
```
SocketTimeoutException: Read timed out
```

**Cause:** Slow network or large web page

**Resolution:**

**Step 1: Increase timeout**
```kotlin
// In HtmlParser
private val HTTP_TIMEOUT = 30_000L // Increase from 15s to 30s

val connection = Jsoup.connect(url)
    .timeout(HTTP_TIMEOUT.toInt())
    .get()
```

**Step 2: Download locally first**
```bash
# Download HTML manually
curl -L "https://example.com/large-page.html" -o page.html

# Push to device
adb push page.html /sdcard/Download/

# Import as local file
repository.addDocument(
    AddDocumentRequest(
        filePath = "/sdcard/Download/page.html",
        documentType = DocumentType.HTML
    )
)
```

**Step 3: Check network connectivity**
```bash
# Test from device
adb shell "ping -c 4 example.com"

# If no internet, enable WiFi/mobile data
```

---

### Issue #10: Chunking Generated Too Many/Few Chunks

**Symptom:** 100-page document generates 10 chunks (too few) or 10,000 chunks (too many)

**Diagnosis:**

**Expected:** ~3 chunks per page (for 512-token chunks)

```
100 pages → ~300 chunks (normal)
100 pages → 10 chunks (TOO FEW - check parsing)
100 pages → 10,000 chunks (TOO MANY - check chunking config)
```

**Resolutions:**

**A. Too Few Chunks**
```kotlin
// Check if document was parsed correctly
val parsedDoc = parser.parse(filePath, DocumentType.PDF).getOrThrow()
Log.i("Debug", "Extracted text length: ${parsedDoc.text.length}")

// If length is very small, parsing failed
// → Check Issue #7 or #8
```

**B. Too Many Chunks**
```kotlin
// Check chunking configuration
val config = ChunkingConfig(
    maxTokens = 512, // Increase if too many chunks
    minChunkTokens = 100 // Increase minimum
)
```

---

## Search & Retrieval Issues

### Issue #11: Search Returns No Results

**Symptom:** Query returns empty list despite having documents

**Diagnosis Steps:**

**Step 1: Verify documents are indexed**
```kotlin
val stats = repository.getStatistics().getOrThrow()
Log.i("Debug", "Total documents: ${stats.totalDocuments}")
Log.i("Debug", "Indexed documents: ${stats.indexedDocuments}")
Log.i("Debug", "Total chunks: ${stats.totalChunks}")

// If totalChunks = 0, documents weren't processed
```

**Step 2: Check similarity threshold**
```kotlin
// Try with lower threshold
val results = repository.search(
    SearchQuery(
        query = "device",
        maxResults = 20,
        minSimilarity = 0.3f // Lower from default 0.7
    )
)

Log.i("Debug", "Results: ${results.getOrNull()?.results?.size ?: 0}")
```

**Step 3: Test with simple query**
```kotlin
// Try very generic query
val results = repository.search(
    SearchQuery(
        query = "the",
        maxResults = 10,
        minSimilarity = 0.1f
    )
)

// If this returns results, original query was too specific
```

**Resolutions:**

**A. Threshold Too High**
```kotlin
// Lower threshold in settings
chatPreferences.setRAGSimilarity(0.6f) // Down from 0.7
```

**B. Query Doesn't Match Content**
```
Issue: User asks "How to reset?", docs say "How to factory restore?"
Solution: Try synonyms or rephrase query
  - "reset" → "restore", "reboot", "restart"
  - Use broader terms
```

**C. Embeddings Not Generated**
```kotlin
// Check if chunks have embeddings
val chunks = chunkDao.getChunksByDocument(documentId)
chunks.forEach { chunk ->
    if (chunk.embeddingBlob == null) {
        Log.w("Debug", "Chunk ${chunk.id} missing embedding!")
    }
}

// If embeddings missing, re-process document
repository.processDocuments(documentId)
```

---

### Issue #12: Search Results Not Relevant

**Symptom:** Top results have low similarity scores (< 0.5)

**Diagnosis:**

```kotlin
val results = repository.search(SearchQuery(query = "reset device")).getOrThrow()

results.results.forEach { result ->
    Log.i("Debug", "Similarity: ${result.similarity}")
    Log.i("Debug", "Content: ${result.chunk.content.take(100)}")
}

// If all similarities < 0.5, embedding model mismatch
```

**Resolutions:**

**A. Model Language Mismatch**
```
Issue: Using English model on Chinese documents
Solution: Use multilingual model
  - Download AVA-ONX-384-MULTI-INT8
  - Configure in settings
```

**B. Embedding Dimension Mismatch**
```kotlin
// Verify consistent dimensions
val provider = ONNXEmbeddingProvider(context, modelId = "AVA-ONX-384-BASE-INT8")
val testEmbedding = provider.embed("test").getOrThrow()

Log.i("Debug", "Embedding dims: ${testEmbedding.values.size}") // Should be 384

// Check database
val chunk = chunkDao.getChunkById("chunk_1")
val storedDims = chunk.embeddingDimension

if (storedDims != testEmbedding.values.size) {
    Log.e("Debug", "Dimension mismatch! Stored: $storedDims, Current: ${testEmbedding.values.size}")
    // Re-index all documents with correct model
}
```

**C. Content-Query Mismatch**
```
Issue: Documents are technical, query is conversational
Example:
  - Query: "my thing won't turn on"
  - Doc: "Device power initialization failure diagnostic procedure"

Solution: Rephrase query to match document style
  - Try: "device power failure troubleshooting"
```

---

### Issue #13: Cluster Search Not Working

**Symptom:** Clustering enabled but search still slow

**Diagnosis:**

```kotlin
val stats = repository.getStatistics().getOrThrow()
Log.i("Debug", "Cluster count: ${stats.clusterCount}")

// If clusterCount = 0, clustering not built
```

**Resolution:**

```kotlin
// Rebuild clusters
val clusterStats = repository.rebuildClusters().getOrThrow()

Log.i("Debug", "Rebuilt ${clusterStats.clusterCount} clusters")
Log.i("Debug", "Clustered ${clusterStats.chunkCount} chunks")
Log.i("Debug", "Took ${clusterStats.timeMs / 1000}s")
```

---

## Performance Problems

### Issue #16: Slow Search Performance

**Symptom:** Search takes > 500ms (target: < 50ms)

**Diagnosis:**

**Step 1: Measure latency**
```kotlin
val start = System.currentTimeMillis()
val results = repository.search(SearchQuery(query = "test"))
val end = System.currentTimeMillis()

Log.i("Perf", "Search took ${end - start}ms")
```

**Step 2: Check cluster status**
```kotlin
val stats = repository.getStatistics().getOrThrow()
Log.i("Perf", "Clusters: ${stats.clusterCount}")
Log.i("Perf", "Total chunks: ${stats.totalChunks}")

// Without clusters: 200k chunks = ~1000ms
// With clusters: 200k chunks = ~25-50ms
```

**Resolutions:**

**A. Clustering Not Enabled**
```kotlin
// Enable clustering
val repository = SQLiteRAGRepository(
    context = context,
    embeddingProvider = embeddingProvider,
    enableClustering = true, // CRITICAL
    clusterCount = 256,
    topClusters = 3
)

// Rebuild clusters
repository.rebuildClusters()
```

**B. Too Many Clusters Searched**
```kotlin
// Reduce topClusters for faster search
val repository = SQLiteRAGRepository(
    // ...
    topClusters = 2 // Down from 3 or 5
)
```

**C. Database Not Indexed**
```sql
-- Add indices
CREATE INDEX IF NOT EXISTS idx_chunks_cluster_id ON chunks(cluster_id);
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON chunks(document_id);
```

---

### Issue #17: Slow Document Indexing

**Symptom:** Processing 100-page PDF takes > 5 minutes

**Expected:** 100-page PDF = ~1.5 minutes (parallel mode)

**Diagnosis:**

```bash
adb logcat | grep "RAG" | grep -E "Parsing|Chunking|Embedding"

# Check which stage is slow:
# - Parsing: ~30 sec
# - Chunking: ~5 sec
# - Embedding: ~60 sec
# - Storage: ~5 sec
```

**Resolutions:**

**A. Use Faster Document Format**
```
PDF (100 pages) = ~1.5 min
DOCX (100 pages) = ~30 sec  ← 3x faster!

Recommendation: Convert PDF → DOCX if possible
```

**B. Increase Batch Size** (if device has memory)
```kotlin
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    batchSize = 64 // Up from 32 (requires 6GB+ RAM)
)
```

**C. Disable Parallel Processing** (if crashes occur)
```kotlin
// Fallback to sequential processing
val processor = SequentialRAGProcessor(...) // Instead of ParallelRAGProcessor
```

---

## Memory & Crash Issues

### Issue #21: Out of Memory Crash

**Error:**
```
OutOfMemoryError: Failed to allocate XXXX bytes
```

**Cause:** Large batch size or too many chunks in memory

**Resolutions:**

**A. Reduce Embedding Batch Size**
```kotlin
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    batchSize = 16 // Down from 32
)
```

**B. Enable Large Heap**
```xml
<!-- AndroidManifest.xml -->
<application
    android:largeHeap="true"
    ...>
```

**C. Process Documents One at a Time**
```kotlin
// Don't process all documents at once
documents.forEach { doc ->
    repository.addDocument(
        AddDocumentRequest(
            filePath = doc,
            processImmediately = true
        )
    )
    // Wait for completion before next
    delay(1000)
}
```

**D. Clear Cache Regularly**
```kotlin
// After processing
System.gc()
```

---

### Issue #22: App Freezes During Clustering

**Symptom:** UI becomes unresponsive when rebuilding clusters

**Cause:** Clustering runs on main thread or blocks UI

**Resolution:**

```kotlin
// Ensure clustering runs in background
lifecycleScope.launch(Dispatchers.IO) {
    repository.rebuildClusters()

    withContext(Dispatchers.Main) {
        // Update UI
        showToast("Clustering complete!")
    }
}
```

---

## Integration Issues

### Issue #26: Sources Not Displayed

**Symptom:** RAG chat works but no sources shown in UI

**Diagnosis:**

**Step 1: Check message model**
```kotlin
// Verify Message has sources field
data class Message(
    // ... existing fields
    val sources: List<MessageSource>? = null // Should be present
)
```

**Step 2: Check ViewModel populates sources**
```kotlin
// In ChatViewModel
updateMessage(
    assistantMessage.copy(
        content = fullResponse,
        sources = sources, // CRITICAL - must be set
        isStreaming = false
    )
)
```

**Step 3: Check UI displays sources**
```kotlin
@Composable
fun MessageBubble(message: Message) {
    // ...

    message.sources?.let { sources ->
        if (sources.isNotEmpty()) {
            SourcesSection(sources = sources) // Should be present
        }
    }
}
```

**Resolution:**

If any component missing, add it per [RAG-ChatViewModel-Integration.md](#).

---

### Issue #27: RAG Not Triggering

**Symptom:** RAG enabled but always uses standard LLM flow

**Diagnosis:**

```kotlin
// Add debug logging
override fun sendMessage(text: String) {
    viewModelScope.launch {
        val useRAG = shouldUseRAG(text)
        Log.d(TAG, "RAG enabled: ${_ragEnabled.value}")
        Log.d(TAG, "Should use RAG: $useRAG")

        if (useRAG) {
            Log.i(TAG, "Using RAG pathway")
            sendMessageWithRAG(text)
        } else {
            Log.i(TAG, "Using standard pathway")
            sendMessageStandard(text)
        }
    }
}
```

**Check logs:**
```bash
adb logcat | grep "ChatViewModel"

# Look for:
# - "RAG enabled: false" → Enable in settings
# - "Should use RAG: false" → Check decision logic
```

**Resolutions:**

**A. RAG Not Enabled**
```kotlin
chatPreferences.setRAGEnabled(true)
```

**B. No Documents Selected**
```kotlin
// Select documents
val documents = repository.listDocuments(status = DocumentStatus.INDEXED).toList()
chatPreferences.setRAGDocuments(documents.map { it.id })
```

**C. Decision Logic Blocks RAG**
```kotlin
// Check shouldUseRAG() logic
private suspend fun shouldUseRAG(query: String): Boolean {
    if (!_ragEnabled.value) {
        Log.d(TAG, "RAG not enabled")
        return false
    }

    val hasDocuments = repository.getStatistics().getOrNull()?.indexedDocuments ?: 0 > 0
    if (!hasDocuments) {
        Log.d(TAG, "No documents indexed")
        return false
    }

    // Check if query is factual
    val isFactual = query.contains("?") || query.lowercase().startsWith("how")
    Log.d(TAG, "Query is factual: $isFactual")

    return isFactual
}
```

---

## Debug Logging Guide

### Enable Verbose Logging

```kotlin
// Set log level
val TAG = "RAG"

// In relevant classes
Log.v(TAG, "Verbose debug message")
Log.d(TAG, "Debug message")
Log.i(TAG, "Info message")
Log.w(TAG, "Warning message")
Log.e(TAG, "Error message", exception)
```

### Key Log Tags

```bash
# Filter by tag
adb logcat RAG:D *:S         # Only RAG debug+ messages
adb logcat ChatViewModel:D *:S # Only ChatViewModel debug+ messages
adb logcat ONNX:D *:S        # Only ONNX debug+ messages

# Combined
adb logcat RAG:D ChatViewModel:D ONNX:D *:S
```

### Useful Log Searches

```bash
# Search errors
adb logcat | grep -i "error"

# Search RAG operations
adb logcat | grep -E "RAG.*search|RAG.*index|RAG.*cluster"

# Search performance
adb logcat | grep -E "took.*ms|latency|performance"

# Search memory
adb logcat | grep -E "OutOfMemory|GC_|memory"
```

### Performance Logging

```kotlin
// Log with timing
fun <T> measureTime(tag: String, operation: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    val end = System.currentTimeMillis()
    Log.i(tag, "$operation took ${end - start}ms")
    return result
}

// Usage
val results = measureTime("RAG", "Search") {
    repository.search(SearchQuery(query = "test"))
}
```

---

## FAQ

### Q1: How do I reset the RAG database?

**A:** Clear app data or delete database manually:

```bash
# Option 1: Clear app data (removes everything)
adb shell pm clear com.augmentalis.ava

# Option 2: Delete database only
adb shell rm /data/data/com.augmentalis.ava/databases/rag_database*
```

---

### Q2: Can I use multiple models simultaneously?

**A:** No, only one embedding model at a time. If you switch models, you must re-index all documents.

```kotlin
// Switching models requires re-indexing
chatPreferences.setEmbeddingModel("AVA-ONX-384-MULTI-INT8")

// Re-process all documents
repository.listDocuments().collect { doc ->
    repository.processDocuments(doc.id)
}
```

---

### Q3: How do I export/import RAG data?

**A:** Use adb to backup/restore database and models:

```bash
# Export
adb pull /data/data/com.augmentalis.ava/databases/rag_database ~/backup/
adb pull /sdcard/Android/data/com.augmentalis.ava/files/models/ ~/backup/models/

# Import
adb push ~/backup/rag_database /data/data/com.augmentalis.ava/databases/
adb push ~/backup/models/ /sdcard/Android/data/com.augmentalis.ava/files/
```

---

### Q4: What's the maximum document size?

**A:**
- **Recommended:** < 500 pages per document
- **Maximum:** ~2,000 pages (limited by memory, not file size)
- **Workaround:** Split large documents into multiple files

---

### Q5: Can I use RAG offline?

**A:** Yes! RAG is 100% offline after model and documents are downloaded.

- ✅ Model: Bundled or downloaded once
- ✅ Documents: Indexed locally
- ✅ Search: All local
- ❌ Web document import: Requires internet

---

### Q6: How do I monitor RAG performance in production?

**A:** Implement performance monitoring:

```kotlin
class RAGAnalytics {
    fun logSearchLatency(latencyMs: Long) {
        // Send to analytics backend
        Firebase.analytics.logEvent("rag_search_latency") {
            param("latency_ms", latencyMs)
        }
    }

    fun logIndexingTime(documentType: String, pageCount: Int, timeMs: Long) {
        Firebase.analytics.logEvent("rag_indexing") {
            param("document_type", documentType)
            param("page_count", pageCount)
            param("time_ms", timeMs)
        }
    }
}
```

---

## Summary

**Most Common Issues:**

1. **Model not found** → Check bundled model or re-download
2. **Document processing stuck** → Check logs for OOM, restart app
3. **No search results** → Lower similarity threshold
4. **Slow search** → Rebuild clusters
5. **Out of memory** → Reduce batch size
6. **Sources not showing** → Update Message model

**Debug Workflow:**

1. Check logs: `adb logcat | grep RAG`
2. Verify statistics: `repository.getStatistics()`
3. Test with simple query: `query="test", minSimilarity=0.1`
4. Check clustering: `clusterCount > 0`
5. Monitor memory: `adb shell dumpsys meminfo com.augmentalis.ava`

**Get Help:**

- **Documentation:** `docs/RAG-*.md`
- **Logs:** `adb logcat RAG:D *:S`
- **Statistics:** `repository.getStatistics()`
- **GitHub Issues:** [github.com/augmentalis/ava/issues](https://github.com/augmentalis/ava/issues)

---

**Author:** AVA AI Documentation Team
**Date:** 2025-11-22
**Phase:** 2.0 - Task 4/4
**Status:** ✅ Complete
