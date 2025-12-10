# AVA External Model Files

**Author:** Manoj Jhawar
**Updated:** 2025-12-04

---

## Model Types Overview

| Type | Runtime | File Format | Extension |
|------|---------|-------------|-----------|
| TVM/MLC-LLM | TVMRuntime | TVM compiled code | `.adm`, `.ads` |
| GGUF | GGUFInferenceStrategy | llama.cpp format | `.gguf` |
| MLC Weights | TVMRuntime | SafeTensor shards | `.bin` |

---

## LLM Models

### AVA-GE2-2B16 (Gemma 2 - 2B, INT16)

**Type:** TVM/MLC-LLM compiled
**Status:** TVM v0.22.0 compatible (.ads built)

| File | Type | Size | Required |
|------|------|------|----------|
| `AVA-GE2-2B16.ads` | Compiled shared library | 3.6MB | Yes |
| `AVA-GE2-2B16.adm` | TVM object code | 2.8MB | No (source) |
| `AVALibrary.adm` | TVM library code | 1.0MB | No (source) |
| `tokenizer.ats` | AVA tokenizer | 4.0MB | Yes |
| `tokenizer.json` | HuggingFace tokenizer | 17MB | Yes |
| `mlc-chat-config.json` | Model config | 2KB | Yes |
| `ndarray-cache.json` | Weight index | 128KB | Yes |
| `params_shard_*.bin` | Model weights | ~42 files | Yes |

---

### AVA-GE3-4B16 (Gemma 3 - 4B, INT16)

**Type:** TVM/MLC-LLM compiled
**Status:** TVM v0.22.0 compatible (.ads built)

| File | Type | Size | Required |
|------|------|------|----------|
| `AVA-GE3-4B16.ads` | Compiled shared library | 5.8MB | Yes |
| `AVA-GE3-4B16.adm` | TVM object code | 4.6MB | No (source) |
| `AVALibrary.adm` | TVM library code | 1.6MB | No (source) |
| `tokenizer.ats` | AVA tokenizer | 4.5MB | Yes |
| `tokenizer.json` | HuggingFace tokenizer | 33MB | Yes |
| `mlc-chat-config.json` | Model config | 2KB | Yes |
| `ndarray-cache.json` | Weight index | 257KB | Yes |
| `params_shard_*.bin` | Model weights | ~69 files | Yes |

---

### AVA-GE3N-E4B16 (Gemma 3N Edge - 4B, GGUF)

**Type:** GGUF (llama.cpp)
**Status:** Ready for GGUFInferenceStrategy

| File | Type | Size | Required |
|------|------|------|----------|
| `gemma-3n-E4B-it-Q4_K_M.gguf` | GGUF model | 4.2GB | Yes |
| `mlc-chat-config.json` | Model config | 1KB | Yes |
| `template` | Prompt template | 358B | Optional |

**Note:** GGUF models don't require TVM compilation. They use llama.cpp directly.

---

### AVA-LL32-1B16 (LLaMA 3.2 - 1B, INT16)

**Type:** MLC-LLM weights only
**Status:** Missing TVM code (.adm files)

| File | Type | Required |
|------|------|----------|
| `params_shard_*.bin` | Model weights (~22 files) | Yes |
| Missing: | `.adm`, `.ads`, config files | Needs compilation |

---

### AVA-LL32-3B16 (LLaMA 3.2 - 3B, INT16)

**Type:** MLC-LLM weights only
**Status:** Missing TVM code (.adm files)

| File | Type | Required |
|------|------|----------|
| `mlc-chat-config.json` | Model config | Yes |
| `tokenizer.json` | HuggingFace tokenizer | Yes |
| `params_shard_*.bin` | Model weights (~58 files) | Yes |
| Missing: | `.adm`, `.ads` | Needs compilation |

---

### AVA-QW3-06B16 (Qwen 2.5 - 0.6B, INT16)

**Type:** MLC-LLM weights only
**Status:** Missing TVM code (.adm files)

| File | Type | Required |
|------|------|----------|
| `mlc-chat-config.json` | Model config | Yes |
| `tokenizer.json` | HuggingFace tokenizer | Yes |
| `params_shard_*.bin` | Model weights (~9 files) | Yes |
| Missing: | `.adm`, `.ads` | Needs compilation |

---

### AVA-QW3-17B16 (Qwen 2.5 - 1.7B, INT16)

