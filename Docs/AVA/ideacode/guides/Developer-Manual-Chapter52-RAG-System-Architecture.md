# Developer Manual - Chapter 52: RAG System Architecture & Implementation

**Version**: 1.0
**Date**: 2025-11-27
**Author**: Manoj Jhawar
**Related ADR**: ADR-012-RAG-System-Architecture
**Related Spec**: rag-system-spec.md

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Document Ingestion](#3-document-ingestion)
4. [Text Chunking](#4-text-chunking)
5. [Embedding Generation](#5-embedding-generation)
6. [Vector Indexing](#6-vector-indexing)
7. [RAG Query Pipeline](#7-rag-query-pipeline)
8. [Storage & Models](#8-storage--models)
9. [Integration Guide](#9-integration-guide)
10. [Performance Optimization](#10-performance-optimization)
11. [Testing](#11-testing)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Overview

AVA's RAG (Retrieval-Augmented Generation) system enables document Q&A, knowledge bases, and long-term conversation memory by combining vector search with large language models.

### Key Features

- **Document Q&A**: Upload PDFs, web pages, or images and ask questions
- **Knowledge Bases**: Organize documents by category (medical, legal, technical)
- **Conversation Memory**: Long-term context retention
- **Multilingual**: 100+ languages supported
- **Privacy-First**: 100% on-device processing (no cloud)
- **Performance**: <2s end-to-end query time

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Document Parser** | Apache PDFBox 3.0.1 | PDF text extraction |
| **Web Scraper** | Jsoup 1.17.1 | HTML to text conversion |
| **OCR** | Tesseract4Android 4.5.0 | Image text recognition |
| **Embedding Model** | MiniLM E5-small-multilingual | 384-dim semantic vectors |
| **Vector Index** | Faiss IndexFlatIP | Fast similarity search |
| **Database** | SQLDelight | Metadata & chunk storage |
| **ONNX Runtime** | Shared with NLU | Embedding inference |

**Total Size**: ~20-25MB APK impact (excluding embedding models)

---

## 2. Architecture

### System Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         AVA RAG System                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────┐ │
│  │  Document       │  │  Embedding       │  │  Vector       │ │
│  │  Ingestion      │→ │  Generation      │→ │  Indexing     │ │
│  │  (PDF/Web/Img)  │  │  (MiniLM)        │  │  (Faiss)      │ │
│  └─────────────────┘  └──────────────────┘  └───────────────┘ │
│           ↓                                          ↓          │
│  ┌─────────────────┐                     ┌───────────────────┐ │
│  │  Text           │                     │  Metadata         │ │
│  │  Chunking       │                     │  Storage          │ │
│  │  (512/128)      │                     │  (SQLDelight)     │ │
│  └─────────────────┘                     └───────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              RAG Query Pipeline                         │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  User Query → Embed → Vector Search → Context Assembly │   │
│  │  → LLM Generation → Response with Sources              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
User Upload PDF
       ↓
[PdfDocumentParser]
       ↓
  ParsedDocument
       ↓
[SemanticTextChunker]
       ↓
  List<TextChunk>
       ↓
[MiniLmEmbeddingGenerator]
       ↓
  List<FloatArray> (384-dim)
       ↓
[FaissVectorIndex]
       ↓
  Indexed & Stored


User Asks Question
       ↓
[MiniLmEmbeddingGenerator]
       ↓
  Query Embedding
       ↓
[FaissVectorIndex.search()]
       ↓
  Top-5 Relevant Chunks
       ↓
[ContextAssembler]
       ↓
  Formatted Context
       ↓
[LLMProvider.chat()]
       ↓
  Response with Citations
```

---

## 3. Document Ingestion

### Supported Formats

#### PDF Documents

**Implementation**: Apache PDFBox 3.0.1

**API:**
```kotlin
class PdfDocumentParser : DocumentParser {
    override suspend fun parse(file: File): ParsedDocument = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { document ->
            val stripper = PDFBoxTextStripper()

            ParsedDocument(
                id = UUID.randomUUID().toString(),
                title = document.documentInformation.title
                    ?: file.nameWithoutExtension,
                author = document.documentInformation.author,
                pages = document.numberOfPages,
                text = stripper.getText(document),
                metadata = mapOf(
                    "format" to "PDF",
                    "file_size" to file.length().toString(),
                    "created_date" to document.documentInformation.creationDate?.toString()
                )
            )
        }
    }
}
```

**Usage:**
```kotlin
val parser = PdfDocumentParser()
val document = parser.parse(File("/path/to/manual.pdf"))
println("Title: ${document.title}, Pages: ${document.pages}")
```

---

#### Web Pages

**Implementation**: Jsoup 1.17.1

**API:**
```kotlin
class WebDocumentParser(
    private val httpClient: OkHttpClient
) : DocumentParser {
    override suspend fun parse(url: String): ParsedDocument = withContext(Dispatchers.IO) {
        val html = httpClient.newCall(Request.Builder().url(url).build())
            .execute()
            .body?.string() ?: throw IOException("Failed to fetch URL")

        val doc = Jsoup.parse(html)

        ParsedDocument(
            id = UUID.randomUUID().toString(),
            title = doc.title(),
            text = doc.body().text(),
            url = url,
            metadata = mapOf(
                "format" to "HTML",
                "fetched_at" to System.currentTimeMillis().toString()
            )
        )
    }
}
```

**Usage:**
```kotlin
val parser = WebDocumentParser(httpClient)
val document = parser.parse("https://docs.example.com/guide")
```

---

#### Images (OCR)

**Implementation**: Tesseract4Android 4.5.0

**API:**
```kotlin
class ImageDocumentParser(
    private val context: Context
) : DocumentParser {
    private lateinit var tessBaseAPI: TessBaseAPI

    override suspend fun parse(file: File): ParsedDocument = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(file.path)

        tessBaseAPI = TessBaseAPI()
        tessBaseAPI.init(context.filesDir.path, "eng")  // English data
        tessBaseAPI.setImage(bitmap)

        val text = tessBaseAPI.utF8Text
        tessBaseAPI.end()

        ParsedDocument(
            id = UUID.randomUUID().toString(),
            title = file.nameWithoutExtension,
            text = text,
            metadata = mapOf(
                "format" to "IMAGE",
                "ocr_confidence" to tessBaseAPI.meanConfidence().toString()
            )
        )
    }
}
```

**Usage:**
```kotlin
val parser = ImageDocumentParser(context)
val document = parser.parse(File("/path/to/screenshot.png"))
```

---

## 4. Text Chunking

### Semantic Chunker

**Purpose**: Split documents into semantically meaningful chunks with overlap for better retrieval.

**Configuration:**
```kotlin
data class ChunkConfig(
    val chunkSize: Int = 512,      // tokens
    val overlap: Int = 128,        // tokens
    val respectSentences: Boolean = true,
    val respectParagraphs: Boolean = true
)
```

**Implementation:**
```kotlin
class SemanticTextChunker(
    private val tokenizer: Tokenizer,
    private val config: ChunkConfig = ChunkConfig()
) {
    suspend fun chunk(document: ParsedDocument): List<TextChunk> = withContext(Dispatchers.Default) {
        val text = document.text
        val sentences = splitIntoSentences(text)

        val chunks = mutableListOf<TextChunk>()
        var currentChunk = StringBuilder()
        var currentTokenCount = 0
        var chunkIndex = 0

        for (sentence in sentences) {
            val sentenceTokens = tokenizer.tokenize(sentence)

            if (currentTokenCount + sentenceTokens.size > config.chunkSize) {
                // Chunk is full, save it
                if (currentChunk.isNotEmpty()) {
                    chunks.add(TextChunk(
                        id = "${document.id}_chunk_$chunkIndex",
                        documentId = document.id,
                        text = currentChunk.toString().trim(),
                        tokens = currentTokenCount,
                        chunkIndex = chunkIndex
                    ))

                    // Start new chunk with overlap
                    currentChunk = StringBuilder(getOverlapText(chunks.last(), config.overlap))
                    currentTokenCount = tokenizer.tokenize(currentChunk.toString()).size
                    chunkIndex++
                }
            }

            currentChunk.append(sentence).append(" ")
            currentTokenCount += sentenceTokens.size
        }

        // Add final chunk
        if (currentChunk.isNotEmpty()) {
            chunks.add(TextChunk(
                id = "${document.id}_chunk_$chunkIndex",
                documentId = document.id,
                text = currentChunk.toString().trim(),
                tokens = currentTokenCount,
                chunkIndex = chunkIndex
            ))
        }

        chunks
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Simple sentence splitter (can use BreakIterator for better results)
        return text.split(Regex("[.!?]\\s+"))
            .filter { it.isNotBlank() }
    }

    private fun getOverlapText(chunk: TextChunk, overlapTokens: Int): String {
        val tokens = tokenizer.tokenize(chunk.text)
        val overlapStartIndex = maxOf(0, tokens.size - overlapTokens)
        return tokens.subList(overlapStartIndex, tokens.size).joinToString(" ")
    }
}
```

**Usage:**
```kotlin
val chunker = SemanticTextChunker(tokenizer)
val chunks = chunker.chunk(document)
println("Created ${chunks.size} chunks")
```

---

## 5. Embedding Generation

### MiniLM Embedding Generator

**Model**: MiniLM E5-small-multilingual (384 dimensions)

**Storage**: Hierarchical (External → APK fallback)

**API:**
```kotlin
class MiniLmEmbeddingGenerator(
    private val context: Context,
    private val languageMode: LanguageMode
) {
    private var onnxSession: OrtSession? = null
    private val embeddingCache = LruCache<String, FloatArray>(1000)

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val modelFile = getModelFile(languageMode)

        val ortEnv = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
            setInterOpNumThreads(2)
            addNnapi()  // Use NNAPI if available
        }

        onnxSession = ortEnv.createSession(modelFile.path, sessionOptions)
    }

    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        // Check cache first
        embeddingCache.get(text)?.let { return@withContext it }

        // Tokenize
        val tokens = tokenizer.encode(text, maxLength = 512)

        // Create ONNX tensors
        val inputIds = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            arrayOf(tokens.ids.toLongArray())
        )
        val attentionMask = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            arrayOf(tokens.attentionMask.toLongArray())
        )

        // Run inference
        val outputs = onnxSession!!.run(mapOf(
            "input_ids" to inputIds,
            "attention_mask" to attentionMask
        ))

        // Extract embedding (last hidden state, mean pooling)
        val embedding = outputs[0].value as Array<FloatArray>
        val normalized = normalize(embedding[0])

        // Cache
        embeddingCache.put(text, normalized)

        normalized
    }

    suspend fun embedBatch(texts: List<String>, batchSize: Int = 32): List<FloatArray> {
        return texts.chunked(batchSize).flatMap { batch ->
            batch.map { embed(it) }
        }
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.map { it * it }.sum())
        return vector.map { it / norm }.toFloatArray()
    }

    private fun getModelFile(mode: LanguageMode): File {
        return when (mode) {
            MULTILINGUAL -> {
                File(
                    ExternalStorageMigration.getEmbeddingsFolder(),
                    "AVA-384-MiniLM-Multi-INT8.AON"
                ).takeIf { it.exists() } ?: getEnglishModel()
            }
            ENGLISH_ONLY -> getEnglishModel()
        }
    }

    private fun getEnglishModel(): File {
        // Fallback to APK assets
        return File(context.filesDir, "minilm-en-int8.onnx")
    }
}
```

**Usage:**
```kotlin
val generator = MiniLmEmbeddingGenerator(context, MULTILINGUAL)
generator.initialize()

// Single embedding
val embedding = generator.embed("What is the meaning of life?")
println("Embedding: ${embedding.size} dimensions")

// Batch processing
val embeddings = generator.embedBatch(chunks.map { it.text })
```

---

## 6. Vector Indexing

### Faiss Vector Index

**Implementation**: Faiss IndexFlatIP (Inner Product search)

**API:**
```kotlin
class FaissVectorIndex(
    private val dimensions: Int = 384,
    private val indexPath: File
) {
    private var index: IndexFlatIP? = null
    private val idMapping = mutableMapOf<Long, String>()  // Faiss ID → Chunk ID

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (indexPath.exists()) {
            // Load existing index
            index = IndexFlatIP.read_index(indexPath.path) as IndexFlatIP
            loadIdMapping()
        } else {
            // Create new index
            index = IndexFlatIP(dimensions.toLong())
        }
    }

    suspend fun add(chunkId: String, embedding: FloatArray) {
        val faissId = index!!.ntotal
        index!!.add(1, embedding)
        idMapping[faissId] = chunkId
    }

    suspend fun addBatch(chunkIds: List<String>, embeddings: List<FloatArray>) {
        val flatEmbeddings = embeddings.flatMap { it.toList() }.toFloatArray()
        val startId = index!!.ntotal

        index!!.add(chunkIds.size.toLong(), flatEmbeddings)

        chunkIds.forEachIndexed { i, chunkId ->
            idMapping[startId + i] = chunkId
        }
    }

    suspend fun search(queryEmbedding: FloatArray, k: Int = 5): List<SearchResult> {
        val distances = FloatArray(k)
        val labels = LongArray(k)

        index!!.search(1, queryEmbedding, k.toLong(), distances, labels)

        return labels.zip(distances).mapNotNull { (faissId, distance) ->
            idMapping[faissId]?.let { chunkId ->
                SearchResult(
                    chunkId = chunkId,
                    score = distance,
                    rank = labels.indexOf(faissId) + 1
                )
            }
        }
    }

    suspend fun save() = withContext(Dispatchers.IO) {
        IndexFlatIP.write_index(index, indexPath.path)
        saveIdMapping()
    }

    private fun saveIdMapping() {
        val mappingFile = File(indexPath.parent, "id_mapping.json")
        mappingFile.writeText(Json.encodeToString(idMapping))
    }

    private fun loadIdMapping() {
        val mappingFile = File(indexPath.parent, "id_mapping.json")
        if (mappingFile.exists()) {
            idMapping.putAll(Json.decodeFromString(mappingFile.readText()))
        }
    }

    fun getStats(): IndexStats {
        return IndexStats(
            totalVectors = index!!.ntotal.toInt(),
            dimensions = dimensions,
            sizeBytes = indexPath.length()
        )
    }
}
```

**Usage:**
```kotlin
val index = FaissVectorIndex(
    dimensions = 384,
    indexPath = File(context.filesDir, "rag/indexes/default.faiss")
)
index.initialize()

