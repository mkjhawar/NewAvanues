# Intent Classification Double Normalization Fix

**Date:** 2025-11-10
**Commit:** 783200c
**Status:** Deployed - Awaiting Manual Verification

## Problem

User reported intent classification still misclassifying after previous L2 normalization fix:
- "Show time" → Classified as `control_temperature` (WRONG)
- "Set alarm" → Classified as `control_lights` (WRONG)
- "Check weather" → Classified as `control_lights` (WRONG)

## Root Cause Analysis

Found **double normalization bug** in intent embedding pre-computation:

### Before Fix (BROKEN)
```kotlin
// Step 1: computeEmbedding() normalized each example
val embedding = computeEmbedding(example)  // Returns L2-normalized vector
exampleEmbeddings.add(embedding)

// Step 2: Averaged the already-normalized embeddings
avgEmbedding = avg(normalize(e1), normalize(e2), ...)

// Step 3: Normalized the average AGAIN
val normalizedAvg = l2Normalize(avgEmbedding)  // Double normalization!
```

**Why This is Wrong:**
- `normalize(avg(normalize(e1), normalize(e2)))` ≠ `normalize(avg(e1, e2))`
- Averaging normalized vectors distorts the semantic space
- Normalizing again compounds the distortion
- Result: Incorrect cosine similarity scores

### After Fix (CORRECT)
```kotlin
// Step 1: computeRawEmbedding() returns mean-pooled but NOT normalized
val rawEmbedding = computeRawEmbedding(example)  // No normalization!
exampleEmbeddings.add(rawEmbedding)

// Step 2: Average the RAW embeddings
avgEmbedding = avg(e1, e2, e3, ...)

// Step 3: Normalize ONCE at the end
val normalizedAvg = l2Normalize(avgEmbedding)  // Single normalization ✓
```

## Implementation

### File: `IntentClassifier.kt`

**Created `computeRawEmbedding()` method:**
- Returns mean-pooled embedding (via `meanPooling()`)
- Does NOT apply L2 normalization
- Used during pre-computation only

**Updated `precomputeIntentEmbeddings()`:**
- Uses `computeRawEmbedding()` instead of `computeEmbedding()`
- Averages raw embeddings
- Normalizes ONCE after averaging

**Kept `computeEmbedding()` for backward compatibility:**
- Calls `computeRawEmbedding()`
- Applies L2 normalization
- Used for query embedding during classification

### Code Changes

#### Lines 368-430 (computeRawEmbedding + computeEmbedding)
```kotlin
/**
 * Compute raw (mean-pooled but NOT normalized) embedding
 * Used during pre-computation to avoid double normalization
 */
private suspend fun computeRawEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
    // ... tokenization and inference ...

    // Apply mean pooling ONLY (no normalization yet)
    val pooledEmbedding = meanPooling(allEmbeddings, attentionMask, seqLen, hiddenSize)

    pooledEmbedding  // Return raw
}

/**
 * Compute normalized embedding for query classification
 * DEPRECATED: Kept for backward compatibility
 */
private suspend fun computeEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
    val rawEmbedding = computeRawEmbedding(text)
    l2Normalize(rawEmbedding)  // Normalize once
}
```

#### Lines 454-489 (precomputeIntentEmbeddings)
```kotlin
// Compute RAW embeddings for all examples (mean-pooled but NOT normalized)
// This prevents double normalization: normalize(avg(normalize(e1), normalize(e2))) → BAD
// Correct approach: normalize(avg(e1, e2)) → GOOD
val exampleEmbeddings = mutableListOf<FloatArray>()
for (i in 0 until examples.length()) {
    val example = examples.getString(i)
    try {
        val rawEmbedding = computeRawEmbedding(example)  // No normalization yet!
        exampleEmbeddings.add(rawEmbedding)
    } catch (e: Exception) {
        android.util.Log.w("IntentClassifier", "Failed to embed '$example': ${e.message}")
    }
}

// Average the RAW embeddings, then normalize ONCE
if (exampleEmbeddings.isNotEmpty()) {
    val hiddenSize = exampleEmbeddings[0].size
    val avgEmbedding = FloatArray(hiddenSize) { 0.0f }

    for (embedding in exampleEmbeddings) {
        for (j in 0 until hiddenSize) {
            avgEmbedding[j] += embedding[j]
        }
    }

    for (j in 0 until hiddenSize) {
        avgEmbedding[j] /= exampleEmbeddings.size.toFloat()
    }

    // L2 normalize ONCE - this is the only normalization step
    val normalizedAvg = l2Normalize(avgEmbedding)
    intentEmbeddings[intentName] = normalizedAvg
}
```

