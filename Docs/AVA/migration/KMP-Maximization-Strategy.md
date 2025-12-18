# KMP Maximization Strategy for AVA AI

**Date**: 2025-10-29
**Directive**: "Use as much KMP as we can for cross-platform compatibility"
**Target Platforms**: Android (Phase 1) + iOS (Phase 2) for VoiceAvenue integration

---

## Executive Summary

**GOAL**: Maximize Kotlin Multiplatform usage across AVA AI to ensure seamless cross-platform compatibility for VoiceAvenue master app integration.

**CURRENT STATE**:
- ✅ Core modules (common, domain, data) are KMP
- ❌ Feature modules (nlu, chat, teach) are Android-only
- ❌ No iOS targets configured

**TARGET STATE**:
- ✅ All business logic in commonMain
- ✅ Platform-specific code isolated in expect/actual
- ✅ iOS targets configured for all modules
- ✅ 70-80% code reuse between Android and iOS

---

## 1. Current KMP Status Audit

### ✅ Already KMP (Configured Correctly)

| Module | Status | Targets | Code Sharing |
|--------|--------|---------|--------------|
| `core/common` | ✅ KMP | Android, Desktop | 100% |
| `core/domain` | ✅ KMP | Android, Desktop | 100% (models, interfaces) |
| `core/data` | ✅ KMP | Android, Desktop | ~40% (interfaces in commonMain, Room in androidMain) |

**What's Good**:
- Domain models (Message, Conversation, TrainExample) are fully portable
- Repository interfaces are shared
- Result wrapper is shared
- Coroutines usage is cross-platform

### ❌ Android-Only (Needs KMP Conversion)

| Module | Current Status | KMP Potential | Priority |
|--------|----------------|---------------|----------|
| `features/nlu` | Android-only | 🟡 Medium (50%) | P1 High |
| `features/chat` | Android-only | 🟢 High (80%) | P0 Critical |
| `features/teach` | Android-only | 🟢 High (90%) | P0 Critical |
| `features/alc-llm` | Not created | 🟢 High (80%) | P1 High |

---

## 2. Module-by-Module KMP Migration Plan

### 2.1 features/chat (Priority P0 - Critical)

**Current Architecture**: Android-only Compose UI + ViewModel

**KMP Potential**: 🟢 **80% sharable**

#### What Can Be Shared (commonMain)

```kotlin
// commonMain/com/augmentalis/ava/features/chat/domain/

// ✅ 100% shareable - Business logic
interface ChatRepository {
    suspend fun sendMessage(message: String): Flow<ChatResponse>
    suspend fun getConversationHistory(): Flow<List<Message>>
}

// ✅ 100% shareable - Use cases
class SendMessageUseCase(private val chatRepo: ChatRepository) {
    suspend operator fun invoke(message: String): Result<Unit> {
        // Pure business logic - works on all platforms
    }
}

// ✅ 100% shareable - Conversation state
class ConversationManager {
    private val messages = mutableStateListOf<Message>()

    fun addMessage(message: Message) {
        messages.add(message)
    }

    fun getHistory(): List<Message> = messages.toList()
}

// ✅ 100% shareable - ViewModels (with compose-multiplatform)
class ChatViewModel(
    private val sendMessage: SendMessageUseCase,
    private val conversationManager: ConversationManager
) : ViewModel() {
    val messages: StateFlow<List<Message>> = ...

    fun sendMessage(text: String) {
        viewModelScope.launch {
            // Business logic - works on all platforms
        }
    }
}
```

#### What Must Be Platform-Specific (expect/actual)

```kotlin
// commonMain - expect declaration
expect class PlatformTextToSpeech() {
    fun speak(text: String)
}

// androidMain - actual implementation
actual class PlatformTextToSpeech actual constructor() {
    private val tts = android.speech.tts.TextToSpeech(context, ...)
    actual fun speak(text: String) {
        tts.speak(text, ...)
    }
}

// iosMain - actual implementation
actual class PlatformTextToSpeech actual constructor() {
    private val synthesizer = AVSpeechSynthesizer()
    actual fun speak(text: String) {
        val utterance = AVSpeechUtterance(text)
        synthesizer.speakUtterance(utterance)
    }
}
```

