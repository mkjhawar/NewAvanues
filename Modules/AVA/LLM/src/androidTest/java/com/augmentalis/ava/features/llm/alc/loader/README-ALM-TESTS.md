# ALM Extraction Tests

This directory contains comprehensive tests for the ALM (AVA LLM Model) auto-extraction feature.

## Test Files

### 1. ALMExtractorIntegrationTest.kt
Tests the core `ALMExtractor` functionality in isolation.

**Test Cases:**
- ✅ Extract simple .ALM archive
- ✅ Extraction creates marker file
- ✅ Repeated extraction is skipped (caching)
- ✅ Changed .ALM triggers re-extraction
- ✅ Verification fails without required files
- ✅ Extract multiple .ALM files
- ✅ Extraction status detection
- ✅ needsExtraction check
- ✅ Extraction with nested directories
- ✅ Cleanup .ALM files after extraction

### 2. TVMModelLoaderALMTest.kt
Tests the integration of ALM extraction with model loading.

**Test Cases:**
- ✅ ALM extraction triggered on model load
- ✅ Multiple .ALM files extracted
- ✅ Extraction skipped on second load
- ✅ Extraction status can be queried
- ✅ Pre-extracted directory works without .ALM

## Running the Tests

### From Android Studio

1. Open the project in Android Studio
2. Navigate to the test file
3. Right-click on the test class or method
4. Select "Run 'TestName'"

### From Command Line

```bash
# Run all ALM tests
./gradlew :Universal:AVA:Features:LLM:connectedAndroidTest \
  --tests "com.augmentalis.ava.features.llm.alc.loader.ALM*"

# Run specific test class
./gradlew :Universal:AVA:Features:LLM:connectedAndroidTest \
  --tests "ALMExtractorIntegrationTest"

# Run specific test method
./gradlew :Universal:AVA:Features:LLM:connectedAndroidTest \
  --tests "ALMExtractorIntegrationTest.testExtractSimpleALM"
```

### Device Requirements

- Android device or emulator with API 28+
- USB debugging enabled
- Sufficient storage (~3GB for full tests)

## Manual Testing

For manual testing with real .ALM files, use the provided script:

```bash
cd /Volumes/M-Drive/Coding/AVA/DeveloperTools
./test-alm-extraction.sh AVA-GE2-2B16
```

This script:
1. Verifies .ALM file exists
2. Cleans old files on device
3. Pushes .ALM to device
4. Optionally launches app and watches logs

## Test Coverage

### ALMExtractor Coverage

| Method | Test Coverage |
|--------|---------------|
| `extractAllALMFiles()` | ✅ Multiple files |
| `extractALMFile()` | ✅ Simple & nested |
| `needsExtraction()` | ✅ Before/after |
| `getExtractionStatus()` | ✅ All states |
| `cleanupALMFiles()` | ✅ With deletion |
| Marker creation | ✅ Timestamp |
| Marker validation | ✅ Re-extraction |
| Verification | ✅ Required files |

### TVMModelLoader Coverage

| Scenario | Test Coverage |
|----------|---------------|
| Auto-extraction on load | ✅ |
| Multiple .ALM files | ✅ |
| Extraction caching | ✅ |
| Pre-extracted directory | ✅ |
| Status queries | ✅ |

## Expected Behavior

### First Model Load
```
1. App scans for .ALM files
2. Finds AVA-GE3-4B16.ALM
3. Extracts to AVA-GE3-4B16/
4. Creates .alm_extracted marker
5. Loads model from extracted dir
```

### Subsequent Model Loads
```
1. App scans for .ALM files
2. Finds AVA-GE3-4B16.ALM
3. Checks marker file
4. Timestamp matches - skip extraction
5. Loads model directly
```

### Model Update
```
1. New .ALM pushed (different timestamp)
2. App scans for .ALM files
3. Marker timestamp doesn't match
4. Re-extracts to AVA-GE3-4B16/
5. Updates marker with new timestamp
6. Loads updated model
```

## Troubleshooting Tests

### Test Fails: "No space left on device"

**Solution:**
```bash
# Clear device cache
adb shell rm -rf /sdcard/ava-ai-models/llm/TestALM*

# Or use emulator with more storage
```

### Test Fails: "TarArchiveInputStream not found"

**Solution:**
Ensure Apache Commons Compress is in dependencies:
```kotlin
implementation("org.apache.commons:commons-compress:1.25.0")
```

### Test Fails: "Permission denied"

**Solution:**
```bash
# Grant storage permissions
adb shell pm grant com.augmentalis.ava android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.augmentalis.ava android.permission.WRITE_EXTERNAL_STORAGE
```

### Tests Pass but Real Model Fails

**Check:**
1. Real .ALM file is valid tar archive:
   ```bash
   tar -tzf AVA-GE3-4B16.ALM | head
   ```

2. .ALM contains required files:
   - AVALibrary.ADco
   - {ModelName}.ADco
   - tokenizer.model
   - ava-model-config.json

3. Device logs for errors:
   ```bash
   adb logcat | grep -E "ALMExtractor|ERROR"
   ```

## Integration with CI/CD

### GitHub Actions Example

```yaml
- name: Run ALM Extraction Tests
  run: |
    ./gradlew :Universal:AVA:Features:LLM:connectedAndroidTest \
      --tests "ALM*" \
      --stacktrace
```

### Test Reports

After running tests, reports are available at:
```
Universal/AVA/Features/LLM/build/reports/androidTests/connected/
```

## Performance Benchmarks

| Model | Archive Size | Extraction Time | Test Device |
|-------|-------------|-----------------|-------------|
| Small (test) | 1KB | <0.1s | Emulator |
| AVA-GE2-2B16 | 3.8MB | ~0.5s | Pixel 6 |
| AVA-GE3-4B16 | 2.1GB | ~45s | Pixel 6 |

**Notes:**
- Extraction is one-time per model
- Subsequent loads: <0.1s (marker check only)
- Storage: Same as archive size (no compression overhead)

## Related Documentation

- [ALM-AUTO-EXTRACTION.md](/Volumes/M-Drive/Coding/AVA/docs/ALM-AUTO-EXTRACTION.md) - Feature documentation
- [MODEL-FILES-REQUIRED.md](/Volumes/M-Drive/Coding/AVA/docs/MODEL-FILES-REQUIRED.md) - File requirements
- [AVA-FILE-FORMATS.md](/Volumes/M-Drive/Coding/AVA/docs/standards/AVA-FILE-FORMATS.md) - File format specs

## Contributing

When adding new tests:

1. Follow existing naming conventions
2. Add descriptive comments
3. Clean up test files in `@After`
4. Update this README with new test cases
5. Verify tests pass on real device

## Questions?

See main documentation or contact the AVA development team.
