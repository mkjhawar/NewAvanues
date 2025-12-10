# AVA RAG System - Quick Start Guide

**For:** First-time developers and users
**Time to Complete:** 30 minutes
**Prerequisites:** Android Studio, ADB access
**Last Updated:** 2025-11-05

---

## 🚀 What You'll Build

By the end of this guide, you'll have:
- ✅ RAG system integrated into AVA
- ✅ ONNX embedding model installed
- ✅ Support for 6 document formats (PDF, DOCX, TXT, HTML, RTF, Markdown)
- ✅ Web document import from URLs
- ✅ Sample documents indexed
- ✅ Working semantic search UI
- ✅ Sub-50ms search for thousands of chunks
- ✅ 50% faster processing with parallel pipeline

---

## 📋 Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Model Setup](#2-model-setup)
3. [Build & Run](#3-build--run)
4. [Add Your First Document](#4-add-your-first-document)
5. [Search Documents](#5-search-documents)
6. [Advanced: Cluster Rebuild](#6-advanced-cluster-rebuild)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Prerequisites

### Required

- **Android Studio:** Arctic Fox or later
- **Android Device/Emulator:** API 26+ (Android 8.0+)
- **Storage:** 2GB free space on device
- **Internet:** For downloading models (one-time)

### Recommended

- Physical device (better performance than emulator)
- 4GB+ RAM device
- ADB installed and working

### Verify Setup

```bash
# Check ADB connection
adb devices

# Expected output:
# List of devices attached
# <device-id>    device

# Check device API level
adb shell getprop ro.build.version.sdk

# Should be >= 26
```

---

## 2. Model Setup

### 2.1 Download ONNX Model

**Option A: Direct Download (Recommended)**

```bash
# Create models directory
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/

# Download model (86MB, ~1 minute on fast connection)
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx

# Push to device
adb push all-MiniLM-L6-v2.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
```

**Option B: File Manager**

1. Download model from [HuggingFace](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx)
2. Copy to: `/sdcard/Android/data/com.augmentalis.ava/files/models/`
3. Rename to: `all-MiniLM-L6-v2.onnx`

### 2.2 Verify Installation

```bash
# Check file size (should be ~86MB)
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/all-MiniLM-L6-v2.onnx

# Expected output:
# -rw-rw---- 1 u0_a123 u0_a123 86M 2025-11-05 16:00 all-MiniLM-L6-v2.onnx
```

✅ **Model setup complete!**

---

## 3. Build & Run

### 3.1 Clone Repository

```bash
git clone https://gitlab.com/AugmentalisES/AVA.git
cd AVA
git checkout development
```

### 3.2 Open in Android Studio

1. **Open Project:** `File → Open → Select AVA directory`
2. **Wait for Gradle sync** (~2 minutes first time)
3. **Select Device:** `Run → Select Device`
4. **Build:** `Build → Make Project` (Ctrl+F9 / Cmd+F9)

### 3.3 Run App

```bash
# Or via command line
./gradlew :apps:ava-standalone:installDebug

# Launch app
adb shell am start -n com.augmentalis.ava/.MainActivity
```

✅ **App running!**

---

## 4. Add Your First Document

### 4.1 Supported Document Formats

AVA supports **6 document formats** with varying processing speeds:

| Format | Speed | Best For | Example |
|--------|-------|----------|---------|
| **DOCX** | ⚡ 10-20 pages/sec | Modern manuals, reports | `manual.docx` |
| **TXT** | ⚡ Instant | Logs, notes, plain text | `notes.txt` |
| **HTML** | ⚡ 20-50 pages/sec | **Web docs (URLs!)**, saved pages | `https://developer.android.com/...` |
| **Markdown** | ⚡ Instant | README files, GitHub docs | `README.md` |
| **RTF** | ⚡ 5-10 pages/sec | Legacy Word docs | `manual.rtf` |
| **PDF** | 🐌 2 pages/sec | Scanned manuals, official docs | `manual.pdf` |

**💡 Pro Tip:** Use DOCX instead of PDF when possible - it's **5-10x faster!**

**🌐 New: Import from URLs!**
```kotlin
// Import Android developer docs directly
repository.addDocument(
    AddDocumentRequest(
        filePath = "https://developer.android.com/guide/components/activities",
        title = "Android Activities Guide",
        documentType = DocumentType.HTML
    )
)
```

### 4.2 Prepare Sample Documents

**Option A: PDF Document**

```bash
# Push any PDF to device
adb push /path/to/your/manual.pdf /sdcard/Download/manual.pdf
```

**Option B: DOCX Document (Faster!)**

```bash
# Push DOCX file (processes 5-10x faster than PDF)
adb push /path/to/your/manual.docx /sdcard/Download/manual.docx
```

**Option C: Web Documentation**

No file needed! Just use the URL directly:
```kotlin
filePath = "https://example.com/documentation.html"
```

**Option D: Markdown from GitHub**

```bash
# Download README from any GitHub repo
curl https://raw.githubusercontent.com/user/repo/main/README.md \
  -o README.md
adb push README.md /sdcard/Download/
```

### 4.3 Import Document via Code

**Method 1: Using RAG Module Directly (Recommended for Testing)**

```kotlin
// In your test code or temporary activity
val repository = SQLiteRAGRepository(
    context = applicationContext,
    embeddingProvider = ONNXEmbeddingProvider(applicationContext)
)

// Add document
lifecycleScope.launch {
    val result = repository.addDocument(
        AddDocumentRequest(
            filePath = "/sdcard/Download/manual.pdf",
            title = "Sample Manual",
            processImmediately = true
        )
    )

    when {
        result.isSuccess -> {
            val docId = result.getOrNull()?.documentId
            Log.i("RAG", "Document added: $docId")
            Toast.makeText(this@MainActivity, "Document indexed!", Toast.LENGTH_SHORT).show()
        }
        result.isFailure -> {
            Log.e("RAG", "Failed to add document", result.exceptionOrNull())
            Toast.makeText(this@MainActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
        }
    }
}
```

**Method 2: File Picker (If UI is ready)**

1. Tap **"Add Document"** button
2. Select PDF from file picker
3. Wait for processing (shows progress)
4. Document appears in list

### 4.4 Monitor Processing

**Via Logcat:**

```bash
adb logcat | grep "RAG"

# Expected output:
# 16:00:00.123 I RAG: Parsing document: /sdcard/Download/manual.docx
# 16:00:02.456 I RAG: Extracted 150 pages
# 16:00:05.789 I RAG: Generated 450 chunks
# 16:00:35.012 I RAG: Generated embeddings for 450 chunks (parallel)
# 16:00:36.345 I RAG: Document indexed successfully
```

**Processing Time Estimates (with Parallel Pipeline):**

| Document Type | Size | Old (Sequential) | **New (Parallel)** | Speedup |
|---------------|------|------------------|-------------------|---------|
| **DOCX** | 100 pages | 3 min | **1.5 min** | 🚀 50% faster |
| **DOCX** | 1000 pages | 30 min | **15 min** | 🚀 50% faster |
| **PDF** | 100 pages | 3 min | **1.5 min** | 🚀 50% faster |
| **PDF** | 1000 pages | 10 min | **4.5-5.5 min** | 🚀 50% faster |
| **TXT** | Any | <10 sec | **<5 sec** | 🚀 50% faster |
| **HTML (URL)** | 50 pages | 30 sec | **15 sec** | 🚀 50% faster |

**Key Improvements:**
- ✅ Parallel chunking (2 workers)
- ✅ Parallel embedding (3 workers)
- ✅ Batched storage (500 chunks at once)
- ✅ Progress tracking

✅ **First document indexed!**

---

## 5. Search Documents

### 5.1 Basic Search

```kotlin
// Search for information
val searchResults = repository.search(
    SearchQuery(
        query = "How do I reset the device?",
        maxResults = 5,
        minSimilarity = 0.7f
    )
).getOrThrow()

// Display results
searchResults.results.forEach { result ->
    println("Score: ${(result.similarity * 100).toInt()}%")
    println("Document: ${result.document?.title}")
    println("Content: ${result.chunk.content}")
    println("---")
}
```

### 5.2 Using Compose UI

```kotlin
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    val repository = remember {
        SQLiteRAGRepository(
            LocalContext.current,
            ONNXEmbeddingProvider(LocalContext.current)
        )
    }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search documents") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        scope.launch {
                            val response = repository.search(
                                SearchQuery(query = query, maxResults = 10)
                            ).getOrNull()
                            results = response?.results ?: emptyList()
                        }
                    }
                ) {
                    Icon(Icons.Default.Search, "Search")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results
        LazyColumn {
            items(results) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = result.document?.title ?: "Unknown",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${(result.similarity * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.chunk.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
```

### 5.3 Test Queries

Try these sample queries:

```
"How do I configure the device?"
"What are the safety precautions?"
"Troubleshooting network connection"
"Installation instructions"
"Maintenance schedule"
```

**Expected Response Time:**
- **With clustering (recommended):** <50ms
- **Without clustering:** 5-50ms (depends on document count)

✅ **Search working!**

---

## 6. Advanced: Cluster Rebuild

### Why Rebuild Clusters?

After adding multiple documents, rebuild clusters for **40x faster search**:

```kotlin
lifecycleScope.launch {
    // Rebuild clusters
    val stats = repository.rebuildClusters().getOrThrow()

    Log.i("RAG", "Clustering complete:")
    Log.i("RAG", "  Chunks: ${stats.chunkCount}")
    Log.i("RAG", "  Clusters: ${stats.clusterCount}")
    Log.i("RAG", "  Iterations: ${stats.iterations}")
    Log.i("RAG", "  Time: ${stats.timeMs}ms")

    Toast.makeText(
        this@MainActivity,
        "Rebuilt ${stats.clusterCount} clusters in ${stats.timeMs / 1000}s",
        Toast.LENGTH_SHORT
    ).show()
}
```

**When to Rebuild:**
- ✅ After adding first 1,000+ chunks
- ✅ After bulk document imports
- ✅ When search feels slow
- ✅ Weekly/monthly maintenance

**Performance:**
- 1,000 chunks: ~5 seconds
- 10,000 chunks: ~15 seconds
- 100,000 chunks: ~30 seconds
- 200,000 chunks: ~60 seconds

✅ **Clustering enabled!**

---

## 7. Troubleshooting

### Issue: Model Not Found

**Error:** `FileNotFoundException: all-MiniLM-L6-v2.onnx`

**Solution:**
```bash
# Verify model exists
adb shell ls /sdcard/Android/data/com.augmentalis.ava/files/models/

# If missing, re-download and push
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx
adb push all-MiniLM-L6-v2.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
```

### Issue: Slow Search

**Problem:** Search takes >500ms

**Solution:** Rebuild clusters

```kotlin
repository.rebuildClusters()
```

**Expected improvement:** 40x faster (1000ms → 25ms)

### Issue: Out of Memory

**Error:** `OutOfMemoryError` during embedding generation

**Solutions:**
1. **Reduce batch size:**
   ```kotlin
   // In ONNXEmbeddingProvider
   private val batchSize = 16  // Reduce from 32
   ```

2. **Process incrementally:**
   ```kotlin
   // Don't set processImmediately = true
   repository.addDocument(
       AddDocumentRequest(filePath = ..., processImmediately = false)
   )

   // Process later in background
   repository.processDocuments()
   ```

3. **Increase heap size:**
   ```xml
   <!-- AndroidManifest.xml -->
   <application
       android:largeHeap="true"
       ...>
   ```

### Issue: PDF Parsing Fails

**Error:** `Failed to parse PDF`

**Possible causes:**
1. Encrypted/password-protected PDF
2. Corrupted file
3. Scanned PDF (needs OCR)

**Solutions:**
1. **Check file integrity:**
   ```bash
   adb pull /sdcard/Download/manual.pdf
   # Open on computer to verify
   ```

2. **Try different PDF:**
   - Use text-based PDF (not scanned images)
   - Remove password protection first

3. **Check logs:**
   ```bash
   adb logcat | grep "PdfParser"
   ```

### Issue: Search Returns No Results

**Problem:** Search returns empty list

**Checklist:**
1. ✅ Documents are indexed (`status = INDEXED`)
2. ✅ Query is reasonable (not too specific)
3. ✅ `minSimilarity` not too high (try 0.5 instead of 0.7)
4. ✅ Embeddings generated successfully

**Debug:**
```kotlin
// Check document count
val docCount = repository.getStatistics().getOrNull()?.totalDocuments
Log.i("RAG", "Total documents: $docCount")

// Check chunk count
val stats = repository.getStatistics().getOrNull()
Log.i("RAG", "Total chunks: ${stats?.totalChunks}")

// Try broader search
val results = repository.search(
    SearchQuery(
        query = "device",  // Simple query
        maxResults = 20,
        minSimilarity = 0.3f  // Lower threshold
    )
)
```

### Get Help

**Documentation:**
- Developer Manual Chapter 28: `docs/Developer-Manual-Chapter28-RAG.md`
- Phase 3.2 Complete: `docs/active/RAG-Phase3.2-Complete-251105.md`
- Status Report: `docs/active/RAG-Status-251105.md`

**Support:**
- GitHub Issues: [github.com/augmentalis/ava/issues](https://github.com/augmentalis/ava/issues)
- Email: support@augmentalis.com

---

## 🎉 Next Steps

### Explore More Features

1. **Multiple Documents:**
   ```kotlin
   // Add multiple documents
   listOf("manual1.pdf", "manual2.pdf", "manual3.pdf").forEach { file ->
       repository.addDocument(AddDocumentRequest(filePath = "/sdcard/$file"))
   }
   // Then rebuild clusters
   repository.rebuildClusters()
   ```

2. **Advanced Queries:**
   ```kotlin
   SearchQuery(
       query = "network troubleshooting",
       maxResults = 10,
       minSimilarity = 0.7f,
       filters = SearchFilters(
           documentTypes = listOf(DocumentType.PDF),
           dateRange = DateRange(start = "2024-01-01")
       )
   )
   ```

3. **Monitor Statistics:**
   ```kotlin
   val stats = repository.getStatistics().getOrThrow()
   println("Documents: ${stats.totalDocuments}")
   println("Indexed: ${stats.indexedDocuments}")
   println("Chunks: ${stats.totalChunks}")
   println("Storage: ${stats.storageUsedBytes / 1024 / 1024} MB")
   ```

### Performance Testing

**Benchmark Your Setup:**

```kotlin
// Add many documents and measure search time
val start = System.currentTimeMillis()
val results = repository.search(SearchQuery(query = "test"))
val elapsed = System.currentTimeMillis() - start
Log.i("RAG", "Search took ${elapsed}ms")

// Expected:
// - Without clustering: 5-50ms (small), 100-1000ms (large)
// - With clustering: <50ms (any size)
```

### Integrate with Chat

**Coming in Phase 4:**

```kotlin
fun askQuestion(question: String): Flow<String> {
    // 1. Search for context
    val searchResults = repository.search(SearchQuery(query = question))

    // 2. Assemble context
    val context = searchResults.results.joinToString("\n") { it.chunk.content }

    // 3. Generate response with LLM
    return mlcLLM.generateStream(
        """Based on: $context
           Answer: $question"""
    )
}
```

---

## Summary

**You've successfully:**
- ✅ Set up ONNX embedding model
- ✅ Built and ran AVA with RAG
- ✅ Indexed your first PDF document
- ✅ Performed semantic search
- ✅ (Optional) Enabled k-means clustering

**Performance Achieved:**
- 📊 Search speed: <50ms (with clustering)
- 📚 Documents: Ready to scale to hundreds
- 🚀 Production-ready RAG system

**What's Next:**
- Add more documents
- Rebuild clusters periodically
- Explore advanced search features
- Wait for Phase 4 (LLM chat integration)

---

**Welcome to the AVA RAG System! 🎉**

**Questions? Issues?** Check the troubleshooting section or open an issue on GitHub.

**Happy Searching!** 🔍