**Migration Steps**:

1. **Create KMP module structure** (1 day)
   ```
   features/chat/
   ├── src/
   │   ├── commonMain/kotlin/
   │   │   ├── domain/          # Use cases, repositories
   │   │   ├── presentation/    # ViewModel (shared)
   │   │   └── ui/              # Composables (Compose Multiplatform)
   │   ├── androidMain/kotlin/
   │   │   └── platform/        # Android-specific (TTS, etc.)
   │   └── iosMain/kotlin/
   │       └── platform/        # iOS-specific (TTS, etc.)
   ```

2. **Move business logic to commonMain** (2 days)
   - ChatViewModel → commonMain/presentation/
   - SendMessageUseCase → commonMain/domain/
   - ConversationManager → commonMain/domain/

3. **Move UI to commonMain** (2 days)
   - ChatScreen.kt → commonMain/ui/ (Compose Multiplatform)
   - MessageBubble.kt → commonMain/ui/
   - All composables are portable!

4. **Extract platform-specific code** (1 day)
   - TTS → expect/actual
   - Notifications → expect/actual
   - File I/O → expect/actual

**Timeline**: 6 days
**Code Sharing**: 80% (UI + ViewModels + business logic)

---

### 2.2 features/nlu (Priority P1 - High)

**Current Architecture**: Android-only ONNX Runtime + MobileBERT

**KMP Potential**: 🟡 **50% sharable** (business logic only, native inference stays platform-specific)

#### What Can Be Shared (commonMain)

```kotlin
// commonMain/com/augmentalis/ava/features/nlu/

// ✅ Domain models
data class IntentClassification(
    val intent: String,
    val confidence: Float,
    val alternatives: List<Alternative>
)

// ✅ Use cases
class ClassifyIntentUseCase(private val classifier: PlatformIntentClassifier) {
    suspend operator fun invoke(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> {
        // Validation logic (shared)
        if (utterance.isBlank()) return Result.Error(...)

        // Delegate to platform classifier
        return classifier.classify(utterance, candidateIntents)
    }
}

// ✅ Caching logic
class IntentCache(private val maxSize: Int = 100) {
    private val cache = mutableMapOf<String, IntentClassification>()

    fun get(utterance: String): IntentClassification? = cache[utterance]

    fun put(utterance: String, classification: IntentClassification) {
        if (cache.size >= maxSize) {
            // LRU eviction
        }
        cache[utterance] = classification
    }
}
```

#### What Must Be Platform-Specific (expect/actual)

```kotlin
// commonMain - expect declaration
expect class PlatformIntentClassifier {
    suspend fun initialize(modelPath: String): Result<Unit>
    suspend fun classify(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification>
}

// androidMain - actual implementation
actual class PlatformIntentClassifier actual constructor() {
    private val onnxEngine = OrtEnvironment.getEnvironment()
    private lateinit var session: OrtSession

    actual suspend fun classify(...): Result<IntentClassification> {
        // ONNX Runtime Android-specific code
        val inputs = OnnxTensor.createTensor(...)
        val outputs = session.run(inputs)
        // Process outputs
    }
}

// iosMain - actual implementation
actual class PlatformIntentClassifier actual constructor() {
    private var mlModel: MLModel?

    actual suspend fun classify(...): Result<IntentClassification> {
        // Core ML iOS-specific code
        let prediction = try mlModel?.prediction(from: input)
        // Process prediction
    }
}
```

**Migration Steps**:

1. **Add iosMain target** (1 day)
2. **Move domain models to commonMain** (1 day)
3. **Extract IntentClassifier to expect/actual** (2 days)
4. **Implement iOS Core ML backend** (3 days)

**Timeline**: 7 days
**Code Sharing**: 50% (models, use cases, caching)

**Note**: iOS uses Core ML instead of ONNX Runtime (better Metal performance)

