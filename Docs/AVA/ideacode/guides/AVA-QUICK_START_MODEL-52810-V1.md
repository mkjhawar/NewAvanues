# Quick Start: MobileBERT Model Setup

## TL;DR - Just Get It Working

```bash
# Option 1: Fastest (Recommended for testing)
cd "/Volumes/M Drive/Coding/AVA AI"
./scripts/setup_model.sh

# Choose option 1 when prompted
# Wait for download (~25MB)
# Done!
```

## What This Does

1. Downloads pre-converted MobileBERT ONNX model from Hugging Face
2. Copies model files to `app/src/main/assets/models/`
3. Verifies files are correct size
4. Ready to build and test!

## Files You'll Get

```
app/src/main/assets/models/
├── mobilebert_int8.onnx  (~25MB for FP32, ~12MB for INT8)
└── vocab.txt             (~460KB, 30,522 tokens)
```

## Next: Test the Model

```bash
# Build the app
./gradlew assembleDebug

# Run NLU tests (requires physical device or emulator)
./gradlew :features:nlu:connectedAndroidTest

# Install on device
./gradlew installDebug
```

## Verify Model Works

Open the app and navigate to settings/debug:
- Check "Model Status": Should show "✅ Loaded"
- Check "Model Size": Should show "~25MB" or "~12MB"
- Test inference: Type "Turn on the lights"
- Expected result: `control_lights` with >0.7 confidence

## Performance Expectations

### FP32 Model (25MB)
- **Inference Time:** 60-100ms (first run), 40-80ms (warm)
- **Memory:** ~150MB peak
- **Accuracy:** Best
- **Use Case:** Development, high-accuracy testing

### INT8 Model (12MB)
- **Inference Time:** 30-60ms (first run), 20-50ms (warm)
- **Memory:** ~120MB peak
- **Accuracy:** ~97% of FP32 (negligible loss)
- **Use Case:** Production deployment

## Troubleshooting

### "Permission denied" when running script
```bash
chmod +x scripts/setup_model.sh
./scripts/setup_model.sh
```

### "Python not found"
```bash
# Install Python 3
brew install python3  # macOS
# or download from python.org
```

### "Model download failed"
- Check internet connection
- Try option 2 (conversion) instead
- Or manually download from: https://huggingface.co/onnx-community/mobilebert-uncased-ONNX

### "Model file too large"
Use option 2 for INT8 quantization:
```bash
./scripts/setup_model.sh
# Choose option 2
# This reduces size from 25MB to ~12MB
```

### "Test inference slow (>100ms)"
This is normal on first run. Subsequent runs should be faster:
- Enable NNAPI (already configured in IntentClassifier)
- Use INT8 quantized model
- Test on physical device (not emulator)

## Manual Download (If Scripts Fail)

```bash
# 1. Install Hugging Face CLI
pip3 install huggingface_hub

# 2. Download model
huggingface-cli download onnx-community/mobilebert-uncased-ONNX \
  --local-dir ./models/mobilebert-onnx

# 3. Create assets directory
mkdir -p app/src/main/assets/models

# 4. Copy files
cp models/mobilebert-onnx/model.onnx app/src/main/assets/models/mobilebert_int8.onnx
cp models/mobilebert-onnx/vocab.txt app/src/main/assets/models/vocab.txt

# 5. Verify
ls -lh app/src/main/assets/models/
```

## Advanced: INT8 Quantization

For production deployment (smaller size, faster inference):

```bash
# Run conversion script with quantization
python3 scripts/convert_mobilebert.py

# Or use setup script option 2
./scripts/setup_model.sh
# Choose option 2
```

This will:
1. Download PyTorch model
2. Convert to ONNX
3. Quantize to INT8
4. Copy to assets
5. Verify with test inference

## What's Next?

After model setup:

1. **Run Integration Tests**
   ```bash
   ./gradlew :features:nlu:connectedAndroidTest
   ```

2. **Profile Performance**
   - Open Android Studio Profiler
   - Run app on physical device
   - Navigate to Teach-Ava and test classification
   - Check CPU/Memory usage during inference

3. **Validate Performance Budgets**
   - Tokenization: < 5ms ✅
   - Inference: < 50ms (target), < 100ms (max)
   - Total E2E: < 60ms (target), < 120ms (max)

4. **Start Using Teach-Ava**
   - Add training examples
   - Test intent classification
   - Verify usage tracking works
   - Check deduplication (try adding same example twice)

## Resources

- **Full Guide:** [MODEL_ACQUISITION_GUIDE.md](./MODEL_ACQUISITION_GUIDE.md)
- **Performance Testing:** [PERFORMANCE_VALIDATION.md](./PERFORMANCE_VALIDATION.md)
- **NLU Tests:** [features/nlu/NLU_INTEGRATION_TESTS.md](./features/nlu/NLU_INTEGRATION_TESTS.md)
- **Hugging Face Model:** https://huggingface.co/onnx-community/mobilebert-uncased-ONNX

---

**Estimated Time:** 5-10 minutes (depending on download speed)

**Disk Space Required:**
- Download: ~50MB (model + dependencies)
- Final app size increase: ~12-25MB (depending on quantization)

**Status:** ✅ Ready to use
