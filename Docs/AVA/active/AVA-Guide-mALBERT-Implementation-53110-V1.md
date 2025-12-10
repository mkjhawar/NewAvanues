# Guide: mALBERT Implementation (Week 7 Day 3-7)

**Date**: 2025-10-31 11:45 PDT
**Phase**: Week 7 - mALBERT Integration
**Status**: 🔄 In Progress

---

## 🎯 Overview

This guide covers the complete implementation of mALBERT support for AVA's dual NLU strategy:
- **Day 3-4**: Download and convert mALBERT to ONNX INT8
- **Day 5-6**: Implement mALBERTModel and SentencePieceTokenizer
- **Day 7**: Integration, testing, and documentation

---

## 📋 Prerequisites

### Python Environment Setup

```bash
# Create virtual environment
cd /Users/manoj_mbpm14/Coding/ava
python3 -m venv venv
source venv/bin/activate  # On macOS/Linux
# venv\Scripts\activate   # On Windows

# Install dependencies
pip install transformers optimum onnx onnxruntime torch sentencepiece
```

### Required Packages
- `transformers>=4.35.0` - HuggingFace model loading
- `optimum>=1.14.0` - ONNX export utilities
- `onnx>=1.15.0` - ONNX model manipulation
- `onnxruntime>=1.16.0` - ONNX inference and quantization
- `torch>=2.1.0` - PyTorch backend
- `sentencepiece>=0.1.99` - Tokenizer library

---

## 🔧 Day 3-4: Model Conversion

### Step 1: Run Conversion Script

```bash
cd /Users/manoj_mbpm14/Coding/ava

# Make script executable
chmod +x scripts/convert_malbert_to_onnx.py

# Run conversion
python scripts/convert_malbert_to_onnx.py
```

### Expected Output

```
==========================================
mALBERT ONNX Conversion Pipeline
==========================================

STEP 1: Downloading mALBERT from HuggingFace
Model: cservan/malbert-base-cased-128k
✓ Tokenizer saved
✓ Model saved
Total parameters: 117,844,864
Total model size: 82.34 MB

STEP 2: Converting to ONNX Format
✓ ONNX model created: 164.68 MB (FP32)

STEP 3: Quantizing to INT8
Original size: 164.68 MB
✓ Quantized size: 41.23 MB
✓ Size reduction: 75.0%
✓ Target achieved: 41.23 MB <= 45 MB

STEP 4: Validating Model Outputs
Running inference on test sentences...
  1. [68.2ms] This is a test sentence in English.
  2. [71.5ms] Esta es una oración de prueba en español.
  3. [69.8ms] Ceci est une phrase de test en français.
✓ Model validation passed!
✓ Performance target MET

STEP 5: Exporting Tokenizer Vocab
✓ Vocab exported: 128,000 tokens

CONVERSION COMPLETE!
Final model size: 41.23 MB
Target achieved: ✓
```

### Output Files

```
models/malbert/
├── pytorch/                  # Original PyTorch model
│   ├── config.json
│   ├── model.safetensors
│   └── tokenizer files...
├── malbert_fp32.onnx        # ONNX FP32 (164 MB)
├── malbert_int8.onnx        # ONNX INT8 (41 MB) ← Use this
└── tokenizer/               # Tokenizer files for Android
    ├── sentencepiece.model  # SentencePiece model
    ├── tokenizer.json
    └── vocab.txt            # 128K vocab
```

### Troubleshooting

**Issue**: Download fails
- **Solution**: Check internet connection, HuggingFace may be down

**Issue**: Quantization produces model >50 MB
- **Solution**: Try different quantization settings:
  ```python
  quantize_dynamic(
      per_channel=True,    # More aggressive
      reduce_range=True    # Better compatibility
  )
  ```

**Issue**: Validation errors
- **Solution**: Check ONNX Runtime version, try opset_version=13 or 15

---

## 💻 Day 5-6: Kotlin Implementation

### Implementation Files

1. **`mALBERTModel.kt`** - mALBERT NLU model implementation
2. **`SentencePieceTokenizer.kt`** - SentencePiece tokenization
3. **Update `NLUModelFactory.kt`** - Add mALBERT instantiation
4. **Tests** - Unit and integration tests

---

## 📝 Implementation Checklist

### mALBERTModel Implementation
- [x] File created: `mALBERTModel.kt`
- [ ] Implements `INLUModel` interface
- [ ] ONNX Runtime initialization (similar to MobileBERT)
- [ ] Model loading from assets
- [ ] Language validation (52 languages)
- [ ] Error handling
- [ ] Resource cleanup
- [ ] Documentation

### SentencePieceTokenizer Implementation
- [x] File created: `SentencePieceTokenizer.kt`
- [ ] Implements `ITokenizer` interface
- [ ] SentencePiece model loading
- [ ] Tokenization logic
- [ ] Language support (52 languages)
- [ ] Decode functionality
- [ ] Error handling
- [ ] Documentation

### NLUModelFactory Update
- [ ] Update `createModel()` to instantiate mALBERT
- [ ] Remove `NotImplementedError`
- [ ] Add mALBERT initialization
- [ ] Update metadata

