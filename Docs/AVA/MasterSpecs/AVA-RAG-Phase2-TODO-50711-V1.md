# RAG Phase 2 TODOs: iOS & Desktop Platform Support

**Created:** 2025-11-07
**Status:** Planning
**Target:** Cross-platform feature parity
**Depends On:** Phase 3.2 (Android) - ✅ Complete

---

## Overview

Phase 2 focuses on bringing iOS and Desktop platforms to feature parity with Android. This includes implementing platform-specific document parsers, embedding providers, and UI components.

**Current State:**
- ✅ Android: Full implementation with UI (98% complete)
- ⚠️ iOS: Backend compiles, all providers return null
- ⚠️ Desktop: Backend compiles, all providers return null

---

## 1. iOS Implementation

### 1.1 Document Parsers

**File:** `Universal/AVA/Features/RAG/src/iosMain/kotlin/com/augmentalis/ava/features/rag/parser/DocumentParserFactory.ios.kt`

**Current State:** All methods return null/empty/false

**TODOs:**

#### PDF Parser
- [ ] Integrate PDFKit (native iOS framework)
- [ ] Implement `createPdfParser()` returning PDFParser instance
- [ ] Text extraction with page metadata
- [ ] Handle encrypted PDFs
- [ ] Test with sample PDFs (user manuals, technical docs)

**Dependencies:**
- PDFKit framework (built-in iOS)
- Swift interop if needed

**Estimated Effort:** 2-3 days

#### DOCX Parser
- [ ] Port Apache POI or find iOS-compatible library
- [ ] Consider native NSAttributedString with RTF conversion
- [ ] Implement `createDocxParser()`
- [ ] Extract text, headings, tables
- [ ] Test with Office 365 documents

**Dependencies:**
- Research: libxml2 + custom OOXML parsing vs third-party lib
- May require C/Objective-C interop

**Estimated Effort:** 3-4 days

#### HTML Parser
- [ ] Use WKWebView for HTML parsing
- [ ] Extract text content from DOM
- [ ] Implement `createHtmlParser()`
- [ ] Handle JavaScript-rendered content
- [ ] Test with web documentation

**Dependencies:**
- WebKit framework (built-in iOS)

**Estimated Effort:** 1-2 days

#### Markdown, TXT, RTF
- [ ] Implement lightweight parsers
- [ ] Use NSString for TXT
- [ ] NSAttributedString for RTF
- [ ] Markdown: Consider SwiftMarkdown or custom parser
- [ ] Implement `getSupportedFormats()` and `canParse()`

**Estimated Effort:** 1-2 days

### 1.2 Embedding Providers

**File:** `Universal/AVA/Features/RAG/src/iosMain/kotlin/com/augmentalis/ava/features/rag/embeddings/EmbeddingProviderFactory.ios.kt`

**Current State:** All methods return null

**TODOs:**

#### ONNX Embedding Provider
- [ ] Integrate ONNX Runtime for iOS
- [ ] Port Android ONNXEmbeddingProvider logic
- [ ] Implement `createOnnxProvider()`
- [ ] Bundle .onnx models in iOS app
- [ ] Test model loading and inference
- [ ] Benchmark performance on iOS devices

**Dependencies:**
- ONNX Runtime iOS framework (com.microsoft.onnxruntime:onnxruntime-mobile-objc)
- Model files in iOS bundle

**Estimated Effort:** 3-4 days

**Challenges:**
- Model file paths differ (iOS bundle vs Android assets)
- CoreML acceleration vs CPU/GPU
- Memory constraints on older devices

#### Local LLM Provider
- [ ] Port MLC-LLM for iOS
- [ ] Implement `createLocalLlmProvider()`
- [ ] Model loading from iOS bundle
- [ ] Test Gemma-2b-it inference
- [ ] Optimize for Metal GPU

**Dependencies:**
- MLC-LLM iOS SDK
- Model binaries (may require re-quantization for Metal)

**Estimated Effort:** 4-5 days

