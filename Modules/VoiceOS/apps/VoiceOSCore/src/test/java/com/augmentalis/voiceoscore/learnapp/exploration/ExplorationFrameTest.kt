/**
 * ExplorationFrameTest.kt - Unit tests for ExplorationFrame data class
 * Path: modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/exploration/ExplorationFrameTest.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-04
 *
 * Tests for iterative DFS stack frame (VOS-EXPLORE-001)
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.graphics.Rect
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test suite for ExplorationFrame
 *
 * Note: ExplorationFrame is a private data class in ExplorationEngine.
 * These tests verify the expected behavior of the frame structure.
 *
 * Validates:
 * 1. Frame initialization
 * 2. Element iteration (hasMoreElements, getNextElement)
 * 3. Current element index tracking
 * 4. Parent-child relationships
 */
class ExplorationFrameTest {

    /**
     * Test 1: Frame initialization
     *
     * Validates:
     * - Frame starts at index 0
     * - Has correct number of elements
     * - Depth is set correctly
     */
    @Test
    fun testFrameInitialization() {
        val elements = createTestElements(5)
        val screenState = createTestScreenState("screen_1")

        // Create frame (simulating ExplorationFrame structure)
        val frameData = FrameData(
            screenHash = "screen_1",
            screenState = screenState,
            elements = elements.toMutableList(),
            currentElementIndex = 0,
            depth = 1,
            parentScreenHash = null
        )

        // Verify
        assertEquals("Should start at index 0", 0, frameData.currentElementIndex)
        assertEquals("Should have 5 elements", 5, frameData.elements.size)
        assertEquals("Depth should be 1", 1, frameData.depth)
        assertNull("Root frame should have no parent", frameData.parentScreenHash)
        assertTrue("Should have more elements", frameData.hasMoreElements())
    }

    /**
     * Test 2: Element iteration
     *
     * Validates:
     * - getNextElement returns elements in order
     * - currentElementIndex increments correctly
     * - hasMoreElements returns false when exhausted
     */
    @Test
    fun testElementIteration() {
        val elements = createTestElements(3)
        val screenState = createTestScreenState("screen_1")

        val frameData = FrameData(
            screenHash = "screen_1",
            screenState = screenState,
            elements = elements.toMutableList(),
            currentElementIndex = 0,
            depth = 0,
            parentScreenHash = null
        )

        // Iterate through elements
        assertTrue("Should have element 1", frameData.hasMoreElements())
        val elem1 = frameData.getNextElement()
        assertNotNull("Element 1 should not be null", elem1)
        assertEquals("Element 1 should have correct UUID", "uuid_1", elem1?.uuid)
        assertEquals("Index should be 1 after first get", 1, frameData.currentElementIndex)

        assertTrue("Should have element 2", frameData.hasMoreElements())
        val elem2 = frameData.getNextElement()
        assertNotNull("Element 2 should not be null", elem2)
        assertEquals("Element 2 should have correct UUID", "uuid_2", elem2?.uuid)
        assertEquals("Index should be 2 after second get", 2, frameData.currentElementIndex)

        assertTrue("Should have element 3", frameData.hasMoreElements())
        val elem3 = frameData.getNextElement()
        assertNotNull("Element 3 should not be null", elem3)
        assertEquals("Element 3 should have correct UUID", "uuid_3", elem3?.uuid)
        assertEquals("Index should be 3 after third get", 3, frameData.currentElementIndex)

        assertFalse("Should have no more elements", frameData.hasMoreElements())
        val elem4 = frameData.getNextElement()
        assertNull("Should return null when exhausted", elem4)
    }

    /**
     * Test 3: Empty frame
     *
     * Validates:
     * - Frame with no elements handled correctly
     * - hasMoreElements returns false immediately
     */
    @Test
    fun testEmptyFrame() {
        val screenState = createTestScreenState("screen_1")

        val frameData = FrameData(
            screenHash = "screen_1",
            screenState = screenState,
            elements = mutableListOf(),
            currentElementIndex = 0,
            depth = 0,
            parentScreenHash = null
        )

        // Verify
        assertFalse("Empty frame should have no elements", frameData.hasMoreElements())
        assertNull("Empty frame should return null", frameData.getNextElement())
    }

