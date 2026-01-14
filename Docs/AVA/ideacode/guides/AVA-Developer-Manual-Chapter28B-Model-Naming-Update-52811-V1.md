# Developer Manual - Chapter 28B: RAG Model Naming Update

**Date:** 2025-11-28
**Version:** 1.0
**Status:** Active - Supersedes naming in Chapter 28
**Purpose:** Update all model references to AVA-AON naming convention v2

---

## Overview

This addendum updates Developer Manual Chapter 28 (RAG) to reflect the official **AVA-AON Naming Convention v2** (Chapter 44). All ONNX embedding models now use the `.AON` extension and standardized naming format.

**Key Change:**
- ❌ OLD: `all-minilm-l6-v2.onnx`, `model_quantized.onnx`
- ✅ NEW: `AVA-384-Base-INT8.AON`, `model_qint8_arm64.onnx`

---

## Naming Convention Summary

### Format
```
AVA-{dimension}-{variant}-{quantization}.AON
```

### Examples
| Old Name | New Name | Model |
|----------|----------|-------|
| `all-minilm-l6-v2.onnx` | `AVA-384-Base-INT8.AON` | all-MiniLM-L6-v2 (English) |
| `AVA-ONX-384-MULTI.onnx` | `AVA-384-Multi-INT8.AON` | paraphrase-multilingual-MiniLM-L12-v2 |
| `AVA-ONX-768-QUAL-INT8.onnx` | `AVA-768-Qual-INT8.AON` | all-mpnet-base-v2 (English, high quality) |

**See Developer Manual Chapter 44 for complete naming specification.**

---

## Updated Model References (Chapter 28)

### Section 28.1 - System Overview (Line 67)

**OLD:**
```
- English: all-MiniLM-L6-v2 (384 dimensions)
```

**NEW:**
```
- English: AVA-384-Base-INT8.AON (all-MiniLM-L6-v2, 384 dimensions)
- Multilingual: AVA-384-Multi-INT8.AON (paraphrase-multilingual-MiniLM-L12-v2, 384 dimensions)
```

---

### Section 28.6.1 - External Model Loading (Line 437-449)

**OLD:**
```kotlin
private val modelId: String = "all-MiniLM-L6-v2"
```

**NEW:**
```kotlin
private val modelId: String = "AVA-384-Base-INT8"  // Maps to all-MiniLM-L6-v2
private val modelFile: String = "$modelId.AON"
```

---

### Section 28.6.1 - Model Storage Paths (Line 478)

**OLD:**
```
External: /sdcard/Android/data/com.augmentalis.ava/files/models/all-MiniLM-L6-v2.onnx
```

**NEW:**
```
External: /sdcard/ava-ai-models/rag/AVA-384-Base-INT8.AON
```

**Note:** Storage location changed from app-private to shared `/sdcard/ava-ai-models/` for ecosystem-wide model sharing.

---

### Section 28.6.4 - Model Download Instructions (Lines 746-750, 854-910)

#### English Base Model (all-MiniLM-L6-v2)

**OLD:**
```bash
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx

adb push all-MiniLM-L6-v2.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
```

**NEW:**
```bash
# Step 1: Download quantized ONNX model (ARM64 optimized)
wget https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_arm64.onnx

# Step 2: Wrap in AON format using AONFileManager
# (Run in Kotlin/Android environment)
import com.augmentalis.ava.features.rag.embeddings.AONFileManager

val onnxFile = File("model_qint8_arm64.onnx")
val aonFile = File("AVA-384-Base-INT8.AON")

AONFileManager.wrapONNX(
    onnxFile = onnxFile,
    outputFile = aonFile,
    modelId = "AVA-384-Base-INT8",
    allowedPackages = listOf("com.augmentalis.ava")
)

# Step 3: Deploy to device
adb push AVA-384-Base-INT8.AON /sdcard/ava-ai-models/rag/
```

**Size:** ~23 MB (quantized for ARM64)

#### Multilingual Model (paraphrase-multilingual-MiniLM-L12-v2)

**OLD:**
```bash
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-MULTI.onnx

adb push AVA-ONX-384-MULTI.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
```

**NEW:**
```bash
# Step 1: Download quantized ONNX model (ARM64 optimized)
wget https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model_qint8_arm64.onnx

# Step 2: Wrap in AON format
import com.augmentalis.ava.features.rag.embeddings.AONFileManager

val onnxFile = File("model_qint8_arm64.onnx")
val aonFile = File("AVA-384-Multi-INT8.AON")

AONFileManager.wrapONNX(
    onnxFile = onnxFile,
    outputFile = aonFile,
    modelId = "AVA-384-Multi-INT8",
    allowedPackages = listOf("com.augmentalis.ava")
)

# Step 3: Deploy to device
adb push AVA-384-Multi-INT8.AON /sdcard/ava-ai-models/rag/
```

