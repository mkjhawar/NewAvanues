/**
 * ExplorationStateTest.kt - Unit tests for ExplorationState
 *
 * Tests exploration state management, phase transitions, and data tracking.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition - Phase 1 Testing)
 */

package com.augmentalis.learnappcore.exploration

import android.graphics.Rect
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.safety.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ExplorationState.
 *
 * Tests:
 * - Phase transitions (IDLE -> INITIALIZING -> EXPLORING -> COMPLETED)
 * - Screen recording and tracking
 * - Element discovery and click tracking
 * - Navigation history
 * - Statistics calculation
 * - Dangerous element tracking
 * - Dynamic region detection
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExplorationStateTest {

    private lateinit var explorationState: ExplorationState
    private lateinit var mockCallback: ExplorationStateCallback

    companion object {
        private const val TEST_PACKAGE = "com.example.testapp"
        private const val TEST_APP_NAME = "Test App"
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        explorationState = ExplorationState(TEST_PACKAGE, TEST_APP_NAME)
        mockCallback = mockk(relaxed = true)
        explorationState.setCallback(mockCallback)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ============================================================
    // Test: start_FromIdle_TransitionsToInitializing
    // ============================================================

    @Test
    fun start_FromIdle_TransitionsToInitializing() {
        // Given: State is IDLE (default)
        assertEquals(ExplorationPhase.IDLE, explorationState.phase)

        // When: Starting exploration
        explorationState.start()

        // Then: Phase transitions to INITIALIZING
        assertEquals(ExplorationPhase.INITIALIZING, explorationState.phase)
        assertTrue(explorationState.startTimestamp > 0, "Start timestamp should be set")
        assertTrue(explorationState.lastActionTimestamp > 0, "Last action timestamp should be set")

        // Then: Callback is invoked
        verify {
            mockCallback.onPhaseChanged(
                ExplorationPhase.IDLE,
                ExplorationPhase.INITIALIZING
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun start_FromNonIdle_ThrowsException() {
        // Given: State is already INITIALIZING
        explorationState.start()

        // When: Trying to start again
        // Then: Should throw IllegalArgumentException
        explorationState.start()
    }

    // ============================================================
    // Test: beginExploring_FromInitializing_TransitionsToExploring
    // ============================================================

    @Test
    fun beginExploring_FromInitializing_TransitionsToExploring() {
        // Given: State is INITIALIZING
        explorationState.start()
        assertEquals(ExplorationPhase.INITIALIZING, explorationState.phase)

        // When: Beginning exploration
        explorationState.beginExploring()

        // Then: Phase transitions to EXPLORING
        assertEquals(ExplorationPhase.EXPLORING, explorationState.phase)

        // Then: Callback is invoked
        verify {
            mockCallback.onPhaseChanged(
                ExplorationPhase.INITIALIZING,
                ExplorationPhase.EXPLORING
            )
        }
    }

    // ============================================================
    // Test: complete_FromExploring_TransitionsToCompleted
    // ============================================================

    @Test
    fun complete_FromExploring_TransitionsToCompleted() {
        // Given: State is EXPLORING
        explorationState.start()
        explorationState.beginExploring()
        assertEquals(ExplorationPhase.EXPLORING, explorationState.phase)

        // When: Completing exploration
        explorationState.complete()

        // Then: Phase transitions to COMPLETED
        assertEquals(ExplorationPhase.COMPLETED, explorationState.phase)

        // Then: Callbacks are invoked
        verify {
            mockCallback.onPhaseChanged(
                ExplorationPhase.EXPLORING,
                ExplorationPhase.COMPLETED
            )
            mockCallback.onExplorationComplete(any())
        }
    }

    // ============================================================
    // Test: waitForUser_FromExploring_TransitionsToWaiting
    // ============================================================

    @Test
    fun waitForUser_FromExploring_TransitionsToWaiting() {
        // Given: State is EXPLORING
        explorationState.start()
        explorationState.beginExploring()
        assertEquals(ExplorationPhase.EXPLORING, explorationState.phase)

        val reason = "Login screen detected"

        // When: Waiting for user
        explorationState.waitForUser(reason)

        // Then: Phase transitions to WAITING_USER
        assertEquals(ExplorationPhase.WAITING_USER, explorationState.phase)

        // Then: Callbacks are invoked
        verify {
            mockCallback.onPhaseChanged(
                ExplorationPhase.EXPLORING,
                ExplorationPhase.WAITING_USER
            )
            mockCallback.onWaitingForUser(reason)
        }
    }

    // ============================================================
    // Test: recordScreen_NewScreen_AddsToFingerprints
    // ============================================================

    @Test
    fun recordScreen_NewScreen_AddsToFingerprints() {
        // Given: Fresh exploration state
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(
            createTestElement("Button", "Click Me", "com.test:id/button"),
            createTestElement("TextView", "Title", "com.test:id/title")
        )

        val fingerprint = ScreenFingerprint.create(
            activityName = "MainActivity",
            packageName = TEST_PACKAGE,
            elements = elements
        )

        // When: Recording a screen
        explorationState.recordScreen(fingerprint)

        // Then: Screen is tracked
        assertEquals(fingerprint.screenHash, explorationState.currentScreenHash)
        assertEquals("MainActivity", explorationState.currentActivityName)
        assertEquals(1, explorationState.getScreenFingerprints().size)
        assertTrue(explorationState.getScreenFingerprints().contains(fingerprint))

        // Then: Callback is invoked
        verify {
            mockCallback.onScreenChanged("", fingerprint)
        }
    }

    @Test
    fun recordScreen_MultipleScreens_TracksDepth() {
        // Given: Fresh exploration state
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))

        val fingerprint1 = ScreenFingerprint.create(
            activityName = "MainActivity",
            packageName = TEST_PACKAGE,
            elements = elements
        )

        val fingerprint2 = ScreenFingerprint.create(
            activityName = "DetailActivity",
            packageName = TEST_PACKAGE,
            elements = elements
        )

        // When: Recording multiple screens
        explorationState.recordScreen(fingerprint1)
        assertEquals(0, explorationState.currentDepth, "Initial screen should be depth 0")

        explorationState.recordScreen(fingerprint2)

        // Then: Depth increases
        assertEquals(1, explorationState.currentDepth, "Second screen should be depth 1")

        // Then: Both screens tracked
        assertEquals(2, explorationState.getScreenFingerprints().size)
    }

    // ============================================================
    // Test: recordElements_ValidElements_TracksCorrectly
    // ============================================================

    @Test
    fun recordElements_ValidElements_TracksCorrectly() {
        // Given: State with recorded screen
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(
            createTestElement("Button", "Click Me", "com.test:id/button"),
            createTestElement("TextView", "Title", "com.test:id/title")
        )

        val fingerprint = ScreenFingerprint.create(
            activityName = "MainActivity",
            packageName = TEST_PACKAGE,
            elements = elements
        )
        explorationState.recordScreen(fingerprint)

        val testElements = listOf(
            createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true),
            createTestElement("EditText", "", "com.test:id/input", isClickable = false),
            createTestElement("ImageButton", "Delete", "com.test:id/delete", isClickable = true)
        )

        // When: Recording elements
        explorationState.recordElements(testElements)

        // Then: Elements are tracked
        assertEquals(3, explorationState.getElements().size)
        val trackedElements = explorationState.getElements()
        assertEquals("Submit", trackedElements.find { it.resourceId == "com.test:id/submit" }?.text)
        assertEquals("com.test:id/input", trackedElements.find { it.resourceId == "com.test:id/input" }?.resourceId)

        // Then: Callback is invoked
        verify {
            mockCallback.onElementsDiscovered(3)
        }
    }

    @Test
    fun recordElements_TracksElementsPerScreen() {
        // Given: State with recorded screen
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint = ScreenFingerprint.create(
            activityName = "MainActivity",
            packageName = TEST_PACKAGE,
            elements = elements
        )
        explorationState.recordScreen(fingerprint)

        val testElements = listOf(
            createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true),
            createTestElement("Button", "Cancel", "com.test:id/cancel", isClickable = true)
        )

        // When: Recording elements
        explorationState.recordElements(testElements)

        // Then: Elements are associated with screen
        val screenElements = explorationState.getElementsForScreen(fingerprint.screenHash)
        assertEquals(2, screenElements.size)
        assertTrue(screenElements.any { it.resourceId == "com.test:id/submit" })
        assertTrue(screenElements.any { it.resourceId == "com.test:id/cancel" })
    }

    @Test
    fun recordClick_TracksClickedElements() {
        // Given: State with recorded elements
        explorationState.start()
        explorationState.beginExploring()

        val element = createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true)

        // When: Recording a click
        explorationState.recordClick(element)

        // Then: Element is tracked as clicked
        assertTrue(explorationState.hasClicked(element))

        // Then: Callback is invoked
        verify {
            mockCallback.onElementClicked(element)
        }
    }

    @Test
    fun hasClicked_UnclickedElement_ReturnsFalse() {
        // Given: Fresh state
        explorationState.start()
        explorationState.beginExploring()

        val element = createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true)

        // When/Then: Element has not been clicked
        assertFalse(explorationState.hasClicked(element))
    }

    // ============================================================
    // Test: getStats_WithData_ReturnsCorrectCounts
    // ============================================================

    @Test
    fun getStats_WithData_ReturnsCorrectCounts() {
        // Given: State with exploration data
        explorationState.start()
        explorationState.beginExploring()

        // Add 2 screens
        val elements1 = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint1 = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements1)
        explorationState.recordScreen(fingerprint1)

        val elements2 = listOf(createTestElement("Button", "Back", "com.test:id/back"))
        val fingerprint2 = ScreenFingerprint.create("DetailActivity", TEST_PACKAGE, elements2)
        explorationState.recordScreen(fingerprint2)

        // Add 5 elements to screen 1
        val testElements = listOf(
            createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true),
            createTestElement("Button", "Cancel", "com.test:id/cancel", isClickable = true),
            createTestElement("TextView", "Label", "com.test:id/label", isClickable = false),
            createTestElement("ImageButton", "Share", "com.test:id/share", isClickable = true),
            createTestElement("EditText", "", "com.test:id/input", isClickable = false)
        )
        explorationState.recordScreen(fingerprint1) // Back to screen 1
        explorationState.recordElements(testElements)

        // Click 2 elements
        explorationState.recordClick(testElements[0]) // Submit
        explorationState.recordClick(testElements[1]) // Cancel

        // Record navigation
        explorationState.recordNavigation(
            fingerprint1.screenHash,
            fingerprint2.screenHash,
            testElements[0]
        )

        // Add dangerous element
        explorationState.recordDangerousElement(testElements[3], DoNotClickReason.CONTENT_CREATION)

        // Add dynamic region
        val dynamicRegion = DynamicRegion(
            screenHash = fingerprint1.screenHash,
            regionId = "feed",
            bounds = Rect(0, 0, 100, 100),
            changeType = DynamicChangeType.INFINITE_SCROLL
        )
        explorationState.recordDynamicRegion(dynamicRegion)

        // Record command
        explorationState.recordCommand("cmd-001", "submit form")

        // When: Getting stats
        val stats = explorationState.getStats()

        // Then: Stats are correct
        assertEquals(2, stats.screensExplored, "Should have 2 screens")
        assertEquals(5, stats.elementsDiscovered, "Should have 5 elements")
        assertEquals(2, stats.elementsClicked, "Should have 2 clicked elements")
        assertEquals(1, stats.commandsGenerated, "Should have 1 command")
        assertEquals(1, stats.navigationCount, "Should have 1 navigation")
        assertEquals(1, stats.dangerousElementsSkipped, "Should have 1 dangerous element")
        assertEquals(1, stats.dynamicRegionsDetected, "Should have 1 dynamic region")
        assertTrue(stats.durationMs > 0, "Duration should be positive")
    }

    @Test
    fun getStats_EmptyState_ReturnsZeros() {
        // Given: Fresh state
        explorationState.start()

        // When: Getting stats
        val stats = explorationState.getStats()

        // Then: All counts are zero
        assertEquals(0, stats.screensExplored)
        assertEquals(0, stats.elementsDiscovered)
        assertEquals(0, stats.elementsClicked)
        assertEquals(0, stats.commandsGenerated)
        assertEquals(0, stats.navigationCount)
        assertEquals(0, stats.dangerousElementsSkipped)
    }

    @Test
    fun getStats_WithClicks_CalculatesCoverage() {
        // Given: State with clickable elements
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)

        val clickableElements = listOf(
            createTestElement("Button", "Submit", "com.test:id/submit", isClickable = true),
            createTestElement("Button", "Cancel", "com.test:id/cancel", isClickable = true),
            createTestElement("Button", "Delete", "com.test:id/delete", isClickable = true),
            createTestElement("Button", "Share", "com.test:id/share", isClickable = true)
        )
        explorationState.recordElements(clickableElements)

        // Click 2 out of 4
        explorationState.recordClick(clickableElements[0])
        explorationState.recordClick(clickableElements[1])

        // When: Getting stats
        val stats = explorationState.getStats()

        // Then: Coverage is 50%
        assertEquals(50.0f, stats.coverage, 0.1f)
    }

    // ============================================================
    // Test: Additional Coverage Tests
    // ============================================================

    @Test
    fun recordNavigation_ValidTransition_RecordsCorrectly() {
        // Given: State with two screens
        explorationState.start()
        explorationState.beginExploring()

        val elements1 = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint1 = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements1)
        explorationState.recordScreen(fingerprint1)

        val elements2 = listOf(createTestElement("Button", "Back", "com.test:id/back"))
        val fingerprint2 = ScreenFingerprint.create("DetailActivity", TEST_PACKAGE, elements2)
        explorationState.recordScreen(fingerprint2)

        val triggerElement = createTestElement("Button", "Next", "com.test:id/next", isClickable = true)

        // When: Recording navigation
        explorationState.recordNavigation(
            fingerprint1.screenHash,
            fingerprint2.screenHash,
            triggerElement
        )

        // Then: Navigation is tracked
        val navHistory = explorationState.getNavigationHistory()
        assertEquals(1, navHistory.size)
        assertEquals(fingerprint1.screenHash, navHistory[0].fromScreenHash)
        assertEquals(fingerprint2.screenHash, navHistory[0].toScreenHash)
        assertEquals("Next", navHistory[0].triggerLabel)

        // Then: Callback is invoked
        verify {
            mockCallback.onNavigation(any())
        }
    }

    @Test
    fun recordDangerousElement_ValidReason_TracksCorrectly() {
        // Given: Fresh state
        explorationState.start()
        explorationState.beginExploring()

        val dangerousElement = createTestElement("Button", "Delete All", "com.test:id/delete_all", isClickable = true)
        val reason = DoNotClickReason.EXIT_ACTION

        // When: Recording dangerous element
        explorationState.recordDangerousElement(dangerousElement, reason)

        // Then: Element is tracked
        val dangerousElements = explorationState.getDangerousElements()
        assertEquals(1, dangerousElements.size)
        assertEquals("Delete All", dangerousElements[0].first.text)
        assertEquals(DoNotClickReason.EXIT_ACTION, dangerousElements[0].second)
    }

    @Test
    fun recordBackNavigation_WithBackStack_DecrementsDepth() {
        // Given: State with navigation history
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint1 = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        val fingerprint2 = ScreenFingerprint.create("DetailActivity", TEST_PACKAGE, elements)

        explorationState.recordScreen(fingerprint1)
        explorationState.recordScreen(fingerprint2)

        assertEquals(1, explorationState.currentDepth, "Should be at depth 1")

        // When: Recording back navigation
        val previousScreen = explorationState.recordBackNavigation()

        // Then: Depth decreases
        assertEquals(0, explorationState.currentDepth, "Should be back at depth 0")
        assertNotNull(previousScreen)
        assertEquals(fingerprint1.screenHash, previousScreen)
    }

    @Test
    fun reset_ClearsAllState() {
        // Given: State with data
        explorationState.start()
        explorationState.beginExploring()

        val elements = listOf(createTestElement("Button", "Click Me", "com.test:id/button"))
        val fingerprint = ScreenFingerprint.create("MainActivity", TEST_PACKAGE, elements)
        explorationState.recordScreen(fingerprint)
        explorationState.recordElements(elements)

        // When: Resetting
        explorationState.reset()

        // Then: State is cleared
        assertEquals(ExplorationPhase.IDLE, explorationState.phase)
        assertEquals(0, explorationState.startTimestamp)
        assertEquals(0, explorationState.currentDepth)
        assertEquals(0, explorationState.getElements().size)
        assertEquals(0, explorationState.getScreenFingerprints().size)
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private fun createTestElement(
        className: String,
        text: String,
        resourceId: String,
        isClickable: Boolean = false
    ): ElementInfo {
        return ElementInfo(
            className = "android.widget.$className",
            text = text,
            resourceId = resourceId,
            isClickable = isClickable,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )
    }
}
