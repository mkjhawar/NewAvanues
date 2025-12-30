# AVA LLM Model Setup Guide

**Purpose:** External LLM model management for AVA
**Models:** ONNX/GGUF LLM Models (TinyLlama, Phi-3, Mistral, etc.)
**Why External:** Keeps APK size small (~30MB vs 600MB-8GB+)

---

## Overview

AVA can use local Large Language Models (LLMs) for on-device natural language understanding and chat. Instead of bundling these massive models in the APK, users place models in a specific folder on their device.

### Benefits

✅ **Small APK Size** - App stays ~30MB instead of 600MB-8GB
✅ **User Choice** - Download only the LLM models you need
✅ **Easy Updates** - Replace model files without app update
✅ **Multi-Source** - Download from HuggingFace, our servers, or other sources
✅ **Privacy** - LLMs stay on device, no cloud dependency
✅ **Flexibility** - Switch between different model sizes based on device capability

---

## Quick Start

### 1. Locate AVA LLM Models Folder

AVA looks for LLM models in this device location:

```
/data/data/com.augmentalis.ava/files/models/
```

**Structure:**
```
/data/data/com.augmentalis.ava/files/models/
├── AVA-GEM-2B-Q4/
│   └── AVA-GEM-2B-Q4.tar
├── AVA-QWN-1B-Q4/
│   └── AVA-QWN-1B-Q4.tar
└── AVA-MST-7B-Q4/
    └── AVA-MST-7B-Q4.tar
```

### 2. Choose and Download LLM Model

**AVA uses proprietary naming for security. See Developer Manual Chapter 38 for details.**

**For most devices (Recommended):**
- `AVA-GEM-2B-Q4` (~1.2GB, 2B params) - Best balance of quality and speed, optimized for mobile
- Languages: English, Spanish, French, German, Italian, Portuguese

**For multilingual support:**
- `AVA-QWN-1B-Q4` (~1.0GB, 1.5B params) - All 52+ languages, efficient, excellent for Asian languages

**For high-end devices:**
- `AVA-MST-7B-Q4` (~4.5GB, 7B params) - Highest quality responses
- Best reasoning and European language support

### 3. Automatic Download or Local Copy

**Option A: In-App Download (Recommended):**
- AVA automatically downloads models from HuggingFace when needed
- Shows progress and manages storage

**Option B: Local Copy (For Testing/Development):**
- For developers: Place MLC-LLM binary files in local folder
- AVA automatically copies to device storage (faster, no download)
- See `ModelSelector.kt` for localSourcePath configuration

**Note:** Models are stored with AVA naming convention for security (obscures model origins).

---

## Model Download Sources

### TinyLlama (Entry-Level)

**Model:** TinyLlama-1.1B-Chat-v1.0
**Size:** ~600MB (Q4 quantized)
**Parameters:** 1.1B
**RAM Required:** 2GB minimum
**Speed:** Very Fast ⚡⚡⚡
**Quality:** Basic ⭐⭐⭐

**Best For:** Low-end devices, quick responses, basic chat

**Download:**
```bash
# Option 1: Direct download (GGUF format)
curl -L https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  -o tinyllama-1.1b-chat-q4.gguf

# Option 2: Browser
https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/blob/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

---

### AVA-GEM-2B-Q4 (Recommended)

**Display Name:** Gemma 2B Instruct
**Model ID:** AVA-GEM-2B-Q4
**Size:** ~1.2GB (Q4F16 quantized)
**Parameters:** 2B
**RAM Required:** 3GB minimum
**Speed:** Fast ⚡⚡
**Quality:** Good ⭐⭐⭐⭐
**Languages:** English, Spanish, French, German, Italian, Portuguese

**Best For:** Most Android devices, optimized for mobile, balanced performance

**Download:**
- **In-App:** AVA downloads automatically from HuggingFace
- **Developer:** Configure localSourcePath in ModelSelector.kt for testing
  ```kotlin
  localSourcePath = "/path/to/gemma-2b-it-q4f16_1-android.tar"
  ```
- **Manual:** Download from mlc-ai/gemma-2b-it-q4f16_1-MLC repository

**Note:** Files are saved as `AVA-GEM-2B-Q4.tar` on device (AVA naming convention)

---

### AVA-MST-7B-Q4 (High-End)

**Display Name:** Mistral 7B Instruct
**Model ID:** AVA-MST-7B-Q4
**Size:** ~4.5GB (Q4 quantized)
**Parameters:** 7B
**RAM Required:** 8GB minimum
**Speed:** Moderate ⚡
**Quality:** Excellent ⭐⭐⭐⭐⭐
**Languages:** English, Spanish, French, German, Italian

**Best For:** High-end devices, complex tasks, highest quality reasoning

**Download:**
- **In-App:** AVA downloads automatically from HuggingFace
- **Developer:** Configure localSourcePath in ModelSelector.kt
  ```kotlin
  localSourcePath = "/path/to/mistral-7b-instruct-v0.2-q4f16_1-android.tar"
  ```

**Note:** Files are saved as `AVA-MST-7B-Q4.tar` on device (AVA naming convention)

---

## Setup Instructions by Platform

### Android (Standard)

**Via Computer (ADB - For Development):**

```bash
# 1. Get model from MLC-LLM directory (if testing locally)
cd /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/

