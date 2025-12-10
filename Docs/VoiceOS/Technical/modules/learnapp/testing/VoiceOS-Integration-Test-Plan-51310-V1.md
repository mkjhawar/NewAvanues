# Integration Test Plan

**Created:** 2025-10-13 01:41:00 PDT
**Author:** Integration Agent
**Version:** 1.0
**Status:** READY FOR EXECUTION (once components are implemented)

---

## Overview

This document defines the comprehensive testing strategy for the enhanced AppStateDetector integration. Tests cover unit, integration, and end-to-end scenarios.

---

## Test Environment Setup

### Prerequisites

```gradle
// build.gradle (LearnApp module)
dependencies {
    // Unit testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.3.1'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:5.0.0'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'app.cash.turbine:turbine:1.0.0'  // For Flow testing

    // Integration testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
    androidTestImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
}
```

### Test Directory Structure

```
modules/apps/LearnApp/src/
├── test/java/com/augmentalis/learnapp/
│   ├── state/
│   │   ├── AppStateDetectorTest.kt (UNIT)
│   │   ├── StateDetectorFactoryTest.kt (UNIT)
│   │   ├── detectors/
│   │   │   ├── LoginDetectorTest.kt (UNIT)
│   │   │   ├── LoadingDetectorTest.kt (UNIT)
│   │   │   └── ... (one per detector)
│   │   └── MultiStateDetectionTest.kt (UNIT)
│   ├── validation/
│   │   └── MetadataValidatorTest.kt (UNIT)
│   ├── notification/
│   │   └── NotificationManagerTest.kt (UNIT)
│   └── integration/
│       └── ComponentIntegrationTest.kt (INTEGRATION - JVM)
│
└── androidTest/java/com/augmentalis/learnapp/
    ├── integration/
    │   ├── AccessibilityScrapingIntegrationTest.kt
    │   ├── ExplorationEngineIntegrationTest.kt
    │   ├── CommandGeneratorIntegrationTest.kt
    │   └── SystemIntegrationTest.kt
    └── e2e/
        └── EndToEndFlowTest.kt
```

---

## Unit Tests

### 1. AppStateDetector Tests

**File:** `modules/apps/LearnApp/src/test/java/com/augmentalis/learnapp/state/AppStateDetectorTest.kt`

