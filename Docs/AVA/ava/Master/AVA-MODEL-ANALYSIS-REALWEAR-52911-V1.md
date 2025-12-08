# AVA Model Analysis for Wearable & Mobile Devices

**Date:** 2025-11-29
**Devices:** RealWear, Vuzix, Rokid, Samsung, Motorola, Apple iPhone
**Purpose:** Find optimal LLM + Embedding combinations for Jarvis-like assistant

---

## Device Specifications

### RealWear HMT-1 (Industrial Wearable)
| Spec | Value |
|------|-------|
| **CPU** | Qualcomm Snapdragon 625/626 (8-core 2.0 GHz) |
| **GPU** | Adreno 506 |
| **RAM** | 2-3 GB |
| **Storage** | 16-32 GB |
| **OS** | Android 6.0.1 (AOSP) |
| **Target Models** | ≤1B params, ≤600MB |

### RealWear Arc 3 (Consumer Wearable)
| Spec | Value |
|------|-------|
| **CPU** | Qualcomm Snapdragon 662 (8-core) |
| **GPU** | Adreno 610 |
| **RAM** | 4 GB |
| **Storage** | 64 GB |
| **OS** | Android 13 |
| **Target Models** | ≤3B params, ≤2GB |

### RealWear Navigator 500 (Industrial Wearable)
| Spec | Value |
|------|-------|
| **CPU** | Qualcomm Snapdragon 662 (8-core 2.0 GHz) |
| **GPU** | Adreno 610 |
| **RAM** | 4 GB |
| **Storage** | 64 GB |
| **OS** | Android 11 (AOSP) + WearHF |
| **Target Models** | ≤3B params, ≤2GB |

### RealWear Navigator 520 (Industrial Wearable - Enhanced)
| Spec | Value |
|------|-------|
| **CPU** | Qualcomm Snapdragon 662 (8-core 2.0 GHz) |
| **GPU** | Adreno 610 |
| **RAM** | 4 GB |
| **Storage** | 64 GB |
| **Display** | 20% larger, 2x resolution vs Nav 500 |
| **OS** | Android 13 (AOSP) + WearHF |
| **Target Models** | ≤3B params, ≤2GB |

### RealWear Navigator Z1 (Intrinsically Safe)
| Spec | Value |
|------|-------|
| **CPU** | Qualcomm Snapdragon 662 (8-core 2.0 GHz) |
| **GPU** | Adreno 610 |
| **RAM** | 4 GB |
| **Storage** | 64 GB |
| **Connectivity** | WiFi 6/6E, Bluetooth 5.2, 5G option |
| **OS** | Android 13 (AOSP) + WearHF |
| **Certification** | ATEX/IECEx Zone 1/21 |
| **Target Models** | ≤3B params, ≤2GB |

### Vuzix M400/M4000 (Enterprise AR)
| Spec | Value |
|------|-------|
| **CPU** | Qualcomm Snapdragon XR1 (8-core Kryo 260) |
| **GPU** | Adreno 615 |
| **RAM** | 6 GB |
| **Storage** | 64 GB |
| **OS** | Android 9 |
| **Target Models** | ≤3B params, ≤2GB |

### Rokid X-Craft (Industrial AR)
| Spec | Value |
|------|-------|
| **CPU** | Amlogic A311D (4x Cortex-A73 + 2x A53) |
| **GPU** | Mali-G52 MP4 |
| **NPU** | 5.0 TOPS |
| **RAM** | 4 GB |
| **Storage** | 64 GB |
| **OS** | YodaOS-XR (Android-based) |
| **Target Models** | ≤3B params, ≤2GB |

### Samsung Galaxy S23/S24 (Flagship Phone)
| Spec | S23 | S24 |
|------|-----|-----|
| **CPU** | Snapdragon 8 Gen 2 for Galaxy | Snapdragon 8 Gen 3 for Galaxy |
| **GPU** | Adreno 740 | Adreno 750 |
| **NPU** | Hexagon | Hexagon (45 TOPS) |
| **RAM** | 8-12 GB | 8-12 GB |
| **Storage** | 128-512 GB | 128-512 GB |
| **OS** | Android 13+ | Android 14+ |
| **Target Models** | ≤7B params, ≤4GB | ≤7B params, ≤4GB |

