# AVA RAG Module

**Status:** Phase 2 - Document Processing (80% Complete)
**Version:** 0.2.0
**Last Updated:** 2025-11-04

---

## Overview

The RAG (Retrieval-Augmented Generation) module provides semantic document search capabilities for AVA. It enables parsing, chunking, embedding, and searching through large document collections with intelligent retrieval.

### Key Features

✅ **Document Processing**
- PDF parsing (Android PdfRenderer)
- Text chunking with 3 strategies (Fixed, Semantic, Hybrid)
- Metadata preservation (sections, pages, headings)

✅ **Semantic Embeddings**
- ONNX Runtime integration
- all-MiniLM-L6-v2 model (384 dimensions)
- Int8 quantization (75% space savings)
- Batch processing support

✅ **Intelligent Search**
- Cosine similarity search
- Configurable filters
- Result ranking
- Full-text retrieval

✅ **Cross-Platform**
- Android (API 26+)
- iOS (arm64, x64, simulator)
- Desktop (JVM 17)

---

## Installation

### 1. Add Module Dependency

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":Universal:AVA:Features:RAG"))
}
```

### 2. Model Setup

The module requires ONNX models for generating embeddings. **Models are placed in external storage** to keep the APK size small (~30MB instead of ~120MB).

#### IMPORTANT: External Model Management (Required ⭐)

To keep APK size minimal, place models in device-specific external storage:

**Location:** `/sdcard/Android/data/com.augmentalis.ava/files/models/`

**Download model:**
```bash
# Download all-MiniLM-L6-v2 (86MB)
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx

# Place on device via ADB
adb push all-MiniLM-L6-v2.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
```

**See [docs/MODEL-SETUP.md](../../docs/MODEL-SETUP.md) for complete setup instructions.**

#### Alternative: On-Demand Download

You can also download models programmatically:

```kotlin
import com.augmentalis.ava.features.rag.embeddings.AndroidModelDownloadManager
import com.augmentalis.ava.features.rag.embeddings.AvailableModels

// Create download manager
val downloadManager = AndroidModelDownloadManager(context)

// Download default model (all-MiniLM-L6-v2, 90MB)
lifecycleScope.launch {
    downloadManager.downloadModel(
        modelId = AvailableModels.ALL_MINILM_L6_V2.id,
        onProgress = { progress ->
            // Update UI: progress is 0.0 to 1.0
            println("Download progress: ${(progress * 100).toInt()}%")
        }
    ).onSuccess { filePath ->
        println("Model downloaded to: $filePath")
    }.onFailure { error ->
        println("Download failed: ${error.message}")
    }
}

// Or observe progress with Flow
lifecycleScope.launch {
    downloadManager.observeDownloadProgress(AvailableModels.ALL_MINILM_L6_V2.id)
        .collect { progress ->
            when (progress) {
                is DownloadProgress.Downloading -> {
                    val percent = (progress.progress * 100).toInt()
                    updateUI("Downloading: $percent%")
                }
                is DownloadProgress.Completed -> {
                    updateUI("Model ready!")
                }
                is DownloadProgress.Failed -> {
                    showError(progress.error)
                }
                else -> { /* handle other states */ }
            }
        }
}
```

**Available Models:**
- `all-MiniLM-L6-v2` (86MB, 384 dim) - Default, good balance
- `paraphrase-MiniLM-L3-v2` (61MB, 384 dim) - Faster, smaller
- `all-mpnet-base-v2` (420MB, 768 dim) - Higher quality, larger

**Benefits of External Storage:**
- ✅ **Small APK:** Keeps APK ~30MB instead of ~120MB
- ✅ **User Choice:** Download only the models you need
- ✅ **Easy Updates:** Replace model files without app update
- ✅ **Multi-Source:** Download from HuggingFace, custom servers, or other sources
- ✅ **Privacy:** Models stay on device, no cloud dependency

#### Legacy: Bundle in APK (NOT Recommended ❌)

Bundling models in the APK is **strongly discouraged** as it adds 86-420MB per model.

Only use for development/testing if absolutely necessary. See docs/MODEL-SETUP.md for reasoning.

### 3. Initialize Factories

In your Android Application class:

```kotlin
class AvaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize document parser factory
        DocumentParserFactory.initialize(this)

        // Initialize embedding provider factory
        EmbeddingProviderFactory.initialize(this)
    }
}
```

---

## Usage

### Basic Example

```kotlin
import com.augmentalis.ava.features.rag.data.InMemoryRAGRepository
import com.augmentalis.ava.features.rag.domain.*
import com.augmentalis.ava.features.rag.embeddings.EmbeddingProviderFactory

