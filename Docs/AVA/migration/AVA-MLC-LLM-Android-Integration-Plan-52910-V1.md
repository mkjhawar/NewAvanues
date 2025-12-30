# MLC-LLM Android Integration Plan

**Date**: 2025-10-29
**Status**: Planning Phase
**Priority**: High (Post Phase 5)

---

## Executive Summary

This document outlines the plan to adopt the MLC-LLM Android implementation code from https://github.com/mlc-ai/mlc-llm/tree/main/android and integrate it into AVA AI. This approach makes the Android integration layer "ours" while leveraging the battle-tested MLC core runtime.

---

## 1. Strategic Decision: Adopt vs Wrapper Approach

### Question: Should We Apply This Approach to Other Android Implementations?

**SHORT ANSWER**: **NO** - MLC-LLM is a special case.

### Analysis of Current Android Dependencies

#### Category A: Framework Libraries (DO NOT ADOPT)

These are foundational Android/Google libraries that should remain as standard dependencies:

| Library | Purpose | Approach | Reasoning |
|---------|---------|----------|-----------|
| **AndroidX (Jetpack)** | UI, lifecycle, navigation | ✅ Standard dependency | Google-maintained, stable API, industry standard |
| **Compose Material 3** | UI framework | ✅ Standard dependency | Core Android UI, no customization needed |
| **Room Database** | Local persistence | ✅ Standard dependency | Well-designed API, minimal wrapper needed |
| **ONNX Runtime** | ML inference | ✅ Thin wrapper (existing) | Already using IntentClassifier pattern |

**Why NOT to adopt**:
- **Stable APIs**: These libraries have stable, well-documented APIs
- **No customization needed**: We use them as-is
- **Maintenance burden**: Adopting would require tracking massive codebases
- **No competitive advantage**: These are commodity dependencies

#### Category B: Speech Recognition (SPECIAL CASE - VOS4)

| Library | Purpose | Current Status | Approach |
|---------|---------|----------------|----------|
| **Vosk Android** | On-device speech recognition | Used in VOS4 (external) | ❓ Evaluate after VOS4 integration |
| **Google ML Kit** | ML services (translation, etc.) | Used in VOS4 | ✅ Standard dependency |

**VOS4 Note**: Speech recognition integration is handled by external VOS4 submodule. We should evaluate adoption strategy after VOS4 API integration is complete.

#### Category C: MLC-LLM (ADOPT THIS ONE)

| Library | Purpose | Why Adopt |
|---------|---------|-----------|
| **MLC-LLM Android** | Local LLM inference | ✅ **High customization value**<br>✅ **Core feature differentiator**<br>✅ **Privacy-critical component**<br>✅ **Requires AVA-specific integration** |

**Why MLC-LLM is Different**:
1. **Core Feature**: Local LLM is a key AVA differentiator (privacy-first AI)
2. **Heavy Customization**: Need AVA-specific:
   - Prompt templates
   - Conversation state management
   - Caching strategies
   - Hybrid cloud/local routing
   - Model configuration
3. **Integration Complexity**: Requires deep integration with our architecture
4. **Control Requirements**: Privacy and UX demand we control the integration layer

### Verdict: Adopt ONLY MLC-LLM Android Code

✅ **Adopt**: MLC-LLM Android implementation (mlc4j/)
❌ **Do NOT Adopt**: AndroidX, Compose, Room, ONNX Runtime, Google ML Kit
❓ **Evaluate Later**: Vosk (after VOS4 integration)

---

## 2. MLC-LLM Android Code Structure

### Upstream Repository

```
https://github.com/mlc-ai/mlc-llm
└── android/
    ├── mlc4j/                     # ✅ ADOPT THIS
    │   ├── src/main/java/
    │   │   └── ai/mlc/mlc/
    │   │       ├── MLCEngine.java          # Main inference API
    │   │       ├── LLMChat.kt              # Conversation management
    │   │       ├── ModelConfig.kt          # Model configuration
    │   │       ├── TokenProcessor.kt       # Tokenization
    │   │       └── utils/                  # Utilities
    │   └── build.gradle
    │
    ├── app/                       # 📖 REFERENCE (sample app)
    │   └── src/main/
    │       └── ChatActivity.kt
    │
    ├── build.gradle               # 🔧 ADAPT (build config)
    └── gradle/                    # 🔧 ADAPT (dependencies)
```

