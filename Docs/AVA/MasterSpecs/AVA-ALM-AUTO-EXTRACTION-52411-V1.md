# ALM Auto-Extraction Feature

**Version:** 1.0
**Last Updated:** 2025-11-24
**Status:** Active

---

## Overview

The ALM Auto-Extraction feature automatically detects and extracts `.ALM` (AVA LLM Model) archive files when models are loaded. This eliminates the need for manual extraction and makes model deployment as simple as pushing a single `.ALM` file to the device.

---

## What Are .ALM Files?

`.ALM` (AVA LLM Model) files are **tar archives** containing a complete model package:

```
AVA-GE3-4B16.ALM
├── AVA-GE3-4B16.ADco          # Compiled model code
├── AVALibrary.ADco            # TVM runtime library
├── ava-model-config.json      # Model configuration
├── tokenizer.model            # Tokenizer
├── tokenizer.json             # Tokenizer vocabulary
├── tokenizer_config.json      # Tokenizer settings
├── added_tokens.json          # Special tokens
├── ndarray-cache.json         # Weight mapping
└── params_shard_*.bin         # Model weights (69 files)
```

---

## How Auto-Extraction Works

### 1. **Automatic Scanning**
When a model is loaded, the `ALMExtractor` automatically scans these locations:
- `/sdcard/ava-ai-models/llm/`
- External storage: `<external-files-dir>/ava-ai-models/llm/`
- Fallback: `<files-dir>/models/llm/`

### 2. **Smart Extraction**
For each `.ALM` file found:
- Checks if already extracted (using timestamp markers)
- Extracts to directory with same name (e.g., `AVA-GE3-4B16.ALM` → `AVA-GE3-4B16/`)
- Verifies extraction succeeded (checks for essential files)
- Creates marker file to prevent re-extraction

### 3. **Transparent Loading**
After extraction, model loading proceeds normally with the extracted directory.

---

## Usage

### Simple Deployment (Recommended)

**Just push the .ALM file to the device:**

```bash
# Push Gemma 3 4B model
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/

# That's it! The app will auto-extract on first use
```

**On first model load:**
1. App detects `AVA-GE3-4B16.ALM`
2. Extracts to `AVA-GE3-4B16/` directory
3. Loads model from extracted directory
4. Creates `.alm_extracted` marker to skip re-extraction

**On subsequent loads:**
1. App sees marker file
2. Skips extraction (already done)
3. Loads model directly

---

## File Locations

### Development Machine

```
/Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/
├── AVA-GE2-2B16.ALM           # 3.8MB archive
├── AVA-GE3-4B16.ALM           # 2.1GB archive
├── AVA-GE2-2B16/              # Extracted directory
│   ├── AVA-GE2-2B16.ADco
│   ├── AVALibrary.ADco
│   └── .alm_extracted         # Marker file
└── AVA-GE3-4B16/              # Extracted directory
    ├── AVA-GE3-4B16.ADco
    ├── AVALibrary.ADco
    ├── ava-model-config.json
    ├── tokenizer.*
    ├── params_shard_*.bin
    └── .alm_extracted         # Marker file
```

### Android Device (After Auto-Extraction)

```
/sdcard/ava-ai-models/llm/
├── AVA-GE3-4B16.ALM           # Original archive (can be deleted)
└── AVA-GE3-4B16/              # Auto-extracted
    ├── AVA-GE3-4B16.ADco
    ├── AVALibrary.ADco
    ├── ava-model-config.json
    ├── tokenizer.*
    ├── params_shard_*.bin
    └── .alm_extracted         # Timestamp marker
```

---

## Deployment Scenarios

### Scenario 1: Fresh Install (Easiest)

Push just the `.ALM` file:

```bash
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/
```

**Result:**
- First model load: Auto-extracts
- Subsequent loads: Uses extracted directory

---

### Scenario 2: Update Existing Model

Replace the `.ALM` file:

```bash
# Remove old files
adb shell rm -rf /sdcard/ava-ai-models/llm/AVA-GE3-4B16
adb shell rm /sdcard/ava-ai-models/llm/AVA-GE3-4B16.ALM

# Push new version
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/
```

**Result:**
- New timestamp on `.ALM` file invalidates old marker
- Auto-extracts new version on next load

---

### Scenario 3: Pre-Extracted Directory (Traditional)

Push the extracted directory directly:

```bash
adb push AVA-GE3-4B16/ /sdcard/ava-ai-models/llm/
```

**Result:**
- No `.ALM` file, so no extraction needed
- Loads directly from directory
- Works exactly as before (backward compatible)

---

## API Reference

### ALMExtractor Class

```kotlin
class ALMExtractor(context: Context)
```

#### Main Methods

