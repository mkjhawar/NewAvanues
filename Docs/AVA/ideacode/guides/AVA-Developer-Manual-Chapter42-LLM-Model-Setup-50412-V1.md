# Developer Manual - Chapter 42: LLM Model Setup Instructions

**Last Updated:** 2025-11-17
**Author:** AVA Development Team
**Status:** Production

---

## Overview

This chapter provides complete instructions for setting up LLM models for AVA. It covers everything from downloading pre-compiled models to compiling your own.

**Full Implementation Guide:** [implementation-instructions/LLM-MODEL-COMPLETE-SETUP-GUIDE.md](implementation-instructions/LLM-MODEL-COMPLETE-SETUP-GUIDE.md)

---

## Quick Reference

### What You Need for a Complete LLM

| Component | Size | Source | Required |
|-----------|------|--------|----------|
| **TVM Runtime** (.so with FFI) | ~98MB | MLC Chat APK or build | Yes |
| Compiled Library (.o files) | ~300KB | Local or GitHub | Yes |
| Model Weights (params_shard_*.bin) | ~2GB | HuggingFace | Yes |
| Tokenizer (tokenizer.model) | ~4MB | HuggingFace | Yes |
| Config Files (JSON) | ~5KB | HuggingFace | Yes |

### TVM Runtime with FFI Bindings (TVM v0.22.0)

**Critical:** The pre-compiled model libraries (.o files) are NOT the same as the TVM runtime. You need BOTH:

- **Model Libraries:** `gemma_q4f16_1_devc.o` (compiled with TVM v0.22.0)
- **TVM Runtime:** `libtvm_runtime.so` + `libtvm4j.so` (built from TVM v0.22.0)

**AVA's Custom TVM Build (Current):**
```
apps/ava-standalone/src/main/jniLibs/arm64-v8a/
├── libtvm_runtime.so    # 62MB - TVM runtime with new FFI API
└── libtvm4j.so          # 124KB - JNI bridge for org.apache.tvm
```

**Build Location:** `~/Downloads/Coding/tvm-build/build-android/`

**Why Custom Build?**
- MLC Chat APK uses old TVM C API (`TVMFuncCall`)
- AVA needs TVM v0.22.0 FFI API (`TVMFFIFunctionCall`)
- Models must be compiled with matching TVM version

**If you see errors:**
- `UnsatisfiedLinkError: tvmFFIFunctionGetGlobal` → Runtime/model API mismatch
- Models compiled with old TVM won't work with v0.22.0 runtime

See: [Gemma Model Compilation Instructions](instructions/GEMMA-MODEL-COMPILATION-INSTRUCTIONS.md)

### What You Already Have Locally

Pre-compiled TVM libraries at:
```
/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/
```

**Available models:**
- `gemma-2b-it/gemma-2b-it-q4f16_1-android.tar` (311KB) - Recommended
- `Llama-2-7b-chat-hf/Llama-2-7b-chat-hf-q4f16_1-android.tar` (348KB)
- `Mistral-7B-Instruct-v0.2/Mistral-7B-Instruct-v0.2-q4f16_1-android.tar` (349KB)
- `phi-2/phi-2-q4f16_1-android.tar` (349KB)

### What You Still Need

Weights, tokenizer, and configs from HuggingFace:
```bash
git lfs install
git clone https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC
```

---

## Device Directory Structure

AVA expects models at:

```
/sdcard/Android/data/com.augmentalis.ava/files/models/
├── AVA-384-Base.AON                         ← NLU Embedding model
└── llm/
    └── AVA-GE3-4B16/                        ← LLM folder (AVA naming)
        ├── AVALibrary.ADco                  ← Model library code
        ├── AVA-GE3-4B16.ADco                ← Device code
        ├── mlc-chat-config.json             ← Keep original name
        ├── ndarray-cache.json               ← Keep original name
        ├── tokenizer.model                  ← Keep original name
        ├── tokenizer_config.json            ← Keep original name
        ├── params_shard_0.bin               ← Keep original name
        ├── params_shard_1.bin
        └── ...
```

---

## AVA Naming Convention v2

### File Extensions

| Extension | Description | Replaces |
|-----------|-------------|----------|
| `.ADco` | AVA Device Code (compiled object) | `.o` |
| `.ALM` | AVA LLM Model (compiled model package) | `.tar` |
| `.AON` | AVA ONNX Model | `.onnx` |

### Model Naming Format: `AVA-{MODEL}{VERSION}-{SIZE}{BITS}`

**What the AVA prefix means:**
- **Retokenized and optimized** for ALC (AVA Local Compute)
- **NOT compatible** with standard MLC-LLM
- **Uses TVM v0.22.0** with new FFI API (`TVMFFIFunctionCall`, not `TVMFuncCall`)
- **Ready for on-device inference** with AVA's custom runtime