// Add embeddings
index.addBatch(chunks.map { it.id }, embeddings)
index.save()

// Search
val results = index.search(queryEmbedding, k = 5)
results.forEach { result ->
    println("Chunk ${result.chunkId}: score ${result.score}")
}
```

---

## 7. RAG Query Pipeline

### Complete Pipeline

**Implementation:**
```kotlin
class RAGQueryPipeline(
    private val embeddingGenerator: MiniLmEmbeddingGenerator,
    private val vectorIndex: FaissVectorIndex,
    private val chunkRepository: ChunkRepository,
    private val llmProvider: LLMProvider,
    private val maxContextTokens: Int = 2048
) {
    suspend fun query(
        userQuery: String,
        k: Int = 5
    ): RAGResponse = withContext(Dispatchers.Default) {

        // 1. Embed user query
        val queryEmbedding = embeddingGenerator.embed(userQuery)

        // 2. Vector search
        val searchResults = vectorIndex.search(queryEmbedding, k)

        // 3. Fetch chunks
        val chunks = chunkRepository.getByIds(searchResults.map { it.chunkId })

        // 4. Assemble context
        val context = assembleContext(chunks, maxContextTokens)

        // 5. Generate LLM response
        val prompt = buildPrompt(userQuery, context)
        val llmResponse = llmProvider.chat(
            messages = listOf(ChatMessage.user(prompt)),
            options = GenerationOptions(temperature = 0.7)
        ).first()  // Take first streaming chunk for simplicity

        // 6. Build response with citations
        RAGResponse(
            answer = llmResponse.text,
            sources = chunks.map { chunk ->
                SourceCitation(
                    documentId = chunk.documentId,
                    chunkId = chunk.id,
                    score = searchResults.first { it.chunkId == chunk.id }.score
                )
            },
            queryTime = measureTime { /* timing */ }.inWholeMilliseconds
        )
    }

    private fun assembleContext(chunks: List<TextChunk>, maxTokens: Int): String {
        val contextBuilder = StringBuilder()
        var tokenCount = 0

        for (chunk in chunks.sortedByDescending { chunk ->
            // Sort by relevance score (from search results)
            chunk.score
        }) {
            val chunkTokens = tokenizer.tokenize(chunk.text).size
            if (tokenCount + chunkTokens > maxTokens) break

            contextBuilder.append("Source: ${chunk.documentTitle}\n")
            contextBuilder.append(chunk.text)
            contextBuilder.append("\n\n")
            tokenCount += chunkTokens
        }

        return contextBuilder.toString()
    }

    private fun buildPrompt(query: String, context: String): String {
        return """
            You are a helpful assistant. Answer the user's question based on the following context.
            If the answer is not in the context, say "I don't have enough information to answer that."

            Context:
            $context

            Question: $query

            Answer:
        """.trimIndent()
    }
}
```

**Usage:**
```kotlin
val pipeline = RAGQueryPipeline(
    embeddingGenerator = generator,
    vectorIndex = index,
    chunkRepository = chunkRepo,
    llmProvider = llmProvider
)

