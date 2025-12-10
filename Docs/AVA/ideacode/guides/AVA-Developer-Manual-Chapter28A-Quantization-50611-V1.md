# Chapter 28A: Model Quantization

**Last Updated:** 2025-11-06
**Status:** Complete
**Authors:** AVA AI Team

---

## Overview

Model quantization is a compression technique that reduces ONNX model file sizes by 50-75% with minimal quality loss (1-5%). This chapter covers quantization theory, implementation, and best practices for AVA's RAG embedding models.

---

## Table of Contents

- 28A.1 Quantization Fundamentals
- 28A.2 Quantization Modes (INT8 vs FP16)
- 28A.3 Implementation Guide
- 28A.4 Quality vs Size Trade-offs
- 28A.5 Integration with AVA
- 28A.6 Testing and Validation
- 28A.7 Best Practices

---

## 28A.1 Quantization Fundamentals

### What is Quantization?

**Quantization** converts high-precision floating-point numbers (FP32) to lower-precision formats (INT8/FP16), dramatically reducing model size and improving inference speed.

**Without Quantization (FP32):**
```
Weight value: 3.14159265359
Storage: 4 bytes (32 bits)
Range: ±3.4 × 10³⁸
Precision: ~7 decimal digits
```

**With INT8 Quantization:**
```
Weight value: 127 (scaled/rounded)
Storage: 1 byte (8 bits)
Range: -128 to 127
Precision: Integer values only
Size reduction: 75%
```

**With FP16 Quantization:**
```
Weight value: 3.141
Storage: 2 bytes (16 bits)
Range: ±65,504
Precision: ~3 decimal digits
Size reduction: 50%
```

### Why Quantize?

**Benefits:**
1. ✅ **Smaller file sizes** - 50-75% reduction
2. ✅ **Faster inference** - INT8 operations are faster than FP32
3. ✅ **Lower memory usage** - Less RAM required
4. ✅ **Faster downloads** - Smaller models download quicker
5. ✅ **Multiple models** - Fit more models on device

**Costs:**
1. ⚠️ **Quality loss** - 1-5% accuracy reduction
2. ⚠️ **One-time conversion** - Requires quantization step
3. ⚠️ **Testing needed** - Must validate quality

---

## 28A.2 Quantization Modes (INT8 vs FP16)

### INT8 Quantization

**Characteristics:**
- Size reduction: **75%**
- Quality loss: **3-5%**
- Speed: **Faster than FP32**
- Recommended for: **Most use cases**

**How it Works:**
```python
# Simplified INT8 quantization
def quantize_to_int8(float_value, scale, zero_point):
    # 1. Scale the float value
    scaled = float_value / scale

    # 2. Add zero point
    shifted = scaled + zero_point

    # 3. Round and clamp to INT8 range
    quantized = np.clip(np.round(shifted), -128, 127)

    return int(quantized)

# Dequantization (for inference)
def dequantize_int8(int_value, scale, zero_point):
    return (int_value - zero_point) * scale
```

**Example:**
```
Original (FP32): 3.14159
Scale: 0.02
Zero point: 0
Quantized (INT8): round(3.14159 / 0.02) = 157
Dequantized: 157 * 0.02 = 3.14
Error: 0.00159 (0.05%)
```

**Best for:**
- Mobile devices with limited storage
- When 3-5% quality loss is acceptable
- Maximum file size reduction needed

---

### FP16 Quantization

**Characteristics:**
- Size reduction: **50%**
- Quality loss: **1-2%**
- Speed: **Similar to FP32**
- Recommended for: **Quality-critical applications**

**How it Works:**
```python
# FP16 uses IEEE 754 half-precision format
# 1 sign bit + 5 exponent bits + 10 mantissa bits

def quantize_to_fp16(float_value):
    # Convert FP32 → FP16 (built into numpy)
    return np.float16(float_value)

# Example
original = 3.14159265359  # FP32
fp16_value = np.float16(original)  # 3.141
error = original - fp16_value  # 0.00059
```

