# AVA Model Files Required for NLU and LLM

**Version:** 1.0
**Last Updated:** 2025-11-24
**Status:** Active

---

## Overview

This document specifies the exact files and directory structure required on Android devices for AVA's NLU (Natural Language Understanding) and LLM (Large Language Model) features to work.

---

## Quick Reference

| Feature | Files Required | Total Size | Location |
|---------|---------------|------------|----------|
| **NLU** | 1 ONNX model | 22MB | `/sdcard/ava-ai-models/embeddings/` |
| **LLM** | Model directory with 4-75+ files | 2.1GB - 4GB+ | `/sdcard/ava-ai-models/llm/` |

---

## Device Storage Structure

### Complete Directory Layout

```
/sdcard/ava-ai-models/
├── embeddings/
│   └── AVA-384-Base-INT8.AON          # NLU embedding model (22MB)
└── llm/
    ├── AVA-GE2-2B16/                   # Gemma 2 2B (optional)
    │   ├── AVA-GE2-2B16.ADco          # Model device code (2.8MB)
    │   ├── AVALibrary.ADco            # TVM runtime library (1.0MB)
    │   ├── ava-model-config.json      # Model configuration
    │   ├── ndarray-cache.json         # Weight mapping
    │   ├── tokenizer.model            # SentencePiece tokenizer
    │   ├── tokenizer.json             # Tokenizer vocabulary
    │   ├── tokenizer_config.json      # Tokenizer settings
    │   ├── added_tokens.json          # Special tokens
    │   └── params_shard_*.bin         # Weight files (50-70 files, ~1.2GB)
    │
    └── AVA-GE3-4B16/                   # Gemma 3 4B (recommended)
        ├── AVA-GE3-4B16.ADco          # Model device code (4.6MB)
        ├── AVALibrary.ADco            # TVM runtime library (1.6MB)
        ├── ava-model-config.json      # Model configuration
        ├── ndarray-cache.json         # Weight mapping
        ├── tensor-cache.json          # Tensor metadata
        ├── tokenizer.model            # SentencePiece tokenizer
        ├── tokenizer.json             # Tokenizer vocabulary
        ├── tokenizer_config.json      # Tokenizer settings
        ├── added_tokens.json          # Special tokens
        └── params_shard_*.bin         # Weight files (69 files, ~2.1GB)
```

---

## NLU Files (Required)

### Embedding Model

**Purpose:** Intent classification, semantic search, RAG embeddings

| File | Extension | Size | Required |
|------|-----------|------|----------|
| `AVA-384-Base-INT8.AON` | `.AON` | 22MB | ✅ YES |

**Location on Device:**
```
/sdcard/ava-ai-models/embeddings/AVA-384-Base-INT8.AON
```

**Source Location (Development):**
```
/Volumes/M-Drive/Coding/AVA/ava-ai-models/embeddings/AVA-384-Base-INT8.AON
```

**Push Command:**
```bash
adb push /Volumes/M-Drive/Coding/AVA/ava-ai-models/embeddings/AVA-384-Base-INT8.AON \
         /sdcard/ava-ai-models/embeddings/
```

**Technical Details:**
- **Base Model:** MobileBERT (Google)
- **Dimensions:** 384
- **Quantization:** INT8
- **Format:** ONNX (binary identical to `.onnx`)
- **Used By:**
  - NLU module for intent classification
  - RAG module for document embeddings
  - Semantic search

---

## LLM Files (Choose One Model)

### Option 1: AVA-GE2-2B16 (Gemma 2 2B) - Compact

**Purpose:** Lightweight conversational AI, faster inference

#### Required Files (Minimum 4 files)

| File | Extension | Size | Purpose |
|------|-----------|------|---------|
| `AVA-GE2-2B16.ADco` | `.ADco` | 2.8MB | Compiled model code |
| `AVALibrary.ADco` | `.ADco` | 1.0MB | TVM runtime library |
| `ava-model-config.json` | `.json` | ~2KB | Model hyperparameters |
| `tokenizer.model` | `.model` | ~500KB | SentencePiece tokenizer |

#### Optional But Recommended Files

