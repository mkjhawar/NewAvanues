# AVA Model Naming Registry

**Purpose:** Master mapping of proprietary AVA model filenames to original model names
**Status:** Internal Reference Document
**Classification:** Proprietary
**Last Updated:** 2025-11-06

---

## Overview

AVA uses proprietary filenames for all AI models to:
- Obscure model origins from casual inspection
- Simplify filename handling (no version numbers, no special characters)
- Maintain consistent naming across all platforms
- Protect intellectual property choices

---

## Naming Convention

**Format:** `AVA-{MODEL}-{PARAMS}-{QUANT}.{ext}`

**Components:**
- `AVA` - Prefix (all models)
- `MODEL` - Model type (ONX, GEM, PHI, MST, etc.)
- `PARAMS` - Parameters/Dimensions (384, 768, 2B, 7B, etc.)
- `QUANT` - Quantization/Variant (BASE, FAST, Q4, INT8, FP16, etc.)
- `ext` - File extension (.onnx, .tar, .gguf, .bin)

**Examples:**
- `AVA-ONX-384-BASE.onnx` - all-MiniLM-L6-v2 (384 dimensions, base variant)
- `AVA-ONX-384-BASE-INT8.onnx` - all-MiniLM-L6-v2 (INT8 quantized, 75% smaller)
- `AVA-GEM-2B-Q4.tar` - Gemma 2 billion parameters, Q4 quantized
- `AVA-PHI-3B-Q4.tar` - Phi 3.8 billion parameters, Q4 quantized
- `AVA-MST-7B-Q4.tar` - Mistral 7 billion parameters, Q4 quantized

**Quantization Suffixes:**
- `INT8` - INT8 quantization (75% size reduction)
- `FP16` - FP16 quantization (50% size reduction)
- No suffix - Original FP32 precision

**Why Hyphens?**
- ✅ More readable than dots
- ✅ Easier to parse programmatically
- ✅ Industry standard convention
- ✅ Clear separation between components

---

## Model Registry

### Embedding Models (ONNX)

| AVA Filename | Original Model | Source | Dimensions | Size | Purpose |
|--------------|----------------|--------|------------|------|---------|
| **AVA-ONX-384-BASE.onnx** | all-MiniLM-L6-v2 | HuggingFace/sentence-transformers | 384 | 86 MB | RAG embeddings (default) |
| **AVA-ONX-384-FAST.onnx** | paraphrase-MiniLM-L3-v2 | HuggingFace/sentence-transformers | 384 | 61 MB | Fast embeddings |
| **AVA-ONX-768-QUAL.onnx** | all-mpnet-base-v2 | HuggingFace/sentence-transformers | 768 | 420 MB | High-quality embeddings |

**Download Links:**
```bash
# AVA-ONX-384-BASE.onnx (all-MiniLM-L6-v2, 384 dimensions)
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx

# AVA-ONX-384-FAST.onnx (paraphrase-MiniLM-L3-v2, 384 dimensions)
curl -L https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-FAST.onnx

# AVA-ONX-768-QUAL.onnx (all-mpnet-base-v2, 768 dimensions)
curl -L https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-768-QUAL.onnx
```

---

### Quantized Embedding Models (INT8/FP16)

**Quantized versions of above models - 50-75% smaller file size**

| AVA Filename | Original Model | Quantization | Dimensions | Size | Reduction |
|--------------|----------------|--------------|------------|------|-----------|
| **AVA-ONX-384-BASE-INT8.onnx** | all-MiniLM-L6-v2 | INT8 | 384 | 22 MB | 75% |
| **AVA-ONX-384-BASE-FP16.onnx** | all-MiniLM-L6-v2 | FP16 | 384 | 43 MB | 50% |
| **AVA-ONX-384-FAST-INT8.onnx** | paraphrase-MiniLM-L3-v2 | INT8 | 384 | 15 MB | 75% |
| **AVA-ONX-768-QUAL-INT8.onnx** | all-mpnet-base-v2 | INT8 | 768 | 105 MB | 75% |
| **AVA-ONX-384-MULTI-INT8.onnx** | paraphrase-multilingual-MiniLM-L12-v2 | INT8 | 384 | 117 MB | 75% |
| **AVA-ONX-384-MULTI-FP16.onnx** | paraphrase-multilingual-MiniLM-L12-v2 | FP16 | 384 | 235 MB | 50% |
| **AVA-ONX-768-MULTI-INT8.onnx** | paraphrase-multilingual-mpnet-base-v2 | INT8 | 768 | 275 MB | 75% |

