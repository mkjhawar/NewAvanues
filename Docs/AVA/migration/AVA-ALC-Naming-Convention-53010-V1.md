# ALC Naming Convention & Architecture

**Date**: 2025-10-30
**Status**: Active

---

## What is ALC?

**ALC = Adaptive LLM Coordinator**

### Breakdown:
- **Adaptive** - Dynamically adjusts behavior based on:
  - Device capabilities (GPU, RAM, battery)
  - Privacy requirements (sensitive data → local only)
  - Network conditions (offline → local fallback)
  - User preferences (speed vs quality tradeoffs)

- **LLM** - Large Language Model
  - On-device inference using quantized models
  - Gemma 2B, Llama 3.2 3B, etc.
  - INT4/INT8 quantization for mobile efficiency

- **Coordinator** - Intelligent routing and resource management:
  - Routes between local and cloud LLMs
  - Manages model loading/unloading
  - Optimizes memory and battery usage
  - Handles fallback strategies

---

## Why "ALC" Not "MLC"?

### MLC LLM (Upstream Project)
- **MLC = Machine Learning Compilation**
- Open-source project by Apache TVM team
- Provides model compilation and runtime infrastructure
- **We use** their TVM runtime but **not** their Android wrapper

### Our ALC Engine
- **100% custom rewrite** (1,032 lines of AVA-specific code)
- Designed for AVA AI's privacy-first requirements
- Kotlin-idiomatic (Flow, coroutines, sealed classes)
- Tailored for smart glasses and low-memory devices

### What We Keep from MLC:
1. **TVM Runtime Libraries** (binary dependencies):
   - `tvm4j-core-*.jar` - Java bindings for TVM
   - `libtvm4j_runtime_packed.so` - Native TVM runtime
   - These are from **Apache TVM**, not MLC-specific

2. **Model Format**:
   - INT4 quantized models compiled by MLC tools
   - Model files: `.o` object files + configs

3. **Build Tools**:
   - `mlc_llm package` command to build Android libraries
   - Only used during development, not shipped with app

### What's 100% Ours (ALC):
1. **ALCEngine.kt** - Core inference engine
2. **TVMRuntime.kt** - TVM integration wrapper
3. **LocalLLMProvider.kt** - LLMProvider implementation
4. **All business logic**:
   - Privacy-sensitive routing
   - Streaming implementation
   - Thread-safe state management
   - Memory optimization
   - Battery awareness

---

## File Naming Convention

### Our Code (Always "ALC"):
```
features/llm/src/main/java/com/augmentalis/ava/features/llm/alc/
├── ALCEngine.kt              # Core engine
├── TVMRuntime.kt             # TVM wrapper
└── LocalLLMProvider.kt       # Provider

Package: com.augmentalis.ava.features.llm.alc
```

### External Dependencies (Keep Original Names):
```
features/llm/libs/
├── tvm4j-core-0.15.0.jar            # Apache TVM (keep name)
└── arm64-v8a/
    └── libtvm4j_runtime_packed.so   # Apache TVM (keep name)
```

**Rationale**: Binary libraries must keep original names for JNI loading to work correctly.

---

## Package Structure

### AVA AI Package Hierarchy:
```
com.augmentalis.ava
├── core
│   ├── common          # Result wrapper, utilities
│   └── domain          # Domain models, interfaces
├── data                # Repository implementations
├── features
│   ├── llm
│   │   ├── alc         # ← OUR CODE (ALC Engine)
│   │   ├── provider    # ← OUR CODE (LocalLLMProvider)
│   │   └── domain      # ← OUR CODE (LLMProvider interface)
│   ├── nlu             # ONNX NLU (MobileBERT)
│   ├── chat            # Chat UI
│   └── teachava        # Teach-Ava training UI
└── platform            # Android-specific code
```

**No MLC references in our package names** - all AVA-branded.

---

## Class Naming Convention

### ✅ Use "ALC" Prefix:
- `ALCEngine` - Main inference engine
- `ALCConfiguration` - ALC-specific configuration (if needed)
- `ALCPerformanceMonitor` - Performance tracking (future)

### ✅ Use Descriptive Names (No Prefix):
- `LocalLLMProvider` - Implementation of LLMProvider
- `TVMRuntime` - TVM integration wrapper
- `TVMModule` - TVM module wrapper
- `ModelConfig` - Model configuration
- `GenerationOptions` - Inference options

### ❌ Avoid "MLC" Prefix:
- ~~`MLCEngine`~~ → `ALCEngine`
- ~~`MLCChat`~~ → `LocalLLMProvider`
- ~~`MLCConfig`~~ → `ModelConfig`