# 2. Connect phone via USB

# 3. Push to device (using AVA naming)
adb push gemma-2b-it-q4f16_1-android.tar /data/data/com.augmentalis.ava/files/models/AVA-GEM-2B-Q4/AVA-GEM-2B-Q4.tar

# 4. Verify
adb shell ls -lh /data/data/com.augmentalis.ava/files/models/AVA-GEM-2B-Q4/
```

**Note:** For production use, let AVA download automatically (no manual file management needed).

**Via Android File Manager:**

1. Download model to your Downloads folder (using browser)
2. Open Files app
3. Navigate to: `Internal Storage → Android → data → com.augmentalis.ava → files → models`
4. Create `llm` folder if it doesn't exist
5. Move downloaded file into `llm` folder
6. Restart AVA

**Via Termux (On-Device):**

```bash
# Install termux from F-Droid
pkg install curl tar

# Create llm models directory
mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Copy from shared storage if available
# Or download via MLC-LLM repository
```

### AOSP / LineageOS / Custom ROMs

Same as Android standard. No Google Play Services required.

### Via AVA's In-App Downloader (Planned)

**Future Feature:**

```
AVA Settings → LLM Models → Download Models
- [Select Model] Gemma-2b-it
- [Download] → Shows progress (~2GB download)
- [Verify] → Checks integrity
- [Set as Default] → Use for chat/NLU
```

---

## Model Verification

AVA automatically verifies LLM models on startup:

**Checks:**
1. File exists in llm models folder
2. File size matches expected size
3. File format is valid (GGUF/ONNX)
4. (Future) SHA256 checksum validation

**View Status:**

In AVA app:
```
Settings → About → LLM Model Status
```

Shows:
- ✅ LLM Model: Gemma-2b-it (~2GB)
- ⚙️ Inference Engine: MLC-LLM
- 📊 RAM Usage: 2.5GB / 8GB
- 📁 Models folder: /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

---

## Troubleshooting

### Model Not Detected

**Symptoms:** AVA says "LLM model not found"

**Solutions:**

1. **Check file location:**
   ```bash
   adb shell ls -la /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
   ```
   Should show: `phi-3-mini-4k-instruct-q4.gguf` or similar

2. **Check file size:**
   - TinyLlama Q4: ~600MB
   - Phi-3-mini Q4: ~2GB
   - Mistral-7B Q4: ~4GB

3. **Check permissions:**
   ```bash
   adb shell chmod 644 /sdcard/Android/data/com.augmentalis.ava/files/models/llm/*.gguf
   ```

4. **Check filename:**
   Must end with `.gguf` or `.onnx` (case-sensitive)

5. **Restart AVA**

### Download Failed / Corrupted

**Symptoms:** File downloaded but AVA can't load it

**Solution:**
```bash
# Re-download with verification
curl -L https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf \
  -o phi-3-mini-4k-instruct-q4.gguf

# Check file size
ls -lh phi-3-mini-4k-instruct-q4.gguf
# Should be ~2GB for Phi-3

# Check it's actually GGUF (should show binary data)
file phi-3-mini-4k-instruct-q4.gguf
# Should say: "GGUF model" or "data"

# NOT: "HTML document" (means download URL was wrong)
```

### Storage Space Issues

**LLM models:** 600MB - 8GB each

**Check available space:**
```bash
adb shell df -h /sdcard
```

**Free up space:**
- Delete unused LLM models from llm folder
- Use smaller model (TinyLlama instead of Mistral-7B)
- Clear app cache (not models folder)
- Move media files to SD card

### Out of Memory Errors

**Symptoms:** App crashes when loading LLM

**Solutions:**

1. **Use smaller model:**
   - Try TinyLlama (600MB) instead of Mistral (4GB)
   - Use Q4 quantization instead of Q5/Q6

2. **Close other apps:**
   Free up RAM before launching AVA

3. **Check device RAM:**
   ```bash
   adb shell cat /proc/meminfo | grep MemTotal
   ```
   - TinyLlama needs 2GB minimum
   - Phi-3 needs 4GB minimum
   - Mistral needs 8GB minimum

### Permission Denied

**Symptoms:** Can't access llm models folder

**Solution:**

1. Grant storage permission to AVA (should happen on first run)
2. Manually grant: `Settings → Apps → AVA → Permissions → Storage`
3. Use ADB to set permissions:
   ```bash
   adb shell pm grant com.augmentalis.ava android.permission.READ_EXTERNAL_STORAGE
   adb shell pm grant com.augmentalis.ava android.permission.WRITE_EXTERNAL_STORAGE
   ```

---

## Advanced: Custom Model Server (AOSP)

For AOSP/custom ROM users who want to download from a custom server:

**1. Set up server:** Host LLM models on your own server

**2. Configure AVA:**
```kotlin
// In your AVA config (future feature)
val llmDownloadManager = AndroidLLMDownloadManager(
    context = context,
    customServerUrl = "https://your-server.com/ava-llm-models"
)
```

**3. Server structure:**
```
https://your-server.com/ava-llm-models/
├── tinyllama-1.1b-chat-q4.gguf
├── phi-3-mini-4k-instruct-q4.gguf
├── mistral-7b-instruct-q4.gguf
└── checksums.txt
```

---

## Future Enhancements

### Planned Features

✨ **In-App LLM Manager**
- Browse available LLM models
- Download with progress bar
- Verify integrity (SHA256)
- Delete unused models
- Switch active model
- Test model performance

✨ **Google Play Dynamic Delivery**
- LLM models as dynamic features (Google Play only)
- Optional download on first use
- Managed by Google Play

✨ **Model Updates**
- Notify when new model versions available
- One-tap update
- Changelog and performance comparisons

✨ **Model Presets**
- "Fast" (TinyLlama 1.1B)
- "Balanced" (Phi-3-mini 3.8B) - Default
- "Quality" (Mistral-7B)
- "Custom" (User-provided models)

✨ **Performance Optimization**
- GPU acceleration (OpenCL/Vulkan)
- Multi-threading
- Model caching
- Quantization options (Q4, Q5, Q6, Q8)

---

## Model Information

### LLM Models Comparison

| Model ID | Display Name | Size | Params | RAM | Speed | Quality | Languages | Use Case |
|----------|--------------|------|--------|-----|-------|---------|-----------|----------|
| AVA-QWN-1B-Q4 | Qwen 2.5 1.5B | 1.0GB | 1.5B | 2GB | ⚡⚡⚡ | ⭐⭐⭐⭐ | All 52+ | Multilingual, efficient |
| AVA-GEM-2B-Q4 | Gemma 2B | 1.2GB | 2B | 3GB | ⚡⚡ | ⭐⭐⭐⭐ | European | Most devices, default |
| AVA-LLM-3B-Q4 | Llama 3.2 3B | 1.9GB | 3B | 4GB | ⚡⚡ | ⭐⭐⭐⭐ | European + Russian | Better reasoning |
| AVA-PHI-3B-Q4 | Phi 3.5 Mini | 2.4GB | 3.8B | 5GB | ⚡ | ⭐⭐⭐⭐⭐ | English only | English specialist |
| AVA-MST-7B-Q4 | Mistral 7B | 4.5GB | 7B | 8GB | ⚡ | ⭐⭐⭐⭐⭐ | European | Highest quality |

**Recommendation:** Start with `AVA-GEM-2B-Q4` (default, MLC-LLM optimized)

**Auto-Selection:** AVA automatically selects best model based on detected language

### Quantization Formats

| Format | Size | Quality | Speed | Notes |
|--------|------|---------|-------|-------|
| Q4_K_M | Smallest | Good | Fastest | Recommended (default) |
| Q5_K_M | Medium | Better | Medium | Higher quality |
| Q6_K | Larger | Best | Slower | Near-original quality |
| Q8_0 | Largest | Original | Slowest | Same as FP16 |

**Recommendation:** Use Q4_K_M for best balance

---

## Platform-Specific Notes

### Android
- Requires minSdk 26 (Android 8.0)
- LLM runs using MLC-LLM inference engine
- GPU acceleration supported via Vulkan/OpenCL

### AOSP/LineageOS
- Full support without Google Play Services
- Download from custom server or HuggingFace
- Same performance as stock Android

### Google Play
- Future: Dynamic Feature Modules for models
- Optional automatic downloads
- Managed updates

---

## Support

**Issues?**
- Check troubleshooting section above
- Visit: https://github.com/augmentalis/ava/issues
- Contact: support@augmentalis.com

**Model Questions?**
- HuggingFace docs: https://huggingface.co/docs
- AVA docs: https://docs.augmentalis.com/llm-models
- llama.cpp: https://github.com/ggerganov/llama.cpp

---

## License

**Models:** Various licenses (check HuggingFace model cards)
- TinyLlama: Apache 2.0
- Phi-3: MIT
- Mistral: Apache 2.0

**AVA App:** © Augmentalis Inc, Intelligent Devices LLC

---

**Last Updated:** 2025-11-17
**Version:** 2.0

**Changes in v2.0:**
- Updated to AVA model naming convention (AVA-GEM-2B-Q4, etc.)
- Added 5 MLC-LLM models with language-aware auto-selection
- Documented local storage support for testing
- Updated file paths to app-specific storage
- See Developer Manual Chapter 38 for complete details
