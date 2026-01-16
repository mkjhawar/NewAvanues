# VoiceOSCoreNG Implementation Plan
**Date:** 2026-01-16 | **Version:** V1 | **Type:** Multi-Terminal Distributable

---

## Overview

This plan addresses all issues identified in the Full Analysis Report to make VoiceOSCoreNG a fully functional accessibility service with intelligent caching, NLU integration, and LLM fallback.

**Total Tasks:** 45
**Terminals Recommended:** 4-6
**Estimated Complexity:** High

---

## Terminal Distribution Strategy

| Terminal | Focus Area | Tasks |
|----------|------------|-------|
| **Terminal 1** | Android Core Fixes | P0/P1 Android bugs |
| **Terminal 2** | iOS Platform | iOS handlers, NLU, LLM, Speech |
| **Terminal 3** | Desktop Platform | Desktop handlers, NLU, LLM |
| **Terminal 4** | Speech Engines | Whisper, Google Cloud, Learning System |
| **Terminal 5** | NLU Enhancements | BERT improvements, embeddings |
| **Terminal 6** | Testing & Verification | Integration tests, E2E validation |

---

## Phase 1: Android Critical Fixes (Terminal 1)

### Task 1.1: Fix StaticCommandPersistence.refresh()
**Priority:** P1 | **Effort:** 1 hour | **Risk:** Low

**File:** `/Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/persistence/StaticCommandPersistence.kt`

**Issue:** The `refresh()` method at lines 95-100 starts deletion logic but doesn't complete it.

**Fix:**
```kotlin
override suspend fun refresh(): Int {
    var deletedCount = 0
    CommandCategory.entries.forEach { category ->
        // Delete existing static commands by category
        val deleted = voiceCommandRepository.deleteByCategory(category.name)
        deletedCount += deleted
    }
    return deletedCount
}
```

**Verification:**
```bash
# Run unit test
./gradlew :Modules:VoiceOSCoreNG:testDebugUnitTest --tests "*StaticCommandPersistenceTest*"
```

---

### Task 1.2: Implement LearnAppCore Integration
**Priority:** P1 | **Effort:** 4 hours | **Risk:** Medium

**File:** `/Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/exploration/ElementRegistrar.kt:86`

**Issue:** TODO comment indicates voice command generation not implemented.

**Implementation:**
1. Import LearnAppCore voice command generator
2. Call generator during element registration
3. Register generated commands with CommandRegistry
4. Persist static commands to database

```kotlin
// At line 86, replace TODO with:
private suspend fun generateVoiceCommands(elements: List<ElementInfo>, packageName: String) {
    val generator = LearnAppCore.getCommandGenerator()
    val commands = generator.generateFromElements(elements, packageName)
    commands.forEach { cmd ->
        commandRegistry.register(cmd)
        if (cmd.shouldPersist) {
            commandPersistence.insert(cmd)
        }
    }
}
```

**Verification:**
- Launch voiceoscoreng app
- Navigate to a new app screen
- Check logs for "Generated X voice commands"
- Verify commands appear in CommandRegistry

---

### Task 1.3: Verify Model File Availability
**Priority:** P1 | **Effort:** 30 mins | **Risk:** Low

**Check these paths exist:**
1. NLU Model: `models/nlu/malbert-intent-v1.onnx` in assets or external storage
2. LLM Models: `/sdcard/ava-ai-models/llm/` directory with model files
3. Vivoka Models: Check VivokaPathResolver configuration

**Verification Script:**
```bash
# On device via adb
adb shell "ls -la /sdcard/ava-ai-models/llm/"
adb shell "ls -la /sdcard/Download/vivoka-models/" # or configured path
```

---

### Task 1.4: Add packageName Validation at QuantizedCommand Creation
**Priority:** P2 | **Effort:** 1 hour | **Risk:** Low

**File:** `/Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/QuantizedCommand.kt`

**Issue:** packageName required at persistence but not enforced at creation.

