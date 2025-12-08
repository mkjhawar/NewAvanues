# AVA AI Models - Quick Start Installation Guide

**Last Updated:** 2025-11-20
**Version:** 1.0
**For:** AVA AI App Users

---

## 🚀 Quick Start (3 Steps)

### Prerequisites
- AVA AI app installed on Android device
- USB cable or WiFi connection
- Android Debug Bridge (adb) installed
- Computer with the AVA repository

### Installation Steps

```bash
# Step 1: Connect your device
adb devices

# Step 2: Push all models to device (single command!)
cd /Volumes/M-Drive/Coding/AVA/
adb push ava-ai-models /sdcard/

# Step 3: Verify
adb shell ls -lh /sdcard/ava-ai-models/
```

**That's it!** All models are now installed and ready to use.

---

## 📁 What Gets Installed

```
/sdcard/ava-ai-models/
├── embeddings/
│   └── AVA-384-Base-INT8.AON (22MB)
└── llm/
    ├── AVA-GE2-2B16/ (3.8MB)
    └── AVA-GE3-4B16/ (2.1GB)
```

**Total Size:** ~2.2GB

---

## 📱 For End Users (No Computer Required)

### Option 1: Download Link (Future)
*Coming soon: Direct download from within AVA app*

### Option 2: Cloud Storage Transfer

1. **Upload to Cloud:**
   - Zip the `ava-ai-models` folder on your computer
   - Upload to Google Drive, Dropbox, or similar

2. **Download on Device:**
   - Download zip file on your Android device
   - Extract to `/sdcard/ava-ai-models/`

3. **Verify in AVA:**
   - Open AVA AI app
   - Go to Settings → Models
   - Should see available models listed

---

## 🔍 Verify Installation

### Check Files Exist
```bash
# Check embeddings
adb shell ls /sdcard/ava-ai-models/embeddings/

# Check LLM models
adb shell ls /sdcard/ava-ai-models/llm/

# Check total size
adb shell du -sh /sdcard/ava-ai-models/
```

**Expected Output:** `2.2G    /sdcard/ava-ai-models/`

### In AVA App
1. Open AVA AI
2. Go to Settings → Models
3. You should see:
   - ✅ AVA-384-Base-INT8 (Embeddings)
   - ✅ AVA-GE2-2B16 (Gemma 2 2B)
   - ✅ AVA-GE3-4B16 (Gemma 3 4B)

---

## 🛠️ Troubleshooting

### Models Not Showing in App
**Solution:** Restart the AVA app after copying files.

### "No Space Left on Device"
**Check available space:**
```bash
adb shell df -h /sdcard/
```

**Required:** At least 3GB free (2.2GB models + overhead)

### Permission Denied
**Solution:** The `/sdcard/` path is accessible to all apps on Android.
If issues persist, try:
```bash
adb shell mkdir -p /sdcard/ava-ai-models
adb push ava-ai-models/embeddings /sdcard/ava-ai-models/
adb push ava-ai-models/llm /sdcard/ava-ai-models/
```

### Transfer Interrupted
**Resume from where you left off:**
```bash
# Remove incomplete transfer
adb shell rm -rf /sdcard/ava-ai-models/

# Restart full transfer
cd /Volumes/M-Drive/Coding/AVA/
adb push ava-ai-models /sdcard/
```

---

## 📊 Model Comparison

| Model | Size | RAM | Languages | Speed | Use Case |
|-------|------|-----|-----------|-------|----------|
| **AVA-384-Base-INT8** | 22MB | <1GB | English | Very Fast | Embeddings/Search |
| **AVA-GE2-2B16** | 1.2GB* | ~2GB | English | Fast | Quick responses |
| **AVA-GE3-4B16** | 2.1GB | ~3.2GB | 140+ | Medium | Multilingual chat |

*Note: AVA-GE2-2B16 currently only includes compiled code (3.8MB). Full model coming soon.

---

## 🔄 Update Models

To update models when new versions are released:

```bash
# Pull latest from repository
cd /Volumes/M-Drive/Coding/AVA/
git pull

# Re-push to device (overwrites old versions)
adb push ava-ai-models /sdcard/
```

---

## 🗑️ Uninstall Models

To remove all models and free up space:

```bash
adb shell rm -rf /sdcard/ava-ai-models/
```

To remove specific model:
```bash
# Remove Gemma 3 4B only
adb shell rm -rf /sdcard/ava-ai-models/llm/AVA-GE3-4B16/
```

---

## 📖 Related Documentation

- [Developer Manual Chapter 42: LLM Model Setup](Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [Developer Manual Chapter 44: AVA Naming Convention](Developer-Manual-Chapter44-AVA-Naming-Convention.md)
- [Developer Manual Chapter 45: AVA LLM Naming Standard](Developer-Manual-Chapter45-AVA-LLM-Naming-Standard.md)
- [Complete Installation Guide](LLM-Model-Installation-Guide.md) - Detailed technical guide

---

## 💡 Tips

1. **WiFi Transfer:** Use `adb connect <device-ip>:5555` for wireless transfer
2. **Faster Transfer:** USB 3.0 cable significantly speeds up the 2.1GB transfer
3. **Storage:** Use SD card if device has limited internal storage
4. **Backup:** Keep a backup of `ava-ai-models/` folder on your computer

---

## ❓ FAQ

**Q: Can I use models from the old location?**
A: Yes! AVA checks the old paths for backward compatibility, but we recommend using the new unified location.

**Q: Do I need all models?**
A: No. You can copy only the models you need:
- Minimum: `embeddings/AVA-384-Base-INT8.AON` (22MB)
- Add LLM: Choose either AVA-GE2-2B16 or AVA-GE3-4B16

**Q: Can I add my own models?**
A: Yes! Place compatible models in `/sdcard/ava-ai-models/llm/` following the AVA naming convention.

**Q: Will this work on emulator?**
A: Yes, but most emulators have limited storage (5-6GB). You may need to increase emulator storage or use a smaller model.

---

**Need Help?** See [LLM-Model-Installation-Guide.md](LLM-Model-Installation-Guide.md) for detailed troubleshooting.
