# Developer Manual - Chapter 45: AVA LLM Naming Standard

**Date:** 2025-11-20
**Version:** 1.0
**Status:** Active

---

## Overview

This chapter documents the AVA LLM Naming Standard, a comprehensive system for renaming and branding Large Language Models as native AVA models. This standard makes externally sourced models (Gemma, Llama, Mistral, etc.) appear as first-party AVA models while preserving technical accuracy and traceability.

**Purpose:**
- Brand LLMs as AVA's proprietary models
- Maintain clear technical specifications
- Preserve model provenance for developers
- Create consistent user-facing experience

---

## Table of Contents

1. [AVA Model Names (User-Facing)](#ava-model-names-user-facing)
2. [Technical Architecture](#technical-architecture)
3. [Naming Components](#naming-components)
4. [Complete Model Catalog](#complete-model-catalog)
5. [File Structure Standard](#file-structure-standard)
6. [Configuration Updates](#configuration-updates)
7. [UI/UX Integration](#uiux-integration)
8. [Migration Guide](#migration-guide)

---

## AVA Model Names (User-Facing)

### Primary Brand Names

AVA models are branded with descriptive names that hide the underlying architecture:

| AVA Brand Name | Base Model | Size | Description |
|----------------|-----------|------|-------------|
| **AVA Nexus** | Gemma 3 | 4B | Flagship multilingual model (140+ languages) |
| **AVA Core** | Gemma 2 | 2B | Compact English-focused model |
| **AVA Ultra** | Gemma 4 Nano | 4B | Next-gen efficiency model |
| **AVA Quantum** | Llama 3.1 | 8B | Advanced reasoning model |
| **AVA Titan** | Llama 3.1 | 70B | Maximum capability model |
| **AVA Swift** | Phi-3.5 | 3.8B | Speed-optimized model |
| **AVA Sage** | Mistral | 7B | Expert reasoning model |

### Naming Philosophy

**User-Facing (Marketing):**
- "AVA Nexus" - Sounds like a proprietary AVA model
- No mention of "Gemma" or "Google"
- Focus on capabilities, not origins

**Developer-Facing (Technical):**
- Full technical name: `AVA-GE3-4B16`
- Clear model provenance in docs
- Traceability to upstream model

**Legal Compliance:**
- Model licenses preserved in `LICENSE.model` file
- Attribution in developer docs
- No misrepresentation of model origins

---

## Technical Architecture

### Three-Layer Naming System

```
┌─────────────────────────────────────────────────────────────────┐
│                    LAYER 1: BRAND NAME                          │
│                      (User-Facing)                              │
│                      "AVA Nexus"                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  LAYER 2: TECHNICAL CODE                        │
│                   (Developer-Facing)                            │
│                     AVA-GE3-4B16                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 LAYER 3: UPSTREAM SOURCE                        │
│                  (Documentation Only)                           │
│            "Based on Gemma 3 4B (Google)"                       │
└─────────────────────────────────────────────────────────────────┘
```

### Naming Convention Format

```
Brand Name:     AVA {Codename}
Technical ID:   AVA-{MODEL}-{SIZE}{QUANT}
File Structure: /models/llm/{Technical-ID}/
Display Name:   "AVA {Codename} {Capability}"
```

---

## Naming Components

### 1. AVA Brand Codenames

**Tier 1: Flagship Models (4B-8B)**
- **Nexus** - Multilingual flagship (Gemma 3 4B)
- **Quantum** - Advanced reasoning (Llama 3.1 8B)
- **Ultra** - Next-gen efficiency (Gemma 4N 4B)

**Tier 2: Compact Models (2B-3B)**
- **Core** - Essential capabilities (Gemma 2 2B)
- **Swift** - Speed-optimized (Phi-3.5 3.8B)

**Tier 3: Advanced Models (7B-70B)**
- **Sage** - Expert reasoning (Mistral 7B)
- **Titan** - Maximum capability (Llama 3.1 70B)

### 2. Technical Model Codes

| Code | Model Family | Versions |
|------|-------------|----------|
| **GE2** | Gemma 2 | 2B, 9B, 27B |
| **GE3** | Gemma 3 | 2B, 4B, 9B, 27B |
| **G4N** | Gemma 4 Nano | 2B, 4B |
| **LL2** | Llama 2 | 7B, 13B, 70B |
| **LL3** | Llama 3 / 3.1 | 8B, 70B, 405B |
| **MST** | Mistral | 7B, 8x7B, 8x22B |
| **PH2** | Phi-2 | 2.7B |
| **PH3** | Phi-3 / 3.5 | 3.8B, 7B, 14B |
| **QW2** | Qwen 2 | 0.5B, 1.5B, 7B, 72B |

### 3. Size Notation

| Code | Meaning | Parameters |
|------|---------|------------|
| `2B` | 2 Billion | ~2,000,000,000 |
| `3B` | 3 Billion | ~3,800,000,000 (Phi-3.5) |
| `4B` | 4 Billion | ~4,000,000,000 |
| `7B` | 7 Billion | ~7,000,000,000 |
| `8B` | 8 Billion | ~8,000,000,000 |
| `13B` | 13 Billion | ~13,000,000,000 |
| `70B` | 70 Billion | ~70,000,000,000 |

### 4. Quantization Suffix

| Code | Format | Description |
|------|--------|-------------|
| `16` | q4f16_1 / q4bf16_1 | 4-bit weights, 16-bit activations |
| `8` | q8f16_1 | 8-bit weights, 16-bit activations |
| `4` | q4 | 4-bit quantization |

---

## Complete Model Catalog

### Currently Deployed

| Brand Name | Technical ID | Base Model | Size | Languages | Use Case |
|-----------|--------------|-----------|------|-----------|----------|
| **AVA Core** | AVA-GE2-2B16 | Gemma 2 2B | ~1.2GB | English | Compact, fast responses |
| **AVA Nexus** | AVA-GE3-4B16 | Gemma 3 4B | ~2.3GB | 140+ | Multilingual flagship |

### Planned Models

| Brand Name | Technical ID | Base Model | Size | Priority | Status |
|-----------|--------------|-----------|------|----------|--------|
| **AVA Ultra** | AVA-G4N-4B16 | Gemma 4N 4B | ~2.3GB | High | Q1 2026 |
| **AVA Swift** | AVA-PH3-4B16 | Phi-3.5 3.8B | ~2.2GB | Medium | Q2 2026 |
| **AVA Quantum** | AVA-LL3-8B16 | Llama 3.1 8B | ~4.5GB | Medium | Q2 2026 |
| **AVA Sage** | AVA-MST-7B16 | Mistral 7B | ~4.0GB | Low | Q3 2026 |
| **AVA Titan** | AVA-LL3-70B8 | Llama 3.1 70B | ~35GB | Future | 2027+ |

---

## File Structure Standard

### Directory Naming

```
/sdcard/Android/data/com.augmentalis.ava/files/models/llm/
├── AVA-GE2-2B16/              # AVA Core
├── AVA-GE3-4B16/              # AVA Nexus
├── AVA-G4N-4B16/              # AVA Ultra
├── AVA-PH3-4B16/              # AVA Swift
├── AVA-LL3-8B16/              # AVA Quantum
└── AVA-MST-7B16/              # AVA Sage
```

### File Contents

**Standard LLM Model Directory (MLC Format .amm):**
```
AVA-GE3-4B16/
├── AVALibrary.adm                  # MLC runtime (.adm = Ava Device MLC)
├── AVA-GE3-4B16.adm               # Model device code (163MB)
├── mlc-chat-config.json           # Model configuration
├── ndarray-cache.json             # Weight shard mapping
├── tokenizer.ats                   # SentencePiece tokenizer (.ats)
├── tokenizer_config.json          # Tokenizer settings
├── params_shard_0.bin             # Model weights
├── params_shard_1.bin
├── ...
├── params_shard_68.bin
├── LICENSE.model                   # Model license
└── AVA-MODEL-INFO.json            # AVA metadata
```

**GGUF Model Directory (.amg):**
```
AVA-GE3N-4BQ4/
├── AVA-GE3N-4BQ4.amg              # AMG container (GGUF format)
├── manifest.json                   # AVA metadata
├── model.gguf                      # GGUF model file
├── tokenizer.ats                   # SentencePiece tokenizer
└── config.json                     # Model configuration
```

**LiteRT Model Directory (.amr):**
```
AVA-GE3N-4BQ4/
├── AVA-GE3N-4BQ4.amr              # AMR container (LiteRT format)
├── manifest.json                   # AVA metadata
├── model.task                      # MediaPipe task bundle
├── tokenizer.ats                   # SentencePiece tokenizer
└── config.json                     # Model configuration
```

### New Required Files

#### LICENSE.model
Contains the upstream model license:

```text
This model is based on Gemma 3 4B developed by Google.

Original Model License: Gemma Terms of Use
https://ai.google.dev/gemma/terms

AVA Modifications:
- Compiled with TVM v0.22.0 for Android
- Quantized to q4bf16_1 format
- Optimized for on-device inference
- Retokenized for ALC runtime

AVA does not claim ownership of the base model architecture.
All modifications are provided under the same license terms.
```

#### AVA-MODEL-INFO.json
AVA-specific metadata:

```json
{
  "ava_model_name": "AVA Nexus",
  "ava_model_id": "AVA-GE3-4B16",
  "ava_codename": "Nexus",
  "base_model": "Gemma 3 4B",
  "base_model_provider": "Google",
  "base_model_license": "Gemma Terms of Use",
  "ava_version": "1.0",
  "compiled_date": "2025-11-20",
  "tvm_version": "0.22.0",
  "quantization": "q4bf16_1",
  "target_platform": "android-arm64-opencl",
  "context_length": 8192,
  "languages": 140,
  "capabilities": [
    "multilingual",
    "code_generation",
    "reasoning",
    "creative_writing"
  ],
  "recommended_use": "General-purpose multilingual assistant",
  "size_on_disk_mb": 2340,
  "memory_required_mb": 3200,
  "min_android_version": 26
}
```

---

## Configuration Updates

### mlc-chat-config.json Updates

**Before (Original):**
```json
{
  "model_type": "gemma3",
  "model_lib": "gemma3_q4bf16_1",
  "local_id": "gemma-3-4b-it-q4bf16_1-MLC",
  ...
}
```

**After (AVA Branded):**
```json
{
  "model_type": "gemma3",
  "model_lib": "AVA-GE3-4B16",
  "local_id": "AVA-GE3-4B16",
  "ava_brand_name": "AVA Nexus",
  "ava_codename": "Nexus",
  "ava_display_name": "AVA Nexus (Multilingual)",
  ...
}
```

**Key Changes:**
- `model_lib`: Update to AVA technical ID
- `local_id`: Update to AVA technical ID
- Add `ava_brand_name`, `ava_codename`, `ava_display_name`

---

## UI/UX Integration

### Model Selection UI

**Current (Technical):**
```
┌─────────────────────────────────┐
│ Select LLM Model:               │
├─────────────────────────────────┤
│ ○ AVA-GE2-2B16                 │
│ ○ AVA-GE3-4B16                 │
└─────────────────────────────────┘
```

**New (Branded):**
```
┌─────────────────────────────────┐
│ Select AI Model:                │
├─────────────────────────────────┤
│ ○ AVA Core                      │
│   Fast, English-focused         │
│   1.2GB · English               │
│                                 │
│ ● AVA Nexus                     │
│   Flagship, multilingual        │
│   2.3GB · 140+ languages        │
│                                 │
│ ○ AVA Swift (Coming Soon)      │
│   Speed-optimized               │
│   2.2GB · Multilingual          │
└─────────────────────────────────┘
```

### Chat Interface Display

**Header Display:**
```
┌─────────────────────────────────┐
│ AVA Nexus                  ⋮    │  ← Brand name
│ Powered by on-device AI         │  ← Generic description
└─────────────────────────────────┘
```

### Settings Display

**Model Information:**
```
Current Model: AVA Nexus
Status: Active
Memory: 2.3GB / 3.2GB
Languages: 140+
Version: 1.0

[Technical Details]  [Change Model]
```

**Technical Details (Expandable):**
```
Technical ID: AVA-GE3-4B16
Base Architecture: Gemma 3 4B
Quantization: q4bf16_1
TVM Version: 0.22.0
License: Gemma Terms of Use
```

---

## Migration Guide

### Step 1: Rename Model Directories

```bash
# On device
cd /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Rename existing models
mv AVA-GEM-2B-Q4 AVA-GE2-2B16
mv AVA-GEM-4B-Q4 AVA-GE3-4B16  # If exists
```

### Step 2: Rename Device Code Files

```bash
cd AVA-GE2-2B16/
mv lib0.o AVALibrary.adm        # .adm = Ava Device MLC
mv gemma_q4f16_1_devc.o AVA-GE2-2B16.adm

cd ../AVA-GE3-4B16/
mv lib0.o AVALibrary.adm
mv gemma3_q4bf16_1_devc.o AVA-GE3-4B16.adm
```

### Step 3: Update mlc-chat-config.json

```bash
# Edit mlc-chat-config.json in each model directory
nano AVA-GE2-2B16/mlc-chat-config.json
```

Add AVA metadata:
```json
{
  "model_lib": "AVA-GE2-2B16",
  "local_id": "AVA-GE2-2B16",
  "ava_brand_name": "AVA Core",
  "ava_codename": "Core",
  "ava_display_name": "AVA Core (English)",
  ...
}
```

### Step 4: Create AVA Metadata Files

```bash
# Create LICENSE.model
cat > AVA-GE3-4B16/LICENSE.model <<'EOF'
This model is based on Gemma 3 4B developed by Google.
...
EOF

# Create AVA-MODEL-INFO.json
cat > AVA-GE3-4B16/AVA-MODEL-INFO.json <<'EOF'
{
  "ava_model_name": "AVA Nexus",
  ...
}
EOF
```

### Step 5: Update Code References

**ModelSelector.kt:**
```kotlin
object ModelSelector {
    val models = listOf(
        ModelInfo(
            id = "AVA-GE2-2B16",
            name = "AVA Core",
            displayName = "AVA Core (English)",
            codename = "Core",
            description = "Fast, English-focused model",
            baseModel = "Gemma 2 2B",
            sizeGB = 1.2f,
            languages = listOf("en"),
            capabilities = listOf("chat", "reasoning")
        ),
        ModelInfo(
            id = "AVA-GE3-4B16",
            name = "AVA Nexus",
            displayName = "AVA Nexus (Multilingual)",
            codename = "Nexus",
            description = "Flagship multilingual model",
            baseModel = "Gemma 3 4B",
            sizeGB = 2.3f,
            languages = listOf("140+"),
            capabilities = listOf("multilingual", "chat", "code", "reasoning")
        )
    )
}
```

**ChatPreferences.kt:**
```kotlin
class ChatPreferences {
    private val _selectedModel = MutableStateFlow("AVA-GE3-4B16")

    fun getModelDisplayName(): String {
        return when (_selectedModel.value) {
            "AVA-GE2-2B16" -> "AVA Core"
            "AVA-GE3-4B16" -> "AVA Nexus"
            "AVA-G4N-4B16" -> "AVA Ultra"
            "AVA-PH3-4B16" -> "AVA Swift"
            else -> "AVA Model"
        }
    }
}
```

### Step 6: Update UI Components

**ChatScreen.kt:**
```kotlin
Text(
    text = when (selectedModel) {
        "AVA-GE2-2B16" -> "AVA Core"
        "AVA-GE3-4B16" -> "AVA Nexus"
        else -> "AVA"
    },
    style = MaterialTheme.typography.titleLarge
)
```

---

## Legal Compliance

### Required Attributions

**In App (Settings → About → Model Licenses):**
```
AVA Nexus is based on Gemma 3 4B developed by Google.
Licensed under Gemma Terms of Use.
https://ai.google.dev/gemma/terms

AVA Core is based on Gemma 2 2B developed by Google.
Licensed under Gemma Terms of Use.
https://ai.google.dev/gemma/terms
```

**In Developer Documentation:**
All technical documentation must include base model attribution:
- Model name
- Original developer
- License
- AVA modifications

**In Source Code:**
```kotlin
/**
 * AVA Nexus - Flagship multilingual AI model
 *
 * Base Model: Gemma 3 4B (Google)
 * License: Gemma Terms of Use
 * AVA Modifications: TVM v0.22.0 compilation, q4bf16_1 quantization
 *
 * This is a derivative work. AVA does not claim ownership of the
 * base model architecture.
 */
```

---

## Brand Positioning

### Marketing Messaging

**User-Facing:**
- "Powered by AVA Nexus AI"
- "AVA's flagship multilingual model"
- "On-device AI with AVA Core"
- ❌ Never mention "Gemma" or "Google" in user-facing text

**Developer-Facing:**
- "AVA Nexus (based on Gemma 3 4B)"
- "Built on Google's Gemma architecture"
- "Licensed under Gemma Terms of Use"

### Differentiation Points

What makes it "AVA's model":
1. **Custom compilation** - TVM v0.22.0 (not standard MLC-LLM)
2. **Optimized runtime** - AVA's custom Android runtime
3. **Integration** - Deep integration with AVA ecosystem
4. **Retokenization** - Optimized for ALC engine
5. **On-device focus** - Specifically tuned for mobile inference
6. **Privacy features** - Enhanced with AVA's privacy layer

---

## Summary

### Key Principles

1. **Brand Ownership** - User-facing names are AVA brands (Nexus, Core, etc.)
2. **Technical Accuracy** - Developer docs show full provenance
3. **Legal Compliance** - All licenses preserved and attributed
4. **Consistent Experience** - Unified naming across all touchpoints
5. **Traceability** - Technical IDs map to upstream models

### Naming Layers

| Layer | Audience | Example | Use |
|-------|----------|---------|-----|
| Brand | Users | "AVA Nexus" | UI, marketing, user docs |
| Technical | Developers | "AVA-GE3-4B16" | Code, file structure, technical docs |
| Provenance | Compliance | "Gemma 3 4B (Google)" | Licenses, attributions |

### Implementation Status

- ✅ Naming standard defined
- ✅ Model catalog created
- ✅ File structure specified
- ⏳ Code migration (pending)
- ⏳ UI updates (pending)
- ⏳ Documentation updates (pending)

---

## Reference

- [Chapter 44: AVA Naming Convention v2](Developer-Manual-Chapter44-AVA-Naming-Convention.md)
- [Chapter 42: LLM Model Setup](Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [Chapter 38: LLM Model Management](Developer-Manual-Chapter38-LLM-Model-Management.md)

---

**Document Version:** 2.0
**Last Updated:** 2025-12-01
**Author:** AVA Development Team
**Status:** Active Standard
