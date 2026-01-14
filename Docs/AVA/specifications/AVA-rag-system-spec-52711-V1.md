# RAG System Specification

**Feature:** Complete RAG (Retrieval-Augmented Generation) System
**Version:** 1.0
**Date:** 2025-11-27
**Status:** Approved
**Priority:** P1 - High
**Platforms:** Android (KMP-ready for future iOS/Desktop)
**Estimated Effort:** 2-3 weeks

---

## Executive Summary

Implement a complete RAG system for AVA that enables users to:
1. Upload and ingest documents (PDF, web, images)
2. Ask questions about their documents
3. Build personalized knowledge bases
4. Maintain long-term conversation memory
5. *(Future)* Remember browser search history

**Key Features:**
- ✅ Document Q&A (PDFs, manuals, research papers)
- ✅ Domain-specific knowledge bases (medical, legal, technical)
- ✅ Long-term memory for conversations
- ✅ Multilingual support (100+ languages)
- ✅ On-device privacy (all processing local)
- 🔮 Browser search memory integration (future backlog)

---

## Table of Contents

1. [Business Value](#business-value)
2. [Architecture Overview](#architecture-overview)
3. [Requirements](#requirements)
4. [Component Specifications](#component-specifications)
5. [Data Models](#data-models)
6. [Storage Strategy](#storage-strategy)
7. [Integration Points](#integration-points)
8. [Testing Strategy](#testing-strategy)
9. [Deployment Plan](#deployment-plan)
10. [Future Enhancements](#future-enhancements)

---

## Business Value

### User Stories

**US-01: Document Q&A**
> "As a user, I want to upload a PDF manual and ask AVA questions about it, so I can quickly find information without reading the entire document."

**Acceptance Criteria:**
- User can upload PDF via UI
- AVA extracts text and creates searchable index
- User asks questions in natural language
- AVA returns relevant answers with source citations
- Response time: <2 seconds for query

**US-02: Knowledge Base**
> "As a professional, I want to load domain-specific documents (medical, legal) into AVA, so I can get expert-level assistance in my field."

**Acceptance Criteria:**
- Multiple documents per knowledge base
- Category/tag organization
- Cross-document search
- Source attribution (which document?)

**US-03: Conversation Memory**
> "As a user, I want AVA to remember past conversations and context, so I get personalized responses over time."

**Acceptance Criteria:**
- Conversation history stored as RAG documents
- Contextual awareness in responses
- Privacy controls (opt-in/opt-out)
- Ability to delete memory

**US-04: Multilingual Support**
> "As a multilingual user, I want to upload documents in any language and ask questions in my preferred language, so I can use AVA globally."

**Acceptance Criteria:**
- 100+ language support
- Cross-language query (ask in English, search Spanish docs)
- Automatic language detection

---

## Architecture Overview

### System Components

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
│                                                                 │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────┐ │
│  │  RAG Management │  │  Settings UI     │  │  Language     │ │
│  │  UI             │  │  (Privacy)       │  │  Mode Control │ │
│  └─────────────────┘  └──────────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology | Version | Size |
|-----------|-----------|---------|------|
| **Document Parsing** | Apache PDFBox | 3.0.1 | ~5MB |
| **Web Scraping** | Jsoup | 1.17.1 | ~450KB |
| **Image OCR** | Tesseract4Android | 4.5.0 | ~10MB |
| **Embedding Model** | MiniLM E5-small | 1.0 | 30MB (multi) / 20MB (en) |
| **ONNX Runtime** | ONNX Runtime | 1.16.3 | Shared with NLU |
| **Vector Index** | Faiss-Android | 1.7.4 | ~3MB |
| **Metadata DB** | SQLDelight | 2.0.1 | Shared |
| **Text Chunking** | LangChain4j (port) | Custom | ~50KB |

**Total APK Impact:** ~20-25MB (excluding embedding models)

---

## Requirements

### Functional Requirements

#### FR-01: Document Ingestion
- **SHALL** support PDF document upload
- **SHALL** extract text from PDFs with >95% accuracy
- **SHALL** support web page URL ingestion
- **SHALL** support image upload with OCR
- **SHALL** handle documents up to 100MB
- **SHALL** process documents in background thread
- **SHALL** show progress indicator during processing
- **SHOULD** support EPUB, DOCX, TXT (future)

#### FR-02: Text Chunking
- **SHALL** split documents into 512-token chunks
- **SHALL** use 128-token overlap between chunks
- **SHALL** preserve sentence boundaries
- **SHALL** maintain metadata (page number, section)
- **SHALL** handle multi-column layouts
- **SHALL** respect paragraph boundaries

#### FR-03: Embedding Generation
- **SHALL** use MiniLM E5-small-multilingual (384-dim)
- **SHALL** support 100+ languages
- **SHALL** generate embeddings in <10ms per chunk (CPU)
- **SHALL** batch process chunks (batch size: 32)
- **SHALL** normalize embeddings (L2 norm)
- **SHALL** cache embeddings to disk

#### FR-04: Vector Indexing
- **SHALL** use Faiss IndexFlatIP (Inner Product)
- **SHALL** support up to 100,000 vectors per index
- **SHALL** search in <100ms (k=5 nearest neighbors)
- **SHALL** persist index to disk
- **SHALL** rebuild index on model change
- **SHALL** support multiple indexes (per knowledge base)

#### FR-05: RAG Query Pipeline
- **SHALL** embed user query with same model
- **SHALL** retrieve top-k (k=5) relevant chunks
- **SHALL** assemble context for LLM (max 2048 tokens)
- **SHALL** generate response with on-device LLM or cloud
- **SHALL** cite sources (document name, page number)
- **SHALL** complete full pipeline in <2 seconds

#### FR-06: RAG Management UI
- **SHALL** provide document upload interface
- **SHALL** show document library (list view)
- **SHALL** display document details (title, pages, size, chunks)
- **SHALL** allow document deletion
- **SHALL** show storage usage
- **SHALL** export/import RAG datasets

#### FR-07: Language Mode Integration
- **SHALL** respect unified language mode setting
- **SHALL** use English model when mode = "English-only"
- **SHALL** use Multilingual model when mode = "Multilingual"
- **SHALL** load models from hierarchical storage
- **SHALL** fallback gracefully if external model missing

#### FR-08: Privacy & Security
- **SHALL** process all documents on-device
- **SHALL** require explicit user consent for RAG
- **SHALL** provide opt-in/opt-out toggle
- **SHALL** encrypt RAG database
- **SHALL** delete all data on user request
- **SHALL** never send documents to cloud (without explicit consent)

### Non-Functional Requirements

#### NFR-01: Performance
- PDF processing: <30 seconds per 100 pages
- Embedding generation: <10ms per chunk
- Vector search: <100ms for k=5
- Full RAG query: <2 seconds end-to-end
- UI responsiveness: <100ms for all actions

#### NFR-02: Storage
- Embedding model: 30MB (multilingual) / 20MB (English)
- Faiss index: ~4 bytes × dimensions × vectors
  - Example: 4 × 384 × 10,000 = 15.36MB
- SQLDelight metadata: ~1KB per chunk
- Total for 1000-page PDF: ~50MB

#### NFR-03: Memory
- Peak memory: <512MB during processing
- Faiss index in memory: <100MB
- Embedding model: ~200MB resident
- Background processing: Use WorkManager

#### NFR-04: Battery
- Document processing: <5% battery per 100 pages
- Idle state: 0% battery drain
- Query processing: <0.1% battery per query

#### NFR-05: Compatibility
- Android API 28+ (Android 9.0+)
- ARMv8 (64-bit) only
- Future: iOS, Desktop (Kotlin Multiplatform ready)

---

## Component Specifications

### Component 1: Document Ingestion

**Module:** `Universal/AVA/Features/RAG/Ingestion`

#### 1.1 PDF Parser

**Class:** `PdfDocumentParser`

```kotlin
interface DocumentParser {
    suspend fun parse(file: File): ParsedDocument
}

data class ParsedDocument(
    val title: String,
    val author: String?,
    val pages: Int,
    val text: String,
    val metadata: Map<String, String>,
    val pageTexts: List<PageText>
)

data class PageText(
    val pageNumber: Int,
    val text: String,
    val bounds: List<TextBound>?
)

class PdfDocumentParser : DocumentParser {
    private val pdfBox = PDFBoxTextStripper()

    override suspend fun parse(file: File): ParsedDocument = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { document ->
            val text = pdfBox.getText(document)
            val info = document.documentInformation

            ParsedDocument(
                title = info.title ?: file.nameWithoutExtension,
                author = info.author,
                pages = document.numberOfPages,
                text = text,
                metadata = extractMetadata(info),
                pageTexts = extractPageTexts(document)
            )
        }
    }
}
```

**Dependencies:**
- `org.apache.pdfbox:pdfbox-android:3.0.1` (~5MB)

#### 1.2 Web Scraper

**Class:** `WebPageScraper`

```kotlin
class WebPageScraper : DocumentParser {
    private val jsoup = Jsoup.connect()

    override suspend fun parse(url: String): ParsedDocument = withContext(Dispatchers.IO) {
        val doc = jsoup.url(url).get()

        ParsedDocument(
            title = doc.title(),
            author = doc.select("meta[name=author]").attr("content"),
            pages = 1,
            text = doc.body().text(),
            metadata = mapOf(
                "url" to url,
                "domain" to URI(url).host
            ),
            pageTexts = listOf(PageText(1, doc.body().text(), null))
        )
    }
}
```

**Dependencies:**
- `org.jsoup:jsoup:1.17.1` (~450KB)

#### 1.3 Image OCR

**Class:** `ImageOcrParser`

```kotlin
class ImageOcrParser(private val context: Context) : DocumentParser {
    private val tesseract = TessBaseAPI()

    init {
        // Initialize Tesseract with language data
        tesseract.init(context.filesDir.absolutePath, "eng+spa+fra")
    }

    override suspend fun parse(file: File): ParsedDocument = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        tesseract.setImage(bitmap)

        val text = tesseract.utF8Text

        ParsedDocument(
            title = file.nameWithoutExtension,
            author = null,
            pages = 1,
            text = text,
            metadata = mapOf(
                "type" to "image",
                "format" to file.extension
            ),
            pageTexts = listOf(PageText(1, text, null))
        )
    }
}
```

**Dependencies:**
- `cz.adaptech.tesseract4android:tesseract4android:4.5.0` (~10MB + language data)

---

### Component 2: Text Chunking

**Module:** `Universal/AVA/Features/RAG/Chunking`

**Class:** `SemanticTextChunker`

```kotlin
data class ChunkConfig(
    val chunkSize: Int = 512,      // tokens
    val overlap: Int = 128,        // tokens
    val respectSentences: Boolean = true,
    val respectParagraphs: Boolean = true
)

data class TextChunk(
    val id: String,                // UUID
    val text: String,              // Chunk text
    val startIndex: Int,           // Start position in document
    val endIndex: Int,             // End position in document
    val pageNumber: Int?,          // Page number (if available)
    val section: String?,          // Section/heading (if available)
    val tokenCount: Int            // Actual token count
)

class SemanticTextChunker(
    private val tokenizer: Tokenizer,
    private val config: ChunkConfig = ChunkConfig()
) {
    suspend fun chunk(document: ParsedDocument): List<TextChunk> {
        val chunks = mutableListOf<TextChunk>()

        // Split by paragraphs first (if enabled)
        val paragraphs = if (config.respectParagraphs) {
            document.text.split("\n\n")
        } else {
            listOf(document.text)
        }

        for (paragraph in paragraphs) {
            // Split by sentences
            val sentences = if (config.respectSentences) {
                splitSentences(paragraph)
            } else {
                listOf(paragraph)
            }

            var currentChunk = StringBuilder()
            var currentTokens = 0
            var chunkStart = 0

            for (sentence in sentences) {
                val sentenceTokens = tokenizer.tokenize(sentence).size

                if (currentTokens + sentenceTokens > config.chunkSize && currentTokens > 0) {
                    // Create chunk
                    chunks.add(createChunk(
                        text = currentChunk.toString(),
                        startIndex = chunkStart,
                        pageNumber = findPageNumber(document, chunkStart)
                    ))

                    // Start new chunk with overlap
                    val overlapText = getOverlapText(currentChunk.toString(), config.overlap)
                    currentChunk = StringBuilder(overlapText)
                    currentTokens = tokenizer.tokenize(overlapText).size
                    chunkStart += currentChunk.length - overlapText.length
                }

                currentChunk.append(sentence).append(" ")
                currentTokens += sentenceTokens
            }

            // Add remaining chunk
            if (currentChunk.isNotEmpty()) {
                chunks.add(createChunk(
                    text = currentChunk.toString(),
                    startIndex = chunkStart,
                    pageNumber = findPageNumber(document, chunkStart)
                ))
            }
        }

        return chunks
    }

    private fun splitSentences(text: String): List<String> {
        // Simple sentence boundary detection
        return text.split(Regex("[.!?]+\\s+"))
    }
}
```

---

### Component 3: Embedding Generation

**Module:** `Universal/AVA/Features/RAG/Embeddings`

**Class:** `MiniLmEmbeddingGenerator`

```kotlin
class MiniLmEmbeddingGenerator(
    private val context: Context,
    private val languageMode: LanguageMode
) {
    private var onnxSession: OrtSession? = null
    private val embeddingCache = LruCache<String, FloatArray>(1000)

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val modelFile = getModelFile(languageMode)

        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions()
        sessionOptions.setInterOpNumThreads(4)
        sessionOptions.setIntraOpNumThreads(4)

        onnxSession = env.createSession(modelFile.readBytes(), sessionOptions)

        Log.i(TAG, "MiniLM embedding model loaded: $languageMode")
    }

    /**
     * Generate embedding for single text
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        // Check cache first
        embeddingCache.get(text)?.let { return@withContext it }

        val tokens = tokenize(text)
        val inputIds = LongArray(tokens.size) { tokens[it].toLong() }
        val attentionMask = LongArray(tokens.size) { 1L }

        val inputTensor = OnnxTensor.createTensor(
            onnxSession!!.ortEnvironment,
            arrayOf(inputIds)
        )

        val attentionTensor = OnnxTensor.createTensor(
            onnxSession!!.ortEnvironment,
            arrayOf(attentionMask)
        )

        val outputs = onnxSession!!.run(mapOf(
            "input_ids" to inputTensor,
            "attention_mask" to attentionTensor
        ))

        val embedding = outputs[0].value as Array<FloatArray>
        val normalized = normalize(embedding[0])

        // Cache result
        embeddingCache.put(text, normalized)

        return@withContext normalized
    }

    /**
     * Generate embeddings for batch of texts
     */
    suspend fun embedBatch(texts: List<String>, batchSize: Int = 32): List<FloatArray> {
        return texts.chunked(batchSize).flatMap { batch ->
            batch.map { embed(it) }
        }
    }

    private fun getModelFile(mode: LanguageMode): File {
        val storage = ExternalStorageMigration
        val embeddingsFolder = storage.getEmbeddingsFolder()

        return when (mode) {
            LanguageMode.MULTILINGUAL -> {
                // PRIORITY 1: External multilingual
                File(embeddingsFolder, "AVA-384-MiniLM-Multi-INT8.AON")
                    .takeIf { it.exists() }
                    ?: getEnglishModel()  // Fallback
            }
            LanguageMode.ENGLISH_ONLY -> getEnglishModel()
        }
    }

    private fun getEnglishModel(): File {
        // Check external first
        val externalEnglish = File(
            ExternalStorageMigration.getEmbeddingsFolder(),
            "AVA-384-MiniLM-Base-INT8.AON"
        )
        if (externalEnglish.exists()) return externalEnglish

        // Fallback to bundled
        val internalFile = File(context.filesDir, "models/AVA-384-MiniLM-Base-INT8.AON")
        if (!internalFile.exists()) {
            // Copy from assets
            context.assets.open("models/AVA-384-MiniLM-Base-INT8.AON").use { input ->
                internalFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return internalFile
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { it * it.toDouble() }).toFloat()
        return FloatArray(vector.size) { vector[it] / norm }
    }
}
```

---

### Component 4: Vector Indexing (Faiss)

**Module:** `Universal/AVA/Features/RAG/Index`

**Class:** `FaissVectorIndex`

```kotlin
class FaissVectorIndex(
    private val dimensions: Int = 384,
    private val indexPath: File
) {
    private var index: IndexFlatIP? = null
    private val idMapping = mutableMapOf<Long, String>()  // Faiss ID → Chunk ID

    suspend fun initialize() = withContext(Dispatchers.IO) {
        index = if (indexPath.exists()) {
            // Load existing index
            read_index(indexPath.absolutePath) as IndexFlatIP
        } else {
            // Create new index
            IndexFlatIP(dimensions)
        }

        Log.i(TAG, "Faiss index initialized: ${index!!.ntotal} vectors")
    }

    suspend fun add(chunkId: String, embedding: FloatArray) {
        require(embedding.size == dimensions) {
            "Embedding dimension mismatch: ${embedding.size} != $dimensions"
        }

        val faissId = index!!.ntotal
        index!!.add(floatArrayOf(embedding))
        idMapping[faissId] = chunkId
    }

    suspend fun addBatch(chunks: List<Pair<String, FloatArray>>) {
        val embeddings = FloatArray(chunks.size * dimensions)

        chunks.forEachIndexed { idx, (chunkId, embedding) ->
            System.arraycopy(embedding, 0, embeddings, idx * dimensions, dimensions)
            val faissId = index!!.ntotal + idx
            idMapping[faissId] = chunkId
        }

        index!!.add(chunks.size.toLong(), embeddings)
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

    suspend fun persist() = withContext(Dispatchers.IO) {
        write_index(index, indexPath.absolutePath)

        // Save ID mapping
        val mappingFile = File(indexPath.parent, "${indexPath.nameWithoutExtension}.mapping")
        mappingFile.writeText(Json.encodeToString(idMapping))
    }

    fun getStats(): IndexStats {
        return IndexStats(
            totalVectors = index!!.ntotal.toInt(),
            dimensions = dimensions,
            sizeBytes = indexPath.length()
        )
    }
}

data class SearchResult(
    val chunkId: String,
    val score: Float,
    val rank: Int
)

data class IndexStats(
    val totalVectors: Int,
    val dimensions: Int,
    val sizeBytes: Long
)
```

**Dependencies:**
- `com.github.luhenry.swigfaiss:faiss-android:1.7.4` (~3MB)

---

## Data Models

### 3-Letter JSON Schema

#### RAG Document Metadata: `ava-rag-1.0`

**File:** `ava-rag-metadata.json`

```json
{
  "sch": "ava-rag-1.0",
  "ver": "1.0.0",
  "met": {
    "doc": "user-manual.pdf",
    "tit": "AVA User Manual",
    "aut": "Augmentalis",
    "dat": "2025-11-27T10:30:00Z",
    "siz": 5242880,
    "pag": 150,
    "typ": "pdf",
    "lan": "en"
  },
  "cfg": {
    "chu": 512,
    "ovr": 128,
    "emb": "AVA-384-MiniLM-Multi-INT8.AON",
    "idx": "faiss"
  },
  "sta": {
    "chu_cnt": 450,
    "vec_cnt": 450,
    "idx_siz": 1728000,
    "pro_tim": 28500
  }
}
```

### SQLDelight Schema

**File:** `RAGDatabase.sq`

```sql
-- Documents table
CREATE TABLE IF NOT EXISTS rag_documents (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    author TEXT,
    page_count INTEGER,
    file_path TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    file_type TEXT NOT NULL,
    language TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Chunks table
CREATE TABLE IF NOT EXISTS rag_chunks (
    id TEXT NOT NULL PRIMARY KEY,
    document_id TEXT NOT NULL,
    text TEXT NOT NULL,
    start_index INTEGER NOT NULL,
    end_index INTEGER NOT NULL,
    page_number INTEGER,
    section TEXT,
    token_count INTEGER NOT NULL,
    embedding_id INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (document_id) REFERENCES rag_documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_chunks_document ON rag_chunks(document_id);
CREATE INDEX idx_chunks_embedding ON rag_chunks(embedding_id);

-- Knowledge bases table
CREATE TABLE IF NOT EXISTS rag_knowledge_bases (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT,
    document_count INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Document-KnowledgeBase junction
CREATE TABLE IF NOT EXISTS rag_kb_documents (
    knowledge_base_id TEXT NOT NULL,
    document_id TEXT NOT NULL,
    added_at INTEGER NOT NULL,
    PRIMARY KEY (knowledge_base_id, document_id),
    FOREIGN KEY (knowledge_base_id) REFERENCES rag_knowledge_bases(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES rag_documents(id) ON DELETE CASCADE
);

-- Queries
selectAllDocuments:
SELECT * FROM rag_documents ORDER BY created_at DESC;

selectDocumentById:
SELECT * FROM rag_documents WHERE id = ?;

selectChunksByDocument:
SELECT * FROM rag_chunks WHERE document_id = ? ORDER BY start_index;

selectChunkById:
SELECT * FROM rag_chunks WHERE id = ?;

selectChunksByEmbeddingIds:
SELECT * FROM rag_chunks WHERE embedding_id IN ?;

insertDocument:
INSERT INTO rag_documents VALUES ?;

insertChunk:
INSERT INTO rag_chunks VALUES ?;

deleteDocument:
DELETE FROM rag_documents WHERE id = ?;

getStorageStats:
SELECT
    COUNT(*) as document_count,
    SUM(file_size) as total_size,
    (SELECT COUNT(*) FROM rag_chunks) as chunk_count
FROM rag_documents;
```

---

## Storage Strategy

### Model Storage (Hybrid)

```
APK (always available):
  assets/models/
  ├── AVA-384-Base-INT8.AON             (22MB) NLU English
  └── AVA-384-MiniLM-Base-INT8.AON      (20MB) RAG English

External Storage (optional upgrade):
  /sdcard/.AVAVoiceAvanues/.embeddings/
  ├── AVA-384-Multi-INT8.AON            (113MB) NLU Multilingual
  └── AVA-384-MiniLM-Multi-INT8.AON     (30MB)  RAG Multilingual
```

### RAG Data Storage

```
Internal Storage:
  /data/data/com.augmentalis.ava/
  ├── databases/
  │   └── rag.db                         (SQLDelight metadata)
  │
  ├── files/
  │   ├── rag-indices/
  │   │   ├── default.faiss              (Faiss vector index)
  │   │   ├── default.mapping            (ID mapping)
  │   │   ├── medical.faiss              (Knowledge base indices)
  │   │   └── legal.faiss
  │   │
  │   └── rag-documents/
  │       ├── {doc-id}.pdf               (Original files)
  │       └── {doc-id}.json              (ava-rag-1.0 metadata)
```

**Storage Estimates:**
- 100-page PDF: ~50MB (embeddings + index + metadata)
- 1000-page PDF: ~500MB
- Recommended limit: 10GB per user

---

## Integration Points

### 1. Language Mode Setting

**Module:** `Universal/AVA/Core/Settings`

```kotlin
enum class LanguageMode {
    ENGLISH_ONLY,
    MULTILINGUAL
}

data class AVASettings(
    val languageMode: LanguageMode = LanguageMode.ENGLISH_ONLY,
    val ragEnabled: Boolean = false,
    val ragStorageLimit: Long = 10_000_000_000L,  // 10GB
    val confidenceThreshold: Float = 0.5f
)

interface SettingsRepository {
    suspend fun getLanguageMode(): LanguageMode
    suspend fun setLanguageMode(mode: LanguageMode)
    suspend fun isRagEnabled(): Boolean
    suspend fun setRagEnabled(enabled: Boolean)
}
```

**Affects:**
- NLU model selection
- RAG embedding model selection
- LLM model selection (future)

### 2. NLU Integration

**Reuse existing ModelManager:**

```kotlin
// Universal/AVA/Features/NLU/ModelManager.kt (ALREADY EXISTS)

// Same hierarchical loading for RAG embeddings
val ragModel = when (settings.getLanguageMode()) {
    MULTILINGUAL -> {
        File("/sdcard/.AVAVoiceAvanues/.embeddings/AVA-384-MiniLM-Multi-INT8.AON")
            .takeIf { it.exists() }
            ?: getRagEnglishModel()
    }
    ENGLISH_ONLY -> getRagEnglishModel()
}
```

### 3. LLM Integration

**Module:** `Universal/AVA/Features/LLM`

```kotlin
data class RAGContext(
    val chunks: List<TextChunk>,
    val sources: List<DocumentSource>
)

data class DocumentSource(
    val documentTitle: String,
    val pageNumber: Int?,
    val score: Float
)

interface LLMProvider {
    suspend fun generate(
        query: String,
        context: RAGContext?,
        options: GenerationOptions
    ): LLMResponse
}

// RAG-aware prompt template
fun buildRAGPrompt(query: String, context: RAGContext): String {
    return """
Context from documents:
${context.chunks.joinToString("\n\n") { "- ${it.text}" }}

User question: $query

Please answer based on the context above. Cite the source document and page number.
""".trimIndent()
}
```

---

## Testing Strategy

### Unit Tests

**Module:** `Universal/AVA/Features/RAG/src/test`

```kotlin
class SemanticTextChunkerTest {
    @Test
    fun `chunk respects sentence boundaries`() {
        val text = "First sentence. Second sentence. Third sentence."
        val chunker = SemanticTextChunker(tokenizer, ChunkConfig(chunkSize = 10))

        val chunks = chunker.chunk(ParsedDocument(text = text, ...))

        assertTrue(chunks.all { it.text.trim().endsWith(".") })
    }

    @Test
    fun `chunk overlap works correctly`() {
        val text = "A".repeat(1000)
        val chunker = SemanticTextChunker(tokenizer, ChunkConfig(
            chunkSize = 100,
            overlap = 20
        ))

        val chunks = chunker.chunk(ParsedDocument(text = text, ...))

        // Check overlap between consecutive chunks
        for (i in 0 until chunks.size - 1) {
            val overlapText = chunks[i].text.takeLast(20)
            assertTrue(chunks[i + 1].text.startsWith(overlapText))
        }
    }
}

class MiniLmEmbeddingGeneratorTest {
    @Test
    fun `embedding dimensions are correct`() = runTest {
        val generator = MiniLmEmbeddingGenerator(context, LanguageMode.ENGLISH_ONLY)
        generator.initialize()

        val embedding = generator.embed("test text")

        assertEquals(384, embedding.size)
    }

    @Test
    fun `embeddings are normalized`() = runTest {
        val generator = MiniLmEmbeddingGenerator(context, LanguageMode.ENGLISH_ONLY)
        generator.initialize()

        val embedding = generator.embed("test text")
        val norm = sqrt(embedding.sumOf { it * it.toDouble() })

        assertEquals(1.0, norm, 0.01)  // L2 norm = 1
    }
}

class FaissVectorIndexTest {
    @Test
    fun `search returns top k results`() = runTest {
        val index = FaissVectorIndex(dimensions = 384, indexPath = tempFile())
        index.initialize()

        // Add vectors
        index.add("chunk1", FloatArray(384) { 1.0f })
        index.add("chunk2", FloatArray(384) { 0.5f })
        index.add("chunk3", FloatArray(384) { 0.8f })

        // Search
        val results = index.search(FloatArray(384) { 1.0f }, k = 2)

        assertEquals(2, results.size)
        assertEquals("chunk1", results[0].chunkId)  // Highest similarity
    }
}
```

### Integration Tests

```kotlin
class RAGPipelineIntegrationTest {
    @Test
    fun `full RAG pipeline works end-to-end`() = runTest {
        // 1. Ingest document
        val pdfFile = File("test-resources/sample.pdf")
        val parser = PdfDocumentParser()
        val document = parser.parse(pdfFile)

        // 2. Chunk text
        val chunker = SemanticTextChunker(tokenizer)
        val chunks = chunker.chunk(document)

        // 3. Generate embeddings
        val embeddingGen = MiniLmEmbeddingGenerator(context, LanguageMode.ENGLISH_ONLY)
        embeddingGen.initialize()
        val embeddings = embeddingGen.embedBatch(chunks.map { it.text })

        // 4. Index vectors
        val index = FaissVectorIndex(dimensions = 384, indexPath = tempFile())
        index.initialize()
        chunks.zip(embeddings).forEach { (chunk, embedding) ->
            index.add(chunk.id, embedding)
        }

        // 5. Query
        val query = "What is the main topic?"
        val queryEmbedding = embeddingGen.embed(query)
        val results = index.search(queryEmbedding, k = 5)

        // 6. Verify
        assertFalse(results.isEmpty())
        assertTrue(results[0].score > 0.5f)  // Relevant result
    }
}
```

### Performance Tests

```kotlin
class RAGPerformanceTest {
    @Test
    fun `PDF processing meets performance target`() = runTest {
        val pdfFile = File("test-resources/100-page-manual.pdf")

        val startTime = System.currentTimeMillis()

        val parser = PdfDocumentParser()
        val document = parser.parse(pdfFile)

        val elapsedTime = System.currentTimeMillis() - startTime

        assertTrue(elapsedTime < 30_000, "Processing took ${elapsedTime}ms, expected <30s")
    }

    @Test
    fun `vector search meets latency target`() = runTest {
        val index = createIndexWith10kVectors()
        val queryEmbedding = FloatArray(384) { Random.nextFloat() }

        val startTime = System.nanoTime()

        val results = index.search(queryEmbedding, k = 5)

        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000

        assertTrue(elapsedMs < 100, "Search took ${elapsedMs}ms, expected <100ms")
    }
}
```

---

## Deployment Plan

### Phase 1: Foundation (Week 1)
- [ ] Setup RAG module structure
- [ ] Implement document parsers (PDF, Web)
- [ ] Implement text chunking
- [ ] Unit tests for parsers and chunking
- [ ] SQLDelight schema migration

### Phase 2: Embeddings & Indexing (Week 2)
- [ ] Integrate MiniLM ONNX model
- [ ] Implement embedding generation
- [ ] Integrate Faiss Android
- [ ] Implement vector indexing
- [ ] Performance tests (<100ms search)

### Phase 3: RAG Pipeline (Week 2-3)
- [ ] Implement RAG query coordinator
- [ ] Context assembly logic
- [ ] LLM integration for generation
- [ ] Source citation system
- [ ] End-to-end integration tests

### Phase 4: UI & Management (Week 3)
- [ ] Document upload screen
- [ ] Document library screen
- [ ] RAG settings screen
- [ ] Storage usage display
- [ ] Privacy controls (opt-in/opt-out)

### Phase 5: Polish & Testing (Week 3)
- [ ] Error handling
- [ ] Progress indicators
- [ ] Background processing (WorkManager)
- [ ] Memory optimization
- [ ] Battery optimization
- [ ] Device testing

---

## Future Enhancements

### Phase 2 (Month 2-3)

**FE-01: Browser Search Memory**
- Integrate with Android Accessibility Service
- Capture Google/browser searches
- Store as RAG documents
- Privacy controls (opt-in required)

**FE-02: Advanced Document Types**
- EPUB support
- DOCX support
- Markdown support
- Code files (syntax-aware chunking)

**FE-03: Conversation Memory**
- Store chat history as RAG documents
- Semantic search over past conversations
- "Remember when we talked about..."

**FE-04: Knowledge Graph**
- Extract entities and relationships
- Build knowledge graph from documents
- Graph-based retrieval

**FE-05: Multi-Modal RAG**
- Image embeddings (CLIP)
- Video transcription + RAG
- Audio transcription + RAG

---

## Success Criteria

### Functional Success
- ✅ User can upload PDF and ask questions
- ✅ Answers are accurate (>80% relevance)
- ✅ Sources are cited correctly
- ✅ Multilingual support works (100+ languages)
- ✅ All 5 phases complete

### Performance Success
- ✅ PDF processing: <30s per 100 pages
- ✅ Vector search: <100ms for k=5
- ✅ Full RAG query: <2s end-to-end
- ✅ 90%+ test coverage

### User Satisfaction
- ✅ Privacy controls clearly explained
- ✅ Storage usage manageable (<500MB for typical use)
- ✅ Battery impact minimal (<5% per 100 pages)
- ✅ UI responsive and intuitive

---

## References

- **MiniLM Model:** https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2
- **E5 Multilingual:** https://huggingface.co/intfloat/multilingual-e5-small
- **Faiss Documentation:** https://github.com/facebookresearch/faiss
- **Apache PDFBox:** https://pdfbox.apache.org/
- **3-Letter Schema:** `docs/standards/AVA-3LETTER-JSON-SCHEMA.md`
- **External Storage:** `docs/build/EXTERNAL-STORAGE-SETUP.md`

---

**Status:** ✅ SPECIFICATION COMPLETE
**Ready for:** Implementation planning and task breakdown
**Estimated Delivery:** 3 weeks from start
