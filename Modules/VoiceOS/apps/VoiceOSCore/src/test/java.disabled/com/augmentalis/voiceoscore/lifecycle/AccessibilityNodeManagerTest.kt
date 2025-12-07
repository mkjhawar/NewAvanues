/**
 * AccessibilityNodeManagerTest.kt - TDD Tests for AccessibilityNodeInfo Lifecycle Management
 *
 * YOLO Phase 1 - Critical Issue #2: Missing Node Recycling in Error Paths
 *
 * Test Strategy:
 * - RED: Write comprehensive failing tests first
 * - GREEN: Implement minimal code to pass tests
 * - REFACTOR: Optimize and clean up
 *
 * Coverage Target: 100% (critical path)
 */
package com.augmentalis.voiceoscore.lifecycle

import android.view.accessibility.AccessibilityNodeInfo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith

/**
 * Test suite for AccessibilityNodeManager RAII pattern
 *
 * Tests verify:
 * 1. All nodes recycled in success path
 * 2. All nodes recycled when exception thrown
 * 3. All nodes recycled on early return
 * 4. Depth limit enforcement
 * 5. Circular reference detection
 * 6. Memory leak prevention
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccessibilityNodeManagerTest {

    @Mock
    private lateinit var mockRootNode: AccessibilityNodeInfo

    @Mock
    private lateinit var mockChildNode1: AccessibilityNodeInfo

    @Mock
    private lateinit var mockChildNode2: AccessibilityNodeInfo

    @Mock
    private lateinit var mockGrandchildNode: AccessibilityNodeInfo

    private lateinit var closeable: AutoCloseable

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    // ==================== RED PHASE TESTS (Should FAIL initially) ====================

    /**
     * TEST 1: Verify all nodes recycled in success path
     * Critical for preventing memory leaks in normal operation
     */
    @Test
    fun `test all nodes recycled in success path`() {
        // Arrange
        `when`(mockRootNode.childCount).thenReturn(2)
        `when`(mockRootNode.getChild(0)).thenReturn(mockChildNode1)
        `when`(mockRootNode.getChild(1)).thenReturn(mockChildNode2)
        `when`(mockChildNode1.childCount).thenReturn(0)
        `when`(mockChildNode2.childCount).thenReturn(0)

        // Act
        AccessibilityNodeManager().use { manager ->
            manager.track(mockRootNode)
            manager.track(mockChildNode1)
            manager.track(mockChildNode2)
        }

        // Assert - ALL nodes must be recycled
        verify(mockRootNode, times(1)).recycle()
        verify(mockChildNode1, times(1)).recycle()
        verify(mockChildNode2, times(1)).recycle()
    }

    /**
     * TEST 2: Verify all nodes recycled when exception thrown
     * Critical for preventing leaks in error paths
     */
    @Test
    fun `test all nodes recycled when exception thrown`() {
        // Arrange
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(mockChildNode1)
        `when`(mockChildNode1.childCount).thenReturn(0)

        // Act & Assert - Exception should be thrown but nodes still recycled
        assertFailsWith<IllegalStateException> {
            AccessibilityNodeManager().use { manager ->
                manager.track(mockRootNode)
                manager.track(mockChildNode1)
                throw IllegalStateException("Simulated error")
            }
        }

        // Verify all nodes recycled despite exception
        verify(mockRootNode, times(1)).recycle()
        verify(mockChildNode1, times(1)).recycle()
    }

    /**
     * TEST 3: Verify all nodes recycled on early return
     * Critical for preventing leaks when conditions not met
     */
    @Test
    fun `test all nodes recycled on early return`() {
        // Arrange
        `when`(mockRootNode.childCount).thenReturn(2)
        `when`(mockRootNode.getChild(0)).thenReturn(mockChildNode1)
        `when`(mockRootNode.getChild(1)).thenReturn(mockChildNode2)
        `when`(mockChildNode1.childCount).thenReturn(0)
        `when`(mockChildNode2.childCount).thenReturn(0)

        // Act
        val result = AccessibilityNodeManager().use { manager ->
            manager.track(mockRootNode)
            manager.track(mockChildNode1)

            // Early return condition
            if (true) {
                return@use false
            }

            manager.track(mockChildNode2)
            true
        }

        // Assert
        assertThat(result).isFalse()

        // Verify tracked nodes recycled (childNode2 never tracked)
        verify(mockRootNode, times(1)).recycle()
        verify(mockChildNode1, times(1)).recycle()
        verify(mockChildNode2, never()).recycle()
    }

    /**
     * TEST 4: Verify traverse method with depth limit
     * Critical for preventing stack overflow
     */
    @Test
    fun `test traverse respects depth limit`() {
        // Arrange - Create a deep tree
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(mockChildNode1)
        `when`(mockChildNode1.childCount).thenReturn(1)
        `when`(mockChildNode1.getChild(0)).thenReturn(mockGrandchildNode)
        `when`(mockGrandchildNode.childCount).thenReturn(0)

        val visitedNodes = mutableListOf<AccessibilityNodeInfo>()

        // Act - Traverse with maxDepth = 1 (should only visit root and child1)
        AccessibilityNodeManager().use { manager ->
            manager.traverse(mockRootNode, maxDepth = 1) { node, depth ->
                visitedNodes.add(node)
            }
        }

        // Assert - Should visit root (depth 0) and child1 (depth 1), but NOT grandchild (depth 2)
        assertThat(visitedNodes).hasSize(2)
        assertThat(visitedNodes).containsExactly(mockRootNode, mockChildNode1)

        // All accessed nodes should be recycled
        verify(mockRootNode, times(1)).recycle()
        verify(mockChildNode1, times(1)).recycle()
    }

    /**
     * TEST 5: Verify circular reference detection
     * Critical for preventing infinite loops
     */
    @Test
    fun `test circular reference detection prevents infinite loop`() {
        // Arrange - Create circular reference (parent -> child -> parent)
        `when`(mockRootNode.childCount).thenReturn(1)
        `when`(mockRootNode.getChild(0)).thenReturn(mockChildNode1)
        `when`(mockChildNode1.childCount).thenReturn(1)
        `when`(mockChildNode1.getChild(0)).thenReturn(mockRootNode)  // Circular!

        val visitedCount = mutableMapOf<AccessibilityNodeInfo, Int>()

        // Act
        AccessibilityNodeManager().use { manager ->
            manager.traverse(mockRootNode, maxDepth = 50) { node, _ ->
                visitedCount[node] = visitedCount.getOrDefault(node, 0) + 1
            }
        }

        // Assert - Each node visited only once (cycle detected)
        assertThat(visitedCount[mockRootNode]).isEqualTo(1)
        assertThat(visitedCount[mockChildNode1]).isEqualTo(1)
    }

    /**
     * TEST 6: Verify null child handling
     * Critical for robustness
     */
    @Test
    fun `test null child nodes handled gracefully`() {
        // Arrange
        `when`(mockRootNode.childCount).thenReturn(2)
        `when`(mockRootNode.getChild(0)).thenReturn(null)  // Null child!
        `when`(mockRootNode.getChild(1)).thenReturn(mockChildNode1)
        `when`(mockChildNode1.childCount).thenReturn(0)

        val visitedNodes = mutableListOf<AccessibilityNodeInfo>()

        // Act
        AccessibilityNodeManager().use { manager ->
            manager.traverse(mockRootNode) { node, _ ->
                visitedNodes.add(node)
            }
        }

        // Assert - Should visit root and childNode1, skip null
        assertThat(visitedNodes).containsExactly(mockRootNode, mockChildNode1)

        // Verify recycling
        verify(mockRootNode, times(1)).recycle()
        verify(mockChildNode1, times(1)).recycle()
    }

    /**
     * TEST 7: Verify track() returns the node for chaining
     * Usability feature
     */
    @Test
    fun `test track returns node for convenient usage`() {
        // Act
        AccessibilityNodeManager().use { manager ->
            val tracked = manager.track(mockRootNode)

            // Assert - track() should return the node
            assertThat(tracked).isEqualTo(mockRootNode)
        }

        // Verify recycling happened
        verify(mockRootNode, times(1)).recycle()
    }

    /**
     * TEST 8: Verify track(null) returns null and doesn't crash
     * Defensive programming
     */
    @Test
    fun `test track null returns null safely`() {
        // Act
        AccessibilityNodeManager().use { manager ->
            val tracked = manager.track(null)

            // Assert
            assertThat(tracked).isNull()
        }

        // No exception thrown = test passes
    }

    /**
     * TEST 9: Verify double-close is safe (idempotent)
     * Defensive programming
     */
    @Test
    fun `test double close is safe`() {
        // Arrange
        val manager = AccessibilityNodeManager()
        manager.track(mockRootNode)

        // Act - Close twice
        manager.close()
        manager.close()  // Should not throw

        // Assert - recycle() called only once
        verify(mockRootNode, times(1)).recycle()
    }

    /**
     * TEST 10: Verify exception during recycle doesn't crash manager
     * Defensive programming
     */
    @Test
    fun `test recycle exception handled gracefully`() {
        // Arrange - Make recycle() throw
        doThrow(IllegalStateException("Already recycled"))
            .`when`(mockRootNode).recycle()

        // Act - Should not throw
        AccessibilityNodeManager().use { manager ->
            manager.track(mockRootNode)
        }

        // If we get here, exception was handled = test passes
        verify(mockRootNode, times(1)).recycle()
    }

    /**
     * TEST 11: Performance test - large tree traversal
     * Ensure efficient traversal of realistic UI trees
     */
    @Test
    fun `test performance with large tree`() {
        // Arrange - Create a tree with 100 nodes
        val nodes = (0..99).map { mock(AccessibilityNodeInfo::class.java) }
        `when`(mockRootNode.childCount).thenReturn(nodes.size)
        nodes.forEachIndexed { index, node ->
            `when`(mockRootNode.getChild(index)).thenReturn(node)
            `when`(node.childCount).thenReturn(0)
        }

        val startTime = System.currentTimeMillis()
        var visitedCount = 0

        // Act
        AccessibilityNodeManager().use { manager ->
            manager.traverse(mockRootNode) { _, _ ->
                visitedCount++
            }
        }

        val duration = System.currentTimeMillis() - startTime

        // Assert - Should complete quickly (<100ms for 101 nodes)
        assertThat(duration).isLessThan(100L)  // Explicit Long to avoid overload ambiguity
        assertThat(visitedCount).isEqualTo(101)  // Root + 100 children

        // All nodes recycled
        verify(mockRootNode, times(1)).recycle()
        nodes.forEach { verify(it, times(1)).recycle() }
    }
}
