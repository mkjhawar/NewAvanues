# Gemma 3 4B Model Compilation Instructions for TVM v0.22.0

**Created:** 2025-11-18
**Updated:** 2025-11-19
**Purpose:** Instructions for compiling Gemma 3 4B model compatible with AVA's TVM v0.22.0 runtime
**Coordination:** Another Claude instance is updating ALC (AVA Local Compute) to use this model
**AVA Model Name:** AVA-GE3-4B16

---

## AVA Naming Convention v2

### File Extensions

| Extension | Description | Replaces |
|-----------|-------------|----------|
| `.ADco` | AVA Device Code (compiled object) | `.o` |
| `.ALM` | AVA LLM Model (compiled model package) | `.tar` |
| `.AON` | AVA ONNX Model | `.onnx` |

### Model Naming Format: `AVA-{MODEL}{VERSION}-{SIZE}{BITS}`

**Examples:**
- `AVA-GE3-4B16` = Gemma 3, 4B parameters, q4bf16_1
- `AVA-GE2-2B16` = Gemma 2, 2B parameters, q4f16_1
- `AVA-G4N-4B16` = Gemma 4N, 4B parameters, q4bf16_1

### File Naming Examples

| Old Name | New Name | Description |
|----------|----------|-------------|
| `gemma3_q4bf16_1_devc.o` | `AVA-GE3-4B16.ADco` | Device code |
| `lib0.o` | `AVALibrary.ADco` | Library code |
| `model-android.tar` | `AVA-GE3-4B16.ALM` | Compiled model |
| `AVA-ONX-384-BASE.onnx` | `AVA-384-Base.AON` | ONNX embedding model |

**The AVA prefix indicates:**
- Retokenized and optimized for ALC (AVA Local Compute)
- NOT compatible with standard MLC-LLM
- Uses TVM v0.22.0 with new FFI API (TVMFFIFunctionCall, not TVMFuncCall)
- Ready for on-device inference

### Model Codes
| Model Family | Code | Example |
|-------------|------|---------|
| Gemma 2 | GE2 | AVA-GE2-2B16 |
| Gemma 3 | GE3 | AVA-GE3-4B16 |
| Gemma 4N | G4N | AVA-G4N-4B16 |
| Llama 2 | LL2 | AVA-LL2-7B16 |
| Mistral | MST | AVA-MST-7B16 |
| Phi | PHI | AVA-PHI-3B16 |

### Quantization Suffix
- `16` = q4f16_1 or q4bf16_1 (4-bit quantization with bf16/f16)

---

## CRITICAL: TVM Version Compatibility

**You MUST compile with TVM v0.22.0** to match AVA's runtime. The compiled `.o` files must use the **new FFI API** symbols, NOT the old C API.

---

## Background: Why Recompilation is Needed

### The Problem
AVA's pre-compiled Gemma models (from March 2024) use the **old TVM C API**:
- `TVMFuncCall`
- `TVMAPISetLastError`
- `TVMBackendRegisterSystemLibSymbol`

But TVM v0.22.0 uses the **new FFI API**:
- `TVMFFIFunctionCall`
- `TVMFFIErrorSetRaised`
- `TVMFFIEnvModRegisterContextSymbol`

**Result:** Models won't load - symbol mismatch errors.

### The Solution
Recompile Gemma models using TVM v0.22.0 / MLC-LLM that generates compatible `.o` files.

---

## Your Task

### Step 0: Fix MLC-LLM CLI to Recognize Gemma 3

The MLC-LLM CLI doesn't recognize "gemma3" model type. You need to fix this first.

**Location:** `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/`

**Files to modify:**

1. **Register gemma3 in model __init__.py:**
   `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/python/mlc_llm/model/__init__.py`
   - Add: `from .gemma3 import *` or register in MODELS dict

2. **Add to CLI compile.py:**
   `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/python/mlc_llm/cli/compile.py` (line 74)
   - Add `gemma3` to model-type choices

3. **Check quantization support:**
   `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/python/mlc_llm/quantization/__init__.py`
   - Ensure `q4bf16_1` is registered

4. **Populate gemma3/__init__.py:**
   The file at `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/python/mlc_llm/model/gemma3/__init__.py` is EMPTY!
   - Export the necessary classes from `gemma3_model.py` and `gemma3_loader.py`

### Step 1: Compile Gemma 3 4B for Android ARM64

**Model Already Downloaded:**
```
/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-MLC/
```

**Model Config (from mlc-chat-config.json):**
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

**AVA Naming:** AVA-GE3-4B16
**Output File:** AVA-GE3-4B16.ALM

---

## Compilation Environment

### Prerequisites
1. Python 3.10+
2. TVM v0.22.0 (or build from `main` branch)
3. MLC-LLM (latest from `main` branch)
4. Android NDK 27.x
5. CMake 3.18+

### Install MLC-LLM
```bash
# Clone MLC-LLM
git clone --recursive https://github.com/mlc-ai/mlc-llm.git
cd mlc-llm

# Create conda environment
conda create -n mlc-llm python=3.11
conda activate mlc-llm

# Install dependencies
pip install -e .

# Build TVM with Android support
cd 3rdparty/tvm
mkdir build && cd build
cp ../cmake/config.cmake .

# Edit config.cmake:
# set(USE_LLVM ON)
# set(USE_OPENCL ON)
# set(USE_GRAPH_EXECUTOR ON)

cmake ..
make -j$(nproc)
```

---

## Compilation Steps

**Note:** Model weights are already downloaded. You only need to compile.