    /**
     * Test 4: Parent-child relationship
     *
     * Validates:
     * - Child frame stores parent hash
     * - Depth increases by 1
     */
    @Test
    fun testParentChildRelationship() {
        val elements = createTestElements(3)
        val parentScreenState = createTestScreenState("parent_screen")
        val childScreenState = createTestScreenState("child_screen")

        val parentFrame = FrameData(
            screenHash = "parent_screen",
            screenState = parentScreenState,
            elements = elements.toMutableList(),
            currentElementIndex = 0,
            depth = 0,
            parentScreenHash = null
        )

        val childFrame = FrameData(
            screenHash = "child_screen",
            screenState = childScreenState,
            elements = elements.toMutableList(),
            currentElementIndex = 0,
            depth = parentFrame.depth + 1,
            parentScreenHash = parentFrame.screenHash
        )

        // Verify
        assertEquals("Child depth should be parent + 1", parentFrame.depth + 1, childFrame.depth)
        assertEquals("Child should store parent hash", "parent_screen", childFrame.parentScreenHash)
        assertNull("Parent should have no parent", parentFrame.parentScreenHash)
    }

    /**
     * Test 5: Partial iteration
     *
     * Validates:
     * - Can iterate partway through elements
     * - State is preserved correctly
     */
    @Test
    fun testPartialIteration() {
        val elements = createTestElements(10)
        val screenState = createTestScreenState("screen_1")

        val frameData = FrameData(
            screenHash = "screen_1",
            screenState = screenState,
            elements = elements.toMutableList(),
            currentElementIndex = 0,
            depth = 0,
            parentScreenHash = null
        )

        // Iterate 5 times
        repeat(5) {
            assertTrue("Should have more elements", frameData.hasMoreElements())
            frameData.getNextElement()
        }

        // Verify state
        assertEquals("Should be at index 5", 5, frameData.currentElementIndex)
        assertTrue("Should still have 5 elements remaining", frameData.hasMoreElements())

        // Verify remaining count
        var remaining = 0
        while (frameData.hasMoreElements()) {
            frameData.getNextElement()
            remaining++
        }
        assertEquals("Should have 5 remaining elements", 5, remaining)
    }

    /**
     * Test 6: Deep nesting
     *
     * Validates:
     * - Frames can be nested deeply
     * - Depth tracking works for deep hierarchies
     */
    @Test
    fun testDeepNesting() {
        val maxDepth = 10
        val frames = mutableListOf<FrameData>()

        // Create chain of frames
        var parentHash: String? = null
        for (depth in 0 until maxDepth) {
            val elements = createTestElements(2)
            val screenState = createTestScreenState("screen_$depth")

            val frame = FrameData(
                screenHash = "screen_$depth",
                screenState = screenState,
                elements = elements.toMutableList(),
                currentElementIndex = 0,
                depth = depth,
                parentScreenHash = parentHash
            )

            frames.add(frame)
            parentHash = frame.screenHash
        }

        // Verify chain
        assertEquals("Should have 10 frames", maxDepth, frames.size)

        // Verify first frame (root)
        assertEquals("First frame should be depth 0", 0, frames[0].depth)
        assertNull("First frame should have no parent", frames[0].parentScreenHash)

        // Verify last frame
        assertEquals("Last frame should be depth 9", 9, frames[9].depth)
        assertEquals("Last frame should have correct parent", "screen_8", frames[9].parentScreenHash)

        // Verify each child points to correct parent
        for (i in 1 until maxDepth) {
            assertEquals("Frame $i should point to parent ${i-1}",
                frames[i - 1].screenHash,
                frames[i].parentScreenHash)
        }
    }

    // Helper Classes and Functions

    /**
     * Data class simulating ExplorationFrame (which is private in ExplorationEngine)
     */
    private data class FrameData(
        val screenHash: String,
        val screenState: ScreenState,
        val elements: MutableList<ElementInfo>,
        var currentElementIndex: Int = 0,
        val depth: Int,
        val parentScreenHash: String? = null
    ) {
        fun hasMoreElements(): Boolean = currentElementIndex < elements.size
        fun getNextElement(): ElementInfo? {
            return if (hasMoreElements()) {
                elements[currentElementIndex].also { currentElementIndex++ }
            } else null
        }
    }

    /**
     * Create test elements
     */
    private fun createTestElements(count: Int): List<ElementInfo> {
        return (1..count).map { i ->
            ElementInfo(
                uuid = "uuid_$i",
                className = "android.widget.Button",
                text = "Element $i",
                contentDescription = "Description $i",
                resourceId = "id/button_$i",
                isClickable = true,
                isEnabled = true,
                isScrollable = false,
                bounds = Rect(0, i * 100, 100, (i + 1) * 100),
                node = null
            )
        }
    }

    /**
     * Create test screen state
     */
    private fun createTestScreenState(hash: String): ScreenState {
        return ScreenState(
            hash = hash,
            packageName = "com.example.app",
            activityName = "MainActivity",
            depth = 0,
            timestamp = System.currentTimeMillis(),
            elementCount = 5
        )
    }
}
