# ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture

**Status**: Accepted
**Date**: 2025-12-04
**Authors**: Manoj Jhawar
**Deciders**: Architecture Team
**Platform**: Android (with cross-platform considerations)
**Related**: ADR-003 (ONNX NLU), ADR-008 (Hardware-Aware Inference)

---

## Context

AVA AI uses a dual-system architecture:
1. **NLU (Intent Classifier)**: Fast, low-power BERT embeddings for 338 pre-trained intents
2. **LLM (Gemma 2B)**: Flexible, conversational AI for complex queries

**Current Problem**: When NLU confidence is low (<0.5), the system shows a "Teach AVA" dialog, requiring manual user intervention. This creates friction and exposes implementation details to users.

**Industry Research Findings**:
| Source | Finding |
|--------|---------|
| [Taylor & Francis 2024](https://www.tandfonline.com/doi/full/10.1080/08839514.2024.2414483) | LLM-generated training data improves intent classifier accuracy significantly |
| [IntentGPT (arXiv)](https://arxiv.org/html/2411.10670v1) | LLMs can discover novel intents with few-shot learning |
| [Rasa LLM Intent](https://rasa.com/docs/rasa/next/llms/llm-intent/) | Production-ready LLM intent classification |
| [Home Assistant 2024.12](https://rc.home-assistant.io/blog/2024/12/04/release-202412/) | Fast NLU for common commands, automatic LLM fallback |

**Goal**: Create a seamless, self-improving system where:
1. User never sees NLU vs LLM distinction
2. LLM automatically teaches NLU new intents
3. System gets smarter with each interaction
4. Battery and CPU usage are optimized

---

## Decision

**We will implement an LLM-as-Teacher architecture where the LLM automatically classifies intents for unknown queries and teaches the NLU in real-time.**

### System Architecture

```
User Input: "Play some smooth jazz"
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                    INTENT ROUTER                            │
│                                                             │
│  NLU Classifier (338 embeddings)                           │
│  Confidence: 0.42 (LOW)                                    │
│                                                             │
│  Decision: confidence < 0.75 → LLM Fallback                │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                    INFERENCE MANAGER                        │
│                                                             │
│  Battery: 65% │ Temp: 38°C │ Charging: No                  │
│                                                             │
│  Decision: Use LOCAL LLM (battery OK, not thermal limited) │
│  Fallback: Cloud LLM if local unavailable/overloaded       │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                    LLM TEACHER                              │
│                                                             │
│  Input: "Play some smooth jazz"                            │
│                                                             │
│  Output:                                                    │
│  [RESPONSE] Playing smooth jazz playlist now...            │
│  [INTENT] play_music                                       │
│  [VARIATIONS]                                              │
│  - play jazz music                                          │
│  - put on some jazz                                         │
│  - play smooth jazz                                         │
│  [CONFIDENCE] 0.95                                         │
└─────────────────────────────────────────────────────────────┘
          │
          ├──────────────────────────────────────────┐
          ▼                                          ▼
┌────────────────────┐              ┌───────────────────────────┐
│   USER RESPONSE    │              │   BACKGROUND LEARNING     │
│                    │              │                           │
│   "Playing smooth  │              │   1. Save to TrainExample │
│    jazz playlist   │              │   2. Compute embedding    │
│    now..."         │              │   3. Update NLU cache     │
└────────────────────┘              └───────────────────────────┘
                                              │
                                              ▼
                               ┌───────────────────────────────┐
                               │   NEXT TIME: NLU RECOGNIZES   │
                               │                               │
                               │   "play smooth jazz"          │
                               │   → play_music @ 0.92 conf    │
                               │   → FAST LOCAL EXECUTION      │
                               └───────────────────────────────┘
```

### Confidence Threshold Matrix

| Confidence | Action | User Experience |
|------------|--------|-----------------|
| ≥ 0.75 | NLU executes directly | Instant response (<50ms) |
| 0.40 - 0.74 | LLM fallback + auto-teach | Natural response (1-3s) |
| < 0.40 | LLM with clarification | "Could you clarify?" + teach |

### Battery-Aware Inference Switching

| Condition | Primary | Fallback | Rationale |
|-----------|---------|----------|-----------|
| Battery > 30%, Temp < 45°C | Local LLM | Cloud LLM | Preserve battery life |
| Battery < 30% | Cloud LLM | Local LLM | Critical battery mode |
| Temp > 45°C (thermal) | Cloud LLM | Queue + Wait | Prevent throttling |
| Charging | Local LLM | Local LLM | No battery concern |
| No network | Local LLM | Local LLM | Offline mode |
| Cloud not configured | Local LLM | Local LLM | No cloud option |

---

## Implementation

### 1. LLM Teacher System Prompt

```kotlin
object LLMTeacherPrompt {
    const val SYSTEM_PROMPT = """
You are AVA, a helpful AI assistant. When responding to the user, you MUST:

1. RESPOND naturally and helpfully to the user's request
2. CLASSIFY the intent from the categories below
3. GENERATE 3-5 natural variations of the user's phrase
4. RATE your confidence in the classification (0.0-1.0)

INTENT CATEGORIES:
- play_music, pause_music, skip_track, volume_control
- control_lights, control_temperature, control_device
- check_weather, show_time, search_web, get_directions
- set_alarm, set_reminder, set_timer
- add_calendar_event, check_schedule
- make_call, send_message
- perform_calculation, create_note, add_to_list
- open_app, close_app
- general_question (for anything not fitting above)

FORMAT YOUR RESPONSE EXACTLY AS:
[RESPONSE]
Your natural response here...

[INTENT]
intent_name

[VARIATIONS]
- variation 1
- variation 2
- variation 3

[CONFIDENCE]
0.XX
"""
}
```

### 2. LLM Response Parser

```kotlin
data class LLMTeacherResult(
    val response: String,
    val intent: String,
    val variations: List<String>,
    val confidence: Float
)

object LLMResponseParser {
    private val RESPONSE_REGEX = """\[RESPONSE\](.*?)\[INTENT\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val INTENT_REGEX = """\[INTENT\]\s*(\w+)""".toRegex()
    private val VARIATIONS_REGEX = """\[VARIATIONS\](.*?)\[CONFIDENCE\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val CONFIDENCE_REGEX = """\[CONFIDENCE\]\s*([\d.]+)""".toRegex()

    fun parse(llmOutput: String): LLMTeacherResult? {
        val response = RESPONSE_REGEX.find(llmOutput)?.groupValues?.get(1)?.trim() ?: return null
        val intent = INTENT_REGEX.find(llmOutput)?.groupValues?.get(1)?.trim() ?: "general_question"
        val variationsBlock = VARIATIONS_REGEX.find(llmOutput)?.groupValues?.get(1) ?: ""
        val variations = variationsBlock.lines()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
        val confidence = CONFIDENCE_REGEX.find(llmOutput)?.groupValues?.get(1)?.toFloatOrNull() ?: 0.8f

        return LLMTeacherResult(response, intent, variations, confidence)
    }
}
```

### 3. Inference Manager (Battery-Aware)

```kotlin
@Singleton
class InferenceManager @Inject constructor(
    private val context: Context,
    private val localLLMProvider: LocalLLMProvider,
    private val cloudLLMProvider: CloudLLMProvider?,
    private val batteryManager: BatteryManager,
    private val thermalManager: ThermalManager?
) {
    enum class InferenceBackend {
        LOCAL_LLM,
        CLOUD_LLM,
        QUEUED  // Wait for better conditions
    }

    data class DeviceState(
        val batteryPercent: Int,
        val isCharging: Boolean,
        val temperatureCelsius: Float,
        val hasNetwork: Boolean,
        val hasCloudConfig: Boolean
    )

    fun selectBackend(): InferenceBackend {
        val state = getDeviceState()

        return when {
            // Thermal throttling - use cloud or queue
            state.temperatureCelsius > 45f -> {
                if (state.hasNetwork && state.hasCloudConfig) InferenceBackend.CLOUD_LLM
                else InferenceBackend.QUEUED
            }

            // Low battery - prefer cloud
            state.batteryPercent < 30 && !state.isCharging -> {
                if (state.hasNetwork && state.hasCloudConfig) InferenceBackend.CLOUD_LLM
                else InferenceBackend.LOCAL_LLM
            }

            // Normal conditions - prefer local
            else -> InferenceBackend.LOCAL_LLM
        }
    }

    private fun getDeviceState(): DeviceState {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = (level * 100 / scale)

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        // Thermal status (API 29+)
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            thermalManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }
        val tempCelsius = when (thermalStatus) {
            PowerManager.THERMAL_STATUS_SEVERE,
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> 50f
            PowerManager.THERMAL_STATUS_MODERATE -> 42f
            else -> 35f
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val hasNetwork = cm.activeNetwork != null

        return DeviceState(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            temperatureCelsius = tempCelsius,
            hasNetwork = hasNetwork,
            hasCloudConfig = cloudLLMProvider != null
        )
    }
}
```

### 4. NLU Self-Learning

```kotlin
@Singleton
class NLUSelfLearner @Inject constructor(
    private val trainExampleRepository: TrainExampleRepository,
    private val intentClassifier: IntentClassifier,
    private val workManager: WorkManager
) {
    /**
     * Learn from LLM classification result
     * Called automatically when LLM provides intent classification
     */
    suspend fun learnFromLLM(
        utterance: String,
        result: LLMTeacherResult
    ) {
        // 1. Save primary utterance
        trainExampleRepository.insert(
            utterance = utterance,
            intent = result.intent,
            source = "llm_auto",
            confidence = result.confidence
        )

        // 2. Save variations (if confidence high enough)
        if (result.confidence >= 0.85f) {
            result.variations.forEach { variation ->
                trainExampleRepository.insert(
                    utterance = variation,
                    intent = result.intent,
                    source = "llm_variation",
                    confidence = result.confidence * 0.9f // Slightly lower for generated
                )
            }
        }

        // 3. Schedule background embedding computation
        scheduleEmbeddingComputation(utterance, result.intent)
    }

    private fun scheduleEmbeddingComputation(utterance: String, intent: String) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<EmbeddingComputeWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "utterance" to utterance,
                "intent" to intent
            ))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "embedding_$intent",
            ExistingWorkPolicy.APPEND,
            workRequest
        )
    }
}
```

### 5. Background Embedding Worker

```kotlin
class EmbeddingComputeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val utterance = inputData.getString("utterance") ?: return Result.failure()
        val intent = inputData.getString("intent") ?: return Result.failure()

        return try {
            // Get IntentClassifier instance
            val classifier = EntryPointAccessors.fromApplication(
                applicationContext,
                IntentClassifierEntryPoint::class.java
            ).intentClassifier()

            // Compute embedding
            val embedding = classifier.computeEmbedding(utterance)

            // Save to database
            classifier.saveIntentEmbedding(intent, utterance, embedding)

            Timber.i("Computed embedding for '$utterance' → $intent")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to compute embedding")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
```

### 6. Cloud LLM Fallback Provider

```kotlin
interface CloudLLMProvider {
    suspend fun generateWithTeaching(
        prompt: String,
        systemPrompt: String
    ): Flow<String>

    fun isConfigured(): Boolean
}

/**
 * OpenAI-compatible cloud LLM provider
 * Supports: OpenAI, Claude, Groq, Together, etc.
 */
class OpenAICompatibleProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val model: String = "gpt-4o-mini"
) : CloudLLMProvider {

    override suspend fun generateWithTeaching(
        prompt: String,
        systemPrompt: String
    ): Flow<String> = flow {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("API error: ${response.code}")

            val reader = response.body?.source()?.buffer() ?: return@use
            while (!reader.exhausted()) {
                val line = reader.readUtf8Line() ?: break
                if (line.startsWith("data: ") && line != "data: [DONE]") {
                    val json = JSONObject(line.removePrefix("data: "))
                    val content = json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                    if (!content.isNullOrBlank()) {
                        emit(content)
                    }
                }
            }
        }
    }

    override fun isConfigured(): Boolean = apiKey.isNotBlank()
}
```

### 7. Updated ChatViewModel

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val inferenceManager: InferenceManager,
    private val localLLMProvider: LocalLLMProvider,
    private val cloudLLMProvider: CloudLLMProvider?,
    private val nluSelfLearner: NLUSelfLearner,
    private val actionExecutor: ActionExecutor
) : ViewModel() {

    fun processUserInput(input: String) {
        viewModelScope.launch {
            // Step 1: NLU Classification
            val nluResult = intentClassifier.classify(input)

            when {
                // High confidence → Execute directly
                nluResult.confidence >= 0.75f -> {
                    executeIntent(nluResult.intent, input)
                }

                // Medium/Low confidence → LLM Fallback with Teaching
                else -> {
                    llmFallbackWithTeaching(input, nluResult)
                }
            }
        }
    }

    private suspend fun llmFallbackWithTeaching(
        input: String,
        nluResult: IntentClassificationResult
    ) {
        // Select backend based on battery/thermal state
        val backend = inferenceManager.selectBackend()

        // Show typing indicator
        _uiState.update { it.copy(isLoading = true) }

        val fullResponse = StringBuilder()

        when (backend) {
            InferenceManager.InferenceBackend.LOCAL_LLM -> {
                localLLMProvider.generateWithTeaching(
                    prompt = input,
                    systemPrompt = LLMTeacherPrompt.SYSTEM_PROMPT
                ).collect { token ->
                    fullResponse.append(token)
                    _uiState.update { it.copy(streamingResponse = fullResponse.toString()) }
                }
            }

            InferenceManager.InferenceBackend.CLOUD_LLM -> {
                cloudLLMProvider?.generateWithTeaching(
                    prompt = input,
                    systemPrompt = LLMTeacherPrompt.SYSTEM_PROMPT
                )?.collect { token ->
                    fullResponse.append(token)
                    _uiState.update { it.copy(streamingResponse = fullResponse.toString()) }
                } ?: run {
                    // Cloud not available, fall back to local
                    localLLMProvider.generateWithTeaching(
                        prompt = input,
                        systemPrompt = LLMTeacherPrompt.SYSTEM_PROMPT
                    ).collect { token ->
                        fullResponse.append(token)
                        _uiState.update { it.copy(streamingResponse = fullResponse.toString()) }
                    }
                }
            }

            InferenceManager.InferenceBackend.QUEUED -> {
                // Device too hot - show message and queue
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Device is warm. Please try again in a moment."
                    )
                }
                return
            }
        }

        // Parse LLM response for teaching data
        val parsed = LLMResponseParser.parse(fullResponse.toString())

        if (parsed != null) {
            // Show clean response to user (without metadata)
            addAssistantMessage(parsed.response)

            // Background: Teach NLU
            nluSelfLearner.learnFromLLM(input, parsed)
        } else {
            // Fallback: Show raw response
            addAssistantMessage(fullResponse.toString())
        }

        _uiState.update { it.copy(isLoading = false) }
    }
}
```

---

## Database Schema

```sql
-- Updated TrainExample table
CREATE TABLE TrainExample (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    utterance TEXT NOT NULL,
    intent TEXT NOT NULL,
    source TEXT NOT NULL DEFAULT 'user',  -- 'user', 'llm_auto', 'llm_variation', 'llm_confirmed'
    confidence REAL DEFAULT 1.0,
    embedding BLOB,                        -- 384 floats (1536 bytes)
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    user_confirmed INTEGER DEFAULT 0,      -- 1 if user verified
    times_matched INTEGER DEFAULT 0        -- How often this was matched
);

-- Indexes
CREATE INDEX idx_train_intent ON TrainExample(intent);
CREATE INDEX idx_train_source ON TrainExample(source);
CREATE INDEX idx_train_confidence ON TrainExample(confidence);
CREATE UNIQUE INDEX idx_train_utterance ON TrainExample(utterance);
```

---

## Cloud LLM Configuration (User Settings)

### Supported Providers

| Provider | API URL | Models | Cost |
|----------|---------|--------|------|
| **OpenAI** | api.openai.com | gpt-4o-mini, gpt-4o | $0.15-$10/1M tokens |
| **Anthropic** | api.anthropic.com | claude-3-haiku | $0.25/1M tokens |
| **Groq** | api.groq.com | llama-3.1-8b | Free tier available |
| **Together** | api.together.xyz | Llama, Mistral | $0.20/1M tokens |
| **Local** | localhost:1234 | Any GGUF | Free |

### Settings UI

```kotlin
@Composable
fun CloudLLMSettingsScreen() {
    var provider by remember { mutableStateOf("none") }
    var apiKey by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }

    Column {
        Text("Cloud AI Backup", style = MaterialTheme.typography.titleLarge)
        Text(
            "When your device is low on battery or too warm, AVA can use a cloud AI service.",
            style = MaterialTheme.typography.bodyMedium
        )

        RadioButtonGroup(
            options = listOf("None", "OpenAI", "Groq (Free)", "Custom"),
            selected = provider,
            onSelect = { provider = it }
        )

        if (provider != "None") {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation()
            )
        }

        if (provider == "Custom") {
            OutlinedTextField(
                value = customUrl,
                onValueChange = { customUrl = it },
                label = { Text("API URL") },
                placeholder = { Text("https://api.example.com/v1") }
            )
        }

        Button(onClick = { saveSettings() }) {
            Text("Save")
        }
    }
}
```

---

## Rationale

### Why LLM-as-Teacher?

| Approach | Pros | Cons |
|----------|------|------|
| **Manual Teaching** | User control | Friction, low adoption |
| **Cloud-only** | Always accurate | Privacy concerns, cost, latency |
| **LLM-as-Teacher** | Seamless, self-improving, private | Requires LLM availability |

### Why Battery-Aware Switching?

**Research Findings**:
- Mobile LLM inference drains 6-25% battery in 15 minutes ([arXiv](https://arxiv.org/html/2410.03613v3))
- CPU temp reaches 80°C during inference, triggering throttling
- MNN-AECS achieves 23% energy reduction through adaptive core selection ([arXiv](https://arxiv.org/html/2506.19884v1))

**Our Approach**:
- Monitor battery level and thermal status
- Switch to cloud when device is stressed
- Queue requests during critical thermal events
- Prefer local when charging or battery healthy

### Why WorkManager for Embeddings?

- **Battery-efficient**: Respects Doze mode, batches work
- **Guaranteed execution**: Survives app kills, reboots
- **Constraint-aware**: Only runs when battery not low
- **No user impact**: Background, non-blocking

---

## Consequences

### Positive

| Benefit | Impact |
|---------|--------|
| **Seamless UX** | User never sees NLU vs LLM distinction |
| **Self-improving** | System accuracy increases over time |
| **Battery-efficient** | Smart switching reduces drain |
| **Privacy-first** | All learning happens on-device |
| **Graceful degradation** | Works offline, low battery, high temp |

### Negative

| Drawback | Mitigation |
|----------|------------|
| **LLM latency** | NLU handles 90% of queries instantly |
| **Storage growth** | Prune old/low-confidence examples monthly |
| **LLM hallucination** | Confidence threshold + user confirmation option |
| **Cloud cost** | Free tier options (Groq), usage limits |

### Metrics to Track

| Metric | Target | Current |
|--------|--------|---------|
| NLU accuracy | > 85% | TBD |
| LLM fallback rate | < 15% | TBD |
| Self-taught intents/week | > 10 | TBD |
| Battery impact (30min use) | < 5% | TBD |
| Thermal throttle events | < 1/day | TBD |

---

## Testing Strategy

### Unit Tests

```kotlin
@Test fun `LLMResponseParser extracts all fields correctly`()
@Test fun `InferenceManager selects cloud when battery low`()
@Test fun `InferenceManager selects local when charging`()
@Test fun `NLUSelfLearner saves utterance and variations`()
@Test fun `EmbeddingComputeWorker computes valid embeddings`()
```

### Integration Tests

```kotlin
@Test fun `End-to-end LLM teaching flow saves to database`()
@Test fun `NLU recognizes previously-taught intent`()
@Test fun `Cloud fallback works when local unavailable`()
```

### Device Tests

| Scenario | Expected Behavior |
|----------|-------------------|
| Battery 80%, charging | Local LLM |
| Battery 20%, not charging | Cloud LLM |
| Battery 50%, temp 48°C | Cloud LLM |
| No network, battery 15% | Local LLM (no choice) |

---

## References

- [Taylor & Francis: LLM-generated Training Data](https://www.tandfonline.com/doi/full/10.1080/08839514.2024.2414483)
- [IntentGPT: Few-Shot Intent Discovery](https://arxiv.org/html/2411.10670v1)
- [Rasa LLM Intent Classification](https://rasa.com/docs/rasa/next/llms/llm-intent/)
- [MNN-AECS: Energy Optimization](https://arxiv.org/html/2506.19884v1)
- [Android WorkManager](https://developer.android.com/develop/background-work/background-tasks/optimize-battery)
- [NNAPI Battery Preferences](https://developer.android.com/ndk/guides/neuralnetworks/)
- ADR-003: ONNX NLU Integration
- ADR-008: Hardware-Aware Inference Backend

---

## Changelog

**v1.0 (2025-12-04)**: Initial decision - Self-Learning NLU with LLM-as-Teacher, battery-aware inference switching, cloud LLM fallback

---

**Created by Manoj Jhawar, manoj@ideahq.net**
