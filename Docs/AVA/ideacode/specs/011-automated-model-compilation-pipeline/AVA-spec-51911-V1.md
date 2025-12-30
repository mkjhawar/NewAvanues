# Feature Specification: Automated Model Compilation Pipeline

**Spec ID:** 011-automated-model-compilation-pipeline
**Created:** 2025-11-19
**Author:** AVA Development Team
**Status:** Draft
**Profile:** android-app
**Complexity:** Tier 3 (High)

---

## Executive Summary

Build an automated pipeline for compiling LLM models and BERT tokenizer files that ensures TVM API version compatibility between compiled models and AVA's runtime. The system must use TVM v0.22.0 with the new FFI API (`TVMFFIFunctionCall`) to match AVA's custom Android runtime, automatically verify symbol compatibility, and apply AVA naming conventions.

---

## Problem Statement

### Current State

1. **Manual compilation process** - Model compilation requires extensive manual steps with environment setup
2. **TVM API version mismatch** - Models compiled with pip-installed `mlc-llm-nightly` use OLD API symbols (`TVMFuncCall`) incompatible with AVA's TVM v0.22.0 runtime (`TVMFFIFunctionCall`)
3. **Python bindings issues** - Local TVM v0.22.0 build has incomplete Python bindings (missing Cython `core.so` with FFI testing symbols)
4. **No verification** - No automated check to verify compiled model symbols match runtime expectations
5. **Complex dependencies** - TVM, MLC-LLM, Android NDK, LLVM must all be correctly configured

### Pain Points

- **Symbol mismatch errors at runtime**: `UnsatisfiedLinkError: tvmFFIFunctionGetGlobal`
- **Hours wasted debugging** API incompatibilities between toolchain versions
- **No standard process** for adding new models (Gemma 3, Gemma 4N, etc.)
- **Manual file renaming** to AVA naming convention (`.ADco`, `.ALM`, `.AON`)
- **No BERT/embedding model** compilation automation

### Desired State

- **One-command compilation**: `alc compile --model gemma3-4b --target android`
- **Automatic verification**: Pipeline verifies compiled symbols match runtime expectations
- **Version-locked toolchain**: Pinned TVM v0.22.0 + MLC-LLM with new FFI API
- **AVA naming applied**: Output files automatically renamed to AVA convention
- **BERT model support**: Also compile ONNX embedding models for NLU

---

## Requirements

### Functional Requirements

#### FR-1: TVM v0.22.0 Python Environment
- **FR-1.1**: Build complete TVM v0.22.0 with Python bindings including tvm_ffi Cython module
- **FR-1.2**: Include FFI testing symbols (`TVMFFITestingDummyTarget`, etc.)
- **FR-1.3**: Support both macOS (host compilation) and Android (cross-compilation)
- **FR-1.4**: Python bindings must load libtvm.dylib without missing symbols

#### FR-2: MLC-LLM Integration
- **FR-2.1**: Use local MLC-LLM from `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/`
- **FR-2.2**: Register Gemma 3 model type in MLC-LLM
- **FR-2.3**: Support all quantization formats: q4f16_1, q4bf16_1, q8f16_1
- **FR-2.4**: Generate Android OpenCL target code

#### FR-3: Compilation Pipeline
- **FR-3.1**: Download model weights from HuggingFace (if not local)
- **FR-3.2**: Convert weights with proper quantization
- **FR-3.3**: Generate config with optimized context/prefill settings
- **FR-3.4**: Compile to Android ARM64 with OpenCL
- **FR-3.5**: Package into `.ALM` archive with all required files

#### FR-4: Symbol Verification
- **FR-4.1**: Automatically verify compiled `.o` files use new FFI API symbols
- **FR-4.2**: Check for `TVMFFIFunctionCall`, `TVMFFIErrorSetRaised`, `TVMFFIEnvModRegisterContextSymbol`
- **FR-4.3**: Fail build if old API symbols detected (`TVMFuncCall`, `TVMAPISetLastError`)
- **FR-4.4**: Generate verification report with symbol list

#### FR-5: AVA Naming Convention
- **FR-5.1**: Rename output files to AVA convention:
  - `lib0.o` → `AVALibrary.ADco`
  - `{model}_devc.o` → `AVA-{CODE}-{SIZE}{BITS}.ADco`
  - `model.tar` → `AVA-{CODE}-{SIZE}{BITS}.ALM`