// Create embedding provider
val embeddingProvider = EmbeddingProviderFactory.getONNXProvider(
    modelPath = "models/all-MiniLM-L6-v2.onnx"
) ?: throw IllegalStateException("ONNX provider not available")

// Create repository
val repository = InMemoryRAGRepository(
    embeddingProvider = embeddingProvider,
    chunkingConfig = ChunkingConfig(
        strategy = ChunkingStrategy.HYBRID,
        maxTokens = 512,
        overlapTokens = 50
    )
)

// Add a document
val result = repository.addDocument(
    AddDocumentRequest(
        filePath = "/path/to/document.pdf",
        title = "My Document",
        processImmediately = true
    )
)

when {
    result.isSuccess -> {
        val docResult = result.getOrNull()
        println("Document added: ${docResult?.documentId}")
    }
    result.isFailure -> {
        println("Error: ${result.exceptionOrNull()?.message}")
    }
}

// Search documents
val searchResult = repository.search(
    SearchQuery(
        query = "What is machine learning?",
        maxResults = 10,
        minSimilarity = 0.5f
    )
)

searchResult.getOrNull()?.results?.forEach { result ->
    println("Similarity: ${result.similarity}")
    println("Content: ${result.chunk.content}")
    println("Document: ${result.document?.title}")
    println("---")
}
```

### Advanced Configuration

```kotlin
// Custom chunking configuration
val chunkingConfig = ChunkingConfig(
    strategy = ChunkingStrategy.HYBRID,
    maxTokens = 512,
    overlapTokens = 50,
    respectSectionBoundaries = true,
    minChunkTokens = 100
)

// Custom search filters
val searchQuery = SearchQuery(
    query = "neural networks",
    maxResults = 20,
    minSimilarity = 0.6f,
    filters = SearchFilters(
        documentTypes = listOf(DocumentType.PDF, DocumentType.DOCX),
        dateRange = DateRange(
            start = "2024-01-01T00:00:00Z",
            end = "2024-12-31T23:59:59Z"
        )
    )
)
```

---

## Architecture

### Components

```
RAG Module
├── domain/          Domain models (Document, Chunk, Embedding)
├── parser/          Document parsing (PDF, TXT, etc.)
├── embeddings/      Embedding generation (ONNX, Local LLM, Cloud)
├── data/            Repository implementations
└── search/          Search algorithms (currently linear)
```

### Data Flow

```
1. User adds document
   ↓
2. DocumentParser extracts text + structure
   ↓
3. TextChunker splits into semantic chunks
   ↓
4. EmbeddingProvider generates vectors
   ↓
5. Repository stores chunks with embeddings
   ↓
6. User searches
   ↓
7. Generate query embedding
   ↓
8. Calculate similarities (cosine)
   ↓