---

### 2.3 features/teach (Priority P0 - Critical)

**Current Architecture**: Android-only Teach-Ava UI

**KMP Potential**: 🟢 **90% sharable** (mostly UI + ViewModel, minimal platform-specific)

#### What Can Be Shared (commonMain)

```kotlin
// commonMain/com/augmentalis/ava/features/teach/

// ✅ ViewModel (100% shareable)
class TeachAvaViewModel(
    private val trainExampleRepo: TrainExampleRepository
) : ViewModel() {
    val examples: StateFlow<List<TrainExample>> = ...
    val selectedIntent: MutableStateFlow<String?> = ...

    fun addExample(utterance: String, intent: String) {
        viewModelScope.launch {
            trainExampleRepo.insert(TrainExample(...))
        }
    }

    fun deleteExample(id: String) {
        viewModelScope.launch {
            trainExampleRepo.delete(id)
        }
    }
}

// ✅ UI Components (100% shareable with Compose Multiplatform)
@Composable
fun TeachAvaScreen(viewModel: TeachAvaViewModel) {
    val examples by viewModel.examples.collectAsState()

    LazyColumn {
        items(examples) { example ->
            TrainingExampleCard(example, onDelete = { viewModel.deleteExample(it) })
        }
    }
}

@Composable
fun AddExampleDialog(onAdd: (String, String) -> Unit) {
    // 100% portable Compose code
}
```

**Migration Steps**:

1. **Move ViewModel to commonMain** (1 day)
2. **Move all Composables to commonMain** (1 day)
3. **Test on iOS** (1 day)

**Timeline**: 3 days
**Code Sharing**: 90% (UI + ViewModel, minimal platform-specific)

---

### 2.4 features/alc-llm (Priority P1 - New Module)

**See ALC_CROSS_PLATFORM_STRATEGY.md** - Already designed for KMP from day 1.

**KMP Potential**: 🟢 **80% sharable**

**Timeline**: Build as KMP from start (no migration needed)

---

## 3. Compose Multiplatform for UI Sharing

### Current UI Framework

- **Android**: Jetpack Compose Material 3
- **iOS**: Not yet implemented

### Strategy: Compose Multiplatform

**Compose Multiplatform** allows sharing Compose UI code between Android, iOS, Desktop, and Web.

```kotlin
// commonMain/ui/ChatScreen.kt - Works on Android + iOS!

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()

    Scaffold(
        topBar = { ChatTopBar() },
        bottomBar = { MessageInputField(onSend = viewModel::sendMessage) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(messages) { message ->
                MessageBubble(message)  // Shared component!
            }
        }
    }
}
```

**Benefits**:
- ✅ UI code works identically on Android + iOS
- ✅ ViewModels are shared (business logic)
- ✅ Navigation logic is shared
- ✅ Theme/styling is shared (Material 3)

**What's Platform-Specific**:
- ❌ Platform-specific APIs (camera, location, biometrics)
- ❌ Native views (WebView, MapView)
- ❌ Platform permissions

**Solution**: Use expect/actual for platform-specific UI components:

```kotlin
// commonMain
@Composable
expect fun PlatformWebView(url: String)

// androidMain
@Composable
actual fun PlatformWebView(url: String) {
    AndroidView(factory = { WebView(it).apply { loadUrl(url) } })
}

// iosMain
@Composable
actual fun PlatformWebView(url: String) {
    UIKitView(factory = { WKWebView().apply { load(URLRequest(URL(url))) } })
}
```

---

## 4. Database Layer: Room vs SQLDelight

### Current: Room (Android-only)

```kotlin
// core/data/src/androidMain/
@Database(entities = [ConversationEntity::class, ...])
abstract class AVADatabase : RoomDatabase() {
    // Android-only
}
```

### Problem

Room is Android-only. For iOS, we need a cross-platform solution.

### Solution: SQLDelight (KMP-Native)

**SQLDelight** is Kotlin Multiplatform-native and generates type-safe Kotlin APIs from SQL.

#### Migration Strategy

