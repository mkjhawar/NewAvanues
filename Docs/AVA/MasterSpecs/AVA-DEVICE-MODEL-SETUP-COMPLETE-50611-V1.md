# Complete Device Model Setup Guide for AVA

**Purpose:** Step-by-step instructions to set up all AI models on your Android device
**For:** RAG (document search) + LLM (chat) functionality
**Total Size:** ~2.1 GB (RAG: 86 MB, LLM: ~2 GB)

---

## Quick Overview

AVA needs 2 types of models on your device:

1. **Embedding Model (ONNX)** - 86 MB - For RAG document search
2. **LLM Model (MLC-LLM)** - ~2 GB - For chat responses

Both models go into your device's external storage in the AVA app directory.

---

## Directory Structure

All models go in this base location on your device:

```
/sdcard/Android/data/com.augmentalis.ava/files/models/
```

Your final structure should look like:

```
/sdcard/Android/data/com.augmentalis.ava/files/models/
├── AVA-ONX-384-BASE.onnx              ← RAG embedding model (86 MB)
└── llm/
    └── AVA-GEM-2B-Q4.tar              ← LLM model (~2 GB)
```

---

## Part 1: RAG Embedding Model Setup (Required for Document Search)

### What You Need

**Model:** AVA-ONX-384-BASE (ONNX format)
**Size:** 86 MB
**Purpose:** Generates embeddings for document search (RAG functionality)
**Location:** `/sdcard/Android/data/com.augmentalis.ava/files/models/`

### Option A: Using ADB (Computer Required)

```bash
# Step 1: Download model on your computer
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx

# Step 2: Connect your Android device via USB
# Enable USB debugging: Settings → About Phone → tap Build Number 7 times → Developer Options → USB Debugging

# Step 3: Push model to device
adb push AVA-ONX-384-BASE.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# Step 4: Verify it's there
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
# Should show: AVA-ONX-384-BASE.onnx  86M
```

### Option B: Using Android File Manager (No Computer)

```
1. On your Android device, open Chrome browser

2. Download the model:
   https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/blob/main/onnx/model.onnx

   Click: "Download" button (top right)
   File will save to: Downloads/model.onnx

3. Open Files app (or any file manager)

4. Navigate to Downloads folder

5. Long-press "model.onnx" → Rename to:
   AVA-ONX-384-BASE.onnx

6. Move file to AVA models directory:
   - Navigate to: Internal Storage → Android → data → com.augmentalis.ava → files
   - Create "models" folder if it doesn't exist
   - Move "AVA-ONX-384-BASE.onnx" into "models" folder

7. Verify: File should be at:
   Internal Storage/Android/data/com.augmentalis.ava/files/models/AVA-ONX-384-BASE.onnx
```

### Option C: Using Termux (On-Device Terminal)

```bash
# Step 1: Install Termux from F-Droid
# https://f-droid.org/en/packages/com.termux/

# Step 2: Install curl
pkg install curl

# Step 3: Create models directory
mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/

# Step 4: Download model directly
cd /sdcard/Android/data/com.augmentalis.ava/files/models/
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-BASE.onnx

# Step 5: Verify
ls -lh
# Should show: AVA-ONX-384-BASE.onnx  86M
```

---

## Part 2: LLM Model Setup (Required for Chat)

### What You Need

**Model:** AVA-GEM-2B-Q4 (MLC-LLM compiled)
**Size:** ~2 GB (INT4 quantized)
**Purpose:** Generates conversational responses in RAG chat
**Location:** `/sdcard/Android/data/com.augmentalis.ava/files/models/llm/`

### Option A: Using ADB (Recommended)

```bash
# Step 1: Get the MLC-LLM compiled model and rename
# Navigate to your MLC-LLM directory
cd /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/

# Copy and rename to proprietary format
cp gemma-2b-it-q4f16_1-android.tar AVA-GEM-2B-Q4.tar

# Step 2: Connect your Android device via USB

# Step 3: Create llm directory on device
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Step 4: Push the renamed model tar file
adb push AVA-GEM-2B-Q4.tar /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Step 5: Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
# Should show: AVA-GEM-2B-Q4.tar  ~2GB
```

