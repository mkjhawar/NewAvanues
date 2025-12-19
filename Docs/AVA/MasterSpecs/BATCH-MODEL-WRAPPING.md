# Batch Model Wrapping Guide

**Version:** 1.0
**Last Updated:** 2025-11-24
**For:** AVA Build Engineers & DevOps

---

## Overview

This guide explains how to batch wrap multiple ONNX embedding models into AVA-AON format for deployment.

**Use cases:**
- Preparing models for new AVA release
- Creating model packages for different app tiers (Free/Pro)
- Updating models with new package whitelist
- Testing model deployment pipeline

---

## Quick Start

### Option 1: Kotlin Batch Wrapper (Recommended)

```bash
cd /Volumes/M-Drive/Coding/AVA

./gradlew :Universal:AVA:Features:RAG:run \
  --args="batch \
    --input /path/to/onnx-models \
    --output /path/to/aon-models \
    --strategy AVA_STANDARD"
```

**Output:**
```
/path/to/aon-models/
├── free/
│   ├── AVA-384-Base-INT8.AON    (90 MB)
│   └── AVA-384-Fast-INT8.AON    (61 MB)
└── pro/
    ├── AVA-768-Qual-INT8.AON    (420 MB)
    ├── AVA-384-Multi-INT8.AON   (470 MB)
    ├── AVA-768-Multi-INT8.AON   (1.1 GB)
    ├── AVA-384-ZH-INT8.AON      (220 MB)
    └── AVA-768-JA-INT8.AON      (340 MB)
```

### Option 2: Bash Script

```bash
cd /Volumes/M-Drive/Coding/AVA

./scripts/wrap-all-rag-models.sh \
  -i /path/to/onnx-models \
  -o /path/to/aon-models \
  -s AVA_STANDARD
```

---

## Model Registry

The batch wrapper processes models defined in `BatchModelWrapper.MODEL_REGISTRY`:

| Model ID | ONNX Filename | Tier | Size | Languages |
|----------|---------------|------|------|-----------|
| AVA-384-Base-INT8 | all-MiniLM-L6-v2.onnx | Free | 90 MB | English |
| AVA-384-Fast-INT8 | paraphrase-MiniLM-L3-v2.onnx | Free | 61 MB | English |
| AVA-768-Qual-INT8 | all-mpnet-base-v2.onnx | Pro | 420 MB | English |
| AVA-384-Multi-INT8 | paraphrase-multilingual-MiniLM-L12-v2.onnx | Pro | 470 MB | 50+ |
| AVA-768-Multi-INT8 | paraphrase-multilingual-mpnet-base-v2.onnx | Pro | 1.1 GB | 50+ |
| AVA-384-ZH-INT8 | text2vec-base-chinese.onnx | Pro | 220 MB | Chinese |
| AVA-768-JA-INT8 | sentence-bert-base-ja-mean-tokens-v2.onnx | Pro | 340 MB | Japanese |

**Total:** 7 models, ~2.5 GB combined

---

## Adding New Models

### Step 1: Download ONNX Model

```bash
# Example: Download from HuggingFace
cd /path/to/onnx-models
wget https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx
mv model.onnx all-MiniLM-L6-v2.onnx
```

### Step 2: Add to Registry

Edit `Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/BatchModelWrapper.kt`:

```kotlin
companion object {
    val MODEL_REGISTRY = listOf(
        // ... existing models ...

        // NEW MODEL
        ModelRegistryEntry(
            modelId = "AVA-1024-Code-INT8",
            onnxFilename = "codebert-base.onnx",
            licenseTier = 1,  // 0=free, 1=pro, 2=enterprise
            modelVersion = 1
        )
    )
}
```

### Step 3: Run Batch Wrapper

```bash
./gradlew :Universal:AVA:Features:RAG:run \
  --args="batch --input /path/to/onnx-models --output /path/to/aon-models"
```

### Step 4: Verify Output

```bash
ls -lh /path/to/aon-models/pro/AVA-1024-Code-INT8.AON

# Verify AON format
file /path/to/aon-models/pro/AVA-1024-Code-INT8.AON
# Should show: data (AVA-AON format)
```

**That's it!** The new model is now ready for deployment.

---

## Distribution Strategies

Choose the appropriate package whitelist strategy for your deployment:

### AVA_STANDARD (Default)
**Packages:**
- `com.augmentalis.ava` (AVA Standalone)
- `com.augmentalis.avaconnect` (AVA Connect)
- `com.augmentalis.voiceos` (VoiceOS)

**Use for:** Standard AVA ecosystem apps

```bash
--strategy AVA_STANDARD
```

### AVANUES_PLATFORM
**Packages:**
- `com.augmentalis.avanues` (Avanues Platform)
- `com.augmentalis.ava` (AVA Standalone)
- `com.augmentalis.avaconnect` (AVA Connect)

**Use for:** Avanues platform deployment

```bash
--strategy AVANUES_PLATFORM
```

### DEVELOPMENT
**Packages:**
- `com.augmentalis.ava.debug`
- `com.augmentalis.ava.staging`
- `com.augmentalis.ava.test`

