# AVA File Formats Reference

**Author:** Manoj Jhawar
**Version:** 1.0
**Updated:** 2025-12-03

## Overview

AVA uses proprietary encrypted file formats for model storage and distribution. This document specifies all AVA-specific file formats.

## File Format Summary

| Extension | Full Name | Purpose | Encrypted |
|-----------|-----------|---------|-----------|
| `.adm` | Ava Data Module | LLM model weights | Yes |
| `.ats` | Ava Tokenizer State | Tokenizer configuration | Yes |
| `.amm` | Ava Model MLC | MLC-LLM compiled model | No |
| `.aon` | Ava ONNX Network | NLU model (ONNX) | Yes |
| `.aot` | Ava Ontology Template | Intent definitions (JSON) | No |
| `.ava` | Ava Intent File | Intent training data | No |
| `.alm` | Ava Language Model | Model archive (tar) | No |

## LLM Model Formats

### .adm (Ava Data Module)

**Purpose:** Encrypted LLM model weights and architecture.

**Structure:**
```
Header (32 bytes):
  - Magic: "AVAMOD01" (8 bytes)
  - Version: uint32 (4 bytes)
  - Model Type: uint32 (4 bytes)
  - Encrypted Size: uint64 (8 bytes)
  - Checksum: uint64 (8 bytes)

Payload:
  - AES-256-GCM encrypted model data
```

**Model Types:**
| ID | Name | Description |
|----|------|-------------|
| 1 | GGUF | llama.cpp quantized |
| 2 | MLC | TVM-compiled |
| 3 | ONNX | ONNX format |

**Location:** `/sdcard/ava-ai-models/llm/MODEL_NAME/`

**Example Files:**
- `AVA-GE2-2B16.adm` - Gemma 2B model
- `AVA-GE3-4B16.adm` - Gemma 4B model

### .ats (Ava Tokenizer State)

**Purpose:** Encrypted HuggingFace tokenizer configuration.

**Structure:**
```
Header (16 bytes):
  - Magic: "AVATOKS1" (8 bytes)
  - Version: uint32 (4 bytes)
  - Flags: uint32 (4 bytes)

Payload:
  - AES-256-GCM encrypted tokenizer.json
```

**Flags:**
| Bit | Name | Description |
|-----|------|-------------|
| 0 | BPE | Byte-pair encoding |
| 1 | WordPiece | WordPiece tokenizer |
| 2 | SentencePiece | SentencePiece tokenizer |
| 3 | HasSpecialTokens | Contains special tokens map |

**Location:** Same directory as corresponding `.adm` file.

### .amm (Ava Model MLC)

**Purpose:** MLC-LLM compiled model directory (unencrypted).

**Structure:**
```
MODEL_NAME/
├── mlc-chat-config.json    # Model configuration
├── tokenizer.json          # HuggingFace tokenizer
├── tokenizer_config.json   # Tokenizer settings
├── params_shard_0.bin      # Weight shard 0
├── params_shard_1.bin      # Weight shard 1
└── ...                     # Additional shards
```

**Note:** This is the default MLC-LLM output format. For distribution, convert to `.adm` + `.ats`.

## NLU Model Formats

### .aon (Ava ONNX Network)

**Purpose:** Encrypted ONNX model for NLU inference (embedding, classification).

**Structure:**
```
Header (32 bytes):
  - Magic: "AVAONNX1" (8 bytes)
  - Version: uint32 (4 bytes)
  - Model Type: uint32 (4 bytes)
  - Input Dim: uint32 (4 bytes)
  - Output Dim: uint32 (4 bytes)
  - Reserved: uint64 (8 bytes)

Payload:
  - AES-256-GCM encrypted ONNX model
```

**Model Types:**
| ID | Name | Description |
|----|------|-------------|
| 1 | Embedding | Text embedding model |
| 2 | Classification | Intent classifier |
| 3 | NER | Named entity recognition |

**Location:** `app/src/main/assets/models/`

