# AVA Developer Device-Model Matrix

**Last Updated:** 2025-12-01
**Version:** 1.0
**Author:** AVA Development Team
**Audience:** Developers, DevOps, Model Engineers

---

## Model Cross-Reference Table

### LLM Models

| AVA Name | Original Model | HuggingFace Repo | Architecture | Compile Flag |
|----------|----------------|------------------|--------------|--------------|
| AVA-LL32-1B16 | Llama 3.2 1B Instruct | mlc-ai/Llama-3.2-1B-Instruct-q4f16_1-MLC | llama | `--model-type llama` |
| AVA-LL32-3B16 | Llama 3.2 3B Instruct | mlc-ai/Llama-3.2-3B-Instruct-q4f16_1-MLC | llama | `--model-type llama` |
| AVA-QW3-06B16 | Qwen 3 0.6B | mlc-ai/Qwen3-0.6B-q4f16_1-MLC | qwen2 | `--model-type qwen2` |
| AVA-QW3-17B16 | Qwen 3 1.7B | mlc-ai/Qwen3-1.7B-q4f16_1-MLC | qwen2 | `--model-type qwen2` |
| AVA-QW3-4B16 | Qwen 3 4B | mlc-ai/Qwen3-4B-q4f16_1-MLC | qwen2 | `--model-type qwen2` |
| AVA-GE2-2B16 | Gemma 2 2B IT | mlc-ai/gemma-2-2b-it-q4f16_1-MLC | gemma2 | `--model-type gemma2` |
| AVA-GE3N-2B | Gemma 3n E2B | google/gemma-3n-E2B-it (LiteRT) | gemma3n | N/A (LiteRT) |
| AVA-GE3N-4B | Gemma 3n E4B | google/gemma-3n-E4B-it (LiteRT) | gemma3n | N/A (LiteRT) |
| AVA-MI7-7B16 | Mistral 7B v0.3 | mlc-ai/Mistral-7B-Instruct-v0.3-q4f16_1-MLC | mistral | `--model-type mistral` |
| AVA-PH3-3B16 | Phi-3 Mini 4K | mlc-ai/Phi-3-mini-4k-instruct-q4f16_1-MLC | phi3 | `--model-type phi3` |

### NLU/Embedding Models

| AVA Name | Original Model | HuggingFace Repo | Dimensions | Languages |
|----------|----------------|------------------|------------|-----------|
| AVA-384-BASE | MobileBERT Quantized | Custom AVA | 384 | English |
| AVA-768-MULTI | mALBERT Quantized | Custom AVA | 768 | 100+ |
| AVA-MINILM | all-MiniLM-L6-v2 | sentence-transformers/all-MiniLM-L6-v2 | 384 | English |
| AVA-GTE-MULTI | GTE Multilingual Base | Alibaba-NLP/gte-multilingual-base | 768 | 70+ |

---

## Device Inventory

### RealWear Devices

| Device | RAM | GPU | Chipset | Year |
|--------|-----|-----|---------|------|
| HMT-1 | 2GB | Adreno 506 | Snapdragon 625 | 2017 |
| HMT-1Z1 | 3GB | Adreno 506 | Snapdragon 625 | 2019 |
| Navigator 500 | 4GB | Adreno 619 | Snapdragon 662 | 2022 |
| Navigator 520 | 4GB | Adreno 619 | Snapdragon 662 | 2023 |
| Navigator Z1 | 4GB | Adreno 619 | Snapdragon 662 | 2023 |
| Arc 3 | 4GB | Adreno 619 | Snapdragon 662 | 2024 |

### Vuzix Devices

| Device | RAM | GPU | Chipset | Year |
|--------|-----|-----|---------|------|
| M400 | 6GB | Adreno 615 | Snapdragon XR1 | 2020 |
| M400C | 6GB | Adreno 615 | Snapdragon XR1 | 2021 |
| Z100 | 6GB | Adreno 650 | Snapdragon XR2 | 2023 |
| M4000 | 6GB | Adreno 650 | Snapdragon XR2 | 2024 |

### Rokid Devices

