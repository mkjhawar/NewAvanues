# LLM Integration Quick Reference
**For Developers - TL;DR Version**

---

## What Was Built

5 new classes for natural language response generation:
1. **ResponseGenerator** - Strategy interface
2. **TemplateResponseGenerator** - Current (templates)
3. **LLMResponseGenerator** - Future (natural LLM, blocked by P7)
4. **HybridResponseGenerator** - Recommended (LLM + template fallback)
5. **LLMContextBuilder** - Prompt builder

---

## Usage (When P7 Complete)

### Quick Start

```kotlin
// In ChatViewModel
class ChatViewModel(...) {
    private lateinit var responseGenerator: HybridResponseGenerator

    init {
        // Initialize generator
        val llmProvider = LocalLLMProvider(context)
        responseGenerator = HybridResponseGenerator(context, llmProvider)

        // Try to enable LLM (optional)
        viewModelScope.launch {
            val config = LLMConfig(
                modelPath = modelManager.getModelPath(),
                device = "opencl"
            )
            responseGenerator.initialize(config)
        }
    }

    fun sendMessage(text: String) {
        // ... NLU classification ...

        // Generate response
        val context = ResponseContext()
        var response = ""

        responseGenerator.generateResponse(text, classification, context)
            .collect { chunk ->
                when (chunk) {
                    is ResponseChunk.Text -> response += chunk.content
                    is ResponseChunk.Complete -> response = chunk.fullText
                    is ResponseChunk.Error -> Log.e(TAG, chunk.message)
                }
            }

        // Save to database...
    }
}
```

---

## Architecture Diagram

```
User Message
    ↓
ChatViewModel.sendMessage()
    ↓
NLU Classification (IntentClassifier)
    ↓
HybridResponseGenerator.generateResponse()
    ├─→ Try LLM (timeout: 2s)
    │   ├─→ LLMContextBuilder.buildPrompt()
    │   ├─→ LocalLLMProvider.generateResponse()
    │   └─→ Stream response chunks
    │
    └─→ Fallback to Template (on failure/timeout)
        ├─→ IntentTemplates.getResponse()
        └─→ Return instantly
    ↓
Response displayed to user
```

---

## File Locations

```
/Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/

response/
├── ResponseGenerator.kt              [3.5 KB] Interface
├── TemplateResponseGenerator.kt      [3.3 KB] Templates (current)
├── LLMResponseGenerator.kt           [11 KB]  LLM (blocked by P7)
├── HybridResponseGenerator.kt        [9.8 KB] Hybrid (recommended)
└── LLMContextBuilder.kt              [6.2 KB] Prompt builder

Documentation:
├── LLM_INTEGRATION_DESIGN.md         [13 KB]  Full design doc
├── INTEGRATION_SUMMARY.md            [11 KB]  Detailed summary
└── QUICK_REFERENCE.md                [This file]
```

---

## Response Types

### Template Response (Current)
```kotlin
User: "What time is it?"
AVA: "Here's the current time." // Instant, deterministic
```

### LLM Response (Future)
```kotlin
User: "What time is it?"
AVA: "It's 3:45 PM - afternoon is flying by!" // Natural, ~300ms
```

### Hybrid Response (Recommended)
```kotlin
User: "What time is it?"
// Try LLM first
// If success: Natural response (~300ms)
// If fail/timeout: Template fallback (<1ms)
```

---

## Key Classes

### ResponseGenerator (Interface)
```kotlin
interface ResponseGenerator {
    suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext
    ): Flow<ResponseChunk>

    fun isReady(): Boolean
    fun getInfo(): GeneratorInfo
}
```

### TemplateResponseGenerator
- Uses `IntentTemplates.getResponse()`
- Always ready, always works
- <1ms latency
- Production-ready

### LLMResponseGenerator
- Uses `LocalLLMProvider.generateResponse()`
- Blocked by P7 (TVMTokenizer)
- 100-500ms latency
- Natural, contextual responses

### HybridResponseGenerator
- Tries LLM first, falls back to template
- 2-second timeout
- Tracks success rate
- Adaptive switching
- **Recommended for production**

### LLMContextBuilder
- `buildPrompt()` - Standard prompt
- `buildLowConfidencePrompt()` - Teach mode
- `buildIntentPrompt()` - Intent-specific
- Token budget: 50-100 input, 30-50 output

---

## Response Flow

```kotlin
// 1. Build context
val context = ResponseContext(
    actionResult = ActionResult(success = true, message = "Lights turned on"),
    conversationHistory = listOf(
        "Turn on lights" to "I'll control the lights for you.",
        "Thanks!" to "You're welcome!"
    ),
    metadata = mapOf("time" to "1699564800000")
)

// 2. Generate response
var fullResponse = ""
generator.generateResponse(userMessage, classification, context)
    .collect { chunk ->
        when (chunk) {
            is ResponseChunk.Text -> {
                fullResponse += chunk.content
                // Update UI progressively (typewriter effect)
            }
            is ResponseChunk.Complete -> {
                fullResponse = chunk.fullText
                Log.i(TAG, "Latency: ${chunk.metadata["latency_ms"]}ms")
            }
            is ResponseChunk.Error -> {
                Log.e(TAG, "Error: ${chunk.message}")
                // Already fell back to template in hybrid mode
            }
        }
    }
```

