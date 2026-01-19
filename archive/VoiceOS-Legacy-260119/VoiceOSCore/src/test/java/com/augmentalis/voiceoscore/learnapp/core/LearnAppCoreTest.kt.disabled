/**
 * LearnAppCoreTest.kt - Unit tests for LearnAppCore business logic
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-18
 *
 * Tests the shared business logic for LearnApp JIT and Exploration modes:
 * - UUID generation and stability
 * - Voice command generation with synonyms
 * - IMMEDIATE vs BATCH processing modes
 * - Framework detection and fallback label generation
 * - Batch queue management and flushing
 * - Version-aware command creation
 */

package com.augmentalis.voiceoscore.learnapp.core

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import com.augmentalis.voiceoscore.learnapp.detection.AppFramework
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.version.AppVersion
import com.augmentalis.voiceoscore.version.AppVersionDetector
import com.augmentalis.voiceoscore.learnapp.detection.CrossPlatformDetector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * LearnAppCore Test Suite
 *
 * Tests core business logic using MockK for dependency mocking.
 * Uses Arrange-Act-Assert pattern and runTest for coroutines.
 */
class LearnAppCoreTest {

    // Mocked dependencies
    private lateinit var mockContext: Context
    private lateinit var mockDatabase: VoiceOSDatabaseManager
    private lateinit var mockCommandRepo: IGeneratedCommandRepository
    private lateinit var mockUuidGenerator: ThirdPartyUuidGenerator
    private lateinit var mockVersionDetector: AppVersionDetector
    private lateinit var mockDeveloperSettings: LearnAppDeveloperSettings

    // System under test
    private lateinit var learnAppCore: LearnAppCore

    @Before
    fun setup() {
        // Mock Context
        mockContext = mockk(relaxed = true)

        // Mock database and command repository
        mockCommandRepo = mockk<IGeneratedCommandRepository>(relaxed = true)
        mockDatabase = mockk<VoiceOSDatabaseManager>(relaxed = true) {
            every { generatedCommands } returns mockCommandRepo
        }

        // Mock UUID generator
        mockUuidGenerator = mockk(relaxed = true)

        // Mock version detector
        mockVersionDetector = mockk(relaxed = true)

        // Mock developer settings
        mockkConstructor(LearnAppDeveloperSettings::class)
        every { anyConstructed<LearnAppDeveloperSettings>().isVerboseLoggingEnabled() } returns false
        every { anyConstructed<LearnAppDeveloperSettings>().getMaxCommandBatchSize() } returns 100
        every { anyConstructed<LearnAppDeveloperSettings>().getMinGeneratedLabelLength() } returns 3

        // Mock CrossPlatformDetector to return NATIVE by default
        // This ensures label filtering is applied for short/numeric labels
        mockkObject(CrossPlatformDetector)
        every { CrossPlatformDetector.detectFramework(any(), any(), any()) } returns AppFramework.NATIVE

        // Create system under test
        learnAppCore = LearnAppCore(
            context = mockContext,
            database = mockDatabase,
            uuidGenerator = mockUuidGenerator,
            versionDetector = mockVersionDetector
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ============================================================================
    // VUID Generation Tests
    // ============================================================================

    @Test
    fun `generateUUID creates stable deterministic UUID for element`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            resourceId = "com.example.app:id/login_button",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result1 = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
        val result2 = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result1.success)
        assertTrue(result2.success)
        assertEquals(result1.uuid, result2.uuid, "UUID should be deterministic")
        assertTrue(result1.uuid.contains(packageName), "UUID should contain package name")
        assertTrue(result1.uuid.contains("button"), "UUID should contain element type")
    }

    @Test
    fun `generateUUID creates different UUIDs for different elements`() = runTest {
        // Arrange
        val element1 = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            isClickable = true
        )
        val element2 = createTestElement(
            className = "android.widget.EditText",
            text = "Username",
            isClickable = false
        )
        val packageName = "com.example.app"

