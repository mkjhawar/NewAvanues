# Vivoka Model File Paths - Device Deployment Guide

**Date:** 2025-11-20
**Module:** SpeechRecognition
**Engine:** Vivoka VSDK

---

## Overview

Vivoka language models and configuration files are stored on the device filesystem at runtime. This document specifies the expected paths for deployment and troubleshooting.

---

## Device File Paths

### Path Resolution Strategy (NEW - 2025-11-20)

**The app now checks MULTIPLE locations before downloading models:**

**Priority Order:**
1. **Internal App Storage** (current default)
2. **External App-Specific** (accessible via file manager)
3. **Shared Hidden Folder** (survives app uninstall)
4. **Download** (if not found in any location above)

**Implementation:** `VivokaPathResolver.kt` (NEW)

---

### 1. VSDK Configuration and Assets

#### Location 1: Internal App Storage (Primary)
```
/data/data/com.augmentalis.voiceos/files/vsdk/
```

#### Location 2: External App-Specific (NEW - Accessible)
```
/storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/
```

#### Location 3: Shared Hidden Folder (NEW - Survives Uninstall)
```
/storage/emulated/0/.voiceos/vivoka/vsdk/
```

**Contents (all locations):**
- `config/vsdk.json` - Main VSDK configuration file
- Core VSDK assets extracted from APK `assets/vsdk/` folder
- License files (if applicable)

**Source:** Extracted at first app launch from APK `assets/vsdk/` directory via `AssetsExtractor.extract()`

**Implementation:** `VivokaConfig.kt:86-103` with `VivokaPathResolver.kt`
```kotlin
val pathResolver = VivokaPathResolver(context)
val vsdkDir = pathResolver.resolveVsdkPath() // Checks all 3 locations
assetsPath = vsdkDir.absolutePath
```

---

### 2. Language Model Files

#### Location 1: Internal App Storage (Primary)
```
/data/data/com.augmentalis.voiceos/files/vsdk/data/csdk/asr/
```

#### Location 2: External App-Specific (NEW)
```
/storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/data/csdk/asr/
```

#### Location 3: Shared Hidden Folder (NEW - Recommended for Manual Deployment)
```
/storage/emulated/0/.voiceos/vivoka/vsdk/data/csdk/asr/
```

**Directory Structure:**
```
vivoka_models/
├── en-US/
│   ├── acoustic_model.bin
│   ├── language_model.bin
│   └── lexicon.bin
├── es-ES/
│   ├── acoustic_model.bin
│   ├── language_model.bin
│   └── lexicon.bin
└── [other languages...]
```

**Source:**
1. **Bundled Models** (if included in APK): Extracted from `assets/vivoka_models/` at first launch
2. **Downloaded Models**: Fetched on-demand via `VivokaLanguageRepository` and `FileZipManager`

**Implementation:** `VivokaInitializer.kt:20`
```kotlin
private const val MODELS_DIR = "vivoka_models"
```

---

## Download and Caching Strategy

### On-Demand Download Flow

1. **Check Local Cache:** `VivokaEngine.kt:217-221`
   ```kotlin
   if (languageRepository.isModelDownloaded(targetLanguage)) {
       Log.i(TAG, "VIVOKA_LANG_PREP Using cached/bundled model for $targetLanguage")
       return true
   }
   ```

2. **Download Model:** `VivokaEngine.kt:260-290`
   - Downloads ZIP file via `FileZipManager`
   - Saves to `/data/data/com.augmentalis.voiceos/files/vivoka_models/[lang]/`
   - Extracts model files
   - Validates checksums

3. **Initialize Model:** `VivokaEngine.kt:314-323`
   - Loads model into VSDK engine
   - Registers with `VivokaModel` for runtime access

---

## Pre-Deployment Options

### Option 1: Bundle Models in APK (Current Default)

**Pros:**
- Works offline immediately
- No download delay on first use
- Guaranteed availability

**Cons:**
- Increases APK size significantly (~100-200MB per language)
- Users download models they may not need

**Implementation:**
Place model files in:
```
app/src/main/assets/vivoka_models/[lang]/
```

---

### Option 2: On-Demand Download (Recommended for Production)

**Pros:**
- Smaller APK size
- Download only needed languages
- Easier to update models without app update