### Testing
- [ ] Unit tests for mALBERTModel
- [ ] Unit tests for SentencePieceTokenizer
- [ ] Integration tests (ClassifyIntentUseCase)
- [ ] Multi-language validation tests
- [ ] Performance benchmarks

---

## 🧪 Testing Strategy

### Unit Tests

```kotlin
class mALBERTModelTest {
    @Test
    fun `classify English text returns valid intent`()

    @Test
    fun `classify Spanish text returns valid intent`()

    @Test
    fun `unsupported language returns error`()

    @Test
    fun `model initialization succeeds`()

    @Test
    fun `getSupportedLanguages returns 52 languages`()
}
```

### Integration Tests

```kotlin
class DualNLUIntegrationTest {
    @Test
    fun `lite flavor uses MobileBERT`()

    @Test
    fun `full flavor uses mALBERT`()

    @Test
    fun `ClassifyIntentUseCase selects correct model`()

    @Test
    fun `multilingual classification works end-to-end`()
}
```

### Performance Benchmarks

```kotlin
@Test
fun `mALBERT inference completes in under 80ms`() {
    val model = mALBERTModel(context)
    model.initialize()

    val start = System.currentTimeMillis()
    val result = runBlocking {
        model.classify("test sentence", "en")
    }
    val duration = System.currentTimeMillis() - start

    assertTrue(duration < 80, "Inference took ${duration}ms, expected <80ms")
}
```

---

## 📊 Supported Languages (52 total)

### European Languages (26)
English, Spanish, French, German, Italian, Portuguese, Dutch, Polish, Romanian, Czech, Swedish, Hungarian, Greek, Danish, Finnish, Norwegian, Slovak, Bulgarian, Croatian, Lithuanian, Slovenian, Estonian, Latvian, Irish, Maltese, Luxembourgish

### Asian Languages (15)
Japanese, Chinese (Simplified), Chinese (Traditional), Korean, Thai, Vietnamese, Indonesian, Malay, Hindi, Bengali, Tamil, Telugu, Urdu, Nepali, Sinhala

### Middle Eastern Languages (5)
Arabic, Hebrew, Persian, Turkish, Kurdish

### Other Languages (6)
Russian, Ukrainian, Catalan, Serbian, Albanian, Macedonian

---

## 🎯 Acceptance Criteria

### Functional
- ✅ mALBERT model loads successfully from assets
- ✅ SentencePiece tokenizer works for all 52 languages
- ✅ Classification returns valid intents with confidence scores
- ✅ Unsupported languages return clear error messages
- ✅ Resource cleanup works properly

### Performance
- ✅ Model size: ≤45 MB (target: 41 MB)
- ✅ Inference time: <80ms (CPU, Pixel 6a)
- ✅ Memory usage: <200MB peak
- ✅ Tokenization: <10ms

### Quality
- ✅ Code coverage: ≥80%
- ✅ All unit tests pass
- ✅ Integration tests pass
- ✅ No memory leaks
- ✅ Proper error handling

---

## 🚀 Deployment

### Build Commands

```bash
# Build Lite variant (MobileBERT only)
./gradlew assembleLiteDebug

# Build Full variant (mALBERT)
./gradlew assembleFullDebug

# Run tests for Full variant
./gradlew testFullDebugUnitTest

# Install Full variant on device
./gradlew installFullDebug
```

### Asset Management

**Lite Variant** (AVA Lite):
```
platform/app/src/lite/assets/models/
└── mobilebert_int8.onnx (25 MB)
└── vocab.txt (226 KB)
```

**Full Variant** (AVA Full):
```
platform/app/src/full/assets/models/
└── malbert_int8.onnx (41 MB)
└── sentencepiece.model (4 MB)
└── tokenizer.json (2 MB)
```

---

## 📚 References

### Documentation
- [mALBERT Paper](https://arxiv.org/abs/1909.11942) - ALBERT architecture
- [SentencePiece](https://github.com/google/sentencepiece) - Tokenizer library
- [ONNX Runtime](https://onnxruntime.ai/docs/) - Inference engine
- [HuggingFace Optimum](https://huggingface.co/docs/optimum/) - ONNX export

### Model Info
- **HuggingFace**: cservan/malbert-base-cased-128k
- **Architecture**: ALBERT-base
- **Vocab Size**: 128,000
- **Languages**: 52 (multilingual Wikipedia)
- **Max Sequence**: 128 tokens

---

## ✅ Completion Criteria

### Week 7 Day 3-4 (Complete when):
- [x] Conversion script created
- [ ] mALBERT downloaded successfully
- [ ] ONNX INT8 model ≤45 MB
- [ ] Validation tests pass
- [ ] Tokenizer vocab exported

### Week 7 Day 5-6 (Complete when):
- [ ] mALBERTModel.kt implemented
- [ ] SentencePieceTokenizer.kt implemented
- [ ] NLUModelFactory updated
- [ ] Unit tests pass (≥80% coverage)
- [ ] Integration tests pass

### Week 7 Day 7 (Complete when):
- [ ] End-to-end dual NLU tests pass
- [ ] Performance benchmarks meet targets
- [ ] Documentation updated
- [ ] Ready for Week 8 (UI integration)

---

**Created by**: AVA Team
**Last Updated**: 2025-10-31 11:45 PDT