**Size:** ~90 MB (quantized for ARM64)
**Languages:** 50+ (English, Spanish, French, German, Chinese, Japanese, Arabic, Hindi, and more)

---

### Section 28.6.5 - Model Registry Table (Lines 949-951)

**OLD:**
```
| AVA-ONX-384-BASE.onnx | all-MiniLM-L6-v2 | 86 MB | English embeddings |
| AVA-ONX-384-MULTI.onnx | paraphrase-multilingual-MiniLM-L12-v2 | 470 MB | Multilingual embeddings |
| AVA-ONX-384-MULTI-INT8.onnx | paraphrase-multilingual-MiniLM-L12-v2 (quantized) | 117 MB | Multilingual (75% smaller) |
```

**NEW:**
```
| AVA-384-Base-INT8.AON | all-MiniLM-L6-v2 | 23 MB | English embeddings (INT8 quantized) |
| AVA-384-Multi-INT8.AON | paraphrase-multilingual-MiniLM-L12-v2 | 90 MB | Multilingual (50+ languages, INT8) |
| AVA-768-Qual-INT8.AON | all-mpnet-base-v2 | 120 MB | English high-quality (768-dim) |
```

**Note:** All models now use ARM64-optimized INT8 quantization (75% smaller than FP32).

---

### Section 28.6.5 - Model Registry Code (Lines 959-989)

**OLD:**
```kotlin
private val modelIdToOriginalName = mapOf(
    "AVA-ONX-384-BASE" to "all-MiniLM-L6-v2",
    ...
    "AVA-ONX-384-BASE-INT8" to "all-MiniLM-L6-v2",
    ...
)

// File loading
val avaModelFile = File(externalModelsDir, "$modelId.onnx")
val originalModelFile = File(externalModelsDir, "$originalModelId.onnx")
```

**NEW:**
```kotlin
private val modelIdToOriginalName = mapOf(
    "AVA-384-Base-INT8" to "all-MiniLM-L6-v2",
    "AVA-384-Multi-INT8" to "paraphrase-multilingual-MiniLM-L12-v2",
    "AVA-768-Qual-INT8" to "all-mpnet-base-v2",

    // Legacy compatibility (deprecated)
    "AVA-ONX-384-BASE-INT8" to "all-MiniLM-L6-v2",
    "AVA-ONX-384-MULTI-INT8" to "paraphrase-multilingual-MiniLM-L12-v2"
)

// File loading (AON format)
val avaModelFile = File(externalModelsDir, "$modelId.AON")

// Legacy fallback (deprecated, for migration)
val legacyOnnxFile = File(externalModelsDir, "$modelId.onnx")
if (!avaModelFile.exists() && legacyOnnxFile.exists()) {
    Log.w("ModelManager", "Using legacy .onnx file. Please migrate to .AON format")
}
```

---

### Section 28.8 - External Storage Directory Structure (Lines 1626-1634)

**OLD:**
```
/sdcard/Android/data/com.augmentalis.ava/files/models/
│   ├── AVA-ONX-384-BASE.onnx (86 MB)
│   └── AVA-ONX-384-MULTI.onnx (449 MB)
apps/ava-standalone/src/main/assets/models/
    ├── AVA-ONX-384-BASE-INT8.onnx (22 MB) ← Bundled in APK
    └── AVA-ONX-384-MULTI-INT8.onnx (113 MB) ← Available for download
```

**NEW:**
```
/sdcard/ava-ai-models/
├── embeddings/           # NLU models (AON format)
│   ├── AVA-384-Mobile-INT8.AON    # MobileBERT (25 MB)
│   ├── AVA-768-Multi-INT8.AON     # mALBERT multilingual (90 MB)
│   └── vocab.txt
├── rag/                  # RAG embedding models (AON format)
│   ├── AVA-384-Base-INT8.AON      # all-MiniLM-L6-v2 (23 MB)
│   ├── AVA-384-Multi-INT8.AON     # Multilingual (90 MB)
│   └── vocab.txt
└── llm/                  # LLM models (ALM format)
    ├── AVA-GE2-2B16.ALM/          # Gemma 2 2B
    └── AVA-GE3-4B16.ALM/          # Gemma 3 4B

apps/ava-app-android/src/main/assets/models/
├── AVA-384-Base-INT8.AON          # Bundled RAG model (22 MB)
├── mobilebert_int8.onnx           # Bundled NLU (25 MB, raw ONNX for size)
└── vocab.txt
```