---

## Documentation References

### When Referring to Our Code:
- "ALC Engine" or "Adaptive LLM Coordinator"
- "AVA's on-device LLM system"
- "Local inference engine"

### When Referring to Upstream:
- "Apache TVM runtime" (for TVM dependencies)
- "MLC LLM tools" (for build process)
- "MLC-compiled models" (for model format)

### Examples:

**✅ Good**:
> "The ALC Engine uses Apache TVM runtime for efficient on-device inference."
> "We compile models using MLC LLM tools, then load them with our custom ALC Engine."

**❌ Confusing**:
> "The MLC Engine runs locally on device."
> (Sounds like we're using MLC's wrapper, which we're not)

---

## User-Facing Terminology

### In App UI:
- "On-Device AI" (not "Local LLM" - too technical)
- "Privacy Mode" (when forcing local processing)
- "Smart Response" (general term for LLM output)

### In Settings:
- "Use On-Device AI" (toggle for local vs cloud)
- "AI Model: Gemma 2B" (model selection)
- "Performance: Balanced" (quality vs speed tradeoff)

### In Logs/Errors:
- "ALC Engine" (for developers/debugging)
- "Local AI Model" (for user-facing errors)

---

## Technical Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│ AVA AI Chat UI                                  │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│ LocalLLMProvider (OUR CODE)                     │
│ - Implements LLMProvider interface              │
│ - Converts AVA types to ALC types               │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│ ALCEngine (OUR CODE - 100% Custom)              │
│ - Adaptive routing logic                        │
│ - Streaming via Kotlin Flow                     │
│ - Thread-safe state management                  │
│ - Privacy-first decision making                 │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│ TVMRuntime (OUR WRAPPER)                        │
│ - Kotlin-friendly TVM interface                 │
│ - Device management (CPU/GPU/OpenCL)            │
│ - Model loading from assets                     │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│ Apache TVM Runtime (BINARY DEPENDENCY)          │
│ - tvm4j-core.jar (Java bindings)                │
│ - libtvm4j_runtime_packed.so (Native runtime)   │
│ - NOT CUSTOMIZED (use as-is)                    │
└─────────────────────────────────────────────────┘
```

**Key Point**: We control layers 1-4, TVM is a black-box binary dependency.

---

## Branding Guidelines

### Internal Code:
- Use "ALC" prefix for our custom engine code
- Use clear descriptive names for utilities
- Package: `com.augmentalis.ava.features.llm.alc`

### Documentation:
- **User Docs**: "On-Device AI", "Privacy Mode"
- **Developer Docs**: "ALC Engine (Adaptive LLM Coordinator)"
- **Technical Specs**: Full name + acronym on first use

### Marketing Materials:
- **Primary**: "Privacy-First On-Device AI"
- **Secondary**: "Powered by AVA's Adaptive LLM Coordinator (ALC)"
- **Technical**: "Built on Apache TVM runtime with custom inference engine"

---

## FAQ

### Q: Can we rename the .so files to libALC instead of libTVM?
**A**: No. The TVM Java bindings (`tvm4j-core.jar`) expect specific filenames for JNI loading. Renaming would break the JNI bindings.

### Q: Should we fork MLC LLM and rename it?
**A**: No. We've already rewritten the Android integration layer (100% custom code). We only use MLC's build tools and TVM runtime (which is Apache's, not MLC's).

### Q: What about the model files - do they reference MLC?
**A**: The models are in "MLC format" (compiled by MLC tools), but this is just a technical detail. In our app, we call them "AI models" or reference by name (Gemma 2B, etc.).

### Q: How do we explain this to users?
**A**: Users never see "ALC", "MLC", or "TVM". They see:
- "On-Device AI" (in settings)
- "AI Model: Gemma 2B" (model selection)
- "Privacy Mode: Enabled" (when forcing local)

---

## Summary

- **ALC = Adaptive LLM Coordinator** (our brand)
- **Our code**: 100% custom, AVA-branded, privacy-first
- **TVM runtime**: Binary dependency, Apache project, use as-is
- **MLC tools**: Only for building (not shipped with app)
- **User-facing**: "On-Device AI" (simple, clear, non-technical)

**The key message**: We built a custom AI engine (ALC) tailored for AVA's privacy-first, offline-capable architecture. We use battle-tested open-source components (Apache TVM runtime) but the intelligence and integration are 100% ours.

---

**Created**: 2025-10-30
**Last Updated**: 2025-10-30
**Maintained By**: AVA AI Team
