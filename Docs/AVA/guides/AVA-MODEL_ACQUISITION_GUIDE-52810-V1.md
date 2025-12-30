# MobileBERT Model Acquisition & Integration Guide

## Overview

This guide explains how to acquire, download, and integrate the MobileBERT ONNX model for AVA AI's intent classification system.

## Model Sources

### Option 1: Pre-Converted ONNX Model (Recommended)

**Model:** `onnx-community/mobilebert-uncased-ONNX`
- **URL:** https://huggingface.co/onnx-community/mobilebert-uncased-ONNX
- **Format:** ONNX (pre-converted from PyTorch)
- **Base Model:** google/mobilebert-uncased
- **Quantization:** FP32 (will need INT8 conversion for mobile)
- **Status:** Production-ready, auto-converted

**Advantages:**
- ✅ Already in ONNX format
- ✅ Community-maintained
- ✅ Compatible with ONNX Runtime
- ✅ Well-tested

**Disadvantages:**
- ⚠️ FP32 (larger size ~25MB vs INT8 ~12MB)
- ⚠️ Slower inference than quantized version

### Option 2: Convert PyTorch Model to ONNX with Quantization

**Model:** `google/mobilebert-uncased`
- **URL:** https://huggingface.co/google/mobilebert-uncased
- **Format:** PyTorch (requires conversion)
- **Size:** ~95MB (PyTorch), ~12MB (ONNX INT8)

**Conversion Process:** Use Hugging Face Optimum

## Step-by-Step Integration

### Step 1: Install Required Tools (Local Development)

```bash
# Create Python environment for model conversion
python3 -m venv model_env
source model_env/bin/activate  # On Windows: model_env\Scripts\activate

# Install required packages
pip install torch transformers optimum[exporters] onnx onnxruntime
```

### Step 2: Download and Convert Model

#### Option A: Download Pre-Converted ONNX Model

```bash
# Install Hugging Face CLI
pip install huggingface_hub

# Download model files
huggingface-cli download onnx-community/mobilebert-uncased-ONNX \
  --local-dir ./models/mobilebert-onnx

# Files downloaded:
# - model.onnx (~25MB FP32)
# - config.json
# - tokenizer.json
# - vocab.txt
```

#### Option B: Convert PyTorch to ONNX with INT8 Quantization

```python
# convert_mobilebert.py
from optimum.onnxruntime import ORTModelForSequenceClassification
from optimum.onnxruntime.configuration import AutoQuantizationConfig
from transformers import AutoTokenizer
import onnx

# Load model from Hugging Face
model_id = "google/mobilebert-uncased"
tokenizer = AutoTokenizer.from_pretrained(model_id)

# Export to ONNX
onnx_model = ORTModelForSequenceClassification.from_pretrained(
    model_id,
    export=True
)

# Save ONNX model
onnx_model.save_pretrained("./models/mobilebert-onnx")

# Save tokenizer and vocab
tokenizer.save_pretrained("./models/mobilebert-onnx")

print("✅ Model exported to ONNX format")
print(f"📁 Model location: ./models/mobilebert-onnx/model.onnx")
```

**Run conversion:**
```bash
python convert_mobilebert.py
```

#### Option C: Quantize to INT8 (for mobile optimization)

```python
# quantize_model.py
from optimum.onnxruntime import ORTModelForSequenceClassification
from optimum.onnxruntime.configuration import AutoQuantizationConfig
from optimum.onnxruntime import ORTQuantizer

# Load ONNX model
model = ORTModelForSequenceClassification.from_pretrained(
    "./models/mobilebert-onnx",
    export=False
)

# Configure quantization
qconfig = AutoQuantizationConfig.avx512_vnni(is_static=False, per_channel=False)

# Quantize to INT8
quantizer = ORTQuantizer.from_pretrained(model)
quantizer.quantize(
    save_dir="./models/mobilebert-onnx-int8",
    quantization_config=qconfig
)

print("✅ Model quantized to INT8")
print(f"📁 Quantized model: ./models/mobilebert-onnx-int8/model.onnx")
```

### Step 3: Verify Model Files

After download/conversion, verify you have:

```
models/
├── mobilebert-onnx-int8/
│   ├── model.onnx          # ~12-15MB (INT8)
│   ├── config.json         # Model configuration
│   ├── tokenizer.json      # Tokenizer config
│   └── vocab.txt           # ~460KB vocabulary
```