**Fix:** Add validation in factory/builder:
```kotlin
fun create(
    phrase: String,
    avid: String,
    category: CommandCategory,
    metadata: Map<String, String>
): QuantizedCommand {
    require(metadata.containsKey("packageName")) {
        "QuantizedCommand requires packageName in metadata"
    }
    return QuantizedCommand(...)
}
```

---

## Phase 2: iOS Platform Implementation (Terminal 2)

### Task 2.1: Implement iOS BertTokenizer
**Priority:** P0 | **Effort:** 8 hours | **Risk:** High

**File:** `/Modules/Shared/NLU/src/iosMain/kotlin/com/augmentalis/nlu/BertTokenizer.kt`

**Current State:** Returns zero arrays (stub)

**Implementation:**
1. Load vocab.txt from bundle
2. Implement WordPiece tokenization algorithm
3. Handle [CLS], [SEP], [UNK], [PAD] special tokens
4. Return proper input_ids, attention_mask, token_type_ids

```kotlin
actual class BertTokenizer actual constructor() {
    private val vocab: Map<String, Int> = loadVocab()

    private fun loadVocab(): Map<String, Int> {
        // Load from bundle resource
        val vocabText = NSBundle.mainBundle.pathForResource("vocab", "txt")
            ?.let { NSString.stringWithContentsOfFile(it, NSUTF8StringEncoding, null) }
        return vocabText?.split("\n")?.mapIndexed { i, word -> word to i }?.toMap() ?: emptyMap()
    }

    actual fun tokenize(text: String, maxLength: Int): TokenizerOutput {
        val tokens = wordPieceTokenize(text.lowercase())
        val inputIds = IntArray(maxLength)
        val attentionMask = IntArray(maxLength)
        val tokenTypeIds = IntArray(maxLength)

        inputIds[0] = vocab["[CLS]"] ?: 101
        var idx = 1
        tokens.take(maxLength - 2).forEach { token ->
            inputIds[idx] = vocab[token] ?: vocab["[UNK]"] ?: 100
            attentionMask[idx] = 1
            idx++
        }
        inputIds[idx] = vocab["[SEP]"] ?: 102
        attentionMask[idx] = 1

        return TokenizerOutput(inputIds, attentionMask, tokenTypeIds)
    }

    private fun wordPieceTokenize(text: String): List<String> {
        // Implement WordPiece algorithm
        // Split on whitespace, then greedily match longest vocab entries
        // Prefix continuation tokens with ##
    }
}
```

**Verification:**
```bash
./gradlew :Modules:Shared:NLU:iosSimulatorArm64Test
```

---

### Task 2.2: Implement iOS NLU Processor with CoreML
**Priority:** P0 | **Effort:** 12 hours | **Risk:** High

**File:** `/Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/nlu/IOSNluProcessor.kt`

**Implementation:**
1. Load CoreML BERT model (.mlmodel)
2. Create prediction inputs from tokenizer output
3. Run inference
4. Process embeddings with SemanticMatcher

```kotlin
actual class IOSNluProcessor : INluProcessor {
    private var model: MLModel? = null
    private val tokenizer = BertTokenizer()
    private val semanticMatcher = SemanticMatcher()

    actual override suspend fun initialize() {
        val modelUrl = NSBundle.mainBundle.URLForResource("mobilebert", "mlmodelc")
        model = modelUrl?.let { MLModel.modelWithContentsOfURL(it, null) }
    }

    actual override suspend fun classify(text: String): NluResult {
        val tokens = tokenizer.tokenize(text, 64)
        val embedding = runInference(tokens)
        return semanticMatcher.match(embedding)
    }

    private fun runInference(tokens: TokenizerOutput): FloatArray {
        // Create MLFeatureProvider with input_ids, attention_mask
        // Call model.predictionFromFeatures()
        // Extract embedding from output
    }

    actual override fun isAvailable(): Boolean = model != null
}
```

---

### Task 2.3: Implement iOS LLM Processor
**Priority:** P0 | **Effort:** 16 hours | **Risk:** High

**File:** `/Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/llm/IOSLlmProcessor.kt`