9. Return ranked results
```

---

## Phase Status

### Phase 1: Foundation ✅ COMPLETE
- Domain models
- Repository interface
- Configuration system
- Platform abstractions

### Phase 2: Document Processing 🟡 80% COMPLETE
- ✅ Token counting
- ✅ Text chunking (3 strategies)
- ✅ Android PDF parser structure
- ✅ ONNX embedding provider
- ✅ Simplified tokenizer
- ✅ In-memory repository
- ✅ Unit tests (25 tests, all passing)
- ⏸️ Proper BERT tokenization (Phase 3)
- ⏸️ Full PDF text extraction (Phase 3)

### Phase 3: Vector Storage ⏸️ PLANNED
- SQLite-vec integration
- Cluster-based indexing (k-means)
- LRU cache for hot data
- Desktop sync protocol
- Power optimization

### Phase 4: Production ⏸️ PLANNED
- Proper BERT WordPiece tokenization
- Full PDF text extraction (TomRoush/PdfBox)
- Additional document formats (DOCX, TXT, MD)
- Cloud embedding fallback
- UI integration

---

## Performance

### Current (Phase 2 - Linear Search)

| Operation | Time | Notes |
|-----------|------|-------|
| Token counting | ~5k tokens/ms | Whitespace-based |
| Chunking | Not measured | Expect >1k chunks/sec |
| Embedding (ONNX) | ~10ms/text | Android mid-range |
| Search (10k chunks) | ~100ms | Linear scan |
| Search (200k chunks) | ~2s | Needs optimization |

### Target (Phase 3 - Clustered)

| Operation | Time | Notes |
|-----------|------|-------|
| Search (200k chunks) | <50ms | 40x speedup with clustering |
| Insertion | <100ms | With cluster assignment |
| Cache hit | <1ms | LRU hot cache |

---

## Testing

### Run Unit Tests

```bash
./gradlew :Universal:AVA:Features:RAG:test
```

### Test Coverage

- TokenCounter: 14 tests ✅
- SimpleTokenizer: 11 tests ✅
- Total: 25 tests, all passing

**Coverage:** ~60% (Phase 2 target: 85%)

### Add More Tests

Create test files in:
```
Universal/AVA/Features/RAG/src/commonTest/kotlin/
```

---

## Limitations & Known Issues

### Current Limitations

1. **Simplified Tokenization**
   - Uses whitespace splitting, not proper BERT WordPiece
   - Embedding quality may be suboptimal
   - **Fix:** Phase 3 will add ONNX Runtime Extensions tokenizer

2. **PDF Text Extraction Stub**
   - Android PdfRenderer only provides structure, not text
   - Currently returns placeholder text
   - **Fix:** Phase 3 will integrate TomRoush/PdfBox-Android

3. **Linear Search**
   - O(n) search time, slow for large collections
   - 200k chunks take ~2 seconds
   - **Fix:** Phase 3 cluster-based indexing (40x speedup)

4. **In-Memory Storage**
   - Data lost on restart
   - Limited by device RAM
   - **Fix:** Phase 3 SQLite-vec persistent storage

5. **Single Document Format**
   - Only PDF supported
   - **Fix:** Phase 4 will add DOCX, TXT, MD, HTML, EPUB

### Platform-Specific Notes

**Android:**
- Requires minSdk 26 (Android 8.0)
- ONNX model (~90MB) increases APK size
- Consider APK splits or dynamic feature modules

**iOS:**
- Embedding provider is stub (Phase 3)
- PDF parser is stub (Phase 3)
- Framework builds successfully

**Desktop:**
- Embedding provider is stub (Phase 3)
- PDF parser is stub (Phase 3)
- Can use full Apache PDFBox

---

## Troubleshooting

### Model not found error

```
IllegalStateException: Model file not found at assets/models/all-MiniLM-L6-v2.onnx
```

**Solution:** Download the ONNX model (see Installation step 2)

### Factory not initialized

```
DocumentParserFactory not initialized. Call initialize(context) first.
```

**Solution:** Call `DocumentParserFactory.initialize(context)` in Application.onCreate()

### Out of memory during search

**Cause:** Too many chunks in memory (>100k)

**Solution:**
- Reduce document count
- Use filters to narrow search
- Wait for Phase 3 (efficient storage)

### Slow search performance

**Expected:** Linear search is O(n), slow for large collections

**Solution:**
- Reduce search scope with filters
- Wait for Phase 3 (cluster-based indexing)

---

## Roadmap

### Next Session (Phase 2 Completion)
- Implement proper BERT tokenization
- Add more document format parsers
- Increase test coverage to 85%
- Performance benchmarks
- Integration tests

### Phase 3 (Vector Storage)
- SQLite-vec integration
- k-means clustering (256 clusters)
- LRU cache implementation
- Desktop sync protocol
- Power optimization

### Phase 4 (Production)
- Full PDF text extraction
- Additional document formats
- Cloud embedding fallback
- UI components
- Production deployment

---

## Contributing

### Code Style

- Kotlin 1.9+
- Compose Multiplatform
- Material 3 design
- MVVM architecture

### Adding Document Parsers

1. Implement `DocumentParser` interface
2. Register in platform-specific `DocumentParserFactory`
3. Add tests
4. Update documentation

### Adding Embedding Providers

1. Implement `EmbeddingProvider` interface
2. Register in `EmbeddingProviderFactory`
3. Add tests
4. Update documentation

---

## License

© Augmentalis Inc, Intelligent Devices LLC

---

## Support

For issues, questions, or feature requests, contact the AVA development team.

**Documentation:**
- Implementation Plan: `docs/active/RAG-System-Implementation-Plan.md`
- Phase 1 Progress: `docs/active/RAG-Phase1-Progress-251104.md`
- Phase 2 Progress: `docs/active/RAG-Phase2-Progress-251104.md`
- Phase 2 Spec: `docs/specs/RAG-Phase2-DocumentProcessing.md`