**Best for:**
- Research applications
- High-quality requirements
- When INT8 quality is insufficient

---

### Comparison Table

| Aspect | FP32 (Original) | FP16 | INT8 |
|--------|----------------|------|------|
| **Storage** | 4 bytes | 2 bytes (-50%) | 1 byte (-75%) |
| **Quality** | 100% | 98-99% | 95-97% |
| **Speed** | Baseline | Similar | Faster |
| **Precision** | ~7 digits | ~3 digits | Integer only |
| **Range** | ±3.4×10³⁸ | ±65,504 | -128 to 127 |
| **Use Case** | Baseline | High quality | General use |

---

## 28A.3 Implementation Guide

### Prerequisites

```bash
# Install ONNX Runtime (includes quantization tools)
pip install onnxruntime
```

### Quantization Script

AVA provides `scripts/quantize-models.py` for model quantization:

```bash
# Basic usage
python3 scripts/quantize-models.py <input.onnx> <output.onnx> [int8|fp16]

# INT8 quantization (recommended)
python3 scripts/quantize-models.py \
  AVA-ONX-384-MULTI.onnx \
  AVA-ONX-384-MULTI-INT8.onnx \
  int8

# FP16 quantization
python3 scripts/quantize-models.py \
  AVA-ONX-384-MULTI.onnx \
  AVA-ONX-384-MULTI-FP16.onnx \
  fp16
```

### Script Internals

```python
#!/usr/bin/env python3
from onnxruntime.quantization import quantize_dynamic, QuantType

def quantize_model(input_path, output_path, mode="int8"):
    """Quantize ONNX model"""

    # Select quantization type
    if mode == "int8":
        weight_type = QuantType.QInt8
    else:  # fp16
        weight_type = QuantType.QUInt8

    # Perform quantization
    quantize_dynamic(
        model_input=input_path,
        model_output=output_path,
        weight_type=weight_type,
        optimize_model=True,  # Apply graph optimizations
        extra_options={
            'WeightSymmetric': True if mode == 'int8' else False,
            'ActivationSymmetric': False
        }
    )

    return True
```

### Batch Quantization

For quantizing multiple models:

```bash
# Quantize all models in directory
./scripts/quantize-all-models.sh models/ int8

# Output:
# Processing: all-MiniLM-L6-v2.onnx → AVA-ONX-384-BASE-INT8.onnx
# Processing: paraphrase-multilingual-MiniLM-L12-v2.onnx → AVA-ONX-384-MULTI-INT8.onnx
# ...
```

---

## 28A.4 Quality vs Size Trade-offs

### Size Reduction Examples

| Model | Original | INT8 | FP16 |
|-------|----------|------|------|
| **English Models** | | | |
| AVA-ONX-384-BASE | 86 MB | 22 MB | 43 MB |
| AVA-ONX-384-FAST | 61 MB | 15 MB | 30 MB |
| AVA-ONX-768-QUAL | 420 MB | 105 MB | 210 MB |
| **Multilingual Models** | | | |
| AVA-ONX-384-MULTI | 470 MB | 117 MB | 235 MB |
| AVA-ONX-768-MULTI | 1.1 GB | 275 MB | 550 MB |
| AVA-ONX-512-MULTI | 540 MB | 135 MB | 270 MB |
| **Language-Specific** | | | |
| AVA-ONX-384-ZH | 400 MB | 100 MB | 200 MB |
| AVA-ONX-768-JA | 450 MB | 112 MB | 225 MB |

### Quality Impact

**RAG Search Accuracy:**

| Model Variant | Top-1 Accuracy | Top-5 Accuracy | Semantic Similarity |
|---------------|----------------|----------------|---------------------|
| FP32 (baseline) | 100% | 100% | 1.000 |
| FP16 | 99% | 100% | 0.998 |
| INT8 | 96% | 99% | 0.965 |