**Option 1: Dual Backend (Recommended for Gradual Migration)**

```kotlin
// commonMain - Repository interface (already exists)
interface ConversationRepository {
    suspend fun getAll(): Flow<List<Conversation>>
    suspend fun insert(conversation: Conversation)
}

// androidMain - Room implementation (keep existing)
class ConversationRepositoryImpl(
    private val dao: ConversationDao  // Room
) : ConversationRepository {
    override suspend fun getAll() = dao.getAll().map { it.toDomain() }
}

// iosMain - SQLDelight implementation (new)
class ConversationRepositoryImpl(
    private val database: AvaDatabase  // SQLDelight
) : ConversationRepository {
    override suspend fun getAll() = database.conversationQueries
        .selectAll()
        .asFlow()
        .map { it.map { it.toDomain() } }
}
```

**Option 2: Full SQLDelight Migration (Long-term)**

- Migrate all Room code to SQLDelight
- Use SQLDelight on both Android + iOS
- Benefit: Single database implementation

**Recommendation**: Start with Option 1 (dual backend), migrate to Option 2 in Phase 3.

**Timeline**:
- Option 1 (dual backend): 5 days (iOS SQLDelight only)
- Option 2 (full migration): 15 days (rewrite all Room code)

---

## 5. expect/actual Boundaries

### Guiding Principle

**Place expect/actual at the LOWEST level possible** to maximize code sharing.

### Good expect/actual Usage

```kotlin
// ✅ GOOD: Thin platform wrapper

// commonMain
expect class FileStorage() {
    fun write(path: String, data: ByteArray)
    fun read(path: String): ByteArray
}

// ALL business logic stays in commonMain
class ModelManager(private val storage: FileStorage) {
    fun saveModel(model: ByteArray) {
        val path = "models/gemma-2b.mlc"

        // ✅ Validation logic (SHARED)
        if (model.isEmpty()) throw IllegalArgumentException()

        // ❌ Platform-specific (DELEGATED)
        storage.write(path, model)
    }
}
```

### Bad expect/actual Usage

```kotlin
// ❌ BAD: Too much platform-specific code

// commonMain
expect class ModelManager() {
    fun saveModel(model: ByteArray)  // Too high-level!
    fun loadModel(): ByteArray
    fun validateModel(): Boolean
}

// Problem: All logic is duplicated in androidMain + iosMain
```

### Categories

| Category | Place in commonMain | Use expect/actual |
|----------|---------------------|-------------------|
| **Business Logic** | ✅ Yes | ❌ No |
| **Domain Models** | ✅ Yes | ❌ No |
| **ViewModels** | ✅ Yes | ❌ No |
| **Use Cases** | ✅ Yes | ❌ No |
| **UI Components** | ✅ Yes (Compose MP) | ❌ No (mostly) |
| **File I/O** | ❌ No | ✅ Yes |
| **Network** | ✅ Yes (Ktor) | ❌ No (Ktor is KMP) |
| **Database** | ❌ No (Room vs SQLDelight) | ✅ Yes (Repository impl) |
| **Native ML Runtime** | ❌ No (ONNX vs Core ML) | ✅ Yes (Thin wrapper) |
| **Permissions** | ❌ No | ✅ Yes |
| **Sensors** | ❌ No | ✅ Yes |

---

## 6. iOS Target Configuration

### Add iOS Targets to All Modules

#### Example: features/chat/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)  // Compose Multiplatform
}