**Key Changes:**
1. Shared storage: `/sdcard/ava-ai-models/` (ecosystem-wide)
2. Organized by purpose: `embeddings/`, `rag/`, `llm/`
3. Consistent AON format for all ONNX models
4. LLM models use `.ALM` format (tar archives)

---

## Migration Guide

### For Existing Installations

If you have models with old naming, follow these steps:

#### Step 1: Identify Old Models
```bash
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
```

Look for:
- `all-minilm-l6-v2.onnx`
- `AVA-ONX-*.onnx`
- Any `.onnx` files in old location

#### Step 2: Download Correct Models
```bash
# Download from Hugging Face (see instructions above)
wget https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_arm64.onnx
```

#### Step 3: Wrap in AON Format
```kotlin
// Use AONFileManager.wrapONNX() as shown in download instructions above
```

#### Step 4: Deploy to New Location
```bash
# Create new directory structure
adb shell mkdir -p /sdcard/ava-ai-models/rag
adb shell mkdir -p /sdcard/ava-ai-models/embeddings
adb shell mkdir -p /sdcard/ava-ai-models/llm

# Push models
adb push AVA-384-Base-INT8.AON /sdcard/ava-ai-models/rag/
adb push AVA-384-Multi-INT8.AON /sdcard/ava-ai-models/rag/
adb push vocab.txt /sdcard/ava-ai-models/rag/
```

#### Step 5: Clean Up Old Files (Optional)
```bash
# Remove old models after verifying new ones work
adb shell rm -rf /sdcard/Android/data/com.augmentalis.ava/files/models/*.onnx
```

---

## AON Format Benefits

### Why .AON instead of .onnx?

1. **Security**: HMAC-SHA256 authentication prevents tampering
2. **Package Whitelisting**: Only authorized apps can load models
3. **Versioning**: Built-in version tracking and compatibility checks
4. **Encryption**: Optional AES-256-GCM encryption for sensitive models
5. **Professional**: Clear AVA branding distinguishes from standard ONNX

### Technical Details

**Structure:**
```
┌─────────────────────────────────────┐
│  AON Header (256 bytes)             │  ← Magic: AVA-AON\x01
├─────────────────────────────────────┤
│  ONNX Model Data (variable)         │  ← Standard ONNX model
├─────────────────────────────────────┤
│  AON Footer (128 bytes)             │  ← HMAC-SHA256 integrity
└─────────────────────────────────────┘
```

**Reference:**
`Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/AONFileManager.kt`

---

## Corrected Hugging Face URLs

### ❌ WRONG (404 Error)
```
https://huggingface.co/.../model_quantized.onnx
```

### ✅ CORRECT (ARM64 Quantized)
```
https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model_qint8_arm64.onnx
https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model_qint8_arm64.onnx
```

**Key Pattern:** Always use `model_qint8_arm64.onnx` for Android ARM devices, NOT `model_quantized.onnx` (which doesn't exist).

---

## Quick Reference Table

| Task | Old Way | New Way |
|------|---------|---------|
| **Model Name** | `all-minilm-l6-v2.onnx` | `AVA-384-Base-INT8.AON` |
| **Download URL** | `model_quantized.onnx` (404) | `model_qint8_arm64.onnx` ✅ |
| **File Format** | Raw ONNX | AON-wrapped ONNX |
| **Storage Path** | `/sdcard/Android/data/.../files/models/` | `/sdcard/ava-ai-models/rag/` |
| **Size** | 86 MB (FP32) | 23 MB (INT8) |

---

## Related Documentation

- **Chapter 44:** AVA Naming Convention v2 (complete specification)
- **Chapter 28:** RAG System Architecture (base documentation)
- **Chapter 50:** External Storage Migration (storage paths)
- **ava-testing-guide.md:** Step-by-step setup instructions
- **ava-file-locations.md:** File inventory and locations

---

## Implementation Checklist

When updating any RAG-related code or documentation:

- [ ] Use `.AON` extension (not `.onnx`)
- [ ] Follow `AVA-{dim}-{variant}-{quant}.AON` naming
- [ ] Use `/sdcard/ava-ai-models/` storage path
- [ ] Download `model_qint8_arm64.onnx` from Hugging Face
- [ ] Wrap with `AONFileManager.wrapONNX()`
- [ ] Update model registry mappings
- [ ] Document ARM64 quantization (23 MB vs 86 MB)
- [ ] Reference Chapter 44 for naming rules

---

**Version:** 1.0
**Date:** 2025-11-28
**Status:** Active - Supersedes Chapter 28 model naming
**Author:** AVA AI Team
