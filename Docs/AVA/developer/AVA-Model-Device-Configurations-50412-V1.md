# AVA Model Device Configurations

**Author:** Manoj Jhawar
**Updated:** 2025-12-04

---

## Device Tier Overview

| Tier | Example Devices | RAM | CPU | GPU | Max Model Size |
|------|-----------------|-----|-----|-----|----------------|
| High | ARC 3, Pixel 8 Pro, Samsung S24 | 12GB+ | Snapdragon 8 Gen 3 | Adreno 750 | 4B |
| Mid | Pixel 6, Samsung A54 | 6-8GB | Snapdragon 7 Gen 1 | Adreno 644 | 2B |
| Low | RealWear HMT-1, Budget phones | 2-4GB | Snapdragon 625 | Adreno 506 | 0.6B-1B |

---

## Configuration Matrix

### Tier 1: High Performance (ARC 3, Flagship)

**Use Case:** Multilingual, maximum quality, complex tasks

| Component | Model | Size | Format | Status |
|-----------|-------|------|--------|--------|
| LLM | AVA-GE3-4B16 | ~2.5GB | TVM | Ready |
| Embeddings | AVA-768-Multi-INT8 | ~150MB | ONNX | Ready |
| Tokenizer | tokenizer.ats | 4.5MB | AVA | Ready |

**Required Files:**
```
external-models/llm/AVA-GE3-4B16/
├── AVA-GE3-4B16.ads          # 5.8MB - Compiled model
├── tokenizer.ats             # 4.5MB - Tokenizer
├── tokenizer.json            # 33MB - HF tokenizer
├── mlc-chat-config.json      # 2KB - Config
├── ndarray-cache.json        # 257KB - Weight index
└── params_shard_*.bin        # ~2.2GB - Weights (69 files)

external-models/embeddings/AVA-768-Multi-INT8/
└── AVA-768-Multi-INT8.AON    # ~150MB - Embedding model
```

**Performance:**
- Inference: ~15 tokens/sec
- First token latency: ~800ms
- Memory footprint: ~4GB
- Languages: 100+ (multilingual)

---

### Tier 2: Balanced Performance (Mid-range)

**Use Case:** English-first with multilingual fallback

| Component | Model | Size | Format | Status |
|-----------|-------|------|--------|--------|
| LLM | AVA-GE2-2B16 | ~1.2GB | TVM | Ready |
| Embeddings | AVA-384-Multi-INT8 | ~90MB | ONNX | Ready |
| Tokenizer | tokenizer.ats | 4.0MB | AVA | Ready |

**Required Files:**
```
external-models/llm/AVA-GE2-2B16/
├── AVA-GE2-2B16.ads          # 3.6MB - Compiled model
├── tokenizer.ats             # 4.0MB - Tokenizer
├── tokenizer.json            # 17MB - HF tokenizer
├── mlc-chat-config.json      # 2KB - Config
├── ndarray-cache.json        # 128KB - Weight index
└── params_shard_*.bin        # ~1.0GB - Weights (42 files)

external-models/embeddings/AVA-384-Multi-INT8/
└── AVA-384-Multi-INT8.AON    # ~90MB - Embedding model
```

**Performance:**
- Inference: ~25 tokens/sec
- First token latency: ~400ms
- Memory footprint: ~2GB
- Languages: 100+ (multilingual)

---

### Tier 3: Low-Spec Optimized (RealWear HMT-1)

**Use Case:** English-only, basic commands, industrial

| Component | Model | Size | Format | Status |
|-----------|-------|------|--------|--------|
| LLM (Option A) | AVA-QW3-06B16 | ~400MB | TVM | Needs compile |
| LLM (Option B) | AVA-LL32-1B16 | ~600MB | TVM | Needs compile |
| Embeddings | AVA-384-Base-INT8 | ~45MB | ONNX | Ready |
| Tokenizer | tokenizer.ats | 2.5MB | AVA | Ready |

**Required Files (Option A - Smallest):**
```
external-models/llm/AVA-QW3-06B16/
├── AVA-QW3-06B16.ads         # TBD - Compiled model (NEEDS BUILD)
├── tokenizer.ats             # TBD - Tokenizer (NEEDS CONVERT)
├── tokenizer.json            # Existing
├── vocab.json                # Existing
├── mlc-chat-config.json      # Existing
└── params_shard_*.bin        # ~350MB - Weights (9 files)

external-models/embeddings/AVA-384-Base-INT8/
└── AVA-384-Base-INT8.AON     # ~45MB - Embedding model
```

**Performance (Estimated):**
- Inference: ~40 tokens/sec
- First token latency: ~200ms
- Memory footprint: ~800MB
- Languages: English only

---

### Tier 4: GGUF Alternative (Any Device)

**Use Case:** llama.cpp backend, no TVM dependency

| Component | Model | Size | Format | Status |
|-----------|-------|------|--------|--------|
| LLM | AVA-GE3N-E4B16 | 4.2GB | GGUF | Ready |
| Embeddings | AVA-384-Multi-INT8 | ~90MB | ONNX | Ready |