---

## Prompt Examples

### Standard Prompt
```
You are AVA, a helpful AI assistant. Respond naturally and concisely.

User: What time is it?
Detected intent: show_time (confidence: 0.87)

AVA:
```

### Low Confidence (Teach Mode)
```
You are AVA, a helpful AI assistant learning from users.

User: Turn on the thingamajig
You're not sure how to interpret this (confidence: 0.42).
Respond helpfully and invite the user to teach you what they meant.

AVA:
```

### Intent-Specific
```
You are AVA. User asked: "Set alarm for 7am"
Intent: set_alarm
Guidance: Confirm you'll set the alarm, ask for time if not specified.

AVA:
```

---

## Error Handling

### Hybrid Generator Fallback Logic
```kotlin
if (llmReady && confidence > 0.6) {
    try {
        withTimeout(2000) {
            llmGenerator.generateResponse(...)
        }
    } catch (e: Exception) {
        // Fall back to template
        templateGenerator.generateResponse(...)
    }
} else {
    // Use template directly
    templateGenerator.generateResponse(...)
}
```

### Failure Scenarios
| Scenario | Hybrid Behavior |
|----------|----------------|
| LLM not initialized | Use template |
| LLM timeout (>2s) | Fall back to template |
| LLM error | Fall back to template |
| Low confidence (<0.6) | Use template |
| Both fail | Emergency template |

---

## Performance Targets

| Metric | Template | LLM | Hybrid (LLM) | Hybrid (Fallback) |
|--------|----------|-----|--------------|-------------------|
| Latency | <1ms | 100-500ms | 100-500ms | <1ms |
| Success Rate | 100% | 70-90% | 100% | 100% |
| Token Budget | N/A | <150 | <150 | N/A |

---

## Testing Checklist

### Unit Tests
- [ ] TemplateResponseGenerator returns correct templates
- [ ] LLMContextBuilder builds valid prompts
- [ ] HybridResponseGenerator falls back correctly

### Integration Tests
- [ ] ChatViewModel generates responses
- [ ] Streaming UI updates work
- [ ] Error handling doesn't crash

### End-to-End Tests
- [ ] User sends message → LLM response appears
- [ ] LLM failure → template fallback works
- [ ] Low confidence → teach mode prompt

---

## P7 Completion Steps

1. **Verify P7 works:**
   ```kotlin
   LocalLLMProvider.initialize(config) // Success
   LocalLLMProvider.generateResponse(prompt, options) // Streams tokens
   ```

2. **Uncomment LLMResponseGenerator:**
   - File: `LLMResponseGenerator.kt`
   - Lines: 87-148
   - Remove: `emit(ResponseChunk.Error(...))` stub

3. **Integrate with ChatViewModel:**
   - Add: `private lateinit var responseGenerator: HybridResponseGenerator`
   - Init: Call `initializeResponseGenerator()` in `init` block
   - Replace: Template code in `sendMessage()` (line 851-859)

4. **Test & Monitor:**
   - Send messages, verify natural responses
   - Check metrics: `generator.getInfo().metadata`
   - Watch success rate: Should be >70%

---

## Monitoring

### Key Metrics
```kotlin
val info = responseGenerator.getInfo()
val metrics = responseGenerator.getMetrics()

Log.i(TAG, "LLM Success Rate: ${metrics.llmSuccessRate * 100}%")
Log.i(TAG, "LLM Usage Rate: ${metrics.llmUsageRate * 100}%")
Log.i(TAG, "Template Fallbacks: ${metrics.templateFallbackCount}")
```

### Expected Values
- **LLM Success Rate:** >70% (target >90%)
- **LLM Usage Rate:** 50-80% (depends on confidence distribution)
- **Average Latency:** 100-300ms (LLM), <1ms (template)

---

## Common Issues

### Issue: LLM not generating responses
**Fix:** Check P7 status, verify TVMTokenizer is implemented

### Issue: Responses always templates
**Fix:** Check `generator.getInfo().metadata["llm_ready"]`, verify initialization

### Issue: High timeout rate
**Fix:** Increase timeout or optimize model/device settings

### Issue: Low success rate (<50%)
**Fix:** Check model health, verify prompts are well-formed

---

## Resources

- **Full Design:** `LLM_INTEGRATION_DESIGN.md`
- **Detailed Summary:** `INTEGRATION_SUMMARY.md`
- **Code:** `/response/` directory
- **P7 Status:** Check with P7 team

---

## Quick Commands

```bash
# View created files
ls -lh /Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/response/

# Read design doc
cat /Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/LLM_INTEGRATION_DESIGN.md

# Check ChatViewModel integration point
# Line 851-859 in ChatViewModel.kt
```

---

**Agent 3 Complete - Ready for P7 Integration**
