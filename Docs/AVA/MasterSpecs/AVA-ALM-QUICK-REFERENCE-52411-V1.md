# ALM Auto-Extraction - Quick Reference

**TL;DR:** Push `.ALM` file to device, app auto-extracts on first load. Done!

---

## Quick Commands

### Deploy Model
```bash
# That's it!
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/
```

### Verify Extraction
```bash
# Check extracted directory
adb shell ls -la /sdcard/ava-ai-models/llm/AVA-GE3-4B16/

# Check marker file
adb shell cat /sdcard/ava-ai-models/llm/AVA-GE3-4B16/.alm_extracted
```

### Watch Logs
```bash
adb logcat | grep ALMExtractor
```

### Test Script
```bash
cd DeveloperTools
./test-alm-extraction.sh AVA-GE3-4B16
```

---

## File Locations

### Development Machine
```
/Volumes/M-Drive/Coding/AVA/ava-ai-models/llm/
├── AVA-GE2-2B16.ALM    # 3.8MB
└── AVA-GE3-4B16.ALM    # 2.1GB
```

### Android Device
```
/sdcard/ava-ai-models/llm/
├── AVA-GE3-4B16.ALM         # Original (can delete)
└── AVA-GE3-4B16/            # Extracted
    └── .alm_extracted       # Marker
```

---

## What Happens

### First Load
```
1. Scan for .ALM files
2. Extract AVA-GE3-4B16.ALM → AVA-GE3-4B16/
3. Create .alm_extracted marker
4. Load model
Time: ~30-60 seconds (one-time)
```

### Subsequent Loads
```
1. Scan for .ALM files
2. Check marker → already extracted
3. Skip extraction
4. Load model
Time: <0.1 seconds
```

---

## Expected Logs

```
ALMExtractor: Scanning for .ALM files in: /sdcard/ava-ai-models/llm
ALMExtractor: Found .ALM file: AVA-GE3-4B16.ALM
ALMExtractor: Extracting ALM: AVA-GE3-4B16.ALM -> AVA-GE3-4B16
ALMExtractor: Successfully extracted ALM: AVA-GE3-4B16.ALM
```

---

## File Structure

### .ALM Contents
```
AVA-GE3-4B16.ALM (tar archive)
├── AVA-GE3-4B16.ADco          # Required
├── AVALibrary.ADco            # Required
├── tokenizer.model            # Required
├── ava-model-config.json
├── ndarray-cache.json
└── params_shard_*.bin (69 files)
```

---

## Common Tasks

### Create .ALM
```bash
cd ava-ai-models/llm/AVA-GE3-4B16
tar -cf ../AVA-GE3-4B16.ALM *
```

### Update Model
```bash
# Remove old
adb shell rm -rf /sdcard/ava-ai-models/llm/AVA-GE3-4B16*

# Push new
adb push AVA-GE3-4B16.ALM /sdcard/ava-ai-models/llm/
```

### Save Space
```bash
# Delete .ALM after extraction
adb shell rm /sdcard/ava-ai-models/llm/AVA-GE3-4B16.ALM
```

---

## Troubleshooting

### Extraction Fails
```bash
# Check .ALM is valid
tar -tzf AVA-GE3-4B16.ALM | head

# Check logs
adb logcat | grep -E "ALMExtractor|ERROR"
```

### Re-extracts Every Time
```bash
# Check marker
adb shell cat /sdcard/ava-ai-models/llm/AVA-GE3-4B16/.alm_extracted

# Create marker manually
adb shell "echo '$(date +%s)000' > /sdcard/ava-ai-models/llm/AVA-GE3-4B16/.alm_extracted"
```

### Insufficient Storage
```bash
# Check free space
adb shell df -h /sdcard

# Clean old models
adb shell rm -rf /sdcard/ava-ai-models/llm/old-model-*
```

---

## API Quick Reference

### In Code
```kotlin
val extractor = ALMExtractor(context)

// Auto-extract all .ALM files
extractor.extractAllALMFiles()

// Check status
val status = extractor.getExtractionStatus("AVA-GE3-4B16")

// Clean up archives
extractor.cleanupALMFiles(deleteAfterExtraction = true)
```

### In TVMModelLoader
```kotlin
// Automatic! No code changes needed
val model = tvmModelLoader.loadModel(config)
```

---

## Performance

| Model | Size | First Load | Cached Load |
|-------|------|------------|-------------|
| GE2-2B | 3.8MB | 0.5s | <0.1s |
| GE3-4B | 2.1GB | 30-60s | <0.1s |

---

## Documentation

- Full Guide: `docs/ALM-AUTO-EXTRACTION.md`
- File Requirements: `docs/MODEL-FILES-REQUIRED.md`
- Test Guide: `Universal/.../loader/README-ALM-TESTS.md`
- Summary: `docs/ALM-EXTRACTION-SUMMARY.md`

---

## Run Tests

```bash
# All tests
./gradlew :Universal:AVA:Features:LLM:connectedAndroidTest --tests "ALM*"

# Specific test
./gradlew :Universal:AVA:Features:LLM:connectedAndroidTest \
  --tests "ALMExtractorIntegrationTest.testExtractSimpleALM"
```

---

## Key Points

✅ **No code changes** - Just push .ALM file
✅ **Automatic** - Extracts on first load
✅ **Fast after first load** - Cached via marker
✅ **Backward compatible** - Extracted dirs still work
✅ **Space efficient** - Delete .ALM after extraction

---

**Need Help?** See full documentation in `docs/ALM-AUTO-EXTRACTION.md`
