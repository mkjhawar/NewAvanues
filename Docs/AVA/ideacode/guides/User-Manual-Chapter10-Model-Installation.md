# User Manual - Chapter 10: Installing AI Models

**Date:** 2025-11-20
**Version:** 1.0
**Audience:** End Users

---

## Overview

AVA AI uses advanced language models to understand and respond to your requests. This chapter explains how to install these models on your device.

**Why separate installation?**
Models are large files (2-4GB) that would make the app download too big. Instead, you install only the models you need.

---

## What You Get

### Built-in Models
✅ **Basic NLU** (22MB) - Bundled with the app
- Understands basic commands
- Fast and lightweight
- Works immediately after install

### Optional Models (Recommended)

#### 🌟 AVA Nexus (Gemma 3 4B)
- **Size:** 2.1GB
- **Languages:** 140+ languages
- **Best for:** Multilingual users, complex conversations
- **RAM needed:** 4GB+ device recommended

#### ⚡ AVA Core (Gemma 2 2B)
- **Size:** 1.2GB (coming soon - currently 3.8MB compiled code)
- **Languages:** English
- **Best for:** Fast responses, English-only users
- **RAM needed:** 2GB+ device

---

## Installation Methods

### Method 1: Using a Computer (Recommended)

**What you need:**
- Computer with AVA repository or downloaded models
- USB cable
- Android Debug Bridge (adb) - [Download here](https://developer.android.com/tools/releases/platform-tools)

**Steps:**

1. **Connect your device**
   - Enable USB Debugging on your Android device
   - Settings → Developer Options → USB Debugging (ON)
   - Connect USB cable to computer

2. **Open terminal/command prompt**
   ```bash
   # Check device is connected
   adb devices
   ```
   Should show your device ID

3. **Copy models to device**
   ```bash
   # Navigate to AVA folder (adjust path for your system)
   cd /path/to/AVA/

   # Push all models (single command!)
   adb push ava-ai-models /sdcard/
   ```

4. **Wait for transfer**
   - Takes 5-10 minutes for 2.1GB
   - You'll see progress in the terminal

5. **Restart AVA app**
   - Close and reopen AVA AI
   - Go to Settings → Models
   - New models should appear

**Done!** ✅

---

### Method 2: Cloud Storage (No Computer)

**What you need:**
- Cloud storage account (Google Drive, Dropbox, etc.)
- Someone to upload the models for you, OR
- Download link from AVA website (coming soon)

**Steps:**

1. **Get the models**
   - Download `ava-ai-models.zip` from shared link
   - Or have someone upload to cloud storage

2. **Download on your device**
   - Open cloud storage app on Android
   - Download `ava-ai-models.zip`
   - Wait for download to complete

3. **Extract files**
   - Use Files app (Files by Google recommended)
   - Navigate to Downloads
   - Long-press `ava-ai-models.zip`
   - Tap "Extract"

4. **Move to correct location**
   - Files app → Internal Storage
   - Create folder named `ava-ai-models` if it doesn't exist
   - Move extracted files into this folder

5. **Restart AVA**
   - Close and reopen AVA AI app
   - Check Settings → Models

---

### Method 3: Automatic Download (NEW - Available Now!)

**What you need:**
- Active internet connection (WiFi recommended)
- Sufficient storage space (see requirements below)
- AVA app installed

**Steps:**

1. **Open AVA Settings**
   - Launch AVA AI app
   - Tap hamburger menu (☰)
   - Go to Settings → Model Management

2. **Browse Available Models**
   - Switch to **Available** tab
   - See 4 downloadable LLM models:
     - **QWEN2-1.5B** (1GB) - Fastest, good for older devices
     - **AVA-GE2-2B16** (~1.5GB) - Recommended for most devices
     - **AVA-GE3-4B16** (~1.7GB) - Better quality, needs more RAM
     - **PHI3-MINI** (~2.3GB) - Advanced features, power users

3. **Select and Download**
   - Tap on a model to see details
   - Check storage requirements
   - Tap **Download** button

4. **Monitor Progress**
   - See real-time progress bar
   - View download speed (MB/s)
   - Estimated time remaining shown
   - Notification when complete

5. **Automatic Installation**
   - Model installs automatically after download
   - Appears in "Downloaded" tab when ready
   - Ready to use immediately!

**Done!** ✅ Your model is ready to use.

---

### Download Features

**Progress Tracking:**
- Real-time progress percentage
- Download speed display
- Time remaining estimate
- Pause/resume support (automatic on connection loss)

**Resume Support:**
- Downloads resume if interrupted
- Works across app restarts
- Uses HTTP Range requests for efficient resumption

**Verification:**
- Automatic SHA-256 checksum verification
- Ensures download integrity
- Retries up to 3 times on failure

**Concurrent Downloads:**
- Download up to 2 models simultaneously
- Queue additional downloads automatically

**Storage Management:**
- Checks available space before download
- Requires 500MB safety buffer
- Shows clear error if insufficient space

---

### Storage Requirements

| Model | Download Size | Free Space Needed | Recommended For |
|-------|--------------|-------------------|-----------------|
| **QWEN2-1.5B** | 1GB | 1.5GB | Older phones (2-3GB RAM), basic tasks |
| **AVA-GE2-2B16** | 1.5GB | 2GB | Most users (4GB+ RAM), balanced |
| **AVA-GE3-4B16** | 1.7GB | 2.2GB | Better responses (6GB+ RAM) |
| **PHI3-MINI** | 2.3GB | 2.8GB | Power users (8GB+ RAM), complex tasks |

**Where Models Are Stored:**
```
/sdcard/ava-ai-models/llm-gguf/
├── qwen2-1_5b-instruct-q4_0.gguf
├── gemma-2-2b-it-Q4_K_M.gguf
├── gemma-2-9b-it-Q4_K_M.gguf
└── Phi-3-mini-4k-instruct-q4.gguf
```

---

## Verifying Installation

### In AVA App

1. Open AVA AI
2. Tap hamburger menu (☰)
3. Go to **Settings**
4. Tap **Models**

You should see:

```
✅ AVA-384-Base-INT8 (Embeddings)
   Status: Active
   Size: 22MB
   Location: Built-in

✅ AVA-GE3-4B16 (Gemma 3 4B)
   Status: Available
   Size: 2.1GB
   Location: External Storage

✅ AVA-GE2-2B16 (Gemma 2 2B)
   Status: Available
   Size: 1.2GB
   Location: External Storage
```

### Using File Manager

1. Open Files app
2. Navigate to Internal Storage
3. Look for `ava-ai-models` folder
4. Should contain:
   - `embeddings/` folder
   - `llm/` folder

---

## Selecting Your Model

After installation, choose which model to use:

1. **Open AVA AI**
2. **Go to Settings → Models**
3. **Tap on a model:**
   - AVA Nexus (recommended for multilingual)
   - AVA Core (recommended for English only)
4. **Tap "Use This Model"**
5. **Wait 10-20 seconds for model to load**
6. **Start chatting!**

---

## Storage Requirements

### Minimum Storage Needed

| What You Want | Storage Needed |
|---------------|----------------|
| Basic AVA (app only) | 150MB |
| + English support | +1.2GB (AVA Core) |
| + Multilingual support | +2.1GB (AVA Nexus) |
| Both LLM models | +3.3GB |

### Check Your Available Space

1. Settings → Storage
2. Look at "Available" space
3. Need at least 3GB free for AVA Nexus

**Tip:** If low on space:
- Install only AVA Core (English-only, smaller)
- Delete unused apps
- Move photos to cloud storage
- Use SD card if your device supports it

---

---

## Troubleshooting Downloads

### Download Stuck or Slow

**Problem:** Download not progressing or very slow speed

**Solutions:**
1. **Check WiFi connection**
   - Strong signal required for large downloads
   - Switch to WiFi if on mobile data
   - Downloads pause automatically if disconnected

2. **Resume download**
   - Downloads resume from where they left off
   - Close and reopen app if stuck
   - Check notification tray for progress

3. **Try again later**
   - Server may be busy
   - Network congestion during peak hours
   - AVA retries automatically (up to 3 times)

---

### Not Enough Storage

**Problem:** "Insufficient storage" error when trying to download

**Solutions:**
1. **Free up space:**
   - Delete unused apps (Settings → Apps)
   - Clear app caches (Settings → Storage → Free up space)
   - Move photos/videos to cloud storage
   - Use SD card if available

2. **Check requirements:**
   - AVA shows exact space needed
   - Includes 500MB safety buffer
   - See storage table above for each model

3. **Try smaller model:**
   - Start with QWEN2-1.5B (1GB)
   - Upgrade later when space available

---

### Download Failed

**Problem:** Download fails with error message

**Solutions:**
1. **Automatic retries:**
   - AVA retries up to 3 times automatically
   - Wait a few moments between retries

2. **Check internet:**
   - Verify internet connection working
   - Try opening web browser
   - Restart WiFi if needed

3. **Check storage:**
   - Ensure enough free space (see table above)
   - Clear cache if borderline on space

4. **Verify integrity:**
   - If download completes but fails verification
   - Checksum mismatch detected
   - AVA will retry download automatically

---

### Download Disappeared

**Problem:** Download started but can't find it now

**Check:**
1. **Notification area** - Download may be paused
2. **Settings → Model Management → Downloads** - See active downloads
3. **Storage** - Check if file exists at `/sdcard/ava-ai-models/llm-gguf/`

---

## Troubleshooting Installations

### Models Don't Appear in AVA

**Problem:** Copied models but don't see them in Settings → Models

**Solutions:**
1. **Restart the app** - Close completely and reopen
2. **Check file location:**
   - Should be `/sdcard/ava-ai-models/`
   - NOT `/sdcard/Android/data/.../ava-ai-models/`
3. **Check folder structure:**
   - Must have `embeddings/` and `llm/` subfolders
   - Files must be inside these folders

### "Not Enough Space" Error

**Problem:** Transfer fails with space error

**Solutions:**
1. **Free up space:**
   - Delete unused apps
   - Clear app caches (Settings → Storage → Free up space)
   - Move photos/videos to cloud
2. **Install fewer models:**
   - Choose only AVA Core (smaller) instead of AVA Nexus
   - Skip models you don't need

### Transfer Very Slow

**Problem:** Taking hours to transfer

**Solutions:**
1. **Use USB 3.0 cable** - Much faster than USB 2.0
2. **Connect directly to computer** - Not through USB hub
3. **Use Method 2** - Cloud storage may be faster depending on internet speed
4. **Be patient** - 2.1GB normally takes 5-15 minutes

### Model Loads But Doesn't Respond

**Problem:** Model seems to load but chat doesn't work

**Possible causes:**
1. **Insufficient RAM** - Need 4GB+ for AVA Nexus
2. **Partial transfer** - Files corrupted during copy
3. **Wrong model format** - Using old model files

**Solutions:**
1. **Try smaller model** - Use AVA Core instead of Nexus
2. **Re-copy models:**
   ```bash
   # Delete and recopy
   adb shell rm -rf /sdcard/ava-ai-models/
   adb push ava-ai-models /sdcard/
   ```
3. **Check device compatibility** - Settings → About Phone → RAM

---

## Updating Models

When new model versions are released:

### Automatic Updates (Future)
AVA will notify you when updates are available and allow one-tap updates.

### Manual Updates (Current)
1. Download new `ava-ai-models` folder
2. Follow installation steps again
3. New files will replace old ones

---

## Uninstalling Models

To remove models and free up space:

### Via File Manager
1. Open Files app
2. Navigate to Internal Storage
3. Find `ava-ai-models` folder
4. Long-press → Delete
5. Confirm deletion

### Via Computer
```bash
adb shell rm -rf /sdcard/ava-ai-models/
```

**Note:** AVA will continue to work with built-in basic NLU (no LLM features).

---

## Privacy & Security

### Where Are Models Stored?
- **Location:** `/sdcard/ava-ai-models/`
- **Accessibility:** Only AVA app can use them
- **Internet:** Models run 100% offline, no data sent to servers

### What Data Do Models See?
- **Your messages:** Used to generate responses
- **Your data:** Never leaves your device
- **No tracking:** Models don't collect or send data

### Can I Delete Models?
- **Yes!** You own the files and can delete anytime
- **Effect:** AVA will fallback to basic mode (no LLM chat)
- **Reversible:** Just reinstall to get features back

---

## FAQ

**Q: Do I need internet to use models?**
A: No! Once installed, models work completely offline.

**Q: Can I use models on multiple devices?**
A: Yes, copy the `ava-ai-models` folder to each device.

**Q: How often are models updated?**
A: Approximately every 3-6 months, or when significant improvements are available.

**Q: Can I use my own models?**
A: Advanced users can add compatible models following the AVA naming convention. See Developer Manual for details.

**Q: What if I only have 2GB storage free?**
A: Install only AVA Core (1.2GB) instead of AVA Nexus (2.1GB).

**Q: Are models the same as the app?**
A: No. The app is the interface, models are the AI "brains" that power conversations.

---

## Next Steps

After installing models:
- [Chapter 11: Using AVA Chat](User-Manual-Chapter11-Using-Chat.md)
- [Chapter 12: Training AVA](User-Manual-Chapter12-Training.md)
- [Chapter 13: Voice Commands](User-Manual-Chapter13-Voice-Commands.md)

---

**Need More Help?**
- Email: support@augmentalis.com
- Community: [AVA Forums](https://community.augmentalis.com)
- Developer Guide: [LLM-Model-Installation-Guide.md](LLM-Model-Installation-Guide.md)

---

**Document Version:** 2.0 (Major feature: Automatic model downloads)
**Last Updated:** 2025-12-06
**For AVA Version:** 1.0+

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 2.0 | 2025-12-06 | Added automatic download feature (Method 3), download troubleshooting, storage requirements table |
| 1.0 | 2025-11-20 | Initial release with manual installation methods |
