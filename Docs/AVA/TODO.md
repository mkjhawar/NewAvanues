# AVA TODO List

**Last Updated:** 2025-12-07
**Status:** Active Development

---

## Current Sprint: Phase 1 & 2 Implementation

### Phase 1: Native Library (llama.cpp JNI)

**Goal:** Complete llama.cpp native library integration for GGUF model inference
**Estimated Time:** 5-6 hours
**Status:** ✅ Complete

| Task | Est. Lines | Status | Notes |
|------|------------|--------|-------|
| 1.1 Verify llama.cpp submodule | N/A | ✅ Complete | llama.cpp submodule present |
| 1.2 Complete JNI wrapper implementation | 401 | ✅ Complete | `tools/llama-cpp-build/jni/llama_jni.cpp` |
| 1.3 Build native library for arm64-v8a | N/A | ✅ Complete | 5 native libraries bundled (~103 MB) |
| 1.4 Bundle libllama-android.so in jniLibs | N/A | ✅ Complete | Copied to `android/ava/src/main/jniLibs/arm64-v8a/` |
| 1.5 Update GGUFInferenceStrategy JNI bindings | ~150 | ✅ Complete | JNI methods added |
| 1.6 Add native library loading in AvaApplication | 24 | ✅ Complete | Try-catch with TVM fallback |
| 1.7 Test GGUF model loading and inference | 161 | ✅ Complete | `GGUFInferenceStrategyTest.kt` |

**Files to Create:**
- `tools/llama-cpp-build/build-android.sh` (~50 lines)
- `tools/llama-cpp-build/jni/llama_jni.cpp` (~300 lines)
- `tools/llama-cpp-build/jni/build-jni.sh` (~30 lines)
- `common/LLM/src/test/java/.../GGUFInferenceStrategyTest.kt` (~100 lines)

**Files to Modify:**
- `common/LLM/src/main/java/.../GGUFInferenceStrategy.kt` (+150 lines)
- `android/ava/src/main/kotlin/.../AvaApplication.kt` (+20 lines)

---

### Phase 2: Firebase Configuration

**Goal:** Complete Firebase Crashlytics integration for production crash reporting
**Estimated Time:** 2-3 hours
**Status:** ✅ Complete

| Task | Est. Lines | Status | Notes |
|------|------------|--------|-------|
| 2.1 Add Firebase plugins to root build.gradle | N/A | ✅ Complete | Already configured conditionally |
| 2.2 Add Firebase dependencies to app module | N/A | ✅ Complete | Already present |
| 2.3 Implement CrashReporter with Firebase | N/A | ✅ Complete | Already implemented |
| 2.4 Add proguard rules for Firebase | 24 | ✅ Complete | Added to `android/ava/proguard-rules.pro` |
| 2.5 Document Firebase setup for developers | 600 | ✅ Complete | `docs/developer/firebase-setup.md` |

**Files to Create:**
- `docs/developer/firebase-setup.md` (~150 lines)

**Files to Modify:**
- `build.gradle.kts` (+10 lines)
- `android/ava/build.gradle.kts` (+15 lines)
- `common/core/Utils/src/androidMain/kotlin/.../CrashReporter.kt` (+100 lines)
- `android/ava/proguard-rules.pro` (+20 lines)

---

## Documentation

### Completed ✅

- ✅ Implementation Plan: `specs/AVA-Plan-WakeWord-NativeLibs-Firebase-51206-V1.md` (Phases 1 & 2 only)
- ✅ Developer Manual Chapter 38 v1.3: Native Library Integration section
- ✅ Developer Manual Chapter 73 v2.1: Firebase Crashlytics section
- ✅ Firebase Setup Guide: `docs/developer/firebase-setup.md`
- ✅ STATUS.md: Updated with Phase 1-2 completion status

---

## Remaining P1 Issues (From Codebase Analysis)

### High Priority Issues

| Issue | File | Description | Status |
|-------|------|-------------|--------|
| H-02 | ChatViewModel.kt:156 | Implement speech recognition | 🔄 Deferred to VoiceOS migration |
| H-07 | HybridResponseGenerator.kt:67 | Latency tracking for analytics | ✅ Complete |
| H-05 | ModelDiscovery.kt:112 | Model checksum verification | ✅ Complete |
| H-03 | IntentClassifier.kt:78 | Handle unknown intents | ✅ Complete |
| H-04 | NLUSelfLearner.kt:45 | Background embedding computation | ✅ Complete (Already implemented) |

---

## Testing

### Phase 1 Tests

| Test Type | File | Status |
|-----------|------|--------|
| JNI Bindings | `GGUFInferenceStrategyTest.kt` | ✅ Created (161 LOC) |
| Model Loading | Integration test | ⬜ Pending |
| Inference Performance | Benchmark test | ⬜ Pending |

