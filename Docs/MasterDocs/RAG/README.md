# RAG Module - Retrieval-Augmented Generation

**Version:** 1.0 | **Platform:** Kotlin Multiplatform | **Last Updated:** 2026-01-11

---

## Executive Summary

The RAG module provides enterprise-grade semantic document search and retrieval for the AVA platform.

### Key Capabilities

| Capability | Description |
|------------|-------------|
| **Semantic Search** | Vector-based similarity matching |
| **Multi-Format Support** | PDF, DOCX, TXT, MD, HTML, EPUB, RTF |
| **Cross-Platform** | Android, iOS, Desktop via KMP |
| **High Performance** | <100ms search latency |
| **Intelligent Chunking** | Hybrid semantic/fixed-size splitting |
| **K-Means Clustering** | 256 clusters for fast retrieval |
| **LRU Caching** | >70% cache hit rates |

### Performance Metrics

| Metric | Value |
|--------|-------|
| Search Latency | <100ms |
| Indexing Speed | 10x faster (Phase 3) |
| Cache Hit Rate | >70% |
| Memory (200k chunks) | ~178MB |

---

## Architecture Overview

```
User Document
    ↓
[DocumentParser] extracts text + structure
    ↓
[TextChunker] creates semantic chunks
    ↓
[EmbeddingProvider] generates 384-dim vectors
    ↓
[SQLiteRAGRepository] stores chunks + embeddings
    ↓
[KMeansClustering] organizes into 256 clusters
    ↓
[QueryCache] LRU cache for hot queries
    ↓
[ClusteredSearchHandler] finds nearest clusters + chunks
    ↓
[RAGChatEngine] assembles context + generates response
    ↓
User Response + Source Citations
```

### Layer Architecture

```
┌────────────────────────────────────┐
│    Application Layer               │
│  (RAGChatScreen, RAGSearch)        │
└────────────┬───────────────────────┘
             │
┌────────────▼───────────────────────┐
│  RAGChatEngine / ViewModels        │
└────────────┬───────────────────────┘
             │
┌────────────▼───────────────────────┐
│  SQLiteRAGRepository (Facade)      │
│  ├─ DocumentIngestionHandler       │
│  ├─ ChunkEmbeddingHandler          │
│  └─ ClusteredSearchHandler         │
└────────┬──────────┬────────────┬───┘
         │          │            │
     ┌───▼──┐  ┌───▼───┐  ┌────▼────┐
     │Query │  │K-Means│  │Embedding│
     │Cache │  │Cluster│  │Provider │
     └──────┘  └───────┘  └─────────┘
         │          │            │
     ┌───▼──────────▼────────────▼───┐
     │  SQLDelight Database Layer    │
     └───────────────────────────────┘
```

---

## Module Structure

```
Modules/RAG/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/rag/
│   │   ├── domain/              # Models
│   │   │   ├── Document.kt
│   │   │   ├── Chunk.kt
│   │   │   ├── SearchQuery.kt
│   │   │   └── RAGConfig.kt
│   │   ├── parser/              # Document parsing
│   │   │   ├── DocumentParser.kt
│   │   │   └── TextChunker.kt
│   │   ├── embeddings/          # Embedding providers
│   │   ├── search/              # Search algorithms
│   │   ├── cache/               # Query caching
│   │   └── chat/                # Chat integration
│   │
│   └── androidMain/kotlin/com/augmentalis/rag/
│       ├── data/                # SQLite repository
│       ├── parser/              # Platform parsers
│       ├── embeddings/          # ONNX provider
│       ├── handlers/            # Batch processing
│       ├── clustering/          # K-means
│       └── security/            # Encryption
```

---

## Class Inventory

### Domain Models

| Class | Purpose |
|-------|---------|
| `Document` | Document representation |
| `Chunk` | Text segment with embedding |
| `SearchQuery` | Query parameters |
| `SearchResult` | Single search result |
| `RAGConfig` | System configuration |

### Repositories

| Class | Purpose |
|-------|---------|
| `RAGRepository` | Interface for RAG operations |
| `SQLiteRAGRepository` | SQLite persistent storage |
| `InMemoryRAGRepository` | In-memory implementation |

### Parsers

| Class | Formats |
|-------|---------|
| `PdfParser` | PDF documents |
| `DocxParser` | Word documents |
| `MarkdownParser` | .md files |
| `HtmlParser` | HTML pages |
| `TxtParser` | Plain text |

### Embeddings

| Class | Purpose |
|-------|---------|
| `EmbeddingProvider` | Interface |
| `ONNXEmbeddingProvider` | ONNX Runtime embeddings |
| `Quantization` | Int8 quantization (75% savings) |

---

## API Reference

### RAGRepository Interface

```kotlin
interface RAGRepository {
    suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult>
    suspend fun getDocument(documentId: String): Result<Document?>
    fun listDocuments(status: DocumentStatus?): Flow<Document>
    suspend fun deleteDocument(documentId: String): Result<Unit>
    suspend fun search(query: SearchQuery): Result<SearchResponse>
    suspend fun getStatistics(): Result<RAGStatistics>
}
```

### SearchQuery API