### Target AVA AI Structure

```
AVA AI/
├── features/llm/                  # ✅ NEW MODULE
│   ├── src/
│   │   ├── androidMain/kotlin/com/augmentalis/ava/features/llm/
│   │   │   ├── mlc/               # ✅ ADOPTED MLC CODE (customized)
│   │   │   │   ├── MLCEngine.kt   # Adopted from mlc4j/ (Java → Kotlin)
│   │   │   │   ├── LLMChat.kt     # Adopted from mlc4j/
│   │   │   │   ├── ModelConfig.kt # Adopted from mlc4j/
│   │   │   │   └── AVAModelConfig.kt  # 🆕 OUR EXTENSION
│   │   │   │
│   │   │   ├── LocalLLMProvider.kt     # 🆕 100% OURS
│   │   │   ├── CloudLLMProvider.kt     # 🆕 100% OURS
│   │   │   ├── HybridLLMProvider.kt    # 🆕 100% OURS
│   │   │   ├── PromptTemplates.kt      # 🆕 100% OURS
│   │   │   └── ConversationManager.kt  # 🆕 100% OURS
│   │   │
│   │   └── commonMain/kotlin/
│   │       └── domain/
│   │           └── LLMProvider.kt      # 🆕 Interface
│   │
│   ├── build.gradle.kts
│   └── libs/
│       └── libmlc_llm.so          # 🔵 THIRD-PARTY BINARY
│
└── docs/
    └── MLC_LLM_ANDROID_INTEGRATION_PLAN.md  # This document
```

---

## 3. Integration Plan: Step-by-Step

### Phase 1: Repository Setup & Code Adoption

**Estimated Time**: 1-2 days

#### Task 1.1: Fork MLC-LLM Repository (Optional)

```bash
# Option A: Fork on GitHub/GitLab
# Fork https://github.com/mlc-ai/mlc-llm to your organization

# Option B: Git remote tracking
cd "/Volumes/M Drive/Coding/AVA AI"
git remote add mlc-upstream https://github.com/mlc-ai/mlc-llm.git
git fetch mlc-upstream
```

**Deliverable**: Ability to track upstream MLC updates

#### Task 1.2: Create features/llm Module

```bash
cd "/Volumes/M Drive/Coding/AVA AI"
mkdir -p features/llm/src/androidMain/kotlin/com/augmentalis/ava/features/llm/mlc
mkdir -p features/llm/src/commonMain/kotlin/com/augmentalis/ava/features/llm/domain
mkdir -p features/llm/libs/arm64-v8a
mkdir -p features/llm/libs/armeabi-v7a
```

**Deliverable**: Module structure created

#### Task 1.3: Copy MLC Android Code

```bash
# Clone MLC-LLM repo temporarily
git clone https://github.com/mlc-ai/mlc-llm.git /tmp/mlc-llm

# Copy mlc4j Java/Kotlin source code
cp -r /tmp/mlc-llm/android/mlc4j/src/main/java/ai/mlc/mlc/* \
      features/llm/src/androidMain/kotlin/com/augmentalis/ava/features/llm/mlc/

# Copy native libraries (libmlc_llm.so)
cp /tmp/mlc-llm/android/mlc4j/src/main/jniLibs/*/libmlc_llm.so \
   features/llm/libs/
```

**Deliverable**: MLC Android code copied to AVA AI

#### Task 1.4: Convert Java to Kotlin (If Needed)

MLC-LLM mlc4j code is primarily Java. Convert to Kotlin:

```kotlin
// BEFORE (Java - from MLC)
public class MLCEngine {
    private long engineHandle;

    public String generate(String prompt) throws MLCException {
        // Native call
    }
}

// AFTER (Kotlin - our adopted version)
class MLCEngine {
    private var engineHandle: Long = 0

    @Throws(MLCException::class)
    fun generate(prompt: String): String {
        // Native call (unchanged)
    }
}
```

**Tool**: Android Studio's Java → Kotlin converter

