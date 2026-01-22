# Handover Report: LLM Module - MLC/TVM to llama.cpp Migration

**Document ID:** Handover-LLM-MLC-to-GGUF-260117
**Date:** 2026-01-17
**Module:** Modules/AI/LLM
**Priority:** High
**Status:** Ready for Implementation

---

## 1. Executive Summary

This handover covers the analysis and decision to migrate AVA's on-device LLM inference from MLC/TVM to llama.cpp (GGUF format). The decision was made based on licensing simplification, larger model ecosystem, and comparable performance.

**Decision:** Migrate to llama.cpp/GGUF as primary backend, keep MLC as optional fallback.

**Estimated Effort:** ~2 weeks (80% of GGUF code already exists)

---

## 2. Context & Background

### 2.1 What is ALC?

**ALC = Adaptive LLM Coordinator** (naming standardized during this session)

- Orchestrates between local and cloud LLM providers
- Handles tokenization, inference, streaming responses
- Supports multilingual models (10 languages)
- Located at: `Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/`

### 2.2 Current Architecture

```
User Input
    ↓
NLU Module (intent classification) ← Separate module, NOT affected
    ↓
Chat Coordinators (routing)
    ↓
ALC/LLM Module ← THIS IS WHAT WE'RE CHANGING
├── MLCInferenceStrategy (TVM) ← Current primary
├── GGUFInferenceStrategy (llama.cpp) ← New primary
├── TVMTokenizer ← Will be replaced
└── HuggingFaceTokenizer ← Will become primary
    ↓
Response to User
```

### 2.3 Why Migrate?

| Factor | MLC/TVM | llama.cpp | Winner |
|--------|---------|-----------|--------|
| License | Apache 2.0 + modifications | MIT | llama.cpp |
| Model ecosystem | ~50 models | 1000+ models | llama.cpp |
| Native lib size | 104 MB | 15-25 MB | llama.cpp |
| Memory efficiency | Good | Better (mmap) | llama.cpp |
| Community updates | Monthly | Weekly | llama.cpp |
| Attribution complexity | Higher (patched source) | Simpler | llama.cpp |

---

## 3. Technical Details

### 3.1 Current Dependencies (To Be Removed/Optional)

| Artifact | Path | Size |
|----------|------|------|
| `tvm4j_core.jar` | `libs/tvm4j_core.jar` | ~2 MB |
| `libtvm4j_runtime_packed.so` | `jniLibs/arm64-v8a/` | ~104 MB |
| `Module.java` (patched) | `tvm-patches/Module.java` | N/A |

### 3.2 New Dependencies (Already Partially Integrated)

| Artifact | Path | Size |
|----------|------|------|
| `libllama-android.so` | `jniLibs/arm64-v8a/` | ~15-25 MB |

### 3.3 Key Files to Modify

| File | Change | Effort |
|------|--------|--------|
| `GGUFInferenceStrategy.kt` | Complete implementation (80% done) | 2-3 days |
| `LocalLLMProvider.kt` | Add GGUF backend selection | 1 day |
| `ALCEngine.kt` | Update model name mapping for GGUF | 1 day |
| `build.gradle.kts` | Remove TVM deps (optional) | 0.5 day |

### 3.4 Files That Stay the Same

| File | Reason |
|------|--------|
| `HuggingFaceTokenizer.kt` | Already exists, will become primary |
| `IInferenceStrategy.kt` | Interface unchanged |
| `ITokenizer.kt` | Interface unchanged |
| `MultiProviderInferenceStrategy.kt` | Fallback logic unchanged |
| All RAG files | Completely separate (ONNX embeddings) |
| All NLU files | Completely separate module |

---

## 4. What Was Completed This Session

### 4.1 Analysis & Documentation

- [x] Deep analysis of MLC vs GGUF trade-offs
- [x] Created comprehensive analysis report
- [x] Documented impact on tokenization, RAG, NLU (none on latter two)
- [x] Created migration path with 4 phases

### 4.2 Naming Standardization

Fixed inconsistent ALC naming across codebase:

| File | Before | After |
|------|--------|-------|
| `MIGRATION-REPORT.md:67` | Augmentalis Local Compute | Adaptive LLM Coordinator |
| `DEPRECATED.md:11` | AI Language Core | Adaptive LLM Coordinator |

**Official:** ALC = **Adaptive LLM Coordinator**

### 4.3 Build Fixes (Earlier in Session)

- Fixed imports in `ALCEngine.kt` and `ALCEngineSingleLanguage.kt`
- Fixed Result type usage in `TokenCacheManager.kt`
- Fixed imports in `AnthropicProvider.kt` and `GoogleAIProvider.kt`
- Build now passes: `./gradlew :modules:AI:LLM:compileDebugKotlinAndroid`