### Model Family Codes

| Model Family | Code | Example |
|-------------|------|---------|
| Gemma 2 | GE2 | `AVA-GE2-2B16` |
| Gemma 3 | GE3 | `AVA-GE3-4B16` |
| Gemma 4N | G4N | `AVA-G4N-4B16` |
| Llama 2 | LL2 | `AVA-LL2-7B16` |
| Mistral | MST | `AVA-MST-7B16` |
| Phi | PHI | `AVA-PHI-3B16` |

### Quantization Suffix
- `16` = q4f16_1 or q4bf16_1 (4-bit quantization with bf16/f16)

### File Naming Examples

| Old Name | New Name | Description |
|----------|----------|-------------|
| `gemma3_q4bf16_1_devc.o` | `AVA-GE3-4B16.ADco` | Device code |
| `lib0.o` | `AVALibrary.ADco` | Library code |
| `model-android.tar` | `AVA-GE3-4B16.ALM` | Compiled model package |
| `AVA-ONX-384-BASE.onnx` | `AVA-384-Base.AON` | ONNX embedding model |

### Inner Files (Keep original names for TVM compatibility)

These JSON/tokenizer files must keep original names:
- `mlc-chat-config.json` - Model configuration
- `tokenizer.model` - SentencePiece tokenizer
- `ndarray-cache.json` - Weight shard mapping
- `params_shard_*.bin` - Model weight files

**Why:** The `mlc-chat-config.json` contains `"model_lib"` which references internal structures.

### ALC vs MLC Compatibility

| Aspect | MLC-LLM Models | AVA/ALC Models |
|--------|----------------|----------------|
| TVM API | Old C API (TVMFuncCall) | New FFI API (TVMFFIFunctionCall) |
| TVM Version | Pre-0.20 | v0.22.0 |
| Runtime | libtvm4j_runtime_packed.so | libtvm_runtime.so + libtvm4j.so |
| Compatibility | MLC Chat App | AVA only |

---

## Setup Methods

### Method 1: Download Pre-Compiled (Recommended)

1. Clone from HuggingFace (weights + tokenizer)
2. Add compiled library from local files
3. Rename folder to AVA convention
4. Push to device

**Time:** ~30 minutes (mostly download)

### Method 2: Download Components Separately

Download each component individually via curl.

**Use when:** Partial download or specific components needed

### Method 3: Compile Your Own (Advanced)

1. Install mlc-llm
2. Download original model from HuggingFace
3. Convert weights: `mlc_llm convert_weight`
4. Compile for Android: `mlc_llm compile`
5. Assemble package

**Use when:** Custom quantization, unsupported model, hardware optimization

---

## Quick Setup Commands

```bash
# 1. Clone weights and tokenizer from HuggingFace
git lfs install
git clone https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC ~/mlc-models/AVA-GE2-2B16

# 2. Compile with ALC-LLM or extract pre-compiled .ALM
alc_llm compile \
    ~/mlc-models/AVA-GE2-2B16/mlc-chat-config.json \
    --device android \
    -o ~/mlc-models/AVA-GE2-2B16.ALM

# Extract .ALM and rename device code files
tar -xf ~/mlc-models/AVA-GE2-2B16.ALM -C ~/mlc-models/AVA-GE2-2B16/
mv ~/mlc-models/AVA-GE2-2B16/lib0.o ~/mlc-models/AVA-GE2-2B16/AVALibrary.ADco
mv ~/mlc-models/AVA-GE2-2B16/gemma_q4f16_1_devc.o ~/mlc-models/AVA-GE2-2B16/AVA-GE2-2B16.ADco

# 3. Push to device
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
adb push ~/mlc-models/AVA-GE2-2B16 /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# 4. Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GE2-2B16/
```

---

## File Checklist

Before deployment, verify all files present:

- [ ] `AVALibrary.ADco` - TVM base library (renamed from lib0.o)
- [ ] `AVA-{MODEL}-{SIZE}{BITS}.ADco` - Compiled device code
- [ ] `mlc-chat-config.json` - Model configuration
- [ ] `ndarray-cache.json` - Weight shard index
- [ ] `tokenizer.model` - SentencePiece tokenizer
- [ ] `tokenizer_config.json` - Tokenizer settings
- [ ] `params_shard_*.bin` - Model weight shards (multiple files)

---

## Common Issues

### "Model file not found"

Check directory path and file permissions:
```bash
adb shell ls -la /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GE3-4B16/
```

### "TVM module load failed"

Verify device code files exist with correct naming:
```bash
adb shell ls -la .../AVA-GE3-4B16/*.ADco
```

### "Tokenizer not found"