```kotlin
package com.augmentalis.learnapp.state

import android.view.accessibility.AccessibilityNodeInfo
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

class AppStateDetectorTest {

    private lateinit var detector: AppStateDetector
    private lateinit var mockNode: AccessibilityNodeInfo

    @Before
    fun setup() {
        detector = AppStateDetector(
            StateDetectorConfig(
                logDetections = false  // Reduce test noise
            )
        )
        mockNode = mock()
    }

    @Test
    fun `detectState returns UNKNOWN for null node`() {
        val result = detector.detectState(null)

        assertEquals(AppState.UNKNOWN, result.state)
        assertEquals(0.0f, result.confidence)
    }

    @Test
    fun `detectState returns LOGIN for login screen`() {
        // Arrange: Create login screen node
        whenever(mockNode.childCount).thenReturn(3)
        whenever(mockNode.text).thenReturn("Login")
        whenever(mockNode.className).thenReturn("android.widget.Button")

        val child1 = mock<AccessibilityNodeInfo>().apply {
            whenever(text).thenReturn("Username")
            whenever(className).thenReturn("android.widget.EditText")
        }
        val child2 = mock<AccessibilityNodeInfo>().apply {
            whenever(text).thenReturn("Password")
            whenever(className).thenReturn("android.widget.EditText")
        }
        val child3 = mock<AccessibilityNodeInfo>().apply {
            whenever(text).thenReturn("Login")
            whenever(className).thenReturn("android.widget.Button")
        }

        whenever(mockNode.getChild(0)).thenReturn(child1)
        whenever(mockNode.getChild(1)).thenReturn(child2)
        whenever(mockNode.getChild(2)).thenReturn(child3)

        // Act
        val result = detector.detectState(mockNode)

        // Assert
        assertEquals(AppState.LOGIN, result.state)
        assertTrue("Confidence should be >= 0.7", result.confidence >= 0.7f)
        assertTrue("Should have indicators", result.indicators.isNotEmpty())
    }

    @Test
    fun `detectState returns LOADING for loading screen`() {
        // Arrange: Create loading screen node
        whenever(mockNode.childCount).thenReturn(1)
        whenever(mockNode.text).thenReturn("Loading...")

        val child = mock<AccessibilityNodeInfo>().apply {
            whenever(className).thenReturn("android.widget.ProgressBar")
        }
        whenever(mockNode.getChild(0)).thenReturn(child)

        // Act
        val result = detector.detectState(mockNode)

        // Assert
        assertEquals(AppState.LOADING, result.state)
        assertTrue("Confidence should be >= 0.7", result.confidence >= 0.7f)
    }

    @Test
    fun `currentState flow emits state changes`() = runTest {
        // Arrange
        val loginNode = createLoginScreenNode()
        val loadingNode = createLoadingScreenNode()

        // Act & Assert
        detector.currentState.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(AppState.UNKNOWN, initial.state)

            // Detect login
            detector.detectState(loginNode)
            val loginState = awaitItem()
            assertEquals(AppState.LOGIN, loginState.state)

            // Detect loading
            detector.detectState(loadingNode)
            val loadingState = awaitItem()
            assertEquals(AppState.LOADING, loadingState.state)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `transitions flow tracks state changes`() = runTest {
        // Arrange
        val loginNode = createLoginScreenNode()
        val loadingNode = createLoadingScreenNode()

        // Act
        detector.detectState(loginNode)
        detector.detectState(loadingNode)

        // Assert
        detector.transitions.test {
            val transitions = awaitItem()
            assertEquals(2, transitions.size)

            val firstTransition = transitions[0]
            assertEquals(AppState.UNKNOWN, firstTransition.fromState)
            assertEquals(AppState.LOGIN, firstTransition.toState)

            val secondTransition = transitions[1]
            assertEquals(AppState.LOGIN, secondTransition.fromState)
            assertEquals(AppState.LOADING, secondTransition.toState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset clears state and transitions`() = runTest {
        // Arrange
        detector.detectState(createLoginScreenNode())
        detector.detectState(createLoadingScreenNode())

        // Act
        detector.reset()

        // Assert
        val currentState = detector.getCurrentState()
        assertEquals(AppState.UNKNOWN, currentState.state)

        val transitions = detector.getTransitionHistory()
        assertTrue("Transitions should be empty", transitions.isEmpty())
    }

    // Helper methods
    private fun createLoginScreenNode(): AccessibilityNodeInfo {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.childCount).thenReturn(3)
        // ... configure login screen
        return node
    }

    private fun createLoadingScreenNode(): AccessibilityNodeInfo {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.childCount).thenReturn(1)
        // ... configure loading screen
        return node
    }
}
```

---

### 2. StateDetectorFactory Tests

**File:** `modules/apps/LearnApp/src/test/java/com/augmentalis/learnapp/state/StateDetectorFactoryTest.kt`

```kotlin
package com.augmentalis.learnapp.state

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

class StateDetectorFactoryTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mock()
    }

    @Test
    fun `createBasicDetector returns AppStateDetector instance`() {
        val detector = StateDetectorFactory.createBasicDetector()

        assertNotNull(detector)
        assertTrue(detector is AppStateDetector)
    }

    @Test
    fun `createBasicDetector with config uses provided config`() {
        val config = StateDetectorConfig(
            confidenceThreshold = 0.8f,
            logDetections = false
        )

        val detector = StateDetectorFactory.createBasicDetector(config)

        assertNotNull(detector)
        // Config is private, but we can test behavior
        // (detector will use 0.8 threshold internally)
    }

    @Test
    fun `createEnhancedDetector returns EnhancedAppStateDetector instance`() {
        val detector = StateDetectorFactory.createEnhancedDetector()

        assertNotNull(detector)
        assertTrue(detector is EnhancedAppStateDetector)
    }

    @Test
    fun `createEnhancedDetector with all detectors enabled`() {
        val config = StateDetectionConfig(
            enableLoginDetection = true,
            enableLoadingDetection = true,
            enableErrorDetection = true,
            enablePermissionDetection = true,
            enableTutorialDetection = true,
            enableEmptyStateDetection = true,
            enableDialogDetection = true
        )

        val detector = StateDetectorFactory.createEnhancedDetector(config)

        assertNotNull(detector)
        // All detectors should be wired up
    }

    @Test
    fun `createMetadataValidator returns MetadataValidator instance`() {
        val validator = StateDetectorFactory.createMetadataValidator(mockContext)

        assertNotNull(validator)
        assertTrue(validator is MetadataValidator)
    }

    @Test
    fun `createNotificationManager returns NotificationManager instance`() {
        val manager = StateDetectorFactory.createNotificationManager(mockContext)

        assertNotNull(manager)
        assertTrue(manager is NotificationManager)
    }
}
```

---

### 3. Individual Detector Tests

**Example:** `LoginDetectorTest.kt`

```kotlin
package com.augmentalis.learnapp.state.detectors

