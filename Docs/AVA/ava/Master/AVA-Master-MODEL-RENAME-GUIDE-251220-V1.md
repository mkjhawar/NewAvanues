# AVA Model Renaming Guide

**Purpose:** Quick guide to rename models to AVA proprietary filenames
**Target:** Users setting up models for the first time
**Time:** < 5 minutes

---

## Why Rename?

AVA uses proprietary filenames for AI models:
- ✅ Obscures model origins
- ✅ Simpler filenames (no version numbers)
- ✅ Consistent naming across platforms
- ✅ Professional branding

---

## Quick Rename Commands

### Option 1: Download & Rename in One Step

**Embedding Model (86 MB):**
```bash
# Download and save as AVA-ONX-384-BASE.onnx
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx

# Push to device
adb push AVA-ONX-384-BASE.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
```

**LLM Model (~2 GB) - From Your Local MLC-LLM:**
```bash
# Navigate to your MLC-LLM downloads
cd /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/

# Copy and rename
cp gemma-2b-it-q4f16_1-android.tar AVA-GEM-2B-Q4.tar

# Push to device
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
adb push AVA-GEM-2B-Q4.tar /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
```

### Option 2: Rename Existing Files

**If you already downloaded models with original names:**

```bash
# Rename embedding model
mv all-MiniLM-L6-v2.onnx AVA-ONX-384-BASE.onnx
mv model.onnx AVA-ONX-384-BASE.onnx  # if named just "model.onnx"

# Rename LLM model
mv gemma-2b-it-q4f16_1-android.tar AVA-GEM-2B-Q4.tar

# Push to device
adb push AVA-ONX-384-BASE.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
adb push AVA-GEM-2B-Q4.tar /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
```

---

## Complete Setup Script

**Copy-paste this entire block:**

```bash
#!/bin/bash
# AVA Model Setup Script with Proprietary Naming

echo "🚀 AVA Model Setup - Proprietary Naming"
echo "========================================"

# Step 1: Download embedding model
echo ""
echo "📥 Step 1: Downloading embedding model (86 MB)..."
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx

if [ -f "AVA-ONX-384-BASE.onnx" ]; then
    echo "✅ Embedding model downloaded: AVA-ONX-384-BASE.onnx ($(ls -lh AVA-ONX-384-BASE.onnx | awk '{print $5}'))"
else
    echo "❌ Failed to download embedding model"
    exit 1
fi

# Step 2: Copy and rename LLM model from local MLC-LLM
echo ""
echo "📦 Step 2: Copying LLM model from local MLC-LLM..."
MLC_PATH="/Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/gemma-2b-it-q4f16_1-android.tar"

if [ -f "$MLC_PATH" ]; then
    cp "$MLC_PATH" AVA-GEM-2B-Q4.tar
    echo "✅ LLM model copied: AVA-GEM-2B-Q4.tar ($(ls -lh AVA-GEM-2B-Q4.tar | awk '{print $5}'))"
else
    echo "⚠️  MLC-LLM model not found at: $MLC_PATH"
    echo "   Please adjust the path or download manually"
fi

# Step 3: Push to device
echo ""
echo "📱 Step 3: Pushing models to Android device..."

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device detected"
    echo "   Please connect device via USB and enable USB debugging"
    exit 1
fi

# Push embedding model
echo "   Pushing AVA-ONX-384-BASE.onnx..."
adb push AVA-ONX-384-BASE.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# Create LLM directory and push
echo "   Creating llm directory..."
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

if [ -f "AVA-GEM-2B-Q4.tar" ]; then
    echo "   Pushing AVA-GEM-2B-Q4.tar..."
    adb push AVA-GEM-2B-Q4.tar /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
fi

# Step 4: Verify
echo ""
echo "✅ Step 4: Verification"
echo "========================================"
echo ""
echo "Models on device:"
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
echo ""
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

echo ""
echo "🎉 Setup Complete!"
echo ""
echo "Expected files:"
echo "  ✓ AVA-ONX-384-BASE.onnx  (86M)"
echo "  ✓ llm/AVA-GEM-2B-Q4.tar  (~2.0G or 311K)"
echo ""
echo "Next: Launch AVA app and test RAG + Chat features"
```

