# ADR-012: RAG (Retrieval-Augmented Generation) System Architecture

**Status:** Accepted
**Date:** 2025-11-27
**Authors:** AVA AI Team
**Related:** RAG Module, Developer Manual Chapter 52, User Manual Chapter 12
**Dependencies:** ADR-008 (Hardware-Aware Inference), ADR-010 (External Storage), ADR-011 (3-Letter Schema)

---

## Context

AVA's core functionality allows users to ask questions and get responses from the on-device LLM. However, the LLM's knowledge is limited to its training data (cutoff date) and cannot answer questions about:

### Problems Identified

1. **No Document Understanding**: Cannot answer questions about user's documents
   - User has a 150-page PDF manual
   - User asks: "How do I reset this device?"
   - LLM cannot access PDF content
   - **Result**: Generic or incorrect answer

2. **No Domain Expertise**: Cannot provide specialized knowledge
   - Medical professionals need medical reference lookups
   - Legal professionals need case law search
   - Technical users need specification documents
   - **Result**: LLM gives general answers, not domain-specific

3. **No Long-Term Memory**: Cannot remember past conversations
   - User discusses topic in detail
   - Returns next day, asks follow-up
   - LLM has no memory of previous conversation
   - **Result**: User must re-explain context every time

4. **Static Knowledge**: Training data becomes outdated
   - LLM trained on 2023 data
   - User needs 2025 information
   - No mechanism to update knowledge
   - **Result**: Outdated responses

5. **No Browser Search Memory** (Future): Cannot remember what user searched for
   - User searches Google for "best Python frameworks 2025"
   - Later asks AVA: "What was that framework I looked up?"
   - AVA has no access to search history
   - **Result**: Cannot help with recent discoveries

### Requirements

- **Document Ingestion**: Upload PDF, web pages, images (OCR)
- **Semantic Search**: Find relevant content in <100ms
- **Context Assembly**: Provide relevant chunks to LLM
- **Source Citation**: Tell user which document/page
- **Multilingual**: 100+ language support
- **Privacy-First**: All processing on-device
- **Storage Efficient**: Compact embeddings and indexes
- **Fast**: <2 second end-to-end query time

---

## Decision

Implement a **complete RAG system** with document ingestion, semantic chunking, vector indexing, and LLM integration.

### Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                      AVA RAG System                           │
└───────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ↓                    ↓                    ↓
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   INGESTION     │  │   RETRIEVAL     │  │  GENERATION     │
│                 │  │                 │  │                 │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │
│ │ PDF Parser  │ │  │ │ Query       │ │  │ │ Context     │ │
│ │ Web Scraper │ │  │ │ Embedding   │ │  │ │ Assembly    │ │
│ │ Image OCR   │ │  │ │             │ │  │ │             │ │
│ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │
│        ↓         │  │        ↓         │  │        ↓         │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │
│ │ Text        │ │  │ │ Vector      │ │  │ │ LLM         │ │
│ │ Chunking    │ │  │ │ Search      │ │  │ │ Generation  │ │
│ │ (512/128)   │ │  │ │ (Faiss)     │ │  │ │ (TVM)       │ │
│ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │
│        ↓         │  │        ↓         │  │        ↓         │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │
│ │ Embedding   │ │  │ │ Chunk       │ │  │ │ Response    │ │
│ │ Generation  │ │  │ │ Retrieval   │ │  │ │ with        │ │
│ │ (MiniLM)    │ │  │ │ (Top-K)     │ │  │ │ Sources     │ │
│ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │
│        ↓         │  │                 │  │                 │
│ ┌─────────────┐ │  │                 │  │                 │
│ │ Vector      │ │  │                 │  │                 │
│ │ Indexing    │ │  │                 │  │                 │
│ │ (Faiss)     │ │  │                 │  │                 │
│ └─────────────┘ │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              ↓
                    ┌──────────────────┐
                    │  SQLDelight DB   │
                    │  (Metadata)      │
                    └──────────────────┘
