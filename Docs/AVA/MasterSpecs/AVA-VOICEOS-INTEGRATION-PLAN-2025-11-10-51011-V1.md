# AVA ↔ VoiceOS CommandManager Integration Plan

**Date:** 2025-11-10
**Status:** Specification Phase
**Priority:** High - Blocks production readiness

---

## Executive Summary

This document addresses 4 critical integration tasks:
1. **Intent Action Handlers** - Make AVA actually execute actions (not just acknowledge)
2. **JSON → Room Database Migration** - Move `intent_examples.json` into structured database
3. **Intent Storage Strategy** - Unified intent/command repository
4. **LLM Integration** - How LocalLLMProvider generates responses

**Key Decision:** Integrate AVA's NLU with VoiceOS CommandManager's existing dual-database architecture rather than creating a separate system.

---

## Architecture Overview

### Current State (AVA Standalone)

```
User Input → IntentClassifier → Intent Name → Template Response → Display
             (NLU + Embeddings)   (e.g. "show_time")  ("Here's the time")   (No action!)
```

**Problems:**
- ✅ Intent classification works perfectly (after double normalization fix)
- ❌ No action execution - just template responses
- ❌ Intent examples in JSON file (`intent_examples.json`)
- ❌ No unified command/intent storage
- ❌ LLM not integrated for response generation

### Target State (AVA + VoiceOS Integration)

```
User Input → IntentClassifier → Intent Name → CommandManager → Action Execution
             (NLU + Embeddings)   (semantic)     (VoiceOS)      (Real results!)
                                      ↓
                                 CommandDatabase
                                 (Room + SQLite)
                                      ↓
                                 LocalLLMProvider → Generated Response
                                 (Context-aware)
```

---

## 1. Intent Action Handlers Implementation

### Current Limitation

AVA returns template responses without executing actions:

```kotlin
// IntentTemplates.kt (Current - Templates Only)
private val templates = mapOf(
    "show_time" to "Here's the current time.",  // ❌ Doesn't show time
    "set_alarm" to "Setting an alarm for you.", // ❌ Doesn't set alarm
    "check_weather" to "Let me check the weather for you." // ❌ No weather
)
```

### Solution: CommandManager Integration

VoiceOS CommandManager already has action handlers:

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/CommandManager.kt`

```kotlin
// CommandManager.kt (VoiceOS - Real Actions)
private val navigationActions = mapOf(
    "nav_back" to NavigationActions.BackAction(),
    "nav_home" to NavigationActions.HomeAction(),
    "nav_recent" to NavigationActions.RecentAppsAction()
)

private val volumeActions = mapOf(
    "volume_up" to VolumeActions.VolumeUpAction(),
    "volume_down" to VolumeActions.VolumeDownAction(),
    "mute" to VolumeActions.MuteAction()
)

private val systemActions = mapOf(
    "wifi_toggle" to SystemActions.WifiToggleAction(),
    "bluetooth_toggle" to SystemActions.BluetoothToggleAction(),
    "open_settings" to SystemActions.OpenSettingsAction()
)
```

### Implementation Plan

#### Phase 1: Create Intent Action Handlers (Week 1)

**New Module:** `Universal/AVA/Features/Actions`

```kotlin
// IntentActionHandler.kt
interface IntentActionHandler {
    suspend fun canHandle(intent: String): Boolean
    suspend fun execute(intent: String, context: Context, params: Map<String, Any>): ActionResult
}

data class ActionResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)

// TimeActionHandler.kt
class TimeActionHandler : IntentActionHandler {
    override suspend fun canHandle(intent: String) = intent == "show_time"

    override suspend fun execute(intent: String, context: Context, params: Map<String, Any>): ActionResult {
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))

        // Option 1: Show in overlay UI
        // Option 2: Launch Clock app
        val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        context.startActivity(clockIntent)

        return ActionResult(
            success = true,
            message = "Current time: $currentTime",
            data = mapOf("time" to currentTime)
        )
    }
}

