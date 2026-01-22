/**
 * ChecklistManagerTest.kt - Unit tests for ChecklistManager
 * Path: modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/exploration/ChecklistManagerTest.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-04
 *
 * Tests for element traversal checklist system (VOS-EXPLORE-001)
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.graphics.Rect
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Test suite for ChecklistManager
 *
 * Validates:
 * 1. Checklist initialization and screen tracking
 * 2. Element completion marking
 * 3. Progress calculation
 * 4. Export to markdown
 * 5. Multi-screen tracking
 */
class ChecklistManagerTest {

    private lateinit var checklistManager: ChecklistManager
    private lateinit var tempExportFile: File

    @Before
    fun setup() {
        checklistManager = ChecklistManager()
        tempExportFile = File.createTempFile("checklist_test", ".md")
    }

    @After
    fun teardown() {
        checklistManager.clear()
        if (tempExportFile.exists()) {
            tempExportFile.delete()
        }
    }

    /**
     * Test 1: Initialize checklist
     *
     * Validates:
     * - Checklist starts empty
     * - Package name is stored correctly
     */
    @Test
    fun testInitializeChecklist() {
        // Initialize
        checklistManager.startChecklist("com.example.app")

        // Get checklist
        val checklist = checklistManager.getChecklist()

        // Verify
        assertEquals("Package name should match", "com.example.app", checklist.packageName)
        assertEquals("Should start with 0 screens", 0, checklist.totalScreens)
        assertEquals("Should start with 0 elements", 0, checklist.totalElements)
        assertTrue("Start time should be set", checklist.startTime > 0)
    }

    /**
     * Test 2: Add screen with elements
     *
     * Validates:
     * - Screen is added to checklist
     * - Elements are tracked as pending
     * - Progress starts at 0%
     */
    @Test
    fun testAddScreen() {
        checklistManager.startChecklist("com.example.app")

        // Create test elements
        val elements = createTestElements(5)

        // Add screen
        checklistManager.addScreen(
            screenHash = "screen_hash_123",
            screenTitle = "Home Screen",
            elements = elements
        )

        // Verify
        val checklist = checklistManager.getChecklist()
        assertEquals("Should have 1 screen", 1, checklist.totalScreens)
        assertEquals("Should have 5 elements", 5, checklist.totalElements)
        assertEquals("Should have 0 completed", 0, checklist.completedElements)
        assertEquals("Progress should be 0%", 0, checklistManager.getOverallProgress())
    }

    /**
     * Test 3: Mark elements completed
     *
     * Validates:
     * - Elements move from pending to completed
     * - Progress updates correctly
     * - Completed count increases
     */
    @Test
    fun testMarkElementsCompleted() {
        checklistManager.startChecklist("com.example.app")

        val elements = createTestElements(10)
        checklistManager.addScreen("screen_1", "Screen 1", elements)

        // Mark first 5 elements as completed
        for (i in 0 until 5) {
            checklistManager.markElementCompleted("screen_1", elements[i].uuid!!)
        }

        // Verify
        val checklist = checklistManager.getChecklist()
        assertEquals("Should have 5 completed", 5, checklist.completedElements)
        assertEquals("Should have 10 total", 10, checklist.totalElements)
        assertEquals("Progress should be 50%", 50, checklistManager.getOverallProgress())

        // Verify screen-specific progress
        val screenChecklist = checklist.screens.first()
        assertEquals("Screen should have 5 completed", 5, screenChecklist.completedElements)
        assertEquals("Screen should have 5 pending", 5, screenChecklist.pendingElements.size)
        assertEquals("Screen should have 5 in completed list", 5, screenChecklist.completedElementsList.size)
    }

    /**
     * Test 4: Multi-screen tracking
     *
     * Validates:
     * - Multiple screens tracked independently
     * - Overall progress calculated across all screens
     * - Screen-specific progress isolated
     */
    @Test
    fun testMultiScreenTracking() {
        checklistManager.startChecklist("com.example.app")

        // Screen 1: 10 elements
        val screen1Elements = createTestElements(10, prefix = "s1_")
        checklistManager.addScreen("screen_1", "Screen 1", screen1Elements)

        // Screen 2: 5 elements
        val screen2Elements = createTestElements(5, prefix = "s2_")
        checklistManager.addScreen("screen_2", "Screen 2", screen2Elements)

        // Complete all elements on screen 1
        screen1Elements.forEach {
            checklistManager.markElementCompleted("screen_1", it.uuid!!)
        }

        // Complete 2 elements on screen 2
        for (i in 0 until 2) {
            checklistManager.markElementCompleted("screen_2", screen2Elements[i].uuid!!)
        }

        // Verify
        val checklist = checklistManager.getChecklist()
        assertEquals("Should have 2 screens", 2, checklist.totalScreens)
        assertEquals("Should have 15 total elements", 15, checklist.totalElements)
        assertEquals("Should have 12 completed elements", 12, checklist.completedElements)
        assertEquals("Progress should be 80%", 80, checklistManager.getOverallProgress())

        // Verify screen 1
        val screen1Checklist = checklist.screens.find { it.screenHash == "screen_1" }
        assertNotNull("Screen 1 should exist", screen1Checklist)
        assertEquals("Screen 1 should be 100% complete", 100, screen1Checklist!!.progressPercent)

        // Verify screen 2
        val screen2Checklist = checklist.screens.find { it.screenHash == "screen_2" }
        assertNotNull("Screen 2 should exist", screen2Checklist)
        assertEquals("Screen 2 should be 40% complete", 40, screen2Checklist!!.progressPercent)
    }