**Options:**
1. **llama.cpp with Swift bindings** - Best for iOS
2. **CoreML LLM** - If model available
3. **Cloud fallback only** - Simplest

**Recommended: llama.cpp approach:**
```kotlin
actual class IOSLlmProcessor : ILlmProcessor {
    private var context: COpaquePointer? = null

    actual override suspend fun initialize(modelPath: String) {
        // Load GGUF model via llama.cpp
        context = llama_load_model_from_file(modelPath, params)
    }

    actual override suspend fun generate(prompt: String): LlmResult {
        // Tokenize prompt
        // Run inference with llama_eval
        // Decode tokens to text
    }
}
```

---

### Task 2.4: Implement iOS Speech Engine
**Priority:** P0 | **Effort:** 8 hours | **Risk:** Medium

**File:** `/Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/features/AppleSpeechEngine.kt`

**Implementation using SFSpeechRecognizer:**
```kotlin
actual class AppleSpeechEngine : ISpeechEngine {
    private var recognizer: SFSpeechRecognizer? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var audioEngine: AVAudioEngine? = null

    actual override fun initialize() {
        recognizer = SFSpeechRecognizer(locale: NSLocale.currentLocale)
        audioEngine = AVAudioEngine()
    }

    actual override fun startListening() {
        val request = SFSpeechAudioBufferRecognitionRequest()
        val inputNode = audioEngine?.inputNode

        inputNode?.installTapOnBus(0, bufferSize = 1024, format: inputNode.outputFormatForBus(0)) { buffer, _ ->
            request.appendAudioPCMBuffer(buffer)
        }

        audioEngine?.prepare()
        audioEngine?.startAndReturnError(null)

        recognitionTask = recognizer?.recognitionTaskWithRequest(request) { result, error ->
            if (result != null) {
                val text = result.bestTranscription.formattedString
                val isFinal = result.isFinal
                emitResult(RecognitionResult(text, isFinal))
            }
        }
    }
}
```

---

### Task 2.5: Implement iOS Handler Executors
**Priority:** P0 | **Effort:** 8 hours | **Risk:** Medium

**File:** `/Modules/VoiceOSCoreNG/src/iosMain/kotlin/com/augmentalis/voiceoscoreng/handlers/IOSHandlerFactory.kt`

**Implementation using UIAccessibility APIs:**
```kotlin
class IOSNavigationExecutor : INavigationExecutor {
    override suspend fun scrollUp() {
        // UIAccessibilityPostNotification(UIAccessibilityPageScrolledNotification)
        // Or use UIScrollView APIs
    }

    override suspend fun goBack() {
        // UIApplication.sharedApplication.sendAction("back:", to: nil, from: nil, forEvent: nil)
    }
}

class IOSSystemExecutor : ISystemExecutor {
    override suspend fun goHome() {
        // UIControl().sendAction("suspend", to: UIApplication.sharedApplication)
    }
}
```

---

## Phase 3: Desktop Platform Implementation (Terminal 3)

### Task 3.1: Implement Desktop NLU with ONNX Runtime
**Priority:** P0 | **Effort:** 6 hours | **Risk:** Medium

**File:** `/Modules/VoiceOSCoreNG/src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/nlu/DesktopNluProcessor.kt`

**Implementation:**
```kotlin
actual class DesktopNluProcessor : INluProcessor {
    private var session: OrtSession? = null
    private val tokenizer = BertTokenizer()

    actual override suspend fun initialize() {
        val env = OrtEnvironment.getEnvironment()
        val modelPath = "models/mobilebert.onnx"
        session = env.createSession(modelPath)
    }

    actual override suspend fun classify(text: String): NluResult {
        val tokens = tokenizer.tokenize(text, 64)
        val inputs = mapOf(
            "input_ids" to OnnxTensor.createTensor(env, tokens.inputIds),
            "attention_mask" to OnnxTensor.createTensor(env, tokens.attentionMask)
        )
        val outputs = session?.run(inputs)
        // Process outputs to get embeddings
    }
}
```