#### Cloud Provider
- [ ] Implement URLSession-based HTTP client
- [ ] API key management (iOS Keychain)
- [ ] Implement `createCloudProvider()`
- [ ] Support OpenAI, Anthropic, Google APIs
- [ ] Error handling and retry logic

**Dependencies:**
- URLSession (built-in)
- Keychain access

**Estimated Effort:** 2-3 days

### 1.3 UI (SwiftUI)

**Scope:** Equivalent to Android Compose UI

**Components:**

#### DocumentManagementView (SwiftUI)
- [ ] Document list with LazyVStack
- [ ] Grid layout for landscape (LazyVGrid)
- [ ] File picker integration (UIDocumentPickerViewController)
- [ ] Status indicators with SF Symbols
- [ ] Delete confirmation alerts
- [ ] Progress tracking with ProgressView

**Estimated Effort:** 3-4 days

#### RAGChatView (SwiftUI)
- [ ] Two-pane layout with NavigationSplitView (iOS 16+)
- [ ] Sources sidebar
- [ ] Chat messages with ScrollView
- [ ] Streaming response animation
- [ ] Gradient backgrounds (LinearGradient)
- [ ] Slide-in animations with .transition()

**Estimated Effort:** 4-5 days

#### RAGSearchView (SwiftUI)
- [ ] Search bar with TextField
- [ ] Results list
- [ ] Relevance scoring display
- [ ] Source highlighting

**Estimated Effort:** 2-3 days

### 1.4 Database (Core Data / Realm)

**Current:** RAG uses Room (Android)

**Options:**
1. **Core Data** - Native iOS, but requires Objective-C/Swift interop
2. **Realm** - Cross-platform, Kotlin-friendly
3. **SQLite directly** - Use SQLDelight for KMP

**Recommendation:** SQLDelight (already used elsewhere in project?)

**TODO:**
- [ ] Decide on iOS database strategy
- [ ] Implement DAO layer for iOS
- [ ] Migrate schema from Room to chosen solution
- [ ] Test CRUD operations

**Estimated Effort:** 3-5 days

---

## 2. Desktop Implementation

### 2.1 Document Parsers

**File:** `Universal/AVA/Features/RAG/src/desktopMain/kotlin/com/augmentalis/ava/features/rag/parser/DocumentParserFactory.desktop.kt`

**Current State:** All methods return null/empty/false

**TODOs:**

#### PDF Parser
- [ ] Use Apache PDFBox (JVM library)
- [ ] Reuse Android parser logic (both JVM-based)
- [ ] Implement `createPdfParser()`
- [ ] Test with large PDFs (100+ pages)

**Dependencies:**
- org.apache.pdfbox:pdfbox (JVM)

**Estimated Effort:** 1 day (reuse Android code)

#### DOCX Parser
- [ ] Use Apache POI (JVM library)
- [ ] Reuse Android parser logic
- [ ] Implement `createDocxParser()`

**Dependencies:**
- org.apache.poi:poi-ooxml (JVM)

**Estimated Effort:** 1 day (reuse Android code)

#### HTML Parser
- [ ] Use Jsoup (JVM library)
- [ ] Reuse Android parser logic
- [ ] Implement `createHtmlParser()`

**Dependencies:**
- org.jsoup:jsoup (JVM)

**Estimated Effort:** 1 day (reuse Android code)

#### Markdown, TXT, RTF
- [ ] Reuse Android/common implementations
- [ ] Implement `getSupportedFormats()` and `canParse()`

**Estimated Effort:** 0.5 day

### 2.2 Embedding Providers

**File:** `Universal/AVA/Features/RAG/src/desktopMain/kotlin/com/augmentalis/ava/features/rag/embeddings/EmbeddingProviderFactory.desktop.kt`

**Current State:** All methods return null

**TODOs:**

#### ONNX Embedding Provider
- [ ] Use ONNX Runtime for JVM
- [ ] Reuse Android logic (both JVM-based)
- [ ] Implement `createOnnxProvider()`
- [ ] Bundle models in desktop resources
- [ ] Test CPU inference performance

