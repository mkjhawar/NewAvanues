# LLM Integration Design for AVA
**Created:** 2025-11-10
**Author:** Claude Code (Agent 3)
**Status:** Design Complete, Implementation Blocked by P7

---

## Overview

This document describes how AVA will transition from template-based responses to natural LLM-generated responses using the LocalLLMProvider.

### Current State
- **Response Method:** Template-based (`IntentTemplates.getResponse()`)
- **Characteristics:** Fast (<1ms), deterministic, limited contextual awareness
- **Location:** `ChatViewModel.sendMessage()` line 851-859

### Target State
- **Response Method:** LLM-generated with template fallback
- **Characteristics:** Natural, contextual, ~100-500ms, adaptive
- **Blocking Issue:** P7 (TVMTokenizer) not yet implemented

---

## Architecture

### Component Overview

```
ChatViewModel
    ↓
ResponseGenerator (interface)
    ├── TemplateResponseGenerator (current fallback)
    ├── LLMResponseGenerator (future primary, blocked by P7)
    └── HybridResponseGenerator (recommended)
           ├── Try LLM first
           └── Fall back to template on failure/timeout
```

### Key Classes

#### 1. **ResponseGenerator** (Interface)
- **Location:** `/Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/response/ResponseGenerator.kt`
- **Purpose:** Abstract response generation strategy
- **Methods:**
  - `generateResponse()`: Generate response for user message
  - `isReady()`: Check if generator is operational
  - `getInfo()`: Get metadata about generator

#### 2. **TemplateResponseGenerator**
- **Location:** `/Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/response/TemplateResponseGenerator.kt`
- **Purpose:** Current implementation (templates)
- **Status:** ✅ Complete, production-ready
- **Performance:** <1ms, always reliable

#### 3. **LLMResponseGenerator**
- **Location:** `/Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/response/LLMResponseGenerator.kt`
- **Purpose:** LLM-based natural responses
- **Status:** ⏳ Stub, blocked by P7 (TVMTokenizer)
- **Performance Target:** 100-500ms on-device

#### 4. **HybridResponseGenerator** (Recommended)
- **Location:** `/Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/response/HybridResponseGenerator.kt`
- **Purpose:** Automatic LLM + template fallback
- **Status:** ✅ Complete, ready for testing when P7 is done
- **Behavior:**
  - Try LLM first (timeout: 2s)
  - Fall back to template on failure
  - Track success rate for adaptive switching

#### 5. **LLMContextBuilder**
- **Location:** `/Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/response/LLMContextBuilder.kt`
- **Purpose:** Build mobile-optimized prompts
- **Features:**
  - Standard prompts (50-100 tokens)
  - Low-confidence prompts (teach mode)
  - Intent-specific prompts
  - Conversation history formatting

---

## Integration Flow

### Phase 1: ChatViewModel Integration (Ready for P7)

**Current Code (line 851-859 in ChatViewModel.kt):**
```kotlin
// 4. Generate AVA response template (Task P2T04, P2T06)
val responseContent = if (shouldShowTeachButton(confidenceScore)) {
    // Low confidence: Use unknown template to prompt teaching
    Log.d(TAG, "Low confidence (${confidenceScore}), prompting teach mode")
    IntentTemplates.getResponse(BuiltInIntents.UNKNOWN)
} else {
    // Normal confidence: Use intent-specific template
    IntentTemplates.getResponse(classifiedIntent ?: BuiltInIntents.UNKNOWN)
}
```

**Future Code (when P7 complete):**
```kotlin
// 4. Generate AVA response via LLM or template (adaptive)
val responseContent = try {
    // Use hybrid generator (LLM with template fallback)
    val context = ResponseContext(
        actionResult = null, // Add when action execution implemented
        conversationHistory = buildRecentHistory(),
        metadata = mapOf("time" to System.currentTimeMillis().toString())
    )

    var fullResponse = ""
    responseGenerator.generateResponse(userMessage, classification, context)
        .collect { chunk ->
            when (chunk) {
                is ResponseChunk.Text -> {
                    fullResponse += chunk.content
                    // Optionally: emit chunk for streaming UI
                }
                is ResponseChunk.Complete -> {
                    fullResponse = chunk.fullText
                    Log.i(TAG, "Response generated: ${chunk.metadata}")
                }
                is ResponseChunk.Error -> {
                    Log.e(TAG, "Response generation error: ${chunk.message}")
                    // Already fell back to template in hybrid mode
                }
            }
        }
    fullResponse
} catch (e: Exception) {
    Log.e(TAG, "Response generation failed: ${e.message}")
    // Emergency fallback
    IntentTemplates.getResponse(classifiedIntent ?: BuiltInIntents.UNKNOWN)
}
```

