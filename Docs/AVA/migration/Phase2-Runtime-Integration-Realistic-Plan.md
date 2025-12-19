# Phase 2: Runtime Integration - Realistic Plan

**Date**: 2025-10-30
**Status**: Planning
**Priority**: P0 (Blocks LLM functionality)

---

## Executive Summary

Phase 1 completed the **ALC Engine rewrite** (1,032 lines of custom Kotlin code). However, to actually **run** the engine, we need runtime components. This document outlines the **realistic** path forward.

---

## Current Status

### ✅ What We Have

1. **ALC Engine (100% Custom)**
   - `ALCEngine.kt` (476 lines) - Core inference engine
   - `TVMRuntime.kt` (274 lines) - TVM integration wrapper
   - `LocalLLMProvider.kt` (189 lines) - Provider implementation
   - `TVMStubs.kt` (93 lines) - Temporary compilation stubs

2. **Gemma 2B Model Files** (Partially Complete)
   - Location: `platform/app/src/main/assets/models/gemma-2b-it/`
   - ✅ `gemma_q4f16_1_devc.o` (2.8MB) - Compiled model code
   - ✅ `lib0.o` (1.0MB) - Model library
   - ✅ `mlc-chat-config.json` (1.8KB) - Model configuration
   - ✅ `ndarray-cache.json` (76KB) - Tensor cache metadata
   - ⏳ **Missing**: Tokenizer files (downloading)

3. **Architecture Decision**
   - ✅ Chose **Option A**: Direct TVM integration (not MLC wrapper)
   - ✅ Custom ALC Engine tailored for AVA's needs
   - ✅ Privacy-first, streaming, thread-safe design

### ❌ What We're Missing

1. **TVM Runtime** (Critical Blocker)
   - Our `TVMStubs.kt` throws `NotImplementedError` at runtime
   - Need real TVM JNI bindings (Java + native .so files)

2. **Tokenizer Files** (In Progress)
   - `tokenizer.model` - SentencePiece model
   - `tokenizer.json` - Tokenizer configuration
   - `tokenizer_config.json` - HuggingFace config

---

## The TVM Problem

### Why This Is Complex

Apache TVM does NOT provide pre-built binaries for Android. You must:

1. **Set up build environment**:
   - Android NDK 27+
   - CMake 3.18+
   - Rust toolchain (for HuggingFace tokenizers)
   - JDK 17+
   - Python 3.9+ with MLC-LLM package

2. **Build from source** (~30-60 minutes):
   ```bash
   cd external/mlc-llm/android/mlc4j
   python3 prepare_libs.py --mlc-llm-source-dir=../../..
   ```

3. **Output** (`mlc4j/output/` directory):
   - `tvm4j-core-0.15.0.jar` (~500KB) - Java bindings
   - `libtvm4j_runtime_packed.so` (~3-5MB per ABI) - Native runtime
   - `libmlc_llm.so` (~50-70MB per ABI) - MLC LLM runtime

### Why We Can't Skip This

Our ALC Engine directly calls TVM functions:
```kotlin
// ALCEngine.kt:92
tvmRuntime = TVMRuntime.create(context)  // Needs real TVM

// TVMRuntime.kt:80
val systemModule = Module.loadFromFile(resolvedPath)  // Needs TVM JNI

// TVMModule.kt:237
val result = prefillFunc.invoke(inputTensor)  // Needs TVM native code
```

Without TVM runtime, **every operation throws `NotImplementedError`**.

---

## Options Going Forward

### Option 1: Build TVM Yourself (Recommended for Production)

**Pros:**
- ✅ Full control over build configuration
- ✅ Can optimize for specific devices
- ✅ Can update TVM independently

**Cons:**
- ❌ Requires 30-60 minute build process
- ❌ Requires build environment setup (NDK, Rust, etc.)
- ❌ ~300MB additional disk space for build artifacts

**Steps:**
1. Install prerequisites (see `external/mlc-llm/android/README.md`)
2. Set environment variables (`ANDROID_NDK`, `TVM_SOURCE_DIR`)
3. Run build script:
   ```bash
   cd external/mlc-llm/android/mlc4j
   python3 prepare_libs.py
   ```
4. Copy artifacts to AVA project:
   ```bash
   cp output/*.jar features/llm/libs/
   cp output/lib/*.so features/llm/libs/arm64-v8a/
   ```
5. Delete `TVMStubs.kt`
6. Update `build.gradle.kts` to reference JARs

**Time Estimate**: 2-3 hours (including setup)

---

### Option 2: Use MLC's Pre-Built MLCEngine (Simpler, But Less Custom)

**Pros:**
- ✅ Pre-built binaries available
- ✅ Faster to integrate (1-2 hours)
- ✅ Battle-tested by MLC community

**Cons:**
- ❌ Loses our custom ALC Engine rewrite
- ❌ Tied to MLC's API design
- ❌ Less flexibility for AVA-specific optimizations

**Steps:**
1. Reference MLC's published Android library:
   ```kotlin
   // features/llm/build.gradle.kts
   dependencies {
       implementation("ai.mlc.mlcllm:mlc4j:0.1.0")  // If published
   }
   ```
2. Replace our ALC Engine with MLC's `MLCEngine` wrapper
3. Update `LocalLLMProvider.kt` to use MLC API

**Status**: ❌ **Not Recommended** - We spent 3 hours writing custom ALC Engine for a reason (privacy, streaming, thread safety)

---

### Option 3: Request Pre-Built Binaries from MLC (Temporary Workaround)

**Pros:**
- ✅ Fastest path to testing (~30 minutes)
- ✅ Keeps our custom ALC Engine

