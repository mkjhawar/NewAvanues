# AVA NLU Module Validation Checklist

**Feature:** NLU-based command disambiguation
**Date:** 2025-11-12
**Status:** Pre-Implementation Validation
**Blocking:** Implementation blocked until AVA NLU validated

---

## Purpose

Before implementing NLU-based command disambiguation in VoiceOS, we must validate that AVA's NLU module meets all technical requirements for this use case.

---

## Critical Requirements

### 1. IntentClassifier API Compatibility

**Question:** Does AVA's IntentClassifier support our use case?

**Required API:**
```kotlin
suspend fun classifyIntent(
    utterance: String,
    candidateIntents: List<String>
): Result<IntentClassification>
```

**What We Need:**
- ✅ Accepts user utterance (voice command)
- ✅ Accepts list of candidate intents (command texts from database)
- ✅ Returns classification with confidence scores for ALL candidates
- ✅ Returns scores as Map<String, Float> (intent → confidence)

**Validation Test:**
```kotlin
val utterance = "Clear history"
val candidates = listOf("Clear", "Clear history", "Clear cache")
val result = intentClassifier.classifyIntent(utterance, candidates)

// Expected result:
// result.data.allScores = {
//   "Clear history": 0.92,
//   "Clear": 0.45,
//   "Clear cache": 0.31
// }
```

**Pass Criteria:**
- [ ] API exists and matches signature
- [ ] Returns scores for all candidates
- [ ] Scores are between 0.0 and 1.0
- [ ] Scores can be converted to percentages (score * 100)

---

### 2. Performance Requirements

**Question:** Can AVA NLU meet VoiceOS performance budgets?

**Requirements:**
- Cold start (model loading): <500ms
- Warm inference (per command): <100ms
- Memory footprint: <100MB total

**Validation Test:**
```kotlin
// Test 1: Cold start
val startTime = System.currentTimeMillis()
val classifier = IntentClassifier.getInstance(context)
classifier.initialize(modelPath)
val loadTime = System.currentTimeMillis() - startTime

// Test 2: Warm inference (run 100 times)
val candidates = (1..20).map { "Command $it" }
repeat(100) {
    val inferenceStart = System.nanoTime()
    classifier.classifyIntent("test command", candidates)
    val inferenceTime = (System.nanoTime() - inferenceStart) / 1_000_000
    // Log inference time
}

// Test 3: Memory usage
val runtime = Runtime.getRuntime()
val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
classifier.initialize(modelPath)
classifier.classifyIntent("test", candidates)
val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
val memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024) // MB
```

**Pass Criteria:**
- [ ] Cold start <500ms
- [ ] Average warm inference <100ms
- [ ] Memory usage <100MB
- [ ] No memory leaks after 1000 inferences

---

### 3. Model Availability

**Question:** Is MobileBERT model available and ready to use?

**Requirements:**
- Model file: mobilebert_int8.onnx
- Size: ~25.5 MB
- Format: ONNX Runtime compatible

**Validation:**
```bash
# Check if model exists in AVA project
find /Volumes/M-Drive/Coding/AVA -name "*mobilebert*.onnx" -o -name "*albert*.onnx"

# Check model size
ls -lh <model_path>

# Verify ONNX Runtime dependency
grep "onnxruntime" /Volumes/M-Drive/Coding/AVA -r
```

**Pass Criteria:**
- [ ] Model file exists in AVA project
- [ ] Model size ≈ 25.5 MB
- [ ] ONNX Runtime 1.17.0+ is configured
- [ ] Model can be loaded successfully

---

### 4. Candidate Intent Ranking

**Question:** Does NLU correctly rank semantic similarity?

**Validation Test Cases:**

**Test 1: Exact semantic match**
```kotlin
utterance = "Clear history"
candidates = ["Clear", "Clear history", "Clear cache", "History settings"]
// Expected: "Clear history" has highest score (>0.8)
```

**Test 2: Partial semantic match**
```kotlin
utterance = "Open settings"
candidates = ["Open", "Settings", "Open menu", "Close"]
// Expected: "Settings" or "Open" has highest score
// "Close" should have low score (opposite meaning)
```

**Test 3: Synonym detection**
```kotlin
utterance = "Delete browsing data"
candidates = ["Clear history", "Remove data", "Delete cache"]
// Expected: All three should have similar high scores (synonyms)
```

**Test 4: Multi-word command**
```kotlin
utterance = "Turn on dark mode"
candidates = ["Dark mode", "Turn on", "Enable dark mode", "Light mode"]
// Expected: "Enable dark mode" > "Dark mode" > "Light mode"
```

