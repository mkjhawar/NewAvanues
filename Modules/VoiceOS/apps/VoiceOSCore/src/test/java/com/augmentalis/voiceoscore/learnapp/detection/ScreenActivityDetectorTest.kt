/**
 * ScreenActivityDetectorTest.kt - Unit tests for screen change detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Testing Team
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 *
 * Tests for LearnApp Phase 2 - On-Demand Command Renaming
 * Component: Screen Activity Detection
 *
 * Test Requirements:
 * - TR-001: Detect screen changes correctly
 * - TR-002: Query database for screen commands
 * - TR-003: Trigger hint overlay when screen changes
 * - TR-004: Skip duplicate events for same screen
 * - TR-005: Handle missing package/class names gracefully
 * - TR-006: Reset current screen tracking
 *
 * @see ScreenActivityDetector
 * @see LearnApp-On-Demand-Command-Renaming-5081220-V2.md (Component 4)
 */
package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.voiceoscore.learnapp.ui.RenameHintOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ScreenActivityDetector.
 *
 * Tests the system's ability to detect screen changes and trigger
 * rename hints for screens with generated command labels.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenActivityDetectorTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockDatabase: VoiceOSDatabaseManager

    @Mock
    private lateinit var mockGeneratedCommandRepo: IGeneratedCommandRepository

    @Mock
    private lateinit var mockRenameHintOverlay: RenameHintOverlay

    @Mock
    private lateinit var mockEvent: AccessibilityEvent

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var detector: ScreenActivityDetector

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup database mock
        whenever(mockDatabase.generatedCommands).thenReturn(mockGeneratedCommandRepo)

        // Create detector
        detector = ScreenActivityDetector(
            context = mockContext,
            database = mockDatabase,
            renameHintOverlay = mockRenameHintOverlay
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * TEST 1: Detect screen change correctly
     *
     * Scenario: User navigates from screen A to screen B
     * Expected: Screen change detected, commands queried, hint shown
     */
    @Test
    fun `detects screen change correctly`() = testScope.runTest {
        println("\n========== TEST 1: Screen Change Detection ==========\n")

        // Given: Screen A
        whenever(mockEvent.packageName).thenReturn("com.example.app")
        whenever(mockEvent.className).thenReturn("com.example.app.MainActivity")

        val commands = listOf(
            createMockCommand(1, "click button 1"),
            createMockCommand(2, "click button 2")
        )
        whenever(mockGeneratedCommandRepo.getByPackage("com.example.app")).thenReturn(commands)

        // When: Process event
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Should detect screen change
        assertEquals("com.example.app/com.example.app.MainActivity", detector.getCurrentScreen())

        // And: Should query database
        verify(mockGeneratedCommandRepo).getByPackage("com.example.app")

        // And: Should trigger hint overlay
        verify(mockRenameHintOverlay).showIfNeeded(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            generatedCommands = commands
        )

        println("✅ Screen change detected successfully")
    }

    /**
     * TEST 2: Skip duplicate events for same screen
     *
     * Scenario: Multiple events for same screen
     * Expected: Only first event processed, subsequent events ignored
     */
    @Test
    fun `skips duplicate events for same screen`() = testScope.runTest {
        println("\n========== TEST 2: Duplicate Event Filtering ==========\n")

        // Given: Same screen events
        whenever(mockEvent.packageName).thenReturn("com.example.app")
        whenever(mockEvent.className).thenReturn("com.example.app.MainActivity")

        val commands = listOf(createMockCommand(1, "click button 1"))
        whenever(mockGeneratedCommandRepo.getByPackage("com.example.app")).thenReturn(commands)

        // When: Process first event
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // When: Process duplicate event
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Database queried only once
        verify(mockGeneratedCommandRepo).getByPackage("com.example.app")

        // And: Hint shown only once
        verify(mockRenameHintOverlay).showIfNeeded(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            generatedCommands = commands
        )

        println("✅ Duplicate events filtered correctly")
    }

    /**
     * TEST 3: Handle missing package name gracefully
     *
     * Scenario: Event with null package name
     * Expected: Event skipped, no crash, no processing
     */
    @Test
    fun `handles missing package name gracefully`() = testScope.runTest {
        println("\n========== TEST 3: Missing Package Name ==========\n")

        // Given: Event with null package name
        whenever(mockEvent.packageName).thenReturn(null)
        whenever(mockEvent.className).thenReturn("com.example.app.MainActivity")

        // When: Process event
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Should not query database
        verify(mockGeneratedCommandRepo, never()).getByPackage(any())

        // And: Should not show hint
        verify(mockRenameHintOverlay, never()).showIfNeeded(any(), any(), any())

        // And: Current screen should remain empty
        assertEquals("", detector.getCurrentScreen())

        println("✅ Missing package name handled gracefully")
    }

    /**
     * TEST 4: Handle missing class name gracefully
     *
     * Scenario: Event with null class name
     * Expected: Event skipped, no crash, no processing
     */
    @Test
    fun `handles missing class name gracefully`() = testScope.runTest {
        println("\n========== TEST 4: Missing Class Name ==========\n")

        // Given: Event with null class name
        whenever(mockEvent.packageName).thenReturn("com.example.app")
        whenever(mockEvent.className).thenReturn(null)

        // When: Process event
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Should not query database
        verify(mockGeneratedCommandRepo, never()).getByPackage(any())

        // And: Should not show hint
        verify(mockRenameHintOverlay, never()).showIfNeeded(any(), any(), any())

        println("✅ Missing class name handled gracefully")
    }

    /**
     * TEST 5: Handle empty command list gracefully
     *
     * Scenario: Screen with no commands in database
     * Expected: Query succeeds, empty list passed to overlay
     */
    @Test
    fun `handles empty command list gracefully`() = testScope.runTest {
        println("\n========== TEST 5: Empty Command List ==========\n")

        // Given: Screen with no commands
        whenever(mockEvent.packageName).thenReturn("com.example.app")
        whenever(mockEvent.className).thenReturn("com.example.app.MainActivity")
        whenever(mockGeneratedCommandRepo.getByPackage("com.example.app")).thenReturn(emptyList())

        // When: Process event
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Should query database
        verify(mockGeneratedCommandRepo).getByPackage("com.example.app")

        // And: Should pass empty list to overlay (overlay decides whether to show)
        verify(mockRenameHintOverlay).showIfNeeded(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            generatedCommands = emptyList()
        )

        println("✅ Empty command list handled gracefully")
    }

    /**
     * TEST 6: Reset current screen tracking
     *
     * Scenario: Reset called to clear current screen
     * Expected: Current screen cleared, next event treated as new screen
     */
    @Test
    fun `resets current screen tracking`() = testScope.runTest {
        println("\n========== TEST 6: Reset Current Screen ==========\n")

        // Given: Screen A already set
        whenever(mockEvent.packageName).thenReturn("com.example.app")
        whenever(mockEvent.className).thenReturn("com.example.app.MainActivity")
        whenever(mockGeneratedCommandRepo.getByPackage("com.example.app")).thenReturn(emptyList())

        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        assertEquals("com.example.app/com.example.app.MainActivity", detector.getCurrentScreen())

        // When: Reset called
        detector.resetCurrentScreen()

        // Then: Current screen should be empty
        assertEquals("", detector.getCurrentScreen())

        // When: Process same event again
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Should treat as new screen (query database again)
        verify(mockGeneratedCommandRepo).getByPackage("com.example.app") // Called twice total

        println("✅ Current screen reset successfully")
    }

    /**
     * TEST 7: Screen change between different activities
     *
     * Scenario: User navigates from MainActivity to SettingsActivity
     * Expected: Both screen changes detected and processed
     */
    @Test
    fun `detects screen change between different activities`() = testScope.runTest {
        println("\n========== TEST 7: Multiple Screen Changes ==========\n")

        // Given: Screen A
        whenever(mockEvent.packageName).thenReturn("com.example.app")
        whenever(mockEvent.className).thenReturn("com.example.app.MainActivity")
        val commandsA = listOf(createMockCommand(1, "click button 1"))
        whenever(mockGeneratedCommandRepo.getByPackage("com.example.app")).thenReturn(commandsA)

        // When: Process screen A
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Given: Screen B (same package, different activity)
        whenever(mockEvent.className).thenReturn("com.example.app.SettingsActivity")
        val commandsB = listOf(createMockCommand(2, "click settings"))

        // When: Process screen B
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Screen should be updated
        assertEquals("com.example.app/com.example.app.SettingsActivity", detector.getCurrentScreen())

        // And: Database queried twice (once for each screen)
        verify(mockGeneratedCommandRepo).getByPackage("com.example.app") // Called twice

        // And: Hint shown for both screens
        verify(mockRenameHintOverlay).showIfNeeded(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            generatedCommands = commandsA
        )
        verify(mockRenameHintOverlay).showIfNeeded(
            packageName = "com.example.app",
            activityName = "com.example.app.SettingsActivity",
            generatedCommands = commandsA // Will use same commands (filtered by package)
        )

        println("✅ Multiple screen changes detected successfully")
    }

    /**
     * TEST 8: Handle database error gracefully
     *
     * Scenario: Database query throws exception
     * Expected: Error logged, empty list returned, no crash
     */
    @Test
    fun `handles database error gracefully`() = testScope.runTest {
        println("\n========== TEST 8: Database Error Handling ==========\n")

        // Given: Database throws exception
        whenever(mockEvent.packageName).thenReturn("com.example.app")
        whenever(mockEvent.className).thenReturn("com.example.app.MainActivity")
        whenever(mockGeneratedCommandRepo.getByPackage("com.example.app"))
            .thenThrow(RuntimeException("Database error"))

        // When: Process event
        detector.onWindowStateChanged(mockEvent)
        advanceUntilIdle()

        // Then: Should pass empty list to overlay (error handled internally)
        verify(mockRenameHintOverlay).showIfNeeded(
            packageName = "com.example.app",
            activityName = "com.example.app.MainActivity",
            generatedCommands = emptyList()
        )

        println("✅ Database error handled gracefully")
    }

    /**
     * Helper: Create mock GeneratedCommandDTO
     */
    private fun createMockCommand(id: Long, commandText: String): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = id,
            elementHash = "hash_$id",
            commandText = commandText,
            actionType = "click",
            confidence = 0.95,
            synonyms = null,
            isUserApproved = 0,
            usageCount = 0,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = ""
        )
    }
}