| File | Extension | Size | Purpose |
|------|-----------|------|---------|
| `tokenizer.json` | `.json` | ~2MB | Tokenizer vocabulary |
| `tokenizer_config.json` | `.json` | ~1KB | Tokenizer configuration |
| `added_tokens.json` | `.json` | ~100B | Special tokens |
| `ndarray-cache.json` | `.json` | ~50KB | Weight shard mapping |

#### Weight Files (50-70 files, ~1.2GB total)

| Pattern | Extension | Count | Total Size |
|---------|-----------|-------|------------|
| `params_shard_0.bin` to `params_shard_N.bin` | `.bin` | 50-70 | ~1.2GB |

**Location on Device:**
```
/sdcard/ava-ai-models/llm/AVA-GE2-2B16/
```

**Source Location (Development):**
```
/Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/AVA-GE2-2B16/
```

**Push Command:**
```bash
adb push /Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/AVA-GE2-2B16 \
         /sdcard/ava-ai-models/llm/
```

---

### Option 2: AVA-GE3-4B16 (Gemma 3 4B) - Recommended

**Purpose:** High-quality conversational AI, 140+ languages, better reasoning

#### Required Files (Minimum 4 files)

| File | Extension | Size | Purpose |
|------|-----------|------|---------|
| `AVA-GE3-4B16.ADco` | `.ADco` | 4.6MB | Compiled model code |
| `AVALibrary.ADco` | `.ADco` | 1.6MB | TVM runtime library |
| `ava-model-config.json` | `.json` | ~2KB | Model hyperparameters |
| `tokenizer.model` | `.model` | ~4MB | SentencePiece tokenizer |

#### Optional But Recommended Files

| File | Extension | Size | Purpose |
|------|-----------|------|---------|
| `tokenizer.json` | `.json` | ~17MB | Tokenizer vocabulary (large) |
| `tokenizer_config.json` | `.json` | ~1KB | Tokenizer configuration |
| `added_tokens.json` | `.json` | ~35B | Special tokens |
| `ndarray-cache.json` | `.json` | ~250KB | Weight shard mapping |
| `tensor-cache.json` | `.json` | ~1KB | Tensor metadata |

#### Weight Files (69 files, ~2.1GB total)

| Pattern | Extension | Count | Total Size |
|---------|-----------|-------|------------|
| `params_shard_0.bin` to `params_shard_68.bin` | `.bin` | 69 | ~2.1GB |

**Weight File Sizes (approximate):**
- `params_shard_0.bin`: 320MB (largest)
- `params_shard_1.bin`: 40MB
- `params_shard_2.bin` - `params_shard_68.bin`: 25-32MB each

**Location on Device:**
```
/sdcard/ava-ai-models/llm/AVA-GE3-4B16/
```

**Source Location (Development):**
```
/Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/AVA-GE3-4B16/
```

**Push Command:**
```bash
adb push /Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/AVA-GE3-4B16 \
         /sdcard/ava-ai-models/llm/
```

---

## File Extension Reference

### AVA Custom Extensions

| Extension | Full Name | Format | Description |
|-----------|-----------|--------|-------------|
| `.AON` | AVA ONNX Naming | ONNX | Binary-identical to `.onnx` |
| `.ADco` | AVA Device Code | Object Code | Compiled TVM library (`.o` file) |
| `.ALM` | AVA LLM Model | TAR Archive | Complete model package |

### Standard Extensions

| Extension | Format | Description |
|-----------|--------|-------------|
| `.json` | JSON | Configuration and metadata |
| `.model` | SentencePiece | Tokenizer model file |
| `.bin` | Binary | Model weight tensors |

---

## Installation Methods

### Method 1: Push Entire Repository (Recommended)

Push everything at once:

```bash
cd /Volumes/M-Drive/Coding/AVA/
adb push ava-ai-models /sdcard/
```

**Verify:**
```bash
adb shell ls -lh /sdcard/ava-ai-models/
adb shell ls -lh /sdcard/ava-ai-models/embeddings/
adb shell ls -lh /sdcard/ava-ai-models/llm/
```

---

### Method 2: Push Individual Components

#### Step 1: Push NLU Embedding
```bash
adb push ava-ai-models/embeddings/AVA-384-Base-INT8.AON \
         /sdcard/ava-ai-models/embeddings/
```

#### Step 2: Push LLM Model (Choose One)