**Use for:** Development and testing builds

```bash
--strategy DEVELOPMENT
```

### ALL_AVA
**Packages:**
- `com.augmentalis.ava`
- `com.augmentalis.avaconnect`
- `com.augmentalis.voiceos`

**Use for:** All primary AVA apps (same as AVA_STANDARD)

```bash
--strategy ALL_AVA
```

---

## Command-Line Options

### Kotlin Batch Wrapper

```bash
./gradlew :Universal:AVA:Features:RAG:run --args="batch [OPTIONS]"
```

| Option | Required | Description | Default |
|--------|----------|-------------|---------|
| `--input DIR` | Yes | Directory containing ONNX models | - |
| `--output DIR` | Yes | Output directory for AON files | - |
| `--strategy STR` | No | Distribution strategy | AVA_STANDARD |
| `--verbose` | No | Enable verbose output | false |
| `--dry-run` | No | Show what would be wrapped | false |

### Bash Script

```bash
./scripts/wrap-all-rag-models.sh [OPTIONS]
```

| Option | Required | Description | Default |
|--------|----------|-------------|---------|
| `-i, --input-dir DIR` | Yes | Directory containing ONNX models | - |
| `-o, --output-dir DIR` | No | Output directory for AON files | ./aon-models |
| `-s, --strategy STR` | No | Distribution strategy | AVA_STANDARD |
| `-v, --verbose` | No | Enable verbose output | false |
| `-n, --dry-run` | No | Show what would be wrapped | false |
| `-h, --help` | No | Show help message | - |

---

## Examples

### Example 1: Wrap All Models

```bash
# Prepare input directory
mkdir -p /tmp/onnx-models
cd /tmp/onnx-models

# Download ONNX models from HuggingFace
# (see Model Registry section for URLs)

# Wrap all models
cd /Volumes/M-Drive/Coding/AVA
./gradlew :Universal:AVA:Features:RAG:run \
  --args="batch \
    --input /tmp/onnx-models \
    --output /tmp/aon-models"

# Output:
# ✓ Wrapped: 7/7 models
# Output: /tmp/aon-models/free/ (2 models)
# Output: /tmp/aon-models/pro/ (5 models)
```

### Example 2: Dry Run (Preview)

```bash
./gradlew :Universal:AVA:Features:RAG:run \
  --args="batch \
    --input /tmp/onnx-models \
    --output /tmp/aon-models \
    --dry-run"

# Shows what would be wrapped without creating files
```

### Example 3: Wrap for Avanues Platform

```bash
./gradlew :Universal:AVA:Features:RAG:run \
  --args="batch \
    --input /tmp/onnx-models \
    --output /tmp/aon-models \
    --strategy AVANUES_PLATFORM \
    --verbose"
```

### Example 4: Wrap Only Free Tier

Manually run wrapper for specific models:

```bash
./gradlew :Universal:AVA:Features:RAG:run \
  --args="wrap \
    --input /tmp/onnx-models/all-MiniLM-L6-v2.onnx \
    --output /tmp/aon-models/free/AVA-384-Base-INT8.AON \
    --model-id AVA-384-Base-INT8 \
    --packages com.augmentalis.ava,com.augmentalis.avaconnect,com.augmentalis.voiceos \
    --license-tier 0"
```

---

## Output Structure

```
output-dir/
├── free/                          # Free tier models (license-tier=0)
│   ├── AVA-384-Base-INT8.AON
│   └── AVA-384-Fast-INT8.AON
└── pro/                           # Pro tier models (license-tier=1)
    ├── AVA-768-Qual-INT8.AON
    ├── AVA-384-Multi-INT8.AON
    ├── AVA-768-Multi-INT8.AON
    ├── AVA-384-ZH-INT8.AON
    └── AVA-768-JA-INT8.AON
```

**Deployment:**
- Upload `free/` to public CDN
- Upload `pro/` to authenticated CDN (license verification)

---

## Troubleshooting

### Error: "Input file not found"

**Problem:** ONNX file missing from input directory

**Solution:**
```bash
# Check input directory
ls -l /path/to/onnx-models/

# Ensure filenames match MODEL_REGISTRY
# Example: all-MiniLM-L6-v2.onnx (NOT model.onnx)
```

### Error: "Package list exceeds 3"

**Problem:** AON v1.0 supports maximum 3 packages

**Solution:**
- Use predefined strategies (AVA_STANDARD, AVANUES_PLATFORM)
- Create separate AON files for different app groups
- Wait for AON v2.0 (unlimited packages)

### Error: "HMAC signature failed"

**Problem:** Master key mismatch or corrupted file

**Solution:**
```bash
# Verify master key
echo $AVA_AON_MASTER_KEY

# Re-wrap model
./gradlew :Universal:AVA:Features:RAG:run \
  --args="wrap --input model.onnx --output model.AON ..."
```

### Error: "Gradle task not found"

**Problem:** RAG module not built

**Solution:**
```bash
# Build RAG module first
./gradlew :Universal:AVA:Features:RAG:build

# Then run batch wrapper
./gradlew :Universal:AVA:Features:RAG:run --args="batch ..."
```