**Deliverable**: All MLC code converted to Kotlin

---

### Phase 2: Build Integration

**Estimated Time**: 1-2 days

#### Task 2.1: Create features/llm/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                // Native library bundled in libs/
            }
        }
    }
}

android {
    namespace = "com.augmentalis.ava.features.llm"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        // Load native library
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("libs")
        }
    }
}
```

**Deliverable**: Build configuration for LLM module

#### Task 2.2: Add to settings.gradle.kts

```kotlin
include(":features:llm")
```

**Deliverable**: Module recognized by Gradle

#### Task 2.3: Build & Verify Native Library Loading

```bash
./gradlew :features:llm:assembleDebug
```

**Expected**: Module builds successfully, libmlc_llm.so bundled

**Deliverable**: Successful build with native library

---

### Phase 3: AVA-Specific Wrappers (100% Our Code)

**Estimated Time**: 3-4 days

#### Task 3.1: Create LLMProvider Interface

```kotlin
// features/llm/src/commonMain/kotlin/domain/LLMProvider.kt
package com.augmentalis.ava.features.llm.domain

sealed class LLMResponse {
    data class Success(val text: String) : LLMResponse()
    data class Error(val message: String, val exception: Throwable? = null) : LLMResponse()
    data class Streaming(val chunk: String) : LLMResponse()
}

interface LLMProvider {
    suspend fun generateResponse(prompt: String): LLMResponse
    suspend fun generateStreamingResponse(prompt: String): Flow<LLMResponse>
    suspend fun initialize(): Result<Unit>
    suspend fun cleanup()
}
```

**Deliverable**: Platform-agnostic LLM interface

#### Task 3.2: Create LocalLLMProvider (Wraps MLC)

```kotlin
// features/llm/src/androidMain/kotlin/LocalLLMProvider.kt
package com.augmentalis.ava.features.llm

import com.augmentalis.ava.features.llm.mlc.MLCEngine
import com.augmentalis.ava.features.llm.mlc.AVAModelConfig

class LocalLLMProvider private constructor(
    private val context: Context
) : LLMProvider {

    private lateinit var mlcEngine: MLCEngine
    private val conversationCache = LRUCache<String, String>(50)

    companion object {
        @Volatile
        private var instance: LocalLLMProvider? = null

        fun getInstance(context: Context): LocalLLMProvider {
            return instance ?: synchronized(this) {
                instance ?: LocalLLMProvider(context.applicationContext).also { instance = it }
            }
        }
    }

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // OUR initialization logic
            mlcEngine = MLCEngine(context)

            // OUR model configuration
            val config = AVAModelConfig.default()
            mlcEngine.load(config)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e, message = "Failed to initialize MLC engine")
        }
    }

    override suspend fun generateResponse(prompt: String): LLMResponse {
        // OUR caching strategy
        conversationCache[prompt]?.let {
            return LLMResponse.Success(it)
        }

        return try {
            // OUR prompt template
            val formattedPrompt = PromptTemplates.formatUserQuery(prompt)

            // Call MLC (thin wrapper)
            val response = mlcEngine.generate(formattedPrompt)

            // OUR post-processing
            conversationCache.put(prompt, response)
            LLMResponse.Success(response)
        } catch (e: Exception) {
            LLMResponse.Error("Generation failed", e)
        }
    }

    override suspend fun cleanup() {
        mlcEngine.unload()
    }
}
```

**Deliverable**: LocalLLMProvider wrapping MLC with our logic

#### Task 3.3: Create CloudLLMProvider

```kotlin
// features/llm/src/androidMain/kotlin/CloudLLMProvider.kt
package com.augmentalis.ava.features.llm