### Phase 2: Initialization (in ChatViewModel.init)

**Add to ChatViewModel:**
```kotlin
private lateinit var responseGenerator: HybridResponseGenerator

init {
    initializeNLU()
    initializeConversation()
    initializeResponseGenerator() // NEW
}

private fun initializeResponseGenerator() {
    viewModelScope.launch {
        try {
            // Create hybrid generator
            val llmProvider = LocalLLMProvider(context)
            responseGenerator = HybridResponseGenerator(context, llmProvider)

            // Try to initialize LLM (optional)
            val modelPath = modelManager.getModelPath()
            if (modelPath.isNotEmpty()) {
                val config = LLMConfig(
                    modelPath = modelPath,
                    device = "opencl",
                    maxMemoryMB = 512
                )

                when (val result = responseGenerator.initialize(config)) {
                    is Result.Success -> {
                        Log.i(TAG, "LLM response generation enabled")
                    }
                    is Result.Error -> {
                        Log.w(TAG, "LLM init failed, using template-only mode: ${result.message}")
                    }
                }
            } else {
                Log.w(TAG, "No LLM model available, using template-only mode")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Response generator init failed, emergency fallback", e)
            // Emergency: Create template-only generator
            responseGenerator = HybridResponseGenerator(context, LocalLLMProvider(context))
        }
    }
}
```

---

## Prompt Design

### Mobile Optimization
- **Token Budget:** 50-100 input, 30-50 output (total <150)
- **Reason:** Mobile LLMs are resource-constrained
- **Strategy:** Concise, focused prompts

### Prompt Templates

#### Standard Prompt (High Confidence)
```
You are AVA, a helpful AI assistant. Respond naturally and concisely.

User: What time is it?
Detected intent: show_time (confidence: 0.87)
Guidance: Tell the time naturally and contextually.

AVA:
```

#### Low Confidence Prompt (Teach Mode)
```
You are AVA, a helpful AI assistant learning from users.

User: Turn on the thingamajig
You're not sure how to interpret this (confidence: 0.42).
Respond helpfully and invite the user to teach you what they meant.

AVA:
```

#### Intent-Specific Prompt
```
You are AVA. User asked: "Set alarm for 7am"
Intent: set_alarm
Context: time=1699564800000
Guidance: Confirm you'll set the alarm, ask for time if not specified.

AVA:
```

---

## Error Handling & Fallback Strategy

### Failure Scenarios

| Scenario | Hybrid Behavior | User Experience |
|----------|----------------|-----------------|
| LLM not initialized | Use template | Fast, reliable response |
| LLM timeout (>2s) | Fall back to template | Fast, reliable response |
| LLM error | Fall back to template | Fast, reliable response |
| Low confidence (<0.6) | Use template | Fast, safe (no hallucination) |
| Both fail | Emergency template | Always works |

### Adaptive Switching
- **Success Rate Tracking:** Monitor LLM success/failure
- **Threshold:** If success rate <50%, disable LLM temporarily
- **Recovery:** Re-enable after metric reset or manual intervention

---

## Performance Targets

### Latency
| Generator | Target | Acceptable | Max |
|-----------|--------|-----------|-----|
| Template | <1ms | <5ms | 10ms |
| LLM (on-device) | 100-300ms | 300-500ms | 1000ms |
| Hybrid (LLM success) | 100-300ms | 300-500ms | 1000ms |
| Hybrid (fallback) | <1ms | <5ms | 10ms |

### Token Usage
- **Input:** 50-100 tokens per request
- **Output:** 30-50 tokens per response
- **Total:** <150 tokens per interaction

### Success Rate
- **Target:** >90% LLM success rate
- **Acceptable:** >70% LLM success rate
- **Action:** If <50%, disable LLM and investigate

---

## Testing Plan

### Unit Tests
1. **LLMContextBuilder**
   - ✓ Prompt generation for all intent types
   - ✓ Token estimation accuracy
   - ✓ Context formatting

2. **TemplateResponseGenerator**
   - ✓ Template lookup for all intents
   - ✓ Unknown intent fallback
   - ✓ Streaming interface consistency