### Motorola Razr+ 2024 / Edge 50 Ultra (Flagship Phone)
| Spec | Value |
|------|-------|
| **CPU** | Snapdragon 8s Gen 3 (8-core) |
| **GPU** | Adreno 735 |
| **NPU** | Hexagon (12 TOPS) |
| **RAM** | 12 GB |
| **Storage** | 256-512 GB |
| **OS** | Android 14 |
| **Target Models** | ≤7B params, ≤4GB |

### Apple iPhone 15/16 Pro (iOS Flagship)
| Spec | iPhone 15 Pro | iPhone 16 Pro |
|------|---------------|---------------|
| **CPU** | A17 Pro (6-core) | A18 Pro (6-core) |
| **GPU** | 6-core GPU | 6-core GPU (hardware ray-tracing) |
| **Neural Engine** | 16-core (35 TOPS) | 16-core (35 TOPS) |
| **RAM** | 8 GB | 8 GB |
| **Storage** | 128GB-1TB | 128GB-1TB |
| **OS** | iOS 17+ | iOS 18+ |
| **Target Models** | ≤7B params, ≤4GB (CoreML) | ≤7B params, ≤4GB (CoreML) |

---

## Downloaded Models

### LLM Models (MLC-LLM Format) - VERIFIED SIZES

| AVA Name | Source | Size | Params | Target Device | Language |
|----------|--------|------|--------|---------------|----------|
| **AVA-QW3-06B16** | mlc-ai/Qwen3-0.6B-q4f16_1-MLC | 335 MB | 0.6B | HMT-1 | Multilingual |
| **AVA-QW3-17B16** | mlc-ai/Qwen3-1.7B-q4f16_1-MLC | 939 MB | 1.7B | Arc 3 | Multilingual |
| **AVA-QW3-4B16** | mlc-ai/Qwen3-4B-q4f16_1-MLC | 2.1 GB | 4B | High-end | Multilingual |
| **AVA-LL32-1B16** | mlc-ai/Llama-3.2-1B-Instruct-q4f16_1-MLC | 672 MB | 1B | HMT-1 | English |
| **AVA-LL32-3B16** | mlc-ai/Llama-3.2-3B-Instruct-q4f16_1-MLC | 1.7 GB | 3B | Arc 3 | English |
| **AVA-GE2-2B16** | mlc-ai/gemma-2-2b-it-q4f16_1-MLC | 1.4 GB | 2B | Arc 3 | English |
| **AVA-GE3-4B16** | Gemma 3 4B | 2.1 GB | 4B | High-end | Multilingual |

### Embedding Models (ONNX Format)

| AVA Name | Source | Size | Dim | Target | Language |
|----------|--------|------|-----|--------|----------|
| **AVA-384-Base-INT8.AON** | MobileBERT | 22MB | 384 | HMT-1 | English |
| **AVA-384-Multi-INT8.AON** | MiniLM-L12-Multi | 113MB | 384 | HMT-1/Arc 3 | Multilingual |
| **AVA-768-Multi-INT8.AON** | mALBERT | 266MB | 768 | Arc 3 | Multilingual |
| **all-MiniLM-L6-v2** | sentence-transformers | ~85MB | 384 | HMT-1 | English |
| **GTE-Multilingual-Base** | Alibaba-NLP | ~600MB | 768 | Arc 3 | Multilingual |

---

## Performance Analysis

### Estimated Inference Speed (tokens/sec)

Based on Snapdragon/Apple benchmarks and MLC-LLM performance data:

#### AR Glasses & Industrial Wearables (2-6GB RAM)

| Model | HMT-1 (SD625) | Nav 500/520/Z1 & Arc 3 (SD662) | Vuzix (XR1) | Rokid (A311D) |
|-------|---------------|--------------------------------|-------------|---------------|
| Qwen3-0.6B | 15-20 tok/s | 25-35 tok/s | 20-30 tok/s | 18-25 tok/s |
| Llama-3.2-1B | 8-12 tok/s | 18-25 tok/s | 15-20 tok/s | 12-18 tok/s |
| Qwen3-1.7B | ❌ Too slow | 12-18 tok/s | 10-15 tok/s | 8-12 tok/s |
| Llama-3.2-3B | ❌ Won't fit | 8-12 tok/s | 8-12 tok/s | ❌ Won't fit |
| Gemma 2 2B | ❌ Won't fit | 10-15 tok/s | 10-14 tok/s | ❌ Won't fit |

