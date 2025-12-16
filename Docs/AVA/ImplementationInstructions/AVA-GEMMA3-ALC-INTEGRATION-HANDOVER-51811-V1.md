# Gemma 3 4B ALC Integration - Handover Document

**Date:** 2025-11-18
**Project:** AVA (Android Voice Assistant)
**Feature:** Add Gemma 3 4B model support to ALC (AVA's version of MLC-LLM)

---

## Executive Summary

AVA needs to support two LLM models for A/B testing:
1. **AVA-GEM-2B-Q4** - Existing Gemma 2B (English-focused, 1.2GB)
2. **AVA-GEM-4B-Q4** - New Gemma 3 4B (Multilingual 140+ languages, 2.3GB)

The application code for A/B testing model switching is **COMPLETE**. The blocker is that ALC (MLC-LLM) needs to be updated to support Gemma 3 model compilation for Android.

---

## What Has Been Completed

### 1. Model Downloaded
**Location:** `/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-MLC/`

**Source:** `mlc-ai/gemma-3-4b-it-q4bf16_1-MLC` from Hugging Face

**Size:** ~2.3GB (69 parameter shard files + tokenizer)

**Model Config:** `mlc-chat-config.json`
```json
{
  "model_type": "gemma3",
  "quantization": "q4bf16_1",
  "context_window_size": 8192,
  "vocab_size": 262208,
  "num_hidden_layers": 34,
  "hidden_size": 2560
}
```

### 2. A/B Testing Code Implemented

**ChatPreferences.kt** - Model selection preference
- Path: `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/ava/core/data/prefs/ChatPreferences.kt`
- Added: `selectedLLMModel` StateFlow, `getSelectedLLMModel()`, `setSelectedLLMModel()`, `isUsingGemma3()`

**ModelSelector.kt** - Model definitions
- Path: `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/ModelSelector.kt`
- Added: AVA-GEM-4B-Q4 ModelInfo with 140+ language support

**ChatViewModel.kt** - Dynamic LLM configuration
- Path: `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/ChatViewModel.kt`
- Updated: `initializeLLM()` to switch between models based on preference
- Config for Gemma 3: `modelPath = "models/AVA-GEM-4B-Q4"`, `modelLib = "gemma3_q4bf16_1_android.o"`, `maxMemoryMB = 3072`

### 3. Documentation Created

- **ADR-002:** `docs/architecture/decisions/ADR-002-Gemma3-Multilingual-LLM.md`
- **Dev Manual:** `docs/Developer-Manual-Chapter43-Intent-Learning-System.md`
- **User Manual:** Updated `docs/active/User-Manual.md`

### 4. Commits Made

- `58bf825` - feat(llm): add A/B testing support for Gemma 2B vs Gemma 3 4B models
- `f29d33e` - docs: add Intent Learning System chapter and update User Manual

---

## What Needs To Be Done

### The Core Problem

The MLC-LLM CLI cannot compile Gemma 3 models because:

1. **Pip-installed version** (`/Users/manoj_mbpm14/Library/Python/3.9/lib/python/site-packages/mlc_llm/`) - Old version, no gemma3 support
2. **Local source** (`/Volumes/M-Drive/Coding/AVA/external/mlc-llm/`) - Has gemma3 files but not registered in CLI

**Error when compiling:**
```
ValueError: Unknown model type: gemma3. Available ones: ['llama', 'mistral', 'gemma', 'gemma2', ...]
```

### Solution Options

#### Option A: Update MLC-LLM CLI Model Registry (Recommended)

Register gemma3 in the CLI's model list:

1. **File to modify:** `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/python/mlc_llm/model/__init__.py`
   - Add: `from .gemma3 import *` or register in MODELS dict

2. **File to modify:** `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/python/mlc_llm/cli/compile.py`
   - Add `gemma3` to model-type choices (line 74)

3. **Check quantization:** `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/python/mlc_llm/quantization/__init__.py`
   - Ensure `q4bf16_1` is registered

4. **Rebuild/reinstall local MLC-LLM**

#### Option B: Use Newer MLC-LLM Release

Find and install a newer MLC-LLM version that already has Gemma 3 support:
- Check: https://github.com/mlc-ai/mlc-llm/releases
- Look for releases after Gemma 3 was added (late 2024/early 2025)

#### Option C: Use Pre-compiled Binary

If available, download pre-compiled Android binary for Gemma 3 4B similar to how Gemma 2B was obtained:
- Existing: `/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/`

---

## Key Files Reference

### MLC-LLM/ALC Source Structure

```
/Volumes/M-Drive/Coding/AVA/external/mlc-llm/
├── python/mlc_llm/
│   ├── model/
│   │   ├── gemma/           # Gemma 1 support
│   │   ├── gemma2/          # Gemma 2 support
│   │   └── gemma3/          # Gemma 3 support (EXISTS but not registered)
│   │       ├── __init__.py          # Empty!
│   │       ├── gemma3_loader.py     # Model loader
│   │       ├── gemma3_model.py      # Model architecture (27KB)
│   │       └── gemma3_quantization.py
│   ├── cli/
│   │   └── compile.py       # CLI entry point
│   ├── quantization/        # Quantization methods
│   └── support/
│       └── auto_config.py   # Model type detection
└── 3rdparty/tvm/            # TVM runtime
```

### Existing Compiled Model (Reference)

```
/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/
├── gemma-2b-it-q4f16_1-android.tar   # 318KB - Android compiled model
│   ├── lib0.o                        # Main library
│   └── gemma_q4f16_1_devc.o          # Model-specific library
└── *.wasm                            # WebGPU versions (not needed)
```

### Target Output

After compilation, need:
```
gemma-3-4b-it-q4bf16_1-android.tar
├── lib0.o
└── gemma3_q4bf16_1_android.o    # This is what ChatViewModel expects
```

---

## Compile Command (Once CLI is Fixed)

```bash
cd /Volumes/M-Drive/Coding/AVA/external/mlc-llm

python3 -m mlc_llm compile \
    /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-MLC/mlc-chat-config.json \
    --device android \
    -o /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-android.tar
```

---

## After Compilation

1. **Extract the tar:**
   ```bash
   tar -xf gemma-3-4b-it-q4bf16_1-android.tar -C output/
   ```

2. **Copy to app jniLibs:**
   ```bash
   cp output/*.o /Volumes/M-Drive/Coding/AVA/apps/ava-standalone/src/main/jniLibs/arm64-v8a/
   ```

3. **Copy model weights to assets:**
   ```bash
   cp -r /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-MLC \
       /Volumes/M-Drive/Coding/AVA/apps/ava-standalone/src/main/assets/models/AVA-GEM-4B-Q4/
   ```

4. **Test model loading** in app

---

## TVM Runtime Consideration

Your project has custom TVM/FFI bindings:
- `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/TVMRuntime.kt`
- `Universal/AVA/Features/LLM/libs/tvm4j_core.jar`

Ensure these are compatible with the Gemma 3 model architecture (may need updates for new ops).

---

## Testing Plan

1. Build app with both models configured
2. Set preference to AVA-GEM-2B-Q4, verify English works
3. Set preference to AVA-GEM-4B-Q4, verify multilingual works
4. Compare response quality and latency
5. Test memory usage on device

---

## Success Criteria

- [ ] MLC-LLM CLI compiles Gemma 3 4B for Android without errors
- [ ] Compiled .o files are under 500KB (similar to Gemma 2B)
- [ ] Model loads on device without crashes
- [ ] Inference produces coherent responses in multiple languages
- [ ] A/B switching works via preferences

---

## Questions for Implementation

1. Should ALC be branded as a distinct fork or remain as patched MLC-LLM?
2. Is there a CI/CD pipeline for model compilation that needs updating?
3. Are there device memory constraints beyond the 3GB budget?
4. Which languages should be prioritized for testing (Hindi, Japanese, Arabic)?

---

## Contact

**Project Owner:** Manoj Jhawar (manoj@ideahq.net)
**Repository:** /Volumes/M-Drive/Coding/AVA
**Branch:** development

---

*Generated: 2025-11-18 | IDEACODE v8.4*
