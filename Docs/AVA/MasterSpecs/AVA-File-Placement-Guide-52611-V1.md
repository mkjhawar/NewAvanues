# AVA File Placement Guide

**Version**: 1.0
**Last Updated**: 2025-11-26

This document specifies where all model files, ontology files, and configuration files must be placed for AVA to function correctly.

---

## Quick Reference

```
AVA/
├── apps/ava-standalone/src/main/assets/
│   ├── models/
│   │   └── mobilebert_int8.onnx          # NLU Model (REQUIRED)
│   └── ontology/
│       └── en-US/
│           ├── communication.aot          # AON 3.0 Ontology
│           ├── device_control.aot
│           ├── media.aot
│           ├── navigation.aot
│           └── productivity.aot
│
├── ava-ai-models/
│   ├── llm/
│   │   ├── AVA-GE2-2B16/                 # Gemma 2 2B (smaller)
│   │   └── AVA-GE3-4B16/                 # Gemma 3 4B (recommended)
│   │       ├── ava-model-config.json
│   │       ├── params_shard_*.bin
│   │       ├── tokenizer.json
│   │       └── *.ADco
│   └── embeddings/
│       └── AVA-384-Base-INT8.AON         # Embedding model package
│
└── models/
    ├── original/
    │   └── AVA-ONX-384-*.onnx            # Original ONNX models
    └── quantized/
        └── AVA-ONX-384-*-INT8.onnx       # Quantized INT8 models
```

---

## 1. NLU Model (MobileBERT)

### Location
```
apps/ava-standalone/src/main/assets/models/mobilebert_int8.onnx
```

### Purpose
Intent classification using ONNX Runtime with NNAPI/QNN acceleration.

### Specifications
| Property | Value |
|----------|-------|
| Format | ONNX INT8 Quantized |
| Size | ~12-15 MB |
| Hidden Size | 384 |
| Backend | NNAPI (Android 8.1+), QNN (Qualcomm) |

### How to Obtain
```bash
# Download from HuggingFace
huggingface-cli download augmentalis/mobilebert-int8-onnx \
  --local-dir apps/ava-standalone/src/main/assets/models/
```

---

## 2. AON 3.0 Ontology Files

> **Note on Naming**: AON refers to the format specification (AVA Ontology), while `.aot` is the file extension (AVA Ontology Template/Text). Do not confuse `.aot` files with `.AON` files (uppercase), which are binary wrapper packages used for model distribution.

### Location
```
apps/ava-standalone/src/main/assets/ontology/{locale}/
```

### Current Files

| File | Category | Intents | Purpose |
|------|----------|---------|---------|
| `communication.aot` | communication | 3 | Email, text, call |
| `device_control.aot` | device_control | 8 | Lights, volume, settings |
| `media.aot` | media | 6 | Music, video playback |
| `navigation.aot` | navigation | 5 | Maps, directions |
| `productivity.aot` | productivity | 6 | Calendar, reminders |

### Adding New Locales
```bash
# Create new locale directory
mkdir -p apps/ava-standalone/src/main/assets/ontology/es-ES/

# Copy and translate files
cp apps/ava-standalone/src/main/assets/ontology/en-US/*.aot \
   apps/ava-standalone/src/main/assets/ontology/es-ES/

# Edit files to translate synonyms and descriptions
```

### Schema Requirement
All files MUST use:
```json
{
  "schema": "ava-ontology-3.0",
  "version": "3.0.0"
}
```

---

## 3. LLM Models (Gemma)

### Location
```
ava-ai-models/llm/{model-name}/
```

### Available Models

| Model | Size | Parameters | Use Case |
|-------|------|------------|----------|
| `AVA-GE2-2B16` | ~1.2 GB | 2B | Low-memory devices |
| `AVA-GE3-4B16` | ~2.1 GB | 4B | Recommended |

### Required Files

```
AVA-GE3-4B16/
├── ava-model-config.json      # Model configuration (REQUIRED)
├── tokenizer.json             # Tokenizer data (REQUIRED)
├── tokenizer.model            # SentencePiece model
├── tokenizer_config.json      # Tokenizer settings
├── added_tokens.json          # Special tokens
├── params_shard_0.bin         # Model weights (REQUIRED)
├── params_shard_1.bin
├── ... (up to 68 shards)
├── params_shard_68.bin
├── ndarray-cache.json         # Cache metadata
├── tensor-cache.json          # Tensor info
├── AVA-GE3-4B16.ADco          # Compiled device code (ARM64)
└── AVALibrary.ADco            # TVM library code (ARM64)
```

