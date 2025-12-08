# RAG Phase 2: Document Processing Specification

**Feature ID:** RAG-P2
**Created:** 2025-11-04
**Status:** Ready for Implementation
**Dependencies:** RAG Phase 1 (Foundation) ✅ Complete

---

## Overview

Implement document processing capabilities for the RAG system, enabling AVA to parse PDF documents, extract text chunks, and generate embeddings for semantic search.

---

## Goals

1. **PDF Parsing**: Extract text from PDF files with structure preservation
2. **Text Chunking**: Split documents into semantic chunks (512 tokens with 50 token overlap)
3. **Embedding Generation**: Generate vector embeddings using ONNX Runtime
4. **Repository Implementation**: Create in-memory repository for testing

---

## Requirements

### Functional Requirements

**FR1: PDF Document Parsing**
- Parse PDF files using native libraries
- Extract page-by-page text content
- Detect and preserve document structure (headings, sections)
- Extract metadata (title, author, page count)
- Support Android, iOS, and Desktop platforms

**FR2: Semantic Text Chunking**
- Split text into chunks of max 512 tokens
- Use 50-token overlap between chunks
- Respect section boundaries when possible
- Preserve metadata (page number, section, heading)
- Classify chunk semantic type (HEADING, PARAGRAPH, etc.)

**FR3: Embedding Generation**
- Use all-MiniLM-L6-v2 ONNX model (384 dimensions)
- Support batch processing (32 texts per batch)
- Implement int8 quantization for space savings
- Provide ONNX, Local LLM, and Cloud API provider options
- Platform-specific optimizations (GPU on Android, CPU on iOS)

**FR4: Repository Implementation**
- In-memory storage for testing
- Document lifecycle management (PENDING → PROCESSING → INDEXED)
- Chunk storage with embeddings
- Basic linear search implementation
- Statistics and metrics tracking

### Non-Functional Requirements

**NFR1: Performance**
- PDF parsing: <5 seconds for 100-page document
- Chunking: >1000 chunks/second
- Embedding: 100-200 chunks/second on mobile
- Search: <100ms for 10k chunks (linear search)

**NFR2: Quality**
- Test coverage: ≥80%
- No memory leaks
- Proper error handling
- Comprehensive logging

**NFR3: Cross-Platform**
- Same API across Android, iOS, Desktop
- Platform-specific optimizations
- Graceful degradation when features unavailable

---

## Architecture

### Components

```
RAG Module (Phase 2)
├── parser/
│   ├── PDFParser (Android/iOS/Desktop implementations)
│   ├── TextChunker (semantic chunking logic)
│   └── StructureDetector (heading detection)
├── embeddings/
│   ├── ONNXEmbeddingProvider (ONNX Runtime integration)
│   ├── LocalLLMEmbeddingProvider (stub for Phase 3)
│   └── CloudEmbeddingProvider (stub for Phase 3)
├── data/
│   ├── InMemoryRAGRepository (testing implementation)
│   └── ChunkCache (LRU cache)
└── processing/
    └── DocumentProcessor (orchestrates parse → chunk → embed)
```

### Data Flow

```
1. User adds document → addDocument()
2. Repository marks as PENDING
3. processDocuments() triggers processing:
   a. PDFParser.parse() → ParsedDocument
   b. TextChunker.chunk() → List<Chunk>
   c. EmbeddingProvider.embedBatch() → List<Embedding>
   d. Repository stores chunks with embeddings
   e. Document marked as INDEXED
4. User searches → search()
   a. Generate query embedding
   b. Compare with all chunk embeddings (linear for now)
   c. Return top-K results
```

---

## Implementation Tasks

### Phase 2.1: PDF Parsing (4 hours)

**Task P2.1.1: Android PDF Parser**
- Use Android PdfRenderer API (no external dependencies)
- Extract text page by page
- Implement basic heading detection
- Handle malformed PDFs gracefully

**Task P2.1.2: iOS PDF Parser**
- Use PDFKit framework
- Extract text with structure
- Match Android feature parity

**Task P2.1.3: Desktop PDF Parser**
- Use Apache PDFBox (full version)
- Advanced structure detection
- Metadata extraction

