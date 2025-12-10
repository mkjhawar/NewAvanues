# AVA Model Download Sources

**Project:** ava
**Version:** 1.0
**Last Updated:** 2025-11-06

Complete guide to downloading all required AI models for AVA's RAG and LLM features.

---

## Table of Contents

1. [ONNX Embedding Models](#onnx-embedding-models)
   - [English-Only Models](#english-only-models)
   - [Multilingual Models](#multilingual-models)
   - [Language-Specific Models](#language-specific-models)
2. [Model Quantization](#model-quantization-reduce-file-size-by-50-75)
3. [MLC-LLM Models](#mlc-llm-models)
4. [Alternative LLM Models (GGUF)](#alternative-llm-models-gguf)
5. [Quick Download Scripts](#quick-download-scripts)
6. [Verification](#verification)

---

## ONNX Embedding Models

These models generate embeddings for RAG (document search) functionality.

---

### English-Only Models

For English-only content, these models offer the best performance.

#### Recommended: all-MiniLM-L6-v2 (86 MB, 384 dimensions)

**Best balance of speed and quality**

**Download via curl:**
```bash
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx
```

**Download via browser:**
- Direct link: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx
- Model page: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2

**Rename to:** `AVA-ONX-384-BASE.onnx`

---

#### Alternative 1: paraphrase-MiniLM-L3-v2 (61 MB, 384 dimensions)

**Faster, smaller, slightly lower quality**

**Download via curl:**
```bash
curl -L https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-FAST.onnx
```

**Download via browser:**
- Direct link: https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx
- Model page: https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2

**Rename to:** `AVA-ONX-384-FAST.onnx`

---

#### Alternative 2: all-mpnet-base-v2 (420 MB, 768 dimensions)

**Higher quality, larger, slower**

**Download via curl:**
```bash
curl -L https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-768-QUAL.onnx
```

**Download via browser:**
- Direct link: https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx
- Model page: https://huggingface.co/sentence-transformers/all-mpnet-base-v2

**Rename to:** `AVA-ONX-768-QUAL.onnx`

---

### Multilingual Models

For content in multiple languages or non-English languages.

#### Recommended: paraphrase-multilingual-MiniLM-L12-v2 (470 MB, 384 dimensions)

**Best multilingual model for most use cases**

**Supported Languages:** 50+ languages including:
- Arabic, Chinese, Dutch, English, French, German, Italian, Japanese
- Korean, Polish, Portuguese, Russian, Spanish, Turkish
- And 35+ more

**Download via curl:**
```bash
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-MULTI.onnx
```

**Download via browser:**
- Direct link: https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx
- Model page: https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2

**Rename to:** `AVA-ONX-384-MULTI.onnx`

**Use case:** General multilingual document search, cross-lingual retrieval

---

#### High-Quality: paraphrase-multilingual-mpnet-base-v2 (1.1 GB, 768 dimensions)

**Best quality for multilingual content**

**Supported Languages:** 50+ languages (same as above)

**Download via curl:**
```bash
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-mpnet-base-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-768-MULTI.onnx
```

**Download via browser:**
- Direct link: https://huggingface.co/sentence-transformers/paraphrase-multilingual-mpnet-base-v2/resolve/main/onnx/model.onnx
- Model page: https://huggingface.co/sentence-transformers/paraphrase-multilingual-mpnet-base-v2

**Rename to:** `AVA-ONX-768-MULTI.onnx`

**Use case:** High-accuracy multilingual search, research applications

**Note:** Requires dimension change in code to 768

---

#### Compact: distiluse-base-multilingual-cased-v2 (540 MB, 512 dimensions)

**Good quality, smaller language set**

**Supported Languages:** 15 major languages:
- Arabic, Chinese, Dutch, English, French, German, Italian
- Japanese, Korean, Polish, Portuguese, Russian, Spanish, Turkish, Finnish

**Download via curl:**
```bash
curl -L https://huggingface.co/sentence-transformers/distiluse-base-multilingual-cased-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-512-MULTI.onnx
```

**Download via browser:**
- Direct link: https://huggingface.co/sentence-transformers/distiluse-base-multilingual-cased-v2/resolve/main/onnx/model.onnx
- Model page: https://huggingface.co/sentence-transformers/distiluse-base-multilingual-cased-v2

**Rename to:** `AVA-ONX-512-MULTI.onnx`

**Use case:** Common languages only, moderate device resources

**Note:** Requires dimension change in code to 512

---

### Language-Specific Models

For applications focused on a single non-English language.

#### Chinese: DMetaSoul/sbert-chinese-general-v2 (400 MB, 384 dimensions)

**Optimized for Chinese text**

**Download via curl:**
```bash
curl -L https://huggingface.co/DMetaSoul/sbert-chinese-general-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-ZH.onnx
```

**Download via browser:**
- Model page: https://huggingface.co/DMetaSoul/sbert-chinese-general-v2

**Rename to:** `AVA-ONX-384-ZH.onnx`

**Use case:** Chinese-only document collections

---

#### Japanese: sonoisa/sentence-bert-base-ja-mean-tokens-v2 (~450 MB, 768 dimensions)

**Optimized for Japanese text**

**Download via curl:**
```bash
curl -L https://huggingface.co/sonoisa/sentence-bert-base-ja-mean-tokens-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-768-JA.onnx
```

**Download via browser:**
- Model page: https://huggingface.co/sonoisa/sentence-bert-base-ja-mean-tokens-v2

**Rename to:** `AVA-ONX-768-JA.onnx`

**Use case:** Japanese-only document collections

**Note:** Requires dimension change in code to 768

---

#### Arabic/Multi-language: Use paraphrase-multilingual-MiniLM-L12-v2

For Arabic, Spanish, Portuguese, and other major languages, the multilingual models provide excellent performance.

---

### Finding More ONNX Models

Browse all sentence-transformer models with ONNX exports:
- https://huggingface.co/sentence-transformers

**How to check if a model has ONNX:**
1. Go to model page on HuggingFace
2. Click "Files and versions" tab
3. Look for `onnx/` folder
4. Download `onnx/model.onnx`

---

## Model Quantization (Reduce File Size by 50-75%)

**Want to reduce model file sizes?** Use quantization to compress models with minimal quality loss.

### What is Quantization?

Quantization reduces model file sizes by using lower-precision numbers (8-bit or 16-bit instead of 32-bit floats).

**Benefits:**
- ✅ 50-75% smaller file size
- ✅ Faster inference
- ✅ Lower memory usage
- ✅ Only 1-5% quality loss

### Quick Quantization

```bash
# Install dependencies
pip install onnxruntime

# Download a model (example: multilingual model)
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-MULTI.onnx

# Quantize it (470 MB → 117 MB)
python3 scripts/quantize-models.py \
  AVA-ONX-384-MULTI.onnx \
  AVA-ONX-384-MULTI-INT8.onnx \
  int8

# Push quantized model to device
adb push AVA-ONX-384-MULTI-INT8.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
```

### Quantization Modes

**INT8 (Recommended) - 75% reduction:**
```bash
python3 scripts/quantize-models.py input.onnx output-int8.onnx int8
```
- Quality loss: ~3-5%
- Best for: General use, maximum size savings

**FP16 - 50% reduction:**
```bash
python3 scripts/quantize-models.py input.onnx output-fp16.onnx fp16
```
- Quality loss: ~1-2%
- Best for: When higher quality is needed

### Size Reduction Examples

| Model | Original | INT8 (75%) | FP16 (50%) |
|-------|----------|------------|------------|
| AVA-ONX-384-BASE | 86 MB | 22 MB | 43 MB |
| AVA-ONX-384-MULTI | 470 MB | 117 MB | 235 MB |
| AVA-ONX-768-QUAL | 420 MB | 105 MB | 210 MB |
| AVA-ONX-768-MULTI | 1.1 GB | 275 MB | 550 MB |

### Quantized Model Downloads

All models above can be quantized using the script. See **`docs/MODEL-QUANTIZATION-GUIDE.md`** for complete instructions.

**Naming convention for quantized models:**
- INT8: `AVA-ONX-384-MULTI-INT8.onnx`
- FP16: `AVA-ONX-384-MULTI-FP16.onnx`

---

## MLC-LLM Models

These are pre-compiled models optimized for mobile/edge deployment.

### Official MLC-LLM Binary Releases

**Main Repository:**
- https://github.com/mlc-ai/mlc-llm
- https://github.com/mlc-ai/binary-mlc-llm-libs

### Recommended: Gemma-2B-IT (2 GB, 2 billion parameters)

**Best for AVA - default model**

**Download via Git LFS:**
```bash
# Install git-lfs first
brew install git-lfs  # macOS
# or
apt install git-lfs   # Linux

# Clone the binary libs repo
git clone https://github.com/mlc-ai/binary-mlc-llm-libs.git
cd binary-mlc-llm-libs

# Navigate to Gemma model
cd gemma-2b-it/

# Copy and rename
cp gemma-2b-it-q4f16_1-android.tar ~/AVA-GEM-2B-Q4.tar
```

**Manual Download:**
1. Go to: https://github.com/mlc-ai/binary-mlc-llm-libs
2. Navigate to `gemma-2b-it/` folder
3. Download `gemma-2b-it-q4f16_1-android.tar`
4. Rename to: `AVA-GEM-2B-Q4.tar`

**Size:** ~2 GB
**Quantization:** Q4F16_1 (4-bit weights, 16-bit activations)
**Platform:** Android

---

### Alternative 1: Phi-2 (~1.5 GB, 2.7 billion parameters)

**Smaller alternative to Gemma**

**From binary-mlc-llm-libs:**
```bash
cd binary-mlc-llm-libs/phi-2/
cp phi-2-q4f16_1-android.tar ~/AVA-PHI-3B-Q4.tar
```

**Or download manually:**
- https://github.com/mlc-ai/binary-mlc-llm-libs/tree/main/phi-2

**Rename to:** `AVA-PHI-3B-Q4.tar`

---

### Alternative 2: Mistral-7B (~4 GB, 7 billion parameters)

**Higher quality, requires more RAM**

**From binary-mlc-llm-libs:**
```bash
cd binary-mlc-llm-libs/Mistral-7B-Instruct-v0.2/
cp mistral-7b-instruct-v0.2-q4f16_1-android.tar ~/AVA-MST-7B-Q4.tar
```

**Or download manually:**
- https://github.com/mlc-ai/binary-mlc-llm-libs/tree/main/Mistral-7B-Instruct-v0.2

**Rename to:** `AVA-MST-7B-Q4.tar`

---

### Alternative 3: TinyLlama (600 MB, 1.1 billion parameters)

**Smallest option for low-end devices**

**From binary-mlc-llm-libs:**
```bash
cd binary-mlc-llm-libs/TinyLlama-1.1B-Chat-v1.0/
cp tinyllama-1.1b-chat-v1.0-q4f16_1-android.tar ~/AVA-TNY-1B-Q4.tar
```

**Rename to:** `AVA-TNY-1B-Q4.tar`

---

### Alternative 4: Llama-2-7B (~4 GB, 7 billion parameters)

**Meta's Llama 2**

**From binary-mlc-llm-libs:**
```bash
cd binary-mlc-llm-libs/Llama-2-7b-chat-hf/
cp llama-2-7b-chat-q4f16_1-android.tar ~/AVA-LLM-7B-Q4.tar
```

**Rename to:** `AVA-LLM-7B-Q4.tar`

---

### Compiling Your Own MLC-LLM Models

If you want to compile custom models:

**Documentation:**
- https://llm.mlc.ai/docs/compilation/compile_models.html

**Quick guide:**
```bash
# Install MLC-LLM
pip install mlc-llm

# Compile a model for Android
mlc_llm compile <MODEL_NAME> \
  --target android \
  --quantization q4f16_1 \
  --output ./dist/

# Example: Compile Gemma-2B
mlc_llm compile google/gemma-2b-it \
  --target android \
  --quantization q4f16_1 \
  --output ./gemma-2b-android/
```

---

## Alternative LLM Models (GGUF)

GGUF models are alternative format (primarily for llama.cpp ecosystem). AVA's MLCLLMProvider may require additional configuration for GGUF support.

### TinyLlama (600 MB, 1.1 billion parameters)

**Download via curl:**
```bash
curl -L https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  -o AVA-TNY-1B-Q4.gguf
```

**Download via browser:**
- https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF

**Rename to:** `AVA-TNY-1B-Q4.gguf`

---

### Phi-3-Mini (~2 GB, 3.8 billion parameters)

**Download via curl:**
```bash
curl -L https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf \
  -o AVA-PHI-3B-Q4.gguf
```

**Download via browser:**
- https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf

**Rename to:** `AVA-PHI-3B-Q4.gguf`

---

### Mistral-7B (~4 GB, 7 billion parameters)

**Download via curl:**
```bash
curl -L https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf \
  -o AVA-MST-7B-Q4.gguf
```

**Download via browser:**
- https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF

**Rename to:** `AVA-MST-7B-Q4.gguf`

---

### Finding More GGUF Models

**TheBloke's quantized models:**
- https://huggingface.co/TheBloke

Search for models ending in `-GGUF`. TheBloke provides quantized versions of most popular models.

**Quantization levels:**
- `Q4_K_M` - 4-bit, medium quality (recommended)
- `Q5_K_M` - 5-bit, higher quality
- `Q8_0` - 8-bit, highest quality (larger size)

---

## Quick Download Scripts

### Recommended Setup (Embedding + LLM)

```bash
#!/bin/bash
# Download recommended models for AVA

echo "Downloading ONNX embedding model (86 MB)..."
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx

echo "Done! Now download Gemma-2B from MLC-LLM binary libs repo"
echo "Visit: https://github.com/mlc-ai/binary-mlc-llm-libs"
echo "Or use existing local copy if you have it"
```

---

### Low-Memory Setup (Embedding + Small LLM)

```bash
#!/bin/bash
# Download low-memory models for AVA

echo "Downloading fast ONNX embedding model (61 MB)..."
curl -L https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-FAST.onnx

echo "Downloading TinyLlama GGUF (600 MB)..."
curl -L https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  -o AVA-TNY-1B-Q4.gguf

echo "Done! Total size: ~661 MB"
```

---

### High-Quality Setup (Embedding + Large LLM)

```bash
#!/bin/bash
# Download high-quality models for AVA

echo "Downloading high-quality ONNX embedding model (420 MB)..."
curl -L https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-768-QUAL.onnx

echo "Downloading Mistral-7B GGUF (4 GB)..."
curl -L https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf \
  -o AVA-MST-7B-Q4.gguf

echo "Done! Total size: ~4.4 GB"
```

---

### Multilingual Setup (Embedding + LLM)

```bash
#!/bin/bash
# Download multilingual models for AVA

echo "Downloading multilingual ONNX embedding model (470 MB)..."
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-MULTI.onnx

echo "Multilingual embedding downloaded!"
echo "For LLM: Download Gemma-2B from MLC-LLM binary libs (supports all languages)"
echo "Visit: https://github.com/mlc-ai/binary-mlc-llm-libs"
echo ""
echo "Total size: ~2.5 GB (embedding + Gemma-2B)"
```

---

## Verification

### Check Downloaded Files

```bash
# Check ONNX embedding model
ls -lh AVA-ONX-384-BASE.onnx
# Should be: 86M (90,400,000 bytes)

file AVA-ONX-384-BASE.onnx
# Should say: "data" or "ONNX model"
# NOT: "HTML document" (means download failed)

# Check MLC-LLM model
ls -lh AVA-GEM-2B-Q4.tar
# Should be: ~2.0G (varies by exact version)

file AVA-GEM-2B-Q4.tar
# Should say: "tar archive" or "POSIX tar archive"
```

---

### Verify File Sizes

| Model | Expected Size | Type |
|-------|--------------|------|
| **English-Only Embeddings** | | |
| AVA-ONX-384-BASE.onnx | 86 MB | ONNX Embedding |
| AVA-ONX-384-FAST.onnx | 61 MB | ONNX Embedding |
| AVA-ONX-768-QUAL.onnx | 420 MB | ONNX Embedding |
| **Multilingual Embeddings** | | |
| AVA-ONX-384-MULTI.onnx | 470 MB | ONNX Embedding (50+ languages) |
| AVA-ONX-768-MULTI.onnx | 1.1 GB | ONNX Embedding (50+ languages) |
| AVA-ONX-512-MULTI.onnx | 540 MB | ONNX Embedding (15 languages) |
| AVA-ONX-384-ZH.onnx | 400 MB | ONNX Embedding (Chinese) |
| AVA-ONX-768-JA.onnx | 450 MB | ONNX Embedding (Japanese) |
| **LLM Models** | | |
| AVA-GEM-2B-Q4.tar | ~2 GB | MLC-LLM |
| AVA-PHI-3B-Q4.tar | ~1.5 GB | MLC-LLM |
| AVA-MST-7B-Q4.tar | ~4 GB | MLC-LLM |
| AVA-TNY-1B-Q4.gguf | 600 MB | GGUF LLM |

---

## Model Recommendations by Device

### Low-End Device (< 4 GB RAM)
- **Embedding:** AVA-ONX-384-FAST.onnx (61 MB)
- **LLM:** AVA-TNY-1B-Q4.gguf (600 MB)
- **Total:** ~661 MB

### Mid-Range Device (4-6 GB RAM)
- **Embedding:** AVA-ONX-384-BASE.onnx (86 MB)
- **LLM:** AVA-GEM-2B-Q4.tar (~2 GB)
- **Total:** ~2.1 GB

### High-End Device (8+ GB RAM)
- **Embedding:** AVA-ONX-768-QUAL.onnx (420 MB)
- **LLM:** AVA-MST-7B-Q4.tar (~4 GB)
- **Total:** ~4.4 GB

---

## Troubleshooting Downloads

### Issue: Download fails or stops midway

**Solution:**
```bash
# Use curl with resume support
curl -L -C - <URL> -o <filename>

# Example:
curl -L -C - https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx
```

---

### Issue: File is 1 KB or very small

**Problem:** Downloaded an HTML error page instead of the model

**Solution:**
- Check the URL is correct
- Try downloading via browser instead
- Check if you need to accept HuggingFace terms for that model

---

### Issue: MLC-LLM binary libs repo is huge

**Solution:** Use sparse checkout to download only specific models

```bash
# Clone with sparse checkout
git clone --depth=1 --filter=blob:none --sparse https://github.com/mlc-ai/binary-mlc-llm-libs.git
cd binary-mlc-llm-libs

# Checkout only gemma-2b-it
git sparse-checkout set gemma-2b-it
```

---

## Next Steps

After downloading models:

1. **Rename files** to AVA proprietary format (if not already done)
2. **Push to device** using ADB or file manager
3. **Verify** files are in correct locations
4. **Test** in AVA app

See these guides for next steps:
- `docs/AVA-MODEL-RENAME-GUIDE.md` - Renaming and pushing models
- `docs/DEVICE-MODEL-SETUP-COMPLETE.md` - Complete device setup
- `docs/AVA-MODEL-NAMING-REGISTRY.md` - Full naming reference

---

## Additional Resources

### Official Documentation
- **MLC-LLM:** https://llm.mlc.ai/
- **Sentence Transformers:** https://www.sbert.net/
- **HuggingFace Models:** https://huggingface.co/models

### Model Cards (Performance Info)
- **all-MiniLM-L6-v2:** https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2
- **Gemma-2B:** https://huggingface.co/google/gemma-2b-it
- **Phi-2:** https://huggingface.co/microsoft/phi-2
- **Mistral-7B:** https://huggingface.co/mistralai/Mistral-7B-Instruct-v0.2

### Community
- **MLC-LLM Discord:** https://discord.gg/9Xpy2HGBuD
- **HuggingFace Forums:** https://discuss.huggingface.co/

---

**Document Version:** 1.0
**Last Updated:** 2025-11-06
**Maintained by:** AVA Development Team