**How to create quantized models:**
```bash
# Install quantization tool
pip install onnxruntime

# Download original model
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o model.onnx

# Quantize to INT8 (75% reduction, recommended)
python3 scripts/quantize-models.py model.onnx AVA-ONX-384-BASE-INT8.onnx int8

# Or quantize to FP16 (50% reduction)
python3 scripts/quantize-models.py model.onnx AVA-ONX-384-BASE-FP16.onnx fp16
```

**Quality Impact:**
- INT8: ~3-5% accuracy loss (barely noticeable in practice)
- FP16: ~1-2% accuracy loss (minimal)

**See:** `docs/MODEL-QUANTIZATION-GUIDE.md` for complete instructions

---

### LLM Models (MLC-LLM Compiled)

| AVA Filename | Original Model | Source | Size | Parameters | Purpose |
|--------------|----------------|--------|------|------------|---------|
| **AVA-GEM-2B-Q4.tar** | gemma-2b-it-q4f16_1-android | MLC-LLM Binary Libs | ~2 GB | 2B | Chat/NLU (default) |
| **AVA-GEM-2B-Q4.wasm** | gemma-2b-it-q4f16_1-ctx4k_cs1k-webgpu | MLC-LLM Binary Libs | 2.7 MB | 2B | Web version |
| **AVA-PHI-3B-Q4.tar** | phi-2-q4f16_1-android | MLC-LLM Binary Libs | ~1.5 GB | 3B | Alternative chat |
| **AVA-MST-7B-Q4.tar** | Mistral-7B-Instruct-v0.2-q4f16_1 | MLC-LLM Binary Libs | ~4 GB | 7B | High-quality chat |

**Local Source Mapping:**
```bash
# AVA-GEM-2B-Q4.tar
# Source: /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/gemma-2b-it-q4f16_1-android.tar

# AVA-PHI-3B-Q4.tar
# Source: /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/phi-2/phi-2-q4f16_1-android.tar

# AVA-MST-7B-Q4.tar
# Source: /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/Mistral-7B-Instruct-v0.2/mistral-7b-instruct-v0.2-q4f16_1-android.tar
```

---

### LLM Models (GGUF Format - Alternative)

| AVA Filename | Original Model | Source | Size | Parameters | Purpose |
|--------------|----------------|--------|------|------------|---------|
| **AVA-TNY-1B-Q4.gguf** | tinyllama-1.1b-chat-v1.0.Q4_K_M | TheBloke/HuggingFace | 600 MB | 1.1B | Fast, low-memory chat |
| **AVA-PHI-3B-Q4.gguf** | Phi-3-mini-4k-instruct-q4 | Microsoft/HuggingFace | ~2 GB | 3.8B | Balanced chat |
| **AVA-MST-7B-Q4.gguf** | mistral-7b-instruct-v0.2.Q4_K_M | TheBloke/HuggingFace | ~4 GB | 7B | High-quality chat |

**Download Links:**
```bash
# AVA-TNY-1B-Q4.gguf (TinyLlama)
curl -L https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  -o AVA-TNY-1B-Q4.gguf

# AVA-PHI-3B-Q4.gguf (Phi-3)
curl -L https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf \
  -o AVA-PHI-3B-Q4.gguf

# AVA-MST-7B-Q4.gguf (Mistral-7B)
curl -L https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf \
  -o AVA-MST-7B-Q4.gguf
```

---

## Type Codes

| Code | Meaning | Models |
|------|---------|--------|
| **ONX** | ONNX Embedding | Sentence transformers, embeddings |
| **GEM** | Gemma | Google Gemma series |
| **PHI** | Phi | Microsoft Phi series |
| **MST** | Mistral | Mistral AI series |
| **TNY** | TinyLlama | TinyLlama series |
| **LLM** | Llama | Meta Llama series |
| **GPT** | GPT | OpenAI GPT series (if used) |

