# AVA RAG System - Complete Implementation Plan

**Created:** 2025-11-04
**Author:** AVA AI Team
**Version:** 1.0
**Purpose:** Detailed specification for implementing Retrieval-Augmented Generation in AVA

---

## 📋 Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Configuration Decisions](#configuration-decisions)
4. [Module Structure](#module-structure)
5. [Implementation Phases](#implementation-phases)
6. [Technical Specifications](#technical-specifications)
7. [Power Optimization](#power-optimization)
8. [Testing Strategy](#testing-strategy)
9. [Success Metrics](#success-metrics)

---

## Executive Summary

### Project Goal
Build a production-ready RAG system for AVA that enables field workers to query large document libraries instantly, with maximum battery efficiency and offline-first design.

### Key Requirements
- **Offline-first:** 200k chunks on-device, instant retrieval
- **Field-optimized:** Zero background drain during field work
- **Power-efficient:** Auto power modes based on context
- **Privacy-first:** On-device by default, cloud optional
- **Flexible:** Multiple embedding models, processing locations

### Target Performance
- **Search latency:** <50ms for 200k chunks
- **Indexing speed:** 500-1000 chunks/sec (desktop), 100-200 chunks/sec (mobile)
- **Storage efficiency:** 93MB for 200k chunks (quantized)
- **Battery impact:** 0mA during field use, <10mA during idle processing

---

## Architecture Overview

### Three-Tier Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      MOBILE DEVICE                          │
├─────────────────────────────────────────────────────────────┤
│  Tier 1: Hot Memory Cache (10k chunks, 4MB RAM)            │
│  - LRU cache for instant access (<5ms)                      │
│  - Recently accessed + frequently queried                   │
│  - Current conversation context                             │
├─────────────────────────────────────────────────────────────┤
│  Tier 2: Warm Disk Cache (190k chunks, ~89MB)              │
│  - SQLite with quantized int8 vectors                       │
│  - Memory-mapped I/O for fast access (<50ms)                │
│  - Cluster-based indexing (1000 clusters)                   │
├─────────────────────────────────────────────────────────────┤
│  Tier 3: Cold Storage (compressed archives)                 │
│  - Full float32 embeddings (compressed)                     │
│  - Backup for re-indexing                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    DESKTOP COMPANION                         │
├─────────────────────────────────────────────────────────────┤
│  ChromaDB Vector Store (1M+ chunks)                         │
│  - HNSW indexing for fast search                            │
│  - Full float32 precision                                   │
│  - Heavy processing offload                                  │
│  - LAN sync with mobile                                     │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    CLOUD SERVICES (Optional)                 │
├─────────────────────────────────────────────────────────────┤
│  - Cloud embedding APIs (OpenAI, Anthropic)                 │
│  - Web scraping service (stubbed for integration)           │
│  - Cloud storage sync (stubbed for integration)             │
│  - No persistent storage (privacy-first)                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Configuration Decisions

### 1. Document Formats Supported
**Decision:** All common formats
**Formats:**
- Text: TXT, MD, JSON, CSV, RTF
- Office: DOCX, XLSX, PPTX
- PDF: All variants
- Web: HTML, EPUB
- Code: All languages (syntax-aware chunking)

**Libraries:**
- Apache POI (~15MB) - Office documents
- PDFBox (~5MB) - PDF parsing
- jsoup (~400KB) - HTML parsing
- Custom parsers - MD, TXT, code

---

### 2. Processing Location
**Decision:** Hybrid (user selectable)

**Default Processing Order:**
1. Check for connected desktop (LAN) → offload heavy processing
2. If no desktop, use on-device processing
3. Optional: Cloud processing (user provides API key)

**Implementation:**
```kotlin
interface DocumentProcessor {
    suspend fun process(document: Document): ProcessingResult
}

class HybridProcessor(
    private val desktopClient: DesktopClient,
    private val onDeviceProcessor: OnDeviceProcessor,
    private val cloudProcessor: CloudProcessor
) : DocumentProcessor {

    override suspend fun process(document: Document): ProcessingResult {
        return when {
            desktopClient.isAvailable() ->
                desktopClient.process(document)

            userPreferences.allowCloudProcessing && cloudProcessor.hasApiKey() ->
                cloudProcessor.process(document)

            else ->
                onDeviceProcessor.process(document)
        }
    }
}
```

---

### 3. Embedding Models
**Decision:** All three options (user selectable)

**Option A: On-device ONNX (all-MiniLM-L6-v2)**
- Model size: 80MB
- Dimensions: 384
- Speed: 50 chunks/sec (mobile), 500 chunks/sec (desktop)
- Quality: Good (85% retrieval accuracy)
- **Default for mobile**

**Option B: Cloud API (OpenAI text-embedding-3-small)**
- Dimensions: 1536
- Speed: 1000 chunks/sec (API limited)
- Quality: Excellent (92% retrieval accuracy)
- Cost: ~$0.02 per 1M tokens
- **Optional (user provides key)**

**Option C: Local LLM (Gemma/Llama)**
- Dimensions: 768-4096 (model dependent)
- Speed: 10-30 chunks/sec
- Quality: Very good (88-90% accuracy)
- **Used when LLM already loaded**

**Smart Selection:**
```kotlin
class EmbeddingModelSelector {
    fun selectModel(context: ProcessingContext): EmbeddingModel {
        return when {
            // Desktop with local LLM loaded - reuse it
            context.isDesktop && context.localLLM.isLoaded() ->
                LocalLLMEmbedder(context.localLLM)

            // User wants max quality and has API key
            context.userPreferences.useCloudEmbeddings && hasApiKey() ->
                CloudEmbedder(apiKey)

            // Default: On-device ONNX
            else ->
                ONNXEmbedder(modelPath = "models/all-MiniLM-L6-v2.onnx")
        }
    }
}
```

---

### 4. Vector Database
**Decision:** Tiered approach

**Mobile: SQLite-vec with quantization**
- Capacity: 200k chunks (auto-scale based on device)
- Storage: ~93MB (with int8 quantization)
- Search: ~35ms (cluster-based indexing)

**Desktop: ChromaDB (embedded)**
- Capacity: 1M+ chunks
- Storage: ~400MB
- Search: ~10-30ms (HNSW indexing)

**Device Capacity Auto-Detection:**
```kotlin
fun getOptimalChunkLimit(): Int {
    val availableStorage = getAvailableStorage()
    val deviceRAM = getTotalRAM()

    return when {
        deviceRAM >= 8_GB && availableStorage >= 50_GB -> 500_000
        deviceRAM >= 6_GB && availableStorage >= 20_GB -> 200_000
        deviceRAM >= 4_GB -> 100_000
        else -> 50_000
    }
}
```

---

### 5. Document Input Methods
**Decision:** All methods implemented

**Phase 1 (MVP):**
- ✅ A: File Picker (primary method)
- ✅ E: Share Sheet Integration

**Phase 2:**
- ✅ B: Folder Watching (optional, disabled by default)
- ⏸️ C: Web Scraping (stubbed - integration with existing module)
- ⏸️ D: Cloud Storage (stubbed - integration with existing module)

**Future:**
- F: Screenshot OCR (if requested)

---

### 6. Chunking Strategy
**Decision:** Hybrid semantic + LLM-assisted

**Primary: Hybrid Semantic Chunking**
- Parse document structure (headings, paragraphs, sections)
- Split at natural boundaries
- Enforce max chunk size (800 tokens)
- Add overlap (100 tokens) when subdividing

**Secondary: LLM-Assisted (for complex documents)**
- Use onboard LLM for legal documents, research papers
- LLM decides optimal boundaries
- Fallback to hybrid if LLM unavailable

**Fallback: Fixed-size**
- 512 tokens per chunk, 128 overlap
- Used for unparseable formats or performance mode

**Format-Specific Optimizations:**
```kotlin
class AdaptiveChunker {
    fun chunk(document: Document): List<Chunk> {
        return when (document.format) {
            MARKDOWN -> MarkdownChunker().chunkByHeadings(document)
            PDF -> PDFChunker().chunkByStructure(document)
            CODE -> CodeChunker().chunkByFunctions(document)
            HTML -> HTMLChunker().chunkBySections(document)

            // Complex documents - use LLM if available
            LEGAL, RESEARCH_PAPER -> {
                if (localLLM.isAvailable()) {
                    LLMAssistedChunker().chunk(document, localLLM)
                } else {
                    HybridChunker().chunk(document)
                }
            }

            // Fallback
            else -> HybridChunker().chunk(document)
        }
    }
}
```

---

### 7. Re-indexing & Updates
**Decision:** Manual with smart prompts + Scheduled optional + Incremental

**Smart Prompts:**
```
AVA detects file changes:
  → Notification: "XYZ document has changed, shall I review it
                   so my information to you is up to date?"
  → [Update Now] [Schedule for Tonight] [Ignore]
```

**Scheduled Updates (Optional User Setting):**
- Default: "Every night at 2 AM (when charging)"
- Customizable schedule
- Only runs when charging + WiFi

**Incremental Updates:**
- Detect changed sections via diffing
- Only re-embed modified chunks
- Example: 1000-page manual, 3 pages changed = 135 chunks vs 4500 chunks

**Implementation:**
```kotlin
class UpdateManager {
    // Smart prompt on file change
    suspend fun onFileChanged(document: Document) {
        if (!userPreferences.autoUpdate) {
            notificationManager.show(
                title = "Document updated",
                message = "${document.name} has changed. Shall I review it so my information to you is up to date?",
                actions = listOf(
                    Action("Update Now") { reindexNow(document) },
                    Action("Schedule for Tonight") { scheduleUpdate(document) },
                    Action("Ignore") { /* no-op */ }
                )
            )
        }
    }

    // Incremental re-indexing
    suspend fun reindexIncremental(document: Document) {
        val oldVersion = getStoredDocument(document.id)
        val changedChunks = detectChangedChunks(oldVersion, document)

        if (changedChunks.isNotEmpty()) {
            embedAndStore(changedChunks)
            logger.info("Incremental update: ${changedChunks.size} chunks re-indexed")
        } else {
            logger.info("No changes detected")
        }
    }
}
```

---

### 8. Power/Performance Modes
**Decision:** Automatic detection with manual override

**Power Modes:**

| Mode | Background Processing | Memory | Battery | Use Case |
|------|---------------------|---------|---------|----------|
| **Ultra Saver** | None | 4MB | 0mA | Field work, <20% battery |
| **Power Saver** | None | 20MB | 0mA | Field work, on battery |
| **Balanced** | Light | 40MB | 5-10mA | Office, idle time |
| **Performance** | Full | 80MB | 20-30mA | Charging, desktop sync |

**Automatic Detection:**
```kotlin
fun detectOptimalMode(): PowerMode {
    return when {
        // Field work - ultra conservative
        isMoving() && !isCharging() -> ULTRA_SAVER

        // Low battery - save power
        batteryLevel < 20 -> POWER_SAVER

        // Charging at home/office - process everything
        isCharging() && isOnWiFi() -> PERFORMANCE

        // Stationary on battery - light work ok
        !isMoving() && batteryLevel > 50 -> BALANCED

        // Default conservative
        else -> POWER_SAVER
    }
}
```

---

## Module Structure

```
Universal/AVA/Features/RAG/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/ava/features/rag/
│   │   ├── domain/
│   │   │   ├── models/
│   │   │   │   ├── Document.kt
│   │   │   │   ├── Chunk.kt
│   │   │   │   ├── Embedding.kt
│   │   │   │   ├── SearchResult.kt
│   │   │   │   └── ProcessingConfig.kt
│   │   │   ├── repositories/
│   │   │   │   ├── DocumentRepository.kt
│   │   │   │   ├── VectorStore.kt
│   │   │   │   └── EmbeddingRepository.kt
│   │   │   └── usecases/
│   │   │       ├── IndexDocumentUseCase.kt
│   │   │       ├── SearchDocumentsUseCase.kt
│   │   │       ├── UpdateDocumentUseCase.kt
│   │   │       └── ManageIndexUseCase.kt
│   │   ├── data/
│   │   │   ├── parsers/
│   │   │   │   ├── DocumentParser.kt
│   │   │   │   ├── PDFParser.kt
│   │   │   │   ├── MarkdownParser.kt
│   │   │   │   ├── CodeParser.kt
│   │   │   │   └── OfficeParser.kt
│   │   │   ├── chunkers/
│   │   │   │   ├── Chunker.kt
│   │   │   │   ├── HybridChunker.kt
│   │   │   │   ├── SemanticChunker.kt
│   │   │   │   ├── FixedSizeChunker.kt
│   │   │   │   └── LLMAssistedChunker.kt
│   │   │   ├── embedders/
│   │   │   │   ├── EmbeddingModel.kt
│   │   │   │   ├── ONNXEmbedder.kt
│   │   │   │   ├── CloudEmbedder.kt
│   │   │   │   └── LocalLLMEmbedder.kt
│   │   │   ├── vectorstore/
│   │   │   │   ├── VectorStoreImpl.kt
│   │   │   │   ├── SqliteVectorStore.kt
│   │   │   │   ├── QuantizedVector.kt
│   │   │   │   └── ClusterIndex.kt
│   │   │   └── sync/
│   │   │       ├── DesktopSyncClient.kt
│   │   │       └── CloudSyncClient.kt (stub)
│   │   ├── processing/
│   │   │   ├── DocumentProcessor.kt
│   │   │   ├── HybridProcessor.kt
│   │   │   ├── IncrementalProcessor.kt
│   │   │   └── PowerModeManager.kt
│   │   └── ui/ (if needed)
│   ├── androidMain/kotlin/
│   │   ├── platform/
│   │   │   ├── AndroidDocumentPicker.kt
│   │   │   ├── ShareSheetReceiver.kt
│   │   │   ├── FolderWatcher.kt
│   │   │   └── PowerDetector.kt
│   │   └── workers/
│   │       ├── IndexingWorker.kt
│   │       ├── IncrementalUpdateWorker.kt
│   │       └── MaintenanceWorker.kt
│   ├── iosMain/kotlin/
│   │   └── platform/
│   │       └── IOSDocumentPicker.kt
│   └── desktopMain/kotlin/
│       └── platform/
│           ├── DesktopDocumentPicker.kt
│           └── ChromaDBStore.kt
└── build.gradle.kts
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1-2)
**Goal:** Basic document ingestion and search

**Tasks:**
1. Create module structure
2. Implement core domain models
3. Build basic document parsers (TXT, MD, PDF)
4. Implement fixed-size chunker (fallback)
5. Integrate ONNX embedding model
6. Build SQLite vector store (no quantization yet)
7. Implement file picker UI
8. Basic search functionality

**Deliverables:**
- Can index TXT/MD/PDF files
- Search 10k chunks in <100ms
- File picker working

**Success Criteria:**
- Index 100-page PDF in <30 seconds
- Search accuracy >80%
- Zero crashes

---

### Phase 2: Optimization (Week 3-4)
**Goal:** Power efficiency and scale

**Tasks:**
1. Implement vector quantization (int8)
2. Build cluster-based indexing
3. Add hybrid semantic chunker
4. Implement power mode detection
5. Add memory-mapped I/O
6. Optimize for 200k chunks
7. Add incremental updates

**Deliverables:**
- 200k chunk capacity
- Search <50ms
- Quantized storage (~93MB for 200k)
- Auto power modes

**Success Criteria:**
- 0mA battery drain in field mode
- 200k chunks search in <50ms
- 75% storage reduction with quantization

---

### Phase 3: Advanced Features (Week 5-6)
**Goal:** Multi-format support and desktop sync

**Tasks:**
1. Add Office document parsers (DOCX, XLSX, PPTX)
2. Add HTML/EPUB parsers
3. Implement LLM-assisted chunking
4. Build desktop companion client
5. Implement LAN sync protocol
6. Add folder watching
7. Add share sheet integration

**Deliverables:**
- All document formats supported
- Desktop sync working
- Folder watching (optional)
- Share sheet integration

**Success Criteria:**
- Parse all format types
- Desktop sync <1 min for 10k chunks
- Folder watching <5mA battery impact

---

### Phase 4: Cloud & Polish (Week 7-8)
**Goal:** Cloud options and production readiness

**Tasks:**
1. Integrate cloud embedding APIs
2. Add multi-embedding model support
3. Implement scheduled updates
4. Add smart update prompts
5. Build comprehensive settings UI
6. Stub cloud storage integration
7. Stub web scraping integration
8. Performance profiling and optimization

**Deliverables:**
- Cloud API integration
- All 3 embedding models working
- Smart update notifications
- Production-ready UI

**Success Criteria:**
- Cloud API <100ms per chunk
- Settings UI complete
- All features tested
- Documentation complete

---

## Technical Specifications

### Vector Quantization (int8)

**Algorithm:**
```kotlin
class QuantizedVector(
    val quantized: ByteArray,  // int8 values
    val scale: Float,          // Reconstruction scale
    val offset: Float          // Reconstruction offset
) {
    fun toFloat32(): FloatArray {
        return quantized.map { byte ->
            (byte.toFloat() * scale) + offset
        }.toFloatArray()
    }

    companion object {
        fun fromFloat32(vector: FloatArray): QuantizedVector {
            val min = vector.minOrNull() ?: 0f
            val max = vector.maxOrNull() ?: 0f
            val scale = (max - min) / 255f
            val offset = min

            val quantized = vector.map { value ->
                ((value - offset) / scale).toInt().toByte()
            }.toByteArray()

            return QuantizedVector(quantized, scale, offset)
        }
    }

    // Fast similarity without decompression
    fun cosineSimilarity(other: QuantizedVector): Float {
        // Approximate similarity in quantized space
        // Trade accuracy for speed (~2% error, 10x faster)
        var dotProduct = 0
        for (i in quantized.indices) {
            dotProduct += quantized[i] * other.quantized[i]
        }
        return dotProduct.toFloat() / (255f * 255f * quantized.size)
    }
}
```

**Storage Comparison:**
- float32: 384 dims × 4 bytes = 1,536 bytes
- int8: 384 dims × 1 byte + 8 bytes (scale/offset) = 392 bytes
- **Reduction: 75%**

---

### Cluster-Based Indexing

**Structure:**
```kotlin
class ClusterIndex(
    private val numClusters: Int = 1000
) {
    // Cluster centroids (in-memory)
    private val centroids = Array(numClusters) { FloatArray(384) }

    // Chunk-to-cluster mapping (in-memory)
    private val chunkToCluster = IntArray(200_000) // 800KB

    // Build clusters using k-means
    suspend fun buildIndex(chunks: List<EmbeddedChunk>) {
        val kMeans = KMeansClustering(k = numClusters)
        val assignments = kMeans.fit(chunks.map { it.embedding })

        // Store centroids
        assignments.centroids.forEachIndexed { idx, centroid ->
            centroids[idx] = centroid
        }

        // Store assignments
        assignments.labels.forEachIndexed { idx, cluster ->
            chunkToCluster[idx] = cluster
        }
    }

    // Search: find top clusters, then search within
    suspend fun search(
        query: FloatArray,
        limit: Int
    ): List<SearchResult> {
        // 1. Find top 10 closest clusters (5ms)
        val topClusters = centroids
            .mapIndexed { idx, centroid ->
                idx to cosineSimilarity(query, centroid)
            }
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }

        // 2. Load chunks from those clusters (20ms)
        val candidates = topClusters.flatMap { clusterId ->
            loadChunksInCluster(clusterId) // ~200 chunks per cluster
        }

        // 3. Search within candidates (10ms)
        return candidates
            .map { chunk ->
                chunk to cosineSimilarity(query, chunk.embedding)
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (chunk, score) -> SearchResult(chunk, score) }
    }
}
```

**Performance:**
- Without clustering: Linear scan 200k chunks = ~200ms
- With clustering: Search 10 clusters × 200 chunks = ~35ms
- **Speedup: 5.7x**

---

### SQLite Schema

```sql
-- Main embeddings table
CREATE TABLE embeddings (
    id TEXT PRIMARY KEY,
    document_id TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    cluster_id INTEGER NOT NULL,
    embedding BLOB NOT NULL,        -- Quantized vector (392 bytes)
    text TEXT NOT NULL,
    metadata JSON,
    created_at INTEGER NOT NULL,
    access_count INTEGER DEFAULT 0,
    last_accessed INTEGER,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Documents metadata
CREATE TABLE documents (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    path TEXT NOT NULL,
    format TEXT NOT NULL,
    size_bytes INTEGER NOT NULL,
    chunk_count INTEGER NOT NULL,
    last_modified INTEGER NOT NULL,
    indexed_at INTEGER NOT NULL,
    hash TEXT NOT NULL,             -- For change detection
    metadata JSON
);

-- Cluster index (in-memory loaded at startup)
CREATE TABLE clusters (
    cluster_id INTEGER PRIMARY KEY,
    centroid BLOB NOT NULL,         -- Float32 vector
    chunk_count INTEGER NOT NULL
);

-- Indexes for fast queries
CREATE INDEX idx_cluster ON embeddings(cluster_id, access_count DESC);
CREATE INDEX idx_document ON embeddings(document_id);
CREATE INDEX idx_hot ON embeddings(last_accessed DESC)
    WHERE last_accessed IS NOT NULL;
CREATE INDEX idx_doc_hash ON documents(hash);
```

---

### LAN Sync Protocol

**Protocol:**
```kotlin
// Desktop Server
class DesktopSyncServer(port: Int = 8765) {
    fun start() {
        server.route("/api/process") { request ->
            val document = request.receiveDocument()
            val result = processor.process(document)
            respond(result)
        }

        server.route("/api/search") { request ->
            val query = request.receiveQuery()
            val results = chromaDB.search(query)
            respond(results.compressed())
        }
    }
}

// Mobile Client
class DesktopClient {
    suspend fun isAvailable(): Boolean {
        // mDNS/Bonjour discovery
        return mdnsDiscovery.findService("_ava-desktop._tcp")
    }

    suspend fun process(document: Document): ProcessingResult {
        return httpClient.post("http://${desktopIP}:8765/api/process") {
            setBody(document)
        }
    }

    suspend fun search(query: String): List<SearchResult> {
        val response = httpClient.post("http://${desktopIP}:8765/api/search") {
            setBody(SearchQuery(query))
        }
        return response.body()
    }
}
```

---

## Power Optimization

### Background Processing Strategy

**Nightly Maintenance (when charging):**
```kotlin
class RAGMaintenanceWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        if (!isCharging() || !isWiFi()) {
            return Result.retry()
        }

        withTimeout(30.minutes) {
            // 1. Rebuild cluster index (10 min)
            rebuildClusters()

            // 2. Promote hot chunks to memory cache (2 min)
            updateHotCache()

            // 3. Evict cold chunks (not accessed in 30 days) (5 min)
            evictColdChunks()

            // 4. Sync with desktop if available (10 min)
            syncWithDesktop()

            // 5. Compress database (VACUUM) (3 min)
            compressDatabase()
        }

        return Result.success()
    }
}
```

**Power Mode Implementation:**
```kotlin
class PowerModeManager {
    private val currentMode = MutableStateFlow(PowerMode.BALANCED)

    init {
        // Monitor device state
        combine(
            batteryMonitor.level,
            motionDetector.isMoving,
            chargingMonitor.isCharging,
            networkMonitor.isWiFi
        ) { battery, moving, charging, wifi ->
            detectOptimalMode(battery, moving, charging, wifi)
        }.onEach { mode ->
            if (mode != currentMode.value) {
                switchMode(mode)
            }
        }.launchIn(scope)
    }

    private suspend fun switchMode(newMode: PowerMode) {
        val oldMode = currentMode.value
        currentMode.value = newMode

        // Apply mode settings
        applyModeSettings(newMode)

        // Notify user
        if (newMode.severity > oldMode.severity) {
            notificationManager.show(
                title = "Switched to ${newMode.name}",
                message = newMode.description
            )
        }
    }

    private fun applyModeSettings(mode: PowerMode) {
        // Adjust memory cache size
        hotCache.resize(mode.keepChunksInMemory)

        // Enable/disable background processing
        if (mode.allowBackgroundProcessing) {
            workManager.resume()
        } else {
            workManager.pause()
        }

        // Switch vector precision
        vectorStore.setQuantizationMode(mode.useQuantizedVectors)
    }
}
```

---

## Testing Strategy

### Unit Tests

**Coverage targets:**
- Parsers: 90%
- Chunkers: 95%
- Embedders: 85%
- Vector Store: 95%
- Sync: 80%

**Key test cases:**
```kotlin
class ChunkerTest {
    @Test
    fun `hybrid chunker respects max size`() {
        val largeSection = generateText(2000) // 2000 tokens
        val chunks = hybridChunker.chunk(largeSection)

        chunks.forEach { chunk ->
            assert(chunk.tokenCount <= 800)
        }
    }

    @Test
    fun `semantic chunker preserves paragraphs`() {
        val document = loadMarkdown("sample.md")
        val chunks = semanticChunker.chunk(document)

        chunks.forEach { chunk ->
            // No mid-sentence splits
            assert(!chunk.text.first().isLowerCase())
            assert(chunk.text.last() in listOf('.', '!', '?', '\n'))
        }
    }
}

class VectorStoreTest {
    @Test
    fun `quantized similarity within 3% of float32`() {
        val vector = randomFloatArray(384)
        val quantized = QuantizedVector.fromFloat32(vector)
        val restored = quantized.toFloat32()

        val originalSim = cosineSimilarity(vector, vector)
        val quantizedSim = cosineSimilarity(vector, restored)

        assert(abs(originalSim - quantizedSim) < 0.03)
    }

    @Test
    fun `search 200k chunks in under 50ms`() {
        // Index 200k chunks
        repeat(200_000) { i ->
            store.insert(generateChunk(i))
        }

        val query = randomEmbedding()
        val startTime = System.currentTimeMillis()
        val results = store.search(query, limit = 10)
        val duration = System.currentTimeMillis() - startTime

        assert(duration < 50)
        assert(results.size == 10)
    }
}
```

---

### Integration Tests

**Test scenarios:**
1. **End-to-end indexing:**
   - Add 100-page PDF
   - Verify chunks created
   - Search and retrieve
   - Accuracy >85%

2. **Desktop sync:**
   - Connect to desktop
   - Process document on desktop
   - Sync to mobile
   - Verify chunks match

3. **Incremental updates:**
   - Index document
   - Modify 10%
   - Re-index incrementally
   - Verify only 10% re-processed

4. **Power mode switching:**
   - Simulate battery drain
   - Verify mode switches
   - Verify background processing paused

---

### Performance Tests

**Benchmarks:**
```kotlin
@Test
fun benchmarkIndexing() {
    val documents = listOf(
        loadPDF("manual-100-pages.pdf"),
        loadDOCX("report-50-pages.docx"),
        loadMD("documentation.md")
    )

    val startTime = System.currentTimeMillis()
    documents.forEach { doc ->
        ragSystem.index(doc)
    }
    val duration = System.currentTimeMillis() - startTime

    println("Indexed ${documents.size} documents in ${duration}ms")
    println("Average: ${duration / documents.size}ms per document")

    // Target: <5 seconds per 100-page document
    assert(duration < documents.size * 5000)
}

@Test
fun benchmarkSearch() {
    // Index 100k chunks
    repeat(100_000) { i ->
        ragSystem.index(generateChunk(i))
    }

    // Perform 100 searches
    val queries = generateQueries(100)
    val durations = queries.map { query ->
        measureTimeMillis {
            ragSystem.search(query)
        }
    }

    val avgDuration = durations.average()
    val p95Duration = durations.sorted()[95]

    println("Average search: ${avgDuration}ms")
    println("P95 search: ${p95Duration}ms")

    // Target: avg <30ms, p95 <50ms
    assert(avgDuration < 30)
    assert(p95Duration < 50)
}
```

---

## Success Metrics

### Performance Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Indexing Speed** | 100-200 chunks/sec (mobile) | Benchmark |
| **Search Latency** | <50ms (200k chunks) | P95 latency |
| **Storage Efficiency** | 93MB for 200k chunks | Actual DB size |
| **Battery Impact (idle)** | <10mA | Battery stats |
| **Battery Impact (field)** | 0mA | Battery stats |
| **Retrieval Accuracy** | >85% | Relevance tests |

### Quality Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Crash-free rate** | >99.5% | Crash reporting |
| **Search relevance** | >4.0/5.0 | User ratings |
| **Answer quality** | >85% helpful | User feedback |
| **Document support** | All formats | Format coverage |

### User Experience Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Setup time** | <2 min to first search | User testing |
| **Time to index 100 docs** | <10 min | Benchmark |
| **Field mode activation** | Automatic | Detection rate |
| **Update prompt accuracy** | >90% relevant | User acceptance |

---

## Next Steps

### Immediate Actions

1. **Create module structure** (Universal/AVA/Features/RAG/)
2. **Set up dependencies** (ONNX Runtime, PDFBox, Apache POI)
3. **Implement domain models** (Document, Chunk, Embedding)
4. **Build basic document parser** (start with PDF)
5. **Integrate ONNX embedding model**

### Week 1 Deliverable

- Basic RAG module compiling
- Can index a simple PDF
- Can search indexed chunks
- Basic file picker UI

---

## Appendices

### A. Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // ONNX Runtime for embeddings
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // Document parsing
    implementation("org.apache.pdfbox:pdfbox:2.0.30")              // ~5MB
    implementation("org.apache.poi:poi-ooxml:5.2.5")               // ~15MB
    implementation("org.jsoup:jsoup:1.17.2")                       // ~400KB

    // Vector operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // SQLite
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Network (for desktop sync)
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-android:2.3.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

### B. Storage Estimates

**200k chunks:**
- Quantized vectors: 76MB
- Cluster index: 2MB
- Metadata: 10MB
- SQLite overhead: 5MB
- **Total: ~93MB**

**500k chunks (high-end devices):**
- Quantized vectors: 190MB
- Cluster index: 5MB
- Metadata: 25MB
- SQLite overhead: 13MB
- **Total: ~233MB**

### C. API Surface

```kotlin
// Public API for RAG module
interface RAGSystem {
    // Indexing
    suspend fun indexDocument(document: Document): Result<IndexResult>
    suspend fun indexDocuments(documents: List<Document>): Result<IndexResult>
    suspend fun deleteDocument(documentId: String): Result<Unit>

    // Searching
    suspend fun search(query: String, limit: Int = 10): Result<List<SearchResult>>
    suspend fun searchInDocument(query: String, documentId: String): Result<List<SearchResult>>

    // Management
    suspend fun getIndexedDocuments(): Result<List<DocumentMetadata>>
    suspend fun getIndexStats(): Result<IndexStats>
    suspend fun rebuildIndex(): Result<Unit>

    // Settings
    fun setEmbeddingModel(model: EmbeddingModel)
    fun setPowerMode(mode: PowerMode)
    fun setProcessingLocation(location: ProcessingLocation)
}
```

---

**End of Implementation Plan**

**Document Version:** 1.0
**Last Updated:** 2025-11-04
**Total Pages:** Comprehensive specification complete

This plan provides everything needed to implement a production-ready RAG system for AVA with field-optimized performance and power efficiency.
