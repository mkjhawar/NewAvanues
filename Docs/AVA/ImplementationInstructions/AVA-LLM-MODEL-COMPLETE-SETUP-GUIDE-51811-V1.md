# AVA LLM Model Complete Setup Guide

**Version:** 1.0
**Last Updated:** 2025-11-17
**Purpose:** Complete instructions for obtaining and setting up LLM models for AVA

---

## Table of Contents

1. [Overview](#1-overview)
2. [TVM Runtime with FFI Bindings](#2-tvm-runtime-with-ffi-bindings)
3. [Understanding MLC-LLM Model Structure](#3-understanding-mlc-llm-model-structure)
4. [Method 1: Download Pre-Compiled from HuggingFace (Easiest)](#4-method-1-download-pre-compiled-from-huggingface-easiest)
5. [Method 2: Download Components Separately](#5-method-2-download-components-separately)
6. [Method 3: Compile Your Own Model (Advanced)](#6-method-3-compile-your-own-model-advanced)
7. [Device Setup and Deployment](#7-device-setup-and-deployment)
8. [File Naming Conventions](#8-file-naming-conventions)
9. [Verification and Testing](#9-verification-and-testing)
10. [Troubleshooting](#10-troubleshooting)
11. [Alternative Models](#11-alternative-models)

---

## 1. Overview

AVA uses MLC-LLM (Machine Learning Compilation for LLMs) for on-device inference. A complete model requires multiple components working together.

### What You Need

| Component | Description | Size | Source |
|-----------|-------------|------|--------|
| Compiled Library | TVM-compiled device code (.o files) | ~300KB | binary-mlc-llm-libs or compile yourself |
| Model Weights | Quantized neural network weights | ~1.5-2GB | HuggingFace (converted) |
| Tokenizer | Text ↔ token conversion | ~4MB | Original model or conversion |
| Config Files | Model configuration | ~2KB | Conversion process |

### What You Already Have Locally

You have pre-compiled libraries at:
```
/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/
```

**For Gemma-2B (recommended):**
```
gemma-2b-it/
├── gemma-2b-it-q4f16_1-android.tar     (311KB) ← COMPILED LIBRARY ONLY
├── gemma-2b-it-q4f16_1-ctx1k_cs1k-webgpu.wasm
├── gemma-2b-it-q4f16_1-ctx4k_cs1k-webgpu.wasm
├── gemma-2b-it-q4f16_1-iphone.tar
└── ... (other platforms)
```

**Other available models (with Android support):**
- `Llama-2-7b-chat-hf/Llama-2-7b-chat-hf-q4f16_1-android.tar` (348KB)
- `Mistral-7B-Instruct-v0.2/Mistral-7B-Instruct-v0.2-q4f16_1-android.tar` (349KB)
- `phi-2/phi-2-q4f16_1-android.tar` (349KB)
- `RedPajama-INCITE-Chat-3B-v1/RedPajama-INCITE-Chat-3B-v1-q4f16_1-android.tar` (355KB)

### What You Still Need

The local `.tar` files only contain **compiled TVM libraries** (~300KB). You still need:

| Component | Size | Where to Get |
|-----------|------|--------------|
| Model Weights | ~2GB | HuggingFace: `mlc-ai/gemma-2b-it-q4f16_1-MLC` |
| Tokenizer | ~4MB | HuggingFace (same repo) |
| Config Files | ~5KB | HuggingFace (same repo) |

**Do You Need to Compile?**

**NO** - The pre-compiled `.tar` files work perfectly. Compilation is only needed if you want:
- Different quantization (e.g., q8 instead of q4)
- Custom model not in binary-mlc-llm-libs
- Specific hardware optimization (Snapdragon, Mali GPU)

### Recommended Model: Gemma-2B-IT

- **Parameters:** 2 billion
- **Quantization:** Q4F16_1 (4-bit weights, 16-bit activations)
- **Size:** ~2GB total
- **RAM Required:** 4GB minimum
- **Quality:** Good for general conversation

---

## 2. TVM Runtime with FFI Bindings

### Understanding the Difference

MLC-LLM requires **two separate components**:

| Component | Purpose | What It Contains |
|-----------|---------|------------------|
| **Model Libraries** | Model-specific compiled code | `.o` files (gemma_q4f16_1_devc.o) |
| **TVM Runtime** | Execution engine with Java bindings | `libtvm4j_runtime_packed.so` + `tvm4j_core.jar` |

**Important:** The pre-compiled `.tar` files from `binary-mlc-llm-libs` contain only model libraries, NOT the runtime. You need both for LLM to work.

### The Problem

If you see this error:
```
UnsatisfiedLinkError: No implementation found for
org.apache.tvm.LibInfo.tvmFFIFunctionGetGlobal
```

This means the TVM runtime doesn't have FFI (Foreign Function Interface) bindings. The Java code cannot communicate with the native TVM library.

### Solution Options

#### Option A: Extract from MLC Chat APK (Fastest)

1. **Download MLC Chat APK:**
   - From APKMirror: https://www.apkmirror.com/apk/mlc-ai/mlcchat/
   - Or GitHub releases: https://github.com/mlc-ai/binary-mlc-llm-libs/releases

2. **Extract native libraries:**
   ```bash
   # Unzip the APK
   unzip MLCChat.apk -d mlc-chat-extracted

   # Find the libraries
   ls mlc-chat-extracted/lib/arm64-v8a/
   # Should contain:
   # - libtvm4j_runtime_packed.so (with FFI bindings)
   # - libmlc_llm.so
   ```

3. **Copy to your project:**
   ```bash
   cp mlc-chat-extracted/lib/arm64-v8a/*.so \
      /path/to/AVA/apps/ava-standalone/src/main/jniLibs/arm64-v8a/
   ```

#### Option B: Build from MLC-LLM Source (Recommended for Production)

1. **Prerequisites:**
   ```bash
   # Install Rust (for tokenizers)
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
   rustup target add aarch64-linux-android

   # Set environment variables
   export ANDROID_NDK=/path/to/android-ndk
   export TVM_NDK_CC=$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android24-clang
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
   ```

2. **Clone MLC-LLM:**
   ```bash
   git clone --recursive https://github.com/mlc-ai/mlc-llm.git
   cd mlc-llm
   ```

3. **Build the runtime:**
   ```bash
   cd android/MLCChat

   # Create package config
   cat > mlc-package-config.json << 'EOF'
   {
     "device": "android",
     "model_list": [
       {
         "model": "HF://mlc-ai/gemma-2b-it-q4f16_1-MLC",
         "model_id": "gemma-2b-it-q4f16_1",
         "estimated_vram_bytes": 3000000000
       }
     ]
   }
   EOF

   # Build package (this creates the runtime)
   mlc_llm package
   ```

4. **Output location:**
   ```
   dist/lib/mlc4j/
   ├── build.gradle
   ├── src/
   └── output/
       └── arm64-v8a/
           └── libtvm4j_runtime_packed.so  ← This has FFI bindings!
   ```

5. **Integrate with AVA:**
   ```gradle
   // In settings.gradle
   include ':mlc4j'
   project(':mlc4j').projectDir = file('path/to/dist/lib/mlc4j')

   // In app/build.gradle
   dependencies {
       implementation project(':mlc4j')
   }
   ```

### Verifying FFI Bindings

After integration, test that FFI works:

```kotlin
// This should not throw UnsatisfiedLinkError
System.loadLibrary("tvm4j_runtime_packed")
val func = org.apache.tvm.Function.getFunction("runtime.Module")
```

If this works without errors, FFI bindings are properly configured.

### Current AVA Status

AVA currently loads `tvm4j_runtime_packed` but the library lacks FFI bindings, causing the app to fall back to template responses. To enable full LLM functionality:

1. Obtain runtime with FFI bindings (Option A or B above)
2. Replace the current `.so` file
3. Rebuild and test

---

## 3. Understanding MLC-LLM Model Structure

### Complete Model Directory Structure

```
AVA-GEM-2B-Q4/                          ← AVA folder name (can be renamed)
├── lib0.o                              ← Compiled library (DO NOT rename)
├── gemma_q4f16_1_devc.o                ← TVM device code (DO NOT rename)
├── mlc-chat-config.json                ← Model configuration
├── ndarray-cache.json                  ← Weight shard index
├── tokenizer.model                     ← SentencePiece tokenizer
├── tokenizer_config.json               ← Tokenizer settings
├── params_shard_0.bin                  ← Model weights
├── params_shard_1.bin
├── params_shard_2.bin
└── ...                                 ← More weight shards
```

### Important: Naming Rules

**CAN Rename:**
- Outer folder name (e.g., `AVA-GEM-2B-Q4/`)
- Archive name (e.g., `AVA-GEM-2B-Q4.tar`)

**DO NOT Rename (internal files must keep original names):**
- `gemma_q4f16_1_devc.o` - Referenced by mlc-chat-config.json
- `tokenizer.model` - Expected by tokenizer loader
- Config JSON files - Contain internal references

---

## 3. Method 1: Download Pre-Compiled from HuggingFace (Easiest)

This is the recommended approach. You already have the compiled library locally - just need the weights and tokenizer.

### Step 1: Install Git LFS

Git LFS (Large File Storage) is required for downloading large model files.

```bash
# macOS
brew install git-lfs

# Ubuntu/Debian
sudo apt install git-lfs

# Windows
# Download from https://git-lfs.github.com/

# Initialize Git LFS
git lfs install
```

### Step 2: Clone Weights and Tokenizer from HuggingFace

```bash
# Create working directory
mkdir -p ~/mlc-models && cd ~/mlc-models

# Clone the complete Gemma-2B model (weights + tokenizer + configs)
git clone https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC

# This downloads (~2GB):
# - Model weights (params_shard_*.bin) ← ~2GB
# - Tokenizer (tokenizer.model) ← ~4MB
# - Config files (mlc-chat-config.json, ndarray-cache.json) ← ~5KB
```

**HuggingFace Link:** https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC

### Step 3: Add Compiled Library from Local Files

The HuggingFace download includes weights but NOT the compiled library. You already have this locally:

```bash
# Your local compiled library location:
# /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/gemma-2b-it-q4f16_1-android.tar

# Extract to model folder
cd /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it
tar -xf gemma-2b-it-q4f16_1-android.tar -C ~/mlc-models/gemma-2b-it-q4f16_1-MLC/

# This adds:
# - lib0.o (~100KB)
# - gemma_q4f16_1_devc.o (~200KB)
```

**Alternative: If you don't have local files:**
```bash
cd ~/mlc-models

# Clone with sparse checkout (faster, only Android files)
git clone --depth=1 --filter=blob:none --sparse \
    https://github.com/mlc-ai/binary-mlc-llm-libs.git

cd binary-mlc-llm-libs
git sparse-checkout set gemma-2b-it

# Extract the tar file
cd gemma-2b-it
tar -xf gemma-2b-it-q4f16_1-android.tar -C ~/mlc-models/gemma-2b-it-q4f16_1-MLC/
```

### Step 4: Rename for AVA

```bash
# Rename folder to AVA naming convention
cd ~/mlc-models
mv gemma-2b-it-q4f16_1-MLC AVA-GEM-2B-Q4
```

### Step 5: Verify Contents

```bash
ls -lh ~/mlc-models/AVA-GEM-2B-Q4/

# Should see ALL these files:
# lib0.o                    ~100KB   ← From local .tar
# gemma_q4f16_1_devc.o      ~200KB   ← From local .tar
# mlc-chat-config.json      ~1KB     ← From HuggingFace
# ndarray-cache.json        ~5KB     ← From HuggingFace
# tokenizer.model           ~4MB     ← From HuggingFace
# tokenizer_config.json     ~1KB     ← From HuggingFace
# params_shard_0.bin        ~500MB   ← From HuggingFace
# params_shard_1.bin        ~500MB   ← From HuggingFace
# ...                                ← More weight shards
```

### What Each File Does

| File | Source | Purpose |
|------|--------|---------|
| `lib0.o` | Local .tar | TVM base runtime library |
| `gemma_q4f16_1_devc.o` | Local .tar | Compiled TVM device code for this specific model |
| `mlc-chat-config.json` | HuggingFace | Model configuration (context length, vocab size, etc.) |
| `ndarray-cache.json` | HuggingFace | Index of weight shard files |
| `tokenizer.model` | HuggingFace | SentencePiece tokenizer for text↔token conversion |
| `tokenizer_config.json` | HuggingFace | Tokenizer settings (special tokens, etc.) |
| `params_shard_*.bin` | HuggingFace | Quantized neural network weights (the actual model) |

---

## 4. Method 2: Download Components Separately

Use this if you already have some components or want more control.

### Component 1: Compiled Library (.o files)

**Source:** https://github.com/mlc-ai/binary-mlc-llm-libs

```bash
# Download and extract
cd ~/mlc-models
mkdir -p AVA-GEM-2B-Q4

# Get the tar file
curl -L https://github.com/mlc-ai/binary-mlc-llm-libs/raw/main/gemma-2b-it/gemma-2b-it-q4f16_1-android.tar \
    -o gemma-android.tar

# Extract to model folder
tar -xf gemma-android.tar -C AVA-GEM-2B-Q4/
rm gemma-android.tar

# Verify
ls AVA-GEM-2B-Q4/
# lib0.o
# gemma_q4f16_1_devc.o
```

### Component 2: Model Weights (params_shard_*.bin)

**Source:** https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC

```bash
cd ~/mlc-models/AVA-GEM-2B-Q4

# Download weight files individually
# (Or use git clone as shown in Method 1)

# Download ndarray-cache.json first (lists all shards)
curl -L https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC/resolve/main/ndarray-cache.json \
    -o ndarray-cache.json

# Download each shard (example for first 3)
curl -L https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC/resolve/main/params_shard_0.bin \
    -o params_shard_0.bin

curl -L https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC/resolve/main/params_shard_1.bin \
    -o params_shard_1.bin

# Continue for all shards listed in ndarray-cache.json...
```

### Component 3: Tokenizer

**Source:** Original Gemma model on HuggingFace

```bash
cd ~/mlc-models/AVA-GEM-2B-Q4

# Download tokenizer (requires HuggingFace login for Gemma)
# Option A: From MLC-converted model (no login required)
curl -L https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC/resolve/main/tokenizer.model \
    -o tokenizer.model

curl -L https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC/resolve/main/tokenizer_config.json \
    -o tokenizer_config.json

# Option B: From original Google model (requires HuggingFace login)
# huggingface-cli login
# huggingface-cli download google/gemma-2b-it tokenizer.model --local-dir .
```

### Component 4: Config Files

**Source:** MLC-converted model

```bash
cd ~/mlc-models/AVA-GEM-2B-Q4

# Download mlc-chat-config.json
curl -L https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC/resolve/main/mlc-chat-config.json \
    -o mlc-chat-config.json
```

---

## 5. Method 3: Compile Your Own Model (Advanced)

Use this for custom models, different quantizations, or latest versions.

### Prerequisites

```bash
# Install Python dependencies
pip install mlc-llm transformers torch

# Install TVM (for compilation)
pip install apache-tvm

# Verify installation
python -c "import mlc_llm; print('MLC-LLM version:', mlc_llm.__version__)"
```

### Step 1: Download Original Model from HuggingFace

```bash
# Login to HuggingFace (required for Gemma)
huggingface-cli login
# Enter your token from https://huggingface.co/settings/tokens

# Accept Gemma license at https://huggingface.co/google/gemma-2b-it

# Download original model
mkdir -p ~/mlc-compile && cd ~/mlc-compile
huggingface-cli download google/gemma-2b-it --local-dir ./gemma-2b-it-original

# This downloads:
# - Model weights (pytorch_model*.bin or model*.safetensors)
# - Tokenizer (tokenizer.model)
# - Config (config.json)
```

### Step 2: Convert Weights to MLC Format

```bash
cd ~/mlc-compile

# Convert and quantize weights
mlc_llm convert_weight ./gemma-2b-it-original \
    --quantization q4f16_1 \
    --output ./gemma-2b-it-q4f16_1-weights

# This creates:
# - params_shard_*.bin (quantized weights)
# - ndarray-cache.json (shard index)
# - mlc-chat-config.json (model config)
```

**Quantization Options:**

| Option | Description | Size | Quality |
|--------|-------------|------|---------|
| `q4f16_1` | 4-bit weights, 16-bit activations (recommended) | ~2GB | Good |
| `q4f32_1` | 4-bit weights, 32-bit activations | ~2GB | Better |
| `q8f16_1` | 8-bit weights, 16-bit activations | ~4GB | Best |
| `q0f16` | No quantization, 16-bit | ~8GB | Original |

### Step 3: Compile for Android

```bash
cd ~/mlc-compile

# Compile TVM model for Android
mlc_llm compile ./gemma-2b-it-q4f16_1-weights \
    --target android \
    --output ./gemma-android-lib

# This creates:
# - lib0.o
# - gemma_q4f16_1_devc.o
```

**Target Options:**

| Target | Description |
|--------|-------------|
| `android` | Android with OpenCL GPU |
| `android -mcpu=snapdragon` | Optimized for Snapdragon |
| `android -mcpu=mali` | Optimized for Mali GPU |
| `ios` | iOS Metal |
| `webgpu` | Browser WebGPU |

### Step 4: Assemble Complete Model

```bash
cd ~/mlc-compile

# Create final model directory
mkdir -p AVA-GEM-2B-Q4

# Copy compiled library
cp gemma-android-lib/*.o AVA-GEM-2B-Q4/

# Copy weights and configs
cp gemma-2b-it-q4f16_1-weights/params_shard_*.bin AVA-GEM-2B-Q4/
cp gemma-2b-it-q4f16_1-weights/ndarray-cache.json AVA-GEM-2B-Q4/
cp gemma-2b-it-q4f16_1-weights/mlc-chat-config.json AVA-GEM-2B-Q4/

# Copy tokenizer from original
cp gemma-2b-it-original/tokenizer.model AVA-GEM-2B-Q4/
cp gemma-2b-it-original/tokenizer_config.json AVA-GEM-2B-Q4/
```

### Step 5: Verify Compilation

```bash
# Check all files present
ls -lh ~/mlc-compile/AVA-GEM-2B-Q4/

# Verify mlc-chat-config.json contains correct model_lib
cat ~/mlc-compile/AVA-GEM-2B-Q4/mlc-chat-config.json | grep model_lib
# Should show: "model_lib": "gemma_q4f16_1"
```

---

## 6. Device Setup and Deployment

### Device Directory Structure

AVA expects models at this specific location:

```
/sdcard/Android/data/com.augmentalis.ava/files/models/
├── AVA-ONX-384-BASE.onnx                    ← NLU Embedding model
└── llm/
    └── AVA-GEM-2B-Q4/                       ← LLM folder (AVA naming)
        ├── lib0.o                           ← Keep original name
        ├── gemma_q4f16_1_devc.o              ← Keep original name
        ├── mlc-chat-config.json             ← Keep original name
        ├── ndarray-cache.json               ← Keep original name
        ├── tokenizer.model                  ← Keep original name
        ├── tokenizer_config.json            ← Keep original name
        ├── params_shard_0.bin               ← Keep original name
        ├── params_shard_1.bin               ← Keep original name
        └── ...
```

### Folder Naming Rules

**Outer folder (CAN rename to AVA convention):**
- `AVA-GEM-2B-Q4` for Gemma 2B
- `AVA-PHI-3B-Q4` for Phi-2
- `AVA-MST-7B-Q4` for Mistral 7B

**Inner files (MUST keep original names):**
- `.o` files - TVM looks for specific names from config
- `tokenizer.model` - Tokenizer loader expects this name
- Config JSONs - Contain references to other files

### Why This Matters

The `mlc-chat-config.json` contains:
```json
{
  "model_lib": "gemma_q4f16_1",  ← Must match .o filename
  ...
}
```

TVM loads `{model_lib}_devc.o`, so `gemma_q4f16_1_devc.o` must exist with that exact name.

### Push Model to Android Device

```bash
# Connect device via USB with debugging enabled

# Create directory structure
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Push entire model folder
adb push ~/mlc-models/AVA-GEM-2B-Q4 \
    /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GEM-2B-Q4/
```

### Alternative: Create Archive

```bash
# Create tar archive for easier transfer
cd ~/mlc-models
tar -cvf AVA-GEM-2B-Q4.tar AVA-GEM-2B-Q4/

# Push archive
adb push AVA-GEM-2B-Q4.tar \
    /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Extract on device
adb shell "cd /sdcard/Android/data/com.augmentalis.ava/files/models/llm && tar -xf AVA-GEM-2B-Q4.tar"
```

### Device Directory Structure

```
/sdcard/Android/data/com.augmentalis.ava/files/models/
├── AVA-ONX-384-BASE.onnx              ← Embedding model for NLU
└── llm/
    └── AVA-GEM-2B-Q4/                 ← LLM model
        ├── lib0.o
        ├── gemma_q4f16_1_devc.o
        ├── mlc-chat-config.json
        ├── ndarray-cache.json
        ├── tokenizer.model
        ├── tokenizer_config.json
        ├── params_shard_0.bin
        ├── params_shard_1.bin
        └── ...
```

---

## 7. File Naming Conventions

### AVA Proprietary Naming

AVA uses proprietary names for external identification while keeping internal filenames original.

**Format:** `AVA-{TYPE}-{SIZE}-{QUANT}`

| Component | Meaning | Examples |
|-----------|---------|----------|
| AVA | Prefix | Always "AVA" |
| TYPE | Model family | GEM (Gemma), PHI (Phi), MST (Mistral), LLM (Llama) |
| SIZE | Parameter count | 2B, 3B, 7B, 13B |
| QUANT | Quantization | Q4 (4-bit), Q8 (8-bit), FP16 |

**Examples:**
- `AVA-GEM-2B-Q4` - Gemma 2B, 4-bit quantization
- `AVA-PHI-3B-Q4` - Phi 3B, 4-bit quantization
- `AVA-MST-7B-Q4` - Mistral 7B, 4-bit quantization

### Internal vs External Names

| What | External (AVA) | Internal (Original) |
|------|----------------|---------------------|
| Folder | `AVA-GEM-2B-Q4/` | N/A |
| Compiled lib | N/A | `gemma_q4f16_1_devc.o` |
| Config | N/A | `mlc-chat-config.json` |
| Tokenizer | N/A | `tokenizer.model` |

---

## 8. Verification and Testing

### Check Model Files

```bash
# List all files with sizes
adb shell ls -lhR /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GEM-2B-Q4/

# Expected sizes:
# lib0.o                    ~100KB
# gemma_q4f16_1_devc.o      ~200KB
# mlc-chat-config.json      ~1KB
# ndarray-cache.json        ~5KB
# tokenizer.model           ~4MB
# params_shard_*.bin        ~500MB each
# Total: ~2GB
```

### Verify Config Content

```bash
# Check model_lib matches .o filename
adb shell cat /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GEM-2B-Q4/mlc-chat-config.json

# Should contain:
# "model_lib": "gemma_q4f16_1"
```

### Test in AVA App

1. Open AVA app
2. Go to Settings → Model Management
3. Verify model shows as "Downloaded"
4. Start a chat: "Tell me a joke"
5. Verify LLM responds with generated text

### Check Logs

```bash
# Filter for LLM-related logs
adb logcat -s "ALCEngine:*" "TVMRuntime:*" "LocalLLMProvider:*"

# Should see:
# I/TVMRuntime: TVM native runtime loaded successfully
# I/ALCEngine: Loading language pack: en
# I/TVMRuntime: TVM module loaded successfully
```

---

## 9. Troubleshooting

### Issue: "Model file not found"

**Symptoms:** LLM initialization fails with file not found error

**Solutions:**
```bash
# Check directory exists
adb shell ls -la /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Check permissions
adb shell chmod -R 755 /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Verify exact path in logs
adb logcat -s "TVMRuntime:*" | grep "not found"
```

### Issue: "TVM module load failed"

**Symptoms:** TVM runtime can't load the .o files

**Solutions:**
```bash
# Verify .o files exist
adb shell ls -l /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GEM-2B-Q4/*.o

# Check model_lib in config matches .o filename
adb shell cat .../AVA-GEM-2B-Q4/mlc-chat-config.json | grep model_lib
# Must match: gemma_q4f16_1 (without _devc.o suffix)

# Check device compatibility
# .o files must be compiled for same Android ABI
adb shell getprop ro.product.cpu.abi
# Should be: arm64-v8a (for most modern devices)
```

### Issue: "Tokenizer not found"

**Symptoms:** Model loads but can't tokenize text

**Solutions:**
```bash
# Verify tokenizer.model exists
adb shell ls -l /sdcard/Android/data/com.augmentalis.ava/files/models/llm/AVA-GEM-2B-Q4/tokenizer.model

# Check file size (should be ~4MB for Gemma)
# If 0 or very small, re-download
```

### Issue: Out of Memory

**Symptoms:** App crashes or model fails to load

**Solutions:**
- Use smaller model (TinyLlama 1.1B instead of Gemma 2B)
- Close other apps before loading model
- Increase device swap space (requires root)
- Use more aggressive quantization (q4f16_1 vs q8f16_1)

### Issue: Slow Inference

**Symptoms:** LLM generates text very slowly

**Solutions:**
- Enable OpenCL GPU acceleration (check device support)
- Use smaller context length in settings
- Reduce max generation length
- Consider Snapdragon-optimized compilation

---

## 10. Using a Different Model

### What Changes Per Model

The **TVM Runtime stays the same** for all models. Only model-specific files change:

| Component | Same or Different | Example |
|-----------|-------------------|---------|
| TVM Runtime (.so) | **SAME** | `libtvm4j_runtime_packed.so` |
| Model Library (.o) | Different | `llama_q4f16_1_devc.o` |
| Weights | Different | `params_shard_*.bin` |
| Tokenizer | Different | `tokenizer.model` |
| Config | Different | `mlc-chat-config.json` |

### Available Models

From `binary-mlc-llm-libs-Android-09262024/`:

| Model | Folder | Size | RAM | Quality | Speed |
|-------|--------|------|-----|---------|-------|
| TinyLlama 1.1B | `TinyLlama-1.1B-Chat-v0.4` | ~1GB | 3GB | Basic | Fast |
| Gemma 2B | `gemma-2b-it` | ~2GB | 4GB | Good | Medium |
| Phi-2 2.7B | `phi-2` | ~2GB | 4GB | Good | Medium |
| RedPajama 3B | `RedPajama-INCITE-Chat-3B-v1` | ~2GB | 4GB | Good | Medium |
| Llama-2 7B | `Llama-2-7b-chat-hf` | ~4GB | 6GB | Excellent | Slow |
| Mistral 7B | `Mistral-7B-Instruct-v0.2` | ~4GB | 6GB | Excellent | Slow |

### Step-by-Step: Switch to a Different Model

#### Step 1: Get Model Library (.tar)

```bash
# Example: Llama-2 7B
cd /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/Llama-2-7b-chat-hf

# List available .tar files
ls *.tar

# Extract the Android version
tar -xf Llama-2-7b-chat-hf-q4f16_1-android.tar
# Creates: lib0.o, Llama_2_7b_chat_hf_q4f16_1_devc.o
```

#### Step 2: Get Weights from HuggingFace

Each model has a corresponding MLC-converted repo:

| Model | HuggingFace Repo |
|-------|------------------|
| Gemma 2B | `mlc-ai/gemma-2b-it-q4f16_1-MLC` |
| Llama-2 7B | `mlc-ai/Llama-2-7b-chat-hf-q4f16_1-MLC` |
| Mistral 7B | `mlc-ai/Mistral-7B-Instruct-v0.2-q4f16_1-MLC` |
| Phi-2 | `mlc-ai/phi-2-q4f16_1-MLC` |
| TinyLlama | `mlc-ai/TinyLlama-1.1B-Chat-v1.0-q4f16_1-MLC` |

```bash
# Clone weights (example: Llama-2 7B)
git lfs install
git clone https://huggingface.co/mlc-ai/Llama-2-7b-chat-hf-q4f16_1-MLC ~/mlc-models/AVA-LLM-7B-Q4
```

#### Step 3: Combine Library + Weights

```bash
# Copy compiled library files into model folder
cp Llama_2_7b_chat_hf_q4f16_1_devc.o ~/mlc-models/AVA-LLM-7B-Q4/
cp lib0.o ~/mlc-models/AVA-LLM-7B-Q4/
```

#### Step 4: Push to Device

```bash
adb push ~/mlc-models/AVA-LLM-7B-Q4 \
    /sdcard/Android/data/com.augmentalis.ava.debug/files/models/
```

#### Step 5: Update AVA Configuration

Update the model path in your code:

```kotlin
// In LLMConfig or wherever model path is set
val config = LLMConfig(
    modelPath = "models/AVA-LLM-7B-Q4",  // Changed from AVA-GEM-2B-Q4
    device = "opencl"
)
```

### AVA Naming Convention

| Original Model | AVA Folder Name |
|----------------|-----------------|
| gemma-2b-it-q4f16_1 | `AVA-GEM-2B-Q4` |
| Llama-2-7b-chat-hf-q4f16_1 | `AVA-LLM-7B-Q4` |
| Mistral-7B-Instruct-v0.2-q4f16_1 | `AVA-MST-7B-Q4` |
| phi-2-q4f16_1 | `AVA-PHI-3B-Q4` |
| TinyLlama-1.1B-Chat-v0.4-q4f16_1 | `AVA-TLL-1B-Q4` |
| RedPajama-INCITE-Chat-3B-v1-q4f16_1 | `AVA-RPJ-3B-Q4` |

### Important: Model Library Name Must Match

The `mlc-chat-config.json` contains a `model_lib` field that **must match** the `.o` filename:

```json
{
  "model_lib": "Llama_2_7b_chat_hf_q4f16_1"
}
```

This means the file `Llama_2_7b_chat_hf_q4f16_1_devc.o` must exist in the same folder.

**NEVER rename the .o files** - they must keep their original names.

### Directory Structure Comparison

```bash
# Gemma 2B (current)
models/AVA-GEM-2B-Q4/
├── gemma_q4f16_1_devc.o          # Model-specific
├── lib0.o
├── mlc-chat-config.json
├── params_shard_*.bin             # ~2GB total
└── tokenizer.model

# Llama-2 7B (alternative)
models/AVA-LLM-7B-Q4/
├── Llama_2_7b_chat_hf_q4f16_1_devc.o   # Different name
├── lib0.o
├── mlc-chat-config.json
├── params_shard_*.bin                   # ~4GB total
└── tokenizer.model

# TinyLlama 1.1B (lightweight)
models/AVA-TLL-1B-Q4/
├── TinyLlama_1_1B_Chat_v0_4_q4f16_1_devc.o
├── lib0.o
├── mlc-chat-config.json
├── params_shard_*.bin                   # ~600MB total
└── tokenizer.model
```

---

## Quick Reference

### Minimum Setup (Copy-Paste)

```bash
# 1. Clone complete model
git lfs install
git clone https://huggingface.co/mlc-ai/gemma-2b-it-q4f16_1-MLC ~/mlc-models/AVA-GEM-2B-Q4

# 2. Add compiled library
cd /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it
tar -xf gemma-2b-it-q4f16_1-android.tar -C ~/mlc-models/AVA-GEM-2B-Q4/

# 3. Push to device
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
adb push ~/mlc-models/AVA-GEM-2B-Q4 /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# 4. Restart AVA and test
```

### File Checklist

Before deployment, verify you have:

- [ ] `lib0.o` - TVM base library
- [ ] `gemma_q4f16_1_devc.o` - Compiled device code
- [ ] `mlc-chat-config.json` - Model configuration
- [ ] `ndarray-cache.json` - Weight shard index
- [ ] `tokenizer.model` - SentencePiece tokenizer
- [ ] `params_shard_*.bin` - Model weight shards (multiple files)

---

## Resources

### Official Documentation
- **MLC-LLM:** https://llm.mlc.ai/docs/
- **TVM:** https://tvm.apache.org/docs/
- **HuggingFace:** https://huggingface.co/docs

### Model Repositories
- **Pre-compiled models:** https://huggingface.co/mlc-ai
- **Compiled libraries:** https://github.com/mlc-ai/binary-mlc-llm-libs
- **MLC-LLM source:** https://github.com/mlc-ai/mlc-llm

### Community
- **MLC-LLM Discord:** https://discord.gg/9Xpy2HGBuD
- **GitHub Issues:** https://github.com/mlc-ai/mlc-llm/issues

---

**Document Version:** 1.0
**Last Updated:** 2025-11-17
**Maintained by:** AVA Development Team