| Device | RAM | GPU | Chipset | Year |
|--------|-----|-----|---------|------|
| X-Craft | 4GB | Mali-G57 | MediaTek | 2023 |
| Max Pro | 6GB | Mali-G78 | MediaTek | 2024 |

---

## Optimal Configurations by Device (Developer Reference)

### HMT-1 / HMT-1Z1 (2-3GB RAM)

| Mode | NLU Model | LLM Model | Original LLM | Total RAM |
|------|-----------|-----------|--------------|-----------|
| Base | AVA-384-BASE | AVA-LL32-1B16 | Llama 3.2 1B | 1.57 GB |
| Multi | AVA-384-BASE | AVA-QW3-06B16 | Qwen 3 0.6B | 1.0 GB |

**Notes:**
- AVA-768-MULTI NLU too large for these devices
- AVA-QW3-06B16 provides basic multilingual support at minimal cost
- Original models: Llama 3.2 1B Instruct, Qwen 3 0.6B

---

### Navigator 500/520/Z1 & Arc 3 (4GB RAM)

| Mode | NLU Model | LLM Model | Original LLM | Total RAM |
|------|-----------|-----------|--------------|-----------|
| Base | AVA-768-MULTI | AVA-GE2-2B16 | Gemma 2 2B IT | 1.97 GB |
| Multi | AVA-768-MULTI | AVA-QW3-17B16 | Qwen 3 1.7B | 2.25 GB |
| **Gemma 3n** | AVA-768-MULTI | AVA-GE3N-2B | Gemma 3n E2B | 2.45 GB |

**Notes:**
- AVA-GE2-2B16 best for reasoning/English (original: Gemma 2 2B IT)
- AVA-QW3-17B16 best for multilingual (original: Qwen 3 1.7B)
- AVA-GE3N-2B (Gemma 3n E2B) **now available** via LiteRT - recommended

---

### Vuzix M400 / M400C (6GB RAM)

| Mode | NLU Model | LLM Model | Original LLM | Total RAM |
|------|-----------|-----------|--------------|-----------|
| Base | AVA-768-MULTI | AVA-LL32-3B16 | Llama 3.2 3B | 2.95 GB |
| Multi | AVA-768-MULTI | AVA-GE2-2B16 | Gemma 2 2B IT | 1.97 GB |
| **Gemma 3n** | AVA-768-MULTI | AVA-GE3N-4B | Gemma 3n E4B | 3.45 GB |

**Notes:**
- AVA-LL32-3B16 highest English quality (original: Llama 3.2 3B Instruct)
- AVA-GE3N-4B (Gemma 3n E4B) **now available** - optimal for 6GB devices

---

### Vuzix Z100 / M4000 (6GB RAM, XR2)

| Mode | NLU Model | LLM Model | Original LLM | Total RAM | Speed |
|------|-----------|-----------|--------------|-----------|-------|
| Base | AVA-768-MULTI | AVA-LL32-3B16 | Llama 3.2 3B | 2.95 GB | 25 t/s |
| Premium | AVA-768-MULTI | AVA-QW3-4B16 | Qwen 3 4B | 3.95 GB | 25 t/s |
| **Gemma 3n** | AVA-768-MULTI | AVA-GE3N-4B | Gemma 3n E4B | 3.45 GB | ~25 t/s |

**Notes:**
- XR2 provides fastest inference (Adreno 650)
- AVA-QW3-4B16 (Qwen 3 4B) best overall quality
- AVA-GE3N-4B **now available** - optimal for premium devices

---

### Rokid X-Craft (4GB RAM, Mali)

| Mode | NLU Model | LLM Model | Original LLM | GPU Target |
|------|-----------|-----------|--------------|------------|
| Base | AVA-768-MULTI | AVA-GE2-2B16-MALI | Gemma 2 2B IT | Mali OpenCL |
| Multi | AVA-768-MULTI | AVA-QW3-17B16-MALI | Qwen 3 1.7B | Mali OpenCL |
| **Gemma 3n** | AVA-768-MULTI | AVA-GE3N-2B | Gemma 3n E2B | LiteRT Mali |

**Notes:**
- Requires Mali-compiled models (llm-mali/ directory)
- AVA-GE3N-2B **now available** with native Mali support via LiteRT

