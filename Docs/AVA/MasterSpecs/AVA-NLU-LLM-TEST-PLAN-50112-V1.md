# AVA AI - NLU & LLM Comprehensive Test Plan

**Version:** 1.0
**Created:** 2025-12-01
**Status:** Ready for Testing

---

## Table of Contents

1. [Test Environment Setup](#1-test-environment-setup)
2. [NLU Test Cases](#2-nlu-test-cases)
3. [LLM Test Cases](#3-llm-test-cases)
4. [Integration Test Cases](#4-integration-test-cases)
5. [Performance Benchmarks](#5-performance-benchmarks)
6. [Edge Cases & Error Handling](#6-edge-cases--error-handling)
7. [Regression Tests](#7-regression-tests)

---

## 1. Test Environment Setup

### Prerequisites

| Requirement | Details |
|-------------|---------|
| Device | RealWear Navigator 500/520 or Android 8.0+ device |
| RAM | Minimum 4GB (8GB recommended) |
| Storage | 2GB free for models |
| Model | AVA-QW3-4B16 or AVA-LL32-3B16 installed |

### Model Installation

```bash
# Copy model to device
adb push AVA-QW3-4B16/ /sdcard/ava-ai-models/llm/AVA-QW3-4B16/

# Verify model files
adb shell ls -la /sdcard/ava-ai-models/llm/AVA-QW3-4B16/
# Expected: tokenizer.json, *.ADco, mlc-chat-config.json
```

### Logcat Filters

```bash
# Full NLU/LLM logging
adb logcat -s "IntentClassifier" "SentenceEncoder" "LocalLLMProvider" "TokenCacheManager" "HuggingFaceTokenizer" "HybridResponseGenerator"

# Compact filter
adb logcat | grep -E "(NLU|LLM|Intent|Token|Cache)"
```

---

## 2. NLU Test Cases

### 2.1 Intent Classification

| ID | Input | Expected Intent | Min Confidence | Priority |
|----|-------|-----------------|----------------|----------|
| NLU-001 | "What time is it?" | `time.check` | 0.70 | P0 |
| NLU-002 | "Set an alarm for 7am" | `alarm.set` | 0.70 | P0 |
| NLU-003 | "What's the weather?" | `check_weather` | 0.70 | P0 |
| NLU-004 | "Open settings" | `settings.open` | 0.70 | P0 |
| NLU-005 | "Play music" | `media.play` | 0.70 | P1 |
| NLU-006 | "Call John" | `communication.call` | 0.65 | P1 |
| NLU-007 | "Send a text to Mom" | `communication.sms` | 0.65 | P1 |
| NLU-008 | "Navigate to home" | `navigation.start` | 0.70 | P1 |
| NLU-009 | "Turn on flashlight" | `device.flashlight` | 0.70 | P1 |
| NLU-010 | "Take a photo" | `camera.capture` | 0.70 | P1 |

### 2.2 Similarity Score Validation

| ID | Test | Input | Expected |
|----|------|-------|----------|
| NLU-020 | Exact match | Training example verbatim | Confidence > 0.95 |
| NLU-021 | Paraphrase | Similar meaning, different words | Confidence 0.75-0.95 |
| NLU-022 | Partial match | Some keywords match | Confidence 0.50-0.75 |
| NLU-023 | No match | Unrelated input | Confidence < 0.50 |
| NLU-024 | Ambiguous | Could be multiple intents | Top intent clearly higher |

### 2.3 Entity Extraction

| ID | Input | Expected Entities |
|----|-------|-------------------|
| NLU-030 | "Set alarm for 7:30 AM" | `time: 07:30`, `period: AM` |
| NLU-031 | "Call John Smith" | `contact: John Smith` |
| NLU-032 | "Navigate to 123 Main St" | `address: 123 Main St` |
| NLU-033 | "Play Bohemian Rhapsody" | `song: Bohemian Rhapsody` |
| NLU-034 | "Weather in Seattle" | `location: Seattle` |

### 2.4 Double Normalization Bug (Regression)

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| NLU-040 | No 0.9999 scores | Send 10 diverse queries | All scores < 0.99 unless exact match |
| NLU-041 | Score distribution | Check score histogram | Normal distribution, not clustered at 0.99 |

---

## 3. LLM Test Cases

### 3.1 Model Loading

| ID | Test | Steps | Expected | Log Check |
|----|------|-------|----------|-----------|
| LLM-001 | Model discovery | Launch app with model installed | Model found | "Auto-discovered model:" |
| LLM-002 | Tokenizer loading | Initialize LLM | HuggingFace tokenizer loads | "Loaded model-specific tokenizer" |
| LLM-003 | Token cache init | Initialize LLM | Cache enabled | "Token cache enabled for model:" |
| LLM-004 | No model fallback | Remove model, launch | Graceful error | "No LLM models found" |

### 3.2 Response Generation

| ID | Input | Expected Behavior | Timeout |
|----|-------|-------------------|---------|
| LLM-010 | "Hello" | Friendly greeting | 5s |
| LLM-011 | "What can you do?" | Lists capabilities | 10s |
| LLM-012 | "Tell me a joke" | Generates joke | 10s |
| LLM-013 | "Explain quantum physics" | Educational response | 30s |
| LLM-014 | "Write a haiku about robots" | Creative 5-7-5 haiku | 15s |

### 3.3 Stop Token Detection

| ID | Test | Input | Expected |
|----|------|-------|----------|
| LLM-020 | Clean ending | Any query | Response ends cleanly, no trailing tokens |
| LLM-021 | No repetition | Long response | No repeated phrases at end |
| LLM-022 | EOS token | Check raw output | Generation stops at EOS |

### 3.4 Streaming Response

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| LLM-030 | Token streaming | Ask question, watch UI | Text appears token-by-token |
| LLM-031 | Backpressure | Ask during slow render | No dropped tokens |
| LLM-032 | Stop mid-stream | Press stop during generation | Clean cancellation |

### 3.5 Token Caching

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| LLM-040 | Cache miss | First request for text | "Cache miss" in logs |
| LLM-041 | Cache hit | Same text again | Faster response, "Cache hit" |
| LLM-042 | Model change invalidates | Switch model, same text | Cache miss (new model) |
| LLM-043 | Cache stats | `tokenCacheManager.getStats()` | Shows hit rate, entry count |

---

## 4. Integration Test Cases

### 4.1 NLU → Action Handler Pipeline

| ID | Input | Expected Flow |
|----|-------|---------------|
| INT-001 | "What time is it?" | NLU → `time.check` → TimeActionHandler → Shows time |
| INT-002 | "Set alarm 7am" | NLU → `alarm.set` → AlarmActionHandler → Alarm set |
| INT-003 | "Open settings" | NLU → `settings.open` → SettingsActionHandler → Settings opens |

### 4.2 NLU → LLM Fallback

| ID | Input | Expected Flow |
|----|-------|---------------|
| INT-010 | "Tell me about Mars" | NLU confidence < threshold → LLM generates response |
| INT-011 | "What's 2+2?" | Low confidence → LLM → "4" |
| INT-012 | "Unknown gibberish xyz" | NLU fails → LLM fallback or template |

### 4.3 Hybrid Response Generator

| ID | Test | Scenario | Expected |
|----|------|----------|----------|
| INT-020 | LLM success | LLM initialized, valid query | LLM response |
| INT-021 | LLM timeout | Slow response (>2s) | Template fallback |
| INT-022 | LLM error | Model corrupted | Template fallback |
| INT-023 | Template only | No LLM model | Always template |

### 4.4 Context Building

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| INT-030 | System prompt | Generate response | System prompt prepended |
| INT-031 | Conversation history | Multi-turn chat | Previous messages in context |
| INT-032 | RAG context | Enable RAG + query | Document chunks in context |

---

## 5. Performance Benchmarks

### 5.1 Latency Targets

| Metric | Target | Acceptable | Unacceptable |
|--------|--------|------------|--------------|
| NLU classification | < 100ms | < 200ms | > 500ms |
| LLM first token | < 2000ms | < 3000ms | > 5000ms |
| LLM tokens/sec | > 10 t/s | > 5 t/s | < 3 t/s |
| Token cache lookup | < 5ms | < 20ms | > 50ms |
| End-to-end response | < 3000ms | < 5000ms | > 10000ms |

### 5.2 Memory Benchmarks

| State | Target | Max |
|-------|--------|-----|
| Idle (no model) | < 100MB | 150MB |
| Model loaded | < 400MB | 512MB |
| During inference | < 500MB | 600MB |
| Peak | < 550MB | 650MB |

### 5.3 Benchmark Script

```kotlin
// Pseudo-code for performance testing
repeat(100) { i ->
    val start = System.currentTimeMillis()
    val result = intentClassifier.classify("Test query $i")
    val elapsed = System.currentTimeMillis() - start
    log("NLU latency: ${elapsed}ms, confidence: ${result.confidence}")
}
```

---

## 6. Edge Cases & Error Handling

### 6.1 Input Edge Cases

| ID | Input | Expected Behavior |
|----|-------|-------------------|
| EDGE-001 | Empty string "" | Graceful handling, no crash |
| EDGE-002 | Very long input (10000 chars) | Truncation, warning |
| EDGE-003 | Special characters "!@#$%^&*()" | Processed without crash |
| EDGE-004 | Unicode emoji "🤖🎉" | Handled gracefully |
| EDGE-005 | Mixed languages "Hello 你好" | Best effort classification |
| EDGE-006 | Numbers only "12345" | Classified or fallback |

### 6.2 State Edge Cases

| ID | Scenario | Expected |
|----|----------|----------|
| EDGE-010 | Query before NLU init | Queued or error message |
| EDGE-011 | Query during model switch | Wait or use old model |
| EDGE-012 | Rapid queries (spam) | Queued, no crash |
| EDGE-013 | Query after cleanup | Re-init or error |

### 6.3 Resource Edge Cases

| ID | Scenario | Expected |
|----|----------|----------|
| EDGE-020 | Low memory warning | Model unloads gracefully |
| EDGE-021 | Storage full | Clear warning, no crash |
| EDGE-022 | Model file corrupted | Fallback to templates |
| EDGE-023 | Database locked | Retry logic, eventual success |

---

## 7. Regression Tests

### 7.1 Fixed Bugs Regression

| ID | Bug | Test | Expected |
|----|-----|------|----------|
| REG-001 | P0-1 Tokenizer vocab | Run 10 queries | Correct tokenization |
| REG-002 | P0-3 Stop tokens | Generate 10 responses | Clean endings |
| REG-003 | Double normalization | Check similarity scores | No 0.9999 clusters |

### 7.2 SQLDelight Migration Regression

| ID | Test | Steps | Expected |
|----|------|-------|----------|
| REG-010 | Message save | Send message | Saved to DB |
| REG-011 | Message load | Restart app | Messages restored |
| REG-012 | Conversation list | View conversations | All conversations shown |
| REG-013 | Token cache persist | Send same query | Cache hit on second |

---

## Test Execution Checklist

### Pre-Test
- [ ] Build debug APK: `./gradlew assembleDebug`
- [ ] Install on test device
- [ ] Copy LLM model to device
- [ ] Start logcat capture
- [ ] Clear app data (fresh start)

### During Test
- [ ] Execute tests by priority (P0 → P1 → P2)
- [ ] Record pass/fail for each case
- [ ] Capture logs for failures
- [ ] Note latency measurements

### Post-Test
- [ ] Save logcat output
- [ ] Document any new bugs found
- [ ] Update test results spreadsheet
- [ ] Create issues for failures

---

**Test Plan Status:** Ready for Execution
**Last Updated:** 2025-12-01