**Option A: Gemma 2 2B**
```bash
adb push ava-ai-models/llm/AVA-GE2-2B16 \
         /sdcard/ava-ai-models/llm/
```

**Option B: Gemma 3 4B** (Recommended)
```bash
adb push ava-ai-models/llm/AVA-GE3-4B16 \
         /sdcard/ava-ai-models/llm/
```

---

### Method 3: Using .ALM Archives

If you have `.ALM` archive files:

#### Step 1: Extract Archive
```bash
# On development machine
cd /Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/
mkdir -p AVA-GE3-4B16-extracted
tar -xf AVA-GE3-4B16.ALM -C AVA-GE3-4B16-extracted/
```

#### Step 2: Push Extracted Directory
```bash
adb push AVA-GE3-4B16-extracted /sdcard/ava-ai-models/llm/AVA-GE3-4B16
```

**Or extract directly on device:**
```bash
# Push archive
adb push AVA-GE3-4B16.ALM /sdcard/

# Extract on device
adb shell tar -xf /sdcard/AVA-GE3-4B16.ALM -C /sdcard/ava-ai-models/llm/
```

---

## Storage Requirements

### Minimum Requirements (NLU Only)

| Component | Size |
|-----------|------|
| NLU Embedding | 22MB |
| **Total** | **22MB** |

### Recommended Configuration (NLU + Gemma 3)

| Component | Size |
|-----------|------|
| NLU Embedding | 22MB |
| Gemma 3 4B (code) | 6.2MB |
| Gemma 3 4B (weights) | 2.1GB |
| **Total** | **~2.13GB** |

### Full Configuration (NLU + Both LLMs)

| Component | Size |
|-----------|------|
| NLU Embedding | 22MB |
| Gemma 2 2B (complete) | ~1.2GB |
| Gemma 3 4B (complete) | ~2.1GB |
| **Total** | **~3.32GB** |

**Recommended Free Space:** 5GB (for overhead and future models)

---

## File Validation

### Verify NLU Files

```bash
# Check if embedding exists
adb shell ls -lh /sdcard/ava-ai-models/embeddings/AVA-384-Base-INT8.AON

# Expected output:
# -rw-rw---- 1 root sdcard_rw 22M 2025-11-24 AVA-384-Base-INT8.AON
```

### Verify LLM Files (Gemma 3 Example)

```bash
# Check directory exists
adb shell ls -lh /sdcard/ava-ai-models/llm/AVA-GE3-4B16/

# Count weight files (should be 69)
adb shell "ls /sdcard/ava-ai-models/llm/AVA-GE3-4B16/params_shard_*.bin | wc -l"

# Check critical files
adb shell ls -lh /sdcard/ava-ai-models/llm/AVA-GE3-4B16/AVA-GE3-4B16.ADco
adb shell ls -lh /sdcard/ava-ai-models/llm/AVA-GE3-4B16/AVALibrary.ADco
adb shell ls -lh /sdcard/ava-ai-models/llm/AVA-GE3-4B16/tokenizer.model
```

### Check Total Size

```bash
# Check embeddings size
adb shell du -sh /sdcard/ava-ai-models/embeddings/

# Check LLM size
adb shell du -sh /sdcard/ava-ai-models/llm/AVA-GE3-4B16/

# Check total size
adb shell du -sh /sdcard/ava-ai-models/
```

---

## Minimum Working Configuration

### For NLU Only (Intent Classification, RAG)

**Required Files (1 file):**
```
/sdcard/ava-ai-models/
└── embeddings/
    └── AVA-384-Base-INT8.AON          # 22MB
```

**Size:** 22MB
**Features:** NLU intent classification, RAG embeddings, semantic search

---

### For NLU + LLM (Full Conversational AI)

**Required Files (74+ files):**
```
/sdcard/ava-ai-models/
├── embeddings/
│   └── AVA-384-Base-INT8.AON          # 22MB
└── llm/
    └── AVA-GE3-4B16/
        ├── AVA-GE3-4B16.ADco          # 4.6MB ✅ REQUIRED
        ├── AVALibrary.ADco            # 1.6MB ✅ REQUIRED
        ├── ava-model-config.json      # 2KB ✅ REQUIRED
        ├── tokenizer.model            # 4MB ✅ REQUIRED
        ├── tokenizer.json             # 17MB (optional)
        ├── tokenizer_config.json      # 1KB (optional)
        ├── added_tokens.json          # 35B (optional)
        ├── ndarray-cache.json         # 250KB (optional)
        ├── tensor-cache.json          # 1KB (optional)
        └── params_shard_*.bin         # 69 files, 2.1GB ✅ REQUIRED
```

