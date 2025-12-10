# Developer Manual - Chapter 44: AVA Naming Convention v3

**Date:** 2025-12-01
**Version:** 3.0
**Status:** Active

---

## Overview

This chapter documents the AVA Naming Convention v3, which introduces the **3-character extension scheme** for all AVA model files. This scheme uses the format `A` + `Type` + `Tech` to create memorable, consistent extensions.

**Key Features:**
- 3-character extension scheme: `.amm`, `.amg`, `.amr`, `.adm`, `.ats`, `.ath`
- Standardized naming format: `AVA-{CODE}-{SIZE}{BITS}`
- Clear model family identification
- Runtime compatibility indicators
- Backward compatibility with legacy extensions

---

## Table of Contents

1. [File Extensions](#file-extensions)
2. [Naming Format](#naming-format)
3. [Model Family Codes](#model-family-codes)
4. [Quantization Suffixes](#quantization-suffixes)
5. [Complete Examples](#complete-examples)
6. [Migration Guide](#migration-guide)
7. [Compatibility Notes](#compatibility-notes)

---

## 3-Character Extension Scheme v2.0

### Scheme Format: `A` + `Type` + `Tech`

| Position | Meaning | Examples |
|----------|---------|----------|
| 1st char | `A` = Ava (always) | `a` |
| 2nd char | Type indicator | `m` = Model, `d` = Device, `t` = Tokenizer |
| 3rd char | Technology/Format | `m` = MLC, `g` = GGUF, `r` = liteRT, `s` = SentencePiece |

### Model Archives (.amX)

| Extension | Full Name | Runtime | Replaces |
|-----------|-----------|---------|----------|
| `.amm` | Ava Model MLC | MLC-LLM (TVM) | `.ALM` |
| `.amg` | Ava Model GGUF | llama.cpp | - |
| `.amr` | Ava Model liteRT | Google AI Edge | - |

### Device Libraries (.adX)

| Extension | Full Name | Description | Replaces |
|-----------|-----------|-------------|----------|
| `.adm` | Ava Device MLC | MLC-LLM object code (ELF .o) | `.ADco` |
| `.ads` | Ava Device Shared | Compiled shared library (.so) | `.so` |
| `.adt` | Ava Device TVM | TVM runtime library | `.o` |
| `.adg` | Ava Device GGUF | GGUF native library | - |
| `.adr` | Ava Device liteRT | LiteRT delegate | - |

### Tokenizers (.atX)

| Extension | Full Name | Description | Replaces |
|-----------|-----------|-------------|----------|
| `.ats` | Ava Tokenizer SentencePiece | SentencePiece model | `tokenizer.model` |
| `.ath` | Ava Tokenizer HuggingFace | HuggingFace tokenizer | `tokenizer.json` |
| `.atv` | Ava Tokenizer Vocabulary | Vocabulary file | - |

### Configuration Files (.acX)

| Extension | Full Name | Description | Replaces |
|-----------|-----------|-------------|----------|
| `.amc` | Ava Model Config | Model configuration JSON | `mlc-chat-config.json` |
| `.apt` | Ava Prompt Template | Prompt/chat template | `template` |
| `.aci` | Ava Cache Index | Weight shard mapping | `ndarray-cache.json` |

**Note:** For TVM/MLC-LLM models, internal files keep original names for compatibility. For GGUF models, use AVA extensions.

### NLU Models (.aon)

| Extension | Full Name | Description |
|-----------|-----------|-------------|
| `.aon` | Ava Optimized NLU | ONNX embedding models |

### Legacy Extension Mapping

| Legacy | New | Notes |
|--------|-----|-------|
| `.ADco` | `.adm` | Device MLC library |
| `.ALM` | `.amm` | MLC model archive |
| `.AON` | `.aon` | NLU models (unchanged) |
| `tokenizer.model` | `tokenizer.ats` | SentencePiece |
| `tokenizer.json` | `tokenizer.ath` | HuggingFace |

### Extension Usage

#### .amm (Ava Model MLC)
Complete MLC-LLM model package for TVM runtime.

```
AVA-GE3-4B16.amm         # Complete Gemma 3 4B package
```

**Contents:**
```
AVA-GE3-4B16.amm/
├── AVALibrary.adm              # MLC runtime library
├── AVA-GE3-4B16.adm            # Model device code
├── mlc-chat-config.json        # Model configuration
├── ndarray-cache.json          # Weight shard mapping
├── tokenizer.ats               # SentencePiece tokenizer
├── tokenizer_config.json       # Tokenizer settings
└── params_shard_*.bin          # Model weight files
```

#### .amg (Ava Model GGUF)
GGUF format model for llama.cpp runtime.

```
AVA-QW3-4B16.amg         # Qwen3 4B GGUF package
```

#### .amr (Ava Model liteRT)
LiteRT format model for Google AI Edge runtime.

```
AVA-GE3N-E4B16.amr       # Gemma 3n E4B LiteRT package
```

#### .adm (Ava Device MLC)
MLC-LLM compiled object code (ELF relocatable `.o` files).

```
AVALibrary.adm            # Core MLC library object code
AVA-GE3-4B16.adm          # Gemma 3 4B model object code
```

**Technical Details:**
- ELF relocatable object files (e_type: ET_REL)
- Must be linked into `.ads` shared library before loading
- Built with TVM v0.22.0 compiler
- Uses new FFI API: `TVMFFIFunctionCall`
- Android ARM64 target with OpenCL acceleration
- Typically 50-200MB per file

#### .ads (Ava Device Shared)
Compiled shared library ready for dynamic loading.

```
AVA-GE3-4B16.ads          # Linked shared library (loadable)
```

**Technical Details:**
- ELF shared object (e_type: ET_DYN)
- Created by linking `.adm` object files
- Directly loadable via `dlopen()` / `System.loadLibrary()`
- Requires TVM runtime symbols (loads after libtvm4j_runtime_packed.so)
- Build command: `$NDK_CC -shared -o model.ads *.adm -llog`

**Conversion:**
```bash
# Link .adm object files to .ads shared library
$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android24-clang \
    -shared -o AVA-GE3-4B16.ads AVALibrary.adm AVA-GE3-4B16.adm -llog
```

#### .ats (Ava Tokenizer SentencePiece)
SentencePiece tokenizer model.

```
tokenizer.ats             # SentencePiece model file
```

#### .aon (Ava Optimized NLU)
ONNX format models for embeddings and NLU.

```
AVA-384-Base-INT8.aon     # 384-dim BERT embedding model
```

**Technical Details:**
- Standard ONNX format
- INT8 quantization for mobile efficiency
- Used for NLU intent classification
- Typically 10-50MB

---

## Naming Format

### Standard Format

```
AVA-{CODE}-{SIZE}{BITS}
```

### Component Breakdown

| Component | Description | Example |
|-----------|-------------|---------|
| `AVA-` | Prefix (required) | `AVA-` |
| `{CODE}` | Model family code (2-3 chars) | `GE3`, `LL2`, `MST` |
| `{SIZE}` | Parameter size | `2B`, `4B`, `7B`, `13B` |
| `{BITS}` | Quantization bits | `16`, `8`, `4` |

### Size Notation

| Notation | Meaning | Example |
|----------|---------|---------|
| `2B` | 2 billion parameters | 2,000,000,000 |
| `4B` | 4 billion parameters | 4,000,000,000 |
| `7B` | 7 billion parameters | 7,000,000,000 |
| `13B` | 13 billion parameters | 13,000,000,000 |
| `70B` | 70 billion parameters | 70,000,000,000 |

### Quantization Notation

| Bits | Meaning | Precision |
|------|---------|-----------|
| `16` | q4f16_1 or q4bf16_1 | 4-bit weights, 16-bit activations |
| `8` | INT8 | 8-bit integer quantization |
| `4` | q4 | 4-bit quantization |

---

## Model Family Codes

### LLM Models

| Model Family | Code | Versions | Example |
|-------------|------|----------|---------|
| Gemma 2 | `GE2` | 2B, 9B, 27B | `AVA-GE2-2B16` |
| Gemma 3 | `GE3` | 2B, 4B, 9B, 27B | `AVA-GE3-4B16` |
| Gemma 4 Nano | `G4N` | 2B, 4B | `AVA-G4N-4B16` |
| Llama 2 | `LL2` | 7B, 13B, 70B | `AVA-LL2-7B16` |
| Llama 3 | `LL3` | 8B, 70B | `AVA-LL3-8B16` |
| Mistral | `MST` | 7B, 8x7B | `AVA-MST-7B16` |
| Phi-2 | `PH2` | 2.7B | `AVA-PH2-3B16` |
| Phi-3 | `PH3` | 3.8B, 7B, 14B | `AVA-PH3-4B16` |

### Embedding Models

| Model Type | Code | Dimensions | Example |
|-----------|------|------------|---------|
| BERT Base | `Base` | 384, 768 | `AVA-384-Base-INT8.AON` |
| MiniLM | `Mini` | 384 | `AVA-384-Mini-INT8.AON` |
| E5 Small | `E5S` | 384 | `AVA-384-E5S-INT8.AON` |

---

## Quantization Suffixes

### LLM Quantization

| Suffix | Format | Size | Quality | Use Case |
|--------|--------|------|---------|----------|
| `16` | q4f16_1 | ~1.2GB (2B) | High | General use |
| `16` | q4bf16_1 | ~2.3GB (4B) | High | Better accuracy |
| `8` | q8f16_1 | ~3.5GB (4B) | Highest | Best quality |
| `4` | q4 | ~800MB (2B) | Good | Low memory |

### Embedding Quantization

| Suffix | Format | Size | Quality | Use Case |
|--------|--------|------|---------|----------|
| `INT8` | INT8 | ~22MB | High | Mobile optimized |
| `FP16` | Float16 | ~44MB | Highest | Desktop/server |

---

## Complete Examples

### LLM Model Examples

#### Gemma 2 2B (Existing)
```
Folder: AVA-GE2-2B16/
Files:
  - AVALibrary.ADco           # 51MB
  - AVA-GE2-2B16.ADco        # 83MB
  - mlc-chat-config.json
  - ndarray-cache.json
  - tokenizer.model
  - params_shard_*.bin        # ~1.2GB total
```

#### Gemma 3 4B (New)
```
Folder: AVA-GE3-4B16/
Files:
  - AVALibrary.ADco           # 51MB
  - AVA-GE3-4B16.ADco        # 163MB
  - mlc-chat-config.json
  - ndarray-cache.json
  - tokenizer.model
  - params_shard_*.bin        # ~2.3GB total
```

### Embedding Model Examples

#### BERT 384-dimensional
```
File: AVA-384-Base-INT8.AON    # 22MB
Used by: NLU intent classification
Location: /sdcard/Android/data/com.augmentalis.ava/files/models/
```

### Archive Format

When distributing models:
```
AVA-GE3-4B16.ALM              # Complete package
AVA-384-Base-INT8.AON         # Embedding model
```

---

## Migration Guide

### From v2 (Legacy) to v3 (3-Character Scheme)

#### Device Libraries

| v2 (Legacy) | v3 (Current) | Notes |
|-------------|--------------|-------|
| `AVALibrary.ADco` | `AVALibrary.adm` | MLC library |
| `AVA-GE2-2B16.ADco` | `AVA-GE2-2B16.adm` | Model device code |

#### Model Archives

| v2 (Legacy) | v3 (Current) | Notes |
|-------------|--------------|-------|
| `AVA-GE2-2B16.ALM` | `AVA-GE2-2B16.amm` | MLC archive |

#### Tokenizers

| v2 (Legacy) | v3 (Current) | Notes |
|-------------|--------------|-------|
| `tokenizer.model` | `tokenizer.ats` | SentencePiece |
| `tokenizer.json` | `tokenizer.ath` | HuggingFace |

### Code Migration

Update all references:

**Before (v2):**
```kotlin
val deviceCode = "$modelDir/AVALibrary.ADco"
val tokenizer = "$modelDir/tokenizer.model"
val archive = "$modelsDir/AVA-GE2-2B16.ALM"
```

**After (v3):**
```kotlin
val deviceCode = "$modelDir/AVALibrary.adm"
val tokenizer = "$modelDir/tokenizer.ats"
val archive = "$modelsDir/AVA-GE2-2B16.amm"
```

### File System Migration

```bash
# Rename device code files
cd AVA-GE2-2B16/
mv AVALibrary.ADco AVALibrary.adm
mv AVA-GE2-2B16.ADco AVA-GE2-2B16.adm
mv tokenizer.model tokenizer.ats

# Rename model archive
mv AVA-GE2-2B16.ALM AVA-GE2-2B16.amm
```

### mlc-chat-config.json Update

Update tokenizer reference in config:

```json
"tokenizer_files": [
    "tokenizer.ats",    // was: tokenizer.model
    "tokenizer.json",
    "tokenizer_config.json"
]
```

---

## Compatibility Notes

### TVM API Versions

**Critical:** AVA models use TVM v0.22.0 with new FFI API.

| Aspect | MLC-LLM Models | AVA Models |
|--------|----------------|------------|
| TVM Version | Pre-0.20 | v0.22.0 |
| API Type | Old C API | New FFI API |
| API Symbols | `TVMFuncCall` | `TVMFFIFunctionCall` |
| Runtime | `libtvm4j_runtime_packed.so` | `libtvm_runtime.so` + `libtvm4j.so` |
| Compatible With | MLC Chat App | AVA only |

### Runtime Requirements

**AVA's Custom TVM Build:**
```
apps/ava-standalone/src/main/jniLibs/arm64-v8a/
├── libtvm_runtime.so    # 62MB - TVM v0.22.0 runtime
└── libtvm4j.so          # 124KB - JNI bridge
```

**Build Location:** `~/Downloads/Coding/tvm-build/build-android/`

### Error Messages

If you see these errors, you have a version mismatch:

```
UnsatisfiedLinkError: tvmFFIFunctionGetGlobal
```
→ Model compiled with old TVM, runtime uses new FFI

```
Symbol not found: TVMFuncCall
```
→ Model compiled with new TVM, runtime uses old API

**Solution:** Recompile model with matching TVM version.

---

## File Locations

### Development
```
/Volumes/M-Drive/Coding/AVA/
├── Universal/AVA/Features/LLM/libs/
│   └── tvm4j_core.jar              # Java FFI bindings
└── apps/ava-standalone/src/main/
    ├── assets/models/
    │   └── AVA-384-Base-INT8.AON   # Bundled embedding
    └── jniLibs/arm64-v8a/
        ├── libtvm_runtime.so       # TVM runtime (62MB)
        └── libtvm4j.so             # JNI bridge (124KB)
```

### Local Model Storage
```
~/Downloads/Coding/AVA-LLM/models/
├── AVA-GE2-2B16/                   # Gemma 2 2B (MLC)
│   ├── AVALibrary.adm (51MB)
│   ├── AVA-GE2-2B16.adm (83MB)
│   ├── tokenizer.ats
│   └── params_shard_*.bin
└── AVA-GE3-4B16/                   # Gemma 3 4B (MLC)
    ├── AVALibrary.adm (51MB)
    ├── AVA-GE3-4B16.adm (163MB)
    ├── mlc-chat-config.json
    ├── tokenizer.ats
    └── params_shard_*.bin (69 files)
```

### Device (Runtime)
```
/sdcard/ava-ai-models/llm/
├── AVA-GE2-2B16/                   # Gemma 2 2B (MLC)
│   ├── AVALibrary.adm
│   ├── AVA-GE2-2B16.adm
│   ├── tokenizer.ats
│   └── params_shard_*.bin
├── AVA-GE3-4B16/                   # Gemma 3 4B (MLC)
│   ├── AVALibrary.adm
│   ├── AVA-GE3-4B16.adm
│   ├── mlc-chat-config.json
│   ├── tokenizer.ats
│   └── params_shard_*.bin
└── AVA-QW3-4B16/                   # Qwen3 4B (GGUF)
    └── AVA-QW3-4B16.amg
```

**Note:** NLU embedding (AVA-384-Base-INT8.aon) is bundled in APK, stored at app-specific location managed by Android.

---

## Reference Documentation

- [Chapter 42: LLM Model Setup](Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [Chapter 38: LLM Model Management](Developer-Manual-Chapter38-LLM-Model-Management.md)
- [Gemma Model Compilation Instructions](instructions/GEMMA-MODEL-COMPILATION-INSTRUCTIONS.md)
- [Spec 011: Automated Model Compilation Pipeline](../.ideacode/specs/011-automated-model-compilation-pipeline/spec.md)

---

## Summary

AVA Naming Convention v3 provides:

| Feature | Description |
|---------|-------------|
| 3-Character Extensions | `.amm`, `.amg`, `.amr`, `.adm`, `.ats`, `.ath` |
| Clear Identification | Know model family, size, runtime at a glance |
| Multi-Runtime Support | MLC-LLM, llama.cpp, Google AI Edge |
| Backward Compatibility | Legacy extensions still recognized |
| Migration Tools | Clear mapping from v2 to v3 |

**Extension Quick Reference:**

| Type | Extensions |
|------|------------|
| Models | `.amm` (MLC), `.amg` (GGUF), `.amr` (LiteRT) |
| Device | `.adm` (MLC), `.ads` (Shared), `.adt` (TVM), `.adg` (GGUF), `.adr` (LiteRT) |
| Tokenizers | `.ats` (SentencePiece), `.ath` (HuggingFace), `.atv` (Vocab) |
| Config | `.amc` (Model Config), `.apt` (Prompt Template), `.aci` (Cache Index) |
| NLU | `.aon` (ONNX embeddings) |

**Adoption Status:** Active (as of 2025-12-01)

**Related Chapters:**
- Chapter 45: AVA LLM Naming Standard
- Chapter 54: Cross-GPU Model Compilation
- Chapter 62: Toolchain Build System

---

**Document Version:** 3.0
**Last Updated:** 2025-12-01
**Author:** AVA Development Team