#### Flagship Smartphones (8-12GB RAM)

| Model | Samsung S23/S24 | Motorola Razr+ | iPhone 15/16 Pro | Notes |
|-------|-----------------|----------------|------------------|-------|
| Qwen3-0.6B | 60-80 tok/s | 50-70 tok/s | 70-100 tok/s | Ultra-fast |
| Llama-3.2-1B | 45-60 tok/s | 40-55 tok/s | 55-75 tok/s | Fast |
| Qwen3-1.7B | 35-50 tok/s | 30-45 tok/s | 45-65 tok/s | Good balance |
| Llama-3.2-3B | 25-40 tok/s | 22-35 tok/s | 35-50 tok/s | High quality |
| Gemma 2 2B | 30-45 tok/s | 28-40 tok/s | 40-55 tok/s | Good English |
| Qwen3-4B | 18-28 tok/s | 15-25 tok/s | 25-40 tok/s | Best quality |
| Gemma 3 4B | 16-25 tok/s | 14-22 tok/s | 22-35 tok/s | Flagship quality |

*Note: iPhone uses CoreML/Neural Engine; Android uses MLC-LLM with GPU acceleration*

### RAM Usage Estimates (Verified Sizes)

#### AR Glasses & Industrial Wearables

| Model | Model Size | Runtime | Total | HMT-1 (2-3GB) | Arc 3/Rokid (4GB) | Vuzix (6GB) |
|-------|------------|---------|-------|---------------|-------------------|-------------|
| Qwen3-0.6B | 335 MB | ~400MB | ~735MB | ✅ Safe | ✅ Safe | ✅ Safe |
| Llama-3.2-1B | 672 MB | ~500MB | ~1.2GB | ⚠️ Tight | ✅ Safe | ✅ Safe |
| Qwen3-1.7B | 939 MB | ~600MB | ~1.5GB | ❌ No | ✅ Safe | ✅ Safe |
| Gemma 2 2B | 1.4 GB | ~700MB | ~2.1GB | ❌ No | ⚠️ Tight | ✅ Safe |
| Llama-3.2-3B | 1.7 GB | ~800MB | ~2.5GB | ❌ No | ⚠️ Tight | ✅ Safe |

#### Flagship Smartphones

| Model | Model Size | Runtime | Total | iPhone (8GB) | Samsung (8-12GB) | Motorola (12GB) |
|-------|------------|---------|-------|--------------|------------------|-----------------|
| Qwen3-0.6B | 335 MB | ~400MB | ~735MB | ✅ Safe | ✅ Safe | ✅ Safe |
| Llama-3.2-1B | 672 MB | ~500MB | ~1.2GB | ✅ Safe | ✅ Safe | ✅ Safe |
| Qwen3-1.7B | 939 MB | ~600MB | ~1.5GB | ✅ Safe | ✅ Safe | ✅ Safe |
| Gemma 2 2B | 1.4 GB | ~700MB | ~2.1GB | ✅ Safe | ✅ Safe | ✅ Safe |
| Llama-3.2-3B | 1.7 GB | ~800MB | ~2.5GB | ✅ Safe | ✅ Safe | ✅ Safe |
| Qwen3-4B | 2.1 GB | ~1.0GB | ~3.1GB | ✅ Safe | ✅ Safe | ✅ Safe |
| Gemma 3 4B | 2.1 GB | ~1.0GB | ~3.1GB | ✅ Safe | ✅ Safe | ✅ Safe |

---

## Recommendations

### HMT-1 (Most Constrained - 2-3GB RAM)

#### English Configuration
```
LLM: AVA-LL32-1B16 (Llama 3.2 1B) - 600MB
Embedding: AVA-384-Base-INT8.AON - 22MB
Total: ~1.1GB (safe for 2-3GB device)

Speed: 8-12 tokens/sec
Latency: 500-800ms first token
```