---

## Variant Codes

| Code | Meaning | Details |
|------|---------|---------|
| **BASE** | Base/Default | Standard version |
| **FAST** | Fast | Optimized for speed |
| **QUAL** | Quality | Optimized for accuracy |
| **Q4** | 4-bit Quantized | Most common, good balance |
| **Q8** | 8-bit Quantized | Higher quality |
| **F16** | FP16 | Half precision |
| **F32** | FP32 | Full precision |

---

## File Locations on Device

### Android
```
/sdcard/Android/data/com.augmentalis.ava/files/models/
├── AVA-ONX-384-BASE.onnx      ← Embedding model
└── llm/
    └── AVA-GEM-2B-Q4.tar      ← LLM model
```

### iOS (Future)
```
/Documents/AVA/models/
├── AVA-ONX-384-BASE.onnx
└── llm/
    └── AVA-GEM-2B-Q4.mlpackage
```

---

## Renaming Instructions

### From Existing Downloads

**Step 1: Rename embedding model**
```bash
# Download original
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o model.onnx

# Rename to AVA format
mv model.onnx AVA-ONX-384-BASE.onnx
```

**Step 2: Rename LLM model**
```bash
# Copy from MLC-LLM downloads
cd /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/

# Rename to AVA format
cp gemma-2b-it-q4f16_1-android.tar AVA-GEM-2B-Q4.tar
```

**Step 3: Push to device**
```bash
# Push embedding model
adb push AVA-ONX-384-BASE.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# Create LLM directory and push
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
adb push AVA-GEM-2B-Q4.tar /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
```

---

## Verification

### Check renamed files
```bash
# On device
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/

# Expected output:
# AVA-ONX-384-BASE.onnx  86M
# llm/

adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Expected output:
# AVA-GEM-2B-Q4.tar  ~2.0G  (or 311K if just config)
```

---

## Configuration Mapping

### Code References

**ONNXEmbeddingProvider.android.kt:**
```kotlin
// Old: modelId = "all-MiniLM-L6-v2"
// New: modelId = "AVA-ONX-384-BASE"
```

**MLCLLMProvider.android.kt:**
```kotlin
// Old: modelPath = "gemma-2b-it-q4f16_1-android.tar"
// New: modelPath = "AVA-GEM-2B-Q4.tar"
```

---

## Default Configuration

**Recommended Setup:**
- Embedding: `AVA-ONX-384-BASE.onnx` (all-MiniLM-L6-v2, 86 MB)
- LLM: `AVA-GEM-2B-Q4.tar` (Gemma-2b-it, ~2 GB)

**Alternative Setups:**

**Low-Memory:**
- Embedding: `AVA-ONX-384-FAST.onnx` (61 MB)
- LLM: `AVA-TNY-1B-Q4.gguf` (600 MB)

**High-Quality:**
- Embedding: `AVA-ONX-768-QUAL.onnx` (420 MB)
- LLM: `AVA-MST-7B-Q4.tar` (4 GB)

---

## Security Notes

**Filename Obscurity:**
- Filenames don't reveal model architecture
- Version numbers removed
- Source unclear without this registry
- Reduces competitive intelligence exposure

**Internal Use Only:**
- This registry is for development team only
- Do NOT distribute to end users
- Do NOT commit to public repositories
- Keep synchronized with code changes

---

## Change Log

| Date | Change | Updated By |
|------|--------|------------|
| 2025-11-06 | Initial registry created | AVA AI Team |
| 2025-11-06 | Added MLC-LLM model mappings | AVA AI Team |
| 2025-11-06 | Added GGUF alternatives | AVA AI Team |

---

## Related Documentation

- `MODEL-SETUP.md` - Public user guide (uses generic names)
- `LLM-SETUP.md` - Public LLM setup guide (uses generic names)
- `DEVICE-MODEL-SETUP-COMPLETE.md` - Complete setup guide (needs update)
- `ONNXEmbeddingProvider.android.kt` - Code implementation
- `MLCLLMProvider.android.kt` - Code implementation

---

**Classification:** INTERNAL USE ONLY
**Distribution:** Development Team Only
**Last Updated:** 2025-11-06
**Version:** 1.0