---

## 5. What Needs to Be Done

### Phase 1: Complete GGUF Integration (1-2 weeks)

```
[ ] Complete GGUFInferenceStrategy.kt implementation
    - Error handling improvements
    - Memory management
    - Streaming generation completion
    - Stop sequence handling

[ ] Update LocalLLMProvider.kt
    - Add InferenceBackend enum (MLC, GGUF, AUTO)
    - Add backend selection logic
    - Switch tokenizer to HuggingFaceTokenizer for GGUF

[ ] Test GGUF path end-to-end
    - Unit tests
    - Integration tests with real GGUF model
```

### Phase 2: Parallel Testing (2-3 weeks)

```
[ ] Run MLC and GGUF side-by-side
[ ] Performance benchmarking
[ ] Quality comparison (response coherence)
[ ] Device compatibility testing (10+ devices)
```

### Phase 3: Migration (2-4 weeks)

```
[ ] Convert language-specific models to GGUF format
[ ] Update model download pipeline
[ ] Switch default to GGUF
[ ] Keep MLC as fallback option
```

### Phase 4: Cleanup (1 week)

```
[ ] Remove TVM dependencies (optional)
[ ] Update documentation
[ ] Archive MLC code for reference
```

---

## 6. Key Code Locations

### Primary Files (ALC)

```
Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/
├── alc/
│   ├── ALCEngine.kt                    # Multilingual orchestrator
│   ├── ALCEngineSingleLanguage.kt      # Single-language engine
│   ├── TVMRuntime.kt                   # TVM JNI wrapper (to be optional)
│   ├── inference/
│   │   ├── MLCInferenceStrategy.kt     # TVM inference (current)
│   │   ├── GGUFInferenceStrategy.kt    # llama.cpp inference (NEW PRIMARY)
│   │   └── MultiProviderInferenceStrategy.kt
│   └── tokenizer/
│       ├── TVMTokenizer.kt             # To be deprecated
│       └── HuggingFaceTokenizer.kt     # NEW PRIMARY
├── provider/
│   └── LocalLLMProvider.kt             # Needs backend selection
└── ...
```

### Analysis Report

```
Modules/AI/LLM/docs/analysis/Analysis-MLC-vs-GGUF-260117-V1.md
CodeAvenue/docs/analysis/Analysis-MLC-vs-GGUF-260117-V1.md
```

### Build Configuration

```
Modules/AI/LLM/build.gradle.kts
Modules/AI/LLM/libs/tvm4j_core.jar      # To be removed/optional
Modules/AI/LLM/tvm-patches/Module.java  # To be removed
```

---

## 7. Important Decisions Made

| Decision | Rationale |
|----------|-----------|
| Use llama.cpp as primary | Larger ecosystem, simpler licensing, 80% code exists |
| Keep MLC as fallback | Zero-risk transition, some optimized models may perform better |
| HuggingFace tokenizer for GGUF | Model-agnostic, already implemented |
| Don't rewrite from scratch | Would take 2-4 years, not practical |
| RAG/NLU unaffected | Separate modules with ONNX embeddings |

---

## 8. Risks & Mitigations

| Risk | Probability | Mitigation |
|------|-------------|------------|
| Performance regression | Low | Benchmark before switching |
| Missing language models | Medium | Convert existing or find GGUF alternatives |
| Native library issues | Low | Test on diverse devices |
| Tokenizer mismatch | Low | Use model-bundled tokenizer |

---

## 9. Commands to Get Started

```bash
# Navigate to module
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/AI/LLM

# Verify build still works
./gradlew :modules:AI:LLM:compileDebugKotlinAndroid

# Read the analysis report
cat docs/analysis/Analysis-MLC-vs-GGUF-260117-V1.md

# Key file to start with
code src/androidMain/kotlin/com/augmentalis/llm/alc/inference/GGUFInferenceStrategy.kt
```

---

## 10. Reference Documents

| Document | Location |
|----------|----------|
| Analysis Report | `docs/analysis/Analysis-MLC-vs-GGUF-260117-V1.md` |
| Migration Report | `MIGRATION-REPORT.md` |
| LLM README | `README.md` |
| Deprecated Notice | `DEPRECATED.md` |

---

## 11. Questions for Next Session

1. Which GGUF model to use for initial testing? (Recommend: gemma-2b-it.Q4_K_M.gguf)
2. Should we convert existing MLC models or download new GGUF versions?
3. Timeline priority - fast migration or thorough parallel testing?

---

**Handover Prepared By:** Manoj Jhawar
**Date:** 2026-01-17
**Next Action:** Complete GGUFInferenceStrategy.kt implementation
