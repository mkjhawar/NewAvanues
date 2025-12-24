/**
 * LearnAppCoreTest.kt - Unit tests for LearnAppCore
 *
 * Tests voice command generation, element processing, and batch operations.
 * Validates both IMMEDIATE and BATCH processing modes.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: Phase 1 Architecture Improvement Plan
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.core

import android.content.Context
import android.graphics.Rect
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for LearnAppCore
 *
 * Tests:
 * - Voice command generation with various label sources
 * - UUID generation and stability
 * - Processing modes (IMMEDIATE vs BATCH)
 * - Batch queue management
 * - Cross-platform fallback labels
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearnAppCoreTest {

    private lateinit var mockContext: Context
    private lateinit var mockDatabase: VoiceOSDatabaseManager
    private lateinit var mockCommandsRepository: IGeneratedCommandRepository
    private lateinit var mockUuidGenerator: ThirdPartyUuidGenerator
    private lateinit var learnAppCore: LearnAppCore

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockDatabase = mockk(relaxed = true)
        mockCommandsRepository = mockk(relaxed = true)
        mockUuidGenerator = mockk(relaxed = true)

        // Setup database mock - uses Repository pattern (SQLDelight), NOT DAO
        every { mockDatabase.generatedCommands } returns mockCommandsRepository
        coEvery { mockCommandsRepository.insert(any()) } returns 1L

        // Create LearnAppCore instance
        learnAppCore = LearnAppCore(mockContext, mockDatabase, mockUuidGenerator)
    }

    // ============================================================
    // Voice Command Generation Tests
    // ============================================================

    @Test
    fun generateVoiceCommand_WithText_ReturnsText() = runTest {
        // Given: Element with text label
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element in IMMEDIATE mode
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Command generated with text label
        assertTrue(result.success)
        assertNotNull(result.command)
        assertTrue(result.command!!.commandText.contains("submit"))
        assertEquals("click", result.command!!.actionType)
    }

    @Test
    fun generateVoiceCommand_WithContentDesc_ReturnsContentDesc() = runTest {
        // Given: Element with contentDescription only
        val element = ElementInfo(
            className = "android.widget.ImageButton",
            text = "",
            contentDescription = "Search",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element in IMMEDIATE mode
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Command generated with contentDescription label
        assertTrue(result.success)
        assertNotNull(result.command)
        assertTrue(result.command!!.commandText.contains("search"))
    }

    @Test
    fun generateVoiceCommand_WithResourceId_ReturnsId() = runTest {
        // Given: Element with resourceId only
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "",
            contentDescription = "",
            resourceId = "com.example.app:id/btn_login",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element in IMMEDIATE mode
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Command generated with resourceId label
        assertTrue(result.success)
        assertNotNull(result.command)
        assertTrue(result.command!!.commandText.contains("btn_login"))
    }

    @Test
    fun generateVoiceCommand_NoLabels_GeneratesFallback() = runTest {
        // Given: Actionable element with no semantic labels
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(100, 200, 200, 300),
            screenWidth = 1080,
            screenHeight = 1920,
            index = 0
        )

        // When: Process element in IMMEDIATE mode
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: CRITICAL - Actionable elements MUST ALWAYS get commands
        // With the fix, fallback labels are generated for ALL frameworks (including native)
        assertTrue("Actionable elements must ALWAYS get commands", result.success)
        assertNotNull(result.command)
        // Command will use fallback label (type + index): "click button 1"
        assertTrue(result.command!!.commandText.startsWith("click"))
    }

    @Test
    fun generateVoiceCommand_EditText_ReturnsTypeAction() = runTest {
        // Given: EditText element
        val element = ElementInfo(
            className = "android.widget.EditText",
            text = "",
            contentDescription = "Email",
            resourceId = "",
            isClickable = false,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Command generated with type action
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("type", result.command!!.actionType)
        assertTrue(result.command!!.commandText.contains("email"))
    }

    @Test
    fun generateVoiceCommand_ScrollableView_ReturnsScrollAction() = runTest {
        // Given: Scrollable element
        val element = ElementInfo(
            className = "androidx.recyclerview.widget.RecyclerView",
            text = "",
            contentDescription = "Feed",
            resourceId = "",
            isClickable = false,
            isEnabled = true,
            isScrollable = true,
            bounds = Rect(0, 0, 1080, 1920),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Command generated with scroll action
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals("scroll", result.command!!.actionType)
    }

    @Test
    fun generateVoiceCommand_WithSynonyms_GeneratesSynonyms() = runTest {
        // Given: Clickable element
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Login",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Command generated with synonyms (JSON array format)
        assertTrue(result.success)
        assertNotNull(result.command)
        val synonymsJson = result.command!!.synonyms ?: ""
        // Synonyms stored as JSON: ["tap login","press login","select login"]
        assertTrue("Should contain tap synonym", synonymsJson.contains("tap login"))
        assertTrue("Should contain press synonym", synonymsJson.contains("press login"))
        assertTrue("Should contain select synonym", synonymsJson.contains("select login"))
    }

    // ============================================================
    // Processing Mode Tests
    // ============================================================

    @Test
    fun processElement_Immediate_SavesDirectly() = runTest {
        // Given: Element to process
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        val commandSlot = slot<GeneratedCommandDTO>()
        coEvery { mockCommandsRepository.insert(capture(commandSlot)) } returns 1L

        // When: Process in IMMEDIATE mode
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Command inserted to database immediately
        assertTrue(result.success)
        coVerify(exactly = 1) { mockCommandsRepository.insert(any()) }
        assertEquals("click submit", commandSlot.captured.commandText)
    }

    @Test
    fun processElement_Batch_QueuesCommand() = runTest {
        // Given: Element to process
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process in BATCH mode
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.BATCH)

        // Then: Command queued, not inserted yet
        assertTrue(result.success)
        coVerify(exactly = 0) { mockCommandsRepository.insert(any()) }
        assertEquals(1, learnAppCore.getBatchQueueSize())
    }

    @Test
    fun flushBatch_InsertsAllQueuedCommands() = runTest {
        // Given: Multiple elements processed in BATCH mode
        val elements = listOf(
            ElementInfo(
                className = "android.widget.Button",
                text = "Button1",
                isClickable = true,
                bounds = Rect(0, 0, 100, 100),
                screenWidth = 1080,
                screenHeight = 1920
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Button2",
                isClickable = true,
                bounds = Rect(100, 0, 200, 100),
                screenWidth = 1080,
                screenHeight = 1920
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Button3",
                isClickable = true,
                bounds = Rect(200, 0, 300, 100),
                screenWidth = 1080,
                screenHeight = 1920
            )
        )

        // Track batch calls
        var capturedBatchSize = 0
        coEvery { mockCommandsRepository.insertBatch(any()) } answers {
            capturedBatchSize = firstArg<List<GeneratedCommandDTO>>().size
        }

        elements.forEach { element ->
            learnAppCore.processElement(element, "com.example.app", ProcessingMode.BATCH)
        }

        assertEquals(3, learnAppCore.getBatchQueueSize())

        // When: Flush batch
        learnAppCore.flushBatch()

        // Then: All commands inserted via batch (20x faster than sequential)
        coVerify(exactly = 1) { mockCommandsRepository.insertBatch(any()) }
        assertEquals(3, capturedBatchSize)
        assertEquals(0, learnAppCore.getBatchQueueSize())
    }

    @Test
    fun flushBatch_EmptyQueue_DoesNothing() = runTest {
        // Given: Empty batch queue

        // When: Flush batch
        learnAppCore.flushBatch()

        // Then: No database operations
        coVerify(exactly = 0) { mockCommandsRepository.insert(any()) }
    }

    @Test
    fun clearBatchQueue_RemovesQueuedCommands() = runTest {
        // Given: Commands in batch queue
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            isClickable = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        learnAppCore.processElement(element, "com.example.app", ProcessingMode.BATCH)
        assertEquals(1, learnAppCore.getBatchQueueSize())

        // When: Clear queue
        learnAppCore.clearBatchQueue()

        // Then: Queue is empty
        assertEquals(0, learnAppCore.getBatchQueueSize())
    }

    // ============================================================
    // VUID Generation Tests
    // ============================================================

    @Test
    fun processElement_GeneratesStableUUID() = runTest {
        // Given: Same element processed twice
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            resourceId = "com.example:id/btn_submit",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element twice
        val result1 = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)
        val result2 = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: UUID is stable (same for same element)
        assertTrue(result1.success)
        assertTrue(result2.success)
        assertEquals(result1.uuid, result2.uuid)
    }

    @Test
    fun processElement_DifferentElements_DifferentUUIDs() = runTest {
        // Given: Two different elements
        val element1 = ElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            isClickable = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        val element2 = ElementInfo(
            className = "android.widget.Button",
            text = "Cancel",
            isClickable = true,
            bounds = Rect(100, 0, 200, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process both elements
        val result1 = learnAppCore.processElement(element1, "com.example.app", ProcessingMode.IMMEDIATE)
        val result2 = learnAppCore.processElement(element2, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: UUIDs are different
        assertTrue(result1.success)
        assertTrue(result2.success)
        assertNotNull(result1.uuid)
        assertNotNull(result2.uuid)
        assertTrue(result1.uuid != result2.uuid)
    }

    // ============================================================
    // Confidence Score Tests
    // ============================================================

    @Test
    fun processElement_HighQualityLabel_HighConfidence() = runTest {
        // Given: Element with good text label (semantic)
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Submit Form",
            isClickable = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: High confidence for semantic labels (0.95)
        assertTrue(result.success)
        assertNotNull(result.command)
        assertEquals(0.95, result.command!!.confidence, 0.01)
    }

    // ============================================================
    // Error Handling Tests
    // ============================================================

    @Test
    fun processElement_DatabaseError_ReturnsFailure() = runTest {
        // Given: Database throws exception
        coEvery { mockCommandsRepository.insert(any()) } throws RuntimeException("Database error")

        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Submit",
            isClickable = true,
            bounds = Rect(0, 0, 100, 100),
            screenWidth = 1080,
            screenHeight = 1920
        )

        // When: Process element
        val result = learnAppCore.processElement(element, "com.example.app", ProcessingMode.IMMEDIATE)

        // Then: Failure result returned
        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Database error"))
    }
}