Verify tokenizer.model exists and is not empty:
```bash
adb shell ls -l .../AVA-GE3-4B16/tokenizer.model
```

---

## Related Documentation

- **Chapter 38:** [LLM Model Management](Developer-Manual-Chapter38-LLM-Model-Management.md) - Architecture and API
- **Full Guide:** [LLM Model Complete Setup Guide](implementation-instructions/LLM-MODEL-COMPLETE-SETUP-GUIDE.md) - Step-by-step instructions
- **Model Sources:** [Model Download Sources](MODEL-DOWNLOAD-SOURCES.md) - Where to get models
- **Device Setup:** [Device Model Setup](DEVICE-MODEL-SETUP-COMPLETE.md) - Device deployment

---

## Summary

1. **You have:** Pre-compiled libraries locally (~300KB each)
2. **You need:** Weights + tokenizer from HuggingFace (~2GB)
3. **No compilation needed:** Pre-compiled works perfectly
4. **Only rename:** Outer folder to AVA convention
5. **Keep original:** All internal filenames

**Full implementation details:** See [implementation-instructions/LLM-MODEL-COMPLETE-SETUP-GUIDE.md](implementation-instructions/LLM-MODEL-COMPLETE-SETUP-GUIDE.md)

---

## TVM v0.22.0 Compatibility Shim (NEW)

### Problem

Android loads JNI libraries with `RTLD_LOCAL`, making TVM symbols invisible to subsequently loaded model libraries (.adm/.ads files).

### Solution

A compatibility shim that uses `dlsym(RTLD_DEFAULT, ...)` to dynamically look up TVM functions at runtime.

### Files

| File | Purpose |
|------|---------|
| `tools/tvm-compat-shim/tvm_compat.c` | Dynamic symbol lookup shim |
| `tools/tvm-compat-shim/build-model.sh` | Build script for .ads files |
| `tools/tvm-compat-shim/.gitignore` | Ignore build artifacts |

### Building .ads Files

```bash
cd tools/tvm-compat-shim
./build-model.sh /path/to/model /path/to/model/{model}.ads
```

**Example:**
```bash
./build-model.sh /Volumes/M-Drive/Coding/AVA/external-models/llm/AVA-GE2-2B16 \
    /Volumes/M-Drive/Coding/AVA/external-models/llm/AVA-GE2-2B16/AVA-GE2-2B16.ads
```

### Built Models (TVM v0.22.0 Ready)

| Model | .ads Size | Status |
|-------|-----------|--------|
| AVA-GE2-2B16 | 3.6MB | Ready |
| AVA-GE3-4B16 | 5.8MB | Ready |

### Symbol Resolution

The shim provides:

1. **New FFI API** (TVM v0.22.0):
   - `TVMFFIFunctionCall`
   - `TVMFFIErrorSetRaisedFromCStr`
   - `TVMFFIEnvModRegisterSystemLibSymbol`

2. **Old C API Wrappers** (for legacy models):
   - `TVMFuncCall` → forwards to `TVMFFIFunctionCall`
   - `TVMAPISetLastError` → forwards to `TVMFFIErrorSetRaisedFromCStr`

3. **Backend Functions**:
   - `TVMBackendGetFuncFromEnv`
   - `TVMBackendAllocWorkspace`
   - `TVMBackendFreeWorkspace`
   - `TVMBackendParallelLaunch`

### TVMRuntime.kt Updates

- Prioritizes `.ads` files over legacy `.so`
- Renames `.ads` → `.so` when caching for TVM loader compatibility
- Logs "TVMShim v2 initialized" on successful load

---

## Backlog: Model Compilation Tasks

### Priority 0: AVA-QW3-06B16 (Qwen 0.6B) - BACKLOG (Low-Spec Devices)

**Status:** Weights only - needs MLC-LLM compilation for TVM v0.22.0

**Target Devices:** RealWear HMT-1 (2GB RAM)

**Required Output:**
- `AVA-QW3-06B16.ads` - Compiled model with TVM shim
- `tokenizer.ats` - AVA tokenizer

**Why Important:** Only model small enough for HMT-1's 2GB RAM constraint (~450MB total with embeddings)

**Dependencies:**
1. MLC-LLM compilation environment setup
2. TVM v0.22.0 build for macOS (host)
3. .adm generation from model weights

---

### Priority 1: AVA-GE3-4B16 (Gemma 3 4B) - COMPLETED

**Status:** Compilation instructions created, ALC-LLM CLI created, awaiting macOS TVM build

**Instructions:** [instructions/GEMMA-MODEL-COMPILATION-INSTRUCTIONS.md](instructions/GEMMA-MODEL-COMPILATION-INSTRUCTIONS.md)

**Model Location:** `/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-MLC/`