class CloudLLMProvider(
    private val apiKey: String,
    private val provider: CloudProvider = CloudProvider.ANTHROPIC
) : LLMProvider {

    enum class CloudProvider {
        ANTHROPIC,  // Claude
        OPENAI,     // GPT
        GOOGLE      // Gemini
    }

    override suspend fun generateResponse(prompt: String): LLMResponse {
        // OUR cloud API wrapper
        return when (provider) {
            CloudProvider.ANTHROPIC -> callClaudeAPI(prompt)
            CloudProvider.OPENAI -> callOpenAIAPI(prompt)
            CloudProvider.GOOGLE -> callGeminiAPI(prompt)
        }
    }

    private suspend fun callClaudeAPI(prompt: String): LLMResponse {
        // Implementation
    }
}
```

**Deliverable**: Cloud LLM provider wrapper

#### Task 3.4: Create HybridLLMProvider (Our Routing Logic)

```kotlin
// features/llm/src/androidMain/kotlin/HybridLLMProvider.kt
package com.augmentalis.ava.features.llm

class HybridLLMProvider(
    private val context: Context,
    private val cloudApiKey: String
) : LLMProvider {

    private val localProvider = LocalLLMProvider.getInstance(context)
    private val cloudProvider = CloudLLMProvider(cloudApiKey)

    override suspend fun generateResponse(prompt: String): LLMResponse {
        // 100% OUR ROUTING LOGIC
        return when {
            isPrivacySensitive(prompt) -> {
                // Keep on-device
                localProvider.generateResponse(prompt)
            }

            isOffline() -> {
                // Fallback to local
                localProvider.generateResponse(prompt)
            }

            requiresAdvancedReasoning(prompt) -> {
                // Use cloud for complex queries
                cloudProvider.generateResponse(prompt)
            }

            else -> {
                // Default: try local first, fallback to cloud
                val localResponse = localProvider.generateResponse(prompt)
                if (localResponse is LLMResponse.Error) {
                    cloudProvider.generateResponse(prompt)
                } else {
                    localResponse
                }
            }
        }
    }

    // OUR DECISION ALGORITHMS

    private fun isPrivacySensitive(prompt: String): Boolean {
        val sensitiveKeywords = listOf("password", "personal", "private", "ssn", "credit card")
        return sensitiveKeywords.any { prompt.lowercase().contains(it) }
    }

    private fun requiresAdvancedReasoning(prompt: String): Boolean {
        // OUR complexity heuristics
        val complexityIndicators = listOf("analyze", "explain why", "compare", "summarize")
        val wordCount = prompt.split(" ").size

        return wordCount > 50 || complexityIndicators.any { prompt.lowercase().contains(it) }
    }

    private fun isOffline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork == null
    }
}
```

**Deliverable**: Intelligent hybrid routing (100% our competitive advantage)

#### Task 3.5: Create AVA Prompt Templates

```kotlin
// features/llm/src/androidMain/kotlin/PromptTemplates.kt
package com.augmentalis.ava.features.llm

object PromptTemplates {

    // OUR AVA-SPECIFIC SYSTEM PROMPT
    private const val AVA_SYSTEM_PROMPT = """
        You are AVA (Augmented Virtual Assistant), a privacy-first AI assistant.

        Core Principles:
        - Privacy: Never suggest sending user data to external services
        - Helpfulness: Provide clear, actionable answers
        - Brevity: Keep responses concise (under 200 words)
        - Context-Aware: Remember conversation history

        User preferences:
        - Response style: {style}
        - Privacy mode: {privacy_mode}
    """.trimIndent()

    // OUR CONVERSATION FORMATTING
    fun formatUserQuery(
        userInput: String,
        conversationHistory: List<Message> = emptyList(),
        userPreferences: UserPreferences = UserPreferences.default()
    ): String {
        val systemPrompt = AVA_SYSTEM_PROMPT
            .replace("{style}", userPreferences.responseStyle.name)
            .replace("{privacy_mode}", userPreferences.privacyMode.name)

        val history = conversationHistory.joinToString("\n") { message ->
            "${message.role}: ${message.content}"
        }

        return buildString {
            appendLine(systemPrompt)
            appendLine()
            if (history.isNotEmpty()) {
                appendLine("Conversation History:")
                appendLine(history)
                appendLine()
            }
            appendLine("User: $userInput")
            append("AVA:")
        }
    }

    // OUR PROMPT VARIANTS

    fun formatPrivacyQuery(userInput: String): String {
        return """
            [PRIVACY MODE - KEEP ALL PROCESSING LOCAL]

            User Query: $userInput

            Respond without mentioning external services or data sharing.
        """.trimIndent()
    }