```kotlin
// Extract all .ALM files in standard locations
suspend fun extractAllALMFiles(): List<File>

// Extract a specific .ALM file
suspend fun extractALMFile(almFile: File): File?

// Check if model needs extraction
fun needsExtraction(modelDir: File): Boolean

// Get extraction status for a model
fun getExtractionStatus(modelName: String): ExtractionStatus

// Clean up .ALM files after extraction
suspend fun cleanupALMFiles(deleteAfterExtraction: Boolean = false)
```

#### ExtractionStatus

```kotlin
sealed class ExtractionStatus {
    // Model directory exists (no .ALM)
    data class DirectoryExists(val directory: File) : ExtractionStatus()

    // .ALM extracted successfully
    data class Extracted(val directory: File) : ExtractionStatus()

    // .ALM needs extraction
    data class NeedsExtraction(val almFile: File) : ExtractionStatus()

    // Neither found
    object NotFound : ExtractionStatus()
}
```

---

## Integration with TVMModelLoader

The `TVMModelLoader` automatically calls `ALMExtractor` before loading models:

```kotlin
override suspend fun loadModel(config: ModelConfig): LoadedModel {
    // Step 1: Auto-extract any .ALM files
    almExtractor.extractAllALMFiles()

    // Step 2: Continue with normal model loading
    // ...
}
```

**No code changes needed in app code!** Just load models normally:

```kotlin
val config = ModelConfig(
    modelPath = "/sdcard/ava-ai-models/llm/AVA-GE3-4B16",
    modelName = "AVA-GE3-4B16"
)
val model = tvmModelLoader.loadModel(config)
```

---

## Extraction Process Details

### 1. Detection
```kotlin
// Scans for *.ALM files in:
/sdcard/ava-ai-models/llm/
<external-files-dir>/ava-ai-models/llm/
<files-dir>/models/llm/
```

### 2. Marker Check
```kotlin
// Checks for .alm_extracted marker file
// Contains timestamp of source .ALM file
// If timestamp matches, skip extraction
val markerFile = File(extractDir, ".alm_extracted")
val almTimestamp = almFile.lastModified()
```

### 3. Extraction
```kotlin
// Uses Apache Commons Compress
TarArchiveInputStream(FileInputStream(almFile)).use { tarIn ->
    // Extract each entry to destination directory
    // Preserve file permissions
}
```

### 4. Verification
```kotlin
// Verifies essential files exist:
- *.ADco device code (required)
- AVALibrary.ADco (required)
- tokenizer.* files (required)
```

### 5. Marker Creation
```kotlin
// Creates marker with ALM timestamp
markerFile.writeText("${almFile.lastModified()}")
```

---

## Advantages

### 1. **Simpler Deployment**
- Single file push instead of entire directory
- Easier to manage and distribute
- Less chance of incomplete transfers

### 2. **Atomic Updates**
- Replace one file instead of many
- Either old or new model, never partial
- Safer updates over slow connections

### 3. **Bandwidth Efficient**
- Tar compression can save space
- Only push what changed
- Optional: Delete .ALM after extraction

### 4. **Version Control**
- Clear versioning with single file
- Easier to track what's deployed
- Simpler rollback (just replace .ALM)

### 5. **Backward Compatible**
- Extracted directories still work
- No breaking changes
- Gradual adoption possible

---

## Performance Considerations

### Extraction Time

| Model | Archive Size | Extraction Time | Storage Used |
|-------|-------------|-----------------|--------------|
| AVA-GE2-2B16 | 3.8MB | ~0.5 seconds | 3.8MB |
| AVA-GE3-4B16 | 2.1GB | ~30-60 seconds | 2.1GB |

**Notes:**
- Extraction happens once per model
- Subsequent loads are instant (no re-extraction)
- Storage: Extracted directory uses same space as archive

### Storage Options

**Keep Both (Default):**
```
AVA-GE3-4B16.ALM        # 2.1GB
AVA-GE3-4B16/           # 2.1GB
Total: 4.2GB
```

**Delete Archive After Extraction:**
```kotlin
almExtractor.cleanupALMFiles(deleteAfterExtraction = true)
```
```
AVA-GE3-4B16/           # 2.1GB
Total: 2.1GB
```

---

## Troubleshooting

### Issue: Extraction Fails

**Symptom:** Model won't load, extraction errors in logs

**Check:**
```bash
# Verify .ALM file is valid tar
tar -tzf AVA-GE3-4B16.ALM | head

# Check device storage
adb shell df -h /sdcard
```

**Fix:**
```bash
# Re-create .ALM archive
cd ava-ai-models/llm/AVA-GE3-4B16
tar -cf ../AVA-GE3-4B16.ALM *

# Push to device
adb push ../AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/
```

---

### Issue: Re-extraction on Every Load

**Symptom:** Slow model loading, extraction happens repeatedly

**Check:**
```bash
# Verify marker file exists
adb shell ls -la /sdcard/ava-ai-models/llm/AVA-GE3-4B16/.alm_extracted

# Check marker content
adb shell cat /sdcard/ava-ai-models/llm/AVA-GE3-4B16/.alm_extracted
```