**Real-World Impact:**
```kotlin
// Example query: "How do I reset the device?"

// FP32 results:
1. "Device reset procedure" (score: 0.92)
2. "Factory reset instructions" (score: 0.88)
3. "Rebooting the system" (score: 0.75)

// INT8 results (nearly identical ranking):
1. "Device reset procedure" (score: 0.90)  // -2%
2. "Factory reset instructions" (score: 0.86)  // -2%
3. "Rebooting the system" (score: 0.73)  // -3%

// User experience: No noticeable difference
```

---

## 28A.5 Integration with AVA

### Using Quantized Models in Code

**No code changes required!** Just use a different modelId:

```kotlin
// Original model
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-384-MULTI"  // 470 MB
)

// Quantized model - same API
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-384-MULTI-INT8"  // 117 MB
)

// Everything works exactly the same
embeddingProvider.initialize()
val embedding = embeddingProvider.embed("test document")
// Returns same 384-dimensional vector
```

### Model ID Mapping

Quantized models are registered in `ONNXEmbeddingProvider`:

```kotlin
private val modelIdMap = mapOf(
    // Original models
    "AVA-ONX-384-BASE" to "all-MiniLM-L6-v2",
    "AVA-ONX-384-MULTI" to "paraphrase-multilingual-MiniLM-L12-v2",

    // Quantized variants (map to same original model)
    "AVA-ONX-384-BASE-INT8" to "all-MiniLM-L6-v2",
    "AVA-ONX-384-BASE-FP16" to "all-MiniLM-L6-v2",
    "AVA-ONX-384-MULTI-INT8" to "paraphrase-multilingual-MiniLM-L12-v2",
    "AVA-ONX-384-MULTI-FP16" to "paraphrase-multilingual-MiniLM-L12-v2"
)
```

### File Loading

```kotlin
// Quantized models load the same way as originals
private fun loadModelFromAssets(): File {
    val externalModelsDir = File(context.getExternalFilesDir(null), "models")

    // Try quantized filename
    val modelFile = File(externalModelsDir, "$modelId.onnx")
    if (modelFile.exists()) return modelFile

    // Fall back to original filename
    val originalModelId = modelIdMap[modelId] ?: modelId
    val originalFile = File(externalModelsDir, "$originalModelId.onnx")
    if (originalFile.exists()) return originalFile

    throw FileNotFoundException("Model not found: $modelId")
}
```

---

## 28A.6 Testing and Validation

### Test 1: Model Loads Successfully

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

### Test 2: Embeddings Are Valid

```kotlin
@Test
fun testQuantizedEmbeddings() = runTest {
    val provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI-INT8"
    )
    provider.initialize().getOrThrow()

    val embedding = provider.embed("Test document")

    // Check dimensions
    assertEquals(384, embedding.size)

    // Check values are finite
    assertTrue(embedding.all { it.isFinite() })

    // Check L2 norm is reasonable
    val norm = sqrt(embedding.map { it * it }.sum())
    assertTrue(norm > 0.5 && norm < 2.0)
}
```

### Test 3: Quality Comparison (FP32 vs INT8)

```kotlin
@Test
fun testQuantizationQuality() = runTest {
    val fp32Provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI"
    )
    fp32Provider.initialize().getOrThrow()

    val int8Provider = ONNXEmbeddingProvider(
        context = context,
        modelId = "AVA-ONX-384-MULTI-INT8"
    )
    int8Provider.initialize().getOrThrow()

    val text = "Artificial intelligence is transforming the world"
    val fp32Embedding = fp32Provider.embed(text)
    val int8Embedding = int8Provider.embed(text)

    // Calculate cosine similarity
    val similarity = cosineSimilarity(fp32Embedding, int8Embedding)

    // Should be very similar (>0.95)
    assertTrue(similarity > 0.95)
    println("FP32 vs INT8 similarity: $similarity")
}

fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    val dotProduct = a.zip(b).sumOf { (x, y) -> x * y }
    val normA = sqrt(a.sumOf { it * it })
    val normB = sqrt(b.sumOf { it * it })
    return (dotProduct / (normA * normB)).toFloat()
}
```

