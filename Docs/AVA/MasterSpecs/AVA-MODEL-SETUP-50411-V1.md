# AVA Model Setup Guide

**Purpose:** External model management for AVA
**Models:** ONNX Embeddings, LLM Models
**Why External:** Keeps APK size small (~30MB vs ~500MB+)

---

## Overview

AVA uses large AI models for embeddings and language processing. Instead of bundling these in the APK, users place models in a specific folder on their device.

### Benefits

✅ **Small APK Size** - App is ~30MB instead of 500MB+
✅ **User Choice** - Download only the models you need
✅ **Easy Updates** - Replace model files without app update
✅ **Multi-Source** - Download from HuggingFace, our servers, or other sources
✅ **Privacy** - Models stay on device, no cloud dependency

---

## Quick Start

### 1. Locate AVA Models Folder

AVA looks for models in this device location:

```
/sdcard/Android/data/com.augmentalis.ava/files/models/
```

**Or via file manager:**
```
Internal Storage → Android → data → com.augmentalis.ava → files → models
```

### 2. Download Required Models

**Minimum (for basic RAG functionality):**
- `all-MiniLM-L6-v2.onnx` (86 MB) - Embedding model

**Recommended:**
- `all-MiniLM-L6-v2.onnx` (86 MB) - Embedding model
- Your chosen LLM model (varies by size)

### 3. Place Models in Folder

Copy downloaded `.onnx` files to the models folder:

```
/sdcard/Android/data/com.augmentalis.ava/files/models/
├── .nomedia (auto-created, hides folder from media scanners)
├── all-MiniLM-L6-v2.onnx
└── your-llm-model.onnx
```

**Note:** The folder is automatically hidden from media scanners via a `.nomedia` file.

---

## Model Download Sources

### Embedding Models (ONNX)

#### all-MiniLM-L6-v2 (Recommended for RAG)

**Size:** 86 MB
**Dimensions:** 384
**Speed:** Fast

**Download:**
```bash
# Option 1: Direct download
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx

# Option 2: Browser
https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/blob/main/onnx/model.onnx
```

**After download:**
1. Rename to `all-MiniLM-L6-v2.onnx` (if needed)
2. Copy to AVA models folder
3. Restart AVA

#### paraphrase-MiniLM-L3-v2 (Smaller, Faster)

**Size:** 61 MB
**Dimensions:** 384
**Speed:** Very Fast

**Download:**
```bash
curl -L https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx \
  -o paraphrase-MiniLM-L3-v2.onnx
```

#### all-mpnet-base-v2 (Higher Quality)

**Size:** 420 MB
**Dimensions:** 768
**Speed:** Slower, more accurate

**Download:**
```bash
curl -L https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx \
  -o all-mpnet-base-v2.onnx
```

---

### LLM Models (For Chat/NLU)

**Note:** LLM setup will be documented separately. Common options:

- **TinyLlama** (1.1B params, ~600MB) - Fast, basic
- **Phi-3-mini** (3.8B params, ~2GB) - Balanced
- **Mistral-7B** (7B params, ~4GB) - High quality

**Future:** AVA will include LLM management UI

---

## Setup Instructions by Platform

### Android (Standard)

**Via Computer (ADB):**

```bash
# 1. Download model on your computer
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx

# 2. Connect phone via USB

# 3. Push to device
adb push all-MiniLM-L6-v2.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# 4. Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
```

**Via Android File Manager:**

1. Download model to your Downloads folder (using browser)
2. Open Files app
3. Navigate to: `Internal Storage → Android → data → com.augmentalis.ava → files`
4. Create `models` folder if it doesn't exist
5. Move downloaded file into `models` folder
6. Restart AVA

**Via Termux (On-Device):**

```bash
# Install termux from F-Droid
pkg install curl

# Create models directory
mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/

# Download model
cd /sdcard/Android/data/com.augmentalis.ava/files/models/
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx
```

### AOSP / LineageOS / Custom ROMs

Same as Android standard. No Google Play Services required.

### Via AVA's In-App Downloader (Planned)

**Future Feature:**

```
AVA Settings → Models → Download Models
- [Select Model] all-MiniLM-L6-v2
- [Download] → Shows progress
- [Verify] → Checks integrity
```

---

## Model Verification

AVA automatically verifies models on startup:

**Checks:**
1. File exists in models folder
2. File size matches expected size
3. File can be loaded by ONNX Runtime
4. (Future) SHA256 checksum validation

**View Status:**

In AVA app:
```
Settings → About → Model Status
```