**Save as:** `setup-ava-models.sh`

**Run:**
```bash
chmod +x setup-ava-models.sh
./setup-ava-models.sh
```

---

## Manual Verification

**Check files are correctly named:**

```bash
# On your computer (before pushing)
ls -lh AVA-*

# Expected output:
# AVA-GEM-2B-Q4.tar        (varies)
# AVA-ONX-384-BASE.onnx    (86M)

# On device (after pushing)
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/

# Expected output:
# AVA-ONX-384-BASE.onnx  86M

adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Expected output:
# AVA-GEM-2B-Q4.tar  311K (or ~2.0G if full model)
```

---

## Filename Reference

| AVA Filename | Original Filename | Size | Purpose |
|--------------|-------------------|------|---------|
| AVA-ONX-384-BASE.onnx | all-MiniLM-L6-v2/model.onnx | 86 MB | RAG embeddings |
| AVA-GEM-2B-Q4.tar | gemma-2b-it-q4f16_1-android.tar | ~2 GB | LLM chat |

**More mappings:** See `docs/AVA-MODEL-NAMING-REGISTRY.md`

---

## Troubleshooting

### File too small (KB instead of MB/GB)

**Problem:** Downloaded HTML error page instead of model

**Solution:**
```bash
# Check file type
file AVA-ONX-384-BASE.onnx

# Should say: "data" or "ONNX model"
# NOT: "HTML document"

# If HTML, re-download with correct URL
```

### Permission denied when pushing to device

**Solution:**
```bash
# Grant storage permissions
adb shell pm grant com.augmentalis.ava android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.augmentalis.ava android.permission.WRITE_EXTERNAL_STORAGE
```

### Model not found after setup

**Check:**
1. Filename is EXACTLY correct (case-sensitive)
2. File is in correct directory
3. App has storage permissions
4. Restart AVA app

---

## Alternative Models (Optional)

**Fast/Smaller:**
```bash
# Embedding: 61 MB (faster)
curl -L https://huggingface.co/sentence-transformers/paraphrase-MiniLM-L3-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-FAST.onnx

# LLM: 600 MB (TinyLlama, faster)
curl -L https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  -o AVA-TNY-1B-Q4.gguf
```

**High-Quality:**
```bash
# Embedding: 420 MB (more accurate)
curl -L https://huggingface.co/sentence-transformers/all-mpnet-base-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-768-QUAL.onnx
```

---

## Cleanup

**Remove original named files (optional):**

```bash
# On computer
rm model.onnx all-MiniLM-L6-v2.onnx gemma-2b-it-q4f16_1-android.tar

# On device (if you pushed originals first - keep for backward compatibility if needed)
adb shell rm /sdcard/Android/data/com.augmentalis.ava/files/models/all-MiniLM-L6-v2.onnx
adb shell rm /sdcard/Android/data/com.augmentalis.ava/files/models/llm/gemma-2b-it-q4f16_1-android.tar
```

---

## Summary Checklist

- [ ] Downloaded embedding model
- [ ] Renamed to `AVA-ONX-384-BASE.onnx`
- [ ] Copied LLM model from MLC-LLM folder
- [ ] Renamed to `AVA-GEM-2B-Q4.tar`
- [ ] Pushed embedding model to `/sdcard/Android/data/com.augmentalis.ava/files/models/`
- [ ] Pushed LLM model to `/sdcard/Android/data/com.augmentalis.ava/files/models/llm/`
- [ ] Verified files on device with correct sizes
- [ ] Launched AVA app
- [ ] Tested RAG document search
- [ ] Tested LLM chat responses

**All checked? You're ready to go! 🚀**

---

**See Also:**
- AVA-MODEL-NAMING-REGISTRY.md - Complete model mapping
- DEVICE-MODEL-SETUP-COMPLETE.md - Detailed setup guide
- MODEL-SETUP.md - Public documentation (generic names)

---

**Last Updated:** 2025-11-06
**Version:** 1.0
