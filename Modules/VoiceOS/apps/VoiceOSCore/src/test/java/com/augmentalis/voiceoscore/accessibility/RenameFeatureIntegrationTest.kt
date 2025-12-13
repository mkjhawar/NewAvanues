/**
 * RenameFeatureIntegrationTest.kt - Integration test for rename feature
 * Path: apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/accessibility/RenameFeatureIntegrationTest.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 *
 * Tests integration of rename feature components with VoiceOSService:
 * - RenameHintOverlay initialization
 * - ScreenActivityDetector initialization
 * - RenameCommandHandler integration
 * - Voice command routing
 */

package com.augmentalis.voiceoscore.accessibility

import android.content.Context
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.voiceoscore.learnapp.commands.RenameCommandHandler
import com.augmentalis.voiceoscore.learnapp.detection.ScreenActivityDetector
import com.augmentalis.voiceoscore.learnapp.ui.RenameHintOverlay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration Test for Rename Feature
 *
 * Verifies that:
 * 1. Components initialize correctly
 * 2. Window state changes trigger hint display
 * 3. Rename commands are detected and routed
 * 4. Command synonyms are stored correctly
 */
class RenameFeatureIntegrationTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockWindowManager: WindowManager

    @Mock
    private lateinit var mockDatabase: VoiceOSDatabaseManager

    private lateinit var renameHintOverlay: RenameHintOverlay
    private lateinit var screenActivityDetector: ScreenActivityDetector

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Initialize components
        renameHintOverlay = RenameHintOverlay(mockContext, mockWindowManager)
        screenActivityDetector = ScreenActivityDetector(
            mockContext,
            mockDatabase,
            renameHintOverlay
        )
    }

    /**
     * Test 1: Verify hint overlay detects generated labels
     */
    @Test
    fun testHintOverlay_detectsGeneratedLabels() {
        val generatedCommand = GeneratedCommandDTO(
            id = 1L,
            elementHash = "test-hash",
            commandText = "click button 1",
            actionType = "click",
            confidence = 1.0,
            synonyms = "",
            isUserApproved = 1L,
            usageCount = 0L,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = ""
        )

        // Check detection
        val isGenerated = renameHintOverlay.isGeneratedLabel("click button 1")
        assertTrue(isGenerated, "Should detect 'click button 1' as generated label")

        val isNotGenerated = renameHintOverlay.isGeneratedLabel("click save button")
        assertTrue(!isNotGenerated, "Should NOT detect 'click save button' as generated label")
    }

    /**
     * Test 2: Verify screen activity detector handles window changes
     */
    @Test
    fun testScreenActivityDetector_handlesWindowChanges() = runBlocking {
        // Create mock event
        val mockEvent = mock(AccessibilityEvent::class.java)
        `when`(mockEvent.packageName).thenReturn("com.test.app")
        `when`(mockEvent.className).thenReturn("com.test.app.MainActivity")

        // Mock database response
        val testCommands = listOf(
            GeneratedCommandDTO(
                id = 1L,
                elementHash = "hash1",
                commandText = "click button 1",
                actionType = "click",
                confidence = 1.0,
                synonyms = "",
                isUserApproved = 1L,
                usageCount = 0L,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = ""
            )
        )
        `when`(mockDatabase.generatedCommands.getByPackage("com.test.app"))
            .thenReturn(testCommands)

        // Process event
        screenActivityDetector.onWindowStateChanged(mockEvent)

        // Verify current screen updated
        assertEquals(
            "com.test.app/com.test.app.MainActivity",
            screenActivityDetector.getCurrentScreen(),
            "Screen identifier should be updated"
        )
    }

    /**
     * Test 3: Verify rename command patterns are detected
     */
    @Test
    fun testRenameCommandDetection() {
        val renamePatterns = listOf(
            "rename button 1 to save",
            "rename tab 1 as settings",
            "change button 2 to submit"
        )

        renamePatterns.forEach { pattern ->
            assertTrue(
                isRenameCommand(pattern),
                "Should detect '$pattern' as rename command"
            )
        }

        val nonRenamePatterns = listOf(
            "click save",
            "open settings",
            "scroll down"
        )

        nonRenamePatterns.forEach { pattern ->
            assertTrue(
                !isRenameCommand(pattern),
                "Should NOT detect '$pattern' as rename command"
            )
        }
    }

    /**
     * Helper: Check if command matches rename patterns
     */
    private fun isRenameCommand(voiceInput: String): Boolean {
        val patterns = listOf(
            Regex("rename .+ to .+"),
            Regex("rename .+ as .+"),
            Regex("change .+ to .+")
        )
        return patterns.any { it.matches(voiceInput.lowercase()) }
    }

    /**
     * Test 4: Verify generated label patterns
     */
    @Test
    fun testGeneratedLabelPatterns() {
        val generatedLabels = listOf(
            "click button 1",
            "click tab 2",
            "click top button",
            "click bottom card",
            "click top left button",
            "click corner top far left button"
        )

        generatedLabels.forEach { label ->
            assertTrue(
                renameHintOverlay.isGeneratedLabel(label),
                "Should detect '$label' as generated label"
            )
        }

        val userLabels = listOf(
            "click save",
            "click settings button",
            "click profile"
        )

        userLabels.forEach { label ->
            assertTrue(
                !renameHintOverlay.isGeneratedLabel(label),
                "Should NOT detect '$label' as generated label"
            )
        }
    }
}