### Option B: Using Android File Manager

```
1. Copy and rename the MLC-LLM model:
   - On your computer, navigate to MLC-LLM directory
   - Copy: gemma-2b-it-q4f16_1-android.tar
   - Rename to: AVA-GEM-2B-Q4.tar
   - Connect device to computer via USB
   - Copy AVA-GEM-2B-Q4.tar to: Device/Downloads/

2. On Android device, open Files app

3. Navigate to Downloads folder

4. Long-press "AVA-GEM-2B-Q4.tar"
   Select: Move

5. Navigate to:
   Internal Storage → Android → data → com.augmentalis.ava → files → models

6. Create "llm" folder if it doesn't exist

7. Move file into "llm" folder

8. Final location should be:
   Internal Storage/Android/data/com.augmentalis.ava/files/models/llm/AVA-GEM-2B-Q4.tar
```

### Alternative LLM Models (If You Don't Have Gemma)

If you don't have the MLC-LLM compiled Gemma model, you can download alternatives:

#### TinyLlama (Smaller, Faster - 600 MB)

```bash
# Download (GGUF format)
curl -L https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  -o AVA-TNY-1B-Q4.gguf

# Push to device
adb push AVA-TNY-1B-Q4.gguf /sdcard/Android/data/com.augmentalis.ava/files/models/llm/
```

**Note:** However, AVA's RAGChatEngine is optimized for MLC-LLM compiled models. GGUF support may require additional configuration.

---

## Verification

### Check All Files Are In Place

```bash
# Use ADB from computer
adb shell ls -lhR /sdcard/Android/data/com.augmentalis.ava/files/models/

# Expected output:
# /sdcard/Android/data/com.augmentalis.ava/files/models/:
# total 86M
# -rw-rw---- 1 u0_a123 media_rw 86M Nov 06 14:00 AVA-ONX-384-BASE.onnx
# drwxrwx--- 2 u0_a123 media_rw 4.0K Nov 06 14:05 llm
#
# /sdcard/Android/data/com.augmentalis.ava/files/models/llm:
# total 2.0G
# -rw-rw---- 1 u0_a123 media_rw 2.0G Nov 06 14:05 AVA-GEM-2B-Q4.tar
```

### Or Using Device File Manager

```
Navigate to:
Internal Storage → Android → data → com.augmentalis.ava → files → models

Should see:
✓ AVA-ONX-384-BASE.onnx (86 MB)
✓ llm/ (folder)
  └── AVA-GEM-2B-Q4.tar (~2 GB)
```

---

## File Size Reference

Use this to verify your downloads are complete:

| File | Expected Size | Purpose |
|------|---------------|---------|
| AVA-ONX-384-BASE.onnx | 86 MB (90,400,000 bytes) | RAG embeddings |
| AVA-GEM-2B-Q4.tar | ~2 GB (varies) | LLM chat |
| AVA-TNY-1B-Q4.gguf | ~600 MB (if using alternative) | LLM chat (alternative) |

---

## Troubleshooting

### Issue 1: "Embedding model not found"

**Fix:**
```bash
# Check file exists
adb shell ls -l /sdcard/Android/data/com.augmentalis.ava/files/models/AVA-ONX-384-BASE.onnx

# Check filename is EXACT (case-sensitive)
# Must be: AVA-ONX-384-BASE.onnx
# NOT: ava-onx-384-base.onnx or AVA.ONX.BASE.onnx

# Fix permissions if needed
adb shell chmod 644 /sdcard/Android/data/com.augmentalis.ava/files/models/AVA-ONX-384-BASE.onnx
```

### Issue 2: "LLM model not found"

**Fix:**
```bash
# Check file exists
adb shell ls -l /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Verify it's in the llm/ subfolder
# NOT in models/ directly

# Check permissions
adb shell chmod 644 /sdcard/Android/data/com.augmentalis.ava/files/models/llm/*.tar
```

### Issue 3: Download corrupted (file won't load)