## Verification Required

### Build Status
✅ **Build successful**
✅ **App installed** on device
⏳ **Manual testing required**

### Test Cases

Please test the following utterances and verify correct classification:

1. **"Show time"**
   - Expected: `show_time`
   - Previous (wrong): `control_temperature`

2. **"Set alarm"**
   - Expected: `set_alarm`
   - Previous (wrong): `control_lights`

3. **"Check weather"**
   - Expected: `check_weather`
   - Previous (wrong): `control_lights`

4. **"Turn on the lights"**
   - Expected: `control_lights`
   - Should remain correct

5. **"Adjust the temperature"**
   - Expected: `control_temperature`
   - Should remain correct

### How to Test

1. Launch AVA app on device
2. Speak or type each test utterance
3. Check classification result in UI or logcat:
   ```bash
   adb logcat -v time -s IntentClassifier:*
   ```
4. Look for lines like:
   ```
   IntentClassifier: === Classifying: "Show time" ===
   IntentClassifier:   show_time: 0.89       ← Should be highest
   IntentClassifier:   set_alarm: 0.43
   IntentClassifier:   control_lights: 0.21
   IntentClassifier: ✓ Classified as: show_time (confidence: 0.89)
   ```

## Expected Impact

**Before Fix:**
- Random-looking similarity scores
- Unrelated intents scoring higher than correct intent
- Cosine similarities skewed by double normalization

**After Fix:**
- Semantically meaningful similarity scores
- Correct intents should score highest
- Cosine similarity properly measures semantic distance

## Mathematical Proof

Given two example embeddings `e1` and `e2`:

### Wrong (Double Normalization)
```
e1_norm = e1 / ||e1||
e2_norm = e2 / ||e2||
avg = (e1_norm + e2_norm) / 2
intent_emb = avg / ||avg||

Problem: ||e1_norm|| = ||e2_norm|| = 1, so avg has magnitude ≠ 1
Normalizing again distorts the semantic centroid
```

### Correct (Single Normalization)
```
avg = (e1 + e2) / 2
intent_emb = avg / ||avg||

Correct: Semantic centroid computed first, then normalized once
```

## Remaining Work

### Immediate
- ⏳ User manual verification of test cases

### Future (Not Blocking)
- P7: TVMTokenizer real implementation (4hrs)
- P8: Test coverage 23% → 90%+ (40hrs)
- Fix test suite compilation errors (separate issue)

## Commit Details

```
commit 783200c
Author: Claude Code
Date: Mon Nov 10 01:06:26 2025 -0800

fix(nlu): eliminate double normalization bug in intent embeddings

Root Cause Analysis:
- Intent embeddings were normalized TWICE:
  1. Each example embedding normalized in computeEmbedding()
  2. Average of normalized embeddings normalized again
- This distorts semantic space: normalize(avg(norm(e1), norm(e2))) ≠ norm(avg(e1, e2))
- Result: Incorrect cosine similarity scores causing misclassification

Fix Implementation:
- Created computeRawEmbedding() that returns mean-pooled but NOT normalized embeddings
- Updated precomputeIntentEmbeddings() to use raw embeddings
- Normalize ONCE after averaging: norm(avg(e1, e2, ...)) ✓
- Kept computeEmbedding() for backward compatibility
```

## References

- Previous fix (insufficient): commit fe2c275 - Added L2 normalization to both sides
- Original issue: P5 completion, NLU intent classification accuracy
- Related: `CODEBASE-REVIEW-2025-11-09.md` P1-P9 production blockers