#### Multilingual Configuration
```
LLM: AVA-QW3-06B16 (Qwen3 0.6B) - 350MB
Embedding: AVA-384-Multi-INT8.AON - 113MB
Total: ~750MB (safe for 2-3GB device)

Speed: 15-20 tokens/sec
Latency: 300-500ms first token
```

**RECOMMENDED FOR HMT-1:** Qwen3-0.6B + AVA-384-Multi for best speed and multilingual support.

---

### Arc 3 (4GB RAM)

#### English Configuration
```
LLM: AVA-LL32-3B16 (Llama 3.2 3B) - 1.8GB
Embedding: AVA-384-Base-INT8.AON - 22MB
Total: ~2.6GB (fits in 4GB)

Speed: 8-12 tokens/sec
Latency: 600-1000ms first token
Quality: Best English comprehension
```

#### Multilingual Configuration
```
LLM: AVA-QW3-17B16 (Qwen3 1.7B) - 1.0GB
Embedding: AVA-384-Multi-INT8.AON - 113MB
Total: ~1.6GB (safe for 4GB device)

Speed: 12-18 tokens/sec
Latency: 400-700ms first token
Quality: Good multilingual (29+ languages)
```

**RECOMMENDED FOR ARC 3:** Qwen3-1.7B + AVA-384-Multi for balanced speed/quality multilingual.

---

### Navigator 500/520/Z1 (4GB RAM - Same as Arc 3)

*Note: Navigator 500, 520, and Z1 share identical CPU/RAM specs with Arc 3 (SD662, 4GB). Same model recommendations apply.*

#### English Configuration
```
LLM: AVA-LL32-3B16 (Llama 3.2 3B) - 1.8GB
Embedding: AVA-384-Base-INT8.AON - 22MB
Total: ~2.6GB (fits in 4GB)

Speed: 8-12 tokens/sec
Latency: 600-1000ms first token
Quality: Best English comprehension
```

#### Multilingual Configuration
```
LLM: AVA-QW3-17B16 (Qwen3 1.7B) - 1.0GB
Embedding: AVA-384-Multi-INT8.AON - 113MB
Total: ~1.6GB (safe for 4GB device)

Speed: 12-18 tokens/sec
Latency: 400-700ms first token
Quality: Good multilingual (29+ languages)
```

**RECOMMENDED FOR NAVIGATOR:** Qwen3-1.7B + AVA-384-Multi (same as Arc 3).

---

### Vuzix M400/M4000 (6GB RAM)

#### English Configuration
```
LLM: AVA-LL32-3B16 (Llama 3.2 3B) - 1.7GB
Embedding: AVA-384-Base-INT8.AON - 22MB
Total: ~2.5GB (comfortable for 6GB device)

Speed: 8-12 tokens/sec
Latency: 500-800ms first token
Quality: Best English comprehension
```

#### Multilingual Configuration
```
LLM: AVA-GE2-2B16 (Gemma 2 2B) - 1.4GB
Embedding: AVA-768-Multi-INT8.AON - 266MB
Total: ~2.4GB (comfortable for 6GB device)

Speed: 10-14 tokens/sec
Latency: 400-600ms first token
Quality: High-quality multilingual
```

**RECOMMENDED FOR VUZIX:** Gemma-2-2B + AVA-768-Multi for best quality with multilingual support.

---

### Rokid X-Craft (4GB RAM + 5 TOPS NPU)

#### English Configuration
```
LLM: AVA-LL32-3B16 (Llama 3.2 3B) - 1.7GB
Embedding: AVA-384-Base-INT8.AON - 22MB
Total: ~2.5GB (fits with NPU acceleration)

Speed: 8-12 tokens/sec (NPU-accelerated)
Latency: 500-800ms first token
Quality: Good English comprehension
```

#### Multilingual Configuration
```
LLM: AVA-QW3-17B16 (Qwen3 1.7B) - 939MB
Embedding: AVA-384-Multi-INT8.AON - 113MB
Total: ~1.5GB (safe for 4GB device)

Speed: 8-12 tokens/sec (NPU-accelerated)
Latency: 400-600ms first token
Quality: Good multilingual (29+ languages)
```