```

---

## Components

### 1. Document Ingestion

**Technology Choices:**

| Document Type | Library | Size | Justification |
|--------------|---------|------|---------------|
| **PDF** | Apache PDFBox 3.0.1 | ~5MB | Industry standard, Android compatible |
| **Web** | Jsoup 1.17.1 | ~450KB | Simple HTML parsing, no JS execution |
| **Images** | Tesseract4Android 4.5.0 | ~10MB | Best OCR for Android, 95%+ accuracy |

**Design Decision:** Use separate parsers per document type (not unified)

**Rationale:**
- Each type requires different processing
- Allows optimization per type
- Easy to add new types later
- Failover: If PDF fails, others still work

**Alternative Considered:** Unified DocumentParser interface with plugins
- **Rejected**: Over-engineered for 3 document types

### 2. Text Chunking

**Strategy:** Semantic chunking with overlap

**Parameters:**
- Chunk size: **512 tokens**
- Overlap: **128 tokens** (25%)
- Respect: Sentence boundaries, paragraph boundaries

**Rationale:**
- 512 tokens = optimal for MiniLM (max 512)
- 128 overlap = context preservation across chunks
- Sentence respect = no mid-sentence cuts
- Paragraph respect = semantic coherence

**Alternative Considered:** Fixed character chunking (2000 chars)
- **Rejected**: Breaks sentences, loses context

**Alternative Considered:** No overlap
- **Rejected**: Context loss between chunks

### 3. Embedding Generation

**Model:** MiniLM E5-small-multilingual

**Decision Matrix:**

| Model | Size | Dims | Languages | Accuracy | Choice |
|-------|------|------|-----------|----------|--------|
| MiniLM L6-v2 | 20MB | 384 | English | 95% | ✅ APK |
| E5-small-multi | 30MB | 384 | 100+ | 94% | ✅ External |
| mALBERT | 82MB | 768 | 52 | 96% | ❌ Too large |
| MobileBERT | 22MB | 384 | English | 94% | ❌ Already used for NLU |

**Rationale:**
- **384 dimensions**: Same as NLU (can share infrastructure)
- **Multilingual**: Matches AVA's language strategy
- **Hybrid storage**: English in APK (20MB), multilingual external (30MB)
- **INT8 quantization**: <1% accuracy loss, 4x smaller

**Design Decision:** Reuse NLU's ONNX Runtime infrastructure

**Benefits:**
- No additional runtime dependency
- Shared ONNX optimizations
- Consistent performance profile
- Lower total APK size

### 4. Vector Indexing

**Technology:** Faiss (Facebook AI Similarity Search)

**Index Type:** `IndexFlatIP` (Inner Product, flat index)

**Rationale:**
- **Flat index**: Exact search, no approximation
- **Inner Product**: Natural for normalized embeddings
- **Performance**: <100ms for 100K vectors
- **Simplicity**: No training required
- **Accuracy**: 100% recall (exact search)

**Alternative Considered:** ANN (Approximate Nearest Neighbors)
- **Examples**: HNSW, IVF
- **Rejected**: Overkill for <100K vectors, slower on mobile

**Alternative Considered:** SQLite with custom similarity
- **Rejected**: Too slow (>1 second for 10K vectors)

**Faiss Android:**
- **Library**: `com.github.luhenry.swigfaiss:faiss-android:1.7.4`
- **Size**: ~3MB
- **JNI**: Native library (optimized C++)

### 5. Storage Strategy

**Hierarchical Model Loading:**

```
Priority 1: External Multilingual (if language mode = multilingual)
  /sdcard/.AVAVoiceAvanues/.embeddings/AVA-384-MiniLM-Multi-INT8.AON (30MB)

Priority 2: External English
  /sdcard/.AVAVoiceAvanues/.embeddings/AVA-384-MiniLM-Base-INT8.AON (20MB)

Priority 3: Bundled English (always available)
  assets/models/AVA-384-MiniLM-Base-INT8.AON (20MB)
```

**Metadata Storage:**

```
Internal Storage (SQLDelight):
  /data/data/com.augmentalis.ava/databases/rag.db
  - rag_documents (title, author, page_count, file_path)
  - rag_chunks (document_id, text, start_index, page_number)
  - rag_knowledge_bases (name, description, category)
  - rag_kb_documents (junction table)
```

**Vector Indices:**

```
Internal Storage (Faiss):
  /data/data/com.augmentalis.ava/files/rag-indices/
  - default.faiss           (Global index)
  - default.mapping         (Faiss ID → Chunk ID)
  - medical.faiss           (Knowledge base indices)
  - legal.faiss
```

**Original Documents:**

```
Internal Storage:
  /data/data/com.augmentalis.ava/files/rag-documents/
  - {uuid}.pdf              (Original files)
  - {uuid}.json             (ava-rag-1.0 metadata)
```

**Storage Estimates:**
- 100-page PDF: ~50MB (text + embeddings + index)
- 1000-page PDF: ~500MB
- Recommended limit: 10GB per user

### 6. RAG Query Pipeline

**Flow:**

```
User Query
    ↓