**Dependencies:**
- com.microsoft.onnxruntime:onnxruntime (JVM)

**Estimated Effort:** 1-2 days (mostly reuse)

#### Local LLM Provider
- [ ] Port MLC-LLM for desktop
- [ ] Alternative: Use llama.cpp JNI bindings
- [ ] Implement `createLocalLlmProvider()`
- [ ] GPU acceleration (CUDA/ROCm/Metal)

**Dependencies:**
- MLC-LLM JVM bindings or llama.cpp

**Estimated Effort:** 3-4 days

#### Cloud Provider
- [ ] Use Ktor client (already in project)
- [ ] Reuse API logic from Android/iOS
- [ ] Implement `createCloudProvider()`

**Dependencies:**
- Ktor client (commonMain)

**Estimated Effort:** 1-2 days

### 2.3 UI (Compose Desktop)

**Scope:** Equivalent to Android Compose UI

**Components:**

#### DocumentManagementScreen (Compose Desktop)
- [ ] Reuse Android Composables (mostly)
- [ ] File picker integration (JFileChooser)
- [ ] Desktop-specific keyboard shortcuts
- [ ] Window size adaptation
- [ ] Drag-and-drop support

**Estimated Effort:** 2-3 days

#### RAGChatScreen (Compose Desktop)
- [ ] Reuse Android two-pane layout
- [ ] Desktop window sizing
- [ ] Copy-paste functionality
- [ ] Export chat transcripts

**Estimated Effort:** 2-3 days

#### RAGSearchScreen (Compose Desktop)
- [ ] Reuse Android implementation
- [ ] Desktop keyboard navigation

**Estimated Effort:** 1 day

### 2.4 Database

**Current:** Room (Android-only)

**Options:**
1. **SQLDelight** - Cross-platform, KMP-friendly
2. **Exposed** - Kotlin SQL framework (JVM-only)
3. **SQLite JDBC** - Direct access

**Recommendation:** SQLDelight for consistency

**TODO:**
- [ ] Configure SQLDelight for desktop target
- [ ] Test migrations from Room schema
- [ ] Ensure vector storage compatibility

**Estimated Effort:** 2-3 days

---

## 3. Shared Work Items

### 3.1 Code Reuse Strategy

**Goal:** Maximize code sharing between Android, iOS, Desktop

**Tasks:**
- [ ] Extract common parser interfaces to commonMain
- [ ] Create abstract EmbeddingProvider base class
- [ ] Share prompt templates and RAG logic
- [ ] Unified model management (ModelRegistry in commonMain)
- [ ] Common chunking and tokenization

**Estimated Effort:** 3-4 days

### 3.2 Testing

**Tasks:**
- [ ] Unit tests for parsers (all platforms)
- [ ] Integration tests for RAG pipeline
- [ ] Performance benchmarks (iOS vs Android vs Desktop)
- [ ] Cross-platform data compatibility tests
- [ ] Model inference accuracy tests

**Estimated Effort:** 5-7 days

### 3.3 Documentation

**Tasks:**
- [ ] Update Developer Manual Chapter 28 with iOS/Desktop sections
- [ ] Platform-specific setup guides
- [ ] Model deployment instructions per platform
- [ ] Performance tuning recommendations

**Estimated Effort:** 2-3 days

---

## 4. Dependency Matrix

| Feature | Android | iOS | Desktop |
|---------|---------|-----|---------|
| **Parsers** | | | |
| PDF | PDFBox ✅ | PDFKit ❌ | PDFBox ❌ |
| DOCX | Apache POI ✅ | TBD ❌ | Apache POI ❌ |
| HTML | Jsoup ✅ | WebKit ❌ | Jsoup ❌ |
| **Embeddings** | | | |
| ONNX | ONNX Runtime ✅ | ONNX Runtime ❌ | ONNX Runtime ❌ |
| Local LLM | MLC-LLM ✅ | MLC-LLM ❌ | TBD ❌ |
| **Database** | | | |
| Storage | Room ✅ | TBD ❌ | TBD ❌ |
| **UI** | | | |
| Framework | Compose ✅ | SwiftUI ❌ | Compose Desktop ❌ |