    fun formatQuickResponse(userInput: String): String {
        return """
            [QUICK RESPONSE MODE - MAXIMUM 50 WORDS]

            User: $userInput
            AVA (brief):
        """.trimIndent()
    }
}

data class UserPreferences(
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val privacyMode: PrivacyMode = PrivacyMode.STANDARD
) {
    enum class ResponseStyle { BRIEF, BALANCED, DETAILED }
    enum class PrivacyMode { STRICT, STANDARD, RELAXED }

    companion object {
        fun default() = UserPreferences()
    }
}
```

**Deliverable**: AVA-specific prompt engineering (100% ours)

---

### Phase 4: Integration with Chat UI

**Estimated Time**: 2-3 days

#### Task 4.1: Update ChatViewModel

```kotlin
// features/chat/ui/ChatViewModel.kt
class ChatViewModel(
    private val llmProvider: LLMProvider = HybridLLMProvider(context, apiKey)
) : ViewModel() {

    override fun sendMessage(text: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Add user message
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = text,
                role = MessageRole.USER,
                timestamp = System.currentTimeMillis()
            )
            addMessage(userMessage)

            // Generate LLM response
            when (val response = llmProvider.generateResponse(text)) {
                is LLMResponse.Success -> {
                    val avaMessage = Message(
                        id = UUID.randomUUID().toString(),
                        content = response.text,
                        role = MessageRole.ASSISTANT,
                        timestamp = System.currentTimeMillis()
                    )
                    addMessage(avaMessage)
                }

                is LLMResponse.Error -> {
                    _errorMessage.value = response.message
                }
            }

            _isLoading.value = false
        }
    }
}
```

**Deliverable**: Chat UI integrated with LLM

#### Task 4.2: Add Settings UI for LLM Preferences

```kotlin
// features/settings/SettingsScreen.kt
@Composable
fun LLMSettingsSection() {
    var useLocalLLM by remember { mutableStateOf(true) }
    var privacyMode by remember { mutableStateOf(PrivacyMode.STANDARD) }

    Column {
        SwitchPreference(
            title = "Use On-Device AI",
            summary = "Process queries locally for privacy",
            checked = useLocalLLM,
            onCheckedChange = { useLocalLLM = it }
        )

        DropdownPreference(
            title = "Privacy Mode",
            summary = "Control data sharing",
            value = privacyMode,
            options = PrivacyMode.values().toList(),
            onValueChange = { privacyMode = it }
        )
    }
}
```

**Deliverable**: User controls for LLM behavior

---

### Phase 5: Model Management

**Estimated Time**: 2-3 days

#### Task 5.1: Model Download & Storage

```kotlin
// features/llm/src/androidMain/kotlin/ModelManager.kt
class ModelManager(private val context: Context) {

    private val modelDir = File(context.filesDir, "llm_models")

    suspend fun downloadModel(
        modelId: String = "Llama-3.2-3B-Instruct-q4f16_1-MLC"
    ): Flow<DownloadProgress> = flow {
        // Download from MLC precompiled models
        val modelUrl = "https://huggingface.co/mlc-ai/$modelId/resolve/main/"

        // Download model weights
        downloadFile("$modelUrl/params_shard_0.bin", modelDir)
        emit(DownloadProgress(0.5f, "Downloading model weights..."))

        // Download config
        downloadFile("$modelUrl/mlc-chat-config.json", modelDir)
        emit(DownloadProgress(1.0f, "Model ready!"))
    }

    fun getInstalledModels(): List<ModelInfo> {
        return modelDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { ModelInfo(it.name, it.length()) }
            ?: emptyList()
    }

    fun deleteModel(modelId: String) {
        File(modelDir, modelId).deleteRecursively()
    }
}