**Type:** MLC-LLM weights only
**Status:** Missing TVM code (.adm files)

| File | Type | Required |
|------|------|----------|
| `mlc-chat-config.json` | Model config | Yes |
| `tokenizer.json` | HuggingFace tokenizer | Yes |
| `vocab.json` | Vocabulary | Yes |
| `params_shard_*.bin` | Model weights (~30 files) | Yes |
| Missing: | `.adm`, `.ads` | Needs compilation |

---

### AVA-QW3-4B16 (Qwen 2.5 - 4B, INT16)

**Type:** MLC-LLM weights only
**Status:** Missing TVM code (.adm files)

| File | Type | Required |
|------|------|----------|
| `mlc-chat-config.json` | Model config | Yes |
| `tokenizer.json` | HuggingFace tokenizer | Yes |
| `vocab.json` | Vocabulary | Yes |
| `params_shard_*.bin` | Model weights (~74 files) | Yes |
| Missing: | `.adm`, `.ads` | Needs compilation |

---

## Embedding Models (NLU)

Located in: `external-models/embeddings/`

### AVA-384-Base-INT8

**Type:** ONNX quantized
**Purpose:** Intent classification embeddings

| File | Type | Required |
|------|------|----------|
| `AVA-384-Base-INT8.AON` | AVA ONNX model archive | Yes |

---

### AVA-384-Multi-INT8

**Type:** ONNX quantized
**Purpose:** Multilingual embeddings

| File | Type | Required |
|------|------|----------|
| `AVA-384-Multi-INT8.AON` | AVA ONNX model archive | Yes |

---

### AVA-768-Multi-INT8

**Type:** ONNX quantized
**Purpose:** High-dimensional multilingual embeddings

| File | Type | Required |
|------|------|----------|
| `AVA-768-Multi-INT8.AON` | AVA ONNX model archive | Yes |

---

## TVM v0.22.0 Binaries

Located in: `external-models/tvm-v0220-binaries/`

| File | Purpose |
|------|---------|
| `libtvm4j_runtime_packed.so` | Android arm64-v8a TVM runtime |
| `tvm4j_core.jar` | Java bindings for TVM |

---

## Required Files Per Model Type

### TVM/MLC-LLM Model (Complete)

```
{model}/
├── {model}.ads          # Compiled shared library (REQUIRED)
├── tokenizer.ats        # AVA tokenizer (REQUIRED)
├── tokenizer.json       # HuggingFace tokenizer (REQUIRED)
├── mlc-chat-config.json # Model config (REQUIRED)
├── ndarray-cache.json   # Weight index (REQUIRED)
├── params_shard_*.bin   # Model weights (REQUIRED)
├── {model}.adm          # TVM object code (BUILD SOURCE)
└── AVALibrary.adm       # TVM library (BUILD SOURCE)
```

### GGUF Model

```
{model}/
├── {model}.gguf         # GGUF model (REQUIRED)
├── mlc-chat-config.json # Model config (REQUIRED)
└── template             # Prompt template (OPTIONAL)
```

---

## Build Commands

### Build .ads from .adm files

```bash
cd tools/tvm-compat-shim
./build-model.sh /path/to/model /path/to/model/{model}.ads
```

### Requirements

- Android NDK 25.2.9519653
- aarch64-linux-android24-clang
- .adm files present in model directory

---

## Encryption Status

| Model Type | Encryption Required | Status |
|------------|---------------------|--------|
| LLM weights (.bin) | AES-256-GCM | Not implemented |
| TVM code (.adm) | AES-256-GCM | Not implemented |
| Shared lib (.ads) | AES-256-GCM | Not implemented |
| Tokenizer (.ats) | AES-256-GCM | Not implemented |
| Config (.json) | None | Plain text |

**Note:** Encryption is planned for production release. Current files are unencrypted for development.

---

## File Extensions Reference

| Extension | Full Name | Description |
|-----------|-----------|-------------|
| `.adm` | Ava Device MLC | TVM object code (ELF .o) |
| `.ads` | Ava Device Shared | Compiled shared library |
| `.ats` | Ava Tokenizer Serialized | Tokenizer data |
| `.alm` | Ava Language Model | Model archive (tar) |
| `.aon` | Ava ONNX | ONNX model archive |
| `.gguf` | GGML Universal Format | llama.cpp format |
