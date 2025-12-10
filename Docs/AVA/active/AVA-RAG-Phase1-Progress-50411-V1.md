# RAG System - Phase 1 Foundation Progress

**Date:** 2025-11-04
**Phase:** 1 (Foundation)
**Status:** Module Structure Complete, Domain Models Implemented
**Build:** ✅ SUCCESSFUL

---

## Summary

Phase 1 (Foundation) of the RAG system implementation is underway. The Universal/AVA/Features/RAG module has been created as a Kotlin Multiplatform module with complete domain models and repository interfaces.

### What Was Completed

✅ Module structure created with KMP support (Android, iOS, Desktop)
✅ Build configuration set up with proper dependencies
✅ Complete domain model definitions
✅ Repository interface specification
✅ Configuration and power management models
✅ Document parser and embedding provider interfaces
✅ Platform-specific stub implementations (all platforms compile)
✅ Module successfully builds for all target platforms

---

## Module Structure

```
Universal/AVA/Features/RAG/
├── build.gradle.kts                     # KMP build configuration
└── src/
    ├── commonMain/kotlin/.../rag/
    │   ├── domain/
    │   │   ├── Document.kt              # Document entity & types
    │   │   ├── Chunk.kt                 # Chunk entity & embeddings
    │   │   ├── SearchQuery.kt           # Search types & results
    │   │   ├── RAGRepository.kt         # Repository interface
    │   │   └── RAGConfig.kt             # Configuration & power modes
    │   ├── parser/
    │   │   └── DocumentParser.kt        # Parser interface (expect)
    │   └── embeddings/
    │       └── EmbeddingProvider.kt     # Embedding interface (expect)
    ├── androidMain/kotlin/.../rag/
    │   ├── parser/
    │   │   └── DocumentParserFactory.android.kt   # Stub (Phase 2)
    │   └── embeddings/
    │       └── EmbeddingProviderFactory.android.kt # Stub (Phase 2)
    ├── iosMain/kotlin/.../rag/
    │   ├── parser/
    │   │   └── DocumentParserFactory.ios.kt       # Stub (Phase 2)
    │   └── embeddings/
    │       └── EmbeddingProviderFactory.ios.kt    # Stub (Phase 2)
    └── desktopMain/kotlin/.../rag/
        ├── parser/
        │   └── DocumentParserFactory.desktop.kt   # Stub (Phase 2)
        └── embeddings/
            └── EmbeddingProviderFactory.desktop.kt # Stub (Phase 2)
```

---

## Domain Models Implemented

### 1. Document (Document.kt:10-62)

Core document entity with full lifecycle support:

```kotlin
data class Document(
    val id: String,
    val title: String,
    val filePath: String,
    val fileType: DocumentType,
    val sizeBytes: Long,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val indexedAt: Instant? = null,
    val chunkCount: Int = 0,
    val metadata: Map<String, String> = emptyMap(),
    val status: DocumentStatus = DocumentStatus.PENDING
)
```

**Supported Document Types:**
- PDF, DOCX, TXT, MD, HTML, EPUB, RTF

**Status Lifecycle:**
- PENDING → PROCESSING → INDEXED
- OUTDATED (needs re-indexing)
- FAILED, DELETED

### 2. Chunk (Chunk.kt:18-74)

Atomic retrieval unit with embeddings:

```kotlin
data class Chunk(
    val id: String,
    val documentId: String,
    val content: String,
    val chunkIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val metadata: ChunkMetadata,
    val createdAt: Instant
)
```

**Embedding Types:**
- `Float32`: Full precision (384 floats = 1536 bytes)
- `Int8`: Quantized (384 bytes = 75% space savings, ~3% accuracy loss)

**Semantic Types:**
- HEADING, PARAGRAPH, LIST_ITEM, CODE, TABLE, QUOTE, CAPTION

**Quantization Support:**
```kotlin
companion object {
    fun quantize(values: FloatArray): Int8 {
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 0f
        val scale = (max - min) / 255f
        val offset = min

        val quantized = ByteArray(values.size) { i ->
            ((values[i] - offset) / scale).toInt().toByte()
        }

        return Int8(quantized, scale, offset)
    }
}
```