**Fix:**
```bash
# Manually create marker (use .ALM timestamp)
adb shell "echo '1732488060000' > /sdcard/ava-ai-models/llm/AVA-GE3-4B16/.alm_extracted"
```

---

### Issue: Insufficient Storage

**Symptom:** Extraction fails with "No space left on device"

**Check:**
```bash
adb shell df -h /sdcard
```

**Fix:**
- Free up space on device
- Use smaller model (Gemma 2 instead of Gemma 3)
- Extract on device with more storage, then move

---

## Migration Guide

### From Extracted Directories to .ALM

**Current setup:**
```bash
adb push AVA-GE3-4B16/ /sdcard/ava-ai-models/llm/
```

**New setup:**
```bash
# Create .ALM archive (one-time)
cd ava-ai-models/llm/AVA-GE3-4B16
tar -cf ../AVA-GE3-4B16.ALM *

# Deploy
adb push ../AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/

# Optional: Remove old directory
adb shell rm -rf /sdcard/ava-ai-models/llm/AVA-GE3-4B16
```

**Result:**
- Smaller deployment (compression)
- Faster transfers (single file)
- Auto-extraction on first use

---

## Dependencies

### Apache Commons Compress

**Added to build.gradle.kts:**
```kotlin
implementation("org.apache.commons:commons-compress:1.25.0")
```

**Used for:**
- TAR archive reading
- Preserving file permissions
- Efficient streaming extraction

---

## Related Documentation

- [MODEL-FILES-REQUIRED.md](MODEL-FILES-REQUIRED.md) - Complete file requirements
- [AVA File Format Standards](standards/AVA-FILE-FORMATS.md) - .ALM specification
- [Developer Manual Chapter 44](Developer-Manual-Chapter44-AVA-Naming-Convention.md) - Naming conventions

---

## Examples

### Example 1: Deploy Single Model

```bash
# Development machine
cd /Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/

# Push .ALM archive
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/

# Launch app
adb shell am start -n com.augmentalis.ava/.MainActivity

# Check logs
adb logcat | grep ALMExtractor
```

**Expected logs:**
```
ALMExtractor: Scanning for .ALM files in: /sdcard/ava-ai-models/llm
ALMExtractor: Found .ALM file: AVA-GE3-4B16.ALM
ALMExtractor: Extracting ALM: AVA-GE3-4B16.ALM -> AVA-GE3-4B16
ALMExtractor: Successfully extracted ALM: AVA-GE3-4B16.ALM
ALMExtractor: Extracted 1 .ALM archive(s)
```

---

### Example 2: Update Existing Model

```bash
# Remove old version
adb shell rm -rf /sdcard/ava-ai-models/llm/AVA-GE3-4B16*

# Push new version
adb push AVA-GE3-4B16-v2.ALM /sdcard/ava-ai-models/llm/AVA-GE3-4B16.ALM

# Next app launch will auto-extract new version
```

---

### Example 3: Manual Extraction

```bash
# Extract on device directly
adb shell tar -xf /sdcard/ava-ai-models/llm/AVA-GE3-4B16.ALM \
              -C /sdcard/ava-ai-models/llm/

# Create marker manually
adb shell "echo '$(date +%s)000' > /sdcard/ava-ai-models/llm/AVA-GE3-4B16/.alm_extracted"

# Delete archive to save space
adb shell rm /sdcard/ava-ai-models/llm/AVA-GE3-4B16.ALM
```

---

## Best Practices

### 1. **Distribution**
- Always distribute models as `.ALM` archives
- Include MD5 checksum file
- Version the archive name if needed: `AVA-GE3-4B16-v1.0.ALM`

### 2. **Deployment**
- Push `.ALM` to standard location: `/sdcard/ava-ai-models/llm/`
- Let auto-extraction handle the rest
- Delete `.ALM` after extraction if storage is limited

### 3. **Updates**
- Delete old directory before pushing new `.ALM`
- Timestamp change will trigger re-extraction
- Test on one device before mass deployment

### 4. **Storage Management**
```kotlin
// After successful load, clean up archives
almExtractor.cleanupALMFiles(deleteAfterExtraction = true)
```

---

## Summary

The ALM Auto-Extraction feature provides:

✅ **Simple deployment** - Push one file instead of directories
✅ **Automatic extraction** - No manual steps required
✅ **Smart caching** - Extracts once, reuses forever
✅ **Backward compatible** - Extracted directories still work
✅ **Safe updates** - Timestamp-based re-extraction
✅ **Space efficient** - Optional cleanup after extraction

**Recommendation:** Use `.ALM` archives for all model deployments going forward!

---

**Document Version:** 1.0
**Last Updated:** 2025-11-24
**Author:** AVA Development Team