---

## Performance

| Models | Total Size | Wrap Time | CPU | Memory |
|--------|------------|-----------|-----|--------|
| 7 models | 2.5 GB | ~30 seconds | 4 cores | 2 GB |
| 1 model (90 MB) | 90 MB | ~5 seconds | 1 core | 512 MB |

**Note:** Wrapping is I/O bound (reading/writing files), not CPU intensive.

---

## Security Best Practices

### 1. Master Key Management

**NEVER commit master key to Git:**
```bash
# Use environment variable
export AVA_AON_MASTER_KEY="$(openssl rand -hex 32)"

# Add to .gitignore
echo "*.key" >> .gitignore
```

**See:** `docs/backlog/SECURE-KEY-ROTATION.md` for production setup

### 2. Package Whitelist Verification

**Always use preset strategies:**
```kotlin
// GOOD
strategy = AONPackageManager.DistributionStrategy.AVA_STANDARD

// BAD - hardcoded packages
packages = listOf("com.augmentalis.ava", "com.example.untrusted", ...)
```

### 3. Model Integrity

**Verify wrapped models:**
```bash
# Check AON magic bytes
xxd /path/to/model.AON | head -1
# Should start with: 4156 412d 414f 4e01 (AVA-AON\x01)

# Verify HMAC
./gradlew :Universal:AVA:Features:RAG:run \
  --args="verify --input /path/to/model.AON"
```

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Wrap RAG Models

on:
  workflow_dispatch:
    inputs:
      strategy:
        description: 'Distribution strategy'
        required: true
        default: 'AVA_STANDARD'
        type: choice
        options:
          - AVA_STANDARD
          - AVANUES_PLATFORM
          - DEVELOPMENT

jobs:
  wrap-models:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Download ONNX models
        run: |
          mkdir -p onnx-models
          # Download from HuggingFace or S3
          # ...

      - name: Wrap models
        env:
          AVA_AON_MASTER_KEY: ${{ secrets.AVA_AON_MASTER_KEY }}
        run: |
          ./gradlew :Universal:AVA:Features:RAG:run \
            --args="batch \
              --input onnx-models \
              --output aon-models \
              --strategy ${{ inputs.strategy }}"

      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: aon-models
          path: aon-models/
```

### GitLab CI

```yaml
wrap_models:
  stage: build
  script:
    - mkdir -p onnx-models
    # Download ONNX models
    - ./gradlew :Universal:AVA:Features:RAG:run
        --args="batch
          --input onnx-models
          --output aon-models
          --strategy AVA_STANDARD"
  artifacts:
    paths:
      - aon-models/
  only:
    - tags
```

---

## Deployment Checklist

- [ ] Download all ONNX models from HuggingFace
- [ ] Verify ONNX model checksums
- [ ] Set `AVA_AON_MASTER_KEY` environment variable
- [ ] Choose distribution strategy (AVA_STANDARD, AVANUES_PLATFORM, etc.)
- [ ] Run batch wrapper with `--dry-run` first
- [ ] Review output summary (wrapped/skipped/failed counts)
- [ ] Run batch wrapper without `--dry-run`
- [ ] Verify AON magic bytes on wrapped files
- [ ] Test wrapped models on device (download + unwrap)
- [ ] Upload to CDN (free tier → public, pro tier → authenticated)
- [ ] Update model download URLs in `RAGModelDownloadScreen.kt`
- [ ] Test download UI in app
- [ ] Document model versions in release notes

---

## FAQ

### Can I wrap models in parallel?

**Yes.** The batch wrapper processes models sequentially, but you can run multiple batch wrappers in parallel for different model sets:

```bash
# Terminal 1: Free tier
./gradlew :Universal:AVA:Features:RAG:run \
  --args="wrap --input all-MiniLM-L6-v2.onnx ..." &

# Terminal 2: Pro tier
./gradlew :Universal:AVA:Features:RAG:run \
  --args="wrap --input all-mpnet-base-v2.onnx ..." &
```

### Can I wrap non-embedding models?

**Yes.** AON format works with any ONNX model (LLM, image, audio). Just update `MODEL_REGISTRY` with your model details.

### How do I unwrap for testing?

```bash
./gradlew :Universal:AVA:Features:RAG:run \
  --args="unwrap \
    --input /path/to/model.AON \
    --output /path/to/model.onnx \
    --package com.augmentalis.ava"
```

**Note:** Requires valid package name from whitelist.

### Can I change package whitelist after wrapping?

**No.** Package whitelist is signed with HMAC. You must re-wrap the model with new packages.

---

## Related Documentation

- **AON File Format:** `docs/AON-FILE-FORMAT.md` - Technical specification
- **Developer Guide:** `docs/DEVELOPER-GUIDE-AON-MODELS.md` - Integration guide
- **User Guide:** `docs/USER-GUIDE-RAG-MODELS.md` - End-user documentation
- **Backlog:** `docs/backlog/SECURE-KEY-ROTATION.md` - Master key rotation plan

---

**Last Updated:** 2025-11-24
**AVA Version:** 3.0+
**AON Format:** v1.0
