# AVA Project - Next Steps

**Updated:** 2025-11-04
**Last Commit:** 281a0f5 - feat(rag): implement Phase 1 & 2 foundation for RAG system
**Branch:** development

---

## ✅ Just Completed

**Commit Summary:**
- 31 files changed, 5,707 insertions
- RAG Phase 1 (Foundation) - 100% complete
- RAG Phase 2 (Document Processing) - 70% complete
- Universal Theme System - 100% complete
- Build status: ✅ SUCCESS (all platforms)

**Key Achievements:**
1. Complete RAG domain model architecture
2. Text chunking with 3 strategies (914 lines)
3. In-memory repository with semantic search
4. Android PDF parser structure
5. Cross-platform theme engine
6. Comprehensive documentation (4 docs)

---

## 🎯 Immediate Next Steps (Priority Order)

### 1. ONNX Embedding Provider (Est. 4 hours) 🔴 HIGH

**Why Critical:** Unblocks document processing and search functionality

**Tasks:**
```
[ ] Download all-MiniLM-L6-v2 ONNX model (~90MB)
    URL: huggingface.co/sentence-transformers/all-MiniLM-L6-v2

[ ] Add model to assets/models/ directory

[ ] Implement ONNXEmbeddingProvider for Android
    - Initialize ONNX Runtime
    - Implement tokenization
    - Batch processing (32 texts)
    - Int8 quantization support

[ ] Create iOS implementation (stub or native)

[ ] Create Desktop implementation (full JVM version)

[ ] Test embedding generation accuracy
```

**Files to Create:**
```
Universal/AVA/Features/RAG/src/
├── androidMain/kotlin/.../embeddings/
│   └── ONNXEmbeddingProvider.android.kt
├── iosMain/kotlin/.../embeddings/
│   └── ONNXEmbeddingProvider.ios.kt
└── desktopMain/kotlin/.../embeddings/
    └── ONNXEmbeddingProvider.desktop.kt
```

**Success Criteria:**
- Generates 384-dim embeddings
- Processes 100 chunks in <1 second
- Quantization works (75% space savings)

---

### 2. Unit Testing Suite (Est. 3 hours) 🟡 MEDIUM

**Why Important:** Ensure correctness before optimizing

**Test Files to Create:**
```
Universal/AVA/Features/RAG/src/commonTest/kotlin/.../rag/
├── parser/
│   ├── TokenCounterTest.kt
│   └── TextChunkerTest.kt
├── data/
│   └── InMemoryRAGRepositoryTest.kt
└── domain/
    ├── ChunkTest.kt (quantization accuracy)
    └── EmbeddingTest.kt (similarity calculation)
```

**Coverage Target:** 85%

**Key Tests:**
- Token counting accuracy (±10%)
- Chunk overlap correctness
- Semantic chunking preserves structure
- Search ranking correctness
- Quantization accuracy (>95%)

---

### 3. Integration Testing (Est. 2 hours) 🟡 MEDIUM

**Test Scenarios:**
```kotlin
// End-to-end document processing
test_AddDocumentProcessSearch()
test_MultiDocumentIndexing()
test_SearchAccuracyWithRealDocs()

// Performance benchmarks
test_Chunking1000ChunksPerSecond()
test_Embedding100ChunksPerSecond()
test_Search10kChunksUnder100ms()
```

**Test Assets Needed:**
- Sample PDF (10 pages, technical content)
- Sample PDF (100 pages, book)
- Malformed PDF for error handling

---

### 4. Complete PDF Text Extraction (Phase 3) 🟢 LOW

**Options:**

**Option A: TomRoush/PdfBox-Android**
```gradle
implementation("com.tom-roush:pdfbox-android:2.0.27.0")
```
- Pros: Full text extraction, good structure
- Cons: Large dependency (~15MB)

**Option B: ML Kit Text Recognition**
```gradle
implementation("com.google.mlkit:text-recognition:16.0.0")
```
- Pros: OCR support, handles scanned PDFs
- Cons: Requires Google Play Services, slower

