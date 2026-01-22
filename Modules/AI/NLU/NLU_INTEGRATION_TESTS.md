# NLU Integration Tests

## Overview

This document describes the integration test suite for the AVA AI Natural Language Understanding (NLU) module. The tests validate the end-to-end intent classification pipeline from tokenization through model inference to result handling.

## Test Architecture

### Test Layers

1. **IntentClassifierIntegrationTest**
   - Tests ONNX Runtime integration
   - Validates model inference pipeline
   - Performance benchmarks (< 50ms target)

2. **ClassifyIntentUseCaseIntegrationTest**
   - Tests end-to-end use case flow
   - Validates TrainExampleRepository integration
   - Tests confidence thresholding
   - Validates usage tracking

3. **BertTokenizerIntegrationTest**
   - Tests WordPiece tokenization
   - Validates BERT token format
   - Performance benchmarks (< 5ms target)

## Test Coverage

### IntentClassifier Tests

| Test Case | Purpose | Performance Budget |
|-----------|---------|-------------------|
| `classifyIntent_withValidUtterance` | Basic classification | < 50ms |
| `classifyIntent_performanceWithin50ms_budget` | Validates inference speed | < 50ms |
| `classifyIntent_withEmptyUtterance` | Error handling | N/A |
| `classifyIntent_withEmptyCandidates` | Input validation | N/A |
| `classifyIntent_withMultipleCalls` | Singleton reuse | < 50ms per call |
| `classifyIntent_withLongUtterance` | Truncation handling | < 50ms |
| `classifyIntent_withSpecialCharacters` | Unicode/emoji support | < 50ms |
| `classifyIntent_confidenceThresholdValidation` | Output validation | N/A |

### ClassifyIntentUseCase Tests

| Test Case | Purpose | Validates |
|-----------|---------|-----------|
| `invoke_withNoTrainedExamples` | Empty database handling | `needsTraining=true` |
| `invoke_withTrainedExamples` | End-to-end classification | Repository → Classifier |
| `invoke_withLowConfidence` | Confidence thresholding | Threshold enforcement |
| `invoke_incrementsUsageCount` | Usage tracking | Repository updates |
| `invoke_multipleLocales` | Locale filtering | Correct candidate selection |
| `invoke_withDuplicateIntents` | Intent deduplication | Unique candidates |
| `invoke_endToEndPipeline` | Realistic workflow | Data flow validation |
| `invoke_performanceBenchmark` | Batch performance | < 50ms per classification |

### BertTokenizer Tests

| Test Case | Purpose | Performance Budget |
|-----------|---------|-------------------|
| `tokenize_simpleUtterance` | Basic tokenization | < 5ms |
| `tokenize_emptyString` | Edge case handling | < 5ms |
| `tokenize_longText` | Truncation to 128 tokens | < 5ms |
| `tokenize_specialCharacters` | Unicode/emoji handling | < 5ms |
| `tokenize_attentionMask` | Attention mask correctness | < 5ms |
| `tokenize_tokenTypeIds` | Token type ID format | < 5ms |
| `tokenize_multipleUtterances` | Consistency validation | < 5ms |
| `tokenize_caseVariations` | Lowercasing behavior | < 5ms |
| `tokenize_punctuation` | Punctuation handling | < 5ms |
| `tokenize_numbers` | Number tokenization | < 5ms |
| `tokenize_performance` | Batch performance | < 5ms avg (100 samples) |

## Running the Tests

### Prerequisites

```bash
# Required for integration tests
- Android device/emulator (API 26+)
- ONNX Runtime Mobile AAR (1.17.0)
- MobileBERT model (optional, tests work with mock)
```

### Run All NLU Integration Tests

```bash
./gradlew :features:nlu:connectedAndroidTest
```

### Run Specific Test Class

```bash
./gradlew :features:nlu:connectedAndroidTest \
  --tests "com.augmentalis.ava.features.nlu.IntentClassifierIntegrationTest"
```

### Run Performance Tests Only

```bash
./gradlew :features:nlu:connectedAndroidTest \
  --tests "*performance*"
```

## Test Data Setup

### Training Examples

Tests use programmatically generated training data:

```kotlin
val trainingData = listOf(
    createTrainExample("Turn on the lights", "control_lights"),
    createTrainExample("What's the weather?", "check_weather"),
    createTrainExample("Set alarm for 7am", "set_alarm")
)
```

### In-Memory Database

All tests use Room in-memory database for isolation:

```kotlin
database = Room.inMemoryDatabaseBuilder(
    context,
    AVADatabase::class.java
).build()
```

## Performance Targets

### Overall NLU Pipeline Budget

| Component | Target | Measured |
|-----------|--------|----------|
| Tokenization | < 5ms | TBD |
| Model Inference | < 50ms | TBD |
| Total E2E | < 60ms | TBD |

### Memory Budget

| Component | Target | Measured |
|-----------|--------|----------|
| Model Size | < 15MB | ~12MB (MobileBERT INT8) |
| Vocabulary | < 1MB | ~460KB |
| Runtime Overhead | < 10MB | TBD |

## Known Limitations

### Model Dependency

- Tests require MobileBERT model initialization
- Without model, tests validate interface contracts
- Model download tested separately in `ModelManagerTest`

### Mock vs Real Model

Current tests use mock behavior:
- ✅ Validates interface contracts
- ✅ Tests error handling
- ❌ Cannot validate actual inference quality
- ❌ Cannot measure real performance

### CI/CD Considerations

For continuous integration:
1. **Option A**: Bundle mock ONNX model in assets
2. **Option B**: Skip performance tests in CI, run on device
3. **Option C**: Use model stub with predictable outputs

## Future Enhancements

### Phase 1 (Week 5-6)
- [ ] Add mock ONNX model for deterministic testing
- [ ] Implement confidence calibration tests
- [ ] Add multilingual tokenization tests

### Phase 2 (Week 7-8)
- [ ] Test model quantization accuracy
- [ ] Benchmark NNAPI vs CPU inference
- [ ] Add memory pressure tests

### Phase 3 (Week 9+)
- [ ] Integration with Teach-Ava UI
- [ ] Real user utterance corpus testing
- [ ] A/B testing framework for model versions

## Troubleshooting

### Common Issues

**Issue**: `Model not initialized` errors
- **Cause**: ONNX Runtime not finding model file
- **Fix**: Ensure model downloaded or use mock

**Issue**: Performance tests timeout
- **Cause**: Model inference too slow (CPU fallback)
- **Fix**: Enable NNAPI acceleration

**Issue**: Tokenization produces UNK tokens
- **Cause**: Vocabulary file missing or corrupted
- **Fix**: Re-download vocabulary from Hugging Face

## References

- [ONNX Runtime Mobile Docs](https://onnxruntime.ai/docs/get-started/with-android.html)
- [MobileBERT Paper](https://arxiv.org/abs/2004.02984)
- [BERT Tokenization](https://huggingface.co/docs/transformers/tokenizer_summary)
- [AVA AI Architecture](../../ARCHITECTURE.md)

---

**Last Updated**: 2025-01-XX
**Test Coverage**: 3 test classes, 29 test cases
**Performance Budget**: ✅ Defined, ⏳ Validation Pending