**Verify file sizes:**
```bash
ls -lh models/mobilebert-onnx-int8/

# Expected output:
# model.onnx      ~12-15MB
# vocab.txt       ~460KB
# config.json     ~2KB
# tokenizer.json  ~700KB
```

### Step 4: Copy to Android Assets

```bash
# Create assets directory in Android app
mkdir -p app/src/main/assets/models

# Copy model files
cp models/mobilebert-onnx-int8/model.onnx app/src/main/assets/models/mobilebert_int8.onnx
cp models/mobilebert-onnx-int8/vocab.txt app/src/main/assets/models/vocab.txt

# Verify files are in assets
ls -lh app/src/main/assets/models/
```

### Step 5: Update ModelManager URLs

Now update the ModelManager with real download URLs:

```kotlin
// features/nlu/src/androidMain/kotlin/.../ModelManager.kt

/**
 * Model download URLs
 * Using Hugging Face CDN for direct file access
 */
private val mobileBertUrl =
    "https://huggingface.co/onnx-community/mobilebert-uncased-ONNX/resolve/main/model.onnx"

private val vocabUrl =
    "https://huggingface.co/google/mobilebert-uncased/resolve/main/vocab.txt"

// Note: These are FP32 model URLs
// For INT8, you would need to host the quantized model yourself
// or use the bundled assets approach
```

**Alternative: Use bundled assets only (Recommended for v1.0)**

```kotlin
// Disable downloads, use bundled assets
suspend fun downloadModelsIfNeeded(
    onProgress: (Float) -> Unit = {}
): Result<Unit> = withContext(Dispatchers.IO) {
    // Skip download, always use bundled assets
    if (isModelAvailable()) {
        return@withContext Result.Success(Unit)
    }

    // Copy from assets
    return@withContext copyModelFromAssets()
}
```

### Step 6: Update Build Configuration

Add to `app/build.gradle.kts`:

```kotlin
android {
    // Prevent compression of ONNX models
    androidResources {
        noCompress += "onnx"
    }

    // Ensure assets are included
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
}
```

### Step 7: Test Model Initialization

Create a simple test:

```kotlin
// features/nlu/src/androidTest/kotlin/.../ModelInitializationTest.kt

@Test
fun testModelLoadsFromAssets() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val modelManager = ModelManager(context)

    // Copy from assets
    val result = modelManager.copyModelFromAssets()
    assertTrue(result is Result.Success)

    // Verify files exist
    assertTrue(modelManager.isModelAvailable())

    // Verify file sizes
    val modelSize = modelManager.getModelsSize()
    assertTrue(
        modelSize in 12_000_000L..20_000_000L,
        "Model size: ${modelSize / 1_000_000}MB"
    )
}

@Test
fun testClassifierInitialization() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val classifier = IntentClassifier.getInstance(context)
    val modelManager = ModelManager(context)

    // Ensure model is available
    modelManager.copyModelFromAssets()

    // Initialize classifier
    val result = classifier.initialize(modelManager.getModelPath())

    assertTrue(result is Result.Success, "Classifier initialization failed")
}

@Test
fun testRealInference() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val classifier = IntentClassifier.getInstance(context)
    val modelManager = ModelManager(context)

    modelManager.copyModelFromAssets()
    classifier.initialize(modelManager.getModelPath())

    // Test real inference
    val result = classifier.classifyIntent(
        utterance = "Turn on the lights",
        candidateIntents = listOf("control_lights", "check_weather", "set_alarm")
    )

    assertTrue(result is Result.Success)
    if (result is Result.Success) {
        println("Intent: ${result.data.intent}")
        println("Confidence: ${result.data.confidence}")
        println("Inference time: ${result.data.inferenceTimeMs}ms")

        // Validate performance
        assertTrue(result.data.inferenceTimeMs < 100,
            "Inference too slow: ${result.data.inferenceTimeMs}ms")
    }
}
```

Run tests:
```bash
./gradlew :features:nlu:connectedAndroidTest
```

## Troubleshooting

### Model File Too Large

**Problem:** App size increases significantly with bundled model

**Solutions:**
1. **Use INT8 quantization:** Reduces size from ~25MB to ~12MB
2. **Download on demand:** Remove from assets, download on first launch
3. **Android App Bundle:** Google Play handles multi-APK automatically

### ONNX Runtime Initialization Fails

**Problem:** `Model not found` or `Invalid model format`

