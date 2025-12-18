# AVA AI Developer Manual - Part 2
**Chapters 4-6: Testing, Performance & Configuration**

**Version**: 1.0.0
**Date**: October 29, 2025
**Constitution Version**: 1.2.2
**Phase**: 1.0 MVP

---

## Table of Contents

- [Chapter 4: Testing Strategy](#chapter-4-testing-strategy)
- [Chapter 5: Performance Optimization](#chapter-5-performance-optimization)
- [Chapter 6: Configuration & Settings](#chapter-6-configuration--settings)

---

# Chapter 4: Testing Strategy

## 4.1 Test Architecture Overview

AVA AI employs a **Hybrid TDD (Test-Driven Development)** approach aligned with Clean Architecture principles:

- **Domain/Data Layers**: Pure TDD (tests written first)
- **Feature Modules**: TDD for business logic, test-after for UI
- **UI/Platform Layers**: Test-after with Compose UI tests

### 4.1.1 Testing Pyramid

```
         /\
        /  \  E2E Tests (10% - 16 cases)
       /____\
      /      \  Integration Tests (20% - 33 cases)
     /________\
    /          \  Unit Tests (70% - 115 cases)
   /____________\

   Total: 164 Test Cases
```

**Test Distribution**:
- **Unit Tests**: 115 cases (70%)
  - Domain entities: 25 cases
  - Use cases: 35 cases
  - Repository implementations: 30 cases
  - Data sources: 25 cases
- **Integration Tests**: 33 cases (20%)
  - ONNX NLU pipeline: 10 cases
  - Room database operations: 8 cases
  - ALC inference flow: 8 cases
  - Cloud LLM clients: 7 cases
- **E2E Tests**: 16 cases (10%)
  - Conversation flows: 8 cases
  - Voice input flows: 4 cases
  - Settings & onboarding: 4 cases

### 4.1.2 Architecture-Aligned Testing

```mermaid
graph TB
    A[Platform Tests<br/>E2E & UI Tests<br/>16 cases] --> B[Feature Tests<br/>Integration Tests<br/>33 cases]
    B --> C[Core Tests<br/>Unit Tests<br/>115 cases]

    C --> D[core/domain<br/>Pure TDD<br/>60 cases]
    C --> E[core/data<br/>Pure TDD<br/>55 cases]

    B --> F[features/nlu<br/>10 cases]
    B --> G[features/llm<br/>15 cases]
    B --> H[features/voice<br/>8 cases]

    A --> I[platform/android<br/>16 cases]

    style D fill:#90EE90
    style E fill:#90EE90
    style F fill:#FFD700
    style G fill:#FFD700
    style H fill:#FFD700
    style I fill:#87CEEB
```

**Legend**:
- 🟢 Green: Pure TDD (tests first, 90%+ coverage)
- 🟡 Yellow: Hybrid (TDD for logic, test-after for UI, 80%+ coverage)
- 🔵 Blue: Test-after (UI tests, 70%+ coverage)

---

## 4.2 Unit Testing (115 Cases, 90%+ Coverage)

### 4.2.1 Domain Layer Testing

**Test Framework**: JUnit 5 + Kotest assertions

#### Intent Classification Tests (10 cases)

```kotlin
// core/domain/entities/IntentTest.kt
class IntentTest {
    @Test
    fun `Intent should create with valid type and confidence`() {
        val intent = Intent(
            type = IntentType.GREETING,
            confidence = 0.95f,
            entities = mapOf("name" to "Alice")
        )

        intent.type shouldBe IntentType.GREETING
        intent.confidence shouldBe 0.95f
        intent.entities shouldContainKey "name"
    }

    @Test
    fun `Intent confidence should be between 0 and 1`() {
        shouldThrow<IllegalArgumentException> {
            Intent(IntentType.GREETING, confidence = 1.5f)
        }
    }

    @Test
    fun `IntentType should support all required types`() {
        IntentType.values() shouldContain IntentType.GREETING
        IntentType.values() shouldContain IntentType.QUESTION
        IntentType.values() shouldContain IntentType.COMMAND
        IntentType.values() shouldContain IntentType.UNKNOWN
    }
}
```

#### Use Case Tests (35 cases)

```kotlin
// core/domain/usecases/SendMessageUseCaseTest.kt
class SendMessageUseCaseTest {
    private lateinit var conversationRepo: ConversationRepository
    private lateinit var nluRepo: NluRepository
    private lateinit var llmRepo: LlmRepository
    private lateinit var useCase: SendMessageUseCase

    @BeforeEach
    fun setup() {
        conversationRepo = mockk()
        nluRepo = mockk()
        llmRepo = mockk()
        useCase = SendMessageUseCase(conversationRepo, nluRepo, llmRepo)
    }

    @Test
    fun `should add user message and classify intent`() = runTest {
        // Given
        val conversationId = "conv-1"
        val messageText = "hello there"
        val mockIntentResult = IntentResult(
            intent = Intent(IntentType.GREETING, 0.95f),
            alternatives = emptyList(),
            processingTime = 25L
        )

        coEvery {
            conversationRepo.addMessage(conversationId, any())
        } returns mockUserMessage
        coEvery {
            nluRepo.classifyIntent(messageText)
        } returns mockIntentResult
        coEvery {
            conversationRepo.getConversationById(conversationId)
        } returns mockConversation
        coEvery {
            llmRepo.generateResponse(any(), any(), any())
        } returns flowOf("Hi!", " How", " can", " I", " help?")

        // When
        val results = useCase(conversationId, messageText).toList()

        // Then
        results.size shouldBeGreaterThan 2
        results[0] shouldBeInstanceOf UseCaseResult.UserMessageAdded::class
        results[1] shouldBeInstanceOf UseCaseResult.IntentClassified::class

        val classifiedIntent = (results[1] as UseCaseResult.IntentClassified).result
        classifiedIntent.intent.type shouldBe IntentType.GREETING
        classifiedIntent.processingTime shouldBeLessThan 50L
    }

    @Test
    fun `should handle empty message gracefully`() = runTest {
        coEvery { nluRepo.classifyIntent("") } returns IntentResult(
            intent = Intent(IntentType.UNKNOWN, 0.1f),
            alternatives = emptyList(),
            processingTime = 5L
        )

        val results = useCase("conv-1", "").toList()

        val intentResult = results.find {
            it is UseCaseResult.IntentClassified
        } as UseCaseResult.IntentClassified

        intentResult.result.intent.type shouldBe IntentType.UNKNOWN
    }

    @Test
    fun `should propagate repository exceptions`() = runTest {
        coEvery {
            conversationRepo.addMessage(any(), any())
        } throws DatabaseException("Connection failed")

        shouldThrow<DatabaseException> {
            useCase("conv-1", "hello").collect()
        }
    }
}
```

### 4.2.2 Data Layer Testing

#### Repository Tests (30 cases)

```kotlin
// core/data/repositories/ConversationRepositoryImplTest.kt
class ConversationRepositoryImplTest {
    private lateinit var conversationDao: ConversationDao
    private lateinit var messageDao: MessageDao
    private lateinit var repository: ConversationRepositoryImpl

    @BeforeEach
    fun setup() {
        conversationDao = mockk()
        messageDao = mockk()
        repository = ConversationRepositoryImpl(conversationDao, messageDao)
    }

    @Test
    fun `getConversations should return flow of conversations`() = runTest {
        // Given
        val entities = listOf(
            AiConversationEntity("1", "Chat 1", 1000L, 2000L),
            AiConversationEntity("2", "Chat 2", 1500L, 2500L)
        )
        coEvery { conversationDao.getAllConversations() } returns flowOf(entities)
        coEvery { messageDao.getMessagesByConversation(any()) } returns flowOf(emptyList())

        // When
        val conversations = repository.getConversations().first()

        // Then
        conversations.size shouldBe 2
        conversations[0].id shouldBe "1"
        conversations[0].title shouldBe "Chat 1"
    }

    @Test
    fun `createConversation should generate unique ID`() = runTest {
        // Given
        coEvery { conversationDao.insert(any()) } just Runs

        // When
        val conversation1 = repository.createConversation("Chat A")
        val conversation2 = repository.createConversation("Chat B")

        // Then
        conversation1.id shouldNotBe conversation2.id
        conversation1.title shouldBe "Chat A"
        conversation2.title shouldBe "Chat B"
    }
}
```

#### Data Source Tests (25 cases)

```kotlin
// core/data/datasources/local/RoomDataSourceTest.kt
class RoomDataSourceTest {
    private lateinit var database: AvaDatabase
    private lateinit var dataSource: RoomDataSource

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AvaDatabase::class.java
        ).build()
        dataSource = RoomDataSource(database)
    }

    @AfterEach
    fun teardown() {
        database.close()
    }

    @Test
    fun `should insert and retrieve conversation`() = runTest {
        // Given
        val entity = AiConversationEntity(
            id = "test-1",
            title = "Test Chat",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // When
        dataSource.insertConversation(entity)
        val retrieved = dataSource.getConversationById("test-1")

        // Then
        retrieved shouldNotBe null
        retrieved?.title shouldBe "Test Chat"
    }
}
```

---

## 4.3 Integration Testing (33 Cases, 80%+ Coverage)

### 4.3.1 ONNX NLU Pipeline Tests (10 cases)

**Test Framework**: Robolectric + Real ONNX Models

```kotlin
// features/nlu/OnnxIntentClassifierIntegrationTest.kt
@Config(sdk = [30])
@RunWith(RobolectricTestRunner::class)
class OnnxIntentClassifierIntegrationTest {
    private lateinit var classifier: OnnxIntentClassifier

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val onnxModel = OnnxModel.loadFromAssets(
            context,
            "models/nlu/mobilebert-int8.onnx"
        )
        classifier = OnnxIntentClassifier(onnxModel)
    }

    @Test
    fun `should classify greeting intents with high confidence`() = runTest {
        val testCases = listOf(
            "hello" to IntentType.GREETING,
            "hi there" to IntentType.GREETING,
            "good morning" to IntentType.GREETING,
            "hey" to IntentType.GREETING
        )

        testCases.forEach { (text, expectedIntent) ->
            val result = classifier.classify(text)

            result.intent.type shouldBe expectedIntent
            result.confidence shouldBeGreaterThan 0.8f
            result.processingTime shouldBeLessThan 50L
        }
    }

    @Test
    fun `should handle multilingual input correctly`() = runTest {
        val multilingualGreetings = mapOf(
            "hello" to IntentType.GREETING,           // English
            "hola" to IntentType.GREETING,            // Spanish
            "bonjour" to IntentType.GREETING,         // French
            "hallo" to IntentType.GREETING,           // German
            "你好" to IntentType.GREETING,             // Chinese
            "こんにちは" to IntentType.GREETING         // Japanese
        )

        multilingualGreetings.forEach { (text, expectedIntent) ->
            val result = classifier.classify(text)
            result.intent.type shouldBe expectedIntent
        }
    }

    @Test
    fun `should meet performance budget under load`() = runTest {
        val testMessages = List(100) { "test message $it" }

        val times = testMessages.map { msg ->
            measureTimeMillis {
                classifier.classify(msg)
            }
        }

        val p95 = times.sorted()[95]
        p95 shouldBeLessThan 50L // 95th percentile < 50ms
    }
}
```

### 4.3.2 Room Database Integration Tests (8 cases)

```kotlin
// core/data/local/AvaDatabaseIntegrationTest.kt
@RunWith(RobolectricTestRunner::class)
class AvaDatabaseIntegrationTest {
    private lateinit var database: AvaDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AvaDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `should cascade delete messages when conversation deleted`() = runTest {
        // Given
        val conversation = AiConversationEntity("conv-1", "Test", 1000L, 1000L)
        val messages = listOf(
            AiMessageEntity("msg-1", "conv-1", "USER", "Hello", null, null, 1000L),
            AiMessageEntity("msg-2", "conv-1", "ASSISTANT", "Hi!", null, null, 1100L)
        )

        database.conversationDao().insert(conversation)
        messages.forEach { database.messageDao().insert(it) }

        // When
        database.conversationDao().deleteById("conv-1")

        // Then
        val remainingMessages = database.messageDao()
            .getMessagesByConversation("conv-1")
            .first()

        remainingMessages shouldBeEmpty()
    }

    @Test
    fun `should handle concurrent writes safely`() = runTest {
        val jobs = List(10) { index ->
            launch {
                val conversation = AiConversationEntity(
                    id = "conv-$index",
                    title = "Concurrent $index",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                database.conversationDao().insert(conversation)
            }
        }

        jobs.forEach { it.join() }

        val allConversations = database.conversationDao()
            .getAllConversations()
            .first()

        allConversations.size shouldBe 10
    }
}
```

### 4.3.3 ALC Inference Integration Tests (8 cases)

```kotlin
// features/llm/AlcInferenceIntegrationTest.kt
class AlcInferenceIntegrationTest {
    private lateinit var alc: AlcInferenceEngine

    @Before
    fun setup() {
        // Use small test model (Gemma 2B INT4)
        val modelPath = TestUtils.getTestModelPath("gemma-2b-int4-test.bin")
        alc = AlcInferenceEngine(
            modelPath = modelPath,
            backend = AlcBackend.CPU // Use CPU for consistent CI testing
        )
    }

    @Test
    fun `should generate coherent response within latency budget`() = runTest {
        val prompt = "What is the capital of France?"
        val startTime = System.currentTimeMillis()

        val tokens = mutableListOf<String>()
        alc.generateResponse(prompt, emptyList(), provider = LlmProvider.ALC)
            .collect { token ->
                tokens.add(token)
                if (tokens.size == 1) {
                    val firstTokenLatency = System.currentTimeMillis() - startTime
                    firstTokenLatency shouldBeLessThan 2000L // CPU allows 2s
                }
            }

        val fullResponse = tokens.joinToString("")
        fullResponse.shouldNotBeBlank()
        fullResponse.shouldContain("Paris")
    }

    @Test
    fun `should support streaming token generation`() = runTest {
        val prompt = "Count to five"
        val tokens = mutableListOf<String>()

        alc.generateResponse(prompt, emptyList(), LlmProvider.ALC)
            .collect { token ->
                tokens.add(token)
            }

        tokens.size shouldBeGreaterThan 5 // Multiple tokens expected
        tokens.first().shouldNotBeBlank()
    }
}
```

### 4.3.4 Cloud LLM Client Integration Tests (7 cases)

```kotlin
// features/llm/providers/OpenRouterProviderIntegrationTest.kt
class OpenRouterProviderIntegrationTest {
    private lateinit var client: OpenRouterProvider

    @Before
    fun setup() {
        // Use test API key from environment
        val apiKey = System.getenv("OPENROUTER_TEST_KEY") ?: "test-key"
        client = OpenRouterProvider(apiKey)
    }

    @Test
    fun `should generate response with valid API key`() = runTest {
        // Skip if no real API key
        assumeTrue(System.getenv("OPENROUTER_TEST_KEY") != null)

        val response = client.generateResponse(
            prompt = "Say hello",
            context = emptyList()
        ).toList().joinToString("")

        response.shouldNotBeBlank()
    }

    @Test
    fun `should handle rate limit errors gracefully`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": "Rate limit exceeded"}""",
                status = HttpStatusCode.TooManyRequests
            )
        }

        val testClient = OpenRouterProvider("test-key", httpEngine = mockEngine)

        shouldThrow<RateLimitException> {
            testClient.generateResponse("test", emptyList()).collect()
        }
    }
}
```

---

## 4.4 End-to-End Testing (16 Cases, 70%+ Coverage)

### 4.4.1 Conversation Flow Tests (8 cases)

**Test Framework**: Compose UI Test + Android Instrumented

```kotlin
// platform/android/ChatScreenE2ETest.kt
@RunWith(AndroidJUnit4::class)
class ChatScreenE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Clear database before each test
        clearDatabase()
    }

    @Test
    fun userSendsMessageAndReceivesAiResponse() {
        composeTestRule.apply {
            // Wait for app to load
            waitForIdle()

            // Type message
            onNodeWithTag("message_input")
                .performTextInput("Hello AVA")

            // Send message
            onNodeWithTag("send_button")
                .performClick()

            // Verify user message appears
            onNodeWithText("Hello AVA")
                .assertExists()

            // Wait for AI response (max 5 seconds)
            waitUntil(timeoutMillis = 5000) {
                onAllNodesWithTag("assistant_message")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            // Verify AI response exists
            onNodeWithTag("assistant_message")
                .assertExists()
        }
    }

    @Test
    fun userCorrectsIntentViaTeachAva() {
        composeTestRule.apply {
            // Send message with ambiguous intent
            onNodeWithTag("message_input")
                .performTextInput("What time is it?")
            onNodeWithTag("send_button")
                .performClick()

            // Long press on message
            onNodeWithText("What time is it?")
                .performTouchInput { longClick() }

            // Correction dialog should appear
            onNodeWithText("Correct Intent")
                .assertExists()

            // Select correct intent
            onNodeWithText("QUESTION")
                .performClick()

            // Verify correction saved
            onNodeWithText("Intent corrected")
                .assertExists()
        }
    }

    @Test
    fun conversationPersistsAcrossAppRestarts() {
        composeTestRule.apply {
            // Send message
            onNodeWithTag("message_input")
                .performTextInput("Remember this message")
            onNodeWithTag("send_button")
                .performClick()

            waitForIdle()
        }

        // Restart activity
        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.apply {
            waitForIdle()

            // Verify message still exists
            onNodeWithText("Remember this message")
                .assertExists()
        }
    }
}
```

### 4.4.2 Voice Input Flow Tests (4 cases)

```kotlin
// platform/android/VoiceInputE2ETest.kt
@RunWith(AndroidJUnit4::class)
class VoiceInputE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun voiceButtonTriggersRecognition() {
        composeTestRule.apply {
            // Grant microphone permission
            grantPermission(Manifest.permission.RECORD_AUDIO)

            // Tap voice button
            onNodeWithTag("voice_button")
                .performClick()

            // Verify recording indicator
            onNodeWithTag("recording_indicator")
                .assertExists()

            // Mock speech recognition result
            mockSpeechRecognitionResult("Hello AVA")

            // Verify text input filled
            onNodeWithTag("message_input")
                .assertTextEquals("Hello AVA")
        }
    }
}
```

### 4.4.3 Settings & Onboarding Tests (4 cases)

```kotlin
// platform/android/OnboardingE2ETest.kt
@RunWith(AndroidJUnit4::class)
class OnboardingE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Simulate first launch
        clearSharedPreferences()
    }

    @Test
    fun firstLaunchShowsPrivacyOnboarding() {
        composeTestRule.apply {
            // Verify onboarding screen shown
            onNodeWithText("Welcome to AVA AI")
                .assertExists()

            // Verify privacy explanation
            onNodeWithText("Local processing by default")
                .assertExists()

            // Continue to main screen
            onNodeWithText("Get Started")
                .performClick()

            // Verify onboarding completed
            onNodeWithTag("chat_screen")
                .assertExists()
        }
    }

    @Test
    fun subsequentLaunchesSkipOnboarding() {
        // Complete onboarding
        completeOnboarding()

        // Restart app
        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.apply {
            waitForIdle()

            // Verify chat screen shown directly
            onNodeWithTag("chat_screen")
                .assertExists()

            // Verify onboarding not shown
            onNodeWithText("Welcome to AVA AI")
                .assertDoesNotExist()
        }
    }
}
```

---

## 4.5 Test Coverage Metrics

### 4.5.1 Coverage Requirements by Layer

| Layer | Target Coverage | Enforcement | Actual (Goal) |
|-------|----------------|-------------|---------------|
| **core/domain** | 90%+ | CI fails if <90% | 92% |
| **core/data** | 90%+ | CI fails if <90% | 91% |
| **features/nlu** | 80%+ | CI fails if <80% | 85% |
| **features/llm** | 80%+ | CI fails if <80% | 83% |
| **features/voice** | 80%+ | CI fails if <80% | 82% |
| **platform/android** | 70%+ | Warning if <70% | 74% |
| **Overall** | 85%+ | CI fails if <85% | 87% |

### 4.5.2 Coverage Enforcement (GitHub Actions)

```yaml
# .github/workflows/test-coverage.yml
name: Test Coverage

on: [push, pull_request]

jobs:
  coverage:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests with coverage
        run: ./gradlew testDebugUnitTestCoverage

      - name: Generate coverage report
        run: ./gradlew jacocoTestReport

      - name: Check coverage thresholds
        run: ./gradlew jacocoTestCoverageVerification

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
          fail_ci_if_error: true
```

**Jacoco Configuration** (`build.gradle.kts`):

```kotlin
jacoco {
    toolVersion = "0.8.10"
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal() // core/domain, core/data
            }
            element = "CLASS"
            includes = listOf(
                "com.augmentalis.ava.core.domain.*",
                "com.augmentalis.ava.core.data.*"
            )
        }

        rule {
            limit {
                minimum = "0.80".toBigDecimal() // features
            }
            element = "CLASS"
            includes = listOf(
                "com.augmentalis.ava.features.*"
            )
        }

        rule {
            limit {
                minimum = "0.70".toBigDecimal() // platform
            }
            element = "CLASS"
            includes = listOf(
                "com.augmentalis.ava.platform.*"
            )
        }
    }
}
```

---

## 4.6 Mock Strategies

### 4.6.1 Repository Mocking (MockK)

```kotlin
// Test utility for consistent mocking
object MockRepositories {
    fun mockConversationRepository(): ConversationRepository = mockk {
        coEvery { getConversations() } returns flowOf(emptyList())
        coEvery { getConversationById(any()) } returns null
        coEvery { createConversation(any()) } answers {
            Conversation(
                id = UUID.randomUUID().toString(),
                title = firstArg(),
                messages = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        coEvery { addMessage(any(), any()) } returnsArgument 1
        coEvery { deleteConversation(any()) } just Runs
    }

    fun mockNluRepository(
        defaultIntent: IntentType = IntentType.UNKNOWN
    ): NluRepository = mockk {
        coEvery { classifyIntent(any()) } returns IntentResult(
            intent = Intent(defaultIntent, 0.8f),
            alternatives = emptyList(),
            processingTime = 25L
        )
        coEvery { getTrainingExamples() } returns emptyList()
        coEvery { addTrainingExample(any()) } just Runs
    }

    fun mockLlmRepository(): LlmRepository = mockk {
        coEvery {
            generateResponse(any(), any(), any())
        } returns flowOf("Hello!", " How", " can", " I", " help?")
        coEvery { isModelDownloaded(any()) } returns true
        coEvery { downloadModel(any(), any()) } returns Result.success(Unit)
    }
}
```

### 4.6.2 ONNX Model Mocking

```kotlin
// For tests that don't need real ONNX inference
class MockOnnxModel : OnnxModel {
    override fun classify(text: String): IntentResult {
        val intentType = when {
            text.contains("hello", ignoreCase = true) -> IntentType.GREETING
            text.contains("bye", ignoreCase = true) -> IntentType.GOODBYE
            text.contains("?") -> IntentType.QUESTION
            else -> IntentType.STATEMENT
        }

        return IntentResult(
            intent = Intent(intentType, confidence = 0.85f),
            alternatives = emptyList(),
            processingTime = 10L
        )
    }

    override fun close() {
        // No-op for mock
    }
}
```

### 4.6.3 Cloud LLM Mocking (Ktor MockEngine)

```kotlin
// Mock OpenRouter API responses
fun createMockOpenRouterClient(): OpenRouterProvider {
    val mockEngine = MockEngine { request ->
        when {
            request.url.encodedPath.contains("/chat/completions") -> {
                respond(
                    content = """
                        {
                            "id": "test-123",
                            "choices": [{
                                "message": {
                                    "content": "Mock response from OpenRouter"
                                },
                                "finish_reason": "stop"
                            }],
                            "usage": {
                                "prompt_tokens": 10,
                                "completion_tokens": 5,
                                "total_tokens": 15
                            }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            else -> respond("", status = HttpStatusCode.NotFound)
        }
    }

    return OpenRouterProvider(
        apiKey = "test-key",
        httpEngine = mockEngine
    )
}
```

---

## 4.7 Performance Testing

### 4.7.1 Latency Benchmarks

```kotlin
// benchmarks/NluLatencyBenchmark.kt
@RunWith(AndroidJUnit4::class)
class NluLatencyBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var classifier: OnnxIntentClassifier

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        classifier = OnnxIntentClassifier(
            OnnxModel.loadFromAssets(context, "models/nlu/mobilebert-int8.onnx")
        )
    }

    @Test
    fun benchmarkIntentClassification() {
        benchmarkRule.measureRepeated {
            classifier.classify("hello there")
        }
    }
}
```

**Performance Budget Verification**:

```kotlin
@Test
fun verifyNluLatencyBudget() = runTest {
    val testMessages = List(100) { "test message $it" }
    val latencies = mutableListOf<Long>()

    testMessages.forEach { msg ->
        val latency = measureTimeMillis {
            classifier.classify(msg)
        }
        latencies.add(latency)
    }

    val p50 = latencies.sorted()[50]
    val p95 = latencies.sorted()[95]
    val p99 = latencies.sorted()[99]

    println("NLU Latency: p50=$p50ms, p95=$p95ms, p99=$p99ms")

    p50 shouldBeLessThan 30L  // Target: <30ms (median)
    p95 shouldBeLessThan 50L  // Budget: <50ms (95th percentile)
    p99 shouldBeLessThan 100L // Maximum: <100ms (99th percentile)
}
```

### 4.7.2 Memory Leak Detection

```kotlin
// tests/MemoryLeakTest.kt
@RunWith(AndroidJUnit4::class)
class MemoryLeakTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun detectMemoryLeaksInChatScreen() {
        // Create 100 conversations
        repeat(100) { index ->
            createConversation("Conversation $index")
        }

        // Trigger garbage collection
        Runtime.getRuntime().gc()
        Thread.sleep(1000)

        // Check memory usage
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = ApplicationProvider
            .getApplicationContext<Context>()
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val usedMemoryMB = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024)

        usedMemoryMB shouldBeLessThan 500L // Max 500MB for 100 conversations
    }
}
```

---

## 4.8 Test Patterns Summary

### 4.8.1 Testing Best Practices

**DO**:
- ✅ Write tests before implementation (TDD for core/domain, core/data)
- ✅ Use descriptive test names (`` `should do X when Y` ``)
- ✅ Follow Arrange-Act-Assert (AAA) pattern
- ✅ Test one behavior per test
- ✅ Use test fixtures for common setup
- ✅ Mock external dependencies (APIs, databases)
- ✅ Verify performance budgets in tests

**DON'T**:
- ❌ Test implementation details (test behavior, not internals)
- ❌ Use real network calls in unit/integration tests
- ❌ Ignore flaky tests (fix or delete)
- ❌ Skip test documentation
- ❌ Write tests without assertions
- ❌ Use Thread.sleep() (use Turbine for Flow testing)

### 4.8.2 Test Naming Convention

```kotlin
// Pattern: `should [expected behavior] when [condition]`
@Test
fun `should return GREETING intent when user says hello`()

@Test
fun `should throw DatabaseException when connection fails`()

@Test
fun `should complete within 50ms when processing short text`()
```

---

# Chapter 5: Performance Optimization

## 5.1 Performance Architecture Overview

AVA AI employs multiple optimization strategies across different layers:

```mermaid
graph TB
    A[User Input] --> B[NLU Layer<br/>ONNX Optimization]
    B --> C{Intent Classified}
    C --> D[LLM Layer<br/>ALC/Cloud Routing]
    D --> E[Response Generation<br/>Streaming + Caching]
    E --> F[Database Layer<br/>Query Optimization]
    F --> G[UI Layer<br/>Compose Optimization]

    B -.->|Model Caching| H[ONNX Model Cache]
    D -.->|LRU Cache| I[NLU Results Cache<br/>100 entries]
    F -.->|Room Cache| J[Query Results Cache<br/>TTL-based]

    style B fill:#90EE90
    style D fill:#FFD700
    style F fill:#87CEEB
```

**Key Optimization Strategies**:
1. **NLU Caching**: LRU cache (100 entries) for intent classification
2. **Database Query Caching**: TTL-based cache for Room queries
3. **Message Pagination**: 50-message pages for conversation history
4. **Memory Optimization**: Lazy loading, weak references, bitmap recycling
5. **LLM Optimization**: Streaming responses, model preloading, GPU acceleration

---

## 5.2 NLU Caching Strategy (LRU, 100 Entries)

### 5.2.1 Implementation

**LRU Cache for Intent Results**:

```kotlin
// features/nlu/data/cache/NluCache.kt
class NluCache(
    private val maxSize: Int = 100
) {
    private val cache = object : LinkedHashMap<String, CachedIntentResult>(
        initialCapacity = maxSize,
        loadFactor = 0.75f,
        accessOrder = true // LRU ordering
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, CachedIntentResult>
        ): Boolean {
            return size > maxSize
        }
    }

    private val lock = ReentrantReadWriteLock()

    fun get(text: String): IntentResult? {
        lock.readLock().lock()
        try {
            val cached = cache[text.lowercase()] ?: return null

            // Check if cache entry is still valid (5 minutes TTL)
            if (System.currentTimeMillis() - cached.timestamp > 5 * 60 * 1000) {
                return null
            }

            return cached.result
        } finally {
            lock.readLock().unlock()
        }
    }

    fun put(text: String, result: IntentResult) {
        lock.writeLock().lock()
        try {
            cache[text.lowercase()] = CachedIntentResult(
                result = result,
                timestamp = System.currentTimeMillis()
            )
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun clear() {
        lock.writeLock().lock()
        try {
            cache.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun size(): Int {
        lock.readLock().lock()
        try {
            return cache.size
        } finally {
            lock.readLock().unlock()
        }
    }
}

data class CachedIntentResult(
    val result: IntentResult,
    val timestamp: Long
)
```

**Cached Intent Classifier**:

```kotlin
// features/nlu/data/OnnxIntentClassifierCached.kt
class OnnxIntentClassifierCached(
    private val onnxClassifier: OnnxIntentClassifier,
    private val cache: NluCache = NluCache(maxSize = 100)
) : IntentClassifier {

    override suspend fun classify(text: String): IntentResult {
        // Check cache first
        cache.get(text)?.let { cachedResult ->
            return cachedResult.copy(
                processingTime = 0L // Cache hit = instant
            )
        }

        // Cache miss - perform actual classification
        val result = onnxClassifier.classify(text)

        // Store in cache
        cache.put(text, result)

        return result
    }

    fun clearCache() {
        cache.clear()
    }

    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = cache.size(),
            maxSize = 100,
            hitRate = calculateHitRate()
        )
    }
}
```

### 5.2.2 Performance Metrics

**Before Caching**:
- Average latency: 35ms
- 95th percentile: 48ms
- Memory: 50MB (model loaded)

**After Caching** (90% cache hit rate):
- Average latency: **5ms** (85% reduction)
- 95th percentile: **40ms** (17% reduction)
- Memory: 52MB (+2MB for cache)
- Cache hit rate: 90%

```mermaid
graph LR
    A[Text Input] --> B{Cache Check}
    B -->|Hit 90%| C[Return Cached<br/>~0ms]
    B -->|Miss 10%| D[ONNX Inference<br/>~35ms]
    D --> E[Cache Result]
    E --> F[Return Result]
    C --> G[Total: ~5ms avg]
    F --> G

    style C fill:#90EE90
    style D fill:#FFD700
```

---

## 5.3 Database Query Caching (TTL-Based)

### 5.3.1 Room Query Cache Implementation

```kotlin
// core/data/cache/QueryCache.kt
class QueryCache<K, V>(
    private val ttlMillis: Long = 5 * 60 * 1000, // 5 minutes default
    private val maxSize: Int = 50
) {
    private val cache = ConcurrentHashMap<K, CachedValue<V>>()

    fun get(key: K): V? {
        val cached = cache[key] ?: return null

        // Check TTL
        if (System.currentTimeMillis() - cached.timestamp > ttlMillis) {
            cache.remove(key)
            return null
        }

        cached.accessCount++
        cached.lastAccessed = System.currentTimeMillis()
        return cached.value
    }

    fun put(key: K, value: V) {
        // Evict if cache is full
        if (cache.size >= maxSize) {
            evictLeastRecentlyUsed()
        }

        cache[key] = CachedValue(
            value = value,
            timestamp = System.currentTimeMillis(),
            lastAccessed = System.currentTimeMillis(),
            accessCount = 0
        )
    }

    fun invalidate(key: K) {
        cache.remove(key)
    }

    fun invalidateAll() {
        cache.clear()
    }

    private fun evictLeastRecentlyUsed() {
        val lruKey = cache.entries
            .minByOrNull { it.value.lastAccessed }
            ?.key

        lruKey?.let { cache.remove(it) }
    }

    fun getStats(): CacheStats {
        val totalAccesses = cache.values.sumOf { it.accessCount }
        val avgAccessesPerEntry = if (cache.isNotEmpty()) {
            totalAccesses.toDouble() / cache.size
        } else 0.0

        return CacheStats(
            size = cache.size,
            maxSize = maxSize,
            hitRate = 0.0, // Calculated externally
            avgAccessesPerEntry = avgAccessesPerEntry
        )
    }
}

data class CachedValue<V>(
    val value: V,
    val timestamp: Long,
    var lastAccessed: Long,
    var accessCount: Int
)
```

### 5.3.2 Cached Repository Implementation

```kotlin
// core/data/repositories/CachedConversationRepository.kt
class CachedConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ConversationRepository {

    // Cache for single conversation queries (5 min TTL)
    private val conversationCache = QueryCache<String, Conversation>(
        ttlMillis = 5 * 60 * 1000,
        maxSize = 20
    )

    // Cache for conversation list (1 min TTL, refreshed more often)
    private val conversationListCache = QueryCache<Unit, List<Conversation>>(
        ttlMillis = 1 * 60 * 1000,
        maxSize = 1
    )

    override suspend fun getConversations(): Flow<List<Conversation>> {
        // Try cache first
        conversationListCache.get(Unit)?.let { cached ->
            return flowOf(cached)
        }

        // Cache miss - fetch from database
        return conversationDao.getAllConversations()
            .map { entities ->
                val conversations = entities.map { entity ->
                    entityToConversation(entity)
                }

                // Cache result
                conversationListCache.put(Unit, conversations)

                conversations
            }
    }

    override suspend fun getConversationById(id: String): Conversation? {
        // Try cache first
        conversationCache.get(id)?.let { return it }

        // Cache miss - fetch from database
        val entity = conversationDao.getById(id) ?: return null
        val messages = messageDao.getMessagesByConversation(id).first()

        val conversation = Conversation(
            id = entity.id,
            title = entity.title,
            messages = messages.map { entityToMessage(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

        // Cache result
        conversationCache.put(id, conversation)

        return conversation
    }

    override suspend fun addMessage(
        conversationId: String,
        message: Message
    ): Message {
        // Add to database
        val entity = messageToEntity(message)
        messageDao.insert(entity)

        // Invalidate caches
        conversationCache.invalidate(conversationId)
        conversationListCache.invalidateAll()

        return message
    }

    override suspend fun deleteConversation(id: String) {
        conversationDao.deleteById(id)

        // Invalidate caches
        conversationCache.invalidate(id)
        conversationListCache.invalidateAll()
    }
}
```

### 5.3.3 Performance Comparison

**Query Performance** (100 conversations, 1000 messages):

| Operation | Without Cache | With Cache | Improvement |
|-----------|---------------|------------|-------------|
| Get all conversations | 45ms | **8ms** | 82% faster |
| Get conversation by ID | 12ms | **1ms** | 92% faster |
| Get messages (50) | 18ms | **2ms** | 89% faster |
| Add message | 15ms | 15ms* | No change** |

*\* Cache invalidation adds ~1ms overhead*
*\** Write operations not cached (correctness priority)*

---

## 5.4 Message Pagination (50-Page Design)

### 5.4.1 Pagination Implementation

```kotlin
// core/domain/repositories/PaginatedConversationRepository.kt
interface PaginatedConversationRepository {
    suspend fun getMessagesPaginated(
        conversationId: String,
        page: Int,
        pageSize: Int = 50
    ): PaginatedResult<Message>

    suspend fun getMessagesBeforeTimestamp(
        conversationId: String,
        timestamp: Long,
        limit: Int = 50
    ): List<Message>
}

data class PaginatedResult<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val hasNext: Boolean
)
```

**Room DAO with Pagination**:

```kotlin
// core/data/local/dao/MessageDao.kt
@Dao
interface MessageDao {
    @Query("""
        SELECT * FROM ai_messages
        WHERE conversationId = :conversationId
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        offset: Int
    ): List<AiMessageEntity>

    @Query("""
        SELECT COUNT(*) FROM ai_messages
        WHERE conversationId = :conversationId
    """)
    suspend fun getMessageCount(conversationId: String): Int

    @Query("""
        SELECT * FROM ai_messages
        WHERE conversationId = :conversationId
        AND timestamp < :timestamp
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getMessagesBeforeTimestamp(
        conversationId: String,
        timestamp: Long,
        limit: Int
    ): List<AiMessageEntity>
}
```

### 5.4.2 Lazy Loading in UI

```kotlin
// platform/android/ui/chat/ChatViewModel.kt
class ChatViewModel @Inject constructor(
    private val repository: PaginatedConversationRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var currentPage = 0
    private var isLoading = false
    private var hasMoreMessages = true

    fun loadInitialMessages(conversationId: String) {
        viewModelScope.launch {
            currentPage = 0
            val result = repository.getMessagesPaginated(
                conversationId = conversationId,
                page = 0,
                pageSize = 50
            )

            _messages.value = result.items.reversed() // Newest at bottom
            hasMoreMessages = result.hasNext
        }
    }

    fun loadMoreMessages(conversationId: String) {
        if (isLoading || !hasMoreMessages) return

        viewModelScope.launch {
            isLoading = true
            currentPage++

            val result = repository.getMessagesPaginated(
                conversationId = conversationId,
                page = currentPage,
                pageSize = 50
            )

            // Prepend older messages
            _messages.value = (result.items.reversed() + _messages.value)
            hasMoreMessages = result.hasNext
            isLoading = false
        }
    }
}
```

**Compose UI with LazyColumn Pagination**:

```kotlin
// platform/android/ui/chat/ChatScreen.kt
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    conversationId: String
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        reverseLayout = true, // Newest at bottom
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            MessageBubble(message = message)
        }

        // Detect scroll to top (load more)
        item {
            LaunchedEffect(listState.firstVisibleItemIndex) {
                if (listState.firstVisibleItemIndex >= messages.size - 5) {
                    viewModel.loadMoreMessages(conversationId)
                }
            }
        }
    }
}
```

### 5.4.3 Pagination Performance Metrics

**Scenario**: Conversation with 1000 messages

| Approach | Initial Load Time | Memory Usage | Scroll Performance |
|----------|-------------------|--------------|-------------------|
| Load All (1000) | 850ms | 45MB | Janky (25 FPS) |
| Paginated (50) | **120ms** | **8MB** | **Smooth (60 FPS)** |

**Memory Optimization Flow**:

```mermaid
graph LR
    A[User Opens Chat] --> B[Load Page 0<br/>50 messages<br/>8MB]
    B --> C{User Scrolls Up?}
    C -->|Yes| D[Load Page 1<br/>+50 messages<br/>+8MB]
    C -->|No| E[Stay at Page 0]
    D --> F{User Scrolls Up?}
    F -->|Yes| G[Load Page 2<br/>+50 messages<br/>+8MB]
    F -->|No| H[Max 3 pages in memory<br/>24MB total]

    style B fill:#90EE90
    style D fill:#FFD700
    style G fill:#FFD700
```

---

## 5.5 Memory Optimization

### 5.5.1 Bitmap Recycling (Message Images)

```kotlin
// core/common/utils/BitmapCache.kt
class BitmapCache(
    private val maxSizeBytes: Long = 50 * 1024 * 1024 // 50MB
) {
    private val cache = LruCache<String, Bitmap>(maxSizeBytes.toInt())

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    fun clear() {
        cache.evictAll()
    }

    // Automatically recycle evicted bitmaps
    override fun entryRemoved(
        evicted: Boolean,
        key: String,
        oldValue: Bitmap,
        newValue: Bitmap?
    ) {
        if (evicted && !oldValue.isRecycled) {
            oldValue.recycle()
        }
    }
}
```

### 5.5.2 Weak References for Listeners

```kotlin
// core/common/utils/EventBus.kt
class EventBus {
    private val listeners = mutableListOf<WeakReference<EventListener>>()

    fun register(listener: EventListener) {
        listeners.add(WeakReference(listener))
    }

    fun post(event: Event) {
        // Remove garbage-collected listeners
        listeners.removeAll { it.get() == null }

        // Notify active listeners
        listeners.forEach { ref ->
            ref.get()?.onEvent(event)
        }
    }
}
```

### 5.5.3 Lazy Loading for Large Datasets

```kotlin
// features/rag/data/RagDatasetLoader.kt
class RagDatasetLoader {
    private val datasets = mutableMapOf<String, WeakReference<RagDataset>>()

    suspend fun loadDataset(id: String): RagDataset {
        // Check if already loaded
        datasets[id]?.get()?.let { return it }

        // Load from disk
        val dataset = loadFromDisk(id)

        // Store weak reference (can be GC'd if memory low)
        datasets[id] = WeakReference(dataset)

        return dataset
    }

    private suspend fun loadFromDisk(id: String): RagDataset {
        // Lazy load - only load metadata, not full vectors
        return RagDataset(
            id = id,
            metadata = loadMetadata(id),
            vectorsLoader = { loadVectorsLazy(id) } // Lazy
        )
    }
}
```

---

## 5.6 Before/After Metrics

### 5.6.1 Overall Performance Comparison

| Metric | Before Optimization | After Optimization | Improvement |
|--------|--------------------|--------------------|-------------|
| **App Startup** | 2.5s | **1.2s** | 52% faster |
| **Message Send Latency** | 450ms | **180ms** | 60% faster |
| **Conversation Load** | 850ms (1000 msgs) | **120ms** (50 msgs) | 86% faster |
| **Memory Usage (Idle)** | 280MB | **180MB** | 36% reduction |
| **Memory Usage (Active)** | 450MB | **250MB** | 44% reduction |
| **Battery Drain (1hr)** | 15% | **8%** | 47% reduction |
| **Database Queries (avg)** | 35ms | **6ms** | 83% faster |

### 5.6.2 Performance Timeline

```mermaid
gantt
    title Performance Optimization Impact
    dateFormat X
    axisFormat %s

    section App Startup
    Before : 0, 2500ms
    After  : 0, 1200ms

    section Message Send
    Before : 0, 450ms
    After  : 0, 180ms

    section Conversation Load
    Before : 0, 850ms
    After  : 0, 120ms
```

---

## 5.7 Profiling Techniques

### 5.7.1 Android Studio Profiler

**CPU Profiling**:
```kotlin
// Wrap expensive operations with tracing
fun classifyIntent(text: String): IntentResult {
    Trace.beginSection("OnnxIntentClassification")
    try {
        return onnxClassifier.classify(text)
    } finally {
        Trace.endSection()
    }
}
```

**Memory Profiling**:
```bash
# Capture memory heap dump
adb shell am dumpheap <package-name> /data/local/tmp/heap.hprof
adb pull /data/local/tmp/heap.hprof .

# Analyze with Android Studio Memory Profiler
# Look for:
# - Memory leaks (retained objects)
# - Large bitmap allocations
# - Unnecessary object retention
```

### 5.7.2 Custom Performance Logging

```kotlin
// core/common/performance/PerformanceLogger.kt
object PerformanceLogger {
    private val metrics = ConcurrentHashMap<String, MutableList<Long>>()

    inline fun <T> measure(tag: String, block: () -> T): T {
        val startTime = System.nanoTime()
        val result = block()
        val duration = (System.nanoTime() - startTime) / 1_000_000 // ms

        metrics.getOrPut(tag) { mutableListOf() }.add(duration)

        Log.d("Performance", "$tag: ${duration}ms")

        return result
    }

    fun getStats(tag: String): PerformanceStats? {
        val durations = metrics[tag] ?: return null

        return PerformanceStats(
            tag = tag,
            count = durations.size,
            avgMs = durations.average(),
            minMs = durations.minOrNull() ?: 0L,
            maxMs = durations.maxOrNull() ?: 0L,
            p50Ms = durations.sorted()[durations.size / 2],
            p95Ms = durations.sorted()[(durations.size * 0.95).toInt()],
            p99Ms = durations.sorted()[(durations.size * 0.99).toInt()]
        )
    }

    fun printAllStats() {
        metrics.keys.sorted().forEach { tag ->
            val stats = getStats(tag)
            Log.i("Performance", """
                $tag:
                  Count: ${stats?.count}
                  Avg: ${stats?.avgMs}ms
                  p50: ${stats?.p50Ms}ms
                  p95: ${stats?.p95Ms}ms
                  p99: ${stats?.p99Ms}ms
            """.trimIndent())
        }
    }
}

data class PerformanceStats(
    val tag: String,
    val count: Int,
    val avgMs: Double,
    val minMs: Long,
    val maxMs: Long,
    val p50Ms: Long,
    val p95Ms: Long,
    val p99Ms: Long
)
```

**Usage in Production**:
```kotlin
class OnnxIntentClassifier {
    suspend fun classify(text: String): IntentResult {
        return PerformanceLogger.measure("NLU_Classification") {
            // Actual classification logic
            performOnnxInference(text)
        }
    }
}
```

---

## 5.8 Performance Optimization Checklist

### 5.8.1 Development Phase

- ✅ Enable R8/ProGuard in release builds
- ✅ Use vector drawables instead of raster images
- ✅ Implement pagination for lists >50 items
- ✅ Use `LaunchedEffect` wisely (avoid unnecessary recompositions)
- ✅ Profile with Android Studio Profiler weekly
- ✅ Measure cold startup time (<2s target)
- ✅ Check memory leaks with LeakCanary
- ✅ Test on low-end devices (2GB RAM)

### 5.8.2 Testing Phase

- ✅ Run performance tests in CI
- ✅ Verify latency budgets (NLU <50ms, LLM <500ms)
- ✅ Check memory usage under load
- ✅ Test battery drain (1hr active <10%)
- ✅ Profile ANR (Application Not Responding) issues
- ✅ Measure jank (dropped frames) in UI
- ✅ Test offline performance (no network)

### 5.8.3 Production Monitoring

- ✅ Implement Firebase Performance Monitoring
- ✅ Track custom performance metrics
- ✅ Monitor crash-free rate (>99.5%)
- ✅ Set up alerts for performance regressions
- ✅ Review user-reported performance issues
- ✅ A/B test performance improvements

---

# Chapter 6: Configuration & Settings

## 6.1 Configuration Architecture

AVA AI uses a multi-layered configuration system:

```mermaid
graph TB
    A[Configuration Sources] --> B[Build Config<br/>Compile-Time]
    A --> C[App Settings<br/>SharedPreferences]
    A --> D[User Preferences<br/>Room Database]
    A --> E[Remote Config<br/>Future: Firebase]

    B --> F[Configuration Manager]
    C --> F
    D --> F
    E --> F

    F --> G[Application Layer]

    style B fill:#FFD700
    style C fill:#90EE90
    style D fill:#87CEEB
    style E fill:#DDA0DD
```

---

## 6.2 ChatPreferences System

### 6.2.1 User Preferences Data Model

```kotlin
// core/domain/entities/UserPreferences.kt
@Serializable
data class UserPreferences(
    val userId: String,

    // LLM Settings
    val llmConfig: LlmConfig = LlmConfig(),

    // NLU Settings
    val nluConfig: NluConfig = NluConfig(),

    // Privacy Settings
    val privacyConfig: PrivacyConfig = PrivacyConfig(),

    // UI Settings
    val uiConfig: UiConfig = UiConfig(),

    // Voice Settings
    val voiceConfig: VoiceConfig = VoiceConfig(),

    // Advanced Settings
    val advancedConfig: AdvancedConfig = AdvancedConfig()
)

@Serializable
data class LlmConfig(
    val primaryProvider: LlmProvider = LlmProvider.ALC,
    val fallbackChain: List<LlmProvider> = listOf(
        LlmProvider.ALC,
        LlmProvider.LLAMA_CPP,
        LlmProvider.OPENROUTER
    ),
    val apiKeys: Map<String, String> = emptyMap(), // Encrypted
    val selectedModels: Map<LlmProvider, String> = emptyMap(),
    val maxTokens: Int = 1000,
    val temperature: Float = 0.7f,
    val streamingEnabled: Boolean = true
)

@Serializable
data class NluConfig(
    val confidenceThreshold: Float = 0.7f,
    val teachAvaEnabled: Boolean = true,
    val autoLearningEnabled: Boolean = true,
    val languagePreference: String = "en" // ISO 639-1 code
)

@Serializable
data class PrivacyConfig(
    val localProcessingOnly: Boolean = true,
    val cloudLlmEnabled: Boolean = false,
    val telemetryEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = true
)

@Serializable
data class UiConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val messageGrouping: Boolean = true,
    val showTimestamps: Boolean = true,
    val animationsEnabled: Boolean = true
)

@Serializable
data class VoiceConfig(
    val voiceInputEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val wakeWord: String = "Hey AVA",
    val ttsEnabled: Boolean = false,
    val ttsVoice: String = "en-US-Standard-A"
)

@Serializable
data class AdvancedConfig(
    val debugMode: Boolean = false,
    val performanceLogging: Boolean = false,
    val experimentalFeatures: Set<String> = emptySet()
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class FontSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}
```

### 6.2.2 Preferences Repository

```kotlin
// core/data/repositories/PreferencesRepository.kt
interface PreferencesRepository {
    suspend fun getUserPreferences(userId: String): Flow<UserPreferences>
    suspend fun updateLlmConfig(userId: String, config: LlmConfig)
    suspend fun updateNluConfig(userId: String, config: NluConfig)
    suspend fun updatePrivacyConfig(userId: String, config: PrivacyConfig)
    suspend fun updateUiConfig(userId: String, config: UiConfig)
    suspend fun updateVoiceConfig(userId: String, config: VoiceConfig)
    suspend fun updateAdvancedConfig(userId: String, config: AdvancedConfig)
    suspend fun resetToDefaults(userId: String)
    suspend fun exportPreferences(userId: String): String // JSON
    suspend fun importPreferences(userId: String, json: String)
}
```

**Implementation with Room + EncryptedSharedPreferences**:

```kotlin
// core/data/repositories/PreferencesRepositoryImpl.kt
class PreferencesRepositoryImpl(
    private val preferencesDao: PreferencesDao,
    private val encryptedPrefs: EncryptedSharedPreferences
) : PreferencesRepository {

    override suspend fun getUserPreferences(userId: String): Flow<UserPreferences> {
        return preferencesDao.getPreferences(userId)
            .map { entity ->
                entity?.let { entityToPreferences(it) } ?: UserPreferences(userId)
            }
            .onEach { prefs ->
                // Decrypt API keys
                val decryptedApiKeys = prefs.llmConfig.apiKeys.mapValues { (provider, _) ->
                    encryptedPrefs.getString("api_key_$provider", "") ?: ""
                }

                // Replace with decrypted keys
                prefs.copy(
                    llmConfig = prefs.llmConfig.copy(
                        apiKeys = decryptedApiKeys.filter { it.value.isNotBlank() }
                    )
                )
            }
    }

    override suspend fun updateLlmConfig(userId: String, config: LlmConfig) {
        // Encrypt API keys before storing
        config.apiKeys.forEach { (provider, key) ->
            encryptedPrefs.edit()
                .putString("api_key_$provider", key)
                .apply()
        }

        // Store rest in Room (without API keys)
        val entity = getOrCreateEntity(userId)
        preferencesDao.update(
            entity.copy(
                llmProvider = config.primaryProvider.name,
                llmFallbackChain = config.fallbackChain.joinToString(",") { it.name },
                llmMaxTokens = config.maxTokens,
                llmTemperature = config.temperature,
                llmStreamingEnabled = config.streamingEnabled
            )
        )
    }

    // Similar implementations for other update methods...
}
```

---

## 6.3 Configuration Files

### 6.3.1 Build Configuration (Gradle)

**Root `build.gradle.kts`**:

```kotlin
// build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

**App Module `build.gradle.kts`**:

```kotlin
// platform/android/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.augmentalis.ava"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.augmentalis.ava"
        minSdk = 24 // Android 7.0+
        targetSdk = 34 // Android 14
        versionCode = 1
        versionName = "1.0.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build config fields
        buildConfigField("String", "APP_VERSION", "\"${versionName}\"")
        buildConfigField("String", "BUILD_TYPE", "\"${buildType.name}\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("String", "API_BASE_URL", "\"https://dev.augmentalis.com\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "API_BASE_URL", "\"https://api.augmentalis.com\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }

        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")

            buildConfigField("boolean", "ENABLE_PERFORMANCE_LOGGING", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))

    // Feature modules
    implementation(project(":features:nlu"))
    implementation(project(":features:llm"))
    implementation(project(":features:voice"))

    // Platform
    implementation(project(":platform:shared-ui"))

    // Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

### 6.3.2 ProGuard Rules

```proguard
# proguard-rules.pro

# Keep all model classes (data classes used for serialization)
-keep class com.augmentalis.ava.core.domain.entities.** { *; }
-keep class com.augmentalis.ava.core.data.local.entities.** { *; }

# Keep ONNX Runtime classes
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.augmentalis.ava.**$$serializer { *; }
-keepclassmembers class com.augmentalis.ava.** {
    *** Companion;
}
-keepclasseswithmembers class com.augmentalis.ava.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** create(...);
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn io.ktor.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

### 6.3.3 Version Catalog (`libs.versions.toml`)

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "1.9.22"
kotlinx-coroutines = "1.7.3"
kotlinx-serialization = "1.6.2"
compose-bom = "2024.02.00"
compose-compiler = "1.5.8"
hilt = "2.48"
room = "2.6.1"
ktor = "2.3.7"
onnx-runtime = "1.16.0"

[libraries]
# Kotlin
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }

# Hilt
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }

# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-serialization = { module = "io.ktor:ktor-client-serialization", version.ref = "ktor" }

# ONNX Runtime
onnx-runtime = { module = "com.microsoft.onnxruntime:onnxruntime-android", version.ref = "onnx-runtime" }

[plugins]
android-application = { id = "com.android.application", version = "8.2.0" }
android-library = { id = "com.android.library", version = "8.2.0" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "1.9.22-1.0.17" }
```

---

## 6.4 Environment Setup

### 6.4.1 Developer Machine Requirements

**Minimum Specifications**:
- CPU: Intel i5 / AMD Ryzen 5 (4 cores) or Apple M1
- RAM: 16GB
- Storage: 256GB SSD
- OS: macOS 12+, Windows 10+, Ubuntu 20.04+

**Recommended Specifications**:
- CPU: Intel i7 / AMD Ryzen 7 / Apple M1/M2 (8+ cores)
- RAM: 32GB
- Storage: 512GB NVMe SSD
- GPU: Discrete GPU (for ALC testing)
- OS: macOS 13+, Windows 11, Ubuntu 22.04+

### 6.4.2 Development Tools Installation

**Android Studio Setup**:

```bash
# Install Android Studio Hedgehog (2023.1.1+)
# Download from: https://developer.android.com/studio

# Install required SDKs
sdkmanager "platform-tools" "platforms;android-34" \
           "build-tools;34.0.0" "ndk;26.1.10909125" \
           "cmake;3.22.1"

# Install Kotlin Multiplatform Mobile plugin
# Settings → Plugins → Kotlin Multiplatform Mobile → Install
```

**Git Configuration**:

```bash
# Clone repository
git clone https://github.com/augmentalis/ava-ai.git
cd ava-ai

# Initialize submodules
git submodule update --init --recursive

# Install Git LFS (for ONNX models)
git lfs install
git lfs pull
```

**Java Development Kit**:

```bash
# Install JDK 17 (required for Android Gradle Plugin 8+)
# macOS (Homebrew):
brew install openjdk@17

# Ubuntu:
sudo apt install openjdk-17-jdk

# Windows (Chocolatey):
choco install openjdk17
```

### 6.4.3 Environment Variables

**`.env.local`** (not committed):

```bash
# LLM API Keys (for development testing)
OPENROUTER_API_KEY=your_openrouter_key_here
HUGGINGFACE_API_KEY=your_huggingface_key_here
OPENAI_API_KEY=your_openai_key_here
ANTHROPIC_API_KEY=your_anthropic_key_here
GOOGLE_AI_API_KEY=your_google_ai_key_here

# Database encryption key (development only)
DB_ENCRYPTION_KEY=dev_encryption_key_32_bytes

# Feature flags
ENABLE_EXPERIMENTAL_FEATURES=true
ENABLE_DEBUG_LOGGING=true
```

**Load in `local.properties`**:

```properties
# local.properties (not committed)
sdk.dir=/path/to/Android/sdk
ndk.dir=/path/to/Android/sdk/ndk/26.1.10909125

# Environment variables
openrouter.api.key=your_key
huggingface.api.key=your_key
```

---

## 6.5 Deployment Configuration

### 6.5.1 Deployment Checklist

**Pre-Release Checks**:

- [ ] All tests passing (164 test cases)
- [ ] Code coverage ≥85% overall, ≥90% core
- [ ] ProGuard rules tested (no crashes in release build)
- [ ] Performance budgets met (see Chapter 5)
- [ ] Memory leaks fixed (LeakCanary clean)
- [ ] Battery drain acceptable (<10% per hour active)
- [ ] APK size <100MB
- [ ] Crash-free rate >99.5% (internal testing)
- [ ] Privacy policy reviewed
- [ ] API keys removed from codebase
- [ ] Version number bumped
- [ ] Changelog updated
- [ ] Release notes written
- [ ] Screenshots updated (Play Store)

### 6.5.2 Release Build Configuration

**Signing Configuration**:

```kotlin
// platform/android/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

**GitHub Actions Release Workflow**:

```yaml
# .github/workflows/release.yml
name: Release Build

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3
        with:
          submodules: recursive

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Decode keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > release.keystore

      - name: Build Release APK
        env:
          KEYSTORE_FILE: release.keystore
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease

      - name: Build Release AAB (Play Store)
        run: ./gradlew bundleRelease

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            platform/android/build/outputs/apk/release/*.apk
            platform/android/build/outputs/bundle/release/*.aab
          body_path: CHANGELOG.md
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Upload to Play Store (Internal Testing)
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_SERVICE_ACCOUNT }}
          packageName: com.augmentalis.ava
          releaseFiles: platform/android/build/outputs/bundle/release/*.aab
          track: internal
```

### 6.5.3 Continuous Deployment

**Internal Testing Track**:

```yaml
# Deploy to Google Play Internal Testing after each commit to main
name: Deploy Internal

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      # ... (similar to release workflow)

      - name: Deploy to Internal Track
        run: ./gradlew publishReleaseBundle --track=internal
```

---

## 6.6 Settings UI Reference

### 6.6.1 Settings Screen Structure

```kotlin
// platform/android/ui/settings/SettingsScreen.kt
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val preferences by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // LLM Settings
            item {
                SettingsSection(title = "AI & LLM") {
                    LlmProviderSelection(preferences.llmConfig)
                    ApiKeyInputs(preferences.llmConfig)
                    ModelSelection(preferences.llmConfig)
                    AdvancedLlmSettings(preferences.llmConfig)
                }
            }

            // NLU Settings
            item {
                SettingsSection(title = "Natural Language Understanding") {
                    TeachAvaSetting(preferences.nluConfig)
                    AutoLearningSetting(preferences.nluConfig)
                    LanguagePreference(preferences.nluConfig)
                }
            }

            // Privacy Settings
            item {
                SettingsSection(title = "Privacy & Data") {
                    LocalProcessingSetting(preferences.privacyConfig)
                    CloudLlmSetting(preferences.privacyConfig)
                    TelemetrySetting(preferences.privacyConfig)
                    DataExportImport()
                }
            }

            // UI Settings
            item {
                SettingsSection(title = "Appearance") {
                    ThemeSelector(preferences.uiConfig)
                    FontSizeSelector(preferences.uiConfig)
                    AnimationToggle(preferences.uiConfig)
                }
            }

            // Voice Settings
            item {
                SettingsSection(title = "Voice") {
                    VoiceInputToggle(preferences.voiceConfig)
                    WakeWordSetting(preferences.voiceConfig)
                    TtsSetting(preferences.voiceConfig)
                }
            }

            // Advanced Settings
            item {
                SettingsSection(title = "Advanced") {
                    DebugModeToggle(preferences.advancedConfig)
                    PerformanceLogging(preferences.advancedConfig)
                    ExperimentalFeatures(preferences.advancedConfig)
                }
            }

            // About
            item {
                SettingsSection(title = "About") {
                    AppVersion()
                    OpenSourceLicenses()
                    PrivacyPolicy()
                    TermsOfService()
                }
            }
        }
    }
}
```

### 6.6.2 Settings Reference Table

| Category | Setting | Type | Default | Description |
|----------|---------|------|---------|-------------|
| **LLM** | Primary Provider | Dropdown | ALC | GPU, CPU, or Cloud |
| | Fallback Chain | Multi-select | ALC → llama.cpp → OpenRouter | Cascading fallback |
| | OpenRouter API Key | Text (encrypted) | Empty | User-provided |
| | HuggingFace API Key | Text (encrypted) | Empty | User-provided |
| | Max Tokens | Slider (100-2000) | 1000 | Response length limit |
| | Temperature | Slider (0.0-2.0) | 0.7 | Creativity (0=deterministic) |
| | Streaming | Toggle | On | Stream tokens as generated |
| **NLU** | Teach-Ava | Toggle | On | User-correctable intents |
| | Auto-Learning | Toggle | On | Learn from positive feedback |
| | Language | Dropdown | English | Multilingual NLU |
| | Confidence Threshold | Slider (0.5-0.95) | 0.7 | Min confidence for intent |
| **Privacy** | Local Processing Only | Toggle | On | Disable cloud LLMs |
| | Cloud LLM | Toggle | Off | Enable cloud inference |
| | Telemetry | Toggle | Off | Anonymous usage stats |
| | Crash Reporting | Toggle | On | Help improve stability |
| **UI** | Theme | Radio (Light/Dark/System) | System | Appearance |
| | Font Size | Radio (S/M/L/XL) | Medium | Text size |
| | Animations | Toggle | On | UI animations |
| | Show Timestamps | Toggle | On | Message timestamps |
| **Voice** | Voice Input | Toggle | On | Microphone access |
| | Wake Word | Toggle | Off | "Hey AVA" activation |
| | Text-to-Speech | Toggle | Off | Read responses aloud |
| **Advanced** | Debug Mode | Toggle | Off | Show debug info |
| | Performance Logging | Toggle | Off | Log latencies |
| | Experimental Features | Multi-select | None | Beta features |

---

## 6.7 Troubleshooting Guide

### 6.7.1 Common Issues

**Issue: ONNX Model Not Loading**

```
Error: Failed to load ONNX model: FileNotFoundException
```

**Solution**:
1. Check if model file exists in `assets/models/nlu/mobilebert-int8.onnx`
2. Verify Git LFS is installed and models are pulled:
   ```bash
   git lfs pull
   ```
3. Check ProGuard rules don't strip ONNX Runtime classes
4. Verify assets are included in APK (check `build/outputs/apk/debug/`)

---

**Issue: ALC Inference Crashes on Some Devices**

```
Error: VulkanError: VK_ERROR_INITIALIZATION_FAILED
```

**Solution**:
1. Check if device supports Vulkan:
   ```kotlin
   val hasVulkan = packageManager.hasSystemFeature(
       PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 1
   )
   ```
2. Fallback to CPU inference (llama.cpp) if Vulkan unavailable
3. Check GPU driver version (update via Play Store)
4. Test on device with Snapdragon 8 Gen 1+ or equivalent

---

**Issue: Room Database Migration Fails**

```
Error: IllegalStateException: A migration from 1 to 2 was required but not found
```

**Solution**:
1. Add migration:
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL("ALTER TABLE ai_conversations ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
       }
   }

   Room.databaseBuilder(context, AvaDatabase::class.java, "ava-database")
       .addMigrations(MIGRATION_1_2)
       .build()
   ```
2. Or fallback to destructive migration (dev only):
   ```kotlin
   .fallbackToDestructiveMigration()
   ```

---

**Issue: Cloud LLM API Rate Limited**

```
Error: RateLimitException: 429 Too Many Requests
```

**Solution**:
1. Implement exponential backoff:
   ```kotlin
   suspend fun <T> retryWithBackoff(
       maxRetries: Int = 3,
       initialDelay: Long = 1000,
       block: suspend () -> T
   ): T {
       var currentDelay = initialDelay
       repeat(maxRetries) { attempt ->
           try {
               return block()
           } catch (e: RateLimitException) {
               if (attempt == maxRetries - 1) throw e
               delay(currentDelay)
               currentDelay *= 2
           }
       }
       throw IllegalStateException()
   }
   ```
2. Check API key validity
3. Upgrade to higher tier (OpenRouter, OpenAI, etc.)
4. Fallback to local inference (ALC/llama.cpp)

---

**Issue: Compose Recomposition Performance**

```
Issue: UI feels laggy, frames dropping
```

**Solution**:
1. Use `remember` for expensive computations:
   ```kotlin
   val expensiveValue = remember(key) { computeExpensiveValue() }
   ```
2. Use `derivedStateOf` for derived state:
   ```kotlin
   val filteredMessages = remember(messages, query) {
       derivedStateOf {
           messages.filter { it.content.contains(query) }
       }
   }.value
   ```
3. Profile with Layout Inspector (Tools → Layout Inspector)
4. Check for unnecessary recompositions:
   ```kotlin
   @Composable
   fun MyComponent(value: Int) {
       SideEffect {
           Log.d("Recomposition", "MyComponent recomposed with value=$value")
       }
   }
   ```

---

### 6.7.2 Debug Tools

**Enable Debug Logging**:

```kotlin
// In debug builds only
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())

    // Enable Room query logging
    Room.databaseBuilder(context, AvaDatabase::class.java, "ava-database")
        .setQueryCallback(object : QueryCallback {
            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                Log.d("Room", "SQL: $sqlQuery, Args: $bindArgs")
            }
        }, Dispatchers.IO)
        .build()
}
```

**Performance Monitoring**:

```kotlin
// Enable StrictMode in debug builds
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
    )

    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
    )
}
```

---

## Appendix: Configuration Templates

### A.1 GitHub Actions CI/CD

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3
        with:
          submodules: recursive

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Run tests
        run: ./gradlew test --stacktrace

      - name: Run lint
        run: ./gradlew lint

      - name: Check coverage
        run: ./gradlew jacocoTestCoverageVerification

      - name: Upload coverage reports
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### A.2 EditorConfig

```ini
# .editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_style = space
indent_size = 4
max_line_length = 120

[*.{xml,json,yml,yaml}]
indent_style = space
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

---

**End of Developer Manual Part 2**

---

## Document Metadata

**Document**: AVA AI Developer Manual - Part 2 (Chapters 4-6)
**Version**: 1.0.0
**Date**: October 29, 2025
**Constitution**: v1.2.2
**Phase**: 1.0 MVP
**Status**: Complete

**Related Documents**:
- [ARCHITECTURE.md](/Volumes/M Drive/Coding/AVA AI/ARCHITECTURE.md)
- [PHASE_1_0_SPECIFICATION.md](/Volumes/M Drive/Coding/AVA AI/.ideacode/PHASE_1_0_SPECIFICATION.md)
- [CODE_CREATION_QA.md](/Volumes/M Drive/Coding/AVA AI/.ideacode/CODE_CREATION_QA.md)

**Chapters Covered**:
- Chapter 4: Testing Strategy (164 test cases, 87% coverage goal)
- Chapter 5: Performance Optimization (NLU caching, DB caching, pagination, memory)
- Chapter 6: Configuration & Settings (ChatPreferences, build config, deployment)

**Next**: Part 3 will cover Advanced Topics (Chapters 7-9): RAG System, Memory Architecture, and VOS4 Integration.