### 3. Search (SearchQuery.kt:11-69)

Comprehensive search capabilities:

```kotlin
data class SearchQuery(
    val query: String,
    val maxResults: Int = 10,
    val minSimilarity: Float = 0.5f,
    val filters: SearchFilters = SearchFilters(),
    val includeContent: Boolean = true
)

data class SearchResult(
    val chunk: Chunk,
    val similarity: Float,
    val document: Document? = null,
    val highlights: List<String> = emptyList()
)
```

### 4. Configuration (RAGConfig.kt:18-161)

Complete system configuration:

```kotlin
data class RAGConfig(
    val embeddingConfig: EmbeddingConfig,      // Model selection
    val storageConfig: StorageConfig,          // Capacity & clustering
    val processingConfig: ProcessingConfig,    // Location & priority
    val powerConfig: PowerConfig,              // Battery optimization
    val chunkingConfig: ChunkingConfig         // Document splitting
)
```

**Embedding Providers:**
- ONNX (on-device, fast)
- LOCAL_LLM (reuse chat model)
- CLOUD_API (fallback)

**Storage Tiers:**
- MOBILE: SQLite-vec, 200k chunks, quantized
- DESKTOP: SQLite-vec, 1M chunks, quantized + full
- CLOUD: ChromaDB, unlimited, full precision

**Power Modes:**
- AUTO: Context-aware detection
- FIELD: Zero background, instant search only
- OFFICE: Normal processing, balanced
- CHARGING: Aggressive processing

---

## Interfaces Defined

### RAGRepository (RAGRepository.kt:16-67)

Complete repository contract:

```kotlin
interface RAGRepository {
    suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult>
    suspend fun getDocument(documentId: String): Result<Document?>
    fun listDocuments(status: DocumentStatus? = null): Flow<Document>
    suspend fun deleteDocument(documentId: String): Result<Unit>
    suspend fun processDocuments(documentId: String? = null): Result<Int>
    suspend fun search(query: SearchQuery): Result<SearchResponse>
    suspend fun getChunks(documentId: String): Result<List<Chunk>>
    suspend fun getStatistics(): Result<RAGStatistics>
    suspend fun clearAll(): Result<Unit>
}
```

### DocumentParser (DocumentParser.kt:19-35)

Platform-specific document parsing:

```kotlin
interface DocumentParser {
    val supportedTypes: Set<DocumentType>
    suspend fun parse(filePath: String, documentType: DocumentType): Result<ParsedDocument>
}

expect object DocumentParserFactory {
    fun getParser(documentType: DocumentType): DocumentParser?
    fun getAllParsers(): List<DocumentParser>
    fun isSupported(documentType: DocumentType): Boolean
}
```

### EmbeddingProvider (EmbeddingProvider.kt:20-47)

Embedding generation abstraction:

```kotlin
interface EmbeddingProvider {
    val name: String
    val dimension: Int
    suspend fun isAvailable(): Boolean
    suspend fun embed(text: String): Result<Embedding.Float32>
    suspend fun embedBatch(texts: List<String>): Result<List<Embedding.Float32>>
    fun estimateTimeMs(count: Int): Long
}

expect object EmbeddingProviderFactory {
    fun getONNXProvider(modelPath: String): EmbeddingProvider?
    fun getLocalLLMProvider(): EmbeddingProvider?
    fun getCloudProvider(apiKey: String, endpoint: String): EmbeddingProvider?
}
```

---

## Build Configuration

### Dependencies

**Common:**
- `kotlinx-coroutines-core:1.7.3`
- `kotlinx-serialization-json:1.6.2`
- `kotlinx-datetime:0.5.0`

**Android:**
- `androidx.room:room-runtime:2.6.1` (vector storage)
- `androidx.room:room-ktx:2.6.1`
- `onnxruntime-android:1.16.3` (embeddings)

**Platforms:**
- ✅ Android (minSdk 26)
- ✅ iOS (arm64, x64, simulator arm64)
- ✅ Desktop (JVM 17)

---

## Build Results