### Compile Command
```bash
cd /Volumes/M-Drive/Coding/AVA/external/mlc-llm

python3 -m mlc_llm compile \
    /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-MLC/mlc-chat-config.json \
    --device android \
    -o /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/gemma-3-4b-it-q4bf16_1-android.tar
```

This generates a `.ALM` file containing:
- `AVALibrary.ADco` - Model computation code
- `AVA-GE3-4B16.ADco` - Device code for OpenCL

---

## Expected Output

### Directory Structure
After compilation, provide this structure:

```
AVA-GE3-4B16/
├── AVALibrary.ADco                 # Model code (ARM64 ELF relocatable)
├── AVA-GE3-4B16.ADco               # Device code (ARM64 ELF relocatable)
├── mlc-chat-config.json            # Model configuration
├── ndarray-cache.json              # Weight shard mapping
├── tokenizer.model                 # SentencePiece tokenizer
├── tokenizer_config.json           # Tokenizer settings
├── params_shard_0.bin              # Weight shards
├── params_shard_1.bin
└── ...
```

### Verification

Verify the compiled `.ADco` files link against new FFI API:
```bash
nm -u AVALibrary.ADco | grep TVM
```

**Expected output (new API):**
```
U TVMFFIFunctionCall
U TVMFFIErrorSetRaised
U TVMFFIEnvModRegisterContextSymbol
```

**NOT this (old API):**
```
U TVMFuncCall
U TVMAPISetLastError
```

---

## Information Needed for ALC Update

The other Claude instance updating ALC needs to know:

### 1. Model Metadata
For each compiled model variant, provide:

```json
{
  "model_id": "AVA-GE3-4B16",
  "display_name": "Gemma 3 4B IT (Q4BF16)",
  "tvm_version": "0.22.0",
  "quantization": "q4bf16_1",
  "context_length": 8192,
  "prefill_chunk_size": 1024,
  "vocab_size": 262208,
  "hidden_size": 2560,
  "num_attention_heads": 32,
  "num_hidden_layers": 34,
  "device_code_file": "AVA-GE3-4B16.ADco",
  "library_file": "AVALibrary.ADco"
}
```

### 2. Function Names
What TVM functions does the model export? Common ones:
- `prefill` - Process initial prompt
- `decode` - Generate next token
- `reset_kv_cache` - Clear KV cache
- `get_metadata` - Model info

### 3. Input/Output Tensor Format
- Input shape: `[batch_size, seq_len]` or `[1, seq_len]`?
- Input dtype: `int32` or `int64`?
- Output shape: `[batch_size, seq_len, vocab_size]`?
- Output dtype: `float32` or `float16`?

### 4. Special Requirements
- Any OpenCL kernel compilation needed at runtime?
- Memory requirements (min/max)?
- Warmup needed?

### 5. Stop Tokens
```json
{
  "eos_token_id": 1,
  "bos_token_id": 2,
  "pad_token_id": 0,
  "additional_stop_tokens": [107]
}
```

---

## Where to Place Output

Put compiled models at:
```
/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/compiled-tvm22/
└── AVA-GE3-4B16/
    ├── AVALibrary.ADco
    ├── AVA-GE3-4B16.ADco
    └── ... (all files)
```

**For distribution:** Create `AVA-GE3-4B16.ALM` package containing all model files.

---

## Coordination Notes

### What I'm Doing (ALC Update)
I will update these files to support the new TVM v0.22.0 models:

1. **TVMRuntime.kt** - Already updated to load `tvm_runtime` + `tvm4j`
2. **TVMModelLoader.kt** - Will update model loading for new format
3. **Models.kt** - Will add new model configurations
4. **ALCEngine.kt** - May need updates for new inference flow

### What I Need From You
1. The compiled model files (`.o`, `.json`, weights)
2. Model metadata JSON (see format above)
3. Any compilation notes or warnings
4. Verification that symbols use new FFI API

### Timeline Coordination
- You: Compile models → provide output
- Me: Update ALC to use new models → test integration

---

## Troubleshooting

### Common Issues

**1. "No module named mlc_llm"**
```bash
pip install -e . --verbose
```

**2. TVM build fails**
Ensure you have LLVM installed:
```bash
brew install llvm@15
export LLVM_CONFIG=/opt/homebrew/opt/llvm@15/bin/llvm-config
```

**3. Android NDK not found**
```bash
export ANDROID_NDK=/path/to/ndk/27.x
```

**4. Model too large for device**
Try smaller context window:
```bash
--context-window-size 1024
```

---

## References

- TVM Documentation: https://tvm.apache.org/docs/
- MLC-LLM: https://github.com/mlc-ai/mlc-llm
- Gemma Model: https://huggingface.co/google/gemma-2b-it
- AVA Model Setup: `/Volumes/M-Drive/Coding/AVA/docs/Developer-Manual-Chapter42-LLM-Model-Setup.md`

---

## Checklist

Before delivering compiled models:

- [ ] Model compiles without errors
- [ ] `.ADco` files are ARM64 ELF relocatable
- [ ] Symbols use new FFI API (TVMFFIFunctionCall, not TVMFuncCall)
- [ ] All config files included
- [ ] Tokenizer files included
- [ ] Weight shards included
- [ ] Model metadata JSON provided
- [ ] Placed in correct output directory
- [ ] Files renamed to AVA convention (AVALibrary.ADco, AVA-GE3-4B16.ADco)

---

**Questions?** The other Claude instance working on ALC can provide clarification on specific requirements.