**Required Files:**
```
external-models/llm/AVA-GE3N-E4B16/
├── gemma-3n-E4B-it-Q4_K_M.gguf  # 4.2GB - GGUF model
├── mlc-chat-config.json         # 1KB - Config
└── template                     # 358B - Prompt template

external-models/embeddings/AVA-384-Multi-INT8/
└── AVA-384-Multi-INT8.AON       # ~90MB - Embedding model
```

**Performance:**
- Inference: ~20 tokens/sec (Q4_K_M quantization)
- First token latency: ~500ms
- Memory footprint: ~5GB
- Languages: Multilingual

---

## Recommended Configurations Summary

| Priority | Device Type | LLM | Embeddings | Total Size |
|----------|-------------|-----|------------|------------|
| 1 | High-end (ARC 3) | AVA-GE3-4B16 | AVA-768-Multi-INT8 | ~2.6GB |
| 2 | Mid-range | AVA-GE2-2B16 | AVA-384-Multi-INT8 | ~1.4GB |
| 3 | Low-spec (HMT-1) | AVA-QW3-06B16* | AVA-384-Base-INT8 | ~450MB |
| 4 | GGUF fallback | AVA-GE3N-E4B16 | AVA-384-Multi-INT8 | ~4.4GB |

*Requires TVM compilation (missing .ads file)

---

## Models Requiring TVM Compilation

| Model | Status | Files Present | Missing |
|-------|--------|---------------|---------|
| AVA-QW3-06B16 | Weights only | params_shard_*.bin, config | .adm, .ads |
| AVA-QW3-17B16 | Weights only | params_shard_*.bin, config | .adm, .ads |
| AVA-QW3-4B16 | Weights only | params_shard_*.bin, config | .adm, .ads |
| AVA-LL32-1B16 | Weights only | params_shard_*.bin | .adm, .ads, config |
| AVA-LL32-3B16 | Weights only | params_shard_*.bin, config | .adm, .ads |

**To compile:** Run MLC-LLM compilation to generate .adm files, then use `build-model.sh` to create .ads

---

## Embedding Model Comparison

| Model | Dimensions | Size | Languages | Use Case |
|-------|------------|------|-----------|----------|
| AVA-384-Base-INT8 | 384 | ~45MB | English | Low-spec, English-only |
| AVA-384-Multi-INT8 | 384 | ~90MB | 100+ | Mid-range, multilingual |
| AVA-768-Multi-INT8 | 768 | ~150MB | 100+ | High-end, best accuracy |

---

## RealWear HMT-1 Specific Notes

| Constraint | Value | Impact |
|------------|-------|--------|
| RAM | 2GB | Max 800MB for model |
| CPU | Snapdragon 625 (8-core) | Limited parallel compute |
| GPU | Adreno 506 | No GPU inference |
| Storage | 16GB | Need smallest models |
| Android | 8.1 | API 27 compatibility |

**Recommended Setup:**
1. AVA-QW3-06B16 (once compiled) - 350MB weights
2. AVA-384-Base-INT8 embeddings - 45MB
3. CPU-only inference (disable GPU)
4. Reduced context length (512 tokens max)
5. Aggressive memory cleanup between inferences

---

## ARC 3 (High-Performance) Notes

| Capability | Value | Impact |
|------------|-------|--------|
| RAM | 12GB+ | Full 4B models possible |
| CPU | Snapdragon 8 Gen 3 | Fast CPU inference |
| GPU | Adreno 750 | GPU acceleration available |
| NPU | Hexagon | Hardware AI acceleration |
| Storage | 256GB+ | All models fit |

**Recommended Setup:**
1. AVA-GE3-4B16 - Full quality multilingual
2. AVA-768-Multi-INT8 - Highest embedding accuracy
3. GPU inference enabled
4. Full context length (4096 tokens)
5. Batch processing supported

---

## File Verification Checklist

### Pre-deployment verification:
```bash
# Check .ads file exists and has symbols
llvm-nm -u external-models/llm/AVA-GE3-4B16/AVA-GE3-4B16.ads

# Verify weight count matches config
ls external-models/llm/AVA-GE3-4B16/params_shard_*.bin | wc -l

# Check tokenizer
file external-models/llm/AVA-GE3-4B16/tokenizer.ats

# Verify embedding model
file external-models/embeddings/AVA-768-Multi-INT8/AVA-768-Multi-INT8.AON
```

---

## Runtime Selection Logic

```kotlin
fun selectModelConfig(deviceProfile: DeviceProfile): ModelConfig {
    return when {
        deviceProfile.ramGB >= 8 && deviceProfile.hasGPU ->
            ModelConfig.HIGH_PERFORMANCE  // AVA-GE3-4B16 + 768-Multi

        deviceProfile.ramGB >= 4 ->
            ModelConfig.BALANCED          // AVA-GE2-2B16 + 384-Multi

        deviceProfile.ramGB >= 2 ->
            ModelConfig.LOW_SPEC          // AVA-QW3-06B16 + 384-Base

        else ->
            ModelConfig.MINIMAL           // Intent-only mode
    }
}
```