**Option C: Native Platform APIs**
- Android: PdfRenderer + custom text extraction
- iOS: PDFKit (built-in)
- Desktop: Apache PDFBox (full version)

**Recommendation:** Option C (platform-specific, no extra deps)

---

## 🚀 Phase 3: Vector Storage (Next Sprint)

**Goal:** Optimize storage and search for 200k+ chunks

### Phase 3.1: SQLite-vec Integration

```
[ ] Add SQLite-vec dependency
[ ] Create database schema with vector columns
[ ] Implement SQLiteRAGRepository
[ ] Migration from InMemoryRAGRepository
[ ] Memory-mapped I/O for performance
```

### Phase 3.2: Cluster-based Indexing

```
[ ] Implement k-means clustering (256 clusters)
[ ] Cluster assignment on insertion
[ ] Two-stage search (cluster → chunks)
[ ] Target: <50ms for 200k chunks (40x speedup)
```

### Phase 3.3: LRU Cache

```
[ ] Implement hot chunk cache (1000 most recent)
[ ] Background cache warming
[ ] Cache hit metrics
```

### Phase 3.4: Power Optimization

```
[ ] GPS-based field mode detection
[ ] Scheduled background processing
[ ] Battery impact monitoring (<10mA idle)
[ ] Charging-only indexing mode
```

**Timeline:** 2 weeks
**Effort:** ~40 hours

---

## 📋 Technical Debt & Improvements

### High Priority
- [ ] ONNX model compression (INT8 quantized model)
- [ ] Error recovery in document processing
- [ ] Proper logging throughout
- [ ] Memory leak testing

### Medium Priority
- [ ] Support for DOCX, TXT, MD formats
- [ ] Web scraping integration (stubbed)
- [ ] Cloud storage integration (stubbed)
- [ ] Desktop sync protocol

### Low Priority
- [ ] UI for document management
- [ ] Progress indicators for processing
- [ ] Settings panel for configuration
- [ ] Export/import functionality

---

## 🎓 Learning Resources

**ONNX Runtime:**
- Docs: https://onnxruntime.ai/docs/
- Android: https://onnxruntime.ai/docs/tutorials/mobile/

**Sentence Transformers:**
- Models: https://huggingface.co/sentence-transformers
- all-MiniLM-L6-v2: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2

**Vector Search:**
- SQLite-vec: https://github.com/asg017/sqlite-vec
- Faiss: https://github.com/facebookresearch/faiss

---

## 📊 Current Project Status

**Overall Progress:**
- Phase 1 (Foundation): ✅ 100%
- Phase 2 (Processing): 🟡 70%
- Phase 3 (Storage): ⏸️ 0%
- Phase 4 (Power): ⏸️ 0%
- Phase 5 (UI): ⏸️ 0%

**Code Metrics:**
- Total Lines: ~5,700 (RAG + Theme)
- RAG Module: ~1,714 lines
- Theme Module: ~800 lines
- Documentation: ~3,200 lines
- Test Coverage: 0% (pending)

**Build Status:**
- ✅ Android (minSdk 26)
- ✅ iOS (arm64, x64, simulator)
- ✅ Desktop (JVM 17)
- ⏱️ Build Time: 22s (incremental)

---

## 🎯 Sprint Goal (Next Session)

**Objective:** Complete RAG Phase 2 (100%)

**Deliverables:**
1. ONNX embedding provider working
2. 85%+ test coverage
3. End-to-end integration test passing
4. Performance benchmarks documented
5. Ready to start Phase 3

**Success Metrics:**
- Can process a real PDF document
- Can search and retrieve relevant chunks
- <1s for 100 chunks processing
- >95% search accuracy

**Estimated Time:** 10 hours

---

## 📞 Questions / Blockers

None currently. Clear path forward.

**Next Review:** After ONNX provider implementation