1. Embed query with MiniLM (5ms)
    ↓
2. Vector search in Faiss (50ms, k=5)
    ↓
3. Retrieve top-5 chunks from DB (10ms)
    ↓
4. Assemble context (trim to 2048 tokens) (5ms)
    ↓
5. Build LLM prompt with context (5ms)
    ↓
6. LLM generation (1500ms)
    ↓
7. Parse sources from response (10ms)
    ↓
Response with citations
```

**Total:** <2 seconds (target met)

**LLM Prompt Template:**

```
Context from documents:
---
Document: user-manual.pdf, Page: 42
"To reset the device, press and hold the power button for 10 seconds..."

Document: user-manual.pdf, Page: 15
"The power button is located on the right side of the device..."
---

User question: How do I reset this device?

Instructions:
- Answer based ONLY on the context above
- Cite the document name and page number
- If context doesn't contain the answer, say "I don't have that information in your documents"

Answer:
```

### 7. Integration with Language Mode

**Unified Setting:**

```kotlin
enum class LanguageMode {
    ENGLISH_ONLY,
    MULTILINGUAL
}

// Affects:
// 1. NLU model (MobileBERT)
// 2. RAG model (MiniLM)
// 3. LLM model (future)

val nluModel = when (languageMode) {
    MULTILINGUAL -> AVA-384-Multi-INT8.AON
    ENGLISH_ONLY -> AVA-384-Base-INT8.AON
}

val ragModel = when (languageMode) {
    MULTILINGUAL -> AVA-384-MiniLM-Multi-INT8.AON
    ENGLISH_ONLY -> AVA-384-MiniLM-Base-INT8.AON
}
```

---

## Alternatives Considered

### Alternative 1: Cloud-Based RAG

**Approach:** Send documents to cloud, query cloud index

**Pros:**
- Unlimited storage
- Faster index building (powerful servers)
- Can use larger models

**Cons:**
- Privacy violation (documents leave device)
- Requires internet
- Monthly costs
- Latency (network round-trip)
- **REJECTED** (violates AVA's privacy-first principle)

### Alternative 2: Keyword-Based Search

**Approach:** Use SQLite FTS (Full-Text Search) instead of vector search

**Pros:**
- Simpler implementation
- No embedding model needed
- Smaller storage

**Cons:**
- No semantic understanding (exact keyword match only)
- Poor recall (misses synonyms, paraphrases)
- No multilingual support
- **REJECTED** (insufficient quality)

### Alternative 3: No Chunking (Whole Document)

**Approach:** Embed entire document as single vector

**Pros:**
- Simpler implementation
- No chunking logic needed
- Fewer vectors to index

**Cons:**
- Context too large (LLM can't handle)
- Poor precision (whole document not relevant)
- Loses granularity (can't cite specific pages)
- **REJECTED** (unusable for long documents)

### Alternative 4: Use Existing NLU Model for RAG

**Approach:** Reuse MobileBERT for both NLU and RAG embeddings

**Pros:**
- No additional model
- Save 20-30MB

**Cons:**
- MobileBERT not optimized for document embeddings
- Lower accuracy for retrieval (-10%)
- Different use case (classification vs similarity)
- **REJECTED** (quality matters more than size)

---

## Consequences

### Positive

✅ **Document Q&A**: Users can ask questions about their PDFs
✅ **Knowledge Bases**: Domain-specific expertise (medical, legal)
✅ **Long-Term Memory**: Conversation history as RAG documents
✅ **Multilingual**: 100+ language support
✅ **Privacy**: 100% on-device, no cloud
✅ **Fast**: <2 second queries
✅ **Accurate**: >80% relevance (vector search)
✅ **Cited Sources**: User knows where answer came from
✅ **Ecosystem Consistency**: 3-letter JSON schema

### Negative

⚠️ **APK Size**: +20-25MB (PDFBox, Faiss, Tesseract)
  - Mitigation: Acceptable for core feature
  - Mitigation: Only English model in APK (20MB)

⚠️ **Storage Usage**: 50MB per 100-page PDF
  - Mitigation: User controls uploads
  - Mitigation: 10GB limit (200 PDFs)
  - Mitigation: Delete unused documents

⚠️ **Memory Footprint**: +200MB for embedding model
  - Mitigation: Lazy load (only when RAG used)
  - Mitigation: Shared ONNX Runtime with NLU

⚠️ **Complexity**: 5 components vs simple Q&A
  - Mitigation: Modular design (each component independent)
  - Mitigation: Comprehensive testing

⚠️ **OCR Accuracy**: Images may have errors
  - Mitigation: Tesseract 95%+ accuracy
  - Mitigation: User can review extracted text

---

## Implementation

### Phase 1: Foundation (Week 1)
- [ ] Create RAG module structure
- [ ] Implement PDF parser (Apache PDFBox)
- [ ] Implement web scraper (Jsoup)
- [ ] Implement text chunker (512/128)
- [ ] SQLDelight schema (documents, chunks, KBs)
- [ ] Unit tests (20+ tests)

### Phase 2: Embeddings & Indexing (Week 2)
- [ ] Integrate MiniLM ONNX model
- [ ] Implement embedding generator
- [ ] Integrate Faiss Android
- [ ] Implement vector index
- [ ] Hierarchical model loading
- [ ] Performance tests (<100ms search)

### Phase 3: RAG Pipeline (Week 2-3)
- [ ] RAG query coordinator
- [ ] Context assembly logic
- [ ] LLM integration (prompt templates)
- [ ] Source citation extraction
- [ ] End-to-end integration tests

### Phase 4: UI & Management (Week 3)
- [ ] Document upload screen
- [ ] Document library screen
- [ ] RAG settings screen
- [ ] Storage usage display
- [ ] Privacy controls
- [ ] Knowledge base management

### Phase 5: Polish & Testing (Week 3)
- [ ] Error handling
- [ ] Progress indicators
- [ ] Background processing (WorkManager)
- [ ] Memory optimization
- [ ] Battery optimization
- [ ] Device testing (3 devices)

---

## Testing Strategy

### Unit Tests

```kotlin
class SemanticTextChunkerTest {
    @Test fun `chunk respects sentence boundaries`()
    @Test fun `chunk overlap works correctly`()
    @Test fun `chunk handles empty text`()
}