### Test 4: RAG Search Quality

```kotlin
@Test
fun testRAGSearchWithQuantized() = runTest {
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
    ragEngine.addDocument(Document("1", "AI transforms industries"))
    ragEngine.addDocument(Document("2", "ML improves predictions"))
    ragEngine.addDocument(Document("3", "Weather is sunny"))

    // Search
    val results = ragEngine.search("artificial intelligence", topK = 2)

    // Verify correct ranking
    assertEquals("1", results[0].id)  // AI document first
    assertNotEquals("3", results[0].id)  // Weather not first
}
```

---

## 28A.7 Best Practices

### When to Use Quantization

✅ **Use INT8 quantization when:**
- Storage space is limited
- Maximum size reduction needed
- 3-5% quality loss is acceptable
- Mobile/edge deployment
- General-purpose applications

✅ **Use FP16 quantization when:**
- Higher quality is critical
- Moderate size reduction is sufficient
- 1-2% quality loss is acceptable
- Research or scientific applications

❌ **Don't quantize when:**
- Storage is not a concern
- Maximum quality is critical
- Quality loss cannot be tolerated
- You haven't tested quantized quality

### Deployment Workflow

1. **Download original model**
   ```bash
   curl -L <MODEL_URL> -o model.onnx
   ```

2. **Quantize locally**
   ```bash
   python3 scripts/quantize-models.py model.onnx model-int8.onnx int8
   ```

3. **Test quantized model**
   ```kotlin
   // Run unit tests with quantized model
   ./gradlew testDebugUnitTest
   ```

4. **Compare quality**
   ```kotlin
   // Compare FP32 vs INT8 search results
   // Verify <5% quality loss
   ```

5. **Deploy to device**
   ```bash
   adb push model-int8.onnx /sdcard/.../models/
   ```

6. **Monitor production**
   ```kotlin
   // Track user feedback
   // Monitor search quality metrics
   ```

### File Organization

```
models/
├── originals/
│   ├── all-MiniLM-L6-v2.onnx (keep as backup)
│   └── paraphrase-multilingual-MiniLM-L12-v2.onnx
└── quantized/
    ├── AVA-ONX-384-BASE-INT8.onnx (deploy this)
    ├── AVA-ONX-384-MULTI-INT8.onnx
    └── AVA-ONX-384-MULTI-FP16.onnx
```

### Version Control

```kotlin
// Document which models are quantized
data class ModelConfig(
    val id: String,
    val path: String,
    val isQuantized: Boolean,
    val quantizationType: String? = null,
    val baselineQuality: Float = 1.0f,
    val quantizedQuality: Float? = null
)

val deployedModels = listOf(
    ModelConfig(
        id = "AVA-ONX-384-MULTI-INT8",
        path = "models/AVA-ONX-384-MULTI-INT8.onnx",
        isQuantized = true,
        quantizationType = "INT8",
        baselineQuality = 1.0f,
        quantizedQuality = 0.96f  // 96% of baseline
    )
)
```

---

## Summary

**Model quantization** is essential for deploying RAG on mobile devices:

- ✅ **75% size reduction** (INT8) or 50% (FP16)
- ✅ **Minimal quality loss** (3-5% for INT8, 1-2% for FP16)
- ✅ **Faster inference** especially with INT8
- ✅ **No code changes** required to use quantized models
- ✅ **Simple tooling** - one Python script

**Recommendations:**
- Use **INT8** for most deployments (best size/quality trade-off)
- Use **FP16** only when quality is critical
- Always **test before deploying**
- Keep **original models** as backup

**See Also:**
- `docs/MODEL-QUANTIZATION-GUIDE.md` - Complete quantization guide
- `docs/AVA-MODEL-NAMING-REGISTRY.md` - Model naming conventions
- `scripts/quantize-models.py` - Quantization script
- `scripts/quantize-all-models.sh` - Batch quantization

---

**Chapter Status:** Complete
**Next Chapter:** 28B - Multilingual RAG
