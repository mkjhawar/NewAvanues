# AVA File Format Standards

**Version:** 2.0
**Last Updated:** 2025-11-19
**Status:** Active

---

## Overview

AVA uses proprietary file extensions to distinguish its models and compiled artifacts from standard industry formats. This provides branding, security through obscurity, and clear identification of AVA-processed files.

---

## File Extension Mappings

| AVA Extension | Industry Standard | Format Type | Description |
|---------------|-------------------|-------------|-------------|
| `.AON` | `.onnx` | Neural Network | ONNX models (identical binary format) |
| `.ALM` | `.tar` | LLM Archive | Compiled LLM model packages |
| `.ADco` | `.o` | Object Code | Compiled device code |

---

## AON - AVA ONNX Naming

### Definition
`.AON` (AVA ONNX Naming) is AVA's standard extension for ONNX neural network models.

### Key Points
- **Binary Identical**: AON files ARE ONNX files - same format, different extension
- **Proprietary Branding**: Distinguishes AVA-processed models from third-party files
- **Backward Compatible**: Code falls back to `.onnx` if `.AON` not found

### When to Use
| Context | Use | Example |
|---------|-----|---------|
| File paths in code | `.AON` | `models/AVA-384-Base-INT8.AON` |
| Download from HuggingFace | `.onnx` | `model.onnx` (source format) |
| Library imports | Package name | `ai.onnxruntime.*` |
| User instructions | `.AON` | `adb push model.AON /path/` |

### Naming Convention
```
AVA-{DIM}-{Variant}-{Quant}.AON
```

**Components:**
- `AVA` - Prefix (required)
- `DIM` - Embedding dimensions (384, 768)
- `Variant` - Model variant (Base, Multi, Fast, Qual)
- `Quant` - Quantization (INT8, FP16, FP32)

**Examples:**
```
AVA-384-Base-INT8.AON       # English base, 384-dim, INT8
AVA-384-Multi-INT8.AON      # Multilingual, 384-dim, INT8
AVA-768-Qual-INT8.AON       # High quality, 768-dim, INT8
AVA-384-Fast-INT8.AON       # Fast/small, 384-dim, INT8
```

---

## ALM - AVA LLM Model

### Definition
`.ALM` (AVA LLM Model) is AVA's standard extension for compiled LLM model archives.

### Contents
ALM files are tar archives containing:
- `lib0.o` → Renamed to `AVALibrary.ADco`
- `{model}_devc.o` → Renamed to `AVA-{CODE}-{SIZE}{BITS}.ADco`
- `mlc-chat-config.json` - Model configuration
- `ndarray-cache.json` - Weight mappings

### Naming Convention
```
AVA-{MODEL}{VERSION}-{SIZE}{BITS}.ALM
```

**Model Codes:**
| Code | Model | Company |
|------|-------|---------|
| GE2 | Gemma 2 | Google |
| GE3 | Gemma 3 | Google |
| G4N | Gemma 4N | Google |
| QWN | Qwen | Alibaba |
| LLM | Llama | Meta |
| PHI | Phi | Microsoft |
| MST | Mistral | Mistral AI |

**Examples:**
```
AVA-GE2-2B16.ALM    # Gemma 2, 2B params, FP16
AVA-GE3-4B16.ALM    # Gemma 3, 4B params, FP16
AVA-QWN-1B16.ALM    # Qwen, 1.5B params, FP16
```

---

## ADco - AVA Device Code

### Definition
`.ADco` (AVA Device Code) is AVA's standard extension for compiled object files.

### Types
1. **Library Code**: `AVALibrary.ADco` - Common TVM runtime functions
2. **Model Code**: `AVA-{MODEL}.ADco` - Model-specific compiled kernels

### Naming Convention
```
AVALibrary.ADco                    # Common library
AVA-{MODEL}{VERSION}-{SIZE}{BITS}.ADco  # Model-specific
```

---

## Implementation Guidelines

### Code References
```kotlin
// Correct - use AON extension
val modelPath = "models/AVA-384-Base-INT8.AON"

// Correct - library imports unchanged
import ai.onnxruntime.OrtSession

// Correct - fallback for backward compatibility
val aonFile = File(dir, "$modelId.AON")
val onnxFile = File(dir, "$modelId.onnx")
val modelFile = if (aonFile.exists()) aonFile else onnxFile
```

### Download URLs
```kotlin
// Source downloads use original extension
val huggingFaceUrl = "https://huggingface.co/.../model.onnx"

// Custom server uses AVA extension
val customServerUrl = "https://models.ava.ai/$modelId.AON"
```

### User Instructions
```bash
# Download from HuggingFace (original format)
curl -L https://huggingface.co/.../model.onnx -o model.onnx

# Rename to AVA format
mv model.onnx AVA-384-Base-INT8.AON

# Push to device
adb push AVA-384-Base-INT8.AON /sdcard/Android/data/com.augmentalis.ava/files/models/
```

---

## Storage Locations

### Android Device
```
/sdcard/Android/data/com.augmentalis.ava/files/
├── models/
│   ├── AVA-384-Base-INT8.AON      # NLU/RAG embedding model
│   └── llm/
│       ├── AVA-GE2-2B16/          # Gemma 2 2B
│       │   ├── AVA-GE2-2B16.ALM
│       │   └── params/
│       └── AVA-GE3-4B16/          # Gemma 3 4B
│           ├── AVA-GE3-4B16.ALM
│           └── params/
```

### APK Assets (Bundled)
```
assets/
├── models/
│   └── AVA-384-Base-INT8.AON      # Bundled NLU model
```

---

## Migration Notes

### From .onnx to .AON
1. Rename files: `mv model.onnx model.AON`
2. Update code references
3. Keep fallback to `.onnx` for backward compatibility

### Backward Compatibility
All model loading code should:
1. Check for `.AON` first
2. Fall back to `.onnx` if not found
3. Log which format was loaded

---

## Related Documents

- [Developer Manual Chapter 38: LLM Model Management](../Developer-Manual-Chapter38-LLM-Model-Management.md)
- [Developer Manual Chapter 42: LLM Model Setup](../Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [Model Setup Guide](../MODEL-SETUP.md)

---

## Changelog

### v2.0 (2025-11-19)
- Added AON extension for ONNX models
- Comprehensive naming conventions
- Implementation guidelines

### v1.0 (2025-11-17)
- Initial ALM and ADco extensions
- Basic naming convention