**RECOMMENDED FOR ROKID:** Qwen3-1.7B + AVA-384-Multi for reliable multilingual with NPU.

---

### Samsung Galaxy S23/S24 (8-12GB RAM)

#### Fast Configuration (Instant Response)
```
LLM: AVA-QW3-17B16 (Qwen3 1.7B) - 939MB
Embedding: AVA-384-Multi-INT8.AON - 113MB
Total: ~1.5GB (plenty of headroom)

Speed: 35-50 tokens/sec
Latency: 150-250ms first token
Quality: Good multilingual, fast responses
```

#### Flagship Configuration (Best Quality)
```
LLM: AVA-QW3-4B16 (Qwen3 4B) - 2.1GB
Embedding: AVA-768-Multi-INT8.AON - 266MB
Total: ~3.4GB (fits comfortably in 8GB)

Speed: 18-28 tokens/sec
Latency: 200-400ms first token
Quality: Flagship multilingual (29+ languages)
```

**RECOMMENDED FOR SAMSUNG:** Qwen3-4B + AVA-768-Multi for best-in-class on-device AI.

---

### Motorola Razr+ 2024 / Edge 50 Ultra (12GB RAM)

#### Fast Configuration
```
LLM: AVA-LL32-3B16 (Llama 3.2 3B) - 1.7GB
Embedding: AVA-384-Multi-INT8.AON - 113MB
Total: ~2.5GB (excellent headroom)

Speed: 22-35 tokens/sec
Latency: 180-300ms first token
Quality: Excellent English
```

#### Flagship Configuration
```
LLM: AVA-QW3-4B16 (Qwen3 4B) - 2.1GB
Embedding: AVA-768-Multi-INT8.AON - 266MB
Total: ~3.4GB (excellent headroom)

Speed: 15-25 tokens/sec
Latency: 250-400ms first token
Quality: Flagship multilingual
```

**RECOMMENDED FOR MOTOROLA:** Qwen3-4B + AVA-768-Multi for flagship experience.

---

### Apple iPhone 15/16 Pro (8GB RAM + Neural Engine)

*Note: Uses CoreML for Neural Engine acceleration (35 TOPS). Requires model conversion to CoreML format.*

#### Fast Configuration (CoreML)
```
LLM: AVA-QW3-17B16 (Qwen3 1.7B) - 939MB (CoreML converted)
Embedding: AVA-384-Multi-INT8.AON - 113MB
Total: ~1.5GB (optimized for Neural Engine)

Speed: 45-65 tokens/sec (Neural Engine)
Latency: 100-200ms first token
Quality: Good multilingual
```

#### Flagship Configuration (CoreML)
```
LLM: AVA-QW3-4B16 (Qwen3 4B) - 2.1GB (CoreML converted)
Embedding: AVA-768-Multi-INT8.AON - 266MB
Total: ~3.4GB (fits in 8GB)

Speed: 25-40 tokens/sec (Neural Engine)
Latency: 150-300ms first token
Quality: Flagship multilingual
```

**RECOMMENDED FOR IPHONE:** Qwen3-4B + AVA-768-Multi (CoreML) for best Apple experience.
*Conversion: Use coremltools to convert MLC models to CoreML format.*

---

## Best Combinations Summary

### AR Glasses & Industrial Wearables

| Device | LLM | Embedding | Total Size | Est. Speed |
|--------|-----|-----------|------------|------------|
| **HMT-1 English** | Llama-3.2-1B | AVA-384-Base | ~1.1GB | 8-12 tok/s |
| **HMT-1 Multi** | Qwen3-0.6B | AVA-384-Multi | ~750MB | 15-20 tok/s |
| **Arc 3 English** | Llama-3.2-3B | AVA-384-Base | ~2.6GB | 8-12 tok/s |
| **Arc 3 Multi** | Qwen3-1.7B | AVA-384-Multi | ~1.6GB | 12-18 tok/s |
| **Vuzix Multi** | Gemma-2-2B | AVA-768-Multi | ~2.4GB | 10-14 tok/s |
| **Rokid Multi** | Qwen3-1.7B | AVA-384-Multi | ~1.5GB | 8-12 tok/s |

