# AVA AI - Developer Manual

**Version:** 2.0 (Phase 2.0 Production Ready)
**Last Updated:** 2025-11-26
**Framework:** IDEACODE v8.4

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture Overview](#architecture-overview)
3. [Getting Started](#getting-started)
4. [Core Modules](#core-modules)
5. [LLM Providers](#llm-providers)
6. [Testing](#testing)
7. [Deployment](#deployment)
8. [Contributing](#contributing)

---

## Introduction

AVA AI is a privacy-first, voice-enabled AI assistant built with Kotlin Multiplatform for Android. This manual provides comprehensive guidance for developers working on the AVA codebase.

### Key Features

- **On-Device NLU**: ONNX-based intent classification with MobileBERT + GPU acceleration
- **AON 3.0 Semantic Ontology**: Zero-shot intent classification with semantic descriptions (.aot files)
- **Hardware-Aware Inference**: Automatic backend selection (QNN/HTP > NNAPI > Vulkan > OpenCL > CPU)
- **Local LLM**: MLC-LLM with Gemma 2B/4B for on-device inference with Vulkan/OpenCL support
- **Cloud LLM**: 5 cloud providers (OpenRouter, Anthropic, OpenAI, HuggingFace, Google AI)
- **Teach-AVA**: User-trainable intent system
- **Privacy-First**: All data stays on device by default
- **RAG Support**: Document chat with retrieval-augmented generation

### File Extension Disambiguation

**Important:** AVA uses two similar but distinct file types:

- **.aot files** (lowercase) = **AVA Ontology Template/Text**
  - JSON-based semantic intent ontology files
  - Human-readable, editable text format
  - Schema: `ava-ontology-3.0`
  - Location: `apps/ava-app-android/src/main/assets/ontology/en-US/*.aot`
  - Examples: `communication.aot`, `device_control.aot`, `media.aot`

- **.AON files** (uppercase) = **AVA ONNX Network**
  - Binary model wrapper with security header
  - Compiled ONNX models for on-device inference
  - Location: External storage or app-specific directories
  - Examples: `malbert_v1.AON`, `mobilebert_v2.AON`

This naming convention was established to eliminate confusion between the two formats. Always use the correct extension when referencing these files in code or documentation.

### Tech Stack

- **Language**: Kotlin (Android), Kotlin Multiplatform (future iOS/Desktop)
- **UI**: Jetpack Compose with Material 3
- **Database**: Room with coroutines/Flow
- **DI**: Hilt
- **AI/ML**: ONNX Runtime, MLC-LLM, TVM
- **Networking**: OkHttp, Kotlinx Serialization

---

## Architecture Overview

### Clean Architecture Layers

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Compose UI, ViewModels, States)   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Domain Layer               │
│   (UseCases, Entities, Interfaces)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          Data Layer                 │
│  (Repositories, DAOs, Data Sources) │
└─────────────────────────────────────┘
```

### Module Structure

```
AVA/
├── Universal/AVA/
│   ├── Core/
│   │   ├── Common/         # Shared utilities, Result, etc.
│   │   ├── Domain/         # Domain entities and interfaces
│   │   └── Data/           # Room database, repositories
│   │
│   └── Features/
│       ├── NLU/            # Natural Language Understanding
│       ├── LLM/            # Large Language Models
│       ├── Chat/           # Chat UI and conversation management
│       ├── Teach/          # Teach-AVA training system
│       ├── RAG/            # Document chat
│       ├── Actions/        # Intent action execution
│       └── Overlay/        # UI overlays
│
└── apps/
    └── ava-app-android/    # Android application
    # Future platforms:
    # ├── ava-app-ios/      # iOS application
    # ├── ava-app-macos/    # macOS application
    # ├── ava-app-linux/    # Linux application
    # ├── ava-app-windows/  # Windows application
    # └── ava-app-web/      # Web application
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Gradle 8.5+
- Physical device or emulator (API 30+)

### Setup

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd AVA
   ```

2. **Open in Android Studio**
   - File → Open → Select AVA directory
   - Wait for Gradle sync

3. **Build Project**
   ```bash
   ./gradlew build
   ```

4. **Run Tests**
   ```bash
   # Unit tests
   ./gradlew test

   # NLU instrumented tests
   ./gradlew :Universal:AVA:Features:NLU:connectedDebugAndroidTest
   ```

5. **Run App**
   - Select `ava-app-android` configuration
   - Click Run (Shift+F10)

### Configuration

**Local Properties** (`local.properties`):
```properties
sdk.dir=/path/to/Android/sdk
ndk.dir=/path/to/Android/sdk/ndk/27.0.12077973
```

**API Keys** (for cloud providers):
- OpenRouter: Settings → Cloud LLM → OpenRouter API Key
- Anthropic: Settings → Cloud LLM → Anthropic API Key
- OpenAI: Settings → Cloud LLM → OpenAI API Key
- HuggingFace: Settings → Cloud LLM → HuggingFace Token
- Google AI: Settings → Cloud LLM → Google AI API Key

---

## Core Modules

### 1. NLU Module (`/Universal/AVA/Features/NLU`)

**Purpose:** On-device intent classification using ONNX MobileBERT

**Key Components:**
- `IntentClassifier`: ONNX-based classifier
- `BertTokenizer`: WordPiece tokenization (30,522 vocab)
- `ModelManager`: Model loading and fallback
- `ClassifyIntentUseCase`: Main use case

**Usage:**
```kotlin
// Initialize
val classifier = IntentClassifier(context)
classifier.initialize()

// Classify
val result = classifier.classify("turn on the lights", candidateIntents)
when (result) {
    is Result.Success -> {
        println("Intent: ${result.data.intent}, Confidence: ${result.data.confidence}")
    }
    is Result.Error -> {
        println("Error: ${result.message}")
    }
}
```

**Performance:**
- Inference: <100ms on mid-range devices
- Model size: 25.5 MB (INT8 quantized)
- Memory: <100MB overhead

**Tests:** 33 instrumented tests, 92% coverage

---

### 2. LLM Module (`/Universal/AVA/Features/LLM`)

**Purpose:** Local and cloud LLM integration

**Providers:**

| Provider | Type | Models | Cost |
|----------|------|--------|------|
| LocalLLM | On-device | Gemma 2B | Free |
| OpenRouter | Cloud | 100+ models | $0.10-$30/1M tokens |
| Anthropic | Cloud | Claude 3.5 Sonnet/Opus/Haiku | $3-$75/1M tokens |
| OpenAI | Cloud | GPT-4 Turbo, GPT-3.5 | $0.50-$30/1M tokens |
| HuggingFace | Cloud | Llama, Mistral, etc. | Free tier available |
| Google AI | Cloud | Gemini 1.5 Pro/Flash | $0.35-$10/1M tokens |

**Architecture:**
```kotlin
interface LLMProvider {
    suspend fun initialize(config: LLMConfig): Result<Unit>
    suspend fun chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse>
    suspend fun stop()
    fun isGenerating(): Boolean
    suspend fun checkHealth(): Result<ProviderHealth>
    fun estimateCost(inputTokens: Int, outputTokens: Int): Double
}
```

**Local LLM Usage:**
```kotlin
val provider = LocalLLMProvider(context)
provider.initialize(LLMConfig(
    modelPath = "gemma-2b-it-q4bf16_1-MLC",
    device = "opencl",
    maxMemoryMB = 2048
))

provider.chat(messages, options).collect { response ->
    when (response) {
        is LLMResponse.Streaming -> print(response.chunk)
        is LLMResponse.Complete -> println("\nDone: ${response.usage}")
        is LLMResponse.Error -> println("Error: ${response.message}")
    }
}
```

**Cloud LLM Usage:**
```kotlin
val provider = OpenAIProvider(context, apiKeyManager)
provider.initialize(LLMConfig(
    modelPath = "gpt-4-turbo-preview",
    apiKey = "sk-..."
))

provider.chat(messages, options).collect { response ->
    // Same as local
}
```

**Multi-Provider Fallback:**
```kotlin
val strategy = MultiProviderInferenceStrategy(
    providers = listOf(
        LocalLLMProvider(context),
        OpenRouterProvider(context, apiKeyManager),
        AnthropicProvider(context, apiKeyManager)
    )
)

// Automatically falls back if higher priority provider fails
val result = strategy.infer(request)
```

---

### 3. Chat Module (`/Universal/AVA/Features/Chat`)

**Purpose:** Conversational UI with NLU and LLM integration

**Key Components:**
- `ChatScreen`: Main UI (Compose)
- `ChatViewModel`: State management
- `MessageBubble`: Message display component
- `ConversationRepository`: Persistence

**Features:**
- Real-time streaming responses
- Intent-based responses
- Low-confidence → Teach-AVA flow
- Message persistence with Room
- Material 3 design

**Usage:**
```kotlin
@Composable
fun MyApp() {
    val viewModel: ChatViewModel = hiltViewModel()

    ChatScreen(
        viewModel = viewModel,
        onNavigateToTeach = { /* Navigate to Teach-AVA */ }
    )
}
```

---

### 4. Teach-AVA Module (`/Universal/AVA/Features/Teach`)

**Purpose:** User-trainable intent system

**Features:**
- Add/Edit/Delete training examples
- Intent filtering
- Locale support
- Hash-based deduplication (MD5)
- Usage tracking

**Database Schema:**
```kotlin
@Entity(tableName = "train_examples")
data class TrainExampleEntity(
    @PrimaryKey val id: String,
    val utterance: String,
    val intent: String,
    val locale: String = "en-US",
    val hash: String,  // MD5 of utterance
    val addedAt: Long,
    val usageCount: Int = 0
)
```

**Usage:**
```kotlin
// Add training example
val example = TrainExample(
    utterance = "turn on the lights",
    intent = "home.lights.on",
    locale = "en-US"
)
teachRepository.addExample(example)

// Train NLU with new examples
val examples = teachRepository.getAllExamples().first()
nluTrainer.train(examples)
```

---

### 5. Database Module (`/Universal/AVA/Core/Data`)

**Purpose:** Room-based persistence layer

**Entities:**
- `ConversationEntity`: Conversation metadata
- `MessageEntity`: Individual messages
- `TrainExampleEntity`: Training examples
- `DecisionEntity`: User decisions
- `LearningEntity`: Learning patterns
- `MemoryEntity`: Long-term memory

**Usage:**
```kotlin
// Repository pattern
class ConversationRepositoryImpl @Inject constructor(
    private val dao: ConversationDao
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveConversation(conversation: Conversation) {
        dao.insert(conversation.toEntity())
    }
}
```

**Performance Benchmarks:**
- Insert 1K records: <300ms (target <500ms) ✅
- Query 100 records: <40ms (target <100ms) ✅

---

## LLM Providers

### Adding a New Provider

**Step 1: Implement LLMProvider Interface**

```kotlin
package com.augmentalis.ava.features.llm.provider

class MyCustomProvider(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager
) : LLMProvider {

    override suspend fun initialize(config: LLMConfig): Result<Unit> {
        // Initialize your provider
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<LLMResponse> = flow {
        // Implement streaming chat
    }

    override suspend fun stop() {
        // Cancel ongoing requests
    }

    override fun isGenerating(): Boolean {
        // Return generation status
    }

    override fun getInfo(): LLMProviderInfo {
        // Return provider metadata
    }

    override suspend fun checkHealth(): Result<ProviderHealth> {
        // Health check
    }

    override fun estimateCost(inputTokens: Int, outputTokens: Int): Double {
        // Cost estimation
    }
}
```

**Step 2: Add ProviderType Enum**

```kotlin
// In LLMProvider.kt
enum class ProviderType {
    LOCAL,
    OPENROUTER,
    ANTHROPIC,
    OPENAI,
    HUGGINGFACE,
    GOOGLE_AI,
    MY_CUSTOM  // Add your provider
}
```

**Step 3: Register with Dependency Injection**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LLMModule {

    @Provides
    @Singleton
    fun provideMyCustomProvider(
        @ApplicationContext context: Context,
        apiKeyManager: ApiKeyManager
    ): MyCustomProvider {
        return MyCustomProvider(context, apiKeyManager)
    }
}
```

**Step 4: Add to Settings UI**

```kotlin
// In SettingsScreen.kt
DropdownSettingItem(
    title = "LLM Provider",
    options = listOf(
        "Local (On-Device)",
        "Anthropic (Claude)",
        "OpenRouter",
        "OpenAI",
        "HuggingFace",
        "Google AI",
        "My Custom"  // Add here
    ),
    // ...
)
```

---

## Testing

### Test Structure

```
src/
├── test/                  # Unit tests (JVM)
├── androidTest/           # Instrumented tests (device/emulator)
└── androidInstrumentedTest/  # NLU-specific instrumented tests
```

### Running Tests

**All Unit Tests:**
```bash
./gradlew test
```

**NLU Instrumented Tests:**
```bash
./gradlew :Universal:AVA:Features:NLU:connectedDebugAndroidTest
```

**LLM Tests:**
```bash
./gradlew :Universal:AVA:Features:LLM:test
```

**Chat Tests:**
```bash
./gradlew :Universal:AVA:Features:Chat:test
```

**Coverage Report:**
```bash
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

### Writing Tests

**Unit Test Example:**
```kotlin
@Test
fun `classify intent returns correct result`() = runTest {
    // Arrange
    val classifier = IntentClassifier(context)
    val candidates = listOf("home.lights.on", "home.lights.off")

    // Act
    val result = classifier.classify("turn on the lights", candidates)

    // Assert
    assertThat(result).isInstanceOf<Result.Success>()
    assertThat((result as Result.Success).data.intent).isEqualTo("home.lights.on")
    assertThat(result.data.confidence).isGreaterThan(0.7f)
}
```

**Instrumented Test Example:**
```kotlin
@RunWith(AndroidJUnit4::class)
class IntentClassifierIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var classifier: IntentClassifier

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking { classifier.initialize() }
    }

    @Test
    fun testClassification() = runTest {
        val result = classifier.classify("hello", listOf("greeting"))
        assertThat(result).isInstanceOf<Result.Success>()
    }
}
```

### Test Coverage Requirements

- **Critical Paths:** 100% (NLU, LLM providers)
- **Feature Modules:** 80%+
- **UI Components:** 60%+
- **Overall Target:** 85%+

---

## Deployment

### Build Variants

**Debug:**
```bash
./gradlew assembleDebug
```

**Release:**
```bash
./gradlew assembleRelease
```

**Release APK Location:**
`apps/ava-app-android/build/outputs/apk/release/ava-app-android-release.apk`

### Code Signing

Configure in `~/.gradle/gradle.properties`:
```properties
AVA_KEYSTORE_FILE=/path/to/keystore.jks
AVA_KEYSTORE_PASSWORD=***
AVA_KEY_ALIAS=ava-release
AVA_KEY_PASSWORD=***
```

### Proguard/R8

**Rules** (`proguard-rules.pro`):
```proguard
# Keep ONNX Runtime classes
-keep class ai.onnxruntime.** { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep MLC-LLM native methods
-keep class org.apache.tvm.** { *; }
```

### CI/CD

**GitHub Actions** (`.github/workflows/android.yml`):
```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run tests
        run: ./gradlew test
```

---

## Contributing

### Branch Strategy

- `main`: Production-ready releases
- `development`: Active development
- `feature/*`: Feature branches
- `bugfix/*`: Bug fixes
- `hotfix/*`: Critical fixes

### Commit Convention

Follow Conventional Commits:
```
feat: add OpenAI provider
fix: resolve memory leak in LLM module
docs: update developer manual
test: add NLU integration tests
refactor: simplify chat viewmodel
```

### Pull Request Process

1. Create feature branch from `development`
2. Implement changes
3. Write/update tests (maintain 85%+ coverage)
4. Update documentation
5. Run full test suite
6. Create PR to `development`
7. Address review feedback
8. Squash merge after approval

### Code Style

**Kotlin:**
- Official Kotlin style guide
- 4-space indentation
- 120 character line limit
- Use `val` over `var` where possible

**Format Code:**
```bash
./gradlew ktlintFormat
```

---

## Resources

### Documentation
- Architecture: `/docs/architecture/`
- Design Standards: `/docs/design-standards/`
- Context Saves: `/docs/context/`
- Status Reports: `/tmp/PHASE-1.0-FINAL-STATUS-REPORT.md`

### Additional Developer Manual Chapters

| Chapter | Topic | Description |
|---------|-------|-------------|
| [Chapter 34](../Developer-Manual-Chapter34-Intent-Management.md) | Intent Management | Intent classification and management |
| [Chapter 36](../Developer-Manual-Chapter36-VoiceOS-Command-Delegation.md) | VoiceOS Commands | Command delegation to VoiceOS |
| [Chapter 38](../Developer-Manual-Chapter38-LLM-Model-Management.md) | LLM Model Management | Managing local and cloud LLM models |
| [Chapter 41](../Developer-Manual-Chapter41-Status-Indicator.md) | Status Indicator | UI status indicator implementation |
| [Chapter 46](../Developer-Manual-Chapter46-VoiceOS-Centralized-Commands.md) | Centralized Commands | VoiceOS centralized command system |
| [Chapter 47](../Developer-Manual-Chapter47-GPU-Acceleration.md) | **GPU Acceleration** | Vulkan, OpenCL, NNAPI, QNN backends |
| [Chapter 48](../Developer-Manual-Chapter48-AON-3.0-Semantic-Ontology.md) | **AON 3.0 Format** | Semantic ontology format specification |
| [Chapter 49](../Developer-Manual-Chapter49-Action-Handlers.md) | **Action Handlers** | Intent action handler implementation guide |
| [Chapter 50](../Developer-Manual-Chapter50-External-Storage-Migration.md) | **External Storage Migration** | .AVAVoiceAvanues folder structure and migration |
| [Chapter 51](../Developer-Manual-Chapter51-3Letter-JSON-Schema.md) | **3-Letter JSON Schema** | Ecosystem-wide compact JSON standard |
| [Chapter 52](../Developer-Manual-Chapter52-RAG-System-Architecture.md) | **RAG System** | Document Q&A and knowledge base implementation |
| [Chapter 53](../ideacode/specs/UNIVERSAL-FILE-FORMAT-FINAL.md) | **Universal File Format (.aai)** | AVU format specification and cross-project integration |

### Architecture Decision Records (ADRs)

| ADR | Decision | Description |
|-----|----------|-------------|
| [ADR-003](../architecture/android/ADR-003-ONNX-NLU-Integration.md) | ONNX NLU | ONNX Runtime for NLU inference |
| [ADR-008](../architecture/android/ADR-008-Hardware-Aware-Inference-Backend.md) | **Hardware-Aware Backend** | Automatic GPU/DSP backend selection |
| [ADR-009](../architecture/android/ADR-009-iOS-CoreML-ANE-Integration.md) | **iOS Core ML** | ANE acceleration for iOS (staged) |
| [ADR-010](../architecture/android/ADR-010-External-Storage-AVAVoiceAvanues.md) | **External Storage Migration** | .AVAVoiceAvanues folder structure |
| [ADR-011](../architecture/shared/ADR-011-3Letter-JSON-Schema-Standard.md) | **3-Letter JSON Schema** | Ecosystem-wide JSON format standard |
| [ADR-012](../architecture/android/ADR-012-RAG-System-Architecture.md) | **RAG System** | Document Q&A architecture decisions |

### File Placement Guide

See [AVA-File-Placement-Guide.md](../AVA-File-Placement-Guide.md) for model and ontology file locations.

### External Links
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [ONNX Runtime](https://onnxruntime.ai/)
- [MLC-LLM](https://mlc.ai/mlc-llm/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt DI](https://dagger.dev/hilt/)

---

**Version:** 1.0 (Phase 1.0 MVP Complete)
**Last Updated:** 2025-11-21
**Maintained By:** AVA AI Team