import org.junit.Test
import org.junit.Assert.*

class LoginDetectorTest {

    @Test
    fun `detect returns high confidence for login screen with 2+ input fields`() {
        // Arrange
        val detector = LoginDetector()
        val textContent = listOf("Login", "Username", "Password")
        val viewIds = listOf("et_username", "et_password", "btn_login")
        val classNames = listOf(
            "android.widget.EditText",
            "android.widget.EditText",
            "android.widget.Button"
        )

        // Act
        val result = detector.detect(textContent, viewIds, classNames)

        // Assert
        assertEquals(AppState.LOGIN, result.state)
        assertTrue("Confidence should be >= 0.8", result.confidence >= 0.8f)
        assertTrue("Should detect input fields",
            result.indicators.any { it.contains("input fields") })
    }

    @Test
    fun `detect returns low confidence for non-login screen`() {
        // Arrange
        val detector = LoginDetector()
        val textContent = listOf("Home", "Settings", "Profile")
        val viewIds = listOf("btn_home", "btn_settings")
        val classNames = listOf("android.widget.Button", "android.widget.Button")

        // Act
        val result = detector.detect(textContent, viewIds, classNames)

        // Assert
        assertTrue("Confidence should be < 0.5", result.confidence < 0.5f)
    }

    @Test
    fun `detect handles Material input fields`() {
        // Arrange
        val detector = LoginDetector()
        val textContent = listOf("Sign In")
        val viewIds = listOf("til_username", "til_password")
        val classNames = listOf(
            "com.google.android.material.textfield.TextInputLayout",
            "com.google.android.material.textfield.TextInputLayout"
        )

        // Act
        val result = detector.detect(textContent, viewIds, classNames)

        // Assert
        assertTrue("Should detect Material inputs", result.confidence > 0.5f)
        assertTrue("Should mention Material",
            result.indicators.any { it.contains("Material") })
    }
}
```

---

### 4. MetadataValidator Tests

**File:** `modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/validation/MetadataValidatorTest.kt`

```kotlin
package com.augmentalis.voiceoscore.validation

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

class MetadataValidatorTest {