- **FR-5.2**: Apply model codes: GE2 (Gemma 2), GE3 (Gemma 3), G4N (Gemma 4N)
- **FR-5.3**: Preserve internal filenames for TVM compatibility (`mlc-chat-config.json`, etc.)

#### FR-6: BERT/Embedding Model Compilation
- **FR-6.1**: Convert ONNX embedding models for Android
- **FR-6.2**: Quantize to INT8 for mobile efficiency
- **FR-6.3**: Rename to `.AON` extension
- **FR-6.4**: Generate `AVA-384-Base-INT8.AON` format

#### FR-7: Model Registry
- **FR-7.1**: Maintain registry of available models and their metadata
- **FR-7.2**: Store TVM version, quantization, context length, vocab size
- **FR-7.3**: Track compilation date and verification status
- **FR-7.4**: Support A/B testing model configurations

### Non-Functional Requirements

#### NFR-1: Build Time
- Full LLM compilation: < 30 minutes
- BERT model conversion: < 5 minutes
- Incremental rebuild: < 10 minutes

#### NFR-2: Reliability
- Pipeline must be idempotent
- Failed builds must be resumable
- All errors must have clear messages with fix suggestions

#### NFR-3: Portability
- Run on macOS (ARM64 and x86_64)
- Support CI/CD integration (GitHub Actions)
- Containerizable with Docker

#### NFR-4: Storage
- Compiled models: 1-5 GB per model
- Build artifacts: < 10 GB
- Cached weights: HuggingFace cache location

### Success Criteria

1. **SC-1**: Compile Gemma 3 4B with correct FFI API symbols in < 30 minutes
2. **SC-2**: Model loads in AVA without `UnsatisfiedLinkError`
3. **SC-3**: Symbol verification passes with 100% new API symbols
4. **SC-4**: All output files follow AVA naming convention v2
5. **SC-5**: BERT embedding model compiles to `.AON` format
6. **SC-6**: Pipeline runs without manual intervention

---

## User Stories

### US-1: Compile New LLM Model
**As a** developer
**I want to** compile a new LLM model with one command
**So that** I can add new models to AVA without debugging TVM version issues

**Acceptance Criteria:**
- [ ] Single command: `alc compile gemma3-4b --target android`
- [ ] Automatic environment setup and verification
- [ ] Output placed in correct directory with AVA naming
- [ ] Compilation report with memory usage and timing

### US-2: Verify Model Compatibility
**As a** developer
**I want to** automatically verify compiled models use correct TVM API
**So that** I don't waste hours debugging runtime symbol errors

**Acceptance Criteria:**
- [ ] Build fails fast if wrong symbols detected
- [ ] Clear error message: "Model uses old TVM API (TVMFuncCall). Expected new FFI API (TVMFFIFunctionCall)"
- [ ] Suggestion to use correct TVM version
- [ ] Symbol verification report saved to build output

### US-3: Add Gemma 3 4B to AVA
**As a** developer
**I want to** add Gemma 3 4B as an A/B test model alongside Gemma 2 2B
**So that** we can compare performance and quality

**Acceptance Criteria:**
- [ ] Gemma 3 4B compiled with AVA-GE3-4B16 naming
- [ ] Model metadata JSON generated for ALC
- [ ] Both models loadable in same build
- [ ] Model selection configurable

### US-4: Convert BERT Model for NLU
**As a** developer
**I want to** compile BERT embedding models for NLU
**So that** intent classification works on-device

**Acceptance Criteria:**
- [ ] ONNX model converted to optimized format
- [ ] INT8 quantization applied
- [ ] Output: `AVA-384-Base-INT8.AON`
- [ ] Works with existing IntentClassifier

---

## Technical Constraints

### Android Requirements
- Minimum API Level: 26 (Android 8.0)
- Target API Level: 34 (Android 14)
- ABI: arm64-v8a
- GPU: OpenCL for inference acceleration

### TVM Requirements
- Version: 0.22.0 (commit 9dbf3f22ff6f44962472f9af310fda368ca85ef2)
- FFI API: New symbols (TVMFFIFunctionCall, not TVMFuncCall)
- LLVM: 15.x for code generation
- Android NDK: 27.x for cross-compilation