```kotlin
data class SearchQuery(
    val query: String,
    val maxResults: Int = 10,
    val minSimilarity: Float = 0.5f,
    val filters: SearchFilters = SearchFilters()
)

data class SearchFilters(
    val documentIds: List<String>? = null,
    val documentTypes: List<DocumentType>? = null,
    val dateRange: DateRange? = null,
    val metadata: Map<String, String>? = null,
    val semanticTypes: List<SemanticType>? = null
)
```

### RAGChatEngine

```kotlin
val chatEngine = RAGChatEngine(
    ragRepository = repository,
    llmProvider = llmProvider,
    config = ChatConfig(
        maxContextChunks = 5,
        minSimilarity = 0.7f,
        maxContextLength = 2000
    )
)

chatEngine.ask(question).collect { response ->
    when (response) {
        is ChatResponse.Streaming -> print(response.text)
        is ChatResponse.Complete -> {
            println("Answer: ${response.fullText}")
            response.sources.forEach { source ->
                println("Source: ${source.title} (page ${source.page})")
            }
        }
        is ChatResponse.Error -> println("Error: ${response.message}")
    }
}
```

---

## Embedding Models

| Model ID | Dimension | Size | Use Case |
|----------|-----------|------|----------|
| `AVA-384-Base-INT8` | 384 | 32MB | Default, general-purpose |
| `AVA-384-Fast-INT8` | 384 | 23MB | Speed critical |
| `AVA-768-Qual-INT8` | 768 | 112MB | Best quality |
| `AVA-384-Multi-INT8` | 384 | 32MB | 50+ languages |

---

## Usage Examples

### Basic Setup

```kotlin
val embeddingProvider = EmbeddingProviderFactory.getONNXProvider(
    modelPath = "/path/to/AVA-384-Base-INT8.aon"
)

val repository = SQLiteRAGRepository(
    context = context,
    embeddingProvider = embeddingProvider,
    chunkingConfig = ChunkingConfig(
        strategy = ChunkingStrategy.HYBRID,
        maxTokens = 512,
        overlapTokens = 50
    )
)
```

### Adding Documents

```kotlin
val result = repository.addDocument(
    AddDocumentRequest(
        filePath = "/sdcard/Documents/manual.pdf",
        title = "User Manual",
        metadata = mapOf("category" to "Documentation"),
        processImmediately = true
    )
)
```

### Semantic Search

```kotlin
val response = repository.search(
    SearchQuery(
        query = "How do I reset the device?",
        maxResults = 10,
        minSimilarity = 0.6f
    )
)

response.onSuccess { results ->
    println("Found ${results.results.size} in ${results.searchTimeMs}ms")
    results.results.forEach { result ->
        println("${result.document?.title}: ${result.similarity}")
    }
}
```

### RAG-Enhanced Chat

```kotlin
chatEngine.ask(
    question = "What are the system requirements?",
    conversationHistory = emptyList()
).collect { response ->
    when (response) {
        is ChatResponse.Complete -> {
            println("Answer: ${response.fullText}")
            println("Sources: ${response.sources.map { it.title }}")
        }
    }
}
```

---

## Configuration

### RAGConfig

```kotlin
data class RAGConfig(
    val embeddingConfig: EmbeddingConfig = EmbeddingConfig(),
    val storageConfig: StorageConfig = StorageConfig(),
    val chunkingConfig: ChunkingConfig = ChunkingConfig()
)

data class EmbeddingConfig(
    val modelName: String = "all-MiniLM-L6-v2",
    val dimension: Int = 384,
    val batchSize: Int = 32,
    val quantize: Boolean = true  // 75% space savings
)

data class StorageConfig(
    val maxChunks: Int = 200_000,
    val enableClustering: Boolean = true,
    val clusterCount: Int = 256,
    val cacheSize: Int = 100
)

data class ChunkingConfig(
    val strategy: ChunkingStrategy = ChunkingStrategy.HYBRID,
    val maxTokens: Int = 512,
    val overlapTokens: Int = 50
)
```

---

## Performance

### Phase 3.0 Optimizations

| Operation | Before | After | Speedup |
|-----------|--------|-------|---------|
| Index 1000 docs | 50s | 5s | 10x |
| Search 200k chunks | 2000ms | 80ms | 25x |
| Memory per 200k | 300MB | 78MB | 3.8x |

### Search Benchmark (<100ms)

```
1. Check cache → HIT? Return in 1-5ms
2. Cache MISS:
   a. Embedding generation: 10ms
   b. Find nearest clusters: 2ms
   c. Search within clusters: 20ms
   d. Rank results: 5ms
   e. Assemble response: 15ms
Total: ~52ms (under 100ms target)
```

---

## Integration with AVA

```kotlin
class AVAKnowledgeManager(
    private val ragRepository: SQLiteRAGRepository
) {
    suspend fun addDocumentation(file: File) {
        ragRepository.addDocument(
            AddDocumentRequest(
                filePath = file.absolutePath,
                title = file.nameWithoutExtension,
                processImmediately = true
            )
        )
    }

    suspend fun search(query: String) = ragRepository.search(
        SearchQuery(query, maxResults = 10)
    )
}
```

---

## Related Documentation

- [AVA Module](../AVA/README.md)
- [LLM Module](../LLM/README.md)
- [NLU Module](../NLU/README.md)

---

**Author:** Avanues RAG Team | **Last Updated:** 2026-01-11