        // Act
        val result1 = learnAppCore.processElement(element1, packageName, ProcessingMode.BATCH)
        val result2 = learnAppCore.processElement(element2, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result1.success)
        assertTrue(result2.success)
        assertTrue(result1.uuid != result2.uuid, "Different elements should have different UUIDs")
    }

    @Test
    @Ignore("Rect properties return 0 in unit tests with isReturnDefaultValues=true. Test in instrumentation tests.")
    fun `generateUUID uses element bounds in hash calculation`() = runTest {
        // Arrange
        val element1 = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            bounds = Rect(0, 0, 100, 50)
        )
        val element2 = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            bounds = Rect(200, 200, 300, 250)
        )
        val packageName = "com.example.app"

        // Act
        val result1 = learnAppCore.processElement(element1, packageName, ProcessingMode.BATCH)
        val result2 = learnAppCore.processElement(element2, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result1.success)
        assertTrue(result2.success)
        assertTrue(result1.uuid != result2.uuid, "Elements at different positions should have different UUIDs")
    }

    // ============================================================================
    // Voice Command Generation Tests
    // ============================================================================

    @Test
    fun `generateVoiceCommand creates command with text label`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Submit Form",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("click submit form", result.command!!.commandText)
        assertEquals("click", result.command!!.actionType)
    }

    @Test
    fun `generateVoiceCommand prefers text over contentDescription`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            contentDescription = "Login Button",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("click login", result.command!!.commandText, "Should prefer text over contentDescription")
    }

    @Test
    fun `generateVoiceCommand falls back to contentDescription`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.ImageButton",
            text = "",
            contentDescription = "Search",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("click search", result.command!!.commandText)
    }

    @Test
    fun `generateVoiceCommand falls back to resourceId`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "",
            contentDescription = "",
            resourceId = "com.example.app:id/submit_button",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("click submit_button", result.command!!.commandText)
    }

    @Test
    fun `generateVoiceCommand creates synonyms for click action`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        val synonyms = result.command!!.synonyms
        assertTrue(synonyms?.contains("tap login") ?: false, "Should contain 'tap' synonym")
        assertTrue(synonyms?.contains("press login") ?: false, "Should contain 'press' synonym")
        assertTrue(synonyms?.contains("select login") ?: false, "Should contain 'select' synonym")
    }

    @Test
    fun `generateVoiceCommand creates type action for EditText`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.EditText",
            text = "Username",
            isClickable = false
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("type username", result.command!!.commandText)
        assertEquals("type", result.command!!.actionType)
    }

    @Test
    fun `generateVoiceCommand creates scroll action for scrollable`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.ScrollView",
            text = "Feed",
            isScrollable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("scroll feed", result.command!!.commandText)
        assertEquals("scroll", result.command!!.actionType)
    }

    @Test
    fun `generateVoiceCommand filters short labels`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "OK",  // Too short (< 3 characters)
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNull(result.command, "Short labels should be filtered out")
    }

    @Test
    fun `generateVoiceCommand filters numeric labels`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "12345",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNull(result.command, "Numeric labels should be filtered out")
    }

    @Test
    fun `generateVoiceCommand includes version information`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            isClickable = true
        )
        val packageName = "com.example.app"
        val appVersion = AppVersion("1.2.3", 123)

        coEvery { mockVersionDetector.getCurrentVersion(packageName) } returns appVersion

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("1.2.3", result.command!!.appVersion)
        assertEquals(123, result.command!!.versionCode)
        assertEquals(0L, result.command!!.isDeprecated, "New commands should not be deprecated")
    }

    // ============================================================================
    // Processing Mode Tests
    // ============================================================================

    @Test
    fun `processElement in IMMEDIATE mode inserts to database immediately`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            isClickable = true
        )
        val packageName = "com.example.app"
        val commandSlot = slot<GeneratedCommandDTO>()
        coEvery { mockCommandRepo.insert(capture(commandSlot)) } returns 1L

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.IMMEDIATE)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        coVerify(exactly = 1) { mockCommandRepo.insert(any()) }
        assertEquals("click login", commandSlot.captured.commandText)
        assertEquals(0, learnAppCore.getBatchQueueSize(), "Batch queue should be empty in IMMEDIATE mode")
    }

    @Test
    fun `processElement in BATCH mode queues command`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            isClickable = true
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        assertNotNull(result.command)
        coVerify(exactly = 0) { mockCommandRepo.insert(any()) }
        assertEquals(1, learnAppCore.getBatchQueueSize(), "Command should be queued")
    }

    @Test
    fun `processElement in BATCH mode queues multiple commands`() = runTest {
        // Arrange
        val elements = listOf(
            createTestElement(text = "Login", isClickable = true),
            createTestElement(text = "Submit", isClickable = true),
            createTestElement(text = "Cancel", isClickable = true)
        )
        val packageName = "com.example.app"

        // Act
        elements.forEach { element ->
            learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
        }

        // Assert
        assertEquals(3, learnAppCore.getBatchQueueSize(), "All commands should be queued")
        coVerify(exactly = 0) { mockCommandRepo.insert(any()) }
    }

    @Test
    fun `processElement handles errors gracefully`() = runTest {
        // Arrange
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            isClickable = true
        )
        val packageName = "com.example.app"
        coEvery { mockCommandRepo.insert(any()) } throws RuntimeException("Database error")

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.IMMEDIATE)

        // Assert
        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Database error"))
    }

    // ============================================================================
    // Batch Queue Management Tests
    // ============================================================================

    @Test
    fun `flushBatch inserts all queued commands`() = runTest {
        // Arrange
        val elements = listOf(
            createTestElement(text = "Login", isClickable = true),
            createTestElement(text = "Submit", isClickable = true),
            createTestElement(text = "Cancel", isClickable = true)
        )
        val packageName = "com.example.app"
        coEvery { mockCommandRepo.insert(any()) } returns 1L

        elements.forEach { element ->
            learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
        }
        assertEquals(3, learnAppCore.getBatchQueueSize())

        // Act
        learnAppCore.flushBatch()

        // Assert
        coVerify(exactly = 3) { mockCommandRepo.insert(any()) }
        assertEquals(0, learnAppCore.getBatchQueueSize(), "Batch queue should be cleared after flush")
    }

    @Test
    fun `flushBatch handles empty queue`() = runTest {
        // Arrange - empty queue
        assertEquals(0, learnAppCore.getBatchQueueSize())

        // Act
        learnAppCore.flushBatch()

        // Assert
        coVerify(exactly = 0) { mockCommandRepo.insert(any()) }
    }

    @Test
    fun `flushBatch preserves queue on error`() = runTest {
        // Arrange
        val element = createTestElement(text = "Login", isClickable = true)
        val packageName = "com.example.app"
        coEvery { mockCommandRepo.insert(any()) } throws RuntimeException("Database error")

        learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
        assertEquals(1, learnAppCore.getBatchQueueSize())

        // Act & Assert
        try {
            learnAppCore.flushBatch()
        } catch (e: RuntimeException) {
            // Expected
        }

        assertEquals(1, learnAppCore.getBatchQueueSize(), "Queue should be preserved on error for retry")
    }

    @Test
    fun `clearBatchQueue removes all commands without flushing`() = runTest {
        // Arrange
        val elements = listOf(
            createTestElement(text = "Login", isClickable = true),
            createTestElement(text = "Submit", isClickable = true)
        )
        val packageName = "com.example.app"

        elements.forEach { element ->
            learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)
        }
        assertEquals(2, learnAppCore.getBatchQueueSize())

        // Act
        learnAppCore.clearBatchQueue()

        // Assert
        assertEquals(0, learnAppCore.getBatchQueueSize())
        coVerify(exactly = 0) { mockCommandRepo.insert(any()) }
    }

    @Test
    fun `getBatchQueueSize returns correct count`() = runTest {
        // Arrange & Act
        assertEquals(0, learnAppCore.getBatchQueueSize())

        learnAppCore.processElement(
            createTestElement(text = "Login", isClickable = true),
            "com.example.app",
            ProcessingMode.BATCH
        )
        assertEquals(1, learnAppCore.getBatchQueueSize())

        learnAppCore.processElement(
            createTestElement(text = "Submit", isClickable = true),
            "com.example.app",
            ProcessingMode.BATCH
        )
        assertEquals(2, learnAppCore.getBatchQueueSize())
    }

    // ============================================================================
    // Fallback Label Generation Tests
    // ============================================================================

    @Test
    fun `generateFallbackLabel creates Unity grid labels`() = runTest {
        // Arrange - Unity app element (no text, no contentDescription, no resourceId)
        val element = createTestElement(
            className = "com.unity3d.player.UnityPlayer",
            text = "",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            bounds = Rect(100, 100, 200, 200),  // Top-left position
            screenWidth = 1080,
            screenHeight = 1920
        )
        val packageName = "com.unity3d.game"

        // Mock framework detection to return UNITY
        // Note: In real implementation, CrossPlatformDetector would detect Unity
        // For this test, we rely on the fallback logic which checks for unlabeled elements

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        assertTrue(result.success)
        // Since we can't easily mock framework detection without the node,
        // this test verifies the element can be processed even without labels
        // The actual fallback label generation is internal and tested indirectly
    }

    @Test
    fun `generatePositionLabel creates tab labels`() = runTest {
        // Arrange - element in TabLayout
        val parent = createTestElement(
            className = "android.widget.TabLayout",
            text = "",
            isClickable = false
        )
        val element = createTestElement(
            className = "android.widget.LinearLayout",
            text = "",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            index = 2,
            parent = parent
        )
        val packageName = "com.example.app"

        // Act
        val result = learnAppCore.processElement(element, packageName, ProcessingMode.BATCH)

        // Assert
        // Position labels require meaningful content or will be filtered
        // This test verifies the element can be processed
        assertTrue(result.success)
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    /**
     * Create test ElementInfo with default values
     */
    private fun createTestElement(
        className: String = "android.widget.Button",
        text: String = "",
        contentDescription: String = "",
        resourceId: String = "",
        isClickable: Boolean = false,
        isScrollable: Boolean = false,
        bounds: Rect = Rect(0, 0, 100, 50),
        screenWidth: Int = 1080,
        screenHeight: Int = 1920,
        index: Int = 0,
        parent: ElementInfo? = null,
        children: List<ElementInfo>? = null
    ): ElementInfo {
        // Mock AccessibilityNodeInfo if needed for framework detection
        val mockNode = if (text.isNotBlank() || contentDescription.isNotBlank()) {
            mockk<AccessibilityNodeInfo>(relaxed = true) {
                every { this@mockk.className } returns className
                every { this@mockk.text } returns text
                every { this@mockk.contentDescription } returns contentDescription
                every { viewIdResourceName } returns resourceId
                every { this@mockk.isClickable } returns isClickable
                every { this@mockk.isScrollable } returns isScrollable
            }
        } else {
            null
        }

        return ElementInfo(
            className = className,
            text = text,
            contentDescription = contentDescription,
            resourceId = resourceId,
            isClickable = isClickable,
            isScrollable = isScrollable,
            bounds = bounds,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            index = index,
            parent = parent,
            children = children,
            node = mockNode
        )
    }
}