**Dependencies (build.gradle.kts):**
```kotlin
val desktopMain by getting {
    dependencies {
        implementation("com.microsoft.onnxruntime:onnxruntime:1.16.0")
    }
}
```

---

### Task 3.2: Implement Desktop LLM with llama.cpp JNI
**Priority:** P0 | **Effort:** 12 hours | **Risk:** High

**File:** `/Modules/VoiceOSCoreNG/src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/llm/DesktopLlmProcessor.kt`

**Implementation:**
1. Add llama.cpp JNI bindings
2. Load GGUF model
3. Implement generate() with streaming

```kotlin
actual class DesktopLlmProcessor : ILlmProcessor {
    private var modelPointer: Long = 0L

    actual override suspend fun initialize(modelPath: String) {
        System.loadLibrary("llama")
        modelPointer = LlamaJni.loadModel(modelPath)
    }

    actual override suspend fun generate(prompt: String): LlmResult {
        val tokens = LlamaJni.tokenize(modelPointer, prompt)
        val output = LlamaJni.generate(modelPointer, tokens, maxTokens = 256)
        return LlmResult(text = output, confidence = 1.0f)
    }
}
```

---

### Task 3.3: Implement Desktop Executors with AWT Robot
**Priority:** P0 | **Effort:** 6 hours | **Risk:** Low

**File:** `/Modules/VoiceOSCoreNG/src/desktopMain/kotlin/com/augmentalis/voiceoscoreng/handlers/StubExecutors.kt`

**Implementation:**
```kotlin
class DesktopNavigationExecutor : INavigationExecutor {
    private val robot = Robot()

    override suspend fun scrollUp() {
        robot.mouseWheel(-3) // Scroll up
    }

    override suspend fun scrollDown() {
        robot.mouseWheel(3) // Scroll down
    }

    override suspend fun goBack() {
        // Alt+Left on Windows/Linux, Cmd+Left on Mac
        if (System.getProperty("os.name").contains("Mac")) {
            robot.keyPress(KeyEvent.VK_META)
            robot.keyPress(KeyEvent.VK_LEFT)
            robot.keyRelease(KeyEvent.VK_LEFT)
            robot.keyRelease(KeyEvent.VK_META)
        } else {
            robot.keyPress(KeyEvent.VK_ALT)
            robot.keyPress(KeyEvent.VK_LEFT)
            robot.keyRelease(KeyEvent.VK_LEFT)
            robot.keyRelease(KeyEvent.VK_ALT)
        }
    }
}

class DesktopSystemExecutor : ISystemExecutor {
    override suspend fun goHome() {
        // Platform-specific home action
        if (System.getProperty("os.name").contains("Mac")) {
            Runtime.getRuntime().exec(arrayOf("osascript", "-e",
                "tell application \"Finder\" to activate"))
        }
    }
}

class DesktopUIExecutor : IUIExecutor {
    private val robot = Robot()

    override suspend fun click(x: Int, y: Int) {
        robot.mouseMove(x, y)
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    }
}
```

---

## Phase 4: Speech Engine Fixes (Terminal 4)

### Task 4.1: Bind Whisper JNI Methods
**Priority:** P0 | **Effort:** 8 hours | **Risk:** High

**File:** `/Modules/VoiceOS/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperNative.kt`

**Issue:** JNI methods return null/0L (stubs)

**Implementation:**
1. Build whisper.cpp as shared library
2. Create JNI header from WhisperNative class
3. Implement native methods in C++
4. Package .so files in jniLibs/

```cpp
// whisper_jni.cpp
JNIEXPORT jlong JNICALL Java_com_augmentalis_voiceos_speech_engines_whisper_WhisperNative_loadModelNative
  (JNIEnv *env, jobject obj, jstring path) {
    const char* modelPath = env->GetStringUTFChars(path, nullptr);
    whisper_context* ctx = whisper_init_from_file(modelPath);
    env->ReleaseStringUTFChars(path, modelPath);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jstring JNICALL Java_com_augmentalis_voiceos_speech_engines_whisper_WhisperNative_runInferenceNative
  (JNIEnv *env, jobject obj, jlong ctxPtr, jfloatArray audioData) {
    whisper_context* ctx = reinterpret_cast<whisper_context*>(ctxPtr);
    // Run whisper_full() inference
    // Return transcribed text
}
```