### GPU Backend
The `.ADco` files are ARM64 ELF binaries. GPU backend (Vulkan/OpenCL) is selected at runtime:

```kotlin
// Automatic selection
TVMRuntime.create(context, "auto")

// Force specific backend
TVMRuntime.create(context, "vulkan")  // Modern GPUs
TVMRuntime.create(context, "opencl")  // Legacy/wide support
TVMRuntime.create(context, "cpu")     // Fallback
```

---

## 4. Embedding Models

### Location
```
ava-ai-models/embeddings/
```

### Current Files

| File | Format | Size | Purpose |
|------|--------|------|---------|
| `AVA-384-Base-INT8.AON` | AON Package | ~22 MB | Sentence embeddings |

### AON Package Format
The `.AON` file is a packaged ONNX model with AVA metadata header.

---

## 5. Source ONNX Models

### Location
```
models/
├── original/      # Full-precision models
└── quantized/     # INT8 optimized
```

### Files

| File | Precision | Size | Use Case |
|------|-----------|------|----------|
| `AVA-ONX-384-BASE.onnx` | FP32 | ~45 MB | Development |
| `AVA-ONX-384-MULTI.onnx` | FP32 | ~90 MB | Multilingual |
| `AVA-ONX-384-BASE-INT8.onnx` | INT8 | ~12 MB | Production |
| `AVA-ONX-384-MULTI-INT8.onnx` | INT8 | ~23 MB | Production |

---

## 6. Runtime Model Paths

### Accessing Models in Code

```kotlin
// NLU Model (from assets)
val nluModelPath = context.filesDir.resolve("models/mobilebert_int8.onnx")
ModelManager.extractAssetIfNeeded(context, "models/mobilebert_int8.onnx", nluModelPath)

// LLM Model (from external storage)
val llmModelDir = File(context.getExternalFilesDir(null), "ava-ai-models/llm/AVA-GE3-4B16")

// AON Files (from assets)
val parser = AonFileParser(context)
parser.loadAllAonFiles("ontology/en-US")
```

---

## 7. Build Integration

### Gradle Configuration

```kotlin
// build.gradle.kts (app module)
android {
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

    // Exclude large files from debug APK
    packagingOptions {
        resources.excludes += "**/*.onnx"  // Copy at runtime instead
    }
}
```

### Asset Copying

For large models, copy at first launch instead of bundling:

```kotlin
class ModelDownloader {
    suspend fun ensureModelsAvailable(context: Context) {
        // Download from server or copy from OBB
        if (!nluModelExists(context)) {
            downloadModel("mobilebert_int8.onnx", getNluModelPath(context))
        }
    }
}
```

---

## 8. File Size Summary

| Component | Size | Bundled in APK |
|-----------|------|----------------|
| AON 3.0 Ontology (`.aot` files) | ~40 KB | Yes |
| MobileBERT INT8 | ~12 MB | Optional |
| AVA-GE3-4B16 | ~2.1 GB | No (download) |
| Embedding Model | ~22 MB | No (download) |

**Recommended APK Strategy**:
- Bundle: AON files (`.aot`) only (~40 KB)
- Download on first launch: NLU model (~12 MB)
- Download on demand: LLM model (~2.1 GB)

---

## 9. Validation Checklist

Before release, verify:

- [ ] `mobilebert_int8.onnx` loads without errors
- [ ] All `.aot` files parse with schema `ava-ontology-3.0`
- [ ] LLM model directory contains all required files
- [ ] `InferenceBackendSelector` detects correct backend
- [ ] Intent classification returns results < 100ms

### Validation Command

```bash
# Run validation tests
./gradlew :apps:ava-standalone:connectedAndroidTest \
  --tests "com.augmentalis.ava.features.nlu.aon.*"
```

---

## 10. Troubleshooting

### "Model not found"
Check file exists at expected path:
```kotlin
val modelFile = File(context.filesDir, "models/mobilebert_int8.onnx")
Log.d(TAG, "Model exists: ${modelFile.exists()}, size: ${modelFile.length()}")
```

### "Invalid AON schema"
Ensure file starts with:
```json
{"schema": "ava-ontology-3.0"
```

### "LLM shards missing"
Verify all 69 shards (0-68) exist:
```bash
ls -la ava-ai-models/llm/AVA-GE3-4B16/params_shard_*.bin | wc -l
# Should output: 69
```

---

**Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis**
