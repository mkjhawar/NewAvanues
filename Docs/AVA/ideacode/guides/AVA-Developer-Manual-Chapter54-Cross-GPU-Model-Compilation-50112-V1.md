# Developer Manual - Chapter 54: Cross-GPU Model Compilation

**Last Updated:** 2025-11-30
**Author:** AVA Development Team
**Status:** Production

---

## Overview

This chapter provides complete instructions for compiling LLM models for different GPU backends. AVA supports multiple target platforms including:

- **Android Adreno** (Qualcomm) - Most Android phones, RealWear, Samsung
- **Android Mali** (ARM) - Rokid X-Craft, some MediaTek devices
- **iOS Metal** (Apple) - iPhone, iPad
- **Android Vulkan** (Universal) - Pre-compiled, works on most Android

---

## TVM Version Architecture

### Important: Compilation vs Runtime TVM

AVA uses **two separate TVM instances**:

| Component | TVM Version | Purpose | Location |
|-----------|-------------|---------|----------|
| **Compilation Toolchain** | 0.18.dev0 (bundled with mlc-ai-nightly 0.15) | Cross-compile models on macOS/Linux | Python pip package |
| **Android Runtime** | 0.22.0 | Execute models on device | `libtvm_runtime.so` + `libtvm4j.so` |

**Why Different Versions?**
- The Python compilation toolchain uses its own bundled TVM for cross-compilation
- The compiled `.tar` output files are **ABI-compatible** with TVM 0.22.0 runtime
- The model weights (`.bin` files) are architecture-independent

### AVA's TVM 0.22.0 Runtime (Android)

```
Universal/AVA/Apps/AVA-Standalone/src/main/jniLibs/arm64-v8a/
├── libtvm_runtime.so    # 62MB - TVM v0.22.0 runtime with FFI API
└── libtvm4j.so          # 124KB - JNI bridge for org.apache.tvm
```

**Custom Build Patches:**
- `external/mlc-llm/3rdparty/tvm/jvm/core/src/main/java/org/apache/tvm/Module.java`
- `Universal/AVA/Features/LLM/tvm-patches/Module.java`
- `Universal/AVA/Features/LLM/libs/tvm4j_core.jar` (rebuilt)

**Key FFI Fix:**
```java
// TVM v0.22.0 uses single-argument API
return Base.loadFromFile(path);  // NOT loadFromFile(path, format)
```

---

## Compilation Toolchain Setup

### Prerequisites

```bash
# 1. Install mlc-ai-nightly (includes TVM 0.18)
python3 -m pip install --pre mlc-ai-nightly

# 2. Verify installation
python3 -m mlc_llm --help

# 3. Check bundled TVM version
python3 -c "import tvm; print(tvm.__version__)"
# Output: 0.18.dev0
```

### Supported Model Types

The `mlc_llm compile` command supports these AVA model architectures:

| AVA Model | Type Code | Supported |
|-----------|-----------|-----------|
| AVA-LL32-* | `llama` | Yes |
| AVA-GE2-* | `gemma2` | Yes |
| AVA-GE3-* | `gemma3` | Yes |
| AVA-QW3-* (0.6B-4B) | `qwen2` | Yes |
| AVA-MI7-* | `mistral` | Yes |
| AVA-PH3-* | `phi3` | Yes |

**Note:** Pre-compiled Adreno/Vulkan models are available from HuggingFace for all AVA-QW3-* models.

---

## GPU Backend Compilation

### Backend Overview

| GPU Type | Target Device | Compile Flag | Output | Notes |
|----------|---------------|--------------|--------|-------|
| **Adreno Vulkan** | Samsung, RealWear, most Android | `--device vulkan` | Universal | Pre-compiled available |
| **Mali OpenCL** | Rokid X-Craft, MediaTek | `--device opencl` | Mali-specific | Needs compilation |
| **Apple Metal** | iPhone, iPad | `--host arm64-apple-ios` | iOS-specific | Needs compilation |

### Directory Structure

```
ava-ai-models-external/
├── llm/                  # Adreno/Vulkan (universal Android)
│   ├── AVA-LL32-1B16/
│   ├── AVA-GE2-2B16/
│   └── ...
├── llm-mali/             # Mali GPU (Rokid, MediaTek)
│   ├── AVA-LL32-1B16/
│   └── AVA-GE2-2B16/
└── llm-coreml/           # iOS CoreML (iPhone)
    ├── AVA-LL32-1B16/
    └── AVA-GE2-2B16/
```

---

## Compilation Commands

### Mali GPU (Rokid X-Craft, MediaTek)