**Cons:**
- Requires network connection for first use
- Download delay (30-60 seconds per language)

**Implementation:**
1. Remove models from APK assets
2. Configure download URLs in `VivokaLanguageRepository.kt`
3. Implement download progress UI in `MainActivity.kt` or settings

---

## Manual Model Deployment (For Testing)

### Recommended: Shared Hidden Folder (NEW - Easiest Option)

**This is now the RECOMMENDED method for manual deployment:**

```bash
# Push entire VSDK folder structure to shared location
adb push ./vsdk /storage/emulated/0/.voiceos/vivoka/vsdk

# Or push specific language models
adb push ./es_voice_resource /storage/emulated/0/.voiceos/vivoka/vsdk/data/csdk/asr

# Verify files exist
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/

# Check specific language
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/data/csdk/asr/acmod/
```

**Benefits:**
- ✅ No special permissions needed
- ✅ Survives app uninstall
- ✅ App will automatically find files here
- ✅ Can be accessed via file manager for updates

---

### Alternative: External App-Specific Storage

```bash
# Push to external app-specific storage
adb push ./vsdk /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk

# Verify
adb shell ls -la /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/
```

**Benefits:**
- ✅ No special permissions needed
- ✅ Accessible via file manager
- ⚠️ Deleted on app uninstall

---

### Legacy: Internal App Storage (Requires run-as)

**Only use if external methods don't work:**

```bash
# Push model files to device (requires run-as)
adb push ./vsdk/data/csdk/asr /data/local/tmp/
adb shell run-as com.augmentalis.voiceos cp -r /data/local/tmp/asr /data/data/com.augmentalis.voiceos/files/vsdk/data/csdk/

# Verify files exist
adb shell run-as com.augmentalis.voiceos ls -la /data/data/com.augmentalis.voiceos/files/vsdk/

# Set correct permissions (if needed)
adb shell run-as com.augmentalis.voiceos chmod -R 755 /data/data/com.augmentalis.voiceos/files/vsdk/
```

### File Permissions:
- Owner: App UID (typically `u0_a123`)
- Permissions: `rwxr-xr-x` (755) for directories, `rw-r--r--` (644) for files

---

## Troubleshooting

### Models Not Found

**Symptom:** `VIVOKA_LANG_PREP Using cached/bundled model` log not shown, download initiated every time

**Check:**
```bash
adb shell ls -la /data/data/com.augmentalis.voiceos/files/vivoka_models/
```

**Fix:**
- Ensure models are in correct directory structure
- Check file permissions (should be readable by app)
- Verify model files are not corrupted (check file sizes)

---

### Config File Missing

**Symptom:** `Config file not found` error in logs

**Check:**
```bash
adb shell ls -la /data/data/com.augmentalis.voiceos/files/vsdk/
```

**Fix:**
- Delete app data and reinstall to trigger asset extraction
- Manually push `vsdk_config.json` via ADB

---

### Download Failures

**Symptom:** `VIVOKA_DOWNLOAD Language model download failed` in logs

**Common Causes:**
1. Network connectivity issues
2. Invalid download URLs in `VivokaLanguageRepository`
3. Insufficient storage space
4. Firewall blocking downloads

**Debug:**
- Enable verbose logging in `FileZipManager.kt`
- Check network connectivity
- Verify download URLs are accessible

---

## Related Files

| File | Purpose |
|------|---------|
| `VivokaInitializer.kt` | Asset extraction and SDK initialization |
| `VivokaEngine.kt` | Model download and preparation |
| `VivokaLanguageRepository.kt` | Language model metadata and URLs |
| `FileZipManager.kt` | ZIP download and extraction |
| `VivokaAssets.kt` | Asset validation and checksumming |

---

## TODO: User Configurable Engine Selection

Currently Vivoka is hardcoded in `VoiceOSService.kt:853`:
```kotlin
speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)
```

**Future Enhancement:**
Add user preference for engine selection (Vivoka/Vosk) in Settings:
1. Add preference in `SharedPreferences`
2. Read preference in `VoiceOSService.initializeVoiceRecognition()`
3. Initialize selected engine dynamically

**Implementation:** Tracked in backlog

---

**Document Status:** Active
**Last Updated:** 2025-11-20
**Maintainer:** Development Team
