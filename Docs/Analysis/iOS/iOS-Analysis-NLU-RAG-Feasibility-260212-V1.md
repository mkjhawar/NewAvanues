# iOS NLU + RAG Feasibility Analysis

**Document:** iOS-Analysis-NLU-RAG-Feasibility-260212-V1.md
**Date:** 2026-02-12
**Version:** V1
**Status:** RESEARCH COMPLETE

---

## Executive Summary

All four AI modules (NLU, RAG, LLM, Chat) are **designed as KMP** with iOS targets already scaffolded. However, iOS implementation varies significantly:

- **NLU**: ~30% complete (CoreML backend for inference, tokenizers stubbed)
- **RAG**: ~5% complete (embedding factories are empty stubs)
- **LLM**: 0% complete (no iOS source set exists; only Android/Desktop)
- **Chat**: KMP structure ready, depends on above three

---

## 1. NLU Module (Modules/AI/NLU/)

### Build Configuration
- KMP: Yes (androidTarget, iosX64/Arm64/SimulatorArm64, desktop jvm)
- iOS targets: Conditional (enabled with kotlin.mpp.enableNativeTargets or "ios" in task names)

### iOS Implementation Status

**Implemented (30%):**
- `src/iosMain/coreml/CoreMLModelManager.kt` (460 lines) — Complete CoreML wrapper
  - Loads .mlmodel/.mlpackage bundles
  - Supports compute backends: ANE, GPU, CPU (iOS 17+)
  - Inference execution with MLFeatureProvider
  - Performance metrics tracking
  - Input/output tensor conversion (Long -> MLMultiArray -> Float)
  - Mean pooling for sequence reduction
- `src/iosMain/IntentClassifier.kt` — Expect/actual implementation stub
- `src/iosMain/ModelManager.kt` — Model lifecycle stub
- `src/iosMain/BertTokenizer.kt` — Tokenizer stub
- `src/iosMain/coreml/CoreMLBackendSelector.kt` — Backend selection placeholder

**Missing (70%):**
- **Tokenization pipeline**: BertTokenizer.iosMain is empty; needs BERT WordPiece tokenizer
  - Android uses: ONNX Runtime tokenizer + custom BERT impl
  - iOS needs: Swift BERT tokenizer OR pure Kotlin implementation
- **Model download/caching**: iOS needs download manager for .mlmodel files
  - Android has: HuggingFaceModelDownloader.kt (WorkManager-based)
  - iOS could use: URLSession or direct file management
- **Metadata loading**: loadModelMetadata() not in iOS implementation

### Key Dependencies
```
// Shared across platforms
kotlinx.coroutines.core
kotlinx.serialization.json
sqldelight (runtime + drivers — NO iOS driver specified)

// Android-only
onnxruntime.android (1.16.3)
tensorflow.lite.support
hilt.work (WorkManager)

// iOS Gap: No SQLDelight driver
```

### Model Format
- **Android**: ONNX models (MobileBERT, mALBERT multilingual)
- **iOS**: CoreML format (.mlmodel or .mlpackage)
- **Translation needed**: Python coremltools to convert ONNX -> CoreML

---

## 2. RAG Module (Modules/AI/RAG/)

### Build Configuration
- KMP: Yes (androidTarget, iosX64/Arm64/SimulatorArm64, desktop jvm)
- iOS targets: Conditional

### iOS Implementation Status

**Implemented (5%):**
- `src/iosMain/embeddings/EmbeddingProviderFactory.ios.kt` (28 lines) — **Empty stubs** (all return null)
- `src/iosMain/parser/DocumentParserFactory.ios.kt` (30 lines) — **Empty stubs** (all return null)

**Missing (95%):**
- **Embedding generation**: No ONNX embedding provider for iOS
  - Android uses: ONNX Runtime Mobile + all-MiniLM-L6-v2 (384-dim)
  - iOS options:
    1. ONNX Runtime iOS (exists, but heavy)
    2. Convert to CoreML + use CoreML embedding
    3. Cloud API (OpenAI, HuggingFace Inference)
- **Document parsing**: All parsers stubbed
  - iOS options: PDFKit (native PDF), Foundation HTMLParser, plain text only
- **Vector storage**: No SQLite or RAG repository for iOS
  - Need SQLDelight native iOS driver

### Key Dependencies
```
// Android-only (heavy, no iOS equivalent)
onnxruntime.android (1.16.3)
pdfbox.android (2.0.27.0) — use PDFKit on iOS
jsoup (1.17.1) — HTML parsing
poi (5.2.5) + xmlbeans (5.1.1) — DOCX parsing
security.crypto (1.1.0-alpha06)
```

---

## 3. LLM Module (Modules/AI/LLM/)