Shows:
- ✅ Embedding Model: all-MiniLM-L6-v2 (86 MB)
- ❌ LLM Model: Not found
- 📁 Models folder: /sdcard/Android/data/com.augmentalis.ava/files/models/

---

## Troubleshooting

### Model Not Detected

**Symptoms:** AVA says "Embedding model not found"

**Solutions:**

1. **Check file location:**
   ```bash
   adb shell ls -la /sdcard/Android/data/com.augmentalis.ava/files/models/
   ```
   Should show: `all-MiniLM-L6-v2.onnx`

2. **Check file size:**
   Should be approximately 86 MB (90,400,000 bytes)

3. **Check permissions:**
   ```bash
   adb shell chmod 644 /sdcard/Android/data/com.augmentalis.ava/files/models/*.onnx
   ```

4. **Check filename:**
   Must be exactly: `all-MiniLM-L6-v2.onnx` (case-sensitive)

5. **Restart AVA**

### Download Failed / Corrupted

**Symptoms:** File downloaded but AVA can't load it

**Solution:**
```bash
# Re-download with checksum verification
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o all-MiniLM-L6-v2.onnx

# Check file size
ls -lh all-MiniLM-L6-v2.onnx
# Should be ~86M

# Check it's actually ONNX (should show binary data, not HTML)
file all-MiniLM-L6-v2.onnx
# Should say: "ONNX model" or "data"

# NOT: "HTML document" (means download URL was wrong)
```

### Storage Space Issues

**Embedding models:** 60-420 MB each
**LLM models:** 600MB - 8GB each

**Check available space:**
```bash
adb shell df -h /sdcard
```

**Free up space:**
- Delete unused models from models folder
- Clear app cache (not models folder)
- Use smaller model (paraphrase-MiniLM-L3-v2 instead of all-mpnet-base-v2)

### Permission Denied

**Symptoms:** Can't access models folder

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

**1. Set up server:** Host models on your own server

**2. Configure AVA:**
```kotlin
// In your AVA config (future feature)
val downloadManager = AndroidModelDownloadManager(
    context = context,
    customServerUrl = "https://your-server.com/ava-models"
)
```

**3. Server structure:**
```
https://your-server.com/ava-models/
├── all-MiniLM-L6-v2.onnx
├── paraphrase-MiniLM-L3-v2.onnx
└── checksums.txt
```

---

## Future Enhancements

### Planned Features

✨ **In-App Model Manager**
- Browse available models
- Download with progress bar
- Verify integrity (SHA256)
- Delete unused models
- Switch models

✨ **Google Play Dynamic Delivery**
- Models as dynamic features (Google Play only)
- Auto-download on first use
- Managed by Google Play

✨ **Model Updates**
- Notify when new model versions available
- One-tap update
- Changelog

✨ **Model Presets**
- "Balanced" (all-MiniLM-L6-v2)
- "Fast" (paraphrase-MiniLM-L3-v2)
- "Quality" (all-mpnet-base-v2)
- "Offline" (TinyLlama + MiniLM)

---

## Model Information

### Embedding Models Comparison

| Model | Size | Dims | Speed | Quality | Use Case |
|-------|------|------|-------|---------|----------|
| paraphrase-MiniLM-L3-v2 | 61MB | 384 | ⚡⚡⚡ | ⭐⭐⭐ | Fast search |
| all-MiniLM-L6-v2 | 86MB | 384 | ⚡⚡ | ⭐⭐⭐⭐ | Balanced (default) |
| all-mpnet-base-v2 | 420MB | 768 | ⚡ | ⭐⭐⭐⭐⭐ | High accuracy |

**Recommendation:** Start with `all-MiniLM-L6-v2` (default)

### LLM Models (Coming Soon)

| Model | Size | Params | RAM | Speed | Use Case |
|-------|------|--------|-----|-------|----------|
| TinyLlama | 600MB | 1.1B | 2GB | ⚡⚡⚡ | Basic chat |
| Phi-3-mini | 2GB | 3.8B | 4GB | ⚡⚡ | Balanced |
| Mistral-7B | 4GB | 7B | 8GB | ⚡ | High quality |

---

## Support

**Issues?**
- Check troubleshooting section above
- Visit: https://github.com/augmentalis/ava/issues
- Contact: support@augmentalis.com

**Model Questions?**
- HuggingFace docs: https://huggingface.co/sentence-transformers
- AVA docs: https://docs.augmentalis.com/models

---

## License

**Models:** Apache 2.0 (HuggingFace)
**AVA App:** © Augmentalis Inc, Intelligent Devices LLC

---

**Last Updated:** 2025-11-04
**Version:** 1.0