// AlarmActionHandler.kt
class AlarmActionHandler : IntentActionHandler {
    override suspend fun canHandle(intent: String) = intent == "set_alarm"

    override suspend fun execute(intent: String, context: Context, params: Map<String, Any>): ActionResult {
        // Extract time from params or use default
        val hour = params["hour"] as? Int ?: 7
        val minute = params["minute"] as? Int ?: 0

        // Use AlarmClock.ACTION_SET_ALARM
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Show clock UI
        }
        context.startActivity(alarmIntent)

        return ActionResult(
            success = true,
            message = "Alarm set for $hour:${minute.toString().padStart(2, '0')}"
        )
    }
}

// WeatherActionHandler.kt
class WeatherActionHandler : IntentActionHandler {
    override suspend fun canHandle(intent: String) = intent == "check_weather"

    override suspend fun execute(intent: String, context: Context, params: Map<String, Any>): ActionResult {
        // Option 1: Use Weather API (requires key)
        // Option 2: Launch default weather app
        val weatherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_WEATHER)
        }

        // Fallback: Open weather website
        if (weatherIntent.resolveActivity(context.packageManager) == null) {
            weatherIntent.action = Intent.ACTION_VIEW
            weatherIntent.data = Uri.parse("https://weather.com")
        }

        context.startActivity(weatherIntent)

        return ActionResult(
            success = true,
            message = "Opening weather information..."
        )
    }
}
```

#### Phase 2: Integrate with ChatViewModel (Week 1)

```kotlin
// ChatViewModel.kt (Enhanced)
class ChatViewModel @Inject constructor(
    private val classifyIntentUseCase: ClassifyIntentUseCase,
    private val actionHandlerRegistry: IntentActionHandlerRegistry, // NEW
    private val llmProvider: LocalLLMProvider // For context-aware responses
) : ViewModel() {

    suspend fun processUserMessage(message: String) {
        // Step 1: Classify intent (existing)
        val classification = classifyIntentUseCase(message, BuiltInIntents.ALL_INTENTS)

        when (classification) {
            is Result.Success -> {
                val intent = classification.data.intent
                val confidence = classification.data.confidence

                // Step 2: Execute action (NEW)
                val handler = actionHandlerRegistry.getHandler(intent)
                if (handler != null) {
                    val actionResult = handler.execute(intent, context, extractParams(message))

                    // Step 3: Generate response with LLM (NEW)
                    val response = if (actionResult.success) {
                        llmProvider.generateResponse(
                            prompt = "User asked: $message. Action taken: ${actionResult.message}. Provide a friendly confirmation.",
                            context = mapOf(
                                "intent" to intent,
                                "confidence" to confidence,
                                "action_result" to actionResult.data
                            )
                        )
                    } else {
                        "Sorry, I couldn't complete that action: ${actionResult.message}"
                    }

                    addMessage(ChatMessage(text = response, isUser = false))
                } else {
                    // Fallback to template if no handler
                    val response = IntentTemplates.getResponse(intent)
                    addMessage(ChatMessage(text = response, isUser = false))
                }
            }
            is Result.Error -> {
                addMessage(ChatMessage(text = "Sorry, I didn't understand that.", isUser = false))
            }
        }
    }
}
```

---

## 2. JSON → Room Database Migration

### Current State

**File:** `apps/ava-standalone/src/main/assets/intent_examples.json`

```json
{
  "control_lights": [
    "Turn on the lights",
    "Turn off the lights",
    "Dim the bedroom lights",
    "Make lights brighter",
    "Lights on"
  ],
  "show_time": [
    "What time is it?",
    "Show clock",
    "Time in Tokyo?",
    "Current time",
    "Tell me the time"
  ]
}
```

**Problems:**
- Static file - can't be updated at runtime
- No versioning or migration support
- No analytics (usage tracking)
- Not shareable across apps/devices

### Solution: VoiceOS CommandDatabase

VoiceOS already has a dual-database architecture:

#### Database 1: CommandDatabase (Voice Commands)

**Entities:**
1. **VoiceCommandEntity** - Localized command definitions
2. **DatabaseVersionEntity** - Schema version tracking
3. **CommandUsageEntity** - Usage analytics

**Schema:**

```kotlin
@Entity(
    tableName = "voice_commands",
    indices = [
        Index(value = ["id", "locale"], unique = true),
        Index(value = ["locale"]),
        Index(value = ["is_fallback"])
    ]
)
data class VoiceCommandEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    @ColumnInfo(name = "id") val id: String,           // "show_time"
    @ColumnInfo(name = "locale") val locale: String,   // "en-US"
    @ColumnInfo(name = "primary_text") val primaryText: String,  // "what time is it"
    @ColumnInfo(name = "synonyms") val synonyms: String,  // JSON array
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "priority") val priority: Int = 50,
    @ColumnInfo(name = "is_fallback") val isFallback: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