### Build Configuration
- KMP structure: Yes (androidTarget, jvm("desktop"))
- **iOS targets: MISSING** — No iOS configuration in build.gradle.kts

### iOS Implementation Status: **0%**
- No iOS source set exists
- Android uses TVM Runtime (tvm4j) and LiteRT (TFLite) — neither available on iOS from Kotlin
- Cloud providers (OpenAI, Anthropic, Google, HuggingFace) use Ktor — **COULD work on iOS**
- Tokenizers (SimpleVocabTokenizer, HuggingFaceTokenizer) may be Kotlin/common-compatible

### iOS Inference Options

| Framework | Android | iOS | Status |
|-----------|---------|-----|--------|
| TVM Runtime (MLC-LLM) | tvm4j + native libs | Not available from Kotlin | **Blocker** |
| TensorFlow Lite | tf-lite | Swift-only, not Kotlin | **Blocker** |
| ONNX Runtime | onnxruntime-android | onnxruntime-mobile-objc | Possible via cinterop |
| CoreML | N/A | Native | **Ready (partial in NLU)** |

**Decision**: iOS must use **CoreML exclusively** for local inference, or fall back to cloud APIs.

---

## 4. Chat Module (Modules/AI/Chat/)

### Build Configuration
- KMP: Yes (androidTarget, iosX64/Arm64/SimulatorArm64, desktop jvm)
- Dependencies: NLU, RAG, LLM, Actions
- Cannot fully function on iOS until NLU/RAG/LLM complete

---

## 5. iOS AI Strategy — Phased Approach

### Phase 1 (Ship with WebAvanue iOS — 4-6 weeks)
| Feature | Implementation | Effort |
|---------|---------------|--------|
| Intent Classification (NLU) | Complete CoreML wrapper, add tokenizer | 2-3 weeks |
| Cloud LLM | Wire Ktor-based providers (OpenAI/Claude) | 1 week |
| Multi-locale NLU | Already in commonMain | 1 week |
| Skip RAG | Defer to Phase 2 | 0 |
| Skip local LLM | Use cloud only | 0 |

### Phase 2 (Post-launch — 2-3 weeks)
| Feature | Implementation | Effort |
|---------|---------------|--------|
| RAG with cloud embeddings | OpenAI API for embeddings | 2 weeks |
| Document parsing | PDFKit + HTML only | 1 week |
| Persistent vector store | SQLDelight + iOS native driver | 1 week |

### Phase 3 (Advanced — 4-6 weeks)
| Feature | Implementation | Effort |
|---------|---------------|--------|
| Local LLM inference | CoreML Llama + quantization | 4 weeks |
| Local embeddings | CoreML conversion of MiniLM | 2 weeks |
| Full RAG pipeline | End-to-end local | 2 weeks |

---

## 6. Model Format Strategy

### Recommended iOS Pipeline
```
Option A (CoreML — Recommended for NLU):
HuggingFace Model -> coremltools convert -> .mlpackage -> CoreML inference

Option B (Cloud — Recommended for Phase 1 LLM/RAG):
HuggingFace/OpenAI API -> Ktor HTTP -> Response parsing

Option C (Local LLM — Phase 3):
GGUF Model -> CoreML or MLX convert -> Deploy to device -> Inference
```

---

## 7. Current iOS Readiness Score

| Module | Inference | Tokenization | Storage | Model Mgmt | Overall |
|--------|-----------|-------------|---------|------------|---------|
| NLU | 80% (CoreML) | 20% | 0% | 30% | **30%** |
| RAG | 0% | 0% | 10% | 0% | **5%** |
| LLM | 0% | 50% | 0% | 0% | **0%** |
| Chat | — | — | — | — | **10%** |

---

## 8. Key Gaps & Blockers

| Gap | Solution | Effort |
|-----|----------|--------|
| No SQLDelight iOS driver in NLU/RAG | Add `sqldelight.native.driver` to build.gradle.kts | 1 day |
| No BERT tokenizer for iOS | Port pure Kotlin tokenizer or create Swift bridge | 1 week |
| No iOS targets in LLM module | Add iOS target config to build.gradle.kts | 1 day |
| No local inference engine for iOS | Use CoreML (NLU) or cloud API (LLM/RAG) | Varies |
| ONNX -> CoreML model conversion | Python coremltools script | 2-3 days |
| No document parser for iOS | PDFKit + basic HTML extraction | 1 week |

---

## Conclusion

**iOS NLU is achievable in Phase 1** (CoreML inference 80% done). **RAG and local LLM should be deferred** to Phase 2/3, using cloud APIs initially. The path: complete NLU CoreML + cloud LLM for intent+chat, ship with WebAvanue, add local RAG/LLM later.
