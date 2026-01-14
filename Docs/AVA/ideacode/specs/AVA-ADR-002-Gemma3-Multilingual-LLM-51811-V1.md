# ADR-002: Gemma 3 4B as Universal Multilingual LLM

**Status:** Accepted
**Date:** 2025-11-18
**Decision Makers:** Manoj Jhawar

## Context

AVA currently uses AVA-GEM-2B-Q4 (Gemma 2B) as the on-device LLM, which is primarily optimized for English. For a universal JARVIS-like assistant, we need:

1. **Multilingual support** without switching models per language
2. **Mobile-friendly size** that fits in device memory
3. **Strong performance** competitive with larger models
4. **Future-proof architecture** supporting vision and long context

## Decision

**Upgrade from Gemma 2B to Gemma 3 4B as the primary on-device LLM.**

Model designation: `AVA-GEM-4B-Q4`

## Rationale

### Model Comparison

| Feature | AVA-GEM-2B-Q4 (Current) | AVA-GEM-4B-Q4 (New) |
|---------|-------------------------|---------------------|
| Parameters | 2B | 4B |
| Quantized Size | ~1.2GB | ~2.3GB |
| Languages | ~20-30 | **140+** |
| Context Window | 8K tokens | **128K tokens** |
| Multimodal | No | **Yes (vision)** |
| MATH Benchmark | ~40% | **75.6%** |
| HumanEval | ~35% | **71.3%** |
| Multilingual (MGSM) | 18.7% | **34.7%** |

### Why Gemma 3 4B over alternatives?

1. **vs Gemma 2 9B (~5GB):**
   - Too large for most mobile devices
   - Limited multilingual support (~20-30 languages)
   - Gemma 3 4B performs comparably at half the size

2. **vs Gemma 2 27B (~15GB):**
   - Not feasible for mobile deployment
   - Gemma 3 4B is competitive with Gemma 2 27B on instruction-following
   - Report claims "Gemma3-4B-IT competitive with Gemma2-27B-IT"

3. **vs Qwen 2.5 1.5B:**
   - Better for Asian languages but weaker for others
   - Gemma 3 4B covers 140+ languages uniformly

### Benefits

1. **Universal Language Support**
   - No need for language detection and model switching
   - Simplifies `ModelSelector.kt` and `LanguageDetector.kt`
   - Consistent quality across all languages

2. **Performance Improvements**
   - MATH: 75.6% vs ~40% (88% improvement)
   - HumanEval: 71.3% vs ~35% (104% improvement)
   - Better at complex reasoning and code generation

3. **Future Capabilities**
   - Vision support for image understanding
   - 128K context for long conversations
   - Better foundation for RAG and memory features

4. **Efficiency**
   - Only 1.1GB larger than current model
   - Better performance-per-byte
   - Reduced KV-cache memory via optimized attention

## Technical Implementation

### Model Configuration

```json
{
  "model_id": "gemma-3-4b-it-q4bf16_1-MLC",
  "model_lib": "gemma3_q4bf16_1_[hash]",
  "model_url": "https://huggingface.co/mlc-ai/gemma-3-4b-it-q4bf16_1-MLC",
  "estimated_vram_bytes": 3500000000
}
```

### AVA Naming Convention

- **HuggingFace:** `mlc-ai/gemma-3-4b-it-q4bf16_1-MLC`
- **AVA Internal:** `AVA-GEM-4B-Q4`
- **Asset Path:** `models/AVA-GEM-4B-Q4/`

### Migration Steps

1. Download pre-converted MLC weights from HuggingFace
2. Compile model library for Android target
3. Update `mlc-app-config.json` with new model entry
4. Update `ModelSelector.kt` to use single multilingual model
5. Update `ChatViewModel.kt` LLM configuration
6. Remove language-specific model routing (simplify)
7. Test with multiple languages

### Memory Requirements

| Component | Size |
|-----------|------|
| Model weights | ~2.3GB |
| KV-cache (8K ctx) | ~500MB |
| Runtime overhead | ~200MB |
| **Total** | **~3.0GB** |

Suitable for devices with 4GB+ RAM.

## Consequences

### Positive

- Universal language support out of the box
- Significantly better performance on benchmarks
- Future-ready for multimodal features
- Simplified codebase (no language routing)
- Better user experience in non-English regions

### Negative

- Increased storage requirement (+1.1GB)
- Slightly longer model load time
- Requires MLC-LLM update if not already supporting Gemma 3
- Need to recompile model library for Android

### Risks

1. **MLC-LLM Compatibility:** Gemma 3 is newer; verify library support
2. **Memory Pressure:** 3GB may be tight on 4GB RAM devices
3. **Inference Speed:** Larger model may be slower on low-end devices

### Mitigations

1. Test on reference devices before release
2. Keep AVA-GEM-2B-Q4 as fallback for low-memory devices
3. Implement dynamic model selection based on device capabilities

## Alternatives Considered

### Option A: Keep Gemma 2B + Language-specific models

- Pros: Smaller base model, optimized per language
- Cons: Complex routing, inconsistent quality, maintenance burden
- **Rejected:** Complexity outweighs size savings

### Option B: Gemma 2 9B

- Pros: Better than 2B, well-tested
- Cons: ~5GB too large for most phones, limited languages
- **Rejected:** Not mobile-friendly

### Option C: Wait for Gemma 3 1B

- Pros: Smallest, mobile-optimized
- Cons: No multimodal, likely weaker multilingual
- **Rejected:** 4B is the sweet spot for capabilities

## Implementation Timeline

1. **Phase 1:** Download and compile Gemma 3 4B for Android
2. **Phase 2:** Update configuration and codebase
3. **Phase 3:** Test multilingual performance
4. **Phase 4:** Deploy with fallback to 2B for low-memory

## References

- [Gemma 3 Technical Report](https://arxiv.org/html/2503.19786v1)
- [MLC-LLM Gemma 3 4B](https://huggingface.co/mlc-ai/gemma-3-4b-it-q4bf16_1-MLC)
- [Google Gemma Documentation](https://ai.google.dev/gemma)
- Chapter 38: LLM Model Management
- Chapter 43: Intent Learning System

## Approval

- [x] Technical Lead: Manoj Jhawar
- [ ] Implementation Complete
- [ ] Testing Complete
- [ ] Deployed to Production

---

**Author:** AVA AI Team
**Version:** 1.0