val response = pipeline.query("What is the return policy?")
println("Answer: ${response.answer}")
println("Sources: ${response.sources.size} documents")
```

---

## 8. Storage & Models

### Hierarchical Model Loading

**Strategy**: External → APK fallback

**Paths:**
```
External Storage: /sdcard/.AVAVoiceAvanues/.embeddings/
├── AVA-384-MiniLM-Multi-INT8.AON  (Multilingual, 30MB)
└── AVA-384-Base-INT8.AON          (English, 20MB)

APK Assets: assets/models/rag/
└── minilm-en-int8.onnx            (English fallback, 20MB)
```

**Loading Logic:**
```kotlin
fun loadRAGModel(languageMode: LanguageMode): File {
    return when (languageMode) {
        MULTILINGUAL -> {
            val external = File(
                ExternalStorageMigration.getEmbeddingsFolder(),
                "AVA-384-MiniLM-Multi-INT8.AON"
            )
            external.takeIf { it.exists() } ?: getRagEnglishModel()
        }
        ENGLISH_ONLY -> getRagEnglishModel()
    }
}

private fun getRagEnglishModel(): File {
    // Try external English model first
    val externalEnglish = File(
        ExternalStorageMigration.getEmbeddingsFolder(),
        "AVA-384-Base-INT8.AON"
    )
    if (externalEnglish.exists()) return externalEnglish

    // Fallback to APK assets
    return extractAssetModel("models/rag/minilm-en-int8.onnx")
}
```

---

### Database Schema (SQLDelight)

**Tables:**

```sql
-- Documents
CREATE TABLE IF NOT EXISTS rag_documents (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    author TEXT,
    url TEXT,
    format TEXT NOT NULL,  -- PDF, HTML, IMAGE
    pages INTEGER,
    size_bytes INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    metadata TEXT  -- JSON
);

