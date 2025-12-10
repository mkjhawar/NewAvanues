# Model Quantization Guide for AVA

**Project:** ava
**Version:** 1.0
**Last Updated:** 2025-11-06

Complete guide to quantizing ONNX embedding models to reduce file size by 50-75% with minimal quality loss.

---

## Table of Contents

1. [Overview](#overview)
2. [What is Quantization?](#what-is-quantization)
3. [Quick Start](#quick-start)
4. [Quantization Modes](#quantization-modes)
5. [Size Reduction Examples](#size-reduction-examples)
6. [Quality vs Size Trade-offs](#quality-vs-size-trade-offs)
7. [Step-by-Step Guide](#step-by-step-guide)
8. [Batch Quantization](#batch-quantization)
9. [Testing Quantized Models](#testing-quantized-models)
10. [Troubleshooting](#troubleshooting)

---

## Overview

**Model quantization** reduces the file size of AI models by using lower-precision numbers (8-bit or 16-bit integers instead of 32-bit floats).

**Benefits:**
- ✅ **50-75% smaller file size** - Less storage, faster downloads
- ✅ **Faster inference** - INT8 operations are faster than FP32
- ✅ **Lower memory usage** - Smaller models use less RAM
- ✅ **Minimal quality loss** - 95-98% of original accuracy retained

**Perfect for:**
- Mobile devices with limited storage
- Reducing download sizes
- Improving app startup time
- Running multiple models simultaneously

---

## What is Quantization?

### Without Quantization (FP32)

```
Original model weight: 3.14159265359 (32-bit float)
Storage: 4 bytes per weight
```

### With INT8 Quantization

```
Quantized weight: 127 (8-bit integer)
Storage: 1 byte per weight
Reduction: 75%
```

### With FP16 Quantization

```
Quantized weight: 3.141 (16-bit float)
Storage: 2 bytes per weight
Reduction: 50%
```

**The model learns to work with these lower-precision numbers!**

---

## Quick Start

### 1. Install Python Dependencies

```bash
pip install onnxruntime
```

### 2. Download a Model

```bash
# Download multilingual model (470 MB)
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-MULTI.onnx
```

### 3. Quantize It

```bash
# Quantize to INT8 (75% reduction: 470 MB → 117 MB)
python3 scripts/quantize-models.py \
  AVA-ONX-384-MULTI.onnx \
  AVA-ONX-384-MULTI-INT8.onnx \
  int8
```

### 4. Push to Device

```bash
adb push AVA-ONX-384-MULTI-INT8.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/
```

### 5. Use in Code

```kotlin
// No code changes needed!
val provider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-384-MULTI-INT8"  // Use quantized model
)
```

**Done! You just saved 353 MB of storage.**

---

## Quantization Modes

### INT8 - Aggressive (Recommended)

**Size reduction:** 75%
**Quality loss:** ~3-5%
**Speed:** Faster than FP32

```bash
python3 scripts/quantize-models.py model.onnx model-int8.onnx int8
```

**Best for:**
- Maximum size reduction
- Mobile devices
- General use cases

**Example:**
- 470 MB → 117 MB (multilingual model)
- 86 MB → 22 MB (English-only model)

---

### FP16 - Conservative

**Size reduction:** 50%
**Quality loss:** ~1-2%
**Speed:** Similar to FP32

```bash
python3 scripts/quantize-models.py model.onnx model-fp16.onnx fp16
```

**Best for:**
- When you need highest quality
- If INT8 quality is insufficient
- Research applications

**Example:**
- 470 MB → 235 MB (multilingual model)
- 86 MB → 43 MB (English-only model)

---

## Size Reduction Examples

### English-Only Models

| Model | Original | INT8 | FP16 |
|-------|----------|------|------|
| AVA-ONX-384-BASE | 86 MB | 22 MB (-75%) | 43 MB (-50%) |
| AVA-ONX-384-FAST | 61 MB | 15 MB (-75%) | 30 MB (-50%) |
| AVA-ONX-768-QUAL | 420 MB | 105 MB (-75%) | 210 MB (-50%) |

---

### Multilingual Models

| Model | Original | INT8 | FP16 |
|-------|----------|------|------|
| AVA-ONX-384-MULTI (L12) | 470 MB | 117 MB (-75%) | 235 MB (-50%) |
| AVA-ONX-384-MULTI-L6 | 125 MB | 31 MB (-75%) | 62 MB (-50%) |
| AVA-ONX-768-MULTI | 1.1 GB | 275 MB (-75%) | 550 MB (-50%) |
| AVA-ONX-512-MULTI | 540 MB | 135 MB (-75%) | 270 MB (-50%) |

---

### Language-Specific Models

| Model | Original | INT8 | FP16 |
|-------|----------|------|------|
| AVA-ONX-384-ZH (Chinese) | 400 MB | 100 MB (-75%) | 200 MB (-50%) |
| AVA-ONX-768-JA (Japanese) | 450 MB | 112 MB (-75%) | 225 MB (-50%) |

---

## Quality vs Size Trade-offs

### Accuracy Retention

| Quantization | Accuracy Retention | Use Case |
|--------------|-------------------|----------|
| None (FP32) | 100% | Baseline |
| FP16 | 98-99% | High quality needed |
| INT8 | 95-97% | General use (recommended) |

### Real-World Impact

**For document search (RAG):**
- **FP32:** Finds top 5 most relevant documents
- **INT8:** Finds top 5 most relevant documents (same 5, slightly different order)

**Impact:** Minimal - users won't notice the difference in search quality.

---

## Step-by-Step Guide

### Full Workflow Example

#### Step 1: Download Original Model

```bash
# Create working directory
mkdir -p ~/ava-models
cd ~/ava-models

# Download multilingual model
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o paraphrase-multilingual-MiniLM-L12-v2.onnx

# Verify download
ls -lh paraphrase-multilingual-MiniLM-L12-v2.onnx
# Should be: ~470 MB
```

---

#### Step 2: Quantize Model

```bash
# INT8 quantization (recommended)
python3 /path/to/ava/scripts/quantize-models.py \
  paraphrase-multilingual-MiniLM-L12-v2.onnx \
  AVA-ONX-384-MULTI-INT8.onnx \
  int8

# Output:
# ============================================================
# AVA Model Quantization
# ============================================================
# Input:  paraphrase-multilingual-MiniLM-L12-v2.onnx
# Output: AVA-ONX-384-MULTI-INT8.onnx
# Mode:   INT8 (expected ~75% size reduction)
# ============================================================
# Original size: 470.00 MB (492,830,720 bytes)
# Quantizing... This may take a few minutes...
# ============================================================
# ✓ Quantization Complete!
# ============================================================
# Quantized size: 117.50 MB (123,207,680 bytes)
# Size reduction: 75.0%
# Saved:          352.50 MB
# ============================================================
```

---

#### Step 3: Verify Quantized Model

```bash
# Check file size
ls -lh AVA-ONX-384-MULTI-INT8.onnx

# Check it's a valid ONNX file
file AVA-ONX-384-MULTI-INT8.onnx
# Should say: "data" or "ONNX model"
```

---

#### Step 4: Push to Android Device

```bash
# Create models directory
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/

# Push quantized model
adb push AVA-ONX-384-MULTI-INT8.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
# Should show: AVA-ONX-384-MULTI-INT8.onnx  117M
```

---

#### Step 5: Use in Code

```kotlin
// No code changes needed!
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-384-MULTI-INT8"  // Quantized model
)

// Works exactly the same as FP32 model
embeddingProvider.initialize().getOrThrow()

val embedding = embeddingProvider.embed("Test document")
// Returns 384-dimensional vector, just like original model
```

---

## Batch Quantization

### Quantize All Models Script

```bash
#!/bin/bash
# Save as: scripts/quantize-all-models.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_DIR="${1:-./models}"

echo "Quantizing all models in: $MODELS_DIR"
echo ""

# Quantize each .onnx file
for model in "$MODELS_DIR"/*.onnx; do
    if [[ ! -f "$model" ]]; then
        echo "No .onnx files found"
        exit 1
    fi

    filename=$(basename "$model" .onnx)

    # Skip if already quantized
    if [[ "$filename" == *"-INT8" ]] || [[ "$filename" == *"-FP16" ]]; then
        echo "Skipping already quantized: $filename"
        continue
    fi

    echo "Processing: $filename"

    # INT8 quantization
    python3 "$SCRIPT_DIR/quantize-models.py" \
        "$model" \
        "$MODELS_DIR/${filename}-INT8.onnx" \
        int8

    echo ""
done

echo "✓ Batch quantization complete!"
```

**Usage:**

```bash
chmod +x scripts/quantize-all-models.sh

# Download multiple models
mkdir -p models
cd models
curl -L https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx -o all-MiniLM-L6-v2.onnx
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx -o paraphrase-multilingual-MiniLM-L12-v2.onnx

# Quantize all at once
cd ..
./scripts/quantize-all-models.sh models/

# Results:
# models/all-MiniLM-L6-v2-INT8.onnx
# models/paraphrase-multilingual-MiniLM-L12-v2-INT8.onnx
```

---

## Testing Quantized Models

### Test 1: Load Model

```kotlin
@Test
fun testQuantizedModelLoads() = runTest {
    val provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI-INT8"
    )

    val result = provider.initialize()
    assertTrue(result.isSuccess)

    assertEquals(384, provider.dimension)
}
```

---

### Test 2: Generate Embeddings

```kotlin
@Test
fun testQuantizedEmbeddings() = runTest {
    val provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI-INT8"
    )
    provider.initialize().getOrThrow()

    val text = "This is a test document"
    val embedding = provider.embed(text)

    // Check embedding is valid
    assertEquals(384, embedding.size)
    assertTrue(embedding.all { it.isFinite() })

    // Check L2 norm is reasonable
    val norm = sqrt(embedding.map { it * it }.sum())
    assertTrue(norm > 0.5 && norm < 2.0)
}
```

---

### Test 3: Compare FP32 vs INT8 Quality

```kotlin
@Test
fun testQuantizationQuality() = runTest {
    val fp32Provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI"  // Original
    )
    fp32Provider.initialize().getOrThrow()

    val int8Provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI-INT8"  // Quantized
    )
    int8Provider.initialize().getOrThrow()

    val text = "Artificial intelligence is transforming the world"

    val fp32Embedding = fp32Provider.embed(text)
    val int8Embedding = int8Provider.embed(text)

    // Calculate cosine similarity
    val similarity = cosineSimilarity(fp32Embedding, int8Embedding)

    // Should be very similar (> 0.95)
    assertTrue(similarity > 0.95, "Similarity: $similarity")
    println("FP32 vs INT8 similarity: $similarity")
}

fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    val dotProduct = a.zip(b).sumOf { (x, y) -> x * y }
    val normA = sqrt(a.sumOf { it * it })
    val normB = sqrt(b.sumOf { it * it })
    return (dotProduct / (normA * normB)).toFloat()
}
```

---

### Test 4: RAG Search Quality

```kotlin
@Test
fun testRAGWithQuantizedModel() = runTest {
    val provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI-INT8"
    )
    provider.initialize().getOrThrow()

    val vectorStore = InMemoryVectorStore()
    val ragEngine = RAGEngine(
        embeddingProvider = provider,
        vectorStore = vectorStore
    )

    // Add test documents
    val docs = listOf(
        Document("1", "Artificial intelligence transforms industries"),
        Document("2", "Machine learning improves predictions"),
        Document("3", "Deep learning uses neural networks"),
        Document("4", "The weather is sunny today")
    )

    docs.forEach { ragEngine.addDocument(it) }

    // Search
    val results = ragEngine.search("AI and ML", topK = 3)

    // Check top results are relevant (not weather)
    assertFalse(results[0].id == "4")
    assertTrue(results.size == 3)

    println("Search results:")
    results.forEach { println("  ${it.id}: ${it.content}") }
}
```

---

## Troubleshooting

### Issue 1: "ModuleNotFoundError: No module named 'onnxruntime'"

**Fix:**
```bash
pip install onnxruntime
```

Or install in virtual environment:
```bash
python3 -m venv venv
source venv/bin/activate
pip install onnxruntime
python3 scripts/quantize-models.py ...
```

---

### Issue 2: Quantization fails with "Invalid model"

**Problem:** Input file is corrupted or not a valid ONNX model

**Fix:**
```bash
# Verify input file
file model.onnx
# Should say: "data" or similar

# Check file size (should not be very small)
ls -lh model.onnx

# Re-download if corrupted
curl -L <MODEL_URL> -o model.onnx
```

---

### Issue 3: "Permission denied" when running script

**Fix:**
```bash
chmod +x scripts/quantize-models.py
```

Or run with python3 explicitly:
```bash
python3 scripts/quantize-models.py ...
```

---

### Issue 4: Quantized model doesn't load on device

**Problem:** Model file might be corrupted during transfer

**Fix:**
```bash
# Verify file integrity after push
adb pull /sdcard/Android/data/com.augmentalis.ava/files/models/AVA-ONX-384-MULTI-INT8.onnx test.onnx

# Compare file sizes
ls -l AVA-ONX-384-MULTI-INT8.onnx test.onnx
# Should be identical

# Check MD5 hash
md5sum AVA-ONX-384-MULTI-INT8.onnx test.onnx
# Should match
```

---

### Issue 5: Lower quality than expected

**Problem:** INT8 quantization might be too aggressive for your use case

**Fix:** Use FP16 instead
```bash
python3 scripts/quantize-models.py model.onnx model-fp16.onnx fp16
```

---

## Best Practices

### 1. Always Test Before Deploying

```bash
# Quantize
python3 scripts/quantize-models.py model.onnx model-int8.onnx int8

# Test locally first
# Then push to device for testing
# Only deploy to production after validation
```

---

### 2. Keep Original Models

```bash
# Good structure:
models/
├── originals/
│   ├── all-MiniLM-L6-v2.onnx          (original)
│   └── paraphrase-multilingual-MiniLM-L12-v2.onnx
└── quantized/
    ├── AVA-ONX-384-BASE-INT8.onnx     (quantized)
    └── AVA-ONX-384-MULTI-INT8.onnx
```

---

### 3. Document Which Models Are Quantized

```kotlin
// In your model configuration
data class ModelConfig(
    val id: String,
    val path: String,
    val isQuantized: Boolean,
    val quantizationType: String? = null  // "INT8" or "FP16"
)

val models = listOf(
    ModelConfig("AVA-ONX-384-BASE", "AVA-ONX-384-BASE.onnx", false),
    ModelConfig("AVA-ONX-384-BASE-INT8", "AVA-ONX-384-BASE-INT8.onnx", true, "INT8"),
    ModelConfig("AVA-ONX-384-MULTI-INT8", "AVA-ONX-384-MULTI-INT8.onnx", true, "INT8")
)
```

---

### 4. Version Your Models

```bash
# Include version in filename
AVA-ONX-384-MULTI-v1.0-INT8.onnx
AVA-ONX-384-MULTI-v1.1-INT8.onnx

# Or use git tags
git tag -a models-v1.0 -m "Quantized multilingual models v1.0"
```

---

## Recommended Models to Quantize

### Priority 1: Large Models (> 400 MB)

Biggest savings:
- ✅ **AVA-ONX-768-MULTI** (1.1 GB → 275 MB) = 825 MB saved
- ✅ **AVA-ONX-384-MULTI** (470 MB → 117 MB) = 353 MB saved
- ✅ **AVA-ONX-768-QUAL** (420 MB → 105 MB) = 315 MB saved

---

### Priority 2: Commonly Used Models

Most impact on users:
- ✅ **AVA-ONX-384-BASE** (86 MB → 22 MB) = Default English model
- ✅ **AVA-ONX-384-MULTI** (470 MB → 117 MB) = Default multilingual model

---

### Priority 3: Language-Specific (If Used)

- ✅ **AVA-ONX-384-ZH** (400 MB → 100 MB) = Chinese
- ✅ **AVA-ONX-768-JA** (450 MB → 112 MB) = Japanese

---

## Command Reference

```bash
# INT8 quantization (recommended)
python3 scripts/quantize-models.py input.onnx output.onnx int8

# FP16 quantization
python3 scripts/quantize-models.py input.onnx output.onnx fp16

# Get help
python3 scripts/quantize-models.py

# Batch quantize all models
./scripts/quantize-all-models.sh models/

# Verify quantized model
file output.onnx
ls -lh output.onnx
```

---

## Next Steps

After quantizing models:

1. **Test thoroughly** - Use the test suite above
2. **Update documentation** - Note which models are quantized
3. **Update naming registry** - Add quantized model IDs
4. **Deploy to users** - Push quantized models to devices
5. **Monitor quality** - Track user feedback and search accuracy

---

## Additional Resources

### Official Documentation
- **ONNX Runtime Quantization:** https://onnxruntime.ai/docs/performance/model-optimizations/quantization.html
- **Quantization Overview:** https://pytorch.org/docs/stable/quantization.html

### Related Guides
- `docs/MODEL-DOWNLOAD-SOURCES.md` - Download all models
- `docs/AVA-MODEL-NAMING-REGISTRY.md` - Model naming reference
- `docs/MULTILINGUAL-RAG-SETUP.md` - Multilingual setup

---

**Document Version:** 1.0
**Last Updated:** 2025-11-06
**Maintained by:** AVA Development Team