    /**
     * Test 5: Export to markdown
     *
     * Validates:
     * - Checklist exports to file successfully
     * - Markdown format is valid
     * - File contains expected sections
     */
    @Test
    fun testExportToMarkdown() {
        checklistManager.startChecklist("com.example.app")

        val elements = createTestElements(5)
        checklistManager.addScreen("screen_1", "Home Screen", elements)

        // Mark 2 elements completed
        checklistManager.markElementCompleted("screen_1", elements[0].uuid!!)
        checklistManager.markElementCompleted("screen_1", elements[1].uuid!!)

        // Export
        checklistManager.exportToFile(tempExportFile.absolutePath)

        // Verify file exists and has content
        assertTrue("Export file should exist", tempExportFile.exists())
        assertTrue("Export file should not be empty", tempExportFile.length() > 0)

        // Verify content
        val content = tempExportFile.readText()
        assertTrue("Should contain package name", content.contains("com.example.app"))
        assertTrue("Should contain screen title", content.contains("Home Screen"))
        assertTrue("Should contain progress", content.contains("40%"))
        assertTrue("Should contain completed section", content.contains("✅ Completed"))
        assertTrue("Should contain pending section", content.contains("⏳ Pending"))
        assertTrue("Should show 2 completed items", content.contains("- [x]"))
        assertTrue("Should show 3 pending items", content.contains("- [ ]"))
    }

    /**
     * Test 6: Clear checklist
     *
     * Validates:
     * - Clear removes all data
     * - Checklist can be reused after clear
     */
    @Test
    fun testClearChecklist() {
        checklistManager.startChecklist("com.example.app")

        val elements = createTestElements(10)
        checklistManager.addScreen("screen_1", "Screen 1", elements)

        // Clear
        checklistManager.clear()

        // Verify empty
        val checklist = checklistManager.getChecklist()
        assertEquals("Should have 0 screens after clear", 0, checklist.totalScreens)
        assertEquals("Should have 0 elements after clear", 0, checklist.totalElements)
        assertEquals("Progress should be 0 after clear", 0, checklistManager.getOverallProgress())

        // Verify can be reused
        checklistManager.startChecklist("com.new.app")
        checklistManager.addScreen("new_screen", "New Screen", elements)
        val newChecklist = checklistManager.getChecklist()
        assertEquals("Should work after clear", "com.new.app", newChecklist.packageName)
    }

    /**
     * Test 7: Mark non-existent element
     *
     * Validates:
     * - Gracefully handles marking element that doesn't exist
     * - No exceptions thrown
     * - State remains consistent
     */
    @Test
    fun testMarkNonExistentElement() {
        checklistManager.startChecklist("com.example.app")

        val elements = createTestElements(5)
        checklistManager.addScreen("screen_1", "Screen 1", elements)

        // Try to mark non-existent element
        checklistManager.markElementCompleted("screen_1", "non_existent_uuid")

        // Verify state unchanged
        val checklist = checklistManager.getChecklist()
        assertEquals("Completed count should still be 0", 0, checklist.completedElements)
    }

    /**
     * Test 8: Progress rounding
     *
     * Validates:
     * - Progress percentage rounds correctly
     * - No floating point issues
     */
    @Test
    fun testProgressRounding() {
        checklistManager.startChecklist("com.example.app")

        // 3 elements (33.33% per element)
        val elements = createTestElements(3)
        checklistManager.addScreen("screen_1", "Screen 1", elements)

        // Mark 1 completed (33.33%)
        checklistManager.markElementCompleted("screen_1", elements[0].uuid!!)
        assertEquals("Should round to 33%", 33, checklistManager.getOverallProgress())

        // Mark 2 completed (66.66%)
        checklistManager.markElementCompleted("screen_1", elements[1].uuid!!)
        assertEquals("Should round to 66%", 66, checklistManager.getOverallProgress())

        // Mark 3 completed (100%)
        checklistManager.markElementCompleted("screen_1", elements[2].uuid!!)
        assertEquals("Should be 100%", 100, checklistManager.getOverallProgress())
    }

    // Helper Functions

    /**
     * Create test elements with UUIDs
     */
    private fun createTestElements(count: Int, prefix: String = ""): List<ElementInfo> {
        return (1..count).map { i ->
            ElementInfo(
                className = "android.widget.Button",
                text = "Element $i",
                contentDescription = "Description $i",
                resourceId = "id/button_$i",
                isClickable = true,
                isEnabled = true,
                isScrollable = false,
                bounds = Rect(0, i * 100, 100, (i + 1) * 100),
                node = null, // No actual node needed for unit test
                uuid = "${prefix}uuid_$i"
            )
        }
    }
}