### Runtime Libraries
```
apps/ava-standalone/src/main/jniLibs/arm64-v8a/
├── libtvm_runtime.so    # 62MB - TVM runtime with new FFI API
└── libtvm4j.so          # 124KB - JNI bridge for org.apache.tvm
```

### Model Storage
```
/sdcard/Android/data/com.augmentalis.ava/files/models/
├── AVA-384-Base-INT8.AON              # NLU Embedding
└── llm/
    ├── AVA-GE2-2B16/                  # Gemma 2 2B
    └── AVA-GE3-4B16/                  # Gemma 3 4B
```

---

## Dependencies

### Build Dependencies
1. **TVM v0.22.0** - From `~/Downloads/Coding/tvm-build/`
2. **MLC-LLM** - From `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/`
3. **LLVM 15** - From Homebrew `/opt/homebrew/opt/llvm@15/`
4. **Android NDK 27** - From `~/Library/Android/sdk/ndk/27.0.12077973/`
5. **Python 3.9+** - System Python with Cython

### Runtime Dependencies
1. **libtvm_runtime.so** - TVM v0.22.0 Android build
2. **libtvm4j.so** - JNI bindings for org.apache.tvm
3. **tvm4j_core.jar** - Java bindings for TVM FFI

### Model Dependencies
1. **HuggingFace models** - Weights and tokenizers
2. **Pre-compiled .o files** - From MLC-LLM compilation

---

## Out of Scope

1. **iOS compilation** - Future feature, not in this spec
2. **Windows build host** - macOS only for now
3. **Custom model architectures** - Only supported MLC-LLM models
4. **Cloud inference** - This is for on-device models only
5. **Model training/fine-tuning** - Only compilation of pre-trained models
6. **Real-time model updates** - Models are bundled or downloaded once

---

## Technical Notes

### The Core Problem (from Current Session)

The pip-installed `mlc-llm-nightly` package uses an older TVM version that generates models with old C API symbols:
- `TVMFuncCall`
- `TVMAPISetLastError`
- `TVMBackendRegisterSystemLibSymbol`

AVA's TVM v0.22.0 runtime expects new FFI API symbols:
- `TVMFFIFunctionCall`
- `TVMFFIErrorSetRaised`
- `TVMFFIEnvModRegisterContextSymbol`

### Solution Approach

1. **Build TVM v0.22.0 Python bindings completely**
   - Include tvm_ffi Cython module with all testing symbols
   - Ensure libtvm.dylib exports FFI testing functions

2. **Use local MLC-LLM with correct TVM**
   - Set PYTHONPATH to local TVM and MLC-LLM
   - Verify compilation uses new FFI API

3. **Automated symbol verification**
   - Run `nm -u lib0.o | grep TVM` after compilation
   - Fail if any old API symbols present

### File Locations

- **TVM Build**: `~/Downloads/Coding/tvm-build/build-macos/`
- **MLC-LLM**: `/Volumes/M-Drive/Coding/AVA/external/mlc-llm/`
- **Model Weights**: `/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/gemma-3-models/`
- **Compiled Output**: `/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/compiled-tvm22/`

---

## Appendix: AVA Naming Convention v2

### Extensions
| Extension | Description | Replaces |
|-----------|-------------|----------|
| `.ADco` | AVA Device Code | `.o` |
| `.ALM` | AVA LLM Model | `.tar` |
| `.AON` | AVA ONNX Model | `.onnx` |

### Model Naming Format
`AVA-{MODEL}{VERSION}-{SIZE}{BITS}`

### Model Codes
| Model | Code | Example |
|-------|------|---------|
| Gemma 2 | GE2 | AVA-GE2-2B16 |
| Gemma 3 | GE3 | AVA-GE3-4B16 |
| Gemma 4N | G4N | AVA-G4N-4B16 |

---

## References

- [Developer Manual Chapter 42: LLM Model Setup](../../../docs/Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [Developer Manual Chapter 38: LLM Model Management](../../../docs/Developer-Manual-Chapter38-LLM-Model-Management.md)
- [Gemma Model Compilation Instructions](../../../docs/instructions/GEMMA-MODEL-COMPILATION-INSTRUCTIONS.md)
- [TVM Documentation](https://tvm.apache.org/docs/)
- [MLC-LLM GitHub](https://github.com/mlc-ai/mlc-llm)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-19