```bash
# Create output directory
mkdir -p ava-ai-models-external/llm-mali/AVA-LL32-1B16

# Compile for Mali OpenCL
python3 -m mlc_llm compile \
  ava-ai-models-external/llm/AVA-LL32-1B16 \
  --model-type llama \
  --device opencl \
  --host aarch64-linux-android \
  --output ava-ai-models-external/llm-mali/AVA-LL32-1B16/AVA-LL32-1B16-mali.tar

# Copy model weights (same for all backends)
cp ava-ai-models-external/llm/AVA-LL32-1B16/*.bin \
   ava-ai-models-external/llm-mali/AVA-LL32-1B16/
cp ava-ai-models-external/llm/AVA-LL32-1B16/*.json \
   ava-ai-models-external/llm-mali/AVA-LL32-1B16/
cp ava-ai-models-external/llm/AVA-LL32-1B16/tokenizer* \
   ava-ai-models-external/llm-mali/AVA-LL32-1B16/
```

### iOS CoreML (iPhone, iPad)

```bash
# Create output directory
mkdir -p ava-ai-models-external/llm-coreml/AVA-LL32-1B16

# Compile for iOS Metal
python3 -m mlc_llm compile \
  ava-ai-models-external/llm/AVA-LL32-1B16 \
  --model-type llama \
  --host arm64-apple-ios \
  --output ava-ai-models-external/llm-coreml/AVA-LL32-1B16/AVA-LL32-1B16-ios.tar

# Copy model weights
cp ava-ai-models-external/llm/AVA-LL32-1B16/*.bin \
   ava-ai-models-external/llm-coreml/AVA-LL32-1B16/
cp ava-ai-models-external/llm/AVA-LL32-1B16/*.json \
   ava-ai-models-external/llm-coreml/AVA-LL32-1B16/
```

### AVA-GE2-2B16 Compilation

```bash
# Mali GPU
python3 -m mlc_llm compile \
  ava-ai-models-external/llm/AVA-GE2-2B16 \
  --model-type gemma2 \
  --device opencl \
  --host aarch64-linux-android \
  --output ava-ai-models-external/llm-mali/AVA-GE2-2B16/AVA-GE2-2B16-mali.tar

# iOS
python3 -m mlc_llm compile \
  ava-ai-models-external/llm/AVA-GE2-2B16 \
  --model-type gemma2 \
  --host arm64-apple-ios \
  --output ava-ai-models-external/llm-coreml/AVA-GE2-2B16/AVA-GE2-2B16-ios.tar
```

---

## Compiled Output Format

### .tar File Contents

The compiled `.tar` file contains TVM kernels:

```bash
tar -tf AVA-LL32-1B16-mali.tar
# lib0.o           # TVM library code
# llama_...devc.o  # Device-specific kernels
```

### Memory Requirements

| Model | Parameters | Runtime Memory | Best Devices |
|-------|-----------|----------------|--------------|
| AVA-LL32-1B16 | 1.24B | **1.37 GB** | HMT-1, all |
| AVA-GE2-2B16 | 2.6B | **1.52 GB** | Arc 3, Navigator, Vuzix |
| AVA-QW3-17B16 | 1.7B | **~0.9 GB** | 4GB+ devices |
| AVA-QW3-4B16 | 4B | **~2.5 GB** | 8GB+ devices |

---

## Device Compatibility Matrix

### Android Devices

| Device | GPU | RAM | Recommended Backend | Models |
|--------|-----|-----|---------------------|--------|
| RealWear HMT-1 | Adreno 506 | 2-3GB | Vulkan (llm/) | AVA-LL32-1B16 |
| RealWear Arc 3 | Adreno 619 | 4GB | Vulkan (llm/) | AVA-QW3-17B16 |
| RealWear Navigator 500/520 | Adreno 619 | 4GB | Vulkan (llm/) | AVA-QW3-17B16 |
| Vuzix M400 | Snapdragon | 6GB | Vulkan (llm/) | AVA-GE2-2B16 |
| Rokid X-Craft | Mali-G57 | 4GB | **OpenCL (llm-mali/)** | AVA-QW3-17B16 |
| Samsung Galaxy S23/S24 | Adreno 740/750 | 8-12GB | Vulkan (llm/) | AVA-QW3-4B16 |

### iOS Devices

| Device | Chip | RAM | Backend | Models |
|--------|------|-----|---------|--------|
| iPhone 15 Pro | A17 Pro | 8GB | **Metal (llm-coreml/)** | AVA-QW3-4B16 |
| iPhone 16 Pro | A18 Pro | 12GB | **Metal (llm-coreml/)** | AVA-QW3-4B16 |
| iPhone 15 | A16 | 6GB | Metal (llm-coreml/) | AVA-QW3-17B16 |

---

## Gemma 3n Support (NOW AVAILABLE)

### Current Status