**Size:** ~2.13GB
**Features:** Full NLU + conversational AI with 140+ languages

---

## Troubleshooting

### NLU Not Working

**Symptom:** Intent classification fails, RAG not working

**Check:**
```bash
adb shell ls /sdcard/ava-ai-models/embeddings/AVA-384-Base-INT8.AON
```

**Fix:**
```bash
adb push ava-ai-models/embeddings/AVA-384-Base-INT8.AON \
         /sdcard/ava-ai-models/embeddings/
```

---

### LLM Not Working

**Symptom:** Model loading fails, inference errors

**Check Critical Files:**
```bash
# Check device code
adb shell ls /sdcard/ava-ai-models/llm/AVA-GE3-4B16/AVA-GE3-4B16.ADco

# Check library
adb shell ls /sdcard/ava-ai-models/llm/AVA-GE3-4B16/AVALibrary.ADco

# Check tokenizer
adb shell ls /sdcard/ava-ai-models/llm/AVA-GE3-4B16/tokenizer.model

# Check config
adb shell ls /sdcard/ava-ai-models/llm/AVA-GE3-4B16/ava-model-config.json

# Count weight files (should be 69)
adb shell "ls /sdcard/ava-ai-models/llm/AVA-GE3-4B16/params_shard_*.bin | wc -l"
```

**Fix:**
```bash
# Re-push entire model directory
adb push ava-ai-models/llm/AVA-GE3-4B16 /sdcard/ava-ai-models/llm/
```

---

### Insufficient Storage

**Symptom:** Push fails with "No space left on device"

**Check Available Space:**
```bash
adb shell df -h /sdcard
```

**Solution:**
- Free up space on device (delete photos, videos, apps)
- Use smaller model (Gemma 2 instead of Gemma 3)
- Use external SD card if available

---

## File Naming Rules

### DO Use These Extensions

| File Type | Correct Extension | Example |
|-----------|------------------|---------|
| Embedding models | `.AON` | `AVA-384-Base-INT8.AON` |
| Compiled device code | `.ADco` | `AVA-GE3-4B16.ADco` |
| Model archives | `.ALM` | `AVA-GE3-4B16.ALM` |
| Configuration | `.json` | `ava-model-config.json` |
| Tokenizer | `.model` | `tokenizer.model` |
| Weights | `.bin` | `params_shard_0.bin` |

### DON'T Use These Extensions

| ❌ Incorrect | ✅ Correct | Notes |
|-------------|-----------|-------|
| `.onnx` | `.AON` | Old naming convention |
| `.o` | `.ADco` | Generic object file |
| `.tar` | `.ALM` | Generic archive |

---

## Related Documentation

- [AVA AI Models README](/Volumes/M-Drive/Coding/AVA/ava-ai-models/README.md)
- [AVA File Format Standards](AVA-FILE-FORMATS.md)
- [Developer Manual Chapter 44: AVA Naming Convention](Developer-Manual-Chapter44-AVA-Naming-Convention.md)
- [Developer Manual Chapter 42: LLM Model Setup](Developer-Manual-Chapter42-LLM-Model-Setup.md)

---

## Quick Command Reference

### Push Everything
```bash
cd /Volumes/M-Drive/Coding/AVA/
adb push ava-ai-models /sdcard/
```

### Push NLU Only
```bash
adb push ava-ai-models/embeddings/AVA-384-Base-INT8.AON /sdcard/ava-ai-models/embeddings/
```

### Push Gemma 3 Only
```bash
adb push ava-ai-models/llm/AVA-GE3-4B16 /sdcard/ava-ai-models/llm/
```

### Verify Installation
```bash
adb shell ls -lh /sdcard/ava-ai-models/embeddings/
adb shell ls -lh /sdcard/ava-ai-models/llm/AVA-GE3-4B16/
adb shell du -sh /sdcard/ava-ai-models/
```

---

**Document Version:** 1.0
**Last Updated:** 2025-11-24
**Author:** AVA Development Team