**Task P2.1.4: Structure Detection**
- Heuristic-based heading detection:
  - ALL CAPS lines
  - Numbered sections (1., 1.1, etc.)
  - Short lines followed by whitespace
- Section extraction
- Page number tracking

### Phase 2.2: Text Chunking (3 hours)

**Task P2.2.1: Token Counter**
- Simple whitespace tokenizer
- Accurate token counting
- Unicode handling

**Task P2.2.2: Semantic Chunker**
- Respect paragraph boundaries
- 512 token max per chunk
- 50 token overlap
- Preserve section context

**Task P2.2.3: Metadata Preservation**
- Track source document
- Page numbers
- Section headings
- Semantic type classification

### Phase 2.3: Embedding Generation (5 hours)

**Task P2.3.1: ONNX Model Integration**
- Download all-MiniLM-L6-v2 ONNX model
- Initialize ONNX Runtime
- Tokenization pipeline
- Inference implementation

**Task P2.3.2: Batch Processing**
- Implement batch embedding (32 texts)
- Memory-efficient processing
- Progress tracking

**Task P2.3.3: Quantization**
- Implement int8 quantization
- Validate accuracy (<5% loss)
- Space savings verification

**Task P2.3.4: Platform Optimizations**
- Android: GPU acceleration if available
- iOS: CPU optimizations
- Desktop: Multi-threading

### Phase 2.4: Repository Implementation (4 hours)

**Task P2.4.1: In-Memory Storage**
- Document storage (HashMap)
- Chunk storage (List)
- Thread-safe operations
- Memory limits

**Task P2.4.2: Document Lifecycle**
- addDocument implementation
- processDocuments implementation
- Status tracking
- Error handling

**Task P2.4.3: Search Implementation**
- Query embedding generation
- Linear similarity search
- Result ranking
- Filter support

**Task P2.4.4: Statistics & Metrics**
- Document counts by status
- Chunk counts
- Storage usage
- Performance metrics

### Phase 2.5: Testing (4 hours)

**Task P2.5.1: Unit Tests**
- PDF parser tests (mock documents)
- Chunker tests (boundary conditions)
- Embedding tests (consistency)
- Repository tests (lifecycle)

**Task P2.5.2: Integration Tests**
- End-to-end document processing
- Search accuracy tests
- Performance benchmarks
- Error handling tests

**Task P2.5.3: Test Documents**
- Create test PDF corpus
- Various sizes (1-100 pages)
- Different structures
- Edge cases (empty, malformed)

---

## File Changes

### New Files

```
Universal/AVA/Features/RAG/src/
├── commonMain/kotlin/.../rag/
│   ├── parser/
│   │   ├── TextChunker.kt                    # NEW
│   │   ├── StructureDetector.kt              # NEW
│   │   └── TokenCounter.kt                   # NEW
│   ├── embeddings/
│   │   └── ONNXEmbeddingProvider.kt          # NEW (common interface)
│   ├── data/
│   │   ├── InMemoryRAGRepository.kt          # NEW
│   │   └── ChunkCache.kt                     # NEW
│   └── processing/
│       └── DocumentProcessor.kt               # NEW
├── androidMain/kotlin/.../rag/
│   ├── parser/
│   │   └── PDFParser.android.kt              # IMPLEMENT
│   └── embeddings/
│       └── ONNXEmbeddingProvider.android.kt  # NEW
├── iosMain/kotlin/.../rag/
│   ├── parser/
│   │   └── PDFParser.ios.kt                  # IMPLEMENT
│   └── embeddings/
│       └── ONNXEmbeddingProvider.ios.kt      # NEW
└── desktopMain/kotlin/.../rag/
    ├── parser/
    │   └── PDFParser.desktop.kt              # IMPLEMENT
    └── embeddings/
        └── ONNXEmbeddingProvider.desktop.kt  # NEW
```

### Test Files