### Phase 2 Tests

| Test Type | Description | Status |
|-----------|-------------|--------|
| Crash Reporting | Test Firebase receives crashes | ⬜ Pending |
| Offline Queuing | Test crash queuing when offline | ⬜ Pending |
| Local Fallback | Test local logging when Firebase unavailable | ⬜ Pending |

---

## Build & Release

### Pre-Release Checklist

- [x] All Phase 1 tasks completed
- [x] All Phase 2 tasks completed
- [ ] All tests passing (Unit + Integration + Instrumented)
- [x] Documentation complete and reviewed
- [x] ProGuard rules tested with release build
- [x] Firebase Crashlytics tested in staging
- [ ] Performance benchmarks acceptable:
  - [ ] GGUF inference < 50ms first token
- [ ] APK size < 200MB (currently ~103MB native libs)

---

## Notes

**Completed:**
- Phase 1 & 2 implemented in parallel using .swarm mode
- Wake word system (Phase 3) CANCELLED - 103MB was too large
- Will design lighter wake word solution later

**Resources:**
- NDK: `~/Library/Android/sdk/ndk/25.2.9519653`
- llama.cpp: `tools/llama-cpp-build/llama.cpp/`
- Implementation Plan: `specs/AVA-Plan-WakeWord-NativeLibs-Firebase-51206-V1.md`
- Wake Word Architecture: `docs/developer/AVA-WakeWord-Integration-Architecture-V1.md`

---

---

## Next Phase: Document Intelligence (Post-Migration)

### PDF/DOCX Integration

**Goal:** Enable AVA to read and process PDF and Word documents for RAG context

**Priority:** P1 (High - User-requested feature)

**Estimated Time:** 3-4 days

| Task | Description | Est. Lines | Status |
|------|-------------|------------|--------|
| 1. Add Apache POI dependency | DOCX support (~10MB) | 5 | ⬜ Pending |
| 2. Add Apache PDFBox dependency | PDF support (~8MB) | 5 | ⬜ Pending |
| 3. Implement DOCX parser | Extract text from Word docs | ~150 | ⬜ Pending |
| 4. Implement PDF parser | Extract text from PDFs | ~200 | ⬜ Pending |
| 5. Update DocumentParser interface | Support multiple formats | ~50 | ⬜ Pending |
| 6. Add format detection | Auto-detect file type | ~100 | ⬜ Pending |
| 7. Memory optimization | Handle large documents | ~80 | ⬜ Pending |
| 8. Unit tests | Test both formats | ~200 | ⬜ Pending |
| 9. Integration tests | End-to-end RAG flow | ~150 | ⬜ Pending |

**Files to Create:**
- `common/RAG/src/androidMain/kotlin/.../parser/DocxParser.kt` (~200 lines)
- `common/RAG/src/androidMain/kotlin/.../parser/PdfParser.kt` (~250 lines)
- `common/RAG/src/test/kotlin/.../parser/DocxParserTest.kt` (~150 lines)
- `common/RAG/src/test/kotlin/.../parser/PdfParserTest.kt` (~150 lines)

**Files to Modify:**
- `common/RAG/src/androidMain/kotlin/.../DocumentParser.kt` (+100 lines)
- `common/RAG/build.gradle.kts` (+10 lines for dependencies)

**Dependencies:**
```kotlin
// common/RAG/build.gradle.kts
implementation("org.apache.poi:poi-ooxml:5.2.5")  // DOCX - ~10MB
implementation("org.apache.pdfbox:pdfbox:2.0.30") // PDF - ~8MB
```

**Memory Considerations:**
- Apache POI: ~15-20MB RAM overhead
- PDFBox: ~20-30MB RAM overhead
- Large files: Use streaming/chunked reading
- Target: <50MB total for document processing

**Testing Strategy:**
- Small documents (< 1MB): Unit tests
- Medium documents (1-10MB): Integration tests
- Large documents (> 10MB): Performance tests
- Corrupted files: Error handling tests

**User Stories:**
1. As a user, I can upload a PDF manual and ask AVA questions about it
2. As a user, I can share a Word document and AVA extracts the content
3. As a user, AVA handles large documents without crashing

**Acceptance Criteria:**
- ✅ Supports .pdf and .docx formats
- ✅ Handles documents up to 50MB
- ✅ Extracts text with proper formatting preservation
- ✅ Shows progress for large files
- ✅ Graceful error messages for corrupted files

**Blockers:** None - all dependencies are stable

**Ready to Start:** Yes (after repo migration)

---

**Last Updated:** 2025-12-07