---

### Task 4.2: Integrate Google Cloud Speech Library
**Priority:** P1 | **Effort:** 4 hours | **Risk:** Medium

**Files:**
- `/Modules/VoiceOS/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleAuth.kt`
- `GoogleConfig.kt`, `GoogleNetwork.kt`, `GoogleStreaming.kt`

**Implementation:** When GoogleCloudSpeechLite library becomes available:
1. Add dependency to build.gradle.kts
2. Implement OAuth2 authentication
3. Create streaming recognition client
4. Handle results and errors

---

### Task 4.3: Re-enable Learning System
**Priority:** P1 | **Effort:** 6 hours | **Risk:** Medium

**File:** `/Modules/VoiceOS/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/common/LearningSystem.kt`

**Issue:** VoiceDataManager dependency removed, all methods are no-ops

**Fix:**
1. Create new `ILearningRepository` interface
2. Implement with SQLDelight (VoiceOS database)
3. Replace VoiceDataManager calls with repository calls
4. Re-enable learning in Vivoka and Android STT engines

---

## Phase 5: NLU Enhancements (Terminal 5)

### Task 5.1: Fix OnnxEmbeddingProvider Placeholder Mode
**Priority:** P1 | **Effort:** 4 hours | **Risk:** Medium

**File:** `/Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/shared/nlu/embedding/OnnxEmbeddingProvider.kt`

**Issue:** Returns hash-based placeholder embeddings instead of real BERT embeddings

**Fix:**
1. Ensure ONNX Runtime mobile is properly initialized
2. Load MobileBERT model from assets
3. Run actual inference for embeddings

---

### Task 5.2: Implement iOS CoreMLBackendSelector
**Priority:** P1 | **Effort:** 4 hours | **Risk:** Low

**File:** `/Modules/Shared/NLU/src/iosMain/kotlin/com/augmentalis/nlu/coreml/CoreMLBackendSelector.kt`

**Implementation:**
```kotlin
object CoreMLBackendSelector {
    fun selectBackend(): MLComputeUnits {
        // Check device capabilities
        return if (ProcessInfo.processInfo.isLowPowerModeEnabled) {
            MLComputeUnits.cpuOnly
        } else if (hasNeuralEngine()) {
            MLComputeUnits.all  // Prefer Neural Engine
        } else {
            MLComputeUnits.cpuAndGPU
        }
    }

    private fun hasNeuralEngine(): Boolean {
        // Check for A11+ chip (iPhone 8 and later)
        val model = UIDevice.currentDevice.model
        // Parse model identifier
    }
}
```

---

## Phase 6: Testing & Verification (Terminal 6)

### Task 6.1: Create E2E Test for Android Command Flow
**Priority:** P1 | **Effort:** 4 hours

**Test Scenario:**
1. Initialize VoiceOSCoreNG
2. Process "go back" command
3. Verify ActionCoordinator routes correctly
4. Verify handler executes
5. Verify result returned

```kotlin
@Test
fun testCommandFlowEndToEnd() = runTest {
    val core = VoiceOSCoreNG.createForAndroid(mockService)
    core.initialize()

    val result = core.processCommand("go back", 0.95f)

    assertTrue(result.success)
    assertEquals("back", result.action)
}
```

---

### Task 6.2: Create NLU Integration Test
**Priority:** P1 | **Effort:** 2 hours

**Test:**
```kotlin
@Test
fun testNluClassification() = runTest {
    val nlu = AndroidNluProcessor(context)
    nlu.initialize()

    val result = nlu.classify("scroll down please")

    assertTrue(result.confidence > 0.6f)
    assertEquals("scroll_down", result.intentId)
}
```

---

### Task 6.3: Create LLM Fallback Test
**Priority:** P1 | **Effort:** 2 hours

