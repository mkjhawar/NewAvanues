# LLM Model Installation Guide for AVA

**Date:** 2025-11-20
**Version:** 1.0
**Target:** AVA AI App v1.0+

---

## Overview

This guide provides complete instructions for installing LLM models on your Android device for use with AVA. Models are too large to bundle in the APK (2-4GB each) and must be sideloaded separately.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Device Storage Structure](#device-storage-structure)
3. [Available Models](#available-models)
4. [Installation Methods](#installation-methods)
5. [Model File Locations (Development)](#model-file-locations-development)
6. [Verification](#verification)
7. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites

1. AVA AI app installed on Android device
2. USB cable or WiFi connection to device
3. Android Debug Bridge (adb) installed on computer
4. At least 3GB free storage on device

### Basic Installation (Recommended: Gemma 2 2B)

```bash
# 1. Connect device via USB
adb devices

# 2. Create model directory
adb shell mkdir -p /sdcard/ava-llm/models/

# 3. Push model files (see sections below for file locations)
adb push <model-directory> /sdcard/ava-llm/models/

# 4. Launch AVA and select model in settings
```

---

## Device Storage Structure

### Target Location on Device

All LLM models must be placed in this simplified directory:

```
/sdcard/ava-llm/models/
```

**Note:** This is a cleaner path structure recommended for AVA. The app will check both the new simplified path and the legacy Android scoped storage path for compatibility.

### Full Directory Structure

```
/sdcard/ava-llm/
└── models/                            ← CREATE THIS DIRECTORY
    ├── AVA-GE2-2B16/                  ← Gemma 2 2B model
    │   ├── AVALibrary.ADco            ← Renamed from lib0.o
    │   ├── AVA-GE2-2B16.ADco          ← Renamed from gemma_q4f16_1_devc.o
    │   ├── mlc-chat-config.json
    │   ├── ndarray-cache.json
    │   ├── tokenizer.model
    │   ├── tokenizer_config.json
    │   └── params_shard_*.bin         (multiple files)
    │
    └── AVA-GE3-4B16/                  ← Gemma 3 4B model (optional)
        ├── AVALibrary.ADco
        ├── AVA-GE3-4B16.ADco
        ├── mlc-chat-config.json
        ├── ndarray-cache.json
        ├── tokenizer.model
        ├── tokenizer_config.json
        └── params_shard_*.bin         (69 files)
```

**Important Notes:**
- The `llm/` subdirectory does NOT exist by default
- You must create it before pushing models
- Model folder names MUST match exactly (case-sensitive)
- Internal filenames can be original OR use AVA naming convention

---

## Available Models

### Model 1: AVA Core (Gemma 2 2B) - RECOMMENDED

**Brand Name:** AVA Core
**Technical ID:** AVA-GE2-2B16
**Base Model:** Gemma 2 2B (Google)
**Size:** ~1.2 GB
**Languages:** English
**Use Case:** Fast, compact, English-focused responses

**Source Files Location:**
```
~/Downloads/Coding/AVA-LLM/models/
└── AVA-GE2-2B16/                      ← Ready to push!
    ├── AVALibrary.ADco                (51MB - TVM library)
    └── AVA-GE2-2B16.ADco              (83MB - model device code)
```

**Contents:**
- Compiled device code with AVA naming convention
- NOTE: This contains compiled code only (~3.8MB total), weight files need to be added separately

### Model 2: AVA Nexus (Gemma 3 4B) - FLAGSHIP

**Brand Name:** AVA Nexus
**Technical ID:** AVA-GE3-4B16
**Base Model:** Gemma 3 4B (Google)
**Size:** ~4.1 GB
**Languages:** 140+ languages
**Use Case:** Multilingual flagship model

**Source Files Location:**
```
~/Downloads/Coding/AVA-LLM/models/
└── AVA-GE3-4B16/                      ← Complete model (4.1GB)
    ├── AVALibrary.ADco                (51MB - TVM library)
    ├── AVA-GE3-4B16.ADco             (163MB - model device code)
    ├── mlc-chat-config.json
    ├── ndarray-cache.json
    ├── tokenizer.model
    ├── tokenizer_config.json
    └── params_shard_*.bin             (69 files, ~3.8GB total)
```

**Contents:**
- Complete model with all weight files and compiled device code
- Ready to push directly to device
- Uses AVA naming convention throughout

**Note:** Compiled with TVM v0.22.0 Apache (new FFI API)

---

## Installation Methods

### Method 1: ADB Push (Recommended for Developers)

**Advantages:**
- Fast transfer
- Direct to app directory
- No file manager needed

**Steps:**

#### For Gemma 2 2B (AVA Core):

**Note:** Contains compiled code only (~3.8MB). Weight files need to be added separately.

```bash
# 1. Go to model directory
cd ~/Downloads/Coding/AVA-LLM/models/

# Files in AVA-GE2-2B16/:
# - AVALibrary.ADco (51MB)
# - AVA-GE2-2B16.ADco (83MB)

# 2. Connect device
adb devices

# 3. Create directory on device
adb shell mkdir -p /sdcard/ava-llm/models/

# 4. Push compiled code to device
adb push AVA-GE2-2B16 /sdcard/ava-llm/models/

# 5. Verify
adb shell ls -la /sdcard/ava-llm/models/AVA-GE2-2B16/

# NOTE: You'll also need to add model weights, tokenizer, and config files
# for a complete working model (see Gemma 3 for full model structure)
```

#### For Gemma 3 4B (AVA Nexus):

**Note:** Complete model with all weight files! Ready to push directly.

```bash
# 1. Go to model directory
cd ~/Downloads/Coding/AVA-LLM/models/

# 2. Verify complete model contents
ls -lh AVA-GE3-4B16/*.ADco
# Should see:
#  - AVALibrary.ADco (51MB)
#  - AVA-GE3-4B16.ADco (163MB)

# 3. Connect device and create directory
adb devices
adb shell mkdir -p /sdcard/ava-llm/models/

# 4. Push complete model to device (~4.1GB - this will take several minutes!)
adb push AVA-GE3-4B16 /sdcard/ava-llm/models/

# 5. Verify
adb shell ls -la /sdcard/ava-llm/models/AVA-GE3-4B16/
adb shell du -sh /sdcard/ava-llm/models/AVA-GE3-4B16/
# Expected: ~4.1G
```

---

### Method 2: Manual Transfer via File Manager

**Advantages:**
- No adb required
- Works without computer after initial download
- Can use cloud storage

**Steps:**

1. **Prepare Model on Computer:**
   - Extract and rename as shown in Method 1
   - Zip the entire model directory: `zip -r AVA-GE2-2B16.zip AVA-GE2-2B16/`

2. **Transfer to Device:**
   - Copy zip file to device via USB
   - Or upload to cloud (Google Drive, Dropbox) and download on device

3. **Extract on Device:**
   - Use file manager app (Files by Google, Solid Explorer)
   - Navigate to: `/sdcard/ava-llm/`
   - Create `models` folder if it doesn't exist
   - Extract zip into `models` folder

4. **Verify Structure:**
   ```
   /sdcard/ava-llm/models/AVA-GE2-2B16/
   ```

---

### Method 3: Direct Download (Future)

**Note:** Not yet implemented. Future versions will support:
- In-app model download
- Automatic extraction and setup
- Progress indicators
- Verification

---

## Model File Locations (Development)

### On Development Machine

#### Repository Location:
```
/Volumes/M-Drive/Coding/AVA/
├── apps/ava-standalone/src/main/
│   ├── assets/models/
│   │   └── AVA-384-Base-INT8.AON    ← Bundled NLU model only
│   └── jniLibs/arm64-v8a/
│       ├── libtvm_runtime.so         ← TVM runtime
│       └── libtvm4j.so               ← JNI bridge
```

**Note:** LLM models are NOT in the repository (too large for git).

#### External Model Storage:

**AVA Models (New Structure):**
```
~/Downloads/Coding/AVA-LLM/models/
├── AVA-GE2-2B16/                     ← Gemma 2 2B (compiled code only)
│   ├── AVALibrary.ADco (51MB)
│   └── AVA-GE2-2B16.ADco (83MB)
└── AVA-GE3-4B16/                     ← Gemma 3 4B (complete model)
    ├── AVALibrary.ADco (51MB)
    ├── AVA-GE3-4B16.ADco (163MB)
    ├── mlc-chat-config.json
    ├── tokenizer.model
    └── params_shard_*.bin (69 files)
```

**Legacy Locations (old structure - for reference):**
```
~/Downloads/Coding/MLC-LLM-Code/
├── binary-mlc-llm-libs-Android-09262024/   ← Other models
└── gemma-3-models/                         ← Original Gemma 3 source
```

---

## Verification

### On Device (via adb)

```bash
# Check if model directory exists
adb shell ls -la /sdcard/ava-llm/models/

# Check model contents
adb shell ls -la /sdcard/ava-llm/models/AVA-GE3-4B16/

# Check file sizes
adb shell du -sh /sdcard/ava-llm/models/AVA-GE3-4B16/

# Expected output: ~1.2G for Gemma 2 2B, ~4.1G for Gemma 3 4B
```

### Required Files Checklist

For each model directory, verify these files exist:

**Compiled Code (ONE of these naming schemes):**
- ✅ Original naming: `lib0.o` + `gemma_q4f16_1_devc.o` OR `gemma3_q4bf16_1_devc.o`
- ✅ AVA naming: `AVALibrary.ADco` + `AVA-GE*-*B16.ADco`

**Configuration:**
- ✅ `mlc-chat-config.json`
- ✅ `ndarray-cache.json`

**Tokenizer:**
- ✅ `tokenizer.model`
- ✅ `tokenizer_config.json`

**Model Weights:**
- ✅ `params_shard_0.bin`
- ✅ `params_shard_1.bin`
- ✅ ... (multiple shard files)

### In AVA App

1. Launch AVA AI
2. Go to Settings → Model Selection
3. Available models should appear:
   - AVA Core (if installed)
   - AVA Nexus (if installed)
4. Select a model
5. Wait for model to load (first load takes 10-20 seconds)
6. Try a test message

---

## Troubleshooting

### Model Not Appearing in App

**Problem:** Model doesn't show up in Settings → Model Selection

**Solutions:**
1. Verify directory name matches exactly:
   - ✅ `AVA-GE3-4B16` (correct)
   - ❌ `AVA-GE3-4b16` (wrong - case matters)
   - ❌ `ava-nexus` (wrong - brand name)
   - ❌ `gemma-3-4b-it` (wrong - old naming)

2. Check directory location:
   ```bash
   adb shell ls /sdcard/ava-llm/models/
   ```

3. Restart AVA app

### Model Fails to Load

**Problem:** Model appears but fails to load with error

**Check 1: Verify compiled files**
```bash
adb shell ls -lh /sdcard/ava-llm/models/AVA-GE3-4B16/*.ADco
```

Expected sizes:
- `AVALibrary.ADco`: ~51 MB
- `AVA-GE2-2B16.ADco`: ~83 MB (Gemma 2)
- `AVA-GE3-4B16.ADco`: ~163 MB (Gemma 3)

**Check 2: Verify mlc-chat-config.json**
```bash
adb shell cat /sdcard/ava-llm/models/AVA-GE3-4B16/mlc-chat-config.json
```

Look for:
- `"model_lib"` field (should reference the compiled code)
- `"model_type"` field (should be "gemma" or "gemma3")

**Check 3: Check app logs**
```bash
adb logcat -s AVA:* TVMRuntime:* ALCEngine:*
```

Common errors:
- `UnsatisfiedLinkError` → Wrong TVM version (need v0.22.0)
- `Model file not found` → File path issue
- `Failed to load model_lib` → Compiled code mismatch

### Insufficient Storage

**Problem:** Not enough space on device

**Solutions:**
1. Check available space:
   ```bash
   adb shell df -h /sdcard/
   ```

2. Required space:
   - Gemma 2 2B: 1.5 GB free (1.2 GB + buffer)
   - Gemma 3 4B: 3.0 GB free (2.3 GB + buffer)

3. Clear space or use smaller model

### Transfer Interrupted

**Problem:** `adb push` failed or was interrupted

**Solution:**
```bash
# Remove incomplete transfer
adb shell rm -rf /sdcard/ava-llm/models/AVA-GE3-4B16/

# Restart transfer
cd ~/Downloads/Coding/AVA-LLM/models/
adb push AVA-GE3-4B16 /sdcard/ava-llm/models/
```

### Permission Denied

**Problem:** Cannot access `/sdcard/ava-llm/`

**Solutions:**

**On Android 11+:**
1. The `/sdcard/` directory is accessible to all apps
2. If directory doesn't exist, create it: `adb shell mkdir -p /sdcard/ava-llm/models/`
3. This simplified path avoids scoped storage restrictions
4. AVA will check both `/sdcard/ava-llm/models/` and legacy path

---

## Model Comparison

| Model | Brand Name | Size | RAM | Languages | Speed | Quality |
|-------|-----------|------|-----|-----------|-------|---------|
| AVA-GE2-2B16 | AVA Core | 1.2GB | ~2GB | English | Fast | Good |
| AVA-GE3-4B16 | AVA Nexus | 2.3GB | ~3.2GB | 140+ | Medium | Excellent |
| AVA-LL2-7B16 | AVA Sage | 4.0GB | ~5GB | Multilingual | Slow | Excellent |

**Recommendations:**
- **Low-end devices** (4GB RAM): Use AVA Core (Gemma 2 2B)
- **Mid-range devices** (6GB RAM): Use AVA Nexus (Gemma 3 4B)
- **High-end devices** (8GB+ RAM): Use AVA Nexus or larger models

---

## Additional Resources

- [Chapter 42: LLM Model Setup](Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [Chapter 44: AVA Naming Convention v2](Developer-Manual-Chapter44-AVA-Naming-Convention.md)
- [Chapter 45: AVA LLM Naming Standard](Developer-Manual-Chapter45-AVA-LLM-Naming-Standard.md)
- [Spec 011: Automated Model Compilation Pipeline](../.ideacode/specs/011-automated-model-compilation-pipeline/spec.md)

---

## Quick Reference Card

### Essential Commands

```bash
# Connect device
adb devices

# Create directory
adb shell mkdir -p /sdcard/ava-llm/models/

# Push model
cd ~/Downloads/Coding/AVA-LLM/models/
adb push AVA-GE3-4B16 /sdcard/ava-llm/models/

# Verify
adb shell ls -la /sdcard/ava-llm/models/AVA-GE3-4B16/

# Check size
adb shell du -sh /sdcard/ava-llm/models/*

# Remove model
adb shell rm -rf /sdcard/ava-llm/models/AVA-GE3-4B16/
```

---

**Document Version:** 1.0
**Last Updated:** 2025-11-20
**Author:** AVA Development Team