data class DownloadProgress(val progress: Float, val message: String)
data class ModelInfo(val id: String, val sizeBytes: Long)
```

**Deliverable**: Model download and management

#### Task 5.2: Model Selection UI

```kotlin
// features/settings/ModelSelectionScreen.kt
@Composable
fun ModelSelectionScreen() {
    val modelManager = remember { ModelManager(context) }
    val installedModels by remember { modelManager.getInstalledModels() }.collectAsState()

    LazyColumn {
        items(installedModels) { model ->
            ModelCard(
                modelInfo = model,
                onDelete = { modelManager.deleteModel(model.id) }
            )
        }

        item {
            Button(onClick = { /* Show download dialog */ }) {
                Text("Download New Model")
            }
        }
    }
}
```

**Deliverable**: Model selection UI

---

### Phase 6: Testing & Optimization

**Estimated Time**: 3-4 days

#### Task 6.1: Unit Tests

```kotlin
// features/llm/src/androidUnitTest/kotlin/LocalLLMProviderTest.kt
@Test
fun `test local LLM generation`() = runTest {
    val provider = LocalLLMProvider.getInstance(context)
    provider.initialize()

    val response = provider.generateResponse("Hello AVA")

    assertTrue(response is LLMResponse.Success)
    assertFalse((response as LLMResponse.Success).text.isEmpty())
}

@Test
fun `test hybrid routing for privacy-sensitive query`() = runTest {
    val hybrid = HybridLLMProvider(context, "test-api-key")

    val response = hybrid.generateResponse("What's my password?")

    // Should use local provider for privacy-sensitive queries
    verify { localProvider.generateResponse(any()) }
    verify(exactly = 0) { cloudProvider.generateResponse(any()) }
}
```

**Deliverable**: Comprehensive test suite

#### Task 6.2: Performance Benchmarking

```kotlin
@Test
fun `benchmark local LLM inference time`() = runTest {
    val provider = LocalLLMProvider.getInstance(context)
    provider.initialize()

    val startTime = System.currentTimeMillis()
    provider.generateResponse("Test query")
    val inferenceTime = System.currentTimeMillis() - startTime

    // Target: < 3 seconds for first token
    assertTrue(inferenceTime < 3000, "Inference took ${inferenceTime}ms")
}
```

**Deliverable**: Performance metrics

---

### Phase 7: Documentation

**Estimated Time**: 1 day

#### Task 7.1: Developer Documentation

Create `docs/LLM_DEVELOPER_GUIDE.md`:
- How to add new model support
- How to customize prompts
- How to extend routing logic
- Troubleshooting guide

**Deliverable**: Developer guide

#### Task 7.2: User Documentation

Update `docs/USER_MANUAL.md`:
- How to download models
- How to switch between local/cloud
- Privacy implications
- Storage requirements

**Deliverable**: User guide

---

## 4. Tracking Upstream MLC Updates

### Strategy

```bash
# Setup (one-time)
git remote add mlc-upstream https://github.com/mlc-ai/mlc-llm.git
git fetch mlc-upstream

# Check for updates (monthly)
git fetch mlc-upstream
git log --oneline mlc-upstream/main -- android/mlc4j/