**Fix:**
```bash
# Check file size
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/AVA-ONX-384-BASE.onnx
# Should be ~86M, not 1K or 100K

# Check file type
adb shell file /sdcard/Android/data/com.augmentalis.ava/files/models/AVA-ONX-384-BASE.onnx
# Should say "data" or "ONNX model"
# NOT "HTML document" (means you downloaded an HTML error page)

# Re-download if corrupted
```

### Issue 4: No storage permission

**Fix:**
```bash
# Grant storage permissions
adb shell pm grant com.augmentalis.ava android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.augmentalis.ava android.permission.WRITE_EXTERNAL_STORAGE

# Or on device:
# Settings → Apps → AVA → Permissions → Storage → Allow
```

### Issue 5: Out of storage space

**Check available space:**
```bash
adb shell df -h /sdcard
```

**Free up space:**
- Need at least 2.5 GB free (2.1 GB models + 400 MB overhead)
- Delete old downloads
- Clear other apps' cache
- Consider using smaller model (TinyLlama 600MB instead of Gemma 2GB)

---

## Testing After Setup

### Test RAG (Embedding Model)

1. Open AVA app
2. Navigate to: Document Management
3. Add a test document (PDF, DOCX, or HTML)
4. Wait for processing to complete
5. Try searching: "test query"
6. If you see search results → RAG is working ✅

### Test LLM (Chat Model)

1. Open AVA app
2. Navigate to: RAG Chat
3. Add some documents first (if you haven't)
4. Ask a question: "What is in the documents?"
5. If you see streaming response → LLM is working ✅

---

## Quick Command Summary

**Complete setup in 4 commands (via ADB):**

```bash
# 1. Download embedding model
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx -o AVA-ONX-384-BASE.onnx

# 2. Push embedding model
adb push AVA-ONX-384-BASE.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# 3. Create LLM directory
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# 4. Rename and push LLM model (adjust path to your local file)
cp /Users/manoj_mbpm14/Downloads/Coding/MLC-LLM-Code/binary-mlc-llm-libs-Android-09262024/gemma-2b-it/gemma-2b-it-q4f16_1-android.tar AVA-GEM-2B-Q4.tar
adb push AVA-GEM-2B-Q4.tar /sdcard/Android/data/com.augmentalis.ava/files/models/llm/

# Done! Restart AVA app
```

---

## Device Requirements

**Minimum:**
- Android 8.0 (API 26) or higher
- 4 GB RAM (for Gemma-2b)
- 3 GB free storage

**Recommended:**
- Android 10 or higher
- 6 GB RAM
- 4 GB free storage

**For best performance:**
- Android 12 or higher
- 8 GB RAM
- SSD/fast storage
- GPU/NPU support

---

## Where To Get Help

**Documentation:**
- MODEL-SETUP.md - Embedding model details
- LLM-SETUP.md - LLM model details
- RAG-Chat-Integration-Guide.md - Complete integration guide

**Support:**
- GitHub Issues: https://github.com/augmentalis/ava/issues
- Email: support@augmentalis.com

**Model Sources:**
- HuggingFace: https://huggingface.co/sentence-transformers
- MLC-LLM: https://github.com/mlc-ai/mlc-llm

---

## Summary Checklist

- [ ] Downloaded and renamed embedding model to AVA-ONX-384-BASE.onnx (86 MB)
- [ ] Placed it in: `/sdcard/Android/data/com.augmentalis.ava/files/models/`
- [ ] Renamed LLM model to AVA-GEM-2B-Q4.tar (~2 GB)
- [ ] Created llm folder: `/sdcard/Android/data/com.augmentalis.ava/files/models/llm/`
- [ ] Placed LLM model in llm folder
- [ ] Verified file sizes match expected values
- [ ] Granted storage permissions to AVA
- [ ] Restarted AVA app
- [ ] Tested RAG search with a document
- [ ] Tested LLM chat responses

**If all checked ✅ → You're ready to use AVA's RAG+LLM features!**

---

**Last Updated:** 2025-11-06
**Version:** 1.0
**Status:** Production Ready
