# DEPRECATED: AVA/LLM Module

**Status:** DEPRECATED
**Date:** 2026-01-02
**Replaced By:** `Modules/ALC`

---

## Migration Notice

This Android-only LLM module has been replaced by the Kotlin Multiplatform **ALC (AI Language Core)** module.

### Why Migrated?

| Issue | Resolution |
|-------|------------|
| Android-only | ALC supports Android, iOS, macOS, Windows, Linux |
| TVM-only inference | ALC supports TVM, CoreML, ONNX, llama.cpp |
| Limited cloud providers | ALC has 6 providers (Anthropic, OpenAI, Groq, Google, OpenRouter, HuggingFace) |
| No Hilt DI | ALC has full Hilt integration |
| Inconsistent API | ALC has unified IInferenceEngine interface |

### New Module Location

```
Modules/ALC/
├── src/
│   ├── commonMain/     # Shared domain, providers, response generators
│   ├── androidMain/    # TVM Runtime + Hilt DI
│   ├── iosMain/        # Core ML via cinterop
│   ├── desktopMain/    # ONNX Runtime (JVM)
│   ├── macosMain/      # Core ML native
│   ├── linuxMain/      # llama.cpp native
│   └── mingwMain/      # DirectML/ONNX native
└── build.gradle.kts    # KMP configuration
```

### Migration Steps

1. Update imports: `com.augmentalis.llm` → `com.augmentalis.alc`
2. Update Gradle: `implementation(project(":Modules:ALC"))`
3. Inject `ALCManager` instead of old LLM classes
4. Use `IInferenceEngine` for local inference

### Files Preserved

- `libs/tvm4j_core.jar.bak` - Original TVM library (copied to ALC)
- `build/` - Build artifacts for reference

---

**Do Not Use:** This module is no longer maintained. Use `:Modules:ALC` instead.