kotlin {
    // Android target (existing)
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // 🆕 iOS targets (NEW)
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ChatFeature"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:common"))

                // Kotlin Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific dependencies
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // iOS-specific dependencies
            }
        }

        // Configure iOS targets
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.augmentalis.ava.features.chat"
    compileSdk = 34
    // ... rest of Android config
}
```

---

## 7. Migration Priority & Timeline

### Phase 1: Foundation (Week 1-2) - 10 days

**Goal**: Prepare infrastructure for iOS

| Task | Duration | Deliverable |
|------|----------|-------------|
| Add iOS targets to core modules | 2 days | core/common, core/domain with iOS |
| Setup SQLDelight for core/data (iOS) | 3 days | Dual backend (Room + SQLDelight) |
| Configure Compose Multiplatform | 2 days | Compose MP working on Android |
| Create expect/actual templates | 1 day | Reusable patterns documented |
| iOS development environment setup | 2 days | Xcode, CocoaPods, etc. |

### Phase 2: Features Migration (Week 3-5) - 15 days

| Module | Duration | Priority | Code Sharing |
|--------|----------|----------|--------------|
| features/chat | 6 days | P0 | 80% |
| features/teach | 3 days | P0 | 90% |
| features/nlu | 7 days | P1 | 50% |

### Phase 3: ALC-LLM (Week 6-7) - 10 days

| Task | Duration | Deliverable |
|------|----------|-------------|
| Build features/alc-llm as KMP | 5 days | Android + iOS stubs |
| Adopt MLC-LLM Android | 3 days | Android working |
| Bridge to MLC-LLM iOS | 2 days | iOS working |

### Phase 4: Polish & Testing (Week 8) - 5 days

| Task | Duration | Deliverable |
|------|----------|-------------|
| Cross-platform UI testing | 2 days | Test on iOS devices |
| Performance benchmarking | 2 days | Validate performance targets |
| Bug fixes & optimization | 1 day | Production-ready |

**Total Timeline**: 40 days (8 weeks)

---

## 8. Code Sharing Targets

### Target Metrics

| Layer | Target Sharing | Actual (Estimated) |
|-------|----------------|--------------------|
| **Domain Models** | 100% | 100% ✅ |
| **Business Logic** | 100% | 100% ✅ |
| **Use Cases** | 100% | 100% ✅ |
| **ViewModels** | 100% | 100% ✅ |
| **UI Components** | 90% | 90% ✅ |
| **Repository Interfaces** | 100% | 100% ✅ |
| **Repository Impl** | 40% | 40% (Room/SQLDelight split) |
| **ML Runtime** | 30% | 30% (ONNX/Core ML split) |
| **Platform Services** | 10% | 10% (TTS, permissions, etc.) |

**Overall Code Sharing**: **70-75%** of total codebase

---

## 9. Performance Considerations

### Kotlin/Native Performance on iOS

**Modern Kotlin/Native (1.9+)**:
- Memory: ARC-like, comparable to Swift
- CPU: 95-98% of pure Swift
- Interop: Minimal overhead with proper bridging

### Benchmarks (Estimated)

| Operation | Android (Kotlin/JVM) | iOS (Kotlin/Native) | iOS (Pure Swift) |
|-----------|---------------------|---------------------|------------------|
| Business Logic | 100% | 97% | 100% |
| UI Rendering | 100% (Compose) | 98% (Compose MP) | 100% (SwiftUI) |
| ML Inference | 100% (ONNX) | 100% (Core ML) | 100% (Core ML) |

**Verdict**: **No practical performance difference** for AVA AI use case.

---

## 10. Dependency Strategy

### KMP-Friendly Dependencies

Use these instead of platform-specific alternatives:

| Category | Android-Only | KMP Alternative | Status |
|----------|-------------|-----------------|--------|
| **Networking** | Retrofit | Ktor Client | ✅ Recommended |
| **Serialization** | Gson | kotlinx.serialization | ✅ Already using |
| **Coroutines** | ✅ (works everywhere) | kotlinx.coroutines | ✅ Already using |
| **Database** | Room | SQLDelight | 🔄 Migrate in Phase 1 |
| **DI** | Hilt | Koin | ⏳ Consider for Phase 2 |
| **Image Loading** | Coil | Kamel | ⏳ If needed |
| **Preferences** | SharedPreferences | Multiplatform Settings | ⏳ If needed |

### Migration Plan

1. **Phase 1**: Add SQLDelight alongside Room (dual backend)
2. **Phase 2**: Replace Retrofit with Ktor (if using REST APIs)
3. **Phase 3**: Migrate Room to SQLDelight fully (optional)

---

## 11. Testing Strategy

### Shared Tests (commonTest)

```kotlin
// commonTest/kotlin/features/chat/ChatViewModelTest.kt