**Example Files:**
- `AVA-384-Base-INT8.aon` - 384-dim embedding model

### .aot (Ava Ontology Template)

**Purpose:** JSON intent definitions (unencrypted).

**Schema:** `ava-ontology-3.0`

**Structure:**
```json
{
  "metadata": {
    "schema": "ava-ontology-3.0",
    "version": "3.0.0",
    "category": "category_name",
    "filename": "category.aot",
    "locale": "en-US"
  },
  "intents": [
    {
      "id": "intent_id",
      "name": "Intent Name",
      "description": "What this intent does",
      "utterances": ["example 1", "example 2"],
      "entities": [...],
      "actions": [...]
    }
  ]
}
```

**Location:** `app/src/main/assets/ontology/en-US/`

**Categories:**
- `communication.aot` (3 intents)
- `device_control.aot` (8 intents)
- `media.aot` (6 intents)
- `navigation.aot` (5 intents)
- `productivity.aot` (6 intents)

### .ava (Ava Intent File)

**Purpose:** Intent training data in AVA universal format.

**Schema:** `avu-1.0`

**Structure:**
```yaml
# AVA Intent File (avu-1.0)
# Intent: intent_id
# Category: category_name

intent: intent_id
category: category_name
examples:
  - "training example 1"
  - "training example 2"
  - ...
```

**Location:** `tools/embedding-generator/intents/`

## Archive Formats

### .alm (Ava Language Model)

**Purpose:** Tar archive containing complete model package.

**Structure:**
```
model.alm (tar archive)
├── model.adm           # Encrypted weights
├── tokenizer.ats       # Encrypted tokenizer
├── config.json         # Model configuration
└── manifest.json       # Package manifest
```

**Manifest Schema:**
```json
{
  "name": "AVA-GE3-4B16",
  "version": "1.0.0",
  "model_type": "gemma",
  "quantization": "int16",
  "size_bytes": 4294967296,
  "sha256": "..."
}
```

## Encryption Details

### Algorithm

- **Cipher:** AES-256-GCM
- **Key Derivation:** HKDF-SHA256
- **Nonce:** 12 bytes (random per file)
- **Auth Tag:** 16 bytes

### Key Management

Keys are derived from:
1. Device-specific hardware ID
2. App signing certificate
3. User unlock credential (optional)

This ensures:
- Models are bound to specific device
- Cannot be extracted and used elsewhere
- Optional user authentication

## File Placement Guide

```
/sdcard/ava-ai-models/
├── llm/
│   ├── AVA-GE2-2B16/
│   │   ├── AVA-GE2-2B16.adm
│   │   ├── tokenizer.ats
│   │   └── mlc-chat-config.json
│   └── AVA-GE3-4B16/
│       ├── AVA-GE3-4B16.adm
│       ├── tokenizer.ats
│       └── mlc-chat-config.json
└── nlu/
    └── embedding/
        └── AVA-384-Base-INT8.aon

app/src/main/assets/
├── models/
│   └── AVA-384-Base-INT8.aon
└── ontology/
    └── en-US/
        ├── communication.aot
        ├── device_control.aot
        ├── media.aot
        ├── navigation.aot
        └── productivity.aot
```

## Conversion Tools

### GGUF to ADM
```bash
python tools/model-converter/gguf_to_adm.py \
  --input model.gguf \
  --output model.adm \
  --key-file device.key
```

### Tokenizer to ATS
```bash
python tools/model-converter/tokenizer_to_ats.py \
  --input tokenizer.json \
  --output tokenizer.ats \
  --key-file device.key
```

### ONNX to AON
```bash
python tools/model-converter/onnx_to_aon.py \
  --input model.onnx \
  --output model.aon \
  --key-file device.key
```

## Validation

### Check File Integrity
```bash
python tools/model-validator/validate.py \
  --file model.adm \
  --key-file device.key
```

### Verify Manifest
```bash
python tools/model-validator/verify_manifest.py \
  --archive model.alm
```