    private lateinit var validator: MetadataValidator
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mock()
        validator = MetadataValidator(mockContext)
    }

    @Test
    fun `validate returns high score for quality element`() {
        // Arrange: High-quality element
        val node = mock<AccessibilityNodeInfo>().apply {
            whenever(text).thenReturn("Submit Button")
            whenever(contentDescription).thenReturn("Submit form")
            whenever(viewIdResourceName).thenReturn("btn_submit")
            whenever(className).thenReturn("android.widget.Button")
            whenever(isClickable).thenReturn(true)
            whenever(isEnabled).thenReturn(true)
        }

        val element = ScrapedElementEntity(
            elementHash = "hash123",
            appId = "app123",
            className = "android.widget.Button",
            viewIdResourceName = "btn_submit",
            text = "Submit Button",
            contentDescription = "Submit form",
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = true,
            isEnabled = true
        )

        // Act
        val result = validator.validate(node, element)

        // Assert
        assertTrue("Quality score should be >= 0.8", result.qualityScore >= 0.8f)
        assertTrue("Should have no major issues", result.issues.isEmpty() ||
            result.issues.all { it.severity == IssueSeverity.MINOR })
    }

    @Test
    fun `validate returns low score for poor element`() {
        // Arrange: Poor-quality element (no text, no content description, no ID)
        val node = mock<AccessibilityNodeInfo>().apply {
            whenever(text).thenReturn(null)
            whenever(contentDescription).thenReturn(null)
            whenever(viewIdResourceName).thenReturn(null)
            whenever(className).thenReturn("android.view.View")
            whenever(isClickable).thenReturn(true)
            whenever(isEnabled).thenReturn(true)
        }

        val element = ScrapedElementEntity(
            elementHash = "hash456",
            appId = "app123",
            className = "android.view.View",
            viewIdResourceName = null,
            text = null,
            contentDescription = null,
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = true,
            isEnabled = true
        )

        // Act
        val result = validator.validate(node, element)

        // Assert
        assertTrue("Quality score should be < 0.5", result.qualityScore < 0.5f)
        assertTrue("Should have critical issues",
            result.issues.any { it.severity == IssueSeverity.CRITICAL })
        assertTrue("Should recommend improvements", result.recommendations.isNotEmpty())
    }

    @Test
    fun `validateFromEntity works without AccessibilityNodeInfo`() {
        // Arrange
        val element = ScrapedElementEntity(
            elementHash = "hash789",
            appId = "app123",
            className = "android.widget.Button",
            viewIdResourceName = "btn_action",
            text = "Action",
            contentDescription = "Perform action",
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = true,
            isEnabled = true
        )

        // Act
        val result = validator.validateFromEntity(element)

        // Assert
        assertNotNull(result)
        assertTrue("Should calculate quality score", result.qualityScore > 0.0f)
    }
}
```

---

## Integration Tests (Android)

### 1. AccessibilityScrapingIntegration Tests

**File:** `modules/apps/VoiceOSCore/src/androidTest/java/com/augmentalis/voiceoscore/integration/AccessibilityScrapingIntegrationTest.kt`

```kotlin
package com.augmentalis.voiceoscore.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceoscore.scraping.AccessibilityScrapingIntegration
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class AccessibilityScrapingIntegrationTest {

    private lateinit var context: Context
    private lateinit var mockService: AccessibilityService
    private lateinit var integration: AccessibilityScrapingIntegration

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockService = mock()
        integration = AccessibilityScrapingIntegration(context, mockService)
    }

    @Test
    fun `scraping validates metadata for each element`() = runTest {
        // Arrange
        val event = createMockWindowStateChangedEvent()
        val mockRootNode = createMockUITree()

        whenever(event.source).thenReturn(mockRootNode)

        // Act
        integration.onAccessibilityEvent(event)

        // Wait for async scraping
        delay(2000)

        // Assert
        // Verify metadata validation was called
        // (This requires instrumenting AccessibilityScrapingIntegration
        // to track validation calls - could use a test double)
    }

    @Test
    fun `scraping tracks poor quality elements in LearnApp mode`() = runTest {
        // Arrange
        val event = createMockWindowStateChangedEvent()
        val mockRootNode = createMockUITreeWithPoorQualityElements()

        whenever(event.source).thenReturn(mockRootNode)

        // Act
        val result = integration.learnApp("com.test.app")

        // Assert
        assertTrue("Should complete successfully", result.success)
        // Check that poor quality elements were tracked
    }

    // Helper methods
    private fun createMockWindowStateChangedEvent(): AccessibilityEvent {
        return mock<AccessibilityEvent>().apply {
            whenever(eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            whenever(packageName).thenReturn("com.test.app")
        }
    }

    private fun createMockUITree(): AccessibilityNodeInfo {
        // Create mock UI tree with various elements
        return mock()
    }

    private fun createMockUITreeWithPoorQualityElements(): AccessibilityNodeInfo {
        // Create mock UI tree with some poor-quality elements
        return mock()
    }
}
```

---

### 2. ExplorationEngine Integration Tests

**File:** `modules/apps/LearnApp/src/androidTest/java/com/augmentalis/learnapp/integration/ExplorationEngineIntegrationTest.kt`

```kotlin
package com.augmentalis.learnapp.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.augmentalis.learnapp.exploration.ExplorationEngine
import com.augmentalis.learnapp.models.ExplorationState
import com.augmentalis.learnapp.state.AppState
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ExplorationEngineIntegrationTest {

    private lateinit var context: Context
    private lateinit var mockService: AccessibilityService
    private lateinit var uuidCreator: UUIDCreator
    private lateinit var thirdPartyGenerator: ThirdPartyUuidGenerator
    private lateinit var aliasManager: UuidAliasManager
    private lateinit var engine: ExplorationEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockService = mock()
        uuidCreator = UUIDCreator()
        thirdPartyGenerator = ThirdPartyUuidGenerator()
        aliasManager = UuidAliasManager()

        engine = ExplorationEngine(
            accessibilityService = mockService,
            uuidCreator = uuidCreator,
            thirdPartyGenerator = thirdPartyGenerator,
            aliasManager = aliasManager
        )
    }

    @Test
    fun `exploration detects blocking states and pauses`() = runTest {
        // Arrange
        val mockRootNode = createMockLoginScreen()
        whenever(mockService.rootInActiveWindow).thenReturn(mockRootNode)

        // Act
        engine.startExploration("com.test.app")

        // Assert
        engine.explorationState.test {
            // Wait for initial running state
            var state = awaitItem()
            while (state !is ExplorationState.PausedForLogin) {
                state = awaitItem()
            }

            assertTrue("Should pause for login", state is ExplorationState.PausedForLogin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exploration resumes after handling blocking state`() = runTest {
        // Arrange
        val loginNode = createMockLoginScreen()
        val homeNode = createMockHomeScreen()

        whenever(mockService.rootInActiveWindow)
            .thenReturn(loginNode)  // First call
            .thenReturn(homeNode)   // After user logs in

        // Act
        engine.startExploration("com.test.app")

        // Simulate user login after 2 seconds
        delay(2000)
        // Screen should change, exploration should resume

        // Assert
        engine.explorationState.test {
            var state = awaitItem()

            // Wait for pause
            while (state !is ExplorationState.PausedForLogin) {
                state = awaitItem()
            }

            // Wait for resume
            while (state !is ExplorationState.Running ||
                   (state as ExplorationState.Running).progress.screensExplored == 0) {
                state = awaitItem()
            }

            assertTrue("Should resume exploration", state is ExplorationState.Running)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Helper methods
    private fun createMockLoginScreen(): AccessibilityNodeInfo {
        // Create mock login screen with LOGIN state indicators
        return mock()
    }

    private fun createMockHomeScreen(): AccessibilityNodeInfo {
        // Create mock home screen
        return mock()
    }
}
```

---

### 3. CommandGenerator Integration Tests

**File:** `modules/apps/VoiceOSCore/src/androidTest/java/com/augmentalis/voiceoscore/integration/CommandGeneratorIntegrationTest.kt`

```kotlin
package com.augmentalis.voiceoscore.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceoscore.scraping.CommandGenerator
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommandGeneratorIntegrationTest {

    private lateinit var context: Context
    private lateinit var generator: CommandGenerator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        generator = CommandGenerator(context)
    }

    @Test
    fun `generateCommands skips poor quality elements`() {
        // Arrange
        val elements = listOf(
            createHighQualityElement("Submit", "btn_submit"),
            createPoorQualityElement(),  // No text, no ID
            createHighQualityElement("Cancel", "btn_cancel"),
            createPoorQualityElement()   // No text, no ID
        )

        // Act
        val commands = generator.generateCommandsForElements(elements)

        // Assert
        assertEquals("Should generate 2 commands (skip 2 poor elements)",
            2, commands.size)

        assertTrue("All commands should have valid hashes",
            commands.all { it.elementHash.isNotBlank() })
    }

    @Test
    fun `generateCommands generates quality report`() {
        // Arrange
        val elements = listOf(
            createHighQualityElement("Submit", "btn_submit"),
            createPoorQualityElement(),
            createHighQualityElement("Cancel", "btn_cancel")
        )

        // Act
        val commands = generator.generateCommandsForElements(elements)

        // Assert
        // Quality report is logged to Logcat
        // In a real test, we'd capture log output or use a test logger
        assertEquals(2, commands.size)
    }

    // Helper methods
    private fun createHighQualityElement(text: String, id: String): ScrapedElementEntity {
        return ScrapedElementEntity(
            elementHash = "hash_${System.nanoTime()}",
            appId = "app123",
            className = "android.widget.Button",
            viewIdResourceName = id,
            text = text,
            contentDescription = "$text button",
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = true,
            isEnabled = true
        )
    }

    private fun createPoorQualityElement(): ScrapedElementEntity {
        return ScrapedElementEntity(
            elementHash = "hash_${System.nanoTime()}",
            appId = "app123",
            className = "android.view.View",
            viewIdResourceName = null,
            text = null,
            contentDescription = null,
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = true,
            isEnabled = true
        )
    }
}
```

---

## End-to-End Tests

**File:** `modules/apps/LearnApp/src/androidTest/java/com/augmentalis/learnapp/e2e/EndToEndFlowTest.kt`

```kotlin
package com.augmentalis.learnapp.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.augmentalis.learnapp.exploration.ExplorationEngine
import com.augmentalis.learnapp.models.ExplorationState
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndFlowTest {

    private lateinit var context: Context
    private lateinit var device: UiDevice

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun `complete LearnApp flow with all integrations`() = runTest {
        // This test requires a real app to be installed
        // For CI/CD, use a test app with known UI

        // 1. Launch test app
        launchTestApp()

        // 2. Start VoiceOSService
        startVoiceOSService()

        // 3. Start LearnApp exploration
        val engine = getExplorationEngine()
        engine.startExploration("com.test.sampleapp")

        // 4. Monitor exploration state
        var finalState: ExplorationState? = null
        val timeout = 60000L  // 1 minute

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val state = engine.explorationState.value

            when (state) {
                is ExplorationState.PausedForLogin -> {
                    // Simulate user login
                    simulateLogin()
                }
                is ExplorationState.PausedForPermission -> {
                    // Grant permission
                    grantPermission()
                }
                is ExplorationState.Completed -> {
                    finalState = state
                    break
                }
                is ExplorationState.Failed -> {
                    fail("Exploration failed: ${state.error.message}")
                }
                else -> {
                    // Continue waiting
                    delay(1000)
                }
            }
        }

        // 5. Verify completion
        assertNotNull("Should complete within timeout", finalState)
        assertTrue("Should be completed", finalState is ExplorationState.Completed)

        val stats = (finalState as ExplorationState.Completed).stats
        assertTrue("Should discover elements", stats.totalElements > 0)
        assertTrue("Should discover screens", stats.totalScreens > 0)

        // 6. Verify metadata validation happened
        // (This requires access to internal metrics)

        // 7. Verify commands were generated
        // (Check database or metrics)
    }

    // Helper methods
    private fun launchTestApp() {
        val intent = context.packageManager.getLaunchIntentForPackage("com.test.sampleapp")
        intent?.let {
            context.startActivity(it)
            device.wait(Until.hasObject(By.pkg("com.test.sampleapp")), 5000)
        }
    }

    private fun startVoiceOSService() {
        // Start VoiceAccessibilityService
        // (Requires special permissions and setup)
    }

    private fun getExplorationEngine(): ExplorationEngine {
        // Get engine from service
        // (Requires service to be running)
        return mock()  // Placeholder
    }

    private fun simulateLogin() {
        // Use UiAutomator to fill login form
        device.findObject(By.res("et_username")).text = "testuser"
        device.findObject(By.res("et_password")).text = "testpass"
        device.findObject(By.res("btn_login")).click()
        Thread.sleep(2000)  // Wait for login
    }

    private fun grantPermission() {
        // Use UiAutomator to grant permission
        device.findObject(By.text("Allow")).click()
        Thread.sleep(1000)
    }
}
```

---

## Performance Tests

### Memory Leak Test

```kotlin
@RunWith(AndroidJUnit4::class)
class MemoryLeakTest {

    @Test
    fun `no memory leaks during repeated state detection`() {
        val detector = StateDetectorFactory.createEnhancedDetector()
        val runtime = Runtime.getRuntime()

        // Force GC and measure baseline
        System.gc()
        Thread.sleep(1000)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()

        // Run 1000 detections
        repeat(1000) {
            val node = createMockNode()
            detector.detectState(node)
            node.recycle()
        }

        // Force GC and measure final memory
        System.gc()
        Thread.sleep(1000)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()

        val memoryGrowth = finalMemory - baselineMemory
        val growthMB = memoryGrowth / (1024 * 1024)

        assertTrue("Memory growth should be < 50MB, was ${growthMB}MB",
            growthMB < 50)
    }
}
```

### Performance Benchmark Test

```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {

    @Test
    fun `state detection completes within 50ms`() {
        val detector = StateDetectorFactory.createEnhancedDetector()
        val node = createComplexMockUITree()  // 100+ elements

        val times = mutableListOf<Long>()

        // Run 100 times
        repeat(100) {
            val start = System.currentTimeMillis()
            detector.detectState(node)
            val end = System.currentTimeMillis()
            times.add(end - start)
        }

        val avgTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L

        println("Average detection time: ${avgTime}ms")
        println("Max detection time: ${maxTime}ms")

        assertTrue("Average time should be < 50ms, was ${avgTime}ms",
            avgTime < 50)
        assertTrue("Max time should be < 100ms, was ${maxTime}ms",
            maxTime < 100)
    }
}
```

---

## Test Execution Strategy

### Local Development

```bash
# Run unit tests
./gradlew :modules:apps:LearnApp:test

# Run Android integration tests
./gradlew :modules:apps:LearnApp:connectedAndroidTest

# Run specific test class
./gradlew :modules:apps:LearnApp:test --tests AppStateDetectorTest

# Run with coverage
./gradlew :modules:apps:LearnApp:testDebugUnitTestCoverage
```

### CI/CD Pipeline

```yaml
# .github/workflows/integration-tests.yml
name: Integration Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run unit tests
        run: ./gradlew :modules:apps:LearnApp:test

  android-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          script: ./gradlew :modules:apps:LearnApp:connectedAndroidTest
```

---

## Test Coverage Goals

| Component | Target Coverage | Minimum Coverage |
|-----------|----------------|------------------|
| AppStateDetector | 90% | 80% |
| StateDetectorFactory | 95% | 85% |
| Individual Detectors | 85% | 75% |
| MetadataValidator | 90% | 80% |
| NotificationManager | 85% | 75% |
| Integration Points | 80% | 70% |
| Overall | 85% | 75% |

---

## Test Reporting

### Generate HTML Report

```bash
# Run tests with coverage
./gradlew :modules:apps:LearnApp:testDebugUnitTestCoverage

# Open report
open modules/apps/LearnApp/build/reports/coverage/test/debug/index.html
```

### View Test Results

```bash
# Open test results
open modules/apps/LearnApp/build/reports/tests/testDebugUnitTest/index.html
```

---

## Success Criteria

Tests must pass with:
- ✅ 100% of unit tests passing
- ✅ 100% of integration tests passing
- ✅ 90% of E2E tests passing (some may be flaky)
- ✅ >= 85% overall test coverage
- ✅ No memory leaks detected
- ✅ Performance within acceptable bounds:
  - State detection: < 50ms average
  - Metadata validation: < 50ms per element
  - Memory usage: < 20MB increase

---

## Known Test Limitations

1. **Accessibility Testing:** Requires real AccessibilityService running, difficult to mock completely
2. **UI Automation:** Flaky on CI/CD, may need retries
3. **Device-Specific:** Some tests may behave differently on different Android versions
4. **Timing Issues:** Async operations may cause race conditions

### Mitigation Strategies

1. Use coroutine test dispatcher for deterministic timing
2. Add generous timeouts for UI automation tests
3. Use idling resources for Espresso tests
4. Mock AccessibilityService where possible
5. Run flaky tests multiple times in CI/CD

---

**END OF INTEGRATION TEST PLAN**