class ChatViewModelTest {
    @Test
    fun `sendMessage should add message to conversation`() = runTest {
        val viewModel = ChatViewModel(...)

        viewModel.sendMessage("Hello")

        // ✅ Test runs on Android + iOS + JVM
        assertEquals(1, viewModel.messages.value.size)
    }
}
```

**Benefits**:
- Write tests ONCE
- Run on Android, iOS, and JVM
- 80%+ test coverage shared

### Platform-Specific Tests

```kotlin
// androidTest/kotlin/features/nlu/OnnxClassifierTest.kt

@Test
fun `ONNX classifier should load model on Android`() {
    val classifier = PlatformIntentClassifier()
    val result = classifier.initialize("model.onnx")
    assertTrue(result is Result.Success)
}

// iosTest/kotlin/features/nlu/CoreMLClassifierTest.kt

@Test
fun `Core ML classifier should load model on iOS`() {
    val classifier = PlatformIntentClassifier()
    val result = classifier.initialize("model.mlmodel")
    assertTrue(result is Result.Success)
}
```

---

## 12. Folder Structure (Target State)

```
AVA AI/
├── core/
│   ├── common/                          # ✅ Already KMP
│   │   └── src/
│   │       ├── commonMain/kotlin/       # Shared utilities, Result
│   │       ├── androidMain/kotlin/      # Android-specific
│   │       └── iosMain/kotlin/          # 🆕 iOS-specific (NEW)
│   │
│   ├── domain/                          # ✅ Already KMP
│   │   └── src/
│   │       ├── commonMain/kotlin/       # Domain models, repository interfaces
│   │       ├── androidMain/kotlin/      # (minimal)
│   │       └── iosMain/kotlin/          # 🆕 (minimal) (NEW)
│   │
│   └── data/                            # 🔄 Enhance KMP
│       └── src/
│           ├── commonMain/kotlin/       # Repository interfaces
│           ├── androidMain/kotlin/      # Room implementations
│           └── iosMain/kotlin/          # 🆕 SQLDelight implementations (NEW)
│
├── features/
│   ├── chat/                            # 🔄 Convert to KMP (Phase 2)
│   │   └── src/
│   │       ├── commonMain/kotlin/       # 🆕 ViewModel, UI, business logic
│   │       │   ├── domain/
│   │       │   ├── presentation/        # ChatViewModel
│   │       │   └── ui/                  # Composables (Compose MP)
│   │       ├── androidMain/kotlin/      # 🆕 Platform-specific (TTS, etc.)
│   │       └── iosMain/kotlin/          # 🆕 Platform-specific (TTS, etc.)
│   │
│   ├── teach/                           # 🔄 Convert to KMP (Phase 2)
│   │   └── src/
│   │       ├── commonMain/kotlin/       # 🆕 ViewModel, UI (90% shared)
│   │       ├── androidMain/kotlin/      # 🆕 (minimal)
│   │       └── iosMain/kotlin/          # 🆕 (minimal)
│   │
│   ├── nlu/                             # 🔄 Convert to KMP (Phase 2)
│   │   └── src/
│   │       ├── commonMain/kotlin/       # 🆕 Domain models, use cases, caching
│   │       ├── androidMain/kotlin/      # 🆕 ONNX Runtime wrapper
│   │       └── iosMain/kotlin/          # 🆕 Core ML wrapper
│   │
│   └── alc-llm/                         # 🆕 Build as KMP from start (Phase 3)
│       └── src/
│           ├── commonMain/kotlin/       # Business logic, routing, prompts
│           ├── androidMain/kotlin/      # MLC Android wrapper
│           └── iosMain/kotlin/          # MLC iOS wrapper
│
└── platform/
    ├── android/                         # Android app entry point
    └── ios/                             # 🆕 iOS app entry point (NEW)