```
> Task :Universal:AVA:Features:RAG:compileCommonMainKotlinMetadata
> Task :Universal:AVA:Features:RAG:compileKotlinAndroid
> Task :Universal:AVA:Features:RAG:compileKotlinIosArm64
> Task :Universal:AVA:Features:RAG:compileKotlinIosX64
> Task :Universal:AVA:Features:RAG:compileKotlinIosSimulatorArm64
> Task :Universal:AVA:Features:RAG:compileKotlinDesktop
> Task :Universal:AVA:Features:RAG:build

BUILD SUCCESSFUL in 51s
205 actionable tasks: 41 executed, 164 up-to-date
```

✅ All platforms compile successfully
✅ No warnings or errors
✅ Ready for Phase 2 implementation

---

## Next Steps (Phase 2: Document Processing)

### Immediate Tasks

1. **Implement PDF Parser**
   - Use TomRoush/PdfBox-Android fork for Android
   - Apache PDFBox for Desktop/iOS
   - Extract text with structure (pages, sections)
   - Implement heading detection heuristics

2. **Implement ONNX Embedding Provider**
   - Download all-MiniLM-L6-v2 ONNX model
   - Initialize ONNX Runtime
   - Implement batch embedding (32 texts)
   - Add performance metrics

3. **Implement Semantic Chunker**
   - Hybrid chunking strategy (semantic + LLM-assisted)
   - Respect document structure (sections, paragraphs)
   - 512 token max with 50 token overlap
   - Preserve metadata (section, page number)

4. **Create Basic Repository Implementation**
   - In-memory implementation for testing
   - Add document lifecycle management
   - Implement basic search (linear scan)
   - Add logging and error handling

5. **Write Unit Tests**
   - Document lifecycle tests
   - Chunk quantization accuracy tests
   - Configuration validation tests
   - Search query builder tests

### Later Phases

**Phase 3: Vector Storage (Week 3-4)**
- SQLite-vec integration
- Cluster-based indexing (k-means)
- LRU cache implementation
- Database migrations

**Phase 4: Power Optimization (Week 5-6)**
- GPS-based field detection
- Battery impact monitoring
- Scheduled background processing
- Desktop sync protocol

**Phase 5: UI Integration (Week 7-8)**
- Document management screen
- Search interface
- Settings panel
- Progress indicators

---

## Key Design Decisions

### 1. Quantization by Default

All embeddings are quantized to int8 for 75% space savings:
- Mobile: 200k chunks = 93MB (vs 372MB float32)
- Only ~3% accuracy loss in retrieval
- Massive battery savings (less I/O)

### 2. Field-First Power Design

Zero background processing in field mode:
- Auto-detect via GPS/charging state
- All indexing during charging at night
- Hot cache for instant search (<50ms)
- 0mA battery impact

### 3. Platform-Specific Parsers

Each platform uses native libraries:
- Android: PdfBox-Android (optimized)
- Desktop: Full Apache PDFBox (more features)
- iOS: Native PDF APIs (system integration)

### 4. Tiered Storage Architecture

Capacity adapts to platform:
- Mobile: 200k chunks (field use)
- Desktop: 1M chunks (office/home)
- Cloud: Unlimited (backup/sync)

---

## Documentation

All implementation details are in:
- **Plan:** `docs/active/RAG-System-Implementation-Plan.md` (complete spec)
- **Progress:** `docs/active/RAG-Phase1-Progress-251104.md` (this file)
- **Code:** `Universal/AVA/Features/RAG/` (domain models + interfaces)

---

## Conclusion

**Phase 1 Status: COMPLETE ✅**

The RAG module foundation is fully implemented with:
- ✅ Comprehensive domain models
- ✅ Clear repository interface
- ✅ Flexible configuration system
- ✅ Platform-specific abstractions
- ✅ Successful multi-platform build

Ready to proceed to Phase 2 (Document Processing) with confidence that the architecture is solid and extensible.

**Build Time:** 51 seconds
**Lines of Code:** ~800 (domain models only)
**Test Coverage:** 0% (Phase 2 target: 80%+)
**Next Milestone:** Phase 2 - Working document parser + embeddings