3. **LLMResponseGenerator** (blocked by P7)
   - ⏳ Response generation
   - ⏳ Timeout handling
   - ⏳ Error propagation

4. **HybridResponseGenerator**
   - ⏳ Fallback logic (blocked by P7)
   - ⏳ Success rate tracking
   - ⏳ Adaptive switching

### Integration Tests
1. **ChatViewModel Integration**
   - ⏳ Response generation in sendMessage()
   - ⏳ Streaming UI updates
   - ⏳ Error handling

2. **End-to-End Tests**
   - ⏳ User sends message → LLM response displayed
   - ⏳ LLM failure → template fallback → response displayed
   - ⏳ Low confidence → teach mode prompt

---

## P7 Completion Checklist

When P7 (TVMTokenizer) is complete, follow these steps:

### 1. Verify P7 Readiness
- [ ] `LocalLLMProvider.initialize()` succeeds with real model
- [ ] `LocalLLMProvider.generateResponse()` returns streaming responses
- [ ] Token generation is stable (<500ms per response)

### 2. Update LLMResponseGenerator
- [ ] Uncomment blocked implementation (line 87-148)
- [ ] Test with real model inference
- [ ] Tune MAX_RESPONSE_TOKENS based on performance

### 3. Integrate with ChatViewModel
- [ ] Add `responseGenerator` field
- [ ] Initialize in `init` block
- [ ] Replace template code in `sendMessage()` with hybrid generator
- [ ] Add response streaming to UI (optional)

### 4. Test Integration
- [ ] Send message → LLM generates natural response
- [ ] Cause LLM failure → template fallback works
- [ ] Low confidence message → teach mode prompt
- [ ] Check metrics → LLM success rate >70%

### 5. UI Enhancements (Optional)
- [ ] Add streaming typewriter effect
- [ ] Show "AVA is thinking..." indicator during LLM inference
- [ ] Add generator indicator (LLM vs template) in dev mode

---

## Monitoring & Metrics

### Key Metrics to Track
1. **LLM Success Rate:** `llmSuccessCount / (llmSuccessCount + llmFailureCount)`
2. **LLM Usage Rate:** `llmSuccessCount / totalRequests`
3. **Average Latency:** Track separately for LLM vs template
4. **Template Fallback Rate:** How often fallback is used

### Logging
```kotlin
// In HybridResponseGenerator.generateResponse()
Log.i(TAG, "=== Response Generation Metrics ===")
Log.i(TAG, "  Generator: ${if (useLLM) "LLM" else "Template"}")
Log.i(TAG, "  Latency: ${latency}ms")
Log.i(TAG, "  LLM Success Rate: ${metrics.llmSuccessRate * 100}%")
Log.i(TAG, "  LLM Usage Rate: ${metrics.llmUsageRate * 100}%")
```

---

## Future Enhancements

### Short Term (Post-P7)
1. **Action Result Integration:** Include action execution results in prompts
2. **Conversation History:** Add last 3-5 message pairs for context
3. **Streaming UI:** Real-time typewriter effect for LLM responses

### Medium Term
1. **Model Hot-Swapping:** Switch models based on task complexity
2. **Personalization:** User-specific prompt customization
3. **Multi-Language:** Language-aware prompt templates

### Long Term
1. **Cloud Fallback:** Route complex queries to cloud LLM
2. **Fine-Tuning:** Train custom model for AVA-specific responses
3. **Tool Calling:** Integrate function calling for action execution

---

## Summary

### What's Complete ✅
- ResponseGenerator interface and implementations
- LLMContextBuilder with mobile-optimized prompts
- TemplateResponseGenerator (production-ready)
- HybridResponseGenerator (ready for P7)
- Integration design document

### What's Blocked ⏳
- LLMResponseGenerator implementation (needs P7)
- ChatViewModel integration (needs P7)
- End-to-end testing (needs P7)

### When P7 is Complete
1. Uncomment LLMResponseGenerator implementation
2. Add responseGenerator to ChatViewModel
3. Replace template code in sendMessage()
4. Test and tune performance
5. Deploy to production

### Recommended Approach
Use **HybridResponseGenerator** for production:
- Automatic fallback ensures reliability
- Gradual rollout via success rate monitoring
- Zero user-visible failures
- Best user experience (natural when possible, fast always)

---

**End of Design Document**