---

## 5. Effort Estimation

### iOS
- **Parsers:** 8-11 days
- **Embeddings:** 9-12 days
- **UI:** 9-12 days
- **Database:** 3-5 days
- **Total:** ~30-40 days (1.5-2 months, 1 developer)

### Desktop
- **Parsers:** 3-4 days (mostly reuse)
- **Embeddings:** 5-7 days
- **UI:** 5-7 days (mostly reuse)
- **Database:** 2-3 days
- **Total:** ~15-21 days (3-4 weeks, 1 developer)

### Shared Work
- **Code refactoring:** 3-4 days
- **Testing:** 5-7 days
- **Documentation:** 2-3 days
- **Total:** ~10-14 days

**Overall Phase 2 Estimate:** 55-75 days (2.5-3.5 months, 1 developer)

**Parallelization:** iOS and Desktop can be done in parallel → ~40-50 days with 2 developers

---

## 6. Prioritization

**High Priority (MVP for cross-platform):**
1. iOS parsers (PDF, DOCX, HTML)
2. iOS ONNX embeddings
3. iOS database
4. iOS basic UI (document management)
5. Desktop parsers (reuse Android)
6. Desktop ONNX embeddings

**Medium Priority:**
1. iOS chat UI
2. Desktop chat UI
3. Local LLM for iOS/Desktop

**Low Priority:**
1. Advanced animations on iOS/Desktop
2. Cloud providers
3. Platform-specific optimizations

---

## 7. Risks & Mitigation

### Risk 1: ONNX Runtime iOS compatibility
**Impact:** High (core feature)
**Likelihood:** Medium
**Mitigation:** Evaluate CoreML as alternative, pre-convert models to CoreML format

### Risk 2: MLC-LLM iOS integration complexity
**Impact:** High (RAG chat depends on it)
**Likelihood:** High
**Mitigation:** Start early, consider cloud LLM fallback for iOS

### Risk 3: SwiftUI learning curve
**Impact:** Medium (iOS UI)
**Likelihood:** Medium
**Mitigation:** Hire iOS expert, or simplify iOS UI for MVP

### Risk 4: Database migration from Room
**Impact:** High (data loss risk)
**Likelihood:** Low
**Mitigation:** Use SQLDelight from start, write migration scripts, extensive testing

---

## 8. Next Steps

**Immediate:**
1. Decide on iOS database strategy (SQLDelight recommended)
2. Research ONNX Runtime iOS integration
3. Prototype PDF parsing on iOS with PDFKit
4. Set up Desktop build configuration

**Week 1-2:**
- Implement iOS PDF parser
- Implement iOS ONNX provider
- Desktop parser reuse (quick wins)

**Week 3-4:**
- iOS DOCX/HTML parsers
- iOS database integration
- Desktop ONNX provider

**Month 2:**
- iOS UI implementation (SwiftUI)
- Desktop UI reuse (Compose)
- Cross-platform testing

---

## 9. Success Criteria

**Phase 2 Complete When:**
- ✅ All iOS parsers operational (PDF, DOCX, HTML, Markdown, TXT)
- ✅ iOS ONNX embeddings working
- ✅ iOS RAG search functional
- ✅ iOS basic UI complete (document management)
- ✅ All Desktop parsers operational
- ✅ Desktop ONNX embeddings working
- ✅ Desktop RAG search functional
- ✅ Desktop UI complete (all screens)
- ✅ Cross-platform tests passing
- ✅ Documentation updated
- ✅ Performance benchmarks documented

---

**Author:** AVA AI Team
**Review Date:** 2025-11-15 (check progress)
**Target Completion:** Q1 2026
