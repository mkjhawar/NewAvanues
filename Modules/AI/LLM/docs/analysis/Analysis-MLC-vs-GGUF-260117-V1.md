# Analysis Report: MLC/TVM vs llama.cpp (GGUF) Runtime Comparison

**Document ID:** Analysis-MLC-vs-GGUF-260117-V1
**Date:** 2026-01-17
**Author:** Manoj Jhawar
**Status:** For Review
**Module:** AI/LLM

---

## Executive Summary

This report analyzes the trade-offs between continuing with MLC/TVM-based inference versus migrating to llama.cpp (GGUF format) for AVA's on-device LLM capabilities. The analysis covers technical dependencies, licensing implications, performance characteristics, and migration effort.

**Recommendation:** Migrate to llama.cpp/GGUF as the primary inference backend while maintaining MLC as an optional fallback during transition.

**Key Drivers:**
- Simplified licensing (MIT vs Apache 2.0 with modifications)
- Larger model ecosystem (1000+ GGUF models vs ~50 MLC models)
- Comparable or better performance on modern devices
- Lower migration effort (~2 weeks, 80% of GGUF code already exists)
- RAG and embeddings completely unaffected

---

## Table of Contents

1. [Current Architecture](#1-current-architecture)
2. [Dependency Analysis](#2-dependency-analysis)
3. [Licensing Implications](#3-licensing-implications)
4. [Performance Comparison](#4-performance-comparison)
5. [Impact Analysis](#5-impact-analysis)
6. [Model Ecosystem](#6-model-ecosystem)
7. [Migration Path](#7-migration-path)
8. [Risk Assessment](#8-risk-assessment)
9. [Recommendation](#9-recommendation)
10. [Appendix](#10-appendix)

---

## 1. Current Architecture

### 1.1 System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           AVA AI Stack                                   │
├─────────────────────────────────────────────────────────────────────────┤
│  Application Layer                                                       │
│  ├── Chat UI (Compose)                                                  │
│  ├── Voice Commands (VoiceOS)                                           │
│  └── RAG Chat (RAGChatEngine)                                           │
├─────────────────────────────────────────────────────────────────────────┤
│  Orchestration Layer                                                     │
│  ├── HybridResponseGenerator (cloud + local routing)                    │
│  ├── ALCEngine (Adaptive LLM Coordinator)                               │
│  └── MultiProviderInferenceStrategy (fallback chain)                    │
├─────────────────────────────────────────────────────────────────────────┤
│  Inference Layer                                                         │
│  ├── MLCInferenceStrategy ←── TVMRuntime ←── tvm4j_runtime_packed.so   │
│  ├── GGUFInferenceStrategy ←── llama.cpp ←── libllama-android.so       │
│  └── LiteRTInferenceStrategy (planned)                                  │
├─────────────────────────────────────────────────────────────────────────┤
│  Tokenization Layer                                                      │
│  ├── TVMTokenizer (TVM-coupled, current primary)                        │
│  ├── HuggingFaceTokenizer (model-agnostic, available)                   │
│  └── SimpleVocabTokenizer (fallback)                                    │
├─────────────────────────────────────────────────────────────────────────┤
│  Embedding Layer (SEPARATE - unaffected by LLM backend choice)          │
│  └── ONNXEmbeddingProvider ←── all-MiniLM-L6-v2 (384-dim)              │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Key Components

| Component | File Path | Purpose |
|-----------|-----------|---------|
| ALCEngine | `llm/alc/ALCEngine.kt` | Multilingual LLM orchestrator |
| ALCEngineSingleLanguage | `llm/alc/ALCEngineSingleLanguage.kt` | SOLID-refactored single-language engine |
| LocalLLMProvider | `llm/provider/LocalLLMProvider.kt` | LLMProvider interface implementation |
| MLCInferenceStrategy | `llm/alc/inference/MLCInferenceStrategy.kt` | TVM/MLC inference execution |
| GGUFInferenceStrategy | `llm/alc/inference/GGUFInferenceStrategy.kt` | llama.cpp inference execution |
| TVMRuntime | `llm/alc/TVMRuntime.kt` | TVM JNI wrapper |
| TVMTokenizer | `llm/alc/tokenizer/TVMTokenizer.kt` | TVM-based tokenization |
| HuggingFaceTokenizer | `llm/alc/tokenizer/HuggingFaceTokenizer.kt` | Model-agnostic tokenization |

### 1.3 Inference Strategy Interface

```kotlin
interface IInferenceStrategy {
    suspend fun infer(request: InferenceRequest): InferenceResult
    fun isAvailable(): Boolean
    fun getName(): String
    fun getPriority(): Int  // Lower = higher priority
}
```

| Strategy | Priority | Status |
|----------|----------|--------|
| MLCInferenceStrategy | 50 | ✅ Complete |
| GGUFInferenceStrategy | 100 | ⏳ 80% Complete |
| LiteRTInferenceStrategy | 150 | 📋 Planned |

---

## 2. Dependency Analysis

### 2.1 MLC/TVM Dependencies

| Artifact | Type | Size | License | Source |
|----------|------|------|---------|--------|
| `tvm4j_core.jar` | Java bindings | ~2 MB | Apache 2.0 | Apache TVM |
| `libtvm4j_runtime_packed.so` | Native library | ~104 MB | Apache 2.0 | MLC-LLM + TVM |
| `Module.java` (patched) | Modified source | N/A | Apache 2.0 | Custom patch |

**Direct API Usage in `TVMRuntime.kt`:**
```kotlin
import org.apache.tvm.Device
import org.apache.tvm.Module
import org.apache.tvm.Function
import org.apache.tvm.TVMValue
import org.apache.tvm.TensorBase
import org.apache.tvm.Tensor
import org.apache.tvm.TVMType
```

**Native Library Loading:**
```kotlin
System.loadLibrary("tvm4j_runtime_packed")  // 104MB packed runtime
```

### 2.2 llama.cpp Dependencies

| Artifact | Type | Size | License | Source |
|----------|------|------|---------|--------|
| `libllama-android.so` | Native library | ~15-25 MB | MIT | llama.cpp |

**Native Methods in `GGUFInferenceStrategy.kt`:**
```kotlin
private external fun nativeLoadModel(modelPath: String, contextLength: Int, gpuLayers: Int): Long
private external fun nativeCreateContext(modelPtr: Long, contextLength: Int): Long
private external fun nativeInfer(contextPtr: Long, tokens: IntArray): FloatArray
private external fun nativeTokenize(contextPtr: Long, text: String): IntArray
private external fun nativeSampleToken(contextPtr: Long, ...): Int
```

### 2.3 Dependency Comparison

| Aspect | MLC/TVM | llama.cpp |
|--------|---------|-----------|
| Native lib size | ~104 MB | ~15-25 MB |
| Java bindings | Required (tvm4j) | Not required |
| Source modifications | Yes (Module.java) | No |
| Build complexity | High (Maven + CMake) | Low (CMake only) |
| Version coupling | Tight (v0.22.0 specific) | Loose |

---

## 3. Licensing Implications

### 3.1 Apache 2.0 (TVM/MLC-LLM) Requirements

1. **Attribution Required:** Must include notice in documentation
2. **License Copy:** Must distribute copy of Apache 2.0 license
3. **State Changes:** Must document modifications (Module.java patch)
4. **No Trademark Use:** Cannot use Apache TVM trademarks

**Required NOTICE file content:**
```
This product includes software developed by:
- Apache TVM (https://tvm.apache.org/) - Apache 2.0 License
- MLC-LLM (https://mlc.ai/) - Apache 2.0 License

Modifications:
- Module.java: Modified for TVM v0.22.0 FFI compatibility
  (1-argument ModuleLoadFromFile instead of 2-argument)
```

### 3.2 MIT (llama.cpp) Requirements

1. **Attribution Required:** Must include copyright notice
2. **License Copy:** Must include MIT license text
3. **No Additional Restrictions:** More permissive

**Required attribution:**
```
llama.cpp - MIT License
Copyright (c) 2023-2024 Georgi Gerganov
```

### 3.3 Licensing Comparison

| Requirement | Apache 2.0 (TVM) | MIT (llama.cpp) |
|-------------|------------------|-----------------|
| Attribution | ✅ Required | ✅ Required |
| License copy | ✅ Required | ✅ Required |
| State changes | ✅ Required | ❌ Not required |
| Patent grant | ✅ Included | ❌ Not included |
| Complexity | Higher | Lower |

**Conclusion:** Neither option allows avoiding attribution entirely. MIT is simpler to comply with.

---

## 4. Performance Comparison

### 4.1 Theoretical Performance

| Metric | MLC/TVM | llama.cpp | Notes |
|--------|---------|-----------|-------|
| Compilation | AOT (ahead-of-time) | JIT + AOT | TVM pre-compiles kernels |
| GPU backends | Vulkan, OpenCL, Metal | Vulkan, Metal, CUDA | Similar coverage |
| CPU optimization | TVM auto-tuning | Hand-optimized SIMD | Both excellent |
| Memory mapping | Limited | Full mmap support | llama.cpp advantage |
| Quantization | Q4F16_1, INT4, INT8 | Q2-Q8, K-quants | llama.cpp more options |

### 4.2 Expected Performance (Android)

| Scenario | MLC/TVM | llama.cpp | Winner |
|----------|---------|-----------|--------|
| Cold start (model load) | 3-5 seconds | 2-4 seconds | llama.cpp |
| Tokens/sec (Vulkan GPU) | 15-25 t/s | 12-22 t/s | Similar |
| Tokens/sec (OpenCL GPU) | 12-20 t/s | 10-18 t/s | Similar |
| Tokens/sec (CPU only) | 5-10 t/s | 8-15 t/s | llama.cpp |
| Peak memory (2B model) | ~2.5 GB | ~1.8 GB | llama.cpp |
| Memory with mmap | N/A | ~0.5 GB active | llama.cpp |

### 4.3 Device-Specific Considerations

| Device Class | Recommendation | Reason |
|--------------|----------------|--------|
| Flagship (8+ GB RAM) | Either | Both perform well |
| Mid-range (4-6 GB RAM) | llama.cpp | Better memory efficiency |
| Budget (3-4 GB RAM) | llama.cpp | mmap enables larger models |
| Older devices (OpenCL only) | MLC/TVM | Better OpenCL tuning |

---

## 5. Impact Analysis

### 5.1 Tokenization Impact

| Component | Current State | After Migration | Impact Level |
|-----------|---------------|-----------------|--------------|
| TVMTokenizer | Primary for LLM | Deprecated | 🔴 Replaced |
| HuggingFaceTokenizer | Available, unused | Primary for LLM | 🟢 No code change |
| SimpleVocabTokenizer | Fallback | Fallback | 🟢 No change |
| TokenCacheManager | Caches TVM tokens | Caches HF tokens | 🟢 Interface unchanged |

**Code Change Required:**
```kotlin
// LocalLLMProvider.kt - BEFORE
private val tokenizer = TVMTokenizer(tvmRuntime)

// LocalLLMProvider.kt - AFTER
private val tokenizer = HuggingFaceTokenizer.load(modelDir)
```

### 5.2 RAG System Impact

| Component | Affected? | Reason |
|-----------|-----------|--------|
| ONNXEmbeddingProvider | ❌ No | Uses ONNX, not TVM |
| RAGChatEngine | ❌ No | Uses LLMProvider interface |
| Semantic search | ❌ No | Embedding-based, not LLM |
| Document chunking | ❌ No | Text processing only |
| Query caching | ❌ No | Hash-based caching |

**Key Finding:** RAG uses a completely separate embedding pipeline (ONNX + all-MiniLM-L6-v2). The LLM backend choice does NOT affect:
- Search quality
- Embedding generation
- Document indexing
- Retrieval accuracy

### 5.3 Multilingual Support Impact

| Language | Current Model (MLC) | GGUF Alternative | Status |
|----------|---------------------|------------------|--------|
| English | gemma-2b-en | gemma-2b-it.Q4_K_M.gguf | ✅ Available |
| Spanish | flor-1.3b-es | Custom or Mistral-es | ⚠️ May need conversion |
| French | croissant-1.3b-fr | Mistral-fr variants | ⚠️ May need conversion |
| German | leo-7b-de | leo-hessianai.Q4_K_M | ✅ Available |
| Japanese | rinna-3.6b-ja | ELYZA, rinna GGUF | ✅ Available |
| Chinese | qwen-1.8b-zh | Qwen GGUF variants | ✅ Available |
| Portuguese | tucano-1.1b-pt | Sabiá, custom | ⚠️ Limited |
| Italian | minerva-1b-it | Mistral-it variants | ⚠️ May need conversion |
| Korean | polyglot-1.3b-ko | polyglot GGUF | ✅ Available |
| Arabic | jais-1.3b-ar | jais GGUF | ✅ Available |

**Assessment:** 6/10 languages have direct GGUF equivalents. Others may need conversion or alternative models.

### 5.4 API Surface Impact

| Interface | Change Required? | Notes |
|-----------|------------------|-------|
| LLMProvider | ❌ No | Abstract interface unchanged |
| IInferenceStrategy | ❌ No | Already supports both |
| ITokenizer | ❌ No | HuggingFaceTokenizer implements it |
| GenerationOptions | ❌ No | Model-agnostic parameters |
| LLMResponse | ❌ No | Sealed class unchanged |
| ChatMessage | ❌ No | Data class unchanged |

---

## 6. Model Ecosystem

### 6.1 Model Availability Comparison

| Category | MLC-LLM Models | GGUF Models |
|----------|----------------|-------------|
| Total available | ~50-100 | 1000+ |
| Gemma variants | 5-10 | 50+ |
| Llama variants | 10-20 | 200+ |
| Mistral variants | 5-10 | 100+ |
| Multilingual | Limited | Extensive |
| Quantization options | 2-3 per model | 6-8 per model |
| Update frequency | Monthly | Weekly |

### 6.2 Quantization Format Comparison

| Format | MLC/TVM | llama.cpp | Quality | Size |
|--------|---------|-----------|---------|------|
| Q2_K | ❌ | ✅ | Low | Smallest |
| Q3_K_S | ❌ | ✅ | Low-Med | Very small |
| Q4_0 | ✅ (INT4) | ✅ | Medium | Small |
| Q4_K_M | ❌ | ✅ | Med-High | Small |
| Q4F16_1 | ✅ | ❌ | Medium | Small |
| Q5_K_M | ❌ | ✅ | High | Medium |
| Q6_K | ❌ | ✅ | Very High | Medium |
| Q8_0 | ✅ (INT8) | ✅ | Excellent | Large |
| F16 | ✅ | ✅ | Best | Largest |

**Recommendation:** Q4_K_M offers the best quality/size trade-off for mobile devices.

### 6.3 Model Sources

| Source | MLC Format | GGUF Format |
|--------|------------|-------------|
| HuggingFace | Limited | Extensive (TheBloke, etc.) |
| MLC-LLM repo | Primary | N/A |
| Ollama library | N/A | Direct GGUF support |
| Custom conversion | Complex | Simple (llama.cpp tools) |

---

## 7. Migration Path

### 7.1 Phased Approach

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 1: Complete GGUF Integration (1-2 weeks)                  │
├─────────────────────────────────────────────────────────────────┤
│ • Complete GGUFInferenceStrategy implementation                 │
│ • Integrate HuggingFaceTokenizer as primary                     │
│ • Add GGUF model loading in LocalLLMProvider                    │
│ • Unit tests for GGUF path                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 2: Parallel Testing (2-3 weeks)                           │
├─────────────────────────────────────────────────────────────────┤
│ • Run MLC and GGUF side-by-side                                 │
│ • A/B testing on test devices                                   │
│ • Performance benchmarking                                      │
│ • Quality comparison (perplexity, coherence)                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 3: Gradual Migration (2-4 weeks)                          │
├─────────────────────────────────────────────────────────────────┤
│ • Switch default to GGUF for new installations                  │
│ • Convert language-specific models to GGUF                      │
│ • Update model download pipeline                                │
│ • Keep MLC as fallback option                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 4: Cleanup (1 week)                                       │
├─────────────────────────────────────────────────────────────────┤
│ • Remove TVM dependencies (optional)                            │
│ • Update documentation                                          │
│ • Simplify build configuration                                  │
│ • Archive MLC code for reference                                │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Files to Modify

| File | Change Type | Effort |
|------|-------------|--------|
| `GGUFInferenceStrategy.kt` | Complete implementation | 2-3 days |
| `LocalLLMProvider.kt` | Add GGUF support | 1 day |
| `ALCEngine.kt` | Update model mapping | 1 day |
| `build.gradle.kts` | Remove TVM deps (optional) | 0.5 day |
| `proguard-rules.pro` | Update rules | 0.5 day |
| Documentation | Update README, design docs | 1 day |

### 7.3 Code Changes Required

**GGUFInferenceStrategy.kt** - Complete the implementation:
```kotlin
// Current: 80% complete
// Needed:
// - Error handling improvements
// - Memory management
// - Streaming generation completion
// - Stop sequence handling
```

**LocalLLMProvider.kt** - Add backend selection:
```kotlin
// Add configuration option
enum class InferenceBackend { MLC, GGUF, AUTO }

// In initialize():
val strategy = when (config.backend) {
    InferenceBackend.GGUF -> GGUFInferenceStrategy(context, modelPath)
    InferenceBackend.MLC -> MLCInferenceStrategy(tvmModule)
    InferenceBackend.AUTO -> detectBestBackend()
}
```

**ALCEngine.kt** - Update model paths:
```kotlin
private fun getModelNameForLanguage(languageCode: String): String {
    return when (languageCode) {
        "en" -> "gemma-2b-it.Q4_K_M.gguf"  // Changed from gemma-2b-en
        "es" -> "mistral-7b-es.Q4_K_M.gguf"
        // ... etc
    }
}
```

---

## 8. Risk Assessment

### 8.1 Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Performance regression | Low | Medium | Benchmark before switching |
| Model quality difference | Low | Medium | A/B test with users |
| Missing language models | Medium | High | Convert existing or find alternatives |
| Native library issues | Low | High | Test on diverse devices |
| Tokenizer mismatch | Low | Medium | Use model-bundled tokenizer |
| Migration delays | Medium | Low | Phased approach allows fallback |

### 8.2 Rollback Plan

If issues arise during migration:

1. **Phase 1-2:** Simply continue using MLC (no user impact)
2. **Phase 3:** Feature flag to switch users back to MLC
3. **Phase 4:** Restore TVM dependencies from git history

### 8.3 Testing Requirements

| Test Type | Scope | Pass Criteria |
|-----------|-------|---------------|
| Unit tests | Tokenization, inference | All existing tests pass |
| Integration tests | End-to-end chat flow | Response quality maintained |
| Performance tests | Latency, throughput | Within 20% of MLC |
| Memory tests | Peak usage, leaks | No regression |
| Device compatibility | 10+ device models | Works on all |
| Multilingual tests | All 10 languages | Coherent responses |

---

## 9. Recommendation

### 9.1 Decision Summary

| Option | Recommendation | Confidence |
|--------|----------------|------------|
| Continue MLC only | ❌ Not recommended | - |
| Switch to GGUF only | ⚠️ Conditional | Medium |
| **Hybrid (GGUF primary, MLC fallback)** | ✅ **Recommended** | High |

### 9.2 Rationale

**Why GGUF as primary:**
1. Larger model ecosystem (20x more models)
2. Simpler licensing compliance (MIT vs Apache 2.0 + modifications)
3. Better memory efficiency (mmap support)
4. More active community and faster updates
5. 80% of implementation already exists

**Why keep MLC as fallback:**
1. Some optimized models may perform better
2. Zero-risk transition (can always fall back)
3. Existing investment not wasted
4. Some device-specific optimizations

### 9.3 Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Token generation speed | ≥80% of MLC | Benchmark suite |
| Response quality | No degradation | User surveys, A/B tests |
| Memory usage | ≤100% of MLC | Profiling |
| Model availability | ≥8/10 languages | Model audit |
| User satisfaction | No complaints | App reviews, support tickets |

### 9.4 Timeline

| Milestone | Target Date | Dependencies |
|-----------|-------------|--------------|
| Phase 1 complete | +2 weeks | None |
| Phase 2 complete | +5 weeks | Phase 1 |
| Phase 3 complete | +9 weeks | Phase 2, model conversions |
| Phase 4 complete | +10 weeks | Phase 3, stakeholder approval |

---

## 10. Appendix

### 10.1 File References

**Core LLM Module:**
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/ALCEngine.kt`
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/ALCEngineSingleLanguage.kt`
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/TVMRuntime.kt`
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/provider/LocalLLMProvider.kt`

**Inference Strategies:**
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/inference/MLCInferenceStrategy.kt`
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/inference/GGUFInferenceStrategy.kt`
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/inference/MultiProviderInferenceStrategy.kt`

**Tokenizers:**
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/tokenizer/TVMTokenizer.kt`
- `/Modules/AI/LLM/src/androidMain/kotlin/com/augmentalis/llm/alc/tokenizer/HuggingFaceTokenizer.kt`

**RAG (Unaffected):**
- `/Modules/AI/RAG/src/commonMain/kotlin/com/augmentalis/rag/chat/RAGChatEngine.kt`
- `/Modules/AI/RAG/src/androidMain/kotlin/com/augmentalis/rag/embeddings/ONNXEmbeddingProvider.android.kt`

**Build Configuration:**
- `/Modules/AI/LLM/build.gradle.kts`
- `/Modules/AI/LLM/libs/tvm4j_core.jar`
- `/Modules/AI/LLM/tvm-patches/Module.java`

### 10.2 Naming Standardization (Completed)

During this analysis, inconsistent naming for "ALC" was identified and corrected:

| File | Previous Name | Corrected Name |
|------|---------------|----------------|
| `MIGRATION-REPORT.md` | Augmentalis Local Compute | Adaptive LLM Coordinator |
| `DEPRECATED.md` | AI Language Core | Adaptive LLM Coordinator |
| `README.md` | Adaptive LLM Coordinator | ✅ Already correct |
| `ALCEngine.kt` | Adaptive LLM Coordinator | ✅ Already correct |

**Official Name:** ALC = **Adaptive LLM Coordinator**

**Note:** NLU (Natural Language Understanding) is a separate module (`Modules/AI/NLU/`) and should not be conflated with ALC. The system architecture is:

```
User Input → NLU Module (intent classification) → Chat Coordinators → ALC (response generation)
```

### 10.3 Glossary

| Term | Definition |
|------|------------|
| ALC | Adaptive LLM Coordinator - AVA's LLM orchestration layer |
| GGUF | GPT-Generated Unified Format - llama.cpp's model format |
| MLC-LLM | Machine Learning Compilation for LLMs - TVM-based runtime |
| TVM | Apache TVM - deep learning compiler framework |
| KV Cache | Key-Value cache for transformer attention optimization |
| Q4_K_M | 4-bit quantization with K-means clustering (medium) |
| mmap | Memory-mapped file I/O for efficient model loading |
| ONNX | Open Neural Network Exchange - used for embeddings |

### 10.4 References

1. Apache TVM Documentation: https://tvm.apache.org/docs/
2. MLC-LLM Project: https://mlc.ai/mlc-llm/
3. llama.cpp Repository: https://github.com/ggerganov/llama.cpp
4. GGUF Format Specification: https://github.com/ggerganov/ggml/blob/master/docs/gguf.md
5. HuggingFace Tokenizers: https://huggingface.co/docs/tokenizers/

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| V1 | 2026-01-17 | Manoj Jhawar | Initial analysis |
| V1.1 | 2026-01-17 | Manoj Jhawar | Added naming standardization section; fixed ALC terminology |

---

**Prepared for:** AVA AI Team
**Review Required By:** Engineering Lead, Product Owner
**Decision Deadline:** TBD