---

### Rokid Max Pro (6GB RAM, Mali-G78)

| Mode | NLU Model | LLM Model | Original LLM | GPU Target |
|------|-----------|-----------|--------------|------------|
| Base | AVA-768-MULTI | AVA-LL32-3B16-MALI | Llama 3.2 3B | Mali OpenCL |
| Multi | AVA-768-MULTI | AVA-QW3-4B16-MALI | Qwen 3 4B | Mali OpenCL |
| **Gemma 3n** | AVA-768-MULTI | AVA-GE3N-4B | Gemma 3n E4B | LiteRT Mali |

**Notes:**
- Mali-G78 is high-performance GPU
- AVA-GE3N-4B **now available** via LiteRT with Mali support

---

### Samsung Galaxy S23/S24 (8-12GB RAM)

| Mode | NLU Model | LLM Model | Original LLM | Speed |
|------|-----------|-----------|--------------|-------|
| Premium | AVA-768-MULTI | AVA-QW3-4B16 | Qwen 3 4B | 30+ t/s |
| **Gemma 3n** | AVA-768-MULTI | AVA-GE3N-4B | Gemma 3n E4B | ~35 t/s |

**Notes:**
- Flagship Adreno 740/750 GPUs
- AVA-GE3N-4B **now available** - optimal for flagship devices

---

### iPhone 15/16 Pro (8-12GB RAM)

| Mode | NLU Model | LLM Model | Original LLM | Speed |
|------|-----------|-----------|--------------|-------|
| Premium | AVA-768-MULTI | AVA-QW3-4B16-IOS | Qwen 3 4B | 35+ t/s |
| **Gemma 3n** | AVA-768-MULTI | AVA-GE3N-4B | Gemma 3n E4B | ~40 t/s |

**Notes:**
- Apple Neural Engine acceleration
- CoreML for NLU, MLC-LLM (Metal) for LLM
- AVA-GE3N-4B **now available** via LiteRT (CoreML backend)

---

## Runtime Requirements

### MLC-LLM (TVM)

| Component | Size | Notes |
|-----------|------|-------|
| libmlc_llm.so | 62 MB | Android native library |
| libtvm_runtime.so | included | TVM runtime |
| tvm4j_core.jar | 2 MB | Java bindings |

**Supported architectures:** llama, qwen2, gemma2, gemma3, mistral, phi3

### LiteRT (TensorFlow Lite)

| Component | Size | Notes |
|-----------|------|-------|
| libtensorflowlite.so | ~5 MB | Core runtime |
| libtensorflowlite_gpu.so | ~15 MB | GPU delegate |

**Supported architectures:** gemma3n (native, **now available**), gemma2 (converted)

### llama.cpp (GGUF)

| Component | Size | Notes |
|-----------|------|-------|
| libllama.adco | ~15 MB | AVA distribution |

**Supported architectures:** All GGUF models

---

## Model Directory Structure

```
ava-ai-models-external/
├── embeddings/
│   ├── AVA-384-BASE/                    # MobileBERT English (200 MB)
│   ├── AVA-768-MULTI/                   # mALBERT Multilingual (450 MB)
│   ├── all-MiniLM-L6-v2-raw/            # Source for AVA-MINILM
│   └── GTE-Multilingual-Base-raw/       # Source for AVA-GTE-MULTI
├── llm/
│   ├── AVA-LL32-1B16/                   # Llama 3.2 1B (660 MB)
│   ├── AVA-LL32-3B16/                   # Llama 3.2 3B (1.69 GB)
│   ├── AVA-QW3-06B16/                   # Qwen 3 0.6B (330 MB)
│   ├── AVA-QW3-17B16/                   # Qwen 3 1.7B (920 MB)
│   ├── AVA-QW3-4B16/                    # Qwen 3 4B (2.12 GB)
│   └── AVA-GE2-2B16/                    # Gemma 2 2B (1.39 GB)
├── llm-mali/                            # Mali OpenCL variants
│   ├── AVA-GE2-2B16-MALI/
│   └── AVA-QW3-17B16-MALI/
├── llm-litert/                          # LiteRT models (now available)
│   ├── AVA-GE3N-2B/                     # Gemma 3n E2B
│   └── AVA-GE3N-4B/                     # Gemma 3n E4B
├── llm-gguf/                            # GGUF models (future)
│   └── AVA-GE3N-2B-Q4/
├── wakeword/                            # Porcupine wake words (1 MB)
└── bundled/                             # APK bundled models (22 MB)
```