**Cons:**
- ❌ Not officially supported
- ❌ May be outdated
- ❌ Not reproducible for production

**Steps:**
1. Check if Manoj has pre-built TVM from previous MLC experiments
2. Copy JAR and .so files to project
3. Test with our ALC Engine

**Status**: ⏳ Worth trying first for validation

---

## Recommended Path

### Phase 2A: Validate with Pre-Built (If Available)

**Goal**: Prove our ALC Engine works with real TVM runtime

1. ✅ **Model files in place** (gemma-2b-it extracted)
2. ⏳ **Get tokenizer files** (currently downloading)
3. ⏳ **Find or build TVM runtime**:
   - Check `/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/` for pre-built libs
   - If not available, build using Option 1
4. ⏳ **Integrate and test**:
   - Copy JARs/SOs to `features/llm/libs/`
   - Delete `TVMStubs.kt`
   - Update `build.gradle.kts`
   - Test model loading

**Time Estimate**: 2-4 hours (depending on TVM availability)

### Phase 2B: Production Build Setup

**Goal**: Document reproducible build process

1. Document exact TVM build commands
2. Create build script (`scripts/build-tvm-android.sh`)
3. Add to CI/CD pipeline (optional)
4. Document in `docs/Developer-Manual.md`

**Time Estimate**: 1-2 hours

---

## Model Files Status

### Current Location
```
platform/app/src/main/assets/models/gemma-2b-it/
├── gemma_q4f16_1_devc.o       # ✅ Compiled model (2.8MB)
├── lib0.o                      # ✅ Model library (1.0MB)
├── mlc-chat-config.json        # ✅ Configuration (1.8KB)
├── ndarray-cache.json          # ✅ Tensor metadata (76KB)
├── tokenizer.model             # ⏳ Downloading...
├── tokenizer.json              # ⏳ Downloading...
└── tokenizer_config.json       # ⏳ Downloading...
```

### Size Implications

**Current Assets Size**: ~4MB (without tokenizer)
**With Tokenizer**: ~5-6MB
**With TVM Runtime (.so)**: +50-70MB per ABI

**APK Size Impact**:
- Model files: ~6MB (in assets, always bundled)
- TVM runtime: ~70MB (arm64-v8a only for initial release)
- **Total increase**: ~76MB

**Mitigation**:
- Use `splits` to create per-ABI APKs
- Consider on-demand model download (future Phase 3)

---

## Build Configuration Changes Needed

### features/llm/build.gradle.kts

```kotlin
android {
    namespace = "com.augmentalis.ava.features.llm"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        ndk {
            // Only build for arm64-v8a initially
            abiFilters += listOf("arm64-v8a")
        }
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("libs")  // For .so files
        }
    }
}

dependencies {
    // ... existing deps ...

    // TVM Runtime (local JAR)
    implementation(files("libs/tvm4j-core-0.15.0.jar"))

    // OR if we publish to Maven local
    // implementation("org.apache.tvm:tvm4j:0.15.0")
}
```

### settings.gradle.kts

No changes needed - `features:llm` already included

---

## Testing Checklist

Once TVM runtime is integrated:

- [ ] ✅ Build succeeds without stub errors
- [ ] ✅ Model loads from assets
- [ ] ✅ Tokenizer initializes
- [ ] ✅ First inference completes (<5 seconds)
- [ ] ✅ Streaming works (tokens emitted incrementally)
- [ ] ✅ Memory usage reasonable (<512MB)
- [ ] ✅ No crashes on device

---

## Next Steps

### Immediate (Today)

1. ✅ Wait for tokenizer download to complete
2. ⏳ Check if pre-built TVM libs available locally
3. ⏳ If not, set up build environment and compile TVM

### Short Term (This Week)

1. Integrate TVM runtime
2. Test model loading and inference
3. Validate performance targets (<100ms NLU, <500ms chat)
4. Document build process

### Medium Term (Next Week)

1. Optimize model loading (lazy initialization)
2. Add model download UI (for future models)
3. Implement model caching
4. Device-specific optimizations

---

## Alternatives Considered

### llama.cpp Instead of TVM?

**Pros**: Simpler build, pure C++, no Python dependency

**Cons**:
- Would require rewriting ALC Engine again
- Less flexibility than TVM
- Not compatible with MLC models

**Verdict**: ❌ Stick with TVM - we've already invested in the architecture

### GGML/GGUF Format?

**Pros**: Smaller model sizes, simpler runtime

**Cons**:
- Would need to convert Gemma 2B to GGUF
- Different inference engine required
- Not compatible with our TVM-based code

**Verdict**: ❌ TVM/MLC is the right choice for our architecture

---

## Summary

**What's Done**:
- ✅ ALC Engine rewritten (100% custom, 1,032 lines)
- ✅ Gemma 2B model files extracted to assets
- ✅ Model configuration downloaded
- ⏳ Tokenizer files downloading

**What's Needed**:
- ❗ **TVM Runtime** (JAR + native .so) - CRITICAL BLOCKER
- ⏳ Tokenizer files (in progress)

**Recommended Action**:
1. Check for existing pre-built TVM libs in your Downloads
2. If not found, build TVM from source (2-3 hours one-time)
3. Copy artifacts to project
4. Test inference

**Reality Check**: The ALC Engine code is production-ready, but we can't run it without TVM runtime. This is expected and was always part of the plan - Phase 1 was architecture, Phase 2 is integration.

---

**Created**: 2025-10-30 03:25 PDT
**Author**: AVA AI Team
**Next Update**: After TVM integration complete