**Test:**
```kotlin
@Test
fun testLlmFallback() = runTest {
    val llm = AndroidLlmProcessor(context)
    llm.initialize()

    // Use ambiguous command that NLU won't match
    val result = llm.interpret("can you make the text bigger on screen")

    assertTrue(result.confidence > 0.5f)
    assertNotNull(result.action)
}
```

---

### Task 6.4: Create Screen Caching Test
**Priority:** P2 | **Effort:** 2 hours

**Test:**
```kotlin
@Test
fun testScreenCachingWorks() = runTest {
    val cacheManager = ScreenCacheManager(mockRepo, mockResources)

    // First visit - should scan
    val hash1 = cacheManager.generateScreenHash(mockNode)
    assertFalse(cacheManager.hasScreen(hash1))

    // Save screen
    cacheManager.saveScreen(hash1, "com.test.app", null, "1.0", 50)

    // Second visit - should hit cache
    assertTrue(cacheManager.hasScreen(hash1))
}
```

---

## Execution Instructions

### For Each Terminal

1. **Read the Full Analysis Report first:**
   ```
   /Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCoreNG/docs/analysis/VoiceOSCoreNG-Full-Analysis-260116-V1.md
   ```

2. **Set JDK 17:**
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   ```

3. **Work in isolation:**
   - Each terminal focuses on its assigned phase
   - No overlap on files unless coordinated
   - Use `/i.branch .createworktree` for isolation if needed

4. **Test after each change:**
   ```bash
   ./gradlew :Modules:VoiceOSCoreNG:allTests
   ```

5. **Commit with proper message:**
   ```bash
   git commit -m "fix(voiceoscoreng): [Task X.Y] Description"
   ```

---

## Task Checklist

### Phase 1: Android Core (Terminal 1)
- [ ] Task 1.1: Fix StaticCommandPersistence.refresh()
- [ ] Task 1.2: Implement LearnAppCore Integration
- [ ] Task 1.3: Verify Model File Availability
- [ ] Task 1.4: Add packageName Validation

### Phase 2: iOS Platform (Terminal 2)
- [ ] Task 2.1: Implement iOS BertTokenizer
- [ ] Task 2.2: Implement iOS NLU Processor with CoreML
- [ ] Task 2.3: Implement iOS LLM Processor
- [ ] Task 2.4: Implement iOS Speech Engine
- [ ] Task 2.5: Implement iOS Handler Executors

### Phase 3: Desktop Platform (Terminal 3)
- [ ] Task 3.1: Implement Desktop NLU with ONNX Runtime
- [ ] Task 3.2: Implement Desktop LLM with llama.cpp JNI
- [ ] Task 3.3: Implement Desktop Executors with AWT Robot

### Phase 4: Speech Engines (Terminal 4)
- [ ] Task 4.1: Bind Whisper JNI Methods
- [ ] Task 4.2: Integrate Google Cloud Speech Library
- [ ] Task 4.3: Re-enable Learning System

### Phase 5: NLU Enhancements (Terminal 5)
- [ ] Task 5.1: Fix OnnxEmbeddingProvider Placeholder Mode
- [ ] Task 5.2: Implement iOS CoreMLBackendSelector

### Phase 6: Testing (Terminal 6)
- [ ] Task 6.1: Create E2E Test for Android Command Flow
- [ ] Task 6.2: Create NLU Integration Test
- [ ] Task 6.3: Create LLM Fallback Test
- [ ] Task 6.4: Create Screen Caching Test

---

## Priority Order for Single Developer

If working alone, execute in this order:

1. **Task 1.1** - Quick fix, high impact
2. **Task 1.2** - Enables voice command generation
3. **Task 1.3** - Verify models before testing
4. **Task 6.1-6.4** - Create tests to validate
5. **Task 4.3** - Re-enable learning for better accuracy
6. **Task 4.1** - Whisper for offline support
7. **Tasks 2.x** - iOS platform (big effort)
8. **Tasks 3.x** - Desktop platform (big effort)

---

*Plan generated by Claude Opus 4.5 | 2026-01-16*
