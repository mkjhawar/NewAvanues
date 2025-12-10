# Developer Manual - Chapter 32: Testing Strategy for AVA

**Version**: 1.0
**Date**: 2025-11-12
**Author**: AVA Development Team

---

## Overview

This chapter documents AVA's comprehensive testing strategy, covering unit tests, instrumented tests, and test organization. Based on lessons learned from Feature 006 (Chat UI), this guide ensures consistent, maintainable test coverage across all modules.

---

## Table of Contents

1. [Testing Philosophy](#testing-philosophy)
2. [Test Types](#test-types)
3. [Test Organization](#test-organization)
4. [Unit Testing Guidelines](#unit-testing-guidelines)
5. [Instrumented Testing Guidelines](#instrumented-testing-guidelines)
6. [MockK Best Practices](#mockk-best-practices)
7. [Testing Android Components](#testing-android-components)
8. [Coverage Requirements](#coverage-requirements)
9. [Continuous Integration](#continuous-integration)
10. [Common Patterns](#common-patterns)

---

## 1. Testing Philosophy

### Core Principles

1. **Test Business Logic First**: Pure Kotlin logic (no Android dependencies) gets unit tests
2. **Android Components Need Context**: ViewModels, Activities require instrumented tests
3. **Fast Feedback**: Unit tests run in <5s, instrumented tests in <30s
4. **Maintainability**: Tests should be as readable as production code
5. **Coverage Quality > Quantity**: 80% meaningful coverage beats 100% shallow coverage

### Test Pyramid

```
        /\
       /  \    E2E Tests (10%)
      /----\
     /      \  Instrumented Tests (30%)
    /--------\
   /          \
  /____________\ Unit Tests (60%)
```

**Recommended Distribution**:
- **60% Unit Tests**: Fast, isolated, pure logic
- **30% Instrumented Tests**: Android components, UI
- **10% E2E Tests**: Full user flows

---

## 2. Test Types

### Unit Tests (`src/test/`)

**Purpose**: Test pure Kotlin logic without Android framework

**When to Use**:
- ✅ Data classes, sealed classes
- ✅ Repository interfaces
- ✅ Business logic (IntentTemplates, calculations)
- ✅ Utility functions
- ✅ Extension functions

**When NOT to Use**:
- ❌ Android ViewModels (need Context)
- ❌ Activities, Fragments
- ❌ Jetpack Compose UI
- ❌ Database operations (Room)

**Example**: IntentTemplatesTest (19/19 pass ✅)

```kotlin
class IntentTemplatesTest {
    @Test
    fun `getResponse returns correct template for control_lights`() {
        val response = IntentTemplates.getResponse("control_lights")
        assertEquals("I'll control the lights for you.", response)
    }
}
```

### Instrumented Tests (`src/androidTest/`)

**Purpose**: Test Android components with real Android framework

**When to Use**:
- ✅ ViewModels with @ApplicationContext
- ✅ Hilt dependency injection
- ✅ Room database operations
- ✅ Jetpack Compose UI
- ✅ Activities, Fragments
- ✅ Content Providers

**When NOT to Use**:
- ❌ Pure Kotlin logic (use unit tests)
- ❌ Network calls (mock in unit tests)
- ❌ Time-consuming operations

**Example**: ChatViewModelTest (should be instrumented)

```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ChatViewModelTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Test
    fun `sendMessage creates user message`() = runTest {
        // Test with real Android Context
    }
}
```

---

## 3. Test Organization

### Directory Structure

```
Universal/AVA/Features/Chat/
├── src/
│   ├── main/kotlin/              # Production code
│   ├── test/kotlin/               # Unit tests (JVM)
│   │   └── com/augmentalis/ava/features/chat/
│   │       ├── data/              # Pure logic tests
│   │       │   └── IntentTemplatesTest.kt  ✅ 19/19 pass
│   │       └── ui/                # ViewModel tests (WRONG - move below)
│   └── androidTest/kotlin/        # Instrumented tests (Android)
│       └── com/augmentalis/ava/features/chat/
│           ├── ui/                # ViewModel, UI tests
│           │   ├── ChatViewModelTest.kt
│           │   └── ChatScreenTest.kt
│           └── data/              # Room database tests
```

### Package Naming

**Rule**: Test packages mirror production packages

```
Production:  com.augmentalis.ava.features.chat.data.IntentTemplates
Test:        com.augmentalis.ava.features.chat.data.IntentTemplatesTest
```

---

## 4. Unit Testing Guidelines

### Writing Effective Unit Tests

#### Structure: Given-When-Then

```kotlin
@Test
fun `getAllTemplates returns all templates`() {
    // GIVEN: Initial state
    val templates = IntentTemplates.getAllTemplates()

    // WHEN: Action occurs
    val count = templates.size

    // THEN: Verify outcome
    assertEquals(17, count)
}
```

#### Test Naming

**Pattern**: \`description in backticks\` or camelCase

```kotlin
// Good: Descriptive, action-focused
@Test
fun `getResponse returns unknown template for empty string`()

// Bad: Vague, implementation-focused
@Test
fun testGetResponse()
```

#### Arrange-Act-Assert

```kotlin
@Test
fun `confidence below threshold triggers teach mode`() = runTest {
    // ARRANGE
    val confidence = 0.49f
    coEvery { mockIntentClassifier.classifyIntent(any(), any()) } returns
        Result.Success(IntentClassification(intent = "test", confidence = confidence, inferenceTimeMs = 50L))

    // ACT
    viewModel.sendMessage("Test utterance")
    kotlinx.coroutines.delay(100)

    // ASSERT
    assertTrue("Confidence 0.49 should trigger teach mode", confidence <= 0.5f)
}
```

---

## 5. Instrumented Testing Guidelines

### Setup with Hilt

```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ChatViewModelInstrumentedTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var chatPreferences: ChatPreferences

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `ChatViewModel initializes with real context`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = ChatViewModel(
            context = context,
            // ... inject real dependencies
        )
        assertNotNull(viewModel.activeConversationId.first())
    }
}
```

### Compose UI Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ChatScreen displays message bubbles`() {
        composeTestRule.setContent {
            ChatScreen(/* ... */)
        }

        composeTestRule.onNodeWithText("Hello AVA").assertExists()
    }
}
```

---

## 6. MockK Best Practices

### Mocking Interfaces

```kotlin
@Before
fun setup() {
    mockConversationRepo = mockk(relaxed = true)
    mockMessageRepo = mockk(relaxed = true)

    // Setup specific behaviors
    coEvery { mockMessageRepo.getMessagesForConversation(any()) } returns flowOf(emptyList())
}
```

### Mocking Classes with Properties

**Problem**: MockK can't mock `val` properties with `relaxed = true`

**Solution**: Use builder syntax

```kotlin
// ❌ Wrong - fails with "Missing mocked calls"
mockChatPreferences = mockk(relaxed = true)
every { mockChatPreferences.conversationMode } returns MutableStateFlow(ConversationMode.APPEND)

// ✅ Correct - use builder
mockChatPreferences = mockk {
    every { conversationMode } returns MutableStateFlow(ConversationMode.APPEND)
    every { confidenceThreshold } returns MutableStateFlow(0.5f)
    every { getLastActiveConversationId() } returns null
}
```

### Verifying Calls

```kotlin
@Test
fun `sendMessage triggers NLU classification`() = runTest {
    viewModel.sendMessage("Turn on the lights")

    coVerify { mockIntentClassifier.classifyIntent("Turn on the lights", any()) }
}
```

---

## 7. Testing Android Components

### Why ChatViewModel Tests Fail in Unit Tests

**Error**: `java.lang.RuntimeException: Method getMainLooper in android.os.Looper not mocked`

**Cause**: ChatViewModel requires Android Context for:
1. `@ApplicationContext` injection
2. Hilt dependency injection
3. StateFlow/Flow (uses Android Looper)
4. Coroutine dispatchers

**Solution**: Move to `src/androidTest/` as instrumented tests

### Before (Unit Test - Fails ❌)

```kotlin
// src/test/kotlin/.../ChatViewModelTest.kt
class ChatViewModelTest {
    @Test
    fun `initialization loads most recent conversation`() = runTest {
        // FAILS: Looper.getMainLooper() not mocked
        val viewModel = ChatViewModel(mockContext, ...)
    }
}
```

### After (Instrumented Test - Works ✅)

```kotlin
// src/androidTest/kotlin/.../ChatViewModelTest.kt
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ChatViewModelTest {
    @Test
    fun `initialization loads most recent conversation`() = runTest {
        // WORKS: Real Android Context available
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = ChatViewModel(context, ...)
    }
}
```

---

## 8. Coverage Requirements

### Target Coverage

| Component | Target | Actual (Feature 006) |
|-----------|--------|----------------------|
| **Business Logic** | 90%+ | 100% ✅ (IntentTemplates) |
| **ViewModels** | 80%+ | Pending (instrumented tests) |
| **UI Components** | 70%+ | Pending |
| **Overall** | 80%+ | 53% (19/36 passing) |

### Measuring Coverage

#### Unit Test Coverage

```bash
./gradlew :Universal:AVA:Features:Chat:testDebugUnitTestCoverage
open Universal/AVA/Features/Chat/build/reports/coverage/test/debug/index.html
```

#### Instrumented Test Coverage

```bash
./gradlew :Universal:AVA:Features:Chat:createDebugCoverageReport
open Universal/AVA/Features/Chat/build/reports/coverage/androidTest/debug/index.html
```

#### Combined Coverage

```bash
./gradlew :Universal:AVA:Features:Chat:jacocoTestReport
```

---

## 9. Continuous Integration

### Running Tests in CI/CD

```yaml
# .gitlab-ci.yml
test:unit:
  stage: test
  script:
    - ./gradlew testDebugUnitTest
  artifacts:
    reports:
      junit: '**/build/test-results/test*UnitTest/**.xml'

test:instrumented:
  stage: test
  script:
    - ./gradlew connectedDebugAndroidTest
  artifacts:
    reports:
      junit: '**/build/outputs/androidTest-results/connected/**.xml'
```

### Quality Gates

**Block Merge if**:
- ❌ Unit tests fail
- ❌ Coverage drops below 80%
- ❌ Critical bugs detected

**Warn (Don't Block) if**:
- ⚠️ Instrumented tests fail (device-dependent)
- ⚠️ Coverage drops below 90%

---

## 10. Common Patterns

### Testing Repositories

```kotlin
class MessageRepositoryTest {
    private lateinit var database: AVADatabase
    private lateinit var repository: MessageRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AVADatabase::class.java).build()
        repository = MessageRepositoryImpl(database.messageDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `addMessage inserts message successfully`() = runTest {
        val message = Message(
            id = "msg-1",
            conversationId = "conv-1",
            role = MessageRole.USER,
            content = "Hello",
            timestamp = System.currentTimeMillis()
        )

        val result = repository.addMessage(message)

        assertTrue(result is Result.Success)
    }
}
```

### Testing Flows

```kotlin
@Test
fun `messages flow emits updates`() = runTest {
    val messages = repository.getMessagesForConversation("conv-1")

    messages.test {
        // Initial state
        assertEquals(emptyList(), awaitItem())

        // Add message
        repository.addMessage(testMessage)

        // Verify emission
        assertEquals(listOf(testMessage), awaitItem())

        cancelAndIgnoreRemainingEvents()
    }
}
```

### Testing StateFlow

```kotlin
@Test
fun `viewModel state updates correctly`() = runTest {
    viewModel.activeConversationId.test {
        // Initial state
        assertNull(awaitItem())

        // Trigger state change
        viewModel.loadConversation("conv-1")

        // Verify new state
        assertEquals("conv-1", awaitItem())

        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## Summary

### Key Takeaways

1. ✅ **Pure Logic → Unit Tests**: IntentTemplates, utilities (fast, isolated)
2. ✅ **Android Components → Instrumented Tests**: ViewModels, UI (real Context)
3. ✅ **MockK Properties**: Use builder syntax `mockk { every { prop } returns ... }`
4. ✅ **Coverage**: Target 80%+ overall, 90%+ for business logic
5. ✅ **Test Organization**: Match production package structure

### Success Story: Feature 006

**IntentTemplatesTest**: **19/19 PASS** ✅
- Pure Kotlin logic
- No Android dependencies
- Fast execution (< 1s)
- 100% coverage

**Lesson**: Separate business logic from Android framework for better testability.

---

**Next**: [Chapter 33: CI/CD Pipeline](Developer-Manual-Chapter33-CICD.md)