**Pass Criteria:**
- [ ] Semantically similar commands ranked higher
- [ ] Exact matches score >0.8
- [ ] Partial matches score 0.5-0.8
- [ ] Opposite meanings score <0.3

---

### 5. Android Integration

**Question:** Can AVA NLU module be imported into VoiceOSCore?

**Requirements:**
- AVA NLU is a KMP module (Kotlin Multiplatform)
- Can be added as Gradle dependency
- No version conflicts with VoiceOS dependencies

**Validation:**
```bash
# Check AVA project structure
ls -la /Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/NLU/

# Check build files
cat /Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/NLU/build.gradle.kts

# Check dependencies
grep -E "(onnxruntime|kotlin|coroutines)" \
  /Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/NLU/build.gradle.kts
```

**Pass Criteria:**
- [ ] NLU module has build.gradle.kts
- [ ] Module is KMP compatible
- [ ] Dependencies compatible with VoiceOS
- [ ] No conflicting versions

---

### 6. Error Handling

**Question:** How does NLU handle edge cases?

**Test Cases:**

**Test 1: Empty candidates list**
```kotlin
utterance = "Clear history"
candidates = emptyList()
// Expected: Returns error or empty result (not crash)
```

**Test 2: Single candidate**
```kotlin
utterance = "Clear history"
candidates = listOf("Clear")
// Expected: Returns score for single candidate
```

**Test 3: Many candidates (100+)**
```kotlin
utterance = "Clear history"
candidates = (1..100).map { "Command $it" }
// Expected: Completes within 100ms, returns top scores
```

**Test 4: Special characters in utterance**
```kotlin
utterance = "Clear history!!!"
candidates = listOf("Clear history", "Clear")
// Expected: Handles gracefully, matches "Clear history"
```

**Pass Criteria:**
- [ ] Empty candidates handled gracefully
- [ ] Single candidate works
- [ ] 100+ candidates complete <100ms
- [ ] Special characters handled

---

## Validation Procedure

### Step 1: Check AVA Project Status

```bash
cd /Volumes/M-Drive/Coding/AVA
git status
git branch
# Verify project is buildable
./gradlew :Universal:AVA:Features:NLU:build
```

### Step 2: Create Test App

Create minimal Android test app that imports AVA NLU:

```kotlin
// VoiceOS/tests/ava-nlu-validation/build.gradle.kts
dependencies {
    implementation(project(":AVA:Universal:AVA:Features:NLU"))
}

// MainActivity.kt
class AVANLUValidationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            runValidationTests()
        }
    }

    private suspend fun runValidationTests() {
        // Run all 6 validation tests above
    }
}
```

### Step 3: Run Validation Tests

1. Build test app
2. Install on device/emulator
3. Run validation tests
4. Capture results
5. Verify all pass criteria met

### Step 4: Document Results

Create `AVA-NLU-VALIDATION-RESULTS.md` with:
- Test results for each requirement
- Performance benchmarks
- Any issues/blockers found
- Go/No-Go decision

---

## Go/No-Go Criteria

**GO (Proceed with implementation):**
- ✅ All 6 critical requirements pass
- ✅ Performance meets budgets
- ✅ No blocking issues

**NO-GO (Do not proceed):**
- ❌ API incompatible
- ❌ Performance unacceptable
- ❌ Model unavailable
- ❌ Cannot integrate into VoiceOS

---

## Alternative Solutions (If NO-GO)

If AVA NLU validation fails:

**Option 1: Fix AVA NLU Issues**
- Work with AVA team to resolve blockers
- May require AVA module updates
- Timeline: 1-2 weeks

**Option 2: Use Different NLU Library**
- TensorFlow Lite with MobileBERT
- Hugging Face Transformers
- Custom LSTM model
- Timeline: 2-3 weeks

**Option 3: Enhanced String Matching**
- Levenshtein distance for fuzzy matching
- TF-IDF for command similarity
- No ML required
- Timeline: 1 week

**Option 4: Defer NLU Integration**
- Keep current real-time element search
- Wait for AVA NLU maturity
- Revisit in 3-6 months

---

## Next Steps

**Immediate Actions:**
1. Read this validation checklist
2. Review AVA NLU implementation
3. Create validation test app
4. Run validation tests
5. Document results
6. Make Go/No-Go decision

**Timeline:**
- Validation testing: 1-2 days
- Decision: After validation complete
- Implementation: Only if GO decision

---

**Created:** 2025-11-12
**Author:** Manoj Jhawar
**Status:** Awaiting Validation