**Debug steps:**
```kotlin
// Check if file exists
val modelFile = File(context.filesDir, "models/mobilebert_int8.onnx")
Log.d("ModelDebug", "Model exists: ${modelFile.exists()}")
Log.d("ModelDebug", "Model size: ${modelFile.length()} bytes")

// Try loading ONNX model directly
val env = OrtEnvironment.getEnvironment()
val session = env.createSession(modelFile.absolutePath)
Log.d("ModelDebug", "ONNX session created successfully")
```

**Common causes:**
- File corrupted during copy
- Incorrect file path
- ONNX Runtime version mismatch
- Model not compatible with mobile runtime

### Inference Too Slow (>100ms)

**Problem:** Inference exceeds performance budget

**Solutions:**
1. **Enable NNAPI:**
```kotlin
val sessionOptions = OrtSession.SessionOptions().apply {
    addNnapi()  // Hardware acceleration
}
```

2. **Use INT8 quantization:** Already reduces inference time by ~2x

3. **Reduce input length:** Truncate to fewer tokens
```kotlin
val tokenizer = BertTokenizer(context, maxSequenceLength = 64)  // Instead of 128
```

4. **Profile inference:**
```kotlin
android.os.Trace.beginSection("ONNX Inference")
val outputs = ortSession.run(inputs)
android.os.Trace.endSection()
```

View in Android Studio Profiler to identify bottlenecks.

### Vocabulary Not Loading

**Problem:** Tokenizer produces all UNK tokens

**Debug:**
```kotlin
val vocabFile = File(context.filesDir, "models/vocab.txt")
Log.d("VocabDebug", "Vocab exists: ${vocabFile.exists()}")
Log.d("VocabDebug", "Vocab lines: ${vocabFile.readLines().size}")

// Should have ~30,522 tokens for BERT
```

## Alternative: Simpler Approach for Testing

If model conversion is complex, use a simpler approach for initial testing:

### Option: Use Sentence Transformers (Smaller Model)

```python
# Download a smaller sentence-transformer model
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('paraphrase-MiniLM-L3-v2')  # Only 17MB
model.save('models/minilm-onnx', create_onnx_model=True)
```

This creates a much smaller model (~17MB) that's easier to integrate initially.

## Production Deployment Checklist

- [ ] Model files verified (correct sizes, not corrupted)
- [ ] INT8 quantization applied (12-15MB vs 25MB)
- [ ] Model bundled in assets OR download mechanism working
- [ ] Vocabulary file included (vocab.txt ~460KB)
- [ ] ONNX Runtime dependency added (`com.microsoft.onnxruntime:onnxruntime-mobile`)
- [ ] Model initialization tested on physical device
- [ ] Inference performance validated (< 50ms target)
- [ ] NNAPI acceleration enabled and tested
- [ ] Memory usage profiled (< 150MB with model loaded)
- [ ] Error handling for model load failures
- [ ] Fallback behavior defined (if model unavailable)

## Quick Start Script

Create this helper script for automated setup:

```bash
#!/bin/bash
# setup_model.sh

echo "🚀 AVA AI Model Setup"

# Step 1: Create directories
mkdir -p models/mobilebert-onnx
mkdir -p app/src/main/assets/models

# Step 2: Download model
echo "📥 Downloading MobileBERT ONNX model..."
huggingface-cli download onnx-community/mobilebert-uncased-ONNX \
  --local-dir models/mobilebert-onnx

# Step 3: Copy to assets
echo "📁 Copying to Android assets..."
cp models/mobilebert-onnx/model.onnx app/src/main/assets/models/mobilebert_int8.onnx
cp models/mobilebert-onnx/vocab.txt app/src/main/assets/models/vocab.txt

# Step 4: Verify
echo "✅ Setup complete!"
ls -lh app/src/main/assets/models/

echo ""
echo "Next steps:"
echo "1. Run ./gradlew :features:nlu:connectedAndroidTest"
echo "2. Check model initialization tests pass"
echo "3. Profile inference performance on device"
```

Make executable and run:
```bash
chmod +x setup_model.sh
./setup_model.sh
```

## References

- **ONNX Community Model:** https://huggingface.co/onnx-community/mobilebert-uncased-ONNX
- **Base Model:** https://huggingface.co/google/mobilebert-uncased
- **ONNX Runtime Mobile Docs:** https://onnxruntime.ai/docs/tutorials/mobile/
- **Hugging Face Optimum:** https://huggingface.co/docs/optimum/

---

**Last Updated:** 2025-01-28
**Status:** Ready for implementation
**Next Step:** Execute Step 2 (Download model)