Gemma 3n is Google's latest edge-optimized model - **now available via LiteRT**:

| Model | Parameters | Runtime Memory | Runtime | Status |
|-------|-----------|----------------|---------|--------|
| **AVA-GE3N-2B** | ~5B (2B eff) | **2GB** | LiteRT | **Available** |
| **AVA-GE3N-4B** | ~8B (4B eff) | **3GB** | LiteRT | **Available** |

### Supported Runtimes for Gemma 3n

1. **Google AI Edge SDK (LiteRT)** - **Recommended**
   - Models: `AVA-GE3N-2B`, `AVA-GE3N-4B`
   - Native LiteRT format with GPU delegate
   - Best performance on Android/iOS
   - Native Mali GPU support

2. **llama.cpp** - GGUF format (alternative)
   - Models: `bartowski/google_gemma-3n-E4B-it-GGUF`
   - Cross-platform support

### Device Recommendations for Gemma 3n

| Device | RAM | AVA-GE3N-2B | AVA-GE3N-4B | Recommended |
|--------|-----|-------------|-------------|-------------|
| RealWear HMT-1 | 2-3GB | Tight | No | AVA-LL32-1B16 |
| RealWear Arc 3 | 4GB | **Recommended** | Works | **AVA-GE3N-2B** |
| RealWear Navigator 500/520 | 4GB | **Recommended** | Works | **AVA-GE3N-2B** |
| Vuzix M400/M4000 | 6GB | Fast | **Optimal** | **AVA-GE3N-4B** |
| Vuzix Z100 | 6GB | Fast | **Optimal** | **AVA-GE3N-4B** |
| Rokid X-Craft | 4GB | **Recommended** | Works | **AVA-GE3N-2B** |
| Rokid Max Pro | 6GB | Fast | **Optimal** | **AVA-GE3N-4B** |
| Samsung S23/S24 | 8-12GB | Instant | **Optimal** | **AVA-GE3N-4B** |
| iPhone 15/16 Pro | 8-12GB | Instant | **Optimal** | **AVA-GE3N-4B** |

---

## Troubleshooting

### "Unknown model type: qwen3"

**Problem:** mlc-ai-nightly 0.15 doesn't support Qwen3 architecture.

**Solution:** Use pre-compiled Adreno/Vulkan models from HuggingFace:
```bash
# Pre-compiled models work on all Adreno devices
python3 << 'EOF'
from huggingface_hub import snapshot_download
snapshot_download(
    repo_id="mlc-ai/Qwen3-1.7B-q4f16_1-MLC",
    local_dir="ava-ai-models-external/llm/AVA-QW3-17B16"
)
EOF
```

### "UnsatisfiedLinkError: tvmFFIFunctionGetGlobal"

**Problem:** Runtime/model API mismatch.

**Solution:** Ensure using TVM 0.22.0 runtime with FFI API:
- Check `libtvm_runtime.so` is from custom build
- Verify `tvm4j_core.jar` uses single-argument `loadFromFile()`

### Mali Compilation Fails

**Problem:** OpenCL device not found on macOS.

**Solution:** The `--device opencl` flag is for target device, not host:
```bash
# Cross-compile on macOS for Mali target
python3 -m mlc_llm compile model/ \
  --device opencl \           # Target: Mali OpenCL
  --host aarch64-linux-android  # Target: Android ARM64
```

---

## Deployment Workflow

### 1. Choose Target Backend

```
Adreno GPU (most Android)  → Use pre-compiled from llm/
Mali GPU (Rokid)           → Compile to llm-mali/
Apple Metal (iOS)          → Compile to llm-coreml/
```

### 2. Deploy to Device

**Android (Adreno/Mali):**
```bash
adb shell mkdir -p /sdcard/ava-ai-models/
adb push llm-mali/AVA-LL32-1B16 /sdcard/ava-ai-models/
```

**iOS:**
- Include in Xcode project resources
- Or download via app at runtime

### 3. Verify Installation

```bash
# Check file sizes
adb shell du -sh /sdcard/ava-ai-models/*

# Expected:
# AVA-LL32-1B16: ~700MB
# AVA-GE2-2B16: ~1.4GB
```

---

## Quick Reference

### Compilation Commands

| Target | Command |
|--------|---------|
| Mali GPU | `python3 -m mlc_llm compile MODEL --device opencl --host aarch64-linux-android -o OUTPUT.tar` |
| iOS Metal | `python3 -m mlc_llm compile MODEL --host arm64-apple-ios -o OUTPUT.tar` |
| macOS Metal | `python3 -m mlc_llm compile MODEL --host arm64-apple-darwin -o OUTPUT.dylib` |

### Model Type Flags