class MiniLmEmbeddingGeneratorTest {
    @Test fun `embedding dimensions are correct`()
    @Test fun `embeddings are normalized`()
    @Test fun `batch embedding works`()
}

class FaissVectorIndexTest {
    @Test fun `search returns top k results`()
    @Test fun `add and search are consistent`()
    @Test fun `persist and load work`()
}
```

### Integration Tests

```kotlin
class RAGPipelineIntegrationTest {
    @Test fun `full RAG pipeline works end-to-end`() {
        // 1. Ingest PDF
        // 2. Chunk text
        // 3. Generate embeddings
        // 4. Index vectors
        // 5. Query
        // 6. Verify results
    }
}
```

### Performance Tests

```kotlin
class RAGPerformanceTest {
    @Test fun `PDF processing meets 30s target`()
    @Test fun `vector search meets 100ms target`()
    @Test fun `full pipeline meets 2s target`()
}
```

---

## Success Metrics

### Quality
- Answer relevance: >80% (user survey)
- Source citation accuracy: >95%
- Multilingual support: 100+ languages working

### Performance
- PDF processing: <30s per 100 pages
- Vector search: <100ms for k=5
- Full query: <2s end-to-end

### Coverage
- Test coverage: >90%
- Edge case handling: 20+ scenarios
- Error recovery: Graceful degradation

---

## References

- **Full Specification:** `docs/specifications/rag-system-spec.md`
- **3-Letter Schema:** `docs/standards/AVA-3LETTER-JSON-SCHEMA.md`
- **External Storage:** `docs/build/EXTERNAL-STORAGE-SETUP.md`
- **Related ADRs:**
  - ADR-008: Hardware-Aware Inference Backend
  - ADR-010: External Storage Migration
  - ADR-011: 3-Letter JSON Schema Standard
- **Papers:**
  - [RAG: Retrieval-Augmented Generation](https://arxiv.org/abs/2005.11401)
  - [Dense Passage Retrieval](https://arxiv.org/abs/2004.04906)
  - [Faiss: A Library for Efficient Similarity Search](https://arxiv.org/abs/1702.08734)

---

## Changelog

**v1.0 (2025-11-27):**
- Initial ADR
- Complete RAG system architecture
- 5 components defined
- Implementation plan created

---

**Status:** ✅ ACCEPTED
**Implementation:** 0% (specification complete, ready to start)
**Risk Level:** Medium (complex system, but well-scoped)
**Estimated Effort:** 3 weeks (5 phases)