---

## Compilation Commands

### MLC-LLM Cross-GPU Compilation

```bash
# Gemma 2 2B for Adreno
mlc_llm compile \
    --model mlc-ai/gemma-2-2b-it-q4f16_1-MLC \
    --model-type gemma2 \
    --target android \
    --device adreno \
    --output AVA-GE2-2B16

# Qwen 3 1.7B for Adreno
mlc_llm compile \
    --model mlc-ai/Qwen3-1.7B-q4f16_1-MLC \
    --model-type qwen2 \
    --target android \
    --device adreno \
    --output AVA-QW3-17B16

# Llama 3.2 3B for Adreno
mlc_llm compile \
    --model mlc-ai/Llama-3.2-3B-Instruct-q4f16_1-MLC \
    --model-type llama \
    --target android \
    --device adreno \
    --output AVA-LL32-3B16

# Gemma 2 2B for Mali (Rokid devices)
mlc_llm compile \
    --model mlc-ai/gemma-2-2b-it-q4f16_1-MLC \
    --model-type gemma2 \
    --target android \
    --device mali \
    --output AVA-GE2-2B16-MALI
```

### Model Type Reference

| AVA Model Pattern | Original Architecture | --model-type |
|-------------------|-----------------------|--------------|
| AVA-LL32-* | Llama 3.2 | llama |
| AVA-QW3-* (0.6B-4B) | Qwen 3 | qwen2 |
| AVA-GE2-* | Gemma 2 | gemma2 |
| AVA-GE3-* | Gemma 3 | gemma3 |
| AVA-MI7-* | Mistral 7B | mistral |
| AVA-PH3-* | Phi-3 | phi3 |

---

## Performance Benchmarks

### Token Generation Speed (t/s)

| Model | HMT-1 | Nav 500 | Z100 | Galaxy S24 |
|-------|-------|---------|------|------------|
| AVA-LL32-1B16 | 12 | 18 | 22 | 28 |
| AVA-QW3-06B16 | 15 | 20 | 25 | 32 |
| AVA-QW3-17B16 | 8 | 15 | 22 | 28 |
| AVA-GE2-2B16 | 6 | 12 | 18 | 25 |
| AVA-LL32-3B16 | N/A | 10 | 16 | 22 |
| AVA-QW3-4B16 | N/A | 8 | 14 | 20 |

### Memory Usage (Peak)

| Model | RAM Usage | +NLU 384 | +NLU 768 |
|-------|-----------|----------|----------|
| AVA-LL32-1B16 | 1.37 GB | 1.57 GB | 1.82 GB |
| AVA-QW3-06B16 | 0.8 GB | 1.0 GB | 1.25 GB |
| AVA-QW3-17B16 | 1.8 GB | 2.0 GB | 2.25 GB |
| AVA-GE2-2B16 | 1.52 GB | 1.72 GB | 1.97 GB |
| AVA-LL32-3B16 | 2.5 GB | 2.7 GB | 2.95 GB |
| AVA-QW3-4B16 | 3.5 GB | 3.7 GB | 3.95 GB |

---

## Related Documentation

- **User-Manual-Chapter12-Model-Selection-Guide.md** - User-facing model guide (AVA names only)
- **AVA-DEVICE-MODEL-MATRIX.md** - User-facing device matrix (AVA names only)
- **Developer-Manual-Chapter54-Cross-GPU-Model-Compilation.md** - GPU compilation guide
- **ALM-LITERT-FORMAT-SPEC.md** - LiteRT format specification
- **ALM-GGUF-FORMAT-SPEC.md** - GGUF format specification
- **FORMAT-REGISTRY.md** - Complete format registry

---

**Document Version:** 1.1
**Last Updated:** 2025-11-30