### Flagship Smartphones

| Device | LLM | Embedding | Total Size | Est. Speed | Latency |
|--------|-----|-----------|------------|------------|---------|
| **Samsung Fast** | Qwen3-1.7B | AVA-384-Multi | ~1.5GB | 35-50 tok/s | 150-250ms |
| **Samsung Best** | Qwen3-4B | AVA-768-Multi | ~3.4GB | 18-28 tok/s | 200-400ms |
| **Motorola Fast** | Llama-3.2-3B | AVA-384-Multi | ~2.5GB | 22-35 tok/s | 180-300ms |
| **Motorola Best** | Qwen3-4B | AVA-768-Multi | ~3.4GB | 15-25 tok/s | 250-400ms |
| **iPhone Fast** | Qwen3-1.7B (CoreML) | AVA-384-Multi | ~1.5GB | 45-65 tok/s | 100-200ms |
| **iPhone Best** | Qwen3-4B (CoreML) | AVA-768-Multi | ~3.4GB | 25-40 tok/s | 150-300ms |

---

## Model Naming Convention

AVA uses the following naming format:

```
AVA-{MODEL}-{SIZE}{QUANT}.{TYPE}

MODEL: QW3 (Qwen3), LL32 (Llama 3.2), GE2 (Gemma 2), GE3 (Gemma 3)
SIZE: 06B (0.6B), 1B, 17B (1.7B), 2B, 3B, 4B
QUANT: 16 (q4f16), 32 (q4f32)
TYPE: .ALM (LLM archive), .AON (ONNX embedding)
```

Examples:
- `AVA-QW3-06B16/` - Qwen3 0.6B q4f16 LLM folder
- `AVA-384-Multi-INT8.AON` - 384-dim multilingual embedding

---

## File Locations (Verified)

```
ava-ai-models-external/
├── llm/
│   ├── AVA-QW3-06B16/    # Qwen3 0.6B (335 MB) ✅ HMT-1 Multilingual
│   ├── AVA-LL32-1B16/    # Llama 3.2 1B (672 MB) ✅ HMT-1 English
│   ├── AVA-QW3-17B16/    # Qwen3 1.7B (939 MB) ✅ Arc 3 Multilingual
│   ├── AVA-GE2-2B16/     # Gemma 2 2B (1.4 GB)
│   ├── AVA-LL32-3B16/    # Llama 3.2 3B (1.7 GB) ✅ Arc 3 English
│   ├── AVA-GE3-4B16/     # Gemma 3 4B (2.1 GB)
│   └── AVA-QW3-4B16/     # Qwen3 4B (2.1 GB)
├── embeddings/
│   ├── AVA-384-Base-INT8.AON      # English (22 MB) ✅
│   ├── AVA-384-Multi-INT8.AON     # Multilingual 384 (113 MB) ✅
│   └── AVA-768-Multi-INT8.AON     # Multilingual 768 (266 MB)
└── wakeword/
    └── [15 wake word models + params]
```

---

## Sources

### Device Specifications
- [RealWear HMT-1 Specs](https://support.realwear.com/knowledge/hmt-1-specifications-for-model-t1200g)
- [RealWear Arc 3 Overview](https://support.realwear.com/knowledge/arc3/device-overview)
- [Vuzix M400 Specs](https://www.vuzix.com/products/m400-smart-glasses)
- [Rokid X-Craft Specs](https://www.rokid.com/en/products/xcraft)
- [Samsung Galaxy S24 Specs](https://www.samsung.com/us/smartphones/galaxy-s24/)
- [Motorola Razr+ 2024 Specs](https://www.motorola.com/us/smartphones-razr-plus-gen-3/p)
- [Apple iPhone 16 Pro](https://www.apple.com/iphone-16-pro/)

### ML Models & Frameworks
- [MLC-AI HuggingFace](https://huggingface.co/mlc-ai)
- [MLC-LLM GitHub](https://github.com/mlc-ai/mlc-llm)
- [Qwen3 Official Blog](https://qwenlm.github.io/blog/qwen3/)
- [Llama 3.2 HuggingFace](https://huggingface.co/blog/llama32)
- [Apple CoreML Tools](https://github.com/apple/coremltools)