**What needs to be done:**
1. Build TVM v0.22.0 for macOS with LLVM (for host compilation)
2. Compile model with ALC-LLM for Android ARM64
3. Rename output files to AVA naming convention (.ADco, .ALM)
4. Verify output uses new FFI API symbols
5. Provide model metadata JSON for ALC integration

**Output files:**
- `AVA-GE3-4B16.ALM` - Compiled model package
- `AVALibrary.ADco` - Library code
- `AVA-GE3-4B16.ADco` - Device code

---

### Priority 2: AVA-GE2-2B16 (Gemma 2 2B) - BACKLOG

**Status:** Existing model compiled with old TVM, needs recompilation

**Current Model:**
```
/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/
└── gemma-2b-it-q4f16_1-android.tar  # Old TVM API - NOT COMPATIBLE
```

**Problem:** Model uses old C API symbols (`TVMFuncCall`, `TVMAPISetLastError`) which are not provided by TVM v0.22.0 runtime.

**What needs to be done:**

1. **Download original model** (if not already):
   ```bash
   git lfs install
   git clone https://huggingface.co/google/gemma-2b-it ~/models/gemma-2b-it-original
   ```

2. **Convert and compile with TVM v0.22.0:**
   ```bash
   cd /Volumes/M-Drive/Coding/AVA/external/mlc-llm

   # Convert weights
   python3 -m mlc_llm convert_weight \
       ~/models/gemma-2b-it-original \
       --quantization q4f16_1 \
       --output ~/models/gemma-2b-it-q4f16_1-weights

   # Generate config
   python3 -m mlc_llm gen_config \
       ~/models/gemma-2b-it-original \
       --quantization q4f16_1 \
       --context-window-size 2048 \
       --prefill-chunk-size 1024 \
       --output ~/models/gemma-2b-it-q4f16_1-weights

   # Compile for Android
   python3 -m mlc_llm compile \
       ~/models/gemma-2b-it-q4f16_1-weights/mlc-chat-config.json \
       --device android \
       -o ~/models/AVA-G2M-2B-Q4-android.tar
   ```

3. **Verify FFI API:**
   ```bash
   tar -xf ~/models/AVA-G2M-2B-Q4-android.tar -C /tmp/g2m-check
   nm -u /tmp/g2m-check/lib0.o | grep TVM
   # Should show: TVMFFIFunctionCall (NOT TVMFuncCall)
   ```

4. **Assemble model package:**
   ```bash
   mkdir -p ~/models/AVA-G2M-2B-Q4
   tar -xf ~/models/AVA-G2M-2B-Q4-android.tar -C ~/models/AVA-G2M-2B-Q4/

   # Copy weights and tokenizer from HuggingFace
   cp ~/models/gemma-2b-it-q4f16_1-weights/*.json ~/models/AVA-G2M-2B-Q4/
   cp ~/models/gemma-2b-it-q4f16_1-weights/tokenizer.* ~/models/AVA-G2M-2B-Q4/
   cp ~/models/gemma-2b-it-q4f16_1-weights/params_shard_*.bin ~/models/AVA-G2M-2B-Q4/
   ```

5. **Deploy to device:**
   ```bash
   adb push ~/models/AVA-G2M-2B-Q4 \
       /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
   ```

**Expected output files:**
- `lib0.o` - ~1MB
- `gemma_q4f16_1_devc.o` - ~3MB
- Must use TVMFFIFunctionCall, NOT TVMFuncCall

**Model metadata needed for ALC:**
```json
{
  "model_id": "AVA-GE2-2B16",
  "display_name": "Gemma 2 2B (Q4F16)",
  "tvm_version": "0.22.0",
  "quantization": "q4f16_1",
  "context_length": 2048,
  "vocab_size": 256000,
  "hidden_size": 2048,
  "num_hidden_layers": 18,
  "device_code_file": "AVA-GE2-2B16.ADco",
  "library_file": "AVALibrary.ADco"
}
```

---

## Related Documentation

- **Chapter 38:** [LLM Model Management](Developer-Manual-Chapter38-LLM-Model-Management.md) - Architecture and API
- **Full Guide:** [LLM Model Complete Setup Guide](implementation-instructions/LLM-MODEL-COMPLETE-SETUP-GUIDE.md) - Step-by-step instructions
- **Model Sources:** [Model Download Sources](MODEL-DOWNLOAD-SOURCES.md) - Where to get models
- **Device Setup:** [Device Model Setup](DEVICE-MODEL-SETUP-COMPLETE.md) - Device deployment
- **Compilation Instructions:** [Gemma Model Compilation](instructions/GEMMA-MODEL-COMPILATION-INSTRUCTIONS.md) - TVM v0.22.0 compilation

---

**Document Version:** 2.2
**Last Updated:** 2025-12-04