# Apply selective updates
git checkout -b update-mlc-android-2025-11
git cherry-pick <commit-hash-from-mlc>
# Resolve conflicts with our customizations
git push origin update-mlc-android-2025-11
```

### Monitoring Upstream

**Subscribe to**:
- MLC-LLM releases: https://github.com/mlc-ai/mlc-llm/releases
- Android folder changes: https://github.com/mlc-ai/mlc-llm/commits/main/android

**Review quarterly**:
- New model support
- Performance improvements
- Bug fixes
- API changes

---

## 5. Success Criteria

### Technical Metrics

- [ ] Local LLM generates first token in < 3 seconds
- [ ] Model download completes in < 5 minutes (on WiFi)
- [ ] Hybrid routing accuracy > 95% (privacy queries stay local)
- [ ] Memory usage < 2GB during inference
- [ ] APK size increase < 500MB (with bundled model)

### Code Ownership

- [ ] Android integration layer: ~80% our code
- [ ] Routing logic: 100% our code
- [ ] Prompt templates: 100% our code
- [ ] UI integration: 100% our code

### User Experience

- [ ] Offline mode works seamlessly
- [ ] Privacy-sensitive queries stay on-device
- [ ] Complex queries leverage cloud
- [ ] Model management UI intuitive

---

## 6. Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| 1. Repository Setup | 1-2 days | None |
| 2. Build Integration | 1-2 days | Phase 1 |
| 3. AVA Wrappers | 3-4 days | Phase 2 |
| 4. Chat UI Integration | 2-3 days | Phase 3 |
| 5. Model Management | 2-3 days | Phase 2 |
| 6. Testing & Optimization | 3-4 days | Phase 3, 4 |
| 7. Documentation | 1 day | All phases |

**Total Estimated Time**: 13-19 days (2.5-4 weeks)

---

## 7. Risks & Mitigations

### Risk 1: MLC API Changes

**Likelihood**: Medium
**Impact**: High
**Mitigation**:
- Pin to specific MLC commit initially
- Monitor upstream releases
- Test updates in separate branch before merging

### Risk 2: Model Size Constraints

**Likelihood**: High
**Impact**: Medium
**Mitigation**:
- Use quantized models (4-bit) initially
- Implement on-demand model download (not bundled in APK)
- Provide model size warnings in UI

### Risk 3: Performance on Lower-End Devices

**Likelihood**: High
**Impact**: Medium
**Mitigation**:
- Detect device capabilities at runtime
- Disable local LLM on devices < 4GB RAM
- Graceful fallback to cloud-only mode

### Risk 4: Native Library Conflicts

**Likelihood**: Low
**Impact**: High
**Mitigation**:
- Isolate MLC native libraries in separate classloader
- Test on multiple device types/Android versions
- Monitor crash reports post-release

---

## 8. Post-Integration Maintenance

### Monthly Tasks

- [ ] Check MLC-LLM releases for Android updates
- [ ] Review model repository for new quantized models
- [ ] Monitor user feedback on local LLM performance

### Quarterly Tasks

- [ ] Benchmark performance on new Android versions
- [ ] Evaluate new model architectures
- [ ] Review and update prompt templates based on user interactions

### Yearly Tasks

- [ ] Major refactor if MLC API changes significantly
- [ ] Consider alternative local LLM runtimes (GGML, etc.)

---

## 9. Comparison: Adopt vs Thin Wrapper

| Aspect | Thin Wrapper | Adopt MLC Android Code |
|--------|-------------|------------------------|
| **Code Ownership** | ~5% (wrapper only) | ~80% (integration layer) |
| **Customization** | Limited to wrapper API | Full control over integration |
| **Upstream Updates** | Easy (just update dependency) | Manual (selective cherry-pick) |
| **Maintenance Burden** | Low | Medium |
| **Competitive Advantage** | Low | High (custom routing, prompts) |
| **Integration Depth** | Surface-level | Deep integration |
| **Debugging** | Limited (opaque library) | Full visibility |
| **Privacy Control** | Limited | Complete |
| **Recommended for AVA?** | ❌ No | ✅ Yes |

---

## 10. Answer: Should We Do This for Other Android Implementations?

### Final Recommendation

**✅ YES for**: MLC-LLM Android only

**❌ NO for**:
- AndroidX/Jetpack libraries
- Compose Material 3
- Room Database
- ONNX Runtime
- Google ML Kit

**❓ EVALUATE LATER for**:
- Vosk Android (after VOS4 integration complete)

### Reasoning

MLC-LLM is the **ONLY** Android library where:

1. **High customization value**: We need deep control over LLM behavior
2. **Core differentiator**: Local, privacy-first AI is our competitive advantage
3. **Integration complexity**: Requires AVA-specific prompt engineering, routing, caching
4. **Privacy critical**: We must control how user data flows through LLM
5. **Manageable scope**: ~2,000 lines of integration code (not a massive framework)

Other libraries (AndroidX, Room, etc.) are:
- **Well-designed APIs**: No need to fork
- **Commodity dependencies**: Not differentiators
- **Massive codebases**: Too costly to maintain
- **Stable**: Minimal customization needed

---

## 11. Next Steps

1. **Review this plan** with team
2. **Approve scope** (MLC-LLM only, not other libraries)
3. **Schedule implementation** (2.5-4 weeks)
4. **Assign Phase 1** to begin repository setup

---

**Document Version**: 1.0
**Created**: 2025-10-29
**Last Updated**: 2025-10-29
**Status**: Awaiting Approval