-- Text Chunks
CREATE TABLE IF NOT EXISTS rag_chunks (
    id TEXT PRIMARY KEY NOT NULL,
    document_id TEXT NOT NULL,
    text TEXT NOT NULL,
    tokens INTEGER NOT NULL,
    chunk_index INTEGER NOT NULL,
    page_number INTEGER,
    FOREIGN KEY (document_id) REFERENCES rag_documents(id) ON DELETE CASCADE
);

-- Chunk Embeddings (cached)
CREATE TABLE IF NOT EXISTS rag_embeddings (
    chunk_id TEXT PRIMARY KEY NOT NULL,
    embedding BLOB NOT NULL,  -- FloatArray serialized
    model_version TEXT NOT NULL,
    FOREIGN KEY (chunk_id) REFERENCES rag_chunks(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_chunks_document ON rag_chunks(document_id);
CREATE INDEX idx_embeddings_model ON rag_embeddings(model_version);
```

**Repository:**
```kotlin
class RAGRepository(
    private val database: Database
) {
    suspend fun saveDocument(document: ParsedDocument) {
        database.ragDocumentsQueries.insert(
            id = document.id,
            title = document.title,
            author = document.author,
            url = document.url,
            format = document.format,
            pages = document.pages,
            size_bytes = document.sizeBytes,
            created_at = System.currentTimeMillis(),
            metadata = Json.encodeToString(document.metadata)
        )
    }

    suspend fun saveChunks(chunks: List<TextChunk>) {
        database.transaction {
            chunks.forEach { chunk ->
                database.ragChunksQueries.insert(
                    id = chunk.id,
                    document_id = chunk.documentId,
                    text = chunk.text,
                    tokens = chunk.tokens.toLong(),
                    chunk_index = chunk.chunkIndex.toLong(),
                    page_number = chunk.pageNumber?.toLong()
                )
            }
        }
    }

    suspend fun getAllDocuments(): List<ParsedDocument> {
        return database.ragDocumentsQueries.selectAll()
            .executeAsList()
            .map { it.toDomain() }
    }

    suspend fun deleteDocument(documentId: String) {
        database.ragDocumentsQueries.deleteById(documentId)
        // Chunks are cascade deleted automatically
    }
}
```

---

## 9. Integration Guide

### Step 1: Add Dependencies

**`build.gradle.kts`:**
```kotlin
dependencies {
    // PDF parsing
    implementation("org.apache.pdfbox:pdfbox-android:3.0.1")

    // Web scraping
    implementation("org.jsoup:jsoup:1.17.1")

    // OCR
    implementation("io.github.adaptech-cz:tesseract4android:4.5.0")

    // Vector search
    implementation("com.facebook.faiss:faiss-android:1.7.4")

    // ONNX (shared with NLU)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
}
```

---

### Step 2: Initialize RAG Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RAGModule {

    @Provides
    @Singleton
    fun provideMiniLmEmbeddingGenerator(
        @ApplicationContext context: Context,
        languageMode: LanguageMode
    ): MiniLmEmbeddingGenerator {
        return MiniLmEmbeddingGenerator(context, languageMode).apply {
            runBlocking { initialize() }
        }
    }

    @Provides
    @Singleton
    fun provideFaissVectorIndex(
        @ApplicationContext context: Context
    ): FaissVectorIndex {
        val indexPath = File(context.filesDir, "rag/indexes/default.faiss")
        return FaissVectorIndex(384, indexPath).apply {
            runBlocking { initialize() }
        }
    }

    @Provides
    @Singleton
    fun provideRAGQueryPipeline(
        generator: MiniLmEmbeddingGenerator,
        index: FaissVectorIndex,
        chunkRepo: ChunkRepository,
        llmProvider: LLMProvider
    ): RAGQueryPipeline {
        return RAGQueryPipeline(generator, index, chunkRepo, llmProvider)
    }
}
```

---

### Step 3: Upload Document

```kotlin
// In your ViewModel
suspend fun uploadDocument(uri: Uri) {
    viewModelScope.launch {
        try {
            _uploadState.value = UploadState.Loading

            // 1. Parse document
            val parser = PdfDocumentParser()
            val document = parser.parse(uriToFile(uri))

            // 2. Chunk text
            val chunker = SemanticTextChunker(tokenizer)
            val chunks = chunker.chunk(document)

            // 3. Generate embeddings
            val embeddings = embeddingGenerator.embedBatch(chunks.map { it.text })

            // 4. Index vectors
            vectorIndex.addBatch(chunks.map { it.id }, embeddings)
            vectorIndex.save()

            // 5. Save to database
            ragRepository.saveDocument(document)
            ragRepository.saveChunks(chunks)

            _uploadState.value = UploadState.Success(document.id)
        } catch (e: Exception) {
            _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
        }
    }
}
```

---

### Step 4: Query Documents

```kotlin
// In your ViewModel
suspend fun askQuestion(query: String) {
    viewModelScope.launch {
        try {
            _queryState.value = QueryState.Loading

            val response = ragPipeline.query(query, k = 5)

            _queryState.value = QueryState.Success(response)
        } catch (e: Exception) {
            _queryState.value = QueryState.Error(e.message ?: "Query failed")
        }
    }
}
```

---

## 10. Performance Optimization

### Benchmarks

| Operation | Target | Actual |
|-----------|--------|--------|
| **PDF Parsing** | <5s per 100 pages | ~3s |
| **Text Chunking** | <500ms per 100 pages | ~200ms |
| **Embedding (Single)** | <10ms | ~5ms (NNAPI) |
| **Embedding (Batch 32)** | <200ms | ~120ms |
| **Vector Search (k=5)** | <100ms | ~30ms |
| **Full Pipeline** | <2s | ~1.5s |

### Optimization Strategies

**1. Batch Processing**
```kotlin
// Bad: Sequential embedding
chunks.forEach { chunk ->
    val embedding = generator.embed(chunk.text)  // Slow!
}

// Good: Batch embedding
val embeddings = generator.embedBatch(
    chunks.map { it.text },
    batchSize = 32
)
```

**2. Caching**
```kotlin
private val embeddingCache = LruCache<String, FloatArray>(1000)

suspend fun embed(text: String): FloatArray {
    embeddingCache.get(text)?.let { return it }
    // ... generate embedding
    embeddingCache.put(text, embedding)
    return embedding
}
```

**3. Background Processing**
```kotlin
// Offload heavy work to background thread
viewModelScope.launch(Dispatchers.IO) {
    val chunks = chunker.chunk(document)
    val embeddings = generator.embedBatch(chunks.map { it.text })
}
```

**4. NNAPI Acceleration**
```kotlin
val sessionOptions = OrtSession.SessionOptions().apply {
    addNnapi()  // Use Android Neural Networks API
    setIntraOpNumThreads(4)
}
```

---

## 11. Testing

### Unit Tests

```kotlin
class RAGQueryPipelineTest {

    @Test
    fun `query returns relevant results`() = runTest {
        // Arrange
        val mockEmbedding = FloatArray(384) { Random.nextFloat() }
        val mockGenerator = mockk<MiniLmEmbeddingGenerator> {
            coEvery { embed(any()) } returns mockEmbedding
        }

        val pipeline = RAGQueryPipeline(
            embeddingGenerator = mockGenerator,
            vectorIndex = mockIndex,
            chunkRepository = mockChunkRepo,
            llmProvider = mockLLMProvider
        )

        // Act
        val response = pipeline.query("What is the return policy?")

        // Assert
        assertThat(response.answer).isNotEmpty()
        assertThat(response.sources).hasSize(5)
        assertThat(response.queryTime).isLessThan(2000)
    }
}
```

---

### Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class RAGIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var ragPipeline: RAGQueryPipeline

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testFullRAGPipeline() = runTest {
        // 1. Upload test document
        val testPdf = File(context.cacheDir, "test.pdf")
        // ... create test PDF

        val parser = PdfDocumentParser()
        val document = parser.parse(testPdf)

        // 2. Ingest and index
        // ... chunking, embedding, indexing

        // 3. Query
        val response = ragPipeline.query("What is this document about?")

        // 4. Verify
        assertThat(response.answer).isNotEmpty()
        assertThat(response.sources).isNotEmpty()
    }
}
```

---

## 12. Troubleshooting

### Issue: Embeddings are all zeros

**Symptoms:**
- Vector search returns random results
- All embeddings identical

**Diagnosis:**
```kotlin
val embedding = generator.embed("test")
println("Embedding: ${embedding.take(10).joinToString()}")
// Should NOT be: [0.0, 0.0, 0.0, ...]
```

**Causes:**
1. ONNX model not loaded correctly
2. Tokenization failed (empty input)
3. ONNX Runtime backend error

**Solutions:**
- Check model file exists: `modelFile.exists()`
- Verify tokenization: `tokenizer.encode("test")`
- Try CPU backend: Remove `addNnapi()` call

---

### Issue: Vector search is slow (>500ms)

**Symptoms:**
- Search takes multiple seconds
- UI freezes during search

**Diagnosis:**
```kotlin
val startTime = System.currentTimeMillis()
val results = index.search(queryEmbedding, k=5)
println("Search time: ${System.currentTimeMillis() - startTime}ms")
```

**Causes:**
1. Index too large (>100k vectors)
2. Search on main thread
3. Faiss not optimized

**Solutions:**
1. Use IndexIVFFlat for large datasets:
   ```kotlin
   val index = IndexIVFFlat(quantizer, dimensions, nlist = 100)
   ```
2. Always search on background thread:
   ```kotlin
   withContext(Dispatchers.Default) {
       index.search(embedding, k)
   }
   ```

---

### Issue: Out of memory during batch embedding

**Error:**
```
java.lang.OutOfMemoryError: Failed to allocate tensor
```

**Cause**: Batch size too large

**Solution**: Reduce batch size
```kotlin
val embeddings = generator.embedBatch(
    chunks.map { it.text },
    batchSize = 16  // Reduce from 32
)
```

---

## Related Documentation

- **ADR-012**: [RAG System Architecture](architecture/android/ADR-012-RAG-System-Architecture.md)
- **RAG Spec**: [rag-system-spec.md](specifications/rag-system-spec.md)
- **Developer Manual Chapter 50**: [External Storage Migration](Developer-Manual-Chapter50-External-Storage-Migration.md)
- **Developer Manual Chapter 51**: [3-Letter JSON Schema](Developer-Manual-Chapter51-3Letter-JSON-Schema.md)
- **User Manual Chapter 12**: [Using RAG Features](User-Manual-Chapter12-RAG-Features.md)

---

**Version**: 1.0
**Date**: 2025-11-27
**Author**: Manoj Jhawar
**Maintained By**: AVA AI Team