```
Universal/AVA/Features/RAG/src/
├── commonTest/kotlin/.../rag/
│   ├── parser/
│   │   ├── TextChunkerTest.kt                # NEW
│   │   └── StructureDetectorTest.kt          # NEW
│   ├── embeddings/
│   │   └── QuantizationTest.kt               # NEW
│   ├── data/
│   │   └── InMemoryRAGRepositoryTest.kt      # NEW
│   └── integration/
│       └── DocumentProcessingTest.kt         # NEW
└── androidTest/kotlin/.../rag/
    ├── parser/
    │   └── PDFParserTest.kt                  # NEW
    └── embeddings/
        └── ONNXEmbeddingProviderTest.kt      # NEW
```

---

## Dependencies

### Gradle Updates

**build.gradle.kts** (RAG module):

```kotlin
val androidMain by getting {
    dependencies {
        // PDF parsing (Android native)
        implementation("androidx.core:core-ktx:1.12.0")

        // ONNX Runtime
        implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
    }
}

val desktopMain by getting {
    dependencies {
        // PDF parsing
        implementation("org.apache.pdfbox:pdfbox:3.0.0")

        // ONNX Runtime
        implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")
    }
}
```

### External Resources

**ONNX Model:**
- Model: all-MiniLM-L6-v2
- Format: ONNX
- Source: Hugging Face
- Size: ~90MB
- Location: `assets/models/all-MiniLM-L6-v2.onnx`

---

## Testing Strategy

### Unit Tests (Target: 85% coverage)

- **Parser Tests**: Mock PDFs, structure detection accuracy
- **Chunker Tests**: Token counting, overlap verification, boundary conditions
- **Embedding Tests**: Quantization accuracy, batch processing
- **Repository Tests**: Lifecycle, search, error handling

### Integration Tests

- **End-to-End**: PDF → Parse → Chunk → Embed → Index → Search
- **Performance**: Benchmark all operations
- **Error Cases**: Malformed PDFs, OOM, missing models

### Manual Testing

- Real PDFs (technical docs, books, papers)
- Various sizes and formats
- Search quality evaluation
- Performance profiling

---

## Success Criteria

1. ✅ PDF parser extracts text with >95% accuracy
2. ✅ Chunker creates valid chunks with proper overlap
3. ✅ ONNX embeddings generated successfully
4. ✅ Quantization maintains >95% search accuracy
5. ✅ End-to-end document processing works
6. ✅ Search returns relevant results
7. ✅ All tests passing (>80% coverage)
8. ✅ Performance targets met

---

## Risks & Mitigations

### Risk 1: PDF Parsing Complexity
**Impact:** High
**Probability:** Medium
**Mitigation:** Start with Android native APIs (simple), add features incrementally
**Contingency:** Fall back to text-only extraction, defer structure detection to Phase 3

### Risk 2: ONNX Model Size
**Impact:** Medium
**Probability:** Low
**Mitigation:** Use quantized model (INT8), lazy loading
**Contingency:** Offer cloud-only embedding for low-end devices

### Risk 3: Performance on Low-End Devices
**Impact:** High
**Probability:** Medium
**Mitigation:** Aggressive optimization, background processing, user settings
**Contingency:** Offload to desktop/cloud, process fewer documents

### Risk 4: Search Accuracy
**Impact:** High
**Probability:** Low
**Mitigation:** Thorough testing with real documents, tune similarity thresholds
**Contingency:** Add keyword fallback, improve chunking strategy

---

## Timeline

**Total Estimated Time:** 20 hours (2.5 days)

- Phase 2.1: PDF Parsing - 4 hours
- Phase 2.2: Text Chunking - 3 hours
- Phase 2.3: Embedding Generation - 5 hours
- Phase 2.4: Repository Implementation - 4 hours
- Phase 2.5: Testing - 4 hours

**Target Completion:** End of Week 2

---

## Next Phase Preview (Phase 3)

After Phase 2 completion, Phase 3 will implement:
- SQLite-vec integration for persistent storage
- Cluster-based indexing (k-means)
- LRU cache for hot data
- Desktop sync protocol

---

## References

- RAG Implementation Plan: `docs/active/RAG-System-Implementation-Plan.md`
- Phase 1 Progress: `docs/active/RAG-Phase1-Progress-251104.md`
- Domain Models: `Universal/AVA/Features/RAG/src/commonMain/kotlin/.../domain/`