```

---

## 13. Risk Mitigation

### Risk 1: Kotlin/Native Learning Curve

**Likelihood**: Medium
**Impact**: Medium

**Mitigation**:
- Start with simple modules (chat, teach)
- Use expect/actual sparingly
- Reference Compose Multiplatform samples
- Budget extra time for first iOS module

### Risk 2: Third-Party Library Compatibility

**Likelihood**: Medium
**Impact**: High

**Mitigation**:
- Audit dependencies before migration
- Use KMP alternatives (Ktor, SQLDelight, etc.)
- Isolate platform-specific libraries in expect/actual

### Risk 3: Performance on iOS

**Likelihood**: Low
**Impact**: Medium

**Mitigation**:
- Benchmark early (Week 2)
- Profile with Instruments (Xcode)
- Optimize hot paths with platform-specific code if needed

### Risk 4: Xcode Integration Issues

**Likelihood**: Medium
**Impact**: Medium

**Mitigation**:
- Use stable Kotlin version (1.9.21)
- Follow official KMP iOS integration guide
- Test on real devices early

---

## 14. Success Metrics

### Technical Metrics

- [ ] 70%+ code sharing between Android and iOS
- [ ] All business logic in commonMain (100%)
- [ ] All domain models in commonMain (100%)
- [ ] All ViewModels in commonMain (100%)
- [ ] 90%+ UI components in commonMain (Compose MP)
- [ ] <5% performance difference between Android/iOS
- [ ] All tests passing on both platforms

### Development Metrics

- [ ] 50% reduction in feature development time (second platform)
- [ ] 80% reduction in bug fix effort (fix once, not twice)
- [ ] Single CI/CD pipeline for both platforms

### Business Metrics

- [ ] iOS app launches within 2 months of Android
- [ ] Feature parity between Android and iOS
- [ ] VoiceAvenue integration ready

---

## 15. Next Steps (Immediate)

### Week 1 Actions

1. ✅ **Review this strategy** - Get team approval
2. ⏳ **Setup iOS development environment** (2 days)
   - Install Xcode
   - Configure CocoaPods
   - Setup iOS simulators
3. ⏳ **Add iOS targets to core modules** (2 days)
   - core/common → Add iosMain
   - core/domain → Add iosMain
   - core/data → Add iosMain with SQLDelight
4. ⏳ **Proof of concept** (2 days)
   - Build simplest KMP module on iOS
   - Verify Compose Multiplatform works
   - Test expect/actual pattern

### Week 2 Actions

5. ⏳ **Migrate features/chat to KMP** (6 days)
   - Move ViewModel to commonMain
   - Move Composables to commonMain
   - Test on iOS

6. ⏳ **Document patterns** (ongoing)
   - Create expect/actual examples
   - Document common pitfalls
   - Share learnings with team

---

## 16. Resources

### Official Documentation

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Ktor](https://ktor.io/docs/client.html)

### Sample Projects

- [KMP Template](https://github.com/JetBrains/compose-multiplatform-template)
- [Real-World KMP Apps](https://kotlinlang.org/docs/multiplatform-mobile-samples.html)

### Tools

- [KMP Wizard](https://kmp.jetbrains.com/) - Project setup
- [Kotlin/Native Memory Profiler](https://kotlinlang.org/docs/native-memory-manager.html)

---

## 17. Conclusion

**By maximizing KMP usage across AVA AI**:

✅ **70-75% code sharing** between Android and iOS
✅ **Faster iOS development** (build once, deploy twice)
✅ **Consistent behavior** across platforms
✅ **Easier maintenance** (fix bugs once)
✅ **VoiceAvenue integration ready** (cross-platform from day 1)

**Investment**: 8 weeks (40 days)
**Return**: 2x development speed for second platform, ongoing 50% maintenance reduction

**Recommendation**: Start migration immediately, prioritize features/chat and features/teach for quick wins.

---

**Document Version**: 1.0
**Created**: 2025-10-29
**Status**: Ready for Implementation
**Approval Required**: Yes (Strategic Decision)