```

#### Database 2: LearningDatabase (AI Learning)

**Entities:**
1. **CommandUsageEntity** - Track which commands users actually use
2. **ContextPreferenceEntity** - Learn context-based preferences

### Migration Strategy

#### Step 1: Create Migration Script

```kotlin
// IntentExamplesMigration.kt
object IntentExamplesMigration {
    suspend fun migrateJsonToDatabase(context: Context, database: CommandDatabase) {
        val json = context.assets.open("intent_examples.json")
            .bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(json)
        val dao = database.voiceCommandDao()
        val entities = mutableListOf<VoiceCommandEntity>()

        jsonObject.keys().forEach { intentId ->
            val examples = jsonObject.getJSONArray(intentId)
            val examplesList = mutableListOf<String>()

            for (i in 0 until examples.length()) {
                examplesList.add(examples.getString(i))
            }

            // First example is primary, rest are synonyms
            entities.add(
                VoiceCommandEntity(
                    id = intentId,
                    locale = "en-US",
                    primaryText = examplesList[0],
                    synonyms = JSONArray(examplesList.drop(1)).toString(),
                    description = BuiltInIntents.getDisplayLabel(intentId),
                    category = BuiltInIntents.getCategory(intentId),
                    priority = 70,
                    isFallback = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        dao.insertAll(entities)
        Log.i("Migration", "Migrated ${entities.size} intents to database")
    }
}
```

#### Step 2: Update IntentClassifier to Use Database

```kotlin
// IntentClassifier.kt (Modified)
private suspend fun precomputeIntentEmbeddings() = withContext(Dispatchers.IO) {
    // NEW: Load from database instead of JSON
    val database = CommandDatabase.getInstance(context)
    val commands = database.voiceCommandDao().getCommandsByLocale("en-US")

    commands.forEach { command ->
        // Get all examples (primary + synonyms)
        val examples = listOf(command.primaryText) + command.parseSynonyms()

        // Compute RAW embeddings for all examples
        val exampleEmbeddings = examples.mapNotNull { example ->
            try {
                computeRawEmbedding(example)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to embed: $example")
                null
            }
        }

        // Average and normalize ONCE
        if (exampleEmbeddings.isNotEmpty()) {
            val avgEmbedding = averageEmbeddings(exampleEmbeddings)
            val normalizedAvg = l2Normalize(avgEmbedding)
            intentEmbeddings[command.id] = normalizedAvg
        }
    }

    Log.i(TAG, "Pre-computed ${intentEmbeddings.size} intent embeddings from database")
}
```

---

## 3. Intent/Command Storage Strategy

### Design Decision: Unified Repository

**Recommendation:** Use VoiceOS CommandDatabase as the single source of truth for both:
1. **Intents** (semantic classification) - AVA's NLU domain
2. **Commands** (action execution) - VoiceOS's execution domain

**Rationale:**
- Intents and commands are conceptually the same (user actions)
- Avoids data duplication and sync issues
- Leverages VoiceOS's mature database system (v3, with migrations)
- Enables cross-app learning (AVA learns from VoiceOS usage and vice versa)

### Mapping Strategy

```kotlin
// Intent ↔ Command Mapping
data class IntentCommandMapping(
    val intentId: String,      // AVA intent ID: "show_time"
    val commandId: String,     // VoiceOS command ID: "show_time" (same)
    val requiresParams: Boolean = false,
    val paramExtractors: List<ParamExtractor> = emptyList()
)

// Example Mappings
val mappings = listOf(
    IntentCommandMapping("show_time", "show_time"),
    IntentCommandMapping("set_alarm", "set_alarm", requiresParams = true,
        paramExtractors = listOf(TimeExtractor(), DateExtractor())),
    IntentCommandMapping("control_lights", "smart_home_lights", requiresParams = true,
        paramExtractors = listOf(ActionExtractor(), RoomExtractor()))
)
```

### Database Schema Enhancement

Add AVA-specific fields to VoiceCommandEntity:

```kotlin
@Entity(tableName = "voice_commands")
data class VoiceCommandEntity(
    // ... existing fields ...

    // NEW: AVA Integration Fields
    @ColumnInfo(name = "embedding_version") val embeddingVersion: Int = 1,
    @ColumnInfo(name = "last_embedding_update") val lastEmbeddingUpdate: Long = 0,
    @ColumnInfo(name = "confidence_threshold") val confidenceThreshold: Float = 0.7f,
    @ColumnInfo(name = "requires_llm_params") val requiresLLMParams: Boolean = false
)
```

---

## 4. LLM Integration & Response Generation

### Current State

LocalLLMProvider is a stub (P6 just completed):
- ✅ Model file validation
- ✅ Latency metrics tracking
- ❌ No actual inference
- ❌ Awaiting P7 (TVMTokenizer implementation)

### LLM Response Generation Flow

```
User Input → Intent Classification → Action Execution → LLM Context Building → Response Generation
             (Confidence: 88%)       (Alarm set!)      (Format context)       (Natural language)
```

#### Step 1: Build LLM Context

```kotlin
// LLMContextBuilder.kt
class LLMContextBuilder {
    fun buildContext(
        userMessage: String,
        intent: String,
        confidence: Float,
        actionResult: ActionResult
    ): String {
        return buildString {
            appendLine("User said: \"$userMessage\"")
            appendLine("Detected intent: $intent (confidence: ${(confidence * 100).toInt()}%)")
            appendLine("Action taken: ${actionResult.message}")
            if (actionResult.data != null) {
                appendLine("Result: ${actionResult.data}")
            }
            appendLine()
            appendLine("Generate a natural, friendly confirmation response (1-2 sentences).")
        }
    }
}
```

#### Step 2: Generate Response with LocalLLMProvider

```kotlin
// ChatViewModel.kt (LLM Integration)
suspend fun generateResponseWithLLM(
    intent: String,
    actionResult: ActionResult,
    userMessage: String,
    confidence: Float
): String {
    val context = llmContextBuilder.buildContext(
        userMessage, intent, confidence, actionResult
    )

    val llmResult = llmProvider.generate(
        prompt = context,
        maxTokens = 50,
        temperature = 0.7f,
        stopSequences = listOf("\n", "User:")
    )

    return when (llmResult) {
        is Result.Success -> llmResult.data.text
        is Result.Error -> {
            // Fallback to template if LLM fails
            IntentTemplates.getResponse(intent)
        }
    }
}
```

#### Step 3: Streaming Responses (Optional Enhancement)

```kotlin
// For better UX, stream LLM responses token-by-token
llmProvider.generateStreaming(prompt = context)
    .collect { token ->
        updateMessageStream(token)
    }
```

### LLM Requirements (Post-P7)

After TVMTokenizer is implemented:
1. Load quantized model (INT8 recommended for mobile)
2. Implement token-by-token generation
3. Add stop sequence detection
4. Implement context windowing (max 2048 tokens)
5. Add temperature/top-k/top-p sampling

---

## Implementation Timeline

### Phase 1: Intent Action Handlers (Week 1)
- [ ] Create `Features/Actions` module
- [ ] Implement 9 action handlers (BuiltInIntents)
- [ ] Create `IntentActionHandlerRegistry`
- [ ] Integrate with `ChatViewModel`
- [ ] Test with actual Android system intents

### Phase 2: Database Migration (Week 1-2)
- [ ] Copy VoiceOS CommandDatabase to AVA project
- [ ] Create migration script (JSON → Room)
- [ ] Update `IntentClassifier` to use database
- [ ] Add AVA-specific fields
- [ ] Test database versioning/migrations

### Phase 3: Unified Repository (Week 2)
- [ ] Design intent ↔ command mapping strategy
- [ ] Implement `IntentCommandRepository`
- [ ] Add usage analytics tracking
- [ ] Enable cross-app learning

### Phase 4: LLM Integration (Week 3, after P7)
- [ ] Complete P7 (TVMTokenizer)
- [ ] Implement `LLMContextBuilder`
- [ ] Integrate with `ChatViewModel`
- [ ] Add streaming support
- [ ] Implement fallback strategies

---

## Success Criteria

**Phase 1 Success:**
- ✅ "Show time" → Actually shows time or launches Clock app
- ✅ "Set alarm" → Launches AlarmClock with pre-filled time
- ✅ "Check weather" → Opens weather app or website

**Phase 2 Success:**
- ✅ All intent examples migrated to Room database
- ✅ No more `intent_examples.json` file
- ✅ Runtime intent updates possible
- ✅ Usage analytics tracking working

**Phase 3 Success:**
- ✅ Single source of truth for intents/commands
- ✅ AVA learns from VoiceOS usage
- ✅ VoiceOS learns from AVA usage
- ✅ No data duplication

**Phase 4 Success:**
- ✅ LLM generates context-aware responses
- ✅ Responses feel natural, not template-based
- ✅ Streaming works smoothly
- ✅ Fallback to templates if LLM fails

---

## Open Questions

1. **Should AVA reuse VoiceOS CommandManager directly?**
   - **Option A:** Import CommandManager as dependency
   - **Option B:** Create AVA-specific adapter layer
   - **Recommendation:** Option A - direct dependency to avoid duplication

2. **How to handle parameters (e.g., alarm time, light brightness)?**
   - Need parameter extraction from user message
   - Consider using regex, NER, or LLM for extraction
   - **Recommendation:** Start with regex, enhance with LLM later

3. **Should we use VoiceOS's confidence scoring system?**
   - VoiceOS has `ConfidenceScorer` and `ConfidenceLevel` (HIGH/MEDIUM/LOW/REJECT)
   - **Recommendation:** Yes - integrate with AVA's confidence scores

4. **Multi-language support timeline?**
   - VoiceOS supports multi-language commands
   - AVA currently English-only
   - **Recommendation:** Phase 2 enhancement (after basic integration)

---

## Dependencies

### AVA Project
- `Universal/AVA/Features/NLU` - Intent classification (✅ working)
- `Universal/AVA/Features/Chat` - UI and templates (✅ exists)
- `Universal/AVA/Features/LLM` - Response generation (⏳ P6 done, P7 needed)
- `Universal/AVA/Features/Actions` - **NEW** (❌ to be created)

### VoiceOS Project
- `managers/CommandManager` - Command execution system (✅ exists)
- `CommandDatabase` - Room database v3 (✅ mature)
- `LearningDatabase` - Usage analytics (✅ exists)

### External Dependencies
- Android AlarmClock API - `android.provider.AlarmClock`
- Android Settings API - `android.provider.Settings`
- Weather APIs (optional) - OpenWeatherMap, WeatherAPI.com

---

## References

- VoiceOS CommandManager: `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/`
- CommandDatabase Analysis: `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/docs/modules/CommandManager/database/CommandManager-Database-Analysis-251013-0309.md`
- AVA Intent Classification Fix: `docs/INTENT-CLASSIFICATION-FIX-2025-11-10.md`
- P6-P8 Implementation Spec: `docs/P6-P7-P8-IMPLEMENTATION-SPEC.md`

---

**Author:** AVA AI Team
**Reviewed:** Pending
**Next Steps:** Review and approve plan, then begin Phase 1 implementation