| AVA Model | Compile Flag |
|-----------|--------------|
| AVA-LL32-* | `--model-type llama` |
| AVA-GE2-* | `--model-type gemma2` |
| AVA-GE3-* | `--model-type gemma3` |
| AVA-QW3-* | `--model-type qwen2` |
| AVA-MI7-* | `--model-type mistral` |
| AVA-PH3-* | `--model-type phi3` |

---

## Multi-Runtime Architecture (Future)

AVA's ALC engine supports multiple inference runtimes via the Strategy Pattern:

### Current Runtime: MLC-LLM (TVM)

| Component | File | Description |
|-----------|------|-------------|
| Runtime | `libtvm_runtime.adt` | TVM v0.22.0 FFI runtime (62MB) |
| JNI Bridge | `libtvm4j.adt` | Java/Kotlin interface (124KB) |
| Strategy | `MLCInferenceStrategy.kt` | Primary inference implementation |

### Runtime: llama.cpp (GGUF)

For models not supported by MLC-LLM (e.g., Gemma 3n):

| Component | File | Description |
|-----------|------|-------------|
| Runtime | `libllama.adg` | llama.cpp runtime (~15MB) |
| Strategy | `LlamaCppInferenceStrategy.kt` | GGUF model inference |
| Model Format | `.amg` | AMG container (GGUF format) |

**Supported Models:** All GGUF models including Gemma 3n, Qwen 3, etc.

### Google AI Edge (LiteRT) - NOW AVAILABLE

For optimal Google model performance:

| Component | File | Description |
|-----------|------|-------------|
| Runtime | `libmediapipe.adr` | Google AI Edge SDK (~8MB) |
| Strategy | `LiteRTInferenceStrategy.kt` | LiteRT model inference |
| Model Format | `.amr` | AMR container (LiteRT format) |

**Supported Models:** AVA-GE3N-2B, AVA-GE3N-4B (Gemma 3n), future Google edge models

### Runtime Selection Logic

```kotlin
fun selectRuntime(model: ModelConfig): IInferenceStrategy {
    return when (model.format) {
        ModelFormat.MLC -> MLCInferenceStrategy(tvmModule)
        ModelFormat.GGUF -> LlamaCppInferenceStrategy(llamaContext)
        ModelFormat.LITERT -> LiteRTInferenceStrategy(liteRTInference)
    }
}
```

### Native Library Directory

```
Universal/AVA/Apps/AVA-Standalone/src/main/jniLibs/arm64-v8a/
├── libtvm_runtime.adt      # TVM runtime (.adt = Ava Device TVM)
├── libtvm4j.adt            # TVM JNI bridge
├── AVALibrary.adm          # MLC-LLM runtime (.adm = Ava Device MLC)
├── libllama.adg            # llama.cpp (.adg = Ava Device GGUF)
└── libmediapipe.adr        # Google AI Edge (.adr = Ava Device liteRT)
```

---

## AVA 3.0 Model Protection

All production models should use `.ava3` encryption wrapper:

```bash
# Encode model for production (MLC format)
python -m ava_toolchain.ava3_encoder encode model.amm -o model.amm.ava3

# Encode GGUF model for production
python -m ava_toolchain.ava3_encoder encode model.amg -o model.amg.ava3

# Encode LiteRT model for production
python -m ava_toolchain.ava3_encoder encode model.amr -o model.amr.ava3

# Batch encode directory
python -m ava_toolchain.ava3_encoder encode models/ --batch -o encoded/

# Verify integrity
python -m ava_toolchain.ava3_encoder verify model.amm.ava3
```

**Security Features:**
- AES-256-CTR encryption
- XOR scramble + byte shuffle obfuscation
- SHA-256 integrity verification
- Block-based encoding (64KB blocks)

**See:** `DeveloperTools/docs/AVA3-ENCODING-SPEC.md`

---

## Related Documentation

- **Chapter 42:** [LLM Model Setup](Developer-Manual-Chapter42-LLM-Model-Setup.md)
- **Chapter 45:** [AVA LLM Naming Standard](Developer-Manual-Chapter45-AVA-LLM-Naming-Standard.md)
- **Device Specs:** [AVA-DEVICE-SPECS.json](AVA-DEVICE-SPECS.json)
- **Model Analysis:** [AVA-MODEL-ANALYSIS-REALWEAR.md](AVA-MODEL-ANALYSIS-REALWEAR.md)
- **Format Registry:** [FORMAT-REGISTRY.md](../DeveloperTools/FORMAT-REGISTRY.md)
- **AVA 3.0 Encoding:** [AVA3-ENCODING-SPEC.md](../DeveloperTools/docs/AVA3-ENCODING-SPEC.md)

---

**Document Version:** 2.0
**Last Updated:** 2025-12-01
